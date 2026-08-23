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
The local SQLDelight datastore and `DatabaseEventRepository` SHALL exclusively own `confirmation_effect_outbox`; it contains no provider recipient, delivery, or calendar artifact and SHALL NOT be reused as a server outbox. The backend datastore and notification backend SHALL exclusively own `domain_event_ingestion`, `notification_logical`, `notification_recipient`, `notification_delivery`, and durable calendar artifacts. Wakeve MUST NOT claim atomicity across those datastores.

`domainEventId` SHALL identify the immutable domain decision. New writes SHALL use the injective length-prefix serialization `effectKey = ek2.<len(domainEventId)>:<domainEventId><len(effectType)>:<effectType>.v<schemaVersion>`, where `schemaVersion` is a positive safe integer and source components are non-empty. `recipientKey`, `deliveryKey` and `calendarArtifactKey` SHALL use the corresponding `rk2`, `dk2` and `ck2` length-prefix tuples. Delimiters inside an existing domain identity remain data and cannot collide with tuple boundaries.

During migration, the sole accepted historical local confirmation format SHALL be `poll-date-confirmed:<eventId>:<slotId>:v1:confirmation`. Its mapper SHALL receive the authoritative typed local tuple containing `legacyVersion=1`, `eventId`, `slotId`, the stored `domainEventId`, and the stored `effectKey`; it SHALL reconstruct and compare both historical strings exactly and SHALL NOT parse the string to recover components. The canonical domain identity SHALL be the injective length-prefix `pdc2(eventId,slotId)` tuple and its canonical effect identity SHALL be `ek2(pdc2(...),confirmation,v1)`. A durable migration record SHALL bind the historical effect key, injective source tuple and canonical identities. Exact replay SHALL return the same mapping idempotently. Unknown formats or versions, malformed components, any key mismatch, corrupt mapping record, or reuse of the same legacy key by a different source tuple SHALL be quarantined without ingestion or automatic merge. All new writes SHALL remain `canonical-v2` only.

`installationId` SHALL identify the stable app installation, while `registrationId` SHALL identify one historical account/token/scope association for it. `deliveryKey` SHALL bind `(recipientKey, registrationId, provider)`, so a delivery can never be retargeted when that installation changes account. When registrations are already resolved, the backend ingestion transaction SHALL idempotently persist the domain event receipt, logical notification, recipient projections, and exactly one delivery per distinct registration before provider I/O. A pending recipient SHALL persist without a delivery and later fan out through a separate receipt-fenced transaction. Exactly one unique `delivery_authority` from the closed set `legacy | outbox-v2` SHALL be permitted to send a `deliveryKey` during migration.

`notificationDeliveryMachine` SHALL be the single provider-delivery transition authority. Its normative durable states SHALL include `policyCheck`, `suppressed`, `deferredQuietHours`, `awaitingToken`, `queued`, `auth`, `sending`, `retry`, `unknownOutcome`, `providerAuthBlocked`, `accepted`, `invalidToken`, `rejectedPayload`, `expired`, `retryExhausted`, and `cancelled`. Provider terminal status changes SHALL pass through an explicitly emitted exact durable checkpoint; no historical direct-result event or checkpoint wrapper may terminalize a delivery.

#### Scenario: Backend acknowledges an effect envelope
- **GIVEN** a local confirmation persisted one domain-effect envelope
- **WHEN** the backend idempotently accepts its `domainEventId` and `effectKey`
- **THEN** one backend ingestion transaction SHALL persist `domain_event_ingestion`, `notification_logical`, currently resolvable `notification_recipient` projections and, for resolved registrations, their idempotent `notification_delivery` rows
- **AND** the local producer MAY mark decision synchronization acknowledged only from that durable acknowledgement
- **AND** unresolved recipients SHALL contain no fabricated delivery and SHALL use a later receipt-fenced fan-out transaction
- **AND** provider and calendar I/O SHALL begin only after the owning backend transaction commits
- **AND** acknowledgement SHALL NOT imply recipient resolution or provider acceptance.

#### Scenario: Historical poll confirmation is mapped without parsing
- **GIVEN** the local source row authoritatively provides `eventId`, `slotId`, its stored `domainEventId`, its stored `effectKey`, and legacy version 1
- **WHEN** both stored values exactly equal `poll-date-confirmed:<eventId>:<slotId>:v1` and `poll-date-confirmed:<eventId>:<slotId>:v1:confirmation`
- **THEN** migration SHALL map the source tuple to injective `pdc2` and `ek2` identities and persist their binding
- **AND** replay of that exact binding SHALL be idempotent
- **AND** delimiter-bearing IDs SHALL remain distinct in v2 even where their historical strings collide
- **AND** a different tuple claiming an already-bound historical key, an unknown version, or any mismatch SHALL be quarantined without heuristic parsing, ingestion, or merge.

#### Scenario: Intended recipient has no target
- **GIVEN** a participant is an intended recipient with no eligible installation
- **WHEN** the backend resolves the envelope
- **THEN** it SHALL retain a `notification_recipient` in `pendingTarget` with attempts, next-attempt time, and bounded expiry
- **AND** its monotone logical clock, retry, fan-out, expiry and exhaustion SHALL use distinct durable checkpoints
- **AND** later registration or membership reconciliation before expiry SHALL resume fan-out idempotently from `recipientKey`
- **AND** expiry SHALL persist terminal `targetExpired` acknowledgement and SHALL prevent resurrection without a new effect
- **AND** no target or successful delivery SHALL be fabricated by the client.

