# Design: Enhanced DRAFT Phase

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                         UI Layer                                │
│  ┌──────────────────────┐        ┌──────────────────────┐      │
│  │  CreateEventScreen   │        │  CreateEventView     │      │
│  │  (Android Compose)   │        │  (iOS SwiftUI)       │      │
│  │                      │        │                      │      │
│  │  - EventTypeSelector │        │  - EventTypePicker   │      │
│  │  - ParticipantsCard  │        │  - ParticipantsCard  │      │
│  │  - LocationsList     │        │  - LocationsList     │      │
│  │  - TimeSlotPicker    │        │  - TimeSlotPicker    │      │
│  └──────────┬───────────┘        └──────────┬───────────┘      │
│             │                               │                  │
│             └───────────────┬───────────────┘                  │
│                             ▼                                  │
├─────────────────────────────────────────────────────────────────┤
│                    State Machine Layer                          │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │          EventManagementStateMachine                    │   │
│  │                                                         │   │
│  │  Intent.CreateEvent(event) with new fields             │   │
│  │  Intent.UpdateDraftEvent(event)                        │   │
│  │  Intent.AddPotentialLocation(eventId, location)        │   │
│  │  Intent.RemovePotentialLocation(eventId, locationId)   │   │
│  └───────────────────────┬─────────────────────────────────┘   │
│                          ▼                                      │
├─────────────────────────────────────────────────────────────────┤
│                    Repository Layer                             │
│  ┌──────────────────────┐      ┌──────────────────────────┐    │
│  │  EventRepository     │      │ PotentialLocationRepo    │    │
│  │  - create()          │      │ - add()                  │    │
│  │  - update()          │      │ - remove()               │    │
│  │  - validate()        │      │ - getByEventId()         │    │
│  └──────────┬───────────┘      └──────────┬───────────────┘    │
│             │                              │                    │
│             └──────────────┬───────────────┘                    │
│                            ▼                                    │
├─────────────────────────────────────────────────────────────────┤
│                    Persistence Layer                            │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              SQLDelight (SQLite)                        │   │
│  │                                                         │   │
│  │  - event (modified: +eventType, +participants counts)  │   │
│  │  - potential_location (new table)                      │   │
│  │  - time_slot (modified: +timeOfDay)                    │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

## Data Flow: Creating a DRAFT Event

### Happy Path

```
1. User opens CreateEventScreen
   ↓
2. UI shows wizard Step 1 (Title, Description, EventType)
   ↓
3. User fills basic info → auto-save to local state
   ↓
4. User proceeds to Step 2 (Participants estimation)
   ↓
5. User fills minParticipants=15, maxParticipants=25, expected=20
   → Validation: max >= min ✅
   ↓
6. User proceeds to Step 3 (Potential locations)
   ↓
7. User taps "Add Location" → LocationInputDialog opens
   ↓
8. User enters "Paris" (CITY) → Intent.AddPotentialLocation
   ↓
9. StateMachine → Repository → SQLDelight
   ↓
10. PotentialLocation saved, UI refreshed
    ↓
11. User repeats 7-10 for "Lyon" and "Berlin"
    ↓
12. User proceeds to Step 4 (Time slots)
    ↓
13. User adds 3 time slots:
    - June 15, AFTERNOON (flexible)
    - June 22, 14:00-18:00 (specific)
    - June 29, ALL_DAY
    ↓
14. User taps "Create Event" → Intent.CreateEvent
    ↓
15. StateMachine validates all fields
    ↓
16. EventRepository.create(event) → SQLDelight
    ↓
17. Event saved with status = DRAFT
    ↓
18. Side effect: NavigateTo("event/{eventId}")
    ↓
19. User sees event detail screen
```

### Error Handling

```
Scenario: maxParticipants < minParticipants
   ↓
Validation fails in StateMachine
   ↓
SideEffect.ShowError("Max must be >= min")
   ↓
UI displays inline error message
   ↓
Field highlighted in red
   ↓
User corrects values
   ↓
Validation passes, continue
```

## Design Decisions

### 1. Wizard vs Single Form

