import Foundation

/// Iconography policy used by contract tests and component reviews.
enum IconographyGuidelines {
    static let standardActionSymbols: [String: String] = [
        "share": "square.and.arrow.up",
        "delete": "trash",
        "edit": "pencil",
        "search": "magnifyingglass",
        "back": "chevron.backward",
        "close": "xmark",
        "add": "plus",
        "confirm": "checkmark",
        "calendar": "calendar"
    ]

    static let wakeveConceptSymbols: [String: String] = [
        "sharedMoment": "sparkles",
        "group": "person.2",
        "mood": "paintpalette",
        "socialVote": "chart.bar",
        "coordination": "point.3.connected.trianglepath.dotted",
        "sharedTravel": "airplane.departure"
    ]

    static func isStandardAction(_ action: String) -> Bool {
        standardActionSymbols[action] != nil
    }
}
