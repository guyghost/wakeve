//
//  AuthenticationService.swift
//  iosApp
//
//  Created by Wakev on 2025
//

import Foundation
import SwiftUI

/**
 * Authentication service for iOS OAuth integration.
 *
 * Handles communication with the Wakeve backend for:
 * - Apple Sign-In (POST /api/auth/apple)
 * - Google Sign-In (POST /api/auth/google)
 * - Token refresh (POST /api/auth/refresh)
 * - Session management and token storage
 */
@MainActor
public class AuthenticationService: ObservableObject {

    // MARK: - Configuration

    /// Base URL for the Wakeve API server
    /// TODO: Move to build config or environment variable
    private let baseUrl: String = {
        #if DEBUG
        return "http://localhost:8080/api"
        #else
        return "https://api.wakeve.app/api"
        #endif
    }()

    /// Secure token storage (Keychain-backed)
    private let tokenStorage = SecureTokenStorage()

    /// URLSession for network requests
    private let session: URLSession = {
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 30
        config.timeoutIntervalForResource = 60
        return URLSession(configuration: config)
    }()

    // MARK: - Apple Sign-In

    /**
     * Login with Apple Sign-In credentials.
     *
     * Sends the identity token and user info to the backend for verification
     * and account creation/login.
     *
     * - Parameters:
     *   - authCode: The authorization code from Apple Sign-In
     *   - idToken: The identity token (JWT) from Apple Sign-In
     *   - email: The user's email (only provided on first sign-in)
     *   - fullName: The user's full name (only provided on first sign-in)
     * - Returns: AuthLoginResponse with user data and tokens
     */
    func loginWithApple(
        authCode: String,
        idToken: String?,
        email: String? = nil,
        fullName: String? = nil
    ) async throws -> AuthLoginResponse {
        guard let idToken = idToken, !idToken.isEmpty else {
            throw AuthError.invalidCredentials
        }

        let requestBody = AppleAuthRequestBody(
            idToken: idToken,
            email: email,
            name: fullName
        )

        let url = URL(string: "\(baseUrl)/auth/apple")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONEncoder().encode(requestBody)

        let (data, response) = try await session.data(for: request)

        guard let httpResponse = response as? HTTPURLResponse else {
            throw AuthError.networkError
        }

        switch httpResponse.statusCode {
        case 200:
            let loginResponse = try JSONDecoder().decode(AuthLoginResponse.self, from: data)

            // Store tokens securely
            try await storeAuthTokens(loginResponse)

            return loginResponse

        case 400:
            let errorResponse = try? JSONDecoder().decode(AuthErrorResponse.self, from: data)
            throw AuthError.serverError(errorResponse?.message ?? "Invalid request")

        case 401:
            let errorResponse = try? JSONDecoder().decode(AuthErrorResponse.self, from: data)
            throw AuthError.authenticationFailed(errorResponse?.message ?? "Authentication failed")

        default:
            throw AuthError.serverError("Server error: \(httpResponse.statusCode)")
        }
    }

    // MARK: - Google Sign-In

    /**
     * Login with Google Sign-In credentials.
     *
     * Sends the Google ID token to the backend for verification
     * and account creation/login.
     *
     * - Parameters:
     *   - idToken: The Google ID token
     *   - email: The user's email from Google
     *   - name: The user's display name from Google
     * - Returns: AuthLoginResponse with user data and tokens
     */
    func loginWithGoogle(
        idToken: String,
        email: String,
        name: String? = nil
    ) async throws -> AuthLoginResponse {
        let requestBody = GoogleAuthRequestBody(
            idToken: idToken,
            email: email,
            name: name
        )

        let url = URL(string: "\(baseUrl)/auth/google")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONEncoder().encode(requestBody)

        let (data, response) = try await session.data(for: request)

        guard let httpResponse = response as? HTTPURLResponse else {
            throw AuthError.networkError
        }

        switch httpResponse.statusCode {
        case 200:
            let loginResponse = try JSONDecoder().decode(AuthLoginResponse.self, from: data)

            // Store tokens securely
            try await storeAuthTokens(loginResponse)

            return loginResponse

        case 400:
            let errorResponse = try? JSONDecoder().decode(AuthErrorResponse.self, from: data)
            throw AuthError.serverError(errorResponse?.message ?? "Invalid request")

        case 401:
            let errorResponse = try? JSONDecoder().decode(AuthErrorResponse.self, from: data)
            throw AuthError.authenticationFailed(errorResponse?.message ?? "Authentication failed")

        default:
            throw AuthError.serverError("Server error: \(httpResponse.statusCode)")
        }
    }

    // MARK: - Token Refresh

    /**
     * Refresh the access token using the stored refresh token.
     *
     * - Returns: Updated AuthLoginResponse with new tokens
     */
    func refreshToken() async throws -> AuthLoginResponse {
        guard let refreshToken = await tokenStorage.getRefreshToken(), !refreshToken.isEmpty else {
            throw AuthError.noRefreshToken
        }

        let requestBody = TokenRefreshRequestBody(refreshToken: refreshToken)

        let url = URL(string: "\(baseUrl)/auth/refresh")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONEncoder().encode(requestBody)

        let (data, response) = try await session.data(for: request)

        guard let httpResponse = response as? HTTPURLResponse else {
            throw AuthError.networkError
        }

        switch httpResponse.statusCode {
        case 200:
            let loginResponse = try JSONDecoder().decode(AuthLoginResponse.self, from: data)

            // Update stored tokens
            try await storeAuthTokens(loginResponse)

            return loginResponse

        case 401:
            // Refresh token is invalid/expired - user needs to re-login
            throw AuthError.tokenExpired

        default:
            throw AuthError.serverError("Token refresh failed: \(httpResponse.statusCode)")
        }
    }

