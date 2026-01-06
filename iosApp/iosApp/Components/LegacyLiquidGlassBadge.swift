import SwiftUI

/// Liquid Glass Badge Component (Legacy)
///
/// A reusable badge component following Apple's Liquid Glass guidelines.
/// Displays status indicators, counts, and information labels with 5 colors.
///
/// **Note:** This is the legacy version. Consider using `LiquidGlassBadge` for new code.
///
/// ## Features
/// - 5 badge colors (primary, accent, success, warning, error)
/// - 3 size variants (small, medium, large)
/// - Icon support with automatic sizing
/// - Dot style for minimal badges
/// - Automatic color adaptation based on badge type
///
/// ## Usage Examples
/// ```swift
/// // Status badges
/// LegacyLiquidGlassBadge(
///     text: "Draft",
///     type: .primary,
///     size: .small
/// )
///
/// LegacyLiquidGlassBadge(
///     text: "Confirmed",
///     type: .success,
///     size: .medium
/// )
///
/// LegacyLiquidGlassBadge(
///     text: "Warning",
///     type: .warning,
///     size: .medium
/// )
///
/// // Count badges
/// LegacyLiquidGlassBadge(
///     text: "12",
///     type: .primary,
///     size: .large
/// )
///
/// // Icon badges
/// LegacyLiquidGlassBadge(
///     icon: "checkmark.circle.fill",
///     type: .success,
///     size: .medium
/// )
///
/// // Dot badge (minimal)
/// LegacyLiquidGlassBadge(
///     type: .dot,
///     dotSize: .small,
///     dotColor: .primary
/// )
/// ```

struct LegacyLiquidGlassBadge: View {
    let text: String?
    let icon: String?
    let type: BadgeType
    let size: BadgeSize
    let dotSize: DotSize?
    let dotColor: BadgeColor?
    
    // MARK: - Badge Types
    
    enum BadgeType {
        /// Primary blue badge (wakevPrimary #2563EB)
        case primary
        
        /// Accent purple badge (wakevAccent #7C3AED)
        case accent
        
        /// Success green badge (wakevSuccess #059669)
        case success
        
        /// Warning orange badge (wakevWarning #D97706)
        case warning
        
        /// Error red badge (wakevError #DC2626)
        case error
        
        /// Minimal dot badge
        case dot
    }
    
    enum BadgeSize {
        /// Small badge - 20dp height
        case small
        
        /// Medium badge - 24dp height
        case medium
        
        /// Large badge - 32dp height
        case large
    }
    
    enum DotSize {
        /// Extra small dot - 6dp
        case extraSmall
        
        /// Small dot - 8dp
        case small
        
        /// Medium dot - 10dp
        case medium
    }
    
    enum BadgeColor {
        case primary
        case accent
        case success
        case warning
        case error
        case dot
    }
    
    // MARK: - Computed Properties
    
    private var badgeHeight: CGFloat {
        switch size {
        case .small: return 20
        case .medium: return 24
        case .large: return 32
        }
    }
    
    private var fontSize: Font {
        switch size {
        case .small: return .caption.weight(.semibold)
        case .medium: return .caption.weight(.medium)
        case .large: return .body.weight(.semibold)
        }
    }
    
    private var iconSize: CGFloat {
        switch size {
        case .small: return 10
        case .medium: return 14
        case .large: return 18
        }
    }
    
    private var badgeColor: Color {
        switch type {
        case .primary: return Color.wakevPrimary
        case .accent: return Color.wakevAccent
        case .success: return Color.wakevSuccess
        case .warning: return Color.wakevWarning
        case .error: return Color.wakevError
        case .dot: return resolvedDotColor
        }
    }
    
    private var backgroundColor: Color {
        switch type {
        case .primary: return Color.wakevPrimary.opacity(0.15)
        case .accent: return Color.wakevAccent.opacity(0.15)
        case .success: return Color.wakevSuccess.opacity(0.15)
        case .warning: return Color.wakevWarning.opacity(0.15)
        case .error: return Color.wakevError.opacity(0.15)
        case .dot: return resolvedDotColor
        }
    }
    
    private var textColor: Color {
        switch type {
        case .primary: return .white
        case .accent: return .white
        case .success: return .white
        case .warning: return .white
        case .error: return .white
        case .dot: return resolvedDotColor
        }
    }
    
    /// Resolves the dotColor enum to an actual Color
    private var resolvedDotColor: Color {
        guard let dotColor = dotColor else { return .secondary }
        switch dotColor {
        case .primary: return Color.wakevPrimary
        case .accent: return Color.wakevAccent
        case .success: return Color.wakevSuccess
        case .warning: return Color.wakevWarning
        case .error: return Color.wakevError
        case .dot: return .secondary
        }
    }
    
