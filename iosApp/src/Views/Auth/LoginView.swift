import SwiftUI
import AuthenticationServices

// Colors and LiquidGlass components are defined in Theme/WakeveColors.swift and UIComponents/

/**
 * Login view for iOS using SwiftUI with Liquid Glass design system.
 *
 * Provides a native iOS login experience with:
 * - Sign in with Apple button (native ASAuthorizationAppleIDButton)
 * - Liquid Glass UI components for modern glassmorphism aesthetic
 * - App branding and welcome message
 * - Release-safe guest access for local-only exploration
 * - Loading indicator during authentication
 * - Error handling with retry option
 *
 * Design System:
 * - Background: semantic WakeveTheme colors with dark-mode-first contrast
 * - Colors: shared iOS design tokens, with light-mode fallback
 */
struct LoginView: View {
    @StateObject private var appleSignInHelper = AppleSignInHelper()
    @EnvironmentObject var authService: AuthenticationService
    @EnvironmentObject var authStateManager: AuthStateManager
    @Environment(\.openURL) private var openURL
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    
    @State private var isLoading = false
    @State private var errorMessage: String?
    @State private var showError = false
    
    // MARK: - Body
    
    var body: some View {
        ZStack {
            backgroundGradient
                .ignoresSafeArea()
            
            // Pattern overlay
            patternOverlay
            
            ScrollView(showsIndicators: false) {
                VStack(spacing: dynamicTypeSize.isAccessibilitySize ? 28 : 42) {
                    appBrandingView

                    welcomeTextView

                    loginValueProofCard

                    signInSection
                }
                .padding(.horizontal, 24)
                .padding(.top, dynamicTypeSize.isAccessibilitySize ? 48 : 72)
                .padding(.bottom, 48)
                .frame(maxWidth: .infinity)
            }
        }
        .alert(String(localized: "auth.sign_in_failed"), isPresented: $showError) {
            Button(String(localized: "auth.try_again"), role: .cancel) {
                showError = false
            }
        } message: {
            Text(errorMessage ?? String(localized: "auth.error.unknown"))
        }
    }

