# Refonte iOS — Couche 2 (shell progressif) — Plan d'implémentation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Derrière le flag `iosRedesign2026`, remplacer le `TabView` 4 onglets par le shell de la refonte (barre flottante Événements · ＋ · Activité, profil en avatar, réglages en bouton rond) avec un `AppRouter` qui aiguille deep links et notifications — sans réécrire les écrans existants.

**Architecture (décision du 2026-09-28 : « shell progressif ») :** Le shell est une vue générique `RedesignShellView` qui garde les deux zones montées (comme un `TabView`, pour préserver leur état) et superpose `WKFloatingNavBar`. La zone Événements héberge **tel quel** l'aiguillage `homeTabContent` (`switch currentView`) de `AuthenticatedView` ; la zone Activité héberge `InboxView`. `AppRouter` (`@MainActor @Observable`) décide, pour chaque `IosRoute`, s'il s'agit d'un changement de zone / d'une présentation (Activité, profil, réglages, création) ou d'une route d'événement déléguée à la logique existante `handleDeepLinkNavigation`. Le vrai `NavigationStack` arrivera écran par écran quand chacun sera refondu (couches 3-5) ; `AppView` disparaît en couche 9.

**Tech Stack:** SwiftUI (iOS 18.2 min), Observation (`@Observable`), XCTest. Dossiers Xcode synchronisés (tout fichier créé sous `iosApp/src` ou `iosApp/WakeveTests` est inclus automatiquement ; ne pas éditer `project.pbxproj`).

**Spec :** `docs/superpowers/specs/2026-09-28-ios-redesign-design.md` §4 — **Proposition :** Swarm DAO #47.

**Écarts assumés vs spec (reportés dans la spec en Task 8) :**
- Pas de `NavigationStack(path:)` en couche 2 : la zone Événements garde l'aiguillage `AppView` ; migration écran par écran en couches 3-5.
- `presentedModule` / `EventModule` reportés à la couche 5 (aucun module en sheet avant).
- Le flag `iosInvitationExperienceV1` reste tel quel (tests source ancrés dessus) ; `FeatureFlags` ne porte que `iosRedesign2026` ; fusion en couche 9.
- Réglages : en couche 2, le bouton rond ouvre la sheet existante des préférences de notification.

---

## Contraintes et commandes

- Worktree : `/Users/guy/Developer/dev/wakeve/.claude/worktrees/ios-app-design-e1ae98`. Jamais `git stash`. Ne jamais indexer `.dao/*`. **Aucune ligne d'attribution (Co-Authored-By…) dans les messages de commit.**
- Test d'une classe :
  ```bash
  xcodebuild test -project iosApp/iosApp.xcodeproj -scheme WakeveApp \
    -destination 'platform=iOS Simulator,name=iPhone 18 Pro' \
    -only-testing:WakeveTests/<Classe> 2>&1 | tail -30
  ```
- Suite complète : `-only-testing:WakeveTests` (≈ 15 min). Baseline : seuls 5 échecs préexistants, tous dans `InvitationExperienceRuntimeSurfaceTests`.
- Ne jamais toucher au simulateur « Wakeve-QA-iPhone-16-Pro » ; jamais deux `xcodebuild` en parallèle.
- **Tests de contrat source ancrés dans `ContentView.swift` — à préserver à l'identique** (textes exacts, et ordre relatif) :
  - `TabView(selection: $selectedTab)` … `.tint(.wakevePrimary)` (PremiumNavigationContractTests)
  - `.fullScreenCover(isPresented: $showEventCreationSheet)` doit précéder `.sheet(isPresented: $showNotificationPreferencesSheet)` **et** `private var tabBarVisibility` dans le fichier (OrganizationPhase7, WakeveAIContractTests)
  - `private func handleDeepLinkNavigation(_ route: IosRoute)` suivi, plus bas, de `navigateToEvent` (plusieurs tests)
  - `private var homeTabContent` … `// MARK: - Tab Content`, `case .transportPlanning:` … `case .inbox:`, `private var invitationExperienceRootContent`, `@AppStorage("iosInvitationExperienceV1") private var iosInvitationExperienceV1 = false`
  - `WakeveTab.allCases == [.home, .groups, .messages, .profile]` (WakeveTab reste inchangé)

## Structure des fichiers

