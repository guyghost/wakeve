# Contributing to Wakeve

## Overview
Wakeve is an event planning application built with Kotlin Multiplatform (KMP), featuring collaborative availability polling, automatic scheduling, and offline-first synchronization.

## Project Structure

```
wakeve/
├── shared/                    # Kotlin Multiplatform shared code
│   ├── src/commonMain/       # Cross-platform code
│   ├── src/androidMain/      # Android-specific code
│   ├── src/iosMain/          # iOS-specific code
│   ├── src/jvmMain/          # JVM-specific code
│   ├── src/commonTest/       # Cross-platform tests
│   ├── src/jvmTest/          # JVM-specific tests
│   └── src/commonMain/sqldelight/  # Database schema
├── composeApp/              # Android app with Jetpack Compose
│   └── src/commonMain/       # Shared Compose UI
├── server/                   # Ktor REST backend
│   └── src/main/kotlin/      # Server API endpoints
├── iosApp/                   # Native iOS SwiftUI app and Xcode project
└── openspec/                 # Specification documents
    ├── changes/              # Change proposals
    └── specs/                # Capability specifications
```

## Development Workflow

### OpenSpec Process
Wakeve follows the OpenSpec process for structured feature development:

1. **Create GitHub Issue** with change ID (e.g., `add-event-organization`)
2. **Create Feature Branch** with naming: `codex/<change-id>` for Codex work
3. **Create Proposal**: `openspec/changes/<change-id>/proposal.md`
4. **Create Spec Delta**: `openspec/changes/<change-id>/specs/<capability>/spec.md`
5. **Get Approval** on PR before implementation
6. **Implement** with tests and documentation
7. **Merge** to main after code review

### Key Files
- `openspec/AGENTS.md` - AI assistant instructions and conventions
- `openspec/PROCESS.md` - Simplified workflow documentation
- `ROADMAP.md` - Current release-hardening roadmap
- `QUICK_START.md` - Current setup and validation guide

## Getting Started

### Prerequisites
- Kotlin 2.2.20
- Java 11+
- Gradle 8.14+
- Android SDK (for Android development)
- Xcode with an available iOS simulator (for iOS development)

### Building the Project

```bash
# Build all modules
./gradlew build

# Run tests
./gradlew :shared:jvmTest

# Run specific test class
./gradlew :shared:jvmTest --tests "EventRepositoryTest"

# Build Android app
./gradlew :composeApp:assembleDebug

# Run Ktor server
./gradlew :server:run
```

### Development Environment Setup

1. **Clone repository**:
   ```bash
   git clone https://github.com/guyghost/wakeve.git
   cd wakeve
   ```

2. **Create feature branch**:
   ```bash
   git checkout -b codex/<change-id>
   ```

3. **Install dependencies**:
   ```bash
   ./gradlew build
   ```

4. **Run tests**:
   ```bash
   ./gradlew :shared:jvmTest
   ```

## Architecture

### Multiplatform Architecture
- **Domain Models**: Kotlin common module (shared across platforms)
- **Business Logic**: Platform-agnostic services
- **Persistence**: SQLDelight for type-safe database access
- **UI**: Jetpack Compose (Android), SwiftUI (iOS)
- **Backend**: Ktor server with REST API

### Dependency Flow
```
UI Layer (Compose/SwiftUI)
    ↓
Business Logic (Repository, Services)
    ↓
Database Layer (SQLDelight)
    ↓
SQLite (Platform-specific drivers)

API Layer
    ↓
Ktor Server
    ↓
Database Layer
```

## Code Style Guide

