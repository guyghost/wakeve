import Foundation
import Shared

/// Source injectable du jour J (couche 8, #47).
protocol EventDaySource {
    func loadEventDay(eventId: String, viewerId: String, now: Date) async throws -> EventDayFacts
}

/// Source réelle du jour J : lit le schéma existant via les dépôts Kotlin `Shared`, hors du fil principal
/// (mêmes garanties de fil que `SharedEventHubSource`). Règles pures testées unitairement
/// (`EventDayRule`, `EventDayPlaceResolver`, `EventDayMeal`) ; l'accès base est vérifié sur simulateur.
struct SharedEventDaySource: EventDaySource {
    struct EventNotFound: Error {}

    private let repository: DatabaseEventRepository
    private let database: WakeveDb

    init(
        repository: DatabaseEventRepository = RepositoryProvider.shared.databaseRepository,
        database: WakeveDb = RepositoryProvider.shared.database
    ) {
        self.repository = repository
        self.database = database
    }

    func loadEventDay(eventId: String, viewerId: String, now: Date) async throws -> EventDayFacts {
        let reader = self
        let work = Task.detached(priority: .userInitiated) {
            try reader.facts(eventId: eventId, viewerId: viewerId, now: now)
        }
        return try await withTaskCancellationHandler {
            try await work.value
        } onCancel: {
            work.cancel()
        }
    }

    /// Créneau retenu : `confirmedDate` joint à `timeSlot` (début, fin, fuseau) et moment de la journée.
    static func retainedSlot(eventId: String, database: WakeveDb) -> RetainedSlot? {
        guard let row = database.confirmedDateQueries.selectWithTimeslotDetails(eventId: eventId).executeAsOneOrNull() else {
            return nil
        }
        let timeOfDay = database.timeSlotQueries.selectById(id: row.timeslotId).executeAsOneOrNull()?.timeOfDay
        return RetainedSlot(
            start: HomeDateText.parseISO(row.startTime),
            end: HomeDateText.parseISO(row.endTime),
            timeZoneIdentifier: row.timezone,
            timeOfDayName: timeOfDay
        )
    }

    // MARK: - Lecture (hors fil principal)

    private func facts(eventId: String, viewerId: String, now: Date) throws -> EventDayFacts {
        guard let event = repository.getEvent(id: eventId) else { throw EventNotFound() }
        try Task.checkCancellation()

        let records = repository.getParticipantRecords(eventId: event.id)
        let hasAccess = OrganizationDetailsAccess.isGranted(
            organizerId: event.organizerId, viewerId: viewerId, records: records
        )
        // Invités : même règle que le hub (refusés exclus, confirmés = accès aux détails).
        let guests = SharedEventHubSource.guestCounts(
            SharedEventHubSource.guestEntries(records: records, participantIds: event.participants, organizerId: event.organizerId)
        )
        let slot = Self.retainedSlot(eventId: event.id, database: database)
        try Task.checkCancellation()

        // Lieu : scénario retenu, sinon premier lieu potentiel (coordonnées du JSON des lieux potentiels).
        let scenario = ScenarioRepository(db: database).getSelectedScenario(eventId: event.id)
        let locations = database.potentialLocationQueries.selectByEventId(eventId: event.id).executeAsList()
        let place = EventDayPlaceResolver.resolve(
            selectedScenario: scenario.map {
                EventDayPlaceResolver.Scenario(location: $0.location, sourcePotentialLocationId: $0.sourcePotentialLocationId)
            },
            potentialLocations: locations.map {
                EventDayPlaceResolver.Location(id: $0.id, name: $0.name, address: $0.address, coordinatesJSON: $0.coordinates)
            }
        )
        try Task.checkCancellation()

        // Détails logistiques : seulement avec l'accès aux détails (mêmes gardes que les tuiles du hub).
        var transport: HubModuleSheetData.TransportState?
        var meals: [EventDayMeal] = []
        if hasAccess {
            let bridge = TransportRepositoryBridge(database: database)
            let planIds = bridge.getPlansByEvent(eventId: event.id).map(\.id)
            transport = HubModuleSheetData.transportState(
                planIds: planIds,
                selectedPlanId: bridge.getSelectedPlanId(eventId: event.id),
                notNeeded: database.transportQueries.selectTransportEventStatus(event_id: event.id)
                    .executeAsOneOrNull()?.transport_not_needed == 1
            )
            let zone = slot?.timeZone ?? .current
            let day = EventDayRule.dayString(now, timeZone: zone)
            meals = EventDayMeal.today(
                MealRepository(db: database).getMealsByDate(eventId: event.id, date: day).map {
                    EventDayMeal(name: $0.name, time: $0.time, statusName: $0.status.name)
                }
            )
        }

        return EventDayFacts(
            eventId: event.id,
            title: event.title,
            eventTypeName: event.eventType.name,
            phase: SharedEventHubSource.phase(statusName: event.status.name),
            hasAccess: hasAccess,
            slot: slot,
            place: place,
            transport: transport,
            meals: meals,
            confirmedCount: guests.confirmed,
            pendingCount: guests.pending
        )
    }
}
