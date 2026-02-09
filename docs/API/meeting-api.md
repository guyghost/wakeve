# Meeting API Documentation

**Backend Proxy API for Secure Meeting Creation**

## Overview

The Meeting API provides secure endpoints for creating virtual meetings on external platforms (Zoom, Google Meet). These endpoints act as a **proxy** to hide API keys from clients, ensuring security by handling all external API authentication server-side.

**Base URL:** `http://localhost:8080`

**Content-Type:** `application/json`

**Authentication:** Currently uses static user IDs (Phase 2). OAuth2 authentication planned (Phase 3).

---

## Security Model

### Why a Proxy?

The Meeting API uses a proxy pattern to secure external platform credentials:

```
Client              Wakeve Server           External Platform
  │                      │                        │
  ├─POST /proxy/zoom/create  │                        │
  │  {title, date, ...}      │                        │
  │──────────────────────→      │                        │
  │                      ├─Add API Key              │
  │                      ├─POST /meetings           │
  │                      │  (Zoom API)               │
  │                      │──────────────────────────→   │
  │                      │←{meetingId, url, pwd}     │
  │                      │←───────────────────────────   │
  │                      ├─Strip sensitive data      │
  │←{meetingId, url, pwd}  │                        │
  │──────────────────────┤                        │
```

### Benefits

1. **API Key Protection**: Zoom and Google Meet credentials stored in server environment variables, never exposed to clients
2. **Rate Limiting**: Server can implement per-user/per-IP rate limiting
3. **Audit Logging**: All meeting creation events logged server-side for compliance
4. **CORS Control**: Server controls which domains can access the proxy
5. **Centralized Management**: Rotate API keys in one place without deploying client updates

---

## Endpoints

### 1. Create Zoom Meeting

**Endpoint:** `POST /api/meetings/proxy/zoom/create`

**Description:** Creates a new Zoom meeting with the specified parameters. The server authenticates with the Zoom API using stored credentials and returns meeting details to the client.

**Headers:**
```
Content-Type: application/json
```

**Request Body:**
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

**Request Fields:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `title` | string | ✅ Yes | Meeting title (must not be blank) |
| `description` | string | ❌ No | Optional meeting description |
| `scheduledFor` | string | ✅ Yes | ISO-8601 datetime (UTC preferred) |
| `duration` | integer | ✅ Yes | Duration in minutes |
| `timezone` | string | ❌ No | Timezone identifier (default: "UTC") |
| `participantLimit` | integer | ❌ No | Maximum participants (optional) |
| `requirePassword` | boolean | ❌ No | Enable password protection (default: true) |
| `waitingRoom` | boolean | ❌ No | Enable waiting room (default: true) |

**Response (200 OK):**
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

**Response Fields:**

| Field | Type | Description |
|-------|------|-------------|
| `meetingId` | string | 10-digit Zoom meeting ID |
| `joinUrl` | string | URL for participants to join |
| `password` | string | Meeting password (if enabled) |
| `hostUrl` | string | URL for host to start meeting |
| `hostKey` | string | 6-digit host key for in-meeting controls |
| `dialInNumber` | string | Phone number for dial-in access |
| `dialInPassword` | string | Dial-in password |

**Error Responses:**

| Status | Error | Description |
|--------|--------|-------------|
| 400 | `"title is required"` | Title field is blank |
| 503 | `"zoom_not_configured"` | Zoom API credentials not configured on server |
| 500 | Internal Server Error | Unexpected server error |

**Example cURL:**
```bash
curl -X POST http://localhost:8080/api/meetings/proxy/zoom/create \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Team Planning Session",
    "description": "Q4 2026 planning meeting",
    "scheduledFor": "2026-02-15T14:00:00Z",
    "duration": 60,
    "timezone": "Europe/Paris",
    "participantLimit": 100,
    "requirePassword": true,
    "waitingRoom": true
  }'
```

**Response:**
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

---

### 2. Cancel Zoom Meeting

**Endpoint:** `POST /api/meetings/proxy/zoom/{meetingId}/cancel`

