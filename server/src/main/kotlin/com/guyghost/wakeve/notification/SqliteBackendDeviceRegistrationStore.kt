package com.guyghost.wakeve.notification

import java.nio.charset.StandardCharsets
import java.nio.file.FileAlreadyExistsException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.PosixFilePermissions
import java.security.MessageDigest
import java.security.SecureRandom
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.util.Base64
import java.util.Properties
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class SqliteBackendDeviceRegistrationStoreFactory private constructor(
    private val configuration: DeviceRegistrationStoreConfiguration?,
    private val transactionFaultInjector: DeviceRegistrationTransactionFaultInjector,
    private val jdbcOpenTestHook: DeviceRegistrationJdbcOpenTestHook,
    @Suppress("UNUSED_PARAMETER") constructorMarker: Unit
) : BackendDeviceRegistrationStoreFactory {
    constructor() : this(
        null,
        DeviceRegistrationTransactionFaultInjector { },
        DeviceRegistrationJdbcOpenTestHook { },
        Unit
    )

    constructor(configuration: DeviceRegistrationStoreConfiguration) : this(
        configuration,
        DeviceRegistrationTransactionFaultInjector { },
        DeviceRegistrationJdbcOpenTestHook { },
        Unit
    )

    internal constructor(
        configuration: DeviceRegistrationStoreConfiguration,
        transactionFaultInjector: DeviceRegistrationTransactionFaultInjector
    ) : this(
        configuration,
        transactionFaultInjector,
        DeviceRegistrationJdbcOpenTestHook { },
        Unit
    )

    internal constructor(
        configuration: DeviceRegistrationStoreConfiguration,
        transactionFaultInjector: DeviceRegistrationTransactionFaultInjector,
        jdbcOpenTestHook: DeviceRegistrationJdbcOpenTestHook
    ) : this(configuration, transactionFaultInjector, jdbcOpenTestHook, Unit)

    override fun open(): BackendDeviceRegistrationStore {
        val configuration = resolvedConfiguration()
        requireCompatibilityUniqueMigrationReady(configuration)
        return SqliteBackendDeviceRegistrationStore(
            databasePath = databasePath(configuration),
            tokenCipher = tokenCipher(configuration),
            legacyHmacKey = configuration.legacyIdentityHmacKeyCopy(),
            transactionFaultInjector = transactionFaultInjector,
            jdbcOpenTestHook = jdbcOpenTestHook
        )
    }

    override fun openProviderTokenPort(): BackendDeviceRegistrationProviderTokenPort {
        val configuration = resolvedConfiguration()
        requireCompatibilityUniqueMigrationReady(configuration)
        return SqliteBackendDeviceRegistrationProviderTokenPort(
            databasePath = databasePath(configuration),
            tokenCipher = tokenCipher(configuration),
            jdbcOpenTestHook = jdbcOpenTestHook
        )
    }

    internal fun openCompatibilityUniqueMigration(
        migrationId: String,
        schemaVersion: Int,
        initialLogicalNowEpochSeconds: Long,
        faultInjector: LegacyCompatibilityUniqueMigrationFaultInjector,
        preflightProbe: LegacyCompatibilityUniqueMigrationReadOnlyPreflightProbe =
            NoOpLegacyCompatibilityUniqueMigrationReadOnlyPreflightProbe
    ): LegacyCompatibilityUniqueMigrationRuntime {
        val configuration = resolvedConfiguration()
        return SqliteLegacyCompatibilityUniqueMigrationRuntime(
            databasePath = databasePath(configuration),
            migrationId = migrationId,
            schemaVersion = schemaVersion,
            initialLogicalNowEpochSeconds = initialLogicalNowEpochSeconds,
            faultInjector = faultInjector,
            preflightProbe = preflightProbe,
            jdbcOpenTestHook = jdbcOpenTestHook
        )
    }

    override fun openCompatibilitySagaStore(): LegacyNotificationCompatibilitySagaStore {
        return openCompatibilitySagaStore(NoOpLegacyCompatibilityLeaseClaimProbe)
    }

    internal fun openCompatibilitySagaStore(
        leaseClaimProbe: LegacyCompatibilityLeaseClaimProbe
    ): LegacyNotificationCompatibilitySagaStore {
        val configuration = resolvedConfiguration()
        requireCompatibilityUniqueMigrationReady(configuration)
        val encryptionKey = configuration.tokenEncryptionKeyCopy()
        return try {
            SqliteLegacyNotificationCompatibilitySagaStore(
                databasePath = databasePath(configuration),
                tokenEncryptionKey = encryptionKey,
                jdbcOpenTestHook = jdbcOpenTestHook,
                leaseClaimProbe = leaseClaimProbe
            )
        } finally {
            encryptionKey.fill(0)
        }
    }

    private fun requireCompatibilityUniqueMigrationReady(
        configuration: DeviceRegistrationStoreConfiguration
    ) {
        val preparedDatabasePath = databasePath(configuration)
        SqliteLegacyCompatibilityUniqueMigrationRuntime(
            databasePath = preparedDatabasePath,
            migrationId = DEFAULT_COMPATIBILITY_UNIQUE_MIGRATION_ID,
            schemaVersion = DEFAULT_COMPATIBILITY_UNIQUE_MIGRATION_SCHEMA_VERSION,
            initialLogicalNowEpochSeconds = 0L,
            faultInjector = NoOpLegacyCompatibilityUniqueMigrationFaultInjector,
            preflightProbe = NoOpLegacyCompatibilityUniqueMigrationReadOnlyPreflightProbe,
            jdbcOpenTestHook = jdbcOpenTestHook
        ).use { migration ->
            val observed = migration.currentSnapshot()
            val snapshot = when {
                observed.state != LegacyCompatibilityUniqueMigrationState.STARTUP_PREFLIGHT ->
                    observed
                databaseHasNoCompatibilitySchema(preparedDatabasePath) ->
                    // A brand-new datastore has no legacy rows to reconcile. Provisioning it
                    // through the migration control port preserves the clean-install flow.
                    migration.startOrResume()
                else ->
                    // Another process may have completed pristine provisioning between the
                    // read-only observation and the schema-presence check. Re-read only; never
                    // advance an existing uninitialized datastore from this canonical port.
                    migration.currentSnapshot()
            }
            if (!snapshot.runtimeReady) {
                val diagnostic = migration.diagnostic()
                throw LegacyCompatibilityUniqueMigrationNotReadyException(
                    migrationState = snapshot.state,
                    duplicateGroupCount = diagnostic.duplicateGroupCount,
                    duplicateRowCount = diagnostic.duplicateRowCount,
                    failure = diagnostic.failure
                )
            }
        }
    }

    private fun databaseHasNoCompatibilitySchema(databasePath: Path): Boolean =
        openDeviceRegistrationJdbcConnection(databasePath, jdbcOpenTestHook).use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("PRAGMA query_only = ON")
                statement.executeQuery(
                    "SELECT 1 FROM sqlite_schema " +
                        "WHERE type = 'table' AND name IN (" +
                        "'legacy_notification_compatibility_saga', " +
                        "'legacy_compatibility_unique_migration_state') LIMIT 1"
                ).use { rows -> !rows.next() }
            }
        }

    override fun deriveLegacyCompatibilityIdentity(
        legacyRowKey: String
    ): LegacyCompatibilityIdentity {
        val configuration = resolvedConfiguration()
        val hmacKey = configuration.legacyIdentityHmacKeyCopy()
        return try {
            deriveLegacyCompatibilityIdentity(hmacKey, legacyRowKey)
        } finally {
            hmacKey.fill(0)
        }
    }

    internal fun prepareDatabasePathForSharedBackendStore(): Path =
        databasePath(resolvedConfiguration())

    /**
     * Lets the application keep unrelated notification endpoints available in installations
     * where compatibility storage was deliberately not configured. The compatibility routes and
     * scheduler remain fail-closed because every canonical store open still requires a READY
     * migration.
     */
    internal fun hasCompatibilityRuntimeConfiguration(): Boolean =
        configuration != null || DeviceRegistrationStoreConfiguration.resolve().isSuccess

    private fun resolvedConfiguration(): DeviceRegistrationStoreConfiguration =
        configuration ?: DeviceRegistrationStoreConfiguration.resolve().getOrElse { failure ->
            throw IllegalStateException(
                failure.message ?: "Device registration store configuration is invalid"
            )
        }

    private fun databasePath(configuration: DeviceRegistrationStoreConfiguration): Path =
        prepareDurableDatabaseFile(configuration.databasePath)

    private fun prepareDurableDatabaseFile(configuredDatabasePath: Path): Path {
        val databasePath = pathWithoutUserControlledSymbolicLinks(configuredDatabasePath)
        val parent = requireNotNull(databasePath.parent) {
            "A durable database parent directory is required"
        }
        val posix = FileSystems.getDefault().supportedFileAttributeViews().contains("posix")
        prepareOwnerOnlyParent(parent, posix)
        requireSafeExistingPath(parent, expectDirectory = true)
        requireOwnerOnlyPermissions(parent, OWNER_DIRECTORY_PERMISSIONS, posix)

        if (Files.exists(databasePath, LinkOption.NOFOLLOW_LINKS)) {
            requireSafeExistingPath(databasePath, expectDirectory = false)
        } else {
            try {
                if (posix) {
                    Files.createFile(
                        databasePath,
                        PosixFilePermissions.asFileAttribute(OWNER_FILE_PERMISSIONS)
                    )
                } else {
                    Files.createFile(databasePath)
                }
            } catch (_: FileAlreadyExistsException) {
                // Another actor populated the final component after validation. Treat it as
                // untrusted and validate without following it before any permission change.
            }
            requireSafeExistingPath(databasePath, expectDirectory = false)
        }
        if (posix && Files.getPosixFilePermissions(
                databasePath,
                LinkOption.NOFOLLOW_LINKS
            ) != OWNER_FILE_PERMISSIONS
        ) {
            Files.setAttribute(
                databasePath,
                "posix:permissions",
                OWNER_FILE_PERMISSIONS,
                LinkOption.NOFOLLOW_LINKS
            )
        }
        requireSafeExistingPath(databasePath, expectDirectory = false)
        requireOwnerOnlyPermissions(databasePath, OWNER_FILE_PERMISSIONS, posix)
        require(
            pathWithoutUserControlledSymbolicLinks(databasePath) == databasePath
        ) { "The durable database path changed during secure preparation" }
        return databasePath
    }

    private fun prepareOwnerOnlyParent(parent: Path, posix: Boolean) {
        if (Files.exists(parent, LinkOption.NOFOLLOW_LINKS)) {
            requireSafeExistingPath(parent, expectDirectory = true)
            requireOwnerOnlyPermissions(parent, OWNER_DIRECTORY_PERMISSIONS, posix)
            return
        }

        val missingDirectories = mutableListOf<Path>()
        var existingAncestor: Path? = parent
        while (existingAncestor != null && !Files.exists(
                existingAncestor,
                LinkOption.NOFOLLOW_LINKS
            )
        ) {
            val missingDirectory = existingAncestor
            missingDirectories.add(missingDirectory)
            existingAncestor = missingDirectory.parent
        }
        requireNotNull(existingAncestor) { "A durable database parent root is required" }
        requireSafeExistingPath(existingAncestor, expectDirectory = true)

        for (directory in missingDirectories.asReversed()) {
            pathWithoutUserControlledSymbolicLinks(
                requireNotNull(directory.parent) { "A durable database parent root is required" }
            )
            try {
                if (posix) {
                    Files.createDirectory(
                        directory,
                        PosixFilePermissions.asFileAttribute(OWNER_DIRECTORY_PERMISSIONS)
                    )
                } else {
                    Files.createDirectory(directory)
                }
            } catch (_: FileAlreadyExistsException) {
                // A concurrently created component is never trusted without the same no-follow
                // validation and owner-only permission checks as a pre-existing parent.
            }
            requireSafeExistingPath(directory, expectDirectory = true)
            requireOwnerOnlyPermissions(directory, OWNER_DIRECTORY_PERMISSIONS, posix)
        }
    }

    private fun pathWithoutUserControlledSymbolicLinks(path: Path): Path {
        var current = requireNotNull(path.root) { "The durable database path must have a root" }
        for (index in 0 until path.nameCount) {
            val candidate = current.resolve(path.getName(index))
            if (Files.isSymbolicLink(candidate)) {
                require(index == 0) {
                    "The durable database path must not contain symbolic links"
                }
                // Root-owned aliases such as macOS /var -> /private/var are the only links
                // accepted by configuration. Resolve that privileged component once so every
                // later validation and the JDBC URL use the same link-free path.
                current = candidate.toRealPath()
            } else {
                current = candidate
            }
        }
        return current.normalize()
    }

    private fun requireSafeExistingPath(path: Path, expectDirectory: Boolean) {
        require(!Files.isSymbolicLink(path)) {
            "The durable database path must not contain symbolic links"
        }
        val validType = if (expectDirectory) {
            Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)
        } else {
            Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
        }
        require(validType) {
            if (expectDirectory) {
                "The durable database parent must be a directory"
            } else {
                "The durable database path must identify a regular file"
            }
        }
    }

    private fun requireOwnerOnlyPermissions(
        path: Path,
        expected: Set<PosixFilePermission>,
        posix: Boolean
    ) {
        if (!posix) return
        require(Files.getPosixFilePermissions(path, LinkOption.NOFOLLOW_LINKS) == expected) {
            "Durable device registration storage must use owner-only permissions"
        }
    }

    private fun tokenCipher(configuration: DeviceRegistrationStoreConfiguration): DeviceRegistrationTokenCipher {
        val keyMaterial = configuration.tokenEncryptionKeyCopy()
        return try {
            DeviceRegistrationTokenCipher(keyMaterial)
        } finally {
            keyMaterial.fill(0)
        }
    }

}

