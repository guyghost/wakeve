import SwiftUI

/// AI Suggestion Card View - A liquid glass styled card for AI recommendations
///
/// Displays AI-generated suggestions with confidence scores, reasoning, and actions.
///
/// Features:
/// - LiquidGlassCard with consistent styling
/// - Dynamic confidence badge using LiquidGlassBadge
/// - Reasoning display with italic styling
/// - LiquidGlassButton actions (accept/dismiss)
/// - Spring animations on appear
/// - Full accessibility support
///
/// Example usage:
/// ```swift
/// let suggestion = DateRecommendation.mock()
/// let metadata = AIMetadata.mock()
/// AISuggestionCardView(
///     suggestion: suggestion,
///     metadata: metadata,
///     onAccept: { acceptSuggestion(id) },
///     onDismiss: { dismissSuggestion(id) }
/// )
/// ```
struct AISuggestionCardView: View {
    let suggestion: DateRecommendation
    let metadata: AIMetadata
    let onAccept: () -> Void
    let onDismiss: () -> Void
    
    @State private var isVisible = false
    @State private var isAccepting = false
    @State private var isDismissing = false
    
    private let animation = Animation.spring(response: 0.3, dampingFraction: 0.7)
    
    var body: some View {
        cardContent
            .opacity(isVisible ? 1 : 0)
            .offset(y: isVisible ? 0 : 20)
            .animation(animation, value: isVisible)
            .onAppear {
                withAnimation(animation.delay(0.1)) {
                    isVisible = true
                }
            }
    }
    
    // MARK: - Card Content (Imperative Shell)
    
    private var cardContent: some View {
        LiquidGlassCard(cornerRadius: 16, padding: 16) {
            VStack(alignment: .leading, spacing: 12) {
                headerSection
                
                LiquidGlassDivider(style: .subtle)
                
                contentSection
                
                if let reasoning = suggestion.reasoning {
                    reasoningSection(reasoning)
                }
                
                Spacer(minLength: 0)
                
                actionsSection
            }
        }
        .accessibilityLabel("AI Suggestion for \(suggestion.date)")
    }
    
    // MARK: - Header Section
    
    private var headerSection: some View {
        HStack(spacing: 8) {
            confidenceBadge
                .accessibilityHidden(true)
            
            Spacer()
            
            confidenceIndicator
                .accessibilityLabel(confidenceAccessibilityLabel)
        }
    }
    
    private var confidenceBadge: some View {
        LiquidGlassBadge(
            text: badgeText,
            icon: "sparkles",
            style: badgeStyle
        )
    }
    
    private var badgeText: String {
        switch metadata.confidenceLevel {
        case .veryHigh:
            return "Excellent Match"
        case .high:
            return "Great Match"
        case .medium:
            return "Good Match"
        case .low:
            return "Fair Match"
        case .veryLow:
            return "AI Suggestion"
        }
    }
    
    private var badgeStyle: LiquidGlassBadgeStyle {
        switch metadata.confidenceLevel {
        case .veryHigh:
            return .success
        case .high:
            return .info
        case .medium:
            return .warning
        case .low, .veryLow:
            return .accent
        }
    }
    
    private var confidenceIndicator: some View {
        HStack(spacing: 4) {
            Image(systemName: metadata.predictionSource.icon)
                .font(.caption2)
                .foregroundColor(confidenceColor)
            
            Text("\(Int(metadata.confidenceScore * 100))%")
                .font(.caption)
                .fontWeight(.medium)
                .foregroundColor(confidenceColor)
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 5)
        .background(
            Capsule()
                .fill(confidenceColor.opacity(0.12))
        )
        .overlay(
            Capsule()
                .stroke(confidenceColor.opacity(0.3), lineWidth: 0.5)
        )
    }
    
    private var confidenceColor: Color {
        switch metadata.confidenceLevel {
        case .veryHigh:
            return .wakevSuccess
        case .high:
            return .wakevPrimary
        case .medium:
            return .wakevWarning
        case .low:
            return .wakevError
        case .veryLow:
            return .secondary
        }
    }
    
    private var confidenceAccessibilityLabel: String {
        "\(Int(metadata.confidenceScore * 100)) percent confidence"
    }
    
    // MARK: - Content Section (Functional Core)
    
