export type LegacyCompatibilityUniqueMigrationEffectCheckpoint =
  | 'scanDuplicates'
  | 'installUniqueRequestKeyIndex'
  | 'archiveNoncanonicalRowsAndEffects'
  | 'deleteArchivedNoncanonicalRows'

export interface LegacyCompatibilityDuplicateGroup {
  /** Digest of the duplicated request key. The raw request key is forbidden. */
  groupDigest: string
  rowCount: number
  /** Opaque saga identities used only to validate an explicit operator choice. */
  sagaIds: readonly string[]
  /** One digest per row over the reviewed business identity tuple. */
  businessIdentityDigests: readonly string[]
  activeLeaseCount: number
  quiescent: boolean
  divergent: boolean
}

export interface LegacyCompatibilityUniqueMigrationInput {
  migrationId: string
  schemaVersion: number
  /** Explicit deterministic model clock seed. Runtime wall-clock reads are forbidden. */
  initialLogicalNowEpochSeconds: number
}

export interface LegacyCompatibilityUniqueMigrationRecoveryLease {
  leaseId: string
  holderId: string
  version: number
  fencingToken: number
  expiresAtLogicalEpochSeconds: number
  acquiredAtClockRevision: number
  effectId: string
  effectCheckpoint: LegacyCompatibilityUniqueMigrationEffectCheckpoint
  checkpointRevision: number
  effectEmitted: boolean
}

export interface LegacyCompatibilityArchiveAuditEntry {
  archiveId: string
  archiveDigest: string
  groupDigest: string
  canonicalSagaId: string
  archivedRowCount: number
  scanRevision: number
  operatorResolutionId: string
}

export interface ActiveDuplicateResolution {
  groupDigest: string
  canonicalSagaId: string
  archivedSagaIds: readonly string[]
  operatorResolutionId: string
  scanRevision: number
  archiveId: string | null
  archiveDigest: string | null
}

export interface LegacyCompatibilityUniqueMigrationContext
  extends LegacyCompatibilityUniqueMigrationInput {
  scanRevision: number
  indexInstalled: boolean
  duplicateGroups: readonly LegacyCompatibilityDuplicateGroup[]
  blockingDisposition: 'none' | 'operatorResolutionAvailable' | 'externalRepairRequired'
  activeResolution: ActiveDuplicateResolution | null
  archiveAudit: readonly LegacyCompatibilityArchiveAuditEntry[]
  effectCheckpoint: LegacyCompatibilityUniqueMigrationEffectCheckpoint | null
  checkpointRevision: number
  authorityFencingToken: number
  lastRecoveryLeaseVersion: number
  lastRecoveryFencingToken: number
  recoveryLease: LegacyCompatibilityUniqueMigrationRecoveryLease | null
  logicalNowEpochSeconds: number
  clockRevision: number
  recoveryCount: number
  blockedFailure:
    | 'preflightUnavailable'
    | 'archiveUnavailable'
    | 'deleteUnavailable'
    | 'ddlUnavailable'
    | 'checkpointConflict'
    | null
}

export interface LegacyCompatibilityUniqueMigrationEffectReference {
  expectedEffectId: string
  checkpointRevision: number
  fencingToken: number
}

export interface LegacyCompatibilityUniqueMigrationEffectRequested {
  type: 'LEGACY_COMPATIBILITY_UNIQUE_MIGRATION_EFFECT_REQUESTED'
  migrationId: string
  effectId: string
  effectCheckpoint: LegacyCompatibilityUniqueMigrationEffectCheckpoint
  checkpointRevision: number
  fencingToken: number
  scanRevision: number
  groupDigest?: string
  canonicalSagaId?: string
  archivedSagaIds?: readonly string[]
  operatorResolutionId?: string
  archiveId?: string
  archiveDigest?: string
  readOnlyPreflight: boolean
  recoveryLeaseId: string | null
  recoveryLeaseHolderId: string | null
  cause: 'stateTransition' | 'recovery'
}

type MigrationFailureCode = NonNullable<
  LegacyCompatibilityUniqueMigrationContext['blockedFailure']
>

