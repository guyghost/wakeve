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
            // Background gradient
            LinearGradient(
                gradient: Gradient(colors: [
                    Color(red: 0.4, green: 0.5, blue: 0.92),
                    Color(red: 0.46, green: 0.29, blue: 0.64)
                ]),
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()

            VStack(spacing: 32) {
                Spacer()

                // App Branding
                AppBrandingView()

                // Welcome Text
                VStack(spacing: 8) {
                    Text("Welcome to Wakeve")
                        .font(.largeTitle)
                        .fontWeight(.bold)
                        .foregroundColor(.white)

                    Text("Collaborative event planning made easy")
                        .font(.body)
                        .foregroundColor(.white.opacity(0.9))
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 32)
                }

                Spacer()

                // Sign-in Button
                if isLoading {
                    VStack(spacing: 16) {
                        ProgressView()
                            .progressViewStyle(CircularProgressViewStyle(tint: .white))
                            .scaleEffect(1.5)

                        Text("Signing in...")
                            .font(.headline)
                            .foregroundColor(.white.opacity(0.9))
                    }
                } else {
                    // Native Apple Sign-In Button
                    SignInWithAppleButton(.signIn) { request in
                        request.requestedScopes = [.fullName, .email]
                    } onCompletion: { result in
                        handleAppleSignInResult(result)
                    }
                    .signInWithAppleButtonStyle(.white)
                    .frame(height: 56)
                    .cornerRadius(12)
                    .padding(.horizontal, 32)
                }

                // Privacy & Terms
                VStack(spacing: 4) {
                    Text("By signing in, you agree to our")
                        .font(.caption)
                        .foregroundColor(.white.opacity(0.7))

                    HStack(spacing: 4) {
                        Button("Privacy Policy") {
                            // TODO: Open privacy policy
                        }
                        .font(.caption)
                        .foregroundColor(.white)

                        Text("and")
                            .font(.caption)
                            .foregroundColor(.white.opacity(0.7))

                        Button("Terms of Service") {
                            // TODO: Open terms
                        }
                        .font(.caption)
                        .foregroundColor(.white)
                    }
                }
                .padding(.bottom, 32)
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
                .fill(Color.white)
                .frame(width: 120, height: 120)
                .shadow(color: .black.opacity(0.2), radius: 10, x: 0, y: 5)

            Text("W")
                .font(.system(size: 60, weight: .bold))
                .foregroundColor(Color(red: 0.4, green: 0.5, blue: 0.92))
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
                    await authStateManager.login(
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
}

// MARK: - Authentication Error

enum AuthenticationError: LocalizedError {
    case invalidCredentials
    case networkError
    case serverError(String)

    var errorDescription: String? {
        switch self {
        case .invalidCredentials:
            return "Invalid credentials received from Apple"
        case .networkError:
            return "Network error. Please check your connection and try again"
        case .serverError(let message):
            return "Server error: \(message)"
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
