# Wakeve API Documentation - Production Ready

> **Version:** v1  
> **Last Updated:** 2026-02-11  
> **Status:** ✅ Production Ready

---

## 📋 Table of Contents

1. [Overview](#1-overview)
2. [Authentication Endpoints](#2-authentication-endpoints)
3. [Event Endpoints](#3-event-endpoints)
4. [Poll Endpoints](#4-poll-endpoints)
5. [Scenario Endpoints](#5-scenario-endpoints)
6. [Meeting Endpoints](#6-meeting-endpoints)
7. [Notification Endpoints](#7-notification-endpoints)
8. [Calendar Endpoints](#8-calendar-endpoints)
9. [Deep Link Endpoints](#9-deep-link-endpoints)
10. [Data Models](#10-data-models)
11. [Error Codes](#11-error-codes)
12. [Code Examples](#12-code-examples)

---

## 1. Overview

### Base URL

```
https://api.wakeve.com/v1
```

### API Specifications

| Property | Value |
|----------|-------|
| **Version** | v1 |
| **Format** | JSON |
| **Encoding** | UTF-8 |
| **Content-Type** | `application/json` |
| **Authentication** | Bearer JWT Token |
| **Rate Limiting** | 100 requests/minute per user |

### Request Headers

All API requests must include the following headers:

```http
Content-Type: application/json
Authorization: Bearer {access_token}
X-Request-ID: {uuid}          # Optional: For request tracing
Accept-Language: fr,en        # Optional: Preferred language
```

### Response Format

All responses follow a consistent envelope structure:

```json
{
  "success": true,
  "data": { ... },
  "meta": {
    "timestamp": "2026-02-11T17:30:00Z",
    "requestId": "req-uuid-123"
  }
}
```

---

## 2. Authentication Endpoints

### 2.1 POST /auth/login

Login with email and password.

**Request:**
```json
{
  "email": "user@example.com",
  "password": "securePassword123"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "user": {
      "id": "user_123",
      "email": "user@example.com",
      "name": "John Doe",
      "isGuest": false,
      "authMethod": "EMAIL",
      "createdAt": 1704067200000,
      "lastLoginAt": 1706745600000
    },
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 3600
  },
  "meta": {
    "timestamp": "2026-02-11T17:30:00Z"
  }
}
```

---

### 2.2 POST /auth/oauth/google

Authenticate with Google OAuth.

**Request:**
```json
{
  "idToken": "google_id_token_from_sdk",
  "email": "user@gmail.com",
  "name": "John Doe"
}
```

**Response (200 OK):** Same as `/auth/login`

---

### 2.3 POST /auth/oauth/apple

Authenticate with Apple Sign-In.

**Request:**
```json
{
  "idToken": "apple_identity_token",
  "email": "user@icloud.com",
  "name": "John Doe",
  "authorizationCode": "auth_code_optional"
}
```

**Response (200 OK):** Same as `/auth/login`

---

### 2.4 POST /auth/refresh

Refresh an expired access token.

**Request:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 3600
  }
}
```

---

### 2.5 POST /auth/logout

Logout and invalidate tokens.

**Request:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "message": "Logged out successfully"
  }
}
```

---

### 2.6 POST /auth/guest

Create a guest session for offline use.

**Request:**
```json
{
  "deviceId": "device_uuid_123"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "user": {
      "id": "guest_123",
      "email": null,
      "name": null,
      "isGuest": true,
      "authMethod": "GUEST",
      "createdAt": 1704067200000,
      "lastLoginAt": 1706745600000
    },
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 604800
  }
}
```

**Note:** Guest tokens have 7-day expiry and cannot sync data to backend.

---

### 2.7 POST /auth/email/request-otp

Request OTP for email authentication.

**Request:**
```json
{
  "email": "user@example.com"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "message": "OTP sent successfully",
    "expiresIn": 300
  }
}
```

---

### 2.8 POST /auth/email/verify-otp

Verify OTP and authenticate.

**Request:**
```json
{
  "email": "user@example.com",
  "otp": "123456"
}
```

**Response (200 OK):** Same as `/auth/login`

---

## 3. Event Endpoints

### 3.1 GET /events

List all events for the authenticated user.

**Query Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `status` | string | Filter by status (DRAFT, POLLING, CONFIRMED, etc.) |
| `page` | integer | Page number (default: 1) |
| `limit` | integer | Items per page (default: 20, max: 100) |
| `sort` | string | Sort field: `createdAt`, `updatedAt`, `deadline` |
| `order` | string | Sort order: `asc`, `desc` (default: desc) |

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "events": [
      {
        "id": "event-123",
        "title": "Team Building Retreat",
        "description": "Annual team building event",
        "organizerId": "user-123",
        "participants": ["user-456", "user-789"],
        "proposedSlots": [
          {
            "id": "slot-1",
            "start": "2026-06-15T14:00:00Z",
            "end": "2026-06-15T18:00:00Z",
            "timezone": "Europe/Paris",
            "timeOfDay": "SPECIFIC"
          }
        ],
        "deadline": "2026-05-01T23:59:59Z",
        "status": "POLLING",
        "finalDate": null,
        "createdAt": "2026-02-01T10:00:00Z",
        "updatedAt": "2026-02-10T15:30:00Z",
        "eventType": "TEAM_BUILDING",
        "eventTypeCustom": null,
        "minParticipants": 15,
        "maxParticipants": 25,
        "expectedParticipants": 20,
        "heroImageUrl": "https://cdn.wakeve.com/events/hero-123.jpg"
      }
    ],
    "pagination": {
      "page": 1,
      "limit": 20,
      "total": 45,
      "totalPages": 3,
      "hasNext": true,
      "hasPrev": false
    }
  }
}
```

---

### 3.2 GET /events/{id}

Get detailed information about a specific event.

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | string | Event ID |

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "id": "event-123",
    "title": "Team Building Retreat",
    "description": "Annual team building event",
    "organizerId": "user-123",
    "participants": ["user-456", "user-789"],
    "proposedSlots": [
      {
        "id": "slot-1",
        "start": "2026-06-15T14:00:00Z",
        "end": "2026-06-15T18:00:00Z",
        "timezone": "Europe/Paris",
        "timeOfDay": "SPECIFIC"
      },
      {
        "id": "slot-2",
        "start": null,
        "end": null,
        "timezone": "Europe/Paris",
        "timeOfDay": "AFTERNOON"
      }
    ],
    "deadline": "2026-05-01T23:59:59Z",
    "status": "POLLING",
    "finalDate": null,
    "createdAt": "2026-02-01T10:00:00Z",
    "updatedAt": "2026-02-10T15:30:00Z",
    "eventType": "TEAM_BUILDING",
    "eventTypeCustom": null,
    "minParticipants": 15,
    "maxParticipants": 25,
    "expectedParticipants": 20,
    "heroImageUrl": null,
    "potentialLocations": [
      {
        "id": "loc-1",
        "eventId": "event-123",
        "name": "Paris",
        "locationType": "CITY",
        "address": null,
        "coordinates": null,
        "createdAt": "2026-02-01T10:05:00Z"
      }
    ]
  }
}
```

---

### 3.3 POST /events

Create a new event.

**Request:**
```json
{
  "title": "Séminaire d'équipe",
  "description": "Annual team building event",
  "eventType": "TEAM_BUILDING",
  "eventTypeCustom": null,
  "expectedParticipants": 20,
  "minParticipants": 15,
  "maxParticipants": 25,
  "proposedSlots": [
    {
      "start": "2026-06-15T14:00:00Z",
      "end": "2026-06-15T18:00:00Z",
      "timezone": "Europe/Paris",
      "timeOfDay": "SPECIFIC"
    },
    {
      "start": null,
      "end": null,
      "timezone": "Europe/Paris",
      "timeOfDay": "AFTERNOON"
    }
  ],
  "deadline": "2026-05-01T23:59:59Z",
  "potentialLocations": [
    {
      "name": "Paris",
      "locationType": "CITY",
      "address": null,
      "coordinates": null
    }
  ]
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "id": "event-123",
    "title": "Séminaire d'équipe",
    "description": "Annual team building event",
    "organizerId": "user-123",
    "participants": [],
    "proposedSlots": [
      {
        "id": "slot-1",
        "start": "2026-06-15T14:00:00Z",
        "end": "2026-06-15T18:00:00Z",
        "timezone": "Europe/Paris",
        "timeOfDay": "SPECIFIC"
      }
    ],
    "deadline": "2026-05-01T23:59:59Z",
    "status": "DRAFT",
    "finalDate": null,
    "createdAt": "2026-02-11T17:30:00Z",
    "updatedAt": "2026-02-11T17:30:00Z",
    "eventType": "TEAM_BUILDING",
    "eventTypeCustom": null,
    "minParticipants": 15,
    "maxParticipants": 25,
    "expectedParticipants": 20,
    "heroImageUrl": null
  }
}
```

---

### 3.4 PUT /events/{id}

Update an existing event. Only allowed when event is in DRAFT status.

**Request:**
```json
{
  "title": "Updated Event Title",
  "description": "Updated description",
  "eventType": "CONFERENCE",
  "expectedParticipants": 30
}
```

**Response (200 OK):** Updated event object

---

### 3.5 DELETE /events/{id}

Delete an event. Only allowed for organizer when event is in DRAFT or FINALIZED status.

**Response (204 No Content):** Empty body

---

### 3.6 POST /events/{id}/start-poll

Transition event from DRAFT to POLLING status.

**Request:**
```json
{
  "notifyParticipants": true
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "eventId": "event-123",
    "status": "POLLING",
    "message": "Poll started successfully"
  }
}
```

---

### 3.7 POST /events/{id}/confirm-date

Confirm the final date for an event. Transitions from POLLING to CONFIRMED.

**Request:**
```json
{
  "slotId": "slot-1",
  "notifyParticipants": true
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "eventId": "event-123",
    "status": "CONFIRMED",
    "finalDate": "2026-06-15T14:00:00Z",
    "scenariosUnlocked": true,
    "message": "Date confirmed successfully"
  }
}
```

---

### 3.8 POST /events/{id}/transition-organizing

Transition event from CONFIRMED to ORGANIZING status.

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "eventId": "event-123",
    "status": "ORGANIZING",
    "meetingsUnlocked": true,
    "message": "Event moved to organizing phase"
  }
}
```

---

### 3.9 POST /events/{id}/finalize

Finalize the event. Transitions from ORGANIZING to FINALIZED.

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "eventId": "event-123",
    "status": "FINALIZED",
    "finalizedAt": "2026-02-11T17:30:00Z",
    "message": "Event finalized successfully"
  }
}
```

---

## 4. Poll Endpoints

### 4.1 GET /events/{id}/poll

Get the poll information and current votes for an event.

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "id": "poll-123",
    "eventId": "event-123",
    "votes": {
      "slot-1": {
        "user-456": "YES",
        "user-789": "MAYBE"
      },
      "slot-2": {
        "user-456": "NO",
        "user-789": "YES"
      }
    },
    "deadline": "2026-05-01T23:59:59Z",
    "isClosed": false
  }
}
```

---

### 4.2 POST /events/{id}/poll/votes

Submit or update votes for time slots.

**Request:**
```json
{
  "votes": [
    {
      "slotId": "slot-1",
      "vote": "YES"
    },
    {
      "slotId": "slot-2",
      "vote": "MAYBE"
    },
    {
      "slotId": "slot-3",
      "vote": "NO"
    }
  ]
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "message": "Votes submitted successfully",
    "updatedVotes": {
      "slot-1": "YES",
      "slot-2": "MAYBE",
      "slot-3": "NO"
    }
  }
}
```

---

### 4.3 GET /events/{id}/poll/results

Get aggregated poll results with scores.

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "eventId": "event-123",
    "totalParticipants": 5,
    "totalVotes": 12,
    "results": [
      {
        "slotId": "slot-1",
        "slot": {
          "id": "slot-1",
          "start": "2026-06-15T14:00:00Z",
          "end": "2026-06-15T18:00:00Z",
          "timezone": "Europe/Paris",
          "timeOfDay": "SPECIFIC"
        },
        "yesCount": 3,
        "maybeCount": 1,
        "noCount": 1,
        "score": 6,
        "participationRate": 100
      },
      {
        "slotId": "slot-2",
        "slot": { ... },
        "yesCount": 2,
        "maybeCount": 2,
        "noCount": 1,
        "score": 5,
        "participationRate": 100
      }
    ],
    "recommendedSlotId": "slot-1",
    "isClosed": false
  }
}
```

**Scoring Algorithm:**
- YES = +2 points
- MAYBE = +1 point
- NO = -1 point

---

## 5. Scenario Endpoints

### 5.1 GET /events/{id}/scenarios

List all scenarios for an event.

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "scenarios": [
      {
        "scenario": {
          "id": "scenario-1",
          "eventId": "event-123",
          "name": "Option A - Paris Weekend",
          "dateOrPeriod": "2026-06-15",
          "location": "Paris, France",
          "duration": 2,
          "estimatedParticipants": 20,
          "estimatedBudgetPerPerson": 250.00,
          "description": "Weekend team building in Paris",
          "status": "PROPOSED",
          "createdAt": "2026-02-05T10:00:00Z",
          "updatedAt": "2026-02-05T10:00:00Z"
        },
        "votes": [
          {
            "id": "vote-1",
            "scenarioId": "scenario-1",
            "participantId": "user-456",
            "vote": "PREFER",
            "createdAt": "2026-02-06T14:30:00Z"
          }
        ],
        "votingResult": {
          "scenarioId": "scenario-1",
          "preferCount": 3,
          "neutralCount": 1,
          "againstCount": 0,
          "totalVotes": 4,
          "score": 7,
          "preferPercentage": 75.0,
          "neutralPercentage": 25.0,
          "againstPercentage": 0.0
        }
      }
    ]
  }
}
```

---

### 5.2 POST /events/{id}/scenarios

Create a new scenario for an event.

**Request:**
```json
{
  "name": "Option B - Lyon Day Trip",
  "dateOrPeriod": "2026-06-16",
  "location": "Lyon, France",
  "duration": 1,
  "estimatedParticipants": 20,
  "estimatedBudgetPerPerson": 150.00,
  "description": "Day trip to Lyon for gastronomic experience"
}
```

**Response (201 Created):** Created scenario object

---

### 5.3 POST /events/{id}/scenarios/{scenarioId}/vote

Vote on a scenario.

**Request:**
```json
{
  "vote": "PREFER"
}
```

**Vote Types:**
- `PREFER` - Participant prefers this scenario
- `NEUTRAL` - Participant is neutral
- `AGAINST` - Participant is against this scenario

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "voteId": "vote-123",
    "scenarioId": "scenario-1",
    "vote": "PREFER",
    "updatedVotingResult": {
      "scenarioId": "scenario-1",
      "preferCount": 4,
      "neutralCount": 1,
      "againstCount": 0,
      "totalVotes": 5,
      "score": 9
    }
  }
}
```

---

### 5.4 POST /events/{id}/scenarios/{scenarioId}/select

Select a scenario as the final choice.

**Request:**
```json
{
  "notifyParticipants": true
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "scenarioId": "scenario-1",
    "status": "SELECTED",
    "message": "Scenario selected as final"
  }
}
```

---

## 6. Meeting Endpoints

### 6.1 GET /events/{id}/meetings

List all meetings for an event.

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "meetings": [
      {
        "id": "meeting-1",
        "eventId": "event-123",
        "organizerId": "user-123",
        "platform": "ZOOM",
        "meetingId": "1234567890",
        "meetingPassword": "ABC123",
        "meetingUrl": "https://zoom.us/j/1234567890?pwd=ABC123",
        "dialInNumber": "+33 1 23 45 67 89",
        "dialInPassword": "123456",
        "title": "Pre-event Planning Call",
        "description": "Discuss final details",
        "scheduledFor": "2026-02-20T10:00:00Z",
        "duration": "PT60M",
        "timezone": "Europe/Paris",
        "participantLimit": 100,
        "requirePassword": true,
        "waitingRoom": true,
        "hostKey": "123456",
        "createdAt": "2026-02-11T17:30:00Z",
        "status": "SCHEDULED"
      }
    ]
  }
}
```

---

### 6.2 POST /events/{id}/meetings

Create a new virtual meeting.

**Request:**
```json
{
  "platform": "ZOOM",
  "title": "Pre-event Planning Call",
  "description": "Discuss final details",
  "scheduledFor": "2026-02-20T10:00:00Z",
  "duration": "PT60M",
  "timezone": "Europe/Paris",
  "participantLimit": 100,
  "requirePassword": true,
  "waitingRoom": true
}
```

**Platforms:**
- `ZOOM` - Zoom Meeting
- `GOOGLE_MEET` - Google Meet
- `TEAMS` - Microsoft Teams
- `WEBEX` - Cisco Webex
- `FACETIME` - Apple FaceTime (iOS only)

**Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "id": "meeting-1",
    "platform": "ZOOM",
    "meetingId": "1234567890",
    "meetingUrl": "https://zoom.us/j/1234567890?pwd=ABC123",
    "password": "ABC123",
    "hostUrl": "https://zoom.us/j/1234567890?pwd=XYZ789",
    "dialInNumber": "+33 1 23 45 67 89",
    "dialInPassword": "123456",
    "hostKey": "123456"
  }
}
```

