# Specification Delta: Event Organization - Enhanced DRAFT Phase

> **Change ID**: `enhance-draft-phase`
> **Capability**: `event-organization`
> **Type**: Enhancement
> **Date**: 2025-12-31

## Summary

Ce delta enrichit la phase DRAFT de création d'événement avec :
- Système de types d'événements (catégorisation)
- Estimation du nombre de participants (min/max/expected)
- Liste de lieux potentiels
- Créneaux horaires flexibles (moment de journée)

---

## MODIFIED Requirements

<!-- All requirements have been merged into openspec/specs/event-organization/spec.md -->

### Requirement: Enhanced Event Creation
**ID**: `event-org-001` (from original spec)

Organizers SHALL be able to create a new event with title (required), description (required), event type (optional, default OTHER), estimated participants (optional: min, max, expected), potential locations (optional, 0 or more), proposed time slots (required, 1 or more) with flexible timeOfDay, and voting deadline (required).

**Changes from original:**
- ➕ Added eventType, eventTypeCustom
- ➕ Added minParticipants, maxParticipants, expectedParticipants
- ➕ Added PotentialLocations list
- ➕ TimeSlots now support flexible timeOfDay

### Requirement: Event Type Classification
**ID**: `event-org-101`

The system SHALL allow organizers to categorize an event with a predefined or custom type.

**Business Rules:**
- Types prédéfinis : BIRTHDAY, WEDDING, TEAM_BUILDING, CONFERENCE, WORKSHOP, PARTY, SPORTS_EVENT, CULTURAL_EVENT, FAMILY_GATHERING, OTHER, CUSTOM
- Si type = CUSTOM, un champ texte libre `eventTypeCustom` DOIT être fourni
- Le type d'événement PEUT être modifié tant que l'événement est en status DRAFT
- Le type d'événement est utilisé par les agents (Suggestions, Destination) pour personnaliser les recommandations

### Requirement: Participant Count Estimation
**ID**: `event-org-102`

The system SHALL allow organizers to estimate the expected number of participants.

**Business Rules:**
- 3 champs optionnels : `minParticipants`, `maxParticipants`, `expectedParticipants`
- Validation : Si min ET max fournis, alors `maxParticipants >= minParticipants`
- Si seul `expectedParticipants` fourni, il sert de référence pour les calculs
- Ces valeurs sont indicatives et peuvent être mises à jour jusqu'à la phase ORGANIZING
- Utilisé par les agents Transport, Destination, Budget pour dimensionner les propositions

### Requirement: Potential Locations List
**ID**: `event-org-103`

The system SHALL allow organizers to propose a list of potential locations for the event.

**Business Rules:**
- Un événement PEUT avoir 0 ou plusieurs PotentialLocations
- Chaque PotentialLocation a :
  - `name` (obligatoire) : nom du lieu
  - `locationType` : CITY, REGION, SPECIFIC_VENUE, ONLINE
  - `address` (optionnel) : adresse textuelle
  - `coordinates` (optionnel) : lat/lng JSON pour géolocalisation future
- Les PotentialLocations sont **indicatives** en phase DRAFT
- Le vote/comparaison sur les lieux se fait en phase COMPARING via Scenarios
- Les PotentialLocations PEUVENT être ajoutées/supprimées en DRAFT uniquement

### Requirement: Flexible Time Slots with Time of Day
**ID**: `event-org-104`

The system SHALL support flexible time slots with time-of-day indication.

**Business Rules:**
- TimeSlot a un nouveau champ `timeOfDay` : ALL_DAY, MORNING, AFTERNOON, EVENING, SPECIFIC
- Si `timeOfDay = SPECIFIC`, alors `start` et `end` DOIVENT être fournis (comportement actuel)
- Si `timeOfDay != SPECIFIC`, alors `start` et `end` PEUVENT être null (ou indicatifs)
- Permet de proposer "un après-midi mi-juin" sans préciser l'heure exacte
- L'organisateur PEUT préciser plus tard (avant POLLING) les heures exactes
- Migration : TimeSlots existants → `timeOfDay = SPECIFIC`

---

## REMOVED Requirements

_Aucun requirement supprimé dans ce delta._

---

## Data Model Changes

<!-- See openspec/specs/event-organization/spec.md for complete data models -->

### Event (modified)
```kotlin
data class Event(
    // ... existing fields
    
    // NEW FIELDS
    val eventType: EventType = EventType.OTHER,
    val eventTypeCustom: String? = null,
    val minParticipants: Int? = null,
    val maxParticipants: Int? = null,
    val expectedParticipants: Int? = null
)
```

### EventType (new enum)
```kotlin
enum class EventType {
    BIRTHDAY,
    WEDDING,
    TEAM_BUILDING,
    CONFERENCE,
    WORKSHOP,
    PARTY,
    SPORTS_EVENT,
    CULTURAL_EVENT,
    FAMILY_GATHERING,
    OTHER,
    CUSTOM
}
```

### PotentialLocation (new model)
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

enum class LocationType {
    CITY,
    REGION,
    SPECIFIC_VENUE,
    ONLINE
}
```

### TimeSlot (modified)
```kotlin
data class TimeSlot(
    // ... existing fields
    
    // NEW FIELD
    val timeOfDay: TimeOfDay = TimeOfDay.SPECIFIC
)

