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
  assert.deepEqual(projectProductLanguage(input), { domainStatus: 'CONFIRMED', titleKey: 'event.state.confirmed', statusKey: 'sync.waiting', primaryActionKey: 'sync.retry', sharedConfirmation: false })
  actor.send({ type: 'SYNC_SUCCEEDED' })
  assert.equal(actor.getSnapshot().value, 'ready')
})
