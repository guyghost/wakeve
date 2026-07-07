## ADDED Requirements

### Requirement: Finalization Readiness Gate MUST be enforced
The workflow MUST block `ORGANIZING -> FINALIZED` until required organization sections are complete or explicitly marked not needed.

#### Scenario: Finalization blocked by missing transport plan
- **GIVEN** an event is in `ORGANIZING`
- **AND** transport is required for the selected scenario
- **AND** no selected transport plan exists
- **WHEN** the organizer attempts to finalize the event
- **THEN** the workflow keeps the event in `ORGANIZING`
- **AND** reports transport as a blocking readiness item
- **AND** does not emit a finalized navigation or success side effect

#### Scenario: Finalization succeeds after all readiness checks pass
- **GIVEN** an event is in `ORGANIZING`
- **AND** required scenario, destination, lodging, transport, meeting, calendar, notification, budget, payment, and sync checks are complete or explicitly not needed
- **WHEN** the organizer finalizes the event
- **THEN** the event transitions to `FINALIZED`
- **AND** organization sections become read-only
- **AND** participants receive a finalization notification

### Requirement: Repository-Mediated Cross-Module Progress MUST be coordinated
State machines MUST communicate workflow progress through repositories and persisted event-related records instead of direct state-machine calls.

#### Scenario: Scenario selection unlocks organization work
- **GIVEN** a scenario is selected as final
- **WHEN** the scenario state machine persists the selected scenario and event status
- **THEN** other organization modules derive availability from repository state
- **AND** no direct call is made from the scenario state machine to meeting, transport, budget, or notification state machines
