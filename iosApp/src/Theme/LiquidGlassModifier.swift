import SwiftUI

// MARK: - Liquid Glass Modifier
///
/// Modifier for Liquid Glass effect.
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

// MARK: - Glass Card Modifier
///
/// Unified glass card modifier with iOS 26+ support.
///
/// Replaces duplicate implementations from LiquidGlassAnimations.swift
/// and ViewExtensions.swift.
struct GlassCardModifier: ViewModifier {
    var cornerRadius: CGFloat = 16
    var material: Material = .regularMaterial

    @ViewBuilder
    func body(content: Content) -> some View {
        if #available(iOS 26.0, *) {
            content
                .glassEffect(.regular, in: .rect(cornerRadius: cornerRadius))
        } else {
            content
                .background(material)
                .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
                .shadow(color: .black.opacity(0.05), radius: 8, x: 0, y: 4)
        }
    }
}

// MARK: - Thin Glass Modifier

struct ThinGlassModifier: ViewModifier {
    var cornerRadius: CGFloat = 16

    @ViewBuilder
    func body(content: Content) -> some View {
        if #available(iOS 26.0, *) {
            content
                .glassEffect(.regular, in: .rect(cornerRadius: cornerRadius))
        } else {
            content
                .background(.thinMaterial)
                .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
        }
    }
}

// MARK: - Ultra Thin Glass Modifier

struct UltraThinGlassModifier: ViewModifier {
    var cornerRadius: CGFloat = 16

    @ViewBuilder
    func body(content: Content) -> some View {
        if #available(iOS 26.0, *) {
            content
                .glassEffect(.regular, in: .rect(cornerRadius: cornerRadius))
        } else {
            content
                .background(.ultraThinMaterial)
                .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
        }
    }
}

// MARK: - Thick Glass Modifier

struct ThickGlassModifier: ViewModifier {
    var cornerRadius: CGFloat = 24

    @ViewBuilder
    func body(content: Content) -> some View {
        if #available(iOS 26.0, *) {
            content
                .glassEffect(.regular, in: .rect(cornerRadius: cornerRadius))
        } else {
            content
                .background(.thickMaterial)
                .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
                .shadow(color: .black.opacity(0.08), radius: 12, x: 0, y: 6)
        }
    }
}

// MARK: - View Extensions

extension View {
    /// Apply Liquid Glass effect with standard styling
    /// - Parameter cornerRadius: Corner radius (default: 20)
    func liquidGlass(cornerRadius: CGFloat = 20) -> some View {
        modifier(LiquidGlassModifier(cornerRadius: cornerRadius))
    }

    /// Apply glass card style following Apple's Liquid Glass guidelines
    /// - Parameters:
    ///   - cornerRadius: Corner radius (default: 16)
    ///   - material: Material to use for fallback (default: .regularMaterial)
    func glassCard(
        cornerRadius: CGFloat = 16,
        material: Material = .regularMaterial
    ) -> some View {
        modifier(GlassCardModifier(cornerRadius: cornerRadius, material: material))
    }

    /// Apply thin glass style for subtle backgrounds
    /// - Parameter cornerRadius: Corner radius (default: 16)
    func thinGlass(cornerRadius: CGFloat = 16) -> some View {
        modifier(ThinGlassModifier(cornerRadius: cornerRadius))
    }

    /// Apply ultra thin glass style for very subtle backgrounds
    /// - Parameter cornerRadius: Corner radius (default: 16)
    func ultraThinGlass(cornerRadius: CGFloat = 16) -> some View {
        modifier(UltraThinGlassModifier(cornerRadius: cornerRadius))
    }

    /// Apply thick glass style for prominent cards
    /// - Parameter cornerRadius: Corner radius (default: 24)
    func thickGlass(cornerRadius: CGFloat = 24) -> some View {
        modifier(ThickGlassModifier(cornerRadius: cornerRadius))
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

            Section("Glass Variants") {
                Text("Thin Glass")
                    .frame(maxWidth: .infinity)
                    .padding(20)
                    .thinGlass()

                Text("Ultra Thin Glass")
                    .frame(maxWidth: .infinity)
                    .padding(20)
                    .ultraThinGlass()

                Text("Thick Glass")
                    .frame(maxWidth: .infinity)
                    .padding(20)
                    .thickGlass()
            }
        }
        .padding()
    }
}
