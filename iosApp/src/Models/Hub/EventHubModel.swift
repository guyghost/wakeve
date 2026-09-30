import Foundation

/// Modules affichables dans le hub d'un événement (couche 4, #47).
enum HubModule: String, CaseIterable, Equatable {
    case date, location, participants, budget, scenarios
    case transport, accommodation, meals, equipment, activities, meetings
    case recap, photos, payments
}

/// Faits sur un événement, vus par l'utilisateur courant (couche 4, #47).
/// Calculés par `EventHubSource` hors du fil principal ; aucune lecture en base ici.
struct EventHubFacts: Equatable {
    enum Phase: Equatable { case draft, polling, comparing, confirmed, organizing, finalized }

    let id: String
    let title: String
    let phase: Phase
    let isOrganizer: Bool
    /// Organisateur ou invitation acceptée.
    let viewerAccepted: Bool
    /// Règle existante `canAccessOrganizationDetails` (organisateur → vrai).
    let hasDetailsAccess: Bool
    let isLocalGuest: Bool
    /// Échéance strictement future, ou absente.
    let pollOpen: Bool
    let userBallotComplete: Bool
    /// Faux si le sondage n'a pas pu être lu : ni appel à voter, ni « prêt à confirmer ».
    let ballotsKnown: Bool
    let votersWithCompleteBallot: Int
    let eligibleVoters: Int
    let otherEligibleVoters: Int
    let otherVotersComplete: Int
    let slotCount: Int
    /// Créneau en tête (PollLogic), nil si aucun vote.
    let leadingSlotStart: Date?
    let finalDate: Date?
    let confirmedCount: Int
    let pendingCount: Int
    let participantNames: [String]
    /// Résumés d'une ligne déjà localisés par la source (absent → indice statique).
    let summaries: [HubModule: String]

    /// Même règle que l'accueil (`PollReadiness.readyToConfirm`), limitée au sondage.
    var readyToConfirm: Bool {
        phase == .polling && PollReadiness.readyToConfirm(
            isOrganizer: isOrganizer, pollOpen: pollOpen, ballotsKnown: ballotsKnown,
            votersWithCompleteBallot: votersWithCompleteBallot,
            otherVotersComplete: otherVotersComplete, otherEligibleVoters: otherEligibleVoters
        )
    }

    /// Invitation acceptée, sondage ouvert et lisible, bulletin incomplet ;
    /// « prêt à confirmer » l'emporte (même priorité que `HomeEventFacts.voteRequired`).
    var voteRequired: Bool {
        phase == .polling && viewerAccepted && pollOpen && ballotsKnown && !userBallotComplete && !readyToConfirm
    }
}

/// Présentation pure du hub : statut, tuiles de modules, action principale.
struct EventHubModel: Equatable {
    enum Primary: Equatable { case vote, pollResults, confirmDate, organize, finalize, signInToFinalize, addDates, none }

    struct Tile: Equatable {
        let module: HubModule
        let isLocked: Bool
        let isHighlighted: Bool
        let status: WK.Status?
    }

    let status: WK.Status
    let statusKey: String
    let tiles: [Tile]
    let primary: Primary
    let showsQuickVote: Bool

    init(facts: EventHubFacts) {
        (status, statusKey) = Self.status(for: facts)
        let primary = Self.primary(for: facts)
        self.primary = primary
        showsQuickVote = facts.voteRequired && facts.slotCount >= 1

        let highlighted: HubModule?
        switch primary {
        case .vote, .pollResults, .confirmDate, .addDates: highlighted = .date
        default: highlighted = facts.phase == .comparing ? .scenarios : nil
        }
        let dateNeedsAction = facts.voteRequired || facts.readyToConfirm
        tiles = Self.modules(for: facts.phase).map { module in
            Tile(
                module: module,
                isLocked: Self.isLocked(module, facts: facts),
                isHighlighted: module == highlighted,
                status: module == .date && dateNeedsAction ? .actionNeeded : nil
            )
        }
    }

    static func modules(for phase: EventHubFacts.Phase) -> [HubModule] {
        switch phase {
        case .draft: return [.date, .location, .participants]
        case .polling: return [.date, .location, .participants, .budget]
        case .confirmed, .comparing: return [.date, .scenarios, .participants, .budget]
        case .organizing: return [.transport, .accommodation, .meals, .equipment, .activities, .budget, .meetings]
        case .finalized: return [.recap, .photos, .payments]
        }
    }

    /// Tuile visible mais non actionnable quand les règles d'accès existantes refusent l'écran.
    static func isLocked(_ module: HubModule, facts: EventHubFacts) -> Bool {
        let access = facts.hasDetailsAccess
        let organizationPhase = facts.phase == .organizing || facts.phase == .finalized
        switch module {
        case .date, .location, .participants, .scenarios, .recap:
            return false
        case .transport:
            // Même règle que `canAccessTransportPlanning(for:)` : confirmé, organisation ou finalisé.
            let transportPhase = facts.phase == .confirmed || organizationPhase
            return !transportPhase || !access
        case .accommodation, .meals, .equipment, .activities, .photos:
            return !access
        case .budget:
            // Sondage/confirmé (et comparaison) : estimation consultable par l'organisateur et les acceptés.
            if [.polling, .confirmed, .comparing].contains(facts.phase) {
                return !(facts.isOrganizer || facts.viewerAccepted)
            }
            return !access || !organizationPhase
        case .meetings, .payments:
            return !access || !organizationPhase
        }
    }

    private static func status(for facts: EventHubFacts) -> (WK.Status, String) {
        switch facts.phase {
        case .draft:
            return (.draft, "home.v2.status.draft")
        case .polling:
            if facts.voteRequired { return (.actionNeeded, "home.v2.status.vote_required") }
            if facts.readyToConfirm { return (.actionNeeded, "home.v2.status.ready_to_confirm") }
            return (.pending, "home.v2.status.polling")
        case .comparing, .confirmed, .organizing:
            return facts.isOrganizer
                ? (.pending, "home.v2.status.organizing")
                : (.confirmed, "home.v2.status.confirmed")
        case .finalized:
            return (.confirmed, "home.v2.status.confirmed")
        }
    }

    private static func primary(for facts: EventHubFacts) -> Primary {
        switch facts.phase {
        case .draft:
            guard facts.isOrganizer else { return .none }
            // Le lancement du sondage reste dans le flux de création.
            return facts.slotCount == 0 ? .addDates : .none
        case .polling:
            if facts.voteRequired { return .vote }
            if facts.readyToConfirm { return .confirmDate }
            if facts.isOrganizer { return .pollResults }
            if facts.viewerAccepted && facts.ballotsKnown && facts.userBallotComplete { return .pollResults }
            return .none
        case .confirmed:
            return facts.isOrganizer ? .organize : .none
        case .organizing:
            guard facts.isOrganizer else { return .none }
            return facts.isLocalGuest ? .signInToFinalize : .finalize
        case .comparing, .finalized:
            return .none
        }
    }
}
