# Specification: Offline Sync

> **Capability**: `offline-sync`
> **Version**: 1.0.0
> **Status**: Active
> **Last Updated**: 2026-02-08

## Purpose

The Offline Sync capability ensures Wakeve remains usable without network connectivity by treating the local database as the source of truth and replaying queued changes when connectivity returns.

## Overview

This specification defines the offline-first synchronization strategy for Wakeve. The application uses SQLite as the local source of truth, with background synchronization to a remote backend. All operations work offline, with changes queued for later synchronization.

**Version**: 1.0.0
**Status**: Active
**Created**: 2026-02-08
**Maintainer**: Backend Team

### Core Concepts

**Local Source of Truth**: SQLite database is the primary data store. All reads come from local data.

**Sync Queue**: Pending operations are queued for background synchronization.

**Conflict Resolution**: When multiple clients modify the same data, conflicts are resolved using last-write-wins based on timestamps.

**Optimistic Locking**: Updates proceed without locking, with conflicts detected and resolved during sync.

### Key Features

- **Offline-First**: Full functionality without network connectivity
- **Background Sync**: Automatic synchronization when network is available
- **Conflict Resolution**: Last-write-wins with user notification
- **Retry Logic**: Exponential backoff for failed sync operations
- **Sync Indicators**: UI shows pending operations and sync status

### Dependencies

| Dependency | Type | Description |
|------------|------|-------------|
| All data specs | Spec | All specs with persistent data follow this pattern |

## Purpose

The Offline Sync capability ensures users can interact with Wakeve without network connectivity. This is critical for events in areas with poor reception.

### Use Cases

- **Offline Event Creation**: User creates an event while on a plane
- **Offline Voting**: User votes on dates while at a remote cabin
- **Background Sync**: Changes automatically sync when WiFi becomes available
- **Conflict Resolution**: Two users edit the same event, system merges changes
## Requirements
### Requirement: Local-First Data Access
The system SHALL read application data from the local SQLite database first.

**ID**: `SYNC-001`

The system SHALL read all data from the local SQLite database.

#### Scenario: Read events while offline
- **GIVEN** a user is offline
- **WHEN** they open the events list
- **THEN** the system SHALL load events from local SQLite and display immediately

### Requirement: Write Operations with Sync Queue
The system SHALL queue local write operations for background synchronization.

**ID**: `SYNC-002`

The system SHALL queue all write operations for background synchronization.

#### Scenario: Create event while offline
- **GIVEN** a user is offline
- **WHEN** they create a new event
- **THEN** the system SHALL insert into local SQLite and queue for sync

### Requirement: Conflict Resolution
The system SHALL resolve synchronization conflicts deterministically.

**ID**: `SYNC-003`

The system SHALL resolve conflicts using last-write-wins based on updatedAt timestamp.

#### Scenario: Server wins conflict
- **GIVEN** local has older timestamp than server
- **WHEN** sync occurs
- **THEN** apply server changes and notify user

### Requirement: Sync Status Indicators
The system SHALL display synchronization state to users.

**ID**: `SYNC-004`

The system SHALL display sync status to users.

#### Scenario: Offline indicator
- **GIVEN** a user is offline
- **WHEN** viewing any screen
- **THEN** show "Offline" indicator

### Requirement: Retry Logic
The system SHALL retry failed synchronization operations with backoff.

**ID**: `SYNC-005`

The system SHALL retry failed sync operations with exponential backoff (30s, 60s, 120s, 240s, max 1h).

#### Scenario: Retry failed sync operation
- **GIVEN** a queued sync operation fails due to a transient error
- **WHEN** the retry delay elapses
- **THEN** the system SHALL retry the operation using exponential backoff
- **AND** preserve the operation until it succeeds or reaches the retry policy limit

### Requirement: Background Sync
The system SHALL perform background synchronization when device and app conditions allow it.

**ID**: `SYNC-006`

The system SHALL perform background synchronization when conditions are met (app foreground, network available, periodic 15min).

#### Scenario: Background sync runs when online
- **GIVEN** pending sync operations exist
- **WHEN** the app is foregrounded or the periodic background task runs with network connectivity
- **THEN** the system SHALL replay pending operations in the background

### Requirement: Critical Organization Offline Writes MUST be replayable
Wakeve MUST support local-first writes and queued synchronization for every critical organization operation.

#### Scenario: Organizer completes logistics while offline
- **GIVEN** the organizer is offline during `ORGANIZING`
- **WHEN** they select lodging, select transport, create a meeting, update budget, record a payment handoff, and schedule reminders
- **THEN** each operation writes to local storage immediately
- **AND** each operation creates a sync queue entry with entity type, operation type, payload, timestamp, and retry state
- **AND** the UI shows pending sync status for affected sections

### Requirement: Finalization Sync Safety MUST be enforced
Wakeve MUST prevent finalization while blocking critical organization operations are unsynced or conflicted.

#### Scenario: Finalization blocked by unresolved conflict
- **GIVEN** an event is in `ORGANIZING`
- **AND** a selected lodging update has an unresolved sync conflict
- **WHEN** the organizer attempts to finalize
- **THEN** finalization is blocked
- **AND** the readiness summary points to the conflicted lodging update
- **AND** the event remains editable until the conflict is resolved

