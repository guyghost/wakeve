# Change: Harden the iOS poll confirmation workflow

## Why

Confirming a poll date is a business-state transition, not a SwiftUI persistence shortcut. The current iOS results flow can mutate `Event.status` directly while the shared workflow applies a different set of guards and queues downstream notification/calendar work separately. This creates production risks: the wrong slot can be recorded, a partial write can look confirmed, duplicate taps can duplicate side effects, offline work can be presented as server-confirmed, and navigation can occur without a complete decision record.

The deadline semantics are also contradictory. Poll results are expected to remain useful after voting closes, yet the shared confirmation path currently treats an elapsed voting deadline as a reason to reject the organizer's decision. This proposal resolves that ambiguity: the deadline closes voting; it does not close organizer confirmation.

## What Changes

- Add an explicit XState model under `/models` for the iOS interaction flow `reviewingResults -> confirmPrompt -> confirming -> confirmed | failed`, including cancellation, retry, permission, offline, pending-sync, rehydration, and duplicate-dispatch behavior.
- Make the shared deterministic workflow the only authority that can confirm a time-slot poll. SwiftUI renders state and dispatches typed events; it does not call `updateEventStatus` or another repository mutation directly.
- Route confirmation through one application-scoped repository instance and one typed confirmation command.
- Commit the event transition, exact selected-slot record, exactly one `confirmation_effect_outbox` domain envelope, sync metadata, and a committed workflow receipt in one local SQLDelight transaction. Backend notification and calendar fan-out occurs only after later envelope ingestion. Navigation is emitted only from the local committed receipt.
- Make confirmation idempotent: repeated or concurrent dispatches for the same decision return the existing result without duplicate outbox work, while a later request for a different slot is rejected as a conflict.
- Treat an offline local commit as `confirmed.pendingSync`, not as remote/server confirmation. The organizer can continue locally while Wakeve clearly shows that synchronization and participant delivery are pending.
- Inject a clock for deadline evaluation and all confirmation timestamps. At the deadline, voting becomes read-only; confirmation remains allowed.
- Add a release guard that fails if `updateEventStatus` is called from `iosApp/src/Views`.
- Add RED tests before production implementation for model transitions, shared guards, atomic rollback, idempotency, offline replay, iOS rendering/dispatch, and architecture boundaries.

**Behavior change:** an organizer with an otherwise valid confirmation request can confirm after the voting deadline. The deadline remains a hard cutoff for new or changed votes.

## Product Excellence Fit

- **Event relevance:** this hardens the core private-group decision that turns poll results into an actionable event date and unlocks organization work.
- **Mental-load reduction:** one explicit confirmation prompt, deterministic retry behavior, and automatic durable invitation/calendar work remove uncertainty and manual follow-up.
- **State clarity:** the UI distinguishes `reviewing`, `confirming`, `confirmed locally / sync pending`, `confirmed and synced`, and `failed`. It identifies the organizer as the responsible actor and scenarios as the next useful action.
- **Mobile usability:** cancellation is immediate and side-effect free, duplicate taps are coalesced, progress is visible, and errors offer a focused retry without re-entering data.
- **Premium execution:** success is acknowledged only after a durable local commit; failures never produce false success, haptics, or navigation.
- **Event-scoped collaboration:** notification and calendar artifacts are tied to the confirmed event decision. This does not add generic chat, task, calendar, or workspace behavior.
- **AI boundary:** no LLM participates in selection, permission, deadline, persistence, conflict resolution, or navigation. The deterministic model decides every transition.

## Impact

- **Affected specs:** `event-organization`, `workflow-coordination`.
- **Anticipated shared surfaces:** `EventManagementContract`, `EventManagementStateMachine`, `EventRepositoryInterface`, `DatabaseEventRepository`, SQLDelight confirmation/outbox/sync schema, and confirmation/outbox workers.
- **Anticipated iOS surfaces:** `PollResultsView`, its state-machine-backed view model/adapter, `IosFactory`, localized pending/error copy, and iOS contract tests.
- **Verification surfaces:** shared state-machine tests, repository transaction tests, XState model tests, iOS view/view-model tests, and `scripts/test-critical-release-gates.sh`.
- **Related active change:** `align-ios-android-feature-parity` may touch the poll route. This narrower workflow hardening must be reviewed first and parity work must consume the resulting model rather than introduce another confirmation path.
- **No production code in this change proposal:** implementation remains blocked until the XState model and this OpenSpec change are reviewed and approved.

## Reviewed Boundary Amendment

The atomic local result is the confirmed decision plus exactly one `confirmation_effect_outbox` domain envelope owned by `DatabaseEventRepository`, not one notification row and one calendar artifact. The backend later acknowledges the envelope, resolves participants, and independently fans out `notification_recipient`/`notification_delivery` and per-participant calendar work. No atomicity spans the two databases.

`decisionSyncStatus` and `effectDispatchStatus` remain separate. The rollout depends on backend envelope ingestion and the APNs delivery-authority migration before participant delivery is enabled. Stable identities are `domainEventId`, `effectKey`, `recipientKey`, `deliveryKey`, and `calendarArtifactKey`; every consumer has a separate retry and acknowledgement contract.