#### Scenario: Recipient targeting resumes exactly once
- **GIVEN** a durable `notification_recipient(status=pendingTarget)` before its expiry
- **WHEN** registration reconciliation resolves one or more eligible registrations
- **THEN** the recipient SHALL transition durably to `targeted`
- **AND** fan-out SHALL create exactly one immutable `deliveryKey` per distinct `registrationId`
- **AND** a resolver SHALL first durably acquire the exact pending checkpoint under a holder, lease version and fencing token
- **AND** the fan-out transaction SHALL carry exact `transactionReceiptId`, effect ID, checkpoint revision, holder, lease version and fence before `targeted` commits
- **AND** XState restore SHALL emit nothing until the exact holder explicitly requests the effect
- **AND** an expired resolver lease SHALL require a strictly newer lease/fence, while stale, foreign, ACK-before-emission and duplicate acknowledgements remain inert
- **AND** duplicate reconciliation or restart SHALL NOT duplicate or retarget those deliveries
- **AND** a durable `targetExpired` recipient SHALL ignore later registrations and SHALL NOT resurrect without a new `effectKey`.

#### Scenario: Provider result waits for a durable checkpoint
- **GIVEN** a provider worker has observed an APNs result
- **WHEN** the delivery result has not yet been durably acknowledged against its exact `effectId`, checkpoint revision, authority, fencing token, lease holder and lease version
- **THEN** the model SHALL retain the prior durable delivery status
- **AND** crash recovery SHALL explicitly replay the idempotent persistence effect because XState restore does not replay entry actions
- **AND** acknowledgement before the exact persistence effect was explicitly emitted SHALL be ignored
- **AND** only the exact durable acknowledgement SHALL commit acceptance, invalidation, blocking, terminal failure or the next retry timestamp
- **AND** a stale acknowledgement SHALL be inert.

#### Scenario: Delivery policy and schedule remain expirable
- **GIVEN** a delivery is in policy check, quiet-hours deferral, token wait, queue, auth, sending, retry, unknown outcome, or provider-auth block
- **WHEN** a revision-correlated monotone `CLOCK_TICK` reaches business expiry
- **THEN** the machine SHALL stage a durable typed expiry checkpoint
- **AND** a new provider lease SHALL be rejected unless the persisted logical clock is strictly before business expiry
- **AND** `retry` SHALL remain nonterminal and SHALL leave only after `RETRY_DUE` when the persisted clock has reached its schedule, or through durable expiry
- **AND** `providerAuthBlocked` SHALL be reached only after its exact persistence effect and ACK, and expiry from that state SHALL require a separate exact expiry effect and ACK
- **AND** an event-local future timestamp SHALL NOT bypass the persisted clock.

#### Scenario: Transport may have written
- **GIVEN** a correlated provider transport reports `mayHaveWritten`
- **WHEN** the canonical classifier stages its result
- **THEN** it SHALL stage durable `unknownOutcome`, never `unknownTerminal` or acceptance
- **AND** only its exact emitted-and-acknowledged checkpoint SHALL enter `unknownOutcome`
- **AND** bounded reconciliation retry MAY reuse the same `deliveryKey` and stable APNs id under the exact current lease and attempt budget
- **AND** expiry or budget exhaustion SHALL still pass through its own exact durable checkpoint.

#### Scenario: Provider token expires during one send
- **GIVEN** a correlated APNs send observes `ExpiredProviderToken`
- **WHEN** the per-send refresh coordinator is unused
- **THEN** the model SHALL stage and durably acknowledge exactly one `refreshAuth` checkpoint before returning to auth
- **AND** a second `ExpiredProviderToken` for that same send SHALL stage `providerAuthBlocked`
- **AND** the exact ACK SHALL block the process circuit for that credential version without consuming delivery retry budget
- **AND** only a validated different credential version SHALL clear the circuit.

#### Scenario: Calendar failure retries independently
- **GIVEN** a calendar artifact owns its own correlation, attempt, schedule, expiry and durable holder/version/fencing lease
- **WHEN** its closed provider event reports a retryable failure
- **THEN** it SHALL stage an exact calendar checkpoint without changing provider-delivery state
- **AND** exact ACK SHALL enter nonterminal calendar `retry`, which waits for its own monotone clock and `CALENDAR_RETRY_DUE`
- **AND** restore or lease expiry SHALL require explicit exact-holder recovery with a strictly newer lease/fence
- **AND** stale, foreign or duplicate callbacks SHALL be inert and calendar expiry SHALL NOT inherit provider retry state.

#### Scenario: Retry metadata is closed and bounded
- **GIVEN** a provider result is classified as retryable
- **WHEN** its reason or `Retry-After` metadata is normalized
- **THEN** the reason SHALL belong to the closed provider-reason vocabulary
- **AND** the next attempt SHALL be a safe integer strictly after the authoritative logical time, no more than 300 seconds later, and strictly before delivery expiry
- **AND** unknown, past or over-bound metadata SHALL fail closed to typed `unknownOutcome`, while expiry-reaching metadata SHALL use typed `expired`; neither may create an ambiguous schedule.
- **AND** absent `Retry-After`, including a `500` or `503` without reason, SHALL use injected overflow-safe full jitter in `[1,300]` within the attempt budget.

