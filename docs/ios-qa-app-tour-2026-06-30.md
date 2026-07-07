# iOS QA App Tour - 2026-06-30

## Scope

Manual QA was run on the iOS simulator using the `WakeveApp` scheme.

- Simulator: iPhone 17 Pro, iOS 27.0
- Bundle id: `com.guyghost.wakeve`
- Debug auth: `WAKEVE_DEBUG_AUTHENTICATED=1`
- AI availability override: `WAKEVE_AI_AVAILABILITY_OVERRIDE=unsupported`

## Result

The app launched successfully and the authenticated Home screen rendered.
The event creation wizard opened and could be advanced through name, date,
place, invite, confirmation, and preview screens.

Creating an event from the preview did not leave a persisted event in the
simulator SQLite database. To continue the visualization pass, a QA-only event
was inserted directly in the simulator database, then removed after the tour.

## Findings

### P1 - Event creation does not persist from the preview flow

The creation wizard reaches the preview screen, but after tapping the final
create action the event was not present in the local database on relaunch.

Relevant code:

- `iosApp/src/Views/Events/CreateEventSheet.swift`
- `iosApp/src/ViewModels/CreateEventViewModel.swift`
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/presentation/usecase/CreateEventUseCase.kt`

The shared create use case requires at least one proposed slot. The iOS wizard
allows the user to proceed with "date to decide", so creation can fail in the
state machine while the UI still dismisses.

### P1 - Event deletion is not exposed in the current iOS detail UI

`EventDetailViewModel` exposes `deleteEvent()`, but the active `EventDetailView`
menu does not include a delete action. The manual QA could not delete an event
through the UI.

Relevant code:

- `iosApp/src/ViewModels/EventDetailViewModel.swift`
- `iosApp/src/Views/App/ContentView.swift`

### P2 - Repository uses a fixed current date

`DatabaseEventRepository.getCurrentUtcIsoString()` returns
`2025-11-12T10:00:00Z`, which can skew deadline and status calculations during
current-date QA.

Relevant code:

- `shared/src/commonMain/kotlin/com/guyghost/wakeve/repository/DatabaseEventRepository.kt`

### P2 - UI test files are present but not wired as a runnable UI test target

Files such as `iosApp/iosAppUITests/DeleteEventUITests.swift` exist, but the
Xcode project/test plan does not appear to include a UI test target for them.

## Screenshot Evidence

- Launch/onboarding: `../qa-screenshots/01-launch-simctl.png`
- Authenticated empty Home: `../qa-screenshots/02-home-authenticated.png`
- Create wizard opened: `../qa-screenshots/03-after-create-tap.png`
- Date step: `../qa-screenshots/08-date-step.png`
- Place step: `../qa-screenshots/09-place-step.png`
- Invite step: `../qa-screenshots/10-invite-step.png`
- Confirmation step: `../qa-screenshots/11-confirm-step.png`
- Preview: `../qa-screenshots/12-preview.png`
- Home with QA event: `../qa-screenshots/15-seeded-home.png`
- Event detail: `../qa-screenshots/21-detail-after-active-card-tap.png`
- Poll results: `../qa-screenshots/24-poll-results.png`
- Home after QA cleanup: `../qa-screenshots/25-home-after-qa-cleanup.png`

## Cleanup

The QA-only event `qa-event-visualize-1` was removed from the simulator
database after testing. The final Home screenshot confirms the empty state.
