import XCTest
@testable import Wakeve

@MainActor
final class EventsHomeViewModelTests: XCTestCase {

    private struct StubSource: EventsHomeSource {
        var result: Result<[HomeRawEvent], Error>
        func loadEvents(viewerId: String) async throws -> [HomeRawEvent] { try result.get() }
    }
    private struct Boom: Error {}

    /// Source dont chaque appel reste suspendu jusqu'à ce que le test le termine.
    @MainActor
    private final class ControlledSource: EventsHomeSource {
        var pending: [CheckedContinuation<[HomeRawEvent], Error>] = []
        func loadEvents(viewerId: String) async throws -> [HomeRawEvent] {
            try await withCheckedThrowingContinuation { pending.append($0) }
        }
    }

    private func raw(_ id: String, status: String = "POLLING", organizer: Bool = false, past: Bool = false,
                     voted: Bool = false, pending: Bool = false, accepted: Bool = true,
                     deadline: String = "2026-10-05T10:00:00Z", finalDate: String? = nil,
                     earliestSlot: String? = nil) -> HomeRawEvent {
        HomeRawEvent(id: id, title: id, statusName: status, isOrganizer: organizer, isOwner: organizer,
                     isPast: past, readOnly: false, viewerAccepted: accepted || organizer,
                     deadlineISO: deadline, finalDateISO: finalDate, earliestSlotStartISO: earliestSlot,
                     ballots: HomeBallotStats(userBallotComplete: voted, votersWithCompleteBallot: 3, eligibleVoters: 6,
                                              otherVotersComplete: 2, otherEligibleVoters: 5),
                     participantNames: ["Léa"], hasPendingSync: pending)
    }

    private let fixedNow = ISO8601DateFormatter().date(from: "2026-10-01T10:00:00Z")!

    func testEmpty() async {
        let vm = EventsHomeViewModel(viewerId: "u", source: StubSource(result: .success([])), now: { self.fixedNow })
        await vm.reload()
        XCTAssertEqual(vm.state, .empty)
        XCTAssertNil(vm.nextStep)
    }

    func testSplitsSortsAndPicksNextStep() async {
        let vm = EventsHomeViewModel(viewerId: "u", source: StubSource(result: .success([
            raw("waiting", voted: true), raw("mine"), raw("old", status: "FINALIZED", past: true),
            raw("draft", status: "DRAFT", organizer: true, pending: true)
        ])), now: { self.fixedNow })
        await vm.reload()
        XCTAssertEqual(vm.state, .loaded)
        XCTAssertEqual(vm.active.map(\.id), ["mine", "waiting", "draft"])
        XCTAssertEqual(vm.past.map(\.id), ["old"])
        XCTAssertEqual(vm.nextStep?.eventId, "mine")
        XCTAssertEqual(vm.pendingSyncCount, 1)
    }

    func testFailureWithoutDataShowsFailed() async {
        let vm = EventsHomeViewModel(viewerId: "u", source: StubSource(result: .failure(Boom())), now: { self.fixedNow })
        await vm.reload()
        XCTAssertEqual(vm.state, .failed)
    }

    func testUnknownStatusIsTreatedAsFinalized() {
        XCTAssertEqual(EventsHomeViewModel.facts(from: raw("x", status: "WHATEVER"), now: fixedNow).phase, .finalized)
    }

    // MARK: - Règles de la source et des faits (revue couche 3)

    func testPastDeadlinePollIsNotOpen() {
        let closed = EventsHomeViewModel.facts(from: raw("p", deadline: "2026-09-30T10:00:00Z"), now: fixedNow)
        XCTAssertFalse(closed.pollOpen)
        XCTAssertFalse(closed.voteRequired)
        XCTAssertTrue(EventsHomeViewModel.facts(from: raw("p"), now: fixedNow).pollOpen)
        XCTAssertTrue(EventsHomeViewModel.facts(from: raw("p", deadline: ""), now: fixedNow).pollOpen,
                      "Sans échéance, le sondage reste ouvert.")
    }

    func testPastDeadlinePollStaysActiveButWithoutHeroVote() async {
        let vm = EventsHomeViewModel(viewerId: "u", source: StubSource(result: .success([
            raw("closed", deadline: "2026-09-30T10:00:00Z")
        ])), now: { self.fixedNow })
        await vm.reload()
        XCTAssertEqual(vm.active.first?.status, .pending)
        XCTAssertNil(vm.nextStep)
    }

    func testInviteeWhoDidNotAcceptIsNotAskedToVote() {
        XCTAssertFalse(EventsHomeViewModel.facts(from: raw("p", accepted: false), now: fixedNow).voteRequired)
    }

