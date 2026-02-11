import SwiftUI

// MARK: - Liquid Glass Animations
///
/// Reusable animation constants and helpers for Liquid Glass components.
///
/// All animations follow Apple's Human Interface Guidelines with
/// spring physics for natural, fluid motion.

struct LiquidGlassAnimations {
    /// Standard spring animation for UI interactions
    /// - response: 0.3s for snappy, responsive feel
    /// - dampingFraction: 0.7 for slight bounce without overshoot
    static let spring = Animation.spring(response: 0.3, dampingFraction: 0.7)
    
    /// Slower spring for larger, more prominent animations
    static let springSlow = Animation.spring(response: 0.4, dampingFraction: 0.8)
    
    /// Fast ease-out animation for fade transitions
    static let fadeIn = Animation.easeOut(duration: 0.25)
    
    /// Fast ease-in animation for fade out transitions
    static let fadeOut = Animation.easeIn(duration: 0.2)
    
    /// Scale animation for press feedback
    static let scale = Animation.spring(response: 0.25, dampingFraction: 0.6)
    
    /// Slide animation for modal presentations
    static let slideUp = Animation.interpolatingSpring(
        mass: 1.0,
        stiffness: 250,
        damping: 25,
        initialVelocity: 0
    )
    
    /// Expansion animation for tooltips and popovers
    static let expand = Animation.spring(response: 0.35, dampingFraction: 0.7)
    
    /// Collapse animation for dismissing elements
    static let collapse = Animation.easeOut(duration: 0.2)
    
    // MARK: - Convenience Methods
    
    /// Get animation with custom delay
    static func withDelay(_ delay: Double, animation: Animation = spring) -> Animation {
        animation.delay(delay)
    }
    
    /// Get animation with repeat configuration
    static func repeating(_ animation: Animation = spring, autoreverses: Bool = true) -> Animation {
        animation.repeatForever(autoreverses: autoreverses)
    }
}

// MARK: - Animation Modifiers

extension View {
    /// Apply standard Liquid Glass spring animation
    func liquidGlassAnimation() -> some View {
        self.animation(LiquidGlassAnimations.spring, value: UUID())
    }
    
    /// Apply animation with custom value trigger
    func liquidGlassAnimation<Value: Equatable>(_ value: Value) -> some View {
        self.animation(LiquidGlassAnimations.spring, value: value)
    }
    
    /// Apply spring animation with delay based on index
    func staggerAnimation(for index: Int, baseDelay: Double = 0.1) -> some View {
        self.animation(
            LiquidGlassAnimations.spring.delay(Double(index) * baseDelay),
            value: UUID()
        )
    }
    
    /// Apply smooth scale effect on press
    func scaleOnPress(scale: CGFloat = 0.95) -> some View {
        self.scaleEffect(scale)
            .animation(LiquidGlassAnimations.scale, value: scale)
    }
}

// MARK: - Glass Card Modifier
///
/// Applies Liquid Glass styling to any view with customizable
/// corner radius, material, and shadow properties.

struct GlassCardModifier: ViewModifier {
    var cornerRadius: CGFloat = 16
    var material: Material = .regularMaterial
    var showShadow: Bool = true
    var shadowOpacity: Double = 0.05
    var shadowRadius: CGFloat = 8
    
    @ViewBuilder
    func body(content: Content) -> some View {
        if showShadow {
            content
                .background(material)
                .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
                .shadow(
                    color: Color.black.opacity(shadowOpacity),
                    radius: shadowRadius,
                    x: 0,
                    y: shadowRadius / 2
                )
        } else {
            content
                .background(material)
                .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
        }
    }
}

extension View {
    /// Apply Liquid Glass card styling
    /// - Parameters:
    ///   - cornerRadius: Corner radius (default: 16)
    ///   - material: Material to use (default: .regularMaterial)
    ///   - showShadow: Whether to show shadow (default: true)
    ///   - shadowOpacity: Shadow opacity (default: 0.05)
    ///   - shadowRadius: Shadow radius (default: 8)
    func glassCard(
        cornerRadius: CGFloat = 16,
        material: Material = .regularMaterial,
        showShadow: Bool = true,
        shadowOpacity: Double = 0.05,
        shadowRadius: CGFloat = 8
    ) -> some View {
        modifier(
            GlassCardModifier(
                cornerRadius: cornerRadius,
                material: material,
                showShadow: showShadow,
                shadowOpacity: shadowOpacity,
                shadowRadius: shadowRadius
            )
        )
    }
}

// MARK: - Pulse Effect Modifier
///
/// Adds a subtle pulsing animation to views, useful for
/// attention-grabbing elements like AI badges.

struct PulseEffect: ViewModifier {
    @State private var isPulsing = false
    var scale: CGFloat = 1.05
    var duration: Double = 1.0
    
    func body(content: Content) -> some View {
        content
            .scaleEffect(isPulsing ? scale : 1.0)
            .opacity(isPulsing ? 0.8 : 1.0)
            .animation(
                .easeInOut(duration: duration).repeatForever(autoreverses: true),
                value: isPulsing
            )
            .onAppear {
                isPulsing = true
            }
    }
}

