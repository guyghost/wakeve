package com.guyghost.wakeve.app

import com.guyghost.wakeve.auth.core.models.User
import com.guyghost.wakeve.auth.shell.statemachine.AuthContract
import kotlinx.coroutines.flow.StateFlow

/**
 * Global Application State that manages authentication and navigation state.
 * 
 * This class serves as the single source of truth for the application's global state,
 * integrating authentication state from AuthStateMachine with navigation state.
 * 
 * Architecture: Functional Core & Imperative Shell
 * - The state data is immutable and pure
 * - State transitions are handled by the AuthStateMachine (imperative shell)
 * 
 * @property isAuthenticated Whether the current user is authenticated (not guest)
 * @property isGuest Whether the current user is in guest mode
 * @property currentUser The current user (null if no session)
 * @property authStateFlow StateFlow exposing the full AuthContract.State
 */
data class AppState(
    val isAuthenticated: Boolean = false,
    val isGuest: Boolean = true,
    val currentUser: User? = null,
    val authStateFlow: StateFlow<AuthContract.State>? = null
) {
    companion object {
        /**
         * Creates the initial app state before any authentication.
         */
        fun initial() = AppState()
    }
    
    /**
     * Returns the display name for the current user context.
     */
    val displayName: String
        get() = currentUser?.displayName ?: "Invité"
    
    /**
     * Returns true if the user can sync data to the cloud.
     * Only authenticated users can sync; guests are local-only.
     */
    val canSync: Boolean
        get() = currentUser?.canSync == true
    
    /**
     * Returns true if the user has completed onboarding.
     */
    val hasCompletedOnboarding: Boolean
        get() = !isGuest || currentUser != null
}

/**
 * Navigation events that can be triggered from the global app state.
 */
sealed class NavigationEvent {
    /**
     * Navigate to the main home screen.
     */
    data object NavigateToHome : NavigationEvent()
    
    /**
     * Navigate to the onboarding flow.
     */
    data object NavigateToOnboarding : NavigationEvent()
    
    /**
     * Navigate to the authentication screen.
     */
    data object NavigateToAuth : NavigationEvent()
    
    /**
     * Navigate to a specific event detail.
     */
    data class NavigateToEvent(val eventId: String) : NavigationEvent()
    
    /**
     * Show an error to the user.
     */
    data class ShowError(val message: String) : NavigationEvent()
}

/**
 * Manager for global application state and navigation.
 * 
 * This class coordinates between:
 * - AuthStateMachine for authentication flow
 * - Navigation system for screen transitions
 * - AppState for global state access
 */
class AppStateManager {
    private var _appState = AppState.initial()
    
    /**
     * The current application state.
     */
    val appState: AppState
        get() = _appState
    
    /**
     * Updates the application state with new authentication state.
     * 
     * @param authState The new authentication state from AuthStateMachine
     */
    fun updateAuthState(authState: AuthContract.State) {
        _appState = _appState.copy(
            isAuthenticated = authState.isAuthenticated,
            isGuest = authState.isGuest,
            currentUser = authState.currentUser,
            authStateFlow = null // Will be set when integrating with StateFlow
        )
    }
    
    /**
     * Handles navigation events from the authentication flow.
     * 
     * @param event The navigation event to handle
     * @return The next navigation target, or null if no navigation needed
     */
    fun handleNavigationEvent(event: AuthContract.SideEffect): NavigationEvent? {
        return when (event) {
            is AuthContract.SideEffect.NavigateToMain -> NavigationEvent.NavigateToHome
            is AuthContract.SideEffect.NavigateToOnboarding -> NavigationEvent.NavigateToOnboarding
            is AuthContract.SideEffect.ShowError -> NavigationEvent.ShowError(event.message)
            else -> null
        }
    }
    
    /**
     * Resets the application state to initial (no session).
     */
    fun reset() {
        _appState = AppState.initial()
    }
}
