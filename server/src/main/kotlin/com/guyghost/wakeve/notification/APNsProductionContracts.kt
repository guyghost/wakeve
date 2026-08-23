package com.guyghost.wakeve.notification

import java.math.BigInteger
import java.security.KeyFactory
import java.security.interfaces.ECPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64

/** Explicit APNs runtime selection. There is deliberately no implicit environment fallback. */
enum class APNsEnvironment(val authority: String) {
    SANDBOX("api.sandbox.push.apple.com:443"),
    PRODUCTION("api.push.apple.com:443")
}

/** Validated, secret-bearing provider configuration. Its string representation is always redacted. */
class APNsProviderConfig private constructor(
    val keyId: String,
    val teamId: String,
    internal val authKey: String,
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
            val privateKey = authKey.required("APNS_AUTH_KEY")
            validateP256Pkcs8(privateKey)
            APNsProviderConfig(
                keyId = keyId.required("APNS_KEY_ID"),
                teamId = teamId.required("APNS_TEAM_ID"),
                authKey = privateKey,
                topic = topic.required("APNS_BUNDLE_ID"),
                environment = parsedEnvironment
            )
        }

        private fun String?.required(name: String): String =
            this?.takeIf { it.isNotBlank() } ?: error("$name is required")

        internal fun parseP256Pkcs8(pem: String): ECPrivateKey {
            val encoded = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace(Regex("\\s"), "")
                .let { Base64.getDecoder().decode(it) }
            return KeyFactory.getInstance("EC").generatePrivate(PKCS8EncodedKeySpec(encoded)) as? ECPrivateKey
                ?: error("APNS_AUTH_KEY must be an EC PKCS#8 private key")
        }

        private fun validateP256Pkcs8(pem: String) {
            val privateKey = runCatching { parseP256Pkcs8(pem) }
                .getOrElse { error("APNS_AUTH_KEY must be a valid P-256 PKCS#8 private key") }
            val p256Order = BigInteger("FFFFFFFF00000000FFFFFFFFFFFFFFFFBCE6FAADA7179E84F3B9CAC2FC632551", 16)
            check(privateKey.params.order == p256Order) { "APNS_AUTH_KEY must use P-256" }
        }
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
) {
    override fun toString(): String =
        "APNsHttp2Request(authority=$authority, path=[redacted], method=$method, " +
            "headers=[redacted], body=[redacted], correlationId=$correlationId, " +
            "mayBeWritten=$mayBeWritten)"
}

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
    val retry: APNsRetryMetadata? = null,
    val retryDirective: APNsProviderRetryDirective? = null
)

/** Provider data only; the delivery machine owns all retry scheduling and expiry decisions. */
data class APNsProviderRetryDirective(val retryAfterEpochSeconds: Long?)

data class APNsProviderRequest(
    val deliveryKey: DeliveryKey,
    val apnsId: String,
    val deviceToken: String,
    val payload: String,
    val expirationEpochSeconds: Long,
    val priority: Int,
    val pushType: String
) {
    override fun toString(): String =
        "APNsProviderRequest(deliveryKey=$deliveryKey, apnsId=$apnsId, " +
            "deviceToken=[redacted], payload=[redacted], " +
            "expirationEpochSeconds=$expirationEpochSeconds, priority=$priority, pushType=$pushType)"
}

data class APNsProviderResult(
    val classification: APNsProviderClassification,
    val diagnostic: APNsSanitizedDiagnostic
)

sealed interface APNsProviderMachineEvent {
    data class ResponseReceived(
        val statusCode: Int,
        val reason: String?,
        val retryAfterEpochSeconds: Long?,
        val correlationId: String
    ) : APNsProviderMachineEvent
    data class TransportFailedBeforeWrite(val correlationId: String) : APNsProviderMachineEvent
    data class TransportOutcomeUnknown(val correlationId: String) : APNsProviderMachineEvent
}

object APNsProviderMachineEventAdapter {
    fun toMachineEvent(result: APNsProviderResult): APNsProviderMachineEvent = when (result.classification.statusCode) {
        null -> when (result.classification.outcome) {
            APNsProviderOutcome.UNKNOWN_OUTCOME -> APNsProviderMachineEvent.TransportOutcomeUnknown(result.diagnostic.correlationId)
            else -> APNsProviderMachineEvent.TransportFailedBeforeWrite(result.diagnostic.correlationId)
        }
        else -> APNsProviderMachineEvent.ResponseReceived(
            statusCode = result.classification.statusCode,
            reason = result.classification.sanitizedReason,
            retryAfterEpochSeconds = result.classification.retryDirective?.retryAfterEpochSeconds,
            correlationId = result.diagnostic.correlationId
        )
    }
}

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
@JvmInline value class CalendarArtifactKey(val value: String)
data class DeliveryAuthority(val value: String) {
    init {
        require(value == "legacy" || value == "outbox-v2") { "Unsupported delivery authority" }
    }
}

/**
 * The confirmation envelope is accepted before any participant effect can be sent.
 *
 * `DISABLED` is the production default: the backend may persist and acknowledge an
 * envelope, but it must not resolve recipients, create calendar artifacts, or invoke a
 * provider. Later rollout stages can shadow-write projections before enabling sends.
 */
