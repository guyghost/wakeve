import XCTest
import Shared
@testable import Wakeve

@MainActor
final class EventHubViewModelTests: XCTestCase {

    private struct StubSource: EventHubSource {
        var result: Result<EventHubFacts, Error>
        func loadFacts(eventId: String, viewerId: String, isLocalGuest: Bool) async throws -> EventHubFacts {
            try result.get()
        }
    }
    private struct Boom: Error {}

    /// Source dont chaque appel reste suspendu jusqu'à ce que le test le termine.
    @MainActor
    private final class ControlledSource: EventHubSource {
        var pending: [CheckedContinuation<EventHubFacts, Error>] = []
        func loadFacts(eventId: String, viewerId: String, isLocalGuest: Bool) async throws -> EventHubFacts {
            try await withCheckedThrowingContinuation { pending.append($0) }
        }
    }

    /// Source qui renvoie une suite de résultats, un par appel.
    @MainActor
    private final class SequenceSource: EventHubSource {
        var results: [Result<EventHubFacts, Error>]
        var calls: [(String, String, Bool)] = []
        init(_ results: [Result<EventHubFacts, Error>]) { self.results = results }
        func loadFacts(eventId: String, viewerId: String, isLocalGuest: Bool) async throws -> EventHubFacts {
            calls.append((eventId, viewerId, isLocalGuest))
            return try results.removeFirst().get()
        }
    }

    private func facts(title: String = "Week-end Annecy", phase: EventHubFacts.Phase = .polling) -> EventHubFacts {
        EventHubFacts(
            id: "e1", title: title, phase: phase, isOrganizer: false, viewerAccepted: true,
            hasDetailsAccess: false, isLocalGuest: false, pollOpen: true, userBallotComplete: false,
            ballotsKnown: true, votersWithCompleteBallot: 1, otherEligibleVoters: 2,
            otherVotersComplete: 1, slotCount: 2, leadingSlotStart: nil, finalDate: nil,
            confirmedCount: 2, pendingCount: 1, participantNames: ["Léa"], summaries: [.date: "2 créneaux"]
        )
    }

    private func viewModel(_ source: EventHubSource, guest: Bool = false) -> EventHubViewModel {
        EventHubViewModel(eventId: "e1", viewerId: "u", isLocalGuest: guest, source: source)
    }

    // MARK: - ViewModel

    func testStartsLoading() {
        let vm = viewModel(StubSource(result: .success(facts())))
        XCTAssertEqual(vm.state, .loading)
        XCTAssertNil(vm.facts)
        XCTAssertNil(vm.model)
    }

    func testLoadPublishesFactsAndModel() async {
        let source = SequenceSource([.success(facts())])
        let vm = viewModel(source, guest: true)
        await vm.reload()
        XCTAssertEqual(vm.state, .loaded)
        XCTAssertEqual(vm.facts, facts())
        XCTAssertEqual(vm.model, EventHubModel(facts: facts()))
        XCTAssertEqual(vm.model?.primary, .vote)
        XCTAssertEqual(source.calls.first?.0, "e1")
        XCTAssertEqual(source.calls.first?.1, "u")
        XCTAssertEqual(source.calls.first?.2, true)
    }

    func testFailureWithoutDataShowsFailed() async {
        let vm = viewModel(StubSource(result: .failure(Boom())))
        await vm.reload()
        XCTAssertEqual(vm.state, .failed)
        XCTAssertNil(vm.model)
    }

    func testFailureAfterALoadKeepsTheData() async {
        let vm = viewModel(SequenceSource([.success(facts()), .failure(Boom())]))
        await vm.reload()
        await vm.reload()
        XCTAssertEqual(vm.state, .loaded)
        XCTAssertEqual(vm.facts, facts())
        XCTAssertNotNil(vm.model)
    }

    func testCancelledLoadKeepsTheCurrentState() async {
        let vm = viewModel(StubSource(result: .failure(CancellationError())))
        await vm.reload()
        XCTAssertEqual(vm.state, .loading, "Une annulation n'est pas un échec de chargement.")
    }

    func testStaleReloadResultIsIgnored() async {
        let source = ControlledSource()
        let vm = viewModel(source)
        let first = Task { await vm.reload() }
        while source.pending.count < 1 { await Task.yield() }
        let second = Task { await vm.reload() }
        while source.pending.count < 2 { await Task.yield() }

        source.pending[1].resume(returning: facts(title: "fresh"))
        await second.value
        source.pending[0].resume(returning: facts(title: "stale"))
        await first.value

        XCTAssertEqual(vm.facts?.title, "fresh")
    }

    func testStaleFailureDoesNotOverrideFreshResult() async {
        let source = ControlledSource()
        let vm = viewModel(source)
        let first = Task { await vm.reload() }
        while source.pending.count < 1 { await Task.yield() }
        let second = Task { await vm.reload() }
        while source.pending.count < 2 { await Task.yield() }

        source.pending[1].resume(returning: facts(title: "fresh"))
        await second.value
        source.pending[0].resume(throwing: Boom())
        await first.value

        XCTAssertEqual(vm.state, .loaded)
        XCTAssertEqual(vm.facts?.title, "fresh")
    }

