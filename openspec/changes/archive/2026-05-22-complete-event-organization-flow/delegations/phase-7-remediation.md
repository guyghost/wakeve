# Delegation: Phase 7.4 UX Remediation

## Scope
Remediate the final Phase 7.4 read-only review blockers before checking the cross-platform UX phase.

## Context
- Change: `complete-event-organization-flow`
- Phase: `7. Cross-Platform UX`
- Blocking task: `7.4`
- Remediation tasks: `7.7` through `7.21`

## Blocking Findings

### P1: Android finalized events still expose organization mutations
- Route setup passes organizer state into meeting, payment, and Tricount screens without workflow/read-only state.
- Finalized organizers can still see or invoke organization mutations.
- Acceptance: Android finalized organization sections remain visible where appropriate but all meeting, payment, and Tricount mutations are hidden or disabled.

### P1: iOS payment and Tricount finalized mutation gap
- `ContentView` gates access but does not pass finalized/read-only state into payment and Tricount surfaces.
- Finalized organizers can still create/activate/close payment pots and link/unlink/mark Tricount not needed.
- Acceptance: iOS payment and Tricount views receive or derive read-only state and disable/hide mutations in `FINALIZED`.

### P2: iOS finalized transport is hidden instead of read-only
- `canAccessTransportPlanning` excludes finalized events even though transport details should remain consultable.
- Acceptance: finalized events keep transport reachable for organizer/confirmed participants with read-only controls.

### P2: Android meeting visible copy remains English
- Visible strings include `Link:`, `Edit`, `Share Link`, `No meetings yet`, `Create Meeting`, `Edit Meeting`, `Duration (hours)`, `Generate Meeting Link`, and `Cancel`.
- Acceptance: visible Android meeting copy is product-ready French copy while non-displayed test anchors may remain.

## Required Delegation Order
1. `@tests` adds RED regression coverage only.
2. `@codegen` fixes production after RED evidence exists and does not weaken tests.
3. `@review` performs a read-only re-review and explicitly states whether `7.4` can be checked.

## Remaining Phase 7.12 P1

### P1: iOS meeting detail and shared cancellation actor gating
- `MeetingDetailView` still exposes meeting-detail mutation actions without enough organizer/current-user/read-only state.
- `MeetingListViewModel.cancelMeeting` must not accept any non-empty actor id as sufficient authorization.
- Shared cancellation must compare the current actor with the real meeting organizer and reject participants, non-organizers, and read-only/finalized flows.
- Acceptance: `@tests` adds RED coverage for 7.13, `@codegen` fixes only after RED evidence, and `@review` explicitly approves 7.15 before `7.4` and `7.12` are checked.

## Remaining Phase 7.15 blockers

### P1: iOS real meeting detail uses a DEBUG-only preview initializer
- `MeetingListView` currently navigates to `MeetingDetailView(... previewMeeting:)` whenever the meeting exists in the list.
- That initializer is compiled only under `#if DEBUG`, so non-DEBUG iOS builds are at risk, and DEBUG builds treat real meetings as previews.
- Acceptance: real meeting navigation uses a production-safe `MeetingDetailView` initializer, and preview initializers stay limited to explicit preview/test paths.

### P2: iOS meeting detail loading is not event-scoped
- `MeetingDetailViewModel` still dispatches `LoadMeetings(eventId: "")`.
- Direct detail navigation can fail to load the real event meetings and show `Réunion introuvable`.
- Acceptance: the real `MeetingDetailView` initializer passes `eventId` into `MeetingDetailViewModel`, and `MeetingDetailViewModel.loadMeetings()` dispatches `LoadMeetings(eventId: eventId)`.

## Remaining Phase 7.18 P1

### P1: Android meeting visible dynamic labels are not localized
- `MeetingListScreen` still renders enum-backed values such as `meeting.platform.name` and `platform.name`.
- Date rendering uses `localDateTime.month.name.take(3)`, which yields English enum fragments.
- Acceptance: Android meeting visible copy uses product-ready French labels for meeting platforms and date/month display; source-contract tests must prove visible UI does not render enum `.name` values.

## Verification Gate
Run after `@codegen` reports completion:

```bash
./gradlew :composeApp:testDebugUnitTest --tests '*Phase7*' --no-daemon --no-configuration-cache --no-build-cache --rerun-tasks --max-workers=1 -Dkotlin.compiler.execution.strategy=in-process -Dkotlin.incremental=false
./gradlew :shared:jvmTest --tests 'com.guyghost.wakeve.presentation.usecase.CancelMeetingUseCaseTest' --no-daemon --no-configuration-cache --rerun-tasks --max-workers=1 -Dkotlin.compiler.execution.strategy=in-process -Dkotlin.incremental=false
xcodebuild test -project iosApp/iosApp.xcodeproj -scheme WakeveApp -destination 'platform=iOS Simulator,name=iPhone 17,OS=26.5' -only-testing:WakeveTests/OrganizationPhase7ContractTests -derivedDataPath /tmp/wakeve-ios-dd-phase7-remediation CODE_SIGNING_ALLOWED=NO
git diff --check
openspec validate complete-event-organization-flow --strict
```
