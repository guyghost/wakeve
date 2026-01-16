# Specification: Event Organization

> **Capability**: `event-organization`
> **Version**: 1.1.0
> **Status**: Draft
> **Last Updated**: 2026-01-01

## Purpose

The Event Organization capability enables organizers to create collaborative events, invite participants, manage availability polls, and validate final event dates. This is the foundational capability for the Wakeve event planning system.

This specification includes the **Enhanced DRAFT Phase** features:
- Event Type Classification (categorization)
- Participant Count Estimation (min/max/expected)
- Potential Locations List
- Flexible Time Slots (time of day)

### Core Concepts

**Event**: A collaborative event created by an organizer with proposed time slots and a voting deadline.

**TimeSlot**: A proposed date and time range for the event, stored in UTC with timezone information, optionally including a time-of-day indication.

**PotentialLocation**: A suggested location (city, region, specific venue, or online) proposed by the organizer during the DRAFT phase.

**Poll**: A collection of participant votes on proposed time slots.

**Vote**: A participant's preference (YES, MAYBE, NO) for a specific time slot.

**Event Status**: 
- `DRAFT`: Event created, not yet open for voting
- `POLLING`: Event open for participant voting
- `CONFIRMED`: Event has a final confirmed date
## Requirements
### Requirement: Organizers SHALL be able to create a new event with a title, description, event type, estimated participants, potential locations, proposed time slots with flexible time-of-day, and voting deadline
**ID**: `event-org-001`

Organizers SHALL be able to create a new event with title (required), description (required), event type (optional, default OTHER), estimated participants (optional: min, max, expected), potential locations (optional, 0 or more), proposed time slots (required, 1 or more) with flexible timeOfDay, and voting deadline (required).

#### Scenario: Organizer creates a comprehensive DRAFT event
- **GIVEN** Organizer starts event creation
- **WHEN** Organizer fills in the wizard:
  - Step 1: Title="Team Retreat 2025", Description="Annual team building", Type=TEAM_BUILDING
  - Step 2: expectedParticipants=20, minParticipants=15, maxParticipants=25
  - Step 3: Adds "Paris" (CITY) and "Lyon" (CITY) as potential locations
  - Step 4: Adds 3 slots (2 with timeOfDay=AFTERNOON, 1 with specific hours)
  - Deadline = 2 weeks
- **THEN** Event is created with status DRAFT, all fields are saved, and potential locations are linked.

### Organizers SHALL be able to invite participants and manage the participant list for an event
**ID**: `event-org-002`

#### Scenario: Organizer invites participants
- **WHEN** Organizer adds participant emails (e.g., alice@example.com, bob@example.com)
- **THEN** Participants are added to the event, and invitations are queued.

### The system SHALL transition an event to POLLING status and enable participants to vote on proposed time slots
**ID**: `event-org-003`

#### Scenario: Organizer launches poll
- **WHEN** Organizer clicks "Start Poll"
- **THEN** Event status changes to POLLING, poll is initialized with empty votes, and participants can vote.

### Participants SHALL be able to vote on proposed time slots with options: YES, MAYBE, NO
**ID**: `event-org-004`

#### Scenario: Participant votes on slots
- **WHEN** Participant selects votes for each proposed slot (e.g., YES for Slot 1, MAYBE for Slot 2, NO for Slot 3)
- **THEN** Votes are recorded in the poll with the participant's ID and slot ID.

### The system SHALL enforce the voting deadline and prevent new votes after the deadline has passed
**ID**: `event-org-005`

#### Scenario: Voting closes at deadline
- **WHEN** Deadline time arrives
- **THEN** Voting interface becomes read-only, and no new votes can be submitted.

### The system SHALL calculate the best time slot based on weighted participant votes: YES=2 points, MAYBE=1 point, NO=-1 point
**ID**: `event-org-006`

#### Scenario: System recommends best slot
- **WHEN** Organizer views the poll results after voting deadline
- **THEN** System displays the slot with the highest score as recommended, with score breakdown visible.

