import Foundation
import SwiftUI

/// Suggestion models - Swift equivalents to Kotlin models for predictive features
///
/// These models will eventually be synced with Kotlin/Native interop but serve
/// as the Swift source of truth for UI development and testing.
///
/// Matches Kotlin models in shared/src/commonMain/kotlin/com/guyghost/wakeve/domain/model/ai/

// MARK: - AI Badge Models

/// Represents an AI-generated badge with display properties and metadata
struct AIBadge: Identifiable, Codable, Hashable {
    let id: String
    let type: AIBadgeType
    let displayName: String
    let icon: String
    let colorHex: String
    let tooltip: String?
    
    /// Computed SwiftUI Color from hex string
    var color: Color {
        Color(hex: colorHex)
    }
    
    /// Default badges for common AI scenarios
    static let defaults: [AIBadgeType: AIBadge] = [
        .aiSuggestion: AIBadge(
            id: "ai-suggestion",
            type: .aiSuggestion,
            displayName: "Suggestion",
            icon: "✦",
            colorHex: "#2563EB",
            tooltip: "Proposition locale à relire avant application"
        ),
        .highConfidence: AIBadge(
            id: "high-confidence",
            type: .highConfidence,
            displayName: "Solide",
            icon: "✓",
            colorHex: "#2F855A",
            tooltip: "Suggestion cohérente avec les informations visibles"
        ),
        .mediumConfidence: AIBadge(
            id: "medium-confidence",
            type: .mediumConfidence,
            displayName: "À vérifier",
            icon: "…",
            colorHex: "#B7791F",
            tooltip: "Suggestion utile, à compléter avant usage"
        ),
        .personalized: AIBadge(
            id: "personalized",
            type: .personalized,
            displayName: "Adapté",
            icon: "✦",
            colorHex: "#0F766E",
            tooltip: "Suggestion adaptée au contexte de cet événement"
        ),
        .popularChoice: AIBadge(
            id: "popular-choice",
            type: .popularChoice,
            displayName: "Souvent choisi",
            icon: "↑",
            colorHex: "#C2410C",
            tooltip: "Option souvent retenue dans ce type d'organisation"
        ),
        .seasonal: AIBadge(
            id: "seasonal",
            type: .seasonal,
            displayName: "Saisonnier",
            icon: "◐",
            colorHex: "#64748B",
            tooltip: "Suggestion liée à la période de l'événement"
        )
    ]
    
    /// Factory method to create badge from type
    static func from(type: AIBadgeType) -> AIBadge {
        defaults[type] ?? AIBadge(
            id: type.rawValue.lowercased(),
            type: type,
            displayName: type.rawValue.replacingOccurrences(of: "_", with: " "),
            icon: "✦",
            colorHex: "#2563EB",
            tooltip: "Proposition à relire avant application"
        )
    }
}

/// Types of AI badges for different recommendation contexts
enum AIBadgeType: String, Codable, CaseIterable, Hashable {
    case aiSuggestion = "AI_SUGGESTION"
    case highConfidence = "HIGH_CONFIDENCE"
    case mediumConfidence = "MEDIUM_CONFIDENCE"
    case personalized = "PERSONALIZED"
    case popularChoice = "POPULAR_CHOICE"
    case seasonal = "SEASONAL"
    
    /// Human-readable display name
    var displayName: String {
        switch self {
        case .aiSuggestion:
            return "Suggestion"
        case .highConfidence:
            return "Solide"
        case .mediumConfidence:
            return "À vérifier"
        case .personalized:
            return "Adapté"
        case .popularChoice:
            return "Souvent choisi"
        case .seasonal:
            return "Saisonnier"
        }
    }
    
    /// Default icon for this badge type
    var defaultIcon: String {
        switch self {
        case .aiSuggestion:
            return "✦"
        case .highConfidence:
            return "✓"
        case .mediumConfidence:
            return "…"
        case .personalized:
            return "✦"
        case .popularChoice:
            return "↑"
        case .seasonal:
            return "◐"
        }
    }
    
    /// Default color hex for this badge type
    var defaultColorHex: String {
        switch self {
        case .aiSuggestion:
            return "#2563EB"
        case .highConfidence:
            return "#2F855A"
        case .mediumConfidence:
            return "#B7791F"
        case .personalized:
            return "#0F766E"
        case .popularChoice:
            return "#C2410C"
        case .seasonal:
            return "#64748B"
        }
    }
}

// MARK: - AI Metadata

/// Metadata about AI predictions including confidence and source information
struct AIMetadata: Codable, Hashable {
    let confidenceScore: Double
    let predictionSource: PredictionSource
    let modelVersion: String
    let featuresUsed: [String: String]
    let createdAt: String
    
    /// Human-readable confidence level
    var confidenceLevel: ConfidenceLevel {
        switch confidenceScore {
        case 0.9...1.0:
            return .veryHigh
        case 0.7..<0.9:
            return .high
        case 0.5..<0.7:
            return .medium
        case 0.3..<0.5:
            return .low
        default:
            return .veryLow
        }
    }
    
