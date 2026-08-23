package com.guyghost.wakeve.notification

import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.spec.ECGenParameterSpec
import java.util.Base64
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Integration contracts for the review findings. They exercise [ServerAPNsSender] and the real
 * HTTP/2/TLS transport; no test-only helper is the subject under test.
 */
class APNsProductionReviewFindingsRedTest {
    @Test
    fun legacySendNotificationSucceedsOnlyForAcceptedAndFailsForEveryOtherProviderOutcome() = runBlocking {
        val failures = listOf(
            "invalid token" to APNsTransportResult.Response(APNsHttp2Response(400, reason = "BadDeviceToken", receivedAtEpochSeconds = 1_000)),
            "rejected payload" to APNsTransportResult.Response(APNsHttp2Response(400, reason = "BadTopic", receivedAtEpochSeconds = 1_000)),
            "retry" to APNsTransportResult.Response(APNsHttp2Response(503, receivedAtEpochSeconds = 1_000)),
            "provider auth blocked" to APNsTransportResult.Response(APNsHttp2Response(403, reason = "InvalidProviderToken", receivedAtEpochSeconds = 1_000)),
            "unknown terminal" to APNsTransportResult.Response(APNsHttp2Response(418, reason = "NovelReason", receivedAtEpochSeconds = 1_000)),
            "unknown outcome" to APNsTransportResult.OutcomeUnknown(APNsSanitizedDiagnostic("legacy", "unknown", errorClass = "timeout")),
            "failed before write" to APNsTransportResult.FailedBeforeWrite(APNsSanitizedDiagnostic("legacy", "before-write", errorClass = "connect"))
        )

        assertTrue(sender(RecordingTransport(APNsTransportResult.Response(APNsHttp2Response(200, receivedAtEpochSeconds = 1_000))))
            .sendNotification("token", "title", "body", emptyMap()).isSuccess)
        failures.forEach { (name, transportResult) ->
            assertTrue(
                sender(RecordingTransport(transportResult)).sendNotification("token", "title", "body", emptyMap()).isFailure,
                name
            )
        }
        assertTrue(
            sender(
                RecordingTransport(APNsTransportResult.Response(APNsHttp2Response(403, reason = "ExpiredProviderToken", receivedAtEpochSeconds = 1_000))),
                tokenSigner = NonRefreshableSigner()
            ).sendNotification("token", "title", "body", emptyMap()).isFailure,
            "refresh auth"
        )
    }

    @Test
    fun everyNormative400PayloadReasonIsTerminalPayloadRejection() {
        listOf(
            "BadCollapseId", "BadMessageId", "BadTopic", "BadPath", "MethodNotAllowed",
            "PayloadEmpty", "PayloadTooLarge", "BadPriority", "BadExpirationDate", "MissingTopic"
        ).forEach { reason ->
            assertEquals(APNsProviderOutcome.REJECTED_PAYLOAD, classifyApnsResponse(
                APNsHttp2Response(400, reason = reason, receivedAtEpochSeconds = 1_000)
            ).outcome, reason)
        }
    }

    @Test
    fun absentOrInvalidDeploymentEnvironmentIsNotReadyAndNeverInvokesSignerOrTransport() = runBlocking {
        listOf(
            DeploymentEnvironmentCase(null, "MISSING_DEPLOYMENT_ENVIRONMENT"),
            DeploymentEnvironmentCase("staging", "INVALID_DEPLOYMENT_ENVIRONMENT")
        ).forEach { case ->
            val signer = CountingSigner()
            val transport = RecordingTransport(APNsTransportResult.Response(APNsHttp2Response(200, receivedAtEpochSeconds = 1_000)))
            val sender = sender(transport, signer, deploymentEnvironmentRaw = case.rawValue)

            val readiness = sender.readiness()
            assertEquals(APNsProviderReadinessStatus.NOT_READY, readiness.status)
            assertEquals(setOf(APNsProviderReadinessReason.valueOf(case.expectedReasonName)), readiness.reasons)
            assertTrue(sender.sendProvider(request()).isFailure)
            assertEquals(0, signer.calls, "readiness must fail before signing")
            assertEquals(0, transport.requests.size, "readiness must fail before transport")
        }
    }

