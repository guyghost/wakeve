# Wakeve - API Documentation

## Base URL

**Production**: `https://api.wakeve.app`  
**Staging**: `https://staging-api.wakeve.app`  
**Local**: `http://localhost:8080`

## Authentication

Currently using **temporary auth** (will be replaced with OAuth2.0 in Phase 3):

```json
{
  "Authorization": "Bearer <temporary_token>"
}
```

## Rate Limiting

- **Limit**: 1000 requests/hour/IP
- **Headers returned**: `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`

---

## Events API

### Get All Events

**Endpoint**: `GET /api/events`

**Query Parameters**:
- `limit` (optional): Number of results (default: 50)
- `offset` (optional): Pagination offset (default: 0)
- `status` (optional): Filter by status (DRAFT, POLLING, COMPARING, CONFIRMED, ORGANIZING, FINALIZED, CANCELLED)

**Response** (200 OK):
```json
{
  "events": [
    {
      "id": "event-1",
      "title": "Summer Trip 2025",
      "description": "Planning our annual summer trip",
      "organizerId": "user-1",
      "status": "ORGANIZING",
      "createdAt": "2025-12-01T10:00:00Z",
      "deadline": "2025-12-15T18:00:00Z",
      "participantCount": 8
    }
  ],
  "total": 45,
  "limit": 50,
  "offset": 0
}
```

### Get Event by ID

**Endpoint**: `GET /api/events/{id}`

**Path Parameters**:
- `id`: Event ID

**Response** (200 OK):
```json
{
  "id": "event-1",
  "title": "Summer Trip 2025",
  "description": "Planning our annual summer trip",
  "organizerId": "user-1",
  "status": "ORGANIZING",
  "createdAt": "2025-12-01T10:00:00Z",
  "deadline": "2025-12-15T18:00:00Z",
  "participantCount": 8
}
```

**Error** (404 Not Found):
```json
{
  "error": "Event not found",
  "code": "EVENT_NOT_FOUND",
  "details": "No event with ID 'invalid-id'"
}
```

### Create Event

**Endpoint**: `POST /api/events`

**Request Body**:
```json
{
  "title": "Summer Trip 2025",
  "description": "Planning our annual summer trip",
  "organizerId": "user-1",
  "deadline": "2025-12-15T18:00:00Z"
}
```

**Response** (201 Created):
```json
{
  "id": "event-1",
  "title": "Summer Trip 2025",
  "description": "Planning our annual summer trip",
  "organizerId": "user-1",
  "status": "POLLING",
  "createdAt": "2025-12-01T10:00:00Z",
  "deadline": "2025-12-15T18:00:00Z",
  "participantCount": 0
}
```

**Error** (400 Bad Request):
```json
{
  "error": "Invalid request",
  "code": "INVALID_REQUEST",
  "details": {
    "title": "Title is required",
    "deadline": "Deadline must be in the future"
  }
}
```

### Update Event Status

**Endpoint**: `PUT /api/events/{id}/status`

**Request Body**:
```json
{
  "status": "ORGANIZING"
}
```

**Response** (200 OK):
```json
{
  "id": "event-1",
  "status": "ORGANIZING",
  "updatedAt": "2025-12-10T14:30:00Z"
}
```

---

## Participants API

### Get Participants

**Endpoint**: `GET /api/events/{id}/participants`

**Response** (200 OK):
```json
{
  "participants": [
    {
      "id": "participant-1",
      "eventId": "event-1",
      "userId": "user-1",
      "name": "Alice Smith",
      "email": "alice@example.com",
      "status": "ACCEPTED",
      "isValidated": true,
      "createdAt": "2025-12-01T11:00:00Z"
    }
  ]
}
```

### Add Participant

**Endpoint**: `POST /api/events/{id}/participants`

**Request Body**:
```json
{
  "userId": "user-2",
  "name": "Bob Johnson",
  "email": "bob@example.com"
}
```

**Response** (201 Created):
```json
{
  "id": "participant-2",
  "eventId": "event-1",
  "userId": "user-2",
  "name": "Bob Johnson",
  "email": "bob@example.com",
  "status": "PENDING",
  "isValidated": false,
  "createdAt": "2025-12-01T12:00:00Z"
}
```

---

## Poll API

### Get Poll Results

