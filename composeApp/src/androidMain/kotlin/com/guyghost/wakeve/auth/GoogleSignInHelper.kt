package com.guyghost.wakeve.auth

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task

/**
 * Helper class to manage Google Sign-In flow on Android.
 *
 * This wraps the Google Sign-In SDK and provides a clean interface for:
 * - Launching the Google account picker
 * - Extracting the authorization code from the result
 * - Handling errors (user cancellation, network issues, etc.)
 *
 * Usage:
 * ```
 * val googleSignInHelper = GoogleSignInHelper(context, clientId)
 *
 * // Launch sign-in
 * val intent = googleSignInHelper.getSignInIntent()
 * startActivityForResult(intent, RC_SIGN_IN)
 *
 * // Handle result in onActivityResult
 * val result = googleSignInHelper.handleSignInResult(data)
 * result.onSuccess { authCode ->
 *     // Pass to AndroidAuthenticationService.loginWithGoogle(authCode)
 * }
 * ```
 */
class GoogleSignInHelper(
    private val context: Context,
    private val clientId: String,
    private val serverClientId: String? = null
) {
    private val googleSignInClient: GoogleSignInClient

    init {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            // Request ID token for authentication
            .requestIdToken(clientId)
            .apply {
                // Request server auth code if backend needs it
                serverClientId?.let {
                    requestServerAuthCode(it)
                }
            }
            // Request OpenID scope
            .requestScopes(Scope("openid"))
            .build()

        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    /**
     * Get the Intent to launch Google Sign-In account picker.
     *
     * @return Intent to start with startActivityForResult()
     */
    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    /**
     * Handle the result from Google Sign-In activity.
     *
     * Extract the authorization code or ID token from the result Intent.
     *
     * @param data The Intent data from onActivityResult
     * @return Result containing authorization code on success, or error on failure
     */
    fun handleSignInResult(data: Intent?): Result<String> {
        return try {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)

            // Extract auth code or ID token
            val authCode = account.serverAuthCode
                ?: account.idToken
                ?: return Result.failure(Exception("No authorization code or ID token received"))

            Log.d(TAG, "Google Sign-In successful for: ${account.email}")
            Result.success(authCode)

        } catch (e: ApiException) {
            Log.e(TAG, "Google Sign-In failed with code: ${e.statusCode}", e)

            val errorMessage = when (e.statusCode) {
                SIGN_IN_CANCELLED -> "Sign-in was cancelled by the user"
                NETWORK_ERROR -> "Network error. Please check your connection and try again"
                INTERNAL_ERROR -> "Internal error occurred. Please try again"
                DEVELOPER_ERROR -> "Developer error. Check your Google Sign-In configuration"
                else -> "Sign-in failed: ${e.message}"
            }

            Result.failure(GoogleSignInException(errorMessage, e.statusCode, e))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during sign-in", e)
            Result.failure(GoogleSignInException("Unexpected error: ${e.message}", UNKNOWN_ERROR, e))
        }
    }

    /**
     * Get currently signed-in Google account (if any).
     *
     * @return GoogleSignInAccount if user is signed in, null otherwise
     */
    fun getLastSignedInAccount(): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    /**
     * Sign out the current user from Google Sign-In.
     *
     * This clears the Google account selection. The user will be prompted
     * to pick an account again on next sign-in.
     */
    suspend fun signOut(): Result<Unit> = runCatching {
        googleSignInClient.signOut().await()
    }

    /**
     * Revoke access for the current user.
     *
     * This removes all granted permissions. The user will need to grant
     * permissions again on next sign-in.
     */
    suspend fun revokeAccess(): Result<Unit> = runCatching {
        googleSignInClient.revokeAccess().await()
    }

    /**
     * Check if user is currently signed in with Google.
     *
     * @return true if signed in, false otherwise
     */
    fun isSignedIn(): Boolean {
        return getLastSignedInAccount() != null
    }

    companion object {
        private const val TAG = "GoogleSignInHelper"

        // Google Sign-In status codes
        const val SIGN_IN_CANCELLED = 12501
        const val NETWORK_ERROR = 7
        const val INTERNAL_ERROR = 8
        const val DEVELOPER_ERROR = 10
        const val UNKNOWN_ERROR = -1

        /**
         * Request code for Google Sign-In activity result.
         * Use this in startActivityForResult() and onActivityResult().
         */
        const val RC_SIGN_IN = 9001
    }
}

/**
 * Custom exception for Google Sign-In errors.
 *
 * @property message User-friendly error message
 * @property statusCode Google Sign-In API status code
 * @property cause Original exception
 */
class GoogleSignInException(
    message: String,
    val statusCode: Int,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Extension function to await Task completion with Kotlin coroutines.
 */
private suspend fun <T> Task<T>.await(): T {
    return kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            continuation.resumeWith(Result.success(result))
        }
        addOnFailureListener { exception ->
            continuation.resumeWith(Result.failure(exception))
        }
        addOnCanceledListener {
            continuation.cancel()
        }
    }
}
