## ADDED Requirements

### Requirement: Confirmed Date Calendar Fan-Out
Calendar work SHALL be derived on the backend per eligible participant from one confirmed-date domain envelope. Each artifact SHALL use `calendarArtifactKey = (effectKey, participantId, calendarProvider)`. Calendar processing SHALL persist a separate acknowledgement, bounded retry budget with backoff, expiry, and terminal reason independently from decision synchronization and notification delivery.

#### Scenario: Calendar artifact retry is isolated
- **GIVEN** the confirmed decision is acknowledged and a participant calendar artifact fails transiently
- **WHEN** its retry becomes due
- **THEN** Wakeve SHALL retry only that `calendarArtifactKey`
- **AND** SHALL NOT resend notifications, republish the decision, or create a second artifact for the participant.

#### Scenario: Calendar artifact reaches its terminal boundary
- **GIVEN** a calendar artifact has exhausted its retry budget or reached expiry
- **WHEN** the calendar worker evaluates it
- **THEN** it SHALL persist terminal `retryExhausted` or `expired` acknowledgement
- **AND** make no further provider call
- **AND** leave the confirmed decision and notification deliveries unchanged.
