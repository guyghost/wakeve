import assert from 'node:assert/strict'
import test from 'node:test'
import { createActor } from 'xstate'
import {
  buildBackendIngestionTransaction,
  calendarArtifactIdentity,
  classifyApnsResponse,
  effectIdentity,
  effectIdentityMigration,
  epochSeconds,
  fullJitterBackoffSeconds,
  migrateLegacyEffectIdentity,
  notificationCalendarArtifactMachine,
  notificationDeliveryAuthorityMachine,
  notificationDeliveryMachine,
  notificationIngestionMachine,
  notificationRecipientTargetMachine,
} from './notification-delivery.machine.ts'

const epoch = epochSeconds
const deliveryInput = (extra = {}) => ({
  deliveryKey: 'recipient-1:registration-1:apns', registrationId: 'registration-1',
  authority: 'outbox-v2' as const, authorityFencingToken: 11,
  expiresAtEpochSeconds: epoch(1_000), nowEpochSeconds: epoch(100), maxAttempts: 3,
  ...extra,
})
const lease = (actor: ReturnType<typeof createActor<typeof notificationDeliveryMachine>>, extra = {}) => {
  actor.send({ type: 'POLICY_ALLOWED' })
  actor.send({
    type: 'DELIVERY_LEASE_DURABLY_ACQUIRED', deliveryKey: actor.getSnapshot().context.deliveryKey,
    holderId: 'worker-1', leaseVersion: 4, fencingToken: 11,
    leaseExpiresAtEpochSeconds: epoch(500), expectedCheckpointRevision: actor.getSnapshot().context.checkpointRevision,
    ...extra,
  })
  const snapshot = actor.getSnapshot()
  actor.send({
    type: 'PROVIDER_AUTH_READY', deliveryKey: snapshot.context.deliveryKey,
    correlationId: snapshot.context.correlationId!, attempt: snapshot.context.attempt,
    leaseHolderId: snapshot.context.lease!.holderId, leaseVersion: snapshot.context.lease!.version,
    fencingToken: snapshot.context.lease!.fencingToken,
  })
}
const observationRef = (actor: ReturnType<typeof createActor<typeof notificationDeliveryMachine>>, extra = {}) => ({
  deliveryKey: actor.getSnapshot().context.deliveryKey,
  correlationId: actor.getSnapshot().context.correlationId!, attempt: actor.getSnapshot().context.attempt,
  leaseHolderId: 'worker-1', leaseVersion: 4, fencingToken: 11,
  ...extra,
})
const providerCheckpointRef = (actor: ReturnType<typeof createActor<typeof notificationDeliveryMachine>>, extra = {}) => {
  const checkpoint = actor.getSnapshot().context.pendingCheckpoint!
  return {
    effectId: checkpoint.effectId, checkpointRevision: checkpoint.revision,
    authority: 'outbox-v2' as const, authorityFencingToken: 11,
    leaseHolderId: checkpoint.leaseHolderId, leaseVersion: checkpoint.leaseVersion, leaseFencingToken: checkpoint.leaseFencingToken,
    ...extra,
  }
}

test('HTTP status is the only outcome authority and 503 cannot be caller-forced to accepted', () => {
  const actor = createActor(notificationDeliveryMachine, { input: deliveryInput() }).start()
  lease(actor)
  assert.equal(actor.getSnapshot().value, 'sending')
  actor.send({
    type: 'PROVIDER_HTTP_OBSERVED', status: 503, jitterSample: 0,
    ...observationRef(actor), result: 'accepted',
  } as never)
  assert.equal(actor.getSnapshot().value, 'awaitingProviderResultPersistence')
  assert.equal(actor.getSnapshot().context.pendingCheckpoint?.outcome, 'retry')
  assert.equal(actor.getSnapshot().context.deliveryStatus, 'sending')
})

test('stale correlation, attempt, delivery and foreign lease observations are ignored', () => {
  const mutations = [
    { correlationId: 'stale' }, { attempt: 9 }, { deliveryKey: 'foreign' },
    { leaseHolderId: 'foreign' }, { leaseVersion: 3 }, { fencingToken: 10 },
  ]
  for (const mutation of mutations) {
    const actor = createActor(notificationDeliveryMachine, { input: deliveryInput() }).start()
    lease(actor)
    actor.send({ type: 'PROVIDER_HTTP_OBSERVED', status: 200, ...observationRef(actor, mutation) })
    assert.equal(actor.getSnapshot().value, 'sending', JSON.stringify(mutation))
    assert.equal(actor.getSnapshot().context.pendingCheckpoint, null, JSON.stringify(mutation))
  }
})

