import assert from 'node:assert/strict'
import test from 'node:test'
import { createActor } from 'xstate'

import {
  createLegacyNotificationCompatibilityActor,
  legacyCompatibilityBackoffSeconds,
  legacyCompatibilityInvariants,
  legacyCompatibilityExpectedEffectReference,
  legacyCompatibilityPortContract,
  legacyCompatibilityRequestKey,
  legacyNotificationCompatibilityMachine,
  legacyRegistrationRolloutMachine,
  type LegacyCompatibilityEffectCheckpoint,
  type LegacyCompatibilityEffectRequested,
  type LegacyCompatibilityEvent,
  type LegacyCompatibilityInput,
  type LegacyCompatibilityRecoveryLease,
} from './legacy-notification-registration-compatibility.machine.ts'

const input = (
  overrides: Partial<LegacyCompatibilityInput> = {},
): LegacyCompatibilityInput => ({
  sagaId: 'saga-1',
  operation: 'register',
  clientGeneration: 'N_MINUS_1',
  authenticatedUserId: 'user-1',
  platform: 'ios',
  legacyPrimaryKeyFingerprint: 'legacy-row-fingerprint',
  legacyInstallationId: 'hmac-installation-id',
  legacyRegistrationId: 'hmac-registration-id',
  targetInstallationId: 'hmac-installation-id',
  targetRegistrationId: null,
  tokenFingerprint: 'token-fingerprint-v1',
  compatibilityGeneration: 1,
  maxAttemptsPerStore: 3,
  initialNowEpochSeconds: 100,
  ...overrides,
})

const actorFor = (overrides: Partial<LegacyCompatibilityInput> = {}) => {
  const actor = createLegacyNotificationCompatibilityActor(input(overrides))
  assert.notEqual(actor, null)
  return actor!
}

const persistIntent = (actor: ReturnType<typeof actorFor>) => {
  const effect = legacyCompatibilityExpectedEffectReference(actor.getSnapshot().context)
  assert.notEqual(effect, null)
  actor.send({
    type: 'INTENT_PERSISTED',
    sagaId: actor.getSnapshot().context.sagaId,
    reconciliationId: `reconciliation-${actor.getSnapshot().context.sagaId}`,
    ...effect!,
  })
}

const currentEffect = (actor: ReturnType<typeof actorFor>) => {
  const effect = legacyCompatibilityExpectedEffectReference(actor.getSnapshot().context)
  assert.notEqual(effect, null)
  return effect!
}

const legacySucceeded = (actor: ReturnType<typeof actorFor>) => actor.send({
  type: 'LEGACY_WRITE_SUCCEEDED',
  sagaId: actor.getSnapshot().context.sagaId,
  outcome: 'applied',
  ...currentEffect(actor),
})

const v2Succeeded = (actor: ReturnType<typeof actorFor>) => actor.send({
  type: 'V2_WRITE_SUCCEEDED',
  sagaId: actor.getSnapshot().context.sagaId,
  outcome: 'applied',
  ...currentEffect(actor),
})

const recordConvergence = (actor: ReturnType<typeof actorFor>) => actor.send({
  type: 'CONVERGENCE_RECORDED',
  sagaId: actor.getSnapshot().context.sagaId,
  ...currentEffect(actor),
})

const legacyFailed = (
  actor: ReturnType<typeof actorFor>,
  failure: 'unavailable' | 'transient' | 'configuration' | 'identityConflict',
) => actor.send({
  type: 'LEGACY_WRITE_FAILED',
  sagaId: actor.getSnapshot().context.sagaId,
  failure,
  ...currentEffect(actor),
})

const v2Failed = (
  actor: ReturnType<typeof actorFor>,
  failure: 'unavailable' | 'transient' | 'configuration' | 'identityConflict',
) => actor.send({
  type: 'V2_WRITE_FAILED',
  sagaId: actor.getSnapshot().context.sagaId,
  failure,
  ...currentEffect(actor),
})

const retryRecorded = (
  actor: ReturnType<typeof actorFor>,
  nextRetryAtEpochSeconds: number,
) => actor.send({
  type: 'RETRY_RECORDED',
  sagaId: actor.getSnapshot().context.sagaId,
  nextRetryAtEpochSeconds,
  ...currentEffect(actor),
})

