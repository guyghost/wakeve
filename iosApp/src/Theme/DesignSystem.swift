import SwiftUI
#if canImport(UIKit)
import UIKit
#endif

// MARK: - Wakeve Theme

/// Central iOS design tokens for Wakeve.
///
/// These tokens capture the Apple Invites-inspired direction used by the iOS app:
/// dark immersive pages, expressive event gradients, large rounded glass surfaces,
/// circular controls, and capsule actions.
public enum WakeveTheme {
    public enum ColorToken {
        public static let appDark = Color(hex: "111114")
        public static let appDarkElevated = Color(hex: "1C1C1F")
        public static let appDarkCard = Color.white.opacity(0.075)
        public static let appLight = Color(hex: "F7F4F1")
        public static let eventNight = Color(hex: "061B4F")
        public static let eventNightElevated = Color(hex: "071A3E")
        public static let eventLilacAction = Color(hex: "F6D8FF")
        public static let eventLilacText = Color(hex: "1C0B24")
        public static let permissionBlue = Color(hex: "3F8FF2")
        public static let neutralCapsule = Color.white.opacity(0.18)
        public static let neutralCapsuleDark = Color(hex: "5F6066")
        public static let cardStroke = Color.white.opacity(0.14)
        public static let cardStrokeLight = Color.black.opacity(0.08)
        public static let mutedText = Color.white.opacity(0.62)
        public static let mutedTextLight = Color(hex: "636674")
        public static let appLightElevated = Color.white
        public static let appLightControl = Color.black.opacity(0.06)
        public static let profileWarmTop = Color(hex: "F47C27")
        public static let profileWarmMid = Color(hex: "8B4312")
        public static let profileWarmBottom = Color(hex: "171719")
        public static let searchFieldDark = Color(hex: "34343A")
        public static let searchFieldLight = Color.black.opacity(0.06)
        public static let graphite = Color(hex: "17191D")
        public static let midnight = Color(hex: "071421")
        public static let midnightElevated = Color(hex: "101E2A")
        public static let softIvory = Color(hex: "F7F3EC")
        public static let mutedLavender = Color(hex: "B8A8D9")
        public static let paleBlue = Color(hex: "A9C7E8")
        public static let warmAmber = Color(hex: "F3B45B")
        public static let confirmationBase = Color(hex: "7CCFA8")
        public static let progressBase = Color(hex: "8BBBE8")
        public static let destructiveBase = Color(hex: "E34D5C")
        public static let skeletonDark = Color.white.opacity(0.085)
        public static let skeletonLight = Color.black.opacity(0.055)

        public static func pageBackground(for colorScheme: ColorScheme) -> Color {
            colorScheme == .dark ? midnight : softIvory
        }

        public static func primaryText(for colorScheme: ColorScheme) -> Color {
            colorScheme == .dark ? .white : Color(hex: "17171F")
        }

        public static func secondaryText(for colorScheme: ColorScheme) -> Color {
            colorScheme == .dark ? mutedText : mutedTextLight
        }

        public static func cardFill(for colorScheme: ColorScheme) -> Color {
            colorScheme == .dark ? midnightElevated : appLightElevated
        }

        public static func subtleCardFill(for colorScheme: ColorScheme) -> Color {
            colorScheme == .dark ? appDarkCard : Color.white.opacity(0.84)
        }

        public static func controlFill(for colorScheme: ColorScheme) -> Color {
            colorScheme == .dark ? Color.white.opacity(0.1) : appLightControl
        }

        public static func separator(for colorScheme: ColorScheme) -> Color {
            colorScheme == .dark ? Color.white.opacity(0.12) : Color.black.opacity(0.08)
        }

        public static func cardBorder(for colorScheme: ColorScheme) -> Color {
            colorScheme == .dark ? cardStroke : cardStrokeLight
        }

        public static func secondaryBackground(for colorScheme: ColorScheme) -> Color {
            colorScheme == .dark ? graphite.opacity(0.92) : Color.white.opacity(0.92)
        }

        public static func accent(for colorScheme: ColorScheme) -> Color {
            colorScheme == .dark ? paleBlue : Color(hex: "2F6F9F")
        }

        public static func progress(for colorScheme: ColorScheme) -> Color {
            colorScheme == .dark ? progressBase : Color(hex: "2E78A6")
        }

        public static func confirmation(for colorScheme: ColorScheme) -> Color {
            colorScheme == .dark ? confirmationBase : Color(hex: "287A52")
        }

        public static func destructive(for colorScheme: ColorScheme) -> Color {
            colorScheme == .dark ? Color(hex: "FF7A86") : destructiveBase
        }

