import assert from 'node:assert/strict'
import test from 'node:test'
import { createActor } from 'xstate'

import {
  createLegacyCompatibilityUniqueMigrationActor,
  legacyCompatibilityUniqueMigrationDiagnostic,
  legacyCompatibilityUniqueMigrationExpectedEffectReference,
  legacyCompatibilityUniqueMigrationRuntimeReady,
  legacyCompatibilityUniqueMigrationMachine,
  type LegacyCompatibilityDuplicateGroup,
  type LegacyCompatibilityUniqueMigrationEffectReference,
  type LegacyCompatibilityUniqueMigrationEffectRequested,
  type LegacyCompatibilityUniqueMigrationInput,
} from './legacy-compatibility-unique-migration.machine.ts'

const input = (
  overrides: Partial<LegacyCompatibilityUniqueMigrationInput> = {},
): LegacyCompatibilityUniqueMigrationInput => ({
  migrationId: 'legacy-compatibility-request-key-unique-v1',
  schemaVersion: 1,
  initialLogicalNowEpochSeconds: 100,
  ...overrides,
})

const simpleGroup = (
  overrides: Partial<LegacyCompatibilityDuplicateGroup> = {},
): LegacyCompatibilityDuplicateGroup => ({
  groupDigest: 'sha256:duplicate-group-a',
  rowCount: 3,
  sagaIds: ['saga-a', 'saga-b', 'saga-c'],
  businessIdentityDigests: [
    'sha256:business-identity-a',
    'sha256:business-identity-a',
    'sha256:business-identity-a',
  ],
  activeLeaseCount: 0,
  quiescent: true,
  divergent: false,
  ...overrides,
})

const actorWithEffects = (actorInput = input()) => {
  const actor = createLegacyCompatibilityUniqueMigrationActor(actorInput)
  assert.notEqual(actor, null)
  const effects: LegacyCompatibilityUniqueMigrationEffectRequested[] = []
  actor!.on('LEGACY_COMPATIBILITY_UNIQUE_MIGRATION_EFFECT_REQUESTED', (effect) => {
    effects.push(effect)
  })
  actor!.start()
  return { actor: actor!, effects }
}

type MigrationActor = ReturnType<typeof actorWithEffects>['actor']
type MigrationSnapshot = ReturnType<MigrationActor['getPersistedSnapshot']>

const currentEffect = (actor: MigrationActor) => {
  const reference = legacyCompatibilityUniqueMigrationExpectedEffectReference(
    actor.getSnapshot().context,
  )
  assert.notEqual(reference, null)
  return reference!
}

const preflightCompleted = (
  actor: MigrationActor,
  duplicateGroups: LegacyCompatibilityDuplicateGroup[],
  indexPresent: boolean,
  reference = currentEffect(actor),
) => actor.send({
  type: 'PREFLIGHT_COMPLETED',
  migrationId: actor.getSnapshot().context.migrationId,
  scanRevision: actor.getSnapshot().context.scanRevision + 1,
  indexPresent,
  duplicateGroups,
  ...reference,
})

const indexInstalled = (
  actor: MigrationActor,
  reference = currentEffect(actor),
) =>
  actor.send({
    type: 'UNIQUE_INDEX_INSTALLED',
    migrationId: actor.getSnapshot().context.migrationId,
    ...reference,
  })

const acquireRecoveryLease = (
  actor: MigrationActor,
  overrides: Partial<{
    leaseId: string
    holderId: string
    version: number
    fencingToken: number
    expiresAtLogicalEpochSeconds: number
  }> = {},
) => {
  const context = actor.getSnapshot().context
  const effectReference = currentEffect(actor)
  assert.notEqual(context.effectCheckpoint, null)
  const lease = {
    leaseId: overrides.leaseId ?? 'migration-recovery-lease-1',
    holderId: overrides.holderId ?? 'migration-worker-owner',
    version: overrides.version ?? ((context.lastRecoveryLeaseVersion ?? 0) + 1),
    fencingToken: overrides.fencingToken ?? Math.max(
      context.authorityFencingToken,
      context.lastRecoveryFencingToken ?? 0,
    ) + 1,
    expiresAtLogicalEpochSeconds: overrides.expiresAtLogicalEpochSeconds ??
      ((context.logicalNowEpochSeconds ?? 100) + 10),
    effectId: effectReference.expectedEffectId,
    effectCheckpoint: context.effectCheckpoint!,
    checkpointRevision: effectReference.checkpointRevision,
  }
  actor.send({
    type: 'RECOVERY_LEASE_ACQUIRED',
    migrationId: context.migrationId,
    expectedEffectId: lease.effectId,
    effectCheckpoint: lease.effectCheckpoint,
    checkpointRevision: lease.checkpointRevision,
    leaseId: lease.leaseId,
    holderId: lease.holderId,
    version: lease.version,
    fencingToken: lease.fencingToken,
    expiresAtLogicalEpochSeconds: lease.expiresAtLogicalEpochSeconds,
  })
  return lease
}

