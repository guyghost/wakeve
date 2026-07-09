import assert from 'node:assert/strict'
import test from 'node:test'
import { createActor } from 'xstate'

import {
  canMutateVote,
  calendarArtifactKey,
  confirmationEffectKeys,
  deliveryKey,
  pollConfirmationWorkflowMachine,
  recipientKey,
  type PollConfirmationInput,
  type RepositoryProjection,
} from './poll-confirmation-workflow.machine.ts'

const eligible: PollConfirmationInput['eligibility'] = {
  repositoryAvailable: true,
  eventExists: true,
  actorIsOrganizer: true,
  eventStatus: 'POLLING',
  pollHasVotes: true,
  validSlotIds: ['slot-1', 'slot-2'],
  confirmedSlotId: null,
  votingDeadline: '2026-07-09T12:00:00.000Z',
  now: '2026-07-09T12:00:00.000Z',
}

const input = (
  overrides: Partial<PollConfirmationInput['eligibility']> = {},
): PollConfirmationInput => ({
  eventId: 'event-1',
  actorId: 'organizer-1',
  eligibility: { ...eligible, ...overrides },
})

const effectOutbox = {
  domainEventId: 'poll-date-confirmed:event-1:slot-1:v1',
  effectKey: 'poll-date-confirmed:event-1:slot-1:v1:confirmation',
} as const

const actorFor = (machineInput = input()) => {
  const actor = createActor(pollConfirmationWorkflowMachine, {
    input: machineInput,
  }).start()
  return actor
}

const openAndSubmit = (actor: ReturnType<typeof actorFor>) => {
  actor.send({ type: 'OPEN_CONFIRM_PROMPT', slotId: 'slot-1' })
  actor.send({ type: 'SUBMIT_CONFIRMATION', operationId: 'operation-1' })
}

test('nominal commit reaches confirmed.synced and emits success once', () => {
  const actor = actorFor()
  openAndSubmit(actor)
  actor.send({
    type: 'CONFIRMATION_COMMITTED',
    receipt: {
      receiptId: 'receipt-1', operationId: 'operation-1', eventId: 'event-1',
      slotId: 'slot-1', decisionSyncStatus: 'serverAcknowledged',
      effectDispatchStatus: 'queued', effectOutbox,
    },
  })
  assert.equal(actor.getSnapshot().matches({ confirmed: 'synced' }), true)
  assert.deepEqual(actor.getSnapshot().context.effects, [
    'presentConfirmPrompt', 'dispatchConfirmationCommand',
    'successFeedback', 'navigationEligible',
  ])
})

test('cancellation returns to results with zero business effects', () => {
  const actor = actorFor()
  actor.send({ type: 'OPEN_CONFIRM_PROMPT', slotId: 'slot-1' })
  actor.send({ type: 'CANCEL_CONFIRMATION' })
  assert.equal(actor.getSnapshot().matches('reviewingResults'), true)
  assert.equal(actor.getSnapshot().context.selectedSlotId, null)
  assert.deepEqual(actor.getSnapshot().context.effects, [
    'presentConfirmPrompt', 'dismissConfirmPrompt',
  ])
})

test('duplicate submit while confirming is coalesced', () => {
  const actor = actorFor()
  openAndSubmit(actor)
  actor.send({ type: 'SUBMIT_CONFIRMATION', operationId: 'operation-2' })
  assert.equal(actor.getSnapshot().matches('confirming'), true)
  assert.equal(actor.getSnapshot().context.operationId, 'operation-1')
  assert.equal(actor.getSnapshot().context.effects.filter(
    (effect) => effect === 'dispatchConfirmationCommand').length, 1)
})

