package com.guyghost.wakeve.deeplink

import platform.Foundation.NSURL

/**
 * iOS implementation of DeepLinkHandler.
 *
 * This implementation integrates with iOS's URL handling system for deep links.
 */
actual class DeepLinkHandler : BaseDeepLinkHandler() {

    private var pendingURL: NSURL? = null

    /**
     * Creates an NSURL for the given deep link.
     *
     * @param deepLink The deep link to convert
     * @return An NSURL that can be used for navigation
     */
    fun createNSURL(deepLink: DeepLink): NSURL? {
        return NSURL.URLWithString(deepLink.fullUri)
    }

    /**
     * Gets and clears the pending URL.
     *
     * @return The pending URL or null
     */
    fun getPendingURL(): NSURL? {
        val url = pendingURL
        pendingURL = null
        return url
    }

    /**
     * Sets a pending URL to be handled later.
     *
     * @param url The URL to set as pending
     */
    fun setPendingURL(url: NSURL) {
        pendingURL = url
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
        // Store as pending URL for the view controller to handle
        pendingURL = createNSURL(deepLink)
        return true
    }

    /**
     * Handles an iOS NSURL as a deep link.
     *
     * @param url The URL to handle
     * @return True if the URL was handled as a deep link
     */
    fun handleURL(url: NSURL?): Boolean {
        if (url == null) return false

        val urlString = url.absoluteString ?: return false
        if (!urlString.startsWith("${DeepLink.WAKEVE_SCHEME}://")) return false

        val deepLink = DeepLink.parse(urlString).getOrNull() ?: return false

        return handleDeepLink(deepLink)
    }

    /**
     * Extracts deep link data from an NSURL.
     *
     * @param url The URL to extract from
     * @return The DeepLink or null if not a valid deep link URL
     */
    fun extractDeepLink(url: NSURL?): DeepLink? {
        if (url == null) return null

        val urlString = url.absoluteString ?: return null
        if (!urlString.startsWith("${DeepLink.WAKEVE_SCHEME}://")) return null

        return DeepLink.parse(urlString).getOrNull()
    }

    /**
     * Handles a universal link (HTTPS).
     *
     * @param url The universal link URL
     * @param domain The expected domain for universal links
     * @return True if the link was handled
     */
    fun handleUniversalLink(url: NSURL?, domain: String): Boolean {
        if (url == null) return false

        val urlString = url.absoluteString ?: return false
        val host = url.host ?: return false

        // Check if this is our domain
        if (!host.contains(domain)) return false

        // Convert universal link to app deep link
        val path = url.path ?: return false
        val query = url.query?.let { "?$it" } ?: ""
        val appDeepLink = "${DeepLink.WAKEVE_SCHEME}://$path$query"

        val deepLink = DeepLink.parse(appDeepLink).getOrNull() ?: return false
        return handleDeepLink(deepLink)
    }
}

/**
 * Helper object for iOS deep link handling.
 */
object IOSDeepLinkHelper {

    /**
     * Gets the URL scheme for the app.
     *
     * @return The URL scheme string
     */
    fun getURLScheme(): String = DeepLink.WAKEVE_SCHEME

    /**
     * Checks if a URL is a wakeve deep link.
     *
     * @param url The URL to check
     * @return True if it's a wakeve deep link
     */
    fun isWakeveDeepLink(url: NSURL?): Boolean {
        return url?.scheme == DeepLink.WAKEVE_SCHEME
    }

    /**
     * Gets the deep link route from a URL.
     *
     * @param url The URL to extract from
     * @return The DeepLinkRoute or null
     */
    fun getRouteFromURL(url: NSURL?): DeepLinkRoute? {
        val urlString = url?.absoluteString ?: return null
        if (!urlString.startsWith("${DeepLink.WAKEVE_SCHEME}://")) return null

        val deepLink = DeepLink.parse(urlString).getOrNull() ?: return null
        return deepLink.toDeepLinkRoute()
    }

    /**
     * Creates a user activity for a deep link (for Handoff).
     *
     * @param deepLink The deep link
     * @return Map of activity data
     */
    fun createUserActivityData(deepLink: DeepLink): Map<String, String> {
        return mapOf(
            "activityType" to "com.guyghost.wakeve.view",
            "deepLink" to deepLink.fullUri,
            "route" to (deepLink.toDeepLinkRoute()?.name ?: "unknown")
        )
    }
}
