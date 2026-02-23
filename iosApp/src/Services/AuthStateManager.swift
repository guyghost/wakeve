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

    /// Last authentication error message
    @Published var authError: String? = nil

    private let authService: AuthenticationService
    private let enableOAuth: Bool

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
            print("[AuthStateManager] OAuth is disabled, using development mode")
            await setAuthStateForDevelopment(
                userId: "dev-\(UUID().uuidString.prefix(8))",
                accessToken: "dev-token"
            )
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

            // Register push token with backend now that user is authenticated
            APNsService.shared.registerTokenWithBackendIfAuthenticated()

            print("[AuthStateManager] Successfully signed in as \(loginResponse.user.email)")

        } catch let error as AuthError {
            print("[AuthStateManager] Authentication failed: \(error.localizedDescription)")
            self.isAuthenticated = false
            self.currentUser = nil
            self.authError = error.localizedDescription
        } catch {
            print("[AuthStateManager] Authentication failed: \(error)")
            self.isAuthenticated = false
            self.currentUser = nil
            self.authError = error.localizedDescription
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
                print("[AuthStateManager] Push token unregistration failed: \(error?.localizedDescription ?? "unknown")")
            }
        }

        Task {
            await authService.signOut()
        }

        // Clear authentication state
        isAuthenticated = false
        currentUser = nil
        authError = nil

        print("[AuthStateManager] User signed out")
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
            let hasValidSession = await authService.isAuthenticated()

            if hasValidSession {
                // Restore user profile from cached data
                if let user = await authService.getCurrentUser() {
                    self.isAuthenticated = true
                    self.currentUser = user

                    // Re-register push token on app launch if authenticated
                    APNsService.shared.registerTokenWithBackendIfAuthenticated()

                    print("[AuthStateManager] Restored session for user: \(user.id)")
                } else {
                    // Token exists but no user data - try refresh
                    self.isAuthenticated = false
                    self.currentUser = nil
                }
            } else {
                self.isAuthenticated = false
                self.currentUser = nil
            }
        }
    }

    // MARK: - Token Refresh

    /**
     * Refresh authentication token if needed.
     *
     * Called by background token refresh and on app resume.
     */
    func refreshTokenIfNeeded() async {
        guard isAuthenticated else { return }

        do {
            let response = try await authService.refreshToken()

            // Update user data from refresh response
            self.currentUser = User(
                id: response.user.id,
                name: response.user.name,
                email: response.user.email,
                avatarUrl: response.user.avatarUrl
            )

            print("[AuthStateManager] Token refreshed successfully")
        } catch {
            print("[AuthStateManager] Token refresh failed: \(error)")

            if case AuthError.tokenExpired = error {
                // Refresh token expired - user needs to re-login
                self.isAuthenticated = false
                self.currentUser = nil
                self.authError = "Session expired. Please sign in again."
            }
        }
    }

    // MARK: - Development Mode

    /**
     * Set authentication state for development (bypasses normal auth flow).
     *
     * - Parameters:
     *   - userId: Mock user ID
     *   - accessToken: Mock access token
     */
    func setAuthStateForDevelopment(userId: String, accessToken: String) async {
        self.isAuthenticated = true
        self.currentUser = User(
            id: userId,
            name: "Dev User",
            email: "dev@example.com",
            avatarUrl: nil
        )
    }
}