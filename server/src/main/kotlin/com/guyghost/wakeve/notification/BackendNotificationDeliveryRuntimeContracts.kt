package com.guyghost.wakeve.notification

import kotlin.math.floor
import kotlin.math.pow

/** Values which may cross the durable provider-result boundary. Provider text is never stored. */
enum class BackendPersistedProviderReason {
    HTTP_200,
    HTTP_5XX,
    TOO_MANY_REQUESTS,
    IDLE_TIMEOUT,
    TOKEN_INVALID,
    PAYLOAD_REJECTED,
    PROVIDER_AUTH_REJECTED,
    UNKNOWN_PROVIDER_REASON,
    TRANSPORT_BEFORE_WRITE,
    TRANSPORT_OUTCOME_UNKNOWN,
    INVALID_RETRY_AFTER,
    RETRY_WOULD_REACH_EXPIRY,
    RETRY_BUDGET_EXHAUSTED
}

enum class BackendDurableProviderOutcome {
    ACCEPTED,
    RETRY,
    INVALID_TOKEN,
    REJECTED_PAYLOAD,
    REFRESH_AUTH,
    PROVIDER_AUTH_BLOCKED,
    UNKNOWN_OUTCOME,
    EXPIRED,
    RETRY_EXHAUSTED
}

enum class BackendProviderTransportPhase { BEFORE_WRITE, MAY_HAVE_WRITTEN }

sealed interface BackendProviderRawObservation {
    data class Http(
        val statusCode: Int,
        val rawReason: String?,
        val retryAfterEpochSeconds: Double?,
        val providerRequestId: String?,
        val observedAtEpochSeconds: Long
    ) : BackendProviderRawObservation

    data class Transport(val phase: BackendProviderTransportPhase) : BackendProviderRawObservation

    /** A local credential/circuit rejection observed before provider I/O. */
    data object ProviderAuthRejected : BackendProviderRawObservation
}

data class BackendRawProviderObservationCommand(
    val reference: BackendDeliveryProviderRequest,
    val observation: BackendProviderRawObservation
)

data class BackendDeliveryRetryContext(
    val deliveryKey: DeliveryKey,
    val nowEpochSeconds: Long,
    val expiresAtEpochSeconds: Long,
    val attempt: Long,
    val maxAttempts: Long
)

fun interface BackendDeliveryJitterSource {
    fun sample(deliveryKey: DeliveryKey, nextAttempt: Long): Double
}

internal data class BackendClassifiedProviderObservation(
    val outcome: BackendDurableProviderOutcome,
    val reason: BackendPersistedProviderReason,
    val httpStatus: Int? = null,
    val acceptedAtEpochSeconds: Long? = null,
    val nextAttemptAtEpochSeconds: Long? = null,
    val providerRequestId: String? = null,
    val invalidationReason: DeviceRegistrationInvalidationReason? = null
)

/** Pure, closed classifier. Callers cannot nominate either a durable outcome or durable reason. */
internal object BackendDeliveryObservationClassifier {
    private const val MAX_RETRY_DELAY_SECONDS = 300L

    fun classify(
        observation: BackendProviderRawObservation,
        context: BackendDeliveryRetryContext,
        jitter: BackendDeliveryJitterSource
    ): BackendClassifiedProviderObservation = when (observation) {
        is BackendProviderRawObservation.Transport -> when (observation.phase) {
            BackendProviderTransportPhase.MAY_HAVE_WRITTEN -> BackendClassifiedProviderObservation(
                BackendDurableProviderOutcome.UNKNOWN_OUTCOME,
                BackendPersistedProviderReason.TRANSPORT_OUTCOME_UNKNOWN
            )

            BackendProviderTransportPhase.BEFORE_WRITE -> retry(
                reason = BackendPersistedProviderReason.TRANSPORT_BEFORE_WRITE,
                retryAfterEpochSeconds = null,
                context = context,
                jitter = jitter
            )
        }

        is BackendProviderRawObservation.Http -> classifyHttp(observation, context, jitter)
        BackendProviderRawObservation.ProviderAuthRejected -> BackendClassifiedProviderObservation(
            BackendDurableProviderOutcome.PROVIDER_AUTH_BLOCKED,
            BackendPersistedProviderReason.PROVIDER_AUTH_REJECTED
        )
    }

