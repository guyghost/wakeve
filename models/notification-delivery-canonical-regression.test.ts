import assert from 'node:assert/strict'
import test from 'node:test'
import { createActor } from 'xstate'
import {
  classifyApnsResponse,
  deliveryIdentity,
  epochSeconds,
  fullJitterBackoffSeconds,
  notificationCalendarArtifactMachine,
  notificationDeliveryAuthorityMachine,
  notificationDeliveryMachine,
  notificationRecipientTargetMachine,
} from './notification-delivery.machine.ts'

const epoch = epochSeconds
const recipientKey = 'rk2.9:effect-v13:p-14:push'
const registrationId = 'registration-1'
const canonicalDeliveryKey = deliveryIdentity(recipientKey, registrationId, 'apns')
const deliveryInput = (extra = {}) => ({
  deliveryKey: canonicalDeliveryKey,
  canonicalDeliveryKey,
  registrationId,
  authority: 'outbox-v2' as const,
  authorityFencingToken: 11,
  expiresAtEpochSeconds: epoch(100),
  nowEpochSeconds: epoch(0),
  maxAttempts: 3,
  credentialVersion: 'credential-1',
  apnsId: 'apns-id-stable',
  ...extra,
})
type DeliveryActor = ReturnType<typeof createActor<typeof notificationDeliveryMachine>>
const delivery = (extra = {}) => createActor(notificationDeliveryMachine, { input: deliveryInput(extra) }).start()
const send = (actor: DeliveryActor, event: object) => actor.send(event as never)
const policyAllowed = (actor: DeliveryActor) => send(actor, { type: 'POLICY_ALLOWED' })
const acquireDeliveryLease = (actor: DeliveryActor, extra = {}) => send(actor, {
  type: 'DELIVERY_LEASE_DURABLY_ACQUIRED', deliveryKey: canonicalDeliveryKey,
  holderId: 'worker-1', leaseVersion: 1, fencingToken: 12,
  leaseExpiresAtEpochSeconds: epoch(50), expectedCheckpointRevision: actor.getSnapshot().context.checkpointRevision,
  ...extra,
})
const exactDeliveryAttempt = (actor: DeliveryActor, extra = {}) => ({
  deliveryKey: canonicalDeliveryKey,
  correlationId: actor.getSnapshot().context.correlationId!,
  attempt: actor.getSnapshot().context.attempt,
  leaseHolderId: actor.getSnapshot().context.lease!.holderId,
  leaseVersion: actor.getSnapshot().context.lease!.version,
  fencingToken: actor.getSnapshot().context.lease!.fencingToken,
  ...extra,
})
const authReady = (actor: DeliveryActor, extra = {}) => send(actor, { type: 'PROVIDER_AUTH_READY', ...exactDeliveryAttempt(actor, extra) })
const enterSending = (actor: DeliveryActor) => { policyAllowed(actor); acquireDeliveryLease(actor); authReady(actor) }
const deliveryCheckpointReference = (actor: DeliveryActor, extra = {}) => {
  const checkpoint = actor.getSnapshot().context.pendingCheckpoint!
  return {
    effectId: checkpoint.effectId,
    checkpointRevision: checkpoint.revision,
    authority: actor.getSnapshot().context.authority,
    authorityFencingToken: actor.getSnapshot().context.authorityFencingToken,
    leaseHolderId: checkpoint.leaseHolderId,
    leaseVersion: checkpoint.leaseVersion,
    leaseFencingToken: checkpoint.leaseFencingToken,
    ...extra,
  }
}
const persistDeliveryCheckpoint = (actor: DeliveryActor, extra = {}) => {
  const reference = deliveryCheckpointReference(actor, extra)
  send(actor, { type: 'PROVIDER_RESULT_PERSISTENCE_REQUESTED', ...reference })
  send(actor, { type: 'PROVIDER_RESULT_DURABLY_RECORDED', ...reference })
}

// Historical normative scenario 01 -> canonical policy checkpoint path.
test('canonical delivery suppresses policy without acquiring a provider lease', () => {
  const actor = delivery()
  assert.equal(actor.getSnapshot().value, 'policyCheck')
  send(actor, { type: 'POLICY_SUPPRESSED' })
  assert.equal(actor.getSnapshot().value, 'suppressed')
  assert.equal(actor.getSnapshot().context.lease, null)
})

// Historical normative scenario 02 -> durable quiet-hours wait and explicit policy recheck.
test('canonical delivery defers quiet hours until its authoritative clock reaches the schedule', () => {
  const actor = delivery()
  send(actor, { type: 'QUIET_HOURS_ACTIVE', nextEligibleAtEpochSeconds: epoch(20) })
  assert.equal(actor.getSnapshot().value, 'deferredQuietHours')
  send(actor, { type: 'QUIET_HOURS_ENDED' })
  assert.equal(actor.getSnapshot().value, 'deferredQuietHours')
  send(actor, { type: 'CLOCK_TICK', expectedClockRevision: 0, nowEpochSeconds: epoch(20) })
  send(actor, { type: 'QUIET_HOURS_ENDED' })
  assert.equal(actor.getSnapshot().value, 'policyCheck')
})