---

### 6.3 PUT /events/{id}/meetings/{meetingId}

Update a meeting.

**Request:**
```json
{
  "title": "Updated Meeting Title",
  "scheduledFor": "2026-02-20T11:00:00Z",
  "duration": "PT90M"
}
```

**Response (200 OK):** Updated meeting object

---

### 6.4 DELETE /events/{id}/meetings/{meetingId}

Cancel and delete a meeting.

**Response (204 No Content):** Empty body

---

## 7. Notification Endpoints

### 7.1 GET /notifications

List notifications for the authenticated user.

**Query Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `unreadOnly` | boolean | Show only unread notifications |
| `page` | integer | Page number |
| `limit` | integer | Items per page |

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "notifications": [
      {
        "id": "notif-1",
        "userId": "user-123",
        "type": "DEADLINE_REMINDER",
        "title": "Poll Closing Soon",
        "body": "The poll for 'Team Building Retreat' closes in 24 hours",
        "data": {
          "eventId": "event-123",
          "deepLink": "wakeve://event/event-123/poll"
        },
        "sentAt": "2026-04-30T10:00:00Z",
        "readAt": null
      }
    ],
    "unreadCount": 3,
    "pagination": {
      "page": 1,
      "limit": 20,
      "total": 15
    }
  }
}
```

---

### 7.2 POST /notifications/mark-read

Mark notifications as read.

**Request:**
```json
{
  "notificationIds": ["notif-1", "notif-2"]
}
```

Or mark all as read:
```json
{
  "markAll": true
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "markedAsRead": 2
  }
}
```

---

### 7.3 PUT /notifications/preferences

Update notification preferences.

**Request:**
```json
{
  "emailEnabled": true,
  "pushEnabled": true,
  "deadlineReminders": true,
  "eventUpdates": true,
  "voteReminders": true,
  "participantJoined": false,
  "commentPosted": true,
  "quietHoursStart": "22:00",
  "quietHoursEnd": "08:00",
  "timezone": "Europe/Paris"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "preferences": {
      "emailEnabled": true,
      "pushEnabled": true,
      "deadlineReminders": true,
      "eventUpdates": true,
      "voteReminders": true,
      "participantJoined": false,
      "commentPosted": true,
      "quietHoursStart": "22:00",
      "quietHoursEnd": "08:00",
      "timezone": "Europe/Paris"
    }
  }
}
```

---

### 7.4 POST /notifications/register-token

Register a push notification token.

**Request:**
```json
{
  "token": "fcm_or_apns_token_here",
  "platform": "ios",
  "deviceId": "device_uuid_123"
}
```

**Platforms:** `ios`, `android`

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "message": "Token registered successfully"
  }
}
```

