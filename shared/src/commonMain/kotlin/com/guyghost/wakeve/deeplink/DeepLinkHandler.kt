package com.guyghost.wakeve.deeplink

/**
 * Interface for handling deep links in a platform-agnostic way.
 *
 * This interface uses the expect/actual pattern to allow platform-specific
 * implementations for Android and iOS while sharing common logic.
 */
expect class DeepLinkHandler {
    /**
     * Handles a deep link by navigating to the appropriate screen.
     *
     * @param deepLink The deep link to handle
     * @return True if the deep link was handled successfully, false otherwise
     */
    fun handleDeepLink(deepLink: DeepLink): Boolean

    /**
     * Checks if this handler can handle the given URI.
     *
     * @param uri The URI string to check
     * @return True if the URI can be handled
     */
    fun canHandle(uri: String): Boolean

    /**
     * Registers a handler for a specific deep link route.
     *
     * @param route The route to handle
     * @param handler The handler function that will be called when the route is matched
     */
    fun registerHandler(route: DeepLinkRoute, handler: (DeepLink) -> Unit)

    /**
     * Unregisters a handler for a specific deep link route.
     *
     * @param route The route to unregister
     */
    fun unregisterHandler(route: DeepLinkRoute)

    /**
     * Gets all registered handlers.
     *
     * @return Map of routes to their handlers
     */
    fun getRegisteredHandlers(): Map<DeepLinkRoute, (DeepLink) -> Unit>

    /**
     * Clears all registered handlers.
     */
    fun clearHandlers()
}

/**
 * Common interface that both expect and actual implementations should conform to.
 * This allows for shared logic and testing.
 */
interface DeepLinkHandlerInterface {
    fun handleDeepLink(deepLink: DeepLink): Boolean
    fun canHandle(uri: String): Boolean
    fun registerHandler(route: DeepLinkRoute, handler: (DeepLink) -> Unit)
    fun unregisterHandler(route: DeepLinkRoute)
    fun getRegisteredHandlers(): Map<DeepLinkRoute, (DeepLink) -> Unit>
    fun clearHandlers()
}

/**
 * Base implementation of deep link handling logic that can be used by both platforms.
 */
abstract class BaseDeepLinkHandler : DeepLinkHandlerInterface {
    protected val handlers = mutableMapOf<DeepLinkRoute, (DeepLink) -> Unit>()

    override fun canHandle(uri: String): Boolean {
        return try {
            val deepLink = DeepLink.parse(uri).getOrNull()
            deepLink != null && canHandleDeepLink(deepLink)
        } catch (e: Exception) {
            false
        }
    }

    override fun handleDeepLink(deepLink: DeepLink): Boolean {
        // First, try to find a registered handler
        val route = deepLink.toDeepLinkRoute()
        if (route != null) {
            val handler = handlers[route]
            if (handler != null) {
                handler(deepLink)
                return true
            }
        }

        // Fall back to default handling
        return handleDefault(deepLink)
    }

    override fun registerHandler(route: DeepLinkRoute, handler: (DeepLink) -> Unit) {
        handlers[route] = handler
    }

    override fun unregisterHandler(route: DeepLinkRoute) {
        handlers.remove(route)
    }

    override fun getRegisteredHandlers(): Map<DeepLinkRoute, (DeepLink) -> Unit> {
        return handlers.toMap()
    }

    override fun clearHandlers() {
        handlers.clear()
    }

    /**
     * Checks if this deep link can be handled (implementation-specific).
     *
     * @param deepLink The deep link to check
     * @return True if it can be handled
     */
    protected abstract fun canHandleDeepLink(deepLink: DeepLink): Boolean

    /**
     * Default handling logic when no specific handler is registered.
     *
     * @param deepLink The deep link to handle
     * @return True if handled successfully
     */
    protected abstract fun handleDefault(deepLink: DeepLink): Boolean
}

/**
 * A mock implementation of DeepLinkHandler for testing purposes.
 */
class MockDeepLinkHandler : BaseDeepLinkHandler() {
    private val handledDeepLinks = mutableListOf<DeepLink>()

    override fun canHandleDeepLink(deepLink: DeepLink): Boolean {
        return true
    }

    override fun handleDefault(deepLink: DeepLink): Boolean {
        handledDeepLinks.add(deepLink)
        return true
    }

    /**
     * Gets all deep links that were handled.
     *
     * @return List of handled deep links
     */
    fun getHandledDeepLinks(): List<DeepLink> = handledDeepLinks.toList()

    /**
     * Clears the list of handled deep links.
     */
    fun clearHandledDeepLinks() {
        handledDeepLinks.clear()
    }

    /**
     * Checks if a specific deep link was handled.
     *
     * @param uri The URI to check
     * @return True if the deep link was handled
     */
    fun wasHandled(uri: String): Boolean {
        return handledDeepLinks.any { it.fullUri == uri }
    }
}

/**
 * Result of handling a deep link.
 */
sealed class DeepLinkResult {
    /**
     * The deep link was handled successfully.
     *
     * @property route The route that was matched
     * @property parameters The extracted parameters
     */
    data class Success(
        val route: DeepLinkRoute,
        val parameters: Map<String, String>
    ) : DeepLinkResult()

    /**
     * The deep link could not be handled.
     *
     * @property reason The reason why it failed
     */
    data class Failure(val reason: String) : DeepLinkResult()

    /**
     * No handler was found for this deep link.
     *
     * @property deepLink The deep link that couldn't be handled
     */
    data class NoHandler(val deepLink: DeepLink) : DeepLinkResult()
}

/**
 * Processes a deep link URI and returns the result.
 *
 * @param uri The URI to process
 * @param handler The handler to use
 * @return The result of processing
 */
fun processDeepLinkUri(uri: String, handler: DeepLinkHandlerInterface): DeepLinkResult {
    val deepLink = DeepLink.parse(uri).getOrElse {
        return DeepLinkResult.Failure("Failed to parse URI: ${it.message}")
    }

    val route = deepLink.toDeepLinkRoute()
        ?: return DeepLinkResult.Failure("Unknown route: ${deepLink.route}")

    val parameters = route.extractParameters(deepLink) + deepLink.parameters

    val handled = handler.handleDeepLink(deepLink)

    return if (handled) {
        DeepLinkResult.Success(route, parameters)
    } else {
        DeepLinkResult.NoHandler(deepLink)
    }
}
