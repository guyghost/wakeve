package com.guyghost.wakeve.notification

/** Explicit APNs runtime selection. There is deliberately no implicit environment fallback. */
enum class APNsEnvironment(val authority: String) {
    SANDBOX("api.sandbox.push.apple.com:443"),
    PRODUCTION("api.push.apple.com:443")
}

/** Validated, secret-bearing provider configuration. Its string representation is always redacted. */
class APNsProviderConfig private constructor(
    val keyId: String,
    val teamId: String,
    val authKey: String,
    val topic: String,
    val environment: APNsEnvironment
) {
    override fun toString(): String = "APNsProviderConfig(environment=$environment, topic=[redacted])"

    companion object {
        fun create(
            keyId: String?,
            teamId: String?,
            authKey: String?,
            topic: String?,
            environment: String?
        ): Result<APNsProviderConfig> = runCatching {
            val parsedEnvironment = when (environment?.trim()?.lowercase()) {
                "sandbox", "development" -> APNsEnvironment.SANDBOX
                "production" -> APNsEnvironment.PRODUCTION
                else -> error("APNS_ENVIRONMENT must be explicitly set to sandbox or production")
            }
            APNsProviderConfig(
                keyId = keyId.required("APNS_KEY_ID"),
                teamId = teamId.required("APNS_TEAM_ID"),
                authKey = authKey.required("APNS_AUTH_KEY"),
                topic = topic.required("APNS_BUNDLE_ID"),
                environment = parsedEnvironment
            )
        }

        private fun String?.required(name: String): String =
            this?.takeIf { it.isNotBlank() } ?: error("$name is required")
    }
}

fun interface APNsProviderClock {
    fun epochSeconds(): Long
}

class APNsProviderToken internal constructor(
    internal val authorizationValue: String,
    val issuedAtEpochSeconds: Long
) {
    override fun toString(): String = "APNsProviderToken(authorizationValue=[redacted], issuedAtEpochSeconds=$issuedAtEpochSeconds)"
}

fun interface APNsTokenSigner {
    suspend fun sign(config: APNsProviderConfig, clock: APNsProviderClock): Result<APNsProviderToken>
}

data class APNsHttp2Request(
    val authority: String,
    val path: String,
    val method: String = "POST",
    val headers: Map<String, String>,
    val body: String,
    val correlationId: String,
    val mayBeWritten: Boolean = false
)

data class APNsHttp2Response(
    val statusCode: Int,
    val headers: Map<String, String> = emptyMap(),
    val reason: String? = null,
    val apnsId: String? = null,
    val receivedAtEpochSeconds: Long
)

sealed interface APNsTransportResult {
    data class Response(val value: APNsHttp2Response) : APNsTransportResult
    data class FailedBeforeWrite(val diagnostic: APNsSanitizedDiagnostic) : APNsTransportResult
    data class OutcomeUnknown(val diagnostic: APNsSanitizedDiagnostic) : APNsTransportResult
}

fun interface APNsHttp2Transport {
    suspend fun execute(request: APNsHttp2Request): APNsTransportResult
}

enum class APNsProviderOutcome {
    ACCEPTED,
    INVALID_TOKEN,
    REJECTED_PAYLOAD,
    RETRY,
    REFRESH_AUTH,
    PROVIDER_AUTH_BLOCKED,
    UNKNOWN_OUTCOME,
    UNKNOWN_TERMINAL
}

data class APNsRetryMetadata(
    val retryAfterEpochSeconds: Long?,
    val nextAttemptAtEpochSeconds: Long?,
    val expiresAtEpochSeconds: Long,
    val attempt: Int,
    val maxAttempts: Int
)

data class APNsProviderClassification(
    val outcome: APNsProviderOutcome,
    val statusCode: Int?,
    val sanitizedReason: String?,
    val apnsId: String?,
    val acceptedAtEpochSeconds: Long?,
    val retry: APNsRetryMetadata? = null
)

data class APNsProviderRequest(
    val deliveryKey: DeliveryKey,
    val apnsId: String,
    val deviceToken: String,
    val payload: String,
    val expirationEpochSeconds: Long,
    val priority: Int,
    val pushType: String
)

data class APNsProviderResult(
    val classification: APNsProviderClassification,
    val diagnostic: APNsSanitizedDiagnostic
)

/** Only this already-sanitized shape may cross the provider's operational diagnostic boundary. */
data class APNsSanitizedDiagnostic(
    val deliveryKey: String,
    val correlationId: String,
    val errorClass: String? = null,
    val statusCode: Int? = null,
    val reasonCode: String? = null
)

@JvmInline value class EffectKey(val value: String)
@JvmInline value class RecipientKey(val value: String)
@JvmInline value class DeliveryKey(val value: String)

enum class BackendRecipientStatus { PENDING_TARGET, TARGETED, EXPIRED }

enum class BackendDeliveryStatus {
    QUEUED, LEASED, RETRY, UNKNOWN_OUTCOME, ACCEPTED_BY_APNS, INVALID_TOKEN,
    REJECTED_PAYLOAD, PROVIDER_AUTH_BLOCKED, EXPIRED, RETRY_EXHAUSTED, CANCELLED, UNKNOWN_TERMINAL
}

data class BackendNotificationRecipient(
    val recipientKey: RecipientKey,
    val effectKey: EffectKey,
    val status: BackendRecipientStatus,
    val installationIds: Set<String>,
    val expiresAtEpochSeconds: Long
)

data class BackendNotificationDelivery(
    val deliveryKey: DeliveryKey,
    val recipientKey: RecipientKey,
    val installationId: String,
    val provider: String,
    val status: BackendDeliveryStatus,
    val attempt: Int,
    val nextAttemptAtEpochSeconds: Long?,
    val expiresAtEpochSeconds: Long,
    val leaseOwner: String? = null,
    val leaseExpiresAtEpochSeconds: Long? = null
)

data class BackendEnqueueResult(val delivery: BackendNotificationDelivery, val created: Boolean)

/** Backend-owned persistence port. Local SQLDelight is intentionally not an implementation of this port. */
interface BackendNotificationDeliveryStore {
    suspend fun persistPendingRecipient(recipient: BackendNotificationRecipient): Boolean
    suspend fun recipient(recipientKey: RecipientKey): BackendNotificationRecipient?
    suspend fun registerInstallation(recipientKey: RecipientKey, installationId: String): Boolean
    suspend fun enqueue(delivery: BackendNotificationDelivery): BackendEnqueueResult?
    suspend fun delivery(deliveryKey: DeliveryKey): BackendNotificationDelivery?
    suspend fun deliveryCount(deliveryKey: DeliveryKey): Int
    suspend fun acquireLease(deliveryKey: DeliveryKey, owner: String, nowEpochSeconds: Long, leaseUntilEpochSeconds: Long): Boolean
    suspend fun recordRetry(deliveryKey: DeliveryKey, attempt: Int, nextAttemptAtEpochSeconds: Long): Boolean
    suspend fun isEligible(deliveryKey: DeliveryKey, nowEpochSeconds: Long): Boolean
}

fun interface BackendNotificationDeliveryStoreFactory {
    fun open(): BackendNotificationDeliveryStore
}
