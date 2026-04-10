package com.guyghost.wakeve.security

import io.ktor.server.application.*
import io.ktor.server.request.*

/**
 * Centralized security configuration for the Wakeve API.
 *
 * Provides environment-based security settings and validation helpers.
 */
object SecurityConfig {

    /**
     * Environment names
     */
    private const val ENV_PRODUCTION = "production"
    private const val ENV_STAGING = "staging"
    private const val ENV_DEVELOPMENT = "development"

    /**
     * Check if the application is running in production mode
     */
    fun isProduction(): Boolean {
        return System.getenv("ENVIRONMENT")?.lowercase() == ENV_PRODUCTION
    }

    /**
     * Check if the application is running in staging mode
     */
    fun isStaging(): Boolean {
        return System.getenv("ENVIRONMENT")?.lowercase() == ENV_STAGING
    }

    /**
     * Check if the application is running in development mode
     */
    fun isDevelopment(): Boolean {
        val env = System.getenv("ENVIRONMENT")?.lowercase()
        return env == null || env == ENV_DEVELOPMENT
    }

    /**
     * Get the JWT secret with validation
     * @throws IllegalStateException if not configured in production
     */
    fun getJwtSecret(): String {
        val secret = System.getenv("JWT_SECRET")
        return when {
            secret != null -> secret
            isProduction() -> throw IllegalStateException(
                "JWT_SECRET must be configured in production"
            )
            else -> "default-secret-key-change-in-production"
        }
    }

    /**
     * Check if metrics endpoint access is allowed from the given IP
     *
     * Uses METRICS_WHITELIST_IPS environment variable (comma-separated).
     * If not set, allows localhost only in development, denies all in production.
     */
    fun isMetricsAllowed(clientIp: String): Boolean {
        val whitelist = System.getenv("METRICS_WHITELIST_IPS")
        return when {
            // Explicit whitelist configured
            !whitelist.isNullOrBlank() -> {
                val allowedIps = whitelist.split(",").map { it.trim() }
                allowedIps.any { allowedIp ->
                    when {
                        allowedIp.contains("/") -> ipInRange(clientIp, allowedIp) // CIDR notation
                        else -> clientIp == allowedIp // Exact match
                    }
                }
            }
            // Development: allow localhost
            isDevelopment() -> {
                clientIp == "127.0.0.1" ||
                clientIp == "::1" ||
                clientIp == "localhost"
            }
            // Production: deny by default
            else -> false
        }
    }

    /**
     * Check if an IP is in a CIDR range.
     * Supports IPv4 (e.g. 10.0.0.0/8) and IPv4-mapped IPv6 (::ffff:10.0.0.1).
     */
    private fun ipInRange(ip: String, cidr: String): Boolean {
        val parts = cidr.split("/")
        if (parts.size != 2) return false

        val cidrBase = parts[0]
        val prefixLength = parts[1].toIntOrNull() ?: return false

        // Normalise IPv4-mapped IPv6 addresses (::ffff:a.b.c.d)
        val normalised = normaliseIp(ip)
        val cidrNorm = normaliseIp(cidrBase)

        // Convert both to 32-bit integers for IPv4 comparison
        val ipInt = ipv4ToInt(normalised) ?: return false
        val cidrInt = ipv4ToInt(cidrNorm) ?: return false

        val effectivePrefix = prefixLength.coerceIn(0, 32)
        val mask = if (effectivePrefix == 0) 0 else (0xFFFFFFFFL.toInt() shl (32 - effectivePrefix))

        return (ipInt and mask) == (cidrInt and mask)
    }

    /** Strip IPv4-mapped IPv6 prefix if present. */
    private fun normaliseIp(ip: String): String =
        if (ip.startsWith("::")) ip.removePrefix("::ffff:") else ip

    /** Convert a dotted-decimal IPv4 string to a 32-bit Int, or null if invalid. */
    private fun ipv4ToInt(ip: String): Int? {
        val octets = ip.split(".")
        if (octets.size != 4) return null
        return octets.fold(0) { acc, part ->
            val octet = part.toIntOrNull() ?: return null
            if (octet !in 0..255) return null
            (acc shl 8) or octet
        }
    }

    /**
     * Get allowed CORS origins
     */
    fun getAllowedOrigins(): List<String> {
        val origins = System.getenv("CORS_ALLOWED_ORIGINS")
        return if (!origins.isNullOrBlank()) {
            origins.split(",").map { it.trim() }
        } else {
            when {
                isProduction() -> listOf("https://wakeve.app")
                isStaging() -> listOf("https://staging.wakeve.app")
                else -> listOf("http://localhost:8080", "http://localhost:3000")
            }
        }
    }

    /**
     * Get rate limit configuration
     */
    data class RateLimitConfig(
        val requests: Int,
        val windowMinutes: Int
    )

    fun getRateLimitConfig(type: String): RateLimitConfig {
        return when (type) {
            "auth" -> RateLimitConfig(
                requests = System.getenv("RATE_LIMIT_AUTH_REQUESTS")?.toIntOrNull() ?: 10,
                windowMinutes = System.getenv("RATE_LIMIT_AUTH_WINDOW")?.toIntOrNull() ?: 1
            )
            "api" -> RateLimitConfig(
                requests = System.getenv("RATE_LIMIT_API_REQUESTS")?.toIntOrNull() ?: 100,
                windowMinutes = System.getenv("RATE_LIMIT_API_WINDOW")?.toIntOrNull() ?: 1
            )
            else -> RateLimitConfig(requests = 100, windowMinutes = 1)
        }
    }

    /**
     * Check if HTTPS is required
     */
    fun isHttpsRequired(): Boolean {
        return isProduction() || isStaging()
    }

    /**
     * Get session timeout in minutes
     */
    fun getSessionTimeoutMinutes(): Long {
        return System.getenv("SESSION_TIMEOUT_MINUTES")?.toLongOrNull() ?: when {
            isProduction() -> 60 // 1 hour
            isStaging() -> 120 // 2 hours
            else -> 480 // 8 hours for development
        }
    }
}

/**
 * Extension property to get client IP from ApplicationCall
 */
val ApplicationCall.clientIp: String
    get() {
        return request.headers["X-Forwarded-For"]
            ?.split(",")?.firstOrNull()?.trim()
            ?: request.headers["X-Real-IP"]
            ?: request.local.localAddress
            ?: "unknown"
    }
