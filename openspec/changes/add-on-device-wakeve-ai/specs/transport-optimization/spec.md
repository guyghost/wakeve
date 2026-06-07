## ADDED Requirements

### Requirement: AI-Assisted Transport Coordination Helper
Wakeve SHALL provide an on-device AI transport helper that suggests coordination next steps from real transport context.

The helper MAY suggest who can coordinate with whom, which departure details are missing, and an editable group message draft. It SHALL NOT compute authoritative route prices, invent schedules, assign participants to trips, or select a transport plan automatically.

#### Scenario: Transport helper identifies missing departure details
- **GIVEN** an event has a confirmed destination and some confirmed participants have no departure location
- **WHEN** the organizer opens the transport helper
- **THEN** WakeveAI uses transport and participant tools to identify missing departure details
- **AND** suggests an editable message asking the relevant participants to complete their departure information
- **AND** no message is sent automatically.

#### Scenario: Existing route facts are summarized
- **GIVEN** transport routes have already been proposed in Wakeve
- **WHEN** WakeveAI summarizes transport coordination
- **THEN** any mentioned route, participant, schedule, or cost is grounded in `GetTransportContextTool`
- **AND** unsupported route facts are rejected or shown only as uncertain hints.
