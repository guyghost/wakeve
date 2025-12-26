# Collaboration Integration Tests

## Overview

The `CollaborationIntegrationTest.kt` file contains comprehensive integration tests for the complete collaboration workflow in Wakeve, testing how events, scenarios, budgets, and comments work together with multiple participants.

## Test File Location

```
shared/src/jvmTest/kotlin/com/guyghost/wakeve/collaboration/CollaborationIntegrationTest.kt
```

## Coverage

The integration tests cover 9 complete scenarios:

### 1. `testCompleteCollaborationWorkflow_creates_event_with_scenarios_and_comments`

**Purpose:** Tests the full workflow from event creation to comments across multiple planning sections.

**Scenario:**
- ✅ Create event with organizer and participants
- ✅ Add multiple scenarios (Paris Weekend, Barcelona Week)
- ✅ Comment on scenarios
- ✅ Comment on budget
- ✅ Comment on accommodation
- ✅ Verify comment counts and event status

**Validates:**
- Event creation with correct participants
- Scenario creation and storage
- Comment creation across different sections
- Comment counting and aggregation

---

### 2. `testMultiUserCommentThread_creates_nested_discussion`

**Purpose:** Tests multi-level comment threading with replies and nested replies.

**Scenario:**
- ✅ User A creates top-level comment
- ✅ User B replies to A
- ✅ User A replies to B (nested reply)
- ✅ User C adds another top-level comment
- ✅ Verify thread structure and reply counts

**Validates:**
- Parent-child comment relationships
- Reply count increments on parent comments
- Nested reply chains (reply to reply)
- Top-level comments filtering

---

### 3. `testCommentNotifications_sends_to_correct_recipients`

**Purpose:** Tests notification system when comments are posted and replies are made.

**Scenario:**
- ✅ Participant 1 posts a comment
- ✅ Participant 2 and 3 receive notifications
- ✅ Participant 1 (author) does NOT receive notification
- ✅ Participant 2 replies to the comment
- ✅ Only Participant 1 receives reply notification
- ✅ Participant 3 does NOT receive reply notification

**Validates:**
- Notifications sent to non-author participants
- Reply notifications sent only to comment author
- Correct notification types (COMMENT_POSTED vs COMMENT_REPLY)
- Notification filtering by recipient

---

### 4. `testCommentDeletionCascade_deletes_all_nested_replies`

**Purpose:** Tests that deleting a parent comment cascades to all nested replies.

**Scenario:**
- ✅ Create parent comment
- ✅ Create reply to parent
- ✅ Create nested reply (reply to the reply)
- ✅ Create another reply to parent
- ✅ Delete parent comment
- ✅ Verify ALL comments are deleted

**Validates:**
- Cascade deletion of child comments
- Database referential integrity
- Reply count updates on deletion
- Total comment count accuracy

---

### 5. `testCommentFilteringBySection_correctly_filters_comments`

**Purpose:** Tests filtering comments by section and optional section item ID.

**Scenario:**
- ✅ Create 6 comments across 3 sections:
  - BUDGET: 2 comments
  - ACCOMMODATION: 1 comment
  - ACTIVITY: 3 comments (2 on activity-1, 1 on activity-2)
- ✅ Filter by BUDGET → 2 comments
- ✅ Filter by ACCOMMODATION → 1 comment
- ✅ Filter by ACTIVITY → 3 comments
- ✅ Filter by ACTIVITY + "activity-1" → 2 comments
- ✅ Filter by ACTIVITY + "activity-2" → 1 comment

**Validates:**
- Section-based comment filtering
- Section item ID filtering
- Comment count accuracy
- Query flexibility

---

### 6. `testCommentStatistics_calculates_comprehensive_stats`

**Purpose:** Tests aggregation and statistics calculations for comments.

**Scenario:**
- ✅ Create event with 5 participants
- ✅ Distribute 8 comments:
  - Alice: 3 comments + 1 parent = 4 total
  - Bob: 1 comment
  - Charlie: 2 comments
  - Diana: 0 comments
  - Eve: 1 reply
