# Troubleshooting Guide: Workflow Coordination Issues

> **Change ID**: `verify-statemachine-workflow`  
> **Phase**: 7 - Documentation  
> **Date**: 2025-12-31

---

## Overview

This guide helps diagnose and resolve common issues with workflow coordination between state machines in Wakeve.

---

## Quick Diagnosis

### Symptom Checklist

- [ ] **Status Transition Rejected**: Intent fails with "Invalid status" error
- [ ] **Navigation Not Working**: NavigateTo side effect not routing correctly
- [ ] **Business Rule Violation**: Action allowed when it shouldn't be (or vice versa)
- [ ] **Scenario Creation Blocked**: Cannot create scenarios after date confirmation
- [ ] **Meeting Creation Blocked**: Cannot create meetings after transitioning to organizing
- [ ] **Repository Sync Issues**: Status not propagating between state machines

---

## Common Issues & Solutions

### 1. "Cannot start poll: Event not in DRAFT status"

**Symptom**: StartPoll intent rejected with error

**Cause**: Event.status is not DRAFT

**Diagnosis**:
```kotlin
// Check current event status
val event = eventRepository.getEvent(eventId)
println("Current status: ${event.status}") // Should be DRAFT

// Check if poll already started
if (event.status == EventStatus.POLLING) {
    // Poll already started
}
```

**Solution**:
- Ensure event is in DRAFT status before calling StartPoll
- If poll already started, skip this step
- If event is CONFIRMED/ORGANIZING/FINALIZED, cannot restart poll

**Guard**:
```kotlin
// EventManagementStateMachine.kt
private fun handleStartPoll(eventId: String) {
    val event = eventRepository.getEvent(eventId) ?: return
    
    // Guard: Must be DRAFT
    if (event.status != EventStatus.DRAFT) {
        emitSideEffect(ShowError("Cannot start poll: Event not in DRAFT status"))
        return
    }
    
    // Proceed with transition
    eventRepository.updateEvent(eventId, status = EventStatus.POLLING)
}
```

---

### 2. "Cannot confirm date: No votes recorded"

**Symptom**: ConfirmDate intent rejected

**Cause**: No participants have voted on time slots

**Diagnosis**:
```kotlin
// Check if votes exist
val poll = pollRepository.getPoll(eventId)
if (poll.votes.isEmpty()) {
    // No votes recorded
}

// Check vote count
println("Vote count: ${poll.votes.size}")
```

**Solution**:
- Ensure at least one participant has voted before confirming date
- Wait for voting period to complete
- Consider allowing organizer to confirm without votes (product decision)

**Business Rule**:
```kotlin
// EventManagementStateMachine.kt
private fun handleConfirmDate(eventId: String, slotId: String) {
    val poll = pollRepository.getPoll(eventId)
    
    // Guard: Must have votes
    if (poll.votes.isEmpty()) {
        emitSideEffect(ShowError("Cannot confirm date: No votes recorded"))
        return
    }
    
    // Proceed with confirmation
    // ...
}
```

---

### 3. "Cannot create scenario: Event not ready"

**Symptom**: Scenario creation blocked even after date confirmation

**Cause**: Event.status not CONFIRMED or Event.scenariosUnlocked is false

**Diagnosis**:
```kotlin
// Check event status and flags
val event = eventRepository.getEvent(eventId)
println("Status: ${event.status}") // Should be CONFIRMED or COMPARING
println("Scenarios unlocked: ${event.scenariosUnlocked}") // Should be true

// Check helper method
val state = ScenarioManagementContract.State(eventStatus = event.status)
println("Can create scenarios: ${state.canCreateScenarios()}") // Should be true
```

**Solution**:
- Ensure ConfirmDate has been called successfully
- Verify Event.status is CONFIRMED or COMPARING
- Check Event.scenariosUnlocked flag is true
- If status is ORGANIZING or FINALIZED, scenario phase has passed

