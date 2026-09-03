# iOS Critical Cycle Evidence - 2026-09-03

## Scope

This note records local verification evidence for Swarm DAO proposal #12 (prouver le cycle iOS création → invitation → vote → date confirmée → jour J). It covers the simulator-testable contract, workflow, and regression layers. The TestFlight/release-build leg remains external.

## Local Verification - 2026-09-03

Simulator: `Wakeve-QA-iPhone-16-Pro` (id `D0C6C94E-801C-4256-A3A5-95A99EAE2F27`), Xcode 27.0.

### iOS critical-cycle contract suite

```bash
xcodebuild test -project iosApp/iosApp.xcodeproj -scheme WakeveApp \
  -destination 'platform=iOS Simulator,id=D0C6C94E-801C-4256-A3A5-95A99EAE2F27' \
  -only-testing:WakeveTests/PremiumCreateEventContractTests \
  -only-testing:WakeveTests/PremiumInvitationContractTests \
  -only-testing:WakeveTests/PremiumPollVotingContractTests \
  -only-testing:WakeveTests/PollConfirmationWorkflowContractTests \
  -only-testing:WakeveTests/OrganizationPhase5ContractTests \
  -only-testing:WakeveTests/OrganizationPhase7ContractTests
```

Result: `TEST SUCCEEDED` — 86/86 passed, 0 failed. Coverage by cycle stage:

- Création: `PremiumCreateEventContractTests` (wizard, slots, preview, persistence receipt)
- Invitation: `PremiumInvitationContractTests` (share sheet, deep links, guest path)
- Vote: `PremiumPollVotingContractTests` (ballot submission machine, offline queue)
- Date confirmée: `PollConfirmationWorkflowContractTests` (confirmation guards, status transitions)
- Jour J: `OrganizationPhase5ContractTests`, `OrganizationPhase7ContractTests` (organization unlock, access control)

### QA findings regression suite

```bash
xcodebuild test ... -only-testing:WakeveTests/FindingsRegressionTests
```

Result: `TEST SUCCEEDED` — 24/24 passed, 0 failed. This suite guards the fixes for the 2026-06-30 QA tour findings (commit `d62ebd6e`), including:

- P1 création non persistée: `testCreateEventCompletionIsEmittedOnlyAfterTheStateMachinePersists`, `testCreateEventTurnsSelectedDateIntoProposedSlot`
- Dispatch n'est pas un reçu de persistance; la dismissal ne survient que sur completion persistée

### KMP shared workflow tests

```bash
./gradlew shared:jvmTest --tests "com.guyghost.wakeve.workflow.*" --tests "com.guyghost.wakeve.presentation.statemachine.*"
```

Result: `BUILD SUCCESSFUL`. Includes `CompleteWorkflowE2ETest` (5 tests, DRAFT → FINALIZED), `DraftWorkflowIntegrationTest` (8 tests), and the state-machine suites.

## Prior Screenshot Evidence

The 2026-06-30 manual QA tour (`docs/ios-qa-app-tour-2026-06-30.md`) captured the wizard flow in `qa-screenshots/` (30 captures: launch → home → create wizard steps → preview → seeded detail → poll results → cleanup). Its P1 findings were fixed and regression-guarded in `d62ebd6e` (verified above). Those captures predate the fixes and the current invitation-experience surfaces; they are historical context, not closure evidence.

## Remaining External Closure

- Signed release build on TestFlight with the full cycle executed end-to-end (création réelle persistée → invitation envoyée/acceptée → vote → confirmation → organisation jour J).
- Fresh multi-screen captures on the release build (replacement set for `qa-screenshots/`).
- Crash-free session evidence over the cycle (observability per `docs/APP_STORE_OBSERVABILITY_EVIDENCE.md`).

## Closure Rule

Do not mark proposal #12 complete on simulator contract tests alone. Closure requires the TestFlight leg with captures and crash-free evidence recorded in this file.
