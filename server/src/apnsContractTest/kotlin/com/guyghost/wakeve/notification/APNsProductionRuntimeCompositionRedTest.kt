package com.guyghost.wakeve.notification

import com.guyghost.wakeve.createApplicationAPNsSender
import java.nio.charset.StandardCharsets.UTF_8
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.Base64
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Compile-RED contracts for tasks 5.1/5.2.
 *
 * The production application composition creates a no-argument [ServerAPNsSender]. Tests may
 * replace its process runtime only through a scoped loopback override; the sender itself never
 * receives a signer, transport, or runtime factory from Application.
 */
class APNsProductionRuntimeCompositionRedTest {
    @Test
    fun productionSignerCreatesVerifiableEs256JwtFromP8Key() = runBlocking {
        val signing = signingFixture()
        val config = providerConfig(authKey = signing.pem)

        val token = ProductionAPNsTokenSigner(refreshSafetyWindowSeconds = 300)
            .sign(config, APNsProviderClock { 1_000 })
            .getOrThrow()
        val jwt = token.authorizationValue.removePrefix("bearer ")
        val parts = jwt.split('.')

        assertEquals(3, parts.size)
        assertTrue(String(Base64.getUrlDecoder().decode(parts[0]), UTF_8).contains("\"alg\":\"ES256\""))
        assertTrue(String(Base64.getUrlDecoder().decode(parts[0]), UTF_8).contains("\"kid\":\"KEY123\""))
        assertTrue(String(Base64.getUrlDecoder().decode(parts[1]), UTF_8).contains("\"iss\":\"TEAM123\""))
        assertTrue(String(Base64.getUrlDecoder().decode(parts[1]), UTF_8).contains("\"iat\":1000"))

        val verifier = Signature.getInstance("SHA256withECDSAinP1363Format")
        verifier.initVerify(signing.keyPair.public)
        verifier.update("${parts[0]}.${parts[1]}".toByteArray(UTF_8))
        assertTrue(verifier.verify(Base64.getUrlDecoder().decode(parts[2])))
    }

    @Test
    fun productionSignerReusesThenRotatesInsideTheExplicitFiveMinuteSafetyWindow() = runBlocking {
        val config = providerConfig(authKey = signingFixture().pem)
        val signer = ProductionAPNsTokenSigner(refreshSafetyWindowSeconds = 300)

        val first = signer.sign(config, APNsProviderClock { 1_000 }).getOrThrow()
        val reused = signer.sign(config, APNsProviderClock { 4_299 }).getOrThrow()
        val rotated = signer.sign(config, APNsProviderClock { 4_300 }).getOrThrow()

        assertEquals(first.authorizationValue, reused.authorizationValue, "JWT remains cached before the safety window")
        assertNotEquals(first.authorizationValue, rotated.authorizationValue, "JWT rotates five minutes before one-hour age")
        assertEquals(4_300, rotated.issuedAtEpochSeconds)
    }

    @Test
    fun productionSignerCacheIsInvalidatedByKeyIdTeamIdOrP8Changes() = runBlocking {
        val firstSigning = signingFixture()
        val replacementSigning = signingFixture()
        val base = providerConfig(authKey = firstSigning.pem)

        suspend fun token(config: APNsProviderConfig, signer: ProductionAPNsTokenSigner, now: Long) =
            signer.sign(config, APNsProviderClock { now }).getOrThrow().authorizationValue

        val keyIdSigner = ProductionAPNsTokenSigner(refreshSafetyWindowSeconds = 300)
        val keyIdBase = token(base, keyIdSigner, 1_000)
        val keyIdChanged = token(providerConfig(keyId = "KEY456", authKey = firstSigning.pem), keyIdSigner, 1_100)
        assertNotEquals(keyIdBase, keyIdChanged)

        val teamSigner = ProductionAPNsTokenSigner(refreshSafetyWindowSeconds = 300)
        val teamBase = token(base, teamSigner, 1_000)
        val teamChanged = token(providerConfig(teamId = "TEAM456", authKey = firstSigning.pem), teamSigner, 1_100)
        assertNotEquals(teamBase, teamChanged)

        val keyMaterialSigner = ProductionAPNsTokenSigner(refreshSafetyWindowSeconds = 300)
        val keyMaterialBase = token(base, keyMaterialSigner, 1_000)
        val keyMaterialChanged = token(providerConfig(authKey = replacementSigning.pem), keyMaterialSigner, 1_100)
        assertNotEquals(keyMaterialBase, keyMaterialChanged)
    }

