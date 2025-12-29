# iOS ViewModel Integration Guide

## Overview

Two new ViewModels have been created to manage the state and intents for displaying events in iOS using SwiftUI:

1. **EventListViewModel** - Manages a list of events
2. **EventDetailViewModel** - Manages details for a single event

Both ViewModels use the shared Kotlin `EventManagementStateMachine` and are created via the `IosFactory`.

## Architecture

```
SwiftUI View
    ↓
EventListViewModel / EventDetailViewModel
    ↓
ViewModelWrapper (Kotlin/Native bridge)
    ↓
EventManagementStateMachine (shared Kotlin)
    ↓
EventRepositoryInterface (shared Kotlin)
    ↓
SQLDelight Database (SQLite)
```

## Files Created

### 1. EventListViewModel.swift
Location: `iosApp/iosApp/ViewModels/EventListViewModel.swift`

**Purpose**: Manage the state and intents for displaying a list of events.

**Published Properties**:
- `state: EventManagementContractState` - Current state from the state machine
- `toastMessage: String?` - Toast message to display
- `navigationRoute: String?` - Navigation route to trigger
- `shouldNavigateBack: Bool` - Whether to pop back to previous screen

**Key Methods**:
- `init()` - Creates state machine via `IosFactory` and loads events
- `dispatch(_ intent:)` - Dispatches intents to the state machine

**Side Effect Handling**:
- `ShowToast` → Sets `toastMessage`
- `NavigateTo` → Sets `navigationRoute`
- `NavigateBack` → Sets `shouldNavigateBack`

### 2. EventDetailViewModel.swift
Location: `iosApp/iosApp/ViewModels/EventDetailViewModel.swift`

**Purpose**: Manage the state and intents for displaying detailed information about a single event.

**Published Properties**:
- `state: EventManagementContractState` - Current state from the state machine
- `selectedEvent: Event?` - The currently selected event (filtered from state.events)
- `toastMessage: String?` - Toast message to display
- `navigationRoute: String?` - Navigation route to trigger
- `shouldNavigateBack: Bool` - Whether to pop back to previous screen

**Key Methods**:
- `init(eventId: String)` - Creates state machine and loads participant/poll data
- `dispatch(_ intent:)` - Dispatches intents to the state machine
- `updateSelectedEvent()` - Filters the selected event from state.events

**Side Effect Handling**: Same as EventListViewModel

## Usage Examples

### EventListViewModel

```swift
import SwiftUI
import Shared

struct EventListView: View {
    @StateObject private var viewModel = EventListViewModel()
    @State private var showingCreateSheet = false
    
    var body: some View {
        NavigationStack {
            ZStack {
                if viewModel.state.isLoading {
                    ProgressView()
                } else if viewModel.state.isEmpty {
                    Text("Aucun événement créé")
                        .foregroundColor(.gray)
                } else {
                    List(viewModel.state.events) { event in
                        NavigationLink(destination: EventDetailView(eventId: event.id)) {
                            EventRow(event: event)
                        }
                    }
                }
                
                if let message = viewModel.toastMessage {
                    VStack {
                        Spacer()
                        Text(message)
                            .padding()
                            .background(Color.black)
                            .foregroundColor(.white)
                            .cornerRadius(8)
                            .padding()
                    }
                    .onAppear {
                        DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
                            viewModel.toastMessage = nil
                        }
                    }
                }
            }
            .navigationTitle("Événements")
            .toolbar {
                ToolbarItem(placement: .primaryAction) {
                    Button(action: { showingCreateSheet = true }) {
                        Image(systemName: "plus")
                    }
                }
            }
            .sheet(isPresented: $showingCreateSheet) {
                EventCreationSheet(viewModel: viewModel)
            }
        }
    }
}

#Preview {
    EventListView()
}
```

### EventDetailViewModel

```swift
import SwiftUI
import Shared

struct EventDetailView: View {
    let eventId: String
    @StateObject private var viewModel: EventDetailViewModel
    @Environment(\.dismiss) var dismiss
    
    init(eventId: String) {
        self.eventId = eventId
        _viewModel = StateObject(wrappedValue: EventDetailViewModel(eventId: eventId))
    }
    
    var body: some View {
        ZStack {
            if let event = viewModel.selectedEvent {
                VStack {
                    Text(event.title)
                        .font(.title2)
                        .fontWeight(.bold)
                    
                    Text(event.eventDescription ?? "")
                        .font(.body)
                        .foregroundColor(.gray)
                    
                    Divider()
                    
                    VStack(alignment: .leading, spacing: 12) {
                        Label("Participants: \(viewModel.state.participantIds.count)", 
                              systemImage: "person.2.fill")
                        Label("Votes: \(viewModel.state.pollVotes.count)", 
                              systemImage: "checkmark.circle.fill")
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding()
                    .background(Color(.systemGray6))
                    .cornerRadius(8)
                    
                    Spacer()
                    
                    HStack(spacing: 12) {
                        Button(role: .destructive) {
                            viewModel.dispatch(
                                EventManagementContractIntent.deleteEvent(eventId: eventId)
                            )
                        } label: {
                            Label("Supprimer", systemImage: "trash")
                        }
                        .buttonStyle(.bordered)
                        
                        Button {
                            // Edit action
                        } label: {
                            Label("Modifier", systemImage: "pencil")
                        }
                        .buttonStyle(.borderedProminent)
                    }
                }
                .padding()
            } else if viewModel.state.isLoading {
                ProgressView()
            } else {
                Text("Événement non trouvé")
                    .foregroundColor(.red)
            }
            
            // Toast handling
            if let message = viewModel.toastMessage {
                VStack {
                    Spacer()
                    Text(message)
                        .padding()
                        .background(Color.black)
                        .foregroundColor(.white)
                        .cornerRadius(8)
                        .padding()
                }
                .onAppear {
                    DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
                        viewModel.toastMessage = nil
                    }
                }
            }
        }
        .navigationTitle("Détails de l'événement")
        .navigationBarTitleDisplayMode(.inline)
        .onReceive(Just(viewModel.shouldNavigateBack)) { shouldGoBack in
            if shouldGoBack {
                dismiss()
            }
        }
    }
}

#Preview {
    NavigationStack {
        EventDetailView(eventId: "event-123")
    }
}
```

