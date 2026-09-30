import XCTest
import SwiftUI
@testable import Wakeve

/// Sheets de modules du hub (couche 5a, #47) : routage, repli plein écran, modèle de vue, rendu.
@MainActor
final class HubModuleSheetViewTests: XCTestCase {
    private let fr = Locale(identifier: "fr")

    // MARK: - Routage

    func testSimpleModulesOpenInSheetsAndOthersKeepTheirScreens() {
        XCTAssertEqual(EventHubRouting.sheetModules, [.meals, .equipment, .activities, .accommodation, .photos])
        for rollout in [true, false] {
            for phase in [EventHubFacts.Phase.organizing, .finalized, .confirmed] {
                for module in EventHubRouting.sheetModules {
                    XCTAssertEqual(EventHubRouting.route(for: module, phase: phase, invitationRollout: rollout), .sheet(module), "\(module) \(phase)")
                }
                XCTAssertEqual(EventHubRouting.route(for: .budget, phase: phase, invitationRollout: rollout), .screen(.budgetOverview))
                XCTAssertEqual(EventHubRouting.route(for: .transport, phase: phase, invitationRollout: rollout), .screen(.transportPlanning))
                XCTAssertEqual(EventHubRouting.route(for: .meetings, phase: phase, invitationRollout: rollout), .screen(.meetingList))
                XCTAssertEqual(EventHubRouting.route(for: .payments, phase: phase, invitationRollout: rollout), .screen(.paymentPot))
            }
        }
    }

    func testFullScreenFallbackIsTheLegacyScreenOfEachSheetModule() {
        let expected: [HubModule: AppView] = [
            .meals: .mealPlanning, .equipment: .equipmentChecklist, .activities: .activityPlanning,
            .accommodation: .accommodation, .photos: .eventPhotos
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
        HubModuleSheetBody(viewModel: vm, onClose: {}, onAdd: {}, onOpenFullScreen: {}, onOpenComments: {})
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
                       "hub.sheet.meals.add", "common.retry", ".task"] {
            XCTAssertTrue(view.contains(anchor), anchor)
        }
        XCTAssertFalse(view.contains("NavigationStack"), "Le formulaire legacy porte déjà sa pile de navigation.")
    }

    func testContentViewPresentsTheSheetAndFallsBackAfterDismissal() throws {
        let content = try source("src/Views/App/ContentView.swift")
        XCTAssertTrue(content.contains("@State private var presentedHubModule: HubModule?"))
        XCTAssertTrue(content.contains("@State private var pendingHubFallback: AppView?"))
        XCTAssertTrue(content.contains(".sheet(item: $presentedHubModule"))
        guard let route = content.range(of: "private func performHubRoute(_ route: EventHubRoute, for event: Event)") else {
            return XCTFail("performHubRoute")
        }
        let perform = String(content[route.lowerBound...].prefix(1600))
        XCTAssertTrue(perform.contains("repository.getEvent(id: event.id)"))
        XCTAssertTrue(perform.contains("case .sheet(let module):"))
        XCTAssertTrue(perform.contains("canAccessDetailedPlanning(for: event)"), "Même garde que le `case` legacy.")
        XCTAssertTrue(perform.contains("presentedHubModule = module"))
        guard let dismiss = content.range(of: "private func finishHubModuleSheet()") else { return XCTFail("finishHubModuleSheet") }
        let finish = String(content[dismiss.lowerBound...].prefix(600))
        // Le repli se fait après la fermeture, jamais pendant la présentation ; sinon le hub se recharge.
        XCTAssertTrue(finish.contains("pendingHubFallback = nil"))
        XCTAssertTrue(finish.contains("currentView = view"))
        XCTAssertTrue(finish.contains("eventHubReloadToken += 1"))
        guard let builder = content.range(of: "private func hubModuleSheet(_ module: HubModule, for event: Event)") else {
            return XCTFail("hubModuleSheet")
        }
        let sheet = String(content[builder.lowerBound...].prefix(1600))
        XCTAssertTrue(sheet.contains("EventHubRouting.fullScreenFallback(for: module)"))
        XCTAssertTrue(sheet.contains("selectedCommentSection = section"))
        XCTAssertTrue(sheet.contains("pendingHubFallback = .comments"))
        XCTAssertTrue(sheet.contains("presentedHubModule = nil"))
        XCTAssertTrue(sheet.contains("participantModels(for: event)"), "Mêmes participants que le formulaire legacy.")
    }
}
