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

    /// L'écran transport s'ouvre pour tous ceux qui ont accès ; seul l'organisateur d'un événement modifiable
    /// l'« organise », les autres le consultent.
    func testTransportIsOrganizedByTheOrganizerAndViewedByOthers() {
        XCTAssertEqual(make(transport(), isOrganizer: true, isReadOnly: false).primary, .organizeTransport)
        for (organizer, readOnly) in [(false, false), (true, true), (false, true)] {
            XCTAssertEqual(make(transport(), isOrganizer: organizer, isReadOnly: readOnly).primary, .viewTransport)
        }
        XCTAssertEqual(HubModuleSheetView.title(for: .organizeTransport, locale: fr), "Organiser le transport")
        XCTAssertEqual(HubModuleSheetView.title(for: .viewTransport, locale: fr), "Voir le transport")
        XCTAssertEqual(HubModuleSheetView.title(for: .viewTransport, locale: en), "View transport")
        XCTAssertEqual(EventHubRouting.fallback(for: .organizeTransport), .transportPlanning)
        XCTAssertEqual(EventHubRouting.fallback(for: .viewTransport), .transportPlanning)
        XCTAssertEqual(HubModuleSheetView.primaryTitle(module: .transport, state: .loading, data: nil, canAddHint: false, locale: fr),
                       "Voir le transport")
        XCTAssertEqual(HubModuleSheetView.primaryTitle(module: .transport, state: .loading, data: nil, canAddHint: true, locale: fr),
                       "Organiser le transport")
        XCTAssertEqual(HubModuleSheetView.secondaryActions(for: .transport, primary: .viewTransport), [.comments])
    }

    /// Même file que l'écran legacy (`TransportPlanningViewModel.hasReplayablePendingSync`).
    func testTransportSheetReadsTheTransportPendingSync() {
        XCTAssertTrue(SharedEventModuleSheetSource.readsTransportPendingSync(for: .transport))
        XCTAssertFalse(SharedEventModuleSheetSource.readsTransportPendingSync(for: .meals))
        XCTAssertTrue(SharedEventModuleSheetSource.hasPendingSync(
            module: .transport, workflowPending: false, phase5Pending: false, transportPending: true))
        XCTAssertFalse(SharedEventModuleSheetSource.hasPendingSync(
            module: .meals, workflowPending: false, phase5Pending: false, transportPending: true))
        XCTAssertFalse(SharedEventModuleSheetSource.hasPendingSync(
            module: .transport, workflowPending: false, phase5Pending: true, transportPending: false))
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

    // MARK: - Invités : contenu

    private func guest(_ id: String, _ group: HubModuleSheetRaw.GuestGroup, organizer: Bool = false) -> HubModuleSheetRaw.Guest {
        .init(id: id, name: "Nom \(id)", group: group, isOrganizer: organizer)
    }

    func testGuestGroupMatchesTheHubCount() {
        XCTAssertEqual(HubModuleSheetData.guestGroup(declined: true, confirmed: true), .declined)
        XCTAssertEqual(HubModuleSheetData.guestGroup(declined: false, confirmed: true), .confirmed)
        XCTAssertEqual(HubModuleSheetData.guestGroup(declined: false, confirmed: false), .pending)
    }

    func testGuestsAreGroupedConfirmedPendingDeclinedWithTheOrganizerFirst() {
        let data = make(.participants([
            guest("p", .pending), guest("c", .confirmed), guest("d", .declined), guest("o", .confirmed, organizer: true),
            guest("p2", .pending)
        ]))
        XCTAssertEqual(data.items.map(\.id), ["o", "c", "p", "p2", "d"])
        XCTAssertEqual(data.items.map(\.sectionTitle), ["Confirmés", "Confirmés", "En attente", "En attente", "Ont décliné"])
        XCTAssertEqual(data.items[0].title, "Nom o")
        XCTAssertEqual(data.items[0].statusText, "Organisateur")
        XCTAssertNil(data.items[0].status, "Rôle affiché en texte, sans pastille.")
        XCTAssertEqual(data.items[0].assigneeNames, ["Nom o"], "Avatar de la personne.")
        XCTAssertNil(data.items[1].statusText)
        XCTAssertEqual(data.items[0].accessibilityLabel, "Nom o, Confirmés, Organisateur")
        XCTAssertEqual(data.items[2].accessibilityLabel, "Nom p, En attente", "VoiceOver annonce le groupe de chaque invité.")
        XCTAssertEqual(data.items[4].accessibilityLabel, "Nom d, Ont décliné")
        XCTAssertEqual(data.status, .init(text: "2 confirmés", status: .pending))
        XCTAssertEqual(data.missing, "2 invités en attente")
    }

    /// Sans enregistrement : l'organisateur est confirmé (il a toujours accès), les autres en attente ;
    /// la tuile et la sheet lisent la même liste (`SharedEventHubSource.guestEntries`).
    func testGuestsWithoutRecordsMatchBetweenTileAndSheet() {
        for records in [nil, [ParticipantRepositoryRecord]()] {
            let entries = SharedEventHubSource.guestEntries(records: records, participantIds: ["o", "a", "b"], organizerId: "o")
            XCTAssertEqual(entries, [
                .init(id: "o", declined: false, confirmed: true, isOrganizer: true),
                .init(id: "a", declined: false, confirmed: false, isOrganizer: false),
                .init(id: "b", declined: false, confirmed: false, isOrganizer: false)
            ])
            let tile = SharedEventHubSource.guestCounts(entries)
            XCTAssertEqual(tile.confirmed, 1)
            XCTAssertEqual(tile.pending, 2)
            let sheet = make(.participants(SharedEventModuleSheetSource.rawGuests(entries) { "Nom \($0)" }))
            XCTAssertEqual(sheet.status, .init(text: "1 confirmé", status: .pending))
            XCTAssertEqual(sheet.missing, "2 invités en attente")
            XCTAssertEqual(sheet.items.map(\.id), ["o", "a", "b"])
        }
    }

    func testGuestEntriesFromRecordsKeepTheLegacyRule() {
        func record(_ user: String, role: String, rsvp: String, validated: Int64) -> ParticipantRepositoryRecord {
            ParticipantRepositoryRecord(id: "r-\(user)", eventId: "e", userId: user, role: role, rsvp: rsvp,
                                        hasValidatedDate: validated, dateValidation: nil)
        }
        let entries = SharedEventHubSource.guestEntries(records: [
            record("org", role: "ORGANIZER", rsvp: "ACCEPTED", validated: 0),
            record("lea", role: "PARTICIPANT", rsvp: "ACCEPTED", validated: 1),
            record("tom", role: "PARTICIPANT", rsvp: "PENDING", validated: 0),
            record("max", role: "PARTICIPANT", rsvp: "DECLINED", validated: 0)
        ], participantIds: ["ignored"], organizerId: "org")
        XCTAssertEqual(entries.map(\.id), ["org", "lea", "tom", "max"])
        XCTAssertEqual(entries.map(\.confirmed), [true, true, false, false])
        XCTAssertEqual(entries.map(\.declined), [false, false, false, true])
        XCTAssertEqual(entries.map(\.isOrganizer), [true, false, false, false])
        let tile = SharedEventHubSource.guestCounts(entries)
        XCTAssertEqual(tile.confirmed, 2)
        XCTAssertEqual(tile.pending, 1)
    }

    func testEveryoneConfirmedHasAConfirmedPillAndNoMissingSentence() {
        let data = make(.participants([guest("o", .confirmed, organizer: true), guest("c", .confirmed), guest("d", .declined)]))
        XCTAssertEqual(data.status, .init(text: "2 confirmés", status: .confirmed))
        XCTAssertNil(data.missing)
        XCTAssertEqual(make(.participants([guest("c", .confirmed), guest("p", .pending)])).missing, "1 invité en attente")
    }

    func testNobodyInvitedSaysSo() {
        XCTAssertEqual(make(.participants([])).missing, "Personne n'est invité pour l'instant.")
        XCTAssertNil(make(.participants([])).status)
        let alone = make(.participants([guest("o", .confirmed, organizer: true)]))
        XCTAssertEqual(alone.missing, "Personne n'est invité pour l'instant.", "L'organisateur seul n'a invité personne.")
        XCTAssertEqual(alone.items.map(\.id), ["o"])
    }

    func testOnlyTheOrganizerOfAnOpenEventInvites() {
        XCTAssertEqual(make(.participants([]), isOrganizer: true, isReadOnly: false).primary, .invite)
        XCTAssertNil(make(.participants([]), isOrganizer: false, isReadOnly: false).primary)
        XCTAssertNil(make(.participants([]), isOrganizer: true, isReadOnly: true).primary)
        XCTAssertEqual(HubModuleSheetView.title(for: .invite, locale: fr), "Inviter")
        XCTAssertNil(EventHubRouting.fallback(for: .invite), "« Inviter » suit la route d'ajout, sensible au flag invitations.")
    }

    // MARK: - Invités : actions et routage

    func testParticipantsOfferFullScreenOnlyWithoutInvite() {
        XCTAssertEqual(HubModuleSheetView.secondaryActions(for: .participants, primary: nil), [.fullScreen])
        XCTAssertEqual(HubModuleSheetView.secondaryActions(for: .participants, primary: .invite), [],
                       "« Inviter » ouvre déjà l'écran plein.")
        XCTAssertEqual(HubModuleSheetView.secondaryActions(for: .meals, primary: .addMeal), [.fullScreen, .comments])
        XCTAssertEqual(HubModuleSheetView.secondaryActions(for: .transport, primary: .organizeTransport), [.comments])
    }

    func testParticipantsOpenInASheetWithoutGuardAndFallBackByTheInvitationFlag() {
        XCTAssertTrue(EventHubRouting.sheetModules.contains(.participants))
        for rollout in [true, false] {
            for phase in [EventHubFacts.Phase.draft, .polling, .confirmed, .organizing, .finalized] {
                XCTAssertEqual(EventHubRouting.route(for: .participants, phase: phase, invitationRollout: rollout), .sheet(.participants))
            }
        }
        XCTAssertEqual(EventHubRouting.sheetGuard(for: .participants), .unguarded)
        XCTAssertEqual(EventHubRouting.fullScreenRoute(for: .participants, invitationRollout: true), .invitationParticipants)
        XCTAssertEqual(EventHubRouting.fullScreenRoute(for: .participants, invitationRollout: false), .screen(.participantManagement))
        XCTAssertEqual(EventHubRouting.fullScreenRoute(for: .meals, invitationRollout: true), .screen(.mealPlanning))
        XCTAssertNil(EventHubRouting.fullScreenRoute(for: .date, invitationRollout: true))
        XCTAssertTrue(SharedEventModuleSheetSource.readsDatabase(for: .participants))
    }

    func testOrganizerSeesInviteFromTheFirstFrame() {
        XCTAssertEqual(HubModuleSheetView.primaryTitle(module: .participants, state: .loading, data: nil, canAddHint: true, locale: fr), "Inviter")
        XCTAssertNil(HubModuleSheetView.primaryTitle(module: .participants, state: .loading, data: nil, canAddHint: false, locale: fr))
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

    func testParticipantsSheetRendersEveryStateAtAX5WithinAPhone() async {
        await renderAtAX5([
            .participants([guest("o", .confirmed, organizer: true), guest("c", .confirmed), guest("p", .pending), guest("d", .declined)]),
            .participants([])
        ], failing: [.participants])
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
        "hub.sheet.transport.no_plan", "hub.sheet.transport.choose", "hub.sheet.transport.organize", "hub.sheet.transport.view",
        "hub.sheet.status.transport_chosen", "hub.sheet.status.transport_to_decide", "hub.sheet.status.transport_not_needed",
        "hub.sheet.participants.invite", "hub.sheet.participants.empty", "hub.sheet.participants.section.confirmed",
        "hub.sheet.participants.section.pending", "hub.sheet.participants.section.declined", "participants.role.organizer"
    ]
    static var pluralKeys = ["hub.sheet.participants.pending_count", "hub.confirmed_count"]

    func testEnglishGuestPluralsResolve() {
        XCTAssertEqual(String(format: WK.localizedFormat("hub.sheet.participants.pending_count", locale: en), locale: en, 1),
                       "1 guest pending")
        XCTAssertEqual(String(format: WK.localizedFormat("hub.sheet.participants.pending_count", locale: en), locale: en, 3),
                       "3 guests pending")
    }
}
