# cross-platform-organization-ux Specification

## Purpose
TBD - created by archiving change complete-event-organization-flow. Update Purpose after archive.
## Requirements
### Requirement: Cross-Platform Organization Dashboard
Wakeve MUST provide coherent Android and iOS organization dashboards that expose the same workflow phases, readiness items, access states, and offline states using platform-native UI patterns.

#### Scenario: Organizer compares Android and iOS organization state
- **GIVEN** the same event is in `ORGANIZING` on Android and iOS
- **WHEN** the organizer opens the organization dashboard on both platforms
- **THEN** both platforms show the same readiness sections and blocking items
- **AND** Android uses Material You Compose patterns
- **AND** iOS uses SwiftUI Liquid Glass-compatible patterns
- **AND** actions are labeled consistently through localization keys

### Requirement: Participant Access and Pending States UX
Wakeve MUST clearly distinguish unavailable, access-denied, optional-not-needed, incomplete, complete, and pending-sync states in Android and iOS UI.

#### Scenario: Confirmed participant views pending offline budget update
- **GIVEN** a confirmed participant is offline
- **AND** a budget expense was recorded locally but not synced
- **WHEN** the participant opens the organization dashboard
- **THEN** the budget section shows a pending-sync state
- **AND** the participant can still view local details
- **AND** the UI does not imply the update has been confirmed by the server
