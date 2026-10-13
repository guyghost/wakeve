import XCTest
@testable import Wakeve

final class PremiumNavigationContractTests: XCTestCase {
    /// Couche 9 (#47) : les zones du shell remplacent les onglets legacy ; elles restent des destinations
    /// (la création passe par ＋, jamais par une zone).
    func testShellZonesAreDestinationOnly() {
        XCTAssertEqual(AppZone.allCases, [.events, .activity])
        let rawValues = AppZone.allCases.map(\.rawValue)
        XCTAssertFalse(rawValues.contains("create"))
        XCTAssertFalse(rawValues.contains("eventCreation"))
        XCTAssertFalse(rawValues.contains("inbox"))
        XCTAssertFalse(rawValues.contains("explore"))
    }

    /// Couche 9 (#47) : le profil est une feuille du routeur du shell, plus un onglet.
    func testContentViewPresentsProfileFromTheShellRouter() throws {
        let source = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let chrome = slice(source, from: "private var redesignChrome: some View", to: "private var redesignBackDestination")

        XCTAssertTrue(chrome.contains(".sheet(item: $redesignRouter.presentation)"))
        XCTAssertTrue(chrome.contains("case .profile:"))
        XCTAssertTrue(chrome.contains("ProfileTabView("))
        XCTAssertFalse(source.contains("TabView(selection:"), "Plus de shell à onglets legacy.")
        XCTAssertFalse(source.contains("ExploreTabView("))
        XCTAssertFalse(source.contains("InboxView("))
    }

    func testAuthenticatedViewObservesTypedDeepLinkRoute() throws {
        let source = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let handler = slice(source, from: "private func handleDeepLinkNavigation", to: "private func navigateToEvent")

        XCTAssertTrue(source.contains("@EnvironmentObject private var deepLinkService: DeepLinkService"))
        XCTAssertTrue(source.contains(".onReceive(deepLinkService.$navigationRoute)"))
        XCTAssertTrue(handler.contains("private func handleDeepLinkNavigation(_ route: IosRoute)"))
        XCTAssertTrue(handler.contains("case .event(.detail(let eventId))"))
        XCTAssertTrue(handler.contains("case .event(.pollVoting(let eventId))"))
        XCTAssertTrue(handler.contains("case .invite(let token)"))
        XCTAssertTrue(handler.contains("await resolveInvitationDeepLink(token: token)"))
        XCTAssertTrue(source.contains("invitationDeepLinkResolver.resolve(token: token)"))
        XCTAssertFalse(source.contains("InvitationTokenCodec.eventId(fromInvitationCode: token)"))
        XCTAssertTrue(source.contains("deepLinkService.clearPendingInvite()"))
        XCTAssertTrue(source.contains("repository.getEvent(id: eventId)"))
    }

    func testNavigationFallbacksAndAccessMessagesUseLocalizationKeys() throws {
        let source = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let routing = slice(source, from: "private var homeTabContent", to: "private var invitationExperienceRootContent")

        XCTAssertTrue(routing.contains("navigation.placeholder.select_event_options"))
        XCTAssertTrue(routing.contains("navigation.placeholder.select_event_transport"))
        XCTAssertTrue(routing.contains("organization.access.confirm_before_budget"))
        XCTAssertTrue(routing.contains("organization.access.confirm_before_transport"))
        XCTAssertTrue(source.contains("safe_link.verified"))
        XCTAssertFalse(routing.contains("Sélectionne un événement pour voir les options"))
        XCTAssertFalse(routing.contains("Confirme ta présence avant d'ouvrir le budget."))
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

    func testLegacySheetsUseNavigationStack() throws {
        let sheetPaths = [
            "iosApp/src/Components/InvitationShareSheet.swift",
            "iosApp/src/Components/LocationSelectionSheet.swift",
            "iosApp/src/Views/Collaboration/CommentListView.swift",
            "iosApp/src/Views/Events/MealPlanningSheets.swift"
        ]

        for path in sheetPaths {
            let source = try readProjectFile(path)

            XCTAssertTrue(source.contains("NavigationStack"), "\(path) must use the modern NavigationStack container.")
            XCTAssertFalse(source.contains("NavigationView"), "\(path) must not retain legacy NavigationView containers.")
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
