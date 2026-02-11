package com.guyghost.wakeve.deeplink

/**
 * JVM implementation of DeepLinkHandler for testing and server-side use.
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
        // In JVM, just track that we tried to handle it
        println("Handling deep link: ${deepLink.fullUri}")
        return true
    }
}