**Decision**: Multi-step wizard (4 steps)

**Rationale**:
- Reduces cognitive load (focus on one topic at a time)
- Allows progressive disclosure (show advanced options only when needed)
- Mobile-friendly (less scrolling, clearer CTAs)
- Auto-save at each step prevents data loss

**Implementation**:
- Android: `HorizontalPager` (Compose)
- iOS: `TabView` with `.tabViewStyle(.page)` (SwiftUI)

### 2. EventType: Enum vs String

**Decision**: Enum with CUSTOM fallback

**Rationale**:
- Type safety in code
- Easy to add new presets without breaking changes
- Enables automatic suggestions (Phase 3)
- CUSTOM allows flexibility

**Alternative rejected**: Free text only → no categorization for agents

### 3. PotentialLocation Storage

**Decision**: Separate table with foreign key to Event

**Rationale**:
- Relational integrity (CASCADE DELETE)
- Easy to query locations by eventId
- Supports future features (voting, cost estimation per location)
- Cleaner than JSON blob in Event table

**Schema**:
```sql
CREATE TABLE potential_location (
    id TEXT PRIMARY KEY,
    eventId TEXT NOT NULL,
    name TEXT NOT NULL,
    locationType TEXT NOT NULL,
    address TEXT,
    coordinates TEXT, -- JSON
    createdAt TEXT NOT NULL,
    FOREIGN KEY (eventId) REFERENCES event(id) ON DELETE CASCADE
);
```

### 4. TimeOfDay: Separate Field vs Nullable Start/End

**Decision**: Add `timeOfDay` enum field, keep `start/end` nullable

**Rationale**:
- Explicit intent (ALL_DAY vs SPECIFIC)
- Backward compatible (existing slots → SPECIFIC)
- Simplifies UI (show time picker only if SPECIFIC)
- Supports flexible scheduling ("sometime in the afternoon")

**Enum**:
```kotlin
enum class TimeOfDay {
    ALL_DAY,      // 00:00 - 23:59
    MORNING,      // ~6h-12h
    AFTERNOON,    // ~12h-18h
    EVENING,      // ~18h-23h
    SPECIFIC      // Exact start/end times
}
```

### 5. Auto-save Strategy

**Decision**: Save on step change + debounced save on text input

**Rationale**:
- Prevents data loss (app crash, navigation away)
- Better UX (no explicit "Save Draft" button)
- Consistent with modern mobile apps (Google Docs, Notion)

**Implementation**:
```kotlin
// Debounced auto-save (500ms after last edit)
LaunchedEffect(eventDraftState) {
    snapshotFlow { eventDraftState }
        .debounce(500)
        .collect { draft ->
            viewModel.dispatch(Intent.UpdateDraftEvent(draft))
        }
}
```

### 6. Validation: Client vs Server

**Decision**: Client-side validation for UX, server-side for security

**Client validation** (immediate feedback):
- maxParticipants >= minParticipants
- Required fields (title, description)
- EventType = CUSTOM → eventTypeCustom required

**Server validation** (security):
- Same rules as client
- SQL injection prevention
- Data sanitization

## UI/UX Design

### Android (Material You)

#### EventTypeSelector
```kotlin
@Composable
fun EventTypeSelector(
    selectedType: EventType,
    customText: String,
    onTypeSelected: (EventType) -> Unit,
    onCustomTextChanged: (String) -> Unit
) {
    ExposedDropdownMenuBox(...) {
        // Dropdown with presets
        EventType.values().forEach { type ->
            DropdownMenuItem(
                text = { Text(type.displayName) },
                onClick = { onTypeSelected(type) }
            )
        }
    }
    
    // Show TextField if CUSTOM selected
    AnimatedVisibility(selectedType == EventType.CUSTOM) {
        OutlinedTextField(
            value = customText,
            onValueChange = onCustomTextChanged,
            label = { Text("Custom event type") }
        )
    }
}
```

