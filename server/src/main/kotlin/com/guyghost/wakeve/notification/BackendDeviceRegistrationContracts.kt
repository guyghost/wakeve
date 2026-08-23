package com.guyghost.wakeve.notification

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path

enum class DeviceRegistrationStatus {
    ACTIVE,
    INVALID,
    UNREGISTERED
}

enum class DeviceRegistrationInvalidationReason {
    BAD_DEVICE_TOKEN,
    DEVICE_TOKEN_NOT_FOR_TOPIC,
    EXPIRED_TOKEN,
    UNREGISTERED
}

enum class DeviceRegistrationUnregisteredReason {
    ACCOUNT_CHANGED,
    SCOPE_CHANGED,
    LOGOUT,
    USER_REQUESTED,
    ADMIN_REVOKED
}

@ConsistentCopyVisibility
data class DeviceRegistrationScope private constructor(
    val environment: APNsEnvironment,
    val topic: String
) {
    companion object {
        fun create(environment: APNsEnvironment, topic: String?): Result<DeviceRegistrationScope> = runCatching {
            DeviceRegistrationScope(
                environment = environment,
                topic = topic?.trim()?.takeIf { it.isNotEmpty() }
                    ?: throw IllegalArgumentException("APNs topic is required")
            )
        }
    }
}

data class BackendDeviceInstallation(
    val installationId: String,
    val platform: Platform,
    val createdAtEpochSeconds: Long,
    val updatedAtEpochSeconds: Long
)

data class BackendDeviceRegistration(
    val registrationId: String,
    val installationId: String,
    val userId: String,
    val scope: DeviceRegistrationScope,
    val tokenHash: String,
    val status: DeviceRegistrationStatus,
    val createdAtEpochSeconds: Long,
    val updatedAtEpochSeconds: Long,
    val lastRegisteredAtEpochSeconds: Long,
    val invalidatedAtEpochSeconds: Long?,
    val invalidationReason: DeviceRegistrationInvalidationReason?,
    val unregisteredAtEpochSeconds: Long?,
    val unregisteredReason: DeviceRegistrationUnregisteredReason?
)

class BackendDeviceRegistrationRequest private constructor(
    val installationId: String,
    val authenticatedUserId: String,
    val platform: Platform,
    val scope: DeviceRegistrationScope,
    internal val rawToken: String,
    val registeredAtEpochSeconds: Long
) {
    override fun toString(): String =
        "BackendDeviceRegistrationRequest(installationId=$installationId, authenticatedUserId=$authenticatedUserId, " +
            "platform=$platform, scope=$scope, rawToken=[redacted], registeredAtEpochSeconds=$registeredAtEpochSeconds)"

    companion object {
        fun create(
            installationId: String?,
            authenticatedUserId: String?,
            platform: Platform,
            scope: DeviceRegistrationScope,
            rawToken: String?,
            registeredAtEpochSeconds: Long
        ): Result<BackendDeviceRegistrationRequest> = runCatching {
            require(registeredAtEpochSeconds >= 0) { "registeredAtEpochSeconds must be non-negative" }
            BackendDeviceRegistrationRequest(
                installationId = installationId.required("installationId"),
                authenticatedUserId = authenticatedUserId.required("authenticatedUserId"),
                platform = platform,
                scope = scope,
                rawToken = rawToken.required("APNs token"),
                registeredAtEpochSeconds = registeredAtEpochSeconds
            )
        }
    }
}

class LegacyNotificationTokenBackfill private constructor(
    internal val legacyRowKey: String,
    val userId: String,
    val platform: Platform,
    internal val rawToken: String,
    val scope: DeviceRegistrationScope,
    val updatedAtEpochSeconds: Long
) {
    override fun toString(): String =
        "LegacyNotificationTokenBackfill(legacyRowKey=[redacted], userId=$userId, platform=$platform, " +
            "rawToken=[redacted], scope=$scope, updatedAtEpochSeconds=$updatedAtEpochSeconds)"

    companion object {
        fun create(
            legacyRowKey: String?,
            userId: String?,
            platform: Platform,
            rawToken: String?,
            scope: DeviceRegistrationScope,
            updatedAtEpochSeconds: Long
        ): Result<LegacyNotificationTokenBackfill> = runCatching {
            require(updatedAtEpochSeconds >= 0) { "updatedAtEpochSeconds must be non-negative" }
            LegacyNotificationTokenBackfill(
                legacyRowKey = legacyRowKey.required("legacyRowKey"),
                userId = userId.required("userId"),
                platform = platform,
                rawToken = rawToken.required("APNs token"),
                scope = scope,
                updatedAtEpochSeconds = updatedAtEpochSeconds
            )
        }
    }
}

data class LegacyNotificationTokenBackfillResult(
    val installation: BackendDeviceInstallation,
    val registration: BackendDeviceRegistration,
    val created: Boolean
)

