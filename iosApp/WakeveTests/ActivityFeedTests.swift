import XCTest
@testable import Wakeve

/// Règles pures du fil d'activité (couche 6, #47).
final class ActivityFeedTests: XCTestCase {
    private let now = ISO8601DateFormatter().date(from: "2026-10-01T10:00:00Z")!

    private func facts(
        _ id: String,
        title: String? = nil,
        phase: HomeEventFacts.Phase = .polling,
        role: HomeEventFacts.Role = .participant,
        accepted: Bool = true,
        voted: Bool = true,
        othersComplete: Int = 1,
        othersEligible: Int = 3,
        past: Bool = false,
        readOnly: Bool = false,
        deadline: Date? = nil,
        rsvpPending: Bool = false
    ) -> ActivityEventFacts {
        ActivityEventFacts(
            facts: HomeEventFacts(
                id: id, title: title ?? id, phase: phase, role: role, isOwner: role == .organizer,
                isPast: past, readOnly: readOnly, pollOpen: true, viewerAccepted: accepted || role == .organizer,
                ballots: HomeBallotStats(
                    userBallotComplete: voted, votersWithCompleteBallot: othersComplete + (voted ? 1 : 0),
                    eligibleVoters: othersEligible + 1,
                    otherVotersComplete: othersComplete, otherEligibleVoters: othersEligible
                ),
                deadline: deadline, eventDate: nil, participantNames: []
            ),
            rsvpPending: rsvpPending
        )
    }

    private func note(
        _ id: String, event: String?, read: Bool = false, minutesAgo: Double = 10
    ) -> ActivityNotification {
        ActivityNotification(
            id: id, eventId: event, title: "T\(id)", message: "M\(id)", isRead: read,
            date: now.addingTimeInterval(-minutesAgo * 60)
        )
    }

    // MARK: - Actions

    func testMissingVoteIsAnActionTargetingTheVoteScreen() {
        let groups = ActivityFeed.build(facts: [facts("e1", voted: false)], notifications: [], newMessages: [:], filter: .toDo)
        XCTAssertEqual(groups.count, 1)
        XCTAssertEqual(groups[0].eventId, "e1")
        XCTAssertEqual(groups[0].entries.map(\.kind), [.voteRequired])
        XCTAssertEqual(groups[0].entries.first?.target, .vote(eventId: "e1"))
        XCTAssertTrue(groups[0].entries.allSatisfy(\.needsAction))
        XCTAssertEqual(groups[0].actionCount, 1)
    }

    func testReadyToConfirmTargetsPollResultsForTheOrganizer() {
        let ready = facts("e1", role: .organizer, voted: false, othersComplete: 3, othersEligible: 3)
        let groups = ActivityFeed.build(facts: [ready], notifications: [], newMessages: [:], filter: .toDo)
        XCTAssertEqual(groups.first?.entries.map(\.kind), [.readyToConfirm])
        XCTAssertEqual(groups.first?.entries.first?.target, .pollResults(eventId: "e1"))
    }

    func testPendingInvitationIsAnActionOpeningTheHubAndNeverAVote() {
        // Invitation en attente : pas encore votant (viewerAccepted faux), la réponse d'abord.
        let invited = facts("e1", accepted: false, voted: false, rsvpPending: true)
        let groups = ActivityFeed.build(facts: [invited], notifications: [], newMessages: [:], filter: .toDo)
        XCTAssertEqual(groups.first?.entries.map(\.kind), [.rsvpPending])
        XCTAssertEqual(groups.first?.entries.first?.target, .hub(eventId: "e1"))
    }

    func testPendingInvitationOnAPastOrReadOnlyEventIsNotAnAction() {
        let past = facts("p", accepted: false, past: true, rsvpPending: true)
        let finalized = facts("f", phase: .finalized, accepted: false, readOnly: true, rsvpPending: true)
        let organizer = facts("o", role: .organizer, rsvpPending: true)
        XCTAssertTrue(ActivityFeed.build(facts: [past, finalized, organizer], notifications: [], newMessages: [:], filter: .toDo).isEmpty)
    }

    func testVotedEventHasNoActionEvenWithAVoteNotification() {
        // Le type de notification ne décide pas : une notification de vote après avoir voté reste informative.
        let groups = ActivityFeed.build(
            facts: [facts("e1", voted: true)], notifications: [note("n1", event: "e1")], newMessages: [:], filter: .toDo
        )
        XCTAssertTrue(groups.isEmpty)
    }

    // MARK: - Filtres

