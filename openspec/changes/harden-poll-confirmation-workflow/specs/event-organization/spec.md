## MODIFIED Requirements

### Requirement: Confirmed Date Downstream Effects SHALL Fan Out Per Participant
One confirmed-date decision SHALL produce exactly one durable domain-effect envelope. After backend acknowledgement, Wakeve SHALL resolve eligible participants and independently fan out notification and calendar effects using stable recipient and artifact identities. Exactly-one applies to the domain envelope, not to a single calendar artifact for the whole event.

#### Scenario: Confirmed date has multiple participants
- **GIVEN** one confirmed-date envelope is accepted by the backend
- **WHEN** three eligible participants are resolved
- **THEN** Wakeve SHALL retain one `effectKey`
- **AND** SHALL create idempotent per-participant `recipientKey` and `calendarArtifactKey` projections as applicable
- **AND** replay SHALL NOT duplicate any participant artifact.

### Requirement: Organizers SHALL be able to validate and confirm the final event date
Organizers SHALL be able to validate and confirm the final event date, transitioning the event to CONFIRMED status. Confirmation MUST require an explicit organizer action, a `POLLING` event, at least one recorded vote, and a valid proposed slot belonging to the event. The voting deadline MUST close new or changed votes but MUST NOT prevent an otherwise valid organizer confirmation at or after the deadline.

**ID**: `event-org-007`

#### Scenario: Organizer confirms final date
- **GIVEN** a `TIME_SLOT_POLL` event is in `POLLING`
- **AND** at least one participant vote has been recorded
- **AND** the organizer has selected the recommended proposed slot or another valid proposed slot
- **WHEN** the organizer explicitly confirms that slot
- **THEN** the event transitions to `CONFIRMED`
- **AND** the final date and confirmed-date record reference the exact selected slot
- **AND** exactly one local domain-effect envelope is durably queued by the confirmation workflow
- **AND** participant notification and calendar work is derived later by backend fan-out after envelope acknowledgement.

#### Scenario: Organizer confirms after the voting deadline
- **GIVEN** a `TIME_SLOT_POLL` event is in `POLLING`
- **AND** the injected clock instant is equal to or later than the event voting deadline
- **AND** voting is read-only and at least one vote was recorded before closure
- **AND** the current user is the organizer
- **AND** the organizer is reviewing results with a valid proposed slot selected
- **WHEN** the organizer explicitly confirms the selected slot
- **THEN** the elapsed deadline does not reject the confirmation
- **AND** the event transitions to `CONFIRMED`
- **AND** the final date and confirmed-date record reference the exact selected slot
- **AND** no new or changed vote is accepted after the deadline
- **AND** downstream notification, calendar/invitation, and sync work follows the deterministic confirmation workflow.
