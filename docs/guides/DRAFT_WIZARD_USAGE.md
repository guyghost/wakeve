# DRAFT Wizard Usage Guide

> **Version**: 1.0.0
> **Last Updated**: 2026-01-04
> **Platforms**: Android (Jetpack Compose), iOS (SwiftUI)

## Overview

The DRAFT wizard is a multi-step interface for creating events in DRAFT status. It guides organizers through event creation with validation at each step and automatic persistence (auto-save).

### Key Features

- **4 Sequential Steps**: Basic Info → Participants → Locations → Time Slots
- **Auto-Save**: Event data is automatically persisted at each step transition
- **Strict Validation**: Navigation is blocked until current step is valid
- **Offline-First**: Works seamlessly without network connection
- **Cross-Platform**: Consistent UX between Android (Material You) and iOS (Liquid Glass)

## Wizard Steps

### Step 1: Basic Info
**Required Fields:**
- `title` (string, required)
- `description` (string, required)

**Optional Fields:**
- `eventType` (enum, optional, default: `OTHER`)
  - Presets: `BIRTHDAY`, `WEDDING`, `TEAM_BUILDING`, `CONFERENCE`, `WORKSHOP`, `PARTY`, `SPORTS_EVENT`, `CULTURAL_EVENT`, `FAMILY_GATHERING`
  - Custom: `CUSTOM` with `eventTypeCustom` text field

**Validation:**
- Title must be non-empty and trimmed
- Description must be non-empty and trimmed
- If `eventType = CUSTOM`, `eventTypeCustom` must be non-empty

### Step 2: Participants Estimation
**Optional Fields:**
- `minParticipants` (integer, optional)
- `maxParticipants` (integer, optional)
- `expectedParticipants` (integer, optional)

**Validation:**
- If provided, values must be positive (> 0)
- If both `min` and `max` provided: `max >= min`
- All fields can be left empty

### Step 3: Potential Locations
**Optional:**
- List of `PotentialLocation` (can be empty)

**Each Location:**
- `name` (string, required)
- `locationType` (enum, required): `CITY`, `REGION`, `SPECIFIC_VENUE`, `ONLINE`
- `address` (string, optional)
- `coordinates` (optional)

**Validation:**
- Locations list can be empty (optional)
- Each location must have name and locationType

### Step 4: Time Slots
**Required:**
- At least 1 `TimeSlot`

**Each TimeSlot:**
- `date` (string, required)
- `timeOfDay` (enum, required): `ALL_DAY`, `MORNING`, `AFTERNOON`, `EVENING`, `SPECIFIC`
- `start` (string, optional, required if `timeOfDay = SPECIFIC`)
- `end` (string, optional, required if `timeOfDay = SPECIFIC`)

**Validation:**
- At least 1 TimeSlot must be added
- If `timeOfDay = SPECIFIC`, both `start` and `end` must be provided

## Android (Jetpack Compose)

### Basic Usage

```kotlin
@Composable
fun CreateEventScreen(
    viewModel: EventManagementViewModel = koinViewModel(),
    navController: NavController
) {
    DraftEventWizard(
        initialEvent = null, // Set to existing event for editing
        onSaveStep = { event ->
            // Auto-save event at each step transition
            viewModel.dispatch(Intent.UpdateDraftEvent(event))
        },
        onComplete = { event ->
            // Create event when wizard is complete
            viewModel.dispatch(Intent.CreateEvent(event))
            // Navigate to event detail
            navController.navigate("event-detail/${event.id}")
        },
        onCancel = {
            // Cancel wizard
            navController.popBackStack()
        }
    )
}
```

### Editing Mode

