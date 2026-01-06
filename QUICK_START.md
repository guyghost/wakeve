# Wakeve Quick Start Guide

Welcome to Wakeve! This guide will get you up and running in minutes.

## What is Wakeve?
Wakeve is a collaborative event planning application that makes scheduling across timezones easy. Features include:
- **Availability Polling**: Create events and let participants vote on their preferred times
- **Smart Scheduling**: Automatic calculation of best meeting time based on weighted voting
- **Offline-First**: Work without internet and sync changes when back online
- **Multiplatform**: Android, iOS, and web support

## Project Status

**Phase 2 Complete** ✅
- Core event organization with polls and voting
- Multiplatform database (Android/iOS/JVM)
- REST API server
- Android Compose UI
- 36 comprehensive tests

**Phase 3 Planning** 🚀
- User authentication (OAuth2)
- Offline-first synchronization
- Push notifications
- Calendar integration

## Quick Start (5 minutes)

### Prerequisites
```bash
# Check you have required tools
java -version          # Java 11 or higher
kotlin -version        # Kotlin 2.2.20
gradle --version       # Gradle 8.14+
```

### Clone & Build
```bash
# Clone the repository
git clone https://github.com/guyghost/wakeve.git
cd wakeve

# Build all modules (takes 2-3 minutes first time)
./gradlew build

# Run tests to verify everything works
./gradlew shared:test
# Expected: 36/36 tests passing ✅
```

### Explore the Project
```bash
# View project structure
tree -I 'build|.gradle' -L 2

# Check git history
git log --oneline | head -20

# View current branch and status
git branch -v
git status
```

## What's Included

### Phase 1: Core Features (Implemented)
✅ Event creation with time slots
✅ Participant management
✅ Availability polling (YES/MAYBE/NO)
✅ Automatic best time calculation
✅ Role-based access control
✅ 4 Android Compose screens

### Phase 2: Database & API (Implemented)
✅ SQLDelight multiplatform database
✅ 6 database tables with constraints
✅ Platform-specific drivers (Android/iOS/JVM)
✅ Ktor REST server
✅ 8 API endpoints
✅ Offline data recovery

### Phase 2.5: iOS Tab Interface (Implemented)
✅ 3 functional tabs (Events, Explore, Profile)
✅ Events tab with filtering and pull-to-refresh
✅ Explore tab with suggestions and event ideas
✅ Profile tab with preferences and dark mode
✅ Liquid Glass design system applied
✅ 27 reusable UI components
✅ 75/89 tasks completed (84%)

### Phase 2.6: First Time Onboarding (Implemented)
✅ 4-screen onboarding flow (Android + iOS)
✅ Material You design for Android
✅ Liquid Glass design for iOS
✅ Local persistence (SharedPreferences/UserDefaults)
✅ Skip functionality
✅ 35 automated tests (25 Android + 10 iOS)
✅ 30/38 tasks completed (79%)

## iOS Interface - Tab Navigation

The iOS application features a tab-based navigation with 3 main tabs:

### 📅 Events Tab
**Purpose**: View and manage your events

**Features:**
- **Event Filtering**: Filter events by status (upcoming, in progress, past)
- **Event List**: Display all events in a scrollable list with status badges
- **Pull-to-Refresh**: Pull down to refresh the event list
- **Empty State**: CTA to create new event when no events exist
- **Event Details**: Tap on any event to view details and participate

**UI Components:**
- `EventsTabView.swift` - Main view with NavigationStack
- `EventFilter` enum - Filter options (upcoming, inProgress, past)
- `EventRowView` - Reusable event card component
- `EventStatusBadge` - Color-coded status indicator
- `EmptyEventsView` - Empty state with "Create Event" CTA

### 🔍 Explore Tab
**Purpose**: Discover new events and features

**Features:**
- **Daily Suggestions**: Featured event idea of the day
- **Event Ideas**: Browse event categories (weekend, team building, birthday, party)
- **New Features**: Discover latest app features (Liquid Glass, Navigation, Collaboration)
- **Interactive Cards**: Tap on any card to create an event

**UI Components:**
- `ExploreTabView.swift` - Main view with ScrollView
- `DailySuggestionSection` - Featured card with gradient background
- `EventIdeasSection` - Grid of event idea cards
- `NewFeaturesSection` - Feature announcement cards
- `EventIdeaCard` - Reusable idea card component
- `FeatureCard` - Feature announcement component

### 👤 Profile Tab
**Purpose**: Manage your account and preferences

