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

    private val _pendingInviteCode = MutableStateFlow<String?>(null)
    val pendingInviteCode: StateFlow<String?> = _pendingInviteCode.asStateFlow()

    /**
     * Update the pending deep link.
     *
     * Call this from MainActivity's onNewIntent() or onCreate() when a deep link is received.
     * Unsupported links are rejected before they enter app-level navigation state.
     *
     * @param uri The deep link URI to handle
     * @return true when the URI was accepted for later handling, false when it was rejected
     */
    fun updatePendingDeepLink(uri: Uri): Boolean {
        val isSupported = runCatching {
            if (hasUnsupportedDeepLinkUriComponents(uri)) {
                return@runCatching false
            }

            isSupportedPendingDeepLink(
                scheme = uri.scheme,
                host = uri.host,
                pathSegments = uri.pathSegments,
                queryParameters = uri.queryParameterNames.associateWith { name ->
                    uri.getQueryParameter(name).orEmpty()
                }
            )
        }.getOrDefault(false)

        return if (isSupported) {
            _pendingDeepLink.value = uri
            true
        } else {
            false
        }
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

    /**
     * Store an invitation code extracted from a deep link until the app can resolve and accept it.
     */
    fun updatePendingInviteCode(code: String): Boolean {
        val normalizedCode = normalizeDeepLinkPathSegment(code)
        _pendingInviteCode.value = normalizedCode
        return normalizedCode != null
    }

    /**
     * Clear the pending invitation code after it has been resolved, accepted, or explicitly dismissed.
     */
    fun clearPendingInviteCode() {
        _pendingInviteCode.value = null
    }

    /**
     * Check if there's a pending invitation code waiting for app-level processing.
     */
    fun hasPendingInviteCode(): Boolean {
        return _pendingInviteCode.value != null
    }
}

internal fun isSupportedPendingDeepLink(
    scheme: String?,
    host: String?,
    pathSegments: List<String>,
    queryParameters: Map<String, String> = emptyMap()
): Boolean {
    return parseDeepLinkParts(
        scheme = scheme,
        host = host,
        pathSegments = pathSegments,
        queryParameters = queryParameters
    ) != null
}