private class SqliteBackendDeviceRegistrationStore(
    private val databasePath: Path,
    private val tokenCipher: DeviceRegistrationTokenCipher,
    private val legacyHmacKey: ByteArray,
    private val transactionFaultInjector: DeviceRegistrationTransactionFaultInjector,
    private val jdbcOpenTestHook: DeviceRegistrationJdbcOpenTestHook
) : BackendDeviceRegistrationStore {
    private val closed = AtomicBoolean(false)

    init {
        synchronized(REGISTRATION_STORE_LOCK) {
            connection().use(::createSchema)
        }
    }

    override suspend fun register(
        request: BackendDeviceRegistrationRequest
    ): BackendDeviceRegistration = synchronized(REGISTRATION_STORE_LOCK) {
        transaction { connection ->
            ensureInstallation(
                connection = connection,
                installationId = request.installationId,
                platform = request.platform,
                atEpochSeconds = request.registeredAtEpochSeconds
            )
            val active = activeRegistration(connection, request.installationId)
            when {
                active == null -> insertRegistration(
                    connection = connection,
                    registrationId = UUID.randomUUID().toString(),
                    installationId = request.installationId,
                    userId = request.authenticatedUserId,
                    scope = request.scope,
                    rawToken = request.rawToken,
                    atEpochSeconds = request.registeredAtEpochSeconds
                )

                active.userId == request.authenticatedUserId && active.scope == request.scope ->
                    rotateRegistration(
                        connection = connection,
                        registrationId = active.registrationId,
                        installationId = active.installationId,
                        rawToken = request.rawToken,
                        atEpochSeconds = request.registeredAtEpochSeconds
                    )

                else -> replaceActiveRegistration(
                    connection = connection,
                    active = active,
                    request = request
                )
            }
        }
    }

    override suspend fun installation(
        installationId: String
    ): BackendDeviceInstallation? = synchronized(REGISTRATION_STORE_LOCK) {
        read { connection -> installation(connection, installationId) }
    }

    override suspend fun registration(
        registrationId: String
    ): BackendDeviceRegistration? = synchronized(REGISTRATION_STORE_LOCK) {
        read { connection -> registration(connection, registrationId) }
    }

    override suspend fun activeRegistration(
        installationId: String
    ): BackendDeviceRegistration? = synchronized(REGISTRATION_STORE_LOCK) {
        read { connection -> activeRegistration(connection, installationId) }
    }

    override suspend fun activeRegistrations(
        userId: String,
        scope: DeviceRegistrationScope
    ): List<BackendDeviceRegistration> = synchronized(REGISTRATION_STORE_LOCK) {
        read { connection ->
            connection.prepareStatement(
                """
                SELECT *
                FROM device_registration
                WHERE user_id = ?
                  AND environment = ?
                  AND topic = ?
                  AND status = ?
                ORDER BY created_at_epoch_seconds ASC, registration_id ASC
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, userId)
                statement.setString(2, scope.environment.name)
                statement.setString(3, scope.topic)
                statement.setString(4, DeviceRegistrationStatus.ACTIVE.name)
                statement.executeQuery().use(::registrations)
            }
        }
    }

    override suspend fun registrationHistory(
        installationId: String
    ): List<BackendDeviceRegistration> = synchronized(REGISTRATION_STORE_LOCK) {
        read { connection ->
            connection.prepareStatement(
                """
                SELECT *
                FROM device_registration
                WHERE installation_id = ?
                ORDER BY created_at_epoch_seconds ASC, registration_id ASC
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, installationId)
                statement.executeQuery().use(::registrations)
            }
        }
    }

    override suspend fun invalidate(
        registrationId: String,
        authenticatedUserId: String,
        reason: DeviceRegistrationInvalidationReason,
        atEpochSeconds: Long
    ): BackendDeviceRegistration = synchronized(REGISTRATION_STORE_LOCK) {
        require(atEpochSeconds >= 0) { "atEpochSeconds must be non-negative" }
        transaction { connection ->
            connection.prepareStatement(
                """
                UPDATE device_registration
                SET status = ?,
                    updated_at_epoch_seconds = ?,
                    invalidated_at_epoch_seconds = ?,
                    invalidation_reason = ?,
                    unregistered_at_epoch_seconds = NULL,
                    unregistered_reason = NULL
                WHERE registration_id = ?
                  AND user_id = ?
                  AND status = ?
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, DeviceRegistrationStatus.INVALID.name)
                statement.setLong(2, atEpochSeconds)
                statement.setLong(3, atEpochSeconds)
                statement.setString(4, reason.name)
                statement.setString(5, registrationId)
                statement.setString(6, authenticatedUserId)
                statement.setString(7, DeviceRegistrationStatus.ACTIVE.name)
                require(statement.executeUpdate() == 1) {
                    "Active device registration is unavailable for invalidation"
                }
            }
            registration(connection, registrationId)
                ?: error("Invalidated device registration could not be reloaded")
        }
    }

    override suspend fun unregisterRegistration(
        registrationId: String,
        authenticatedUserId: String,
        reason: DeviceRegistrationUnregisteredReason,
        atEpochSeconds: Long
    ): BackendDeviceUnregistrationResult = synchronized(REGISTRATION_STORE_LOCK) {
        require(registrationId.isNotBlank()) { "registrationId is required" }
        require(authenticatedUserId.isNotBlank()) { "authenticatedUserId is required" }
        require(atEpochSeconds >= 0) { "atEpochSeconds must be non-negative" }
        transaction { connection ->
            unregisterResolvedRegistration(
                connection = connection,
                registration = registration(connection, registrationId),
                authenticatedUserId = authenticatedUserId,
                reason = reason,
                atEpochSeconds = atEpochSeconds
            )
        }
    }

    override suspend fun unregisterInstallation(
        installationId: String,
        authenticatedUserId: String,
        reason: DeviceRegistrationUnregisteredReason,
        atEpochSeconds: Long
    ): BackendDeviceUnregistrationResult = synchronized(REGISTRATION_STORE_LOCK) {
        require(installationId.isNotBlank()) { "installationId is required" }
        require(authenticatedUserId.isNotBlank()) { "authenticatedUserId is required" }
        require(atEpochSeconds >= 0) { "atEpochSeconds must be non-negative" }
        transaction { connection ->
            unregisterResolvedRegistration(
                connection = connection,
                registration = activeRegistration(connection, installationId),
                authenticatedUserId = authenticatedUserId,
                reason = reason,
                atEpochSeconds = atEpochSeconds
            )
        }
    }

    override suspend fun unregisterLegacy(
        legacyRowKey: String,
        authenticatedUserId: String,
        platform: Platform,
        scope: DeviceRegistrationScope,
        reason: DeviceRegistrationUnregisteredReason,
        atEpochSeconds: Long
    ): BackendDeviceUnregistrationResult = synchronized(REGISTRATION_STORE_LOCK) {
        require(legacyRowKey.isNotBlank()) { "legacyRowKey is required" }
        require(authenticatedUserId.isNotBlank()) { "authenticatedUserId is required" }
        require(atEpochSeconds >= 0) { "atEpochSeconds must be non-negative" }
        transaction { connection ->
            val installationId = legacyIdentity("wakeve/legacy-installation/v1", legacyRowKey)
            val registration = activeRegistration(connection, installationId)
            val matchesCompatibilityIdentity = registration == null || (
                registration.scope == scope &&
                    installation(connection, registration.installationId)?.platform == platform
                )
            if (!matchesCompatibilityIdentity) {
                BackendDeviceUnregistrationResult(
                    outcome = BackendDeviceUnregistrationOutcome.NOT_OWNED,
                    registrationId = null
                )
            } else {
                unregisterResolvedRegistration(
                    connection = connection,
                    registration = registration,
                    authenticatedUserId = authenticatedUserId,
                    reason = reason,
                    atEpochSeconds = atEpochSeconds
                )
            }
        }
    }

    override suspend fun backfillLegacy(
        request: LegacyNotificationTokenBackfill
    ): LegacyNotificationTokenBackfillResult = synchronized(REGISTRATION_STORE_LOCK) {
        transaction { connection ->
            val installationId = legacyIdentity("wakeve/legacy-installation/v1", request.legacyRowKey)
            val registrationId = legacyIdentity("wakeve/legacy-registration/v1", request.legacyRowKey)
            ensureInstallation(
                connection = connection,
                installationId = installationId,
                platform = request.platform,
                atEpochSeconds = request.updatedAtEpochSeconds
            )
            val existing = registration(connection, registrationId)
            val created = existing == null
            val persisted = when {
                existing == null -> {
                    check(activeRegistration(connection, installationId) == null) {
                        "Legacy installation already has a different active registration"
                    }
                    insertRegistration(
                        connection = connection,
                        registrationId = registrationId,
                        installationId = installationId,
                        userId = request.userId,
                        scope = request.scope,
                        rawToken = request.rawToken,
                        atEpochSeconds = request.updatedAtEpochSeconds
                    )
                }

                existing.installationId != installationId ||
                    existing.userId != request.userId ||
                    existing.scope != request.scope -> {
                    error("Legacy row identity conflicts with an existing registration")
                }

                existing.status == DeviceRegistrationStatus.ACTIVE -> rotateRegistration(
                    connection = connection,
                    registrationId = registrationId,
                    installationId = installationId,
                    rawToken = request.rawToken,
                    atEpochSeconds = request.updatedAtEpochSeconds
                )

                else -> {
                    val active = activeRegistration(connection, installationId)
                    when {
                        active == null -> insertRegistration(
                            connection = connection,
                            registrationId = UUID.randomUUID().toString(),
                            installationId = installationId,
                            userId = request.userId,
                            scope = request.scope,
                            rawToken = request.rawToken,
                            atEpochSeconds = request.updatedAtEpochSeconds
                        )

                        active.userId != request.userId || active.scope != request.scope ->
                            error("Legacy installation has a conflicting active registration")

                        else -> rotateRegistration(
                            connection = connection,
                            registrationId = active.registrationId,
                            installationId = installationId,
                            rawToken = request.rawToken,
                            atEpochSeconds = request.updatedAtEpochSeconds
                        )
                    }
                }
            }
            LegacyNotificationTokenBackfillResult(
                installation = installation(connection, installationId)
                    ?: error("Legacy installation could not be reloaded"),
                registration = persisted,
                created = created
            )
        }
    }

    override suspend fun legacyReadCompatibility(
        userId: String,
        platform: Platform,
        scope: DeviceRegistrationScope
    ): LegacyNotificationTokenRead? = synchronized(REGISTRATION_STORE_LOCK) {
        read { connection ->
            connection.prepareStatement(
                """
                SELECT r.registration_id,
                       r.token_hash,
                       i.platform,
                       r.environment,
                       r.topic,
                       r.status,
                       r.created_at_epoch_seconds,
                       r.last_registered_at_epoch_seconds
                FROM device_registration r
                JOIN device_installation i ON i.installation_id = r.installation_id
                WHERE r.user_id = ?
                  AND i.platform = ?
                  AND r.environment = ?
                  AND r.topic = ?
                  AND r.status = ?
                ORDER BY r.last_registered_at_epoch_seconds DESC,
                         r.created_at_epoch_seconds DESC,
                         r.registration_id ASC
                LIMIT 1
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, userId)
                statement.setString(2, platform.name)
                statement.setString(3, scope.environment.name)
                statement.setString(4, scope.topic)
                statement.setString(5, DeviceRegistrationStatus.ACTIVE.name)
                statement.executeQuery().use { rows ->
                    if (!rows.next()) return@read null
                    LegacyNotificationTokenRead(
                        selectedRegistrationId = rows.getString("registration_id"),
                        tokenHash = rows.getString("token_hash"),
                        platform = Platform.valueOf(rows.getString("platform")),
                        scope = DeviceRegistrationScope.create(
                            APNsEnvironment.valueOf(rows.getString("environment")),
                            rows.getString("topic")
                        ).getOrThrow(),
                        status = DeviceRegistrationStatus.valueOf(rows.getString("status")),
                        createdAtEpochSeconds = rows.getLong("created_at_epoch_seconds"),
                        lastRegisteredAtEpochSeconds = rows.getLong("last_registered_at_epoch_seconds")
                    )
                }
            }
        }
    }

    override fun close(): Unit = synchronized(REGISTRATION_STORE_LOCK) {
        if (closed.compareAndSet(false, true)) {
            legacyHmacKey.fill(0)
            tokenCipher.close()
        }
    }

    private fun replaceActiveRegistration(
        connection: Connection,
        active: BackendDeviceRegistration,
        request: BackendDeviceRegistrationRequest
    ): BackendDeviceRegistration {
        val reason = if (active.userId != request.authenticatedUserId) {
            DeviceRegistrationUnregisteredReason.ACCOUNT_CHANGED
        } else {
            DeviceRegistrationUnregisteredReason.SCOPE_CHANGED
        }
        connection.prepareStatement(
            """
            UPDATE device_registration
            SET status = ?,
                updated_at_epoch_seconds = ?,
                unregistered_at_epoch_seconds = ?,
                unregistered_reason = ?,
                invalidated_at_epoch_seconds = NULL,
                invalidation_reason = NULL
            WHERE registration_id = ? AND status = ?
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, DeviceRegistrationStatus.UNREGISTERED.name)
            statement.setLong(2, request.registeredAtEpochSeconds)
            statement.setLong(3, request.registeredAtEpochSeconds)
            statement.setString(4, reason.name)
            statement.setString(5, active.registrationId)
            statement.setString(6, DeviceRegistrationStatus.ACTIVE.name)
            check(statement.executeUpdate() == 1) { "Active registration changed during replacement" }
        }
        transactionFaultInjector.inject(
            DeviceRegistrationTransactionFaultPoint.AFTER_ACTIVE_REGISTRATION_CLOSED
        )
        return insertRegistration(
            connection = connection,
            registrationId = UUID.randomUUID().toString(),
            installationId = request.installationId,
            userId = request.authenticatedUserId,
            scope = request.scope,
            rawToken = request.rawToken,
            atEpochSeconds = request.registeredAtEpochSeconds
        )
    }

    private fun unregisterResolvedRegistration(
        connection: Connection,
        registration: BackendDeviceRegistration?,
        authenticatedUserId: String,
        reason: DeviceRegistrationUnregisteredReason,
        atEpochSeconds: Long
    ): BackendDeviceUnregistrationResult {
        if (registration == null) {
            return BackendDeviceUnregistrationResult(
                outcome = BackendDeviceUnregistrationOutcome.ALREADY_ABSENT,
                registrationId = null
            )
        }
        if (registration.userId != authenticatedUserId) {
            return BackendDeviceUnregistrationResult(
                outcome = BackendDeviceUnregistrationOutcome.NOT_OWNED,
                registrationId = null
            )
        }
        if (registration.status != DeviceRegistrationStatus.ACTIVE) {
            return BackendDeviceUnregistrationResult(
                outcome = BackendDeviceUnregistrationOutcome.ALREADY_ABSENT,
                registrationId = registration.registrationId
            )
        }

        connection.prepareStatement(
            """
            UPDATE device_registration
            SET status = ?,
                updated_at_epoch_seconds = ?,
                unregistered_at_epoch_seconds = ?,
                unregistered_reason = ?,
                invalidated_at_epoch_seconds = NULL,
                invalidation_reason = NULL
            WHERE registration_id = ?
              AND user_id = ?
              AND status = ?
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, DeviceRegistrationStatus.UNREGISTERED.name)
            statement.setLong(2, atEpochSeconds)
            statement.setLong(3, atEpochSeconds)
            statement.setString(4, reason.name)
            statement.setString(5, registration.registrationId)
            statement.setString(6, authenticatedUserId)
            statement.setString(7, DeviceRegistrationStatus.ACTIVE.name)
            check(statement.executeUpdate() == 1) {
                "Active registration changed during unregistration"
            }
        }
        updateInstallationTimestamp(connection, registration.installationId, atEpochSeconds)
        return BackendDeviceUnregistrationResult(
            outcome = BackendDeviceUnregistrationOutcome.UNREGISTERED,
            registrationId = registration.registrationId
        )
    }

    private fun ensureInstallation(
        connection: Connection,
        installationId: String,
        platform: Platform,
        atEpochSeconds: Long
    ): BackendDeviceInstallation {
        val existing = installation(connection, installationId)
        if (existing != null) {
            require(existing.platform == platform) {
                "Installation platform is immutable"
            }
            return existing
        }
        connection.prepareStatement(
            """
            INSERT INTO device_installation(
                installation_id, platform, created_at_epoch_seconds, updated_at_epoch_seconds
            ) VALUES (?, ?, ?, ?)
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, installationId)
            statement.setString(2, platform.name)
            statement.setLong(3, atEpochSeconds)
            statement.setLong(4, atEpochSeconds)
            statement.executeUpdate()
        }
        return installation(connection, installationId)
            ?: error("Device installation could not be persisted")
    }

    private fun insertRegistration(
        connection: Connection,
        registrationId: String,
        installationId: String,
        userId: String,
        scope: DeviceRegistrationScope,
        rawToken: String,
        atEpochSeconds: Long
    ): BackendDeviceRegistration {
        val ciphertext = tokenCipher.encrypt(registrationId, rawToken)
        try {
            connection.prepareStatement(
                """
                INSERT INTO device_registration(
                    registration_id, installation_id, user_id, environment, topic,
                    token_ciphertext, token_hash, status,
                    created_at_epoch_seconds, updated_at_epoch_seconds,
                    last_registered_at_epoch_seconds,
                    invalidated_at_epoch_seconds, invalidation_reason,
                    unregistered_at_epoch_seconds, unregistered_reason
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, NULL, NULL, NULL)
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, registrationId)
                statement.setString(2, installationId)
                statement.setString(3, userId)
                statement.setString(4, scope.environment.name)
                statement.setString(5, scope.topic)
                statement.setBytes(6, ciphertext)
                statement.setString(7, tokenHash(rawToken))
                statement.setString(8, DeviceRegistrationStatus.ACTIVE.name)
                statement.setLong(9, atEpochSeconds)
                statement.setLong(10, atEpochSeconds)
                statement.setLong(11, atEpochSeconds)
                statement.executeUpdate()
            }
        } finally {
            ciphertext.fill(0)
        }
        updateInstallationTimestamp(connection, installationId, atEpochSeconds)
        return registration(connection, registrationId)
            ?: error("Device registration could not be persisted")
    }

    private fun rotateRegistration(
        connection: Connection,
        registrationId: String,
        installationId: String,
        rawToken: String,
        atEpochSeconds: Long
    ): BackendDeviceRegistration {
        val ciphertext = tokenCipher.encrypt(registrationId, rawToken)
        try {
            connection.prepareStatement(
                """
                UPDATE device_registration
                SET token_ciphertext = ?,
                    token_hash = ?,
                    updated_at_epoch_seconds = ?,
                    last_registered_at_epoch_seconds = ?
                WHERE registration_id = ? AND status = ?
                """.trimIndent()
            ).use { statement ->
                statement.setBytes(1, ciphertext)
                statement.setString(2, tokenHash(rawToken))
                statement.setLong(3, atEpochSeconds)
                statement.setLong(4, atEpochSeconds)
                statement.setString(5, registrationId)
                statement.setString(6, DeviceRegistrationStatus.ACTIVE.name)
                check(statement.executeUpdate() == 1) { "Active registration is unavailable for rotation" }
            }
        } finally {
            ciphertext.fill(0)
        }
        updateInstallationTimestamp(connection, installationId, atEpochSeconds)
        return registration(connection, registrationId)
            ?: error("Rotated device registration could not be reloaded")
    }

    private fun updateInstallationTimestamp(
        connection: Connection,
        installationId: String,
        atEpochSeconds: Long
    ) {
        connection.prepareStatement(
            """
            UPDATE device_installation
            SET updated_at_epoch_seconds = ?
            WHERE installation_id = ?
            """.trimIndent()
        ).use { statement ->
            statement.setLong(1, atEpochSeconds)
            statement.setString(2, installationId)
            statement.executeUpdate()
        }
    }

    private fun installation(
        connection: Connection,
        installationId: String
    ): BackendDeviceInstallation? = connection.prepareStatement(
        """
        SELECT installation_id, platform, created_at_epoch_seconds, updated_at_epoch_seconds
        FROM device_installation
        WHERE installation_id = ?
        """.trimIndent()
    ).use { statement ->
        statement.setString(1, installationId)
        statement.executeQuery().use { rows ->
            if (!rows.next()) return@use null
            BackendDeviceInstallation(
                installationId = rows.getString("installation_id"),
                platform = Platform.valueOf(rows.getString("platform")),
                createdAtEpochSeconds = rows.getLong("created_at_epoch_seconds"),
                updatedAtEpochSeconds = rows.getLong("updated_at_epoch_seconds")
            )
        }
    }

    private fun registration(
        connection: Connection,
        registrationId: String
    ): BackendDeviceRegistration? = connection.prepareStatement(
        "SELECT * FROM device_registration WHERE registration_id = ?"
    ).use { statement ->
        statement.setString(1, registrationId)
        statement.executeQuery().use { rows ->
            if (!rows.next()) return@use null
            rows.registration()
        }
    }

    private fun activeRegistration(
        connection: Connection,
        installationId: String
    ): BackendDeviceRegistration? = connection.prepareStatement(
        """
        SELECT *
        FROM device_registration
        WHERE installation_id = ? AND status = ?
        LIMIT 1
        """.trimIndent()
    ).use { statement ->
        statement.setString(1, installationId)
        statement.setString(2, DeviceRegistrationStatus.ACTIVE.name)
        statement.executeQuery().use { rows ->
            if (!rows.next()) return@use null
            rows.registration()
        }
    }

    private fun registrations(rows: ResultSet): List<BackendDeviceRegistration> = buildList {
        while (rows.next()) add(rows.registration())
    }

    private fun ResultSet.registration(): BackendDeviceRegistration = BackendDeviceRegistration(
        registrationId = getString("registration_id"),
        installationId = getString("installation_id"),
        userId = getString("user_id"),
        scope = DeviceRegistrationScope.create(
            APNsEnvironment.valueOf(getString("environment")),
            getString("topic")
        ).getOrThrow(),
        tokenHash = getString("token_hash"),
        status = DeviceRegistrationStatus.valueOf(getString("status")),
        createdAtEpochSeconds = getLong("created_at_epoch_seconds"),
        updatedAtEpochSeconds = getLong("updated_at_epoch_seconds"),
        lastRegisteredAtEpochSeconds = getLong("last_registered_at_epoch_seconds"),
        invalidatedAtEpochSeconds = nullableLong("invalidated_at_epoch_seconds"),
        invalidationReason = getString("invalidation_reason")
            ?.let(DeviceRegistrationInvalidationReason::valueOf),
        unregisteredAtEpochSeconds = nullableLong("unregistered_at_epoch_seconds"),
        unregisteredReason = getString("unregistered_reason")
            ?.let(DeviceRegistrationUnregisteredReason::valueOf)
    )

    private fun ResultSet.nullableLong(column: String): Long? =
        getLong(column).takeUnless { wasNull() }

    private fun legacyIdentity(domain: String, legacyRowKey: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(legacyHmacKey, "HmacSHA256"))
        val digest = mac.doFinal("$domain\u0000$legacyRowKey".toByteArray(StandardCharsets.UTF_8))
        return "legacy-${Base64.getUrlEncoder().withoutPadding().encodeToString(digest)}"
    }

    private fun tokenHash(rawToken: String): String {
        val bytes = rawToken.toByteArray(StandardCharsets.UTF_8)
        return try {
            Base64.getUrlEncoder().withoutPadding().encodeToString(
                MessageDigest.getInstance("SHA-256").digest(bytes)
            )
        } finally {
            bytes.fill(0)
        }
    }

    private fun <T> read(block: (Connection) -> T): T {
        ensureOpen()
        return connection().use(block)
    }

    private fun <T> transaction(block: (Connection) -> T): T {
        ensureOpen()
        return connection().use { connection ->
            connection.autoCommit = false
            try {
                val result = block(connection)
                connection.commit()
                result
            } catch (failure: Throwable) {
                runCatching { connection.rollback() }
                throw failure
            }
        }
    }

    private fun connection(): Connection =
        openDeviceRegistrationJdbcConnection(databasePath, jdbcOpenTestHook).also { connection ->
            connection.createStatement().use { statement ->
                statement.execute("PRAGMA foreign_keys = ON")
                statement.execute("PRAGMA busy_timeout = 5000")
            }
        }

    private fun ensureOpen() {
        check(!closed.get()) { "Device registration store is closed" }
    }

    private fun createSchema(connection: Connection) {
        connection.createStatement().use { statement ->
            statement.execute(
                """
                CREATE TABLE IF NOT EXISTS device_installation (
                    installation_id TEXT PRIMARY KEY NOT NULL,
                    platform TEXT NOT NULL,
                    created_at_epoch_seconds INTEGER NOT NULL,
                    updated_at_epoch_seconds INTEGER NOT NULL
                )
                """.trimIndent()
            )
            statement.execute(
                """
                CREATE TABLE IF NOT EXISTS device_registration (
                    registration_id TEXT PRIMARY KEY NOT NULL,
                    installation_id TEXT NOT NULL,
                    user_id TEXT NOT NULL,
                    environment TEXT NOT NULL,
                    topic TEXT NOT NULL,
                    token_ciphertext BLOB NOT NULL,
                    token_hash TEXT NOT NULL,
                    status TEXT NOT NULL CHECK(status IN ('ACTIVE', 'INVALID', 'UNREGISTERED')),
                    created_at_epoch_seconds INTEGER NOT NULL,
                    updated_at_epoch_seconds INTEGER NOT NULL,
                    last_registered_at_epoch_seconds INTEGER NOT NULL,
                    invalidated_at_epoch_seconds INTEGER,
                    invalidation_reason TEXT,
                    unregistered_at_epoch_seconds INTEGER,
                    unregistered_reason TEXT,
                    CHECK (
                        (
                            status = 'ACTIVE'
                            AND invalidated_at_epoch_seconds IS NULL
                            AND invalidation_reason IS NULL
                            AND unregistered_at_epoch_seconds IS NULL
                            AND unregistered_reason IS NULL
                        )
                        OR (
                            status = 'INVALID'
                            AND invalidated_at_epoch_seconds IS NOT NULL
                            AND invalidation_reason IS NOT NULL
                            AND invalidation_reason IN (
                                'BAD_DEVICE_TOKEN',
                                'DEVICE_TOKEN_NOT_FOR_TOPIC',
                                'EXPIRED_TOKEN',
                                'UNREGISTERED'
                            )
                            AND unregistered_at_epoch_seconds IS NULL
                            AND unregistered_reason IS NULL
                        )
                        OR (
                            status = 'UNREGISTERED'
                            AND invalidated_at_epoch_seconds IS NULL
                            AND invalidation_reason IS NULL
                            AND unregistered_at_epoch_seconds IS NOT NULL
                            AND unregistered_reason IS NOT NULL
                            AND unregistered_reason IN (
                                'ACCOUNT_CHANGED',
                                'SCOPE_CHANGED',
                                'LOGOUT',
                                'USER_REQUESTED',
                                'ADMIN_REVOKED'
                            )
                        )
                    ),
                    FOREIGN KEY (installation_id)
                        REFERENCES device_installation(installation_id)
                        ON UPDATE RESTRICT
                        ON DELETE RESTRICT
                )
                """.trimIndent()
            )
            statement.execute(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS one_active_registration_per_installation
                ON device_registration(installation_id)
                WHERE status = 'ACTIVE'
                """.trimIndent()
            )
            statement.execute(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS one_active_registration_per_token_scope
                ON device_registration(environment, topic, token_hash)
                WHERE status = 'ACTIVE'
                """.trimIndent()
            )
            statement.execute(
                """
                CREATE INDEX IF NOT EXISTS active_registration_recipient_lookup
                ON device_registration(user_id, environment, topic, status)
                """.trimIndent()
            )
        }
    }
}

