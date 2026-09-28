import Foundation

/// Données brutes nécessaires à l'accueil, fournies par une source injectable.
struct HomeRawEvent: Equatable {
    let id: String
    let title: String
    let statusName: String          // `EventStatus.name` Kotlin : "DRAFT", "POLLING", …
    let isOrganizer: Bool
    let isPast: Bool                // depuis LibraryCardProjection.temporalClass
    let deadlineISO: String
    let finalDateISO: String?
    let firstSlotStartISO: String?
    let userBallotComplete: Bool
    let votersWithCompleteBallot: Int
    let eligibleVoters: Int
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

    private let viewerId: String
    private let source: EventsHomeSource
    private let now: () -> Date

    init(viewerId: String, source: EventsHomeSource, now: @escaping () -> Date = Date.init) {
        self.viewerId = viewerId
        self.source = source
        self.now = now
    }

    func reload() async {
        do {
            let raw = try await source.loadEvents(viewerId: viewerId)
            let current = now()
            let facts = raw.map { Self.facts(from: $0) }
            let summaries = HomeEventSummary.sorted(facts.map { HomeEventSummary(facts: $0, now: current) })
            active = summaries.filter { !$0.isPast }
            past = summaries.filter(\.isPast)
            nextStep = HomeNextStep.pick(from: facts, now: current)
            pendingSyncCount = raw.filter(\.hasPendingSync).count
            state = raw.isEmpty ? .empty : .loaded
        } catch {
            state = active.isEmpty && past.isEmpty ? .failed : .loaded
        }
    }

    static func facts(from raw: HomeRawEvent) -> HomeEventFacts {
        let phase: HomeEventFacts.Phase
        switch raw.statusName {
        case "DRAFT": phase = .draft
        case "POLLING": phase = .polling
        case "COMPARING": phase = .comparing
        case "CONFIRMED": phase = .confirmed
        case "ORGANIZING": phase = .organizing
        default: phase = .finalized
        }
        return HomeEventFacts(
            id: raw.id, title: raw.title, phase: phase,
            role: raw.isOrganizer ? .organizer : .participant, isPast: raw.isPast,
            userBallotComplete: raw.userBallotComplete,
            votersWithCompleteBallot: raw.votersWithCompleteBallot, eligibleVoters: raw.eligibleVoters,
            deadline: HomeDateText.parseISO(raw.deadlineISO),
            eventDate: HomeDateText.parseISO(raw.finalDateISO) ?? HomeDateText.parseISO(raw.firstSlotStartISO),
            participantNames: raw.participantNames
        )
    }
}
