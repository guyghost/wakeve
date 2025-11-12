# Specs: Add Event Organization

> **Change ID**: `add-event-organization`
> **Issue**: #2
> **Design**: [Link to design wiki page](./design)
> **Status**: Draft

## Affected Specifications
- New capability: `event-organization` (will be moved to `openspec/specs/event-organization/spec.md` after approval)

## ADDED Requirements

### REQ-EVT-001: Event Creation
**Priority**: High
**Category**: Functional

**Description**:
The system SHALL allow an Organizer to create a new event with a title, description, proposed time slots, and voting deadline.

#### Scenario: Organizer creates event
**Given**: Organizer is authenticated and has the app open
**When**: Organizer fills in event title "Team Retreat 2025", description, selects 3 proposed date slots, and sets deadline to 1 week
**Then**: Event is created with status DRAFT, assigned a unique ID, and ready for poll setup

**Acceptance Criteria**:
- [ ] Event has title, description, organizerId, proposedSlots, deadline
- [ ] Event starts in DRAFT status
- [ ] Unique event ID is generated (UUID recommended)
- [ ] Organizer cannot leave title or deadline empty

### REQ-EVT-002: Event Invitation and Participant Addition
**Priority**: High
**Category**: Functional

**Description**:
The system SHALL allow the Organizer to invite participants and manage the participant list for an event.

#### Scenario: Organizer invites participants
**Given**: Event exists in DRAFT status
**When**: Organizer adds participant emails (e.g., alice@example.com, bob@example.com)
**Then**: Participants are added to the event, and invitations are queued (actual sending in Phase 2)

**Acceptance Criteria**:
- [ ] Participants can be added before poll starts
- [ ] Organizer can view list of invited participants
- [ ] Duplicate invitations are prevented

### REQ-EVT-003: Poll Creation and Transition to POLLING
**Priority**: High
**Category**: Functional

**Description**:
The system SHALL transition an event to POLLING status and enable participants to vote on proposed time slots.

#### Scenario: Organizer launches poll
**Given**: Event is in DRAFT status with participants added
**When**: Organizer clicks "Start Poll"
**Then**: Event status changes to POLLING, poll is initialized with empty votes, and participants can vote

**Acceptance Criteria**:
- [ ] Event transitions from DRAFT to POLLING
- [ ] Poll is created with empty vote map
- [ ] Participants can access the voting UI

### REQ-EVT-004: Participant Voting
**Priority**: High
**Category**: Functional

**Description**:
The system SHALL allow participants to vote on proposed time slots with options: YES, MAYBE, NO.

#### Scenario: Participant votes on slots
**Given**: Poll is active (event in POLLING status) and participant has the app open
**When**: Participant selects votes for each proposed slot (e.g., YES for Slot 1, MAYBE for Slot 2, NO for Slot 3)
**Then**: Votes are recorded in the poll with the participant's ID and slot ID

**Acceptance Criteria**:
- [ ] Each participant can vote once per slot
- [ ] Votes can be changed before deadline
- [ ] Only active poll participants can vote

### REQ-EVT-005: Deadline Enforcement
**Priority**: High
**Category**: Functional

**Description**:
The system SHALL enforce the voting deadline and prevent new votes after the deadline has passed.

#### Scenario: Voting closes at deadline
**Given**: Poll is active and current time is before deadline
**When**: Deadline time arrives
**Then**: Voting interface becomes read-only, and no new votes can be submitted

**Acceptance Criteria**:
- [ ] Deadline is stored as ISO string (UTC)
- [ ] Voting disabled automatically after deadline
- [ ] Participants see countdown or deadline time

### REQ-EVT-006: Best Slot Calculation
**Priority**: High
**Category**: Functional

**Description**:
The system SHALL calculate the best time slot based on weighted participant votes: YES=2 points, MAYBE=1 point, NO=-1 point.

#### Scenario: System recommends best slot
**Given**: Voting deadline has passed and votes have been recorded
**When**: Organizer views the poll results
**Then**: System displays the slot with the highest score as recommended, with score breakdown visible

**Acceptance Criteria**:
- [ ] Best slot is calculated correctly using weighted scoring
- [ ] All participants' votes are considered
- [ ] Score breakdown is visible (YES count, MAYBE count, NO count)
- [ ] Ties are handled (first in list or configurable)

### REQ-EVT-007: Organizer Date Validation
**Priority**: High
**Category**: Functional

**Description**:
The system SHALL allow the Organizer to validate (confirm) the final event date, transitioning the event to CONFIRMED status.

#### Scenario: Organizer confirms final date
**Given**: Poll has closed and best slot is displayed
**When**: Organizer clicks "Confirm" on the recommended slot (or selects a different slot and confirms)
**Then**: Event transitions to CONFIRMED status, finalDate is set, and all participants are notified (notification in Phase 2)

**Acceptance Criteria**:
- [ ] Organizer can confirm the recommended slot or override it
- [ ] Event status changes to CONFIRMED
- [ ] finalDate is stored as ISO string
- [ ] Confirmation is locked and cannot be changed without new poll

### REQ-EVT-008: Timezone Support
**Priority**: Medium
**Category**: Functional

**Description**:
The system SHALL support timezone-aware time slot creation and display, storing times in UTC and displaying in participant local timezone.

#### Scenario: Participant in different timezone views slots
**Given**: Event has slots created in UTC (e.g., 15:00 UTC to 17:00 UTC)
**When**: Participant in US/Eastern timezone opens the app
**Then**: Slots are displayed in their local time (e.g., 10:00 AM to 12:00 PM EST)

**Acceptance Criteria**:
- [ ] TimeSlot includes timezone field
- [ ] All times stored in ISO format (UTC)
- [ ] Display logic converts to participant's local timezone
- [ ] Deadline respects timezone to avoid premature closure

### REQ-EVT-009: Role-Based Access Control
**Priority**: Medium
**Category**: Functional, Security

**Description**:
The system SHALL enforce role-based access control: Organizers can create/confirm events; Participants can only vote and view event details.

#### Scenario: Participant attempts unauthorized action
**Given**: Participant is viewing an event poll
**When**: Participant tries to change the event deadline or confirm the final date
**Then**: System prevents the action and displays an error message

**Acceptance Criteria**:
- [ ] Organizer role is assigned at event creation
- [ ] Participants cannot transition event status
- [ ] Participants cannot modify deadlines
- [ ] Role checks occur on all privileged operations

### REQ-EVT-010: Event Data Persistence
**Priority**: Medium
**Category**: Non-Functional

**Description**:
The system SHALL persist event data and poll votes to survive app restart (SQLDelight in Phase 2; currently in-memory for Phase 1).

#### Scenario: App restart preserves event state
**Given**: Event has been created and votes recorded
**When**: User closes and reopens the app
**Then**: Event and poll data are restored exactly as before

**Acceptance Criteria**:
- [ ] Phase 1: In-memory storage sufficient for testing
- [ ] Phase 2: SQLDelight integration for persistence
- [ ] No data loss on normal shutdown

## Validation Results
```bash
$ openspec validate add-event-organization --strict
# Results will be pasted here after running locally
```
