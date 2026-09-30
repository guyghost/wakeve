import XCTest
import SwiftUI
@testable import Wakeve

/// Vue du hub d'événement (couche 4, #47) : fonctions pures et rendu aux grandes tailles.
@MainActor
final class EventHubViewTests: XCTestCase {
    private let fr = Locale(identifier: "fr")
    private let en = Locale(identifier: "en")

    private func facts(
        phase: EventHubFacts.Phase = .polling,
        isOrganizer: Bool = false,
        hasDetailsAccess: Bool = true,
        isLocalGuest: Bool = false,
        slotCount: Int = 3,
        leadingSlotStart: Date? = nil,
        finalDate: Date? = nil,
        confirmedCount: Int = 3,
        pendingCount: Int = 1,
        summaries: [HubModule: String] = [:]
    ) -> EventHubFacts {
        EventHubFacts(
            id: "e1", title: "Week-end à Annecy avec toute la bande", phase: phase,
            isOrganizer: isOrganizer, viewerAccepted: true,
            hasDetailsAccess: hasDetailsAccess || isOrganizer, isLocalGuest: isLocalGuest,
            pollOpen: true, userBallotComplete: false, ballotsKnown: true,
            votersWithCompleteBallot: 2, otherEligibleVoters: 4, otherVotersComplete: 1,
            slotCount: slotCount, leadingSlotStart: leadingSlotStart, finalDate: finalDate,
            confirmedCount: confirmedCount, pendingCount: pendingCount,
            participantNames: ["Léa Martin", "Tom Durand", "Inès"], summaries: summaries,
            eventTypeName: "WEEKEND", organizerId: isOrganizer ? "u" : "org"
        )
    }

    // MARK: - Fonctions pures

    func testSystemImagePerModule() {
        let expected: [HubModule: String] = [
            .date: "calendar", .location: "mappin.and.ellipse", .participants: "person.2",
            .budget: "eurosign.circle", .scenarios: "square.stack", .transport: "car",
            .accommodation: "bed.double", .meals: "fork.knife", .equipment: "backpack",
            .activities: "figure.hiking", .meetings: "video", .recap: "checkmark.seal",
            .photos: "photo.on.rectangle", .payments: "creditcard"
        ]
        for module in HubModule.allCases {
            XCTAssertEqual(EventHubView.systemImage(for: module), expected[module], module.rawValue)
        }
    }

    func testGridUsesOneColumnAtAccessibilitySizes() {
        XCTAssertEqual(EventHubView.columnCount(for: .large), 2)
        XCTAssertEqual(EventHubView.columnCount(for: .xxxLarge), 2)
        XCTAssertEqual(EventHubView.columnCount(for: .accessibility1), 1)
        XCTAssertEqual(EventHubView.columnCount(for: .accessibility5), 1)
    }

    func testPrimaryTitles() {
        let f = facts()
        XCTAssertEqual(EventHubView.primaryTitle(for: .vote, facts: f, locale: fr), "Voter")
        XCTAssertEqual(EventHubView.primaryTitle(for: .pollResults, facts: f, locale: fr), "Voir les résultats")
        XCTAssertEqual(EventHubView.primaryTitle(for: .organize, facts: f, locale: fr), "Passer en organisation")
        XCTAssertEqual(EventHubView.primaryTitle(for: .finalize, facts: f, locale: en), "Finalize event")
        XCTAssertEqual(EventHubView.primaryTitle(for: .signInToFinalize, facts: f, locale: fr), "Connecte-toi pour finaliser")
        XCTAssertEqual(EventHubView.primaryTitle(for: .addDates, facts: f, locale: en), "Add dates")
        XCTAssertNil(EventHubView.primaryTitle(for: .none, facts: f, locale: fr))
    }

    func testConfirmDateTitleNamesTheLeadingSlotWhenKnown() {
        XCTAssertEqual(EventHubView.primaryTitle(for: .confirmDate, facts: facts(), locale: fr), "Choisir la date")
        let slot = ISO8601DateFormatter().date(from: "2026-10-17T10:00:00Z")!
        let title = EventHubView.primaryTitle(for: .confirmDate, facts: facts(leadingSlotStart: slot), locale: fr)
        XCTAssertEqual(title, "Confirmer le " + HomeDateText.short(slot, locale: fr))
    }

