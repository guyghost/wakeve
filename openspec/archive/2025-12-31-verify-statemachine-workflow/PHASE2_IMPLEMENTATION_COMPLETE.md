# Phase 2 Implementation Complete: Contract Modifications

**Date**: 2025-12-31  
**Change ID**: `verify-statemachine-workflow`  
**Status**: Phase 2 Complete ✅ | Phase 3 Next

---

## 🎯 What We Accomplished

We successfully modified all 3 Contract files to add missing Intents, State fields, and helper methods for workflow coordination. The changes are now ready for Phase 3 implementation in the StateMachines.

---

## ✅ Files Modified

### 1. EventManagementContract.kt

**Location**: `shared/src/commonMain/kotlin/com/guyghost/wakeve/presentation/state/EventManagementContract.kt`

#### State Changes
- ✅ Added `scenariosUnlocked: Boolean = false`
- ✅ Added `meetingsUnlocked: Boolean = false`
- ✅ Added helper method `canAccessScenarios(): Boolean`
- ✅ Added helper method `canAccessMeetings(): Boolean`

#### New Intents Added (4)
1. ✅ **StartPoll(eventId)** - Transition DRAFT → POLLING
2. ✅ **ConfirmDate(eventId, slotId)** - Transition POLLING → CONFIRMED + navigate to scenarios
3. ✅ **TransitionToOrganizing(eventId)** - Transition CONFIRMED → ORGANIZING + navigate to meetings
4. ✅ **MarkAsFinalized(eventId)** - Transition ORGANIZING → FINALIZED

---

### 2. ScenarioManagementContract.kt

**Location**: `shared/src/commonMain/kotlin/com/guyghost/wakeve/presentation/state/ScenarioManagementContract.kt`

#### State Changes
- ✅ Added import `com.guyghost.wakeve.models.EventStatus`
- ✅ Added `eventStatus: EventStatus? = null`
- ✅ Added helper method `canCreateScenarios(): Boolean`
- ✅ Added helper method `canSelectScenarioAsFinal(): Boolean`

#### New Intents Added (1)
1. ✅ **SelectScenarioAsFinal(eventId, scenarioId)** - Select scenario as final choice, transition COMPARING → CONFIRMED

#### Intent Clarifications
- ✅ Updated `SelectScenario` documentation to clarify it's for navigation/viewing only

---

### 3. MeetingManagementContract.kt

**Location**: `shared/src/commonMain/kotlin/com/guyghost/wakeve/presentation/state/MeetingManagementContract.kt`

#### State Changes
- ✅ Added import `com.guyghost.wakeve.models.EventStatus`
- ✅ Added `eventStatus: EventStatus? = null`
- ✅ Added helper method `canCreateMeetings(): Boolean`

#### Intents
- ✅ No new intents needed (existing intents are complete)

---

## 📊 Compilation Status

### ✅ Contract Compilation
```bash
./gradlew shared:compileKotlinMetadata
# Result: BUILD SUCCESSFUL ✅
```

### ⚠️ Full Build Status (Expected)
```bash
./gradlew shared:build
# Result: BUILD FAILED ❌ (Expected - StateMachines need updating)
```

**Errors (Expected)**:
```
EventManagementStateMachine.kt:94:9 'when' expression must be exhaustive.
Add the 'is ConfirmDate', 'is MarkAsFinalized', 'is StartPoll', 'is TransitionToOrganizing' branches

ScenarioManagementStateMachine.kt:126:9 'when' expression must be exhaustive.
Add the 'is SelectScenarioAsFinal' branch
```

**Why This Is Expected**:
- Phase 2 focused on modifying Contracts
- Phase 3 will implement handlers in StateMachines
- Kotlin's exhaustive `when` ensures we don't forget to handle new Intents

---

## 🎯 Next Steps: Phase 3 Implementation

### Immediate Tasks

#### 1. Implement EventManagementStateMachine Handlers

**File**: `shared/src/commonMain/kotlin/com/guyghost/wakeve/presentation/statemachine/EventManagementStateMachine.kt`

**Line 94**: Add handlers for 4 new Intents:

```kotlin
when (intent) {
    // ... existing handlers
    
    is Intent.StartPoll -> {
        // Load event
        // Validate: event.status == DRAFT && isOrganizer
        // Update event.status = POLLING
        // Save to repository
        // Update state with new status
    }
    
    is Intent.ConfirmDate -> {
        // Load event
        // Validate: event.status == POLLING && isOrganizer
        // Validate: at least one participant voted
        // Update event.status = CONFIRMED
        // Set selectedSlot
        // Save to repository
        // Emit SideEffect.NavigateTo("scenarios/${eventId}")
        // Update state with scenariosUnlocked = true
    }
    
    is Intent.TransitionToOrganizing -> {
        // Load event
        // Validate: event.status == CONFIRMED && isOrganizer
        // Validate: at least one scenario selected
        // Update event.status = ORGANIZING
        // Save to repository
        // Emit SideEffect.NavigateTo("meetings/${eventId}")
        // Update state with meetingsUnlocked = true
    }
    
    is Intent.MarkAsFinalized -> {
        // Load event
        // Validate: event.status == ORGANIZING && isOrganizer
        // Validate: all critical details confirmed
        // Update event.status = FINALIZED
        // Save to repository
        // Emit SideEffect.ShowToast("Event finalized!")
    }
}
```

