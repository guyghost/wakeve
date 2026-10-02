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
/// - lancement : échéance repoussée à 7 jours si besoin (`UpdateEvent` relu), puis `StartPoll` via
///   `EventPollStartController`.
/// Une étape inchangée n'écrit rien (pas de nouvelle révision).
///
/// Confirmations tardives : un intent sans toast dans le délai de garde n'est pas oublié. Les toasts
/// arrivent dans l'ordre des intents ; un toast n'en règle un autre que s'il est le sien (rang) ou si
/// l'effet attendu est déjà visible dans le dépôt. Une création expirée garde son identifiant : elle est
/// reprise si elle aboutit plus tard, et une nouvelle tentative réutilise le même identifiant (jamais de doublon).
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
    @Published private(set) var isSaving = false
    /// « Brouillon enregistré » : vrai après une reprise ou un enregistrement réussi, faux après un échec d'écriture.
    @Published private(set) var isDraftSaved = false

    /// Point d'injection des tests : reçoit l'envoi de chaque intent à la machine d'état et peut le
    /// différer (écriture lente simulée). nil : envoi immédiat.
    var intentGate: ((_ send: @escaping () -> Void) -> Void)?

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
    /// Intent en attente de règlement : rang d'envoi, continuation, effet attendu dans le dépôt.
    private struct PendingIntent {
        let rank: Int
        let continuation: CheckedContinuation<String?, Never>
        let isApplied: () -> Bool
    }

    private var pending: PendingIntent?
    private var dispatchedIntents = 0
    private var receivedToasts = 0
    private var toastTimeoutTask: Task<Void, Never>?
    /// Identifiant d'une création non confirmée : réutilisé (ou repris) à la tentative suivante.
    private var pendingCreationId: String?

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
                self?.receiveToast(message)
            }
        }
    }

    deinit {
        stateMachine.dispose()
    }

    /// Dépôt de la même base, sans `SyncManager` — comme `CreateEventViewModel`
    /// (`createEventStateMachine(database:)`). Avec le gestionnaire de synchro, chaque écriture attend
    /// `triggerSync()` (≈ 7 s de nouvelles tentatives quand le serveur est injoignable) avant son toast :
    /// « Continuer » resterait bloqué. Les événements du flux restent locaux, comme ceux de l'ancienne
    /// feuille : la ligne `syncMetadata` de création n'est jamais transportée (Swarm DAO #48).
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
        // Tous les créneaux sont repris, même sans date (début vide) : un enregistrement de Quand ? ne
        // doit rien supprimer en silence. Le fuseau d'origine est conservé.
        form.slots = event.proposedSlots.map { slot in
            CreateEventSlot(
                id: slot.id,
                input: EventTimeSlotInput(start: slot.start ?? "", end: slot.end, timeOfDay: slot.timeOfDay),
                timezone: slot.timezone
            )
        }
        self.eventId = eventId
        isDraftSaved = true
        return form
    }

    // MARK: - Sauvegarde

    func save(step: CreateEventFlowStep, form: CreateEventForm) async -> SaveResult {
        guard !isSaving else { return .failed(saveFailedMessage) }
        if let key = form.errors(for: step).values.sorted().first {
            return .failed(String(localized: String.LocalizationValue(key)))
        }
        isSaving = true
        defer { isSaving = false }

        // Création expirée puis aboutie entre-temps : reprise du brouillon, pas de doublon.
        if eventId == nil, let pendingCreationId, repository.getEvent(id: pendingCreationId) != nil {
            adoptCreation(pendingCreationId)
        }
        let succeeded: Bool
        if let eventId {
            succeeded = await update(eventId: eventId, step: step, form: form)
        } else {
            guard form.isValid(.what) else { return .failed(saveFailedMessage) }
            succeeded = await create(form: form)
        }
        guard succeeded else {
            isDraftSaved = false
            return .failed(saveFailedMessage)
        }
        isDraftSaved = true
        return .saved
    }

    private func adoptCreation(_ id: String) {
        eventId = id
        pendingCreationId = nil
    }

    private func create(form: CreateEventForm) async -> Bool {
        let iso8601 = ISO8601DateFormatter()
        let date = now()
        // Identifiant gardé d'une tentative à l'autre : une création expirée qui aboutit plus tard
        // fait échouer la nouvelle tentative (même clé) au lieu de créer un doublon.
        let id = pendingCreationId ?? "event-\(UUID().uuidString)"
        pendingCreationId = id
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
        let repository = repository
        _ = await perform(EventManagementContractIntentCreateEvent(event: event)) {
            repository.getEvent(id: id) != nil
        }
        guard repository.getEvent(id: id) != nil else { return false }
        adoptCreation(id)
        syncLocations(eventId: id, names: form.locations)
        // Brouillon d'une tentative précédente repris : l'étape 1 est remise à jour si elle a changé.
        return await update(eventId: id, step: .what, form: form)
    }

    private func update(eventId: String, step: CreateEventFlowStep, form: CreateEventForm) async -> Bool {
        guard let current = repository.getEvent(id: eventId), current.status == .draft else { return false }
        switch step {
        case .what:
            let title = CreateEventForm.trimmed(form.title)
            let description = CreateEventForm.trimmed(form.description)
            let custom = form.persistedEventTypeCustom
            let eventType = form.eventType
            let matches: (WakeveEvent) -> Bool = { event in
                event.title == title && event.description_ == description
                    && event.eventType == eventType && event.eventTypeCustom == custom
            }
            guard !matches(current) else { return true }
            return await write(
                EventManagementContractIntentUpdateEvent(event: Self.copy(
                    current, title: title, description: description, eventType: eventType, eventTypeCustom: .some(custom)
                )),
                eventId: eventId,
                until: matches
            )

        case .who:
            let wanted = Counts(min: form.minParticipants, expected: form.expectedParticipants, max: form.maxParticipants)
            let stored = Counts(current)
            guard wanted != stored else { return true }
            let intent: EventManagementContractIntent
            if wanted.clears(stored) {
                intent = EventManagementContractIntentUpdateEvent(event: Self.copy(current, counts: wanted))
            } else {
                intent = EventManagementContractIntentUpdateDraftEvent(
                    eventId: eventId,
                    eventType: nil,
                    eventTypeCustom: nil,
                    expectedParticipants: wanted.kotlin(wanted.expected),
                    minParticipants: wanted.kotlin(wanted.min),
                    maxParticipants: wanted.kotlin(wanted.max)
                )
            }
            return await write(intent, eventId: eventId) { Counts($0) == wanted }

        case .place:
            syncLocations(eventId: eventId, names: form.locations)
            let stored = storedLocations(eventId: eventId).map { CreateEventForm.trimmed($0.name) }
            return Set(stored) == Set(Self.locationNames(form.locations))

        case .time:
            // Ensembles comparés sans tenir compte de l'ordre (le dépôt trie par début).
            let wanted = form.slots.map(SlotKey.init).sorted()
            let matches: (WakeveEvent) -> Bool = { $0.proposedSlots.map(SlotKey.init).sorted() == wanted }
            guard !matches(current) else { return true }
            return await write(
                EventManagementContractIntentUpdateEvent(event: Self.copy(current, slots: Self.timeSlots(form.slots))),
                eventId: eventId,
                until: matches
            )
        }
    }

    /// Envoie l'intent puis vérifie l'effet dans le dépôt relu (jamais déduit du seul toast).
    private func write(
        _ intent: EventManagementContractIntent,
        eventId: String,
        until matches: @escaping (WakeveEvent) -> Bool
    ) async -> Bool {
        let repository = repository
        let isApplied = { repository.getEvent(id: eventId).map(matches) ?? false }
        _ = await perform(intent, isApplied: isApplied)
        return isApplied()
    }

    // MARK: - Lancement

    /// `StartPoll` ; succès quand le brouillon passe en `POLLING`. Un brouillon repris tard garde au moins
    /// `minimumVotingDays` jours de vote : l'échéance est repoussée avant le lancement.
    func launch() async -> LaunchResult {
        guard let eventId else { return .failed(saveFailedMessage) }
        guard await refreshDeadline(eventId: eventId) else { return .failed(saveFailedMessage) }
        let starter = EventPollStartController(eventId: eventId, userId: userId, repository: repository)
        let outcome = await withCheckedContinuation { (continuation: CheckedContinuation<EventPollStartController.Outcome, Never>) in
            starter.start { continuation.resume(returning: $0) }
        }
        withExtendedLifetime(starter) {}
        switch outcome {
        case .started:
            return .launched(eventId)
        case .failed(let message):
            return .failed(message)
        }
    }

    static let minimumVotingDays = 7
    /// Marge : un lancement juste après la création ne réécrit pas l'échéance (création + 7 jours).
    private static let deadlineTolerance: TimeInterval = 3_600

    /// Relit l'événement ; échéance à moins de 7 jours (ou passée) → `UpdateEvent` à maintenant + 7 jours.
    private func refreshDeadline(eventId: String) async -> Bool {
        guard let current = repository.getEvent(id: eventId), current.status == .draft else {
            return true // `StartPoll` signale lui-même un brouillon absent ou déjà lancé.
        }
        let date = now()
        let minimum = Calendar.current.date(byAdding: .day, value: Self.minimumVotingDays, to: date)
            ?? date.addingTimeInterval(Double(Self.minimumVotingDays) * 86_400)
        if let deadline = Self.parseDate(current.deadline),
           deadline >= minimum.addingTimeInterval(-Self.deadlineTolerance) {
            return true
        }
        let refreshed = ISO8601DateFormatter().string(from: minimum)
        return await write(
            EventManagementContractIntentUpdateEvent(event: Self.copy(current, deadline: refreshed)),
            eventId: eventId
        ) { $0.deadline == refreshed }
    }

    private static func parseDate(_ value: String) -> Date? {
        if let date = ISO8601DateFormatter().date(from: value) { return date }
        let fractional = ISO8601DateFormatter()
        fractional.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return fractional.date(from: value)
    }

    // MARK: - Machine d'état

    /// Envoie l'intent et attend son toast de règlement (ou le délai de garde). Le résultat est
    /// toujours vérifié ensuite dans le dépôt, jamais déduit du seul toast.
    private func perform(
        _ intent: EventManagementContractIntent,
        isApplied: @escaping () -> Bool
    ) async -> String? {
        dispatchedIntents += 1
        let rank = dispatchedIntents
        return await withCheckedContinuation { continuation in
            pending = PendingIntent(rank: rank, continuation: continuation, isApplied: isApplied)
            toastTimeoutTask = Task { [weak self, toastTimeout] in
                try? await Task.sleep(for: toastTimeout)
                guard !Task.isCancelled, self?.pending?.rank == rank else { return }
                self?.settle(nil)
            }
            let send: () -> Void = { [stateMachine] in stateMachine.dispatch(intent: intent) }
            if let intentGate {
                intentGate(send)
            } else {
                send()
            }
        }
    }

    /// Toast reçu : règle l'intent en attente s'il est le sien (rang atteint) ou si son effet est déjà
    /// visible. Le toast tardif d'un intent expiré est ignoré.
    private func receiveToast(_ message: String) {
        receivedToasts += 1
        guard let pending else { return }
        if receivedToasts >= pending.rank || pending.isApplied() {
            settle(message)
        }
    }

    private func settle(_ message: String?) {
        guard let pending else { return }
        self.pending = nil
        toastTimeoutTask?.cancel()
        toastTimeoutTask = nil
        pending.continuation.resume(returning: message)
    }

    // MARK: - Lieux (SQL)

    private func storedLocations(eventId: String) -> [PotentialLocation] {
        database.potentialLocationQueries.selectByEventId(eventId: eventId).executeAsList()
    }

    private static func locationNames(_ names: [String]) -> [String] {
        names.map(CreateEventForm.trimmed).filter { !$0.isEmpty }
    }

    /// Noms comparés à l'identique (après trim) : un changement de casse remplace la ligne. Les doublons
    /// insensibles à la casse sont refusés en amont par la validation.
    private func syncLocations(eventId: String, names: [String]) {
        let wanted = Self.locationNames(names)
        let stored = storedLocations(eventId: eventId)
        let wantedNames = Set(wanted)
        for row in stored where !wantedNames.contains(CreateEventForm.trimmed(row.name)) {
            database.potentialLocationQueries.deleteLocation(id: row.id)
        }
        let storedNames = Set(stored.map { CreateEventForm.trimmed($0.name) }.filter(wantedNames.contains))
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        let base = now()
        for (offset, name) in wanted.enumerated() where !storedNames.contains(name) {
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
        let timezone: String

        init(_ slot: CreateEventSlot) {
            id = slot.id
            start = slot.input.start
            end = slot.input.end ?? ""
            timeOfDay = slot.input.timeOfDay.name
            timezone = slot.timezone ?? TimeZone.current.identifier
        }

        init(_ slot: TimeSlot) {
            id = slot.id
            start = slot.start ?? ""
            end = slot.end ?? ""
            timeOfDay = slot.timeOfDay.name
            timezone = slot.timezone
        }

        static func < (lhs: SlotKey, rhs: SlotKey) -> Bool {
            (lhs.id, lhs.start, lhs.end, lhs.timeOfDay, lhs.timezone)
                < (rhs.id, rhs.start, rhs.end, rhs.timeOfDay, rhs.timezone)
        }
    }

    /// Début vide → `start` nil (créneau repris sans date) ; fuseau d'origine, sinon celui de l'appareil.
    private static func timeSlots(_ slots: [CreateEventSlot]) -> [TimeSlot] {
        slots.map { slot in
            TimeSlot(
                id: slot.id,
                start: slot.isDateless ? nil : slot.input.start,
                end: slot.input.end,
                timezone: slot.timezone ?? TimeZone.current.identifier,
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
        slots: [TimeSlot]? = nil,
        deadline: String? = nil
    ) -> WakeveEvent {
        WakeveEvent(
            id: draft.id,
            title: title ?? draft.title,
            description: description ?? draft.description_,
            organizerId: draft.organizerId,
            participants: draft.participants,
            proposedSlots: slots ?? draft.proposedSlots,
            deadline: deadline ?? draft.deadline,
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
