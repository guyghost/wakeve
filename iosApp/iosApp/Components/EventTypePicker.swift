import SwiftUI
import Shared

/// Event type picker with Liquid Glass design components.
/// Mirrors Android's EventTypeSelector with modern iOS aesthetics.
///
/// Features:
/// - LiquidGlassButton grid for type selection
/// - LiquidGlassBadge to indicate selected type
/// - Custom text field for CUSTOM type
/// - Validation with error message
/// - VoiceOver accessibility
/// - Full Liquid Glass styling
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
    
    // MARK: - Computed Properties
    
    private var isCustomSelected: Bool {
        selectedType == Shared.EventType.custom
    }
    
    private var customTypeError: Bool {
        isCustomSelected && customTypeValue.isEmpty
    }
    
    // MARK: - Body
    
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            // Section Label
            Text("Event Type")
                .font(.subheadline)
                .fontWeight(.medium)
                .foregroundColor(.secondary)
                .padding(.horizontal, 4)
            
            // Event Types Grid
            LazyVGrid(
                columns: [
                    GridItem(.flexible()),
                    GridItem(.flexible())
                ],
                spacing: 12
            ) {
                ForEach(Shared.EventType.entries, id: \.hashValue) { type in
                    EventTypeButton(
                        type: type,
                        isSelected: type.hashValue == selectedType.hashValue,
                        enabled: enabled,
                        action: {
                            withAnimation(.easeInOut(duration: 0.2)) {
                                selectedType = type
                            }
                        }
                    )
                }
            }
            
            // Custom Type Section (conditional)
            if isCustomSelected {
                customTypeSection
                    .transition(.opacity.combined(with: .move(edge: .top)))
            }
        }
        .animation(.easeInOut(duration: 0.25), value: selectedType.hashValue)
    }
    
    // MARK: - Custom Type Section
    
    private var customTypeSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            // Label
            Text("Custom Event Type")
                .font(.caption)
                .fontWeight(.medium)
                .foregroundColor(.secondary)
                .padding(.horizontal, 4)
            
            // Custom Text Field with Liquid Glass styling
            customTextField
                .accessibilityLabel("Custom Event Type")
                .accessibilityHint("Describe your custom event type, for example: Charity Gala or Product Launch")
            
            // Error Message
            if customTypeError {
                HStack(spacing: 4) {
                    Image(systemName: "exclamationmark.circle.fill")
                        .font(.caption)
                        .foregroundColor(.red)
                    Text("Custom event type is required")
                        .font(.caption)
                        .foregroundColor(.red)
                }
                .padding(.leading, 4)
                .transition(.opacity.combined(with: .scale))
            }
        }
    }
    
    private var customTextField: some View {
        HStack(spacing: 0) {
            // Placeholder
            ZStack(alignment: .leading) {
                if customTypeValue.isEmpty {
                    Text("Describe your event type")
                        .font(.body)
                        .foregroundColor(.tertiary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, 16)
                }
                
                TextField("", text: $customTypeValue)
                    .font(.body)
                    .foregroundColor(.primary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, 16)
                    .disabled(!enabled)
                    .autocapitalization(.words)
            }
            .frame(minHeight: 56)
        }
        .background(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(Color(UIColor.ultraThinMaterial))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .stroke(
                    customTypeError 
                        ? Color.red.opacity(0.5) 
                        : Color.primary.opacity(0.1),
                    lineWidth: 1
                )
        )
        .shadow(
            color: Color.wakevPrimary.opacity(customTypeError ? 0.1 : 0.05),
            radius: customTypeError ? 4 : 2,
            x: 0,
            y: customTypeError ? 2 : 1
        )
        .opacity(enabled ? 1.0 : 0.6)
    }
}

// MARK: - Event Type Button