    // MARK: - Session Management

    /**
     * Check if user has a valid (non-expired) token.
     *
     * - Returns: true if a valid access token exists
     */
    func isAuthenticated() async -> Bool {
        return await tokenStorage.hasValidToken()
    }

    /**
     * Get the current user from stored profile data.
     *
     * - Returns: User if profile data is stored, nil otherwise
     */
    func getCurrentUser() async -> User? {
        guard let userId = await tokenStorage.getUserId() else { return nil }

        // Read stored profile data from keychain
        let name = UserDefaults.standard.string(forKey: "wakeve_user_name")
        let email = UserDefaults.standard.string(forKey: "wakeve_user_email")
        let avatarUrl = UserDefaults.standard.string(forKey: "wakeve_user_avatar")

        return User(
            id: userId,
            name: name ?? "User",
            email: email ?? "",
            avatarUrl: avatarUrl
        )
    }

    /**
     * Sign out and clear all stored tokens and user data.
     */
    func signOut() async {
        try? await tokenStorage.clearAllTokens()

        // Clear cached user profile
        UserDefaults.standard.removeObject(forKey: "wakeve_user_name")
        UserDefaults.standard.removeObject(forKey: "wakeve_user_email")
        UserDefaults.standard.removeObject(forKey: "wakeve_user_avatar")
        UserDefaults.standard.removeObject(forKey: "wakeve_user_provider")

        print("[AuthenticationService] User signed out, tokens cleared")
    }

    /**
     * Get the stored access token for API calls.
     *
     * - Returns: The access token string, or nil if not available
     */
    func getAccessToken() async -> String? {
        return await tokenStorage.getAccessToken()
    }

    // MARK: - Private Helpers

    /**
     * Store authentication tokens and user profile from login response.
     */
    private func storeAuthTokens(_ response: AuthLoginResponse) async throws {
        try await tokenStorage.storeAccessToken(response.accessToken)

        if let refreshToken = response.refreshToken {
            try await tokenStorage.storeRefreshToken(refreshToken)
        }

        // Store token expiry
        let expiryTimestamp = Int64(Date().timeIntervalSince1970 * 1000) + (response.expiresIn * 1000)
        try await tokenStorage.storeTokenExpiry(expiryTimestamp)

        // Store user info
        try await tokenStorage.storeUserId(response.user.id)

        // Cache user profile in UserDefaults for quick access
        UserDefaults.standard.set(response.user.name, forKey: "wakeve_user_name")
        UserDefaults.standard.set(response.user.email, forKey: "wakeve_user_email")
        UserDefaults.standard.set(response.user.avatarUrl, forKey: "wakeve_user_avatar")
        UserDefaults.standard.set(response.user.provider, forKey: "wakeve_user_provider")

        print("[AuthenticationService] Tokens stored for user: \(response.user.id)")
    }
}

// MARK: - API Request/Response Types

/// Apple authentication request body (matches server AppleAuthRequest)
private struct AppleAuthRequestBody: Codable {
    let idToken: String
    let email: String?
    let name: String?
}

/// Google authentication request body (matches server GoogleAuthRequest)
private struct GoogleAuthRequestBody: Codable {
    let idToken: String
    let email: String
    let name: String?
}

/// Token refresh request body (matches server TokenRefreshRequest)
private struct TokenRefreshRequestBody: Codable {
    let refreshToken: String
}

/// Authentication login response (matches server OAuthLoginResponse)
public struct AuthLoginResponse: Codable {
    let user: AuthUserResponse
    let accessToken: String
    let refreshToken: String?
    let tokenType: String
    let expiresIn: Int64
    let scope: String?

    enum CodingKeys: String, CodingKey {
        case user, accessToken, refreshToken, tokenType, expiresIn, scope
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        user = try container.decode(AuthUserResponse.self, forKey: .user)
        accessToken = try container.decode(String.self, forKey: .accessToken)
        refreshToken = try container.decodeIfPresent(String.self, forKey: .refreshToken)
        tokenType = try container.decodeIfPresent(String.self, forKey: .tokenType) ?? "Bearer"
        expiresIn = try container.decode(Int64.self, forKey: .expiresIn)
        scope = try container.decodeIfPresent(String.self, forKey: .scope)
    }
}

/// User data from auth response (matches server UserResponse)
public struct AuthUserResponse: Codable {
    let id: String
    let email: String
    let name: String
    let avatarUrl: String?
    let provider: String
    let role: String?
    let createdAt: String
}

/// Error response from auth endpoints
private struct AuthErrorResponse: Codable {
    let error: String
    let message: String
    let details: String?
}

// MARK: - Supporting Types

/// Authentication token (kept for compatibility)
public struct AuthToken: Codable {
    let accessToken: String
    let refreshToken: String
    let expiresIn: Int
    let tokenType: String
}

/// Authentication errors
public enum AuthError: LocalizedError {
    case authenticationFailed(String? = nil)
    case tokenExpired
    case networkError
    case serverError(String)
    case noRefreshToken
    case invalidCredentials
    case userCancelled

    public var errorDescription: String? {
        switch self {
        case .authenticationFailed(let message):
            return message ?? "Authentication failed"
        case .tokenExpired:
            return "Session expired. Please sign in again."
        case .networkError:
            return "Network error. Please check your connection."
        case .serverError(let message):
            return message
        case .noRefreshToken:
            return "No refresh token available"
        case .invalidCredentials:
            return "Invalid credentials"
        case .userCancelled:
            return "Sign-in was cancelled"
        }
    }
}