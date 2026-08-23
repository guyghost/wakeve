import assert from 'node:assert/strict'
import test from 'node:test'
import { createActor } from 'xstate'
import { buildBackendIngestionTransaction, effectIdentity, epochSeconds, migrateLegacyEffectIdentity, notificationCalendarArtifactMachine, notificationDeliveryAuthorityMachine, notificationDeliveryMachine, notificationIngestionMachine, notificationRecipientTargetMachine } from './notification-delivery.machine.ts'

const epoch = epochSeconds
const beginDeliveryAttempt = (actor: ReturnType<typeof createActor<typeof notificationDeliveryMachine>>, leaseExpiresAtEpochSeconds = epoch(50)) => {
  actor.send({ type: 'POLICY_ALLOWED' })
  actor.send({ type: 'DELIVERY_LEASE_DURABLY_ACQUIRED', deliveryKey: 'delivery-1', holderId: 'w-1', leaseVersion: 1, fencingToken: 7, leaseExpiresAtEpochSeconds, expectedCheckpointRevision: actor.getSnapshot().context.checkpointRevision })
  actor.send({ type: 'PROVIDER_AUTH_READY', deliveryKey: 'delivery-1', correlationId: actor.getSnapshot().context.correlationId!, attempt: actor.getSnapshot().context.attempt, leaseHolderId: 'w-1', leaseVersion: 1, fencingToken: 7 })
}
const providerReference = (actor: ReturnType<typeof createActor<typeof notificationDeliveryMachine>>, extra = {}) => {
  const checkpoint = actor.getSnapshot().context.pendingCheckpoint!
  return { effectId: checkpoint.effectId, checkpointRevision: checkpoint.revision, authority: 'outbox-v2' as const, authorityFencingToken: 7, leaseHolderId: checkpoint.leaseHolderId, leaseVersion: checkpoint.leaseVersion, leaseFencingToken: checkpoint.leaseFencingToken, ...extra }
}
const acquireTargetLease = (actor: ReturnType<typeof createActor<typeof notificationRecipientTargetMachine>>, extra = {}) => actor.send({ type: 'TARGET_LEASE_DURABLY_ACQUIRED', holderId: 'resolver', leaseVersion: 1, fencingToken: 1, expiresAtEpochSeconds: epoch(50), expectedCheckpointRevision: actor.getSnapshot().context.checkpointRevision, ...extra } as never)

test('backend ingestion ignores a stale or incomplete transaction receipt', () => {
  const source = { domainEventId: 'tenant:event:1', effectType: 'DATE_CONFIRMED', schemaVersion: 2, participantId: 'p-1', channel: 'push', provider: 'apns', registrationIds: ['r-1'] }
  const plan = buildBackendIngestionTransaction(source)
  const actor = createActor(notificationIngestionMachine, { input: source }).start()
  actor.send({ type: 'BACKEND_INGESTION_TRANSACTION_COMMITTED', transactionId: 'tx-1', effectKey: plan.effectKey, transactionDigest: 'stale' })
  assert.equal(actor.getSnapshot().value, 'awaitingBackendIngestionCommit')
  actor.send({ type: 'BACKEND_INGESTION_TRANSACTION_COMMITTED', transactionId: 'tx-1', effectKey: plan.effectKey, transactionDigest: plan.transactionDigest })
  assert.equal(actor.getSnapshot().value, 'backendIngestionCommitted')
})

test('legacy effect migration requires its authoritative tuple', () => {
  const source = {
    format: 'poll-date-confirmed' as const,
    legacyVersion: 1,
    eventId: 'event-1',
    slotId: 'slot-1',
    legacyDomainEventId: 'poll-date-confirmed:event-1:slot-1:v1',
    legacyEffectKey: 'poll-date-confirmed:event-1:slot-1:v1:confirmation',
  }
  assert.equal(migrateLegacyEffectIdentity(source, null).status, 'mapped')
  assert.deepEqual(
    migrateLegacyEffectIdentity({ ...source, legacyDomainEventId: 'poll-date-confirmed:event-1:other:v1' }, null),
    { status: 'quarantined', reason: 'legacyKeyMismatch' },
  )
  assert.notEqual(effectIdentity('tenant:event', 'DATE_CONFIRMED', 2), effectIdentity('tenant', 'event:DATE_CONFIRMED', 2))
})