data class LegacyNotificationTokenRead(
    val selectedRegistrationId: String,
    val tokenHash: String,
    val platform: Platform,
    val scope: DeviceRegistrationScope,
    val status: DeviceRegistrationStatus,
    val createdAtEpochSeconds: Long,
    val lastRegisteredAtEpochSeconds: Long
)

enum class BackendDeviceUnregistrationOutcome {
    UNREGISTERED,
    ALREADY_ABSENT,
    NOT_OWNED
}

data class BackendDeviceUnregistrationResult(
    val outcome: BackendDeviceUnregistrationOutcome,
    val registrationId: String?
)

class DeviceRegistrationStoreConfiguration private constructor(
    val databasePath: Path,
    private val legacyIdentityHmacKey: ByteArray,
    private val tokenEncryptionKey: ByteArray
) {
    override fun toString(): String =
        "DeviceRegistrationStoreConfiguration(databasePath=$databasePath, " +
            "legacyIdentityHmacKey=[redacted], tokenEncryptionKey=[redacted])"

    internal fun legacyIdentityHmacKeyCopy(): ByteArray = legacyIdentityHmacKey.copyOf()

    internal fun tokenEncryptionKeyCopy(): ByteArray = tokenEncryptionKey.copyOf()

    companion object {
        private const val DATABASE_PATH_PROPERTY = "wakeve.notification.device-registration.db.path"
        private const val LEGACY_HMAC_PROPERTY =
            "wakeve.notification.device-registration.legacy-identity-hmac-key"
        private const val TOKEN_ENCRYPTION_PROPERTY =
            "wakeve.notification.device-registration.token-encryption-key"
        private const val DATABASE_PATH_ENV = "WAKEVE_NOTIFICATION_DEVICE_REGISTRATION_DB_PATH"
        private const val LEGACY_HMAC_ENV = "WAKEVE_NOTIFICATION_LEGACY_IDENTITY_HMAC_KEY"
        private const val TOKEN_ENCRYPTION_ENV = "WAKEVE_NOTIFICATION_TOKEN_ENCRYPTION_KEY"
        private const val MINIMUM_SECRET_BYTES = 32

        fun resolve(
            environment: Map<String, String> = System.getenv(),
            systemProperties: Map<String, String> = System.getProperties()
                .entries
                .associate { (key, value) -> key.toString() to value.toString() }
        ): Result<DeviceRegistrationStoreConfiguration> = runCatching {
            val configuredPath = configuredValue(
                systemProperties,
                environment,
                DATABASE_PATH_PROPERTY,
                DATABASE_PATH_ENV
            ) ?: error("A durable database path is required for device registration storage")
            val path = validatedDurablePath(configuredPath)
            val hmacKey = configuredSecret(
                systemProperties,
                environment,
                LEGACY_HMAC_PROPERTY,
                LEGACY_HMAC_ENV,
                "legacy identity HMAC key"
            )
            val encryptionKey = configuredSecret(
                systemProperties,
                environment,
                TOKEN_ENCRYPTION_PROPERTY,
                TOKEN_ENCRYPTION_ENV,
                "token encryption key"
            )
            DeviceRegistrationStoreConfiguration(path, hmacKey, encryptionKey)
        }

        private fun validatedDurablePath(configuredPath: String): Path {
            require(configuredPath != ":memory:") {
                "An in-memory database is not durable device registration storage"
            }
            require(!configuredPath.startsWith("file:", ignoreCase = true)) {
                "A database URI is not an allowed durable device registration path"
            }
            val path = try {
                Path.of(configuredPath).normalize()
            } catch (_: InvalidPathException) {
                error("The durable database path is invalid")
            }
            require(path.isAbsolute) { "The durable database path must be absolute" }
            require(path.fileName != null) { "The durable database path must identify a file" }
            requireNoUserControlledSymbolicLinks(path)
            return path
        }

        private fun requireNoUserControlledSymbolicLinks(path: Path) {
            var current = checkNotNull(path.root) { "The durable database path must have a root" }
            for (index in 0 until path.nameCount) {
                current = current.resolve(path.getName(index))
                if (!Files.isSymbolicLink(current)) continue
                if (index == 0) {
                    // macOS exposes trusted root aliases such as /var -> /private/var and
                    // /tmp -> /private/tmp. Canonicalize that privileged root component while
                    // rejecting every user-controlled symbolic-link component below it.
                    current = current.toRealPath()
                } else {
                    error("The durable database path must not contain symbolic links")
                }
            }
        }

        private fun configuredValue(
            systemProperties: Map<String, String>,
            environment: Map<String, String>,
            propertyName: String,
            environmentName: String
        ): String? = systemProperties[propertyName]?.trim()?.takeIf { it.isNotEmpty() }
            ?: environment[environmentName]?.trim()?.takeIf { it.isNotEmpty() }

        private fun configuredSecret(
            systemProperties: Map<String, String>,
            environment: Map<String, String>,
            propertyName: String,
            environmentName: String,
            label: String
        ): ByteArray {
            val value = configuredValue(systemProperties, environment, propertyName, environmentName)
                ?: error("$label is required")
            val bytes = value.toByteArray(StandardCharsets.UTF_8)
            require(bytes.size >= MINIMUM_SECRET_BYTES) { "$label must contain at least 32 bytes" }
            return bytes
        }
    }
}

