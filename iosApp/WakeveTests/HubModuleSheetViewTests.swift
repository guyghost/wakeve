import XCTest
import SwiftUI
@testable import Wakeve

/// Sheets de modules du hub (couche 5a, #47) : routage, repli plein écran, modèle de vue, rendu.
@MainActor
final class HubModuleSheetViewTests: XCTestCase {
    private let fr = Locale(identifier: "fr")

    // MARK: - Routage

    func testSimpleModulesOpenInSheetsAndOthersKeepTheirScreens() {
        XCTAssertEqual(EventHubRouting.sheetModules, [
            .meals, .equipment, .activities, .accommodation, .photos, .budget, .payments, .meetings
        ])
        for rollout in [true, false] {
            for phase in [EventHubFacts.Phase.organizing, .finalized, .confirmed] {
                for module in EventHubRouting.sheetModules {
                    XCTAssertEqual(EventHubRouting.route(for: module, phase: phase, invitationRollout: rollout), .sheet(module), "\(module) \(phase)")
                }
                XCTAssertEqual(EventHubRouting.route(for: .transport, phase: phase, invitationRollout: rollout), .screen(.transportPlanning))
            }
        }
    }

    /// Couche 5b : budget, cagnotte et réunions gardés comme leur `case` legacy (`canAccessOrganizationDashboard`).
    func testEachSheetUsesTheGuardOfItsLegacyCase() {
        for module in HubModule.allCases {
            let expected: EventHubRouting.SheetGuard = [.budget, .payments, .meetings].contains(module)
                ? .organizationDashboard : .detailedPlanning
            XCTAssertEqual(EventHubRouting.sheetGuard(for: module), expected, "\(module)")
        }
    }

    func testPrimaryActionsFallBackToTheLegacyScreens() {
        XCTAssertNil(EventHubRouting.fallback(for: .addMeal), "Le repas s'ajoute depuis la sheet.")
        XCTAssertEqual(EventHubRouting.fallback(for: .viewExpenses), .budgetOverview)
        XCTAssertEqual(EventHubRouting.fallback(for: .managePot), .paymentPot)
        XCTAssertEqual(EventHubRouting.fallback(for: .planMeeting), .meetingList)
        XCTAssertEqual(HubModuleSheetView.fallback(for: .tricount), .tricount)
        XCTAssertNil(HubModuleSheetView.fallback(for: .fullScreen), "Plein écran : repli du module.")
        XCTAssertNil(HubModuleSheetView.fallback(for: .comments))
    }

    func testModuleRoutingReadsTheSheetListOnly() throws {
        let routing = try source("src/Views/Hub/EventHubRouting.swift")
        XCTAssertTrue(routing.contains("if sheetModules.contains(module) { return .sheet(module) }"))
        XCTAssertEqual(routing.components(separatedBy: "return .sheet(module)").count - 1, 2,
                       "Un seul `.sheet(module)` pour les modules (plus `sheetRoute`).")
    }

    func testFullScreenFallbackIsTheLegacyScreenOfEachSheetModule() {
        let expected: [HubModule: AppView] = [
            .meals: .mealPlanning, .equipment: .equipmentChecklist, .activities: .activityPlanning,
            .accommodation: .accommodation, .photos: .eventPhotos,
            .budget: .budgetOverview, .payments: .paymentPot, .meetings: .meetingList
        ]
        for module in HubModule.allCases {
            XCTAssertEqual(EventHubRouting.fullScreenFallback(for: module), expected[module], "\(module)")
        }
    }

    func testSheetOpensOnlyWithTheLegacyAccessOtherwiseTheLegacyScreenRefuses() {
        for module in EventHubRouting.sheetModules {
            XCTAssertEqual(EventHubRouting.sheetRoute(for: module, accessGranted: true), .sheet(module))
            XCTAssertEqual(EventHubRouting.sheetRoute(for: module, accessGranted: false),
                           EventHubRouting.fullScreenFallback(for: module).map(EventHubRoute.screen))
        }
    }

