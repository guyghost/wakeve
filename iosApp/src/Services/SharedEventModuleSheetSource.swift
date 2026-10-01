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
        let locale = WK.appLocale
        if let immediate = Self.immediateData(for: module, locale: locale) { return immediate }
        guard Self.readsDatabase(for: module) else { throw UnsupportedModule() }
        let reader = self
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
        case .budget:
            // Lecture seule du dépôt, comme la tuile : `BudgetViewModel.load()` créerait un budget absent.
            raw = .budget(BudgetRepository(db: database).getBudgetByEventId(eventId: event.id).map { budget in
                Self.rawBudget(
                    totalEstimated: budget.totalEstimated, totalActual: budget.totalActual,
                    transport: (budget.transportEstimated, budget.transportActual),
                    accommodation: (budget.accommodationEstimated, budget.accommodationActual),
                    meals: (budget.mealsEstimated, budget.mealsActual),
                    activities: (budget.activitiesEstimated, budget.activitiesActual),
                    equipment: (budget.equipmentEstimated, budget.equipmentActual),
                    other: (budget.otherEstimated, budget.otherActual)
                )
            })
        case .payments:
            // Mêmes lectures que `paymentPotSummaryValue` / `tricountSummaryValue` (ContentView).
            let pot = PaymentPotRepository(db: database).getActivePotForEvent(eventId: event.id)
            let readiness = TricountHandoffRepository(db: database).getPaymentReadiness(eventId: event.id)
            raw = .payments(
                pot: pot.map { pot in
                    HubModuleSheetRaw.PaymentPot(
                        title: pot.title, goalAmount: pot.goalAmount, currency: pot.currency, statusName: pot.status
                    )
                },
                tricount: HubModuleSheetData.tricountState(
                    explicitNotNeeded: readiness.handoff?.explicitNotNeeded,
                    complete: readiness.complete,
                    hasHandoff: readiness.handoff != nil
                )
            )
        case .meetings:
            // Même requête que la tuile Réunions du hub.
            raw = .meetings(database.meetingQueries.selectByEventId(eventId: event.id).executeAsList().map { meeting in
                HubModuleSheetRaw.Meeting(
                    id: meeting.id,
                    title: meeting.title,
                    startTime: meeting.startTime,
                    platformName: meeting.platform,
                    statusName: meeting.status,
                    hasLink: Self.hasMeetingLink(meeting.meetingLink)
                )
            })
        default:
            throw UnsupportedModule()
        }
        try Task.checkCancellation()

        return HubModuleSheetData.make(
            raw: raw, isOrganizer: isOrganizer, isReadOnly: isReadOnly, pendingSync: pendingSync, locale: locale
        )
    }

    // MARK: - Règles pures (testées unitairement)

    /// Modules sans liste à lire (photos : indice seulement) : servis sans lecture en base.
    static func immediateData(for module: HubModule, locale: Locale) -> HubModuleSheetData? {
        guard module == .photos else { return nil }
        return HubModuleSheetData.make(raw: .photos, isOrganizer: false, isReadOnly: false, pendingSync: false, locale: locale)
    }

    /// Lecture en base seulement pour un module routé en sheet (`EventHubRouting.sheetModules`), hors photos.
    static func readsDatabase(for module: HubModule) -> Bool {
        module != .photos && EventHubRouting.sheetModules.contains(module)
    }

    /// Budget (`Budget_`) converti, catégories dans l'ordre de `BudgetOverviewView`.
    static func rawBudget(
        totalEstimated: Double,
        totalActual: Double,
        transport: (Double, Double),
        accommodation: (Double, Double),
        meals: (Double, Double),
        activities: (Double, Double),
        equipment: (Double, Double),
        other: (Double, Double)
    ) -> HubModuleSheetRaw.Budget {
        let amounts: [(String, (Double, Double))] = [
            ("transport", transport), ("accommodation", accommodation), ("meals", meals),
            ("activities", activities), ("equipment", equipment), ("other", other)
        ]
        return HubModuleSheetRaw.Budget(
            totalEstimated: totalEstimated,
            totalActual: totalActual,
            categories: amounts.map { .init(key: $0.0, estimated: $0.1.0, actual: $0.1.1) }
        )
    }

    /// Lien de réunion généré : non vide une fois les espaces retirés.
    static func hasMeetingLink(_ link: String) -> Bool {
        !link.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

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
