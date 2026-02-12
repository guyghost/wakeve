import SwiftUI

// MARK: - Wakeve Tab Enum

/// Defines available tabs in Wakeve app
enum WakeveTab: String, CaseIterable, Identifiable {
    case home
    case inbox
    case explore

    var id: String { rawValue }

    var title: String {
        switch self {
        case .home: return "Accueil"
        case .inbox: return "Inbox"
        case .explore: return "Explorer"
        }
    }
}