### Organizers SHALL be able to validate and confirm the final event date, transitioning the event to CONFIRMED status
**ID**: `event-org-007`

#### Scenario: Organizer confirms final date
- **WHEN** Organizer clicks "Confirm" on the recommended slot (or selects a different slot and confirms)
- **THEN** Event transitions to CONFIRMED status, finalDate is set, and all participants are notified.

### The system SHALL support timezone-aware time slot creation and display, storing times in UTC and displaying in participant local timezone
**ID**: `event-org-008`

#### Scenario: Participant in different timezone views slots
- **WHEN** Participant in US/Eastern timezone opens the app
- **THEN** Slots are displayed in their local time from UTC times.

### The system SHALL enforce role-based access control where Organizers can create and confirm events while Participants can only vote and view event details
**ID**: `event-org-009`

#### Scenario: Participant attempts unauthorized action
- **WHEN** Participant tries to change the event deadline or confirm the final date
- **THEN** System prevents the action and displays an error message.

### The system SHALL persist event data and poll votes to survive app restart
**ID**: `event-org-010`

#### Scenario: App restart preserves event state
- **WHEN** User closes and reopens the app
- **THEN** Event and poll data are restored exactly as before.

### Requirement: Event Type Classification
**ID**: `event-org-101`

The system SHALL allow organizers to categorize an event with a predefined or custom type.

**Business Rules:**
- Predefined types: BIRTHDAY, WEDDING, TEAM_BUILDING, CONFERENCE, WORKSHOP, PARTY, SPORTS_EVENT, CULTURAL_EVENT, FAMILY_GATHERING, OTHER, CUSTOM.
- If type = CUSTOM, a free text field `eventTypeCustom` MUST be provided.
- Event type CAN be modified as long as the event is in DRAFT status.
- Event type is used by agents (Suggestions, Destination) to personalize recommendations.

#### Scenario: Organizer selects a preset event type
- **GIVEN** Organizer creates a new event
- **WHEN** They select "WEDDING" from the list
- **THEN** Event.eventType = WEDDING, Event.eventTypeCustom = null.

#### Scenario: Organizer uses custom event type
- **GIVEN** Organizer creates a new event
- **WHEN** They select "CUSTOM" and enter "Robotics Hackathon"
- **THEN** Event.eventType = CUSTOM, Event.eventTypeCustom = "Robotics Hackathon".

### Requirement: Participant Count Estimation
**ID**: `event-org-102`

The system SHALL allow organizers to estimate the expected number of participants.

**Business Rules:**
- 3 optional fields: `minParticipants`, `maxParticipants`, `expectedParticipants`.
- Validation: If both min and max are provided, `maxParticipants >= minParticipants`.
- If only `expectedParticipants` is provided, it serves as the reference for calculations.
- Values are indicative and can be updated until the ORGANIZING phase.

#### Scenario: Organizer provides participant counts
- **GIVEN** Organizer creates a TEAM_BUILDING event
- **WHEN** They enter min=15, max=25, expected=20
- **THEN** All 3 values are saved and used by agents for suggestions.

#### Scenario: Validation fails if max < min
- **GIVEN** Organizer enters minParticipants = 30, maxParticipants = 20
- **WHEN** They attempt to save
- **THEN** Validation error is displayed and save is blocked.

### Requirement: Potential Locations List
**ID**: `event-org-103`

The system SHALL allow organizers to propose a list of potential locations for the event.

**Business Rules:**
- An event CAN have 0 or more PotentialLocations.
- Each PotentialLocation has: `name` (required), `locationType`, `address` (optional), `coordinates` (optional).
- PotentialLocations are indicative in the DRAFT phase.
- PotentialLocations CAN be added/removed in DRAFT status only.

#### Scenario: Organizer adds multiple potential locations
- **GIVEN** Organizer creates a CONFERENCE event
- **WHEN** They add "Paris" (CITY) and "Château de Versailles" (SPECIFIC_VENUE)
- **THEN** PotentialLocations are created in the DB and displayed in the DRAFT UI.

