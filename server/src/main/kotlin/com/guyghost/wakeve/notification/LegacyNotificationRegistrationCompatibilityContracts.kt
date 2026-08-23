package com.guyghost.wakeve.notification

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64

enum class LegacyCompatibilityOperation {
    REGISTER,
    UNREGISTER
}

enum class LegacyCompatibilityClientGeneration {
    N,
    N_MINUS_1
}

enum class LegacyCompatibilitySagaState {
    WRITING_LEGACY,
    WRITING_V2,
    RECORDING_RETRY,
    RETRY_WAIT,
    RECORDING_CONVERGENCE,
    RECORDING_BLOCK,
    CONVERGED,
    BLOCKED
}

enum class LegacyCompatibilityReconciliationStatus {
    PENDING,
    CONVERGED,
    BLOCKED
}

enum class LegacyCompatibilityResponseDisposition {
    RECONCILIATION_ACCEPTED,
    CONVERGED_SUCCESS,
    BLOCKED_FAILURE
}

enum class LegacyCompatibilityEffectCheckpoint {
    WRITE_LEGACY_STORE,
    WRITE_V2_REGISTRATION_STORE,
    PERSIST_RETRY_SCHEDULE,
    PERSIST_CONVERGENCE,
    PERSIST_BLOCKED_TERMINAL
}

enum class LegacyCompatibilityWriteOutcome {
    APPLIED,
    ALREADY_APPLIED
}

enum class LegacyCompatibilityWriteStatus {
    PENDING,
    APPLIED,
    NOT_REQUIRED
}

enum class LegacyCompatibilityV2TargetKind {
    EXACT_REGISTRATION_OR_INSTALLATION,
    LEGACY_DETERMINISTIC_INSTALLATION_ONLY
}

enum class LegacyCompatibilityFailure(val retryable: Boolean) {
    TRANSIENT(true),
    UNAVAILABLE(true),
    MISCONFIGURED(false),
    CONFLICT(false)
}

internal enum class LegacyCompatibilityLeaseClaimStrategy {
    /** The SQLite write lock is held before the authoritative snapshot is read. */
    SERIALIZED_MUTATION_AUTHORITY_ALREADY_HELD,

    /** Reserved for stores that validate first and then use an exact optimistic SQL CAS. */
    OPTIMISTIC_CAS_STILL_REQUIRED
}

internal interface LegacyCompatibilityLeaseClaimProbe {
    suspend fun attemptStarted(request: LegacyCompatibilityRecoveryLeaseRequest) = Unit

    suspend fun validatedRead(
        request: LegacyCompatibilityRecoveryLeaseRequest,
        strategy: LegacyCompatibilityLeaseClaimStrategy
    ) = Unit
}

internal object NoOpLegacyCompatibilityLeaseClaimProbe : LegacyCompatibilityLeaseClaimProbe

data class LegacyCompatibilityIdentity(
    val installationId: String,
    val registrationId: String
)

data class LegacyCompatibilityEffectReference(
    val effectId: String,
    val checkpoint: LegacyCompatibilityEffectCheckpoint,
    val checkpointRevision: Long,
    val fencingToken: Long
)

data class LegacyCompatibilityRecoveryLeaseRequest(
    val sagaId: String,
    val expectedEffectId: String,
    val effectCheckpoint: LegacyCompatibilityEffectCheckpoint,
    val checkpointRevision: Long,
    val holderId: String,
    val expectedLeaseVersion: Long,
    val newLeaseVersion: Long,
    val fencingToken: Long,
    val expiresAtEpochSeconds: Long
)

data class LegacyCompatibilityRecoveryRequest(
    val sagaId: String,
    val leaseId: String,
    val holderId: String,
    val leaseVersion: Long,
    val fencingToken: Long,
    val expiresAtEpochSeconds: Long,
    val expectedEffectId: String,
    val effectCheckpoint: LegacyCompatibilityEffectCheckpoint,
    val checkpointRevision: Long
)

