package com.guyghost.wakeve.auth.core.models

import com.guyghost.wakeve.auth.core.models.AuthError.*
import com.guyghost.wakeve.auth.core.models.AuthResult.*

/**
 * Represents the result of an authentication attempt.
 * This is a sealed class that can be one of:
 * - Success: Authentication succeeded with user data
 * - Guest: Guest mode was selected
 * - Error: Authentication failed with specific error
 */
sealed class AuthResult {
    /**
     * Authentication succeeded with user data.
     * 
     * @property user The authenticated user
     * @property token The authentication token (JWT or session token)
     */
    data class Success(
        val user: User,
        val token: AuthToken
    ) : AuthResult()

    /**
     * Guest mode was selected by the user.
     * 
     * @property guestUser The guest user created for this session
     */
    data class Guest(
        val guestUser: User
    ) : AuthResult()

    /**
     * Authentication failed with a specific error.
     * 
     * @property error The specific authentication error that occurred
     */
    data class Error(
        val error: AuthError
    ) : AuthResult()

    companion object {
        /**
         * Creates a Success result from user and token.
         * 
         * @param user The authenticated user
         * @param token The authentication token
         * @return A Success AuthResult
         */
        fun success(user: User, token: AuthToken): AuthResult = Success(user, token)

        /**
         * Creates a Guest result from guest user.
         * 
         * @param guestUser The guest user created
         * @return A Guest AuthResult
         */
        fun guest(guestUser: User): AuthResult = Guest(guestUser)

        /**
         * Creates an Error result from AuthError.
         * 
         * @param error The authentication error
         * @return An Error AuthResult
         */
        fun error(error: AuthError): AuthResult = Error(error)

        /**
         * Creates an Error result from a network error.
         * 
         * @return An Error AuthResult with NetworkError
         */
        fun networkError(): AuthResult = Error(NetworkError)

        /**
         * Creates an Error result from invalid credentials.
         * 
         * @return An Error AuthResult with InvalidCredentials
         */
        fun invalidCredentials(): AuthResult = Error(InvalidCredentials)

        /**
         * Creates an Error result from invalid OTP.
         * 
         * @param attemptsRemaining Number of attempts remaining before lockout
         * @return An Error AuthResult with InvalidOTP
         */
        fun invalidOTP(attemptsRemaining: Int = 0): AuthResult = Error(InvalidOTP(attemptsRemaining))
    }

    /**
     * Returns true if this is a successful authentication.
     */
    val isSuccess: Boolean
        get() = this is Success

    /**
     * Returns true if this is a guest session.
     */
    val isGuest: Boolean
        get() = this is Guest

    /**
     * Returns true if this is an error.
     */
    val isError: Boolean
        get() = this is Error

    /**
     * Returns the user if this is a Success or Guest result.
     */
    val userOrNull: User?
        get() = when (this) {
            is Success -> user
            is Guest -> guestUser
            is Error -> null
        }

    /**
     * Returns the error if this is an Error result.
     */
    val errorOrNull: AuthError?
        get() = when (this) {
            is Error -> error
            else -> null
        }
}
