# Preview Factories Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Enable auth-free screen development with rich SwiftUI Previews and Compose Previews using factory-generated mock data.

**Architecture:** Each platform gets a `Preview/` module with model factories (builder + named variants), a mock auth environment, and per-view previews. Preview code compiles only in DEBUG. No KMP shared mocks — each platform uses native factories for fastest compilation.

**Tech Stack:** SwiftUI `#Preview` macro (iOS 17+), Jetpack Compose `@Preview`, Swift enums with static factories, Kotlin objects with factory methods.

---

### Task 1: iOS — UserFactory

**Files:**
- Create: `iosApp/src/Preview/Factories/UserFactory.swift`

**Step 1: Create UserFactory with named variants and builder**

```swift
import Foundation

#if DEBUG

enum UserFactory {

    // MARK: - Named Variants

    static var organizer: User {
        User(id: "user-organizer-001", name: "Marie Dupont", email: "marie@example.com", avatarUrl: nil)
    }

    static var participant: User {
        User(id: "user-participant-002", name: "Lucas Martin", email: "lucas@example.com", avatarUrl: nil)
    }

    static var guest: User {
        User(id: "user-guest-003", name: "Invité", email: "guest@example.com", avatarUrl: nil)
    }

    static var withAvatar: User {
        User(id: "user-avatar-004", name: "Sophie Bernard", email: "sophie@example.com", avatarUrl: "https://i.pravatar.cc/150?u=sophie")
    }

    // MARK: - Builder

    static func make(
        id: String = "user-\(UUID().uuidString.prefix(8))",
        name: String = "Test User",
        email: String = "test@example.com",
        avatarUrl: String? = nil
    ) -> User {
        User(id: id, name: name, email: email, avatarUrl: avatarUrl)
    }

    // MARK: - Collections

    static func group(count: Int) -> [User] {
        let names = ["Marie Dupont", "Lucas Martin", "Sophie Bernard", "Thomas Leroy", "Emma Petit", "Hugo Moreau", "Léa Fournier", "Nathan Girard"]
        return (0..<min(count, names.count)).map { i in
            User(id: "user-group-\(i)", name: names[i], email: "\(names[i].lowercased().replacingOccurrences(of: " ", with: "."))@example.com", avatarUrl: nil)
        }
    }
}

#endif
```

**Step 2: Verify it compiles**

Run: `xcodebuild build -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 16' -configuration Debug CODE_SIGNING_ALLOWED=NO 2>&1 | tail -5`
Expected: BUILD SUCCEEDED

**Step 3: Commit**

```bash
git add iosApp/src/Preview/Factories/UserFactory.swift
git commit -m "feat(ios): add UserFactory for preview mock data"
```

---

### Task 2: iOS — EventFactory

**Files:**
- Create: `iosApp/src/Preview/Factories/EventFactory.swift`

**Step 1: Create EventFactory with variants covering key screen states**

Note: The iOS app imports Event from the Shared KMP framework. The Event model is `Shared.Event` with properties: `id`, `title`, `description_`, `organizerId`, `participants`, `proposedSlots`, `deadline`, `status`, `finalDate`, `createdAt`, `updatedAt`, `eventType`, `eventTypeCustom`, `minParticipants`, `maxParticipants`, `expectedParticipants`, `heroImageUrl`. Status values are from `Shared.EventStatus` enum: `draft`, `polling`, `comparing`, `confirmed`, `organizing`, `finalized`. Check the actual Swift interface generated from KMP to get exact property names and types (they may be bridged as NSString, KotlinInt, etc.).

Since the iOS `SharedModels.swift` defines its own `User` struct but may use KMP `Event` or its own wrapper, check what `ModernEventDetailView` actually expects as input. If it uses a local Swift model, create the factory for that model. If it uses the KMP `Shared.Event`, create a factory that builds `Shared.Event` instances.

**Approach:** Look at how existing Views reference Event (e.g., `ExploreTabView`, `ModernEventDetailView`) and match the factory to whatever type those Views consume. Create the factory accordingly.

The factory should provide these variants:
- `empty` — minimal event, draft status, no participants
- `complete` — full event with participants, confirmed status, all fields populated
- `past` — event with finalDate in the past, finalized status
- `polling` — event in polling status with proposed time slots
- `cancelled` — event with draft status and empty description (closest to cancelled state)
- `withManyParticipants` — event with 15+ participants
- `make(...)` — builder with sensible defaults

