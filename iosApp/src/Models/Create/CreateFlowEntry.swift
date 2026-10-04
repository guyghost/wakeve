import Foundation
import Shared

/// Aiguillage du flux de création de la refonte (couche 7, #47) : ＋ et réouverture d'un brouillon.
///
/// Critère « brouillon du studio » : au moins une ligne `event_operation_receipt` pour l'événement.
/// Le studio d'invitation (`InvitationExperiencePublicContracts`, action `UPDATE_DRAFT_AGGREGATE`) en
/// écrit une à chaque enregistrement (`PENDING_SYNC`, puis `COMMITTED`) ; c'est le seul écrivain de
/// cette table. `CreateEvent` (flux, ancienne feuille) n'en écrit aucune. L'illustration ne convient
/// pas : `createEvent` crée aussi une ligne `event_artwork` (`NONE`) pour tout événement. Un brouillon
/// avec reçu refuse d'ailleurs `UpdateEvent` (`hasCurrentProtectedCommit` du dépôt).
enum CreateFlowEntry {
    enum NewEventRoute: Equatable {
        /// Flux 4 questions (`CreateEventFlow`).
        case createFlow
        /// Studio d'invitation (`currentView = .eventCreation`), synchronisé par son outbox.
        case studio
    }

    /// ＋, « Créer » de l'accueil vide et lien profond `.eventCreate`. Décision du 2026-10-02 : le flux n'envoie pas encore les
    /// événements au serveur (lacune du code partagé, Swarm DAO #48) ; il ne remplace donc le point
    /// d'entrée que lorsque le rollout invitation est éteint. Rollout allumé : studio, comme avant la couche 7.
    static func newEventRoute(invitationRollout: Bool) -> NewEventRoute {
        invitationRollout ? .studio : .createFlow
    }

    enum DraftRoute: Equatable {
        /// Brouillon de sondage créé hors studio : flux 4 questions, étape de la première validation en échec.
        case createFlow
        /// Chemin actuel (studio si le rollout invitation est actif, sinon hub/détail).
        case existing
    }

    /// Seul un brouillon de sondage de créneaux (ce que le flux crée) se rouvre dans le flux ; un brouillon
    /// matrice garde son chemin.
    static func draftRoute(
        status: EventStatus?,
        planningMode: EventPlanningMode?,
        hasInvitationReceipt: Bool
    ) -> DraftRoute {
        status == .draft && planningMode == .timeSlotPoll && !hasInvitationReceipt ? .createFlow : .existing
    }

    static func hasInvitationReceipt(eventId: String, database: WakeveDb) -> Bool {
        !database.invitationExperienceQueries
            .selectOperationReceiptsByEventId(event_id: eventId)
            .executeAsList()
            .isEmpty
    }
}
