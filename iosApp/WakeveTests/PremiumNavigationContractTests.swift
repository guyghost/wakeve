import XCTest
@testable import Wakeve

final class PremiumNavigationContractTests: XCTestCase {
    func testWakeveTabsAreDestinationOnly() throws {
        let source = try readProjectFile("iosApp/src/Models/WakeveTab.swift")

        XCTAssertEqual(WakeveTab.allCases, [.home, .groups, .messages, .profile])
        XCTAssertEqual(WakeveTab.allCases.map(\.systemImage), ["calendar", "sparkles", "message", "person.crop.circle"])
        XCTAssertTrue(source.contains("String(localized: \"tab.explore\")"))
        XCTAssertFalse(source.contains("return \"Groupes\""), "The templates tab must not be mislabeled as Groups.")
    }

    func testWakeveTabsDoNotExposeCreateEventAsDestination() {
        let rawValues = WakeveTab.allCases.map(\.rawValue)

        XCTAssertFalse(rawValues.contains("create"))
        XCTAssertFalse(rawValues.contains("eventCreation"))
        XCTAssertFalse(rawValues.contains("inbox"))
        XCTAssertFalse(rawValues.contains("explore"))
    }

    func testContentViewUsesProfileAsTabDestination() throws {
        let source = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let tabView = slice(source, from: "TabView(selection: $selectedTab)", to: ".tint(.wakevePrimary)")
        let tabContent = slice(source, from: "private func tabContent(for tab: WakeveTab)", to: "private func isParticipantConfirmed")

        XCTAssertTrue(tabView.contains("tabContent(for: .profile)"))
        XCTAssertTrue(tabView.contains("WakeveTab.profile.title"))
        XCTAssertTrue(tabContent.contains("case .profile:"))
        XCTAssertTrue(tabContent.contains("ProfileTabView("))
        XCTAssertFalse(tabView.contains("tabContent(for: .inbox)"))
        XCTAssertFalse(tabView.contains("tabContent(for: .explore)"))
    }

    func testAuthenticatedViewObservesDeepLinkNavigationPath() throws {
        let source = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let handler = slice(source, from: "private func handleDeepLinkNavigation", to: "private func navigateToEvent")

        XCTAssertTrue(source.contains("@EnvironmentObject private var deepLinkService: DeepLinkService"))
        XCTAssertTrue(source.contains(".onReceive(deepLinkService.$navigationPath)"))
        XCTAssertTrue(handler.contains("case (\"event\", let eventId?, nil)"))
        XCTAssertTrue(handler.contains("case (\"event\", let eventId?, \"poll\")"))
        XCTAssertTrue(handler.contains("case (\"invite\", let token?, _)"))
        XCTAssertTrue(source.contains("InvitationTokenCodec.eventId(fromInvitationCode: token)"))
        XCTAssertTrue(source.contains("deepLinkService.clearPendingInvite()"))
        XCTAssertTrue(source.contains("repository.getEvent(id: eventId)"))
    }

    func testNavigationFallbacksAndAccessMessagesUseLocalizationKeys() throws {
        let source = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let routing = slice(source, from: "private var homeTabContent", to: "private func tabContent")

        XCTAssertTrue(routing.contains("navigation.placeholder.select_event_options"))
        XCTAssertTrue(routing.contains("navigation.placeholder.select_event_transport"))
        XCTAssertTrue(routing.contains("organization.access.confirm_before_budget"))
        XCTAssertTrue(routing.contains("organization.access.confirm_before_transport"))
        XCTAssertTrue(source.contains("safe_link.verified"))
        XCTAssertFalse(routing.contains("Sélectionnez un événement pour voir les options"))
        XCTAssertFalse(routing.contains("Confirmez votre présence avant d'ouvrir le budget."))
        XCTAssertFalse(source.contains("verificationStatus: \"vérifié\""))
    }

    func testRootErrorViewUsesLocalizedRecoveryCopy() throws {
        let source = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let errorView = slice(source, from: "struct ErrorView: View", to: "// MARK: - Authenticated Content View")

        XCTAssertTrue(errorView.contains("String(localized: \"common.error_generic\")"))
        XCTAssertTrue(errorView.contains("String(localized: \"common.try_again\")"))
        XCTAssertFalse(errorView.contains("Text(\"Something went wrong\")"))
        XCTAssertFalse(errorView.contains("Text(\"Try Again\")"))
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
