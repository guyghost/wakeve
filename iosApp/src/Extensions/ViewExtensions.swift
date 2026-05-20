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
