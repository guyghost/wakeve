import SwiftUI
#if canImport(UIKit)
import UIKit
#endif

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
