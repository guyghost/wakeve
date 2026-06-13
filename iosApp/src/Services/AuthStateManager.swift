//
//  AuthStateManager.swift
//  iosApp
//
//  Created by Wakev on 2025
//

import Foundation
import SwiftUI

// MARK: - AuthStateManager

@MainActor
public class AuthStateManager: ObservableObject {

    /// Current authentication state
    @Published var isAuthenticated: Bool = false

    /// Current authenticated user
    @Published var currentUser: User? = nil

    /// Loading state during authentication
    @Published var isLoading: Bool = false

    /// Whether the startup session restoration has completed.
    @Published var hasCheckedAuthStatus: Bool = false

    /// Last authentication error message
    @Published var authError: String? = nil

    private let authService: AuthenticationService
    private let enableOAuth: Bool

    private enum GuestSessionKeys {
        static let userId = "wakeve_guest_user_id"
        static let userName = "wakeve_guest_user_name"
    }

    init(authService: AuthenticationService, enableOAuth: Bool = false) {
        self.authService = authService
        self.enableOAuth = enableOAuth
    }

    // MARK: - Sign In

    /**
     * Sign in with OAuth provider.
     *
     * Routes the authentication to the correct backend endpoint based on provider.
     *
     * - Parameters:
     *   - provider: OAuth provider ("apple" or "google")
     *   - authCode: Authorization code from the OAuth provider
     *   - userInfo: Identity token (JWT) from the OAuth provider
     *   - email: User email (optional, provided on first Apple sign-in)
     *   - fullName: User full name (optional, provided on first Apple sign-in)
     */
    func signIn(
        provider: String? = nil,
        authCode: String? = nil,
        userInfo: String? = nil,
        email: String? = nil,
        fullName: String? = nil
    ) async {
        guard enableOAuth else {
            #if DEBUG
            debugLog("[AuthStateManager] OAuth is disabled, using development mode")
            await setAuthStateForDevelopment(
                userId: "dev-\(UUID().uuidString.prefix(8))",
                accessToken: "dev-token"
            )
            #else
            self.isAuthenticated = false
            self.currentUser = nil
            self.authError = "Authentication is not configured for this build."
            self.hasCheckedAuthStatus = true
            #endif
            return
        }

        isLoading = true
        authError = nil

        do {
            let loginResponse: AuthLoginResponse

            switch provider {
            case "apple":
                guard let authCode = authCode else {
                    throw AuthError.invalidCredentials
                }
                loginResponse = try await authService.loginWithApple(
                    authCode: authCode,
                    idToken: userInfo,
                    email: email,
                    fullName: fullName
                )

            case "google":
                guard let idToken = userInfo else {
                    throw AuthError.invalidCredentials
                }
                loginResponse = try await authService.loginWithGoogle(
                    idToken: idToken,
                    email: email ?? "",
                    name: fullName
                )

            default:
                throw AuthError.authenticationFailed("Unknown provider: \(provider ?? "nil")")
            }

            // Update authenticated state from response
            self.isAuthenticated = true
            self.currentUser = User(
                id: loginResponse.user.id,
                name: loginResponse.user.name,
                email: loginResponse.user.email,
                avatarUrl: loginResponse.user.avatarUrl
            )
            self.authError = nil
            self.hasCheckedAuthStatus = true

            // Register push token with backend now that user is authenticated
            APNsService.shared.registerTokenWithBackendIfAuthenticated()

            debugLog("[AuthStateManager] Successfully signed in as \(loginResponse.user.email)")

        } catch let error as AuthError {
            debugLog("[AuthStateManager] Authentication failed: \(error.localizedDescription)")
            self.isAuthenticated = false
            self.currentUser = nil
            self.authError = error.localizedDescription
            self.hasCheckedAuthStatus = true
        } catch {
            debugLog("[AuthStateManager] Authentication failed: \(error)")
            self.isAuthenticated = false
            self.currentUser = nil
            self.authError = error.localizedDescription
            self.hasCheckedAuthStatus = true
        }

        isLoading = false
    }

    // MARK: - Sign Out

    /**
     * Sign out the current user and clear all stored tokens.
     */
    func signOut() {
        // Unregister push token from backend before clearing auth
        APNsService.shared.unregisterToken { success, error in
            if !success {
                debugLog("[AuthStateManager] Push token unregistration failed: \(error?.localizedDescription ?? "unknown")")
            }
        }

        Task {
            await authService.signOut()
        }

        clearGuestSession()

        // Clear authentication state
        isAuthenticated = false
        currentUser = nil
        authError = nil
        hasCheckedAuthStatus = true

        debugLog("[AuthStateManager] User signed out")
    }

    /**
     * Delete the current authenticated account after a server-confirmed erasure.
     *
     * If the backend cannot be reached, this throws and keeps credentials
     * available so the user can retry the deletion later.
     */
    func deleteCurrentAccount() async throws {
        guard isAuthenticated else {
            throw AuthError.authenticationFailed("Authentication required")
        }

        if isCurrentSessionGuest {
            await deleteGuestData()
            return
        }

        isLoading = true
        authError = nil
        defer {
            isLoading = false
            hasCheckedAuthStatus = true
        }

        _ = try await authService.deleteAccount()

        await completeLocalAccountDeletion()
        debugLog("[AuthStateManager] Account deleted and local state cleared")
    }

    /**
     * Clear local-only guest data without contacting the backend.
     */
    func deleteGuestData() async {
        isLoading = true
        authError = nil
        defer {
            isLoading = false
            hasCheckedAuthStatus = true
        }

        await completeLocalAccountDeletion()
        debugLog("[AuthStateManager] Guest data deleted locally")
    }

    // MARK: - Auth Status