**Step 2: Verify it compiles**

Run: `xcodebuild build -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 16' -configuration Debug CODE_SIGNING_ALLOWED=NO 2>&1 | tail -5`
Expected: BUILD SUCCEEDED

**Step 3: Commit**

```bash
git add iosApp/src/Preview/Factories/EventFactory.swift
git commit -m "feat(ios): add EventFactory for preview mock data"
```

---

### Task 3: iOS — InboxItemFactory and ScenarioFactory

**Files:**
- Create: `iosApp/src/Preview/Factories/InboxItemFactory.swift`
- Create: `iosApp/src/Preview/Factories/ScenarioFactory.swift`

**Step 1: Create InboxItemFactory**

Check how `InboxView.swift` models inbox items. The KMP shared module has `InboxItem` with: `id`, `type` (InboxItemType), `status` (InboxItemStatus), `title`, `subtitle`, `description`, `eventId`, `eventTitle`, `timestamp`, `isRead`, `commentCount`, `metadata`. Check if iOS uses the KMP model directly or wraps it.

Variants needed:
- `unreadInvitation` — EVENT_INVITATION type, ACTION_REQUIRED status, isRead=false
- `readUpdate` — POLL_UPDATE type, INFO status, isRead=true
- `actionRequired` — VOTE_REMINDER type, ACTION_REQUIRED status
- `completed` — EVENT_CONFIRMED type, SUCCESS status
- `make(...)` — builder
- `list(count:)` — generates a mixed list of inbox items

**Step 2: Create ScenarioFactory**

Check what `ExploreView.swift` and `ExploreScenarioDetailView.swift` use. The iOS explore tab may use `EventScenario` (a local Swift model) rather than KMP `Scenario`. Look at `ExploreViewModel.swift` to determine the exact model.

Variants needed:
- `birthday` — birthday party scenario
- `teamBuilding` — team building event
- `wedding` — wedding scenario
- `make(...)` — builder

**Step 3: Verify it compiles**

Run: `xcodebuild build -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 16' -configuration Debug CODE_SIGNING_ALLOWED=NO 2>&1 | tail -5`
Expected: BUILD SUCCEEDED

**Step 4: Commit**

```bash
git add iosApp/src/Preview/Factories/InboxItemFactory.swift iosApp/src/Preview/Factories/ScenarioFactory.swift
git commit -m "feat(ios): add InboxItemFactory and ScenarioFactory"
```

---

### Task 4: iOS — MockAuthStateManager

**Files:**
- Create: `iosApp/src/Preview/PreviewEnvironment.swift`

**Step 1: Create MockAuthStateManager and PreviewEnvironment ViewModifier**

`AuthStateManager` is a `@MainActor class` with `@Published` properties: `isAuthenticated`, `currentUser`, `isLoading`, `authError`. It requires `AuthenticationService` in its init.

For the mock, we need to either:
- Subclass `AuthStateManager` and override published properties (if possible given `@MainActor`)
- Or create a protocol that both real and mock conform to

Check if `AuthStateManager` methods are called from Views or just its `@Published` properties. If Views only read published properties, a subclass that sets them in init is sufficient.

```swift
import SwiftUI

#if DEBUG

// MARK: - Mock Auth State Manager

class MockAuthStateManager: AuthStateManager {

    init(user: User = UserFactory.organizer, isAuthenticated: Bool = true) {
        // AuthStateManager requires AuthenticationService — check if we can pass a minimal stub
        // or if we need to create a MockAuthenticationService too
        let mockAuthService = AuthenticationService() // May need adjustment based on AuthenticationService.init requirements
        super.init(authService: mockAuthService, enableOAuth: false)
        self.isAuthenticated = isAuthenticated
        self.currentUser = user
        self.isLoading = false
        self.authError = nil
    }

    // Override async methods to be no-ops
    override func signIn(provider: String?, authCode: String?, userInfo: String?, email: String?, fullName: String?) async {
        // no-op
    }

    override func signOut() {
        isAuthenticated = false
        currentUser = nil
    }

    override func checkAuthStatus() {
        // no-op — already set in init
    }

    override func refreshTokenIfNeeded() async {
        // no-op
    }
}

// MARK: - Preview Environment

struct PreviewEnvironment: ViewModifier {
    let user: User
    let isAuthenticated: Bool

    init(user: User = UserFactory.organizer, isAuthenticated: Bool = true) {
        self.user = user
        self.isAuthenticated = isAuthenticated
    }

    func body(content: Content) -> some View {
        content
            .environmentObject(MockAuthStateManager(user: user, isAuthenticated: isAuthenticated))
    }
}

extension View {
    func previewEnvironment(user: User = UserFactory.organizer, isAuthenticated: Bool = true) -> some View {
        self.modifier(PreviewEnvironment(user: user, isAuthenticated: isAuthenticated))
    }
}

#endif
```

