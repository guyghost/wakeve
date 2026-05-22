## ADDED Requirements

### Requirement: Critical Organization Offline Writes MUST be replayable
Wakeve MUST support local-first writes and queued synchronization for every critical organization operation.

#### Scenario: Organizer completes logistics while offline
- **GIVEN** the organizer is offline during `ORGANIZING`
- **WHEN** they select lodging, select transport, create a meeting, update budget, record a payment handoff, and schedule reminders
- **THEN** each operation writes to local storage immediately
- **AND** each operation creates a sync queue entry with entity type, operation type, payload, timestamp, and retry state
- **AND** the UI shows pending sync status for affected sections

### Requirement: Finalization Sync Safety MUST be enforced
Wakeve MUST prevent finalization while blocking critical organization operations are unsynced or conflicted.

#### Scenario: Finalization blocked by unresolved conflict
- **GIVEN** an event is in `ORGANIZING`
- **AND** a selected lodging update has an unresolved sync conflict
- **WHEN** the organizer attempts to finalize
- **THEN** finalization is blocked
- **AND** the readiness summary points to the conflicted lodging update
- **AND** the event remains editable until the conflict is resolved
