# Specification: Workflow Coordination

> **Capability**: `workflow-coordination`  
> **Version**: 1.0.0  
> **Status**: Draft  
> **Last Updated**: 2025-12-31  

---

## Purpose

The Workflow Coordination capability ensures seamless transitions between the three core state machines (Event Management, Scenario Management, and Meeting Service) throughout the event lifecycle. It defines the rules, invariants, and communication patterns that guarantee a consistent user experience from event creation to finalization.

### Core Concepts

**Event Lifecycle Phases**:
- `DRAFT`: Event created, not yet open for voting
- `POLLING`: Participants voting on proposed time slots
- `SCENARIO_COMPARISON`: Optional scenarios being compared and voted on
- `CONFIRMED`: Final date and scenario locked by organizer
- `ORGANIZATION`: Detailed planning (meetings, logistics, budget)
- `FINALIZED`: All critical information confirmed, event ready

**State Machine**:
A finite state machine implementing the MVI pattern, managing immutable state, intents (actions), and side effects (one-shot events like navigation).

**Workflow Coordinator**:
A component (or pattern) responsible for orchestrating transitions between state machines, enforcing business rules, and maintaining consistency.

**Invariants**:
Business rules that must always be true (e.g., "Scenarios cannot be created in DRAFT phase").

---

## ADDED Requirements

### Requirement: State machines SHALL observe Event status and adapt their behavior accordingly

The system MUST ensure all three state machines (Event Management, Scenario Management, Meeting Service) observe the current Event status and enable/disable features based on the lifecycle phase.

#### Scenario: Scenario Management is disabled in DRAFT phase
- **GIVEN** an event with status DRAFT
- **WHEN** a user attempts to navigate to Scenario Management
- **THEN** the UI displays a message "Scenarios are available after confirming a date" and blocks navigation

#### Scenario: Meeting Service is enabled in ORGANIZATION phase
- **GIVEN** an event with status CONFIRMED
- **WHEN** the organizer transitions to ORGANIZATION phase
- **THEN** the Meeting Service is enabled and the organizer can create virtual meetings

### Requirement: The system SHALL enforce transition rules between lifecycle phases

The system MUST prevent invalid transitions (e.g., DRAFT → ORGANIZATION without CONFIRMED) and enforce the correct order of phases.

#### Scenario: Cannot skip POLLING phase
- **GIVEN** an event with status DRAFT
- **WHEN** the organizer attempts to transition directly to CONFIRMED
- **THEN** the system prevents the action and shows "You must complete polling before confirming a date"

#### Scenario: POLLING to CONFIRMED transition requires votes
- **GIVEN** an event with status POLLING and no votes
- **WHEN** the organizer attempts to confirm the date
- **THEN** the system prevents the action and shows "At least one participant must vote before confirming"

### Requirement: The system SHALL synchronize shared state between state machines via repository

Changes to Event status or data in one state machine MUST be reflected in all others through a shared repository that emits updates.

#### Scenario: Event status update propagates to all state machines
- **GIVEN** Event Management confirms a date (status → CONFIRMED)
- **WHEN** the repository emits the updated event
- **THEN** Scenario Management receives the update and enables scenario creation
- **AND** Meeting Service receives the update and enables meeting creation

### Requirement: Navigation side effects SHALL be consistent with the workflow

Each state machine MUST emit correct navigation side effects that align with the event lifecycle and user role.

#### Scenario: Confirming date navigates to Scenario Management
- **GIVEN** an event with status POLLING
- **WHEN** the organizer confirms the final date
- **THEN** Event Management emits SideEffect.NavigateTo("scenarios/{eventId}")
- **AND** the UI navigates to Scenario Management screen

#### Scenario: Participant cannot navigate to organizer-only screens
- **GIVEN** a participant (not organizer) viewing an event
- **WHEN** they attempt to confirm a date
- **THEN** the state machine emits SideEffect.ShowError("Only organizers can confirm dates")
- **AND** no navigation occurs

### Requirement: The system SHALL handle offline actions queue in correct order

Actions performed offline MUST be queued and executed in the correct order when connectivity is restored, respecting workflow dependencies.

