package com.guyghost.wakeve.deeplink

import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Deep link state manager for Wakeve Android app.
 *
 * This singleton manages the current deep link that needs to be handled.
 * It acts as a bridge between MainActivity (which receives the deep link intent)
 * and the Compose UI (which has the NavController).
 *
 * Usage:
 * 1. MainActivity receives deep link → updatePendingDeepLink(uri)
 * 2. App composable observes pendingDeepLink → navigates to appropriate screen
 * 3. App composable → clearPendingDeepLink() after navigation
 */
object DeepLinkStateManager {

    private val _pendingDeepLink = MutableStateFlow<Uri?>(null)
    val pendingDeepLink: StateFlow<Uri?> = _pendingDeepLink.asStateFlow()

    /**
     * Update the pending deep link.
     *
     * Call this from MainActivity's onNewIntent() or onCreate() when a deep link is received.
     *
     * @param uri The deep link URI to handle
     */
    fun updatePendingDeepLink(uri: Uri) {
        _pendingDeepLink.value = uri
    }

    /**
     * Clear the pending deep link.
     *
     * Call this from App composable after navigating to the deep link destination.
     */
    fun clearPendingDeepLink() {
        _pendingDeepLink.value = null
    }

    /**
     * Check if there's a pending deep link.
     *
     * @return true if there's a pending deep link to handle
     */
    fun hasPendingDeepLink(): Boolean {
        return _pendingDeepLink.value != null
    }
}