export type LegacyCompatibilityUniqueMigrationEvent =
  | ({
      type: 'PREFLIGHT_COMPLETED'
      migrationId: string
      scanRevision: number
      indexPresent: boolean
      duplicateGroups: readonly LegacyCompatibilityDuplicateGroup[]
    } & LegacyCompatibilityUniqueMigrationEffectReference)
  | ({
      type: 'UNIQUE_INDEX_INSTALLED'
      migrationId: string
    } & LegacyCompatibilityUniqueMigrationEffectReference)
  | {
      type: 'OPERATOR_RESOLUTION_REQUESTED'
      migrationId: string
      scanRevision: number
      groupDigest: string
      canonicalSagaId: string
      operatorResolutionId: string
    }
  | ({
      type: 'ARCHIVE_COMMITTED'
      migrationId: string
      archiveId: string
      archiveDigest: string
    } & LegacyCompatibilityUniqueMigrationEffectReference)
  | ({
      type: 'ARCHIVED_ROWS_DELETED'
      migrationId: string
    } & LegacyCompatibilityUniqueMigrationEffectReference)
  | ({
      type: 'MIGRATION_EFFECT_FAILED'
      migrationId: string
      failureCode: MigrationFailureCode
    } & LegacyCompatibilityUniqueMigrationEffectReference)
  | {
      type: 'EXTERNAL_REPAIR_CONFIRMED'
      migrationId: string
      scanRevision: number
      repairEvidenceDigest: string
    }
  | {
      type: 'CLOCK_ADVANCED'
      migrationId: string
      clockRevision: number
      nowEpochSeconds: number
    }
  | {
      type: 'RECOVERY_LEASE_ACQUIRED'
      migrationId: string
      expectedEffectId: string
      effectCheckpoint: LegacyCompatibilityUniqueMigrationEffectCheckpoint
      checkpointRevision: number
      leaseId: string
      holderId: string
      version: number
      fencingToken: number
      expiresAtLogicalEpochSeconds: number
    }
  | {
      type: 'RECOVERY_REQUESTED'
      migrationId: string
      expectedEffectId: string
      effectCheckpoint: LegacyCompatibilityUniqueMigrationEffectCheckpoint
      checkpointRevision: number
      leaseId: string
      holderId: string
      version: number
      fencingToken: number
    }

const effectId = (
  context: LegacyCompatibilityUniqueMigrationContext,
  checkpoint: LegacyCompatibilityUniqueMigrationEffectCheckpoint,
): string => JSON.stringify([
  'legacy-compatibility-request-key-unique-migration',
  'v1',
  context.migrationId,
  context.schemaVersion,
  checkpoint,
  context.checkpointRevision,
  context.scanRevision,
  context.activeResolution?.groupDigest ?? null,
  context.activeResolution?.operatorResolutionId ?? null,
  context.activeResolution?.archiveId ?? null,
])

export const legacyCompatibilityUniqueMigrationExpectedEffectReference = (
  context: LegacyCompatibilityUniqueMigrationContext,
): LegacyCompatibilityUniqueMigrationEffectReference | null =>
  context.effectCheckpoint === null
    ? null
    : {
        expectedEffectId: effectId(context, context.effectCheckpoint),
        checkpointRevision: context.checkpointRevision,
        fencingToken: context.authorityFencingToken,
      }

export const eventAcknowledgesCurrentEffect = (
  context: LegacyCompatibilityUniqueMigrationContext,
  event: LegacyCompatibilityUniqueMigrationEvent,
): boolean => {
  if (!('expectedEffectId' in event)) return false
  const expected = legacyCompatibilityUniqueMigrationExpectedEffectReference(context)
  if (!(expected !== null &&
    event.migrationId === context.migrationId &&
    event.expectedEffectId === expected.expectedEffectId &&
    event.checkpointRevision === expected.checkpointRevision &&
    event.fencingToken === expected.fencingToken)) return false
  const lease = context.recoveryLease
  return lease === null || (
    lease.effectEmitted &&
    lease.fencingToken === expected.fencingToken &&
    context.logicalNowEpochSeconds < lease.expiresAtLogicalEpochSeconds
  )
}