---

## 8. Calendar Endpoints

### 8.1 GET /events/{id}/calendar/ics

Download calendar invitation as ICS file.

**Query Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `slotId` | string | Specific slot ID (optional, uses final date if not provided) |

**Response (200 OK):**
```
Content-Type: text/calendar
Content-Disposition: attachment; filename="event-123.ics"

BEGIN:VCALENDAR
VERSION:2.0
PRODID:-//Wakeve//Event//EN
BEGIN:VEVENT
UID:event-123@wakeve.com
DTSTART:20260615T140000Z
DTEND:20260615T180000Z
SUMMARY:Team Building Retreat
DESCRIPTION:Annual team building event
LOCATION:Paris, France
END:VEVENT
END:VCALENDAR
```

---

### 8.2 POST /events/{id}/calendar/add

Add event to user's calendar (server-side).

**Request:**
```json
{
  "provider": "google",
  "slotId": "slot-1",
  "reminderMinutes": [60, 1440]
}
```

**Providers:** `google`, `outlook`, `apple`

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "calendarEventId": "google_event_123",
    "syncedAt": "2026-02-11T17:30:00Z",
    "message": "Event added to calendar"
  }
}
```

---

## 9. Deep Link Endpoints

### 9.1 GET /deeplink/validate

Validate a deep link URL.

**Query Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `url` | string | Deep link URL to validate |

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "valid": true,
    "route": "event/{eventId}/details",
    "parameters": {
      "eventId": "event-123"
    },
    "deepLinkRoute": "EVENT_DETAILS"
  }
}
```

