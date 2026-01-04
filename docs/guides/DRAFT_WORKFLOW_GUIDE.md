# DraftEventWizard Usage Guide

## Overview

`DraftEventWizard` is a multi-step wizard component that guides organizers through the event creation process with validation at each step. It provides a consistent user experience across Android (Jetpack Compose) and iOS (SwiftUI) platforms.

## Table of Contents

1. [Quick Start](#quick-start)
2. [Integration](#integration)
3. [Customization](#customization)
4. [State Machine Integration](#state-machine-integration)
5. [Validation](#validation)
6. [Best Practices](#best-practices)
7. [Troubleshooting](#troubleshooting)

## Quick Start

### Android (Jetpack Compose)

```kotlin
import com.guyghost.wakeve.ui.event.DraftEventWizard
import com.guyghost.wakeve.presentation.statemachine.EventManagementStateMachine

@Composable
fun EventCreationScreen(
    stateMachine: EventManagementStateMachine,
    onEventCreated: (eventId: String) -> Unit,
) {
    DraftEventWizard(
        stateMachine = stateMachine,
        onEventCreated = onEventCreated,
    )
}
```

### iOS (SwiftUI)

```swift
import Shared  // KMP shared module

struct EventCreationView: View {
    @StateObject var viewModel: EventCreationViewModel
    @ObservedRealmObject var eventStateMachine: EventManagementStateMachine
    
    var body: some View {
        DraftEventWizardView(
            stateMachine: eventStateMachine,
            onEventCreated: { eventId in
                // Handle event creation
            }
        )
    }
}
```

## Integration

### Step 1: Inject the State Machine

The `DraftEventWizard` requires an instance of `EventManagementStateMachine`. This should be provided via dependency injection or a view model.

**Android:**
```kotlin
val viewModel: EventCreationViewModel by viewModels()

DraftEventWizard(
    stateMachine = viewModel.stateMachine,
    onEventCreated = { eventId ->
        navController.navigate("eventDetail/$eventId")
    }
)
```

**iOS:**
```swift
@StateObject var viewModel = EventCreationViewModel()

DraftEventWizardView(
    stateMachine: viewModel.eventStateMachine,
    onEventCreated: { eventId in
        navigationController?.pushViewController(
            EventDetailViewController(eventId: eventId),
            animated: true
        )
    }
)
```

### Step 2: Handle Side Effects

The state machine emits side effects for navigation, validation feedback, and auto-save operations.

**Android (Collect side effects):**
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
                showErrorDialog(sideEffect.error)
            }
        }
    }
}
```

**iOS (Observe side effects):**
```swift
.onReceive(viewModel.sideEffects) { sideEffect in
    switch sideEffect {
    case .navigateTo(let route):
        navigationController?.navigate(to: route)
    case .showToast(let message):
        showToast(message)
    case .showError(let error):
        showAlert(error.localizedDescription)
    }
}
```

### Step 3: Provide Callbacks

The wizard emits callbacks for key events:
- `onEventCreated(eventId)` - Called when the event is successfully created
- `onCancel()` - Called when the user cancels the wizard
- `onError(error)` - Called when an error occurs

## Customization

### Custom Step Content

You can customize the content of each step while keeping the wizard navigation logic:

**Android:**
```kotlin
DraftEventWizard(
    stateMachine = stateMachine,
    customStepContent = { step, event ->
        when (step) {
            DraftStep.BasicInfo -> CustomBasicInfoStep(event)
            DraftStep.Participants -> CustomParticipantsStep(event)
            DraftStep.Locations -> CustomLocationsStep(event)
            DraftStep.TimeSlots -> CustomTimeSlotsStep(event)
        }
    }
)
```

### Custom Validation Messages

Provide custom validation error messages:

**Android:**
```kotlin
DraftEventWizard(
    stateMachine = stateMachine,
    validationMessages = mapOf(
        "title_required" to "L'événement doit avoir un titre",
        "description_required" to "Veuillez ajouter une description",
    )
)
```

### Theme Customization

The wizard respects the app's theme system:

**Android (Material You):**
- Colors are inherited from `WakevColors`
- Typography follows `WakevTypography`
- Spacing uses `WakevDimensions`

**iOS (Liquid Glass):**
- Colors follow `WakevColors.swift`
- Uses `LiquidGlassModifier` for background effects
- Spacing follows `WakevTypography.swift`

## State Machine Integration

### Understanding Intents

The wizard dispatches intents to the state machine for each user action:

```kotlin
// Create Event (Step 1)
stateMachine.dispatch(Intent.CreateEvent(
    title = "Team Retreat",
    description = "Annual team building event",
    eventType = EventType.TEAM_BUILDING
))

// Update Participants (Step 2)
stateMachine.dispatch(Intent.UpdateDraftEvent(
    event = event.copy(
        minParticipants = 5,
        maxParticipants = 20,
        expectedParticipants = 12
    )
))

// Add Location (Step 3)
stateMachine.dispatch(Intent.AddPotentialLocation(
    location = PotentialLocation(...)
))

// Add Time Slot (Step 4)
stateMachine.dispatch(Intent.AddTimeSlot(
    timeSlot = TimeSlot(...)
))

// Complete Draft (Step 4 → Poll)
stateMachine.dispatch(Intent.StartPoll(eventId))
```

### Monitoring State Changes

Observe state changes for progress tracking:

**Android:**
```kotlin
LaunchedEffect(Unit) {
    viewModel.eventState.collect { state ->
        when (state.wizardStep) {
            DraftStep.BasicInfo -> println("Step 1: Basic Info")
            DraftStep.Participants -> println("Step 2: Participants")
            DraftStep.Locations -> println("Step 3: Locations")
            DraftStep.TimeSlots -> println("Step 4: Time Slots")
        }
    }
}
```

**iOS:**
```swift
@ObservedRealmObject var state: EventManagementState

var body: some View {
    VStack {
        ProgressBar(current: state.wizardStep.rawValue, total: 4)
        DraftEventWizardView(...)
    }
}
```

## Validation

### Validation Rules by Step

#### Step 1: Basic Info (Required)
```
- title: non-empty, trimmed (required)
- description: non-empty, trimmed (required)
- eventType: enum value (optional, default: OTHER)
- eventTypeCustom: non-empty if eventType == CUSTOM (conditional)
```

#### Step 2: Participants (Optional)
```
- minParticipants: positive integer (optional)
- maxParticipants: positive integer (optional)
- expectedParticipants: positive integer (optional)
- Constraint: max >= min (if both provided)
```

#### Step 3: Locations (Optional)
```
- At least 1 location should be added for better UX
- Each location must have a valid LocationType
- Can contain empty lists
```

#### Step 4: Time Slots (Required)
```
- At least 1 time slot must be provided
- Each time slot must have valid dates/times
- timeOfDay must be a valid TimeOfDay enum
```

### Custom Validation

Implement custom validators:

**Android:**
```kotlin
val customValidators = listOf(
    Validator { event ->
        if (event.title.length < 3) {
            ValidationError("title", "Le titre doit avoir au moins 3 caractères")
        } else null
    }
)

DraftEventWizard(
    stateMachine = stateMachine,
    customValidators = customValidators
)
```

## Best Practices

### 1. Auto-Save Data

The wizard automatically saves data at each step transition. Ensure:
- The repository implementation handles concurrent writes
- Use optimistic updates in the UI
- Provide feedback to the user about save status

### 2. Handle Network Errors

```kotlin
is SideEffect.ShowError -> {
    when (it.error) {
        is NetworkError -> retryLastAction()
        is ValidationError -> focusInvalidField()
        is ConflictError -> showConflictResolutionDialog()
        else -> showGenericError()
    }
}
```

### 3. Offline Support

The wizard works offline - data is persisted locally:
```kotlin
// Data is saved to SQLite immediately
stateMachine.dispatch(Intent.UpdateDraftEvent(event))

// On reconnection, sync with backend
viewModel.syncOfflineChanges()
```

### 4. Progress Indication

Show clear progress to the user:

**Android:**
```kotlin
LinearProgressIndicator(
    progress = (currentStep.ordinal + 1) / 4f,
    modifier = Modifier.fillMaxWidth()
)
```

**iOS:**
```swift
ProgressView(value: Double(currentStep.rawValue) / 4.0)
    .tint(.blue)
```

### 5. Data Recovery

If the user closes the wizard without completing:
- Data is saved locally
- Next time, show "Continue" button with existing data
- Allow the user to discard and start fresh

```kotlin
LaunchedEffect(Unit) {
    val existingEvent = eventRepository.getEvent(eventId)
    if (existingEvent != null && existingEvent.status == EventStatus.DRAFT) {
        showContinueDialog(existingEvent)
    }
}
```

## Troubleshooting

### Issue: Validation Not Triggering

**Symptom:** User can proceed with invalid data

**Solution:** Ensure you're dispatching the correct intent:
```kotlin
// ✅ Correct - uses UpdateDraftEvent for validation
stateMachine.dispatch(Intent.UpdateDraftEvent(event))

// ❌ Wrong - no validation
repository.saveEvent(event)
```

### Issue: State Machine Not Updating UI

**Symptom:** UI doesn't reflect state changes

**Solution:** Ensure you're collecting the state:
```kotlin
// ✅ Correct
LaunchedEffect(Unit) {
    viewModel.eventState.collect { state ->
        // UI updates here
    }
}

// ❌ Wrong - single snapshot, doesn't update
val state = viewModel.eventState.value
```

### Issue: Auto-Save Not Working

**Symptom:** Data is lost when app closes

**Solution:** Check repository implementation:
```kotlin
// Ensure repository persists to SQLite
override suspend fun updateEvent(event: Event) {
    database.eventDao().insert(event.toEntity())
}
```

### Issue: Performance Lag on Step Transition

**Symptom:** UI freezes when moving between steps

**Solution:** Offload validation to background:
```kotlin
// ✅ Correct - validation on IO dispatcher
viewModel.validateStep(step)  // runs on IO

// ❌ Wrong - blocks UI thread
val isValid = event.isValid()  // runs on Main
```

## Examples

### Complete Event Creation Flow

```kotlin
@Composable
fun EventCreationFlow() {
    val viewModel: EventCreationViewModel = viewModel()
    var createdEventId by remember { mutableStateOf<String?>(null) }
    
    if (createdEventId != null) {
        // Navigate to event detail
        EventDetailScreen(eventId = createdEventId!!)
    } else {
        DraftEventWizard(
            stateMachine = viewModel.stateMachine,
            onEventCreated = { eventId ->
                createdEventId = eventId
            },
            onCancel = {
                // Handle cancellation
            }
        )
    }
    
    // Observe and handle side effects
    LaunchedEffect(Unit) {
        viewModel.sideEffects.collect { effect ->
            when (effect) {
                is SideEffect.ShowError -> {
                    // Show error UI
                }
                else -> {}
            }
        }
    }
}
```

### Edit Existing Event

```kotlin
@Composable
fun EditEventWizard(eventId: String) {
    val viewModel: EventCreationViewModel = viewModel()
    
    LaunchedEffect(eventId) {
        // Load existing event into wizard
        viewModel.loadEvent(eventId)
    }
    
    DraftEventWizard(
        stateMachine = viewModel.stateMachine,
        initialEvent = viewModel.currentEvent,
        onEventCreated = { /* Handle update */ }
    )
}
```

## See Also

- [State Machine Integration Guide](./STATE_MACHINE_INTEGRATION_GUIDE.md)
- [Event Organization Specification](../specs/event-organization/)
- [Workflow Coordination Specification](../specs/workflow-coordination/)