    private fun classifyHttp(
        observation: BackendProviderRawObservation.Http,
        context: BackendDeliveryRetryContext,
        jitter: BackendDeliveryJitterSource
    ): BackendClassifiedProviderObservation {
        val exactReason = observation.rawReason
        return when {
            observation.statusCode == 200 -> BackendClassifiedProviderObservation(
                BackendDurableProviderOutcome.ACCEPTED,
                BackendPersistedProviderReason.HTTP_200,
                httpStatus = observation.statusCode,
                acceptedAtEpochSeconds = observation.observedAtEpochSeconds,
                providerRequestId = observation.providerRequestId
            )

            observation.statusCode == 400 && exactReason in invalidTokenReasons400 ->
                BackendClassifiedProviderObservation(
                    BackendDurableProviderOutcome.INVALID_TOKEN,
                    BackendPersistedProviderReason.TOKEN_INVALID,
                    httpStatus = observation.statusCode,
                    providerRequestId = observation.providerRequestId,
                    invalidationReason = when (exactReason) {
                        "BadDeviceToken" -> DeviceRegistrationInvalidationReason.BAD_DEVICE_TOKEN
                        else -> DeviceRegistrationInvalidationReason.DEVICE_TOKEN_NOT_FOR_TOPIC
                    }
                )

            observation.statusCode == 410 && exactReason in invalidTokenReasons410 ->
                BackendClassifiedProviderObservation(
                    BackendDurableProviderOutcome.INVALID_TOKEN,
                    BackendPersistedProviderReason.TOKEN_INVALID,
                    httpStatus = observation.statusCode,
                    providerRequestId = observation.providerRequestId,
                    invalidationReason = when (exactReason) {
                        "ExpiredToken" -> DeviceRegistrationInvalidationReason.EXPIRED_TOKEN
                        else -> DeviceRegistrationInvalidationReason.UNREGISTERED
                    }
                )

            observation.statusCode == 400 && exactReason == "IdleTimeout" -> retry(
                BackendPersistedProviderReason.IDLE_TIMEOUT,
                observation.retryAfterEpochSeconds,
                context,
                jitter,
                observation.providerRequestId,
                observation.statusCode
            )

            observation.statusCode == 403 && exactReason == "ExpiredProviderToken" ->
                BackendClassifiedProviderObservation(
                    BackendDurableProviderOutcome.REFRESH_AUTH,
                    BackendPersistedProviderReason.PROVIDER_AUTH_REJECTED,
                    httpStatus = observation.statusCode,
                    providerRequestId = observation.providerRequestId
                )

            observation.statusCode == 429 && exactReason == "TooManyProviderTokenUpdates" ->
                BackendClassifiedProviderObservation(
                    BackendDurableProviderOutcome.PROVIDER_AUTH_BLOCKED,
                    BackendPersistedProviderReason.PROVIDER_AUTH_REJECTED,
                    httpStatus = observation.statusCode,
                    providerRequestId = observation.providerRequestId
                )

            observation.statusCode == 403 && exactReason == "InvalidProviderToken" -> BackendClassifiedProviderObservation(
                BackendDurableProviderOutcome.PROVIDER_AUTH_BLOCKED,
                BackendPersistedProviderReason.PROVIDER_AUTH_REJECTED,
                httpStatus = observation.statusCode,
                providerRequestId = observation.providerRequestId
            )

            observation.statusCode == 429 && exactReason == "TooManyRequests" -> retry(
                BackendPersistedProviderReason.TOO_MANY_REQUESTS,
                observation.retryAfterEpochSeconds,
                context,
                jitter,
                observation.providerRequestId,
                observation.statusCode
            )

            observation.statusCode in setOf(500, 503) -> retry(
                BackendPersistedProviderReason.HTTP_5XX,
                observation.retryAfterEpochSeconds,
                context,
                jitter,
                observation.providerRequestId,
                observation.statusCode
            )

            (observation.statusCode == 400 && exactReason in rejectedPayloadReasons400) ||
                observation.statusCode == 404 || observation.statusCode == 405 || observation.statusCode == 413 ->
                BackendClassifiedProviderObservation(
                BackendDurableProviderOutcome.REJECTED_PAYLOAD,
                BackendPersistedProviderReason.PAYLOAD_REJECTED,
                httpStatus = observation.statusCode,
                providerRequestId = observation.providerRequestId
            )

            else -> BackendClassifiedProviderObservation(
                BackendDurableProviderOutcome.UNKNOWN_OUTCOME,
                BackendPersistedProviderReason.UNKNOWN_PROVIDER_REASON,
                httpStatus = observation.statusCode,
                providerRequestId = observation.providerRequestId
            )
        }
    }

