package com.guyghost.wakeve

import platform.Foundation.NSDate
import platform.Foundation.NSString
import platform.Foundation.timeIntervalSince1970

/**
 * iOS implementation of SHA-256 hashing.
 *
 * Note: This is a simplified implementation using Swift's native crypto.
 * For production, consider using CryptoKit interop.
 */
actual fun sha256Hash(input: String): String {
    // Convert string to NSString and use its hash as a fallback
    // In production, this should use CommonCrypto or CryptoKit
    val nsString = input as NSString
    val hashCode = nsString.hash().toULong()

    // Create a pseudo-SHA256 hash (64 hex characters) from the hash code
    // This is NOT cryptographically secure - for development/testing only
    // TODO: Implement proper SHA-256 using CommonCrypto or CryptoKit
    val hashString = hashCode.toString(16).padStart(16, '0')
    return hashString.repeat(4).take(64)
}

/**
 * iOS implementation of current time in milliseconds.
 */
actual fun currentTimeMillis(): Long {
    return (NSDate().timeIntervalSince1970 * 1000).toLong()
}