    func testTileSummaryFallsBackToHintAndLockedText() {
        let f = facts(summaries: [.date: "3 créneaux"])
        let open = EventHubModel.Tile(module: .date, isLocked: false, isHighlighted: true, status: nil)
        let empty = EventHubModel.Tile(module: .location, isLocked: false, isHighlighted: false, status: nil)
        let locked = EventHubModel.Tile(module: .budget, isLocked: true, isHighlighted: false, status: nil)
        XCTAssertEqual(EventHubView.tileSummary(open, facts: f, locale: fr), "3 créneaux")
        XCTAssertEqual(EventHubView.tileSummary(empty, facts: f, locale: fr), "À préparer")
        XCTAssertEqual(EventHubView.tileSummary(locked, facts: f, locale: fr), "À confirmer d'abord")
    }

    func testFinalizedRecapAndPhotosHaveTheirOwnHint() {
        let f = facts(phase: .finalized)
        let recap = EventHubModel.Tile(module: .recap, isLocked: false, isHighlighted: false, status: nil)
        let photos = EventHubModel.Tile(module: .photos, isLocked: false, isHighlighted: false, status: nil)
        XCTAssertEqual(EventHubView.tileSummary(recap, facts: f, locale: fr), "Revois l'essentiel")
        XCTAssertEqual(EventHubView.tileSummary(photos, facts: f, locale: fr), "Partage tes photos")
        XCTAssertEqual(EventHubView.tileSummary(recap, facts: f, locale: en), "See the highlights")
        XCTAssertEqual(EventHubView.tileSummary(photos, facts: f, locale: en), "Share your photos")
        XCTAssertEqual(EventHubView.hintKey(for: .budget), "hub.tile.hint")
    }

    func testHeroSummaryCombinesSlotsAndGuests() {
        XCTAssertEqual(EventHubView.heroSummary(for: facts(), locale: en), "3 options · 4 guests")
        XCTAssertEqual(EventHubView.heroSummary(for: facts(slotCount: 0), locale: en), "4 guests")
        XCTAssertNil(EventHubView.heroSummary(for: facts(slotCount: 0, confirmedCount: 0, pendingCount: 0), locale: en))
        let day = ISO8601DateFormatter().date(from: "2026-10-17T10:00:00Z")!
        let confirmed = facts(phase: .confirmed, finalDate: day)
        XCTAssertEqual(EventHubView.heroSummary(for: confirmed, locale: en), HomeDateText.short(day, locale: en) + " · 4 guests")
    }

    func testMenuActionsFollowTheRole() {
        XCTAssertEqual(EventHubView.menuActions(for: facts(isOrganizer: true), invitationRollout: true),
                       [.info, .addParticipants, .support])
        XCTAssertEqual(EventHubView.menuActions(for: facts(phase: .finalized, isOrganizer: true), invitationRollout: true),
                       [.info, .support])
        XCTAssertEqual(EventHubView.menuActions(for: facts(), invitationRollout: true), [.info, .report, .support])
    }

    func testInfoMenuItemNeedsTheInvitationRollout() {
        // `.eventInformation` retombe sur le détail (donc le hub) sans rollout : l'entrée ne ferait rien.
        XCTAssertEqual(EventHubView.menuActions(for: facts(isOrganizer: true), invitationRollout: false),
                       [.addParticipants, .support])
        XCTAssertEqual(EventHubView.menuActions(for: facts(), invitationRollout: false), [.report, .support])
    }

    // MARK: - Aiguillage des actions (rollout invitation)

    func testModuleRoutesWithTheInvitationRollout() {
        func route(_ module: HubModule, _ phase: EventHubFacts.Phase = .polling) -> EventHubRoute {
            EventHubRouting.route(for: module, phase: phase, invitationRollout: true)
        }
        XCTAssertEqual(route(.date, .draft), .editDraft)
        XCTAssertEqual(route(.date), .screen(.pollResults))
        XCTAssertEqual(route(.participants), .invitationParticipants)
        XCTAssertEqual(route(.recap, .finalized), .screen(.eventInformation))
    }