private class SqliteBackendDeviceRegistrationProviderTokenPort(
    private val databasePath: Path,
    private val tokenCipher: DeviceRegistrationTokenCipher,
    private val jdbcOpenTestHook: DeviceRegistrationJdbcOpenTestHook
) : BackendDeviceRegistrationProviderTokenPort {
    private val closed = AtomicBoolean(false)
    private val callbackDepth = ThreadLocal<Int>()

    override suspend fun withDecryptedToken(
        registrationId: String,
        block: (String) -> Unit
    ): Boolean = synchronized(REGISTRATION_STORE_LOCK) {
        check(!closed.get()) { "Device registration provider token port is closed" }
        openDeviceRegistrationJdbcConnection(databasePath, jdbcOpenTestHook).use { connection ->
            connection.prepareStatement(
                """
                SELECT token_ciphertext
                FROM device_registration
                WHERE registration_id = ? AND status = ?
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, registrationId)
                statement.setString(2, DeviceRegistrationStatus.ACTIVE.name)
                statement.executeQuery().use { rows ->
                    check(rows.next()) { "Provider token is unavailable for the selected registration" }
                    val ciphertext = rows.getBytes("token_ciphertext")
                    val plaintext = try {
                        tokenCipher.decrypt(registrationId, ciphertext)
                    } finally {
                        ciphertext.fill(0)
                    }
                    callbackDepth.set((callbackDepth.get() ?: 0) + 1)
                    try {
                        block(String(plaintext, StandardCharsets.UTF_8))
                    } finally {
                        val remainingDepth = (callbackDepth.get() ?: 1) - 1
                        if (remainingDepth == 0) {
                            callbackDepth.remove()
                        } else {
                            callbackDepth.set(remainingDepth)
                        }
                        plaintext.fill(0)
                    }
                }
            }
        }
        true
    }

    override fun close(): Unit = synchronized(REGISTRATION_STORE_LOCK) {
        check((callbackDepth.get() ?: 0) == 0) {
            "Device registration provider token port cannot close from its active callback"
        }
        if (closed.compareAndSet(false, true)) tokenCipher.close()
    }
}

internal fun interface DeviceRegistrationJdbcOpenTestHook {
    fun beforeJdbcOpen()
}

internal fun openDeviceRegistrationJdbcConnection(
    databasePath: Path,
    jdbcOpenTestHook: DeviceRegistrationJdbcOpenTestHook
): Connection {
    requirePreparedDatabasePath(databasePath)
    jdbcOpenTestHook.beforeJdbcOpen()
    val properties = Properties().apply {
        setProperty("open_mode", SQLITE_OPEN_MODE.toString())
    }
    return DriverManager.getConnection("jdbc:sqlite:$databasePath", properties)
}

private fun requirePreparedDatabasePath(databasePath: Path) {
    var current = requireNotNull(databasePath.root) {
        "The durable database path must have a root"
    }
    for (index in 0 until databasePath.nameCount) {
        current = current.resolve(databasePath.getName(index))
        require(!Files.isSymbolicLink(current)) {
            "The durable database path must not contain symbolic links"
        }
    }
    require(current.normalize() == databasePath) {
        "The durable database path changed after secure preparation"
    }

    val parent = requireNotNull(databasePath.parent) {
        "A durable database parent directory is required"
    }
    val posix = FileSystems.getDefault().supportedFileAttributeViews().contains("posix")
    require(Files.isDirectory(parent, LinkOption.NOFOLLOW_LINKS)) {
        "The durable database parent must be a directory"
    }
    require(Files.isRegularFile(databasePath, LinkOption.NOFOLLOW_LINKS)) {
        "The durable database path must identify a regular file"
    }
    if (posix) {
        require(
            Files.getPosixFilePermissions(parent, LinkOption.NOFOLLOW_LINKS) ==
                OWNER_DIRECTORY_PERMISSIONS
        ) { "Durable device registration storage must use owner-only permissions" }
        require(
            Files.getPosixFilePermissions(databasePath, LinkOption.NOFOLLOW_LINKS) ==
                OWNER_FILE_PERMISSIONS
        ) { "Durable device registration storage must use owner-only permissions" }
    }
}

private class DeviceRegistrationTokenCipher(secret: ByteArray) : AutoCloseable {
    private val keyBytes = secret.copyOf().let { keyMaterial ->
        try {
            MessageDigest.getInstance("SHA-256").digest(keyMaterial)
        } finally {
            keyMaterial.fill(0)
        }
    }
    private val random = SecureRandom()
    private val closed = AtomicBoolean(false)

    fun encrypt(registrationId: String, rawToken: String): ByteArray {
        ensureOpen()
        val nonce = ByteArray(NONCE_BYTES).also(random::nextBytes)
        val plaintext = rawToken.toByteArray(StandardCharsets.UTF_8)
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.ENCRYPT_MODE,
                SecretKeySpec(keyBytes, "AES"),
                GCMParameterSpec(TAG_BITS, nonce)
            )
            cipher.updateAAD(aad(registrationId))
            val encrypted = cipher.doFinal(plaintext)
            ByteArray(1 + nonce.size + encrypted.size).also { output ->
                output[0] = FORMAT_VERSION
                nonce.copyInto(output, destinationOffset = 1)
                encrypted.copyInto(output, destinationOffset = 1 + nonce.size)
            }
        } finally {
            plaintext.fill(0)
            nonce.fill(0)
        }
    }

    fun decrypt(registrationId: String, encoded: ByteArray): ByteArray {
        ensureOpen()
        require(encoded.size > 1 + NONCE_BYTES) { "Encrypted device token is malformed" }
        require(encoded[0] == FORMAT_VERSION) { "Encrypted device token version is unsupported" }
        val nonce = encoded.copyOfRange(1, 1 + NONCE_BYTES)
        val ciphertext = encoded.copyOfRange(1 + NONCE_BYTES, encoded.size)
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(keyBytes, "AES"),
                GCMParameterSpec(TAG_BITS, nonce)
            )
            cipher.updateAAD(aad(registrationId))
            cipher.doFinal(ciphertext)
        } finally {
            nonce.fill(0)
            ciphertext.fill(0)
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) keyBytes.fill(0)
    }

    private fun ensureOpen() {
        check(!closed.get()) { "Device token cipher is closed" }
    }

    private fun aad(registrationId: String): ByteArray =
        "wakeve/device-registration-token/v1\u0000$registrationId"
            .toByteArray(StandardCharsets.UTF_8)

    private companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val NONCE_BYTES = 12
        const val TAG_BITS = 128
        const val FORMAT_VERSION: Byte = 1
    }
}

private val REGISTRATION_STORE_LOCK = Any()

private const val SQLITE_OPEN_READWRITE = 0x00000002
private const val SQLITE_OPEN_CREATE = 0x00000004
private const val SQLITE_OPEN_URI = 0x00000040
private const val SQLITE_OPEN_NOFOLLOW = 0x01000000
private const val SQLITE_OPEN_MODE =
    SQLITE_OPEN_READWRITE or SQLITE_OPEN_CREATE or SQLITE_OPEN_URI or SQLITE_OPEN_NOFOLLOW

private val OWNER_DIRECTORY_PERMISSIONS = setOf(
    PosixFilePermission.OWNER_READ,
    PosixFilePermission.OWNER_WRITE,
    PosixFilePermission.OWNER_EXECUTE
)

private val OWNER_FILE_PERMISSIONS = setOf(
    PosixFilePermission.OWNER_READ,
    PosixFilePermission.OWNER_WRITE
)