    private var contentSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            dateRecommendationRow
            timeSlotRow
        }
    }
    
    private var dateRecommendationRow: some View {
        HStack(spacing: 8) {
            Image(systemName: "calendar")
                .foregroundColor(.wakevPrimary)
                .font(.subheadline)
                .frame(width: 20)
            
            Text("Recommended: \(suggestion.date)")
                .font(.subheadline)
                .fontWeight(.medium)
                .foregroundColor(wakevTextPrimary)
        }
    }
    
    private var timeSlotRow: some View {
        HStack(spacing: 8) {
            Image(systemName: "clock")
                .foregroundColor(.wakevAccent)
                .font(.subheadline)
                .frame(width: 20)
            
            Text(suggestion.timeSlot)
                .font(.subheadline)
                .foregroundColor(wakevTextSecondary)
        }
    }
    
    // MARK: - Reasoning Section
    
    private func reasoningSection(_ reasoning: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: 4) {
                Image(systemName: "lightbulb.fill")
                    .font(.caption)
                    .foregroundColor(.wakevWarning)
                
                Text("Why this recommendation?")
                    .font(.caption)
                    .fontWeight(.medium)
                    .foregroundColor(wakevTextPrimary)
            }
            
            Text(reasoning)
                .font(.caption)
                .foregroundColor(wakevTextSecondary)
                .italic()
                .multilineTextAlignment(.leading)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(12)
        .background(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .fill(wakevSurface)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .stroke(wakevBorder.opacity(0.5), lineWidth: 0.5)
        )
    }
    
    // MARK: - Actions Section (Imperative Shell)
    
    private var actionsSection: some View {
        HStack(spacing: 12) {
            dismissButton
            acceptButton
        }
        .padding(.top, 8)
    }
    
    private var dismissButton: some View {
        LiquidGlassButton(
            title: "Dismiss",
            style: .secondary
        ) {
            handleDismiss()
        }
        .disabled(isDismissing || isAccepting)
        .opacity(isDismissing ? 0.5 : 1.0)
        .accessibilityLabel("Dismiss suggestion")
        .accessibilityHint("Rejects this AI recommendation")
    }
    
    private var acceptButton: some View {
        LiquidGlassButton(
            title: isAccepting ? "Accepting..." : "Accept",
            style: .primary
        ) {
            handleAccept()
        }
        .disabled(isDismissing || isAccepting)
        .opacity(isAccepting ? 0.5 : 1.0)
        .accessibilityLabel("Accept suggestion")
        .accessibilityHint("Confirms this AI recommendation")
    }
    
    // MARK: - Actions
    
    private func handleAccept() {
        withAnimation(animation) {
            isAccepting = true
        }
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            onAccept()
        }
    }
    
    private func handleDismiss() {
        withAnimation(animation) {
            isDismissing = true
        }
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
            onDismiss()
        }
    }
    
    // MARK: - Design System Colors
    
    private var wakevTextPrimary: Color {
        Color.wakevTextPrimaryLight
    }
    
    private var wakevTextSecondary: Color {
        Color.wakevTextSecondaryLight
    }
    
    private var wakevSurface: Color {
        Color.wakevSurfaceLight
    }
    
    private var wakevBorder: Color {
        Color.wakevBorderLight
    }
}

// MARK: - Preview

#Preview("AISuggestionCardView - High Confidence") {
    ScrollView {
        VStack(spacing: 20) {
            AISuggestionCardView(
                suggestion: DateRecommendation.mock(),
                metadata: AIMetadata.mock(confidenceScore: 0.92),
                onAccept: { print("Accepted") },
                onDismiss: { print("Dismissed") }
            )
            .padding(.horizontal)
            
            AISuggestionCardView(
                suggestion: DateRecommendation.mock(
                    date: "Sunday, January 16, 2026",
                    timeSlot: "Morning (9AM - 12PM)",
                    reasoning: "Alternative option with slightly lower participant overlap but better weather forecast"
                ),
                metadata: AIMetadata.mock(confidenceScore: 0.75),
                onAccept: { print("Accepted") },
                onDismiss: { print("Dismissed") }
            )
            .padding(.horizontal)
            
            AISuggestionCardView(
                suggestion: DateRecommendation.mock(
                    reasoning: nil
                ),
                metadata: AIMetadata.mock(confidenceScore: 0.55),
                onAccept: { print("Accepted") },
                onDismiss: { print("Dismissed") }
            )
            .padding(.horizontal)
        }
        .padding(.vertical)
    }
    .background(Color.wakevBackgroundLight)
}

#Preview("AISuggestionCardView - Variants") {
    VStack(spacing: 16) {
        AISuggestionCardView(
            suggestion: DateRecommendation.mock(),
            metadata: AIMetadata.mock(confidenceScore: 0.98),
            onAccept: {},
            onDismiss: {}
        )
        
        AISuggestionCardView(
            suggestion: DateRecommendation.mock(),
            metadata: AIMetadata.mock(
                confidenceScore: 0.65,
                predictionSource: .heuristic
            ),
            onAccept: {},
            onDismiss: {}
        )
        
        AISuggestionCardView(
            suggestion: DateRecommendation.mock(),
            metadata: AIMetadata.mock(
                confidenceScore: 0.82,
                predictionSource: .hybrid
            ),
            onAccept: {},
            onDismiss: {}
        )
    }
    .padding()
    .background(Color.wakevBackgroundLight)
}
