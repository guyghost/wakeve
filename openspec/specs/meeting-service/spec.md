# Meeting Service Specification

> **Capability**: `meeting-service`
> **Version**: 2.0.0
> **Status**: Active
> **Last Updated**: 2026-02-08

## Overview

The Meeting Service enables Wakeve users to create and manage virtual meetings across multiple platforms (Zoom, Google Meet, FaceTime) for event coordination. It provides secure API proxy endpoints to hide external platform credentials and handles the complete meeting lifecycle from creation to cancellation.

**Key Features**:
- Multi-platform meeting creation (Zoom, Google Meet, FaceTime)
- Secure backend proxy for external API authentication
- Meeting lifecycle management (create, update, cancel, status)
- Rate limiting per provider
- Integration with notification system
- Calendar integration for automatic event creation

**Version**: 2.0.0
**Status**: Active
**Created**: 2025-12-26
**Maintainer**: Wakeve Team

### Core Concepts

**VirtualMeeting**: A scheduled online meeting associated with an event, containing platform-specific connection details and metadata.

**MeetingPlatform**: Enum defining supported video conferencing platforms (Zoom, Google Meet, FaceTime, Teams, Webex).

**MeetingProxy**: Backend API proxy that authenticates with external platforms using server-stored credentials, never exposing API keys to clients.

**MeetingStatus**: Lifecycle state of a meeting (scheduled, started, ended, cancelled).

### Dependencies

| Dependency | Type | Description |
|------------|------|-------------|
| `event-organization` | Spec | Events that meetings are associated with |
| `calendar-management` | Spec | Native calendar integration |
| `notification-management` | Spec | Meeting reminders and invitations |
| `collaboration-management` | Spec | Participant management |

## Purpose

Enable event organizers to create virtual meetings for remote coordination while keeping external platform credentials secure server-side.

### Use Cases

- **Remote Event Planning**: Create Zoom/Google Meet meetings for planning sessions
- **International Coordination**: Support participants across different timezones
- **Quick FaceTime**: iOS users can quickly start FaceTime calls
- **Automated Reminders**: Send notifications before meetings start
- **Calendar Sync**: Automatically add meetings to native calendars

## Requirements

### Requirement: Secure Platform API Integration
**ID**: `MEETING-001`

The system SHALL integrate with external meeting platforms (Zoom, Google Meet) via a backend proxy that:
- Hides API keys and secrets from clients
- Authenticates with external platforms server-side
- Returns only necessary meeting details to clients
- Implements rate limiting per provider
- Logs all meeting creation events for audit

#### Scenario: Create Zoom meeting via proxy
- **GIVEN** User is authenticated and Zoom API credentials are configured on server
- **WHEN** User requests meeting creation with title, time, and duration
- **THEN** Server authenticates with Zoom API using stored JWT credentials
- **AND** Server creates meeting via Zoom REST API
- **AND** Server returns meeting ID, join URL, password, and dial-in details
- **AND** API keys are never exposed to client

#### Scenario: Handle missing Zoom credentials
- **GIVEN** Zoom API credentials are not configured on server
- **WHEN** User requests Zoom meeting creation
- **THEN** Server returns 503 Service Unavailable with error code `zoom_not_configured`

### Requirement: Zoom OAuth Flow
**ID**: `MEETING-002`

The system SHALL support Zoom OAuth 2.0 authentication for production use:
- Generate JWT tokens using API Key and Secret
- Include Bearer token in Zoom API requests
- Handle token expiration and renewal
- Support both Server-to-Server OAuth and JWT app types

#### Scenario: Generate Zoom JWT token
- **GIVEN** ZOOM_API_KEY and ZOOM_API_SECRET are configured
- **WHEN** Server needs to call Zoom API
- **THEN** Server generates JWT token with API credentials
- **AND** Token includes required claims (iss, exp)
- **AND** Token is valid for maximum expiration time

### Requirement: Google Meet API Integration
**ID**: `MEETING-003`

The system SHALL integrate with Google Calendar API to create Google Meet meetings:
- Authenticate using service account credentials
- Create calendar events with conference data
- Extract meeting URL and code from response
- Handle domain-wide delegation for enterprise accounts

