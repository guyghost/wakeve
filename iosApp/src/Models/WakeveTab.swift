import SwiftUI

// MARK: - Wakeve Tab Enum

/// Defines available tabs in Wakeve app
enum WakeveTab: String, CaseIterable, Identifiable {
    case home
    case groups
    case messages
    case profile

    var id: String { rawValue }

    var title: String {
        switch self {
        case .home: return "Accueil"
        case .groups: return "Groupes"
        case .messages: return "Messages"
        case .profile: return "Profil"
        }
    }

    var systemImage: String {
        switch self {
        case .home: return "house.fill"
        case .groups: return "person.2.fill"
        case .messages: return "message.fill"
        case .profile: return "person.crop.circle.fill"
        }
    }
}