---

### 9.2 POST /deeplink/generate

Generate a deep link URL.

**Request:**
```json
{
  "route": "event/{eventId}/poll",
  "pathParams": {
    "eventId": "event-123"
  },
  "queryParams": {
    "tab": "votes",
    "utm_source": "email"
  }
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "deepLink": "wakeve://event/event-123/poll?tab=votes&utm_source=email",
    "webFallbackUrl": "https://wakeve.com/event/event-123/poll?tab=votes&utm_source=email"
  }
}
```

---

## 10. Data Models

### 10.1 Event

```typescript
{
  id: string;                          // Unique event ID
  title: string;                       // Event title
  description: string;                 // Event description
  organizerId: string;                 // Organizer user ID
  participants: string[];              // List of participant IDs
  proposedSlots: TimeSlot[];           // Available time slots
  deadline: string;                    // Voting deadline (ISO 8601)
  status: EventStatus;                 // Current event status
  finalDate: string | null;            // Confirmed date (ISO 8601)
  createdAt: string;                   // Creation timestamp (ISO 8601)
  updatedAt: string;                   // Last update timestamp (ISO 8601)
  
  // Enhanced DRAFT phase fields
  eventType: EventType;                // Type of event
  eventTypeCustom: string | null;      // Custom type description (if CUSTOM)
  minParticipants: number | null;      // Minimum participants required
  maxParticipants: number | null;      // Maximum participants allowed
  expectedParticipants: number | null; // Expected number of participants
  heroImageUrl: string | null;         // Hero image URL
}
```