**Description:** Cancels an existing Zoom meeting. This sends a DELETE request to the Zoom API to end the meeting and prevent future access.

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `meetingId` | string | ✅ Yes | The 10-digit Zoom meeting ID to cancel |

**Headers:**
```
Content-Type: application/json
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Meeting 1234567890 cancelled successfully"
}
```

**Error Responses:**

| Status | Error | Description |
|--------|--------|-------------|
| 400 | `"meetingId is required"` | meetingId parameter missing or blank |
| 500 | Internal Server Error | Unexpected server error or Zoom API failure |

**Example cURL:**
```bash
curl -X POST http://localhost:8080/api/meetings/proxy/zoom/1234567890/cancel \
  -H "Content-Type: application/json"
```

**Response:**
```json
{
  "success": true,
  "message": "Meeting 1234567890 cancelled successfully"
}
```

---

### 3. Get Zoom Meeting Status

**Endpoint:** `GET /api/meetings/proxy/zoom/{meetingId}/status`

**Description:** Retrieves the current status of a Zoom meeting (scheduled, started, ended, cancelled). Useful for displaying real-time meeting state in the UI.

**Path Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `meetingId` | string | ✅ Yes | The 10-digit Zoom meeting ID to query |

**Headers:**
```
Content-Type: application/json
```

**Response (200 OK):**
```json
{
  "meetingId": "1234567890",
  "status": "scheduled",
  "startTime": "2026-02-15T14:00:00Z",
  "duration": 60,
  "participantCount": 0
}
```

**Response Fields:**

| Field | Type | Description |
|-------|------|-------------|
| `meetingId` | string | The 10-digit Zoom meeting ID |
| `status` | string | One of: `"scheduled"`, `"started"`, `"ended"`, `"cancelled"` |
| `startTime` | string | ISO-8601 datetime when meeting starts |
| `duration` | integer | Duration in minutes |
| `participantCount` | integer | Number of participants currently in meeting |

**Error Responses:**

| Status | Error | Description |
|--------|--------|-------------|
| 400 | `"meetingId is required"` | meetingId parameter missing or blank |
| 500 | Internal Server Error | Unexpected server error or Zoom API failure |

**Example cURL:**
```bash
curl -X GET http://localhost:8080/api/meetings/proxy/zoom/1234567890/status \
  -H "Content-Type: application/json"
```

**Response:**
```json
{
  "meetingId": "1234567890",
  "status": "scheduled",
  "startTime": "2026-02-15T14:00:00Z",
  "duration": 60,
  "participantCount": 0
}
```

---

### 4. Create Google Meet Meeting

**Endpoint:** `POST /api/meetings/proxy/google-meet/create`

**Description:** Creates a new Google Meet meeting via the Google Calendar API. The server authenticates using service account credentials and returns the meeting code.

**Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "title": "Team Sync",
  "description": "Weekly sync meeting",
  "scheduledFor": "2026-02-15T15:00:00Z",
  "duration": 45,
  "timezone": "Europe/Paris"
}
```

**Request Fields:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `title` | string | ✅ Yes | Meeting title (must not be blank) |
| `description` | string | ❌ No | Optional meeting description |
| `scheduledFor` | string | ✅ Yes | ISO-8601 datetime (UTC preferred) |
| `duration` | integer | ✅ Yes | Duration in minutes |
| `timezone` | string | ❌ No | Timezone identifier (default: "UTC") |

**Response (200 OK):**
```json
{
  "meetingUrl": "https://meet.google.com/abc-def-ghi",
  "meetingCode": "abc-def-ghi"
}
```

**Response Fields:**

| Field | Type | Description |
|-------|------|-------------|
| `meetingUrl` | string | Full Google Meet URL |
| `meetingCode` | string | Meeting code (3-3-4 format) |

**Error Responses:**

| Status | Error | Description |
|--------|--------|-------------|
| 400 | `"title is required"` | Title field is blank |
| 503 | `"google_meet_not_configured"` | Google Meet credentials not configured on server |
| 500 | Internal Server Error | Unexpected server error or Google API failure |

**Example cURL:**
```bash
curl -X POST http://localhost:8080/api/meetings/proxy/google-meet/create \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Team Sync",
    "description": "Weekly sync meeting",
    "scheduledFor": "2026-02-15T15:00:00Z",
    "duration": 45,
    "timezone": "Europe/Paris"
  }'
