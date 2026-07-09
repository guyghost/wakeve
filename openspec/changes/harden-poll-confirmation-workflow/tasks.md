## 1. Model (mandatory first gate)

- [x] 1.1 `@codegen` Create `models/poll-confirmation-workflow.machine.ts` as an executable XState v5 model for `reviewingResults -> confirmPrompt -> confirming -> confirmed | failed`, including `confirmed.pendingSync`, cancellation, retry, rehydration, permission failures, duplicate dispatch, deadline semantics, effects, and invariants.
  - **Acceptance:** every state, event, guard, effect, and invariant in `design.md` is represented without production Kotlin/Swift imports.
  - **Verify:** model transition tests demonstrate nominal, error, cancellation, retry, offline, duplicate, and terminal paths.
- [x] 1.2 `@review` Review the model against nominal flow, errors, cancellation, retries, permissions, offline/restart behavior, terminal states, atomicity, idempotency, and the after-deadline decision.
  - **Acceptance:** review records explicit approval or actionable findings; implementation remains blocked until approval.
  - **Verify:** no Kotlin, Swift, SQLDelight, or production script change is started before this task is approved.
  - **Evidence:** final review `APPROVED`; 18/18 focused model tests pass and TypeScript strict checking passes for the poll-confirmation model.

## 2. RED tests before implementation

- [x] 2.1 `@tests` Add failing shared state-machine tests for organizer/non-organizer, `POLLING`/invalid status, votes/no votes, valid/invalid slot, prompt cancellation mapping, retry, and one in-flight command. (RED review approved; evidence: `red-tests-2.1-2.3.md`)
  - **Acceptance:** the existing after-deadline rejection expectation is replaced by a RED expectation that confirmation succeeds at and after the deadline while votes remain closed.
  - **Verify:** run `./gradlew :shared:jvmTest --tests "*EventManagementStateMachinePollingConfirmationTest*"` and capture the expected failures.
- [ ] 2.2 `@tests` Add failing repository integration tests for exact-slot selection, atomic event + confirmed-date + durable outbox + sync/receipt commit, injected failure rollback at each write boundary, and one captured clock instant.
  - **Acceptance:** tests fail against split writes, in-memory outbox, fixed clocks, or partial persistence.
  - **Verify:** run `./gradlew :shared:jvmTest --tests "*DatabaseEventRepositoryConfirmDateTest*"` and capture the expected failures.
- [ ] 2.3 `@tests` Add failing idempotency/restart tests for duplicate concurrent dispatch, retry with the same operation ID, same-slot replay, different-slot conflict, force-quit rehydration, and outbox replay without duplicate delivery.
  - **Acceptance:** uniqueness is proven at the database boundary, not only by UI button disabling.
  - **Verify:** run the focused shared JVM repository/workflow suite and capture RED evidence.
- [ ] 2.4 `@tests` Add failing iOS view-model/view contract tests for `reviewingResults`, native prompt, cancellation with zero dispatch, `confirming`, `failed`, `confirmed.pendingSync`, `confirmed.synced`, and post-commit-only navigation.
  - **Acceptance:** pending copy never claims remote success or participant delivery; accessibility labels identify confirm, cancel, progress, error, retry, and pending status.
  - **Verify:** run the focused `WakeveTests` poll-confirmation test selection on an available iOS simulator.
  - **Evidence:** still pending; the generalized architecture matcher and its self-test do not replace the required iOS render/dispatch contract tests.
- [x] 2.5 `@tests` Add a failing release/architecture guard that rejects `updateEventStatus` under `iosApp/src/Views` and direct confirmation repository mutation from SwiftUI views.
  - **Acceptance:** the guard reports the file and offending call and is wired into `scripts/test-critical-release-gates.sh`.
  - **Verify:** prove RED against the current direct view call, then run `bash -n` on every touched shell script.
  - **Evidence:** `@review` approved the generalized matcher and self-test; the self-test passes and the gate reports the two current real view-layer confirmation violations. Removing those production violations remains implementation work and is not represented as complete here.

## 3. Shared deterministic workflow

- [ ] 3.1 `@codegen` Add typed confirmation command/result/error contracts and inject a clock into confirmation state-machine and repository composition.
  - **Acceptance:** no fixed timestamp, lexicographic timestamp comparison, or direct production clock lookup remains in the confirmation path.
  - **Verify:** boundary tests pass for before, exactly at, and after the deadline.
- [ ] 3.2 `@codegen` Implement one application-scoped repository confirmation command and race-safe guards using the exact selected slot.
  - **Acceptance:** same decision is idempotent; a different confirmed slot returns `ALREADY_CONFIRMED_DIFFERENT_SLOT` without mutation.
  - **Verify:** focused shared state-machine and repository tests pass.