#### Scenario: Create Google Meet via Calendar API
- **GIVEN** GOOGLE_MEET_CREDENTIALS JSON is configured
- **WHEN** User requests Google Meet creation
- **THEN** Server creates calendar event with conferenceData.createRequest
- **AND** Server extracts hangoutLink and meeting code
- **AND** Server returns meeting URL to client

### Requirement: Rate Limiting Per Provider
**ID**: `MEETING-004`

The system SHALL implement rate limiting for each platform provider:
- Enforce platform-specific rate limits (Zoom: 100 requests/hour per user)
- Track usage per user/IP combination
- Return 429 Too Many Requests when limits exceeded
- Implement exponential backoff for retries

#### Scenario: Enforce Zoom rate limit
- **GIVEN** User has made 100 Zoom meeting creation requests in past hour
- **WHEN** User attempts another Zoom meeting creation
- **THEN** Server returns 429 Too Many Requests
- **AND** Response includes Retry-After header

### Requirement: Meeting Lifecycle Management
**ID**: `MEETING-005`

The system SHALL support complete meeting lifecycle operations:
- Create meetings with platform-specific settings
- Update meeting time, duration, and details
- Cancel meetings and notify participants
- Query real-time meeting status
- Track meeting state transitions

#### Scenario: Update scheduled meeting
- **GIVEN** Meeting exists with status SCHEDULED
- **WHEN** Organizer updates meeting time
- **THEN** Server calls platform API to update meeting
- **AND** Database record is updated
- **AND** Participants are notified of change
- **AND** Calendar event is updated

#### Scenario: Cancel meeting
- **GIVEN** Meeting exists with status SCHEDULED
- **WHEN** Organizer cancels meeting
- **THEN** Server calls platform API to cancel
- **AND** Status updated to CANCELLED
- **AND** All scheduled reminders are cancelled
- **AND** Participants receive cancellation notification

### Requirement: Notification Integration
**ID**: `MEETING-006`

The system SHALL integrate with notification service for:
- Meeting invitations to validated participants
- Pre-meeting reminders (1 day, 1 hour, 15 min, 5 min before)
- Meeting cancellation notifications
- Meeting update notifications

#### Scenario: Send meeting invitations
- **GIVEN** Meeting created with 3 validated participants
- **WHEN** Organizer sends invitations
- **THEN** System sends push notification to each participant
- **AND** Creates invitation records with PENDING status
- **AND** Adds meeting to participant's native calendar

### Requirement: Calendar Integration
**ID**: `MEETING-007`

The system SHALL automatically sync meetings with native device calendars:
- Create calendar event when meeting is created
- Update calendar event when meeting details change
- Delete calendar event when meeting is cancelled
- Include meeting link in calendar event description

## Data Models

> All models are defined in language-agnostic format (JSON/Kotlin) to support multiplatform implementation.

### MeetingPlatform

```kotlin
enum class MeetingPlatform {
    ZOOM,           // Zoom Meetings
    GOOGLE_MEET,    // Google Meet
    FACETIME,       // Apple FaceTime
    TEAMS,          // Microsoft Teams (future)
    WEBEX           // Cisco Webex (future)
}
```

### MeetingStatus

```kotlin
enum class MeetingStatus {
    SCHEDULED,      // Meeting is scheduled
    STARTED,        // Meeting is in progress
    ENDED,          // Meeting has ended
    CANCELLED       // Meeting was cancelled
}
```

### VirtualMeeting

| Field | Type | Description | Constraints |
|-------|------|-------------|-------------|
| `id` | string (UUID) | Unique identifier | Required, auto-generated |
| `eventId` | string | Associated event ID | Required |
| `organizerId` | string | Meeting organizer ID | Required |
| `platform` | MeetingPlatform | Meeting platform | Required |
| `meetingId` | string | Platform meeting ID | Required |
| `meetingPassword` | string? | Meeting password | Optional |
| `meetingUrl` | string | Full meeting URL | Required |
| `dialInNumber` | string? | Phone dial-in number | Optional (Zoom only) |
| `dialInPassword` | string? | Dial-in PIN | Optional (Zoom only) |
| `title` | string | Meeting title | Required, max 200 chars |
| `description` | string? | Meeting description | Optional, max 5000 chars |
| `scheduledFor` | Instant | Scheduled start time | Required |
| `duration` | Duration | Meeting duration | Required, 1-1440 minutes |
| `timezone` | string | Timezone identifier | Required, default "UTC" |
| `participantLimit` | Int? | Max participants | Optional |
| `requirePassword` | boolean | Password required | Default: true |
| `waitingRoom` | boolean | Waiting room enabled | Default: true |
| `hostKey` | string? | Host control key | Optional (Zoom only) |
| `createdAt` | Instant | Creation timestamp | Required |
| `status` | MeetingStatus | Current status | Required |

