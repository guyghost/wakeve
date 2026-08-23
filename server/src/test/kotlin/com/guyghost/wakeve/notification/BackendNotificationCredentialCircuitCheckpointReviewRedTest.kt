package com.guyghost.wakeve.notification

import kotlinx.coroutines.runBlocking
import java.sql.DriverManager
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BackendNotificationCredentialCircuitCheckpointReviewRedTest {
    @Test
    fun `already open credential circuit stages and replays exact providerAuthBlocked checkpoint`() = runBlocking {
        BackendNotificationDurabilityTestFixture().use { fixture ->
            val credential = RecordingCredential(refreshResult = true)
            val firstRegistration = fixture.register("open-circuit-first")
            val firstKey = ingestDelivery(fixture, firstRegistration, "open-circuit-first")
            val firstProviderCalls = AtomicInteger()
            val opener = composition(
                fixture,
                credential,
                BackendDeliveryProviderPort {
                    firstProviderCalls.incrementAndGet()
                    BackendProviderRawObservation.Http(
                        429, "TooManyProviderTokenUpdates", null, "apns-open-circuit", 100
                    )
                }
            )
            BackendNotificationDeliveryRecoveryScheduler(opener, "circuit-opener").use {
                it.startAndDrainDueWork()
            }
            assertEquals(1, firstProviderCalls.get())
            assertEquals(BackendDurableDeliveryState.PROVIDER_AUTH_BLOCKED, opener.current(firstKey)?.state)

            val secondRegistration = fixture.register("open-circuit-second")
            val secondKey = ingestDelivery(fixture, secondRegistration, "open-circuit-second")
            val secondProviderCalls = AtomicInteger()
            val crashing = composition(
                fixture,
                credential,
                BackendDeliveryProviderPort {
                    secondProviderCalls.incrementAndGet()
                    error("an open credential circuit must block before provider I/O")
                },
                BackendDeliveryWorkerFaultInjector { checkpoint ->
                    if (checkpoint == BackendDeliveryWorkerFaultCheckpoint.AFTER_PROVIDER_OBSERVATION_DURABLE) {
                        error("crash-after-circuit-block-checkpoint")
                    }
                }
            )
            assertFails {
                BackendNotificationDeliveryRecoveryScheduler(crashing, "circuit-blocked-worker").use {
                    it.startAndDrainDueWork()
                }
            }
            assertEquals(0, secondProviderCalls.get())

            assertBlockedCheckpointThenCommit(fixture, secondKey)
        }
    }

    @Test
    fun `failed credential refresh stages and replays exact providerAuthBlocked checkpoint`() = runBlocking {
        BackendNotificationDurabilityTestFixture().use { fixture ->
            val credential = RecordingCredential(refreshResult = false)
            val registration = fixture.register("refresh-false")
            val key = ingestDelivery(fixture, registration, "refresh-false")
            val providerCalls = AtomicInteger()
            val refreshCheckpoint = AtomicReference<CheckpointIdentity>()
            val crashAfterRefreshAck = composition(
                fixture,
                credential,
                BackendDeliveryProviderPort {
                    providerCalls.incrementAndGet()
                    BackendProviderRawObservation.Http(403, "ExpiredProviderToken", null, "apns-refresh-false", 100)
                },
                BackendDeliveryWorkerFaultInjector { checkpoint ->
                    if (checkpoint == BackendDeliveryWorkerFaultCheckpoint.AFTER_PROVIDER_OBSERVATION_DURABLE) {
                        refreshCheckpoint.compareAndSet(null, readCheckpointIdentity(fixture, key))
                    }
                    if (checkpoint == BackendDeliveryWorkerFaultCheckpoint.AFTER_PROVIDER_RESULT_COMMIT) {
                        error("crash-after-refresh-auth-commit")
                    }
                }
            )
            assertFails {
                BackendNotificationDeliveryRecoveryScheduler(crashAfterRefreshAck, "refresh-stage-worker").use {
                    it.startAndDrainDueWork()
                }
            }
            assertEquals(1, providerCalls.get())
            assertEquals(0, credential.refreshCalls.get())
            val auth = assertNotNull(crashAfterRefreshAck.current(key))
            assertEquals(BackendDurableDeliveryState.AUTH, auth.state)
            assertTrue(auth.refreshUsed)
            val activeLease = assertNotNull(auth.lease)
            val refreshIdentity = assertNotNull(refreshCheckpoint.get())
            assertEquals("refresh-stage-worker", activeLease.holderId)
            assertTrue(activeLease.expiresAtLogicalEpochSeconds > auth.nowEpochSeconds)

            val crashAfterBlockStage = composition(
                fixture,
                credential,
                BackendDeliveryProviderPort {
                    providerCalls.incrementAndGet()
                    error("a failed refresh cannot perform a second provider send")
                },
                BackendDeliveryWorkerFaultInjector { checkpoint ->
                    if (checkpoint == BackendDeliveryWorkerFaultCheckpoint.AFTER_PROVIDER_OBSERVATION_DURABLE) {
                        error("crash-after-refresh-failure-block-checkpoint")
                    }
                }
            )
            assertFails {
                BackendNotificationDeliveryRecoveryScheduler(crashAfterBlockStage, "refresh-stage-worker").use {
                    it.startAndDrainDueWork()
                }
            }
            assertEquals(1, credential.refreshCalls.get())
            assertEquals(1, providerCalls.get())

            assertBlockedCheckpointThenCommit(fixture, key, refreshIdentity, activeLease)
        }
    }

    private suspend fun assertBlockedCheckpointThenCommit(
        fixture: BackendNotificationDurabilityTestFixture,
        key: DeliveryKey,
        previousCheckpoint: CheckpointIdentity? = null,
        expectedLease: BackendDeliveryLease? = null
    ) {
        lateinit var exact: BackendDeliveryCheckpointReference
        fixture.deliveryFactory.openDeliveryRuntime().use { runtime ->
            val staged = assertNotNull(runtime.current(key))
            assertEquals(BackendDurableDeliveryState.AWAITING_PROVIDER_RESULT_PERSISTENCE, staged.state)
            val checkpoint = assertNotNull(staged.pendingCheckpoint)
            assertEquals(BackendDurableProviderOutcome.PROVIDER_AUTH_BLOCKED, checkpoint.outcome)
            assertEquals(BackendPersistedProviderReason.PROVIDER_AUTH_REJECTED, checkpoint.reason)
            assertFalse(checkpoint.effectRequested)
            assertNotNull(checkpoint.leaseHolderId)
            assertNotNull(checkpoint.leaseVersion)
            assertNotNull(checkpoint.leaseFencingToken)
            previousCheckpoint?.let { previous ->
                assertTrue(checkpoint.revision > previous.revision)
                assertTrue(checkpoint.effectId != previous.effectId)
            }
            expectedLease?.let { lease ->
                assertEquals(lease.holderId, checkpoint.leaseHolderId)
                assertEquals(lease.version, checkpoint.leaseVersion)
                assertEquals(lease.fencingToken, checkpoint.leaseFencingToken)
            }
            exact = staged.requireProviderReference()
            assertEquals(staged, runtime.acknowledgeProviderCheckpoint(exact), "ACK before request is inert")
            assertTrue(runtime.requestProviderCheckpoint(exact))
        }
        fixture.deliveryFactory.openDeliveryRuntime().use { restored ->
            val requested = assertNotNull(restored.current(key))
            assertTrue(requested.pendingCheckpoint?.effectRequested == true)
            val stale = exact.copy(checkpointRevision = exact.checkpointRevision - 1)
            val foreign = exact.copy(leaseHolderId = "foreign-block-worker")
            assertEquals(requested, restored.acknowledgeProviderCheckpoint(stale))
            assertEquals(requested, restored.acknowledgeProviderCheckpoint(foreign))
            val blocked = restored.acknowledgeProviderCheckpoint(exact)
            assertEquals(BackendDurableDeliveryState.PROVIDER_AUTH_BLOCKED, blocked.state)
            assertNull(blocked.pendingCheckpoint)
            assertEquals(blocked, restored.acknowledgeProviderCheckpoint(exact))
        }
    }

    private fun composition(
        fixture: BackendNotificationDurabilityTestFixture,
        credential: RecordingCredential,
        provider: BackendDeliveryProviderPort,
        fault: BackendDeliveryWorkerFaultInjector = BackendDeliveryWorkerFaultInjector { }
    ) = BackendNotificationDeliveryWorkerComposition(
        deliveryStoreFactory = fixture.deliveryFactory,
        registrationStoreFactory = fixture.registrationFactory,
        authority = BackendDeliveryAuthority.OUTBOX_V2,
        clock = BackendDeliveryWorkerClock { 100 },
        policy = BackendDeliveryPolicyPort { BackendDeliveryPolicyDecision.ALLOW },
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
        identity: String
    ): DeliveryKey = BackendNotificationIngestionService(
        fixture.deliveryFactory,
        BackendNotificationIngestionFaultInjector { },
        BackendNotificationIngestionCommittedPort { }
    ).ingest(
        BackendNotificationIngestionCommand(
            "credential-circuit-$identity", "DATE_CONFIRMED", 1,
            "logical-credential-circuit-$identity",
            listOf(
                BackendNotificationRecipientIntent(
                    "participant-$identity", "push", "apns",
                    listOf(registration.registrationId), 1_000
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

    private fun readCheckpointIdentity(
        fixture: BackendNotificationDurabilityTestFixture,
        key: DeliveryKey
    ): CheckpointIdentity = DriverManager.getConnection("jdbc:sqlite:${fixture.databasePath}").use { connection ->
        connection.prepareStatement(
            "SELECT effect_id, checkpoint_revision FROM notification_delivery_provider_checkpoint " +
                "WHERE delivery_key = ?"
        ).use { statement ->
            statement.setString(1, key.value)
            statement.executeQuery().use { rows ->
                assertTrue(rows.next())
                CheckpointIdentity(rows.getString("effect_id"), rows.getLong("checkpoint_revision"))
            }
        }
    }

    private data class CheckpointIdentity(val effectId: String, val revision: Long)

    private class RecordingCredential(
        private val refreshResult: Boolean
    ) : BackendDeliveryCredentialPort, BackendValidatedDeliveryCredentialPort {
        val refreshCalls = AtomicInteger()

        override suspend fun credentialVersion(): String = "credential-v1"

        override suspend fun refreshAfterProviderRejection(expectedVersion: String): Boolean =
            error("refresh must carry its durable provider correlation")

        override suspend fun refreshAfterProviderRejection(
            expectedVersion: String,
            idempotencyKey: String
        ): Boolean {
            assertEquals("credential-v1", expectedVersion)
            assertTrue(idempotencyKey.isNotBlank())
            refreshCalls.incrementAndGet()
            return refreshResult
        }

        override suspend fun validatedCredential() =
            BackendValidatedDeliveryCredential("credential-v1", "credential-fingerprint-v1")
    }
}
