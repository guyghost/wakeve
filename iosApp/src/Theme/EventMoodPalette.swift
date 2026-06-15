import SwiftUI

/// Content-layer palettes for recognizable event moods.
///
/// Mood palettes are intentionally separate from native UI chrome. They can drive
/// event heroes, cards, invitation previews, widget previews, and empty states.
public struct EventMoodPalette: Equatable {
    public enum Mood: String, CaseIterable, Identifiable {
        case evening
        case travel
        case birthday
        case family
        case dinner
        case beach
        case weekend

        public var id: String { rawValue }
    }

    public let mood: Mood
    public let name: String
    public let primaryHex: String
    public let secondaryHex: String
    public let accentHex: String
    public let darkPrimaryHex: String
    public let darkSecondaryHex: String
    public let darkAccentHex: String
    public let symbolName: String
    public let microcopy: String

    public func primary(for colorScheme: ColorScheme) -> Color {
        Color(hex: colorScheme == .dark ? darkPrimaryHex : primaryHex)
    }

    public func secondary(for colorScheme: ColorScheme) -> Color {
        Color(hex: colorScheme == .dark ? darkSecondaryHex : secondaryHex)
    }

    public func accent(for colorScheme: ColorScheme) -> Color {
        Color(hex: colorScheme == .dark ? darkAccentHex : accentHex)
    }

    public func gradient(for colorScheme: ColorScheme) -> LinearGradient {
        LinearGradient(
            colors: [
                primary(for: colorScheme),
                secondary(for: colorScheme),
                accent(for: colorScheme).opacity(colorScheme == .dark ? 0.72 : 0.58)
            ],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }

    public static func palette(for mood: Mood) -> EventMoodPalette {
        palettes[mood] ?? weekend
    }

    public static func palette(for eventType: String?) -> EventMoodPalette {
        guard let value = eventType?.lowercased() else { return weekend }

        if value.contains("soir") || value.contains("party") || value.contains("fete") || value.contains("fête") {
            return evening
        }
        if value.contains("voyage") || value.contains("travel") || value.contains("trip") {
            return travel
        }
        if value.contains("anniversaire") || value.contains("birthday") {
            return birthday
        }
        if value.contains("famille") || value.contains("family") {
            return family
        }
        if value.contains("diner") || value.contains("dîner") || value.contains("dinner") || value.contains("restaurant") {
            return dinner
        }
        if value.contains("plage") || value.contains("beach") || value.contains("sea") {
            return beach
        }
        return weekend
    }

    public static let evening = EventMoodPalette(
        mood: .evening,
        name: "Soiree",
        primaryHex: "3B2B59",
        secondaryHex: "D98D73",
        accentHex: "F3B45B",
        darkPrimaryHex: "120D20",
        darkSecondaryHex: "2D2446",
        darkAccentHex: "D99A6C",
        symbolName: "sparkles",
        microcopy: "Le moment prend forme."
    )

    public static let travel = EventMoodPalette(
        mood: .travel,
        name: "Voyage",
        primaryHex: "2F6F9F",
        secondaryHex: "A9C7E8",
        accentHex: "F4A26D",
        darkPrimaryHex: "071421",
        darkSecondaryHex: "12344A",
        darkAccentHex: "D78B5D",
        symbolName: "airplane.departure",
        microcopy: "Le groupe se met en route."
    )

    public static let birthday = EventMoodPalette(
        mood: .birthday,
        name: "Anniversaire",
        primaryHex: "B35E72",
        secondaryHex: "F4A26D",
        accentHex: "F3B45B",
        darkPrimaryHex: "24101A",
        darkSecondaryHex: "5C3240",
        darkAccentHex: "E0A65D",
        symbolName: "gift.fill",
        microcopy: "Une date a celebrer ensemble."
    )

    public static let family = EventMoodPalette(
        mood: .family,
        name: "Famille",
        primaryHex: "617A68",
        secondaryHex: "E7D4BE",
        accentHex: "D49461",
        darkPrimaryHex: "101C16",
        darkSecondaryHex: "2D4035",
        darkAccentHex: "C99061",
        symbolName: "person.3.fill",
        microcopy: "Tout le monde trouve sa place."
    )

    public static let dinner = EventMoodPalette(
        mood: .dinner,
        name: "Diner",
        primaryHex: "6E3E2E",
        secondaryHex: "D49461",
        accentHex: "F0D3AF",
        darkPrimaryHex: "1E1210",
        darkSecondaryHex: "513024",
        darkAccentHex: "D6AA78",
        symbolName: "fork.knife",
        microcopy: "La table se prepare."
    )

    public static let beach = EventMoodPalette(
        mood: .beach,
        name: "Plage",
        primaryHex: "2F8F96",
        secondaryHex: "B6D7C9",
        accentHex: "F4C06A",
        darkPrimaryHex: "061E24",
        darkSecondaryHex: "1D5258",
        darkAccentHex: "D9A856",
        symbolName: "sun.max.fill",
        microcopy: "Cap sur l'air libre."
    )

    public static let weekend = EventMoodPalette(
        mood: .weekend,
        name: "Week-end",
        primaryHex: "33475B",
        secondaryHex: "B8A8D9",
        accentHex: "F4A26D",
        darkPrimaryHex: "071421",
        darkSecondaryHex: "243346",
        darkAccentHex: "C98A62",
        symbolName: "calendar",
        microcopy: "Un moment simple a organiser."
    )

    private static let palettes: [Mood: EventMoodPalette] = [
        .evening: evening,
        .travel: travel,
        .birthday: birthday,
        .family: family,
        .dinner: dinner,
        .beach: beach,
        .weekend: weekend
    ]
}
