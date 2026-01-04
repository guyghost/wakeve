import SwiftUI

// MARK: - Badge Rarity Enum (Local Definition for Animation)
///
/// Rarity level affecting display appearance and prestige.
enum BadgeRarity: String, Codable, CaseIterable {
    case COMMON = "COMMON"
    case RARE = "RARE"
    case EPIC = "EPIC"
    case LEGENDARY = "LEGENDARY"

    var color: Color {
        switch self {
        case .COMMON:
            return .gray
        case .RARE:
            return .blue
        case .EPIC:
            return .purple
        case .LEGENDARY:
            return .orange
        @unknown default:
            return .primary
        }
    }

    var glowColor: Color {
        switch self {
        case .COMMON:
            return .gray.opacity(0.3)
        case .RARE:
            return .blue.opacity(0.4)
        case .EPIC:
            return .purple.opacity(0.5)
        case .LEGENDARY:
            return .orange.opacity(0.6)
        @unknown default:
            return .primary.opacity(0.3)
        }
    }
}

// MARK: - Badge Category Enum (Local Definition for Animation)
enum BadgeCategory: String, Codable, CaseIterable {
    case CREATION = "CREATION"
    case VOTING = "VOTING"
    case PARTICIPATION = "PARTICIPATION"
    case ENGAGEMENT = "ENGAGEMENT"
    case SPECIAL = "SPECIAL"
}

// MARK: - Badge Model (Local Definition for Animation)
///
/// Represents a badge that can be earned by users for specific achievements.
struct Badge: Identifiable, Codable, Equatable {
    let id: String
    let name: String
    let description: String
    let icon: String
    let requirement: Int
    let pointsReward: Int
    let category: BadgeCategory
    let rarity: BadgeRarity
    let unlockedAt: String?

    var isUnlocked: Bool {
        unlockedAt != nil
    }
}

// MARK: - Badge Celebration Animation
///
/// Liquid Glass compliant celebration animation for badge unlocking.
/// Features confetti particles, pulsing effects, and smooth transitions.
///
/// All animations follow Apple's Human Interface Guidelines with
/// spring physics for natural, fluid motion.

struct BadgeCelebrationAnimation: View {
    let badge: Badge
    let onAnimationComplete: () -> Void

    @State private var isVisible = true
    @State private var scale: CGFloat = 0.8
    @State private var rotation: Double = 0.0
    @State private var opacity: Double = 0.0
    @State private var particles: [ConfettiParticle] = []

    var body: some View {
        ZStack {
            if isVisible {
                // Semi-transparent overlay
                Color.black.opacity(0.5)
                    .ignoresSafeArea()
                    .transition(.opacity)

                // Confetti particles layer
                ParticleView(particles: particles)

                // Badge card with liquid glass effect
                VStack(spacing: 12) {
                    // Badge icon
                    Text(badge.icon)
                        .font(.system(size: 80))
                        .scaleEffect(scale * 1.2)
                        .rotationEffect(.degrees(rotation))
                        .shadow(color: badge.rarity.glowColor, radius: 20)

                    // "Badge unlocked" text
                    Text("Badge débloqué !")
                        .font(.title3)
                        .fontWeight(.bold)
                        .foregroundColor(.primary)
                        .opacity(opacity)

                    // Badge name
                    Text(badge.name)
                        .font(.title2)
                        .fontWeight(.bold)
                        .foregroundColor(badge.rarity.color)
                        .opacity(opacity)

                    // Badge description
                    Text(badge.description)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .opacity(opacity)
                        .padding(.horizontal)

                    // Points reward
                    if badge.pointsReward > 0 {
                        Text("+\(badge.pointsReward) points")
                            .font(.headline)
                            .fontWeight(.semibold)
                            .foregroundColor(.white)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 8)
                            .background(badge.rarity.color)
                            .cornerRadius(20)
                            .opacity(opacity)
                            .padding(.top, 8)
                    }
                }
                .padding(24)
                .background(.ultraThinMaterial)
                .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
                .shadow(
                    color: Color.black.opacity(0.15),
                    radius: 20,
                    x: 0,
                    y: 10
                )
                .frame(width: 260, height: 320)
                .scaleEffect(scale)
                .opacity(opacity)
                .transition(.scale.combined(with: .opacity))
                .zIndex(1)
            }
        }
        .onAppear {
            startCelebration()
        }
        .onDisappear {
            onAnimationComplete()
        }
    }

    private func startCelebration() {
        // Initial entrance animation
        withAnimation(.spring(response: 0.6, dampingFraction: 0.7)) {
            scale = 1.0
            opacity = 1.0
        }

        // Rotation animation
        withAnimation(
            .linear(duration: 3.0)
                .repeatForever(autoreverses: false)
        ) {
            rotation = 360.0
        }

        // Generate confetti particles
        particles = (0..<50).map { _ in
            ConfettiParticle(
                x: CGFloat.random(in: 0...1),
                y: CGFloat.random(in: 0...0.4),
                size: CGFloat.random(in: 6...14),
                color: getRandomConfettiColor(),
                speed: CGFloat.random(in: 0.3...0.8)
            )
        }

        // Auto-dismiss after duration
        DispatchQueue.main.asyncAfter(deadline: .now() + 3.5) {
            withAnimation(.easeOut(duration: 0.3)) {
                isVisible = false
                opacity = 0.0
                scale = 0.8
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) {
                onAnimationComplete()
            }
        }
    }

    private func getRandomConfettiColor() -> Color {
        let colors: [Color] = [
            .red, .blue, .green, .yellow, .purple, .orange, .pink, .cyan
        ]
        return colors.randomElement() ?? .accentColor
    }
}

