//
//  AuthenticationService.swift
//  iosApp
//
//  Created by Wakev on 2025
//

import Foundation

/**
 * iOS-specific authentication service
 */
class AuthenticationService: ClientAuthenticationServiceProtocol {
    private let secureStorage: SecureTokenStorageProtocol
    private let baseUrl: String
    private let httpClient: URLSession

    init(
        secureStorage: SecureTokenStorageProtocol = SecureTokenStorage(),
        baseUrl: String = "http://localhost:8080"
    ) {
        self.secureStorage = secureStorage
        self.baseUrl = baseUrl
        self.httpClient = URLSession.shared
    }

    func loginWithGoogle(authorizationCode: String) async throws -> OAuthLoginResponse {
        let request = OAuthLoginRequest(
            provider: "google",
            authorizationCode: authorizationCode
        )

        let response = try await performLoginRequest(request)

        // Store tokens securely
        try await secureStorage.storeAccessToken(response.accessToken)
        if let refreshToken = response.refreshToken {
            try await secureStorage.storeRefreshToken(refreshToken)
        }
        try await secureStorage.storeUserId(response.user.id)

        // Calculate and store expiry
        let expiryTimestamp = Int64(Date().timeIntervalSince1970 * 1000) + Int64(response.expiresIn * 1000)
        try await secureStorage.storeTokenExpiry(expiryTimestamp)

        return response
    }

    func loginWithApple(authorizationCode: String, userInfo: String? = nil) async throws -> OAuthLoginResponse {
        let request = OAuthLoginRequest(
            provider: "apple",
            authorizationCode: authorizationCode,
            accessToken: userInfo
        )

        let response = try await performLoginRequest(request)

        // Store tokens securely
        try await secureStorage.storeAccessToken(response.accessToken)
        if let refreshToken = response.refreshToken {
            try await secureStorage.storeRefreshToken(refreshToken)
        }
        try await secureStorage.storeUserId(response.user.id)

        // Calculate and store expiry
        let expiryTimestamp = Int64(Date().timeIntervalSince1970 * 1000) + Int64(response.expiresIn * 1000)
        try await secureStorage.storeTokenExpiry(expiryTimestamp)

        return response
    }

    func refreshToken() async throws -> OAuthLoginResponse {
        guard let refreshToken = await secureStorage.getRefreshToken() else {
            throw AuthenticationError.noRefreshToken
        }

        let url = URL(string: "\(baseUrl)/auth/refresh")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        let body = ["refresh_token": refreshToken]
        request.httpBody = try JSONSerialization.data(withJSONObject: body)

        let (data, response) = try await httpClient.data(for: request)

        guard let httpResponse = response as? HTTPURLResponse,
              httpResponse.statusCode == 200 else {
            throw AuthenticationError.refreshFailed
        }

        let newResponse = try JSONDecoder().decode(OAuthLoginResponse.self, from: data)

        // Update stored tokens
        try await secureStorage.storeAccessToken(newResponse.accessToken)
        if let refreshToken = newResponse.refreshToken {
            try await secureStorage.storeRefreshToken(refreshToken)
        }

        // Calculate and store new expiry
        let expiryTimestamp = Int64(Date().timeIntervalSince1970 * 1000) + Int64(newResponse.expiresIn * 1000)
        try await secureStorage.storeTokenExpiry(expiryTimestamp)

        return newResponse
    }

    func getAccessToken() async -> String? {
        if await secureStorage.hasValidToken() {
            return await secureStorage.getAccessToken()
        } else {
            // Try to refresh token
            return try? await refreshToken().accessToken
        }
    }

    func getCurrentUserId() async -> String? {
        return await secureStorage.getUserId()
    }

    func isAuthenticated() async -> Bool {
        return await secureStorage.hasValidToken()
    }

    func logout() async throws {
        try await secureStorage.clearAllTokens()
    }

    func getGoogleAuthorizationUrl(state: String = Self.generateState()) async throws -> String {
        let url = URL(string: "\(baseUrl)/auth/google/url?state=\(state)")!
        let (data, response) = try await httpClient.data(from: url)

        guard let httpResponse = response as? HTTPURLResponse,
              httpResponse.statusCode == 200 else {
            throw AuthenticationError.urlGenerationFailed
        }

        let urlResponse = try JSONDecoder().decode([String: String].self, from: data)
        guard let authUrl = urlResponse["url"] else {
            throw AuthenticationError.invalidResponse
        }

        return authUrl
    }

    func getAppleAuthorizationUrl(state: String = Self.generateState()) async throws -> String {
        let url = URL(string: "\(baseUrl)/auth/apple/url?state=\(state)")!
        let (data, response) = try await httpClient.data(from: url)

        guard let httpResponse = response as? HTTPURLResponse,
              httpResponse.statusCode == 200 else {
            throw AuthenticationError.urlGenerationFailed
        }

        let urlResponse = try JSONDecoder().decode([String: String].self, from: data)
        guard let authUrl = urlResponse["url"] else {
            throw AuthenticationError.invalidResponse
        }

        return authUrl
    }

    private func performLoginRequest(_ request: OAuthLoginRequest) async throws -> OAuthLoginResponse {
        let endpoint = request.provider == "google" ? "/auth/google" : "/auth/apple"
        let url = URL(string: "\(baseUrl)\(endpoint)")!

        var urlRequest = URLRequest(url: url)
        urlRequest.httpMethod = "POST"
        urlRequest.setValue("application/json", forHTTPHeaderField: "Content-Type")

        urlRequest.httpBody = try JSONEncoder().encode(request)

        let (data, response) = try await httpClient.data(for: urlRequest)

        guard let httpResponse = response as? HTTPURLResponse,
              httpResponse.statusCode == 200 else {
            throw AuthenticationError.loginFailed
        }

        return try JSONDecoder().decode(OAuthLoginResponse.self, from: data)
    }

    private static func generateState() -> String {
        let characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return String((0..<32).map { _ in characters.randomElement()! })
    }
}

// MARK: - Supporting Types

protocol ClientAuthenticationServiceProtocol {
    func loginWithGoogle(authorizationCode: String) async throws -> OAuthLoginResponse
    func loginWithApple(authorizationCode: String, userInfo: String?) async throws -> OAuthLoginResponse
    func refreshToken() async throws -> OAuthLoginResponse
    func getAccessToken() async -> String?
    func getCurrentUserId() async -> String?
    func isAuthenticated() async -> Bool
    func logout() async throws
    func getGoogleAuthorizationUrl(state: String) async throws -> String
    func getAppleAuthorizationUrl(state: String) async throws -> String
}

enum AuthenticationError: Error {
    case noRefreshToken
    case refreshFailed
    case loginFailed
    case urlGenerationFailed
    case invalidResponse
}

// MARK: - Data Models (matching Kotlin models)

struct OAuthLoginRequest: Codable {
    let provider: String
    let authorizationCode: String?
    let accessToken: String?

    enum CodingKeys: String, CodingKey {
        case provider
        case authorizationCode = "authorizationCode"
        case accessToken = "accessToken"
    }
}

struct OAuthLoginResponse: Codable {
    let user: UserResponse
    let accessToken: String
    let refreshToken: String?
    let tokenType: String
    let expiresIn: Int64
    let scope: String?
}

struct UserResponse: Codable {
    let id: String
    let email: String
    let name: String
    let avatarUrl: String?
    let provider: String
    let createdAt: String
}