    @Test
    fun senderRefreshesOnceWithNewJwtThenBlocksOnSecondExpiredProviderTokenWithoutThirdWrite() = runBlocking {
        val transport = SequencedTransport(
            APNsHttp2Response(403, reason = "ExpiredProviderToken", receivedAtEpochSeconds = 1_100),
            APNsHttp2Response(403, reason = "ExpiredProviderToken", receivedAtEpochSeconds = 1_100)
        )
        val sender = ServerAPNsSender(
            apnsKeyId = "KEY123",
            apnsTeamId = "TEAM123",
            apnsAuthKey = signingFixture().pem,
            apnsBundleId = "com.guyghost.wakeve",
            apnsEnvironment = "production",
            deploymentEnvironmentRaw = "production",
            tokenSigner = ProductionAPNsTokenSigner(refreshSafetyWindowSeconds = 300),
            clock = APNsProviderClock { 1_100 },
            transport = transport
        )

        val result = sender.sendProvider(providerRequest()).getOrThrow()

        assertEquals(APNsProviderOutcome.PROVIDER_AUTH_BLOCKED, result.classification.outcome)
        assertEquals(2, transport.requests.size, "only one forced refresh write is permitted")
        assertNotEquals(
            transport.requests[0].headers.getValue("authorization"),
            transport.requests[1].headers.getValue("authorization"),
            "the retry after ExpiredProviderToken must force a new JWT inside the normal cache window"
        )
    }

    @Test
    fun validProductionConfigurationReportsReadyWithoutExposingSecrets() {
        val privateKey = signingFixture().pem
        val readiness = senderForReadiness(authKey = privateKey).readiness()

        assertEquals(APNsProviderReadinessStatus.READY, readiness.status)
        assertTrue(readiness.reasons.isEmpty())
        assertFalse(readiness.toString().contains(privateKey))
    }

    @Test
    fun missingOrMalformedProductionConfigurationReportsSanitizedNotReadyReasons() {
        val validP8 = signingFixture().pem
        val secretMarker = "MALFORMED_PRIVATE_KEY_SECRET"
        val cases = listOf(
            APNsProviderReadinessReason.MISSING_KEY_ID to senderForReadiness(keyId = null, authKey = validP8),
            APNsProviderReadinessReason.MISSING_TEAM_ID to senderForReadiness(teamId = null, authKey = validP8),
            APNsProviderReadinessReason.MISSING_AUTH_KEY to senderForReadiness(authKey = null),
            APNsProviderReadinessReason.INVALID_AUTH_KEY to senderForReadiness(authKey = secretMarker),
            APNsProviderReadinessReason.MISSING_TOPIC to senderForReadiness(topic = null, authKey = validP8),
            APNsProviderReadinessReason.MISSING_ENVIRONMENT to senderForReadiness(apnsEnvironment = null, authKey = validP8),
            APNsProviderReadinessReason.PRODUCTION_REQUIRES_PRODUCTION_APNS to senderForReadiness(
                apnsEnvironment = "sandbox",
                authKey = validP8,
                deploymentEnvironment = APNsDeploymentEnvironment.PRODUCTION
            )
        )

        cases.forEach { (reason, sender) ->
            val readiness = sender.readiness()
            assertEquals(APNsProviderReadinessStatus.NOT_READY, readiness.status, reason.name)
            assertTrue(reason in readiness.reasons, "missing readiness reason $reason")
            assertFalse(readiness.toString().contains(secretMarker), "readiness diagnostics leaked invalid key material")
        }
    }

