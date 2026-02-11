package com.guyghost.wakeve.deeplink

import kotlinx.serialization.Serializable

/**
 * Represents a deep link within the Wakeve application.
 *
 * @property route The route path (e.g., "event/123/details")
 * @property parameters Query parameters as a map (e.g., {"tab" to "votes"})
 * @property fullUri The complete URI string (e.g., "wakeve://event/123/details?tab=votes")
 */
@Serializable
data class DeepLink(
    val route: String,
    val parameters: Map<String, String> = emptyMap(),
    val fullUri: String
) {
    companion object {
        const val WAKEVE_SCHEME = "wakeve"
        const val WAKEVE_HOST = "app"

        /**
         * Parses a URI string into a DeepLink object.
         *
         * @param uri The URI to parse (e.g., "wakeve://event/123/details?tab=votes")
         * @return Result containing the DeepLink or an exception
         */
        fun parse(uri: String): Result<DeepLink> = runCatching {
            require(uri.isNotBlank()) { "URI cannot be blank" }
            require(uri.startsWith("$WAKEVE_SCHEME://")) { "URI must start with $WAKEVE_SCHEME://" }

            // Remove scheme
            val withoutScheme = uri.removePrefix("$WAKEVE_SCHEME://")

            // Split path and query
            val (pathPart, queryPart) = when {
                withoutScheme.contains("?") -> {
                    val parts = withoutScheme.split("?", limit = 2)
                    parts[0] to parts.getOrNull(1)
                }
                else -> withoutScheme to null
            }

            // Parse parameters
            val params = queryPart?.let { parseQueryString(it) } ?: emptyMap()

            DeepLink(
                route = pathPart.trim('/'),
                parameters = params,
                fullUri = uri
            )
        }

        /**
         * Creates a DeepLink from route and parameters.
         *
         * @param route The route path
         * @param parameters Query parameters
         * @return A new DeepLink instance
         */
        fun create(
            route: String,
            parameters: Map<String, String> = emptyMap()
        ): DeepLink {
            val cleanRoute = route.trim('/')
            val queryString = if (parameters.isNotEmpty()) {
                "?" + parameters.entries.joinToString("&") { (key, value) ->
                    "${encodeURIComponent(key)}=${encodeURIComponent(value)}"
                }
            } else {
                ""
            }

            return DeepLink(
                route = cleanRoute,
                parameters = parameters,
                fullUri = "$WAKEVE_SCHEME://$cleanRoute$queryString"
            )
        }

        /**
         * Parses a query string into a map.
         */
        private fun parseQueryString(query: String): Map<String, String> {
            return query.split("&")
                .filter { it.isNotEmpty() }
                .mapNotNull { param ->
                    val parts = param.split("=", limit = 2)
                    if (parts.size == 2) {
                        decodeURIComponent(parts[0]) to decodeURIComponent(parts[1])
                    } else if (parts.size == 1) {
                        decodeURIComponent(parts[0]) to ""
                    } else null
                }
                .toMap()
        }

        /**
         * Simple URL encoding for components.
         */
        private fun encodeURIComponent(value: String): String {
            return value
                .replace("%", "%25")
                .replace("&", "%26")
                .replace("=", "%3D")
                .replace("?", "%3F")
                .replace(" ", "%20")
        }

        /**
         * Simple URL decoding for components.
         */
        private fun decodeURIComponent(value: String): String {
            return value
                .replace("%20", " ")
                .replace("%3F", "?")
                .replace("%3D", "=")
                .replace("%26", "&")
                .replace("%25", "%")
        }
    }

    /**
     * Gets a parameter value by key.
     *
     * @param key The parameter key
     * @return The parameter value or null if not found
     */
    fun getParameter(key: String): String? = parameters[key]

    /**
     * Gets a required parameter value by key.
     *
     * @param key The parameter key
     * @return The parameter value
     * @throws IllegalArgumentException if the parameter is missing
     */
    fun getRequiredParameter(key: String): String {
        return parameters[key] ?: throw IllegalArgumentException("Required parameter '$key' is missing")
    }

    /**
     * Checks if this deep link matches a specific route pattern.
     *
     * @param pattern The route pattern (e.g., "event/{id}/details")
     * @return True if the route matches the pattern
     */
    fun matchesPattern(pattern: String): Boolean {
        val patternParts = pattern.trim('/').split("/")
        val routeParts = route.split("/")

        if (patternParts.size != routeParts.size) return false

        return patternParts.zip(routeParts).all { (patternPart, routePart) ->
            patternPart.startsWith("{") && patternPart.endsWith("}") || patternPart == routePart
        }
    }

    /**
     * Extracts path parameters from a route pattern.
     *
     * @param pattern The route pattern (e.g., "event/{id}/details")
     * @return Map of parameter names to values
     */
    fun extractPathParameters(pattern: String): Map<String, String> {
        val patternParts = pattern.trim('/').split("/")
        val routeParts = route.split("/")

        if (patternParts.size != routeParts.size) return emptyMap()

        return patternParts.zip(routeParts)
            .filter { (patternPart, _) ->
                patternPart.startsWith("{") && patternPart.endsWith("}")
            }
            .map { (patternPart, routePart) ->
                val paramName = patternPart.substring(1, patternPart.length - 1)
                paramName to routePart
            }
            .toMap()
    }

    /**
     * Returns the DeepLinkRoute enum value if the route matches a known route.
     *
     * @return The matching DeepLinkRoute or null
     */
    fun toDeepLinkRoute(): DeepLinkRoute? {
        return DeepLinkRoute.entries.find { it.matches(this) }
    }
}

