package com.guyghost.wakeve.notification

import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.deleteIfExists
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * RED contracts for legacy notification registration tasks 1.4, 6.1 and 7.1.
 *
 * These tests intentionally name the durable backend port that is still absent.  They do not
 * emulate the saga in test code: production must persist and restore the reviewed XState model
 * through the existing backend device-registration factory.  A compile failure for the missing
 * types/methods is therefore the expected RED result before implementation.
 */
class LegacyNotificationRegistrationCompatibilitySagaRedTest {
    private val temporaryRoots = mutableListOf<Path>()

    @AfterTest
    fun tearDown() {
        temporaryRoots.forEach { root ->
            Files.walk(root).use { paths ->
                paths.sorted(Comparator.reverseOrder()).forEach { path -> path.deleteIfExists() }
            }
        }
        temporaryRoots.clear()
    }

    @Test
    fun `pending intent is durable before N-1 register exposes legacy then v2 effects`() = runBlocking {
        val factory = fixtureFactory()
        val command = command(
            sagaId = "saga-register-order",
            operation = LegacyCompatibilityOperation.REGISTER,
            clientGeneration = LegacyCompatibilityClientGeneration.N_MINUS_1
        )

        val afterIntent = factory.openCompatibilitySagaStore().use { store ->
            store.persistIntent(command)
        }

        assertEquals(LegacyCompatibilitySagaState.WRITING_LEGACY, afterIntent.state)
        assertEquals(LegacyCompatibilityReconciliationStatus.PENDING, afterIntent.reconciliationStatus)
        assertEquals(
            LegacyCompatibilityResponseDisposition.RECONCILIATION_ACCEPTED,
            afterIntent.responseDisposition
        )
        assertEquals(
            LegacyCompatibilityEffectCheckpoint.WRITE_LEGACY_STORE,
            afterIntent.requiredEffect?.checkpoint
        )

        val restoredIntent = factory.openCompatibilitySagaStore().use { store ->
            assertNotNull(store.findBySagaId(command.sagaId))
        }
        assertEquals(afterIntent, restoredIntent, "The intent and first checkpoint must survive reopen.")

        val afterLegacy = factory.openCompatibilitySagaStore().use { store ->
            store.acknowledgeWriteSucceeded(
                sagaId = command.sagaId,
                reference = assertNotNull(restoredIntent.requiredEffect),
                outcome = LegacyCompatibilityWriteOutcome.APPLIED
            )
        }
        assertEquals(LegacyCompatibilitySagaState.WRITING_V2, afterLegacy.state)
        assertEquals(LegacyCompatibilityWriteStatus.APPLIED, afterLegacy.legacyWriteStatus)
        assertEquals(
            LegacyCompatibilityEffectCheckpoint.WRITE_V2_REGISTRATION_STORE,
            afterLegacy.requiredEffect?.checkpoint
        )

        val afterV2 = factory.openCompatibilitySagaStore().use { store ->
            store.acknowledgeWriteSucceeded(
                sagaId = command.sagaId,
                reference = assertNotNull(afterLegacy.requiredEffect),
                outcome = LegacyCompatibilityWriteOutcome.APPLIED
            )
        }
        assertEquals(LegacyCompatibilitySagaState.RECORDING_CONVERGENCE, afterV2.state)

        val converged = factory.openCompatibilitySagaStore().use { store ->
            store.recordConvergence(
                sagaId = command.sagaId,
                reference = assertNotNull(afterV2.requiredEffect)
            )
        }
        assertEquals(LegacyCompatibilitySagaState.CONVERGED, converged.state)
        assertEquals(
            LegacyCompatibilityResponseDisposition.CONVERGED_SUCCESS,
            converged.responseDisposition
        )
    }