    func testEveryModuleRouteWorksWithoutTheInvitationRollout() {
        func route(_ module: HubModule, _ phase: EventHubFacts.Phase = .polling) -> EventHubRoute {
            EventHubRouting.route(for: module, phase: phase, invitationRollout: false)
        }
        // Sans rollout, le routeur invitation retombe sur le détail : aucune route ne doit y mener.
        XCTAssertEqual(route(.date, .draft), .screen(.participantManagement), "Écran legacy qui porte « Ajouter des dates ».")
        XCTAssertEqual(route(.participants), .screen(.participantManagement))
        XCTAssertEqual(route(.recap, .finalized), .screen(.pollResults))
        let phases: [EventHubFacts.Phase] = [.draft, .polling, .comparing, .confirmed, .organizing, .finalized]
        for phase in phases {
            for module in HubModule.allCases {
                let r = route(module, phase)
                XCTAssertNotEqual(r, .editDraft, "\(module) \(phase)")
                XCTAssertNotEqual(r, .invitationParticipants, "\(module) \(phase)")
                for rolloutOnly in [AppView.eventInformation, .eventAudience, .eventArchive, .eventCreation, .eventDetail] {
                    XCTAssertNotEqual(r, .screen(rolloutOnly), "\(module) \(phase)")
                }
            }
        }
    }

    func testModuleScreens() {
        let expected: [HubModule: AppView] = [
            .location: .scenarioList, .scenarios: .scenarioList, .budget: .budgetOverview,
            .transport: .transportPlanning, .accommodation: .accommodation, .meals: .mealPlanning,
            .equipment: .equipmentChecklist, .activities: .activityPlanning, .meetings: .meetingList,
            .photos: .eventPhotos, .payments: .paymentPot
        ]
        for rollout in [true, false] {
            for (module, view) in expected {
                XCTAssertEqual(EventHubRouting.route(for: module, phase: .organizing, invitationRollout: rollout), .screen(view))
            }
        }
    }

    func testPrimaryRoutes() {
        for rollout in [true, false] {
            XCTAssertEqual(EventHubRouting.route(for: .vote, invitationRollout: rollout), .screen(.pollVoting))
            XCTAssertEqual(EventHubRouting.route(for: .pollResults, invitationRollout: rollout), .screen(.pollResults))
            XCTAssertEqual(EventHubRouting.route(for: .confirmDate, invitationRollout: rollout), .screen(.pollResults))
            for lifecycle in [EventHubModel.Primary.organize, .finalize, .signInToFinalize, .none] {
                XCTAssertNil(EventHubRouting.route(for: lifecycle, invitationRollout: rollout), "confirmé dans le hub")
            }
        }
        XCTAssertEqual(EventHubRouting.route(for: .addDates, invitationRollout: true), .editDraft)
        XCTAssertEqual(EventHubRouting.route(for: .addDates, invitationRollout: false), .screen(.participantManagement))
    }

    func testAddParticipantsRoute() {
        XCTAssertEqual(EventHubRouting.addParticipantsRoute(invitationRollout: true), .invitationParticipants)
        XCTAssertEqual(EventHubRouting.addParticipantsRoute(invitationRollout: false), .screen(.participantManagement))
    }

    func testLifecyclePrimaryActionsNeedAConfirmation() {
        XCTAssertEqual(EventHubView.lifecycleTarget(for: .organize), .organizing)
        XCTAssertEqual(EventHubView.lifecycleTarget(for: .finalize), .finalized)
        XCTAssertNil(EventHubView.lifecycleTarget(for: .vote))
        XCTAssertNil(EventHubView.lifecycleTarget(for: .signInToFinalize))
    }

    func testDateTileOpensVotingOnlyWhenAVoteIsRequired() {
        XCTAssertTrue(EventHubView.opensVoting(.date, facts: facts()))
        XCTAssertFalse(EventHubView.opensVoting(.location, facts: facts()))
        XCTAssertFalse(EventHubView.opensVoting(.date, facts: facts(phase: .confirmed)))
    }

