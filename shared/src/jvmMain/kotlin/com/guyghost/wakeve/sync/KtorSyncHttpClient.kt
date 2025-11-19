package com.guyghost.wakeve.sync

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Ktor-based HTTP client for sync operations (JVM implementation)
 */
class KtorSyncHttpClient(
    private val baseUrl: String = "http://localhost:8080",
    private val httpClient: HttpClient = HttpClient()
) : SyncHttpClient {

    override suspend fun sync(requestJson: String, authToken: String): Result<String> = runCatching {
        val response = httpClient.post("$baseUrl/api/sync") {
            contentType(ContentType.Application.Json)
            bearerAuth(authToken)
            setBody(requestJson)
        }

        if (response.status.isSuccess()) {
            response.body<String>()
        } else {
            throw Exception("Sync failed with status: ${response.status}")
        }
    }
}

/**
 * Simple JVM network status detector (always assumes network is available for desktop)
 */
class JvmNetworkStatusDetector : NetworkStatusDetector {
    private val _isNetworkAvailable = MutableStateFlow(true) // Assume always available for JVM
    override val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable
}

actual fun createSyncHttpClient(baseUrl: String): SyncHttpClient = KtorSyncHttpClient(baseUrl)
actual fun createNetworkStatusDetector(): NetworkStatusDetector = JvmNetworkStatusDetector()