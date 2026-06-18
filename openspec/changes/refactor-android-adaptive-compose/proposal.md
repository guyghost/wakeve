# Change: Refactor Android UI to adaptive Compose Material 3

## Why
Wakeve's Android UI is already mostly Jetpack Compose, but the main event flows are split across older screen-level implementations, deprecated creation UI, duplicated theme files, and phone-first navigation. The app needs a centralized Material 3 design foundation and adaptive event list/detail structure so the experience feels coherent on phones, foldables, tablets, and desktop-size windows.

## Current UI Audit
- No legacy XML layout files were found under `composeApp/src/androidMain/res/layout`; the main UI dependency risk is not View XML.
- Main Android entry and navigation use Compose in `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/App.kt`, `navigation/WakeveNavHost.kt`, and `navigation/WakeveBottomBar.kt`.
- Core event screens exist as Compose files, but several are phone-first and locally styled:
  - `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/HomeScreen.kt`
  - `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/EventDetailScreen.kt`
  - `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/PollVotingScreen.kt`
  - `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/PollResultsScreen.kt`
  - `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/event/DraftEventWizard.kt`
- `EventCreationScreen.kt` is explicitly deprecated and still contains local state/business object creation inside the composable; it should be removed from the main flow in favor of the wizard/ViewModel path.
- `PollVotingScreen.kt` directly receives `EventRepositoryInterface` and launches repository writes from the composable; this violates the requested ViewModel + immutable UI-state boundary.
- `HomeScreen.kt` contains event filtering and visual event theming directly in the screen file with hardcoded colors/emoji-based styling; this should move to presentation state/mappers and theme-driven reusable components.
- `WakeveTheme` exists in `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/theme/Theme.kt` with dynamic color support, but design tokens are partial and split across `androidMain` and `commonMain` theme files. Shapes and component tokens are not centralized.
- There is no detected adaptive list-detail event route, WindowSizeClass-style layout policy, or Navigation 3-compatible route model for large screens.
- Existing Android UI tests cover auth, onboarding, draft wizard, and selected components, but not the modernized core event list/detail adaptive flow.

## What Changes
- Add a centralized Compose-first Android design system package with `WakeveTheme`, typography, spacing, shapes, color roles, adaptive layout tokens, and reusable Wakeve component wrappers.
- Refactor the main event flows to theme-driven Material 3 Compose screens:
  - Event list/home
  - Event detail
  - Event creation via DraftEventWizard
  - Poll/date voting and poll results
  - RSVP/participant response flow
- Prepare expressive wrappers for segmented lists, search, button groups, menus, and progress indicators so future Material 3 Expressive APIs can be adopted behind stable Wakeve components.
- Add adaptive layout support with single-pane phone behavior and list-detail event behavior on expanded width classes.
- Introduce a route/navigation model that remains compatible with current Compose Navigation while isolating destinations for future Navigation 3 migration.
- Ensure composables render immutable UI state and dispatch events to ViewModels/use cases rather than performing repository writes or business decisions directly.
- Add phone, tablet, foldable, portrait, and landscape previews for key screens.
- Add Android Compose UI tests for event list, event detail, list-detail adaptive layout, event creation wizard entry, and poll/RSVP voting.

## Impact
- Affected specs: `android-ui-system`, `event-organization`, `collaboration-management`, `cross-platform-organization-ux`
- Affected code:
  - `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/theme/`
  - `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/theme/`
  - `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/components/`
  - `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/HomeScreen.kt`
  - `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/EventDetailScreen.kt`
  - `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/PollVotingScreen.kt`
  - `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/PollResultsScreen.kt`
  - `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/event/DraftEventWizard.kt`
  - `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/`
  - `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/viewmodel/`
  - `composeApp/src/androidInstrumentedTest/kotlin/com/guyghost/wakeve/`
