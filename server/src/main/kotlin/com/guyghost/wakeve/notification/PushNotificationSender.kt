package com.guyghost.wakeve.notification

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.statement.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.slf4j.LoggerFactory

/**
 * Server-side FCM sender implementation.
 *
 * Sends push notifications to Android devices via Firebase Cloud Messaging HTTP v1 API.
 * In production, configure FCM_SERVER_KEY environment variable with a valid service account key.
 */
class ServerFCMSender(
    private val fcmServerKey: String? = System.getenv("FCM_SERVER_KEY")
) : FCMSender {

    private val logger = LoggerFactory.getLogger("ServerFCMSender")

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
            logger.warn("FCM_SERVER_KEY not configured; notification delivery failed")
            error("FCM_SERVER_KEY is not configured")
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
            logger.error("FCM send failed: status=${response.status}")
            error("FCM send failed with status ${response.status}")
        }

        validateFcmLegacyResponse(response.bodyAsText()).getOrThrow()

        logger.info("FCM notification sent")
    }
}

internal fun validateFcmLegacyResponse(responseBody: String): Result<Unit> = runCatching {
    val jsonObject = runCatching { Json.parseToJsonElement(responseBody).jsonObject }
        .getOrElse { error("FCM response body is not valid JSON") }

    val success = jsonObject["success"]?.jsonPrimitive?.intOrNull
        ?: error("FCM response body does not include success count")
    val failure = jsonObject["failure"]?.jsonPrimitive?.intOrNull
        ?: error("FCM response body does not include failure count")

    if (failure > 0 || success <= 0) {
        val firstError = jsonObject["results"]
            ?.let {
                runCatching {
                    it.jsonArray.firstOrNull()
                        ?.jsonObject
                        ?.get("error")
                        ?.jsonPrimitive
                        ?.contentOrNull
                }.getOrNull()
            }
        val detail = firstError?.let { ": $it" }.orEmpty()
        error("FCM delivery failed (success=$success, failure=$failure)$detail")
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
class ServerAPNsSender(
    private val apnsKeyId: String? = System.getenv("APNS_KEY_ID"),
    private val apnsTeamId: String? = System.getenv("APNS_TEAM_ID"),
    private val apnsAuthKey: String? = System.getenv("APNS_AUTH_KEY"),
    private val apnsBundleId: String = System.getenv("APNS_BUNDLE_ID") ?: "com.guyghost.wakeve",
    private val apnsEnvironment: String? = System.getenv("APNS_ENVIRONMENT"),
    private val tokenSigner: APNsTokenSigner? = null,
    private val clock: APNsProviderClock = APNsProviderClock { System.currentTimeMillis() / 1_000 },
    private val transport: APNsHttp2Transport? = null
) : APNsSender {

    private val logger = LoggerFactory.getLogger("ServerAPNsSender")

    override suspend fun sendNotification(
        token: String,
        title: String,
        body: String,
        data: Map<String, String>
    ): Result<Unit> = runCatching {
        val request = APNsProviderRequest(
            deliveryKey = DeliveryKey("legacy-unpersisted"),
            apnsId = "legacy-unpersisted",
            deviceToken = token,
            payload = buildJsonObject {
                putJsonObject("aps") {
                    putJsonObject("alert") {
                        put("title", title)
                        put("body", body)
                    }
                }
                data.forEach { (key, value) -> put(key, value) }
            }.toString(),
            expirationEpochSeconds = clock.epochSeconds() + 3_600,
            priority = 10,
            pushType = "alert"
        )
        sendProvider(request).getOrThrow()
    }

    suspend fun sendProvider(request: APNsProviderRequest): Result<APNsProviderResult> = runCatching {
        val configuration = APNsProviderConfig.create(
            keyId = apnsKeyId,
            teamId = apnsTeamId,
            authKey = apnsAuthKey,
            topic = apnsBundleId,
            environment = apnsEnvironment
        )
        if (configuration.isFailure) {
            logger.warn("APNs credentials not configured; notification delivery failed")
            error("APNs credentials are not configured: ${configuration.exceptionOrNull()?.message}")
        }

        // The approved ports are injected, but actual signing, HTTP/2 delivery and success
        // classification are intentionally deferred to tasks 5.1-5.3.
        @Suppress("UNUSED_VARIABLE") val approvedSeams = listOf(configuration.getOrThrow(), tokenSigner, clock, transport, request)
        logger.warn("APNs sender is not implemented")
        error("APNs sender is not implemented")
    }
}