    @Test
    fun `N-1 unregister exposes targeted v2 then legacy and N never exposes a legacy effect`() = runBlocking {
        val factory = fixtureFactory()
        val legacyUnregister = command(
            sagaId = "saga-unregister-order",
            operation = LegacyCompatibilityOperation.UNREGISTER,
            clientGeneration = LegacyCompatibilityClientGeneration.N_MINUS_1,
            tokenFingerprint = null
        )
        val nRegister = command(
            sagaId = "saga-n-register",
            operation = LegacyCompatibilityOperation.REGISTER,
            clientGeneration = LegacyCompatibilityClientGeneration.N,
            legacyPrimaryKeyFingerprint = null,
            legacyInstallationId = null,
            legacyRegistrationId = null,
            targetInstallationId = "installation-n"
        )
        val nUnregister = command(
            sagaId = "saga-n-unregister",
            operation = LegacyCompatibilityOperation.UNREGISTER,
            clientGeneration = LegacyCompatibilityClientGeneration.N,
            legacyPrimaryKeyFingerprint = null,
            legacyInstallationId = null,
            legacyRegistrationId = null,
            targetInstallationId = "installation-n",
            targetRegistrationId = "registration-n",
            tokenFingerprint = null
        )

        val unregisterStart = factory.openCompatibilitySagaStore().use { it.persistIntent(legacyUnregister) }
        assertEquals(LegacyCompatibilityEffectCheckpoint.WRITE_V2_REGISTRATION_STORE, unregisterStart.requiredEffect?.checkpoint)
        assertEquals(LegacyCompatibilityV2TargetKind.LEGACY_DETERMINISTIC_INSTALLATION_ONLY, unregisterStart.v2TargetKind)
        val afterV2 = factory.openCompatibilitySagaStore().use {
            it.acknowledgeWriteSucceeded(
                legacyUnregister.sagaId,
                assertNotNull(unregisterStart.requiredEffect),
                LegacyCompatibilityWriteOutcome.ALREADY_APPLIED
            )
        }
        assertEquals(LegacyCompatibilityEffectCheckpoint.WRITE_LEGACY_STORE, afterV2.requiredEffect?.checkpoint)

        listOf(nRegister, nUnregister).forEach { commandN ->
            val start = factory.openCompatibilitySagaStore().use { it.persistIntent(commandN) }
            assertEquals(LegacyCompatibilityWriteStatus.NOT_REQUIRED, start.legacyWriteStatus)
            assertEquals(LegacyCompatibilityEffectCheckpoint.WRITE_V2_REGISTRATION_STORE, start.requiredEffect?.checkpoint)
            val afterWrite = factory.openCompatibilitySagaStore().use {
                it.acknowledgeWriteSucceeded(
                    commandN.sagaId,
                    assertNotNull(start.requiredEffect),
                    LegacyCompatibilityWriteOutcome.APPLIED
                )
            }
            assertEquals(LegacyCompatibilitySagaState.RECORDING_CONVERGENCE, afterWrite.state)
            assertFalse(
                factory.openCompatibilitySagaStore().use { store ->
                    store.effectHistory(commandN.sagaId)
                }.any { it.checkpoint == LegacyCompatibilityEffectCheckpoint.WRITE_LEGACY_STORE },
                "Generation N must never mutate the lossy legacy user/platform row."
            )
        }
    }

    @Test
    fun `crash restore keeps the exact checkpoint and idempotent effectId after each durable step`() = runBlocking {
        val factory = fixtureFactory()
        val command = command(sagaId = "saga-crash")
        val afterIntent = factory.openCompatibilitySagaStore().use { it.persistIntent(command) }
        val legacyC1 = assertNotNull(afterIntent.requiredEffect)

        val restoredC1 = factory.openCompatibilitySagaStore().use { assertNotNull(it.findBySagaId(command.sagaId)) }
        assertEquals(legacyC1, restoredC1.requiredEffect)

        val afterLegacy = factory.openCompatibilitySagaStore().use {
            it.acknowledgeWriteSucceeded(
                command.sagaId,
                legacyC1,
                LegacyCompatibilityWriteOutcome.APPLIED
            )
        }
        val v2C2 = assertNotNull(afterLegacy.requiredEffect)
        assertNotEquals(legacyC1.effectId, v2C2.effectId)
        val restoredC2 = factory.openCompatibilitySagaStore().use { assertNotNull(it.findBySagaId(command.sagaId)) }
        assertEquals(v2C2, restoredC2.requiredEffect)
        assertEquals(LegacyCompatibilityWriteStatus.APPLIED, restoredC2.legacyWriteStatus)

        val afterV2 = factory.openCompatibilitySagaStore().use {
            it.acknowledgeWriteSucceeded(
                command.sagaId,
                v2C2,
                LegacyCompatibilityWriteOutcome.APPLIED
            )
        }
        val convergenceC3 = assertNotNull(afterV2.requiredEffect)
        val restoredC3 = factory.openCompatibilitySagaStore().use { assertNotNull(it.findBySagaId(command.sagaId)) }
        assertEquals(LegacyCompatibilitySagaState.RECORDING_CONVERGENCE, restoredC3.state)
        assertEquals(convergenceC3, restoredC3.requiredEffect)

        val duplicateOldAck = factory.openCompatibilitySagaStore().use {
            it.acknowledgeWriteSucceeded(
                command.sagaId,
                legacyC1,
                LegacyCompatibilityWriteOutcome.ALREADY_APPLIED
            )
        }
        assertEquals(restoredC3, duplicateOldAck, "A replayed old effect must be audit-only.")
    }

