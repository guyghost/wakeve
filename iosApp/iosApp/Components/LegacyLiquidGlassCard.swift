import SwiftUI

/// Liquid Glass Card Component
///
/// A reusable card component following Apple's Liquid Glass guidelines.
/// Uses native materials for depth and translucency with continuous corners.
///
/// Features:
/// - Four distinct glass styles (regular, thin, ultraThin, thick)
/// - Automatic corner radius based on style
/// - Customizable padding and shadows
/// - Continuous corners for smooth appearance
/// - Optimized shadow properties per style
///
/// Example:
/// ```swift
/// LiquidGlassCard {
///     Text("Content")
///         .font(.headline)
/// }
/// ```
struct LiquidGlassCard<Content: View>: View {
    private let style: GlassStyle
    private let cornerRadius: CGFloat
    private let padding: CGFloat
    private let showShadow: Bool
    private let content: Content

    /// Glass style options for different visual contexts
    enum GlassStyle {
        /// Standard glass for regular cards (most common use)
        case regular

        /// Subtle glass for secondary elements
        case thin

        /// Very subtle glass for backgrounds
        case ultraThin

        /// Prominent glass for elevated cards and modals
        case thick
    }

    /// Initialize a LiquidGlassCard with full control
    ///
    /// - Parameters:
    ///   - style: The glass material style (default: .regular)
    ///   - cornerRadius: Custom corner radius (default: computed from style)
    ///   - padding: Internal padding (default: 16)
    ///   - shadow: Whether to apply shadow (default: true for regular/thick)
    ///   - content: Card content closure
    init(
        style: GlassStyle = .regular,
        cornerRadius: CGFloat? = nil,
        padding: CGFloat = 16,
        shadow: Bool? = nil,
        @ViewBuilder content: () -> Content
    ) {
        self.style = style
        self.cornerRadius = cornerRadius ?? Self.defaultRadius(for: style)
        self.padding = padding
        self.showShadow = shadow ?? Self.shouldShowShadow(for: style)
        self.content = content()
    }

    // MARK: - Computed Properties

    /// Default corner radius for each style
    private static func defaultRadius(for style: GlassStyle) -> CGFloat {
        switch style {
        case .regular, .thin, .ultraThin:
            return 16
        case .thick:
            return 20
        }
    }

    /// Whether shadow is enabled by default for a style
    private static func shouldShowShadow(for style: GlassStyle) -> Bool {
        switch style {
        case .regular, .thick:
            return true
        case .thin, .ultraThin:
            return false
        }
    }

    /// Material to apply based on style
    private var material: Material {
        switch style {
        case .regular:
            return .regularMaterial
        case .thin:
            return .thinMaterial
        case .ultraThin:
            return .ultraThinMaterial
        case .thick:
            return .thickMaterial
        }
    }

    /// Shadow properties based on style
    private var shadowProperties: ShadowProperties {
        switch style {
        case .regular:
            return ShadowProperties(radius: 8, opacity: 0.05, yOffset: 4)
        case .thin:
            return ShadowProperties(radius: 4, opacity: 0.02, yOffset: 2)
        case .ultraThin:
            return ShadowProperties(radius: 0, opacity: 0, yOffset: 0)
        case .thick:
            return ShadowProperties(radius: 12, opacity: 0.08, yOffset: 6)
        }
    }

    var body: some View {
        content
            .padding(padding)
            .background(material)
            .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
            .if(showShadow && shadowProperties.radius > 0) {
                $0.shadow(
                    color: Color.black.opacity(shadowProperties.opacity),
                    radius: shadowProperties.radius,
                    x: 0,
                    y: shadowProperties.yOffset
                )
            }
    }

    // MARK: - Helper Types

    private struct ShadowProperties {
        let radius: CGFloat
        let opacity: Double
        let yOffset: CGFloat
    }
}

// MARK: - Convenience Initializers

extension LiquidGlassCard {
    /// Regular glass card with default parameters
    ///
    /// Most common use case. Includes subtle shadow.
    ///
    /// - Parameters:
    ///   - cornerRadius: Corner radius (default: 16)
    ///   - padding: Internal padding (default: 16)
    ///   - content: Card content
    init(
        cornerRadius: CGFloat = 16,
        padding: CGFloat = 16,
        @ViewBuilder content: () -> Content
    ) {
        self.init(
            style: .regular,
            cornerRadius: cornerRadius,
            padding: padding,
            shadow: true,
            content: content
        )
    }

    /// Thin glass card for subtle backgrounds
    ///
    /// Use for secondary cards and subtle visual separation.
    /// No shadow by default.
    ///
    /// - Parameters:
    ///   - cornerRadius: Corner radius (default: 16)
    ///   - padding: Internal padding (default: 16)
    ///   - content: Card content
    static func thin(
        cornerRadius: CGFloat = 16,
        padding: CGFloat = 16,
        @ViewBuilder content: () -> Content
    ) -> some View {
        LiquidGlassCard(style: .thin, cornerRadius: cornerRadius, padding: padding, shadow: false, content: content)
    }

