import SwiftUI
import AuthenticationServices

// MARK: - Design System Colors

extension Color {
    static let wakevPrimary = Color(hex: "2563EB")
    static let wakevAccent = Color(hex: "7C3AED")
    static let wakevSuccess = Color(hex: "059669")
    static let wakevWarning = Color(hex: "D97706")
    static let wakevError = Color(hex: "DC2626")
    
    init(hex: String) {
        let scanner = Scanner(string: hex)
        var rgbValue: UInt64 = 0
        scanner.scanHexInt64(&rgbValue)
        
        let r = Double((rgbValue & 0xFF0000) >> 16) / 255.0
        let g = Double((rgbValue & 0x00FF00) >> 8) / 255.0
        let b = Double(rgbValue & 0x0000FF) / 255.0
        
        self.init(red: r, green: g, blue: b)
    }
}

// MARK: - Liquid Glass Components

enum LiquidGlassButtonStyle {
    case primary, secondary, text, icon
}

enum LiquidGlassButtonSize {
    case small, medium, large
}

struct LiquidGlassButton: View {
    let title: String?
    let icon: String?
    let style: LiquidGlassButtonStyle
    let size: LiquidGlassButtonSize
    let isDisabled: Bool
    let action: () -> Void
    
    init(
        title: String? = nil,
        icon: String? = nil,
        style: LiquidGlassButtonStyle = .primary,
        size: LiquidGlassButtonSize = .medium,
        isDisabled: Bool = false,
        action: @escaping () -> Void
    ) {
        self.title = title
        self.icon = icon
        self.style = style
        self.size = size
        self.isDisabled = isDisabled
        self.action = action
    }
    
    private var buttonHeight: CGFloat {
        switch size {
        case .small: return 36
        case .medium: return 44
        case .large: return 52
        }
    }
    
    private var foregroundColor: Color {
        switch style {
        case .primary: return .white
        case .secondary: return .wakevPrimary
        case .text: return .wakevPrimary
        case .icon: return .white
        }
    }
    
    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                if let icon = icon {
                    Image(systemName: icon)
                        .font(.system(size: 16, weight: .semibold))
                }
                if let title = title {
                    Text(title)
                        .font(.subheadline.weight(.semibold))
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: buttonHeight)
            .foregroundColor(isDisabled ? .gray : foregroundColor)
            .background(buttonBackground)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        }
        .disabled(isDisabled)
        .opacity(isDisabled ? 0.6 : 1.0)
    }
    
    @ViewBuilder
    private var buttonBackground: some View {
        if isDisabled {
            Color.gray.opacity(0.1)
        } else if style == .primary {
            LinearGradient(
                gradient: Gradient(colors: [.wakevPrimary, .wakevAccent]),
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        } else {
            Color.clear
        }
    }
}

/**
 * Login view for iOS using SwiftUI with Liquid Glass design system.
 *
 * Provides a native iOS login experience with:
 * - Sign in with Apple button (native ASAuthorizationAppleIDButton)
 * - Liquid Glass UI components for modern glassmorphism aesthetic
 * - App branding and welcome message
 * - Loading indicator during authentication
 * - Error handling with retry option
 *
 * Design System:
 * - Gradient: .wakevPrimary → .wakevAccent
 * - Cards: LiquidGlassCard with ultra-thin material
 * - Buttons: LiquidGlassButton with gradient background
 * - Colors: Full WakevColors palette
 */
struct LoginView: View {
    @StateObject private var appleSignInHelper = AppleSignInHelper()
    @EnvironmentObject var authService: AuthenticationService
    @EnvironmentObject var authStateManager: AuthStateManager
    
    @State private var isLoading = false
    @State private var errorMessage: String?
    @State private var showError = false
    
    // MARK: - Body
    