        public static func glassTint(for colorScheme: ColorScheme) -> Color {
            colorScheme == .dark ? Color.white.opacity(0.14) : Color.white.opacity(0.72)
        }

        public static func skeletonFill(for colorScheme: ColorScheme) -> Color {
            colorScheme == .dark ? skeletonDark : skeletonLight
        }

        public static func eventHighlight(for colorScheme: ColorScheme) -> Color {
            colorScheme == .dark ? warmAmber.opacity(0.92) : Color(hex: "A36518")
        }
    }

    public enum Typography {
        public static let display = Font.largeTitle.weight(.bold)
        public static let hero = Font.title.weight(.bold)
        public static let largeTitle = Font.largeTitle.weight(.bold)
        public static let title = Font.title.weight(.bold)
        public static let title2 = Font.title2.weight(.bold)
        public static let section = Font.title3.weight(.bold)
        public static let rowTitle = Font.headline
        public static let body = Font.body
        public static let bodySemibold = Font.body.weight(.semibold)
        public static let callout = Font.callout
        public static let metadata = Font.callout.weight(.medium)
        public static let caption = Font.caption.weight(.semibold)
        public static let tiny = Font.caption2.weight(.semibold)
    }

    public enum Spacing {
        public static let xxs: CGFloat = 4
        public static let xs: CGFloat = 8
        public static let sm: CGFloat = 12
        public static let md: CGFloat = 16
        public static let lg: CGFloat = 20
        public static let xl: CGFloat = 24
        public static let xxl: CGFloat = 32
        public static let page: CGFloat = 16
    }

    public enum Navigation {
        public static let controlTopSpacing: CGFloat = Spacing.sm
        public static let controlHorizontalPadding: CGFloat = Spacing.lg

        public static func controlTopPadding(safeAreaTop: CGFloat) -> CGFloat {
            safeAreaTop + controlTopSpacing
        }

        public static var currentSafeAreaTop: CGFloat {
            #if canImport(UIKit)
            let inset = UIApplication.shared.connectedScenes
                .compactMap { $0 as? UIWindowScene }
                .flatMap(\.windows)
                .first(where: \.isKeyWindow)?
                .safeAreaInsets.top ?? 0
            return max(inset, 44)
            #else
            return 44
            #endif
        }
    }

    public enum Radius {
        public static let sm: CGFloat = 12
        public static let md: CGFloat = 16
        public static let lg: CGFloat = 20
        public static let xl: CGFloat = 24
        public static let panel: CGFloat = 34
        public static let full: CGFloat = 999
    }

    public enum Shadow {
        public static let card = ShadowStyle(color: .black.opacity(0.18), radius: 22, x: 0, y: 12)
        public static let control = ShadowStyle(color: .black.opacity(0.18), radius: 16, x: 0, y: 8)
        public static let subtle = ShadowStyle(color: .black.opacity(0.08), radius: 10, x: 0, y: 4)
    }

    public enum Blur {
        public static let subtle: CGFloat = 8
        public static let glass: CGFloat = 18
        public static let sheet: CGFloat = 28
    }

    public enum Opacity {
        public static let glassLow = 0.10
        public static let glassRegular = 0.16
        public static let glassProminent = 0.24
        public static let border = 0.16
        public static let disabled = 0.42
        public static let scrim = 0.38
    }

    public enum Motion {
        public static let quick = 0.16
        public static let standard = 0.26
        public static let sheet = 0.36
        public static let confirmation = 0.42
        public static let tab = 0.22

        public static let standardSpring = Animation.spring(response: standard, dampingFraction: 0.86)
        public static let sheetSpring = Animation.spring(response: sheet, dampingFraction: 0.88)
        public static let confirmationSpring = Animation.spring(response: confirmation, dampingFraction: 0.74)
    }

    public enum Glass {
        public static let toolbarRadius: CGFloat = 24
        public static let cardRadius: CGFloat = Radius.xl
        public static let buttonRadius: CGFloat = Radius.full
        public static let tabBarRadius: CGFloat = 28
        public static let bottomSheetRadius: CGFloat = 32
    }

    public enum EventGradient {
        public static let invitation = LinearGradient(
            colors: [
                ColorToken.midnight,
                Color(hex: "102A3B"),
                Color(hex: "243346"),
                Color(hex: "5D5572")
            ],
            startPoint: .top,
            endPoint: .bottom
        )

        public static let profile = LinearGradient(
            colors: [
                ColorToken.profileWarmTop,
                ColorToken.profileWarmMid,
                ColorToken.profileWarmBottom
            ],
            startPoint: .top,
            endPoint: .bottom
        )