    @Test
    fun `lease CAS is fenced and one lease emits one recovery effect`(): Unit = runBlocking {
        val factory = fixtureFactory()
        val command = command(sagaId = "saga-lease")
        val pending = factory.openCompatibilitySagaStore().use { it.persistIntent(command) }
        val checkpoint = assertNotNull(pending.requiredEffect)

        val lease1 = factory.openCompatibilitySagaStore().use { store ->
            store.acquireRecoveryLease(
                LegacyCompatibilityRecoveryLeaseRequest(
                    sagaId = command.sagaId,
                    expectedEffectId = checkpoint.effectId,
                    effectCheckpoint = checkpoint.checkpoint,
                    checkpointRevision = checkpoint.checkpointRevision,
                    holderId = "worker-a",
                    expectedLeaseVersion = 0,
                    newLeaseVersion = 1,
                    fencingToken = checkpoint.fencingToken + 10,
                    expiresAtEpochSeconds = 110
                )
            )
        }
        assertNotNull(lease1)

        val lostConcurrentCas = factory.openCompatibilitySagaStore().use { store ->
            store.acquireRecoveryLease(
                LegacyCompatibilityRecoveryLeaseRequest(
                    sagaId = command.sagaId,
                    expectedEffectId = checkpoint.effectId,
                    effectCheckpoint = checkpoint.checkpoint,
                    checkpointRevision = checkpoint.checkpointRevision,
                    holderId = "worker-b",
                    expectedLeaseVersion = 0,
                    newLeaseVersion = 1,
                    fencingToken = checkpoint.fencingToken + 11,
                    expiresAtEpochSeconds = 110
                )
            )
        }
        assertNull(lostConcurrentCas, "Only one holder may win the durable lease CAS.")

        val recovery1 = factory.openCompatibilitySagaStore().use { store ->
            store.requestRecovery(lease1.asRecoveryRequest())
        }
        assertNotNull(recovery1)
        assertEquals(checkpoint.effectId, recovery1.effectId)
        val duplicateRecovery = factory.openCompatibilitySagaStore().use { store ->
            store.requestRecovery(lease1.asRecoveryRequest())
        }
        assertNull(duplicateRecovery, "One recorded lease may emit the checkpoint at most once.")

        val afterClock = factory.openCompatibilitySagaStore().use { store ->
            store.advanceLogicalClock(command.sagaId, clockRevision = 1, nowEpochSeconds = 110)
        }
        assertEquals(110, afterClock.logicalNowEpochSeconds)

        val lease2 = factory.openCompatibilitySagaStore().use { store ->
            store.acquireRecoveryLease(
                LegacyCompatibilityRecoveryLeaseRequest(
                    sagaId = command.sagaId,
                    expectedEffectId = checkpoint.effectId,
                    effectCheckpoint = checkpoint.checkpoint,
                    checkpointRevision = checkpoint.checkpointRevision,
                    holderId = "worker-b",
                    expectedLeaseVersion = 1,
                    newLeaseVersion = 2,
                    fencingToken = lease1.fencingToken + 1,
                    expiresAtEpochSeconds = 120
                )
            )
        }
        assertNotNull(lease2)
        val staleRecovery: LegacyCompatibilityEffectReference? =
            factory.openCompatibilitySagaStore().use { it.requestRecovery(lease1.asRecoveryRequest()) }
        assertNull(
            staleRecovery,
            "The previous holder/fence stays stale after replacement."
        )
        assertNotNull(factory.openCompatibilitySagaStore().use { it.requestRecovery(lease2.asRecoveryRequest()) })
    }

