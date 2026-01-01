import SwiftUI
import Shared

/// Event type picker with native iOS Menu and custom TextField.
/// Mirrors Android's EventTypeSelector with Liquid Glass design.
///
/// Features:
/// - Menu with all 11 predefined event types from Kotlin Shared module
/// - Conditional TextField for CUSTOM type
/// - Validation with error message
/// - VoiceOver accessibility
/// - Liquid Glass styling
///
/// Example:
/// ```swift
/// EventTypePicker(
///     selectedType: $eventType,
///     customTypeValue: $customTypeText,
///     enabled: true
/// )
/// ```
struct EventTypePicker: View {
    @Binding var selectedType: Shared.EventType
    @Binding var customTypeValue: String
    var enabled: Bool = true
    
    // MARK: - Body
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Event Type Menu
            Menu {
                ForEach(Shared.EventType.entries, id: \.hashValue) { type in
                    Button {
                        withAnimation(.easeInOut(duration: 0.2)) {
                            selectedType = type
                        }
                    } label: {
                        HStack {
                            Text(type.displayName)
                            if type.hashValue == selectedType.hashValue {
                                Spacer()
                                Image(systemName: "checkmark")
                                    .foregroundColor(.blue)
                            }
                        }
                    }
                }
            } label: {
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Event Type")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        
                        Text(selectedType.displayName)
                            .font(.body)
                            .foregroundColor(.primary)
                    }
                    
                    Spacer()
                    
                    Image(systemName: "chevron.up.chevron.down")
                        .font(.system(size: 14, weight: .medium))
                        .foregroundColor(.secondary)
                }
                .padding()
                .background(.ultraThinMaterial)
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .stroke(Color.primary.opacity(0.1), lineWidth: 1)
                )
            }
            .disabled(!enabled)
            .accessibilityLabel("Event Type")
            .accessibilityValue(selectedType.displayName)
            .accessibilityHint("Select the type of event you're organizing")
            
            // Custom Type TextField (conditional)
            if selectedType == Shared.EventType.custom {
                VStack(alignment: .leading, spacing: 8) {
                    TextField("Custom Event Type", text: $customTypeValue)
                        .font(.body)
                        .padding()
                        .background(.ultraThinMaterial)
                        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                        .overlay(
                            RoundedRectangle(cornerRadius: 12, style: .continuous)
                                .stroke(
                                    customTypeValue.isEmpty ? Color.red.opacity(0.5) : Color.primary.opacity(0.1),
                                    lineWidth: 1
                                )
                        )
                        .disabled(!enabled)
                        .accessibilityLabel("Custom Event Type")
                        .accessibilityHint("Describe your custom event type, for example: Charity Gala or Product Launch")
                    
                    // Helper Text
                    Text("Describe your event type")
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .padding(.leading, 4)
                    
                    // Error Message
                    if customTypeValue.isEmpty {
                        Label("Custom event type is required", systemImage: "exclamationmark.circle.fill")
                            .font(.caption)
                            .foregroundColor(.red)
                            .padding(.leading, 4)
                            .transition(.opacity.combined(with: .scale))
                    }
                }
                .transition(.opacity.combined(with: .move(edge: .top)))
            }
        }
        .animation(.easeInOut(duration: 0.25), value: selectedType.hashValue)
    }
}

// MARK: - Previews

struct EventTypePicker_Previews: PreviewProvider {
    static var previews: some View {
        VStack(spacing: 32) {
            // Preview 1: Birthday selected
            PreviewWrapper(
                selectedType: Shared.EventType.birthday,
                customTypeValue: "",
                title: "Predefined Type Selected"
            )
            
            // Preview 2: Custom selected (empty)
            PreviewWrapper(
                selectedType: Shared.EventType.custom,
                customTypeValue: "",
                title: "Custom Type (Empty - Error)"
            )
            
            // Preview 3: Custom selected (filled)
            PreviewWrapper(
                selectedType: Shared.EventType.custom,
                customTypeValue: "Charity Gala",
                title: "Custom Type (Valid)"
            )
            
            // Preview 4: Disabled state
            PreviewWrapper(
                selectedType: Shared.EventType.conference,
                customTypeValue: "",
                title: "Disabled State",
                enabled: false
            )
        }
        .padding()
        .background(Color(UIColor.systemGroupedBackground))
    }
    
    /// Helper wrapper for previews with state
    private struct PreviewWrapper: View {
        @State var selectedType: Shared.EventType
        @State var customTypeValue: String
        let title: String
        var enabled: Bool = true
        
        var body: some View {
            VStack(alignment: .leading, spacing: 12) {
                Text(title)
                    .font(.headline)
                    .foregroundColor(.secondary)
                
                LiquidGlassCard(padding: 16) {
                    EventTypePicker(
                        selectedType: $selectedType,
                        customTypeValue: $customTypeValue,
                        enabled: enabled
                    )
                }
            }
        }
    }
}
