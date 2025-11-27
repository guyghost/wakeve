package com.guyghost.wakeve.auth

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import kotlinx.coroutines.*
import java.awt.Desktop
import java.net.URI
import java.net.URLEncoder
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Helper class to manage browser-based OAuth flow on JVM/Desktop.
 *
 * This opens the system browser for OAuth authorization and starts a local
 * HTTP server to receive the callback with the authorization code.
 *
 * Flow:
 * 1. Start embedded HTTP server on localhost:8181
 * 2. Open system browser with OAuth provider URL
 * 3. User authenticates with provider
 * 4. Provider redirects to http://localhost:8181/callback?code=...
 * 5. Extract authorization code from callback URL
 * 6. Stop HTTP server
 *
 * Usage:
 * ```kotlin
 * val helper = BrowserOAuthHelper(
 *     clientId = "your-client-id",
 *     redirectUri = "http://localhost:8181/callback",
 *     authorizationEndpoint = "https://accounts.google.com/o/oauth2/v2/auth"
 * )
 *
 * val result = helper.authorize()
 * result.onSuccess { authCode ->
 *     // Pass to AuthenticationService
 * }
 * ```
 */
class BrowserOAuthHelper(
    private val clientId: String,
    private val redirectUri: String = "http://localhost:8181/callback",
    private val authorizationEndpoint: String,
    private val scopes: List<String> = listOf("openid", "email", "profile"),
    private val port: Int = 8181
) {
    private var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Start the OAuth authorization flow.
     *
     * This opens the browser and waits for the callback with the authorization code.
     *
     * @return Result containing authorization code on success, or error on failure
     */
    suspend fun authorize(): Result<String> = suspendCancellableCoroutine { continuation ->
        try {
            // Start local HTTP server to receive callback
            server = startCallbackServer { result ->
                continuation.resume(result)
            }

            // Build authorization URL
            val authUrl = buildAuthorizationUrl()

            // Open system browser
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(authUrl))
            } else {
                continuation.resumeWithException(
                    OAuthException("Desktop browsing not supported on this system")
                )
                return@suspendCancellableCoroutine
            }

            // Set up cancellation
            continuation.invokeOnCancellation {
                stopServer()
            }

        } catch (e: Exception) {
            continuation.resumeWithException(OAuthException("Failed to start OAuth flow: ${e.message}", e))
        }
    }

    /**
     * Build the OAuth authorization URL with all required parameters.
     */
    private fun buildAuthorizationUrl(): String {
        val params = mapOf(
            "client_id" to clientId,
            "redirect_uri" to redirectUri,
            "response_type" to "code",
            "scope" to scopes.joinToString(" "),
            "access_type" to "offline", // Request refresh token
            "prompt" to "consent" // Force consent screen
        )

        val queryString = params.entries.joinToString("&") { (key, value) ->
            "$key=${URLEncoder.encode(value, "UTF-8")}"
        }

        return "$authorizationEndpoint?$queryString"
    }

    /**
     * Start embedded HTTP server to receive OAuth callback.
     */
    private fun startCallbackServer(onResult: (Result<String>) -> Unit): EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration> {
        return embeddedServer(Netty, port = port) {
            routing {
                get("/callback") {
                    val code = call.request.queryParameters["code"]
                    val error = call.request.queryParameters["error"]
                    val errorDescription = call.request.queryParameters["error_description"]

                    when {
                        code != null -> {
                            // Success - got authorization code
                            call.respondText(
                                """
                                <!DOCTYPE html>
                                <html>
                                <head>
                                    <title>Sign-in Successful</title>
                                    <style>
                                        body {
                                            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                                            display: flex;
                                            justify-content: center;
                                            align-items: center;
                                            height: 100vh;
                                            margin: 0;
                                            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                                        }
                                        .container {
                                            background: white;
                                            padding: 3rem;
                                            border-radius: 16px;
                                            box-shadow: 0 10px 40px rgba(0,0,0,0.2);
                                            text-align: center;
                                            max-width: 400px;
                                        }
                                        .checkmark {
                                            font-size: 4rem;
                                            color: #10b981;
                                            margin-bottom: 1rem;
                                        }
                                        h1 {
                                            color: #1f2937;
                                            margin: 0 0 0.5rem 0;
                                            font-size: 1.75rem;
                                        }
                                        p {
                                            color: #6b7280;
                                            margin: 0;
                                            font-size: 1rem;
                                        }
                                    </style>
                                </head>
                                <body>
                                    <div class="container">
                                        <div class="checkmark">✓</div>
                                        <h1>Sign-in Successful!</h1>
                                        <p>You can close this window and return to the application.</p>
                                    </div>
                                    <script>
                                        // Auto-close after 3 seconds
                                        setTimeout(() => window.close(), 3000);
                                    </script>
                                </body>
                                </html>
                                """.trimIndent(),
                                ContentType.Text.Html,
                                HttpStatusCode.OK
                            )
                            onResult(Result.success(code))
                            // Stop server after successful callback
                            coroutineScope.launch {
                                delay(4000) // Give time for response to be sent
                                stopServer()
                            }
                        }
                        error != null -> {
                            // Error from OAuth provider
                            call.respondText(
                                """
                                <!DOCTYPE html>
                                <html>
                                <head>
                                    <title>Sign-in Failed</title>
                                    <style>
                                        body {
                                            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                                            display: flex;
                                            justify-content: center;
                                            align-items: center;
                                            height: 100vh;
                                            margin: 0;
                                            background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
                                        }
                                        .container {
                                            background: white;
                                            padding: 3rem;
                                            border-radius: 16px;
                                            box-shadow: 0 10px 40px rgba(0,0,0,0.2);
                                            text-align: center;
                                            max-width: 400px;
                                        }
                                        .error-icon {
                                            font-size: 4rem;
                                            color: #ef4444;
                                            margin-bottom: 1rem;
                                        }
                                        h1 {
                                            color: #1f2937;
                                            margin: 0 0 0.5rem 0;
                                            font-size: 1.75rem;
                                        }
                                        p {
                                            color: #6b7280;
                                            margin: 0;
                                            font-size: 1rem;
                                        }
                                        .error-details {
                                            background: #fef2f2;
                                            border: 1px solid #fecaca;
                                            border-radius: 8px;
                                            padding: 1rem;
                                            margin-top: 1rem;
                                            font-size: 0.875rem;
                                            color: #991b1b;
                                        }
                                    </style>
                                </head>
                                <body>
                                    <div class="container">
                                        <div class="error-icon">✗</div>
                                        <h1>Sign-in Failed</h1>
                                        <p>An error occurred during authentication.</p>
                                        <div class="error-details">
                                            ${errorDescription ?: error}
                                        </div>
                                        <p style="margin-top: 1rem;">You can close this window and try again.</p>
                                    </div>
                                </body>
                                </html>
                                """.trimIndent(),
                                ContentType.Text.Html,
                                HttpStatusCode.OK
                            )
                            onResult(Result.failure(OAuthException("OAuth error: $error - $errorDescription")))
                            coroutineScope.launch {
                                delay(5000)
                                stopServer()
                            }
                        }
                        else -> {
                            // Invalid callback - missing both code and error
                            call.respond(HttpStatusCode.BadRequest, "Invalid OAuth callback")
                            onResult(Result.failure(OAuthException("Invalid OAuth callback: no code or error")))
                            coroutineScope.launch {
                                delay(2000)
                                stopServer()
                            }
                        }
                    }
                }

                // Health check endpoint
                get("/health") {
                    call.respondText("OAuth callback server is running", ContentType.Text.Plain, HttpStatusCode.OK)
                }
            }
        }.start(wait = false)
    }

    /**
     * Stop the callback server.
     */
    fun stopServer() {
        server?.stop(1000, 2000)
        server = null
    }

    /**
     * Clean up resources.
     */
    fun close() {
        stopServer()
        coroutineScope.cancel()
    }

    companion object {
        /**
         * Get OAuth authorization endpoint for Google.
         */
        fun getGoogleAuthEndpoint() = "https://accounts.google.com/o/oauth2/v2/auth"

        /**
         * Get OAuth authorization endpoint for Apple (not typically used on JVM).
         */
        fun getAppleAuthEndpoint() = "https://appleid.apple.com/auth/authorize"

        /**
         * Default scopes for Google OAuth.
         */
        fun getDefaultGoogleScopes() = listOf("openid", "email", "profile")

        /**
         * Create a BrowserOAuthHelper pre-configured for Google Sign-In.
         */
        fun forGoogle(clientId: String, port: Int = 8181): BrowserOAuthHelper {
            return BrowserOAuthHelper(
                clientId = clientId,
                redirectUri = "http://localhost:$port/callback",
                authorizationEndpoint = getGoogleAuthEndpoint(),
                scopes = getDefaultGoogleScopes(),
                port = port
            )
        }
    }
}

/**
 * Custom exception for OAuth errors.
 */
class OAuthException(message: String, cause: Throwable? = null) : Exception(message, cause)
