import SwiftUI

/**
 * ModernGetStartedView - Welcome screen for iOS
 *
 * First screen users see when opening the app
 * Uses Liquid Glass design system with spring animations
 *
 * ## Components Used
 * - Custom Liquid Glass button styling
 * - Custom Liquid Glass card containers
 * - Custom Liquid Glass dividers
 * - Design system colors
 *
 * ## Architecture
 * - Functional Core: State management for entrance animations
 * - Imperative Shell: UI rendering with Liquid Glass styling
 */
struct ModernGetStartedView: View {
    let onGetStarted: () -> Void

    // MARK: - Animation State

    @State private var iconScale: CGFloat = 0.8
    @State private var iconOpacity: Double = 0
    @State private var titleOffset: CGFloat = 50
    @State private var titleOpacity: Double = 0
    @State private var featuresOffset: CGFloat = 30
    @State private var featuresOpacity: Double = 0
    @State private var buttonOffset: CGFloat = 20
    @State private var buttonOpacity: Double = 0

    #if DEBUG
    @EnvironmentObject var authStateManager: AuthStateManager
    @State private var isSkipping = false
    #endif

    // MARK: - Body

    var body: some View {
        ZStack {
            // Gradient background using design system colors
            LinearGradient(
                gradient: Gradient(colors: [
                    Color(hex: "2563EB"),
                    Color(hex: "7C3AED")
                ]),
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()

            // Pattern overlay
            GeometryReader { geometry in
                Image(systemName: "calendar")
                    .font(.system(size: 300))
                    .foregroundColor(.white.opacity(0.05))
                    .offset(x: geometry.size.width * 0.3, y: geometry.size.height * 0.2)
                    .rotationEffect(.degrees(-15))
            }

            VStack(spacing: 0) {
                #if DEBUG
                // Dev Skip Button - Glass style
                HStack {
                    Spacer()
                    if isSkipping {
                        ProgressView()
                            .progressViewStyle(CircularProgressViewStyle(tint: .white))
                            .padding(.trailing, 20)
                            .padding(.top, 60)
                    } else {
                        skipDevButton
                            .padding(.trailing, 20)
                            .padding(.top, 60)
                    }
                }
                #endif

                Spacer()

                // App Icon with Liquid Glass effect
                appIconView
                    .padding(.bottom, 40)

                // App Name with animation
                appTitleView
                    .padding(.bottom, 16)

                // Tagline with animation
                taglineView
                    .padding(.bottom, 60)

                // Features Section with staggered animation
                featuresSectionView
                    .padding(.horizontal, 40)
                    .padding(.bottom, 60)

                Spacer()

                // Get Started Button with Liquid Glass styling
                getStartedButton
                    .padding(.horizontal, 40)
                    .padding(.bottom, 50)
            }
        }
        .onAppear {
            withAnimation(
                Animation.spring(response: 0.8, dampingFraction: 0.6)
            ) {
                iconScale = 1.0
                iconOpacity = 1.0
            }

            withAnimation(
                Animation.spring(response: 0.6, dampingFraction: 0.7).delay(0.2)
            ) {
                titleOffset = 0
                titleOpacity = 1.0
            }

            withAnimation(
                Animation.spring(response: 0.6, dampingFraction: 0.7).delay(0.4)
            ) {
                featuresOffset = 0
                featuresOpacity = 1.0
            }

            withAnimation(
                Animation.spring(response: 0.6, dampingFraction: 0.7).delay(0.6)
            ) {
                buttonOffset = 0
                buttonOpacity = 1.0
            }
        }
    }

    // MARK: - Subviews

    #if DEBUG
    private var skipDevButton: some View {
        Button(action: skipForDevelopment) {
            Text("Skip (Dev)")
                .font(.system(size: 15, weight: .semibold))
                .foregroundColor(.white)
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
                .background(
                    RoundedRectangle(cornerRadius: 20)
                        .fill(Color.white.opacity(0.15))
                        .overlay(
                            RoundedRectangle(cornerRadius: 20)
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
                        )
                )
        }
        .accessibilityLabel("Skip for development")
    }
    #endif

    private var appIconView: some View {
        ZStack {
            // Outer glow ring
            Circle()
                .fill(Color.white.opacity(0.1))
                .frame(width: 160, height: 160)
                .blur(radius: 20)

            // Glass circle container
            Circle()
                .fill(.ultraThinMaterial)
                .frame(width: 140, height: 140)
                .overlay(
                    Circle()
                        .stroke(
                            LinearGradient(
                                gradient: Gradient(colors: [
                                    Color.white.opacity(0.5),
                                    Color.white.opacity(0.2)
                                ]),
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            ),
                            lineWidth: 2
                        )
                )

            // App icon
            Image(systemName: "calendar.badge.plus")
                .font(.system(size: 64, weight: .light))
                .foregroundColor(.white)
        }
        .scaleEffect(iconScale)
        .opacity(iconOpacity)
    }

    private var appTitleView: some View {
        Text("Wakeve")
            .font(.system(size: 52, weight: .bold, design: .rounded))
            .foregroundColor(.white)
            .offset(y: titleOffset)
            .opacity(titleOpacity)
    }

    private var taglineView: some View {
        Text("Plan Events Together")
            .font(.system(size: 22, weight: .medium))
            .foregroundColor(.white.opacity(0.9))
            .offset(y: titleOffset)
            .opacity(titleOpacity)
    }

    private var featuresSectionView: some View {
        VStack(spacing: 24) {
            FeatureRow(
                icon: "person.2.fill",
                title: "Collaborate",
                description: "Invite participants to vote on times",
                iconColor: Color(hex: "2563EB")
            )

            customDivider

            FeatureRow(
                icon: "chart.bar.fill",
                title: "Vote",
                description: "Find the best time that works for everyone",
                iconColor: Color(hex: "7C3AED")
            )

            customDivider

            FeatureRow(
                icon: "checkmark.circle.fill",
                title: "Confirm",
                description: "Lock in the final date and notify all",
                iconColor: Color(hex: "059669")
            )
        }
        .offset(y: featuresOffset)
        .opacity(featuresOpacity)
    }

    private var customDivider: some View {
        Rectangle()
            .fill(
                LinearGradient(
                    gradient: Gradient(colors: [
                        Color.clear,
                        Color.white.opacity(0.2),
                        Color.white.opacity(0.3),
                        Color.white.opacity(0.2),
                        Color.clear
                    ]),
                    startPoint: .leading,
                    endPoint: .trailing
                )
            )
            .frame(height: 1)
            .padding(.horizontal, 12)
    }

    private var getStartedButton: some View {
        Button(action: onGetStarted) {
            HStack(spacing: 8) {
                Text("Get Started")
                    .font(.system(size: 18, weight: .semibold))
                Image(systemName: "arrow.right")
                    .font(.system(size: 16, weight: .semibold))
            }
            .foregroundColor(.white)
            .frame(maxWidth: .infinity)
            .frame(height: 56)
            .background(
                LinearGradient(
                    gradient: Gradient(colors: [
                        Color(hex: "2563EB"),
                        Color(hex: "7C3AED")
                    ]),
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
            )
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
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
            )
            .shadow(
                color: Color(hex: "2563EB").opacity(0.3),
                radius: 12,
                x: 0,
                y: 6
            )
        }
        .offset(y: buttonOffset)
        .opacity(buttonOpacity)
        .scaleEffectOnPress()
        .accessibilityLabel("Get Started")
        .accessibilityHint("Tap to begin using Wakeve")
    }

    // MARK: - Development Functions

    #if DEBUG
    /**
     * Skip authentication for development.
     */
    private func skipForDevelopment() {
        isSkipping = true

        Task {
            // Simulate loading
            try? await Task.sleep(nanoseconds: 500_000_000) // 0.5 seconds

            // Create mock auth state
            let devUserId = "dev-user-\(UUID().uuidString.prefix(8))"
            let mockToken = "dev-token-mock"

            await authStateManager.setAuthStateForDevelopment(
                userId: devUserId,
                accessToken: mockToken
            )

            isSkipping = false
        }
    }
    #endif
}

// MARK: - Feature Row Component

struct FeatureRow: View {
    let icon: String
    let title: String
    let description: String
    let iconColor: Color