    /// Ultra thin glass card for very subtle backgrounds
    ///
    /// Use for subtle UI separations and backgrounds.
    /// No shadow.
    ///
    /// - Parameters:
    ///   - cornerRadius: Corner radius (default: 12)
    ///   - padding: Internal padding (default: 12)
    ///   - content: Card content
    static func ultraThin(
        cornerRadius: CGFloat = 12,
        padding: CGFloat = 12,
        @ViewBuilder content: () -> Content
    ) -> some View {
        LiquidGlassCard(style: .ultraThin, cornerRadius: cornerRadius, padding: padding, shadow: false, content: content)
    }

    /// Thick glass card for prominent cards
    ///
    /// Use for elevated cards, modal backgrounds, and primary containers.
    /// Includes enhanced shadow.
    ///
    /// - Parameters:
    ///   - cornerRadius: Corner radius (default: 20)
    ///   - padding: Internal padding (default: 20)
    ///   - content: Card content
    static func thick(
        cornerRadius: CGFloat = 20,
        padding: CGFloat = 20,
        @ViewBuilder content: () -> Content
    ) -> some View {
        LiquidGlassCard(style: .thick, cornerRadius: cornerRadius, padding: padding, shadow: true, content: content)
    }
}

// MARK: - Helper Extension

extension View {
    /// Conditionally apply a transform to the view
    ///
    /// Useful for conditionally applying modifiers without creating separate branches.
    ///
    /// - Parameters:
    ///   - condition: Boolean condition
    ///   - transform: Transform to apply when condition is true
    @ViewBuilder
    func `if`<Transform: View>(
        _ condition: Bool,
        transform: (Self) -> Transform
    ) -> some View {
        if condition {
            transform(self)
        } else {
            self
        }
    }
}

// MARK: - Previews

struct LiquidGlassCard_Previews: PreviewProvider {
    static var previews: some View {
        ScrollView {
            VStack(spacing: 24) {
                // Section: Regular Cards
                VStack(spacing: 4) {
                    Text("Regular Cards")
                        .font(.headline)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal)

                    LiquidGlassCard {
                        VStack(spacing: 8) {
                            Text("Regular Card")
                                .font(.headline)
                            Text("Standard liquid glass with subtle shadow")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                    .padding(.horizontal)
                }

                // Section: Thin Cards
                VStack(spacing: 4) {
                    Text("Thin Cards")
                        .font(.headline)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal)

                    LiquidGlassCard.thin {
                        HStack {
                            Text("Thin Card")
                                .font(.headline)
                            Spacer()
                            Image(systemName: "arrow.right")
                                .foregroundColor(.blue)
                        }
                    }
                    .padding(.horizontal)

                    LiquidGlassCard.thin {
                        VStack(spacing: 6) {
                            Text("Subtle Background")
                                .font(.caption)
                                .foregroundColor(.secondary)
                            Text("No shadow, minimal visual weight")
                                .font(.caption2)
                                .foregroundColor(.secondary)
                        }
                    }
                    .padding(.horizontal)
                }

                // Section: Ultra Thin Cards
                VStack(spacing: 4) {
                    Text("Ultra Thin Cards")
                        .font(.headline)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal)

                    LiquidGlassCard.ultraThin {
                        HStack(spacing: 12) {
                            Image(systemName: "info.circle.fill")
                                .foregroundColor(.blue)
                            Text("Very subtle glass effect")
                                .font(.caption)
                        }
                    }
                    .padding(.horizontal)
                }

                // Section: Thick Cards
                VStack(spacing: 4) {
                    Text("Thick Cards")
                        .font(.headline)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal)

                    LiquidGlassCard.thick {
                        VStack(spacing: 12) {
                            Image(systemName: "star.fill")
                                .font(.system(size: 32))
                                .foregroundColor(.blue)
                            Text("Thick Card")
                                .font(.headline)
                            Text("Prominent glass effect with enhanced shadow")
                                .font(.caption)
                                .foregroundColor(.secondary)
                                .multilineTextAlignment(.center)
                        }
                    }
                    .padding(.horizontal)
                }

                // Section: Custom Cards
                VStack(spacing: 4) {
                    Text("Custom Cards")
                        .font(.headline)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal)

                    LiquidGlassCard(cornerRadius: 24, padding: 20) {
                        VStack(spacing: 12) {
                            HStack {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text("Custom Configuration")
                                        .font(.headline)
                                    Text("cornerRadius: 24, padding: 20")
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                }
                                Spacer()
                                Image(systemName: "gear")
                                    .font(.title3)
                                    .foregroundColor(.blue)
                            }
                        }
                    }
                    .padding(.horizontal)

                    LiquidGlassCard(cornerRadius: 12, padding: 12, shadow: false) {
                        HStack(spacing: 12) {
                            Image(systemName: "checkmark.circle.fill")
                                .foregroundColor(.green)
                            Text("No shadow variant")
                                .font(.caption)
                        }
                    }
                    .padding(.horizontal)
                }

                Spacer(minLength: 20)
            }
            .padding(.vertical, 24)
        }
        .background(Color(red: 0.97, green: 0.97, blue: 0.98))
    }
}
