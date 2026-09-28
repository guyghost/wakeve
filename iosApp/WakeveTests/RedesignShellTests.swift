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
