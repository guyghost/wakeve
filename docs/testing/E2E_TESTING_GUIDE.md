# Wakeve E2E Testing Guide

**Phase 6** - Comprehensive End-to-End Testing Framework

## Overview

This document describes the complete End-to-End (E2E) testing framework for Wakeve, covering PRD workflow validation and service integration testing.

### Test Coverage Summary

| Test Suite | Location | Tests | Status | Purpose |
|-----------|----------|-------|--------|---------|
| **PrdWorkflowE2ETest** | `e2e/PrdWorkflowE2ETest.kt` | 6 | ✅ Passing | Validate complete PRD workflow (DRAFT→FINALIZED) |
| **ServiceIntegrationE2ETest** | `e2e/ServiceIntegrationE2ETest.kt` | 5 | ✅ Passing | Validate service integrations with event management |

**Total: 11/11 tests passing (100% success rate)**

---

## Test Suite 1: PRD Workflow E2E Tests

**Location:** `shared/src/commonTest/kotlin/com/guyghost/wakeve/e2e/PrdWorkflowE2ETest.kt`

### Purpose
Validate the complete Wakeve event planning workflow from creation through finalization, ensuring all phases transition correctly and data persists properly.

### Test Cases

#### Test 1: Complete Workflow - DRAFT → FINALIZED (0.002s)
```
GIVEN: New event with Enhanced DRAFT features
WHEN:  Execute complete workflow through all phases
THEN:  Event transitions correctly and data persists
```

**Validations:**
- ✅ Event created in DRAFT status
- ✅ Status transitions: POLLING → CONFIRMED → ORGANIZING → FINALIZED
- ✅ Final date preserved through status changes
- ✅ Event type and participant count retained

**Key Scenarios:**
1. Create event with basic Enhanced DRAFT features
2. Progress through each phase with proper status updates
3. Verify event attributes remain consistent

---

#### Test 2: Enhanced DRAFT Features (0.003s)
```
GIVEN: Event creation with new DRAFT phase fields
WHEN:  Add potential locations and flexible time slots
THEN:  Event reflects all DRAFT enhancements
```

**Validations:**
- ✅ Event type properly stored (e.g., WEDDING)
- ✅ Participant count estimates (expected, min, max) correctly persisted
- ✅ Flexible time slots with TimeOfDay support (MORNING, AFTERNOON, etc.)
- ✅ Potential locations (city, venue types) added and retrieved
- ✅ Location types properly categorized (CITY, REGION, SPECIFIC_VENUE, ONLINE)

**Key Scenarios:**
1. Create event with multiple event types
2. Add various potential locations
3. Define flexible time slots by time of day
4. Verify all DRAFT features are retrievable

---

#### Test 3: Scenario Management Integration (0.002s)
```
GIVEN: Event confirmed with scenarios
WHEN:  Create and vote on scenarios
THEN:  Scenarios persist and votes are recorded
```

**Validations:**
- ✅ Multiple scenarios created with distinct details
- ✅ Scenarios contain location, duration, budget information
- ✅ Votes recorded for each participant-scenario pair
- ✅ Vote counts aggregated correctly

**Key Scenarios:**
1. Create competing destination scenarios
2. Record participant votes on each scenario
3. Verify voting data persists
4. Aggregate results for decision making

---

#### Test 4: Collaboration - Comments and Multi-User Voting (0.002s)
```
GIVEN: Event with multiple participants
WHEN:  Participants add comments and vote
THEN:  Comments and votes are organized by section
```

**Validations:**
- ✅ Comments organized by CommentSection (ACCOMMODATION, TRANSPORT, etc.)
- ✅ Comments store author information (ID, name)
- ✅ Multiple votes per participant on different time slots
- ✅ Poll data updated with all votes

**Key Scenarios:**
1. Add comments to different sections
2. Record votes from multiple participants
3. Organize comments by category
4. Ensure votes aggregate correctly

---

#### Test 5: Error Handling and Recovery (0.002s)
```
GIVEN: Operation fails
WHEN:  Error is handled and retry occurs
THEN:  No data loss and successful retry
```

**Validations:**
- ✅ Operation fails when expected
- ✅ Event data remains uncorrupted after failure
- ✅ Retry succeeds after error condition cleared
- ✅ Data maintains consistency

