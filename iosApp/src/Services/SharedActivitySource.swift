import Foundation
import Shared

/// Données brutes du fil d'activité (couche 6, #47).
struct ActivitySnapshot: Equatable {
    /// Événements de l'accueil (`SharedEventsHomeSource`), mêmes faits et mêmes règles.
    let events: [HomeRawEvent]
    /// Événements dont l'invitation attend la réponse du spectateur (membre non organisateur).
    let rsvpPendingEventIds: Set<String>
    let notifications: [ActivityNotification]
    /// Nouveaux commentaires par événement depuis la dernière consultation.
    let newMessages: [String: Int]
}

protocol ActivitySource {
    func load(viewerId: String) async throws -> ActivitySnapshot
    func markNotificationRead(id: String)
}

/// Dernière consultation des commentaires d'un événement depuis Activité. Aucun état de lecture
/// par utilisateur n'existe en base : ce marqueur reste local à l'appareil.
protocol ActivitySeenStore: AnyObject {
    func lastSeenDates() -> [String: Date]
    func markSeen(eventId: String, at date: Date)
}

final class UserDefaultsActivitySeenStore: ActivitySeenStore {
    private let defaults: UserDefaults
    private let key: String

    init(userId: String, defaults: UserDefaults = .standard) {
        self.defaults = defaults
        self.key = "activity.feed.lastSeen.\(userId)"
    }

    func lastSeenDates() -> [String: Date] {
        let stored = defaults.dictionary(forKey: key) as? [String: Double] ?? [:]
        return stored.mapValues { Date(timeIntervalSince1970: $0) }
    }

    func markSeen(eventId: String, at date: Date) {
        var stored = defaults.dictionary(forKey: key) as? [String: Double] ?? [:]
        stored[eventId] = date.timeIntervalSince1970
        defaults.set(stored, forKey: key)
    }
}

/// Source réelle du fil : événements et faits via `SharedEventsHomeSource` (non dupliqué), puis
/// invitations en attente, notifications et nouveaux commentaires, lus hors du fil principal.
struct SharedActivitySource: ActivitySource {
    /// Sans marqueur, les commentaires des 7 derniers jours seulement (évite « 200 nouveaux messages »).
    static let defaultLookback: TimeInterval = 7 * 86_400

    private let home: EventsHomeSource
    private let repository: DatabaseEventRepository
    private let database: WakeveDb
    private let seenStore: ActivitySeenStore

    init(
        seenStore: ActivitySeenStore,
        home: EventsHomeSource = SharedEventsHomeSource(),
        repository: DatabaseEventRepository = RepositoryProvider.shared.databaseRepository,
        database: WakeveDb = RepositoryProvider.shared.database
    ) {
        self.seenStore = seenStore
        self.home = home
        self.repository = repository
        self.database = database
    }

    func load(viewerId: String) async throws -> ActivitySnapshot {
        let events = try await home.loadEvents(viewerId: viewerId)
        try Task.checkCancellation()
        // Envoyer ces objets Kotlin non `Sendable` vers un autre fil est sûr : Kotlin 2.2 utilise le
        // modèle mémoire moderne (objets partageables entre fils, pas de gel), les lectures ci-dessous
        // sont synchrones (aucun saut de dispatcher), et le pilote SQLDelight natif est sûr entre
        // fils — l'app lit déjà cette base depuis `Dispatchers.Default`.
        let repository = repository
        let database = database
        let lastSeen = seenStore.lastSeenDates()
        let now = Date()
        let work = Task.detached(priority: .userInitiated) {
            try Self.details(
                events: events, viewerId: viewerId, lastSeen: lastSeen, now: now,
                repository: repository, database: database
            )
        }
        return try await withTaskCancellationHandler {
            try await work.value
        } onCancel: {
            work.cancel()
        }
    }

    func markNotificationRead(id: String) {
        database.notificationQueries.markAsRead(
            read_at: KotlinLong(value: Int64(Date().timeIntervalSince1970 * 1_000)),
            id: id
        )
    }

    // MARK: - Lecture (hors fil principal)

