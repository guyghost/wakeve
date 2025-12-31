# Workflow Coordination Diagrams

> **Change ID**: `verify-statemachine-workflow`  
> **Phase**: 7 - Documentation  
> **Date**: 2025-12-31

---

## Overview

This document provides comprehensive visual documentation of the workflow coordination between state machines in Wakeve, demonstrating how EventManagementStateMachine, ScenarioManagementStateMachine, and MeetingServiceStateMachine coordinate through repository-mediated communication.

---

## Complete Workflow Sequence Diagram

```mermaid
sequenceDiagram
    participant User
    participant EventMgmt as EventManagement<br/>StateMachine
    participant Repo as Event<br/>Repository
    participant ScenarioMgmt as ScenarioManagement<br/>StateMachine
    participant MeetingMgmt as MeetingService<br/>StateMachine

    Note over User,MeetingMgmt: Phase 1: Event Creation (DRAFT)
    User->>EventMgmt: CreateEvent
    EventMgmt->>Repo: Save Event(status=DRAFT)
    Repo-->>EventMgmt: Event created
    EventMgmt-->>User: Navigate to EventDetail

    Note over User,MeetingMgmt: Phase 2: Poll Management (POLLING)
    User->>EventMgmt: StartPoll(eventId)
    EventMgmt->>Repo: Update Event(status=POLLING)
    Repo-->>EventMgmt: Status updated
    EventMgmt-->>User: Show poll UI
    
    User->>EventMgmt: Vote on slots (multiple users)
    EventMgmt->>Repo: Save votes
    
    Note over User,MeetingMgmt: Phase 3: Date Confirmation (CONFIRMED → Scenarios)
    User->>EventMgmt: ConfirmDate(eventId, slotId)
    EventMgmt->>Repo: Update Event(status=CONFIRMED, finalDate, scenariosUnlocked=true)
    Repo-->>EventMgmt: Status updated
    EventMgmt-->>User: NavigateTo("scenarios/{eventId}")
    
    Note over User,MeetingMgmt: Phase 4: Scenario Management (COMPARING)
    User->>ScenarioMgmt: Load scenarios
    ScenarioMgmt->>Repo: Read Event(eventId)
    Repo-->>ScenarioMgmt: Event(status=CONFIRMED)
    
    ScenarioMgmt->>ScenarioMgmt: canCreateScenarios() ✅
    Note right of ScenarioMgmt: CONFIRMED allows<br/>scenario creation
    
    User->>ScenarioMgmt: CreateScenario (destination, lodging, activities)
    ScenarioMgmt->>Repo: Save Scenario
    
    User->>ScenarioMgmt: VoteOnScenario (multiple users)
    ScenarioMgmt->>Repo: Save scenario votes
    
    opt Optional: Select Final Scenario
        User->>ScenarioMgmt: SelectScenarioAsFinal(scenarioId)
        ScenarioMgmt->>Repo: Update Scenario(isFinal=true)
        ScenarioMgmt-->>User: NavigateTo("meetings/{eventId}")
    end
    
    Note over User,MeetingMgmt: Phase 5: Transition to Organization (ORGANIZING)
    User->>EventMgmt: TransitionToOrganizing(eventId)
    EventMgmt->>Repo: Read Event(eventId)
    EventMgmt->>EventMgmt: Validate Event.status == CONFIRMED ✅
    EventMgmt->>Repo: Update Event(status=ORGANIZING, meetingsUnlocked=true)
    Repo-->>EventMgmt: Status updated
    EventMgmt-->>User: NavigateTo("meetings/{eventId}")
    
    Note over User,MeetingMgmt: Phase 6: Meeting Management
    User->>MeetingMgmt: Load meetings
    MeetingMgmt->>Repo: Read Event(eventId)
    Repo-->>MeetingMgmt: Event(status=ORGANIZING)
    
    MeetingMgmt->>MeetingMgmt: canCreateMeetings() ✅
    Note right of MeetingMgmt: ORGANIZING allows<br/>meeting creation
    
    User->>MeetingMgmt: CreateMeeting (Zoom/Meet/FaceTime)
    MeetingMgmt->>Repo: Save Meeting
    MeetingMgmt-->>User: Show meeting details
    
    Note over User,MeetingMgmt: Phase 7: Event Finalization (FINALIZED)
    User->>EventMgmt: MarkAsFinalized(eventId)
    EventMgmt->>Repo: Read Event(eventId)
    EventMgmt->>EventMgmt: Validate Event.status == ORGANIZING ✅
    EventMgmt->>Repo: Update Event(status=FINALIZED)
    Repo-->>EventMgmt: Status updated
    EventMgmt-->>User: Show finalized event
    
    Note over User,MeetingMgmt: ✅ Workflow Complete: DRAFT → FINALIZED
```