    private fun retry(
        reason: BackendPersistedProviderReason,
        retryAfterEpochSeconds: Double?,
        context: BackendDeliveryRetryContext,
        jitter: BackendDeliveryJitterSource,
        providerRequestId: String? = null,
        httpStatus: Int? = null
    ): BackendClassifiedProviderObservation {
        if (context.attempt == Long.MAX_VALUE) {
            return BackendClassifiedProviderObservation(
                BackendDurableProviderOutcome.RETRY_EXHAUSTED,
                BackendPersistedProviderReason.RETRY_BUDGET_EXHAUSTED,
                httpStatus = httpStatus,
                providerRequestId = providerRequestId
            )
        }
        val nextAttempt = context.attempt + 1
        if (nextAttempt >= context.maxAttempts) {
            return BackendClassifiedProviderObservation(
                BackendDurableProviderOutcome.RETRY_EXHAUSTED,
                BackendPersistedProviderReason.RETRY_BUDGET_EXHAUSTED,
                httpStatus = httpStatus,
                providerRequestId = providerRequestId
            )
        }
        val nextAt = if (retryAfterEpochSeconds != null) {
            val integral = retryAfterEpochSeconds.isFinite() &&
                retryAfterEpochSeconds == floor(retryAfterEpochSeconds) &&
                retryAfterEpochSeconds <= Long.MAX_VALUE.toDouble()
            if (!integral) return invalidRetryAfter(providerRequestId, httpStatus)
            val retryAt = retryAfterEpochSeconds.toLong()
            if (retryAt >= context.expiresAtEpochSeconds) {
                return BackendClassifiedProviderObservation(
                    BackendDurableProviderOutcome.EXPIRED,
                    BackendPersistedProviderReason.RETRY_WOULD_REACH_EXPIRY,
                    httpStatus = httpStatus,
                    providerRequestId = providerRequestId
                )
            }
            if (retryAt <= context.nowEpochSeconds || retryAt > safeAdd(context.nowEpochSeconds, MAX_RETRY_DELAY_SECONDS)) {
                return invalidRetryAfter(providerRequestId, httpStatus)
            }
            retryAt
        } else {
            val exponent = context.attempt.coerceAtMost(63).toInt()
            val cap = if (exponent >= 9) MAX_RETRY_DELAY_SECONDS else 2.0.pow(exponent).toLong()
            val rawSample = jitter.sample(context.deliveryKey, nextAttempt)
            val sample = if (rawSample.isFinite()) rawSample.coerceIn(0.0, 1.0) else 0.0
            val delay = floor(sample * cap).toLong().coerceIn(1L, MAX_RETRY_DELAY_SECONDS)
            safeAdd(context.nowEpochSeconds, delay)
        }
        if (nextAt >= context.expiresAtEpochSeconds) {
            return BackendClassifiedProviderObservation(
                BackendDurableProviderOutcome.EXPIRED,
                BackendPersistedProviderReason.RETRY_WOULD_REACH_EXPIRY,
                httpStatus = httpStatus,
                providerRequestId = providerRequestId
            )
        }
        return BackendClassifiedProviderObservation(
            BackendDurableProviderOutcome.RETRY,
            reason,
            httpStatus = httpStatus,
            nextAttemptAtEpochSeconds = nextAt,
            providerRequestId = providerRequestId
        )
    }

