## ADDED Requirements

### Requirement: Confirmed-Attendee Detail Access MUST be enforced
Wakeve MUST restrict full event organization details to organizers and participants confirmed for the retained date.

#### Scenario: Unconfirmed participant opens organization details
- **GIVEN** an event has a confirmed date
- **AND** the current user is invited but has not confirmed attendance
- **WHEN** the user requests transport, lodging, budget, payment, or meeting details
- **THEN** the system denies access
- **AND** returns or displays an access-denied reason
- **AND** records the authorization denial in audit logs where backend access is attempted

### Requirement: External Organization Link Safety MUST be enforced
Wakeve MUST validate and audit external links used for payment, Tricount, booking, calendar download, and meetings.

#### Scenario: Suspicious payment link is saved
- **GIVEN** an organizer adds an external payment link
- **WHEN** the URL scheme, host, or provider metadata fails validation
- **THEN** the system rejects the link or marks it unverified
- **AND** the unsafe link cannot be presented as a trusted action
