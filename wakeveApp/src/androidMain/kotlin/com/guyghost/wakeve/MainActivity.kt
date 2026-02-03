package com.guyghost.wakeve

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import com.guyghost.wakeve.auth.AndroidAuthenticationService
import com.guyghost.wakeve.di.appModule
import com.guyghost.wakeve.di.platformModule
import com.guyghost.wakeve.navigation.AuthCallbacks
import com.guyghost.wakeve.navigation.LocalAuthCallbacks
import com.guyghost.wakeve.security.AndroidSecureTokenStorage
import com.guyghost.wakeve.auth.shell.services.AuthService
import com.guyghost.wakeve.auth.shell.services.GoogleSignInProvider
import com.guyghost.wakeve.auth.shell.services.AppleSignInProvider
import com.guyghost.wakeve.auth.shell.services.AppleSignInWebFlow
import com.guyghost.wakeve.ui.auth.AuthViewModel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class MainActivity : ComponentActivity(), AuthCallbacks {

    /**
     * OAuth Configuration
     *
     * Read from BuildConfig which is populated from local.properties (not in version control).
     *
     * Developers must configure these values in local.properties:
     * - google.web.client.id=YOUR_ACTUAL_CLIENT_ID (from Google Cloud Console)
     * - apple.client.id=com.yourcompany.wakeve (your Apple App ID)
     * - apple.redirect.uri=wakeve://apple-auth-callback (your custom scheme)
     *
     * See https://developers.google.com/identity/sign-in/android for Google setup.
     * See https://developer.apple.com/sign-in-with-apple/ for Apple setup.
     */

    // AuthViewModel for propagating OAuth results to the state machine
    private val authViewModel: AuthViewModel by inject()

    // OAuth Providers (will be initialized in setupOAuthProviders)
    private var googleProvider: GoogleSignInProvider? = null
    private var appleProvider: AppleSignInProvider? = null

    // Google Sign-In Client
    private var googleSignInClient: com.google.android.gms.auth.api.signin.GoogleSignInClient? = null

    // Store Apple OAuth state parameter for CSRF validation
    private var appleOAuthState: String? = null

    // Activity Result Launcher for Google Sign-In
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("MainActivity", "Google Sign-In result received")
        lifecycleScope.launch {
            try {
                val authService = createAuthService()
                val authResult = authService.handleGoogleSignInResult(result.data)
                handleAuthResult(authResult)
            } catch (e: Exception) {
                Log.e("MainActivity", "Error handling Google Sign-In result", e)
                showToast("Erreur lors de la connexion Google: ${e.message}")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate called")

        try {
            enableEdgeToEdge()
            Log.d("MainActivity", "enableEdgeToEdge completed")
        } catch (e: Exception) {
            Log.e("MainActivity", "enableEdgeToEdge failed", e)
        }

        // Initialize Koin with Android context BEFORE setContent
        initializeKoinWithContext()

        // Setup OAuth providers
        setupOAuthProviders()

        setContent {
            Log.d("MainActivity", "setContent called")
            // Provide AuthCallbacks to the composition tree
            CompositionLocalProvider(LocalAuthCallbacks provides this) {
                App()
            }
        }

        // Check for existing session after content is set
        checkExistingSession()
    }
    
    // ========================================================================
    // AuthCallbacks Implementation
    // ========================================================================
    
    /**
     * Launch Google Sign-In flow.
     * 
     * Implements AuthCallbacks.launchGoogleSignIn().
     * This creates the GoogleSignInClient and launches the sign-in intent.
     */
    override fun launchGoogleSignIn() {
        Log.d("MainActivity", "launchGoogleSignIn() called via AuthCallbacks")
        onGoogleSignInClick()
    }
    
    /**
     * Launch Apple Sign-In flow (web-based on Android).
     * 
     * Implements AuthCallbacks.launchAppleSignIn().
     * Note: Apple Sign-In is not natively available on Android.
     */
    override fun launchAppleSignIn() {
        Log.d("MainActivity", "launchAppleSignIn() called via AuthCallbacks")
        onAppleSignInClick()
    }

    /**
     * Setup OAuth providers for Google and Apple sign-in.
     *
     * This method initializes the OAuth providers that will be used by the AuthService.
     * OAuth configuration is now read from BuildConfig (populated from local.properties).
     *
     * TODO: Integrate with Koin DI for provider injection
     */
    private fun setupOAuthProviders() {
        try {
            // Initialize Google Sign-In Provider
            googleProvider = GoogleSignInProvider()

            // Initialize Apple Sign-In Provider (Web Flow)
            appleProvider = AppleSignInWebFlow()

            Log.d("MainActivity", "OAuth providers initialized: Google=${googleProvider != null}, Apple=${appleProvider != null}")
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to setup OAuth providers", e)
            // Don't crash the app if OAuth setup fails - we can still run in guest mode
        }
    }

    /**
     * Check for existing authentication session.
     *
     * This method checks if there's a stored authentication token and restores the session.
     * The actual session restoration is handled by AuthStateManager in App.kt.
     */
    private fun checkExistingSession() {
        lifecycleScope.launch {
            try {
                val secureStorage = AndroidSecureTokenStorage(this@MainActivity)

                // Check if we have a stored token
                val storedToken = secureStorage.getAccessToken()
                if (storedToken != null) {
                    Log.d("MainActivity", "Found existing session token - AuthStateManager will handle restoration")
                } else {
                    Log.d("MainActivity", "No existing session found")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error checking existing session", e)
            }
        }
    }

    /**
     * Creates an AuthService with OAuth providers configured.
     *
     * This is a helper method to create an AuthService instance with the OAuth providers
     * that were initialized in setupOAuthProviders().
     *
     * @return AndroidAuthService instance with OAuth providers
     */
    private fun createAuthService(): com.guyghost.wakeve.auth.shell.services.AuthService {
        val authService = com.guyghost.wakeve.auth.shell.services.AuthService()

        googleProvider?.let { provider ->
            authService.setOAuthProvider(provider)
            Log.d("MainActivity", "Google OAuth provider set on AuthService")
        }

        appleProvider?.let { provider ->
            authService.setAppleSignInProvider(provider)
            Log.d("MainActivity", "Apple OAuth provider set on AuthService")
        }

        return authService
    }

    /**
     * Handle Google Sign-In click from AuthScreen.
     *
     * This method triggers the Google Sign-In flow using the GoogleSignInClient.
     * In the current placeholder implementation, it shows a toast message.
     *
     * TODO: Replace with actual OAuth integration:
     * - Create GoogleSignInClient using createGoogleSignInClient()
     * - Get sign-in intent using getGoogleSignInIntent()
     * - Launch the intent using googleSignInLauncher
     */
    fun onGoogleSignInClick() {
        Log.d("MainActivity", "Google Sign-In clicked")

        lifecycleScope.launch {
            try {
                val authService = createAuthService()

                // Create Google Sign-In Client
                googleSignInClient = authService.createGoogleSignInClient(
                    this@MainActivity,
                    com.guyghost.wakeve.BuildConfig.GOOGLE_WEB_CLIENT_ID
                )

                if (googleSignInClient != null) {
                    // Get sign-in intent and launch it
                    val signInIntent = authService.getGoogleSignInIntent(googleSignInClient!!)
                    if (signInIntent != null) {
                        googleSignInLauncher.launch(signInIntent)
                    } else {
                        showToast("Impossible de lancer Google Sign-In")
                    }
                } else {
                    showToast("Google Sign-In non configuré - OAuth provider non initialisé")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error launching Google Sign-In", e)
                showToast("Erreur: ${e.message}")
            }
        }
    }

    /**
     * Handle Apple Sign-In click from AuthScreen.
     *
     * This method triggers the Apple Sign-In web flow using OAuth 2.0.
     * Opens a Chrome Custom Tab with the Apple authorization page.
     *
     * Implementation steps:
     * 1. Generate secure state string to prevent CSRF
     * 2. Get Apple auth URL using getAppleAuthUrl()
     * 3. Open URL in Chrome Custom Tab
     * 4. Handle callback in onNewIntent() via deep link
     */
    fun onAppleSignInClick() {
        Log.d("MainActivity", "Apple Sign-In clicked")

        lifecycleScope.launch {
            try {
                val authService = createAuthService()

                // Generate secure random state string for CSRF protection
                appleOAuthState = generateSecureState()

                // Get Apple authorization URL
                val authUrl = authService.getAppleAuthUrl(
                    clientId = com.guyghost.wakeve.BuildConfig.APPLE_CLIENT_ID,
                    redirectUri = com.guyghost.wakeve.BuildConfig.APPLE_REDIRECT_URI,
                    state = appleOAuthState!!
                )

                if (authUrl != null) {
                    // Open URL in Chrome Custom Tab
                    openCustomTab(authUrl)
                } else {
                    showToast("Apple Sign-In non configuré - OAuth provider non initialisé")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error launching Apple Sign-In", e)
                showToast("Erreur: ${e.message}")
            }
        }
    }

    /**
     * Opens a URL in a Chrome Custom Tab.
     *
     * Custom Tabs provide a better user experience than external browsers:
     * - Faster loading (Chrome pre-fetching)
     * - Seamless transition back to the app
     * - Shared cookies with Chrome browser
     *
     * @param url The URL to open
     */
    private fun openCustomTab(url: String) {
        try {
            val builder = CustomTabsIntent.Builder()
            builder.setShowTitle(true)
            builder.setToolbarColor(android.graphics.Color.parseColor("#FFFFFF"))

            val customTabsIntent = builder.build()
            customTabsIntent.launchUrl(this, Uri.parse(url))

            Log.d("MainActivity", "Opened Custom Tab with URL: $url")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error opening Custom Tab", e)
            // Fallback: Open in default browser
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(browserIntent)
        }
    }

    /**
     * Handle Apple Sign-In callback from deep link.
     *
     * This method is called when the app receives an intent with an Apple Sign-In callback
     * via deep linking (wakeve://apple-auth-callback).
     *
     * @param intent The intent received from the OS
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d("MainActivity", "onNewIntent called")

        // Handle Apple Sign-In callback (deep link)
        intent.data?.let { uri ->
            if (uri.scheme == "wakeve" && uri.host == "apple-auth-callback") {
                Log.d("MainActivity", "Apple Sign-In callback received: $uri")
                handleAppleAuthCallback(uri)
            }
        }
    }

    /**
     * Handle Apple Sign-In OAuth callback.
     *
     * This method processes the authorization code received from Apple and exchanges it
     * for access and ID tokens.
     *
     * @param uri The callback URI containing the authorization code and state
     */
    private fun handleAppleAuthCallback(uri: Uri) {
        lifecycleScope.launch {
            try {
                val code = uri.getQueryParameter("code")
                val state = uri.getQueryParameter("state")
                val error = uri.getQueryParameter("error")
                val user = uri.getQueryParameter("user") // JSON string for Apple

                when {
                    error != null -> {
                        // User denied or error occurred
                        val errorDesc = uri.getQueryParameter("error_description") ?: error
                        Log.e("MainActivity", "Apple Sign-In error: $error - $errorDesc")
                        authViewModel.handleOAuthError("Erreur Apple Sign-In: $errorDesc")
                    }

                    code == null -> {
                        // No authorization code
                        Log.e("MainActivity", "Apple Sign-In callback missing authorization code")
                        authViewModel.handleOAuthError("Erreur: Code d'autorisation manquant")
                    }

                    state != appleOAuthState -> {
                        // State mismatch - possible CSRF attack
                        Log.e("MainActivity", "Apple Sign-In state mismatch: expected $appleOAuthState, got $state")
                        authViewModel.handleOAuthError("Erreur: État de sécurité invalide")
                    }

                    else -> {
                        // Process authorization code
                        val authService = createAuthService()

                        // Exchange code for tokens
                        val authResult = authService.handleAppleAuthCallback(
                            code = code,
                            clientId = com.guyghost.wakeve.BuildConfig.APPLE_CLIENT_ID,
                            clientSecret = null, // Using backend proxy (no client secret needed on client)
                            redirectUri = com.guyghost.wakeve.BuildConfig.APPLE_REDIRECT_URI
                        )

                        handleAuthResult(authResult)
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error handling Apple auth callback", e)
                authViewModel.handleOAuthError("Erreur lors de la connexion Apple: ${e.message}")
            }
        }
    }

    /**
     * Handle authentication result from Google or Apple sign-in.
     *
     * This method processes the AuthResult and updates the app state accordingly.
     * The result is propagated to AuthViewModel, which will update the StateMachine
     * and trigger navigation.
     *
     * @param authResult The authentication result from the OAuth provider
     */
    private fun handleAuthResult(authResult: com.guyghost.wakeve.auth.core.models.AuthResult) {
        when (authResult) {
            is com.guyghost.wakeve.auth.core.models.AuthResult.Success -> {
                val user = authResult.user
                val token = authResult.token

                Log.d("MainActivity", "Authentication successful: ${user.displayName} (${user.email})")

                // Propagate result to AuthViewModel
                // This will update the StateMachine and trigger navigation
                authViewModel.handleOAuthSuccess(user, token)

                showToast("Connexion réussie!")
            }

            is com.guyghost.wakeve.auth.core.models.AuthResult.Guest -> {
                val guestUser = authResult.guestUser
                Log.d("MainActivity", "Guest mode activated: ${guestUser.displayName}")
                // Guest mode is handled by AuthViewModel when user taps "Skip"
                showToast("Mode invité activé")
            }

            is com.guyghost.wakeve.auth.core.models.AuthResult.Error -> {
                val error = authResult.error
                Log.e("MainActivity", "Authentication error: ${error::class.simpleName}")

                val errorMessage = when (error) {
                    is com.guyghost.wakeve.auth.core.models.AuthError.OAuthCancelled -> {
                        "Connexion annulée"
                    }
                    is com.guyghost.wakeve.auth.core.models.AuthError.OAuthError -> {
                        "Erreur OAuth: ${error.message}"
                    }
                    is com.guyghost.wakeve.auth.core.models.AuthError.NetworkError -> {
                        "Erreur réseau: Vérifiez votre connexion"
                    }
                    else -> {
                        "Erreur d'authentification"
                    }
                }

                // Propagate error to AuthViewModel
                authViewModel.handleOAuthError(errorMessage)
            }
        }
    }

    /**
     * Generate a cryptographically secure random state string for OAuth flows.
     *
     * This is used to prevent CSRF (Cross-Site Request Forgery) attacks.
     *
     * @return Secure random string (16 bytes, hex encoded)
     */
    private fun generateSecureState(): String {
        val bytes = ByteArray(16)
        java.security.SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Show a toast message to the user.
     *
     * @param message The message to display
     */
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    /**
     * Initialize Koin with Android context for platform-specific dependencies.
     *
     * This enables:
     * - SQLite database via AndroidSqliteDriver
     * - DatabaseEventRepository for persistent storage
     * - State Machine connected to real data
     */
    private fun initializeKoinWithContext() {
        // Check if Koin is already started (e.g., from a previous Activity instance)
        if (GlobalContext.getOrNull() != null) {
            Log.d("MainActivity", "Koin already initialized, skipping")
            return
        }

        try {
            startKoin {
                // Android logger for debugging
                androidLogger(Level.INFO)
                
                // Provide Android context for platform dependencies
                androidContext(this@MainActivity)
                
                // Load modules: common + platform-specific
                modules(appModule, platformModule())
            }
            Log.d("MainActivity", "Koin initialized successfully with database repository")
        } catch (e: Exception) {
            Log.e("MainActivity", "Koin initialization failed", e)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
