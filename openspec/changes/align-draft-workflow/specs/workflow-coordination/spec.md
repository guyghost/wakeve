- If both `min` and `max` provided: `max >= min` (required)
- All fields MUST be left empty (optional)

**Auto-save Behavior:**
- MUST dispatch `UpdateDraftEvent` intent when leaving Step 2
- MUST persist minParticipants, maxParticipants, expectedParticipants

**State Machine Integration:**
```kotlin
// Intent dispatched on step transition
EventManagementContract.Intent.UpdateDraftEvent(
    event = Event(
        id = eventId,
        minParticipants = minParticipants,
        maxParticipants = maxParticipants,
        expectedParticipants = expectedParticipants
    )
)
```

#### Scenario: Organizer provides participant estimation
- **GIVEN** Organizer is on Step 2
- **WHEN** They enter min=15, max=25, expected=20
- **THEN** Validation passes (max >= min), auto-save occurs, and "Next" button becomes enabled.

#### Scenario: Validation fails for invalid participant counts
- **GIVEN** Organizer enters min=30, max=20
- **WHEN** They attempt to proceed
- **THEN** Validation error "max must be >= min" is shown, navigation is blocked.

### Requirement: The system SHALL allow organizers to propose potential locations
**ID**: `workflow-draft-103`

The system SHALL allow organizers to propose potential locations.

**Validation Rules:**
- Locations list MUST be empty (optional)
- Each location MUST have: `name` (required), `locationType` (required)
- `address` and `coordinates` MUST be nullable (optional)

**Auto-save Behavior:**
- MUST dispatch `AddPotentialLocation` intent when adding a location
- MUST dispatch `RemovePotentialLocation` intent when removing a location
- Auto-save MUST occur on add/remove operations

**State Machine Integration:**
```kotlin
// Intent dispatched when adding location
EventManagementContract.Intent.AddPotentialLocation(
    eventId = eventId,
    location = PotentialLocation(
        id = UUID.randomUUID().toString(),
        eventId = eventId,
        name = "Paris",
        locationType = LocationType.CITY,
        address = null,
        coordinates = null
    )
)
```

#### Scenario: Organizer adds multiple potential locations
- **GIVEN** Organizer is on Step 3
- **WHEN** They add "Paris" (CITY) and "Lyon" (CITY)
- **THEN** Both locations are added, auto-save occurs, and they appear in list.

#### Scenario: Organizer removes a location
- **GIVEN** Organizer has 2 locations on Step 3
- **WHEN** They delete "Lyon"
- **THEN** Location is removed, auto-save occurs, and only "Paris" remains.

### Requirement: The system SHALL allow organizers to propose time slots with flexible time-of-day
**ID**: `workflow-draft-104`

The system SHALL allow organizers to propose time slots with flexible time-of-day.

**Validation Rules:**
- At least 1 TimeSlot MUST be added (required)
- Each TimeSlot MUST have: `date` (required), `timeOfDay` (required)
- If `timeOfDay = SPECIFIC`, `start` and `end` MUST be provided (required)
- If `timeOfDay != SPECIFIC`, `start` and `end` MUST be nullable (optional)

**Auto-save Behavior:**
- MUST dispatch `AddTimeSlot` intent when adding a slot
- MUST dispatch `RemoveTimeSlot` intent when removing a slot

**State Machine Integration:**
```kotlin
// Intent dispatched when adding time slot
EventManagementContract.Intent.AddTimeSlot(
    eventId = eventId,
    slot = TimeSlot(
        id = UUID.randomUUID().toString(),
        start = if (timeOfDay == SPECIFIC) specificStart else null,
        end = if (timeOfDay == SPECIFIC) specificEnd else null,
        timezone = "UTC",
        timeOfDay = timeOfDay
    )
)
```

#### Scenario: Organizer adds flexible time slot
- **GIVEN** Organizer is on Step 4
- **WHEN** They add slot: date="2025-06-15", timeOfDay=AFTERNOON
- **THEN** Slot is added with timeOfDay=AFTERNOON, auto-save occurs, "Create Event" button becomes enabled.

#### Scenario: Organizer adds specific time slot
- **GIVEN** Organizer is on Step 4
- **WHEN** They add slot: date="2025-06-15", timeOfDay=SPECIFIC, start="14:00", end="16:00"
- **THEN** Slot is added with exact times, auto-save occurs.

### Requirement: The system SHALL create event and transition to poll setup after wizard completion
**ID**: `workflow-draft-005`

The system SHALL create event and transition to poll setup after wizard completion.

**Business Rules:**
- All 4 steps MUST be completed
- All validations MUST pass
- Event MUST be created with status=DRAFT
- User MUST be navigated to poll setup (StartPoll flow)

**Side Effects:**
- MUST dispatch `CreateEvent` intent
- MUST emit `NavigateTo("event-detail/{eventId}")` side effect
- MAY show `ShowToast("Event created successfully")`

**State Machine Integration:**
```kotlin
// Final intent dispatched on wizard completion
EventManagementContract.Intent.CreateEvent(
    event = Event(
        id = eventId,
        title = title,
        description = description,
        organizerId = currentUserId,
        eventType = eventType,
        minParticipants = minParticipants,
        maxParticipants = maxParticipants,
        expectedParticipants = expectedParticipants,
        proposedSlots = timeSlots,
        potentialLocations = locations,
        deadline = calculateDefaultDeadline(),
        status = EventStatus.DRAFT
    )
)

// Side effect for navigation
EventManagementContract.SideEffect.NavigateTo(
    route = "event-detail/${eventId}"
)
```

#### Scenario: Organizer completes wizard and starts poll
- **GIVEN** Organizer completed all 4 steps
- **WHEN** They click "Create Event"
- **THEN** Event is created, saved to repository, and navigates to event detail where they can StartPoll.

