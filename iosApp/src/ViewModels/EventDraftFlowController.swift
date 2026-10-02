import Foundation
import SwiftUI
import Shared

/// Persistance étape par étape du flux de création de la refonte (couche 7, #47).
///
/// Chaque écriture passe par la machine d'état partagée (qui convertit les erreurs Kotlin en `ShowToast`)
/// ou par une requête SQL simple, puis le résultat est relu dans le dépôt :
/// - étape 1 (Quoi ?) : `CreateEvent` à la première sauvegarde, ensuite `UpdateEvent` avec l'événement **relu**
///   (révision d'agrégat à jour) pour le titre, la description et le type — une seule écriture, et
///   `UpdateDraftEvent` ne sait pas effacer le libellé d'un type personnalisé ;
/// - étape 2 (Qui ?) : `UpdateDraftEvent` pour les effectifs, `UpdateEvent` relu pour remettre une valeur à nil ;
/// - étape 3 (Où ?) : lieux synchronisés en SQL (`potentialLocationQueries`, comme `persistCreationContext` :
///   l'intent `AddPotentialLocation` ne persiste pas) ;
/// - étape 4 (Quand ?) : `UpdateEvent` relu (chemin `saveEvent`, qui synchronise les créneaux), identifiants
///   logiques des créneaux conservés ;
/// - lancement : `StartPoll` via `EventPollStartController`.
/// Une étape inchangée n'écrit rien (pas de nouvelle révision).
@MainActor
final class EventDraftFlowController: ObservableObject {
    enum SaveResult: Equatable {
        case saved
        case failed(String)
    }

    enum LaunchResult: Equatable {
        case launched(String)
        case failed(String)
    }

    @Published private(set) var eventId: String?
    @Published private(set) var lastSavedAt: Date?
    @Published private(set) var isSaving = false
    @Published private(set) var errorMessage: String?

    private let userId: String
    private let repository: EventRepositoryInterface
    private let database: WakeveDb
    private let now: () -> Date
    private let toastTimeout: Duration
    private let stateMachine: ObservableStateMachine<
        EventManagementContract.State,
        EventManagementContractIntent,
        EventManagementContractSideEffect
    >
    private var pendingToast: CheckedContinuation<String?, Never>?
    private var toastTimeoutTask: Task<Void, Never>?

    init(
        userId: String,
        repository: EventRepositoryInterface = EventDraftFlowController.localRepository(),
        database: WakeveDb = RepositoryProvider.shared.database,
        now: @escaping () -> Date = Date.init,
        toastTimeout: Duration = .seconds(10)
    ) {
        self.userId = userId
        self.repository = repository
        self.database = database
        self.now = now
        self.toastTimeout = toastTimeout
        self.stateMachine = IosFactory.shared.createEventStateMachine(
            database: database,
            eventRepository: repository
        )
        // Chaque intent utilisé se règle par un toast, en succès comme en échec ; les états sont
        // fusionnés et ne permettent pas d'observer un échec répété identique.
        stateMachine.onSideEffect = { [weak self] effect in
            guard let toast = effect as? EventManagementContractSideEffectShowToast else { return }
            let message = toast.message
            DispatchQueue.main.async {
                self?.settleToast(message)
            }
        }
    }

    deinit {
        stateMachine.dispose()
    }

    /// Dépôt de la même base, sans `SyncManager` — comme `CreateEventViewModel`
    /// (`createEventStateMachine(database:)`). Avec le gestionnaire de synchro, chaque écriture attend
    /// `triggerSync()` (≈ 7 s de nouvelles tentatives quand le serveur est injoignable) avant son toast :
    /// « Continuer » resterait bloqué. La création garde sa ligne `syncMetadata` (écrite dans la transaction).
    nonisolated static func localRepository() -> EventRepositoryInterface {
        DatabaseEventRepository(db: RepositoryProvider.shared.database, syncManager: nil)
    }

    // MARK: - Reprise