| Fichier | Action | Responsabilité |
|---|---|---|
| `iosApp/src/Models/FeatureFlags.swift` | Créer | Clés et lecture des flags de la refonte |
| `iosApp/src/Navigation/AppRouter.swift` | Créer | État du shell (zone, présentations) + plan de routage pur |
| `iosApp/src/Services/NotificationDeepLink.swift` | Créer | Extraction de l'URL d'un tap de notification (corrige le bug `deepLink` ignoré) |
| `iosApp/src/iOSApp.swift` | Modifier | Utiliser `NotificationDeepLink` |
| `iosApp/src/Views/App/RedesignShellView.swift` | Créer | Conteneur des zones + en-tête + barre flottante |
| `iosApp/src/Views/App/ContentView.swift` | Modifier | Brancher le shell derrière le flag dans `AuthenticatedView` |
| `iosApp/src/Resources/{en,fr,es,it,pt}.lproj/Localizable.strings` | Modifier | Clés `wk.nav.profile`, `wk.nav.settings` |
| `iosApp/WakeveTests/FeatureFlagsTests.swift` | Créer | |
| `iosApp/WakeveTests/AppRouterTests.swift` | Créer | |
| `iosApp/WakeveTests/NotificationDeepLinkTests.swift` | Créer | |
| `iosApp/WakeveTests/RedesignShellTests.swift` | Créer | |

---

### Task 1 : `FeatureFlags`

**Files:** Create `iosApp/src/Models/FeatureFlags.swift` ; Test `iosApp/WakeveTests/FeatureFlagsTests.swift`

- [ ] **Step 1 : Test**

```swift
import XCTest
@testable import Wakeve

final class FeatureFlagsTests: XCTestCase {
    private let suiteName = "FeatureFlagsTests"

    override func tearDown() {
        UserDefaults().removePersistentDomain(forName: suiteName)
        super.tearDown()
    }

    func testRedesignFlagIsOffByDefault() {
        let defaults = UserDefaults(suiteName: suiteName)!
        XCTAssertFalse(FeatureFlags.isRedesign2026Enabled(in: defaults))
    }

    func testRedesignFlagReadsItsKey() {
        let defaults = UserDefaults(suiteName: suiteName)!
        defaults.set(true, forKey: FeatureFlags.redesign2026Key)
        XCTAssertTrue(FeatureFlags.isRedesign2026Enabled(in: defaults))
    }

    func testRedesignKeyIsStable() {
        // Utilisée par @AppStorage et par l'argument de lancement `-iosRedesign2026 YES`.
        XCTAssertEqual(FeatureFlags.redesign2026Key, "iosRedesign2026")
    }
}
```

- [ ] **Step 2 : Lancer** `-only-testing:WakeveTests/FeatureFlagsTests` → BUILD FAILED (`FeatureFlags` introuvable).

- [ ] **Step 3 : Implémenter**

```swift
import Foundation

/// Flags de déploiement de la refonte iOS (proposition #47).
/// Activable en debug via l'argument de lancement `-iosRedesign2026 YES`
/// (domaine d'arguments de UserDefaults).
enum FeatureFlags {
    static let redesign2026Key = "iosRedesign2026"

    static func isRedesign2026Enabled(in defaults: UserDefaults = .standard) -> Bool {
        defaults.bool(forKey: redesign2026Key)
    }
}
```

- [ ] **Step 4 : Lancer** → `** TEST SUCCEEDED **` (3 tests).

- [ ] **Step 5 : Commit**

```bash
git add iosApp/src/Models/FeatureFlags.swift iosApp/WakeveTests/FeatureFlagsTests.swift
git commit -m "feat(ios): add FeatureFlags with iosRedesign2026 rollout flag

Refs Swarm DAO #47 (layer 2)."
```

### Task 2 : `AppRouter` et plan de routage

**Files:** Create `iosApp/src/Navigation/AppRouter.swift` ; Test `iosApp/WakeveTests/AppRouterTests.swift`

- [ ] **Step 1 : Tests**

