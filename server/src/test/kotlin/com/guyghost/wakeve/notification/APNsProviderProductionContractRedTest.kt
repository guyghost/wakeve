package com.guyghost.wakeve.notification

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
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
            200 to APNsProviderOutcome.ACCEPTED,
            400 to APNsProviderOutcome.REJECTED_PAYLOAD,
            403 to APNsProviderOutcome.PROVIDER_AUTH_BLOCKED,
            410 to APNsProviderOutcome.INVALID_TOKEN,
            429 to APNsProviderOutcome.RETRY,
            500 to APNsProviderOutcome.RETRY,
            503 to APNsProviderOutcome.RETRY,
            599 to APNsProviderOutcome.UNKNOWN_TERMINAL
        )

        cases.forEach { (status, expected) ->
            val transport = RecordingTransport(APNsHttp2Response(status, receivedAtEpochSeconds = 1_000))
            val result = configuredSender(transport = transport).sendProvider(request())
            assertEquals(expected, result.getOrNull()?.classification?.outcome, "HTTP $status")
        }
    }

    @Test
    fun retryAfterIsExposedToExpiryBoundedRetryScheduling() = runBlocking {
        val transport = RecordingTransport(
            APNsHttp2Response(429, headers = mapOf("retry-after" to "120"), receivedAtEpochSeconds = 1_000)
        )
        val result = configuredSender(transport = transport).sendProvider(request(expiration = 2_000)).getOrNull()

        assertEquals(APNsProviderOutcome.RETRY, result?.classification?.outcome)
        val retry = assertNotNull(result?.classification?.retry)
        val nextAttempt = assertNotNull(retry.nextAttemptAtEpochSeconds)
        assertEquals(1_120L, nextAttempt)
        assertTrue(nextAttempt < retry.expiresAtEpochSeconds)
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
        authKey: String = "P8_CONTENT",
        tokenSigner: APNsTokenSigner = RecordingTokenSigner(),
        transport: APNsHttp2Transport = RecordingTransport(APNsHttp2Response(200, receivedAtEpochSeconds = 1_000))
    ) = ServerAPNsSender(
        apnsKeyId = "KEY123",
        apnsTeamId = "TEAM123",
        apnsAuthKey = authKey,
        apnsBundleId = "com.guyghost.wakeve",
        apnsEnvironment = "production",
        tokenSigner = tokenSigner,
        clock = APNsProviderClock { 1_000 },
        transport = transport
    )

    private fun request(
        token: String = "device-token",
        payload: String = "{\"aps\":{}}",
        expiration: Long = 10_000
    ) = APNsProviderRequest(DeliveryKey("delivery-1"), "apns-1", token, payload, expiration, 10, "alert")
}

private class RecordingTokenSigner(private val authorization: String = "bearer signed-token") : APNsTokenSigner {
    var wasInvoked = false
    override suspend fun sign(config: APNsProviderConfig, clock: APNsProviderClock): Result<APNsProviderToken> {
        wasInvoked = true
        return Result.success(APNsProviderToken(authorization, clock.epochSeconds()))
    }
}

private class RecordingTransport(private val response: APNsHttp2Response) : APNsHttp2Transport {
    var request: APNsHttp2Request? = null
    override suspend fun execute(request: APNsHttp2Request): APNsTransportResult {
        this.request = request
        return APNsTransportResult.Response(response)
    }
}