    func testCommentSectionsMatchTheLegacyScreens() {
        XCTAssertEqual(EventHubRouting.commentSection(for: .meals), .meal)
        XCTAssertEqual(EventHubRouting.commentSection(for: .equipment), .equipment)
        XCTAssertEqual(EventHubRouting.commentSection(for: .activities), .activity)
        XCTAssertEqual(EventHubRouting.commentSection(for: .accommodation), .accommodation)
        XCTAssertNil(EventHubRouting.commentSection(for: .photos))
    }

    func testHubModuleIsIdentifiedByItsRawValue() {
        XCTAssertEqual(HubModule.meals.id, "meals")
    }

    // MARK: - Actions

    func testSecondaryActionsOfferFullScreenAndCommentsWhereTheyExist() {
        XCTAssertEqual(HubModuleSheetView.secondaryActions(for: .meals), [.fullScreen, .comments])
        XCTAssertEqual(HubModuleSheetView.secondaryActions(for: .accommodation), [.fullScreen, .comments])
        XCTAssertEqual(HubModuleSheetView.secondaryActions(for: .photos), [.fullScreen])
        XCTAssertEqual(HubModuleSheetView.secondaryActions(for: .budget), [.fullScreen])
        XCTAssertEqual(HubModuleSheetView.secondaryActions(for: .payments), [.tricount, .fullScreen])
        XCTAssertEqual(HubModuleSheetView.secondaryActions(for: .meetings), [.fullScreen])
    }

    func testLayer5bPrimaryTitlesFollowRoleAndReadOnly() {
        func data(_ raw: HubModuleSheetRaw, organizer: Bool, readOnly: Bool) -> HubModuleSheetData {
            HubModuleSheetData.make(raw: raw, isOrganizer: organizer, isReadOnly: readOnly, pendingSync: false, locale: fr)
        }
        func title(_ module: HubModule, _ state: HubModuleSheetViewModel.State, _ data: HubModuleSheetData?, hint: Bool) -> String? {
            HubModuleSheetView.primaryTitle(module: module, state: state, data: data, canAddHint: hint, locale: fr)
        }
        // Consultation ouverte à tous, dès la première image.
        XCTAssertEqual(title(.budget, .loading, nil, hint: false), "Voir les dépenses")
        XCTAssertEqual(title(.budget, .loaded, data(.budget(nil), organizer: false, readOnly: true), hint: false), "Voir les dépenses")
        XCTAssertEqual(title(.payments, .loading, nil, hint: true), "Gérer la cagnotte")
        XCTAssertNil(title(.payments, .loading, nil, hint: false))
        XCTAssertNil(title(.payments, .loaded, data(.payments(pot: nil, tricount: .undecided), organizer: true, readOnly: true), hint: true))
        XCTAssertEqual(title(.meetings, .loaded, data(.meetings([]), organizer: true, readOnly: false), hint: false), "Planifier une réunion")
        XCTAssertNil(title(.meetings, .loaded, data(.meetings([]), organizer: false, readOnly: false), hint: true))
        XCTAssertNil(title(.budget, .failed, nil, hint: true), "Échec : seule la relance est proposée.")
    }

    func testPrimaryAddsAMealOnlyWhenAllowed() {
        let organizer = HubModuleSheetData.make(raw: .meals([]), isOrganizer: true, isReadOnly: false, pendingSync: false, locale: fr)
        let guest = HubModuleSheetData.make(raw: .meals([]), isOrganizer: false, isReadOnly: false, pendingSync: false, locale: fr)
        let photos = HubModuleSheetData.make(raw: .photos, isOrganizer: true, isReadOnly: false, pendingSync: false, locale: fr)
        XCTAssertEqual(HubModuleSheetView.primaryTitle(for: organizer, locale: fr), "Ajouter un repas")
        XCTAssertNil(HubModuleSheetView.primaryTitle(for: guest, locale: fr))
        XCTAssertNil(HubModuleSheetView.primaryTitle(for: photos, locale: fr))
        XCTAssertNil(HubModuleSheetView.primaryTitle(for: nil, locale: fr))
    }