**Features:**
- **Profile Header**: Display avatar, name, and email
- **Notifications Toggle**: Enable/disable push and email notifications
- **Dark Mode Toggle**: Switch between light and dark themes
- **Liquid Glass Option**: Toggle Liquid Glass material (iOS 26+)
- **About Section**: App version, documentation links, GitHub repo
- **Sign Out**: Sign out button connected to AuthStateManager

**UI Components:**
- `ProfileTabView.swift` - Main view with ScrollView
- `ProfileHeaderSection` - Avatar and user info
- `PreferencesSection` - Toggle switches for notifications
- `AppearanceSection` - Theme and dark mode controls
- `AboutSection` - Version info and links
- `PreferenceToggleRow` - Reusable toggle row component
- `AboutRow` - Info row component
- `AboutLinkRow` - Link row component

### Design System Applied

**Liquid Glass (iOS 26+)**
- Materials: `.regularMaterial`, `.thinMaterial`, `.thickMaterial`
- Corners: `.continuous` style for all rounded corners
- Shadows: Subtle shadows (opacity 0.05-0.08)
- Vibrancy: Automatic text/icon adaptation to background

**Wakev Colors**
- Primary: #2563EB (blue) - Main actions, active states
- Accent: #7C3AED (purple) - Secondary highlights
- Success: #059669 (green) - Confirmed status
- Warning: #D97706 (orange) - In-progress state
- Error: #DC2626 (red) - Sign out button, errors

**Typography**
- Title3: Large headings
- Headline: Section titles
- Subheadline: Card titles
- Body: Main content text
- Caption: Small helper text

### State Management

- **@State**: Local component state
- **@AppStorage**: Persisted preferences (darkMode, notificationsEnabled)
- **@EnvironmentObject**: Shared state (AuthStateManager)

### Navigation

- **NavigationStack**: Each tab has its own navigation stack
- **Tab Bar**: Bottom tab bar with 4 tabs (Events, Explore, Profile, +)
- **Routes**: Navigation between views within each tab

### Phase 3: Enterprise Features (Planning)
⏳ OAuth2 authentication
⏳ Offline-first synchronization
⏳ Push notifications
⏳ Native calendar integration

## First Time User Experience

### Onboarding Flow

When you launch the app for the first time, you'll be greeted with a beautiful onboarding experience:

**Android (Material You)**
- 4-screen horizontal pager introducing key features
- Material Design 3 components and animations
- Skip button to jump directly to the app
- "Get Started" button to complete onboarding

**iOS (Liquid Glass)**
- 4-page TabView with smooth transitions
- Liquid Glass materials and continuous corners
- Skip button in top-right corner
- "Get Started" button on final page

**Onboarding Screens:**
1. **Create Events** - Learn how to create and organize events
2. **Collaborate** - Invite participants and vote on dates
3. **Organize** - Manage scenarios, budgets, and logistics
4. **Enjoy** - Focus on the experience, not the planning

**Persistence:**
- Onboarding state is saved locally (Android SharedPreferences, iOS UserDefaults)
- Only shown on first launch
- Can be skipped at any time
- Does not reappear on subsequent launches

## Development Workflow

### 1. Start a New Feature
```bash
# Create a feature branch
git checkout -b change/your-feature-name

# Make your changes
# ... edit files ...

# Run tests
./gradlew shared:test

# Commit your work
git commit -m "[#123] Your feature description"

# Push to remote
git push origin change/your-feature-name
```

### 2. Run the Server
```bash
# Start Ktor server (listens on http://localhost:8080)
./gradlew server:run

# Test an endpoint
curl http://localhost:8080/health
# Expected response: OK

# View available endpoints
curl http://localhost:8080/api/events
```

### 3. Run Tests
```bash
# Run all tests
./gradlew shared:test

# Run specific test class
./gradlew shared:test --tests "EventRepositoryTest"

# Run a single test
./gradlew shared:test --tests "EventRepositoryTest.testCreateEvent"

# Run with verbose output
./gradlew shared:test --info
```

### 4. Build Android App
```bash
# Build debug APK
./gradlew composeApp:assembleDebug

# Build release APK
./gradlew composeApp:assembleRelease

# Run on emulator
./gradlew composeApp:installDebug
```

### 5. Build iOS App
```bash
# Open in Xcode
open wakeveApp/wakeveApp.xcodeproj

# Build in Xcode (Cmd+B)
# Run in Simulator (Cmd+R)
```

## Key Files to Know

