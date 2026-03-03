//
//  PreviewEnvironment.swift
//  iosApp
//
//  Mock authentication manager and preview environment modifier for SwiftUI previews.
//

import SwiftUI

#if DEBUG

// MARK: - MockAuthStateManager

/// A mock subclass of AuthStateManager for SwiftUI previews.
///
/// Provides deterministic, offline authentication state without network calls.
/// All async methods are no-ops; published properties are set directly in init.
@MainActor
final class MockAuthStateManager: AuthStateManager {

    /// Create a mock auth state manager with the given user and authentication status.
    ///
    /// - Parameters:
    ///   - user: The mock user to expose as `currentUser`. Defaults to `UserFactory.organizer`.
    ///   - isAuthenticated: Whether the mock session is authenticated. Defaults to `true`.
    init(user: User = UserFactory.organizer, isAuthenticated: Bool = true) {
        // AuthenticationService is required by the superclass but will never be
        // used because every method is overridden below.
        super.init(authService: AuthenticationService(), enableOAuth: false)

        self.isAuthenticated = isAuthenticated
        self.currentUser = isAuthenticated ? user : nil
        self.isLoading = false
        self.authError = nil
    }

    // MARK: - Overrides (all no-ops except signOut)

    override func signIn(
        provider: String?,
        authCode: String?,
        userInfo: String?,
        email: String?,
        fullName: String?
    ) async {
        // No-op in previews
    }

    override func signOut() {
        isAuthenticated = false
        currentUser = nil
        authError = nil
    }

    override func checkAuthStatus() {
        // No-op in previews
    }

    override func refreshTokenIfNeeded() async {
        // No-op in previews
    }

    override func setAuthStateForDevelopment(userId: String, accessToken: String) async {
        // No-op in previews
    }
}

// MARK: - PreviewEnvironment ViewModifier

/// Injects all required `@EnvironmentObject` types for SwiftUI previews.
///
/// Usage:
/// ```swift
/// MyView()
///     .previewEnvironment()
/// ```
struct PreviewEnvironment: ViewModifier {
    let user: User
    let isAuthenticated: Bool

    func body(content: Content) -> some View {
        let authService = AuthenticationService()
        let mockAuthManager = MockAuthStateManager(user: user, isAuthenticated: isAuthenticated)
        let deepLinkService = DeepLinkService()

        content
            .environmentObject(mockAuthManager as AuthStateManager)
            .environmentObject(authService)
            .environmentObject(deepLinkService)
    }
}

// MARK: - View Extension

extension View {

    /// Wrap a view with all the `@EnvironmentObject` types needed by Wakeve views.
    ///
    /// - Parameters:
    ///   - user: The mock user exposed to previews. Defaults to `UserFactory.organizer`.
    ///   - isAuthenticated: Whether the mock session is authenticated. Defaults to `true`.
    func previewEnvironment(
        user: User = UserFactory.organizer,
        isAuthenticated: Bool = true
    ) -> some View {
        self.modifier(PreviewEnvironment(user: user, isAuthenticated: isAuthenticated))
    }
}

#endif