test('provider result cannot terminalize before persistence emission and exact durable ACK', () => {
  const actor = createActor(notificationDeliveryMachine, { input: deliveryInput() }).start()
  lease(actor)
  actor.send({ type: 'PROVIDER_HTTP_OBSERVED', status: 200, acceptedAtEpochSeconds: epoch(101), ...observationRef(actor) })
  const reference = providerCheckpointRef(actor)
  actor.send({ type: 'PROVIDER_RESULT_DURABLY_RECORDED', ...reference })
  assert.equal(actor.getSnapshot().value, 'awaitingProviderResultPersistence')
  assert.equal(actor.getSnapshot().context.acceptedAtEpochSeconds, null)
  actor.send({ type: 'PROVIDER_RESULT_PERSISTENCE_REQUESTED', ...reference })
  actor.send({ type: 'PROVIDER_RESULT_DURABLY_RECORDED', ...reference, effectId: 'stale' })
  assert.equal(actor.getSnapshot().value, 'awaitingProviderResultPersistence')
  actor.send({ type: 'PROVIDER_RESULT_DURABLY_RECORDED', ...reference })
  assert.equal(actor.getSnapshot().value, 'accepted')
  assert.equal(actor.getSnapshot().context.acceptedAtEpochSeconds, 101)
})

test('provider checkpoint restore requires explicit idempotent recovery request', () => {
  const actor = createActor(notificationDeliveryMachine, { input: deliveryInput() }).start()
  lease(actor)
  actor.send({ type: 'PROVIDER_HTTP_OBSERVED', status: 410, reason: 'Unregistered', ...observationRef(actor) })
  const reference = providerCheckpointRef(actor)
  const restored = createActor(notificationDeliveryMachine, { snapshot: actor.getPersistedSnapshot() }).start()
  assert.equal(restored.getSnapshot().context.persistenceEffectEmitted, false)
  restored.send({ type: 'PROVIDER_RESULT_DURABLY_RECORDED', ...reference })
  assert.equal(restored.getSnapshot().value, 'awaitingProviderResultPersistence')
  restored.send({ type: 'PROVIDER_RESULT_PERSISTENCE_REQUESTED', ...reference })
  restored.send({ type: 'PROVIDER_RESULT_DURABLY_RECORDED', ...reference })
  assert.equal(restored.getSnapshot().value, 'invalidToken')
})

test('backend ingestion transaction owns receipt, logical notification, recipients and resolved deliveries before I/O', () => {
  const resolved = buildBackendIngestionTransaction({
    domainEventId: 'tenant:event:42', effectType: 'DATE_CONFIRMED', schemaVersion: 2,
    participantId: 'participant-1', channel: 'push', provider: 'apns', registrationIds: ['r-2', 'r-1', 'r-2'],
  })
  assert.deepEqual(resolved.transactionalTables, ['domain_event_ingestion', 'notification_logical', 'notification_recipient', 'notification_delivery'])
  assert.equal(resolved.deliveries.length, 2)
  assert.equal(resolved.providerIoBeforeCommit, false)
  assert.equal(resolved.localOutboxWrites, 0)
  const pending = buildBackendIngestionTransaction({ ...resolved.source, registrationIds: [] })
  assert.deepEqual(pending.transactionalTables, ['domain_event_ingestion', 'notification_logical', 'notification_recipient'])
  assert.equal(pending.recipientStatus, 'pendingTarget')

  const actor = createActor(notificationIngestionMachine, { input: resolved.source }).start()
  assert.deepEqual(actor.getSnapshot().context.transactionPlan.deliveryKeys, resolved.transactionPlan.deliveryKeys)
  actor.send({ type: 'BACKEND_INGESTION_TRANSACTION_COMMITTED', transactionId: 'tx-1', effectKey: resolved.effectKey, transactionDigest: resolved.transactionDigest })
  assert.equal(actor.getSnapshot().value, 'backendIngestionCommitted')
})

