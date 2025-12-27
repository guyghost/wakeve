package com.guyghost.wakeve.comment

import com.guyghost.wakeve.models.Comment
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * In-memory cache for comment data with TTL (Time To Live) and LRU eviction.
 * Designed for performance optimization of frequently accessed comment lists.
 */
class CommentCache(
    private val maxCacheSize: Int = 100,
    private val ttlSeconds: Long = 300 // 5 minutes default TTL
) {
    private val cache = mutableMapOf<String, CacheEntry<CommentListResult>>()

    /**
     * Data class for cache entries with timestamp tracking
     */
    data class CacheEntry<T>(
        val value: T,
        val timestamp: Instant = Clock.System.now()
    )

    /**
     * Result wrapper for cached comment lists with metadata
     */
    data class CommentListResult(
        val comments: List<Comment>,
        val totalCount: Int
    )

    /**
     * Retrieve cached data if available and not expired
     */
    fun get(key: String): CommentListResult? {
        val entry = cache[key] ?: return null
        val now = Clock.System.now()

        // Check TTL expiration
        if (now.minus(entry.timestamp).inWholeSeconds > ttlSeconds) {
            cache.remove(key)
            return null
        }

        return entry.value
    }

    /**
     * Store data in cache with LRU eviction if needed
     */
    fun put(key: String, value: CommentListResult) {
        // Simple LRU eviction: remove oldest entry if at capacity
        if (cache.size >= maxCacheSize) {
            val oldestKey = cache.entries.minByOrNull { it.value.timestamp }?.key
            oldestKey?.let { cache.remove(it) }
        }

        cache[key] = CacheEntry(value)
    }

    /**
     * Invalidate all cache entries related to a specific event
     */
    fun invalidate(eventId: String) {
        val keysToRemove = cache.keys.filter {
            it.startsWith("$eventId:")
        }
        keysToRemove.forEach { cache.remove(it) }
    }

    /**
     * Invalidate cache entries containing a specific comment
     */
    fun invalidateComment(commentId: String) {
        val keysToRemove = cache.keys.filter {
            it.contains(":$commentId:")
        }
        keysToRemove.forEach { cache.remove(it) }
    }

    /**
     * Clear entire cache
     */
    fun clear() {
        cache.clear()
    }

    /**
     * Get current cache size for monitoring
     */
    fun size(): Int = cache.size

    /**
     * Get cache statistics for monitoring
     */
    fun getStats(): CacheStats {
        val now = Clock.System.now()
        val entries = cache.values
        val expiredCount = entries.count { now.minus(it.timestamp).inWholeSeconds > ttlSeconds }

        return CacheStats(
            totalEntries = cache.size,
            expiredEntries = expiredCount,
            averageAgeSeconds = entries.map { now.minus(it.timestamp).inWholeSeconds }.average()
        )
    }

    data class CacheStats(
        val totalEntries: Int,
        val expiredEntries: Int,
        val averageAgeSeconds: Double
    )
}