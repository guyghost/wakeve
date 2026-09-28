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

    /// Forme des boutons texte : rectangle continu de rayon `Radius.lg` (28 pt). C'est une capsule tant que
    /// la hauteur reste ≤ 56 pt (le rayon est plafonné à la demi-hauteur ; ≈ AX1 sur une ligne), puis un
    /// rectangle arrondi continu au-delà (grandes tailles, titre sur plusieurs lignes) pour ne pas rogner le texte.
    static var pill: RoundedRectangle { shape(Radius.lg) }

    // MARK: - Localisation

    /// Langue effective de l'app (celle que résout `String(localized:)`), avec la région de l'utilisateur
    /// (formats de nombres et listes).
    static var appLocale: Locale {
        let lang = Bundle.main.preferredLocalizations.first ?? "en"
        return Locale(languageComponents: .init(languageCode: .init(lang), region: Locale.current.region))
    }

    /// Format localisé (strings ou stringsdict) pour la langue de `locale`, indépendamment de la langue de l'app.
    static func localizedFormat(_ key: String, locale: Locale) -> String {
        let bundle = locale.language.languageCode
            .flatMap { Bundle.main.path(forResource: $0.identifier, ofType: "lproj") }
            .flatMap(Bundle.init(path:)) ?? .main
        return bundle.localizedString(forKey: key, value: nil, table: nil)
    }

    // MARK: - Couleurs

    enum Colors {
        static let canvas = WK.dynamic(light: 0xF2F2F4, dark: 0x000000)
        static let card = WK.dynamic(light: 0xFFFFFF, dark: 0x1C1C1E)
        static let cardInset = WK.dynamic(light: 0xF6F6F8, dark: 0x2C2C2E)
        static let onCardInset = WK.dynamic(light: 0x5A5A5F, dark: 0xAEAEB2)

        static let textPrimary = Color(uiColor: .label)
        static let textSecondary = Color(uiColor: .secondaryLabel)
        static let textTertiary = Color(uiColor: .tertiaryLabel)
        /// Texte secondaire opaque garantissant 4.5:1 sur card, cardInset et canvas (clair et sombre).
        static let textMuted = WK.dynamic(light: 0x6C6C70, dark: 0xAEAEB2)

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

        /// Nom localisé du statut : l'état n'est jamais porté par la couleur seule.
        var localizedName: String {
            switch self {
            case .confirmed: return String(localized: "wk.status.confirmed")
            case .pending: return String(localized: "wk.status.pending")
            case .actionNeeded: return String(localized: "wk.status.action_needed")
            case .draft: return String(localized: "wk.status.draft")
            }
        }

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
        static let xxxs: CGFloat = 2
        static let xxs: CGFloat = 4
        static let xs: CGFloat = 8
        static let sm: CGFloat = 12
        static let md: CGFloat = 16
        static let lg: CGFloat = 24
        static let xl: CGFloat = 32
        static let screen: CGFloat = 16
    }

    enum Stroke {
        static let hairline: CGFloat = 1
        static let emphasis: CGFloat = 1.5
    }

    enum Size {
        static let minTapTarget: CGFloat = 44
        static let avatar: CGFloat = 28
        static let primaryButtonHeight: CGFloat = 52
        /// Hauteur minimale de la pastille de compteur (badge de la barre flottante).
        static let badge: CGFloat = 16
    }

    // MARK: - Typographie (toujours indexée sur Dynamic Type)

    enum Typo {
        static let display = Font.system(.largeTitle, design: .rounded).weight(.semibold)
        static let title = Font.system(.title2).weight(.semibold)
        static let headline = Font.headline
        static let body = Font.body
        static let caption = Font.footnote
        static let micro = Font.caption
    }

    // MARK: - Mouvement

    enum Motion {
        static let reducedFade = Animation.easeInOut(duration: 0.15)

        static func snappy(reduceMotion: Bool) -> Animation {
            reduceMotion ? reducedFade : .snappy(duration: 0.25)
        }

        static func smooth(reduceMotion: Bool) -> Animation {
            reduceMotion ? reducedFade : .smooth(duration: 0.35)
        }

        static func bouncy(reduceMotion: Bool) -> Animation {
            reduceMotion ? reducedFade : .bouncy(duration: 0.4)
        }
    }

    // MARK: - Mode immersif

    /// Ambiance sombre teintée par la palette de l'événement (invitation, jour J).
    struct Mood {
        let background: Color
        let surface: Color
        let textPrimary: Color
        let textSecondary: Color
        let pillStroke: Color
        let accent: Color

        init(palette: EventMoodPalette) {
            let tint = UIColor(palette.primary(for: .dark))
            background = Color(uiColor: WK.darkened(tint, towardsBlackBy: 0.72))
            surface = Color(uiColor: WK.uiColor(0xFFFFFF, alpha: 0.08))
            textPrimary = Color(uiColor: WK.uiColor(0xF2F7F6))
            textSecondary = Color(uiColor: WK.uiColor(0xB8C4C2))
            pillStroke = Color(uiColor: WK.uiColor(0xFFFFFF, alpha: 0.25))
            accent = palette.accent(for: .dark)
        }
    }

    static func darkened(_ color: UIColor, towardsBlackBy amount: CGFloat) -> UIColor {
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        color.getRed(&r, green: &g, blue: &b, alpha: &a)
        let keep = 1 - amount
        return UIColor(red: r * keep, green: g * keep, blue: b * keep, alpha: 1)
    }
}

extension View {
    /// Identifiant d'accessibilité stable (spec §7), appliqué uniquement s'il est fourni.
    @ViewBuilder
    func wkAccessibilityID(_ id: String?) -> some View {
        if let id {
            accessibilityIdentifier(id)
        } else {
            self
        }
    }
}