const persistableTargetReference = (actor: ReturnType<typeof createActor<typeof notificationRecipientTargetMachine>>) => {
  const checkpoint = actor.getSnapshot().context.pendingCheckpoint!
  return { effectId: checkpoint.effectId, checkpointRevision: checkpoint.revision, transactionReceiptId: checkpoint.transactionReceiptId, holderId: checkpoint.holderId, leaseVersion: checkpoint.leaseVersion, fencingToken: checkpoint.fencingToken }
}
const persistTargetCheckpoint = (actor: ReturnType<typeof createActor<typeof notificationRecipientTargetMachine>>) => {
  const ref = persistableTargetReference(actor)
  actor.send({ type: 'TARGET_CHECKPOINT_DURABLE_EFFECT_REQUESTED', ...ref })
  actor.send({ type: 'TARGET_CHECKPOINT_DURABLY_RECORDED', ...ref })
}
const targetLease = (actor: ReturnType<typeof createActor<typeof notificationRecipientTargetMachine>>) => {
  actor.send({ type: 'TARGET_LEASE_DURABLY_ACQUIRED', holderId: 'resolver-1', leaseVersion: 1, fencingToken: 1, expiresAtEpochSeconds: epoch(19), expectedCheckpointRevision: actor.getSnapshot().context.checkpointRevision })
}
const targetLeaseReference = { holderId: 'resolver-1', leaseVersion: 1, fencingToken: 1 }

test('pending target retry and expiry use distinct durable checkpoints and a monotone clock', () => {
  const actor = createActor(notificationRecipientTargetMachine, {
    input: { recipientKey: 'recipient-1', provider: 'apns', expiresAtEpochSeconds: epoch(20), nowEpochSeconds: epoch(0), maxAttempts: 2, nextAttemptAtEpochSeconds: epoch(10) },
  }).start()
  actor.send({ type: 'CLOCK_DURABLY_ADVANCED', expectedClockRevision: 0, newEpochSeconds: epoch(9) })
  actor.send({ type: 'TARGET_RESOLUTION_DUE' })
  assert.equal(actor.getSnapshot().value, 'pendingTarget')
  actor.send({ type: 'CLOCK_DURABLY_ADVANCED', expectedClockRevision: 1, newEpochSeconds: epoch(10) })
  actor.send({ type: 'CLOCK_DURABLY_ADVANCED', expectedClockRevision: 2, newEpochSeconds: epoch(8) })
  assert.equal(actor.getSnapshot().context.nowEpochSeconds, 10)
  actor.send({ type: 'TARGET_RESOLUTION_DUE' })
  targetLease(actor)
  actor.send({ type: 'NO_REGISTRATIONS_RESOLVED', jitterSample: 0, ...targetLeaseReference })
  assert.equal(actor.getSnapshot().value, 'awaitingTargetRetryPersistence')
  assert.equal(actor.getSnapshot().context.pendingCheckpoint?.kind, 'retry')
  actor.send({ type: 'TARGET_CHECKPOINT_DURABLY_RECORDED', ...persistableTargetReference(actor) })
  assert.equal(actor.getSnapshot().value, 'awaitingTargetRetryPersistence')
  persistTargetCheckpoint(actor)
  assert.equal(actor.getSnapshot().context.attempt, 1)
  assert.equal(actor.getSnapshot().context.nextAttemptAtEpochSeconds, 11)
  actor.send({ type: 'CLOCK_DURABLY_ADVANCED', expectedClockRevision: 2, newEpochSeconds: epoch(20) })
  actor.send({ type: 'TARGET_EXPIRY_DETECTED' })
  assert.equal(actor.getSnapshot().value, 'awaitingTargetExpiryPersistence')
  assert.equal(actor.getSnapshot().context.pendingCheckpoint?.kind, 'expiry')
  persistTargetCheckpoint(actor)
  assert.equal(actor.getSnapshot().value, 'targetExpired')
})

test('resolved target fan-out is a separate crash-safe transaction and freezes its registration set', () => {
  const actor = createActor(notificationRecipientTargetMachine, {
    input: { recipientKey: 'recipient-1', provider: 'apns', expiresAtEpochSeconds: epoch(100), nowEpochSeconds: epoch(10), maxAttempts: 3, nextAttemptAtEpochSeconds: epoch(10) },
  }).start()
  actor.send({ type: 'TARGET_RESOLUTION_DUE' })
  targetLease(actor)
  actor.send({ type: 'REGISTRATIONS_RESOLVED', registrationIds: ['r-2', 'r-1', 'r-2'], ...targetLeaseReference })
  assert.equal(actor.getSnapshot().value, 'awaitingTargetFanoutPersistence')
  assert.equal(actor.getSnapshot().context.pendingCheckpoint?.kind, 'fanout')
  assert.equal(actor.getSnapshot().context.deliveries.length, 0)
  const restored = createActor(notificationRecipientTargetMachine, { snapshot: actor.getPersistedSnapshot() }).start()
  persistTargetCheckpoint(restored)
  assert.equal(restored.getSnapshot().value, 'targeted')
  assert.deepEqual(restored.getSnapshot().context.deliveries.map((it) => it.registrationId), ['r-1', 'r-2'])
  restored.send({ type: 'REGISTRATIONS_RESOLVED', registrationIds: ['r-3'], ...targetLeaseReference })
  assert.deepEqual(restored.getSnapshot().context.deliveries.map((it) => it.registrationId), ['r-1', 'r-2'])
})

