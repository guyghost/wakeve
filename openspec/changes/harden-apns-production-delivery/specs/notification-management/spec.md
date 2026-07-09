## MODIFIED Requirements

### Requirement: Notification Service
The system SHALL provide a unified notification service for cross-platform push notifications. The iOS permission prompt SHALL only be requested in direct response to an explicit user action; app launch or activation MAY inspect the current permission status but MUST NOT display the prompt.

**ID**: `notif-001`

#### Scenario: App launch inspects permission without prompting
- **GIVEN** the iOS application launches or becomes active
- **WHEN** Wakeve evaluates notification readiness
- **THEN** the system SHALL read the current notification authorization status without calling the permission request API
- **AND** an already authorized, provisional, or ephemeral installation MAY continue APNs registration without showing a new prompt
- **AND** a `notDetermined` installation SHALL remain pending until the user explicitly chooses to enable notifications
- **AND** a denied installation SHALL expose a user action to open system settings without automatically requesting again.

#### Scenario: Register device for push notifications after explicit consent
- **GIVEN** a user is authenticated
- **AND** notification authorization is already allowed or the user explicitly chooses to enable notifications while status is `notDetermined`
- **WHEN** the deterministic iOS registration workflow runs
- **THEN** the system SHALL request platform permission only when required by that explicit action
- **AND** retrieve an FCM token on Android or APNs token on iOS
- **AND** register the token and installation identity with the backend via an authenticated notification registration endpoint
- **AND** persist a reviewable registration state
- **AND** surface or retry registration failures according to the modeled policy instead of treating them as success.

#### Scenario: Receive push notification in foreground
- **GIVEN** the app is in foreground
- **WHEN** a push notification is received
- **THEN** the system SHALL:
  - Display in-app banner/snackbar
  - Play notification sound (if enabled)
  - Update notification badge
  - Store in local notification history

#### Scenario: Receive push notification in background
- **GIVEN** the app is in background or killed
- **WHEN** a push notification is received
- **THEN** the system SHALL:
  - Display system notification
  - Play notification sound
  - Update badge count
  - On tap: open relevant screen

#### Scenario: Notification types
- **GIVEN** various event activities occur
- **WHEN** notifications are triggered
- **THEN** the system SHALL support types:
  - `EVENT_INVITE`: User invited to event
  - `VOTE_REMINDER`: Poll deadline approaching
  - `DATE_CONFIRMED`: Event date confirmed
  - `NEW_SCENARIO`: New scenario proposed
  - `SCENARIO_SELECTED`: Final scenario selected
  - `NEW_COMMENT`: New comment on event
  - `MENTION`: User mentioned in comment
  - `MEETING_REMINDER`: Meeting starting soon
  - `PAYMENT_DUE`: Settlement pending

## ADDED Requirements

### Requirement: Notification Outbox Ownership and Delivery Authority
The local SQLDelight datastore and `DatabaseEventRepository` SHALL exclusively own `confirmation_effect_outbox`; it contains no provider recipient, delivery, or calendar artifact. The backend datastore and notification backend SHALL exclusively own `domain_event_ingestion`, `notification_recipient`, `notification_delivery`, and calendar fan-out projections. Wakeve MUST NOT claim atomicity across those datastores.

`domainEventId` SHALL identify the immutable domain decision. `effectKey = (domainEventId, effectType, schemaVersion)` SHALL identify its one logical effect envelope. `recipientKey = (effectKey, participantId, channel)` SHALL identify intended-recipient resolution. `deliveryKey = (recipientKey, installationId, provider)` SHALL identify one provider delivery. `calendarArtifactKey = (effectKey, participantId, calendarProvider)` SHALL identify one participant calendar artifact. The backend ingestion transaction SHALL idempotently persist the domain event receipt and all currently resolvable recipient projections; provider and calendar I/O occur only after commit. Exactly one unique `delivery_authority` SHALL be permitted to send a `deliveryKey` during migration.

