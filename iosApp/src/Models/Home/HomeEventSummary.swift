import Foundation

/// Faits calculés pour un événement, vus par l'utilisateur courant (couche 3, #47).
struct HomeEventFacts: Equatable {
    enum Phase: Equatable { case draft, polling, comparing, confirmed, organizing, finalized }
    enum Role: Equatable { case organizer, participant }

    let id: String
    let title: String
    let phase: Phase
    let role: Role
    let isPast: Bool
    let userBallotComplete: Bool
    let votersWithCompleteBallot: Int
    let eligibleVoters: Int
    let deadline: Date?
    let eventDate: Date?
    let participantNames: [String]

    var everyoneVoted: Bool { eligibleVoters > 0 && votersWithCompleteBallot >= eligibleVoters }
    var voteRequired: Bool { phase == .polling && !isPast && !userBallotComplete }
    var readyToConfirm: Bool { phase == .polling && !isPast && role == .organizer && everyoneVoted }
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

    init(facts: HomeEventFacts, now: Date) {
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
            if facts.voteRequired {
                status = .actionNeeded; label = .key("home.v2.status.vote_required"); sortRank = 0
            } else if facts.readyToConfirm {
                status = .actionNeeded; label = .key("home.v2.status.ready_to_confirm"); sortRank = 0
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

    let eventId: String
    let title: String
    let kind: Kind
    let action: Action
    let value: String
    let unit: String?
    let daysLeft: Int?

    static func pick(from facts: [HomeEventFacts], now: Date) -> HomeNextStep? {
        let active = facts.filter { !$0.isPast }
        func soonest(_ items: [HomeEventFacts], by date: (HomeEventFacts) -> Date?) -> HomeEventFacts? {
            items.min { (date($0) ?? .distantFuture) < (date($1) ?? .distantFuture) }
        }
        if let f = soonest(active.filter(\.voteRequired), by: \.deadline) {
            return votes(f, kind: .voteRequired, action: .vote, now: now)
        }
        if let f = soonest(active.filter(\.readyToConfirm), by: \.deadline) {
            return votes(f, kind: .readyToConfirm, action: .pollResults, now: now)
        }
        if let f = soonest(active.filter { $0.phase == .polling && $0.role == .organizer }, by: \.deadline) {
            return votes(f, kind: .pollInProgress, action: .pollResults, now: now)
        }
        let organizing = active.filter {
            $0.role == .organizer && [.comparing, .confirmed, .organizing].contains($0.phase)
        }
        if let f = soonest(organizing, by: \.eventDate) {
            let days = f.eventDate.map { HomeDateText.daysBetween(now, $0) }
            return HomeNextStep(
                eventId: f.id, title: f.title, kind: .organizing, action: .open,
                value: days.map(String.init) ?? "—", unit: days == nil ? nil : "j", daysLeft: days
            )
        }
        return nil
    }

    private static func votes(_ f: HomeEventFacts, kind: Kind, action: Action, now: Date) -> HomeNextStep {
        HomeNextStep(
            eventId: f.id, title: f.title, kind: kind, action: action,
            value: String(f.votersWithCompleteBallot), unit: "/\(f.eligibleVoters)",
            daysLeft: f.deadline.map { HomeDateText.daysBetween(now, $0) }
        )
    }
}
