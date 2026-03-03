import SwiftUI

#if DEBUG

enum ScenarioFactory {

    // MARK: - Named Variants

    /// Birthday party scenario.
    static var birthday: EventScenario {
        EventScenario(
            title: "Birthday Party",
            subtitle: "Celebrate in style",
            description: "Plan the perfect birthday celebration with friends and family.",
            eventType: "BIRTHDAY",
            suggestedTitle: "Birthday Celebration",
            checklistItems: [
                "Choose a venue",
                "Send invitations",
                "Order the cake",
                "Plan decorations"
            ],
            icon: "birthday.cake",
            gradientColors: [Color(hex: "EC4899"), Color(hex: "8B5CF6")],
            category: .social,
            isFeatured: true
        )
    }

    /// Team building event scenario.
    static var teamBuilding: EventScenario {
        EventScenario(
            title: "Team Building",
            subtitle: "Strengthen your team",
            description: "Organize a fun team building activity to boost collaboration and morale.",
            eventType: "TEAM_BUILDING",
            suggestedTitle: "Team Building Day",
            checklistItems: [
                "Pick an activity",
                "Book the venue",
                "Set the budget",
                "Notify the team"
            ],
            icon: "person.3.fill",
            gradientColors: [Color(hex: "2563EB"), Color(hex: "06B6D4")],
            category: .professional,
            isFeatured: true
        )
    }

    /// Wedding scenario.
    static var wedding: EventScenario {
        EventScenario(
            title: "Wedding",
            subtitle: "Your special day",
            description: "Plan every detail of the most memorable day of your life.",
            eventType: "WEDDING",
            suggestedTitle: "Our Wedding",
            checklistItems: [
                "Choose the venue",
                "Select catering",
                "Arrange flowers",
                "Send save-the-dates"
            ],
            icon: "heart.fill",
            gradientColors: [Color(hex: "BE185D"), Color(hex: "7C3AED")],
            category: .social,
            isFeatured: true
        )
    }

    // MARK: - Builder

    /// Create a scenario with sensible defaults. Override only the parameters you need.
    static func make(
        title: String = "Test Scenario",
        subtitle: String = "A test subtitle",
        description: String = "A test scenario for preview purposes.",
        eventType: String = "PARTY",
        suggestedTitle: String = "Test Event",
        checklistItems: [String] = ["Item 1", "Item 2", "Item 3"],
        icon: String = "calendar",
        gradientColors: [Color] = [Color(hex: "8B5CF6"), Color(hex: "EC4899")],
        category: EventCategoryItem = .social,
        isFeatured: Bool = false
    ) -> EventScenario {
        EventScenario(
            title: title,
            subtitle: subtitle,
            description: description,
            eventType: eventType,
            suggestedTitle: suggestedTitle,
            checklistItems: checklistItems,
            icon: icon,
            gradientColors: gradientColors,
            category: category,
            isFeatured: isFeatured
        )
    }
}

#endif