    var body: some View {
        ZStack {
            // Design system gradient background
            backgroundGradient
                .ignoresSafeArea()
            
            // Pattern overlay
            patternOverlay
            
            // Main content
            VStack(spacing: 0) {
                Spacer()
                
                // App Branding
                appBrandingView
                    .padding(.bottom, 32)
                
                // Welcome Text
                welcomeTextView
                    .padding(.bottom, 60)
                
                Spacer()
                
                // Sign-in section
                signInSection
                    .padding(.bottom, 60)
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
    
    // MARK: - Background Components
    
    private var backgroundGradient: some View {
        LinearGradient(
            gradient: Gradient(colors: [.wakevPrimary, .wakevAccent]),
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
    }
    
    private var patternOverlay: some View {
        GeometryReader { geometry in
            Image(systemName: "calendar")
                .font(.system(size: 300))
                .foregroundColor(.white.opacity(0.05))
                .offset(x: geometry.size.width * 0.3, y: geometry.size.height * 0.2)
                .rotationEffect(.degrees(-15))
        }
    }
    
    // MARK: - Branding
    
    private var appBrandingView: some View {
        ZStack {
            // Outer glow
            Circle()
                .fill(Color.white.opacity(0.15))
                .frame(width: 140, height: 140)
                .blur(radius: 10)
            
            // Glass card circle
            Circle()
                .fill(.ultraThinMaterial)
                .frame(width: 120, height: 120)
                .overlay(
                    Circle()
                        .stroke(.white.opacity(0.3), lineWidth: 1)
                )
                .shadow(color: .black.opacity(0.1), radius: 8, x: 0, y: 4)
            
            // App icon
            Image(systemName: "calendar.badge.plus")
                .font(.system(size: 52, weight: .light))
                .foregroundColor(.white)
                .shadow(color: .black.opacity(0.2), radius: 4, x: 0, y: 2)
        }
    }
    
    // MARK: - Welcome Text
    
    private var welcomeTextView: some View {
        VStack(spacing: 12) {
            Text("Sign In")
                .font(.system(size: 42, weight: .bold))
                .foregroundColor(.white)
                .shadow(color: .black.opacity(0.1), radius: 4, x: 0, y: 2)
            
            Text("Continue with your Apple ID to get started")
                .font(.system(size: 17))
                .foregroundColor(.white.opacity(0.9))
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)
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
                .progressViewStyle(CircularProgressViewStyle(tint: .white))
                .scaleEffect(1.5)
            
            Text("Signing in...")
                .font(.system(size: 17, weight: .medium))
                .foregroundColor(.white.opacity(0.9))
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
            .signInWithAppleButtonStyle(.white)
            .frame(height: 56)
            .cornerRadius(16)
            .shadow(color: Color.black.opacity(0.15), radius: 10, x: 0, y: 5)
            .accessibilityLabel("Sign in with Apple")
            .accessibilityHint("Use your Apple ID to authenticate")
            
            #if DEBUG
            // Development Mode - Skip Authentication
            developmentSkipButton
            #endif
            
            // Privacy & Terms
            privacyTermsView
        }
        .padding(.horizontal, 40)
    }
    
    #if DEBUG
    private var developmentSkipButton: some View {
        LiquidGlassButton(
            title: "Skip (Development)",
            icon: "chevron.right",
            style: .secondary,
            size: .medium
        ) {
            skipAuthForDevelopment()
        }
        .accessibilityLabel("Development mode: Skip authentication")
        .accessibilityHint("Creates a mock authenticated session for testing")
    }
    #endif
    
    private var privacyTermsView: some View {
        VStack(spacing: 6) {
            Text("By signing in, you agree to our")
                .font(.system(size: 13))
                .foregroundColor(.white.opacity(0.7))
            
            HStack(spacing: 6) {
                Button {
                    // TODO: Open privacy policy
                } label: {
                    Text("Privacy Policy")
                        .font(.system(size: 13, weight: .medium))
                        .foregroundColor(.white)
                }
                .accessibilityLabel("Read Privacy Policy")
                
                Text("and")
                    .font(.system(size: 13))
                    .foregroundColor(.white.opacity(0.7))
                
                Button {
                    // TODO: Open terms
                } label: {
                    Text("Terms of Service")
                        .font(.system(size: 13, weight: .medium))
                        .foregroundColor(.white)
                }
                .accessibilityLabel("Read Terms of Service")
            }
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