const advanceClock = (
  actor: ReturnType<typeof actorFor>,
  nowEpochSeconds: number,
) => actor.send({
  type: 'CLOCK_ADVANCED',
  sagaId: actor.getSnapshot().context.sagaId,
  clockRevision: actor.getSnapshot().context.clockRevision + 1,
  nowEpochSeconds,
})

const retryDue = (actor: ReturnType<typeof actorFor>) => actor.send({
  type: 'RETRY_DUE',
  sagaId: actor.getSnapshot().context.sagaId,
  retryScheduleRevision: actor.getSnapshot().context.checkpointRevision,
})

const blockRecorded = (actor: ReturnType<typeof actorFor>) => actor.send({
  type: 'BLOCK_RECORDED',
  sagaId: actor.getSnapshot().context.sagaId,
  ...currentEffect(actor),
})

const acquireRecoveryLease = (
  actor: ReturnType<typeof actorFor>,
  overrides: Partial<Pick<LegacyCompatibilityRecoveryLease,
    'leaseId' | 'holderId' | 'version' | 'fencingToken' | 'expiresAtEpochSeconds'>> = {},
) => {
  const context = actor.getSnapshot().context
  const effect = currentEffect(actor)
  assert.notEqual(context.effectCheckpoint, null)
  actor.send({
    type: 'RECOVERY_LEASE_ACQUIRED',
    sagaId: context.sagaId,
    expectedEffectId: effect.expectedEffectId,
    effectCheckpoint: context.effectCheckpoint!,
    checkpointRevision: effect.checkpointRevision,
    leaseId: 'lease-1',
    holderId: 'worker-a',
    version: 1,
    fencingToken: effect.fencingToken + 10,
    expiresAtEpochSeconds: context.logicalNowEpochSeconds + 10,
    ...overrides,
  })
  return actor.getSnapshot().context.recoveryLease
}

const requestRecovery = (
  actor: ReturnType<typeof actorFor>,
  lease: LegacyCompatibilityRecoveryLease,
  overrides: Partial<Pick<LegacyCompatibilityRecoveryLease,
    'leaseId' | 'holderId' | 'version' | 'fencingToken' | 'effectId' |
    'effectCheckpoint' | 'checkpointRevision'>> = {},
) => actor.send({
  type: 'RECOVERY_REQUESTED',
  sagaId: actor.getSnapshot().context.sagaId,
  expectedEffectId: overrides.effectId ?? lease.effectId,
  effectCheckpoint: overrides.effectCheckpoint ?? lease.effectCheckpoint,
  checkpointRevision: overrides.checkpointRevision ?? lease.checkpointRevision,
  leaseId: overrides.leaseId ?? lease.leaseId,
  holderId: overrides.holderId ?? lease.holderId,
  version: overrides.version ?? lease.version,
  fencingToken: overrides.fencingToken ?? lease.fencingToken,
})

test('N-1 register persists reconciliation before legacy then v2 and terminal convergence', () => {
  const actor = actorFor()
  assert.equal(actor.getSnapshot().value, 'acceptingIntent')
  assert.equal(actor.getSnapshot().context.responseDisposition, 'none')

  persistIntent(actor)
  assert.equal(actor.getSnapshot().value, 'writingLegacy')
  assert.equal(actor.getSnapshot().context.reconciliationStatus, 'pending')
  assert.equal(actor.getSnapshot().context.responseDisposition, 'reconciliationAccepted')

  legacySucceeded(actor)
  assert.equal(actor.getSnapshot().value, 'writingV2')
  v2Succeeded(actor)
  assert.equal(actor.getSnapshot().value, 'recordingConvergence')
  recordConvergence(actor)
  assert.equal(actor.getSnapshot().value, 'converged')
  assert.equal(actor.getSnapshot().context.responseDisposition, 'convergedSuccess')
  assert.equal(actor.getSnapshot().context.legacyWriteStatus, 'applied')
  assert.equal(actor.getSnapshot().context.v2WriteStatus, 'applied')
})

test('N-1 unregister removes only the deterministic v2 legacy association before the legacy row', () => {
  const actor = actorFor({ operation: 'unregister', tokenFingerprint: null })
  persistIntent(actor)
  assert.equal(actor.getSnapshot().value, 'writingV2')
  v2Succeeded(actor)
  assert.equal(actor.getSnapshot().value, 'writingLegacy')
  legacySucceeded(actor)
  assert.equal(actor.getSnapshot().value, 'recordingConvergence')
  recordConvergence(actor)
  assert.equal(actor.getSnapshot().value, 'converged')
  assert.equal(actor.getSnapshot().context.v2TargetKind, 'legacyDeterministicInstallationOnly')
})

