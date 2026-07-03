## ADDED Requirements

### Requirement: Deterministic AI Product Gate
User-facing AI proposals SHALL explain how AI remains reviewable, event-scoped, metadata-backed, and subordinate to deterministic workflow state. Proposals SHALL identify which deterministic model, state machine, validator, or use case owns any eventual state change.

#### Scenario: Future AI proposal changes event data
- **GIVEN** a future proposal adds AI-assisted event updates
- **WHEN** the proposal is reviewed against product excellence
- **THEN** it identifies the deterministic owner that applies the update after user confirmation
- **AND** it describes metadata, validation, rejected-output behavior, and fallback behavior when AI is unavailable.

#### Scenario: AI would make an irreversible decision
- **GIVEN** a proposed feature allows AI to send invitations, confirm payments, finalize events, modify permissions, or resolve sync conflicts without explicit user confirmation and deterministic guards
- **WHEN** the proposal is reviewed
- **THEN** Wakeve rejects or rescopes the feature before implementation.
