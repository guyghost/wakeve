package com.guyghost.wakeve.notification

import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.sql.Connection
import java.sql.ResultSet

/**
 * Server-owned SQLite implementation of the notification delivery port.
 *
 * This database deliberately does not share the mobile SQLDelight schema. Confirmation
 * acceptance is a separate database boundary from recipient/calendar/provider fan-out.
 * Delivery state is stored in the same immutable backend datastore as device registrations.
 * The legacy delivery path setting is accepted only when it names that exact datastore, so a
 * deployment cannot silently split registration identity from its delivery foreign keys.
 */
internal fun interface DeliveryEnqueueAfterAbsenceHook {
    fun afterAbsenceBeforeInsert(delivery: BackendNotificationDelivery)
}

class SqliteBackendNotificationDeliveryStoreFactory private constructor(
    private val registrationConfiguration: DeviceRegistrationStoreConfiguration,
    private val enqueueAfterAbsenceHook: DeliveryEnqueueAfterAbsenceHook,
    @Suppress("UNUSED_PARAMETER") constructorMarker: Unit
) : BackendNotificationDeliveryRuntimeFactory {
    constructor() : this(
        resolveProductionConfiguration(),
        DeliveryEnqueueAfterAbsenceHook { },
        Unit
    )

    constructor(registrationConfiguration: DeviceRegistrationStoreConfiguration) : this(
        registrationConfiguration,
        DeliveryEnqueueAfterAbsenceHook { },
        Unit
    )

    internal constructor(
        registrationConfiguration: DeviceRegistrationStoreConfiguration,
        enqueueAfterAbsenceHook: DeliveryEnqueueAfterAbsenceHook
    ) : this(registrationConfiguration, enqueueAfterAbsenceHook, Unit)

    override fun open(): BackendNotificationDeliveryStore {
        val databasePath = preparedDatabasePath()
        return SqliteBackendNotificationDeliveryStore(databasePath, enqueueAfterAbsenceHook)
    }

    override fun openRecipientTargetRuntime(
        faultInjector: BackendRecipientTargetFaultInjector
    ): BackendRecipientTargetRuntime {
        val databasePath = preparedDatabasePath()
        SqliteBackendNotificationDeliveryStore(databasePath, enqueueAfterAbsenceHook).close()
        return SqliteBackendRecipientTargetRuntime(databasePath, faultInjector)
    }

    override fun openDeliveryRuntime(
        faultInjector: BackendDeliveryWorkerFaultInjector
    ): BackendDeliveryRuntime {
        val databasePath = preparedDatabasePath()
        SqliteBackendNotificationDeliveryStore(databasePath, enqueueAfterAbsenceHook).close()
        return SqliteBackendDeliveryRuntime(databasePath, faultInjector)
    }

    internal fun preparedDatabasePath(): Path {
        requireLegacyDeliveryPathMatchesRegistrationStore()
        val registrationFactory = SqliteBackendDeviceRegistrationStoreFactory(registrationConfiguration)
        registrationFactory.open().close()
        return registrationFactory.prepareDatabasePathForSharedBackendStore()
    }

    private fun requireLegacyDeliveryPathMatchesRegistrationStore() {
        // The deprecated delivery path participates only when the canonical registration path
        // is itself process-configured. Explicitly injected factories are already immutably bound
        // and must not be coupled to an unrelated process default used by another test/runtime.
        val canonicalPathConfigured = System.getProperty(REGISTRATION_PATH_PROPERTY)?.isNotBlank() == true ||
            System.getenv(REGISTRATION_PATH_ENVIRONMENT)?.isNotBlank() == true
        if (!canonicalPathConfigured) return
        val configuredLegacyPath = configuredLegacyPath() ?: return
        val legacyPath = try {
            Path.of(configuredLegacyPath)
        } catch (_: InvalidPathException) {
            throw IllegalStateException("The legacy delivery database path is invalid")
        }
        check(legacyPath == registrationConfiguration.databasePath) {
            "The legacy delivery database path must exactly match the device registration datastore"
        }
    }

    private fun configuredLegacyPath(): String? = System.getProperty(LEGACY_DELIVERY_PATH_PROPERTY)
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: System.getenv(LEGACY_DELIVERY_PATH_ENVIRONMENT)?.trim()?.takeIf { it.isNotEmpty() }

    private companion object {
        const val LEGACY_DELIVERY_PATH_PROPERTY = "wakeve.notification.delivery.db.path"
        const val LEGACY_DELIVERY_PATH_ENVIRONMENT = "WAKEVE_NOTIFICATION_DELIVERY_DB_PATH"
        const val REGISTRATION_PATH_PROPERTY = "wakeve.notification.device-registration.db.path"
        const val REGISTRATION_PATH_ENVIRONMENT = "WAKEVE_NOTIFICATION_DEVICE_REGISTRATION_DB_PATH"

        fun resolveProductionConfiguration(): DeviceRegistrationStoreConfiguration =
            DeviceRegistrationStoreConfiguration.resolve().getOrElse {
                throw IllegalStateException(
                    "A complete device registration datastore configuration is required for notification delivery"
                )
            }
    }
}