### Specifications
- `openspec/specs/event-organization/spec.md` - Full requirements
- `openspec/changes/implement-tabs-content/tasks.md` - iOS tabs implementation tasks

### Implementation - Shared
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/models/` - Domain models
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/EventRepository.kt` - Business logic
- `shared/src/commonMain/sqldelight/` - Database schema

### Implementation - iOS
- `iosApp/iosApp/Views/` - SwiftUI views (EventsTabView, ExploreTabView, ProfileTabView)
- `iosApp/iosApp/Components/` - Reusable components (WakevTabBar, SharedComponents)
- `iosApp/iosApp/Theme/` - Design system (WakevColors, LiquidGlassModifier)

### Testing
- `shared/src/commonTest/kotlin/com/guyghost/wakeve/EventRepositoryTest.kt` - Domain tests
- `shared/src/jvmTest/kotlin/com/guyghost/wakeve/DatabaseEventRepositoryTest.kt` - Persistence tests
- `shared/src/jvmTest/kotlin/com/guyghost/wakeve/OfflineScenarioTest.kt` - Offline tests

### Configuration
- `gradle/libs.versions.toml` - Dependency versions
- `shared/build.gradle.kts` - Shared module config
- `iosApp/iosApp.xcodeproj/project.pbxproj` - Xcode project config

## Understanding the Architecture

### Layer 1: Domain Models
Classes representing core concepts:
- `Event` - Event with participants and time slots
- `TimeSlot` - Proposed meeting time with timezone
- `Vote` - Participant's preference (YES/MAYBE/NO)
- `Poll` - Collection of votes

### Layer 2: Business Logic
Services handling operations:
- `EventRepository` - In-memory event management
- `PollLogic` - Vote scoring and slot calculation

### Layer 3: Database
Persistent storage:
- `SQLDelight` - Type-safe database queries
- `DatabaseEventRepository` - Database-backed implementation
- Platform-specific drivers (Android/iOS/JVM)

### Layer 4: API
REST endpoints:
- `EventRoutes` - Event CRUD and status updates
- `ParticipantRoutes` - Participant management
- `VoteRoutes` - Vote submission and results

### Layer 5: UI - Android
User interfaces:
- `EventCreationScreen` - Create events
- `ParticipantManagementScreen` - Invite participants
- `PollVotingScreen` - Cast votes
- `PollResultsScreen` - View results and confirm

### Layer 5: UI - iOS
User interfaces:
- `EventsTabView` - View and filter events
- `ExploreTabView` - Discover new features and ideas
- `ProfileTabView` - Manage preferences and account
- `ContentView` - Main app with tab navigation

## API Examples

### Create an Event
```bash
curl -X POST http://localhost:8080/api/events \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Team Retreat 2025",
    "description": "Annual team planning",
    "organizerId": "org-123",
    "deadline": "2025-11-20T18:00:00Z",
    "proposedSlots": [
      {
        "id": "slot-1",
        "start": "2025-12-01T10:00:00Z",
        "end": "2025-12-01T12:00:00Z",
        "timezone": "UTC"
      }
    ]
  }'
```

### Submit a Vote
```bash
curl -X POST http://localhost:8080/api/events/event-1/poll/votes \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "event-1",
    "participantId": "user-123",
    "slotId": "slot-1",
    "vote": "YES"
  }'
```

### View Poll Results
```bash
curl http://localhost:8080/api/events/event-1/poll
```

## Common Tasks

### Add a New Feature
1. Create feature branch: `git checkout -b change/my-feature`
2. Create proposal: `openspec/changes/my-feature/proposal.md`
3. Create spec: `openspec/specs/my-capability/spec.md`
4. Implement with tests
5. Submit PR with issue reference

### Run on Android Device
1. Connect Android device via USB
2. Enable USB debugging
3. Run: `./gradlew composeApp:installDebug`
4. App appears in launcher as "Wakeve"

### Run on iOS Simulator
1. Open project: `open iosApp/iosApp.xcodeproj`
2. Select simulator (e.g., iPhone 15)
3. Press Cmd+R to run

### Debug Tests
```bash
# Run with debug output
./gradlew shared:test --info

# Run single test with debug
./gradlew shared:test --tests "TestClass.testMethod" -d

# View test reports
open build/reports/tests/test/index.html
```

### Update Dependencies
1. Edit `gradle/libs.versions.toml`
2. Update version numbers
3. Run: `./gradlew --refresh-dependencies`
4. Run tests to verify compatibility

