# Design: Event Organization

> **Capability**: `event-organization`
> **Version**: 1.0.0
> **Status**: Draft

## Overview

The Event Organization capability provides the foundation for Wakeve's multi-agent event planning system. It manages the complete lifecycle from event creation through poll completion and date confirmation.

## Architecture

### Domain Models

```
Event
├── id: String (UUID)
├── title: String
├── description: String
├── organizerId: String
├── proposedSlots: List<TimeSlot>
├── deadline: String (ISO 8601)
├── status: EventStatus (DRAFT, POLLING, CONFIRMED)
└── finalDate: String? (ISO 8601, when status == CONFIRMED)

TimeSlot
├── id: String
├── start: String (ISO 8601, UTC)
├── end: String (ISO 8601, UTC)
└── timezone: String (e.g., "Europe/Paris")

Poll
├── id: String
├── eventId: String
└── votes: Map<participantId, Map<slotId, Vote>>

Vote (enum)
├── YES (2 points)
├── MAYBE (1 point)
└── NO (-1 point)
```

### Repository Pattern

**EventRepository** manages in-memory storage of events and polls:
- `createEvent(event: Event)`
- `getEvent(id: String): Event?`
- `getPoll(eventId: String): Poll?`
- `addVote(eventId, participantId, slotId, vote)`
- `updateEventStatus(id, status, finalDate?)`

### Scoring Algorithm

Best slot calculation uses weighted voting:

```
score(slot) = sum(
  votes.values.sumOf { participantVotes ->
    when (participantVotes[slot.id]) {
      YES -> 2
      MAYBE -> 1
      NO -> -1
      null -> 0
    }
  }
)
```

Best slot = slot with maximum score.

## Technology Decisions

### Decision 1: Timezone Handling
**Context**: Global users spanning multiple timezones.

**Options Considered**:
1. Store all times in UTC, display in local timezone
2. Store times with timezone info, validate during scoring
3. Use server-side timezone conversion

**Decision**: Option 1 - UTC storage with local display
**Rationale**: 
- Simplest to implement and test
- Prevents timezone conversion bugs
- Aligns with ISO 8601 standards
- Easy to extend later with timezone-aware scoring

### Decision 2: Vote Weighting
**Context**: Need to reflect participation quality in slot recommendation.

**Options Considered**:
1. Simple voting (YES counts 1, others count 0)
2. Weighted voting (YES=2, MAYBE=1, NO=-1)
3. Threshold-based (need ≥50% YES)

**Decision**: Option 2 - Weighted voting
**Rationale**:
- Encourages participation (MAYBE is better than NO)
- Aligns with AGENTS.md scoring strategy
- Extensible for future scoring tweaks
- Handles minority blocking edge case

### Decision 3: Event Status Lifecycle
**Context**: Clear event workflow from creation to confirmation.

**Options Considered**:
1. Two-state (DRAFT, CONFIRMED)
2. Three-state (DRAFT, POLLING, CONFIRMED)
3. Four-state (DRAFT, POLLING, PENDING_CONFIRMATION, CONFIRMED)

**Decision**: Option 2 - Three states
**Rationale**:
- Clear separation: creation, voting, confirmation
- Simple for participants to understand
- Extensible for future states (e.g., CANCELLED)

### Decision 4: In-Memory Storage (Phase 1)
**Context**: Initial development needs fast iteration without database setup.

**Options Considered**:
1. In-memory Map
2. File-based JSON
3. SQLite + SQLDelight

**Decision**: Option 1 - In-memory
**Rationale**:
- Fastest to develop and test
- Sufficient for Phase 1 prototyping
- Phase 2 will add SQLDelight for persistence
- No external dependencies

## Data Flow

### Event Creation Flow
```
Organizer -> createEvent() -> EventRepository
                           -> Event (DRAFT status)
                           -> Poll initialized (empty)
```

### Poll Voting Flow
```
Participant -> addVote(participantId, slotId, voteValue)
            -> EventRepository.Poll updated
            -> UI reflects vote change
```

### Date Confirmation Flow
```
Organizer -> calculateBestSlot() -> PollLogic
                                 -> Best slot (highest score)
                                 -> Organizer reviews & confirms
          -> updateEventStatus(CONFIRMED, finalDate)
          -> Event locked, downstream agents notified
```

## Implementation Phases

### Phase 1: Core Models & Logic (COMPLETED)
- [x] Event, TimeSlot, Poll, Vote data classes
- [x] EventRepository in-memory storage
- [x] PollLogic scoring algorithm
- [x] Basic UI for event creation (Android Compose)

### Phase 2: Persistence & Full UI
- [ ] SQLDelight integration for data persistence
- [ ] Complete Android UI (creation, polling, confirmation)
- [ ] iOS SwiftUI implementation
- [ ] Participant list management

### Phase 3: Timezone & Validation
- [ ] Timezone-aware deadline handling
- [ ] Timezone display conversion
- [ ] Timezone conflict scoring penalties
- [ ] Validation tests for edge cases

### Phase 4: Integration with Downstream Agents
- [ ] Notification triggers on date confirmation
- [ ] Destination agent notification
- [ ] Transport agent notification
- [ ] Payment agent setup

## Risks & Mitigation

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|-----------|
| Timezone miscalculation | Wrong slot selected | Medium | Add comprehensive timezone tests; validate during voting |
| Concurrent vote updates | Data loss | Low | In-memory for Phase 1; SQLDelight locking in Phase 2 |
| Lost data on restart | User frustration | High | Phase 2: SQLDelight persistence |
| Ambiguous deadline | Voting confusion | Medium | Clear countdown UI; email reminders in Phase 2 |
| Organizer override abuse | Poll integrity | Low | Audit logging; role checks in code |

## Migration Path

**Phase 1 → Phase 2**: 
- Replace `EventRepository` in-memory storage with SQLDelight
- Add database schema for events, polls, votes
- Implement sync with backend

**Phase 2 → Phase 3**:
- Extend TimeSlot validation for timezone conflicts
- Add timezone-aware scoring penalties
- Extend UI for timezone display

**Phase 3 → Phase 4**:
- Add event confirmation triggers
- Implement observer pattern for downstream agents
- Add inter-agent communication protocol

## Open Questions

- [ ] **Proposal of new slots by participants**: Should participants be able to suggest new slots during POLLING, or is voting only on organizer-proposed slots?
- [ ] **Timezone scoring penalties**: How should timezone conflicts be scored? E.g., should a slot be penalized if it's very late for some participants?
- [ ] **Participation threshold**: Is there a minimum participation rate needed to confirm a date? (e.g., ≥50% responses)
- [ ] **Re-voting after confirmation**: Can an organizer re-open polling if the confirmed slot becomes unavailable?
- [ ] **Invite acceptance flow**: Should participants explicitly accept invitations before they can vote?
