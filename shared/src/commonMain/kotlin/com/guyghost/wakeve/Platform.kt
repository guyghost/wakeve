package com.guyghost.wakeve

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
expect fun getCurrentTimeMillis(): Long
expect fun getCurrentTimeNanos(): Long
expect fun getNotificationService(): NotificationService
expect fun measureMemoryUsageMB(): Double?
expect fun getMaxMemoryMB(): Double?

/**
 * Gets the current time as a kotlinx.datetime.Instant.
 * Pure function, uses kotlinx-datetime.
 */
fun getCurrentInstant(): Instant = Clock.System.now()

/**
 * Formats an ISO timestamp string.
 * Pure function, uses kotlinx-datetime.
 */
fun getCurrentIsoTimestamp(): String = Clock.System.now().toString()

/**
 * Calculates duration in milliseconds from start to end.
 * Pure function.
 */
fun calculateDurationMillis(startNanos: Long, endNanos: Long): Long =
    (endNanos - startNanos) / 1_000_000

/**
 * Checks if memory measurement is available on this platform.
 */
fun isMemoryMeasurementAvailable(): Boolean = measureMemoryUsageMB() != null