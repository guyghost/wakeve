import SwiftUI

/// Card for estimating participant counts.
/// Mirrors Android's ParticipantsEstimationCard with Liquid Glass design.
///
/// Features:
/// - Three number inputs: minimum, maximum, expected participants
/// - Real-time validation (max >= min, positive values)
/// - Warning when expected is outside range
/// - SF Symbols icons
/// - Liquid Glass styling
/// - VoiceOver accessibility
///
/// Validation Rules:
/// - All values must be positive integers (>= 1)
/// - Maximum must be >= minimum
/// - Expected should be between min and max (warning only, not error)
///
/// Example:
/// ```swift
/// ParticipantsEstimationCard(
///     minParticipants: $minCount,
///     maxParticipants: $maxCount,
///     expectedParticipants: $expectedCount,
///     enabled: true
/// )
/// ```
struct ParticipantsEstimationCard: View {
    @Binding var minParticipants: Int?
    @Binding var maxParticipants: Int?
    @Binding var expectedParticipants: Int?
    var enabled: Bool = true
    
    // Local text state for TextField binding
    @State private var minText: String
    @State private var maxText: String
    @State private var expectedText: String
    
    // MARK: - Initialization
    
    init(
        minParticipants: Binding<Int?>,
        maxParticipants: Binding<Int?>,
        expectedParticipants: Binding<Int?>,
        enabled: Bool = true
    ) {
        self._minParticipants = minParticipants
        self._maxParticipants = maxParticipants
        self._expectedParticipants = expectedParticipants
        self.enabled = enabled
        
        // Initialize text state from bindings
        _minText = State(initialValue: minParticipants.wrappedValue.map { String($0) } ?? "")
        _maxText = State(initialValue: maxParticipants.wrappedValue.map { String($0) } ?? "")
        _expectedText = State(initialValue: expectedParticipants.wrappedValue.map { String($0) } ?? "")
    }
    
    // MARK: - Computed Properties
    
    /// Parsed minimum value
    private var minValue: Int? {
        Int(minText)
    }
    
    /// Parsed maximum value
    private var maxValue: Int? {
        Int(maxText)
    }
    
    /// Parsed expected value
    private var expectedValue: Int? {
        Int(expectedText)
    }
    
    /// Is the maximum value valid? (must be >= minimum)
    private var isMaxValid: Bool {
        guard let max = maxValue, let min = minValue else { return true }
        return max >= min
    }
    
    /// Is the expected value outside the min-max range?
    private var expectedOutOfRange: Bool {
        guard let expected = expectedValue else { return false }
        
        if let min = minValue, expected < min {
            return true
        }
        if let max = maxValue, expected > max {
            return true
        }
        return false
    }
    
    // MARK: - Body
    
    var body: some View {
        LiquidGlassCard(cornerRadius: 16, padding: 20) {
            VStack(alignment: .leading, spacing: 16) {
                // Header
                headerSection
                
                // Description
                descriptionSection
                
                // Input Fields
                inputFieldsSection
                
                // Helper Info Section
                if minValue != nil || maxValue != nil || expectedValue != nil {
                    helperInfoSection
                }
            }
        }
        .animation(.easeInOut(duration: 0.2), value: minValue != nil ? minValue! : 0)
        .animation(.easeInOut(duration: 0.2), value: maxValue != nil ? maxValue! : 0)
        .animation(.easeInOut(duration: 0.2), value: expectedValue != nil ? expectedValue! : 0)
    }
    
    // MARK: - Header Section
    
    private var headerSection: some View {
        HStack(spacing: 12) {
            // Icon badge using LiquidGlassBadge API
            LiquidGlassBadge(
                text: "Participants",
                icon: "person.2.fill",
                style: .info
            )
            
            Spacer()
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Participants Estimation")
    }
    
    // MARK: - Description Section
    
    private var descriptionSection: some View {
        Text("Help us plan better by estimating participant counts")
            .font(.subheadline)
            .foregroundColor(.secondary)
            .fixedSize(horizontal: false, vertical: true)
    }
    
    // MARK: - Input Fields Section
    
    private var inputFieldsSection: some View {
        VStack(spacing: 16) {
            // Minimum Participants
            minimumParticipantField
            
            // Divider
            LiquidGlassDivider(style: .subtle)
            
            // Maximum Participants
            maximumParticipantField
            
            // Divider
            LiquidGlassDivider(style: .subtle)
            
            // Expected Participants
            expectedParticipantField
        }
    }
    
    // MARK: - Minimum Participant Field
    
    private var minimumParticipantField: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 8) {
                Image(systemName: "person.3.fill")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(.wakevePrimary)
                
                Text("Minimum Participants")
                    .font(.subheadline.weight(.medium))
                    .foregroundColor(.primary)
            }
            
