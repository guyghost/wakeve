package com.guyghost.wakeve.notification

import kotlinx.coroutines.runBlocking
import java.sql.DriverManager
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class BackendNotificationEffectAggregateReviewRedTest {
    @Test
    fun `effect dispatch vocabulary is closed and independent from decision sync`() {
        assertEquals(setOf("ACKNOWLEDGED"), BackendDecisionSyncStatus.entries.map { it.name }.toSet())
        assertEquals(
            setOf(
                "NOT_DISPATCHED", "PENDING_RECIPIENT", "QUEUED",
                "PARTIALLY_DISPATCHED", "DISPATCHED", "TERMINAL_FAILURE"
            ),
            BackendEffectDispatchStatus.entries.map { it.name }.toSet()
        )
    }

    @Test
    fun `pending target projects pending recipient while decision remains acknowledged`() = runBlocking {
        BackendNotificationDurabilityTestFixture().use { fixture ->
            val logicalId = "logical-aggregate-pending-target"
            ingest(fixture, logicalId, emptyList())
            assertAggregate(fixture, logicalId, "ACKNOWLEDGED", "PENDING_RECIPIENT")
        }
    }

    @Test
    fun `queued delivery projects queued while decision remains acknowledged`() = runBlocking {
        BackendNotificationDurabilityTestFixture().use { fixture ->
            val registration = fixture.register("aggregate-queued")
            val logicalId = "logical-aggregate-queued"
            ingest(fixture, logicalId, listOf(registration.registrationId))
            assertAggregate(fixture, logicalId, "ACKNOWLEDGED", "QUEUED")
        }
    }

    @Test
    fun `accepted and token pending mix projects partially dispatched without changing decision sync`() = runBlocking {
        BackendNotificationDurabilityTestFixture().use { fixture ->
            val acceptedRegistration = fixture.register("aggregate-mix-accepted")
            val waitingRegistration = fixture.register("aggregate-mix-waiting")
            val logicalId = "logical-aggregate-mix"
            val receipt = ingest(
                fixture,
                logicalId,
                listOf(acceptedRegistration.registrationId, waitingRegistration.registrationId)
            )
            val providerCalls = AtomicInteger()
            val worker = composition(
                fixture,
                BackendDeliveryTokenAvailabilityPort { it == acceptedRegistration.registrationId },
                BackendDeliveryProviderPort {
                    providerCalls.incrementAndGet()
                    BackendProviderRawObservation.Http(200, null, null, "apns-aggregate-mix", 100)
                }
            )
            BackendNotificationDeliveryRecoveryScheduler(worker, "aggregate-mix-worker").use {
                it.startAndDrainDueWork()
            }
            assertEquals(1, providerCalls.get())
            val states = receipt.deliveryKeys.map { assertNotNull(worker.current(it)).state }.toSet()
            assertEquals(
                setOf(BackendDurableDeliveryState.ACCEPTED_BY_APNS, BackendDurableDeliveryState.AWAITING_TOKEN),
                states
            )
            assertAggregate(fixture, logicalId, "ACKNOWLEDGED", "PARTIALLY_DISPATCHED")
        }
    }

    @Test
    fun `all accepted deliveries project dispatched without changing decision sync`() = runBlocking {
        BackendNotificationDurabilityTestFixture().use { fixture ->
            val registrations = listOf(
                fixture.register("aggregate-all-first"),
                fixture.register("aggregate-all-second")
            )
            val logicalId = "logical-aggregate-all"
            val receipt = ingest(fixture, logicalId, registrations.map { it.registrationId })
            val worker = composition(
                fixture,
                BackendDeliveryTokenAvailabilityPort { true },
                BackendDeliveryProviderPort {
                    BackendProviderRawObservation.Http(200, null, null, "apns-aggregate-all", 100)
                }
            )
            BackendNotificationDeliveryRecoveryScheduler(worker, "aggregate-all-worker").use {
                it.startAndDrainDueWork()
            }
            assertEquals(
                setOf(BackendDurableDeliveryState.ACCEPTED_BY_APNS),
                receipt.deliveryKeys.map { assertNotNull(worker.current(it)).state }.toSet()
            )
            assertAggregate(fixture, logicalId, "ACKNOWLEDGED", "DISPATCHED")
        }
    }

    @Test
    fun `terminal provider failure projects terminal failure without changing decision sync`() = runBlocking {
        BackendNotificationDurabilityTestFixture().use { fixture ->
            val registration = fixture.register("aggregate-terminal")
            val logicalId = "logical-aggregate-terminal"
            val receipt = ingest(fixture, logicalId, listOf(registration.registrationId))
            val worker = composition(
                fixture,
                BackendDeliveryTokenAvailabilityPort { true },
                BackendDeliveryProviderPort {
                    BackendProviderRawObservation.Http(400, "BadPath", null, "apns-aggregate-terminal", 100)
                }
            )
            BackendNotificationDeliveryRecoveryScheduler(worker, "aggregate-terminal-worker").use {
                it.startAndDrainDueWork()
            }
            assertEquals(
                BackendDurableDeliveryState.REJECTED_PAYLOAD,
                worker.current(receipt.deliveryKeys.single())?.state
            )
            assertAggregate(fixture, logicalId, "ACKNOWLEDGED", "TERMINAL_FAILURE")
        }
    }

    private suspend fun ingest(
        fixture: BackendNotificationDurabilityTestFixture,
        logicalId: String,
        registrations: List<String>
    ): BackendNotificationIngestionReceipt = BackendNotificationIngestionService(
        fixture.deliveryFactory,
        BackendNotificationIngestionFaultInjector { },
        BackendNotificationIngestionCommittedPort { }
    ).ingest(
        BackendNotificationIngestionCommand(
            domainEventId = "aggregate-$logicalId",
            effectType = "DATE_CONFIRMED",
            schemaVersion = 1,
            logicalNotificationId = logicalId,
            recipients = listOf(
                BackendNotificationRecipientIntent(
                    participantId = "participant-$logicalId",
                    channel = "push",
                    provider = "apns",
                    registrationIds = registrations,
                    expiresAtEpochSeconds = 1_000
                )
            )
        )
    )

    private fun composition(
        fixture: BackendNotificationDurabilityTestFixture,
        tokenAvailability: BackendDeliveryTokenAvailabilityPort,
        provider: BackendDeliveryProviderPort
    ) = BackendNotificationDeliveryWorkerComposition(
        deliveryStoreFactory = fixture.deliveryFactory,
        registrationStoreFactory = fixture.registrationFactory,
        authority = BackendDeliveryAuthority.OUTBOX_V2,
        clock = BackendDeliveryWorkerClock { 100 },
        policy = BackendDeliveryPolicyPort { BackendDeliveryPolicyDecision.ALLOW },
        tokenAvailability = tokenAvailability,
        credentials = object : BackendDeliveryCredentialPort {
            override suspend fun credentialVersion() = "credential-v1"
            override suspend fun refreshAfterProviderRejection(expectedVersion: String) = true
        },
        provider = provider,
        jitter = BackendDeliveryJitterSource { _, _ -> 0.5 },
        faultInjector = BackendDeliveryWorkerFaultInjector { }
    )

    private fun assertAggregate(
        fixture: BackendNotificationDurabilityTestFixture,
        logicalId: String,
        decision: String,
        dispatch: String
    ) {
        DriverManager.getConnection("jdbc:sqlite:${fixture.databasePath}").use { connection ->
            connection.prepareStatement(
                "SELECT decision_sync_status, effect_dispatch_status FROM notification_logical " +
                    "WHERE logical_notification_id = ?"
            ).use { statement ->
                statement.setString(1, logicalId)
                statement.executeQuery().use { rows ->
                    assertEquals(true, rows.next())
                    assertEquals(decision, rows.getString("decision_sync_status"))
                    assertEquals(dispatch, rows.getString("effect_dispatch_status"))
                    assertEquals(false, rows.next())
                }
            }
        }
    }
}