test('recipient checkpoint stale ACK stays inert across restore', () => {
  const actor = createActor(notificationRecipientTargetMachine, { input: { recipientKey: 'recipient-1', provider: 'apns', expiresAtEpochSeconds: epoch(100), nowEpochSeconds: epoch(10), maxAttempts: 3, nextAttemptAtEpochSeconds: epoch(10) } }).start()
  actor.send({ type: 'TARGET_RESOLUTION_DUE' })
  acquireTargetLease(actor)
  actor.send({ type: 'REGISTRATIONS_RESOLVED', registrationIds: ['r-1'], holderId: 'resolver', leaseVersion: 1, fencingToken: 1 })
  const checkpoint = actor.getSnapshot().context.pendingCheckpoint!
  const restored = createActor(notificationRecipientTargetMachine, { snapshot: actor.getPersistedSnapshot() }).start()
  const reference = { effectId: checkpoint.effectId, checkpointRevision: checkpoint.revision, transactionReceiptId: checkpoint.transactionReceiptId, holderId: checkpoint.holderId, leaseVersion: checkpoint.leaseVersion, fencingToken: checkpoint.fencingToken }
  restored.send({ type: 'TARGET_CHECKPOINT_DURABLE_EFFECT_REQUESTED', ...reference, effectId: 'stale' })
  restored.send({ type: 'TARGET_CHECKPOINT_DURABLY_RECORDED', ...reference })
  assert.equal(restored.getSnapshot().value, 'awaitingTargetFanoutPersistence')
  assert.deepEqual(restored.getSnapshot().context.deliveries, [])
})

test('delivery stale checkpoint ACK cannot commit a later provider result', () => {
  const actor = createActor(notificationDeliveryMachine, { input: { deliveryKey: 'delivery-1', registrationId: 'r-1', authority: 'outbox-v2', authorityFencingToken: 7, expiresAtEpochSeconds: epoch(100), nowEpochSeconds: epoch(0), maxAttempts: 3 } }).start()
  beginDeliveryAttempt(actor)
  const correlationId = actor.getSnapshot().context.correlationId!
  actor.send({ type: 'PROVIDER_HTTP_OBSERVED', deliveryKey: 'delivery-1', correlationId, attempt: 0, leaseHolderId: 'w-1', leaseVersion: 1, fencingToken: 7, status: 200 })
  const checkpoint = actor.getSnapshot().context.pendingCheckpoint!
  actor.send({ type: 'PROVIDER_RESULT_PERSISTENCE_REQUESTED', ...providerReference(actor) })
  actor.send({ type: 'PROVIDER_RESULT_DURABLY_RECORDED', ...providerReference(actor, { checkpointRevision: checkpoint.revision - 1 }) })
  assert.equal(actor.getSnapshot().value, 'awaitingProviderResultPersistence')
})

test('expired delivery lease cannot emit or ACK and recovery restages the checkpoint under a newer fence', () => {
  const actor = createActor(notificationDeliveryMachine, { input: { deliveryKey: 'delivery-1', registrationId: 'r-1', authority: 'outbox-v2', authorityFencingToken: 7, expiresAtEpochSeconds: epoch(100), nowEpochSeconds: epoch(0), maxAttempts: 3 } }).start()
  beginDeliveryAttempt(actor, epoch(10))
  const correlationId = actor.getSnapshot().context.correlationId!
  actor.send({ type: 'PROVIDER_HTTP_OBSERVED', deliveryKey: 'delivery-1', correlationId, attempt: 0, leaseHolderId: 'w-1', leaseVersion: 1, fencingToken: 7, status: 200 })
  const oldCheckpoint = actor.getSnapshot().context.pendingCheckpoint!
  const oldReference = { effectId: oldCheckpoint.effectId, checkpointRevision: oldCheckpoint.revision, authority: 'outbox-v2' as const, authorityFencingToken: 7, leaseHolderId: 'w-1', leaseVersion: 1, leaseFencingToken: 7 }

  actor.send({ type: 'DELIVERY_CLOCK_DURABLY_ADVANCED', expectedClockRevision: 0, newEpochSeconds: epoch(10) })
  actor.send({ type: 'PROVIDER_RESULT_PERSISTENCE_REQUESTED', ...oldReference })
  actor.send({ type: 'PROVIDER_RESULT_DURABLY_RECORDED', ...oldReference })
  assert.equal(actor.getSnapshot().context.persistenceEffectEmitted, false)
  assert.equal(actor.getSnapshot().value, 'awaitingProviderResultPersistence')

  actor.send({ type: 'DELIVERY_LEASE_DURABLY_ACQUIRED', deliveryKey: 'delivery-1', holderId: 'w-2', leaseVersion: 2, fencingToken: 8, leaseExpiresAtEpochSeconds: epoch(20), expectedCheckpointRevision: oldCheckpoint.revision })
  const recovered = actor.getSnapshot().context.pendingCheckpoint!
  assert.equal(recovered.outcome, 'accepted')
  assert.notEqual(recovered.effectId, oldCheckpoint.effectId)
  assert.equal(recovered.revision, oldCheckpoint.revision + 1)

  actor.send({ type: 'PROVIDER_RESULT_PERSISTENCE_REQUESTED', ...oldReference })
  assert.equal(actor.getSnapshot().context.persistenceEffectEmitted, false)
  const recoveredReference = { effectId: recovered.effectId, checkpointRevision: recovered.revision, authority: 'outbox-v2' as const, authorityFencingToken: 7, leaseHolderId: 'w-2', leaseVersion: 2, leaseFencingToken: 8 }
  actor.send({ type: 'PROVIDER_RESULT_PERSISTENCE_REQUESTED', ...recoveredReference })
  actor.send({ type: 'PROVIDER_RESULT_DURABLY_RECORDED', ...recoveredReference })
  assert.equal(actor.getSnapshot().value, 'accepted')
})