**Important:** The exact implementation depends on `AuthenticationService.init` requirements. If `AuthenticationService` needs a base URL or network config, create a `MockAuthenticationService` stub too. Read `AuthenticationService.swift` init signature before implementing.

**Step 2: Verify it compiles**

Run: `xcodebuild build -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 16' -configuration Debug CODE_SIGNING_ALLOWED=NO 2>&1 | tail -5`
Expected: BUILD SUCCEEDED

**Step 3: Commit**

```bash
git add iosApp/src/Preview/PreviewEnvironment.swift
git commit -m "feat(ios): add MockAuthStateManager and PreviewEnvironment modifier"
```

---

### Task 5: iOS — Add previews to InboxView

**Files:**
- Modify: `iosApp/src/Views/InboxView.swift`

**Step 1: Add #Preview blocks at the bottom of InboxView.swift**

Read the file first to understand InboxView's dependencies (EnvironmentObjects, init parameters, state).

Add previews:
```swift
// MARK: - Previews

#Preview("Inbox - With Notifications") {
    InboxView()
        .previewEnvironment()
}

#Preview("Inbox - Empty") {
    InboxView()
        .previewEnvironment()
}
```

If `InboxView` fetches data from a service/ViewModel, the mock environment may need to provide a mock data source. Adjust based on what InboxView actually requires.

**Step 2: Open Xcode and verify previews render**

Run: `xcodebuild build -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 16' -configuration Debug CODE_SIGNING_ALLOWED=NO 2>&1 | tail -5`
Expected: BUILD SUCCEEDED

**Step 3: Commit**

```bash
git add iosApp/src/Views/InboxView.swift
git commit -m "feat(ios): add SwiftUI previews to InboxView"
```

---

### Task 6: iOS — Add previews to ExploreView

**Files:**
- Modify: `iosApp/src/Views/ExploreView.swift`

**Step 1: Read ExploreView.swift and its ViewModel dependencies**

The explore tab is already accessible without auth, but may lack preview configurations. Add previews showing different states (with scenarios loaded, empty state).

**Step 2: Add #Preview blocks**

```swift
#Preview("Explore - With Scenarios") {
    ExploreTabView()
        .previewEnvironment()
}
```

Adjust based on actual dependencies found in Step 1.

**Step 3: Verify it compiles**

Run: `xcodebuild build -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 16' -configuration Debug CODE_SIGNING_ALLOWED=NO 2>&1 | tail -5`
Expected: BUILD SUCCEEDED

**Step 4: Commit**

```bash
git add iosApp/src/Views/ExploreView.swift
git commit -m "feat(ios): add SwiftUI previews to ExploreView"
```

---

### Task 7: iOS — Add previews to ModernGetStartedView

**Files:**
- Modify: `iosApp/src/Views/ModernGetStartedView.swift`

**Step 1: Read the file and add preview**

This is the onboarding/get-started screen. It should work without auth by definition.

```swift
#Preview("Get Started") {
    ModernGetStartedView(onGetStarted: {})
        .previewEnvironment(isAuthenticated: false)
}
```

**Step 2: Verify it compiles**

Run: `xcodebuild build -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 16' -configuration Debug CODE_SIGNING_ALLOWED=NO 2>&1 | tail -5`
Expected: BUILD SUCCEEDED

**Step 3: Commit**

```bash
git add iosApp/src/Views/ModernGetStartedView.swift
git commit -m "feat(ios): add SwiftUI preview to ModernGetStartedView"
```

---

### Task 8: Android — EventFactory and UserFactory

**Files:**
- Create: `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/preview/factories/EventFactory.kt`
- Create: `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/preview/factories/UserFactory.kt`

**Step 1: Create UserFactory**

Uses the KMP `User` model from `com.guyghost.wakeve.auth.core.models.User`.