export const archiveCommitIsValid = (
  context: LegacyCompatibilityUniqueMigrationContext,
  event: LegacyCompatibilityUniqueMigrationEvent,
): event is Extract<LegacyCompatibilityUniqueMigrationEvent, { type: 'ARCHIVE_COMMITTED' }> =>
  event.type === 'ARCHIVE_COMMITTED' &&
  event.archiveId.trim().length > 0 &&
  event.archiveDigest.trim().length > 0 &&
  eventAcknowledgesCurrentEffect(context, event)

const duplicateGroupIsValid = (group: LegacyCompatibilityDuplicateGroup): boolean => {
  if (group.groupDigest.trim().length === 0 || group.rowCount < 2) return false
  if (!Number.isInteger(group.rowCount) || group.sagaIds.length !== group.rowCount) return false
  if (group.businessIdentityDigests.length !== group.rowCount) return false
  if (new Set(group.sagaIds).size !== group.sagaIds.length) return false
  if (group.sagaIds.some((sagaId) => sagaId.trim().length === 0)) return false
  if (group.businessIdentityDigests.some((digest) => digest.trim().length === 0)) return false
  if (!Number.isInteger(group.activeLeaseCount)) return false
  return group.activeLeaseCount >= 0 && group.activeLeaseCount <= group.rowCount
}

const duplicateFindingsAreValid = (
  groups: readonly LegacyCompatibilityDuplicateGroup[],
): boolean => {
  if (!groups.every(duplicateGroupIsValid)) return false
  if (new Set(groups.map((group) => group.groupDigest)).size !== groups.length) return false
  const sagaIds = groups.flatMap((group) => [...group.sagaIds])
  return new Set(sagaIds).size === sagaIds.length
}

export const groupCanBeResolvedByOperator = (
  group: LegacyCompatibilityDuplicateGroup,
): boolean => group.quiescent &&
  !group.divergent &&
  group.activeLeaseCount === 0 &&
  new Set(group.businessIdentityDigests).size === 1

export const findingsRequireExternalRepair = (
  groups: readonly LegacyCompatibilityDuplicateGroup[],
) => groups.some((group) => !groupCanBeResolvedByOperator(group))

export const effectRequested = (
  context: LegacyCompatibilityUniqueMigrationContext,
  cause: 'stateTransition' | 'recovery' = 'stateTransition',
): LegacyCompatibilityUniqueMigrationEffectRequested => {
  const checkpoint = context.effectCheckpoint
  if (checkpoint === null) throw new Error('No migration effect is pending')
  const resolution = context.activeResolution
  return {
    type: 'LEGACY_COMPATIBILITY_UNIQUE_MIGRATION_EFFECT_REQUESTED',
    migrationId: context.migrationId,
    effectId: effectId(context, checkpoint),
    effectCheckpoint: checkpoint,
    checkpointRevision: context.checkpointRevision,
    fencingToken: context.authorityFencingToken,
    scanRevision: context.scanRevision,
    groupDigest: resolution?.groupDigest,
    canonicalSagaId: resolution?.canonicalSagaId,
    archivedSagaIds: resolution?.archivedSagaIds,
    operatorResolutionId: resolution?.operatorResolutionId,
    archiveId: resolution?.archiveId ?? undefined,
    archiveDigest: resolution?.archiveDigest ?? undefined,
    readOnlyPreflight: checkpoint === 'scanDuplicates',
    recoveryLeaseId: cause === 'recovery' ? context.recoveryLease?.leaseId ?? null : null,
    recoveryLeaseHolderId: cause === 'recovery'
      ? context.recoveryLease?.holderId ?? null
      : null,
    cause,
  }
}

export const nextEffect = (
  context: LegacyCompatibilityUniqueMigrationContext,
  checkpoint: LegacyCompatibilityUniqueMigrationEffectCheckpoint,
) => ({
  effectCheckpoint: checkpoint,
  checkpointRevision: context.checkpointRevision + 1,
  authorityFencingToken: context.authorityFencingToken + 1,
  recoveryLease: null,
})