// Historical normative scenario 03 -> token wait resumes without manufacturing a target.
test('canonical delivery waits for a token and returns to policy only on registration', () => {
  const actor = delivery()
  send(actor, { type: 'NO_ACTIVE_TOKEN' })
  assert.equal(actor.getSnapshot().value, 'awaitingToken')
  send(actor, { type: 'TOKEN_REGISTERED', registrationId: 'registration-foreign' })
  assert.equal(actor.getSnapshot().value, 'awaitingToken')
  send(actor, { type: 'TOKEN_REGISTERED', registrationId })
  assert.equal(actor.getSnapshot().value, 'policyCheck')
})

// Historical normative scenario 04 -> exact lease gates auth and exact loss requeues.
test('canonical delivery lease gates authentication and exact lease loss requeues', () => {
  const actor = delivery()
  policyAllowed(actor)
  assert.equal(actor.getSnapshot().value, 'queued')
  acquireDeliveryLease(actor)
  assert.equal(actor.getSnapshot().value, 'auth')
  send(actor, { type: 'DELIVERY_LEASE_LOST', ...exactDeliveryAttempt(actor) })
  assert.equal(actor.getSnapshot().value, 'queued')
  assert.equal(actor.getSnapshot().context.lease, null)
})

// Historical normative scenario 05 -> HTTP 200 is staged before exact durable acceptance.
test('canonical delivery records APNs acceptance only after emission and exact checkpoint ACK', () => {
  const actor = delivery()
  enterSending(actor)
  send(actor, { type: 'PROVIDER_HTTP_OBSERVED', status: 200, acceptedAtEpochSeconds: epoch(7), ...exactDeliveryAttempt(actor) })
  assert.equal(actor.getSnapshot().value, 'awaitingProviderResultPersistence')
  assert.equal(actor.getSnapshot().context.acceptedAtEpochSeconds, null)
  persistDeliveryCheckpoint(actor)
  assert.equal(actor.getSnapshot().value, 'accepted')
  assert.equal(actor.getSnapshot().context.acceptedAtEpochSeconds, 7)
  assert.equal(actor.getSnapshot().context.sentAtEpochSeconds, 7)
})

// Historical normative scenario 06 -> may-have-written is a durable, recoverable unknown outcome.
test('may-have-written becomes durable unknownOutcome and bounded retry preserves delivery and APNs identities', () => {
  const actor = delivery()
  enterSending(actor)
  send(actor, { type: 'PROVIDER_TRANSPORT_OBSERVED', phase: 'mayHaveWritten', ...exactDeliveryAttempt(actor) })
  assert.equal(actor.getSnapshot().context.pendingCheckpoint?.outcome, 'unknownOutcome')
  persistDeliveryCheckpoint(actor)
  assert.equal(actor.getSnapshot().value, 'unknownOutcome')
  const before = { deliveryKey: actor.getSnapshot().context.deliveryKey, apnsId: actor.getSnapshot().context.apnsId }
  send(actor, { type: 'UNKNOWN_OUTCOME_RETRY_REQUESTED', jitterSample: 0, ...exactDeliveryAttempt(actor) })
  assert.equal(actor.getSnapshot().context.pendingCheckpoint?.outcome, 'retry')
  persistDeliveryCheckpoint(actor)
  assert.equal(actor.getSnapshot().value, 'retry')
  assert.deepEqual({ deliveryKey: actor.getSnapshot().context.deliveryKey, apnsId: actor.getSnapshot().context.apnsId }, before)
})

// Historical normative scenario 07 -> token invalidation is scoped and checkpointed.
test('invalid token terminalizes only the historical registration after exact persistence', () => {
  const actor = delivery()
  enterSending(actor)
  send(actor, { type: 'PROVIDER_HTTP_OBSERVED', status: 410, reason: 'Unregistered', ...exactDeliveryAttempt(actor) })
  persistDeliveryCheckpoint(actor)
  assert.equal(actor.getSnapshot().value, 'invalidToken')
  assert.equal(actor.getSnapshot().context.registrationId, registrationId)
})

// Historical normative scenario 08 -> payload rejection does not retry.
test('payload rejection uses an exact terminal checkpoint and never schedules retry', () => {
  const actor = delivery()
  enterSending(actor)
  send(actor, { type: 'PROVIDER_HTTP_OBSERVED', status: 413, reason: 'PayloadTooLarge', ...exactDeliveryAttempt(actor) })
  persistDeliveryCheckpoint(actor)
  assert.equal(actor.getSnapshot().value, 'rejectedPayload')
  assert.equal(actor.getSnapshot().context.nextAttemptAtEpochSeconds, null)
})