extension View {
    /// Add pulsing animation to view
    /// - Parameters:
    ///   - scale: Maximum scale during pulse (default: 1.05)
    ///   - duration: Duration of one pulse cycle (default: 1.0)
    func pulseEffect(scale: CGFloat = 1.05, duration: Double = 1.0) -> some View {
        modifier(PulseEffect(scale: scale, duration: duration))
    }
}

// MARK: - Shimmer Effect Modifier
///
/// Adds a shimmer animation to views, useful for loading states
/// or highlighting AI-generated content.

struct ShimmerEffect: ViewModifier {
    @State private var phase: CGFloat = 0
    
    func body(content: Content) -> some View {
        content
            .overlay(
                GeometryReader { geometry in
                    LinearGradient(
                        colors: [
                            Color.white.opacity(0),
                            Color.white.opacity(0.5),
                            Color.white.opacity(0)
                        ],
                        startPoint: .leading,
                        endPoint: .trailing
                    )
                    .frame(width: geometry.size.width * 2)
                    .offset(x: -geometry.size.width + phase * geometry.size.width * 3)
                }
            )
            .mask(content)
            .onAppear {
                withAnimation(.linear(duration: 1.5).repeatForever(autoreverses: false)) {
                    phase = 1
                }
            }
    }
}

extension View {
    /// Add shimmer animation to view
    func shimmerEffect() -> some View {
        modifier(ShimmerEffect())
    }
}

// MARK: - Hover Effect Modifier (macOS only)
///
/// Adds hover feedback on supported platforms.

#if os(macOS)
struct HoverEffect: ViewModifier {
    @State private var isHovered = false
    
    func body(content: Content) -> some View {
        content
            .background(
                RoundedRectangle(cornerRadius: 8, style: .continuous)
                    .fill(isHovered ? Color.white.opacity(0.1) : Color.clear)
            )
            .onHover { hovering in
                isHovered = hovering
            }
    }
}

extension View {
    /// Add hover effect (macOS only)
    func hoverEffect() -> some View {
        modifier(HoverEffect())
    }
}
#endif

// MARK: - Animated Badge Modifier
///
/// Creates an animated badge with entrance and pulse effects.

struct AnimatedBadgeModifier: ViewModifier {
    let isVisible: Bool
    let showPulse: Bool
    
    @State private var hasAppeared = false
    
    @ViewBuilder
    func body(content: Content) -> some View {
        if showPulse && hasAppeared {
            content
                .opacity(hasAppeared ? 1 : 0)
                .scaleEffect(hasAppeared ? 1 : 0.8)
                .animation(
                    LiquidGlassAnimations.spring,
                    value: hasAppeared
                )
                .pulseEffect(scale: 1.02, duration: 2.0)
                .onAppear {
                    hasAppeared = isVisible
                }
        } else {
            content
                .opacity(hasAppeared ? 1 : 0)
                .scaleEffect(hasAppeared ? 1 : 0.8)
                .animation(
                    LiquidGlassAnimations.spring,
                    value: hasAppeared
                )
                .onAppear {
                    hasAppeared = isVisible
                }
        }
    }
}

extension View {
    /// Create animated badge with entrance and optional pulse
    func animatedBadge(isVisible: Bool, showPulse: Bool = false) -> some View {
        modifier(AnimatedBadgeModifier(isVisible: isVisible, showPulse: showPulse))
    }
}

// MARK: - Previews

#Preview("Liquid Glass Animations") {
    VStack(spacing: 30) {
        Section("Spring Animation") {
            Circle()
                .fill(.blue)
                .frame(width: 60, height: 60)
                .liquidGlassAnimation()
        }
        
        Section("Staggered Animation") {
            ForEach(0..<3, id: \.self) { index in
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .fill(.ultraThinMaterial)
                    .frame(height: 60)
                    .staggerAnimation(for: index)
            }
        }
        
        Section("Pulse Effect") {
            Image(systemName: "sparkles")
                .font(.system(size: 32))
                .foregroundColor(.purple)
                .pulseEffect(scale: 1.1, duration: 1.5)
        }
        
        Section("Shimmer Effect") {
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .fill(.blue)
                .frame(height: 60)
                .shimmerEffect()
        }
    }
    .padding(30)
}

#Preview("Glass Card Modifier") {
    VStack(spacing: 20) {
        Text("Regular Material")
            .frame(maxWidth: .infinity)
            .padding(20)
            .glassCard(cornerRadius: 16, material: .regularMaterial)
        
        Text("Thin Material")
            .frame(maxWidth: .infinity)
            .padding(20)
            .glassCard(cornerRadius: 16, material: .thinMaterial, showShadow: false)
        
        Text("Ultra Thin Material")
            .frame(maxWidth: .infinity)
            .padding(20)
            .glassCard(cornerRadius: 16, material: .ultraThinMaterial, showShadow: false)
        
        Text("Thick Material")
            .frame(maxWidth: .infinity)
            .padding(20)
            .glassCard(cornerRadius: 20, material: .thickMaterial, shadowRadius: 12)
    }
    .padding()
}

#Preview("Animated Badge") {
    HStack(spacing: 20) {
        Text("AI")
            .font(.headline)
            .foregroundColor(.white)
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(Color.purple)
            .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
            .animatedBadge(isVisible: true, showPulse: true)
        
        Text("New")
            .font(.headline)
            .foregroundColor(.white)
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(Color.green)
            .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
            .animatedBadge(isVisible: true, showPulse: true)
    }
    .padding()
}