### Requirement: Flexible Time Slots with Time of Day
**ID**: `event-org-104`

The system SHALL support flexible time slots with time-of-day indication.

**Business Rules:**
- TimeSlot has a new field `timeOfDay`: ALL_DAY, MORNING, AFTERNOON, EVENING, SPECIFIC.
- If `timeOfDay = SPECIFIC`, `start` and `end` MUST be provided.
- If `timeOfDay != SPECIFIC`, `start` and `end` CAN be null (indicative).
- Migration: Existing TimeSlots map to `timeOfDay = SPECIFIC`.

#### Scenario: Organizer creates flexible afternoon slot
- **WHEN** They add a slot: date = "2025-06-15", timeOfDay = AFTERNOON
- **THEN** TimeSlot is created with timeOfDay = AFTERNOON and displayed as "15 June 2025 - Afternoon".

---

### Requirement: Delete Event

The organizer of an event MUST be able to delete that event.

#### Règles métier

| Status de l'événement | Suppression autorisée | Confirmation requise |
|-----------------------|----------------------|---------------------|
| DRAFT | Oui | Simple (1 clic + dialog) |
| POLLING | Oui | Avec avertissement (votes perdus) |
| CONFIRMED | Oui | Avec avertissement fort |
| COMPARING | Oui | Avec avertissement fort |
| ORGANIZING | Oui | Avec avertissement fort |
| FINALIZED | Non | Événement terminé, archivé |

#### Scenario: Suppression d'un événement DRAFT par l'organisateur

- **GIVEN** un événement en status `DRAFT`
- **AND** l'utilisateur courant est l'organisateur
- **WHEN** l'utilisateur dispatch `Intent.DeleteEvent(eventId)`
- **THEN** l'événement est supprimé du repository
- **AND** les données liées sont supprimées (time slots, potential locations)
- **AND** un `SideEffect.ShowToast("Événement supprimé")` est émis
- **AND** un `SideEffect.NavigateBack` est émis
- **AND** le state est mis à jour (événement retiré de la liste)

#### Scenario: Suppression d'un événement POLLING par l'organisateur

- **GIVEN** un événement en status `POLLING`
- **AND** l'utilisateur courant est l'organisateur
- **WHEN** l'utilisateur dispatch `Intent.DeleteEvent(eventId)`
- **THEN** l'événement est supprimé du repository
- **AND** les données liées sont supprimées (participants, votes, time slots)
- **AND** un `SideEffect.ShowToast("Événement supprimé")` est émis
- **AND** un `SideEffect.NavigateBack` est émis

#### Scenario: Tentative de suppression par un non-organisateur

- **GIVEN** un événement existant
- **AND** l'utilisateur courant n'est PAS l'organisateur
- **WHEN** l'utilisateur dispatch `Intent.DeleteEvent(eventId)`
- **THEN** l'événement n'est PAS supprimé
- **AND** un `SideEffect.ShowToast("Seul l'organisateur peut supprimer cet événement")` est émis
- **AND** le state.error contient le message d'erreur

#### Scenario: Tentative de suppression d'un événement FINALIZED

- **GIVEN** un événement en status `FINALIZED`
- **AND** l'utilisateur courant est l'organisateur
- **WHEN** l'utilisateur dispatch `Intent.DeleteEvent(eventId)`
- **THEN** l'événement n'est PAS supprimé
- **AND** un `SideEffect.ShowToast("Impossible de supprimer un événement finalisé")` est émis
- **AND** le state.error contient le message d'erreur

#### Scenario: Tentative de suppression d'un événement inexistant

- **GIVEN** un eventId qui n'existe pas dans le repository
- **WHEN** l'utilisateur dispatch `Intent.DeleteEvent(eventId)`
- **THEN** un `SideEffect.ShowToast("Événement introuvable")` est émis
- **AND** le state.error contient le message d'erreur

### Requirement: Cascade Delete

When deleting an event, all related data MUST be deleted in cascade.

#### Données à supprimer en cascade

