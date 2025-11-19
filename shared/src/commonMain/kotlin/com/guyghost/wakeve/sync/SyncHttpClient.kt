package com.guyghost.wakeve.sync

/**
 * HTTP client for sync operations
 */
interface SyncHttpClient {
    suspend fun sync(requestJson: String, authToken: String): Result<String>
}

/**
 * Factory function to create platform-specific HTTP client
 */
expect fun createSyncHttpClient(baseUrl: String = "http://localhost:8080"): SyncHttpClient