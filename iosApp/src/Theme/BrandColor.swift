import SwiftUI

/// Wakeve brand colors that define the product identity without replacing iOS system chrome.
///
/// Use these tokens for content-layer brand expression: event visuals, invitations,
/// empty states, mood details, progress, and selected content states.
public enum BrandColor {
    public static let midnightBlue = Color(hex: "071421")
    public static let midnightBlueRaised = Color(hex: "101E2A")
    public static let graphite = Color(hex: "17191D")
    public static let softIvory = Color(hex: "F7F3EC")
    public static let warmPeach = Color(hex: "F4A26D")
    public static let mutedLavender = Color(hex: "B8A8D9")
    public static let subtleAmber = Color(hex: "F3B45B")
    public static let blueGrey = Color(hex: "6F8799")
    public static let paleBlue = Color(hex: "A9C7E8")

    public static func primaryAccent(for colorScheme: ColorScheme) -> Color {
        colorScheme == .dark ? paleBlue : Color(hex: "2F6F9F")
    }

    public static func emotionalAccent(for colorScheme: ColorScheme) -> Color {
        colorScheme == .dark ? warmPeach.opacity(0.92) : Color(hex: "B35E35")
    }

    public static func calmAccent(for colorScheme: ColorScheme) -> Color {
        colorScheme == .dark ? mutedLavender.opacity(0.86) : Color(hex: "6F6192")
    }
}
