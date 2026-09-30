import XCTest
@testable import Wakeve

/// Cycle de vie des sheets de modules du hub (couche 5a, #47, revue I1–I3).
final class HubSheetLifecycleTests: XCTestCase {
    private func opened(_ module: HubModule = .meals, eventId: String = "e1") -> HubSheetLifecycle {
        var lifecycle = HubSheetLifecycle()
        lifecycle.present(module, eventId: eventId)
        return lifecycle
    }

    func testPresentationIsKeyedByEventAndModule() {
        let lifecycle = opened()
        XCTAssertEqual(lifecycle.presented, .init(module: .meals, eventId: "e1"))
        XCTAssertNotEqual(HubSheetLifecycle.Presented(module: .meals, eventId: "e1").id,
                          HubSheetLifecycle.Presented(module: .meals, eventId: "e2").id)
        XCTAssertNotEqual(HubSheetLifecycle.Presented(module: .meals, eventId: "e1").id,
                          HubSheetLifecycle.Presented(module: .equipment, eventId: "e1").id)
    }

    func testClosingReloadsTheHub() {
        var lifecycle = opened()
        lifecycle.close()
        XCTAssertNil(lifecycle.presented)
        XCTAssertEqual(lifecycle.didDismiss(currentView: .eventDetail, selectedEventId: "e1"), [.reloadHub])
        XCTAssertEqual(lifecycle, HubSheetLifecycle())
    }

    func testFallbackOpensTheLegacyScreenAfterDismissalOnTheSameHub() {
        var lifecycle = opened()
        lifecycle.requestFallback(.mealPlanning)
        XCTAssertNil(lifecycle.presented)
        XCTAssertEqual(lifecycle.pendingFallback, .init(eventId: "e1", view: .mealPlanning))
        XCTAssertEqual(lifecycle.didDismiss(currentView: .eventDetail, selectedEventId: "e1"), [.show(.mealPlanning)])
        XCTAssertNil(lifecycle.pendingFallback)
    }

    func testFallbackIsDroppedWhenTheHubIsNoLongerShown() {
        var otherScreen = opened()
        otherScreen.requestFallback(.mealPlanning)
        XCTAssertEqual(otherScreen.didDismiss(currentView: .eventList, selectedEventId: "e1"), [])
        var otherEvent = opened()
        otherEvent.requestFallback(.mealPlanning)
        XCTAssertEqual(otherEvent.didDismiss(currentView: .eventDetail, selectedEventId: "e2"), [])
    }

    func testDeepLinkDismissesWithoutFallback() {
        var lifecycle = opened()
        lifecycle.requestFallback(.mealPlanning)
        XCTAssertTrue(lifecycle.dismiss(), "La sheet était encore en cours de fermeture.")
        XCTAssertNil(lifecycle.pendingFallback)
        XCTAssertEqual(lifecycle.didDismiss(currentView: .eventDetail, selectedEventId: "e1"), [.reloadHub])

        var open = opened()
        XCTAssertTrue(open.dismiss())
        XCTAssertNil(open.presented)
        var idle = HubSheetLifecycle()
        XCTAssertFalse(idle.dismiss())
    }

    func testRouterPresentationWaitsForTheSheetToClose() {
        var lifecycle = opened()
        XCTAssertTrue(lifecycle.dismiss())
        XCTAssertNil(lifecycle.routerPresentation(.profile), "Pas de présentation pendant la fermeture de la sheet.")
        XCTAssertEqual(lifecycle.didDismiss(currentView: .eventDetail, selectedEventId: "e1"), [.reloadHub, .presentRouter(.profile)])
        XCTAssertNil(lifecycle.deferredPresentation)
    }

    func testRouterPresentationIsImmediateWithoutSheet() {
        var lifecycle = HubSheetLifecycle()
        XCTAssertEqual(lifecycle.routerPresentation(.settings), .settings)
        XCTAssertNil(lifecycle.routerPresentation(nil))
        var closing = opened()
        closing.dismiss()
        XCTAssertNil(closing.routerPresentation(nil), "Une route qui ferme les présentations reste sans effet différé.")
        XCTAssertEqual(closing.didDismiss(currentView: .eventDetail, selectedEventId: "e1"), [.reloadHub])
    }

    func testChangingTheSelectedEventClosesItsSheet() {
        var same = opened()
        XCTAssertFalse(same.selectedEventChanged(to: "e1"))
        XCTAssertNotNil(same.presented)
        var other = opened()
        XCTAssertTrue(other.selectedEventChanged(to: "e2"))
        XCTAssertNil(other.presented)
        var fallback = opened()
        fallback.requestFallback(.mealPlanning)
        XCTAssertTrue(fallback.selectedEventChanged(to: nil))
        XCTAssertNil(fallback.pendingFallback)
        var idle = HubSheetLifecycle()
        XCTAssertFalse(idle.selectedEventChanged(to: "e2"))
    }

    func testPresentingAgainResetsAStaleClosingState() {
        var lifecycle = opened()
        lifecycle.dismiss()
        _ = lifecycle.routerPresentation(.profile)
        lifecycle.present(.equipment, eventId: "e1")
        XCTAssertFalse(lifecycle.isClosing)
        XCTAssertNil(lifecycle.deferredPresentation)
        XCTAssertEqual(lifecycle.routerPresentation(.profile), .profile)
    }

    func testLeavingTheHubReleasesAPendingClosure() {
        var lifecycle = opened()
        lifecycle.requestFallback(.mealPlanning)
        XCTAssertEqual(lifecycle.hostRemoved(), nil)
        XCTAssertEqual(lifecycle, HubSheetLifecycle())
        var deferred = opened()
        deferred.dismiss()
        _ = deferred.routerPresentation(.profile)
        XCTAssertEqual(deferred.hostRemoved(), .profile, "Sans `onDismiss`, la présentation n'est pas perdue.")
        XCTAssertEqual(deferred.routerPresentation(.settings), .settings, "Plus de fermeture en attente.")
    }
}
