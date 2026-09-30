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
    /// `event.eventType.name` : teinte du hero (`EventMoodPalette.palette(for:)`), nil → palette par défaut.
    var eventTypeName: String? = nil
    /// Auteur signalé par « Signaler l'événement » (`ModerationActionTarget.authorId`).
    var organizerId: String? = nil

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
        // La carte propose le créneau en tête : sans lui, rien à afficher.
        showsQuickVote = facts.voteRequired && facts.slotCount >= 1 && facts.leadingSlotStart != nil

        let highlighted: HubModule?
        switch primary {
        case .vote, .pollResults, .confirmDate, .addDates: highlighted = .date
        default: highlighted = facts.phase == .comparing ? .scenarios : nil
        }
        let dateNeedsAction = facts.voteRequired || facts.readyToConfirm
        tiles = Self.modules(for: facts.phase).map { module in
            let isLocked = Self.isLocked(module, facts: facts)
            return Tile(
                module: module,
                isLocked: isLocked,
                // Une tuile verrouillée n'est jamais la prochaine étape.
                isHighlighted: module == highlighted && !isLocked,
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

    /// Modules dont la source lit un résumé : tuiles visibles et ouvertes (une tuile verrouillée ne lit rien).
    static func summarizedModules(for facts: EventHubFacts) -> [HubModule] {
        modules(for: facts.phase).filter { !isLocked($0, facts: facts) }
    }

    /// Tuile visible mais non actionnable quand la garde du `case` de destination (`homeTabContent`)
    /// afficherait « accès refusé », ou quand l'écran ouvert se verrouille lui-même :
    /// aucune tuile ouverte ne mène à un écran bloqué.
    static func isLocked(_ module: HubModule, facts: EventHubFacts) -> Bool {
        let access = facts.hasDetailsAccess
        switch module {
        case .date, .participants, .recap:
            // Sondage, participants, informations : écrans sans garde d'accès.
            return false
        case .location, .scenarios:
            // Liste des scénarios : sans garde de `case`, mais verrouillée dans l'écran
            // (`ScenarioOrganizationView.isLocked` : `!canAccessOrganizationDetails`).
            // En brouillon, seul l'organisateur (toujours autorisé) voit l'événement.
            return facts.phase != .draft && !access
        case .budget, .meetings, .payments:
            // `canAccessOrganizationDashboard` (et la même règle pour la cagnotte) : organisation ou finalisé.
            return !access || ![.organizing, .finalized].contains(facts.phase)
        case .transport:
            // `canAccessTransportPlanning` : confirmé, organisation ou finalisé (pas la comparaison).
            return !access || ![.confirmed, .organizing, .finalized].contains(facts.phase)
        case .accommodation, .meals, .equipment, .activities, .photos:
            // `canAccessDetailedPlanning` : confirmé, comparaison, organisation ou finalisé.
            return !access || ![.confirmed, .comparing, .organizing, .finalized].contains(facts.phase)
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
