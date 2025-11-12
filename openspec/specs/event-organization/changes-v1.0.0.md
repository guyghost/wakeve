# Change: Event Organization v1.0.0

## ADDED Requirements

### Organizers SHALL be able to create a new event with a title, description, proposed time slots, and voting deadline

#### Scenario: Organizer creates event
- **WHEN** Organizer fills in event title "Team Retreat 2025", description, selects 3 proposed date slots, and sets deadline to 1 week
- **THEN** Event is created with status DRAFT, assigned a unique ID, and ready for poll setup

### Organizers SHALL be able to invite participants and manage the participant list for an event

#### Scenario: Organizer invites participants
- **WHEN** Organizer adds participant emails (e.g., alice@example.com, bob@example.com)
- **THEN** Participants are added to the event, and invitations are queued (actual sending in Phase 2)

### The system SHALL transition an event to POLLING status and enable participants to vote on proposed time slots

#### Scenario: Organizer launches poll
- **WHEN** Organizer clicks "Start Poll"
- **THEN** Event status changes to POLLING, poll is initialized with empty votes, and participants can vote

### Participants SHALL be able to vote on proposed time slots with options: YES, MAYBE, NO

#### Scenario: Participant votes on slots
- **WHEN** Participant selects votes for each proposed slot (e.g., YES for Slot 1, MAYBE for Slot 2, NO for Slot 3)
- **THEN** Votes are recorded in the poll with the participant's ID and slot ID

### The system SHALL enforce the voting deadline and prevent new votes after the deadline has passed

#### Scenario: Voting closes at deadline
- **WHEN** Deadline time arrives
- **THEN** Voting interface becomes read-only, and no new votes can be submitted

### The system SHALL calculate the best time slot based on weighted participant votes: YES=2 points, MAYBE=1 point, NO=-1 point

#### Scenario: System recommends best slot
- **WHEN** Organizer views the poll results after voting deadline
- **THEN** System displays the slot with the highest score as recommended, with score breakdown visible

### Organizers SHALL be able to validate and confirm the final event date, transitioning the event to CONFIRMED status

#### Scenario: Organizer confirms final date
- **WHEN** Organizer clicks "Confirm" on the recommended slot (or selects a different slot and confirms)
- **THEN** Event transitions to CONFIRMED status, finalDate is set, and all participants are notified

### The system SHALL support timezone-aware time slot creation and display, storing times in UTC and displaying in participant local timezone

#### Scenario: Participant in different timezone views slots
- **WHEN** Participant in US/Eastern timezone opens the app
- **THEN** Slots are displayed in their local time (e.g., 10:00 AM to 12:00 PM EST) from UTC times

### The system SHALL enforce role-based access control where Organizers can create and confirm events while Participants can only vote and view event details

#### Scenario: Participant attempts unauthorized action
- **WHEN** Participant tries to change the event deadline or confirm the final date
- **THEN** System prevents the action and displays an error message

### The system SHALL persist event data and poll votes to survive app restart

#### Scenario: App restart preserves event state
- **WHEN** User closes and reopens the app
- **THEN** Event and poll data are restored exactly as before
