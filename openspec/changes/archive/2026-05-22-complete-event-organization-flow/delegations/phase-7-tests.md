# Delegation: @tests Phase 7 Cross-Platform UX RED Tests

## Scope
Add RED tests only for Phase 7 task `7.2`, before `@codegen` starts `7.1`. Do not implement production UI behavior. Keep changes scoped to test files and OpenSpec evidence updates.

## Context
- Change: `complete-event-organization-flow`
- Phase: `7. Cross-Platform UX`
- Phase 6 is checked after shared Phase 6 tests pass.
- Source of truth:
  - `openspec/changes/complete-event-organization-flow/tasks.md`
  - `openspec/changes/complete-event-organization-flow/specs/cross-platform-organization-ux/spec.md`
  - `openspec/changes/complete-event-organization-flow/specs/event-organization/spec.md`
  - `openspec/changes/complete-event-organization-flow/specs/security-management/spec.md`
  - `openspec/changes/complete-event-organization-flow/specs/offline-sync/spec.md`
  - Existing UI contract tests under `composeApp/src/androidUnitTest/`, `composeApp/src/commonTest/`, and `iosApp/WakeveTests/`.

## Required RED Coverage
1. Android organization UX parity.
   - The event detail/organization dashboard must expose the same sections as the shared readiness model: participants, scenario, destination, lodging, transport, meetings, calendar, notifications, budget, payment pot, Tricount, sync, unsafe links, and access control.
   - `DRAFT`, `POLLING`, and unauthorized users must not expose confirmed-only organization details.
   - `FINALIZED` must render organization sections read-only while preserving useful details.
2. iOS organization UX parity.
   - SwiftUI routes and views must expose the same readiness sections and access states as Android.
   - Confirmed participants must see local details; declined, pending, and non-participants must see access-denied or confirmation-required states.
   - `FINALIZED` must disable or hide organization mutation actions.
3. Offline and pending sync UX.
   - Android and iOS must show offline and pending-sync state for critical local-first organization sections.
   - UI copy/state must not imply pending local writes are server-confirmed.
   - Failed retryable sync/conflict state must be visible or actionable enough for the organizer to resolve before finalization.
4. Cross-platform consistency.
   - Android and iOS must use equivalent localization keys or stable labels for readiness sections and state badges.
   - Empty, optional-not-needed, incomplete, complete, pending-sync, failed-sync, and access-denied states must be distinguishable.

## Suggested Test Locations
- Android/source-contract tests:
  - `composeApp/src/androidUnitTest/kotlin/com/guyghost/wakeve/navigation/Phase7OrganizationUxContractTest.kt`
  - or extend nearby existing Phase 5/transport contract tests if that keeps scope smaller.
- Shared/source-contract tests:
  - `composeApp/src/commonTest/kotlin/com/guyghost/wakeve/ui/organization/Phase7OrganizationUxParityTest.kt`
- iOS XCTest source-contract tests:
  - `iosApp/WakeveTests/OrganizationPhase7ContractTests.swift`

## Verification Commands
Run the narrowest commands that cover the tests you add, using no daemon/config cache where Gradle is involved:

```bash
./gradlew :composeApp:testDebugUnitTest --tests '*Phase7*' --no-daemon --no-configuration-cache --rerun-tasks --max-workers=1
./gradlew :composeApp:allTests --tests '*Phase7*' --no-daemon --no-configuration-cache --rerun-tasks --max-workers=1
xcodebuild test -project iosApp/iosApp.xcodeproj -scheme WakeveApp -destination 'platform=iOS Simulator,name=iPhone 17,OS=26.5' -only-testing:WakeveTests/OrganizationPhase7ContractTests -derivedDataPath /tmp/wakeve-ios-dd-phase7-ui-contract CODE_SIGNING_ALLOWED=NO
git diff --check
openspec validate complete-event-organization-flow --strict
```

If a broad command is unavailable for this project layout, record the exact failure and rerun the closest targeted command that proves the RED tests compile and fail for missing UX behavior.

## Expected RED Failures
- Missing unified Android/iOS organization dashboard readiness surface across all Phase 6 readiness sections.
- Missing visible distinction between optional-not-needed, pending-sync, failed-sync/conflict, incomplete, complete, and access-denied states.
- Missing finalized/read-only UI gating for organization mutations.
- Missing or inconsistent labels/localization keys between Android and iOS.

## Return Format
- Test files added or modified.
- Commands run and exact RED failures.
- Confirmation that no production behavior was implemented.
- Notes for `@codegen` on the smallest UI contracts needed to make the tests green.