    // MARK: - Source : contrat

    private var srcRoot: URL {
        URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent()
            .appendingPathComponent("src")
    }

    func testHubFactsAreLoadedOffTheMainActorWithoutCreatingABudget() throws {
        let source = try String(contentsOf: srcRoot.appendingPathComponent("Services/SharedEventHubSource.swift"),
                                encoding: .utf8)
        XCTAssertFalse(source.contains("@MainActor"))
        XCTAssertTrue(source.contains("Task.detached"))
        XCTAssertTrue(source.contains("withTaskCancellationHandler"))
        XCTAssertFalse(source.contains("BudgetViewModel"), "`BudgetViewModel.load()` crée un budget : lire le dépôt.")
        XCTAssertTrue(source.contains("getBudgetByEventId"))
        XCTAssertTrue(source.contains("SharedEventsHomeSource.ballotStats("), "Réutiliser la règle des bulletins de l'accueil.")
    }

    func testAuthenticatedViewDelegatesDetailsAccessToTheSharedRule() throws {
        let content = try String(contentsOf: srcRoot.appendingPathComponent("Views/App/ContentView.swift"), encoding: .utf8)
        guard let start = content.range(of: "private func isParticipantConfirmed(for event: Event) -> Bool? {"),
              let end = content.range(of: "private func canAccessOrganizationDetails(for event: Event)", range: start.upperBound..<content.endIndex)
        else { return XCTFail("isParticipantConfirmed introuvable") }
        let body = content[start.upperBound..<end.lowerBound]
        XCTAssertTrue(body.contains("OrganizationDetailsAccess.isGranted("),
                      "Le hub et l'app partagent la même règle d'accès aux détails.")
    }

    // MARK: - Règles pures de la source

    private func record(_ userId: String, role: String = "PARTICIPANT", rsvp: String = "ACCEPTED",
                        validated: Bool = true) -> ParticipantRepositoryRecord {
        ParticipantRepositoryRecord(
            id: "r-\(userId)", eventId: "e1", userId: userId, role: role, rsvp: rsvp,
            hasValidatedDate: validated ? 1 : 0,
            dateValidation: validated ? "VALIDATED_RETAINED_DATE" : "NOT_VALIDATED"
        )
    }

    func testDetailsAccessMatchesTheExistingRule() {
        let records = [
            record("org", role: "ORGANIZER"),
            record("lea"),
            record("tom", validated: false),
            record("zoe", rsvp: "PENDING"),
            record("max", rsvp: "DECLINED")
        ]
        // Organisateur : toujours, même sans enregistrement.
        XCTAssertTrue(OrganizationDetailsAccess.isGranted(organizerId: "org", viewerId: "org", records: nil))
        XCTAssertTrue(OrganizationDetailsAccess.isGranted(organizerId: "org", viewerId: "org", records: []))
        // Accepté avec date retenue validée.
        XCTAssertTrue(OrganizationDetailsAccess.isGranted(organizerId: "org", viewerId: "lea", records: records))
        // Accepté sans validation, en attente, refusé, inconnu, dépôt illisible.
        XCTAssertFalse(OrganizationDetailsAccess.isGranted(organizerId: "org", viewerId: "tom", records: records))
        XCTAssertFalse(OrganizationDetailsAccess.isGranted(organizerId: "org", viewerId: "zoe", records: records))
        XCTAssertFalse(OrganizationDetailsAccess.isGranted(organizerId: "org", viewerId: "max", records: records))
        XCTAssertFalse(OrganizationDetailsAccess.isGranted(organizerId: "org", viewerId: "ghost", records: records))
        XCTAssertFalse(OrganizationDetailsAccess.isGranted(organizerId: "org", viewerId: "lea", records: nil))
        XCTAssertFalse(OrganizationDetailsAccess.isGranted(organizerId: "org", viewerId: "lea", records: []))
    }

    func testPhaseFromStatusName() {
        XCTAssertEqual(SharedEventHubSource.phase(statusName: "DRAFT"), .draft)
        XCTAssertEqual(SharedEventHubSource.phase(statusName: "POLLING"), .polling)
        XCTAssertEqual(SharedEventHubSource.phase(statusName: "COMPARING"), .comparing)
        XCTAssertEqual(SharedEventHubSource.phase(statusName: "CONFIRMED"), .confirmed)
        XCTAssertEqual(SharedEventHubSource.phase(statusName: "ORGANIZING"), .organizing)
        XCTAssertEqual(SharedEventHubSource.phase(statusName: "FINALIZED"), .finalized)
        XCTAssertEqual(SharedEventHubSource.phase(statusName: "WHATEVER"), .finalized)
    }