    private var loginValueProofCard: some View {
        WakeveContentCard(prominence: .regular, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.sm) {
                HStack(spacing: WakeveTheme.Spacing.sm) {
                    Image(systemName: "sparkles")
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundColor(WakeveTheme.ColorToken.accent(for: colorScheme))

                    Text(String(localized: "auth.value_proof_title"))
                        .font(WakeveTheme.Typography.rowTitle)
                        .foregroundColor(primaryTextColor)
                }

                VStack(alignment: .leading, spacing: WakeveTheme.Spacing.xs) {
                    LoginProofRow(text: String(localized: "auth.value_proof_invite"))
                    LoginProofRow(text: String(localized: "auth.value_proof_decide"))
                    LoginProofRow(text: String(localized: "auth.value_proof_relaunch"))
                }
            }
        }
        .accessibilityIdentifier("loginValueProofCard")
    }
    
    // MARK: - Background Components
    
    private var backgroundGradient: some View {
        LinearGradient(
            gradient: Gradient(colors: backgroundColors),
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }

    private var backgroundColors: [Color] {
        if colorScheme == .dark {
            return [
                WakeveTheme.ColorToken.midnight,
                Color(hex: "0C2430"),
                WakeveTheme.ColorToken.graphite
            ]
        }

        return [
            WakeveTheme.ColorToken.softIvory,
            Color(hex: "EAF1F6"),
            Color(hex: "D8E5EE")
        ]
    }

    private var primaryTextColor: Color {
        WakeveTheme.ColorToken.primaryText(for: colorScheme)
    }

    private var secondaryTextColor: Color {
        WakeveTheme.ColorToken.secondaryText(for: colorScheme)
    }

    private var glassStrokeColor: Color {
        WakeveTheme.ColorToken.cardBorder(for: colorScheme)
    }
    
    private var patternOverlay: some View {
        GeometryReader { geometry in
            Image(systemName: "calendar")
                .font(.system(size: 300))
                .foregroundColor(WakeveTheme.ColorToken.accent(for: colorScheme).opacity(colorScheme == .dark ? 0.08 : 0.12))
                .offset(x: geometry.size.width * 0.3, y: geometry.size.height * 0.2)
                .rotationEffect(.degrees(-15))
        }
    }
    
    // MARK: - Branding
    
    private var appBrandingView: some View {
        ZStack {
            // Outer glow
            Circle()
                .fill(WakeveTheme.ColorToken.accent(for: colorScheme).opacity(colorScheme == .dark ? 0.18 : 0.2))
                .frame(width: 140, height: 140)
                .blur(radius: 10)
            
            // Glass card circle
            Circle()
                .fill(.ultraThinMaterial)
                .frame(width: 120, height: 120)
                .overlay(
                    Circle()
                        .stroke(glassStrokeColor, lineWidth: 1)
                )
                .shadow(color: .black.opacity(colorScheme == .dark ? 0.18 : 0.08), radius: 8, x: 0, y: 4)
            
            // App icon
            Image(systemName: "calendar.badge.plus")
                .font(.system(size: 52, weight: .light))
                .foregroundColor(primaryTextColor)
                .shadow(color: .black.opacity(colorScheme == .dark ? 0.2 : 0.08), radius: 4, x: 0, y: 2)
        }
    }
    
    // MARK: - Welcome Text
    
    private var welcomeTextView: some View {
        VStack(spacing: 12) {
            Text(String(localized: "auth.sign_in"))
                .font(WakeveTheme.Typography.display)
                .foregroundColor(primaryTextColor)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
                .shadow(color: .black.opacity(colorScheme == .dark ? 0.18 : 0.04), radius: 4, x: 0, y: 2)
            
            Text(String(localized: "auth.sign_in_subtitle"))
                .font(WakeveTheme.Typography.body)
                .foregroundColor(secondaryTextColor)
                .multilineTextAlignment(.center)
                .lineLimit(3)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.horizontal, 8)
        }
    }
    
    // MARK: - Sign In Section
    
    @ViewBuilder
    private var signInSection: some View {
        if isLoading {
            loadingView
        } else {
            signInButtonsView
        }
    }
    
    private var loadingView: some View {
        VStack(spacing: 20) {
            ProgressView()
                .progressViewStyle(CircularProgressViewStyle(tint: WakeveTheme.ColorToken.accent(for: colorScheme)))
                .scaleEffect(1.5)
                .accessibilityLabel(String(localized: "auth.signing_in"))
            
            Text(String(localized: "auth.signing_in"))
                .font(WakeveTheme.Typography.bodySemibold)
                .foregroundColor(secondaryTextColor)
        }
        .padding(.bottom, 80)
    }
    
    private var signInButtonsView: some View {
        VStack(spacing: 20) {
            // Native Apple Sign-In Button
            SignInWithAppleButton(.signIn) { request in
                request.requestedScopes = [.fullName, .email]
            } onCompletion: { result in
                handleAppleSignInResult(result)
            }
            .signInWithAppleButtonStyle(colorScheme == .dark ? .white : .black)
            .frame(height: 56)
            .cornerRadius(16)
            .shadow(color: Color.black.opacity(0.15), radius: 10, x: 0, y: 5)
            .accessibilityLabel(Text(String(localized: "auth.apple_sign_in_accessibility_label")))
            .accessibilityHint(Text(String(localized: "auth.apple_sign_in_accessibility_hint")))

            guestAccessButton
            
            #if DEBUG
            // Development Mode - Skip Authentication
            developmentSkipButton
            #endif
            
            // Privacy & Terms
            privacyTermsView
        }
        .padding(.horizontal, 0)
    }

    private var guestAccessButton: some View {
        Button(action: { continueAsGuest() }) {
            HStack(spacing: 8) {
                Text(String(localized: "auth.continue_as_guest"))
                    .lineLimit(2)
                    .multilineTextAlignment(.center)
                Image(systemName: "chevron.right")
            }
            .font(.headline)
            .foregroundColor(primaryTextColor)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 15)
            .background(WakeveTheme.ColorToken.glassTint(for: colorScheme))
            .overlay(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .stroke(glassStrokeColor, lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        }
        .accessibilityLabel(Text(String(localized: "auth.continue_as_guest_accessibility_label")))
        .accessibilityHint(Text(String(localized: "auth.continue_as_guest_accessibility_hint")))
    }
    
    #if DEBUG
    private var developmentSkipButton: some View {
        Button(action: { skipAuthForDevelopment() }) {
            HStack(spacing: 8) {
                Text(String(localized: "auth.skip_dev"))
                    .lineLimit(2)
                    .multilineTextAlignment(.center)
                Image(systemName: "chevron.right")
            }
            .font(.headline)
            .foregroundColor(secondaryTextColor)
            .padding(.horizontal, 24)
            .padding(.vertical, 12)
            .background(WakeveTheme.ColorToken.subtleCardFill(for: colorScheme))
            .overlay(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .stroke(glassStrokeColor, lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        }
        .accessibilityLabel(Text(String(localized: "auth.skip_dev_accessibility_label")))
        .accessibilityHint(Text(String(localized: "auth.skip_dev_accessibility_hint")))
    }
    #endif
    
    private var privacyTermsView: some View {
        VStack(spacing: 6) {
            Text(String(localized: "auth.privacy_agree"))
                .font(WakeveTheme.Typography.caption)
                .foregroundColor(secondaryTextColor)
            
            VStack(spacing: 6) {
                Button {
                    openLegalURL("https://wakeve.app/privacy")
                } label: {
                    Text(String(localized: "auth.privacy_policy"))
                        .font(WakeveTheme.Typography.caption)
                        .foregroundColor(primaryTextColor)
                        .multilineTextAlignment(.center)
                }
                .frame(minHeight: 44)
                .accessibilityLabel(Text(String(localized: "auth.read_privacy_policy_accessibility_label")))
                
                Text(String(localized: "auth.and"))
                    .font(WakeveTheme.Typography.caption)
                    .foregroundColor(secondaryTextColor)
                
                Button {
                    openLegalURL("https://wakeve.app/terms")
                } label: {
                    Text(String(localized: "auth.terms_of_service"))
                        .font(WakeveTheme.Typography.caption)
                        .foregroundColor(primaryTextColor)
                        .multilineTextAlignment(.center)
                }
                .frame(minHeight: 44)
                .accessibilityLabel(Text(String(localized: "auth.read_terms_accessibility_label")))
            }
        }
    }
    
    // MARK: - Actions

    private func openLegalURL(_ urlString: String) {
        guard let url = URL(string: urlString) else { return }
        openURL(url)
    }

    private func continueAsGuest() {
        isLoading = true

        Task {
            await authStateManager.continueAsGuest()

            if let authManagerError = authStateManager.authError {
                errorMessage = authManagerError
                showError = true
            }

            isLoading = false
        }
    }

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
                    
                    // Extract user info for first-time sign-in
                    let email = appleIDCredential.email
                    var fullName: String?
                    if let nameComponents = appleIDCredential.fullName {
                        let formatter = PersonNameComponentsFormatter()
                        let formattedName = formatter.string(from: nameComponents)
                        if !formattedName.isEmpty {
                            fullName = formattedName
                        }
                    }

                    // Login with backend via AuthStateManager
                    await authStateManager.signIn(
                        provider: "apple",
                        authCode: authCode,
                        userInfo: idToken,
                        email: email,
                        fullName: fullName
                    )

                    // Check if auth state manager reported an error
                    if let authManagerError = authStateManager.authError {
                        errorMessage = authManagerError
                        showError = true
                    } else if authStateManager.isAuthenticated {
                        debugLog("[LoginView] Login successful")
                    }
                    
                case .failure(let error):
                    if let authError = error as? ASAuthorizationError {
                        switch authError.code {
                        case .canceled:
                            // User cancelled - don't show error
                            break
                        case .failed:
                            errorMessage = String(localized: "auth.error.sign_in_failed_retry")
                            showError = true
                        case .invalidResponse:
                            errorMessage = String(localized: "auth.error.invalid_response")
                            showError = true
                        case .notHandled:
                            errorMessage = String(localized: "auth.error.not_handled")
                            showError = true
                        case .unknown:
                            errorMessage = String(localized: "auth.error.unknown")
                            showError = true
                        default:
                            errorMessage = String(
                                format: String(localized: "auth.error.sign_in_failed_format"),
                                authError.localizedDescription
                            )
                            showError = true
                        }
                    } else {
                        errorMessage = String(
                            format: String(localized: "auth.error.sign_in_failed_format"),
                            error.localizedDescription
                        )
                        showError = true
                    }
                }
            } catch {
                errorMessage = String(
                    format: String(localized: "auth.error.authentication_failed_format"),
                    error.localizedDescription
                )
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

private struct LoginProofRow: View {
    let text: String

    var body: some View {
        HStack(alignment: .top, spacing: WakeveTheme.Spacing.xs) {
            Image(systemName: "checkmark.circle.fill")
                .font(.system(size: 14, weight: .semibold))
                .foregroundColor(WakeveColors.success)
                .padding(.top, 2)

            Text(text)
                .font(WakeveTheme.Typography.callout)
                .foregroundColor(.secondary)
                .fixedSize(horizontal: false, vertical: true)
        }
    }
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
            .preferredColorScheme(.light)
    }
}

#Preview("Login - Dark") {
    let authService = AuthenticationService()
    LoginView()
        .environmentObject(authService)
        .environmentObject(AuthStateManager(authService: authService, enableOAuth: true))
        .preferredColorScheme(.dark)
}
#endif
