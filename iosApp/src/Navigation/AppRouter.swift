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

    var zone: AppZone = .events
    var isProfilePresented = false
    var isSettingsPresented = false

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
            isSettingsPresented = false
            isProfilePresented = true
            return nil
        case .presentSettings:
            isProfilePresented = false
            isSettingsPresented = true
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
        isProfilePresented = false
        isSettingsPresented = false
    }
}