#### Scenario: Shadow-write cutover avoids duplicate sends
- **GIVEN** legacy and new records are shadow-written
- **WHEN** a worker selects sendable work
- **THEN** a uniqueness constraint SHALL permit only one `delivery_authority` for each `deliveryKey`
- **AND** cutover SHALL acquire a durable unexpired recovery lease and require exact fenced `pauseLegacy`, `reconcileLegacy`, and `commitOutboxV2` effects
- **AND** rollback SHALL acquire a durable unexpired recovery lease and require exact fenced `pauseOutboxV2`, `reconcileOutboxV2`, and `commitLegacy` effects
- **AND** every step SHALL require explicit effect emission before an exact ACK matching effect ID, checkpoint revision, holder, lease version and fencing token
- **AND** the previous authority SHALL remain authoritative until the exact transfer acknowledgement is durable
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
Wakeve MUST store a stable backend-owned `device_installation(installationId)` separately from historical `device_registration(registrationId)` associations, rather than storing one mutable row per `(user, platform)`. SQLDelight local MUST NOT create or replicate either table. `platform` SHALL be canonical on `device_installation`; a registration inherits it through its FK and MUST NOT carry a divergent platform value. A registration SHALL contain its installation FK, authenticated user, APNs environment, topic, token ciphertext encrypted at rest, token hash, lifecycle timestamps, `invalid_reason`, and distinct typed `unregistered_reason` (`ACCOUNT_CHANGED`, `SCOPE_CHANGED`, `LOGOUT`, `USER_REQUESTED`, or `ADMIN_REVOKED`), with state `ACTIVE`, `INVALID`, or `UNREGISTERED`. Ordinary registration reads, diagnostics, errors and rollback projections MUST NOT expose token or ciphertext; only a separate provider port MAY decrypt the token in memory for the duration of the provider call. That port SHALL return a fixed tokenless result (or encapsulate provider send) and MUST NOT return the callback's generic result, so the callback cannot make the raw token escape its dynamic scope. Missing durable path, token-encryption key or legacy-identity HMAC key SHALL fail closed before datastore access or mutation. The durable path SHALL be absolute, local and free of symbolic-link components; relative paths, SQLite memory identifiers and URI paths SHALL be rejected. On POSIX filesystems its parent and database file SHALL be created and verified owner-only. The durable factory SHALL accept an explicitly validated immutable configuration so tests and composed callers do not depend on ambient process environment; the service-loaded production adapter MAY resolve ambient configuration before constructing that factory. At most one registration per installation MAY be `ACTIVE`; the backend SHALL enforce that invariant transactionally and with datastore constraints, including lifecycle status/timestamp/reason consistency against direct SQL writes. `activeRegistration(installationId)` is the sole active association and `registrationHistory(installationId)` retains closed associations. Registering, rotating, invalidating, or removing one registration MUST NOT overwrite or remove another installation or historical registration owned by the same user. Store/provider close SHALL serialize with registration and scoped decryption: a close waits for an active operation and destroys key material only after that operation completes.

#### Scenario: User registers two iOS devices
- **GIVEN** the same authenticated user has two distinct iOS installations
- **WHEN** both installations register valid production APNs tokens
- **THEN** the backend retains two active registration records
- **AND** a notification eligible for that user creates one idempotent delivery per eligible active `registrationId`
- **AND** neither token replaces the other.

#### Scenario: Registration token remains protected outside the provider port
- **GIVEN** the backend accepts a raw APNs token through the authenticated registration command
- **WHEN** the store is closed and reopened or registration and rollback read-models are inspected
- **THEN** the durable database contains only encrypted token material and a token hash
- **AND** no ordinary DTO, diagnostic, exception, or `toString` output exposes token or ciphertext
- **AND** only the provider token port may decrypt the token in memory for the selected `registrationId`.

#### Scenario: Provider callback cannot return the token
- **GIVEN** a provider token port invokes a callback with the decrypted token
- **WHEN** the callback attempts to return that token as its result
- **THEN** the port returns only a fixed tokenless receipt or completion result
- **AND** the raw token cannot escape through a generic callback return value.

#### Scenario: Closing waits for registration and provider operations
- **GIVEN** a registration transaction or scoped provider decryption is in progress
- **WHEN** another thread closes the store or provider port
- **THEN** close waits until the active operation has completed
- **AND** key material is not destroyed while ciphertext can still be committed or decrypted
- **AND** closing and reopening exposes one coherent durable registration state.

#### Scenario: Reentrant provider close cannot destroy an in-use key
- **GIVEN** a provider token callback is executing on one thread with decrypted material in scope
- **WHEN** that same callback calls `close()` reentrantly
- **THEN** close is explicitly rejected or deferred until the callback has completely returned
- **AND** key material remains usable for the remainder of the callback
- **AND** the callback produces one coherent tokenless completion result
- **AND** if reentrant close reports success, the port is closed immediately after callback return and before any later external close
- **AND** if reentrant close is rejected, the port remains usable until a subsequent external close
- **AND** that subsequent external close closes the port safely and is idempotent.

#### Scenario: Lifecycle invariants are enforced by the datastore
- **GIVEN** a direct SQL client bypasses the Kotlin store API
- **WHEN** it attempts an `ACTIVE`, `INVALID`, or `UNREGISTERED` row with contradictory timestamps or reasons
- **THEN** SQL constraints reject the write
- **AND** invalidation and unregistration reasons remain members of their closed enums.