```swift
import XCTest
@testable import Wakeve

@MainActor
final class AppRouterTests: XCTestCase {

    func testZoneAndPresentationRoutesAreHandledByTheShell() {
        XCTAssertEqual(AppRouter.plan(for: .topLevel(.notifications(filter: nil))), .showActivity)
        XCTAssertEqual(AppRouter.plan(for: .topLevel(.notifications(filter: "unread"))), .showActivity)
        XCTAssertEqual(AppRouter.plan(for: .topLevel(.profile)), .presentProfile)
        XCTAssertEqual(AppRouter.plan(for: .topLevel(.settings)), .presentSettings)
        XCTAssertEqual(AppRouter.plan(for: .topLevel(.notificationPreferences)), .presentSettings)
        XCTAssertEqual(AppRouter.plan(for: .topLevel(.home)), .showEventsRoot)
    }

    func testEventRoutesAreDelegatedToTheEventsZone() {
        let routes: [IosRoute] = [
            .eventCreate,
            .event(.detail(eventId: "e1")),
            .event(.pollVoting(eventId: "e1")),
            .event(.transport(eventId: "e1")),
            .meetingDetail(meetingId: "m1"),
            .invite(token: "t"),
            .topLevel(.leaderboard),
            .topLevel(.organizerDashboard)
        ]
        for route in routes {
            XCTAssertEqual(AppRouter.plan(for: route), .delegateToEvents(route), "\(route)")
        }
    }

    func testApplyingAPlanUpdatesShellState() {
        let router = AppRouter()
        XCTAssertEqual(router.zone, .events)

        XCTAssertNil(router.apply(.showActivity))
        XCTAssertEqual(router.zone, .activity)

        XCTAssertNil(router.apply(.presentProfile))
        XCTAssertTrue(router.isProfilePresented)

        XCTAssertNil(router.apply(.presentSettings))
        XCTAssertTrue(router.isSettingsPresented)
        XCTAssertFalse(router.isProfilePresented, "Une seule présentation à la fois.")

        let delegated = router.apply(.delegateToEvents(.event(.detail(eventId: "e1"))))
        XCTAssertEqual(delegated, .event(.detail(eventId: "e1")))
        XCTAssertEqual(router.zone, .events)
        XCTAssertFalse(router.isSettingsPresented, "Une navigation d'événement ferme les présentations.")

        XCTAssertEqual(router.apply(.showEventsRoot), .topLevel(.home))
        XCTAssertEqual(router.zone, .events)
    }
}
```

- [ ] **Step 2 : Lancer** `-only-testing:WakeveTests/AppRouterTests` → BUILD FAILED.

- [ ] **Step 3 : Implémenter**

```swift
import Foundation
import Observation

/// Routeur du shell de la refonte (couche 2, proposition #47).
/// Décide si une route change de zone / présente une surface du shell,
/// ou si elle est déléguée à l'aiguillage d'événements existant.
@MainActor
@Observable
final class AppRouter {

    enum Plan: Equatable {
        case showEventsRoot
        case showActivity
        case presentProfile
        case presentSettings
        case delegateToEvents(IosRoute)
    }

    var zone: AppZone = .events
    var isProfilePresented = false
    var isSettingsPresented = false

    nonisolated static func plan(for route: IosRoute) -> Plan {
        switch route {
        case .topLevel(.home):
            return .showEventsRoot
        case .topLevel(.notifications):
            return .showActivity
        case .topLevel(.profile):
            return .presentProfile
        case .topLevel(.settings), .topLevel(.notificationPreferences):
            return .presentSettings
        default:
            return .delegateToEvents(route)
        }
    }

    /// Applique le plan à l'état du shell.
    /// - Returns: la route que l'aiguillage d'événements doit encore traiter, sinon `nil`.
    @discardableResult
    func apply(_ plan: Plan) -> IosRoute? {
        switch plan {
        case .showActivity:
            dismissPresentations()
            zone = .activity
            return nil
        case .presentProfile:
            isSettingsPresented = false
            isProfilePresented = true
            return nil
        case .presentSettings:
            isProfilePresented = false
            isSettingsPresented = true
            return nil
        case .showEventsRoot:
            dismissPresentations()
            zone = .events
            return .topLevel(.home)
        case .delegateToEvents(let route):
            dismissPresentations()
            zone = .events
            return route
        }
    }

    private func dismissPresentations() {
        isProfilePresented = false
        isSettingsPresented = false
    }
}
```

Si `IosRoute` n'est pas utilisable dans un `enum … : Equatable` imbriqué (il est déjà `Equatable`), rien à changer. Ne pas ajouter de `case` à `IosRoute` (test `ParityRouteInventoryContractTests`).

