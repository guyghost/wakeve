import SwiftUI

/// Styles de bouton Liquid Glass disponibles
enum LiquidGlassButtonStyle {
    case primary
    case secondary
    case accent
    case icon
    case text
}

/// Bouton avec effet Liquid Glass selon le design system Wakeve
struct LiquidGlassButton: View {
    let title: String?
    let icon: String?
    let style: LiquidGlassButtonStyle
    let isEnabled: Bool
    let action: () -> Void
    
    init(
        title: String? = nil,
        icon: String? = nil,
        style: LiquidGlassButtonStyle = .primary,
        isEnabled: Bool = true,
        action: @escaping () -> Void
    ) {
        self.title = title
        self.icon = icon
        self.style = style
        self.isEnabled = isEnabled
        self.action = action
    }
    
    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                if let icon = icon {
                    Image(systemName: icon)
                        .font(.system(size: 16, weight: .medium))
                }
                if let title = title {
                    Text(title)
                        .font(.system(size: 16, weight: .semibold))
                }
            }
            .foregroundColor(foregroundColor)
            .padding(.horizontal, style == .icon ? 12 : 20)
            .padding(.vertical, style == .icon ? 12 : 14)
            .background(backgroundGradient)
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(borderGradient, lineWidth: 1)
            )
            .shadow(color: shadowColor, radius: 8, x: 0, y: 4)
            .opacity(isEnabled ? 1.0 : 0.6)
        }
        .disabled(!isEnabled)
    }
    
    // MARK: - Design System Colors
    
    private var foregroundColor: Color {
        switch style {
        case .primary:
            return .wakevPrimary
        case .secondary:
            return .wakevTextPrimary
        case .accent:
            return .wakevAccent
        case .icon, .text:
            return .wakevPrimary
        }
    }
    
    private var backgroundGradient: LinearGradient {
        switch style {
        case .primary:
            return LinearGradient(
                colors: [Color.white.opacity(0.9), Color.white.opacity(0.7)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        case .secondary, .icon, .text:
            return LinearGradient(
                colors: [Color.white.opacity(0.15), Color.white.opacity(0.08)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        case .accent:
            return LinearGradient(
                colors: [Color.wakevAccent.opacity(0.2), Color.wakevAccent.opacity(0.1)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        }
    }
    
    private var borderGradient: LinearGradient {
        LinearGradient(
            colors: [Color.white.opacity(0.4), Color.white.opacity(0.1)],
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }
    
    private var shadowColor: Color {
        switch style {
        case .primary:
            return .wakevPrimary.opacity(0.3)
        case .secondary, .icon, .text:
            return .clear
        case .accent:
            return .wakevAccent.opacity(0.3)
        }
    }
}

// MARK: - Preview

#Preview("Liquid Glass Button - Primary") {
    VStack(spacing: 20) {
        LiquidGlassButton(title: "Primary Button", style: .primary) {}
        
        LiquidGlassButton(title: "Secondary Button", style: .secondary) {}
        
        LiquidGlassButton(title: "Accent Button", style: .accent) {}
        
        HStack(spacing: 16) {
            LiquidGlassButton(icon: "heart.fill", style: .icon) {}
            
            LiquidGlassButton(icon: "bubble.right.fill", style: .icon) {}
            
            LiquidGlassButton(title: "Text", style: .text) {}
        }
    }
    .padding()
    .background(Color.wakevBackgroundDark)
}