#### Scenario: Unsafe durable paths fail closed
- **GIVEN** datastore configuration supplies a relative path, `:memory:`, SQLite file URI, or a path containing a symbolic-link component
- **WHEN** the durable configuration is resolved or the store is opened
- **THEN** Wakeve rejects it before datastore access
- **AND** on POSIX filesystems a newly created parent is owner `rwx` only and the database file is owner `rw` only
- **AND** a pre-existing broad/shared parent is rejected without changing its permissions
- **AND** a pre-existing broad database file under an owner-only parent is rejected or normalized to owner `rw` before use.

#### Scenario: Path swaps after configuration resolution fail closed
- **GIVEN** an absolute non-symbolic database path has passed configuration resolution
- **WHEN** an intermediate component or the final database file is replaced by a symbolic link before factory open
- **THEN** factory open rejects the path after revalidation
- **AND** it does not chmod, create SQLite artifacts in, or otherwise mutate the symbolic-link target.

#### Scenario: Installation platform cannot drift
- **GIVEN** an installation was created with canonical platform `IOS`
- **WHEN** a later registration request presents another platform for the same `installationId`
- **THEN** the request fails without changing the installation, active association, or registration history.

#### Scenario: APNs rotates a token for one installation
- **GIVEN** an installation already has an active APNs registration
- **WHEN** iOS supplies a replacement token for the same installation
- **THEN** Wakeve atomically updates the same active `registrationId` only when account, environment and topic match
- **AND** preserves registrations for all other installations
- **AND** no future delivery targets the superseded token.

#### Scenario: APNs reports an inactive token
- **GIVEN** a delivery targets one of several active registrations
- **WHEN** APNs returns `BadDeviceToken`, `DeviceTokenNotForTopic`, `ExpiredToken`, or `Unregistered`
- **THEN** Wakeve marks only that `registrationId` invalid
- **AND** does not retry the same token
- **AND** continues to use the user's other active registrations.

#### Scenario: User logs out on one device
- **GIVEN** a user is registered on multiple devices
- **WHEN** logout unregistration succeeds for one active `registrationId`
- **THEN** only that registration is no longer eligible for new deliveries
- **AND** other device registrations remain active.

#### Scenario: An installation changes account
- **GIVEN** one installation has an `ACTIVE` registration for account A
- **WHEN** an authenticated request associates that installation with account B
- **THEN** one backend transaction closes A as `UNREGISTERED` with an account-change reason and creates a fresh `ACTIVE registrationId` for B
- **AND** no committed read observes two active associations or a partial change
- **AND** every delivery already linked to A remains linked to A's historical `registrationId`.

#### Scenario: An active installation changes APNs topic only
- **GIVEN** one installation has an `ACTIVE` registration for a valid environment/topic scope
- **WHEN** an authenticated request keeps the environment and presents another nonblank topic
- **THEN** one backend transaction closes the old registration as `UNREGISTERED` with `unregistered_reason=SCOPE_CHANGED` and creates a fresh `ACTIVE registrationId`
- **AND** no committed read observes two active associations or a partial change.

#### Scenario: An active installation changes APNs environment only
- **GIVEN** one installation has an `ACTIVE` registration for a valid environment/topic scope
- **WHEN** an authenticated request keeps the topic and changes only `sandbox`/`production`
- **THEN** one backend transaction closes the old registration as `UNREGISTERED` with `unregistered_reason=SCOPE_CHANGED` and creates a fresh `ACTIVE registrationId`
- **AND** no committed read observes two active associations or a partial change.

#### Scenario: Association replacement rolls back after provisional closure
- **GIVEN** one installation has one exact durable `ACTIVE` registration
- **WHEN** an injected datastore transaction fault occurs after the replacement transaction attempts to close that row but before it can persist the replacement
- **THEN** the replacement request fails
- **AND** after closing and reopening the datastore the original registration is exactly unchanged and `ACTIVE`
- **AND** no replacement or closed historical row from the failed attempt exists.

#### Scenario: Invalid or unregistered installation is registered again
- **GIVEN** an installation's latest association is `INVALID` or `UNREGISTERED`
- **WHEN** the authenticated app registers a valid token
- **THEN** Wakeve retains the old association in `registrationHistory`
- **AND** creates a fresh active `registrationId` without reactivating or overwriting the historical row.

#### Scenario: Registration lifecycle fields survive process restart
- **GIVEN** a token rotation, invalidation and subsequent registration have completed
- **WHEN** the backend store is closed and reopened after each durable boundary
- **THEN** every registration field, including IDs, scope, token hash, status, all lifecycle timestamps and typed reasons, matches the committed result
- **AND** no default value or reconstructed projection replaces persisted lifecycle metadata.