- [ ] **Step 4 : Lancer** → `** TEST SUCCEEDED **` (3 tests).

- [ ] **Step 5 : Commit** — `feat(ios): add AppRouter deciding shell zone vs event routing` + corps `Refs Swarm DAO #47 (layer 2).`

### Task 3 : Corriger les taps de notification ignorés

Aujourd'hui `APNsService` (`Services/APNsService.swift:1022-1042`) poste `NavigateToEvent` avec `["deepLink": uri]` **ou** `["eventId": id]`, mais `iOSApp.swift:71-76` ne lit que `eventId` : les notifications portant un `deepLink` n'ouvrent rien.

**Files:** Create `iosApp/src/Services/NotificationDeepLink.swift` ; Modify `iosApp/src/iOSApp.swift:71-76` ; Test `iosApp/WakeveTests/NotificationDeepLinkTests.swift`

- [ ] **Step 1 : Tests**

```swift
import XCTest
@testable import Wakeve

final class NotificationDeepLinkTests: XCTestCase {
    func testPrefersExplicitDeepLink() {
        let url = NotificationDeepLink.url(from: ["deepLink": "wakeve://event/e1/poll", "eventId": "e1"])
        XCTAssertEqual(url?.absoluteString, "wakeve://event/e1/poll")
    }

    func testFallsBackToEventId() {
        XCTAssertEqual(NotificationDeepLink.url(from: ["eventId": "e1"])?.absoluteString, "wakeve://event/e1")
    }

    func testAcceptsURLValues() {
        let url = URL(string: "https://wakeve.app/event/e1")!
        XCTAssertEqual(NotificationDeepLink.url(from: ["deepLink": url]), url)
    }

    func testRejectsEmptyOrMissingValues() {
        XCTAssertNil(NotificationDeepLink.url(from: [:]))
        XCTAssertNil(NotificationDeepLink.url(from: ["deepLink": ""]))
        XCTAssertNil(NotificationDeepLink.url(from: ["eventId": ""]))
    }

    func testPercentEncodesEventIds() {
        XCTAssertEqual(NotificationDeepLink.url(from: ["eventId": "a b"])?.absoluteString, "wakeve://event/a%20b")
    }
}
```

- [ ] **Step 2 : Lancer** → BUILD FAILED.

- [ ] **Step 3 : Implémenter**

```swift
import Foundation

/// Convertit le `userInfo` d'un tap de notification (`NavigateToEvent`) en URL de deep link.
enum NotificationDeepLink {
    static func url(from userInfo: [AnyHashable: Any]) -> URL? {
        if let url = userInfo["deepLink"] as? URL { return url }
        if let raw = userInfo["deepLink"] as? String, !raw.isEmpty, let url = URL(string: raw) { return url }
        guard let eventId = userInfo["eventId"] as? String, !eventId.isEmpty,
              let encoded = eventId.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) else {
            return nil
        }
        return URL(string: "wakeve://event/\(encoded)")
    }
}
```

Dans `iOSApp.swift`, remplacer le corps du `.onReceive(… "NavigateToEvent" …)` par :

```swift
                if let url = NotificationDeepLink.url(from: notification.userInfo ?? [:]) {
                    handleDeepLink(url)
                }
```

(Garder le commentaire existant. Vérifier avec `grep -rn "NavigateToEvent" iosApp/WakeveTests` qu'aucun test source n'ancre l'ancien code ; s'il y en a, les mettre à jour pour exiger `NotificationDeepLink.url(from:`.)

- [ ] **Step 4 : Lancer** `NotificationDeepLinkTests` → PASS (5 tests).

- [ ] **Step 5 : Commit** — `fix(ios): open deep links carried by notification taps` + corps expliquant le bug + `Refs Swarm DAO #47 (layer 2).`

### Task 4 : Clés localisées du shell

**Files:** Modify les 5 `Localizable.strings` ; Test : ajouter les clés à `WKComponentsContractTests.requiredKeys` (tableau statique existant).