enum class ConfirmationFanOutReadiness {
    DISABLED,
    SHADOW_WRITE,
    ENABLED
}

enum class BackendRecipientStatus { PENDING_TARGET, TARGETED, EXPIRED }

enum class BackendRecipientTerminalReason {
    EXPIRED_WITHOUT_TARGET,
    RETRY_EXHAUSTED
}

enum class BackendDeliveryStatus {
    POLICY_CHECK, SUPPRESSED, QUEUED, LEASED, RETRY, DEFERRED_QUIET_HOURS, AWAITING_TOKEN, AUTH, SENDING,
    AWAITING_PROVIDER_RESULT_PERSISTENCE, RETRY_SCHEDULED,
    UNKNOWN_OUTCOME, ACCEPTED_BY_APNS, INVALID_TOKEN,
    REJECTED_PAYLOAD, PROVIDER_AUTH_BLOCKED, EXPIRED, RETRY_EXHAUSTED, CANCELLED, UNKNOWN_TERMINAL
}

data class BackendNotificationRecipient(
    val recipientKey: RecipientKey,
    val effectKey: EffectKey,
    val status: BackendRecipientStatus,
    val registrationIds: Set<String>,
    val expiresAtEpochSeconds: Long
)

data class BackendNotificationDelivery(
    val deliveryKey: DeliveryKey,
    val recipientKey: RecipientKey,
    val registrationId: String,
    val provider: String,
    val status: BackendDeliveryStatus,
    val attempt: Int,
    val nextAttemptAtEpochSeconds: Long?,
    val expiresAtEpochSeconds: Long,
    val leaseOwner: String? = null,
    val leaseExpiresAtEpochSeconds: Long? = null,
    val logicalNotificationId: String = deliveryKey.value,
    val idempotencyKey: String = deliveryKey.value,
    val acceptedAtEpochSeconds: Long? = null,
    val providerStatus: Int? = null,
    val providerReason: BackendPersistedProviderReason? = null,
    val providerRequestId: String? = null
)

data class BackendEnqueueResult(val delivery: BackendNotificationDelivery, val created: Boolean)

internal fun persistedProviderReasonFromLegacy(value: String): BackendPersistedProviderReason =
    runCatching { BackendPersistedProviderReason.valueOf(value) }.getOrElse {
        when (value.trim().lowercase()) {
            "success" -> BackendPersistedProviderReason.HTTP_200
            "idleTimeout".lowercase() -> BackendPersistedProviderReason.IDLE_TIMEOUT
            else -> BackendPersistedProviderReason.UNKNOWN_PROVIDER_REASON
        }
    }

data class BackendRecipientTerminalAcknowledgement(
    val recipientKey: RecipientKey,
    val reason: BackendRecipientTerminalReason,
    val acknowledgedAtEpochSeconds: Long
)

/** Backend-owned persistence port. Local SQLDelight is intentionally not an implementation of this port. */
interface BackendNotificationDeliveryStore : AutoCloseable {
    suspend fun persistPendingRecipient(recipient: BackendNotificationRecipient): Boolean
    suspend fun recipient(recipientKey: RecipientKey): BackendNotificationRecipient?
    suspend fun registerRegistration(recipientKey: String, registrationId: String): Boolean
    suspend fun delivery(deliveryKey: DeliveryKey): BackendNotificationDelivery?
    suspend fun deliveryCount(deliveryKey: DeliveryKey): Int
    suspend fun isEligible(deliveryKey: DeliveryKey, nowEpochSeconds: Long): Boolean

    /**
     * A delivery authority is durable and unique. It is intentionally separate from the
     * short-lived worker lease so a restart or a lease expiry cannot authorize two senders.
     */
    suspend fun acquireDeliveryAuthority(deliveryKey: String, authority: DeliveryAuthority): Boolean

    /**
     * Resolves a recipient that may have no registered installations yet. This does not
     * enqueue a provider delivery; rollout readiness owns that separate decision.
     */
    suspend fun resolvePendingRecipient(recipientKey: String, nowEpochSeconds: Long): BackendRecipientStatus?

    /** Persists a terminal zero-target/retry acknowledgement without creating a delivery. */
    suspend fun recordRecipientTerminalAcknowledgement(
        acknowledgement: BackendRecipientTerminalAcknowledgement
    ): Boolean

    override fun close() = Unit
}

/** Type-safe Kotlin conveniences while keeping the backend port inspectable from Java tooling. */
suspend fun BackendNotificationDeliveryStore.acquireDeliveryAuthority(
    deliveryKey: DeliveryKey,
    authority: DeliveryAuthority
): Boolean = acquireDeliveryAuthority(deliveryKey.value, authority)

suspend fun BackendNotificationDeliveryStore.registerRegistration(
    recipientKey: RecipientKey,
    registrationId: String
): Boolean = registerRegistration(recipientKey.value, registrationId)

suspend fun BackendNotificationDeliveryStore.resolvePendingRecipient(
    recipientKey: RecipientKey,
    nowEpochSeconds: Long
): BackendRecipientStatus? = resolvePendingRecipient(recipientKey.value, nowEpochSeconds)

fun interface BackendNotificationDeliveryStoreFactory {
    fun open(): BackendNotificationDeliveryStore
}
