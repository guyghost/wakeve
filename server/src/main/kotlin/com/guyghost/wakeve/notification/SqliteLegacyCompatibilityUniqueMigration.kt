package com.guyghost.wakeve.notification

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.security.MessageDigest
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Types
import java.util.Base64
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

internal class SqliteLegacyCompatibilityUniqueMigrationRuntime(
    private val databasePath: Path,
    private val migrationId: String,
    private val schemaVersion: Int,
    private val initialLogicalNowEpochSeconds: Long,
    private val faultInjector: LegacyCompatibilityUniqueMigrationFaultInjector,
    private val preflightProbe: LegacyCompatibilityUniqueMigrationReadOnlyPreflightProbe,
    private val jdbcOpenTestHook: DeviceRegistrationJdbcOpenTestHook
) : LegacyCompatibilityUniqueMigrationRuntime {
    private val closed = AtomicBoolean(false)
    private var recoveryClaimRequired = read { connection -> migrationRecord(connection) != null }
    private var lastObservedGroups: List<LegacyCompatibilityUniqueMigrationDuplicateGroup> = emptyList()

    init {
        require(migrationId.isNotBlank()) { "migrationId is required" }
        require(schemaVersion > 0) { "schemaVersion must be positive" }
        require(initialLogicalNowEpochSeconds >= 0) {
            "initialLogicalNowEpochSeconds must be non-negative"
        }
    }

    override fun startOrResume(): LegacyCompatibilityUniqueMigrationSnapshot {
        ensureOpen()
        var record = read { connection -> migrationRecord(connection) }
        if (record == null) {
            return initialPreflight()
        }
        if (record.snapshot.requiredEffect == null) return record.snapshot
        return runPendingEffects(record.snapshot, recoverFirst = recoveryClaimRequired)
    }

    override fun currentSnapshot(): LegacyCompatibilityUniqueMigrationSnapshot {
        ensureOpen()
        return read { connection -> migrationRecord(connection)?.snapshot } ?: ephemeralPreflightSnapshot()
    }

    override fun advanceLogicalClock(
        expectedRevision: Long,
        newEpochSeconds: Long
    ): LegacyCompatibilityUniqueMigrationSnapshot {
        ensureOpen()
        require(expectedRevision >= 0) { "Expected clock revision must be non-negative" }
        require(newEpochSeconds >= 0) { "Logical epoch seconds must be non-negative" }
        val observed = read { connection -> migrationRecord(connection) }
            ?: return ephemeralPreflightSnapshot()
        if (
            expectedRevision != observed.snapshot.clockRevision ||
            newEpochSeconds < observed.snapshot.logicalNowEpochSeconds
        ) {
            return observed.snapshot
        }
        return immediateTransaction { connection ->
            val record = migrationRecord(connection)
                ?: return@immediateTransaction ephemeralPreflightSnapshot()
            val current = record.snapshot
            if (
                expectedRevision != current.clockRevision ||
                newEpochSeconds < current.logicalNowEpochSeconds
            ) {
                return@immediateTransaction current
            }
            val updated = record.copy(
                snapshot = current.copy(
                    logicalNowEpochSeconds = newEpochSeconds,
                    clockRevision = current.clockRevision + 1L
                )
            )
            upsertMigrationRecord(connection, updated)
            updated.snapshot.copy(archiveAudit = archiveAudit(connection))
        }
    }

    override fun diagnostic(): LegacyCompatibilityUniqueMigrationDiagnostic {
        val snapshot = currentSnapshot()
        val groups = if (snapshot.state == LegacyCompatibilityUniqueMigrationState.BLOCKED_DUPLICATES) {
            inspectOperatorGroups()
        } else {
            lastObservedGroups
        }
        return LegacyCompatibilityUniqueMigrationDiagnostic(
            migrationId = migrationId,
            schemaVersion = schemaVersion,
            scanRevision = snapshot.scanRevision,
            duplicateGroupCount = groups.size,
            duplicateRowCount = groups.sumOf { it.rowCount },
            groupDigests = groups.map { it.groupDigest },
            failure = snapshot.failure
        )
    }

    override fun inspectOperatorGroups(): List<LegacyCompatibilityUniqueMigrationDuplicateGroup> {
        ensureOpen()
        return read(::scanDuplicateGroups).also { lastObservedGroups = it }
    }

    override fun requestOperatorResolution(
        request: LegacyCompatibilityUniqueMigrationOperatorResolutionRequest
    ): LegacyCompatibilityUniqueMigrationSnapshot {
        ensureOpen()
        val current = currentSnapshot()
        if (
            current.state != LegacyCompatibilityUniqueMigrationState.BLOCKED_DUPLICATES ||
            request.migrationId != migrationId ||
            request.scanRevision != current.scanRevision ||
            request.operatorResolutionId.isBlank()
        ) {
            return current
        }
        val group = inspectOperatorGroups().singleOrNull { it.groupDigest == request.groupDigest }
            ?: return current
        if (!group.operatorResolvable || request.canonicalSagaId !in group.sagaIds) return current

        val record = immediateTransaction { connection ->
            ensureLegacyCompatibilityBaseSchema(connection)
            ensureMigrationSchema(connection)
            val authoritative = migrationRecord(connection)
            if (
                authoritative != null &&
                (
                    authoritative.snapshot.state != LegacyCompatibilityUniqueMigrationState.BLOCKED_DUPLICATES ||
                        authoritative.snapshot.scanRevision != request.scanRevision
                    )
            ) {
                return@immediateTransaction authoritative
            }
            val groups = scanDuplicateGroups(connection)
            val authoritativeGroup = groups.singleOrNull { it.groupDigest == request.groupDigest }
            if (
                authoritativeGroup == null ||
                !authoritativeGroup.operatorResolvable ||
                request.canonicalSagaId !in authoritativeGroup.sagaIds
            ) {
                return@immediateTransaction authoritative ?: return@immediateTransaction null
            }
            val previousRevision = authoritative?.snapshot?.requiredEffect?.checkpointRevision ?: 1L
            val previousFence = max(
                authoritative?.snapshot?.requiredEffect?.fencingToken ?: 1L,
                authoritative?.snapshot?.lastRecoveryFencingToken ?: 0L
            )
            val reference = effectReference(
                checkpoint = LegacyCompatibilityUniqueMigrationEffectCheckpoint
                    .ARCHIVE_NONCANONICAL_ROWS_AND_EFFECTS,
                revision = previousRevision + 1L,
                fencingToken = previousFence + 1L,
                scanRevision = request.scanRevision,
                groupDigest = request.groupDigest,
                operatorResolutionId = request.operatorResolutionId
            )
            val next = MigrationRecord(
                snapshot = LegacyCompatibilityUniqueMigrationSnapshot(
                    migrationId = migrationId,
                    schemaVersion = schemaVersion,
                    state = LegacyCompatibilityUniqueMigrationState.ARCHIVING_NONCANONICAL_ROWS,
                    scanRevision = request.scanRevision,
                    logicalNowEpochSeconds = authoritative?.snapshot?.logicalNowEpochSeconds
                        ?: initialLogicalNowEpochSeconds,
                    clockRevision = authoritative?.snapshot?.clockRevision ?: 0L,
                    requiredEffect = reference,
                    recoveryLease = null,
                    lastRecoveryLeaseVersion = authoritative?.snapshot?.lastRecoveryLeaseVersion ?: 0L,
                    lastRecoveryFencingToken = authoritative?.snapshot?.lastRecoveryFencingToken ?: 0L,
                    archiveAudit = archiveAudit(connection),
                    failure = null
                ),
                activeGroupDigest = request.groupDigest,
                canonicalSagaId = request.canonicalSagaId,
                operatorResolutionId = request.operatorResolutionId,
                activeArchiveId = null,
                activeArchiveDigest = null
            )
            upsertMigrationRecord(connection, next)
            next
        } ?: return currentSnapshot()
        recoveryClaimRequired = false
        return runPendingEffects(record.snapshot, recoverFirst = false)
    }

    override fun archiveAudit(): List<LegacyCompatibilityUniqueMigrationArchiveAudit> {
        ensureOpen()
        return read(::archiveAudit)
    }

    override fun archiveBundle(
        archiveId: String
    ): LegacyCompatibilityUniqueMigrationArchiveBundle? {
        ensureOpen()
        val normalized = archiveId.trim()
        if (normalized.isEmpty()) return null
        return read { connection -> archiveBundle(connection, normalized) }
    }

    override fun acquireRecoveryLease(
        request: LegacyCompatibilityUniqueMigrationRecoveryLeaseRequest
    ): LegacyCompatibilityUniqueMigrationRecoveryLease? {
        ensureOpen()
        return immediateTransaction { connection ->
            val record = migrationRecord(connection) ?: return@immediateTransaction null
            val current = record.snapshot
            val effect = current.requiredEffect ?: return@immediateTransaction null
            val activeLease = current.recoveryLease
            val authoritativeLeaseVersion = activeLease?.leaseVersion
                ?: current.lastRecoveryLeaseVersion
            if (
                request.migrationId != migrationId ||
                request.holderId.isBlank() ||
                request.expectedEffectId != effect.effectId ||
                request.effectCheckpoint != effect.checkpoint ||
                request.checkpointRevision != effect.checkpointRevision ||
                request.expectedLeaseVersion != authoritativeLeaseVersion ||
                request.newLeaseVersion <= request.expectedLeaseVersion ||
                request.fencingToken <= max(effect.fencingToken, current.lastRecoveryFencingToken) ||
                request.expiresAtLogicalEpochSeconds <= current.logicalNowEpochSeconds ||
                activeLease?.let {
                    current.logicalNowEpochSeconds < it.expiresAtLogicalEpochSeconds
                } == true
            ) {
                return@immediateTransaction null
            }
            val lease = LegacyCompatibilityUniqueMigrationRecoveryLease(
                migrationId = migrationId,
                leaseId = UUID.randomUUID().toString(),
                holderId = request.holderId,
                leaseVersion = request.newLeaseVersion,
                fencingToken = request.fencingToken,
                expiresAtLogicalEpochSeconds = request.expiresAtLogicalEpochSeconds,
                acquiredAtClockRevision = current.clockRevision,
                expectedEffectId = effect.effectId,
                effectCheckpoint = effect.checkpoint,
                checkpointRevision = effect.checkpointRevision,
                effectEmitted = false
            )
            val updated = record.copy(
                snapshot = current.copy(
                    requiredEffect = effect.copy(fencingToken = request.fencingToken),
                    recoveryLease = lease,
                    lastRecoveryLeaseVersion = request.newLeaseVersion,
                    lastRecoveryFencingToken = request.fencingToken
                )
            )
            upsertMigrationRecord(connection, updated)
            lease
        }
    }

    override fun requestRecovery(
        request: LegacyCompatibilityUniqueMigrationRecoveryRequest
    ): LegacyCompatibilityUniqueMigrationEffectReference? {
        ensureOpen()
        return immediateTransaction { connection ->
            val record = migrationRecord(connection) ?: return@immediateTransaction null
            val current = record.snapshot
            val lease = current.recoveryLease ?: return@immediateTransaction null
            val effect = current.requiredEffect ?: return@immediateTransaction null
            if (
                lease.effectEmitted ||
                current.logicalNowEpochSeconds >= lease.expiresAtLogicalEpochSeconds ||
                request.migrationId != migrationId ||
                request.leaseId != lease.leaseId ||
                request.holderId != lease.holderId ||
                request.leaseVersion != lease.leaseVersion ||
                request.fencingToken != lease.fencingToken ||
                request.expectedEffectId != lease.expectedEffectId ||
                request.effectCheckpoint != lease.effectCheckpoint ||
                request.checkpointRevision != lease.checkpointRevision ||
                effect.effectId != request.expectedEffectId ||
                effect.checkpoint != request.effectCheckpoint ||
                effect.checkpointRevision != request.checkpointRevision ||
                effect.fencingToken != request.fencingToken
            ) {
                return@immediateTransaction null
            }
            val updatedLease = lease.copy(effectEmitted = true)
            upsertMigrationRecord(
                connection,
                record.copy(snapshot = current.copy(recoveryLease = updatedLease))
            )
            effect
        }
    }

    override fun acknowledgeEffect(
        acknowledgement: LegacyCompatibilityUniqueMigrationEffectAcknowledgement
    ): LegacyCompatibilityUniqueMigrationSnapshot {
        ensureOpen()
        val reference = LegacyCompatibilityUniqueMigrationEffectReference(
            effectId = acknowledgement.effectId,
            checkpoint = acknowledgement.checkpoint,
            checkpointRevision = acknowledgement.checkpointRevision,
            fencingToken = acknowledgement.fencingToken
        )
        return acknowledgeCompletedEffect(reference)
    }

    override fun confirmExternalRepair(
        scanRevision: Long,
        repairEvidenceDigest: String
    ): LegacyCompatibilityUniqueMigrationSnapshot {
        ensureOpen()
        val current = currentSnapshot()
        if (
            current.state !in setOf(
                LegacyCompatibilityUniqueMigrationState.BLOCKED_MIGRATION_FAILURE,
                LegacyCompatibilityUniqueMigrationState.BLOCKED_DUPLICATES
            ) ||
            scanRevision != current.scanRevision ||
            repairEvidenceDigest.isBlank()
        ) {
            return current
        }
        val updated = immediateTransaction { connection ->
            ensureLegacyCompatibilityBaseSchema(connection)
            ensureMigrationSchema(connection)
            val record = migrationRecord(connection)
            if (record != null && record.snapshot.scanRevision != scanRevision) {
                return@immediateTransaction record
            }
            val currentRevision = record?.snapshot?.requiredEffect?.checkpointRevision ?: 1L
            val currentFence = max(
                record?.snapshot?.requiredEffect?.fencingToken ?: 1L,
                record?.snapshot?.lastRecoveryFencingToken ?: 0L
            )
            val reference = effectReference(
                checkpoint = LegacyCompatibilityUniqueMigrationEffectCheckpoint.SCAN_DUPLICATES,
                revision = currentRevision + 1L,
                fencingToken = currentFence + 1L,
                scanRevision = scanRevision
            )
            val next = MigrationRecord(
                snapshot = LegacyCompatibilityUniqueMigrationSnapshot(
                    migrationId = migrationId,
                    schemaVersion = schemaVersion,
                    state = LegacyCompatibilityUniqueMigrationState.STARTUP_PREFLIGHT,
                    scanRevision = scanRevision,
                    logicalNowEpochSeconds = record?.snapshot?.logicalNowEpochSeconds
                        ?: initialLogicalNowEpochSeconds,
                    clockRevision = record?.snapshot?.clockRevision ?: 0L,
                    requiredEffect = reference,
                    recoveryLease = null,
                    lastRecoveryLeaseVersion = record?.snapshot?.lastRecoveryLeaseVersion ?: 0L,
                    lastRecoveryFencingToken = record?.snapshot?.lastRecoveryFencingToken ?: 0L,
                    archiveAudit = archiveAudit(connection),
                    failure = null
                ),
                activeGroupDigest = null,
                canonicalSagaId = null,
                operatorResolutionId = null,
                activeArchiveId = null,
                activeArchiveDigest = null
            )
            upsertMigrationRecord(connection, next)
            next
        }
        recoveryClaimRequired = true
        return updated.snapshot
    }

    override fun close() {
        closed.compareAndSet(false, true)
    }

    private fun initialPreflight(): LegacyCompatibilityUniqueMigrationSnapshot {
        faultInjector.evaluate(
            LegacyCompatibilityUniqueMigrationEffectCheckpoint.SCAN_DUPLICATES,
            LegacyCompatibilityUniqueMigrationCheckpointPhase.BEFORE_EFFECT
        )?.let { return persistInitialFailure(it) }
        val observation = try {
            read { connection ->
                val groups = scanDuplicateGroups(connection)
                ReadOnlyPreflight(
                    groups = groups,
                    uniqueRequestKeyIndexPresent = hasUniqueRequestKeyIndex(connection)
                )
            }
        } catch (_: SQLException) {
            return persistInitialFailure(
                LegacyCompatibilityUniqueMigrationFailure.PREFLIGHT_UNAVAILABLE
            )
        }
        lastObservedGroups = observation.groups
        preflightProbe.afterObservation(
            LegacyCompatibilityUniqueMigrationPreflightObservation(
                duplicateGroupCount = observation.groups.size,
                duplicateRowCount = observation.groups.sumOf { it.rowCount },
                uniqueRequestKeyIndexPresent = observation.uniqueRequestKeyIndexPresent
            )
        )
        faultInjector.evaluate(
            LegacyCompatibilityUniqueMigrationEffectCheckpoint.SCAN_DUPLICATES,
            LegacyCompatibilityUniqueMigrationCheckpointPhase.AFTER_COMMIT
        )?.let { return persistInitialFailure(it) }
        if (observation.groups.isNotEmpty()) {
            return ephemeralBlockedSnapshot(observation.groups)
        }

        val record = immediateTransaction { connection ->
            ensureLegacyCompatibilityBaseSchema(connection)
            ensureMigrationSchema(connection)
            migrationRecord(connection) ?: run {
                val indexed = observation.uniqueRequestKeyIndexPresent
                val state = if (indexed) {
                    LegacyCompatibilityUniqueMigrationState.READY
                } else {
                    LegacyCompatibilityUniqueMigrationState.INSTALLING_UNIQUE_INDEX
                }
                val reference = if (indexed) null else effectReference(
                    checkpoint = LegacyCompatibilityUniqueMigrationEffectCheckpoint
                        .INSTALL_UNIQUE_REQUEST_KEY_INDEX,
                    revision = 2L,
                    fencingToken = 2L,
                    scanRevision = 1L
                )
                MigrationRecord(
                    snapshot = LegacyCompatibilityUniqueMigrationSnapshot(
                        migrationId = migrationId,
                        schemaVersion = schemaVersion,
                        state = state,
                        scanRevision = 1L,
                        logicalNowEpochSeconds = initialLogicalNowEpochSeconds,
                        clockRevision = 0L,
                        requiredEffect = reference,
                        recoveryLease = null,
                        lastRecoveryLeaseVersion = 0L,
                        lastRecoveryFencingToken = 0L,
                        archiveAudit = archiveAudit(connection),
                        failure = null
                    ),
                    activeGroupDigest = null,
                    canonicalSagaId = null,
                    operatorResolutionId = null,
                    activeArchiveId = null,
                    activeArchiveDigest = null
                ).also { upsertMigrationRecord(connection, it) }
            }
        }
        recoveryClaimRequired = false
        return runPendingEffects(record.snapshot, recoverFirst = false)
    }

    private fun runPendingEffects(
        startingSnapshot: LegacyCompatibilityUniqueMigrationSnapshot,
        recoverFirst: Boolean
    ): LegacyCompatibilityUniqueMigrationSnapshot {
        var current = startingSnapshot
        var needsRecoveryLease = recoverFirst
        repeat(MAX_EFFECTS_PER_RUN) {
            val required = current.requiredEffect ?: return current
            val executable = if (needsRecoveryLease) {
                acquireInternalRecoveryAuthority(current) ?: return currentSnapshot()
            } else {
                required
            }
            needsRecoveryLease = false
            recoveryClaimRequired = false

            faultInjector.evaluate(
                executable.checkpoint,
                LegacyCompatibilityUniqueMigrationCheckpointPhase.BEFORE_EFFECT
            )?.let { failure -> return blockMigrationFailure(executable, failure) }

            try {
                performEffect(executable)
            } catch (failure: Throwable) {
                if (failure is Error) throw failure
                return blockMigrationFailure(executable, failureFor(executable.checkpoint))
            }

            faultInjector.evaluate(
                executable.checkpoint,
                LegacyCompatibilityUniqueMigrationCheckpointPhase.AFTER_COMMIT
            )?.let { failure -> return blockMigrationFailure(executable, failure) }

            current = acknowledgeCompletedEffect(executable)
            if (current.requiredEffect == null) return current
        }
        return currentSnapshot()
    }

    private fun performEffect(reference: LegacyCompatibilityUniqueMigrationEffectReference) {
        when (reference.checkpoint) {
            LegacyCompatibilityUniqueMigrationEffectCheckpoint
                .ARCHIVE_NONCANONICAL_ROWS_AND_EFFECTS -> performArchive(reference)
            LegacyCompatibilityUniqueMigrationEffectCheckpoint
                .DELETE_ARCHIVED_NONCANONICAL_ROWS -> performDelete(reference)
            LegacyCompatibilityUniqueMigrationEffectCheckpoint.SCAN_DUPLICATES -> {
                lastObservedGroups = read(::scanDuplicateGroups)
            }
            LegacyCompatibilityUniqueMigrationEffectCheckpoint
                .INSTALL_UNIQUE_REQUEST_KEY_INDEX -> immediateTransaction { connection ->
                connection.createStatement().use { statement ->
                    statement.execute(
                        "CREATE UNIQUE INDEX IF NOT EXISTS " +
                            "$UNIQUE_REQUEST_KEY_INDEX ON $SAGA_TABLE(request_key)"
                    )
                }
            }
        }
    }

    private fun performArchive(reference: LegacyCompatibilityUniqueMigrationEffectReference) {
        immediateTransaction { connection ->
            val record = migrationRecord(connection)
                ?: throw IllegalStateException("Migration checkpoint is unavailable")
            if (record.snapshot.requiredEffect != reference) {
                throw IllegalStateException("Migration checkpoint changed")
            }
            val groupDigest = record.activeGroupDigest
                ?: throw IllegalStateException("Migration group is unavailable")
            val canonicalSagaId = record.canonicalSagaId
                ?: throw IllegalStateException("Canonical saga is unavailable")
            val operatorResolutionId = record.operatorResolutionId
                ?: throw IllegalStateException("Operator resolution is unavailable")
            val group = scanDuplicateGroups(connection).singleOrNull { it.groupDigest == groupDigest }
                ?: archiveAuditByResolution(connection, operatorResolutionId)?.let { return@immediateTransaction }
                ?: throw IllegalStateException("Duplicate group changed")
            if (!group.operatorResolvable || canonicalSagaId !in group.sagaIds) {
                throw IllegalStateException("Duplicate group is no longer safely resolvable")
            }
            archiveAuditByResolution(connection, operatorResolutionId)?.let {
                return@immediateTransaction
            }

            val archivedSagaIds = group.sagaIds.filterNot { it == canonicalSagaId }
            val rowPayloads = archivedSagaIds.associateWith { sagaId ->
                querySingleRowPayload(connection, sagaId)
            }
            val effects = archivedSagaIds.flatMap { sagaId ->
                queryLiveEffects(connection, sagaId)
            }
            val archiveId = opaqueCompatibilityDigest(
                listOf(
                    "legacy-compatibility-unique-migration-archive",
                    "v1",
                    migrationId,
                    record.snapshot.scanRevision.toString(),
                    groupDigest,
                    operatorResolutionId
                )
            )
            val archiveDigest = opaqueCompatibilityDigest(
                buildList {
                    add("archive-digest")
                    rowPayloads.toSortedMap().forEach { (sagaId, payload) ->
                        add(sagaId)
                        add(Base64.getEncoder().encodeToString(payload))
                    }
                    effects.sortedWith(compareBy({ it.sagaId }, { it.effectId }, { it.fencingToken }))
                        .forEach { effect ->
                            add(effect.sagaId)
                            add(effect.effectId)
                            add(effect.checkpoint)
                            add(effect.checkpointRevision.toString())
                            add(effect.fencingToken.toString())
                        }
                }
            )
            connection.prepareStatement(
                """
                INSERT INTO $ARCHIVE_AUDIT_TABLE(
                    archive_id, migration_id, archive_digest, group_digest,
                    canonical_saga_id, archived_row_count, scan_revision,
                    operator_resolution_id
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, archiveId)
                statement.setString(2, migrationId)
                statement.setString(3, archiveDigest)
                statement.setString(4, groupDigest)
                statement.setString(5, canonicalSagaId)
                statement.setInt(6, archivedSagaIds.size)
                statement.setLong(7, record.snapshot.scanRevision)
                statement.setString(8, operatorResolutionId)
                statement.executeUpdate()
            }
            connection.prepareStatement(
                "INSERT INTO $ARCHIVE_ROW_TABLE(archive_id, saga_id, row_payload, payload_digest) " +
                    "VALUES (?, ?, ?, ?)"
            ).use { statement ->
                rowPayloads.toSortedMap().forEach { (sagaId, payload) ->
                    statement.setString(1, archiveId)
                    statement.setString(2, sagaId)
                    statement.setBytes(3, payload)
                    statement.setString(
                        4,
                        digestBytes(payload)
                    )
                    statement.addBatch()
                }
                statement.executeBatch()
            }
            connection.prepareStatement(
                """
                INSERT INTO $ARCHIVE_EFFECT_TABLE(
                    archive_id, saga_id, effect_id, effect_checkpoint,
                    checkpoint_revision, fencing_token
                ) VALUES (?, ?, ?, ?, ?, ?)
                """.trimIndent()
            ).use { statement ->
                effects.forEach { effect ->
                    statement.setString(1, archiveId)
                    statement.setString(2, effect.sagaId)
                    statement.setString(3, effect.effectId)
                    statement.setString(4, effect.checkpoint)
                    statement.setLong(5, effect.checkpointRevision)
                    statement.setLong(6, effect.fencingToken)
                    statement.addBatch()
                }
                statement.executeBatch()
            }
        }
    }

    private fun performDelete(reference: LegacyCompatibilityUniqueMigrationEffectReference) {
        immediateTransaction { connection ->
            val record = migrationRecord(connection)
                ?: throw IllegalStateException("Migration checkpoint is unavailable")
            if (record.snapshot.requiredEffect != reference) {
                throw IllegalStateException("Migration checkpoint changed")
            }
            val archiveId = record.activeArchiveId
                ?: throw IllegalStateException("Archive identity is unavailable")
            val sagaIds = connection.prepareStatement(
                "SELECT saga_id FROM $ARCHIVE_ROW_TABLE WHERE archive_id = ? ORDER BY saga_id"
            ).use { statement ->
                statement.setString(1, archiveId)
                statement.executeQuery().use { rows ->
                    buildList { while (rows.next()) add(rows.getString(1)) }
                }
            }
            sagaIds.forEach { sagaId ->
                connection.prepareStatement(
                    "DELETE FROM legacy_notification_compatibility_effect_history WHERE saga_id = ?"
                ).use { statement ->
                    statement.setString(1, sagaId)
                    statement.executeUpdate()
                }
                connection.prepareStatement(
                    "DELETE FROM $SAGA_TABLE WHERE saga_id = ?"
                ).use { statement ->
                    statement.setString(1, sagaId)
                    statement.executeUpdate()
                }
            }
        }
    }

    private fun acknowledgeCompletedEffect(
        reference: LegacyCompatibilityUniqueMigrationEffectReference
    ): LegacyCompatibilityUniqueMigrationSnapshot = immediateTransaction { connection ->
        val record = migrationRecord(connection) ?: return@immediateTransaction ephemeralPreflightSnapshot()
        val current = record.snapshot
        if (current.requiredEffect != reference || !ackAuthorityIsCurrent(current, reference)) {
            return@immediateTransaction current
        }
        val next = when (reference.checkpoint) {
            LegacyCompatibilityUniqueMigrationEffectCheckpoint
                .ARCHIVE_NONCANONICAL_ROWS_AND_EFFECTS -> {
                val resolutionId = record.operatorResolutionId
                    ?: return@immediateTransaction current
                val audit = archiveAuditByResolution(connection, resolutionId)
                    ?: return@immediateTransaction current
                record.withNextEffect(
                    state = LegacyCompatibilityUniqueMigrationState.DELETING_ARCHIVED_ROWS,
                    checkpoint = LegacyCompatibilityUniqueMigrationEffectCheckpoint
                        .DELETE_ARCHIVED_NONCANONICAL_ROWS,
                    activeArchiveId = audit.archiveId,
                    activeArchiveDigest = audit.archiveDigest
                )
            }
            LegacyCompatibilityUniqueMigrationEffectCheckpoint
                .DELETE_ARCHIVED_NONCANONICAL_ROWS -> record.withNextEffect(
                state = LegacyCompatibilityUniqueMigrationState.STARTUP_PREFLIGHT,
                checkpoint = LegacyCompatibilityUniqueMigrationEffectCheckpoint.SCAN_DUPLICATES
            )
            LegacyCompatibilityUniqueMigrationEffectCheckpoint.SCAN_DUPLICATES -> {
                val groups = scanDuplicateGroups(connection)
                lastObservedGroups = groups
                val newScanRevision = current.scanRevision + 1L
                if (groups.isNotEmpty()) {
                    record.copy(
                        snapshot = current.copy(
                            state = LegacyCompatibilityUniqueMigrationState.BLOCKED_DUPLICATES,
                            scanRevision = newScanRevision,
                            requiredEffect = null,
                            recoveryLease = null,
                            failure = null
                        ),
                        activeGroupDigest = null,
                        canonicalSagaId = null,
                        operatorResolutionId = null,
                        activeArchiveId = null,
                        activeArchiveDigest = null
                    )
                } else if (hasUniqueRequestKeyIndex(connection)) {
                    record.copy(
                        snapshot = current.copy(
                            state = LegacyCompatibilityUniqueMigrationState.READY,
                            scanRevision = newScanRevision,
                            requiredEffect = null,
                            recoveryLease = null,
                            failure = null
                        ),
                        activeGroupDigest = null,
                        canonicalSagaId = null,
                        operatorResolutionId = null,
                        activeArchiveId = null,
                        activeArchiveDigest = null
                    )
                } else {
                    record.copy(snapshot = current.copy(scanRevision = newScanRevision))
                        .withNextEffect(
                            state = LegacyCompatibilityUniqueMigrationState.INSTALLING_UNIQUE_INDEX,
                            checkpoint = LegacyCompatibilityUniqueMigrationEffectCheckpoint
                                .INSTALL_UNIQUE_REQUEST_KEY_INDEX
                        )
                }
            }
            LegacyCompatibilityUniqueMigrationEffectCheckpoint
                .INSTALL_UNIQUE_REQUEST_KEY_INDEX -> {
                if (!hasUniqueRequestKeyIndex(connection)) return@immediateTransaction current
                record.copy(
                    snapshot = current.copy(
                        state = LegacyCompatibilityUniqueMigrationState.READY,
                        requiredEffect = null,
                        recoveryLease = null,
                        failure = null
                    ),
                    activeGroupDigest = null,
                    canonicalSagaId = null,
                    operatorResolutionId = null,
                    activeArchiveId = null,
                    activeArchiveDigest = null
                )
            }
        }
        upsertMigrationRecord(connection, next)
        next.snapshot.copy(archiveAudit = archiveAudit(connection))
    }

    private fun acquireInternalRecoveryAuthority(
        snapshot: LegacyCompatibilityUniqueMigrationSnapshot
    ): LegacyCompatibilityUniqueMigrationEffectReference? {
        val required = snapshot.requiredEffect ?: return null
        val expectedVersion = snapshot.recoveryLease?.leaseVersion
            ?: snapshot.lastRecoveryLeaseVersion
        val fence = max(required.fencingToken, snapshot.lastRecoveryFencingToken) + 1L
        val expiry = snapshot.logicalNowEpochSeconds + INTERNAL_LEASE_DURATION_SECONDS
        val lease = acquireRecoveryLease(
            LegacyCompatibilityUniqueMigrationRecoveryLeaseRequest(
                migrationId = migrationId,
                expectedEffectId = required.effectId,
                effectCheckpoint = required.checkpoint,
                checkpointRevision = required.checkpointRevision,
                holderId = "migration-runtime-${UUID.randomUUID()}",
                expectedLeaseVersion = expectedVersion,
                newLeaseVersion = expectedVersion + 1L,
                fencingToken = fence,
                expiresAtLogicalEpochSeconds = expiry
            )
        ) ?: return null
        return requestRecovery(lease.asRecoveryRequest())
    }

    private fun blockMigrationFailure(
        reference: LegacyCompatibilityUniqueMigrationEffectReference,
        failure: LegacyCompatibilityUniqueMigrationFailure
    ): LegacyCompatibilityUniqueMigrationSnapshot = immediateTransaction { connection ->
        val record = migrationRecord(connection) ?: return@immediateTransaction persistInitialFailure(failure)
        if (record.snapshot.requiredEffect != reference) return@immediateTransaction record.snapshot
        val blocked = record.copy(
            snapshot = record.snapshot.copy(
                state = LegacyCompatibilityUniqueMigrationState.BLOCKED_MIGRATION_FAILURE,
                requiredEffect = null,
                recoveryLease = null,
                failure = failure
            )
        )
        upsertMigrationRecord(connection, blocked)
        blocked.snapshot.copy(archiveAudit = archiveAudit(connection))
    }

    private fun persistInitialFailure(
        failure: LegacyCompatibilityUniqueMigrationFailure
    ): LegacyCompatibilityUniqueMigrationSnapshot = immediateTransaction { connection ->
        ensureLegacyCompatibilityBaseSchema(connection)
        ensureMigrationSchema(connection)
        val existing = migrationRecord(connection)
        if (existing != null) return@immediateTransaction existing.snapshot
        val record = MigrationRecord(
            snapshot = LegacyCompatibilityUniqueMigrationSnapshot(
                migrationId = migrationId,
                schemaVersion = schemaVersion,
                state = LegacyCompatibilityUniqueMigrationState.BLOCKED_MIGRATION_FAILURE,
                scanRevision = 0L,
                logicalNowEpochSeconds = initialLogicalNowEpochSeconds,
                clockRevision = 0L,
                requiredEffect = null,
                recoveryLease = null,
                lastRecoveryLeaseVersion = 0L,
                lastRecoveryFencingToken = 0L,
                archiveAudit = emptyList(),
                failure = failure
            ),
            activeGroupDigest = null,
            canonicalSagaId = null,
            operatorResolutionId = null,
            activeArchiveId = null,
            activeArchiveDigest = null
        )
        upsertMigrationRecord(connection, record)
        record.snapshot
    }

    private fun ephemeralPreflightSnapshot(): LegacyCompatibilityUniqueMigrationSnapshot {
        val groups = inspectOperatorGroups()
        return if (groups.isNotEmpty()) {
            ephemeralBlockedSnapshot(groups)
        } else {
            LegacyCompatibilityUniqueMigrationSnapshot(
                migrationId = migrationId,
                schemaVersion = schemaVersion,
                state = LegacyCompatibilityUniqueMigrationState.STARTUP_PREFLIGHT,
                scanRevision = 0L,
                logicalNowEpochSeconds = initialLogicalNowEpochSeconds,
                clockRevision = 0L,
                requiredEffect = effectReference(
                    LegacyCompatibilityUniqueMigrationEffectCheckpoint.SCAN_DUPLICATES,
                    revision = 1L,
                    fencingToken = 1L,
                    scanRevision = 0L
                ),
                recoveryLease = null,
                lastRecoveryLeaseVersion = 0L,
                lastRecoveryFencingToken = 0L,
                archiveAudit = emptyList(),
                failure = null
            )
        }
    }

    private fun ephemeralBlockedSnapshot(
        groups: List<LegacyCompatibilityUniqueMigrationDuplicateGroup>
    ): LegacyCompatibilityUniqueMigrationSnapshot {
        lastObservedGroups = groups
        return LegacyCompatibilityUniqueMigrationSnapshot(
            migrationId = migrationId,
            schemaVersion = schemaVersion,
            state = LegacyCompatibilityUniqueMigrationState.BLOCKED_DUPLICATES,
            scanRevision = 1L,
            logicalNowEpochSeconds = initialLogicalNowEpochSeconds,
            clockRevision = 0L,
            requiredEffect = null,
            recoveryLease = null,
            lastRecoveryLeaseVersion = 0L,
            lastRecoveryFencingToken = 0L,
            archiveAudit = emptyList(),
            failure = null
        )
    }

    private fun scanDuplicateGroups(
        connection: Connection
    ): List<LegacyCompatibilityUniqueMigrationDuplicateGroup> {
        if (!tableExists(connection, SAGA_TABLE)) return emptyList()
        val requestKeys = connection.createStatement().use { statement ->
            statement.executeQuery(
                "SELECT request_key FROM $SAGA_TABLE GROUP BY request_key " +
                    "HAVING COUNT(*) > 1 ORDER BY request_key"
            ).use { rows -> buildList { while (rows.next()) add(rows.getString(1)) } }
        }
        return requestKeys.map { requestKey -> duplicateGroup(connection, requestKey) }
    }

    private fun duplicateGroup(
        connection: Connection,
        requestKey: String
    ): LegacyCompatibilityUniqueMigrationDuplicateGroup {
        val rows = connection.prepareStatement(
            "SELECT * FROM $SAGA_TABLE WHERE request_key = ? ORDER BY saga_id"
        ).use { statement ->
            statement.setString(1, requestKey)
            statement.executeQuery().use { result ->
                buildList {
                    while (result.next()) {
                        val logicalNow = result.getLong("logical_now_epoch_seconds")
                        val leaseExpiry = result.nullableLong("lease_expires_at_epoch_seconds")
                        val activeLease = result.getString("lease_id") != null &&
                            leaseExpiry != null && logicalNow < leaseExpiry
                        add(
                            DuplicateSagaRow(
                                sagaId = result.getString("saga_id"),
                                businessIdentityDigest = businessIdentityDigest(result),
                                quiescent = result.getString("state") in QUIESCENT_SAGA_STATES,
                                activeLease = activeLease
                            )
                        )
                    }
                }
            }
        }
        val identityDigests = rows.map { it.businessIdentityDigest }
        return LegacyCompatibilityUniqueMigrationDuplicateGroup(
            groupDigest = opaqueCompatibilityDigest(
                listOf("legacy-compatibility-request-key-duplicate", "v1", requestKey)
            ),
            rowCount = rows.size,
            sagaIds = rows.map { it.sagaId },
            businessIdentityDigests = identityDigests,
            activeLeaseCount = rows.count { it.activeLease },
            quiescent = rows.all { it.quiescent },
            divergent = identityDigests.toSet().size != 1
        )
    }

    private fun businessIdentityDigest(row: ResultSet): String = opaqueCompatibilityDigest(
        listOf(
            "legacy-compatibility-business-identity",
            "v1",
            row.getString("operation"),
            row.getString("client_generation"),
            row.getString("authenticated_user_id"),
            row.getString("platform"),
            row.getString("legacy_primary_key_fingerprint"),
            row.getString("legacy_installation_id"),
            row.getString("legacy_registration_id"),
            row.getString("target_installation_id"),
            row.getString("target_registration_id"),
            row.getString("token_fingerprint"),
            row.getLong("compatibility_generation").toString(),
            row.getInt("max_attempts_per_store").toString(),
            row.getString("scope_environment"),
            row.getString("scope_topic")
        )
    )

    private fun querySingleRowPayload(connection: Connection, sagaId: String): ByteArray =
        connection.prepareStatement("SELECT * FROM $SAGA_TABLE WHERE saga_id = ?").use { statement ->
            statement.setString(1, sagaId)
            statement.executeQuery().use { row ->
                check(row.next()) { "Saga selected for archive disappeared" }
                val metadata = row.metaData
                buildString {
                    for (column in 1..metadata.columnCount) {
                        if (column > 1) append('\u001f')
                        append(metadata.getColumnName(column)).append('=')
                        when (val value = row.getObject(column)) {
                            null -> append("<null>")
                            is ByteArray -> append(Base64.getEncoder().encodeToString(value))
                            else -> append(value.toString())
                        }
                    }
                }.toByteArray(StandardCharsets.UTF_8)
            }
        }

    private fun queryLiveEffects(
        connection: Connection,
        sagaId: String
    ): List<LegacyCompatibilityUniqueMigrationArchivedEffect> =
        connection.prepareStatement(
            """
            SELECT saga_id, effect_id, effect_checkpoint, checkpoint_revision, fencing_token
            FROM legacy_notification_compatibility_effect_history
            WHERE saga_id = ?
            ORDER BY sequence_id
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, sagaId)
            statement.executeQuery().use { rows ->
                buildList {
                    while (rows.next()) {
                        add(
                            LegacyCompatibilityUniqueMigrationArchivedEffect(
                                sagaId = rows.getString("saga_id"),
                                effectId = rows.getString("effect_id"),
                                checkpoint = rows.getString("effect_checkpoint"),
                                checkpointRevision = rows.getLong("checkpoint_revision"),
                                fencingToken = rows.getLong("fencing_token")
                            )
                        )
                    }
                }
            }
        }

    private fun archiveAudit(
        connection: Connection
    ): List<LegacyCompatibilityUniqueMigrationArchiveAudit> {
        if (!tableExists(connection, ARCHIVE_AUDIT_TABLE)) return emptyList()
        return connection.prepareStatement(
            """
            SELECT archive_id, archive_digest, group_digest, canonical_saga_id,
                   archived_row_count, scan_revision, operator_resolution_id
            FROM $ARCHIVE_AUDIT_TABLE
            WHERE migration_id = ?
            ORDER BY rowid
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, migrationId)
            statement.executeQuery().use { rows ->
                buildList { while (rows.next()) add(rows.toArchiveAudit()) }
            }
        }
    }

    private fun archiveAuditByResolution(
        connection: Connection,
        operatorResolutionId: String
    ): LegacyCompatibilityUniqueMigrationArchiveAudit? {
        if (!tableExists(connection, ARCHIVE_AUDIT_TABLE)) return null
        return connection.prepareStatement(
            """
            SELECT archive_id, archive_digest, group_digest, canonical_saga_id,
                   archived_row_count, scan_revision, operator_resolution_id
            FROM $ARCHIVE_AUDIT_TABLE
            WHERE migration_id = ? AND operator_resolution_id = ?
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, migrationId)
            statement.setString(2, operatorResolutionId)
            statement.executeQuery().use { rows -> if (rows.next()) rows.toArchiveAudit() else null }
        }
    }

    private fun archiveBundle(
        connection: Connection,
        archiveId: String
    ): LegacyCompatibilityUniqueMigrationArchiveBundle? {
        if (!tableExists(connection, ARCHIVE_AUDIT_TABLE)) return null
        val audit = connection.prepareStatement(
            """
            SELECT archive_id, archive_digest, group_digest, canonical_saga_id,
                   archived_row_count, scan_revision, operator_resolution_id
            FROM $ARCHIVE_AUDIT_TABLE
            WHERE migration_id = ? AND archive_id = ?
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, migrationId)
            statement.setString(2, archiveId)
            statement.executeQuery().use { rows -> if (rows.next()) rows.toArchiveAudit() else null }
        } ?: return null
        val rows = connection.prepareStatement(
            "SELECT saga_id, payload_digest FROM $ARCHIVE_ROW_TABLE " +
                "WHERE archive_id = ? ORDER BY saga_id"
        ).use { statement ->
            statement.setString(1, archiveId)
            statement.executeQuery().use { result ->
                buildList {
                    while (result.next()) {
                        add(
                            LegacyCompatibilityUniqueMigrationArchivedRow(
                                sagaId = result.getString("saga_id"),
                                payloadDigest = result.getString("payload_digest")
                            )
                        )
                    }
                }
            }
        }
        val effects = connection.prepareStatement(
            """
            SELECT saga_id, effect_id, effect_checkpoint, checkpoint_revision, fencing_token
            FROM $ARCHIVE_EFFECT_TABLE WHERE archive_id = ?
            ORDER BY saga_id, effect_id, fencing_token
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, archiveId)
            statement.executeQuery().use { result ->
                buildList {
                    while (result.next()) {
                        add(
                            LegacyCompatibilityUniqueMigrationArchivedEffect(
                                sagaId = result.getString("saga_id"),
                                effectId = result.getString("effect_id"),
                                checkpoint = result.getString("effect_checkpoint"),
                                checkpointRevision = result.getLong("checkpoint_revision"),
                                fencingToken = result.getLong("fencing_token")
                            )
                        )
                    }
                }
            }
        }
        return LegacyCompatibilityUniqueMigrationArchiveBundle(audit, rows, effects)
    }

    private fun ResultSet.toArchiveAudit() = LegacyCompatibilityUniqueMigrationArchiveAudit(
        archiveId = getString("archive_id"),
        archiveDigest = getString("archive_digest"),
        groupDigest = getString("group_digest"),
        canonicalSagaId = getString("canonical_saga_id"),
        archivedRowCount = getInt("archived_row_count"),
        scanRevision = getLong("scan_revision"),
        operatorResolutionId = getString("operator_resolution_id")
    )

    private fun migrationRecord(connection: Connection): MigrationRecord? {
        if (!tableExists(connection, MIGRATION_STATE_TABLE)) return null
        return connection.prepareStatement(
            "SELECT * FROM $MIGRATION_STATE_TABLE WHERE migration_id = ?"
        ).use { statement ->
            statement.setString(1, migrationId)
            statement.executeQuery().use { rows ->
                if (!rows.next()) return@use null
                val effectId = rows.getString("required_effect_id")
                val effect = effectId?.let {
                    LegacyCompatibilityUniqueMigrationEffectReference(
                        effectId = it,
                        checkpoint = LegacyCompatibilityUniqueMigrationEffectCheckpoint.valueOf(
                            rows.getString("required_effect_checkpoint")
                        ),
                        checkpointRevision = rows.getLong("required_effect_revision"),
                        fencingToken = rows.getLong("required_effect_fencing")
                    )
                }
                val leaseId = rows.getString("lease_id")
                val lease = leaseId?.let {
                    val required = checkNotNull(effect)
                    LegacyCompatibilityUniqueMigrationRecoveryLease(
                        migrationId = migrationId,
                        leaseId = it,
                        holderId = rows.getString("lease_holder_id"),
                        leaseVersion = rows.getLong("lease_version"),
                        fencingToken = rows.getLong("lease_fencing_token"),
                        expiresAtLogicalEpochSeconds = rows.getLong("lease_expires_at"),
                        acquiredAtClockRevision = rows.getLong("lease_acquired_clock_revision"),
                        expectedEffectId = required.effectId,
                        effectCheckpoint = required.checkpoint,
                        checkpointRevision = required.checkpointRevision,
                        effectEmitted = rows.getInt("lease_effect_emitted") == 1
                    )
                }
                MigrationRecord(
                    snapshot = LegacyCompatibilityUniqueMigrationSnapshot(
                        migrationId = migrationId,
                        schemaVersion = rows.getInt("schema_version"),
                        state = LegacyCompatibilityUniqueMigrationState.valueOf(
                            rows.getString("state")
                        ),
                        scanRevision = rows.getLong("scan_revision"),
                        logicalNowEpochSeconds = rows.getLong("logical_now_epoch_seconds"),
                        clockRevision = rows.getLong("clock_revision"),
                        requiredEffect = effect,
                        recoveryLease = lease,
                        lastRecoveryLeaseVersion = rows.getLong("last_lease_version"),
                        lastRecoveryFencingToken = rows.getLong("last_fencing_token"),
                        archiveAudit = archiveAudit(connection),
                        failure = rows.getString("failure")
                            ?.let(LegacyCompatibilityUniqueMigrationFailure::valueOf)
                    ),
                    activeGroupDigest = rows.getString("active_group_digest"),
                    canonicalSagaId = rows.getString("canonical_saga_id"),
                    operatorResolutionId = rows.getString("operator_resolution_id"),
                    activeArchiveId = rows.getString("active_archive_id"),
                    activeArchiveDigest = rows.getString("active_archive_digest")
                )
            }
        }
    }

    private fun upsertMigrationRecord(connection: Connection, record: MigrationRecord) {
        val snapshot = record.snapshot
        val effect = snapshot.requiredEffect
        val lease = snapshot.recoveryLease
        connection.prepareStatement(
            """
            INSERT INTO $MIGRATION_STATE_TABLE(
                migration_id, schema_version, state, scan_revision,
                logical_now_epoch_seconds, clock_revision,
                required_effect_id, required_effect_checkpoint,
                required_effect_revision, required_effect_fencing,
                last_lease_version, last_fencing_token,
                lease_id, lease_holder_id, lease_version, lease_fencing_token,
                lease_expires_at, lease_acquired_clock_revision, lease_effect_emitted,
                failure, active_group_digest, canonical_saga_id,
                operator_resolution_id, active_archive_id, active_archive_digest
            ) VALUES (
                ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
            ) ON CONFLICT(migration_id) DO UPDATE SET
                schema_version = excluded.schema_version,
                state = excluded.state,
                scan_revision = excluded.scan_revision,
                logical_now_epoch_seconds = excluded.logical_now_epoch_seconds,
                clock_revision = excluded.clock_revision,
                required_effect_id = excluded.required_effect_id,
                required_effect_checkpoint = excluded.required_effect_checkpoint,
                required_effect_revision = excluded.required_effect_revision,
                required_effect_fencing = excluded.required_effect_fencing,
                last_lease_version = excluded.last_lease_version,
                last_fencing_token = excluded.last_fencing_token,
                lease_id = excluded.lease_id,
                lease_holder_id = excluded.lease_holder_id,
                lease_version = excluded.lease_version,
                lease_fencing_token = excluded.lease_fencing_token,
                lease_expires_at = excluded.lease_expires_at,
                lease_acquired_clock_revision = excluded.lease_acquired_clock_revision,
                lease_effect_emitted = excluded.lease_effect_emitted,
                failure = excluded.failure,
                active_group_digest = excluded.active_group_digest,
                canonical_saga_id = excluded.canonical_saga_id,
                operator_resolution_id = excluded.operator_resolution_id,
                active_archive_id = excluded.active_archive_id,
                active_archive_digest = excluded.active_archive_digest
            """.trimIndent()
        ).use { statement ->
            var index = 1
            statement.setString(index++, migrationId)
            statement.setInt(index++, snapshot.schemaVersion)
            statement.setString(index++, snapshot.state.name)
            statement.setLong(index++, snapshot.scanRevision)
            statement.setLong(index++, snapshot.logicalNowEpochSeconds)
            statement.setLong(index++, snapshot.clockRevision)
            statement.setNullableString(index++, effect?.effectId)
            statement.setNullableString(index++, effect?.checkpoint?.name)
            statement.setNullableLong(index++, effect?.checkpointRevision)
            statement.setNullableLong(index++, effect?.fencingToken)
            statement.setLong(index++, snapshot.lastRecoveryLeaseVersion)
            statement.setLong(index++, snapshot.lastRecoveryFencingToken)
            statement.setNullableString(index++, lease?.leaseId)
            statement.setNullableString(index++, lease?.holderId)
            statement.setNullableLong(index++, lease?.leaseVersion)
            statement.setNullableLong(index++, lease?.fencingToken)
            statement.setNullableLong(index++, lease?.expiresAtLogicalEpochSeconds)
            statement.setNullableLong(index++, lease?.acquiredAtClockRevision)
            if (lease == null) statement.setNull(index++, Types.INTEGER)
            else statement.setInt(index++, if (lease.effectEmitted) 1 else 0)
            statement.setNullableString(index++, snapshot.failure?.name)
            statement.setNullableString(index++, record.activeGroupDigest)
            statement.setNullableString(index++, record.canonicalSagaId)
            statement.setNullableString(index++, record.operatorResolutionId)
            statement.setNullableString(index++, record.activeArchiveId)
            statement.setNullableString(index, record.activeArchiveDigest)
            statement.executeUpdate()
        }
    }

    private fun MigrationRecord.withNextEffect(
        state: LegacyCompatibilityUniqueMigrationState,
        checkpoint: LegacyCompatibilityUniqueMigrationEffectCheckpoint,
        activeArchiveId: String? = this.activeArchiveId,
        activeArchiveDigest: String? = this.activeArchiveDigest
    ): MigrationRecord {
        val current = snapshot
        val revision = (current.requiredEffect?.checkpointRevision ?: 0L) + 1L
        val fence = max(
            current.requiredEffect?.fencingToken ?: 0L,
            current.lastRecoveryFencingToken
        ) + 1L
        return copy(
            snapshot = current.copy(
                state = state,
                requiredEffect = effectReference(
                    checkpoint = checkpoint,
                    revision = revision,
                    fencingToken = fence,
                    scanRevision = current.scanRevision,
                    groupDigest = activeGroupDigest,
                    operatorResolutionId = operatorResolutionId
                ),
                recoveryLease = null,
                failure = null
            ),
            activeArchiveId = activeArchiveId,
            activeArchiveDigest = activeArchiveDigest
        )
    }

    private fun ackAuthorityIsCurrent(
        snapshot: LegacyCompatibilityUniqueMigrationSnapshot,
        reference: LegacyCompatibilityUniqueMigrationEffectReference
    ): Boolean {
        val lease = snapshot.recoveryLease ?: return true
        return lease.effectEmitted &&
            lease.fencingToken == reference.fencingToken &&
            snapshot.logicalNowEpochSeconds < lease.expiresAtLogicalEpochSeconds
    }

    private fun effectReference(
        checkpoint: LegacyCompatibilityUniqueMigrationEffectCheckpoint,
        revision: Long,
        fencingToken: Long,
        scanRevision: Long,
        groupDigest: String? = null,
        operatorResolutionId: String? = null
    ) = LegacyCompatibilityUniqueMigrationEffectReference(
        effectId = opaqueCompatibilityDigest(
            listOf(
                "legacy-compatibility-request-key-unique-migration-effect",
                "v1",
                migrationId,
                schemaVersion.toString(),
                checkpoint.name,
                revision.toString(),
                scanRevision.toString(),
                groupDigest,
                operatorResolutionId
            )
        ),
        checkpoint = checkpoint,
        checkpointRevision = revision,
        fencingToken = fencingToken
    )

    private fun failureFor(
        checkpoint: LegacyCompatibilityUniqueMigrationEffectCheckpoint
    ): LegacyCompatibilityUniqueMigrationFailure = when (checkpoint) {
        LegacyCompatibilityUniqueMigrationEffectCheckpoint.SCAN_DUPLICATES ->
            LegacyCompatibilityUniqueMigrationFailure.PREFLIGHT_UNAVAILABLE
        LegacyCompatibilityUniqueMigrationEffectCheckpoint.INSTALL_UNIQUE_REQUEST_KEY_INDEX ->
            LegacyCompatibilityUniqueMigrationFailure.DDL_UNAVAILABLE
        LegacyCompatibilityUniqueMigrationEffectCheckpoint.ARCHIVE_NONCANONICAL_ROWS_AND_EFFECTS ->
            LegacyCompatibilityUniqueMigrationFailure.ARCHIVE_UNAVAILABLE
        LegacyCompatibilityUniqueMigrationEffectCheckpoint.DELETE_ARCHIVED_NONCANONICAL_ROWS ->
            LegacyCompatibilityUniqueMigrationFailure.DELETE_UNAVAILABLE
    }

    private fun hasUniqueRequestKeyIndex(connection: Connection): Boolean {
        if (!tableExists(connection, SAGA_TABLE)) return false
        connection.createStatement().use { statement ->
            statement.executeQuery("PRAGMA index_list('$SAGA_TABLE')").use { indexes ->
                while (indexes.next()) {
                    if (indexes.getInt("unique") != 1) continue
                    val name = indexes.getString("name").replace("'", "''")
                    val columns = connection.createStatement().use { columnStatement ->
                        columnStatement.executeQuery("PRAGMA index_info('$name')").use { rows ->
                            buildList { while (rows.next()) add(rows.getString("name")) }
                        }
                    }
                    if (columns == listOf("request_key")) return true
                }
            }
        }
        return false
    }

    private fun ensureMigrationSchema(connection: Connection) {
        connection.createStatement().use { statement ->
            statement.execute(
                """
                CREATE TABLE IF NOT EXISTS $MIGRATION_STATE_TABLE (
                    migration_id TEXT PRIMARY KEY NOT NULL,
                    schema_version INTEGER NOT NULL CHECK(schema_version > 0),
                    state TEXT NOT NULL,
                    scan_revision INTEGER NOT NULL CHECK(scan_revision >= 0),
                    logical_now_epoch_seconds INTEGER NOT NULL CHECK(logical_now_epoch_seconds >= 0),
                    clock_revision INTEGER NOT NULL CHECK(clock_revision >= 0),
                    required_effect_id TEXT,
                    required_effect_checkpoint TEXT,
                    required_effect_revision INTEGER,
                    required_effect_fencing INTEGER,
                    last_lease_version INTEGER NOT NULL CHECK(last_lease_version >= 0),
                    last_fencing_token INTEGER NOT NULL CHECK(last_fencing_token >= 0),
                    lease_id TEXT,
                    lease_holder_id TEXT,
                    lease_version INTEGER,
                    lease_fencing_token INTEGER,
                    lease_expires_at INTEGER,
                    lease_acquired_clock_revision INTEGER,
                    lease_effect_emitted INTEGER CHECK(lease_effect_emitted IN (0, 1)),
                    failure TEXT,
                    active_group_digest TEXT,
                    canonical_saga_id TEXT,
                    operator_resolution_id TEXT,
                    active_archive_id TEXT,
                    active_archive_digest TEXT,
                    CHECK (
                        (required_effect_id IS NULL AND required_effect_checkpoint IS NULL
                            AND required_effect_revision IS NULL AND required_effect_fencing IS NULL)
                        OR
                        (required_effect_id IS NOT NULL AND required_effect_checkpoint IS NOT NULL
                            AND required_effect_revision IS NOT NULL AND required_effect_fencing IS NOT NULL)
                    ),
                    CHECK (
                        (lease_id IS NULL AND lease_holder_id IS NULL AND lease_version IS NULL
                            AND lease_fencing_token IS NULL AND lease_expires_at IS NULL
                            AND lease_acquired_clock_revision IS NULL AND lease_effect_emitted IS NULL)
                        OR
                        (lease_id IS NOT NULL AND lease_holder_id IS NOT NULL AND lease_version IS NOT NULL
                            AND lease_fencing_token IS NOT NULL AND lease_expires_at IS NOT NULL
                            AND lease_acquired_clock_revision IS NOT NULL AND lease_effect_emitted IS NOT NULL)
                    )
                )
                """.trimIndent()
            )
            statement.execute(
                """
                CREATE TABLE IF NOT EXISTS $ARCHIVE_AUDIT_TABLE (
                    archive_id TEXT PRIMARY KEY NOT NULL,
                    migration_id TEXT NOT NULL,
                    archive_digest TEXT NOT NULL,
                    group_digest TEXT NOT NULL,
                    canonical_saga_id TEXT NOT NULL,
                    archived_row_count INTEGER NOT NULL CHECK(archived_row_count > 0),
                    scan_revision INTEGER NOT NULL CHECK(scan_revision > 0),
                    operator_resolution_id TEXT NOT NULL,
                    UNIQUE(migration_id, operator_resolution_id)
                )
                """.trimIndent()
            )
            statement.execute(
                """
                CREATE TABLE IF NOT EXISTS $ARCHIVE_ROW_TABLE (
                    archive_id TEXT NOT NULL,
                    saga_id TEXT NOT NULL,
                    row_payload BLOB NOT NULL,
                    payload_digest TEXT NOT NULL,
                    PRIMARY KEY(archive_id, saga_id),
                    FOREIGN KEY(archive_id) REFERENCES $ARCHIVE_AUDIT_TABLE(archive_id)
                        ON DELETE RESTRICT
                )
                """.trimIndent()
            )
            statement.execute(
                """
                CREATE TABLE IF NOT EXISTS $ARCHIVE_EFFECT_TABLE (
                    archive_id TEXT NOT NULL,
                    saga_id TEXT NOT NULL,
                    effect_id TEXT NOT NULL,
                    effect_checkpoint TEXT NOT NULL,
                    checkpoint_revision INTEGER NOT NULL,
                    fencing_token INTEGER NOT NULL,
                    PRIMARY KEY(archive_id, saga_id, effect_id, fencing_token),
                    FOREIGN KEY(archive_id) REFERENCES $ARCHIVE_AUDIT_TABLE(archive_id)
                        ON DELETE RESTRICT
                )
                """.trimIndent()
            )
        }
    }

    private fun tableExists(connection: Connection, table: String): Boolean =
        connection.prepareStatement(
            "SELECT 1 FROM sqlite_schema WHERE type = 'table' AND name = ?"
        ).use { statement ->
            statement.setString(1, table)
            statement.executeQuery().use(ResultSet::next)
        }

    private fun <T> read(block: (Connection) -> T): T {
        ensureOpen()
        return connection().use(block)
    }

    private fun <T> immediateTransaction(block: (Connection) -> T): T {
        ensureOpen()
        val connection = connection()
        try {
            connection.createStatement().use { it.execute("BEGIN IMMEDIATE") }
            return try {
                val result = block(connection)
                connection.createStatement().use { it.execute("COMMIT") }
                result
            } catch (failure: Throwable) {
                runCatching { connection.createStatement().use { it.execute("ROLLBACK") } }
                throw failure
            }
        } finally {
            connection.close()
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
        check(!closed.get()) { "Legacy compatibility unique migration is closed" }
    }

    private data class MigrationRecord(
        val snapshot: LegacyCompatibilityUniqueMigrationSnapshot,
        val activeGroupDigest: String?,
        val canonicalSagaId: String?,
        val operatorResolutionId: String?,
        val activeArchiveId: String?,
        val activeArchiveDigest: String?
    )

    private data class DuplicateSagaRow(
        val sagaId: String,
        val businessIdentityDigest: String,
        val quiescent: Boolean,
        val activeLease: Boolean
    )

    private data class ReadOnlyPreflight(
        val groups: List<LegacyCompatibilityUniqueMigrationDuplicateGroup>,
        val uniqueRequestKeyIndexPresent: Boolean
    )

    private companion object {
        const val MAX_EFFECTS_PER_RUN = 8
        const val INTERNAL_LEASE_DURATION_SECONDS = 30L
        const val SAGA_TABLE = "legacy_notification_compatibility_saga"
        const val UNIQUE_REQUEST_KEY_INDEX =
            "legacy_notification_compatibility_saga_request_key_unique"
        const val MIGRATION_STATE_TABLE = "legacy_compatibility_unique_migration_state"
        const val ARCHIVE_AUDIT_TABLE = "legacy_compatibility_unique_migration_archive"
        const val ARCHIVE_ROW_TABLE = "legacy_compatibility_unique_migration_archive_row"
        const val ARCHIVE_EFFECT_TABLE = "legacy_compatibility_unique_migration_archive_effect"
        val QUIESCENT_SAGA_STATES = setOf("CONVERGED", "BLOCKED")
    }
}