- [ ] **Step 1 :** Ajouter `"wk.nav.profile"` et `"wk.nav.settings"` à `requiredKeys` → lancer `WKComponentsContractTests` → FAIL.
- [ ] **Step 2 :** Ajouter en fin de chaque fichier (sous le bloc WK existant) :
  - en : `"wk.nav.profile" = "Profile";` / `"wk.nav.settings" = "Settings";`
  - fr : `"wk.nav.profile" = "Profil";` / `"wk.nav.settings" = "Réglages";`
  - es : `"Perfil"` / `"Ajustes"` · it : `"Profilo"` / `"Impostazioni"` · pt : `"Perfil"` / `"Ajustes"`
- [ ] **Step 3 :** `plutil -lint` sur les 5 fichiers, relancer → PASS.
- [ ] **Step 4 : Commit** — `feat(ios): add localized labels for shell profile and settings` + `Refs Swarm DAO #47 (layer 2).`

### Task 5 : `RedesignShellView`

**Files:** Create `iosApp/src/Views/App/RedesignShellView.swift` ; Test `iosApp/WakeveTests/RedesignShellTests.swift`

- [ ] **Step 1 : Tests**

```swift
import XCTest
import SwiftUI
@testable import Wakeve

@MainActor
final class RedesignShellTests: XCTestCase {

    func testNavBarShowsOnlyAtZoneRoots() {
        XCTAssertTrue(RedesignShellView<EmptyView, EmptyView>.showsNavBar(zone: .events, eventsAtRoot: true))
        XCTAssertFalse(RedesignShellView<EmptyView, EmptyView>.showsNavBar(zone: .events, eventsAtRoot: false))
        XCTAssertTrue(RedesignShellView<EmptyView, EmptyView>.showsNavBar(zone: .activity, eventsAtRoot: false))
    }

    func testHeaderShowsOnlyAtEventsRoot() {
        XCTAssertTrue(RedesignShellView<EmptyView, EmptyView>.showsHeader(zone: .events, eventsAtRoot: true))
        XCTAssertFalse(RedesignShellView<EmptyView, EmptyView>.showsHeader(zone: .events, eventsAtRoot: false))
        XCTAssertFalse(RedesignShellView<EmptyView, EmptyView>.showsHeader(zone: .activity, eventsAtRoot: true))
    }

    func testShellRendersBothZonesAndTheFloatingBar() throws {
        let router = AppRouter()
        let shell = RedesignShellView(
            router: router,
            eventsAtRoot: true,
            activityBadge: 2,
            userId: "u1",
            userName: "Léa Martin",
            onCreate: {},
            events: { Text("EVENTS") },
            activity: { Text("ACTIVITY") }
        )
        let host = UIHostingController(rootView: shell)
        let size = host.sizeThatFits(in: CGSize(width: 390, height: 844))
        XCTAssertGreaterThan(size.height, 0)
    }

    func testShellUsesWKChromeAndKeepsZonesMounted() throws {
        let source = try String(
            contentsOf: URL(fileURLWithPath: #filePath)
                .deletingLastPathComponent().deletingLastPathComponent()
                .appendingPathComponent("src/Views/App/RedesignShellView.swift"),
            encoding: .utf8
        )
        XCTAssertTrue(source.contains("WKFloatingNavBar("))
        XCTAssertTrue(source.contains("WKCircleButton("))
        XCTAssertTrue(source.contains(".accessibilityHidden(router.zone != .events)"), "La zone inactive reste montée mais masquée.")
        XCTAssertTrue(source.contains(".accessibilityHidden(router.zone != .activity)"))
    }
}
```

- [ ] **Step 2 : Lancer** `RedesignShellTests` → BUILD FAILED.

- [ ] **Step 3 : Implémenter**

