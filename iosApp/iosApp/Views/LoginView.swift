import SwiftUI
import AuthenticationServices

/**
 * Login view for iOS using SwiftUI.
 *
 * Provides a native iOS login experience with:
 * - Sign in with Apple button (native ASAuthorizationAppleIDButton)
 * - App branding and welcome message
 * - Loading indicator during authentication
 * - Error handling with retry option
 */
struct LoginView: View {
    @StateObject private var appleSignInHelper = AppleSignInHelper()
    @EnvironmentObject var authService: AuthenticationService
    @EnvironmentObject var authStateManager: AuthStateManager

    @State private var isLoading = false
    @State private var errorMessage: String?
    @State private var showError = false

    var body: some View {
        ZStack {
            // Clean background gradient
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
                Spacer()

                // App Branding
                AppBrandingView()
                    .padding(.bottom, 32)

                // Welcome Text
                VStack(spacing: 12) {
                    Text("Sign In")
                        .font(.system(size: 42, weight: .bold))
                        .foregroundColor(.white)

                    Text("Continue with your Apple ID to get started")
                        .font(.system(size: 17))
                        .foregroundColor(.white.opacity(0.9))
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 40)
                }
                .padding(.bottom, 60)

                Spacer()

                // Sign-in Button
                if isLoading {
                    VStack(spacing: 20) {
                        ProgressView()
                            .progressViewStyle(CircularProgressViewStyle(tint: .white))
                            .scaleEffect(1.5)

                        Text("Signing in...")
                            .font(.system(size: 17, weight: .medium))
                            .foregroundColor(.white.opacity(0.9))
                    }
                    .padding(.bottom, 80)
                } else {
                    VStack(spacing: 20) {
                        // Native Apple Sign-In Button
                        SignInWithAppleButton(.signIn) { request in
                            request.requestedScopes = [.fullName, .email]
                        } onCompletion: { result in
                            handleAppleSignInResult(result)
                        }
                        .signInWithAppleButtonStyle(.white)
                        .frame(height: 56)
                        .cornerRadius(16)
                        .shadow(color: Color.black.opacity(0.1), radius: 10, x: 0, y: 5)

                        #if DEBUG
                        // Development Mode - Skip Authentication
                        Button(action: skipAuthForDevelopment) {
                            Text("Skip (Development)")
                                .font(.system(size: 17, weight: .semibold))
                                .foregroundColor(.white)
                                .frame(maxWidth: .infinity)
                                .frame(height: 56)
                                .background(Color.white.opacity(0.2))
                                .cornerRadius(16)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 16)
                                        .stroke(Color.white.opacity(0.5), lineWidth: 2)
                                )
                        }
                        #endif

                        // Privacy & Terms
                        VStack(spacing: 6) {
                            Text("By signing in, you agree to our")
                                .font(.system(size: 13))
                                .foregroundColor(.white.opacity(0.7))

                            HStack(spacing: 6) {
                                Button("Privacy Policy") {
                                    // TODO: Open privacy policy
                                }
                                .font(.system(size: 13, weight: .medium))
                                .foregroundColor(.white)

                                Text("and")
                                    .font(.system(size: 13))
                                    .foregroundColor(.white.opacity(0.7))

                                Button("Terms of Service") {
                                    // TODO: Open terms
                                }
                                .font(.system(size: 13, weight: .medium))
                                .foregroundColor(.white)
                            }
                        }
                    }
                    .padding(.horizontal, 40)
                    .padding(.bottom, 60)
                }
            }
        }
        .alert("Sign-in Failed", isPresented: $showError) {
            Button("Try Again", role: .cancel) {
                showError = false
            }
        } message: {
            Text(errorMessage ?? "An unknown error occurred. Please try again.")
        }
    }

    // MARK: - Subviews

    /**
     * App branding view with logo/icon.
     */
    private func AppBrandingView() -> some View {
        ZStack {
            Circle()
                .fill(Color.white.opacity(0.2))
                .frame(width: 120, height: 120)
                .background(.ultraThinMaterial, in: Circle())

            Image(systemName: "calendar.badge.plus")
                .font(.system(size: 52, weight: .light))
                .foregroundColor(.white)
        }
    }

    // MARK: - Actions

    /**
     * Handle Apple Sign-In result.
     */
    private func handleAppleSignInResult(_ result: Result<ASAuthorization, Error>) {
        isLoading = true

        Task {
            do {
                switch result {
                case .success(let authorization):
                    guard let appleIDCredential = authorization.credential as? ASAuthorizationAppleIDCredential else {
                        throw AuthenticationError.invalidCredentials
                    }

                    // Extract authorization code
                    guard let authCodeData = appleIDCredential.authorizationCode,
                          let authCode = String(data: authCodeData, encoding: .utf8) else {
                        throw AuthenticationError.invalidCredentials
                    }

                    // Extract identity token (optional)
                    var idToken: String?
                    if let identityTokenData = appleIDCredential.identityToken {
                        idToken = String(data: identityTokenData, encoding: .utf8)
                    }

                    // Login with backend via AuthStateManager
                    await authStateManager.signIn(
                        provider: "apple",
                        authCode: authCode,
                        userInfo: idToken
                    )

                    // Success - AuthStateManager will update state
                    print("Login successful")

                case .failure(let error):
                    if let authError = error as? ASAuthorizationError {
                        switch authError.code {
                        case .canceled:
                            // User cancelled - don't show error
                            break
                        case .failed:
                            errorMessage = "Sign-in failed. Please try again."
                            showError = true
                        case .invalidResponse:
                            errorMessage = "Invalid response from Apple. Please try again."
                            showError = true
                        case .notHandled:
                            errorMessage = "Sign-in request not handled."
                            showError = true
                        case .unknown:
                            errorMessage = "An unknown error occurred."
                            showError = true
                        default:
                            errorMessage = "Sign-in failed: \(authError.localizedDescription)"
                            showError = true
                        }
                    } else {
                        errorMessage = "Sign-in failed: \(error.localizedDescription)"
                        showError = true
                    }
                }
            } catch {
                errorMessage = "Authentication failed: \(error.localizedDescription)"
                showError = true
            }

            isLoading = false
        }
    }

    #if DEBUG
    /**
     * Skip authentication for development.
     * Creates a mock authenticated session.
     */
    private func skipAuthForDevelopment() {
        isLoading = true

        Task {
            // Simulate network delay
            try? await Task.sleep(nanoseconds: 500_000_000) // 0.5 seconds

            // Create mock authentication state
            let devUserId = "dev-user-\(UUID().uuidString.prefix(8))"
            let mockToken = "dev-token-mock"

            // Manually set auth state for development
            await authStateManager.setAuthStateForDevelopment(
                userId: devUserId,
                accessToken: mockToken
            )

            isLoading = false
        }
    }
    #endif
}

// MARK: - Authentication Error

enum AuthenticationError: LocalizedError {
    case invalidCredentials
    case networkError
    case serverError(String)
    case noRefreshToken
    case refreshFailed
    case loginFailed
    case urlGenerationFailed
    case invalidResponse

    var errorDescription: String? {
        switch self {
        case .invalidCredentials:
            return "Invalid credentials received from Apple"
        case .networkError:
            return "Network error. Please check your connection and try again"
        case .serverError(let message):
            return "Server error: \(message)"
        case .noRefreshToken:
            return "No refresh token available"
        case .refreshFailed:
            return "Failed to refresh authentication token"
        case .loginFailed:
            return "Login failed. Please try again"
        case .urlGenerationFailed:
            return "Failed to generate authorization URL"
        case .invalidResponse:
            return "Invalid response from server"
        }
    }
}

// MARK: - Preview

#if DEBUG
struct LoginView_Previews: PreviewProvider {
    static var previews: some View {
        let authService = AuthenticationService()
        LoginView()
            .environmentObject(authService)
            .environmentObject(AuthStateManager(authService: authService, enableOAuth: true))
    }
}
#endif
