//
//  AuthStateManager.swift
//  iosApp
//
//  Created by Wakev on 2025
//

import Foundation
import Combine

/**
 * Authentication state enum.
 */
enum AuthState {
    case loading
    case unauthenticated
    case authenticated(userId: String, user: UserResponse)
    case error(message: String)
}

/**
 * Central authentication state manager for iOS.
 *
 * This class manages the authentication state across the entire application using
 * Combine publishers for reactive updates. It handles:
 * - Initial authentication check on app startup
 * - OAuth login flow coordination
 * - Token refresh and expiry handling
 * - Logout and session cleanup
 */
@MainActor
class AuthStateManager: ObservableObject {
    @Published private(set) var authState: AuthState = .loading
    @Published private(set) var currentUserId: String?
    @Published private(set) var currentAccessToken: String?

    private let authService: AuthenticationService
    private let enableOAuth: Bool
    private var tokenRefreshTask: Task<Void, Never>?

    init(
        authService: AuthenticationService,
        enableOAuth: Bool = false
    ) {
        self.authService = authService
        self.enableOAuth = enableOAuth
    }

    /**
     * Initialize authentication state.
     *
     * This should be called when the app starts. It checks for existing
     * authentication and validates the stored tokens.
     */
    func initialize() async {
        authState = .loading

        // If OAuth is disabled, skip auth check
        guard enableOAuth else {
            authState = .unauthenticated
            return
        }

        do {
            // Check if user is already logged in
            let isAuthenticated = await authService.isAuthenticated()

            if isAuthenticated {
                // Get user information
                guard let userId = await authService.getCurrentUserId(),
                      let accessToken = await authService.getAccessToken() else {
                    await logout()
                    return
                }

                currentUserId = userId
                currentAccessToken = accessToken

                // Create user response
                // TODO: Fetch full user profile from server
                let user = UserResponse(
                    id: userId,
                    email: "user@example.com",
                    name: "User",
                    avatarUrl: nil,
                    provider: "apple",
                    createdAt: ISO8601DateFormatter().string(from: Date())
                )

                authState = .authenticated(userId: userId, user: user)

                // Start token refresh monitoring
                startTokenRefreshMonitoring()
            } else {
                authState = .unauthenticated
            }
        } catch {
            authState = .error(message: "Failed to initialize authentication: \(error.localizedDescription)")
        }
    }

    /**
     * Login with OAuth provider.
     */
    func login(provider: String, authCode: String, userInfo: String? = nil) async {
        authState = .loading

        do {
            let response: OAuthLoginResponse

            if provider == "apple" {
                response = try await authService.loginWithApple(
                    authorizationCode: authCode,
                    userInfo: userInfo
                )
            } else {
                response = try await authService.loginWithGoogle(
                    authorizationCode: authCode
                )
            }

            currentUserId = response.user.id
            currentAccessToken = response.accessToken
            authState = .authenticated(userId: response.user.id, user: response.user)

            // Start token refresh monitoring
            startTokenRefreshMonitoring()

        } catch {
            authState = .error(message: "Login failed: \(error.localizedDescription)")
        }
    }

    /**
     * Logout the current user.
     */
    func logout() async {
        // Stop token refresh monitoring
        tokenRefreshTask?.cancel()
        tokenRefreshTask = nil

        // Clear tokens
        try? await authService.logout()

        // Update state
        currentUserId = nil
        currentAccessToken = nil
        authState = .unauthenticated
    }

    /**
     * Refresh access token if needed.
     */
    func refreshTokenIfNeeded() async {
        do {
            let response = try await authService.refreshToken()
            currentAccessToken = response.accessToken

            // Update authenticated state with new token
            if case .authenticated(let userId, _) = authState {
                authState = .authenticated(userId: userId, user: response.user)
            }
        } catch {
            // Refresh failed - logout user
            await logout()
        }
    }

    /**
     * Start background token refresh monitoring.
     *
     * Checks token expiry periodically and refreshes proactively.
     */
    private func startTokenRefreshMonitoring() {
        tokenRefreshTask?.cancel()
        tokenRefreshTask = Task {
            while !Task.isCancelled {
                try? await Task.sleep(nanoseconds: 5 * 60 * 1_000_000_000) // 5 minutes

                if case .authenticated = authState {
                    await refreshTokenIfNeeded()
                }
            }
        }
    }

    #if DEBUG
    /**
     * Set authentication state for development mode.
     * Bypasses normal OAuth flow.
     */
    func setAuthStateForDevelopment(userId: String, accessToken: String) {
        currentUserId = userId
        currentAccessToken = accessToken

        // Create mock user
        let mockUser = UserResponse(
            id: userId,
            email: "dev@wakeve.local",
            name: "Dev User",
            avatarUrl: nil,
            provider: "development",
            createdAt: ISO8601DateFormatter().string(from: Date())
        )

        authState = .authenticated(userId: userId, user: mockUser)

        print("✅ Development mode: Authenticated as \(userId)")
    }
    #endif

    /**
     * Clean up resources.
     */
    func dispose() {
        tokenRefreshTask?.cancel()
        tokenRefreshTask = nil
    }
}
