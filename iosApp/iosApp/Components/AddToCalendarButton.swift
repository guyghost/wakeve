import SwiftUI
import Shared

/// Reusable button component for adding events to calendar
/// Can be used standalone or as part of a larger UI
struct AddToCalendarButton: View {
    let event: Event
    let isLoading: Bool
    let isEnabled: Bool
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            HStack(spacing: 12) {
                Image(systemName: "calendar.badge.plus")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(.white)
                
                Text("Add to Calendar")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(.white)
                
                Spacer()
                
                if isLoading {
                    ProgressView()
                        .scaleEffect(0.8)
                        .tint(.white)
                }
            }
            .frame(height: 48)
            .frame(maxWidth: .infinity)
            .background(Color.blue)
            .continuousCornerRadius(12)
        }
        .disabled(!isEnabled || isLoading)
        .opacity(isLoading || !isEnabled ? 0.7 : 1.0)
        .accessibilityLabel("Add event to calendar")
        .accessibilityHint("Adds '\(event.title)' to your native calendar")
    }
}

// MARK: - Preview

#Preview {
    VStack(spacing: 20) {
        AddToCalendarButton(
            event: Event(
                id: "event-1",
                title: "Team Meeting",
                description: "Q4 Planning",
                organizerId: "user-1",
                participants: [],
                proposedSlots: [],
                deadline: ISO8601DateFormatter().string(from: Date()),
                status: .confirmed,
                finalDate: ISO8601DateFormatter().string(from: Date().addingTimeInterval(86400)),
                createdAt: ISO8601DateFormatter().string(from: Date()),
                updatedAt: ISO8601DateFormatter().string(from: Date()),
                eventType: .teamBuilding,
                eventTypeCustom: nil,
                minParticipants: nil,
                maxParticipants: nil,
                expectedParticipants: nil,
                heroImageUrl: nil
            ),
            isLoading: false,
            isEnabled: true,
            action: { print("Tapped add to calendar") }
        )
        
        AddToCalendarButton(
            event: Event(
                id: "event-2",
                title: "Team Meeting",
                description: "Q4 Planning",
                organizerId: "user-1",
                participants: [],
                proposedSlots: [],
                deadline: ISO8601DateFormatter().string(from: Date()),
                status: .polling,
                finalDate: nil,
                createdAt: ISO8601DateFormatter().string(from: Date()),
                updatedAt: ISO8601DateFormatter().string(from: Date()),
                eventType: .teamBuilding,
                eventTypeCustom: nil,
                minParticipants: nil,
                maxParticipants: nil,
                expectedParticipants: nil,
                heroImageUrl: nil
            ),
            isLoading: true,
            isEnabled: false,
            action: { print("Tapped add to calendar") }
        )
    }
    .padding()
}