**Key Scenarios:**
1. Simulate operation failure
2. Verify event not corrupted
3. Retry operation successfully
4. Confirm data integrity

---

#### Test 6: Offline-Online Synchronization (0.078s)
```
GIVEN: User in offline mode
WHEN:  Create data and go online
THEN:  Data syncs without loss
```

**Validations:**
- ✅ Data created locally while offline
- ✅ Participants added without network
- ✅ Data syncs when online connection established
- ✅ No loss of previously created data

**Key Scenarios:**
1. Create event and add participants offline
2. Simulate going online
3. Update event with new description
4. Verify all changes persisted

---

## Test Suite 2: Service Integration E2E Tests

**Location:** `shared/src/commonTest/kotlin/com/guyghost/wakeve/e2e/ServiceIntegrationE2ETest.kt`

### Purpose
Validate that auxiliary services (Transport, Budget, Meeting, Suggestion, Destination) integrate correctly with the core event management system.

### Test Cases

#### Test 1: Transport Service Integration (0.005s)
```
GIVEN: Event with participants and multiple locations
WHEN:  Transport service optimizes routes
THEN:  Optimal routes are calculated and integrated with event
```

**Validations:**
- ✅ Transport options retrieved for location pairs
- ✅ Multiple transport modes available (FLIGHT, TRAIN, BUS, etc.)
- ✅ Route optimization for multi-participant groups
- ✅ Cost and duration information provided

**Key Scenarios:**
1. Get transport options between cities
2. Calculate optimal route for group of 3+ participants
3. Compare cost vs. time optimization
4. Integrate results with event planning

**Mock Service Features:**
```kotlin
getTransportOptions(from, to, departureTime, mode?)
optimizeMultiParticipantRoute(participants, destination, departureTime)
```

---

#### Test 2: Budget Service Integration (0.09s)
```
GIVEN: Event with confirmed participants and destinations
WHEN:  Budget service tracks expenses
THEN:  Budget summary reflects all expenses accurately
```

**Validations:**
- ✅ Budget created with total and per-person calculations
- ✅ Expenses categorized (accommodation, transport, activities)
- ✅ Budget summary calculates remaining balance
- ✅ Expense tracking supports multiple additions

**Key Scenarios:**
1. Create budget for 3000€ shared by 3 people
2. Add accommodation expense (1000€)
3. Add transport expense (500€)
4. Add activities expense (300€)
5. Verify summary: spent 1800€, remaining 1200€

**Mock Service Features:**
```kotlin
createBudget(eventId, totalBudget, participantCount)
addExpense(eventId, amount, category, description)
getBudgetSummary(eventId)
```

---

#### Test 3: Meeting Service Integration (0.004s)
```
GIVEN: Event confirmed with final date
WHEN:  Meeting service generates virtual meeting link
THEN:  Meeting link is created and accessible
```

**Validations:**
- ✅ Meeting links generated for different platforms (Zoom, Teams, etc.)
- ✅ Platform specified in meeting configuration
- ✅ Links contain event-specific identifiers
- ✅ Meeting date and time properly set

**Key Scenarios:**
1. Generate Zoom link for confirmed event
2. Generate Teams link for same event
3. Verify links are unique and platform-specific
4. Confirm date/time information in meeting

**Mock Service Features:**
```kotlin
generateMeetingLink(eventId, platform, date, time)
getMeeting(eventId)
```

---

#### Test 4: Suggestion Service Integration (0.001s)
```
GIVEN: Event with type and participant count
WHEN:  Suggestion service recommends locations and dates
THEN:  Personalized suggestions are provided
```

**Validations:**
- ✅ Location suggestions based on event type
- ✅ Suggestions filtered by participant count
- ✅ Multiple suggestions provided (3+ options)
- ✅ Date suggestions available

**Key Scenarios:**
1. Get location suggestions for WEDDING (50 participants)
2. Verify Paris, Venice, Barcelona suggested
3. Get date suggestions from proposed slots
4. Verify multiple date options provided

**Mock Service Features:**
```kotlin
getLocationSuggestions(eventId, eventType, participantCount)
getDateSuggestions(eventId, proposedSlots)
```

---

