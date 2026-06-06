import XCTest
import Shared
@testable import Wakeve

@MainActor
final class FindingsRegressionTests: XCTestCase {
    func testDevelopmentAuthStoresAccessTokenForSyncConsumers() async throws {
        let authService = AuthenticationService(tokenStorage: InMemorySecureTokenStorage())
        await authService.signOut()
        let manager = AuthStateManager(authService: authService)

        await manager.setAuthStateForDevelopment(userId: "dev-user-test", accessToken: "dev-token-test")

        let token = await authService.getAccessToken()
        let isAuthenticated = await authService.isAuthenticated()
        XCTAssertEqual(token, "dev-token-test")
        XCTAssertTrue(isAuthenticated)
    }

    func testDevelopmentLaunchAuthRequiresExplicitFlag() {
        XCTAssertFalse(
            AuthStateManager.shouldUseDevelopmentLaunchAuthentication(
                arguments: ["Wakeve"],
                environment: [:]
            )
        )
        XCTAssertTrue(
            AuthStateManager.shouldUseDevelopmentLaunchAuthentication(
                arguments: ["Wakeve", "--wakeve-debug-authenticated"],
                environment: [:]
            )
        )
        XCTAssertTrue(
            AuthStateManager.shouldUseDevelopmentLaunchAuthentication(
                arguments: ["Wakeve"],
                environment: ["WAKEVE_DEBUG_AUTHENTICATED": "1"]
            )
        )
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

    func testCreateEventTurnsMultipleSelectedSlotsIntoProposedSlots() {
        let viewModel = CreateEventViewModel()
        var proposedSlotStarts: [String?] = []
        var proposedSlotEnds: [String?] = []
        var proposedSlotTimesOfDay: [Shared.TimeOfDay] = []
        viewModel.onEventCreated = { event in
            proposedSlotStarts = event.proposedSlots.map(\.start)
            proposedSlotEnds = event.proposedSlots.map(\.end)
            proposedSlotTimesOfDay = event.proposedSlots.map(\.timeOfDay)
        }

        viewModel.createEvent(
            title: "Week-end Lyon",
            description: "Pique-nique",
            userId: "dev-user-test",
            selectedSlots: [
                EventTimeSlotInput(
                    start: "2026-06-12T18:00:00Z",
                    end: "2026-06-12T20:00:00Z",
                    timeOfDay: .specific
                ),
                EventTimeSlotInput(
                    start: "2026-06-13T09:00:00Z",
                    end: nil,
                    timeOfDay: .allDay
                )
            ]
        )

        XCTAssertEqual(proposedSlotStarts, [
            "2026-06-12T18:00:00Z",
            "2026-06-13T09:00:00Z"
        ])
        XCTAssertEqual(proposedSlotEnds.first ?? nil, "2026-06-12T20:00:00Z")
        XCTAssertEqual(proposedSlotTimesOfDay, [.specific, .allDay])
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

private final class InMemorySecureTokenStorage: SecureTokenStorageProtocol {
    private var accessToken: String?
    private var refreshToken: String?
    private var userId: String?
    private var tokenExpiry: Int64?

    func storeAccessToken(_ token: String) async throws {
        accessToken = token
    }

    func storeRefreshToken(_ token: String) async throws {
        refreshToken = token
    }

    func storeUserId(_ userId: String) async throws {
        self.userId = userId
    }

    func storeTokenExpiry(_ expiryTimestamp: Int64) async throws {
        tokenExpiry = expiryTimestamp
    }

    func getAccessToken() async -> String? {
        accessToken
    }

    func getRefreshToken() async -> String? {
        refreshToken
    }

    func getUserId() async -> String? {
        userId
    }

    func getTokenExpiry() async -> Int64? {
        tokenExpiry
    }

    func clearAllTokens() async throws {
        accessToken = nil
        refreshToken = nil
        userId = nil
        tokenExpiry = nil
    }

    func isTokenExpired() async -> Bool {
        guard let tokenExpiry else {
            return true
        }

        return Date().timeIntervalSince1970 * 1000 >= Double(tokenExpiry)
    }

    func hasValidToken() async -> Bool {
        guard let accessToken, !accessToken.isEmpty else {
            return false
        }

        return !(await isTokenExpired())
    }
}