    func testPrimaryShowsFromTheFirstFrameWithTheCallerHint() {
        let organizer = HubModuleSheetData.make(raw: .meals([]), isOrganizer: true, isReadOnly: false, pendingSync: false, locale: fr)
        let guest = HubModuleSheetData.make(raw: .meals([]), isOrganizer: false, isReadOnly: false, pendingSync: false, locale: fr)
        func title(_ module: HubModule, _ state: HubModuleSheetViewModel.State, _ data: HubModuleSheetData?, hint: Bool) -> String? {
            HubModuleSheetView.primaryTitle(module: module, state: state, data: data, canAddHint: hint, locale: fr)
        }
        // Chargement : l'indice de l'appelant (organisateur, non finalisé) affiche déjà la barre.
        XCTAssertEqual(title(.meals, .loading, nil, hint: true), "Ajouter un repas")
        XCTAssertNil(title(.meals, .loading, nil, hint: false))
        XCTAssertNil(title(.equipment, .loading, nil, hint: true), "Seul le module Repas a un formulaire.")
        // Chargé : les données font foi.
        XCTAssertEqual(title(.meals, .loaded, organizer, hint: false), "Ajouter un repas")
        XCTAssertNil(title(.meals, .loaded, guest, hint: true))
        XCTAssertNil(title(.meals, .failed, nil, hint: true), "Échec : seule la relance est proposée.")
    }

    func testFormMealBecomesARawMealWithItsStatusName() {
        let model = MealModel(
            id: "m", eventId: "e", type: .dinner, name: "Raclette", date: "2026-10-03", time: "20:00",
            location: nil, responsibleParticipantIds: ["lea@example.com"], estimatedCost: 0, actualCost: nil,
            servings: 6, status: .completed, notes: nil, createdAt: "", updatedAt: ""
        )
        XCTAssertEqual(HubModuleSheetView.rawMeal(from: model), HubModuleSheetRaw.Meal(
            id: "m", name: "Raclette", date: "2026-10-03", time: "20:00", servings: 6,
            statusName: "COMPLETED", responsibleNames: ["lea@example.com"]
        ))
    }

    // MARK: - Modèle de vue

    private struct Stub: EventModuleSheetSource {
        struct Boom: Error {}
        var raw: HubModuleSheetRaw?
        func load(module: HubModule, eventId: String, viewerId: String) async throws -> HubModuleSheetData {
            guard let raw else { throw Boom() }
            return HubModuleSheetData.make(raw: raw, isOrganizer: true, isReadOnly: false, pendingSync: true)
        }
    }

    private let barbecue = HubModuleSheetRaw.Meal(id: "a", name: "Barbecue", date: "2026-10-03", time: "19:30",
                                                  servings: 8, statusName: "COMPLETED", responsibleNames: ["Léa"])
    private let raclette = HubModuleSheetRaw.Meal(id: "b", name: "Raclette", date: "2026-10-04", time: "20:00",
                                                  servings: 6, statusName: "PLANNED", responsibleNames: [])

    func testViewModelLoadsOrFails() async {
        let loaded = HubModuleSheetViewModel(module: .meals, eventId: "e", viewerId: "u", source: Stub(raw: .meals([barbecue])))
        XCTAssertEqual(loaded.state, .loading)
        await loaded.reload()
        XCTAssertEqual(loaded.state, .loaded)
        XCTAssertEqual(loaded.data?.items.map(\.id), ["a"])
        let failed = HubModuleSheetViewModel(module: .meals, eventId: "e", viewerId: "u", source: Stub(raw: nil))
        await failed.reload()
        XCTAssertEqual(failed.state, .failed)
        XCTAssertNil(failed.data)
    }