    @Test
    fun `every persisted effect checkpoint is recoverable exactly once after reopen`() = runBlocking {
        val factory = fixtureFactory()
        val writingLegacy = factory.openCompatibilitySagaStore().use {
            it.persistIntent(
                command(
                    sagaId = "recover-writing-legacy",
                    compatibilityGeneration = 1
                )
            )
        }
        val writingV2 = factory.openCompatibilitySagaStore().use {
            it.persistIntent(
                command(
                    sagaId = "recover-writing-v2",
                    clientGeneration = LegacyCompatibilityClientGeneration.N,
                    legacyPrimaryKeyFingerprint = null,
                    legacyInstallationId = null,
                    legacyRegistrationId = null,
                    targetInstallationId = "installation-n",
                    compatibilityGeneration = 2
                )
            )
        }
        val recordingRetryStart = factory.openCompatibilitySagaStore().use {
            it.persistIntent(
                command(
                    sagaId = "recover-recording-retry",
                    compatibilityGeneration = 3
                )
            )
        }
        val recordingRetry = factory.openCompatibilitySagaStore().use {
            it.acknowledgeWriteFailed(
                recordingRetryStart.sagaId,
                assertNotNull(recordingRetryStart.requiredEffect),
                LegacyCompatibilityFailure.TRANSIENT
            )
        }
        val convergenceStart = factory.openCompatibilitySagaStore().use {
            it.persistIntent(
                command(
                    sagaId = "recover-recording-convergence",
                    compatibilityGeneration = 4
                )
            )
        }
        val convergenceAfterLegacy = factory.openCompatibilitySagaStore().use {
            it.acknowledgeWriteSucceeded(
                convergenceStart.sagaId,
                assertNotNull(convergenceStart.requiredEffect),
                LegacyCompatibilityWriteOutcome.APPLIED
            )
        }
        val recordingConvergence = factory.openCompatibilitySagaStore().use {
            it.acknowledgeWriteSucceeded(
                convergenceAfterLegacy.sagaId,
                assertNotNull(convergenceAfterLegacy.requiredEffect),
                LegacyCompatibilityWriteOutcome.APPLIED
            )
        }
        val blockStart = factory.openCompatibilitySagaStore().use {
            it.persistIntent(
                command(
                    sagaId = "recover-recording-block",
                    compatibilityGeneration = 5,
                    maxAttemptsPerStore = 1
                )
            )
        }
        val recordingBlock = factory.openCompatibilitySagaStore().use {
            it.acknowledgeWriteFailed(
                blockStart.sagaId,
                assertNotNull(blockStart.requiredEffect),
                LegacyCompatibilityFailure.UNAVAILABLE
            )
        }

        val cases = listOf(
            writingLegacy to LegacyCompatibilityEffectCheckpoint.WRITE_LEGACY_STORE,
            writingV2 to LegacyCompatibilityEffectCheckpoint.WRITE_V2_REGISTRATION_STORE,
            recordingRetry to LegacyCompatibilityEffectCheckpoint.PERSIST_RETRY_SCHEDULE,
            recordingConvergence to LegacyCompatibilityEffectCheckpoint.PERSIST_CONVERGENCE,
            recordingBlock to LegacyCompatibilityEffectCheckpoint.PERSIST_BLOCKED_TERMINAL
        )
        cases.forEachIndexed { index, (beforeCrash, checkpoint) ->
            val restored = factory.openCompatibilitySagaStore().use {
                assertNotNull(it.findBySagaId(beforeCrash.sagaId))
            }
            assertEquals(beforeCrash, restored)
            val effect = assertNotNull(restored.requiredEffect)
            assertEquals(checkpoint, effect.checkpoint)
            val lease = factory.openCompatibilitySagaStore().use {
                it.acquireRecoveryLease(
                    LegacyCompatibilityRecoveryLeaseRequest(
                        sagaId = restored.sagaId,
                        expectedEffectId = effect.effectId,
                        effectCheckpoint = checkpoint,
                        checkpointRevision = effect.checkpointRevision,
                        holderId = "recovery-worker-$index",
                        expectedLeaseVersion = 0,
                        newLeaseVersion = 1,
                        fencingToken = effect.fencingToken + 10,
                        expiresAtEpochSeconds = restored.logicalNowEpochSeconds + 10
                    )
                )
            }
            assertNotNull(lease)
            val recovered = factory.openCompatibilitySagaStore().use {
                it.requestRecovery(lease.asRecoveryRequest())
            }
            assertNotNull(recovered)
            assertEquals(effect.effectId, recovered.effectId)
            assertEquals(checkpoint, recovered.checkpoint)
            assertNull(
                factory.openCompatibilitySagaStore().use {
                    it.requestRecovery(lease.asRecoveryRequest())
                },
                "$checkpoint may be emitted only once by one recorded lease."
            )
        }
    }