// Historical normative scenario 09 -> process credential circuit is independent of delivery attempts.
test('provider authentication rejection blocks one credential without consuming delivery budget', () => {
  const actor = delivery()
  policyAllowed(actor); acquireDeliveryLease(actor)
  send(actor, { type: 'PROVIDER_AUTH_CONFIGURATION_REJECTED', ...exactDeliveryAttempt(actor) })
  assert.equal(actor.getSnapshot().context.pendingCheckpoint?.outcome, 'providerAuthBlocked')
  persistDeliveryCheckpoint(actor)
  assert.equal(actor.getSnapshot().value, 'providerAuthBlocked')
  assert.equal(actor.getSnapshot().context.attempt, 0)
  assert.equal(actor.getSnapshot().context.providerCircuit.blockedCredentialVersion, 'credential-1')
  send(actor, { type: 'CREDENTIALS_ROTATED', credentialVersion: 'credential-2' })
  assert.equal(actor.getSnapshot().value, 'auth')
  assert.equal(actor.getSnapshot().context.providerCircuit.blockedCredentialVersion, null)
})

// Historical normative scenario 10 -> retry is nonterminal and budget exhaustion is checkpointed.
test('transient response enters retry until RETRY_DUE and later exhausts its bounded budget', () => {
  const actor = delivery({ maxAttempts: 1 })
  enterSending(actor)
  send(actor, { type: 'PROVIDER_HTTP_OBSERVED', status: 503, jitterSample: 0, ...exactDeliveryAttempt(actor) })
  assert.equal(actor.getSnapshot().context.pendingCheckpoint?.outcome, 'retryExhausted')
  persistDeliveryCheckpoint(actor)
  assert.equal(actor.getSnapshot().value, 'retryExhausted')

  const retrying = delivery({ maxAttempts: 2 })
  enterSending(retrying)
  send(retrying, { type: 'PROVIDER_HTTP_OBSERVED', status: 503, jitterSample: 0, ...exactDeliveryAttempt(retrying) })
  persistDeliveryCheckpoint(retrying)
  assert.equal(retrying.getSnapshot().value, 'retry')
  send(retrying, { type: 'RETRY_DUE' })
  assert.equal(retrying.getSnapshot().value, 'retry')
  send(retrying, { type: 'CLOCK_TICK', expectedClockRevision: 0, nowEpochSeconds: epoch(1) })
  send(retrying, { type: 'RETRY_DUE' })
  assert.equal(retrying.getSnapshot().value, 'queued')
})

// Historical normative scenario 11 -> cancellation is safe before write and unknown after possible write.
test('cancellation is direct before provider write and checkpointed unknown after send begins', () => {
  const queued = delivery(); policyAllowed(queued); send(queued, { type: 'CANCEL_REQUESTED' })
  assert.equal(queued.getSnapshot().value, 'cancelled')
  const sending = delivery(); enterSending(sending); send(sending, { type: 'CANCEL_REQUESTED' })
  assert.equal(sending.getSnapshot().context.pendingCheckpoint?.outcome, 'unknownOutcome')
  persistDeliveryCheckpoint(sending)
  assert.equal(sending.getSnapshot().value, 'unknownOutcome')
})

// Historical normative scenario 12 -> one context exposes the reviewed durable vocabularies.
test('canonical delivery exposes one durable authority and independent sync and dispatch statuses', () => {
  const actor = delivery()
  assert.equal(actor.getSnapshot().context.authority, 'outbox-v2')
  assert.equal(actor.getSnapshot().context.decisionSyncStatus, 'acknowledged')
  assert.equal(actor.getSnapshot().context.effectDispatchStatus, 'notDispatched')
  assert.equal(actor.getSnapshot().context.deliveryStatus, 'policyCheck')
})

// Historical normative scenario 13 -> every auth/provider callback is correlated to the current flight.
test('stale auth and provider correlations cannot advance the canonical machine', () => {
  const actor = delivery(); policyAllowed(actor); acquireDeliveryLease(actor)
  authReady(actor, { correlationId: 'stale' })
  assert.equal(actor.getSnapshot().value, 'auth')
  const authCorrelation = actor.getSnapshot().context.correlationId
  authReady(actor)
  assert.equal(actor.getSnapshot().value, 'sending')
  send(actor, { type: 'PROVIDER_HTTP_OBSERVED', status: 200, ...exactDeliveryAttempt(actor, { correlationId: authCorrelation }) })
  assert.equal(actor.getSnapshot().value, 'sending')
})

// Historical normative scenario 14 -> one correlated refresh, then a credential circuit block.
test('ExpiredProviderToken refreshes the same send exactly once then blocks its credential circuit', () => {
  const actor = delivery(); enterSending(actor)
  send(actor, { type: 'PROVIDER_HTTP_OBSERVED', status: 403, reason: 'ExpiredProviderToken', ...exactDeliveryAttempt(actor) })
  assert.equal(actor.getSnapshot().context.pendingCheckpoint?.outcome, 'refreshAuth')
  persistDeliveryCheckpoint(actor)
  assert.equal(actor.getSnapshot().value, 'auth')
  assert.equal(actor.getSnapshot().context.providerAuthSendCoordinator.refreshUsed, true)
  authReady(actor)
  send(actor, { type: 'PROVIDER_HTTP_OBSERVED', status: 403, reason: 'ExpiredProviderToken', ...exactDeliveryAttempt(actor) })
  assert.equal(actor.getSnapshot().context.pendingCheckpoint?.outcome, 'providerAuthBlocked')
  persistDeliveryCheckpoint(actor)
  assert.equal(actor.getSnapshot().value, 'providerAuthBlocked')
  assert.equal(actor.getSnapshot().context.providerCircuit.blockedCredentialVersion, 'credential-1')
})