1. **Participants** - Table `participant` où `eventId = ?`
2. **Time Slots** - Table `time_slot` où `eventId = ?`
3. **Votes** - Table `vote` où `eventId = ?`
4. **Potential Locations** - Table `potential_location` où `eventId = ?`
5. **Scenarios** - Table `scenario` où `eventId = ?`
6. **Scenario Votes** - Table `scenario_vote` où `scenarioId IN (SELECT id FROM scenario WHERE eventId = ?)`
7. **Confirmed Date** - Table `confirmed_date` où `eventId = ?`
8. **Sync Metadata** - Table `sync_metadata` où `entityId = ?`

#### Scenario: Cascade delete vérifié

- **GIVEN** un événement avec des participants, votes et time slots
- **WHEN** l'événement est supprimé
- **THEN** aucune donnée orpheline ne reste dans la base
- **AND** les queries `SELECT * FROM participant WHERE eventId = ?` retournent 0 résultats
- **AND** les queries `SELECT * FROM time_slot WHERE eventId = ?` retournent 0 résultats
- **AND** les queries `SELECT * FROM vote WHERE eventId = ?` retournent 0 résultats

### Requirement: UI Confirmation Dialog

The user interface MUST display a confirmation dialog before deletion.

#### Android (Material You)

```kotlin
AlertDialog(
    title = "Supprimer l'événement ?",
    text = "Cette action est irréversible. Toutes les données liées seront supprimées.",
    confirmButton = "Supprimer" (destructive, rouge),
    dismissButton = "Annuler"
)
```

#### iOS (SwiftUI)

```swift
Alert(
    title: "Supprimer l'événement ?",
    message: "Cette action est irréversible. Toutes les données liées seront supprimées.",
    primaryButton: .destructive("Supprimer"),
    secondaryButton: .cancel("Annuler")
)
```

#### Scenario: Confirmation dialog accessibility

- **GIVEN** the delete button is visible on screen
- **WHEN** the user activates it with VoiceOver/TalkBack
- **THEN** the accessibility label announces "Delete this event"
- **AND** the confirmation dialog is announced as a modal
- **AND** the destructive action is identified by its semantic role

### Requirement: Delete Event Repository Method

The EventRepositoryInterface MUST include a deleteEvent method for cascade deletion.

```kotlin
interface EventRepositoryInterface {
    /**
     * Delete an event and all related data.
     *
     * @param eventId The ID of the event to delete
     * @return Result<Unit> success if deleted, failure with exception otherwise
     */
    suspend fun deleteEvent(eventId: String): Result<Unit>
}
```

#### Scenario: Delete event via repository

- **GIVEN** an event exists in the repository
- **WHEN** deleteEvent is called with the event ID
- **THEN** the event is removed from the repository
- **AND** Result.success is returned

### Requirement: DeleteEvent Intent Authorization

The DeleteEvent intent MUST include userId for authorization verification.

```kotlin
data class DeleteEvent(
    val eventId: String,
    val userId: String  // For authorization verification
) : Intent
```

#### Scenario: DeleteEvent intent with userId

- **GIVEN** a DeleteEvent intent with eventId and userId
- **WHEN** the state machine processes it
- **THEN** it verifies userId matches the event's organizerId
- **AND** proceeds with deletion only if authorized

## MODIFIED Requirements

### Requirement: Enhanced Event Creation
**ID**: `event-org-001` (from original spec)

Organizers SHALL be able to create a new event with title (required), description (required), event type (optional, default OTHER), estimated participants (optional: min, max, expected), potential locations (optional, 0 or more), proposed time slots (required, 1 or more) with flexible timeOfDay, and voting deadline (required).

**Changes from original:**
- ➕ Added eventType, eventTypeCustom
- ➕ Added minParticipants, maxParticipants, expectedParticipants
- ➕ Added PotentialLocations list
- ➕ TimeSlots now support flexible timeOfDay

## Data Models