    func testConfirmedEventDateUsesFinalDateOnly() {
        let confirmed = EventsHomeViewModel.facts(
            from: raw("c", status: "CONFIRMED", earliestSlot: "2026-10-03T10:00:00Z"), now: fixedNow
        )
        XCTAssertNil(confirmed.eventDate)
        let dated = EventsHomeViewModel.facts(
            from: raw("c", status: "ORGANIZING", finalDate: "2026-10-09T10:00:00Z", earliestSlot: "2026-10-03T10:00:00Z"),
            now: fixedNow
        )
        XCTAssertEqual(dated.eventDate, ISO8601DateFormatter().date(from: "2026-10-09T10:00:00Z"))
        let poll = EventsHomeViewModel.facts(from: raw("p", earliestSlot: "2026-10-03T10:00:00Z"), now: fixedNow)
        XCTAssertEqual(poll.eventDate, ISO8601DateFormatter().date(from: "2026-10-03T10:00:00Z"))
    }

    func testEarliestSlotStartIsTheMinimum() {
        XCTAssertEqual(
            SharedEventsHomeSource.earliestStartISO(["2026-10-09T10:00:00Z", "2026-10-03T10:00:00Z", "", "bad"]),
            "2026-10-03T10:00:00Z"
        )
        XCTAssertNil(SharedEventsHomeSource.earliestStartISO([]))
    }

    func testPastClassKeepsOnlyDraftsAndUnboundedPollsActive() {
        XCTAssertTrue(SharedEventsHomeSource.keepsActive(statusName: "POLLING", isTemporallyPast: false, hasStructuredEndBound: true))
        XCTAssertTrue(SharedEventsHomeSource.keepsActive(statusName: "DRAFT", isTemporallyPast: true, hasStructuredEndBound: true))
        XCTAssertTrue(SharedEventsHomeSource.keepsActive(statusName: "POLLING", isTemporallyPast: true, hasStructuredEndBound: false))
        XCTAssertFalse(SharedEventsHomeSource.keepsActive(statusName: "POLLING", isTemporallyPast: true, hasStructuredEndBound: true),
                       "Un sondage dont tous les créneaux sont passés va dans Passés.")
        XCTAssertFalse(SharedEventsHomeSource.keepsActive(statusName: "CONFIRMED", isTemporallyPast: true, hasStructuredEndBound: false))
    }

    func testBallotStatsCountOnlyCompleteBallotsFromAcceptedVotersAndOrganizer() {
        let slots: Set<String> = ["s1", "s2"]
        let stats = SharedEventsHomeSource.ballotStats(
            slotIds: slots,
            ballots: [
                "org": ["s1", "s2"],
                "lea": ["s1", "s2"],
                "tom": ["s1"],
                "declined": ["s1", "s2"]
            ],
            organizerId: "org",
            acceptedParticipantIds: ["lea", "tom", "zoe"],
            viewerId: "tom"
        )
        XCTAssertEqual(stats, HomeBallotStats(
            userBallotComplete: false,
            votersWithCompleteBallot: 2, eligibleVoters: 4,
            otherVotersComplete: 1, otherEligibleVoters: 3
        ))
    }

    func testBallotStatsWithoutSlotsHasNoCompleteBallot() {
        let stats = SharedEventsHomeSource.ballotStats(
            slotIds: [], ballots: ["org": []], organizerId: "org", acceptedParticipantIds: [], viewerId: "org"
        )
        XCTAssertFalse(stats.userBallotComplete)
        XCTAssertEqual(stats.eligibleVoters, 1)
        XCTAssertEqual(stats.otherEligibleVoters, 0)
    }

    func testStaleReloadResultIsIgnored() async {
        let source = ControlledSource()
        let vm = EventsHomeViewModel(viewerId: "u", source: source, now: { self.fixedNow })
        let first = Task { await vm.reload() }
        while source.pending.count < 1 { await Task.yield() }
        let second = Task { await vm.reload() }
        while source.pending.count < 2 { await Task.yield() }

        source.pending[1].resume(returning: [raw("fresh")])
        await second.value
        source.pending[0].resume(returning: [raw("stale")])
        await first.value

        XCTAssertEqual(vm.active.map(\.id), ["fresh"])
    }

    func testStaleFailureDoesNotOverrideFreshResult() async {
        let source = ControlledSource()
        let vm = EventsHomeViewModel(viewerId: "u", source: source, now: { self.fixedNow })
        let first = Task { await vm.reload() }
        while source.pending.count < 1 { await Task.yield() }
        let second = Task { await vm.reload() }
        while source.pending.count < 2 { await Task.yield() }

        source.pending[1].resume(returning: [])
        await second.value
        source.pending[0].resume(throwing: Boom())
        await first.value

        XCTAssertEqual(vm.state, .empty)
    }
}