    /// Relit un brouillon (événement + lieux) pour reprendre le flux ; nil s'il n'existe plus.
    func hydrate(eventId: String) -> CreateEventForm? {
        guard let event = repository.getEvent(id: eventId) else { return nil }
        var form = CreateEventForm()
        form.title = event.title
        form.description = event.description_
        form.eventTypeName = event.eventType.name
        form.eventTypeCustom = event.eventTypeCustom ?? ""
        form.minParticipants = event.minParticipants.map { Int($0.intValue) }
        form.expectedParticipants = event.expectedParticipants.map { Int($0.intValue) }
        form.maxParticipants = event.maxParticipants.map { Int($0.intValue) }
        form.locations = storedLocations(eventId: eventId).map(\.name)
        form.slots = event.proposedSlots.compactMap { slot in
            guard let start = slot.start, !start.isEmpty else { return nil }
            return CreateEventSlot(id: slot.id, input: EventTimeSlotInput(
                start: start, end: slot.end, timeOfDay: slot.timeOfDay
            ))
        }
        self.eventId = eventId
        return form
    }

    // MARK: - Sauvegarde

    func save(step: CreateEventFlowStep, form: CreateEventForm) async -> SaveResult {
        guard !isSaving else { return .failed(saveFailedMessage) }
        if let key = form.errors(for: step).values.sorted().first {
            return fail(String(localized: String.LocalizationValue(key)))
        }
        isSaving = true
        defer { isSaving = false }

        let succeeded: Bool
        if let eventId {
            succeeded = await update(eventId: eventId, step: step, form: form)
        } else {
            guard form.isValid(.what) else { return fail(saveFailedMessage) }
            succeeded = await create(form: form)
        }
        guard succeeded else { return fail(saveFailedMessage) }
        errorMessage = nil
        lastSavedAt = now()
        return .saved
    }

    private func create(form: CreateEventForm) async -> Bool {
        let iso8601 = ISO8601DateFormatter()
        let date = now()
        let id = "event-\(Int(date.timeIntervalSince1970 * 1000))"
        let deadline = Calendar.current.date(byAdding: .day, value: 7, to: date) ?? date
        let event = WakeveEvent(
            id: id,
            title: CreateEventForm.trimmed(form.title),
            description: CreateEventForm.trimmed(form.description),
            organizerId: userId,
            participants: [],
            proposedSlots: Self.timeSlots(form.slots),
            deadline: iso8601.string(from: deadline),
            status: .draft,
            finalDate: nil,
            createdAt: iso8601.string(from: date),
            updatedAt: iso8601.string(from: date),
            eventType: form.eventType,
            eventTypeCustom: form.persistedEventTypeCustom,
            minParticipants: form.minParticipants.map { KotlinInt(value: Int32($0)) },
            maxParticipants: form.maxParticipants.map { KotlinInt(value: Int32($0)) },
            expectedParticipants: form.expectedParticipants.map { KotlinInt(value: Int32($0)) },
            heroImageUrl: nil,
            planningMode: .timeSlotPoll,
            aggregateRevision: 1,
            aggregateSchemaVersion: 1
        )
        _ = await perform(EventManagementContractIntentCreateEvent(event: event))
        guard repository.getEvent(id: id) != nil else { return false }
        eventId = id
        syncLocations(eventId: id, names: form.locations)
        return true
    }

