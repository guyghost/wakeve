# Wakeve - Collaborative Event Planning

![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Status](https://img.shields.io/badge/status-Roadmap%20Active-blue.svg)
![OpenSpec](https://img.shields.io/badge/openspec-26%2F26%20passing-brightgreen.svg)
![Platforms](https://img.shields.io/badge/platforms-Android%20|%20iOS%20|%20JVM-blue.svg)

Wakeve is a collaborative event planning application for groups that need one place to decide when to meet, who is coming, what the plan is, and what still needs organizing. It combines Kotlin Multiplatform shared logic, Android Compose, native SwiftUI iOS, a Ktor backend, offline-first persistence, and OpenSpec-driven release tracking.

## 🎯 Features

### Current Roadmap State

The project is beyond the original Phase 2/Phase 3 README plan. Current source-of-truth tracking lives in [ROADMAP.md](./ROADMAP.md), [openspec/specs/](./openspec/specs/), and the active OpenSpec changes.

Active changes:

| Change | Status | Remaining blocker |
|---|---:|---|
| `add-event-weather-forecast` | 20/22 tasks | WeatherKit Apple Developer capability, signed entitlement, and physical-device WeatherKit validation. |
| `add-on-device-wakeve-ai` | 40/41 tasks | Supported physical-device profiling for Foundation Models latency, cancellation, memory, and production-log privacy. |

Recent archived roadmap work includes Android AI workflows, Android adaptive UI, contact participant selection, scenario matrix voting, web microfrontends, account deletion, UGC moderation, iOS brand identity, and create-event slot previews.

### Implemented Capabilities

✅ **Event Organization**
- Create events with multiple time slot options
- Invite participants and manage RSVPs
- Real-time availability polling
- Weighted voting system (YES=2, MAYBE=1, NO=-1)
- Automatic best-time calculation
- Offline-first database persistence
- DRAFT → POLLING → CONFIRMED → ORGANIZING → FINALIZED workflow coordination

✅ **Virtual Meetings**
- MeetingService with support for Zoom, Google Meet, FaceTime
- Secure meeting link generation via backend proxy
- Meeting invitation and reminder scheduling
- Integration with native calendar
- Meeting lifecycle management (create, update, cancel, start, end)

✅ **Multiplatform Support**
- Android with Jetpack Compose UI
- iOS with native SwiftUI app, shared KMP integration, App Store readiness evidence, and WeatherKit/WakeveAI work in progress
- JVM/Desktop support
- Single shared codebase via Kotlin Multiplatform

✅ **Backend Infrastructure**
- Production-ready Ktor REST API
- SQLDelight type-safe database
- Event, meeting, auth, moderation, calendar, and organization APIs
- Role-based access control
- Secure API key management for external platforms

✅ **Release Readiness**
- App Store blocker register and final signoff workflow
- Local privacy, accessibility, media, license, export, live URL, and Store metadata evidence docs
- Account deletion and UGC moderation implementations archived after local validation
- Critical release gate scripts for OpenSpec, iOS metadata, web, and selected regression tests

## 🚀 Quick Start

```bash
# Clone repository
git clone https://github.com/guyghost/wakeve.git
cd wakeve

# Build and test
./gradlew build
./gradlew :shared:jvmTest
openspec validate --all --strict

# Start server
./gradlew server:run   # http://localhost:8080

# Build Android app
./gradlew :composeApp:assembleDebug
```

See [QUICK_START.md](./QUICK_START.md) for detailed setup instructions.

## 📁 Project Structure

```
wakeve/
├── shared/               # Kotlin Multiplatform shared code
│   ├── src/commonMain/  # Cross-platform models & logic
│   ├── src/jvmTest/     # JVM-specific tests
│   └── sqldelight/      # Type-safe database schema
├── composeApp/          # Android app with Jetpack Compose
├── server/              # Ktor REST backend server
├── iosApp/              # Native iOS app and Xcode project
├── openspec/            # Specification documents
└── docs/                # Documentation
```

## 🏗️ Architecture

### Multiplatform Layers
```
┌─────────────────────────────────────┐
│  UI Layer (Compose/SwiftUI)         │
├─────────────────────────────────────┤
│  Business Logic (EventRepository)   │
├─────────────────────────────────────┤
│  Persistence (SQLDelight)           │
├─────────────────────────────────────┤
│  Platform Drivers (Android/iOS/JVM) │
└─────────────────────────────────────┘
```

### Architecture Pattern: Functional Core & Imperative Shell (FC&IS)

Wakeve follows the **Functional Core, Imperative Shell** pattern to ensure testability and separation of concerns:

```
┌─────────────────────────────────────────────────────────┐
│                    IMPERATIVE SHELL                  │
│  ┌───────────────────────────────────────────────────┐  │
│  │  Services (MeetingService, EventRepository)      │  │
│  │  State Machines (MVI FSM)                      │  │
│  │  External APIs (MeetingProxyRoutes)              │  │
│  └───────────────────────────────────────────────────┘  │
│  Handles side effects: I/O, async, state mutations   │
└─────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────┐
│                     FUNCTIONAL CORE                   │
│  ┌───────────────────────────────────────────────────┐  │
│  │  Pure Functions (logic, validators, transforms)  │  │
│  │  Domain Models (Event, Meeting, Vote)           │  │
│  │  Business Rules (scoring, eligibility)           │  │
│  └───────────────────────────────────────────────────┘  │
│  No side effects, 100% testable                      │
└─────────────────────────────────────────────────────────┘
```

**Key Principles:**
- ✅ Shell CAN call Core
- ✅ Core CANNOT call Shell
- ✅ Core ignores Shell's existence
- ✅ Pure functions in Core are easily testable
- ✅ Side effects isolated in Shell (database, network, API)

See [docs/architecture/meeting-service.md](./docs/architecture/meeting-service.md) for detailed MeetingService architecture.

### Key Technologies
- **Language**: Kotlin 2.2.20 with Multiplatform support
- **UI**: Jetpack Compose (Android), SwiftUI (iOS)
- **Database**: SQLDelight with type-safe queries
- **Backend**: Ktor 3.3.1 REST server
- **Testing**: Kotlin test, XCTest, OpenSpec validation, release gate scripts
- **Serialization**: kotlinx-serialization for JSON

## 📊 Project Statistics

| Metric | Value |
|--------|-------|
| Roadmap Status | Active release hardening |
| Active OpenSpec Changes | 2 |
| Validated Specs/Changes | 26/26 with `openspec validate --all --strict` |
| Shared Test Files | 92 common test files |
| Android Test Files | 85 Compose test files |
| iOS Test Files | 28 XCTest files |
| Supported Platforms | Android, iOS, JVM/server |
| Current External Blockers | App Store/live infrastructure, signed-device WeatherKit, WakeveAI real-device profiling |

## 📖 Documentation

### Getting Started
- **[QUICK_START.md](./QUICK_START.md)** - 5-minute setup guide
- **[CONTRIBUTING.md](./CONTRIBUTING.md)** - Development guidelines
- **[AGENTS.md](./AGENTS.md)** - OpenSpec workflow and AI agents

### Complete Documentation
For comprehensive documentation, visit **[docs/](./docs/)**:
- [Architecture](./docs/architecture/README.md) - System architecture and KMP patterns
- [API Documentation](./docs/API.md) - REST API endpoints
- [Testing](./docs/testing/README.md) - Testing strategy and reports
- [Integrations](./docs/integrations/README.md) - Calendar, OAuth, and external services
- [Migration Guides](./docs/migration/README.md) - Design system migrations
- [Refactoring Docs](./docs/refactoring/README.md) - Major refactorings
- [Implementation Status](./docs/implementation/prd-status.md) - PRD feature tracking

### OpenSpec
- **[openspec/specs/](./openspec/specs/)** - Detailed specifications
- **[openspec/AGENTS.md](./openspec/AGENTS.md)** - Specification-driven development workflow

## 🔧 Development

### Prerequisites
- Java 11+
- Kotlin 2.2.20
- Gradle 8.14+
- Android SDK (for Android development)
- Xcode 15+ (for iOS development)

### Build Commands
```bash
# Run all tests
./gradlew shared:test

# Run specific test
./gradlew shared:test --tests "EventRepositoryTest"

# Build Android app
./gradlew :composeApp:assembleDebug

# Start server
./gradlew server:run

# Format code
./gradlew spotlessApply
```

## 🧪 Testing

Wakeve has comprehensive test coverage:

```
EventRepositoryTest          10 tests
PollLogicTest               6 tests
DatabaseEventRepositoryTest 13 tests
OfflineScenarioTest         7 tests
───────────────────────────────────
Unit Tests                  36 tests ✅

PrdWorkflowE2ETest          6 tests
ServiceIntegrationE2ETest    5 tests
MultiUserCollaborationTest   10 tests
DeleteEventE2ETest           6 tests
AuthFlowE2ETest              4 tests
───────────────────────────────────
E2E Tests                  35 tests ✅

TOTAL                      71 tests ✅
```

All tests cover:
- ✅ Event creation and lifecycle
- ✅ Participant management
- ✅ Vote submission and aggregation
- ✅ Database persistence
- ✅ Offline data recovery
- ✅ API endpoints
- ✅ Complete PRD workflow (DRAFT → FINALIZED)
- ✅ Multi-user collaboration scenarios
- ✅ Virtual meeting link generation
- ✅ Service integration (Budget, Transport, Meeting, Suggestion)

## 📡 REST API

### Event Endpoints
```
GET    /health                    # Health check
GET    /api/events                # List all events
GET    /api/events/{id}           # Get event details
POST   /api/events                # Create event
PUT    /api/events/{id}/status    # Update event status
GET    /api/events/{id}/participants   # List participants
POST   /api/events/{id}/participants   # Add participant
GET    /api/events/{id}/poll      # Get poll results
POST   /api/events/{id}/poll/votes    # Submit vote
```

### Meeting Proxy Endpoints (Secure)
```
POST   /api/meetings/proxy/zoom/create              # Create Zoom meeting
POST   /api/meetings/proxy/google-meet/create       # Create Google Meet meeting
POST   /api/meetings/proxy/zoom/{id}/cancel        # Cancel Zoom meeting
GET    /api/meetings/proxy/zoom/{id}/status        # Get Zoom meeting status
```

**Security Note:** Meeting proxy endpoints secure external API keys (Zoom, Google Meet) by handling all external API calls server-side.

See [docs/api/meeting-api.md](./docs/api/meeting-api.md) for detailed MeetingProxy API documentation.

### Example: Create Event
```bash
curl -X POST http://localhost:8080/api/events \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Team Meeting",
    "description": "Q4 Planning",
    "organizerId": "user-1",
    "deadline": "2025-11-20T18:00:00Z",
    "proposedSlots": [{
      "id": "slot-1",
      "start": "2025-12-01T10:00:00Z",
      "end": "2025-12-01T12:00:00Z",
      "timezone": "UTC"
    }]
  }'
```

## 🔐 Security

### Current
- OAuth/email/guest authentication flows in shared and platform code
- Role-based access control for organizer and participant behavior
- Account deletion flow implemented and locally validated
- UGC moderation, reporting, blocking, and review controls implemented and archived
- Store-readiness checks for privacy manifests, logging hygiene, live URL/AASA evidence, App Review notes, and release signoff markers

## 📱 Platform Support

### Android
- **UI Framework**: Jetpack Compose
- **Target**: API 24+
- **Build**: `./gradlew :composeApp:assembleDebug`

### iOS
- **Framework**: SwiftUI with shared KMP framework integration
- **Project**: `iosApp/iosApp.xcodeproj`
- **Target**: tracked in Xcode build settings
- **Database**: Native SQLite driver

### JVM/Server
- **Framework**: Ktor REST server
- **Database**: SQLDelight with JDBC driver
- **Run**: `./gradlew server:run`

## 🚦 Development Workflow

### Creating a Feature
1. Create feature branch: `codex/<feature-name>` for Codex work
2. Follow OpenSpec process (see [CONTRIBUTING.md](./CONTRIBUTING.md))
3. Write tests for all new code
4. Submit PR with issue reference

### Git Commit Format
```
<type>(optional-scope): <description>

<optional body>
```

**Examples:**
```
feat(events): add event creation API
fix(timezone): handle poll slot conversion
test(sync): add offline conflict scenarios
```

## 🐛 Troubleshooting

### Common Issues

**Build Fails**
```bash
./gradlew clean build  # Clean rebuild
java -version          # Verify Java 11+
```

**Tests Failing**
```bash
./gradlew shared:test --info  # Verbose output
./gradlew shared:test --tests "TestName" -d  # Debug mode
```

**Server Won't Start**
```bash
lsof -i :8080          # Check port 8080
./gradlew server:run --info  # Debug mode
```

See [QUICK_START.md](./QUICK_START.md) for more solutions.

## 📋 Workflow

Wakeve follows the **OpenSpec** specification-driven development process:

```
1. Create Issue → 2. Create Proposal → 3. Create Spec → 
4. Get Approval → 5. Implement with Tests → 6. Merge & Deploy
```

See [openspec/PROCESS.md](./openspec/PROCESS.md) for detailed workflow.

## 🤝 Contributing

We welcome contributions! Please see [CONTRIBUTING.md](./CONTRIBUTING.md) for:
- Development setup
- Code style guidelines
- Testing requirements
- Commit conventions
- Pull request process

## 📞 Support

- **Issues**: Create GitHub Issue for bugs/features
- **Discussions**: Use GitHub Discussions for questions
- **Documentation**: See full docs in repository
- **Email**: Contact maintainers for security issues

## 📄 License

Wakeve is licensed under the MIT License. See [LICENSE](./LICENSE) file for details.

## 🙏 Acknowledgments

Built with:
- [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Ktor](https://ktor.io/)
- [SQLDelight](https://cashapp.github.io/sqldelight/)

## 🎯 Vision

Wakeve's mission is to make collaborative scheduling effortless. By combining intelligent polling, automatic scheduling, and offline-first principles, we're building the event planning tool for distributed teams.

---

**Ready to contribute?** Start with [QUICK_START.md](./QUICK_START.md) and [CONTRIBUTING.md](./CONTRIBUTING.md)!

**Questions?** Check [openspec/](./openspec/) for detailed specifications and documentation.