const requestRecovery = (
  actor: MigrationActor,
  lease: ReturnType<typeof acquireRecoveryLease>,
  overrides: Partial<ReturnType<typeof acquireRecoveryLease>> = {},
) => actor.send({
  type: 'RECOVERY_REQUESTED',
  migrationId: actor.getSnapshot().context.migrationId,
  expectedEffectId: overrides.effectId ?? lease.effectId,
  effectCheckpoint: overrides.effectCheckpoint ?? lease.effectCheckpoint,
  checkpointRevision: overrides.checkpointRevision ?? lease.checkpointRevision,
  leaseId: overrides.leaseId ?? lease.leaseId,
  holderId: overrides.holderId ?? lease.holderId,
  version: overrides.version ?? lease.version,
  fencingToken: overrides.fencingToken ?? lease.fencingToken,
})

const effectsFromRestoredActor = (
  snapshot: MigrationSnapshot,
) => {
  const actor = createActor(legacyCompatibilityUniqueMigrationMachine, { snapshot })
  const effects: LegacyCompatibilityUniqueMigrationEffectRequested[] = []
  actor.on('LEGACY_COMPATIBILITY_UNIQUE_MIGRATION_EFFECT_REQUESTED', (effect) => {
    effects.push(effect)
  })
  actor.start()
  return { actor, effects }
}

test('fresh and existing databases reach READY only through a successful preflight', () => {
  const cases = [
    { name: 'fresh database', indexPresent: false, installsIndex: true },
    { name: 'existing indexed database', indexPresent: true, installsIndex: false },
  ] as const

  for (const fixture of cases) {
    const { actor, effects } = actorWithEffects(input({ migrationId: fixture.name }))
    assert.equal(actor.getSnapshot().value, 'startupPreflight')
    assert.equal(legacyCompatibilityUniqueMigrationRuntimeReady(actor.getSnapshot()), false)
    assert.deepEqual(effects.map((effect) => effect.effectCheckpoint), ['scanDuplicates'])

    preflightCompleted(actor, [], fixture.indexPresent)
    if (fixture.installsIndex) {
      assert.equal(actor.getSnapshot().value, 'installingUniqueIndex')
      assert.equal(legacyCompatibilityUniqueMigrationRuntimeReady(actor.getSnapshot()), false)
      assert.deepEqual(
        effects.map((effect) => effect.effectCheckpoint),
        ['scanDuplicates', 'installUniqueRequestKeyIndex'],
      )
      indexInstalled(actor)
    }

    assert.equal(actor.getSnapshot().value, 'ready')
    assert.equal(legacyCompatibilityUniqueMigrationRuntimeReady(actor.getSnapshot()), true)
  }
})

test('duplicate preflight blocks before DDL and exposes only sanitized diagnostics', () => {
  const rawRequestKey = 'raw-request-key-must-not-escape'
  const rawToken = 'raw-token-must-not-escape'
  const { actor, effects } = actorWithEffects()
  preflightCompleted(actor, [simpleGroup()], false)

  assert.equal(actor.getSnapshot().value, 'blockedDuplicates')
  assert.equal(legacyCompatibilityUniqueMigrationRuntimeReady(actor.getSnapshot()), false)
  assert.deepEqual(effects.map((effect) => effect.effectCheckpoint), ['scanDuplicates'])

  const diagnostic = JSON.stringify(
    legacyCompatibilityUniqueMigrationDiagnostic(actor.getSnapshot().context),
  )
  assert.match(diagnostic, /sha256:duplicate-group-a/)
  assert.match(diagnostic, /"duplicateRowCount":3/)
  assert.doesNotMatch(diagnostic, /saga-a|saga-b|saga-c/)
  assert.doesNotMatch(diagnostic, new RegExp(rawRequestKey))
  assert.doesNotMatch(diagnostic, new RegExp(rawToken))
})