    private func update(eventId: String, step: CreateEventFlowStep, form: CreateEventForm) async -> Bool {
        guard let current = repository.getEvent(id: eventId), current.status == .draft else { return false }
        switch step {
        case .what:
            let title = CreateEventForm.trimmed(form.title)
            let description = CreateEventForm.trimmed(form.description)
            let custom = form.persistedEventTypeCustom
            guard current.title != title || current.description_ != description
                || current.eventType != form.eventType || current.eventTypeCustom != custom else { return true }
            _ = await perform(EventManagementContractIntentUpdateEvent(event: Self.copy(
                current, title: title, description: description, eventType: form.eventType, eventTypeCustom: .some(custom)
            )))
            guard let saved = repository.getEvent(id: eventId) else { return false }
            return saved.title == title && saved.description_ == description
                && saved.eventType == form.eventType && saved.eventTypeCustom == custom

        case .who:
            let wanted = Counts(min: form.minParticipants, expected: form.expectedParticipants, max: form.maxParticipants)
            let stored = Counts(current)
            guard wanted != stored else { return true }
            if wanted.clears(stored) {
                _ = await perform(EventManagementContractIntentUpdateEvent(event: Self.copy(current, counts: wanted)))
            } else {
                _ = await perform(EventManagementContractIntentUpdateDraftEvent(
                    eventId: eventId,
                    eventType: nil,
                    eventTypeCustom: nil,
                    expectedParticipants: wanted.expected.map { KotlinInt(value: Int32($0)) },
                    minParticipants: wanted.min.map { KotlinInt(value: Int32($0)) },
                    maxParticipants: wanted.max.map { KotlinInt(value: Int32($0)) }
                ))
            }
            return repository.getEvent(id: eventId).map(Counts.init) == wanted

        case .place:
            syncLocations(eventId: eventId, names: form.locations)
            let stored = storedLocations(eventId: eventId).map { CreateEventForm.locationKey($0.name) }
            return Set(stored) == Set(form.locations.map(CreateEventForm.locationKey))

        case .time:
            let wanted = form.slots.map(SlotKey.init)
            guard current.proposedSlots.map(SlotKey.init) != wanted else { return true }
            _ = await perform(EventManagementContractIntentUpdateEvent(
                event: Self.copy(current, slots: Self.timeSlots(form.slots))
            ))
            return repository.getEvent(id: eventId)?.proposedSlots.map(SlotKey.init).sorted() == wanted.sorted()
        }
    }

    // MARK: - Lancement

    /// `StartPoll` ; succès quand le brouillon passe en `POLLING`.
    func launch() async -> LaunchResult {
        guard let eventId else { return .failed(saveFailedMessage) }
        let starter = EventPollStartController(eventId: eventId, userId: userId, repository: repository)
        let outcome = await withCheckedContinuation { (continuation: CheckedContinuation<EventPollStartController.Outcome, Never>) in
            starter.start { continuation.resume(returning: $0) }
        }
        withExtendedLifetime(starter) {}
        switch outcome {
        case .started:
            errorMessage = nil
            return .launched(eventId)
        case .failed(let message):
            errorMessage = message
            return .failed(message)
        }
    }

    // MARK: - Machine d'état

    /// Envoie l'intent et attend son toast de règlement (ou le délai de garde). Le résultat est
    /// toujours vérifié ensuite dans le dépôt, jamais déduit du seul toast.
    private func perform(_ intent: EventManagementContractIntent) async -> String? {
        await withCheckedContinuation { continuation in
            pendingToast = continuation
            toastTimeoutTask = Task { [weak self, toastTimeout] in
                try? await Task.sleep(for: toastTimeout)
                guard !Task.isCancelled else { return }
                self?.settleToast(nil)
            }
            stateMachine.dispatch(intent: intent)
        }
    }

    private func settleToast(_ message: String?) {
        guard let continuation = pendingToast else { return }
        pendingToast = nil
        toastTimeoutTask?.cancel()
        toastTimeoutTask = nil
        continuation.resume(returning: message)
    }

    // MARK: - Lieux (SQL)

    private func storedLocations(eventId: String) -> [PotentialLocation] {
        database.potentialLocationQueries.selectByEventId(eventId: eventId).executeAsList()
    }

    private func syncLocations(eventId: String, names: [String]) {
        let wanted = names.map(CreateEventForm.trimmed).filter { !$0.isEmpty }
        let stored = storedLocations(eventId: eventId)
        let wantedKeys = Set(wanted.map(CreateEventForm.locationKey))
        for row in stored where !wantedKeys.contains(CreateEventForm.locationKey(row.name)) {
            database.potentialLocationQueries.deleteLocation(id: row.id)
        }
        let storedKeys = Set(stored.map { CreateEventForm.locationKey($0.name) })
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        let base = now()
        for (offset, name) in wanted.enumerated() where !storedKeys.contains(CreateEventForm.locationKey(name)) {
            // Horodatages distincts : `selectByEventId` trie par date de création.
            database.potentialLocationQueries.insertLocation(
                id: "location-\(UUID().uuidString.prefix(8).lowercased())",
                eventId: eventId,
                name: name,
                locationType: "SPECIFIC_VENUE",
                address: nil,
                coordinates: nil,
                createdAt: formatter.string(from: base.addingTimeInterval(Double(offset) / 1000))
            )
        }
    }

