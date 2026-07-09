package com.guyghost.wakeve.notification

import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet

/**
 * Server-owned SQLite implementation of the notification delivery port.
 *
 * This database deliberately does not share the mobile SQLDelight schema. Confirmation
 * acceptance is a separate database boundary from recipient/calendar/provider fan-out.
 * Deployments set `WAKEVE_NOTIFICATION_DELIVERY_DB_PATH` (or the matching JVM property)
 * to retain the store across server process restarts. Opening this production factory without
 * that explicit durable path is rejected; test/development callers must inject a path instead
 * of silently receiving a process-local fallback.
 */
class SqliteBackendNotificationDeliveryStoreFactory : BackendNotificationDeliveryStoreFactory {
    override fun open(): BackendNotificationDeliveryStore = SqliteBackendNotificationDeliveryStore(storageUrl())

    private fun storageUrl(): String = "jdbc:sqlite:${configuredPath()
        ?: error("WAKEVE_NOTIFICATION_DELIVERY_DB_PATH must be configured for the production delivery store")}"

    private fun configuredPath(): String? = System.getProperty("wakeve.notification.delivery.db.path")
        ?.takeIf { it.isNotBlank() }
        ?: System.getenv("WAKEVE_NOTIFICATION_DELIVERY_DB_PATH")?.takeIf { it.isNotBlank() }

}

