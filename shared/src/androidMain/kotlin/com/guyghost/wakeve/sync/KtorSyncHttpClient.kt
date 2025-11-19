package com.guyghost.wakeve.sync

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

/**
 * Ktor-based HTTP client for sync operations (Android implementation)
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

actual fun createSyncHttpClient(baseUrl: String): SyncHttpClient = KtorSyncHttpClient(baseUrl)
actual fun createNetworkStatusDetector(): NetworkStatusDetector = TODO("Implement Android network detector")