### Requirement: Scenario Matrix Operations MUST be offline-first
Wakeve MUST support scenario matrix generation, editing, publication, voting, and final selection while offline by persisting local changes and queuing sync metadata.

#### Scenario: Organizer publishes matrix while offline
- **GIVEN** the organizer is offline
- **AND** draft matrix scenarios exist locally
- **WHEN** the organizer publishes the matrix
- **THEN** scenarios are updated locally to `PROPOSED`
- **AND** the event status is updated locally to `COMPARING`
- **AND** sync operations are queued for the event and affected scenarios

#### Scenario: Organizer selects final matrix scenario while offline
- **GIVEN** the organizer is offline
- **AND** a published matrix scenario exists
- **WHEN** the organizer selects it as final
- **THEN** the selected scenario and event final date are persisted locally
- **AND** sync operations are queued for replay when network access returns

## Data Models

### Sync Operation

```kotlin
@Serializable
data class SyncOperation(
    val id: String,
    val type: SyncOperationType,
    val tableName: String,
    val recordId: String,
    val data: String,
    val createdAt: Instant,
    val retryCount: Int = 0,
    val nextRetryAt: Instant? = null,
    val status: SyncStatus
)
```

### Sync Operation Type

```kotlin
enum class SyncOperationType { CREATE, UPDATE, DELETE }
```

### Sync Status

```kotlin
enum class SyncStatus {
    PENDING, IN_PROGRESS, FAILED, PERMANENT_FAILURE, SUCCESS
}
```

## API / Interface

### POST /api/sync

**Description**: Synchronize local changes with server

**Request Body**:
```json
{
  "operations": [...],
  "lastSyncAt": "2026-02-08T13:00:00Z"
}
```

**Response 200 OK**:
```json
{
  "syncedAt": "2026-02-08T14:05:00Z",
  "operations": [{"id": "...", "status": "SUCCESS"}],
  "conflicts": [...],
  "serverChanges": [...]
}
```

## Security

### Authentication Requirements

All sync operations require valid JWT token. Sync endpoint enforces user ownership of synced records.

## State Machine Integration

### Sync Intents

```kotlin
sealed interface SyncIntent : Intent {
    data class StartSync(val force: Boolean = false) : SyncIntent
    data class RetryOperation(val operationId: String) : SyncIntent
    object SyncAll : SyncIntent
}
```

## Database Schema

```sql
CREATE TABLE sync_operation (
    id TEXT PRIMARY KEY,
    type TEXT NOT NULL,
    table_name TEXT NOT NULL,
    record_id TEXT NOT NULL,
    data TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    retry_count INTEGER DEFAULT 0,
    next_retry_at INTEGER,
    status TEXT NOT NULL DEFAULT 'PENDING'
);
```

## Testing Requirements

**Coverage Target**: 85%

### Unit Tests

- SyncQueueTest: Enqueue, retry logic
- ConflictResolutionTest: Server wins, local wins, tie
- OfflineSyncFlowTest: Offline CREATE → Online sync

### Integration Tests

- BackgroundSyncTest: Sync on app foreground, network available

## Platform-Specific Implementation

### Android: WorkManager

```kotlin
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = syncManager.syncPendingOperations()
}
```

### iOS: BGTaskScheduler

```swift
class SyncTask: BGTask {
    func schedule() {
        let request = BGProcessingTaskRequest(identifier: "com.wakeve.sync")
        try? BGTaskScheduler.shared.submit(request)
    }
}
```

## Implementation Files

### Core
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/sync/SyncManager.kt`
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/sync/SyncQueue.kt`
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/sync/ConflictResolver.kt`

### Backend
- `server/src/main/kotlin/com/guyghost/wakeve/routes/SyncRoutes.kt`

### Android
- `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/service/SyncWorker.kt`

### iOS
- `iosApp/src/Services/SyncTask.swift`

## Related Specifications

All data-intensive specs follow this pattern for offline support.

## Internationalization

| Key | English | French |
|-----|---------|--------|
| `sync.offline` | You're offline | Vous êtes hors ligne |
| `sync.pending` | Pending changes | Modifications en attente |
| `sync.syncing` | Syncing... | Synchronisation... |

## Performance Considerations

- Batch Size: Up to 100 operations per request
- Compression: Gzip compress sync payload > 10KB
- Throttling: Don't sync more than once per 30 seconds automatically

## Change History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2026-02-08 | Initial version |

## Acceptance Criteria

- [x] All reads work offline immediately
- [x] All writes queue for background sync
- [x] Conflicts resolve with last-write-wins
- [x] Sync status is visible in UI
- [x] Failed operations retry with exponential backoff
- [x] Background sync runs on all platforms

## Success Metrics

- < 100ms average read time from local database
- > 95% sync success rate on good network
- < 5 minute average sync delay

---

**Spec Version**: 1.0.0
**Last Updated**: 2026-02-08
**Status**: Active
**Maintainer**: Backend Team