```kotlin
@Serializable
data class VirtualMeeting(
    val id: String,
    val eventId: String,
    val organizerId: String,
    val platform: MeetingPlatform,
    val meetingId: String,
    val meetingPassword: String?,
    val meetingUrl: String,
    val dialInNumber: String?,
    val dialInPassword: String?,
    val title: String,
    val description: String?,
    val scheduledFor: Instant,
    val duration: Duration,
    val timezone: String,
    val participantLimit: Int?,
    val requirePassword: Boolean,
    val waitingRoom: Boolean,
    val hostKey: String?,
    val createdAt: Instant,
    val status: MeetingStatus
)
```

### InvitationStatus

```kotlin
enum class InvitationStatus {
    PENDING,        // Invitation sent, awaiting response
    ACCEPTED,       // Participant accepted
    DECLINED,       // Participant declined
    TENTATIVE       // Participant tentative
}
```

### MeetingInvitation

| Field | Type | Description | Constraints |
|-------|------|-------------|-------------|
| `id` | string (UUID) | Unique identifier | Required |
| `meetingId` | string | Associated meeting ID | Required |
| `participantId` | string | Participant ID | Required |
| `status` | InvitationStatus | Response status | Required |
| `sentAt` | Instant | Sent timestamp | Required |
| `respondedAt` | Instant? | Response timestamp | Optional |
| `acceptedAt` | Instant? | Acceptance timestamp | Optional |

```kotlin
@Serializable
data class MeetingInvitation(
    val id: String,
    val meetingId: String,
    val participantId: String,
    val status: InvitationStatus,
    val sentAt: Instant,
    val respondedAt: Instant?,
    val acceptedAt: Instant?
)
```

### MeetingReminder

| Field | Type | Description | Constraints |
|-------|------|-------------|-------------|
| `id` | string (UUID) | Unique identifier | Required |
| `meetingId` | string | Associated meeting ID | Required |
| `participantId` | string? | Participant ID (null for all) | Optional |
| `timing` | ReminderTiming | When to send reminder | Required |
| `scheduledFor` | Instant | Scheduled send time | Required |
| `sentAt` | Instant? | Actual send time | Optional |
| `status` | ReminderStatus | Reminder status | Required |

```kotlin
enum class ReminderTiming {
    ONE_DAY_BEFORE,
    ONE_HOUR_BEFORE,
    FIFTEEN_MINUTES_BEFORE,
    FIVE_MINUTES_BEFORE
}

enum class ReminderStatus {
    SCHEDULED,
    SENT,
    FAILED
}

@Serializable
data class MeetingReminder(
    val id: String,
    val meetingId: String,
    val participantId: String?,
    val timing: ReminderTiming,
    val scheduledFor: Instant,
    val sentAt: Instant?,
    val status: ReminderStatus
)
```

## API / Interface

### REST API Endpoints

> Base path: `/api/meetings/proxy`
> These endpoints act as a secure proxy to external platform APIs

| Endpoint | Method | Description | Auth Required |
|----------|--------|-------------|---------------|
| POST /api/meetings/proxy/zoom/create | POST | Create Zoom meeting | Yes |
| POST /api/meetings/proxy/zoom/{meetingId}/cancel | POST | Cancel Zoom meeting | Yes |
| GET /api/meetings/proxy/zoom/{meetingId}/status | GET | Get Zoom meeting status | Yes |
| POST /api/meetings/proxy/google-meet/create | POST | Create Google Meet | Yes |

### Create Zoom Meeting

**Endpoint**: `POST /api/meetings/proxy/zoom/create`

