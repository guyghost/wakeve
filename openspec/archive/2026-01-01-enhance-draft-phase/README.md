# Enhanced DRAFT Phase for Event Creation

> **Status**: ✅ Proposal Ready  
> **Change ID**: `enhance-draft-phase`  
> **Estimated Duration**: 8 days  
> **Priority**: High (blocks Phase 3 agents)

## 📋 Quick Summary

This change enriches the DRAFT phase of event creation with:

1. **Event Type Classification** - Categorize events (wedding, team building, etc.) for personalized suggestions
2. **Participant Estimation** - Specify min/max/expected participant counts for better planning
3. **Potential Locations** - Propose multiple locations before voting phase
4. **Flexible Time Slots** - Support "afternoon" or "all day" slots, not just precise times

## 🎯 Why This Change?

**Current limitation**: DRAFT phase only captures basic info (title, description, time slots). This is insufficient for:
- Agents to provide relevant suggestions (no context on event type)
- Transport/Destination agents to dimension their proposals (no participant count)
- Realistic planning (forced to specify exact times too early)

**Solution**: Structured DRAFT phase with optional fields that prepare for Phase 3 agents while maintaining flexibility.

## 📊 Impact

### Benefits
✅ Better UX (guided wizard, auto-save)  
✅ Enables Phase 3 agents (Suggestions, Transport, Destination)  
✅ More realistic planning (flexible time slots)  
✅ Backward compatible (existing events unaffected)

### Risks
⚠️ Increased complexity (more fields to fill)  
⚠️ Migration risk (schema changes)

**Mitigation**: All new fields optional with smart defaults, extensive migration tests.

## 📁 Files in This Change

```
openspec/changes/enhance-draft-phase/
├── README.md                           ← You are here
├── proposal.md                         ← Full context, objectives, timeline
├── tasks.md                            ← 82 tasks checklist (7 phases)
├── design.md                           ← Technical design, architecture, testing
└── specs/
    └── event-organization/
        └── spec.md                     ← Specification deltas (4 ADDED, 1 MODIFIED)
```

## 🚀 Quick Start

### 1. Read the Proposal
Start with `proposal.md` to understand context, objectives, and decisions.

### 2. Review the Specs
Check `specs/event-organization/spec.md` for requirements and scenarios:
- **event-org-101**: Event Type Classification
- **event-org-102**: Participant Count Estimation
- **event-org-103**: Potential Locations List
- **event-org-104**: Flexible Time Slots with Time of Day

### 3. Check Technical Design
See `design.md` for:
- Architecture diagrams
- Data models (EventType, PotentialLocation, TimeOfDay)
- UI/UX mockups (Android/iOS)
- Testing strategy
- Migration plan

### 4. Start Implementation
Follow `tasks.md` sequentially:
- Phase 1: Schema & Data Models (5 tasks)
- Phase 2: Business Logic (10 tasks)
- Phase 3: UI Android (13 tasks)
- Phase 4: UI iOS (14 tasks)
- Phase 5: Backend API (6 tasks)
- Phase 6: Testing & Documentation (15 tasks)
- Phase 7: Review & Deployment (6 tasks)

## 🔑 Key Concepts

### Event Type (New)
```kotlin
enum class EventType {
    BIRTHDAY, WEDDING, TEAM_BUILDING, CONFERENCE,
    WORKSHOP, PARTY, SPORTS_EVENT, CULTURAL_EVENT,
    FAMILY_GATHERING, OTHER, CUSTOM
}
```

Usage: Enables agents to suggest relevant services (e.g., caterer for wedding).

### Participant Estimation (New)
```kotlin
data class Event(
    // ...existing fields
    val minParticipants: Int? = null,      // e.g., 15
    val maxParticipants: Int? = null,      // e.g., 25
    val expectedParticipants: Int? = null  // e.g., 20 (used by default)
)
```

Usage: Transport/Destination agents calculate cost ranges for 15-25 people.

### Potential Location (New Model)
```kotlin
data class PotentialLocation(
    val id: String,
    val eventId: String,
    val name: String,                    // e.g., "Paris"
    val locationType: LocationType,      // CITY, REGION, SPECIFIC_VENUE, ONLINE
    val address: String? = null,
    val coordinates: Coordinates? = null
)
```

Usage: Organizer proposes cities/venues in DRAFT, participants vote in COMPARING phase.

### Time of Day (New for TimeSlot)
```kotlin
data class TimeSlot(
    // ...existing fields
    val timeOfDay: TimeOfDay = TimeOfDay.SPECIFIC
)

enum class TimeOfDay {
    ALL_DAY,      // Full day event
    MORNING,      // ~6h-12h
    AFTERNOON,    // ~12h-18h
    EVENING,      // ~18h-23h
    SPECIFIC      // Exact start/end times (existing behavior)
}
```

Usage: Allows "sometime in June afternoon" without forcing exact time too early.

## 🧪 Testing Highlights

### Unit Tests (Shared)
- Validation: `maxParticipants >= minParticipants`
- EventType.CUSTOM requires `eventTypeCustom` text
- TimeOfDay.ALL_DAY allows null start/end

### Integration Tests
- Full DRAFT creation workflow (wizard steps)
- Add/remove PotentialLocations
- Block modifications after DRAFT status

### UI Tests (Android/iOS)
- Wizard navigation (4 steps)
- Auto-save on step change
- Validation error feedback
- Accessibility (TalkBack/VoiceOver)

**Coverage Target**: ≥90% on new models/logic

## 🗺️ Roadmap

### Current Phase (Phase 2.7)
✅ Proposal complete  
✅ Specs validated  
⏳ Implementation pending

### Next Steps
1. **Week 1**: Backend (schema, repositories, API)
2. **Week 2**: Android UI (Compose wizard)
3. **Week 3**: iOS UI (SwiftUI wizard)
4. **Week 4**: Testing, documentation, deployment

### Downstream Impact (Phase 3)
This change **unblocks**:
- 🤖 Agent Suggestions (uses EventType)
- 🚗 Agent Transport (uses expectedParticipants + PotentialLocation)
- 🏨 Agent Destination (uses PotentialLocation)
- 🎭 ScenarioManagement enhancements (location voting)

## 📞 Questions?

### For Developers
- See `design.md` for detailed architecture
- See `tasks.md` for granular breakdown
- See `specs/event-organization/spec.md` for requirements

### For PMs/Stakeholders
- See `proposal.md` for business context and timeline
- Success metrics: 70% adoption of EventType, 50% of participant estimation

### For QA
- See `design.md` → "Testing Strategy" section
- See `tasks.md` → Phase 6 (Testing & Documentation)

## 🔗 Related Changes

- **Depends on**: None (fully standalone)
- **Blocks**: Phase 3 agents (add-full-prd-features)
- **Related to**: 
  - `implement-first-time-onboarding` (UI patterns)
  - `apply-liquidglass-cards` (iOS design system)

## ✅ Validation

```bash
# Validate this change
openspec validate enhance-draft-phase --strict
# ✅ Change 'enhance-draft-phase' is valid
```

---

**Ready to start?** Open `tasks.md` and check off tasks as you go! 🚀
