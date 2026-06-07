## ADDED Requirements
### Requirement: Scenario Matrix Generation MUST be deterministic
Wakeve MUST generate one draft scenario for every valid `TimeSlot × PotentialLocation` combination for an event using scenario matrix planning mode.

#### Scenario: Generate all combinations
- **GIVEN** an event has 3 proposed time slots
- **AND** 2 potential locations
- **WHEN** the organizer generates the scenario matrix
- **THEN** Wakeve creates 6 draft scenarios
- **AND** each scenario stores its source time slot ID, source potential location ID, and generation type `MATRIX`

#### Scenario: Regeneration avoids duplicates
- **GIVEN** an event already has generated matrix scenarios
- **WHEN** the organizer regenerates the matrix without changing source time slots or locations
- **THEN** Wakeve does not create duplicate scenarios for the same source time slot and potential location pair

### Requirement: Generated Matrix Scenarios MUST be editable before publication
Wakeve MUST let the organizer edit or remove generated draft matrix scenarios before participants can vote on them.

#### Scenario: Organizer removes a generated scenario
- **GIVEN** an event has draft matrix scenarios
- **WHEN** the organizer removes one scenario before publishing
- **THEN** the removed scenario is deleted locally
- **AND** it is not published for participant voting

#### Scenario: Organizer publishes generated scenarios
- **GIVEN** an event has at least one draft matrix scenario
- **WHEN** the organizer publishes the matrix
- **THEN** draft matrix scenarios become `PROPOSED`
- **AND** participants can vote `PREFER`, `NEUTRAL`, or `AGAINST`
## MODIFIED Requirements
### Requirement: Scenario Comparison for Confirmed Events MUST be supported
Wakeve MUST let organizers and eligible participants compare scenarios that combine date or period, destination, lodging option when available, estimated participants, duration, and estimated cost. Scenario comparison MUST be available either after date confirmation in the legacy flow or directly in `COMPARING` for scenario matrix events.

#### Scenario: Participants vote on generated matrix scenarios
- **GIVEN** an event uses planning mode `SCENARIO_MATRIX`
- **AND** the organizer has published generated scenarios
- **WHEN** participants open the scenario list
- **THEN** they can compare each complete date-and-destination scenario
- **AND** they can vote `PREFER`, `NEUTRAL`, or `AGAINST` on each scenario
- **AND** the system ranks scenarios using weighted scoring