#### Scenario: Offline create event → add participant → start poll
- **GIVEN** the user is offline
- **WHEN** they create an event, add participants, and start polling
- **THEN** actions are queued in order: CreateEvent → AddParticipants → StartPoll
- **WHEN** connectivity is restored
- **THEN** actions execute sequentially, and each waits for the previous to succeed

### Requirement: Error propagation SHALL be isolated per state machine

Errors in one state machine MUST NOT block or crash other state machines, but MAY emit informational side effects.

#### Scenario: Scenario creation fails but Event Management continues
- **GIVEN** Event Management and Scenario Management are both active
- **WHEN** Scenario Management fails to create a scenario (network error)
- **THEN** Scenario Management shows an error message
- **AND** Event Management continues to function normally

### Requirement: The system SHALL validate invariants before state transitions

Business invariants (e.g., "cannot create scenarios in DRAFT") MUST be checked before allowing state transitions.

#### Scenario: Attempt to create scenario in DRAFT phase fails
- **GIVEN** an event with status DRAFT
- **WHEN** Scenario Management receives Intent.CreateScenario
- **THEN** the state machine checks the event status invariant
- **AND** emits SideEffect.ShowError("Scenarios are only available after confirming a date")
- **AND** does NOT create the scenario

#### Scenario: Attempt to vote after CONFIRMED phase fails
- **GIVEN** an event with status CONFIRMED
- **WHEN** Event Management receives Intent.Vote(slotId, voteType)
- **THEN** the state machine checks the status invariant
- **AND** emits SideEffect.ShowError("Voting is closed after the date is confirmed")
- **AND** does NOT record the vote

### Requirement: The system SHALL provide workflow visibility to users

Users MUST be able to see the current phase of the event and what actions are available/locked at each phase.

#### Scenario: Event detail screen shows current phase and next actions
- **GIVEN** an event with status POLLING
- **WHEN** the user views the event detail screen
- **THEN** the UI displays:
  - Current phase: "Polling in Progress"
  - Available actions: "Vote on time slots", "View poll results"
  - Locked actions: "Create scenarios (available after confirming date)"

---

## MODIFIED Requirements

### Requirement: Event status transitions MUST be atomic and persisted

> **Modified from**: `event-organization/spec.md` - Requirement "Organizers SHALL be able to validate and confirm the final event date"  
> **Why**: Multi-state-machine coordination requires atomic updates and explicit notification to prevent inconsistencies  
> **Changes**: Added atomicity requirement, repository notification, and state machine coordination

When an organizer confirms the final date, the system MUST atomically update the event status to CONFIRMED, persist to repository, and notify all state machines before emitting navigation side effects.

#### Scenario: Organizer confirms final date (updated)
- **GIVEN** an event with status POLLING and at least one vote
- **WHEN** the organizer confirms the final date
- **THEN** the system atomically updates Event.status = CONFIRMED, Event.finalDate = selectedDate
- **AND** persists to local repository (SQLDelight)
- **AND** emits repository update event
- **AND** all state machines receive the update and adapt their behavior
- **AND** Event Management emits SideEffect.NavigateTo("scenarios/{eventId}")

---

## REMOVED Requirements

_None. This spec introduces new requirements without removing existing ones._

---

## Workflow State Diagram

```
┌─────────┐
│  DRAFT  │  ← Create event, add participants
└────┬────┘
     │ StartPoll
     ▼
┌─────────┐
│ POLLING │  ← Vote on time slots
└────┬────┘
     │ ConfirmDate (with votes)
     ▼
┌──────────┐
│CONFIRMED │  ← Date locked
└────┬─────┘
     │ (Optional: Create/Compare Scenarios)
     ▼
┌────────────────────┐
│SCENARIO_COMPARISON │  ← Vote on scenarios
└────────┬───────────┘
         │ SelectScenario
         ▼
┌──────────┐
│CONFIRMED │  ← Scenario locked (status may revert to CONFIRMED)
└────┬─────┘
     │ TransitionToOrganization
     ▼
┌──────────────┐
│ORGANIZATION  │  ← Create meetings, plan logistics
└────┬─────────┘
     │ MarkAsFinalized
     ▼
┌──────────┐
│FINALIZED │  ← All details confirmed
└──────────┘
```

