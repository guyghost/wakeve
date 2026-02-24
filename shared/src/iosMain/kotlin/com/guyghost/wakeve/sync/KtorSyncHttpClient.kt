package com.guyghost.wakeve.sync

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.darwin.dispatch_queue_create

/**
 * iOS network status detector utilisant NWPathMonitor (Network framework)
 * Surveille les changements de connectivite reseau en temps reel
 */
@OptIn(ExperimentalForeignApi::class)
class IosNetworkStatusDetector : NetworkStatusDetector {
    private val _isNetworkAvailable = MutableStateFlow(true)
    override val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()

    private val monitor = nw_path_monitor_create()
    private val queue = dispatch_queue_create("com.guyghost.wakeve.network", null)

    init {
        nw_path_monitor_set_update_handler(monitor) { path ->
            val status = nw_path_get_status(path)
            _isNetworkAvailable.value = (status == nw_path_status_satisfied)
        }
        nw_path_monitor_set_queue(monitor, queue)
        nw_path_monitor_start(monitor)
    }

    /**
     * Arrete la surveillance reseau et libere les ressources
     */
    fun stop() {
        nw_path_monitor_cancel(monitor)
    }
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