**Description**: Creates a new Zoom meeting via Zoom API. Server authenticates using JWT token generated from stored credentials.

**Authentication**: Required

**Request Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `title` | string | Yes | Meeting title (1-200 chars) |
| `description` | string | No | Meeting description (max 5000 chars) |
| `scheduledFor` | string | Yes | ISO-8601 datetime |
| `duration` | int | Yes | Duration in minutes (1-1440) |
| `timezone` | string | No | Timezone identifier (default: "UTC") |
| `participantLimit` | int | No | Max participants (optional) |
| `requirePassword` | boolean | No | Enable password (default: true) |
| `waitingRoom` | boolean | No | Enable waiting room (default: true) |

**Request Body**:
```json
{
  "title": "Team Planning Session",
  "description": "Q4 2026 planning meeting",
  "scheduledFor": "2026-02-15T14:00:00Z",
  "duration": 60,
  "timezone": "Europe/Paris",
  "participantLimit": 100,
  "requirePassword": true,
  "waitingRoom": true
}
```

**Response 200 OK**:
```json
{
  "meetingId": "1234567890",
  "joinUrl": "https://zoom.us/j/1234567890?pwd=ABC123",
  "password": "ABC123",
  "hostUrl": "https://zoom.us/j/1234567890?pwd=XYZ789",
  "hostKey": "123456",
  "dialInNumber": "+33 1 23 45 67 89",
  "dialInPassword": "123456"
}
```

**Error Responses**:
| Code | Description |
|------|-------------|
| 400 Bad Request | Invalid request parameters |
| 429 Too Many Requests | Rate limit exceeded |
| 503 Service Unavailable | Zoom API not configured |

### Cancel Zoom Meeting

**Endpoint**: `POST /api/meetings/proxy/zoom/{meetingId}/cancel`

**Description**: Cancels an existing Zoom meeting via Zoom API.

**Authentication**: Required

**Path Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `meetingId` | string | Yes | 10-digit Zoom meeting ID |

**Response 200 OK**:
```json
{
  "success": true,
  "message": "Meeting 1234567890 cancelled successfully"
}
```

### Get Zoom Meeting Status

**Endpoint**: `GET /api/meetings/proxy/zoom/{meetingId}/status`

**Description**: Retrieves current status of a Zoom meeting from Zoom API.

**Authentication**: Required

**Response 200 OK**:
```json
{
  "meetingId": "1234567890",
  "status": "scheduled",
  "startTime": "2026-02-15T14:00:00Z",
  "duration": 60,
  "participantCount": 0
}
```

### Create Google Meet Meeting

**Endpoint**: `POST /api/meetings/proxy/google-meet/create`

**Description**: Creates a Google Meet meeting via Google Calendar API with conference data.

**Authentication**: Required

**Request Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `title` | string | Yes | Meeting title (1-200 chars) |
| `description` | string | No | Meeting description (max 5000 chars) |
| `scheduledFor` | string | Yes | ISO-8601 datetime |
| `duration` | int | Yes | Duration in minutes (1-1440) |
| `timezone` | string | No | Timezone identifier (default: "UTC") |

**Response 200 OK**:
```json
{
  "meetingUrl": "https://meet.google.com/abc-def-ghi",
  "meetingCode": "abc-def-ghi"
}
```

### Kotlin Interface (for shared layer)

