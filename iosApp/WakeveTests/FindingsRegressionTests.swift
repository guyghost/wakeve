import XCTest
import Shared
@testable import Wakeve

@MainActor
final class FindingsRegressionTests: XCTestCase {
    func testDevelopmentAuthStoresAccessTokenForSyncConsumers() async throws {
        let authService = AuthenticationService()
        await authService.signOut()
        let manager = AuthStateManager(authService: authService)

        await manager.setAuthStateForDevelopment(userId: "dev-user-test", accessToken: "dev-token-test")

        let token = await authService.getAccessToken()
        let isAuthenticated = await authService.isAuthenticated()
        XCTAssertEqual(token, "dev-token-test")
        XCTAssertTrue(isAuthenticated)
    }

    func testParticipantManagementRequiresAtLeastOneProposedSlotBeforeStartingPoll() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/ParticipantManagementView.swift")
        let canStartPoll = slice(source, from: "private var canStartPoll", to: "private var heroColors")

        XCTAssertTrue(
            canStartPoll.contains("!event.proposedSlots.isEmpty"),
            "A draft poll must not be startable until at least one time slot exists."
        )
    }

    func testCreateEventTurnsSelectedDateIntoProposedSlot() {
        let viewModel = CreateEventViewModel()
        var proposedSlotStarts: [String?] = []
        viewModel.onEventCreated = { event in
            proposedSlotStarts = event.proposedSlots.map(\.start)
        }

        viewModel.createEvent(
            title: "Week-end Lyon",
            description: "Pique-nique",
            userId: "dev-user-test",
            selectedDate: "2026-06-12T18:00:00Z"
        )

        XCTAssertEqual(proposedSlotStarts.count, 1)
        XCTAssertEqual(proposedSlotStarts.first ?? nil, "2026-06-12T18:00:00Z")
    }

    func testAppLaunchDoesNotRequestNotificationAuthorizationImmediately() throws {
        let source = try readProjectFile("iosApp/src/Services/AppDelegate.swift")
        let didFinishLaunching = slice(
            source,
            from: "func application(",
            to: "// MARK: - Remote Notification Registration"
        )

        XCTAssertFalse(
            didFinishLaunching.contains("requestAuthorization"),
            "Notification permission must be requested from an explicit user action, not during app launch/onboarding."
        )
    }

    func testInvitationPreviewReservesHeaderSpaceForTitle() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/CreateEventSheet.swift")
        let preview = slice(source, from: "struct EventPreviewSheet", to: "// MARK: - Preview Cards")

        XCTAssertTrue(
            preview.contains("previewHeaderReservedHeight"),
            "EventPreviewSheet must reserve enough vertical space so the title cannot sit under the back/next buttons."
        )
    }

    func testVisibleFindingStringsAreLocalized() throws {
        let french = try readProjectFile("iosApp/src/Resources/fr.lproj/Localizable.strings")
        let requiredFrenchKeys = [
            "\"profile.edit\" = \"Modifier\"",
            "\"poll.results.title\" = \"Résultats\"",
            "\"poll.results.no_slots_title\" = \"Aucun créneau proposé\"",
            "\"poll.results.no_votes_title\" = \"Aucun vote pour le moment\"",
            "\"inbox.filter.inbox\" = \"Inbox\"",
            "\"inbox.filter.focused\" = \"Prioritaires\"",
            "\"inbox.filter.new\" = \"Nouveau\"",
            "\"inbox.filter.unread\" = \"Non lus\"",
            "\"inbox.filter.event\" = \"Événement\""
        ]

        for key in requiredFrenchKeys {
            XCTAssertTrue(french.contains(key), "Missing localized French string: \(key)")
        }
    }

    func testEventTypeDisplayUsesLocalizedHelper() throws {
        let helpers = try readProjectFile("iosApp/src/Extensions/ViewExtensions.swift")
        let createSheet = try readProjectFile("iosApp/src/Views/Events/CreateEventSheet.swift")

        XCTAssertTrue(helpers.contains("func eventTypeDisplayName"))
        XCTAssertTrue(
            createSheet.contains("eventTypeDisplayName(selectedEventType)") &&
            createSheet.contains("eventTypeDisplayName(eventType)"),
            "Event type labels must use localized display names instead of Kotlin displayName."
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