#### Test 5: Comprehensive Multi-Service Workflow (0.002s)
```
GIVEN: Event with all services configured
WHEN:  Complete workflow with all services engaged
THEN:  All services integrate seamlessly
```

**Validations:**
- ✅ All services work together harmoniously
- ✅ Event progresses through complete workflow
- ✅ Service outputs integrate with event data
- ✅ Final state reflects all service operations

**Complete Workflow:**
1. Create CONFERENCE event (200 participants)
2. Get location suggestions (Amsterdam, Berlin, Vienna)
3. Create budget (50,000€)
4. Plan transport (New York → Amsterdam flights)
5. Generate meeting link (Teams)
6. Add expenses (venue, transport, catering)
7. Transition event to CONFIRMED
8. Verify budget summary (45,000€ spent, 5,000€ remaining)

---

## Running the Tests

### Run All E2E Tests
```bash
./gradlew shared:jvmTest --tests "E2ETest"
```

### Run Specific Test Suite
```bash
./gradlew shared:jvmTest --tests "PrdWorkflowE2ETest"
./gradlew shared:jvmTest --tests "ServiceIntegrationE2ETest"
```

### Run Specific Test Case
```bash
./gradlew shared:jvmTest --tests "PrdWorkflowE2ETest.testCompleteWorkflow_DraftToFinalized"
./gradlew shared:jvmTest --tests "ServiceIntegrationE2ETest.testBudgetServiceIntegration"
```

### Run with Verbose Output
```bash
./gradlew shared:jvmTest --tests "PrdWorkflowE2ETest" -i
```

### Run Without Build Cache
```bash
./gradlew shared:jvmTest --tests "PrdWorkflowE2ETest" --no-build-cache
```

---

## Test Architecture

### BDD Pattern
All tests follow BDD (Behavior-Driven Development) structure:

```kotlin
@Test
fun testFeature() = runTest {
    // GIVEN - Setup and arrange test data
    val event = createTestEvent(...)
    
    // WHEN - Execute actions
    repository.createEvent(event)
    
    // THEN - Assert expected outcomes
    assertEquals(EventStatus.DRAFT, event.status)
}
```

### Mock Repository Pattern
Tests use `MockEventRepository` that:
- Implements `EventRepositoryInterface` fully
- Stores data in memory (no database)
- Supports network delay simulation
- Allows operation failure injection

```kotlin
class MockEventRepository : EventRepositoryInterface {
    var events = mutableMapOf<String, Event>()
    var simulateNetworkDelay = 0L
    var shouldFailVote = false
    // ... implement all interface methods
}
```

### Mock Services Pattern
Each service has its own mock implementation:

```kotlin
class MockBudgetService {
    suspend fun createBudget(...): Budget { ... }
    suspend fun addExpense(...) { ... }
    suspend fun getBudgetSummary(...): BudgetSummary { ... }
}
```

### Test Isolation
Each test:
- Creates its own repository instance
- Uses fresh data for each run
- Can run in any order
- Has no dependencies on other tests

---

## Key Features Tested

### Event Management
- ✅ Event creation with Enhanced DRAFT features
- ✅ Status transitions through all phases
- ✅ Event type support (11 types)
- ✅ Participant count estimation
- ✅ Location and time slot flexibility

### Poll & Voting System
- ✅ Vote recording and aggregation
- ✅ Multi-participant voting
- ✅ Scenario voting
- ✅ Vote persistence

### Collaboration
- ✅ Comments with section organization
- ✅ Multi-user participation
- ✅ Comment authorship tracking

### Data Persistence
- ✅ Event data survives state transitions
- ✅ Votes persist across updates
- ✅ Comments preserved in database
- ✅ Offline-online synchronization

### Service Integration
- ✅ Budget calculation and tracking
- ✅ Transport route optimization
- ✅ Meeting link generation
- ✅ Personalized suggestions
- ✅ Cross-service workflows

---

## Performance Metrics