```kotlin
interface MeetingService {
    /**
     * Creates a virtual meeting on the specified platform
     *
     * @param eventId Associated event ID
     * @param organizerId Meeting organizer user ID
     * @param platform Meeting platform to use
     * @param title Meeting title
     * @param description Optional meeting description
     * @param scheduledFor When the meeting is scheduled
     * @param duration Expected duration
     * @param timezone Timezone for the meeting
     * @param participantLimit Optional max participants
     * @param requirePassword Whether password is required
     * @param waitingRoom Whether waiting room is enabled
     * @return Result<VirtualMeeting> success or failure
     */
    suspend fun createMeeting(
        eventId: String,
        organizerId: String,
        platform: MeetingPlatform,
        title: String,
        description: String?,
        scheduledFor: Instant,
        duration: Duration,
        timezone: String,
        participantLimit: Int? = null,
        requirePassword: Boolean = true,
        waitingRoom: Boolean = true
    ): Result<VirtualMeeting>

    /**
     * Updates an existing meeting
     *
     * @param meetingId Meeting to update
     * @param title New title
     * @param description New description
     * @param scheduledFor New scheduled time
     * @param duration New duration
     * @return Result<VirtualMeeting> updated meeting
     */
    suspend fun updateMeeting(
        meetingId: String,
        title: String? = null,
        description: String? = null,
        scheduledFor: Instant? = null,
        duration: Duration? = null
    ): Result<VirtualMeeting>

    /**
     * Cancels a meeting
     *
     * @param meetingId Meeting to cancel
     * @return Result<Unit> success or failure
     */
    suspend fun cancelMeeting(meetingId: String): Result<Unit>

    /**
     * Starts a meeting
     *
     * @param meetingId Meeting to start
     * @return Result<VirtualMeeting> updated meeting
     */
    suspend fun startMeeting(meetingId: String): Result<VirtualMeeting>

    /**
     * Ends a meeting
     *
     * @param meetingId Meeting to end
     * @return Result<VirtualMeeting> updated meeting
     */
    suspend fun endMeeting(meetingId: String): Result<VirtualMeeting>

    /**
     * Sends invitations to validated participants
     *
     * @param meetingId Meeting to invite participants to
     * @return Result<Unit> success or failure
     */
    suspend fun sendInvitations(meetingId: String): Result<Unit>

    /**
     * Records participant response to invitation
     *
     * @param invitationId Invitation to respond to
     * @param status Participant's response
     * @return Result<Unit> success or failure
     */
    suspend fun respondToInvitation(
        invitationId: String,
        status: InvitationStatus
    ): Result<Unit>
}
```

## Security

### Authentication Requirements

- All proxy endpoints require authentication via Bearer token
- Static user IDs in Phase 2 (development)
- OAuth2 authentication planned for Phase 3

### Authorization Requirements

| Role | Create | Read | Update | Delete | Special Operations |
|------|--------|------|--------|--------|-------------------|
| Organizer | Yes | Yes | Yes | Yes | Send invitations |
| Participant | No | Yes | No | No | Respond to invitations |
| Guest | No | Limited | No | No | None |

### Data Protection

- API keys stored in server environment variables only
- Never expose API keys or secrets to clients
- All meeting creation events logged for audit
- Zoom/Google credentials encrypted at rest

### Validation Rules

- Title: 1-200 characters, non-blank
- Description: Max 5000 characters
- Duration: 1-1440 minutes (24 hours max)
- Rate limiting: 100 requests per hour per user per platform
- Input sanitization for all user-provided fields

### API Key Management

The system manages external platform credentials via environment variables:

```bash
# Zoom API credentials (JWT app type)
ZOOM_API_KEY=your_zoom_api_key_here
ZOOM_API_SECRET=your_zoom_api_secret_here

# Google Meet credentials (service account JSON)
GOOGLE_MEET_CREDENTIALS='{"type":"service_account","project_id":"..."}'
```

**Credential Management Best Practices**:
- Rotate credentials every 90 days
- Use separate credentials for dev/staging/production
- Never commit credentials to version control
- Use secret management services (AWS Secrets Manager, Azure Key Vault)
- Implement credential health checks

## State Machine Integration

### Intents

```kotlin
sealed interface MeetingIntent : Intent {
    data class CreateMeeting(
        val eventId: String,
        val platform: MeetingPlatform,
        val title: String,
        val scheduledFor: Instant,
        val duration: Duration
    ) : MeetingIntent

    data class UpdateMeeting(
        val meetingId: String,
        val title: String? = null,
        val scheduledFor: Instant? = null
    ) : MeetingIntent

    data class CancelMeeting(val meetingId: String) : MeetingIntent

    data class SendInvitations(val meetingId: String) : MeetingIntent

    data class RespondToInvitation(
        val invitationId: String,
        val status: InvitationStatus
    ) : MeetingIntent
}
```

### State

```kotlin
data class MeetingState(
    val meetings: List<VirtualMeeting> = emptyList(),
    val invitations: List<MeetingInvitation> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)
```

### Side Effects