**Endpoint**: `GET /api/events/{id}/poll`

**Response** (200 OK):
```json
{
  "eventId": "event-1",
  "proposedSlots": [
    {
      "id": "slot-1",
      "startDate": "2025-12-20T10:00:00Z",
      "endDate": "2025-12-20T12:00:00Z",
      "timezone": "Europe/Paris",
      "votes": {
        "YES": 5,
        "MAYBE": 2,
        "NO": 1,
        "total": 8
      },
      "score": 11
    }
  ],
  "bestSlot": {
    "id": "slot-1",
    "score": 11
  }
}
```

### Submit Votes

**Endpoint**: `POST /api/events/{id}/poll/votes`

**Request Body**:
```json
{
  "participantId": "user-1",
  "votes": [
    {
      "slotId": "slot-1",
      "voteType": "YES"
    },
    {
      "slotId": "slot-2",
      "voteType": "MAYBE"
    }
  ]
}
```

**Response** (201 Created):
```json
{
  "message": "Votes submitted successfully",
  "votesCount": 2
}
```

---

## Scenarios API

### Get Scenarios

**Endpoint**: `GET /api/scenarios/event/{eventId}`

**Response** (200 OK):
```json
{
  "scenarios": [
    {
      "id": "scenario-1",
      "eventId": "event-1",
      "name": "Beach Weekend",
      "destination": "Nice, France",
      "startDate": "2025-06-15T00:00:00Z",
      "duration": 3,
      "estimatedBudget": 300.0,
      "currency": "EUR",
      "votes": {
        "PREFER": 4,
        "NEUTRAL": 2,
        "AGAINST": 1,
        "total": 7
      },
      "score": 7
    }
  ]
}
```

### Create Scenario

**Endpoint**: `POST /api/scenarios`

**Request Body**:
```json
{
  "eventId": "event-1",
  "name": "Beach Weekend",
  "destination": "Nice, France",
  "startDate": "2025-06-15T00:00:00Z",
  "duration": 3,
  "estimatedBudget": 300.0,
  "currency": "EUR"
}
```

### Submit Scenario Vote

**Endpoint**: `POST /api/scenarios/{id}/vote`

**Request Body**:
```json
{
  "participantId": "user-1",
  "voteType": "PREFER"
}
```

### Get Ranked Scenarios

**Endpoint**: `GET /api/scenarios/event/{eventId}/ranked`

**Response** (200 OK):
```json
{
  "scenarios": [
    {
      "id": "scenario-1",
      "name": "Beach Weekend",
      "score": 7,
      "rank": 1
    },
    {
      "id": "scenario-2",
      "name": "City Break",
      "score": 4,
      "rank": 2
    }
  ]
}
```

---

## Budget API

### Get Budget

**Endpoint**: `GET /api/events/{id}/budget`

**Response** (200 OK):
```json
{
  "id": "budget-1",
  "eventId": "event-1",
  "targetAmount": 1000.0,
  "currency": "EUR",
  "currentAmount": 850.0,
  "estimatedTotal": 950.0,
  "actualTotal": 820.0,
  "remaining": 180.0,
  "itemsCount": 12
}
```

### Create Budget

**Endpoint**: `POST /api/events/{id}/budget`

**Request Body**:
```json
{
  "targetAmount": 1000.0,
  "currency": "EUR"
}
```

### Get Budget Items

**Endpoint**: `GET /api/events/{id}/budget/items`

**Query Parameters**:
- `category` (optional): Filter by category
- `paid` (optional): Filter by paid status (true/false)

**Response** (200 OK):
```json
{
  "items": [
    {
      "id": "item-1",
      "budgetId": "budget-1",
      "category": "ACCOMMODATION",
      "name": "Hotel Booking",
      "estimatedAmount": 300.0,
      "actualAmount": 280.0,
      "paidBy": "user-1",
      "isPaid": true
    }
  ]
}
```

### Add Budget Item

**Endpoint**: `POST /api/events/{id}/budget/items`

**Request Body**:
```json
{
  "name": "Hotel Booking",
  "category": "ACCOMMODATION",
  "estimatedAmount": 300.0,
  "actualAmount": 280.0,
  "paidBy": "user-1"
}
```

### Get Budget Summary

**Endpoint**: `GET /api/events/{id}/budget/summary`

