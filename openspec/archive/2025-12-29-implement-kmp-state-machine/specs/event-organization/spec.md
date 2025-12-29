## MODIFIED Requirements

### Requirement: Event Management State Machine
The system SHALL implement a state machine for event management following the MVI/FSM pattern, centralizing all state and actions in `EventManagementStateMachine`.

#### Scenario: Initial state loading
- **WHEN** the events screen is opened
- **THEN** the initial state is `isLoading = true`
- **AND** `events` is an empty list
- **AND** `LoadEvents` intent is automatically dispatched
- **WHEN** loading completes successfully
- **THEN** `isLoading = false`
- **AND** `events` contains the full list
- **AND** `error = null`

#### Scenario: Handle error state
- **WHEN** an error occurs during loading
- **THEN** `isLoading = false`
- **AND** `events` remains empty
- **AND** `error` contains the error message
- **AND** `ShowToast` side effect is emitted with error message

#### Scenario: Select event navigation
- **WHEN** user clicks on an event
- **THEN** `SelectEvent(eventId)` intent is dispatched
- **AND** `selectedEvent` is updated
- **AND** `NavigateTo("detail/{eventId}")` side effect is emitted

#### Scenario: Create event flow
- **WHEN** user creates a new event
- **THEN** `CreateEvent(event)` intent is dispatched
- **AND** `isLoading = true` during creation
- **WHEN** creation succeeds
- **THEN** `isLoading = false`
- **AND** the new event is added to `events`
- **AND** `NavigateBack` side effect is emitted

#### Scenario: Side effects handling
- **WHEN** an action requires navigation
- **THEN** `NavigateTo(route)` side effect is emitted
- **WHEN** an action succeeds and goes back
- **THEN** `NavigateBack` side effect is emitted
- **WHEN** an action requires user feedback
- **THEN** `ShowToast(message)` side effect is emitted

## ADDED Requirements

### Requirement: State Machine Base Class
The system SHALL provide a base class `StateMachine<State, Intent, SideEffect>` that all state machines extend.

#### Scenario: State update mechanism
- **WHEN** `updateState(reducer)` is called
- **THEN** the state is immutable
- **AND** the transition is thread-safe via StateFlow
- **AND** subscribers receive the new state

#### Scenario: Intent dispatch
- **WHEN** `dispatch(intent)` is called
- **THEN** `handleIntent(intent)` is executed in a coroutine scope
- **AND** state can be updated
- **AND** side effects can be emitted

#### Scenario: Side effect emission
- **WHEN** `emitSideEffect(effect)` is called
- **THEN** the effect is emitted into the Channel
- **AND** subscribers to `sideEffect` flow receive it
- **AND** each effect is received only once (one-shot)

### Requirement: iOS Bridge ObservableStateMachine
The system SHALL provide a wrapper `ObservableStateMachine<State, Intent, SideEffect>` to expose Kotlin state machines to SwiftUI via `@Published` properties.

#### Scenario: State synchronization with SwiftUI
- **WHEN** the Kotlin state changes via StateFlow
- **THEN** the `onStateChange` callback is called on iOS main thread
- **AND** the `@Published` property is updated
- **AND** SwiftUI automatically receives the change

#### Scenario: Side effect handling in iOS
- **WHEN** a side effect is emitted from Kotlin
- **THEN** the `onSideEffect` callback is called on iOS main thread
- **AND** the callback can show a toast, navigate, etc.

#### Scenario: Intent dispatch from iOS
- **WHEN** Swift code calls `dispatch(intent)`
- **THEN** the intent is passed to the Kotlin state machine
- **AND** state is updated according to the intent
- **AND** side effects are emitted if needed

### Requirement: Dependency Injection with Koin
The system SHALL use Koin for cross-platform dependency injection.

#### Scenario: State Machine factory
- **WHEN** an Android ViewModel is created
- **THEN** `koinViewModel<EventManagementViewModel>()` injects the state machine
- **AND** the state machine receives its use cases from Koin
- **AND** a dedicated CoroutineScope is provided

#### Scenario: iOS factory
- **WHEN** an iOS ViewModel is created
- **THEN** `IosFactory.createEventStateMachine()` is called
- **AND** dependencies are injected from Koin
- **AND** `ObservableStateMachine` wrapper is returned

### Requirement: Use Cases Pattern
The system SHALL structure business logic in use cases that are consumed by state machines.

#### Scenario: LoadEventsUseCase execution
- **WHEN** `LoadEventsUseCase()` is invoked
- **THEN** it uses `EventRepository.getEvents()`
- **AND** returns `Result<List<Event>>`
- **AND** the state machine handles success or error

#### Scenario: CreateEventUseCase execution
- **WHEN** `CreateEventUseCase(event)` is invoked
- **THEN** it validates the event
- **AND** uses `EventRepository.createEvent(event)`
- **AND** returns `Result<Event>` with generated ID