    @Test
    fun publicSenderConstructionWithoutRawDeploymentEnvironmentFailsClosedBeforeProviderWork() = runBlocking {
        val signer = CountingSigner()
        val transport = RecordingTransport(APNsTransportResult.Response(APNsHttp2Response(200, receivedAtEpochSeconds = 1_000)))
        val sender = ServerAPNsSender(
            apnsKeyId = "KEY123",
            apnsTeamId = "TEAM123",
            apnsAuthKey = validP8(),
            apnsBundleId = "com.guyghost.wakeve",
            apnsEnvironment = "production",
            tokenSigner = signer,
            clock = APNsProviderClock { 1_000 },
            transport = transport
        )

        assertEquals(APNsProviderReadinessStatus.NOT_READY, sender.readiness().status)
        assertEquals(setOf(APNsProviderReadinessReason.MISSING_DEPLOYMENT_ENVIRONMENT), sender.readiness().reasons)
        assertTrue(sender.sendProvider(request("missing-deployment")).isFailure)
        assertEquals(0, signer.calls)
        assertEquals(0, transport.requests.size)
    }

    @Test
    fun publicTypedDeploymentConvenienceWithoutRawEnvironmentAlsoFailsClosedBeforeProviderWork() = runBlocking {
        val signer = CountingSigner()
        val transport = RecordingTransport(APNsTransportResult.Response(APNsHttp2Response(200, receivedAtEpochSeconds = 1_000)))
        val sender = ServerAPNsSender(
            apnsKeyId = "KEY123",
            apnsTeamId = "TEAM123",
            apnsAuthKey = validP8(),
            apnsBundleId = "com.guyghost.wakeve",
            apnsEnvironment = "production",
            deploymentEnvironment = APNsDeploymentEnvironment.DEVELOPMENT,
            tokenSigner = signer,
            clock = APNsProviderClock { 1_000 },
            transport = transport
        )

        assertEquals(APNsProviderReadinessStatus.NOT_READY, sender.readiness().status)
        assertEquals(setOf(APNsProviderReadinessReason.MISSING_DEPLOYMENT_ENVIRONMENT), sender.readiness().reasons)
        assertTrue(sender.sendProvider(request("typed-deployment")).isFailure)
        assertEquals(0, signer.calls)
        assertEquals(0, transport.requests.size)
    }

    @Test
    fun sameSenderReusesDefaultRuntimeJwtAcrossSeparateNormalProviderCalls() = runBlocking {
        val transport = RecordingTransport(
            APNsTransportResult.Response(APNsHttp2Response(200, receivedAtEpochSeconds = 1_000)),
            APNsTransportResult.Response(APNsHttp2Response(200, receivedAtEpochSeconds = 1_000))
        )
        val sender = sender(transport)

        assertEquals(APNsProviderOutcome.ACCEPTED, sender.sendProvider(request("one")).getOrThrow().classification.outcome)
        assertEquals(APNsProviderOutcome.ACCEPTED, sender.sendProvider(request("two")).getOrThrow().classification.outcome)

        assertEquals(2, transport.requests.size)
        assertEquals(
            transport.requests[0].headers.getValue("authorization"),
            transport.requests[1].headers.getValue("authorization"),
            "one ServerAPNsSender must retain its signer/JWT cache between calls"
        )
    }

    @Test
    fun sameSenderKeepsGlobalAuthCircuitClosedUntilExplicitValidatedCredentialReplacement() = runBlocking {
        val originalP8 = validP8()
        val replacementP8 = validP8()
        val transport = RecordingTransport(
            APNsTransportResult.Response(APNsHttp2Response(403, reason = "ExpiredProviderToken", receivedAtEpochSeconds = 1_000)),
            APNsTransportResult.Response(APNsHttp2Response(403, reason = "ExpiredProviderToken", receivedAtEpochSeconds = 1_000)),
            APNsTransportResult.Response(APNsHttp2Response(200, receivedAtEpochSeconds = 1_000))
        )
        var signatureCalls = 0
        val sender = sender(
            transport,
            tokenSigner = ProductionAPNsTokenSigner(onSignatureProduced = { signatureCalls += 1 }),
            authKey = originalP8
        )

        assertEquals(APNsProviderOutcome.PROVIDER_AUTH_BLOCKED, sender.sendProvider(request("blocked-1")).getOrThrow().classification.outcome)
        assertEquals(2, transport.requests.size, "first send permits one forced-refresh retry")
        assertNotEquals(transport.requests[0].headers.getValue("authorization"), transport.requests[1].headers.getValue("authorization"))

        assertEquals(APNsProviderOutcome.PROVIDER_AUTH_BLOCKED, sender.sendProvider(request("blocked-2")).getOrThrow().classification.outcome)
        assertEquals(2, transport.requests.size, "a blocked circuit must stop before signer/transport")
        assertEquals(2, signatureCalls, "a blocked circuit must stop before a second-call signature")

        val originalVersion = providerConfig(originalP8)
        assertEquals(APNsProviderCredentialVersion.from(originalVersion), sender.replaceValidatedCredentials(originalVersion).getOrThrow())
        assertEquals(APNsProviderOutcome.PROVIDER_AUTH_BLOCKED, sender.sendProvider(request("same-version")).getOrThrow().classification.outcome)
        assertEquals(2, transport.requests.size, "same credentials must not reopen the circuit")
        assertEquals(2, signatureCalls, "same credentials must not permit a signer invocation")

        val replacement = providerConfig(replacementP8)
        val installedVersion = sender.replaceValidatedCredentials(replacement).getOrThrow()
        assertEquals(APNsProviderCredentialVersion.from(replacement), installedVersion)
        assertEquals(APNsProviderCredentialVersion.from(replacement), sender.credentialVersion())

        assertEquals(APNsProviderOutcome.ACCEPTED, sender.sendProvider(request("unblocked")).getOrThrow().classification.outcome)
        assertEquals(3, transport.requests.size, "only validated replacement credentials reopen the circuit")
    }

