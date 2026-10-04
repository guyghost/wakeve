import SwiftUI

// MARK: - Liquid Glass Modifier
///
/// Modifier for Liquid Glass effect.
///
/// Uses the native iOS 26+ `.glassEffect()` API with a
/// `.regularMaterial` fallback for earlier versions.
struct LiquidGlassModifier: ViewModifier {
    @Environment(\.accessibilityReduceTransparency) private var reduceTransparency

    var cornerRadius: CGFloat = 20

    @ViewBuilder
    func body(content: Content) -> some View {
        if #available(iOS 26.0, *), !reduceTransparency {
            content
                .glassEffect(.regular, in: .rect(cornerRadius: cornerRadius))
        } else {
            content
                .background(fallbackBackground)
                .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
                .shadow(color: .black.opacity(0.05), radius: 8, x: 0, y: 4)
        }
    }

    private var fallbackBackground: AnyShapeStyle {
        if reduceTransparency {
            return AnyShapeStyle(Color(uiColor: .secondarySystemBackground))
        }
        return AnyShapeStyle(.regularMaterial)
    }
}

// MARK: - Content Surface Card Modifier
///
/// Unified content card modifier.
///
/// Content surfaces should stay stable and readable. Liquid Glass is reserved
/// for navigation, controls, and floating actions.
struct GlassCardModifier: ViewModifier {
    @Environment(\.colorScheme) private var colorScheme

    var cornerRadius: CGFloat = 16
    var material: Material = .regularMaterial

    func body(content: Content) -> some View {
        content
            .background(fill)
            .overlay(
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                    .stroke(WakeveTheme.ColorToken.cardBorder(for: colorScheme), lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
            .shadow(color: WakeveTheme.Shadow.subtle.color, radius: WakeveTheme.Shadow.subtle.radius, x: WakeveTheme.Shadow.subtle.x, y: WakeveTheme.Shadow.subtle.y)
    }

    private var fill: Color {
        WakeveTheme.ColorToken.cardFill(for: colorScheme)
    }
}

// MARK: - View Extensions

extension View {
    /// Apply Liquid Glass effect with standard styling
    /// - Parameter cornerRadius: Corner radius (default: 20)
    func liquidGlass(cornerRadius: CGFloat = 20) -> some View {
        modifier(LiquidGlassModifier(cornerRadius: cornerRadius))
    }

    /// Apply the standard Wakeve content card style.
    /// - Parameters:
    ///   - cornerRadius: Corner radius (default: 16)
    ///   - material: Kept for call-site compatibility; content cards are non-glass.
    func glassCard(
        cornerRadius: CGFloat = 16,
        material: Material = .regularMaterial
    ) -> some View {
        modifier(GlassCardModifier(cornerRadius: cornerRadius, material: material))
    }
}

// MARK: - Previews

#Preview("Liquid Glass Modifiers") {
    ScrollView {
        VStack(spacing: 24) {
            Section("liquidGlass()") {
                Text("Standard Liquid Glass")
                    .frame(maxWidth: .infinity)
                    .padding(20)
                    .liquidGlass()
            }

            Section("glassCard()") {
                Text("Glass Card (default)")
                    .frame(maxWidth: .infinity)
                    .padding(20)
                    .glassCard()

                Text("Glass Card (radius: 20)")
                    .frame(maxWidth: .infinity)
                    .padding(20)
                    .glassCard(cornerRadius: 20)
            }
        }
        .padding()
    }
}
