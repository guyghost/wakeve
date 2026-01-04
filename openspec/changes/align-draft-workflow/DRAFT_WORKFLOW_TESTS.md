# DraftWorkflowIntegrationTest - Documentation Complète

## Vue d'ensemble

`DraftWorkflowIntegrationTest` couvre l'intégralité du workflow DRAFT orchestré par `EventManagementStateMachine`. 

Les tests valident :
- ✅ Le cycle de vie complet de création d'événement (4 étapes du wizard)
- ✅ La persistance (auto-save) à chaque transition d'étape
- ✅ La validation stricte des données
- ✅ Le support des champs optionnels
- ✅ La récupération après interruption
- ✅ La gestion des locations et créneaux multiples

## Emplacement

```
shared/src/commonTest/kotlin/com/guyghost/wakeve/workflow/DraftWorkflowIntegrationTest.kt
```

## 8 Scénarios de Test

### Test 1: Complete DRAFT Wizard Flow
```kotlin
fun `complete draft wizard flow should create event with all fields`()
```

**Scenario GIVEN-WHEN-THEN:**
- GIVEN: A new DRAFT event
- WHEN: User fills all 4 wizard steps and creates the event
  - Step 1: Basic Info (title="Team Retreat 2025", eventType=TEAM_BUILDING)
  - Step 2: Participants (min=15, max=30, expected=22)
  - Step 3: Locations (Paris, London)
  - Step 4: TimeSlots (already in Step 1)
- THEN: Event is persisted to repository with all fields

**Assertions:**
- savedEvent.title == "Team Retreat 2025"
- savedEvent.eventType == TEAM_BUILDING
- savedEvent.minParticipants == 15
- savedEvent.maxParticipants == 30
- savedEvent.expectedParticipants == 22
- repository.getLocationsByEvent("evt-1").size == 2

---

### Test 2: Auto-save at Each Step
```kotlin
fun `auto-save should persist event after each step transition`()
```

**Scenario:**
- GIVEN: User filling out the wizard
- WHEN: User completes Step 1, then Step 2
- THEN: Both updates are persisted to repository

**Assertions:**
- After Step 1: savedEvent.title == "Meeting"
- After Step 2: savedEvent.minParticipants == 5

---

### Test 3: Validation Blocks Invalid Data
```kotlin
fun `validation should prevent empty title`()
```

**Scenario:**
- GIVEN: Invalid event data (empty title)
- WHEN: CreateEventUseCase is invoked
- THEN: Validation rejects the event (not saved)

**Assertions:**
- repository.getEvent("evt-1") == null (event not created)

---

### Test 4: Skip Optional Fields
```kotlin
fun `minimal event creation should succeed with only required fields`()
```

**Scenario:**
- GIVEN: Minimal event data (no participants, no locations)
- WHEN: Event is created with only: title + description + timeSlots
- THEN: Event is created successfully (optional fields are null)

**Assertions:**
- savedEvent.minParticipants == null
- savedEvent.maxParticipants == null
- savedEvent.expectedParticipants == null
- savedEvent.proposedSlots.isNotEmpty()

---

### Test 5: Full Data Creation
```kotlin
fun `full event creation with all optional fields should persist correctly`()
```

**Scenario:**
- GIVEN: All optional fields are filled
- WHEN: Event is created with eventType, participants, custom fields
- THEN: All fields are persisted correctly

**Assertions:**
- savedEvent.eventType == BIRTHDAY
- savedEvent.minParticipants == 10
- savedEvent.maxParticipants == 50
- savedEvent.expectedParticipants == 30

---

### Test 6: Recovery After Interruption
```kotlin
fun `event should be recoverable after interruption in step 2`()
```

**Scenario:**
- GIVEN: User fills Step 1 and Step 2, then closes app
- WHEN: App is reopened and event is reloaded from repository
- THEN: User can resume with all data intact

**Assertions:**
- reloadedEvent.title == "Conference"
- reloadedEvent.minParticipants == 20
- reloadedEvent.expectedParticipants == 100

---

### Test 7: Add and Remove Locations
```kotlin
fun `add and remove potential locations should update event correctly`()
```

**Scenario:**
- GIVEN: Event in Step 3 (Potential Locations)
- WHEN: User adds 3 locations, then removes 1
- THEN: Repository contains exactly 2 locations

**Assertions:**
- After adding 3: locations.size == 3
- After removing 1: locations.size == 2
- Removed location is not in final list

---

### Test 8: Multiple Time Slots with Different TimeOfDay
```kotlin
fun `multiple time slots with different time of day should be persisted`()
```

**Scenario:**
- GIVEN: Event creation with multiple time slots
- WHEN: Three slots with MORNING, AFTERNOON, EVENING are added
- THEN: All slots are persisted with correct timeOfDay values

