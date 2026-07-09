## ADDED Requirements

### Requirement: Invitation Opening SHALL Preserve Truthful Offline State
When an invitation opens without network access or before authentication, Wakeve SHALL retain a protected pending invitation with bounded local lifetime and resume the modeled flow later. Inspection and redemption SHALL have separate bounded retry budgets, attempts, next-attempt timestamps, backoff, expiry, and stable operation identities. Offline state MUST NOT imply server validation or membership. Typed outcomes SHALL include retryable `networkUnavailable`, `timeout`, and `serverUnavailable`, plus terminal `invalid`, `expired`, `revoked`, `forbidden`, and `cancelled`.

#### Scenario: Logged-out user opens an invitation offline
- **GIVEN** a validly shaped canonical invitation URL opens while offline and logged out
- **WHEN** Wakeve handles the Universal Link
- **THEN** it SHALL store a protected pending invitation and show network/authentication requirements
- **AND** SHALL resume inspection after connectivity and authentication
- **AND** SHALL not show the user as joined before server acknowledgement.

#### Scenario: User cancels a pending invitation
- **GIVEN** inspection or authentication handoff is pending and no redeem commit exists
- **WHEN** the user cancels
- **THEN** Wakeve SHALL delete the protected pending invitation and stop scheduled retries
- **AND** SHALL not revoke the server invitation or change membership.

#### Scenario: Invitation is revoked during offline replay
- **GIVEN** a previously valid invitation was queued offline
- **AND** the organizer revoked it before replay
- **WHEN** connectivity returns and inspection or redemption resumes
- **THEN** the backend SHALL return terminal `revoked`
- **AND** Wakeve SHALL clear retry scheduling and expose the responsible organizer/new-link next action
- **AND** SHALL not rely on cached validity.

#### Scenario: Retry budget or pending lifetime ends
- **GIVEN** inspection or redemption remains retryable
- **WHEN** its retry budget or protected pending lifetime is exhausted
- **THEN** Wakeve SHALL transition to terminal `retryExhausted` or `pendingExpired`
- **AND** require a fresh explicit user action or link before further requests.
