import XCTest
@testable import Wakeve

/// Branchement de `ActivityView` dans la zone Activité du shell (couche 6, #47).
@MainActor
final class ActivityShellWiringTests: XCTestCase {

    private func contentViewSource() throws -> String {
        try String(contentsOf: URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent()
            .appendingPathComponent("src/Views/App/ContentView.swift"), encoding: .utf8)
    }

    private func slice(after anchor: String, length: Int) throws -> String {
        let source = try contentViewSource()
        guard let start = source.range(of: anchor) else {
            XCTFail("\(anchor) introuvable")
            return ""
        }
        return String(source[start.lowerBound...].prefix(length))
    }

    // MARK: - Filtre du lien profond

    func testNotificationsDeepLinkFilterMapsToActivityFilter() {
        XCTAssertEqual(AppRouter.activityFilter(for: "unread"), .all)
        XCTAssertEqual(AppRouter.activityFilter(for: nil), .toDo)
        XCTAssertEqual(AppRouter.activityFilter(for: "other"), .toDo)
    }

    func testPreRouteRequestsTheActivityFilter() {
        let router = AppRouter()
        XCTAssertEqual(router.activityFilter, .toDo)
        let request = router.activityFilterRequest

        XCTAssertNil(AppRouter.preRoute(.topLevel(.notifications(filter: "unread")), router: router))
        XCTAssertEqual(router.zone, .activity)
        XCTAssertEqual(router.activityFilter, .all)
        XCTAssertEqual(router.activityFilterRequest, request + 1, "Chaque lien profond est une nouvelle demande.")

        XCTAssertNil(AppRouter.preRoute(.topLevel(.notifications(filter: nil)), router: router))
        XCTAssertEqual(router.activityFilter, .toDo)
        XCTAssertEqual(router.activityFilterRequest, request + 2)

        XCTAssertEqual(AppRouter.preRoute(.event(.detail(eventId: "e")), router: router),
                       .event(.detail(eventId: "e")))
        XCTAssertEqual(router.activityFilterRequest, request + 2, "Les autres routes ne touchent pas au filtre.")
    }

    // MARK: - Shell

    func testActivityZoneShowsTheActivityFeedWithItsOwnBadge() throws {
        let body = try slice(after: "private var redesignChrome: some View", length: 2500)
        XCTAssertTrue(body.contains("ActivityView("))
        XCTAssertFalse(body.contains("InboxView("), "L'Inbox legacy est supprimée (couche 9).")
        XCTAssertTrue(body.contains("activityBadge: activityToDoCount"))
        XCTAssertTrue(body.contains("actionCount: $activityToDoCount"))
        XCTAssertTrue(body.contains("initialFilter: redesignRouter.activityFilter"))
        XCTAssertTrue(body.contains("filterRequestID: redesignRouter.activityFilterRequest"))
        XCTAssertTrue(body.contains("onOpen: openActivityTarget"))
        let source = try contentViewSource()
        XCTAssertTrue(source.contains("@State private var activityToDoCount = 0"))
        XCTAssertFalse(source.contains("unreadInboxCount"), "Plus de compteur d'Inbox legacy (couche 9).")
    }

    /// Les deux zones restent montées : le badge se recharge quand l'accueil ou le hub changent, au
    /// retour à la liste et au retour au premier plan (un vote fait ailleurs fait baisser le badge).
    func testActivityBadgeReloadsWhenEventsChangeElsewhere() throws {
        let body = try slice(after: "private var redesignChrome: some View", length: 4000)
        XCTAssertTrue(body.contains(".onChange(of: eventsHomeReloadToken) { _, _ in activityReloadToken += 1 }"))
        XCTAssertTrue(body.contains(".onChange(of: eventHubReloadToken) { _, _ in activityReloadToken += 1 }"))
        XCTAssertTrue(body.contains(".onChange(of: currentView) { _, view in\n            if view == .eventList { activityReloadToken += 1 }"))
        XCTAssertTrue(body.contains(".onChange(of: scenePhase) { _, phase in\n            if phase == .active { activityReloadToken += 1 }"))
        let source = try contentViewSource()
        XCTAssertTrue(source.contains("@Environment(\\.scenePhase) private var scenePhase"))
        XCTAssertTrue(source.contains("/// Badge de la zone Activité de la refonte (couche 6, #47) : éléments « À traiter » (actions seulement)."))
    }

    func testOpeningAnActivityTargetSwitchesZoneThenNavigatesOnTheNextTurn() throws {
        let body = try slice(after: "private func openActivityTarget(_ target: ActivityTarget)", length: 1500)
        guard let zone = body.range(of: "redesignRouter.zone = .events"),
              let deferred = body.range(of: "Task { @MainActor in") else {
            return XCTFail("Zone puis navigation différée attendues")
        }
        XCTAssertLessThan(zone.lowerBound, deferred.lowerBound, "Le changement de zone précède la navigation.")
        XCTAssertTrue(body.contains("openEventFromHome(eventId)"), "Hub : même ouverture que l'accueil.")
        XCTAssertTrue(body.contains("openHomeAction(.vote, eventId: eventId)"))
        XCTAssertTrue(body.contains("openHomeAction(.pollResults, eventId: eventId)"))
        XCTAssertTrue(body.contains("selectedCommentSection = .general"))
        XCTAssertTrue(body.contains("navigateInvitationDeepLink(eventId: eventId, destination: .comments)"),
                      "Commentaires : même route (et mêmes gardes) que le lien profond.")
    }

    func testHomeNextStepAndActivityShareOnePollRoute() throws {
        let body = try slice(after: "private func handleHomeNextStep(_ step: HomeNextStep)", length: 400)
        XCTAssertTrue(body.contains("openHomeAction(step.action, eventId: step.eventId)"))
        let action = try slice(after: "private func openHomeAction(_ action: HomeNextStep.Action, eventId: String)", length: 1200)
        XCTAssertTrue(action.contains("guard invitationExperienceRolloutEnabled else"))
        XCTAssertTrue(action.contains("destination: .pollVoting"))
        XCTAssertTrue(action.contains("route: .poll, intent: .mutate"))
    }
}