### 10.2 EventStatus

```typescript
enum EventStatus {
  DRAFT = "DRAFT",                     // Event being drafted
  POLLING = "POLLING",                 // Date polling active
  COMPARING = "COMPARING",             // Scenarios being compared
  CONFIRMED = "CONFIRMED",             // Date and scenario confirmed
  ORGANIZING = "ORGANIZING",           // Logistics being organized
  FINALIZED = "FINALIZED"              // All details finalized
}
```

### 10.3 EventType

```typescript
enum EventType {
  BIRTHDAY = "BIRTHDAY",
  WEDDING = "WEDDING",
  TEAM_BUILDING = "TEAM_BUILDING",
  CONFERENCE = "CONFERENCE",
  WORKSHOP = "WORKSHOP",
  PARTY = "PARTY",
  SPORTS_EVENT = "SPORTS_EVENT",
  CULTURAL_EVENT = "CULTURAL_EVENT",
  FAMILY_GATHERING = "FAMILY_GATHERING",
  SPORT_EVENT = "SPORT_EVENT",
  OUTDOOR_ACTIVITY = "OUTDOOR_ACTIVITY",
  FOOD_TASTING = "FOOD_TASTING",
  TECH_MEETUP = "TECH_MEETUP",
  WELLNESS_EVENT = "WELLNESS_EVENT",
  CREATIVE_WORKSHOP = "CREATIVE_WORKSHOP",
  OTHER = "OTHER",
  CUSTOM = "CUSTOM"
}
```

### 10.4 TimeSlot

```typescript
{
  id: string;                          // Unique slot ID
  start: string | null;                // Start time (ISO 8601, null for flexible)
  end: string | null;                  // End time (ISO 8601, null for flexible)
  timezone: string;                    // Timezone identifier (e.g., "Europe/Paris")
  timeOfDay: TimeOfDay;                // Time of day indication
}
```

### 10.5 TimeOfDay

```typescript
enum TimeOfDay {
  ALL_DAY = "ALL_DAY",
  MORNING = "MORNING",
  AFTERNOON = "AFTERNOON",
  EVENING = "EVENING",
  SPECIFIC = "SPECIFIC"                // Requires start/end times
}
```

### 10.6 Vote

```typescript
enum Vote {
  YES = "YES",                         // +2 points in scoring
  MAYBE = "MAYBE",                     // +1 point in scoring
  NO = "NO"                            // -1 point in scoring
}
```

### 10.7 PotentialLocation

```typescript
{
  id: string;                          // Unique location ID
  eventId: string;                     // Parent event ID
  name: string;                        // Location name
  locationType: LocationType;          // Type of location
  address: string | null;              // Full address
  coordinates: Coordinates | null;     // Geographic coordinates
  createdAt: string;                   // Creation timestamp (ISO 8601)
}
```

### 10.8 LocationType

```typescript
enum LocationType {
  CITY = "CITY",
  REGION = "REGION",
  SPECIFIC_VENUE = "SPECIFIC_VENUE",
  ONLINE = "ONLINE"
}
```

### 10.9 Coordinates

```typescript
{
  latitude: number;                    // -90 to 90
  longitude: number;                   // -180 to 180
}
```

### 10.10 User

```typescript
{
  id: string;                          // Unique user ID
  email: string | null;                // Email address (null for guests)
  name: string | null;                 // Display name
  authMethod: AuthMethod;              // Authentication method used
  isGuest: boolean;                    // True if guest session
  createdAt: number;                   // Account creation timestamp (ms)
  lastLoginAt: number;                 // Last login timestamp (ms)
}
```

### 10.11 AuthMethod

```typescript
enum AuthMethod {
  GOOGLE = "GOOGLE",
  APPLE = "APPLE",
  EMAIL = "EMAIL",
  GUEST = "GUEST"
}
```

### 10.12 Scenario

```typescript
{
  id: string;                          // Unique scenario ID
  eventId: string;                     // Parent event ID
  name: string;                        // Scenario name
  dateOrPeriod: string;                // ISO date or period description
  location: string;                    // Location description
  duration: number;                    // Duration in days
  estimatedParticipants: number;       // Estimated participant count
  estimatedBudgetPerPerson: number;    // Budget per person
  description: string;                 // Detailed description
  status: ScenarioStatus;              // Current status
  createdAt: string;                   // Creation timestamp (ISO 8601)
  updatedAt: string;                   // Last update timestamp (ISO 8601)
}
```

### 10.13 ScenarioStatus

