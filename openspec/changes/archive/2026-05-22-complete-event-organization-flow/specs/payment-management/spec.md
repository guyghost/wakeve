## ADDED Requirements

### Requirement: Payment Pot and Tricount Handoff MUST be supported
Wakeve MUST support a payment pot and Tricount handoff state for events that require shared settlement.

#### Scenario: Organizer enables Tricount settlement
- **GIVEN** an event has confirmed participants and shared expenses
- **WHEN** the organizer enables Tricount settlement
- **THEN** the system creates or links a Tricount group record through the configured provider abstraction
- **AND** stores provider id, provider URL, sync status, and last sync timestamp
- **AND** the event payment readiness item becomes complete only when the handoff is linked or explicitly marked not needed

### Requirement: Settlement Calculation MUST be available
Wakeve MUST calculate settlement suggestions from shared expenses, contributions, and payment pot balances.

#### Scenario: System calculates who owes whom
- **GIVEN** multiple confirmed participants have paid or owe shared expenses
- **WHEN** the organizer opens settlements
- **THEN** the system calculates participant balances and suggested transfers
- **AND** settlement suggestions are persisted locally
- **AND** confirmed participants can view their own settlement obligations