---

## State Machine Communication Patterns

### Option A: Shared Repository (Recommended)

```kotlin
// EventManagementStateMachine confirms date
suspend fun confirmDate(slotId: String) {
    val updatedEvent = currentState.selectedEvent.copy(
        status = EventStatus.CONFIRMED,
        finalDate = slotId
    )
    
    // Persist to repository
    eventRepository.updateEvent(updatedEvent)
    
    // Repository emits update via Flow
    // All state machines observing eventRepository.getEvent(eventId) receive update
    
    // Navigate to scenarios
    emitSideEffect(SideEffect.NavigateTo("scenarios/${updatedEvent.id}"))
}

// ScenarioManagementStateMachine observes event
init {
    eventRepository.getEvent(eventId).collect { event ->
        if (event.status == EventStatus.CONFIRMED && !state.value.scenariosUnlocked) {
            updateState { it.copy(scenariosUnlocked = true) }
        }
    }
}
```

### Option B: Workflow Coordinator (Alternative)

```kotlin
class WorkflowCoordinator(
    private val eventStateMachine: EventManagementStateMachine,
    private val scenarioStateMachine: ScenarioManagementStateMachine,
    private val meetingStateMachine: MeetingServiceStateMachine,
    private val eventRepository: EventRepository
) {
    fun initialize(eventId: String) {
        // Observe event changes
        eventRepository.getEvent(eventId).collect { event ->
            when (event.status) {
                EventStatus.CONFIRMED -> {
                    scenarioStateMachine.dispatch(Intent.UnlockScenarios)
                    meetingStateMachine.dispatch(Intent.UnlockMeetings)
                }
                EventStatus.ORGANIZATION -> {
                    // ...
                }
            }
        }
    }
}
```

**Decision**: We recommend **Option A (Shared Repository)** for simplicity and decoupling. Each state machine observes the repository independently.

---

## Invariants

The system MUST enforce these invariants at all times:

1. **Phase Order**: `DRAFT → POLLING → CONFIRMED → (optional SCENARIO_COMPARISON) → ORGANIZATION → FINALIZED`
2. **No Backward Transitions**: Cannot go from CONFIRMED back to POLLING
3. **Scenarios After Confirmation**: Scenarios can only be created/voted on after CONFIRMED
4. **Meetings After Confirmation**: Meetings can only be created after CONFIRMED
5. **Vote Deadline**: Votes cannot be submitted after the deadline or after CONFIRMED
6. **Organizer-Only Actions**: Only organizers can confirm dates, select scenarios, create meetings
7. **At Least One Vote**: Cannot confirm a date without at least one participant vote

---

## Testing Strategy

### Unit Tests

- Test each state machine independently
- Mock repository to verify state changes
- Test invariant checks (e.g., reject scenario creation in DRAFT)

### Integration Tests

- Test repository updates propagating to multiple state machines
- Test offline queue order (create → vote → confirm)
- Test error isolation (one state machine fails, others continue)

### End-to-End Tests

- Complete workflow: DRAFT → POLLING → CONFIRMED → ORGANIZATION → FINALIZED
- Test navigation side effects
- Test role-based access (organizer vs participant)
- Test offline-then-online workflow

---

## Open Questions

1. **Coordinator Pattern**: Should we introduce a WorkflowCoordinator or rely on repository observation? (Recommendation: Repository observation for simplicity)
2. **Navigation Owner**: Should state machines emit navigation side effects or should a central NavController observe state changes? (Current: State machines emit, UI handles)
3. **Offline Queue**: How do we guarantee order for complex workflows (e.g., create event → add participant → start poll → vote)? (Recommendation: Use a priority queue with dependency tracking)

---

## Related Specifications

- `event-organization/spec.md` - Event lifecycle and status transitions
- `scenario-management/spec.md` - Scenario comparison and voting
- `meeting-service/spec.md` - Virtual meeting link generation
- `collaboration-management/spec.md` - Participant roles and permissions