    /// Source dont chaque appel attend d'être libéré par le test, dans l'ordre choisi.
    private final class GatedSource: EventModuleSheetSource, @unchecked Sendable {
        private let lock = NSLock()
        private var waiting: [CheckedContinuation<HubModuleSheetData, Error>] = []

        var pendingCount: Int { lock.withLock { waiting.count } }

        func load(module: HubModule, eventId: String, viewerId: String) async throws -> HubModuleSheetData {
            try await withCheckedThrowingContinuation { continuation in
                lock.withLock { waiting.append(continuation) }
            }
        }

        func resume(_ index: Int, with result: Result<HubModuleSheetData, Error>) {
            let continuation = lock.withLock { waiting[index] }
            continuation.resume(with: result)
        }
    }

    private func waitUntil(_ condition: @escaping () -> Bool) async {
        for _ in 0..<500 where !condition() {
            await Task.yield()
            try? await Task.sleep(nanoseconds: 1_000_000)
        }
    }

    private func mealsData(_ meals: [HubModuleSheetRaw.Meal]) -> HubModuleSheetData {
        HubModuleSheetData.make(raw: .meals(meals), isOrganizer: true, isReadOnly: false, pendingSync: false, locale: fr)
    }

    func testAStaleLoadNeverOverwritesTheLatestOne() async {
        let source = GatedSource()
        let vm = HubModuleSheetViewModel(module: .meals, eventId: "e", viewerId: "u", source: source)
        let first = Task { await vm.reload() }
        await waitUntil { source.pendingCount == 1 }
        let second = Task { await vm.reload() }
        await waitUntil { source.pendingCount == 2 }
        source.resume(1, with: .success(mealsData([raclette])))
        await second.value
        XCTAssertEqual(vm.data?.items.map(\.id), ["b"])
        // Le premier chargement répond en retard, puis en échec : ni ses données ni son échec ne s'affichent.
        source.resume(0, with: .success(mealsData([barbecue])))
        await first.value
        XCTAssertEqual(vm.data?.items.map(\.id), ["b"], "Chargement périmé ignoré.")
        XCTAssertEqual(vm.state, .loaded)
    }

    func testAStaleFailureIsIgnored() async {
        let source = GatedSource()
        let vm = HubModuleSheetViewModel(module: .meals, eventId: "e", viewerId: "u", source: source)
        let first = Task { await vm.reload() }
        await waitUntil { source.pendingCount == 1 }
        let second = Task { await vm.reload() }
        await waitUntil { source.pendingCount == 2 }
        source.resume(0, with: .failure(Stub.Boom()))
        await first.value
        XCTAssertEqual(vm.state, .loading, "L'échec d'un chargement périmé n'affiche pas d'erreur.")
        source.resume(1, with: .success(mealsData([raclette])))
        await second.value
        XCTAssertEqual(vm.state, .loaded)
    }

    func testCancellationKeepsTheCurrentState() async {
        let source = GatedSource()
        let vm = HubModuleSheetViewModel(module: .meals, eventId: "e", viewerId: "u", source: source)
        let cancelled = Task { await vm.reload() }
        await waitUntil { source.pendingCount == 1 }
        source.resume(0, with: .failure(CancellationError()))
        await cancelled.value
        XCTAssertEqual(vm.state, .loading, "Sheet fermée pendant le chargement : pas d'état d'échec.")
        XCTAssertNil(vm.data)

        let reload = Task { await vm.reload() }
        await waitUntil { source.pendingCount == 2 }
        source.resume(1, with: .success(mealsData([barbecue])))
        await reload.value
        let again = Task { await vm.reload() }
        await waitUntil { source.pendingCount == 3 }
        source.resume(2, with: .failure(CancellationError()))
        await again.value
        XCTAssertEqual(vm.state, .loaded)
        XCTAssertEqual(vm.data?.items.map(\.id), ["a"], "Les données affichées restent.")
    }

