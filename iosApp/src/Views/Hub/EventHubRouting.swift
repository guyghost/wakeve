import Foundation

/// Destination d'une action du hub (couche 4, #47).
enum EventHubRoute: Equatable {
    /// Écran existant de `homeTabContent`.
    case screen(AppView)
    /// Chemin « modifier un brouillon » de la bibliothèque (`editDraftFromHome`, rollout invitation).
    case editDraft
    /// Route participants du routeur invitation (`EventAudienceView`, rollout invitation).
    case invitationParticipants
}

/// Aiguillage pur des actions du hub selon le rollout invitation (`iosInvitationExperienceV1`).
/// Sans rollout, le routeur invitation et les écrans `.eventInformation`/`.eventAudience`/`.eventCreation`
/// retombent sur le détail, c'est-à-dire le hub lui-même : chaque action vise alors un écran legacy réel.
enum EventHubRouting {
    static func route(for module: HubModule, phase: EventHubFacts.Phase, invitationRollout: Bool) -> EventHubRoute {
        switch module {
        case .date:
            guard phase == .draft else { return .screen(.pollResults) }
            // Sans rollout, l'écran participants legacy porte « Ajouter des dates » (`DraftDatesSheet`).
            return invitationRollout ? .editDraft : .screen(.participantManagement)
        case .participants:
            return addParticipantsRoute(invitationRollout: invitationRollout)
        case .recap:
            // Sans rollout : la date retenue et les réponses de chacun (`PollResultsView`, sans garde).
            return .screen(invitationRollout ? .eventInformation : .pollResults)
        case .location, .scenarios: return .screen(.scenarioList)
        case .budget: return .screen(.budgetOverview)
        case .transport: return .screen(.transportPlanning)
        case .accommodation: return .screen(.accommodation)
        case .meals: return .screen(.mealPlanning)
        case .equipment: return .screen(.equipmentChecklist)
        case .activities: return .screen(.activityPlanning)
        case .meetings: return .screen(.meetingList)
        case .photos: return .screen(.eventPhotos)
        case .payments: return .screen(.paymentPot)
        }
    }

    /// Actions principales de navigation ; organiser/finaliser/se connecter sont confirmés dans le hub (nil).
    static func route(for primary: EventHubModel.Primary, invitationRollout: Bool) -> EventHubRoute? {
        switch primary {
        case .vote: return .screen(.pollVoting)
        case .pollResults, .confirmDate: return .screen(.pollResults)
        case .addDates: return route(for: .date, phase: .draft, invitationRollout: invitationRollout)
        case .organize, .finalize, .signInToFinalize, .none: return nil
        }
    }

    static func addParticipantsRoute(invitationRollout: Bool) -> EventHubRoute {
        invitationRollout ? .invitationParticipants : .screen(.participantManagement)
    }
}
