import XCTest
import SwiftUI
import Shared
@testable import Wakeve

/// Sheets de résumé Transport et Invités (couche 5c, #47) : règles pures, routage et rendu.
@MainActor
final class HubModuleSheetLayer5cTests: XCTestCase {
    private let fr = Locale(identifier: "fr_FR")
    private let en = Locale(identifier: "en_US")

    private func make(
        _ raw: HubModuleSheetRaw,
        isOrganizer: Bool = true,
        isReadOnly: Bool = false,
        locale: Locale? = nil
    ) -> HubModuleSheetData {
        HubModuleSheetData.make(
            raw: raw, isOrganizer: isOrganizer, isReadOnly: isReadOnly, pendingSync: false, locale: locale ?? fr
        )
    }

    // MARK: - Transport : état et pastille

    private func plan(_ id: String, _ optimization: String = "BALANCED", cost: Double = 240, minutes: Int = 150) -> HubModuleSheetRaw.TransportPlan {
        .init(id: id, optimizationName: optimization, totalCost: cost, currency: "EUR", durationMinutes: minutes)
    }

    private func transport(
        _ plans: [HubModuleSheetRaw.TransportPlan] = [],
        selected: String? = nil,
        notNeeded: Bool = false,
        confirmed: Int = 0,
        missingDepartures: Int = 0
    ) -> HubModuleSheetRaw {
        .transport(.init(
            plans: plans, selectedPlanId: selected, notNeeded: notNeeded,
            confirmedCount: confirmed, missingDepartureCount: missingDepartures
        ))
    }

    func testTransportStateFollowsTheLegacyOrder() {
        typealias State = HubModuleSheetData.TransportState
        XCTAssertEqual(HubModuleSheetData.transportState(planIds: ["p1"], selectedPlanId: "p1", notNeeded: true), State.notNeeded)
        XCTAssertEqual(HubModuleSheetData.transportState(planIds: ["p1"], selectedPlanId: "p1", notNeeded: false), State.chosen)
        XCTAssertEqual(HubModuleSheetData.transportState(planIds: ["p1", "p2"], selectedPlanId: nil, notNeeded: false), State.toDecide)
        XCTAssertEqual(HubModuleSheetData.transportState(planIds: ["p1"], selectedPlanId: "gone", notNeeded: false), State.toDecide,
                       "Un plan retenu introuvable ne compte pas comme choisi.")
        XCTAssertEqual(HubModuleSheetData.transportState(planIds: [], selectedPlanId: nil, notNeeded: false), State.noPlan)
    }

    func testTransportPillSaysChosenToDecideOrNotNeeded() {
        XCTAssertEqual(HubModuleSheetData.transportPill(.chosen, locale: fr), .init(text: "Plan choisi", status: .confirmed))
        XCTAssertEqual(HubModuleSheetData.transportPill(.toDecide, locale: fr), .init(text: "À décider", status: .pending))
        XCTAssertEqual(HubModuleSheetData.transportPill(.notNeeded, locale: fr), .init(text: "Pas nécessaire", status: .draft))
        XCTAssertNil(HubModuleSheetData.transportPill(.noPlan, locale: fr))
    }

    /// La tuile du hub et la pastille de la sheet partagent `transportState` et `transportPill`.
    func testHubTileSummaryUsesTheSameRuleAsThePill() {
        XCTAssertEqual(HubModuleSheetData.transportSummary(.chosen, planCount: 1, locale: fr), "Plan choisi")
        XCTAssertEqual(HubModuleSheetData.transportSummary(.notNeeded, planCount: 0, locale: fr), "Pas nécessaire")
        XCTAssertEqual(HubModuleSheetData.transportSummary(.toDecide, planCount: 2, locale: fr), HubSummaryText.options(2, locale: fr))
        XCTAssertNil(HubModuleSheetData.transportSummary(.noPlan, planCount: 0, locale: fr))
    }

    func testTransportHubSourceReadsTheSharedRule() throws {
        let source = try String(contentsOf: URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent().deletingLastPathComponent()
            .appendingPathComponent("src/Services/SharedEventHubSource.swift"), encoding: .utf8)
        XCTAssertTrue(source.contains("HubModuleSheetData.transportSummary("))
        XCTAssertTrue(source.contains("HubModuleSheetData.transportState("))
    }

    // MARK: - Transport : contenu

    func testNoPlanSaysSoAndShowsDepartures() {
        let data = make(transport(confirmed: 3, missingDepartures: 2))
        XCTAssertNil(data.status)
        XCTAssertEqual(data.missing, "Aucun plan de transport pour l'instant.")
        XCTAssertEqual(data.items.map(\.id), ["departures"])
        XCTAssertEqual(data.items.first?.title, "Départs participants")
        XCTAssertEqual(data.items.first?.detail, "2 participants doivent préciser leur départ.")
        XCTAssertEqual(data.items.first?.status, .pending)
    }

