package com.guyghost.wakeve.notification

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import kotlinx.coroutines.runBlocking
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec
import java.util.Base64
import org.slf4j.LoggerFactory
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** RED contracts consume only production APNs ports and the shipped sender. */
class APNsProviderProductionContractRedTest {
    @Test
    fun missingProviderConfigurationFailsClosed() = runBlocking {
        val result = ServerAPNsSender(apnsKeyId = null, apnsTeamId = null).sendProvider(request())

        assertTrue(result.isFailure)
        assertContains(result.exceptionOrNull()?.message.orEmpty(), "credentials")
    }

    @Test
    fun configuredProviderUsesInjectedSignerBeforeReportingReady() = runBlocking {
        val signer = RecordingTokenSigner()
        val result = configuredSender(tokenSigner = signer).sendProvider(request())

        assertTrue(result.isSuccess, "a fully configured provider must use the production signer port")
        assertTrue(signer.wasInvoked)
    }

    @Test
    fun invalidP8FailsClosedBeforeInjectedSignerOrTransportCanRun() = runBlocking {
        val signer = RecordingTokenSigner()
        val transport = RecordingTransport(APNsHttp2Response(200, apnsId = "must-not-send", receivedAtEpochSeconds = 1_000))

        val result = configuredSender(authKey = "not-a-p8", tokenSigner = signer, transport = transport).sendProvider(request())

        assertTrue(result.isFailure)
        assertFalse(signer.wasInvoked, "validation must precede the injected signer seam")
        assertNull(transport.request, "validation must precede the injected transport seam")
    }

    @Test
    fun configuredProviderExecutesHttp2RequestWithCanonicalEndpointAndHeaders() = runBlocking {
        val transport = RecordingTransport(APNsHttp2Response(200, apnsId = "apns-1", receivedAtEpochSeconds = 1_000))
        val result = configuredSender(transport = transport).sendProvider(request())

        assertTrue(result.isSuccess)
        val observed = assertNotNull(transport.request, "the real provider must execute the injected HTTP/2 transport")
        assertEquals("api.push.apple.com:443", observed.authority)
        assertTrue(observed.path.startsWith("/3/device/"))
        listOf("authorization", "apns-topic", "apns-push-type", "apns-id", "apns-expiration", "apns-priority")
            .forEach { assertTrue(observed.headers.containsKey(it), "missing APNs header $it") }
    }

    @Test
    fun realProviderMapsEveryApnsResponseClassDeterministically() = runBlocking {
        val cases = listOf(
            ApnsCase(200, null, APNsProviderOutcome.ACCEPTED),
            ApnsCase(400, "BadDeviceToken", APNsProviderOutcome.INVALID_TOKEN),
            ApnsCase(400, "DeviceTokenNotForTopic", APNsProviderOutcome.INVALID_TOKEN),
            ApnsCase(410, "ExpiredToken", APNsProviderOutcome.INVALID_TOKEN),
            ApnsCase(410, "Unregistered", APNsProviderOutcome.INVALID_TOKEN),
            ApnsCase(400, "IdleTimeout", APNsProviderOutcome.RETRY),
            ApnsCase(400, "BadTopic", APNsProviderOutcome.REJECTED_PAYLOAD),
            ApnsCase(400, "PayloadTooLarge", APNsProviderOutcome.REJECTED_PAYLOAD),
            ApnsCase(403, "ExpiredProviderToken", APNsProviderOutcome.REFRESH_AUTH),
            ApnsCase(403, "InvalidProviderToken", APNsProviderOutcome.PROVIDER_AUTH_BLOCKED),
            ApnsCase(429, "TooManyProviderTokenUpdates", APNsProviderOutcome.PROVIDER_AUTH_BLOCKED),
            ApnsCase(429, "TooManyRequests", APNsProviderOutcome.RETRY),
            ApnsCase(500, null, APNsProviderOutcome.RETRY),
            ApnsCase(503, null, APNsProviderOutcome.RETRY),
            ApnsCase(404, null, APNsProviderOutcome.REJECTED_PAYLOAD),
            ApnsCase(405, null, APNsProviderOutcome.REJECTED_PAYLOAD),
            ApnsCase(413, null, APNsProviderOutcome.REJECTED_PAYLOAD),
            ApnsCase(400, "Unregistered", APNsProviderOutcome.UNKNOWN_TERMINAL),
            ApnsCase(410, "BadDeviceToken", APNsProviderOutcome.UNKNOWN_TERMINAL),
            ApnsCase(400, "ExpiredProviderToken", APNsProviderOutcome.UNKNOWN_TERMINAL),
            ApnsCase(403, "BadDeviceToken", APNsProviderOutcome.PROVIDER_AUTH_BLOCKED),
            ApnsCase(403, "IdleTimeout", APNsProviderOutcome.PROVIDER_AUTH_BLOCKED),
            ApnsCase(429, "BadDeviceToken", APNsProviderOutcome.RETRY),
            ApnsCase(418, "NovelReason", APNsProviderOutcome.UNKNOWN_TERMINAL)
        )

        cases.forEach { (status, reason, expected) ->
            val transport = RecordingTransport(APNsHttp2Response(status, reason = reason, receivedAtEpochSeconds = 1_000))
            val result = configuredSender(transport = transport).sendProvider(request())
            assertEquals(expected, result.getOrNull()?.classification?.outcome, "HTTP $status / ${reason ?: "no reason"}")
        }
    }