## Troubleshooting

### Build Fails
```bash
# Clean build caches
./gradlew clean

# Full rebuild
./gradlew build

# Check Java version
java -version # Should be 11 or higher
```

### Tests Failing
```bash
# Run tests with more verbose output
./gradlew shared:test --info

# Run single failing test
./gradlew shared:test --tests "FailingTest" -d

# Check test logs
find . -name "*.log" -type f | head -5
```

### Server Won't Start
```bash
# Check port 8080 is not in use
lsof -i :8080

# Run with debug output
./gradlew server:run --info

# Check database initialization
ls -la wakev_server.db
```

### Database Errors
```bash
# Regenerate database interface
./gradlew shared:generateSqlDelightInterface

# Clean database
rm wakev.db
rm wakev_server.db

# Rebuild project
./gradlew clean build
```

### iOS Build Errors
```bash
# Clean Xcode build
cd iosApp
xcodebuild clean

# Rebuild Kotlin Shared framework
cd ..
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64

# Reopen Xcode
open iosApp/iosApp.xcodeproj
```

## Next Steps

### For Developers
1. ✅ Read `IMPLEMENTATION_CHECKLIST.md` to understand Phase 2 completion
2. ✅ Explore `PHASE_3_ROADMAP.md` for upcoming features
3. ✅ Review `CONTRIBUTING.md` for development guidelines
4. 🚀 Start working on Phase 3 features or bug fixes

### For Contributors
1. Create GitHub Issue describing your feature/fix
2. Create feature branch: `change/<issue-id>-<description>`
3. Follow OpenSpec process (proposal → spec → implementation)
4. Write tests for all new code
5. Submit PR linking to issue

### For Project Maintainers
1. Review Phase 3 roadmap and timeline
2. Create GitHub Issues for Phase 3 sprints
3. Set up OAuth provider credentials (Google/Apple)
4. Plan Phase 3 Sprint 1 team assignments

## Learning Resources

### Architecture
- `shared/src/commonMain/kotlin/` - Domain models and business logic
- `iosApp/iosApp/Views/` - SwiftUI view implementations
- `composeApp/src/commonMain/kotlin/` - Android UI implementation
- `server/src/main/kotlin/` - API implementation

### Testing Examples
- `EventRepositoryTest.kt` - Unit testing patterns
- `DatabaseEventRepositoryTest.kt` - Integration testing
- `OfflineScenarioTest.kt` - Scenario testing

### Documentation
- `openspec/specs/event-organization/spec.md` - Requirements
- `openspec/changes/implement-tabs-content/IMPLEMENTATION_SUMMARY.md` - iOS tabs summary
- `iosApp/LIQUID_GLASS_GUIDELINES.md` - Liquid Glass design guidelines
- `CONTRIBUTING.md` - Development guidelines

## Getting Help

**Documentation**: See README.md and docs/ folder
**Issues**: Create GitHub Issue with detailed description
**Discussions**: Use GitHub Discussions for questions
**Code Review**: Submit PR for feedback from maintainers

## Key Statistics

| Metric | Value |
|--------|-------|
| Total Tests | 36 passing ✅ |
| iOS Tabs | 3 implemented ✅ |
| iOS Components | 27 reusable ✅ |
| Code Files | 30+ |
| Lines of Code | ~4500 |
| Database Tables | 6 |
| API Endpoints | 8 |
| Supported Platforms | Android, iOS, JVM |
| Build Time | ~2-3 minutes |

## Commands Cheat Sheet

```bash
# Development
git checkout -b change/your-feature      # Create feature branch
./gradlew build                          # Full build
./gradlew shared:test                    # Run tests
./gradlew server:run                     # Run server

# iOS
open iosApp/iosApp.xcodeproj            # Open in Xcode
xcodebuild -project iosApp.xcodeproj -scheme iosApp build  # Build from CLI

# Testing
./gradlew shared:test --tests "TestName" # Run specific test
./gradlew shared:test --info             # Verbose output
./gradlew server:compileKotlin          # Compile server

# Code Quality
./gradlew spotlessApply                  # Format code
./gradlew detekt                         # Check code style

# Android
./gradlew composeApp:build               # Build Android app
./gradlew composeApp:installDebug        # Install on device

# Git
git status                               # Check changes
git commit -m "[#123] Description"       # Commit with issue ref
git push origin change/your-feature      # Push branch
```

---

**Ready to contribute?** Pick an issue or start with Phase 3 planning! 🚀
