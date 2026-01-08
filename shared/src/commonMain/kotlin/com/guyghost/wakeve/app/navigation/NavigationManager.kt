package com.guyghost.wakeve.app.navigation

import com.guyghost.wakeve.app.AppState
import com.guyghost.wakeve.app.AppStateManager
import com.guyghost.wakeve.app.NavigationEvent
import com.guyghost.wakeve.auth.shell.statemachine.AuthContract
import com.guyghost.wakeve.auth.shell.statemachine.AuthStateMachine

/**
 * Navigation Manager that coordinates navigation based on authentication state.
 * 
 * This class:
 * - Integrates with AuthStateMachine to receive navigation side effects
 * - Maintains the navigation state for the application
 * - Provides navigation routes for all screens
 * 
 * Architecture: Imperative Shell (handles I/O and navigation)
 */
class NavigationManager(
    private val appStateManager: AppStateManager
) {
    private var _currentRoute: NavigationRoute = NavigationRoute.Splash
    private var _pendingNavigationEvent: NavigationEvent? = null
    
    /**
     * The current navigation route.
     */
    val currentRoute: NavigationRoute
        get() = _currentRoute
    
    /**
     * The pending navigation event to process.
     */
    val pendingNavigationEvent: NavigationEvent?
        get() = _pendingNavigationEvent
    
    /**
     * Processes a side effect from the AuthStateMachine.
     * 
     * @param sideEffect The side effect to process
     * @return The navigation event to trigger, if any
     */
    fun processAuthSideEffect(sideEffect: AuthContract.SideEffect): NavigationEvent? {
        val navigationEvent = appStateManager.handleNavigationEvent(sideEffect)
        
        if (navigationEvent != null) {
            _pendingNavigationEvent = navigationEvent
            updateRouteFromEvent(navigationEvent)
        }
        
        return navigationEvent
    }
    
    /**
     * Updates the navigation route based on the navigation event.
     * 
     * @param event The navigation event
     */
    private fun updateRouteFromEvent(event: NavigationEvent) {
        _currentRoute = when (event) {
            is NavigationEvent.NavigateToHome -> NavigationRoute.Home
            is NavigationEvent.NavigateToOnboarding -> NavigationRoute.Onboarding
            is NavigationEvent.NavigateToAuth -> NavigationRoute.Auth
            is NavigationEvent.NavigateToEvent -> NavigationRoute.EventDetail(event.eventId)
            is NavigationEvent.ShowError -> _currentRoute // Stay on current route for errors
        }
    }
    
    /**
     * Clears the pending navigation event after it's been handled.
     */
    fun clearPendingNavigationEvent() {
        _pendingNavigationEvent = null
    }
    
    /**
     * Navigates to a specific route.
     * 
     * @param route The route to navigate to
     */
    fun navigateTo(route: NavigationRoute) {
        _currentRoute = route
        _pendingNavigationEvent = null
    }
    
    /**
     * Gets the route for a screen based on the current app state.
     * 
     * @param appState The current application state
     * @return The appropriate route for the current state
     */
    fun getRouteForState(appState: AppState): NavigationRoute {
        return when {
            // If no user session, go to auth
            appState.currentUser == null -> NavigationRoute.Auth
            
            // If guest user, go to onboarding
            appState.isGuest -> NavigationRoute.Onboarding
            
            // If authenticated user, go to home
            appState.isAuthenticated -> NavigationRoute.Home
            
            // Default fallback
            else -> NavigationRoute.Auth
        }
    }
}

/**
 * Represents a navigation route in the application.
 */
sealed class NavigationRoute {
    /**
     * Splash screen shown at app launch.
     */
    data object Splash : NavigationRoute()
    
    /**
     * Authentication screen (Google/Apple/Email or Skip).
     */
    data object Auth : NavigationRoute()
    
    /**
     * Onboarding flow for new users.
     */
    data object Onboarding : NavigationRoute()
    
    /**
     * Main home screen with event list.
     */
    data object Home : NavigationRoute()
    
    /**
     * Event detail screen.
     * 
     * @property eventId The ID of the event to display
     */
    data class EventDetail(val eventId: String) : NavigationRoute()
    
    /**
     * Event creation wizard.
     */
    data object EventCreation : NavigationRoute()
    
    /**
     * Poll voting screen.
     * 
     * @property eventId The ID of the event
     */
    data class PollVoting(val eventId: String) : NavigationRoute()
    
    /**
     * Settings screen.
     */
    data object Settings : NavigationRoute()
    
    /**
     * User profile screen.
     */
    data object Profile : NavigationRoute()
    
    /**
     * Returns the route string for navigation.
     */
    val routeString: String
        get() = when (this) {
            is Splash -> "splash"
            is Auth -> "auth"
            is Onboarding -> "onboarding"
            is Home -> "home"
            is EventDetail -> "event/$eventId"
            is EventCreation -> "event_creation"
            is PollVoting -> "event/$eventId/poll/vote"
            is Settings -> "settings"
            is Profile -> "profile"
        }
}

/**
 * Navigation routes specifically for the Auth flow.
 */
object AuthRoutes {
    /**
     * Main authentication screen.
     */
    const val AUTH = "/auth"
    
    /**
     * Email authentication screen.
     */
    const val AUTH_EMAIL = "/auth/email"
    
    /**
     * Google OAuth callback route.
     */
    const val AUTH_GOOGLE = "/auth/google"
    
    /**
     * Apple OAuth callback route.
     */
    const val AUTH_APPLE = "/auth/apple"
    
    /**
     * Email OTP request route.
     */
    const val AUTH_EMAIL_REQUEST = "/auth/email/request"
    
    /**
     * Email OTP verification route.
     */
    const val AUTH_EMAIL_VERIFY = "/auth/email/verify"
    
    /**
     * Guest session creation route.
     */
    const val AUTH_GUEST = "/auth/guest"
    
    /**
     * Token refresh route.
     */
    const val AUTH_REFRESH = "/auth/refresh"
}