enum class TimeOfDay {
    ALL_DAY,
    MORNING,
    AFTERNOON,
    EVENING,
    SPECIFIC
}
```

---

## SQLDelight Schema Changes

<!-- See openspec/specs/event-organization/spec.md for complete schema -->

### Event.sq (migration)
```sql
-- Migration: Add new columns to event table
ALTER TABLE event ADD COLUMN eventType TEXT DEFAULT 'OTHER';
ALTER TABLE event ADD COLUMN eventTypeCustom TEXT;
ALTER TABLE event ADD COLUMN minParticipants INTEGER;
ALTER TABLE event ADD COLUMN maxParticipants INTEGER;
ALTER TABLE event ADD COLUMN expectedParticipants INTEGER;
```

### PotentialLocation.sq (new table)
```sql
CREATE TABLE potential_location (
    id TEXT PRIMARY KEY NOT NULL,
    eventId TEXT NOT NULL,
    name TEXT NOT NULL,
    locationType TEXT NOT NULL,
    address TEXT,
    coordinates TEXT,
    createdAt TEXT NOT NULL,
    FOREIGN KEY (eventId) REFERENCES event(id) ON DELETE CASCADE
);
```

### TimeSlot.sq (migration)
```sql
-- Migration: Add timeOfDay column
ALTER TABLE time_slot ADD COLUMN timeOfDay TEXT DEFAULT 'SPECIFIC';
UPDATE time_slot SET timeOfDay = 'SPECIFIC' WHERE timeOfDay IS NULL;
```

---

## API Changes

<!-- See docs/API.md for complete API documentation -->

### New endpoints for PotentialLocations

#### GET /api/events/{eventId}/potential-locations
#### POST /api/events/{eventId}/potential-locations
#### DELETE /api/events/{eventId}/potential-locations/{locationId}

### Updated endpoint

#### POST /api/events (modified request body)
```json
{
  "title": "...",
  "description": "...",
  "eventType": "TEAM_BUILDING",
  "expectedParticipants": 20,
  "minParticipants": 15,
  "maxParticipants": 25
}
```

---

## Testing Requirements

### Unit Tests (shared)
- ✅ EventType enum serialization/deserialization
- ✅ Event validation (maxParticipants >= minParticipants)
- ✅ PotentialLocation creation with all LocationTypes
- ✅ TimeSlot with timeOfDay variations
- ✅ Migration test: existing events get default values

### Integration Tests
- ✅ Create DRAFT event with full data → StartPoll → POLLING
- ✅ Add/remove PotentialLocations in DRAFT
- ✅ Attempt to modify PotentialLocations in POLLING (should fail)
- ✅ Flexible TimeSlot voting (participants vote on AFTERNOON slot)

### UI Tests (Android/iOS)
- ✅ Wizard navigation (4 steps)
- ✅ EventTypeSelector with CUSTOM input
- ✅ ParticipantsEstimation validation (max < min)
- ✅ PotentialLocationsList add/remove
- ✅ TimeOfDay selector
- ✅ Auto-save on step change
- ✅ Accessibility (TalkBack/VoiceOver)

---

## Migration Strategy

### Phase 1: Schema Migration
1. Run SQLDelight migration to add columns/tables
2. Test rollback safety
3. Verify existing events still load correctly

### Phase 2: Data Migration
1. Existing events: `eventType = OTHER`, participants counts = null
2. Existing TimeSlots: `timeOfDay = SPECIFIC`
3. No PotentialLocations for old events (empty list)

### Phase 3: Code Deployment
1. Deploy backend with backward-compatible API
2. Deploy mobile apps with new UI (wizard)
3. Existing users see new fields as optional

---

## Backward Compatibility

✅ **Fully backward compatible:**
- Old events continue to work (default values)
- Old API clients can still POST without new fields
- New API clients get default values if fields omitted

❌ **Breaking changes:**
- None

---

## Dependencies

**Upstream (blocks this change):**
- None

**Downstream (enabled by this change):**
- Agent Suggestions (uses EventType)
- Agent Transport (uses expectedParticipants + PotentialLocation)
- Agent Destination (uses PotentialLocation)
- ScenarioManagement (uses PotentialLocations to create scenarios)

---

## Acceptance Criteria

### Functional
- ✅ Organisateur peut sélectionner un EventType (preset ou custom)
- ✅ Organisateur peut estimer min/max/expected participants
- ✅ Organisateur peut ajouter/supprimer des PotentialLocations (DRAFT uniquement)
- ✅ Organisateur peut créer des TimeSlots flexibles (timeOfDay)
- ✅ Validation empêche maxParticipants < minParticipants
- ✅ Wizard progressif sauvegarde automatiquement à chaque étape
- ✅ Tous les champs optionnels ont des valeurs par défaut

### Non-Functional
- ✅ Migration de données réussie (0 erreur sur événements existants)
- ✅ UI responsive (< 100ms de latence sur interactions)
- ✅ Accessibilité validée (score 100% TalkBack/VoiceOver)
- ✅ Design system respecté (Material You/Liquid Glass)
- ✅ Tests coverage ≥ 90% sur nouveaux modèles

---

## Success Metrics

**Adoption:**
- 70% des nouveaux événements ont un EventType != OTHER
- 50% des événements ont expectedParticipants renseigné
- 30% des événements ont ≥1 PotentialLocation

**Quality:**
- 0 crash lié aux nouveaux champs
- Temps de création d'événement < 3 minutes (wizard complet)
- Taux de validation d'erreur < 5% (participants count)

---

## References

- Original spec: `openspec/specs/event-organization/spec.md`
- State Machine: `EventManagementStateMachine.kt`
- Design System: `.opencode/design-system.md`
- Related change: `add-full-prd-features` (Phase 3 agents)