data class LegacyCompatibilityRecoveryLease(
    val sagaId: String,
    val leaseId: String,
    val holderId: String,
    val leaseVersion: Long,
    val fencingToken: Long,
    val expiresAtEpochSeconds: Long,
    val expectedEffectId: String,
    val effectCheckpoint: LegacyCompatibilityEffectCheckpoint,
    val checkpointRevision: Long,
    val effectEmitted: Boolean
) {
    fun asRecoveryRequest(): LegacyCompatibilityRecoveryRequest =
        LegacyCompatibilityRecoveryRequest(
            sagaId = sagaId,
            leaseId = leaseId,
            holderId = holderId,
            leaseVersion = leaseVersion,
            fencingToken = fencingToken,
            expiresAtEpochSeconds = expiresAtEpochSeconds,
            expectedEffectId = expectedEffectId,
            effectCheckpoint = effectCheckpoint,
            checkpointRevision = checkpointRevision
        )
}

@ConsistentCopyVisibility
data class LegacyCompatibilityCommand private constructor(
    val sagaId: String,
    val operation: LegacyCompatibilityOperation,
    val clientGeneration: LegacyCompatibilityClientGeneration,
    val authenticatedUserId: String,
    val platform: Platform,
    val legacyPrimaryKeyFingerprint: String?,
    val legacyInstallationId: String?,
    val legacyRegistrationId: String?,
    val targetInstallationId: String,
    val targetRegistrationId: String?,
    val tokenFingerprint: String?,
    val compatibilityGeneration: Long,
    val maxAttemptsPerStore: Int,
    val initialNowEpochSeconds: Long,
    val scope: DeviceRegistrationScope,
    val requestKey: String
) {
    companion object {
        fun create(
            sagaId: String?,
            operation: LegacyCompatibilityOperation,
            clientGeneration: LegacyCompatibilityClientGeneration,
            authenticatedUserId: String?,
            platform: Platform,
            legacyPrimaryKeyFingerprint: String?,
            legacyInstallationId: String?,
            legacyRegistrationId: String?,
            targetInstallationId: String?,
            targetRegistrationId: String? = null,
            tokenFingerprint: String?,
            compatibilityGeneration: Long,
            maxAttemptsPerStore: Int,
            initialNowEpochSeconds: Long,
            scope: DeviceRegistrationScope = DeviceRegistrationScope.create(
                APNsEnvironment.PRODUCTION,
                LEGACY_COMPATIBILITY_IOS_TOPIC
            ).getOrThrow()
        ): Result<LegacyCompatibilityCommand> = runCatching {
            val normalizedSagaId = sagaId.requiredCompatibilityField("sagaId")
            val normalizedUserId = authenticatedUserId.requiredCompatibilityField("authenticatedUserId")
            val normalizedTargetInstallationId =
                targetInstallationId.requiredCompatibilityField("targetInstallationId")
            val normalizedLegacyPrimaryKeyFingerprint = legacyPrimaryKeyFingerprint.optionalCompatibilityField()
            val normalizedLegacyInstallationId = legacyInstallationId.optionalCompatibilityField()
            val normalizedLegacyRegistrationId = legacyRegistrationId.optionalCompatibilityField()
            val normalizedTargetRegistrationId = targetRegistrationId.optionalCompatibilityField()
            val normalizedTokenFingerprint = tokenFingerprint.optionalCompatibilityField()

            require(platform == Platform.IOS) { "Only iOS compatibility registrations are supported" }
            require(compatibilityGeneration > 0) { "compatibilityGeneration must be positive" }
            require(maxAttemptsPerStore > 0) { "maxAttemptsPerStore must be positive" }
            require(initialNowEpochSeconds >= 0) { "initialNowEpochSeconds must be non-negative" }
            if (operation == LegacyCompatibilityOperation.REGISTER) {
                require(normalizedTokenFingerprint != null) { "A token fingerprint is required for registration" }
            } else {
                require(normalizedTokenFingerprint == null) { "Unregistration must not carry a token fingerprint" }
            }
            if (clientGeneration == LegacyCompatibilityClientGeneration.N_MINUS_1) {
                require(normalizedLegacyPrimaryKeyFingerprint != null) {
                    "The legacy primary-key fingerprint is required for N-1"
                }
                require(normalizedLegacyInstallationId != null) {
                    "The legacy installation identity is required for N-1"
                }
                require(normalizedLegacyRegistrationId != null) {
                    "The legacy registration identity is required for N-1"
                }
                require(normalizedTargetInstallationId == normalizedLegacyInstallationId) {
                    "N-1 must target only its HMAC-derived legacy installation"
                }
                require(normalizedTargetRegistrationId == null) {
                    "N-1 must not carry a v2 registration target"
                }
            } else {
                require(normalizedLegacyPrimaryKeyFingerprint == null) {
                    "Generation N must not carry a legacy primary-key fingerprint"
                }
                require(normalizedLegacyInstallationId == null && normalizedLegacyRegistrationId == null) {
                    "Generation N must not carry legacy identities"
                }
                if (operation == LegacyCompatibilityOperation.REGISTER) {
                    require(normalizedTargetRegistrationId == null) {
                        "Generation N registration must not carry an existing registration target"
                    }
                }
            }

            val stableTargetIdentity = when (clientGeneration) {
                LegacyCompatibilityClientGeneration.N_MINUS_1 ->
                    checkNotNull(normalizedLegacyRegistrationId)
                LegacyCompatibilityClientGeneration.N ->
                    normalizedTargetRegistrationId ?: normalizedTargetInstallationId
            }
            val requestKey = legacyCompatibilityRequestKey(
                operation = operation,
                authenticatedUserId = normalizedUserId,
                compatibilityGeneration = compatibilityGeneration,
                stableTargetIdentity = stableTargetIdentity,
                tokenFingerprint = normalizedTokenFingerprint
            )

            LegacyCompatibilityCommand(
                sagaId = normalizedSagaId,
                operation = operation,
                clientGeneration = clientGeneration,
                authenticatedUserId = normalizedUserId,
                platform = platform,
                legacyPrimaryKeyFingerprint = normalizedLegacyPrimaryKeyFingerprint,
                legacyInstallationId = normalizedLegacyInstallationId,
                legacyRegistrationId = normalizedLegacyRegistrationId,
                targetInstallationId = normalizedTargetInstallationId,
                targetRegistrationId = normalizedTargetRegistrationId,
                tokenFingerprint = normalizedTokenFingerprint,
                compatibilityGeneration = compatibilityGeneration,
                maxAttemptsPerStore = maxAttemptsPerStore,
                initialNowEpochSeconds = initialNowEpochSeconds,
                scope = scope,
                requestKey = requestKey
            )
        }
    }
}

