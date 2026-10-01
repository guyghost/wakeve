import XCTest
import SwiftUI
@testable import Wakeve

@MainActor
final class ActivityViewTests: XCTestCase {
    private let now = ISO8601DateFormatter().date(from: "2026-10-01T10:00:00Z")!
    private let fr = Locale(identifier: "fr")
    private struct Boom: Error {}

    private final class StubSource: ActivitySource {
        let result: Result<ActivitySnapshot, Error>
        init(_ result: Result<ActivitySnapshot, Error>) { self.result = result }
        func load(viewerId: String) async throws -> ActivitySnapshot { try result.get() }
        func markNotificationRead(id: String) {}
    }

    private final class MemorySeenStore: ActivitySeenStore {
        func lastSeenDates() -> [String: Date] { [:] }
        func markSeen(eventId: String, at date: Date) {}
    }

    private func raw(_ id: String, title: String, voted: Bool) -> HomeRawEvent {
        HomeRawEvent(id: id, title: title, statusName: "POLLING", isOrganizer: false, isOwner: false,
                     isPast: false, readOnly: false, viewerAccepted: true,
                     deadlineISO: "2026-10-05T10:00:00Z", finalDateISO: nil, earliestSlotStartISO: nil,
                     ballots: HomeBallotStats(userBallotComplete: voted, votersWithCompleteBallot: 1, eligibleVoters: 4,
                                              otherVotersComplete: 1, otherEligibleVoters: 3),
                     participantNames: [], hasPendingSync: false)
    }

    private func loadedViewModel(filter: ActivityFilter = .all, empty: Bool = false) async -> ActivityViewModel {
        let snapshot = empty
            ? ActivitySnapshot(events: [], rsvpPendingEventIds: [], notifications: [], newMessages: [:])
            : ActivitySnapshot(
                events: [raw("e1", title: "Week-end raclette à Chamonix avec toute la bande", voted: false),
                         raw("e2", title: "Brunch", voted: true)],
                rsvpPendingEventIds: [],
                notifications: [ActivityNotification(
                    id: "n1", eventId: "e2", title: "Nouveau créneau proposé pour le brunch du dimanche",
                    message: "Léa a ajouté dimanche 11 h", isRead: false, date: now.addingTimeInterval(-7_200)
                )],
                newMessages: ["e1": 3]
            )
        let vm = ActivityViewModel(viewerId: "u", source: StubSource(.success(snapshot)), seenStore: MemorySeenStore(),
                                   now: { self.now })
        vm.filter = filter
        await vm.reload()
        return vm
    }

    private func render<V: View>(_ view: V, size: DynamicTypeSize, width: CGFloat = 390) -> CGSize {
        let host = UIHostingController(rootView: view.environment(\.dynamicTypeSize, size))
        return host.sizeThatFits(in: CGSize(width: width, height: CGFloat.greatestFiniteMagnitude))
    }

    // MARK: - Rendu

    func testLoadedFeedRendersWithinTheWidthAtAX5() async {
        let vm = await loadedViewModel()
        let feed = ActivityFeedView(viewModel: vm, onOpen: { _ in })
        let regular = render(feed, size: .large)
        let ax5 = render(feed, size: .accessibility5)
        XCTAssertGreaterThan(regular.height, 0)
        XCTAssertLessThanOrEqual(ax5.width, 390, "Pas de débordement horizontal en AX5.")
        XCTAssertGreaterThan(ax5.height, regular.height, "Le texte s'agrandit au lieu d'être rogné.")
    }

    func testRowKeepsMinimumTapTargetAtAX5() async {
        let vm = await loadedViewModel()
        let entry = try? XCTUnwrap(vm.groups.first?.entries.first)
        guard let entry else { return XCTFail("Aucune entrée") }
        let row = ActivityEntryRow(entry: entry, now: now, onOpen: {})
        XCTAssertGreaterThanOrEqual(render(row, size: .accessibility5, width: 358).height, WK.Size.minTapTarget)
        XCTAssertGreaterThanOrEqual(render(row, size: .xSmall, width: 358).height, WK.Size.minTapTarget)
    }