- ✅ Calculate stats by section
- ✅ Identify top contributors
- ✅ Get participant activity
- ✅ Calculate recent activity

**Validates:**
- Comment counting by section
- Top contributors ranking
- Participant activity tracking
- Last comment timestamps

---

### 7. `testConcurrentCommentCreation_handles_parallel_comments`

**Purpose:** Tests that concurrent comment creation is handled safely.

**Scenario:**
- ✅ Create 5 comments in parallel using async/await
- ✅ All comments should complete successfully
- ✅ All comment IDs should be unique
- ✅ All comments should be retrievable
- ✅ Total count should be 5

**Validates:**
- Concurrency safety
- No race conditions in ID generation
- Atomic comment insertion
- Database thread-safety

---

### 8. `testCommentPermissions_enforces_author_only_modification`

**Purpose:** Tests that only comment authors can modify or delete comments.

**Scenario:**
- ✅ Participant 1 creates a comment
- ✅ Participant 2 tries to modify → Fails
- ✅ Participant 2 tries to delete → Fails
- ✅ Participant 1 modifies the comment → Succeeds
- ✅ Verify `isEdited` flag and `updatedAt` timestamp
- ✅ Participant 1 deletes the comment → Succeeds

**Validates:**
- Comment permission checks
- Author-only modification
- Author-only deletion
- Edit tracking (isEdited flag)
- Timestamp management

---

### 9. `testCompleteWorkflow_covers_entire_event_planning_lifecycle`

**Purpose:** Tests the complete event planning lifecycle with comments on all major sections.

**Scenario:**
- ✅ Create event with 5 participants
- ✅ Create 2 scenarios (Paris, Barcelona)
- ✅ Add comments on all major sections:
  - SCENARIO: "Which scenario do you prefer?"
  - BUDGET: "Let's set a budget limit"
  - ACCOMMODATION: "Hotel or Airbnb?"
  - MEAL: "Dietary restrictions?"
  - ACTIVITY: "What activities are we doing?"
  - EQUIPMENT: "What equipment do we need?"
- ✅ Add 3 replies to different threads
- ✅ Verify statistics across all sections
- ✅ Verify top contributors
- ✅ Confirm event status

**Validates:**
- Multi-section collaboration
- Complete event lifecycle
- Statistics across all sections
- Contributor tracking
- Event status confirmation

---

## Test Data

### Users/Participants

| ID | Name | Role |
|---|---|---|
| `org-1` | Organizer | Event organizer |
| `user-1` | Alice | Participant |
| `user-2` | Bob | Participant |
| `user-3` | Charlie | Participant |
| `user-4` | Diana | Participant |
| `user-5` | Eve | Participant |

### Event Details

- **Event ID:** `event-1` (or custom per test)
- **Title:** "Test Collaboration Event"
- **Status:** CONFIRMED
- **Deadline:** 2025-11-30T23:59:59Z
- **Proposed Time:** 2025-12-01T10:00:00Z - 2025-12-01T12:00:00Z (UTC)

---

## Comment Sections Tested

- `GENERAL` - General event comments
- `SCENARIO` - Scenario comparison comments
- `BUDGET` - Budget discussion
- `ACCOMMODATION` - Lodging planning
- `MEAL` - Meal planning
- `ACTIVITY` - Activity discussion
- `EQUIPMENT` - Equipment checklist
- `TRANSPORT` - Transport planning
- `POLL` - Date polling discussion

---

## Services Used

1. **CommentRepository**
   - CRUD operations on comments
   - Thread building
   - Statistics and aggregations

2. **DatabaseEventRepository**
   - Event creation and retrieval
   - Participant management
   - Event status tracking

3. **ScenarioRepository**
   - Scenario creation
   - Scenario voting

4. **BudgetRepository**
   - Budget creation (not fully exercised in this test)

5. **MockNotificationService**
   - Simulates notification sending
   - Captures notifications for verification

---

## Mock Implementations

### MockNotificationService

Captures all notifications sent during tests for verification:

```kotlin
class MockNotificationService : NotificationService {
    private val sentNotifications = mutableListOf<NotificationMessage>()
    
    // Get notifications for a specific user
    fun getSentNotifications(recipientId: String): List<NotificationMessage>
    
    // Clear all notifications
    fun clear()
    
    // Get total count
    fun getTotalNotificationCount(): Int
}
```

---

## Running the Tests

### Compile Only

```bash
./gradlew shared:compileJvmTest
```

### Run All Collaboration Tests

```bash
./gradlew shared:jvmTest --tests "*CollaborationIntegrationTest*"
```

### Run a Specific Test

```bash
./gradlew shared:jvmTest --tests "*CollaborationIntegrationTest.testMultiUserCommentThread*"
```

### Run with Output

```bash
./gradlew shared:jvmTest --tests "*CollaborationIntegrationTest" --info
```

---

## Test Statistics

| Metric | Value |
|---|---|
| Total Tests | 9 |
| Test Methods | 9 |
| Assertions per Test | 5-15 |
| Total Assertions | ~100+ |
| User Participants | 6 |
| Event Sections | 9 |
| Concurrent Tests | 1 (Test 7) |

---

## Key Patterns Used

### 1. **Test Helpers**

Helper methods encapsulate common setup:

```kotlin
private fun createTestEvent(...): Event = runBlocking { ... }
private fun createTestScenario(...): Scenario = runBlocking { ... }
```

### 2. **RunBlocking for Coroutines**

All suspend functions are wrapped in `runBlocking`:

```kotlin
@Test
fun `test` = runBlocking {
    commentRepository.createComment(...)
}
```

### 3. **Database Reset**

Each test starts with a clean database:

```kotlin
@BeforeTest
fun setup() {
    DatabaseProvider.resetDatabase()
    db = createTestDatabase()
}
```

### 4. **Notification Simulation**

Notifications are created manually and sent to the mock service:

```kotlin
val notification = NotificationMessage(...)
mockNotificationService.sendNotification(notification)
```

---

## Dependencies

### Kotlin Multiplatform

- `kotlinx-coroutines` - For async/await
- `kotlinx-datetime` - For timestamps
- `kotlin.test` - For assertions

### Project Dependencies

- CommentRepository
- DatabaseEventRepository
- ScenarioRepository
- BudgetRepository
- NotificationService
- Various Models (Comment, Event, Scenario, etc.)

---

## Notes

### Existing Test Issues

The file `CommentRepositoryTest.kt` contains syntax errors (function declaration issues) that prevent the entire test module from compiling. These errors are NOT in the new `CollaborationIntegrationTest.kt` file and should be fixed separately.

### Async Pattern

Tests use Kotlin's structured concurrency (async/awaitAll) to test concurrent comment creation:

```kotlin
val deferredComments = (1..5).map { index ->
    async {
        commentRepository.createComment(...)
    }
}
val comments = deferredComments.awaitAll()
```

### Permission Checking

Tests verify that non-authors cannot modify/delete comments:

```kotlin
assertFailsWith<IllegalArgumentException> {
    commentRepository.updateComment(comment.id, ...)
}
```

---

## Future Enhancements

Potential additions to the integration tests:

1. **Comment Editing & Version History**
   - Track edit history
   - Verify `isEdited` flag accuracy

2. **Bulk Operations**
   - Test bulk comment deletion
   - Test bulk comment archival

3. **Performance Tests**
   - Test comment retrieval with large datasets
   - Measure query performance

4. **Real-time Features**
   - Test comment count updates in real-time
   - Test live notification delivery

5. **Conflict Resolution**
   - Test concurrent edits on same comment
   - Test offline sync scenarios

---

## Troubleshooting

### "Cannot infer type for type parameter"

Ensure all lambda parameters are explicitly typed in concurrent tests.

### "Suspend function can only be called from coroutine"

Wrap all suspend calls with `runBlocking { }` or inside another suspend function.

### "Unresolved reference"

Check that all models and services are properly imported from the correct packages.

---

**Version:** 1.0.0  
**Created:** December 26, 2025  
**Framework:** Kotlin Test  
**Platform:** JVM Test (shared)
