import Foundation

/// Données brutes nécessaires à l'accueil, fournies par une source injectable.
struct HomeRawEvent: Equatable {
    let id: String
    let title: String
    let statusName: String          // `EventStatus.name` Kotlin : "DRAFT", "POLLING", …
    let isOrganizer: Bool
    let isOwner: Bool               // `event.organizerId == viewerId`
    let isPast: Bool                // depuis LibraryCardProjection.temporalClass (voir `keepsActive`)
    let readOnly: Bool              // `SharedEventsHomeSource.isReadOnly` (finalisé ou passé non actif)
    let viewerAccepted: Bool        // RSVP accepté (toujours vrai pour l'organisateur)
    let deadlineISO: String
    let finalDateISO: String?
    let earliestSlotStartISO: String?
    let ballots: HomeBallotStats
    let participantNames: [String]
    let hasPendingSync: Bool
}

protocol EventsHomeSource {
    func loadEvents(viewerId: String) async throws -> [HomeRawEvent]
}

@MainActor
final class EventsHomeViewModel: ObservableObject {
    enum State: Equatable { case loading, empty, loaded, failed }

    @Published private(set) var state: State = .loading
    @Published private(set) var active: [HomeEventSummary] = []
    @Published private(set) var past: [HomeEventSummary] = []
    @Published private(set) var nextStep: HomeNextStep?
    @Published private(set) var pendingSyncCount = 0

    /// Invitation à créer un événement : aucun événement actif (même s'il en existe de passés).
    var showsCreateCTA: Bool {
        state == .empty || (state == .loaded && active.isEmpty)
    }

    private let viewerId: String
    private let source: EventsHomeSource
    private let now: () -> Date
    /// Seul le dernier chargement lancé publie son résultat.
    private var generation = 0

    init(viewerId: String, source: EventsHomeSource, now: @escaping () -> Date = Date.init) {
        self.viewerId = viewerId
        self.source = source
        self.now = now
    }

    func reload() async {
        generation += 1
        let token = generation
        do {
            let raw = try await source.loadEvents(viewerId: viewerId)
            guard token == generation else { return }
            let current = now()
            let facts = raw.map { Self.facts(from: $0, now: current) }
            let summaries = HomeEventSummary.sorted(facts.map { HomeEventSummary(facts: $0) })
            active = summaries.filter { !$0.isPast }
            past = summaries.filter(\.isPast)
            nextStep = HomeNextStep.pick(from: facts, now: current)
            pendingSyncCount = raw.filter(\.hasPendingSync).count
            state = raw.isEmpty ? .empty : .loaded
        } catch {
            guard token == generation else { return }
            state = active.isEmpty && past.isEmpty ? .failed : .loaded
        }
    }

    static func facts(from raw: HomeRawEvent, now: Date) -> HomeEventFacts {
        let phase: HomeEventFacts.Phase
        switch raw.statusName {
        case "DRAFT": phase = .draft
        case "POLLING": phase = .polling
        case "COMPARING": phase = .comparing
        case "CONFIRMED": phase = .confirmed
        case "ORGANIZING": phase = .organizing
        default: phase = .finalized
        }
        let deadline = HomeDateText.parseISO(raw.deadlineISO)
        // Événement daté : date finale uniquement ; sondage/brouillon : plus tôt des créneaux (tri).
        let eventDate: Date?
        switch phase {
        case .draft, .polling: eventDate = HomeDateText.parseISO(raw.earliestSlotStartISO)
        default: eventDate = HomeDateText.parseISO(raw.finalDateISO)
        }
        return HomeEventFacts(
            id: raw.id, title: raw.title, phase: phase,
            role: raw.isOrganizer ? .organizer : .participant, isOwner: raw.isOwner,
            isPast: raw.isPast, readOnly: raw.readOnly,
            pollOpen: deadline.map { $0 > now } ?? true,
            viewerAccepted: raw.isOrganizer || raw.viewerAccepted,
            ballots: raw.ballots,
            deadline: deadline, eventDate: eventDate,
            participantNames: raw.participantNames
        )
    }
}
