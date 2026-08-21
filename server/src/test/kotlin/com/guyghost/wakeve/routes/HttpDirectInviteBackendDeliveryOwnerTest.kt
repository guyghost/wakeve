package com.guyghost.wakeve.routes

import com.guyghost.wakeve.invitationexperience.DirectInviteDeliveryBinding
import com.guyghost.wakeve.invitationexperience.DirectInviteDeliveryEnvelope
import com.guyghost.wakeve.invitationexperience.DirectInviteDeliveryRequest
import com.guyghost.wakeve.invitationexperience.DirectInviteDeliveryResult
import com.guyghost.wakeve.invitationexperience.DirectInviteRecipientOutcome
import com.guyghost.wakeve.invitationexperience.RecipientKey
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.http.ContentType
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.serialization.kotlinx.json.json
import java.net.ServerSocket
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class HttpDirectInviteBackendDeliveryOwnerTest {

    @Test
    fun `production HTTP owner sends only protected envelope and trusts exact provider acceptance`() = runBlocking {
        val port = ServerSocket(0).use { it.localPort }
        val received = CompletableDeferred<ProviderObservation>()
        val server = embeddedServer(Netty, host = "127.0.0.1", port = port) {
            routing {
                post("/deliver") {
                    val body = call.receiveText()
                    received.complete(
                        ProviderObservation(
                            authorization = call.request.headers["Authorization"],
                            body = body
                        )
                    )
                    call.respondText(
                        text = """
                            {
                              "batchId":"batch-http-owner",
                              "operationId":"operation-http-owner",
                              "outcomes":[{
                                "recipientKey":"hmac-v1-0123456789abcdef0123456789abcdef",
                                "status":"PROVIDER_ACCEPTED",
                                "invitationId":"provider-invitation-1"
                              }]
                            }
                        """.trimIndent(),
                        contentType = ContentType.Application.Json
                    )
                }
            }
        }.start(wait = false)
        val client = HttpClient(CIO) {
            install(ClientContentNegotiation) {
                json(Json { ignoreUnknownKeys = false })
            }
        }

        try {
            val owner = instantiateProductionOwner(
                endpoint = "http://127.0.0.1:$port/deliver",
                credential = "provider-secret",
                client = client
            )
            val recipientKey = RecipientKey("hmac-v1-0123456789abcdef0123456789abcdef")
            val binding = DirectInviteDeliveryBinding(
                eventId = "event-http-owner",
                actorId = "organizer-http-owner",
                accessRevision = 7,
                batchId = "batch-http-owner",
                operationId = "operation-http-owner"
            )
            val ciphertext = "enc:v1:cmVjaXBpZW50LWlzLWFlYWQtcHJvdGVjdGVk"
            val result = owner.dispatch(
                DirectInviteDeliveryRequest(
                    binding = binding,
                    envelopes = setOf(
                        DirectInviteDeliveryEnvelope(
                            binding = binding,
                            recipientKey = recipientKey,
                            ciphertext = ciphertext,
                            keyVersion = 1,
                            expiresAt = "2030-09-11T20:00:00Z"
                        )
                    )
                )
            )

            val observation = received.await()
            assertEquals("Bearer provider-secret", observation.authorization)
            assertTrue(observation.body.contains(recipientKey.value), observation.body)
            assertTrue(observation.body.contains(ciphertext), observation.body)
            assertFalse(observation.body.contains("lea@example.com"), observation.body)
            assertFalse(observation.body.contains("rawRecipient"), observation.body)

            val acknowledged = assertIs<DirectInviteDeliveryResult.Acknowledged>(result)
            assertEquals(binding.batchId, acknowledged.batchId)
            assertEquals(binding.operationId, acknowledged.operationId)
            val outcome = acknowledged.outcomesByRecipientKey.getValue(recipientKey)
            assertEquals(
                "provider-invitation-1",
                assertIs<DirectInviteRecipientOutcome.ServerAccepted>(outcome).invitationId
            )
        } finally {
            client.close()
            server.stop(gracePeriodMillis = 0, timeoutMillis = 1_000)
        }
    }

    private fun instantiateProductionOwner(
        endpoint: String,
        credential: String,
        client: HttpClient
    ): DirectInviteBackendDeliveryOwner {
        val constructor = HttpDirectInviteBackendDeliveryOwner::class.java
            .getDeclaredConstructor(
                String::class.java,
                String::class.java,
                String::class.java,
                Int::class.javaPrimitiveType,
                HttpClient::class.java
            )
        constructor.isAccessible = true
        return constructor.newInstance(
            endpoint,
            credential,
            "cHJvdmlkZXItc2VhbGluZy1wdWJsaWMta2V5",
            1,
            client
        )
    }

    private data class ProviderObservation(
        val authorization: String?,
        val body: String
    )
}