| Intent | Side Effect | Description |
|--------|-------------|-------------|
| CreateMeeting | ShowToast("Meeting created") | Success notification |
| CreateMeeting | NavigateTo("meeting-detail") | After successful creation |
| CancelMeeting | ShowToast("Meeting cancelled") | Success notification |
| CancelMeeting | NotifyParticipants() | Notify all participants |
| SendInvitations | ShowToast("Invitations sent") | Success notification |

## Database Schema

```sql
CREATE TABLE virtual_meeting (
    id TEXT PRIMARY KEY,
    event_id TEXT NOT NULL,
    organizer_id TEXT NOT NULL,
    platform TEXT NOT NULL,
    meeting_id TEXT NOT NULL,
    meeting_password TEXT,
    meeting_url TEXT NOT NULL,
    dial_in_number TEXT,
    dial_in_password TEXT,
    title TEXT NOT NULL,
    description TEXT,
    scheduled_for INTEGER NOT NULL,
    duration INTEGER NOT NULL,
    timezone TEXT NOT NULL,
    participant_limit INTEGER,
    require_password INTEGER NOT NULL DEFAULT 1,
    waiting_room INTEGER NOT NULL DEFAULT 1,
    host_key TEXT,
    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
    status TEXT NOT NULL DEFAULT 'SCHEDULED',
    FOREIGN KEY (event_id) REFERENCES event(id) ON DELETE CASCADE,
    FOREIGN KEY (organizer_id) REFERENCES participant(id) ON DELETE CASCADE
);

CREATE INDEX idx_virtual_meeting_event ON virtual_meeting(event_id);
CREATE INDEX idx_virtual_meeting_organizer ON virtual_meeting(organizer_id);
CREATE INDEX idx_virtual_meeting_status ON virtual_meeting(status);
CREATE INDEX idx_virtual_meeting_scheduled_for ON virtual_meeting(scheduled_for);

CREATE TABLE meeting_invitation (
    id TEXT PRIMARY KEY,
    meeting_id TEXT NOT NULL,
    participant_id TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'PENDING',
    sent_at INTEGER NOT NULL,
    responded_at INTEGER,
    accepted_at INTEGER,
    FOREIGN KEY (meeting_id) REFERENCES virtual_meeting(id) ON DELETE CASCADE,
    FOREIGN KEY (participant_id) REFERENCES participant(id) ON DELETE CASCADE,
    UNIQUE (meeting_id, participant_id)
);

CREATE INDEX idx_meeting_invitation_meeting ON meeting_invitation(meeting_id);
CREATE INDEX idx_meeting_invitation_participant ON meeting_invitation(participant_id);

CREATE TABLE meeting_reminder (
    id TEXT PRIMARY KEY,
    meeting_id TEXT NOT NULL,
    participant_id TEXT,
    timing TEXT NOT NULL,
    scheduled_for INTEGER NOT NULL,
    sent_at INTEGER,
    status TEXT NOT NULL DEFAULT 'SCHEDULED',
    FOREIGN KEY (meeting_id) REFERENCES virtual_meeting(id) ON DELETE CASCADE,
    FOREIGN KEY (participant_id) REFERENCES participant(id) ON DELETE CASCADE
);

CREATE INDEX idx_meeting_reminder_meeting ON meeting_reminder(meeting_id);
CREATE INDEX idx_meeting_reminder_scheduled_for ON meeting_reminder(scheduled_for);
```

## Testing Requirements

### Unit Tests

- Meeting creation (3 tests)
  - Create Zoom meeting generates valid ID and password
  - Create Google Meet generates valid code
  - Create FaceTime uses Apple ID
- Meeting lifecycle (3 tests)
  - Update meeting modifies fields
  - Cancel meeting updates status
  - Status transitions are valid
- Invitations (2 tests)
  - Send invitations only to validated participants
  - Respond to invitation updates status
- Reminders (1 test)
  - Schedule reminders creates all timing variants

**Coverage Target**: 85%

### Integration Tests

- Zoom API integration: Create, update, cancel via Zoom API
- Google Meet API integration: Create via Calendar API
- Rate limiting: Enforce per-user limits
- Error handling: Handle API failures gracefully

### Test Commands

```bash
# Run unit tests
./gradlew shared:allTests

# Run integration tests
./gradlew server:integrationTest
```

