package com.guyghost.wakeve.notification

import java.nio.file.Path
import java.security.MessageDigest
import java.sql.Connection
import java.sql.ResultSet
import java.util.Base64
import java.util.UUID

internal val backendNotificationRuntimeLock = Any()

private fun openNotificationRuntimeConnection(databasePath: Path): Connection =
    openDeviceRegistrationJdbcConnection(databasePath, DeviceRegistrationJdbcOpenTestHook { }).also { connection ->
        connection.createStatement().use {
            it.execute("PRAGMA foreign_keys = ON")
            it.execute("PRAGMA busy_timeout = 15000")
        }
    }

internal fun ensureBackendNotificationRuntimeSchema(connection: Connection) {
    connection.createStatement().use { it.execute("BEGIN IMMEDIATE") }
    try {
        connection.createStatement().use { statement ->
            statement.execute(
                """
                CREATE TABLE IF NOT EXISTS domain_event_ingestion (
                    domain_event_id TEXT NOT NULL,
                    effect_type TEXT NOT NULL,
                    schema_version INTEGER NOT NULL,
                    transaction_id TEXT NOT NULL,
                    effect_key TEXT NOT NULL,
                    logical_notification_id TEXT NOT NULL,
                    identity_version TEXT NOT NULL DEFAULT 'LEGACY_V1',
                    PRIMARY KEY(domain_event_id, effect_type, schema_version),
                    UNIQUE(effect_key)
                )
                """.trimIndent()
            )
            statement.execute(
                """
                CREATE TABLE IF NOT EXISTS notification_logical (
                    logical_notification_id TEXT PRIMARY KEY NOT NULL,
                    effect_key TEXT NOT NULL UNIQUE,
                    decision_sync_status TEXT NOT NULL,
                    effect_dispatch_status TEXT NOT NULL,
                    identity_version TEXT NOT NULL DEFAULT 'LEGACY_V1'
                )
                """.trimIndent()
            )
            statement.execute(
                """
                CREATE TABLE IF NOT EXISTS notification_recipient_runtime (
                    recipient_key TEXT PRIMARY KEY NOT NULL,
                    state TEXT NOT NULL DEFAULT 'PENDING_TARGET',
                    attempt INTEGER NOT NULL DEFAULT 0,
                    next_attempt_at INTEGER,
                    logical_now INTEGER NOT NULL DEFAULT 100,
                    clock_revision INTEGER NOT NULL DEFAULT 0,
                    checkpoint_revision INTEGER NOT NULL DEFAULT 0,
                    lease_holder TEXT,
                    lease_version INTEGER NOT NULL DEFAULT 0,
                    lease_fence INTEGER NOT NULL DEFAULT 0,
                    lease_expires_at INTEGER,
                    checkpoint_kind TEXT,
                    checkpoint_effect_id TEXT,
                    checkpoint_receipt_id TEXT,
                    checkpoint_holder TEXT,
                    checkpoint_lease_version INTEGER,
                    checkpoint_fence INTEGER,
                    checkpoint_next_attempt_at INTEGER,
                    checkpoint_effect_requested INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY(recipient_key) REFERENCES notification_recipient(recipient_key)
                )
                """.trimIndent()
            )
            statement.execute(
                """
                CREATE TABLE IF NOT EXISTS notification_recipient_checkpoint_delivery (
                    recipient_key TEXT NOT NULL,
                    checkpoint_revision INTEGER NOT NULL,
                    delivery_key TEXT NOT NULL,
                    device_registration_id TEXT NOT NULL,
                    PRIMARY KEY(recipient_key, checkpoint_revision, device_registration_id)
                )
                """.trimIndent()
            )
            statement.execute(
                """
                CREATE TABLE IF NOT EXISTS notification_recipient_fanout_receipt (
                    transaction_receipt_id TEXT PRIMARY KEY NOT NULL,
                    recipient_key TEXT NOT NULL,
                    checkpoint_revision INTEGER NOT NULL,
                    effect_id TEXT NOT NULL,
                    fencing_token INTEGER NOT NULL,
                    UNIQUE(recipient_key, checkpoint_revision),
                    FOREIGN KEY(recipient_key) REFERENCES notification_recipient(recipient_key)
                )
                """.trimIndent()
            )
            statement.execute(
                """
                CREATE TABLE IF NOT EXISTS notification_delivery_provider_checkpoint (
                    delivery_key TEXT PRIMARY KEY NOT NULL,
                    effect_id TEXT NOT NULL,
                    checkpoint_revision INTEGER NOT NULL,
                    outcome TEXT NOT NULL,
                    reason TEXT NOT NULL,
                    http_status INTEGER,
                    invalidation_reason TEXT,
                    accepted_at INTEGER,
                    next_attempt_at INTEGER,
                    next_attempt INTEGER,
                    provider_request_id TEXT,
                    lease_holder TEXT,
                    lease_version INTEGER,
                    lease_fence INTEGER,
                    effect_requested INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )
            statement.execute(
                """
                CREATE TABLE IF NOT EXISTS notification_provider_credential_circuit (
                    singleton INTEGER PRIMARY KEY CHECK(singleton = 1),
                    blocked_credential_version TEXT,
                    blocked_credential_fingerprint TEXT
                )
                """.trimIndent()
            )
            statement.execute(
                "INSERT OR IGNORE INTO notification_provider_credential_circuit(" +
                    "singleton, blocked_credential_version, blocked_credential_fingerprint) VALUES (1, NULL, NULL)"
            )
        }
        ensureColumns(
            connection,
            "domain_event_ingestion",
            mapOf("identity_version" to "TEXT NOT NULL DEFAULT 'LEGACY_V1'")
        )
        ensureColumns(
            connection,
            "notification_logical",
            mapOf("identity_version" to "TEXT NOT NULL DEFAULT 'LEGACY_V1'")
        )
        ensureColumns(
            connection,
            "notification_recipient",
            mapOf("identity_version" to "TEXT NOT NULL DEFAULT 'LEGACY_V1'")
        )
        ensureColumns(
            connection,
            "notification_delivery_provider_checkpoint",
            mapOf(
                "http_status" to "INTEGER",
                "invalidation_reason" to "TEXT",
                "next_attempt" to "INTEGER"
            )
        )
        ensureColumns(
            connection,
            "notification_provider_credential_circuit",
            mapOf("blocked_credential_fingerprint" to "TEXT")
        )
        ensureColumns(
            connection,
            "notification_delivery",
            mapOf(
                "logical_now" to "INTEGER NOT NULL DEFAULT 100",
                "clock_revision" to "INTEGER NOT NULL DEFAULT 0",
                "checkpoint_revision" to "INTEGER NOT NULL DEFAULT 0",
                "lease_version" to "INTEGER NOT NULL DEFAULT 0",
                "lease_fence" to "INTEGER NOT NULL DEFAULT 0",
                "provider_checkpoint_count" to "INTEGER NOT NULL DEFAULT 0",
                "credential_version" to "TEXT",
                "credential_fingerprint" to "TEXT",
                "auth_refresh_count" to "INTEGER NOT NULL DEFAULT 0",
                "correlation_id" to "TEXT",
                "identity_version" to "TEXT NOT NULL DEFAULT 'LEGACY_V1'"
            )
        )
        ensureColumns(
            connection,
            "notification_delivery_authority",
            mapOf("fencing_token" to "INTEGER NOT NULL DEFAULT 1")
        )
        connection.createStatement().use { it.execute("COMMIT") }
    } catch (failure: Throwable) {
        runCatching { connection.createStatement().use { it.execute("ROLLBACK") } }
            .onFailure(failure::addSuppressed)
        throw failure
    }
}

private fun ensureColumns(connection: Connection, table: String, definitions: Map<String, String>) {
    val columns = connection.createStatement().use { statement ->
        statement.executeQuery("PRAGMA table_info($table)").use { rows ->
            buildSet { while (rows.next()) add(rows.getString("name")) }
        }
    }
    connection.createStatement().use { statement ->
        definitions.forEach { (name, definition) ->
            if (name !in columns) statement.execute("ALTER TABLE $table ADD COLUMN $name $definition")
        }
    }
}

internal class SqliteBackendRecipientTargetRuntime(
    private val databasePath: Path,
    private val faultInjector: BackendRecipientTargetFaultInjector
) : BackendRecipientTargetRuntime {
    init {
        synchronized(backendNotificationRuntimeLock) {
            openNotificationRuntimeConnection(databasePath).use(::ensureBackendNotificationRuntimeSchema)
        }
    }

    override suspend fun current(recipientKey: RecipientKey): BackendRecipientTargetSnapshot? =
        synchronized(backendNotificationRuntimeLock) {
            openNotificationRuntimeConnection(databasePath).use { connection -> snapshot(connection, recipientKey) }
        }

    override suspend fun acquireLease(request: BackendRecipientTargetLeaseRequest): BackendRecipientTargetLease? =
        synchronized(backendNotificationRuntimeLock) {
            openNotificationRuntimeConnection(databasePath).use { connection ->
                if (request.holderId.isBlank() || request.newLeaseVersion <= request.expectedLeaseVersion ||
                    request.fencingToken <= 0L) return@synchronized null
                connection.createStatement().use { it.execute("BEGIN IMMEDIATE") }
                try {
                    ensureRecipientRuntime(connection, request.recipientKey)
                    val before = snapshot(connection, request.recipientKey)
                        ?: error("Recipient target does not exist")
                    val updated = if (before.pendingCheckpoint == null) {
                        acquireFreshTargetLease(connection, request)
                    } else {
                        recoverTargetCheckpointLease(connection, request, before)
                    }
                    if (updated) connection.createStatement().use { it.execute("COMMIT") }
                    else connection.createStatement().use { it.execute("ROLLBACK") }
                    if (!updated) null else BackendRecipientTargetLease(
                        request.holderId,
                        request.newLeaseVersion,
                        request.fencingToken,
                        request.expiresAtLogicalEpochSeconds
                    )
                } catch (failure: Throwable) {
                    runCatching { connection.createStatement().use { it.execute("ROLLBACK") } }
                    throw failure
                }
            }
        }

    private fun acquireFreshTargetLease(
        connection: Connection,
        request: BackendRecipientTargetLeaseRequest
    ): Boolean = connection.prepareStatement(
        """
        UPDATE notification_recipient_runtime
        SET lease_holder = ?, lease_version = ?, lease_fence = ?, lease_expires_at = ?
        WHERE recipient_key = ? AND state = ? AND checkpoint_revision = ?
          AND lease_version = ? AND logical_now < ?
          AND ? > lease_version AND ? > lease_fence AND ? > logical_now
          AND (lease_holder IS NULL OR lease_expires_at <= logical_now)
          AND (next_attempt_at IS NULL OR next_attempt_at <= logical_now)
          AND checkpoint_effect_id IS NULL
        """.trimIndent()
    ).use { statement ->
        statement.setString(1, request.holderId)
        statement.setLong(2, request.newLeaseVersion)
        statement.setLong(3, request.fencingToken)
        statement.setLong(4, request.expiresAtLogicalEpochSeconds)
        statement.setString(5, request.recipientKey.value)
        statement.setString(6, BackendRecipientTargetState.PENDING_TARGET.name)
        statement.setLong(7, request.expectedCheckpointRevision)
        statement.setLong(8, request.expectedLeaseVersion)
        statement.setLong(9, recipientExpiry(connection, request.recipientKey))
        statement.setLong(10, request.newLeaseVersion)
        statement.setLong(11, request.fencingToken)
        statement.setLong(12, request.expiresAtLogicalEpochSeconds)
        statement.executeUpdate() == 1
    }

    private fun recoverTargetCheckpointLease(
        connection: Connection,
        request: BackendRecipientTargetLeaseRequest,
        before: BackendRecipientTargetSnapshot
    ): Boolean {
        val checkpoint = before.pendingCheckpoint ?: return false
        val checkpointHolder = checkpoint.holderId ?: return false
        val checkpointLeaseVersion = checkpoint.leaseVersion ?: return false
        val checkpointFence = checkpoint.fencingToken ?: return false
        if (request.expectedCheckpointRevision != before.checkpointRevision ||
            request.expectedLeaseVersion != before.lastLeaseVersion ||
            request.newLeaseVersion <= before.lastLeaseVersion ||
            request.fencingToken <= before.lastFencingToken ||
            request.expiresAtLogicalEpochSeconds <= before.nowEpochSeconds ||
            before.nowEpochSeconds >= recipientExpiry(connection, request.recipientKey)
        ) return false
        val newRevision = before.checkpointRevision + 1
        val newEffectId = "target-effect:${digest(
            "${request.recipientKey.value}:$newRevision:${checkpoint.kind.name}:f${request.fencingToken}"
        )}"
        connection.prepareStatement(
            """
            INSERT OR IGNORE INTO notification_recipient_checkpoint_delivery(
                recipient_key, checkpoint_revision, delivery_key, device_registration_id
            )
            SELECT recipient_key, ?, delivery_key, device_registration_id
            FROM notification_recipient_checkpoint_delivery
            WHERE recipient_key = ? AND checkpoint_revision = ?
            """.trimIndent()
        ).use { statement ->
            statement.setLong(1, newRevision)
            statement.setString(2, request.recipientKey.value)
            statement.setLong(3, before.checkpointRevision)
            statement.executeUpdate()
        }
        return connection.prepareStatement(
            """
            UPDATE notification_recipient_runtime
            SET lease_holder = ?, lease_version = ?, lease_fence = ?, lease_expires_at = ?,
                checkpoint_revision = ?, checkpoint_effect_id = ?, checkpoint_holder = ?,
                checkpoint_lease_version = ?, checkpoint_fence = ?, checkpoint_effect_requested = 0
            WHERE recipient_key = ? AND state = ?
              AND checkpoint_effect_id = ? AND checkpoint_revision = ?
              AND checkpoint_receipt_id = ? AND checkpoint_holder = ?
              AND checkpoint_lease_version = ? AND checkpoint_fence = ?
              AND lease_holder = ? AND lease_version = ? AND lease_fence = ?
              AND lease_expires_at <= logical_now AND logical_now < ?
              AND ? > lease_version AND ? > lease_fence AND ? > logical_now
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, request.holderId)
            statement.setLong(2, request.newLeaseVersion)
            statement.setLong(3, request.fencingToken)
            statement.setLong(4, request.expiresAtLogicalEpochSeconds)
            statement.setLong(5, newRevision)
            statement.setString(6, newEffectId)
            statement.setString(7, request.holderId)
            statement.setLong(8, request.newLeaseVersion)
            statement.setLong(9, request.fencingToken)
            statement.setString(10, request.recipientKey.value)
            statement.setString(11, BackendRecipientTargetState.PENDING_TARGET.name)
            statement.setString(12, checkpoint.effectId)
            statement.setLong(13, before.checkpointRevision)
            statement.setString(14, checkpoint.transactionReceiptId)
            statement.setString(15, checkpointHolder)
            statement.setLong(16, checkpointLeaseVersion)
            statement.setLong(17, checkpointFence)
            statement.setString(18, checkpointHolder)
            statement.setLong(19, before.lastLeaseVersion)
            statement.setLong(20, before.lastFencingToken)
            statement.setLong(21, recipientExpiry(connection, request.recipientKey))
            statement.setLong(22, request.newLeaseVersion)
            statement.setLong(23, request.fencingToken)
            statement.setLong(24, request.expiresAtLogicalEpochSeconds)
            statement.executeUpdate() == 1
        }
    }

    override suspend fun stageResolution(request: BackendRecipientTargetResolutionRequest): BackendRecipientTargetSnapshot =
        synchronized(backendNotificationRuntimeLock) {
            openNotificationRuntimeConnection(databasePath).use { connection ->
                ensureRecipientRuntime(connection, request.recipientKey)
                connection.autoCommit = false
                try {
                    val before = snapshot(connection, request.recipientKey)
                        ?: error("Recipient target does not exist")
                    if (before.state != BackendRecipientTargetState.PENDING_TARGET || before.pendingCheckpoint != null) {
                        connection.rollback()
                        return@synchronized before
                    }
                    val leaseMatches = connection.prepareStatement(
                        """
                        SELECT 1 FROM notification_recipient_runtime
                        WHERE recipient_key = ? AND lease_holder = ? AND lease_version = ? AND lease_fence = ?
                          AND lease_expires_at > logical_now
                        """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, request.recipientKey.value)
                        statement.setString(2, request.holderId)
                        statement.setLong(3, request.leaseVersion)
                        statement.setLong(4, request.fencingToken)
                        statement.executeQuery().use { it.next() }
                    }
                    if (!leaseMatches) {
                        connection.rollback()
                        return@synchronized before
                    }

                    request.registrationIds.forEach { registrationId ->
                        require(registrationId.isNotEmpty() && registrationId.trim() == registrationId) {
                            "registrationId must be a canonical identity component"
                        }
                    }
                    val registrations = request.registrationIds.distinct().sorted()
                    val nextAttempt = before.attempt + 1
                    val expiry = recipientExpiry(connection, request.recipientKey)
                    val retryAt = if (registrations.isEmpty()) {
                        safeAdd(before.nowEpochSeconds, targetRetryDelay(request.jitterSample, nextAttempt))
                    } else null
                    val kind = when {
                        registrations.isNotEmpty() -> BackendRecipientTargetCheckpointKind.FANOUT
                        nextAttempt >= TARGET_MAX_ATTEMPTS -> BackendRecipientTargetCheckpointKind.EXHAUSTION
                        retryAt == null || retryAt >= expiry -> BackendRecipientTargetCheckpointKind.EXPIRY
                        else -> BackendRecipientTargetCheckpointKind.RETRY
                    }
                    val revision = before.checkpointRevision + 1
                    val checkpointHolder = request.holderId.takeUnless {
                        kind == BackendRecipientTargetCheckpointKind.EXPIRY
                    }
                    val checkpointLeaseVersion = request.leaseVersion.takeUnless {
                        kind == BackendRecipientTargetCheckpointKind.EXPIRY
                    }
                    val checkpointFence = request.fencingToken.takeUnless {
                        kind == BackendRecipientTargetCheckpointKind.EXPIRY
                    }
                    insertTargetCheckpoint(
                        connection, request.recipientKey, kind, revision, checkpointHolder,
                        checkpointLeaseVersion, checkpointFence,
                        retryAt.takeIf { kind == BackendRecipientTargetCheckpointKind.RETRY }
                    )
                    if (registrations.isNotEmpty()) {
                        connection.prepareStatement(
                            """
                            INSERT INTO notification_recipient_checkpoint_delivery(
                                recipient_key, checkpoint_revision, delivery_key, device_registration_id
                            ) VALUES (?, ?, ?, ?)
                            """.trimIndent()
                        ).use { statement ->
                            registrations.forEach { registrationId ->
                                statement.setString(1, request.recipientKey.value)
                                statement.setLong(2, revision)
                                statement.setString(
                                    3,
                                    BackendCanonicalNotificationIdentity.deliveryKey(
                                        request.recipientKey, registrationId, "apns"
                                    ).value
                                )
                                statement.setString(4, registrationId)
                                statement.addBatch()
                            }
                            statement.executeBatch()
                        }
                    }
                    connection.commit()
                    snapshot(connection, request.recipientKey) ?: error("Recipient target disappeared")
                } catch (failure: Throwable) {
                    runCatching { connection.rollback() }.onFailure(failure::addSuppressed)
                    throw failure
                } finally {
                    connection.autoCommit = true
                }
            }
        }

    override suspend fun stageExpiry(recipientKey: RecipientKey): BackendRecipientTargetSnapshot =
        synchronized(backendNotificationRuntimeLock) {
            openNotificationRuntimeConnection(databasePath).use { connection ->
                ensureRecipientRuntime(connection, recipientKey)
                val before = snapshot(connection, recipientKey) ?: error("Recipient target does not exist")
                if (before.state != BackendRecipientTargetState.PENDING_TARGET ||
                    before.pendingCheckpoint != null || before.nowEpochSeconds < recipientExpiry(connection, recipientKey)) {
                    return@synchronized before
                }
                insertTargetCheckpoint(
                    connection, recipientKey, BackendRecipientTargetCheckpointKind.EXPIRY,
                    before.checkpointRevision + 1, null, null, null, null
                )
                snapshot(connection, recipientKey) ?: error("Recipient target disappeared")
            }
        }

    override suspend fun requestCheckpoint(reference: BackendRecipientTargetCheckpointReference): Boolean =
        synchronized(backendNotificationRuntimeLock) {
            openNotificationRuntimeConnection(databasePath).use { connection ->
                connection.prepareStatement(
                    """
                    UPDATE notification_recipient_runtime SET checkpoint_effect_requested = 1
                    WHERE recipient_key = ? AND checkpoint_effect_id = ? AND checkpoint_revision = ?
                      AND checkpoint_receipt_id = ? AND checkpoint_holder IS ?
                      AND checkpoint_lease_version IS ? AND checkpoint_fence IS ?
                      AND (
                        (checkpoint_holder IS NULL AND checkpoint_lease_version IS NULL AND checkpoint_fence IS NULL)
                        OR (
                          lease_holder = checkpoint_holder
                          AND lease_version = checkpoint_lease_version
                          AND lease_fence = checkpoint_fence
                          AND lease_expires_at > logical_now
                        )
                      )
                    """.trimIndent()
                ).use { statement ->
                    bindTargetReference(statement, reference)
                    statement.executeUpdate() == 1
                }
            }
        }

    override suspend fun acknowledgeCheckpoint(reference: BackendRecipientTargetCheckpointReference): BackendRecipientTargetSnapshot =
        acknowledgeCheckpointCas(reference).snapshot

    override suspend fun acknowledgeCheckpointCas(
        reference: BackendRecipientTargetCheckpointReference
    ): BackendRecipientTargetCheckpointCasResult =
        synchronized(backendNotificationRuntimeLock) {
            openNotificationRuntimeConnection(databasePath).use { connection ->
                connection.createStatement().use { it.execute("BEGIN IMMEDIATE") }
                try {
                    val before = snapshot(connection, reference.recipientKey)
                        ?: error("Recipient target does not exist")
                    val checkpoint = before.pendingCheckpoint
                    if (checkpoint == null || !checkpoint.effectRequested || !checkpoint.matches(reference)) {
                        connection.createStatement().use { it.execute("ROLLBACK") }
                        return@synchronized BackendRecipientTargetCheckpointCasResult(before, false, false)
                    }
                    if (!targetCheckpointAuthorityIsLiveOrHolderless(connection, reference)) {
                        connection.createStatement().use { it.execute("ROLLBACK") }
                        return@synchronized BackendRecipientTargetCheckpointCasResult(before, false, false)
                    }
                    if (checkpoint.kind == BackendRecipientTargetCheckpointKind.FANOUT) {
                        faultInjector.inject(BackendRecipientTargetFaultCheckpoint.BEFORE_FANOUT_COMMIT)
                    }
                    if (!applyTargetCheckpointCas(connection, reference, checkpoint)) {
                        connection.createStatement().use { it.execute("ROLLBACK") }
                        val current = snapshot(connection, reference.recipientKey)
                            ?: error("Recipient target does not exist")
                        return@synchronized BackendRecipientTargetCheckpointCasResult(current, false, false)
                    }
                    if (checkpoint.kind == BackendRecipientTargetCheckpointKind.FANOUT) {
                        faultInjector.inject(BackendRecipientTargetFaultCheckpoint.AFTER_RECIPIENT_CAS)
                    }
                    when (checkpoint.kind) {
                        BackendRecipientTargetCheckpointKind.EXPIRY -> {
                            connection.prepareStatement(
                                "UPDATE notification_recipient SET status = ? WHERE recipient_key = ?"
                            ).use { statement ->
                                statement.setString(1, BackendRecipientStatus.EXPIRED.name)
                                statement.setString(2, reference.recipientKey.value)
                                statement.executeUpdate()
                            }
                        }

                        BackendRecipientTargetCheckpointKind.FANOUT -> commitFanout(connection, reference, checkpoint)
                        BackendRecipientTargetCheckpointKind.EXHAUSTION -> connection.prepareStatement(
                            """
                            UPDATE notification_recipient
                            SET status = ?, terminal_reason = ?, terminal_acknowledged_at_epoch_seconds = ?
                            WHERE recipient_key = ?
                            """.trimIndent()
                        ).use { statement ->
                            statement.setString(1, BackendRecipientStatus.EXPIRED.name)
                            statement.setString(2, BackendRecipientTerminalReason.RETRY_EXHAUSTED.name)
                            statement.setLong(3, before.nowEpochSeconds)
                            statement.setString(4, reference.recipientKey.value)
                            statement.executeUpdate()
                        }
                        BackendRecipientTargetCheckpointKind.RETRY -> Unit
                    }
                    if (checkpoint.kind != BackendRecipientTargetCheckpointKind.RETRY) {
                        recomputeEffectDispatchStatusForRecipient(connection, reference.recipientKey)
                    }
                    connection.createStatement().use { it.execute("COMMIT") }
                    val committed = snapshot(connection, reference.recipientKey)
                        ?: error("Recipient target disappeared")
                    if (checkpoint.kind == BackendRecipientTargetCheckpointKind.FANOUT) {
                        faultInjector.inject(BackendRecipientTargetFaultCheckpoint.AFTER_FANOUT_COMMIT)
                    }
                    BackendRecipientTargetCheckpointCasResult(committed, true, true)
                } catch (failure: Throwable) {
                    runCatching { connection.createStatement().use { it.execute("ROLLBACK") } }
                        .onFailure(failure::addSuppressed)
                    throw failure
                }
            }
        }

    override suspend fun advanceLogicalClock(
        recipientKey: RecipientKey,
        expectedClockRevision: Long,
        newEpochSeconds: Long
    ): BackendRecipientTargetSnapshot = synchronized(backendNotificationRuntimeLock) {
        openNotificationRuntimeConnection(databasePath).use { connection ->
            ensureRecipientRuntime(connection, recipientKey)
            connection.prepareStatement(
                """
                UPDATE notification_recipient_runtime
                SET logical_now = ?, clock_revision = clock_revision + 1
                WHERE recipient_key = ? AND clock_revision = ? AND logical_now <= ?
                """.trimIndent()
            ).use { statement ->
                statement.setLong(1, newEpochSeconds)
                statement.setString(2, recipientKey.value)
                statement.setLong(3, expectedClockRevision)
                statement.setLong(4, newEpochSeconds)
                statement.executeUpdate()
            }
            snapshot(connection, recipientKey) ?: error("Recipient target does not exist")
        }
    }

    override fun close() = Unit

    private fun ensureRecipientRuntime(connection: Connection, recipientKey: RecipientKey) {
        connection.prepareStatement(
            "INSERT OR IGNORE INTO notification_recipient_runtime(recipient_key) VALUES (?)"
        ).use { statement -> statement.setString(1, recipientKey.value); statement.executeUpdate() }
    }

    private fun recipientExpiry(connection: Connection, recipientKey: RecipientKey): Long = connection.prepareStatement(
        "SELECT expires_at_epoch_seconds FROM notification_recipient WHERE recipient_key = ?"
    ).use { statement ->
        statement.setString(1, recipientKey.value)
        statement.executeQuery().use { rows -> if (rows.next()) rows.getLong(1) else error("Recipient target does not exist") }
    }

    private fun insertTargetCheckpoint(
        connection: Connection,
        recipientKey: RecipientKey,
        kind: BackendRecipientTargetCheckpointKind,
        revision: Long,
        holderId: String?,
        leaseVersion: Long?,
        fencingToken: Long?,
        nextAt: Long?
    ) {
        val identity = "${recipientKey.value}:$revision:${kind.name}"
        connection.prepareStatement(
            """
            UPDATE notification_recipient_runtime
            SET checkpoint_revision = ?, checkpoint_kind = ?, checkpoint_effect_id = ?,
                checkpoint_receipt_id = ?, checkpoint_holder = ?, checkpoint_lease_version = ?,
                checkpoint_fence = ?, checkpoint_next_attempt_at = ?, checkpoint_effect_requested = 0
            WHERE recipient_key = ? AND checkpoint_effect_id IS NULL
            """.trimIndent()
        ).use { statement ->
            statement.setLong(1, revision)
            statement.setString(2, kind.name)
            statement.setString(3, "target-effect:${digest(identity)}")
            statement.setString(4, "target-receipt:${digest("receipt:$identity")}")
            statement.setString(5, holderId)
            statement.setObject(6, leaseVersion)
            statement.setObject(7, fencingToken)
            statement.setNullableLong(8, nextAt)
            statement.setString(9, recipientKey.value)
            check(statement.executeUpdate() == 1) { "Recipient target checkpoint CAS failed" }
        }
    }

    private fun commitFanout(
        connection: Connection,
        reference: BackendRecipientTargetCheckpointReference,
        checkpoint: BackendRecipientTargetCheckpoint
    ) {
        checkpoint.deliveries.forEach { planned ->
            connection.prepareStatement(
                "INSERT OR IGNORE INTO notification_recipient_registration(recipient_key, device_registration_id) VALUES (?, ?)"
            ).use { statement ->
                statement.setString(1, reference.recipientKey.value)
                statement.setString(2, planned.registrationId)
                statement.executeUpdate()
            }
            connection.prepareStatement(
                """
                INSERT OR IGNORE INTO notification_delivery(
                    delivery_key, logical_notification_id, idempotency_key, recipient_key,
                    device_registration_id, provider, status, attempt, expires_at_epoch_seconds,
                    identity_version
                ) SELECT ?, COALESCE(logical.logical_notification_id, recipient.effect_key), ?,
                         recipient.recipient_key, ?, 'apns', ?, 0,
                         recipient.expires_at_epoch_seconds, recipient.identity_version
                  FROM notification_recipient recipient
                  LEFT JOIN notification_logical logical ON logical.effect_key = recipient.effect_key
                 WHERE recipient.recipient_key = ?
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, planned.deliveryKey.value)
                statement.setString(2, planned.deliveryKey.value)
                statement.setString(3, planned.registrationId)
                statement.setString(4, BackendDeliveryStatus.QUEUED.name)
                statement.setString(5, reference.recipientKey.value)
                statement.executeUpdate()
            }
            connection.prepareStatement(
                "INSERT OR IGNORE INTO notification_delivery_authority(delivery_key, authority, fencing_token) VALUES (?, ?, 1)"
            ).use { statement ->
                statement.setString(1, planned.deliveryKey.value)
                statement.setString(2, BackendDeliveryAuthority.OUTBOX_V2.wireValue)
                statement.executeUpdate()
            }
        }
        faultInjector.inject(BackendRecipientTargetFaultCheckpoint.AFTER_DELIVERY_INSERTS)
        connection.prepareStatement(
            "UPDATE notification_recipient SET status = ? WHERE recipient_key = ?"
        ).use { statement ->
            statement.setString(1, BackendRecipientStatus.TARGETED.name)
            statement.setString(2, reference.recipientKey.value)
            statement.executeUpdate()
        }
        recomputeEffectDispatchStatusForRecipient(connection, reference.recipientKey)
        connection.prepareStatement(
            """
            INSERT INTO notification_recipient_fanout_receipt(
                transaction_receipt_id, recipient_key, checkpoint_revision, effect_id, fencing_token
            ) VALUES (?, ?, ?, ?, ?)
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, reference.transactionReceiptId)
            statement.setString(2, reference.recipientKey.value)
            statement.setLong(3, reference.checkpointRevision)
            statement.setString(4, reference.effectId)
            statement.setLong(5, reference.fencingToken)
            statement.executeUpdate()
        }
        faultInjector.inject(BackendRecipientTargetFaultCheckpoint.AFTER_RECEIPT_INSERT)
    }

    private fun applyTargetCheckpointCas(
        connection: Connection,
        reference: BackendRecipientTargetCheckpointReference,
        checkpoint: BackendRecipientTargetCheckpoint
    ): Boolean {
        val terminalState = when (checkpoint.kind) {
            BackendRecipientTargetCheckpointKind.FANOUT -> BackendRecipientTargetState.TARGETED
            BackendRecipientTargetCheckpointKind.EXPIRY -> BackendRecipientTargetState.TARGET_EXPIRED
            BackendRecipientTargetCheckpointKind.EXHAUSTION -> BackendRecipientTargetState.TARGET_EXHAUSTED
            BackendRecipientTargetCheckpointKind.RETRY -> BackendRecipientTargetState.PENDING_TARGET
        }
        return connection.prepareStatement(
            """
            UPDATE notification_recipient_runtime
            SET state = ?, attempt = attempt + ?, next_attempt_at = ?,
                lease_holder = NULL, lease_expires_at = NULL,
                checkpoint_kind = NULL, checkpoint_effect_id = NULL,
                checkpoint_receipt_id = NULL, checkpoint_holder = NULL,
                checkpoint_lease_version = NULL, checkpoint_fence = NULL,
                checkpoint_next_attempt_at = NULL, checkpoint_effect_requested = 0
            WHERE recipient_key = ? AND state = ?
              AND checkpoint_effect_id = ? AND checkpoint_revision = ?
              AND checkpoint_receipt_id = ? AND checkpoint_holder IS ?
              AND checkpoint_lease_version IS ? AND checkpoint_fence IS ?
              AND checkpoint_effect_requested = 1
              AND (
                (checkpoint_holder IS NULL AND checkpoint_lease_version IS NULL AND checkpoint_fence IS NULL)
                OR (
                  lease_holder = checkpoint_holder
                  AND lease_version = checkpoint_lease_version
                  AND lease_fence = checkpoint_fence
                  AND lease_expires_at > logical_now
                )
              )
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, terminalState.name)
            statement.setInt(
                2,
                if (checkpoint.kind == BackendRecipientTargetCheckpointKind.RETRY ||
                    checkpoint.kind == BackendRecipientTargetCheckpointKind.EXHAUSTION) 1 else 0
            )
            statement.setNullableLong(
                3,
                checkpoint.nextAttemptAtEpochSeconds.takeIf {
                    checkpoint.kind == BackendRecipientTargetCheckpointKind.RETRY
                }
            )
            statement.setString(4, reference.recipientKey.value)
            statement.setString(5, BackendRecipientTargetState.PENDING_TARGET.name)
            statement.setString(6, reference.effectId)
            statement.setLong(7, reference.checkpointRevision)
            statement.setString(8, reference.transactionReceiptId)
            statement.setString(9, reference.holderId.takeUnless { reference.isHolderless })
            statement.setObject(10, reference.leaseVersion.takeUnless { reference.isHolderless })
            statement.setObject(11, reference.fencingToken.takeUnless { reference.isHolderless })
            statement.executeUpdate() == 1
        }
    }

    private fun finishTargetCheckpoint(
        connection: Connection,
        recipientKey: RecipientKey,
        state: BackendRecipientTargetState
    ) {
        connection.prepareStatement(
            """
            UPDATE notification_recipient_runtime
            SET state = ?, next_attempt_at = NULL, lease_holder = NULL, lease_expires_at = NULL,
                checkpoint_kind = NULL, checkpoint_effect_id = NULL, checkpoint_receipt_id = NULL,
                checkpoint_holder = NULL, checkpoint_lease_version = NULL, checkpoint_fence = NULL,
                checkpoint_next_attempt_at = NULL, checkpoint_effect_requested = 0
            WHERE recipient_key = ?
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, state.name)
            statement.setString(2, recipientKey.value)
            statement.executeUpdate()
        }
    }

    private fun snapshot(connection: Connection, recipientKey: RecipientKey): BackendRecipientTargetSnapshot? {
        val baseExists = connection.prepareStatement(
            "SELECT 1 FROM notification_recipient WHERE recipient_key = ?"
        ).use { statement ->
            statement.setString(1, recipientKey.value)
            statement.executeQuery().use { it.next() }
        }
        if (!baseExists) return null
        ensureRecipientRuntime(connection, recipientKey)
        return connection.prepareStatement(
            "SELECT * FROM notification_recipient_runtime WHERE recipient_key = ?"
        ).use { statement ->
            statement.setString(1, recipientKey.value)
            statement.executeQuery().use { rows ->
                check(rows.next())
                val revision = rows.getLong("checkpoint_revision")
                val checkpointKind = rows.getString("checkpoint_kind")?.let(BackendRecipientTargetCheckpointKind::valueOf)
                val checkpoint = checkpointKind?.let { kind ->
                    BackendRecipientTargetCheckpoint(
                        kind = kind,
                        effectId = rows.getString("checkpoint_effect_id"),
                        revision = revision,
                        transactionReceiptId = rows.getString("checkpoint_receipt_id"),
                        holderId = rows.getString("checkpoint_holder"),
                        leaseVersion = rows.getNullableLong("checkpoint_lease_version"),
                        fencingToken = rows.getNullableLong("checkpoint_fence"),
                        deliveries = plannedDeliveries(connection, recipientKey, revision),
                        nextAttemptAtEpochSeconds = rows.getNullableLong("checkpoint_next_attempt_at"),
                        effectRequested = rows.getInt("checkpoint_effect_requested") == 1
                    )
                }
                BackendRecipientTargetSnapshot(
                    recipientKey = recipientKey,
                    state = BackendRecipientTargetState.valueOf(rows.getString("state")),
                    attempt = rows.getLong("attempt"),
                    nextAttemptAtEpochSeconds = rows.getNullableLong("next_attempt_at"),
                    nowEpochSeconds = rows.getLong("logical_now"),
                    clockRevision = rows.getLong("clock_revision"),
                    checkpointRevision = revision,
                    lastLeaseVersion = rows.getLong("lease_version"),
                    lastFencingToken = rows.getLong("lease_fence"),
                    pendingCheckpoint = checkpoint,
                    deliveryKeys = deliveryKeys(connection, recipientKey)
                )
            }
        }
    }

    private fun plannedDeliveries(connection: Connection, key: RecipientKey, revision: Long) = connection.prepareStatement(
        "SELECT delivery_key, device_registration_id FROM notification_recipient_checkpoint_delivery WHERE recipient_key = ? AND checkpoint_revision = ? ORDER BY device_registration_id"
    ).use { statement ->
        statement.setString(1, key.value); statement.setLong(2, revision)
        statement.executeQuery().use { rows ->
            buildList { while (rows.next()) add(BackendRecipientTargetPlannedDelivery(DeliveryKey(rows.getString(1)), rows.getString(2))) }
        }
    }

    private fun deliveryKeys(connection: Connection, key: RecipientKey) = connection.prepareStatement(
        "SELECT delivery_key FROM notification_delivery WHERE recipient_key = ? ORDER BY delivery_key"
    ).use { statement ->
        statement.setString(1, key.value)
        statement.executeQuery().use { rows -> buildSet { while (rows.next()) add(DeliveryKey(rows.getString(1))) } }
    }

    private fun BackendRecipientTargetCheckpoint.matches(reference: BackendRecipientTargetCheckpointReference): Boolean =
        effectId == reference.effectId && revision == reference.checkpointRevision &&
            transactionReceiptId == reference.transactionReceiptId &&
            if (reference.isHolderless) {
                holderId == null && leaseVersion == null && fencingToken == null
            } else {
                holderId == reference.holderId && leaseVersion == reference.leaseVersion &&
                    fencingToken == reference.fencingToken
            }

    private fun bindTargetReference(statement: java.sql.PreparedStatement, reference: BackendRecipientTargetCheckpointReference) {
        statement.setString(1, reference.recipientKey.value)
        statement.setString(2, reference.effectId)
        statement.setLong(3, reference.checkpointRevision)
        statement.setString(4, reference.transactionReceiptId)
        statement.setString(5, reference.holderId.takeUnless { reference.isHolderless })
        statement.setObject(6, reference.leaseVersion.takeUnless { reference.isHolderless })
        statement.setObject(7, reference.fencingToken.takeUnless { reference.isHolderless })
    }

    private fun targetCheckpointAuthorityIsLiveOrHolderless(
        connection: Connection,
        reference: BackendRecipientTargetCheckpointReference
    ): Boolean {
        if (reference.isHolderless) return true
        return connection.prepareStatement(
            """
            SELECT 1 FROM notification_recipient_runtime
            WHERE recipient_key = ? AND lease_holder = ? AND lease_version = ? AND lease_fence = ?
              AND lease_expires_at > logical_now
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, reference.recipientKey.value)
            statement.setString(2, reference.holderId)
            statement.setLong(3, reference.leaseVersion)
            statement.setLong(4, reference.fencingToken)
            statement.executeQuery().use { it.next() }
        }
    }
}

internal fun targetDeliveryKey(recipientKey: RecipientKey, registrationId: String): DeliveryKey =
    BackendCanonicalNotificationIdentity.deliveryKey(recipientKey, registrationId, "apns")

private const val TARGET_MAX_ATTEMPTS = 6L

private fun targetRetryDelay(rawSample: Double, nextAttempt: Long): Long {
    val sample = if (rawSample.isFinite()) rawSample.coerceIn(0.0, 1.0) else 0.0
    val exponent = (nextAttempt - 1).coerceIn(0, 62).toInt()
    val cap = if (exponent >= 9) 300L else 1L shl exponent
    return kotlin.math.floor(sample * cap).toLong().coerceIn(1L, 300L)
}

internal class SqliteBackendDeliveryRuntime(
    private val databasePath: Path,
    private val faultInjector: BackendDeliveryWorkerFaultInjector
) : BackendDeliveryRuntime {
    init {
        synchronized(backendNotificationRuntimeLock) {
            openNotificationRuntimeConnection(databasePath).use(::ensureBackendNotificationRuntimeSchema)
        }
    }

    override suspend fun current(deliveryKey: DeliveryKey): BackendNotificationDeliverySnapshot? =
        synchronized(backendNotificationRuntimeLock) {
            openNotificationRuntimeConnection(databasePath).use { snapshot(it, deliveryKey) }
        }

    override suspend fun dueDeliveryKeys(authority: BackendDeliveryAuthority): List<DeliveryKey> =
        synchronized(backendNotificationRuntimeLock) {
            openNotificationRuntimeConnection(databasePath).use { connection ->
                val ordinaryDue = listOf(
                    BackendDeliveryStatus.POLICY_CHECK,
                    BackendDeliveryStatus.QUEUED,
                    BackendDeliveryStatus.RETRY,
                    BackendDeliveryStatus.RETRY_SCHEDULED,
                    BackendDeliveryStatus.DEFERRED_QUIET_HOURS,
                    BackendDeliveryStatus.AWAITING_TOKEN,
                    BackendDeliveryStatus.AUTH
                ).joinToString(",") { "'${it.name}'" }
                val expiryDue = listOf(
                    BackendDeliveryStatus.POLICY_CHECK,
                    BackendDeliveryStatus.DEFERRED_QUIET_HOURS,
                    BackendDeliveryStatus.AWAITING_TOKEN,
                    BackendDeliveryStatus.QUEUED,
                    BackendDeliveryStatus.AUTH,
                    BackendDeliveryStatus.SENDING,
                    BackendDeliveryStatus.RETRY_SCHEDULED,
                    BackendDeliveryStatus.UNKNOWN_OUTCOME,
                    BackendDeliveryStatus.PROVIDER_AUTH_BLOCKED
                ).joinToString(",") { "'${it.name}'" }
                connection.prepareStatement(
                    """
                    SELECT delivery.delivery_key
                    FROM notification_delivery delivery
                    JOIN notification_delivery_authority authority
                      ON authority.delivery_key = delivery.delivery_key
                    LEFT JOIN notification_delivery_provider_checkpoint checkpoint
                      ON checkpoint.delivery_key = delivery.delivery_key
                    WHERE authority.authority = ?
                      AND (
                        checkpoint.delivery_key IS NOT NULL
                        OR (
                          delivery.expires_at_epoch_seconds > delivery.logical_now
                          AND (delivery.next_attempt_at_epoch_seconds IS NULL
                               OR delivery.next_attempt_at_epoch_seconds <= delivery.logical_now)
                          AND delivery.status IN ($ordinaryDue)
                        )
                        OR (
                          delivery.expires_at_epoch_seconds <= delivery.logical_now
                          AND delivery.status IN ($expiryDue)
                        )
                      )
                    ORDER BY delivery.delivery_key
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, authority.wireValue)
                    statement.executeQuery().use { rows ->
                        buildList { while (rows.next()) add(DeliveryKey(rows.getString(1))) }
                    }
                }
            }
        }

    override suspend fun acquireLease(deliveryKey: DeliveryKey, holderId: String): BackendDeliveryLease? =
        synchronized(backendNotificationRuntimeLock) {
            openNotificationRuntimeConnection(databasePath).use { connection ->
                if (holderId.isBlank()) return@synchronized null
                connection.createStatement().use { it.execute("BEGIN IMMEDIATE") }
                try {
                    val before = snapshot(connection, deliveryKey)
                    if (before == null || before.nowEpochSeconds >= before.expiresAtEpochSeconds) {
                        connection.createStatement().use { it.execute("ROLLBACK") }
                        return@synchronized null
                    }
                    val nextVersion = safeIncrement(before.lastLeaseVersion)
                    val nextFence = safeIncrement(before.lastLeaseFencingToken)
                    if (nextVersion <= before.lastLeaseVersion || nextFence <= before.lastLeaseFencingToken) {
                        connection.createStatement().use { it.execute("ROLLBACK") }
                        return@synchronized null
                    }
                    val leaseUntil = safeAdd(before.nowEpochSeconds, 30)
                    val updated = if (before.pendingCheckpoint == null) {
                        acquireFreshDeliveryLease(
                            connection, deliveryKey, holderId, before,
                            nextVersion, nextFence, leaseUntil
                        )
                    } else {
                        recoverProviderCheckpointLease(
                            connection, deliveryKey, holderId, before,
                            nextVersion, nextFence, leaseUntil
                        )
                    }
                    if (updated) connection.createStatement().use { it.execute("COMMIT") }
                    else connection.createStatement().use { it.execute("ROLLBACK") }
                    if (!updated) null else BackendDeliveryLease(holderId, nextVersion, nextFence, leaseUntil)
                } catch (failure: Throwable) {
                    runCatching { connection.createStatement().use { it.execute("ROLLBACK") } }
                    throw failure
                }
            }
        }

    private fun acquireFreshDeliveryLease(
        connection: Connection,
        deliveryKey: DeliveryKey,
        holderId: String,
        before: BackendNotificationDeliverySnapshot,
        nextVersion: Long,
        nextFence: Long,
        leaseUntil: Long
    ): Boolean {
        if (before.state !in claimableStates ||
            (before.nextAttemptAtEpochSeconds != null && before.nextAttemptAtEpochSeconds > before.nowEpochSeconds)
        ) return false
        val correlationId = "delivery-correlation:${UUID.randomUUID()}"
        return connection.prepareStatement(
                    """
                    UPDATE notification_delivery
                    SET status = ?, lease_owner = ?, lease_expires_at_epoch_seconds = ?,
                        lease_version = ?, lease_fence = ?, correlation_id = ?
                    WHERE delivery_key = ? AND checkpoint_revision = ? AND lease_version = ?
                      AND expires_at_epoch_seconds > logical_now
                      AND (lease_owner IS NULL OR lease_expires_at_epoch_seconds <= logical_now)
                      AND (next_attempt_at_epoch_seconds IS NULL OR next_attempt_at_epoch_seconds <= logical_now)
                      AND status IN (?, ?, ?, ?, ?)
                      AND NOT EXISTS (
                        SELECT 1 FROM notification_delivery_provider_checkpoint checkpoint
                        WHERE checkpoint.delivery_key = notification_delivery.delivery_key
                      )
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, BackendDeliveryStatus.AUTH.name)
                    statement.setString(2, holderId)
                    statement.setLong(3, leaseUntil)
                    statement.setLong(4, nextVersion)
                    statement.setLong(5, nextFence)
                    statement.setString(6, correlationId)
                    statement.setString(7, deliveryKey.value)
                    statement.setLong(8, before.checkpointRevision)
                    statement.setLong(9, before.lastLeaseVersion)
                    listOf(
                        BackendDeliveryStatus.QUEUED,
                        BackendDeliveryStatus.RETRY,
                        BackendDeliveryStatus.RETRY_SCHEDULED,
                        BackendDeliveryStatus.DEFERRED_QUIET_HOURS,
                        BackendDeliveryStatus.AWAITING_TOKEN
                    ).forEachIndexed { index, status -> statement.setString(index + 10, status.name) }
                    statement.executeUpdate() == 1
                }
    }

    private fun recoverProviderCheckpointLease(
        connection: Connection,
        deliveryKey: DeliveryKey,
        holderId: String,
        before: BackendNotificationDeliverySnapshot,
        nextVersion: Long,
        nextFence: Long,
        leaseUntil: Long
    ): Boolean {
        val checkpoint = before.pendingCheckpoint ?: return false
        val oldLease = before.lease ?: return false
        if (before.state != BackendDurableDeliveryState.AWAITING_PROVIDER_RESULT_PERSISTENCE ||
            oldLease.expiresAtLogicalEpochSeconds > before.nowEpochSeconds ||
            leaseUntil <= before.nowEpochSeconds
        ) return false
        val newRevision = safeIncrement(before.checkpointRevision)
        if (newRevision <= before.checkpointRevision) return false
        val newEffectId = "provider-effect:${digest(
            "${deliveryKey.value}:$newRevision:${checkpoint.outcome.name}:f$nextFence"
        )}"
        val checkpointUpdated = connection.prepareStatement(
            """
            UPDATE notification_delivery_provider_checkpoint
            SET effect_id = ?, checkpoint_revision = ?, lease_holder = ?,
                lease_version = ?, lease_fence = ?, effect_requested = 0
            WHERE delivery_key = ? AND effect_id = ? AND checkpoint_revision = ?
              AND lease_holder = ? AND lease_version = ? AND lease_fence = ?
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, newEffectId)
            statement.setLong(2, newRevision)
            statement.setString(3, holderId)
            statement.setLong(4, nextVersion)
            statement.setLong(5, nextFence)
            statement.setString(6, deliveryKey.value)
            statement.setString(7, checkpoint.effectId)
            statement.setLong(8, checkpoint.revision)
            statement.setString(9, checkpoint.leaseHolderId)
            statement.setLong(10, checkpoint.leaseVersion ?: return false)
            statement.setLong(11, checkpoint.leaseFencingToken ?: return false)
            statement.executeUpdate() == 1
        }
        if (!checkpointUpdated) return false
        return connection.prepareStatement(
            """
            UPDATE notification_delivery
            SET checkpoint_revision = ?, lease_owner = ?, lease_version = ?, lease_fence = ?,
                lease_expires_at_epoch_seconds = ?
            WHERE delivery_key = ? AND checkpoint_revision = ?
              AND lease_owner = ? AND lease_version = ? AND lease_fence = ?
              AND lease_expires_at_epoch_seconds <= logical_now
              AND logical_now < expires_at_epoch_seconds
              AND EXISTS (
                SELECT 1 FROM notification_delivery_authority authority
                WHERE authority.delivery_key = notification_delivery.delivery_key
                  AND authority.authority = ? AND authority.fencing_token = ?
              )
            """.trimIndent()
        ).use { statement ->
            statement.setLong(1, newRevision)
            statement.setString(2, holderId)
            statement.setLong(3, nextVersion)
            statement.setLong(4, nextFence)
            statement.setLong(5, leaseUntil)
            statement.setString(6, deliveryKey.value)
            statement.setLong(7, before.checkpointRevision)
            statement.setString(8, oldLease.holderId)
            statement.setLong(9, oldLease.version)
            statement.setLong(10, oldLease.fencingToken)
            statement.setString(11, BackendDeliveryAuthority.OUTBOX_V2.wireValue)
            statement.setLong(12, before.authorityFencingToken)
            statement.executeUpdate() == 1
        }
    }

    override suspend fun markLeaseLost(
        command: BackendDeliveryLeaseLostCommand
    ): BackendNotificationDeliverySnapshot = synchronized(backendNotificationRuntimeLock) {
        openNotificationRuntimeConnection(databasePath).use { connection ->
            connection.createStatement().use { it.execute("BEGIN IMMEDIATE") }
            try {
                val updated = connection.prepareStatement(
                    """
                    UPDATE notification_delivery
                    SET status = ?, lease_owner = NULL, lease_expires_at_epoch_seconds = NULL,
                        correlation_id = NULL
                    WHERE delivery_key = ? AND status = ? AND correlation_id = ? AND attempt = ?
                      AND lease_owner = ? AND lease_version = ? AND lease_fence = ?
                      AND lease_expires_at_epoch_seconds > logical_now
                      AND expires_at_epoch_seconds > logical_now
                      AND NOT EXISTS (
                        SELECT 1 FROM notification_delivery_provider_checkpoint checkpoint
                        WHERE checkpoint.delivery_key = notification_delivery.delivery_key
                      )
                      AND EXISTS (
                        SELECT 1 FROM notification_delivery_authority authority
                        WHERE authority.delivery_key = notification_delivery.delivery_key
                          AND authority.authority IN ('legacy', 'outbox-v2')
                      )
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, BackendDeliveryStatus.QUEUED.name)
                    statement.setString(2, command.deliveryKey.value)
                    statement.setString(3, BackendDeliveryStatus.AUTH.name)
                    statement.setString(4, command.correlationId)
                    statement.setLong(5, command.attempt)
                    statement.setString(6, command.leaseHolderId)
                    statement.setLong(7, command.leaseVersion)
                    statement.setLong(8, command.leaseFencingToken)
                    statement.executeUpdate()
                }
                if (updated == 1) recomputeEffectDispatchStatusForDelivery(connection, command.deliveryKey)
                connection.createStatement().use { it.execute("COMMIT") }
            } catch (failure: Throwable) {
                runCatching { connection.createStatement().use { it.execute("ROLLBACK") } }
                throw failure
            }
            snapshot(connection, command.deliveryKey) ?: error("Delivery does not exist")
        }
    }

    override suspend fun markQuietHours(
        deliveryKey: DeliveryKey,
        expectedCheckpointRevision: Long,
        authority: DeliveryAuthority?,
        authorityFencingToken: Long,
        nextEligibleAtEpochSeconds: Long
    ): BackendNotificationDeliverySnapshot = directLifecycleCas(
        deliveryKey,
        expectedCheckpointRevision,
        authority,
        authorityFencingToken,
        setOf(
            BackendDeliveryStatus.POLICY_CHECK,
            BackendDeliveryStatus.QUEUED,
            BackendDeliveryStatus.DEFERRED_QUIET_HOURS
        ),
        BackendDeliveryStatus.DEFERRED_QUIET_HOURS,
        nextEligibleAtEpochSeconds
    )

    override suspend fun markNoActiveToken(
        deliveryKey: DeliveryKey,
        expectedCheckpointRevision: Long,
        authority: DeliveryAuthority?,
        authorityFencingToken: Long
    ): BackendNotificationDeliverySnapshot = directLifecycleCas(
        deliveryKey,
        expectedCheckpointRevision,
        authority,
        authorityFencingToken,
        setOf(BackendDeliveryStatus.POLICY_CHECK, BackendDeliveryStatus.QUEUED, BackendDeliveryStatus.AWAITING_TOKEN),
        BackendDeliveryStatus.AWAITING_TOKEN,
        null
    )

    override suspend fun markPolicyAllowed(
        deliveryKey: DeliveryKey,
        expectedCheckpointRevision: Long,
        authority: DeliveryAuthority?,
        authorityFencingToken: Long
    ): BackendNotificationDeliverySnapshot = directLifecycleCas(
        deliveryKey,
        expectedCheckpointRevision,
        authority,
        authorityFencingToken,
        setOf(
            BackendDeliveryStatus.POLICY_CHECK,
            BackendDeliveryStatus.DEFERRED_QUIET_HOURS,
            BackendDeliveryStatus.AWAITING_TOKEN,
            BackendDeliveryStatus.QUEUED,
            BackendDeliveryStatus.RETRY,
            BackendDeliveryStatus.RETRY_SCHEDULED
        ),
        BackendDeliveryStatus.QUEUED,
        null
    )

    override suspend fun markProviderAuthReady(
        deliveryKey: DeliveryKey,
        correlationId: String,
        attempt: Long,
        leaseHolderId: String,
        leaseVersion: Long,
        leaseFencingToken: Long
    ): BackendNotificationDeliverySnapshot = synchronized(backendNotificationRuntimeLock) {
        openNotificationRuntimeConnection(databasePath).use { connection ->
            connection.prepareStatement(
                """
                UPDATE notification_delivery SET status = ?
                WHERE delivery_key = ? AND status = ? AND correlation_id = ? AND attempt = ?
                  AND lease_owner = ? AND lease_version = ? AND lease_fence = ?
                  AND lease_expires_at_epoch_seconds > logical_now
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, BackendDeliveryStatus.SENDING.name)
                statement.setString(2, deliveryKey.value)
                statement.setString(3, BackendDeliveryStatus.AUTH.name)
                statement.setString(4, correlationId)
                statement.setLong(5, attempt)
                statement.setString(6, leaseHolderId)
                statement.setLong(7, leaseVersion)
                statement.setLong(8, leaseFencingToken)
                statement.executeUpdate()
            }
            snapshot(connection, deliveryKey) ?: error("Delivery does not exist")
        }
    }

    override suspend fun suppressBeforeLease(
        deliveryKey: DeliveryKey,
        expectedCheckpointRevision: Long,
        authority: DeliveryAuthority?,
        authorityFencingToken: Long
    ): BackendNotificationDeliverySnapshot = directLifecycleCas(
        deliveryKey,
        expectedCheckpointRevision,
        authority,
        authorityFencingToken,
        preWriteStatuses,
        BackendDeliveryStatus.SUPPRESSED,
        null
    )

    override suspend fun cancelBeforeWrite(
        deliveryKey: DeliveryKey,
        expectedCheckpointRevision: Long,
        authority: DeliveryAuthority?,
        authorityFencingToken: Long
    ): BackendNotificationDeliverySnapshot = directLifecycleCas(
        deliveryKey,
        expectedCheckpointRevision,
        authority,
        authorityFencingToken,
        preWriteStatuses + BackendDeliveryStatus.AUTH,
        BackendDeliveryStatus.CANCELLED,
        null
    )

    override suspend fun stageSendingCancellation(
        deliveryKey: DeliveryKey,
        correlationId: String,
        attempt: Long,
        leaseHolderId: String,
        leaseVersion: Long,
        leaseFencingToken: Long
    ): BackendNotificationDeliverySnapshot = synchronized(backendNotificationRuntimeLock) {
        openNotificationRuntimeConnection(databasePath).use { connection ->
            val before = snapshot(connection, deliveryKey) ?: error("Delivery does not exist")
            val activeLease = before.lease
            if (before.state != BackendDurableDeliveryState.SENDING || before.correlationId != correlationId ||
                before.attempt != attempt || activeLease?.holderId != leaseHolderId ||
                activeLease.version != leaseVersion || activeLease.fencingToken != leaseFencingToken) {
                return@synchronized before
            }
            stageDeliveryCheckpoint(
                connection,
                before,
                BackendClassifiedProviderObservation(
                    BackendDurableProviderOutcome.UNKNOWN_OUTCOME,
                    BackendPersistedProviderReason.TRANSPORT_OUTCOME_UNKNOWN
                ),
                before.credentialVersion,
                null,
                activeLease
            )
        }
    }

    override suspend fun stageExpiry(
        deliveryKey: DeliveryKey,
        expectedCheckpointRevision: Long,
        authority: DeliveryAuthority?,
        authorityFencingToken: Long,
        leaseHolderId: String?,
        leaseVersion: Long?,
        leaseFencingToken: Long?
    ): BackendNotificationDeliverySnapshot = synchronized(backendNotificationRuntimeLock) {
        openNotificationRuntimeConnection(databasePath).use { connection ->
            val before = snapshot(connection, deliveryKey) ?: error("Delivery does not exist")
            val leaseMatches = before.lease?.let {
                it.holderId == leaseHolderId && it.version == leaseVersion && it.fencingToken == leaseFencingToken
            } ?: (leaseHolderId == null && leaseVersion == null && leaseFencingToken == null)
            if (before.pendingCheckpoint != null || before.checkpointRevision != expectedCheckpointRevision ||
                before.authority != authority || before.authorityFencingToken != authorityFencingToken ||
                before.nowEpochSeconds < before.expiresAtEpochSeconds || before.state !in expirableStates ||
                !leaseMatches) return@synchronized before
            stageDeliveryCheckpoint(
                connection,
                before,
                BackendClassifiedProviderObservation(
                    BackendDurableProviderOutcome.EXPIRED,
                    BackendPersistedProviderReason.RETRY_WOULD_REACH_EXPIRY
                ),
                before.credentialVersion,
                null,
                before.lease,
                before.lease?.takeIf { it.expiresAtLogicalEpochSeconds > before.nowEpochSeconds }
            )
        }
    }

    override suspend fun deferQuietHours(
        deliveryKey: DeliveryKey,
        lease: BackendDeliveryLease,
        untilEpochSeconds: Long
    ): Boolean = updateLeasedState(
        deliveryKey,
        lease,
        BackendDeliveryStatus.DEFERRED_QUIET_HOURS,
        untilEpochSeconds
    )

    override suspend fun markAwaitingToken(deliveryKey: DeliveryKey, lease: BackendDeliveryLease): Boolean =
        updateLeasedState(deliveryKey, lease, BackendDeliveryStatus.AWAITING_TOKEN, null)

    override suspend fun blockedCredentialVersion(): String? = synchronized(backendNotificationRuntimeLock) {
        openNotificationRuntimeConnection(databasePath).use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery(
                    "SELECT blocked_credential_version FROM notification_provider_credential_circuit WHERE singleton = 1"
                ).use { rows -> if (rows.next()) rows.getString(1) else null }
            }
        }
    }

    override suspend fun blockedCredential(): BackendValidatedDeliveryCredential? =
        synchronized(backendNotificationRuntimeLock) {
            openNotificationRuntimeConnection(databasePath).use { connection ->
                connection.createStatement().use { statement ->
                    statement.executeQuery(
                        "SELECT blocked_credential_version, blocked_credential_fingerprint " +
                            "FROM notification_provider_credential_circuit WHERE singleton = 1"
                    ).use { rows ->
                        if (!rows.next()) return@synchronized null
                        val version = rows.getString(1) ?: return@synchronized null
                        val fingerprint = rows.getString(2) ?: return@synchronized null
                        BackendValidatedDeliveryCredential(version, fingerprint)
                    }
                }
            }
        }

    override suspend fun blockCredentialVersion(credentialVersion: String): Boolean =
        synchronized(backendNotificationRuntimeLock) {
            openNotificationRuntimeConnection(databasePath).use { connection ->
                connection.prepareStatement(
                    """
                    UPDATE notification_provider_credential_circuit
                    SET blocked_credential_version = ?
                    WHERE singleton = 1 AND (blocked_credential_version IS NULL OR blocked_credential_version = ?)
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, credentialVersion)
                    statement.setString(2, credentialVersion)
                    statement.executeUpdate() == 1
                }
            }
        }

    override suspend fun blockCredential(credential: BackendValidatedDeliveryCredential): Boolean =
        synchronized(backendNotificationRuntimeLock) {
            if (credential.version.isBlank() || credential.fingerprint.isBlank()) return@synchronized false
            openNotificationRuntimeConnection(databasePath).use { connection ->
                connection.prepareStatement(
                    """
                    UPDATE notification_provider_credential_circuit
                    SET blocked_credential_version = ?, blocked_credential_fingerprint = ?
                    WHERE singleton = 1 AND (
                      blocked_credential_version IS NULL OR
                      (blocked_credential_version = ? AND blocked_credential_fingerprint = ?)
                    )
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, credential.version)
                    statement.setString(2, credential.fingerprint)
                    statement.setString(3, credential.version)
                    statement.setString(4, credential.fingerprint)
                    statement.executeUpdate() == 1
                }
            }
        }

    override suspend fun clearCredentialCircuit(newCredentialVersion: String): Boolean =
        synchronized(backendNotificationRuntimeLock) {
            openNotificationRuntimeConnection(databasePath).use { connection ->
                connection.autoCommit = false
                try {
                    val previous = connection.createStatement().use { statement ->
                        statement.executeQuery(
                            "SELECT blocked_credential_version FROM notification_provider_credential_circuit WHERE singleton = 1"
                        ).use { rows -> if (rows.next()) rows.getString(1) else null }
                    }
                    if (previous == null || previous == newCredentialVersion) {
                        connection.rollback()
                        return@synchronized false
                    }
                    connection.prepareStatement(
                        "UPDATE notification_provider_credential_circuit SET blocked_credential_version = NULL WHERE singleton = 1 AND blocked_credential_version = ?"
                    ).use { statement ->
                        statement.setString(1, previous)
                        check(statement.executeUpdate() == 1) { "Credential circuit rotation CAS failed" }
                    }
                    connection.prepareStatement(
                        """
                        UPDATE notification_delivery
                        SET status = ?, credential_version = ?, auth_refresh_count = 0,
                            lease_owner = NULL, lease_expires_at_epoch_seconds = NULL
                        WHERE status = ? AND credential_version = ?
                        """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, BackendDeliveryStatus.QUEUED.name)
                        statement.setString(2, newCredentialVersion)
                        statement.setString(3, BackendDeliveryStatus.PROVIDER_AUTH_BLOCKED.name)
                        statement.setString(4, previous)
                        statement.executeUpdate()
                    }
                    connection.commit()
                    true
                } catch (failure: Throwable) {
                    runCatching { connection.rollback() }.onFailure(failure::addSuppressed)
                    throw failure
                } finally {
                    connection.autoCommit = true
                }
            }
        }

    override suspend fun clearCredentialCircuit(credential: BackendValidatedDeliveryCredential): Boolean =
        synchronized(backendNotificationRuntimeLock) {
            if (credential.version.isBlank() || credential.fingerprint.isBlank()) return@synchronized false
            openNotificationRuntimeConnection(databasePath).use { connection ->
                connection.autoCommit = false
                try {
                    val blocked = connection.createStatement().use { statement ->
                        statement.executeQuery(
                            "SELECT blocked_credential_version, blocked_credential_fingerprint " +
                                "FROM notification_provider_credential_circuit WHERE singleton = 1"
                        ).use { rows ->
                            if (!rows.next()) null else rows.getString(1)?.let { version ->
                                version to rows.getString(2)
                            }
                        }
                    }
                    if (blocked == null || blocked.first == credential.version ||
                        blocked.second == credential.fingerprint) {
                        connection.rollback()
                        return@synchronized false
                    }
                    connection.prepareStatement(
                        """
                        UPDATE notification_provider_credential_circuit
                        SET blocked_credential_version = NULL, blocked_credential_fingerprint = NULL
                        WHERE singleton = 1 AND blocked_credential_version = ?
                          AND blocked_credential_fingerprint IS ?
                        """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, blocked.first)
                        statement.setString(2, blocked.second)
                        check(statement.executeUpdate() == 1) { "Credential circuit rotation CAS failed" }
                    }
                    connection.prepareStatement(
                        """
                        UPDATE notification_delivery
                        SET status = ?, credential_version = ?, credential_fingerprint = ?,
                            auth_refresh_count = 0, lease_owner = NULL,
                            lease_expires_at_epoch_seconds = NULL
                        WHERE status = ? AND credential_version = ?
                        """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, BackendDeliveryStatus.POLICY_CHECK.name)
                        statement.setString(2, credential.version)
                        statement.setString(3, credential.fingerprint)
                        statement.setString(4, BackendDeliveryStatus.PROVIDER_AUTH_BLOCKED.name)
                        statement.setString(5, blocked.first)
                        statement.executeUpdate()
                    }
                    connection.commit()
                    true
                } catch (failure: Throwable) {
                    runCatching { connection.rollback() }.onFailure(failure::addSuppressed)
                    throw failure
                } finally {
                    connection.autoCommit = true
                }
            }
        }

    internal suspend fun stageClassifiedProviderObservation(
        deliveryKey: DeliveryKey,
        lease: BackendDeliveryLease,
        observation: BackendClassifiedProviderObservation,
        credentialVersion: String?,
        credentialFingerprint: String?
    ): BackendNotificationDeliverySnapshot = synchronized(backendNotificationRuntimeLock) {
        openNotificationRuntimeConnection(databasePath).use { connection ->
            val before = snapshot(connection, deliveryKey) ?: error("Delivery does not exist")
            val stateAcceptsObservation = before.state == BackendDurableDeliveryState.SENDING ||
                (observation.outcome == BackendDurableProviderOutcome.PROVIDER_AUTH_BLOCKED &&
                    before.state == BackendDurableDeliveryState.AUTH)
            if (before.pendingCheckpoint != null || before.lastLeaseVersion != lease.version ||
                before.lastLeaseFencingToken != lease.fencingToken ||
                before.lease?.holderId != lease.holderId ||
                before.lease.expiresAtLogicalEpochSeconds <= before.nowEpochSeconds ||
                !stateAcceptsObservation) {
                return@synchronized before
            }
            val staged = stageDeliveryCheckpoint(
                connection, before, observation, credentialVersion, credentialFingerprint, lease
            )
            faultInjector.inject(BackendDeliveryWorkerFaultCheckpoint.AFTER_PROVIDER_OBSERVATION_DURABLE)
            staged
        }
    }

    override suspend fun requestProviderCheckpoint(reference: BackendDeliveryCheckpointReference): Boolean =
        synchronized(backendNotificationRuntimeLock) {
            openNotificationRuntimeConnection(databasePath).use { connection ->
                connection.prepareStatement(
                    """
                    UPDATE notification_delivery_provider_checkpoint SET effect_requested = 1
                    WHERE delivery_key = ? AND effect_id = ? AND checkpoint_revision = ?
                      AND lease_holder IS ? AND lease_version IS ? AND lease_fence IS ?
                      AND EXISTS (
                        SELECT 1 FROM notification_delivery_authority authority
                        WHERE authority.delivery_key = notification_delivery_provider_checkpoint.delivery_key
                          AND authority.authority IS ? AND authority.fencing_token = ?
                      )
                      AND (
                        (lease_holder IS NULL AND lease_version IS NULL AND lease_fence IS NULL)
                        OR EXISTS (
                          SELECT 1 FROM notification_delivery delivery
                          WHERE delivery.delivery_key = notification_delivery_provider_checkpoint.delivery_key
                            AND delivery.lease_owner = notification_delivery_provider_checkpoint.lease_holder
                            AND delivery.lease_version = notification_delivery_provider_checkpoint.lease_version
                            AND delivery.lease_fence = notification_delivery_provider_checkpoint.lease_fence
                            AND delivery.lease_expires_at_epoch_seconds > delivery.logical_now
                        )
                      )
                    """.trimIndent()
                ).use { statement ->
                    bindProviderReference(statement, reference)
                    statement.executeUpdate() == 1
                }
            }
        }

    override suspend fun acknowledgeProviderCheckpoint(
        reference: BackendDeliveryCheckpointReference
    ): BackendNotificationDeliverySnapshot = synchronized(backendNotificationRuntimeLock) {
        openNotificationRuntimeConnection(databasePath).use { connection ->
            connection.createStatement().use { it.execute("BEGIN IMMEDIATE") }
            val before = snapshot(connection, reference.deliveryKey) ?: error("Delivery does not exist")
            val checkpoint = before.pendingCheckpoint
            if (checkpoint == null || !checkpoint.effectRequested || !checkpoint.matches(reference, before) ||
                !checkpointAuthorityIsLiveOrHolderless(checkpoint, before)) {
                connection.createStatement().use { it.execute("ROLLBACK") }
                return@synchronized before
            }
            try {
                val status = when (checkpoint.outcome) {
                    BackendDurableProviderOutcome.ACCEPTED -> BackendDeliveryStatus.ACCEPTED_BY_APNS
                    BackendDurableProviderOutcome.RETRY -> BackendDeliveryStatus.RETRY_SCHEDULED
                    BackendDurableProviderOutcome.INVALID_TOKEN -> BackendDeliveryStatus.INVALID_TOKEN
                    BackendDurableProviderOutcome.REJECTED_PAYLOAD -> BackendDeliveryStatus.REJECTED_PAYLOAD
                    BackendDurableProviderOutcome.REFRESH_AUTH -> if (before.authRefreshCount == 0L) {
                        BackendDeliveryStatus.AUTH
                    } else BackendDeliveryStatus.PROVIDER_AUTH_BLOCKED
                    BackendDurableProviderOutcome.PROVIDER_AUTH_BLOCKED -> BackendDeliveryStatus.PROVIDER_AUTH_BLOCKED
                    BackendDurableProviderOutcome.UNKNOWN_OUTCOME -> BackendDeliveryStatus.UNKNOWN_OUTCOME
                    BackendDurableProviderOutcome.EXPIRED -> BackendDeliveryStatus.EXPIRED
                    BackendDurableProviderOutcome.RETRY_EXHAUSTED -> BackendDeliveryStatus.RETRY_EXHAUSTED
                }
                val retainLease = checkpoint.outcome in setOf(
                    BackendDurableProviderOutcome.REFRESH_AUTH,
                    BackendDurableProviderOutcome.UNKNOWN_OUTCOME,
                    BackendDurableProviderOutcome.PROVIDER_AUTH_BLOCKED
                )
                val updated = connection.prepareStatement(
                    """
                    UPDATE notification_delivery
                    SET status = ?, attempt = ?, next_attempt_at_epoch_seconds = ?, accepted_at = ?,
                        provider_status = ?, provider_reason = ?, provider_request_id = ?,
                        auth_refresh_count = ?, lease_owner = ?, lease_expires_at_epoch_seconds = ?
                    WHERE delivery_key = ? AND checkpoint_revision = ?
                      AND EXISTS (
                        SELECT 1 FROM notification_delivery_provider_checkpoint checkpoint
                        WHERE checkpoint.delivery_key = notification_delivery.delivery_key
                          AND checkpoint.effect_id = ? AND checkpoint.checkpoint_revision = ?
                          AND checkpoint.lease_holder IS ? AND checkpoint.lease_version IS ?
                          AND checkpoint.lease_fence IS ? AND checkpoint.effect_requested = 1
                          AND (
                            (checkpoint.lease_holder IS NULL AND checkpoint.lease_version IS NULL
                              AND checkpoint.lease_fence IS NULL)
                            OR (
                              notification_delivery.lease_owner = checkpoint.lease_holder
                              AND notification_delivery.lease_version = checkpoint.lease_version
                              AND notification_delivery.lease_fence = checkpoint.lease_fence
                              AND notification_delivery.lease_expires_at_epoch_seconds >
                                  notification_delivery.logical_now
                            )
                          )
                      )
                      AND EXISTS (
                        SELECT 1 FROM notification_delivery_authority delivery_authority
                        WHERE delivery_authority.delivery_key = notification_delivery.delivery_key
                          AND delivery_authority.authority IS ?
                          AND delivery_authority.fencing_token = ?
                      )
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, status.name)
                    statement.setLong(2, if (checkpoint.outcome == BackendDurableProviderOutcome.RETRY) before.attempt + 1 else before.attempt)
                    statement.setNullableLong(3, checkpoint.nextAttemptAtEpochSeconds)
                    statement.setNullableLong(4, if (checkpoint.outcome == BackendDurableProviderOutcome.ACCEPTED) checkpoint.acceptedAtEpochSeconds else null)
                    statement.setObject(5, checkpoint.httpStatus)
                    statement.setString(6, checkpoint.reason.name)
                    statement.setString(7, checkpoint.providerRequestId)
                    statement.setLong(8, if (checkpoint.outcome == BackendDurableProviderOutcome.REFRESH_AUTH && before.authRefreshCount == 0L) 1 else before.authRefreshCount)
                    statement.setString(9, reference.leaseHolderId.takeIf { retainLease })
                    statement.setObject(10, before.lease?.expiresAtLogicalEpochSeconds.takeIf { retainLease })
                    statement.setString(11, reference.deliveryKey.value)
                    statement.setLong(12, reference.checkpointRevision)
                    statement.setString(13, reference.effectId)
                    statement.setLong(14, reference.checkpointRevision)
                    statement.setString(15, reference.leaseHolderId)
                    statement.setObject(16, reference.leaseVersion)
                    statement.setObject(17, reference.leaseFencingToken)
                    statement.setString(18, reference.authority?.value)
                    statement.setLong(19, reference.authorityFencingToken)
                    statement.executeUpdate()
                }
                if (updated != 1) {
                    connection.createStatement().use { it.execute("ROLLBACK") }
                    return@synchronized snapshot(connection, reference.deliveryKey)
                        ?: error("Delivery does not exist")
                }
                if (status == BackendDeliveryStatus.PROVIDER_AUTH_BLOCKED) {
                    connection.prepareStatement(
                        """
                        UPDATE notification_provider_credential_circuit
                        SET blocked_credential_version = (
                              SELECT credential_version FROM notification_delivery WHERE delivery_key = ?
                            ),
                            blocked_credential_fingerprint = (
                              SELECT credential_fingerprint FROM notification_delivery WHERE delivery_key = ?
                            )
                        WHERE singleton = 1
                          AND EXISTS (
                            SELECT 1 FROM notification_delivery delivery
                            WHERE delivery.delivery_key = ?
                              AND delivery.credential_version IS NOT NULL
                              AND delivery.credential_version != ''
                              AND delivery.credential_fingerprint IS NOT NULL
                              AND delivery.credential_fingerprint != ''
                          )
                          AND (
                            blocked_credential_version IS NULL OR
                            (
                              blocked_credential_version = (
                                SELECT credential_version FROM notification_delivery WHERE delivery_key = ?
                              )
                              AND blocked_credential_fingerprint = (
                                SELECT credential_fingerprint FROM notification_delivery WHERE delivery_key = ?
                              )
                            )
                          )
                        """.trimIndent()
                    ).use { statement ->
                        repeat(5) { index -> statement.setString(index + 1, reference.deliveryKey.value) }
                        statement.executeUpdate()
                    }
                }
                if (checkpoint.outcome == BackendDurableProviderOutcome.INVALID_TOKEN) {
                    checkpoint.invalidationReason?.let {
                        invalidateExactRegistration(connection, before.registrationId, it, before.nowEpochSeconds)
                    }
                }
                recomputeEffectDispatchStatusForDelivery(connection, reference.deliveryKey)
                connection.prepareStatement(
                    "DELETE FROM notification_delivery_provider_checkpoint WHERE delivery_key = ? AND effect_id = ?"
                ).use { statement ->
                    statement.setString(1, reference.deliveryKey.value)
                    statement.setString(2, reference.effectId)
                    statement.executeUpdate()
                }
                connection.createStatement().use { it.execute("COMMIT") }
            } catch (failure: Throwable) {
                runCatching { connection.createStatement().use { it.execute("ROLLBACK") } }
                    .onFailure(failure::addSuppressed)
                throw failure
            }
            val committed = snapshot(connection, reference.deliveryKey) ?: error("Delivery disappeared")
            faultInjector.inject(BackendDeliveryWorkerFaultCheckpoint.AFTER_PROVIDER_RESULT_COMMIT)
            committed
        }
    }

    override suspend fun advanceLogicalClock(
        deliveryKey: DeliveryKey,
        expectedClockRevision: Long,
        newEpochSeconds: Long
    ): BackendNotificationDeliverySnapshot = synchronized(backendNotificationRuntimeLock) {
        openNotificationRuntimeConnection(databasePath).use { connection ->
            connection.prepareStatement(
                """
                UPDATE notification_delivery SET logical_now = ?, clock_revision = clock_revision + 1
                WHERE delivery_key = ? AND clock_revision = ? AND logical_now <= ?
                """.trimIndent()
            ).use { statement ->
                statement.setLong(1, newEpochSeconds)
                statement.setString(2, deliveryKey.value)
                statement.setLong(3, expectedClockRevision)
                statement.setLong(4, newEpochSeconds)
                statement.executeUpdate()
            }
            snapshot(connection, deliveryKey) ?: error("Delivery does not exist")
        }
    }

    override suspend fun deliveriesForRegistration(registrationId: String): List<DeliveryKey> =
        synchronized(backendNotificationRuntimeLock) {
            openNotificationRuntimeConnection(databasePath).use { connection ->
                connection.prepareStatement(
                    "SELECT delivery_key FROM notification_delivery WHERE device_registration_id = ? ORDER BY delivery_key"
                ).use { statement ->
                    statement.setString(1, registrationId)
                    statement.executeQuery().use { rows -> buildList { while (rows.next()) add(DeliveryKey(rows.getString(1))) } }
                }
            }
        }

    override fun close() = Unit

    private suspend fun directLifecycleCas(
        deliveryKey: DeliveryKey,
        expectedCheckpointRevision: Long,
        authority: DeliveryAuthority?,
        authorityFencingToken: Long,
        allowedStatuses: Set<BackendDeliveryStatus>,
        targetStatus: BackendDeliveryStatus,
        nextAttemptAt: Long?
    ): BackendNotificationDeliverySnapshot = synchronized(backendNotificationRuntimeLock) {
        openNotificationRuntimeConnection(databasePath).use { connection ->
            val placeholders = allowedStatuses.joinToString(",") { "?" }
            connection.createStatement().use { it.execute("BEGIN IMMEDIATE") }
            try {
                val updated = connection.prepareStatement(
                """
                UPDATE notification_delivery
                SET status = ?, checkpoint_revision = checkpoint_revision + 1,
                    next_attempt_at_epoch_seconds = ?, lease_owner = NULL,
                    lease_expires_at_epoch_seconds = NULL
                WHERE delivery_key = ? AND checkpoint_revision = ?
                  AND status IN ($placeholders)
                  AND NOT EXISTS (
                    SELECT 1 FROM notification_delivery_provider_checkpoint checkpoint
                    WHERE checkpoint.delivery_key = notification_delivery.delivery_key
                  )
                  AND EXISTS (
                    SELECT 1 FROM notification_delivery_authority delivery_authority
                    WHERE delivery_authority.delivery_key = notification_delivery.delivery_key
                      AND delivery_authority.authority IS ? AND delivery_authority.fencing_token = ?
                  )
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, targetStatus.name)
                statement.setNullableLong(2, nextAttemptAt)
                statement.setString(3, deliveryKey.value)
                statement.setLong(4, expectedCheckpointRevision)
                allowedStatuses.forEachIndexed { index, status -> statement.setString(index + 5, status.name) }
                statement.setString(5 + allowedStatuses.size, authority?.value)
                statement.setLong(6 + allowedStatuses.size, authorityFencingToken)
                    statement.executeUpdate()
                }
                if (updated == 1) recomputeEffectDispatchStatusForDelivery(connection, deliveryKey)
                connection.createStatement().use { it.execute("COMMIT") }
            } catch (failure: Throwable) {
                runCatching { connection.createStatement().use { it.execute("ROLLBACK") } }
                throw failure
            }
            snapshot(connection, deliveryKey) ?: error("Delivery does not exist")
        }
    }

    private fun stageDeliveryCheckpoint(
        connection: Connection,
        before: BackendNotificationDeliverySnapshot,
        observation: BackendClassifiedProviderObservation,
        credentialVersion: String?,
        credentialFingerprint: String? = null,
        lease: BackendDeliveryLease?,
        checkpointLease: BackendDeliveryLease? = lease
    ): BackendNotificationDeliverySnapshot {
        if (before.pendingCheckpoint != null) return before
        val authority = before.authority ?: return before
        if (authority.value != BackendDeliveryAuthority.OUTBOX_V2.wireValue) return before
        val revision = before.checkpointRevision + 1
        val effectId = "provider-effect:${digest(
            "${before.deliveryKey.value}:$revision:${observation.outcome.name}:" +
                "af${before.authorityFencingToken}:lf${lease?.fencingToken ?: 0}"
        )}"
        connection.autoCommit = false
        try {
            connection.prepareStatement(
                """
                INSERT INTO notification_delivery_provider_checkpoint(
                    delivery_key, effect_id, checkpoint_revision, outcome, reason,
                    http_status, invalidation_reason, accepted_at, next_attempt_at,
                    provider_request_id, lease_holder, lease_version, lease_fence, effect_requested
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, before.deliveryKey.value)
                statement.setString(2, effectId)
                statement.setLong(3, revision)
                statement.setString(4, observation.outcome.name)
                statement.setString(5, observation.reason.name)
                statement.setObject(6, observation.httpStatus)
                statement.setString(7, observation.invalidationReason?.name)
                statement.setNullableLong(8, observation.acceptedAtEpochSeconds)
                statement.setNullableLong(9, observation.nextAttemptAtEpochSeconds)
                statement.setString(10, observation.providerRequestId)
                statement.setString(11, checkpointLease?.holderId)
                statement.setObject(12, checkpointLease?.version)
                statement.setObject(13, checkpointLease?.fencingToken)
                statement.executeUpdate()
            }
            connection.prepareStatement(
                """
                UPDATE notification_delivery
                SET status = ?, checkpoint_revision = ?,
                    provider_checkpoint_count = provider_checkpoint_count + 1,
                    credential_version = ?, credential_fingerprint = ?
                WHERE delivery_key = ? AND checkpoint_revision = ?
                  AND lease_owner IS ? AND lease_version = ? AND lease_fence = ?
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, BackendDeliveryStatus.AWAITING_PROVIDER_RESULT_PERSISTENCE.name)
                statement.setLong(2, revision)
                statement.setString(3, credentialVersion)
                statement.setString(4, credentialFingerprint)
                statement.setString(5, before.deliveryKey.value)
                statement.setLong(6, before.checkpointRevision)
                statement.setString(7, lease?.holderId)
                statement.setLong(8, lease?.version ?: before.lastLeaseVersion)
                statement.setLong(9, lease?.fencingToken ?: before.lastLeaseFencingToken)
                check(statement.executeUpdate() == 1) { "Provider observation checkpoint CAS failed" }
            }
            connection.commit()
        } catch (failure: Throwable) {
            runCatching { connection.rollback() }.onFailure(failure::addSuppressed)
            throw failure
        } finally {
            connection.autoCommit = true
        }
        return snapshot(connection, before.deliveryKey) ?: error("Delivery disappeared")
    }

    private suspend fun updateLeasedState(
        deliveryKey: DeliveryKey,
        lease: BackendDeliveryLease,
        status: BackendDeliveryStatus,
        nextAttemptAt: Long?
    ): Boolean = synchronized(backendNotificationRuntimeLock) {
        openNotificationRuntimeConnection(databasePath).use { connection ->
            connection.prepareStatement(
                """
                UPDATE notification_delivery
                SET status = ?, next_attempt_at_epoch_seconds = ?, lease_owner = NULL, lease_expires_at_epoch_seconds = NULL
                WHERE delivery_key = ? AND lease_owner = ? AND lease_version = ? AND lease_fence = ?
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, status.name)
                statement.setNullableLong(2, nextAttemptAt)
                statement.setString(3, deliveryKey.value)
                statement.setString(4, lease.holderId)
                statement.setLong(5, lease.version)
                statement.setLong(6, lease.fencingToken)
                statement.executeUpdate() == 1
            }
        }
    }

    private fun snapshot(connection: Connection, deliveryKey: DeliveryKey): BackendNotificationDeliverySnapshot? =
        connection.prepareStatement(
            """
            SELECT delivery.*, authority.authority, authority.fencing_token
            FROM notification_delivery delivery
            LEFT JOIN notification_delivery_authority authority ON authority.delivery_key = delivery.delivery_key
            WHERE delivery.delivery_key = ?
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, deliveryKey.value)
            statement.executeQuery().use { rows ->
                if (!rows.next()) return@use null
                val checkpoint = providerCheckpoint(connection, deliveryKey)
                BackendNotificationDeliverySnapshot(
                    deliveryKey = deliveryKey,
                    registrationId = rows.getString("device_registration_id"),
                    state = durableState(rows.getString("status")),
                    attempt = rows.getLong("attempt"),
                    maxAttempts = DEFAULT_MAX_ATTEMPTS,
                    nextAttemptAtEpochSeconds = rows.getNullableLong("next_attempt_at_epoch_seconds"),
                    expiresAtEpochSeconds = rows.getLong("expires_at_epoch_seconds"),
                    nowEpochSeconds = rows.getLong("logical_now"),
                    clockRevision = rows.getLong("clock_revision"),
                    checkpointRevision = rows.getLong("checkpoint_revision"),
                    authority = rows.getString("authority")?.let(::DeliveryAuthority),
                    authorityFencingToken = rows.getLong("fencing_token"),
                    lastLeaseVersion = rows.getLong("lease_version"),
                    lastLeaseFencingToken = rows.getLong("lease_fence"),
                    pendingCheckpoint = checkpoint,
                    providerCheckpointCount = rows.getLong("provider_checkpoint_count"),
                    acceptedAtEpochSeconds = rows.getNullableLong("accepted_at"),
                    providerReason = rows.getString("provider_reason")?.let {
                        runCatching { BackendPersistedProviderReason.valueOf(it) }.getOrNull()
                    },
                    credentialVersion = rows.getString("credential_version"),
                    authRefreshCount = rows.getLong("auth_refresh_count"),
                    correlationId = rows.getString("correlation_id"),
                    lease = rows.getString("lease_owner")?.let { holder ->
                        BackendDeliveryLease(
                            holder,
                            rows.getLong("lease_version"),
                            rows.getLong("lease_fence"),
                            rows.getNullableLong("lease_expires_at_epoch_seconds") ?: Long.MAX_VALUE
                        )
                    }
                )
            }
        }

    private fun providerCheckpoint(connection: Connection, key: DeliveryKey): BackendDeliveryProviderCheckpoint? =
        connection.prepareStatement(
            "SELECT * FROM notification_delivery_provider_checkpoint WHERE delivery_key = ?"
        ).use { statement ->
            statement.setString(1, key.value)
            statement.executeQuery().use { rows ->
                if (!rows.next()) return@use null
                BackendDeliveryProviderCheckpoint(
                    effectId = rows.getString("effect_id"),
                    revision = rows.getLong("checkpoint_revision"),
                    outcome = BackendDurableProviderOutcome.valueOf(rows.getString("outcome")),
                    reason = BackendPersistedProviderReason.valueOf(rows.getString("reason")),
                    httpStatus = rows.getNullableInt("http_status"),
                    invalidationReason = rows.getString("invalidation_reason")?.let {
                        DeviceRegistrationInvalidationReason.valueOf(it)
                    },
                    acceptedAtEpochSeconds = rows.getNullableLong("accepted_at"),
                    nextAttemptAtEpochSeconds = rows.getNullableLong("next_attempt_at"),
                    providerRequestId = rows.getString("provider_request_id"),
                    leaseHolderId = rows.getString("lease_holder"),
                    leaseVersion = rows.getNullableLong("lease_version"),
                    leaseFencingToken = rows.getNullableLong("lease_fence"),
                    effectRequested = rows.getInt("effect_requested") == 1
                )
            }
        }

    private fun BackendDeliveryProviderCheckpoint.matches(
        reference: BackendDeliveryCheckpointReference,
        snapshot: BackendNotificationDeliverySnapshot
    ): Boolean = effectId == reference.effectId && revision == reference.checkpointRevision &&
        leaseHolderId == reference.leaseHolderId && leaseVersion == reference.leaseVersion &&
        leaseFencingToken == reference.leaseFencingToken && snapshot.authority == reference.authority &&
        snapshot.authorityFencingToken == reference.authorityFencingToken

    private fun checkpointAuthorityIsLiveOrHolderless(
        checkpoint: BackendDeliveryProviderCheckpoint,
        snapshot: BackendNotificationDeliverySnapshot
    ): Boolean {
        if (checkpoint.leaseHolderId == null || checkpoint.leaseVersion == null ||
            checkpoint.leaseFencingToken == null) {
            return checkpoint.leaseHolderId == null && checkpoint.leaseVersion == null &&
                checkpoint.leaseFencingToken == null
        }
        val lease = snapshot.lease ?: return false
        return lease.holderId == checkpoint.leaseHolderId &&
            lease.version == checkpoint.leaseVersion &&
            lease.fencingToken == checkpoint.leaseFencingToken &&
            lease.expiresAtLogicalEpochSeconds > snapshot.nowEpochSeconds
    }

    private fun bindProviderReference(statement: java.sql.PreparedStatement, reference: BackendDeliveryCheckpointReference) {
        statement.setString(1, reference.deliveryKey.value)
        statement.setString(2, reference.effectId)
        statement.setLong(3, reference.checkpointRevision)
        statement.setString(4, reference.leaseHolderId)
        statement.setObject(5, reference.leaseVersion)
        statement.setObject(6, reference.leaseFencingToken)
        statement.setString(7, reference.authority?.value)
        statement.setLong(8, reference.authorityFencingToken)
    }

    private fun invalidateExactRegistration(
        connection: Connection,
        registrationId: String,
        invalidationReason: DeviceRegistrationInvalidationReason,
        invalidatedAtEpochSeconds: Long
    ) {
        connection.prepareStatement(
            """
            UPDATE device_registration
            SET status = ?, invalidated_at_epoch_seconds = COALESCE(invalidated_at_epoch_seconds, ?),
                invalidation_reason = ?
            WHERE registration_id = ? AND status = ?
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, DeviceRegistrationStatus.INVALID.name)
            statement.setLong(2, invalidatedAtEpochSeconds)
            statement.setString(3, invalidationReason.name)
            statement.setString(4, registrationId)
            statement.setString(5, DeviceRegistrationStatus.ACTIVE.name)
            statement.executeUpdate()
        }
    }

    private fun durableState(status: String): BackendDurableDeliveryState = when (BackendDeliveryStatus.valueOf(status)) {
        BackendDeliveryStatus.POLICY_CHECK -> BackendDurableDeliveryState.POLICY_CHECK
        BackendDeliveryStatus.SUPPRESSED -> BackendDurableDeliveryState.SUPPRESSED
        BackendDeliveryStatus.QUEUED, BackendDeliveryStatus.LEASED, BackendDeliveryStatus.RETRY -> BackendDurableDeliveryState.QUEUED
        BackendDeliveryStatus.DEFERRED_QUIET_HOURS -> BackendDurableDeliveryState.DEFERRED_QUIET_HOURS
        BackendDeliveryStatus.AWAITING_TOKEN -> BackendDurableDeliveryState.AWAITING_TOKEN
        BackendDeliveryStatus.AUTH -> BackendDurableDeliveryState.AUTH
        BackendDeliveryStatus.SENDING -> BackendDurableDeliveryState.SENDING
        BackendDeliveryStatus.AWAITING_PROVIDER_RESULT_PERSISTENCE -> BackendDurableDeliveryState.AWAITING_PROVIDER_RESULT_PERSISTENCE
        BackendDeliveryStatus.RETRY_SCHEDULED -> BackendDurableDeliveryState.RETRY_SCHEDULED
        BackendDeliveryStatus.UNKNOWN_OUTCOME, BackendDeliveryStatus.UNKNOWN_TERMINAL -> BackendDurableDeliveryState.UNKNOWN_OUTCOME
        BackendDeliveryStatus.ACCEPTED_BY_APNS -> BackendDurableDeliveryState.ACCEPTED_BY_APNS
        BackendDeliveryStatus.INVALID_TOKEN -> BackendDurableDeliveryState.INVALID_TOKEN
        BackendDeliveryStatus.REJECTED_PAYLOAD -> BackendDurableDeliveryState.REJECTED_PAYLOAD
        BackendDeliveryStatus.PROVIDER_AUTH_BLOCKED -> BackendDurableDeliveryState.PROVIDER_AUTH_BLOCKED
        BackendDeliveryStatus.EXPIRED -> BackendDurableDeliveryState.EXPIRED
        BackendDeliveryStatus.RETRY_EXHAUSTED -> BackendDurableDeliveryState.RETRY_EXHAUSTED
        BackendDeliveryStatus.CANCELLED -> BackendDurableDeliveryState.CANCELLED
    }

    private companion object {
        const val DEFAULT_MAX_ATTEMPTS = 5L
        val claimableStates = setOf(
            BackendDurableDeliveryState.QUEUED,
            BackendDurableDeliveryState.RETRY_SCHEDULED
        )
        val preWriteStatuses = setOf(
            BackendDeliveryStatus.POLICY_CHECK,
            BackendDeliveryStatus.DEFERRED_QUIET_HOURS,
            BackendDeliveryStatus.AWAITING_TOKEN,
            BackendDeliveryStatus.QUEUED,
            BackendDeliveryStatus.RETRY,
            BackendDeliveryStatus.RETRY_SCHEDULED
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
        val terminalDeliveryStatuses = listOf(
            BackendDeliveryStatus.SUPPRESSED,
            BackendDeliveryStatus.ACCEPTED_BY_APNS,
            BackendDeliveryStatus.INVALID_TOKEN,
            BackendDeliveryStatus.REJECTED_PAYLOAD,
            BackendDeliveryStatus.PROVIDER_AUTH_BLOCKED,
            BackendDeliveryStatus.EXPIRED,
            BackendDeliveryStatus.RETRY_EXHAUSTED,
            BackendDeliveryStatus.CANCELLED,
            BackendDeliveryStatus.UNKNOWN_TERMINAL
        )
    }
}

private fun safeAdd(left: Long, right: Long): Long =
    if (left > Long.MAX_VALUE - right) Long.MAX_VALUE else left + right

private fun safeIncrement(value: Long): Long = if (value == Long.MAX_VALUE) Long.MAX_VALUE else value + 1

private fun recomputeEffectDispatchStatusForDelivery(connection: Connection, deliveryKey: DeliveryKey) {
    val logicalId = connection.prepareStatement(
        "SELECT logical_notification_id FROM notification_delivery WHERE delivery_key = ?"
    ).use { statement ->
        statement.setString(1, deliveryKey.value)
        statement.executeQuery().use { rows -> if (rows.next()) rows.getString(1) else null }
    } ?: return
    recomputeEffectDispatchStatus(connection, logicalId)
}

private fun recomputeEffectDispatchStatusForRecipient(connection: Connection, recipientKey: RecipientKey) {
    val logicalId = connection.prepareStatement(
        """
        SELECT logical.logical_notification_id
        FROM notification_recipient recipient
        JOIN notification_logical logical ON logical.effect_key = recipient.effect_key
        WHERE recipient.recipient_key = ?
        """.trimIndent()
    ).use { statement ->
        statement.setString(1, recipientKey.value)
        statement.executeQuery().use { rows -> if (rows.next()) rows.getString(1) else null }
    } ?: return
    recomputeEffectDispatchStatus(connection, logicalId)
}

private fun recomputeEffectDispatchStatus(connection: Connection, logicalId: String) {
    val recipientCounts = connection.prepareStatement(
        """
        SELECT COUNT(*) AS total,
               SUM(CASE WHEN recipient.status = ? THEN 1 ELSE 0 END) AS pending
        FROM notification_recipient recipient
        JOIN notification_logical logical ON logical.effect_key = recipient.effect_key
        WHERE logical.logical_notification_id = ?
        """.trimIndent()
    ).use { statement ->
        statement.setString(1, BackendRecipientStatus.PENDING_TARGET.name)
        statement.setString(2, logicalId)
        statement.executeQuery().use { rows ->
            rows.next()
            rows.getLong("total") to rows.getLong("pending")
        }
    }
    val terminalStatuses = listOf(
        BackendDeliveryStatus.ACCEPTED_BY_APNS,
        BackendDeliveryStatus.INVALID_TOKEN,
        BackendDeliveryStatus.REJECTED_PAYLOAD,
        BackendDeliveryStatus.PROVIDER_AUTH_BLOCKED,
        BackendDeliveryStatus.EXPIRED,
        BackendDeliveryStatus.RETRY_EXHAUSTED,
        BackendDeliveryStatus.CANCELLED,
        BackendDeliveryStatus.SUPPRESSED,
        BackendDeliveryStatus.UNKNOWN_TERMINAL
    )
    val terminalPlaceholders = terminalStatuses.joinToString(",") { "?" }
    val deliveryCounts = connection.prepareStatement(
        """
        SELECT COUNT(*) AS total,
               SUM(CASE WHEN status = ? THEN 1 ELSE 0 END) AS accepted,
               SUM(CASE WHEN status IN ($terminalPlaceholders) THEN 1 ELSE 0 END) AS terminal
        FROM notification_delivery
        WHERE logical_notification_id = ?
        """.trimIndent()
    ).use { statement ->
        statement.setString(1, BackendDeliveryStatus.ACCEPTED_BY_APNS.name)
        terminalStatuses.forEachIndexed { index, status -> statement.setString(index + 2, status.name) }
        statement.setString(terminalStatuses.size + 2, logicalId)
        statement.executeQuery().use { rows ->
            rows.next()
            Triple(rows.getLong("total"), rows.getLong("accepted"), rows.getLong("terminal"))
        }
    }
    val (recipientTotal, pendingRecipients) = recipientCounts
    val (deliveryTotal, acceptedDeliveries, terminalDeliveries) = deliveryCounts
    val dispatch = when {
        recipientTotal == 0L -> BackendEffectDispatchStatus.NOT_DISPATCHED
        acceptedDeliveries > 0L && acceptedDeliveries == deliveryTotal && pendingRecipients == 0L ->
            BackendEffectDispatchStatus.DISPATCHED
        acceptedDeliveries > 0L -> BackendEffectDispatchStatus.PARTIALLY_DISPATCHED
        deliveryTotal == 0L && pendingRecipients > 0L -> BackendEffectDispatchStatus.PENDING_RECIPIENT
        deliveryTotal > 0L && terminalDeliveries == deliveryTotal -> BackendEffectDispatchStatus.TERMINAL_FAILURE
        deliveryTotal == 0L && pendingRecipients == 0L -> BackendEffectDispatchStatus.TERMINAL_FAILURE
        else -> BackendEffectDispatchStatus.QUEUED
    }
    connection.prepareStatement(
        "UPDATE notification_logical SET effect_dispatch_status = ? WHERE logical_notification_id = ?"
    ).use { statement ->
        statement.setString(1, dispatch.name)
        statement.setString(2, logicalId)
        statement.executeUpdate()
    }
}

internal fun digest(value: String): String = Base64.getUrlEncoder().withoutPadding().encodeToString(
    MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
)

private fun java.sql.PreparedStatement.setNullableLong(index: Int, value: Long?) {
    if (value == null) setObject(index, null) else setLong(index, value)
}

private fun ResultSet.getNullableLong(column: String): Long? = getLong(column).takeUnless { wasNull() }
private fun ResultSet.getNullableInt(column: String): Int? = getInt(column).takeUnless { wasNull() }
