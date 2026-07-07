## ADDED Requirements

### Requirement: Participant Transport Planning MUST be supported
Wakeve MUST generate and persist transport plans from confirmed participants' departure locations to the confirmed destination and date.

#### Scenario: Organizer computes a balanced transport plan
- **GIVEN** an event has a confirmed destination and final date
- **AND** confirmed participants have departure locations
- **WHEN** the organizer requests a balanced transport plan
- **THEN** the system produces route options per participant with cost, duration, arrival time, and provider metadata
- **AND** the organizer can select one plan as the event transport plan
- **AND** selected transport costs feed the event budget

### Requirement: Transport Missing Data Handling MUST be available
Wakeve MUST identify missing participant departure data and keep transport readiness incomplete until required data is provided or transport is marked not needed.

#### Scenario: Participant departure location is missing
- **GIVEN** transport is required for an event
- **AND** one confirmed participant has no departure location
- **WHEN** the organizer opens transport planning
- **THEN** the system shows the missing participant data
- **AND** blocks transport readiness
- **AND** can notify the participant to add the missing departure location