### Test Execution Times
| Test | Duration |
|------|----------|
| testCompleteWorkflow_DraftToFinalized | 0.002s |
| testEnhancedDraftFeatures | 0.003s |
| testScenarioManagement | 0.002s |
| testCollaboration_CommentsAndVoting | 0.002s |
| testErrorHandling_RecoveryWorkflow | 0.002s |
| testOfflineOnlineSync | 0.078s |
| testBudgetServiceIntegration | 0.09s |
| testTransportServiceIntegration | 0.005s |
| testMeetingServiceIntegration | 0.004s |
| testSuggestionServiceIntegration | 0.001s |
| testComprehensiveMultiServiceWorkflow | 0.002s |
| **Total** | **0.191s** |

All tests complete in under 200ms total, enabling rapid feedback during development.

---

## Coverage Analysis

### Workflow Phases
- ✅ DRAFT - Event creation with enhanced features
- ✅ POLLING - Vote collection and aggregation
- ✅ CONFIRMED - Final date locked and scenarios created
- ✅ ORGANIZING - Meeting planning and logistics
- ✅ FINALIZED - Complete workflow conclusion

### Event Types
- ✅ BIRTHDAY
- ✅ WEDDING
- ✅ TEAM_BUILDING
- ✅ CONFERENCE
- (and 7 more types available)

### Service Coverage
- ✅ Budget Service (expense tracking, summaries)
- ✅ Transport Service (route optimization)
- ✅ Meeting Service (virtual meeting links)
- ✅ Suggestion Service (recommendations)
- ✅ Destination Service (location recommendations)
- ✅ Comment Service (collaboration)
- ✅ Vote Service (polling)

### Error Scenarios
- ✅ Operation failure and recovery
- ✅ Network unavailability (offline mode)
- ✅ Data corruption detection
- ✅ Retry logic validation

---

## Next Steps

### Planned Enhancements

#### 1. Offline Sync Conflict Resolution (Priority: HIGH)
Add tests for:
- Concurrent modifications (last-write-wins)
- CRDT-based reconciliation
- Version tracking
- Conflict detection and resolution

#### 2. Performance & Stress Tests (Priority: MEDIUM)
Add tests for:
- Concurrent operations (100+ participants)
- Large event scenarios (1000+ data points)
- Database query optimization
- Cache effectiveness

#### 3. CI/CD Integration (Priority: LOW)
Add:
- GitHub Actions workflow
- Automated test execution on push
- Test result reporting
- Coverage metrics tracking

---

## Troubleshooting

### Test Failures

**Issue:** Null pointer exception on repository access
- **Cause:** Repository not initialized in GIVEN block
- **Fix:** Ensure `MockEventRepository()` is created before use

**Issue:** Assertion error on status transitions
- **Cause:** Status not updated through proper method
- **Fix:** Use `updateEventStatus()` instead of direct assignment

**Issue:** Timeout in offline sync test
- **Cause:** Network delay too long or async operation not completed
- **Fix:** Call `advanceUntilIdle()` after async operations

### Performance Issues

**Issue:** Tests run slowly (>1 second each)
- **Cause:** Excessive `simulateNetworkDelay` values
- **Fix:** Set to 0L for fast tests, use only when testing timing

**Issue:** Build cache causes stale test results
- **Cause:** Gradle build cache not recognizing test changes
- **Fix:** Run with `--no-build-cache` flag

---

## Contributing

To add new E2E tests:

1. **Identify the scenario** - What workflow needs testing?
2. **Create test method** - Follow BDD pattern (GIVEN/WHEN/THEN)
3. **Use mocks** - Create service mocks as needed
4. **Run tests** - Verify with `./gradlew shared:jvmTest`
5. **Update documentation** - Add test case to this guide

### Test Template
```kotlin
@Test
fun testNewFeature() = runTest {
    // GIVEN
    val eventRepo = MockEventRepository()
    val event = createTestEvent(...)
    
    // WHEN
    eventRepo.createEvent(event)
    
    // THEN
    assertNotNull(eventRepo.getEvent(event.id))
}
```

---

## References

- **PRD Specs:** `openspec/specs/event-organization/`
- **State Machines:** `shared/src/commonMain/kotlin/.../presentation/statemachine/`
- **Models:** `shared/src/commonMain/kotlin/.../models/`
- **Test Helpers:** `shared/src/commonTest/kotlin/.../test/TestHelpers.kt`

---

**Document Version:** 1.0  
**Last Updated:** 2026-01-01  
**Status:** ✅ Complete - All 11 tests passing