private class SqliteBackendNotificationDeliveryStore(
    private val jdbcUrl: String
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
                if (created) recipient.installationIds.forEach { installationId ->
                    insertInstallation(connection, recipient.recipientKey, installationId)
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
                        installationIds = installationIds(connection, recipientKey),
                        expiresAtEpochSeconds = rows.getLong("expires_at_epoch_seconds")
                    )
                }
            }
        }
    }

    override suspend fun registerInstallation(recipientKey: RecipientKey, installationId: String): Boolean = synchronized(schemaLock) {
        connection().use { connection ->
            val exists = connection.prepareStatement(
                "SELECT status FROM notification_recipient WHERE recipient_key = ?"
            ).use { statement ->
                statement.setString(1, recipientKey.value)
                statement.executeQuery().use { rows -> rows.next() && rows.getString("status") != BackendRecipientStatus.EXPIRED.name }
            }
            if (!exists) return@synchronized false

            insertInstallation(connection, recipientKey, installationId)
            connection.prepareStatement(
                """
                UPDATE notification_recipient
                SET status = ?
                WHERE recipient_key = ? AND status = ?
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, BackendRecipientStatus.TARGETED.name)
                statement.setString(2, recipientKey.value)
                statement.setString(3, BackendRecipientStatus.PENDING_TARGET.name)
                statement.executeUpdate()
            }
            true
        }
    }

    override suspend fun enqueue(delivery: BackendNotificationDelivery): BackendEnqueueResult? = synchronized(schemaLock) {
        connection().use { connection ->
            val existing = delivery(connection, delivery.deliveryKey)
            val created = when {
                existing == null -> insertDelivery(connection, delivery)
                existing == delivery -> false
                // A fan-out is currently disabled, so this store is only a durable shadow
                // projection. A new business-expiry horizon refreshes that unsent projection
                // in-place, retaining the unique delivery key and never creating a second row.
                // Once sends are enabled, the rollout must make a changed logical payload a
                // conflict instead of replacing an unmaterialized shadow record.
                existing.expiresAtEpochSeconds != delivery.expiresAtEpochSeconds -> {
                    replaceUnmaterializedShadowDelivery(connection, delivery)
                    true
                }
                else -> false
            }
            delivery(connection, delivery.deliveryKey)?.let { BackendEnqueueResult(it, created) }
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

    override suspend fun acquireLease(
        deliveryKey: DeliveryKey,
        owner: String,
        nowEpochSeconds: Long,
        leaseUntilEpochSeconds: Long
    ): Boolean = synchronized(schemaLock) {
        connection().use { connection ->
            connection.prepareStatement(
                """
                UPDATE notification_delivery
                SET status = ?, lease_owner = ?, lease_expires_at_epoch_seconds = ?
                WHERE delivery_key = ?
                  AND expires_at_epoch_seconds > ?
                  AND (lease_expires_at_epoch_seconds IS NULL OR lease_expires_at_epoch_seconds <= ?)
                  AND status NOT IN (?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, BackendDeliveryStatus.LEASED.name)
                statement.setString(2, owner)
                statement.setLong(3, leaseUntilEpochSeconds)
                statement.setString(4, deliveryKey.value)
                statement.setLong(5, nowEpochSeconds)
                statement.setLong(6, nowEpochSeconds)
                listOf(
                    BackendDeliveryStatus.ACCEPTED_BY_APNS,
                    BackendDeliveryStatus.INVALID_TOKEN,
                    BackendDeliveryStatus.REJECTED_PAYLOAD,
                    BackendDeliveryStatus.PROVIDER_AUTH_BLOCKED,
                    BackendDeliveryStatus.EXPIRED,
                    BackendDeliveryStatus.RETRY_EXHAUSTED,
                    BackendDeliveryStatus.CANCELLED
                ).forEachIndexed { index, status -> statement.setString(index + 7, status.name) }
                statement.executeUpdate() == 1
            }
        }
    }

    override suspend fun recordRetry(
        deliveryKey: DeliveryKey,
        attempt: Int,
        nextAttemptAtEpochSeconds: Long
    ): Boolean = synchronized(schemaLock) {
        connection().use { connection ->
            connection.prepareStatement(
                """
                UPDATE notification_delivery
                SET status = ?, attempt = ?, next_attempt_at_epoch_seconds = ?,
                    lease_owner = NULL, lease_expires_at_epoch_seconds = NULL
                WHERE delivery_key = ?
                  AND expires_at_epoch_seconds > ?
                  AND status NOT IN (?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, BackendDeliveryStatus.RETRY.name)
                statement.setInt(2, attempt)
                statement.setLong(3, nextAttemptAtEpochSeconds)
                statement.setString(4, deliveryKey.value)
                statement.setLong(5, nextAttemptAtEpochSeconds)
                listOf(
                    BackendDeliveryStatus.ACCEPTED_BY_APNS,
                    BackendDeliveryStatus.INVALID_TOKEN,
                    BackendDeliveryStatus.REJECTED_PAYLOAD,
                    BackendDeliveryStatus.PROVIDER_AUTH_BLOCKED,
                    BackendDeliveryStatus.EXPIRED,
                    BackendDeliveryStatus.RETRY_EXHAUSTED,
                    BackendDeliveryStatus.CANCELLED
                ).forEachIndexed { index, status -> statement.setString(index + 6, status.name) }
                statement.executeUpdate() == 1
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
            if (recipient.installationIds.isNotEmpty()) {
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

    private fun connection(): Connection = DriverManager.getConnection(jdbcUrl).also { connection ->
        connection.createStatement().use { it.execute("PRAGMA foreign_keys = ON") }
    }

    private fun createSchema(connection: Connection) {
        connection.createStatement().use { statement ->
            statement.execute(
                """
                CREATE TABLE IF NOT EXISTS notification_recipient (
                    recipient_key TEXT PRIMARY KEY NOT NULL,
                    effect_key TEXT NOT NULL,
                    status TEXT NOT NULL,
                    expires_at_epoch_seconds INTEGER NOT NULL,
                    terminal_reason TEXT,
                    terminal_acknowledged_at_epoch_seconds INTEGER
                )
                """.trimIndent()
            )
            statement.execute(
                """
                CREATE TABLE IF NOT EXISTS notification_recipient_installation (
                    recipient_key TEXT NOT NULL,
                    installation_id TEXT NOT NULL,
                    PRIMARY KEY (recipient_key, installation_id),
                    FOREIGN KEY (recipient_key) REFERENCES notification_recipient(recipient_key) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            statement.execute(
                """
                CREATE TABLE IF NOT EXISTS notification_delivery (
                    delivery_key TEXT PRIMARY KEY NOT NULL,
                    recipient_key TEXT NOT NULL,
                    installation_id TEXT NOT NULL,
                    provider TEXT NOT NULL,
                    status TEXT NOT NULL,
                    attempt INTEGER NOT NULL,
                    next_attempt_at_epoch_seconds INTEGER,
                    expires_at_epoch_seconds INTEGER NOT NULL,
                    lease_owner TEXT,
                    lease_expires_at_epoch_seconds INTEGER
                )
                """.trimIndent()
            )
            statement.execute(
                """
                CREATE TABLE IF NOT EXISTS notification_delivery_authority (
                    delivery_key TEXT PRIMARY KEY NOT NULL,
                    authority TEXT NOT NULL
                )
                """.trimIndent()
            )
        }
    }

    private fun insertInstallation(connection: Connection, recipientKey: RecipientKey, installationId: String) {
        connection.prepareStatement(
            """
            INSERT OR IGNORE INTO notification_recipient_installation(recipient_key, installation_id)
            VALUES (?, ?)
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, recipientKey.value)
            statement.setString(2, installationId)
            statement.executeUpdate()
        }
    }

    private fun insertDelivery(connection: Connection, delivery: BackendNotificationDelivery): Boolean = connection.prepareStatement(
        """
        INSERT INTO notification_delivery(
            delivery_key, recipient_key, installation_id, provider, status, attempt,
            next_attempt_at_epoch_seconds, expires_at_epoch_seconds,
            lease_owner, lease_expires_at_epoch_seconds
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()
    ).use { statement ->
        statement.setString(1, delivery.deliveryKey.value)
        statement.setString(2, delivery.recipientKey.value)
        statement.setString(3, delivery.installationId)
        statement.setString(4, delivery.provider)
        statement.setString(5, delivery.status.name)
        statement.setInt(6, delivery.attempt)
        statement.setNullableLong(7, delivery.nextAttemptAtEpochSeconds)
        statement.setLong(8, delivery.expiresAtEpochSeconds)
        statement.setString(9, delivery.leaseOwner)
        statement.setNullableLong(10, delivery.leaseExpiresAtEpochSeconds)
        statement.executeUpdate() == 1
    }

    private fun replaceUnmaterializedShadowDelivery(connection: Connection, delivery: BackendNotificationDelivery) {
        connection.prepareStatement(
            """
            UPDATE notification_delivery
            SET recipient_key = ?, installation_id = ?, provider = ?, status = ?, attempt = ?,
                next_attempt_at_epoch_seconds = ?, expires_at_epoch_seconds = ?,
                lease_owner = ?, lease_expires_at_epoch_seconds = ?
            WHERE delivery_key = ?
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, delivery.recipientKey.value)
            statement.setString(2, delivery.installationId)
            statement.setString(3, delivery.provider)
            statement.setString(4, delivery.status.name)
            statement.setInt(5, delivery.attempt)
            statement.setNullableLong(6, delivery.nextAttemptAtEpochSeconds)
            statement.setLong(7, delivery.expiresAtEpochSeconds)
            statement.setString(8, delivery.leaseOwner)
            statement.setNullableLong(9, delivery.leaseExpiresAtEpochSeconds)
            statement.setString(10, delivery.deliveryKey.value)
            statement.executeUpdate()
        }
    }

    private fun installationIds(connection: Connection, recipientKey: RecipientKey): Set<String> = connection.prepareStatement(
        "SELECT installation_id FROM notification_recipient_installation WHERE recipient_key = ?"
    ).use { statement ->
        statement.setString(1, recipientKey.value)
        statement.executeQuery().use { rows ->
            buildSet {
                while (rows.next()) add(rows.getString("installation_id"))
            }
        }
    }

    private fun delivery(connection: Connection, deliveryKey: DeliveryKey): BackendNotificationDelivery? = connection.prepareStatement(
        """
        SELECT recipient_key, installation_id, provider, status, attempt,
               next_attempt_at_epoch_seconds, expires_at_epoch_seconds,
               lease_owner, lease_expires_at_epoch_seconds
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
                installationId = rows.getString("installation_id"),
                provider = rows.getString("provider"),
                status = BackendDeliveryStatus.valueOf(rows.getString("status")),
                attempt = rows.getInt("attempt"),
                nextAttemptAtEpochSeconds = rows.getNullableLong("next_attempt_at_epoch_seconds"),
                expiresAtEpochSeconds = rows.getLong("expires_at_epoch_seconds"),
                leaseOwner = rows.getString("lease_owner"),
                leaseExpiresAtEpochSeconds = rows.getNullableLong("lease_expires_at_epoch_seconds")
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
                installationIds = installationIds(connection, recipientKey),
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

    private fun ResultSet.getNullableLong(column: String): Long? = getLong(column).takeUnless { wasNull() }

    private companion object {
        val schemaLock = Any()
    }
}