    func testGuestCountsIgnoreDeclinedAndSplitOnDetailsAccess() {
        let counts = SharedEventHubSource.guestCounts([
            (declined: false, confirmed: true),
            (declined: false, confirmed: true),
            (declined: false, confirmed: false),
            (declined: true, confirmed: false)
        ])
        XCTAssertEqual(counts.confirmed, 2)
        XCTAssertEqual(counts.pending, 1)
    }

    func testSummaryTextInFrench() {
        let fr = Locale(identifier: "fr_FR")
        XCTAssertEqual(HubSummaryText.slots(1, locale: fr), "1 créneau")
        XCTAssertEqual(HubSummaryText.slots(3, locale: fr), "3 créneaux")
        XCTAssertEqual(HubSummaryText.options(1, locale: fr), "1 option")
        XCTAssertEqual(HubSummaryText.options(4, locale: fr), "4 options")
        XCTAssertEqual(HubSummaryText.plural("hub.meetings_count", 2, locale: fr), "2 réunions")
        XCTAssertEqual(HubSummaryText.participants(confirmed: 3, pending: 0, locale: fr), "3 confirmés")
        XCTAssertEqual(HubSummaryText.participants(confirmed: 3, pending: 2, locale: fr), "3 confirmés · 2 en attente")
        XCTAssertEqual(HubSummaryText.meals(completed: 2, total: 5, locale: fr), "2/5 repas prêts")
        XCTAssertTrue(HubSummaryText.euros(1200, locale: fr).contains("€"))
    }

    func testParticipantAndScenarioSummariesArePluralized() {
        let fr = Locale(identifier: "fr_FR")
        let en = Locale(identifier: "en")
        XCTAssertEqual(HubSummaryText.participants(confirmed: 1, pending: 0, locale: fr), "1 confirmé")
        XCTAssertEqual(HubSummaryText.participants(confirmed: 1, pending: 1, locale: fr), "1 confirmé · 1 en attente")
        XCTAssertEqual(HubSummaryText.participants(confirmed: 3, pending: 3, locale: fr), "3 confirmés · 3 en attente")
        XCTAssertEqual(HubSummaryText.participants(confirmed: 1, pending: 1, locale: en), "1 confirmed · 1 pending")
        XCTAssertEqual(HubSummaryText.participants(confirmed: 3, pending: 3, locale: en), "3 confirmed · 3 pending")
        XCTAssertEqual(HubSummaryText.scenarios(1, locale: fr), "1 scénario à comparer")
        XCTAssertEqual(HubSummaryText.scenarios(3, locale: fr), "3 scénarios à comparer")
        XCTAssertEqual(HubSummaryText.scenarios(1, locale: en), "1 scenario to compare")
        XCTAssertEqual(HubSummaryText.scenarios(3, locale: en), "3 scenarios to compare")
    }

    private func sourceFacts(phase: EventHubFacts.Phase, isOrganizer: Bool) -> EventHubFacts {
        EventHubFacts(
            id: "e1", title: "t", phase: phase, isOrganizer: isOrganizer, viewerAccepted: true,
            hasDetailsAccess: true, isLocalGuest: false, pollOpen: true, userBallotComplete: false,
            ballotsKnown: true, votersWithCompleteBallot: 0, otherEligibleVoters: 0,
            otherVotersComplete: 0, slotCount: 0, leadingSlotStart: nil, finalDate: nil,
            confirmedCount: 0, pendingCount: 0, participantNames: [], summaries: [:]
        )
    }

    func testLocationSummaryMatchesTheScreenItOpens() {
        // La tuile Lieu ouvre la liste des scénarios : lieux potentiels (visibles de l'organisateur seul)
        // avant la date, scénarios ensuite.
        for phase in [EventHubFacts.Phase.draft, .polling] {
            XCTAssertEqual(SharedEventHubSource.locationSummary(for: sourceFacts(phase: phase, isOrganizer: true)), .potentialLocations)
            XCTAssertEqual(SharedEventHubSource.locationSummary(for: sourceFacts(phase: phase, isOrganizer: false)), .none)
        }
        for phase in [EventHubFacts.Phase.confirmed, .comparing, .organizing, .finalized] {
            for organizer in [true, false] {
                XCTAssertEqual(SharedEventHubSource.locationSummary(for: sourceFacts(phase: phase, isOrganizer: organizer)), .scenarios)
            }
        }
    }

    func testLeadingSlotIsIgnoredWithoutVotes() {
        XCTAssertNil(SharedEventHubSource.leadingStart(hasVotes: false, bestSlotStartISO: "2026-10-03T10:00:00Z"))
        XCTAssertEqual(SharedEventHubSource.leadingStart(hasVotes: true, bestSlotStartISO: "2026-10-03T10:00:00Z"),
                       ISO8601DateFormatter().date(from: "2026-10-03T10:00:00Z"))
        XCTAssertNil(SharedEventHubSource.leadingStart(hasVotes: true, bestSlotStartISO: nil))
    }
}