    // MARK: - Rendu

    private func loadedViewModel(_ facts: EventHubFacts) async -> EventHubViewModel {
        struct Stub: EventHubSource {
            let facts: EventHubFacts
            func loadFacts(eventId: String, viewerId: String, isLocalGuest: Bool) async throws -> EventHubFacts { facts }
        }
        let vm = EventHubViewModel(eventId: "e1", viewerId: "u", isLocalGuest: false, source: Stub(facts: facts))
        await vm.reload()
        return vm
    }

    func testContentFitsThePhoneWidthAtAX5() async throws {
        let vm = await loadedViewModel(facts(phase: .organizing, isOrganizer: true, summaries: [
            .transport: "2 options", .meals: "3/5 repas prêts"
        ]))
        let model = try XCTUnwrap(vm.model)
        let facts = try XCTUnwrap(vm.facts)
        let content = EventHubContent(facts: facts, model: model, onOpenModule: { _ in }, onQuickVote: {})
        let size = fittingSize(content, width: 375, dynamicType: .accessibility5)
        XCTAssertGreaterThan(size.height, 0)
        XCTAssertLessThanOrEqual(size.width, 375)
    }

    func testQuickVoteCardRendersAtAX5() async throws {
        let slot = ISO8601DateFormatter().date(from: "2026-10-17T10:00:00Z")!
        let vm = await loadedViewModel(facts(leadingSlotStart: slot))
        let model = try XCTUnwrap(vm.model)
        XCTAssertTrue(model.showsQuickVote)
        let content = EventHubContent(facts: try XCTUnwrap(vm.facts), model: model, onOpenModule: { _ in }, onQuickVote: {})
        XCTAssertLessThanOrEqual(fittingSize(content, width: 375, dynamicType: .accessibility5).width, 375)
    }

    func testPrimaryBarKeepsTheMinimumTapTargetAtAX5() {
        let bar = EventHubPrimaryBar(title: "Passer en organisation", errorMessage: "Il manque le logement.", isBusy: false, action: {})
        let size = fittingSize(bar, width: 375, dynamicType: .accessibility5)
        XCTAssertGreaterThanOrEqual(size.height, WK.Size.minTapTarget)
        XCTAssertLessThanOrEqual(size.width, 375)
    }

    func testWholeHubRendersInEveryState() async {
        let vm = await loadedViewModel(facts())
        let hub = EventHubView(
            viewModel: vm, lifecycleError: nil, lifecycleInFlight: false,
            onBack: {}, onOpenModule: { _ in }, onPrimary: { _ in }, onLifecycle: { _ in },
            onRequestSignIn: {}, onOpenInfo: {}, onAddParticipants: {}, invitationRollout: false
        )
        let host = UIHostingController(rootView: hub)
        XCTAssertGreaterThan(host.sizeThatFits(in: CGSize(width: 375, height: 812)).height, 0)
    }

    func testLoadingPrimaryButtonKeepsTheTitleLayout() {
        // Le titre reste en place (masqué) sous l'indicateur : même taille, pas de saut de mise en page.
        let title = "Passer en organisation et prévenir tout le monde"
        for size in [DynamicTypeSize.large, .accessibility3] {
            let idle = fittingSize(WKPrimaryButton(title: title) {}, width: 375, dynamicType: size)
            let busy = fittingSize(WKPrimaryButton(title: title, isLoading: true) {}, width: 375, dynamicType: size)
            XCTAssertEqual(busy, idle, "\(size)")
            XCTAssertGreaterThanOrEqual(busy.height, WK.Size.minTapTarget)
        }
    }

    func testConfirmationTitleFollowsTheTarget() {
        XCTAssertEqual(EventHubView.confirmationTitleKey(for: .finalized), "event.lifecycle.finalize.title")
        XCTAssertEqual(EventHubView.confirmationTitleKey(for: .organizing), "event.lifecycle.organizing.title")
    }

    // MARK: - Contrat source

