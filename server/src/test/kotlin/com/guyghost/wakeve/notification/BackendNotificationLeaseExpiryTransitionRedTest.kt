package com.guyghost.wakeve.notification

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BackendNotificationLeaseExpiryTransitionRedTest {
    @Test
    fun `retry reaching business expiry stages a holderless target expiry checkpoint under a live resolver lease`() =
        runBlocking {
            BackendNotificationDurabilityTestFixture().use { fixture ->
                val command = BackendNotificationIngestionCommand(
                    domainEventId = "target-retry-would-reach-expiry",
                    effectType = "DATE_CONFIRMED",
                    schemaVersion = 1,
                    logicalNotificationId = "logical-target-retry-would-reach-expiry",
                    recipients = listOf(
                        BackendNotificationRecipientIntent(
                            participantId = "participant-target-retry-would-reach-expiry",
                            channel = "push",
                            provider = "apns",
                            registrationIds = emptyList(),
                            expiresAtEpochSeconds = 101
                        )
                    )
                )
                val receipt = ingestionService(fixture).ingest(command)
                val recipientKey = BackendCanonicalNotificationIdentity.recipientKey(
                    receipt.effectKey,
                    command.recipients.single().participantId,
                    command.recipients.single().channel
                )

                fixture.deliveryFactory.openRecipientTargetRuntime().use { runtime ->
                    val initial = assertNotNull(runtime.current(recipientKey))
                    assertEquals(100, initial.nowEpochSeconds)
                    val lease = assertNotNull(
                        runtime.acquireLease(
                            BackendRecipientTargetLeaseRequest(
                                recipientKey = recipientKey,
                                expectedCheckpointRevision = initial.checkpointRevision,
                                holderId = "live-target-resolver",
                                expectedLeaseVersion = initial.lastLeaseVersion,
                                newLeaseVersion = initial.lastLeaseVersion + 1,
                                fencingToken = initial.lastFencingToken + 1,
                                expiresAtLogicalEpochSeconds = 130
                            )
                        )
                    )
                    assertTrue(lease.expiresAtLogicalEpochSeconds > initial.nowEpochSeconds)

                    val staged = runtime.stageResolution(
                        BackendRecipientTargetResolutionRequest(
                            recipientKey = recipientKey,
                            registrationIds = emptyList(),
                            jitterSample = 1.0,
                            holderId = lease.holderId,
                            leaseVersion = lease.version,
                            fencingToken = lease.fencingToken
                        )
                    )
                    val checkpoint = assertNotNull(staged.pendingCheckpoint)
                    assertEquals(BackendRecipientTargetCheckpointKind.EXPIRY, checkpoint.kind)
                    assertNull(checkpoint.nextAttemptAtEpochSeconds)
                    assertNull(checkpoint.holderId, "retryWouldReachExpiry is no longer resolver-owned")
                    assertNull(checkpoint.leaseVersion)
                    assertNull(checkpoint.fencingToken)

                    val atBusinessExpiry = runtime.advanceLogicalClock(
                        recipientKey,
                        staged.clockRevision,
                        command.recipients.single().expiresAtEpochSeconds
                    )
                    assertEquals(101, atBusinessExpiry.nowEpochSeconds)
                    val reference = atBusinessExpiry.requireTargetReference()
                    assertTrue(runtime.requestCheckpoint(reference))
                    val expired = runtime.acknowledgeCheckpoint(reference)
                    assertEquals(BackendRecipientTargetState.TARGET_EXPIRED, expired.state)
                    assertNull(expired.pendingCheckpoint)
                }
            }
        }

    @Test
    fun `raw provider observation with an exact but expired sending lease is strictly inert`() = runBlocking {
        BackendNotificationDurabilityTestFixture().use { fixture ->
            val registration = fixture.register("stale-provider-observation")
            val key = ingestionService(fixture).ingest(
                BackendNotificationIngestionCommand(
                    domainEventId = "stale-provider-observation",
                    effectType = "DATE_CONFIRMED",
                    schemaVersion = 1,
                    logicalNotificationId = "logical-stale-provider-observation",
                    recipients = listOf(
                        BackendNotificationRecipientIntent(
                            participantId = "participant-stale-provider-observation",
                            channel = "push",
                            provider = "apns",
                            registrationIds = listOf(registration.registrationId),
                            expiresAtEpochSeconds = 1_000
                        )
                    )
                )
            ).deliveryKeys.single()

            val exactReference = fixture.deliveryFactory.openDeliveryRuntime().use { runtime ->
                val policy = assertNotNull(runtime.current(key))
                runtime.markPolicyAllowed(
                    key,
                    policy.checkpointRevision,
                    policy.authority,
                    policy.authorityFencingToken
                )
                val lease = assertNotNull(runtime.acquireLease(key, "provider-worker"))
                val auth = assertNotNull(runtime.current(key))
                val sending = runtime.markProviderAuthReady(
                    key,
                    assertNotNull(auth.correlationId),
                    auth.attempt,
                    lease.holderId,
                    lease.version,
                    lease.fencingToken
                )
                assertEquals(BackendDurableDeliveryState.SENDING, sending.state)
                BackendDeliveryProviderRequest(
                    deliveryKey = key,
                    registrationId = registration.registrationId,
                    apnsId = "stale-provider-apns-id",
                    correlationId = assertNotNull(sending.correlationId),
                    attempt = sending.attempt,
                    leaseHolderId = lease.holderId,
                    leaseVersion = lease.version,
                    leaseFencingToken = lease.fencingToken
                )
            }

            val atLeaseExpiry = fixture.deliveryFactory.openDeliveryRuntime().use { runtime ->
                val sending = assertNotNull(runtime.current(key))
                val lease = assertNotNull(sending.lease)
                runtime.advanceLogicalClock(
                    key,
                    sending.clockRevision,
                    lease.expiresAtLogicalEpochSeconds
                )
            }
            assertEquals(BackendDurableDeliveryState.SENDING, atLeaseExpiry.state)
            assertTrue(atLeaseExpiry.nowEpochSeconds < atLeaseExpiry.expiresAtEpochSeconds)
            assertEquals(atLeaseExpiry.lease?.expiresAtLogicalEpochSeconds, atLeaseExpiry.nowEpochSeconds)

            val worker = deliveryComposition(fixture)
            val observed = worker.handleProviderObservation(
                BackendRawProviderObservationCommand(
                    reference = exactReference,
                    observation = BackendProviderRawObservation.Http(
                        statusCode = 200,
                        rawReason = null,
                        retryAfterEpochSeconds = null,
                        providerRequestId = "stale-provider-result",
                        observedAtEpochSeconds = atLeaseExpiry.nowEpochSeconds
                    )
                )
            )
            assertEquals(atLeaseExpiry, observed, "expired delivery authority cannot classify or stage an observation")
            val after = assertNotNull(worker.current(key))
            assertEquals(atLeaseExpiry, after)
            assertEquals(BackendDurableDeliveryState.SENDING, after.state)
            assertEquals(atLeaseExpiry.correlationId, after.correlationId)
            assertEquals(atLeaseExpiry.checkpointRevision, after.checkpointRevision)
            assertNull(after.pendingCheckpoint)
            assertEquals(atLeaseExpiry.providerCheckpointCount, after.providerCheckpointCount)
        }
    }

    private fun ingestionService(fixture: BackendNotificationDurabilityTestFixture) =
        BackendNotificationIngestionService(
            fixture.deliveryFactory,
            BackendNotificationIngestionFaultInjector { },
            BackendNotificationIngestionCommittedPort { }
        )

    private fun deliveryComposition(fixture: BackendNotificationDurabilityTestFixture) =
        BackendNotificationDeliveryWorkerComposition(
            deliveryStoreFactory = fixture.deliveryFactory,
            registrationStoreFactory = fixture.registrationFactory,
            authority = BackendDeliveryAuthority.OUTBOX_V2,
            clock = BackendDeliveryWorkerClock { 100 },
            policy = BackendDeliveryPolicyPort { BackendDeliveryPolicyDecision.ALLOW },
            tokenAvailability = BackendDeliveryTokenAvailabilityPort { true },
            credentials = object : BackendDeliveryCredentialPort {
                override suspend fun credentialVersion() = "credential-v1"
                override suspend fun refreshAfterProviderRejection(expectedVersion: String) = true
            },
            provider = BackendDeliveryProviderPort { error("direct raw observation must not invoke provider I/O") },
            jitter = BackendDeliveryJitterSource { _, _ -> 0.5 },
            faultInjector = BackendDeliveryWorkerFaultInjector { }
        )

    private fun BackendRecipientTargetSnapshot.requireTargetReference(): BackendRecipientTargetCheckpointReference {
        val checkpoint = assertNotNull(pendingCheckpoint)
        return BackendRecipientTargetCheckpointReference(
            recipientKey,
            checkpoint.effectId,
            checkpoint.revision,
            checkpoint.transactionReceiptId,
            checkpoint.holderId,
            checkpoint.leaseVersion,
            checkpoint.fencingToken
        )
    }
}
