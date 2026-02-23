package com.guyghost.wakeve.notification

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.slf4j.LoggerFactory

/**
 * Server-side FCM sender implementation.
 *
 * Sends push notifications to Android devices via Firebase Cloud Messaging HTTP v1 API.
 * In production, configure FCM_SERVER_KEY environment variable with a valid service account key.
 */
class ServerFCMSender : FCMSender {

    private val logger = LoggerFactory.getLogger("ServerFCMSender")

    // FCM HTTP v1 API endpoint
    // TODO: Replace with actual project ID from Firebase console
    private val fcmProjectId = System.getenv("FCM_PROJECT_ID") ?: "wakeve-app"
    private val fcmServerKey = System.getenv("FCM_SERVER_KEY")

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    override suspend fun sendNotification(
        token: String,
        title: String,
        body: String,
        data: Map<String, String>
    ): Result<Unit> = runCatching {
        if (fcmServerKey == null) {
            // TODO: En production, configurer FCM_SERVER_KEY pour activer l'envoi FCM
            logger.warn("FCM_SERVER_KEY not configured. Logging notification instead of sending.")
            logger.info("FCM notification: token=${token.take(20)}..., title=$title, body=$body, data=$data")
            return@runCatching
        }

        // FCM legacy HTTP API (simpler setup, suitable for server-to-server)
        val response = httpClient.post("https://fcm.googleapis.com/fcm/send") {
            header("Authorization", "key=$fcmServerKey")
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("to", token)
                putJsonObject("notification") {
                    put("title", title)
                    put("body", body)
                }
                putJsonObject("data") {
                    data.forEach { (key, value) -> put(key, value) }
                    put("title", title)
                    put("body", body)
                }
                put("priority", "high")
            }.toString())
        }

        if (response.status != HttpStatusCode.OK) {
            val responseBody = response.bodyAsText()
            logger.error("FCM send failed: status=${response.status}, body=$responseBody")
            error("FCM send failed with status ${response.status}")
        }

        logger.info("FCM notification sent to ${token.take(20)}...")
    }
}

/**
 * Server-side APNs sender implementation.
 *
 * Sends push notifications to iOS devices via Apple Push Notification service.
 * In production, configure APNs credentials via environment variables.
 *
 * TODO: Pour la production, implémenter HTTP/2 APNs avec:
 *  - APNS_KEY_ID: ID de la clé APNs
 *  - APNS_TEAM_ID: Apple Team ID
 *  - APNS_AUTH_KEY: Contenu du fichier .p8
 *  - APNS_BUNDLE_ID: Bundle ID de l'application iOS
 */
class ServerAPNsSender : APNsSender {

    private val logger = LoggerFactory.getLogger("ServerAPNsSender")

    // APNs configuration from environment
    private val apnsKeyId = System.getenv("APNS_KEY_ID")
    private val apnsTeamId = System.getenv("APNS_TEAM_ID")
    private val apnsBundleId = System.getenv("APNS_BUNDLE_ID") ?: "com.guyghost.wakeve"
    private val apnsEnvironment = System.getenv("APNS_ENVIRONMENT") ?: "development"

    private val apnsHost: String
        get() = if (apnsEnvironment == "production") {
            "https://api.push.apple.com"
        } else {
            "https://api.development.push.apple.com"
        }

    override suspend fun sendNotification(
        token: String,
        title: String,
        body: String,
        data: Map<String, String>
    ): Result<Unit> = runCatching {
        if (apnsKeyId == null || apnsTeamId == null) {
            // TODO: En production, configurer les variables APNs pour activer l'envoi
            logger.warn("APNs credentials not configured. Logging notification instead of sending.")
            logger.info("APNs notification: token=${token.take(20)}..., title=$title, body=$body, data=$data")
            return@runCatching
        }

        // TODO: Implémenter la connexion HTTP/2 avec JWT APNs auth token
        // Pour le moment, on log la notification
        logger.info("APNs notification would be sent to $apnsHost/3/device/$token")
        logger.info("  Payload: title=$title, body=$body, data=$data")
        logger.info("  Bundle ID: $apnsBundleId")
    }
}
