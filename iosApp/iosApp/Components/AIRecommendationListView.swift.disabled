import SwiftUI

// Import AI models from the shared models module
import Foundation

/// AI Recommendation List View - A scrollable list of AI suggestions with fluid animations
///
/// Displays multiple AI suggestions with staggered entrance animations and
/// smooth scrolling behavior.
///
/// Features:
/// - Staggered entrance animations
/// - Scrollable container
/// - Individual item animations
/// - Accept/Dismiss callbacks
/// - Empty state support
/// - Dynamic Type scaling
///
/// Example usage:
/// ```swift
/// let suggestions: [AISuggestion<DateRecommendation>] = [...]
/// AIRecommendationListView(
///     suggestions: suggestions,
///     onAccept: { id in acceptSuggestion(id) },
///     onDismiss: { id in dismissSuggestion(id) }
/// )
/// ```
struct AIRecommendationListView: View {
    let suggestions: [AISuggestion<DateRecommendation>]
    let onAccept: (String) -> Void
    let onDismiss: (String) -> Void
    var showEmptyState: Bool = true
    
    @State private var visibleItems = Set<String>()
    @State private var animationDelay = 0.0
    
    private let listAnimation = Animation.spring(response: 0.3, dampingFraction: 0.7)
    private let staggerDelay = 0.1
    
    var body: some View {
        Group {
            if suggestions.isEmpty && showEmptyState {
                emptyStateView
            } else {
                scrollViewContent
            }
        }
    }
    
    // MARK: - Scroll View Content
    
    private var scrollViewContent: some View {
        ScrollView {
            LazyVStack(spacing: 12) {
                ForEach(suggestions) { suggestion in
                    suggestionItem(for: suggestion)
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
        }
        .scrollIndicators(.hidden)
    }
    
    // MARK: - Suggestion Item
    
    private func suggestionItem(for suggestion: AISuggestion<DateRecommendation>) -> some View {
        AISuggestionCardView(
            suggestion: suggestion.data,
            metadata: suggestion.metadata,
            onAccept: { onAccept(suggestion.id) },
            onDismiss: { onDismiss(suggestion.id) }
        )
        .opacity(visibleItems.contains(suggestion.id) ? 1 : 0)
        .offset(y: visibleItems.contains(suggestion.id) ? 0 : 30)
        .animation(
            listAnimation,
            value: visibleItems.contains(suggestion.id)
        )
        .onAppear {
            withAnimation(
                listAnimation.delay(Double(visibleItems.count) * staggerDelay)
            ) {
                visibleItems.insert(suggestion.id)
            }
        }
    }
    
    // MARK: - Empty State
    
    private var emptyStateView: some View {
        VStack(spacing: 16) {
            Spacer()
            
            Image(systemName: "sparkles")
                .font(.system(size: 48))
                .foregroundColor(.secondary)
                .symbolEffect(.pulse, isActive: true)
            
            VStack(spacing: 8) {
                Text("No AI Recommendations")
                    .font(.title2)
                    .fontWeight(.semibold)
                
                Text("We don't have any suggestions yet. Create an event to get personalized recommendations.")
                    .font(.body)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)
            }
            
            Spacer()
        }
        .padding()
        .glassCard(cornerRadius: 20, material: .regularMaterial)
        .padding()
    }
}

// MARK: - Convenience Methods

extension AIRecommendationListView {
    /// Create mock suggestions for testing
    static func mockSuggestions(count: Int = 3) -> [AISuggestion<DateRecommendation>] {
        let dates = [
            ("Saturday, January 15, 2026", "Afternoon (2PM - 6PM)"),
            ("Sunday, January 16, 2026", "Morning (9AM - 12PM)"),
            ("Saturday, January 22, 2026", "Evening (6PM - 10PM)"),
            ("Friday, January 21, 2026", "All Day"),
            ("Sunday, January 17, 2026", "Afternoon (1PM - 5PM)")
        ]
        
        let reasons = [
            "Best weather forecast and highest participant availability",
            "Alternative option with slightly lower participant overlap",
            "Weekend option with good weather outlook",
            "Optimal timing based on historical data",
            "Flexible option with good participant feedback"
        ]
        
        return (0..<count).map { index in
            let dateInfo = dates[index % dates.count]
            let reason = reasons[index % reasons.count]
            
            return AISuggestion(
                id: "suggestion-\(index)",
                data: DateRecommendation(
                    date: dateInfo.0,
                    timeSlot: dateInfo.1,
                    reasoning: reason,
                    alternativeDates: nil
                ),
                metadata: AIMetadata.mock(
                    confidenceScore: Double.random(in: 0.6...0.98),
                    predictionSource: index % 3 == 0 ? .hybrid : .mlModel
                )
            )
        }
    }
}

// MARK: - Previews

#Preview("AIRecommendationListView - With Suggestions") {
    AIRecommendationListView(
        suggestions: AIRecommendationListView.mockSuggestions(count: 3),
        onAccept: { id in print("Accepted: \(id)") },
        onDismiss: { id in print("Dismissed: \(id)") }
    )
    .background(Color(red: 0.97, green: 0.97, blue: 0.98))
}

#Preview("AIRecommendationListView - Empty State") {
    AIRecommendationListView(
        suggestions: [],
        onAccept: { _ in },
        onDismiss: { _ in }
    )
    .background(Color(red: 0.97, green: 0.97, blue: 0.98))
}

#Preview("AIRecommendationListView - Many Suggestions") {
    AIRecommendationListView(
        suggestions: AIRecommendationListView.mockSuggestions(count: 5),
        onAccept: { id in print("Accepted: \(id)") },
        onDismiss: { id in print("Dismissed: \(id)") }
    )
    .background(Color(red: 0.97, green: 0.97, blue: 0.98))
}

#Preview("AIRecommendationListView - No Empty State") {
    VStack {
        Text("Custom empty state example")
            .font(.headline)
        
        AIRecommendationListView(
            suggestions: [],
            showEmptyState: false
        )
        
        Spacer()
    }
}
