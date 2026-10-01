import XCTest
@testable import Wakeve

@MainActor
final class ActivityViewModelTests: XCTestCase {
    private let fixedNow = ISO8601DateFormatter().date(from: "2026-10-01T10:00:00Z")!
    private struct Boom: Error {}

    private final class StubSource: ActivitySource {
        var result: Result<ActivitySnapshot, Error>
        var loads = 0
        var marked: [String] = []
        init(_ result: Result<ActivitySnapshot, Error>) { self.result = result }
        func load(viewerId: String) async throws -> ActivitySnapshot {
            loads += 1
            return try result.get()
        }
        func markNotificationRead(id: String) { marked.append(id) }
    }

    /// Source dont chaque appel reste suspendu jusqu'à ce que le test le termine.
    @MainActor
    private final class ControlledSource: ActivitySource {
        var pending: [CheckedContinuation<ActivitySnapshot, Error>] = []
        func load(viewerId: String) async throws -> ActivitySnapshot {
            try await withCheckedThrowingContinuation { pending.append($0) }
        }
        nonisolated func markNotificationRead(id: String) {}
    }

    private final class MemorySeenStore: ActivitySeenStore {
        var dates: [String: Date] = [:]
        func lastSeenDates() -> [String: Date] { dates }
        func markSeen(eventId: String, at date: Date) { dates[eventId] = date }
    }

    private func raw(_ id: String, voted: Bool = true, accepted: Bool = true) -> HomeRawEvent {
        HomeRawEvent(id: id, title: id, statusName: "POLLING", isOrganizer: false, isOwner: false,
                     isPast: false, readOnly: false, viewerAccepted: accepted,
                     deadlineISO: "2026-10-05T10:00:00Z", finalDateISO: nil, earliestSlotStartISO: nil,
                     ballots: HomeBallotStats(userBallotComplete: voted, votersWithCompleteBallot: 1, eligibleVoters: 4,
                                              otherVotersComplete: 1, otherEligibleVoters: 3),
                     participantNames: [], hasPendingSync: false)
    }

    private func snapshot(
        events: [HomeRawEvent], rsvp: Set<String> = [], notes: [ActivityNotification] = [], messages: [String: Int] = [:]
    ) -> ActivitySnapshot {
        ActivitySnapshot(events: events, rsvpPendingEventIds: rsvp, notifications: notes, newMessages: messages)
    }

    private func note(_ id: String, event: String?, read: Bool = false) -> ActivityNotification {
        ActivityNotification(id: id, eventId: event, title: id, message: id, isRead: read, date: fixedNow)
    }

    private func makeVM(_ source: ActivitySource, seen: ActivitySeenStore = MemorySeenStore()) -> ActivityViewModel {
        ActivityViewModel(viewerId: "u", source: source, seenStore: seen, now: { self.fixedNow })
    }

    func testDefaultsToTheToDoFilterAndCounts() async {
        let source = StubSource(.success(snapshot(
            events: [raw("vote", voted: false), raw("invite", voted: false, accepted: false), raw("quiet")],
            rsvp: ["invite"],
            notes: [note("n1", event: "quiet"), note("n2", event: nil, read: true)],
            messages: ["quiet": 2]
        )))
        let vm = makeVM(source)
        XCTAssertEqual(vm.state, .loading)
        XCTAssertEqual(vm.filter, .toDo)
        await vm.reload()
        XCTAssertEqual(vm.state, .loaded)
        XCTAssertEqual(vm.toDoCount, 2)
        XCTAssertEqual(vm.badgeCount, 3, "2 actions + 1 notification non lue")
        XCTAssertEqual(Set(vm.groups.compactMap(\.eventId)), ["vote", "invite"])

        vm.filter = .all
        XCTAssertEqual(vm.groups.count, 4, "vote, invite, quiet et Général")
        XCTAssertEqual(vm.groups.last?.eventId, nil)
    }

