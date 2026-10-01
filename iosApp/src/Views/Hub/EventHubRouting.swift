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
        // `sheetModules` est la seule liste des modules en sheet.
        if sheetModules.contains(module) { return .sheet(module) }
        switch module {
        case .date:
            guard phase == .draft else { return .screen(.pollResults) }
            // Sans rollout, l'écran participants legacy porte « Ajouter des dates » (`DraftDatesSheet`).
            return invitationRollout ? .editDraft : .screen(.participantManagement)
        case .participants:
            // Retiré de `sheetModules` : retour à la route d'ajout.
            return addParticipantsRoute(invitationRollout: invitationRollout)
        case .recap:
            // Sans rollout : la date retenue et les réponses de chacun (`PollResultsView`, sans garde).
            return .screen(invitationRollout ? .eventInformation : .pollResults)
        case .location, .scenarios: return .screen(.scenarioList)
        case .transport, .accommodation, .meals, .equipment, .activities, .photos, .budget, .meetings, .payments:
            // Retiré de `sheetModules` : retour à son écran legacy.
            return .screen(fullScreenFallback(for: module) ?? .eventDetail)
        }
    }

    /// Modules convertis en sheet (couche 5a, puis budget, cagnotte et réunions en 5b, transport et invités en 5c, #47).
    /// Date, Lieu, Scénarios et Récap restent des écrans pleins (vote, résultats, comparaison).
    static let sheetModules: Set<HubModule> = [
        .meals, .equipment, .activities, .accommodation, .photos, .budget, .payments, .meetings, .transport, .participants
    ]

    /// Garde d'accès du `case` legacy de chaque module, appliquée avant de présenter sa sheet.
    enum SheetGuard: Equatable {
        /// `canAccessDetailedPlanning` (repas, matériel, activités, hébergement, photos).
        case detailedPlanning
        /// `canAccessOrganizationDashboard` (budget, réunions ; cagnotte : même règle écrite en ligne).
        case organizationDashboard
        /// `canAccessTransportPlanning` (confirmé, organisation, finalisé).
        case transportPlanning
        /// Aucune garde : la tuile Invités n'est jamais verrouillée.
        case unguarded
    }

    static func sheetGuard(for module: HubModule) -> SheetGuard {
        switch module {
        case .budget, .payments, .meetings: return .organizationDashboard
        case .transport: return .transportPlanning
        case .participants: return .unguarded
        default: return .detailedPlanning
        }
    }

    /// Écran legacy ouvert par l'action principale d'une sheet ; nil : action menée dans la sheet (repas).
    static func fallback(for primary: HubModuleSheetData.Primary) -> AppView? {
        switch primary {
        case .addMeal: return nil
        case .viewExpenses: return .budgetOverview
        case .managePot: return .paymentPot
        case .planMeeting: return .meetingList
        case .organizeTransport: return .transportPlanning
        // « Inviter » suit la route d'ajout, sensible au flag invitations (`fullScreenRoute`).
        case .invite: return nil
        }
    }

    /// Écran legacy d'un module en sheet (« Plein écran », ou refus d'accès affiché par son `case`).
    static func fullScreenFallback(for module: HubModule) -> AppView? {
        switch module {
        case .meals: return .mealPlanning
        case .equipment: return .equipmentChecklist
        case .activities: return .activityPlanning
        case .accommodation: return .accommodation
        case .photos: return .eventPhotos
        case .budget: return .budgetOverview
        case .payments: return .paymentPot
        case .meetings: return .meetingList
        case .transport: return .transportPlanning
        default: return nil
        }
    }

    /// Route plein écran d'un module en sheet : invités → route d'ajout (participants legacy sans rollout,
    /// audience du routeur d'invitations avec) ; autres modules → leur écran legacy.
    static func fullScreenRoute(for module: HubModule, invitationRollout: Bool) -> EventHubRoute? {
        if module == .participants { return addParticipantsRoute(invitationRollout: invitationRollout) }
        return fullScreenFallback(for: module).map(EventHubRoute.screen)
    }

    /// Sheet seulement si la garde du `case` legacy (`sheetGuard(for:)`) est satisfaite ;
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
        case .transport: return .transport
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