private class SqliteBackendNotificationDeliveryStore(
    private val databasePath: Path,
    private val enqueueAfterAbsenceHook: DeliveryEnqueueAfterAbsenceHook
) : BackendNotificationDeliveryStore {
    init {
        synchronized(schemaLock) {
            connection().use { createSchema(it) }
        }
    }

    override suspend fun persistPendingRecipient(recipient: BackendNotificationRecipient): Boolean = synchronized(schemaLock) {
        connection().use { connection ->
            connection.prepareStatement(
                """
                INSERT OR IGNORE INTO notification_recipient(
                    recipient_key, effect_key, status, expires_at_epoch_seconds
                ) VALUES (?, ?, ?, ?)
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, recipient.recipientKey.value)
                statement.setString(2, recipient.effectKey.value)
                statement.setString(3, recipient.status.name)
                statement.setLong(4, recipient.expiresAtEpochSeconds)
                val created = statement.executeUpdate() == 1
                if (created) recipient.registrationIds.forEach { registrationId ->
                    insertRegistration(connection, recipient.recipientKey, registrationId)
                }
                created
            }
        }
    }

    override suspend fun recipient(recipientKey: RecipientKey): BackendNotificationRecipient? = synchronized(schemaLock) {
        connection().use { connection ->
            connection.prepareStatement(
                """
                SELECT effect_key, status, expires_at_epoch_seconds
                FROM notification_recipient
                WHERE recipient_key = ?
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, recipientKey.value)
                statement.executeQuery().use { rows ->
                    if (!rows.next()) return@synchronized null
                    BackendNotificationRecipient(
                        recipientKey = recipientKey,
                        effectKey = EffectKey(rows.getString("effect_key")),
                        status = BackendRecipientStatus.valueOf(rows.getString("status")),
                        registrationIds = registrationIds(connection, recipientKey),
                        expiresAtEpochSeconds = rows.getLong("expires_at_epoch_seconds")
                    )
                }
            }
        }
    }

    override suspend fun registerRegistration(recipientKey: String, registrationId: String): Boolean = synchronized(schemaLock) {
        connection().use { connection ->
            val typedRecipientKey = RecipientKey(recipientKey)
            val exists = connection.prepareStatement(
                "SELECT status FROM notification_recipient WHERE recipient_key = ?"
            ).use { statement ->
                statement.setString(1, recipientKey)
                statement.executeQuery().use { rows -> rows.next() && rows.getString("status") != BackendRecipientStatus.EXPIRED.name }
            }
            if (!exists) return@synchronized false

            insertRegistration(connection, typedRecipientKey, registrationId)
            connection.prepareStatement(
                """
                UPDATE notification_recipient
                SET status = ?
                WHERE recipient_key = ? AND status = ?
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, BackendRecipientStatus.TARGETED.name)
                statement.setString(2, recipientKey)
                statement.setString(3, BackendRecipientStatus.PENDING_TARGET.name)
                statement.executeUpdate()
            }
            true
        }
    }

    override suspend fun delivery(deliveryKey: DeliveryKey): BackendNotificationDelivery? = synchronized(schemaLock) {
        connection().use { delivery(it, deliveryKey) }
    }

    override suspend fun deliveryCount(deliveryKey: DeliveryKey): Int = synchronized(schemaLock) {
        connection().use { connection ->
            connection.prepareStatement("SELECT COUNT(*) FROM notification_delivery WHERE delivery_key = ?").use { statement ->
                statement.setString(1, deliveryKey.value)
                statement.executeQuery().use { rows -> rows.next(); rows.getInt(1) }
            }
        }
    }

    override suspend fun isEligible(deliveryKey: DeliveryKey, nowEpochSeconds: Long): Boolean = synchronized(schemaLock) {
        connection().use { connection ->
            connection.prepareStatement(
                """
                SELECT status, next_attempt_at_epoch_seconds, expires_at_epoch_seconds
                FROM notification_delivery
                WHERE delivery_key = ?
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, deliveryKey.value)
                statement.executeQuery().use { rows ->
                    if (!rows.next() || rows.getLong("expires_at_epoch_seconds") <= nowEpochSeconds) return@synchronized false
                    val status = BackendDeliveryStatus.valueOf(rows.getString("status"))
                    val nextAttempt = rows.getLong("next_attempt_at_epoch_seconds").takeUnless { rows.wasNull() }
                    status in setOf(BackendDeliveryStatus.QUEUED, BackendDeliveryStatus.RETRY) &&
                        (nextAttempt == null || nextAttempt <= nowEpochSeconds)
                }
            }
        }
    }

    override suspend fun acquireDeliveryAuthority(
        deliveryKey: String,
        authority: DeliveryAuthority
    ): Boolean = synchronized(schemaLock) {
        connection().use { connection ->
            connection.prepareStatement(
                """
                INSERT OR IGNORE INTO notification_delivery_authority(delivery_key, authority)
                VALUES (?, ?)
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, deliveryKey)
                statement.setString(2, authority.value)
                statement.executeUpdate()
            }
            connection.prepareStatement(
                "SELECT authority FROM notification_delivery_authority WHERE delivery_key = ?"
            ).use { statement ->
                statement.setString(1, deliveryKey)
                statement.executeQuery().use { rows -> rows.next() && rows.getString("authority") == authority.value }
            }
        }
    }

    override suspend fun resolvePendingRecipient(
        recipientKey: String,
        nowEpochSeconds: Long
    ): BackendRecipientStatus? = synchronized(schemaLock) {
        connection().use { connection ->
            val typedRecipientKey = RecipientKey(recipientKey)
            val recipient = recipient(connection, typedRecipientKey) ?: return@synchronized null
            if (recipient.status != BackendRecipientStatus.PENDING_TARGET) return@synchronized recipient.status
            if (recipient.registrationIds.isNotEmpty()) {
                updateRecipientStatus(connection, typedRecipientKey, BackendRecipientStatus.TARGETED)
                return@synchronized BackendRecipientStatus.TARGETED
            }
            if (recipient.expiresAtEpochSeconds <= nowEpochSeconds) {
                updateRecipientStatus(connection, typedRecipientKey, BackendRecipientStatus.EXPIRED)
                return@synchronized BackendRecipientStatus.EXPIRED
            }
            BackendRecipientStatus.PENDING_TARGET
        }
    }

    override suspend fun recordRecipientTerminalAcknowledgement(
        acknowledgement: BackendRecipientTerminalAcknowledgement
    ): Boolean = synchronized(schemaLock) {
        connection().use { connection ->
            connection.prepareStatement(
                """
                UPDATE notification_recipient
                SET status = ?, terminal_reason = ?, terminal_acknowledged_at_epoch_seconds = ?
                WHERE recipient_key = ?
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, BackendRecipientStatus.EXPIRED.name)
                statement.setString(2, acknowledgement.reason.name)
                statement.setLong(3, acknowledgement.acknowledgedAtEpochSeconds)
                statement.setString(4, acknowledgement.recipientKey.value)
                statement.executeUpdate() == 1
            }
        }
    }

    private fun connection(): Connection = openDeviceRegistrationJdbcConnection(
        databasePath,
        DeviceRegistrationJdbcOpenTestHook { }
    ).also { connection ->
        connection.createStatement().use {
            it.execute("PRAGMA foreign_keys = ON")
            it.execute("PRAGMA busy_timeout = 15000")
        }
    }

    private fun createSchema(connection: Connection) {
        connection.createStatement().use { it.execute("BEGIN IMMEDIATE") }
        try {
            requireDeviceRegistrationSchema(connection)
            createRecipientTable(connection)
            ensureRecipientTerminalColumns(connection)
            if (connection.columns("notification_delivery").contains("installation_id")) {
                migrateInstallationScopedDeliverySchema(connection)
            }
            createRegistrationScopedTables(connection)
            ensureDeliveryIdentityColumn(connection)
            createDeliveryAuthorityTable(connection)
            installClosedDeliveryAuthority(connection)
            connection.createStatement().use { it.execute("COMMIT") }
        } catch (failure: Throwable) {
            runCatching { connection.createStatement().use { it.execute("ROLLBACK") } }
                .onFailure(failure::addSuppressed)
            throw failure
        }
    }

    private fun createRecipientTable(connection: Connection) {
        connection.createStatement().use { statement ->
            statement.execute(
                """
                CREATE TABLE IF NOT EXISTS notification_recipient (
                    recipient_key TEXT PRIMARY KEY NOT NULL,
                    effect_key TEXT NOT NULL,
                    status TEXT NOT NULL,
                    expires_at_epoch_seconds INTEGER NOT NULL,
                    terminal_reason TEXT,
                    terminal_acknowledged_at_epoch_seconds INTEGER,
                    identity_version TEXT NOT NULL DEFAULT 'LEGACY_V1'
                )
                """.trimIndent()
            )
        }
    }

    private fun ensureRecipientTerminalColumns(connection: Connection) {
        val columns = connection.columns("notification_recipient")
        connection.createStatement().use { statement ->
            if ("terminal_reason" !in columns) {
                statement.execute("ALTER TABLE notification_recipient ADD COLUMN terminal_reason TEXT")
            }
            if ("terminal_acknowledged_at_epoch_seconds" !in columns) {
                statement.execute(
                    "ALTER TABLE notification_recipient ADD COLUMN terminal_acknowledged_at_epoch_seconds INTEGER"
                )
            }
            if ("identity_version" !in columns) {
                statement.execute(
                    "ALTER TABLE notification_recipient ADD COLUMN identity_version " +
                        "TEXT NOT NULL DEFAULT 'LEGACY_V1'"
                )
            }
        }
    }

    private fun migrateInstallationScopedDeliverySchema(connection: Connection) {
        check(connection.tableExists("notification_recipient_installation")) {
            "The legacy recipient installation mapping is required to migrate notification delivery state"
        }
        check(!connection.tableExists(REGISTRATION_MIGRATION_TABLE)) {
            "A partial notification registration migration is present"
        }
        check(!connection.tableExists(DELIVERY_MIGRATION_TABLE)) {
            "A partial notification delivery migration is present"
        }

        val unresolvedInstallationCount = connection.createStatement().use { statement ->
            statement.executeQuery(
                """
                SELECT COUNT(*)
                FROM (
                    SELECT installation_id FROM notification_recipient_installation
                    UNION
                    SELECT installation_id FROM notification_delivery
                ) legacy
                LEFT JOIN device_registration registration
                    ON registration.installation_id = legacy.installation_id
                   AND registration.status = '${DeviceRegistrationStatus.ACTIVE.name}'
                WHERE registration.registration_id IS NULL
                """.trimIndent()
            ).use { rows -> rows.next(); rows.getInt(1) }
        }
        check(unresolvedInstallationCount == 0) {
            "Every legacy delivery target must have an active backend registration before migration"
        }

        createRegistrationScopedTables(
            connection = connection,
            registrationTable = REGISTRATION_MIGRATION_TABLE,
            deliveryTable = DELIVERY_MIGRATION_TABLE
        )
        connection.createStatement().use { statement ->
            statement.execute(
                """
                INSERT INTO $REGISTRATION_MIGRATION_TABLE(recipient_key, device_registration_id)
                SELECT legacy.recipient_key, registration.registration_id
                FROM notification_recipient_installation legacy
                JOIN device_registration registration
                  ON registration.installation_id = legacy.installation_id
                 AND registration.status = '${DeviceRegistrationStatus.ACTIVE.name}'
                UNION
                SELECT legacy.recipient_key, registration.registration_id
                FROM notification_delivery legacy
                JOIN device_registration registration
                  ON registration.installation_id = legacy.installation_id
                 AND registration.status = '${DeviceRegistrationStatus.ACTIVE.name}'
                """.trimIndent()
            )
            statement.execute(
                """
                INSERT INTO $DELIVERY_MIGRATION_TABLE(
                    delivery_key, logical_notification_id, idempotency_key, recipient_key,
                    device_registration_id, provider, status, attempt,
                    next_attempt_at_epoch_seconds, expires_at_epoch_seconds,
                    lease_owner, lease_expires_at_epoch_seconds, accepted_at,
                    provider_status, provider_reason, provider_request_id
                )
                SELECT legacy.delivery_key, legacy.delivery_key, legacy.delivery_key,
                       legacy.recipient_key, registration.registration_id,
                       legacy.provider, legacy.status, legacy.attempt,
                       legacy.next_attempt_at_epoch_seconds, legacy.expires_at_epoch_seconds,
                       legacy.lease_owner, legacy.lease_expires_at_epoch_seconds,
                       NULL, NULL, NULL, NULL
                FROM notification_delivery legacy
                JOIN device_registration registration
                  ON registration.installation_id = legacy.installation_id
                 AND registration.status = '${DeviceRegistrationStatus.ACTIVE.name}'
                """.trimIndent()
            )
        }

        check(connection.rowCount("notification_delivery") == connection.rowCount(DELIVERY_MIGRATION_TABLE)) {
            "Notification delivery migration must preserve every durable delivery"
        }

        connection.createStatement().use { statement ->
            statement.execute("DROP TABLE notification_delivery")
            statement.execute("DROP TABLE notification_recipient_installation")
            statement.execute("ALTER TABLE $REGISTRATION_MIGRATION_TABLE RENAME TO notification_recipient_registration")
            statement.execute("ALTER TABLE $DELIVERY_MIGRATION_TABLE RENAME TO notification_delivery")
        }
    }

    private fun createRegistrationScopedTables(
        connection: Connection,
        registrationTable: String = "notification_recipient_registration",
        deliveryTable: String = "notification_delivery"
    ) {
        connection.createStatement().use { statement ->
            statement.execute(
                """
                CREATE TABLE IF NOT EXISTS $registrationTable (
                    recipient_key TEXT NOT NULL,
                    device_registration_id TEXT NOT NULL,
                    PRIMARY KEY (recipient_key, device_registration_id),
                    FOREIGN KEY (recipient_key)
                        REFERENCES notification_recipient(recipient_key)
                        ON UPDATE RESTRICT
                        ON DELETE RESTRICT,
                    FOREIGN KEY (device_registration_id)
                        REFERENCES device_registration(registration_id)
                        ON UPDATE RESTRICT
                        ON DELETE RESTRICT
                )
                """.trimIndent()
            )
            statement.execute(
                """
                CREATE TABLE IF NOT EXISTS $deliveryTable (
                    delivery_key TEXT PRIMARY KEY NOT NULL,
                    logical_notification_id TEXT NOT NULL,
                    idempotency_key TEXT NOT NULL UNIQUE,
                    recipient_key TEXT NOT NULL,
                    device_registration_id TEXT NOT NULL,
                    provider TEXT NOT NULL,
                    status TEXT NOT NULL,
                    attempt INTEGER NOT NULL,
                    next_attempt_at_epoch_seconds INTEGER,
                    expires_at_epoch_seconds INTEGER NOT NULL,
                    lease_owner TEXT,
                    lease_expires_at_epoch_seconds INTEGER,
                    accepted_at INTEGER,
                    provider_status INTEGER,
                    provider_reason TEXT,
                    provider_request_id TEXT,
                    identity_version TEXT NOT NULL DEFAULT 'LEGACY_V1',
                    UNIQUE (recipient_key, device_registration_id, provider),
                    FOREIGN KEY (device_registration_id)
                        REFERENCES device_registration(registration_id)
                        ON UPDATE RESTRICT
                        ON DELETE RESTRICT
                )
                """.trimIndent()
            )
        }
    }

    private fun ensureDeliveryIdentityColumn(connection: Connection) {
        val columns = connection.columns("notification_delivery")
        if ("identity_version" !in columns) {
            connection.createStatement().use { statement ->
                statement.execute(
                    "ALTER TABLE notification_delivery ADD COLUMN identity_version " +
                        "TEXT NOT NULL DEFAULT 'LEGACY_V1'"
                )
            }
        }
    }

    private fun createDeliveryAuthorityTable(connection: Connection) {
        connection.createStatement().use { statement ->
            statement.execute(
                """
                CREATE TABLE IF NOT EXISTS notification_delivery_authority (
                    delivery_key TEXT PRIMARY KEY NOT NULL,
                    authority TEXT NOT NULL,
                    fencing_token INTEGER NOT NULL DEFAULT 1
                )
                """.trimIndent()
            )
        }
        if ("fencing_token" !in connection.columns("notification_delivery_authority")) {
            connection.createStatement().use {
                it.execute(
                    "ALTER TABLE notification_delivery_authority ADD COLUMN " +
                        "fencing_token INTEGER NOT NULL DEFAULT 1"
                )
            }
        }
    }

    private fun installClosedDeliveryAuthority(connection: Connection) {
        val invalidCount = connection.createStatement().use { statement ->
            statement.executeQuery(
                "SELECT COUNT(*) FROM notification_delivery_authority " +
                    "WHERE authority NOT IN ('legacy', 'outbox-v2')"
            ).use { rows -> rows.next(); rows.getLong(1) }
        }
        check(invalidCount == 0L) { "Notification delivery authority contains unsupported values" }
        connection.createStatement().use { statement ->
            statement.execute(
                "INSERT OR IGNORE INTO notification_delivery_authority(delivery_key, authority, fencing_token) " +
                    "SELECT delivery_key, 'legacy', 1 FROM notification_delivery"
            )
            statement.execute(
                """
                CREATE TRIGGER IF NOT EXISTS notification_delivery_authority_closed_insert
                BEFORE INSERT ON notification_delivery_authority
                WHEN NEW.authority NOT IN ('legacy', 'outbox-v2')
                BEGIN
                    SELECT RAISE(ABORT, 'invalid notification delivery authority');
                END
                """.trimIndent()
            )
            statement.execute(
                """
                CREATE TRIGGER IF NOT EXISTS notification_delivery_authority_closed_update
                BEFORE UPDATE OF authority ON notification_delivery_authority
                WHEN NEW.authority NOT IN ('legacy', 'outbox-v2')
                BEGIN
                    SELECT RAISE(ABORT, 'invalid notification delivery authority');
                END
                """.trimIndent()
            )
        }
    }

    private fun requireDeviceRegistrationSchema(connection: Connection) {
        val registrationColumns = connection.createStatement().use { statement ->
            statement.executeQuery("PRAGMA table_info(device_registration)").use { rows ->
                buildSet { while (rows.next()) add(rows.getString("name")) }
            }
        }
        check("registration_id" in registrationColumns) {
            "The backend device registration schema is required before notification delivery storage"
        }
    }

    private fun Connection.columns(table: String): Set<String> = createStatement().use { statement ->
        statement.executeQuery("PRAGMA table_info($table)").use { rows ->
            buildSet { while (rows.next()) add(rows.getString("name")) }
        }
    }

    private fun Connection.tableExists(table: String): Boolean = prepareStatement(
        "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?"
    ).use { statement ->
        statement.setString(1, table)
        statement.executeQuery().use { rows -> rows.next() }
    }

    private fun Connection.rowCount(table: String): Int = createStatement().use { statement ->
        statement.executeQuery("SELECT COUNT(*) FROM $table").use { rows -> rows.next(); rows.getInt(1) }
    }

    private fun insertRegistration(connection: Connection, recipientKey: RecipientKey, registrationId: String) {
        connection.prepareStatement(
            """
            INSERT OR IGNORE INTO notification_recipient_registration(recipient_key, device_registration_id)
            VALUES (?, ?)
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, recipientKey.value)
            statement.setString(2, registrationId)
            statement.executeUpdate()
        }
    }

    private fun insertDelivery(connection: Connection, delivery: BackendNotificationDelivery): Boolean = connection.prepareStatement(
        """
        INSERT OR IGNORE INTO notification_delivery(
            delivery_key, logical_notification_id, idempotency_key, recipient_key,
            device_registration_id, provider, status, attempt,
            next_attempt_at_epoch_seconds, expires_at_epoch_seconds,
            lease_owner, lease_expires_at_epoch_seconds, accepted_at,
            provider_status, provider_reason, provider_request_id
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()
    ).use { statement ->
        statement.setString(1, delivery.deliveryKey.value)
        statement.setString(2, delivery.logicalNotificationId)
        statement.setString(3, delivery.idempotencyKey)
        statement.setString(4, delivery.recipientKey.value)
        statement.setString(5, delivery.registrationId)
        statement.setString(6, delivery.provider)
        statement.setString(7, delivery.status.name)
        statement.setInt(8, delivery.attempt)
        statement.setNullableLong(9, delivery.nextAttemptAtEpochSeconds)
        statement.setLong(10, delivery.expiresAtEpochSeconds)
        statement.setString(11, delivery.leaseOwner)
        statement.setNullableLong(12, delivery.leaseExpiresAtEpochSeconds)
        statement.setNullableLong(13, delivery.acceptedAtEpochSeconds)
        statement.setNullableInt(14, delivery.providerStatus)
        statement.setString(15, delivery.providerReason?.name)
        statement.setString(16, delivery.providerRequestId)
        statement.executeUpdate() == 1
    }

    private fun registrationIds(connection: Connection, recipientKey: RecipientKey): Set<String> = connection.prepareStatement(
        "SELECT device_registration_id FROM notification_recipient_registration WHERE recipient_key = ?"
    ).use { statement ->
        statement.setString(1, recipientKey.value)
        statement.executeQuery().use { rows ->
            buildSet {
                while (rows.next()) add(rows.getString("device_registration_id"))
            }
        }
    }

    private fun delivery(connection: Connection, deliveryKey: DeliveryKey): BackendNotificationDelivery? = connection.prepareStatement(
        """
        SELECT logical_notification_id, idempotency_key, recipient_key,
               device_registration_id, provider, status, attempt,
               next_attempt_at_epoch_seconds, expires_at_epoch_seconds,
               lease_owner, lease_expires_at_epoch_seconds, accepted_at,
               provider_status, provider_reason, provider_request_id
        FROM notification_delivery
        WHERE delivery_key = ?
        """.trimIndent()
    ).use { statement ->
        statement.setString(1, deliveryKey.value)
        statement.executeQuery().use { rows ->
            if (!rows.next()) return@use null
            BackendNotificationDelivery(
                deliveryKey = deliveryKey,
                recipientKey = RecipientKey(rows.getString("recipient_key")),
                registrationId = rows.getString("device_registration_id"),
                provider = rows.getString("provider"),
                status = BackendDeliveryStatus.valueOf(rows.getString("status")),
                attempt = rows.getInt("attempt"),
                nextAttemptAtEpochSeconds = rows.getNullableLong("next_attempt_at_epoch_seconds"),
                expiresAtEpochSeconds = rows.getLong("expires_at_epoch_seconds"),
                leaseOwner = rows.getString("lease_owner"),
                leaseExpiresAtEpochSeconds = rows.getNullableLong("lease_expires_at_epoch_seconds"),
                logicalNotificationId = rows.getString("logical_notification_id"),
                idempotencyKey = rows.getString("idempotency_key"),
                acceptedAtEpochSeconds = rows.getNullableLong("accepted_at"),
                providerStatus = rows.getNullableInt("provider_status"),
                providerReason = rows.getString("provider_reason")?.let(::persistedProviderReasonFromLegacy),
                providerRequestId = rows.getString("provider_request_id")
            )
        }
    }

    private fun recipient(connection: Connection, recipientKey: RecipientKey): BackendNotificationRecipient? = connection.prepareStatement(
        """
        SELECT effect_key, status, expires_at_epoch_seconds
        FROM notification_recipient
        WHERE recipient_key = ?
        """.trimIndent()
    ).use { statement ->
        statement.setString(1, recipientKey.value)
        statement.executeQuery().use { rows ->
            if (!rows.next()) return@use null
            BackendNotificationRecipient(
                recipientKey = recipientKey,
                effectKey = EffectKey(rows.getString("effect_key")),
                status = BackendRecipientStatus.valueOf(rows.getString("status")),
                registrationIds = registrationIds(connection, recipientKey),
                expiresAtEpochSeconds = rows.getLong("expires_at_epoch_seconds")
            )
        }
    }

    private fun updateRecipientStatus(connection: Connection, recipientKey: RecipientKey, status: BackendRecipientStatus) {
        connection.prepareStatement("UPDATE notification_recipient SET status = ? WHERE recipient_key = ?").use { statement ->
            statement.setString(1, status.name)
            statement.setString(2, recipientKey.value)
            statement.executeUpdate()
        }
    }

    private fun java.sql.PreparedStatement.setNullableLong(index: Int, value: Long?) {
        if (value == null) setObject(index, null) else setLong(index, value)
    }

    private fun java.sql.PreparedStatement.setNullableInt(index: Int, value: Int?) {
        if (value == null) setObject(index, null) else setInt(index, value)
    }

    private fun ResultSet.getNullableLong(column: String): Long? = getLong(column).takeUnless { wasNull() }

    private fun ResultSet.getNullableInt(column: String): Int? = getInt(column).takeUnless { wasNull() }

    private fun BackendNotificationDelivery.hasSameImmutableIdentityAs(
        other: BackendNotificationDelivery
    ): Boolean =
        deliveryKey == other.deliveryKey &&
            logicalNotificationId == other.logicalNotificationId &&
            idempotencyKey == other.idempotencyKey &&
            recipientKey == other.recipientKey &&
            registrationId == other.registrationId &&
            provider == other.provider

    private companion object {
        const val REGISTRATION_MIGRATION_TABLE = "notification_recipient_registration_migration"
        const val DELIVERY_MIGRATION_TABLE = "notification_delivery_migration"
        val schemaLock = Any()
    }
}
