## ADDED Requirements
### Requirement: Scenario Matrix Planning Mode MUST be supported
Wakeve MUST allow organizers to choose a scenario matrix planning mode where participants vote on complete date-and-destination scenarios instead of voting only on time slots.

#### Scenario: Organizer creates a scenario matrix event
- **GIVEN** an organizer is creating an event with at least one proposed time slot
- **AND** at least one potential location has been added
- **WHEN** the organizer chooses scenario matrix planning mode
- **THEN** the event is saved with planning mode `SCENARIO_MATRIX`
- **AND** the event remains in `DRAFT` until generated scenarios are published
- **AND** the legacy time-slot polling mode remains available as the default planning mode

#### Scenario: Matrix mode requires at least one destination
- **GIVEN** an organizer chooses scenario matrix planning mode
- **AND** no potential location exists for the event
- **WHEN** the organizer attempts to generate or publish matrix scenarios
- **THEN** Wakeve blocks the action
- **AND** reports that at least one destination is required

### Requirement: Matrix Scenario Selection MUST lock date and destination
Wakeve MUST lock both final date and final destination when the organizer selects a final scenario generated from the scenario matrix.

#### Scenario: Organizer selects final matrix scenario
- **GIVEN** an event is in `COMPARING`
- **AND** a published matrix scenario exists with source time slot and potential location references
- **WHEN** the organizer selects that scenario as final
- **THEN** the event final date is set from the scenario source time slot
- **AND** the selected destination is available to destination, lodging, transport, budget, and organization workflows
- **AND** the event transitions to `CONFIRMED`
## MODIFIED Requirements
### Requirement: The system SHALL transition an event to POLLING status and enable participants to vote on proposed time slots
The system SHALL transition a `TIME_SLOT_POLL` event to `POLLING` status and enable participants to vote on proposed time slots. For `SCENARIO_MATRIX` events, participants SHALL vote on published scenarios in `COMPARING` status instead of voting directly on time slots.

#### Scenario: Organizer launches legacy poll
- **GIVEN** an event uses planning mode `TIME_SLOT_POLL`
- **WHEN** Organizer clicks "Start Poll"
- **THEN** Event status changes to `POLLING`, poll is initialized with empty votes, and participants can vote on time slots.

#### Scenario: Organizer publishes matrix scenarios
- **GIVEN** an event uses planning mode `SCENARIO_MATRIX`
- **AND** draft matrix scenarios have been generated
- **WHEN** Organizer publishes the scenario matrix
- **THEN** Event status changes to `COMPARING`
- **AND** participants can vote on the published scenarios.
