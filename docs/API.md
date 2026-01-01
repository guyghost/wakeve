# Wakeve API Documentation

## Overview

REST API endpoints for the Wakeve event planning application.

**Base URL:** `http://localhost:8080`

**Content-Type:** `application/json`

**Authentication:** TBD (Phase 3)

---

## Events Endpoints

### GET /api/events
List all events.

**Response:**
```json
{
  "events": [
    {
      "id": "event-123",
      "title": "Séminaire d'équipe",
      "description": "...",
      "eventType": "TEAM_BUILDING",
      "eventTypeCustom": null,
      "expectedParticipants": 20,
      "minParticipants": 15,
      "maxParticipants": 25,
      "organizerId": "user-123",
      "status": "DRAFT",
      "createdAt": "2025-12-31T10:00:00Z",
      "updatedAt": "2025-12-31T10:00:00Z"
    }
  ]
}
```

### GET /api/events/{id}
Get a specific event by ID.

**Response:**
```json
{
  "id": "event-123",
  "title": "Séminaire d'équipe",
  "description": "...",
  "eventType": "TEAM_BUILDING",
  "eventTypeCustom": null,
  "expectedParticipants": 20,
  "minParticipants": 15,
  "maxParticipants": 25,
  "organizerId": "user-123",
  "status": "DRAFT",
  "proposedSlots": [
    {
      "id": "slot-1",
      "start": "2025-06-15T14:00:00Z",
      "end": "2025-06-15T18:00:00Z",
      "timezone": "Europe/Paris",
      "timeOfDay": "SPECIFIC"
    }
  ],
  "deadline": "2025-06-01T23:59:59Z",
  "createdAt": "2025-12-31T10:00:00Z",
  "updatedAt": "2025-12-31T10:00:00Z"
}
```

### POST /api/events
Create a new event.

**Request:**
```json
{
  "title": "Séminaire d'équipe",
  "description": "...",
  "organizerId": "user-123",
  "eventType": "TEAM_BUILDING",
  "eventTypeCustom": null,
  "expectedParticipants": 20,
  "minParticipants": 15,
  "maxParticipants": 25,
  "proposedSlots": [
    {
      "start": "2025-06-15T14:00:00Z",
      "end": "2025-06-15T18:00:00Z",
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
  "deadline": "2025-06-01T23:59:59Z"
}
```

**Response:** 201 Created with event object

### PUT /api/events/{id}/status
Update event status.

**Request:**
```json
{
  "status": "POLLING"
}
```

**Response:** 200 OK with updated event

### GET /api/events/{id}/participants
Get participants for an event.

**Response:**
```json
{
  "participants": ["user-123", "user-456", "user-789"]
}
```

### POST /api/events/{id}/participants
Add a participant to an event.

**Request:**
```json
{
  "participantId": "user-456"
}
```

**Response:** 201 Created

### GET /api/events/{id}/poll
Get poll results for an event.

**Response:**
```json
{
  "eventId": "event-123",
  "votes": {
    "slot-1": {
      "user-123": "YES",
      "user-456": "MAYBE"
    }
  }
}
```

### POST /api/events/{id}/poll/votes
Submit votes for time slots.

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
      "vote": "NO"
    }
  ]
}
```

**Response:** 200 OK

---

## Potential Locations Endpoints (NEW)

### GET /api/events/{eventId}/potential-locations
Get all potential locations for an event.

**Response:**
```json
[
  {
    "id": "loc-1",
    "eventId": "event-123",
    "name": "Paris",
    "locationType": "CITY",
    "address": null,
    "coordinates": null,
    "createdAt": "2025-12-31T10:00:00Z"
  },
  {
    "id": "loc-2",
    "eventId": "event-123",
    "name": "Château de Versailles",
    "locationType": "SPECIFIC_VENUE",
    "address": "Place d'Armes, 78000 Versailles",
    "coordinates": {
      "latitude": 48.8049,
      "longitude": 2.1204
    },
    "createdAt": "2025-12-31T10:05:00Z"
  }
]
```

### POST /api/events/{eventId}/potential-locations
Add a potential location to an event.

**Request:**
```json
{
  "name": "Paris",
  "locationType": "CITY",
  "address": null,
  "coordinates": null
}
```

**Response:** 201 Created with location object

### DELETE /api/events/{eventId}/potential-locations/{locationId}
Remove a potential location from an event.

**Response:** 204 No Content

---

## Data Models

### Event
```typescript
{
  id: string,
  title: string,
  description: string,
  organizerId: string,
  eventType: EventType,
  eventTypeCustom?: string,
  expectedParticipants?: number,
  minParticipants?: number,
  maxParticipants?: number,
  status: EventStatus,
  proposedSlots: TimeSlot[],
  deadline: string,
  finalDate?: string,
  createdAt: string,
  updatedAt: string
}
```

### EventType
```typescript
type EventType = "BIRTHDAY" | "WEDDING" | "TEAM_BUILDING" | "CONFERENCE" | 
  "WORKSHOP" | "PARTY" | "SPORTS_EVENT" | "CULTURAL_EVENT" | 
  "FAMILY_GATHERING" | "OTHER" | "CUSTOM"
```

### TimeSlot
```typescript
{
  id: string,
  start?: string,  // ISO date-time, can be null if timeOfDay != SPECIFIC
  end?: string,    // ISO date-time, can be null if timeOfDay != SPECIFIC
  timezone: string,
  timeOfDay: TimeOfDay
}
```

### TimeOfDay
```typescript
type TimeOfDay = "ALL_DAY" | "MORNING" | "AFTERNOON" | "EVENING" | "SPECIFIC"
```

### PotentialLocation
```typescript
{
  id: string,
  eventId: string,
  name: string,
  locationType: LocationType,
  address?: string,
  coordinates?: {
    latitude: number,
    longitude: number
  },
  createdAt: string
}
```

### LocationType
```typescript
type LocationType = "CITY" | "REGION" | "SPECIFIC_VENUE" | "ONLINE"
```

---

## Error Responses

All endpoints may return error responses:

```json
{
  "error": "Error message",
  "code": "ERROR_CODE"
}
```

### Common Error Codes
- `VALIDATION_ERROR`: Invalid input data
- `NOT_FOUND`: Resource not found
- `FORBIDDEN`: Action not allowed (e.g., update non-DRAFT event)
- `INTERNAL_ERROR`: Server error

---

## Validation Rules

### Event Creation/Update
- `title` and `description` are required
- If `eventType = CUSTOM`, `eventTypeCustom` must be provided
- If `minParticipants` and `maxParticipants` provided: `max >= min`
- All participant counts must be non-negative

### TimeSlot
- If `timeOfDay = SPECIFIC`, `start` and `end` are required
- If `timeOfDay != SPECIFIC`, `start` and `end` can be null

### PotentialLocation
- `name` is required
- `locationType` is required
- `address` and `coordinates` are optional
- Can only be added/removed when event is in DRAFT status

---

## Status Codes

- `200 OK`: Request successful
- `201 Created`: Resource created
- `204 No Content`: Resource deleted
- `400 Bad Request`: Validation error
- `404 Not Found`: Resource not found
- `403 Forbidden`: Action not allowed
- `500 Internal Server Error`: Server error
