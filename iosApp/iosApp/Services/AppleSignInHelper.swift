import Foundation
import AuthenticationServices
import SwiftUI

/**
 * Helper class to manage Apple Sign-In flow on iOS.
 *
 * This wraps Apple's AuthenticationServices framework and provides a clean interface for:
 * - Launching the native Apple Sign-In sheet
 * - Extracting the authorization code and identity token
 * - Handling errors (user cancellation, failures, etc.)
 *
 * Usage:
 * ```swift
 * let appleSignInHelper = AppleSignInHelper()
 *
 * // Present sign-in sheet
 * appleSignInHelper.signIn { result in
 *     switch result {
 *     case .success(let credentials):
 *         // Pass to AuthenticationService.loginWithApple(credentials.authCode, userInfo: credentials.idToken)
 *         print("Authorization code: \(credentials.authCode)")
 *     case .failure(let error):
 *         print("Sign-in failed: \(error.localizedDescription)")
 *     }
 * }
 * ```
 */
@MainActor
class AppleSignInHelper: NSObject, ObservableObject {

    // MARK: - Published Properties

    /// Current sign-in state
    @Published var isSigningIn: Bool = false

    /// Last sign-in error (if any)
    @Published var lastError: AppleSignInError?

    // MARK: - Private Properties

    private var continuation: CheckedContinuation<AppleSignInCredentials, Error>?

    // MARK: - Types

    /// Apple Sign-In credentials
    struct AppleSignInCredentials {
        let authCode: String
        let idToken: String?
        let userIdentifier: String
        let email: String?
        let fullName: PersonNameComponents?
    }

    /// Apple Sign-In errors
    enum AppleSignInError: LocalizedError {
        case cancelled
        case failed(String)
        case invalidCredentials
        case networkError
        case notSupported
        case unknown

        var errorDescription: String? {
            switch self {
            case .cancelled:
                return "Sign-in was cancelled by the user"
            case .failed(let message):
                return "Sign-in failed: \(message)"
            case .invalidCredentials:
                return "Invalid credentials received from Apple"
            case .networkError:
                return "Network error. Please check your connection and try again"
            case .notSupported:
                return "Apple Sign-In is not supported on this device"
            case .unknown:
                return "An unknown error occurred. Please try again"
            }
        }
    }

    // MARK: - Public Methods

    /**
     * Initiate Apple Sign-In flow.
     *
     * This presents the native Apple Sign-In sheet to the user.
     * The result is returned via the completion handler.
     *
     * @return Result containing AppleSignInCredentials on success, or error on failure
     */
    func signIn() async throws -> AppleSignInCredentials {
        // Check if Apple Sign-In is available
        guard #available(iOS 13.0, *) else {
            throw AppleSignInError.notSupported
        }

        isSigningIn = true
        defer { isSigningIn = false }

        return try await withCheckedThrowingContinuation { continuation in
            self.continuation = continuation

            let appleIDProvider = ASAuthorizationAppleIDProvider()
            let request = appleIDProvider.createRequest()

            // Request user information
            request.requestedScopes = [.fullName, .email]

            let authorizationController = ASAuthorizationController(authorizationRequests: [request])
            authorizationController.delegate = self
            authorizationController.presentationContextProvider = self
            authorizationController.performRequests()
        }
    }

    /**
     * Check credential state for a given user ID.
     *
     * Use this to verify if the user's Apple ID credentials are still valid.
     *
     * @param userIdentifier The user identifier from Apple Sign-In
     * @return Credential state (authorized, revoked, notFound, etc.)
     */
    func getCredentialState(for userIdentifier: String) async throws -> ASAuthorizationAppleIDProvider.CredentialState {
        let appleIDProvider = ASAuthorizationAppleIDProvider()
        return try await appleIDProvider.credentialState(forUserID: userIdentifier)
    }

    /**
     * Handle silent credential renewal.
     *
     * Use this to silently re-authenticate the user without showing UI,
     * if they've previously signed in.
     */
    func renewCredentials() async throws -> AppleSignInCredentials {
        guard #available(iOS 13.0, *) else {
            throw AppleSignInError.notSupported
        }

        isSigningIn = true
        defer { isSigningIn = false }

        return try await withCheckedThrowingContinuation { continuation in
            self.continuation = continuation

            let appleIDProvider = ASAuthorizationAppleIDProvider()
            let request = appleIDProvider.createRequest()

            // Silent renewal doesn't request scopes
            request.requestedScopes = []

            let authorizationController = ASAuthorizationController(authorizationRequests: [request])
            authorizationController.delegate = self
            authorizationController.presentationContextProvider = self
            authorizationController.performRequests()
        }
    }
}