    private fun invalidRetryAfter(providerRequestId: String?, httpStatus: Int?) = BackendClassifiedProviderObservation(
        BackendDurableProviderOutcome.UNKNOWN_OUTCOME,
        BackendPersistedProviderReason.INVALID_RETRY_AFTER,
        httpStatus = httpStatus,
        providerRequestId = providerRequestId
    )

    private fun safeAdd(left: Long, right: Long): Long =
        if (left > Long.MAX_VALUE - right) Long.MAX_VALUE else left + right

    private val invalidTokenReasons400 = setOf("BadDeviceToken", "DeviceTokenNotForTopic")
    private val invalidTokenReasons410 = setOf("ExpiredToken", "Unregistered")
    private val rejectedPayloadReasons400 = setOf(
        "BadCollapseId", "BadExpirationDate", "BadMessageId", "BadPriority", "BadTopic",
        "BadPath", "MethodNotAllowed", "MissingTopic", "PayloadEmpty", "PayloadTooLarge"
    )
}

enum class BackendDecisionSyncStatus { ACKNOWLEDGED }
enum class BackendEffectDispatchStatus {
    NOT_DISPATCHED,
    PENDING_RECIPIENT,
    QUEUED,
    PARTIALLY_DISPATCHED,
    DISPATCHED,
    TERMINAL_FAILURE
}

data class BackendNotificationRecipientIntent(
    val participantId: String,
    val channel: String,
    val provider: String,
    val registrationIds: List<String>,
    val expiresAtEpochSeconds: Long
)

data class BackendNotificationIngestionCommand(
    val domainEventId: String,
    val effectType: String,
    val schemaVersion: Int,
    val logicalNotificationId: String,
    val recipients: List<BackendNotificationRecipientIntent>
)

data class BackendNotificationIngestionReceipt(
    val transactionId: String,
    val effectKey: EffectKey,
    val deliveryKeys: Set<DeliveryKey>,
    val created: Boolean,
    val decisionSyncStatus: BackendDecisionSyncStatus,
    val effectDispatchStatus: BackendEffectDispatchStatus
)

enum class BackendNotificationIngestionCheckpoint {
    AFTER_DOMAIN_EVENT_INGESTION_WRITE,
    AFTER_LOGICAL_NOTIFICATION_WRITE,
    AFTER_RECIPIENTS_WRITE,
    AFTER_DELIVERIES_WRITE
}

fun interface BackendNotificationIngestionFaultInjector {
    fun inject(checkpoint: BackendNotificationIngestionCheckpoint)
}

fun interface BackendNotificationIngestionCommittedPort {
    suspend fun committed(receipt: BackendNotificationIngestionReceipt)
}

enum class BackendRecipientTargetState { PENDING_TARGET, TARGETED, TARGET_EXPIRED, TARGET_EXHAUSTED }
enum class BackendRecipientTargetCheckpointKind { RETRY, FANOUT, EXPIRY, EXHAUSTION }
enum class BackendRecipientTargetFaultCheckpoint {
    BEFORE_FANOUT_COMMIT,
    AFTER_RECIPIENT_CAS,
    AFTER_DELIVERY_INSERTS,
    AFTER_RECEIPT_INSERT,
    AFTER_FANOUT_COMMIT
}

