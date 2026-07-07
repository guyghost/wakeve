import XCTest
@testable import Wakeve

final class ParityRouteInventoryContractTests: XCTestCase {
    private let classifiedAndroidRoutes: [String: String] = [
        "home": "native",
        "events": "android-only-alias",
        "explore": "native-platform-naming",
        "profile": "native",
        "splash": "platform-auth-flow",
        "get_started": "platform-auth-flow",
        "auth": "platform-auth-flow",
        "auth/email": "platform-auth-flow",
        "login": "deprecated-android-alias",
        "onboarding": "platform-auth-flow",
        "event_creation": "native-sheet",
        "event_planning_assistant": "covered-by-add-on-device-wakeve-ai",
        "event/{eventId}": "native",
        "event/{eventId}/participants": "native",
        "event/{eventId}/poll/vote": "native",
        "event/{eventId}/poll/results": "native",
        "event/{eventId}/scenarios": "native",
        "event/{eventId}/scenario/{scenarioId}": "native",
        "event/{eventId}/scenarios/compare": "native",
        "event/{eventId}/scenarios/manage": "native-consolidated",
        "event/{eventId}/budget": "native",
        "event/{eventId}/budget/{budgetItemId}": "native",
        "event/{eventId}/payment": "native",
        "event/{eventId}/tricount": "native",
        "event/{eventId}/accommodation": "native",
        "event/{eventId}/meals": "native",
        "event/{eventId}/equipment": "native",
        "event/{eventId}/activities": "native",
        "event/{eventId}/transport": "native",
        "event/{eventId}/comments": "native",
        "event/{eventId}/photos": "deferred-native-unavailable-state",
        "inbox": "native",
        "event/{eventId}/meetings": "native",
        "meeting/{meetingId}": "native",
        "event/{eventId}/invite": "native",
        "settings": "native-profile-contained",
        "notifications": "native-inbox-mapping",
        "notifications/preferences": "native",
        "leaderboard": "native",
        "organizer_dashboard": "native"
    ]

    func testEveryAndroidScreenRouteIsClassified() throws {
        let screenSource = try readProjectFile("composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/Screen.kt")
        let androidRoutes = Set(extractScreenRoutes(from: screenSource))
        let missing = androidRoutes
            .filter { classifiedAndroidRoutes[$0] == nil }
            .sorted()

        XCTAssertTrue(
            missing.isEmpty,
            "Every Android user-visible Screen route must be classified for iOS parity. Missing: \(missing)"
        )
    }

    func testVerificationMatrixRouteRowsAreClassified() throws {
        let matrix = try readProjectFile("openspec/changes/align-ios-android-feature-parity/verification-matrix.md")
        let routeSection = slice(matrix, from: "## Route Classification", to: "## Deep-Link Evidence")
        let matrixRoutes = routeSection
            .split(separator: "\n")
            .filter { $0.hasPrefix("| `") }
            .flatMap { line -> [String] in
                let cells = line.split(separator: "|", omittingEmptySubsequences: false)
                guard cells.count > 1 else { return [] }
                return extractBacktickedValues(from: String(cells[1]))
            }
            .flatMap { value in value.split(separator: ",").map { $0.trimmingCharacters(in: .whitespaces) } }
            .filter { !$0.isEmpty }
        let missing = Set(matrixRoutes)
            .filter { classifiedAndroidRoutes[$0] == nil }
            .sorted()

        XCTAssertTrue(
            missing.isEmpty,
            "Every verification-matrix route row must have an automated classification. Missing: \(missing)"
        )
    }

    func testIosRouteModelDefinesAndroidEquivalentDestinations() {
        let expectedPaths: [(IosRoute, [String])] = [
            (.topLevel(.home), ["home"]),
            (.topLevel(.profile), ["profile"]),
            (.topLevel(.settings), ["settings"]),
            (.topLevel(.notifications(filter: nil)), ["notifications"]),
            (.topLevel(.notifications(filter: "unread")), ["notifications", "unread"]),
            (.topLevel(.notificationPreferences), ["notification_preferences"]),
            (.topLevel(.leaderboard), ["leaderboard"]),
            (.topLevel(.organizerDashboard), ["organizer_dashboard"]),
            (.eventCreate, ["event", "create"]),
            (.event(.detail(eventId: "event-1")), ["event", "event-1"]),
            (.event(.participants(eventId: "event-1")), ["event", "event-1", "participants"]),
            (.event(.pollVoting(eventId: "event-1")), ["event", "event-1", "poll"]),
            (.event(.pollResults(eventId: "event-1")), ["event", "event-1", "poll_results"]),
            (.event(.scenarioList(eventId: "event-1")), ["event", "event-1", "scenarios"]),
            (.event(.scenarioComparison(eventId: "event-1")), ["event", "event-1", "scenarios_compare"]),
            (.event(.scenarioManagement(eventId: "event-1")), ["event", "event-1", "scenarios_manage"]),
            (.event(.scenarioDetail(eventId: "event-1", scenarioId: "scenario-1")), ["event", "event-1", "scenario", "scenario-1"]),
            (.event(.budgetOverview(eventId: "event-1")), ["event", "event-1", "budget"]),
            (.event(.budgetDetail(eventId: "event-1", budgetItemId: "budget-1")), ["event", "event-1", "budget", "budget-1"]),
            (.event(.meetingList(eventId: "event-1")), ["event", "event-1", "meetings"]),
            (.event(.comments(eventId: "event-1")), ["event", "event-1", "comments"]),
            (.event(.invitationShare(eventId: "event-1")), ["event", "event-1", "invite"]),
            (.event(.transport(eventId: "event-1")), ["event", "event-1", "transport"]),
            (.event(.accommodation(eventId: "event-1")), ["event", "event-1", "accommodation"]),
            (.event(.meals(eventId: "event-1")), ["event", "event-1", "meals"]),
            (.event(.equipment(eventId: "event-1")), ["event", "event-1", "equipment"]),
            (.event(.activities(eventId: "event-1")), ["event", "event-1", "activities"]),
            (.event(.payment(eventId: "event-1")), ["event", "event-1", "payment"]),
            (.event(.tricount(eventId: "event-1")), ["event", "event-1", "tricount"]),
            (.event(.photos(eventId: "event-1")), ["event", "event-1", "photos"]),
            (.meetingDetail(meetingId: "meeting-1"), ["meeting", "meeting-1"]),
            (.invite(token: "invite-token"), ["invite", "invite-token"])
        ]

        for (route, path) in expectedPaths {
            XCTAssertEqual(route.navigationPath, path)
        }
    }

