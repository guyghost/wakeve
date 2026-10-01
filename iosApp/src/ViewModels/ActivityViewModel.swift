import Foundation

/// Fil d'activité de la refonte (couche 6, #47) : faits des événements (règles de l'accueil),
/// invitations en attente, notifications et nouveaux messages, regroupés par `ActivityFeed`.
@MainActor
final class ActivityViewModel: ObservableObject {
    enum State: Equatable { case loading, loaded, failed }

    @Published private(set) var state: State = .loading
    @Published var filter: ActivityFilter = .toDo {
        didSet { if filter != oldValue { regroup() } }
    }
    @Published private(set) var groups: [ActivityEventGroup] = []
    /// Compteur du segment « À traiter (n) », aussi badge de la zone Activité (actions seulement).
    @Published private(set) var toDoCount = 0

    private let viewerId: String
    private let source: ActivitySource
    private let seenStore: ActivitySeenStore
    private let now: () -> Date
    private var facts: [ActivityEventFacts] = []
    private var notifications: [ActivityNotification] = []
    private var newMessages: [String: Int] = [:]
    private var hasData = false
    /// Seul le dernier chargement lancé publie son résultat.
    private var generation = 0

    init(viewerId: String, source: ActivitySource, seenStore: ActivitySeenStore, now: @escaping () -> Date = Date.init) {
        self.viewerId = viewerId
        self.source = source
        self.seenStore = seenStore
        self.now = now
    }

    func reload() async {
        generation += 1
        let token = generation
        do {
            let snapshot = try await source.load(viewerId: viewerId)
            guard token == generation else { return }
            let current = now()
            facts = snapshot.events.map { raw in
                ActivityEventFacts(
                    facts: EventsHomeViewModel.facts(from: raw, now: current),
                    rsvpPending: snapshot.rsvpPendingEventIds.contains(raw.id)
                )
            }
            notifications = snapshot.notifications
            newMessages = snapshot.newMessages
            hasData = true
            regroup()
            state = .loaded
        } catch {
            // Chargement annulé (zone quittée) : on garde l'état courant ; échec : données conservées.
            guard token == generation, !(error is CancellationError) else { return }
            state = hasData ? .loaded : .failed
        }
    }

    /// Commentaires ouverts depuis Activité : la ligne « N nouveaux messages » disparaît.
    func markSeen(eventId: String) async {
        seenStore.markSeen(eventId: eventId, at: now())
        newMessages[eventId] = nil
        regroup()
        await reload()
    }

    func markRead(notificationId: String) {
        guard let index = notifications.firstIndex(where: { $0.id == notificationId }),
              !notifications[index].isRead else { return }
        source.markNotificationRead(id: notificationId)
        let note = notifications[index]
        notifications[index] = ActivityNotification(
            id: note.id, eventId: note.eventId, title: note.title, message: note.message, isRead: true, date: note.date
        )
        regroup()
    }

    private func regroup() {
        groups = ActivityFeed.build(facts: facts, notifications: notifications, newMessages: newMessages, filter: filter)
        toDoCount = ActivityFeed.toDoCount(facts: facts)
    }
}