**Business Rule**:
```kotlin
// ScenarioManagementContract.kt
fun State.canCreateScenarios(): Boolean {
    return eventStatus in listOf(
        EventStatus.COMPARING, 
        EventStatus.CONFIRMED
    )
}
```

**Fix**:
```kotlin
// If scenariosUnlocked is false but status is CONFIRMED
eventRepository.updateEvent(
    eventId = eventId,
    scenariosUnlocked = true
)
```

---

### 4. "Cannot transition to organizing: Date not confirmed"

**Symptom**: TransitionToOrganizing intent rejected

**Cause**: Event.status is not CONFIRMED

**Diagnosis**:
```kotlin
// Check if date has been confirmed
val event = eventRepository.getEvent(eventId)
println("Status: ${event.status}") // Should be CONFIRMED
println("Final date: ${event.finalDate}") // Should not be null

// Check if already organizing
if (event.status == EventStatus.ORGANIZING) {
    // Already transitioned
}
```

**Solution**:
- Ensure ConfirmDate has been called first
- Verify Event.status == CONFIRMED
- Check Event.finalDate is set
- If already ORGANIZING, skip this step

**Guard**:
```kotlin
// EventManagementStateMachine.kt
private fun handleTransitionToOrganizing(eventId: String) {
    val event = eventRepository.getEvent(eventId) ?: return
    
    // Guard: Must be CONFIRMED
    if (event.status != EventStatus.CONFIRMED) {
        emitSideEffect(ShowError("Cannot transition: Date not confirmed"))
        return
    }
    
    // Proceed with transition
    eventRepository.updateEvent(
        eventId = eventId,
        status = EventStatus.ORGANIZING,
        meetingsUnlocked = true
    )
}
```

---

### 5. "Cannot create meeting: Event not organizing"

**Symptom**: Meeting creation blocked

**Cause**: Event.status is not ORGANIZING or Event.meetingsUnlocked is false

**Diagnosis**:
```kotlin
// Check event status and flags
val event = eventRepository.getEvent(eventId)
println("Status: ${event.status}") // Should be ORGANIZING
println("Meetings unlocked: ${event.meetingsUnlocked}") // Should be true

// Check helper method
val state = MeetingManagementContract.State(eventStatus = event.status)
println("Can create meetings: ${state.canCreateMeetings()}") // Should be true
```

**Solution**:
- Ensure TransitionToOrganizing has been called
- Verify Event.status == ORGANIZING
- Check Event.meetingsUnlocked flag is true
- If status is FINALIZED, event is locked

**Business Rule**:
```kotlin
// MeetingManagementContract.kt
fun State.canCreateMeetings(): Boolean {
    return eventStatus == EventStatus.ORGANIZING
}
```

**Fix**:
```kotlin
// If meetingsUnlocked is false but status is ORGANIZING
eventRepository.updateEvent(
    eventId = eventId,
    meetingsUnlocked = true
)
```

---

### 6. "Navigation not working after status change"

**Symptom**: NavigateTo side effect emitted but UI doesn't navigate

**Cause**: Side effect not observed in ViewModel/UI layer

**Diagnosis**:
```kotlin
// Check if side effect is emitted
stateMachine.sideEffects.collect { sideEffect ->
    when (sideEffect) {
        is NavigateTo -> println("Navigate to: ${sideEffect.route}")
        else -> {}
    }
}

// Check navigation route format
// Should be: "scenarios/{eventId}" or "meetings/{eventId}"
```

**Solution**:
- Ensure ViewModel/UI is collecting `stateMachine.sideEffects`
- Verify navigation route is correct format
- Check navigation implementation handles route correctly

**Example Fix (Compose)**:
```kotlin
// In ViewModel or Composable
LaunchedEffect(Unit) {
    stateMachine.sideEffects.collect { sideEffect ->
        when (sideEffect) {
            is NavigateTo -> navController.navigate(sideEffect.route)
            is ShowError -> snackbarHostState.showSnackbar(sideEffect.message)
        }
    }
}
```

