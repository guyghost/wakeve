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
     * Check if an IP is in a CIDR range
     */
    private fun ipInRange(ip: String, cidr: String): Boolean {
        // Simple CIDR check for IPv4
        // TODO: Implement proper CIDR matching for both IPv4 and IPv6
        val parts = cidr.split("/")
        if (parts.size != 2) return false

        val rangeStart = parts[0]
        val prefixLength = parts[1].toIntOrNull() ?: return false

        // For now, do exact match if prefix is /32
        return if (prefixLength == 32) {
            ip == rangeStart
        } else {
            // Simple subnet match for common cases
            val ipPrefix = ip.substringBeforeLast(".")
            val rangePrefix = rangeStart.substringBeforeLast(".")
            ipPrefix == rangePrefix
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
            ?: request.origin.localHost?.host
            ?: "unknown"
    }