test('full jitter is injected, overflow-safe and 5xx without reason stays retryable', () => {
  const cases = [
    [1, 0, 1], [1, Number.NaN, 1], [1, 1, 1], [30, 999, 300], [Number.MAX_SAFE_INTEGER, 1, 300],
  ] as const
  for (const [attempt, sample, expected] of cases) assert.equal(fullJitterBackoffSeconds(attempt, sample), expected)
  assert.equal(classifyApnsResponse(503), 'retry')

  const actor = createActor(notificationDeliveryMachine, { input: deliveryInput() }).start()
  lease(actor)
  actor.send({ type: 'PROVIDER_HTTP_OBSERVED', status: 503, jitterSample: Number.NaN, ...observationRef(actor) })
  assert.equal(actor.getSnapshot().context.pendingCheckpoint?.reason, 'http5xx')
  assert.equal(actor.getSnapshot().context.pendingCheckpoint?.nextAttemptAtEpochSeconds, 101)
})

test('invalid Retry-After fails closed and never persists an unknown raw reason', () => {
  const invalid = [epoch(100), epoch(401), epoch(1_000)]
  for (const retryAfterEpochSeconds of invalid) {
    const actor = createActor(notificationDeliveryMachine, { input: deliveryInput() }).start()
    lease(actor)
    actor.send({ type: 'PROVIDER_HTTP_OBSERVED', status: 429, reason: 'NovelProviderText' as never, retryAfterEpochSeconds, jitterSample: 1, ...observationRef(actor) })
    assert.equal(actor.getSnapshot().context.pendingCheckpoint?.outcome, retryAfterEpochSeconds === 1_000 ? 'expired' : 'unknownOutcome')
    assert.notEqual(actor.getSnapshot().context.pendingCheckpoint?.reason, 'NovelProviderText')
  }
})

const authorityLease = (actor: ReturnType<typeof createActor<typeof notificationDeliveryAuthorityMachine>>, extra = {}) => {
  actor.send({
    type: 'AUTHORITY_RECOVERY_LEASE_DURABLY_ACQUIRED', holderId: 'operator-1', leaseVersion: 3,
    fencingToken: 9, expiresAtEpochSeconds: epoch(100), expectedCheckpointRevision: actor.getSnapshot().context.checkpointRevision,
    ...extra,
  })
}
const persistAuthorityStep = (actor: ReturnType<typeof createActor<typeof notificationDeliveryAuthorityMachine>>) => {
  const effect = actor.getSnapshot().context.pendingEffect!
  const ref = { effectId: effect.effectId, checkpointRevision: effect.checkpointRevision, fencingToken: effect.fencingToken, holderId: 'operator-1', leaseVersion: 3 }
  actor.send({ type: 'AUTHORITY_EFFECT_REQUESTED', ...ref })
  actor.send({ type: 'AUTHORITY_EFFECT_DURABLY_RECORDED', ...ref })
}