test('retryable failure retries with the same operation id', () => {
  const actor = actorFor()
  openAndSubmit(actor)
  actor.send({ type: 'CONFIRMATION_FAILED', operationId: 'operation-1',
    error: { code: 'LOCAL_PERSISTENCE_FAILED', retryable: true } })
  assert.equal(actor.getSnapshot().matches('failed'), true)
  actor.send({ type: 'RETRY_CONFIRMATION' })
  assert.equal(actor.getSnapshot().matches('confirming'), true)
  assert.equal(actor.getSnapshot().context.operationId, 'operation-1')
})

test('permission and eligibility failures are typed and do not dispatch', () => {
  for (const [overrides, code] of [
    [{ repositoryAvailable: false }, 'REPOSITORY_UNAVAILABLE'],
    [{ eventExists: false }, 'EVENT_NOT_FOUND'],
    [{ actorIsOrganizer: false }, 'NOT_ORGANIZER'],
    [{ eventStatus: 'DRAFT' }, 'INVALID_EVENT_STATUS'],
    [{ pollHasVotes: false }, 'NO_VOTES'],
    [{ confirmedSlotId: 'slot-2' }, 'ALREADY_CONFIRMED_DIFFERENT_SLOT'],
  ] as const) {
    const actor = actorFor(input(overrides))
    actor.send({ type: 'OPEN_CONFIRM_PROMPT', slotId: 'slot-1' })
    actor.send({ type: 'SUBMIT_CONFIRMATION', operationId: 'operation-1' })
    assert.equal(actor.getSnapshot().matches('failed'), true)
    assert.equal(actor.getSnapshot().context.failure?.code, code)
    assert.equal(actor.getSnapshot().context.effects.includes('dispatchConfirmationCommand'), false)
  }
})

test('an unknown slot cannot open the confirmation prompt', () => {
  const actor = actorFor(input({ validSlotIds: [] }))
  actor.send({ type: 'OPEN_CONFIRM_PROMPT', slotId: 'slot-1' })
  assert.equal(actor.getSnapshot().matches('reviewingResults'), true)
})

test('deadline closes votes but never rejects an eligible confirmation', () => {
  assert.equal(canMutateVote(eligible), false)
  const actor = actorFor()
  openAndSubmit(actor)
  assert.equal(actor.getSnapshot().matches('confirming'), true)
})

test('same-slot replay remains eligible without requiring mutable poll votes', () => {
  const actor = actorFor(input({
    eventStatus: 'CONFIRMED',
    confirmedSlotId: 'slot-1',
    pollHasVotes: false,
  }))
  openAndSubmit(actor)
  assert.equal(actor.getSnapshot().matches('confirming'), true)
})

test('confirmed event with a different slot returns the typed conflict', () => {
  const actor = actorFor(input({
    eventStatus: 'CONFIRMED',
    confirmedSlotId: 'slot-2',
  }))
  openAndSubmit(actor)
  assert.equal(actor.getSnapshot().matches('failed'), true)
  assert.equal(actor.getSnapshot().context.failure?.code,
    'ALREADY_CONFIRMED_DIFFERENT_SLOT')
  assert.equal(actor.getSnapshot().context.effects.includes('successFeedback'), false)
  assert.equal(actor.getSnapshot().context.effects.includes('navigationEligible'), false)
})

test('local effect identity contains exactly one domain envelope key', () => {
  const keys = confirmationEffectKeys('event-1', 'slot-1')
  assert.equal(keys.domainEventId, 'poll-date-confirmed:event-1:slot-1:v1')
  assert.equal(keys.effectKey, `${keys.domainEventId}:confirmation`)
  assert.deepEqual(Object.keys(keys), ['domainEventId', 'effectKey'])
})

test('backend fan-out identities derive deterministically without becoming local rows', () => {
  const effectKey = confirmationEffectKeys('event-1', 'slot-1').effectKey
  const recipient = recipientKey(effectKey, 'participant-7', 'push')
  assert.equal(recipient, `${effectKey}:participant-7:push`)
  assert.equal(deliveryKey(recipient, 'installation-3', 'apns'),
    `${recipient}:installation-3:apns`)
  assert.equal(calendarArtifactKey(effectKey, 'participant-7', 'ics'),
    `${effectKey}:participant-7:ics`)
})