```kotlin
@Composable
fun EditEventScreen(
    eventId: String,
    viewModel: EventManagementViewModel = koinViewModel(),
    navController: NavController
) {
    val event by viewModel.state.collectAsState().map { it.selectedEvent }

    LaunchedEffect(eventId) {
        viewModel.dispatch(Intent.SelectEvent(eventId))
    }

    event?.let { draftEvent ->
        DraftEventWizard(
            initialEvent = draftEvent, // Pass existing event
            onSaveStep = { event ->
                viewModel.dispatch(Intent.UpdateDraftEvent(event))
            },
            onComplete = { event ->
                viewModel.dispatch(Intent.UpdateEvent(event))
                navController.navigate("event-detail/${event.id}")
            },
            onCancel = {
                navController.popBackStack()
            }
        )
    }
}
```

### Customization

You can customize wizard behavior through the `DraftEventWizard` parameters:

| Parameter | Type | Description |
|-----------|-------|-------------|
| `initialEvent` | `Event?` | Pass existing event for editing mode, or `null` for creation |
| `onSaveStep` | `(Event) -> Unit` | Callback when user navigates to next step (auto-save) |
| `onComplete` | `(Event) -> Unit` | Callback when wizard is complete (create event) |
| `onCancel` | `() -> Unit` | Callback when user cancels wizard |
| `modifier` | `Modifier` | Modifier for the wizard root composable |

### Material You Design

The wizard uses Material You components:
- `OutlinedTextField` for input fields
- `DropdownMenu` for event type selection
- `Card` for locations and time slots display
- `LinearProgressIndicator` for step progress
- `Button` for navigation actions
- `AlertDialog` for error feedback

## iOS (SwiftUI)

### Basic Usage

```swift
import SwiftUI
import Shared

struct CreateEventView: View {
    @StateObject var viewModel = EventListViewModel()
    @Environment(\.dismiss) var dismiss
    
    var body: some View {
        DraftEventWizardView(
            initialEvent: nil, // Set to existing event for editing
            onSaveStep: { event in
                // Auto-save event at each step transition
                viewModel.dispatch(.updateDraftEvent(event))
            },
            onComplete: { event in
                // Create event when wizard is complete
                viewModel.dispatch(.createEvent(event))
                dismiss()
            },
            onCancel: {
                // Cancel wizard
                dismiss()
            }
        )
    }
}
```

### Editing Mode

```swift
import SwiftUI
import Shared

struct EditEventView: View {
    let eventId: String
    @StateObject var viewModel = EventListViewModel()
    @Environment(\.dismiss) var dismiss
    
    var body: some View {
        DraftEventWizardView(
            initialEvent: viewModel.state.selectedEvent,
            onSaveStep: { event in
                viewModel.dispatch(.updateDraftEvent(event))
            },
            onComplete: { event in
                viewModel.dispatch(.updateEvent(event))
                dismiss()
            },
            onCancel: {
                dismiss()
            }
        )
        .task {
            await viewModel.dispatch(.selectEvent(eventId: eventId))
        }
    }
}
```

### Liquid Glass Design

The wizard uses Liquid Glass components:
- `TextField` with glassmorphism style
- `Picker` for event type selection
- `Card` with blur effects for locations and time slots
- `ProgressView` for step progress
- `Button` with gradient backgrounds for navigation actions
- `Alert` for error feedback

## State Machine Integration

The wizard dispatches the following `EventManagementContract.Intent` types:

| Wizard Action | Intent | Side Effect |
|---------------|---------|-------------|
| Step 1: Fill & Next | `UpdateDraftEvent` | Auto-save |
| Step 2: Fill & Next | `UpdateDraftEvent` | Auto-save |
| Step 3: Add Location | `AddPotentialLocation` | Auto-save |
| Step 3: Remove Location | `RemovePotentialLocation` | Auto-save |
| Step 4: Add Slot | `AddTimeSlot` | Auto-save |
| Step 4: Remove Slot | `RemoveTimeSlot` | Auto-save |
| Complete Wizard | `CreateEvent` | `NavigateTo("detail/{id}")` |
| Cancel Wizard | - | `NavigateBack` |

## Validation

### Android

Validation errors are displayed inline:

```kotlin
OutlinedTextField(
    value = state.title,
    onValueChange = { state = state.copy(title = it) },
    label = { Text("Title *") },
    isError = titleError,
    supportingText = if (titleError) { { Text("Title is required") } } else null
)
```

### iOS

Validation errors are displayed via `Alert`:

```swift
if isStepValid(currentStep) {
    Button("Next") {
        onSaveStep(buildEvent())
        withAnimation {
            currentStep += 1
        }
    }
} else {
    Button("Next") {
        showingValidationError = true
    }
    .disabled(true)
}

.alert("Validation Error", isPresented: $showingValidationError) {
    Button("OK") { }
} message: {
    Text("Please fix the errors before proceeding.")
}
```

## Best Practices

### 1. Auto-Save on Every Step

Always dispatch `UpdateDraftEvent` in the `onSaveStep` callback to ensure data persistence:

```kotlin
onSaveStep = { event ->
    viewModel.dispatch(Intent.UpdateDraftEvent(event))
}
```

### 2. Handle Auto-Save Errors

Auto-save errors should be handled gracefully (e.g., show toast):

```kotlin
LaunchedEffect(Unit) {
    viewModel.sideEffect.collect { effect ->
        when (effect) {
            is EventManagementContract.SideEffect.ShowError -> {
                Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
            }
            else -> {}
        }
    }
}
```

### 3. Support Editing Mode

Always pass `initialEvent` to support editing existing events:

```kotlin
val isEditing = initialEvent != null
DraftEventWizard(
    initialEvent = if (isEditing) initialEvent else null,
    // ...
)
```

### 4. Listen to Side Effects

Listen to `sideEffect` for navigation and feedback:

**Android:**
```kotlin
LaunchedEffect(Unit) {
    viewModel.sideEffect.collect { effect ->
        when (effect) {
            is EventManagementContract.SideEffect.NavigateTo -> {
                navController.navigate(effect.route)
            }
            is EventManagementContract.SideEffect.ShowToast -> {
                Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
            }
            is EventManagementContract.SideEffect.ShowError -> {
                Snackbar(snackbarHostState, effect.message).show()
            }
            else -> {}
        }
    }
}
```

**iOS:**
```swift
.onReceive(viewModel.$sideEffect) { effect in
    switch effect {
    case .navigateTo(let route):
        // Navigate using NavigationLink or programmatic navigation
        destination = route
    case .showToast(let message):
        // Show toast using UIHostingController or custom view
        showToast(message)
    case .showError(let message):
        // Show error alert
        errorMessage = message
        showingError = true
    default:
        break
    }
}
```

## Troubleshooting

### Issue: Auto-save not working

**Cause**: Not dispatching `UpdateDraftEvent` in `onSaveStep`.

**Solution**: Always call `viewModel.dispatch(Intent.UpdateDraftEvent(event))` in `onSaveStep`.

### Issue: Navigation blocked after filling fields

**Cause**: Validation error not displayed.

**Solution**: Check validation rules and ensure inline errors are shown. Enable debug logging to see validation errors.

### Issue: Event not created after wizard completion

**Cause**: `CreateEvent` intent not dispatched or failed.

**Solution**: Listen to `sideEffect` for `NavigateTo` or `ShowError` to detect success/failure.

### Issue: Edited event not updating

**Cause**: Using `CreateEvent` instead of `UpdateEvent`.

**Solution**: Check if `initialEvent != null` and use appropriate intent:
```kotlin
if (initialEvent != null) {
    viewModel.dispatch(Intent.UpdateEvent(event))
} else {
    viewModel.dispatch(Intent.CreateEvent(event))
}
```

## Related Documentation

- [State Machine Integration Guide](./STATE_MACHINE_INTEGRATION_GUIDE.md)
- [Event Organization Spec](../../openspec/specs/event-organization/spec.md)
- [Workflow Coordination Spec](../../openspec/specs/workflow-coordination/spec.md)
- [AGENTS.md](../../AGENTS.md)
