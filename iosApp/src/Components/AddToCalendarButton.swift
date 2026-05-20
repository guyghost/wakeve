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

#if DEBUG
#Preview("Enabled") {
    AddToCalendarButton(
        event: EventFactory.make(
            title: "Team Meeting",
            status: .confirmed,
            finalDate: ISO8601DateFormatter().string(from: Date().addingTimeInterval(86400))
        ),
        isLoading: false,
        isEnabled: true,
        action: {}
    )
    .padding()
}

#Preview("Loading / Disabled") {
    AddToCalendarButton(
        event: EventFactory.make(
            title: "Birthday Dinner",
            status: .polling
        ),
        isLoading: true,
        isEnabled: false,
        action: {}
    )
    .padding()
}
#endif