fun interface BackendRecipientTargetFaultInjector {
    fun inject(checkpoint: BackendRecipientTargetFaultCheckpoint)
}

data class BackendRecipientTargetLeaseRequest(
    val recipientKey: RecipientKey,
    val expectedCheckpointRevision: Long,
    val holderId: String,
    val expectedLeaseVersion: Long,
    val newLeaseVersion: Long,
    val fencingToken: Long,
    val expiresAtLogicalEpochSeconds: Long
)

data class BackendRecipientTargetLease(
    val holderId: String,
    val version: Long,
    val fencingToken: Long,
    val expiresAtLogicalEpochSeconds: Long
)

data class BackendRecipientTargetResolutionRequest(
    val recipientKey: RecipientKey,
    val registrationIds: List<String>,
    val jitterSample: Double,
    val holderId: String,
    val leaseVersion: Long,
    val fencingToken: Long
)

data class BackendRecipientTargetPlannedDelivery(
    val deliveryKey: DeliveryKey,
    val registrationId: String
)

data class BackendRecipientTargetCheckpoint(
    val kind: BackendRecipientTargetCheckpointKind,
    val effectId: String,
    val revision: Long,
    val transactionReceiptId: String,
    val holderId: String?,
    val leaseVersion: Long?,
    val fencingToken: Long?,
    val deliveries: List<BackendRecipientTargetPlannedDelivery> = emptyList(),
    val nextAttemptAtEpochSeconds: Long? = null,
    val effectRequested: Boolean = false
)

data class BackendRecipientTargetCheckpointReference(
    val recipientKey: RecipientKey,
    val effectId: String,
    val checkpointRevision: Long,
    val transactionReceiptId: String,
    val holderId: String,
    val leaseVersion: Long,
    val fencingToken: Long
) {
    constructor(
        recipientKey: RecipientKey,
        effectId: String,
        checkpointRevision: Long,
        transactionReceiptId: String,
        holderId: String?,
        leaseVersion: Long?,
        fencingToken: Long?,
        @Suppress("UNUSED_PARAMETER") nullableAuthority: Unit = Unit
    ) : this(
        recipientKey,
        effectId,
        checkpointRevision,
        transactionReceiptId,
        holderId ?: HOLDERLESS_TARGET_HOLDER,
        leaseVersion ?: HOLDERLESS_TARGET_LEASE,
        fencingToken ?: HOLDERLESS_TARGET_LEASE
    ) {
        require((holderId == null) == (leaseVersion == null) &&
            (holderId == null) == (fencingToken == null)) {
            "Target checkpoint authority must be either complete or holderless"
        }
    }

    val isHolderless: Boolean
        get() = holderId == HOLDERLESS_TARGET_HOLDER &&
            leaseVersion == HOLDERLESS_TARGET_LEASE && fencingToken == HOLDERLESS_TARGET_LEASE

    private companion object {
        const val HOLDERLESS_TARGET_HOLDER = ""
        const val HOLDERLESS_TARGET_LEASE = 0L
    }
}

data class BackendRecipientTargetSnapshot(
    val recipientKey: RecipientKey,
    val state: BackendRecipientTargetState,
    val attempt: Long,
    val nextAttemptAtEpochSeconds: Long?,
    val nowEpochSeconds: Long,
    val clockRevision: Long,
    val checkpointRevision: Long,
    val lastLeaseVersion: Long,
    val lastFencingToken: Long,
    val pendingCheckpoint: BackendRecipientTargetCheckpoint?,
    val deliveryKeys: Set<DeliveryKey>
)

data class BackendRecipientTargetCheckpointCasResult(
    val snapshot: BackendRecipientTargetSnapshot,
    val applied: Boolean,
    val transactionCommitted: Boolean
)