    func testEmptyAndFailedStatesRender() async {
        let empty = await loadedViewModel(filter: .toDo, empty: true)
        XCTAssertEqual(empty.state, .loaded)
        XCTAssertTrue(empty.groups.isEmpty)
        XCTAssertGreaterThan(render(ActivityFeedView(viewModel: empty, onOpen: { _ in }), size: .large).height, 0)

        let failing = ActivityViewModel(viewerId: "u", source: StubSource(.failure(Boom())), seenStore: MemorySeenStore())
        await failing.reload()
        XCTAssertEqual(failing.state, .failed)
        XCTAssertGreaterThan(render(ActivityFeedView(viewModel: failing, onOpen: { _ in }), size: .accessibility5).height, 0)
    }

    // MARK: - Textes

    func testSegmentTitlesShowTheToDoCount() {
        XCTAssertEqual(ActivityFeedView.filterTitle(.toDo, count: 3, locale: fr), "À traiter (3)")
        XCTAssertEqual(ActivityFeedView.filterTitle(.all, count: 3, locale: fr), "Tout")
        XCTAssertEqual(ActivityFeedView.filterTitle(.toDo, count: 1, locale: Locale(identifier: "en")), "To do (1)")
    }

    func testEmptyTextDependsOnTheFilter() {
        XCTAssertEqual(ActivityFeedView.emptyText(for: .toDo, locale: fr), "Rien à traiter pour l'instant")
        XCTAssertEqual(ActivityFeedView.emptyText(for: .all, locale: fr), "Aucune activité pour l'instant")
    }

    func testRowTextsAndVoiceOverDoNotRelyOnTheRedDot() {
        let vote = ActivityEntry(id: "e.vote", eventId: "e", kind: .voteRequired, needsAction: true, date: nil,
                                 target: .vote(eventId: "e"))
        XCTAssertEqual(ActivityEntryRow.title(for: vote, locale: fr), "Vote : il manque ton vote")
        XCTAssertEqual(ActivityEntryRow.accessibilityLabel(for: vote, now: now, locale: fr),
                       "À traiter, Vote : il manque ton vote", "Le point rouge n'est pas le seul porteur de l'état.")

        let messages = ActivityEntry(id: "e.messages", eventId: "e", kind: .messages(count: 3), needsAction: false,
                                     date: nil, target: .comments(eventId: "e"))
        XCTAssertEqual(ActivityEntryRow.title(for: messages, locale: fr), "3 nouveaux messages")
        XCTAssertEqual(ActivityEntryRow.accessibilityLabel(for: messages, now: now, locale: fr), "3 nouveaux messages")

        let unread = ActivityEntry(id: "notification.n", eventId: "e", kind: .notification(title: "Titre", message: "Corps", isRead: false),
                                   needsAction: false, date: now.addingTimeInterval(-7_200), target: .hub(eventId: "e"))
        let label = ActivityEntryRow.accessibilityLabel(for: unread, now: now, locale: fr)
        XCTAssertTrue(label.hasPrefix("Titre, Corps, Non lu, "), label)
        XCTAssertTrue(label.contains("2"), "Date relative lue : \(label)")
    }

    func testGeneralNotificationsCanStillBeMarkedRead() {
        let unread = ActivityEntry(id: "notification.n1", eventId: nil, kind: .notification(title: "T", message: "", isRead: false),
                                   needsAction: false, date: now, target: nil)
        let read = ActivityEntry(id: "notification.n2", eventId: nil, kind: .notification(title: "T", message: "", isRead: true),
                                 needsAction: false, date: now, target: nil)
        XCTAssertEqual(unread.notificationId, "n1")
        XCTAssertTrue(ActivityEntryRow.isInteractive(unread), "Une notification générale non lue se marque lue au tap.")
        XCTAssertFalse(ActivityEntryRow.isInteractive(read))
        let vote = ActivityEntry(id: "e.vote", eventId: "e", kind: .voteRequired, needsAction: true, date: nil,
                                 target: .vote(eventId: "e"))
        XCTAssertNil(vote.notificationId)
        XCTAssertTrue(ActivityEntryRow.isInteractive(vote))
    }

