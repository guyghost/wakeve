import SwiftUI

/// Badge style variants for LiquidGlassBadge
enum LiquidGlassBadgeStyle {
    case `default`      // Neutral gray
    case success       // Green for confirmed/finalized
    case warning       // Yellow/Orange for draft/polling
    case info          // Blue for polling/in-progress
    case accent        // Purple for comparing/special
}

/// LiquidGlassBadge - Standardized badge component with Liquid Glass effect
/// 
/// Usage:
/// ```swift
/// LiquidGlassBadge(text: "Confirmé", style: .success)
///
/// LiquidGlassBadge(
///     text: "Sondage",
///     icon: "chart.bar.fill",
///     style: .info
/// )
/// ```
struct LiquidGlassBadge: View {
    let text: String
    let icon: String?
    let style: LiquidGlassBadgeStyle
    
    init(
        text: String,
        icon: String? = nil,
        style: LiquidGlassBadgeStyle = .default
    ) {
        self.text = text
        self.icon = icon
        self.style = style
    }
    
    var body: some View {
        HStack(spacing: 4) {
            if let icon = icon {
                Image(systemName: icon)
                    .font(.caption2.weight(.medium))
            }
            Text(text)
                .font(.caption2.weight(.medium))
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 4)
        .foregroundColor(badgeColor)
        .background(badgeBackground)
        .overlay(badgeOverlay)
        .accessibilityLabel(text)
    }
    
    private var badgeColor: Color {
        switch style {
        case .default:
            return .secondary
        case .success:
            return .green
        case .warning:
            return .orange
        case .info:
            return .blue
        case .accent:
            return .purple
        }
    }
    
    private var badgeBackground: some View {
        RoundedRectangle(cornerRadius: 12)
            .fill(badgeColor.opacity(0.15))
    }
    
    private var badgeOverlay: some View {
        RoundedRectangle(cornerRadius: 12)
            .stroke(badgeColor.opacity(0.3), lineWidth: 0.5)
    }
}

/// Convenience badges for common event statuses
extension LiquidGlassBadge {
    /// Creates a badge for draft status (Brouillon)
    static func draft(text: String = "Brouillon") -> LiquidGlassBadge {
        LiquidGlassBadge(text: text, style: .warning)
    }
    
    /// Creates a badge for polling status (Sondage)
    static func polling(text: String = "Sondage") -> LiquidGlassBadge {
        LiquidGlassBadge(text: text, style: .info)
    }
    
    /// Creates a badge for comparing status (Comparaison)
    static func comparing(text: String = "Comparaison") -> LiquidGlassBadge {
        LiquidGlassBadge(text: text, icon: "arrow.left.arrow.right", style: .accent)
    }
    
    /// Creates a badge for confirmed status (Confirmé)
    static func confirmed(text: String = "Confirmé") -> LiquidGlassBadge {
        LiquidGlassBadge(text: text, icon: "checkmark.circle.fill", style: .success)
    }
    
    /// Creates a badge for organizing status (Organisation)
    static func organizing(text: String = "Organisation") -> LiquidGlassBadge {
        LiquidGlassBadge(text: text, style: .warning)
    }
    
    /// Creates a badge for finalized status (Finalisé)
    static func finalized(text: String = "Finalisé") -> LiquidGlassBadge {
        LiquidGlassBadge(text: text, icon: "checkmark.seal.fill", style: .success)
    }
    
    /// Creates a badge based on MockEventStatus
    static func from(status: MockEventStatus, customText: String? = nil) -> LiquidGlassBadge {
        switch status {
        case .draft:
            return .draft(text: customText ?? "Brouillon")
        case .polling:
            return .polling(text: customText ?? "Sondage")
        case .comparing:
            return .comparing(text: customText ?? "Comparaison")
        case .confirmed:
            return .confirmed(text: customText ?? "Confirmé")
        case .organizing:
            return .organizing(text: customText ?? "Organisation")
        case .finalized:
            return .finalized(text: customText ?? "Finalisé")
        }
    }
}

// MARK: - Previews
#Preview("LiquidGlassBadge - All Styles") {
    VStack(spacing: 12) {
        HStack(spacing: 8) {
            LiquidGlassBadge(text: "Brouillon", style: .warning)
            LiquidGlassBadge(text: "Sondage", style: .info)
            LiquidGlassBadge(text: "Comparaison", style: .accent)
        }
        
        HStack(spacing: 8) {
            LiquidGlassBadge(text: "Confirmé", style: .success)
            LiquidGlassBadge(text: "Organisation", style: .warning)
            LiquidGlassBadge(text: "Finalisé", style: .success)
        }
        
        HStack(spacing: 8) {
            LiquidGlassBadge(text: "Sondage", icon: "chart.bar.fill", style: .info)
            LiquidGlassBadge(text: "Confirmé", icon: "checkmark.circle.fill", style: .success)
        }
    }
    .padding()
}

#Preview("LiquidGlassBadge - Status Badges") {
    VStack(spacing: 12) {
        HStack(spacing: 8) {
            .draft()
            .polling()
            .comparing()
        }
        
        HStack(spacing: 8) {
            .confirmed()
            .organizing()
            .finalized()
        }
    }
    .padding()
}
