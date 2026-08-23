import { assign, createActor, emit, setup } from 'xstate'

import {
  archiveCommitIsValid,
  clearEffect,
  effectRequested,
  eventAcknowledgesCurrentEffect,
  externalRepairIsValid,
  findingsRequireExternalRepair,
  logicalClockAdvanceIsValid,
  nextEffect,
  operatorResolutionIsValid,
  preflightEventIsValid,
  recoveryLeaseCanBeRecorded,
  recoveryRequestOwnsCurrentLease,
  validatedMigrationInput,
  type LegacyCompatibilityUniqueMigrationContext,
  type LegacyCompatibilityUniqueMigrationEffectRequested,
  type LegacyCompatibilityUniqueMigrationEvent,
  type LegacyCompatibilityUniqueMigrationInput,
} from './legacy-compatibility-unique-migration.core.ts'

export {
  legacyCompatibilityUniqueMigrationDiagnostic,
  legacyCompatibilityUniqueMigrationExpectedEffectReference,
  legacyCompatibilityUniqueMigrationInvariants,
  legacyCompatibilityUniqueMigrationPortContract,
  legacyCompatibilityUniqueMigrationRuntimeReady,
} from './legacy-compatibility-unique-migration.core.ts'
export type {
  LegacyCompatibilityArchiveAuditEntry,
  LegacyCompatibilityDuplicateGroup,
  LegacyCompatibilityUniqueMigrationContext,
  LegacyCompatibilityUniqueMigrationEffectCheckpoint,
  LegacyCompatibilityUniqueMigrationEffectReference,
  LegacyCompatibilityUniqueMigrationEffectRequested,
  LegacyCompatibilityUniqueMigrationEvent,
  LegacyCompatibilityUniqueMigrationInput,
  LegacyCompatibilityUniqueMigrationRecoveryLease,
} from './legacy-compatibility-unique-migration.core.ts'

