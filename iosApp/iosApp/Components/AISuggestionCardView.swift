import SwiftUI

/// AI Suggestion Card View - A liquid glass styled card for AI recommendations
///
/// Displays AI-generated suggestions with confidence scores, reasoning, and actions.
///
/// Features:
/// - Liquid Glass card with regular material
/// - Dynamic confidence badge
/// - Reasoning display with italic styling
/// - Accept/Dismiss actions
/// - Spring animations on appear
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
    
    // MARK: - Card Content
    
    private var cardContent: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Header with badge and confidence
            headerSection
            
            Divider()
                .opacity(0.5)
            
            // Content section
            contentSection
            
            // Reasoning (if available)
            if let reasoning = suggestion.reasoning {
                reasoningSection(reasoning)
            }
            
            Spacer(minLength: 0)
            
            // Actions
            actionsSection
        }
        .padding(16)
        .glassCard(cornerRadius: 16, material: .regularMaterial)
    }
    
    // MARK: - Header Section
    
    private var headerSection: some View {
        HStack(spacing: 8) {
            // AI Badge based on confidence
            AIBadgeView(badge: badgeForConfidence(metadata.confidenceScore))
            
            Spacer()
            
            // Confidence indicator
            confidenceIndicator
        }
    }
    
    private var confidenceIndicator: some View {
        HStack(spacing: 4) {
            Image(systemName: metadata.predictionSource.icon)
                .font(.caption2)
            
            Text("\(Int(metadata.confidenceScore * 100))% confidence")
                .font(.caption)
                .fontWeight(.medium)
                .foregroundColor(confidenceColor)
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 4)
        .background(
            Capsule()
                .fill(confidenceColor.opacity(0.1))
        )
        .continuousCornerRadius(8)
    }
    
    private var confidenceColor: Color {
        switch metadata.confidenceLevel {
        case .veryHigh:
            return .green
        case .high:
            return .blue
        case .medium:
            return .orange
        case .low:
            return .red
        case .veryLow:
            return .gray
        }
    }
    
    // MARK: - Content Section
    
    private var contentSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            // Date recommendation
            HStack(spacing: 8) {
                Image(systemName: "calendar")
                    .foregroundColor(.blue)
                    .font(.subheadline)
                
                Text("Recommended: \(suggestion.date)")
                    .font(.subheadline)
                    .fontWeight(.medium)
            }
            
            // Time slot
            HStack(spacing: 8) {
                Image(systemName: "clock")
                    .foregroundColor(.blue)
                    .font(.subheadline)
                
                Text(suggestion.timeSlot)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
        }
    }
    
    // MARK: - Reasoning Section
    
    private func reasoningSection(_ reasoning: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: 4) {
                Image(systemName: "lightbulb.fill")
                    .font(.caption)
                    .foregroundColor(.yellow)
                
                Text("Why this recommendation?")
                    .font(.caption)
                    .fontWeight(.medium)
            }
            
            Text(reasoning)
                .font(.caption)
                .foregroundColor(.secondary)
                .italic()
                .multilineTextAlignment(.leading)
        }
        .padding(12)
        .background(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .fill(Color(.tertiarySystemFill))
        )
    }
    
    // MARK: - Actions Section
    
    private var actionsSection: some View {
        HStack(spacing: 12) {
            // Dismiss button
            Button(action: handleDismiss) {
                HStack(spacing: 4) {
                    Image(systemName: "xmark")
                    Text("Dismiss")
                }
                .font(.callout)
                .frame(maxWidth: .infinity)
                .frame(height: 44)
            }
            .buttonStyle(.bordered)
            .disabled(isDismissing || isAccepting)
            .opacity(isDismissing ? 0.5 : 1)
            
            // Accept button
            Button(action: handleAccept) {
                HStack(spacing: 4) {
                    if isAccepting {
                        ProgressView()
                            .progressViewStyle(CircularProgressViewStyle(tint: .white))
                    } else {
                        Image(systemName: "checkmark")
                        Text("Accept")
                    }
                }
                .font(.callout)
                .fontWeight(.semibold)
                .frame(maxWidth: .infinity)
                .frame(height: 44)
            }
            .buttonStyle(.borderedProminent)
            .disabled(isDismissing || isAccepting)
            .opacity(isAccepting ? 0.5 : 1)
        }
        .padding(.top, 8)
    }
    
    // MARK: - Badge Factory
    
    private func badgeForConfidence(_ score: Double) -> AIBadge {
        switch score {
        case 0.9...1.0:
            return AIBadge.from(type: .highConfidence)
        case 0.7..<0.9:
            return AIBadge.from(type: .mediumConfidence)
        default:
            return AIBadge.from(type: .aiSuggestion)
        }
    }
    
    // MARK: - Actions
    
    private func handleAccept() {
        withAnimation(animation) {
            isAccepting = true
        }
        
        // Simulate async action
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
}

// MARK: - Preview

#Preview("AISuggestionCardView") {
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
    .background(Color(red: 0.97, green: 0.97, blue: 0.98))
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
    .glassCard()
    .padding()
}
