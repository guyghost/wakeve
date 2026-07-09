## Context

The iOS poll-results screen currently owns loading results, opening a confirmation dialog, calling a repository mutation, checking the reloaded event, presenting success/error UI, and navigating. The shared KMP workflow separately owns organizer, status, vote, slot, and downstream-work rules. These two authorities can diverge.

Date confirmation spans more than `Event.status`:

1. the event becomes `CONFIRMED`;
2. the exact selected time slot becomes the confirmed-date record;
3. exactly one local confirmation domain-effect envelope becomes durable work for later backend fan-out;
4. sync metadata records whether the local decision is pending remote acknowledgement;
5. the UI may navigate to the next event-organization step.

Production correctness requires these outcomes to derive from one deterministic command and one committed local result.

## Goals / Non-Goals

### Goals

- Model the complete interaction and domain outcome before implementation.
- Keep one deterministic shared owner for confirmation guards and transitions.
- Make local persistence atomic, idempotent, crash-safe, and offline-first.
- Make the exact selected slot and downstream artifacts reviewable.
- Make cancellation, failure, retry, duplicate dispatch, pending sync, and rehydration explicit.
- Resolve deadline semantics normatively.
- Prevent SwiftUI views from reintroducing business-state mutations.

### Non-Goals

- Change YES/MAYBE/NO scoring or automatic best-slot calculation.
- Automatically choose or confirm a slot for the organizer.
- Redesign scenario-matrix final selection.
- Redesign APNs delivery, calendar rendering, or server sync protocols beyond the durable confirmation outbox contract.
- Introduce AI into any decision or transition.
- Require remote connectivity before local organization can continue.

## Decisions

### Decision 1: `/models` contains the reviewed XState source of truth

Before Kotlin or Swift production code changes, implementation MUST add and review an XState v5 machine at `models/poll-confirmation-workflow.machine.ts`. Kotlin and Swift types may use platform-idiomatic names, but their observable states, events, guards, effects, and invariants MUST remain traceable to this model.

The model context contains only typed workflow data:

- `eventId`
- `actorId`
- `selectedSlotId`
- `operationId` (stable across retry)
- `confirmationReceiptId` when committed
- `syncStatus` (`synced` or `pendingSync`)
- a typed failure code and retryability

Free text and model-generated content are not transition inputs.

#### States

| State | Meaning | User-visible responsibility |
|---|---|---|
| `reviewingResults` | Results are loaded and no confirmation is pending. | Review scores and select/accept a proposed slot. |
| `confirmPrompt` | A valid slot is staged for explicit organizer confirmation. | Confirm or cancel. |
| `confirming` | One typed confirmation operation is in flight. | Wait; the primary action is disabled. |
| `confirmed.synced` | Local decision is committed and no confirmation sync remains pending. | Continue to scenarios/organization. |
| `confirmed.pendingSync` | Local decision is committed, but remote sync or downstream delivery remains pending. | Continue locally while seeing a clear pending label. |
| `failed` | The attempt did not produce a committed confirmation receipt. | Review a typed error, retry safely, or return to results. |

`confirmed` is terminal for the date decision even though its sync substate can progress from `pendingSync` to `synced`. `failed` is terminal for one attempt but recoverable through an explicit retry or dismissal.

#### Events and transitions

