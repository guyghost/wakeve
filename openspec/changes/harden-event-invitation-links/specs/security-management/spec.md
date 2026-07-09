## ADDED Requirements

### Requirement: Invitation Capabilities SHALL Resist Disclosure and Abuse
Invitation tokens SHALL have at least 128 bits of cryptographic entropy, SHALL be stored using protected lookup material, and SHALL never appear in logs, analytics, screenshots, or release evidence. Inspection and redemption SHALL resist enumeration, enforce rate limits, minimize preview data, and return typed expired, revoked, invalid, forbidden, or already-member outcomes without leaking private event information.

Issuance, rotation, and revocation SHALL require an authenticated actor authorized against the server-loaded event and SHALL reject cross-event IDOR. Every token SHALL have an immutable expiry. Revocation SHALL become effective for all new inspections and redemptions after its committed server timestamp. Rotation SHALL create a new token and either revoke the old token immediately or retain it until an explicitly stored grace deadline; no implicit grace is allowed. At the exact grace/revocation boundary, one serialized backend transaction SHALL decide whether an in-flight redemption or revocation/rotation wins, and membership uniqueness SHALL prevent duplicate joins.

#### Scenario: Attacker guesses invitation tokens
- **GIVEN** unauthenticated requests present invalid token candidates
- **WHEN** the inspection endpoint handles them
- **THEN** responses SHALL not reveal whether a private event exists
- **AND** rate limiting and sanitized abuse telemetry SHALL apply
- **AND** no candidate token SHALL be logged.

#### Scenario: Organizer rotates an invitation
- **GIVEN** an active invitation may have been exposed
- **WHEN** an authorized organizer rotates it
- **THEN** the backend SHALL issue a new opaque token
- **AND** SHALL invalidate the prior token according to the explicit grace policy
- **AND** concurrent redemption SHALL resolve deterministically at the backend.

#### Scenario: Redemption races revocation
- **GIVEN** a valid invitation redemption and authorized revocation execute concurrently
- **WHEN** the backend serializes their commits
- **THEN** a redemption committed first MAY create exactly one membership
- **AND** a revocation committed first SHALL make redemption terminal `revoked`
- **AND** no client clock or cached inspection SHALL override the server decision.

#### Scenario: Invitation reaches exact expiry
- **GIVEN** the server clock is equal to or later than the immutable invitation expiry
- **WHEN** inspection or redemption occurs
- **THEN** the backend SHALL return terminal `expired`
- **AND** SHALL create no membership even if a prior client inspection showed valid.