test('cutover and rollback require leased, fenced, explicitly recovered pause/reconciliation/commit effects', () => {
  const actor = createActor(notificationDeliveryAuthorityMachine, { input: { authority: 'legacy', checkpointRevision: 0, activeAttempts: 0, nowEpochSeconds: epoch(0) } }).start()
  actor.send({ type: 'CUTOVER_REQUESTED', holderId: 'operator-1', leaseVersion: 3, fencingToken: 9 })
  assert.equal(actor.getSnapshot().value, 'legacyAuthoritative')
  authorityLease(actor)
  actor.send({ type: 'CUTOVER_REQUESTED', holderId: 'operator-1', leaseVersion: 3, fencingToken: 9 })
  assert.equal(actor.getSnapshot().context.pendingEffect?.kind, 'pauseLegacy')
  for (const expected of ['pauseLegacy', 'reconcileLegacy', 'commitOutboxV2'] as const) {
    assert.equal(actor.getSnapshot().context.pendingEffect?.kind, expected)
    const effect = actor.getSnapshot().context.pendingEffect!
    actor.send({ type: 'AUTHORITY_EFFECT_DURABLY_RECORDED', effectId: effect.effectId, checkpointRevision: effect.checkpointRevision, fencingToken: 9, holderId: 'operator-1', leaseVersion: 3 })
    assert.equal(actor.getSnapshot().context.pendingEffect?.kind, expected, 'ACK before emission must be ignored')
    persistAuthorityStep(actor)
  }
  assert.equal(actor.getSnapshot().value, 'outboxV2Authoritative')

  authorityLease(actor, { leaseVersion: 4, fencingToken: 10 })
  actor.send({ type: 'ROLLBACK_REQUESTED', holderId: 'operator-1', leaseVersion: 4, fencingToken: 10 })
  for (const expected of ['pauseOutboxV2', 'reconcileOutboxV2', 'commitLegacy'] as const) {
    assert.equal(actor.getSnapshot().context.pendingEffect?.kind, expected)
    const effect = actor.getSnapshot().context.pendingEffect!
    const ref = { effectId: effect.effectId, checkpointRevision: effect.checkpointRevision, fencingToken: 10, holderId: 'operator-1', leaseVersion: 4 }
    actor.send({ type: 'AUTHORITY_EFFECT_REQUESTED', ...ref })
    actor.send({ type: 'AUTHORITY_EFFECT_DURABLY_RECORDED', ...ref })
  }
  assert.equal(actor.getSnapshot().value, 'legacyAuthoritative')
})

test('authority recovery rejects foreign, expired, stale and duplicate effects after restore', () => {
  const actor = createActor(notificationDeliveryAuthorityMachine, { input: { authority: 'legacy', checkpointRevision: 4, activeAttempts: 0, nowEpochSeconds: epoch(0) } }).start()
  authorityLease(actor)
  actor.send({ type: 'CUTOVER_REQUESTED', holderId: 'operator-1', leaseVersion: 3, fencingToken: 9 })
  const restored = createActor(notificationDeliveryAuthorityMachine, { snapshot: actor.getPersistedSnapshot() }).start()
  const effect = restored.getSnapshot().context.pendingEffect!
  restored.send({ type: 'AUTHORITY_EFFECT_REQUESTED', effectId: effect.effectId, checkpointRevision: effect.checkpointRevision, fencingToken: 9, holderId: 'foreign', leaseVersion: 3 })
  assert.equal(restored.getSnapshot().context.pendingEffect?.effectEmitted, false)
  restored.send({ type: 'CLOCK_DURABLY_ADVANCED', expectedClockRevision: 0, newEpochSeconds: epoch(100) })
  restored.send({ type: 'AUTHORITY_EFFECT_REQUESTED', effectId: effect.effectId, checkpointRevision: effect.checkpointRevision, fencingToken: 9, holderId: 'operator-1', leaseVersion: 3 })
  assert.equal(restored.getSnapshot().context.pendingEffect?.effectEmitted, false)
})

const legacyPollConfirmation = (extra = {}) => ({
  format: 'poll-date-confirmed' as const,
  legacyVersion: 1,
  eventId: 'event-1',
  slotId: 'slot-1',
  legacyDomainEventId: 'poll-date-confirmed:event-1:slot-1:v1',
  legacyEffectKey: 'poll-date-confirmed:event-1:slot-1:v1:confirmation',
  ...extra,
})

test('real local poll confirmation key maps only from its exact authoritative tuple', () => {
  const result = migrateLegacyEffectIdentity(legacyPollConfirmation(), null)
  assert.deepEqual(result, {
    status: 'mapped',
    record: {
      format: 'poll-date-confirmed-v1',
      sourceTupleId: 'lpdc1.7:event-16:slot-1',
      legacyDomainEventId: 'poll-date-confirmed:event-1:slot-1:v1',
      legacyEffectKey: 'poll-date-confirmed:event-1:slot-1:v1:confirmation',
      canonicalDomainEventId: 'pdc2.7:event-16:slot-1',
      canonicalEffectKey: 'ek2.22:pdc2.7:event-16:slot-112:confirmation.v1',
    },
  })
  assert.deepEqual(effectIdentityMigration, {
    readVersions: ['poll-date-confirmed-v1', 'canonical-v2'],
    writeVersion: 'canonical-v2',
    authoritativeLegacyFormats: ['poll-date-confirmed-v1'],
    ambiguousLegacyKey: 'quarantine',
    collisionPolicy: 'quarantine',
  })
})

