## ADDED Requirements

### Requirement: Workflow State Machines Own AI-Proposed Actions
Workflow state machines SHALL remain the only authority for event lifecycle transitions, feature unlocking, navigation side effects, and workflow persistence. AI modules MAY propose intents or draft values, but those proposals SHALL be validated through existing guards before any state transition occurs.

#### Scenario: AI proposes finalizing an event
- **GIVEN** an AI planning helper proposes that an event is ready to finalize
- **WHEN** the organizer chooses to apply that proposal
- **THEN** Wakeve dispatches the existing finalization intent
- **AND** the workflow state machine enforces all finalization readiness guards
- **AND** the event remains unchanged if any deterministic guard fails.

#### Scenario: AI suggests a next workflow action
- **WHEN** AI suggests the next event action
- **THEN** Wakeve labels it as a suggestion
- **AND** the current persisted event status remains the source of truth for which actions are enabled.