        public static let utility = LinearGradient(
            colors: [
                Color(hex: "202126"),
                Color(hex: "16171A")
            ],
            startPoint: .top,
            endPoint: .bottom
        )

        public static let birthday = LinearGradient(
            colors: [Color(hex: "FFB86B"), Color(hex: "F43F5E"), Color(hex: "061B4F")],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )

        public static let outdoor = LinearGradient(
            colors: [Color(hex: "FDE68A"), Color(hex: "0F766E"), Color(hex: "061B4F")],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )

        public static let work = LinearGradient(
            colors: [Color(hex: "67E8F9"), Color(hex: "2563EB"), Color(hex: "061B4F")],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }
}

// MARK: - Wakeve Design System v2.0
// Modern mobile design system with light/dark mode support

// MARK: - Color Scheme Adaptive Colors

/// Environment-aware colors that adapt to light/dark mode
public struct AdaptiveColors {
    // MARK: Backgrounds

    /// Main app background - uses iOS system color for native feel
    public static var background: Color {
        Color(uiColor: .systemBackground)
    }

    /// Secondary background (cards, sections)
    public static var secondaryBackground: Color {
        Color(uiColor: .secondarySystemBackground)
    }

    /// Tertiary background (grouped content)
    public static var tertiaryBackground: Color {
        Color(uiColor: .tertiarySystemBackground)
    }

    /// Grouped background (lists)
    public static var groupedBackground: Color {
        Color(uiColor: .systemGroupedBackground)
    }

    // MARK: Surfaces

    /// Tab bar surface (floating pill style)
    public static var tabBarSurface: Color {
        Color.dynamic(
            light: Color.appTabBarLight,
            dark: Color.appTabBarDark
        )
    }

    /// Card surface
    public static var cardSurface: Color {
        Color.dynamic(
            light: Color.wakeveSurfaceLight,
            dark: Color.wakeveSurfaceDark
        )
    }

    // MARK: Text

    /// Primary text - uses iOS system color for native feel
    public static var textPrimary: Color {
        Color(uiColor: .label)
    }

    /// Secondary text
    public static var textSecondary: Color {
        Color(uiColor: .secondaryLabel)
    }

    /// Tertiary text
    public static var textTertiary: Color {
        Color(uiColor: .tertiaryLabel)
    }

    // MARK: Accents

    /// Primary brand accent
    public static var accent: Color {
        Color.dynamic(
            light: Color.wakevePrimary,
            dark: Color.wakevePrimaryLight
        )
    }

    /// Secondary accent (purple)
    public static var accentSecondary: Color {
        Color.dynamic(
            light: Color.wakeveAccent,
            dark: Color.wakeveAccentLight
        )
    }

    /// Tab selection glow color
    public static var tabGlow: Color {
        Color.dynamic(
            light: Color.appAccent,
            dark: Color.appAccentLight
        )
    }

    // MARK: Semantic Colors

    /// Success state color
    public static var success: Color {
        Color.dynamic(
            light: Color.wakeveSuccess,
            dark: Color.wakeveSuccessLight
        )
    }

    /// Warning state color
    public static var warning: Color {
        Color.dynamic(
            light: Color.wakeveWarning,
            dark: Color.wakeveWarningLight
        )
    }

    /// Error state color
    public static var error: Color {
        Color.dynamic(
            light: Color.wakeveError,
            dark: Color.wakeveErrorLight
        )
    }

    // MARK: Separators

    /// Divider/separator color
    public static var separator: Color {
        Color(uiColor: .separator)
    }

    /// Opaque separator
    public static var opaqueSeparator: Color {
        Color(uiColor: .opaqueSeparator)
    }
}

// MARK: - Dynamic Color Extension

extension Color {
    /// Creates a dynamic color that adapts to light/dark mode
    static func dynamic(light: Color, dark: Color) -> Color {
        Color(UIColor { traitCollection in
            switch traitCollection.userInterfaceStyle {
            case .dark:
                return UIColor(dark)
            default:
                return UIColor(light)
            }
        })
    }
}

// MARK: - Typography

/// Modern mobile typography
public struct Typography {
    /// Large titles (navigation headers)
    public static let largeTitle = Font.system(size: 34, weight: .bold, design: .default)
    
    /// Navigation title
    public static let navigationTitle = Font.system(size: 17, weight: .semibold, design: .default)
    
    /// Section headers
    public static let sectionHeader = Font.system(size: 13, weight: .semibold, design: .default)
    
    /// Body text
    public static let body = Font.system(size: 16, weight: .regular, design: .default)
    
