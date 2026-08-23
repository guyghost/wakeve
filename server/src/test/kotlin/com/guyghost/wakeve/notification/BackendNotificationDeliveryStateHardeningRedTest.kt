package com.guyghost.wakeve.notification

import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BackendNotificationDeliveryStateHardeningRedTest {
    @Test
    fun `policy suppression is an exact durable state CAS before provider lease or send`() = runBlocking {
        BackendNotificationDurabilityTestFixture().use { fixture ->
            val registration = fixture.register("policy-suppress")
            val key = ingestDelivery(fixture, registration, "policy-suppress")
            val before = fixture.deliveryFactory.openDeliveryRuntime().use { runtime ->
                assertNotNull(runtime.current(key))
            }
            val policyCalls = AtomicInteger()
            val providerCalls = AtomicInteger()
            val composition = composition(
                fixture = fixture,
                policy = BackendDeliveryPolicyPort {
                    policyCalls.incrementAndGet()
                    BackendDeliveryPolicyDecision.SUPPRESS
                },
                provider = BackendDeliveryProviderPort {
                    providerCalls.incrementAndGet()
                    error("suppressed work cannot cross the provider boundary")
                }
            )

            BackendNotificationDeliveryRecoveryScheduler(composition, "suppression-worker").use {
                it.startAndDrainDueWork()
            }
            assertEquals(1, policyCalls.get())
            assertEquals(0, providerCalls.get())

            fixture.deliveryFactory.openDeliveryRuntime().use { restored ->
                val suppressed = assertNotNull(restored.current(key))
                assertEquals(BackendDurableDeliveryState.SUPPRESSED, suppressed.state)
                assertEquals(0L, suppressed.lastLeaseVersion)
                assertEquals(0L, suppressed.lastLeaseFencingToken)
                assertNull(suppressed.pendingCheckpoint)
                val stale = restored.suppressBeforeLeaseContract(
                    key,
                    before.checkpointRevision,
                    before.authority,
                    before.authorityFencingToken
                )
                assertEquals(suppressed, stale, "stale suppression CAS is inert after reopen")
                assertEquals(0, providerCalls.get())
            }
        }
    }

    @Test
    fun `cancel before write is a direct durable CAS while sending cancel becomes correlated unknown outcome`() =
        runBlocking {
            BackendNotificationDurabilityTestFixture().use { fixture ->
                val registration = fixture.register("cancel-before-write")
                val key = ingestDelivery(fixture, registration, "cancel-before-write")
                val before = fixture.deliveryFactory.openDeliveryRuntime().use { runtime ->
                    assertNotNull(runtime.current(key))
                }
                fixture.deliveryFactory.openDeliveryRuntime().use { runtime ->
                    val cancelled = runtime.cancelBeforeWriteContract(
                        key,
                        before.checkpointRevision,
                        before.authority,
                        before.authorityFencingToken
                    )
                    assertEquals(BackendDurableDeliveryState.CANCELLED, cancelled.state)
                    assertNull(cancelled.pendingCheckpoint)
                }
                fixture.deliveryFactory.openDeliveryRuntime().use { restored ->
                    val cancelled = assertNotNull(restored.current(key))
                    assertEquals(BackendDurableDeliveryState.CANCELLED, cancelled.state)
                    val stale = restored.cancelBeforeWriteContract(
                        key,
                        before.checkpointRevision,
                        before.authority,
                        before.authorityFencingToken
                    )
                    assertEquals(cancelled, stale)
                }
            }

            BackendNotificationDurabilityTestFixture().use { fixture ->
                val registration = fixture.register("cancel-sending")
                val key = ingestDelivery(fixture, registration, "cancel-sending")
                fixture.deliveryFactory.openDeliveryRuntime().use { runtime ->
                    val policy = assertNotNull(runtime.current(key))
                    runtime.markPolicyAllowedContract(
                        key, policy.checkpointRevision, policy.authority, policy.authorityFencingToken
                    )
                    val lease = assertNotNull(runtime.acquireLease(key, "cancel-sending-worker"))
                    val auth = assertNotNull(runtime.current(key))
                    assertEquals(BackendDurableDeliveryState.AUTH, auth.state)
                    val sending = runtime.markProviderAuthReadyContract(
                        key, auth.requireCorrelationId(), auth.attempt,
                        lease.holderId, lease.version, lease.fencingToken
                    )
                    assertEquals(BackendDurableDeliveryState.SENDING, sending.state)
                    val staged = runtime.stageSendingCancellationContract(
                        key, sending.requireCorrelationId(), sending.attempt,
                        lease.holderId, lease.version, lease.fencingToken
                    )
                    assertEquals(BackendDurableProviderOutcome.UNKNOWN_OUTCOME, staged.pendingCheckpoint?.outcome)
                    assertEquals(
                        BackendPersistedProviderReason.TRANSPORT_OUTCOME_UNKNOWN,
                        staged.pendingCheckpoint?.reason
                    )
                    assertEquals(lease.holderId, staged.pendingCheckpoint?.leaseHolderId)
                    assertEquals(lease.version, staged.pendingCheckpoint?.leaseVersion)
                    assertEquals(lease.fencingToken, staged.pendingCheckpoint?.leaseFencingToken)
                    val exact = staged.requireProviderReference()
                    assertTrue(runtime.requestProviderCheckpoint(exact))
                    assertEquals(
                        BackendDurableDeliveryState.UNKNOWN_OUTCOME,
                        runtime.acknowledgeProviderCheckpoint(exact).state
                    )
                }
            }
        }

    @Test
    fun `expiry checkpoint is exact from every legitimately reached live recovery state`() = runBlocking {
        val labels = listOf(
            "policy", "quiet", "token", "queued", "auth", "sending", "retry", "unknown", "provider-auth-blocked"
        )
        labels.forEach { label ->
            BackendNotificationDurabilityTestFixture().use { fixture ->
                val registration = fixture.register("expiry-$label")
                val key = ingestDelivery(fixture, registration, "expiry-$label", expiresAt = 105)
                fixture.deliveryFactory.openDeliveryRuntime().use { runtime ->
                    val reached = reachState(runtime, key, label)
                    val atExpiry = runtime.advanceLogicalClock(key, reached.snapshot.clockRevision, 105)
                    val staged = runtime.stageExpiryContract(
                        deliveryKey = key,
                        expectedCheckpointRevision = atExpiry.checkpointRevision,
                        authority = atExpiry.authority,
                        authorityFencingToken = atExpiry.authorityFencingToken,
                        leaseHolderId = reached.lease?.holderId,
                        leaseVersion = reached.lease?.version,
                        leaseFencingToken = reached.lease?.fencingToken
                    )
                    val checkpoint = assertNotNull(staged.pendingCheckpoint)
                    assertEquals(BackendDurableProviderOutcome.EXPIRED, checkpoint.outcome, label)
                    assertFalse(checkpoint.effectRequested, label)
                    assertEquals(reached.lease?.holderId, checkpoint.leaseHolderId, label)
                    assertEquals(reached.lease?.version, checkpoint.leaseVersion, label)
                    assertEquals(reached.lease?.fencingToken, checkpoint.leaseFencingToken, label)
                    assertEquals(reached.snapshot.clockRevision + 1, atExpiry.clockRevision, label)
                    assertEquals(105L, atExpiry.nowEpochSeconds, label)
                    val exact = staged.requireProviderReference()
                    assertEquals(staged, runtime.acknowledgeProviderCheckpoint(exact), "$label ACK before request")
                    if (label == "unknown" || label == "provider-auth-blocked") {
                        val foreign = exact.copy(leaseHolderId = "foreign-expiry-worker")
                        val stale = exact.copy(
                            leaseVersion = assertNotNull(exact.leaseVersion) - 1,
                            leaseFencingToken = assertNotNull(exact.leaseFencingToken) - 1
                        )
                        assertFalse(runtime.requestProviderCheckpoint(foreign), "$label foreign request")
                        assertFalse(runtime.requestProviderCheckpoint(stale), "$label stale request")
                        assertEquals(staged, runtime.acknowledgeProviderCheckpoint(foreign), "$label foreign ACK")
                        assertEquals(staged, runtime.acknowledgeProviderCheckpoint(stale), "$label stale ACK")
                    }
                    assertTrue(runtime.requestProviderCheckpoint(exact), label)
                    val requested = assertNotNull(runtime.current(key))
                    if (label == "unknown" || label == "provider-auth-blocked") {
                        val foreign = exact.copy(leaseHolderId = "foreign-expiry-worker")
                        val stale = exact.copy(
                            leaseVersion = assertNotNull(exact.leaseVersion) - 1,
                            leaseFencingToken = assertNotNull(exact.leaseFencingToken) - 1
                        )
                        assertEquals(requested, runtime.acknowledgeProviderCheckpoint(foreign), "$label emitted foreign ACK")
                        assertEquals(requested, runtime.acknowledgeProviderCheckpoint(stale), "$label emitted stale ACK")
                    }
                    val expired = runtime.acknowledgeProviderCheckpoint(exact)
                    assertEquals(BackendDurableDeliveryState.EXPIRED, expired.state, label)
                    assertEquals(expired, runtime.acknowledgeProviderCheckpoint(exact), "$label double ACK")
                }
            }
        }
    }

    @Test
    fun `ExpiredProviderToken reuses its provider checkpoint and restart refreshes once`() = runBlocking {
        BackendNotificationDurabilityTestFixture().use { fixture ->
            val registration = fixture.register("durable-auth-refresh")
            val key = ingestDelivery(fixture, registration, "durable-auth-refresh")
            val credential = RecordingValidatedCredential("credential-v1", "fingerprint-v1")
            val providerCalls = AtomicInteger()
            val first = composition(
                fixture,
                credential = credential,
                provider = BackendDeliveryProviderPort {
                    providerCalls.incrementAndGet()
                    BackendProviderRawObservation.Http(403, "ExpiredProviderToken", null, "apns-expired", 100)
                },
                fault = BackendDeliveryWorkerFaultInjector { checkpoint ->
                    if (checkpoint == BackendDeliveryWorkerFaultCheckpoint.AFTER_PROVIDER_OBSERVATION_DURABLE) {
                        error("crash-after-provider-refresh-observation")
                    }
                }
            )
            assertFails {
                BackendNotificationDeliveryRecoveryScheduler(first, "refresh-worker-a").use {
                    it.startAndDrainDueWork()
                }
            }
            assertEquals(1, providerCalls.get())
            assertEquals(0, credential.refreshCalls.get())

            lateinit var exact: BackendDeliveryCheckpointReference
            fixture.deliveryFactory.openDeliveryRuntime().use { runtime ->
                val crashed = assertNotNull(runtime.current(key))
                assertEquals(BackendDurableDeliveryState.AWAITING_PROVIDER_RESULT_PERSISTENCE, crashed.state)
                val checkpoint = assertNotNull(crashed.pendingCheckpoint)
                assertEquals(BackendDurableProviderOutcome.REFRESH_AUTH, checkpoint.outcome)
                assertEquals(BackendPersistedProviderReason.PROVIDER_AUTH_REJECTED, checkpoint.reason)
                assertTrue(checkpoint.effectId.isNotBlank())
                assertTrue(checkpoint.revision > 0)
                assertFalse(checkpoint.effectRequested)
                exact = crashed.requireProviderReference()
                assertEquals(crashed.authority, exact.authority)
                assertEquals(crashed.authorityFencingToken, exact.authorityFencingToken)
                assertEquals(checkpoint.leaseHolderId, exact.leaseHolderId)
                assertEquals(checkpoint.leaseVersion, exact.leaseVersion)
                assertEquals(checkpoint.leaseFencingToken, exact.leaseFencingToken)
                assertEquals(crashed, runtime.acknowledgeProviderCheckpoint(exact), "ACK before request is inert")
                assertFalse(
                    runtime.requestProviderCheckpoint(exact.copy(authorityFencingToken = exact.authorityFencingToken - 1))
                )
                assertFalse(runtime.requestProviderCheckpoint(exact.copy(leaseHolderId = "foreign-refresh-worker")))
                assertTrue(runtime.requestProviderCheckpoint(exact))
            }

            lateinit var refreshIdempotencyKey: String
            fixture.deliveryFactory.openDeliveryRuntime().use { runtime ->
                val requested = assertNotNull(runtime.current(key))
                assertTrue(requested.pendingCheckpoint?.effectRequested == true)
                val staleAuthority = exact.copy(authorityFencingToken = exact.authorityFencingToken - 1)
                val foreignLease = exact.copy(leaseHolderId = "foreign-refresh-worker")
                assertEquals(requested, runtime.acknowledgeProviderCheckpoint(staleAuthority))
                assertEquals(requested, runtime.acknowledgeProviderCheckpoint(foreignLease))

                val auth = runtime.acknowledgeProviderCheckpoint(exact)
                assertEquals(BackendDurableDeliveryState.AUTH, auth.state)
                assertNull(auth.pendingCheckpoint)
                assertEquals(1L, auth.authRefreshCount)
                assertTrue(auth.refreshUsed)
                val retainedLease: BackendDeliveryLease = assertNotNull(auth.lease)
                assertEquals(exact.leaseHolderId, retainedLease.holderId)
                assertEquals(exact.leaseVersion, retainedLease.version)
                assertEquals(exact.leaseFencingToken, retainedLease.fencingToken)
                refreshIdempotencyKey = auth.requireCorrelationId()
            }

            val restored = composition(
                fixture,
                credential = credential,
                provider = BackendDeliveryProviderPort {
                    providerCalls.incrementAndGet()
                    BackendProviderRawObservation.Http(403, "ExpiredProviderToken", null, "apns-expired-again", 101)
                }
            )
            BackendNotificationDeliveryRecoveryScheduler(restored, "refresh-worker-a").use {
                it.startAndDrainDueWork()
                it.startAndDrainDueWork()
            }
            assertEquals(1, credential.refreshCalls.get())
            assertEquals(listOf(refreshIdempotencyKey), credential.refreshIdempotencyKeys)
            assertEquals(2, providerCalls.get())
            assertEquals(BackendDurableDeliveryState.PROVIDER_AUTH_BLOCKED, restored.current(key)?.state)
        }
    }

    @Test
    fun `credential circuit opens only from validated port version and fingerprint event`() = runBlocking {
        BackendNotificationDurabilityTestFixture().use { fixture ->
            val credential = RecordingValidatedCredential("credential-v1", "fingerprint-v1")
            val calls = AtomicInteger()
            val first = fixture.register("credential-block-first")
            ingestDelivery(fixture, first, "credential-block-first")
            val composition = composition(
                fixture,
                credential = credential,
                provider = BackendDeliveryProviderPort {
                    calls.incrementAndGet()
                    BackendProviderRawObservation.Http(
                        429, "TooManyProviderTokenUpdates", null, "apns-credential-block", 100
                    )
                }
            )
            BackendNotificationDeliveryRecoveryScheduler(composition, "credential-worker").use { scheduler ->
                scheduler.startAndDrainDueWork()
                val second = fixture.register("credential-block-second")
                val secondKey = ingestDelivery(fixture, second, "credential-block-second")
                scheduler.startAndDrainDueWork()
                assertEquals(1, calls.get())
                assertEquals(BackendDurableDeliveryState.PROVIDER_AUTH_BLOCKED, composition.current(secondKey)?.state)

                credential.version = "credential-v2"
                scheduler.onValidatedCredentialPortChanged()
                val third = fixture.register("credential-block-third")
                val thirdKey = ingestDelivery(fixture, third, "credential-block-third")
                scheduler.startAndDrainDueWork()
                assertEquals(1, calls.get(), "version-only change is not a validated credential rotation")
                assertEquals(BackendDurableDeliveryState.PROVIDER_AUTH_BLOCKED, composition.current(thirdKey)?.state)

                credential.fingerprint = "fingerprint-v2"
                scheduler.onValidatedCredentialPortChanged()
                val fourth = fixture.register("credential-block-fourth")
                ingestDelivery(fixture, fourth, "credential-block-fourth")
                scheduler.startAndDrainDueWork()
                assertTrue(calls.get() > 1)
            }
        }
    }

    @Test
    fun `invalid token reasons and logical timestamp invalidate only the exact registration`() = runBlocking {
        val cases = listOf(
            Triple(400, "BadDeviceToken", DeviceRegistrationInvalidationReason.BAD_DEVICE_TOKEN),
            Triple(400, "DeviceTokenNotForTopic", DeviceRegistrationInvalidationReason.DEVICE_TOKEN_NOT_FOR_TOPIC),
            Triple(410, "ExpiredToken", DeviceRegistrationInvalidationReason.EXPIRED_TOKEN),
            Triple(410, "Unregistered", DeviceRegistrationInvalidationReason.UNREGISTERED)
        )
        cases.forEach { (status, rawReason, expectedReason) ->
            BackendNotificationDurabilityTestFixture().use { fixture ->
                val target = fixture.register("invalid-target-$rawReason")
                val other = fixture.register("invalid-other-$rawReason")
                val key = ingestDelivery(fixture, target, "invalid-target-$rawReason")
                fixture.deliveryFactory.openDeliveryRuntime().use { runtime ->
                    val initial = assertNotNull(runtime.current(key))
                    runtime.advanceLogicalClock(key, initial.clockRevision, 777)
                }
                val composition = composition(
                    fixture,
                    clock = BackendDeliveryWorkerClock { 777 },
                    provider = BackendDeliveryProviderPort {
                        BackendProviderRawObservation.Http(status, rawReason, null, "apns-$rawReason", 777)
                    }
                )
                BackendNotificationDeliveryRecoveryScheduler(composition, "invalidation-worker").use {
                    it.startAndDrainDueWork()
                }
                fixture.registrationFactory.open().use { registrations ->
                    val invalid = assertNotNull(registrations.registration(target.registrationId))
                    assertEquals(DeviceRegistrationStatus.INVALID, invalid.status)
                    assertEquals(expectedReason, invalid.invalidationReason)
                    assertEquals(777L, invalid.invalidatedAtEpochSeconds)
                    assertTrue(assertNotNull(invalid.invalidatedAtEpochSeconds) > 0)
                    val untouched = assertNotNull(registrations.registration(other.registrationId))
                    assertEquals(DeviceRegistrationStatus.ACTIVE, untouched.status)
                    assertNull(untouched.invalidationReason)
                    assertNull(untouched.invalidatedAtEpochSeconds)
                }
            }
        }
    }

    private suspend fun reachState(
        runtime: BackendDeliveryRuntime,
        key: DeliveryKey,
        label: String
    ): ReachedDeliveryState {
        val policy = assertNotNull(runtime.current(key))
        assertEquals("POLICY_CHECK", policy.state.name)
        if (label == "policy") return ReachedDeliveryState(policy, null)
        if (label == "quiet") return ReachedDeliveryState(
            runtime.markQuietHoursContract(
                key, policy.checkpointRevision, policy.authority, policy.authorityFencingToken, 104
            ),
            null
        )
        if (label == "token") return ReachedDeliveryState(
            runtime.markNoActiveTokenContract(
                key, policy.checkpointRevision, policy.authority, policy.authorityFencingToken
            ),
            null
        )
        val queued = runtime.markPolicyAllowedContract(
            key, policy.checkpointRevision, policy.authority, policy.authorityFencingToken
        )
        if (label == "queued") return ReachedDeliveryState(queued, null)
        val lease = assertNotNull(runtime.acquireLease(key, "expiry-$label-worker"))
        val auth = assertNotNull(runtime.current(key))
        assertEquals(BackendDurableDeliveryState.AUTH, auth.state)
        if (label == "auth") return ReachedDeliveryState(auth, lease)
        val sending = runtime.markProviderAuthReadyContract(
            key, auth.requireCorrelationId(), auth.attempt,
            lease.holderId, lease.version, lease.fencingToken
        )
        if (label == "sending") return ReachedDeliveryState(sending, lease)
        val observation = when (label) {
            "retry" -> BackendProviderRawObservation.Http(503, null, null, "apns-retry", 100)
            "unknown" -> BackendProviderRawObservation.Transport(BackendProviderTransportPhase.MAY_HAVE_WRITTEN)
            "provider-auth-blocked" ->
                BackendProviderRawObservation.Http(403, "InvalidProviderToken", null, "apns-blocked", 100)
            else -> error("unknown state label $label")
        }
        val classified = BackendDeliveryObservationClassifier.classify(
            observation,
            BackendDeliveryRetryContext(key, 100, 105, sending.attempt, sending.maxAttempts),
            BackendDeliveryJitterSource { _, _ -> 0.5 }
        )
        val staged = runtime.stageProviderObservation(key, lease, classified, "credential-v1")
        val exact = staged.requireProviderReference()
        assertTrue(runtime.requestProviderCheckpoint(exact))
        val committed = runtime.acknowledgeProviderCheckpoint(exact)
        val retainedLease = when (label) {
            "unknown", "provider-auth-blocked" -> lease
            else -> null
        }
        return ReachedDeliveryState(committed, retainedLease)
    }

    private data class ReachedDeliveryState(
        val snapshot: BackendNotificationDeliverySnapshot,
        val lease: BackendDeliveryLease?
    )

    private fun composition(
        fixture: BackendNotificationDurabilityTestFixture,
        clock: BackendDeliveryWorkerClock = BackendDeliveryWorkerClock { 100 },
        policy: BackendDeliveryPolicyPort = BackendDeliveryPolicyPort { BackendDeliveryPolicyDecision.ALLOW },
        credential: RecordingValidatedCredential = RecordingValidatedCredential("credential-v1", "fingerprint-v1"),
        provider: BackendDeliveryProviderPort,
        fault: BackendDeliveryWorkerFaultInjector = BackendDeliveryWorkerFaultInjector { }
    ) = BackendNotificationDeliveryWorkerComposition(
        deliveryStoreFactory = fixture.deliveryFactory,
        registrationStoreFactory = fixture.registrationFactory,
        authority = BackendDeliveryAuthority.OUTBOX_V2,
        clock = clock,
        policy = policy,
        tokenAvailability = BackendDeliveryTokenAvailabilityPort { true },
        credentials = credential,
        validatedCredentialPort = credential,
        provider = provider,
        jitter = BackendDeliveryJitterSource { _, _ -> 0.5 },
        faultInjector = fault
    )

    private suspend fun ingestDelivery(
        fixture: BackendNotificationDurabilityTestFixture,
        registration: BackendDeviceRegistration,
        identity: String,
        expiresAt: Long = 1_000
    ): DeliveryKey = BackendNotificationIngestionService(
        storeFactory = fixture.deliveryFactory,
        faultInjector = BackendNotificationIngestionFaultInjector { },
        committedPort = BackendNotificationIngestionCommittedPort { }
    ).ingest(
        BackendNotificationIngestionCommand(
            domainEventId = "delivery-state-$identity",
            effectType = "DATE_CONFIRMED",
            schemaVersion = 1,
            logicalNotificationId = "logical-delivery-state-$identity",
            recipients = listOf(
                BackendNotificationRecipientIntent(
                    participantId = "participant-delivery-state-$identity",
                    channel = "push",
                    provider = "apns",
                    registrationIds = listOf(registration.registrationId),
                    expiresAtEpochSeconds = expiresAt
                )
            )
        )
    ).deliveryKeys.single()

    private fun BackendNotificationDeliverySnapshot.requireProviderReference(): BackendDeliveryCheckpointReference {
        val checkpoint = assertNotNull(pendingCheckpoint)
        return BackendDeliveryCheckpointReference(
            deliveryKey, checkpoint.effectId, checkpoint.revision, authority, authorityFencingToken,
            checkpoint.leaseHolderId, checkpoint.leaseVersion, checkpoint.leaseFencingToken
        )
    }

    private fun BackendNotificationDeliverySnapshot.requireCorrelationId(): String =
        correlationId ?: error("correlated delivery state is required")

    private suspend fun BackendDeliveryRuntime.markQuietHoursContract(
        deliveryKey: DeliveryKey,
        expectedCheckpointRevision: Long,
        authority: DeliveryAuthority?,
        authorityFencingToken: Long,
        nextEligibleAtEpochSeconds: Long
    ): BackendNotificationDeliverySnapshot = markQuietHours(
        deliveryKey, expectedCheckpointRevision, authority, authorityFencingToken, nextEligibleAtEpochSeconds
    )

    private suspend fun BackendDeliveryRuntime.markNoActiveTokenContract(
        deliveryKey: DeliveryKey,
        expectedCheckpointRevision: Long,
        authority: DeliveryAuthority?,
        authorityFencingToken: Long
    ): BackendNotificationDeliverySnapshot = markNoActiveToken(
        deliveryKey, expectedCheckpointRevision, authority, authorityFencingToken
    )

    private suspend fun BackendDeliveryRuntime.markPolicyAllowedContract(
        deliveryKey: DeliveryKey,
        expectedCheckpointRevision: Long,
        authority: DeliveryAuthority?,
        authorityFencingToken: Long
    ): BackendNotificationDeliverySnapshot = markPolicyAllowed(
        deliveryKey, expectedCheckpointRevision, authority, authorityFencingToken
    )

    private suspend fun BackendDeliveryRuntime.markProviderAuthReadyContract(
        deliveryKey: DeliveryKey,
        correlationId: String,
        attempt: Long,
        leaseHolderId: String,
        leaseVersion: Long,
        leaseFencingToken: Long
    ): BackendNotificationDeliverySnapshot = markProviderAuthReady(
        deliveryKey, correlationId, attempt, leaseHolderId, leaseVersion, leaseFencingToken
    )

    private suspend fun BackendDeliveryRuntime.suppressBeforeLeaseContract(
        deliveryKey: DeliveryKey,
        expectedCheckpointRevision: Long,
        authority: DeliveryAuthority?,
        authorityFencingToken: Long
    ): BackendNotificationDeliverySnapshot = suppressBeforeLease(
        deliveryKey, expectedCheckpointRevision, authority, authorityFencingToken
    )

    private suspend fun BackendDeliveryRuntime.cancelBeforeWriteContract(
        deliveryKey: DeliveryKey,
        expectedCheckpointRevision: Long,
        authority: DeliveryAuthority?,
        authorityFencingToken: Long
    ): BackendNotificationDeliverySnapshot = cancelBeforeWrite(
        deliveryKey, expectedCheckpointRevision, authority, authorityFencingToken
    )

    private suspend fun BackendDeliveryRuntime.stageSendingCancellationContract(
        deliveryKey: DeliveryKey,
        correlationId: String,
        attempt: Long,
        leaseHolderId: String,
        leaseVersion: Long,
        leaseFencingToken: Long
    ): BackendNotificationDeliverySnapshot = stageSendingCancellation(
        deliveryKey, correlationId, attempt, leaseHolderId, leaseVersion, leaseFencingToken
    )

    private suspend fun BackendDeliveryRuntime.stageExpiryContract(
        deliveryKey: DeliveryKey,
        expectedCheckpointRevision: Long,
        authority: DeliveryAuthority?,
        authorityFencingToken: Long,
        leaseHolderId: String?,
        leaseVersion: Long?,
        leaseFencingToken: Long?
    ): BackendNotificationDeliverySnapshot = stageExpiry(
        deliveryKey, expectedCheckpointRevision, authority, authorityFencingToken,
        leaseHolderId, leaseVersion, leaseFencingToken
    )

    private class RecordingValidatedCredential(
        var version: String,
        var fingerprint: String
    ) : BackendDeliveryCredentialPort, BackendValidatedDeliveryCredentialPort {
        val refreshCalls = AtomicInteger()
        val refreshIdempotencyKeys = mutableListOf<String>()

        override suspend fun credentialVersion(): String = version

        override suspend fun refreshAfterProviderRejection(expectedVersion: String): Boolean {
            error("credential refresh must carry the durable provider correlation key")
        }

        override suspend fun refreshAfterProviderRejection(
            expectedVersion: String,
            idempotencyKey: String
        ): Boolean {
            assertEquals(version, expectedVersion)
            assertTrue(idempotencyKey.isNotBlank())
            refreshCalls.incrementAndGet()
            refreshIdempotencyKeys += idempotencyKey
            return true
        }

        override suspend fun validatedCredential(): BackendValidatedDeliveryCredential =
            BackendValidatedDeliveryCredential(version, fingerprint)
    }
}
