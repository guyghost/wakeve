import XCTest
@testable import Wakeve

/// Règles pures du hub d'événement (couche 4, #47).
final class EventHubModelTests: XCTestCase {

    private func facts(
        phase: EventHubFacts.Phase = .polling,
        isOrganizer: Bool = false,
        viewerAccepted: Bool = true,
        hasDetailsAccess: Bool = true,
        isLocalGuest: Bool = false,
        pollOpen: Bool = true,
        userBallotComplete: Bool = false,
        ballotsKnown: Bool = true,
        votersWithCompleteBallot: Int = 2,
        eligibleVoters: Int = 5,
        otherEligibleVoters: Int = 4,
        otherVotersComplete: Int = 1,
        slotCount: Int = 3
    ) -> EventHubFacts {
        EventHubFacts(
            id: "e1", title: "Week-end Annecy", phase: phase,
            isOrganizer: isOrganizer, viewerAccepted: viewerAccepted || isOrganizer,
            hasDetailsAccess: hasDetailsAccess || isOrganizer, isLocalGuest: isLocalGuest,
            pollOpen: pollOpen, userBallotComplete: userBallotComplete, ballotsKnown: ballotsKnown,
            votersWithCompleteBallot: votersWithCompleteBallot, eligibleVoters: eligibleVoters,
            otherEligibleVoters: otherEligibleVoters, otherVotersComplete: otherVotersComplete,
            slotCount: slotCount, leadingSlotStart: nil, finalDate: nil,
            confirmedCount: 3, pendingCount: 1, participantNames: ["Léa", "Tom"], summaries: [:]
        )
    }

    private func tile(_ module: HubModule, in model: EventHubModel) -> EventHubModel.Tile? {
        model.tiles.first { $0.module == module }
    }

    // MARK: - Modules

    func testModulesPerPhase() {
        XCTAssertEqual(EventHubModel.modules(for: .draft), [.date, .location, .participants])
        XCTAssertEqual(EventHubModel.modules(for: .polling), [.date, .location, .participants, .budget])
        XCTAssertEqual(EventHubModel.modules(for: .confirmed), [.date, .scenarios, .participants, .budget])
        XCTAssertEqual(EventHubModel.modules(for: .comparing), [.date, .scenarios, .participants, .budget])
        XCTAssertEqual(EventHubModel.modules(for: .organizing),
                       [.transport, .accommodation, .meals, .equipment, .activities, .budget, .meetings])
        XCTAssertEqual(EventHubModel.modules(for: .finalized), [.recap, .photos, .payments])
    }

    func testTilesFollowThePhaseModules() {
        let model = EventHubModel(facts: facts(phase: .organizing, isOrganizer: true))
        XCTAssertEqual(model.tiles.map(\.module), EventHubModel.modules(for: .organizing))
    }

    // MARK: - Sondage

    func testAcceptedParticipantWhoHasNotVotedIsAskedToVote() {
        let model = EventHubModel(facts: facts())
        XCTAssertEqual(model.primary, .vote)
        XCTAssertEqual(model.status, .actionNeeded)
        XCTAssertEqual(model.statusKey, "home.v2.status.vote_required")
        XCTAssertEqual(tile(.date, in: model)?.isHighlighted, true)
        XCTAssertEqual(tile(.date, in: model)?.status, .actionNeeded)
        XCTAssertTrue(model.showsQuickVote)
    }

    func testQuickVoteNeedsAtLeastOneSlot() {
        let model = EventHubModel(facts: facts(slotCount: 0))
        XCTAssertEqual(model.primary, .vote)
        XCTAssertFalse(model.showsQuickVote)
    }