```typescript
enum ScenarioStatus {
  PROPOSED = "PROPOSED",
  SELECTED = "SELECTED",
  REJECTED = "REJECTED"
}
```

### 10.14 ScenarioVote

```typescript
{
  id: string;                          // Unique vote ID
  scenarioId: string;                  // Target scenario ID
  participantId: string;               // Voter ID
  vote: ScenarioVoteType;              // Vote type
  createdAt: string;                   // Vote timestamp (ISO 8601)
}
```

### 10.15 ScenarioVoteType

```typescript
enum ScenarioVoteType {
  PREFER = "PREFER",                   // +2 points
  NEUTRAL = "NEUTRAL",                 // +1 point
  AGAINST = "AGAINST"                  // -1 point
}
```

### 10.16 VirtualMeeting

```typescript
{
  id: string;                          // Unique meeting ID
  eventId: string;                     // Parent event ID
  organizerId: string;                 // Organizer user ID
  platform: MeetingPlatform;           // Meeting platform
  meetingId: string;                   // Platform meeting ID
  meetingPassword: string | null;      // Meeting password
  meetingUrl: string;                  // Join URL
  dialInNumber: string | null;         // Phone dial-in number
  dialInPassword: string | null;       // Dial-in password
  title: string;                       // Meeting title
  description: string | null;          // Meeting description
  scheduledFor: string;                // Scheduled time (ISO 8601)
  duration: string;                    // ISO 8601 duration (e.g., "PT60M")
  timezone: string;                    // Timezone identifier
  participantLimit: number | null;     // Max participants
  requirePassword: boolean;            // Password required
  waitingRoom: boolean;                // Waiting room enabled
  hostKey: string | null;              // Host key
  createdAt: string;                   // Creation timestamp (ISO 8601)
  status: MeetingStatus;               // Current status
}
```

### 10.17 MeetingPlatform

```typescript
enum MeetingPlatform {
  ZOOM = "ZOOM",
  GOOGLE_MEET = "GOOGLE_MEET",
  FACETIME = "FACETIME",
  TEAMS = "TEAMS",
  WEBEX = "WEBEX"
}
```

### 10.18 MeetingStatus

```typescript
enum MeetingStatus {
  SCHEDULED = "SCHEDULED",
  STARTED = "STARTED",
  ENDED = "ENDED",
  CANCELLED = "CANCELLED"
}
```

### 10.19 NotificationMessage

```typescript
{
  id: string;                          // Unique notification ID
  userId: string;                      // Recipient user ID
  type: NotificationType;              // Notification type
  title: string;                       // Notification title
  body: string;                        // Notification body
  data: Record<string, string>;        // Additional data
  sentAt: string | null;               // Sent timestamp (ISO 8601)
  readAt: string | null;               // Read timestamp (ISO 8601)
}
```

### 10.20 NotificationType

```typescript
enum NotificationType {
  DEADLINE_REMINDER = "DEADLINE_REMINDER",
  EVENT_UPDATE = "EVENT_UPDATE",
  VOTE_CLOSE_REMINDER = "VOTE_CLOSE_REMINDER",
  EVENT_CONFIRMED = "EVENT_CONFIRMED",
  PARTICIPANT_JOINED = "PARTICIPANT_JOINED",
  VOTE_SUBMITTED = "VOTE_SUBMITTED",
  COMMENT_POSTED = "COMMENT_POSTED",
  COMMENT_REPLY = "COMMENT_REPLY",
  MENTION = "MENTION"
}
```

### 10.21 DeepLink

```typescript
{
  route: string;                       // Route path (e.g., "event/123/details")
  parameters: Record<string, string>;  // Query parameters
  fullUri: string;                     // Complete URI
}
```

### 10.22 DeepLinkRoute

```typescript
enum DeepLinkRoute {
  EVENT_DETAILS = "event/{eventId}/details",
  EVENT_POLL = "event/{eventId}/poll",
  EVENT_SCENARIOS = "event/{eventId}/scenarios",
  EVENT_MEETINGS = "event/{eventId}/meetings",
  PROFILE = "profile",
  SETTINGS = "settings",
  NOTIFICATIONS = "notifications",
  HOME = "home"
}
```

---

## 11. Error Codes

### HTTP Status Codes

| Status | Code | Description |
|--------|------|-------------|
| 200 | OK | Request successful |
| 201 | Created | Resource created successfully |
| 204 | No Content | Resource deleted successfully |
| 400 | Bad Request | Invalid request parameters |
| 401 | Unauthorized | Authentication required or invalid token |
| 403 | Forbidden | Action not allowed for current user |
| 404 | Not Found | Resource not found |
| 409 | Conflict | Resource conflict (e.g., duplicate) |
| 422 | Unprocessable Entity | Validation error |
| 429 | Too Many Requests | Rate limit exceeded |
| 500 | Internal Server Error | Unexpected server error |
| 503 | Service Unavailable | External service unavailable |