| From | Event | Guard | To | Effects |
|---|---|---|---|---|
| `reviewingResults` | `OPEN_CONFIRM_PROMPT(slotId)` | `canStageSlot` | `confirmPrompt` | Store the staged slot; present the native confirmation UI. |
| `confirmPrompt` | `CANCEL_CONFIRMATION` | none | `reviewingResults` | Clear the staged operation; perform no repository, outbox, haptic-success, or navigation effect. |
| `confirmPrompt` | `SUBMIT_CONFIRMATION(operationId)` | `canSubmitConfirmation` | `confirming` | Capture one stable operation ID and dispatch one shared command. |
| `confirming` | `SUBMIT_CONFIRMATION` | `operationInFlight` | `confirming` | Coalesce/ignore the duplicate; dispatch nothing. |
| `confirming` | `CONFIRMATION_COMMITTED(receipt)` | `receiptMatchesOperation` | `confirmed.synced` or `confirmed.pendingSync` | Apply the committed projection, success feedback, and one navigation eligibility receipt. |
| `confirming` | `CONFIRMATION_FAILED(error)` | `failureMatchesOperation` | `failed` | Store a typed error; do not navigate or show success. |
| `failed` | `RETRY_CONFIRMATION` | `isRetryable` | `confirming` | Reuse the same operation ID and selected slot. |
| `failed` | `DISMISS_FAILURE` | none | `reviewingResults` | Clear error and return to the unchanged results. |
| `confirmed.pendingSync` | `SYNC_COMPLETED(receiptId)` | `receiptMatchesConfirmation` | `confirmed.synced` | Remove the pending label; do not repeat navigation or participant delivery. |
| `confirmed.pendingSync` | `SYNC_FAILED(error)` | `receiptMatchesConfirmation` | `confirmed.pendingSync` | Keep the local decision and expose retryable sync status. |
| any | `REHYDRATE(repositoryProjection)` | projection is valid | derived state | Restore confirmed/pending/error-free state without replaying the confirmation command. |

Dismissal while `confirming` does not roll back or cancel an already-started local transaction. If the view disappears, repository rehydration determines the next state. UI cancellation is intentionally limited to `confirmPrompt`.

#### Guards

Guards are evaluated in this order and revalidated inside the repository transaction where concurrency could invalidate them:

1. one application-scoped repository/database is available;
2. event exists;
3. actor is the event organizer;
4. event is `POLLING`, or is already `CONFIRMED` with the same selected slot (idempotent success);
5. poll exists and contains at least one vote;
6. selected slot exists, belongs to the event, and has a confirmable start value;
7. no different slot has already been confirmed;
8. no different operation is in flight for this machine instance.

An elapsed voting deadline is deliberately absent from the rejection guards. It closes vote mutations, not organizer confirmation.

#### Effects

- `OPEN_CONFIRM_PROMPT` and `CANCEL_CONFIRMATION` are UI-only effects.
- `SUBMIT_CONFIRMATION` calls one shared command through the state machine.
- The repository returns a typed committed, already-committed, conflict, or failed result.
- Success haptics and navigation are derived only from a committed receipt.
- Notification, calendar/invitation, and sync delivery are performed by durable workers from outbox records, never inline from SwiftUI.
- Pending sync is derived from persisted records, not an in-memory Boolean or reachability guess.

### Decision 2: the voting deadline closes votes, not confirmation

The clock instant is captured from an injected clock. At `now >= deadline`, new or changed votes MUST be rejected and results MUST remain reviewable. An organizer MAY confirm an otherwise eligible proposed slot at or after that instant.

This proposal does not introduce a requirement to wait for the deadline before confirming; existing organizer/status/vote/slot guards remain the eligibility rules. If Wakeve later wants a mandatory close-before-confirm policy, that is a separate product decision and OpenSpec change.

All deadline parsing, `confirmedAt`, outbox timestamps, and sync timestamps use the injected clock. One confirmation transaction captures `now` once and reuses that instant. Fixed production timestamps, direct `Clock.System` calls in confirmation logic, and lexicographic timestamp comparisons are prohibited.

### Decision 3: one repository command owns an atomic local decision

The shared state machine calls one typed command equivalent to:

```text
ConfirmPollDate(
  operationId,
  eventId,
  slotId,
  actorId
)
```

One SQLDelight transaction owned by `DatabaseEventRepository` re-reads and validates the event, poll, actor, and slot, then atomically:

1. changes the event from `POLLING` to `CONFIRMED` and stores the selected slot's date projection;
2. upserts the single confirmed-date record for the exact `slotId`;
3. inserts exactly one durable `confirmation_effect_outbox` envelope keyed by `domainEventId` and `effectKey`;
4. inserts the sync operation/pending metadata;
5. inserts an idempotency/confirmation receipt containing the committed decision and next navigation target.