    func testParticipantWhoDidNotAcceptIsNotAskedToVote() {
        let model = EventHubModel(facts: facts(viewerAccepted: false, hasDetailsAccess: false))
        XCTAssertNotEqual(model.primary, .vote)
        XCTAssertEqual(model.primary, .none)
        XCTAssertFalse(model.showsQuickVote)
        XCTAssertEqual(model.status, .pending)
        XCTAssertEqual(model.statusKey, "home.v2.status.polling")
        XCTAssertNil(tile(.date, in: model)?.status)
    }

    func testClosedOrUnreadablePollDoesNotAskToVote() {
        XCTAssertNotEqual(EventHubModel(facts: facts(pollOpen: false)).primary, .vote)
        XCTAssertNotEqual(EventHubModel(facts: facts(ballotsKnown: false)).primary, .vote)
    }

    func testParticipantWhoVotedSeesResults() {
        let model = EventHubModel(facts: facts(userBallotComplete: true))
        XCTAssertEqual(model.primary, .pollResults)
        XCTAssertEqual(model.status, .pending)
        XCTAssertEqual(tile(.date, in: model)?.isHighlighted, true)
        XCTAssertNil(tile(.date, in: model)?.status)
        XCTAssertFalse(model.showsQuickVote)
    }

    func testOrganizerIsReadyToConfirmWhenAllOthersVoted() {
        let model = EventHubModel(facts: facts(
            isOrganizer: true, userBallotComplete: false,
            votersWithCompleteBallot: 4, eligibleVoters: 5, otherEligibleVoters: 4, otherVotersComplete: 4
        ))
        XCTAssertEqual(model.primary, .confirmDate, "« Prêt à confirmer » l'emporte sur le vote (règle de la couche 3).")
        XCTAssertEqual(model.status, .actionNeeded)
        XCTAssertEqual(model.statusKey, "home.v2.status.ready_to_confirm")
        XCTAssertEqual(tile(.date, in: model)?.status, .actionNeeded)
        XCTAssertEqual(tile(.date, in: model)?.isHighlighted, true)
    }

    func testOrganizerAfterDeadlineCanConfirmWithOneBallot() {
        let model = EventHubModel(facts: facts(
            isOrganizer: true, pollOpen: false, userBallotComplete: true,
            votersWithCompleteBallot: 1, otherVotersComplete: 0
        ))
        XCTAssertEqual(model.primary, .confirmDate)
    }

    func testLoneOrganizerIsNotReadyToConfirm() {
        let model = EventHubModel(facts: facts(
            isOrganizer: true, userBallotComplete: true,
            votersWithCompleteBallot: 1, eligibleVoters: 1, otherEligibleVoters: 0, otherVotersComplete: 0
        ))
        XCTAssertNotEqual(model.primary, .confirmDate)
        XCTAssertEqual(model.primary, .pollResults)
        XCTAssertEqual(model.statusKey, "home.v2.status.polling")
    }

    func testOrganizerWhoHasNotVotedIsAskedToVoteUntilReady() {
        let model = EventHubModel(facts: facts(isOrganizer: true))
        XCTAssertEqual(model.primary, .vote)
    }

    // MARK: - Brouillon

    func testDraftWithoutSlotsAsksToAddDates() {
        let model = EventHubModel(facts: facts(phase: .draft, isOrganizer: true, slotCount: 0))
        XCTAssertEqual(model.primary, .addDates)
        XCTAssertEqual(model.status, .draft)
        XCTAssertEqual(model.statusKey, "home.v2.status.draft")
        XCTAssertEqual(tile(.date, in: model)?.isHighlighted, true)
    }

    func testDraftWithSlotsHasNoPrimary() {
        let model = EventHubModel(facts: facts(phase: .draft, isOrganizer: true, slotCount: 2))
        XCTAssertEqual(model.primary, .none)
        XCTAssertTrue(model.tiles.allSatisfy { !$0.isHighlighted })
    }

    // MARK: - Cycle de vie

