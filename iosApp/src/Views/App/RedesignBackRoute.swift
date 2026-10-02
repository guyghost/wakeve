import SwiftUI

/// Retour des écrans atteints depuis le hub qui n'ont pas de contrôle de retour propre (couche 4, #47).
/// La barre flottante disparaît hors racine : sans ce retour, l'utilisateur resterait bloqué.
/// Les `case` de `homeTabContent` restent inchangés.
enum RedesignBackRoute {
    enum Placement: Equatable {
        /// Rangée posée en inset haut par le shell : écrans sans pile de navigation interne poussée.
        case inset
        /// Bouton de barre d'outils lu depuis l'environnement (`redesignBackAction`) par l'écran lui-même :
        /// sa pile pousse un détail (dépenses, réunion) dont le retour système prend alors la même place.
        case toolbar
    }

    /// Écran parent, ou nil quand l'écran porte déjà son retour (ou n'en a pas besoin).
    /// - Parameter organizationAccess: garde `canAccessOrganizationDashboard` de l'événement ; sans elle,
    ///   budget/réunions/cagnotte/Tricount affichent `AccessDenied`, qui a déjà son bouton de retour.
    static func destination(from view: AppView, organizationAccess: Bool) -> AppView? {
        switch view {
        case .eventAudience:
            return .eventDetail
        case .leaderboard:
            // Lien profond `wakeve://leaderboard` (couche 9) : l'écran n'a pas de retour propre.
            return .eventList
        case .budgetOverview, .meetingList, .paymentPot:
            return organizationAccess ? .eventDetail : nil
        case .budgetDetail:
            return organizationAccess ? .budgetOverview : nil
        case .meetingDetail:
            return organizationAccess ? .meetingList : nil
        case .tricount:
            return organizationAccess ? .paymentPot : nil
        default:
            return nil
        }
    }

    /// Écrans dont le retour dépend de `canAccessOrganizationDashboard` : l'accès n'est calculé que pour eux.
    static func needsOrganizationAccess(_ view: AppView) -> Bool {
        switch view {
        case .budgetOverview, .budgetDetail, .meetingList, .meetingDetail, .paymentPot, .tricount: return true
        default: return false
        }
    }

    static func placement(for view: AppView) -> Placement {
        switch view {
        case .budgetOverview, .meetingList: return .toolbar
        default: return .inset
        }
    }
}

private struct RedesignBackActionKey: EnvironmentKey {
    static let defaultValue: (() -> Void)? = nil
}

extension EnvironmentValues {
    /// Retour vers le hub fourni par le shell ; nil quand l'écran porte son propre retour.
    var redesignBackAction: (() -> Void)? {
        get { self[RedesignBackActionKey.self] }
        set { self[RedesignBackActionKey.self] = newValue }
    }
}