```kotlin
package com.guyghost.wakeve.preview.factories

import com.guyghost.wakeve.auth.core.models.AuthMethod
import com.guyghost.wakeve.auth.core.models.User

object UserFactory {

    val organizer get() = User.createAuthenticated(
        id = "user-organizer-001",
        email = "marie@example.com",
        name = "Marie Dupont",
        authMethod = AuthMethod.GOOGLE,
        currentTime = System.currentTimeMillis()
    )

    val participant get() = User.createAuthenticated(
        id = "user-participant-002",
        email = "lucas@example.com",
        name = "Lucas Martin",
        authMethod = AuthMethod.APPLE,
        currentTime = System.currentTimeMillis()
    )

    val guest get() = User.createGuest(
        id = "user-guest-003",
        currentTime = System.currentTimeMillis()
    )

    fun group(count: Int): List<User> {
        val names = listOf("Marie Dupont", "Lucas Martin", "Sophie Bernard", "Thomas Leroy", "Emma Petit", "Hugo Moreau", "Léa Fournier", "Nathan Girard")
        return (0 until minOf(count, names.size)).map { i ->
            User.createAuthenticated(
                id = "user-group-$i",
                email = "${names[i].lowercase().replace(" ", ".")}@example.com",
                name = names[i],
                authMethod = AuthMethod.GOOGLE,
                currentTime = System.currentTimeMillis()
            )
        }
    }
}
```

**Step 2: Create EventFactory**

Uses KMP `Event` model from `com.guyghost.wakeve.models.Event`.

```kotlin
package com.guyghost.wakeve.preview.factories

import com.guyghost.wakeve.models.*
import kotlinx.datetime.*

object EventFactory {

    private fun nowIso(): String = Clock.System.now().toString()
    private fun futureIso(days: Int): String = Clock.System.now().plus(days.toLong(), DateTimeUnit.DAY, TimeZone.UTC).toString()
    private fun pastIso(days: Int): String = Clock.System.now().minus(days.toLong(), DateTimeUnit.DAY, TimeZone.UTC).toString()

    val empty get() = Event(
        id = "event-empty",
        title = "Nouvel événement",
        description = "",
        organizerId = UserFactory.organizer.id,
        participants = emptyList(),
        proposedSlots = emptyList(),
        deadline = futureIso(14),
        status = EventStatus.DRAFT,
        createdAt = nowIso(),
        updatedAt = nowIso()
    )

    val complete get() = Event(
        id = "event-complete",
        title = "Weekend à Lyon",
        description = "Un super weekend entre amis pour découvrir la gastronomie lyonnaise.",
        organizerId = UserFactory.organizer.id,
        participants = UserFactory.group(5).map { it.id },
        proposedSlots = listOf(
            TimeSlot(id = "slot-1", start = futureIso(7), end = futureIso(9), timezone = "Europe/Paris"),
            TimeSlot(id = "slot-2", start = futureIso(14), end = futureIso(16), timezone = "Europe/Paris")
        ),
        deadline = futureIso(5),
        status = EventStatus.CONFIRMED,
        finalDate = futureIso(7),
        createdAt = pastIso(10),
        updatedAt = nowIso(),
        eventType = EventType.FOOD_TASTING,
        expectedParticipants = 8
    )

    val polling get() = Event(
        id = "event-polling",
        title = "Team Building Q2",
        description = "Activité team building pour le deuxième trimestre.",
        organizerId = UserFactory.organizer.id,
        participants = UserFactory.group(8).map { it.id },
        proposedSlots = listOf(
            TimeSlot(id = "slot-1", start = futureIso(10), end = futureIso(10), timezone = "Europe/Paris"),
            TimeSlot(id = "slot-2", start = futureIso(17), end = futureIso(17), timezone = "Europe/Paris"),
            TimeSlot(id = "slot-3", start = futureIso(24), end = futureIso(24), timezone = "Europe/Paris")
        ),
        deadline = futureIso(7),
        status = EventStatus.POLLING,
        createdAt = pastIso(3),
        updatedAt = nowIso(),
        eventType = EventType.TEAM_BUILDING,
        minParticipants = 5,
        maxParticipants = 15
    )

    val past get() = Event(
        id = "event-past",
        title = "Anniversaire Sophie",
        description = "Fête surprise pour les 30 ans de Sophie.",
        organizerId = UserFactory.organizer.id,
        participants = UserFactory.group(12).map { it.id },
        proposedSlots = listOf(
            TimeSlot(id = "slot-1", start = pastIso(30), end = pastIso(30), timezone = "Europe/Paris")
        ),
        deadline = pastIso(35),
        status = EventStatus.FINALIZED,
        finalDate = pastIso(30),
        createdAt = pastIso(45),
        updatedAt = pastIso(30),
        eventType = EventType.BIRTHDAY,
        expectedParticipants = 15
    )
}
```