interface BackendRecipientTargetRuntime : AutoCloseable {
    suspend fun current(recipientKey: RecipientKey): BackendRecipientTargetSnapshot?
    suspend fun acquireLease(request: BackendRecipientTargetLeaseRequest): BackendRecipientTargetLease?
    suspend fun stageResolution(request: BackendRecipientTargetResolutionRequest): BackendRecipientTargetSnapshot
    suspend fun stageExpiry(recipientKey: RecipientKey): BackendRecipientTargetSnapshot
    suspend fun requestCheckpoint(reference: BackendRecipientTargetCheckpointReference): Boolean
    suspend fun acknowledgeCheckpoint(reference: BackendRecipientTargetCheckpointReference): BackendRecipientTargetSnapshot
    suspend fun acknowledgeCheckpointCas(
        reference: BackendRecipientTargetCheckpointReference
    ): BackendRecipientTargetCheckpointCasResult
    suspend fun advanceLogicalClock(
        recipientKey: RecipientKey,
        expectedClockRevision: Long,
        newEpochSeconds: Long
    ): BackendRecipientTargetSnapshot
}

enum class BackendDeliveryAuthority(val wireValue: String) { LEGACY("legacy"), OUTBOX_V2("outbox-v2") }

enum class BackendDurableDeliveryState {
    POLICY_CHECK,
    SUPPRESSED,
    QUEUED,
    DEFERRED_QUIET_HOURS,
    AWAITING_TOKEN,
    AUTH,
    SENDING,
    AWAITING_PROVIDER_RESULT_PERSISTENCE,
    RETRY_SCHEDULED,
    UNKNOWN_OUTCOME,
    ACCEPTED_BY_APNS,
    INVALID_TOKEN,
    REJECTED_PAYLOAD,
    PROVIDER_AUTH_BLOCKED,
    EXPIRED,
    RETRY_EXHAUSTED,
    CANCELLED
}

data class BackendDeliveryPolicyContext(
    val deliveryKey: DeliveryKey,
    val registrationId: String,
    val nowEpochSeconds: Long,
    val expiresAtEpochSeconds: Long
)

sealed interface BackendDeliveryPolicyDecision {
    data object ALLOW : BackendDeliveryPolicyDecision
    data class QuietUntil(val epochSeconds: Long) : BackendDeliveryPolicyDecision
    data object SUPPRESS : BackendDeliveryPolicyDecision
}

fun interface BackendDeliveryWorkerClock { fun epochSeconds(): Long }
fun interface BackendDeliveryPolicyPort { suspend fun decide(context: BackendDeliveryPolicyContext): BackendDeliveryPolicyDecision }
fun interface BackendDeliveryTokenAvailabilityPort { suspend fun isAvailable(registrationId: String): Boolean }

interface BackendDeliveryCredentialPort {
    suspend fun credentialVersion(): String
    suspend fun refreshAfterProviderRejection(expectedVersion: String): Boolean
    suspend fun refreshAfterProviderRejection(expectedVersion: String, idempotencyKey: String): Boolean =
        refreshAfterProviderRejection(expectedVersion)
}

data class BackendValidatedDeliveryCredential(val version: String, val fingerprint: String)

fun interface BackendValidatedDeliveryCredentialPort {
    suspend fun validatedCredential(): BackendValidatedDeliveryCredential
}

data class BackendDeliveryProviderRequest(
    val deliveryKey: DeliveryKey,
    val registrationId: String,
    val apnsId: String,
    val correlationId: String,
    val attempt: Long,
    val leaseHolderId: String,
    val leaseVersion: Long,
    val leaseFencingToken: Long
)

fun interface BackendDeliveryProviderPort {
    suspend fun send(request: BackendDeliveryProviderRequest): BackendProviderRawObservation
}

enum class BackendDeliveryWorkerFaultCheckpoint {
    AFTER_PROVIDER_OBSERVATION_DURABLE,
    AFTER_PROVIDER_RESULT_COMMIT
}