    func testAllAddsNotificationsUnreadFirstThenMessagesLine() {
        let groups = ActivityFeed.build(
            facts: [facts("e1", voted: false)],
            notifications: [note("old", event: "e1", read: true, minutesAgo: 1), note("new", event: "e1", minutesAgo: 30)],
            newMessages: ["e1": 3],
            filter: .all
        )
        XCTAssertEqual(groups.count, 1)
        let kinds = groups[0].entries.map(\.kind)
        XCTAssertEqual(kinds, [
            .voteRequired,
            .messages(count: 3),
            .notification(title: "Tnew", message: "Mnew", isRead: false),
            .notification(title: "Told", message: "Mold", isRead: true)
        ])
        XCTAssertEqual(groups[0].entries[1].target, .comments(eventId: "e1"))
        XCTAssertEqual(groups[0].entries[2].target, .hub(eventId: "e1"))
        XCTAssertFalse(groups[0].entries[1].needsAction, "Les messages ne portent pas de point rouge.")
        XCTAssertFalse(groups[0].entries[2].needsAction)
        XCTAssertEqual(groups[0].latestDate, now.addingTimeInterval(-60))
    }

    func testToDoHidesMessagesNotificationsAndEmptyGroups() {
        let groups = ActivityFeed.build(
            facts: [facts("e1", voted: false), facts("quiet")],
            notifications: [note("n", event: "quiet")],
            newMessages: ["quiet": 2, "e1": 4],
            filter: .toDo
        )
        XCTAssertEqual(groups.map(\.eventId), ["e1"])
        XCTAssertEqual(groups[0].entries.map(\.kind), [.voteRequired])
    }

    func testMessagesLineOnlyWhenThereAreNewMessages() {
        let groups = ActivityFeed.build(facts: [facts("e1")], notifications: [], newMessages: ["e1": 0], filter: .all)
        XCTAssertTrue(groups.isEmpty)
    }

    // MARK: - Groupes et tri

    func testGroupsWithActionsComeFirstThenMostRecentActivity() {
        let soon = now.addingTimeInterval(86_400)
        let later = now.addingTimeInterval(3 * 86_400)
        let groups = ActivityFeed.build(
            facts: [
                facts("quietOld", title: "Ancien"), facts("quietNew", title: "Récent"),
                facts("voteLater", voted: false, deadline: later), facts("voteSoon", voted: false, deadline: soon)
            ],
            notifications: [note("a", event: "quietOld", minutesAgo: 120), note("b", event: "quietNew", minutesAgo: 5)],
            newMessages: [:],
            filter: .all
        )
        XCTAssertEqual(groups.map(\.eventId), ["voteSoon", "voteLater", "quietNew", "quietOld"])
        XCTAssertEqual(groups.first?.title, "voteSoon")
    }

    func testNotificationsWithoutAKnownEventGoToTheGeneralGroupLast() {
        let groups = ActivityFeed.build(
            facts: [facts("e1")],
            notifications: [note("free", event: nil, minutesAgo: 1), note("gone", event: "deleted", minutesAgo: 2),
                            note("e", event: "e1", minutesAgo: 60)],
            newMessages: [:],
            filter: .all
        )
        XCTAssertEqual(groups.map(\.eventId), ["e1", nil])
        let general = groups[1]
        XCTAssertNil(general.title, "Le titre « Général » est localisé par la vue.")
        XCTAssertEqual(general.entries.count, 2)
        XCTAssertTrue(general.entries.allSatisfy { $0.target == nil }, "Aucun événement à ouvrir.")
    }

    func testEntryIdsAreUniqueAndStable() {
        let groups = ActivityFeed.build(
            facts: [facts("e1", voted: false)], notifications: [note("n1", event: "e1")], newMessages: ["e1": 1], filter: .all
        )
        let ids = groups.flatMap(\.entries).map(\.id)
        XCTAssertEqual(Set(ids).count, ids.count)
        XCTAssertEqual(ids, ["e1.vote", "e1.messages", "notification.n1"])
    }

    // MARK: - Compteurs

    func testBadgeCountsActionsAndUnreadNotifications() {
        let input: [ActivityEventFacts] = [
            facts("vote", voted: false),
            facts("rsvp", accepted: false, voted: false, rsvpPending: true),
            facts("quiet")
        ]
        let notes = [note("u1", event: "quiet"), note("u2", event: nil), note("r", event: "quiet", read: true)]
        XCTAssertEqual(ActivityFeed.toDoCount(facts: input), 2)
        XCTAssertEqual(ActivityFeed.badgeCount(facts: input, notifications: notes), 4)
        XCTAssertEqual(ActivityFeed.badgeCount(facts: [], notifications: []), 0)
    }

    func testTargetsExposeTheirEvent() {
        XCTAssertEqual(ActivityTarget.hub(eventId: "a").eventId, "a")
        XCTAssertEqual(ActivityTarget.vote(eventId: "b").eventId, "b")
        XCTAssertEqual(ActivityTarget.pollResults(eventId: "c").eventId, "c")
        XCTAssertEqual(ActivityTarget.comments(eventId: "d").eventId, "d")
    }
}