// Historical normative scenario 15 -> closed classifier remains the only APNs reason authority.
test('APNs classifier covers accepted invalid retry auth and safe unknown branches', () => {
  assert.equal(classifyApnsResponse(200), 'accepted')
  assert.equal(classifyApnsResponse(400, 'BadDeviceToken'), 'invalidToken')
  assert.equal(classifyApnsResponse(429, 'TooManyRequests'), 'retry')
  assert.equal(classifyApnsResponse(403, 'ExpiredProviderToken'), 'refreshAuth')
  assert.equal(classifyApnsResponse(403, 'InvalidProviderToken'), 'providerAuthBlocked')
  assert.equal(classifyApnsResponse(418, 'NovelReason'), 'unknownTerminal')
})

// Historical normative scenario 16 -> retry jitter is seconds, future and capped.
test('canonical full jitter has a one-second floor and a 300-second overflow-safe cap', () => {
  assert.equal(fullJitterBackoffSeconds(2, 0), 1)
  assert.equal(fullJitterBackoffSeconds(20, 1), 300)
  assert.equal(fullJitterBackoffSeconds(Number.MAX_SAFE_INTEGER, 1), 300)
})

// Historical normative scenario 17 -> reviewed vocabulary is not inferred from free text.
test('canonical delivery retains reviewed status and authority vocabulary through queueing', () => {
  const actor = delivery(); policyAllowed(actor)
  assert.equal(actor.getSnapshot().context.authority, 'outbox-v2')
  assert.equal(actor.getSnapshot().context.deliveryStatus, 'queued')
  assert.equal(actor.getSnapshot().context.effectDispatchStatus, 'queued')
})

// Historical normative scenario 18 -> lease must be owned, fenced, future and before business expiry.
test('ownerless expired stale and post-business-expiry delivery leases are rejected', () => {
  const actor = delivery({ nowEpochSeconds: epoch(9), expiresAtEpochSeconds: epoch(10) }); policyAllowed(actor)
  acquireDeliveryLease(actor, { holderId: '' })
  acquireDeliveryLease(actor, { leaseExpiresAtEpochSeconds: epoch(9) })
  assert.equal(actor.getSnapshot().value, 'queued')
  send(actor, { type: 'CLOCK_TICK', expectedClockRevision: 0, nowEpochSeconds: epoch(10) })
  acquireDeliveryLease(actor, { holderId: 'worker-1', leaseExpiresAtEpochSeconds: epoch(20) })
  assert.notEqual(actor.getSnapshot().value, 'auth')
})

// Historical normative scenario 19 -> valid Retry-After overrides fallback jitter.
test('valid Retry-After is persisted as the authoritative retry schedule', () => {
  const actor = delivery({ nowEpochSeconds: epoch(10), expiresAtEpochSeconds: epoch(100) }); enterSending(actor)
  send(actor, { type: 'PROVIDER_HTTP_OBSERVED', status: 429, reason: 'TooManyRequests', retryAfterEpochSeconds: epoch(30), jitterSample: 0, ...exactDeliveryAttempt(actor) })
  assert.equal(actor.getSnapshot().context.pendingCheckpoint?.nextAttemptAtEpochSeconds, 30)
  persistDeliveryCheckpoint(actor)
  assert.equal(actor.getSnapshot().value, 'retry')
})

// Historical normative scenario 20 -> unknown HTTP response is durable unknownOutcome, not an unsafe success.
test('unknown APNs classification reaches recoverable unknownOutcome after exact ACK', () => {
  const actor = delivery(); enterSending(actor)
  send(actor, { type: 'PROVIDER_HTTP_OBSERVED', status: 418, reason: 'NovelReason', ...exactDeliveryAttempt(actor) })
  assert.equal(actor.getSnapshot().context.pendingCheckpoint?.outcome, 'unknownOutcome')
  persistDeliveryCheckpoint(actor)
  assert.equal(actor.getSnapshot().value, 'unknownOutcome')
  assert.equal(actor.getSnapshot().context.acceptedAtEpochSeconds, null)
})

// Historical normative scenario 21 -> identity mismatch fails before policy or provider work.
test('mismatched canonical delivery identity fails closed before policy evaluation', () => {
  const actor = delivery({ canonicalDeliveryKey: 'dk2.5:wrong' })
  assert.equal(actor.getSnapshot().value, 'identityMismatch')
  assert.equal(actor.getSnapshot().context.effectDispatchStatus, 'terminalFailure')
})