/**
 * Enum representing all known deep link routes in the Wakeve application.
 */
enum class DeepLinkRoute(
    val pattern: String,
    val description: String
) {
    // Event Routes
    EVENT_DETAILS(
        pattern = "event/{eventId}/details",
        description = "Event details screen"
    ),
    EVENT_POLL(
        pattern = "event/{eventId}/poll",
        description = "Event poll voting screen"
    ),
    EVENT_SCENARIOS(
        pattern = "event/{eventId}/scenarios",
        description = "Event scenarios comparison screen"
    ),
    EVENT_MEETINGS(
        pattern = "event/{eventId}/meetings",
        description = "Event meetings screen"
    ),

    // User Routes
    PROFILE(
        pattern = "profile",
        description = "User profile screen"
    ),
    SETTINGS(
        pattern = "settings",
        description = "App settings screen"
    ),
    NOTIFICATIONS(
        pattern = "notifications",
        description = "Notifications list screen"
    ),

    // Home
    HOME(
        pattern = "home",
        description = "Home screen with event list"
    );

    /**
     * Checks if a deep link matches this route pattern.
     *
     * @param deepLink The deep link to check
     * @return True if the deep link matches this route
     */
    fun matches(deepLink: DeepLink): Boolean {
        return deepLink.matchesPattern(pattern)
    }

    /**
     * Extracts parameters from a matching deep link.
     *
     * @param deepLink The deep link to extract from
     * @return Map of parameter names to values
     */
    fun extractParameters(deepLink: DeepLink): Map<String, String> {
        return deepLink.extractPathParameters(pattern)
    }

    /**
     * Creates a deep link URI for this route with the given parameters.
     *
     * @param pathParams Path parameters (e.g., {"eventId" to "123"})
     * @param queryParams Query parameters (e.g., {"tab" to "votes"})
     * @return The full deep link URI
     */
    fun createUri(
        pathParams: Map<String, String> = emptyMap(),
        queryParams: Map<String, String> = emptyMap()
    ): String {
        var resolvedPath = pattern
        pathParams.forEach { (key, value) ->
            resolvedPath = resolvedPath.replace("{$key}", value)
        }

        val queryString = if (queryParams.isNotEmpty()) {
            "?" + queryParams.entries.joinToString("&") { (key, value) ->
                "${encodeQueryComponent(key)}=${encodeQueryComponent(value)}"
            }
        } else {
            ""
        }

        return "${DeepLink.WAKEVE_SCHEME}://$resolvedPath$queryString"
    }

    companion object {
        /**
         * Finds a route by its pattern.
         *
         * @param pattern The route pattern to find
         * @return The matching DeepLinkRoute or null
         */
        fun fromPattern(pattern: String): DeepLinkRoute? {
            return entries.find { it.pattern == pattern }
        }

        /**
         * Parses a URI and returns the matching route.
         *
         * @param uri The URI to parse
         * @return The matching DeepLinkRoute or null
         */
        fun fromUri(uri: String): DeepLinkRoute? {
            return DeepLink.parse(uri).getOrNull()?.toDeepLinkRoute()
        }

        private fun encodeQueryComponent(value: String): String {
            return value
                .replace("%", "%25")
                .replace("&", "%26")
                .replace("=", "%3D")
                .replace(" ", "%20")
        }
    }
}
