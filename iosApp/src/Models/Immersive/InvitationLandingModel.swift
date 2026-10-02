import Foundation

/// Faits de l'invitation reçue (couche 8, #47) : faits du hub, organisateur et état de réponse du spectateur.
/// L'état de réponse est seulement affiché : aucune API client n'accepte ni ne décline encore une invitation.
struct InvitationLandingFacts: Equatable {
    enum Response: Equatable { case organizer, accepted, pending, declined }

    let hub: EventHubFacts
    /// Nom affichable de l'organisateur, nil s'il est inconnu.
    let organizerName: String?
    let response: Response

    /// L'organisateur organise ; sinon la réponse enregistrée (acceptée, déclinée), en attente par défaut.
    static func response(isOrganizer: Bool, accepted: Bool, declined: Bool) -> Response {
        if isOrganizer { return .organizer }
        if accepted { return .accepted }
        if declined { return .declined }
        return .pending
    }
}

/// Présentation pure de l'invitation reçue, localisée pour `locale`.
struct InvitationLandingModel: Equatable {
    enum Primary: Equatable { case vote, viewEvent }

    let title: String
    /// « Invitation de Léa » (formulation sans genre), « Invitation » si l'organisateur est inconnu.
    let caption: String
    /// Date retenue, ou « Vote en cours · N créneaux » ; nil en brouillon ou sans créneau.
    let when: String?
    let participants: String?
    let response: String
    let responseState: InvitationLandingFacts.Response
    let primary: Primary
    let primaryTitle: String
    let palette: EventMoodPalette

    init(facts: InvitationLandingFacts, locale: Locale = WK.appLocale) {
        func text(_ key: String) -> String { WK.localizedFormat(key, locale: locale) }
        let hub = facts.hub
        title = hub.title
        palette = EventMoodPalette.palette(for: hub.eventTypeName)

        let organizer = facts.organizerName?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        // L'organisateur qui ouvre son propre lien ne lit pas « Invitation de » son propre nom.
        caption = organizer.isEmpty || facts.response == .organizer
            ? text("immersive.invitation.caption")
            : String(format: text("immersive.invitation.caption_format"), organizer)

        switch hub.phase {
        case .draft:
            when = nil
        case .polling:
            when = hub.slotCount > 0
                ? String(format: text("immersive.invitation.polling_format"), HubSummaryText.slots(hub.slotCount, locale: locale))
                : nil
        case .confirmed, .comparing, .organizing, .finalized:
            when = hub.finalDate.map { HomeDateText.short($0, locale: locale) }
        }

        participants = hub.confirmedCount + hub.pendingCount > 0
            ? HubSummaryText.participants(confirmed: hub.confirmedCount, pending: hub.pendingCount, locale: locale)
            : nil

        responseState = facts.response
        switch facts.response {
        case .organizer: response = text("immersive.invitation.response.organizer")
        case .accepted: response = text("immersive.invitation.response.accepted")
        case .pending: response = text("immersive.invitation.response.pending")
        case .declined: response = text("immersive.invitation.response.declined")
        }

        // Mêmes règles que le hub : le vote quand il est attendu, sinon le hub.
        primary = EventHubModel(facts: hub).primary == .vote ? .vote : .viewEvent
        primaryTitle = primary == .vote ? text("hub.primary.vote") : text("immersive.view_event")
    }
}