// MARK: - ASAuthorizationControllerDelegate

extension AppleSignInHelper: ASAuthorizationControllerDelegate {

    func authorizationController(controller: ASAuthorizationController, didCompleteWithAuthorization authorization: ASAuthorization) {
        guard let appleIDCredential = authorization.credential as? ASAuthorizationAppleIDCredential else {
            let error = AppleSignInError.invalidCredentials
            lastError = error
            continuation?.resume(throwing: error)
            continuation = nil
            return
        }

        // Extract authorization code
        guard let authCodeData = appleIDCredential.authorizationCode,
              let authCode = String(data: authCodeData, encoding: .utf8) else {
            let error = AppleSignInError.invalidCredentials
            lastError = error
            continuation?.resume(throwing: error)
            continuation = nil
            return
        }

        // Extract identity token (optional)
        var idToken: String?
        if let identityTokenData = appleIDCredential.identityToken {
            idToken = String(data: identityTokenData, encoding: .utf8)
        }

        let credentials = AppleSignInCredentials(
            authCode: authCode,
            idToken: idToken,
            userIdentifier: appleIDCredential.user,
            email: appleIDCredential.email,
            fullName: appleIDCredential.fullName
        )

        lastError = nil
        continuation?.resume(returning: credentials)
        continuation = nil
    }

    func authorizationController(controller: ASAuthorizationController, didCompleteWithError error: Error) {
        let signInError: AppleSignInError

        if let authError = error as? ASAuthorizationError {
            switch authError.code {
            case .canceled:
                signInError = .cancelled
            case .failed:
                signInError = .failed(authError.localizedDescription)
            case .invalidResponse:
                signInError = .invalidCredentials
            case .notHandled:
                signInError = .failed("Request not handled")
            case .notInteractive:
                signInError = .failed("Not interactive")
            case .unknown:
                signInError = .unknown
            @unknown default:
                signInError = .unknown
            }
        } else {
            // Check for network errors
            let nsError = error as NSError
            if nsError.domain == NSURLErrorDomain {
                signInError = .networkError
            } else {
                signInError = .failed(error.localizedDescription)
            }
        }

        lastError = signInError
        continuation?.resume(throwing: signInError)
        continuation = nil
    }
}

// MARK: - ASAuthorizationControllerPresentationContextProviding

extension AppleSignInHelper: ASAuthorizationControllerPresentationContextProviding {

    func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        // Return the key window for presenting the sign-in sheet
        guard let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let window = scene.windows.first else {
            fatalError("No window available for Apple Sign-In presentation")
        }
        return window
    }
}

// MARK: - SwiftUI Button View

/**
 * SwiftUI view for Apple Sign-In button with native styling.
 *
 * This provides a pre-styled button that matches Apple's Human Interface Guidelines.
 *
 * Usage:
 * ```swift
 * AppleSignInButton {
 *     // Handle sign-in
 * }
 * ```
 */
@available(iOS 14.0, *)
struct AppleSignInButton: View {
    var action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack {
                Image(systemName: "apple.logo")
                    .font(.title3)
                Text("Sign in with Apple")
                    .font(.headline)
            }
            .frame(maxWidth: .infinity)
            .padding()
            .background(Color.black)
            .foregroundColor(.white)
            .cornerRadius(8)
        }
    }
}

// MARK: - Native Sign In Button (iOS 13+)

/**
 * Wrapper for native ASAuthorizationAppleIDButton (iOS 13+).
 *
 * This uses Apple's native button implementation with proper styling.
 */
@available(iOS 13.0, *)
struct NativeAppleSignInButton: UIViewRepresentable {
    var action: () -> Void
    var buttonType: ASAuthorizationAppleIDButton.ButtonType = .signIn
    var buttonStyle: ASAuthorizationAppleIDButton.Style = .black

    func makeUIView(context: Context) -> ASAuthorizationAppleIDButton {
        let button = ASAuthorizationAppleIDButton(type: buttonType, style: buttonStyle)
        button.addTarget(context.coordinator, action: #selector(Coordinator.handleTap), for: .touchUpInside)
        return button
    }

    func updateUIView(_ uiView: ASAuthorizationAppleIDButton, context: Context) {
        // No updates needed
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(action: action)
    }

    class Coordinator: NSObject {
        var action: () -> Void

        init(action: @escaping () -> Void) {
            self.action = action
        }

        @objc func handleTap() {
            action()
        }
    }
}
