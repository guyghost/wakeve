import SwiftUI

/// Button style variants for LiquidGlassButton
enum LiquidGlassButtonStyle {
    case primary      // Gradient background (wakevePrimary to wakeveAccent)
    case secondary    // Outline style with liquid glass
    case text         // Transparent background, just text
    case icon         // Icon-only button
}

/// Button size variants
enum LiquidGlassButtonSize {
    case small   // 36pt
    case medium  // 44pt
    case large   // 52pt

    var height: CGFloat {
        switch self {
        case .small: return 36
        case .medium: return 44
        case .large: return 52
        }
    }
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
/// // Secondary button with icon
/// LiquidGlassButton(title: "Annuler", icon: "xmark", style: .secondary) {
///     cancelAction()
/// }
/// ```
struct LiquidGlassButton: View {
    let title: String?
    let icon: String?
    let style: LiquidGlassButtonStyle
    let size: LiquidGlassButtonSize
    let isDisabled: Bool
    let action: () -> Void

    @State private var isPressed = false

    init(
        title: String,
        style: LiquidGlassButtonStyle = .primary,
        action: @escaping () -> Void
    ) {
        self.title = title
        self.icon = nil
        self.style = style
        self.size = .medium
        self.isDisabled = false
        self.action = action
    }

    init(
        title: String? = nil,
        icon: String? = nil,
        style: LiquidGlassButtonStyle = .primary,
        size: LiquidGlassButtonSize = .medium,
        isDisabled: Bool = false,
        action: @escaping () -> Void
    ) {
        self.title = title
        self.icon = icon
        self.style = style
        self.size = size
        self.isDisabled = isDisabled
        self.action = action
    }
    
    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                if let icon = icon {
                    Image(systemName: icon)
                        .font(.system(size: 16, weight: .semibold))
                }
                if let title = title {
                    Text(title)
                        .font(.subheadline.weight(.semibold))
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: size.height)
            .foregroundColor(isDisabled ? .gray : buttonForegroundColor)
            .background(isDisabled ? AnyView(Color.gray.opacity(0.1)) : AnyView(buttonBackground))
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            .overlay(buttonOverlay)
        }
        .buttonStyle(PlainButtonStyle())
        .disabled(isDisabled)
        .opacity(isDisabled ? 0.6 : 1.0)
        .scaleEffect(isPressed ? 0.98 : 1.0)
        .animation(.easeInOut(duration: 0.15), value: isPressed)
        .simultaneousGesture(
            DragGesture(minimumDistance: 0)
                .onChanged { _ in isPressed = true }
                .onEnded { _ in isPressed = false }
        )
        .accessibilityLabel(title ?? icon ?? "Button")
    }
    
    private var buttonForegroundColor: Color {
        switch style {
        case .primary:
            return .white
        case .secondary:
            return .wakevePrimary
        case .text:
            return .wakevePrimary
        case .icon:
            return .white
        }
    }
    
    private var buttonBackground: some View {
        Group {
            switch style {
            case .primary, .icon:
                LinearGradient(
                    gradient: Gradient(colors: [
                        Color.wakevePrimary,
                        Color.wakeveAccent
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
            case .primary, .icon:
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
                                Color.wakevePrimary.opacity(0.5),
                                Color.wakeveAccent.opacity(0.5)
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
        gradientColors: [Color] = [.wakevePrimary, .wakeveAccent],
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
