# RED evidence — tasks 2.1–2.3

Recorded 2026-07-09 before shared production or SQLDelight schema implementation. Tasks 2.1–2.3 remain unchecked.

## Shared workflow

Command:

`./gradlew :shared:jvmTest --tests "*EventManagementStateMachinePollingConfirmation*" --tests "*DatabaseEventRepositoryConfirmDateTest*"`

Result after review fixes: focused shared/repository selection compiles; exactly 20 tests execute and 17 fail for intended missing behavior. The shared workflow coverage includes before/exact/after deadline expectations and exact-deadline vote closure.

- deadline still rejects confirmation instead of closing votes only;
- presentation exposes legacy untyped messages instead of typed failure objects carrying `NOT_ORGANIZER`, `INVALID_EVENT_STATUS`, `NO_VOTES`, and `SLOT_NOT_FOUND`;
- confirmation queues two provider-oriented in-memory artifacts instead of one domain envelope;
- duplicate dispatch is not coalesced to one durable outcome.

`EventManagementStateMachinePollingConfirmationContractTest` first executes a test-only reducer derived from the approved model fixtures and proves cancel dispatches zero commands, duplicate submit leaves one operation in flight, and retry reuses the same `operationId`. The same test then binds those behaviors to the real shared sealed contract and render state via JVM reflection. It fails because prompt, cancel, retry, submit-with-stable-`operationId`, typed failure, phase, and in-flight operation properties do not exist yet.

## SQLDelight repository, idempotency, and restart

Command:

`./gradlew :shared:jvmTest --tests "*DatabaseEventRepositoryConfirmDateTest*"`

Result: 7 repository tests execute, 6 fail for intended missing behavior (the existing exact-slot test passes):

- atomic outcome: expected one durable `confirmation_effect_outbox`, found zero;
- injected sync-boundary failure: expected event rollback to `POLLING`, found `CONFIRMED`;
- same-slot replay and concurrent duplicate: expected one sync/receipt outcome, observed duplicate metadata (concurrent run expected 1, found 3 including create metadata);
- different-slot replay overwrites instead of returning `ALREADY_CONFIRMED_DIFFERENT_SLOT`;
- restart has no durable confirmation receipt or outbox envelope to rehydrate/replay. Receipt assertions now inspect the real SQLite schema through a second JDBC connection; no constant-null stub remains.

These failures isolate missing command/schema/transaction capabilities; there is no arbitrary compilation failure.

## Architecture guard self-test

- `bash -n scripts/test-ios-poll-confirmation-architecture.sh` passes.
- `./scripts/test-ios-poll-confirmation-architecture.sh --self-test` passes positive and negative fixtures.
- The clean real-path invocation fails on the current direct `updateEventStatus` calls in `PollResultsView.swift` and `ParticipantManagementView.swift`, as expected. The generalized matcher covers any receiver calling `updateEventStatus`, `confirmDate`, `confirmEventDate`, related confirmation aliases, or assigning `.status = .confirmed`.

## Explicit remaining repository blocker

The current schema has no durable envelope or receipt tables and the repository has no transaction-boundary injection contract. Tests inspect the actual database via a second JDBC connection and prove these capabilities are absent. Injecting failures specifically after envelope and receipt writes, or correlating one captured timestamp across all five records, cannot be implemented as a behavioral test without inventing fake persistence; tasks 2.2–2.3 therefore remain unchecked pending the planned production repository interface/schema.