test('N clients never write the lossy legacy user-platform token row', () => {
  for (const operation of ['register', 'unregister'] as const) {
    const actor = actorFor({
      sagaId: `saga-n-${operation}`,
      operation,
      clientGeneration: 'N',
      legacyPrimaryKeyFingerprint: null,
      legacyInstallationId: null,
      legacyRegistrationId: null,
      targetInstallationId: 'installation-n',
      targetRegistrationId: operation === 'unregister' ? 'registration-n' : null,
      tokenFingerprint: operation === 'register' ? 'token-n' : null,
    })
    persistIntent(actor)
    assert.equal(actor.getSnapshot().value, 'writingV2')
    assert.equal(actor.getSnapshot().context.legacyWriteStatus, 'notRequired')
    v2Succeeded(actor)
    assert.equal(actor.getSnapshot().value, 'recordingConvergence')
  }
})

test('register and unregister crash after step one resume at step two with the same saga key', () => {
  for (const operation of ['register', 'unregister'] as const) {
    const sagaId = `saga-crash-step-one-${operation}`
    const actor = actorFor({
      sagaId,
      operation,
      tokenFingerprint: operation === 'register' ? 'token-fingerprint-v1' : null,
    })
    persistIntent(actor)
    if (operation === 'register') legacySucceeded(actor)
    else v2Succeeded(actor)
    const requestKey = actor.getSnapshot().context.requestKey
    const persisted = actor.getPersistedSnapshot()
    actor.stop()

    const restarted = createActor(legacyNotificationCompatibilityMachine, {
      snapshot: persisted,
    }).start()
    assert.equal(restarted.getSnapshot().value, operation === 'register' ? 'writingV2' : 'writingLegacy')
    assert.equal(restarted.getSnapshot().context.sagaId, sagaId)
    assert.equal(restarted.getSnapshot().context.requestKey, requestKey)
    assert.equal(
      operation === 'register'
        ? restarted.getSnapshot().context.legacyWriteStatus
        : restarted.getSnapshot().context.v2WriteStatus,
      'applied',
    )
  }
})

test('register and unregister crash after both writes repeat only convergence recording', () => {
  for (const operation of ['register', 'unregister'] as const) {
    const sagaId = `saga-crash-step-two-${operation}`
    const actor = actorFor({
      sagaId,
      operation,
      tokenFingerprint: operation === 'register' ? 'token-fingerprint-v1' : null,
    })
    persistIntent(actor)
    if (operation === 'register') {
      legacySucceeded(actor)
      v2Succeeded(actor)
    } else {
      v2Succeeded(actor)
      legacySucceeded(actor)
    }
    const persisted = actor.getPersistedSnapshot()
    actor.stop()

    const restarted = createActor(legacyNotificationCompatibilityMachine, {
      snapshot: persisted,
    }).start()
    assert.equal(restarted.getSnapshot().value, 'recordingConvergence')
    assert.equal(restarted.getSnapshot().context.legacyWriteStatus, 'applied')
    assert.equal(restarted.getSnapshot().context.v2WriteStatus, 'applied')
    restarted.send({
      type: 'CONVERGENCE_RECORDED',
      sagaId,
      ...currentEffect(restarted),
    })
    assert.equal(restarted.getSnapshot().value, 'converged')
  }
})