**Example Fix (SwiftUI)**:
```swift
// In SwiftUI View
.onAppear {
    Task {
        for await sideEffect in stateMachine.sideEffects {
            if let nav = sideEffect as? NavigateTo {
                navigationPath.append(nav.route)
            }
        }
    }
}
```

---

### 7. "Status not propagating between state machines"

**Symptom**: EventManagement updates status but ScenarioManagement sees old status

**Cause**: Repository not shared or stale cache

**Diagnosis**:
```kotlin
// Check if repository is shared
val eventMgmtRepo = eventManagementStateMachine.repository
val scenarioMgmtRepo = scenarioManagementStateMachine.repository

// Should be same instance
println("Same repo: ${eventMgmtRepo === scenarioMgmtRepo}")

// Check for caching issues
val event1 = eventMgmtRepo.getEvent(eventId)
val event2 = scenarioMgmtRepo.getEvent(eventId)
println("Status 1: ${event1.status}")
println("Status 2: ${event2.status}") // Should match
```

**Solution**:
- Ensure all state machines share the same EventRepository instance
- Clear any local caches after status updates
- Use Flow<Event> for reactive updates instead of one-time reads

**Fix (Dependency Injection)**:
```kotlin
// Koin or manual DI
val eventRepository = EventRepository() // Single instance

val eventStateMachine = EventManagementStateMachine(
    eventRepository = eventRepository // Shared
)

val scenarioStateMachine = ScenarioManagementStateMachine(
    eventRepository = eventRepository // Same instance
)
```

**Reactive Approach**:
```kotlin
// Use Flow for automatic updates
eventRepository.observeEvent(eventId).collect { event ->
    // Automatically updates when status changes
    updateState { copy(eventStatus = event.status) }
}
```

---

### 8. "Workflow stuck between phases"

**Symptom**: Event in CONFIRMED but cannot proceed to scenarios or meetings

**Cause**: Missing navigation call or state flags not set

**Diagnosis**:
```kotlin
// Check event state
val event = eventRepository.getEvent(eventId)
println("Status: ${event.status}")
println("Scenarios unlocked: ${event.scenariosUnlocked}")
println("Meetings unlocked: ${event.meetingsUnlocked}")
println("Final date: ${event.finalDate}")

// Check what actions are allowed
val scenarioState = ScenarioManagementContract.State(eventStatus = event.status)
println("Can create scenarios: ${scenarioState.canCreateScenarios()}")

val meetingState = MeetingManagementContract.State(eventStatus = event.status)
println("Can create meetings: ${meetingState.canCreateMeetings()}")
```

**Solution**:
- If CONFIRMED but scenariosUnlocked=false, call ConfirmDate again or fix flag
- If CONFIRMED with scenariosUnlocked=true, navigate to scenarios manually
- If ready for meetings, call TransitionToOrganizing

**Recovery**:
```kotlin
// Manually fix stuck event (admin/debug only)
when (event.status) {
    EventStatus.CONFIRMED -> {
        if (!event.scenariosUnlocked) {
            // Unlock scenarios
            eventRepository.updateEvent(eventId, scenariosUnlocked = true)
        }
        // Navigate to scenarios
        navController.navigate("scenarios/$eventId")
    }
    EventStatus.ORGANIZING -> {
        if (!event.meetingsUnlocked) {
            // Unlock meetings
            eventRepository.updateEvent(eventId, meetingsUnlocked = true)
        }
        // Navigate to meetings
        navController.navigate("meetings/$eventId")
    }
}
```

---

## Validation Tools

### 1. Event Status Checker