```swift
import SwiftUI

/// Shell de la refonte (couche 2) : deux zones gardées montées (état préservé,
/// comme un TabView), en-tête profil/réglages à la racine d'Événements,
/// barre flottante à la racine de chaque zone.
struct RedesignShellView<Events: View, Activity: View>: View {
    @Bindable var router: AppRouter
    let eventsAtRoot: Bool
    let activityBadge: Int
    let userId: String
    let userName: String?
    let onCreate: () -> Void
    @ViewBuilder let events: () -> Events
    @ViewBuilder let activity: () -> Activity

    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    static func showsNavBar(zone: AppZone, eventsAtRoot: Bool) -> Bool {
        zone == .activity || eventsAtRoot
    }

    static func showsHeader(zone: AppZone, eventsAtRoot: Bool) -> Bool {
        zone == .events && eventsAtRoot
    }

    var body: some View {
        ZStack {
            events()
                .opacity(router.zone == .events ? 1 : 0)
                .allowsHitTesting(router.zone == .events)
                .accessibilityHidden(router.zone != .events)
                .safeAreaInset(edge: .top, spacing: 0) {
                    if Self.showsHeader(zone: router.zone, eventsAtRoot: eventsAtRoot) {
                        header
                    }
                }
            activity()
                .opacity(router.zone == .activity ? 1 : 0)
                .allowsHitTesting(router.zone == .activity)
                .accessibilityHidden(router.zone != .activity)
        }
        .safeAreaInset(edge: .bottom, spacing: 0) {
            if Self.showsNavBar(zone: router.zone, eventsAtRoot: eventsAtRoot) {
                WKFloatingNavBar(selection: $router.zone, activityBadge: activityBadge, onCreate: onCreate)
                    .padding(.bottom, WK.Space.xs)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
        .animation(WK.Motion.smooth(reduceMotion: reduceMotion), value: Self.showsNavBar(zone: router.zone, eventsAtRoot: eventsAtRoot))
    }

    private var header: some View {
        HStack {
            Button {
                router.apply(.presentProfile)
            } label: {
                WKAvatarStack(avatars: [WKAvatar(id: userId, name: userName ?? "")])
                    .frame(minWidth: WK.Size.minTapTarget, minHeight: WK.Size.minTapTarget)
            }
            .buttonStyle(.plain)
            .accessibilityLabel(String(localized: "wk.nav.profile"))
            .wkAccessibilityID("wk.nav.profile")

            Spacer()

            WKCircleButton(
                systemImage: "gearshape",
                accessibilityLabel: String(localized: "wk.nav.settings"),
                accessibilityID: "wk.nav.settings"
            ) {
                router.apply(.presentSettings)
            }
        }
        .padding(.horizontal, WK.Space.screen)
        .padding(.vertical, WK.Space.xxs)
    }
}
```

Adapter aux signatures réelles des composants WK (lire `Components/WK/WKButtons.swift`, `WKAvatarStack.swift`, `WK.swift` : ordre des paramètres de `WKCircleButton`, nom exact du paramètre `accessibilityID`, présence de `wkAccessibilityID`). Si `WKAvatarStack` masque son libellé propre à l'intérieur du bouton, garder le libellé du bouton (« Profil »). Aucun style en dur (le garde-fou `WKStyleGuardTests` ne scanne pas `Views/App` au-delà du cliquet : ne pas augmenter les compteurs).

- [ ] **Step 4 : Lancer** `RedesignShellTests` → PASS (4 tests).

- [ ] **Step 5 : Commit** — `feat(ios): add RedesignShellView hosting zones under the floating bar` + `Refs Swarm DAO #47 (layer 2).`

### Task 6 : Brancher le shell dans `AuthenticatedView` derrière le flag

**Files:** Modify `iosApp/src/Views/App/ContentView.swift` (struct `AuthenticatedView`, l.185-~1919) ; Test : `iosApp/WakeveTests/RedesignShellTests.swift` (ajouts).

- [ ] **Step 1 : Tests de contrat (ajouter à `RedesignShellTests`)**

```swift
    private func contentViewSource() throws -> String {
        try String(
            contentsOf: URL(fileURLWithPath: #filePath)
                .deletingLastPathComponent().deletingLastPathComponent()
                .appendingPathComponent("src/Views/App/ContentView.swift"),
            encoding: .utf8
        )
    }

    func testAuthenticatedViewBranchesOnTheRedesignFlag() throws {
        let source = try contentViewSource()
        XCTAssertTrue(source.contains("@AppStorage(FeatureFlags.redesign2026Key) private var iosRedesign2026 = false"))
        XCTAssertTrue(source.contains("RedesignShellView("))
        XCTAssertTrue(source.contains("TabView(selection: $selectedTab)"), "Le chemin legacy reste intact tant que le flag est éteint.")
        XCTAssertTrue(source.contains("@State private var redesignRouter = AppRouter()"))
    }

    func testDeepLinksGoThroughTheRouterWhenRedesignIsOn() throws {
        let source = try contentViewSource()
        guard let start = source.range(of: "private func handleDeepLinkNavigation(_ route: IosRoute)") else {
            return XCTFail("handleDeepLinkNavigation introuvable")
        }
        let body = String(source[start.lowerBound...].prefix(1200))
        XCTAssertTrue(body.contains("AppRouter.plan(for: route)"), "Toute route passe d'abord par le plan du routeur.")
    }
```

