import SwiftUI

/// Modifier for Liquid Glass effect
///
/// Uses the native iOS 26+ `.glassEffect()` API with a
/// `.regularMaterial` fallback for earlier versions.
struct LiquidGlassModifier: ViewModifier {
    var cornerRadius: CGFloat = 20

    @ViewBuilder
    func body(content: Content) -> some View {
        if #available(iOS 26.0, *) {
            content
                .glassEffect(.regular, in: .rect(cornerRadius: cornerRadius))
        } else {
            content
                .background(.regularMaterial)
                .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
                .shadow(color: .black.opacity(0.05), radius: 8, x: 0, y: 4)
        }
    }
}

extension View {
    func liquidGlass(cornerRadius: CGFloat = 20) -> some View {
        modifier(LiquidGlassModifier(cornerRadius: cornerRadius))
    }
}
