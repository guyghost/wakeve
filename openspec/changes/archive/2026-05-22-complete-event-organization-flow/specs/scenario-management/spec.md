## ADDED Requirements

### Requirement: Scenario Comparison for Confirmed Events MUST be supported
Wakeve MUST let organizers and confirmed participants compare scenarios that combine date or period, destination, lodging option, estimated participants, duration, and estimated cost.

#### Scenario: Confirmed participants vote on scenarios
- **GIVEN** an event has a confirmed date and scenarios are unlocked
- **WHEN** the organizer publishes multiple scenarios
- **THEN** confirmed participants can vote `PREFER`, `NEUTRAL`, or `AGAINST` on each scenario
- **AND** the system ranks scenarios using weighted scoring
- **AND** pending or declined participants cannot view private scenario details

### Requirement: Final Scenario Selection MUST be available
Wakeve MUST allow the organizer to select a final scenario or explicitly skip scenarios when the event does not require scenario comparison.

#### Scenario: Organizer selects final scenario
- **GIVEN** an event is in `COMPARING`
- **AND** at least one scenario exists
- **WHEN** the organizer selects a final scenario
- **THEN** the selected scenario is marked final
- **AND** related destination, lodging, budget estimate, and transport destination data become inputs for the organizing phase
- **AND** the selection is persisted locally and queued for sync when offline