test('operator resolution is fenced and rejects stale, foreign, divergent and active groups', () => {
  const cases = [
    {
      name: 'stale scan',
      group: simpleGroup(),
      event: { scanRevision: 0, canonicalSagaId: 'saga-a' },
    },
    {
      name: 'foreign canonical',
      group: simpleGroup(),
      event: { scanRevision: 1, canonicalSagaId: 'saga-foreign' },
    },
    {
      name: 'divergent identities',
      group: simpleGroup({
        businessIdentityDigests: ['identity-a', 'identity-b', 'identity-a'],
        divergent: true,
      }),
      event: { scanRevision: 1, canonicalSagaId: 'saga-a' },
    },
    {
      name: 'active lease',
      group: simpleGroup({ activeLeaseCount: 1, quiescent: false }),
      event: { scanRevision: 1, canonicalSagaId: 'saga-a' },
    },
  ] as const

  for (const fixture of cases) {
    const { actor, effects } = actorWithEffects(input({ migrationId: fixture.name }))
    preflightCompleted(actor, [fixture.group], false)
    actor.send({
      type: 'OPERATOR_RESOLUTION_REQUESTED',
      migrationId: actor.getSnapshot().context.migrationId,
      scanRevision: fixture.event.scanRevision,
      groupDigest: fixture.group.groupDigest,
      canonicalSagaId: fixture.event.canonicalSagaId,
      operatorResolutionId: 'operator-resolution-1',
    })

    assert.equal(actor.getSnapshot().value, 'blockedDuplicates', fixture.name)
    assert.deepEqual(effects.map((effect) => effect.effectCheckpoint), ['scanDuplicates'])
  }
})

test('simple quiescent resolution archives immutably before delete then re-preflights and indexes', () => {
  const { actor, effects } = actorWithEffects()
  preflightCompleted(actor, [simpleGroup()], false)
  actor.send({
    type: 'OPERATOR_RESOLUTION_REQUESTED',
    migrationId: actor.getSnapshot().context.migrationId,
    scanRevision: 1,
    groupDigest: 'sha256:duplicate-group-a',
    canonicalSagaId: 'saga-a',
    operatorResolutionId: 'operator-resolution-1',
  })

  assert.equal(actor.getSnapshot().value, 'archivingNoncanonicalRows')
  assert.deepEqual(effects.at(-1)?.archivedSagaIds, ['saga-b', 'saga-c'])
  assert.equal(effects.at(-1)?.canonicalSagaId, 'saga-a')

  actor.send({
    type: 'ARCHIVE_COMMITTED',
    migrationId: actor.getSnapshot().context.migrationId,
    archiveId: '',
    archiveDigest: '',
    ...currentEffect(actor),
  })
  assert.equal(actor.getSnapshot().value, 'archivingNoncanonicalRows')
  assert.equal(actor.getSnapshot().context.archiveAudit.length, 0)

  actor.send({
    type: 'ARCHIVE_COMMITTED',
    migrationId: actor.getSnapshot().context.migrationId,
    archiveId: 'archive-immutable-1',
    archiveDigest: 'sha256:archive-immutable-1',
    ...currentEffect(actor),
  })
  assert.equal(actor.getSnapshot().value, 'deletingArchivedRows')
  assert.equal(actor.getSnapshot().context.archiveAudit.length, 1)
  assert.equal(actor.getSnapshot().context.archiveAudit[0]?.archiveId, 'archive-immutable-1')
  assert.equal(effects.at(-1)?.archiveId, 'archive-immutable-1')

  actor.send({
    type: 'ARCHIVED_ROWS_DELETED',
    migrationId: actor.getSnapshot().context.migrationId,
    ...currentEffect(actor),
  })
  assert.equal(actor.getSnapshot().value, 'startupPreflight')
  assert.equal(effects.at(-1)?.effectCheckpoint, 'scanDuplicates')

  preflightCompleted(actor, [], false)
  assert.equal(actor.getSnapshot().value, 'installingUniqueIndex')
  indexInstalled(actor)
  assert.equal(actor.getSnapshot().value, 'ready')
  assert.equal(legacyCompatibilityUniqueMigrationRuntimeReady(actor.getSnapshot()), true)
})