- [ ] **Step 2 : Lancer** `RedesignShellTests` → FAIL sur ces 2 tests.

- [ ] **Step 3 : Implémenter** (modifications minimales, sans déplacer ni renommer les ancres listées dans « Contraintes ») :

1. Sous `@AppStorage("iosInvitationExperienceV1") …` ajouter :
   ```swift
       @AppStorage(FeatureFlags.redesign2026Key) private var iosRedesign2026 = false
       @State private var redesignRouter = AppRouter()
   ```
2. Dans `body`, remplacer **uniquement** le début `TabView(selection: $selectedTab) { … }.tint(.wakevePrimary).toolbar(tabBarVisibility, for: .tabBar)` par `shellChrome`, et déplacer ce bloc TabView **tel quel** dans une nouvelle propriété `legacyTabChrome` ; tous les modificateurs suivants (`.fullScreenCover(isPresented: $showEventCreationSheet)`, `.sheet(isPresented: $showNotificationPreferencesSheet)`, …, `.onReceive(deepLinkService.$navigationRoute)`) restent dans `body`, dans le même ordre :
   ```swift
       @ViewBuilder
       private var shellChrome: some View {
           if iosRedesign2026 {
               redesignChrome
           } else {
               legacyTabChrome
           }
       }

       private var legacyTabChrome: some View {
           TabView(selection: $selectedTab) {
               // … bloc existant inchangé …
           }
           .tint(.wakevePrimary)
           .toolbar(tabBarVisibility, for: .tabBar)
       }

       private var redesignChrome: some View {
           RedesignShellView(
               router: redesignRouter,
               eventsAtRoot: currentView == .eventList,
               activityBadge: unreadInboxCount,
               userId: userId,
               userName: authStateManager.currentUser?.name,
               onCreate: { showEventCreationSheet = true },
               events: { homeTabContent },
               activity: { InboxView(userId: userId, onBack: {}, unreadCount: $unreadInboxCount) }
           )
           .sheet(isPresented: $redesignRouter.isProfilePresented) {
               ProfileTabView(
                   userId: userId,
                   userName: authStateManager.currentUser?.name,
                   userEmail: authStateManager.currentUser?.email,
                   onDismiss: { redesignRouter.isProfilePresented = false },
                   onSignOut: { authStateManager.signOut() }
               )
           }
           .onChange(of: redesignRouter.isSettingsPresented) { _, isPresented in
               if isPresented {
                   showNotificationPreferencesSheet = true
                   redesignRouter.isSettingsPresented = false
               }
           }
       }
   ```
   **Placement** : déclarer `shellChrome`, `legacyTabChrome` et `redesignChrome` **après** `private var tabBarVisibility` (pour que la tranche `.fullScreenCover(isPresented: $showEventCreationSheet)` → `private var tabBarVisibility` lue par `WakeveAIContractTests` ne change pas de contenu utile) et **avant** `// MARK: - Home Tab`. Vérifier le nom exact des paramètres de `ProfileTabView.init` (l.18 de `Views/Profile/ProfileTabView.swift`) et de `InboxView` ; recopier ceux utilisés dans `tabContent(for:)`. `@Bindable`/`$redesignRouter.isProfilePresented` fonctionne avec `@State` d'un objet `@Observable` ; si le compilateur refuse, utiliser `Binding(get:set:)`.