    func testMealAddedLocallySurvivesAReload() async {
        let vm = HubModuleSheetViewModel(module: .meals, eventId: "e", viewerId: "u", source: Stub(raw: .meals([barbecue])))
        await vm.reload()
        vm.addMeal(raclette)
        XCTAssertEqual(vm.data?.items.map(\.id), ["a", "b"])
        await vm.reload()
        XCTAssertEqual(vm.data?.items.map(\.id), ["a", "b"], "Le formulaire n'écrit pas en base : le repas reste affiché comme dans l'écran legacy.")
    }

    // MARK: - Rendu

    private func body(_ vm: HubModuleSheetViewModel) -> some View {
        HubModuleSheetBody(viewModel: vm, canAddHint: true, onClose: {}, onPrimary: { _ in }, onSecondary: { _ in })
    }

    func testLayer5bSheetsRenderLoadedEmptyAndFailedAtAX5WithinAPhone() async {
        let raws: [HubModuleSheetRaw] = [
            .budget(.init(totalEstimated: 1200, totalActual: 1500, categories: [
                .init(key: "transport", estimated: 400, actual: 700), .init(key: "other", estimated: 800, actual: 800)
            ])),
            .budget(nil),
            .payments(pot: .init(title: "Week-end", goalAmount: 400, currency: "EUR", statusName: "ACTIVE"), tricount: .linkToCheck),
            .payments(pot: nil, tricount: .undecided),
            .meetings([.init(id: "m", title: "Point logistique", startTime: "2026-10-03T18:00:00Z",
                             platformName: "GOOGLE_MEET", statusName: "SCHEDULED", hasLink: false)]),
            .meetings([])
        ]
        var models: [HubModuleSheetViewModel] = []
        for raw in raws {
            let vm = HubModuleSheetViewModel(module: raw.module, eventId: "e", viewerId: "u", source: Stub(raw: raw))
            await vm.reload()
            XCTAssertEqual(vm.state, .loaded, "\(raw.module)")
            models.append(vm)
        }
        for module in [HubModule.budget, .payments, .meetings] {
            let failed = HubModuleSheetViewModel(module: module, eventId: "e", viewerId: "u", source: Stub(raw: nil))
            await failed.reload()
            XCTAssertEqual(failed.state, .failed)
            models.append(failed)
        }
        for vm in models {
            let size = fittingSize(body(vm), width: 375, dynamicType: .accessibility5)
            XCTAssertLessThanOrEqual(size.width, 375, "\(vm.module) \(vm.state) \(size)")
            XCTAssertGreaterThan(size.height, 0, "\(vm.module) \(vm.state)")
        }
    }

    func testSheetRendersEveryStateAtAX5WithinAPhone() async {
        let loading = HubModuleSheetViewModel(module: .meals, eventId: "e", viewerId: "u", source: Stub(raw: nil))
        let failed = HubModuleSheetViewModel(module: .meals, eventId: "e", viewerId: "u", source: Stub(raw: nil))
        await failed.reload()
        let loaded = HubModuleSheetViewModel(module: .meals, eventId: "e", viewerId: "u", source: Stub(raw: .meals([barbecue, raclette])))
        await loaded.reload()
        let empty = HubModuleSheetViewModel(module: .activities, eventId: "e", viewerId: "u", source: Stub(raw: .activities([])))
        await empty.reload()
        XCTAssertEqual([loading.state, failed.state, loaded.state, empty.state], [.loading, .failed, .loaded, .loaded])
        for vm in [loading, failed, loaded, empty] {
            let size = fittingSize(body(vm), width: 375, dynamicType: .accessibility5)
            XCTAssertLessThanOrEqual(size.width, 375, "\(vm.state) \(size)")
            XCTAssertGreaterThan(size.height, 0, "\(vm.state)")
        }
    }

