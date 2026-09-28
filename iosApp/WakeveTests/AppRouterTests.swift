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

    func testPresentationIsIdentifiableForASingleItemSheet() {
        XCTAssertEqual(AppRouter.Presentation.profile.id, "profile")
        XCTAssertEqual(AppRouter.Presentation.settings.id, "settings")
        XCTAssertNil(AppRouter().presentation)
    }

    func testApplyingAPlanUpdatesShellState() {
        let router = AppRouter()
        XCTAssertEqual(router.zone, .events)

        XCTAssertNil(router.apply(.showActivity))
        XCTAssertEqual(router.zone, .activity)

        XCTAssertNil(router.apply(.presentProfile))
        XCTAssertEqual(router.presentation, .profile)

        XCTAssertNil(router.apply(.presentSettings))
        XCTAssertEqual(router.presentation, .settings, "Une seule présentation à la fois : profil → réglages bascule la feuille.")

        XCTAssertNil(router.apply(.presentProfile))
        XCTAssertEqual(router.presentation, .profile, "Réglages → profil bascule aussi la feuille.")

        XCTAssertNil(router.apply(.showActivity))
        XCTAssertNil(router.presentation, "Changer de zone ferme la présentation.")

        router.apply(.presentSettings)
        let delegated = router.apply(.delegateToEvents(.event(.detail(eventId: "e1"))))
        XCTAssertEqual(delegated, .event(.detail(eventId: "e1")))
        XCTAssertEqual(router.zone, .events)
        XCTAssertNil(router.presentation, "Une navigation d'événement ferme les présentations.")

        router.apply(.presentProfile)
        XCTAssertEqual(router.apply(.showEventsRoot), .topLevel(.home))
        XCTAssertEqual(router.zone, .events)
        XCTAssertNil(router.presentation, "Le retour à la racine ferme les présentations.")
    }

    func testPreRouteLeavesRoutesAndRouterUntouchedWhenRedesignIsOff() {
        let router = AppRouter()
        let route = AppRouter.preRoute(.topLevel(.profile), redesignEnabled: false, router: router)
        XCTAssertEqual(route, .topLevel(.profile))
        XCTAssertNil(router.presentation)
        XCTAssertEqual(router.zone, .events)
    }

    func testPreRouteAppliesTheShellPlanWhenRedesignIsOn() {
        let router = AppRouter()
        XCTAssertNil(AppRouter.preRoute(.topLevel(.profile), redesignEnabled: true, router: router))
        XCTAssertEqual(router.presentation, .profile)

        let delegated = AppRouter.preRoute(.event(.detail(eventId: "e1")), redesignEnabled: true, router: router)
        XCTAssertEqual(delegated, .event(.detail(eventId: "e1")))
        XCTAssertNil(router.presentation)
    }
}
