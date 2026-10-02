import XCTest
import SwiftUI
@testable import Wakeve

@MainActor
final class RedesignShellTests: XCTestCase {

    func testNavBarShowsOnlyAtZoneRoots() {
        typealias Shell = RedesignShellView<EmptyView, EmptyView>
        XCTAssertTrue(Shell.showsNavBar(zone: .events, eventsAtRoot: true, activityAtRoot: false))
        XCTAssertFalse(Shell.showsNavBar(zone: .events, eventsAtRoot: false, activityAtRoot: true))
        XCTAssertTrue(Shell.showsNavBar(zone: .activity, eventsAtRoot: false, activityAtRoot: true))
        XCTAssertFalse(
            Shell.showsNavBar(zone: .activity, eventsAtRoot: true, activityAtRoot: false),
            "Activité hors racine (détail poussé ou mode sélection) : la barre se retire."
        )
    }

    func testNavBarAnimationIsScopedToTheBar() throws {
        let source = try String(
            contentsOf: URL(fileURLWithPath: #filePath)
                .deletingLastPathComponent().deletingLastPathComponent()
                .appendingPathComponent("src/Views/App/RedesignShellView.swift"),
            encoding: .utf8
        )
        guard let inset = source.range(of: ".safeAreaInset(edge: .bottom") else {
            return XCTFail("Inset de la barre introuvable")
        }
        let beforeInset = String(source[..<inset.lowerBound])
        XCTAssertFalse(beforeInset.contains(".animation("), "Le basculement de zone ne doit pas être animé.")
        let insetBody = String(source[inset.lowerBound...].prefix(900))
        XCTAssertTrue(insetBody.contains(".animation("), "Seule la barre anime son apparition.")
    }

    func testActivityZoneReloadsAndReportsItsRootState() throws {
        let source = try contentViewSource()
        guard let start = source.range(of: "private var redesignChrome: some View") else {
            return XCTFail("redesignChrome introuvable")
        }
        let body = String(source[start.lowerBound...].prefix(2500))
        XCTAssertTrue(body.contains("activityAtRoot: activityAtRoot"))
        XCTAssertTrue(body.contains("reloadToken: activityReloadToken"))
        XCTAssertTrue(body.contains("activityReloadToken += 1"))

        // Couche 6 : la zone Activité affiche `ActivityView` (l'Inbox legacy est supprimée en couche 9).
        XCTAssertTrue(body.contains("ActivityView("))
        let activity = try String(
            contentsOf: URL(fileURLWithPath: #filePath)
                .deletingLastPathComponent().deletingLastPathComponent()
                .appendingPathComponent("src/Views/Activity/ActivityView.swift"),
            encoding: .utf8
        )
        XCTAssertTrue(activity.contains("var reloadToken: Int = 0"))
        XCTAssertTrue(activity.contains("var onRootStateChange: ((Bool) -> Void)? = nil"))
        XCTAssertTrue(activity.contains(".onChange(of: reloadToken)"))
    }

    func testHeaderShowsOnlyAtEventsRoot() {
        XCTAssertTrue(RedesignShellView<EmptyView, EmptyView>.showsHeader(zone: .events, eventsAtRoot: true))
        XCTAssertFalse(RedesignShellView<EmptyView, EmptyView>.showsHeader(zone: .events, eventsAtRoot: false))
        XCTAssertFalse(RedesignShellView<EmptyView, EmptyView>.showsHeader(zone: .activity, eventsAtRoot: true))
    }

    func testShellRendersSmoke() throws {
        let router = AppRouter()
        let shell = RedesignShellView(
            router: router,
            eventsAtRoot: true,
            activityAtRoot: true,
            activityBadge: 2,
            userId: "u1",
            userName: "Léa Martin",
            onCreate: {},
            events: { Text("EVENTS") },
            activity: { Text("ACTIVITY") }
        )
        let host = UIHostingController(rootView: shell)
        let size = host.sizeThatFits(in: CGSize(width: 390, height: 844))
        XCTAssertGreaterThan(size.height, 0)
    }

    func testShellUsesWKChromeAndKeepsZonesMounted() throws {
        let source = try String(
            contentsOf: URL(fileURLWithPath: #filePath)
                .deletingLastPathComponent().deletingLastPathComponent()
                .appendingPathComponent("src/Views/App/RedesignShellView.swift"),
            encoding: .utf8
        )
        XCTAssertTrue(source.contains("WKFloatingNavBar("))
        XCTAssertTrue(source.contains("WKCircleButton("))
        XCTAssertTrue(source.contains(".accessibilityHidden(router.zone != .events)"), "La zone inactive reste montée mais masquée.")
        XCTAssertTrue(source.contains(".accessibilityHidden(router.zone != .activity)"))
    }

    private func contentViewSource() throws -> String {
        try String(
            contentsOf: URL(fileURLWithPath: #filePath)
                .deletingLastPathComponent().deletingLastPathComponent()
                .appendingPathComponent("src/Views/App/ContentView.swift"),
            encoding: .utf8
        )
    }

    /// Couche 9 : la refonte est le seul shell ; plus de flag `iosRedesign2026`.
    func testAuthenticatedViewInstallsTheRedesignShellUnconditionally() throws {
        let source = try contentViewSource()
        XCTAssertFalse(source.contains("iosRedesign2026"), "Le flag de la refonte est retiré.")
        XCTAssertFalse(source.contains("FeatureFlags"))
        guard let body = source.range(of: "    var body: some View {\n        // Shell de la refonte") else {
            return XCTFail("body d'AuthenticatedView introuvable")
        }
        XCTAssertTrue(String(source[body.lowerBound...].prefix(300)).contains("redesignChrome"))
        XCTAssertTrue(source.contains("RedesignShellView("))
        XCTAssertTrue(source.contains("@State private var redesignRouter = AppRouter()"))
    }

    func testDeepLinksGoThroughTheRouter() throws {
        let source = try contentViewSource()
        guard let start = source.range(of: "private func handleDeepLinkNavigation(_ route: IosRoute)") else {
            return XCTFail("handleDeepLinkNavigation introuvable")
        }
        let body = String(source[start.lowerBound...].prefix(1200))
        XCTAssertTrue(
            body.contains("AppRouter.preRoute(route, router: redesignRouter)"),
            "Toute route passe d'abord par le pré-aiguillage du routeur."
        )
    }

    func testShellPresentsProfileAndSettingsFromOneRouterOwnedSheet() throws {
        let source = try contentViewSource()
        guard let start = source.range(of: "private var redesignChrome: some View") else {
            return XCTFail("redesignChrome introuvable")
        }
        let body = String(source[start.lowerBound...].prefix(2000))
        XCTAssertTrue(body.contains(".sheet(item: $redesignRouter.presentation)"), "Une seule feuille pilotée par le routeur.")
        XCTAssertFalse(body.contains("showNotificationPreferencesSheet"), "Plus de relais vers la feuille legacy.")
    }

    func testRedesignRootInstallsTheNewHome() throws {
        let source = try contentViewSource()
        guard let start = source.range(of: "private var invitationExperienceRootContent") else { return XCTFail() }
        let slice = String(source[start.lowerBound...].prefix(1500))
        // Conteneur propriétaire du modèle de vue, qui rend `EventsHomeView` : seule racine (couche 9).
        XCTAssertTrue(slice.contains("EventsHomeContainer("))
        XCTAssertTrue(slice.contains("invitationRollout: invitationExperienceRolloutEnabled"))
        XCTAssertFalse(slice.contains("EventListView("), "Plus de liste legacy.")
        XCTAssertFalse(slice.contains("eventLibraryContent"), "Plus de bibliothèque legacy.")
    }

    func testShellHeaderShowsTheWordmark() throws {
        let source = try String(contentsOf: URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent().deletingLastPathComponent()
            .appendingPathComponent("src/Views/App/RedesignShellView.swift"), encoding: .utf8)
        XCTAssertTrue(source.contains("\"wk.wordmark\""))
    }

    func testHomeNextStepOpensPollScreensWithoutInvitationRollout() throws {
        let source = try contentViewSource()
        guard let start = source.range(of: "private func handleHomeNextStep(_ step: HomeNextStep)") else {
            return XCTFail()
        }
        let slice = String(source[start.lowerBound...].prefix(1200))
        // Sans rollout invitation, le routeur retombe sur le détail : « Voter » doit ouvrir le vote.
        XCTAssertTrue(slice.contains("guard invitationExperienceRolloutEnabled else"))
        XCTAssertTrue(slice.contains("destination: .pollVoting"))
        XCTAssertTrue(slice.contains("destination: .pollResults"))
    }

    func testShellHeaderIsOpaqueOverScrolledContent() throws {
        let source = try String(contentsOf: URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent().deletingLastPathComponent()
            .appendingPathComponent("src/Views/App/RedesignShellView.swift"), encoding: .utf8)
        guard let start = source.range(of: "private var header: some View") else { return XCTFail() }
        let slice = String(source[start.lowerBound...])
        // Le contenu de l'accueil défile sous l'en-tête : sans fond, il se superpose au logotype.
        XCTAssertTrue(slice.contains(".background(WK.Colors.canvas.ignoresSafeArea(edges: .top))"))
    }

    // MARK: - Hub d'événement (couche 4)

    func testEventDetailOpensTheHub() throws {
        let source = try contentViewSource()
        guard let start = source.range(of: "case .eventDetail:"),
              let end = source.range(of: "case .eventAudience:", range: start.upperBound..<source.endIndex) else {
            return XCTFail("Tranche case .eventDetail introuvable")
        }
        let slice = String(source[start.upperBound..<end.lowerBound])
        XCTAssertTrue(slice.hasPrefix("\n            if let event = selectedEvent {"), "Le hub est la surface de détail (couche 9).")
        XCTAssertTrue(slice.contains("eventHubContent(for: event)"))
        XCTAssertFalse(slice.contains("ArtworkNone.shared"))
        XCTAssertFalse(slice.contains("invitationQAArtwork"))
        XCTAssertFalse(slice.contains("EventHubContainer("), "Les callbacks du hub vivent hors de la tranche legacy.")
    }

    func testHubCallbacksReuseTheExistingRoutes() throws {
        let source = try contentViewSource()
        XCTAssertTrue(source.contains("@State private var eventHubReloadToken = 0"))
        guard let start = source.range(of: "private func eventHubContent(for event: Event)") else {
            return XCTFail("eventHubContent introuvable")
        }
        let body = String(source[start.lowerBound...].prefix(4000))
        XCTAssertTrue(body.contains("EventHubContainer("))
        XCTAssertTrue(body.contains("reloadToken: eventHubReloadToken"))
        XCTAssertTrue(body.contains("isLocalGuest: authStateManager.isCurrentSessionGuest"))
        XCTAssertTrue(body.contains("invitationRollout: invitationExperienceRolloutEnabled"))
        XCTAssertTrue(body.contains("eventHubReloadToken += 1"), "Une transition réussie recharge le hub.")
        XCTAssertTrue(body.contains("authStateManager.signOut()"), "Même entrée de connexion que le détail legacy.")
        XCTAssertTrue(body.contains("currentView = .eventInformation"))
        XCTAssertTrue(body.contains(".id(event.id)"), "Un autre événement recrée le modèle de vue.")
        // Aiguillage pur et testé (`EventHubRouting`), exécuté par un seul point d'entrée.
        XCTAssertTrue(body.contains("EventHubRouting.route(for: module, phase:"))
        XCTAssertTrue(body.contains("EventHubRouting.route(for: primary, invitationRollout:"))
        XCTAssertTrue(body.contains("EventHubRouting.addParticipantsRoute("))
        XCTAssertTrue(body.contains("case .screen(let view):"))
        XCTAssertTrue(body.contains("InvitationExperienceRouteRequestParticipants.shared"), "Même route que onManageParticipants.")
        XCTAssertTrue(body.contains("editDraftFromHome(event.id)"), "Ajouter des dates reprend le chemin brouillon de l'accueil.")
    }

    // MARK: - Retour hors racine (couche 4)

    func testBackRouteReturnsToTheParentScreen() {
        XCTAssertEqual(RedesignBackRoute.destination(from: .budgetOverview, organizationAccess: true), .eventDetail)
        XCTAssertEqual(RedesignBackRoute.destination(from: .budgetDetail, organizationAccess: true), .budgetOverview)
        XCTAssertEqual(RedesignBackRoute.destination(from: .meetingList, organizationAccess: true), .eventDetail)
        XCTAssertEqual(RedesignBackRoute.destination(from: .meetingDetail, organizationAccess: true), .meetingList)
        XCTAssertEqual(RedesignBackRoute.destination(from: .paymentPot, organizationAccess: true), .eventDetail)
        XCTAssertEqual(RedesignBackRoute.destination(from: .tricount, organizationAccess: true), .paymentPot)
        XCTAssertEqual(RedesignBackRoute.destination(from: .eventAudience, organizationAccess: false), .eventDetail)
    }

    /// Couche 9 : `wakeve://leaderboard` n'a pas de retour propre ; sans cette route, l'utilisateur reste bloqué.
    func testLeaderboardDeepLinkReturnsToTheEventsRoot() {
        XCTAssertEqual(RedesignBackRoute.destination(from: .leaderboard, organizationAccess: true), .eventList)
        XCTAssertEqual(RedesignBackRoute.destination(from: .leaderboard, organizationAccess: false), .eventList)
        XCTAssertFalse(RedesignBackRoute.needsOrganizationAccess(.leaderboard))
        XCTAssertEqual(RedesignBackRoute.placement(for: .leaderboard), .inset)
    }

    func testBackRouteStaysOffScreensThatAlreadyHaveABackControl() {
        // Écrans avec leur propre retour (onBack/onDone/onReturn) ou racine : pas de second bouton.
        for view in [AppView.eventList, .eventDetail, .eventCreation, .eventInformation, .eventArchive, .organizerDashboard,
                     .participantManagement, .pollVoting, .pollResults, .scenarioList, .accommodation,
                     .mealPlanning, .equipmentChecklist, .activityPlanning, .transportPlanning, .eventPhotos] {
            XCTAssertNil(RedesignBackRoute.destination(from: view, organizationAccess: true), "\(view)")
        }
        // Sans accès, ces écrans affichent `AccessDenied`, qui porte déjà son retour.
        for view in [AppView.budgetOverview, .budgetDetail, .meetingList, .meetingDetail, .paymentPot, .tricount] {
            XCTAssertNil(RedesignBackRoute.destination(from: view, organizationAccess: false), "\(view)")
        }
    }

    func testScreensThatPushADetailHostTheBackInTheirOwnToolbar() throws {
        // Un détail poussé (dépenses, réunion) remplace ce bouton par le retour système, sans superposition.
        XCTAssertEqual(RedesignBackRoute.placement(for: .budgetOverview), .toolbar)
        XCTAssertEqual(RedesignBackRoute.placement(for: .meetingList), .toolbar)
        for view in [AppView.budgetDetail, .meetingDetail, .paymentPot, .tricount, .eventAudience] {
            XCTAssertEqual(RedesignBackRoute.placement(for: view), .inset, "\(view)")
        }
        let root = URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent()
        for file in ["src/Views/Budget/BudgetOverviewView.swift", "src/Views/Meeting/MeetingListView.swift"] {
            let source = try String(contentsOf: root.appendingPathComponent(file), encoding: .utf8)
            XCTAssertTrue(source.contains("@Environment(\\.redesignBackAction) private var redesignBackAction"), file)
            XCTAssertTrue(source.contains("if let redesignBackAction {"), file)
            XCTAssertTrue(source.contains("Button(action: redesignBackAction)"), file)
        }
    }

    func testRedesignEventsZoneInstallsTheBackBarOutsideTheLegacyCases() throws {
        let source = try contentViewSource()
        guard let start = source.range(of: "private var redesignChrome: some View") else {
            return XCTFail("redesignChrome introuvable")
        }
        let chrome = String(source[start.lowerBound...].prefix(1200))
        XCTAssertTrue(chrome.contains("homeTabContent"))
        XCTAssertTrue(chrome.contains(".safeAreaInset(edge: .top, spacing: 0)"), "Le retour ne recouvre pas le contenu de l'écran.")
        XCTAssertTrue(chrome.contains("redesignBackBar"))
        XCTAssertTrue(chrome.contains(".environment(\\.redesignBackAction, redesignToolbarBackAction)"))
        guard let bar = source.range(of: "private var redesignBackDestination: AppView?") else {
            return XCTFail("redesignBackDestination introuvable")
        }
        let barBody = String(source[bar.lowerBound...].prefix(1800))
        XCTAssertTrue(barBody.contains("RedesignBackRoute.destination(from: currentView"))
        XCTAssertTrue(barBody.contains("RedesignBackRoute.placement(for: currentView) == .inset"))
        XCTAssertTrue(barBody.contains("RedesignBackRoute.placement(for: currentView) == .toolbar"))
        XCTAssertTrue(barBody.contains("WKCircleButton("))
        XCTAssertTrue(barBody.contains("String(localized: \"common.back\")"))
        XCTAssertTrue(barBody.contains("canAccessOrganizationDashboard(for:"))
    }

    // MARK: - Revue de la couche 4

    func testOnlyGuardedScreensNeedTheOrganizationAccess() {
        let guarded: [AppView] = [.budgetOverview, .budgetDetail, .meetingList, .meetingDetail, .paymentPot, .tricount]
        for view in guarded {
            XCTAssertTrue(RedesignBackRoute.needsOrganizationAccess(view), "\(view)")
        }
        for view in [AppView.eventList, .eventDetail, .eventAudience, .pollVoting, .scenarioList, .transportPlanning] {
            XCTAssertFalse(RedesignBackRoute.needsOrganizationAccess(view), "\(view)")
        }
    }

    func testBackDestinationComputesAccessOnlyForGuardedScreens() throws {
        let source = try contentViewSource()
        guard let start = source.range(of: "private var redesignBackDestination: AppView?") else {
            return XCTFail("redesignBackDestination introuvable")
        }
        let body = String(source[start.lowerBound...].prefix(600))
        XCTAssertTrue(body.contains("RedesignBackRoute.needsOrganizationAccess(currentView)"))
    }

    func testHubKeepsTheSelectedEventFresh() throws {
        let source = try contentViewSource()
        guard let start = source.range(of: "private func eventHubContent(for event: Event)"),
              let route = source.range(of: "private func performHubRoute(_ route: EventHubRoute, for event: Event)") else {
            return XCTFail("hub introuvable")
        }
        let hub = String(source[start.lowerBound...].prefix(2000))
        XCTAssertTrue(hub.contains("onLoaded: { facts in refreshSelectedEvent(from: facts) }"))
        let perform = String(source[route.lowerBound...].prefix(700))
        XCTAssertTrue(perform.contains("repository.getEvent(id: event.id)"), "Relire l'événement avant de naviguer.")
    }

    func testRedesignToolbarBackUsesTopBarLeading() throws {
        let root = URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent()
        for file in ["src/Views/Budget/BudgetOverviewView.swift", "src/Views/Meeting/MeetingListView.swift"] {
            let source = try String(contentsOf: root.appendingPathComponent(file), encoding: .utf8)
            guard let start = source.range(of: "if let redesignBackAction {") else { return XCTFail(file) }
            let item = String(source[start.lowerBound...].prefix(400))
            XCTAssertTrue(item.contains("ToolbarItem(placement: .topBarLeading)"), file)
        }
    }
}
