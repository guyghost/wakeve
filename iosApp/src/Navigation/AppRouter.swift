import Foundation
import Observation

/// Routeur du shell de la refonte (couche 2, proposition #47).
/// Décide si une route change de zone / présente une surface du shell,
/// ou si elle est déléguée à l'aiguillage d'événements existant.
@MainActor
@Observable
final class AppRouter {

    enum Plan: Equatable {
        case showEventsRoot
        case showActivity
        case presentProfile
        case presentSettings
        case delegateToEvents(IosRoute)
    }

    /// Surface présentée par le shell. Une seule à la fois : la feuille
    /// `.sheet(item:)` bascule d'elle-même quand la valeur change.
    enum Presentation: String, Identifiable {
        case profile, settings
        var id: String { rawValue }
    }

    var zone: AppZone = .events
    var presentation: Presentation?
    /// Filtre demandé par le dernier lien profond `wakeve://notifications` (couche 6) ;
    /// `activityFilterRequest` change à chaque demande, même quand le filtre est identique.
    private(set) var activityFilter: ActivityFilter = .toDo
    private(set) var activityFilterRequest = 0

    /// `filter=unread` → « Tout » (les non lues y figurent) ; sinon « À traiter ».
    nonisolated static func activityFilter(for filter: String?) -> ActivityFilter {
        filter == "unread" ? .all : .toDo
    }

    nonisolated static func plan(for route: IosRoute) -> Plan {
        switch route {
        case .topLevel(.home):
            return .showEventsRoot
        case .topLevel(.notifications):
            return .showActivity
        case .topLevel(.profile):
            return .presentProfile
        case .topLevel(.settings), .topLevel(.notificationPreferences):
            return .presentSettings
        default:
            return .delegateToEvents(route)
        }
    }

    /// Pré-aiguillage des deep links : le plan est appliqué au shell.
    /// - Returns: la route que l'aiguillage d'événements doit encore traiter, sinon `nil`.
    static func preRoute(_ route: IosRoute, router: AppRouter) -> IosRoute? {
        if case .topLevel(.notifications(let filter)) = route {
            router.activityFilter = activityFilter(for: filter)
            router.activityFilterRequest += 1
        }
        return router.apply(plan(for: route))
    }

    /// Applique le plan à l'état du shell.
    /// - Returns: la route que l'aiguillage d'événements doit encore traiter, sinon `nil`.
    @discardableResult
    func apply(_ plan: Plan) -> IosRoute? {
        switch plan {
        case .showActivity:
            dismissPresentations()
            zone = .activity
            return nil
        case .presentProfile:
            presentation = .profile
            return nil
        case .presentSettings:
            presentation = .settings
            return nil
        case .showEventsRoot:
            dismissPresentations()
            zone = .events
            return .topLevel(.home)
        case .delegateToEvents(let route):
            dismissPresentations()
            zone = .events
            return route
        }
    }

    private func dismissPresentations() {
        presentation = nil
    }
}