internal enum class DeviceRegistrationTransactionFaultPoint {
    AFTER_ACTIVE_REGISTRATION_CLOSED
}

internal fun interface DeviceRegistrationTransactionFaultInjector {
    fun inject(point: DeviceRegistrationTransactionFaultPoint)
}

interface BackendDeviceRegistrationStore : AutoCloseable {
    suspend fun register(request: BackendDeviceRegistrationRequest): BackendDeviceRegistration
    suspend fun installation(installationId: String): BackendDeviceInstallation?
    suspend fun registration(registrationId: String): BackendDeviceRegistration?
    suspend fun activeRegistration(installationId: String): BackendDeviceRegistration?
    suspend fun activeRegistrations(
        userId: String,
        scope: DeviceRegistrationScope
    ): List<BackendDeviceRegistration>

    suspend fun registrationHistory(installationId: String): List<BackendDeviceRegistration>
    suspend fun invalidate(
        registrationId: String,
        authenticatedUserId: String,
        reason: DeviceRegistrationInvalidationReason,
        atEpochSeconds: Long
    ): BackendDeviceRegistration

    /**
     * Terminates exactly one registration association owned by [authenticatedUserId].
     * Missing and already-terminal owned associations are idempotent successes; a registration
     * owned by another account is reported without mutating it.
     */
    suspend fun unregisterRegistration(
        registrationId: String,
        authenticatedUserId: String,
        reason: DeviceRegistrationUnregisteredReason,
        atEpochSeconds: Long
    ): BackendDeviceUnregistrationResult

    /** Terminates only the active association for one stable installation. */
    suspend fun unregisterInstallation(
        installationId: String,
        authenticatedUserId: String,
        reason: DeviceRegistrationUnregisteredReason,
        atEpochSeconds: Long
    ): BackendDeviceUnregistrationResult

    /**
     * Compatibility-only unregistration for the opaque association derived from a legacy row.
     * This must never fall back to another active registration for the same user/platform.
     */
    suspend fun unregisterLegacy(
        legacyRowKey: String,
        authenticatedUserId: String,
        platform: Platform,
        scope: DeviceRegistrationScope,
        reason: DeviceRegistrationUnregisteredReason,
        atEpochSeconds: Long
    ): BackendDeviceUnregistrationResult

    suspend fun backfillLegacy(
        request: LegacyNotificationTokenBackfill
    ): LegacyNotificationTokenBackfillResult

    suspend fun legacyReadCompatibility(
        userId: String,
        platform: Platform,
        scope: DeviceRegistrationScope
    ): LegacyNotificationTokenRead?
}

interface BackendDeviceRegistrationProviderTokenPort : AutoCloseable {
    /**
     * Runs [block] while the selected token is decrypted, then returns only a fixed tokenless
     * completion signal. Callback expressions are coerced to [Unit] and can never become the
     * returned value; callback failures are propagated as exceptions.
     */
    suspend fun withDecryptedToken(registrationId: String, block: (String) -> Unit): Boolean
}

interface BackendDeviceRegistrationStoreFactory {
    fun open(): BackendDeviceRegistrationStore
    fun openProviderTokenPort(): BackendDeviceRegistrationProviderTokenPort
    fun openCompatibilitySagaStore(): LegacyNotificationCompatibilitySagaStore

    /**
     * Derives the compatibility-only identifiers from the immutable legacy row identity.
     * The raw row identity and the configured HMAC key never become part of the result.
     */
    fun deriveLegacyCompatibilityIdentity(legacyRowKey: String): LegacyCompatibilityIdentity
}

/** Migration control is intentionally unavailable to routes and canonical registration ports. */
internal fun BackendDeviceRegistrationStoreFactory.openCompatibilityUniqueMigration(
    migrationId: String,
    schemaVersion: Int,
    initialLogicalNowEpochSeconds: Long,
    faultInjector: LegacyCompatibilityUniqueMigrationFaultInjector,
    preflightProbe: LegacyCompatibilityUniqueMigrationReadOnlyPreflightProbe =
        NoOpLegacyCompatibilityUniqueMigrationReadOnlyPreflightProbe
): LegacyCompatibilityUniqueMigrationRuntime {
    val sqliteFactory = this as? SqliteBackendDeviceRegistrationStoreFactory
        ?: throw IllegalStateException("The compatibility migration control port is unavailable")
    return sqliteFactory.openCompatibilityUniqueMigration(
        migrationId = migrationId,
        schemaVersion = schemaVersion,
        initialLogicalNowEpochSeconds = initialLogicalNowEpochSeconds,
        faultInjector = faultInjector,
        preflightProbe = preflightProbe
    )
}

private fun String?.required(label: String): String =
    this?.trim()?.takeIf { it.isNotEmpty() } ?: throw IllegalArgumentException("$label is required")
