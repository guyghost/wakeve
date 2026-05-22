# Phase 4 Transport Remediation

## Status
Phase 4 Transport remediation is complete. The final read-only review found no remaining P1/P2 blockers and explicitly approved checking Phase 4. Phase 5 may proceed through the planned `@tests` first workflow.

## Final Evidence
- Checked on 2026-05-22 by the orchestrator and final read-only review.
- `openspec validate complete-event-organization-flow --strict` passes.
- `TransportRepository.generatePlan` requires a non-null `generatedByUserId` and unconditionally checks organizer authority before persistence/sync.
- `requireCanSaveDepartureLocation` validates the target participant is confirmed before allowing organizer writes.
- Android transport direct entry scopes transient event, participant, and scenario state to the route `eventId`; repository-backed state is used otherwise.
- Phase 4 tasks are checked in `tasks.md`; Phase 5 remains unchecked and may now begin.

## Blocking Findings

### P1: Shared plan generation still allows missing actor context
- File: `shared/src/commonMain/kotlin/com/guyghost/wakeve/transport/TransportRepository.kt`
- Current risk: `generatePlan` accepts `generatedByUserId: String? = null`, so a local/offline caller can persist and queue a transport plan without proving organizer authority.
- Acceptance: shared `generatePlan` requires an explicit organizer actor for every local write path; rejected calls create no `transport_plan`, no routes, and no pending sync metadata.

### P1: Android direct transport entry still depends on transient event state
- File: `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/WakeveNavHost.kt`
- Current risk: destination is repository-backed, but event status, organizer identity, participant access, and confirmed date still come from `EventManagementViewModel.state.selectedEvent`. Direct route entry or process restart can incorrectly fall back to `DRAFT` and disable/deny valid local transport planning.
- Acceptance: Android transport entry loads event and participant access state from local repositories when the view-model state is empty, while preserving existing in-memory state when available.

### P2: Shared organizer departure writes do not match backend access rules
- File: `shared/src/commonMain/kotlin/com/guyghost/wakeve/transport/TransportRepository.kt`
- Current risk: organizers can write departure locations for unknown or unconfirmed participant IDs locally, queuing operations the backend rejects.
- Acceptance: organizer departure writes are allowed only for confirmed participants on the event; rejected writes persist nothing and queue no pending sync metadata.

## Required Delegation Order
1. `@tests` adds RED regression coverage for all three findings and does not modify production code.
2. `@codegen` fixes production only after RED tests exist and does not modify tests.
3. `@review` performs a read-only review and explicitly states whether Phase 4 can be checked.

## Delegation Packets
- `delegations/phase-4-tests.md`
- `delegations/phase-4-codegen.md`
- `delegations/phase-4-review.md`

## Execution Checklist
- [x] Dispatch `@tests` with the prompt below.
- [x] Confirm `@tests` modified only test files.
- [x] Confirm regression evidence covers all three blockers:
  - missing organizer actor for shared plan generation,
  - Android direct-entry event/access fallback from repositories,
  - organizer departure writes for unknown or unconfirmed participants.
- [x] Mark task `4.5` complete only after the RED test evidence is recorded.
- [x] Dispatch `@codegen` with the prompt below after `4.5` is satisfied.
- [x] Confirm `@codegen` modified production files only and did not weaken tests.
- [x] Run the verification gate below and record command results.
- [x] Mark task `4.6` complete only after targeted and broad verification commands pass.
- [x] Dispatch `@review` for a read-only Phase 4 review.
- [x] Add RED coverage for route-scoped Android transport state after review finding P1.
- [x] Dispatch `@codegen` route-scoping fix only after the RED test exists.
- [x] Dispatch another read-only `@review` after route-scoping verification passes.
- [x] Mark final Phase 4 review task complete only if review explicitly says Phase 4 can be checked.
- [x] Do not start Phase 5 while any Phase 4 remediation checklist item remains unchecked.