#### Scenario: Legacy backfill and rollback coexist with several installations
- **GIVEN** legacy registration rows and several v2 installations for the same user and platform
- **WHEN** backfill is repeated or an N-1 rollback reads its compatibility projection
- **THEN** deterministic legacy installation and registration identities derived from the immutable legacy row key make backfill idempotent across close/reopen and the repeated result reports `created=false`
- **AND** the projection selects only the compatible active association ordered by `lastRegisteredAt DESC`, `createdAt DESC`, then `registrationId ASC`
- **AND** the selection never deletes, merges, deactivates, or otherwise hides the other installations or their registration history
- **AND** a scope absent from legacy storage comes only from explicit rollout configuration
- **AND** the backend uses a stable configured secret HMAC key for those opaque identities, never an unkeyed or reversible derivation
- **AND** an absent or blank HMAC key fails closed before any backfill or rollback-read mutation
- **AND** the HMAC input uses the immutable legacy row key, so two distinct raw tokens with the same row key and HMAC key produce the same installation and registration IDs
- **AND** rollback reads are side-effect free: the exact association histories before the read equal those observed after another close/reopen
- **AND** rollback is a feature-flagged compatible adapter in the deployed binary; `LegacyNotificationTokenRead` exposes only selected registration identity, token hash, scope, and lifecycle metadata, then a separate provider port may decrypt the selected token only in memory
- **AND** N-1 clients remain compatible through the API and never read the backend datastore directly.

### Requirement: Durable Legacy Registration Compatibility Saga
During the N/N-1 compatibility window, Wakeve MUST model each legacy registration or unregistration dual-write as a durable idempotent saga. It MUST persist a pending reconciliation intent before either datastore write and MUST NOT claim a transaction spanning the SQLDelight legacy token store and `BackendDeviceRegistrationStore`. Each store step SHALL have its own durable outcome, attempt count, retry schedule and acknowledgement. The request key SHALL be an injective canonical typed tuple containing the operation, authenticated JWT subject, stable target, a durable monotonic compatibility generation and the token fingerprint or typed null, so HTTP retries coalesce while delimiter-bearing fields, another account or a later lifecycle generation remain distinct. A request MAY return explicit reconciliation acceptance after that intent is durably enqueued, but `{success:true}` SHALL require a durable converged terminal. Terminal blocking SHALL remain auditable and SHALL NOT fabricate convergence.

Each non-terminal effect state SHALL persist an exact `effectCheckpoint`, monotonic `checkpointRevision` and authority fencing token. Because XState snapshot restoration does not replay state `entry` actions, a recovery worker MUST first acquire by durable compare-and-set a lease scoped to the saga and checkpoint. Only a `RECOVERY_LEASE_ACQUIRED` acknowledgement from that durable port may record the opaque lease ID, opaque holder ID, strictly newer version and fencing token, checkpoint revision, deterministic logical expiration and an initially false emission marker in the saga context. Lease expiration SHALL use only persisted monotonic `logicalNowEpochSeconds` advanced by typed clock events and MUST NOT read an implicit wall clock.

After starting the restored actor, the holder MAY send `RECOVERY_REQUESTED` only with every recorded lease and checkpoint field. A foreign holder, mismatched identity, old version or fencing token, expired lease or stale checkpoint MUST NOT emit an effect. One recorded lease SHALL emit at most one recovery effect, including under concurrent requests. A replacement MAY replay the same logical `effectId` only after expiration and durable acquisition of a strictly newer version and fence.

Every effect acknowledgement, including write success/failure, retry persistence, convergence and blocking persistence, SHALL carry the expected `effectId`, `checkpointRevision` and authority fencing token emitted for that effect. The machine MUST match all three before changing state, attempts or retry schedule. A delayed acknowledgement from an earlier checkpoint SHALL be audit-only.

`RETRY_RECORDED.nextRetryAtEpochSeconds` MUST be strictly greater than the saga's persisted `logicalNowEpochSeconds`; a present or past deadline MUST be rejected without an implicit immediate retry. `RETRY_DUE` MUST NOT carry caller-supplied time and MUST match the current retry schedule revision. It MAY advance only when the persisted logical clock has reached the durable deadline. `CLOCK_ADVANCED` MUST carry the correlated saga ID, exactly the next clock revision and a timestamp greater than or equal to the persisted clock; a rewind, foreign saga or skipped/stale revision MUST be ignored. Clock value and revision MUST survive snapshot restoration.

The reconciliation retry delay MUST use full jitter over the exponential window `min(300, 2^(attempt-1))` seconds. Its injected sample SHALL be clamped to `[0, 1]`, with `NaN` treated as the fail-closed lower bound. Because the persisted deadline MUST be strictly future, the resulting delay SHALL be `max(1, floor(window * sample))`: zero sample produces one second and sample `1` reaches the current window or the 300-second cap. The exponent SHALL saturate at the cap before exponentiation, so arbitrarily large attempt counters cannot overflow or produce a delay outside `[1, 300]`.

The legacy installation and registration identities SHALL come exclusively from the configured stable HMAC over the immutable legacy primary key. N clients SHALL mutate only their exact v2 registration/installation and SHALL NOT write the lossy legacy `(user, platform)` row. N-1 operations SHALL target only the HMAC-derived compatibility installation in v2; no compatibility retry, compensation, cutover or rollback may enumerate, close, hide or delete another v2 installation.

#### Scenario: N-1 registration crashes after the legacy write
- **GIVEN** a reconciliation intent for one N-1 register command is durable
- **AND** the SQLDelight legacy write has committed
- **WHEN** the process crashes before the v2 compatibility registration commits
- **THEN** restart SHALL resume the v2 step with the same saga and idempotency keys
- **AND** SHALL treat a replayed legacy step only as `alreadyApplied`
- **AND** SHALL NOT create another v2 installation or overwrite an N client installation.