    func testProposedPlansAreListedUntilOneIsChosen() throws {
        let data = make(transport([plan("p1", "COST_MINIMIZE", cost: 180, minutes: 95), plan("p2", "TIME_MINIMIZE")],
                                  confirmed: 2))
        XCTAssertEqual(data.status, .init(text: "À décider", status: .pending))
        XCTAssertEqual(data.missing, "Aucun plan retenu pour l'instant.")
        XCTAssertEqual(data.items.map(\.id), ["p1", "p2", "departures"])
        XCTAssertEqual(data.items[0].title, "Coût")
        let duration = try XCTUnwrap(HubModuleSheetData.transportDuration(minutes: 95, locale: fr))
        // Espaces insécables possibles selon la version d'iOS.
        let plain = duration.replacingOccurrences(of: "\u{00A0}", with: " ").replacingOccurrences(of: "\u{202F}", with: " ")
        XCTAssertTrue(plain.hasPrefix("1 h") && plain.hasSuffix("35 min"), duration)
        XCTAssertEqual(data.items[0].detail, "\(HubSummaryText.currency(180, code: "EUR", locale: fr)) · \(duration)")
        XCTAssertNil(data.items[0].status)
        XCTAssertEqual(data.items[2].detail, "Tous les participants confirmés ont un point de départ.")
        XCTAssertEqual(data.items[2].status, .confirmed)
    }

    func testChosenPlanIsTheOnlyCard() {
        let data = make(transport([plan("p1"), plan("p2", "COST_MINIMIZE")], selected: "p2", confirmed: 1))
        XCTAssertEqual(data.status, .init(text: "Plan choisi", status: .confirmed))
        XCTAssertNil(data.missing)
        XCTAssertEqual(data.items.map(\.id), ["p2"])
        XCTAssertEqual(data.items[0].status, .confirmed)
        XCTAssertEqual(data.items[0].statusText, "Plan choisi")
    }

    func testTransportNotNeededHasNoCards() {
        let data = make(transport([plan("p1")], notNeeded: true, confirmed: 2, missingDepartures: 2))
        XCTAssertEqual(data.status, .init(text: "Pas nécessaire", status: .draft))
        XCTAssertEqual(data.missing, "Transport non requis pour cet événement.")
        XCTAssertTrue(data.items.isEmpty)
    }

    func testUnknownDurationIsOmitted() {
        let data = make(transport([plan("p1", cost: 0, minutes: 0)]))
        XCTAssertEqual(data.items.first?.detail, HubSummaryText.currency(0, code: "EUR", locale: fr))
    }

    func testOrganizeTransportIsOfferedToEveryoneWithAccess() {
        for (organizer, readOnly) in [(true, false), (false, false), (true, true), (false, true)] {
            XCTAssertEqual(make(transport(), isOrganizer: organizer, isReadOnly: readOnly).primary, .organizeTransport)
        }
        XCTAssertEqual(HubModuleSheetView.title(for: .organizeTransport, locale: fr), "Organiser le transport")
        XCTAssertEqual(EventHubRouting.fallback(for: .organizeTransport), .transportPlanning)
        XCTAssertEqual(HubModuleSheetView.primaryTitle(module: .transport, state: .loading, data: nil, canAddHint: false, locale: fr),
                       "Organiser le transport")
    }

    /// Départs comptés sur les mêmes participants que l'écran legacy (organisateur compris).
    func testDeparturesUseTheLegacyConfirmedParticipants() {
        func record(_ user: String, role: String, rsvp: String, validated: Int64) -> ParticipantRepositoryRecord {
            ParticipantRepositoryRecord(id: "r-\(user)", eventId: "e", userId: user, role: role, rsvp: rsvp,
                                        hasValidatedDate: validated, dateValidation: nil)
        }
        let records = [
            record("org", role: "ORGANIZER", rsvp: "ACCEPTED", validated: 0),
            record("lea", role: "PARTICIPANT", rsvp: "ACCEPTED", validated: 1),
            record("tom", role: "PARTICIPANT", rsvp: "PENDING", validated: 0)
        ]
        XCTAssertEqual(Set(SharedEventModuleSheetSource.confirmedParticipantIds(records: records, fallback: ["x"])), ["org", "lea"])
        XCTAssertEqual(SharedEventModuleSheetSource.confirmedParticipantIds(records: nil, fallback: ["a", "b"]), ["a", "b"])
        XCTAssertEqual(SharedEventModuleSheetSource.confirmedParticipantIds(records: [], fallback: ["a"]), ["a"])
        XCTAssertEqual(SharedEventModuleSheetSource.longestRouteMinutes([40, 95, 60]), 95)
        XCTAssertEqual(SharedEventModuleSheetSource.longestRouteMinutes([]), 0)
    }

