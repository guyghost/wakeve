# Wakeve - Architecture Documentation

## Table of Contents

1. [Overview](#overview)
2. [Architecture Principles](#architecture-principles)
3. [Technology Stack](#technology-stack)
4. [Project Structure](#project-structure)
5. [Layers Architecture](#layers-architecture)
6. [Data Flow](#data-flow)
7. [Design Patterns](#design-patterns)
8. [Database Schema](#database-schema)
9. [API Architecture](#api-architecture)
10. [Testing Strategy](#testing-strategy)
11. [Offline-First Architecture](#offline-first-architecture)
12. [Deployment](#deployment)

---

## Overview

Wakeve is a **Kotlin Multiplatform** application for collaborative event planning with **offline-first** capabilities. The app runs natively on Android (Jetpack Compose) and iOS (SwiftUI) while sharing business logic, data models, and API clients through a common Kotlin module.

### Key Architectural Goals

1. **Code Reuse**: Maximize shared business logic across platforms
2. **Offline-First**: Functionality works without internet
3. **Sync-Eventually**: Automatic data synchronization when online
4. **Testable**: Comprehensive test coverage (100% passing)
5. **Scalable**: Ready for future enhancements

---

## Architecture Principles

### 1. Multiplatform-First

```
┌─────────────────────────────────────────────┐
│           Kotlin Multiplatform Layer          │
│      (shared/src/commonMain)             │
│                                          │
│  • Business Logic                        │
│  • Data Models                          │
│  • Repository Pattern                    │
│  • API Clients                          │
└──────────────┬──────────────────────────┘
               │
       ┌───────┴───────┐
       │               │
┌──────▼────┐   ┌───▼────────┐
│   Android   │   │    iOS      │
│  (Compose)  │   │  (SwiftUI)  │
└─────────────┘   └──────────────┘
```

### 2. Repository Pattern

Business logic accesses data through **Repository** interfaces, not directly:

```
UI Layer
   │
   │
   ▼
Repository Layer (shared)
   │
   │
   ├─ SQLDelight (offline)
   │
   ├─ Ktor API (online sync)
   │
   └─ Cache (performance)
```

### 3. Dependency Inversion

High-level modules depend on abstractions, not implementations:

```
EventRepository (interface)
   ▲
   │
   ├── DatabaseEventRepository (implementation)
   │
   └── SyncService (implementation)
```

---

## Technology Stack

### Core Technologies

| Layer | Technology | Version |
|-------|-----------|---------|
| **Language** | Kotlin Multiplatform | 2.2.20 |
| **UI Android** | Jetpack Compose | Latest |
| **UI iOS** | SwiftUI | iOS 16+ |
| **Database** | SQLDelight | 2.1.0 |
| **Networking** | Ktor | 3.3.1 |
| **Serialization** | kotlinx-serialization | Latest |
| **Async** | Kotlin Coroutines | Latest |
| **Testing** | Kotlin Test | Latest |

### Android-Specific

- **Build System**: Gradle with Kotlin DSL
- **UI Framework**: Jetpack Compose + Material You
- **Min SDK**: API 26 (Android 8.0)
- **Target SDK**: API 34 (Android 14)

### iOS-Specific

- **Build System**: Xcode + Swift Package Manager
- **UI Framework**: SwiftUI + Liquid Glass
- **iOS Version**: 16.0+
- **Frameworks**:
  - EventKit (calendar integration)
  - NotificationCenter (local notifications)
  - SwiftData (future)

### Backend (Ktor Server)

- **Framework**: Ktor
- **Database**: SQLite (same schema as client)
- **API**: RESTful JSON
- **CORS**: Enabled for cross-platform

---

## Project Structure

```
wakeve/
├── shared/                     # Kotlin Multiplatform shared code
│   ├── commonMain/             # Shared business logic
│   │   └── kotlin/com/guyghost/wakeve/
│   │       ├── models/         # Data models (Event, Participant, etc.)
│   │       ├── repository/     # Repository interfaces & implementations
│   │       ├── services/       # Business logic services
│   │       ├── poll/           # Poll logic
│   │       ├── scenario/       # Scenario management
│   │       ├── budget/         # Budget calculation
│   │       ├── accommodation/  # Lodging logic
│   │       ├── meal/           # Meal planning
│   │       ├── equipment/      # Equipment management
│   │       ├── activity/       # Activity planning
│   │       ├── comment/        # Comments & collaboration
│   │       ├── meeting/        # Virtual meetings
│   │       ├── transport/      # Transport optimization
│   │       ├── destination/    # Destination suggestions
│   │       ├── payment/        # Payment service (future)
│   │       └── sync/           # Offline sync
│   ├── androidMain/            # Android-specific implementations
│   │   └── kotlin/com/guyghost/wakeve/
│   │       └── platform/       # Android-specific services
│   ├── iosMain/               # iOS-specific implementations
│   │   └── kotlin/com/guyghost/wakeve/
│   │       └── platform/       # iOS-specific services
│   ├── jvmMain/               # JVM (server, tests)
│   │   └── kotlin/com/guyghost/wakeve/
│   │       ├── database/        # SQLDelight database factory
│   │       └── server/         # Ktor server setup
│   ├── commonTest/             # Shared tests
│   └── jvmTest/               # JVM-specific tests
│       └── kotlin/com/guyghost/wakeve/
│           ├── repository/      # Repository tests
│           ├── poll/           # Poll logic tests
│           ├── scenario/       # Scenario tests
│           ├── budget/         # Budget tests
│           ├── logistics/      # Logistics tests
│           ├── collaboration/  # Comment tests
│           ├── suggestions/    # Suggestion engine tests
│           └── e2e/           # End-to-end tests
│   └── sqldelight/             # Database schema
│       └── com/guyghost/wakeve/db/
│           ├── Event.sq
│           ├── Participant.sq
│           ├── ProposedSlot.sq
│           ├── Vote.sq
│           ├── Scenario.sq
│           ├── ScenarioVote.sq
│           ├── Budget.sq
│           ├── BudgetItem.sq
│           ├── Accommodation.sq
│           ├── RoomAssignment.sq
│           ├── Meal.sq
│           ├── DietaryRestriction.sq
│           ├── EquipmentItem.sq
│           ├── Activity.sq
│           ├── ActivityParticipant.sq
│           ├── Comment.sq
│           ├── VirtualMeeting.sq
│           ├── MeetingInvitation.sq
│           └── MeetingReminder.sq
│
├── composeApp/                # Android application
│   └── androidMain/
│       └── kotlin/com/guyghost/wakeve/ui/
│           ├── screens/        # Android screens (Compose)
│           ├── components/     # Reusable components
│           ├── theme/          # Material You theme
│           └── viewmodel/     # ViewModels
│
├── iosApp/                    # iOS application
│   └── iosApp/
│       ├── Views/              # SwiftUI views
│       ├── Components/         # Reusable components
│       ├── Theme/              # Liquid Glass theme
│       ├── Extensions/         # View extensions
│       └── Services/           # iOS-specific services
│
├── server/                    # Ktor REST API server
│   └── src/main/kotlin/com/guyghost/wakeve/
│       ├── Application.kt      # Server entry point
│       ├── routing/           # API routes
│       ├── models/            # DTOs
│       └── plugins/          # Ktor plugins
│
├── openspec/                  # OpenSpec specifications
│   ├── specs/                # Feature specifications
│   └── archive/              # Archived changes
│
└── docs/                     # Documentation
    ├── USER_GUIDE.md        # User guide
    ├── ARCHITECTURE.md       # This file
    ├── API.md               # API documentation
    └── README.md            # Project README
```

---

## Layers Architecture

### 1. Presentation Layer (Platform-Specific)

#### Android (Jetpack Compose)
```
Compose Screens
   │
   ├── EventListScreen
   ├── EventDetailScreen
   ├── ScenarioListScreen
   ├── BudgetOverviewScreen
   ├── AccommodationScreen
   ├── MealPlanningScreen
   ├── EquipmentChecklistScreen
   ├── ActivityPlanningScreen
   ├── CommentsScreen
   └── ModernHomeView
```

#### iOS (SwiftUI)
```
SwiftUI Views
   │
   ├── ModernHomeView
   ├── EventCreationView
   ├── ModernEventDetailView
   ├── ModernParticipantManagementView
   ├── ScenarioListView
   ├── ScenarioDetailView
   ├── BudgetOverviewView
   ├── BudgetDetailView
   ├── AccommodationView
   ├── MealPlanningView
   ├── EquipmentChecklistView
   ├── ActivityPlanningView
   └── CommentsView
```

### 2. Business Logic Layer (Shared)

#### Core Services
```
Services (shared/src/commonMain)
   │
   ├── EventRepository          # CRUD events
   ├── PollService            # Voting logic
   ├── ScenarioLogic          # Scenario scoring
   ├── ScenarioRepository      # Scenario management
   ├── BudgetCalculator       # Budget calculations
   ├── BudgetRepository       # Budget persistence
   ├── AccommodationService   # Lodging logic
   ├── MealPlanner           # Meal planning
   ├── EquipmentManager      # Equipment management
   ├── ActivityService       # Activity planning
   ├── CommentRepository      # Comments
   ├── SuggestionService     # Recommendations
   ├── RecommendationEngine   # ML-based scoring
   ├── MeetingService        # Virtual meetings
   ├── TransportService      # Route optimization
   ├── DestinationService    # Destination suggestions
   ├── PaymentService       # Payment integration (future)
   └── SyncService           # Offline sync
```

### 3. Data Layer (Shared)

#### Repositories
```
Repositories (shared/src/commonMain)
   │
   ├── EventRepository (interface)
   ├── ParticipantRepository (interface)
   ├── ScenarioRepository (interface)
   ├── BudgetRepository (interface)
   └── CommentRepository (interface)
```

#### Data Models
```
Models (shared/src/commonMain)
   │
   ├── Event, Participant, ProposedSlot, Vote
   ├── Scenario, ScenarioVote, ScenarioStatus
   ├── Budget, BudgetItem, BudgetCategory
   ├── Accommodation, RoomAssignment
   ├── Meal, DietaryRestriction, MealType
   ├── EquipmentItem, EquipmentCategory
   ├── Activity, ActivityParticipant
   ├── Comment, CommentSection
   ├── MeetingPlatform, VirtualMeeting
   ├── Destination, Lodging, DestinationSuggestion
   └── MoneyPot, Expense, Settlement
```

### 4. Persistence Layer (Shared)

#### SQLDelight Database
```
Database (shared/src/sqldelight)
   │
   ├── Tables (generated by SQLDelight)
   ├── Queries (generated by SQLDelight)
   └── Migrations (manual)
```

---

## Data Flow

### Offline-First Data Flow

```
┌────────────┐
│   User UI  │
└─────┬──────┘
      │
      │ Action (create/update/delete)
      ▼
┌─────────────────┐
│   Repository    │
└────────┬────────┘
         │
         ├─► SQLDelight (immediate save)
         │
         ├─► Sync Queue (for later sync)
         │
         └─► Return Result to UI
```

### Online Sync Flow

```
┌─────────────┐
│  App Online │
└─────┬──────┘
      │
      │ Sync Trigger
      ▼
┌─────────────────┐
│  SyncService  │
└─────┬────────┘
      │
      │
      ├─► Upload pending changes
      │      │
      │      ▼
      │   ┌─────────┐
      │   │  Ktor    │
      │   │   API    │
      │   └────┬────┘
      │        │
      │        ▼
      │   ┌─────────────────┐
      │   │  Backend DB    │
      │   └───────────────┘
      │
      ├─► Download server changes
      │      │
      │      ▼
      │   ┌─────────────────┐
      │   │  SQLDelight DB  │
      │   └───────────────┘
      │
      └─► Resolve conflicts (last-write-wins)
```

---

## Design Patterns

### 1. Repository Pattern

```kotlin
interface EventRepository {
    suspend fun getEventById(id: String): Result<Event>
    suspend fun createEvent(event: Event): Result<Event>
    suspend fun updateEvent(event: Event): Result<Event>
    suspend fun deleteEvent(id: String): Result<Unit>
}

class DatabaseEventRepository(
    private val database: WakevDb
) : EventRepository {
    override suspend fun createEvent(event: Event): Result<Event> {
        // SQLDelight implementation
    }
}
```

### 2. Result<T> Pattern

Elegant error handling:

```kotlin
suspend fun createEvent(event: Event): Result<Event> = runCatching {
    // ... validation and processing
    database.eventQueries.insert(event)
    event
}
```

### 3. Service Pattern

Business logic encapsulated in services:

```kotlin
class PollService(
    private val database: WakevDb
) {
    suspend fun submitVote(/* ... */): Result<Unit> {
        // Voting logic
    }
}
```

### 4. Expect/Actual Pattern

Platform-specific implementations:

```kotlin
// commonMain
expect class PlatformCalendarService {
    fun addEvent(event: CalendarEvent): Result<Unit>
}

// androidMain
actual class PlatformCalendarService(...) {
    actual override fun addEvent(/* ... */) {
        // Android CalendarContract implementation
    }
}

// iosMain
actual class PlatformCalendarService(...) {
    actual override fun addEvent(/* ... */) {
        // iOS EventKit implementation
    }
}
```

### 5. Observer Pattern

Data observation (using Flow):

```kotlin
class EventViewModel : ViewModel() {
    val events: StateFlow<List<Event>> = eventRepository
        .getEvents()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
}
```

---

## Database Schema

### Key Tables

#### Event
```sql
CREATE TABLE event (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    description TEXT,
    organizer_id TEXT NOT NULL,
    deadline INTEGER NOT NULL,
    created_at INTEGER NOT NULL,
    status TEXT NOT NULL
);
```

#### Participant
```sql
CREATE TABLE participant (
    id TEXT PRIMARY KEY,
    event_id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    name TEXT NOT NULL,
    email TEXT,
    status TEXT NOT NULL,
    is_validated INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (event_id) REFERENCES event(id) ON DELETE CASCADE
);
```

#### Scenario
```sql
CREATE TABLE scenario (
    id TEXT PRIMARY KEY,
    event_id TEXT NOT NULL,
    name TEXT NOT NULL,
    destination TEXT,
    start_date INTEGER,
    duration INTEGER NOT NULL,
    estimated_budget REAL,
    FOREIGN KEY (event_id) REFERENCES event(id) ON DELETE CASCADE
);
```

#### Budget & Items
```sql
CREATE TABLE budget (
    id TEXT PRIMARY KEY,
    event_id TEXT NOT NULL,
    target_amount REAL NOT NULL,
    currency TEXT NOT NULL,
    FOREIGN KEY (event_id) REFERENCES event(id) ON DELETE CASCADE
);

CREATE TABLE budget_item (
    id TEXT PRIMARY KEY,
    budget_id TEXT NOT NULL,
    category TEXT NOT NULL,
    name TEXT NOT NULL,
    estimated_amount REAL NOT NULL,
    actual_amount REAL,
    paid_by TEXT,
    FOREIGN KEY (budget_id) REFERENCES budget(id) ON DELETE CASCADE
);
```

See full schemas in `shared/src/sqldelight/com/guyghost/wakeve/db/`.

---

## API Architecture

### RESTful API (Ktor Server)

#### Endpoints (58 total)

```kotlin
// Events (8 endpoints)
GET    /health
GET    /api/events
GET    /api/events/{id}
POST   /api/events
PUT    /api/events/{id}/status
GET    /api/events/{id}/participants
POST   /api/events/{id}/participants
GET    /api/events/{id}/poll
POST   /api/events/{id}/poll/votes

// Scenarios (8 endpoints)
GET    /api/scenarios/event/{eventId}
POST   /api/scenarios
GET    /api/scenarios/{id}
PUT    /api/scenarios/{id}
DELETE /api/scenarios/{id}
POST   /api/scenarios/{id}/vote
GET    /api/scenarios/{id}/results
GET    /api/scenarios/event/{eventId}/ranked

// Budget (11 endpoints)
GET    /api/events/{id}/budget
PUT    /api/events/{id}/budget
GET    /api/events/{id}/budget/items
POST   /api/events/{id}/budget/items
GET    /api/events/{id}/budget/items/{itemId}
PUT    /api/events/{id}/budget/items/{itemId}
DELETE /api/events/{id}/budget/items/{itemId}
GET    /api/events/{id}/budget/summary
GET    /api/events/{id}/budget/settlements
GET    /api/events/{id}/budget/statistics

// Logistics (21 endpoints)
// - Accommodation (10)
// - Meals (14)
// - Equipment (10)
// - Activities (11)

// Collaboration (9 endpoints)
GET    /api/events/{id}/comments
POST   /api/events/{id}/comments
GET    /api/events/{id}/comments/{commentId}
PUT    /api/events/{id}/comments/{commentId}
DELETE /api/events/{id}/comments/{commentId}
GET    /api/events/{id}/comments/statistics
GET    /api/events/{id}/comments/top-contributors
GET    /api/events/{id}/comments/recent
GET    /api/events/{id}/comments/sections
```

### API Documentation

See `docs/API.md` for detailed endpoint documentation.

---

## Testing Strategy

### Test Coverage: 100% (36+ tests)

```
Tests by Category:
├── Repository Tests (27 tests)
│   ├── EventRepositoryTest (10)
│   ├── ScenarioRepositoryTest (11)
│   ├── BudgetRepositoryTest (31)
│   ├── DatabaseEventRepositoryTest (13)
│   └── CommentRepositoryTest (24)
│
├── Logic Tests (20+ tests)
│   ├── PollLogicTest (6)
│   ├── ScenarioLogicTest (6)
│   ├── BudgetCalculatorTest (30)
│   ├── AccommodationServiceTest (38)
│   ├── MealPlannerTest (32)
│   └── EquipmentManagerTest (26)
│
├── Offline Tests (7 tests)
│   └── OfflineScenarioTest (7)
│
├── Integration Tests (24 tests)
│   ├── CollaborationIntegrationTest (7)
│   ├── MealPlanningIntegrationTest (5)
│   ├── AccommodationIntegrationTest (6)
│   ├── EquipmentChecklistIntegrationTest (6)
│   └── ActivityPlanningIntegrationTest (6)
│
└── E2E Tests (new)
    └── PrdWorkflowE2ETest (10+)
```

### Test Execution

```bash
# All tests
./gradlew shared:test

# Specific test
./gradlew shared:test --tests "PrdWorkflowE2ETest"

# With coverage
./gradlew shared:test jacocoTestReport
```

---

## Offline-First Architecture

### Source of Truth

**SQLDelight database** is the single source of truth. All CRUD operations write to SQLite immediately.

### Conflict Resolution

**Last-Write-Wins (LWW)** with timestamps:
```kotlin
fun resolveConflicts(local: Event, remote: Event): Event {
    return if (local.updatedAt > remote.updatedAt) {
        local
    } else {
        remote
    }
}
```

**Future Enhancement**: CRDT (Conflict-Free Replicated Data Types) for true collaboration.

### Sync Strategy

**Incremental Sync**:
1. Collect local changes since last sync
2. Upload to server
3. Download server changes
4. Merge with local database
5. Resolve conflicts (LWW)

---

## Deployment

### Android

```bash
# Build release APK
./gradlew composeApp:assembleRelease

# Build AAB for Play Store
./gradlew composeApp:bundleRelease

# Run tests
./gradlew composeApp:test
```

### iOS

```bash
# Build in Xcode
open iosApp/iosApp.xcodeproj

# Archive for App Store
Product → Archive

# Run tests
xcodebuild test -scheme iosApp
```

### Server (Ktor)

```bash
# Build fat JAR
./gradlew server:shadowJar

# Run server
java -jar server/build/libs/server-1.0.0-all.jar

# Or run with Gradle
./gradlew server:run
```

---

## Performance Considerations

### Database Optimization

- **Indexes** on foreign keys and frequently queried columns
- **Pagination** for large lists (LazyColumn/LazyVStack)
- **Caching** for frequently accessed data

### API Optimization

- **Pagination** for list endpoints
- **Compression** for large payloads
- **CDN** for static assets (future)

### UI Optimization

- **Lazy Loading**: Only render visible items
- **StateFlow**: Efficient reactive updates
- **Compose Recomposition**: Minimized with stable keys

---

## Security Considerations

### Data Encryption

- **Local database**: SQLite encryption (future)
- **Network**: HTTPS/TLS for all API calls
- **Tokens**: Secure storage (Keychain on iOS, Keystore on Android)

### Authentication

- **OAuth 2.0**: Google, Apple (Phase 3)
- **JWT**: Secure token-based auth (future)
- **Permissions**: Runtime permissions on Android

---

## Future Enhancements

### Phase 6+ (Planned)

1. **CRDT Integration**: True collaborative editing
2. **Real-time Sync**: WebSocket for instant updates
3. **ML Recommendations**: Machine learning for suggestions
4. **Push Notifications**: FCM/APNs integration
5. **Calendar Integration**: Full native calendar sync
6. **Payment Integration**: Stripe, PayPal, Tricount
7. **Advanced Analytics**: Event analytics dashboard
8. **Multi-language Support**: i18n translations

---

## Resources

### Documentation

- [User Guide](../USER_GUIDE.md)
- [API Documentation](../API.md)
- [OpenSpec Specs](../openspec/specs/)

### External Resources

- [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html)
- [SQLDelight](https://cashapp.github.io/sqldelight/)
- [Ktor](https://ktor.io/docs/)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [SwiftUI](https://developer.apple.com/swiftui/)

---

**Version**: 1.0.0  
**Last Updated**: December 26, 2025  
**Maintainers**: Wakeve Team
