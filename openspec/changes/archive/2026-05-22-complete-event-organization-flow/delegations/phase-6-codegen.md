# Delegation: @codegen Phase 6 Finalization and Offline Sync Integrity

## Scope
Implement only production behavior needed for Phase 6 task `6.2`. Do not modify tests. Keep the workflow `DRAFT -> POLLING -> CONFIRMED -> COMPARING -> ORGANIZING -> FINALIZED` intact.

## Context
- Change: `complete-event-organization-flow`
- Phase: `6. Finalization and Offline Sync Integrity`
- Completed RED tests: `6.1`, `6.3`, `6.4`
- Open task: `6.2`
- Source of truth:
  - `openspec/changes/complete-event-organization-flow/design.md`
  - `openspec/changes/complete-event-organization-flow/specs/event-organization/spec.md`
  - `openspec/changes/complete-event-organization-flow/specs/workflow-coordination/spec.md`
  - `openspec/changes/complete-event-organization-flow/specs/offline-sync/spec.md`
  - `openspec/changes/complete-event-organization-flow/tasks.md`

## Required Fixes
1. Compute full finalization readiness before allowing `ORGANIZING -> FINALIZED`.
   - Readiness must cover participants/attendance, selected scenario, destination, lodging, transport, meetings, calendar, notifications, budget, payment, Tricount, sync, unsafe links, and access control.
   - Optional sections may pass only when an explicit not-needed decision exists.
   - Missing required sections must produce stable blocker codes that can be shown in readiness summaries.
2. Block finalization while critical local-first writes are pending, failed, or conflicted.
   - Pending `syncMetadata` for critical organization entities must block.
   - Failed retryable critical sync metadata must block until retried/resolved.
   - Pending critical `conflict_log` entries must block.
   - Finalization may complete locally only after critical sync convergence.
3. Route finalization through the readiness gate.
   - `DatabaseEventRepository.updateEventStatus(... FINALIZED ...)` must reject invalid finalization and leave the event in `ORGANIZING`.
   - `EventManagementStateMachine.MarkAsFinalized` must surface repository/readiness blocker errors instead of silently finalizing.
   - Finalization remains organizer-only and allowed only from `ORGANIZING`.
4. Enforce read-only organization sections after `FINALIZED`.
   - Scenario/destination/lodging decisions must reject post-finalization mutation.
   - Transport mutations must reject post-finalization mutation.
   - Meeting creation already rejects `FINALIZED`; keep that behavior passing.
5. Preserve local-first behavior for earlier phases.
   - Do not remove existing sync queue creation for scenario, lodging, transport, meeting, budget, payment, or Tricount writes.
   - Do not weaken confirmed-participant/organizer access rules.

## RED Tests to Make Green
```bash
./gradlew :shared:jvmTest --tests 'com.guyghost.wakeve.organization.EventOrganizationPhase6FinalizationReadinessTest' --no-daemon --no-configuration-cache --rerun-tasks --max-workers=1 -Dkotlin.compiler.execution.strategy=in-process
./gradlew :shared:jvmTest --tests 'com.guyghost.wakeve.workflow.EventOrganizationPhase6EndToEndSyncTest' --no-daemon --no-configuration-cache --rerun-tasks --max-workers=1 -Dkotlin.compiler.execution.strategy=in-process
```

Current RED failures:
- `EventOrganizationReadinessRepository.getReadiness` exposes only meetings/budget/payment, not every critical finalization section.
- `DatabaseEventRepository.updateEventStatus(... FINALIZED ...)` succeeds while critical readiness/sync blockers remain.
- The complete shared workflow finalizes before offline sync convergence.
- Failed retryable critical sync and pending critical conflicts do not block finalization.
- `ScenarioRepository.selectFinalScenario` still mutates scenario/destination/lodging state after `FINALIZED`.

## Expected Production Areas
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/organization/EventOrganizationReadinessRepository.kt`
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/repository/DatabaseEventRepository.kt`
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/repository/ScenarioRepository.kt`
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/accommodation/AccommodationRepository.kt`
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/transport/TransportRepository.kt`
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/presentation/statemachine/EventManagementStateMachine.kt`
- Additional production files only if needed to keep readiness cohesive and reusable.

## Verification Commands
```bash
./gradlew :shared:jvmTest --tests 'com.guyghost.wakeve.organization.EventOrganizationPhase6FinalizationReadinessTest' --no-daemon --no-configuration-cache --rerun-tasks --max-workers=1 -Dkotlin.compiler.execution.strategy=in-process
./gradlew :shared:jvmTest --tests 'com.guyghost.wakeve.workflow.EventOrganizationPhase6EndToEndSyncTest' --no-daemon --no-configuration-cache --rerun-tasks --max-workers=1 -Dkotlin.compiler.execution.strategy=in-process
./gradlew :shared:jvmTest --tests '*Phase6*' --no-daemon --no-configuration-cache --rerun-tasks --max-workers=1 -Dkotlin.compiler.execution.strategy=in-process
git diff --check
openspec validate complete-event-organization-flow --strict
```

## Return Format
- Production files modified.
- Verification commands and results.
- Confirm Phase 6 tests were not modified.
- Confirm Phase 7 was not started.