## Integration with RepositoryProvider

Both ViewModels use `RepositoryProvider.shared.database` to access the SQLDelight database:

```swift
let database = RepositoryProvider.shared.database
self.stateMachineWrapper = IosFactory().createEventStateMachine(database: database)
```

This ensures:
- Single instance of the database throughout the app
- Consistent persistence layer
- Offline-first capability via SQLDelight

## Intent Helpers

Both ViewModels include extension methods on `EventManagementContractIntent` for convenient intent creation:

```swift
// EventListViewModel helpers
EventManagementContractIntent.loadEvents()
EventManagementContractIntent.selectEvent(eventId: eventId)
EventManagementContractIntent.clearError()

// EventDetailViewModel helpers
EventManagementContractIntent.loadParticipants(eventId: eventId)
EventManagementContractIntent.loadPollResults(eventId: eventId)
EventManagementContractIntent.deleteEvent(eventId: eventId)
EventManagementContractIntent.updateEvent(event)
```

## Thread Safety

Both ViewModels use `@MainActor` to ensure all UI updates happen on the main thread:

```swift
@MainActor
class EventListViewModel: ObservableObject { ... }

@MainActor
class EventDetailViewModel: ObservableObject { ... }
```

State changes from the Kotlin side are dispatched to the main thread via `DispatchQueue.main.async`.

## Resource Management

Both ViewModels properly clean up resources in their `deinit`:

```swift
deinit {
    stateMachineWrapper.dispose()
}
```

This ensures:
- Coroutine scope is cancelled
- State change callbacks are cleared
- Memory is properly released

## Testing

To test these ViewModels in SwiftUI Previews:

```swift
#Preview {
    EventListView()
}

#Preview {
    NavigationStack {
        EventDetailView(eventId: "test-event-123")
    }
}
```

The Previews will create a fresh ViewModel instance and display the UI with mock/cached data.

## Next Steps

1. **Create EventListView** - Use EventListViewModel to display list of events
2. **Create EventDetailView** - Use EventDetailViewModel to display event details
3. **Wire up navigation** - Connect views with NavigationStack or NavigationLink
4. **Implement event creation** - Add EventCreationSheet that dispatches CreateEvent intent
5. **Add error handling** - Display error messages from state.error

## Related Files

- **Shared State Machine**: `shared/src/commonMain/kotlin/com/guyghost/wakeve/presentation/statemachine/EventManagementStateMachine.kt`
- **State Contract**: `shared/src/commonMain/kotlin/com/guyghost/wakeve/presentation/state/EventManagementContract.kt`
- **iOS Factory**: `shared/src/iosMain/kotlin/com/guyghost/wakeve/di/IosFactory.kt`
- **Repository Provider**: `iosApp/iosApp/Services/RepositoryProvider.swift`

## Architecture Diagram

```
EventListView
    ↓
@StateObject EventListViewModel
    ├─ state: EventManagementContractState
    ├─ toastMessage: String?
    ├─ navigationRoute: String?
    └─ dispatch(_ intent:)
        ↓
    ViewModelWrapper<State, Intent, SideEffect>
        ↓
    EventManagementStateMachine (Kotlin)
        ├─ LoadEventsUseCase
        ├─ CreateEventUseCase
        └─ EventRepositoryInterface
            ↓
        DatabaseEventRepository
            ↓
        SQLDelight / WakevDb (SQLite)
```

## Known Limitations

Currently, the ViewModels use a generic state machine that handles all events. In the future, we could:

1. Create specialized state machines for different features
2. Implement view-specific state filtering
3. Add local state for UI-only concerns (animations, selections)
4. Implement proper error boundaries per view

## References

- [iOS State Management](https://developer.apple.com/documentation/combine)
- [SwiftUI ObservableObject](https://developer.apple.com/documentation/combine/observableobject)
- [Kotlin/Native Interop](https://kotlinlang.org/docs/native-objc-interop.html)

---

**Created**: 2025-12-29  
**Version**: 1.0.0  
**Status**: Ready for Integration