**Response** (200 OK):
```json
{
  "targetAmount": 1000.0,
  "totalEstimated": 950.0,
  "totalActual": 820.0,
  "remaining": 180.0,
  "byCategory": {
    "ACCOMMODATION": {
      "estimated": 300.0,
      "actual": 280.0
    },
    "TRANSPORT": {
      "estimated": 200.0,
      "actual": 180.0
    },
    "MEALS": {
      "estimated": 250.0,
      "actual": 200.0
    }
  }
}
```

### Get Settlements

**Endpoint**: `GET /api/events/{id}/budget/settlements`

**Response** (200 OK):
```json
{
  "settlements": [
    {
      "fromParticipantId": "user-1",
      "toParticipantId": "user-2",
      "amount": 50.0,
      "currency": "EUR",
      "reason": "User-1 paid €200 accommodation, user-2 contributed nothing"
    }
  ]
}
```

---

## Logistics API

### Accommodation

#### Get Accommodations

**Endpoint**: `GET /api/events/{id}/accommodation`

**Response** (200 OK):
```json
{
  "accommodations": [
    {
      "id": "accommodation-1",
      "eventId": "event-1",
      "name": "Hotel Nice",
      "type": "HOTEL",
      "address": "123 Promenade des Anglais, Nice",
      "pricePerNight": 100.0,
      "currency": "EUR",
      "maxCapacity": 4,
      "bookingStatus": "CONFIRMED"
    }
  ]
}
```

#### Add Accommodation

**Endpoint**: `POST /api/events/{id}/accommodation`

**Request Body**:
```json
{
  "name": "Hotel Nice",
  "type": "HOTEL",
  "address": "123 Promenade des Anglais, Nice",
  "pricePerNight": 100.0,
  "currency": "EUR",
  "maxCapacity": 4,
  "bookingStatus": "CONFIRMED"
}
```

#### Get Statistics

**Endpoint**: `GET /api/events/{id}/accommodation/statistics`

**Response** (200 OK):
```json
{
  "totalAccommodations": 3,
  "totalCapacity": 12,
  "totalCost": 600.0,
  "avgPricePerNight": 200.0,
  "byType": {
    "HOTEL": 2,
    "AIRBNB": 1
  }
}
```

### Meals

#### Get Meals

**Endpoint**: `GET /api/events/{id}/meals`

**Query Parameters**:
- `date` (optional): Filter by date
- `type` (optional): Filter by type (BREAKFAST, LUNCH, DINNER)
- `status` (optional): Filter by status

**Response** (200 OK):
```json
{
  "meals": [
    {
      "id": "meal-1",
      "eventId": "event-1",
      "type": "DINNER",
      "date": "2025-06-15",
      "time": "19:00",
      "location": "Restaurant",
      "estimatedCost": 25.0,
      "actualCost": 22.0,
      "servings": 4,
      "responsible": "user-1",
      "status": "COMPLETED"
    }
  ]
}
```

#### Auto-Generate Meals

**Endpoint**: `POST /api/events/{id}/meals/auto-generate`

**Request Body**:
```json
{
  "startDate": "2025-06-15",
  "endDate": "2025-06-17",
  "mealTypes": ["BREAKFAST", "LUNCH", "DINNER"],
  "defaultCostPerMeal": 20.0
}
```

### Equipment

#### Get Equipment

**Endpoint**: `GET /api/events/{id}/equipment`

**Query Parameters**:
- `category` (optional): Filter by category
- `status` (optional): Filter by status

**Response** (200 OK):
```json
{
  "equipment": [
    {
      "id": "equipment-1",
      "eventId": "event-1",
      "category": "CAMPING",
      "name": "Tent",
      "status": "PACKED",
      "quantity": 1,
      "assignedTo": "user-1",
      "estimatedCost": 150.0
    }
  ]
}
```

#### Auto-Generate Checklist

**Endpoint**: `POST /api/events/{id}/equipment/generate`

**Request Body**:
```json
{
  "eventType": "camping",
  "participantCount": 6
}
```

### Activities

#### Get Activities

**Endpoint**: `GET /api/events/{id}/activities`

**Query Parameters**:
- `date` (optional): Filter by date
- `organizerId` (optional): Filter by organizer