    func testConfirmedOrganizerIsOfferedToOrganize() {
        let model = EventHubModel(facts: facts(phase: .confirmed, isOrganizer: true))
        XCTAssertEqual(model.primary, .organize)
        XCTAssertEqual(model.status, .pending)
        XCTAssertEqual(model.statusKey, "home.v2.status.organizing")
    }

    func testConfirmedParticipantHasNoPrimary() {
        let model = EventHubModel(facts: facts(phase: .confirmed))
        XCTAssertEqual(model.primary, .none)
        XCTAssertEqual(model.status, .confirmed)
        XCTAssertEqual(model.statusKey, "home.v2.status.confirmed")
    }

    func testOrganizingOrganizerFinalizes() {
        XCTAssertEqual(EventHubModel(facts: facts(phase: .organizing, isOrganizer: true)).primary, .finalize)
    }

    func testOrganizingLocalGuestIsAskedToSignIn() {
        XCTAssertEqual(
            EventHubModel(facts: facts(phase: .organizing, isOrganizer: true, isLocalGuest: true)).primary,
            .signInToFinalize
        )
    }

    func testComparingHighlightsScenarios() {
        let model = EventHubModel(facts: facts(phase: .comparing, isOrganizer: true))
        XCTAssertEqual(model.primary, .none)
        XCTAssertEqual(tile(.scenarios, in: model)?.isHighlighted, true)
        XCTAssertEqual(model.tiles.filter(\.isHighlighted).count, 1)
    }

    func testFinalizedShowsRecapPhotosPayments() {
        let model = EventHubModel(facts: facts(phase: .finalized, isOrganizer: true))
        XCTAssertEqual(model.tiles.map(\.module), [.recap, .photos, .payments])
        XCTAssertEqual(model.primary, .none)
        XCTAssertEqual(model.status, .confirmed)
        XCTAssertEqual(model.statusKey, "home.v2.status.confirmed")
        XCTAssertTrue(model.tiles.allSatisfy { !$0.isLocked })
    }

    // MARK: - Tuiles verrouillées

    func testConfirmedParticipantWithoutAccessHasOrganizationTilesLocked() {
        let model = EventHubModel(facts: facts(phase: .organizing, hasDetailsAccess: false))
        for module in [HubModule.transport, .accommodation, .meals, .equipment, .activities, .budget, .meetings] {
            XCTAssertEqual(tile(module, in: model)?.isLocked, true, "\(module) devrait être verrouillé")
        }
    }

    func testOrganizingParticipantWithAccessHasTilesUnlocked() {
        let model = EventHubModel(facts: facts(phase: .organizing, hasDetailsAccess: true))
        XCTAssertTrue(model.tiles.allSatisfy { !$0.isLocked })
    }

    func testTransportIsLockedBeforeConfirmation() {
        XCTAssertTrue(EventHubModel.isLocked(.transport, facts: facts(phase: .polling, isOrganizer: true)))
        XCTAssertTrue(EventHubModel.isLocked(.transport, facts: facts(phase: .comparing, isOrganizer: true)))
        XCTAssertFalse(EventHubModel.isLocked(.transport, facts: facts(phase: .confirmed, isOrganizer: true)))
    }

    func testBudgetIsAnEstimateForOrganizerAndAcceptedDuringPollAndConfirmed() {
        XCTAssertFalse(tile(.budget, in: EventHubModel(facts: facts(phase: .polling, hasDetailsAccess: false)))!.isLocked)
        XCTAssertFalse(tile(.budget, in: EventHubModel(facts: facts(phase: .confirmed, hasDetailsAccess: false)))!.isLocked)
        XCTAssertTrue(tile(.budget, in: EventHubModel(facts: facts(
            phase: .polling, viewerAccepted: false, hasDetailsAccess: false
        )))!.isLocked)
        XCTAssertFalse(tile(.budget, in: EventHubModel(facts: facts(phase: .polling, isOrganizer: true)))!.isLocked)
    }