## Dispatch Log
- 2026-05-22: Dispatched `@tests` remediation worker `019e4e98-04d3-7640-b029-2807b4e5f376` (Heisenberg) for task `4.5`.
- 2026-05-22: `@tests` added Phase 4 regression tests only in test files, but Gradle stopped before test execution at `:shared:generateCommonMainWakeveDbInterface` because `TricountHandoff.sq` and `OrganizationReadinessDecision.sq` contain SQLDelight parse errors around `ON CONFLICT`. Task `4.5` remains unchecked until the tests execute and produce RED evidence.
- 2026-05-22: Dispatched narrow build-unblocker `@codegen` worker `019e4e9c-b4d2-7072-8ef0-7d8e3b192535` (Plato) limited to the two SQLDelight files listed below.
- 2026-05-22: Re-ran Phase 4 tests after SQLDelight unblocker. Shared RED currently proves non-organizer generation rejection; Android RED proves direct transport route DRAFT downgrade. Follow-up sent to `@tests` worker `019e4e98-04d3-7640-b029-2807b4e5f376` because the nullable actor source contract and organizer departure bypass tests still passed against known-bad source.
- 2026-05-22: `@tests` follow-up hardened shared regression tests. Parent verification results:
  - `./gradlew :shared:jvmTest --tests com.guyghost.wakeve.transport.TransportOfflineRepositoryPhase4Test --no-daemon` fails with `organizer departure guard must not bypass target participant confirmation` and `local plan generation by non organizer persists no plan routes or pending sync`.
  - `./gradlew :composeApp:testDebugUnitTest --tests com.guyghost.wakeve.navigation.TransportNavigationContractTest --no-daemon --no-configuration-cache` fails with `navHostDoesNotDowngradeDirectTransportEntryToDraftBeforeRepositoryEventLoads`.
  - `generate plan requires an explicit organizer actor before local persistence and sync` passes against current source because `TransportRepository.generatePlan` now has a non-null `generatedByUserId` and unconditional `requireOrganizer`; keep this coverage during `4.6`.
  - Corrected `tasks.md` so `4.6` and `4.7` remain unchecked until codegen and review actually complete.
- 2026-05-22: Dispatched `@codegen` remediation worker `019e4ea2-d0dd-7aa0-97ae-cd03ad1dec9b` (Mill) for task `4.6`, scoped to production fixes only and no Phase 5 work.
- 2026-05-22: Parent verification for `4.6` passed:
  - `./gradlew :shared:jvmTest --tests com.guyghost.wakeve.transport.TransportOfflineRepositoryPhase4Test --no-daemon`
  - `./gradlew :composeApp:testDebugUnitTest --tests com.guyghost.wakeve.navigation.TransportNavigationContractTest --no-daemon --no-configuration-cache`
  - `./gradlew :server:test --tests com.guyghost.wakeve.routes.EventOrganizationPhase4TransportRoutesTest --no-daemon`
  - `./gradlew :composeApp:compileDebugKotlinAndroid --no-daemon --no-configuration-cache`
  - `./gradlew :shared:jvmTest --no-daemon --no-parallel` passed on rerun after transient XML result-write failures.
  - `openspec validate complete-event-organization-flow --strict`
  Task `4.6` is complete; proceed to read-only `@review` for `4.7`.
- 2026-05-22: Dispatched read-only `@review` worker `019e4eab-5b8c-7441-b004-a41cfe0bd2f8` (Kant) for task `4.7`.
- 2026-05-22: `@review` blocked Phase 4 with P1: Android transport direct entry is not safely route-scoped because transient `eventState.selectedEvent`, `participantAccessStates`, and `scenarioState` can belong to a different event than the route `eventId`. Added tasks `4.7` tests, `4.8` codegen, and `4.9` review. P2 noted Phase 5-looking files exist, but the reviewer could not prove they were introduced by this remediation.
- 2026-05-22: Dispatched `@tests` route-scoping worker `019e4eae-06e2-7703-9c62-3e605e3ad27b` (Godel) for task `4.7`.
- 2026-05-22: `@tests` route-scoping worker added RED tests in `TransportNavigationContractTest.kt`. Delegate verification `./gradlew :composeApp:testDebugUnitTest --tests com.guyghost.wakeve.navigation.TransportNavigationContractTest --no-daemon --no-configuration-cache` failed with `13 tests completed, 3 failed`: `navHostUsesTransientSelectedEventOnlyWhenItMatchesTransportRouteEventId`, `navHostUsesTransientParticipantAccessOnlyForRouteScopedInMemoryEvent`, and `navHostFiltersTransientScenariosByTransportRouteEventIdBeforeSelectedDestination`. Task `4.7` is complete. Corrected `tasks.md` so `4.8` and `4.9` remain unchecked until codegen and review actually complete.
- 2026-05-22: Dispatched `@codegen` route-scoping worker `019e4eb0-f53d-7753-8a23-7e4f567ea3d6` (Zeno) for task `4.8`, scoped to `WakeveNavHost.kt` production fix only.
- 2026-05-22: Parent verification for `4.8` passed:
  - `./gradlew :composeApp:testDebugUnitTest --tests com.guyghost.wakeve.navigation.TransportNavigationContractTest --no-daemon --no-configuration-cache`
  - `./gradlew :composeApp:compileDebugKotlinAndroid --no-daemon --no-configuration-cache`
  - `openspec validate complete-event-organization-flow --strict`
  Task `4.8` is complete; proceed to final read-only review for `4.9`.
