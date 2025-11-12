# Design: Add Event Organization

> **Change ID**: `add-event-organization`
> **Issue**: #2
> **Status**: Under Review

## Context
Wakeve needs core functionality for event organizers to create events, manage availability polls, and validate final dates. This is the foundation for all downstream agents (destination, transport, meetings, etc.).

## Goals
- Allow organizers to create events with descriptions
- Enable participants to vote on proposed time slots
- Support deadline management for polls
- Validate and lock the final event date
- Handle timezones globally
- Provide organizer controls

## Non-Goals
- Detailed event planning (destination, lodging, transport)
- Virtual meeting generation
- Payment and cost tracking
- Full sync/offline support (Phase 2)
- Notifications and reminders (Phase 2)

## Architecture
- **Domain Models**: Event, TimeSlot, Poll, Vote (in shared KMP module)
- **Repository**: In-memory EventRepository for state management
- **Scoring Logic**: PollLogic with weighted voting (YES=2, MAYBE=1, NO=-1)
- **UI**: Event creation and polling UI (Android/iOS)

## Technical Decisions

### Decision 1: Timezone Handling
**Context**: Global users have different timezones; deadline and slot times must be clear to all.
**Options Considered**:
- Option A: Store all times in UTC, display in local timezone
- Option B: Store times with timezone, validate during scoring

**Decision**: Option A - Store as ISO strings (UTC), display in local timezone
**Rationale**: Simpler to implement, prevents ambiguity, aligns with standard practices.

### Decision 2: Vote Weighting
**Context**: Scoring needs to reflect participation quality (YES > MAYBE > NO)
**Decision**: Weighted scoring: YES=2, MAYBE=1, NO=-1
**Rationale**: Simple, aligns with AGENTS.md scoring strategy, extensible for future refinements.

### Decision 3: Event Status States
**Context**: Event lifecycle: creation → polling → confirmation
**Decision**: DRAFT → POLLING → CONFIRMED
**Rationale**: Clear transitions, matches workflow requirements.

## Implementation Approach
1. **Phase 1**: Event and Poll domain models, basic voting logic
2. **Phase 2**: UI for event creation and poll voting (Android first)
3. **Phase 3**: Timezone and deadline validation
4. **Phase 4**: Organizer finalization controls

## Risks & Mitigation
| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|-----------|
| Timezone miscalculation | Wrong slot selected | Medium | Add timezone tests, validate during voting |
| Concurrent vote updates | Data loss | Low | Move to SQLDelight with locking |
| No persistence | Lost data on app restart | High | Implement SQLDelight storage (Phase 2) |

## Migration Plan
Current in-memory implementation → SQLDelight persistence (Phase 2)

## Open Questions
- [ ] Should participants be able to propose new slots, or only vote on organizer-provided ones?
- [ ] How should timezone conflicts in scoring be penalized?
- [ ] Should there be a minimum participation threshold to lock a date?
