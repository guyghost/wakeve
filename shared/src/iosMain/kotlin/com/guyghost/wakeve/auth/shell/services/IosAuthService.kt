package com.guyghost.wakeve.auth.shell.services

import com.guyghost.wakeve.auth.core.models.AuthError
import com.guyghost.wakeve.auth.core.models.AuthMethod
import com.guyghost.wakeve.auth.core.models.AuthResult
import com.guyghost.wakeve.auth.core.models.AuthToken
import com.guyghost.wakeve.auth.core.models.User
import com.guyghost.wakeve.auth.core.logic.currentTimeMillis
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.coroutines.resume

/**
 * Callback interface pour l'authentification native iOS.
 *
 * Le côté Swift (LoginView/AppleSignInHelper) implémente ce callback
 * pour fournir les résultats d'authentification au module KMP.
 */
interface IosSignInCallback {
    /**
     * Called when native sign-in completes successfully.
     *
     * @param userId The user's unique identifier
     * @param email The user's email
     * @param name The user's display name
     * @param idToken The identity token (JWT) from the OAuth provider
     * @param authMethod The authentication method ("apple" or "google")
     */
    fun onSignInSuccess(
        userId: String,
        email: String?,
        name: String?,
        idToken: String,
        authMethod: String
    )

    /**
     * Called when native sign-in is cancelled by the user.
     */
    fun onSignInCancelled()

    /**
     * Called when native sign-in fails.
     *
     * @param errorMessage Description of the error
     */
    fun onSignInError(errorMessage: String)
}

/**
 * iOS implementation of AuthService using native Sign in with Apple and Google Sign-In.
 *
 * Architecture:
 * - signInWithApple() et signInWithGoogle() délèguent au côté Swift natif via IosSignInCallback
 * - isAuthenticated(), getCurrentUser(), signOut() utilisent IosTokenStorage (Keychain)
 * - refreshToken() appelle le backend /api/auth/refresh via Ktor
 *
 * Le flux d'authentification iOS principal passe par:
 * LoginView.swift → AuthStateManager.swift → AuthenticationService.swift → Backend
 *
 * Ce service KMP est utilisé quand le code partagé doit vérifier l'état d'auth
 * ou interagir avec le système d'auth depuis le module shared.
 */
