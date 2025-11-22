package com.guyghost.wakeve.sync

import android.app.Application
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

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
actual fun createNetworkStatusDetector(): NetworkStatusDetector {
    val context = try {
        val activityThread = Class.forName("android.app.ActivityThread")
        val currentApplication = activityThread.getDeclaredMethod("currentApplication")
        currentApplication.invoke(null) as android.app.Application
    } catch (e: Exception) {
        throw IllegalStateException("Unable to obtain Android Application context", e)
    }
    return AndroidNetworkStatusDetector(context)
}