    /// Body medium
    public static let bodyMedium = Font.system(size: 16, weight: .medium, design: .default)
    
    /// Secondary text
    public static let secondary = Font.system(size: 14, weight: .regular, design: .default)
    
    /// Caption (tab labels)
    public static let caption = Font.system(size: 10, weight: .medium, design: .default)
    
    /// Caption semibold (selected tab)
    public static let captionSemibold = Font.system(size: 10, weight: .semibold, design: .default)
    
    /// Small text (timestamps, metadata)
    public static let small = Font.system(size: 12, weight: .regular, design: .default)
}

// MARK: - Spacing

/// Design system spacing values
public struct Spacing {
    /// 4pt - Minimal spacing
    public static let xs: CGFloat = 4
    
    /// 8pt - Small spacing
    public static let sm: CGFloat = 8
    
    /// 12pt - Compact spacing
    public static let md: CGFloat = 12
    
    /// 16pt - Standard spacing
    public static let base: CGFloat = 16
    
    /// 20pt - Medium spacing
    public static let lg: CGFloat = 20
    
    /// 24pt - Large spacing
    public static let xl: CGFloat = 24
    
    /// 32pt - Extra large spacing
    public static let xxl: CGFloat = 32
    
    /// Tab bar insets
    public struct TabBar {
        public static let horizontalPadding: CGFloat = 24
        public static let bottomPadding: CGFloat = 12
        public static let itemSpacing: CGFloat = 0
        public static let itemWidth: CGFloat = 56
        public static let height: CGFloat = 64
    }
}

// MARK: - Corner Radius

/// Design system corner radius values
public struct CornerRadius {
    /// 4pt - Small corners (badges)
    public static let sm: CGFloat = 4
    
    /// 8pt - Standard corners (buttons)
    public static let base: CGFloat = 8
    
    /// 12pt - Medium corners (cards)
    public static let md: CGFloat = 12
    
    /// 16pt - Large corners (sheets)
    public static let lg: CGFloat = 16
    
    /// Full rounded (capsule, pills)
    public static let full: CGFloat = 9999
}

// MARK: - Shadows

/// Design system shadow values
public struct Shadows {
    /// Tab bar shadow
    public static var tabBar: ShadowStyle {
        ShadowStyle(
            color: Color.black.opacity(0.08),
            radius: 20,
            x: 0,
            y: 8
        )
    }
    
    /// Card shadow (light mode)
    public static var cardLight: ShadowStyle {
        ShadowStyle(
            color: Color.black.opacity(0.04),
            radius: 8,
            x: 0,
            y: 2
        )
    }
    
    /// Card shadow (dark mode)
    public static var cardDark: ShadowStyle {
        ShadowStyle(
            color: Color.black.opacity(0.2),
            radius: 12,
            x: 0,
            y: 4
        )
    }
    
    /// Selection glow
    public static func glow(color: Color) -> ShadowStyle {
        ShadowStyle(
            color: color.opacity(0.4),
            radius: 8,
            x: 0,
            y: 4
        )
    }
}

/// Shadow style container
public struct ShadowStyle {
    public let color: Color
    public let radius: CGFloat
    public let x: CGFloat
    public let y: CGFloat
    
    public init(color: Color, radius: CGFloat, x: CGFloat, y: CGFloat) {
        self.color = color
        self.radius = radius
        self.x = x
        self.y = y
    }
}

// MARK: - View Modifiers

/// Applies tab bar style shadow
struct TabBarShadowModifier: ViewModifier {
    @Environment(\.colorScheme) private var colorScheme
    
    func body(content: Content) -> some View {
        content
            .shadow(
                color: colorScheme == .dark 
                    ? .black.opacity(0.4)
                    : .black.opacity(0.08),
                radius: 20,
                x: 0,
                y: 8
            )
    }
}

/// Applies card style with adaptive shadow
struct CardStyleModifier: ViewModifier {
    @Environment(\.colorScheme) private var colorScheme
    
    func body(content: Content) -> some View {
        content
            .background(AdaptiveColors.cardSurface)
            .cornerRadius(CornerRadius.md)
            .shadow(
                color: colorScheme == .dark
                    ? .black.opacity(0.2)
                    : .black.opacity(0.04),
                radius: colorScheme == .dark ? 12 : 8,
                x: 0,
                y: colorScheme == .dark ? 4 : 2
            )
    }
}

// MARK: - View Extensions

extension View {
    /// Applies tab bar shadow
    public func tabBarShadow() -> some View {
        modifier(TabBarShadowModifier())
    }
    
    /// Applies card style with shadow
    public func cardStyle() -> some View {
        modifier(CardStyleModifier())
    }
}
