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
        XCTAssertTrue(EventHubModel.isLocked(.transport, facts: facts(phase: .finalized, hasDetailsAccess: false)))
    }

    func testBudgetFollowsTheOrganizationDashboardGuard() {
        // `case .budgetOverview` : `canAccessOrganizationDashboard` (organisation/finalisé + accès), sinon « accès refusé ».
        for phase in [EventHubFacts.Phase.polling, .confirmed, .comparing] {
            XCTAssertTrue(tile(.budget, in: EventHubModel(facts: facts(phase: phase, isOrganizer: true)))!.isLocked,
                          "\(phase) : le budget mènerait à un écran d'accès refusé")
        }
        XCTAssertFalse(tile(.budget, in: EventHubModel(facts: facts(phase: .organizing, hasDetailsAccess: true)))!.isLocked)
        XCTAssertTrue(tile(.budget, in: EventHubModel(facts: facts(phase: .organizing, hasDetailsAccess: false)))!.isLocked)
    }

    func testDetailedPlanningFollowsItsGuard() {
        // `canAccessDetailedPlanning` : confirmé, comparaison, organisation ou finalisé + accès.
        for module in [HubModule.accommodation, .meals, .equipment, .activities, .photos] {
            XCTAssertTrue(EventHubModel.isLocked(module, facts: facts(phase: .polling, isOrganizer: true)), "\(module)")
            XCTAssertTrue(EventHubModel.isLocked(module, facts: facts(phase: .draft, isOrganizer: true)), "\(module)")
            for phase in [EventHubFacts.Phase.confirmed, .comparing, .organizing, .finalized] {
                XCTAssertFalse(EventHubModel.isLocked(module, facts: facts(phase: phase, hasDetailsAccess: true)), "\(module) \(phase)")
                XCTAssertTrue(EventHubModel.isLocked(module, facts: facts(phase: phase, hasDetailsAccess: false)), "\(module) \(phase)")
            }
        }
    }

    /// Miroir indépendant des gardes des `case` de `homeTabContent` : aucune tuile ouverte ne mène à `AccessDenied`.
    func testNoUnlockedTileOpensAnAccessDeniedScreen() {
        let phases: [EventHubFacts.Phase] = [.draft, .polling, .comparing, .confirmed, .organizing, .finalized]
        for phase in phases {
            for access in [true, false] {
                let f = facts(phase: phase, hasDetailsAccess: access)
                let dashboard = [.organizing, .finalized].contains(phase) && access
                let planning = [.confirmed, .comparing, .organizing, .finalized].contains(phase) && access
                let transport = [.confirmed, .organizing, .finalized].contains(phase) && access
                for module in HubModule.allCases {
                    let granted: Bool
                    switch module {
                    case .date, .location, .participants, .scenarios, .recap: granted = true
                    case .budget, .meetings, .payments: granted = dashboard
                    case .accommodation, .meals, .equipment, .activities, .photos: granted = planning
                    case .transport: granted = transport
                    }
                    XCTAssertEqual(EventHubModel.isLocked(module, facts: f), !granted, "\(module) \(phase) accès=\(access)")
                }
            }
        }
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

    // MARK: - Localisation

    static let hubStringKeys = HubModule.allCases.map { "hub.module.\($0.rawValue)" } + [
        "hub.tile.locked", "hub.tile.hint", "hub.summary.format",
        "hub.primary.vote", "hub.primary.results", "hub.primary.confirm_date_format", "hub.primary.confirm_date",
        "hub.primary.organize", "hub.primary.finalize", "hub.primary.sign_in", "hub.primary.add_dates",
        "hub.quick_vote.title_format", "hub.menu.info", "hub.menu.more",
        "hub.summary.meals_progress_format"
    ]
    static let hubPluralKeys = [
        "hub.slots_count", "hub.guests_count", "hub.activities_count", "hub.items_count", "hub.meetings_count"
    ]
    /// Clés existantes réutilisées par le hub.
    static let reusedKeys = [
        "poll.yes", "poll.maybe", "poll.no", "event.detail.menu.add_participants",
        "event.lifecycle.organizing.title", "event.lifecycle.organizing.confirm_message", "event.lifecycle.organizing.action",
        "event.lifecycle.finalize.title", "event.lifecycle.finalize.confirm_message", "event.lifecycle.finalize.action",
        "event.lifecycle.guest.confirm_message", "common.retry", "common.error_generic",
        "event.detail.slot_option_singular_format", "event.detail.slot_options_plural_format",
        "event.detail.canvas.participants.confirmed_format", "event.detail.canvas.participants.pending_format",
        "scenario.options_count_format", "transport.plan.selected",
        "event.detail.payment_pot.define_before_share", "event.detail.payment_pot.define_goal",
        "event.detail.payment_pot.goal_format"
    ]

    func testEveryHubKeyExistsInEveryLanguage() throws {
        let root = URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent()
        for locale in ["en", "fr", "es", "it", "pt"] {
            let strings = try String(contentsOf: root.appendingPathComponent("src/Resources/\(locale).lproj/Localizable.strings"), encoding: .utf8)
            for key in Self.hubStringKeys + Self.reusedKeys {
                XCTAssertTrue(strings.contains("\"\(key)\" ="), "\(key) manquante (\(locale))")
            }
            let dict = try String(contentsOf: root.appendingPathComponent("src/Resources/\(locale).lproj/Localizable.stringsdict"), encoding: .utf8)
            for key in Self.hubPluralKeys {
                XCTAssertTrue(dict.contains("<key>\(key)</key>"), "pluriel \(key) manquant (\(locale))")
            }
        }
    }

    func testHubPluralsResolveInFrenchWithTutoiementCopy() {
        let fr = Locale(identifier: "fr_FR")
        XCTAssertEqual(String(format: WK.localizedFormat("hub.slots_count", locale: fr), locale: fr, 1), "1 créneau")
        XCTAssertEqual(String(format: WK.localizedFormat("hub.slots_count", locale: fr), locale: fr, 3), "3 créneaux")
        XCTAssertEqual(String(format: WK.localizedFormat("hub.guests_count", locale: fr), locale: fr, 2), "2 invités")
        XCTAssertEqual(WK.localizedFormat("hub.primary.sign_in", locale: fr), "Connecte-toi pour finaliser")
        XCTAssertEqual(String(format: WK.localizedFormat("hub.quick_vote.title_format", locale: fr), "sam. 3 oct."),
                       "Tu es dispo le sam. 3 oct. ?")
    }
}
