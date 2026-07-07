# cross-platform-organization-ux Specification

## Purpose
Define the cross-platform organization experience, shared workflow language, and user-facing state clarity for Android and iOS event planning surfaces.

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

### Requirement: Shared Product Naming System
Wakeve MUST maintain a shared FR/EN naming system for primary navigation, event organization objects, action labels, states, empty states, notifications, and AI-assisted copy.

#### Scenario: User compares Android and iOS labels
- **GIVEN** the same event workflow is available on Android and iOS
- **WHEN** the user moves between equivalent screens
- **THEN** primary labels use the same approved concept names in each locale
- **AND** critical actions describe the result of the action without branded or technical wording
- **AND** AI-assisted labels describe the user benefit rather than the underlying technology

### Requirement: Naming Review for User-Facing Copy
Wakeve MUST review new user-facing labels against clarity, expectation fit, localization, spoken naturalness, compact-display readability, and glossary consistency before release.

#### Scenario: Product copy is added to a critical action
- **GIVEN** a new button, destructive action, permission prompt, invitation, or confirmation message is added
- **WHEN** the label is reviewed
- **THEN** the label is scored against the Wakeve naming guidelines
- **AND** labels below the accepted threshold are revised before release
