package com.guyghost.wakeve.notification

const val DEFAULT_COMPATIBILITY_UNIQUE_MIGRATION_ID =
    "legacy-compatibility-request-key-unique-v1"
const val DEFAULT_COMPATIBILITY_UNIQUE_MIGRATION_SCHEMA_VERSION = 1

enum class LegacyCompatibilityUniqueMigrationState {
    STARTUP_PREFLIGHT,
    INSTALLING_UNIQUE_INDEX,
    BLOCKED_DUPLICATES,
    ARCHIVING_NONCANONICAL_ROWS,
    DELETING_ARCHIVED_ROWS,
    BLOCKED_MIGRATION_FAILURE,
    READY
}

enum class LegacyCompatibilityRuntimeSurface {
    SAGA_STORE,
    NOTIFICATION_ROUTES,
    RECOVERY_SCHEDULER
}

enum class LegacyCompatibilityUniqueMigrationEffectCheckpoint {
    SCAN_DUPLICATES,
    INSTALL_UNIQUE_REQUEST_KEY_INDEX,
    ARCHIVE_NONCANONICAL_ROWS_AND_EFFECTS,
    DELETE_ARCHIVED_NONCANONICAL_ROWS
}

enum class LegacyCompatibilityUniqueMigrationCheckpointPhase {
    BEFORE_EFFECT,
    AFTER_COMMIT
}

enum class LegacyCompatibilityUniqueMigrationFailure {
    PREFLIGHT_UNAVAILABLE,
    ARCHIVE_UNAVAILABLE,
    DELETE_UNAVAILABLE,
    DDL_UNAVAILABLE,
    CHECKPOINT_CONFLICT
}

fun interface LegacyCompatibilityUniqueMigrationFaultInjector {
    fun evaluate(
        checkpoint: LegacyCompatibilityUniqueMigrationEffectCheckpoint,
        phase: LegacyCompatibilityUniqueMigrationCheckpointPhase
    ): LegacyCompatibilityUniqueMigrationFailure?
}

internal object NoOpLegacyCompatibilityUniqueMigrationFaultInjector :
    LegacyCompatibilityUniqueMigrationFaultInjector {
    override fun evaluate(
        checkpoint: LegacyCompatibilityUniqueMigrationEffectCheckpoint,
        phase: LegacyCompatibilityUniqueMigrationCheckpointPhase
    ): LegacyCompatibilityUniqueMigrationFailure? = null
}

data class LegacyCompatibilityUniqueMigrationPreflightObservation(
    val duplicateGroupCount: Int,
    val duplicateRowCount: Int,
    val uniqueRequestKeyIndexPresent: Boolean
)

fun interface LegacyCompatibilityUniqueMigrationReadOnlyPreflightProbe {
    fun afterObservation(observation: LegacyCompatibilityUniqueMigrationPreflightObservation)
}

internal object NoOpLegacyCompatibilityUniqueMigrationReadOnlyPreflightProbe :
    LegacyCompatibilityUniqueMigrationReadOnlyPreflightProbe {
    override fun afterObservation(
        observation: LegacyCompatibilityUniqueMigrationPreflightObservation
    ) = Unit
}

data class LegacyCompatibilityUniqueMigrationEffectReference(
    val effectId: String,
    val checkpoint: LegacyCompatibilityUniqueMigrationEffectCheckpoint,
    val checkpointRevision: Long,
    val fencingToken: Long
)

data class LegacyCompatibilityUniqueMigrationArchiveAudit(
    val archiveId: String,
    val archiveDigest: String,
    val groupDigest: String,
    val canonicalSagaId: String,
    val archivedRowCount: Int,
    val scanRevision: Long,
    val operatorResolutionId: String
)

data class LegacyCompatibilityUniqueMigrationArchivedRow(
    val sagaId: String,
    val payloadDigest: String
)

data class LegacyCompatibilityUniqueMigrationArchivedEffect(
    val sagaId: String,
    val effectId: String,
    val checkpoint: String,
    val checkpointRevision: Long,
    val fencingToken: Long
)

data class LegacyCompatibilityUniqueMigrationArchiveBundle(
    val audit: LegacyCompatibilityUniqueMigrationArchiveAudit,
    val rows: List<LegacyCompatibilityUniqueMigrationArchivedRow>,
    val effects: List<LegacyCompatibilityUniqueMigrationArchivedEffect>
)

data class LegacyCompatibilityUniqueMigrationDuplicateGroup(
    val groupDigest: String,
    val rowCount: Int,
    val sagaIds: List<String>,
    val businessIdentityDigests: List<String>,
    val activeLeaseCount: Int,
    val quiescent: Boolean,
    val divergent: Boolean
) {
    val operatorResolvable: Boolean
        get() = quiescent && !divergent && activeLeaseCount == 0 &&
            businessIdentityDigests.toSet().size == 1
}

data class LegacyCompatibilityUniqueMigrationDiagnostic(
    val migrationId: String,
    val schemaVersion: Int,
    val scanRevision: Long,
    val duplicateGroupCount: Int,
    val duplicateRowCount: Int,
    val groupDigests: List<String>,
    val failure: LegacyCompatibilityUniqueMigrationFailure?
)