test('retry budget and target expiry are durable closed outcomes', () => {
  const delivery = createActor(notificationDeliveryMachine, { input: { deliveryKey: 'delivery-1', registrationId: 'r-1', authority: 'outbox-v2', authorityFencingToken: 7, expiresAtEpochSeconds: epoch(100), nowEpochSeconds: epoch(0), maxAttempts: 1 } }).start()
  beginDeliveryAttempt(delivery)
  delivery.send({ type: 'PROVIDER_HTTP_OBSERVED', deliveryKey: 'delivery-1', correlationId: delivery.getSnapshot().context.correlationId!, attempt: 0, leaseHolderId: 'w-1', leaseVersion: 1, fencingToken: 7, status: 503, jitterSample: 1 })
  assert.equal(delivery.getSnapshot().context.pendingCheckpoint?.outcome, 'retryExhausted')
  assert.equal(delivery.getSnapshot().context.pendingCheckpoint?.reason, 'retryBudgetExhausted')

  const recipient = createActor(notificationRecipientTargetMachine, { input: { recipientKey: 'recipient-1', provider: 'apns', expiresAtEpochSeconds: epoch(11), nowEpochSeconds: epoch(10), maxAttempts: 3, nextAttemptAtEpochSeconds: epoch(10) } }).start()
  recipient.send({ type: 'TARGET_RESOLUTION_DUE' })
  acquireTargetLease(recipient)
  recipient.send({ type: 'NO_REGISTRATIONS_RESOLVED', jitterSample: 0, holderId: 'resolver', leaseVersion: 1, fencingToken: 1 })
  assert.equal(recipient.getSnapshot().value, 'awaitingTargetExpiryPersistence')
  assert.equal(recipient.getSnapshot().context.pendingCheckpoint?.kind, 'expiry')
})

test('authority cutover remains blocked while an attempt is active', () => {
  const actor = createActor(notificationDeliveryAuthorityMachine, { input: { authority: 'legacy', checkpointRevision: 0, activeAttempts: 1, nowEpochSeconds: epoch(0) } }).start()
  actor.send({ type: 'AUTHORITY_RECOVERY_LEASE_DURABLY_ACQUIRED', holderId: 'op', leaseVersion: 1, fencingToken: 1, expiresAtEpochSeconds: epoch(50), expectedCheckpointRevision: 0 })
  actor.send({ type: 'CUTOVER_REQUESTED', holderId: 'op', leaseVersion: 1, fencingToken: 1 })
  assert.equal(actor.getSnapshot().value, 'legacyAuthoritative')
  assert.equal(actor.getSnapshot().context.pendingEffect, null)
})

test('authority restore preserves the exact pending effect without entry replay', () => {
  const actor = createActor(notificationDeliveryAuthorityMachine, { input: { authority: 'legacy', checkpointRevision: 0, activeAttempts: 0, nowEpochSeconds: epoch(0) } }).start()
  actor.send({ type: 'AUTHORITY_RECOVERY_LEASE_DURABLY_ACQUIRED', holderId: 'op', leaseVersion: 1, fencingToken: 1, expiresAtEpochSeconds: epoch(50), expectedCheckpointRevision: 0 })
  actor.send({ type: 'CUTOVER_REQUESTED', holderId: 'op', leaseVersion: 1, fencingToken: 1 })
  const restored = createActor(notificationDeliveryAuthorityMachine, { snapshot: actor.getPersistedSnapshot() }).start()
  assert.equal(restored.getSnapshot().context.pendingEffect?.kind, 'pauseLegacy')
  assert.equal(restored.getSnapshot().context.pendingEffect?.effectEmitted, false)
})