**Note:** Check if `kotlinx-datetime` is already a dependency. If not, the factory can use simple ISO string literals instead.

**Step 3: Verify it compiles**

Run: `./gradlew composeApp:compileDebugKotlinAndroid 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add composeApp/src/androidMain/kotlin/com/guyghost/wakeve/preview/factories/
git commit -m "feat(android): add UserFactory and EventFactory for Compose previews"
```

---

### Task 9: Android — InboxItemFactory

**Files:**
- Create: `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/preview/factories/InboxItemFactory.kt`

**Step 1: Create InboxItemFactory**

Uses KMP `InboxItem` model.

```kotlin
package com.guyghost.wakeve.preview.factories

import com.guyghost.wakeve.models.*
import kotlinx.datetime.*

object InboxItemFactory {

    private fun nowIso(): String = Clock.System.now().toString()
    private fun pastIso(hours: Int): String = Clock.System.now().minus(hours.toLong(), DateTimeUnit.HOUR, TimeZone.UTC).toString()

    val unreadInvitation get() = InboxItem(
        id = "inbox-1",
        type = InboxItemType.EVENT_INVITATION,
        status = InboxItemStatus.ACTION_REQUIRED,
        title = "Invitation: Weekend à Lyon",
        subtitle = "Marie Dupont vous invite",
        eventId = "event-complete",
        eventTitle = "Weekend à Lyon",
        timestamp = pastIso(2),
        isRead = false
    )

    val readUpdate get() = InboxItem(
        id = "inbox-2",
        type = InboxItemType.POLL_UPDATE,
        status = InboxItemStatus.INFO,
        title = "Nouveau vote sur Team Building Q2",
        subtitle = "Lucas a voté pour le 15 mars",
        eventId = "event-polling",
        eventTitle = "Team Building Q2",
        timestamp = pastIso(5),
        isRead = true
    )

    val actionRequired get() = InboxItem(
        id = "inbox-3",
        type = InboxItemType.VOTE_REMINDER,
        status = InboxItemStatus.ACTION_REQUIRED,
        title = "N'oubliez pas de voter !",
        subtitle = "Il reste 3 jours pour voter",
        eventId = "event-polling",
        eventTitle = "Team Building Q2",
        timestamp = pastIso(1),
        isRead = false
    )

    val completed get() = InboxItem(
        id = "inbox-4",
        type = InboxItemType.EVENT_CONFIRMED,
        status = InboxItemStatus.SUCCESS,
        title = "Événement confirmé !",
        subtitle = "Weekend à Lyon est confirmé pour le 10 mars",
        eventId = "event-complete",
        eventTitle = "Weekend à Lyon",
        timestamp = pastIso(12),
        isRead = true
    )

    fun mixedList(count: Int = 6): List<InboxItem> {
        val templates = listOf(unreadInvitation, readUpdate, actionRequired, completed)
        return (0 until count).map { i ->
            val template = templates[i % templates.size]
            template.copy(
                id = "inbox-mixed-$i",
                timestamp = pastIso(i * 3 + 1)
            )
        }
    }
}
```

**Step 2: Verify it compiles**

Run: `./gradlew composeApp:compileDebugKotlinAndroid 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add composeApp/src/androidMain/kotlin/com/guyghost/wakeve/preview/factories/InboxItemFactory.kt
git commit -m "feat(android): add InboxItemFactory for Compose previews"
```

---

### Task 10: Android — PreviewTheme wrapper

**Files:**
- Create: `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/preview/PreviewTheme.kt`

**Step 1: Create PreviewTheme composable**

This wraps screens for previews without requiring auth or Koin DI.

```kotlin
package com.guyghost.wakeve.preview

import androidx.compose.runtime.Composable
import com.guyghost.wakeve.theme.WakeveTheme

@Composable
fun PreviewTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    WakeveTheme(
        darkTheme = darkTheme,
        dynamicColor = false, // Disable dynamic color in previews for consistency
        content = content
    )
}
```

**Note:** The current `WakeveTheme` accesses `LocalView.current` and casts to Activity, which crashes in previews. Check if there's already a `view.isInEditMode` guard. If not, the `PreviewTheme` may need to set `dynamicColor = false` and the theme itself may need a guard around the `SideEffect` block. Fix the theme if needed.

