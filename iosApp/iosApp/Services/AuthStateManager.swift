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
    
    private let authService: AuthenticationService
    private let enableOAuth: Bool
    
    init(authService: AuthenticationService, enableOAuth: Bool = false) {
        self.authService = authService
        self.enableOAuth = enableOAuth
    }
    
    /// Sign in the user
    func signIn(provider: String? = nil, authCode: String? = nil, userInfo: String? = nil) async {
        do {
            // Perform authentication
            let _ = try await authService.authenticate()
            
            // Update state
            await MainActor.run {
                self.isAuthenticated = true
                self.currentUser = authService.getCurrentUser()
            }
        } catch {
            print("Authentication failed: \(error)")
            await MainActor.run {
                self.isAuthenticated = false
                self.currentUser = nil
            }
        }
    }
    
    /// Sign out the current user
    func signOut() {
        // Clear authentication state
        isAuthenticated = false
        currentUser = nil
        
        print("User signed out")
    }
    
    /// Check authentication status
    func checkAuthStatus() {
        // In stub mode, check if user was previously authenticated
        if let user = currentUser {
            isAuthenticated = true
        } else {
            isAuthenticated = false
        }
    }
    
    /// Refresh authentication token if needed
    func refreshTokenIfNeeded() async {
        guard isAuthenticated else { return }
        
        do {
            try await authService.refreshToken()
            // In stub implementation, no state update needed
            print("Token refreshed successfully")
        } catch {
            print("Token refresh failed: \(error)")
            // In stub implementation, don't sign out on refresh failure
        }
    }
    
    /// Set authentication state for development (bypasses normal auth flow)
    func setAuthStateForDevelopment(userId: String, accessToken: String) async {
        await MainActor.run {
            self.isAuthenticated = true
            self.currentUser = User(
                id: userId,
                name: "Dev User",
                email: "dev@example.com",
                avatarUrl: nil
            )
        }
    }
}