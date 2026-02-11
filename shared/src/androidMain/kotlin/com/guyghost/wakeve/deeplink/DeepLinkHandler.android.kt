package com.guyghost.wakeve.deeplink

import android.content.Intent
import android.net.Uri

/**
 * Android implementation of DeepLinkHandler.
 *
 * This implementation integrates with Android's Intent system for deep link handling.
 */
actual class DeepLinkHandler : BaseDeepLinkHandler() {

    private var pendingIntent: Intent? = null

    /**
     * Creates an Android Intent for the given deep link.
     *
     * @param deepLink The deep link to convert
     * @return An Intent that can be used to navigate to the deep link destination
     */
    fun createIntent(deepLink: DeepLink): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(deepLink.fullUri)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
    }

    /**
     * Gets and clears the pending intent.
     *
     * @return The pending intent or null
     */
    fun getPendingIntent(): Intent? {
        val intent = pendingIntent
        pendingIntent = null
        return intent
    }

    /**
     * Sets a pending intent to be handled later.
     *
     * @param intent The intent to set as pending
     */
    fun setPendingIntent(intent: Intent) {
        pendingIntent = intent
    }

    override fun canHandleDeepLink(deepLink: DeepLink): Boolean {
        // Check if we have a registered handler for this route
        val route = deepLink.toDeepLinkRoute()
        if (route != null && handlers.containsKey(route)) {
            return true
        }

        // Check if the route is a known route
        return DeepLinkRoute.entries.any { it.matches(deepLink) }
    }

    override fun handleDefault(deepLink: DeepLink): Boolean {
        // Store as pending intent for the activity to handle
        pendingIntent = createIntent(deepLink)
        return true
    }

    /**
     * Handles an Android Intent as a deep link.
     *
     * @param intent The intent to handle
     * @return True if the intent was handled as a deep link
     */
    fun handleIntent(intent: Intent?): Boolean {
        if (intent == null) return false

        val data = intent.data ?: return false
        if (data.scheme != DeepLink.WAKEVE_SCHEME) return false

        val uriString = data.toString()
        val deepLink = DeepLink.parse(uriString).getOrNull() ?: return false

        return handleDeepLink(deepLink)
    }

    /**
     * Extracts deep link data from an Intent.
     *
     * @param intent The intent to extract from
     * @return The DeepLink or null if not a valid deep link intent
     */
    fun extractDeepLink(intent: Intent?): DeepLink? {
        if (intent == null) return null

        val data = intent.data ?: return null
        if (data.scheme != DeepLink.WAKEVE_SCHEME) return null

        return DeepLink.parse(data.toString()).getOrNull()
    }
}

/**
 * Helper object for Android deep link handling.
 */
object AndroidDeepLinkHelper {

    /**
     * Creates an intent filter for the wakeve scheme.
     *
     * @return Intent filter string
     */
    fun getIntentFilter(): String = DeepLink.WAKEVE_SCHEME

    /**
     * Checks if an intent is a wakeve deep link.
     *
     * @param intent The intent to check
     * @return True if it's a wakeve deep link
     */
    fun isWakeveDeepLink(intent: Intent?): Boolean {
        return intent?.data?.scheme == DeepLink.WAKEVE_SCHEME
    }

    /**
     * Gets the deep link route from an intent.
     *
     * @param intent The intent to extract from
     * @return The DeepLinkRoute or null
     */
    fun getRouteFromIntent(intent: Intent?): DeepLinkRoute? {
        val deepLink = intent?.data?.toString()?.let { uri ->
            DeepLink.parse(uri).getOrNull()
        } ?: return null

        return deepLink.toDeepLinkRoute()
    }
}
