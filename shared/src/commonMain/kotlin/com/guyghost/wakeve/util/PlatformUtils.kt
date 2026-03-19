package com.guyghost.wakeve.util

/**
 * Compute SHA-256 hash of a string.
 * Platform-specific implementation.
 */
expect fun sha256Hash(input: String): String

/**
 * Get current time in milliseconds since epoch.
 * Platform-specific implementation.
 */
expect fun currentTimeMillis(): Long
