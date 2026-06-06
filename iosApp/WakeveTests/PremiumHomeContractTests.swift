import XCTest
@testable import Wakeve

final class PremiumHomeContractTests: XCTestCase {
    func testHomeUsesPremiumUpcomingHierarchy() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/HomeView.swift")
        let content = slice(source, from: "struct HomeContentView", to: "// MARK: - Events Carousel View")

        XCTAssertTrue(content.contains("Text(\"Bonjour\")"))
        XCTAssertTrue(content.contains("Text(\"À venir\")"))
        XCTAssertTrue(content.contains("HomeFeaturedEventView("))
        XCTAssertTrue(content.contains("HomeUpcomingEventsSection("))
        XCTAssertTrue(content.contains("HomeFloatingCreateButton(action: onCreateEvent)"))
        XCTAssertFalse(content.contains("EventsCarouselView("))
    }

    func testHomeUsesSharedEmptyAndLoadingStates() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/HomeView.swift")
        let content = slice(source, from: "struct HomeContentView", to: "// MARK: - Events Carousel View")

        XCTAssertTrue(content.contains("LoadingSkeleton(rows: 3, showsHero: true)"))
        XCTAssertTrue(content.contains("EmptyState("))
        XCTAssertTrue(content.contains("systemImage: \"calendar.badge.plus\""))
    }

    func testHomePresentationHelpersPreserveBusinessLogicBoundary() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/HomeView.swift")

        XCTAssertTrue(source.contains("private func eventPrimaryDate"))
        XCTAssertTrue(source.contains("event.finalDate"))
        XCTAssertTrue(source.contains("event.proposedSlots"))
        XCTAssertFalse(
            source.contains("StateMachine("),
            "Home should remain SwiftUI presentation and must not instantiate shared state machines directly."
        )
    }

    private func readProjectFile(_ relativePath: String) throws -> String {
        let fileURL = URL(fileURLWithPath: #filePath)
        let testsDir = fileURL.deletingLastPathComponent()
        let iosAppDir = testsDir.deletingLastPathComponent()
        let projectRoot = iosAppDir.deletingLastPathComponent()
        let targetURL = projectRoot.appendingPathComponent(relativePath)
        return try String(contentsOf: targetURL, encoding: .utf8)
    }

    private func slice(_ source: String, from startMarker: String, to endMarker: String) -> String {
        guard let start = source.range(of: startMarker)?.lowerBound else {
            return source
        }

        let tail = source[start...]
        guard let end = tail.range(of: endMarker)?.lowerBound else {
            return String(tail)
        }

        return String(tail[..<end])
    }
}