test('a conflict result never produces success or navigation', () => {
  const actor = actorFor()
  openAndSubmit(actor)
  actor.send({ type: 'CONFIRMATION_CONFLICT', operationId: 'operation-1',
    code: 'ALREADY_CONFIRMED_DIFFERENT_SLOT' })
  assert.equal(actor.getSnapshot().matches('failed'), true)
  assert.equal(actor.getSnapshot().context.failure?.code,
    'ALREADY_CONFIRMED_DIFFERENT_SLOT')
  assert.equal(actor.getSnapshot().context.effects.includes('successFeedback'), false)
  assert.equal(actor.getSnapshot().context.effects.includes('navigationEligible'), false)
})

test('offline commit is locally terminal pending sync then becomes synced', () => {
  const actor = actorFor()
  openAndSubmit(actor)
  actor.send({ type: 'CONFIRMATION_COMMITTED', receipt: {
    receiptId: 'receipt-1', operationId: 'operation-1', eventId: 'event-1',
    slotId: 'slot-1', decisionSyncStatus: 'localPending',
    effectDispatchStatus: 'partiallyProcessed', effectOutbox,
  } })
  assert.equal(actor.getSnapshot().matches({ confirmed: 'pendingSync' }), true)
  actor.send({ type: 'SYNC_FAILED', receiptId: 'receipt-1', error: 'offline' })
  actor.send({ type: 'OPEN_CONFIRM_PROMPT', slotId: 'slot-2' })
  assert.equal(actor.getSnapshot().matches({ confirmed: 'pendingSync' }), true)
  actor.send({ type: 'SYNC_COMPLETED', receiptId: 'receipt-1' })
  assert.equal(actor.getSnapshot().matches({ confirmed: 'synced' }), true)
})

test('mismatched operation result is ignored', () => {
  const actor = actorFor()
  openAndSubmit(actor)
  actor.send({ type: 'CONFIRMATION_COMMITTED', receipt: {
    receiptId: 'wrong', operationId: 'other', eventId: 'event-1',
    slotId: 'slot-1', decisionSyncStatus: 'serverAcknowledged',
    effectDispatchStatus: 'terminalWithFailures', effectOutbox,
  } })
  assert.equal(actor.getSnapshot().matches('confirming'), true)
})

test('a receipt with a provider-like local effect key is ignored', () => {
  const actor = actorFor()
  openAndSubmit(actor)
  actor.send({ type: 'CONFIRMATION_COMMITTED', receipt: {
    receiptId: 'wrong-envelope', operationId: 'operation-1', eventId: 'event-1',
    slotId: 'slot-1', decisionSyncStatus: 'serverAcknowledged',
    effectDispatchStatus: 'queued', effectOutbox: {
      domainEventId: effectOutbox.domainEventId,
      effectKey: `${effectOutbox.domainEventId}:notification`,
    },
  } })
  assert.equal(actor.getSnapshot().matches('confirming'), true)
  assert.equal(actor.getSnapshot().context.effects.includes('successFeedback'), false)
})

test('rehydration restores pending state without command or navigation replay', () => {
  const actor = actorFor()
  const projection: RepositoryProjection = {
    kind: 'confirmed', eventId: 'event-1', slotId: 'slot-1',
    receiptId: 'receipt-1', decisionSyncStatus: 'localPending',
    effectDispatchStatus: 'queued',
  }
  actor.send({ type: 'REHYDRATE', projection })
  assert.equal(actor.getSnapshot().matches({ confirmed: 'pendingSync' }), true)
  assert.deepEqual(actor.getSnapshot().context.effects, [])
})