test('two restored workers require one durable lease and recover every checkpoint with stale ACKs fenced', () => {
  const { actor } = actorWithEffects()
  preflightCompleted(actor, [simpleGroup()], false)
  actor.send({
    type: 'OPERATOR_RESOLUTION_REQUESTED',
    migrationId: actor.getSnapshot().context.migrationId,
    scanRevision: 1,
    groupDigest: 'sha256:duplicate-group-a',
    canonicalSagaId: 'saga-a',
    operatorResolutionId: 'operator-resolution-1',
  })

  const checkpointCases: ReadonlyArray<{
    expectedState: string
    acknowledge: (
      current: MigrationActor,
      reference: LegacyCompatibilityUniqueMigrationEffectReference,
    ) => void
  }> = [
    {
      expectedState: 'archivingNoncanonicalRows',
      acknowledge: (current, reference) => current.send({
        type: 'ARCHIVE_COMMITTED',
        migrationId: current.getSnapshot().context.migrationId,
        archiveId: 'archive-1',
        archiveDigest: 'sha256:archive-1',
        ...reference,
      }),
    },
    {
      expectedState: 'deletingArchivedRows',
      acknowledge: (current, reference) => current.send({
        type: 'ARCHIVED_ROWS_DELETED',
        migrationId: current.getSnapshot().context.migrationId,
        ...reference,
      }),
    },
    {
      expectedState: 'startupPreflight',
      acknowledge: (current, reference) => preflightCompleted(current, [], false, reference),
    },
    {
      expectedState: 'installingUniqueIndex',
      acknowledge: (current, reference) => indexInstalled(current, reference),
    },
  ]

  let currentActor = actor
  let recoveryOrdinal = 0
  for (const checkpointCase of checkpointCases) {
    recoveryOrdinal += 1
    assert.equal(currentActor.getSnapshot().value, checkpointCase.expectedState)
    const staleReference = currentEffect(currentActor)
    const snapshot = currentActor.getPersistedSnapshot()
    currentActor.stop()

    const leaseRecorder = effectsFromRestoredActor(snapshot)
    assert.equal(
      leaseRecorder.effects.length,
      0,
      'XState restore must not replay entry effects',
    )
    const lease = acquireRecoveryLease(leaseRecorder.actor, {
      leaseId: `migration-recovery-lease-${recoveryOrdinal}`,
      holderId: `migration-worker-owner-${recoveryOrdinal}`,
    })
    assert.equal(leaseRecorder.actor.getSnapshot().context.recoveryLease?.holderId, lease.holderId)
    assert.equal(leaseRecorder.actor.getSnapshot().context.recoveryLease?.effectEmitted, false)

    const leasedSnapshot = leaseRecorder.actor.getPersistedSnapshot()
    leaseRecorder.actor.stop()
    const ownerWorker = effectsFromRestoredActor(leasedSnapshot)
    const foreignWorker = effectsFromRestoredActor(leasedSnapshot)
    assert.equal(ownerWorker.effects.length, 0)
    assert.equal(foreignWorker.effects.length, 0)

    requestRecovery(foreignWorker.actor, lease, {
      holderId: `migration-worker-foreign-${recoveryOrdinal}`,
    })
    assert.equal(foreignWorker.effects.length, 0)
    requestRecovery(ownerWorker.actor, lease)
    requestRecovery(ownerWorker.actor, lease)
    assert.equal(ownerWorker.effects.length, 1, 'one lease emits at most once before ACK')
    assert.equal(ownerWorker.effects[0]?.cause, 'recovery')
    assert.equal(ownerWorker.effects[0]?.recoveryLeaseId, lease.leaseId)
    assert.equal(ownerWorker.effects[0]?.recoveryLeaseHolderId, lease.holderId)

    checkpointCase.acknowledge(ownerWorker.actor, staleReference)
    assert.equal(ownerWorker.actor.getSnapshot().value, checkpointCase.expectedState)
    checkpointCase.acknowledge(ownerWorker.actor, currentEffect(ownerWorker.actor))
    currentActor = ownerWorker.actor
    foreignWorker.actor.stop()
  }
  assert.equal(currentActor.getSnapshot().value, 'ready')
})

