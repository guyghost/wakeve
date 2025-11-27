package com.guyghost.wakeve.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import io.micrometer.core.instrument.binder.MeterBinder
import java.util.concurrent.atomic.AtomicInteger

/**
 * Authentication metrics collector using Micrometer.
 *
 * Collects metrics for:
 * - Login attempts (success/failure)
 * - OAuth authentication (success/failure)
 * - Session creation and revocation
 * - Token refresh operations
 * - Active sessions count
 */
class AuthMetricsCollector : MeterBinder {

    // Store registry for dynamic metric creation
    private lateinit var registry: MeterRegistry

    // Counters
    private lateinit var loginSuccessCounter: Counter
    private lateinit var loginFailureCounter: Counter
    private lateinit var oauthSuccessCounter: Counter
    private lateinit var oauthFailureCounter: Counter
    private lateinit var sessionCreatedCounter: Counter
    private lateinit var sessionRevokedCounter: Counter
    private lateinit var tokenRefreshSuccessCounter: Counter
    private lateinit var tokenRefreshFailureCounter: Counter
    private lateinit var tokenBlacklistedCounter: Counter

    // Timers
    private lateinit var loginTimer: Timer
    private lateinit var oauthTimer: Timer
    private lateinit var tokenRefreshTimer: Timer

    // Gauges
    private val activeSessionsGauge = AtomicInteger(0)

    override fun bindTo(registry: MeterRegistry) {
        this.registry = registry
        // Login metrics
        loginSuccessCounter = Counter.builder("auth.login.success")
            .description("Number of successful login attempts")
            .tag("type", "password")
            .register(registry)

        loginFailureCounter = Counter.builder("auth.login.failure")
            .description("Number of failed login attempts")
            .tag("type", "password")
            .register(registry)

        loginTimer = Timer.builder("auth.login.duration")
            .description("Time taken to process login requests")
            .tag("type", "password")
            .register(registry)

        // OAuth metrics
        oauthSuccessCounter = Counter.builder("auth.oauth.success")
            .description("Number of successful OAuth authentications")
            .register(registry)

        oauthFailureCounter = Counter.builder("auth.oauth.failure")
            .description("Number of failed OAuth authentications")
            .register(registry)

        oauthTimer = Timer.builder("auth.oauth.duration")
            .description("Time taken to process OAuth authentication")
            .register(registry)

        // Session metrics
        sessionCreatedCounter = Counter.builder("auth.session.created")
            .description("Number of sessions created")
            .register(registry)

        sessionRevokedCounter = Counter.builder("auth.session.revoked")
            .description("Number of sessions revoked")
            .register(registry)

        registry.gauge("auth.session.active", activeSessionsGauge)

        // Token refresh metrics
        tokenRefreshSuccessCounter = Counter.builder("auth.token.refresh.success")
            .description("Number of successful token refreshes")
            .register(registry)

        tokenRefreshFailureCounter = Counter.builder("auth.token.refresh.failure")
            .description("Number of failed token refreshes")
            .register(registry)

        tokenRefreshTimer = Timer.builder("auth.token.refresh.duration")
            .description("Time taken to refresh tokens")
            .register(registry)

        // Blacklist metrics
        tokenBlacklistedCounter = Counter.builder("auth.token.blacklisted")
            .description("Number of tokens added to blacklist")
            .register(registry)
    }

    // Login metrics methods
    fun recordLoginSuccess() {
        loginSuccessCounter.increment()
    }

    fun recordLoginFailure() {
        loginFailureCounter.increment()
    }

    fun recordLoginDuration(timeNanos: Long) {
        loginTimer.record(timeNanos, java.util.concurrent.TimeUnit.NANOSECONDS)
    }

    fun <T> recordLogin(block: () -> T): T {
        val startTime = System.nanoTime()
        return try {
            val result = block()
            recordLoginSuccess()
            result
        } catch (e: Exception) {
            recordLoginFailure()
            throw e
        } finally {
            recordLoginDuration(System.nanoTime() - startTime)
        }
    }

    // OAuth metrics methods
    fun recordOAuthSuccess(provider: String) {
        Counter.builder("auth.oauth.success")
            .description("Number of successful OAuth authentications")
            .tag("provider", provider)
            .register(registry)
            .increment()
    }

    fun recordOAuthFailure(provider: String) {
        Counter.builder("auth.oauth.failure")
            .description("Number of failed OAuth authentications")
            .tag("provider", provider)
            .register(registry)
            .increment()
    }

    fun recordOAuthDuration(timeNanos: Long) {
        oauthTimer.record(timeNanos, java.util.concurrent.TimeUnit.NANOSECONDS)
    }

    fun <T> recordOAuth(provider: String, block: () -> T): T {
        val startTime = System.nanoTime()
        return try {
            val result = block()
            recordOAuthSuccess(provider)
            result
        } catch (e: Exception) {
            recordOAuthFailure(provider)
            throw e
        } finally {
            recordOAuthDuration(System.nanoTime() - startTime)
        }
    }

    // Session metrics methods
    fun recordSessionCreated() {
        sessionCreatedCounter.increment()
        activeSessionsGauge.incrementAndGet()
    }

    fun recordSessionRevoked() {
        sessionRevokedCounter.increment()
        activeSessionsGauge.decrementAndGet()
    }

    fun recordMultipleSessionsRevoked(count: Int) {
        repeat(count) {
            recordSessionRevoked()
        }
    }

    fun setActiveSessions(count: Int) {
        activeSessionsGauge.set(count)
    }

    // Token refresh metrics methods
    fun recordTokenRefreshSuccess() {
        tokenRefreshSuccessCounter.increment()
    }

    fun recordTokenRefreshFailure() {
        tokenRefreshFailureCounter.increment()
    }

    fun recordTokenRefreshDuration(timeNanos: Long) {
        tokenRefreshTimer.record(timeNanos, java.util.concurrent.TimeUnit.NANOSECONDS)
    }

    fun <T> recordTokenRefresh(block: () -> T): T {
        val startTime = System.nanoTime()
        return try {
            val result = block()
            recordTokenRefreshSuccess()
            result
        } catch (e: Exception) {
            recordTokenRefreshFailure()
            throw e
        } finally {
            recordTokenRefreshDuration(System.nanoTime() - startTime)
        }
    }

    // Blacklist metrics methods
    fun recordTokenBlacklisted() {
        tokenBlacklistedCounter.increment()
    }

    fun recordMultipleTokensBlacklisted(count: Int) {
        repeat(count) {
            recordTokenBlacklisted()
        }
    }
}