---

## State Transition Diagram

```mermaid
stateDiagram-v2
    [*] --> DRAFT: CreateEvent
    
    DRAFT --> POLLING: StartPoll
    note right of DRAFT
        • Can edit event details
        • Cannot create scenarios
        • Cannot create meetings
    end note
    
    POLLING --> CONFIRMED: ConfirmDate
    note right of POLLING
        • Users vote on slots
        • Cannot create scenarios yet
        • Cannot create meetings
    end note
    
    CONFIRMED --> COMPARING: (Implicit - Navigate to scenarios)
    note right of CONFIRMED
        • scenariosUnlocked = true
        • Can create scenarios ✅
        • Cannot create meetings yet
        • NavigateTo("scenarios/{id}")
    end note
    
    COMPARING --> CONFIRMED: SelectScenarioAsFinal (Optional)
    note right of COMPARING
        • Users vote on scenarios
        • Can select final scenario
        • NavigateTo("meetings/{id}")
    end note
    
    CONFIRMED --> ORGANIZING: TransitionToOrganizing
    note right of ORGANIZING
        • meetingsUnlocked = true
        • Can create meetings ✅
        • Cannot create scenarios anymore
        • NavigateTo("meetings/{id}")
    end note
    
    ORGANIZING --> FINALIZED: MarkAsFinalized
    note right of FINALIZED
        • Event locked
        • No more changes allowed
        • All features read-only
    end note
    
    FINALIZED --> [*]
```

---

## Repository-Mediated Communication Pattern

```mermaid
sequenceDiagram
    participant SM1 as State Machine 1<br/>(EventManagement)
    participant Repo as Shared Repository<br/>(EventRepository)
    participant SM2 as State Machine 2<br/>(ScenarioManagement)

    Note over SM1,SM2: Repository-Mediated Communication Pattern

    SM1->>SM1: User triggers Intent (e.g., ConfirmDate)
    SM1->>SM1: Validate current state
    SM1->>Repo: Update Event.status = CONFIRMED
    SM1->>Repo: Set Event.scenariosUnlocked = true
    Repo->>Repo: Persist changes
    Repo-->>SM1: Success
    SM1-->>SM1: Emit NavigateTo("scenarios/{id}")
    
    Note over SM1,SM2: Time passes - User navigates to scenarios screen
    
    SM2->>Repo: Read Event(eventId)
    Repo-->>SM2: Event(status=CONFIRMED, scenariosUnlocked=true)
    SM2->>SM2: Update State.eventStatus = CONFIRMED
    SM2->>SM2: canCreateScenarios() checks eventStatus
    SM2-->>SM2: Returns true (CONFIRMED allows creation) ✅
    
    Note over SM1,SM2: State Machines never communicate directly<br/>All coordination through shared repository state
```

---

## Business Rule Enforcement