### Error Response Format

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid request parameters",
    "details": {
      "field": "email",
      "reason": "Invalid email format"
    }
  },
  "meta": {
    "timestamp": "2026-02-11T17:30:00Z",
    "requestId": "req-uuid-123"
  }
}
```

### Common Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `VALIDATION_ERROR` | 400 | Invalid input data |
| `INVALID_TOKEN` | 401 | OAuth token validation failed |
| `TOKEN_EXPIRED` | 401 | Token has expired |
| `UNAUTHORIZED` | 401 | Authentication required |
| `FORBIDDEN` | 403 | Action not allowed |
| `NOT_FOUND` | 404 | Resource not found |
| `EVENT_NOT_FOUND` | 404 | Event not found |
| `USER_NOT_FOUND` | 404 | User not found |
| `ALREADY_EXISTS` | 409 | Resource already exists |
| `INVALID_STATUS` | 422 | Invalid status transition |
| `POLL_CLOSED` | 422 | Poll is already closed |
| `INVALID_VOTE` | 422 | Invalid vote value |
| `RATE_LIMIT_EXCEEDED` | 429 | Too many requests |
| `INTERNAL_ERROR` | 500 | Server error |
| `SERVICE_UNAVAILABLE` | 503 | External service down |
| `ZOOM_NOT_CONFIGURED` | 503 | Zoom API not configured |
| `GOOGLE_MEET_NOT_CONFIGURED` | 503 | Google Meet API not configured |

---

## 12. Code Examples

### 12.1 cURL Examples

#### Authentication
```bash
# Login with email
curl -X POST https://api.wakeve.com/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "securePassword123"
  }'

# Google OAuth
curl -X POST https://api.wakeve.com/v1/auth/oauth/google \
  -H "Content-Type: application/json" \
  -d '{
    "idToken": "google_id_token",
    "email": "user@gmail.com",
    "name": "John Doe"
  }'

# Refresh token
curl -X POST https://api.wakeve.com/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
  }'
```

#### Events
```bash
# List events with pagination
curl -X GET "https://api.wakeve.com/v1/events?page=1&limit=20&status=POLLING" \
  -H "Authorization: Bearer {access_token}"

# Get event details
curl -X GET https://api.wakeve.com/v1/events/event-123 \
  -H "Authorization: Bearer {access_token}"

# Create event
curl -X POST https://api.wakeve.com/v1/events \
  -H "Authorization: Bearer {access_token}" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Team Building Retreat",
    "description": "Annual team event",
    "eventType": "TEAM_BUILDING",
    "expectedParticipants": 20,
    "proposedSlots": [
      {
        "start": "2026-06-15T14:00:00Z",
        "end": "2026-06-15T18:00:00Z",
        "timezone": "Europe/Paris",
        "timeOfDay": "SPECIFIC"
      }
    ],
    "deadline": "2026-05-01T23:59:59Z"
  }'

# Start poll
curl -X POST https://api.wakeve.com/v1/events/event-123/start-poll \
  -H "Authorization: Bearer {access_token}" \
  -H "Content-Type: application/json" \
  -d '{"notifyParticipants": true}'

# Confirm date
curl -X POST https://api.wakeve.com/v1/events/event-123/confirm-date \
  -H "Authorization: Bearer {access_token}" \
  -H "Content-Type: application/json" \
  -d '{"slotId": "slot-1", "notifyParticipants": true}'
```

#### Polls
```bash
# Submit votes
curl -X POST https://api.wakeve.com/v1/events/event-123/poll/votes \
  -H "Authorization: Bearer {access_token}" \
  -H "Content-Type: application/json" \
  -d '{
    "votes": [
      {"slotId": "slot-1", "vote": "YES"},
      {"slotId": "slot-2", "vote": "MAYBE"}
    ]
  }'

# Get poll results
curl -X GET https://api.wakeve.com/v1/events/event-123/poll/results \
  -H "Authorization: Bearer {access_token}"
```

#### Scenarios
```bash
# Create scenario
curl -X POST https://api.wakeve.com/v1/events/event-123/scenarios \
  -H "Authorization: Bearer {access_token}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Paris Weekend",
    "dateOrPeriod": "2026-06-15",
    "location": "Paris, France",
    "duration": 2,
    "estimatedParticipants": 20,
    "estimatedBudgetPerPerson": 250.00,
    "description": "Weekend team building"
  }'

# Vote on scenario
curl -X POST https://api.wakeve.com/v1/events/event-123/scenarios/scenario-1/vote \
  -H "Authorization: Bearer {access_token}" \
  -H "Content-Type: application/json" \
  -d '{"vote": "PREFER"}'
```

#### Meetings
```bash
# Create Zoom meeting
curl -X POST https://api.wakeve.com/v1/events/event-123/meetings \
  -H "Authorization: Bearer {access_token}" \
  -H "Content-Type: application/json" \
  -d '{
    "platform": "ZOOM",
    "title": "Planning Call",
    "scheduledFor": "2026-02-20T10:00:00Z",
    "duration": "PT60M",
    "timezone": "Europe/Paris"
  }'
```

### 12.2 Kotlin Example

```kotlin
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class WakeveApiClient(
    private val baseUrl: String = "https://api.wakeve.com/v1",
    private var accessToken: String? = null
) {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    fun setToken(token: String) {
        accessToken = token
    }

    suspend fun login(email: String, password: String): AuthResponse {
        val response = client.post("$baseUrl/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email, password))
        }
        val auth = response.body<AuthResponse>()
        setToken(auth.data.accessToken)
        return auth
    }

    suspend fun getEvents(status: EventStatus? = null): EventsResponse {
        return client.get("$baseUrl/events") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            status?.let { parameter("status", it.name) }
        }.body()
    }

    suspend fun createEvent(request: CreateEventRequest): EventResponse {
        return client.post("$baseUrl/events") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun submitVotes(eventId: String, votes: List<VoteSubmission>) {
        client.post("$baseUrl/events/$eventId/poll/votes") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            contentType(ContentType.Application.Json)
            setBody(VoteRequest(votes))
        }
    }

    suspend fun createMeeting(
        eventId: String,
        platform: MeetingPlatform,
        title: String,
        scheduledFor: Instant,
        duration: Duration
    ): MeetingResponse {
        return client.post("$baseUrl/events/$eventId/meetings") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            contentType(ContentType.Application.Json)
            setBody(CreateMeetingRequest(
                platform = platform,
                title = title,
                scheduledFor = scheduledFor,
                duration = duration
            ))
        }.body()
    }
}