**Response** (200 OK):
```json
{
  "activities": [
    {
      "id": "activity-1",
      "eventId": "event-1",
      "title": "Guided Hike",
      "date": "2025-06-16",
      "startTime": "10:00",
      "duration": 2,
      "location": "Trail",
      "organizerId": "user-1",
      "maxParticipants": 10,
      "costPerParticipant": 15.0
    }
  ]
}
```

#### Register Participant

**Endpoint**: `POST /api/events/{id}/activities/{activityId}/register`

**Request Body**:
```json
{
  "participantId": "user-1"
}
```

---

## Comments API

### Get Comments

**Endpoint**: `GET /api/events/{id}/comments`

**Query Parameters**:
- `section` (optional): Filter by section (GENERAL, SCENARIO, BUDGET, TRANSPORT, ACCOMMODATION, MEAL, EQUIPMENT, ACTIVITY)
- `itemId` (optional): Filter by section item ID
- `threaded` (optional): Return as threads (default: true)

**Response** (200 OK):
```json
{
  "comments": [
    {
      "id": "comment-1",
      "eventId": "event-1",
      "section": "GENERAL",
      "authorId": "user-1",
      "authorName": "Alice Smith",
      "content": "Let's decide on a date!",
      "createdAt": "2025-12-01T14:00:00Z",
      "replyCount": 3,
      "replies": [
        {
          "id": "comment-2",
          "parentId": "comment-1",
          "authorId": "user-2",
          "authorName": "Bob Johnson",
          "content": "I agree!",
          "createdAt": "2025-12-01T14:15:00Z"
        }
      ]
    }
  ]
}
```

### Create Comment

**Endpoint**: `POST /api/events/{id}/comments`

**Request Body**:
```json
{
  "authorId": "user-1",
  "section": "GENERAL",
  "content": "Let's decide on a date!",
  "parentId": null
}
```

### Update Comment

**Endpoint**: `PUT /api/events/{id}/comments/{commentId}`

**Request Body**:
```json
{
  "content": "Updated comment content"
}
```

### Delete Comment

**Endpoint**: `DELETE /api/events/{id}/comments/{commentId}`

### Get Comment Statistics

**Endpoint**: `GET /api/events/{id}/comments/statistics`

**Response** (200 OK):
```json
{
  "totalComments": 45,
  "bySection": {
    "GENERAL": 15,
    "SCENARIO": 12,
    "BUDGET": 8,
    "TRANSPORT": 5,
    "ACCOMMODATION": 3,
    "MEAL": 2
  },
  "topContributors": [
    {
      "participantId": "user-1",
      "name": "Alice Smith",
      "commentCount": 12
    }
  ]
}
```

---

## Suggestions API

### Generate Suggestions

**Endpoint**: `POST /api/events/{id}/suggestions/generate`

**Request Body**:
```json
{
  "type": "SCENARIO",
  "participantId": "user-1",
  "preferences": {
    "budgetRange": {
      "min": 200.0,
      "max": 500.0,
      "currency": "EUR"
    },
    "preferredSeasons": ["SUMMER"],
    "preferredActivities": ["beach", "swimming"]
  }
}
```

**Response** (200 OK):
```json
{
  "suggestions": [
    {
      "itemId": "suggestion-1",
      "type": "SCENARIO",
      "name": "Beach Weekend in Nice",
      "overallScore": 0.85,
      "costScore": 0.9,
      "accessibilityScore": 0.8,
      "seasonalityScore": 1.0,
      "popularityScore": 0.7,
      "personalizationScore": 0.85,
      "reasons": [
        "In your budget",
        "Good season",
        "Matches your preferences"
      ]
    }
  ]
}
```

---

## Calendar API (Future)

### Generate ICS

**Endpoint**: `POST /api/events/{id}/calendar/ics`

**Request Body**:
```json
{
  "invites": ["user-1", "user-2", "user-3"]
}
```

**Response** (200 OK):
```json
{
  "content": "BEGIN:VCALENDAR\nVERSION:2.0\nPRODID:-//Wakeve//Event//FR\n...",
  "filename": "wakeve_event.ics"
}
```

### Add to Native Calendar

**Endpoint**: `POST /api/events/{id}/calendar/native`

**Request Body**:
```json
{
  "participantId": "user-1"
}
```

---

