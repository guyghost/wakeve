import SwiftUI

/// Modern Get Started / Welcome view inspired by Apple Invites
/// First screen users see when opening the app
struct ModernGetStartedView: View {
    let onGetStarted: () -> Void

    #if DEBUG
    @EnvironmentObject var authStateManager: AuthStateManager
    @State private var isSkipping = false
    #endif

    var body: some View {
        ZStack {
            // Gradient background
            LinearGradient(
                colors: [Color.blue, Color.purple],
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
                // Dev Skip Button
                HStack {
                    Spacer()

                    if isSkipping {
                        ProgressView()
                            .progressViewStyle(CircularProgressViewStyle(tint: .white))
                            .padding(.trailing, 20)
                            .padding(.top, 60)
                    } else {
                        Button(action: skipForDevelopment) {
                            Text("Skip (Dev)")
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundColor(.white)
                                .padding(.horizontal, 16)
                                .padding(.vertical, 8)
                                .background(Color.white.opacity(0.2))
                                .cornerRadius(20)
                        }
                        .padding(.trailing, 20)
                        .padding(.top, 60)
                    }
                }
                #endif

                Spacer()

                // App Icon
                ZStack {
                    Circle()
                        .fill(Color.white.opacity(0.2))
                        .frame(width: 140, height: 140)
                        .background(.ultraThinMaterial, in: Circle())

                    Image(systemName: "calendar.badge.plus")
                        .font(.system(size: 64, weight: .light))
                        .foregroundColor(.white)
                }
                .padding(.bottom, 40)

                // App Name
                Text("Wakeve")
                    .font(.system(size: 52, weight: .bold, design: .rounded))
                    .foregroundColor(.white)
                    .padding(.bottom, 16)

                // Tagline
                Text("Plan Events Together")
                    .font(.system(size: 22, weight: .medium))
                    .foregroundColor(.white.opacity(0.9))
                    .padding(.bottom, 60)

                // Features
                VStack(spacing: 24) {
                    FeatureRow(
                        icon: "person.2.fill",
                        title: "Collaborate",
                        description: "Invite participants to vote on times"
                    )

                    FeatureRow(
                        icon: "chart.bar.fill",
                        title: "Vote",
                        description: "Find the best time that works for everyone"
                    )

                    FeatureRow(
                        icon: "checkmark.circle.fill",
                        title: "Confirm",
                        description: "Lock in the final date and notify all"
                    )
                }
                .padding(.horizontal, 40)
                .padding(.bottom, 60)

                Spacer()

                // Get Started Button
                Button(action: onGetStarted) {
                    Text("Get Started")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundColor(.blue)
                        .frame(maxWidth: .infinity)
                        .frame(height: 56)
                        .background(Color.white)
                        .cornerRadius(16)
                }
                .padding(.horizontal, 40)
                .padding(.bottom, 50)
            }
        }
    }

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

// MARK: - Feature Row

struct FeatureRow: View {
    let icon: String
    let title: String
    let description: String

    var body: some View {
        HStack(spacing: 16) {
            // Icon
            ZStack {
                Circle()
                    .fill(Color.white.opacity(0.2))
                    .frame(width: 48, height: 48)

                Image(systemName: icon)
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundColor(.white)
            }

            // Text
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
    }
}

struct ModernGetStartedView_Previews: PreviewProvider {
    static var previews: some View {
        ModernGetStartedView(onGetStarted: {})
    }
}
