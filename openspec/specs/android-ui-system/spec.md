# android-ui-system Specification

## Purpose
Define Wakeve Android's Compose Material 3 design foundation, adaptive event layout behavior, UI state boundaries, previews, and core UI test expectations.
## Requirements
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

### Requirement: Android Adaptive Capability Model
Wakeve Android MUST adapt UI using capability-based signals rather than device names or form-factor-specific branches.

#### Scenario: Layout responds to available space
- **GIVEN** the event workspace is rendered in containers of different width and height
- **WHEN** the adaptive helper classifies the current capability
- **THEN** it exposes width class, height class, navigation kind, grid column count, filter layout kind, list-detail eligibility, and compact-chrome eligibility
- **AND** decisions are based on available size and input capability rather than a hardcoded tablet or phone flag.

### Requirement: Android Adaptive Navigation Chrome
Wakeve Android MUST use adaptive navigation chrome for the main destinations.

#### Scenario: Compact width uses bottom navigation
- **GIVEN** the app is displayed in compact width
- **WHEN** the main scaffold renders
- **THEN** primary navigation is shown as a bottom navigation bar.

#### Scenario: Larger widths use navigation rail
- **GIVEN** the app is displayed with medium or expanded width
- **WHEN** the main scaffold renders
- **THEN** primary navigation is shown as a navigation rail
- **AND** screen content remains in a single shared composable hierarchy.

### Requirement: Android Event Workspace Adaptive Layout
Wakeve Android MUST adapt the event list, event detail, event cards, and filters to the available workspace.

#### Scenario: Compact width uses single pane
- **GIVEN** the event workspace is rendered in compact width
- **WHEN** the user opens the event list
- **THEN** the UI shows a single event-list pane
- **AND** selecting an event navigates to the detail route.

#### Scenario: Large workspace uses list-detail
- **GIVEN** the event workspace has enough width and non-compact height
- **WHEN** the user opens the event workspace
- **THEN** the event list and selected event detail are visible side by side
- **AND** when no event is selected the detail pane shows "Select an event".

#### Scenario: Event cards use adaptive columns
- **GIVEN** the event list container changes width
- **WHEN** event cards are rendered
- **THEN** cards use 1 column on narrow containers, 2 columns on medium containers, and 3 or more columns on large containers.

#### Scenario: Filters wrap on larger containers
- **GIVEN** the filter container is at least 600dp wide
- **WHEN** filters are rendered
- **THEN** filter chips wrap onto multiple lines as needed
- **AND** below 600dp they remain horizontally scrollable.

### Requirement: Android Landscape and Rotation Behavior
Wakeve Android MUST remain usable in landscape and preserve UI state across configuration changes.

#### Scenario: Landscape compact height reduces chrome
- **GIVEN** the app is shown in compact-height landscape
- **WHEN** the event workspace renders
- **THEN** vertical chrome is reduced so event content remains usable
- **AND** edge-to-edge content avoids cropped controls.

#### Scenario: Rotation preserves event workspace state
- **GIVEN** a user has selected an event, filter, and search query
- **WHEN** the Android activity is recreated due to rotation
- **THEN** the selected event, selected filter, and search query are restored.

### Requirement: Android Adaptive Screenshot Coverage
Wakeve Android MUST provide fake-data screenshot or UI tests for the primary adaptive breakpoints.

#### Scenario: Screenshot suite covers adaptive breakpoints
- **WHEN** the Android adaptive screenshot tests run
- **THEN** they cover phone portrait, phone landscape, foldable, tablet, and desktop-size window configurations
- **AND** each configuration uses deterministic fake event data.

### Requirement: Android Navigation 3 Migration Readiness
Wakeve Android MUST document and prepare a migration path from Navigation 2 route strings to Navigation 3 state-driven keys.

#### Scenario: Navigation 3 cannot be fully adopted in one pass
- **GIVEN** the app still uses Navigation 2 for production routes
- **WHEN** the adaptive navigation work is delivered
- **THEN** migration notes explain why Navigation 3 was not fully adopted
- **AND** route/content boundaries are prepared so Navigation 3 `NavDisplay` can reuse the same screen content.