#### Scenario: Backend acknowledges an effect envelope
- **GIVEN** a local confirmation persisted one domain-effect envelope
- **WHEN** the backend idempotently accepts its `domainEventId` and `effectKey`
- **THEN** one backend ingestion transaction SHALL persist the ingestion acknowledgement and currently resolvable `notification_recipient` projections
- **AND** the local producer MAY mark decision synchronization acknowledged only from that durable acknowledgement
- **AND** deliveries and calendar artifacts SHALL be derived after the ingestion transaction commits
- **AND** acknowledgement SHALL NOT imply recipient resolution or provider acceptance.

#### Scenario: Intended recipient has no target
- **GIVEN** a participant is an intended recipient with no eligible installation
- **WHEN** the backend resolves the envelope
- **THEN** it SHALL retain a `notification_recipient` in `pendingTarget` with attempts, next-attempt time, and bounded expiry
- **AND** later registration or membership reconciliation before expiry SHALL resume fan-out idempotently from `recipientKey`
- **AND** expiry SHALL persist terminal `targetExpired` acknowledgement and SHALL prevent resurrection without a new effect
- **AND** no target or successful delivery SHALL be fabricated by the client.

#### Scenario: Shadow-write cutover avoids duplicate sends
- **GIVEN** legacy and new records are shadow-written
- **WHEN** a worker selects sendable work
- **THEN** a uniqueness constraint SHALL permit only one `delivery_authority` for each `deliveryKey`
- **AND** rollback SHALL pause leases and transfer authority at a recorded checkpoint before another worker sends
- **AND** legacy confirmations SHALL be classified without retroactively sending notifications or calendar artifacts.

### Requirement: Decision Sync and Effect Dispatch Status Separation
Wakeve SHALL persist `decisionSyncStatus` independently from `effectDispatchStatus`. Decision synchronization, recipient resolution, provider delivery, and calendar fan-out SHALL use separate retry policies, attempts, expirations, acknowledgements, and terminal outcomes. Effect retry or failure MUST NOT retry, revert, or reinterpret the confirmed decision.

#### Scenario: Decision is synced while delivery remains pending
- **GIVEN** the backend acknowledged the confirmed decision envelope
- **AND** a notification recipient awaits a token
- **WHEN** status is projected
- **THEN** `decisionSyncStatus` SHALL be `acknowledged`
- **AND** `effectDispatchStatus` SHALL remain pending
- **AND** Wakeve SHALL NOT claim that participants were notified.

#### Scenario: Delivery fails after decision acknowledgement
- **GIVEN** `decisionSyncStatus` is `acknowledged`
- **WHEN** one `deliveryKey` reaches `retryExhausted`, `expired`, or another terminal provider outcome
- **THEN** only the corresponding delivery and aggregate `effectDispatchStatus` SHALL change
- **AND** the domain decision acknowledgement SHALL remain unchanged
- **AND** no decision-sync retry SHALL be scheduled.

### Requirement: Deterministic iOS Notification Registration Lifecycle
Wakeve MUST model iOS notification registration as a deterministic state machine before implementing production behavior. The reviewed XState model under `/models` SHALL be the source of truth for permission checks, explicit permission requests, APNs registration, authenticated backend association, retries, cancellation, configuration failures, and logout unregistration. SwiftUI views and service callbacks MUST emit typed events and MUST NOT infer or perform unmodeled state transitions.

#### Scenario: Explicit enable action drives the permission transition
- **GIVEN** the registration state is `notDetermined`
- **WHEN** the user explicitly selects the action to enable notifications
- **THEN** the state machine transitions to permission requesting
- **AND** invokes the iOS authorization prompt exactly from that transition
- **AND** transitions to APNs registration only after an allowed permission result
- **AND** transitions to a denied or error state for the corresponding result.

