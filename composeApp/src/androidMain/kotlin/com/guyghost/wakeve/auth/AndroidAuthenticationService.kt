package com.guyghost.wakeve.auth

import android.content.Context
import com.guyghost.wakeve.security.AndroidSecureTokenStorage

/**
 * Android-specific authentication service
 */
class AndroidAuthenticationService(
    context: Context,
    baseUrl: String = "http://10.0.2.2:8080" // Android emulator localhost
) : ClientAuthenticationService(
    secureStorage = AndroidSecureTokenStorage(context),
    baseUrl = baseUrl
) {

    /**
     * Handle Google OAuth2 callback from Custom Tabs or WebView
     */
    suspend fun handleGoogleCallback(authorizationCode: String): Result<OAuthLoginResponse> {
        return loginWithGoogle(authorizationCode)
    }

    /**
     * Handle Apple Sign In callback
     */
    suspend fun handleAppleCallback(authorizationCode: String, userInfo: String? = null): Result<OAuthLoginResponse> {
        return loginWithApple(authorizationCode, userInfo)
    }
}