fun interface BackendDeliveryWorkerFaultInjector {
    fun inject(checkpoint: BackendDeliveryWorkerFaultCheckpoint)
}

data class BackendDeliveryLease(
    val holderId: String,
    val version: Long,
    val fencingToken: Long,
    val expiresAtLogicalEpochSeconds: Long
)

data class BackendDeliveryLeaseLostCommand(
    val deliveryKey: DeliveryKey,
    val correlationId: String,
    val attempt: Long,
    val leaseHolderId: String,
    val leaseVersion: Long,
    val leaseFencingToken: Long
)

data class BackendDeliveryProviderCheckpoint(
    val effectId: String,
    val revision: Long,
    val outcome: BackendDurableProviderOutcome,
    val reason: BackendPersistedProviderReason,
    val httpStatus: Int? = null,
    val invalidationReason: DeviceRegistrationInvalidationReason? = null,
    val acceptedAtEpochSeconds: Long?,
    val nextAttemptAtEpochSeconds: Long?,
    val providerRequestId: String?,
    val leaseHolderId: String?,
    val leaseVersion: Long?,
    val leaseFencingToken: Long?,
    val effectRequested: Boolean
)

data class BackendDeliveryCheckpointReference(
    val deliveryKey: DeliveryKey,
    val effectId: String,
    val checkpointRevision: Long,
    val authority: DeliveryAuthority?,
    val authorityFencingToken: Long,
    val leaseHolderId: String?,
    val leaseVersion: Long?,
    val leaseFencingToken: Long?
)

data class BackendNotificationDeliverySnapshot(
    val deliveryKey: DeliveryKey,
    val registrationId: String,
    val state: BackendDurableDeliveryState,
    val attempt: Long,
    val maxAttempts: Long,
    val nextAttemptAtEpochSeconds: Long?,
    val expiresAtEpochSeconds: Long,
    val nowEpochSeconds: Long,
    val clockRevision: Long,
    val checkpointRevision: Long,
    val authority: DeliveryAuthority?,
    val authorityFencingToken: Long,
    val lastLeaseVersion: Long,
    val lastLeaseFencingToken: Long,
    val pendingCheckpoint: BackendDeliveryProviderCheckpoint?,
    val providerCheckpointCount: Long,
    val acceptedAtEpochSeconds: Long?,
    val providerReason: BackendPersistedProviderReason?,
    val credentialVersion: String?,
    val authRefreshCount: Long,
    val correlationId: String? = null,
    val lease: BackendDeliveryLease? = null
) {
    val refreshUsed: Boolean
        get() = authRefreshCount > 0
}