export const clearEffect = () => ({ effectCheckpoint: null, recoveryLease: null })

export const logicalClockAdvanceIsValid = (
  context: LegacyCompatibilityUniqueMigrationContext,
  event: LegacyCompatibilityUniqueMigrationEvent,
) => event.type === 'CLOCK_ADVANCED' &&
  event.migrationId === context.migrationId &&
  event.clockRevision === context.clockRevision + 1 &&
  Number.isInteger(event.nowEpochSeconds) &&
  event.nowEpochSeconds >= context.logicalNowEpochSeconds

export const recoveryLeaseCanBeRecorded = (
  context: LegacyCompatibilityUniqueMigrationContext,
  event: LegacyCompatibilityUniqueMigrationEvent,
): event is Extract<LegacyCompatibilityUniqueMigrationEvent, {
  type: 'RECOVERY_LEASE_ACQUIRED'
}> => {
  if (event.type !== 'RECOVERY_LEASE_ACQUIRED') return false
  const expected = legacyCompatibilityUniqueMigrationExpectedEffectReference(context)
  if (expected === null || context.effectCheckpoint === null) return false
  const priorLeaseExpired = context.recoveryLease === null ||
    context.logicalNowEpochSeconds >= context.recoveryLease.expiresAtLogicalEpochSeconds
  return event.migrationId === context.migrationId &&
    event.expectedEffectId === expected.expectedEffectId &&
    event.effectCheckpoint === context.effectCheckpoint &&
    event.checkpointRevision === expected.checkpointRevision &&
    event.leaseId.trim().length > 0 &&
    event.holderId.trim().length > 0 &&
    Number.isInteger(event.version) &&
    event.version > context.lastRecoveryLeaseVersion &&
    Number.isInteger(event.fencingToken) &&
    event.fencingToken > context.lastRecoveryFencingToken &&
    event.fencingToken > context.authorityFencingToken &&
    Number.isInteger(event.expiresAtLogicalEpochSeconds) &&
    event.expiresAtLogicalEpochSeconds > context.logicalNowEpochSeconds &&
    priorLeaseExpired
}

export const recoveryRequestOwnsCurrentLease = (
  context: LegacyCompatibilityUniqueMigrationContext,
  event: LegacyCompatibilityUniqueMigrationEvent,
): event is Extract<LegacyCompatibilityUniqueMigrationEvent, {
  type: 'RECOVERY_REQUESTED'
}> => {
  if (event.type !== 'RECOVERY_REQUESTED') return false
  const lease = context.recoveryLease
  if (lease === null || lease.effectEmitted) return false
  return event.migrationId === context.migrationId &&
    event.expectedEffectId === lease.effectId &&
    event.effectCheckpoint === lease.effectCheckpoint &&
    event.checkpointRevision === lease.checkpointRevision &&
    event.leaseId === lease.leaseId &&
    event.holderId === lease.holderId &&
    event.version === lease.version &&
    event.fencingToken === lease.fencingToken &&
    context.logicalNowEpochSeconds < lease.expiresAtLogicalEpochSeconds
}

export const preflightEventIsValid = (
  context: LegacyCompatibilityUniqueMigrationContext,
  event: LegacyCompatibilityUniqueMigrationEvent,
): event is Extract<LegacyCompatibilityUniqueMigrationEvent, { type: 'PREFLIGHT_COMPLETED' }> =>
  event.type === 'PREFLIGHT_COMPLETED' &&
  eventAcknowledgesCurrentEffect(context, event) &&
  event.scanRevision === context.scanRevision + 1 &&
  duplicateFindingsAreValid(event.duplicateGroups)

export const operatorResolutionIsValid = (
  context: LegacyCompatibilityUniqueMigrationContext,
  event: LegacyCompatibilityUniqueMigrationEvent,
): event is Extract<LegacyCompatibilityUniqueMigrationEvent, {
  type: 'OPERATOR_RESOLUTION_REQUESTED'
}> => {
  if (
    event.type !== 'OPERATOR_RESOLUTION_REQUESTED' ||
    event.migrationId !== context.migrationId ||
    event.scanRevision !== context.scanRevision ||
    event.operatorResolutionId.trim().length === 0
  ) return false
  const group = context.duplicateGroups.find(
    (candidate) => candidate.groupDigest === event.groupDigest,
  )
  return group !== undefined &&
    group.sagaIds.includes(event.canonicalSagaId) &&
    groupCanBeResolvedByOperator(group)
}