    // MARK: - Transport : routage

    func testTransportOpensInASheetGuardedLikeItsLegacyCase() {
        XCTAssertTrue(EventHubRouting.sheetModules.contains(.transport))
        for rollout in [true, false] {
            XCTAssertEqual(EventHubRouting.route(for: .transport, phase: .organizing, invitationRollout: rollout), .sheet(.transport))
        }
        XCTAssertEqual(EventHubRouting.sheetGuard(for: .transport), .transportPlanning)
        XCTAssertEqual(EventHubRouting.fullScreenFallback(for: .transport), .transportPlanning)
        XCTAssertEqual(EventHubRouting.sheetRoute(for: .transport, accessGranted: false), .screen(.transportPlanning))
        XCTAssertEqual(EventHubRouting.commentSection(for: .transport), .transport)
        XCTAssertEqual(HubModuleSheetView.secondaryActions(for: .transport), [.comments], "Pas de « Plein écran » en double.")
        XCTAssertTrue(SharedEventModuleSheetSource.readsDatabase(for: .transport))
    }

    // MARK: - Rendu

    private struct Stub: EventModuleSheetSource {
        struct Boom: Error {}
        var raw: HubModuleSheetRaw?
        var isOrganizer = true
        func load(module: HubModule, eventId: String, viewerId: String) async throws -> HubModuleSheetData {
            guard let raw else { throw Boom() }
            return HubModuleSheetData.make(raw: raw, isOrganizer: isOrganizer, isReadOnly: false, pendingSync: false)
        }
    }

    private func body(_ vm: HubModuleSheetViewModel) -> some View {
        HubModuleSheetBody(viewModel: vm, canAddHint: true, onClose: {}, onPrimary: { _ in }, onSecondary: { _ in })
    }

    /// Rend chaque état (chargé, vide, échec, chargement) à AX5 dans la largeur d'un téléphone.
    func renderAtAX5(_ raws: [HubModuleSheetRaw], failing modules: [HubModule]) async {
        var models: [HubModuleSheetViewModel] = []
        for raw in raws {
            let vm = HubModuleSheetViewModel(module: raw.module, eventId: "e", viewerId: "u", source: Stub(raw: raw))
            await vm.reload()
            XCTAssertEqual(vm.state, .loaded, "\(raw.module)")
            models.append(vm)
        }
        for module in modules {
            let failed = HubModuleSheetViewModel(module: module, eventId: "e", viewerId: "u", source: Stub(raw: nil))
            await failed.reload()
            XCTAssertEqual(failed.state, .failed)
            models.append(failed)
            models.append(HubModuleSheetViewModel(module: module, eventId: "e", viewerId: "u", source: Stub(raw: nil)))
        }
        for vm in models {
            let size = fittingSize(body(vm), width: 375, dynamicType: .accessibility5)
            XCTAssertLessThanOrEqual(size.width, 375, "\(vm.module) \(vm.state) \(size)")
            XCTAssertGreaterThan(size.height, 0, "\(vm.module) \(vm.state)")
        }
    }

    func testTransportSheetRendersEveryStateAtAX5WithinAPhone() async {
        await renderAtAX5([
            transport([plan("p1", "COST_MINIMIZE"), plan("p2")], confirmed: 3, missingDepartures: 1),
            transport([plan("p1")], selected: "p1"),
            transport(notNeeded: true),
            transport()
        ], failing: [.transport])
    }

    // MARK: - Localisation

    func testLayer5cKeysExistInEveryLanguage() throws {
        let root = URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent()
        for locale in ["en", "fr", "es", "it", "pt"] {
            let strings = try String(contentsOf: root.appendingPathComponent("src/Resources/\(locale).lproj/Localizable.strings"), encoding: .utf8)
            for key in Self.stringKeys {
                XCTAssertTrue(strings.contains("\"\(key)\" ="), "\(key) manquante (\(locale))")
            }
            let dict = try String(contentsOf: root.appendingPathComponent("src/Resources/\(locale).lproj/Localizable.stringsdict"), encoding: .utf8)
            for key in Self.pluralKeys {
                XCTAssertTrue(dict.contains("<key>\(key)</key>"), "pluriel \(key) manquant (\(locale))")
            }
        }
    }

    static var stringKeys = [
        "hub.sheet.transport.no_plan", "hub.sheet.transport.choose", "hub.sheet.transport.organize",
        "hub.sheet.status.transport_chosen", "hub.sheet.status.transport_to_decide", "hub.sheet.status.transport_not_needed"
    ]
    static var pluralKeys: [String] = []
}