#### Scenario: N-1 unregistration preserves other v2 devices
- **GIVEN** one account has a deterministic legacy compatibility installation and separate N-client installations
- **WHEN** an N-1 client unregisters
- **THEN** the saga SHALL close the HMAC-derived v2 compatibility installation first
- **AND** delete only the corresponding legacy token row second
- **AND** leave every other v2 registration and its history unchanged.

#### Scenario: One compatibility store is unavailable
- **GIVEN** one saga step has committed and the other store is unavailable
- **WHEN** the unavailable step reports a retryable typed failure
- **THEN** Wakeve SHALL persist the failed store, its independent attempt and next retry time
- **AND** retry only that store with the same saga and request keys
- **AND** reach `blocked` after its bounded budget without reporting converged success
- **AND** use forward reconciliation rather than destructive cross-store compensation.

#### Scenario: Duplicate and restart converge idempotently
- **GIVEN** a pending or terminal saga exists for the same operation, stable target and token fingerprint
- **WHEN** the request is duplicated or the process restarts from any persisted step
- **THEN** Wakeve SHALL reuse the existing saga and durable store outcomes
- **AND** a leased recovery worker SHALL start the restored actor and request the exact persisted effect checkpoint
- **AND** the machine SHALL re-emit the first unacknowledged effect with its original deterministic effect ID
- **AND** SHALL return the persisted converged, accepted or blocked result without creating a parallel authority.

#### Scenario: Restored XState effect state does not replay entry implicitly
- **GIVEN** a durable snapshot is in `writingLegacy`, `writingV2`, `recordingRetry`, `recordingConvergence` or `recordingBlock`
- **WHEN** `createActor(machine, { snapshot }).start()` restores the actor
- **THEN** Wakeve SHALL NOT assume that the state's `entry` action ran
- **AND** a worker SHALL first durably acquire and record an opaque holder/version/fenced lease for the snapshot's exact checkpoint and revision
- **AND** the holder SHALL send `RECOVERY_REQUESTED` with every recorded lease field while the persisted logical clock is before expiry
- **AND** the machine SHALL emit that checkpoint's idempotent effect with the same `effectId`
- **AND** a concurrent second request, foreign holder, expired lease, old fence or mismatched checkpoint SHALL emit no effect.

#### Scenario: Recovery authority survives restart
- **GIVEN** a recovery lease and its not-yet-emitted marker are durable in one saga snapshot
- **WHEN** the actor is restored from that snapshot
- **THEN** its opaque lease identity, holder, version, fencing token, expiration, checkpoint and revision SHALL be unchanged
- **AND** only that holder MAY emit the recovered effect
- **AND** the emitted marker SHALL prevent a concurrent recovery from emitting it twice.

#### Scenario: Delayed effect acknowledgement is fenced
- **GIVEN** store effect checkpoint `c1` failed and the saga advanced to retry-recording checkpoint `c2`
- **WHEN** a success, failure or `RETRY_RECORDED` carrying the effect ID, revision or fencing token of `c1` arrives late
- **THEN** the saga SHALL remain on `c2`
- **AND** SHALL NOT increment an attempt, persist a retry schedule or mark a store applied
- **AND** only an acknowledgement matching the effect ID, revision and fencing token of `c2` MAY advance it.

#### Scenario: Retry deadline uses only the authoritative logical clock
- **GIVEN** the persisted logical clock is `100`
- **WHEN** retry persistence proposes a deadline at `100` or earlier
- **THEN** the schedule SHALL be rejected and no immediate retry SHALL be inferred
- **WHEN** a deadline at `110` is durably recorded and a `RETRY_DUE` event arrives before the clock advances
- **THEN** the saga SHALL remain in `retryWait`, even if the caller attaches an unmodeled future timestamp
- **WHEN** correlated `CLOCK_ADVANCED` reaches `109`
- **THEN** the saga SHALL remain in `retryWait`
- **WHEN** the next correlated clock revision reaches `110`
- **THEN** `RETRY_DUE` for the current schedule revision MAY resume only the failed store
- **AND** a clock rewind SHALL be ignored
- **AND** restoring the snapshot SHALL preserve the authoritative time and clock revision.

#### Scenario: Full jitter remains strictly future and overflow-safe
- **GIVEN** a reconciliation retry attempt and an injected jitter sample
- **WHEN** the sample is `0`, below `0`, or `NaN`
- **THEN** the computed delay SHALL be exactly one second and a deadline derived from it SHALL be strictly greater than the persisted logical clock
- **WHEN** the sample is `1` or above `1`
- **THEN** the delay SHALL reach the current exponential window without exceeding 300 seconds
- **AND** attempts up to the maximum supported integer SHALL remain greater than zero, at most 300 seconds, and free of exponent overflow.

#### Scenario: Delimiter-bearing identities do not collide
- **GIVEN** one command has subject `user:other` and target `target`
- **AND** another has subject `user` and target `other:target`
- **WHEN** their compatibility request keys are derived
- **THEN** the canonical typed tuples SHALL produce distinct keys
- **AND** neither command SHALL coalesce onto the other's saga.