**Step 2: Verify it compiles**

Run: `./gradlew composeApp:compileDebugKotlinAndroid 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add composeApp/src/androidMain/kotlin/com/guyghost/wakeve/preview/PreviewTheme.kt
git commit -m "feat(android): add PreviewTheme wrapper for Compose previews"
```

---

### Task 11: Android — Add previews to InboxScreen

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/inbox/InboxScreen.kt`

**Step 1: Read the file and understand its parameters**

`InboxScreen` takes: `items: List<InboxItem>`, `isLoading`, `isRefreshing`, `onRefresh`, `onItemClick`, `onMarkAllRead`, `modifier`. This is a stateless composable — easy to preview.

**Step 2: Add preview composables**

Add at the bottom of the file (or in a separate androidMain preview file if the composable is in commonMain):

If InboxScreen is in `commonMain`, create a preview in `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/inbox/InboxScreenPreview.kt`:

```kotlin
package com.guyghost.wakeve.ui.inbox

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.guyghost.wakeve.preview.PreviewTheme
import com.guyghost.wakeve.preview.factories.InboxItemFactory

@Preview(showBackground = true)
@Composable
fun InboxScreenPreview() {
    PreviewTheme {
        InboxScreen(
            items = InboxItemFactory.mixedList(),
            onRefresh = {},
            onItemClick = {},
            onMarkAllRead = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun InboxScreenEmptyPreview() {
    PreviewTheme {
        InboxScreen(
            items = emptyList(),
            onRefresh = {},
            onItemClick = {},
            onMarkAllRead = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun InboxScreenLoadingPreview() {
    PreviewTheme {
        InboxScreen(
            items = emptyList(),
            isLoading = true,
            onRefresh = {},
            onItemClick = {},
            onMarkAllRead = {}
        )
    }
}
```

**Step 3: Verify it compiles**

Run: `./gradlew composeApp:compileDebugKotlinAndroid 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/inbox/
git commit -m "feat(android): add Compose previews for InboxScreen"
```

---

### Task 12: Android — Add previews to AuthScreen

**Files:**
- Create: `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/auth/AuthScreenPreview.kt` (if AuthScreen is in commonMain)

**Step 1: Read AuthScreen.kt to understand its parameters**

`AuthScreen` takes callbacks and state: `onGoogleSignIn`, `onAppleSignIn`, `onEmailSignIn`, `onSkip`, `isLoading`, `errorMessage`.

**Step 2: Add previews**

```kotlin
package com.guyghost.wakeve.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.guyghost.wakeve.preview.PreviewTheme

@Preview(showBackground = true)
@Composable
fun AuthScreenPreview() {
    PreviewTheme {
        AuthScreen(
            onGoogleSignIn = {},
            onAppleSignIn = {},
            onEmailSignIn = {},
            onSkip = {},
            isLoading = false,
            errorMessage = null
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AuthScreenLoadingPreview() {
    PreviewTheme {
        AuthScreen(
            onGoogleSignIn = {},
            onAppleSignIn = {},
            onEmailSignIn = {},
            onSkip = {},
            isLoading = true,
            errorMessage = null
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AuthScreenErrorPreview() {
    PreviewTheme {
        AuthScreen(
            onGoogleSignIn = {},
            onAppleSignIn = {},
            onEmailSignIn = {},
            onSkip = {},
            isLoading = false,
            errorMessage = "Connexion impossible. Vérifiez votre connexion internet."
        )
    }
}
```

**Step 3: Verify it compiles**

Run: `./gradlew composeApp:compileDebugKotlinAndroid 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/auth/
git commit -m "feat(android): add Compose previews for AuthScreen"
```

---

### Task 13: Verify all previews work end-to-end

**Files:** None (verification only)

**Step 1: Build iOS in Debug**

Run: `xcodebuild build -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 16' -configuration Debug CODE_SIGNING_ALLOWED=NO 2>&1 | tail -10`
Expected: BUILD SUCCEEDED

**Step 2: Build Android in Debug**

Run: `./gradlew composeApp:compileDebugKotlinAndroid 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL

**Step 3: Run existing tests to confirm no regressions**

Run: `./gradlew :shared:allTests 2>&1 | tail -20`
Expected: All 71 tests pass

**Step 4: Commit any fixes if needed, then final commit**

```bash
git add -A
git commit -m "chore: verify preview factories compile and tests pass"
```
