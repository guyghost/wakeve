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
        case .home: return "À venir"
        case .groups: return "Groupes"
        case .messages: return "Messages"
        case .profile: return "Profil"
        }
    }

    var systemImage: String {
        switch self {
        case .home: return "calendar"
        case .groups: return "person.2"
        case .messages: return "message"
        case .profile: return "person.crop.circle"
        }
    }
}
