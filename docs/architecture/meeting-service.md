# MeetingService Architecture

**Component Level Architecture Documentation**

## Overview

MeetingService is a cross-platform service for managing virtual meetings (Zoom, Google Meet, FaceTime) in the Wakeve application. It follows the **Functional Core, Imperative Shell (FC&IS)** pattern to ensure testability and separation of concerns.

## Architecture Diagram (C4 Component Level)

```
┌─────────────────────────────────────────────────────────────────────┐
│                           Presentation Layer                     │
│  ┌────────────────────┐  ┌────────────────────┐                 │
│  │ MeetingListScreen │  │  CreateMeetingUI  │                 │
│  │   (Compose)       │  │    (Compose)      │                 │
│  └────────────────────┘  └────────────────────┘                 │
│           │                        │                             │
│           └────────────────┬────────┘                             │
│                          ▼                                      │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │  MeetingManagementStateMachine (MVI FSM)                  │   │
│  │  - CreateMeeting, UpdateMeeting, CancelMeeting            │   │
│  │  - GenerateMeetingLink                                    │   │
│  └────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                       Imperative Shell                           │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │  MeetingService (orchestration + side effects)           │   │
│  │  - createMeeting()                                       │   │
│  │  - updateMeeting()                                       │   │
│  │  - cancelMeeting()                                       │   │
│  │  - sendInvitations()                                      │   │
│  │  - generateMeetingLink()                                  │   │
│  └────────────────────────────────────────────────────────────┘   │
│           │                │                 │                     │
│           ▼                ▼                 ▼                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │  Meeting     │  │  Platform    │  │  Calendar    │         │
│  │  Repository  │  │  Providers  │  │  Service     │         │
│  └──────────────┘  └──────────────┘  └──────────────┘         │
│           │                │                 │                     │
│           └────────────────┴─────────────────┘                 │
│                          ▼                                      │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │  NotificationService                                     │   │
│  │  - sendPushNotification()                                │   │
│  └────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        Functional Core                          │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │  Platform Providers (Pure Functions)                     │   │
│  │  ┌──────────────────┐  ┌──────────────────┐            │   │
│  │  │ ZoomProvider     │  │ GoogleMeetProvider│            │   │
│  │  │ - generateLink   │  │ - generateLink   │            │   │
│  │  │ - cancelMeeting  │  │ - cancelMeeting  │            │   │
│  │  └──────────────────┘  └──────────────────┘            │   │
│  │  ┌──────────────────┐                                    │   │
│  │  │ FaceTimeProvider │                                    │   │
│  │  │ - generateLink   │                                    │   │
│  │  └──────────────────┘                                    │   │
│  └────────────────────────────────────────────────────────────┘   │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │  Pure Logic (deterministic, testable)                    │   │
│  │  - MeetingLinkResponse.toModel()                         │   │
│  │  - MeetingUpdates.validate()                            │   │
│  │  - calculateReminderTime()                               │   │
│  └────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

## Meeting Proxy Backend (Security Layer)

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Client App                              │
│  (Android / iOS / JVM)                                           │
│                         │                                       │
│                         ▼                                       │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │  POST /api/meetings/proxy/zoom/create                   │   │
│  │  Body: { title, scheduledFor, duration, ... }          │   │
│  └────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
                              │ HTTPS
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      Ktor Server                                │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │  MeetingProxyRoutes                                      │   │
│  │  - Receives request WITHOUT API keys                   │   │
│  │  - Validates request data                              │   │
│  │  - Adds server-stored API keys (Environment Variables)    │   │
│  └────────────────────────────────────────────────────────────┘   │
│                          │                                       │
│                          ▼                                       │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │  External API Call                                      │   │
│  │  POST https://api.zoom.us/v2/users/me/meetings         │   │
│  │  Authorization: Bearer <JWT>                           │   │
│  │  (API KEY stored server-side only)                      │   │
│  └────────────────────────────────────────────────────────────┘   │
│                          │                                       │
│                          ▼                                       │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │  Response to Client                                     │   │
│  │  { meetingId, joinUrl, password, ... }                │   │
│  └────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

### Security Benefits

1. **API Key Protection**: External API keys (Zoom, Google Meet) are stored in server environment variables, never exposed to clients
2. **Rate Limiting**: Server can implement rate limiting per user/IP
3. **Audit Logging**: All meeting creation events logged server-side
4. **CORS Control**: Server controls which domains can access the proxy
5. **Token Management**: Server manages JWT tokens for external APIs securely

## Meeting Creation Flow

### Sequence Diagram

```
User            UI           StateMachine      MeetingService    Provider         Database
 │               │               │                  │              │                │
 ├─Click "Create"│               │                  │              │                │
 │──────────────→│               │                  │              │                │
 │               ├─Intent.Create│                  │              │                │
 │               │─────────────→│                  │              │                │
 │               │               ├─createMeeting()   │              │                │
 │               │               │──────────────────→│              │                │
 │               │               │                  ├─validate     │                │
 │               │               │                  │  EventStatus  │                │
 │               │               │                  ├─Select       │                │
 │               │               │                  │  Provider    │                │
 │               │               │                  │─────────────→│                │
 │               │               │                  │←generateLink│                │
 │               │               │                  │──────────────│                │
 │               │               │                  ├─insert       │                │
 │               │               │                  │  Meeting     │                │
 │               │               │                  │─────────────→│                │
 │               │               │                  ├─Schedule     │                │
 │               │               │                  │  Reminders   │                │
 │               │               │                  │─────────────→│                │
 │               │               │←Result.Success   │              │                │
 │               │               │──────────────────│              │                │
 │               │←NavigateTo   │                  │              │                │
 │               │  MeetingList  │                  │              │                │
 │←Show Meeting   │               │                  │              │                │
 │────────────────┤               │                  │              │                │