            participantTextField(
                label: "Minimum Participants",
                placeholder: "e.g., 5",
                text: $minText,
                icon: "person.3.fill",
                isError: minValue != nil && (minValue! < 1),
                errorMessage: minValue != nil && (minValue! < 1) ? "Must be at least 1" : nil,
                accessibilityLabel: "Minimum Participants",
                accessibilityHint: "Enter the minimum number of expected participants"
            ) { newValue in
                minParticipants = Int(newValue).flatMap { $0 > 0 ? $0 : nil }
            }
        }
    }
    
    // MARK: - Maximum Participant Field
    
    private var maximumParticipantField: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 8) {
                Image(systemName: "person.2.fill")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(.wakeveAccent)
                
                Text("Maximum Participants")
                    .font(.subheadline.weight(.medium))
                    .foregroundColor(.primary)
            }
            
            participantTextField(
                label: "Maximum Participants",
                placeholder: "e.g., 50",
                text: $maxText,
                icon: "person.2.fill",
                isError: !isMaxValid || (maxValue != nil && maxValue! < 1),
                errorMessage: !isMaxValid ? "Maximum must be ≥ minimum" : (maxValue != nil && maxValue! < 1 ? "Must be at least 1" : nil),
                accessibilityLabel: "Maximum Participants",
                accessibilityHint: "Enter the maximum number of participants allowed"
            ) { newValue in
                maxParticipants = Int(newValue).flatMap { $0 > 0 ? $0 : nil }
            }
        }
    }
    
    // MARK: - Expected Participant Field
    
    private var expectedParticipantField: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 8) {
                Image(systemName: "chart.line.uptrend.xyaxis")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(.wakeveSuccess)
                
                Text("Expected Participants")
                    .font(.subheadline.weight(.medium))
                    .foregroundColor(.primary)
                
                Spacer()
                
                // Warning badge if out of range
                if expectedOutOfRange {
                    LiquidGlassBadge(
                        text: "Outside range",
                        icon: "exclamationmark.triangle.fill",
                        style: .warning
                    )
                }
            }
            
            participantTextField(
                label: "Expected Participants",
                placeholder: "e.g., 20",
                text: $expectedText,
                icon: "chart.line.uptrend.xyaxis",
                isError: expectedValue != nil && expectedValue! < 1,
                errorMessage: expectedValue != nil && expectedValue! < 1 ? "Must be at least 1" : nil,
                warningMessage: expectedOutOfRange ? "Expected is outside min-max range" : nil,
                accessibilityLabel: "Expected Participants",
                accessibilityHint: "Enter the most likely number of participants"
            ) { newValue in
                expectedParticipants = Int(newValue).flatMap { $0 > 0 ? $0 : nil }
            }
        }
    }
    
    // MARK: - Helper Info Section
    
    private var helperInfoSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            // Divider
            LiquidGlassDivider(style: .subtle)
            
            HStack(alignment: .top, spacing: 12) {
                // Info icon with badge
                LiquidGlassBadge(
                    text: "Tip",
                    icon: "lightbulb.fill",
                    style: .info
                )
                
                Text(helperText)
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .transition(.opacity.combined(with: .scale(scale: 0.95)))
    }
    
    // MARK: - Helper Text
    
    private var helperText: String {
        var text = "This helps us suggest appropriate venues"
        if let expected = expectedValue {
            text += " and estimate costs for ~\(expected) people"
        }
        return text
    }
    
    // MARK: - Subviews
    
    /// Reusable text field for participant count input
    /// Uses design system colors and Liquid Glass styling
    @ViewBuilder
    private func participantTextField(
        label: String,
        placeholder: String,
        text: Binding<String>,
        icon: String,
        isError: Bool,
        errorMessage: String?,
        warningMessage: String? = nil,
        accessibilityLabel: String,
        accessibilityHint: String,
        onCommit: @escaping (String) -> Void
    ) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            // Label
            Text(label)
                .font(.subheadline.weight(.medium))
                .foregroundColor(.primary)
            
            // Text Field with Liquid Glass styling
            HStack(spacing: 12) {
                Image(systemName: icon)
                    .font(.system(size: 16, weight: .medium))
                    .foregroundColor(isError ? .red : .secondary)
                    .frame(width: 20)
                
                TextField(placeholder, text: text)
                    .font(.body)
                    .disabled(!enabled)
                    .onChange(of: text.wrappedValue) { _, newValue in
                        onCommit(newValue)
                    }
                    .accessibilityLabel(accessibilityLabel)
                    .accessibilityHint(accessibilityHint)
                    .accessibilityValue(text.wrappedValue.isEmpty ? "Empty" : text.wrappedValue)
#if os(iOS)
                    .keyboardType(.numberPad)
#endif
            }
            .padding(12)
            .background(.ultraThinMaterial)
            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .stroke(
                        isError ? Color.red.opacity(0.5) : Color.primary.opacity(0.1),
                        lineWidth: 1
                    )
            )
            
            // Error Message
            if let errorMessage = errorMessage {
                Label(errorMessage, systemImage: "exclamationmark.circle.fill")
                    .font(.caption)
                    .foregroundColor(.red)
                    .transition(.opacity.combined(with: .scale(scale: 0.95)))
            }
            
            // Warning Message with design system color
            if let warningMessage = warningMessage, errorMessage == nil {
                HStack(spacing: 6) {
                    Image(systemName: "exclamationmark.triangle.fill")
                        .font(.system(size: 12))
                        .foregroundColor(.wakeveWarning)
                    
                    Text(warningMessage)
                        .font(.caption)
                        .foregroundColor(.wakeveWarning)
                }
                .transition(.opacity.combined(with: .scale(scale: 0.95)))
            }
        }
    }
}