// Historical normative scenario 22 -> authoritative clock, never an event-local now, controls lease validity.
test('authoritative clock controls delivery lease validity', () => {
  const actor = delivery({ nowEpochSeconds: epoch(10) }); policyAllowed(actor)
  send(actor, { type: 'CLOCK_TICK', expectedClockRevision: 0, nowEpochSeconds: epoch(20) })
  acquireDeliveryLease(actor, { leaseExpiresAtEpochSeconds: epoch(15) })
  assert.equal(actor.getSnapshot().value, 'queued')
  acquireDeliveryLease(actor, { leaseExpiresAtEpochSeconds: epoch(30) })
  assert.equal(actor.getSnapshot().value, 'auth')
})

const authorityLease = (actor: ReturnType<typeof createActor<typeof notificationDeliveryAuthorityMachine>>, extra = {}) => actor.send({ type: 'AUTHORITY_RECOVERY_LEASE_DURABLY_ACQUIRED', holderId: 'operator', leaseVersion: 1, fencingToken: 1, expiresAtEpochSeconds: epoch(50), expectedCheckpointRevision: actor.getSnapshot().context.checkpointRevision, ...extra })

// Historical normative scenario 23 -> active attempts block durable cutover.
test('cutover stays blocked while canonical delivery work is active', () => {
  const actor = createActor(notificationDeliveryAuthorityMachine, { input: { authority: 'legacy', checkpointRevision: 0, activeAttempts: 1, nowEpochSeconds: epoch(0) } }).start()
  authorityLease(actor)
  actor.send({ type: 'CUTOVER_REQUESTED', holderId: 'operator', leaseVersion: 1, fencingToken: 1 })
  assert.equal(actor.getSnapshot().value, 'legacyAuthoritative')
  assert.equal(actor.getSnapshot().context.authority, 'legacy')
})

// Historical normative scenario 24 -> rollback is a fenced effect sequence and never retro-sends.
test('rollback changes authority only after pause reconciliation and commit checkpoints', () => {
  const actor = createActor(notificationDeliveryAuthorityMachine, { input: { authority: 'outbox-v2', checkpointRevision: 0, activeAttempts: 0, nowEpochSeconds: epoch(0) } }).start()
  authorityLease(actor)
  actor.send({ type: 'ROLLBACK_REQUESTED', holderId: 'operator', leaseVersion: 1, fencingToken: 1 })
  for (const kind of ['pauseOutboxV2', 'reconcileOutboxV2', 'commitLegacy'] as const) {
    const effect = actor.getSnapshot().context.pendingEffect!
    assert.equal(effect.kind, kind)
    const reference = { effectId: effect.effectId, checkpointRevision: effect.checkpointRevision, holderId: 'operator', leaseVersion: 1, fencingToken: 1 }
    actor.send({ type: 'AUTHORITY_EFFECT_REQUESTED', ...reference })
    actor.send({ type: 'AUTHORITY_EFFECT_DURABLY_RECORDED', ...reference })
  }
  assert.equal(actor.getSnapshot().context.authority, 'legacy')
})

// Historical normative scenario 25 -> preparation callbacks cannot mutate active delivery work.
test('cutover and rollback events have no authority inside canonical delivery states', () => {
  for (const state of ['auth', 'sending', 'retry', 'unknownOutcome'] as const) {
    const actor = delivery({ authority: 'legacy' as const }); policyAllowed(actor); acquireDeliveryLease(actor)
    if (state !== 'auth') authReady(actor)
    if (state === 'retry') { send(actor, { type: 'PROVIDER_HTTP_OBSERVED', status: 503, jitterSample: 0, ...exactDeliveryAttempt(actor) }); persistDeliveryCheckpoint(actor) }
    if (state === 'unknownOutcome') { send(actor, { type: 'PROVIDER_TRANSPORT_OBSERVED', phase: 'mayHaveWritten', ...exactDeliveryAttempt(actor) }); persistDeliveryCheckpoint(actor) }
    assert.equal(actor.getSnapshot().value, state)
    send(actor, { type: 'CUTOVER_REQUESTED', holderId: 'operator', leaseVersion: 1, fencingToken: 1 })
    send(actor, { type: 'ROLLBACK_REQUESTED', holderId: 'operator', leaseVersion: 1, fencingToken: 1 })
    assert.equal(actor.getSnapshot().value, state)
    assert.equal(actor.getSnapshot().context.authority, 'legacy')
  }
})

// Historical normative scenario 26 -> rewind cannot revalidate expired work.
test('canonical delivery clock is revision-correlated and monotone', () => {
  const actor = delivery({ nowEpochSeconds: epoch(20) })
  send(actor, { type: 'CLOCK_TICK', expectedClockRevision: 0, nowEpochSeconds: epoch(10) })
  assert.equal(actor.getSnapshot().context.nowEpochSeconds, 20)
  send(actor, { type: 'CLOCK_TICK', expectedClockRevision: 0, nowEpochSeconds: epoch(25) })
  assert.equal(actor.getSnapshot().context.nowEpochSeconds, 25)
  send(actor, { type: 'CLOCK_TICK', expectedClockRevision: 0, nowEpochSeconds: epoch(24) })
  assert.equal(actor.getSnapshot().context.nowEpochSeconds, 25)
})

