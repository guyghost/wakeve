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

    func testShellPresentsProfileAndSettingsFromOneRouterOwnedSheet() throws {
        let source = try contentViewSource()
        guard let start = source.range(of: "private var redesignChrome: some View") else {
            return XCTFail("redesignChrome introuvable")
        }
        let body = String(source[start.lowerBound...].prefix(2000))
        XCTAssertTrue(body.contains(".sheet(item: $redesignRouter.presentation)"), "Une seule feuille pilotée par le routeur.")
        XCTAssertFalse(body.contains("showNotificationPreferencesSheet"), "Plus de relais vers la feuille legacy.")
    }
}
