import SwiftUI

/// Badge avec effet Liquid Glass pour afficher des compteurs et indicateurs
struct LiquidGlassBadge: View {
    let count: Int
    let style: BadgeStyle
    
    enum BadgeStyle {
        case primary
        case accent
        case success
        case warning
        case error
        case neutral
    }
    
    var body: some View {
        if count > 0 {
            Text("\(count)")
                .font(.system(size: 12, weight: .bold))
                .foregroundColor(foregroundColor)
                .padding(.horizontal, 10)
                .padding(.vertical, 4)
                .background(backgroundGradient)
                .clipShape(Capsule())
                .overlay(
                    Capsule()
                        .stroke(borderGradient, lineWidth: 1)
                )
                .shadow(color: shadowColor, radius: 4, x: 0, y: 2)
        }
    }
    
    // MARK: - Design System Colors
    
    private var foregroundColor: Color {
        switch style {
        case .primary:
            return .wakevPrimary
        case .accent:
            return .wakevAccent
        case .success:
            return .wakevSuccess
        case .warning:
            return .wakevWarning
        case .error:
            return .wakevError
        case .neutral:
            return .wakevTextPrimary
        }
    }
    
    private var backgroundGradient: LinearGradient {
        switch style {
        case .primary:
            return LinearGradient(
                colors: [Color.white.opacity(0.95), Color.white.opacity(0.85)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        case .accent:
            return LinearGradient(
                colors: [Color.wakevAccent.opacity(0.2), Color.wakevAccent.opacity(0.1)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        case .success:
            return LinearGradient(
                colors: [Color.wakevSuccess.opacity(0.2), Color.wakevSuccess.opacity(0.1)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        case .warning:
            return LinearGradient(
                colors: [Color.wakevWarning.opacity(0.2), Color.wakevWarning.opacity(0.1)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        case .error:
            return LinearGradient(
                colors: [Color.wakevError.opacity(0.2), Color.wakevError.opacity(0.1)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        case .neutral:
            return LinearGradient(
                colors: [Color.white.opacity(0.2), Color.white.opacity(0.1)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        }
    }
    
    private var borderGradient: LinearGradient {
        LinearGradient(
            colors: [Color.white.opacity(0.5), Color.white.opacity(0.2)],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }
    
    private var shadowColor: Color {
        switch style {
        case .primary:
            return .wakevPrimary.opacity(0.25)
        case .accent:
            return .wakevAccent.opacity(0.25)
        case .success:
            return .wakevSuccess.opacity(0.25)
        case .warning:
            return .wakevWarning.opacity(0.25)
        case .error:
            return .wakevError.opacity(0.25)
        case .neutral:
            return .clear
        }
    }
}

// MARK: - Badge pour les commentaires (style spécialisé)

extension LiquidGlassBadge {
    /// Badge spécialisé pour les compteurs de commentaires
    static func comments(count: Int) -> LiquidGlassBadge {
        LiquidGlassBadge(count: count, style: .primary)
    }
    
    /// Badge spécialisé pour les notifications
    static func notification(count: Int) -> LiquidGlassBadge {
        LiquidGlassBadge(count: count, style: .error)
    }
}

// MARK: - Preview

#Preview("Liquid Glass Badge") {
    VStack(spacing: 20) {
        HStack(spacing: 16) {
            LiquidGlassBadge(count: 5, style: .primary)
            
            LiquidGlassBadge(count: 12, style: .accent)
            
            LiquidGlassBadge(count: 3, style: .success)
            
            LiquidGlassBadge(count: 99, style: .warning)
        }
        
        HStack(spacing: 16) {
            LiquidGlassBadge(count: 1, style: .error)
            
            LiquidGlassBadge(count: 42, style: .neutral)
            
            // Commentaires
            HStack(spacing: 8) {
                Image(systemName: "bubble.right.fill")
                    .foregroundColor(.wakevPrimary)
                
                LiquidGlassBadge.comments(count: 5)
            }
            .padding(12)
            .background(Color.white.opacity(0.1))
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
    }
    .padding()
    .background(Color.wakevBackgroundDark)
}