## State Machine Orchestration

### Intent Mapping

| UI Action | State Machine Intent | Side Effect |
|-----------|---------------------|-------------|
| Step 1: Fill & Next | `UpdateDraftEvent` | Auto-save |
| Step 2: Fill & Next | `UpdateDraftEvent` | Auto-save |
| Step 3: Add Location | `AddPotentialLocation` | Auto-save |
| Step 3: Remove Location | `RemovePotentialLocation` | Auto-save |
| Step 4: Add Slot | `AddTimeSlot` | Auto-save |
| Step 4: Remove Slot | `RemoveTimeSlot` | Auto-save |
| Complete Wizard | `CreateEvent` | NavigateTo("detail/{id}") |
| Cancel Wizard | - | NavigateBack |

### Navigation Flow

```
[EventListScreen]
       ↓ [User taps "Create Event"]
[DraftEventWizard - Step 1: Basic Info]
       ↓ [User fills fields + "Next"]
[UpdateDraftEvent intent] → [Auto-save]
       ↓
[DraftEventWizard - Step 2: Participants]
       ↓ [User fills fields + "Next"]
[UpdateDraftEvent intent] → [Auto-save]
       ↓
[DraftEventWizard - Step 3: Locations]
       ↓ [User adds locations + "Next"]
[AddPotentialLocation intents] → [Auto-save]
       ↓
[DraftEventWizard - Step 4: Time Slots]
       ↓ [User adds slots + "Create Event"]
[AddTimeSlot intents] → [Auto-save]
       ↓
[CreateEvent intent] → [NavigateTo("detail/{id}")]
       ↓
[EventDetailScreen]
       ↓ [User taps "Start Poll"]
[StartPoll intent] → [Event status: POLLING]
       ↓
[PollSetupScreen]
```

## Platform-Specific Implementation

### Android (Jetpack Compose)

**Component**: `DraftEventWizard.kt`
- Uses `TabView` or `AnimatedContent` for step transitions
- Material You design with progress indicator
- Validation feedback via `OutlinedTextField isError`

**State Management**:
```kotlin
@Composable
fun DraftEventWizard(
    initialEvent: Event?,
    onSaveStep: (Event) -> Unit,
    onComplete: (Event) -> Unit,
    onCancel: () -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }
    var title by remember { mutableStateOf(initialEvent?.title ?: "") }
    // ... other state

    Scaffold {
        // Step content
        when (currentStep) {
            0 -> Step1BasicInfo(...)
            1 -> Step2Participants(...)
            2 -> Step3Locations(...)
            3 -> Step4TimeSlots(...)
        }
    }
}
```

### iOS (SwiftUI)

**Component**: `DraftEventWizardView.swift`
- Uses `TabView` with `.page` style for swipeable steps
- Liquid Glass design with smooth animations
- Validation feedback via alerts or inline errors

**State Management**:
```swift
struct DraftEventWizardView: View {
    @State private var currentStep: Int = 0
    @State private var title: String = ""
    // ... other state

    var body: some View {
        NavigationView {
            VStack {
                ProgressView(value: Double(currentStep + 1), total: 4.0)

                TabView(selection: $currentStep) {
                    step1BasicInfo.tag(0)
                    step2Participants.tag(1)
                    step3Locations.tag(2)
                    step4TimeSlots.tag(3)
                }
                .tabViewStyle(.page(indexDisplayMode: .never))
            }
        }
    }
}
```

## Testing Requirements

### Unit Tests
- Validation logic for each step
- Auto-save intent dispatch verification
- Error state handling

### Integration Tests
- Complete wizard flow (Steps 1-4)
- Auto-save persistence verification
- Navigation flow validation
- Edge cases (empty optional fields, validation errors)

### E2E Tests
- Create event → StartPoll → Vote on Android
- Create event → StartPoll → Vote on iOS
- Offline scenario (create DRAFT offline, sync online)

## Migration Strategy

### Deprecation Notice

**EventCreationScreen.kt** (Android) MUST be deprecated and WILL be removed in a future major version.

**Migration Guide**:
```kotlin
// OLD (deprecated)
EventCreationScreen(
    userId = userId,
    onEventCreated = { event -> ... },
    onBack = { ... }
)

// NEW (use DraftEventWizard)
DraftEventWizard(
    initialEvent = null,
    onSaveStep = { event -> viewModel.dispatch(Intent.UpdateDraftEvent(event)) },
    onComplete = { event -> viewModel.dispatch(Intent.CreateEvent(event)) },
    onCancel = { navController.popBackStack() }
)
```

### Timeline
- **Current version**: EventCreationScreen marked @Deprecated
- **Next minor version**: Warning logged when EventCreationScreen is used
- **Next major version**: EventCreationScreen removed

## Dependencies

- **Downstream**: `event-organization` (uses Event model), `workflow-coordination` (defines state machine pattern)
- **Upstream**: `Enhanced DRAFT Phase` (provides enhanced Event fields)

## Acceptance Criteria

- ✅ DRAFT wizard MUST have 4 sequential steps
- ✅ Auto-save MUST occur at each step transition
- ✅ Validation MUST prevent invalid transitions
- ✅ Required fields MUST be enforced (title, description, time slots)
- ✅ Optional fields MUST be skippable (participants, locations)
- ✅ State Machine MUST orchestrate all intents
- ✅ Navigation MUST be consistent across platforms
- ✅ EventCreationScreen MUST be deprecated with migration guide

## Success Metrics

- 100% of new events MUST be created via DraftEventWizard
- 0 crashes related to DRAFT workflow
- < 3 minutes average time to complete wizard
- 95% auto-save success rate
