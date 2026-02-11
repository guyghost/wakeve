package com.guyghost.wakeve

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.guyghost.wakeve.auth.shell.statemachine.AuthStateMachine
import com.guyghost.wakeve.deeplink.DeepLinkHandler
import com.guyghost.wakeve.deeplink.DeepLinkStateManager
import com.guyghost.wakeve.navigation.Screen
import com.guyghost.wakeve.navigation.WakevBottomBar
import com.guyghost.wakeve.navigation.WakevNavHost
import org.koin.compose.koinInject

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
 * - Reactive navigation based on AuthStateMachine state
 * 
 * Flow:
 * 1. Splash → GetStarted → Auth → Onboarding → Home (with bottom nav)
 * 2. Bottom Navigation: Home | Inbox | Explore | Profile
 */
@Composable
@Preview
fun App() {
    val context = LocalContext.current
    
    // Get AuthStateMachine from Koin
    val authStateMachine: AuthStateMachine = koinInject()
    val authState by authStateMachine.state.collectAsState()
    
    // Onboarding status
    var hasOnboarded by remember { mutableStateOf(false) }
    
    // Navigation controller
    val navController = rememberNavController()
    
    // Check onboarding status on first composition
    LaunchedEffect(Unit) {
        hasOnboarded = hasCompletedOnboarding(context)
    }

    // Handle deep links from DeepLinkStateManager
    val deepLinkHandler = remember { DeepLinkHandler() }

    // Collect deep links from state manager and handle them
    LaunchedEffect(Unit) {
        DeepLinkStateManager.pendingDeepLink.collect { uri ->
            if (uri != null) {
                Log.d("App", "Handling deep link from state manager: $uri")

                // Handle deep link
                val handled = deepLinkHandler.handleDeepLink(
                    uri = uri,
                    navController = navController,
                    isAuthenticated = authState.isAuthenticated || authState.isGuest
                )

                if (handled) {
                    // Clear the pending deep link after navigation
                    DeepLinkStateManager.clearPendingDeepLink()
                }
            }
        }
    }
    
    // Determine start destination based on auth state
    val startDestination = remember(authState.isAuthenticated, authState.isGuest, hasOnboarded) {
        when {
            // Loading state - show splash
            authState.isLoading -> Screen.Splash.route
            // Authenticated and has onboarded - go to home
            authState.isAuthenticated && hasOnboarded -> Screen.Home.route
            // Authenticated but not onboarded - show onboarding
            authState.isAuthenticated && !hasOnboarded -> Screen.Onboarding.route
            // Guest mode and has onboarded - go to home
            authState.isGuest && hasOnboarded -> Screen.Home.route
            // Guest mode but not onboarded - show onboarding
            authState.isGuest && !hasOnboarded -> Screen.Onboarding.route
            // Not authenticated - show get started (leads to auth)
            else -> Screen.GetStarted.route
        }
    }
    
    // Derive userId from auth state
    val userId = remember(authState) {
        authState.currentUser?.id ?: ""
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
                modifier = Modifier.padding(paddingValues),
                startDestination = startDestination,
                userId = userId
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
