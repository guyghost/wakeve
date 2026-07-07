import XCTest
@testable import Wakeve

@MainActor
final class ParityDeepLinkContractTests: XCTestCase {
    private var service: DeepLinkService!

    override func setUp() {
        super.setUp()
        service = DeepLinkService()
    }

    func testDeepLinkTypesExposeExplicitIosRoutes() {
        XCTAssertEqual(DeepLinkType.home.route, .topLevel(.home))
        XCTAssertEqual(DeepLinkType.profile.route, .topLevel(.profile))
        XCTAssertEqual(DeepLinkType.notificationPreferences.route, .topLevel(.notificationPreferences))
        XCTAssertEqual(DeepLinkType.leaderboard.route, .topLevel(.leaderboard))
        XCTAssertEqual(DeepLinkType.organizerDashboard.route, .topLevel(.organizerDashboard))
        XCTAssertEqual(DeepLinkType.eventCreate.route, .eventCreate)
        XCTAssertEqual(DeepLinkType.eventDetail(eventId: "event-1").route, .event(.detail(eventId: "event-1")))
        XCTAssertEqual(DeepLinkType.scenarioDetail(eventId: "event-1", scenarioId: "scenario-1").route, .event(.scenarioDetail(eventId: "event-1", scenarioId: "scenario-1")))
        XCTAssertEqual(DeepLinkType.meetingDetail(meetingId: "meeting-1").route, .meetingDetail(meetingId: "meeting-1"))
    }

    func testParsesAndroidEquivalentEventDeepLinks() throws {
        let cases: [(String, DeepLinkType, [String])] = [
            ("wakeve://event/create", .eventCreate, ["event", "create"]),
            ("wakeve://event/event-1", .eventDetail(eventId: "event-1"), ["event", "event-1"]),
            ("wakeve://event/event-1/details?tab=comments", .eventComments(eventId: "event-1"), ["event", "event-1", "comments"]),
            ("wakeve://event/event-1/details?tab=budget", .budgetOverview(eventId: "event-1"), ["event", "event-1", "budget"]),
            ("wakeve://event/event-1/details?tab=participants", .eventParticipants(eventId: "event-1"), ["event", "event-1", "participants"]),
            ("wakeve://event/event-1/participants", .eventParticipants(eventId: "event-1"), ["event", "event-1", "participants"]),
            ("wakeve://event/event-1/poll", .pollVoting(eventId: "event-1"), ["event", "event-1", "poll"]),
            ("wakeve://event/event-1/poll/results", .pollResults(eventId: "event-1"), ["event", "event-1", "poll_results"]),
            ("wakeve://event/event-1/scenarios", .scenarioList(eventId: "event-1"), ["event", "event-1", "scenarios"]),
            ("wakeve://event/event-1/scenarios/compare", .scenarioComparison(eventId: "event-1"), ["event", "event-1", "scenarios_compare"]),
            ("wakeve://event/event-1/scenarios/manage", .scenarioManagement(eventId: "event-1"), ["event", "event-1", "scenarios_manage"]),
            ("wakeve://event/event-1/scenario/scenario-9", .scenarioDetail(eventId: "event-1", scenarioId: "scenario-9"), ["event", "event-1", "scenario", "scenario-9"]),
            ("wakeve://event/event-1/budget", .budgetOverview(eventId: "event-1"), ["event", "event-1", "budget"]),
            ("wakeve://event/event-1/budget/budget-2", .budgetDetail(eventId: "event-1", budgetItemId: "budget-2"), ["event", "event-1", "budget", "budget-2"]),
            ("wakeve://event/event-1/meetings", .meetingList(eventId: "event-1"), ["event", "event-1", "meetings"]),
            ("wakeve://event/event-1/comments", .eventComments(eventId: "event-1"), ["event", "event-1", "comments"]),
            ("wakeve://event/event-1/invite", .invitationShare(eventId: "event-1"), ["event", "event-1", "invite"])
        ]

        for (rawURL, expected, expectedPath) in cases {
            let url = try XCTUnwrap(URL(string: rawURL))
            XCTAssertEqual(service.parseDeepLink(url), expected, rawURL)
            XCTAssertTrue(service.handleDeepLink(url), rawURL)
            XCTAssertEqual(service.navigationPath, expectedPath, rawURL)
            service.resetNavigation()
            service.clearPendingDeepLink()
        }
    }

    func testParsesUtilityAndUniversalLinks() throws {
        let cases: [(String, DeepLinkType, [String])] = [
            ("wakeve://poll/event-1", .pollVoting(eventId: "event-1"), ["event", "event-1", "poll"]),
            ("wakeve://meeting/meeting-1", .meetingDetail(meetingId: "meeting-1"), ["meeting", "meeting-1"]),
            ("wakeve://home", .home, ["home"]),
            ("wakeve://profile", .profile, ["profile"]),
            ("wakeve://settings", .settings(category: nil), ["settings"]),
            ("wakeve://settings?category=notifications", .notificationPreferences, ["notification_preferences"]),
            ("wakeve://notifications", .notifications(filter: nil), ["notifications"]),
            ("wakeve://notifications?filter=unread", .notifications(filter: "unread"), ["notifications", "unread"]),
            ("wakeve://notifications/preferences", .notificationPreferences, ["notification_preferences"]),
            ("wakeve://leaderboard", .leaderboard, ["leaderboard"]),
            ("wakeve://organizer_dashboard", .organizerDashboard, ["organizer_dashboard"]),
            ("https://wakeve.app/event/event-1/comments", .eventComments(eventId: "event-1"), ["event", "event-1", "comments"]),
            ("https://wakeve.app/meeting/meeting-1", .meetingDetail(meetingId: "meeting-1"), ["meeting", "meeting-1"]),
            ("https://wakeve.app/notifications/preferences", .notificationPreferences, ["notification_preferences"]),
            ("https://wakeve.app/leaderboard", .leaderboard, ["leaderboard"]),
            ("https://wakeve.app/organizer_dashboard", .organizerDashboard, ["organizer_dashboard"])
        ]

        for (rawURL, expected, expectedPath) in cases {
            let url = try XCTUnwrap(URL(string: rawURL))
            XCTAssertEqual(service.parseDeepLink(url), expected, rawURL)
            XCTAssertTrue(service.handleDeepLink(url), rawURL)
            XCTAssertEqual(service.navigationPath, expectedPath, rawURL)
            service.resetNavigation()
            service.clearPendingDeepLink()
        }
    }

    func testRejectsUnsafeDeepLinksBeforeNavigation() throws {
        let unsafeLinks = [
            "wakeve://event/event-1%2Fcomments",
            "wakeve://event/event-1/%2E%2E/settings",
            "wakeve://user:pass@event/event-1",
            "wakeve://event:8080/event-1",
            "wakeve://event/event-1#comments",
            "https://wakeve.app:444/event/event-1",
            "https://evil.example/event/event-1"
        ]

        for rawURL in unsafeLinks {
            let url = try XCTUnwrap(URL(string: rawURL))
            XCTAssertNil(service.parseDeepLink(url), rawURL)
            XCTAssertFalse(service.handleDeepLink(url), rawURL)
            XCTAssertTrue(service.navigationPath.isEmpty, rawURL)
        }
    }
}
