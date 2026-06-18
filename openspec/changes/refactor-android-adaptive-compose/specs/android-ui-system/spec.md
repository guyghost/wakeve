## ADDED Requirements
### Requirement: Android Compose Design Foundation
Wakeve Android MUST provide a centralized Compose-first design foundation built on Material 3.

#### Scenario: App content is rendered inside WakeveTheme
- **WHEN** any main Android screen is rendered
- **THEN** the screen is wrapped in `WakeveTheme`
- **AND** colors, typography, spacing, shapes, and component defaults come from centralized Wakeve design tokens.

#### Scenario: Dynamic color has a fallback
- **WHEN** Wakeve runs on Android 12 or newer with dynamic color enabled
- **THEN** the theme uses Material dynamic color schemes
- **AND** when dynamic color is unavailable, Wakeve uses explicit light and dark fallback color schemes.

### Requirement: Android UI Components
Wakeve Android MUST expose reusable Material 3-backed Wakeve UI components for the main event flows.

#### Scenario: Event screens use reusable components
- **WHEN** event list, event detail, event creation, poll/date selection, and RSVP screens are rendered
- **THEN** repeated UI elements such as scaffolds, top bars, cards, list rows, chips, buttons, loading states, empty states, error states, and sync indicators use Wakeve reusable components.

#### Scenario: Expressive-ready components are used behind stable APIs
- **WHEN** Wakeve renders segmented lists, search, button groups, menus, or progress indicators
- **THEN** the app uses Wakeve wrapper components backed by current Material 3 APIs
- **AND** those wrappers can adopt future Material 3 Expressive APIs without changing screen-level composable contracts.

### Requirement: Main Event Flows Use Compose Material 3
Wakeve Android MUST implement the main event planning flows with Jetpack Compose and Material 3 components.

#### Scenario: Main event flow opens without legacy View dependency
- **WHEN** the user navigates through event list, event detail, event creation, poll/date selection, and RSVP screens
- **THEN** those screens are rendered with Compose UI
- **AND** no main-flow screen requires XML layout inflation, Fragment UI, or legacy Android View screens.

#### Scenario: Deprecated creation screen is removed from the main flow
- **WHEN** the user starts creating an event from Android navigation
- **THEN** the app opens the Draft Event Wizard or its modern replacement
- **AND** the deprecated single-form `EventCreationScreen` is not used as the main creation route.

### Requirement: UI State Boundaries
Wakeve Android composables MUST render immutable UI state and keep business logic outside composables.

#### Scenario: Screen content receives immutable state
- **WHEN** a main-flow content composable renders
- **THEN** it receives an immutable UI state object and callback lambdas
- **AND** it does not directly call repositories, run use cases, or construct persisted domain objects.

#### Scenario: User actions are dispatched to ViewModels
- **WHEN** the user filters events, opens details, creates an event, votes on a date, or submits RSVP
- **THEN** the action is dispatched to a ViewModel or presentation event handler
- **AND** repository writes and domain validation happen outside the content composable.

### Requirement: Adaptive Event Layout
Wakeve Android MUST provide adaptive event list and event detail layouts for compact and large screens.

#### Scenario: Phone uses single-pane navigation
- **WHEN** the app is displayed in a compact-width phone layout
- **THEN** the event list and event detail render as separate single-pane destinations
- **AND** navigation between list and detail preserves the selected event.

#### Scenario: Large screens use list-detail layout
- **WHEN** the app is displayed on a tablet, foldable, landscape, or desktop-size expanded layout
- **THEN** the event list and selected event detail can be shown side-by-side
- **AND** selecting a different event updates the detail pane without leaving the list.

#### Scenario: Adaptive navigation is isolated behind route state
- **WHEN** Android navigation renders top-level destinations
- **THEN** destination definitions and selected route state are isolated from screen implementations
- **AND** the structure can migrate toward Navigation 3 concepts without rewriting business logic.

### Requirement: Android Previews and UI Tests
Wakeve Android MUST include previews and UI tests for the modernized main event UI.

#### Scenario: Key screens have size previews
- **WHEN** developers inspect Compose previews
- **THEN** event list, event detail, event creation, poll/date selection, and RSVP screens have representative phone, tablet, and foldable previews.

#### Scenario: Core event flows have UI tests
- **WHEN** Android UI tests run
- **THEN** tests cover event list rendering, event detail rendering, adaptive list-detail behavior, event creation wizard entry, and poll or RSVP vote submission behavior.
