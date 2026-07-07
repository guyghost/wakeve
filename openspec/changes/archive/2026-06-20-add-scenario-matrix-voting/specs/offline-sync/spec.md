## ADDED Requirements
### Requirement: Scenario Matrix Operations MUST be offline-first
Wakeve MUST support scenario matrix generation, editing, publication, voting, and final selection while offline by persisting local changes and queuing sync metadata.

#### Scenario: Organizer publishes matrix while offline
- **GIVEN** the organizer is offline
- **AND** draft matrix scenarios exist locally
- **WHEN** the organizer publishes the matrix
- **THEN** scenarios are updated locally to `PROPOSED`
- **AND** the event status is updated locally to `COMPARING`
- **AND** sync operations are queued for the event and affected scenarios

#### Scenario: Organizer selects final matrix scenario while offline
- **GIVEN** the organizer is offline
- **AND** a published matrix scenario exists
- **WHEN** the organizer selects it as final
- **THEN** the selected scenario and event final date are persisted locally
- **AND** sync operations are queued for replay when network access returns