#### Scenario: Backend token registration fails temporarily
- **GIVEN** APNs returned a token for an authenticated installation
- **WHEN** the backend registration request fails with a transient network or server error
- **THEN** the registration MUST NOT be marked registered
- **AND** the failure class, attempt, and next retry time are retained
- **AND** a bounded retry with jitter is scheduled
- **AND** the UI can expose a truthful pending or retry state.

#### Scenario: APNs token arrives before authentication
- **GIVEN** notification authorization is allowed and APNs returns a device token
- **AND** no authenticated Wakeve session is available
- **WHEN** the registration machine processes the token
- **THEN** it waits for authenticated context without making an anonymous backend registration call
- **AND** resumes backend association when authentication becomes available
- **AND** does not log the raw token.

#### Scenario: Logout unregisters before credentials are cleared
- **GIVEN** the current installation has a confirmed backend registration
- **WHEN** the user requests logout
- **THEN** Wakeve SHALL issue and await an authenticated idempotent unregistration for that installation
- **AND** retain the JWT while the unregistration is pending or retryable
- **AND** allow the authentication workflow to clear credentials only after the notification machine reaches `unregistered`
- **AND** leave registrations belonging to other installations unchanged.

#### Scenario: Registration is cancelled before external work starts
- **GIVEN** notification registration is waiting for explicit consent, authentication, or a retry
- **WHEN** the user cancels the pending flow
- **THEN** the machine reaches a modeled cancelled terminal state
- **AND** no implicit permission request or backend association is performed.

### Requirement: Multi-Device Notification Registration
Wakeve MUST store notification registration per installation rather than per `(user, platform)`. Each active installation SHALL have a stable installation identifier, platform, APNs environment, topic, current token, lifecycle timestamps, and invalidation state. Registering, rotating, invalidating, or removing one installation MUST NOT overwrite or remove another installation owned by the same user.

#### Scenario: User registers two iOS devices
- **GIVEN** the same authenticated user has two distinct iOS installations
- **WHEN** both installations register valid production APNs tokens
- **THEN** the backend retains two active registration records
- **AND** a notification eligible for that user creates one idempotent delivery per active installation
- **AND** neither token replaces the other.

#### Scenario: APNs rotates a token for one installation
- **GIVEN** an installation already has an active APNs registration
- **WHEN** iOS supplies a replacement token for the same installation
- **THEN** Wakeve atomically updates that installation
- **AND** preserves registrations for all other installations
- **AND** no future delivery targets the superseded token.

#### Scenario: APNs reports an inactive token
- **GIVEN** a delivery targets one of several active installations
- **WHEN** APNs returns `BadDeviceToken`, `DeviceTokenNotForTopic`, `ExpiredToken`, or `Unregistered`
- **THEN** Wakeve marks only that installation invalid
- **AND** does not retry the same token
- **AND** continues to use the user's other active installations.

#### Scenario: User logs out on one device
- **GIVEN** a user is registered on multiple devices
- **WHEN** logout unregistration succeeds on one installation
- **THEN** only that installation is no longer eligible for new deliveries
- **AND** other device registrations remain active.

### Requirement: Durable Idempotent Notification Delivery
Within the backend datastore only, Wakeve MUST persist each logical notification and its per-installation `notification_delivery` records transactionally before provider I/O. This backend transaction is downstream of, and not atomic with, local SQLDelight envelope persistence. Delivery records SHALL use stable `deliveryKey` idempotency, a unique recipient/installation/provider constraint, durable states, leases, attempt counts, next-attempt timestamps, expiration, provider response metadata, and terminal outcomes. A process crash or repeated producer event MUST NOT lose the notification or create a duplicate logical delivery.

#### Scenario: Domain event is enqueued twice
- **GIVEN** a workflow event produces a stable notification idempotency key
- **WHEN** the producer submits the same event more than once
- **THEN** Wakeve returns or reuses the same logical notification
- **AND** creates at most one delivery for each active installation
- **AND** does not send duplicate work because of the repeated enqueue.

