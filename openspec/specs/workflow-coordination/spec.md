# Specification: Workflow Coordination

> **Capability**: `workflow-coordination`
> **Version**: 1.0.0
> **Status**: Active
> **Last Updated**: 2025-12-31

## Purpose

The Workflow Coordination capability enables seamless transitions between event lifecycle phases (DRAFT → POLLING → CONFIRMED → COMPARING → ORGANIZING → FINALIZED) through coordinated state machine communication. This ensures that users are automatically guided through the event planning workflow with proper validation, navigation, and feature unlocking at each phase.

### Core Concepts

**State Machine**: An MVI (Model-View-Intent) component that manages a specific domain's state and behavior (EventManagement, ScenarioManagement, MeetingService).

**Repository-Mediated Communication**: State machines communicate indirectly through a shared EventRepository by reading and writing Event.status, rather than directly calling each other.

**Intent**: A user action or system event that triggers a state transition (e.g., ConfirmDate, StartPoll).

**Side Effect**: An observable one-time event emitted by a state machine (e.g., NavigateTo, ShowToast).

**Guard**: A validation check performed before executing a state transition to ensure business rules are enforced.

**Event Status Flow**:
- `DRAFT`: Event created, editable, not yet open for voting
- `POLLING`: Poll active, participants can vote on time slots
- `CONFIRMED`: Date confirmed, scenarios unlocked
- `COMPARING`: Scenarios being compared (implicit state during scenario voting)
- `ORGANIZING`: Meetings unlocked, event organization phase
- `FINALIZED`: Event locked, all details confirmed, read-only

## Requirements

### The system SHALL enable automatic workflow transitions from DRAFT to FINALIZED status

#### Scenario: Complete event workflow execution
- **WHEN** Organizer creates event, starts poll, confirms date, creates scenarios, transitions to organizing, and finalizes event
- **THEN** Event progresses through all statuses: DRAFT → POLLING → CONFIRMED → COMPARING → ORGANIZING → FINALIZED
- **AND** User is automatically navigated to the next appropriate screen at each transition

### State machines SHALL communicate through repository-mediated pattern with no direct dependencies

#### Scenario: EventManagement updates status, ScenarioManagement reacts
- **WHEN** EventManagementStateMachine transitions event to CONFIRMED status
- **THEN** Event.status is persisted to EventRepository with scenariosUnlocked=true
- **AND** ScenarioManagementStateMachine reads Event.status from repository
- **AND** ScenarioManagementStateMachine enables scenario creation based on status
- **AND** No direct method calls occur between state machines

### The system SHALL enforce status-based guards to prevent invalid state transitions

#### Scenario: Attempt to confirm date in wrong status
- **WHEN** User attempts ConfirmDate Intent on event with status=DRAFT
- **THEN** State machine validates current status != POLLING
- **AND** Transition is blocked with error message "Cannot confirm date: Event is not in POLLING status"
- **AND** Event status remains unchanged

#### Scenario: Attempt to create scenario before date confirmation
- **WHEN** User attempts CreateScenario on event with status=DRAFT
- **THEN** ScenarioManagementStateMachine calls canCreateScenarios() helper
- **AND** Helper returns false (status not in [CONFIRMED, COMPARING])
- **AND** UI prevents scenario creation with error "Date must be confirmed first"

### The system SHALL automatically unlock features based on event status transitions

#### Scenario: Scenarios unlocked after date confirmation
- **WHEN** Organizer confirms date via ConfirmDate Intent
- **THEN** Event.scenariosUnlocked field is set to true
- **AND** EventManagementStateMachine.State.scenariosUnlocked is updated to true
- **AND** NavigateTo("scenarios/{eventId}") side effect is emitted
- **AND** Scenario creation UI becomes enabled

#### Scenario: Meetings unlocked after transition to organizing
- **WHEN** Organizer triggers TransitionToOrganizing Intent
- **THEN** Event.meetingsUnlocked field is set to true
- **AND** EventManagementStateMachine.State.meetingsUnlocked is updated to true
- **AND** NavigateTo("meetings/{eventId}") side effect is emitted
- **AND** Meeting creation UI becomes enabled

### The system SHALL emit navigation side effects to guide user workflow progression

#### Scenario: Navigation after date confirmation
- **WHEN** ConfirmDate Intent completes successfully
- **THEN** State machine emits SideEffect.NavigateTo("scenarios/{eventId}")
- **AND** User is automatically navigated to scenario creation screen
- **AND** Toast message "Date confirmed successfully!" is shown

#### Scenario: Navigation after scenario selection
- **WHEN** SelectScenarioAsFinal Intent completes successfully
- **THEN** State machine emits SideEffect.NavigateTo("meetings/{eventId}")
- **AND** User is automatically navigated to meeting management screen
- **AND** Toast message "Scenario selected successfully!" is shown