    func testGeneralGroupTitleIsLocalized() {
        let general = ActivityEventGroup(eventId: nil, title: nil, entries: [], actionCount: 0, latestDate: nil, deadline: nil)
        XCTAssertEqual(ActivityFeedView.groupTitle(general, locale: fr), "Général",
                       "Libellé propre au fil, pas « Conversation générale » hérité de l'Inbox.")
        XCTAssertEqual(ActivityFeedView.groupTitle(general, locale: Locale(identifier: "en")), "General")
        XCTAssertEqual(ActivityFeedView.groupTitle(general, locale: Locale(identifier: "it")), "Generale")
        XCTAssertEqual(ActivityFeedView.groupTitle(general, locale: Locale(identifier: "pt")), "Geral")
        let event = ActivityEventGroup(eventId: "e", title: "Brunch", entries: [], actionCount: 0, latestDate: nil, deadline: nil)
        XCTAssertEqual(ActivityFeedView.groupTitle(event, locale: fr), "Brunch")
    }

    func testPickerLabelNamesTheFilter() {
        XCTAssertEqual(ActivityFeedView.filterLabel(locale: fr), "Filtre")
        XCTAssertEqual(ActivityFeedView.filterLabel(locale: Locale(identifier: "en")), "Filter")
        XCTAssertEqual(ActivityFeedView.filterLabel(locale: Locale(identifier: "es")), "Filtro")
    }

    func testRelativeDateFormatterIsCachedPerLocale() {
        let first = ActivityEntryRow.relativeDateFormatter(for: fr)
        XCTAssertTrue(first === ActivityEntryRow.relativeDateFormatter(for: Locale(identifier: "fr")))
        XCTAssertFalse(first === ActivityEntryRow.relativeDateFormatter(for: Locale(identifier: "en")))
        XCTAssertEqual(first.locale.identifier, "fr")
        XCTAssertEqual(first.unitsStyle, .short)
        XCTAssertEqual(ActivityEntryRow.relativeDate(now.addingTimeInterval(-7_200), now: now, locale: fr),
                       first.localizedString(for: now.addingTimeInterval(-7_200), relativeTo: now))
    }

    // MARK: - Contrat de la vue

    func testViewUsesNativeSegmentsWKCardsAndShellParameters() throws {
        let source = try String(contentsOf: URL(fileURLWithPath: #filePath).deletingLastPathComponent()
            .deletingLastPathComponent().appendingPathComponent("src/Views/Activity/ActivityView.swift"), encoding: .utf8)
        XCTAssertTrue(source.contains(".pickerStyle(.segmented)"))
        XCTAssertTrue(source.contains("WKCard("))
        XCTAssertTrue(source.contains("WK.Status.actionNeeded.color"))
        XCTAssertTrue(source.contains(".refreshable"))
        XCTAssertTrue(source.contains("wk.nav.activity"))
        XCTAssertTrue(source.contains("Text(Self.filterLabel())"), "Le libellé du sélecteur nomme le filtre.")
        XCTAssertTrue(source.contains("var reloadToken: Int = 0"))
        XCTAssertTrue(source.contains("var onRootStateChange: ((Bool) -> Void)? = nil"))
        XCTAssertTrue(source.contains(".onChange(of: reloadToken)"))
        XCTAssertTrue(source.contains("@Binding var actionCount: Int"))
        XCTAssertTrue(source.contains(".onChange(of: viewModel.toDoCount, initial: true)"), "Badge = « À traiter (n) ».")
        XCTAssertFalse(source.contains("badgeCount"))
        XCTAssertTrue(source.contains("viewModel.markSeen(eventId:"), "Ouvrir les messages met à jour le marqueur.")
        XCTAssertTrue(source.contains("@ScaledMetric(relativeTo: .body) private var dotSize"),
                      "Le point d'action suit Dynamic Type (minuscule en AX5 sinon).")
    }
}
