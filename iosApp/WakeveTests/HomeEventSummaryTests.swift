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
        readOnly: Bool = false,
        pollOpen: Bool = true,
        viewerAccepted: Bool = true,
        userBallotComplete: Bool = false,
        votersWithCompleteBallot: Int = 5,
        eligibleVoters: Int = 8,
        otherVotersComplete: Int = 4,
        otherEligibleVoters: Int = 7,
        deadline: Date? = nil,
        eventDate: Date? = nil
    ) -> HomeEventFacts {
        HomeEventFacts(
            id: id, title: title, phase: phase, role: role, isOwner: role == .organizer,
            isPast: isPast, readOnly: readOnly, pollOpen: pollOpen, viewerAccepted: viewerAccepted,
            ballots: HomeBallotStats(
                userBallotComplete: userBallotComplete,
                votersWithCompleteBallot: votersWithCompleteBallot, eligibleVoters: eligibleVoters,
                otherVotersComplete: otherVotersComplete, otherEligibleVoters: otherEligibleVoters
            ),
            deadline: deadline, eventDate: eventDate, participantNames: ["Léa", "Tom"]
        )
    }

    func testParticipantWhoHasNotVotedMustAct() {
        let s = HomeEventSummary(facts: facts())
        XCTAssertEqual(s.status, .actionNeeded)
        XCTAssertEqual(s.label, .key("home.v2.status.vote_required"))
        XCTAssertEqual(s.sortRank, 0)
    }

    func testParticipantWhoVotedWaits() {
        let s = HomeEventSummary(facts: facts(userBallotComplete: true))
        XCTAssertEqual(s.status, .pending)
        XCTAssertEqual(s.label, .key("home.v2.status.polling"))
        XCTAssertEqual(s.sortRank, 1)
    }

    func testOrganizerIsToldWhenEveryoneVoted() {
        let s = HomeEventSummary(
            facts: facts(role: .organizer, userBallotComplete: true, votersWithCompleteBallot: 8, eligibleVoters: 8,
                         otherVotersComplete: 7, otherEligibleVoters: 7)
        )
        XCTAssertEqual(s.status, .actionNeeded)
        XCTAssertEqual(s.label, .key("home.v2.status.ready_to_confirm"))
    }

    func testDraftIsNeutralAndLast() {
        let s = HomeEventSummary(facts: facts(phase: .draft, role: .organizer))
        XCTAssertEqual(s.status, .draft)
        XCTAssertEqual(s.sortRank, 3)
    }

    func testConfirmedUpcomingShowsItsDate() {
        let date = ISO8601DateFormatter().date(from: "2026-10-12T18:00:00Z")!
        let s = HomeEventSummary(facts: facts(phase: .confirmed, role: .participant, eventDate: date))
        XCTAssertEqual(s.status, .confirmed)
        XCTAssertEqual(s.label, .date(date))
        XCTAssertEqual(s.sortRank, 2)
    }

    func testOrganizerOrganizingIsPending() {
        let s = HomeEventSummary(facts: facts(phase: .organizing, role: .organizer))
        XCTAssertEqual(s.status, .pending)
        XCTAssertEqual(s.label, .key("home.v2.status.organizing"))
    }

    func testPastEventsGoToThePastSection() {
        let s = HomeEventSummary(facts: facts(phase: .finalized, isPast: true))
        XCTAssertTrue(s.isPast)
        XCTAssertEqual(s.sortRank, 4)
    }

    func testSortPutsActionFirstThenSoonestDeadline() {
        let soon = now.addingTimeInterval(86_400)
        let later = now.addingTimeInterval(5 * 86_400)
        let items = [
            HomeEventSummary(facts: facts(id: "draft", phase: .draft, role: .organizer)),
            HomeEventSummary(facts: facts(id: "voteLater", deadline: later)),
            HomeEventSummary(facts: facts(id: "waiting", userBallotComplete: true)),
            HomeEventSummary(facts: facts(id: "voteSoon", deadline: soon))
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
        XCTAssertEqual(step?.metric, .votes(complete: 5, eligible: 8))
        XCTAssertEqual(step?.value, "5")
        XCTAssertEqual(step?.unit, "/8")
        XCTAssertEqual(step?.daysLeft, 2)
    }

    func testNextStepForOrganizerWhenEveryoneVoted() {
        let step = HomeNextStep.pick(
            from: [facts(role: .organizer, userBallotComplete: true, votersWithCompleteBallot: 8, eligibleVoters: 8,
                         otherVotersComplete: 7, otherEligibleVoters: 7)],
            now: now
        )
        XCTAssertEqual(step?.action, .pollResults)
        XCTAssertEqual(step?.kind, .readyToConfirm)
    }

    func testNoNextStepWhenNothingToDo() {
        XCTAssertNil(HomeNextStep.pick(from: [facts(userBallotComplete: true)], now: now))
        XCTAssertNil(HomeNextStep.pick(from: [facts(isPast: true)], now: now))
    }

    // MARK: - Qui doit agir (revue couche 3)

    func testClosedPollIsPendingNotAnAction() {
        let s = HomeEventSummary(facts: facts(pollOpen: false))
        XCTAssertFalse(s.facts.voteRequired)
        XCTAssertEqual(s.status, .pending)
        XCTAssertEqual(s.label, .key("home.v2.status.polling"))
        XCTAssertNil(HomeNextStep.pick(from: [facts(pollOpen: false)], now: now))
    }

    func testDeclinedOrPendingInviteeIsNotAskedToVote() {
        let s = HomeEventSummary(facts: facts(viewerAccepted: false))
        XCTAssertFalse(s.facts.voteRequired)
        XCTAssertEqual(s.status, .pending)
        XCTAssertNil(HomeNextStep.pick(from: [facts(viewerAccepted: false)], now: now))
    }

    func testReadOnlyEventNeverAsksForAnAction() {
        XCTAssertFalse(facts(readOnly: true).voteRequired)
        XCTAssertFalse(facts(role: .organizer, readOnly: true, otherVotersComplete: 7, otherEligibleVoters: 7).readyToConfirm)
        XCTAssertNotEqual(HomeEventSummary(facts: facts(readOnly: true)).status, .actionNeeded)
    }

    func testOrganizerAloneIsNeverReadyButMustStillVote() {
        let alone = facts(role: .organizer, userBallotComplete: false, votersWithCompleteBallot: 0, eligibleVoters: 1,
                          otherVotersComplete: 0, otherEligibleVoters: 0)
        XCTAssertFalse(alone.readyToConfirm)
        XCTAssertTrue(alone.voteRequired)
        let voted = facts(role: .organizer, userBallotComplete: true, votersWithCompleteBallot: 1, eligibleVoters: 1,
                          otherVotersComplete: 0, otherEligibleVoters: 0)
        XCTAssertFalse(voted.readyToConfirm)
        XCTAssertEqual(HomeNextStep.pick(from: [voted], now: now)?.kind, .pollInProgress)
    }

    func testOrganizerWhoDidNotVoteIsReadyWhenOthersAreComplete() {
        let f = facts(role: .organizer, userBallotComplete: false, votersWithCompleteBallot: 3, eligibleVoters: 4,
                      otherVotersComplete: 3, otherEligibleVoters: 3)
        XCTAssertTrue(f.readyToConfirm)
        XCTAssertFalse(f.voteRequired, "« Prêt à confirmer » l'emporte sur le vote de l'organisateur.")
        let s = HomeEventSummary(facts: f)
        XCTAssertEqual(s.label, .key("home.v2.status.ready_to_confirm"))
        let step = HomeNextStep.pick(from: [f], now: now)
        XCTAssertEqual(step?.kind, .readyToConfirm)
        XCTAssertEqual(step?.metric, .votes(complete: 3, eligible: 3))
    }

    func testOrganizerNotReadyWhileAnotherVoterIsIncomplete() {
        let f = facts(role: .organizer, userBallotComplete: true, otherVotersComplete: 2, otherEligibleVoters: 3)
        XCTAssertFalse(f.readyToConfirm)
        XCTAssertEqual(HomeEventSummary(facts: f).status, .pending)
    }

    func testClosedPollHeroHasNoDeadlineCountdown() {
        let past = now.addingTimeInterval(-86_400)
        let f = facts(role: .organizer, pollOpen: false, userBallotComplete: true, deadline: past)
        let step = HomeNextStep.pick(from: [f], now: now)
        XCTAssertEqual(step?.kind, .pollInProgress)
        XCTAssertNil(step?.daysLeft)
    }

    func testOnlyTheOwnerCanDeleteAndNeverAFinalizedEvent() {
        XCTAssertTrue(HomeEventSummary(facts: facts(phase: .draft, role: .organizer)).canDelete)
        XCTAssertTrue(HomeEventSummary(facts: facts(phase: .confirmed, role: .organizer)).canDelete)
        XCTAssertFalse(HomeEventSummary(facts: facts(phase: .finalized, role: .organizer)).canDelete)
        XCTAssertFalse(HomeEventSummary(facts: facts(phase: .polling, role: .participant)).canDelete)
    }

    func testOrganizingHeroCountsDaysWithoutALiteralUnit() {
        let date = now.addingTimeInterval(5 * 86_400)
        let step = HomeNextStep.pick(from: [facts(phase: .confirmed, role: .organizer, eventDate: date)], now: now)
        XCTAssertEqual(step?.kind, .organizing)
        XCTAssertEqual(step?.metric, .days(5))
        XCTAssertEqual(step?.value, "5")
        XCTAssertNil(step?.unit, "L'unité des jours est localisée par la vue.")
    }
}