#### ParticipantsEstimationCard
```kotlin
@Composable
fun ParticipantsEstimationCard(
    min: Int?,
    max: Int?,
    expected: Int?,
    onMinChanged: (Int?) -> Unit,
    onMaxChanged: (Int?) -> Unit,
    onExpectedChanged: (Int?) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("How many participants?", style = MaterialTheme.typography.titleMedium)
            Spacer(8.dp)
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(
                    value = min,
                    label = "Min",
                    modifier = Modifier.weight(1f),
                    onValueChange = onMinChanged
                )
                NumberField(
                    value = max,
                    label = "Max",
                    modifier = Modifier.weight(1f),
                    onValueChange = onMaxChanged
                )
                NumberField(
                    value = expected,
                    label = "Expected",
                    modifier = Modifier.weight(1f),
                    onValueChange = onExpectedChanged
                )
            }
            
            // Validation error
            if (max != null && min != null && max < min) {
                Text(
                    "Maximum must be ≥ minimum",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
```

### iOS (Liquid Glass)

#### EventTypePicker
```swift
struct EventTypePicker: View {
    @Binding var selectedType: EventType
    @Binding var customText: String
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Event Type")
                .font(.headline)
            
            Picker("Type", selection: $selectedType) {
                ForEach(EventType.allCases, id: \.self) { type in
                    Text(type.displayName).tag(type)
                }
            }
            .pickerStyle(.menu)
            .liquidGlassCard() // Custom modifier
            
            if selectedType == .custom {
                TextField("Custom event type", text: $customText)
                    .textFieldStyle(.roundedBorder)
                    .transition(.opacity)
            }
        }
    }
}
```

#### ParticipantsEstimationCard
```swift
struct ParticipantsEstimationCard: View {
    @Binding var min: Int?
    @Binding var max: Int?
    @Binding var expected: Int?
    
    var isValid: Bool {
        guard let min = min, let max = max else { return true }
        return max >= min
    }
    
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("How many participants?")
                .font(.headline)
            
            HStack(spacing: 12) {
                NumberTextField("Min", value: $min)
                NumberTextField("Max", value: $max)
                NumberTextField("Expected", value: $expected)
            }
            
            if !isValid {
                Text("Maximum must be ≥ minimum")
                    .font(.caption)
                    .foregroundColor(.red)
            }
        }
        .padding()
        .liquidGlassCard()
    }
}
```

## Testing Strategy

### Unit Tests (Shared - Kotlin)

```kotlin
class EventValidationTest {
    @Test
    fun `maxParticipants must be greater than or equal to minParticipants`() {
        val event = Event(
            // ... other fields
            minParticipants = 20,
            maxParticipants = 10 // Invalid
        )
        
        val result = EventValidator.validate(event)
        
        assertFalse(result.isValid)
        assertTrue(result.errors.contains("maxParticipants must be >= minParticipants"))
    }
    
    @Test
    fun `eventType CUSTOM requires eventTypeCustom`() {
        val event = Event(
            // ... other fields
            eventType = EventType.CUSTOM,
            eventTypeCustom = null // Invalid
        )
        
        val result = EventValidator.validate(event)
        
        assertFalse(result.isValid)
        assertTrue(result.errors.contains("eventTypeCustom required when eventType is CUSTOM"))
    }
    
    @Test
    fun `timeOfDay ALL_DAY allows null start and end`() {
        val timeSlot = TimeSlot(
            id = "slot-1",
            start = null,
            end = null,
            timezone = "UTC",
            timeOfDay = TimeOfDay.ALL_DAY
        )
        
        val result = TimeSlotValidator.validate(timeSlot)
        
        assertTrue(result.isValid)
    }
}
```

### Integration Tests