// Usage
suspend fun main() {
    val api = WakeveApiClient()
    
    // Login
    val auth = api.login("user@example.com", "password")
    println("Logged in as: ${auth.data.user.name}")
    
    // Get events
    val events = api.getEvents(EventStatus.POLLING)
    println("Found ${events.data.events.size} polling events")
    
    // Create meeting
    val meeting = api.createMeeting(
        eventId = "event-123",
        platform = MeetingPlatform.ZOOM,
        title = "Planning Call",
        scheduledFor = Clock.System.now(),
        duration = 60.minutes
    )
    println("Meeting URL: ${meeting.data.meetingUrl}")
}
```

### 12.3 Swift Example

```swift
import Foundation

class WakeveAPIClient {
    private let baseURL = "https://api.wakeve.com/v1"
    private var accessToken: String?
    
    func setToken(_ token: String) {
        accessToken = token
    }
    
    private func request(
        path: String,
        method: String = "GET",
        body: Data? = nil
    ) async throws -> Data {
        guard let url = URL(string: baseURL + path) else {
            throw APIError.invalidURL
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = method
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        if let token = accessToken {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        
        if let body = body {
            request.httpBody = body
        }
        
        let (data, response) = try await URLSession.shared.data(for: request)
        
        guard let httpResponse = response as? HTTPURLResponse else {
            throw APIError.invalidResponse
        }
        
        switch httpResponse.statusCode {
        case 200...299:
            return data
        case 401:
            throw APIError.unauthorized
        default:
            throw APIError.httpError(httpResponse.statusCode)
        }
    }
    
    // MARK: - Authentication
    
    func login(email: String, password: String) async throws -> AuthResponse {
        let body = try JSONEncoder().encode([
            "email": email,
            "password": password
        ])
        let data = try await request(path: "/auth/login", method: "POST", body: body)
        let response = try JSONDecoder().decode(AuthResponse.self, from: data)
        setToken(response.data.accessToken)
        return response
    }
    
    // MARK: - Events
    
    func getEvents(status: EventStatus? = nil) async throws -> EventsResponse {
        var path = "/events"
        if let status = status {
            path += "?status=\(status.rawValue)"
        }
        let data = try await request(path: path)
        return try JSONDecoder().decode(EventsResponse.self, from: data)
    }
    
    func createEvent(title: String, description: String, eventType: EventType) async throws -> Event {
        let body = try JSONEncoder().encode(CreateEventRequest(
            title: title,
            description: description,
            eventType: eventType
        ))
        let data = try await request(path: "/events", method: "POST", body: body)
        return try JSONDecoder().decode(EventResponse.self, from: data).data
    }
    
    func confirmDate(eventId: String, slotId: String) async throws {
        let body = try JSONEncoder().encode(["slotId": slotId])
        _ = try await request(
            path: "/events/\(eventId)/confirm-date",
            method: "POST",
            body: body
        )
    }
    
    // MARK: - Polls
    
    func submitVotes(eventId: String, votes: [VoteSubmission]) async throws {
        let body = try JSONEncoder().encode(["votes": votes])
        _ = try await request(
            path: "/events/\(eventId)/poll/votes",
            method: "POST",
            body: body
        )
    }
    
    // MARK: - Meetings
    
    func createMeeting(
        eventId: String,
        platform: MeetingPlatform,
        title: String,
        scheduledFor: Date
    ) async throws -> VirtualMeeting {
        let body = try JSONEncoder().encode(CreateMeetingRequest(
            platform: platform,
            title: title,
            scheduledFor: scheduledFor
        ))
        let data = try await request(
            path: "/events/\(eventId)/meetings",
            method: "POST",
            body: body
        )
        return try JSONDecoder().decode(MeetingResponse.self, from: data).data
    }
}

// Usage
Task {
    let api = WakeveAPIClient()
    
    do {
        // Login
        let auth = try await api.login(email: "user@example.com", password: "password")
        print("Logged in as: \(auth.data.user.name ?? "Unknown")")
        
        // Get events
        let events = try await api.getEvents(status: .polling)
        print("Found \(events.data.events.count) polling events")
        
        // Create meeting
        let meeting = try await api.createMeeting(
            eventId: "event-123",
            platform: .zoom,
            title: "Planning Call",
            scheduledFor: Date()
        )
        print("Meeting URL: \(meeting.meetingUrl)")
        
    } catch {
        print("Error: \(error)")
    }
}
```

---

## Appendix A: Rate Limiting Headers

All API responses include rate limiting headers:

```http
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1706749200
X-RateLimit-Retry-After: 60
```

When rate limit is exceeded:
```json
{
  "success": false,
  "error": {
    "code": "RATE_LIMIT_EXCEEDED",
    "message": "Rate limit exceeded. Please retry after 60 seconds.",
    "retryAfter": 60
  }
}
```

---

## Appendix B: Changelog

| Version | Date | Changes |
|---------|------|---------|
| v1.0.0 | 2026-02-11 | Initial production release |
| v1.0.0-beta | 2026-01-15 | Beta release with full feature set |

---

## Appendix C: Support

For API support:
- **Documentation:** https://docs.wakeve.com
- **Support Email:** api-support@wakeve.com
- **Status Page:** https://status.wakeve.com

---

*This documentation is maintained by the Wakeve Engineering Team. Last updated: 2026-02-11*