data class LegacyCompatibilitySnapshot(
    val sagaId: String,
    val requestKey: String,
    val operation: LegacyCompatibilityOperation,
    val clientGeneration: LegacyCompatibilityClientGeneration,
    val authenticatedUserId: String,
    val platform: Platform,
    val legacyPrimaryKeyFingerprint: String?,
    val legacyInstallationId: String?,
    val legacyRegistrationId: String?,
    val targetInstallationId: String,
    val targetRegistrationId: String?,
    val tokenFingerprint: String?,
    val compatibilityGeneration: Long,
    val maxAttemptsPerStore: Int,
    val scope: DeviceRegistrationScope,
    val state: LegacyCompatibilitySagaState,
    val reconciliationStatus: LegacyCompatibilityReconciliationStatus,
    val responseDisposition: LegacyCompatibilityResponseDisposition,
    val legacyWriteStatus: LegacyCompatibilityWriteStatus,
    val v2WriteStatus: LegacyCompatibilityWriteStatus,
    val v2TargetKind: LegacyCompatibilityV2TargetKind,
    val legacyAttempt: Int,
    val v2Attempt: Int,
    val nextRetryAtEpochSeconds: Long?,
    val checkpointRevision: Long,
    val logicalNowEpochSeconds: Long,
    val clockRevision: Long,
    val requiredEffect: LegacyCompatibilityEffectReference?,
    val recoveryLease: LegacyCompatibilityRecoveryLease?,
    val lastFailure: LegacyCompatibilityFailure?
)

interface LegacyNotificationCompatibilitySagaStore : AutoCloseable {
    /** Persists the intent and optional encrypted effect payload before any external write. */
    suspend fun persistIntent(
        command: LegacyCompatibilityCommand,
        rawToken: String? = null
    ): LegacyCompatibilitySnapshot

    suspend fun findBySagaId(sagaId: String): LegacyCompatibilitySnapshot?

    suspend fun acknowledgeWriteSucceeded(
        sagaId: String,
        reference: LegacyCompatibilityEffectReference,
        outcome: LegacyCompatibilityWriteOutcome
    ): LegacyCompatibilitySnapshot

