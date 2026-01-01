# Multi-User Collaboration Tests 🤝

Comprehensive E2E test suite for real-time collaboration features in Wakeve.

## 📍 Location
`shared/src/commonTest/kotlin/com/guyghost/wakeve/e2e/MultiUserCollaborationTest.kt`

## 🧪 Test Suite Overview

**12 Tests** covering all collaboration scenarios:

### Core Collaboration Tests (10)

1. **testRealTimePollVoting** - Real-time voting aggregation
   - Validates YES/MAYBE/NO vote scoring
   - Tests cross-participant visibility
   - Score: YES=2, MAYBE=1, NO=-1

2. **testConcurrentScenarioCreation** - Concurrent scenario creation
   - Simulates multiple users creating scenarios simultaneously
   - Validates no conflicts in ID generation
   - Ensures all scenarios persist

3. **testScenarioVotingWithScoring** - Scenario preference voting
   - Tests PREFER (+2), NEUTRAL (+1), AGAINST (-1) scoring
   - Validates ranking by score
   - Scenario 1 (score 5) > Scenario 2 (score 1)

4. **testCommentsBySection** - Comments with threading
   - Tests comment organization by section (TRANSPORT, MEAL, BUDGET, etc.)
   - Validates parent-child relationships via `parentCommentId`
   - Verifies thread traversal

5. **testRealTimeSyncBetweenUsers** - User-to-user synchronization
   - User A creates scenario
   - User B (offline) reconnects and sees changes
   - Validates eventual consistency

6. **testNotificationsOnCollaborationActions** - Action tracking
   - Records votes, comments, scenario creation
   - Validates all actions trackable for notifications
   - Tests activity feed generation

7. **testPermissionBasedActions** - Role-based access control
   - ORGANIZER: always can modify
   - PARTICIPANT: only in CONFIRMED/COMPARING
   - ADMIN: always can modify
   - Status guards: DRAFT/FINALIZED block modifications

8. **testConflictResolutionInVoting** - Last-write-wins strategy
   - User votes YES on slot-1
   - User changes vote to MAYBE
   - Latest vote wins, older vote discarded

9. **testReadOnlyModeAfterFinalization** - Event finalization
   - FINALIZED events block all modifications
   - Organizer can still read data
   - Clear "read-only" mode indication

10. **testCrossDeviceSynchronization** - Multi-device consistency
    - Device 1 (Android) creates scenario
    - Device 2 (iOS) votes on scenario
    - Both devices see same state

### Bonus Tests (2)

11. **testCommentThreading** - Comment thread building
    - Main comment + 2 nested replies
    - Validates thread structure preservation
    - Tests thread traversal

12. **testCommentStatistics** - Comment aggregation
    - 3 BUDGET + 2 TRANSPORT + 1 ACCOMMODATION comments
    - Validates accurate per-section counts
    - Tests statistics calculation

## 🏗️ Architecture

### Test Infrastructure

```kotlin
// Test user with role
data class TestUser(id: String, name: String, role: UserRole)
enum class UserRole { ORGANIZER, PARTICIPANT, ADMIN }

// In-memory event store (no DB required)
class TestEventStore {
    fun recordVote(eventId, participantId, slotId, vote)
    fun voteOnScenario(eventId, scenarioId, participantId, voteType)
    fun canModifyScenario(eventId, role)
    // ... 10 more methods
}

// In-memory comment store
class TestCommentRepository {
    fun addComment(eventId, comment)
    fun getCommentThread(eventId, commentId)
    fun countCommentsBySection(eventId, section)
    // ... 6 more methods
}
```

### Patterns Used

✅ **GIVEN/WHEN/THEN** - Clear test structure  
✅ **Mock In-Memory Stores** - No database dependency  
✅ **User Roles** - ORGANIZER, PARTICIPANT, ADMIN  
✅ **Event Status Guards** - DRAFT, POLLING, CONFIRMED, COMPARING, ORGANIZING, FINALIZED  
✅ **Score Calculation** - Weighted voting scores  
✅ **Thread Building** - Comment parent-child relationships  
✅ **Last-Write-Wins** - Conflict resolution  
✅ **Eventual Consistency** - Sync simulation  

## 📊 Coverage Matrix