#### 2. Implement ScenarioManagementStateMachine Handler

**File**: `shared/src/commonMain/kotlin/com/guyghost/wakeve/presentation/statemachine/ScenarioManagementStateMachine.kt`

**Line 126**: Add handler for 1 new Intent:

```kotlin
when (intent) {
    // ... existing handlers
    
    is Intent.SelectScenarioAsFinal -> {
        // Load event
        // Validate: eventStatus == COMPARING && isOrganizer
        // Load scenario
        // Update scenario.status = SELECTED
        // Save scenario to repository
        // Update event.status = CONFIRMED (in EventRepository)
        // Emit SideEffect.ShowToast("Scenario selected!")
        // Emit SideEffect.NavigateTo("meetings/${intent.eventId}")
    }
}
```

#### 3. Implement Shared Repository Pattern

Each state machine should observe the repository for Event.status changes:

```kotlin
// In each StateMachine init/constructor
eventRepository.getEvent(eventId).collect { event ->
    // Update local state with event.status
    _state.update { it.copy(eventStatus = event.status) }
    
    // React to status changes
    when (event.status) {
        EventStatus.CONFIRMED -> {
            // ScenarioManagement: unlock scenario creation
            // MeetingService: prepare for meetings
        }
        // ... other statuses
    }
}
```

---

## 📝 Implementation Guidelines

### Validation Rules

Each handler MUST validate:
1. **Event Status**: Is the event in the correct status for this transition?
2. **User Role**: Is the current user authorized (organizer vs participant)?
3. **Business Rules**: Are all prerequisites met (e.g., at least one vote)?

### Error Handling

```kotlin
try {
    // Operation
} catch (e: InvalidStatusException) {
    _state.update { it.copy(error = "Cannot perform this action in current status") }
    _sideEffect.emit(SideEffect.ShowError("Invalid status"))
} catch (e: UnauthorizedException) {
    _state.update { it.copy(error = "Only organizer can perform this action") }
    _sideEffect.emit(SideEffect.ShowError("Unauthorized"))
}
```

### State Updates

Always update state atomically:

```kotlin
_state.update { currentState ->
    currentState.copy(
        isLoading = false,
        eventStatus = newStatus,
        scenariosUnlocked = true,
        error = null
    )
}
```

---

## 🧪 Testing Strategy

### Unit Tests to Create

#### EventManagementStateMachineTest.kt

```kotlin
@Test
fun `StartPoll transitions DRAFT to POLLING`()

@Test
fun `ConfirmDate transitions POLLING to CONFIRMED and navigates to scenarios`()

@Test
fun `TransitionToOrganizing transitions CONFIRMED to ORGANIZING and navigates to meetings`()

@Test
fun `MarkAsFinalized transitions ORGANIZING to FINALIZED`()

@Test
fun `StartPoll fails if not organizer`()

@Test
fun `ConfirmDate fails if no votes`()
```

#### ScenarioManagementStateMachineTest.kt

```kotlin
@Test
fun `SelectScenarioAsFinal transitions COMPARING to CONFIRMED`()

@Test
fun `SelectScenarioAsFinal navigates to meetings`()

@Test
fun `SelectScenarioAsFinal fails if not organizer`()

@Test
fun `canCreateScenarios returns true only in COMPARING and CONFIRMED`()
```

#### WorkflowIntegrationTest.kt

```kotlin
@Test
fun `Complete workflow from DRAFT to FINALIZED`()

@Test
fun `Cannot create scenario in DRAFT status`()

@Test
fun `Cannot create meeting before scenario selected`()
```

---

## 📚 Reference Documentation

- **CONTRACT_ANALYSIS.md**: Detailed specifications for each modification
- **AUDIT.md**: Original gap analysis and workflow diagrams
- **EXECUTIVE_SUMMARY.md**: High-level overview of the workflow issues

---

## 🚀 Ready for Phase 3

Phase 2 is now **COMPLETE** ✅. All Contract modifications are in place and validated.

**Next**: Implement the handlers in StateMachines to make the new Intents functional.

**Expected Timeline**:
- Phase 3 Implementation: 2-4 hours
- Testing: 1-2 hours
- Documentation: 30 minutes

**Success Criteria**:
- ✅ All new Intent handlers implemented
- ✅ All validation guards in place
- ✅ All tests passing (target: 100%)
- ✅ Full workflow DRAFT → FINALIZED functional
- ✅ Compilation successful: `./gradlew shared:build`

---

**Last Updated**: 2025-12-31  
**Next Session**: Implement Phase 3 - StateMachine handlers