test('legacy poll confirmation mismatch and unknown version are quarantined without parsing', () => {
  assert.deepEqual(
    migrateLegacyEffectIdentity(legacyPollConfirmation({ legacyEffectKey: 'poll-date-confirmed:event-1:slot-2:v1:confirmation' }), null),
    { status: 'quarantined', reason: 'legacyKeyMismatch' },
  )
  assert.deepEqual(
    migrateLegacyEffectIdentity(legacyPollConfirmation({ legacyVersion: 2 }), null),
    { status: 'quarantined', reason: 'unsupportedLegacyVersion' },
  )
})

test('colon-bearing authoritative tuples stay injective and a colliding legacy key is quarantined', () => {
  const legacyEffectKey = 'poll-date-confirmed:event:other:slot:v1:confirmation'
  const first = migrateLegacyEffectIdentity(legacyPollConfirmation({
    eventId: 'event:other', slotId: 'slot',
    legacyDomainEventId: 'poll-date-confirmed:event:other:slot:v1', legacyEffectKey,
  }), null)
  assert.equal(first.status, 'mapped')
  if (first.status !== 'mapped') return
  assert.equal(first.record.canonicalEffectKey, 'ek2.25:pdc2.11:event:other4:slot12:confirmation.v1')

  const collision = migrateLegacyEffectIdentity(legacyPollConfirmation({
    eventId: 'event', slotId: 'other:slot',
    legacyDomainEventId: 'poll-date-confirmed:event:other:slot:v1', legacyEffectKey,
  }), first.record)
  assert.deepEqual(collision, { status: 'quarantined', reason: 'legacyKeyCollision' })

  const replay = migrateLegacyEffectIdentity(legacyPollConfirmation({
    eventId: 'event:other', slotId: 'slot',
    legacyDomainEventId: 'poll-date-confirmed:event:other:slot:v1', legacyEffectKey,
  }), first.record)
  assert.deepEqual(replay, { status: 'replayed', record: first.record })
})

test('calendar artifact has its own durable lifecycle and cannot complete on observation alone', () => {
  const effectKey = effectIdentity('tenant:event:42', 'DATE_CONFIRMED', 2)
  const artifactKey = calendarArtifactIdentity(effectKey, 'participant-1', 'eventkit')
  const actor = createActor(notificationCalendarArtifactMachine, { input: { calendarArtifactKey: artifactKey, checkpointRevision: 0, nowEpochSeconds: epoch(0), expiresAtEpochSeconds: epoch(100), maxAttempts: 3, nextAttemptAtEpochSeconds: epoch(0) } }).start()
  actor.send({ type: 'CALENDAR_LEASE_DURABLY_ACQUIRED', holderId: 'calendar-worker', leaseVersion: 1, fencingToken: 1, expiresAtEpochSeconds: epoch(50), expectedCheckpointRevision: 0 })
  const attempt = { calendarArtifactKey: artifactKey, correlationId: actor.getSnapshot().context.correlationId!, attempt: 0, holderId: 'calendar-worker', leaseVersion: 1, fencingToken: 1 }
  actor.send({ type: 'CALENDAR_APPLIED_OBSERVED', ...attempt })
  assert.equal(actor.getSnapshot().value, 'awaitingCalendarResultPersistence')
  const checkpoint = actor.getSnapshot().context.pendingCheckpoint!
  const ref = { effectId: checkpoint.effectId, checkpointRevision: checkpoint.revision, holderId: checkpoint.holderId, leaseVersion: checkpoint.leaseVersion, fencingToken: checkpoint.fencingToken }
  actor.send({ type: 'CALENDAR_RESULT_DURABLY_RECORDED', ...ref })
  assert.equal(actor.getSnapshot().value, 'awaitingCalendarResultPersistence')
  actor.send({ type: 'CALENDAR_RESULT_PERSISTENCE_REQUESTED', ...ref })
  actor.send({ type: 'CALENDAR_RESULT_DURABLY_RECORDED', ...ref })
  assert.equal(actor.getSnapshot().value, 'applied')
})