private struct EventTypeButton: View {
    let type: Shared.EventType
    let isSelected: Bool
    let enabled: Bool
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                // Type Icon
                Image(systemName: type.iconName)
                    .font(.system(size: 16, weight: .medium))
                    .foregroundColor(isSelected ? .white : typeColor)
                    .frame(width: 32, height: 32)
                    .background(
                        Circle()
                            .fill(isSelected ? typeGradient : typeColor.opacity(0.15))
                    )
                
                // Type Name
                Text(type.displayName)
                    .font(.subheadline)
                    .fontWeight(isSelected ? .semibold : .regular)
                    .foregroundColor(isSelected ? .white : .primary)
                    .lineLimit(1)
                
                Spacer()
                
                // Selection Indicator (Badge)
                if isSelected {
                    LiquidGlassBadge(
                        text: "Selected",
                        icon: "checkmark.circle.fill",
                        style: .success
                    )
                    .transition(.scale.combined(with: .opacity))
                }
            }
            .padding(12)
            .background(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .fill(isSelected ? selectedBackground : Color.clear)
                    .overlay(
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
                            .stroke(
                                isSelected ? selectedBorder : Color.primary.opacity(0.1),
                                lineWidth: isSelected ? 1.5 : 0.5
                            )
                    )
            )
            .liquidGlass(
                cornerRadius: 16,
                opacity: isSelected ? 0.8 : 0.4,
                intensity: isSelected ? 1.0 : 0.6
            )
        }
        .buttonStyle(PlainButtonStyle())
        .disabled(!enabled)
        .opacity(enabled ? 1.0 : 0.6)
        .scaleEffect(isSelected ? 1.02 : 1.0)
        .animation(.easeInOut(duration: 0.2), value: isSelected)
        .accessibilityLabel(type.displayName)
        .accessibilityValue(isSelected ? "Selected" : "Not selected")
        .accessibilityHint(isSelected ? "Currently selected" : "Tap to select this event type")
    }
    
    // MARK: - Color Helpers
    
    private var typeColor: Color {
        switch type {
        case .birthday:
            return .pink
        case .wedding:
            return .pink.opacity(0.8)
        case .anniversary:
            return .red.opacity(0.8)
        case .teamBuilding:
            return .blue
        case .conference:
            return .purple
        case .workshop:
            return .orange
        case .dinner:
            return .green
        case .vacation:
            return .teal
        case .holiday:
            return .indigo
        case .meeting:
            return .cyan
        case .custom:
            return .gray
        @unknown default:
            return .blue
        }
    }
    
    private var typeGradient: LinearGradient {
        LinearGradient(
            gradient: Gradient(colors: [
                typeColor.opacity(0.9),
                typeColor.opacity(0.7)
            ]),
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }
    
    private var selectedBackground: some View {
        LinearGradient(
            gradient: Gradient(colors: [
                Color.wakevPrimary,
                Color.wakevAccent
            ]),
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }
    
    private var selectedBorder: LinearGradient {
        LinearGradient(
            gradient: Gradient(colors: [
                Color.white.opacity(0.4),
                Color.white.opacity(0.2)
            ]),
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }
}

// MARK: - EventType Extensions

private extension Shared.EventType {
    var iconName: String {
        switch self {
        case .birthday:
            return "cake.fill"
        case .wedding:
            return "heart.fill"
        case .anniversary:
            return "calendar.badge.clock"
        case .teamBuilding:
            return "person.3.fill"
        case .conference:
            return "mic.fill"
        case .workshop:
            return "hammer.fill"
        case .dinner:
            return "fork.knife"
        case .vacation:
            return "sun.max.fill"
        case .holiday:
            return "gift.fill"
        case .meeting:
            return "bubble.left.and.bubble.right.fill"
        case .custom:
            return "sparkles"
        @unknown default:
            return "star.fill"
        }
    }
}

// MARK: - Previews

struct EventTypePicker_Previews: PreviewProvider {
    static var previews: some View {
        ScrollView {
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
        }
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
                
                LiquidGlassCard(padding: 20) {
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