    // MARK: - Outils

    private var saveFailedMessage: String { String(localized: "create_flow.error.save_failed") }

    private func fail(_ message: String) -> SaveResult {
        errorMessage = message
        return .failed(message)
    }

    private struct Counts: Equatable {
        var min: Int?
        var expected: Int?
        var max: Int?

        init(min: Int?, expected: Int?, max: Int?) {
            self.min = min
            self.expected = expected
            self.max = max
        }

        init(_ event: WakeveEvent) {
            min = event.minParticipants.map { Int($0.intValue) }
            expected = event.expectedParticipants.map { Int($0.intValue) }
            max = event.maxParticipants.map { Int($0.intValue) }
        }

        /// `UpdateDraftEvent` ignore les valeurs nil : effacer une valeur passe par `UpdateEvent`.
        func clears(_ stored: Counts) -> Bool {
            (min == nil && stored.min != nil) || (expected == nil && stored.expected != nil)
                || (max == nil && stored.max != nil)
        }

        func kotlin(_ value: Int?) -> KotlinInt? { value.map { KotlinInt(value: Int32($0)) } }
    }

    private struct SlotKey: Equatable, Comparable {
        let id: String
        let start: String
        let end: String
        let timeOfDay: String

        init(_ slot: CreateEventSlot) {
            id = slot.id
            start = slot.input.start
            end = slot.input.end ?? ""
            timeOfDay = slot.input.timeOfDay.name
        }

        init(_ slot: TimeSlot) {
            id = slot.id
            start = slot.start ?? ""
            end = slot.end ?? ""
            timeOfDay = slot.timeOfDay.name
        }

        static func < (lhs: SlotKey, rhs: SlotKey) -> Bool { lhs.id < rhs.id }
    }

    private static func timeSlots(_ slots: [CreateEventSlot]) -> [TimeSlot] {
        slots.map { slot in
            TimeSlot(
                id: slot.id,
                start: slot.input.start,
                end: slot.input.end,
                timezone: TimeZone.current.identifier,
                timeOfDay: slot.input.timeOfDay
            )
        }
    }

    /// Le `copy` des data classes Kotlin n'est pas exposé avec ses valeurs par défaut à Swift :
    /// reconstruction champ par champ depuis l'événement relu (révision d'agrégat comprise).
    private static func copy(
        _ draft: WakeveEvent,
        title: String? = nil,
        description: String? = nil,
        eventType: Shared.EventType? = nil,
        eventTypeCustom: String?? = nil,
        counts: Counts? = nil,
        slots: [TimeSlot]? = nil
    ) -> WakeveEvent {
        WakeveEvent(
            id: draft.id,
            title: title ?? draft.title,
            description: description ?? draft.description_,
            organizerId: draft.organizerId,
            participants: draft.participants,
            proposedSlots: slots ?? draft.proposedSlots,
            deadline: draft.deadline,
            status: draft.status,
            finalDate: draft.finalDate,
            createdAt: draft.createdAt,
            updatedAt: draft.updatedAt,
            eventType: eventType ?? draft.eventType,
            eventTypeCustom: eventTypeCustom ?? draft.eventTypeCustom,
            minParticipants: counts.map { $0.kotlin($0.min) } ?? draft.minParticipants,
            maxParticipants: counts.map { $0.kotlin($0.max) } ?? draft.maxParticipants,
            expectedParticipants: counts.map { $0.kotlin($0.expected) } ?? draft.expectedParticipants,
            heroImageUrl: draft.heroImageUrl,
            planningMode: draft.planningMode,
            aggregateRevision: draft.aggregateRevision,
            aggregateSchemaVersion: draft.aggregateSchemaVersion
        )
    }
}