| Feature | Tests | Status |
|---------|-------|--------|
| Poll Voting | testRealTimePollVoting | ✅ |
| Scenario Creation | testConcurrentScenarioCreation | ✅ |
| Scenario Voting | testScenarioVotingWithScoring | ✅ |
| Comments | testCommentsBySection, testCommentThreading, testCommentStatistics | ✅ |
| Synchronization | testRealTimeSyncBetweenUsers, testCrossDeviceSynchronization | ✅ |
| Notifications | testNotificationsOnCollaborationActions | ✅ |
| Permissions | testPermissionBasedActions | ✅ |
| Conflicts | testConflictResolutionInVoting | ✅ |
| Finalization | testReadOnlyModeAfterFinalization | ✅ |

## 🔗 OpenSpec Coverage

All 10 original test scenarios from requirements are implemented:

- ✅ Real-Time Poll Voting
- ✅ Concurrent Scenario Creation
- ✅ Scenario Voting with Scoring
- ✅ Comments by Section
- ✅ Real-Time Sync
- ✅ Notifications on Collaboration Actions
- ✅ Permission-based Actions
- ✅ Conflict Resolution in Voting
- ✅ Read-Only Mode After Finalization
- ✅ Cross-Device Synchronization

## 🚀 Running Tests

```bash
# Run all JVM tests (includes MultiUserCollaborationTest)
./gradlew shared:jvmTest

# Run only e2e tests
./gradlew shared:jvmTest -k "e2e"

# Run specific test class
./gradlew shared:jvmTest -k "MultiUserCollaborationTest"

# Run specific test
./gradlew shared:jvmTest -k "testRealTimePollVoting"
```

## 📚 Test Framework

- **Language**: Kotlin Multiplatform
- **Framework**: Kotlin Test (kotlin.test)
- **Async**: kotlinx.coroutines.test.runTest
- **Assertions**: assertEquals, assertNotNull, assertTrue, assertFalse
- **Mocking**: In-memory stores (no external mocks)

## 🔍 Key Test Patterns

### Vote Scoring
```kotlin
// PREFER=2, NEUTRAL=1, AGAINST=-1
val score = preferCount * 2 + neutralCount * 1 - againstCount * 1
```

### Comment Threading
```kotlin
// Comments linked by parentCommentId
Comment(id="c1", content="Main comment")
Comment(id="c2", parentCommentId="c1", content="Reply")
Comment(id="c3", parentCommentId="c1", content="Another reply")
```

### Permission Guards
```kotlin
// Status-based access control
when (role) {
    ORGANIZER -> true // always
    PARTICIPANT -> event.status in [CONFIRMED, COMPARING]
    ADMIN -> true // always
}
```

### Conflict Resolution
```kotlin
// Last-write-wins strategy
store.recordVote(eventId, participantId, slotId, Vote.YES)
store.recordVote(eventId, participantId, slotId, Vote.MAYBE) // wins
```

## 📋 Checklist

- [x] 10+ tests implemented
- [x] GIVEN/WHEN/THEN structure
- [x] Mock repositories (no DB)
- [x] Multi-user simulation (1-4+ users)
- [x] Comment threading with parentId
- [x] Score calculation with weights
- [x] Permission-based access control
- [x] Last-write-wins conflict resolution
- [x] Status-based event gates
- [x] Cross-device sync simulation
- [x] Notification action tracking
- [x] Statistics aggregation

## 🎯 Next Steps

1. **Integration Tests** - Test with real SQLite database
2. **Performance Tests** - Stress test with 100+ concurrent users
3. **Network Simulation** - Add latency/packet loss scenarios
4. **CRDT Conflict Resolution** - Advanced conflict handling
5. **Notification Delivery** - Test actual notification routing
6. **Offline Queue** - Test action queuing when offline

## 📖 References

- **OpenSpec**: `openspec/specs/collaboration-management/spec.md`
- **Design System**: `.opencode/design-system.md`
- **Comment Models**: `shared/src/commonMain/kotlin/com/guyghost/wakeve/models/CommentModels.kt`
- **Scenario Models**: `shared/src/commonMain/kotlin/com/guyghost/wakeve/models/ScenarioModels.kt`
- **Event Models**: `shared/src/commonMain/kotlin/com/guyghost/wakeve/models/Event.kt`

---

**Created**: 2026-01-01  
**Status**: Ready for use ✅  
**Maintainer**: Test Agent 🤖