```mermaid
graph TD
    A[User Action: Create Scenario] --> B{Read Event.status<br/>from Repository}
    
    B -->|DRAFT| C[❌ canCreateScenarios = false]
    B -->|POLLING| D[❌ canCreateScenarios = false]
    B -->|COMPARING| E[✅ canCreateScenarios = true]
    B -->|CONFIRMED| F[✅ canCreateScenarios = true]
    B -->|ORGANIZING| G[❌ canCreateScenarios = false]
    B -->|FINALIZED| H[❌ canCreateScenarios = false]
    
    C --> I[Show error: Date not confirmed yet]
    D --> I
    E --> J[Allow scenario creation]
    F --> J
    G --> K[Show error: Event already organizing]
    H --> K
    
    style C fill:#ffcccc
    style D fill:#ffcccc
    style E fill:#ccffcc
    style F fill:#ccffcc
    style G fill:#ffcccc
    style H fill:#ffcccc
```

---

## Navigation Flow

```mermaid
flowchart TD
    A[Event List] -->|CreateEvent| B[Event Detail<br/>DRAFT]
    B -->|StartPoll| C[Event Detail<br/>POLLING]
    C -->|ConfirmDate| D[Event Detail<br/>CONFIRMED]
    
    D -->|NavigateTo scenarios| E[Scenario List<br/>COMPARING]
    E -->|CreateScenario| F[Scenario Detail]
    F -->|VoteScenario| E
    E -->|SelectScenarioAsFinal| G[Navigate to Meetings]
    
    D -->|TransitionToOrganizing| H[Event Detail<br/>ORGANIZING]
    G -->|After transition| H
    H -->|NavigateTo meetings| I[Meeting List]
    
    I -->|CreateMeeting| J[Meeting Detail]
    J -->|Back| I
    
    H -->|MarkAsFinalized| K[Event Detail<br/>FINALIZED]
    
    style D fill:#ffffcc
    style E fill:#ccffff
    style H fill:#ffccff
    style K fill:#ccffcc
```

---

## Event Status Guards

```mermaid
graph LR
    subgraph EventManagementStateMachine
        A1[StartPoll] -->|Guard| A2{Status == DRAFT?}
        A2 -->|Yes ✅| A3[Update to POLLING]
        A2 -->|No ❌| A4[Reject: Already started]
        
        B1[ConfirmDate] -->|Guard| B2{Status == POLLING?}
        B2 -->|Yes ✅| B3[Update to CONFIRMED]
        B2 -->|No ❌| B4[Reject: Not polling]
        
        C1[TransitionToOrganizing] -->|Guard| C2{Status == CONFIRMED?}
        C2 -->|Yes ✅| C3[Update to ORGANIZING]
        C2 -->|No ❌| C4[Reject: Date not confirmed]
        
        D1[MarkAsFinalized] -->|Guard| D2{Status == ORGANIZING?}
        D2 -->|Yes ✅| D3[Update to FINALIZED]
        D2 -->|No ❌| D4[Reject: Not organizing]
    end
    
    style A3 fill:#ccffcc
    style A4 fill:#ffcccc
    style B3 fill:#ccffcc
    style B4 fill:#ffcccc
    style C3 fill:#ccffcc
    style C4 fill:#ffcccc
    style D3 fill:#ccffcc
    style D4 fill:#ffcccc
```

---

## Integration Test Coverage

```mermaid
graph TD
    subgraph WorkflowIntegrationTest
        T1[testCompleteWorkflow_DraftToFinalized]
        T2[testEventStatusPropagation_ThroughRepository]
        T3[testCanCreateScenarios_BasedOnEventStatus]
        T4[testNavigationSideEffects_AcrossWorkflow]
        T5[testRepositoryMediatedCommunication]
        T6[testWorkflowTransitionValidation]
    end
    
    T1 --> V1[✅ Validates complete workflow]
    T2 --> V2[✅ Validates status propagation]
    T3 --> V3[✅ Validates business rules]
    T4 --> V4[✅ Validates navigation]
    T5 --> V5[✅ Validates communication pattern]
    T6 --> V6[✅ Validates guards]
    
    V1 --> R[6/6 Tests Passing]
    V2 --> R
    V3 --> R
    V4 --> R
    V5 --> R
    V6 --> R
    
    style R fill:#ccffcc,stroke:#00aa00,stroke-width:3px
```