test('durable fenced lease authority survives restore and emits each recovered checkpoint once', () => {
  const cases: ReadonlyArray<{
    name: string
    expectedState: string
    expectedEffect: LegacyCompatibilityEffectCheckpoint
    prepare: (actor: ReturnType<typeof actorFor>) => void
    overrides?: Partial<LegacyCompatibilityInput>
  }> = [
    {
      name: 'pending reconciliation persistence',
      expectedState: 'acceptingIntent',
      expectedEffect: 'persistPendingReconciliation',
      prepare: () => {},
    },
    {
      name: 'legacy write',
      expectedState: 'writingLegacy',
      expectedEffect: 'writeLegacyStore',
      prepare: persistIntent,
    },
    {
      name: 'v2 write',
      expectedState: 'writingV2',
      expectedEffect: 'writeV2RegistrationStore',
      prepare: (actor) => {
        persistIntent(actor)
        legacySucceeded(actor)
      },
    },
    {
      name: 'retry persistence',
      expectedState: 'recordingRetry',
      expectedEffect: 'persistRetrySchedule',
      prepare: (actor) => {
        persistIntent(actor)
        legacyFailed(actor, 'unavailable')
      },
    },
    {
      name: 'convergence persistence',
      expectedState: 'recordingConvergence',
      expectedEffect: 'persistConvergence',
      prepare: (actor) => {
        persistIntent(actor)
        legacySucceeded(actor)
        v2Succeeded(actor)
      },
    },
    {
      name: 'blocked terminal persistence',
      expectedState: 'recordingBlock',
      expectedEffect: 'persistBlockedTerminal',
      overrides: { maxAttemptsPerStore: 1 },
      prepare: (actor) => {
        persistIntent(actor)
        legacyFailed(actor, 'unavailable')
      },
    },
  ]

  for (const recoveryCase of cases) {
    const sagaId = `saga-recovery-${recoveryCase.expectedEffect}`
    const source = actorFor({ sagaId, ...recoveryCase.overrides })
    recoveryCase.prepare(source)
    assert.equal(source.getSnapshot().value, recoveryCase.expectedState, recoveryCase.name)
    assert.equal(source.getSnapshot().context.effectCheckpoint, recoveryCase.expectedEffect, recoveryCase.name)
    const persistedWithoutLease = source.getPersistedSnapshot()
    source.stop()

    const acquiring = createActor(legacyNotificationCompatibilityMachine, {
      snapshot: persistedWithoutLease,
    }).start()
    const lease = acquireRecoveryLease(acquiring, {
      leaseId: `lease-${recoveryCase.expectedEffect}`,
      holderId: `worker-${recoveryCase.expectedEffect}`,
    })
    assert.notEqual(lease, null, `${recoveryCase.name}: durable lease acquisition is recorded`)
    assert.equal(lease?.effectCheckpoint, recoveryCase.expectedEffect)
    const persistedWithLease = acquiring.getPersistedSnapshot()
    acquiring.stop()

    const restarted = createActor(legacyNotificationCompatibilityMachine, {
      snapshot: persistedWithLease,
    }).start()
    const effects: LegacyCompatibilityEffectRequested[] = []
    restarted.on('LEGACY_COMPATIBILITY_EFFECT_REQUESTED', (effect) => effects.push(effect))

    assert.equal(effects.length, 0, `${recoveryCase.name}: start does not replay an XState entry`)
    assert.deepEqual(restarted.getSnapshot().context.recoveryLease, lease)

    requestRecovery(restarted, lease!, { holderId: 'foreign-worker' })
    assert.equal(effects.length, 0, `${recoveryCase.name}: a foreign holder is rejected`)
    assert.equal(restarted.getSnapshot().context.recoveryCount, 0)

    requestRecovery(restarted, lease!)
    assert.equal(effects.length, 1, `${recoveryCase.name}: recovery emits the pending effect`)
    assert.equal(effects[0]?.effectCheckpoint, recoveryCase.expectedEffect)
    assert.equal(effects[0]?.cause, 'recovery')
    assert.equal(effects[0]?.recoveryLeaseId, `lease-${recoveryCase.expectedEffect}`)
    assert.equal(effects[0]?.recoveryLeaseHolderId, `worker-${recoveryCase.expectedEffect}`)
    assert.equal(effects[0]?.checkpointRevision, lease?.checkpointRevision)
    assert.equal(effects[0]?.fencingToken, lease?.fencingToken)
    assert.equal(restarted.getSnapshot().context.recoveryCount, 1)

    requestRecovery(restarted, lease!)
    assert.equal(effects.length, 1, `${recoveryCase.name}: concurrent recovery cannot emit twice`)
    assert.equal(restarted.getSnapshot().context.recoveryCount, 1)
  }
})

