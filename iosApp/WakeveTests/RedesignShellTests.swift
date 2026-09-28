import XCTest
import SwiftUI
@testable import Wakeve

@MainActor
final class RedesignShellTests: XCTestCase {

    func testNavBarShowsOnlyAtZoneRoots() {
        typealias Shell = RedesignShellView<EmptyView, EmptyView>
        XCTAssertTrue(Shell.showsNavBar(zone: .events, eventsAtRoot: true, activityAtRoot: false))
        XCTAssertFalse(Shell.showsNavBar(zone: .events, eventsAtRoot: false, activityAtRoot: true))
        XCTAssertTrue(Shell.showsNavBar(zone: .activity, eventsAtRoot: false, activityAtRoot: true))
        XCTAssertFalse(
            Shell.showsNavBar(zone: .activity, eventsAtRoot: true, activityAtRoot: false),
            "Activité hors racine (détail poussé ou mode sélection) : la barre se retire."
        )
    }

    func testNavBarAnimationIsScopedToTheBar() throws {
        let source = try String(
            contentsOf: URL(fileURLWithPath: #filePath)
                .deletingLastPathComponent().deletingLastPathComponent()
                .appendingPathComponent("src/Views/App/RedesignShellView.swift"),
            encoding: .utf8
        )
        guard let inset = source.range(of: ".safeAreaInset(edge: .bottom") else {
            return XCTFail("Inset de la barre introuvable")
        }
        let beforeInset = String(source[..<inset.lowerBound])
        XCTAssertFalse(beforeInset.contains(".animation("), "Le basculement de zone ne doit pas être animé.")
        let insetBody = String(source[inset.lowerBound...].prefix(900))
        XCTAssertTrue(insetBody.contains(".animation("), "Seule la barre anime son apparition.")
    }

    func testActivityZoneReloadsAndReportsItsRootState() throws {
        let source = try contentViewSource()
        guard let start = source.range(of: "private var redesignChrome: some View") else {
            return XCTFail("redesignChrome introuvable")
        }
        let body = String(source[start.lowerBound...].prefix(2500))
        XCTAssertTrue(body.contains("activityAtRoot: activityAtRoot"))
        XCTAssertTrue(body.contains("reloadToken: activityReloadToken"))
        XCTAssertTrue(body.contains("activityReloadToken += 1"))

        let inbox = try String(
            contentsOf: URL(fileURLWithPath: #filePath)
                .deletingLastPathComponent().deletingLastPathComponent()
                .appendingPathComponent("src/Views/Inbox/InboxView.swift"),
            encoding: .utf8
        )
        XCTAssertTrue(inbox.contains("var reloadToken: Int = 0"))
        XCTAssertTrue(inbox.contains("var onRootStateChange: ((Bool) -> Void)? = nil"))
        XCTAssertTrue(inbox.contains(".onChange(of: reloadToken)"))
    }

    func testHeaderShowsOnlyAtEventsRoot() {
        XCTAssertTrue(RedesignShellView<EmptyView, EmptyView>.showsHeader(zone: .events, eventsAtRoot: true))
        XCTAssertFalse(RedesignShellView<EmptyView, EmptyView>.showsHeader(zone: .events, eventsAtRoot: false))
        XCTAssertFalse(RedesignShellView<EmptyView, EmptyView>.showsHeader(zone: .activity, eventsAtRoot: true))
    }

    func testShellRendersSmoke() throws {
        let router = AppRouter()
        let shell = RedesignShellView(
            router: router,
            eventsAtRoot: true,
            activityAtRoot: true,
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
        XCTAssertTrue(
            body.contains("AppRouter.preRoute(route, redesignEnabled: iosRedesign2026, router: redesignRouter)"),
            "Toute route passe d'abord par le pré-aiguillage du routeur."
        )
    }

    func testShellPresentsProfileAndSettingsFromOneRouterOwnedSheet() throws {
        let source = try contentViewSource()
        guard let start = source.range(of: "private var redesignChrome: some View") else {
            return XCTFail("redesignChrome introuvable")
        }
        let body = String(source[start.lowerBound...].prefix(2000))
        XCTAssertTrue(body.contains(".sheet(item: $redesignRouter.presentation)"), "Une seule feuille pilotée par le routeur.")
        XCTAssertFalse(body.contains("showNotificationPreferencesSheet"), "Plus de relais vers la feuille legacy.")
    }

    func testFlippingTheFlagResetsTheShellRouter() throws {
        let source = try contentViewSource()
        XCTAssertTrue(source.contains(".onChange(of: iosRedesign2026) { _, _ in redesignRouter = AppRouter() }"))
    }

    func testRedesignRootInstallsTheNewHome() throws {
        let source = try contentViewSource()
        guard let start = source.range(of: "private var invitationExperienceRootContent") else { return XCTFail() }
        let slice = String(source[start.lowerBound...].prefix(1500))
        XCTAssertTrue(slice.contains("if iosRedesign2026"), "La nouvelle racine est prioritaire sous le flag.")
        // Conteneur propriétaire du modèle de vue, qui rend `EventsHomeView`.
        XCTAssertTrue(slice.contains("EventsHomeContainer("))
        XCTAssertTrue(slice.contains("invitationExperienceRolloutEnabled"), "Le chemin legacy reste en place.")
    }

    func testShellHeaderShowsTheWordmark() throws {
        let source = try String(contentsOf: URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent().deletingLastPathComponent()
            .appendingPathComponent("src/Views/App/RedesignShellView.swift"), encoding: .utf8)
        XCTAssertTrue(source.contains("\"wk.wordmark\""))
    }

    func testHomeNextStepOpensPollScreensWithoutInvitationRollout() throws {
        let source = try contentViewSource()
        guard let start = source.range(of: "private func handleHomeNextStep(_ step: HomeNextStep)") else {
            return XCTFail()
        }
        let slice = String(source[start.lowerBound...].prefix(1200))
        // Sans rollout invitation, le routeur retombe sur le détail : « Voter » doit ouvrir le vote.
        XCTAssertTrue(slice.contains("guard invitationExperienceRolloutEnabled else"))
        XCTAssertTrue(slice.contains("destination: .pollVoting"))
        XCTAssertTrue(slice.contains("destination: .pollResults"))
    }
}
