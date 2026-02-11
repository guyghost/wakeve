package com.guyghost.wakeve.deeplink

/**
 * JS implementation of DeepLinkHandler for web use.
 */
actual class DeepLinkHandler : BaseDeepLinkHandler() {

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
        // In JS, we might want to navigate using window.location or a router
        console.log("Handling deep link: ${deepLink.fullUri}")
        return true
    }

    /**
     * Creates a web URL from a deep link for web sharing.
     *
     * @param deepLink The deep link
     * @param baseUrl The base web URL
     * @return The web URL
     */
    fun createWebUrl(deepLink: DeepLink, baseUrl: String = "https://wakeve.app"): String {
        return "$baseUrl/${deepLink.route}?${deepLink.parameters.entries.joinToString("&") { (k, v) -> "$k=$v" }}"
    }

    /**
     * Parses a web URL into a deep link.
     *
     * @param webUrl The web URL
     * @param baseUrl The expected base URL
     * @return The DeepLink or null
     */
    fun parseWebUrl(webUrl: String, baseUrl: String = "https://wakeve.app"): DeepLink? {
        if (!webUrl.startsWith(baseUrl)) return null

        val pathAndQuery = webUrl.removePrefix(baseUrl).trimStart('/')
        val deepLinkUri = "${DeepLink.WAKEVE_SCHEME}://$pathAndQuery"

        return DeepLink.parse(deepLinkUri).getOrNull()
    }
}