    func testMeetingsAndPaymentsNeedOrganizationPhaseAndAccess() {
        XCTAssertTrue(EventHubModel.isLocked(.meetings, facts: facts(phase: .confirmed, isOrganizer: true)))
        XCTAssertFalse(EventHubModel.isLocked(.meetings, facts: facts(phase: .organizing, isOrganizer: true)))
        XCTAssertTrue(EventHubModel.isLocked(.payments, facts: facts(phase: .finalized, hasDetailsAccess: false)))
        XCTAssertFalse(EventHubModel.isLocked(.payments, facts: facts(phase: .finalized, hasDetailsAccess: true)))
    }

    func testCoreModulesAreNeverLocked() {
        let denied = facts(phase: .polling, viewerAccepted: false, hasDetailsAccess: false)
        for module in [HubModule.date, .location, .participants, .scenarios, .recap] {
            XCTAssertFalse(EventHubModel.isLocked(module, facts: denied), "\(module) ne doit jamais être verrouillé")
        }
    }

    // MARK: - Mise en évidence

    func testAtMostOneTileIsHighlighted() {
        let phases: [EventHubFacts.Phase] = [.draft, .polling, .comparing, .confirmed, .organizing, .finalized]
        for phase in phases {
            for organizer in [true, false] {
                for voted in [true, false] {
                    let model = EventHubModel(facts: facts(phase: phase, isOrganizer: organizer, userBallotComplete: voted))
                    XCTAssertLessThanOrEqual(model.tiles.filter(\.isHighlighted).count, 1, "\(phase) organizer=\(organizer)")
                }
            }
        }
    }

    func testNoHighlightWhileOrganizing() {
        let model = EventHubModel(facts: facts(phase: .organizing, isOrganizer: true))
        XCTAssertTrue(model.tiles.allSatisfy { !$0.isHighlighted })
    }

    // MARK: - Règle partagée « prêt à confirmer »

    func testHomeAndHubShareTheReadyToConfirmRule() {
        for pollOpen in [true, false] {
            for known in [true, false] {
                for (complete, others, othersComplete) in [(0, 0, 0), (1, 0, 0), (2, 3, 1), (4, 3, 3), (3, 3, 3)] {
                    for organizer in [true, false] {
                        let shared = PollReadiness.readyToConfirm(
                            isOrganizer: organizer, pollOpen: pollOpen, ballotsKnown: known,
                            votersWithCompleteBallot: complete,
                            otherVotersComplete: othersComplete, otherEligibleVoters: others
                        )
                        let home = HomeEventFacts(
                            id: "e", title: "t", phase: .polling, role: organizer ? .organizer : .participant,
                            isOwner: organizer, isPast: false, readOnly: false, pollOpen: pollOpen,
                            viewerAccepted: true,
                            ballots: HomeBallotStats(
                                userBallotComplete: true, votersWithCompleteBallot: complete,
                                eligibleVoters: others + 1, otherVotersComplete: othersComplete,
                                otherEligibleVoters: others, ballotsKnown: known
                            ),
                            deadline: nil, eventDate: nil, participantNames: []
                        )
                        let hub = facts(
                            isOrganizer: organizer, pollOpen: pollOpen, userBallotComplete: true, ballotsKnown: known,
                            votersWithCompleteBallot: complete, eligibleVoters: others + 1,
                            otherEligibleVoters: others, otherVotersComplete: othersComplete
                        )
                        XCTAssertEqual(home.readyToConfirm, shared)
                        XCTAssertEqual(hub.readyToConfirm, shared)
                    }
                }
            }
        }
    }

    func testReadyToConfirmOnlyAppliesToPolls() {
        let confirmed = facts(
            phase: .confirmed, isOrganizer: true,
            votersWithCompleteBallot: 4, otherEligibleVoters: 4, otherVotersComplete: 4
        )
        XCTAssertFalse(confirmed.readyToConfirm)
        XCTAssertFalse(confirmed.voteRequired)
    }
}