export const externalRepairIsValid = (
  context: LegacyCompatibilityUniqueMigrationContext,
  event: LegacyCompatibilityUniqueMigrationEvent,
) => event.type === 'EXTERNAL_REPAIR_CONFIRMED' &&
  event.migrationId === context.migrationId &&
  event.scanRevision === context.scanRevision &&
  event.repairEvidenceDigest.trim().length > 0

export const legacyCompatibilityUniqueMigrationDiagnostic = (
  context: LegacyCompatibilityUniqueMigrationContext,
) => ({
  migrationId: context.migrationId,
  schemaVersion: context.schemaVersion,
  scanRevision: context.scanRevision,
  duplicateGroupCount: context.duplicateGroups.length,
  duplicateRowCount: context.duplicateGroups.reduce(
    (count, group) => count + group.rowCount,
    0,
  ),
  blockingDisposition: context.blockingDisposition,
  groups: context.duplicateGroups.map((group) => ({
    groupDigest: group.groupDigest,
    rowCount: group.rowCount,
    activeLeaseCount: group.activeLeaseCount,
    resolution: groupCanBeResolvedByOperator(group)
      ? 'operatorResolutionAvailable'
      : 'externalRepairRequired',
  })),
  blockedFailure: context.blockedFailure,
})

export const legacyCompatibilityUniqueMigrationRuntimeReady = (
  snapshot: { value: unknown },
) => snapshot.value === 'ready'

export const legacyCompatibilityUniqueMigrationPortContract = {
  startupOrder: ['preflight', 'unique-index', 'routes-and-recovery-scheduler'],
  duplicatePreflight: 'read-only-and-fail-closed',
  diagnostics: 'digest-and-count-only',
  resolutionAuthority: 'explicit-operator-event-fenced-by-scan-revision',
  archivePolicy: 'immutable-rows-and-effects-before-delete',
  rollbackPolicy: 'blocked-and-external-repair-before-new-preflight',
  recoveryAuthority: 'durable-cas-lease-before-exact-fenced-recovery-request',
  recoveryClock: 'persisted-monotone-logical-clock-only',
} as const

export const legacyCompatibilityUniqueMigrationInvariants = [
  'routes and the compatibility recovery scheduler remain disabled until READY',
  'preflight is read-only and a duplicate finding never attempts index DDL',
  'diagnostics expose only digests and counts, never a raw token or request key',
  'no duplicate row is selected automatically and no LLM can choose a canonical saga',
  'operator resolution matches the current scan revision and chooses a saga in that group',
  'operator resolution requires identical business identity digests and no active lease',
  'divergent or nonquiescent groups remain blocked pending external repair',
  'noncanonical rows and their effects are archived immutably before deletion',
  'every destructive checkpoint is fenced and idempotent across crash and restart',
  'a restored checkpoint emits only for the exact durable unexpired lease holder',
  'one recovery lease emits at most once and replacement requires higher version and fence',
  'recovery lease expiry uses only the persisted monotone logical clock',
  'every effect acknowledgement matches effect ID, checkpoint revision and current fence',
  'effect failure never enables runtime and rollback is fail-closed',
  'the unique index is installed only after a duplicate-free preflight',
] as const

export const validatedMigrationInput = (
  input: LegacyCompatibilityUniqueMigrationInput,
): LegacyCompatibilityUniqueMigrationInput | null => {
  if (input.migrationId.trim().length === 0) return null
  if (!Number.isInteger(input.schemaVersion) || input.schemaVersion < 1) return null
  if (
    !Number.isInteger(input.initialLogicalNowEpochSeconds) ||
    input.initialLogicalNowEpochSeconds < 0
  ) return null
  return { ...input, migrationId: input.migrationId.trim() }
}