    @Test
    fun interleavedDeliveryAcceptanceCannotResetAnotherSendRefreshBudget() = runBlocking {
        val transport = InterleavedCorrelatedTransport()
        var signatureCalls = 0
        var signInvocations = 0
        val sender = sender(
            transport,
            tokenSigner = ProductionAPNsTokenSigner(
                onSignatureProduced = { signatureCalls += 1 },
                onSignInvoked = { signInvocations += 1 }
            )
        )

        val a = async(Dispatchers.Default) { sender.sendProvider(request("a")).getOrThrow() }
        assertTrue(transport.aSecondWriteEntered.await(5, TimeUnit.SECONDS), "A must consume its one local refresh before B")

        val b = async(Dispatchers.Default) { sender.sendProvider(request("b")).getOrThrow() }
        assertEquals(APNsProviderOutcome.ACCEPTED, b.await().classification.outcome)
        transport.releaseASecondResponse.countDown()

        assertEquals(APNsProviderOutcome.PROVIDER_AUTH_BLOCKED, a.await().classification.outcome)
        assertEquals(2, transport.callsFor("apns-a"), "A must not write a third request")
        assertEquals(1, transport.callsFor("apns-b"), "B has one independently accepted request")
        assertEquals(2, signatureCalls, "A consumed both local signature attempts")
        val signInvocationsAfterAAndB = signInvocations
        assertEquals(signatureCalls + 1, signInvocationsAfterAAndB, "A1/A2 produce signatures while B observes the JWT cache")
        assertEquals(3, signInvocationsAfterAAndB, "A1, A2, and B must each enter the signer before C")

        assertEquals(APNsProviderOutcome.PROVIDER_AUTH_BLOCKED, sender.sendProvider(request("c")).getOrThrow().classification.outcome)
        assertEquals(2, transport.callsFor("apns-a"))
        assertEquals(1, transport.callsFor("apns-b"))
        assertEquals(0, transport.callsFor("apns-c"), "the global circuit must block C before transport")
        assertEquals(2, signatureCalls, "the global circuit must block C before signing")
        assertEquals(signInvocationsAfterAAndB, signInvocations, "the global circuit must block C before even a cached signer invocation")
    }

    @Test
    fun credentialFingerprintIsDerSha256StableAcrossPemWhitespaceAndUsedBySenderVersion() {
        val original = validP8()
        val whitespaceVariant = original.replace("\n", "\n\n")
        val replacement = validP8()
        val originalFingerprint = APNsSigningKeyFingerprint.fromPkcs8Pem(original)

        assertEquals(originalFingerprint, APNsSigningKeyFingerprint.fromPkcs8Pem(whitespaceVariant))
        assertNotEquals(originalFingerprint, APNsSigningKeyFingerprint.fromPkcs8Pem(replacement))
        assertEquals(independentDerSha256(original), originalFingerprint.derSha256Hex)

        val config = providerConfig(original)
        assertEquals(APNsProviderCredentialVersion.from(config), sender(RecordingTransport(), authKey = original).credentialVersion())
    }

    @Test
    fun sameDerWithDifferentPemWhitespaceReusesOneProductionSignerJwtCacheEntry() = runBlocking {
        val original = validP8()
        val whitespaceVariant = original.replace("\n", "\n\n")
        var signatureCalls = 0
        val signer = ProductionAPNsTokenSigner(onSignatureProduced = { signatureCalls += 1 })

        val first = signer.sign(providerConfig(original), APNsProviderClock { 1_000 }).getOrThrow()
        val reused = signer.sign(providerConfig(whitespaceVariant), APNsProviderClock { 1_100 }).getOrThrow()

        assertEquals(first.authorizationValue, reused.authorizationValue)
        assertEquals(1, signatureCalls, "cache identity must be P8 DER fingerprint, never PEM representation/hashCode")
    }

