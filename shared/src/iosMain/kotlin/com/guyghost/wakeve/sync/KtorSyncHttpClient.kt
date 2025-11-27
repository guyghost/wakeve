package com.guyghost.wakeve.sync

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * iOS network status detector (simplified - always available for now)
 * TODO: Implement proper NWPathMonitor integration
 */
class IosNetworkStatusDetector : NetworkStatusDetector {
    private val _isNetworkAvailable = MutableStateFlow(true)
    override val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable
}

/**
 * Ktor-based HTTP client for sync operations (iOS implementation)
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

        when {
            response.status.isSuccess() -> response.body<String>()
            response.status.value == 401 -> throw UnauthorizedException("Authentication failed: token may be expired")
            response.status.value == 403 -> throw ForbiddenException("Access forbidden: insufficient permissions")
            else -> throw HttpException(response.status.value, "Sync failed with status: ${response.status}")
        }
    }
}

actual fun createSyncHttpClient(baseUrl: String): SyncHttpClient = KtorSyncHttpClient(baseUrl)
actual fun createNetworkStatusDetector(): NetworkStatusDetector = IosNetworkStatusDetector()