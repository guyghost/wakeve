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
        XCTAssertTrue(content.contains("HomeDraftResumeCard("))
        XCTAssertTrue(content.contains("shouldShowDraftResumeCard"))
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

    func testHomeSurfacesDraftResumeWithoutChangingUpcomingFilter() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/HomeView.swift")
        let content = slice(source, from: "struct HomeContentView", to: "// MARK: - Premium Home Sections")
        let draftCard = slice(source, from: "private struct HomeDraftResumeCard", to: "private struct HomeUpcomingEventsSection")

        XCTAssertTrue(content.contains("selectedFilter == .upcoming && primaryDraft != nil"))
        XCTAssertTrue(content.contains("return event.status != .finalized && event.status != .draft"))
        XCTAssertTrue(content.contains("onEventSelected(primaryDraft)"))
        XCTAssertTrue(draftCard.contains("home.draft_resume.eyebrow"))
        XCTAssertTrue(draftCard.contains("home.draft_resume.subtitle"))
        XCTAssertTrue(draftCard.contains("home.draft_resume.subtitle_with_count_format"))
        XCTAssertTrue(draftCard.contains("pencil.and.outline"))
    }

    func testHomeContentLayerUsesBrandMoodTokensInsteadOfLegacyEventTheme() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/HomeView.swift")
        let featured = slice(source, from: "private struct HomeFeaturedEventView", to: "private struct HomeNextActionCard")
        let gradientHelper = slice(source, from: "private func eventGradient", to: "private func parseHomeDate")

        XCTAssertTrue(featured.contains("moodPalette: EventMoodPalette.palette"))
        XCTAssertTrue(gradientHelper.contains("EventMoodPalette.palette"))
        XCTAssertFalse(featured.contains("EventTheme.theme"))
    }

    func testHomeDateFormattersUseUserLocale() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/HomeView.swift")
        let homeDateFormatters = slice(source, from: "private let longHomeDateFormatter", to: "// MARK: - Events Carousel View")

        XCTAssertTrue(homeDateFormatters.contains("formatter.locale = .autoupdatingCurrent"))
        XCTAssertTrue(source.contains("String(localized: \"home.date_to_confirm\")"))
        XCTAssertTrue(source.contains("String(localized: \"home.to_define\")"))
        XCTAssertTrue(source.contains("home.expected_participants_format"))
        XCTAssertTrue(source.contains("home.participant_count_singular_format"))
        XCTAssertTrue(source.contains("home.participant_count_plural_format"))
        XCTAssertFalse(source.contains("\"Date à confirmer\""))
        XCTAssertFalse(source.contains("\"À définir\""))
        XCTAssertFalse(source.contains("attendus"))
        XCTAssertFalse(source.contains("participant\\(count > 1 ? \"s\" : \"\")"))
        XCTAssertFalse(homeDateFormatters.contains("Locale(identifier: \"fr_FR\")"), "Home event dates should respect the user's locale.")
    }

    func testHomeDraftResumeCopyIsLocalizedInSupportedLocales() throws {
        let requiredKeys = [
            "home.draft_resume.eyebrow",
            "home.draft_resume.subtitle",
            "home.draft_resume.subtitle_with_count_format",
            "home.expected_participants_format",
            "home.participant_count_singular_format",
            "home.participant_count_plural_format"
        ]

        for locale in ["en", "fr", "es", "it", "pt"] {
            let strings = try readProjectFile("iosApp/src/Resources/\(locale).lproj/Localizable.strings")
            for key in requiredKeys {
                XCTAssertTrue(strings.contains("\"\(key)\""), "Missing localized Home key \(key) for \(locale).")
            }
        }
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