#### Scenario: Worker crashes after acquiring a delivery
- **GIVEN** a queued delivery has a time-bounded worker lease
- **WHEN** the worker stops before persisting a terminal provider result
- **THEN** the durable delivery remains recoverable
- **AND** becomes eligible after the lease expires
- **AND** resumes with the same delivery identity and idempotency key.

#### Scenario: Quiet hours defer a non-urgent delivery
- **GIVEN** a non-urgent notification is allowed by user preferences but falls within quiet hours
- **WHEN** the delivery policy is evaluated
- **THEN** the delivery transitions to a durable deferred state
- **AND** stores the next eligible time
- **AND** re-evaluates policy and expiration before provider send.

#### Scenario: No active token is available yet
- **GIVEN** a valid notification exists for a user with no active installation token
- **WHEN** delivery policy is evaluated
- **THEN** the notification remains durably awaiting a token until its expiration
- **AND** a later token registration causes policy to be evaluated again
- **AND** the absence of a token is not recorded as a successful send.

#### Scenario: APNs accepts a delivery
- **GIVEN** an APNs delivery is queued for an active installation
- **WHEN** APNs returns HTTP `200`
- **THEN** that delivery transitions to `acceptedByAPNs`
- **AND** records the APNs identifier and acceptance timestamp
- **AND** only then may the logical notification receive a non-null `sent_at`
- **AND** Wakeve describes the result as accepted by APNs rather than delivered or read on the device.

#### Scenario: All provider attempts fail
- **GIVEN** one or more provider calls fail before any APNs HTTP `200`
- **WHEN** the retry policy processes those failures
- **THEN** `sent_at` remains null
- **AND** each failure is persisted as a retryable, blocked, unknown, or terminal state
- **AND** the notification API MUST NOT report provider acceptance as successful.

#### Scenario: Transport outcome is unknown
- **GIVEN** an APNs request may have been written but no response was received
- **WHEN** the transport reports an ambiguous outcome
- **THEN** the delivery transitions to `unknownOutcome`
- **AND** remains without `sent_at` or `accepted_at`
- **AND** any allowed retry uses the same logical identity and a bounded retry policy
- **AND** Wakeve does not claim exactly-once delivery to iOS.

#### Scenario: Retry budget or business validity ends
- **GIVEN** a retryable delivery has a maximum attempt count and expiration
- **WHEN** its retry budget is exhausted or its expiry time is reached
- **THEN** it transitions respectively to `retryExhausted` or `expired`
- **AND** no further provider attempt is made.

#### Scenario: Queued delivery is cancelled
- **GIVEN** a delivery is deferred, awaiting a token, queued, or waiting to retry
- **WHEN** an authorized business or operator cancellation is recorded
- **THEN** the delivery transitions to `cancelled`
- **AND** no new provider attempt starts
- **AND** a request already possibly written is classified as `unknownOutcome` instead of falsely cancelled.

### Requirement: Production APNs Provider
The Wakeve backend MUST provide a real APNs HTTP/2 and TLS transport using token-based ES256 authentication. It MUST select the sandbox or production endpoint from explicit validated configuration, send the required topic, push type, APNs identifier, expiration and compatible priority, parse every response, and map the result to the deterministic delivery model. TestFlight and App Store builds MUST use the production APNs environment.

#### Scenario: Provider starts with valid production configuration
- **GIVEN** the production runtime contains valid APNs Key ID, Team ID, private authentication key, bundle topic, and production environment
- **WHEN** the provider initializes
- **THEN** it validates the configuration without logging secrets
- **AND** signs a time-bounded ES256 provider JWT
- **AND** connects to the production APNs endpoint over HTTP/2 and TLS
- **AND** reports ready only after configuration validation succeeds.

#### Scenario: Production configuration is missing or points to sandbox
- **GIVEN** APNs delivery is enabled in a production runtime
- **WHEN** a required credential, topic, private key, or production environment value is absent or invalid
- **THEN** the provider fails closed
- **AND** readiness reports not ready
- **AND** deliveries enter a visible provider-auth/configuration blocked state
- **AND** no mock, development endpoint, or success response is used as fallback.