```

**Response:**
```json
{
  "meetingUrl": "https://meet.google.com/abc-def-ghi",
  "meetingCode": "abc-def-ghi"
}
```

---

## Environment Variables

The following environment variables must be configured on the server:

```bash
# Zoom API credentials
ZOOM_API_KEY=your_zoom_api_key_here
ZOOM_API_SECRET=your_zoom_api_secret_here

# Google Meet credentials (service account JSON)
GOOGLE_MEET_CREDENTIALS='{"type":"service_account","project_id":"...",...}'
```

### Getting Zoom Credentials

1. Go to [Zoom Marketplace](https://marketplace.zoom.us/)
2. Create a JWT-type app
3. Copy API Key and API Secret
4. Set as environment variables

### Getting Google Meet Credentials

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a service account with Calendar API scope
3. Download JSON key file
4. Set contents as `GOOGLE_MEET_CREDENTIALS` environment variable

---

## Mock vs Production

### Current Implementation (Mock)

All endpoints currently return **mock responses** for development and testing:

```kotlin
// In MeetingProxyRoutes.kt
val response = CreateZoomMeetingResponse(
    meetingId = generateZoomMeetingId(),  // Random 10 digits
    joinUrl = generateZoomJoinUrl(title),
    password = generateZoomPassword(),
    // ... other fields
)
```

### Production Implementation

To enable production API calls, replace mock logic with actual API calls:

```kotlin
// Zoom API call (production)
val jwtToken = generateZoomJWT()
val response = httpClient.post("https://api.zoom.us/v2/users/me/meetings") {
    headers {
        append("Authorization", "Bearer $jwtToken")
        append("Content-Type", "application/json")
    }
    setBody(createZoomMeetingRequestJson)
}
```

```kotlin
// Google Meet API call (production)
val response = httpClient.post("https://www.googleapis.com/calendar/v3/calendars/primary/events") {
    headers {
        append("Authorization", "Bearer $googleAccessToken")
        append("Content-Type", "application/json")
    }
    setBody(createGoogleMeetEventJson)
}
```

---

## Platform-Specific Behavior

### Zoom

- **Meeting ID Format:** 10 digits (e.g., `1234567890`)
- **Password:** 6 characters alphanumeric
- **Dial-in:** Available with phone number and dial-in password
- **Host Key:** 6 digits for in-meeting controls
- **Waiting Room:** Optional (default: enabled)
- **Participant Limit:** Optional (default: unlimited)

### Google Meet

- **Meeting Code:** 3-3-4 format (e.g., `abc-def-ghi`)
- **Password:** None (uses Google authentication)
- **Dial-in:** Automatic via Google Meet phone numbers
- **Host Key:** Not applicable (Google authentication)
- **Waiting Room:** Not applicable
- **Participant Limit:** 100 (Google Meet standard limit)

### FaceTime

- **No Backend Endpoint:** FaceTime uses iOS native `facetime://` URL scheme
- **Authentication:** Apple ID
- **Platform-Specific:** Only available on Apple devices
- **Implementation:** See `FaceTimePlatformProvider` in `shared/src/commonMain/kotlin/`

---

## Error Handling

### Common Errors

| Status | Error Code | Description | Resolution |
|--------|-------------|-------------|-------------|
| 400 | `VALIDATION_ERROR` | Invalid request parameters | Check request body format |
| 401 | `UNAUTHORIZED` | Invalid or missing credentials | Configure API keys |
| 404 | `NOT_FOUND` | Meeting not found | Check meeting ID |
| 500 | `INTERNAL_ERROR` | Server error | Check server logs |
| 503 | `SERVICE_UNAVAILABLE` | External platform not configured | Configure environment variables |

