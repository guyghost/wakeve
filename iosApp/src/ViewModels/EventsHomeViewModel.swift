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
    /// Créneau retenu (couche 8), lu en organisation ou finalisé seulement.
    var retainedSlot: RetainedSlot? = nil
    /// Accès aux détails d'organisation (`OrganizationDetailsAccess`), lu avec le créneau retenu.
    var hasDetailsAccess: Bool = false
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
    /// Rollout invitation : un événement finalisé part aux archives et n'a plus de jour J (couche 8).
    private let invitationRollout: Bool
    /// Seul le dernier chargement lancé publie son résultat.
    private var generation = 0

    init(viewerId: String, source: EventsHomeSource, now: @escaping () -> Date = Date.init, invitationRollout: Bool = false) {
        self.viewerId = viewerId
        self.source = source
        self.now = now
        self.invitationRollout = invitationRollout
    }

    func reload() async {
        generation += 1
        let token = generation
        do {
            let raw = try await source.loadEvents(viewerId: viewerId)
            guard token == generation else { return }
            let current = now()
            let facts = raw.map { Self.facts(from: $0, now: current, invitationRollout: invitationRollout) }
            let summaries = HomeEventSummary.sorted(facts.map { HomeEventSummary(facts: $0) })
            active = summaries.filter { !$0.isPast }
            past = summaries.filter(\.isPast)
            nextStep = HomeNextStep.pick(from: facts, now: current)
            pendingSyncCount = raw.filter(\.hasPendingSync).count
            state = raw.isEmpty ? .empty : .loaded
        } catch {
            // Chargement annulé (vue quittée) : on garde l'état courant.
            guard token == generation, !(error is CancellationError) else { return }
            state = active.isEmpty && past.isEmpty ? .failed : .loaded
        }
    }

    static func facts(from raw: HomeRawEvent, now: Date, invitationRollout: Bool = false) -> HomeEventFacts {
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
            participantNames: raw.participantNames,
            isEventDay: EventDayRule.isEventDay(
                phase: Self.hubPhase(phase),
                invitationRollout: invitationRollout,
                finalDate: HomeDateText.parseISO(raw.finalDateISO),
                slotStart: raw.retainedSlot?.start,
                slotEnd: raw.retainedSlot?.end,
                timezone: raw.retainedSlot?.timeZoneIdentifier,
                hasAccess: raw.hasDetailsAccess,
                now: now
            )
        )
    }

    private static func hubPhase(_ phase: HomeEventFacts.Phase) -> EventHubFacts.Phase {
        switch phase {
        case .draft: return .draft
        case .polling: return .polling
        case .comparing: return .comparing
        case .confirmed: return .confirmed
        case .organizing: return .organizing
        case .finalized: return .finalized
        }
    }
}