#### Scenario: Navigation after organizing transition
- **WHEN** TransitionToOrganizing Intent completes successfully
- **THEN** State machine emits SideEffect.NavigateTo("meetings/{eventId}")
- **AND** User is automatically navigated to meeting creation screen

### The system SHALL validate business rules before executing workflow transitions

#### Scenario: Require at least one vote before date confirmation
- **WHEN** Organizer attempts ConfirmDate with no votes submitted
- **THEN** State machine validates poll.votes.isNotEmpty()
- **AND** Transition is blocked with error "Cannot confirm date: No votes have been submitted"
- **AND** Event status remains POLLING

#### Scenario: Require event existence for all transitions
- **WHEN** User triggers Intent with invalid eventId
- **THEN** State machine queries repository for event
- **AND** Returns error "Event not found" if event is null
- **AND** No state changes occur

#### Scenario: Enforce repository availability
- **WHEN** Intent handler is called with null repository dependency
- **THEN** State machine validates repository != null
- **AND** Returns error "Repository not available" immediately
- **AND** No database queries are attempted

### EventManagementStateMachine SHALL provide four workflow transition Intents

#### Scenario: StartPoll transitions DRAFT to POLLING
- **WHEN** Organizer dispatches StartPoll(eventId) Intent
- **THEN** State machine validates event.status == DRAFT
- **AND** Updates event status to POLLING via repository
- **AND** Reloads events list
- **AND** Emits ShowToast("Poll started successfully!")

#### Scenario: ConfirmDate transitions POLLING to CONFIRMED
- **WHEN** Organizer dispatches ConfirmDate(eventId, slotId) Intent
- **THEN** State machine validates event.status == POLLING
- **AND** Validates at least one vote exists
- **AND** Finds selected time slot by slotId
- **AND** Updates event with status=CONFIRMED, finalDate=slot.startTime, scenariosUnlocked=true
- **AND** Emits NavigateTo("scenarios/{eventId}")
- **AND** Updates State.scenariosUnlocked = true

#### Scenario: TransitionToOrganizing transitions CONFIRMED to ORGANIZING
- **WHEN** Organizer dispatches TransitionToOrganizing(eventId) Intent
- **THEN** State machine validates event.status == CONFIRMED
- **AND** Updates event status to ORGANIZING via repository
- **AND** Sets meetingsUnlocked = true
- **AND** Emits NavigateTo("meetings/{eventId}")
- **AND** Updates State.meetingsUnlocked = true

#### Scenario: MarkAsFinalized transitions ORGANIZING to FINALIZED
- **WHEN** Organizer dispatches MarkAsFinalized(eventId) Intent
- **THEN** State machine validates event.status == ORGANIZING
- **AND** Updates event status to FINALIZED via repository
- **AND** Emits ShowToast("Event finalized successfully!")
- **AND** Event becomes read-only

### ScenarioManagementStateMachine SHALL provide status-aware scenario management

#### Scenario: canCreateScenarios helper validates event status
- **WHEN** User attempts to create scenario
- **THEN** State machine calls State.canCreateScenarios() helper
- **AND** Helper returns true if eventStatus in [CONFIRMED, COMPARING]
- **AND** Helper returns false otherwise
- **AND** UI displays appropriate message based on result

#### Scenario: SelectScenarioAsFinal transitions to meeting phase
- **WHEN** User dispatches SelectScenarioAsFinal(eventId, scenarioId) Intent
- **THEN** State machine validates event.status == COMPARING
- **AND** Loads scenario by scenarioId from repository
- **AND** Updates scenario with isFinal=true
- **AND** Optionally transitions event to CONFIRMED status (if needed)
- **AND** Reloads scenarios list
- **AND** Emits NavigateTo("meetings/{eventId}")
- **AND** Emits ShowToast("Scenario selected successfully!")

### MeetingServiceStateMachine SHALL validate event status before meeting creation

#### Scenario: canCreateMeetings helper validates event status
- **WHEN** User attempts to create meeting
- **THEN** State machine calls State.canCreateMeetings() helper
- **AND** Helper returns true if eventStatus in [CONFIRMED, ORGANIZING]
- **AND** Helper returns false otherwise
- **AND** UI prevents meeting creation if status is invalid

### The system SHALL maintain atomic state updates for all workflow transitions

#### Scenario: Repository update failure rolls back state changes
- **WHEN** Intent handler updates event status via repository
- **AND** Repository throws exception during persist
- **THEN** State machine catches exception
- **AND** Reverts any in-memory state changes
- **AND** Emits error SideEffect with message
- **AND** User is notified of failure

#### Scenario: Loading states are managed correctly
- **WHEN** Intent handler begins execution
- **THEN** State.isLoading is set to true immediately
- **AND** User sees loading indicator
- **WHEN** Operation completes (success or failure)
- **THEN** State.isLoading is set to false
- **AND** Loading indicator is hidden

