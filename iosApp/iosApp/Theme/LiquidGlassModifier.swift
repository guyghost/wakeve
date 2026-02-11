import SwiftUI

/// Modifier for Liquid Glass effect
struct LiquidGlassModifier: ViewModifier {
    var cornerRadius: CGFloat = 20
    var opacity: Double = 0.8
    var intensity: Double = 1.0
    
    func body(content: Content) -> some View {
        content
            .background(
                ZStack {
                    RoundedRectangle(cornerRadius: cornerRadius)
                        .fill(
                            LinearGradient(
                                gradient: Gradient(colors: [
                                    Color.white.opacity(opacity * 0.1),
                                    Color.white.opacity(opacity * 0.05)
                                ]),
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                    RoundedRectangle(cornerRadius: cornerRadius)
                        .fill(Color.white.opacity(0.02))
                }
            )
            .overlay(
                RoundedRectangle(cornerRadius: cornerRadius)
                    .stroke(
                        LinearGradient(
                            gradient: Gradient(colors: [
                                Color.white.opacity(0.3 * intensity),
                                Color.white.opacity(0.1 * intensity)
                            ]),
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        ),
                        lineWidth: 1.5
                    )
            )
            .background(
                RoundedRectangle(cornerRadius: cornerRadius)
                    .fill(Color.blue.opacity(0.1))
                    .blur(radius: 10)
            )
    }
}

extension View {
    func liquidGlass(cornerRadius: CGFloat = 20, opacity: Double = 0.8, intensity: Double = 1.0) -> some View {
        modifier(LiquidGlassModifier(cornerRadius: cornerRadius, opacity: opacity, intensity: intensity))
    }
}