export const legacyCompatibilityUniqueMigrationMachine = setup({
  types: {
    context: {} as LegacyCompatibilityUniqueMigrationContext,
    events: {} as LegacyCompatibilityUniqueMigrationEvent,
    input: {} as LegacyCompatibilityUniqueMigrationInput,
    emitted: {} as LegacyCompatibilityUniqueMigrationEffectRequested,
  },
  guards: {
    duplicateFreeAndIndexed: ({ context, event }) =>
      preflightEventIsValid(context, event) &&
      event.duplicateGroups.length === 0 &&
      event.indexPresent,
    duplicateFreeNeedsIndex: ({ context, event }) =>
      preflightEventIsValid(context, event) &&
      event.duplicateGroups.length === 0 &&
      !event.indexPresent,
    duplicatesFound: ({ context, event }) =>
      preflightEventIsValid(context, event) && event.duplicateGroups.length > 0,
    effectAckIsCurrent: ({ context, event }) =>
      eventAcknowledgesCurrentEffect(context, event),
    archiveCommitIsValid: ({ context, event }) =>
      archiveCommitIsValid(context, event),
    operatorResolutionIsValid: ({ context, event }) =>
      operatorResolutionIsValid(context, event),
    externalRepairIsValid: ({ context, event }) =>
      externalRepairIsValid(context, event),
    logicalClockAdvanceIsValid: ({ context, event }) =>
      logicalClockAdvanceIsValid(context, event),
    recoveryLeaseCanBeRecorded: ({ context, event }) =>
      recoveryLeaseCanBeRecorded(context, event),
    recoveryRequestOwnsCurrentLease: ({ context, event }) =>
      recoveryRequestOwnsCurrentLease(context, event),
  },
  actions: {
    emitCurrentEffect: emit(({ context, event }) => effectRequested(
      context,
      event.type === 'RECOVERY_REQUESTED' ? 'recovery' : 'stateTransition',
    )),
    acceptReadyPreflight: assign(({ context, event }) => {
      if (event.type !== 'PREFLIGHT_COMPLETED') return context
      return {
        ...context,
        scanRevision: event.scanRevision,
        indexInstalled: true,
        duplicateGroups: [],
        blockingDisposition: 'none' as const,
        activeResolution: null,
        blockedFailure: null,
        ...clearEffect(),
      }
    }),
    beginIndexInstallation: assign(({ context, event }) => {
      if (event.type !== 'PREFLIGHT_COMPLETED') return context
      return {
        ...context,
        scanRevision: event.scanRevision,
        indexInstalled: false,
        duplicateGroups: [],
        blockingDisposition: 'none' as const,
        activeResolution: null,
        blockedFailure: null,
        ...nextEffect(context, 'installUniqueRequestKeyIndex'),
      }
    }),
    blockOnDuplicates: assign(({ context, event }) => {
      if (event.type !== 'PREFLIGHT_COMPLETED') return context
      return {
        ...context,
        scanRevision: event.scanRevision,
        indexInstalled: false,
        duplicateGroups: event.duplicateGroups,
        blockingDisposition: findingsRequireExternalRepair(event.duplicateGroups)
          ? 'externalRepairRequired' as const
          : 'operatorResolutionAvailable' as const,
        activeResolution: null,
        blockedFailure: null,
        ...clearEffect(),
      }
    }),
    beginArchive: assign(({ context, event }) => {
      if (event.type !== 'OPERATOR_RESOLUTION_REQUESTED') return context
      const group = context.duplicateGroups.find(
        (candidate) => candidate.groupDigest === event.groupDigest,
      )
      if (group === undefined) return context
      return {
        ...context,
        activeResolution: {
          groupDigest: group.groupDigest,
          canonicalSagaId: event.canonicalSagaId,
          archivedSagaIds: group.sagaIds.filter(
            (sagaId) => sagaId !== event.canonicalSagaId,
          ),
          operatorResolutionId: event.operatorResolutionId,
          scanRevision: event.scanRevision,
          archiveId: null,
          archiveDigest: null,
        },
        ...nextEffect(context, 'archiveNoncanonicalRowsAndEffects'),
      }
    }),
    recordArchiveAndBeginDelete: assign(({ context, event }) => {
      if (event.type !== 'ARCHIVE_COMMITTED' || context.activeResolution === null) {
        return context
      }
      const activeResolution = {
        ...context.activeResolution,
        archiveId: event.archiveId,
        archiveDigest: event.archiveDigest,
      }
      return {
        ...context,
        activeResolution,
        archiveAudit: [
          ...context.archiveAudit,
          {
            archiveId: event.archiveId,
            archiveDigest: event.archiveDigest,
            groupDigest: activeResolution.groupDigest,
            canonicalSagaId: activeResolution.canonicalSagaId,
            archivedRowCount: activeResolution.archivedSagaIds.length,
            scanRevision: activeResolution.scanRevision,
            operatorResolutionId: activeResolution.operatorResolutionId,
          },
        ],
        ...nextEffect(context, 'deleteArchivedNoncanonicalRows'),
      }
    }),
    beginRepreflight: assign(({ context }) => ({
      ...context,
      duplicateGroups: [],
      blockingDisposition: 'none' as const,
      activeResolution: null,
      blockedFailure: null,
      ...nextEffect(context, 'scanDuplicates'),
    })),
    markIndexInstalled: assign(({ context }) => ({
      ...context,
      indexInstalled: true,
      blockedFailure: null,
      ...clearEffect(),
    })),
    recordFailure: assign(({ context, event }) => ({
      ...context,
      blockingDisposition: 'externalRepairRequired' as const,
      blockedFailure: event.type === 'MIGRATION_EFFECT_FAILED'
        ? event.failureCode
        : 'checkpointConflict',
      ...clearEffect(),
    })),
    recordRecoveryLease: assign(({ context, event }) => {
      if (event.type !== 'RECOVERY_LEASE_ACQUIRED') return context
      return {
        ...context,
        authorityFencingToken: event.fencingToken,
        lastRecoveryLeaseVersion: event.version,
        lastRecoveryFencingToken: event.fencingToken,
        recoveryLease: {
          leaseId: event.leaseId,
          holderId: event.holderId,
          version: event.version,
          fencingToken: event.fencingToken,
          expiresAtLogicalEpochSeconds: event.expiresAtLogicalEpochSeconds,
          acquiredAtClockRevision: context.clockRevision,
          effectId: event.expectedEffectId,
          effectCheckpoint: event.effectCheckpoint,
          checkpointRevision: event.checkpointRevision,
          effectEmitted: false,
        },
      }
    }),
    markRecoveryEffectEmitted: assign(({ context }) => ({
      ...context,
      recoveryCount: context.recoveryCount + 1,
      recoveryLease: context.recoveryLease === null
        ? null
        : { ...context.recoveryLease, effectEmitted: true },
    })),
    advanceLogicalClock: assign(({ context, event }) => event.type === 'CLOCK_ADVANCED'
      ? {
          ...context,
          logicalNowEpochSeconds: event.nowEpochSeconds,
          clockRevision: event.clockRevision,
        }
      : context),
  },
}).createMachine({
  id: 'legacyCompatibilityUniqueMigration',
  initial: 'startupPreflight',
  context: ({ input }) => ({
    migrationId: input.migrationId,
    schemaVersion: input.schemaVersion,
    initialLogicalNowEpochSeconds: input.initialLogicalNowEpochSeconds,
    scanRevision: 0,
    indexInstalled: false,
    duplicateGroups: [],
    blockingDisposition: 'none',
    activeResolution: null,
    archiveAudit: [],
    effectCheckpoint: 'scanDuplicates',
    checkpointRevision: 1,
    authorityFencingToken: 1,
    lastRecoveryLeaseVersion: 0,
    lastRecoveryFencingToken: 0,
    recoveryLease: null,
    logicalNowEpochSeconds: input.initialLogicalNowEpochSeconds,
    clockRevision: 0,
    recoveryCount: 0,
    blockedFailure: null,
  }),
  on: {
    CLOCK_ADVANCED: {
      guard: 'logicalClockAdvanceIsValid',
      actions: 'advanceLogicalClock',
    },
    RECOVERY_LEASE_ACQUIRED: {
      guard: 'recoveryLeaseCanBeRecorded',
      actions: 'recordRecoveryLease',
    },
    RECOVERY_REQUESTED: {
      guard: 'recoveryRequestOwnsCurrentLease',
      actions: ['markRecoveryEffectEmitted', 'emitCurrentEffect'],
    },
  },
  states: {
    startupPreflight: {
      entry: 'emitCurrentEffect',
      on: {
        PREFLIGHT_COMPLETED: [
          {
            guard: 'duplicateFreeAndIndexed',
            target: 'ready',
            actions: 'acceptReadyPreflight',
          },
          {
            guard: 'duplicateFreeNeedsIndex',
            target: 'installingUniqueIndex',
            actions: 'beginIndexInstallation',
          },
          {
            guard: 'duplicatesFound',
            target: 'blockedDuplicates',
            actions: 'blockOnDuplicates',
          },
        ],
        MIGRATION_EFFECT_FAILED: {
          guard: 'effectAckIsCurrent',
          target: 'blockedMigrationFailure',
          actions: 'recordFailure',
        },
      },
    },
    installingUniqueIndex: {
      entry: 'emitCurrentEffect',
      on: {
        UNIQUE_INDEX_INSTALLED: {
          guard: 'effectAckIsCurrent',
          target: 'ready',
          actions: 'markIndexInstalled',
        },
        MIGRATION_EFFECT_FAILED: {
          guard: 'effectAckIsCurrent',
          target: 'blockedMigrationFailure',
          actions: 'recordFailure',
        },
      },
    },
    blockedDuplicates: {
      on: {
        OPERATOR_RESOLUTION_REQUESTED: {
          guard: 'operatorResolutionIsValid',
          target: 'archivingNoncanonicalRows',
          actions: 'beginArchive',
        },
        EXTERNAL_REPAIR_CONFIRMED: {
          guard: 'externalRepairIsValid',
          target: 'startupPreflight',
          actions: 'beginRepreflight',
        },
      },
    },
    archivingNoncanonicalRows: {
      entry: 'emitCurrentEffect',
      on: {
        ARCHIVE_COMMITTED: {
          guard: 'archiveCommitIsValid',
          target: 'deletingArchivedRows',
          actions: 'recordArchiveAndBeginDelete',
        },
        MIGRATION_EFFECT_FAILED: {
          guard: 'effectAckIsCurrent',
          target: 'blockedMigrationFailure',
          actions: 'recordFailure',
        },
      },
    },
    deletingArchivedRows: {
      entry: 'emitCurrentEffect',
      on: {
        ARCHIVED_ROWS_DELETED: {
          guard: 'effectAckIsCurrent',
          target: 'startupPreflight',
          actions: 'beginRepreflight',
        },
        MIGRATION_EFFECT_FAILED: {
          guard: 'effectAckIsCurrent',
          target: 'blockedMigrationFailure',
          actions: 'recordFailure',
        },
      },
    },
    blockedMigrationFailure: {
      on: {
        EXTERNAL_REPAIR_CONFIRMED: {
          guard: 'externalRepairIsValid',
          target: 'startupPreflight',
          actions: 'beginRepreflight',
        },
      },
    },
    ready: { type: 'final' },
  },
})

export const createLegacyCompatibilityUniqueMigrationActor = (
  rawInput: LegacyCompatibilityUniqueMigrationInput,
) => {
  const input = validatedMigrationInput(rawInput)
  return input === null
    ? null
    : createActor(legacyCompatibilityUniqueMigrationMachine, { input })
}