    @Test
    fun retryAfterIsExposedAsRawProviderDirectiveWithoutSchedulingTheDelivery() = runBlocking {
        val transport = RecordingTransport(
            APNsHttp2Response(429, headers = mapOf("retry-after" to "120"), receivedAtEpochSeconds = 1_000)
        )
        val result = configuredSender(transport = transport).sendProvider(request(expiration = 2_000)).getOrNull()

        assertEquals(APNsProviderOutcome.RETRY, result?.classification?.outcome)
        val directive = assertNotNull(result?.classification?.retryDirective)
        assertIs<APNsProviderRetryDirective>(directive)
        assertEquals(1_120L, directive.retryAfterEpochSeconds)
    }

    @Test
    fun retryAfterAtDeliveryExpiryRemainsRawUntilTheMachineMakesTheExpiryDecision() = runBlocking {
        val transport = RecordingTransport(
            APNsHttp2Response(429, headers = mapOf("retry-after" to "120"), receivedAtEpochSeconds = 1_000)
        )
        val result = assertNotNull(
            configuredSender(transport = transport).sendProvider(request(expiration = 1_120)).getOrNull()
        )

        assertEquals(APNsProviderOutcome.RETRY, result.classification.outcome)
        assertEquals(1_120L, result.classification.retryDirective?.retryAfterEpochSeconds)
        val event = assertIs<APNsProviderMachineEvent.ResponseReceived>(
            APNsProviderMachineEventAdapter.toMachineEvent(result)
        )
        assertEquals(429, event.statusCode)
        assertEquals(1_120L, event.retryAfterEpochSeconds)
        assertTrue(event.correlationId.isNotBlank())
    }

    @Test
    fun defaultRetryResponseCarriesOnlyTheDirectiveOwnedByTheProvider() = runBlocking {
        val result = assertNotNull(
            configuredSender(
                transport = RecordingTransport(APNsHttp2Response(503, receivedAtEpochSeconds = 1_000))
            ).sendProvider(request(expiration = 1_120)).getOrNull()
        )

        assertEquals(APNsProviderOutcome.RETRY, result.classification.outcome)
        val directive = assertNotNull(result.classification.retryDirective)
        assertNull(directive.retryAfterEpochSeconds, "absence of Retry-After delegates default backoff to the machine")
        val event = assertIs<APNsProviderMachineEvent.ResponseReceived>(
            APNsProviderMachineEventAdapter.toMachineEvent(result)
        )
        assertEquals(503, event.statusCode)
        assertNull(event.retryAfterEpochSeconds)
    }

    @Test
    fun transportFailureBeforeWriteIsRetryableWithoutClaimingAcceptance() = runBlocking {
        val result = configuredSender(
            transport = RecordingTransport(
                APNsTransportResult.FailedBeforeWrite(APNsSanitizedDiagnostic("delivery-1", "corr-1", errorClass = "connect"))
            )
        ).sendProvider(request()).getOrNull()

        val providerResult = assertNotNull(result)
        assertEquals(APNsProviderOutcome.RETRY, providerResult.classification.outcome)
        assertNull(providerResult.classification.acceptedAtEpochSeconds)
        val directive = assertNotNull(providerResult.classification.retryDirective)
        assertNull(directive.retryAfterEpochSeconds)
        assertIs<APNsProviderMachineEvent.TransportFailedBeforeWrite>(
            APNsProviderMachineEventAdapter.toMachineEvent(providerResult)
        )
        Unit
    }