test('lease expiry requires a newer version and fence while concurrent and stale recovery stay inert', () => {
  const source = actorWithEffects()
  const snapshot = source.actor.getPersistedSnapshot()
  source.actor.stop()
  const restored = effectsFromRestoredActor(snapshot)
  assert.equal(restored.effects.length, 0)

  const ownerLease = acquireRecoveryLease(restored.actor, {
    leaseId: 'lease-owner-v1',
    holderId: 'worker-owner',
    expiresAtLogicalEpochSeconds: 110,
  })
  assert.equal(restored.actor.getSnapshot().context.recoveryLease?.effectEmitted, false)
  assert.equal(restored.actor.getSnapshot().context.recoveryLease?.acquiredAtClockRevision, 0)

  const concurrentLease = acquireRecoveryLease(restored.actor, {
    leaseId: 'lease-contender-v2',
    holderId: 'worker-contender',
    version: ownerLease.version + 1,
    fencingToken: ownerLease.fencingToken + 1,
    expiresAtLogicalEpochSeconds: 111,
  })
  assert.equal(restored.actor.getSnapshot().context.recoveryLease?.leaseId, ownerLease.leaseId)
  requestRecovery(restored.actor, concurrentLease)
  requestRecovery(restored.actor, ownerLease, { holderId: 'worker-foreign' })
  assert.equal(restored.effects.length, 0)

  requestRecovery(restored.actor, ownerLease)
  requestRecovery(restored.actor, ownerLease)
  assert.equal(restored.effects.length, 1)
  assert.equal(restored.actor.getSnapshot().context.recoveryLease?.effectEmitted, true)

  restored.actor.send({
    type: 'CLOCK_ADVANCED',
    migrationId: restored.actor.getSnapshot().context.migrationId,
    clockRevision: 1,
    nowEpochSeconds: 110,
  })
  requestRecovery(restored.actor, ownerLease)
  assert.equal(restored.effects.length, 1, 'expired holder cannot emit')

  acquireRecoveryLease(restored.actor, {
    leaseId: 'lease-old-fence',
    holderId: 'worker-old-fence',
    version: ownerLease.version,
    fencingToken: ownerLease.fencingToken,
    expiresAtLogicalEpochSeconds: 120,
  })
  assert.equal(restored.actor.getSnapshot().context.recoveryLease?.leaseId, ownerLease.leaseId)

  const replacementLease = acquireRecoveryLease(restored.actor, {
    leaseId: 'lease-owner-v2',
    holderId: 'worker-owner-v2',
    version: ownerLease.version + 1,
    fencingToken: ownerLease.fencingToken + 1,
    expiresAtLogicalEpochSeconds: 120,
  })
  assert.equal(restored.actor.getSnapshot().context.recoveryLease?.leaseId, replacementLease.leaseId)
  assert.equal(restored.actor.getSnapshot().context.recoveryLease?.effectEmitted, false)

  requestRecovery(restored.actor, ownerLease)
  assert.equal(restored.effects.length, 1, 'old fencing authority stays inert')
  requestRecovery(restored.actor, replacementLease)
  assert.equal(restored.effects.length, 2)

  const staleAcknowledgement = {
    expectedEffectId: ownerLease.effectId,
    checkpointRevision: ownerLease.checkpointRevision,
    fencingToken: ownerLease.fencingToken,
  }
  preflightCompleted(restored.actor, [], true, staleAcknowledgement)
  assert.equal(restored.actor.getSnapshot().value, 'startupPreflight')
  preflightCompleted(restored.actor, [], true)
  assert.equal(restored.actor.getSnapshot().value, 'ready')
})

test('effect failure and divergent repair remain fail-closed until explicit fenced re-preflight', () => {
  const { actor, effects } = actorWithEffects()
  preflightCompleted(actor, [], false)
  actor.send({
    type: 'MIGRATION_EFFECT_FAILED',
    migrationId: actor.getSnapshot().context.migrationId,
    failureCode: 'ddlUnavailable',
    ...currentEffect(actor),
  })
  assert.equal(actor.getSnapshot().value, 'blockedMigrationFailure')
  assert.equal(legacyCompatibilityUniqueMigrationRuntimeReady(actor.getSnapshot()), false)

  actor.send({
    type: 'EXTERNAL_REPAIR_CONFIRMED',
    migrationId: actor.getSnapshot().context.migrationId,
    scanRevision: actor.getSnapshot().context.scanRevision - 1,
    repairEvidenceDigest: 'sha256:repair-1',
  })
  assert.equal(actor.getSnapshot().value, 'blockedMigrationFailure')
  actor.send({
    type: 'EXTERNAL_REPAIR_CONFIRMED',
    migrationId: actor.getSnapshot().context.migrationId,
    scanRevision: actor.getSnapshot().context.scanRevision,
    repairEvidenceDigest: 'sha256:repair-1',
  })
  assert.equal(actor.getSnapshot().value, 'startupPreflight')
  assert.equal(effects.at(-1)?.effectCheckpoint, 'scanDuplicates')
})

test('repeated migration on an already indexed database is idempotent', () => {
  for (let run = 1; run <= 3; run += 1) {
    const { actor, effects } = actorWithEffects(input({ migrationId: `repeat-${run}` }))
    preflightCompleted(actor, [], true)
    assert.equal(actor.getSnapshot().value, 'ready')
    assert.deepEqual(effects.map((effect) => effect.effectCheckpoint), ['scanDuplicates'])
  }
})
