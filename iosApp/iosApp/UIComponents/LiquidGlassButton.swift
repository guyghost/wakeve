import SwiftUI

/// Button style variants for LiquidGlassButton
enum LiquidGlassButtonStyle {
    case primary      // Gradient background (wakevPrimary to wakevAccent)
    case secondary    // Outline style with liquid glass
    case text         // Transparent background, just text
}

/// LiquidGlassButton - Standardized button component with Liquid Glass effect
/// 
/// Usage:
/// ```swift
/// // Primary button
/// LiquidGlassButton(title: "Confirmer", style: .primary) {
///     confirmAction()
/// }
///
/// // Secondary button
/// LiquidGlassButton(title: "Annuler", style: .secondary) {
///     cancelAction()
/// }
/// ```
struct LiquidGlassButton: View {
    let title: String
    let style: LiquidGlassButtonStyle
    let action: () -> Void
    
    @State private var isPressed = false
    
    init(
        title: String,
        style: LiquidGlassButtonStyle = .primary,
        action: @escaping () -> Void
    ) {
        self.title = title
        self.style = style
        self.action = action
    }
    
    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                Text(title)
                    .font(.subheadline.weight(.semibold))
            }
            .frame(maxWidth: .infinity)
            .frame(height: 44)
            .foregroundColor(buttonForegroundColor)
            .background(buttonBackground)
            .overlay(buttonOverlay)
        }
        .buttonStyle(PlainButtonStyle())
        .scaleEffect(isPressed ? 0.98 : 1.0)
        .animation(.easeInOut(duration: 0.15), value: isPressed)
        .simultaneousGesture(
            DragGesture(minimumDistance: 0)
                .onChanged { _ in isPressed = true }
                .onEnded { _ in isPressed = false }
        )
        .accessibilityLabel(title)
    }
    
    private var buttonForegroundColor: Color {
        switch style {
        case .primary:
            return .white
        case .secondary:
            return .wakevPrimary
        case .text:
            return .wakevPrimary
        }
    }
    
    private var buttonBackground: some View {
        Group {
            switch style {
            case .primary:
                LinearGradient(
                    gradient: Gradient(colors: [
                        Color.wakevPrimary,
                        Color.wakevAccent
                    ]),
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
            case .secondary:
                Color.clear
            case .text:
                Color.clear
            }
        }
    }
    
    private var buttonOverlay: some View {
        Group {
            switch style {
            case .primary:
                RoundedRectangle(cornerRadius: 12)
                    .stroke(
                        LinearGradient(
                            gradient: Gradient(colors: [
                                Color.white.opacity(0.3),
                                Color.white.opacity(0.1)
                            ]),
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        ),
                        lineWidth: 1
                    )
            case .secondary:
                RoundedRectangle(cornerRadius: 12)
                    .stroke(
                        LinearGradient(
                            gradient: Gradient(colors: [
                                Color.wakevPrimary.opacity(0.5),
                                Color.wakevAccent.opacity(0.5)
                            ]),
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        ),
                        lineWidth: 1.5
                    )
                    .liquidGlass(cornerRadius: 12, opacity: 0.6, intensity: 0.8)
            case .text:
                EmptyView()
            }
        }
    }
}

/// Icon button variant for floating action buttons and small actions
struct LiquidGlassIconButton: View {
    let icon: String
    let size: CGFloat
    let gradientColors: [Color]
    let action: () -> Void
    
    @State private var isPressed = false
    
    init(
        icon: String,
        size: CGFloat = 56,
        gradientColors: [Color] = [.wakevPrimary, .wakevAccent],
        action: @escaping () -> Void
    ) {
        self.icon = icon
        self.size = size
        self.gradientColors = gradientColors
        self.action = action
    }
    
    var body: some View {
        Button(action: action) {
            Image(systemName: icon)
                .font(.system(size: size == 56 ? 20 : 16, weight: .semibold))
                .foregroundColor(.white)
                .frame(width: size, height: size)
                .background(
                    Circle()
                        .fill(
                            LinearGradient(
                                gradient: Gradient(colors: gradientColors),
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                )
                .shadow(
                    color: gradientColors.first?.opacity(0.3) ?? .clear,
                    radius: 12,
                    x: 0,
                    y: 6
                )
        }
        .scaleEffect(isPressed ? 0.95 : 1.0)
        .animation(.easeInOut(duration: 0.15), value: isPressed)
        .simultaneousGesture(
            DragGesture(minimumDistance: 0)
                .onChanged { _ in isPressed = true }
                .onEnded { _ in isPressed = false }
        )
    }
}

// MARK: - Previews
#Preview("LiquidGlassButton - Primary") {
    LiquidGlassButton(title: "Confirmer", style: .primary) {
        print("Primary button tapped")
    }
    .padding()
}

#Preview("LiquidGlassButton - Secondary") {
    LiquidGlassButton(title: "Annuler", style: .secondary) {
        print("Secondary button tapped")
    }
    .padding()
}

#Preview("LiquidGlassButton - Text") {
    LiquidGlassButton(title: "En savoir plus", style: .text) {
        print("Text button tapped")
    }
    .padding()
}

#Preview("LiquidGlassIconButton") {
    VStack(spacing: 20) {
        LiquidGlassIconButton(icon: "plus") {
            print("FAB tapped")
        }
        
        LiquidGlassIconButton(icon: "checkmark", size: 44) {
            print("Small icon button tapped")
        }
    }
    .padding()
}