```

### Step-by-Step Process

1. **User Initiates Creation**
   - User clicks "Create Meeting" button
   - UI collects meeting details (title, platform, date/time, duration)
   - Validation: title required, date must be future

2. **State Machine Dispatches Intent**
   - `MeetingManagementStateMachine` receives `Intent.CreateMeeting`
   - Validates event is in CONFIRMED or ORGANIZING status
   - Delegates to `MeetingService.createMeeting()`

3. **MeetingService Orchestrates**
   - Validates event exists and has correct status
   - Filters participants to only those who validated the date
   - Selects appropriate platform provider (Zoom/GoogleMeet/FaceTime)
   - Calls provider to generate meeting link

4. **Provider Generates Link (Pure Function)**
   - Platform-specific logic executed
   - Returns `MeetingLinkResponse` (URL, meeting ID, password)
   - No side effects (no network calls in pure provider)

5. **Persistence**
   - Meeting data saved to `MeetingRepository` (SQLDelight)
   - Meeting reminders scheduled for all participants
   - Calendar integration triggered (add to native calendar)

6. **Notifications**
   - Push notifications sent to all validated participants
   - Invitation status tracked (PENDING → ACCEPTED/DECLINED)

## Platform Provider Pattern

### Interface

```kotlin
interface MeetingPlatformProvider {
    suspend fun generateMeetingLink(
        platform: MeetingPlatform,
        title: String,
        description: String?,
        startTime: Instant,
        duration: Duration
    ): String

    fun getHostMeetingId(meetingLink: String): String

    fun cancelMeeting(platform: MeetingPlatform, hostMeetingId: String)
}
```

### Implemented Providers

#### ZoomMeetingPlatformProvider

**Features:**
- Generates Zoom meeting URLs with 10-digit meeting ID
- Creates 6-character alphanumeric passwords
- Provides dial-in numbers with phone access
- Supports waiting rooms and participant limits

**Example Link:**
```
https://zoom.us/j/1234567890?pwd=ABC123
Dial-in: +33 1 23 45 67 89 (123456)
```

#### GoogleMeetPlatformProvider

**Features:**
- Generates Google Meet codes (3-3-4 format)
- No password required (Google authentication)
- Integrated with Google Calendar

**Example Link:**
```
https://meet.google.com/abc-def-ghi
```

#### FaceTimePlatformProvider

**Features:**
- Uses iOS FaceTime URL scheme
- No external meeting creation required
- Apple ID authentication handled by iOS

**Example Link:**
```
facetime://apple-id@example.com
```

## Functional Core vs Imperative Shell

### Functional Core (Pure Functions)

Located in `MeetingPlatformProvider` implementations and utility functions:

```kotlin
// ✅ PURE - Deterministic, testable, no side effects
fun generateZoomMeetingId(): String =
    (1..10).map { Random.nextInt(0, 10) }.joinToString("")

