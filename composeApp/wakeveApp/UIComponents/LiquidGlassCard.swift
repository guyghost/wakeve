import SwiftUI

/// LiquidGlassCard - Standardized card component with Liquid Glass effect
/// 
/// Usage:
/// ```swift
/// LiquidGlassCard(cornerRadius: 16, padding: 16) {
///     VStack(alignment: .leading, spacing: 12) {
///         Text("Title")
///             .font(.headline)
///     }
/// }
/// ```
struct LiquidGlassCard<Content: View>: View {
    let cornerRadius: CGFloat
    let padding: CGFloat
    let opacity: Double
    let intensity: Double
    let content: Content
    
    init(
        cornerRadius: CGFloat = 16,
        padding: CGFloat = 16,
        opacity: Double = 0.8,
        intensity: Double = 1.0,
        @ViewBuilder content: () -> Content
    ) {
        self.cornerRadius = cornerRadius
        self.padding = padding
        self.opacity = opacity
        self.intensity = intensity
        self.content = content()
    }
    
    var body: some View {
        content
            .padding(padding)
            .liquidGlass(cornerRadius: cornerRadius, opacity: opacity, intensity: intensity)
    }
}

// MARK: - Preview
#Preview("LiquidGlassCard - Default") {
    LiquidGlassCard(cornerRadius: 16) {
        VStack(alignment: .leading, spacing: 12) {
            Text("Card Title")
                .font(.headline)
            Text("This is a Liquid Glass card with standard styling")
                .font(.subheadline)
                .foregroundColor(.secondary)
        }
    }
    .padding()
}

#Preview("LiquidGlassCard - Large Radius") {
    LiquidGlassCard(cornerRadius: 24, padding: 20) {
        VStack {
            Text("Rounded Card")
                .font(.title2.weight(.semibold))
        }
    }
    .padding()
}

#Preview("LiquidGlassCard - Compact") {
    LiquidGlassCard(cornerRadius: 12, padding: 12) {
        HStack {
            Image(systemName: "star.fill")
                .foregroundColor(.wakevAccent)
            Text("Compact card")
                .font(.subheadline)
        }
    }
    .padding()
}
