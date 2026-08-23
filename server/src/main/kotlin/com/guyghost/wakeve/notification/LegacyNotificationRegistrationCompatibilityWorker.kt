package com.guyghost.wakeve.notification

import java.util.UUID
import java.security.SecureRandom
import kotlin.math.floor

/**
 * Executes one durable compatibility checkpoint at a time. Every external store call happens
 * after the intent and a fenced recovery lease have committed in the backend SQLite store.
 */
class LegacyNotificationRegistrationCompatibilityWorker(
    private val notificationService: NotificationService,
    private val registrationStoreFactory: BackendDeviceRegistrationStoreFactory,
    private val leaseDurationEpochSeconds: Long = DEFAULT_LEASE_DURATION_SECONDS,
    private val logicalClock: (() -> Long)? = null,
    private val jitterSource: () -> Double = SecureRandom()::nextDouble,
    private val tokenCustodian: LegacyCompatibilityTokenCustodian =
        FactoryLegacyCompatibilityTokenCustodian(registrationStoreFactory),
    private val legacyStoreSink: LegacyCompatibilityStoreSink =
        NotificationServiceLegacyCompatibilityStoreSink(notificationService),
    private val v2StoreSink: LegacyCompatibilityStoreSink =
        BackendV2LegacyCompatibilityStoreSink(registrationStoreFactory)
) {
    init {
        require(leaseDurationEpochSeconds > 0) { "Compatibility lease duration must be positive" }
    }

    suspend fun execute(
        command: LegacyCompatibilityCommand,
        rawToken: String? = null
    ): LegacyCompatibilitySnapshot {
        val persisted = registrationStoreFactory.openCompatibilitySagaStore().use { store ->
            store.persistIntent(command, rawToken)
        }
        return executePersisted(persisted.sagaId)
    }

    /**
     * Resumes the exact persisted checkpoint. A caller that owns a durable clock source may
     * advance it explicitly; the worker never reads a wall clock to decide lease or retry expiry.
     */
    suspend fun resume(
        sagaId: String,
        nowEpochSeconds: Long? = null
    ): LegacyCompatibilitySnapshot {
        advanceClockAndReleaseDueRetry(sagaId, nowEpochSeconds ?: logicalClock?.invoke())
        return executePersisted(sagaId)
    }

    /** Discovers and resumes durable checkpoints without requiring another HTTP request. */
    suspend fun recoverDue(): List<LegacyCompatibilitySnapshot> {
        // Opening the canonical store is also the startup preflight gate. Keep it ahead of the
        // optional clock fast-path so a disabled scheduler cannot silently bypass migration.
        registrationStoreFactory.openCompatibilitySagaStore().close()
        val nowEpochSeconds = logicalClock?.invoke() ?: return emptyList()
        val candidates = registrationStoreFactory.openCompatibilitySagaStore().use { store ->
            store.recoveryCandidateSagaIds()
        }
        return buildList {
            candidates.forEach { sagaId ->
                val prepared = advanceClockAndReleaseDueRetry(sagaId, nowEpochSeconds)
                if (
                    prepared.state != LegacyCompatibilitySagaState.RETRY_WAIT ||
                    prepared.nextRetryAtEpochSeconds?.let {
                        prepared.logicalNowEpochSeconds >= it
                    } == true
                ) {
                    add(executePersisted(sagaId))
                }
            }
        }
    }

    private suspend fun advanceClockAndReleaseDueRetry(
        sagaId: String,
        nowEpochSeconds: Long?
    ): LegacyCompatibilitySnapshot = registrationStoreFactory
        .openCompatibilitySagaStore()
        .use { store ->
            var current = store.findBySagaId(sagaId)
                ?: throw IllegalArgumentException("Compatibility saga is unavailable")
            if (nowEpochSeconds != null && nowEpochSeconds >= current.logicalNowEpochSeconds) {
                current = store.advanceLogicalClock(
                    sagaId = sagaId,
                    clockRevision = current.clockRevision + 1,
                    nowEpochSeconds = nowEpochSeconds
                )
            }
            val deadline = current.nextRetryAtEpochSeconds
            if (
                current.state == LegacyCompatibilitySagaState.RETRY_WAIT &&
                deadline != null &&
                current.logicalNowEpochSeconds >= deadline
            ) {
                current = store.retryDue(current.sagaId, current.checkpointRevision)
            }
            current
        }

    private suspend fun executePersisted(sagaId: String): LegacyCompatibilitySnapshot {
        repeat(MAX_CHECKPOINTS_PER_RUN) {
            val current = snapshot(sagaId)
            if (
                current.state == LegacyCompatibilitySagaState.CONVERGED ||
                current.state == LegacyCompatibilitySagaState.BLOCKED ||
                current.state == LegacyCompatibilitySagaState.RETRY_WAIT ||
                current.requiredEffect == null
            ) {
                return current
            }

            val claimed = claimEffect(current) ?: return snapshot(sagaId)
            when (claimed.checkpoint) {
                LegacyCompatibilityEffectCheckpoint.WRITE_LEGACY_STORE -> {
                    acknowledgeWriteResult(sagaId, claimed, writeLegacy(current))
                }
                LegacyCompatibilityEffectCheckpoint.WRITE_V2_REGISTRATION_STORE -> {
                    acknowledgeWriteResult(sagaId, claimed, writeV2(current))
                }
                LegacyCompatibilityEffectCheckpoint.PERSIST_RETRY_SCHEDULE -> {
                    val latest = snapshot(sagaId)
                    val failedAttempt = when (latest.lastFailureCheckpoint()) {
                        LegacyCompatibilityEffectCheckpoint.WRITE_LEGACY_STORE -> latest.legacyAttempt
                        LegacyCompatibilityEffectCheckpoint.WRITE_V2_REGISTRATION_STORE -> latest.v2Attempt
                        else -> maxOf(latest.legacyAttempt, latest.v2Attempt)
                    }
                    val delaySeconds = legacyCompatibilityBackoffSeconds(
                        attempt = failedAttempt,
                        randomSample = jitterSource()
                    )
                    val deadline = if (
                        latest.logicalNowEpochSeconds <= Long.MAX_VALUE - delaySeconds
                    ) {
                        latest.logicalNowEpochSeconds + delaySeconds
                    } else {
                        Long.MAX_VALUE
                    }
                    registrationStoreFactory.openCompatibilitySagaStore().use { store ->
                        store.recordRetry(sagaId, claimed, deadline)
                    }
                }
                LegacyCompatibilityEffectCheckpoint.PERSIST_CONVERGENCE -> {
                    registrationStoreFactory.openCompatibilitySagaStore().use { store ->
                        store.recordConvergence(sagaId, claimed)
                    }
                }
                LegacyCompatibilityEffectCheckpoint.PERSIST_BLOCKED_TERMINAL -> {
                    registrationStoreFactory.openCompatibilitySagaStore().use { store ->
                        store.recordBlocked(sagaId, claimed)
                    }
                }
            }
        }
        return snapshot(sagaId)
    }

    private suspend fun claimEffect(
        snapshot: LegacyCompatibilitySnapshot
    ): LegacyCompatibilityEffectReference? {
        val required = snapshot.requiredEffect ?: return null
        val currentLeaseVersion = snapshot.recoveryLease?.leaseVersion ?: 0L
        val lease = registrationStoreFactory.openCompatibilitySagaStore().use { store ->
            store.acquireRecoveryLease(
                LegacyCompatibilityRecoveryLeaseRequest(
                    sagaId = snapshot.sagaId,
                    expectedEffectId = required.effectId,
                    effectCheckpoint = required.checkpoint,
                    checkpointRevision = required.checkpointRevision,
                    holderId = "compat-worker-${UUID.randomUUID()}",
                    expectedLeaseVersion = currentLeaseVersion,
                    newLeaseVersion = currentLeaseVersion + 1,
                    fencingToken = maxOf(
                        required.fencingToken,
                        snapshot.recoveryLease?.fencingToken ?: 0L
                    ) + 1,
                    expiresAtEpochSeconds = snapshot.logicalNowEpochSeconds + leaseDurationEpochSeconds
                )
            )
        } ?: return null
        return registrationStoreFactory.openCompatibilitySagaStore().use { store ->
            store.requestRecovery(lease.asRecoveryRequest())
        }
    }

    private suspend fun acknowledgeWriteResult(
        sagaId: String,
        reference: LegacyCompatibilityEffectReference,
        result: CompatibilityStoreWriteResult
    ) {
        registrationStoreFactory.openCompatibilitySagaStore().use { store ->
            when (result) {
                is CompatibilityStoreWriteResult.Succeeded -> store.acknowledgeWriteSucceeded(
                    sagaId = sagaId,
                    reference = reference,
                    outcome = result.outcome
                )
                is CompatibilityStoreWriteResult.Failed -> store.acknowledgeWriteFailed(
                    sagaId = sagaId,
                    reference = reference,
                    failure = result.failure
                )
            }
        }
    }

    private suspend fun writeLegacy(
        snapshot: LegacyCompatibilitySnapshot
    ): CompatibilityStoreWriteResult = writeWithScopedToken(snapshot, legacyStoreSink)

    private suspend fun writeV2(
        snapshot: LegacyCompatibilitySnapshot
    ): CompatibilityStoreWriteResult = writeWithScopedToken(snapshot, v2StoreSink)

    private suspend fun writeWithScopedToken(
        snapshot: LegacyCompatibilitySnapshot,
        sink: LegacyCompatibilityStoreSink
    ): CompatibilityStoreWriteResult = try {
        val outcome = if (snapshot.operation == LegacyCompatibilityOperation.REGISTER) {
            var scopedOutcome: LegacyCompatibilityWriteOutcome? = null
            val available = tokenCustodian.withRegistrationToken(snapshot.sagaId) { tokenScope ->
                scopedOutcome = sink.write(snapshot, tokenScope)
            }
            if (!available) {
                return CompatibilityStoreWriteResult.Failed(
                    LegacyCompatibilityFailure.MISCONFIGURED
                )
            }
            checkNotNull(scopedOutcome) { "Token custody callback did not execute" }
        } else {
            sink.write(snapshot, null)
        }
        CompatibilityStoreWriteResult.Succeeded(outcome)
    } catch (_: IllegalArgumentException) {
        CompatibilityStoreWriteResult.Failed(LegacyCompatibilityFailure.MISCONFIGURED)
    } catch (_: LegacyCompatibilityConflictException) {
        CompatibilityStoreWriteResult.Failed(LegacyCompatibilityFailure.CONFLICT)
    } catch (_: IllegalStateException) {
        CompatibilityStoreWriteResult.Failed(LegacyCompatibilityFailure.MISCONFIGURED)
    } catch (_: Exception) {
        CompatibilityStoreWriteResult.Failed(LegacyCompatibilityFailure.UNAVAILABLE)
    }

    private suspend fun snapshot(sagaId: String): LegacyCompatibilitySnapshot =
        registrationStoreFactory.openCompatibilitySagaStore().use { store ->
            store.findBySagaId(sagaId)
                ?: throw IllegalArgumentException("Compatibility saga is unavailable")
        }

    private fun LegacyCompatibilitySnapshot.lastFailureCheckpoint(): LegacyCompatibilityEffectCheckpoint? =
        when {
            state != LegacyCompatibilitySagaState.RECORDING_RETRY -> null
            legacyAttempt > v2Attempt && legacyWriteStatus == LegacyCompatibilityWriteStatus.PENDING ->
                LegacyCompatibilityEffectCheckpoint.WRITE_LEGACY_STORE
            v2WriteStatus == LegacyCompatibilityWriteStatus.PENDING ->
                LegacyCompatibilityEffectCheckpoint.WRITE_V2_REGISTRATION_STORE
            else -> null
        }

    private sealed interface CompatibilityStoreWriteResult {
        data class Succeeded(
            val outcome: LegacyCompatibilityWriteOutcome
        ) : CompatibilityStoreWriteResult

        data class Failed(
            val failure: LegacyCompatibilityFailure
        ) : CompatibilityStoreWriteResult
    }

    private companion object {
        const val DEFAULT_LEASE_DURATION_SECONDS = 30L
        const val MAX_CHECKPOINTS_PER_RUN = 16
    }
}

