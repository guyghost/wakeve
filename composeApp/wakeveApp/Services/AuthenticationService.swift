//
//  AuthenticationService.swift
//  iosApp
//
//  Created by Wakev on 2025
//

import Foundation
import SwiftUI

/**
 * Stub authentication service for Phase 3 development
 *
 * This is a placeholder implementation that will be replaced with real OAuth
 * authentication in Phase 3. Currently returns mock data for development.
 */
@MainActor
public class AuthenticationService: ObservableObject {
    
    /// Authenticate user (stub implementation)
    /// - Returns: Mock authentication token
    func authenticate() async throws -> AuthToken {
        // Simulate network delay
        try await Task.sleep(nanoseconds: 1_000_000_000) // 1 second
        
        // Return mock token
        return AuthToken(
            accessToken: "mock_access_token_\(UUID().uuidString)",
            refreshToken: "mock_refresh_token_\(UUID().uuidString)",
            expiresIn: 3600,
            tokenType: "Bearer"
        )
    }
    
    /// Refresh authentication token (stub implementation)
    /// - Returns: New mock authentication token
    func refreshToken() async throws {
        // Simulate network delay
        try await Task.sleep(nanoseconds: 500_000_000) // 0.5 seconds
        
        // In stub implementation, do nothing - token refresh not needed
    }
    
    /// Check if user is authenticated (stub implementation)
    /// - Returns: Always true for development
    func isAuthenticated() -> Bool {
        // In stub mode, always return true for development
        return true
    }
    
    /// Get current user (stub implementation)
    /// - Returns: Mock user data
    func getCurrentUser() -> User? {
        return User(
            id: "user_123",
            name: "John Doe",
            email: "john.doe@example.com",
            avatarUrl: nil
        )
    }
}

// MARK: - Supporting Types

/// Authentication token structure
public struct AuthToken: Codable {
    let accessToken: String
    let refreshToken: String
    let expiresIn: Int
    let tokenType: String
}

/// Authentication errors
public enum AuthError: Error {
    case authenticationFailed
    case tokenExpired
    case networkError
}