```kotlin
fun validateEventWorkflow(eventId: String): WorkflowValidation {
    val event = eventRepository.getEvent(eventId) ?: return WorkflowValidation.NotFound
    
    return when (event.status) {
        EventStatus.DRAFT -> {
            WorkflowValidation.Valid("Event ready for polling")
        }
        EventStatus.POLLING -> {
            val poll = pollRepository.getPoll(eventId)
            if (poll.votes.isEmpty()) {
                WorkflowValidation.Warning("No votes yet")
            } else {
                WorkflowValidation.Valid("Poll active with ${poll.votes.size} votes")
            }
        }
        EventStatus.CONFIRMED -> {
            if (!event.scenariosUnlocked) {
                WorkflowValidation.Error("Scenarios not unlocked")
            } else if (event.finalDate == null) {
                WorkflowValidation.Error("Final date not set")
            } else {
                WorkflowValidation.Valid("Date confirmed, scenarios unlocked")
            }
        }
        EventStatus.ORGANIZING -> {
            if (!event.meetingsUnlocked) {
                WorkflowValidation.Error("Meetings not unlocked")
            } else {
                WorkflowValidation.Valid("Organizing phase active")
            }
        }
        EventStatus.FINALIZED -> {
            WorkflowValidation.Valid("Event finalized")
        }
        else -> WorkflowValidation.Unknown("Unknown status: ${event.status}")
    }
}

sealed class WorkflowValidation {
    data class Valid(val message: String) : WorkflowValidation()
    data class Warning(val message: String) : WorkflowValidation()
    data class Error(val message: String) : WorkflowValidation()
    object NotFound : WorkflowValidation()
    data class Unknown(val message: String) : WorkflowValidation()
}
```

### 2. Workflow Transition Validator

```kotlin
fun canTransition(eventId: String, targetIntent: String): TransitionCheck {
    val event = eventRepository.getEvent(eventId) ?: return TransitionCheck.NotFound
    
    return when (targetIntent) {
        "StartPoll" -> {
            if (event.status == EventStatus.DRAFT) {
                TransitionCheck.Allowed
            } else {
                TransitionCheck.Blocked("Event must be DRAFT, currently ${event.status}")
            }
        }
        "ConfirmDate" -> {
            when {
                event.status != EventStatus.POLLING -> {
                    TransitionCheck.Blocked("Event must be POLLING, currently ${event.status}")
                }
                pollRepository.getPoll(eventId).votes.isEmpty() -> {
                    TransitionCheck.Warning("No votes recorded")
                }
                else -> TransitionCheck.Allowed
            }
        }
        "TransitionToOrganizing" -> {
            if (event.status == EventStatus.CONFIRMED) {
                TransitionCheck.Allowed
            } else {
                TransitionCheck.Blocked("Event must be CONFIRMED, currently ${event.status}")
            }
        }
        "MarkAsFinalized" -> {
            if (event.status == EventStatus.ORGANIZING) {
                TransitionCheck.Allowed
            } else {
                TransitionCheck.Blocked("Event must be ORGANIZING, currently ${event.status}")
            }
        }
        else -> TransitionCheck.Unknown("Unknown intent: $targetIntent")
    }
}

sealed class TransitionCheck {
    object Allowed : TransitionCheck()
    data class Blocked(val reason: String) : TransitionCheck()
    data class Warning(val message: String) : TransitionCheck()
    object NotFound : TransitionCheck()
    data class Unknown(val message: String) : TransitionCheck()
}
```

### 3. Debug Logger