- [ ] 3.3 `@codegen` Add the additive SQLDelight migration for durable workflow outbox records, sync/idempotency receipts, and required uniqueness constraints.
  - **Acceptance:** event status, selected confirmed slot, exactly one `confirmation_effect_outbox` envelope, sync metadata, and committed receipt succeed or roll back together; no provider/calendar artifact is created locally.
  - **Verify:** fresh-database, migration, injected-failure, and restart tests pass.
- [ ] 3.4 `@codegen` Implement the local envelope publisher and backend ingestion boundary before enabling backend participant fan-out.
  - **Acceptance:** offline envelopes survive restart; backend acknowledgement is idempotent; `decisionSyncStatus` is independent from `effectDispatchStatus`; recipient, delivery, and calendar consumers remain disabled until their schema, reconciliation, and authority-cutover checkpoints pass.
  - **Verify:** offline replay, ingestion duplicate, shadow-write reconciliation, unique `delivery_authority`, bounded fan-out retry, and rollback checkpoint tests pass.
- [ ] 3.5 `@codegen` Align `EventManagementStateMachine` with the reviewed XState model, including typed failures, stable retry operation IDs, pending sync, rehydration, and receipt-gated navigation.
  - **Acceptance:** no implicit transition exists outside the model and no navigation is emitted after rollback/conflict.
  - **Verify:** all model conformance and shared workflow tests pass.

## 4. iOS modeled UX

- [ ] 4.1 `@codegen` Make the iOS poll-results view model/adapter consume the shared state machine from the composition root's single repository instance.
  - **Acceptance:** the view receives render state and typed callbacks only; it does not own a write-capable confirmation repository.
  - **Verify:** architecture tests prove one mutation path and one repository instance per application/database scope.
- [ ] 4.2 `@codegen` Refactor `PollResultsView` to render the modeled review, prompt, confirming, failed, confirmed-pending, and confirmed-synced states.
  - **Acceptance:** cancel performs no dispatch, double submit is coalesced, retry reuses the operation ID, and local pending copy is explicit.
  - **Verify:** focused Swift tests and light/dark previews pass.
- [ ] 4.3 `@codegen` Map success/error/pending feedback and navigation only from committed state-machine receipts.
  - **Acceptance:** success haptic/navigation occur once after local commit; failure and conflict never navigate.
  - **Verify:** view-model side-effect tests pass, including dismissal during `confirming` and cold rehydration.
- [ ] 4.4 `@review` Review the iOS flow for Product Excellence, Dynamic Type, VoiceOver, Reduce Motion, reduced transparency, clear pending-sync language, and native confirmation behavior.
  - **Acceptance:** the organizer, decision state, pending state, and next useful action are understandable without explanatory documentation.
  - **Verify:** findings are resolved or explicitly block completion.

## 5. Verification and release gates

- [ ] 5.1 Run XState model tests and prove model/implementation conformance.
- [ ] 5.2 Run focused and complete shared test suites, including SQLDelight migration and offline/restart tests.
- [ ] 5.3 Run focused iOS tests and an unsigned simulator build using the locally available destination (currently `platform=iOS Simulator,name=iPhone 17 Pro`).
- [ ] 5.4 Run `bash scripts/test-critical-release-gates.sh`, all touched script linters/`bash -n`, and confirm the view-layer `updateEventStatus` guard passes.
- [ ] 5.5 Manually validate online confirmation, after-deadline confirmation, exact-deadline vote closure, offline confirmation, force-quit rehydration, duplicate taps, retry, and pending-to-synced transition.
- [ ] 5.6 Run `openspec validate harden-poll-confirmation-workflow --strict` and `git diff --check`.
- [ ] 5.7 `@review` Perform final architecture and code review; mark tasks complete only when every required proof is attached.

## 6. Domain envelope and fan-out amendment

- [ ] 6.1 Update the XState model and RED tests so local commit creates exactly one `confirmation_effect_outbox` envelope, not notification/calendar provider artifacts.
- [ ] 6.2 Add SQLDelight tests proving `DatabaseEventRepository` ownership, unique `domainEventId`/`effectKey`, and rollback of the local decision envelope only.
- [ ] 6.3 Add backend ingestion tests proving idempotent acknowledgement without cross-database atomicity and without implying effect dispatch.
- [ ] 6.4 Add recipient and calendar fan-out tests using `recipientKey`, `deliveryKey`, and `calendarArtifactKey`, including zero-target pending resolution.
- [ ] 6.5 Prove `decisionSyncStatus` and `effectDispatchStatus` have separate retries and that effect failure cannot revert confirmation.
- [ ] 6.6 Sequence rollout after backend ingestion and unique `delivery_authority` shadow-write readiness; exercise every rollback checkpoint before enabling participant sends.