    @Test
    fun `stale c1 acknowledgements cannot mutate c2 attempts schedule or store outcome`() = runBlocking {
        val factory = fixtureFactory()
        val command = command(sagaId = "saga-stale-ack")
        val afterIntent = factory.openCompatibilitySagaStore().use { it.persistIntent(command) }
        val c1 = assertNotNull(afterIntent.requiredEffect)
        val recordingRetry = factory.openCompatibilitySagaStore().use { store ->
            store.acknowledgeWriteFailed(
                sagaId = command.sagaId,
                reference = c1,
                failure = LegacyCompatibilityFailure.TRANSIENT
            )
        }
        val c2 = assertNotNull(recordingRetry.requiredEffect)
        assertEquals(1, recordingRetry.legacyAttempt)
        assertEquals(0, recordingRetry.v2Attempt)
        assertEquals(LegacyCompatibilityEffectCheckpoint.PERSIST_RETRY_SCHEDULE, c2.checkpoint)

        factory.openCompatibilitySagaStore().use { store ->
            store.acknowledgeWriteSucceeded(
                command.sagaId,
                c1,
                LegacyCompatibilityWriteOutcome.APPLIED
            )
            store.acknowledgeWriteFailed(
                command.sagaId,
                c1,
                LegacyCompatibilityFailure.TRANSIENT
            )
            store.recordRetry(command.sagaId, c1, nextRetryAtEpochSeconds = 105)
        }
        val unchanged = factory.openCompatibilitySagaStore().use { assertNotNull(it.findBySagaId(command.sagaId)) }
        assertEquals(recordingRetry, unchanged)

        val waiting = factory.openCompatibilitySagaStore().use {
            it.recordRetry(command.sagaId, c2, nextRetryAtEpochSeconds = 110)
        }
        assertEquals(LegacyCompatibilitySagaState.RETRY_WAIT, waiting.state)
        assertEquals(110, waiting.nextRetryAtEpochSeconds)
        assertEquals(1, waiting.legacyAttempt)
        assertEquals(0, waiting.v2Attempt)
    }