    @Test
    fun possiblyWrittenTransportOutcomeRemainsUnknownWithoutClaimingAcceptance() = runBlocking {
        val result = configuredSender(
            transport = RecordingTransport(
                APNsTransportResult.OutcomeUnknown(APNsSanitizedDiagnostic("delivery-1", "corr-1", errorClass = "timeout"))
            )
        ).sendProvider(request()).getOrNull()

        val providerResult = assertNotNull(result)
        assertEquals(APNsProviderOutcome.UNKNOWN_OUTCOME, providerResult.classification.outcome)
        assertNull(providerResult.classification.acceptedAtEpochSeconds)
        assertNull(providerResult.classification.retryDirective)
        assertIs<APNsProviderMachineEvent.TransportOutcomeUnknown>(
            APNsProviderMachineEventAdapter.toMachineEvent(providerResult)
        )
        Unit
    }

    @Test
    fun logsAndErrorsRedactTokenJwtPrivateKeyPayloadAndHttpDiagnostics() = runBlocking {
        val logger = LoggerFactory.getLogger("ServerAPNsSender") as Logger
        val appender = ListAppender<ILoggingEvent>().also { it.start() }
        logger.addAppender(appender)
        val secrets = listOf("RAW_DEVICE_TOKEN", "JWT_SECRET", "P8_PRIVATE_KEY", "PRIVATE_EVENT_BODY")
        try {
            val result = configuredSender(
                authKey = secrets[2],
                tokenSigner = RecordingTokenSigner(secrets[1])
            ).sendProvider(request(token = secrets[0], payload = secrets[3]))
            val observable = appender.list.joinToString("\n") { "${it.formattedMessage} ${it.argumentArray?.joinToString().orEmpty()}" } +
                "\n${result.exceptionOrNull()?.stackTraceToString().orEmpty()}"

            secrets.forEach { assertFalse(observable.contains(it), "operational output leaked secret marker") }
        } finally {
            logger.detachAppender(appender)
        }
    }

    private fun configuredSender(
        authKey: String = validP8(),
        tokenSigner: APNsTokenSigner = RecordingTokenSigner(),
        transport: APNsHttp2Transport = RecordingTransport(APNsHttp2Response(200, receivedAtEpochSeconds = 1_000))
    ) = ServerAPNsSender(
        apnsKeyId = "KEY123",
        apnsTeamId = "TEAM123",
        apnsAuthKey = authKey,
        apnsBundleId = "com.guyghost.wakeve",
        apnsEnvironment = "production",
        deploymentEnvironmentRaw = "production",
        tokenSigner = tokenSigner,
        clock = APNsProviderClock { 1_000 },
        transport = transport
    )

    private fun validP8(): String {
        val keyPair = KeyPairGenerator.getInstance("EC").apply {
            initialize(ECGenParameterSpec("secp256r1"))
        }.generateKeyPair()
        return "-----BEGIN PRIVATE KEY-----\n" +
            Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(keyPair.private.encoded) +
            "\n-----END PRIVATE KEY-----"
    }

    private fun request(
        token: String = "device-token",
        payload: String = "{\"aps\":{}}",
        expiration: Long = 10_000
    ) = APNsProviderRequest(DeliveryKey("delivery-1"), "apns-1", token, payload, expiration, 10, "alert")
}

private data class ApnsCase(val status: Int, val reason: String?, val expected: APNsProviderOutcome)

private class RecordingTokenSigner(private val authorization: String = "bearer signed-token") : APNsTokenSigner {
    var wasInvoked = false
    override suspend fun sign(config: APNsProviderConfig, clock: APNsProviderClock): Result<APNsProviderToken> {
        wasInvoked = true
        return Result.success(APNsProviderToken(authorization, clock.epochSeconds()))
    }
}

private class RecordingTransport(
    private val result: APNsTransportResult
) : APNsHttp2Transport {
    constructor(response: APNsHttp2Response) : this(APNsTransportResult.Response(response))
    var request: APNsHttp2Request? = null
    override suspend fun execute(request: APNsHttp2Request): APNsTransportResult {
        this.request = request
        return result
    }
}
