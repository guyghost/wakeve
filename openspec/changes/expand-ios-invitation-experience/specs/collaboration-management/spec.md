## ADDED Requirements

### Requirement: Audience Invitation, Approval, Membership, RSVP, and Date Validation Axes SHALL Remain Independent
Wakeve MUST represent invitation delivery, guest approval, event membership, RSVP, and retained-date validation as separate typed axes for each audience identity. No value on one axis may infer or propagate a value on another axis. Counts and identities MUST derive from the same coherent repository snapshot.

For a non-member or invite/request-only identity, RSVP and date validation MUST be `NOT_APPLICABLE`. For an active member, a missing applicable RSVP or retained-date record MUST be `UNAVAILABLE`, not inferred as pending or not validated. Date validation MUST be `NOT_APPLICABLE` before a retained date exists.

#### Scenario: Server accepts an invitation request
- **GIVEN** a direct invitation recipient has outcome `SERVER_ACCEPTED(invitationId)`
- **WHEN** the Audience projection renders
- **THEN** it does not infer that delivery completed, approval was granted, membership exists, RSVP was accepted, or the retained date was validated
- **AND** each axis renders only its persisted typed state.

#### Scenario: Guest approval does not create membership
- **GIVEN** an approval request becomes `APPROVED(requestId)`
- **WHEN** the repository has not acknowledged membership redemption
- **THEN** membership remains `NON_MEMBER`
- **AND** RSVP and date validation remain `NOT_APPLICABLE`.

#### Scenario: Active member has missing RSVP data
- **GIVEN** an identity has `ACTIVE_MEMBER(memberId)`
- **AND** its applicable RSVP record is unavailable
- **WHEN** Audience computes the participant summary
- **THEN** RSVP is `UNAVAILABLE`
- **AND** Wakeve does not count the identity as pending, accepted, or declined.

### Requirement: Direct Invitation Batches SHALL Be Capability-Gated and Idempotent
Release-1 direct invitations MUST require an interactive organizer and a matching `DirectInviteCapability.READY(eventId, actorId, accessRevision, allowedEventStatuses)`. The canonical release-1 allowed event status set MUST be exactly `{DRAFT}` until the owning participant/invitation use case is explicitly expanded.

A direct invitation operation MUST use one batch id, one stable operation id, a non-empty deduplicated validated recipient set, and exactly one typed outcome for every requested recipient. Offline submission MAY queue the complete batch and show per-recipient `QUEUED_LOCAL`, but MUST NOT claim server acceptance or delivery. Retry MUST replay only retryable unresolved recipients without duplicating accepted recipients. Cancellation MUST affect unresolved recipients only.

The persisted recipient key MUST be an owner-issued protected pseudonymous identifier or a versioned keyed HMAC/digest, never a raw or reversibly normalized email address. Normalized recipient input MUST remain transient for validation/delivery. If delivery/retry requires persistence, the value MUST be encrypted/protected at rest, scoped to the operation, excluded from logs/analytics/UI identifiers, and deleted or irreversibly anonymized at retention expiry, event cascade deletion, or authenticated account erasure. Key material MUST remain inside the trusted owner boundary.

#### Scenario: Organizer submits a mixed-result direct invitation batch
- **GIVEN** a DRAFT event, a matching ready capability, and three validated recipients
- **WHEN** the owner acknowledges one accepted recipient, one invalid recipient, and one failed recipient
- **THEN** the stored outcome key set exactly matches the requested recipient key set
- **AND** each outcome remains independent
- **AND** Audience refreshes from one coherent repository snapshot.

#### Scenario: Organizer retries a partially successful batch
- **GIVEN** a prior batch contains one server-accepted recipient and one retryable failed recipient
- **WHEN** the organizer retries with the still-matching capability
- **THEN** only the retryable failed recipient is replayed under the existing batch/idempotency contract
- **AND** the accepted recipient is not submitted again.