    @Test
    fun `retry clocks and attempts stay durable and independent per store`() = runBlocking {
        val factory = fixtureFactory()
        val command = command(sagaId = "saga-independent-retry")
        val start = factory.openCompatibilitySagaStore().use { it.persistIntent(command) }
        val failedLegacy = factory.openCompatibilitySagaStore().use {
            it.acknowledgeWriteFailed(
                command.sagaId,
                assertNotNull(start.requiredEffect),
                LegacyCompatibilityFailure.UNAVAILABLE
            )
        }
        val waitingLegacy = factory.openCompatibilitySagaStore().use {
            it.recordRetry(
                command.sagaId,
                assertNotNull(failedLegacy.requiredEffect),
                nextRetryAtEpochSeconds = 110
            )
        }
        assertEquals(1, waitingLegacy.legacyAttempt)
        assertEquals(0, waitingLegacy.v2Attempt)

        val premature = factory.openCompatibilitySagaStore().use {
            it.retryDue(command.sagaId, waitingLegacy.checkpointRevision)
        }
        assertEquals(waitingLegacy, premature)
        val at109 = factory.openCompatibilitySagaStore().use {
            it.advanceLogicalClock(command.sagaId, clockRevision = 1, nowEpochSeconds = 109)
        }
        assertEquals(109, at109.logicalNowEpochSeconds)
        assertEquals(
            LegacyCompatibilitySagaState.RETRY_WAIT,
            factory.openCompatibilitySagaStore().use {
                it.retryDue(command.sagaId, at109.checkpointRevision)
            }.state
        )
        val at110 = factory.openCompatibilitySagaStore().use {
            it.advanceLogicalClock(command.sagaId, clockRevision = 2, nowEpochSeconds = 110)
        }
        val retryingLegacy = factory.openCompatibilitySagaStore().use {
            it.retryDue(command.sagaId, at110.checkpointRevision)
        }
        assertEquals(LegacyCompatibilitySagaState.WRITING_LEGACY, retryingLegacy.state)

        val afterLegacy = factory.openCompatibilitySagaStore().use {
            it.acknowledgeWriteSucceeded(
                command.sagaId,
                assertNotNull(retryingLegacy.requiredEffect),
                LegacyCompatibilityWriteOutcome.ALREADY_APPLIED
            )
        }
        val failedV2 = factory.openCompatibilitySagaStore().use {
            it.acknowledgeWriteFailed(
                command.sagaId,
                assertNotNull(afterLegacy.requiredEffect),
                LegacyCompatibilityFailure.TRANSIENT
            )
        }
        assertEquals(1, failedV2.legacyAttempt)
        assertEquals(1, failedV2.v2Attempt)
        assertEquals(LegacyCompatibilityWriteStatus.APPLIED, failedV2.legacyWriteStatus)

        val restored = factory.openCompatibilitySagaStore().use { assertNotNull(it.findBySagaId(command.sagaId)) }
        assertEquals(failedV2, restored)
        assertEquals(110, restored.logicalNowEpochSeconds)
        assertEquals(2, restored.clockRevision)
    }

    @Test
    fun `retry exhaustion persists blocked terminal and can never report success`() = runBlocking {
        val factory = fixtureFactory()
        val command = command(sagaId = "saga-budget-exhausted", maxAttemptsPerStore = 1)
        val start = factory.openCompatibilitySagaStore().use { it.persistIntent(command) }
        val recordingBlock = factory.openCompatibilitySagaStore().use {
            it.acknowledgeWriteFailed(
                command.sagaId,
                assertNotNull(start.requiredEffect),
                LegacyCompatibilityFailure.UNAVAILABLE
            )
        }
        assertEquals(LegacyCompatibilitySagaState.RECORDING_BLOCK, recordingBlock.state)
        assertEquals(
            LegacyCompatibilityEffectCheckpoint.PERSIST_BLOCKED_TERMINAL,
            recordingBlock.requiredEffect?.checkpoint
        )
        assertNotEquals(
            LegacyCompatibilityResponseDisposition.CONVERGED_SUCCESS,
            recordingBlock.responseDisposition
        )
        val restoredRecordingBlock: LegacyCompatibilitySnapshot =
            factory.openCompatibilitySagaStore().use {
                assertNotNull(it.findBySagaId(command.sagaId))
            }
        assertEquals(
            recordingBlock,
            restoredRecordingBlock,
            "The blocked-terminal checkpoint must survive a process restart before ACK."
        )

        val blocked = factory.openCompatibilitySagaStore().use {
            it.recordBlocked(
                command.sagaId,
                assertNotNull(recordingBlock.requiredEffect)
            )
        }
        assertEquals(LegacyCompatibilitySagaState.BLOCKED, blocked.state)
        assertEquals(LegacyCompatibilityReconciliationStatus.BLOCKED, blocked.reconciliationStatus)
        assertEquals(LegacyCompatibilityResponseDisposition.BLOCKED_FAILURE, blocked.responseDisposition)
        assertNull(blocked.requiredEffect)
    }

