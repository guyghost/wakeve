package com.guyghost.wakeve.auth.shell.services

import com.guyghost.wakeve.auth.core.models.AuthResult

/**
 * Android-specific OAuth provider interface.
 *
 * This interface handles Android-specific OAuth operations that require Context,
 * such as Google Sign-In SDK integration. It abstracts the Android OAuth SDKs
 * from the platform-agnostic AuthService interface.
 *
 * Why this interface exists:
 * - The expect AuthService interface cannot accept Context parameters (would break iOS)
 * - Google Sign-In SDK requires Context for creating the sign-in client
 * - This allows injecting platform-specific OAuth providers while keeping AuthService simple
 *
 * @platform Android only - expect/actual pattern is not needed here
 */
interface AndroidOAuthProvider {

    /**
     * Creates a Google Sign-In client with the specified OAuth client ID.
     *
     * @param context Android context (typically Activity context)
     * @param webClientId OAuth web client ID from Google Cloud Console
     * @return Configured GoogleSignInClient instance
     */
    suspend fun createGoogleSignInClient(
        context: android.content.Context,
        webClientId: String
    ): com.google.android.gms.auth.api.signin.GoogleSignInClient

    /**
     * Gets the sign-in intent to launch the Google Sign-In flow.
     *
     * @param client The GoogleSignInClient created by createGoogleSignInClient
     * @return Intent to be launched with startActivityForResult
     */
    suspend fun getGoogleSignInIntent(
        client: com.google.android.gms.auth.api.signin.GoogleSignInClient
    ): android.content.Intent

    /**
     * Handles the result from Google Sign-In activity.
     *
     * @param data Intent data from onActivityResult
     * @return AuthResult with user data or error
     */
    suspend fun handleGoogleSignInResult(data: android.content.Intent?): AuthResult

    /**
     * Signs out the user from Google Sign-In.
     *
     * @param client The GoogleSignInClient to sign out from
     */
    suspend fun signOut(client: com.google.android.gms.auth.api.signin.GoogleSignInClient)
}
