package com.guyghost.wakeve.notification

import kotlinx.coroutines.runBlocking
import java.sql.Connection
import java.sql.DriverManager
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BackendNotificationProviderBoundaryHardeningRedTest {
    @Test
    fun `provider request carries the exact delivery correlation attempt lease and fence`() = runBlocking {
        BackendNotificationDurabilityTestFixture().use { fixture ->
            val registration = fixture.register("provider-envelope")
            val key = seedQueuedDelivery(fixture, registration, "provider-envelope")
            val captured = AtomicReference<BackendDeliveryProviderRequest>()
            val composition = composition(
                fixture,
                BackendDeliveryProviderPort { request ->
                    captured.set(request)
                    BackendProviderRawObservation.Http(200, null, null, "apns-envelope", 100)
                }
            )

            BackendNotificationDeliveryRecoveryScheduler(composition, "provider-worker").use {
                it.startAndDrainDueWork()
            }

            val request = assertNotNull(captured.get())
            assertEquals(key, request.deliveryKey)
            assertEquals(registration.registrationId, request.registrationId)
            assertTrue(request.correlationId.isNotBlank())
            assertEquals(0L, request.attempt)
            assertEquals("provider-worker", request.leaseHolderId)
            assertTrue(request.leaseVersion > 0)
            assertTrue(request.leaseFencingToken > 0)
        }
    }

    @Test
    fun `APNs status and reason matrix is exact and durable HTTP status is closed`() {
        val cases = listOf(
            MatrixCase(200, null, BackendDurableProviderOutcome.ACCEPTED, BackendPersistedProviderReason.HTTP_200),
            MatrixCase(400, "BadDeviceToken", BackendDurableProviderOutcome.INVALID_TOKEN, BackendPersistedProviderReason.TOKEN_INVALID),
            MatrixCase(400, "DeviceTokenNotForTopic", BackendDurableProviderOutcome.INVALID_TOKEN, BackendPersistedProviderReason.TOKEN_INVALID),
            MatrixCase(400, "IdleTimeout", BackendDurableProviderOutcome.RETRY, BackendPersistedProviderReason.IDLE_TIMEOUT),
            MatrixCase(400, "BadCollapseId", BackendDurableProviderOutcome.REJECTED_PAYLOAD, BackendPersistedProviderReason.PAYLOAD_REJECTED),
            MatrixCase(400, "BadMessageId", BackendDurableProviderOutcome.REJECTED_PAYLOAD, BackendPersistedProviderReason.PAYLOAD_REJECTED),
            MatrixCase(400, "BadTopic", BackendDurableProviderOutcome.REJECTED_PAYLOAD, BackendPersistedProviderReason.PAYLOAD_REJECTED),
            MatrixCase(400, "BadPath", BackendDurableProviderOutcome.REJECTED_PAYLOAD, BackendPersistedProviderReason.PAYLOAD_REJECTED),
            MatrixCase(400, "MethodNotAllowed", BackendDurableProviderOutcome.REJECTED_PAYLOAD, BackendPersistedProviderReason.PAYLOAD_REJECTED),
            MatrixCase(400, "PayloadEmpty", BackendDurableProviderOutcome.REJECTED_PAYLOAD, BackendPersistedProviderReason.PAYLOAD_REJECTED),
            MatrixCase(400, "PayloadTooLarge", BackendDurableProviderOutcome.REJECTED_PAYLOAD, BackendPersistedProviderReason.PAYLOAD_REJECTED),
            MatrixCase(400, "BadPriority", BackendDurableProviderOutcome.REJECTED_PAYLOAD, BackendPersistedProviderReason.PAYLOAD_REJECTED),
            MatrixCase(400, "BadExpirationDate", BackendDurableProviderOutcome.REJECTED_PAYLOAD, BackendPersistedProviderReason.PAYLOAD_REJECTED),
            MatrixCase(400, "MissingTopic", BackendDurableProviderOutcome.REJECTED_PAYLOAD, BackendPersistedProviderReason.PAYLOAD_REJECTED),
            MatrixCase(400, "future-provider-text", BackendDurableProviderOutcome.UNKNOWN_OUTCOME, BackendPersistedProviderReason.UNKNOWN_PROVIDER_REASON),
            MatrixCase(403, "ExpiredProviderToken", BackendDurableProviderOutcome.REFRESH_AUTH, BackendPersistedProviderReason.PROVIDER_AUTH_REJECTED),
            MatrixCase(403, "InvalidProviderToken", BackendDurableProviderOutcome.PROVIDER_AUTH_BLOCKED, BackendPersistedProviderReason.PROVIDER_AUTH_REJECTED),
            MatrixCase(410, "ExpiredToken", BackendDurableProviderOutcome.INVALID_TOKEN, BackendPersistedProviderReason.TOKEN_INVALID),
            MatrixCase(410, "Unregistered", BackendDurableProviderOutcome.INVALID_TOKEN, BackendPersistedProviderReason.TOKEN_INVALID),
            MatrixCase(410, "future-provider-text", BackendDurableProviderOutcome.UNKNOWN_OUTCOME, BackendPersistedProviderReason.UNKNOWN_PROVIDER_REASON),
            MatrixCase(429, "TooManyRequests", BackendDurableProviderOutcome.RETRY, BackendPersistedProviderReason.TOO_MANY_REQUESTS),
            MatrixCase(429, "TooManyProviderTokenUpdates", BackendDurableProviderOutcome.PROVIDER_AUTH_BLOCKED, BackendPersistedProviderReason.PROVIDER_AUTH_REJECTED),
            MatrixCase(500, null, BackendDurableProviderOutcome.RETRY, BackendPersistedProviderReason.HTTP_5XX),
            MatrixCase(503, null, BackendDurableProviderOutcome.RETRY, BackendPersistedProviderReason.HTTP_5XX),
            MatrixCase(404, "BadPath", BackendDurableProviderOutcome.REJECTED_PAYLOAD, BackendPersistedProviderReason.PAYLOAD_REJECTED),
            MatrixCase(405, "MethodNotAllowed", BackendDurableProviderOutcome.REJECTED_PAYLOAD, BackendPersistedProviderReason.PAYLOAD_REJECTED),
            MatrixCase(413, "PayloadTooLarge", BackendDurableProviderOutcome.REJECTED_PAYLOAD, BackendPersistedProviderReason.PAYLOAD_REJECTED),
            MatrixCase(599, "future-provider-text", BackendDurableProviderOutcome.UNKNOWN_OUTCOME, BackendPersistedProviderReason.UNKNOWN_PROVIDER_REASON)
        )
        cases.forEach { case ->
            val classified = BackendDeliveryObservationClassifier.classify(
                BackendProviderRawObservation.Http(case.status, case.reason, null, "apns-matrix", 100),
                BackendDeliveryRetryContext(DeliveryKey("matrix-${case.reason}"), 100, 1_000, 0, 5),
                BackendDeliveryJitterSource { _, _ -> 0.5 }
            )
            assertEquals(case.outcome, classified.outcome, "${case.status}/${case.reason}")
            assertEquals(case.persistedReason, classified.reason, "${case.status}/${case.reason}")
            assertEquals(case.status, classified.persistedHttpStatus(), "durable status remains typed numeric")
        }

        val transportCases = listOf(
            Triple(BackendProviderTransportPhase.BEFORE_WRITE, BackendDurableProviderOutcome.RETRY, BackendPersistedProviderReason.TRANSPORT_BEFORE_WRITE),
            Triple(BackendProviderTransportPhase.MAY_HAVE_WRITTEN, BackendDurableProviderOutcome.UNKNOWN_OUTCOME, BackendPersistedProviderReason.TRANSPORT_OUTCOME_UNKNOWN)
        )
        transportCases.forEach { (phase, outcome, reason) ->
            val classified = BackendDeliveryObservationClassifier.classify(
                BackendProviderRawObservation.Transport(phase),
                BackendDeliveryRetryContext(DeliveryKey("matrix-$phase"), 100, 1_000, 0, 5),
                BackendDeliveryJitterSource { _, _ -> 0.5 }
            )
            assertEquals(outcome, classified.outcome, phase.name)
            assertEquals(reason, classified.reason, phase.name)
            assertNull(classified.persistedHttpStatus(), phase.name)
        }
    }

    @Test
    fun `legacy migration is observable without public mutators and durable provider reason stays typed`() = runBlocking {
        val publicStoreMethods = BackendNotificationDeliveryStore::class.java.methods.map { it.name }.toSet()
        assertFalse("enqueue" in publicStoreMethods)
        assertFalse("acquireLease" in publicStoreMethods)
        assertFalse("recordRetry" in publicStoreMethods)

        BackendNotificationDurabilityTestFixture().use { fixture ->
            val registration = fixture.register("observable-legacy-migration")
            createLegacyDeliverySchema(
                fixture,
                registration.installationId,
                "legacy-migrated-delivery"
            )
            fixture.deliveryFactory.open().close()
            DriverManager.getConnection("jdbc:sqlite:${fixture.databasePath}").use { connection ->
                connection.prepareStatement(
                    "SELECT device_registration_id, status, attempt, next_attempt_at_epoch_seconds " +
                        "FROM notification_delivery WHERE delivery_key = ?"
                ).use { statement ->
                    statement.setString(1, "legacy-migrated-delivery")
                    statement.executeQuery().use { rows ->
                        assertTrue(rows.next())
                        assertEquals(registration.registrationId, rows.getString("device_registration_id"))
                        assertEquals(BackendDeliveryStatus.RETRY.name, rows.getString("status"))
                        assertEquals(2, rows.getInt("attempt"))
                        assertEquals(500L, rows.getLong("next_attempt_at_epoch_seconds"))
                        assertFalse(rows.next())
                    }
                }
                assertFalse(connection.tableExists("notification_recipient_installation"))
            }
        }

        BackendNotificationDurabilityTestFixture().use { fixture ->
            val registration = fixture.register("typed-provider-reason")
            val key = seedQueuedDelivery(fixture, registration, "typed-provider-reason")
            val worker = composition(
                fixture,
                BackendDeliveryProviderPort {
                    BackendProviderRawObservation.Http(400, "BadPath", null, "apns-typed-reason", 100)
                }
            )
            BackendNotificationDeliveryRecoveryScheduler(worker, "typed-reason-worker").use {
                it.startAndDrainDueWork()
            }
            fixture.deliveryFactory.open().use { store ->
                val delivery = assertNotNull(store.delivery(key))
                val typedReason: BackendPersistedProviderReason? = delivery.providerReason
                assertEquals(BackendPersistedProviderReason.PAYLOAD_REJECTED, typedReason)
                assertEquals(BackendDeliveryStatus.REJECTED_PAYLOAD, delivery.status)
            }
        }
    }

    private fun composition(
        fixture: BackendNotificationDurabilityTestFixture,
        provider: BackendDeliveryProviderPort
    ) = BackendNotificationDeliveryWorkerComposition(
        deliveryStoreFactory = fixture.deliveryFactory,
        registrationStoreFactory = fixture.registrationFactory,
        authority = BackendDeliveryAuthority.OUTBOX_V2,
        clock = BackendDeliveryWorkerClock { 100 },
        policy = BackendDeliveryPolicyPort { BackendDeliveryPolicyDecision.ALLOW },
        tokenAvailability = BackendDeliveryTokenAvailabilityPort { true },
        credentials = object : BackendDeliveryCredentialPort {
            override suspend fun credentialVersion() = "credential-v1"
            override suspend fun refreshAfterProviderRejection(expectedVersion: String) = true
        },
        provider = provider,
        jitter = BackendDeliveryJitterSource { _, _ -> 0.5 },
        faultInjector = BackendDeliveryWorkerFaultInjector { }
    )

    private suspend fun seedQueuedDelivery(
        fixture: BackendNotificationDurabilityTestFixture,
        registration: BackendDeviceRegistration,
        identity: String
    ): DeliveryKey {
        return BackendNotificationIngestionService(
            storeFactory = fixture.deliveryFactory,
            faultInjector = BackendNotificationIngestionFaultInjector { },
            committedPort = BackendNotificationIngestionCommittedPort { }
        ).ingest(
            BackendNotificationIngestionCommand(
                domainEventId = "provider-boundary-$identity",
                effectType = "DATE_CONFIRMED",
                schemaVersion = 1,
                logicalNotificationId = "logical-provider-boundary-$identity",
                recipients = listOf(
                    BackendNotificationRecipientIntent(
                        participantId = "participant-provider-boundary-$identity",
                        channel = "push",
                        provider = "apns",
                        registrationIds = listOf(registration.registrationId),
                        expiresAtEpochSeconds = 1_000
                    )
                )
            )
        ).deliveryKeys.single()
    }

    private fun createLegacyDeliverySchema(
        fixture: BackendNotificationDurabilityTestFixture,
        installationId: String,
        deliveryKey: String
    ) {
        DriverManager.getConnection("jdbc:sqlite:${fixture.databasePath}").use { connection ->
            connection.createStatement().use { statement ->
                statement.execute(
                    "CREATE TABLE notification_recipient (" +
                        "recipient_key TEXT PRIMARY KEY NOT NULL, effect_key TEXT NOT NULL, " +
                        "status TEXT NOT NULL, expires_at_epoch_seconds INTEGER NOT NULL)"
                )
                statement.execute(
                    "CREATE TABLE notification_recipient_installation (" +
                        "recipient_key TEXT NOT NULL, installation_id TEXT NOT NULL, " +
                        "PRIMARY KEY(recipient_key, installation_id))"
                )
                statement.execute(
                    "CREATE TABLE notification_delivery (" +
                        "delivery_key TEXT PRIMARY KEY NOT NULL, recipient_key TEXT NOT NULL, " +
                        "installation_id TEXT NOT NULL, provider TEXT NOT NULL, status TEXT NOT NULL, " +
                        "attempt INTEGER NOT NULL, next_attempt_at_epoch_seconds INTEGER, " +
                        "expires_at_epoch_seconds INTEGER NOT NULL, lease_owner TEXT, " +
                        "lease_expires_at_epoch_seconds INTEGER)"
                )
            }
            connection.prepareStatement(
                "INSERT INTO notification_recipient(recipient_key, effect_key, status, expires_at_epoch_seconds) " +
                    "VALUES ('legacy-recipient', 'legacy-effect', 'TARGETED', 1000)"
            ).use { it.executeUpdate() }
            connection.prepareStatement(
                "INSERT INTO notification_recipient_installation(recipient_key, installation_id) VALUES (?, ?)"
            ).use { statement ->
                statement.setString(1, "legacy-recipient")
                statement.setString(2, installationId)
                statement.executeUpdate()
            }
            connection.prepareStatement(
                "INSERT INTO notification_delivery(" +
                    "delivery_key, recipient_key, installation_id, provider, status, attempt, " +
                    "next_attempt_at_epoch_seconds, expires_at_epoch_seconds, lease_owner, lease_expires_at_epoch_seconds" +
                    ") VALUES (?, 'legacy-recipient', ?, 'apns', 'RETRY', 2, 500, 1000, 'legacy-worker', 600)"
            ).use { statement ->
                statement.setString(1, deliveryKey)
                statement.setString(2, installationId)
                statement.executeUpdate()
            }
        }
    }

    private fun Connection.tableExists(table: String): Boolean = createStatement().use { statement ->
        statement.executeQuery(
            "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = '$table'"
        ).use { it.next() }
    }

    private data class MatrixCase(
        val status: Int,
        val reason: String?,
        val outcome: BackendDurableProviderOutcome,
        val persistedReason: BackendPersistedProviderReason
    )

    private fun BackendClassifiedProviderObservation.persistedHttpStatus(): Int? = httpStatus
}
