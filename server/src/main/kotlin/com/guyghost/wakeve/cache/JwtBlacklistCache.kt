package com.guyghost.wakeve.cache

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe LRU cache for JWT blacklist checks.
 *
 * This cache stores blacklist status for JWT tokens to avoid
 * repeated database lookups. Entries expire after [ttlMillis].
 *
 * @property maxSize Maximum number of entries in the cache (default: 10,000)
 * @property ttlMillis Time-to-live for cache entries in milliseconds (default: 5 minutes)
 */
class JwtBlacklistCache(
    private val maxSize: Int = 10000,
    private val ttlMillis: Long = 5 * 60 * 1000 // 5 minutes
) {
    internal data class CacheEntry(
        val isBlacklisted: Boolean,
        val timestamp: Long
    )

    init {
        require(maxSize > 0) { "maxSize must be positive" }
        require(ttlMillis > 0) { "ttlMillis must be positive" }
    }

    /**
     * LRU cache using LinkedHashMap with access-order mode.
     * The third parameter (true) enables access-order, which makes the map LRU.
     */
    private val cache = LinkedHashMap<String, CacheEntry>(maxSize, 0.75f, true)
    private val mutex = Mutex()

    /**
     * Get blacklist status from cache.
     * Returns null if not in cache or expired.
     *
     * @param token JWT token to look up
     * @return Boolean blacklist status, or null if not in cache or expired
     */
    suspend fun get(token: String): Boolean? = mutex.withLock {
        val entry = cache[token]
        when {
            entry == null -> null
            isExpired(entry) -> {
                cache.remove(token)
                null
            }
            else -> entry.isBlacklisted
        }
    }

    /**
     * Store blacklist status in cache.
     *
     * @param token JWT token to cache
     * @param isBlacklisted Blacklist status to store
     */
    suspend fun put(token: String, isBlacklisted: Boolean) {
        mutex.withLock {
            if (cache.size >= maxSize) {
                // Remove oldest entry (LRU eviction)
                cache.remove(cache.keys.first())
            }
            cache[token] = CacheEntry(isBlacklisted, System.currentTimeMillis())
        }
    }

    /**
     * Remove entry from cache (e.g., on token revocation).
     *
     * @param token JWT token to remove from cache
     */
    suspend fun remove(token: String) {
        mutex.withLock {
            cache.remove(token)
        }
    }

    /**
     * Clear entire cache.
     */
    suspend fun clear() {
        mutex.withLock {
            cache.clear()
        }
    }

    /**
     * Check if a cache entry has expired.
     *
     * @param entry Cache entry to check
     * @return true if the entry has expired
     */
    private fun isExpired(entry: CacheEntry): Boolean {
        return System.currentTimeMillis() - entry.timestamp > ttlMillis
    }
}
