## ADDED Requirements

### Requirement: Event Invitations SHALL Use Server-Issued Opaque Links
Wakeve SHALL issue event invitation links only through the backend using opaque, high-entropy, expiring, revocable tokens. The backend SHALL remain the sole authority for invitation validity and membership. A client SHALL NOT construct a redeemable invitation token or decide that a user joined.

#### Scenario: Organizer issues an invitation
- **GIVEN** an authenticated organizer is authorized to invite participants
- **WHEN** the organizer requests an invitation
- **THEN** the backend SHALL issue a canonical HTTPS link containing an opaque token
- **AND** SHALL persist protected lookup material and sanitized audit identity
- **AND** the client SHALL share the returned URL without deriving another token.

#### Scenario: Unauthorized actor attempts issuance
- **GIVEN** an authenticated user is not the event organizer or lacks invitation permission
- **WHEN** the user requests issue, rotation, or revocation for that event
- **THEN** the backend SHALL reject the request without creating or changing a token
- **AND** SHALL prevent cross-event IDOR by authorizing the actor against the server-loaded event.

### Requirement: Invitation Redemption SHALL Be Authenticated and Idempotent
Joining through an invitation SHALL require authenticated backend redemption with current authorization and event-policy checks. A stable operation ID and membership uniqueness SHALL make replay and concurrency idempotent.

#### Scenario: User redeems the same invitation twice
- **GIVEN** a valid invitation and authenticated eligible user
- **WHEN** the same redeem operation is replayed
- **THEN** the backend SHALL return the existing joined result
- **AND** SHALL retain exactly one event membership
- **AND** SHALL NOT duplicate downstream effects.

#### Scenario: Authentication handoff preserves intent
- **GIVEN** a logged-out user opens a valid invitation
- **WHEN** authentication completes for that user
- **THEN** Wakeve SHALL resume inspection but require an explicit redeem action
- **AND** the backend SHALL re-evaluate current invitation, event, recipient, and capacity policy
- **AND** authentication alone SHALL NOT create membership.
