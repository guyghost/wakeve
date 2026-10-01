import Foundation

/// Destination d'une entrée du fil d'activité (couche 6, #47).
enum ActivityTarget: Equatable {
    case hub(eventId: String)
    case vote(eventId: String)
    case pollResults(eventId: String)
    case comments(eventId: String)

    var eventId: String {
        switch self {
        case .hub(let id), .vote(let id), .pollResults(let id), .comments(let id): return id
        }
    }
}

/// Filtre du fil : « À traiter » (défaut) ou « Tout ».
enum ActivityFilter: Equatable {
    case toDo, all
}

/// Faits d'un événement pour le fil : mêmes règles que l'accueil (`HomeEventFacts`) + invitation en attente.
struct ActivityEventFacts: Equatable {
    let facts: HomeEventFacts
    /// Membre non organisateur dont la réponse à l'invitation est en attente.
    let rsvpPending: Bool
}

/// Notification de la table `notification`, déjà décodée par la source.
struct ActivityNotification: Equatable {
    let id: String
    /// `eventId` extrait de `data` (JSON), absent pour une notification générale.
    let eventId: String?
    let title: String
    let message: String
    let isRead: Bool
    let date: Date
}

struct ActivityEntry: Identifiable, Equatable {
    enum Kind: Equatable {
        case voteRequired
        case readyToConfirm
        case rsvpPending
        case notification(title: String, message: String, isRead: Bool)
        case messages(count: Int)
    }

    let id: String
    let eventId: String?
    let kind: Kind
    /// Point rouge : attend une action de l'utilisateur (état réel de l'événement, jamais le type de notification).
    let needsAction: Bool
    let date: Date?
    /// `nil` : rien à ouvrir (notification générale ou événement inconnu localement).
    let target: ActivityTarget?

    /// Identifiant de la notification d'origine (`notification.<id>`), sinon `nil`.
    var notificationId: String? {
        guard case .notification = kind, id.hasPrefix(ActivityFeed.notificationPrefix) else { return nil }
        return String(id.dropFirst(ActivityFeed.notificationPrefix.count))
    }
}

struct ActivityEventGroup: Identifiable, Equatable {
    /// `nil` : groupe « Général » (notifications sans événement connu).
    let eventId: String?
    /// `nil` : groupe « Général », libellé localisé par la vue.
    let title: String?
    let entries: [ActivityEntry]
    let actionCount: Int
    let latestDate: Date?
    /// Échéance du sondage : départage les groupes qui attendent une action.
    let deadline: Date?

    var id: String { eventId ?? ActivityFeed.generalGroupId }
}

/// Règles pures du fil d'activité.
enum ActivityFeed {
    static let generalGroupId = "activity.general"
    static let notificationPrefix = "notification."

    /// Entrées d'action d'un événement (« À traiter »), dans l'ordre d'affichage.
    static func actionEntries(for item: ActivityEventFacts) -> [ActivityEntry] {
        let facts = item.facts
        let id = facts.id
        var entries: [ActivityEntry] = []
        if facts.readyToConfirm {
            entries.append(ActivityEntry(id: "\(id).ready", eventId: id, kind: .readyToConfirm, needsAction: true,
                                         date: nil, target: .pollResults(eventId: id)))
        } else if facts.voteRequired {
            entries.append(ActivityEntry(id: "\(id).vote", eventId: id, kind: .voteRequired, needsAction: true,
                                         date: nil, target: .vote(eventId: id)))
        }
        if rsvpActionable(item) {
            entries.append(ActivityEntry(id: "\(id).rsvp", eventId: id, kind: .rsvpPending, needsAction: true,
                                         date: nil, target: .hub(eventId: id)))
        }
        return entries
    }

    /// Une invitation n'est à traiter que sur un événement encore modifiable, pour un participant.
    private static func rsvpActionable(_ item: ActivityEventFacts) -> Bool {
        item.rsvpPending && item.facts.role == .participant && !item.facts.isPast && !item.facts.readOnly
    }

    static func build(
        facts: [ActivityEventFacts],
        notifications: [ActivityNotification],
        newMessages: [String: Int],
        filter: ActivityFilter
    ) -> [ActivityEventGroup] {
        let known = Set(facts.map(\.facts.id))
        let byEvent = Dictionary(grouping: notifications.filter { $0.eventId.map(known.contains) ?? false },
                                 by: { $0.eventId ?? "" })

        var groups: [ActivityEventGroup] = facts.compactMap { item in
            let id = item.facts.id
            var entries = actionEntries(for: item)
            if filter == .all {
                if let count = newMessages[id], count > 0 {
                    entries.append(ActivityEntry(id: "\(id).messages", eventId: id, kind: .messages(count: count),
                                                 needsAction: false, date: nil, target: .comments(eventId: id)))
                }
                entries += sortedNotifications(byEvent[id] ?? []).map { notificationEntry($0, target: .hub(eventId: id)) }
            }
            guard !entries.isEmpty else { return nil }
            return ActivityEventGroup(
                eventId: id, title: item.facts.title, entries: entries,
                actionCount: entries.filter(\.needsAction).count,
                latestDate: entries.compactMap(\.date).max(),
                deadline: item.facts.deadline
            )
        }
        groups.sort(by: precedes)

        if filter == .all {
            let general = notifications.filter { $0.eventId.map { !known.contains($0) } ?? true }
            if !general.isEmpty {
                let entries = sortedNotifications(general).map { notificationEntry($0, target: nil) }
                groups.append(ActivityEventGroup(
                    eventId: nil, title: nil, entries: entries, actionCount: 0,
                    latestDate: entries.compactMap(\.date).max(), deadline: nil
                ))
            }
        }
        return groups
    }

    /// Nombre d'éléments affichés sous « À traiter ».
    static func toDoCount(facts: [ActivityEventFacts]) -> Int {
        facts.reduce(0) { $0 + actionEntries(for: $1).count }
    }

    /// Badge de la zone Activité : actions réelles + notifications non lues.
    static func badgeCount(facts: [ActivityEventFacts], notifications: [ActivityNotification]) -> Int {
        toDoCount(facts: facts) + notifications.filter { !$0.isRead }.count
    }

    // MARK: - Tri

    /// Groupes avec action d'abord (échéance la plus proche), puis activité la plus récente, puis titre.
    private static func precedes(_ a: ActivityEventGroup, _ b: ActivityEventGroup) -> Bool {
        let aAction = a.actionCount > 0
        let bAction = b.actionCount > 0
        if aAction != bAction { return aAction }
        if aAction {
            let da = a.deadline ?? .distantFuture
            let db = b.deadline ?? .distantFuture
            if da != db { return da < db }
        }
        let la = a.latestDate ?? .distantPast
        let lb = b.latestDate ?? .distantPast
        if la != lb { return la > lb }
        return (a.title ?? "").localizedCompare(b.title ?? "") == .orderedAscending
    }

    /// Non lues d'abord, puis les plus récentes.
    private static func sortedNotifications(_ items: [ActivityNotification]) -> [ActivityNotification] {
        items.sorted { a, b in
            if a.isRead != b.isRead { return !a.isRead }
            if a.date != b.date { return a.date > b.date }
            return a.id < b.id
        }
    }

    private static func notificationEntry(_ note: ActivityNotification, target: ActivityTarget?) -> ActivityEntry {
        ActivityEntry(
            id: notificationPrefix + note.id, eventId: target?.eventId,
            kind: .notification(title: note.title, message: note.message, isRead: note.isRead),
            needsAction: false, date: note.date, target: target
        )
    }
}