Any failure rolls back all five local outcomes. The transaction creates no backend `notification_recipient`, `notification_delivery`, or calendar artifact. In-memory outbox lists and cross-database atomicity claims do not satisfy this contract. A later sync publisher sends the envelope; backend ingestion acknowledges it idempotently and performs participant fan-out in backend-owned transactions.

Navigation itself is a UI action and cannot run inside a database transaction. Atomicity therefore means its eligibility is part of the committed receipt: the state machine MUST NOT emit navigation without that receipt, MUST emit it at most once for the active successful operation, and MUST NOT emit it after rollback or conflict. On cold rehydration, an already-confirmed event shows the durable next action instead of unexpectedly replaying navigation.

### Decision 4: duplicate dispatch is idempotent and conflicting reselection is explicit

- A retry reuses the original `operationId`.
- Concurrent dispatches for the same event and slot converge on one confirmation receipt.
- Replaying the same decision returns the existing receipt and does not create another confirmed-date, sync, domain-effect envelope, or navigation record. Backend replay converges independently through recipient, delivery, and calendar artifact keys.
- A request for a different slot after confirmation fails with a typed `ALREADY_CONFIRMED_DIFFERENT_SLOT` conflict and changes nothing.
- Database uniqueness constraints, not only disabled buttons, enforce these rules.

### Decision 5: one application-scoped repository instance serves the workflow

`IosFactory`/the composition root provides the state machine and its single SQLDelight-backed repository instance. The iOS results view receives a render projection and event callbacks; it does not receive a write-capable repository for confirmation.

The state machine and repository command are the only mutation path. A release guard MUST fail if `updateEventStatus` appears under `iosApp/src/Views`. The guard SHOULD also reject direct confirmation repository calls from SwiftUI views so a renamed shortcut cannot bypass the model.

### Decision 6: offline confirmation is locally final and remotely pending

If the local transaction commits while offline:

- the event decision is `CONFIRMED` in the local source of truth;
- the UI enters `confirmed.pendingSync` and may unlock local scenario organization;
- copy says the decision is saved locally and sync/delivery is pending;
- it does not claim that the server or participants have received/confirmed the update;
- outbox and sync workers retry idempotently when connectivity returns.

A network or delivery failure after the local commit does not revert the date decision. A local persistence failure before the commit enters `failed` and leaves the event, confirmed-date record, outbox, sync metadata, and navigation unchanged.

## Invariants

1. Exactly one deterministic workflow model owns the poll-confirmation transition.
2. SwiftUI and free text never decide or persist `Event.status`.
3. Cancelling `confirmPrompt` performs zero business side effects.
4. `confirming` has at most one effective operation in flight.
5. `confirmed` implies one atomic receipt covering the event, exact selected slot, durable outbox, and sync metadata.
6. A failed local commit implies none of those records changed and no success/navigation effect occurred.
7. An event has at most one confirmed poll slot; a different later slot is a conflict, not an overwrite.
8. Replaying the same decision cannot duplicate notification, calendar/invitation, sync, or navigation work.
9. `pendingSync` never implies server acknowledgement or participant delivery.
10. At and after the deadline, voting is closed but organizer confirmation remains eligible.
11. All temporal decisions and confirmation timestamps derive from one injected clock instant.
12. Rehydration derives state from the same repository projection and never re-dispatches confirmation implicitly.

## Error Taxonomy

The command returns stable typed failures so UI copy and retry behavior do not depend on exception text:

- `EVENT_NOT_FOUND`
- `NOT_ORGANIZER`
- `INVALID_EVENT_STATUS`
- `NO_VOTES`
- `SLOT_NOT_FOUND`
- `SLOT_NOT_CONFIRMABLE`
- `ALREADY_CONFIRMED_DIFFERENT_SLOT`
- `LOCAL_PERSISTENCE_FAILED` (retryable with the same operation ID)
- `REPOSITORY_UNAVAILABLE` (retryable after rehydration)

An elapsed deadline is not a confirmation failure code.

