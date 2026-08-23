package com.guyghost.wakeve.notification

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.sql.DriverManager
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BackendNotificationDeliveryWorkerDurabilityRedTest {
    @Test
    fun `provider observation is checkpointed before terminal state and restart resumes without a second send`() =
        runBlocking {
            listOf(
                BackendDeliveryWorkerFaultCheckpoint.AFTER_PROVIDER_OBSERVATION_DURABLE,
                BackendDeliveryWorkerFaultCheckpoint.AFTER_PROVIDER_RESULT_COMMIT
            ).forEach { crashAt ->
                BackendNotificationDurabilityTestFixture().use { fixture ->
                    val registration = fixture.register("checkpoint-${crashAt.name.lowercase()}")
                    val key = seedQueuedDelivery(fixture, registration, "checkpoint-${crashAt.name.lowercase()}")
                    val providerCalls = AtomicInteger()
                    val crashing = composition(
                        fixture = fixture,
                        provider = BackendDeliveryProviderPort { request ->
                            providerCalls.incrementAndGet()
                            BackendProviderRawObservation.Http(
                                statusCode = 200,
                                rawReason = "Success",
                                retryAfterEpochSeconds = null,
                                providerRequestId = "apns-${request.deliveryKey.value}",
                                observedAtEpochSeconds = 123
                            )
                        },
                        fault = BackendDeliveryWorkerFaultInjector { checkpoint ->
                            if (checkpoint == crashAt) error("simulated-process-death-${checkpoint.name}")
                        }
                    )

                    assertFails {
                        BackendNotificationDeliveryRecoveryScheduler(crashing, "worker-a")
                            .use { it.startAndDrainDueWork() }
                    }
                    assertEquals(1, providerCalls.get())

                    fixture.deliveryFactory.openDeliveryRuntime(noDeliveryFault()).use { runtime ->
                        val durable = assertNotNull(runtime.current(key))
                        if (crashAt == BackendDeliveryWorkerFaultCheckpoint.AFTER_PROVIDER_OBSERVATION_DURABLE) {
                            assertEquals(
                                BackendDurableDeliveryState.AWAITING_PROVIDER_RESULT_PERSISTENCE,
                                durable.state
                            )
                            assertEquals(BackendDurableProviderOutcome.ACCEPTED, durable.pendingCheckpoint?.outcome)
                            assertNull(durable.acceptedAtEpochSeconds)
                            val exact = durable.requireProviderReference()
                            val stale = exact.copy(leaseFencingToken = exact.leaseFencingToken?.plus(1))
                            assertEquals(durable, runtime.acknowledgeProviderCheckpoint(stale))
                            assertTrue(runtime.requestProviderCheckpoint(exact))
                        } else {
                            assertEquals(BackendDurableDeliveryState.ACCEPTED_BY_APNS, durable.state)
                            assertEquals(123L, durable.acceptedAtEpochSeconds)
                        }
                    }

                    val restarted = composition(
                        fixture = fixture,
                        provider = BackendDeliveryProviderPort {
                            error("a durable provider observation must be resumed, never resent")
                        }
                    )
                    BackendNotificationDeliveryRecoveryScheduler(restarted, "worker-a")
                        .use { it.startAndDrainDueWork() }
                    val terminal = assertNotNull(restarted.current(key))
                    assertEquals(BackendDurableDeliveryState.ACCEPTED_BY_APNS, terminal.state)
                    assertEquals(123L, terminal.acceptedAtEpochSeconds, "Only HTTP 200 records accepted_at.")
                    assertNull(terminal.pendingCheckpoint)
                    assertEquals(1, providerCalls.get())
                }
            }
        }

    @Test
    fun `retry deadline and logical clock survive restart and scheduler resumes without another ingress request`() =
        runBlocking {
            BackendNotificationDurabilityTestFixture().use { fixture ->
                val registration = fixture.register("retry-restart")
                val key = seedQueuedDelivery(fixture, registration, "retry-restart", expiresAt = 500)
                val observations = ConcurrentLinkedQueue<BackendProviderRawObservation>().apply {
                    add(
                        BackendProviderRawObservation.Http(
                            statusCode = 503,
                            rawReason = "ServiceUnavailable",
                            retryAfterEpochSeconds = 150.0,
                            providerRequestId = "apns-retry",
                            observedAtEpochSeconds = 100
                        )
                    )
                    add(
                        BackendProviderRawObservation.Http(
                            statusCode = 200,
                            rawReason = "Success",
                            retryAfterEpochSeconds = null,
                            providerRequestId = "apns-accepted",
                            observedAtEpochSeconds = 151
                        )
                    )
                }
                val calls = AtomicInteger()
                val provider = BackendDeliveryProviderPort {
                    calls.incrementAndGet()
                    assertNotNull(observations.poll())
                }
                val first = composition(fixture, provider)
                BackendNotificationDeliveryRecoveryScheduler(first, "retry-worker-a")
                    .use { it.startAndDrainDueWork() }
                val retry = assertNotNull(first.current(key))
                assertEquals(BackendDurableDeliveryState.RETRY_SCHEDULED, retry.state)
                assertEquals(1L, retry.attempt)
                assertEquals(150L, retry.nextAttemptAtEpochSeconds)

                val beforeDue = composition(fixture, provider)
                BackendNotificationDeliveryRecoveryScheduler(beforeDue, "retry-worker-b")
                    .use { it.startAndDrainDueWork() }
                assertEquals(1, calls.get(), "A restart before the durable deadline cannot send early.")

                val due = beforeDue.advanceLogicalClock(
                    deliveryKey = key,
                    expectedClockRevision = retry.clockRevision,
                    newEpochSeconds = 150
                )
                assertEquals(150L, due.nowEpochSeconds)
                val restartedAtDeadline = composition(fixture, provider)
                BackendNotificationDeliveryRecoveryScheduler(restartedAtDeadline, "retry-worker-c")
                    .use { it.startAndDrainDueWork() }
                assertEquals(2, calls.get())
                assertEquals(BackendDurableDeliveryState.ACCEPTED_BY_APNS, restartedAtDeadline.current(key)?.state)
                assertEquals(151L, restartedAtDeadline.current(key)?.acceptedAtEpochSeconds)
            }
        }

    @Test
    fun `two concurrent recovery workers have one fenced lease winner and one provider write`() = runBlocking {
        BackendNotificationDurabilityTestFixture().use { fixture ->
            val registration = fixture.register("concurrent-workers")
            val key = seedQueuedDelivery(fixture, registration, "concurrent-workers")
            val startGate = BackendNotificationConcurrentStartGate()
            val sendEntered = CompletableDeferred<Unit>()
            val releaseSend = CompletableDeferred<Unit>()
            val calls = AtomicInteger()
            val provider = BackendDeliveryProviderPort { request ->
                calls.incrementAndGet()
                sendEntered.complete(Unit)
                releaseSend.await()
                BackendProviderRawObservation.Http(
                    statusCode = 200,
                    rawReason = "Success",
                    retryAfterEpochSeconds = null,
                    providerRequestId = request.apnsId,
                    observedAtEpochSeconds = 125
                )
            }
            val workerA = composition(fixture, provider)
            val workerB = composition(fixture, provider)

            val runResults = withTimeout(5_000) {
                coroutineScope {
                    val runs = listOf(
                        async(Dispatchers.IO) {
                            startGate.awaitReleaseBeforePortCall()
                            BackendNotificationDeliveryRecoveryScheduler(workerA, "concurrent-a")
                                .use { it.startAndDrainDueWork() }
                        },
                        async(Dispatchers.IO) {
                            startGate.awaitReleaseBeforePortCall()
                            BackendNotificationDeliveryRecoveryScheduler(workerB, "concurrent-b")
                                .use { it.startAndDrainDueWork() }
                        }
                    )
                    startGate.releaseTogether()
                    sendEntered.await()
                    releaseSend.complete(Unit)
                    runs.awaitAll()
                }
            }

            assertEquals(1, runResults.count { key in it.claimedDeliveryKeys })
            assertEquals(1, runResults.count { key !in it.claimedDeliveryKeys })
            assertEquals(1, calls.get(), "The provider boundary may be crossed by exactly one lease holder.")
            val snapshot = assertNotNull(workerA.current(key))
            assertEquals(BackendDurableDeliveryState.ACCEPTED_BY_APNS, snapshot.state)
            assertTrue(snapshot.lastLeaseVersion > 0)
            assertTrue(snapshot.lastLeaseFencingToken > 0)
            assertEquals(1, snapshot.providerCheckpointCount)
        }
    }

    @Test
    fun `outbox v2 authority policy quiet hours and exact token gate every provider call`() = runBlocking {
        BackendNotificationDurabilityTestFixture().use { fixture ->
            val registration = fixture.register("policy-token")
            val key = seedQueuedDelivery(fixture, registration, "policy-token")
            var now = 100L
            var tokenAvailable = false
            val calls = AtomicInteger()
            val policy = BackendDeliveryPolicyPort { context ->
                if (context.nowEpochSeconds < 120) {
                    BackendDeliveryPolicyDecision.QuietUntil(120)
                } else {
                    BackendDeliveryPolicyDecision.ALLOW
                }
            }
            val composition = composition(
                fixture = fixture,
                provider = BackendDeliveryProviderPort {
                    calls.incrementAndGet()
                    BackendProviderRawObservation.Http(200, "Success", null, "apns-policy", now)
                },
                clock = BackendDeliveryWorkerClock { now },
                policy = policy,
                tokenAvailability = BackendDeliveryTokenAvailabilityPort { candidate ->
                    candidate == registration.registrationId && tokenAvailable
                }
            )

            BackendNotificationDeliveryRecoveryScheduler(composition, "policy-worker")
                .use { it.startAndDrainDueWork() }
            assertEquals(BackendDurableDeliveryState.DEFERRED_QUIET_HOURS, composition.current(key)?.state)
            assertEquals(0, calls.get())

            now = 120
            val quiet = assertNotNull(composition.current(key))
            composition.advanceLogicalClock(key, quiet.clockRevision, now)
            BackendNotificationDeliveryRecoveryScheduler(composition, "policy-worker")
                .use { it.startAndDrainDueWork() }
            assertEquals(BackendDurableDeliveryState.AWAITING_TOKEN, composition.current(key)?.state)
            assertEquals(0, calls.get())

            tokenAvailable = true
            BackendNotificationDeliveryRecoveryScheduler(composition, "policy-worker")
                .use { it.onRegistrationActivated(registration.registrationId); it.startAndDrainDueWork() }
            assertEquals(1, calls.get())
            assertEquals(BackendDurableDeliveryState.ACCEPTED_BY_APNS, composition.current(key)?.state)

            val legacyRegistration = fixture.register("legacy-authority")
            val legacyKey = seedQueuedDelivery(
                fixture,
                legacyRegistration,
                "legacy-authority",
                authority = DeliveryAuthority("legacy")
            )
            BackendNotificationDeliveryRecoveryScheduler(composition, "policy-worker")
                .use { it.startAndDrainDueWork() }
            assertEquals(1, calls.get(), "The outbox-v2 worker cannot send a legacy-authority delivery.")
            assertFalse(composition.current(legacyKey)?.state == BackendDurableDeliveryState.ACCEPTED_BY_APNS)
        }
    }

    @Test
    fun `provider map invalidates only the exact registration while 200 accepts the other delivery`() = runBlocking {
        BackendNotificationDurabilityTestFixture().use { fixture ->
            val invalid = fixture.register("invalid-phone")
            val invalidKey = seedQueuedDelivery(fixture, invalid, "invalid-phone")
            val invalidProviderCalls = AtomicInteger()
            val crashingInvalidation = composition(
                fixture = fixture,
                provider = BackendDeliveryProviderPort {
                    invalidProviderCalls.incrementAndGet()
                    BackendProviderRawObservation.Http(410, "Unregistered", null, "apns-invalid", 130)
                },
                fault = crashAfterProviderObservation()
            )

            assertFails {
                BackendNotificationDeliveryRecoveryScheduler(crashingInvalidation, "mapping-worker-a")
                    .use { it.startAndDrainDueWork() }
            }
            assertEquals(1, invalidProviderCalls.get())
            assertProviderCheckpointRequiresExactEmittedAck(
                fixture,
                invalidKey,
                BackendDurableProviderOutcome.INVALID_TOKEN,
                BackendPersistedProviderReason.TOKEN_INVALID
            )
            fixture.registrationFactory.open().use { registrations ->
                assertEquals(DeviceRegistrationStatus.ACTIVE, registrations.registration(invalid.registrationId)?.status)
            }

            val resumedInvalidation = composition(
                fixture = fixture,
                provider = BackendDeliveryProviderPort { error("410 was durably observed and cannot be resent") }
            )
            BackendNotificationDeliveryRecoveryScheduler(resumedInvalidation, "mapping-worker-a")
                .use { it.startAndDrainDueWork() }
            assertEquals(1, invalidProviderCalls.get())
            assertEquals(BackendDurableDeliveryState.INVALID_TOKEN, resumedInvalidation.current(invalidKey)?.state)
            assertNull(resumedInvalidation.current(invalidKey)?.acceptedAtEpochSeconds)
            fixture.registrationFactory.open().use { registrations ->
                assertEquals(DeviceRegistrationStatus.INVALID, registrations.registration(invalid.registrationId)?.status)
                assertEquals(DeviceRegistrationInvalidationReason.UNREGISTERED, registrations.registration(invalid.registrationId)?.invalidationReason)
            }

            val healthy = fixture.register("healthy-tablet")
            val healthyKey = seedQueuedDelivery(fixture, healthy, "healthy-tablet")
            val healthyComposition = composition(
                fixture = fixture,
                provider = BackendDeliveryProviderPort {
                    BackendProviderRawObservation.Http(200, "Success", null, "apns-healthy", 131)
                }
            )
            BackendNotificationDeliveryRecoveryScheduler(healthyComposition, "mapping-worker-c")
                .use { it.startAndDrainDueWork() }
            assertEquals(BackendDurableDeliveryState.ACCEPTED_BY_APNS, healthyComposition.current(healthyKey)?.state)
            assertEquals(131L, healthyComposition.current(healthyKey)?.acceptedAtEpochSeconds)
            fixture.registrationFactory.open().use { registrations ->
                assertEquals(DeviceRegistrationStatus.ACTIVE, registrations.registration(healthy.registrationId)?.status)
            }
        }
    }

    @Test
    fun `may have written is unknown outcome and one auth refresh then blocks the credential circuit`() = runBlocking {
        BackendNotificationDurabilityTestFixture().use { fixture ->
            val uncertain = fixture.register("unknown-outcome")
            val uncertainKey = seedQueuedDelivery(fixture, uncertain, "unknown-outcome")
            val unknownCalls = AtomicInteger()
            val crashingUnknown = composition(
                fixture = fixture,
                provider = BackendDeliveryProviderPort {
                    unknownCalls.incrementAndGet()
                    BackendProviderRawObservation.Transport(BackendProviderTransportPhase.MAY_HAVE_WRITTEN)
                },
                fault = crashAfterProviderObservation()
            )
            assertFails {
                BackendNotificationDeliveryRecoveryScheduler(crashingUnknown, "unknown-worker-a")
                    .use { it.startAndDrainDueWork() }
            }
            assertEquals(1, unknownCalls.get())
            assertProviderCheckpointRequiresExactEmittedAck(
                fixture,
                uncertainKey,
                BackendDurableProviderOutcome.UNKNOWN_OUTCOME,
                BackendPersistedProviderReason.TRANSPORT_OUTCOME_UNKNOWN
            )
            val resumedUnknown = composition(
                fixture = fixture,
                provider = BackendDeliveryProviderPort { error("may-have-written must never be replayed automatically") }
            )
            BackendNotificationDeliveryRecoveryScheduler(resumedUnknown, "unknown-worker-a")
                .use { it.startAndDrainDueWork() }
            val unknownSnapshot = assertNotNull(resumedUnknown.current(uncertainKey))
            assertEquals(BackendDurableDeliveryState.UNKNOWN_OUTCOME, unknownSnapshot.state)
            assertEquals(BackendPersistedProviderReason.TRANSPORT_OUTCOME_UNKNOWN, unknownSnapshot.providerReason)
            assertNull(unknownSnapshot.acceptedAtEpochSeconds)
            assertEquals(1, unknownCalls.get())

            val authRegistration = fixture.register("auth-refresh")
            val authKey = seedQueuedDelivery(fixture, authRegistration, "auth-refresh")
            val credentials = RecordingCredentials("credential-1")
            val sends = AtomicInteger()
            val crashingAuth = composition(
                fixture = fixture,
                provider = BackendDeliveryProviderPort {
                    sends.incrementAndGet()
                    BackendProviderRawObservation.Http(403, "ExpiredProviderToken", null, "apns-auth", 140)
                },
                credentials = credentials,
                fault = crashAfterProviderObservation()
            )
            assertFails {
                BackendNotificationDeliveryRecoveryScheduler(crashingAuth, "auth-worker-a")
                    .use { it.startAndDrainDueWork() }
            }
            assertEquals(1, sends.get())
            assertEquals(0, credentials.refreshes.get())
            assertProviderCheckpointRequiresExactEmittedAck(
                fixture,
                authKey,
                BackendDurableProviderOutcome.REFRESH_AUTH,
                BackendPersistedProviderReason.PROVIDER_AUTH_REJECTED
            )

            val auth = composition(
                fixture = fixture,
                provider = BackendDeliveryProviderPort {
                    sends.incrementAndGet()
                    BackendProviderRawObservation.Http(403, "ExpiredProviderToken", null, "apns-auth", 141)
                },
                credentials = credentials
            )
            BackendNotificationDeliveryRecoveryScheduler(auth, "auth-worker-a")
                .use { it.startAndDrainDueWork() }
            assertEquals(2, sends.get(), "Exactly one correlated refresh permits one resend.")
            assertEquals(1, credentials.refreshes.get())
            assertEquals(BackendDurableDeliveryState.PROVIDER_AUTH_BLOCKED, auth.current(authKey)?.state)
            assertEquals(0L, auth.current(authKey)?.attempt, "Auth refresh must not consume delivery retry budget.")

            val blockedRegistration = fixture.register("auth-circuit")
            val blockedKey = seedQueuedDelivery(fixture, blockedRegistration, "auth-circuit")
            BackendNotificationDeliveryRecoveryScheduler(auth, "auth-worker")
                .use { it.startAndDrainDueWork() }
            assertEquals(2, sends.get(), "The process circuit blocks the same credential before provider I/O.")
            assertEquals(BackendDurableDeliveryState.PROVIDER_AUTH_BLOCKED, auth.current(blockedKey)?.state)

            credentials.version = "credential-2"
            BackendNotificationDeliveryRecoveryScheduler(auth, "auth-worker")
                .use { it.onCredentialsRotated("credential-2"); it.startAndDrainDueWork() }
            assertTrue(sends.get() > 2, "A validated credential rotation is the only circuit reset.")
        }
    }

    private fun composition(
        fixture: BackendNotificationDurabilityTestFixture,
        provider: BackendDeliveryProviderPort,
        clock: BackendDeliveryWorkerClock = BackendDeliveryWorkerClock { 100 },
        policy: BackendDeliveryPolicyPort = BackendDeliveryPolicyPort { BackendDeliveryPolicyDecision.ALLOW },
        tokenAvailability: BackendDeliveryTokenAvailabilityPort = BackendDeliveryTokenAvailabilityPort { true },
        credentials: BackendDeliveryCredentialPort = RecordingCredentials("credential-1"),
        fault: BackendDeliveryWorkerFaultInjector = noDeliveryFault()
    ) = BackendNotificationDeliveryWorkerComposition(
        deliveryStoreFactory = fixture.deliveryFactory,
        registrationStoreFactory = fixture.registrationFactory,
        authority = BackendDeliveryAuthority.OUTBOX_V2,
        clock = clock,
        policy = policy,
        tokenAvailability = tokenAvailability,
        credentials = credentials,
        provider = provider,
        jitter = BackendDeliveryJitterSource { _, _ -> 0.5 },
        faultInjector = fault
    )

    private suspend fun assertProviderCheckpointRequiresExactEmittedAck(
        fixture: BackendNotificationDurabilityTestFixture,
        deliveryKey: DeliveryKey,
        expectedOutcome: BackendDurableProviderOutcome,
        expectedReason: BackendPersistedProviderReason
    ) {
        fixture.deliveryFactory.openDeliveryRuntime(noDeliveryFault()).use { runtime ->
            val staged = assertNotNull(runtime.current(deliveryKey))
            assertEquals(BackendDurableDeliveryState.AWAITING_PROVIDER_RESULT_PERSISTENCE, staged.state)
            assertEquals(expectedOutcome, staged.pendingCheckpoint?.outcome)
            assertEquals(expectedReason, staged.pendingCheckpoint?.reason)
            assertFalse(staged.pendingCheckpoint?.effectRequested == true)
            assertNull(staged.acceptedAtEpochSeconds)

            val exact = staged.requireProviderReference()
            assertEquals(staged, runtime.acknowledgeProviderCheckpoint(exact), "ACK before emission is inert.")
            val stale = exact.copy(checkpointRevision = exact.checkpointRevision + 1)
            assertEquals(staged, runtime.acknowledgeProviderCheckpoint(stale), "A stale checkpoint ACK is inert.")

            assertTrue(runtime.requestProviderCheckpoint(exact))
            val emitted = assertNotNull(runtime.current(deliveryKey))
            assertTrue(emitted.pendingCheckpoint?.effectRequested == true)
            assertEquals(emitted, runtime.acknowledgeProviderCheckpoint(stale), "Emission cannot authorize a stale ACK.")
            assertEquals(BackendDurableDeliveryState.AWAITING_PROVIDER_RESULT_PERSISTENCE, emitted.state)
        }
    }

    private fun crashAfterProviderObservation() = BackendDeliveryWorkerFaultInjector { checkpoint ->
        if (checkpoint == BackendDeliveryWorkerFaultCheckpoint.AFTER_PROVIDER_OBSERVATION_DURABLE) {
            error("simulated-process-death-after-provider-observation")
        }
    }

    private suspend fun seedQueuedDelivery(
        fixture: BackendNotificationDurabilityTestFixture,
        registration: BackendDeviceRegistration,
        identity: String,
        expiresAt: Long = 1_000,
        authority: DeliveryAuthority = DeliveryAuthority("outbox-v2")
    ): DeliveryKey {
        val key = BackendNotificationIngestionService(
            fixture.deliveryFactory,
            BackendNotificationIngestionFaultInjector { },
            BackendNotificationIngestionCommittedPort { }
        ).ingest(
            BackendNotificationIngestionCommand(
                domainEventId = "worker-$identity",
                effectType = "DATE_CONFIRMED",
                schemaVersion = 1,
                logicalNotificationId = "logical-worker-$identity",
                recipients = listOf(
                    BackendNotificationRecipientIntent(
                        participantId = "participant-$identity",
                        channel = "push",
                        provider = "apns",
                        registrationIds = listOf(registration.registrationId),
                        expiresAtEpochSeconds = expiresAt
                    )
                )
            )
        ).deliveryKeys.single()
        if (authority != DeliveryAuthority("outbox-v2")) {
            DriverManager.getConnection("jdbc:sqlite:${fixture.databasePath}").use { connection ->
                connection.prepareStatement(
                    "UPDATE notification_delivery_authority SET authority = ? WHERE delivery_key = ?"
                ).use { statement ->
                    statement.setString(1, authority.value)
                    statement.setString(2, key.value)
                    assertEquals(1, statement.executeUpdate(), "test-only legacy authority bootstrap failed")
                }
            }
        }
        return key
    }

    private fun BackendNotificationDeliverySnapshot.requireProviderReference(): BackendDeliveryCheckpointReference {
        val checkpoint = assertNotNull(pendingCheckpoint)
        return BackendDeliveryCheckpointReference(
            deliveryKey = deliveryKey,
            effectId = checkpoint.effectId,
            checkpointRevision = checkpoint.revision,
            authority = authority,
            authorityFencingToken = authorityFencingToken,
            leaseHolderId = checkpoint.leaseHolderId,
            leaseVersion = checkpoint.leaseVersion,
            leaseFencingToken = checkpoint.leaseFencingToken
        )
    }

    private class RecordingCredentials(var version: String) : BackendDeliveryCredentialPort {
        val refreshes = AtomicInteger()

        override suspend fun credentialVersion(): String = version

        override suspend fun refreshAfterProviderRejection(expectedVersion: String): Boolean {
            refreshes.incrementAndGet()
            return expectedVersion == version
        }
    }

    private fun noDeliveryFault() = BackendDeliveryWorkerFaultInjector { }
}