    func testFailureWithoutDataShowsFailedAndKeepsDataOtherwise() async {
        let source = StubSource(.failure(Boom()))
        let vm = makeVM(source)
        await vm.reload()
        XCTAssertEqual(vm.state, .failed)

        source.result = .success(snapshot(events: [raw("vote", voted: false)]))
        await vm.reload()
        XCTAssertEqual(vm.groups.count, 1)

        source.result = .failure(Boom())
        await vm.reload()
        XCTAssertEqual(vm.state, .loaded, "Un échec garde les données affichées.")
        XCTAssertEqual(vm.groups.count, 1)
    }

    func testCancellationKeepsTheCurrentState() async {
        let source = StubSource(.failure(CancellationError()))
        let vm = makeVM(source)
        await vm.reload()
        XCTAssertEqual(vm.state, .loading)
    }

    func testOnlyTheLatestLoadPublishes() async {
        let source = ControlledSource()
        let vm = makeVM(source)
        let first = Task { await vm.reload() }
        while source.pending.count < 1 { await Task.yield() }
        let second = Task { await vm.reload() }
        while source.pending.count < 2 { await Task.yield() }
        source.pending[1].resume(returning: snapshot(events: [raw("new", voted: false)]))
        await second.value
        source.pending[0].resume(returning: snapshot(events: [raw("old", voted: false)]))
        await first.value
        XCTAssertEqual(vm.groups.map(\.eventId), ["new"])
    }

    func testMarkSeenStoresTheMarkerHidesTheLineAndReloads() async {
        let seen = MemorySeenStore()
        let source = StubSource(.success(snapshot(events: [raw("e1")], messages: ["e1": 3])))
        let vm = makeVM(source, seen: seen)
        vm.filter = .all
        await vm.reload()
        XCTAssertEqual(vm.groups.first?.entries.map(\.kind), [.messages(count: 3)])

        source.result = .success(snapshot(events: [raw("e1")], messages: ["e1": 0]))
        await vm.markSeen(eventId: "e1")
        XCTAssertEqual(seen.dates["e1"], fixedNow)
        XCTAssertEqual(source.loads, 2)
        XCTAssertTrue(vm.groups.isEmpty)
    }

    func testMarkReadUpdatesTheNotificationAndTheBadge() async {
        let source = StubSource(.success(snapshot(events: [raw("e1")], notes: [note("n1", event: "e1")])))
        let vm = makeVM(source)
        await vm.reload()
        XCTAssertEqual(vm.badgeCount, 1)
        vm.markRead(notificationId: "n1")
        XCTAssertEqual(source.marked, ["n1"])
        XCTAssertEqual(vm.badgeCount, 0)
        vm.filter = .all
        XCTAssertEqual(vm.groups.first?.entries.first?.kind, .notification(title: "n1", message: "n1", isRead: true))
        vm.markRead(notificationId: "n1")
        XCTAssertEqual(source.marked, ["n1"], "Une notification déjà lue n'est pas réécrite.")
    }

    // MARK: - Marqueur de consultation

    func testUserDefaultsSeenStoreIsPerUserAndPersistent() throws {
        let suite = "ActivitySeenStoreTests.\(UUID().uuidString)"
        let defaults = try XCTUnwrap(UserDefaults(suiteName: suite))
        defer { defaults.removePersistentDomain(forName: suite) }
        let lea = UserDefaultsActivitySeenStore(userId: "lea", defaults: defaults)
        XCTAssertTrue(lea.lastSeenDates().isEmpty)
        lea.markSeen(eventId: "e1", at: fixedNow)
        XCTAssertEqual(UserDefaultsActivitySeenStore(userId: "lea", defaults: defaults).lastSeenDates(), ["e1": fixedNow])
        XCTAssertTrue(UserDefaultsActivitySeenStore(userId: "tom", defaults: defaults).lastSeenDates().isEmpty)
    }

