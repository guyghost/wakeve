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
        public static let appDarkCard = Color.white.opacity(0.075)
        public static let eventLilacAction = Color(hex: "F6D8FF")
        public static let eventLilacText = Color(hex: "1C0B24")
        public static let permissionBlue = Color(hex: "3F8FF2")
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
        public static let softIvory = Color.wakeveWarmIvory
        public static let mutedLavender = Color(hex: "B8A8D9")
        public static let paleBlue = Color(hex: "A9C7E8")
        public static let warmAmber = Color(hex: "F3B45B")
        public static let confirmationBase = Color(hex: "7CCFA8")
        public static let progressBase = Color(hex: "8BBBE8")
        public static let destructiveBase = Color(hex: "E34D5C")

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

        public static func controlTopPadding(safeAreaTop: CGFloat) -> CGFloat {
            safeAreaTop + controlTopSpacing
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

    public enum Opacity {
        public static let border = 0.16
        public static let disabled = 0.42
    }

    public enum Motion {
        public static let standard = 0.26
        public static let confirmation = 0.42

        public static let standardSpring = Animation.spring(response: standard, dampingFraction: 0.86)
        public static let confirmationSpring = Animation.spring(response: confirmation, dampingFraction: 0.74)
    }

    public enum Glass {
        public static let toolbarRadius: CGFloat = 24
        public static let cardRadius: CGFloat = Radius.xl
        public static let buttonRadius: CGFloat = Radius.full
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
    }
}

// MARK: - Shadow Style

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
