import Foundation

/// Destination d'une action du hub (couche 4, #47).
enum EventHubRoute: Equatable {
    /// Écran existant de `homeTabContent`.
    case screen(AppView)
    /// Chemin « modifier un brouillon » de la bibliothèque (`editDraftFromHome`, rollout invitation).
    case editDraft
    /// Route participants du routeur invitation (`EventAudienceView`, rollout invitation).
    case invitationParticipants
    /// Sheet de module (couche 5a) ; l'écran legacy reste le repli plein écran.
    case sheet(HubModule)
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
        case .accommodation, .meals, .equipment, .activities, .photos: return .sheet(module)
        case .meetings: return .screen(.meetingList)
        case .payments: return .screen(.paymentPot)
        }
    }

    /// Modules convertis en sheet (couche 5a, #47).
    static let sheetModules: Set<HubModule> = [.meals, .equipment, .activities, .accommodation, .photos]

    /// Écran legacy d'un module en sheet (« Plein écran », ou refus d'accès affiché par son `case`).
    static func fullScreenFallback(for module: HubModule) -> AppView? {
        switch module {
        case .meals: return .mealPlanning
        case .equipment: return .equipmentChecklist
        case .activities: return .activityPlanning
        case .accommodation: return .accommodation
        case .photos: return .eventPhotos
        default: return nil
        }
    }

    /// Sheet seulement si la garde du `case` legacy (`canAccessDetailedPlanning`) est satisfaite ;
    /// sinon l'écran legacy, qui affiche son refus d'accès (comportement d'avant la couche 5a).
    static func sheetRoute(for module: HubModule, accessGranted: Bool) -> EventHubRoute? {
        if accessGranted { return .sheet(module) }
        return fullScreenFallback(for: module).map(EventHubRoute.screen)
    }

    /// Section de commentaires de l'écran legacy du module (même valeur que son `onOpenComments`).
    static func commentSection(for module: HubModule) -> CommentSectionType? {
        switch module {
        case .meals: return .meal
        case .equipment: return .equipment
        case .activities: return .activity
        case .accommodation: return .accommodation
        default: return nil
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