### Test Scenarios

#### Scenario: Create Zoom meeting

```kotlin
@Test
fun `createZoomMeeting generates valid meeting ID and password`() {
    // Given
    val eventId = "event-1"
    val organizerId = "org-1"

    // When
    val result = meetingService.createMeeting(
        eventId = eventId,
        organizerId = organizerId,
        platform = MeetingPlatform.ZOOM,
        title = "Test Meeting",
        description = null,
        scheduledFor = Instant.now(),
        duration = Duration.ofHours(1),
        timezone = "Europe/Paris"
    )

    // Then
    assertTrue(result.isSuccess)
    val meeting = result.getOrThrow()
    assertEquals(meeting.platform, MeetingPlatform.ZOOM)
    assertTrue(meeting.meetingId.length == 10)
    assertTrue(meeting.meetingPassword?.length == 6)
    assertTrue(meeting.meetingUrl.contains("zoom.us/j/"))
}
```

## Platform-Specific Implementation

### Shared Layer

- `shared/src/commonMain/kotlin/com/guyghost/wakeve/meeting/MeetingService.kt`
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/meeting/MeetingPlatformProvider.kt`

### Server (Backend Proxy)

- `server/src/main/kotlin/com/guyghost/wakeve/routes/MeetingProxyRoutes.kt`
- `server/src/main/kotlin/com/guyghost/wakeve/service/ZoomApiClient.kt`
- `server/src/main/kotlin/com/guyghost/wakeve/service/GoogleMeetApiClient.kt`

### Android

- `wakeveApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/meeting/MeetingListScreen.kt`
- `wakeveApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/meeting/MeetingCreateScreen.kt`

### iOS

- `wakeveApp/wakeveApp/Views/MeetingListView.swift`
- `wakeveApp/wakeveApp/Views/MeetingEditSheet.swift`

## Production Implementation Details

### Zoom OAuth Flow

The Zoom integration uses JWT-based Server-to-Server OAuth:

1. **Token Generation**: Server generates JWT token using API Key and Secret
2. **API Authentication**: JWT token included as Bearer in Authorization header
3. **Token Expiration**: Tokens valid for up to 1 hour
4. **Token Renewal**: Generate new token when current expires

```kotlin
// Production Zoom API call
suspend fun createZoomMeeting(request: CreateZoomMeetingRequest): ZoomMeetingResponse {
    val jwtToken = generateZoomJWT(
        apiKey = System.getenv("ZOOM_API_KEY"),
        apiSecret = System.getenv("ZOOM_API_SECRET")
    )

    val response = httpClient.post("https://api.zoom.us/v2/users/me/meetings") {
        headers {
            append("Authorization", "Bearer $jwtToken")
            append("Content-Type", "application/json")
        }
        setBody(
            buildJsonObject {
                put("topic", request.title)
                put("type", 2) // Scheduled meeting
                put("start_time", request.scheduledFor)
                put("duration", request.duration)
                put("timezone", request.timezone)
                put("password", generateRandomPassword(6))
                putJsonObject("settings") {
                    put("join_before_host", true)
                    put("waiting_room", request.waitingRoom)
                }
            }
        )
    }

    return parseZoomResponse(response)
}
```

### Google Meet API Integration

Google Meet is created via Google Calendar API with conference data:

1. **Service Account Auth**: Authenticate using service account JSON credentials
2. **Calendar Event Creation**: Create event with conferenceData.createRequest
3. **Meet URL Extraction**: Extract hangoutLink from response

```kotlin
// Production Google Meet API call
suspend fun createGoogleMeet(request: CreateGoogleMeetRequest): GoogleMeetResponse {
    val credentials = GoogleCredentials.fromStream(
        System.getenv("GOOGLE_MEET_CREDENTIALS").byteInputStream()
    )
    credentials.refreshIfExpired()
    val accessToken = credentials.accessToken.tokenValue

    val response = httpClient.post("https://www.googleapis.com/calendar/v3/calendars/primary/events") {
        headers {
            append("Authorization", "Bearer $accessToken")
            append("Content-Type", "application/json")
        }
        parameter("conferenceDataVersion", "1")
        setBody(
            buildJsonObject {
                put("summary", request.title)
                put("description", request.description)
                putJsonObject("start") {
                    put("dateTime", request.scheduledFor)
                    put("timeZone", request.timezone)
                }
                putJsonObject("end") {
                    put("dateTime", calculateEndTime(request.scheduledFor, request.duration))
                    put("timeZone", request.timezone)
                }
                putJsonObject("conferenceData") {
                    putJsonObject("createRequest") {
                        put("requestId", UUID.randomUUID().toString())
                        put("conferenceSolutionKey") {
                            put("type", "hangoutsMeet")
                        }
                    }
                }
            }
        )
    }

    return parseGoogleMeetResponse(response)
}
```

### Rate Limiting Implementation

Per-provider rate limiting using token bucket algorithm:

```kotlin
class RateLimiter(
    private val maxRequests: Int,
    private val perTimeWindow: Duration
) {
    private val buckets = ConcurrentHashMap<String, TokenBucket>()

    fun checkLimit(userId: String, platform: MeetingPlatform): Boolean {
        val key = "$userId:${platform.name}"
        val bucket = buckets.getOrPut(key) { TokenBucket(maxRequests, perTimeWindow) }
        return bucket.tryConsume()
    }
}

