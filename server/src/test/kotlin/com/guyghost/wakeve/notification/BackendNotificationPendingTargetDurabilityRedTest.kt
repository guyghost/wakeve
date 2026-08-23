package com.guyghost.wakeve.notification

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BackendNotificationPendingTargetDurabilityRedTest {
    @Test
    fun `pending target retry survives reopen then exact receipt fenced ACK freezes one delivery per registration`() =
        runBlocking {
            BackendNotificationDurabilityTestFixture().use { fixture ->
                val recipientKey = RecipientKey("pending-target-retry")
                seedPendingRecipient(fixture, recipientKey, expiresAt = 1_000)
                fixture.deliveryFactory.openRecipientTargetRuntime(noTargetFault()).use { runtime ->
                    val initial = assertNotNull(runtime.current(recipientKey))
                    val lease = assertNotNull(runtime.acquireLease(leaseRequest(initial, "resolver-a", 1, 11, 150)))
                    val stagedRetry = runtime.stageResolution(
                        BackendRecipientTargetResolutionRequest(
                            recipientKey = recipientKey,
                            registrationIds = emptyList(),
                            jitterSample = 1.0,
                            holderId = lease.holderId,
                            leaseVersion = lease.version,
                            fencingToken = lease.fencingToken
                        )
                    )
                    assertEquals(BackendRecipientTargetCheckpointKind.RETRY, stagedRetry.pendingCheckpoint?.kind)
                    val retryReference = stagedRetry.requireReference()
                    assertTrue(runtime.requestCheckpoint(retryReference))
                    val retryCommitted = runtime.acknowledgeCheckpoint(retryReference)
                    assertEquals(BackendRecipientTargetState.PENDING_TARGET, retryCommitted.state)
                    assertEquals(1L, retryCommitted.attempt)
                    assertEquals(101L, retryCommitted.nextAttemptAtEpochSeconds)
                }

                val phone = fixture.register("pending-phone")
                val tablet = fixture.register("pending-tablet")
                fixture.deliveryFactory.openRecipientTargetRuntime(noTargetFault()).use { restored ->
                    val reopened = assertNotNull(restored.current(recipientKey))
                    assertEquals(1L, reopened.attempt)
                    assertEquals(101L, reopened.nextAttemptAtEpochSeconds)
                    val due = restored.advanceLogicalClock(
                        recipientKey = recipientKey,
                        expectedClockRevision = reopened.clockRevision,
                        newEpochSeconds = 101
                    )
                    val lease = assertNotNull(restored.acquireLease(leaseRequest(due, "resolver-b", 2, 12, 151)))
                    val stagedFanout = restored.stageResolution(
                        BackendRecipientTargetResolutionRequest(
                            recipientKey = recipientKey,
                            registrationIds = listOf(tablet.registrationId, phone.registrationId, phone.registrationId),
                            jitterSample = 0.5,
                            holderId = lease.holderId,
                            leaseVersion = lease.version,
                            fencingToken = lease.fencingToken
                        )
                    )
                    assertEquals(BackendRecipientTargetCheckpointKind.FANOUT, stagedFanout.pendingCheckpoint?.kind)
                    assertEquals(
                        listOf(phone.registrationId, tablet.registrationId).sorted(),
                        stagedFanout.pendingCheckpoint?.deliveries?.map { it.registrationId }
                    )
                    assertEquals(0, stagedFanout.deliveryKeys.size, "Staging and XState restore must not fan out implicitly.")
                    val reference = stagedFanout.requireReference()
                    val staleReceipt = reference.copy(transactionReceiptId = "foreign-receipt")
                    assertEquals(stagedFanout, restored.acknowledgeCheckpoint(staleReceipt))
                    assertTrue(restored.requestCheckpoint(reference))
                    val committed = restored.acknowledgeCheckpoint(reference)
                    assertEquals(BackendRecipientTargetState.TARGETED, committed.state)
                    assertEquals(2, committed.deliveryKeys.size)
                    assertEquals(committed, restored.acknowledgeCheckpoint(reference), "A double ACK is inert.")
                }

                fixture.deliveryFactory.openRecipientTargetRuntime(noTargetFault()).use { afterRestart ->
                    val frozen = assertNotNull(afterRestart.current(recipientKey))
                    assertEquals(BackendRecipientTargetState.TARGETED, frozen.state)
                    assertEquals(2, frozen.deliveryKeys.size)
                    val later = fixture.register("pending-later")
                    val ignored = afterRestart.stageResolution(
                        BackendRecipientTargetResolutionRequest(
                            recipientKey = recipientKey,
                            registrationIds = listOf(later.registrationId),
                            jitterSample = 0.5,
                            holderId = "foreign",
                            leaseVersion = 99,
                            fencingToken = 99
                        )
                    )
                    assertEquals(frozen, ignored, "A targeted recipient never appends or retargets after its exact ACK.")
                }
            }
        }

    @Test
    fun `two restored resolvers have one lease winner and stale or foreign ACKs cannot fan out`() = runBlocking {
        BackendNotificationDurabilityTestFixture().use { fixture ->
            val recipientKey = RecipientKey("pending-target-contended")
            seedPendingRecipient(fixture, recipientKey, expiresAt = 1_000)
            val registration = fixture.register("contended-phone")
            val startGate = BackendNotificationConcurrentStartGate()
            fixture.deliveryFactory.openRecipientTargetRuntime(noTargetFault()).use { first ->
                fixture.deliveryFactory.openRecipientTargetRuntime(noTargetFault()).use { second ->
                    val snapshot = assertNotNull(first.current(recipientKey))
                    val leases = withTimeout(5_000) {
                        coroutineScope {
                            val claims = listOf(
                                async(Dispatchers.IO) {
                                    startGate.awaitReleaseBeforePortCall()
                                    first.acquireLease(leaseRequest(snapshot, "resolver-a", 1, 21, 150))
                                },
                                async(Dispatchers.IO) {
                                    startGate.awaitReleaseBeforePortCall()
                                    second.acquireLease(leaseRequest(snapshot, "resolver-b", 1, 22, 150))
                                }
                            )
                            startGate.releaseTogether()
                            claims.awaitAll()
                        }
                    }
                    val winnerIndex = leases.indexOfFirst { it != null }
                    assertTrue(winnerIndex >= 0)
                    assertEquals(1, leases.count { it != null })
                    val winner = assertNotNull(leases[winnerIndex])
                    val loser = leases[1 - winnerIndex]
                    assertNotNull(winner)
                    assertNull(loser)
                    assertTrue(winner.version > snapshot.lastLeaseVersion)
                    assertTrue(winner.fencingToken > 0)
                    val winningRuntime = if (winnerIndex == 0) first else second
                    val losingRuntime = if (winnerIndex == 0) second else first
                    val staged = winningRuntime.stageResolution(
                        BackendRecipientTargetResolutionRequest(
                            recipientKey,
                            listOf(registration.registrationId),
                            0.5,
                            winner.holderId,
                            winner.version,
                            winner.fencingToken
                        )
                    )
                    val exact = staged.requireReference()
                    assertTrue(winningRuntime.requestCheckpoint(exact))
                    val foreign = exact.copy(holderId = "foreign-resolver")
                    assertEquals(
                        staged.copy(pendingCheckpoint = staged.pendingCheckpoint?.copy(effectRequested = true)),
                        losingRuntime.acknowledgeCheckpoint(foreign)
                    )
                    assertEquals(0, staged.deliveryKeys.size)
                    val committed = winningRuntime.acknowledgeCheckpoint(exact)
                    assertEquals(1, committed.deliveryKeys.size)
                    val durable = assertNotNull(losingRuntime.current(recipientKey))
                    assertEquals(winner.version, durable.lastLeaseVersion)
                    assertEquals(winner.fencingToken, durable.lastFencingToken)
                    assertEquals(committed.deliveryKeys, durable.deliveryKeys)
                }
            }
        }
    }

    @Test
    fun `fanout transaction recovers from crashes immediately before and after commit without duplicates`() =
        runBlocking {
            listOf(
                BackendRecipientTargetFaultCheckpoint.BEFORE_FANOUT_COMMIT,
                BackendRecipientTargetFaultCheckpoint.AFTER_FANOUT_COMMIT
            ).forEach { crashAt ->
                BackendNotificationDurabilityTestFixture().use { fixture ->
                    val recipientKey = RecipientKey("fanout-crash-${crashAt.name.lowercase()}")
                    seedPendingRecipient(fixture, recipientKey, expiresAt = 1_000)
                    val registration = fixture.register("fanout-crash-phone-${crashAt.name.lowercase()}")
                    lateinit var reference: BackendRecipientTargetCheckpointReference
                    fixture.deliveryFactory.openRecipientTargetRuntime(
                        BackendRecipientTargetFaultInjector { checkpoint ->
                            if (checkpoint == crashAt) error("crash-${checkpoint.name}")
                        }
                    ).use { crashing ->
                        val initial = assertNotNull(crashing.current(recipientKey))
                        val lease = assertNotNull(crashing.acquireLease(leaseRequest(initial, "resolver", 1, 31, 150)))
                        val staged = crashing.stageResolution(
                            BackendRecipientTargetResolutionRequest(
                                recipientKey,
                                listOf(registration.registrationId),
                                0.5,
                                lease.holderId,
                                lease.version,
                                lease.fencingToken
                            )
                        )
                        reference = staged.requireReference()
                        assertTrue(crashing.requestCheckpoint(reference))
                        assertFails { crashing.acknowledgeCheckpoint(reference) }
                    }

                    fixture.deliveryFactory.openRecipientTargetRuntime(noTargetFault()).use { restored ->
                        val snapshot = assertNotNull(restored.current(recipientKey))
                        if (crashAt == BackendRecipientTargetFaultCheckpoint.BEFORE_FANOUT_COMMIT) {
                            assertEquals(BackendRecipientTargetState.PENDING_TARGET, snapshot.state)
                            assertEquals(0, snapshot.deliveryKeys.size)
                            val committed = restored.acknowledgeCheckpoint(reference)
                            assertEquals(BackendRecipientTargetState.TARGETED, committed.state)
                        } else {
                            assertEquals(BackendRecipientTargetState.TARGETED, snapshot.state)
                            assertEquals(1, snapshot.deliveryKeys.size)
                            assertEquals(snapshot, restored.acknowledgeCheckpoint(reference))
                        }
                        assertEquals(1, restored.current(recipientKey)?.deliveryKeys?.size)
                    }
                }
            }
        }

    @Test
    fun `durable target expiry is terminal and a later registration cannot resurrect it`() = runBlocking {
        BackendNotificationDurabilityTestFixture().use { fixture ->
            val recipientKey = RecipientKey("pending-target-expiry")
            seedPendingRecipient(fixture, recipientKey, expiresAt = 105)
            fixture.deliveryFactory.openRecipientTargetRuntime(noTargetFault()).use { runtime ->
                val initial = assertNotNull(runtime.current(recipientKey))
                runtime.advanceLogicalClock(recipientKey, initial.clockRevision, 105)
                val staged = runtime.stageExpiry(recipientKey)
                assertEquals(BackendRecipientTargetCheckpointKind.EXPIRY, staged.pendingCheckpoint?.kind)
                val reference = staged.requireReference()
                assertTrue(runtime.requestCheckpoint(reference))
                val expired = runtime.acknowledgeCheckpoint(reference)
                assertEquals(BackendRecipientTargetState.TARGET_EXPIRED, expired.state)
            }
            val lateRegistration = fixture.register("late-target-phone")
            fixture.deliveryFactory.openRecipientTargetRuntime(noTargetFault()).use { restored ->
                val terminal = assertNotNull(restored.current(recipientKey))
                val ignored = restored.stageResolution(
                    BackendRecipientTargetResolutionRequest(
                        recipientKey,
                        listOf(lateRegistration.registrationId),
                        0.5,
                        "late-resolver",
                        99,
                        99
                    )
                )
                assertEquals(terminal, ignored)
                assertEquals(BackendRecipientTargetState.TARGET_EXPIRED, ignored.state)
                assertTrue(ignored.deliveryKeys.isEmpty())
            }
        }
    }

    private suspend fun seedPendingRecipient(
        fixture: BackendNotificationDurabilityTestFixture,
        recipientKey: RecipientKey,
        expiresAt: Long
    ) {
        fixture.deliveryFactory.open().use { store ->
            store.persistPendingRecipient(
                BackendNotificationRecipient(
                    recipientKey = recipientKey,
                    effectKey = EffectKey("effect-${recipientKey.value}"),
                    status = BackendRecipientStatus.PENDING_TARGET,
                    registrationIds = emptySet(),
                    expiresAtEpochSeconds = expiresAt
                )
            )
        }
    }

    private fun leaseRequest(
        snapshot: BackendRecipientTargetSnapshot,
        holderId: String,
        leaseVersion: Long,
        fencingToken: Long,
        expiresAt: Long
    ) = BackendRecipientTargetLeaseRequest(
        recipientKey = snapshot.recipientKey,
        expectedCheckpointRevision = snapshot.checkpointRevision,
        holderId = holderId,
        expectedLeaseVersion = snapshot.lastLeaseVersion,
        newLeaseVersion = leaseVersion,
        fencingToken = fencingToken,
        expiresAtLogicalEpochSeconds = expiresAt
    )

    private fun BackendRecipientTargetSnapshot.requireReference(): BackendRecipientTargetCheckpointReference {
        val checkpoint = assertNotNull(pendingCheckpoint)
        return BackendRecipientTargetCheckpointReference(
            recipientKey = recipientKey,
            effectId = checkpoint.effectId,
            checkpointRevision = checkpoint.revision,
            transactionReceiptId = checkpoint.transactionReceiptId,
            holderId = checkpoint.holderId,
            leaseVersion = checkpoint.leaseVersion,
            fencingToken = checkpoint.fencingToken
        )
    }

    private fun noTargetFault() = BackendRecipientTargetFaultInjector { }
}
