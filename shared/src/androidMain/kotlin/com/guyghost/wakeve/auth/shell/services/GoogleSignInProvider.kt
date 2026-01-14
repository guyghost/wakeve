package com.guyghost.wakeve.auth.shell.services

import com.guyghost.wakeve.auth.core.models.AuthError
import com.guyghost.wakeve.auth.core.models.AuthMethod
import com.guyghost.wakeve.auth.core.models.AuthResult
import com.guyghost.wakeve.auth.core.models.AuthToken
import com.guyghost.wakeve.auth.core.models.User
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Google Sign-In implementation for Android.
 *
 * This provider handles:
 * - Creating and configuring GoogleSignInClient
 * - Launching the sign-in intent
 * - Processing sign-in results and extracting user data
 * - Handling Google Sign-In errors gracefully
 *
 * Error handling:
 * - SIGN_IN_CANCELLED (status code 12501): User cancelled the flow
 * - SIGN_IN_FAILED (other codes): Sign-in failed for other reasons
 * - SIGN_IN_REQUIRED (status code 4): User needs to re-authenticate
 *
 * @requires Google Play Services dependency: com.google.android.gms:play-services-auth
 */
class GoogleSignInProvider : AndroidOAuthProvider {

    override suspend fun createGoogleSignInClient(
        context: android.content.Context,
        webClientId: String
    ): GoogleSignInClient = withContext(Dispatchers.Main) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .requestProfile()
            .build()

        GoogleSignIn.getClient(context, gso)
    }

    override suspend fun getGoogleSignInIntent(
        client: GoogleSignInClient
    ): android.content.Intent = withContext(Dispatchers.Main) {
        client.signInIntent
    }

    override suspend fun handleGoogleSignInResult(
        data: android.content.Intent?
    ): AuthResult = withContext(Dispatchers.Main) {
        try {
            // Get the task from the returned Intent
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)

            // Get the GoogleSignInAccount
            val account = task.getResult(ApiException::class.java)

            // Extract user data and create AuthResult
            createAuthResultFromAccount(account)
        } catch (e: ApiException) {
            handleGoogleSignInError(e)
        } catch (e: Exception) {
            // Handle any unexpected errors
            AuthResult.error(
                AuthError.OAuthError(
                    provider = AuthMethod.GOOGLE,
                    message = "Erreur inattendue: ${e.message}"
                )
            )
        }
    }

    override suspend fun signOut(client: GoogleSignInClient) {
        withContext(Dispatchers.Main) {
            client.signOut()
        }
    }

    /**
     * Creates an AuthResult from a GoogleSignInAccount.
     *
     * Extracts user data:
     * - id: Google account ID
     * - email: Google account email
     * - name: Display name (may be null for some accounts)
     * - photoUrl: Profile photo URL (not currently stored)
     *
     * @param account The GoogleSignInAccount from the sign-in result
     * @return AuthResult.Success with user data and ID token
     */
    private fun createAuthResultFromAccount(account: GoogleSignInAccount): AuthResult {
        val id = account.id ?: throw IllegalStateException("Google account ID is null")
        val email = account.email
        val displayName = account.displayName
        val idToken = account.idToken ?: throw IllegalStateException("Google ID token is null")

        // Create User model
        val user = User.createAuthenticated(
            id = id,
            email = email ?: throw IllegalStateException("Google account email is null"),
            name = displayName,
            authMethod = AuthMethod.GOOGLE
        )

        // Create AuthToken with the ID token
        val token = AuthToken.createLongLived(
            value = idToken,
            type = com.guyghost.wakeve.auth.core.models.TokenType.BEARER,
            expiresInDays = 30
        )

        return AuthResult.success(user, token)
    }

    /**
     * Handles Google Sign-In API exceptions.
     *
     * Maps Google Sign-In status codes to appropriate AuthError:
     * - 12501 (SIGN_IN_CANCELLED): User cancelled the flow
     * - 4 (SIGN_IN_REQUIRED): User needs to re-authenticate
     * - Other codes: Generic OAuth error
     *
     * @param exception The ApiException from Google Sign-In
     * @return AuthResult.Error with appropriate error
     */
    private fun handleGoogleSignInError(exception: ApiException): AuthResult {
        return when (exception.statusCode) {
            // User cancelled the sign-in flow
            com.google.android.gms.common.api.CommonStatusCodes.CANCELED -> {
                AuthResult.error(
                    AuthError.OAuthCancelled(
                        provider = AuthMethod.GOOGLE
                    )
                )
            }

            // Sign-in required (user needs to re-authenticate)
            4 -> {
                AuthResult.error(
                    AuthError.OAuthError(
                        provider = AuthMethod.GOOGLE,
                        message = "Ré-authentification requise"
                    )
                )
            }

            // Other sign-in errors
            else -> {
                AuthResult.error(
                    AuthError.OAuthError(
                        provider = AuthMethod.GOOGLE,
                        message = "Erreur Google Sign-In: ${exception.statusCode} - ${exception.message}"
                    )
                )
            }
        }
    }
}