internal fun legacyCompatibilityBackoffSeconds(
    attempt: Int,
    randomSample: Double,
    capSeconds: Long = 300L
): Long {
    val boundedCap = capSeconds.coerceIn(1L, 300L)
    val exponentAtCap = 9
    val exponent = (attempt.coerceAtLeast(1) - 1).coerceAtMost(exponentAtCap)
    val window = minOf(boundedCap, 1L shl exponent)
    val boundedSample = when {
        randomSample.isNaN() -> 0.0
        randomSample < 0.0 -> 0.0
        randomSample > 1.0 -> 1.0
        else -> randomSample
    }
    return maxOf(1L, floor(window.toDouble() * boundedSample).toLong())
}

private class FactoryLegacyCompatibilityTokenCustodian(
    private val factory: BackendDeviceRegistrationStoreFactory
) : LegacyCompatibilityTokenCustodian {
    override suspend fun withRegistrationToken(
        sagaId: String,
        block: suspend (LegacyCompatibilityScopedToken) -> Unit
    ): Boolean = factory.openCompatibilitySagaStore().use { store ->
        val custodian = store as? LegacyCompatibilityTokenCustodian ?: return@use false
        custodian.withRegistrationToken(sagaId, block)
    }
}

private class NotificationServiceLegacyCompatibilityStoreSink(
    private val notificationService: NotificationService
) : LegacyCompatibilityStoreSink {
    override suspend fun write(
        snapshot: LegacyCompatibilitySnapshot,
        tokenScope: LegacyCompatibilityScopedToken?
    ): LegacyCompatibilityWriteOutcome {
        when (snapshot.operation) {
            LegacyCompatibilityOperation.REGISTER -> {
                checkNotNull(tokenScope) { "Registration token custody is required" }
                    .consumeWith { tokenChars ->
                        notificationService.registerPushToken(
                            userId = snapshot.authenticatedUserId,
                            platform = snapshot.platform,
                            token = tokenChars.concatToString()
                        ).getOrThrow()
                    }
            }
            LegacyCompatibilityOperation.UNREGISTER -> {
                require(tokenScope == null) { "Unregistration must not receive token custody" }
                notificationService.unregisterPushToken(
                    userId = snapshot.authenticatedUserId,
                    platform = snapshot.platform
                ).getOrThrow()
            }
        }
        return LegacyCompatibilityWriteOutcome.APPLIED
    }
}