    // MARK: - Body
    
    var body: some View {
        if type == .dot {
            dotBadge
        } else {
            fullBadge
        }
    }
    
    // MARK: - Badge Views
    
    private var dotBadge: some View {
        Circle()
            .fill(resolvedDotColor)
            .frame(width: dotSizeValue, height: dotSizeValue)
    }
    
    @ViewBuilder
    private var fullBadge: some View {
        HStack(spacing: 4) {
            // Icon
            if let icon = icon {
                Image(systemName: icon)
                    .font(.system(size: iconSize, weight: .semibold))
                    .foregroundColor(textColor)
            }
            
            // Text or count
            if let text = text {
                Text(text)
                    .font(fontSize)
                    .foregroundColor(textColor)
                    .fontWeight(.semibold)
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 4)
        .frame(height: badgeHeight)
        .background(backgroundColor)
        .clipShape(RoundedRectangle(cornerRadius: badgeHeight / 2, style: .continuous))
        .shadow(
            color: badgeColor.opacity(0.2),
            radius: 4,
            x: 0,
            y: 2
        )
    }
    
    // MARK: - Helper
    
    private var dotSizeValue: CGFloat {
        switch dotSize {
        case .extraSmall: return 6
        case .small: return 8
        case .medium: return 10
        }
    }
}

// MARK: - Preview

struct LegacyLiquidGlassBadge_Previews: PreviewProvider {
    static var previews: some View {
        ScrollView {
            VStack(spacing: 24) {
                Text("Status Badges")
                    .font(.headline)
                
                HStack(spacing: 12) {
                    LegacyLiquidGlassBadge(
                        text: "Draft",
                        type: .primary,
                        size: .small
                    )
                    
                    LegacyLiquidGlassBadge(
                        text: "Polling",
                        type: .accent,
                        size: .small
                    )
                    
                    LegacyLiquidGlassBadge(
                        text: "Confirmed",
                        type: .success,
                        size: .small
                    )
                    
                    LegacyLiquidGlassBadge(
                        text: "Warning",
                        type: .warning,
                        size: .small
                    )
                    
                    LegacyLiquidGlassBadge(
                        text: "Error",
                        type: .error,
                        size: .small
                    )
                }
                
                Divider()
                    .padding(.vertical)
                
                Text("Icon Badges")
                    .font(.headline)
                
                HStack(spacing: 12) {
                    LegacyLiquidGlassBadge(
                        icon: "checkmark.circle.fill",
                        type: .success,
                        size: .medium
                    )
                    
                    LegacyLiquidGlassBadge(
                        icon: "clock.fill",
                        type: .warning,
                        size: .medium
                    )
                    
                    LegacyLiquidGlassBadge(
                        icon: "exclamationmark.triangle.fill",
                        type: .error,
                        size: .medium
                    )
                    
                    LegacyLiquidGlassBadge(
                        icon: "star.fill",
                        type: .accent,
                        size: .medium
                    )
                }
                
                Divider()
                    .padding(.vertical)
                
                Text("Count Badges")
                    .font(.headline)
                
                HStack(spacing: 12) {
                    LegacyLiquidGlassBadge(
                        text: "3",
                        type: .primary,
                        size: .medium
                    )
                    
                    LegacyLiquidGlassBadge(
                        text: "12",
                        type: .success,
                        size: .medium
                    )
                    
                    LegacyLiquidGlassBadge(
                        text: "99+",
                        type: .warning,
                        size: .medium
                    )
                }
                
                Divider()
                    .padding(.vertical)
                
                Text("Large Badges")
                    .font(.headline)
                
                HStack(spacing: 12) {
                    LegacyLiquidGlassBadge(
                        text: "In Progress",
                        type: .primary,
                        size: .large
                    )
                    
                    LegacyLiquidGlassBadge(
                        text: "Confirmed",
                        type: .success,
                        size: .large
                    )
                    
                    LegacyLiquidGlassBadge(
                        text: "Completed",
                        type: .accent,
                        size: .large
                    )
                }
                
                Divider()
                    .padding(.vertical)
                
                Text("Dot Badges")
                    .font(.headline)
                
                HStack(spacing: 12) {
                    LegacyLiquidGlassBadge(
                        type: .dot,
                        dotSize: .small,
                        dotColor: .primary
                    )
                    
                    LegacyLiquidGlassBadge(
                        type: .dot,
                        dotSize: .medium,
                        dotColor: .success
                    )
                    
                    LegacyLiquidGlassBadge(
                        type: .dot,
                        dotSize: .medium,
                        dotColor: .warning
                    )
                    
                    LegacyLiquidGlassBadge(
                        type: .dot,
                        dotSize: .extraSmall,
                        dotColor: .error
                    )
                }
            }
            .padding()
        }
    }
}
