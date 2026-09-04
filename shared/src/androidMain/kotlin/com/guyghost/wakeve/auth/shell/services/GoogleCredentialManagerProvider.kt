package com.guyghost.wakeve.auth.shell.services

import android.app.Activity
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.guyghost.wakeve.auth.core.logic.parseJWT
import com.guyghost.wakeve.auth.core.models.AuthError
import com.guyghost.wakeve.auth.core.models.AuthMethod
import com.guyghost.wakeve.auth.core.models.AuthResult
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Google Sign-In implementation using Android Credential Manager.
 *
 * Replaces the legacy GoogleSignIn API (play-services-auth) intent-based flow
 * with the modern one-call suspend API (DAO #21):
 * - No Activity Result launcher: [signIn] suspends until the user completes
 *   the account picker flow and returns the [AuthResult] directly.
 * - The Google ID token is extracted from the [GoogleIdTokenCredential] and
 *   mapped through [GoogleAuthResultFactory] (shared with the legacy provider).
 * - Credential Manager error types are mapped through [GoogleCredentialErrorMapper]
 *   (user cancellation → [AuthError.OAuthCancelled], etc.).
 *
 * The email is read from the ID token JWT payload (`email` claim), as
 * Credential Manager does not expose it as a direct field.
 */
class GoogleCredentialManagerProvider {

    /**
     * Runs the Credential Manager Google sign-in flow.
     *
     * @param activity The activity used to show the account picker
     * @param serverClientId OAuth web client ID from Google Cloud Console
     * @return [AuthResult.Success] with the user profile and ID token, or
     *         [AuthResult.Error] (including [AuthError.OAuthCancelled] when
     *         the user cancels the flow).
     */
    suspend fun signIn(
        activity: Activity,
        serverClientId: String
    ): AuthResult = withContext(Dispatchers.Main) {
        try {
            val credentialManager = CredentialManager.create(activity)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(serverClientId)
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response = credentialManager.getCredential(activity, request)
            val credential = response.credential

            if (credential is GoogleIdTokenCredential) {
                val email = parseJWT(credential.idToken)?.get("email") as? String
                GoogleAuthResultFactory.createFromProfile(
                    id = credential.id,
                    email = email,
                    displayName = credential.displayName,
                    idToken = credential.idToken,
                    currentTimeMillis = System.currentTimeMillis()
                )
            } else {
                AuthResult.error(
                    AuthError.OAuthError(
                        provider = AuthMethod.GOOGLE,
                        message = "Type d'identifiant inattendu: ${credential.type}"
                    )
                )
            }
        } catch (e: GetCredentialException) {
            GoogleCredentialErrorMapper.map(e.type, e.message)
        } catch (e: GoogleIdTokenParsingException) {
            AuthResult.error(
                AuthError.OAuthError(
                    provider = AuthMethod.GOOGLE,
                    message = "Erreur de parsing du Google ID token: ${e.message}"
                )
            )
        } catch (e: Exception) {
            AuthResult.error(
                AuthError.OAuthError(
                    provider = AuthMethod.GOOGLE,
                    message = "Erreur inattendue: ${e.message}"
                )
            )
        }
    }

    /**
     * Clears the Credential Manager state (equivalent of legacy signOut).
     */
    suspend fun signOut(activity: Activity): Result<Unit> = runCatching {
        withContext(Dispatchers.Main) {
            CredentialManager.create(activity)
                .clearCredentialState(ClearCredentialStateRequest())
        }
    }
}
