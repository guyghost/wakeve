## ADDED Requirements
### Requirement: Authenticated Web Dashboard Command Center
Wakeve SHALL provide an authenticated web dashboard command center that presents organizer event data as next actions, lifecycle groups, event cards, and contextual details instead of a default data table.

#### Scenario: Organizer opens dashboard with active events
- **WHEN** an authenticated organizer opens `/app/dashboard`
- **THEN** the dashboard SHALL show the primary create-event action
- **AND** the dashboard SHALL show next actions derived from event status, deadlines, vote completion, and archived or finalized states
- **AND** events SHALL be grouped by lifecycle stage using cards, badges, progress indicators, and deadline tokens
- **AND** secondary actions SHALL be available through contextual controls without crowding each event card.

#### Scenario: Organizer opens event details from dashboard
- **WHEN** an organizer selects an event on the dashboard
- **THEN** the dashboard SHALL reveal a detail drawer with normalized analytics, vote progress, participant status, timeline information, and safe fallbacks for missing data
- **AND** the drawer SHALL include copy-link feedback and non-destructive secondary actions.

#### Scenario: Dashboard data is unavailable or incomplete
- **WHEN** dashboard data is loading, empty, offline, denied, erroneous, archived, expired, closed, pending, partial, or contradictory
- **THEN** the dashboard SHALL show a clear state message and the safest available primary action
- **AND** the UI SHALL NOT imply that unavailable backend data such as budget, transport, lodging, or task details has been loaded when it is absent from the dashboard API.