    func testItemCardsGrowWithTheirContent() async throws {
        let vm = HubModuleSheetViewModel(module: .meals, eventId: "e", viewerId: "u", source: Stub(raw: .meals([barbecue, raclette])))
        await vm.reload()
        let data = try XCTUnwrap(vm.data)
        let card = fittingSize(HubModuleSheetItemCard(item: data.items[0]), width: 375, dynamicType: .large)
        // Titre, détail et pastille/avatars : au moins trois lignes et une rangée d'avatars.
        XCTAssertGreaterThan(card.height, WK.Size.minTapTarget + WK.Size.avatar, "\(card)")
    }

    // MARK: - Contrat source

    private func source(_ path: String) throws -> String {
        try String(contentsOf: URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent().deletingLastPathComponent()
            .appendingPathComponent(path), encoding: .utf8)
    }

    func testSheetViewUsesWKComponentsAndTheExistingMealForm() throws {
        let view = try source("src/Views/Hub/Modules/HubModuleSheetView.swift")
        for anchor in ["WKModuleSheet(", "WKCard(style: .inset)", "WKStatusPill(", "WKAvatarStack(", "MealFormSheet(",
                       "arrow.up.left.and.arrow.down.right", "bubble.left", "hub.sheet.open_full", "hub.sheet.comments",
                       "hub.sheet.meals.add", "common.retry", ".task(id: eventId)",
                       "let mealParticipants: () -> [ParticipantModel]", "canAddHint: canAddHint",
                       "hub.sheet.budget.view_expenses", "hub.sheet.payments.manage_pot", "hub.sheet.meetings.plan",
                       "tricount.title"] {
            XCTAssertTrue(view.contains(anchor), anchor)
        }
        // Le conteneur garde ses enfants accessibles avant de recevoir son identifiant.
        XCTAssertTrue(squashed(view).contains(#".accessibilityElement(children: .contain) .wkAccessibilityID("hub.sheet.\(module.rawValue)")"#))
        XCTAssertTrue(view.contains(".accessibilityLabel(item.accessibilityLabel)"))
        XCTAssertTrue(view.contains("item.statusText"))
        XCTAssertFalse(view.contains("NavigationStack"), "Le formulaire legacy porte déjà sa pile de navigation.")
    }

    func testContentViewPresentsTheSheetAndFallsBackAfterDismissal() throws {
        let content = try source("src/Views/App/ContentView.swift")
        XCTAssertTrue(content.contains("@State private var hubSheet = HubSheetLifecycle()"))
        XCTAssertTrue(content.contains(".sheet(item: hubSheetBinding, onDismiss: finishHubModuleSheet)"))
        XCTAssertTrue(content.contains("hubModuleSheet(presented.module, for: event).id(event.id)"), "Sheet recréée pour un autre événement.")
        guard let route = content.range(of: "private func performHubRoute(_ route: EventHubRoute, for event: Event)") else {
            return XCTFail("performHubRoute")
        }
        let perform = String(content[route.lowerBound...].prefix(1600))
        XCTAssertTrue(perform.contains("repository.getEvent(id: event.id)"))
        XCTAssertTrue(perform.contains("case .sheet(let module):"))
        XCTAssertTrue(perform.contains("canAccessDetailedPlanning(for: event)"), "Même garde que le `case` legacy.")
        XCTAssertTrue(perform.contains("EventHubRouting.sheetGuard(for: module)"))
        XCTAssertTrue(perform.contains("canAccessOrganizationDashboard(for: event)"), "Garde des `case` budget, cagnotte, réunions.")
        XCTAssertTrue(perform.contains("hubSheet.present(module, eventId: event.id)"))
        guard let dismiss = content.range(of: "private func finishHubModuleSheet()") else { return XCTFail("finishHubModuleSheet") }
        let finish = String(content[dismiss.lowerBound...].prefix(600))
        // Le repli se fait après la fermeture (règles pures : `HubSheetLifecycle.didDismiss`).
        XCTAssertTrue(finish.contains("hubSheet.didDismiss(currentView: currentView, selectedEventId: selectedEvent?.id)"))
        XCTAssertTrue(finish.contains("case .show(let view): currentView = view"))
        XCTAssertTrue(finish.contains("case .reloadHub: eventHubReloadToken += 1"))
        XCTAssertTrue(finish.contains("case .presentRouter(let presentation): redesignRouter.presentation = presentation"))
        guard let builder = content.range(of: "private func hubModuleSheet(_ module: HubModule, for event: Event)") else {
            return XCTFail("hubModuleSheet")
        }
        let sheet = String(content[builder.lowerBound...].prefix(1800))
        XCTAssertTrue(sheet.contains("EventHubRouting.fullScreenFallback(for: module)"))
        XCTAssertTrue(sheet.contains("selectedCommentSection = section"))
        XCTAssertTrue(sheet.contains("hubSheet.requestFallback(.comments)"))
        XCTAssertTrue(sheet.contains("hubSheet.close()"))
        XCTAssertTrue(sheet.contains("mealParticipants: { participantModels(for: event) }"), "Mêmes participants, lus à l'ouverture du formulaire.")
        XCTAssertTrue(sheet.contains("canAddHint: event.organizerId == userId && !isFinalizedOrganizationState(event)"))
        XCTAssertTrue(sheet.contains("onOpenScreen: { view in hubSheet.requestFallback(view) }"), "Actions 5b : repli après fermeture.")
    }

    /// Espaces et retours à la ligne réduits à une espace : ancres indépendantes de l'indentation.
    private func squashed(_ text: String) -> String {
        text.split(whereSeparator: { $0.isWhitespace }).joined(separator: " ")
    }

    /// Toute navigation hors du hub ferme la sheet sans repli (revue I1–I3).
    func testNavigationAwayFromTheHubDismissesTheSheet() throws {
        let content = squashed(try source("src/Views/App/ContentView.swift"))
        guard let deepLink = content.range(of: "private func handleDeepLinkNavigation(_ route: IosRoute)") else {
            return XCTFail("handleDeepLinkNavigation")
        }
        let handler = String(content[deepLink.lowerBound...].prefix(600))
        let dismissCall = try XCTUnwrap(handler.range(of: "dismissHubModuleSheet()"))
        let preRoute = try XCTUnwrap(handler.range(of: "AppRouter.preRoute("))
        XCTAssertLessThan(dismissCall.lowerBound, preRoute.lowerBound, "La sheet se ferme avant le pré-aiguillage.")
        XCTAssertTrue(handler.contains("redesignRouter.presentation = hubSheet.routerPresentation(redesignRouter.presentation)"))
        XCTAssertTrue(content.contains(".onChange(of: iosRedesign2026) { _, _ in dismissHubModuleSheet() releaseHubSheetHost() }"))
        XCTAssertTrue(content.contains(".onChange(of: selectedEvent?.id) { _, id in hubSheet.selectedEventChanged(to: id) }"))
        XCTAssertTrue(content.contains(".onChange(of: currentView) { _, view in if view != .eventDetail { releaseHubSheetHost() } }"))
        XCTAssertTrue(content.contains(".onChange(of: redesignRouter.zone) { _, zone in dismissHubModuleSheet()"))
        XCTAssertTrue(content.contains("onBack: { dismissHubModuleSheet() invitationLandingEventId = nil currentView = .eventList }"))
        XCTAssertTrue(content.contains("private func dismissHubModuleSheet() { hubSheet.dismiss() }"))
    }

    /// « Plein écran » ouvre l'écran legacy de l'hébergement : son état vide ne doit pas afficher de clé brute.
    func testAccommodationFallbackEmptyTitleIsLocalizedInEveryLanguage() throws {
        let legacy = try source("src/Views/Events/EventSecondaryRouteViews.swift")
        XCTAssertTrue(legacy.contains("String(localized: \"accommodation.empty.title\")"))
        for locale in ["en", "fr", "es", "it", "pt"] {
            let strings = try source("src/Resources/\(locale).lproj/Localizable.strings")
            XCTAssertTrue(strings.contains("\"accommodation.empty.title\" ="), "accommodation.empty.title manquante (\(locale))")
        }
    }
}
