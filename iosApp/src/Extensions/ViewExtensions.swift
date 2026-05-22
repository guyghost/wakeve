import SwiftUI
import Shared

// MARK: - View Extensions

extension View {
    /// Applies Apple's recommended continuous corner radius
    func continuousCornerRadius(_ radius: CGFloat) -> some View {
        self.clipShape(RoundedRectangle(cornerRadius: radius, style: .continuous))
    }
}

// MARK: - Placeholder Extension

extension View {
    /// Overlay a placeholder view when a condition is true
    func placeholder<Content: View>(
        when shouldShow: Bool,
        alignment: Alignment = .leading,
        @ViewBuilder placeholder: () -> Content
    ) -> some View {
        ZStack(alignment: alignment) {
            placeholder().opacity(shouldShow ? 1 : 0)
            self
        }
    }
}

// MARK: - EventType Helpers

/// Returns an emoji for the given KMP EventType.
/// Free function because KMP KotlinEnum subclasses don't support Swift extension property access in all contexts.
func eventTypeEmoji(_ eventType: Shared.EventType) -> String {
    if eventType == Shared.EventType.birthday { return "🎂" }
    if eventType == Shared.EventType.wedding { return "💍" }
    if eventType == Shared.EventType.teamBuilding { return "🤝" }
    if eventType == Shared.EventType.conference { return "🎤" }
    if eventType == Shared.EventType.workshop { return "🛠" }
    if eventType == Shared.EventType.party { return "🎉" }
    if eventType == Shared.EventType.sportsEvent || eventType == Shared.EventType.sportEvent { return "⚽" }
    if eventType == Shared.EventType.culturalEvent { return "🎭" }
    if eventType == Shared.EventType.familyGathering { return "👨‍👩‍👧" }
    if eventType == Shared.EventType.outdoorActivity { return "🏔" }
    if eventType == Shared.EventType.foodTasting { return "🍷" }
    if eventType == Shared.EventType.techMeetup { return "💻" }
    if eventType == Shared.EventType.wellnessEvent { return "🧘" }
    if eventType == Shared.EventType.creativeWorkshop { return "🎨" }
    if eventType == Shared.EventType.custom { return "✨" }
    return "📅"
}

func eventTypeDisplayName(_ eventType: Shared.EventType) -> String {
    if eventType == Shared.EventType.birthday { return String(localized: "explore.event_type.birthday") }
    if eventType == Shared.EventType.wedding { return String(localized: "explore.event_type.wedding") }
    if eventType == Shared.EventType.teamBuilding { return String(localized: "explore.event_type.team_building") }
    if eventType == Shared.EventType.conference { return String(localized: "explore.event_type.conference") }
    if eventType == Shared.EventType.workshop { return String(localized: "explore.event_type.workshop") }
    if eventType == Shared.EventType.party { return String(localized: "explore.event_type.party") }
    if eventType == Shared.EventType.sportsEvent || eventType == Shared.EventType.sportEvent { return String(localized: "explore.event_type.sport") }
    if eventType == Shared.EventType.culturalEvent { return String(localized: "explore.event_type.culture") }
    if eventType == Shared.EventType.familyGathering { return String(localized: "explore.event_type.family") }
    if eventType == Shared.EventType.outdoorActivity { return String(localized: "explore.event_type.outdoor") }
    if eventType == Shared.EventType.foodTasting { return String(localized: "explore.event_type.food") }
    if eventType == Shared.EventType.techMeetup { return String(localized: "explore.event_type.tech") }
    if eventType == Shared.EventType.wellnessEvent { return String(localized: "explore.event_type.wellness") }
    if eventType == Shared.EventType.creativeWorkshop { return String(localized: "explore.event_type.creative") }
    return String(localized: "explore.event_type.other")
}
