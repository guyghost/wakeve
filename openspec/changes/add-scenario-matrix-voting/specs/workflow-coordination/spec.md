## ADDED Requirements
### Requirement: Scenario Matrix Workflow MUST be coordinated through repositories
Wakeve MUST coordinate scenario matrix generation, publication, voting, and selection through persisted repository state without direct state-machine dependencies.

#### Scenario: Matrix generation remains draft-only
- **GIVEN** an event is in `DRAFT` and uses planning mode `SCENARIO_MATRIX`
- **WHEN** the organizer generates the scenario matrix
- **THEN** generated scenarios are persisted with status `DRAFT`
- **AND** event status remains `DRAFT`
- **AND** no participant voting side effect is emitted

#### Scenario: Matrix publication enters comparing phase
- **GIVEN** an event is in `DRAFT` and has at least one draft matrix scenario
- **WHEN** the organizer publishes the matrix
- **THEN** generated scenarios are updated to `PROPOSED`
- **AND** the event status changes to `COMPARING`
- **AND** scenario voting UI becomes available

### Requirement: Final Matrix Scenario Selection MUST confirm the event
Wakeve MUST transition a scenario matrix event from `COMPARING` to `CONFIRMED` when the organizer selects the final matrix scenario.

#### Scenario: Matrix final selection confirms event
- **GIVEN** an event uses planning mode `SCENARIO_MATRIX`
- **AND** the event is in `COMPARING`
- **WHEN** the organizer selects a final matrix scenario
- **THEN** the selected scenario becomes `SELECTED`
- **AND** competing scenarios become `REJECTED`
- **AND** event status changes to `CONFIRMED`
- **AND** organization modules derive date and destination from the selected scenario
## MODIFIED Requirements
### EventManagementStateMachine SHALL provide four workflow transition Intents
EventManagementStateMachine SHALL continue to provide workflow transition intents for the legacy time-slot polling flow. Scenario matrix generation, publication, and final selection SHALL be handled by ScenarioManagementStateMachine using persisted event and scenario repository state.

#### Scenario: StartPoll remains legacy-only
- **WHEN** Organizer dispatches `StartPoll(eventId)` for an event using planning mode `TIME_SLOT_POLL`
- **THEN** the state machine validates event status is `DRAFT`
- **AND** updates event status to `POLLING`

#### Scenario: StartPoll rejected for matrix mode
- **WHEN** Organizer dispatches `StartPoll(eventId)` for an event using planning mode `SCENARIO_MATRIX`
- **THEN** the state machine blocks the transition
- **AND** reports that matrix events must publish scenarios instead
