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
