package com.guyghost.wakeve.auth.core.models

/**
 * Represents authentication errors that can occur during the auth flow.
 * This is a sealed class with specific error types for different failure scenarios.
 */
sealed class AuthError {
    /**
     * Network-related error (no internet, server unavailable, etc.)
     */
    data object NetworkError : AuthError()

    /**
     * Invalid credentials provided (wrong password, expired token, etc.)
     */
    data object InvalidCredentials : AuthError()

    /**
     * Invalid OTP code entered.
     * 
     * @property attemptsRemaining Number of attempts remaining before lockout
     */
    data class InvalidOTP(
        val attemptsRemaining: Int = 0
    ) : AuthError()

    /**
     * OTP has expired (typically after 5 minutes).
     */
    data object OTPExpired : AuthError()

    /**
     * Email address is not registered.
     */
    data object EmailNotRegistered : AuthError()

    /**
     * Email address is already registered.
     */
    data object EmailAlreadyRegistered : AuthError()

    /**
     * OAuth provider error (Google, Apple sign-in failed).
     * 
     * @property provider The OAuth provider that failed
     * @property message The error message from the provider
     */
    data class OAuthError(
        val provider: AuthMethod,
        val message: String? = null
    ) : AuthError()

    /**
     * User cancelled the OAuth flow.
     */
    data class OAuthCancelled(
        val provider: AuthMethod
    ) : AuthError()

    /**
     * Token storage error (Keychain/Keystore access failed).
     */
    data object TokenStorageError : AuthError()

    /**
     * User account is locked or disabled.
     */
    data object AccountLocked : AuthError()

    /**
     * Required permission not granted by user.
     * 
     * @property permission The permission that was not granted
     */
    data class PermissionDenied(
        val permission: String
    ) : AuthError()

    /**
     * Validation error for user input.
     * 
     * @property field The field that failed validation
     * @property message The validation error message
     */
    data class ValidationError(
        val field: String,
        val message: String
    ) : AuthError()

    /**
     * Unknown error occurred.
     * 
     * @property message Optional error message
     */
    data class UnknownError(
        val message: String? = null
    ) : AuthError()

    /**
     * Returns a human-readable error message.
     */
    val userMessage: String
        get() = when (this) {
            is NetworkError -> "Problème de connexion. Vérifiez votre connexion internet."
            is InvalidCredentials -> "Identifiants incorrects. Veuillez réessayer."
            is InvalidOTP -> if (attemptsRemaining > 0) {
                "Code invalide. $attemptsRemaining tentatives restantes."
            } else {
                "Code invalide. Veuillez demander un nouveau code."
            }
            is OTPExpired -> "Le code a expiré. Veuillez demander un nouveau code."
            is EmailNotRegistered -> "Cette adresse email n'est pas enregistrée."
            is EmailAlreadyRegistered -> "Cette adresse email est déjà utilisée."
            is OAuthError -> message ?: "Erreur de connexion avec ${provider.displayName}"
            is OAuthCancelled -> "Connexion annulée"
            is TokenStorageError -> "Erreur de stockage sécurisé. Veuillez réessayer."
            is AccountLocked -> "Compte verrouillé. Veuillez contacter le support."
            is PermissionDenied -> "Permission requise: $permission"
            is ValidationError -> message
            is UnknownError -> message ?: "Une erreur est survenue"
        }

    /**
     * Returns true if this error should trigger a retry prompt.
     */
    val isRetryable: Boolean
        get() = when (this) {
            is NetworkError -> true
            is InvalidCredentials -> true
            is InvalidOTP -> attemptsRemaining > 0
            is OAuthCancelled -> false
            is OAuthError -> true
            else -> true
        }

    /**
     * Returns true if this error requires switching authentication methods.
     */
    val requiresMethodChange: Boolean
        get() = this is OAuthCancelled || this is OAuthError
}
