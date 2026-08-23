package com.guyghost.wakeve.notification

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BackendNotificationExpiredCheckpointPreRecoveryRedTest {
    @Test
    fun `provider unrequested checkpoint rejects old request and ACK after lease expiry`() = runBlocking {
        assertExpiredProviderCheckpointRequiresRecovery(requestBeforeExpiry = false)
    }

    @Test
    fun `provider previously requested checkpoint rejects old request and ACK after lease expiry`() = runBlocking {
        assertExpiredProviderCheckpointRequiresRecovery(requestBeforeExpiry = true)
    }

    private suspend fun assertExpiredProviderCheckpointRequiresRecovery(requestBeforeExpiry: Boolean) {
        BackendNotificationDurabilityTestFixture().use { fixture ->
                val registration = fixture.register("provider-pre-recovery-$requestBeforeExpiry")
                val key = ingestDelivery(fixture, registration, "provider-pre-recovery-$requestBeforeExpiry")
                val crashing = deliveryComposition(
                    fixture,
                    BackendDeliveryProviderPort {
                        BackendProviderRawObservation.Http(200, null, null, "apns-pre-recovery", 110)
                    },
                    BackendDeliveryWorkerFaultInjector { checkpoint ->
                        if (checkpoint == BackendDeliveryWorkerFaultCheckpoint.AFTER_PROVIDER_OBSERVATION_DURABLE) {
                            error("crash-after-provider-checkpoint")
                        }
                    }
                )
                assertFails {
                    BackendNotificationDeliveryRecoveryScheduler(crashing, "provider-old").use {
                        it.startAndDrainDueWork()
                    }
                }

                fixture.deliveryFactory.openDeliveryRuntime().use { runtime ->
                    val staged = assertNotNull(runtime.current(key))
                    val oldLease = assertNotNull(staged.lease)
                    val oldCheckpoint = assertNotNull(staged.pendingCheckpoint)
                    val oldReference = staged.requireProviderReference()
                    if (requestBeforeExpiry) assertTrue(runtime.requestProviderCheckpoint(oldReference))
                    val beforeExpiry = assertNotNull(runtime.current(key))
                    assertEquals(requestBeforeExpiry, beforeExpiry.pendingCheckpoint?.effectRequested)

                    val afterExpiry = runtime.advanceLogicalClock(
                        key,
                        beforeExpiry.clockRevision,
                        oldLease.expiresAtLogicalEpochSeconds
                    )
                    assertTrue(afterExpiry.nowEpochSeconds < afterExpiry.expiresAtEpochSeconds)
                    assertEquals(oldCheckpoint.effectId, afterExpiry.pendingCheckpoint?.effectId)
                    if (requestBeforeExpiry) {
                        assertEquals(
                            afterExpiry,
                            runtime.acknowledgeProviderCheckpoint(oldReference),
                            "an expired holder cannot ACK its previously requested effect"
                        )
                        assertEquals(afterExpiry, runtime.current(key), "expired ACK committed without recovery authority")
                    }
                    assertFalse(
                        runtime.requestProviderCheckpoint(oldReference),
                        "an expired holder cannot newly request or re-request its old effect"
                    )
                    assertEquals(afterExpiry, runtime.current(key), "expired request mutated the checkpoint")
                    if (!requestBeforeExpiry) {
                        assertEquals(afterExpiry, runtime.acknowledgeProviderCheckpoint(oldReference))
                        assertEquals(afterExpiry, runtime.current(key), "expired ACK committed without recovery authority")
                    }

                    val recoveredLease = assertNotNull(runtime.acquireLease(key, "provider-new"))
                    val recovered = assertNotNull(runtime.current(key))
                    val recoveredCheckpoint = assertNotNull(recovered.pendingCheckpoint)
                    assertTrue(recoveredLease.version > oldLease.version)
                    assertTrue(recoveredLease.fencingToken > oldLease.fencingToken)
                    assertTrue(recoveredCheckpoint.revision > oldCheckpoint.revision)
                    assertNotEquals(oldCheckpoint.effectId, recoveredCheckpoint.effectId)
                    assertEquals(recoveredLease.holderId, recoveredCheckpoint.leaseHolderId)
                    assertEquals(recoveredLease.version, recoveredCheckpoint.leaseVersion)
                    assertEquals(recoveredLease.fencingToken, recoveredCheckpoint.leaseFencingToken)
                    assertFalse(recoveredCheckpoint.effectRequested)

                    val recoveredReference = recovered.requireProviderReference()
                    assertTrue(runtime.requestProviderCheckpoint(recoveredReference))
                    val accepted = runtime.acknowledgeProviderCheckpoint(recoveredReference)
                    assertEquals(BackendDurableDeliveryState.ACCEPTED_BY_APNS, accepted.state)
                    assertNull(accepted.pendingCheckpoint)
                }
        }
    }

    @Test
    fun `fanout unrequested checkpoint rejects old request and ACK after lease expiry`() = runBlocking {
        assertExpiredTargetCheckpointRequiresRecovery(requestBeforeExpiry = false)
    }

    @Test
    fun `fanout previously requested checkpoint rejects old request and ACK after lease expiry`() = runBlocking {
        assertExpiredTargetCheckpointRequiresRecovery(requestBeforeExpiry = true)
    }

    private suspend fun assertExpiredTargetCheckpointRequiresRecovery(requestBeforeExpiry: Boolean) {
        BackendNotificationDurabilityTestFixture().use { fixture ->
                val recipientKey = ingestPendingTarget(fixture, "target-pre-recovery-$requestBeforeExpiry", 1_000)
                val registration = fixture.register("target-pre-recovery-$requestBeforeExpiry")
                fixture.deliveryFactory.openRecipientTargetRuntime().use { runtime ->
                    val initial = assertNotNull(runtime.current(recipientKey))
                    val oldLease = assertNotNull(
                        runtime.acquireLease(
                            BackendRecipientTargetLeaseRequest(
                                recipientKey,
                                initial.checkpointRevision,
                                "target-old",
                                initial.lastLeaseVersion,
                                initial.lastLeaseVersion + 1,
                                initial.lastFencingToken + 1,
                                130
                            )
                        )
                    )
                    val staged = runtime.stageResolution(
                        BackendRecipientTargetResolutionRequest(
                            recipientKey,
                            listOf(registration.registrationId),
                            0.5,
                            oldLease.holderId,
                            oldLease.version,
                            oldLease.fencingToken
                        )
                    )
                    val oldCheckpoint = assertNotNull(staged.pendingCheckpoint)
                    val oldReference = staged.requireTargetReference()
                    if (requestBeforeExpiry) assertTrue(runtime.requestCheckpoint(oldReference))
                    val beforeExpiry = assertNotNull(runtime.current(recipientKey))
                    assertEquals(requestBeforeExpiry, beforeExpiry.pendingCheckpoint?.effectRequested)

                    val afterExpiry = runtime.advanceLogicalClock(
                        recipientKey,
                        beforeExpiry.clockRevision,
                        oldLease.expiresAtLogicalEpochSeconds
                    )
                    assertTrue(afterExpiry.nowEpochSeconds < 1_000)
                    if (requestBeforeExpiry) {
                        assertEquals(
                            afterExpiry,
                            runtime.acknowledgeCheckpoint(oldReference),
                            "an expired resolver cannot ACK its previously requested fanout"
                        )
                        assertEquals(afterExpiry, runtime.current(recipientKey), "expired ACK committed fanout")
                    }
                    assertFalse(
                        runtime.requestCheckpoint(oldReference),
                        "an expired target resolver cannot newly request or re-request its old fanout"
                    )
                    assertEquals(afterExpiry, runtime.current(recipientKey), "expired request mutated target checkpoint")
                    if (!requestBeforeExpiry) {
                        assertEquals(afterExpiry, runtime.acknowledgeCheckpoint(oldReference))
                        assertEquals(afterExpiry, runtime.current(recipientKey), "expired ACK committed fanout")
                    }

                    val recoveredLease = assertNotNull(
                        runtime.acquireLease(
                            BackendRecipientTargetLeaseRequest(
                                recipientKey,
                                afterExpiry.checkpointRevision,
                                "target-new",
                                afterExpiry.lastLeaseVersion,
                                afterExpiry.lastLeaseVersion + 1,
                                afterExpiry.lastFencingToken + 1,
                                160
                            )
                        )
                    )
                    val recovered = assertNotNull(runtime.current(recipientKey))
                    val recoveredCheckpoint = assertNotNull(recovered.pendingCheckpoint)
                    assertTrue(recoveredLease.version > oldLease.version)
                    assertTrue(recoveredLease.fencingToken > oldLease.fencingToken)
                    assertTrue(recoveredCheckpoint.revision > oldCheckpoint.revision)
                    assertNotEquals(oldCheckpoint.effectId, recoveredCheckpoint.effectId)
                    assertEquals(oldCheckpoint.transactionReceiptId, recoveredCheckpoint.transactionReceiptId)
                    assertEquals(oldCheckpoint.deliveries, recoveredCheckpoint.deliveries)
                    assertFalse(recoveredCheckpoint.effectRequested)

                    val recoveredReference = recovered.requireTargetReference()
                    assertTrue(runtime.requestCheckpoint(recoveredReference))
                    val targeted = runtime.acknowledgeCheckpoint(recoveredReference)
                    assertEquals(BackendRecipientTargetState.TARGETED, targeted.state)
                    assertEquals(1, targeted.deliveryKeys.size)
                }
        }
    }

    @Test
    fun `holderless provider expiry checkpoint remains requestable and ACKable at business expiry`() = runBlocking {
        BackendNotificationDurabilityTestFixture().use { fixture ->
            val registration = fixture.register("holderless-provider-expiry")
            val key = ingestDelivery(fixture, registration, "holderless-provider-expiry", expiresAt = 105)
            fixture.deliveryFactory.openDeliveryRuntime().use { runtime ->
                val initial = assertNotNull(runtime.current(key))
                val atExpiry = runtime.advanceLogicalClock(key, initial.clockRevision, 105)
                val staged = runtime.stageExpiry(
                    key,
                    atExpiry.checkpointRevision,
                    atExpiry.authority,
                    atExpiry.authorityFencingToken,
                    null,
                    null,
                    null
                )
                val checkpoint = assertNotNull(staged.pendingCheckpoint)
                assertEquals(BackendDurableProviderOutcome.EXPIRED, checkpoint.outcome)
                assertNull(checkpoint.leaseHolderId)
                assertNull(checkpoint.leaseVersion)
                assertNull(checkpoint.leaseFencingToken)
                val reference = staged.requireProviderReference()
                assertTrue(runtime.requestProviderCheckpoint(reference))
                val expired = runtime.acknowledgeProviderCheckpoint(reference)
                assertEquals(BackendDurableDeliveryState.EXPIRED, expired.state)
                assertNull(expired.pendingCheckpoint)
            }
        }
    }

    @Test
    fun `holderless target expiry checkpoint remains requestable and ACKable at business expiry`() = runBlocking {
        BackendNotificationDurabilityTestFixture().use { fixture ->
            val recipientKey = ingestPendingTarget(fixture, "holderless-target-expiry", 105)
            fixture.deliveryFactory.openRecipientTargetRuntime().use { runtime ->
                val initial = assertNotNull(runtime.current(recipientKey))
                runtime.advanceLogicalClock(recipientKey, initial.clockRevision, 105)
                val staged = runtime.stageExpiry(recipientKey)
                val checkpoint = assertNotNull(staged.pendingCheckpoint)
                assertEquals(BackendRecipientTargetCheckpointKind.EXPIRY, checkpoint.kind)
                assertNull(checkpoint.holderId, "business expiry has no resolver authority holder")
                assertNull(checkpoint.leaseVersion)
                assertNull(checkpoint.fencingToken)
                val reference = staged.requireTargetReference()
                assertTrue(runtime.requestCheckpoint(reference))
                val expired = runtime.acknowledgeCheckpoint(reference)
                assertEquals(BackendRecipientTargetState.TARGET_EXPIRED, expired.state)
                assertNull(expired.pendingCheckpoint)
            }
        }
    }

    private suspend fun ingestDelivery(
        fixture: BackendNotificationDurabilityTestFixture,
        registration: BackendDeviceRegistration,
        identity: String,
        expiresAt: Long = 1_000
    ): DeliveryKey = ingestionService(fixture).ingest(
        BackendNotificationIngestionCommand(
            domainEventId = "expired-checkpoint-$identity",
            effectType = "DATE_CONFIRMED",
            schemaVersion = 1,
            logicalNotificationId = "logical-expired-checkpoint-$identity",
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

    private suspend fun ingestPendingTarget(
        fixture: BackendNotificationDurabilityTestFixture,
        identity: String,
        expiresAt: Long
    ): RecipientKey {
        val command = BackendNotificationIngestionCommand(
            domainEventId = "expired-target-$identity",
            effectType = "DATE_CONFIRMED",
            schemaVersion = 1,
            logicalNotificationId = "logical-expired-target-$identity",
            recipients = listOf(
                BackendNotificationRecipientIntent(
                    participantId = "participant-$identity",
                    channel = "push",
                    provider = "apns",
                    registrationIds = emptyList(),
                    expiresAtEpochSeconds = expiresAt
                )
            )
        )
        val receipt = ingestionService(fixture).ingest(command)
        return BackendCanonicalNotificationIdentity.recipientKey(
            receipt.effectKey,
            command.recipients.single().participantId,
            command.recipients.single().channel
        )
    }

    private fun ingestionService(fixture: BackendNotificationDurabilityTestFixture) =
        BackendNotificationIngestionService(
            fixture.deliveryFactory,
            BackendNotificationIngestionFaultInjector { },
            BackendNotificationIngestionCommittedPort { }
        )

    private fun deliveryComposition(
        fixture: BackendNotificationDurabilityTestFixture,
        provider: BackendDeliveryProviderPort,
        fault: BackendDeliveryWorkerFaultInjector
    ) = BackendNotificationDeliveryWorkerComposition(
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
        provider = provider,
        jitter = BackendDeliveryJitterSource { _, _ -> 0.5 },
        faultInjector = fault
    )

    private fun BackendNotificationDeliverySnapshot.requireProviderReference(): BackendDeliveryCheckpointReference {
        val checkpoint = assertNotNull(pendingCheckpoint)
        return BackendDeliveryCheckpointReference(
            deliveryKey,
            checkpoint.effectId,
            checkpoint.revision,
            authority,
            authorityFencingToken,
            checkpoint.leaseHolderId,
            checkpoint.leaseVersion,
            checkpoint.leaseFencingToken
        )
    }

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