### Kotlin Conventions
- Follow [Kotlin Style Guide](https://kotlinlang.org/docs/coding-conventions.html)
- Use `camelCase` for variables and functions
- Use `PascalCase` for classes and interfaces
- Maximum line length: 120 characters

### Package Organization
```kotlin
com.guyghost.wakeve/
├── models/           // Data classes and domain models
├── repositories/     // Data access layer
├── services/         // Business logic
├── routes/           // API endpoints (server only)
├── ui/              // UI screens (composeApp only)
└── database/        // SQLDelight factories
```

### Documentation
- Add comments for complex logic
- Document public APIs with KDoc
- Keep README up-to-date
- Update OpenSpec documents for major changes

### Naming Conventions
- Use descriptive names (avoid abbreviations)
- Use `is/has/can` prefixes for boolean functions
- Use `get/set` for accessor patterns
- Use `Test` suffix for test classes

## Testing Requirements

### Test Coverage Targets
- Domain models: 90%+ coverage
- Business logic: 85%+ coverage
- API endpoints: 80%+ coverage
- Overall: 80%+ coverage

### Test Types
1. **Unit Tests**: Single function/class in isolation
2. **Integration Tests**: Multiple components working together
3. **Scenario Tests**: Real-world usage patterns

### Writing Tests
```kotlin
class EventRepositoryTest {
    private lateinit var repository: EventRepository
    
    fun setup() {
        repository = EventRepository()
    }
    
    @Test
    fun testCreateEvent() {
        // Arrange
        val event = Event(/* ... */)
        
        // Act
        val result = repository.createEvent(event)
        
        // Assert
        assertTrue(result.isSuccess)
    }
}
```

### Running Tests
```bash
# Run shared JVM tests
./gradlew :shared:jvmTest

# Run with coverage report
./gradlew :shared:testWithCoverage :shared:jacocoCoverageVerification

# Run Android unit tests where available
./gradlew :composeApp:testDebugUnitTest
```

## Git Workflow

### Branch Naming
- Feature/Codex work: `codex/<change-id>` (e.g., `codex/add-event-organization`)
- Bugfix: `fix/<issue-number>-<description>`
- Documentation: `docs/<description>`

### Commit Message Format
```
<type>(optional-scope): <description>

<optional body>
<optional footer>
```

**Types**: feat, fix, docs, refactor, test, chore

**Examples**:
```
feat(events): implement event creation API endpoint
fix(timezone): handle timezone conversion correctly
test(sync): add offline sync scenario tests
```

### Code Review Process
1. Push feature branch to remote
2. Create Pull Request with issue reference
3. Ensure CI/CD checks pass
4. Request review from maintainers
5. Address review comments
6. Merge when approved

## Dependencies Management

### Adding Dependencies
1. Add to `gradle/libs.versions.toml`
2. Reference in appropriate `build.gradle.kts`
3. Document the dependency purpose
4. Update this guide if significant

### Current Major Dependencies
- **Kotlin**: 2.2.20
- **Compose Multiplatform**: 1.9.1
- **Ktor**: 3.3.1
- **SQLDelight**: 2.0.2
- **kotlinx-serialization**: 1.7.3
- **kotlinx-coroutines**: 1.10.2

## Troubleshooting

### Common Issues

**Build fails with "Unresolved reference"**
- Run `./gradlew clean build`
- Check SQLDelight `.sq` files syntax
- Verify all imports are correct

**Tests fail intermittently**
- Check for timing issues or race conditions
- Ensure tests are properly isolated
- Use `TestDatabaseFactory` for database tests

**Database migration errors**
- Verify `.sq` file syntax
- Check table constraints and foreign keys
- Review migration files in `openspec/changes/`

**Ktor server won't start**
- Check port 8080 is available
- Verify database initialization succeeds
- Check server logs for detailed errors

## Performance Guidelines

### Mobile Performance
- Keep UI updates under 16ms (60fps target)
- Minimize database queries (use proper indexing)
- Lazy-load large lists
- Cache static data appropriately

### Server Performance
- Index frequently-queried columns
- Use connection pooling
- Implement request batching
- Monitor response times

### Database Performance
- Use SQLDelight indexes
- Avoid N+1 queries
- Batch operations when possible
- Regular maintenance (VACUUM)

## Security Guidelines

### Authentication
- Never store plaintext passwords
- Use the existing OAuth/email/guest authentication flows and secure token storage patterns
- Rotate tokens regularly
- Validate all user inputs

### Data Protection
- Encrypt sensitive data at rest
- Use HTTPS for all API communication
- Validate input size and format
- Log security-relevant events

### API Security
- Implement rate limiting
- Use authentication headers
- Validate request signatures
- Return minimal error information

## Documentation

### When to Document
- Any public API or public method
- Complex business logic
- Architecture decisions
- Configuration options

### Documentation Format
```kotlin
/**
 * Creates a new event with the given details.
 *
 * @param event The event to create
 * @return Result containing the created event or error
 * @throws IllegalArgumentException if event details are invalid
 */
fun createEvent(event: Event): Result<Event>
```

## Release Process

### Release Steps
1. Update version in `build.gradle.kts`
2. Update `CHANGELOG.md`
3. Create release branch: `release/v<version>`
4. Tag commit: `v<version>`
5. Create GitHub release with notes
6. Deploy to production

### Version Scheme
- Format: `MAJOR.MINOR.PATCH`
- Breaking changes: bump MAJOR
- New features: bump MINOR
- Bug fixes: bump PATCH

## Reporting Issues

### Issue Template
```
Title: [Component] Brief description

## Description
What's the problem?

## Steps to Reproduce
1. Step 1
2. Step 2
3. Step 3

## Expected Behavior
What should happen?

## Actual Behavior
What actually happens?

## Environment
- OS: (Android/iOS/JVM)
- Version: X.Y.Z
- Branch: codex/...
```

## Useful Commands

```bash
# View git history
git log --oneline -10

# Check branch status
git status

# Update local repo
git fetch origin
git pull origin main

# Create feature branch
git checkout -b codex/<change-id>

# Run specific test
./gradlew :shared:jvmTest --tests "TestClassName"

# Format code
./gradlew spotlessApply

# Check code quality
./gradlew detekt

# Generate documentation
./gradlew dokka
```

## Contact & Support

- **Issues**: Use GitHub Issues for bugs and features
- **Discussions**: Use GitHub Discussions for general questions
- **Security**: Report security issues privately to maintainers

## License
Wakeve is licensed under the MIT License. See LICENSE file for details.

## Acknowledgments
Built with Kotlin Multiplatform, Jetpack Compose, and Ktor.

---

**Happy Contributing!** 🚀