private class BackendV2LegacyCompatibilityStoreSink(
    private val factory: BackendDeviceRegistrationStoreFactory
) : LegacyCompatibilityStoreSink {
    override suspend fun write(
        snapshot: LegacyCompatibilitySnapshot,
        tokenScope: LegacyCompatibilityScopedToken?
    ): LegacyCompatibilityWriteOutcome = when (snapshot.clientGeneration) {
        LegacyCompatibilityClientGeneration.N_MINUS_1 -> writeLegacyTarget(snapshot, tokenScope)
        LegacyCompatibilityClientGeneration.N -> writeExactTarget(snapshot, tokenScope)
    }

    private suspend fun writeLegacyTarget(
        snapshot: LegacyCompatibilitySnapshot,
        tokenScope: LegacyCompatibilityScopedToken?
    ): LegacyCompatibilityWriteOutcome {
        val legacyRowKey = legacyCompatibilityRowKey(
            snapshot.authenticatedUserId,
            snapshot.platform
        )
        return when (snapshot.operation) {
            LegacyCompatibilityOperation.REGISTER -> {
                var outcome: LegacyCompatibilityWriteOutcome? = null
                checkNotNull(tokenScope) { "Registration token custody is required" }
                    .consumeWith { tokenChars ->
                        val result = factory.open().use { store ->
                            store.backfillLegacy(
                                LegacyNotificationTokenBackfill.create(
                                    legacyRowKey = legacyRowKey,
                                    userId = snapshot.authenticatedUserId,
                                    platform = snapshot.platform,
                                    rawToken = tokenChars.concatToString(),
                                    scope = snapshot.scope,
                                    updatedAtEpochSeconds = snapshot.logicalNowEpochSeconds
                                ).getOrThrow()
                            )
                        }
                        if (
                            result.installation.installationId != snapshot.legacyInstallationId ||
                            result.registration.registrationId != snapshot.legacyRegistrationId
                        ) {
                            throw LegacyCompatibilityConflictException()
                        }
                        outcome = if (result.created) {
                            LegacyCompatibilityWriteOutcome.APPLIED
                        } else {
                            LegacyCompatibilityWriteOutcome.ALREADY_APPLIED
                        }
                    }
                checkNotNull(outcome)
            }
            LegacyCompatibilityOperation.UNREGISTER -> factory.open().use { store ->
                store.unregisterLegacy(
                    legacyRowKey = legacyRowKey,
                    authenticatedUserId = snapshot.authenticatedUserId,
                    platform = snapshot.platform,
                    scope = snapshot.scope,
                    reason = DeviceRegistrationUnregisteredReason.LOGOUT,
                    atEpochSeconds = snapshot.logicalNowEpochSeconds
                ).toCompatibilityWriteOutcome()
            }
        }
    }

    private suspend fun writeExactTarget(
        snapshot: LegacyCompatibilitySnapshot,
        tokenScope: LegacyCompatibilityScopedToken?
    ): LegacyCompatibilityWriteOutcome = when (snapshot.operation) {
        LegacyCompatibilityOperation.REGISTER -> {
            checkNotNull(tokenScope) { "Registration token custody is required" }
                .consumeWith { tokenChars ->
                    factory.open().use { store ->
                        store.register(
                            BackendDeviceRegistrationRequest.create(
                                installationId = snapshot.targetInstallationId,
                                authenticatedUserId = snapshot.authenticatedUserId,
                                platform = snapshot.platform,
                                scope = snapshot.scope,
                                rawToken = tokenChars.concatToString(),
                                registeredAtEpochSeconds = snapshot.logicalNowEpochSeconds
                            ).getOrThrow()
                        )
                    }
                }
            LegacyCompatibilityWriteOutcome.APPLIED
        }
        LegacyCompatibilityOperation.UNREGISTER -> factory.open().use { store ->
            val registrationId = snapshot.targetRegistrationId
            if (registrationId != null) {
                store.unregisterRegistration(
                    registrationId = registrationId,
                    authenticatedUserId = snapshot.authenticatedUserId,
                    reason = DeviceRegistrationUnregisteredReason.LOGOUT,
                    atEpochSeconds = snapshot.logicalNowEpochSeconds
                )
            } else {
                store.unregisterInstallation(
                    installationId = snapshot.targetInstallationId,
                    authenticatedUserId = snapshot.authenticatedUserId,
                    reason = DeviceRegistrationUnregisteredReason.LOGOUT,
                    atEpochSeconds = snapshot.logicalNowEpochSeconds
                )
            }.toCompatibilityWriteOutcome()
        }
    }
}

private fun BackendDeviceUnregistrationResult.toCompatibilityWriteOutcome(): LegacyCompatibilityWriteOutcome =
    when (outcome) {
        BackendDeviceUnregistrationOutcome.UNREGISTERED -> LegacyCompatibilityWriteOutcome.APPLIED
        BackendDeviceUnregistrationOutcome.ALREADY_ABSENT -> LegacyCompatibilityWriteOutcome.ALREADY_APPLIED
        BackendDeviceUnregistrationOutcome.NOT_OWNED -> throw LegacyCompatibilityConflictException()
    }

private class LegacyCompatibilityConflictException : IllegalStateException()

internal fun legacyCompatibilityRowKey(userId: String, platform: Platform): String =
    "legacy-${platform.name.lowercase()}-${userId.trim()}"