- 2026-05-22: Dispatched final read-only `@review` worker `019e4eb5-8d90-7933-b003-852d9a1cbc64` (Leibniz) for task `4.9`.
- 2026-05-22: Final `@review` found no P1/P2 blockers and explicitly stated: "Phase 4 can be checked." Task `4.9` is complete. Phase 5 may now start through the planned `@tests` first workflow.

## Build Unblocker
Before `4.5` can be completed, delegate a narrow `@codegen` build-unblocker to fix only SQLDelight syntax in:
- `shared/src/commonMain/sqldelight/com/guyghost/wakeve/TricountHandoff.sq`
- `shared/src/commonMain/sqldelight/com/guyghost/wakeve/OrganizationReadinessDecision.sq`

This unblocker MUST NOT implement Phase 5 behavior or alter Phase 4 production logic. It only restores DB interface generation so Phase 4 tests can run.

## @tests Prompt
```text
@tests - Phase 4 Transport remediation RED tests uniquement.

Contexte: OpenSpec complete-event-organization-flow, Phase 4 Transport. Ne modifie pas la production.

Ajoute des tests de régression pour les blockers restants:
1. Shared TransportRepository.generatePlan doit refuser tout appel local/offline sans acteur organisateur explicite et ne doit pas créer de plan ni queue sync.
2. Android direct route event/{eventId}/transport doit charger event status, organizer, participant access, confirmed date depuis les repositories locaux quand EventManagementViewModel.selectedEvent est vide.
3. Shared organizer departure writes doivent refuser participant inconnu/non confirmé et ne queue aucune sync, aligné avec backend.

Retour attendu: fichiers tests modifiés, commandes exécutées, sorties RED.
```

## @codegen Prompt
```text
@codegen - Corrige uniquement les blockers Phase 4 Transport couverts par les nouveaux tests RED.

Contraintes:
- Ne modifie pas les tests.
- Pas de contournement du workflow DRAFT -> POLLING -> CONFIRMED -> COMPARING -> ORGANIZING -> FINALIZED.
- generatePlan doit exiger un organizer actor explicite dans le shared repository.
- Android transport direct entry doit être repository-backed pour event/access/date, pas seulement ViewModel state.
- Organizer departure local writes doivent être refusées pour participant inconnu/non confirmé avant persistence/sync.

Retour attendu: fichiers modifiés et commandes vertes ciblées.
```

## Verification Gate
Run these after `@codegen` reports completion:
```bash
./gradlew :shared:jvmTest --tests com.guyghost.wakeve.transport.TransportOfflineRepositoryPhase4Test --no-daemon
./gradlew :composeApp:testDebugUnitTest --tests com.guyghost.wakeve.navigation.TransportNavigationContractTest --no-daemon --no-configuration-cache
./gradlew :server:test --tests com.guyghost.wakeve.routes.EventOrganizationPhase4TransportRoutesTest --no-daemon
./gradlew :shared:jvmTest --no-daemon
./gradlew :composeApp:compileDebugKotlinAndroid --no-daemon --no-configuration-cache
openspec validate complete-event-organization-flow --strict
```
