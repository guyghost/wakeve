# Wakeve - Collaborative Event Planning

![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Status](https://img.shields.io/badge/status-Phase%202%20Complete-brightgreen.svg)
![Tests](https://img.shields.io/badge/tests-36%2F36%20passing-brightgreen.svg)
![Platforms](https://img.shields.io/badge/platforms-Android%20|%20iOS%20|%20JVM-blue.svg)

Wakeve is a modern, collaborative event planning application that solves the scheduling problem for distributed teams. With intelligent availability polling, automatic best-time calculation, and offline-first synchronization, Wakeve makes it easy to find a time that works for everyone.

## 🎯 Features

### Current (Phase 2 ✅)
✅ **Event Organization**
- Create events with multiple time slot options
- Invite participants and manage RSVPs
- Real-time availability polling
- Weighted voting system (YES=2, MAYBE=1, NO=-1)
- Automatic best-time calculation
- Offline-first database persistence

✅ **Multiplatform Support**
- Android with Jetpack Compose UI
- iOS with native database driver (UI in Phase 2)
- JVM/Desktop support
- Single shared codebase via Kotlin Multiplatform

✅ **Backend Infrastructure**
- Production-ready Ktor REST API
- SQLDelight type-safe database
- 8 comprehensive endpoints
- Role-based access control

### Planned (Phase 3 🚀)
⏳ **User Authentication** - OAuth2 with Google/Apple  
⏳ **Offline Sync** - Automatic change synchronization  
⏳ **Push Notifications** - Deadline reminders and updates  
⏳ **Calendar Integration** - Native calendar app support  

## 🚀 Quick Start

```bash
# Clone repository
git clone https://github.com/guyghost/wakeve.git
cd wakeve

# Build and test
./gradlew build
./gradlew shared:test  # 36 tests passing ✅

# Start server
./gradlew server:run   # http://localhost:8080

# Build Android app
./gradlew composeApp:assembleDebug
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
├── iosApp/              # iOS app entry point (SwiftUI)
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

### Key Technologies
- **Language**: Kotlin 2.2.20 with Multiplatform support
- **UI**: Jetpack Compose (Android), SwiftUI (iOS)
- **Database**: SQLDelight with type-safe queries
- **Backend**: Ktor 3.3.1 REST server
- **Testing**: Kotlin test framework, 36+ tests
- **Serialization**: kotlinx-serialization for JSON

## 📊 Project Statistics

| Metric | Value |
|--------|-------|
| Phase Status | 2 Complete, 3 Planning |
| Tests Passing | 36/36 (100%) ✅ |
| Lines of Code | ~3,500 |
| Files Created | 30+ |
| API Endpoints | 8 |
| Database Tables | 6 |
| Supported Platforms | 3 (Android, iOS, JVM) |

## 📖 Documentation

- **[QUICK_START.md](./QUICK_START.md)** - 5-minute setup guide
- **[CONTRIBUTING.md](./CONTRIBUTING.md)** - Development guidelines
- **[IMPLEMENTATION_CHECKLIST.md](./IMPLEMENTATION_CHECKLIST.md)** - Phase 2 completion
- **[openspec/specs/](./openspec/specs/)** - Detailed specifications

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
./gradlew composeApp:assembleDebug

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
────────────────────────────────────
TOTAL                       36 tests ✅
```

All tests cover:
- ✅ Event creation and lifecycle
- ✅ Participant management
- ✅ Vote submission and aggregation
- ✅ Database persistence
- ✅ Offline data recovery
- ✅ API endpoints

## 📡 REST API

### Available Endpoints
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

### Current (Phase 2)
- Static user IDs (for development)
- Role-based access control (organizer vs participant)
- Input validation on all endpoints
- Error handling with appropriate HTTP status codes

### Planned (Phase 3)
- OAuth2 authentication (Google, Apple)
- Secure token storage and refresh
- HTTPS enforcement
- Rate limiting and request validation

## 📱 Platform Support

### Android
- **UI Framework**: Jetpack Compose
- **Target**: API 24+
- **Build**: `./gradlew composeApp:assembleDebug`

### iOS
- **Framework**: Swift/SwiftUI (planned Phase 2)
- **Target**: iOS 13+
- **Database**: Native SQLite driver

### JVM/Server
- **Framework**: Ktor REST server
- **Database**: SQLDelight with JDBC driver
- **Run**: `./gradlew server:run`

## 🚦 Development Workflow

### Creating a Feature
1. Create feature branch: `change/<feature-name>`
2. Follow OpenSpec process (see [CONTRIBUTING.md](./CONTRIBUTING.md))
3. Write tests for all new code
4. Submit PR with issue reference

### Git Commit Format
```
[#<issue>] <type>: <description>

<optional body>
```

**Examples:**
```
[#2] feat: Implement event creation API
[#15] fix: Handle timezone conversion
[#20] test: Add offline sync scenarios
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