test('rehydration from failed attempt returns to unchanged review state', () => {
  const actor = actorFor()
  openAndSubmit(actor)
  actor.send({ type: 'CONFIRMATION_FAILED', operationId: 'operation-1',
    error: { code: 'LOCAL_PERSISTENCE_FAILED', retryable: true } })
  actor.send({ type: 'REHYDRATE', projection: { kind: 'reviewing', eventId: 'event-1' } })
  assert.equal(actor.getSnapshot().matches('reviewingResults'), true)
  assert.equal(actor.getSnapshot().context.failure, null)
})

test('legacyApplied rehydrates without pending sync, delivery, haptic, or navigation effects', () => {
  const actor = actorFor()
  actor.send({
    type: 'REHYDRATE',
    projection: {
      kind: 'legacyApplied',
      eventId: 'event-1',
      slotId: 'slot-1',
      receiptId: 'legacy-confirmation:event-1',
    },
  })

  assert.equal(actor.getSnapshot().matches({ confirmed: 'legacyApplied' }), true)
  assert.equal(actor.getSnapshot().context.decisionSyncStatus, null)
  assert.equal(actor.getSnapshot().context.effectDispatchStatus, null)
  assert.equal(actor.getSnapshot().context.diagnosticReason, null)
  assert.deepEqual(actor.getSnapshot().context.effects, [])
})

test('read-only legacy replay never becomes pending or emits completion effects', () => {
  const actor = actorFor()
  openAndSubmit(actor)
  actor.send({
    type: 'CONFIRMATION_READ_ONLY',
    operationId: 'operation-1',
    projection: {
      kind: 'legacyApplied',
      eventId: 'event-1',
      slotId: 'slot-1',
      receiptId: 'legacy-confirmation:event-1',
    },
  })

  assert.equal(actor.getSnapshot().matches({ confirmed: 'legacyApplied' }), true)
  assert.equal(actor.getSnapshot().context.decisionSyncStatus, null)
  assert.equal(actor.getSnapshot().context.effectDispatchStatus, null)
  assert.equal(actor.getSnapshot().context.effects.includes('successFeedback'), false)
  assert.equal(actor.getSnapshot().context.effects.includes('navigationEligible'), false)
})

test('quarantined legacy confirmation remains diagnostics-only and refuses confirmation effects', () => {
  const actor = actorFor()
  actor.send({
    type: 'REHYDRATE',
    projection: {
      kind: 'quarantined',
      eventId: 'event-1',
      reason: 'missing-or-invalid-confirmed-date',
    },
  })
  actor.send({ type: 'OPEN_CONFIRM_PROMPT', slotId: 'slot-1' })

  assert.equal(actor.getSnapshot().matches('quarantined'), true)
  assert.equal(actor.getSnapshot().context.decisionSyncStatus, null)
  assert.equal(actor.getSnapshot().context.effectDispatchStatus, null)
  assert.equal(actor.getSnapshot().context.diagnosticReason, 'missing-or-invalid-confirmed-date')
  assert.deepEqual(actor.getSnapshot().context.effects, [])
})

test('confirmed decision rejects cancellation, retry and a different confirmation', () => {
  const actor = actorFor()
  openAndSubmit(actor)
  actor.send({ type: 'CONFIRMATION_COMMITTED', receipt: {
    receiptId: 'receipt-1', operationId: 'operation-1', eventId: 'event-1',
    slotId: 'slot-1', decisionSyncStatus: 'serverAcknowledged',
    effectDispatchStatus: 'queued', effectOutbox,
  } })
  actor.send({ type: 'CANCEL_CONFIRMATION' })
  actor.send({ type: 'RETRY_CONFIRMATION' })
  actor.send({ type: 'OPEN_CONFIRM_PROMPT', slotId: 'slot-2' })
  assert.equal(actor.getSnapshot().matches({ confirmed: 'synced' }), true)
  assert.equal(actor.getSnapshot().context.selectedSlotId, 'slot-1')
})
