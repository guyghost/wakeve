## MODIFIED Requirements

### Requirement: Destination-Only iOS Tab Navigation
The iOS application SHALL provide a predictable tab bar containing only navigation destinations. The tab bar SHALL include the internal Home destination labeled `À venir` in French and `Upcoming` in English, plus Ideas, Messages, and Profile using approved localized labels. It SHALL NOT include Create Event as a permanent tab destination. The Ideas destination SHALL contain event-preparation ideas and templates and SHALL NOT represent a generic social or group-discovery feed.

Create Event SHALL appear contextually as a floating Liquid Glass button on Home, a toolbar action where directly relevant, or a bottom sheet action when the user is already in a flow.

#### Scenario: User opens the app tab bar
- **WHEN** the authenticated user views the main iOS tab bar
- **THEN** the internal Home destination SHALL be labeled `À venir`, `Upcoming`, or the approved active-locale equivalent
- **AND** the tab bar SHALL show Ideas, Messages, and Profile as the other localized destinations
- **AND** each tab SHALL use an explicit label and a clean line icon
- **AND** no tab item SHALL represent a one-off action such as creating an event.

#### Scenario: User creates an event from Home
- **WHEN** the user is on Home and wants to create an event
- **THEN** a contextual floating Liquid Glass create action SHALL be available
- **AND** activating it SHALL open the create event flow without changing the tab model.

#### Scenario: Ideas tab identifier is migrated
- **GIVEN** the UI-only tab case was previously `.groups`
- **WHEN** it is migrated to `.ideas`
- **THEN** restored selection and supported deep links SHALL resolve to the Ideas destination
- **AND** domain workflow state SHALL remain unchanged.