data class LegacyCompatibilityUniqueMigrationSnapshot(
    val migrationId: String,
    val schemaVersion: Int,
    val state: LegacyCompatibilityUniqueMigrationState,
    val scanRevision: Long,
    val logicalNowEpochSeconds: Long,
    val clockRevision: Long,
    val requiredEffect: LegacyCompatibilityUniqueMigrationEffectReference?,
    val recoveryLease: LegacyCompatibilityUniqueMigrationRecoveryLease?,
    val lastRecoveryLeaseVersion: Long,
    val lastRecoveryFencingToken: Long,
    val archiveAudit: List<LegacyCompatibilityUniqueMigrationArchiveAudit>,
    val failure: LegacyCompatibilityUniqueMigrationFailure?
) {
    val runtimeReady: Boolean
        get() = state == LegacyCompatibilityUniqueMigrationState.READY

    fun runtimeReadyFor(surface: LegacyCompatibilityRuntimeSurface): Boolean {
        @Suppress("UNUSED_VARIABLE")
        val typedSurface = surface
        return runtimeReady
    }
}

data class LegacyCompatibilityUniqueMigrationOperatorResolutionRequest(
    val migrationId: String,
    val scanRevision: Long,
    val groupDigest: String,
    val canonicalSagaId: String,
    val operatorResolutionId: String
)

data class LegacyCompatibilityUniqueMigrationRecoveryLeaseRequest(
    val migrationId: String,
    val expectedEffectId: String,
    val effectCheckpoint: LegacyCompatibilityUniqueMigrationEffectCheckpoint,
    val checkpointRevision: Long,
    val holderId: String,
    val expectedLeaseVersion: Long,
    val newLeaseVersion: Long,
    val fencingToken: Long,
    val expiresAtLogicalEpochSeconds: Long
)

data class LegacyCompatibilityUniqueMigrationRecoveryRequest(
    val migrationId: String,
    val leaseId: String,
    val holderId: String,
    val leaseVersion: Long,
    val fencingToken: Long,
    val expectedEffectId: String,
    val effectCheckpoint: LegacyCompatibilityUniqueMigrationEffectCheckpoint,
    val checkpointRevision: Long
)

data class LegacyCompatibilityUniqueMigrationRecoveryLease(
    val migrationId: String,
    val leaseId: String,
    val holderId: String,
    val leaseVersion: Long,
    val fencingToken: Long,
    val expiresAtLogicalEpochSeconds: Long,
    val acquiredAtClockRevision: Long,
    val expectedEffectId: String,
    val effectCheckpoint: LegacyCompatibilityUniqueMigrationEffectCheckpoint,
    val checkpointRevision: Long,
    val effectEmitted: Boolean
) {
    fun asRecoveryRequest(): LegacyCompatibilityUniqueMigrationRecoveryRequest =
        LegacyCompatibilityUniqueMigrationRecoveryRequest(
            migrationId = migrationId,
            leaseId = leaseId,
            holderId = holderId,
            leaseVersion = leaseVersion,
            fencingToken = fencingToken,
            expectedEffectId = expectedEffectId,
            effectCheckpoint = effectCheckpoint,
            checkpointRevision = checkpointRevision
        )
}

data class LegacyCompatibilityUniqueMigrationEffectAcknowledgement(
    val effectId: String,
    val checkpoint: LegacyCompatibilityUniqueMigrationEffectCheckpoint,
    val checkpointRevision: Long,
    val fencingToken: Long
)

class LegacyCompatibilityUniqueMigrationNotReadyException(
    val migrationState: LegacyCompatibilityUniqueMigrationState,
    duplicateGroupCount: Int,
    duplicateRowCount: Int,
    failure: LegacyCompatibilityUniqueMigrationFailure?
) : IllegalStateException(
    "Legacy compatibility migration is not ready " +
        "(state=$migrationState, duplicateGroups=$duplicateGroupCount, " +
        "duplicateRows=$duplicateRowCount, failure=$failure)"
)

interface LegacyCompatibilityUniqueMigrationRuntime : AutoCloseable {
    fun startOrResume(): LegacyCompatibilityUniqueMigrationSnapshot

    fun currentSnapshot(): LegacyCompatibilityUniqueMigrationSnapshot

    fun advanceLogicalClock(
        expectedRevision: Long,
        newEpochSeconds: Long
    ): LegacyCompatibilityUniqueMigrationSnapshot

    fun diagnostic(): LegacyCompatibilityUniqueMigrationDiagnostic

    fun inspectOperatorGroups(): List<LegacyCompatibilityUniqueMigrationDuplicateGroup>

    fun requestOperatorResolution(
        request: LegacyCompatibilityUniqueMigrationOperatorResolutionRequest
    ): LegacyCompatibilityUniqueMigrationSnapshot

    fun archiveAudit(): List<LegacyCompatibilityUniqueMigrationArchiveAudit>

    fun archiveBundle(archiveId: String): LegacyCompatibilityUniqueMigrationArchiveBundle?

    fun acquireRecoveryLease(
        request: LegacyCompatibilityUniqueMigrationRecoveryLeaseRequest
    ): LegacyCompatibilityUniqueMigrationRecoveryLease?

    fun requestRecovery(
        request: LegacyCompatibilityUniqueMigrationRecoveryRequest
    ): LegacyCompatibilityUniqueMigrationEffectReference?

    fun acknowledgeEffect(
        acknowledgement: LegacyCompatibilityUniqueMigrationEffectAcknowledgement
    ): LegacyCompatibilityUniqueMigrationSnapshot

    fun confirmExternalRepair(
        scanRevision: Long,
        repairEvidenceDigest: String
    ): LegacyCompatibilityUniqueMigrationSnapshot
}