    // MARK: - Règles pures de la source

    func testEventIdIsReadFromNotificationDataJSON() {
        XCTAssertEqual(SharedActivitySource.eventId(fromData: #"{"eventId":"e1","eventName":"Raclette"}"#), "e1")
        XCTAssertEqual(SharedActivitySource.eventId(fromData: #"{"eventId":42,"count":3}"#), "42", "Valeurs non textuelles tolérées.")
        XCTAssertNil(SharedActivitySource.eventId(fromData: #"{"eventId":""}"#))
        XCTAssertNil(SharedActivitySource.eventId(fromData: "not json"))
        XCTAssertNil(SharedActivitySource.eventId(fromData: nil))
    }

    func testMessagesSinceUsesTheMarkerOrASevenDayBound() {
        let since = SharedActivitySource.messagesSinceISO(lastSeen: nil, ownLastCommentISO: nil, now: fixedNow)
        XCTAssertEqual(since, "2026-09-24T10:00:00.000Z")
        let seen = fixedNow.addingTimeInterval(-3_600)
        XCTAssertEqual(SharedActivitySource.messagesSinceISO(lastSeen: seen, ownLastCommentISO: nil, now: fixedNow),
                       "2026-10-01T09:00:00.000Z")
    }

    func testOwnLaterCommentMovesTheBoundAndIsNeverCounted() {
        // Son propre commentaire, plus récent que le marqueur : la chaîne exacte sert de borne stricte.
        let own = "2026-10-01T09:30:00.123456Z"
        XCTAssertEqual(SharedActivitySource.messagesSinceISO(lastSeen: fixedNow.addingTimeInterval(-3_600),
                                                             ownLastCommentISO: own, now: fixedNow), own)
        XCTAssertEqual(SharedActivitySource.messagesSinceISO(lastSeen: fixedNow, ownLastCommentISO: own, now: fixedNow),
                       "2026-10-01T10:00:00.000Z")
        XCTAssertEqual(SharedActivitySource.messagesSinceISO(lastSeen: nil, ownLastCommentISO: "garbage", now: fixedNow),
                       "2026-09-24T10:00:00.000Z")
    }

    func testSourceStaysOffTheMainActorAndReusesTheHomeSource() throws {
        let source = try String(contentsOf: URL(fileURLWithPath: #filePath).deletingLastPathComponent()
            .deletingLastPathComponent().appendingPathComponent("src/Services/SharedActivitySource.swift"), encoding: .utf8)
        XCTAssertTrue(source.contains("Task.detached"))
        XCTAssertTrue(source.contains("withTaskCancellationHandler"))
        XCTAssertTrue(source.contains("SharedEventsHomeSource()"), "Les événements et leurs faits viennent de l'accueil.")
        XCTAssertTrue(source.contains("getNotifications(user_id: viewerId, value_: 50)"))
        XCTAssertTrue(source.contains("ParticipantAccessMapper"))
    }

    /// `selectParticipantActivity` regroupe aussi par `author_name` : un auteur renommé donne plusieurs
    /// lignes et `executeAsOneOrNull()` lève une exception Kotlin qui arrête l'application.
    func testSourceUsesSingleRowCommentQueriesScopedToOneSection() throws {
        let source = try String(contentsOf: URL(fileURLWithPath: #filePath).deletingLastPathComponent()
            .deletingLastPathComponent().appendingPathComponent("src/Services/SharedActivitySource.swift"), encoding: .utf8)
        XCTAssertFalse(source.contains("selectParticipantActivity"))
        XCTAssertFalse(source.contains("executeAsOneOrNull"))
        XCTAssertTrue(source.contains("selectLastCommentAtByAuthorInSection(event_id:"))
        XCTAssertTrue(source.contains("countRecentActivityInSection(event_id:"))
        XCTAssertFalse(source.contains("countRecentActivity(event_id:"), "Compte limité à une section.")
    }
}
