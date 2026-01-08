package com.guyghost.wakeve.auth.shell.services

import com.guyghost.wakeve.auth.core.models.AuthMethod
import com.guyghost.wakeve.auth.core.models.AuthResult
import com.guyghost.wakeve.auth.core.models.User

/**
 * Service interface for authentication operations.
 * This is the expect class that will be implemented for each platform.
 * 
 * The AuthService handles:
 * - OAuth authentication (Google, Apple)
 * - Token validation and refresh
 * - Session management
 * - Sign out
 * 
 * @platform Android: Uses Google Sign-In SDK and Sign in with Apple
 * @platform iOS: Uses AuthenticationServices framework and Google Sign-In iOS SDK
 */
expect class AuthService {

    /**
     * Initiates Google Sign-In flow.
     * 
     * @return AuthResult containing the authenticated user or an error
     */
    suspend fun signInWithGoogle(): AuthResult

    /**
     * Initiates Apple Sign-In flow.
     * 
     * @return AuthResult containing the authenticated user or an error
     */
    suspend fun signInWithApple(): AuthResult

    /**
     * Signs out the current user and clears tokens.
     */
    suspend fun signOut()

    /**
     * Checks if the user is currently authenticated.
     * 
     * @return true if there is a valid session, false otherwise
     */
    suspend fun isAuthenticated(): Boolean

    /**
     * Gets the current authenticated user.
     * 
     * @return User if authenticated, null otherwise
     */
    suspend fun getCurrentUser(): User?

    /**
     * Refreshes the authentication token.
     * 
     * @return AuthResult with new token or error
     */
    suspend fun refreshToken(): AuthResult

    /**
     * Checks if a specific OAuth provider is available.
     * 
     * @param provider The authentication method to check
     * @return true if the provider is configured and available
     */
    suspend fun isProviderAvailable(provider: AuthMethod): Boolean
}
