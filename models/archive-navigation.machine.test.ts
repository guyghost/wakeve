import assert from 'node:assert/strict'
import test from 'node:test'
import { createActor } from 'xstate'

import { archiveNavigationMachine } from './archive-navigation.machine.ts'

for (const exitEvent of ['CLOSE', 'RETURN'] as const) {
  test(`${exitEvent} closes archive and navigates to Library exactly once`, () => {
    const actor = createActor(archiveNavigationMachine).start()
    actor.send({ type: exitEvent })

    assert.equal(actor.getSnapshot().matches('closed'), true)
    assert.equal(actor.getSnapshot().status, 'done')
    assert.deepEqual(actor.getSnapshot().context.effects, ['NavigateToLibrary'])

    actor.send({ type: exitEvent })
    assert.deepEqual(actor.getSnapshot().context.effects, ['NavigateToLibrary'])
  })
}