### Event
```kotlin
data class Event(
    val id: String,
    val title: String,
    val description: String,
    val organizerId: String,
    val participants: List<String> = emptyList(),
    val proposedSlots: List<TimeSlot>,
    val deadline: String,
    val status: EventStatus,
    val finalDate: String? = null,
    val createdAt: String,
    val updatedAt: String,
    
    // Enhanced DRAFT Fields
    val eventType: EventType = EventType.OTHER,
    val eventTypeCustom: String? = null,
    val minParticipants: Int? = null,
    val maxParticipants: Int? = null,
    val expectedParticipants: Int? = null
)
```

### EventType (enum)
`BIRTHDAY`, `WEDDING`, `TEAM_BUILDING`, `CONFERENCE`, `WORKSHOP`, `PARTY`, `SPORTS_EVENT`, `CULTURAL_EVENT`, `FAMILY_GATHERING`, `OTHER`, `CUSTOM`

### PotentialLocation
```kotlin
data class PotentialLocation(
    val id: String,
    val eventId: String,
    val name: String,
    val locationType: LocationType,
    val address: String? = null,
    val coordinates: Coordinates? = null,
    val createdAt: String
)
```

### LocationType (enum)
`CITY`, `REGION`, `SPECIFIC_VENUE`, `ONLINE`

### Coordinates
```kotlin
data class Coordinates(
    val latitude: Double,
    val longitude: Double
)
```

### TimeSlot
```kotlin
data class TimeSlot(
    val id: String,
    val start: String?, 
    val end: String?,   
    val timezone: String,
    val timeOfDay: TimeOfDay = TimeOfDay.SPECIFIC
)
```

### TimeOfDay (enum)
`ALL_DAY`, `MORNING`, `AFTERNOON`, `EVENING`, `SPECIFIC`

## API Changes

### POST /api/events (modified body)
Includes new fields: `eventType`, `eventTypeCustom`, `minParticipants`, `maxParticipants`, `expectedParticipants`. `proposedSlots` now include `timeOfDay`.

### Potential Locations Endpoints
- **GET /api/events/{eventId}/potential-locations**: List potential locations.
- **POST /api/events/{eventId}/potential-locations**: Add a new potential location.
- **DELETE /api/events/{eventId}/potential-locations/{locationId}**: Remove a potential location.

## Testing Requirements

### Unit Tests
- EventType enum serialization/deserialization.
- Event validation (maxParticipants >= minParticipants).
- PotentialLocation creation with all LocationTypes.
- TimeSlot with timeOfDay variations.
- Migration test: existing events get default values.

### Integration Tests
- Create DRAFT event with full data → StartPoll → POLLING.
- Add/remove PotentialLocations in DRAFT.
- Attempt to modify PotentialLocations in POLLING (should fail).
- Flexible TimeSlot voting (participants vote on AFTERNOON slot).

## Migration Strategy

### Phase 1: Schema Migration
1. Run SQLDelight migration to add columns/tables.
2. Verify existing events still load correctly.

### Phase 2: Data Migration
1. Existing events: `eventType = OTHER`, participants counts = null.
2. Existing TimeSlots: `timeOfDay = SPECIFIC`.
3. No PotentialLocations for old events.

## Backward Compatibility
- **Fully backward compatible**: Old events continue to work with default values. Old API clients can still POST without new fields.

## Dependencies
- **Downstream**: Agent Suggestions (uses EventType), Agent Transport (uses participant counts), Agent Destination (uses PotentialLocations), ScenarioManagement (uses PotentialLocations).

## Acceptance Criteria
- Organizer can select an EventType (preset or custom).
- Organizer can estimate min/max/expected participants.
- Organizer can add/remove PotentialLocations (DRAFT only).
- Organizer can create flexible TimeSlots (timeOfDay).
- Validation prevents maxParticipants < minParticipants.
- Wizard progress saves automatically at each step.

## Success Metrics
- 70% of new events have an EventType != OTHER.
- 50% of events have expectedParticipants provided.
- 30% of events have ≥1 PotentialLocation.
- 0 crashes related to new fields.
- Event creation time < 3 minutes for full wizard.