    /**
     * Check existing authentication status on app startup.
     *
     * Reads stored tokens from Keychain and restores the session
     * if a valid (non-expired) token exists.
     */
    func checkAuthStatus() {
        Task {
            isLoading = true
            defer {
                isLoading = false
                hasCheckedAuthStatus = true
            }

            let hasValidSession = await authService.isAuthenticated()

            if hasValidSession {
                // Restore user profile from cached data
                if let user = await authService.getCurrentUser() {
                    self.isAuthenticated = true
                    self.currentUser = user

                    // Re-register push token on app launch if authenticated
                    APNsService.shared.registerTokenWithBackendIfAuthenticated()

                    debugLog("[AuthStateManager] Restored session for user: \(user.id)")
                } else {
                    // Token exists but no user data - try refresh
                    self.isAuthenticated = false
                    self.currentUser = nil
                }
            } else if let guestUser = restoreGuestUser() {
                self.isAuthenticated = true
                self.currentUser = guestUser
                self.authError = nil
            } else {
                self.isAuthenticated = false
                self.currentUser = nil
            }
        }
    }

    // MARK: - Guest Mode

    /**
     * Continue into Wakeve with a local-only guest session.
     *
     * Guest sessions do not create backend tokens, register push tokens, or enable
     * server synchronization. This keeps App Review and first-run exploration
     * aligned with the shared guest-mode specification.
     */
    func continueAsGuest() async {
        isLoading = true
        defer {
            isLoading = false
            hasCheckedAuthStatus = true
        }

        await authService.signOut()

        let guestUser = createOrRestoreGuestUser()
        self.isAuthenticated = true
        self.currentUser = guestUser
        self.authError = nil
    }

    // MARK: - Token Refresh

    /**
     * Refresh authentication token if needed.
     *
     * Called by background token refresh and on app resume.
     */
    func refreshTokenIfNeeded() async {
        guard isAuthenticated else { return }
        guard !isCurrentSessionGuest else { return }

        do {
            let response = try await authService.refreshToken()

            // Update user data from refresh response
            self.currentUser = User(
                id: response.user.id,
                name: response.user.name,
                email: response.user.email,
                avatarUrl: response.user.avatarUrl
            )

            debugLog("[AuthStateManager] Token refreshed successfully")
        } catch {
            debugLog("[AuthStateManager] Token refresh failed: \(error)")

            if case AuthError.tokenExpired = error {
                // Refresh token expired - user needs to re-login
                self.isAuthenticated = false
                self.currentUser = nil
                self.authError = "Session expired. Please sign in again."
            }
        }
    }

    var isCurrentSessionGuest: Bool {
        currentUser?.id.hasPrefix("guest-") == true
    }

    private func createOrRestoreGuestUser() -> User {
        if let existingGuest = restoreGuestUser() {
            return existingGuest
        }

        let guestId = "guest-\(UUID().uuidString.lowercased())"
        let guestName = String(localized: "auth.guest_display_name")

        UserDefaults.standard.set(guestId, forKey: GuestSessionKeys.userId)
        UserDefaults.standard.set(guestName, forKey: GuestSessionKeys.userName)

        return User(
            id: guestId,
            name: guestName,
            email: "",
            avatarUrl: nil
        )
    }

    private func restoreGuestUser() -> User? {
        guard let guestId = UserDefaults.standard.string(forKey: GuestSessionKeys.userId) else {
            return nil
        }

        return User(
            id: guestId,
            name: UserDefaults.standard.string(forKey: GuestSessionKeys.userName) ?? String(localized: "auth.guest_display_name"),
            email: "",
            avatarUrl: nil
        )
    }

    private func clearGuestSession() {
        UserDefaults.standard.removeObject(forKey: GuestSessionKeys.userId)
        UserDefaults.standard.removeObject(forKey: GuestSessionKeys.userName)
    }

    private func completeLocalAccountDeletion() async {
        await authService.clearLocalAccountData()
        clearGuestSession()
        isAuthenticated = false
        currentUser = nil
        authError = nil
    }

    #if DEBUG
    // MARK: - Development Mode

    static func shouldUseDevelopmentLaunchAuthentication(
        arguments: [String] = ProcessInfo.processInfo.arguments,
        environment: [String: String] = ProcessInfo.processInfo.environment
    ) -> Bool {
        arguments.contains("--wakeve-debug-authenticated") ||
        environment["WAKEVE_DEBUG_AUTHENTICATED"] == "1"
    }

    func authenticateForDevelopmentLaunchIfRequested(
        arguments: [String] = ProcessInfo.processInfo.arguments,
        environment: [String: String] = ProcessInfo.processInfo.environment
    ) async -> Bool {
        guard Self.shouldUseDevelopmentLaunchAuthentication(
            arguments: arguments,
            environment: environment
        ) else {
            return false
        }

        await setAuthStateForDevelopment(
            userId: "dev-\(UUID().uuidString.prefix(8))",
            accessToken: "dev-token"
        )

        return isAuthenticated
    }

    /**
     * Set authentication state for development (bypasses normal auth flow).
     *
     * - Parameters:
     *   - userId: Mock user ID
     *   - accessToken: Mock access token
     */
    func setAuthStateForDevelopment(userId: String, accessToken: String) async {
        do {
            try await authService.storeDevelopmentSession(userId: userId, accessToken: accessToken)
        } catch {
            self.isAuthenticated = false
            self.currentUser = nil
            self.authError = error.localizedDescription
            self.hasCheckedAuthStatus = true
            return
        }

        self.isAuthenticated = true
        self.hasCheckedAuthStatus = true
        self.authError = nil
        self.currentUser = User(
            id: userId,
            name: "Dev User",
            email: "dev@example.com",
            avatarUrl: nil
        )
    }
    #endif
}
