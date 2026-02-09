# Wakeve - Specifications Index

> **Version**: 1.0.0  
> **Last Updated**: 2026-01-16  
> **Status**: Active

This document provides a unified reference for all Wakeve OpenSpec specifications, documenting the overall architecture, capability dependencies, common patterns, and business glossary.

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Architecture Overview](#architecture-overview)
3. [Event Lifecycle](#event-lifecycle)
4. [Capability Map](#capability-map)
5. [Dependency Graph](#dependency-graph)
6. [Data Model Overview](#data-model-overview)
7. [Unified Glossary](#unified-glossary)
8. [Architectural Patterns](#architectural-patterns)
9. [Specification Consistency Issues](#specification-consistency-issues)
10. [Quick Reference](#quick-reference)

---

## Executive Summary

### Vision

**Wakeve** is a collaborative event planning mobile application that simplifies organizing group events through intelligent automation, real-time collaboration, and AI-powered recommendations.

### Core Value Proposition

- **Effortless Coordination**: Automated poll-based date selection with timezone awareness
- **Smart Recommendations**: AI-driven suggestions for destinations, lodging, and activities
- **Offline-First**: Full functionality without network connectivity
- **Cross-Platform**: Native experience on Android (Material You) and iOS (Liquid Glass)
- **Privacy-Focused**: GDPR-compliant with minimal data collection

### Target Users

| User Type | Description |
|-----------|-------------|
| **Organizer** | Creates events, manages polls, coordinates logistics |
| **Participant** | Votes on dates, contributes to planning, joins activities |
| **Guest** | Limited access without account (view-only + basic voting) |

### Tech Stack

| Layer | Technology |
|-------|------------|
| **Shared Logic** | Kotlin Multiplatform 2.2.20 |
| **Database** | SQLDelight 2.1.0 (SQLite) |
| **Backend** | Ktor 3.3.1 |
| **Android UI** | Jetpack Compose + Material You |
| **iOS UI** | SwiftUI + Liquid Glass |
| **State Management** | MVI + Finite State Machines |

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           PRESENTATION LAYER                             │
│  ┌─────────────────────────┐          ┌─────────────────────────┐       │
│  │   Android (Compose)     │          │     iOS (SwiftUI)       │       │
│  │   Material You Theme    │          │   Liquid Glass Theme    │       │
│  └───────────┬─────────────┘          └───────────┬─────────────┘       │
│              │                                    │                      │
│              └──────────────┬─────────────────────┘                      │
│                             │                                            │
│  ┌──────────────────────────▼──────────────────────────────────────┐    │
│  │                    ViewModels (MVI Pattern)                      │    │
│  │         Intent → State Machine → State → Side Effects            │    │
│  └──────────────────────────┬──────────────────────────────────────┘    │
├─────────────────────────────┼───────────────────────────────────────────┤
│                    BUSINESS LOGIC LAYER (Kotlin Multiplatform)           │
│                             │                                            │
│  ┌──────────────────────────▼──────────────────────────────────────┐    │
│  │                     STATE MACHINES (FSM)                         │    │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │    │
│  │  │   Event     │  │  Scenario   │  │  Meeting    │              │    │
│  │  │ Management  │  │ Management  │  │  Service    │              │    │
│  │  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘              │    │
│  │         └────────────────┼────────────────┘                      │    │
│  │                          │                                       │    │
│  │            ┌─────────────▼─────────────┐                         │    │
│  │            │    SHARED REPOSITORY      │ ◄── Source of Truth     │    │
│  │            │   (Repository-Mediated)   │                         │    │
│  │            └─────────────┬─────────────┘                         │    │
│  └──────────────────────────┼──────────────────────────────────────┘    │
│                             │                                            │
│  ┌──────────────────────────▼──────────────────────────────────────┐    │
│  │                        SERVICES                                  │    │
│  │  ┌───────────┐ ┌───────────┐ ┌───────────┐ ┌───────────┐        │    │
│  │  │   Poll    │ │ Calendar  │ │ Transport │ │Suggestion │        │    │
│  │  │  Service  │ │  Service  │ │  Service  │ │  Service  │        │    │
│  │  └───────────┘ └───────────┘ └───────────┘ └───────────┘        │    │
│  │  ┌───────────┐ ┌───────────┐ ┌───────────┐ ┌───────────┐        │    │
│  │  │  Budget   │ │  Meeting  │ │Destination│ │  Payment  │        │    │
│  │  │  Service  │ │  Service  │ │  Service  │ │  Service  │        │    │
│  │  └───────────┘ └───────────┘ └───────────┘ └───────────┘        │    │
│  └─────────────────────────────────────────────────────────────────┘    │
├─────────────────────────────────────────────────────────────────────────┤
│                         PERSISTENCE LAYER                                │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │                  SQLDelight (SQLite)                             │    │
│  │            Local Source of Truth (Offline-First)                 │    │
│  └─────────────────────────────────────────────────────────────────┘    │
├─────────────────────────────────────────────────────────────────────────┤
│                          PLATFORM LAYER                                  │
│  ┌─────────────────────────┐          ┌─────────────────────────┐       │
│  │   Android (actual)      │          │     iOS (actual)        │       │
│  │  • CalendarContract     │          │  • EventKit             │       │
│  │  • FCM Notifications    │          │  • APNs Notifications   │       │
│  │  • Keystore Auth        │          │  • Keychain Auth        │       │
│  └─────────────────────────┘          └─────────────────────────┘       │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Event Lifecycle

### State Diagram

```
                    ┌──────────────────────────────────────────────────────────────┐
                    │                     EVENT LIFECYCLE                           │
                    └──────────────────────────────────────────────────────────────┘

┌─────────┐    StartPoll    ┌─────────┐   ConfirmDate   ┌───────────┐
│  DRAFT  │ ───────────────►│ POLLING │ ───────────────►│ CONFIRMED │
└─────────┘                 └─────────┘                 └─────┬─────┘
     │                           │                            │
     │                           │ (deadline passed,          │ (scenarios
     │                           │  no votes)                 │  created)
     │                           ▼                            ▼
     │                      ┌─────────┐              ┌───────────┐
     │                      │ EXPIRED │              │ COMPARING │
     │                      └─────────┘              └─────┬─────┘
     │                                                     │
     │                                   TransitionToOrganizing
     │                                          or SelectScenarioAsFinal
     │                                                     │
     │                                                     ▼
     │                                              ┌────────────┐
     │                                              │ ORGANIZING │
     │                                              └─────┬──────┘
     │                                                    │
     │                                          MarkAsFinalized
     │                                                    │
     │                                                    ▼
     │                                              ┌───────────┐
     └─────────────────── DeleteEvent ────────────►│  DELETED  │
                          (any status)             └───────────┘
                                                         │
                                                         ▼
                                                  ┌───────────┐
                                                  │ FINALIZED │
                                                  └───────────┘
```

### Status Descriptions

| Status | Description | Allowed Actions |
|--------|-------------|-----------------|
| **DRAFT** | Event being created via wizard (4 steps) | Edit details, Add time slots, StartPoll, Delete |
| **POLLING** | Active voting on proposed dates | Vote, Add dates (if allowed), ConfirmDate, Delete |
| **CONFIRMED** | Date locked, planning begins | Create scenarios, TransitionToOrganizing, Delete |
| **COMPARING** | Multiple scenarios being voted on | Vote scenarios, SelectScenarioAsFinal, Delete |
| **ORGANIZING** | Final planning phase, meetings created | Create meetings, MarkAsFinalized, Delete |
| **FINALIZED** | Event complete, read-only | View only, Export calendar |
| **EXPIRED** | Poll deadline passed with no votes | Delete, Restart poll |
| **DELETED** | Soft-deleted, pending permanent removal | Restore (within 30 days), Permanent delete |

### Phase Unlocks

| Transition | Unlocks |
|------------|---------|
| DRAFT → POLLING | Poll voting UI |
| POLLING → CONFIRMED | Scenarios creation, Calendar integration |
| CONFIRMED → COMPARING | Scenario voting |
| COMPARING → ORGANIZING | Meeting creation, Budget finalization |
| ORGANIZING → FINALIZED | Read-only mode, Export |

---

## Capability Map

### All 16 Capabilities

| # | Capability | Spec Path | Version | Status | Purpose |
|---|------------|-----------|---------|--------|---------|
| 1 | **event-organization** | `specs/event-organization/spec.md` | v1.1.0 | Draft | Core event CRUD, DRAFT wizard, poll voting, date confirmation |
| 2 | **user-auth** | `specs/user-auth/spec.md` | v1.1.0 | ✅ Implémenté | OAuth 2.0 (Google/Apple) with PKCE, Email OTP, Guest mode, Secure token storage |
| 3 | **workflow-coordination** | `specs/workflow-coordination/spec.md` | v1.0.0 | Active | State machine coordination, repository-mediated communication |
| 4 | **scenario-management** | `specs/scenario-management/spec.md` | v1.0.0 | Active | Scenario creation, voting (PREFER/NEUTRAL/AGAINST), comparison |
| 5 | **calendar-management** | `specs/calendar-management/spec.md` | v1.0.0 | Active | ICS generation (RFC 5545), native calendar integration |
| 6 | **collaboration-management** | `specs/collaboration-management/spec.md` | Draft | Draft | Threaded comments, @mentions, notifications |
| 7 | **budget-management** | `specs/budget-management/spec.md` | v1.0.0 | Active | Cost tracking, expense splitting, settlement suggestions |
| 8 | **meeting-service** | `specs/meeting-service/spec.md` | v1.0.0 | Active | Zoom/Meet/FaceTime link generation, scheduling with rate limiting |
| 9 | **destination-planning** | `specs/destination-planning/spec.md` | v1.0.0 | Active | Destination & lodging suggestions with multi-criteria scoring |
| 10 | **transport-optimization** | `specs/transport-optimization/spec.md` | v1.0.0 | Active | Multi-participant route optimization, meeting points |
| 11 | **suggestion-management** | `specs/suggestion-management/spec.md` | v1.0.0 | Active | Personalized AI recommendations (5-criteria scoring) |
| 12 | **payment-management** | `specs/payment-management/spec.md` | v1.0.0 | Active | Money pot, Tricount integration, payment tracking |
| 13 | **notification-management** | `specs/notification-management/spec.md` | v1.0.0 | Active | Push notifications (FCM/APNs), in-app alerts, preferences |
| 14 | **security-management** | `specs/security-management/spec.md` | v1.0.0 | Active | JWT, RBAC, audit logging, rate limiting, token blacklist |
| 15 | **offline-sync** | `specs/offline-sync/spec.md` | v1.0.0 | Active | SQLite-first, sync queue, conflict resolution, retry logic |
| 16 | **suggestion-engine** | `specs/suggestion-engine/spec.md` | v1.0.0 | Active | Multi-criteria scoring algorithm, user profile learning, A/B testing |

### Implementation Status

```
                    ┌─────────────────────────────────────────┐
                    │        IMPLEMENTATION PROGRESS          │
                    └─────────────────────────────────────────┘

  Phase 1-2 (Core)          Phase 3-4 (Services)        Phase 5+ (Advanced)
  ================          ====================        ==================
  
  [████████████] 100%       [████░░░░░░░░] 30%          [░░░░░░░░░░░░] 0%
  
  ✅ event-organization     🔄 user-auth                ⏳ suggestion-management
  ✅ workflow-coordination  ⏳ meeting-service          ⏳ transport-optimization
  ✅ scenario-management    ⏳ destination-planning     ⏳ payment-management
  ✅ calendar-management    ⏳ collaboration-mgmt       
  ✅ budget-management                                  
  
  Legend: ✅ Implemented  🔄 In Progress  ⏳ Planned
```

---

## Dependency Graph

### Visual Representation

```
                              ┌─────────────────┐
                              │   user-auth     │ (Foundation)
                              │   [STANDALONE]  │
                              └────────┬────────┘
                                       │
                                       ▼
                    ┌──────────────────────────────────────┐
                    │         event-organization           │ (Core)
                    │              [BASE]                  │
                    └──────────────────┬───────────────────┘
                                       │
               ┌───────────────────────┼───────────────────────┐
               │                       │                       │
               ▼                       ▼                       ▼
    ┌──────────────────┐    ┌──────────────────┐    ┌──────────────────┐
    │    workflow-     │    │   scenario-      │    │  collaboration-  │
    │   coordination   │    │   management     │    │   management     │
    │ [ORCHESTRATION]  │    │   [PLANNING]     │    │   [SOCIAL]       │
    └────────┬─────────┘    └────────┬─────────┘    └──────────────────┘
             │                       │
             │              ┌────────┴────────┐
             │              │                 │
             ▼              ▼                 ▼
    ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
    │  meeting-service │  │  budget-         │  │  destination-    │
    │   [MEETINGS]     │  │  management      │  │  planning        │
    └────────┬─────────┘  │  [FINANCE]       │  │  [TRAVEL]        │
             │            └────────┬─────────┘  └────────┬─────────┘
             │                     │                     │
             │                     ▼                     ▼
             │            ┌──────────────────┐  ┌──────────────────┐
             │            │  payment-        │  │  transport-      │
             │            │  management      │  │  optimization    │
             │            │  [PAYMENTS]      │  │  [ROUTES]        │
             │            └──────────────────┘  └────────┬─────────┘
             │                                           │
             └─────────────────┬─────────────────────────┘
                               │
                               ▼
                    ┌──────────────────┐
                    │  suggestion-     │
                    │  management      │
                    │  [AI/ML]         │
                    └────────┬─────────┘
                             │
                             ▼
                    ┌──────────────────┐
                    │  calendar-       │
                    │  management      │
                    │  [INTEGRATION]   │
                    └──────────────────┘
```

### Dependency Matrix

| Capability | Depends On | Depended By |
|------------|------------|-------------|
| **user-auth** | - | All (authentication) |
| **event-organization** | user-auth | workflow-coordination, scenario-management, collaboration-management, budget-management, calendar-management |
| **workflow-coordination** | event-organization | meeting-service, All state machines |
| **scenario-management** | event-organization | budget-management, destination-planning |
| **collaboration-management** | event-organization | - |
| **budget-management** | event-organization, scenario-management | payment-management |
| **meeting-service** | workflow-coordination | calendar-management |
| **destination-planning** | event-organization, suggestion-management | transport-optimization |
| **transport-optimization** | destination-planning | suggestion-management |
| **suggestion-management** | All content specs | destination-planning, transport-optimization |
| **payment-management** | budget-management | - |
| **calendar-management** | event-organization, meeting-service | - |

---

## Data Model Overview

### Core Entities

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          CORE DATA MODEL                                 │
└─────────────────────────────────────────────────────────────────────────┘

┌──────────────┐        ┌──────────────┐        ┌──────────────┐
│    Event     │───────►│  TimeSlot    │◄───────│    Vote      │
│              │  1:N   │              │   N:1  │              │
├──────────────┤        ├──────────────┤        ├──────────────┤
│ id           │        │ id           │        │ id           │
│ title        │        │ eventId      │        │ slotId       │
│ description  │        │ startTime    │        │ participantId│
│ status       │        │ endTime      │        │ value (Y/M/N)│
│ eventType    │        │ timeOfDay    │        │ timestamp    │
│ organizerId  │        │ timezone     │        └──────────────┘
│ finalDate    │        └──────────────┘
│ createdAt    │
│ updatedAt    │        ┌──────────────┐        ┌──────────────┐
└──────┬───────┘        │  Scenario    │───────►│ScenarioVote  │
       │           1:N  │              │   1:N  │              │
       │    ┌───────────┤──────────────┤        ├──────────────┤
       │    │           │ id           │        │ scenarioId   │
       ▼    ▼           │ eventId      │        │ participantId│
┌──────────────┐        │ destination  │        │ vote (P/N/A) │
│ Participant  │        │ lodging      │        │ timestamp    │
├──────────────┤        │ estimatedCost│        └──────────────┘
│ id           │        │ isFinal      │
│ eventId      │        └──────────────┘
│ userId       │
│ role         │        ┌──────────────┐        ┌──────────────┐
│ joinedAt     │        │   Meeting    │        │   Budget     │
└──────────────┘        ├──────────────┤        ├──────────────┤
                        │ id           │        │ id           │
┌──────────────┐        │ eventId      │        │ eventId      │
│PotentialLoc. │        │ platform     │        │ totalAmount  │
├──────────────┤        │ scheduledAt  │        │ currency     │
│ id           │        │ meetingUrl   │        └──────┬───────┘
│ eventId      │        │ duration     │               │
│ name         │        └──────────────┘               │ 1:N
│ locationType │                                       ▼
│ coordinates  │        ┌──────────────┐        ┌──────────────┐
└──────────────┘        │   Comment    │        │  BudgetItem  │
                        ├──────────────┤        ├──────────────┤
                        │ id           │        │ id           │
                        │ eventId      │        │ budgetId     │
                        │ authorId     │        │ description  │
                        │ content      │        │ amount       │
                        │ parentId     │        │ paidBy       │
                        │ createdAt    │        │ splitBetween │
                        └──────────────┘        └──────────────┘
```

### Enums

```kotlin
// Event Types (11 options)
enum class EventType {
    BIRTHDAY, WEDDING, TEAM_BUILDING, CONFERENCE, 
    WORKSHOP, PARTY, TRIP, REUNION, MEETUP, 
    OTHER, CUSTOM
}

// Event Status (8 states)
enum class EventStatus {
    DRAFT, POLLING, CONFIRMED, COMPARING, 
    ORGANIZING, FINALIZED, EXPIRED, DELETED
}

// Vote Values
enum class VoteValue { YES, MAYBE, NO }  // Scoring: YES=2, MAYBE=1, NO=-1

// Scenario Vote
enum class ScenarioVoteValue { PREFER, NEUTRAL, AGAINST }

// Time of Day
enum class TimeOfDay { ALL_DAY, MORNING, AFTERNOON, EVENING, SPECIFIC }

// Location Type
enum class LocationType { CITY, REGION, SPECIFIC_VENUE, ONLINE }

// Meeting Platform
enum class MeetingPlatform { ZOOM, GOOGLE_MEET, FACETIME, TEAMS, CUSTOM }

// Participant Role
enum class ParticipantRole { ORGANIZER, PARTICIPANT, GUEST }
```

---

## Unified Glossary

### Business Terms

| Term | Definition | Used In |
|------|------------|---------|
| **Event** | A planned gathering with participants, date, and optional location/activities | All specs |
| **Organizer** | The user who creates and manages an event | event-organization, workflow-coordination |
| **Participant** | A user who is invited to or joins an event | event-organization, collaboration-management |
| **Guest** | A user without an account with limited functionality | user-auth |
| **Time Slot** | A proposed date/time range for an event | event-organization |
| **Poll** | The voting mechanism for selecting the best date | event-organization |
| **Vote** | A participant's preference on a time slot (YES/MAYBE/NO) | event-organization |
| **Scenario** | A combination of destination + lodging for comparison | scenario-management |
| **Budget** | The financial plan for an event with cost breakdown | budget-management |
| **Meeting** | A virtual gathering with video/audio link | meeting-service |
| **Settlement** | A suggested payment to balance debts between participants | budget-management, payment-management |

### Technical Terms

| Term | Definition | Used In |
|------|------------|---------|
| **State Machine (FSM)** | Finite State Machine managing business logic transitions | workflow-coordination |
| **MVI Pattern** | Model-View-Intent architecture for UI state management | All UI specs |
| **Repository-Mediated Communication** | Pattern where state machines communicate via shared repository | workflow-coordination |
| **expect/actual** | Kotlin Multiplatform mechanism for platform-specific implementations | calendar-management, user-auth |
| **Side Effect** | An action triggered by a state transition (navigation, toast, etc.) | workflow-coordination |
| **Intent** | A user action that triggers a state machine transition | All specs |
| **Source of Truth** | The authoritative data source (SQLite for local, Repository for state) | All specs |
| **ICS** | iCalendar format (RFC 5545) for calendar interchange | calendar-management |
| **OTP** | One-Time Password for email authentication | user-auth |

### Scoring Algorithm

Used across multiple specs for recommendations:

```kotlin
/**
 * Multi-criteria weighted scoring algorithm
 * Used by: suggestion-management, destination-planning, transport-optimization
 */
fun calculateScore(
    costScore: Double,           // 0.0 - 1.0, weight: 30%
    personalizationScore: Double, // 0.0 - 1.0, weight: 25%
    accessibilityScore: Double,   // 0.0 - 1.0, weight: 20%
    seasonalityScore: Double,     // 0.0 - 1.0, weight: 15%
    popularityScore: Double       // 0.0 - 1.0, weight: 10%
): Double {
    return (costScore * 0.30) + 
           (personalizationScore * 0.25) + 
           (accessibilityScore * 0.20) +
           (seasonalityScore * 0.15) +
           (popularityScore * 0.10)
}
```

---

## Architectural Patterns

### 1. Repository-Mediated Communication

State machines communicate **indirectly via a shared repository**, not directly with each other.

```kotlin
// Pattern: State machines read/write Event.status via EventRepository
// This ensures loose coupling and single source of truth

// EventManagementStateMachine updates status
fun handleConfirmDate(eventId: String, slotId: String) {
    repository.updateEvent(
        eventId = eventId,
        status = EventStatus.CONFIRMED,
        finalDate = date
    )
    emitSideEffect(NavigateTo("scenarios/$eventId"))
}

// ScenarioManagementStateMachine reads status to validate actions
fun handleCreateScenario(eventId: String, scenario: Scenario) {
    val event = repository.getEvent(eventId)
    if (event?.status !in listOf(EventStatus.CONFIRMED, EventStatus.COMPARING)) {
        emitSideEffect(ShowError("Cannot create scenario in current status"))
        return
    }
    // Proceed with creation...
}
```

**Benefits:**
- Loose coupling between state machines
- Single source of truth (Event.status)
- Easy testing (mock repository only)
- Clear data flow

### 2. MVI + FSM Pattern

```
┌─────────────────────────────────────────────────────────────────┐
│                    MVI + FSM Architecture                        │
└─────────────────────────────────────────────────────────────────┘

     ┌─────────┐         ┌─────────────────┐         ┌─────────┐
     │   UI    │──Intent─►│  State Machine  │──State──►│   UI    │
     │ (View)  │         │     (FSM)       │         │ (View)  │
     └─────────┘         └────────┬────────┘         └─────────┘
                                  │
                            Side Effects
                                  │
                                  ▼
                         ┌─────────────────┐
                         │   Navigation    │
                         │   Toast/Error   │
                         │   Analytics     │
                         └─────────────────┘
```

### 3. Offline-First with Sync

```kotlin
// Pattern: SQLite as local source of truth with background sync

class EventRepository(
    private val localDataSource: LocalDataSource,    // SQLDelight
    private val remoteDataSource: RemoteDataSource,  // Ktor client
    private val syncManager: SyncManager
) {
    // Always read from local
    fun getEvents(): Flow<List<Event>> = localDataSource.getEvents()
    
    // Write to local, queue for sync
    suspend fun createEvent(event: Event): Result<Event> {
        localDataSource.insertEvent(event)
        syncManager.queueSync(SyncOperation.CREATE, event.id)
        return Result.success(event)
    }
    
    // Background sync
    suspend fun syncWithRemote() {
        val pendingOps = syncManager.getPendingOperations()
        pendingOps.forEach { op ->
            try {
                remoteDataSource.sync(op)
                syncManager.markCompleted(op)
            } catch (e: Exception) {
                syncManager.markRetry(op)
            }
        }
    }
}
```

### 4. expect/actual for Platform-Specific Code

```kotlin
// commonMain - expect declaration
expect class CalendarService {
    suspend fun addToCalendar(event: CalendarEvent): Result<String>
    suspend fun checkPermission(): Boolean
    suspend fun requestPermission(): Boolean
}

// androidMain - actual implementation
actual class CalendarService(private val context: Context) {
    actual suspend fun addToCalendar(event: CalendarEvent): Result<String> {
        // Use CalendarContract API
    }
    actual suspend fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }
}

// iosMain - actual implementation
actual class CalendarService {
    private val eventStore = EKEventStore()
    actual suspend fun addToCalendar(event: CalendarEvent): Result<String> {
        // Use EventKit API
    }
    actual suspend fun checkPermission(): Boolean {
        return EKEventStore.authorizationStatus(for: .event) == .fullAccess
    }
}
```

### 5. Functional Core & Imperative Shell

```
┌─────────────────────────────────────────────────────────────────┐
│                     IMPERATIVE SHELL                             │
│  • State Machines (side effects, I/O)                           │
│  • Repositories (database, network)                              │
│  • UI Components (user interaction)                              │
│  • Services (external integrations)                              │
└──────────────────────────┬──────────────────────────────────────┘
                           │ calls
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                     FUNCTIONAL CORE                              │
│  • Domain Models (immutable data classes)                        │
│  • Business Logic (pure functions)                               │
│  • Validation Rules (no side effects)                            │
│  • Scoring Algorithms (deterministic)                            │
└─────────────────────────────────────────────────────────────────┘

RULE: Shell calls Core, Core NEVER calls Shell
```

---

## Specification Consistency Issues

### Issues to Address

| # | Issue | Severity | Specs Affected | Recommendation |
|---|-------|----------|----------------|----------------|
| 1 | **Language inconsistency** | Low | Multiple | Standardize on English for specs, French for user-facing strings |
| 2 | **user-auth Purpose TBD** | Medium | user-auth | Complete the Purpose section with OAuth/OTP/Guest details |
| 3 | **payment-management minimal** | Medium | payment-management | Expand with scenarios for Tricount sync, pot management |
| 4 | **Missing notification-management spec** | High | Multiple | Create `notification-management/spec.md` (referenced but doesn't exist) |
| 5 | **Terminology variations** | Low | Multiple | Standardize: "Scenario" vs "ScenarioWithVotes" → use "Scenario" everywhere |
| 6 | **collaboration-management incomplete** | Medium | collaboration-management | Add scenarios for @mentions, notifications, threading |
| 7 | **Version inconsistency** | Low | Multiple | Some specs have version, others don't |

### Completed Specifications (2026-02-08)

The following specs were created/updated to address identified gaps:

1. ✅ **notification-management** - Already existed (was incorrectly marked missing)
2. ✅ **security-management** - Complete JWT, RBAC, audit logging, rate limiting spec
3. ✅ **offline-sync** - Complete sync strategy with conflict resolution
4. ✅ **suggestion-engine** - Multi-criteria scoring algorithm with A/B testing
5. ✅ **user-auth** - Updated with OAuth 2.0 implementation details (Google/Apple)

### New Resources Added (2026-02-08)

1. **`specs/template/spec.md`** - Standardized template for new specifications
2. **`specs/security-management/spec.md`** - Complete security model (917 lines)
3. **`specs/offline-sync/spec.md`** - Offline-first sync strategy (290 lines)
4. **`specs/suggestion-engine/spec.md`** - AI scoring algorithm (580 lines)
5. **`proposals/next-priority-specs.md`** - Priority analysis for upcoming specs
6. **`SECURITY_PATCHES.md`** - Security patch documentation from code review

### Future Work

1. **analytics-tracking** - Event tracking, A/B testing framework (LOW PRIORITY)
2. **payment-management** - Expand with Tricount sync scenarios (MEDIUM PRIORITY)
3. **collaboration-management** - Complete @mentions and threading scenarios (MEDIUM PRIORITY)

---

## Quick Reference

### File Paths

```
openspec/
├── AGENTS.md                    # AI agent instructions
├── SPECIFICATIONS_INDEX.md      # This file
├── project.md                   # Project configuration
│
├── specs/                       # Active specifications
│   ├── template/                # Template for new specs
│   │   └── spec.md
│   ├── security-management/     # NEW: Security model
│   │   └── spec.md
│   ├── offline-sync/            # NEW: Sync strategy
│   │   └── spec.md
│   ├── event-organization/
│   ├── user-auth/
│   ├── workflow-coordination/
│   ├── scenario-management/
│   ├── calendar-management/
│   ├── collaboration-management/
│   ├── budget-management/
│   ├── meeting-service/         # UPDATED: Production implementation
│   ├── destination-planning/
│   ├── transport-optimization/
│   ├── suggestion-management/
│   ├── payment-management/
│   └── notification-management/
│
├── proposals/                   # NEW: Spec proposals
│   └── next-priority-specs.md
│
├── changes/                     # Active change proposals
│   └── [change-id]/
│       ├── proposal.md
│       ├── tasks.md
│       └── specs/
│
└── archive/                     # Completed changes
    └── YYYY-MM-DD-[change-id]/
```

### Common Commands

```bash
# List active changes
openspec list

# List specifications
openspec spec list --long

# Show a specific spec
openspec show event-organization --type spec

# Validate a change
openspec validate [change-id] --strict

# Archive a completed change
openspec archive [change-id] --yes
```

### State Machine Intents (Quick Reference)

```kotlin
// Event Management
Intent.CreateEvent(title, description, eventType)
Intent.UpdateDraftEvent(event)
Intent.StartPoll(eventId)
Intent.ConfirmDate(eventId, slotId)
Intent.TransitionToOrganizing(eventId)
Intent.MarkAsFinalized(eventId)
Intent.DeleteEvent(eventId)

// Scenario Management
Intent.CreateScenario(eventId, destination, lodging)
Intent.VoteScenario(scenarioId, vote)
Intent.SelectScenarioAsFinal(eventId, scenarioId)

// Meeting Service
Intent.CreateMeeting(eventId, platform)
Intent.GenerateMeetingLink(meetingId)
```

---

## Changelog

| Version | Date | Changes |
|---------|------|---------|
| 1.1.0 | 2026-02-08 | Added security-management, offline-sync, suggestion-engine specs; updated user-auth with OAuth |
| 1.0.0 | 2026-01-16 | Initial creation with all 12 capabilities mapped |

---

*This document is auto-generated and should be updated when specs change.*
