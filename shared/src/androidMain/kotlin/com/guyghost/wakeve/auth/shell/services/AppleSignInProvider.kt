package com.guyghost.wakeve.auth.shell.services

/**
 * Android-specific Apple Sign-In provider interface.
 *
 * This interface handles Apple Sign-In via OAuth 2.0 web flow on Android,
 * since there's no native Sign in with Apple SDK for Android.
 *
 * The web flow consists of:
 * 1. getAuthorizationUrl() - Returns the Apple auth URL to open in a browser/custom tab
 * 2. exchangeCodeForTokens() - Exchanges the authorization code for access/ID tokens
 * 3. parseIdToken() - Parses the JWT ID token to extract user information
 *
 * Why this interface exists:
 * - Apple doesn't provide a native SDK for Android like Google does
 * - Web-based OAuth flow is required (using Custom Tab or WebView)
 * - This abstracts the OAuth flow details from the AuthService
 *
 * Security Considerations:
 * - client_secret should ideally be generated on the server, not the client
 * - For better security, move code exchange to the backend
 * - The id_token should be verified by the backend (signature, issuer, audience)
 *
 * @platform Android only - iOS has native Sign in with Apple framework
 */
interface AppleSignInProvider {

    /**
     * Constructs the Apple Sign-In authorization URL.
     *
     * This URL should be opened in a browser or Custom Tab to initiate the sign-in flow.
     * After the user signs in, they will be redirected to the redirect_uri with an authorization code.
     *
     * Apple's Authorization Endpoint: https://appleid.apple.com/auth/authorize
     *
     * @param clientId The Services ID from Apple Developer portal
     * @param redirectUri The redirect URI registered in Apple Developer portal (must match exactly)
     * @param state A random string to prevent CSRF attacks (should be validated on callback)
     * @param scopes The OAuth scopes to request (default: name, email)
     *               Apple only supports: name, email
     * @return The complete authorization URL to open in a browser/Custom Tab
     *
     * @example
     * ```
     * val url = appleSignInProvider.getAuthorizationUrl(
     *     clientId = "com.example.app",
     *     redirectUri = "https://example.com/callback",
     *     state = "random_state_string"
     * )
     * customTabsIntent.launchUrl(context, Uri.parse(url))
     * ```
     */
    suspend fun getAuthorizationUrl(
        clientId: String,
        redirectUri: String,
        state: String,
        scopes: List<String> = listOf("name", "email")
    ): String

    /**
     * Exchanges the authorization code for access and ID tokens.
     *
     * This is called after the user completes the sign-in flow and the app receives
     * the authorization code in the redirect callback.
     *
     * Apple's Token Endpoint: https://appleid.apple.com/auth/token
     *
     * @param code The authorization code received in the redirect callback
     * @param clientId The Services ID from Apple Developer portal
     * @param clientSecret The JWT client secret generated from Apple private key
     *                     (Note: Should ideally be generated on the server)
     * @param redirectUri The redirect URI registered in Apple Developer portal (must match exactly)
     * @return Result<AppleTokenResponse> with tokens or error
     *
     * @example
     * ```
     * val result = appleSignInProvider.exchangeCodeForTokens(
     *     code = receivedCode,
     *     clientId = "com.example.app",
     *     clientSecret = "generated_jwt_client_secret",
     *     redirectUri = "https://example.com/callback"
     * )
     * result.onSuccess { tokens ->
     *     val idToken = tokens.idToken
     *     // Parse user info from idToken
     * }
     * ```
     */
    suspend fun exchangeCodeForTokens(
        code: String,
        clientId: String,
        clientSecret: String?,
        redirectUri: String
    ): Result<AppleTokenResponse>

    /**
     * Parses the JWT ID token to extract user information.
     *
     * The ID token contains claims about the authenticated user, including:
     * - sub: Subject (unique user ID from Apple)
     * - email: User's email (may be null if not requested/shared)
     * - email_verified: Whether the email is verified
     * - name: User's name (only included in the first sign-in, may be null)
     *
     * Note: This implementation does NOT verify the JWT signature.
     * For production, signature verification should be done on the backend.
     *
     * @param idToken The JWT ID token received from Apple
     * @return Result<AppleUserInfo> with user information or error
     *
     * @example
     * ```
     * val result = appleSignInProvider.parseIdToken(idToken)
     * result.onSuccess { userInfo ->
     *     val userId = userInfo.sub // Use this as the unique user ID
     *     val email = userInfo.email
     *     val name = userInfo.name
     * }
     * ```
     */
    suspend fun parseIdToken(idToken: String): Result<AppleUserInfo>
}

/**
 * Apple Sign-In token response from the token endpoint.
 *
 * @property accessToken The OAuth access token
 * @property idToken The OpenID Connect ID token (JWT containing user info)
 * @property refreshToken The refresh token (may be null)
 * @property expiresIn Access token expiration time in seconds (may be null)
 */
data class AppleTokenResponse(
    val accessToken: String,
    val idToken: String,
    val refreshToken: String? = null,
    val expiresIn: Int? = null
)

/**
 * User information extracted from Apple Sign-In ID token.
 *
 * @property sub Subject - unique user ID from Apple (use this as the primary identifier)
 * @property email User's email address (may be null if user didn't share or it's hidden)
 * @property name User's full name (may be null - Apple only provides this on first sign-in)
 * @property emailVerified Whether the email has been verified by Apple
 */
data class AppleUserInfo(
    val sub: String, // Subject (user ID)
    val email: String?,
    val name: String?,
    val emailVerified: Boolean
)