test('expired, foreign and stale fencing recovery authority is rejected deterministically', () => {
  const source = actorFor({ sagaId: 'saga-fenced-recovery' })
  persistIntent(source)
  const persisted = source.getPersistedSnapshot()
  source.stop()

  const actor = createActor(legacyNotificationCompatibilityMachine, { snapshot: persisted }).start()
  const effects: LegacyCompatibilityEffectRequested[] = []
  actor.on('LEGACY_COMPATIBILITY_EFFECT_REQUESTED', (effect) => effects.push(effect))
  const firstLease = acquireRecoveryLease(actor, {
    leaseId: 'lease-v1', holderId: 'worker-a', version: 1,
    fencingToken: 20, expiresAtEpochSeconds: 110,
  })
  assert.notEqual(firstLease, null)

  requestRecovery(actor, firstLease!, { holderId: 'worker-b' })
  assert.equal(effects.length, 0, 'foreign holder cannot use an active lease')
  advanceClock(actor, 110)
  advanceClock(actor, 109)
  assert.equal(actor.getSnapshot().context.logicalNowEpochSeconds, 110)
  requestRecovery(actor, firstLease!)
  assert.equal(effects.length, 0, 'lease is expired at its logical expiration boundary')

  const rejectedOldLease = acquireRecoveryLease(actor, {
    leaseId: 'lease-stale', holderId: 'worker-c', version: 1,
    fencingToken: 20, expiresAtEpochSeconds: 120,
  })
  assert.equal(rejectedOldLease?.leaseId, 'lease-v1', 'old version and fencing cannot replace the lease')

  const secondLease = acquireRecoveryLease(actor, {
    leaseId: 'lease-v2', holderId: 'worker-c', version: 2,
    fencingToken: 21, expiresAtEpochSeconds: 120,
  })
  assert.equal(secondLease?.leaseId, 'lease-v2')
  requestRecovery(actor, secondLease!, {
    leaseId: firstLease!.leaseId,
    holderId: firstLease!.holderId,
    version: firstLease!.version,
    fencingToken: firstLease!.fencingToken,
  })
  assert.equal(effects.length, 0, 'old fencing cannot recover after a new lease is durable')
  requestRecovery(actor, secondLease!)
  assert.equal(effects.length, 1)
  assert.equal(effects[0]?.fencingToken, 21)
})

test('duplicate requests coalesce onto one durable saga', () => {
  const actor = actorFor()
  persistIntent(actor)
  const requestKey = actor.getSnapshot().context.requestKey
  actor.send({ type: 'DUPLICATE_REQUEST_RECEIVED', requestKey })
  actor.send({ type: 'DUPLICATE_REQUEST_RECEIVED', requestKey })
  assert.equal(actor.getSnapshot().value, 'writingLegacy')
  assert.equal(actor.getSnapshot().context.duplicateCount, 2)
  assert.equal(actor.getSnapshot().context.requestKey, requestKey)

  assert.equal(
    legacyCompatibilityRequestKey('register', 'user-1', 'hmac-registration-id', 'token-fingerprint-v1', 1),
    requestKey,
  )
  assert.notEqual(
    legacyCompatibilityRequestKey('register', 'user-1', 'hmac-registration-id', 'token-fingerprint-v2', 1),
    requestKey,
  )
  assert.notEqual(
    legacyCompatibilityRequestKey('register', 'user-1', 'hmac-registration-id', 'token-fingerprint-v1', 2),
    requestKey,
  )
  assert.notEqual(
    legacyCompatibilityRequestKey('register', 'user-2', 'hmac-registration-id', 'token-fingerprint-v1', 1),
    requestKey,
  )
})

test('canonical compatibility request keys cannot collide across delimiter-bearing tuple fields', () => {
  assert.notEqual(
    legacyCompatibilityRequestKey('register', 'user:other', 'target', 'token-fingerprint-v1', 1),
    legacyCompatibilityRequestKey('register', 'user', 'other:target', 'token-fingerprint-v1', 1),
    'canonical typed tuples cannot collide when delimiters move between subject and target',
  )
})

test('store retries are independent, durable and idempotent', () => {
  const actor = actorFor()
  persistIntent(actor)
  legacyFailed(actor, 'unavailable')
  assert.equal(actor.getSnapshot().value, 'recordingRetry')
  assert.equal(actor.getSnapshot().context.legacyAttempt, 1)
  assert.equal(actor.getSnapshot().context.v2Attempt, 0)
  retryRecorded(actor, 110)
  retryDue(actor)
  assert.equal(actor.getSnapshot().value, 'retryWait')
  advanceClock(actor, 109)
  retryDue(actor)
  assert.equal(actor.getSnapshot().value, 'retryWait')
  advanceClock(actor, 110)
  retryDue(actor)
  assert.equal(actor.getSnapshot().value, 'writingLegacy')
  actor.send({
    type: 'LEGACY_WRITE_SUCCEEDED', sagaId: 'saga-1', outcome: 'alreadyApplied',
    ...currentEffect(actor),
  })
  v2Failed(actor, 'transient')
  assert.equal(actor.getSnapshot().context.legacyAttempt, 1)
  assert.equal(actor.getSnapshot().context.v2Attempt, 1)
  assert.equal(actor.getSnapshot().context.legacyWriteStatus, 'applied')
})