    @Test
    fun `canonical key and HMAC identity isolate subject generation target and secrets`() = runBlocking {
        val factory = fixtureFactory(hmacKey = "stable-migration-hmac-key-value-32")
        val identityA = factory.deriveLegacyCompatibilityIdentity("immutable-row:a")
        val identityAReplay = factory.deriveLegacyCompatibilityIdentity("immutable-row:a")
        val identityB = factory.deriveLegacyCompatibilityIdentity("immutable-row:b")
        assertEquals(identityA, identityAReplay)
        assertNotEquals(identityA, identityB)
        assertFalse(identityA.installationId.contains("immutable-row"))
        assertFalse(identityA.registrationId.contains("immutable-row"))

        val first = command(
            sagaId = "saga-key-a",
            authenticatedUserId = "user:other",
            compatibilityGeneration = 1,
            legacyInstallationId = identityA.installationId,
            legacyRegistrationId = identityA.registrationId,
            targetInstallationId = identityA.installationId
        )
        val delimiterShift = command(
            sagaId = "saga-key-b",
            authenticatedUserId = "user",
            compatibilityGeneration = 1,
            legacyInstallationId = identityA.installationId,
            legacyRegistrationId = "other:${identityA.registrationId}",
            targetInstallationId = identityA.installationId
        )
        val nextGeneration = command(
            sagaId = "saga-key-c",
            authenticatedUserId = "user:other",
            compatibilityGeneration = 2,
            legacyInstallationId = identityA.installationId,
            legacyRegistrationId = identityA.registrationId,
            targetInstallationId = identityA.installationId
        )

        val snapshots = listOf(first, delimiterShift, nextGeneration).map { value ->
            factory.openCompatibilitySagaStore().use { it.persistIntent(value) }
        }
        assertEquals(3, snapshots.map { it.requestKey }.toSet().size)

        val rendered = snapshots.joinToString(separator = "\n")
        assertFalse(rendered.contains("immutable-row:a"))
        assertFalse(rendered.contains("stable-migration-hmac-key-value-32"))
        assertFalse(rendered.contains("raw-apns-token"))
        assertTrue(snapshots.all { it.authenticatedUserId.isNotBlank() })
    }

    private fun fixtureFactory(
        hmacKey: String = "h".repeat(32)
    ): SqliteBackendDeviceRegistrationStoreFactory {
        val root = Files.createTempDirectory("wakeve-legacy-compat-saga-")
        temporaryRoots.add(root)
        val configuration = DeviceRegistrationStoreConfiguration.resolve(
            systemProperties = mapOf(
                "wakeve.notification.device-registration.db.path" to root.resolve("registration.sqlite").toString(),
                "wakeve.notification.device-registration.legacy-identity-hmac-key" to hmacKey,
                "wakeve.notification.device-registration.token-encryption-key" to "e".repeat(32)
            ),
            environment = emptyMap()
        ).getOrThrow()
        return SqliteBackendDeviceRegistrationStoreFactory(configuration)
    }

    private fun command(
        sagaId: String,
        operation: LegacyCompatibilityOperation = LegacyCompatibilityOperation.REGISTER,
        clientGeneration: LegacyCompatibilityClientGeneration = LegacyCompatibilityClientGeneration.N_MINUS_1,
        authenticatedUserId: String = "owner",
        legacyPrimaryKeyFingerprint: String? = "legacy-primary-key-fingerprint",
        legacyInstallationId: String? = "hmac-installation-id",
        legacyRegistrationId: String? = "hmac-registration-id",
        targetInstallationId: String = "hmac-installation-id",
        targetRegistrationId: String? = null,
        tokenFingerprint: String? = if (operation == LegacyCompatibilityOperation.REGISTER) "token-fingerprint" else null,
        compatibilityGeneration: Long = 1,
        maxAttemptsPerStore: Int = 3
    ): LegacyCompatibilityCommand = LegacyCompatibilityCommand.create(
        sagaId = sagaId,
        operation = operation,
        clientGeneration = clientGeneration,
        authenticatedUserId = authenticatedUserId,
        platform = Platform.IOS,
        legacyPrimaryKeyFingerprint = legacyPrimaryKeyFingerprint,
        legacyInstallationId = legacyInstallationId,
        legacyRegistrationId = legacyRegistrationId,
        targetInstallationId = targetInstallationId,
        targetRegistrationId = targetRegistrationId,
        tokenFingerprint = tokenFingerprint,
        compatibilityGeneration = compatibilityGeneration,
        maxAttemptsPerStore = maxAttemptsPerStore,
        initialNowEpochSeconds = 100
    ).getOrThrow()
}