```kotlin
fun logWorkflowState(eventId: String) {
    val event = eventRepository.getEvent(eventId)
    if (event == null) {
        println("[Workflow] Event $eventId not found")
        return
    }
    
    println("""
        [Workflow Debug] Event: $eventId
        ├─ Status: ${event.status}
        ├─ Final Date: ${event.finalDate ?: "Not set"}
        ├─ Scenarios Unlocked: ${event.scenariosUnlocked}
        ├─ Meetings Unlocked: ${event.meetingsUnlocked}
        ├─ Can Create Scenarios: ${ScenarioManagementContract.State(eventStatus = event.status).canCreateScenarios()}
        ├─ Can Create Meetings: ${MeetingManagementContract.State(eventStatus = event.status).canCreateMeetings()}
        └─ Next Actions: ${suggestNextActions(event)}
    """.trimIndent())
}

fun suggestNextActions(event: Event): String {
    return when (event.status) {
        EventStatus.DRAFT -> "Call StartPoll"
        EventStatus.POLLING -> "Collect votes, then call ConfirmDate"
        EventStatus.CONFIRMED -> "Create scenarios or call TransitionToOrganizing"
        EventStatus.ORGANIZING -> "Create meetings, then call MarkAsFinalized"
        EventStatus.FINALIZED -> "Workflow complete"
        else -> "Unknown status"
    }
}
```

---

## Testing Workflow Issues

### Unit Test Template

```kotlin
@Test
fun testWorkflowIssue_YourScenario() {
    // Given: Setup initial state
    val event = createTestEvent(status = EventStatus.DRAFT)
    eventRepository.saveEvent(event)
    
    // When: Perform action that causes issue
    stateMachine.dispatch(Intent.YourIntent(event.id))
    
    // Then: Verify expected outcome or error
    val updatedEvent = eventRepository.getEvent(event.id)
    assertEquals(EventStatus.ExpectedStatus, updatedEvent.status)
    
    // Or verify error
    val sideEffect = stateMachine.sideEffects.first()
    assertTrue(sideEffect is ShowError)
}
```

### Integration Test Template

```kotlin
@Test
fun testCrossStateMachineIssue() {
    // Given: Setup shared repository
    val eventRepo = MockEventRepository()
    val event = createTestEvent(status = EventStatus.CONFIRMED)
    eventRepo.saveEvent(event)
    
    val eventSM = EventManagementStateMachine(eventRepository = eventRepo)
    val scenarioSM = ScenarioManagementStateMachine(eventRepository = eventRepo)
    
    // When: EventSM updates status
    eventSM.dispatch(Intent.TransitionToOrganizing(event.id))
    
    // Then: ScenarioSM sees updated status
    val updatedEvent = eventRepo.getEvent(event.id)
    assertEquals(EventStatus.ORGANIZING, updatedEvent.status)
    
    val scenarioState = ScenarioManagementContract.State(eventStatus = updatedEvent.status)
    assertFalse(scenarioState.canCreateScenarios()) // Should be blocked now
}
```

---

## References

- **WORKFLOW_DIAGRAMS.md**: Visual workflow documentation
- **PHASE4_TESTING_COMPLETE.md**: Unit test examples
- **PHASE5_INTEGRATION_COMPLETE.md**: Integration test patterns
- **EventManagementStateMachine.kt**: Intent handler implementations
- **Test Files**:
  - `EventManagementStateMachineTest.kt`
  - `WorkflowIntegrationTest.kt`

---

## Getting Help

### Quick Debugging Checklist

1. ✅ Check Event.status matches expected phase
2. ✅ Verify state flags (scenariosUnlocked, meetingsUnlocked)
3. ✅ Confirm repository is shared across state machines
4. ✅ Check navigation side effects are being observed
5. ✅ Review guard conditions for the Intent
6. ✅ Look for errors in StateMachine logs
7. ✅ Validate test coverage for the scenario

### Common Fixes

- **Status mismatch**: Check previous transition completed successfully
- **Navigation not working**: Ensure side effects are collected in UI
- **Business rule violation**: Verify Event.status and helper methods
- **Repository sync**: Confirm shared instance, consider Flow-based updates
- **Stuck workflow**: Check state flags, manually recover if needed

---

**Troubleshooting Guide Complete**: ✅  
**Coverage**: All common workflow issues  
**Test Tools**: Included validation utilities  
**Ready for**: Production support