    @Test
    fun applicationDefaultSenderUsesConcreteProductionRuntimeAgainstLoopback() = runBlocking {
        assertApplicationDefaultRuntimeRoundTrip(
            environment = "production",
            deploymentEnvironment = APNsDeploymentEnvironment.PRODUCTION,
            expectedAuthority = "api.push.apple.com:443"
        )
    }

    @Test
    fun applicationDefaultSenderUsesConcreteSandboxRuntimeAgainstLoopback() = runBlocking {
        assertApplicationDefaultRuntimeRoundTrip(
            environment = "sandbox",
            deploymentEnvironment = APNsDeploymentEnvironment.DEVELOPMENT,
            expectedAuthority = "api.sandbox.push.apple.com:443"
        )
    }

    @Test
    fun loopbackOverrideIsTestScopedRestoredOnCloseAndRejectsOverlap() {
        val config = providerConfig(authKey = signingFixture().pem)
        assertFalse(APNsProviderRuntimeTestOverride.isInstalledForTests())
        LocalApnsHttp2TlsServer.start(
            response = APNsHttp2Response(statusCode = 200, apnsId = "apns-local", receivedAtEpochSeconds = 1_000)
        ).use { server ->
            val scope = APNsProviderRuntimeTestOverride.installLoopbackForTests(
                config = config,
                deploymentEnvironment = APNsDeploymentEnvironment.DEVELOPMENT,
                endpoint = server.endpoint,
                trustAnchor = server.trustAnchor,
                clock = APNsProviderClock { 1_000 }
            )
            assertTrue(APNsProviderRuntimeTestOverride.isInstalledForTests())
            assertFailsWith<IllegalStateException> {
                APNsProviderRuntimeTestOverride.installLoopbackForTests(
                    config, APNsDeploymentEnvironment.DEVELOPMENT, server.endpoint, server.trustAnchor, APNsProviderClock { 1_000 }
                )
            }
            scope.close()
            assertFalse(APNsProviderRuntimeTestOverride.isInstalledForTests())
        }
    }

    @Test
    fun noArgumentApplicationCompositionHasNoPublicOverrideConfiguration() {
        assertFalse(APNsProviderRuntimeTestOverride.isInstalledForTests())
        assertIs<ServerAPNsSender>(createApplicationAPNsSender())
    }

    @Test
    fun productionRuntimeFactoryAlwaysBuildsConcreteRuntimeFromValidatedConfig() {
        val config = providerConfig(authKey = signingFixture().pem)

        val runtime = APNsProviderRuntimeFactory.create(config, APNsProviderClock { 1_000 }).getOrThrow()

        assertNotNull(runtime.signer)
        assertNotNull(runtime.transport)
        assertIs<ProductionAPNsTokenSigner>(runtime.signer)
        assertIs<ProductionAPNsHttp2Transport>(runtime.transport)
    }

