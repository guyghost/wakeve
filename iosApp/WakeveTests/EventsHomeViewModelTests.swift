import XCTest
@testable import Wakeve

@MainActor
final class EventsHomeViewModelTests: XCTestCase {

    private struct StubSource: EventsHomeSource {
        var result: Result<[HomeRawEvent], Error>
        func loadEvents(viewerId: String) async throws -> [HomeRawEvent] { try result.get() }
    }
    private struct Boom: Error {}

    private func raw(_ id: String, status: String = "POLLING", organizer: Bool = false, past: Bool = false,
                     voted: Bool = false, pending: Bool = false) -> HomeRawEvent {
        HomeRawEvent(id: id, title: id, statusName: status, isOrganizer: organizer, isPast: past,
                     deadlineISO: "2026-10-05T10:00:00Z", finalDateISO: nil, firstSlotStartISO: nil,
                     userBallotComplete: voted, votersWithCompleteBallot: 3, eligibleVoters: 6,
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
        XCTAssertEqual(EventsHomeViewModel.facts(from: raw("x", status: "WHATEVER")).phase, .finalized)
    }
}
