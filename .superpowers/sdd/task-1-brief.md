### Task 1: Executable Projection Model and Review Matrix

**Delegation:** `@tests` writes model tests first; `@codegen` writes the machine; `@review` checks every nominal/error/cancellation/retry/permission/offline/conflict/terminal branch before Task 2.

**Files:**
- Create: `models/product-language.machine.ts`
- Create: `models/product-language.machine.test.ts`
- Create: `models/product-language.review.md`
- Create: `models/product-language.inventory.json`
- Modify: `openspec/changes/standardize-product-language/tasks.md`

**Interfaces:**
- Consumes: `EventStatus = DRAFT | POLLING | COMPARING | CONFIRMED | ORGANIZING | FINALIZED`, deterministic role/facts/action inputs.
- Produces: XState v5 `productLanguageMachine`, pure `projectProductLanguage(input: ProjectionInput): ProjectionOutput`, reviewed inventory and branch evidence.

- [ ] **Step 1: Create a neutral compilable machine scaffold, then write the failing model tests**

Create `models/product-language.machine.ts` first so the RED run tests behavior rather than module resolution:

```ts
import { setup } from 'xstate'
export type EventStatus = 'DRAFT' | 'POLLING' | 'COMPARING' | 'CONFIRMED' | 'ORGANIZING' | 'FINALIZED'
export type ProjectionInput = { status: EventStatus; role: 'ORGANIZER' | 'PARTICIPANT'; pendingFacts: readonly ('LOCAL_MUTATION' | 'SYNC_CONFLICT')[]; allowedAction: 'EDIT' | 'RETRY_SYNC' | null }
export type ProjectionOutput = { domainStatus: EventStatus; titleKey: string; statusKey: string | null; primaryActionKey: string | null; sharedConfirmation: boolean }
export const projectProductLanguage = (input: ProjectionInput): ProjectionOutput => ({ domainStatus: input.status, titleKey: 'unmodeled', statusKey: null, primaryActionKey: null, sharedConfirmation: false })
export const productLanguageMachine = setup({ types: { context: {} as { projection: ProjectionOutput }, input: {} as ProjectionInput } }).createMachine({ id: 'productLanguage', context: ({ input }) => ({ projection: projectProductLanguage(input) }), initial: 'ready', states: { ready: {} } })
```

```ts
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
```

- [ ] **Step 2: Run the model test and confirm RED**

Run: `node --experimental-strip-types --test models/product-language.machine.test.ts`

Expected: test process executes and FAILS an assertion such as `actual 'unmodeled' !== expected 'event.state.draft'`; test infrastructure completes normally.

- [ ] **Step 3: Implement the minimal pure model**

```ts
import { assign, setup } from 'xstate'

export type EventStatus = "DRAFT" | "POLLING" | "COMPARING" | "CONFIRMED" | "ORGANIZING" | "FINALIZED";
export type ProjectionInput = { status: EventStatus; role: "ORGANIZER" | "PARTICIPANT"; pendingFacts: readonly ("LOCAL_MUTATION" | "SYNC_CONFLICT")[]; allowedAction: "EDIT" | "RETRY_SYNC" | null };
export type ProjectionOutput = { domainStatus: EventStatus; titleKey: string; statusKey: string | null; primaryActionKey: string | null; sharedConfirmation: boolean };

const titleKeys: Record<EventStatus, string> = {
  DRAFT: "event.state.draft", POLLING: "event.state.polling", COMPARING: "event.state.comparing",
  CONFIRMED: "event.state.confirmed", ORGANIZING: "event.state.organizing", FINALIZED: "event.state.finalized",
};

export function projectProductLanguage(input: ProjectionInput): ProjectionOutput {
  const pendingSync = input.pendingFacts.includes("LOCAL_MUTATION");
  return { domainStatus: input.status, titleKey: titleKeys[input.status], statusKey: pendingSync ? "sync.waiting" : null,
    primaryActionKey: input.allowedAction === "RETRY_SYNC" ? "sync.retry" : input.allowedAction === "EDIT" ? "event.action.continue" : null,
    sharedConfirmation: !pendingSync };
}

export const productLanguageMachine = setup({
  types: { context: {} as { input: ProjectionInput; projection: ProjectionOutput }, input: {} as ProjectionInput, events: {} as { type: 'SYNC_SUCCEEDED' } | { type: 'SYNC_FAILED' } },
  guards: { isTerminal: ({ context }) => context.input.status === 'FINALIZED', hasPendingSync: ({ context }) => context.input.pendingFacts.includes('LOCAL_MUTATION') },
  actions: { markSynced: assign(({ context }) => { const input = { ...context.input, pendingFacts: context.input.pendingFacts.filter(f => f !== 'LOCAL_MUTATION'), allowedAction: 'EDIT' as const }; return { input, projection: projectProductLanguage(input) } }) },
}).createMachine({
  id: 'productLanguage',
  context: ({ input }) => ({ input, projection: projectProductLanguage(input) }),
  initial: 'classify',
  states: {
    classify: { always: [{ guard: 'isTerminal', target: 'terminal' }, { guard: 'hasPendingSync', target: 'pendingSync' }, { target: 'ready' }] },
    ready: {},
    pendingSync: { on: { SYNC_SUCCEEDED: { target: 'ready', actions: 'markSynced' }, SYNC_FAILED: 'syncFailed' } },
    syncFailed: { on: { SYNC_SUCCEEDED: { target: 'ready', actions: 'markSynced' } } },
    terminal: { type: 'final' },
  },
})
```

- [ ] **Step 4: Run tests, complete the review matrix, and confirm GREEN**

Generate the inventory with this deterministic command and review every row:

```bash
rg -l 'Text\(|Button\(|contentDescription|accessibilityLabel|String\(localized:|Notification|Siri|Wakeve AI|Générer|Generate|Inbox|Scenario|Party Animal|Social Butterfly|Chatterbox|Event Master' composeApp/src/androidMain iosApp/src shared/src/commonMain server/src/main \
  | sort | jq -R -s '{version:1,files:(split("\n")|map(select(length>0))|map({path:.,category:(if test("AI|Suggestion|Recommendation";"i") then "ai-entry-point" elif test("Gamification|Badge|Achievement|Profile";"i") then "gamification-or-profile" elif startswith("composeApp/") then "android-ui" elif startswith("iosApp/") then "ios-ui" elif test("Notification|Siri";"i") then "delivery-or-siri" else "shared" end)}))}' > models/product-language.inventory.json
```

Run: `node --experimental-strip-types --test models/product-language.machine.test.ts && openspec validate standardize-product-language --strict`

Expected: Node reports 2 tests PASS; OpenSpec reports the change valid. `jq -e '.version == 1 and (.files | length > 0) and all(.files[]; (.path | length > 0) and (.category | length > 0))' models/product-language.inventory.json` exits 0. Record all ten review cases from `design.md` in `models/product-language.review.md`, then mark OpenSpec tasks 1.1–2.5 complete only when `@review` accepts them.

- [ ] **Step 5: Commit the reviewed model**

```bash
git add models/product-language.machine.ts models/product-language.machine.test.ts models/product-language.review.md models/product-language.inventory.json openspec/changes/standardize-product-language/tasks.md
git commit -m "docs(product-language): model deterministic projections"
```

