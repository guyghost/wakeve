package com.guyghost.wakeve.navigation

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Interface for OAuth callbacks that require Activity context.
 * 
 * This interface is implemented by MainActivity and provided via CompositionLocal
 * to allow composables to trigger OAuth flows without direct Activity reference.
 * 
 * Architecture: Imperative Shell (handles platform-specific I/O)
 */
interface AuthCallbacks {
    /**
     * Launch Google Sign-In flow.
     * Will trigger GoogleSignInClient and return result via AuthStateMachine.
     */
    fun launchGoogleSignIn()
    
    /**
     * Launch Apple Sign-In flow (web-based on Android).
     * Opens Custom Tab with Apple OAuth URL.
     * Note: Not natively available on Android.
     */
    fun launchAppleSignIn()
}

/**
 * CompositionLocal providing AuthCallbacks from MainActivity.
 * 
 * Usage in composables:
 * ```kotlin
 * val callbacks = LocalAuthCallbacks.current
 * Button(onClick = { callbacks.launchGoogleSignIn() }) {
 *     Text("Sign in with Google")
 * }
 * ```
 * 
 * Must be provided at the root of the composition tree (in MainActivity):
 * ```kotlin
 * CompositionLocalProvider(LocalAuthCallbacks provides this) {
 *     App()
 * }
 * ```
 */
val LocalAuthCallbacks = staticCompositionLocalOf<AuthCallbacks> {
    error("No AuthCallbacks provided. Make sure to wrap your composables with CompositionLocalProvider(LocalAuthCallbacks provides authCallbacks)")
}
