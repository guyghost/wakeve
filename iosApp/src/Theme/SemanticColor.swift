import SwiftUI

/// Semantic colors describe function rather than hue.
///
/// Keep persistent navigation close to native iOS surfaces. Use stronger Wakeve
/// colors in content where they communicate state, mood, grouping, or feedback.
public enum SemanticColor {
    public static func appBackground(for colorScheme: ColorScheme) -> Color {
        colorScheme == .dark ? BrandColor.midnightBlue : Color(hex: "F6F1EA")
    }

    public static func contentSurface(for colorScheme: ColorScheme) -> Color {
        colorScheme == .dark ? BrandColor.midnightBlueRaised : Color.white.opacity(0.94)
    }

    public static func nativeChromeSurface(for colorScheme: ColorScheme) -> Color {
        colorScheme == .dark ? Color(uiColor: .systemBackground) : Color(uiColor: .systemBackground)
    }

    public static func primaryText(for colorScheme: ColorScheme) -> Color {
        colorScheme == .dark ? Color.white : Color(hex: "17171F")
    }

    public static func secondaryText(for colorScheme: ColorScheme) -> Color {
        colorScheme == .dark ? Color.white.opacity(0.66) : Color(hex: "606576")
    }

    public static func tertiaryText(for colorScheme: ColorScheme) -> Color {
        colorScheme == .dark ? Color.white.opacity(0.48) : Color(hex: "7A7F8D")
    }

    public static func border(for colorScheme: ColorScheme) -> Color {
        colorScheme == .dark ? Color.white.opacity(0.12) : Color.black.opacity(0.08)
    }

    public static func separator(for colorScheme: ColorScheme) -> Color {
        colorScheme == .dark ? Color.white.opacity(0.10) : Color.black.opacity(0.07)
    }

    public static func selectedState(for colorScheme: ColorScheme) -> Color {
        BrandColor.primaryAccent(for: colorScheme)
    }

    public static func badge(for colorScheme: ColorScheme) -> Color {
        colorScheme == .dark ? BrandColor.blueGrey.opacity(0.32) : BrandColor.blueGrey.opacity(0.16)
    }

    public static func callToAction(for colorScheme: ColorScheme) -> Color {
        colorScheme == .dark ? Color.white.opacity(0.92) : BrandColor.midnightBlue
    }

    public static func progress(for colorScheme: ColorScheme) -> Color {
        colorScheme == .dark ? BrandColor.paleBlue : Color(hex: "2E78A6")
    }

    public static func confirmation(for colorScheme: ColorScheme) -> Color {
        colorScheme == .dark ? Color(hex: "7CCFA8") : Color(hex: "287A52")
    }

    public static func warning(for colorScheme: ColorScheme) -> Color {
        colorScheme == .dark ? BrandColor.subtleAmber : Color(hex: "A36518")
    }

    public static func destructive(for colorScheme: ColorScheme) -> Color {
        colorScheme == .dark ? Color(hex: "FF7A86") : Color(hex: "C93445")
    }
}
