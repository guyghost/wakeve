import Foundation

/// Zones de premier niveau de la refonte (remplacera WakeveTab en couche 2).
enum AppZone: String, CaseIterable, Identifiable {
    case events
    case activity

    var id: String { rawValue }

    var title: String {
        switch self {
        case .events: return String(localized: "wk.nav.events")
        case .activity: return String(localized: "wk.nav.activity")
        }
    }

    var systemImage: String {
        switch self {
        case .events: return "calendar"
        case .activity: return "bell"
        }
    }
}
