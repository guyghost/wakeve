import XCTest
@testable import Wakeve

final class HomeEventSummaryTests: XCTestCase {

    private let now = ISO8601DateFormatter().date(from: "2026-10-01T10:00:00Z")!

    private func facts(
        id: String = "e1",
        title: String = "Week-end Annecy",
        phase: HomeEventFacts.Phase = .polling,
        role: HomeEventFacts.Role = .participant,
        isPast: Bool = false,
        userBallotComplete: Bool = false,
        votersWithCompleteBallot: Int = 5,
        eligibleVoters: Int = 8,
        deadline: Date? = nil,
        eventDate: Date? = nil
    ) -> HomeEventFacts {
        HomeEventFacts(
            id: id, title: title, phase: phase, role: role, isPast: isPast,
            userBallotComplete: userBallotComplete,
            votersWithCompleteBallot: votersWithCompleteBallot, eligibleVoters: eligibleVoters,
            deadline: deadline, eventDate: eventDate, participantNames: ["Léa", "Tom"]
        )
    }

    func testParticipantWhoHasNotVotedMustAct() {
        let s = HomeEventSummary(facts: facts(), now: now)
        XCTAssertEqual(s.status, .actionNeeded)
        XCTAssertEqual(s.label, .key("home.v2.status.vote_required"))
        XCTAssertEqual(s.sortRank, 0)
    }

    func testParticipantWhoVotedWaits() {
        let s = HomeEventSummary(facts: facts(userBallotComplete: true), now: now)
        XCTAssertEqual(s.status, .pending)
        XCTAssertEqual(s.label, .key("home.v2.status.polling"))
        XCTAssertEqual(s.sortRank, 1)
    }

    func testOrganizerIsToldWhenEveryoneVoted() {
        let s = HomeEventSummary(
            facts: facts(role: .organizer, userBallotComplete: true, votersWithCompleteBallot: 8, eligibleVoters: 8),
            now: now
        )
        XCTAssertEqual(s.status, .actionNeeded)
        XCTAssertEqual(s.label, .key("home.v2.status.ready_to_confirm"))
    }

    func testDraftIsNeutralAndLast() {
        let s = HomeEventSummary(facts: facts(phase: .draft, role: .organizer), now: now)
        XCTAssertEqual(s.status, .draft)
        XCTAssertEqual(s.sortRank, 3)
    }

    func testConfirmedUpcomingShowsItsDate() {
        let date = ISO8601DateFormatter().date(from: "2026-10-12T18:00:00Z")!
        let s = HomeEventSummary(facts: facts(phase: .confirmed, role: .participant, eventDate: date), now: now)
        XCTAssertEqual(s.status, .confirmed)
        XCTAssertEqual(s.label, .date(date))
        XCTAssertEqual(s.sortRank, 2)
    }

    func testOrganizerOrganizingIsPending() {
        let s = HomeEventSummary(facts: facts(phase: .organizing, role: .organizer), now: now)
        XCTAssertEqual(s.status, .pending)
        XCTAssertEqual(s.label, .key("home.v2.status.organizing"))
    }

    func testPastEventsGoToThePastSection() {
        let s = HomeEventSummary(facts: facts(phase: .finalized, isPast: true), now: now)
        XCTAssertTrue(s.isPast)
        XCTAssertEqual(s.sortRank, 4)
    }

    func testSortPutsActionFirstThenSoonestDeadline() {
        let soon = now.addingTimeInterval(86_400)
        let later = now.addingTimeInterval(5 * 86_400)
        let items = [
            HomeEventSummary(facts: facts(id: "draft", phase: .draft, role: .organizer), now: now),
            HomeEventSummary(facts: facts(id: "voteLater", deadline: later), now: now),
            HomeEventSummary(facts: facts(id: "waiting", userBallotComplete: true), now: now),
            HomeEventSummary(facts: facts(id: "voteSoon", deadline: soon), now: now)
        ]
        XCTAssertEqual(HomeEventSummary.sorted(items).map(\.id), ["voteSoon", "voteLater", "waiting", "draft"])
    }

    func testNextStepPrefersTheMostUrgentVote() {
        let soon = now.addingTimeInterval(2 * 86_400)
        let items = [
            facts(id: "org", role: .organizer, userBallotComplete: true),
            facts(id: "vote", title: "Raclette", deadline: soon)
        ]
        let step = HomeNextStep.pick(from: items, now: now)
        XCTAssertEqual(step?.eventId, "vote")
        XCTAssertEqual(step?.action, .vote)
        XCTAssertEqual(step?.value, "5")
        XCTAssertEqual(step?.unit, "/8")
        XCTAssertEqual(step?.daysLeft, 2)
    }

    func testNextStepForOrganizerWhenEveryoneVoted() {
        let step = HomeNextStep.pick(
            from: [facts(role: .organizer, userBallotComplete: true, votersWithCompleteBallot: 8, eligibleVoters: 8)],
            now: now
        )
        XCTAssertEqual(step?.action, .pollResults)
        XCTAssertEqual(step?.kind, .readyToConfirm)
    }

    func testNoNextStepWhenNothingToDo() {
        XCTAssertNil(HomeNextStep.pick(from: [facts(userBallotComplete: true)], now: now))
        XCTAssertNil(HomeNextStep.pick(from: [facts(isPast: true)], now: now))
    }
}