    @Test
    fun realLoopbackFlushesAnEmptyEndStreamAndOnlyNegotiatesTls12OrTls13() = runBlocking {
        val config = providerConfig(validP8())
        val tlsConfiguration = APNsTransportTlsConfiguration.apnsProduction
        assertEquals(setOf("TLSv1.2", "TLSv1.3"), tlsConfiguration.enabledProtocols)
        LocalApnsHttp2TlsServer.start(APNsHttp2Response(200, receivedAtEpochSeconds = 1_000)).use { server ->
            val result = ProductionAPNsHttp2Transport(
                config, APNsProviderClock { 1_000 }, server.endpoint, server.trustAnchor,
                tlsConfiguration = tlsConfiguration
            )
                .execute(httpRequest(body = ""))

            assertIs<APNsTransportResult.Response>(result)
            val captured = server.awaitRequest()
            assertEquals("", captured.body)
            assertTrue(captured.negotiatedTlsProtocol == "TLSv1.2" || captured.negotiatedTlsProtocol == "TLSv1.3")
        }
        LocalApnsHttp2TlsServer.start(
            response = APNsHttp2Response(200, receivedAtEpochSeconds = 1_000),
            supportedTlsProtocols = arrayOf("TLSv1.1")
        ).use { tls11Only ->
            val result = ProductionAPNsHttp2Transport(
                config, APNsProviderClock { 1_000 }, tls11Only.endpoint, tls11Only.trustAnchor,
                tlsConfiguration = tlsConfiguration
            )
                .execute(httpRequest(body = "{}"))
            assertIs<APNsTransportResult.FailedBeforeWrite>(result, "the production client must not offer TLS 1.1")
        }
        Unit
    }

    @Test
    fun tlsConfigurationControlsTheActualNettyClientProtocolSet() = runBlocking {
        val config = providerConfig(validP8())
        LocalApnsHttp2TlsServer.start(
            response = APNsHttp2Response(200, receivedAtEpochSeconds = 1_000),
            supportedTlsProtocols = arrayOf("TLSv1.3")
        ).use { tls13Only ->
            val tls12Client = APNsTransportTlsConfiguration.forTests(setOf("TLSv1.2"))
            val result = ProductionAPNsHttp2Transport(
                config, APNsProviderClock { 1_000 }, tls13Only.endpoint, tls13Only.trustAnchor,
                tlsConfiguration = tls12Client
            ).execute(httpRequest(body = "{}"))

            assertIs<APNsTransportResult.FailedBeforeWrite>(result)
        }
        LocalApnsHttp2TlsServer.start(
            response = APNsHttp2Response(200, receivedAtEpochSeconds = 1_000),
            supportedTlsProtocols = arrayOf("TLSv1.3")
        ).use { tls13Only ->
            val tls13Client = APNsTransportTlsConfiguration.forTests(setOf("TLSv1.3"))
            val result = ProductionAPNsHttp2Transport(
                config, APNsProviderClock { 1_000 }, tls13Only.endpoint, tls13Only.trustAnchor,
                tlsConfiguration = tls13Client
            ).execute(httpRequest(body = "{}"))

            assertIs<APNsTransportResult.Response>(result)
            assertEquals("TLSv1.3", tls13Only.awaitRequest().negotiatedTlsProtocol)
        }
    }

    @Test
    fun realLoopbackDisconnectAfterFlushedWriteBecomesOutcomeUnknown() = runBlocking {
        val config = providerConfig(validP8())
        LocalApnsHttp2TlsServer.start(
            response = APNsHttp2Response(200, receivedAtEpochSeconds = 1_000),
            fault = LocalApnsHttp2TlsFault.CLOSE_CONNECTION_AFTER_REQUEST
        ).use { server ->
            val result = ProductionAPNsHttp2Transport(config, APNsProviderClock { 1_000 }, server.endpoint, server.trustAnchor)
                .execute(httpRequest(body = "{\"aps\":{}}"))

            assertIs<APNsTransportResult.OutcomeUnknown>(result)
            assertEquals("{\"aps\":{}}", server.awaitRequest().body)
        }
    }

