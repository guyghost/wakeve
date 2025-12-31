package com.guyghost.wakeve

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.guyghost.wakeve.auth.AndroidAuthenticationService
import com.guyghost.wakeve.auth.AuthState
import com.guyghost.wakeve.auth.AuthStateManager
import com.guyghost.wakeve.navigation.Screen
import com.guyghost.wakeve.navigation.WakevBottomBar
import com.guyghost.wakeve.navigation.WakevNavHost
import com.guyghost.wakeve.security.AndroidSecureTokenStorage
import kotlinx.coroutines.launch

// SharedPreferences constants
private const val PREFS_NAME = "wakeve_prefs"
private const val HAS_COMPLETED_ONBOARDING = "has_completed_onboarding"

/**
 * Get SharedPreferences instance.
 */
fun getSharedPreferences(context: Context): SharedPreferences {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}

/**
 * Check if user has completed onboarding.
 */
fun hasCompletedOnboarding(context: Context): Boolean {
    return getSharedPreferences(context).getBoolean(HAS_COMPLETED_ONBOARDING, false)
}

/**
 * Mark onboarding as complete.
 */
fun markOnboardingComplete(context: Context) {
    getSharedPreferences(context).edit().putBoolean(HAS_COMPLETED_ONBOARDING, true).apply()
}

/**
 * Main App composable using Jetpack Compose Navigation.
 * 
 * Architecture:
 * - NavController for navigation management
 * - Scaffold with Bottom Navigation Bar
 * - WakevNavHost for all navigation destinations
 * 
 * Flow:
 * 1. Splash → GetStarted → Login → Onboarding → Home (with bottom nav)
 * 2. Bottom Navigation: Home | Events | Explore | Profile
 */
@Composable
@Preview
fun App() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Authentication state
    var authState by remember { mutableStateOf<AuthState>(AuthState.Loading) }
    var userId by remember { mutableStateOf<String?>(null) }
    var hasOnboarded by remember { mutableStateOf(false) }
    
    // Navigation controller
    val navController = rememberNavController()
    
    // Initialize authentication and check onboarding status
    LaunchedEffect(Unit) {
        // Check onboarding status
        hasOnboarded = hasCompletedOnboarding(context)
        
        // Initialize authentication
        val authService = AndroidAuthenticationService(context)
        val authStateManager = AuthStateManager(
            secureStorage = AndroidSecureTokenStorage(context),
            authService = authService,
            enableOAuth = false // TODO: Enable OAuth when configured
        )
        authStateManager.initialize()
        
        // Observe authentication state
        authStateManager.authState.collect { state ->
            authState = state
            when (state) {
                is AuthState.Authenticated -> {
                    userId = state.userId
                }
                is AuthState.Unauthenticated -> {
                    userId = null
                }
                else -> {
                    // Loading or Error
                }
            }
        }
    }
    
    // Determine start destination based on auth and onboarding state
    val startDestination = remember(authState, hasOnboarded) {
        when {
            authState is AuthState.Loading -> Screen.Splash.route
            authState is AuthState.Unauthenticated -> Screen.GetStarted.route
            authState is AuthState.Authenticated && !hasOnboarded -> Screen.Onboarding.route
            authState is AuthState.Authenticated && hasOnboarded -> Screen.Home.route
            else -> Screen.Splash.route
        }
    }
    
    // Determine if bottom bar should be visible
    val currentRoute = navController.currentBackStackEntryFlow.collectAsState(initial = null)
    val showBottomBar = remember(currentRoute.value?.destination?.route) {
        currentRoute.value?.destination?.route in listOf(
            Screen.Home.route,
            Screen.Inbox.route,
            Screen.Explore.route,
            Screen.Profile.route
        )
    }
    
    MaterialTheme {
        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    WakevBottomBar(navController = navController)
                }
            }
        ) { paddingValues ->
            WakevNavHost(
                navController = navController,
                modifier = androidx.compose.ui.Modifier.padding(paddingValues),
                startDestination = startDestination,
                userId = userId ?: ""
            )
        }
    }
}

/**
 * App preview for Android Studio.
 */
@Composable
fun AppPreview() {
    MaterialTheme {
        App()
    }
}
