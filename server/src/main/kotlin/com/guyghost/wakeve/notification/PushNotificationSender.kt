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
import java.util.UUID

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
    private val apnsBundleId: String? = System.getenv("APNS_BUNDLE_ID"),
    private val apnsEnvironment: String? = System.getenv("APNS_ENVIRONMENT"),
    private val deploymentEnvironment: APNsDeploymentEnvironment? = APNsDeploymentEnvironment.DEVELOPMENT,
    private val deploymentEnvironmentRaw: String? = System.getenv("WAKEVE_DEPLOYMENT_ENVIRONMENT"),
    private val tokenSigner: APNsTokenSigner? = null,
    private val clock: APNsProviderClock = APNsProviderClock { System.currentTimeMillis() / 1_000 },
    private val transport: APNsHttp2Transport? = null
) : APNsSender {

    private val logger = LoggerFactory.getLogger("ServerAPNsSender")
    private val runtimeLock = Any()
    private val circuit = APNsProviderCircuit()
    private var configuredProvider: APNsProviderConfig? = null
    private var configuredRuntime: APNsProviderRuntime? = null

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
        val providerResult = sendProvider(request).getOrThrow()
        check(providerResult.classification.outcome == APNsProviderOutcome.ACCEPTED) {
            "APNs did not accept the legacy notification"
        }
    }

    suspend fun sendProvider(request: APNsProviderRequest): Result<APNsProviderResult> = runCatching {
        val testRuntime = APNsProviderRuntimeTestOverride.current()
        val readiness = readiness(testRuntime)
        if (readiness.status != APNsProviderReadinessStatus.READY) {
            logger.warn("APNs credentials not configured; notification delivery blocked")
            error("APNs credentials are not configured")
        }
        val configuration = configuration(testRuntime)
        val credentialVersion = APNsProviderCredentialVersion.from(configuration)
        if (circuit.isBlocked(credentialVersion)) {
            return@runCatching blockedResult(request, UUID.randomUUID().toString())
        }
        val refreshCoordinator = APNsProviderAuthRefreshCoordinator()
        val runtime = runtime(testRuntime, configuration)
        val signer = tokenSigner ?: runtime.signer
        val providerTransport = transport ?: runtime.transport
        while (true) {
            val token = signer.sign(configuration, clock).getOrThrow()
            val httpRequest = APNsHttp2Request(
                authority = configuration.environment.authority,
                path = "/3/device/${request.deviceToken}",
                headers = linkedMapOf(
                    "authorization" to token.authorizationValue,
                    "apns-topic" to configuration.topic,
                    "apns-push-type" to request.pushType,
                    "apns-id" to request.apnsId,
                    "apns-expiration" to request.expirationEpochSeconds.toString(),
                    "apns-priority" to request.priority.toString()
                ),
                body = request.payload,
                correlationId = UUID.randomUUID().toString()
            )
            when (val result = providerTransport.execute(httpRequest)) {
                is APNsTransportResult.Response -> {
                    val classification = classifyApnsResponse(result.value)
                    val action = when (classification.outcome) {
                        APNsProviderOutcome.REFRESH_AUTH -> refreshCoordinator.actionFor(classification.outcome)
                        APNsProviderOutcome.PROVIDER_AUTH_BLOCKED -> APNsProviderAuthRefreshAction.BLOCK_PROVIDER
                        else -> APNsProviderAuthRefreshAction.NONE
                    }
                    when (action) {
                        APNsProviderAuthRefreshAction.FORCE_REFRESH -> {
                            val productionSigner = signer as? ProductionAPNsTokenSigner
                            if (productionSigner == null) {
                                return@runCatching APNsProviderResult(
                                    classification,
                                    APNsSanitizedDiagnostic(request.deliveryKey.value, httpRequest.correlationId, statusCode = result.value.statusCode, reasonCode = result.value.reason)
                                )
                            }
                            productionSigner.forceInvalidate()
                            continue
                        }
                        APNsProviderAuthRefreshAction.BLOCK_PROVIDER -> {
                            circuit.block(credentialVersion)
                            return@runCatching blockedResult(
                                request,
                                httpRequest.correlationId,
                                result.value.statusCode,
                                result.value.reason
                            )
                        }
                        APNsProviderAuthRefreshAction.NONE -> return@runCatching APNsProviderResult(
                            classification,
                            APNsSanitizedDiagnostic(request.deliveryKey.value, httpRequest.correlationId, statusCode = result.value.statusCode, reasonCode = result.value.reason)
                        )
                    }
                }
                is APNsTransportResult.FailedBeforeWrite -> return@runCatching APNsProviderResult(
                    APNsProviderClassification(APNsProviderOutcome.RETRY, null, null, null, null, retryDirective = APNsProviderRetryDirective(null)), result.diagnostic
                )
                is APNsTransportResult.OutcomeUnknown -> return@runCatching APNsProviderResult(
                    APNsProviderClassification(APNsProviderOutcome.UNKNOWN_OUTCOME, null, null, null, null), result.diagnostic
                )
            }
        }
        error("unreachable")
    }

    /** Reopens a provider circuit only when the already-validated credential version changes. */
    fun replaceValidatedCredentials(configuration: APNsProviderConfig): Result<APNsProviderCredentialVersion> = runCatching {
        val replacementVersion = APNsProviderCredentialVersion.from(configuration)
        synchronized(runtimeLock) {
            val currentVersion = configuredProvider?.let(APNsProviderCredentialVersion::from)
            if (currentVersion != replacementVersion) {
                configuredProvider = configuration
                configuredRuntime = null
            }
        }
        circuit.validatedCredentialsChanged(replacementVersion)
        replacementVersion
    }

    fun credentialVersion(): APNsProviderCredentialVersion = APNsProviderCredentialVersion.from(configuration(null))

    fun readiness(): APNsProviderReadiness = readiness(APNsProviderRuntimeTestOverride.current())

    private fun readiness(testRuntime: APNsProviderRuntimeTestOverride.Value?): APNsProviderReadiness {
        if (testRuntime != null) return APNsProviderReadiness(APNsProviderReadinessStatus.READY, emptySet())
        val parsedDeployment = APNsDeploymentEnvironment.parse(deploymentEnvironmentRaw)
        val reasons = buildSet {
            if (apnsKeyId.isNullOrBlank()) add(APNsProviderReadinessReason.MISSING_KEY_ID)
            if (apnsTeamId.isNullOrBlank()) add(APNsProviderReadinessReason.MISSING_TEAM_ID)
            if (apnsAuthKey.isNullOrBlank()) add(APNsProviderReadinessReason.MISSING_AUTH_KEY)
            else if (APNsProviderConfig.create(apnsKeyId ?: "key", apnsTeamId ?: "team", apnsAuthKey, apnsBundleId ?: "topic", apnsEnvironment ?: "production").isFailure) add(APNsProviderReadinessReason.INVALID_AUTH_KEY)
            if (apnsBundleId.isNullOrBlank()) add(APNsProviderReadinessReason.MISSING_TOPIC)
            if (apnsEnvironment.isNullOrBlank()) add(APNsProviderReadinessReason.MISSING_ENVIRONMENT)
            parsedDeployment.readinessReason?.let(::add)
            if (parsedDeployment.environment == APNsDeploymentEnvironment.PRODUCTION && !apnsEnvironment.equals("production", true)) add(APNsProviderReadinessReason.PRODUCTION_REQUIRES_PRODUCTION_APNS)
        }
        return APNsProviderReadiness(if (reasons.isEmpty()) APNsProviderReadinessStatus.READY else APNsProviderReadinessStatus.NOT_READY, reasons)
    }

    private fun configuration(testRuntime: APNsProviderRuntimeTestOverride.Value?): APNsProviderConfig =
        testRuntime?.config ?: synchronized(runtimeLock) {
            configuredProvider ?: APNsProviderConfig.create(
                keyId = apnsKeyId,
                teamId = apnsTeamId,
                authKey = apnsAuthKey,
                topic = apnsBundleId,
                environment = apnsEnvironment
            ).getOrThrow().also { configuredProvider = it }
        }

    private fun runtime(
        testRuntime: APNsProviderRuntimeTestOverride.Value?,
        configuration: APNsProviderConfig
    ): APNsProviderRuntime = testRuntime?.runtime ?: synchronized(runtimeLock) {
        configuredRuntime ?: APNsProviderRuntimeFactory.create(configuration, clock).getOrThrow()
            .also { configuredRuntime = it }
    }

    private fun blockedResult(
        request: APNsProviderRequest,
        correlationId: String,
        statusCode: Int? = null,
        reason: String? = null
    ) = APNsProviderResult(
        APNsProviderClassification(APNsProviderOutcome.PROVIDER_AUTH_BLOCKED, statusCode, reason, null, null),
        APNsSanitizedDiagnostic(request.deliveryKey.value, correlationId, statusCode = statusCode, reasonCode = reason)
    )
}