test('authority recovery reacquires after expiry and rejects stale and duplicate acknowledgements', () => {
  const actor = createActor(notificationDeliveryAuthorityMachine, { input: { authority: 'legacy', checkpointRevision: 0, activeAttempts: 0, nowEpochSeconds: epoch(0) } }).start()
  actor.send({ type: 'AUTHORITY_RECOVERY_LEASE_DURABLY_ACQUIRED', holderId: 'op-1', leaseVersion: 1, fencingToken: 1, expiresAtEpochSeconds: epoch(10), expectedCheckpointRevision: 0 })
  actor.send({ type: 'CUTOVER_REQUESTED', holderId: 'op-1', leaseVersion: 1, fencingToken: 1 })
  const firstEffect = actor.getSnapshot().context.pendingEffect!
  const firstRef = { effectId: firstEffect.effectId, checkpointRevision: firstEffect.checkpointRevision, holderId: 'op-1', leaseVersion: 1, fencingToken: 1 }
  actor.send({ type: 'AUTHORITY_EFFECT_REQUESTED', ...firstRef })
  actor.send({ type: 'AUTHORITY_EFFECT_DURABLY_RECORDED', ...firstRef })
  const secondEffect = actor.getSnapshot().context.pendingEffect!
  assert.equal(secondEffect.kind, 'reconcileLegacy')

  actor.send({ type: 'AUTHORITY_EFFECT_DURABLY_RECORDED', ...firstRef })
  assert.equal(actor.getSnapshot().context.pendingEffect?.effectId, secondEffect.effectId)
  actor.send({ type: 'CLOCK_DURABLY_ADVANCED', expectedClockRevision: 0, newEpochSeconds: epoch(10) })
  const expiredRef = { effectId: secondEffect.effectId, checkpointRevision: secondEffect.checkpointRevision, holderId: 'op-1', leaseVersion: 1, fencingToken: 1 }
  actor.send({ type: 'AUTHORITY_EFFECT_REQUESTED', ...expiredRef })
  assert.equal(actor.getSnapshot().context.pendingEffect?.effectEmitted, false)

  actor.send({ type: 'AUTHORITY_RECOVERY_LEASE_DURABLY_ACQUIRED', holderId: 'op-2', leaseVersion: 2, fencingToken: 2, expiresAtEpochSeconds: epoch(20), expectedCheckpointRevision: secondEffect.checkpointRevision })
  const recoveredEffect = actor.getSnapshot().context.pendingEffect!
  assert.equal(recoveredEffect.kind, 'reconcileLegacy')
  assert.notEqual(recoveredEffect.effectId, secondEffect.effectId)
  const recoveredRef = { effectId: recoveredEffect.effectId, checkpointRevision: recoveredEffect.checkpointRevision, holderId: 'op-2', leaseVersion: 2, fencingToken: 2 }
  actor.send({ type: 'AUTHORITY_EFFECT_REQUESTED', ...recoveredRef })
  actor.send({ type: 'AUTHORITY_EFFECT_REQUESTED', ...recoveredRef })
  actor.send({ type: 'AUTHORITY_EFFECT_DURABLY_RECORDED', ...recoveredRef })
  assert.equal(actor.getSnapshot().context.pendingEffect?.kind, 'commitOutboxV2')
  actor.send({ type: 'AUTHORITY_EFFECT_DURABLY_RECORDED', ...recoveredRef })
  assert.equal(actor.getSnapshot().context.pendingEffect?.kind, 'commitOutboxV2')
})

test('calendar checkpoint restore does not imply provider completion', () => {
  const actor = createActor(notificationCalendarArtifactMachine, { input: { calendarArtifactKey: 'calendar-1', checkpointRevision: 0, nowEpochSeconds: epoch(0), expiresAtEpochSeconds: epoch(100), maxAttempts: 3, nextAttemptAtEpochSeconds: epoch(0) } }).start()
  actor.send({ type: 'CALENDAR_LEASE_DURABLY_ACQUIRED', holderId: 'calendar-worker', leaseVersion: 1, fencingToken: 1, expiresAtEpochSeconds: epoch(50), expectedCheckpointRevision: 0 })
  actor.send({ type: 'CALENDAR_APPLIED_OBSERVED', calendarArtifactKey: 'calendar-1', correlationId: actor.getSnapshot().context.correlationId!, attempt: 0, holderId: 'calendar-worker', leaseVersion: 1, fencingToken: 1 })
  const restored = createActor(notificationCalendarArtifactMachine, { snapshot: actor.getPersistedSnapshot() }).start()
  assert.equal(restored.getSnapshot().value, 'awaitingCalendarResultPersistence')
  assert.equal(restored.getSnapshot().context.pendingCheckpoint?.effectEmitted, false)
})
