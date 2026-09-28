import SwiftUI
import UIKit

/// Tokens de la refonte iOS 2026 (proposition Swarm DAO #47).
/// Source unique pour toute nouvelle vue. Aucune valeur de style en dur hors de ce fichier.
enum WK {

    // MARK: - Primitives

    static func uiColor(_ hex: UInt32, alpha: CGFloat = 1) -> UIColor {
        UIColor(
            red: CGFloat((hex >> 16) & 0xFF) / 255,
            green: CGFloat((hex >> 8) & 0xFF) / 255,
            blue: CGFloat(hex & 0xFF) / 255,
            alpha: alpha
        )
    }

    static func dynamic(light: UInt32, dark: UInt32) -> Color {
        Color(uiColor: UIColor { traits in
            traits.userInterfaceStyle == .dark ? uiColor(dark) : uiColor(light)
        })
    }

    static func shape(_ radius: CGFloat) -> RoundedRectangle {
        RoundedRectangle(cornerRadius: radius, style: .continuous)
    }

    // MARK: - Couleurs

    enum Colors {
        static let canvas = WK.dynamic(light: 0xF2F2F4, dark: 0x000000)
        static let card = WK.dynamic(light: 0xFFFFFF, dark: 0x1C1C1E)
        static let cardInset = WK.dynamic(light: 0xF6F6F8, dark: 0x2C2C2E)

        static let textPrimary = Color(uiColor: .label)
        static let textSecondary = Color(uiColor: .secondaryLabel)
        static let textTertiary = Color(uiColor: .tertiaryLabel)

        static let accent = WK.dynamic(light: 0x5B54D6, dark: 0x8B85F0)
        static let accentFill = WK.dynamic(light: 0xE7E5FB, dark: 0x2A2750)
        static let onAccentFill = WK.dynamic(light: 0x3F3A9E, dark: 0xC9C5FA)
        static let onAccent = WK.dynamic(light: 0xFFFFFF, dark: 0x1C1C1E)

        static let primaryButton = WK.dynamic(light: 0x1C1C1E, dark: 0xFFFFFF)
        static let onPrimaryButton = WK.dynamic(light: 0xFFFFFF, dark: 0x1C1C1E)
    }

    // MARK: - Statuts

    /// Couleur = signal d'état et d'action requise, jamais décoration.
    enum Status: CaseIterable {
        case confirmed, pending, actionNeeded, draft

        var color: Color {
            switch self {
            case .confirmed: return WK.dynamic(light: 0x2E9D5B, dark: 0x4CC07A)
            case .pending: return WK.dynamic(light: 0xB7791F, dark: 0xE0A33F)
            case .actionNeeded: return WK.dynamic(light: 0xD64545, dark: 0xEF6B6B)
            case .draft: return WK.dynamic(light: 0x8A8A8E, dark: 0x8E8E93)
            }
        }

        var fill: Color {
            switch self {
            case .confirmed: return WK.dynamic(light: 0xDFF3E6, dark: 0x173826)
            case .pending: return WK.dynamic(light: 0xFFF1D6, dark: 0x3D2E12)
            case .actionNeeded: return WK.dynamic(light: 0xFDE7E7, dark: 0x44191A)
            case .draft: return WK.dynamic(light: 0xF2F2F4, dark: 0x2C2C2E)
            }
        }

        var onFill: Color {
            switch self {
            case .confirmed: return WK.dynamic(light: 0x1E6B3C, dark: 0x7FD8A2)
            case .pending: return WK.dynamic(light: 0x8A5A0B, dark: 0xF3C774)
            case .actionNeeded: return WK.dynamic(light: 0xA12E2E, dark: 0xF59A9A)
            case .draft: return WK.dynamic(light: 0x5A5A5F, dark: 0xAEAEB2)
            }
        }
    }

    // MARK: - Géométrie

    enum Radius {
        static let sm: CGFloat = 12
        static let md: CGFloat = 18
        static let lg: CGFloat = 28
    }

    enum Space {
        static let xxs: CGFloat = 4
        static let xs: CGFloat = 8
        static let sm: CGFloat = 12
        static let md: CGFloat = 16
        static let lg: CGFloat = 24
        static let xl: CGFloat = 32
        static let screen: CGFloat = 16
    }

    enum Size {
        static let minTapTarget: CGFloat = 44
        static let avatar: CGFloat = 28
        static let primaryButtonHeight: CGFloat = 52
    }
}