// MARK: - Confetti Particle Model

struct ConfettiParticle: Identifiable {
    let id = UUID()
    var x: CGFloat
    var y: CGFloat
    let size: CGFloat
    let color: Color
    let speed: CGFloat
    var rotation: Double = 0.0
}

// MARK: - Particle View

struct ParticleView: View {
    let particles: [ConfettiParticle]
    @State private var animationPhase: CGFloat = 0

    var body: some View {
        GeometryReader { geometry in
            ZStack {
                ForEach(particles) { particle in
                    ConfettiParticleView(
                        particle: particle,
                        containerSize: geometry.size,
                        animationPhase: animationPhase
                    )
                }
            }
        }
        .onAppear {
            withAnimation(.linear(duration: 2.0).repeatForever(autoreverses: false)) {
                animationPhase = 1.0
            }
        }
    }
}

struct ConfettiParticleView: View {
    let particle: ConfettiParticle
    let containerSize: CGSize
    let animationPhase: CGFloat

    var body: some View {
        Circle()
            .fill(particle.color)
            .frame(width: particle.size, height: particle.size)
            .offset(
                x: particle.x * containerSize.width + (animationPhase * particle.speed * 100 - 50),
                y: particle.y * containerSize.height - (animationPhase * animationPhase * containerSize.height * 0.4)
            )
            .opacity(max(0, 1 - animationPhase))
    }
}

// MARK: - Previews

#Preview("Badge Celebration - Epic") {
    let epicBadge = Badge(
        id: "badge-super-organizer",
        name: "Super Organisateur",
        description: "A créé 10 événements",
        icon: "🏆",
        requirement: 10,
        pointsReward: 100,
        category: .CREATION,
        rarity: .EPIC,
        unlockedAt: nil
    )

    BadgeCelebrationAnimation(badge: epicBadge) {
        print("Animation complete")
    }
}

#Preview("Badge Celebration - Legendary") {
    let legendaryBadge = Badge(
        id: "badge-legend",
        name: "Légende des événements",
        description: "10000 points accumulés",
        icon: "👑",
        requirement: 10000,
        pointsReward: 500,
        category: .ENGAGEMENT,
        rarity: .LEGENDARY,
        unlockedAt: nil
    )

    BadgeCelebrationAnimation(badge: legendaryBadge) {
        print("Animation complete")
    }
}

#Preview("Badge Celebration - Common") {
    let commonBadge = Badge(
        id: "badge-first-event",
        name: "Premier événement",
        description: "A créé son premier événement",
        icon: "🎉",
        requirement: 1,
        pointsReward: 50,
        category: .CREATION,
        rarity: .COMMON,
        unlockedAt: nil
    )

    BadgeCelebrationAnimation(badge: commonBadge) {
        print("Animation complete")
    }
}
