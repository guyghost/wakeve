## ADDED Requirements

### Requirement: Confirmation SHALL Commit One Domain Effect Envelope
The `DatabaseEventRepository` confirmation transaction SHALL commit the decision and exactly one local `confirmation_effect_outbox` envelope identified by `domainEventId` and `effectKey`. It SHALL NOT create backend notification recipients, provider deliveries, or calendar artifacts, and Wakeve SHALL NOT claim atomicity across local SQLDelight and backend databases.

#### Scenario: Local confirmation commits offline
- **GIVEN** all confirmation guards pass while the device is offline
- **WHEN** the repository transaction commits
- **THEN** the confirmed decision, exact slot, receipt, sync metadata, and one domain-effect envelope SHALL be durable
- **AND** `decisionSyncStatus` SHALL be pending
- **AND** `effectDispatchStatus` SHALL be not dispatched
- **AND** no participant notification or calendar artifact SHALL be claimed.

#### Scenario: Backend accepts the envelope later
- **GIVEN** a pending envelope has a stable `domainEventId` and `effectKey`
- **WHEN** the backend accepts it idempotently
- **THEN** decision synchronization MAY become acknowledged
- **AND** backend recipient and calendar fan-out SHALL begin independently
- **AND** downstream failure SHALL NOT revert the confirmed decision.

### Requirement: Poll Date Confirmation MUST be owned by a deterministic workflow model
Wakeve MUST model poll date confirmation as the explicit state flow `reviewingResults -> confirmPrompt -> confirming -> confirmed | failed`. The shared workflow state machine MUST be the only authority for confirmation guards, persistence, feature unlocking, pending-sync state, and navigation eligibility. SwiftUI views MUST render state and dispatch typed events; they MUST NOT call `updateEventStatus` or another confirmation repository mutation directly.

#### Scenario: Organizer opens and cancels the confirmation prompt
- **GIVEN** an organizer is in `reviewingResults` with an eligible proposed slot
- **WHEN** the organizer opens the confirmation prompt
- **THEN** the model transitions to `confirmPrompt`
- **WHEN** the organizer cancels
- **THEN** the model returns to `reviewingResults`
- **AND** no event, confirmed-date, outbox, sync, success-feedback, or navigation side effect occurs.

#### Scenario: Organizer submits one modeled confirmation
- **GIVEN** the model is in `confirmPrompt`
- **AND** organizer, event status, vote, and selected-slot guards pass
- **WHEN** the organizer submits confirmation
- **THEN** the model transitions to `confirming`
- **AND** dispatches one typed shared confirmation command
- **AND** disables or coalesces further submit events until the command resolves.

#### Scenario: Confirmation fails before commit
- **GIVEN** the model is in `confirming`
- **WHEN** the shared workflow returns a typed non-committed failure
- **THEN** the model transitions to `failed`
- **AND** exposes retry or dismissal according to the failure type
- **AND** does not show success, unlock downstream work, or navigate.

#### Scenario: Release guard detects a SwiftUI status write
- **GIVEN** a source file under `iosApp/src/Views` calls `updateEventStatus`
- **WHEN** critical release architecture gates run
- **THEN** the gate fails with the offending file
- **AND** production readiness is blocked until the view dispatches the modeled intent instead.

### Requirement: Poll Date Confirmation MUST be locally atomic and idempotent
One application-scoped `DatabaseEventRepository` command MUST atomically persist the `CONFIRMED` transition, exact selected-slot record, exactly one `confirmation_effect_outbox` domain envelope, sync metadata, and confirmation/navigation receipt. It MUST NOT create provider or calendar artifacts. Navigation MUST be emitted only from the committed local receipt. The repository MUST enforce idempotency and uniqueness independently of UI controls; backend fan-out occurs later without cross-database atomicity.

#### Scenario: Atomic confirmation commits every required outcome
- **GIVEN** a valid confirmation command for a `POLLING` event
- **WHEN** the local repository transaction commits
- **THEN** the event is `CONFIRMED`
- **AND** exactly one confirmed-date record references the selected slot
- **AND** exactly one domain envelope identified by `domainEventId` and `effectKey` is durably queued
- **AND** sync metadata and one confirmation receipt are persisted
- **AND** the state machine may emit the receipt's navigation target once
- **AND** no provider delivery or calendar artifact exists until later backend ingestion and participant fan-out.

#### Scenario: Any local persistence step fails
- **GIVEN** a valid confirmation command
- **WHEN** event, confirmed-slot, outbox, sync, or receipt persistence fails before commit
- **THEN** the transaction rolls back every confirmation write
- **AND** the event remains `POLLING`
- **AND** no confirmed-date, outbox, sync, or confirmation receipt is partially created
- **AND** no success or navigation side effect is emitted.

#### Scenario: Same confirmation is dispatched more than once
- **GIVEN** a confirmation for an event and slot has committed
- **WHEN** the same operation is retried or concurrent duplicate dispatches arrive
- **THEN** the repository returns the existing committed receipt
- **AND** does not duplicate the confirmed-date, outbox, sync, participant-delivery, or navigation work.

#### Scenario: Different slot is requested after confirmation
- **GIVEN** an event has a committed confirmed slot
- **WHEN** a confirmation command requests a different slot
- **THEN** the repository returns a typed conflict
- **AND** preserves the original event, confirmed slot, outbox, sync, receipt, and navigation state unchanged.

### Requirement: Offline Poll Confirmation MUST expose durable pending sync
A successful local confirmation MUST remain usable offline. When remote sync or downstream delivery has not completed, the modeled state MUST be `confirmed.pendingSync`; it MUST NOT imply server acknowledgement or participant delivery. Pending work MUST survive process restart and replay idempotently.

#### Scenario: Organizer confirms while offline
- **GIVEN** a valid poll confirmation and no network connectivity
- **WHEN** the atomic local transaction commits
- **THEN** the event is locally `CONFIRMED`
- **AND** the model enters `confirmed.pendingSync`
- **AND** local scenario organization may continue
- **AND** the UI states that synchronization/delivery is pending
- **AND** it does not claim that the server or participants have received the decision.

#### Scenario: App restarts with confirmation work pending
- **GIVEN** a locally confirmed event has pending sync or confirmation outbox records
- **WHEN** the app restarts and rehydrates the workflow
- **THEN** the model restores `confirmed.pendingSync` without redispatching confirmation
- **AND** workers retry the persisted work with the original idempotency keys
- **AND** successful replay transitions the sync substate to `confirmed.synced` without repeating navigation or duplicating delivery.

### Requirement: Poll Confirmation Time MUST use an injected clock
Deadline evaluation and all confirmation timestamps MUST use an injected clock. A confirmation transaction MUST capture one clock instant and reuse it for its event, confirmed-date, outbox, sync, and receipt timestamps. Production confirmation logic MUST NOT use fixed timestamps, direct system-clock lookups, or lexicographic string comparisons.

#### Scenario: Voting deadline is reached exactly
- **GIVEN** the injected clock instant equals the voting deadline
- **WHEN** a participant submits a new or changed vote
- **THEN** the vote is rejected because voting is closed
- **WHEN** the organizer submits an otherwise valid confirmation
- **THEN** the deadline does not reject confirmation
- **AND** all committed confirmation records use the same captured clock instant.

#### Scenario: Tests control time deterministically
- **GIVEN** workflow tests inject instants before, at, and after the voting deadline
- **WHEN** vote and confirmation guards are evaluated
- **THEN** voting is open only before the deadline
- **AND** otherwise valid organizer confirmation remains allowed at and after the deadline
- **AND** test outcomes do not depend on the machine's wall clock.
