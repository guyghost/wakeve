import Foundation
import Shared

/// Source injectable du contenu d'une sheet de module du hub (couche 5a, #47).
protocol EventModuleSheetSource {
    func load(module: HubModule, eventId: String, viewerId: String) async throws -> HubModuleSheetData
}

/// Source réelle des sheets de modules (couche 5a, #47) : lit les listes via les dépôts Kotlin `Shared`,
/// hors du fil principal, les convertit en entrées Swift simples puis applique `HubModuleSheetData.make`.
/// Mêmes dépôts et mêmes règles que les écrans legacy (`EventSecondaryRouteViews.swift`) ;
/// l'accès base est vérifié sur simulateur, les conversions pures sont testées unitairement.
struct SharedEventModuleSheetSource: EventModuleSheetSource {
    struct EventNotFound: Error {}
    struct UnsupportedModule: Error {}

    private let repository: DatabaseEventRepository
    private let database: WakeveDb

    init(
        repository: DatabaseEventRepository = RepositoryProvider.shared.databaseRepository,
        database: WakeveDb = RepositoryProvider.shared.database
    ) {
        self.repository = repository
        self.database = database
    }

    func load(module: HubModule, eventId: String, viewerId: String) async throws -> HubModuleSheetData {
        // Envoyer ces objets Kotlin non `Sendable` vers un autre fil est sûr : Kotlin 2.2 utilise le
        // modèle mémoire moderne (objets partageables entre fils, pas de gel), les lectures ci-dessous
        // sont synchrones (aucun saut de dispatcher), et le pilote SQLDelight natif est sûr entre
        // fils — l'app lit déjà cette base depuis `Dispatchers.Default`.
        let reader = self
        let locale = WK.appLocale
        let work = Task.detached(priority: .userInitiated) {
            try reader.data(module: module, eventId: eventId, viewerId: viewerId, locale: locale)
        }
        return try await withTaskCancellationHandler {
            try await work.value
        } onCancel: {
            work.cancel()
        }
    }

    // MARK: - Lecture (hors fil principal)

    private func data(module: HubModule, eventId: String, viewerId: String, locale: Locale) throws -> HubModuleSheetData {
        guard let event = repository.getEvent(id: eventId) else { throw EventNotFound() }
        try Task.checkCancellation()

        // Mêmes règles que les `case` legacy : `event.organizerId == userId`, `isFinalizedOrganizationState`,
        // et la file d'envoi du flux (`hasPendingSync`).
        let isOrganizer = event.organizerId == viewerId
        let isReadOnly = Self.isReadOnly(statusName: event.status.name)
        let pendingSync = !repository.getWorkflowOutbox(eventId: event.id).isEmpty
        var names = NameCache { id in
            database.userQueries.selectUserById(id: id).executeAsOneOrNull()?.name
        }

        let raw: HubModuleSheetRaw
        switch module {
        case .meals:
            raw = .meals(MealRepository(db: database).getMealsByEventId(eventId: event.id).map { meal in
                HubModuleSheetRaw.Meal(
                    id: meal.id,
                    name: meal.name,
                    date: meal.date,
                    time: meal.time,
                    servings: Int(meal.servings),
                    statusName: meal.status.name,
                    responsibleNames: names.names(for: meal.responsibleParticipantIds)
                )
            })
        case .equipment:
            raw = .equipment(EquipmentRepository(db: database).getEquipmentItemsByEventId(eventId: event.id).map { item in
                HubModuleSheetRaw.EquipmentItem(
                    id: item.id,
                    name: item.name,
                    quantity: Int(item.quantity),
                    statusName: item.status.name,
                    assigneeName: names.name(for: item.assignedTo)
                )
            })
        case .activities:
            raw = .activities(ActivityRepository(db: database).getActivitiesByEventId(eventId: event.id).map { activity in
                HubModuleSheetRaw.Activity(
                    id: activity.id,
                    name: activity.name,
                    date: activity.date,
                    time: activity.time,
                    location: activity.location,
                    registeredNames: names.names(for: activity.registeredParticipantIds)
                )
            })
        case .accommodation:
            raw = .accommodation(AccommodationRepository(db: database).getAccommodationsByEventId(eventId: event.id).map { option in
                HubModuleSheetRaw.Accommodation(
                    id: option.id,
                    name: option.name,
                    pricePerNightCents: option.pricePerNight,
                    capacity: Int(option.capacity),
                    bookingStatusName: option.bookingStatus.name
                )
            })
        case .photos:
            raw = .photos
        default:
            throw UnsupportedModule()
        }
        try Task.checkCancellation()

        return HubModuleSheetData.make(
            raw: raw, isOrganizer: isOrganizer, isReadOnly: isReadOnly, pendingSync: pendingSync, locale: locale
        )
    }

    // MARK: - Règles pures (testées unitairement)

    /// Même règle que `isFinalizedOrganizationState` (ContentView) : seul un événement finalisé est en lecture seule.
    static func isReadOnly(statusName: String) -> Bool {
        statusName == "FINALIZED"
    }

    /// Noms d'affichage lus une seule fois par chargement ; nom absent ou vide → identifiant.
    struct NameCache {
        private let lookup: (String) -> String?
        private var cache: [String: String] = [:]

        init(lookup: @escaping (String) -> String?) {
            self.lookup = lookup
        }

        mutating func name(for id: String?) -> String? {
            guard let id, !id.isEmpty else { return nil }
            if let cached = cache[id] { return cached }
            let trimmed = lookup(id)?.trimmingCharacters(in: .whitespacesAndNewlines)
            let resolved = (trimmed?.isEmpty ?? true) ? id : trimmed!
            cache[id] = resolved
            return resolved
        }

        mutating func names(for ids: [String]) -> [String] {
            ids.compactMap { name(for: $0) }
        }
    }
}
