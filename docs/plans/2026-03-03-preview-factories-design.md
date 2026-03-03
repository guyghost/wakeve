# Preview Factories — Dev Experience Without Auth

**Date**: 2026-03-03
**Status**: Approved
**Approach**: Platform-native preview factories (Approach 1)

## Problem

Developing new screens requires passing through authentication and navigating to the target screen on every rebuild. Combined with lack of realistic test data, this slows iteration significantly.

## Solution

Each platform gets a dedicated `Preview/` module with:
1. **Factories** — builders that generate model variants (empty, complete, past, cancelled...)
2. **Preview Environment** — injects mocked auth state + stubbed services
3. **Per-view previews** — each View file includes its own previews following a standard convention

All preview code compiles only in DEBUG — zero production impact.

## Architecture

### Directory Structure

```
iosApp/src/
├── Preview/
│   ├── Factories/                 # EventFactory, UserFactory, etc.
│   ├── PreviewEnvironment.swift   # Auth mock + service stubs
│   └── PreviewHelpers.swift       # Utility extensions
├── Views/                         # (existing)
└── ViewModels/                    # (existing)

composeApp/src/
├── preview/
│   ├── factories/                 # EventFactory, UserFactory, etc.
│   ├── PreviewProviders.kt       # @PreviewParameter providers
│   └── PreviewTheme.kt           # Compose wrapper with mocked state
├── ui/                            # (existing)
└── viewmodel/                     # (existing)
```

### Preview Factories

Builder pattern with named variants + custom builder.

**iOS (Swift)**:
```swift
enum EventFactory {
    static var empty: Event { ... }
    static var complete: Event { ... }
    static var past: Event { ... }
    static var cancelled: Event { ... }
    static var withPoll: Event { ... }
    static var withManyParticipants: Event { ... }

    static func make(
        title: String = "Weekend Trip",
        date: Date = .now.addingTimeInterval(86400 * 7),
        participants: [User] = UserFactory.group(count: 5),
        status: EventStatus = .confirmed
    ) -> Event { ... }
}
```

**Android (Kotlin)**:
```kotlin
object EventFactory {
    val empty get() = Event(...)
    val complete get() = Event(...)
    val past get() = Event(...)

    fun make(
        title: String = "Weekend Trip",
        date: Instant = Clock.System.now().plus(7.days),
        participants: List<User> = UserFactory.group(5),
        status: EventStatus = EventStatus.CONFIRMED
    ): Event = Event(...)
}
```

**Factories needed**: EventFactory, UserFactory, PollFactory, ScenarioFactory, MessageFactory, InvitationFactory.

Each factory uses realistic data (real names, relative dates, placeholder images).

### Preview Environment (Auth Bypass)

**iOS — ViewModifier**:
```swift
struct PreviewEnvironment: ViewModifier {
    var user: User = UserFactory.organizer
    var isAuthenticated: Bool = true

    func body(content: Content) -> some View {
        content
            .environmentObject(MockAuthStateManager(user: user, isAuthenticated: isAuthenticated))
            .environmentObject(MockEventService())
            .environmentObject(MockNavigationRouter())
    }
}
```

`MockAuthStateManager` subclasses `AuthStateManager`:
- Always returns `isAuthenticated = true`
- Provides a mocked `currentUser`
- No network calls
- `signIn()` / `signOut()` are no-ops

**Android — CompositionLocal**:
```kotlin
@Composable
fun PreviewTheme(
    user: User = UserFactory.organizer,
    content: @Composable () -> Unit
) {
    WakeveTheme {
        CompositionLocalProvider(
            LocalAuthState provides AuthState(isAuthenticated = true, user = user),
            LocalEventService provides MockEventService(),
        ) {
            content()
        }
    }
}
```

### Preview Convention

Each View file includes previews at the bottom:

```swift
// MARK: - Previews

#Preview("Inbox - With notifications") {
    InboxView()
        .modifier(PreviewEnvironment())
}

#Preview("Inbox - Empty") {
    InboxView()
        .modifier(PreviewEnvironment())
}
```

**Naming**: `"ScreenName - State"` for clear listing in the Xcode canvas.

**Minimum coverage**: 2 previews per screen (happy path + empty state). Complex screens add variants (error, loading, edge cases).

### What We Don't Do

- No separate preview catalog file — previews live in their View files
- No snapshot testing — can be added later
- No network mocking — services return in-memory data directly
- No KMP shared mocks — each platform uses native factories for fastest compilation

## Success Criteria

- Any View file opens with instant preview in Xcode/Android Studio
- No authentication flow needed to see any screen
- Realistic data visible in all previews
- Zero impact on production binary size