    func testAndroidEquivalentOrganizationRoutesDoNotRenderGenericPlaceholders() throws {
        let content = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let homeContent = slice(content, from: "private var homeTabContent", to: "// MARK: - Tab Content")

        XCTAssertFalse(homeContent.contains("navigation.placeholder.event_creation"))
        XCTAssertTrue(caseBlock(".eventCreation", in: homeContent).contains("showEventCreationSheet = true"))

        let routeViews = [
            ".scenarioDetail": "ScenarioDetailParityView",
            ".accommodation": "AccommodationPlanningRouteView",
            ".mealPlanning": "MealPlanningRouteView",
            ".equipmentChecklist": "EquipmentChecklistRouteView",
            ".activityPlanning": "ActivityPlanningRouteView",
            ".comments": "EventCommentsRouteView",
            ".eventPhotos": "EventPhotosFollowUpRouteView",
            ".invitationShare": "InvitationShareSheet",
            ".leaderboard": "LeaderboardView",
            ".organizerDashboard": "OrganizerDashboardRouteView"
        ]

        for (caseName, viewName) in routeViews {
            let block = caseBlock(caseName, in: homeContent)
            XCTAssertTrue(block.contains(viewName), "\(caseName) must render \(viewName), not a generic placeholder.")
        }

        let accommodationBlock = caseBlock(".accommodation", in: homeContent)
        XCTAssertFalse(
            accommodationBlock.contains("ScenarioOrganizationView"),
            "Accommodation must no longer route to generic scenario organization."
        )
    }

    func testRouteLocalizationKeysExistInSupportedLocales() throws {
        let requiredKeys = [
            "scenario.detail.title",
            "scenario.detail.empty_title",
            "organization.state.pending_sync",
            "organization.state.read_only",
            "event.detail.organization.accommodation_label",
            "event.detail.organization.meals_label",
            "event.detail.organization.equipment_label",
            "event.detail.organization.activities_label",
            "event.detail.organization.comments_label",
            "event.detail.organization.photos_label",
            "event.detail.organization.invitation_label",
            "organization.access.confirm_before_accommodation",
            "organization.access.confirm_before_meals",
            "organization.access.confirm_before_equipment",
            "organization.access.confirm_before_activities",
            "organization.access.confirm_before_comments",
            "organization.access.confirm_before_photos",
            "photos.follow_up.title",
            "organizer.dashboard.title"
        ]

        for locale in ["en", "fr", "es", "it", "pt"] {
            let strings = try readProjectFile("iosApp/src/Resources/\(locale).lproj/Localizable.strings")
            for key in requiredKeys {
                XCTAssertTrue(strings.contains("\"\(key)\""), "\(locale) is missing \(key)")
            }
        }
    }

    private func extractScreenRoutes(from source: String) -> [String] {
        let pattern = #"Screen\("([^"]+)"\)"#
        return regexCaptures(pattern: pattern, in: source)
            .map(normalizeRoute)
            .filter { !$0.isEmpty }
    }

    private func extractBacktickedValues(from source: String) -> [String] {
        regexCaptures(pattern: #"`([^`]+)`"#, in: source)
    }

    private func regexCaptures(pattern: String, in source: String) -> [String] {
        guard let regex = try? NSRegularExpression(pattern: pattern) else { return [] }
        let range = NSRange(source.startIndex..<source.endIndex, in: source)
        return regex.matches(in: source, range: range).compactMap { match in
            guard match.numberOfRanges > 1, let matchRange = Range(match.range(at: 1), in: source) else {
                return nil
            }
            return String(source[matchRange])
        }
    }

    private func normalizeRoute(_ route: String) -> String {
        route.split(separator: "?", maxSplits: 1).first.map(String.init) ?? route
    }

    private func caseBlock(_ caseName: String, in source: String) -> String {
        let marker = "case \(caseName):"
        guard let start = source.range(of: marker)?.lowerBound else {
            return ""
        }

        let tail = source[start...]
        let afterMarker = tail.index(tail.startIndex, offsetBy: marker.count)
        let remainder = tail[afterMarker...]
        if let nextCase = remainder.range(of: "\n        case .")?.lowerBound {
            return String(tail[..<nextCase])
        }
        return String(tail)
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
