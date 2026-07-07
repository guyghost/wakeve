## ADDED Requirements

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