    suspend fun acknowledgeWriteFailed(
        sagaId: String,
        reference: LegacyCompatibilityEffectReference,
        failure: LegacyCompatibilityFailure
    ): LegacyCompatibilitySnapshot

    suspend fun recordRetry(
        sagaId: String,
        reference: LegacyCompatibilityEffectReference,
        nextRetryAtEpochSeconds: Long
    ): LegacyCompatibilitySnapshot

    suspend fun retryDue(sagaId: String, checkpointRevision: Long): LegacyCompatibilitySnapshot

    suspend fun recordConvergence(
        sagaId: String,
        reference: LegacyCompatibilityEffectReference
    ): LegacyCompatibilitySnapshot

    suspend fun recordBlocked(
        sagaId: String,
        reference: LegacyCompatibilityEffectReference
    ): LegacyCompatibilitySnapshot

    suspend fun acquireRecoveryLease(
        request: LegacyCompatibilityRecoveryLeaseRequest
    ): LegacyCompatibilityRecoveryLease?

    suspend fun requestRecovery(
        request: LegacyCompatibilityRecoveryRequest
    ): LegacyCompatibilityEffectReference?

    suspend fun advanceLogicalClock(
        sagaId: String,
        clockRevision: Long,
        nowEpochSeconds: Long
    ): LegacyCompatibilitySnapshot

    suspend fun effectHistory(sagaId: String): List<LegacyCompatibilityEffectReference>

    /** Returns non-terminal sagas so a scheduler can resume them without a new HTTP request. */
    suspend fun recoveryCandidateSagaIds(): List<String>

    /**
     * Returns one durable lifecycle generation for a desired compatibility state. Repeating the
     * same desired state reuses the generation; changing it advances monotonically.
     */
    suspend fun allocateCompatibilityGeneration(
        authenticatedUserId: String,
        stableTargetIdentity: String,
        operation: LegacyCompatibilityOperation,
        tokenFingerprint: String?
    ): Long
}

/** A token capability that is usable only while its custodian-owned callback is active. */
interface LegacyCompatibilityScopedToken {
    suspend fun consumeWith(sink: suspend (CharArray) -> Unit)
}

interface LegacyCompatibilityTokenCustodian {
    suspend fun withRegistrationToken(
        sagaId: String,
        block: suspend (LegacyCompatibilityScopedToken) -> Unit
    ): Boolean
}

fun interface LegacyCompatibilityStoreSink {
    suspend fun write(
        snapshot: LegacyCompatibilitySnapshot,
        tokenScope: LegacyCompatibilityScopedToken?
    ): LegacyCompatibilityWriteOutcome
}

internal fun legacyCompatibilityRequestKey(
    operation: LegacyCompatibilityOperation,
    authenticatedUserId: String,
    compatibilityGeneration: Long,
    stableTargetIdentity: String,
    tokenFingerprint: String?
): String = opaqueCompatibilityDigest(
    listOf(
        "legacy-notification-registration-compatibility",
        "v2",
        operation.name,
        authenticatedUserId,
        compatibilityGeneration.toString(),
        stableTargetIdentity,
        tokenFingerprint
    )
)

internal fun opaqueCompatibilityDigest(fields: List<String?>): String {
    val digest = MessageDigest.getInstance("SHA-256")
    fields.forEach { field ->
        if (field == null) {
            digest.update(byteArrayOf(0))
        } else {
            val bytes = field.toByteArray(StandardCharsets.UTF_8)
            digest.update(byteArrayOf(1))
            digest.update(ByteBuffer.allocate(Int.SIZE_BYTES).putInt(bytes.size).array())
            digest.update(bytes)
            bytes.fill(0)
        }
    }
    return "compat-${Base64.getUrlEncoder().withoutPadding().encodeToString(digest.digest())}"
}

private fun String?.requiredCompatibilityField(label: String): String =
    this?.trim()?.takeIf(String::isNotEmpty)
        ?: throw IllegalArgumentException("$label is required")

private fun String?.optionalCompatibilityField(): String? =
    this?.trim()?.takeIf(String::isNotEmpty)

internal const val LEGACY_COMPATIBILITY_IOS_TOPIC = "com.guyghost.wakeve"