## Migration Plan

1. **Model gate:** add the XState model and model tests under `/models`; obtain `@review` approval before implementation.
2. **RED gate:** add failing shared, repository, iOS, offline, duplicate-dispatch, deadline, and architecture tests. Record the expected failures before changing production code.
3. **Local additive persistence:** add confirmation receipt, sync metadata, and the unique `confirmation_effect_outbox(domainEventId, effectKey)` owned by `DatabaseEventRepository`.
4. **Safe legacy classification:** map unambiguous confirmed events to receipts but mark them `legacyApplied`; do not create retroactive envelopes or participant sends. Quarantine ambiguous records for diagnostics.
5. **Backend dependency:** deploy idempotent envelope ingestion and acknowledgement before enabling local publication; then add backend recipient, delivery, and calendar projections with consumers disabled.
6. **Shadow-write and reconcile:** shadow-write backend projections, compare sanitized counts/keys, and require zero unexplained divergence before cutover.
7. **Authority cutover:** assign exactly one unique `delivery_authority` for every logical delivery; enable bounded cohorts only after ingestion, recipient expiry, and calendar retry tests pass.
8. **Shared/iOS consumption:** introduce the atomic command and modeled UI, then enable envelope publication. Participant fan-out remains disabled until the backend cutover checkpoint passes.
9. **Enforcement and retirement:** enable architecture gates and retire legacy producers only after the replay horizon and final reconciliation.

## Rollback Plan

- Persistence changes are additive; rollback never deletes receipts, envelopes, recipient rows, delivery rows, or calendar projections.
- Pause local publication or backend consumers at their recorded checkpoint without reverting locally confirmed dates.
- Before changing senders, pause leases and atomically transfer unique `delivery_authority`; two authorities MUST NOT send the same `deliveryKey`.
- If the new confirmation command must be disabled, make iOS confirmation temporarily read-only while results remain visible. Do not restore direct `updateEventStatus` calls from SwiftUI.
- A rollback MUST NOT reinterpret `confirmed.pendingSync` as remote success, create retroactive envelopes for legacy confirmations, or re-send already acknowledged artifacts.
- After restoration, rehydration resumes from persisted event, confirmation, receipt, outbox, and sync records.

## Risks / Trade-offs

- **Extra persistence records:** receipts and durable outbox rows add schema and cleanup work. This is required for crash safety and reviewability.
- **Local confirmation before server acknowledgement:** users can continue offline, but other participants may temporarily see older state. Explicit pending copy and idempotent replay mitigate this.
- **Navigation is not physically transactional:** persisting its eligibility and gating emission on the committed receipt prevents false navigation; cold-start rehydration deliberately shows a next action rather than replaying navigation unexpectedly.
- **Active parity work may overlap the iOS poll route:** implementation sequencing must make parity consume this model and avoid parallel confirmation adapters.
- **Changed deadline semantics:** allowing post-deadline confirmation fixes the current contradiction but needs localized regression coverage at the exact boundary.

## Verification Strategy

- XState model tests cover every allowed and forbidden transition.
- Shared state-machine tests cover guards, typed failures, cancellation mapping, success, retry, duplicate dispatch, and exact deadline semantics with a fixed injected clock.
- SQLDelight tests inject a failure after each transaction step and prove complete rollback.
- Repository tests prove exact-slot storage, same-slot idempotency, different-slot conflict, durable outbox uniqueness, common timestamps, restart, and pending-sync replay.
- iOS tests prove prompt cancellation, disabled/coalesced submit, pending copy, failed retry, accessibility, and navigation only after a committed receipt.
- Architecture tests and release gates reject `updateEventStatus` from `iosApp/src/Views` and reject a second confirmation repository instance/path.
- Manual simulator validation covers online success, offline success, force-quit/rehydration, retry, duplicate taps, Dynamic Type, VoiceOver, Reduce Motion, and light/dark appearance.

## Open Questions

None blocking. The deadline decision is explicit in this proposal: voting closes at the deadline; organizer confirmation remains allowed after it.