#### Scenario: APNs returns a non-retryable device response
- **GIVEN** the provider receives a token-invalidating APNs response
- **WHEN** the response classifier evaluates its HTTP status and reason
- **THEN** the delivery transitions to `invalidToken`
- **AND** the targeted installation is invalidated
- **AND** the identical request is not retried.

#### Scenario: APNs rejects the request contract
- **GIVEN** APNs returns a non-retryable payload, header, path, method, size, topic, or push-type error
- **WHEN** the response is classified
- **THEN** the delivery transitions to `rejectedPayload`
- **AND** the provider records only sanitized diagnostics
- **AND** raises an operational signal instead of retrying the identical request.

#### Scenario: APNs is transiently unavailable or throttles a token
- **GIVEN** APNs returns a retryable `429`, `500`, `503`, or equivalent transient transport outcome
- **WHEN** the response is classified
- **THEN** the delivery transitions to a durable retry state
- **AND** respects `Retry-After` when present
- **AND** otherwise applies the configured bounded backoff with jitter
- **AND** never retries after expiration.

#### Scenario: Provider authentication is rejected
- **GIVEN** APNs rejects the provider credential or token
- **WHEN** the failure is not resolved by the single allowed expired-token refresh
- **THEN** the provider opens a visible authentication block
- **AND** pauses affected deliveries without consuming their per-message retry budget
- **AND** emits an operator alert with no private key, JWT, or device token.

#### Scenario: Secrets and device tokens are observed operationally
- **WHEN** APNs registration or delivery emits logs, metrics, traces, errors, screenshots, or test evidence
- **THEN** private keys, JWTs, raw device tokens, and private event payload content MUST be absent
- **AND** diagnostics use sanitized registration, notification, delivery, and APNs request identifiers.

### Requirement: APNs Production Readiness Evidence
Wakeve MUST follow tests-first verification and MUST NOT declare iOS push production-ready until model, unit, integration, migration, security, signing, real-device sandbox, and TestFlight production evidence all pass. Evidence SHALL distinguish provider acceptance from observation on the device and SHALL contain no secret or private event content.

#### Scenario: Implementation begins after model review
- **GIVEN** this OpenSpec change is approved
- **WHEN** implementation work starts
- **THEN** the two `/models` XState machines and their transition tests are created and reviewed first
- **AND** failing iOS/shared/server tests are recorded before production behavior is changed
- **AND** no unmodeled transition is added during implementation.

#### Scenario: Sandbox device verification succeeds
- **GIVEN** the provider and iOS registration implementation pass automated tests
- **WHEN** the flow is exercised on a real device in the APNs sandbox environment
- **THEN** permission, registration, foreground, background, terminated-app, deep-link, retry, and token invalidation cases are evidenced
- **AND** Wakeve delivery identifiers are correlated with sanitized APNs development evidence.

#### Scenario: TestFlight production verification succeeds
- **GIVEN** a signed archived build has the production APNs entitlement and the backend uses production APNs configuration
- **WHEN** the build is distributed through TestFlight and tested on real devices
- **THEN** at least invitation and confirmation or reminder notifications are verified in foreground, background, and terminated-app states
- **AND** quiet hours, two devices, token refresh, deep-link access control, logout, and absence of post-logout delivery are evidenced
- **AND** each observed result is correlated with its persisted terminal or pending state.

#### Scenario: A required production proof is missing
- **GIVEN** any model, automated test, migration, secret, entitlement, sandbox-device, or TestFlight gate lacks passing evidence
- **WHEN** release readiness is evaluated
- **THEN** Wakeve MUST report iOS APNs as not ready for production
- **AND** MUST NOT infer readiness from source inspection, a mock sender, or an APNs HTTP `200` alone.