**Assertions:**
- savedEvent.proposedSlots.size == 3
- savedEvent.proposedSlots[0].timeOfDay == MORNING
- savedEvent.proposedSlots[1].timeOfDay == AFTERNOON
- savedEvent.proposedSlots[2].timeOfDay == EVENING

---

## Architecture & Implementation

### Mock Repository

```kotlin
class MockEventRepository : EventRepositoryInterface {
    var events = mutableMapOf<String, Event>()
    var potentialLocations = mutableMapOf<String, MutableList<PotentialLocation>>()
    // ... implementations
}
```

- In-memory storage (no database)
- Implements full EventRepositoryInterface contract
- Supports location management

### State Machine Creation

```kotlin
private fun createStateMachine(repository: MockEventRepository): EventManagementStateMachine {
    val testDispatcher = StandardTestDispatcher()
    val scope = CoroutineScope(testDispatcher + SupervisorJob())
    val loadEventsUseCase = LoadEventsUseCase(repository)
    val createEventUseCase = CreateEventUseCase(repository)
    
    return EventManagementStateMachine(
        loadEventsUseCase = loadEventsUseCase,
        createEventUseCase = createEventUseCase,
        eventRepository = repository,
        scope = scope
    )
}
```

- Real EventManagementStateMachine (not mocked)
- Real use cases (LoadEventsUseCase, CreateEventUseCase)
- StandardTestDispatcher for deterministic async execution

### Test Pattern

Each test follows the **AAA Pattern** (Arrange, Act, Assert):

```kotlin
@Test
fun `test scenario`() = runTest {
    // Arrange
    val repository = MockEventRepository()
    val stateMachine = createStateMachine(repository)
    
    // Act
    val event = createTestEvent(...)
    stateMachine.dispatch(EventManagementContract.Intent.CreateEvent(event))
    advanceUntilIdle()
    
    // Assert
    val savedEvent = repository.getEvent("evt-1")
    assertNotNull(savedEvent)
    assertEquals("expected", savedEvent?.title)
}
```

---

## Test Helpers

### createTestEvent()
Creates a complete Event with sensible defaults for testing.

```kotlin
private fun createTestEvent(
    id: String = "evt-1",
    title: String = "Team Retreat",
    description: String = "Annual team building event",
    eventType: EventType = EventType.TEAM_BUILDING,
    minParticipants: Int? = null,
    maxParticipants: Int? = null,
    expectedParticipants: Int? = null,
    proposedSlots: List<TimeSlot> = listOf(...)
): Event
```

---

## Execution

### Running Tests

```bash
# Run only DraftWorkflowIntegrationTest
./gradlew shared:jvmTest -Dkotlin.tests.filter="*DraftWorkflowIntegration*"

# Run all shared tests
./gradlew shared:jvmTest
```

### Expected Result

All 8 tests should pass:
```
✓ complete draft wizard flow should create event with all fields
✓ auto-save should persist event after each step transition
✓ validation should prevent empty title
✓ minimal event creation should succeed with only required fields
✓ full event creation with all optional fields should persist correctly
✓ event should be recoverable after interruption in step 2
✓ add and remove potential locations should update event correctly
✓ multiple time slots with different time of day should be persisted

8 passed
```

---

## Coverage Summary

| Aspect | Coverage |
|--------|----------|
| **Workflow Steps** | All 4 steps (Basic Info → Participants → Locations → TimeSlots) |
| **Persistence** | Auto-save at each step ✅ |
| **Validation** | Invalid data rejected ✅ |
| **Optional Fields** | All tested (skip some, fill all) ✅ |
| **Recovery** | State preservation after interruption ✅ |
| **Location Management** | Add/remove tested ✅ |
| **Time Slots** | Multiple slots with flexible timeOfDay ✅ |
| **Happy Path** | Complete workflow ✅ |
| **Edge Cases** | Minimal data, full data, invalid data ✅ |

---

## Notes for Future Work

### When Adding New Tests

1. Follow the **AAA Pattern** (Arrange, Act, Assert)
2. Use **MockEventRepository** for isolation
3. Use **StandardTestDispatcher** for determinism
4. Document each test with GIVEN-WHEN-THEN scenario
5. Verify both **state changes** and **repository persistence**

### When Extending the Workflow

1. Add corresponding test case to `DraftWorkflowIntegrationTest`
2. Update MockEventRepository if new repository methods are needed
3. Ensure test covers happy path + edge cases
4. Update this documentation

---

## References

- **EventManagementStateMachine**: `shared/src/commonMain/kotlin/.../EventManagementStateMachine.kt`
- **EventManagementContract**: `shared/src/commonMain/kotlin/.../EventManagementContract.kt`
- **Event Model**: `shared/src/commonMain/kotlin/com/guyghost/wakeve/models/Event.kt`
- **DRAFT Workflow Spec**: `openspec/changes/align-draft-workflow/specs/workflow-coordination/spec.md`