    private static func details(
        events: [HomeRawEvent],
        viewerId: String,
        lastSeen: [String: Date],
        now: Date,
        repository: DatabaseEventRepository,
        database: WakeveDb
    ) throws -> ActivitySnapshot {
        // Invitation en attente : membre (pas organisateur) dont le RSVP est PENDING, comme le hub.
        var rsvpPending = Set<String>()
        for event in events where !event.isOrganizer && !event.isPast && !event.readOnly {
            try Task.checkCancellation()
            let viewer = (repository.getParticipantRecords(eventId: event.id) ?? [])
                .map { ParticipantAccessMapper.shared.fromRepositoryRecord(record: $0) }
                .first { $0.userId == viewerId }
            if viewer?.role == .member && viewer?.rsvp == .pending {
                rsvpPending.insert(event.id)
            }
        }

        try Task.checkCancellation()
        let notifications = database.notificationQueries
            .getNotifications(user_id: viewerId, value_: 50)
            .executeAsList()
            .map { row in
                ActivityNotification(
                    id: row.id,
                    eventId: eventId(fromData: row.data_),
                    title: row.title,
                    message: row.body,
                    isRead: row.read_at != nil || row.is_read?.int64Value == 1,
                    date: Date(timeIntervalSince1970: Double(row.created_at) / 1_000)
                )
            }

        // Commentaires des autres dans la section générale, celle qu'ouvre la ligne messages : la borne
        // avance jusqu'à son propre dernier commentaire de la même section (le compte ne filtre pas
        // l'auteur ; on a vu le fil en y écrivant). Requêtes à une seule ligne : les statistiques par
        // participant regroupent aussi par nom d'auteur, et un auteur renommé y donnait plusieurs lignes
        // (exception Kotlin non rattrapable côté Swift).
        var newMessages: [String: Int] = [:]
        for event in events {
            try Task.checkCancellation()
            let own = database.commentQueries
                .selectLastCommentAtByAuthorInSection(event_id: event.id, author_id: viewerId, section: messagesSection)
                .executeAsOne()
                .lastCommentAt
            let since = messagesSinceISO(lastSeen: lastSeen[event.id], ownLastCommentISO: own, now: now)
            let count = database.commentQueries
                .countRecentActivityInSection(event_id: event.id, section: messagesSection, created_at: since)
                .executeAsOne()
                .int64Value
            if count > 0 { newMessages[event.id] = Int(count) }
        }

        return ActivitySnapshot(
            events: events, rsvpPendingEventIds: rsvpPending,
            notifications: notifications, newMessages: newMessages
        )
    }

    // MARK: - Règles pures (testées unitairement)

    /// Section comptée (`CommentSection.GENERAL.name`) : la ligne messages ouvre les commentaires généraux.
    static let messagesSection = "GENERAL"

    /// `eventId` du JSON `data` d'une notification (valeur textuelle ou numérique).
    static func eventId(fromData data: String?) -> String? {
        guard let data,
              let object = try? JSONSerialization.jsonObject(with: Data(data.utf8)) as? [String: Any],
              let value = object["eventId"] else { return nil }
        let text: String
        switch value {
        case let string as String: text = string
        case let number as NSNumber: text = number.stringValue
        default: return nil
        }
        return text.isEmpty ? nil : text
    }

    private static let isoFormatter: ISO8601DateFormatter = {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return formatter
    }()

    /// Borne stricte (`created_at > borne`, chaînes ISO UTC comparées comme en base) : dernière
    /// consultation, sinon 7 jours ; son propre commentaire plus récent la remplace, chaîne exacte
    /// conservée pour qu'il ne soit jamais compté.
    static func messagesSinceISO(lastSeen: Date?, ownLastCommentISO: String?, now: Date) -> String {
        let base = lastSeen ?? now.addingTimeInterval(-defaultLookback)
        if let own = ownLastCommentISO, let ownDate = HomeDateText.parseISO(own), ownDate >= base {
            return own
        }
        return isoFormatter.string(from: base)
    }
}