test('business expiry is durably checkpointed from every nonterminal delivery phase', () => {
  const preparations: Record<string, (actor: DeliveryActor) => void> = {
    policyCheck: () => {},
    deferredQuietHours: (actor) => send(actor, { type: 'QUIET_HOURS_ACTIVE', nextEligibleAtEpochSeconds: epoch(5) }),
    awaitingToken: (actor) => send(actor, { type: 'NO_ACTIVE_TOKEN' }),
    queued: (actor) => policyAllowed(actor),
    auth: (actor) => { policyAllowed(actor); acquireDeliveryLease(actor, { leaseExpiresAtEpochSeconds: epoch(20) }) },
    sending: (actor) => { policyAllowed(actor); acquireDeliveryLease(actor, { leaseExpiresAtEpochSeconds: epoch(20) }); authReady(actor) },
    retry: (actor) => { enterSending(actor); send(actor, { type: 'PROVIDER_HTTP_OBSERVED', status: 503, jitterSample: 0, ...exactDeliveryAttempt(actor) }); persistDeliveryCheckpoint(actor) },
    unknownOutcome: (actor) => { enterSending(actor); send(actor, { type: 'PROVIDER_TRANSPORT_OBSERVED', phase: 'mayHaveWritten', ...exactDeliveryAttempt(actor) }); persistDeliveryCheckpoint(actor) },
    providerAuthBlocked: (actor) => { enterSending(actor); send(actor, { type: 'PROVIDER_HTTP_OBSERVED', status: 403, reason: 'InvalidProviderToken', ...exactDeliveryAttempt(actor) }); persistDeliveryCheckpoint(actor) },
  }
  for (const [expectedState, prepare] of Object.entries(preparations)) {
    const actor = delivery({ expiresAtEpochSeconds: epoch(10) })
    prepare(actor)
    assert.equal(actor.getSnapshot().value, expectedState)
    send(actor, { type: 'CLOCK_TICK', expectedClockRevision: 0, nowEpochSeconds: epoch(10) })
    assert.equal(actor.getSnapshot().value, 'awaitingProviderResultPersistence', expectedState)
    assert.equal(actor.getSnapshot().context.pendingCheckpoint?.outcome, 'expired', expectedState)
    persistDeliveryCheckpoint(actor)
    assert.equal(actor.getSnapshot().value, 'expired', expectedState)
  }
})

type TargetActor = ReturnType<typeof createActor<typeof notificationRecipientTargetMachine>>
const targetInput = () => ({ recipientKey: 'recipient-1', provider: 'apns', expiresAtEpochSeconds: epoch(100), nowEpochSeconds: epoch(0), maxAttempts: 3, nextAttemptAtEpochSeconds: epoch(0) })
const targetLease = (actor: TargetActor, extra = {}) => actor.send({ type: 'TARGET_LEASE_DURABLY_ACQUIRED', holderId: 'resolver-1', leaseVersion: 1, fencingToken: 1, expiresAtEpochSeconds: epoch(20), expectedCheckpointRevision: actor.getSnapshot().context.checkpointRevision, ...extra } as never)
const targetReference = (actor: TargetActor, extra = {}) => {
  const checkpoint = actor.getSnapshot().context.pendingCheckpoint!
  return { effectId: checkpoint.effectId, checkpointRevision: checkpoint.revision, transactionReceiptId: checkpoint.transactionReceiptId, holderId: checkpoint.holderId, leaseVersion: checkpoint.leaseVersion, fencingToken: checkpoint.fencingToken, ...extra }
}

test('pending target fan-out is leased, receipt-fenced, restore-safe and accepts one exact worker', () => {
  const initial = createActor(notificationRecipientTargetMachine, { input: targetInput() }).start()
  initial.send({ type: 'TARGET_RESOLUTION_DUE' })
  assert.equal(initial.getSnapshot().value, 'awaitingTargetLease')
  const firstWorker = createActor(notificationRecipientTargetMachine, { snapshot: initial.getPersistedSnapshot() }).start()
  const secondWorker = createActor(notificationRecipientTargetMachine, { snapshot: initial.getPersistedSnapshot() }).start()
  targetLease(firstWorker)
  firstWorker.send({ type: 'REGISTRATIONS_RESOLVED', registrationIds: ['r-2', 'r-1'], holderId: 'resolver-1', leaseVersion: 1, fencingToken: 1 } as never)
  secondWorker.send({ type: 'REGISTRATIONS_RESOLVED', registrationIds: ['r-3'], holderId: 'resolver-1', leaseVersion: 1, fencingToken: 1 } as never)
  assert.equal(firstWorker.getSnapshot().value, 'awaitingTargetFanoutPersistence')
  assert.equal(secondWorker.getSnapshot().value, 'awaitingTargetLease')
  const reference = targetReference(firstWorker)
  const restored = createActor(notificationRecipientTargetMachine, { snapshot: firstWorker.getPersistedSnapshot() }).start()
  restored.send({ type: 'TARGET_CHECKPOINT_DURABLY_RECORDED', ...reference } as never)
  assert.equal(restored.getSnapshot().value, 'awaitingTargetFanoutPersistence')
  restored.send({ type: 'TARGET_CHECKPOINT_DURABLE_EFFECT_REQUESTED', ...reference, holderId: 'foreign' } as never)
  assert.equal(restored.getSnapshot().context.pendingCheckpoint?.effectEmitted, false)
  restored.send({ type: 'TARGET_CHECKPOINT_DURABLE_EFFECT_REQUESTED', ...reference } as never)
  restored.send({ type: 'TARGET_CHECKPOINT_DURABLY_RECORDED', ...reference } as never)
  assert.equal(restored.getSnapshot().value, 'targeted')
  assert.deepEqual(restored.getSnapshot().context.deliveries.map((item) => item.registrationId), ['r-1', 'r-2'])
  restored.send({ type: 'TARGET_CHECKPOINT_DURABLY_RECORDED', ...reference } as never)
  assert.equal(restored.getSnapshot().value, 'targeted')
})