test('retry eligibility uses only the durable monotone clock and a strictly future schedule', () => {
  const actor = actorFor({ sagaId: 'saga-authoritative-clock' })
  persistIntent(actor)
  legacyFailed(actor, 'unavailable')
  assert.equal(actor.getSnapshot().value, 'recordingRetry')

  retryRecorded(actor, 100)
  retryRecorded(actor, 99)
  assert.equal(actor.getSnapshot().value, 'recordingRetry', 'present or past schedule is rejected')
  assert.equal(actor.getSnapshot().context.nextRetryAtEpochSeconds, null)

  retryRecorded(actor, 110)
  assert.equal(actor.getSnapshot().value, 'retryWait')
  const retryScheduleRevision = actor.getSnapshot().context.checkpointRevision
  actor.send({
    type: 'RETRY_DUE',
    sagaId: 'saga-authoritative-clock',
    retryScheduleRevision,
    nowEpochSeconds: 999,
  } as unknown as LegacyCompatibilityEvent)
  assert.equal(actor.getSnapshot().value, 'retryWait', 'a caller-supplied future cannot bypass the clock')

  advanceClock(actor, 109)
  retryDue(actor)
  assert.equal(actor.getSnapshot().value, 'retryWait', 'clock below deadline remains waiting')
  const persisted = actor.getPersistedSnapshot()
  actor.stop()

  const restarted = createActor(legacyNotificationCompatibilityMachine, { snapshot: persisted }).start()
  assert.equal(restarted.getSnapshot().context.logicalNowEpochSeconds, 109)
  assert.equal(restarted.getSnapshot().context.clockRevision, 1)
  retryDue(restarted)
  assert.equal(restarted.getSnapshot().value, 'retryWait', 'restore preserves clock authority')

  advanceClock(restarted, 110)
  retryDue(restarted)
  assert.equal(restarted.getSnapshot().value, 'writingLegacy', 'deadline makes retry eligible')
  advanceClock(restarted, 109)
  assert.equal(restarted.getSnapshot().context.logicalNowEpochSeconds, 110, 'clock rewind is refused')
  assert.equal(restarted.getSnapshot().context.clockRevision, 2)
})

test('effect acknowledgements are fenced against delayed callbacks from an earlier checkpoint', () => {
  const actor = actorFor({ sagaId: 'saga-delayed-ack' })
  persistIntent(actor)
  const c1 = currentEffect(actor)
  actor.send({
    type: 'LEGACY_WRITE_FAILED', sagaId: 'saga-delayed-ack', failure: 'unavailable', ...c1,
  })
  assert.equal(actor.getSnapshot().value, 'recordingRetry')
  assert.equal(actor.getSnapshot().context.legacyAttempt, 1)
  const c2 = currentEffect(actor)
  assert.notEqual(c2.expectedEffectId, c1.expectedEffectId)
  assert.ok(c2.checkpointRevision > c1.checkpointRevision)
  assert.ok(c2.fencingToken > c1.fencingToken)

  actor.send({
    type: 'LEGACY_WRITE_SUCCEEDED', sagaId: 'saga-delayed-ack', outcome: 'applied', ...c1,
  })
  actor.send({
    type: 'LEGACY_WRITE_FAILED', sagaId: 'saga-delayed-ack', failure: 'transient', ...c1,
  })
  actor.send({
    type: 'RETRY_RECORDED', sagaId: 'saga-delayed-ack', nextRetryAtEpochSeconds: 105, ...c1,
  })
  assert.equal(actor.getSnapshot().value, 'recordingRetry')
  assert.equal(actor.getSnapshot().context.legacyAttempt, 1)
  assert.equal(actor.getSnapshot().context.nextRetryAtEpochSeconds, null)

  actor.send({
    type: 'RETRY_RECORDED', sagaId: 'saga-delayed-ack', nextRetryAtEpochSeconds: 105, ...c2,
  })
  assert.equal(actor.getSnapshot().value, 'retryWait')
  advanceClock(actor, 105)
  retryDue(actor)
  assert.equal(actor.getSnapshot().value, 'writingLegacy')
  actor.send({
    type: 'LEGACY_WRITE_FAILED', sagaId: 'saga-delayed-ack', failure: 'transient', ...c1,
  })
  assert.equal(actor.getSnapshot().value, 'writingLegacy')
  assert.equal(actor.getSnapshot().context.legacyAttempt, 1)
  assert.equal(actor.getSnapshot().context.nextRetryAtEpochSeconds, null)
})

