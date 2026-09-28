import Foundation

/// Bulletins d'un sondage vus depuis la source (couche 3, #47).
/// Votants éligibles = participants ayant accepté + organisateur ; seuls les bulletins complets comptent.
/// « Autres » = votants éligibles hors organisateur (règle « prêt à confirmer »).
struct HomeBallotStats: Equatable {
    let userBallotComplete: Bool
    let votersWithCompleteBallot: Int
    let eligibleVoters: Int
    let otherVotersComplete: Int
    let otherEligibleVoters: Int

    static let none = HomeBallotStats(
        userBallotComplete: false, votersWithCompleteBallot: 0, eligibleVoters: 0,
        otherVotersComplete: 0, otherEligibleVoters: 0
    )
}

/// Faits calculés pour un événement, vus par l'utilisateur courant (couche 3, #47).
struct HomeEventFacts: Equatable {
    enum Phase: Equatable { case draft, polling, comparing, confirmed, organizing, finalized }
    enum Role: Equatable { case organizer, participant }

    let id: String
    let title: String
    let phase: Phase
    let role: Role
    let isOwner: Bool
    let isPast: Bool
    /// Politique d'interaction « lecture seule » de la projection (passé, finalisé, archivé).
    let readOnly: Bool
    /// Échéance future, ou absente.
    let pollOpen: Bool
    /// Invitation acceptée (toujours vrai pour l'organisateur).
    let viewerAccepted: Bool
    let ballots: HomeBallotStats
    let deadline: Date?
    let eventDate: Date?
    let participantNames: [String]

    var userBallotComplete: Bool { ballots.userBallotComplete }
    var votersWithCompleteBallot: Int { ballots.votersWithCompleteBallot }
    var eligibleVoters: Int { ballots.eligibleVoters }

    private var pollActionable: Bool { phase == .polling && !isPast && !readOnly }

    /// Organisateur : tous les autres votants éligibles (au moins un) ont un bulletin complet,
    /// que l'organisateur ait voté ou non.
    var readyToConfirm: Bool {
        pollActionable && role == .organizer
            && ballots.otherEligibleVoters > 0
            && ballots.otherVotersComplete >= ballots.otherEligibleVoters
    }

    /// Invitation acceptée, sondage ouvert, bulletin incomplet ; « prêt à confirmer » l'emporte.
    var voteRequired: Bool {
        pollActionable && pollOpen && viewerAccepted && !userBallotComplete && !readyToConfirm
    }
}

/// Résumé affichable d'une carte d'événement.
struct HomeEventSummary: Identifiable, Equatable {
    enum Label: Equatable { case key(String), date(Date) }

    let facts: HomeEventFacts
    let status: WK.Status
    let label: Label
    let sortRank: Int

    var id: String { facts.id }
    var isPast: Bool { facts.isPast }
    /// Seul le propriétaire peut supprimer, jamais un événement finalisé
    /// (même règle que `EventDetailViewModel.canDelete`).
    var canDelete: Bool { facts.isOwner && facts.phase != .finalized }

    init(facts: HomeEventFacts) {
        self.facts = facts
        if facts.isPast {
            status = .draft
            label = facts.eventDate.map(Label.date) ?? .key("home.v2.status.past")
            sortRank = 4
            return
        }
        switch facts.phase {
        case .draft:
            status = .draft; label = .key("home.v2.status.draft"); sortRank = 3
        case .polling:
            if facts.readyToConfirm {
                status = .actionNeeded; label = .key("home.v2.status.ready_to_confirm"); sortRank = 0
            } else if facts.voteRequired {
                status = .actionNeeded; label = .key("home.v2.status.vote_required"); sortRank = 0
            } else {
                status = .pending; label = .key("home.v2.status.polling"); sortRank = 1
            }
        case .comparing, .confirmed, .organizing, .finalized:
            if facts.role == .organizer && facts.phase != .finalized {
                status = .pending; label = .key("home.v2.status.organizing"); sortRank = 1
            } else {
                status = .confirmed
                label = facts.eventDate.map(Label.date) ?? .key("home.v2.status.confirmed")
                sortRank = 2
            }
        }
    }