test('pending target recovery replaces an expired lease and rejects stale receipt acknowledgements', () => {
  const actor = createActor(notificationRecipientTargetMachine, { input: targetInput() }).start()
  actor.send({ type: 'TARGET_RESOLUTION_DUE' }); targetLease(actor, { expiresAtEpochSeconds: epoch(5) })
  actor.send({ type: 'REGISTRATIONS_RESOLVED', registrationIds: ['r-1'], holderId: 'resolver-1', leaseVersion: 1, fencingToken: 1 } as never)
  const stale = targetReference(actor)
  actor.send({ type: 'CLOCK_DURABLY_ADVANCED', expectedClockRevision: 0, newEpochSeconds: epoch(5) })
  actor.send({ type: 'TARGET_CHECKPOINT_DURABLE_EFFECT_REQUESTED', ...stale } as never)
  assert.equal(actor.getSnapshot().context.pendingCheckpoint?.effectEmitted, false)
  targetLease(actor, { holderId: 'resolver-2', leaseVersion: 2, fencingToken: 2, expiresAtEpochSeconds: epoch(20) })
  const recovered = targetReference(actor)
  assert.notEqual(recovered.effectId, stale.effectId)
  actor.send({ type: 'TARGET_CHECKPOINT_DURABLY_RECORDED', ...stale } as never)
  assert.equal(actor.getSnapshot().value, 'awaitingTargetFanoutPersistence')
  actor.send({ type: 'TARGET_CHECKPOINT_DURABLE_EFFECT_REQUESTED', ...recovered } as never)
  actor.send({ type: 'TARGET_CHECKPOINT_DURABLY_RECORDED', ...recovered } as never)
  assert.equal(actor.getSnapshot().value, 'targeted')
})

type CalendarActor = ReturnType<typeof createActor<typeof notificationCalendarArtifactMachine>>
const calendar = () => createActor(notificationCalendarArtifactMachine, { input: { calendarArtifactKey: 'calendar-1', checkpointRevision: 0, nowEpochSeconds: epoch(0), expiresAtEpochSeconds: epoch(100), maxAttempts: 3, nextAttemptAtEpochSeconds: epoch(0) } }).start()
const calendarLease = (actor: CalendarActor, extra = {}) => actor.send({ type: 'CALENDAR_LEASE_DURABLY_ACQUIRED', holderId: 'calendar-worker-1', leaseVersion: 1, fencingToken: 1, expiresAtEpochSeconds: epoch(20), expectedCheckpointRevision: actor.getSnapshot().context.checkpointRevision, ...extra } as never)
const calendarAttempt = (actor: CalendarActor, extra = {}) => ({ calendarArtifactKey: actor.getSnapshot().context.calendarArtifactKey, correlationId: actor.getSnapshot().context.correlationId!, attempt: actor.getSnapshot().context.attempt, holderId: actor.getSnapshot().context.lease!.holderId, leaseVersion: actor.getSnapshot().context.lease!.version, fencingToken: actor.getSnapshot().context.lease!.fencingToken, ...extra })
const calendarReference = (actor: CalendarActor, extra = {}) => {
  const checkpoint = actor.getSnapshot().context.pendingCheckpoint!
  return { effectId: checkpoint.effectId, checkpointRevision: checkpoint.revision, holderId: checkpoint.holderId, leaseVersion: checkpoint.leaseVersion, fencingToken: checkpoint.fencingToken, ...extra }
}

test('calendar result is independently correlated, leased, checkpointed and restore-safe', () => {
  const actor = calendar(); calendarLease(actor)
  assert.equal(actor.getSnapshot().value, 'sending')
  actor.send({ type: 'CALENDAR_APPLIED_OBSERVED', ...calendarAttempt(actor, { correlationId: 'foreign' }) } as never)
  assert.equal(actor.getSnapshot().value, 'sending')
  actor.send({ type: 'CALENDAR_APPLIED_OBSERVED', ...calendarAttempt(actor) } as never)
  const restored = createActor(notificationCalendarArtifactMachine, { snapshot: actor.getPersistedSnapshot() }).start()
  const reference = calendarReference(restored)
  restored.send({ type: 'CALENDAR_RESULT_DURABLY_RECORDED', ...reference } as never)
  assert.equal(restored.getSnapshot().value, 'awaitingCalendarResultPersistence')
  restored.send({ type: 'CALENDAR_RESULT_PERSISTENCE_REQUESTED', ...reference } as never)
  restored.send({ type: 'CALENDAR_RESULT_DURABLY_RECORDED', ...reference } as never)
  assert.equal(restored.getSnapshot().value, 'applied')
})