    @State private var isPressed = false

    var body: some View {
        HStack(spacing: 16) {
            // Glass icon container
            ZStack {
                Circle()
                    .fill(iconColor.opacity(0.2))
                    .frame(width: 48, height: 48)

                Image(systemName: icon)
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundColor(.white)
            }
            .scaleEffect(isPressed ? 0.95 : 1.0)
            .animation(.spring(response: 0.3, dampingFraction: 0.6), value: isPressed)

            // Text content
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundColor(.white)

                Text(description)
                    .font(.system(size: 15))
                    .foregroundColor(.white.opacity(0.8))
            }

            Spacer()
        }
        .contentShape(Rectangle())
        .onTapGesture {
            withAnimation {
                isPressed = true
            }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                withAnimation {
                    isPressed = false
                }
            }
        }
    }
}

// MARK: - View Extensions

extension View {
    func scaleEffectOnPress() -> some View {
        modifier(ScaleEffectOnPress())
    }
}

struct ScaleEffectOnPress: ViewModifier {
    @State private var isPressed = false

    func body(content: Content) -> some View {
        content
            .scaleEffect(isPressed ? 0.98 : 1.0)
            .animation(.spring(response: 0.2, dampingFraction: 0.6), value: isPressed)
            .simultaneousGesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { _ in isPressed = true }
                    .onEnded { _ in isPressed = false }
            )
    }
}

// Colors are defined in Theme/WakevColors.swift

// MARK: - Preview

struct ModernGetStartedView_Previews: PreviewProvider {
    static var previews: some View {
        ModernGetStartedView(onGetStarted: {})
            .environmentObject(AuthStateManager())
    }
}