    /// Formatted confidence percentage
    var confidencePercentage: String {
        "\(Int(confidenceScore * 100))%"
    }
    
    /// Convenience for creating mock metadata
    static func mock(
        confidenceScore: Double = 0.85,
        predictionSource: PredictionSource = .mlModel,
        modelVersion: String = "2.1.0",
        featuresUsed: [String: String] = [:]
    ) -> AIMetadata {
        AIMetadata(
            confidenceScore: confidenceScore,
            predictionSource: predictionSource,
            modelVersion: modelVersion,
            featuresUsed: featuresUsed,
            createdAt: ISO8601DateFormatter().string(from: Date())
        )
    }
}

/// Confidence level enum for display purposes
enum ConfidenceLevel: String, Codable, CaseIterable {
    case veryHigh = "Very High"
    case high = "High"
    case medium = "Medium"
    case low = "Low"
    case veryLow = "Very Low"
    
    /// Color based on confidence level
    var color: Color {
        switch self {
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
}

/// Source of AI prediction
enum PredictionSource: String, Codable, Hashable {
    case mlModel = "ML_MODEL"
    case heuristic = "HEURISTIC"
    case hybrid = "HYBRID"
    
    /// Human-readable display name
    var displayName: String {
        switch self {
        case .mlModel:
            return "ML Model"
        case .heuristic:
            return "Heuristic"
        case .hybrid:
            return "Hybrid"
        }
    }
    
    /// Icon for this prediction source
    var icon: String {
        switch self {
        case .mlModel:
            return "brain.head.profile"
        case .heuristic:
            return "lightbulb"
        case .hybrid:
            return "sparkles"
        }
    }
}

// MARK: - Generic Suggestion Wrapper

/// Generic wrapper for AI suggestions containing data and metadata
struct AISuggestion<T: Hashable>: Identifiable, Hashable {
    let id: String
    let data: T
    let metadata: AIMetadata
    
    /// Initialize with data and metadata
    init(id: String = UUID().uuidString, data: T, metadata: AIMetadata) {
        self.id = id
        self.data = data
        self.metadata = metadata
    }
}

// MARK: - Date Recommendation Models

/// AI recommendation for event date/time
struct DateRecommendation: Hashable, Identifiable {
    var id: String { "\(date)-\(timeSlot)" }
    let date: String
    let timeSlot: String
    let reasoning: String?
    let alternativeDates: [String]?
    
    /// Create mock date recommendation
    static func mock(
        date: String = "Saturday, January 15, 2026",
        timeSlot: String = "Afternoon (2PM - 6PM)",
        reasoning: String? = "Best weather forecast and highest participant availability",
        alternativeDates: [String]? = nil
    ) -> DateRecommendation {
        DateRecommendation(
            date: date,
            timeSlot: timeSlot,
            reasoning: reasoning,
            alternativeDates: alternativeDates
        )
    }
}

// MARK: - Location Recommendation Models

/// AI recommendation for event location
struct LocationRecommendation: Hashable, Identifiable {
    var id: String { name }
    let name: String
    let type: LocationType
    let address: String
    let reasoning: String?
    let priceEstimate: String?
    
    enum LocationType: String, Hashable {
        case venue = "Venue"
        case restaurant = "Restaurant"
        case outdoor = "Outdoor"
        case virtual = "Virtual"
    }
    
    /// Create mock location recommendation
    static func mock(
        name: String = "Sunset Beach",
        type: LocationType = .outdoor,
        address: String = "123 Beach Road, Malibu",
        reasoning: String? = "Perfect weather and scenic views",
        priceEstimate: String? = "Free"
    ) -> LocationRecommendation {
        LocationRecommendation(
            name: name,
            type: type,
            address: address,
            reasoning: reasoning,
            priceEstimate: priceEstimate
        )
    }
}

// MARK: - Previews

#Preview("AI Badge Models") {
    VStack(spacing: 16) {
        ForEach(AIBadgeType.allCases, id: \.self) { type in
            let badge = AIBadge.from(type: type)
            HStack {
                Text(badge.icon)
                    .font(.title2)
                VStack(alignment: .leading) {
                    Text(badge.displayName)
                        .font(.headline)
                    Text(badge.tooltip ?? "")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                Spacer()
            }
            .padding()
            .glassCard()
        }
    }
    .padding()
}

#Preview("AIMetadata Confidence Levels") {
    VStack(spacing: 16) {
        ForEach([0.95, 0.75, 0.55, 0.35], id: \.self) { score in
            let metadata = AIMetadata.mock(confidenceScore: score)
            HStack {
                Text(metadata.confidencePercentage)
                    .font(.headline)
                    .foregroundColor(metadata.confidenceLevel.color)
                Text(metadata.predictionSource.displayName)
                    .font(.caption)
                Spacer()
            }
            .padding()
            .glassCard()
        }
    }
    .padding()
}