test('calendar retryable failure schedules nonterminal retry and preserves provider independence', () => {
  const actor = calendar(); calendarLease(actor)
  assert.equal(actor.getSnapshot().value, 'sending')
  actor.send({ type: 'PROVIDER_HTTP_OBSERVED', status: 200, ...calendarAttempt(actor) } as never)
  assert.equal(actor.getSnapshot().value, 'sending')
  actor.send({ type: 'CALENDAR_RETRYABLE_FAILURE_OBSERVED', jitterSample: 0, ...calendarAttempt(actor) } as never)
  const reference = calendarReference(actor)
  actor.send({ type: 'CALENDAR_RESULT_PERSISTENCE_REQUESTED', ...reference } as never)
  actor.send({ type: 'CALENDAR_RESULT_DURABLY_RECORDED', ...reference } as never)
  assert.equal(actor.getSnapshot().value, 'retry')
  actor.send({ type: 'CALENDAR_RETRY_DUE' } as never)
  assert.equal(actor.getSnapshot().value, 'retry')
  actor.send({ type: 'CALENDAR_CLOCK_TICK', expectedClockRevision: 0, nowEpochSeconds: epoch(1) } as never)
  actor.send({ type: 'CALENDAR_RETRY_DUE' } as never)
  assert.equal(actor.getSnapshot().value, 'queued')
})

test('calendar expired lease recovery restages one effect under a newer fence', () => {
  const actor = calendar(); calendarLease(actor, { expiresAtEpochSeconds: epoch(5) })
  assert.equal(actor.getSnapshot().value, 'sending')
  actor.send({ type: 'CALENDAR_APPLIED_OBSERVED', ...calendarAttempt(actor) } as never)
  const stale = calendarReference(actor)
  actor.send({ type: 'CALENDAR_CLOCK_TICK', expectedClockRevision: 0, nowEpochSeconds: epoch(5) } as never)
  actor.send({ type: 'CALENDAR_RESULT_PERSISTENCE_REQUESTED', ...stale } as never)
  assert.equal(actor.getSnapshot().context.pendingCheckpoint?.effectEmitted, false)
  calendarLease(actor, { holderId: 'calendar-worker-2', leaseVersion: 2, fencingToken: 2, expiresAtEpochSeconds: epoch(20) })
  const recovered = calendarReference(actor)
  assert.notEqual(recovered.effectId, stale.effectId)
  actor.send({ type: 'CALENDAR_RESULT_PERSISTENCE_REQUESTED', ...recovered } as never)
  actor.send({ type: 'CALENDAR_RESULT_DURABLY_RECORDED', ...recovered } as never)
  assert.equal(actor.getSnapshot().value, 'applied')
})

test('calendar retry budget and business expiry are exact durable terminal checkpoints', () => {
  const exhausted = createActor(notificationCalendarArtifactMachine, { input: { calendarArtifactKey: 'calendar-budget', checkpointRevision: 0, nowEpochSeconds: epoch(0), expiresAtEpochSeconds: epoch(100), maxAttempts: 1, nextAttemptAtEpochSeconds: epoch(0) } }).start()
  calendarLease(exhausted)
  exhausted.send({ type: 'CALENDAR_RETRYABLE_FAILURE_OBSERVED', jitterSample: 0, ...calendarAttempt(exhausted) } as never)
  assert.equal(exhausted.getSnapshot().context.pendingCheckpoint?.outcome, 'retryExhausted')
  let reference = calendarReference(exhausted)
  exhausted.send({ type: 'CALENDAR_RESULT_PERSISTENCE_REQUESTED', ...reference } as never)
  exhausted.send({ type: 'CALENDAR_RESULT_DURABLY_RECORDED', ...reference } as never)
  assert.equal(exhausted.getSnapshot().value, 'retryExhausted')

  const expired = createActor(notificationCalendarArtifactMachine, { input: { calendarArtifactKey: 'calendar-expiry', checkpointRevision: 0, nowEpochSeconds: epoch(0), expiresAtEpochSeconds: epoch(10), maxAttempts: 3, nextAttemptAtEpochSeconds: epoch(0) } }).start()
  expired.send({ type: 'CALENDAR_CLOCK_TICK', expectedClockRevision: 0, nowEpochSeconds: epoch(10) } as never)
  assert.equal(expired.getSnapshot().context.pendingCheckpoint?.outcome, 'expired')
  reference = calendarReference(expired)
  expired.send({ type: 'CALENDAR_RESULT_PERSISTENCE_REQUESTED', ...reference } as never)
  expired.send({ type: 'CALENDAR_RESULT_DURABLY_RECORDED', ...reference } as never)
  assert.equal(expired.getSnapshot().value, 'expired')
})