    private suspend fun assertApplicationDefaultRuntimeRoundTrip(
        environment: String,
        deploymentEnvironment: APNsDeploymentEnvironment,
        expectedAuthority: String
    ) {
        val config = providerConfig(environment = environment, authKey = signingFixture().pem)
        LocalApnsHttp2TlsServer.start(
            response = APNsHttp2Response(statusCode = 200, apnsId = "apns-local", receivedAtEpochSeconds = 1_000)
        ).use { server ->
            APNsProviderRuntimeTestOverride.installLoopbackForTests(
                config = config,
                deploymentEnvironment = deploymentEnvironment,
                endpoint = server.endpoint,
                trustAnchor = server.trustAnchor,
                clock = APNsProviderClock { 1_000 }
            ).use {
                val sender = createApplicationAPNsSender()
                assertIs<ServerAPNsSender>(sender)
                val result = sender.sendProvider(providerRequest()).getOrThrow()
                val request = server.awaitRequest()

                assertEquals(APNsProviderOutcome.ACCEPTED, result.classification.outcome)
                assertEquals("h2", request.negotiatedApplicationProtocol)
                assertTrue(
                    request.negotiatedTlsProtocol == "TLSv1.2" || request.negotiatedTlsProtocol == "TLSv1.3",
                    "APNs transport negotiated unsupported TLS ${request.negotiatedTlsProtocol}"
                )
                assertEquals("POST", request.method)
                assertEquals("/3/device/device-token", request.path)
                assertEquals(expectedAuthority, request.authority)
                assertEquals("com.guyghost.wakeve", request.headers.getValue("apns-topic"))
                assertEquals("alert", request.headers.getValue("apns-push-type"))
                assertEquals("apns-h2", request.headers.getValue("apns-id"))
                assertEquals("2000", request.headers.getValue("apns-expiration"))
                assertEquals("10", request.headers.getValue("apns-priority"))
                assertTrue(request.headers.getValue("authorization").startsWith("bearer "))
                assertEquals("{\"aps\":{\"alert\":\"hello\"}}", request.body)
            }
        }
    }

    private fun senderForReadiness(
        keyId: String? = "KEY123",
        teamId: String? = "TEAM123",
        authKey: String? = signingFixture().pem,
        topic: String? = "com.guyghost.wakeve",
        apnsEnvironment: String? = "production",
        deploymentEnvironment: APNsDeploymentEnvironment = APNsDeploymentEnvironment.PRODUCTION
    ) = ServerAPNsSender(
        apnsKeyId = keyId,
        apnsTeamId = teamId,
        apnsAuthKey = authKey,
        apnsBundleId = topic,
        apnsEnvironment = apnsEnvironment,
        deploymentEnvironment = deploymentEnvironment,
        deploymentEnvironmentRaw = when (deploymentEnvironment) {
            APNsDeploymentEnvironment.PRODUCTION -> "production"
            APNsDeploymentEnvironment.DEVELOPMENT -> "development"
        }
    )

    private fun providerConfig(
        keyId: String = "KEY123",
        teamId: String = "TEAM123",
        authKey: String,
        environment: String = "production"
    ) = APNsProviderConfig.create(
        keyId = keyId,
        teamId = teamId,
        authKey = authKey,
        topic = "com.guyghost.wakeve",
        environment = environment
    ).getOrThrow()

    private fun providerRequest() = APNsProviderRequest(
        deliveryKey = DeliveryKey("delivery-h2"),
        apnsId = "apns-h2",
        deviceToken = "device-token",
        payload = "{\"aps\":{\"alert\":\"hello\"}}",
        expirationEpochSeconds = 2_000,
        priority = 10,
        pushType = "alert"
    )

    private fun signingFixture(): SigningFixture {
        val keyPair = KeyPairGenerator.getInstance("EC").apply {
            initialize(ECGenParameterSpec("secp256r1"))
        }.generateKeyPair()
        val pem = "-----BEGIN PRIVATE KEY-----\n" +
            Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(keyPair.private.encoded) +
            "\n-----END PRIVATE KEY-----"
        return SigningFixture(keyPair, pem)
    }

    private data class SigningFixture(val keyPair: KeyPair, val pem: String)

    private class SequencedTransport(vararg responses: APNsHttp2Response) : APNsHttp2Transport {
        private val queuedResponses = ArrayDeque(responses.toList())
        val requests = mutableListOf<APNsHttp2Request>()

        override suspend fun execute(request: APNsHttp2Request): APNsTransportResult {
            requests += request
            return APNsTransportResult.Response(checkNotNull(queuedResponses.removeFirstOrNull()))
        }
    }
}