```kotlin
class DraftEventCreationTest {
    @Test
    fun `create DRAFT event with all new fields`() = runTest {
        // Arrange
        val repository = DatabaseEventRepository(database)
        val locationRepo = PotentialLocationRepository(database)
        
        val event = Event(
            id = "event-1",
            title = "Team Retreat",
            description = "Annual team building",
            organizerId = "user-1",
            eventType = EventType.TEAM_BUILDING,
            minParticipants = 15,
            maxParticipants = 25,
            expectedParticipants = 20,
            proposedSlots = listOf(
                TimeSlot(
                    id = "slot-1",
                    start = null,
                    end = null,
                    timezone = "Europe/Paris",
                    timeOfDay = TimeOfDay.AFTERNOON
                )
            ),
            deadline = "2025-06-01T00:00:00Z",
            status = EventStatus.DRAFT,
            createdAt = Clock.System.now().toString(),
            updatedAt = Clock.System.now().toString()
        )
        
        // Act
        repository.create(event)
        locationRepo.add("event-1", PotentialLocation(
            id = "loc-1",
            eventId = "event-1",
            name = "Paris",
            locationType = LocationType.CITY,
            createdAt = Clock.System.now().toString()
        ))
        
        // Assert
        val loaded = repository.getById("event-1")
        assertNotNull(loaded)
        assertEquals(EventType.TEAM_BUILDING, loaded.eventType)
        assertEquals(20, loaded.expectedParticipants)
        
        val locations = locationRepo.getByEventId("event-1")
        assertEquals(1, locations.size)
        assertEquals("Paris", locations[0].name)
    }
}
```

### UI Tests (Android)

```kotlin
@Test
fun eventTypeSelector_showsCustomFieldWhenCustomSelected() {
    composeTestRule.setContent {
        EventTypeSelector(
            selectedType = EventType.CUSTOM,
            customText = "",
            onTypeSelected = {},
            onCustomTextChanged = {}
        )
    }
    
    // Custom text field should be visible
    composeTestRule
        .onNodeWithText("Custom event type")
        .assertIsDisplayed()
}

@Test
fun participantsCard_showsErrorWhenMaxLessThanMin() {
    composeTestRule.setContent {
        ParticipantsEstimationCard(
            min = 30,
            max = 20, // Invalid
            expected = 25,
            onMinChanged = {},
            onMaxChanged = {},
            onExpectedChanged = {}
        )
    }
    
    composeTestRule
        .onNodeWithText("Maximum must be ≥ minimum")
        .assertIsDisplayed()
}
```

### UI Tests (iOS)

```swift
func testEventTypePicker_showsCustomFieldWhenCustomSelected() {
    let view = EventTypePicker(
        selectedType: .constant(.custom),
        customText: .constant("")
    )
    
    let vc = UIHostingController(rootView: view)
    _ = vc.view // Trigger view load
    
    XCTAssertNotNil(vc.view.findTextField(withPlaceholder: "Custom event type"))
}

func testParticipantsCard_showsErrorWhenMaxLessThanMin() {
    let view = ParticipantsEstimationCard(
        min: .constant(30),
        max: .constant(20),
        expected: .constant(25)
    )
    
    let vc = UIHostingController(rootView: view)
    _ = vc.view
    
    XCTAssertNotNil(vc.view.findLabel(withText: "Maximum must be ≥ minimum"))
}
```

## Migration Strategy

### SQLDelight Migration

```sql
-- Migration V1 → V2
-- Add columns to event table
ALTER TABLE event ADD COLUMN eventType TEXT DEFAULT 'OTHER';
ALTER TABLE event ADD COLUMN eventTypeCustom TEXT;
ALTER TABLE event ADD COLUMN minParticipants INTEGER;
ALTER TABLE event ADD COLUMN maxParticipants INTEGER;
ALTER TABLE event ADD COLUMN expectedParticipants INTEGER;

-- Create potential_location table
CREATE TABLE potential_location (
    id TEXT PRIMARY KEY NOT NULL,
    eventId TEXT NOT NULL,
    name TEXT NOT NULL,
    locationType TEXT NOT NULL,
    address TEXT,
    coordinates TEXT,
    createdAt TEXT NOT NULL,
    FOREIGN KEY (eventId) REFERENCES event(id) ON DELETE CASCADE
);

-- Add timeOfDay to time_slot
ALTER TABLE time_slot ADD COLUMN timeOfDay TEXT DEFAULT 'SPECIFIC';

-- Ensure existing timeslots have SPECIFIC
UPDATE time_slot SET timeOfDay = 'SPECIFIC' WHERE timeOfDay IS NULL;
```

