# State Machine Integration Guide

## Overview

The `EventManagementStateMachine` orchestrates the DRAFT workflow using the **MVI (Model-View-Intent)** pattern. This guide explains how to integrate the state machine with UI components and handle state changes effectively.

## Table of Contents

1. [State Machine Architecture](#state-machine-architecture)
2. [Intents](#intents)
3. [State Structure](#state-structure)
4. [Side Effects](#side-effects)
5. [Integration Patterns](#integration-patterns)
6. [Testing](#testing)
7. [Advanced Topics](#advanced-topics)

## State Machine Architecture

### MVI Pattern Overview

The state machine follows the **MVI (Model-View-Intent)** architecture:

```
┌─────────────────────────────────────┐
│           View/UI                   │
│    (Compose/SwiftUI Component)      │
└──────────────┬──────────────────────┘
               │ dispatch(Intent)
               ▼
┌─────────────────────────────────────┐
│    EventManagementStateMachine      │
│        (State Machine)              │
│                                     │
│  ┌──────────────────────────────┐   │
│  │  Reducer (Pure Function)     │   │
│  │  Intent + State -> NewState  │   │
│  └──────────────────────────────┘   │
└──────────────┬──────────────────────┘
               │
         ┌─────┴─────┐
         ▼           ▼
     State      SideEffect
   (Models)     (Navigation,
    Emitted      Toast, etc)
```

### Core Components

**Intent**
: User actions that trigger state changes (e.g., `CreateEvent`, `UpdateDraftEvent`)

**State**
: Current state of the application (e.g., event data, current wizard step)

**SideEffect**
: External effects triggered by state changes (e.g., navigation, showing toasts)

## Intents

### Available Intents for DRAFT Workflow

#### CreateEvent

Creates a new event in DRAFT status.

```kotlin
Intent.CreateEvent(
    title: String,
    description: String,
    eventType: EventType = EventType.OTHER,
    eventTypeCustom: String? = null
)
```

**State Changes:**
- Creates new Event with status = DRAFT
- Generates unique event ID
- Sets creation timestamp

**Side Effects:**
- Auto-saves event to repository

#### UpdateDraftEvent

Updates an existing DRAFT event with new data.

```kotlin
Intent.UpdateDraftEvent(
    event: Event  // Event with updated fields
)
```

**Validates:**
- Event must exist
- Event must be in DRAFT or COMPARING status
- Required fields must be filled

**State Changes:**
- Updates event in state
- Persists to repository

#### AddPotentialLocation

Adds a potential location to the event.

```kotlin
Intent.AddPotentialLocation(
    location: PotentialLocation
)
```

**Validates:**
- Event must exist
- Location must be valid (has coordinates or address)

**State Changes:**
- Adds location to event.potentialLocations list

#### RemovePotentialLocation

Removes a potential location from the event.

```kotlin
Intent.RemovePotentialLocation(
    locationId: String
)
```

#### AddTimeSlot

Adds a time slot to the event.

```kotlin
Intent.AddTimeSlot(
    timeSlot: TimeSlot
)
```

**Validates:**
- Event must exist
- TimeSlot must have valid start/end dates
- Start date must be <= end date

#### RemoveTimeSlot

Removes a time slot from the event.

```kotlin
Intent.RemoveTimeSlot(
    slotId: String
)
```

#### StartPoll

Transitions event from DRAFT to POLLING status and starts the poll.

```kotlin
Intent.StartPoll(eventId: String)
```

**Validates:**
- Event must be in DRAFT status
- Event must have title and description
- Event must have at least one time slot

**State Changes:**
- Changes status from DRAFT to POLLING
- Initializes poll with proposed slots
- Locks DRAFT editing

**Side Effects:**
- Navigates to poll view
- Notifies all participants

### Intent Dispatch Pattern

**Android (Kotlin):**
```kotlin
stateMachine.dispatch(Intent.CreateEvent(
    title = "Team Retreat",
    description = "Annual team building",
    eventType = EventType.TEAM_BUILDING
))
```

**iOS (Swift):**
```swift
stateMachine.dispatch(intent: EventManagementContract.Intent.CreateEvent(
    title: "Team Retreat",
    description: "Annual team building",
    eventType: .teamBuilding,
    eventTypeCustom: nil
))
```

## State Structure

### EventManagementState

The main state object containing all DRAFT workflow data.

```kotlin
data class EventManagementState(
    val event: Event? = null,
    val wizardStep: DraftStep = DraftStep.BasicInfo,
    val isLoading: Boolean = false,
    val error: String? = null,
    val validationErrors: Map<String, String> = emptyMap(),
    val isSaving: Boolean = false,
)

enum class DraftStep {
    BasicInfo,        // Step 1: Title, Description, EventType
    Participants,     // Step 2: Min/Max/Expected participants
    Locations,        // Step 3: Potential locations
    TimeSlots,        // Step 4: Time slots for voting
}
```

### Event Model (Relevant Fields)

```kotlin
data class Event(
    val id: String,
    val title: String,
    val description: String,
    val status: EventStatus = EventStatus.DRAFT,
    val eventType: EventType = EventType.OTHER,
    val eventTypeCustom: String? = null,
    val minParticipants: Int? = null,
    val maxParticipants: Int? = null,
    val expectedParticipants: Int? = null,
    val potentialLocations: List<PotentialLocation> = emptyList(),
    val timeSlots: List<TimeSlot> = emptyList(),
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
```

### Observing State

**Android (Flow):**
```kotlin
@Composable
fun EventCreationScreen(viewModel: EventCreationViewModel) {
    val state by viewModel.eventState.collectAsState()
    
    // Use state values
    when (state.wizardStep) {
        DraftStep.BasicInfo -> BasicInfoStep(state.event)
        DraftStep.Participants -> ParticipantsStep(state.event)
        DraftStep.Locations -> LocationsStep(state.event)
        DraftStep.TimeSlots -> TimeSlotsStep(state.event)
    }
}
```

**iOS (ObservedRealmObject):**
```swift
@ObservedRealmObject var state: EventManagementState

var body: some View {
    switch state.wizardStep {
    case .basicInfo:
        BasicInfoStep(event: state.event)
    case .participants:
        ParticipantsStep(event: state.event)
    case .locations:
        LocationsStep(event: state.event)
    case .timeSlots:
        TimeSlotsStep(event: state.event)
    }
}
```

## Side Effects

### Available Side Effects

#### NavigateTo

Navigates to a specific route.

```kotlin
SideEffect.NavigateTo(route: String)
```

**Examples:**
```kotlin
SideEffect.NavigateTo("events")                  // Event list
SideEffect.NavigateTo("draft/{eventId}/step/1")  // Step 1 of wizard
SideEffect.NavigateTo("poll/{eventId}")          // Poll setup
```

#### ShowToast

Shows a temporary message to the user.

```kotlin
SideEffect.ShowToast(message: String)
```

**Examples:**
```kotlin
SideEffect.ShowToast("Événement créé avec succès")
SideEffect.ShowToast("Données sauvegardées")
```

#### ShowError

Shows an error message or dialog.

```kotlin
SideEffect.ShowError(error: Throwable)
```

#### HideKeyboard

Dismisses the soft keyboard.

```kotlin
SideEffect.HideKeyboard
```

### Collecting Side Effects

**Android (Kotlin):**
```kotlin
LaunchedEffect(Unit) {
    viewModel.sideEffects.collect { sideEffect ->
        when (sideEffect) {
            is SideEffect.NavigateTo -> {
                navController.navigate(sideEffect.route)
            }
            is SideEffect.ShowToast -> {
                Toast.makeText(context, sideEffect.message, Toast.LENGTH_SHORT).show()
            }
            is SideEffect.ShowError -> {
                showErrorDialog(sideEffect.error.message)
            }
            SideEffect.HideKeyboard -> {
                focusManager.clearFocus()
            }
        }
    }
}
```

**iOS (SwiftUI):**
```swift
.onReceive(viewModel.sideEffects) { sideEffect in
    switch sideEffect {
    case .navigateTo(let route):
        navigationController?.navigate(to: route)
    case .showToast(let message):
        showToastMessage(message)
    case .showError(let error):
        presentErrorAlert(error)
    case .hideKeyboard:
        UIApplication.shared.sendAction(
            #selector(UIResponder.resignFirstResponder),
            to: nil, from: nil, for: nil
        )
    }
}
```

## Integration Patterns

### ViewModel Pattern

**Android ViewModel:**
```kotlin
class EventCreationViewModel(
    private val eventRepository: EventRepository,
) : ViewModel() {
    
    private val _eventState = MutableStateFlow(EventManagementState())
    val eventState: StateFlow<EventManagementState> = _eventState.asStateFlow()
    
    private val _sideEffects = Channel<SideEffect>()
    val sideEffects: Flow<SideEffect> = _sideEffects.receiveAsFlow()
    
    private val stateMachine = EventManagementStateMachine(
        initialState = EventManagementState(),
        repository = eventRepository,
        coroutineScope = viewModelScope,
    )
    
    init {
        viewModelScope.launch {
            stateMachine.state.collect { newState ->
                _eventState.value = newState
            }
        }
        
        viewModelScope.launch {
            stateMachine.sideEffects.collect { effect ->
                _sideEffects.send(effect)
            }
        }
    }
    
    fun dispatch(intent: Intent) {
        stateMachine.dispatch(intent)
    }
}
```

**iOS ViewModel:**
```swift
@MainActor
class EventCreationViewModel: ObservableObject {
    @Published var eventState: EventManagementState
    
    private let stateMachine: EventManagementStateMachine
    private let cancellables = Set<AnyCancellable>()
    
    init(repository: EventRepository) {
        self.stateMachine = EventManagementStateMachine(
            initialState: EventManagementState(),
            repository: repository
        )
        
        stateMachine.statePublisher
            .receive(on: DispatchQueue.main)
            .assign(to: &$eventState)
    }
    
    func dispatch(intent: EventManagementContract.Intent) {
        stateMachine.dispatch(intent: intent)
    }
}
```

### Composable Pattern

```kotlin
@Composable
fun DraftEventWizard(
    stateMachine: EventManagementStateMachine,
    onEventCreated: (String) -> Unit,
) {
    val state by stateMachine.state.collectAsState()
    
    LaunchedEffect(Unit) {
        stateMachine.sideEffects.collect { effect ->
            when (effect) {
                is SideEffect.NavigateTo -> {
                    if (effect.route.startsWith("poll/")) {
                        val eventId = effect.route.substringAfter("poll/")
                        onEventCreated(eventId)
                    }
                }
                else -> {}
            }
        }
    }
    
    Column {
        ProgressBar(step = state.wizardStep)
        
        when (state.wizardStep) {
            DraftStep.BasicInfo -> BasicInfoStep(
                event = state.event,
                errors = state.validationErrors,
                onNext = { title, description, eventType ->
                    stateMachine.dispatch(
                        Intent.CreateEvent(title, description, eventType)
                    )
                }
            )
            // ... other steps
        }
    }
}
```

## Testing

### Unit Testing State Machine

```kotlin
@Test
fun `createEvent should save event to repository`() = runTest {
    val repository = MockEventRepository()
    val stateMachine = EventManagementStateMachine(
        initialState = EventManagementState(),
        repository = repository,
        coroutineContext = coroutineContext,
    )
    
    // Act
    stateMachine.dispatch(Intent.CreateEvent(
        title = "Test Event",
        description = "Test Description",
        eventType = EventType.OTHER
    ))
    advanceUntilIdle()
    
    // Assert
    val savedEvent = repository.getEvent("test-event-id")
    assertNotNull(savedEvent)
    assertEquals("Test Event", savedEvent?.title)
}
```

### Testing Side Effects

```kotlin
@Test
fun `startPoll should emit navigateTo side effect`() = runTest {
    val repository = MockEventRepository()
    val stateMachine = EventManagementStateMachine(
        initialState = EventManagementState(),
        repository = repository,
        coroutineContext = coroutineContext,
    )
    
    // Act
    stateMachine.dispatch(Intent.StartPoll("event-1"))
    advanceUntilIdle()
    
    // Assert
    val sideEffects = mutableListOf<SideEffect>()
    viewModelScope.launch {
        stateMachine.sideEffects.collect { sideEffects.add(it) }
    }
    advanceUntilIdle()
    
    val navigateEffect = sideEffects.find { it is SideEffect.NavigateTo }
    assertNotNull(navigateEffect)
}
```

## Advanced Topics

### Custom Reducers

Extend the state machine with custom behavior:

```kotlin
class CustomEventStateMachine(
    initialState: EventManagementState,
    repository: EventRepository,
) : EventManagementStateMachine(initialState, repository) {
    
    override suspend fun reduce(intent: Intent, state: EventManagementState): Pair<EventManagementState, SideEffect?> {
        return when (intent) {
            is Intent.CreateEventWithTemplate -> {
                // Custom logic for template-based creation
                super.reduce(
                    Intent.CreateEvent(
                        title = intent.template.title,
                        description = intent.template.description
                    ),
                    state
                )
            }
            else -> super.reduce(intent, state)
        }
    }
}
```

### Middleware Pattern

Add middleware for logging, analytics, or side effects:

```kotlin
class LoggingMiddleware(
    private val stateMachine: EventManagementStateMachine,
) {
    fun logIntent(intent: Intent) {
        Log.d("StateMachine", "Intent dispatched: ${intent::class.simpleName}")
    }
    
    fun logStateChange(newState: EventManagementState) {
        Log.d("StateMachine", "State updated: ${newState.event?.id}")
    }
}
```

### Offline Support

The state machine automatically handles offline scenarios:

```kotlin
val offlineRepository = OfflineSyncRepository(
    localDb = database,
    remoteApi = apiClient,
)

val stateMachine = EventManagementStateMachine(
    initialState = EventManagementState(),
    repository = offlineRepository,  // Automatically syncs on reconnection
)
```

## See Also

- [DraftEventWizard Usage Guide](./DRAFT_WORKFLOW_GUIDE.md)
- [Workflow Coordination Specification](../specs/workflow-coordination/)
- [Event Organization Specification](../specs/event-organization/)
