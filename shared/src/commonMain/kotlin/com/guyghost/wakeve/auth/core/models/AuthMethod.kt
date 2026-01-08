package com.guyghost.wakeve.auth.core.models

/**
 * Enumeration of available authentication methods in Wakeve.
 * 
 * Each auth method represents a different way users can authenticate:
 * - GOOGLE: OAuth 2.0 via Google Sign-In
 * - APPLE: Sign in with Apple (OAuth 2.0/Sign in with Apple JS)
 * - EMAIL: Email-based authentication using OTP
 * - GUEST: No authentication, local-only session
 */
enum class AuthMethod {
    /** OAuth 2.0 authentication via Google Sign-In */
    GOOGLE,
    
    /** Sign in with Apple via AuthenticationServices framework */
    APPLE,
    
    /** Email-based authentication with One-Time Password */
    EMAIL,
    
    /** Guest mode - no authentication, local-only session */
    GUEST;

    /**
     * Returns a human-readable name for the authentication method.
     */
    val displayName: String
        get() = when (this) {
            GOOGLE -> "Google"
            APPLE -> "Apple"
            EMAIL -> "Email"
            GUEST -> "Invité"
        }

    /**
     * Returns true if this is an OAuth-based authentication method.
     */
    val isOAuth: Boolean
        get() = this == GOOGLE || this == APPLE

    /**
     * Returns true if this authentication method requires network access.
     */
    val requiresNetwork: Boolean
        get() = this != GUEST

    /**
     * Returns true if this method requires email verification.
     */
    val requiresEmailVerification: Boolean
        get() = this == EMAIL
}