#### Scenario: Cutover and rollback use recorded checkpoints
- **GIVEN** legacy and v2 records coexist during migration
- **WHEN** read authority cuts over to v2 or rolls back to legacy
- **THEN** the prior writer SHALL be paused and the reconciliation checkpoint SHALL be recorded before authority changes
- **AND** cutover SHALL require zero pending reconciliations and confirmed uniqueness
- **AND** rollback SHALL require a ready legacy projection while preserving all v2 rows and histories
- **AND** an unsafe checkpoint SHALL leave the previous authority active.

### Requirement: Fail-Closed Legacy Compatibility Request-Key Uniqueness Migration
Before enabling notification routes or the legacy compatibility recovery scheduler, Wakeve MUST run a durable migration preflight for the saga `request_key` uniqueness constraint. The preflight MUST scan duplicate groups and index presence without modifying the database. A duplicate-free database without the index SHALL install `UNIQUE(request_key)` before runtime starts; an already indexed duplicate-free database MAY reach `READY` idempotently. Any duplicate finding MUST enter `BLOCKED_DUPLICATES`, MUST NOT attempt DDL and MUST leave the database unchanged. Observable diagnostics SHALL expose only sanitized group digests and counts, never raw request keys, tokens or saga identifiers.

Every migration effect SHALL carry a deterministic effect ID, checkpoint revision and monotonic fencing token. Acknowledgements and failures MUST match all three. Restoring an XState snapshot MUST NOT rely on implicit replay of state entry actions and MUST initially emit no effect.

Before `RECOVERY_REQUESTED`, a durable port MUST acquire a compare-and-set lease scoped to the exact migration effect ID, checkpoint and revision. Only after that CAS commits MAY `RECOVERY_LEASE_ACQUIRED` record an opaque lease ID and holder ID, a lease version and fencing token strictly greater than every previously recorded value, the exact checkpoint reference, a deterministic logical expiry, the acquisition clock revision and `effectEmitted=false`. The authoritative logical clock MUST be initialized explicitly, persisted in the snapshot and advanced only by a correlated `CLOCK_ADVANCED` carrying exactly the next clock revision and a nondecreasing timestamp. No guard MAY read implicit wall-clock time.

`RECOVERY_REQUESTED` MUST exactly match the recorded lease ID, holder, version, fence, effect ID, checkpoint and checkpoint revision while the persisted logical clock is before expiry. The first accepted request SHALL durably mark `effectEmitted=true` before its emitted signal is consumed; the same lease MUST NOT emit twice, including before an acknowledgement. A foreign holder, concurrent losing worker, expired lease, older version or fence, or stale checkpoint MUST emit nothing. After expiry, only a newly committed lease with strictly higher version and fence MAY replay the same idempotent effect. Every effect acknowledgement after recovery MUST match the current effect ID, checkpoint revision and lease fence; an acknowledgement from the pre-crash effect or replaced lease SHALL be ignored. Any effect failure SHALL keep routes and the scheduler disabled until an explicit repair confirmation fenced by the current scan revision starts a new preflight.

Wakeve MUST NOT choose a canonical duplicate automatically or through an LLM. An operator resolution MUST name the current `scanRevision`, the duplicate group digest, a canonical saga belonging to that group and an opaque resolution ID. It SHALL be accepted only when all rows have the same validated business identity, the group is quiescent and nondivergent, and no row has an active lease. Before deleting any noncanonical row, Wakeve MUST create an immutable archive of those rows and their effects with a nonempty archive identity and digest. Deletion SHALL target only archived noncanonical rows and SHALL be followed by a complete new preflight before index installation. Divergent, nonquiescent or actively leased groups SHALL remain blocked for external repair.

#### Scenario: Fresh and already indexed databases become ready safely
- **GIVEN** startup has not enabled notification routes or the compatibility scheduler
- **WHEN** read-only preflight finds no duplicate and the unique index is absent
- **THEN** Wakeve SHALL install `UNIQUE(request_key)` before reaching `READY`
- **WHEN** a later startup finds no duplicate and the index is already present
- **THEN** it SHALL reach `READY` without repeating DDL or mutating saga rows.

#### Scenario: Duplicate discovery blocks without mutation or disclosure
- **GIVEN** two or more compatibility sagas share a request key
- **WHEN** startup preflight reports that duplicate group
- **THEN** migration SHALL enter `BLOCKED_DUPLICATES` and keep routes and the scheduler disabled
- **AND** SHALL execute no index DDL and leave all database rows unchanged
- **AND** diagnostics SHALL contain only group digest and aggregate counts, without a raw request key, token or saga ID.

#### Scenario: Explicit simple-group resolution archives before deletion
- **GIVEN** a duplicate group whose rows have the same validated business identity, no active lease and a quiescent nondivergent state
- **WHEN** an operator names a canonical saga in that group using the exact current `scanRevision`
- **THEN** Wakeve SHALL archive immutably every noncanonical row and effect with a durable archive ID and digest
- **AND** SHALL delete only those archived rows after the exact archive acknowledgement
- **AND** SHALL preserve the archive audit across restart
- **AND** SHALL run a complete new preflight before installing the unique index or reaching `READY`.

#### Scenario: Invalid or unsafe resolution remains blocked
- **GIVEN** duplicate findings are divergent, nonquiescent, actively leased, or were produced by a newer scan
- **WHEN** a resolution uses a stale scan revision, a canonical saga outside its group, or targets an unsafe group
- **THEN** the event SHALL be ignored and the migration SHALL remain blocked without archive, delete or DDL
- **AND** unsafe groups SHALL require an explicit external repair followed by a new preflight.

