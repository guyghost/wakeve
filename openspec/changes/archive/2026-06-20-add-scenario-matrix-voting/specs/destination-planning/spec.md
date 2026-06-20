## ADDED Requirements
### Requirement: Potential Locations MUST seed scenario matrix destinations
Wakeve MUST use an event's potential locations as the destination source for generated matrix scenarios.

#### Scenario: Matrix scenario references potential location
- **GIVEN** an event has a potential location named "Lyon"
- **AND** the event has a proposed time slot
- **WHEN** Wakeve generates the scenario matrix
- **THEN** the generated scenario location is "Lyon"
- **AND** the scenario stores the potential location ID as its source destination reference

#### Scenario: Selected matrix scenario provides destination input
- **GIVEN** a matrix scenario has been selected as final
- **WHEN** destination, lodging, transport, or budget planning starts
- **THEN** those workflows use the selected scenario location and source potential location as their destination input
