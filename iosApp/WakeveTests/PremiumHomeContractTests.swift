import XCTest
@testable import Wakeve

final class PremiumHomeContractTests: XCTestCase {
    func testHomeUsesPremiumUpcomingHierarchy() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/HomeView.swift")
        let content = slice(source, from: "struct HomeContentView", to: "// MARK: - Events Carousel View")

        XCTAssertTrue(content.contains("String(localized: \"home.greeting\")"))
        XCTAssertTrue(content.contains("String(localized: \"home.upcoming_title\")"))
        XCTAssertTrue(content.contains("HomeFeaturedEventView("))
        XCTAssertTrue(content.contains("HomeUpcomingEventsSection("))
        XCTAssertTrue(content.contains("HomeFloatingCreateButton(action: onCreateEvent)"))
        XCTAssertTrue(source.contains("EventMoodPalette.palette(for: event.eventType.name)"))
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

    func testHomeContentLayerUsesBrandMoodTokensInsteadOfLegacyEventTheme() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/HomeView.swift")
        let featured = slice(source, from: "private struct HomeFeaturedEventView", to: "private struct HomeNextActionCard")
        let gradientHelper = slice(source, from: "private func eventGradient", to: "private func parseHomeDate")

        XCTAssertTrue(featured.contains("moodPalette: EventMoodPalette.palette"))
        XCTAssertTrue(gradientHelper.contains("EventMoodPalette.palette"))
        XCTAssertFalse(featured.contains("EventTheme.theme"))
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