---

## Key Architectural Patterns

### 1. Repository-Mediated Communication

**Pattern**: State machines communicate indirectly through shared repository state.

```kotlin
// State Machine 1: Updates shared state
eventStateMachine.dispatch(Intent.StartPoll("event-1"))
// → Repository: Event.status = POLLING

// State Machine 2: Reads shared state
val event = eventRepository.getEvent("event-1")
val canCreate = when (event?.status) {
    EventStatus.CONFIRMED, EventStatus.COMPARING -> true
    else -> false
}
```

**Benefits**:
- ✅ Loose coupling between state machines
- ✅ Strong consistency through shared repository
- ✅ Simple testing (mock repository only)
- ✅ Clear source of truth (Event.status)

### 2. Navigation Side Effects

**Pattern**: State machines emit navigation side effects after status transitions.

```kotlin
// EventManagementStateMachine
handleConfirmDate(eventId, slotId) {
    // Update status
    repository.updateEvent(eventId, status = CONFIRMED, finalDate = date)
    
    // Emit navigation
    emitSideEffect(NavigateTo("scenarios/$eventId"))
}
```

**Benefits**:
- ✅ Decoupled UI navigation from business logic
- ✅ Testable navigation flows
- ✅ Flexible UI implementation (Compose/SwiftUI)

### 3. Guard Pattern

**Pattern**: Validate EventStatus before executing state transitions.

```kotlin
// Example guard in ConfirmDate handler
if (event.status != EventStatus.POLLING) {
    emitSideEffect(ShowError("Cannot confirm date: event not in polling phase"))
    return
}

// Proceed with transition
repository.updateEvent(eventId, status = CONFIRMED)
```

**Benefits**:
- ✅ Prevents invalid state transitions
- ✅ Clear error messages for users
- ✅ Maintains state machine integrity

---

## Workflow Phases Summary

| Phase | EventStatus | Scenarios Allowed | Meetings Allowed | Navigation Trigger |
|-------|-------------|-------------------|------------------|--------------------|
| 1. Creation | DRAFT | ❌ | ❌ | - |
| 2. Polling | POLLING | ❌ | ❌ | StartPoll |
| 3. Date Confirmed | CONFIRMED | ✅ | ❌ | ConfirmDate → scenarios |
| 4. Scenario Voting | COMPARING | ✅ | ❌ | (implicit) |
| 5. Organization | ORGANIZING | ❌ | ✅ | TransitionToOrganizing → meetings |
| 6. Finalized | FINALIZED | ❌ | ❌ | MarkAsFinalized |

---

## Testing Strategy

### Unit Tests (EventManagementStateMachineTest)
- Test individual Intent handlers
- Validate status transitions
- Test guards and error cases
- **Coverage**: 13 tests, 100% passing

### Integration Tests (WorkflowIntegrationTest)
- Test cross-state-machine coordination
- Validate repository-mediated communication
- Test complete workflow (DRAFT → FINALIZED)
- Test business rule enforcement
- **Coverage**: 6 tests, 100% passing

---

## References

- **AUDIT.md**: Initial workflow analysis and gap identification
- **PHASE3_IMPLEMENTATION_COMPLETE.md**: Intent handler implementations
- **PHASE4_TESTING_COMPLETE.md**: Unit test details
- **PHASE5_INTEGRATION_COMPLETE.md**: Integration test validation
- **Contract Files**:
  - `EventManagementContract.kt`
  - `ScenarioManagementContract.kt`
  - `MeetingManagementContract.kt`
- **State Machine Files**:
  - `EventManagementStateMachine.kt`
  - `ScenarioManagementStateMachine.kt`

---

**Diagrams Complete**: ✅  
**Pattern Documentation**: ✅  
**Test Coverage**: 6/6 integration + 13/13 unit tests passing  
**Production Ready**: ✅
