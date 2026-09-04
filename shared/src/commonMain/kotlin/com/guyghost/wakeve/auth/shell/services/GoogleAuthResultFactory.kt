package com.guyghost.wakeve.auth.shell.services

import com.guyghost.wakeve.auth.core.models.AuthError
import com.guyghost.wakeve.auth.core.models.AuthMethod
import com.guyghost.wakeve.auth.core.models.AuthResult
import com.guyghost.wakeve.auth.core.models.AuthToken
import com.guyghost.wakeve.auth.core.models.TokenType
import com.guyghost.wakeve.auth.core.models.User

/**
 * Pure factory that maps a Google profile (as returned by GoogleSignIn legacy
 * or Credential Manager) into an [AuthResult].
 *
 * Kept free of any Android/SDK imports so it is unit-testable on the JVM
 * and shared by both sign-in implementations (DAO #21).
 */
object GoogleAuthResultFactory {

    /**
     * Builds an [AuthResult] from a Google account profile.
     *
     * @param id Google account ID (null when the SDK did not provide one)
     * @param email Account email (null for some restricted profiles)
     * @param displayName Display name (may be null)
     * @param idToken Google ID token (null when the flow did not return one)
     * @param currentTimeMillis Injected current time for testability
     * @return [AuthResult.Success] with user data and long-lived ID token,
     *         or [AuthResult.Error] with an [AuthError.OAuthError] when a
     *         required field is missing.
     */
    fun createFromProfile(
        id: String?,
        email: String?,
        displayName: String?,
        idToken: String?,
        currentTimeMillis: Long
    ): AuthResult {
        if (id == null) {
            return AuthResult.error(
                AuthError.OAuthError(
                    provider = AuthMethod.GOOGLE,
                    message = "Google account ID is null"
                )
            )
        }
        if (idToken == null) {
            return AuthResult.error(
                AuthError.OAuthError(
                    provider = AuthMethod.GOOGLE,
                    message = "Google ID token is null"
                )
            )
        }
        if (email == null) {
            return AuthResult.error(
                AuthError.OAuthError(
                    provider = AuthMethod.GOOGLE,
                    message = "Google account email is null"
                )
            )
        }

        val user = User.createAuthenticated(
            id = id,
            email = email,
            name = displayName,
            authMethod = AuthMethod.GOOGLE,
            currentTime = currentTimeMillis
        )
        val token = AuthToken.createLongLived(
            value = idToken,
            type = TokenType.BEARER,
            expiresInDays = 30
        )
        return AuthResult.success(user, token)
    }
}

/**
 * Pure mapper from Credential Manager error types ([com.google.android.libraries.identity.googleid.GetGoogleIdException]
 * and [androidx.credentials.GetCredentialException] type strings) to [AuthResult] errors.
 *
 * The type strings are passed as plain values so this mapper stays unit-testable
 * without the Android SDK (DAO #21).
 */
object GoogleCredentialErrorMapper {

    /** Thrown when the user cancels the credential selection UI. */
    const val TYPE_USER_CANCELED: String = "TYPE_USER_CANCELED"

    /** Thrown when no eligible credential/account is available. */
    const val TYPE_NO_CREDENTIAL: String = "TYPE_NO_CREDENTIAL"

    /**
     * Maps a Credential Manager exception type to an [AuthResult] error.
     *
     * @param type Credential Manager exception type string (e.g. TYPE_USER_CANCELED)
     * @param message Optional exception message
     */
    fun map(type: String?, message: String?): AuthResult = when (type) {
        TYPE_USER_CANCELED -> AuthResult.error(
            AuthError.OAuthCancelled(provider = AuthMethod.GOOGLE)
        )
        TYPE_NO_CREDENTIAL -> AuthResult.error(
            AuthError.OAuthError(
                provider = AuthMethod.GOOGLE,
                message = "Aucun compte Google disponible: ${message ?: "type=$type"}"
            )
        )
        else -> AuthResult.error(
            AuthError.OAuthError(
                provider = AuthMethod.GOOGLE,
                message = "Erreur Google Credential Manager: ${type ?: "unknown"} - ${message ?: "no message"}"
            )
        )
    }
}