internal fun ensureLegacyCompatibilityBaseSchema(connection: Connection) {
    connection.createStatement().use { statement ->
        statement.execute(
            """
            CREATE TABLE IF NOT EXISTS legacy_notification_compatibility_saga (
                saga_id TEXT PRIMARY KEY NOT NULL,
                request_key TEXT NOT NULL,
                operation TEXT NOT NULL CHECK(operation IN ('REGISTER', 'UNREGISTER')),
                client_generation TEXT NOT NULL CHECK(client_generation IN ('N', 'N_MINUS_1')),
                authenticated_user_id TEXT NOT NULL,
                platform TEXT NOT NULL CHECK(platform = 'IOS'),
                legacy_primary_key_fingerprint TEXT,
                legacy_installation_id TEXT,
                legacy_registration_id TEXT,
                target_installation_id TEXT NOT NULL,
                target_registration_id TEXT,
                token_fingerprint TEXT,
                compatibility_generation INTEGER NOT NULL CHECK(compatibility_generation > 0),
                max_attempts_per_store INTEGER NOT NULL CHECK(max_attempts_per_store > 0),
                scope_environment TEXT NOT NULL,
                scope_topic TEXT NOT NULL,
                state TEXT NOT NULL,
                reconciliation_status TEXT NOT NULL,
                response_disposition TEXT NOT NULL,
                legacy_write_status TEXT NOT NULL,
                v2_write_status TEXT NOT NULL,
                v2_target_kind TEXT NOT NULL,
                legacy_attempt INTEGER NOT NULL CHECK(legacy_attempt >= 0),
                v2_attempt INTEGER NOT NULL CHECK(v2_attempt >= 0),
                next_retry_at_epoch_seconds INTEGER,
                checkpoint_revision INTEGER NOT NULL CHECK(checkpoint_revision > 0),
                logical_now_epoch_seconds INTEGER NOT NULL CHECK(logical_now_epoch_seconds >= 0),
                clock_revision INTEGER NOT NULL CHECK(clock_revision >= 0),
                required_effect_id TEXT,
                required_effect_checkpoint TEXT,
                required_effect_revision INTEGER,
                required_effect_fencing INTEGER,
                resume_checkpoint TEXT,
                lease_id TEXT,
                lease_holder_id TEXT,
                lease_version INTEGER,
                lease_fencing_token INTEGER,
                lease_expires_at_epoch_seconds INTEGER,
                lease_effect_emitted INTEGER CHECK(lease_effect_emitted IN (0, 1)),
                last_failure TEXT,
                token_ciphertext BLOB,
                CHECK (
                    (required_effect_id IS NULL AND required_effect_checkpoint IS NULL
                        AND required_effect_revision IS NULL AND required_effect_fencing IS NULL)
                    OR
                    (required_effect_id IS NOT NULL AND required_effect_checkpoint IS NOT NULL
                        AND required_effect_revision IS NOT NULL AND required_effect_fencing IS NOT NULL)
                ),
                CHECK (
                    (lease_id IS NULL AND lease_holder_id IS NULL AND lease_version IS NULL
                        AND lease_fencing_token IS NULL AND lease_expires_at_epoch_seconds IS NULL
                        AND lease_effect_emitted IS NULL)
                    OR
                    (lease_id IS NOT NULL AND lease_holder_id IS NOT NULL AND lease_version IS NOT NULL
                        AND lease_fencing_token IS NOT NULL AND lease_expires_at_epoch_seconds IS NOT NULL
                        AND lease_effect_emitted IS NOT NULL)
                )
            )
            """.trimIndent()
        )
        statement.execute(
            """
            CREATE TABLE IF NOT EXISTS legacy_notification_compatibility_effect_history (
                sequence_id INTEGER PRIMARY KEY AUTOINCREMENT,
                saga_id TEXT NOT NULL,
                effect_id TEXT NOT NULL,
                effect_checkpoint TEXT NOT NULL,
                checkpoint_revision INTEGER NOT NULL,
                fencing_token INTEGER NOT NULL,
                UNIQUE(saga_id, effect_id, fencing_token),
                FOREIGN KEY(saga_id)
                    REFERENCES legacy_notification_compatibility_saga(saga_id)
                    ON DELETE RESTRICT
            )
            """.trimIndent()
        )
        statement.execute(
            """
            CREATE TABLE IF NOT EXISTS legacy_notification_compatibility_generation (
                subject_key TEXT NOT NULL,
                target_key TEXT NOT NULL,
                desired_key TEXT NOT NULL,
                compatibility_generation INTEGER NOT NULL CHECK(compatibility_generation > 0),
                PRIMARY KEY(subject_key, target_key)
            )
            """.trimIndent()
        )
    }
}

private fun digestBytes(value: ByteArray): String =
    Base64.getUrlEncoder().withoutPadding().encodeToString(
        MessageDigest.getInstance("SHA-256").digest(value)
    )

private fun java.sql.PreparedStatement.setNullableString(index: Int, value: String?) {
    if (value == null) setNull(index, Types.VARCHAR) else setString(index, value)
}

private fun java.sql.PreparedStatement.setNullableLong(index: Int, value: Long?) {
    if (value == null) setNull(index, Types.INTEGER) else setLong(index, value)
}

private fun ResultSet.nullableLong(column: String): Long? =
    getLong(column).takeUnless { wasNull() }
