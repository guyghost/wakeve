import Foundation
import Shared

/// Aiguillage de la réouverture d'un brouillon sous la refonte (couche 7, #47).
///
/// Critère « brouillon du studio » : au moins une ligne `event_operation_receipt` pour l'événement.
/// Le studio d'invitation (`InvitationExperiencePublicContracts`, action `UPDATE_DRAFT_AGGREGATE`) en
/// écrit une à chaque enregistrement (`PENDING_SYNC`, puis `COMMITTED`) ; c'est le seul écrivain de
/// cette table. `CreateEvent` (flux, ancienne feuille) n'en écrit aucune. L'illustration ne convient
/// pas : `createEvent` crée aussi une ligne `event_artwork` (`NONE`) pour tout événement. Un brouillon
/// avec reçu refuse d'ailleurs `UpdateEvent` (`hasCurrentProtectedCommit` du dépôt).
enum CreateFlowEntry {
    enum DraftRoute: Equatable {
        /// Brouillon créé hors studio : flux 4 questions, étape de la première validation en échec.
        case createFlow
        /// Chemin actuel (studio si le rollout invitation est actif, sinon hub/détail).
        case existing
    }

    static func draftRoute(status: EventStatus?, hasInvitationReceipt: Bool) -> DraftRoute {
        status == .draft && !hasInvitationReceipt ? .createFlow : .existing
    }

    static func hasInvitationReceipt(eventId: String, database: WakeveDb) -> Bool {
        !database.invitationExperienceQueries
            .selectOperationReceiptsByEventId(event_id: eventId)
            .executeAsList()
            .isEmpty
    }
}