// Usage in route
post("/zoom/create") {
    val userId = call.getUserId()
    if (!rateLimiter.checkLimit(userId, MeetingPlatform.ZOOM)) {
        return@post call.respond(
            HttpStatusCode.TooManyRequests,
            mapOf("error" to "rate_limit_exceeded")
        )
    }
    // ... proceed with request
}
```

### Meeting Lifecycle State Transitions

```
     ┌─────────────┐
     │  SCHEDULED  │
     └──────┬──────┘
            │
     ┌──────▼──────┐
     │   STARTED   │
     └──────┬──────┘
            │
     ┌──────▼──────┐
     │    ENDED    │
     └─────────────┘

     ┌─────────────┐
     │  SCHEDULED  │
     └──────┬──────┘
            │
     ┌──────▼──────┐
     │  CANCELLED  │
     └─────────────┘
```

## Related Specifications

- `event-organization`: Main event management
- `calendar-management`: Calendar integration
- `collaboration-management`: Participant and notification management
- `notification-management`: Push notification service

## Internationalization

### User-Facing Strings

| Key | English | French | Context |
|-----|---------|--------|---------|
| `meeting.create.title` | Create Meeting | Créer une réunion | Screen title |
| `meeting.platform.zoom` | Zoom | Zoom | Platform option |
| `meeting.platform.google` | Google Meet | Google Meet | Platform option |
| `meeting.platform.facetime` | FaceTime | FaceTime | Platform option |
| `meeting.reminder.one_day` | 1 day before | 1 jour avant | Reminder timing |
| `meeting.reminder.one_hour` | 1 hour before | 1 heure avant | Reminder timing |
| `meeting.error.zoom_not_configured` | Zoom is not configured | Zoom n'est pas configuré | Error message |

## Performance Considerations

- **Caching**: Cache meeting status for 5 minutes to reduce API calls
- **Database Indexing**: Index on eventId, organizerId, status, scheduledFor
- **Rate Limiting**: 100 requests per hour per user per platform
- **Async Operations**: All external API calls are non-blocking
- **Timeouts**: 30 second timeout for external API calls

## Change History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2025-12-26 | Initial version (French) |
| 2.0.0 | 2026-02-08 | Production implementation details, security model, rate limiting |

## Acceptance Criteria

- [x] Meeting creation via backend proxy
- [x] Zoom API integration (mock)
- [x] Google Meet API integration (mock)
- [x] Rate limiting structure defined
- [ ] Production Zoom API calls
- [ ] Production Google Meet API calls
- [ ] Per-user rate limiting enforcement
- [ ] OAuth2 authentication
- [ ] Calendar sync on all platforms
- [ ] Complete test coverage

## Success Metrics

- API key security: 0 credential leaks
- Meeting creation success rate: >99%
- API response time: <500ms p95
- Rate limiting effectiveness: 0 API bans
- User satisfaction: >4.5/5

---

**Spec Version**: 2.0.0
**Last Updated**: 2026-02-08
**Status**: Active
**Maintainer**: Wakeve Team