fun generateZoomPassword(): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    return (1..6).map { chars.random() }.joinToString("")
}

fun calculateReminderTime(
    meetingTime: Instant,
    timing: MeetingReminderTiming,
    timezone: String
): Instant = when (timing) {
    MeetingReminderTiming.ONE_DAY_BEFORE -> meetingTime.minus(1.days)
    MeetingReminderTiming.ONE_HOUR_BEFORE -> meetingTime.minus(1.hours)
    MeetingReminderTiming.FIFTEEN_MINUTES_BEFORE -> meetingTime.minus(15.minutes)
    MeetingReminderTiming.FIVE_MINUTES_BEFORE -> meetingTime.minus(5.minutes)
}
```

**Characteristics:**
- Input → Output (deterministic)
- No external dependencies
- No I/O operations
- Easy to unit test (no mocks needed)

### Imperative Shell (Side Effects)

Located in `MeetingService` orchestration methods:

```kotlin
// ❌ IMPURE - Side effects (I/O, state, async)
suspend fun createMeeting(
    eventId: String,
    organizerId: String,
    platform: MeetingPlatform,
    // ... other parameters
): Result<VirtualMeeting> {
    // Side effect 1: Database read
    val event = eventQueries.selectById(eventId).executeAsOne()

    // Side effect 2: State mutation
    meetingRepository.createMeeting(meeting)

    // Side effect 3: Network call (calendar service)
    calendarService.addToNativeCalendar(...)

    // Side effect 4: Async notification
    notificationService.sendNotification(...)

    return Result.success(virtualMeeting)
}
```

**Characteristics:**
- Orchestrates pure functions
- Handles I/O operations
- Manages state mutations
- Communicates with external services
- Tests use mocks for external dependencies

## Code Example: Creating a Meeting

### Using MeetingService Directly

```kotlin
class CreateMeetingViewModel(
    private val meetingService: MeetingService,
    private val stateMachine: MeetingManagementStateMachine
) : ViewModel() {

    fun createMeeting(
        eventId: String,
        platform: MeetingPlatform,
        title: String,
        description: String?,
        scheduledFor: Instant,
        duration: Duration
    ) {
        viewModelScope.launch {
            val result = meetingService.createMeeting(
                eventId = eventId,
                organizerId = getCurrentUserId(),
                platform = platform,
                title = title,
                description = description,
                scheduledFor = scheduledFor,
                duration = duration,
                timezone = "Europe/Paris",
                participantLimit = null,
                requirePassword = true,
                waitingRoom = true
            )

            result.onSuccess { meeting ->
                stateMachine.dispatch(Intent.CreateMeetingSuccess(meeting))
            }.onFailure { error ->
                stateMachine.dispatch(Intent.CreateMeetingError(error.message ?: "Unknown error"))
            }
        }
    }
}
```

### Using via State Machine (Recommended)

```kotlin
// In UI Composable
@Composable
fun CreateMeetingScreen(
    eventId: String,
    stateMachine: MeetingManagementStateMachine
) {
    val state by stateMachine.state.collectAsState()

    LaunchedEffect(eventId) {
        stateMachine.dispatch(Intent.LoadEvent(eventId))
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Create Meeting") }) }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Meeting Title") }
            )

            PlatformSelector(
                selectedPlatform = platform,
                onPlatformSelected = { platform = it }
            )

            DateTimePicker(
                scheduledFor = scheduledFor,
                onDateTimeSelected = { scheduledFor = it }
            )

            Button(
                onClick = {
                    stateMachine.dispatch(Intent.CreateMeeting(
                        eventId = eventId,
                        platform = platform,
                        title = title,
                        description = description,
                        scheduledFor = scheduledFor,
                        duration = 1.hours
                    ))
                },
                enabled = title.isNotBlank() && scheduledFor != null
            ) {
                Text("Create Meeting")
            }
        }
    }
}
```

## Key Design Decisions

### 1. FC&IS Pattern

**Why?**
- Separates pure business logic from side effects
- Makes core logic easily testable without mocks
- Improves code maintainability and reasoning

**Impact:**
- Pure functions in `MeetingPlatformProvider` are 100% testable
- Side effects isolated to `MeetingService`
- Clear separation of concerns

### 2. Provider Pattern

**Why?**
- Extensible to new platforms (Teams, Webex, etc.)
- Platform-specific logic encapsulated
- Easy to mock for testing

**Impact:**
- New platforms require minimal changes
- Each provider is independently testable
- Clear interface contract

### 3. Backend Proxy for Security

**Why?**
- API keys never exposed to clients
- Server can implement rate limiting and auditing
- Simplifies client code (no credential management)

**Impact:**
- Enhanced security posture
- Simplified deployment (keys in one place)
- Easier to rotate API keys

### 4. State Machine Coordination

**Why?**
- MVI pattern provides clear state flow
- Easy to test state transitions
- Separates UI from business logic

**Impact:**
- Predictable state changes
- Better test coverage
- Clear intent-based API

## Testing Strategy

### Unit Tests (Functional Core)

```kotlin
class ZoomProviderTest {
    @Test
    fun `generateMeetingLink creates valid URL`() {
        val provider = ZoomMeetingPlatformProvider()
        val result = provider.generateMeetingLink(
            platform = MeetingPlatform.ZOOM,
            title = "Test Meeting",
            description = null,
            startTime = Instant.parse("2026-02-08T14:00:00Z"),
            duration = 1.hours
        )

        assertThat(result).startsWith("https://zoom.us/j/")
        assertThat(result).contains("pwd=")
    }
}
```

### Integration Tests (Imperative Shell)

```kotlin
class MeetingServiceTest {
    @Test
    fun `createMeeting succeeds for confirmed event`() = runTest {
        val mockDb = createMockDatabase()
        val mockCalendar = MockCalendarService()
        val mockNotification = MockNotificationService()
        val service = MeetingService(mockDb, mockCalendar, mockNotification)

        // Setup
        val eventId = createConfirmedEvent(mockDb)

        // Execute
        val result = service.createMeeting(
            eventId = eventId,
            organizerId = "user-1",
            platform = MeetingPlatform.ZOOM,
            title = "Test Meeting",
            description = "Test",
            scheduledFor = Instant.parse("2026-02-08T14:00:00Z"),
            duration = 1.hours,
            timezone = "UTC"
        )

        // Verify
        assertTrue(result.isSuccess)
        val meeting = result.getOrThrow()
        assertEquals("Test Meeting", meeting.title)
        assertEquals(MeetingPlatform.ZOOM, meeting.platform)
    }
}
```

## File Structure

```
shared/src/commonMain/kotlin/com/guyghost/wakeve/
├── meeting/
│   ├── MeetingService.kt                    # Main service (Imperative Shell)
│   ├── MeetingRepository.kt                  # Database repository
│   ├── MeetingPlatformProvider.kt            # Provider interface
│   ├── ZoomMeetingPlatformProvider.kt        # Zoom implementation
│   ├── GoogleMeetPlatformProvider.kt         # Google Meet implementation
│   ├── FaceTimePlatformProvider.kt           # FaceTime implementation
│   ├── MockMeetingPlatformProvider.kt        # Mock for testing
│   ├── Meeting.kt                          # Domain model
│   ├── MeetingStatus.kt                     # Status enum
│   ├── MeetingReminderTiming.kt              # Reminder timing
│   └── MeetingUpdates.kt                    # Update DTOs
├── presentation/
│   ├── statemachine/
│   │   └── MeetingManagementStateMachine.kt   # MVI state machine
│   └── state/
│       └── MeetingManagementContract.kt      # Intent, State, SideEffect
└── models/
    └── MeetingModels.kt                     # Shared models

server/src/main/kotlin/com/guyghost/wakeve/
└── routes/
    └── MeetingProxyRoutes.kt                 # Backend proxy endpoints
```

## References

- **Meeting Service Code:** `shared/src/commonMain/kotlin/com/guyghost/wakeve/meeting/MeetingService.kt`
- **Platform Providers:** `shared/src/commonMain/kotlin/com/guyghost/wakeve/meeting/MeetingPlatformProvider.kt`
- **Backend Proxy:** `server/src/main/kotlin/com/guyghost/wakeve/routes/MeetingProxyRoutes.kt`
- **API Documentation:** `docs/api/meeting-api.md`
- **FC&IS Pattern:** `skill/architecture/SKILL.md`

---

**Document Version:** 1.0
**Last Updated:** 2026-02-07
**Status:** ✅ Complete