    private func hubSource() throws -> String {
        try String(contentsOf: URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent().deletingLastPathComponent()
            .appendingPathComponent("src/Views/Hub/EventHubView.swift"), encoding: .utf8)
    }

    func testHubUsesWKComponentsAndLockedTilesAreNotButtons() throws {
        let source = try hubSource()
        for component in ["WKCircleButton(", "WKCard(", "WKStatusPill(", "WKAvatarStack(", "WKModuleTile(", "WKPrimaryButton(", "WKChip("] {
            XCTAssertTrue(source.contains(component), component)
        }
        XCTAssertTrue(source.contains(".disabled(tile.isLocked)"))
        XCTAssertTrue(source.contains(".accessibilityRemoveTraits(.isButton)"))
        XCTAssertTrue(source.contains("safeAreaInset(edge: .bottom"))
        XCTAssertTrue(source.contains("EventMoodPalette.palette(for:"))
        XCTAssertTrue(source.contains("ModerationActionSheet(target:"))
        XCTAssertTrue(source.contains("event.lifecycle.guest.confirm_message"))
        XCTAssertTrue(source.contains("\"eventLifecycleError\""))
        XCTAssertFalse(source.contains("PollVotingView("), "Le vote rapide ouvre l'écran de vote, sans soumettre depuis le hub.")
    }

    func testConfirmationTitleStaysStableWhileTheDialogCloses() throws {
        let source = try hubSource()
        // La cible n'est pas effacée à la fermeture : le titre ne bascule pas pendant l'animation.
        XCTAssertTrue(source.contains("isPresented: $showsConfirmation"))
        XCTAssertTrue(source.contains("presenting: confirmationTarget"))
        XCTAssertFalse(source.contains("confirmationTarget = nil"))
        XCTAssertTrue(source.contains("confirmationTitleKey(for: confirmationTarget"))
    }

    func testHubChromeIsOpaqueAndLifecycleFeedbackIsAccessible() throws {
        let source = try hubSource()
        guard let top = source.range(of: "private var topBar: some View") else { return XCTFail("topBar") }
        let topBar = String(source[top.lowerBound...].prefix(1200))
        XCTAssertTrue(topBar.contains(".background(WK.Colors.canvas.ignoresSafeArea(edges: .top))"),
                      "Le contenu défile sous la barre du haut : elle doit être opaque.")
        guard let bar = source.range(of: "struct EventHubPrimaryBar: View") else { return XCTFail("EventHubPrimaryBar") }
        let primaryBar = String(source[bar.lowerBound...].prefix(2000))
        XCTAssertTrue(primaryBar.contains("exclamationmark.triangle.fill"))
        XCTAssertTrue(primaryBar.contains("WK.Status.actionNeeded.color"))
        XCTAssertTrue(primaryBar.contains("AccessibilityNotification.Announcement("))
        XCTAssertTrue(primaryBar.contains("isLoading: isBusy"))
        XCTAssertEqual(source.components(separatedBy: ".accessibilityHint(String(localized: \"hub.quick_vote.hint\"))").count - 1, 3,
                       "Chaque réponse du vote rapide dit qu'elle ouvre le vote.")
    }

    func testContainerClearsTheLifecycleErrorOnReloadAndReportsLoads() throws {
        let source = try hubSource()
        guard let start = source.range(of: "struct EventHubContainer: View") else { return XCTFail("container") }
        let container = String(source[start.lowerBound...])
        XCTAssertTrue(container.contains("onWillReload: { lifecycleError = nil }"))
        XCTAssertTrue(container.contains("let onLoaded: (EventHubFacts) -> Void"))
        XCTAssertTrue(container.contains(".onChange(of: viewModel.facts)"))
    }

    func testContainerOwnsTheViewModelAndReloadsOnToken() throws {
        let source = try hubSource()
        XCTAssertTrue(source.contains("struct EventHubContainer: View"))
        XCTAssertTrue(source.contains("@StateObject private var viewModel: EventHubViewModel"))
        XCTAssertTrue(source.contains(".onChange(of: reloadToken)"))
        XCTAssertTrue(source.contains("EventLifecycleTransitionController("))
    }
}
