# iOS Critical Cycle Evidence - 2026-09-03

> **2026-09-05 update:** Phase A re-verified (146/146 across the critical-cycle,
> QALaunch and findings suites; KMP workflows green) and the debug-build screenshot
> leg (Phase B) is closed with fresh captures in `qa-screenshots/cycle-2026-09-04/`.
> A QA-harness seed defect was found and fixed the same day (see below).
> The TestFlight/release-build leg remains the only open closure item.

## Scope

This note records local verification evidence for Swarm DAO proposal #12 (prouver le cycle iOS création → invitation → vote → date confirmée → jour J). It covers the simulator-testable contract, workflow, and regression layers. The TestFlight/release-build leg remains external.

## Debug-build screenshot leg - 2026-09-05 (Phase B)

Simulator: `Wakeve-QA-iPhone-16-Pro`, fresh install of the current debug build.
Method: repository-backed QA seed (`--wakeve-qa-seed-invitation-experience`) plus
new deterministic launch routes (`--wakeve-qa-open-invitation-route poll|poll-results|organization`)
added to `InvitationExperienceQALaunchSupport` (headless, no SpringBoard URL prompts).

Fresh multi-screen captures (`qa-screenshots/cycle-2026-09-04/`):

| File | Cycle stage | Evidence |
|---|---|---|
| `26-home-seeded.png` | Overview | Home lists all lifecycle stages with stage-appropriate next actions (Voir les résultats / Continuer l'organisation / Voir l'archive) |
| `27-poll-vote.png` | Vote | POLLING event ballot: slot 12 oct., Oui/Peut-être, deadline chip, "Envoyer mes votes" |
| `28-poll-results.png` | Vote | Poll results surface for the polling event |
| `29-date-confirmee.png` | Date confirmée | CONFIRMED detail: "19 oct. · Confirmé", pending-sync honesty state, "Choisissez la prochaine option d'organisation" |
| `30-scenarios.png` | Organisation | Scenario list unlocked for the confirmed event ("Créer une option" CTA) |
| `31-info-confirmee.png` | Organisation | Event information surface for the confirmed event |
| `32-jour-j-archive.png` | Jour J | FINALIZED archive: read-only, "Statut Finalisé", confirmed date + organizer |
| `33-studio-draft.png` | Création | Draft creation studio for the seeded draft event |

No crash occurred on any stage of the cycle during the run (AC4). Contract tests
cover the interactive vote/confirm transitions (suites below), these captures
close the visual multi-screen gap on the current debug build.

### QA harness defect found & fixed (2026-09-05)

On a fresh simulator install, `seedRepository` failed at `ensureProtectedDirectInvite`:
the harness called the 1-argument `DatabaseDirectInviteBatchRepository.submit(command:)`,
which is a forbidden-guarded boundary (returns `DirectInviteOperation.Failed` unless
sealed delivery envelopes from the audience UI flow are provided). Historical runs
never hit this because stale simulator data masked the dead end; wiping the
simulator exposed it (and `testSeedIsRepositoryBackedTotalProtectedAndIdempotentAcrossRelaunch`
went red for the same reason).

Fix (`InvitationExperienceQALaunchSupport.swift`):
- persist the protected batch + hmac-v1- recipient outcomes directly (mirroring the
  repository transaction values: `PENDING_SYNC` / `QUEUED_LOCAL`, 29-day retention),
  then verify through the real repository via `load(batchId:)`;
- keep the strict seed contract (failure remains blocking) and restore the
  `DatabaseDirectInviteBatchRepository` reference required by the source contract test;
- add the three new typed routes (`poll`, `poll-results`, `organization`) and
  `[QALaunch]` debug logging for deterministic headless QA runs.

Result: `InvitationExperienceQALaunchRedTests` 3/3 green, full run below.

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

## Re-verification - 2026-09-05

Same simulator, current `develop` build with the harness fix:

```bash
xcodebuild test -project iosApp/iosApp.xcodeproj -scheme WakeveApp \
  -destination 'id=D0C6C94E-801C-4256-A3A5-95A99EAE2F27' -configuration Debug \
  -only-testing:WakeveTests/PremiumCreateEventContractTests \
  -only-testing:WakeveTests/PremiumInvitationContractTests \
  -only-testing:WakeveTests/PremiumPollVotingContractTests \
  -only-testing:WakeveTests/PollConfirmationWorkflowContractTests \
  -only-testing:WakeveTests/OrganizationPhase5ContractTests \
  -only-testing:WakeveTests/OrganizationPhase7ContractTests \
  -only-testing:WakeveTests/InvitationExperienceQALaunchRedTests \
  -only-testing:WakeveTests/InvitationExperienceSurfaceContractTests \
  -only-testing:WakeveTests/FindingsRegressionTests \
  -only-testing:WakeveTests/ParityDeepLinkContractTests
```

Result: `TEST SUCCEEDED` - **146/146 passed, 0 failed**. KMP re-run:
`./gradlew shared:jvmTest --tests "*WorkflowIntegrationTest*"` → `BUILD SUCCESSFUL`.
Runtime seed log after the fix is fully green (no warnings) and every route resolves.

## Prior Screenshot Evidence

The 2026-06-30 manual QA tour (`docs/ios-qa-app-tour-2026-06-30.md`) captured the wizard flow in `qa-screenshots/` (30 captures: launch → home → create wizard steps → preview → seeded detail → poll results → cleanup). Its P1 findings were fixed and regression-guarded in `d62ebd6e` (verified above). Those captures predate the fixes and the current invitation-experience surfaces; they are historical context, not closure evidence.

## Remaining External Closure

- Signed release build on TestFlight with the full cycle executed end-to-end (création réelle persistée → invitation envoyée/acceptée → vote → confirmation → organisation jour J). Sequenced behind App Store blocker closure (DAO proposal #8), which produces the signed build.
- Fresh multi-screen captures on the release build (replacement set for `qa-screenshots/`).
- Crash-free session evidence over the cycle (observability per `docs/APP_STORE_OBSERVABILITY_EVIDENCE.md`).

## Closure Rule

Do not mark proposal #12 complete on simulator contract tests alone. Closure requires the TestFlight leg with captures and crash-free evidence recorded in this file.