### Data Migration Test

```kotlin
class MigrationTest {
    @Test
    fun `migrate existing events to new schema`() = runTest {
        // Arrange: Create old-style event
        val oldEvent = createLegacyEvent()
        legacyRepository.save(oldEvent)
        
        // Act: Run migration
        val migration = Migration_1_to_2()
        migration.migrate(database)
        
        // Assert: Verify defaults applied
        val migratedEvent = repository.getById(oldEvent.id)
        assertNotNull(migratedEvent)
        assertEquals(EventType.OTHER, migratedEvent.eventType)
        assertNull(migratedEvent.minParticipants)
        
        // Verify existing time slots got SPECIFIC
        val timeSlots = timeSlotRepository.getByEventId(oldEvent.id)
        timeSlots.forEach { slot ->
            assertEquals(TimeOfDay.SPECIFIC, slot.timeOfDay)
        }
    }
}
```

## Performance Considerations

### Database Indexes

```sql
-- Index for frequent queries
CREATE INDEX idx_potential_location_eventId 
ON potential_location(eventId);

CREATE INDEX idx_event_type 
ON event(eventType) 
WHERE eventType != 'OTHER';

CREATE INDEX idx_event_status_organizer 
ON event(status, organizerId);
```

### Lazy Loading

- PotentialLocations loaded only when user views Step 3 of wizard
- TimeSlots loaded only in Step 4
- Reduces initial load time for large events

### Pagination (Future)

If events have >100 potential locations (unlikely), implement pagination:

```kotlin
fun getPotentialLocations(eventId: String, limit: Int, offset: Int): List<PotentialLocation>
```

## Accessibility

### Android (TalkBack)

```kotlin
// Content descriptions for IconButtons
IconButton(
    onClick = { onRemoveLocation(location.id) },
    modifier = Modifier.semantics {
        contentDescription = "Remove ${location.name}"
    }
) {
    Icon(Icons.Default.Delete, contentDescription = null)
}

// State descriptions for validation
TextField(
    value = maxParticipants.toString(),
    onValueChange = { ... },
    isError = maxParticipants < minParticipants,
    modifier = Modifier.semantics {
        if (maxParticipants < minParticipants) {
            stateDescription = "Error: Maximum must be greater than minimum"
        }
    }
)
```

### iOS (VoiceOver)

```swift
// Accessibility labels
Button(action: { removeLocation(location) }) {
    Image(systemName: "trash")
}
.accessibilityLabel("Remove \(location.name)")

// Accessibility hints for validation
TextField("Max participants", text: $max)
    .accessibilityHint(isValid ? "" : "Error: Maximum must be greater than minimum")
```

## Open Questions / TODOs

1. **Q: Should we allow editing eventType after DRAFT?**
   - Decision pending: Block after POLLING (consistent with other DRAFT-only fields)

2. **Q: Coordinates format for PotentialLocation?**
   - Current: JSON string `{"lat": 48.8566, "lng": 2.3522}`
   - Alternative: Two separate REAL columns (lat, lng)
   - Decision: JSON for flexibility (future: elevation, accuracy)

3. **Q: Limit on number of PotentialLocations per event?**
   - Proposal: Soft limit of 10 (UI warning), hard limit of 50 (DB constraint)
   - Rationale: Prevent abuse, maintain UX quality

4. **Q: timeOfDay MORNING/AFTERNOON/EVENING → exact times?**
   - MORNING: 06:00-12:00
   - AFTERNOON: 12:00-18:00
   - EVENING: 18:00-23:00
   - Used for filtering/suggestions, not strict enforcement

## References

- [Material Design 3 - Forms](https://m3.material.io/components/text-fields/overview)
- [iOS HIG - Entering Data](https://developer.apple.com/design/human-interface-guidelines/entering-data)
- [SQLDelight Migrations](https://cashapp.github.io/sqldelight/2.0.0/migrations/)
- [RFC 2119 - Key words for RFCs](https://www.ietf.org/rfc/rfc2119.txt)
