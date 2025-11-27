package com.guyghost.wakeve

import java.security.MessageDigest

/**
 * JVM implementation of SHA-256 hashing.
 */
actual fun sha256Hash(input: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

/**
 * JVM implementation of current time in milliseconds.
 */
actual fun currentTimeMillis(): Long {
    return System.currentTimeMillis()
}