test('retry exhaustion records a durable blocked terminal and never success', () => {
  const actor = actorFor({ maxAttemptsPerStore: 1 })
  persistIntent(actor)
  legacyFailed(actor, 'unavailable')
  assert.equal(actor.getSnapshot().value, 'recordingBlock')
  assert.equal(actor.getSnapshot().context.responseDisposition, 'reconciliationAccepted')
  blockRecorded(actor)
  assert.equal(actor.getSnapshot().value, 'blocked')
  assert.equal(actor.getSnapshot().context.responseDisposition, 'blockedFailure')
  assert.notEqual(actor.getSnapshot().context.responseDisposition, 'convergedSuccess')
})

test('retry backoff is deterministic, bounded and store scheduling is durable', () => {
  assert.equal(legacyCompatibilityBackoffSeconds(1, 0), 1, 'zero jitter still schedules strictly in the future')
  assert.equal(legacyCompatibilityBackoffSeconds(1, 1), 1)
  assert.equal(legacyCompatibilityBackoffSeconds(4, 0.5), 4)
  assert.equal(legacyCompatibilityBackoffSeconds(4, -1), 1, 'samples below zero clamp to the future-safe lower bound')
  assert.equal(legacyCompatibilityBackoffSeconds(4, 2), 8, 'samples above one clamp to the full window')
  assert.equal(legacyCompatibilityBackoffSeconds(4, Number.NaN), 1, 'NaN follows the fail-closed lower-bound policy')
  assert.equal(legacyCompatibilityBackoffSeconds(4, Number.POSITIVE_INFINITY), 8)
  assert.equal(legacyCompatibilityBackoffSeconds(10, 1), 300, 'unit sample reaches the capped full-jitter window')

  for (const attempt of [99, 2_147_483_647, Number.MAX_SAFE_INTEGER]) {
    const delay = legacyCompatibilityBackoffSeconds(attempt, 1)
    assert.ok(delay > 0, `attempt ${attempt} remains strictly future`)
    assert.ok(delay <= 300, `attempt ${attempt} remains capped without exponent overflow`)
    assert.equal(delay, 300)
  }

  const actor = actorFor({ sagaId: 'saga-zero-jitter-future' })
  persistIntent(actor)
  legacyFailed(actor, 'unavailable')
  const delay = legacyCompatibilityBackoffSeconds(actor.getSnapshot().context.legacyAttempt, 0)
  retryRecorded(actor, actor.getSnapshot().context.logicalNowEpochSeconds + delay)
  assert.equal(actor.getSnapshot().value, 'retryWait')
  assert.equal(actor.getSnapshot().context.nextRetryAtEpochSeconds, 101)
})

test('uncorrelated and conflicting callbacks cannot advance the saga', () => {
  const actor = actorFor()
  actor.send({
    type: 'INTENT_PERSISTED', sagaId: 'other', reconciliationId: 'foreign',
    ...currentEffect(actor),
  })
  assert.equal(actor.getSnapshot().value, 'acceptingIntent')
  persistIntent(actor)
  actor.send({
    type: 'LEGACY_WRITE_FAILED', sagaId: 'other', failure: 'configuration',
    ...currentEffect(actor),
  })
  assert.equal(actor.getSnapshot().value, 'writingLegacy')
  legacyFailed(actor, 'identityConflict')
  assert.equal(actor.getSnapshot().value, 'recordingBlock')
})

test('invalid legacy identity, fingerprint or generation is rejected before actor creation', () => {
  assert.equal(createLegacyNotificationCompatibilityActor(input({ legacyInstallationId: null })), null)
  assert.equal(createLegacyNotificationCompatibilityActor(input({ legacyPrimaryKeyFingerprint: null })), null)
  assert.equal(createLegacyNotificationCompatibilityActor(input({ legacyRegistrationId: null })), null)
  assert.equal(createLegacyNotificationCompatibilityActor(input({ tokenFingerprint: '' })), null)
  assert.equal(createLegacyNotificationCompatibilityActor(input({ compatibilityGeneration: 0 })), null)
  assert.equal(createLegacyNotificationCompatibilityActor(input({ initialNowEpochSeconds: -1 })), null)
  assert.ok(legacyCompatibilityInvariants.includes(
    'legacy identity is derived from the immutable legacy primary key by a configured stable HMAC and raw identity material never enters the saga snapshot',
  ))
})

