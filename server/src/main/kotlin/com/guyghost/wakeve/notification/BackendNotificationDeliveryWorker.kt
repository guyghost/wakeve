package com.guyghost.wakeve.notification

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val backendNotificationDeliveryWorkerMutexes = ConcurrentHashMap<String, Mutex>()

/** Explicit production composition. Application rollout may construct it without starting it. */
class BackendNotificationDeliveryWorkerComposition(
    private val deliveryStoreFactory: BackendNotificationDeliveryRuntimeFactory,
    @Suppress("unused") private val registrationStoreFactory: BackendDeviceRegistrationStoreFactory,
    private val authority: BackendDeliveryAuthority,
    @Suppress("unused") private val clock: BackendDeliveryWorkerClock,
    private val policy: BackendDeliveryPolicyPort,
    private val tokenAvailability: BackendDeliveryTokenAvailabilityPort,
    private val credentials: BackendDeliveryCredentialPort,
    private val validatedCredentialPort: BackendValidatedDeliveryCredentialPort =
        BackendValidatedDeliveryCredentialPort {
            val version = credentials.credentialVersion()
            BackendValidatedDeliveryCredential(version, digest(version))
        },
    private val provider: BackendDeliveryProviderPort,
    private val jitter: BackendDeliveryJitterSource,
    private val faultInjector: BackendDeliveryWorkerFaultInjector
) {
    suspend fun current(deliveryKey: DeliveryKey): BackendNotificationDeliverySnapshot? =
        deliveryStoreFactory.openDeliveryRuntime(faultInjector).use { it.current(deliveryKey) }

    suspend fun advanceLogicalClock(
        deliveryKey: DeliveryKey,
        expectedClockRevision: Long,
        newEpochSeconds: Long
    ): BackendNotificationDeliverySnapshot = deliveryStoreFactory.openDeliveryRuntime(faultInjector).use {
        it.advanceLogicalClock(deliveryKey, expectedClockRevision, newEpochSeconds)
    }

    suspend fun onCredentialsRotated(@Suppress("UNUSED_PARAMETER") newCredentialVersion: String) {
        onValidatedCredentialPortChanged()
    }

    suspend fun onValidatedCredentialPortChanged() {
        val validated = validatedCredentialPort.validatedCredential()
        deliveryStoreFactory.openDeliveryRuntime(faultInjector).use {
            it.clearCredentialCircuit(validated)
        }
    }

    /**
     * The sole raw provider-observation handler. It binds transport data to the exact
     * durable request before internal classification and checkpoint persistence.
     */
    suspend fun handleProviderObservation(
        command: BackendRawProviderObservationCommand
    ): BackendNotificationDeliverySnapshot? =
        deliveryStoreFactory.openDeliveryRuntime(faultInjector).use { runtime ->
            val request = command.reference
            val before = runtime.current(request.deliveryKey) ?: return@use null
            val lease = before.lease
            val observationStateMatches = when (command.observation) {
                BackendProviderRawObservation.ProviderAuthRejected -> before.state in setOf(
                    BackendDurableDeliveryState.AUTH,
                    BackendDurableDeliveryState.SENDING
                )
                else -> before.state == BackendDurableDeliveryState.SENDING
            }
            if (!observationStateMatches ||
                before.correlationId != request.correlationId ||
                before.attempt != request.attempt ||
                before.registrationId != request.registrationId ||
                lease?.holderId != request.leaseHolderId ||
                lease.version != request.leaseVersion ||
                lease.fencingToken != request.leaseFencingToken ||
                lease.expiresAtLogicalEpochSeconds <= before.nowEpochSeconds
            ) return@use before

            val classified = BackendDeliveryObservationClassifier.classify(
                command.observation,
                BackendDeliveryRetryContext(
                    deliveryKey = request.deliveryKey,
                    nowEpochSeconds = before.nowEpochSeconds,
                    expiresAtEpochSeconds = before.expiresAtEpochSeconds,
                    attempt = before.attempt,
                    maxAttempts = before.maxAttempts
                ),
                jitter
            )
            val credential = validatedCredentialPort.validatedCredential()
            val staged = (runtime as? SqliteBackendDeliveryRuntime)
                ?.stageClassifiedProviderObservation(
                    request.deliveryKey,
                    lease,
                    classified,
                    credential.version,
                    credential.fingerprint
                ) ?: error("The durable SQLite delivery runtime is required")
            val checkpoint = staged.pendingCheckpoint ?: return@use staged
            val reference = staged.providerReference(checkpoint)
            runtime.requestProviderCheckpoint(reference)
            val committed = runtime.acknowledgeProviderCheckpoint(reference)
            committed
        }

    internal suspend fun recoverDue(holderId: String): BackendDeliveryRecoveryReport =
        deliveryStoreFactory.openDeliveryRuntime(faultInjector).use { runtime ->
            val claimed = linkedSetOf<DeliveryKey>()
            runtime.dueDeliveryKeys(authority).forEach { deliveryKey ->
                backendNotificationDeliveryWorkerMutexes
                    .computeIfAbsent(deliveryKey.value) { Mutex() }
                    .withLock { processDelivery(runtime, deliveryKey, holderId, claimed) }
            }
            BackendDeliveryRecoveryReport(claimed)
        }

    private suspend fun processDelivery(
        runtime: BackendDeliveryRuntime,
        deliveryKey: DeliveryKey,
        holderId: String,
        claimed: MutableSet<DeliveryKey>
    ) {
        repeat(MAX_STEPS_PER_DRAIN) {
            val snapshot = runtime.current(deliveryKey) ?: return
            if (snapshot.authority?.value != authority.wireValue) return
            snapshot.pendingCheckpoint?.let { checkpoint ->
                val checkpointSnapshot = if (checkpoint.leaseHolderId != null &&
                    snapshot.lease?.expiresAtLogicalEpochSeconds?.let { it <= snapshot.nowEpochSeconds } == true
                ) {
                    runtime.acquireLease(deliveryKey, holderId) ?: return
                    runtime.current(deliveryKey) ?: return
                } else {
                    if (checkpoint.leaseHolderId != null && checkpoint.leaseHolderId != holderId) return
                    snapshot
                }
                val recoveredCheckpoint = checkpointSnapshot.pendingCheckpoint ?: return
                val reference = checkpointSnapshot.providerReference(recoveredCheckpoint)
                if (!recoveredCheckpoint.effectRequested) runtime.requestProviderCheckpoint(reference)
                val committed = runtime.acknowledgeProviderCheckpoint(reference)
                if (committed.state == BackendDurableDeliveryState.PROVIDER_AUTH_BLOCKED) {
                    return
                }
                return@repeat
            }

            if (snapshot.nowEpochSeconds >= snapshot.expiresAtEpochSeconds && snapshot.state in expirableStates) {
                val staged = runtime.stageExpiry(
                    deliveryKey,
                    snapshot.checkpointRevision,
                    snapshot.authority,
                    snapshot.authorityFencingToken,
                    snapshot.lease?.holderId,
                    snapshot.lease?.version,
                    snapshot.lease?.fencingToken
                )
                if (staged.pendingCheckpoint != null) return@repeat
                return
            }
            if (snapshot.state in terminalStates) return
            if (snapshot.nextAttemptAtEpochSeconds?.let { it > snapshot.nowEpochSeconds } == true) return

            if (snapshot.state in policyStates) {
                when (val decision = policy.decide(
                BackendDeliveryPolicyContext(
                    deliveryKey,
                    snapshot.registrationId,
                    snapshot.nowEpochSeconds,
                    snapshot.expiresAtEpochSeconds
                )
            )) {
                    BackendDeliveryPolicyDecision.ALLOW -> {
                        if (snapshot.state != BackendDurableDeliveryState.QUEUED) {
                            runtime.markPolicyAllowed(
                                deliveryKey,
                                snapshot.checkpointRevision,
                                snapshot.authority,
                                snapshot.authorityFencingToken
                            )
                            return@repeat
                        }
                    }
                is BackendDeliveryPolicyDecision.QuietUntil -> {
                        runtime.markQuietHours(
                            deliveryKey,
                            snapshot.checkpointRevision,
                            snapshot.authority,
                            snapshot.authorityFencingToken,
                            minOf(decision.epochSeconds, snapshot.expiresAtEpochSeconds)
                        )
                    return
                }
                BackendDeliveryPolicyDecision.SUPPRESS -> {
                        runtime.suppressBeforeLease(
                            deliveryKey,
                            snapshot.checkpointRevision,
                            snapshot.authority,
                            snapshot.authorityFencingToken
                        )
                    return
                }
                }

                if (!tokenAvailability.isAvailable(snapshot.registrationId)) {
                    runtime.markNoActiveToken(
                        deliveryKey,
                        snapshot.checkpointRevision,
                        snapshot.authority,
                        snapshot.authorityFencingToken
                    )
                    return
                }
            }

            if (snapshot.state == BackendDurableDeliveryState.QUEUED ||
                snapshot.state == BackendDurableDeliveryState.RETRY_SCHEDULED) {
                val lease = runtime.acquireLease(deliveryKey, holderId) ?: return
                claimed += deliveryKey
                return@repeat
            }

            val activeLease = snapshot.lease ?: return
            if (activeLease.holderId != holderId || activeLease.expiresAtLogicalEpochSeconds <= snapshot.nowEpochSeconds) return
            val credential = validatedCredentialPort.validatedCredential()
            if (runtime.blockedCredential() != null) {
                handleProviderObservation(
                    BackendRawProviderObservationCommand(
                        snapshot.providerRequest(activeLease),
                        BackendProviderRawObservation.ProviderAuthRejected
                    )
                )
                return
            }
            if (snapshot.state == BackendDurableDeliveryState.AUTH) {
                if (snapshot.refreshUsed) {
                    val refreshKey = snapshot.correlationId ?: return
                    if (!credentials.refreshAfterProviderRejection(credential.version, refreshKey)) {
                        handleProviderObservation(
                            BackendRawProviderObservationCommand(
                                snapshot.providerRequest(activeLease),
                                BackendProviderRawObservation.ProviderAuthRejected
                            )
                        )
                        return
                    }
                }
                val correlationId = snapshot.correlationId ?: return
                val sending = runtime.markProviderAuthReady(
                    deliveryKey,
                    correlationId,
                    snapshot.attempt,
                    activeLease.holderId,
                    activeLease.version,
                    activeLease.fencingToken
                )
                if (sending.state != BackendDurableDeliveryState.SENDING) return
                return@repeat
            }
            if (snapshot.state != BackendDurableDeliveryState.SENDING) return

            val correlationId = snapshot.correlationId ?: return
            val request = snapshot.providerRequest(activeLease)
            val observation = provider.send(request)
            val committed = handleProviderObservation(BackendRawProviderObservationCommand(request, observation)) ?: return
            if (committed.state == BackendDurableDeliveryState.PROVIDER_AUTH_BLOCKED) return
            if (committed.state in terminalStates) return
            return@repeat
        }
    }

    private fun BackendNotificationDeliverySnapshot.providerRequest(
        activeLease: BackendDeliveryLease
    ) = BackendDeliveryProviderRequest(
        deliveryKey = deliveryKey,
        registrationId = registrationId,
        apnsId = "wakeve-${digest(deliveryKey.value)}",
        correlationId = correlationId ?: "",
        attempt = attempt,
        leaseHolderId = activeLease.holderId,
        leaseVersion = activeLease.version,
        leaseFencingToken = activeLease.fencingToken
    )

    private fun BackendNotificationDeliverySnapshot.providerReference(
        checkpoint: BackendDeliveryProviderCheckpoint
    ) = BackendDeliveryCheckpointReference(
        deliveryKey = deliveryKey,
        effectId = checkpoint.effectId,
        checkpointRevision = checkpoint.revision,
        authority = authority,
        authorityFencingToken = authorityFencingToken,
        leaseHolderId = checkpoint.leaseHolderId,
        leaseVersion = checkpoint.leaseVersion,
        leaseFencingToken = checkpoint.leaseFencingToken
    )

    private companion object {
        const val MAX_STEPS_PER_DRAIN = 12
        val policyStates = setOf(
            BackendDurableDeliveryState.POLICY_CHECK,
            BackendDurableDeliveryState.DEFERRED_QUIET_HOURS,
            BackendDurableDeliveryState.AWAITING_TOKEN,
            BackendDurableDeliveryState.QUEUED,
            BackendDurableDeliveryState.RETRY_SCHEDULED
        )
        val expirableStates = setOf(
            BackendDurableDeliveryState.POLICY_CHECK,
            BackendDurableDeliveryState.DEFERRED_QUIET_HOURS,
            BackendDurableDeliveryState.AWAITING_TOKEN,
            BackendDurableDeliveryState.QUEUED,
            BackendDurableDeliveryState.AUTH,
            BackendDurableDeliveryState.SENDING,
            BackendDurableDeliveryState.RETRY_SCHEDULED,
            BackendDurableDeliveryState.UNKNOWN_OUTCOME,
            BackendDurableDeliveryState.PROVIDER_AUTH_BLOCKED
        )
        val terminalStates = setOf(
            BackendDurableDeliveryState.SUPPRESSED,
            BackendDurableDeliveryState.UNKNOWN_OUTCOME,
            BackendDurableDeliveryState.ACCEPTED_BY_APNS,
            BackendDurableDeliveryState.INVALID_TOKEN,
            BackendDurableDeliveryState.REJECTED_PAYLOAD,
            BackendDurableDeliveryState.PROVIDER_AUTH_BLOCKED,
            BackendDurableDeliveryState.EXPIRED,
            BackendDurableDeliveryState.RETRY_EXHAUSTED,
            BackendDurableDeliveryState.CANCELLED
        )
    }
}

/** Lifecycle-bounded entry point used by startup recovery and an external scheduler. */
class BackendNotificationDeliveryRecoveryScheduler(
    private val composition: BackendNotificationDeliveryWorkerComposition,
    private val holderId: String
) : AutoCloseable {
    private var closed = false

    suspend fun startAndDrainDueWork(): BackendDeliveryRecoveryReport {
        check(!closed) { "Notification recovery scheduler is closed" }
        return composition.recoverDue(holderId)
    }

    suspend fun onRegistrationActivated(@Suppress("UNUSED_PARAMETER") registrationId: String) {
        check(!closed) { "Notification recovery scheduler is closed" }
    }

    suspend fun onCredentialsRotated(newCredentialVersion: String) {
        check(!closed) { "Notification recovery scheduler is closed" }
        composition.onCredentialsRotated(newCredentialVersion)
    }

    suspend fun onValidatedCredentialPortChanged() {
        check(!closed) { "Notification recovery scheduler is closed" }
        composition.onValidatedCredentialPortChanged()
    }

    override fun close() {
        closed = true
    }
}
