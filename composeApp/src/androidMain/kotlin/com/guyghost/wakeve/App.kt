package com.guyghost.wakeve

import android.content.Context
import android.content.SharedPreferences
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.guyghost.wakeve.auth.AndroidAuthenticationService
import com.guyghost.wakeve.auth.AuthState
import com.guyghost.wakeve.auth.AuthStateManager
import com.guyghost.wakeve.auth.GoogleSignInHelper
import com.guyghost.wakeve.database.WakevDb
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.Vote
import com.guyghost.wakeve.sync.AndroidNetworkStatusDetector
import com.guyghost.wakeve.sync.KtorSyncHttpClient
import com.guyghost.wakeve.sync.SyncManager
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview

private const val PREFS_NAME = "wakeve_prefs"
private const val HAS_COMPLETED_ONBOARDING = "has_completed_onboarding"

fun getSharedPreferences(context: Context): SharedPreferences {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}

fun hasCompletedOnboarding(context: Context): Boolean {
    return getSharedPreferences(context).getBoolean(HAS_COMPLETED_ONBOARDING, false)
}

fun markOnboardingComplete(context: Context) {
    getSharedPreferences(context).edit().putBoolean(HAS_COMPLETED_ONBOARDING, true).apply()
}

enum class AppRoute {
    SPLASH,
    ONBOARDING,
    LOGIN,
    HOME,
    EVENT_CREATION,
    PARTICIPANT_MANAGEMENT,
    POLL_VOTING,
    POLL_RESULTS
}

@Composable
@Preview
fun App() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // App navigation state
    var currentRoute by remember { mutableStateOf(AppRoute.SPLASH) }
    var isAuthenticated by remember { mutableStateOf(false) }
    var userId by remember { mutableStateOf<String?>(null) }
    var hasOnboarded by remember { mutableStateOf(false) }
    
    // Initialize app dependencies
    LaunchedEffect(Unit) {
        // Initialize authentication
        val authService = AndroidAuthenticationService(context)
        val authStateManager = AuthStateManager(
            secureStorage = com.guyghost.wakeve.security.AndroidSecureTokenStorage(context),
            authService = authService,
            enableOAuth = false // TODO: Enable OAuth when configured
        )
        authStateManager.initialize()
        
        // Check onboarding status
        hasOnboarded = hasCompletedOnboarding(context)
        
        // Check authentication state
        authStateManager.authState.collect { state ->
            when (state) {
                is AuthState.Authenticated -> {
                    isAuthenticated = true
                    userId = state.userId
                    // Do not set currentRoute here, let splash decide
                }
                is AuthState.Unauthenticated -> {
                    isAuthenticated = false
                    userId = null
                    // Do not set currentRoute here, let splash decide
                }
                else -> {
                    // Loading or Error, stay on current route
                }
            }
        }
    }
    
    MaterialTheme {
        when (currentRoute) {
            AppRoute.SPLASH -> {
                SplashScreen(
                    onAnimationComplete = {
                        // Route to appropriate screen based on auth and onboarding state
                        currentRoute = when {
                            !isAuthenticated -> AppRoute.LOGIN
                            isAuthenticated && !hasOnboarded -> AppRoute.ONBOARDING
                            isAuthenticated && hasOnboarded -> AppRoute.HOME
                            else -> AppRoute.HOME // fallback
                        }
                    }
                )
            }
            AppRoute.ONBOARDING -> {
                OnboardingScreen(
                    onOnboardingComplete = {
                        scope.launch {
                            markOnboardingComplete(context)
                            currentRoute = AppRoute.HOME // User is authenticated after onboarding
                        }
                    }
                )
            }
            AppRoute.LOGIN -> {
                LoginScreen(
                    onGoogleSignIn = { /* TODO: Implement Google Sign-In */ },
                    onAppleSignIn = { /* Not available on Android */ }
                )
            }
            AppRoute.HOME -> {
                val database = remember { DatabaseProvider.getDatabase(com.guyghost.wakeve.AndroidDatabaseFactory(context)) }
                val eventRepository = remember { com.guyghost.wakeve.DatabaseEventRepository(database, null) }
                val events = remember { eventRepository.getAllEvents() }
                
                HomeScreen(
                    events = events,
                    userId = userId ?: "",
                    onCreateEvent = { currentRoute = AppRoute.EVENT_CREATION },
                    onEventClick = { /* TODO: Navigate to event details */ },
                    onSignOut = {
                        scope.launch {
                            val authService = AndroidAuthenticationService(context)
                            val authStateManager = AuthStateManager(
                                secureStorage = com.guyghost.wakeve.security.AndroidSecureTokenStorage(context),
                                authService = authService,
                                enableOAuth = false
                            )
                            authStateManager.logout()
                            currentRoute = AppRoute.LOGIN
                        }
                    }
                )
            }
            else -> {
                // Placeholder for other routes (will be implemented)
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Ecran en construction")
                }
            }
        }
    }
}

@Composable
fun AppPreview() {
    MaterialTheme {
        App()
    }
}