actual class AuthService {

    /**
     * Token storage for Keychain access.
     */
    private val tokenStorage: TokenStorage = IosTokenStorage()

    /**
     * Native sign-in callback, set from Swift side.
     * Permet au côté Swift d'injecter les résultats d'authentification.
     */
    private var signInCallback: IosSignInCallback? = null

    /**
     * HTTP client for backend API calls (token refresh).
     */
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    /**
     * Base URL for the Wakeve API server.
     * TODO: Move to build configuration.
     */
    private val baseUrl: String = "http://localhost:8080/api"

    /**
     * Sets the native sign-in callback from the Swift side.
     *
     * Call this during app initialization:
     * ```swift
     * let authService = AuthService()
     * authService.setSignInCallback(mySwiftCallback)
     * ```
     *
     * @param callback The IosSignInCallback implementation
     */
    fun setSignInCallback(callback: IosSignInCallback) {
        this.signInCallback = callback
    }

    /**
     * Initiates Apple Sign-In flow on iOS.
     *
     * Delegates to the native Swift layer via IosSignInCallback.
     * The actual sign-in UI (ASAuthorizationController) is presented by the Swift AppleSignInHelper.
     * This method suspends until the callback delivers the result.
     *
     * If no callback is set, returns an error indicating that the iOS native layer
     * should handle authentication directly via AuthenticationService.swift.
     *
     * @return AuthResult with authenticated user or error
     */
    actual suspend fun signInWithApple(): AuthResult = withContext(Dispatchers.Main) {
        val callback = signInCallback
        if (callback == null) {
            // No native callback set - auth should be handled by Swift layer
            return@withContext AuthResult.error(
                AuthError.OAuthError(
                    provider = AuthMethod.APPLE,
                    message = "Utilisez LoginView.swift pour l'authentification Apple sur iOS"
                )
            )
        }

        try {
            suspendCancellableCoroutine { continuation ->
                val wrappedCallback = object : IosSignInCallback {
                    override fun onSignInSuccess(
                        userId: String,
                        email: String?,
                        name: String?,
                        idToken: String,
                        authMethod: String
                    ) {
                        val user = User(
                            id = userId,
                            email = email,
                            name = name,
                            authMethod = AuthMethod.APPLE,
                            isGuest = false,
                            createdAt = currentTimeMillis(),
                            lastLoginAt = currentTimeMillis()
                        )
                        val token = AuthToken.createLongLived(
                            value = idToken,
                            expiresInDays = 30
                        )
                        continuation.resume(AuthResult.success(user, token))
                    }

                    override fun onSignInCancelled() {
                        continuation.resume(
                            AuthResult.error(AuthError.OAuthCancelled(AuthMethod.APPLE))
                        )
                    }

                    override fun onSignInError(errorMessage: String) {
                        continuation.resume(
                            AuthResult.error(
                                AuthError.OAuthError(
                                    provider = AuthMethod.APPLE,
                                    message = errorMessage
                                )
                            )
                        )
                    }
                }
                // Trigger native Apple Sign-In via the callback
                // The Swift layer will call the appropriate method on completion
                signInCallback = wrappedCallback
            }
        } catch (e: Exception) {
            AuthResult.error(
                AuthError.OAuthError(
                    provider = AuthMethod.APPLE,
                    message = e.message ?: "Erreur Sign in with Apple"
                )
            )
        }
    }

    /**
     * Initiates Google Sign-In flow on iOS.
     *
     * Google Sign-In on iOS can be handled via:
     * 1. Google Sign-In SDK (GIDSignIn) - if available
     * 2. Web-based OAuth flow via Safari
     *
     * Currently delegates to the native Swift layer via callback.
     * If no callback is set, returns an error.
     *
     * @return AuthResult with authenticated user or error
     */
    actual suspend fun signInWithGoogle(): AuthResult = withContext(Dispatchers.Main) {
        val callback = signInCallback
        if (callback == null) {
            return@withContext AuthResult.error(
                AuthError.OAuthError(
                    provider = AuthMethod.GOOGLE,
                    message = "Google Sign-In n'est pas encore configuré pour iOS"
                )
            )
        }

        try {
            suspendCancellableCoroutine { continuation ->
                val wrappedCallback = object : IosSignInCallback {
                    override fun onSignInSuccess(
                        userId: String,
                        email: String?,
                        name: String?,
                        idToken: String,
                        authMethod: String
                    ) {
                        val user = User(
                            id = userId,
                            email = email,
                            name = name,
                            authMethod = AuthMethod.GOOGLE,
                            isGuest = false,
                            createdAt = currentTimeMillis(),
                            lastLoginAt = currentTimeMillis()
                        )
                        val token = AuthToken.createLongLived(
                            value = idToken,
                            expiresInDays = 30
                        )
                        continuation.resume(AuthResult.success(user, token))
                    }

                    override fun onSignInCancelled() {
                        continuation.resume(
                            AuthResult.error(AuthError.OAuthCancelled(AuthMethod.GOOGLE))
                        )
                    }

                    override fun onSignInError(errorMessage: String) {
                        continuation.resume(
                            AuthResult.error(
                                AuthError.OAuthError(
                                    provider = AuthMethod.GOOGLE,
                                    message = errorMessage
                                )
                            )
                        )
                    }
                }
                signInCallback = wrappedCallback
            }
        } catch (e: Exception) {
            AuthResult.error(
                AuthError.OAuthError(
                    provider = AuthMethod.GOOGLE,
                    message = e.message ?: "Erreur Google Sign-In"
                )
            )
        }
    }

    /**
     * Signs out the current user and clears all stored tokens.
     *
     * Clears access token, refresh token, user ID, and all profile data
     * from the iOS Keychain via IosTokenStorage.
     */
    actual suspend fun signOut() {
        withContext(Dispatchers.Default) {
            try {
                tokenStorage.clearAll()
                println("[IosAuthService] Déconnexion: tokens supprimés du Keychain")
            } catch (e: Exception) {
                println("[IosAuthService] Erreur lors de la déconnexion: ${e.message}")
            }
        }
    }

    /**
     * Checks if the user is currently authenticated.
     *
     * Reads the access token from Keychain and checks if it exists and
     * has not expired based on the stored expiry timestamp.
     *
     * @return true if a valid (non-expired) access token exists in Keychain
     */
    actual suspend fun isAuthenticated(): Boolean {
        return withContext(Dispatchers.Default) {
            try {
                val accessToken = tokenStorage.getString(TokenKeys.ACCESS_TOKEN)
                val expiryStr = tokenStorage.getString(TokenKeys.TOKEN_EXPIRY)

                if (accessToken.isNullOrBlank()) {
                    return@withContext false
                }

                // Check expiry if stored
                if (expiryStr != null) {
                    val expiry = expiryStr.toLongOrNull() ?: return@withContext false
                    val now = currentTimeMillis()
                    return@withContext now < expiry
                }

                // Token exists but no expiry stored - consider valid
                true
            } catch (e: Exception) {
                println("[IosAuthService] Erreur vérification auth: ${e.message}")
                false
            }
        }
    }

    /**
     * Gets the current authenticated user from stored Keychain data.
     *
     * Reconstructs a User object from individually stored fields
     * in the iOS Keychain (user ID, email, auth method).
     *
     * @return User if valid user data is stored, null otherwise
     */
    actual suspend fun getCurrentUser(): User? {
        return withContext(Dispatchers.Default) {
            try {
                val userId = tokenStorage.getString(TokenKeys.USER_ID) ?: return@withContext null

                val email = tokenStorage.getString("user_email")
                val name = tokenStorage.getString("user_name")
                val authMethodStr = tokenStorage.getString(TokenKeys.AUTH_METHOD)

                val authMethod = when (authMethodStr?.uppercase()) {
                    "APPLE" -> AuthMethod.APPLE
                    "GOOGLE" -> AuthMethod.GOOGLE
                    "EMAIL" -> AuthMethod.EMAIL
                    else -> AuthMethod.GUEST
                }

                User(
                    id = userId,
                    email = email,
                    name = name,
                    authMethod = authMethod,
                    isGuest = false,
                    createdAt = currentTimeMillis(),
                    lastLoginAt = currentTimeMillis()
                )
            } catch (e: Exception) {
                println("[IosAuthService] Erreur récupération utilisateur: ${e.message}")
                null
            }
        }
    }

    /**
     * Refreshes the authentication token by calling the backend /api/auth/refresh endpoint.
     *
     * Reads the refresh token from Keychain, sends it to the backend,
     * and stores the new access token and expiry.
     *
     * @return AuthResult with refreshed user data and new token, or error
     */
    actual suspend fun refreshToken(): AuthResult {
        return withContext(Dispatchers.Default) {
            try {
                val refreshToken = tokenStorage.getString(TokenKeys.REFRESH_TOKEN)
                if (refreshToken.isNullOrBlank()) {
                    return@withContext AuthResult.error(
                        AuthError.UnknownError("Pas de refresh token disponible")
                    )
                }

                val response = httpClient.post("$baseUrl/auth/refresh") {
                    contentType(ContentType.Application.Json)
                    setBody(TokenRefreshBody(refreshToken = refreshToken))
                }

                if (response.status.isSuccess()) {
                    val refreshResponse = response.body<TokenRefreshResponseBody>()

                    // Store new access token
                    tokenStorage.storeString(TokenKeys.ACCESS_TOKEN, refreshResponse.accessToken)

                    // Update expiry
                    val newExpiry = currentTimeMillis() + (refreshResponse.expiresIn * 1000L)
                    tokenStorage.storeString(TokenKeys.TOKEN_EXPIRY, newExpiry.toString())

                    // Store new refresh token if provided
                    if (refreshResponse.refreshToken != null) {
                        tokenStorage.storeString(TokenKeys.REFRESH_TOKEN, refreshResponse.refreshToken)
                    }

                    // Reconstruct current user
                    val currentUser = getCurrentUser()
                    if (currentUser != null) {
                        AuthResult.success(
                            currentUser,
                            AuthToken.create(
                                value = refreshResponse.accessToken,
                                expiresInSeconds = refreshResponse.expiresIn
                            )
                        )
                    } else {
                        AuthResult.error(
                            AuthError.UnknownError("Token rafraîchi mais données utilisateur introuvables")
                        )
                    }
                } else {
                    AuthResult.error(
                        AuthError.NetworkError
                    )
                }
            } catch (e: Exception) {
                println("[IosAuthService] Erreur refresh token: ${e.message}")
                AuthResult.error(AuthError.NetworkError)
            }
        }
    }

    /**
     * Checks if a specific OAuth provider is available on this iOS device.
     *
     * @param provider The authentication method to check
     * @return true if the provider is configured and available
     */
    actual suspend fun isProviderAvailable(provider: AuthMethod): Boolean {
        return withContext(Dispatchers.Main) {
            when (provider) {
                AuthMethod.APPLE -> {
                    // Apple Sign-In est disponible sur iOS 13+
                    true
                }
                AuthMethod.GOOGLE -> {
                    // Google Sign-In nécessite le SDK GIDSignIn
                    // Pour l'instant, non disponible sans le SDK
                    false
                }
                else -> false
            }
        }
    }
}

/**
 * Request body for token refresh (matches server TokenRefreshRequest).
 */
@Serializable
private data class TokenRefreshBody(
    val refreshToken: String
)

/**
 * Response body from token refresh endpoint.
 * Matches the server's OAuthLoginResponse structure.
 */
@Serializable
private data class TokenRefreshResponseBody(
    val accessToken: String,
    val refreshToken: String? = null,
    val expiresIn: Long = 3600,
    val tokenType: String = "Bearer"
)
