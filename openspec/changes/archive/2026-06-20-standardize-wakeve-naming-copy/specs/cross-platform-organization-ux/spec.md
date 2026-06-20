## ADDED Requirements

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
