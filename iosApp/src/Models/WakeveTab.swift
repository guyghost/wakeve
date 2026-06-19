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
        case .home: return String(localized: "tab.upcoming")
        case .groups: return String(localized: "tab.explore")
        case .messages: return String(localized: "tab.messages")
        case .profile: return String(localized: "tab.profile")
        }
    }

    var systemImage: String {
        switch self {
        case .home: return "calendar"
        case .groups: return "sparkles"
        case .messages: return "message"
        case .profile: return "person.crop.circle"
        }
    }
}