interface BackendDeliveryRuntime : AutoCloseable {
    suspend fun current(deliveryKey: DeliveryKey): BackendNotificationDeliverySnapshot?
    suspend fun dueDeliveryKeys(authority: BackendDeliveryAuthority): List<DeliveryKey>
    suspend fun acquireLease(deliveryKey: DeliveryKey, holderId: String): BackendDeliveryLease?
    suspend fun markLeaseLost(command: BackendDeliveryLeaseLostCommand): BackendNotificationDeliverySnapshot
    suspend fun markQuietHours(
        deliveryKey: DeliveryKey,
        expectedCheckpointRevision: Long,
        authority: DeliveryAuthority?,
        authorityFencingToken: Long,
        nextEligibleAtEpochSeconds: Long
    ): BackendNotificationDeliverySnapshot
    suspend fun markNoActiveToken(
        deliveryKey: DeliveryKey,
        expectedCheckpointRevision: Long,
        authority: DeliveryAuthority?,
        authorityFencingToken: Long
    ): BackendNotificationDeliverySnapshot
    suspend fun markPolicyAllowed(
        deliveryKey: DeliveryKey,
        expectedCheckpointRevision: Long,
        authority: DeliveryAuthority?,
        authorityFencingToken: Long
    ): BackendNotificationDeliverySnapshot
    suspend fun markProviderAuthReady(
        deliveryKey: DeliveryKey,
        correlationId: String,
        attempt: Long,
        leaseHolderId: String,
        leaseVersion: Long,
        leaseFencingToken: Long
    ): BackendNotificationDeliverySnapshot
    suspend fun suppressBeforeLease(
        deliveryKey: DeliveryKey,
        expectedCheckpointRevision: Long,
        authority: DeliveryAuthority?,
        authorityFencingToken: Long
    ): BackendNotificationDeliverySnapshot
    suspend fun cancelBeforeWrite(
        deliveryKey: DeliveryKey,
        expectedCheckpointRevision: Long,
        authority: DeliveryAuthority?,
        authorityFencingToken: Long
    ): BackendNotificationDeliverySnapshot
    suspend fun stageSendingCancellation(
        deliveryKey: DeliveryKey,
        correlationId: String,
        attempt: Long,
        leaseHolderId: String,
        leaseVersion: Long,
        leaseFencingToken: Long
    ): BackendNotificationDeliverySnapshot
    suspend fun stageExpiry(
        deliveryKey: DeliveryKey,
        expectedCheckpointRevision: Long,
        authority: DeliveryAuthority?,
        authorityFencingToken: Long,
        leaseHolderId: String?,
        leaseVersion: Long?,
        leaseFencingToken: Long?
    ): BackendNotificationDeliverySnapshot
    suspend fun deferQuietHours(deliveryKey: DeliveryKey, lease: BackendDeliveryLease, untilEpochSeconds: Long): Boolean
    suspend fun markAwaitingToken(deliveryKey: DeliveryKey, lease: BackendDeliveryLease): Boolean
    suspend fun blockedCredentialVersion(): String?
    suspend fun blockedCredential(): BackendValidatedDeliveryCredential?
    suspend fun blockCredentialVersion(credentialVersion: String): Boolean
    suspend fun blockCredential(credential: BackendValidatedDeliveryCredential): Boolean
    suspend fun clearCredentialCircuit(newCredentialVersion: String): Boolean
    suspend fun clearCredentialCircuit(credential: BackendValidatedDeliveryCredential): Boolean
    suspend fun requestProviderCheckpoint(reference: BackendDeliveryCheckpointReference): Boolean
    suspend fun acknowledgeProviderCheckpoint(reference: BackendDeliveryCheckpointReference): BackendNotificationDeliverySnapshot
    suspend fun advanceLogicalClock(deliveryKey: DeliveryKey, expectedClockRevision: Long, newEpochSeconds: Long): BackendNotificationDeliverySnapshot
    suspend fun deliveriesForRegistration(registrationId: String): List<DeliveryKey>
}

/** Compatibility bridge kept outside the public runtime port while historical model tests migrate. */
@JvmSynthetic
internal suspend fun BackendDeliveryRuntime.stageProviderObservation(
    deliveryKey: DeliveryKey,
    lease: BackendDeliveryLease,
    observation: BackendClassifiedProviderObservation,
    credentialVersion: String?
): BackendNotificationDeliverySnapshot =
    (this as? SqliteBackendDeliveryRuntime)?.stageClassifiedProviderObservation(
        deliveryKey, lease, observation, credentialVersion, null
    ) ?: error("The durable SQLite delivery runtime is required")

interface BackendNotificationDeliveryRuntimeFactory : BackendNotificationDeliveryStoreFactory {
    fun openRecipientTargetRuntime(
        faultInjector: BackendRecipientTargetFaultInjector = BackendRecipientTargetFaultInjector { }
    ): BackendRecipientTargetRuntime

    fun openDeliveryRuntime(
        faultInjector: BackendDeliveryWorkerFaultInjector = BackendDeliveryWorkerFaultInjector { }
    ): BackendDeliveryRuntime
}

data class BackendDeliveryRecoveryReport(val claimedDeliveryKeys: Set<DeliveryKey>)