3. En tête de `handleDeepLinkNavigation(_ route: IosRoute)`, avant `invitationLandingEventId = nil` :
   ```swift
           var route = route
           if iosRedesign2026 {
               guard let delegated = redesignRouter.apply(AppRouter.plan(for: route)) else {
                   deepLinkService.clearPendingInvite()
                   deepLinkService.clearPendingDeepLink()
                   deepLinkService.resetNavigation()
                   return
               }
               route = delegated
           }
   ```
   Le reste de la fonction est inchangé (dans le shell, `selectedTab` n'a plus d'effet visible ; c'est voulu).
4. Ne pas modifier `WakeveTab`, `tabContent(for:)`, `homeTabContent`, ni les tests existants.

- [ ] **Step 4 : Vérifier les tests de contrat de navigation** (tous doivent rester verts, sans modification) :

```bash
xcodebuild test -project iosApp/iosApp.xcodeproj -scheme WakeveApp \
  -destination 'platform=iOS Simulator,name=iPhone 18 Pro' \
  -only-testing:WakeveTests/RedesignShellTests \
  -only-testing:WakeveTests/PremiumNavigationContractTests \
  -only-testing:WakeveTests/ParityRouteInventoryContractTests \
  -only-testing:WakeveTests/OrganizationPhase7ContractTests \
  -only-testing:WakeveTests/OrganizationPhase5ContractTests \
  -only-testing:WakeveTests/WakeveAIContractTests \
  -only-testing:WakeveTests/InvitationExperienceArchitectureReviewRedTests \
  -only-testing:WakeveTests/InvitationExperienceSurfaceContractTests \
  -only-testing:WakeveTests/TransportPlanningContractTests 2>&1 | tail -30
```
Expected : `** TEST SUCCEEDED **`. (Si une classe n'existe pas sous ce nom exact, retrouver le nom via `ls iosApp/WakeveTests` / `grep -rn "ContentView.swift" iosApp/WakeveTests -l`.) En cas d'échec d'un test d'ancrage, **ajuster le placement du code, pas le test**.

- [ ] **Step 5 : Commit** — `feat(ios): mount the redesign shell behind the iosRedesign2026 flag` + corps résumant le branchement + `Refs Swarm DAO #47 (layer 2).`

### Task 7 : Vérification dans le simulateur

- [ ] **Step 1 : Build et lancement flag éteint puis allumé** sur « iPhone 18 Pro » (jamais le simulateur QA) :

```bash
xcodebuild build -project iosApp/iosApp.xcodeproj -scheme WakeveApp \
  -destination 'platform=iOS Simulator,name=iPhone 18 Pro' -derivedDataPath /tmp/wk-l2-dd 2>&1 | tail -3
APP=$(find /tmp/wk-l2-dd/Build/Products -name "Wakeve.app" -maxdepth 3 | head -1)
xcrun simctl install "iPhone 18 Pro" "$APP"
xcrun simctl launch "iPhone 18 Pro" com.guyghost.wakeve -iosRedesign2026 YES
sleep 6 && xcrun simctl io "iPhone 18 Pro" screenshot /tmp/wk-l2-on.png
xcrun simctl terminate "iPhone 18 Pro" com.guyghost.wakeve
xcrun simctl launch "iPhone 18 Pro" com.guyghost.wakeve -iosRedesign2026 NO
sleep 6 && xcrun simctl io "iPhone 18 Pro" screenshot /tmp/wk-l2-off.png
```

Si l'app arrive sur l'onboarding/login, utiliser l'argument de lancement de connexion de développement existant (voir `authenticateForDevelopmentLaunchIfRequested` dans `AuthStateManager`) et le préciser dans le rapport.

- [ ] **Step 2 : Contrôle visuel** (lire les PNG) : flag allumé → pas de barre d'onglets système, barre flottante en bas, avatar + bouton réglages en haut ; flag éteint → TabView 4 onglets inchangé. Tester à la main (via `simctl` ou l'outil simulateur) : tap Activité → Inbox ; tap ＋ → création ; ouvrir un événement → la barre disparaît ; retour → elle réapparaît ; `xcrun simctl openurl "iPhone 18 Pro" "wakeve://notifications"` (ou l'URL de notifications prise en charge par `DeepLinkService.parseDeepLink`) → zone Activité.

- [ ] **Step 3 : Suite complète** → seuls les 5 échecs préexistants.

### Task 8 : Spec et revue

- [ ] **Step 1 :** Dans la spec, section 15 (« Écarts d'implémentation »), ajouter une sous-section « Couche 2 » reprenant les 4 écarts listés en tête de ce plan, et corriger la ligne de la couche 2 du tableau §9 en « Shell progressif : `AppRouter` (plan de routage), `RedesignShellView` + `WKFloatingNavBar`, zones Événements (aiguillage existant) et Activité (`InboxView`), derrière `iosRedesign2026` ».
- [ ] **Step 2 : Commit** — `docs(ios): record layer 2 progressive shell in redesign spec` + `Refs Swarm DAO #47.`
- [ ] **Step 3 :** Revue de code de la couche 2 (conformité au plan puis qualité).
