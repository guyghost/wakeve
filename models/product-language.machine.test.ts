import assert from 'node:assert/strict'
import test from 'node:test'
import { createActor } from 'xstate'
import { productLanguageMachine, projectProductLanguage } from './product-language.machine.ts'

const statuses = [
  ['DRAFT', 'event.state.draft'], ['POLLING', 'event.state.polling'],
  ['COMPARING', 'event.state.comparing'], ['CONFIRMED', 'event.state.confirmed'],
  ['ORGANIZING', 'event.state.organizing'], ['FINALIZED', 'event.state.finalized'],
] as const

test('machine projects every domain status without changing its identity', () => {
  for (const [status, titleKey] of statuses) {
    const actor = createActor(productLanguageMachine, { input: { status, role: 'ORGANIZER', pendingFacts: [], allowedAction: status === 'FINALIZED' ? null : 'EDIT' } }).start()
    assert.equal(actor.getSnapshot().value, status === 'FINALIZED' ? 'terminal' : 'ready')
    assert.equal(actor.getSnapshot().context.projection.titleKey, titleKey)
    assert.equal(actor.getSnapshot().context.projection.domainStatus, status)
  }
})

test('offline mutation enters pendingSync and retry recomputes deterministically', () => {
  const input = { status: 'CONFIRMED', role: 'ORGANIZER', pendingFacts: ['LOCAL_MUTATION'], allowedAction: 'RETRY_SYNC' } as const
  const actor = createActor(productLanguageMachine, { input }).start()
  assert.equal(actor.getSnapshot().value, 'pendingSync')
  assert.deepEqual(projectProductLanguage(input), { domainStatus: 'CONFIRMED', titleKey: 'event.state.confirmed', statusKey: 'sync.waiting', detailKey: null, primaryActionKey: 'sync.retry', secondaryActionKey: null, sharedConfirmation: false })
  actor.send({ type: 'SYNC_SUCCEEDED' })
  assert.equal(actor.getSnapshot().value, 'ready')
})

test('sync conflict names affected data and offers deterministic resolution', () => {
  const input = { status: 'CONFIRMED', role: 'ORGANIZER', pendingFacts: ['SYNC_CONFLICT'], allowedAction: 'RESOLVE_CONFLICT' } as const
  const actor = createActor(productLanguageMachine, { input }).start()
  assert.equal(actor.getSnapshot().value, 'syncConflict')
  assert.deepEqual(actor.getSnapshot().context.projection, {
    domainStatus: 'CONFIRMED', titleKey: 'event.state.confirmed', statusKey: 'sync.conflict',
    detailKey: 'sync.conflict.event-details', primaryActionKey: 'sync.resolve', secondaryActionKey: 'sync.retry', sharedConfirmation: false,
  })
  actor.send({ type: 'RESOLVE_CONFLICT' })
  assert.equal(actor.getSnapshot().value, 'pendingSync')
  assert.equal(actor.getSnapshot().context.projection.statusKey, 'sync.waiting')
})

test('sync failure exposes retry and only retry can restart synchronization', () => {
  const input = { status: 'CONFIRMED', role: 'ORGANIZER', pendingFacts: ['LOCAL_MUTATION'], allowedAction: 'RETRY_SYNC' } as const
  const actor = createActor(productLanguageMachine, { input }).start()
  actor.send({ type: 'SYNC_FAILED' })
  assert.equal(actor.getSnapshot().value, 'syncFailed')
  assert.equal(actor.getSnapshot().context.projection.statusKey, 'sync.failed')
  assert.equal(actor.getSnapshot().context.projection.primaryActionKey, 'sync.retry')
  actor.send({ type: 'SYNC_SUCCEEDED' })
  assert.equal(actor.getSnapshot().value, 'syncFailed')
  actor.send({ type: 'RETRY_SYNC' })
  assert.equal(actor.getSnapshot().value, 'pendingSync')
})

test('validation failure is explicit and preserves domain status', () => {
  const input = { status: 'DRAFT', role: 'ORGANIZER', pendingFacts: [], allowedAction: 'EDIT', validation: 'INVALID_FIELD' } as const
  const actor = createActor(productLanguageMachine, { input }).start()
  assert.equal(actor.getSnapshot().value, 'validationError')
  assert.equal(actor.getSnapshot().context.projection.statusKey, 'validation.invalid-field')
  assert.equal(actor.getSnapshot().context.projection.domainStatus, 'DRAFT')
})

test('cancelled operation never projects persistence or success', () => {
  const input = { status: 'ORGANIZING', role: 'ORGANIZER', pendingFacts: [], allowedAction: null, cancellation: 'CANCELLED' } as const
  const actor = createActor(productLanguageMachine, { input }).start()
  assert.equal(actor.getSnapshot().value, 'cancelled')
  assert.equal(actor.getSnapshot().context.projection.statusKey, 'operation.cancelled')
  assert.equal(actor.getSnapshot().context.projection.sharedConfirmation, false)
})

for (const permission of ['DENIED', 'RESTRICTED'] as const) {
  test(`permission ${permission.toLowerCase()} names impact and recovery`, () => {
    const input = { status: 'CONFIRMED', role: 'PARTICIPANT', pendingFacts: [], allowedAction: 'OPEN_SETTINGS', permission } as const
    const actor = createActor(productLanguageMachine, { input }).start()
    assert.equal(actor.getSnapshot().value, 'permissionBlocked')
    assert.equal(actor.getSnapshot().context.projection.statusKey, `permission.${permission.toLowerCase()}`)
    assert.equal(actor.getSnapshot().context.projection.detailKey, 'permission.impact.event-update')
    assert.equal(actor.getSnapshot().context.projection.primaryActionKey, 'permission.open-settings')
  })
}

test('terminal state suppresses caller-provided editing action', () => {
  const projection = projectProductLanguage({ status: 'FINALIZED', role: 'ORGANIZER', pendingFacts: [], allowedAction: 'EDIT' })
  assert.equal(projection.primaryActionKey, null)
})

for (const aiOutcome of ['UNAVAILABLE', 'REJECTED'] as const) {
  test(`AI ${aiOutcome.toLowerCase()} keeps manual path without state transition`, () => {
    const input = { status: 'COMPARING', role: 'ORGANIZER', pendingFacts: [], allowedAction: 'CONTINUE_MANUALLY', aiOutcome } as const
    const actor = createActor(productLanguageMachine, { input }).start()
    assert.equal(actor.getSnapshot().value, 'manualFallback')
    assert.equal(actor.getSnapshot().context.projection.statusKey, `ai.${aiOutcome.toLowerCase()}`)
    assert.equal(actor.getSnapshot().context.projection.primaryActionKey, 'ai.continue-manually')
    assert.equal(actor.getSnapshot().context.projection.domainStatus, 'COMPARING')
  })
}
