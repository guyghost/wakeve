import XCTest
@testable import Wakeve

@MainActor
final class AppRouterTests: XCTestCase {

    func testZoneAndPresentationRoutesAreHandledByTheShell() {
        XCTAssertEqual(AppRouter.plan(for: .topLevel(.notifications(filter: nil))), .showActivity)
        XCTAssertEqual(AppRouter.plan(for: .topLevel(.notifications(filter: "unread"))), .showActivity)
        XCTAssertEqual(AppRouter.plan(for: .topLevel(.profile)), .presentProfile)
        XCTAssertEqual(AppRouter.plan(for: .topLevel(.settings)), .presentSettings)
        XCTAssertEqual(AppRouter.plan(for: .topLevel(.notificationPreferences)), .presentSettings)
        XCTAssertEqual(AppRouter.plan(for: .topLevel(.home)), .showEventsRoot)
    }

    func testEventRoutesAreDelegatedToTheEventsZone() {
        let routes: [IosRoute] = [
            .eventCreate,
            .event(.detail(eventId: "e1")),
            .event(.pollVoting(eventId: "e1")),
            .event(.transport(eventId: "e1")),
            .meetingDetail(meetingId: "m1"),
            .invite(token: "t"),
            .topLevel(.leaderboard),
            .topLevel(.organizerDashboard)
        ]
        for route in routes {
            XCTAssertEqual(AppRouter.plan(for: route), .delegateToEvents(route), "\(route)")
        }
    }

    func testApplyingAPlanUpdatesShellState() {
        let router = AppRouter()
        XCTAssertEqual(router.zone, .events)

        XCTAssertNil(router.apply(.showActivity))
        XCTAssertEqual(router.zone, .activity)

        XCTAssertNil(router.apply(.presentProfile))
        XCTAssertTrue(router.isProfilePresented)

        XCTAssertNil(router.apply(.presentSettings))
        XCTAssertTrue(router.isSettingsPresented)
        XCTAssertFalse(router.isProfilePresented, "Une seule présentation à la fois.")

        let delegated = router.apply(.delegateToEvents(.event(.detail(eventId: "e1"))))
        XCTAssertEqual(delegated, .event(.detail(eventId: "e1")))
        XCTAssertEqual(router.zone, .events)
        XCTAssertFalse(router.isSettingsPresented, "Une navigation d'événement ferme les présentations.")

        XCTAssertEqual(router.apply(.showEventsRoot), .topLevel(.home))
        XCTAssertEqual(router.zone, .events)
    }
}