    /// Tri : rang, puis échéance/date la plus proche, puis titre.
    static func sorted(_ items: [HomeEventSummary]) -> [HomeEventSummary] {
        items.sorted { a, b in
            if a.sortRank != b.sortRank { return a.sortRank < b.sortRank }
            let da = a.facts.deadline ?? a.facts.eventDate ?? .distantFuture
            let db = b.facts.deadline ?? b.facts.eventDate ?? .distantFuture
            if da != db { return da < db }
            return a.facts.title.localizedCompare(b.facts.title) == .orderedAscending
        }
    }
}

/// Carte « Prochaine étape » : l'action la plus urgente tous événements confondus.
struct HomeNextStep: Equatable {
    enum Kind: Equatable { case voteRequired, readyToConfirm, pollInProgress, organizing }
    enum Action: Equatable { case vote, pollResults, open }
    /// Grand chiffre : bulletins complets sur votants éligibles, jours avant l'événement, ou inconnu.
    enum Metric: Equatable { case votes(complete: Int, eligible: Int), days(Int), unknown }

    let eventId: String
    let title: String
    let kind: Kind
    let action: Action
    let metric: Metric
    let daysLeft: Int?

    var value: String {
        switch metric {
        case .votes(let complete, _): return String(complete)
        case .days(let days): return String(days)
        case .unknown: return "—"
        }
    }

    /// Unité non linguistique (« /8 ») ; l'unité des jours est localisée par la vue.
    var unit: String? {
        if case .votes(_, let eligible) = metric { return "/\(eligible)" }
        return nil
    }

    static func pick(from facts: [HomeEventFacts], now: Date) -> HomeNextStep? {
        let active = facts.filter { !$0.isPast }
        func soonest(_ items: [HomeEventFacts], by date: (HomeEventFacts) -> Date?) -> HomeEventFacts? {
            items.min { (date($0) ?? .distantFuture) < (date($1) ?? .distantFuture) }
        }
        if let f = soonest(active.filter(\.voteRequired), by: \.deadline) {
            return votes(f, kind: .voteRequired, action: .vote, now: now)
        }
        if let f = soonest(active.filter(\.readyToConfirm), by: \.deadline) {
            return HomeNextStep(
                eventId: f.id, title: f.title, kind: .readyToConfirm, action: .pollResults,
                metric: .votes(complete: f.ballots.otherVotersComplete, eligible: f.ballots.otherEligibleVoters),
                daysLeft: nil
            )
        }
        if let f = soonest(active.filter { $0.phase == .polling && $0.role == .organizer && !$0.readOnly }, by: \.deadline) {
            return votes(f, kind: .pollInProgress, action: .pollResults, now: now)
        }
        let organizing = active.filter {
            $0.role == .organizer && [.comparing, .confirmed, .organizing].contains($0.phase)
        }
        if let f = soonest(organizing, by: \.eventDate) {
            let days = f.eventDate.map { HomeDateText.daysBetween(now, $0) }
            return HomeNextStep(
                eventId: f.id, title: f.title, kind: .organizing, action: .open,
                metric: days.map(Metric.days) ?? .unknown, daysLeft: days
            )
        }
        return nil
    }

    private static func votes(_ f: HomeEventFacts, kind: Kind, action: Action, now: Date) -> HomeNextStep {
        HomeNextStep(
            eventId: f.id, title: f.title, kind: kind, action: action,
            metric: .votes(complete: f.votersWithCompleteBallot, eligible: f.eligibleVoters),
            daysLeft: f.pollOpen ? f.deadline.map { HomeDateText.daysBetween(now, $0) } : nil
        )
    }
}
