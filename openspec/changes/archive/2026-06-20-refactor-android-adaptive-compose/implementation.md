# Android UI Modernization Implementation Notes

## Architecture
- `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/designsystem/` owns reusable Material 3 wrappers and centralized tokens:
  - `WakeveSpacing`, `WakeveSize`, `WakeveElevation`, `WakeveShapes`
  - `WakeveScaffold`, `WakeveCard`, `WakeveSearchBar`, `WakeveSegmentedOptions`, `WakeveButtonGroup`, `WakeveMenu`, `WakeveProgressIndicator`, state/sync indicators
- `WakeveTheme` remains the Android public theme entrypoint and now consumes centralized Wakeve shapes while keeping dynamic color and fallback light/dark schemes.
- Main event UI is split into route/state layers and stateless content:
  - `EventWorkspaceRoute` collects event state and dispatches ViewModel intents.
  - `EventWorkspaceScreen` renders immutable `EventWorkspaceUiState` and callback lambdas only.
  - `EventDetailScreen` keeps ViewModel and navigation side effects in the route wrapper, while `EventDetailContent` renders `EventDetailUiState`.
  - `PollVotingScreen` renders `PollVotingState`; vote selection and submission are owned by `PollViewModel`.
  - `PollResultsScreen` renders `PollResultsUiState`; final date selection and confirmation are owned by `PollViewModel`.
  - `DraftEventWizard` is entered through `DraftEventCreationRouteUiState`, which tracks route-level draft persistence without ad hoc flags.
  - `DraftEventWizard` now stores its step/form/dialog state in a single immutable `DraftEventWizardUiState`; field changes use `copy(...)` updates instead of independent mutable vars.
  - `DraftEventWizard` emits `DraftEventWizardUiState` on save/complete. The Android navigation route builds the domain `Event` with `DraftEventWizardEventFactory` before dispatching ViewModel intents, so event construction is outside the wizard content.
  - `EventRsvpResponseCard` is reusable and callback-driven. RSVP persistence is ready for a repository-backed use case, but no persistent RSVP field was added in this change.
- Deprecated single-form creation UI is quarantined:
  - Android navigation routes creation to `DraftEventWizard`.
  - `EventCreationScreen` is marked `DeprecationLevel.ERROR` to prevent accidental reuse.
  - The optional JVM demo no longer calls `EventCreationScreen`.

## Adaptive Layout
- `WakeveAdaptivePane` classifies compact, medium, and expanded widths.
- Compact screens render event list/detail as single-pane navigation.
- Medium and expanded screens render event list and selected event detail side by side.
- Navigation remains on current Compose Navigation, with route state isolated in `EventWorkspaceRoute` so future Navigation 3 migration can avoid rewriting screen content.

## Verification
- `./gradlew :composeApp:compileDebugKotlinAndroid --no-configuration-cache` passes.
- `./gradlew :composeApp:testDebugUnitTest --tests com.guyghost.wakeve.ui.event.EventCreationModelsTest --tests com.guyghost.wakeve.ui.event.PollResultsModelsTest --tests com.guyghost.wakeve.viewmodel.PollViewModelAndroidUnitTest --tests com.guyghost.wakeve.ui.event.EventDetailModelsTest --tests com.guyghost.wakeve.ui.event.EventWorkspaceModelsTest --no-configuration-cache` passes.
- `./gradlew :composeApp:compileDebugAndroidTestKotlinAndroid --no-configuration-cache` passes, including the new Compose UI tests.
- A temporary API 32 ARM64 AVD (`wakeve_api32`) was created from the installed Android SDK system image and booted headless.
- `adb shell wm size 1280x800 && adb shell wm density 160` was used for tablet/landscape-width connected verification, then reset after testing.
- `./gradlew :composeApp:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.guyghost.wakeve.ui.event.EventWorkspaceScreenTest --no-configuration-cache` passes with 7 focused Compose UI tests covering compact list, expanded list-detail, landscape-width primary actions, event selection, create action, poll voting, and RSVP selection.
- `./gradlew :composeApp:compileKotlinJvm -PenableDesktopTarget=true --no-configuration-cache` still fails on existing optional desktop-target issues, including unresolved common Android resources and stale JVM auth/database wiring. This is outside the Android modernization target, but the JVM demo no longer calls the deprecated creation screen.
- `openspec validate refactor-android-adaptive-compose --strict` passes.

## Remaining Work
- Complete broader token cleanup in older/deprecated event screens that are no longer the main route.
- Promote the draft wizard route adapter into a dedicated wizard ViewModel if full process-death restoration is required.