// MARK: - Previews

struct ParticipantsEstimationCard_Previews: PreviewProvider {
    static var previews: some View {
        ScrollView {
            VStack(spacing: 24) {
                // Preview 1: Empty state
                PreviewWrapper(
                    min: nil,
                    max: nil,
                    expected: nil,
                    title: "Empty State"
                )
                
                // Preview 2: Valid values
                PreviewWrapper(
                    min: 10,
                    max: 50,
                    expected: 30,
                    title: "Valid Values"
                )
                
                // Preview 3: Max < Min (error)
                PreviewWrapper(
                    min: 50,
                    max: 10,
                    expected: nil,
                    title: "Invalid: Max < Min"
                )
                
                // Preview 4: Expected out of range (warning)
                PreviewWrapper(
                    min: 10,
                    max: 20,
                    expected: 30,
                    title: "Warning: Expected Outside Range"
                )
                
                // Preview 5: Negative values (error)
                PreviewWrapper(
                    min: -5,
                    max: nil,
                    expected: nil,
                    title: "Error: Negative Value"
                )
                
                // Preview 6: Disabled state
                PreviewWrapper(
                    min: 10,
                    max: 50,
                    expected: 25,
                    title: "Disabled State",
                    enabled: false
                )
            }
            .padding()
        }
        .background(Color(red: 0.95, green: 0.95, blue: 0.97))
    }
    
    /// Helper wrapper for previews with state
    private struct PreviewWrapper: View {
        @State var minParticipants: Int?
        @State var maxParticipants: Int?
        @State var expectedParticipants: Int?
        let title: String
        var enabled: Bool = true
        
        init(min: Int?, max: Int?, expected: Int?, title: String, enabled: Bool = true) {
            _minParticipants = State(initialValue: min)
            _maxParticipants = State(initialValue: max)
            _expectedParticipants = State(initialValue: expected)
            self.title = title
            self.enabled = enabled
        }
        
        var body: some View {
            VStack(alignment: .leading, spacing: 8) {
                Text(title)
                    .font(.headline)
                    .foregroundColor(.secondary)
                    .padding(.horizontal, 4)
                
                ParticipantsEstimationCard(
                    minParticipants: $minParticipants,
                    maxParticipants: $maxParticipants,
                    expectedParticipants: $expectedParticipants,
                    enabled: enabled
                )
            }
        }
    }
}