#### Scenario: Crash and restart resume one exact checkpoint
- **GIVEN** the process crashes before or after committing archive, delete, preflight or index work
- **WHEN** the actor is restored from its durable snapshot
- **THEN** startup SHALL NOT assume that a state entry action replayed
- **AND** the worker SHALL first acquire and record a durable CAS lease for the exact effect, checkpoint and revision
- **AND** only the exact nonexpired holder/version/fence SHALL re-emit the current idempotent effect once
- **AND** a stale acknowledgement, foreign holder, concurrent loser or second recovery request SHALL be inert
- **AND** repeated startup SHALL preserve the immutable archive audit and converge without duplicate deletion or DDL.

#### Scenario: Recovery authority survives restore and rotates only after logical expiry
- **GIVEN** a durable recovery lease with `effectEmitted=false` is recorded for archive, delete, re-scan or index
- **WHEN** two workers restore that snapshot and contend for recovery
- **THEN** restored actors SHALL emit no entry effect
- **AND** only the worker acknowledged by the durable CAS MAY match the opaque holder, version, fence and checkpoint
- **AND** its first exact request SHALL persist `effectEmitted=true` and a second request before ACK SHALL emit nothing
- **WHEN** the monotonic logical clock reaches lease expiry
- **THEN** the expired holder and its ACKs SHALL remain inert
- **AND** only a replacement lease with strictly greater version and fence MAY emit again
- **AND** an ACK from the replaced fence SHALL NOT advance the migration.

#### Scenario: Migration failure rolls back fail-closed
- **GIVEN** a migration checkpoint fails or conflicts
- **WHEN** rollback or recovery is evaluated
- **THEN** Wakeve SHALL keep notification routes and the compatibility scheduler disabled
- **AND** SHALL NOT infer `READY` from partial archive, delete or index work
- **AND** only an explicit repair confirmation fenced by the current scan revision MAY start a new read-only preflight.

### Requirement: Durable Idempotent Notification Delivery
Within the backend datastore only, Wakeve MUST persist each logical notification and its per-registration `notification_delivery` records transactionally before provider I/O. This backend transaction is downstream of, and not atomic with, local SQLDelight envelope persistence. Each delivery SHALL hold a non-cascading FK to the historical `registrationId` it targets and use stable `deliveryKey` idempotency, a unique recipient/registration/provider constraint, durable states, leases, attempt counts, next-attempt timestamps, expiration, provider response metadata, and terminal outcomes. A process crash or repeated producer event MUST NOT lose the notification or create a duplicate logical delivery.

#### Scenario: Domain event is enqueued twice
- **GIVEN** a workflow event produces a stable notification idempotency key
- **WHEN** the producer submits the same event more than once
- **THEN** Wakeve returns or reuses the same logical notification
- **AND** creates at most one delivery for each eligible active `registrationId`
- **AND** does not send duplicate work because of the repeated enqueue.

#### Scenario: Worker crashes after acquiring a delivery
- **GIVEN** a queued delivery has a time-bounded worker lease
- **WHEN** the worker stops before persisting a terminal provider result
- **THEN** the durable delivery remains recoverable
- **AND** becomes eligible after the lease expires
- **AND** resumes with the same delivery identity and idempotency key.

#### Scenario: Provider observation cannot choose its outcome
- **GIVEN** a delivery attempt holds a durable lease
- **WHEN** the worker reports an HTTP or transport observation
- **THEN** the observation SHALL match the delivery key, correlation ID, attempt, lease holder, lease version and fencing token
- **AND** the model SHALL classify the outcome internally from HTTP status, closed reason matrix or transport phase
- **AND** an outcome-like caller field, stale correlation, foreign holder or old fence SHALL have no transition authority.
- **AND** `mayHaveWritten` SHALL classify as durable recoverable `unknownOutcome`, never as `unknownTerminal`.

#### Scenario: Calendar provider result is durable independently
- **GIVEN** a stable `calendarArtifactKey`
- **WHEN** a correlated calendar observation is received under its exact attempt and durable holder/version/fencing lease
- **THEN** the calendar artifact SHALL stage its own effect and checkpoint
- **AND** observation alone SHALL NOT imply application
- **AND** only explicit persistence emission followed by the exact durable ACK SHALL reach `applied`, nonterminal `retry`, `expired` or `retryExhausted` after restart
- **AND** calendar retry, clock, expiry and recovery SHALL remain independent from provider delivery.

#### Scenario: Quiet hours defer a non-urgent delivery
- **GIVEN** a non-urgent notification is allowed by user preferences but falls within quiet hours
- **WHEN** the delivery policy is evaluated
- **THEN** the delivery transitions to a durable deferred state
- **AND** stores the next eligible time
- **AND** re-evaluates policy and expiration before provider send.

#### Scenario: No active token is available yet
- **GIVEN** a valid notification exists for a user with no eligible active registration
- **WHEN** delivery policy is evaluated
- **THEN** the notification remains durably awaiting a token until its expiration
- **AND** a later token registration causes policy to be evaluated again
- **AND** the absence of a token is not recorded as a successful send.

#### Scenario: APNs accepts a delivery
- **GIVEN** an APNs delivery is queued for an active `registrationId`
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
- **THEN** the two runtime `/models` XState machines, the compatibility saga, and their transition tests are created and reviewed first
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