#### Scenario: Organizer attempts a direct invite after DRAFT
- **GIVEN** an event is `POLLING`, `COMPARING`, `CONFIRMED`, `ORGANIZING`, or `FINALIZED`
- **WHEN** release-1 Audience computes direct invitation capability
- **THEN** the direct invite action is unavailable
- **AND** the iOS surface does not bypass the owner by writing participants directly.

#### Scenario: Offline direct invite is queued
- **GIVEN** a DRAFT event and a matching direct-invite capability
- **WHEN** the organizer submits a batch while offline
- **THEN** the full batch and stable operation identity are persisted for synchronization
- **AND** every recipient is shown as queued locally
- **AND** no recipient is shown as delivered, approved, joined, accepted, or date-validated.

#### Scenario: Direct invite batch is persisted without raw recipient identity
- **GIVEN** the organizer submits normalized recipient email input
- **WHEN** the owner persists the batch and per-recipient outcomes
- **THEN** `recipient_key` is a protected pseudonymous id or versioned keyed digest
- **AND** no raw or reversibly normalized email appears in the batch/outcome primary key, logs, analytics, or UI identifiers
- **AND** any delivery-only encrypted recipient value has an explicit retention expiry.

#### Scenario: Event deletion or account erasure removes protected recipient data
- **GIVEN** direct-invite batch/outcome data exists for an event or user
- **WHEN** eligible event cascade deletion, retention expiry, or authenticated account erasure completes
- **THEN** protected recipient keys and encrypted delivery values are deleted or irreversibly anonymized according to the existing authority
- **AND** no orphan PII remains
- **AND** idempotency/audit data retained by policy no longer identifies the recipient directly.

### Requirement: Audience Public Sharing SHALL Consume Only a Bound Secure Capability
Audience public-link sharing MUST consume `SecureShareCapability` supplied by `harden-event-invitation-links`. A ready capability MUST bind `eventId`, `actorId`, `accessRevision`, and `capabilityId` to an opaque server-issued payload. Every binding dimension MUST still match at share time.

The opaque payload MUST be forwarded unchanged to the secure share owner and held in memory only for that invocation. Wakeve MUST NOT parse, log, persist, cache, index, analyze, or convert it into a client-generated QR code. A client MUST NOT construct a token, redeemable URL, or membership decision. `HIDDEN`, `UNAVAILABLE`, `LOADING`, `FAILED`, or stale binding MUST expose no public share action.

Guest approval MUST remain `NOT_SUPPORTED` in release 1 unless the secure invitation dependency provides a separately reviewed online backend policy/request capability. Direct invitation and public-link sharing MUST remain distinct capabilities.

These prohibitions apply to the six new invitation-experience surfaces. Legacy invitation implementation and global cleanup outside them remain authoritative under the `harden-event-invitation-links` cutover, including its task 5.2; this change MUST NOT remove or rewrite unrelated legacy flows before that owner completes cutover.

#### Scenario: Secure share binding becomes stale
- **GIVEN** Audience has a ready secure capability
- **WHEN** the current event, actor, access revision, or capability id changes
- **THEN** the capability immediately becomes unavailable with a stale-binding reason
- **AND** the public share action disappears
- **AND** no local fallback link or QR code is offered.

#### Scenario: Secure invitation dependency is not installed
- **GIVEN** `harden-event-invitation-links` has not supplied a ready bound capability
- **WHEN** an organizer opens Audience
- **THEN** public share is hidden or honestly unavailable
- **AND** a separately authorized DRAFT direct-invite capability MAY remain available
- **AND** the client does not simulate public sharing or guest approval.

#### Scenario: Organizer cancels the system share sheet
- **GIVEN** a valid bound server payload was passed to the system share sheet
- **WHEN** the organizer cancels sharing
- **THEN** Wakeve does not rotate, revoke, persist, or regenerate the invitation
- **AND** membership and audience axes remain unchanged.