### Error Response Format

```json
{
  "error": "error_code",
  "message": "Human-readable error message"
}
```

---

## Testing the API

### Using cURL

```bash
# Create Zoom meeting
curl -X POST http://localhost:8080/api/meetings/proxy/zoom/create \
  -H "Content-Type: application/json" \
  -d '{"title":"Test","scheduledFor":"2026-02-15T14:00:00Z","duration":60}'

# Create Google Meet meeting
curl -X POST http://localhost:8080/api/meetings/proxy/google-meet/create \
  -H "Content-Type: application/json" \
  -d '{"title":"Test","scheduledFor":"2026-02-15T15:00:00Z","duration":45}'

# Cancel meeting
curl -X POST http://localhost:8080/api/meetings/proxy/zoom/1234567890/cancel

# Get meeting status
curl -X GET http://localhost:8080/api/meetings/proxy/zoom/1234567890/status
```

### Using Postman

1. Import the following collection:
2. Set base URL: `http://localhost:8080`
3. Configure environment variables if needed
4. Run requests

### Using the Client Code

```kotlin
// In Android Jetpack Compose
val httpClient = HttpClient()

suspend fun createZoomMeeting(
    title: String,
    scheduledFor: Instant,
    duration: Duration
): ZoomMeetingResponse {
    val response = httpClient.post("http://localhost:8080/api/meetings/proxy/zoom/create") {
        setBody(
            Json.encodeToString(
                CreateZoomMeetingRequest(
                    title = title,
                    scheduledFor = scheduledFor.toString(),
                    duration = duration.inWholeMinutes.toInt(),
                    timezone = "Europe/Paris"
                )
            )
        )
    }
    return Json.decodeFromString<ZoomMeetingResponse>(response.bodyAsText())
}
```

---

## Rate Limiting

### Current Status
No rate limiting in Phase 2 (development).

### Planned (Phase 3)
Implement per-user and per-IP rate limiting:

```kotlin
// Rate limiting configuration
val rateLimiter = RateLimiter(
    maxRequests = 100,      // 100 requests
    perTimeWindow = 1.hours // per hour per user
)

// In Ktor route
routing {
    rateLimiter.limit()
    post("/api/meetings/proxy/zoom/create") {
        // Handle request
    }
}
```

---

## Audit Logging

All meeting creation events are logged server-side for compliance:

```kotlin
// In MeetingProxyRoutes.kt
post("/zoom/create") {
    val request = call.receive<CreateZoomMeetingRequest>()
    val userId = getUserIdFromSession()

    // Log meeting creation
    logger.info("Meeting created", mapOf(
        "userId" to userId,
        "platform" to "zoom",
        "title" to request.title,
        "timestamp" to Clock.System.now()
    ))

    // ... create meeting
}
```

Log format:
```json
{
  "level": "info",
  "message": "Meeting created",
  "userId": "user-123",
  "platform": "zoom",
  "title": "Team Planning",
  "timestamp": "2026-02-07T14:30:00Z"
}
```

---

## CORS Configuration

Server controls which domains can access the meeting proxy:

```kotlin
// In Application.kt
install(CORS) {
    allowHost("https://wakeve.app")
    allowHost("https://staging.wakeve.app")
    allowCredentials = true
    allowHeader(HttpHeaders.ContentType)
    allowMethod(HttpMethod.Post)
    allowMethod(HttpMethod.Get)
}
```

---

## References

- **Meeting Service:** `shared/src/commonMain/kotlin/com/guyghost/wakeve/meeting/MeetingService.kt`
- **Platform Providers:** `shared/src/commonMain/kotlin/com/guyghost/wakeve/meeting/MeetingPlatformProvider.kt`
- **Backend Routes:** `server/src/main/kotlin/com/guyghost/wakeve/routes/MeetingProxyRoutes.kt`
- **Architecture:** `docs/architecture/meeting-service.md`
- **Main API Docs:** `docs/API.md`

---

**Document Version:** 1.0
**Last Updated:** 2026-02-07
**Status:** ✅ Complete