    private fun sender(
        transport: APNsHttp2Transport,
        tokenSigner: APNsTokenSigner? = null,
        authKey: String = validP8(),
        deploymentEnvironmentRaw: String? = "production"
    ) = ServerAPNsSender(
        apnsKeyId = "KEY123",
        apnsTeamId = "TEAM123",
        apnsAuthKey = authKey,
        apnsBundleId = "com.guyghost.wakeve",
        apnsEnvironment = "production",
        deploymentEnvironmentRaw = deploymentEnvironmentRaw,
        tokenSigner = tokenSigner,
        clock = APNsProviderClock { 1_000 },
        transport = transport
    )

    private fun providerConfig(authKey: String) = APNsProviderConfig.create(
        keyId = "KEY123", teamId = "TEAM123", authKey = authKey,
        topic = "com.guyghost.wakeve", environment = "production"
    ).getOrThrow()

    private fun request(suffix: String = "request") = APNsProviderRequest(
        DeliveryKey("delivery-$suffix"), "apns-$suffix", "device-token", "{\"aps\":{}}", 2_000, 10, "alert"
    )

    private fun httpRequest(body: String) = APNsHttp2Request(
        authority = "api.push.apple.com:443", path = "/3/device/device-token",
        headers = mapOf("apns-topic" to "com.guyghost.wakeve"), body = body, correlationId = "transport-contract"
    )

    private fun independentDerSha256(pem: String): String {
        val der = pem.replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "").replace(Regex("\\s"), "")
        return MessageDigest.getInstance("SHA-256").digest(Base64.getDecoder().decode(der)).joinToString("") { "%02x".format(it) }
    }

    private fun validP8(): String {
        val pair = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()
        return "-----BEGIN PRIVATE KEY-----\n" + Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(pair.private.encoded) + "\n-----END PRIVATE KEY-----"
    }

    private class CountingSigner : APNsTokenSigner {
        var calls = 0
        override suspend fun sign(config: APNsProviderConfig, clock: APNsProviderClock): Result<APNsProviderToken> {
            calls += 1
            return Result.success(APNsProviderToken("bearer unexpected", clock.epochSeconds()))
        }
    }

    /** Deliberately non-refreshable seam: the sender still routes the classified REFRESH_AUTH result. */
    private class NonRefreshableSigner : APNsTokenSigner {
        override suspend fun sign(config: APNsProviderConfig, clock: APNsProviderClock): Result<APNsProviderToken> =
            Result.success(APNsProviderToken("bearer refresh-seam", clock.epochSeconds()))
    }

    private data class DeploymentEnvironmentCase(val rawValue: String?, val expectedReasonName: String)

    private class RecordingTransport(vararg scripted: APNsTransportResult) : APNsHttp2Transport {
        private val scripted = ArrayDeque(scripted.toList())
        val requests = mutableListOf<APNsHttp2Request>()

        override suspend fun execute(request: APNsHttp2Request): APNsTransportResult {
            requests += request
            return checkNotNull(scripted.removeFirstOrNull()) { "unexpected provider transport call" }
        }
    }

    /** Correlation is the APNs id derived from each distinct delivery key in [request]. */
    private class InterleavedCorrelatedTransport : APNsHttp2Transport {
        private val requestsByApnsId = linkedMapOf<String, Int>()
        val aSecondWriteEntered = CountDownLatch(1)
        val releaseASecondResponse = CountDownLatch(1)

        override suspend fun execute(request: APNsHttp2Request): APNsTransportResult {
            val apnsId = request.headers.getValue("apns-id")
            val ordinal = synchronized(requestsByApnsId) {
                (requestsByApnsId[apnsId] ?: 0).also { requestsByApnsId[apnsId] = it + 1 } + 1
            }
            return when (apnsId) {
                "apns-a" -> when (ordinal) {
                    1 -> APNsTransportResult.Response(APNsHttp2Response(403, reason = "ExpiredProviderToken", receivedAtEpochSeconds = 1_000))
                    2 -> {
                        aSecondWriteEntered.countDown()
                        check(releaseASecondResponse.await(5, TimeUnit.SECONDS)) { "B did not complete its interleaved acceptance" }
                        APNsTransportResult.Response(APNsHttp2Response(403, reason = "ExpiredProviderToken", receivedAtEpochSeconds = 1_000))
                    }
                    else -> APNsTransportResult.Response(APNsHttp2Response(200, receivedAtEpochSeconds = 1_000))
                }
                "apns-b" -> APNsTransportResult.Response(APNsHttp2Response(200, receivedAtEpochSeconds = 1_000))
                else -> error("unexpected APNs id $apnsId")
            }
        }

        fun callsFor(apnsId: String): Int = synchronized(requestsByApnsId) { requestsByApnsId[apnsId] ?: 0 }
    }
}
