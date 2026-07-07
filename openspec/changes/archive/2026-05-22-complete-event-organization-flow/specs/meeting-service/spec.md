## ADDED Requirements

### Requirement: Event Meeting Readiness MUST be tracked
Wakeve MUST support required or optional virtual meeting links for the organizing phase.

#### Scenario: Organizer creates a planning meeting
- **GIVEN** an event is in `ORGANIZING`
- **WHEN** the organizer creates a virtual meeting for confirmed participants
- **THEN** the meeting is persisted with platform, title, date, link, invited participants, and status
- **AND** invitations are sent to confirmed participants
- **AND** calendar entries and reminders are created when enabled

### Requirement: Meeting Link Safety MUST be enforced
Wakeve MUST treat external meeting links as safe link records with provider, display label, target URL, creator, creation time, and verification state.

#### Scenario: Participant opens a meeting link
- **GIVEN** a confirmed participant opens a stored meeting link
- **WHEN** the link target does not match the stored provider metadata
- **THEN** the system warns the participant before opening
- **AND** records the suspicious-link event for audit