## Meetings API (Future)

### Create Meeting

**Endpoint**: `POST /api/events/{id}/meetings`

**Request Body**:
```json
{
  "organizerId": "user-1",
  "platform": "GOOGLE_MEET",
  "title": "Planning Session",
  "description": "Finalize event details",
  "scheduledFor": "2025-12-27T10:00:00Z",
  "duration": 3600000,
  "timezone": "Europe/Paris",
  "requirePassword": false,
  "waitingRoom": true
}
```

**Response** (200 OK):
```json
{
  "id": "meeting-1",
  "platform": "GOOGLE_MEET",
  "meetingId": "abc-defgh",
  "meetingUrl": "https://meet.google.com/abc-defgh",
  "title": "Planning Session",
  "scheduledFor": "2025-12-27T10:00:00Z",
  "status": "SCHEDULED"
}
```

### Send Invitations

**Endpoint**: `POST /api/meetings/{meetingId}/invitations`

**Response** (200 OK):
```json
{
  "message": "Invitations sent",
  "invitationCount": 5
}
```

---

## Common Error Codes

| Code | Description |
|------|-------------|
| `UNAUTHORIZED` | Invalid or missing authentication |
| `FORBIDDEN` | User doesn't have permission |
| `NOT_FOUND` | Resource not found |
| `INVALID_REQUEST` | Invalid request parameters |
| `CONFLICT` | Resource conflict (e.g., duplicate) |
| `RATE_LIMIT_EXCEEDED` | Too many requests |
| `INTERNAL_ERROR` | Server error |

### Error Response Format

```json
{
  "error": "Error message",
  "code": "ERROR_CODE",
  "details": {
    "field": "Validation error message"
  }
}
```

---

## Status Codes

| Status | Description |
|--------|-------------|
| 200 | OK |
| 201 | Created |
| 204 | No Content |
| 400 | Bad Request |
| 401 | Unauthorized |
| 403 | Forbidden |
| 404 | Not Found |
| 409 | Conflict |
| 429 | Too Many Requests |
| 500 | Internal Server Error |

---

## Pagination

For list endpoints, use query parameters:

- `limit`: Number of items per page (max: 100)
- `offset`: Number of items to skip

**Response**:
```json
{
  "data": [...],
  "pagination": {
    "limit": 50,
    "offset": 0,
    "total": 245
  }
}
```

---

## Webhooks (Future)

### Event Webhooks

Webhook notifications for events:

- `event.created`: New event created
- `event.updated`: Event details changed
- `event.status_changed`: Event status changed

### Comment Webhooks

- `comment.created`: New comment
- `comment.reply_added`: Reply to comment

Configure webhooks in app settings.

---

## SDKs & Libraries

### JavaScript/TypeScript

```typescript
const wakeve = require('wakeve-sdk');

const client = new wakeve.Client({
  apiKey: 'your-api-key',
  baseUrl: 'https://api.wakeve.app'
});

// Create event
const event = await client.events.create({
  title: 'My Event',
  description: 'Event description',
  organizerId: 'user-1'
});
```

### Swift

```swift
import WakeveSDK

let client = WakeveClient(apiKey: "your-api-key")

// Create event
client.createEvent(
  title: "My Event",
  description: "Event description",
  organizerId: "user-1"
) { result in
  switch result {
  case .success(let event):
    print("Event created: \(event)")
  case .failure(let error):
    print("Error: \(error)")
  }
}
```

### Kotlin

```kotlin
import com.guyghost.wakeve.api.WakeveClient

val client = WakeveClient(
    apiKey = "your-api-key",
    baseUrl = "https://api.wakeve.app"
)

// Create event
runBlocking {
    val event = client.events.create(
        title = "My Event",
        description = "Event description",
        organizerId = "user-1"
    )
}
```

---

## Support

### Documentation

- [User Guide](USER_GUIDE.md)
- [Architecture Documentation](ARCHITECTURE.md)
- [OpenSpec Specifications](../openspec/specs/)

### Contact

- **Email**: api-support@wakeve.app
- **GitHub Issues**: https://github.com/wakeve/wakeve/issues
- **Discord**: https://discord.gg/wakeve

---

**Version**: 1.0.0  
**Last Updated**: December 26, 2025  
**Maintainer**: Wakeve Team