### The system SHALL support offline-first workflow coordination

#### Scenario: Workflow transitions work offline
- **WHEN** User executes workflow transitions while offline
- **THEN** State machines update local repository (SQLite)
- **AND** Changes are persisted immediately to local database
- **AND** Sync service queues changes for upload when online
- **AND** User can continue workflow without interruption

#### Scenario: Status propagation occurs on app restart
- **WHEN** User closes and reopens app during workflow
- **THEN** State machines load current Event.status from repository
- **AND** State machines restore scenariosUnlocked/meetingsUnlocked flags
- **AND** User sees correct UI state based on persisted status
- **AND** Workflow can continue from last known state

## Architecture

### State Machine Communication

```
EventManagementStateMachine
        ↓ (writes Event.status)
    EventRepository (Shared)
        ↑ (reads Event.status)
ScenarioManagementStateMachine
        ↓ (reads Event.status)
    EventRepository (Shared)
        ↑ (reads Event.status)
MeetingServiceStateMachine
```

**Key Principle**: State machines NEVER call each other directly. All coordination happens through shared repository state.

### Guard Pattern

All Intent handlers implement validation guards in this order:

1. **Repository availability**: Check repository != null
2. **Entity existence**: Check event/scenario/meeting exists
3. **Status validation**: Check current status allows transition
4. **Business rules**: Check domain-specific constraints (e.g., votes exist)
5. **Execute transition**: Update repository
6. **Update state**: Apply state changes
7. **Emit side effects**: Navigate or show toast

### Navigation Side Effects

| Transition | Side Effect | Target Screen |
|------------|-------------|---------------|
| ConfirmDate | NavigateTo("scenarios/{eventId}") | Scenario creation |
| SelectScenarioAsFinal | NavigateTo("meetings/{eventId}") | Meeting management |
| TransitionToOrganizing | NavigateTo("meetings/{eventId}") | Meeting management |

### Status-Based Feature Unlocking

| EventStatus | Scenarios Allowed | Meetings Allowed | Actions |
|-------------|-------------------|------------------|---------|
| DRAFT | ❌ | ❌ | Edit event, add participants |
| POLLING | ❌ | ❌ | Vote on time slots |
| CONFIRMED | ✅ | ❌ | Create scenarios, vote on scenarios |
| COMPARING | ✅ | ❌ | Vote on scenarios, select final |
| ORGANIZING | ❌ | ✅ | Create meetings, generate links |
| FINALIZED | ❌ | ❌ | Read-only, no changes |

## Testing

### Unit Testing Strategy

- Each Intent handler has dedicated tests for success and guard failure cases
- Mock repositories for isolated state machine testing
- Validate state updates, side effects, and error handling

**Coverage**: 23 unit tests for EventManagementStateMachine workflow handlers

### Integration Testing Strategy

- Test complete workflow from DRAFT to FINALIZED
- Validate repository-mediated communication between state machines
- Test navigation side effects across transitions
- Verify business rule enforcement

**Coverage**: 6 integration tests in WorkflowIntegrationTest.kt

### Test Commands

```bash
# Run all state machine tests
./gradlew shared:jvmTest --tests "*EventManagementStateMachineTest*"
./gradlew shared:jvmTest --tests "*WorkflowIntegrationTest*"

# Run specific workflow test
./gradlew shared:jvmTest --tests "*completeWorkflowFromDraftToFinalized*"
```

## Related Specifications

- `event-organization`: Defines EventStatus enum and event lifecycle
- `scenario-management`: Defines scenario creation and voting
- `meeting-service`: Defines meeting link generation and management
- `collaboration-management`: Defines participant roles and permissions

## Implementation Files

### Contracts
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/presentation/state/EventManagementContract.kt`
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/presentation/state/ScenarioManagementContract.kt`
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/presentation/state/MeetingManagementContract.kt`

### State Machines
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/presentation/statemachine/EventManagementStateMachine.kt`
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/presentation/statemachine/ScenarioManagementStateMachine.kt`

### Tests
- `shared/src/commonTest/kotlin/com/guyghost/wakeve/presentation/statemachine/EventManagementStateMachineTest.kt`
- `shared/src/commonTest/kotlin/com/guyghost/wakeve/presentation/statemachine/ScenarioManagementStateMachineTest.kt`
- `shared/src/commonTest/kotlin/com/guyghost/wakeve/workflow/WorkflowIntegrationTest.kt`

## Change History

- **2025-12-31**: Initial specification created from `verify-statemachine-workflow` change
  - Added 5 new Intent handlers across 2 state machines
  - Implemented repository-mediated communication pattern
  - Added 29 tests (23 unit + 6 integration, 100% passing)
  - Established guard pattern for all transitions
  - Enabled complete DRAFT → FINALIZED workflow