test('cutover and rollback require a drained reconciliation checkpoint and preserve v2 rows', () => {
  const actor = createActor(legacyRegistrationRolloutMachine, {
    input: { initialReadAuthority: 'legacy' },
  }).start()
  actor.send({ type: 'CUTOVER_REQUESTED', checkpointId: 'cutover-1' })
  assert.equal(actor.getSnapshot().value, 'cutoverCheckpointPending')
  actor.send({
    type: 'CUTOVER_CHECKPOINT_CONFIRMED', checkpointId: 'cutover-1',
    pendingReconciliationCount: 0, legacyWriterPaused: true, uniquenessConfirmed: true,
  })
  assert.equal(actor.getSnapshot().value, 'v2Authoritative')
  assert.equal(actor.getSnapshot().context.readAuthority, 'v2')

  actor.send({ type: 'ROLLBACK_REQUESTED', checkpointId: 'rollback-1' })
  actor.send({
    type: 'ROLLBACK_CHECKPOINT_CONFIRMED', checkpointId: 'rollback-1',
    pendingReconciliationCount: 0, v2WriterPaused: true,
    legacyProjectionReady: true, reconciliationCheckpointConfirmed: true,
  })
  assert.equal(actor.getSnapshot().value, 'legacyAuthoritative')
  assert.equal(actor.getSnapshot().context.readAuthority, 'legacy')
  assert.equal(actor.getSnapshot().context.v2RowsPreserved, true)
})

test('unsafe cutover is rejected without creating two authorities', () => {
  const actor = createActor(legacyRegistrationRolloutMachine, {
    input: { initialReadAuthority: 'legacy' },
  }).start()
  actor.send({ type: 'CUTOVER_REQUESTED', checkpointId: 'cutover-unsafe' })
  actor.send({
    type: 'CUTOVER_CHECKPOINT_CONFIRMED', checkpointId: 'cutover-unsafe',
    pendingReconciliationCount: 2, legacyWriterPaused: false, uniquenessConfirmed: false,
  })
  assert.equal(actor.getSnapshot().value, 'legacyAuthoritative')
  assert.equal(actor.getSnapshot().context.readAuthority, 'legacy')
  assert.equal(actor.getSnapshot().context.lastCheckpointFailure, 'checkpointNotSafe')
})

test('the reviewed port contract forbids cross-store atomicity and destructive compensation', () => {
  assert.equal(legacyCompatibilityPortContract.transactionBoundary, 'one-durable-store-commit-at-a-time')
  assert.equal(legacyCompatibilityPortContract.compensationPolicy, 'forward-reconciliation-only')
  assert.equal(legacyCompatibilityPortContract.v2DeletionScope, 'exact-registration-or-legacy-deterministic-installation-only')
  assert.equal(legacyCompatibilityPortContract.pendingReconciliation.requiredBeforeStoreWrites, true)
  assert.equal(legacyCompatibilityPortContract.response.successTrueOnlyAfter, 'durable-convergence')
  assert.equal(legacyCompatibilityPortContract.recoveryWorker.restoredEntriesReplayAutomatically, false)
  assert.equal(legacyCompatibilityPortContract.recoveryWorker.leaseRequired, true)
  assert.equal(legacyCompatibilityPortContract.recoveryWorker.concurrentEmission, 'at-most-once-per-recorded-lease')
  assert.deepEqual(
    legacyCompatibilityPortContract.effectAcknowledgement.requiredReference,
    ['expected-effect-id', 'checkpoint-revision', 'authority-fencing-token'],
  )
  assert.equal(legacyCompatibilityPortContract.retryClock.dueEventCarriesTime, false)
  assert.equal(
    legacyCompatibilityPortContract.retryClock.scheduleRule,
    'nextRetryAtEpochSeconds-strictly-greater-than-logicalNowEpochSeconds',
  )
  assert.equal(legacyCompatibilityPortContract.idempotency.requestKeyEncoding, 'canonical-json-typed-tuple-v2')
})
