package com.guyghost.wakeve.notification

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.lang.reflect.Modifier
import java.sql.DriverManager
import java.sql.SQLException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BackendNotificationBoundaryReviewRedTest {
    @Test
    fun `public provider boundary never accepts a caller classified outcome or reason`() {
        val publicBoundaryMethods = listOf(
            BackendDeliveryRuntime::class.java,
            BackendNotificationDeliveryWorkerComposition::class.java,
            BackendDeliveryProviderPort::class.java
        ).flatMap { it.methods.toList() }
        assertFalse(
            publicBoundaryMethods.any { BackendClassifiedProviderObservation::class.java in it.parameterTypes },
            "a public caller must never nominate a classified durable outcome"
        )
        assertFalse(publicBoundaryMethods.any { BackendDurableProviderOutcome::class.java in it.parameterTypes })
        assertFalse(publicBoundaryMethods.any { BackendPersistedProviderReason::class.java in it.parameterTypes })
        assertFalse(publicBoundaryMethods.any { it.name == "markProviderAuthBlocked" })
        assertFalse(
            BackendDeliveryRuntime::class.java.methods.any {
                BackendProviderRawObservation::class.java in it.parameterTypes
            },
            "the persistence runtime must not expose a public raw-observation mutator"
        )
    }

    @Test
    fun `worker binds raw provider observation to its exact typed request and forged references are inert`() = runBlocking {
        BackendNotificationDurabilityTestFixture().use { fixture ->
            val registration = fixture.register("raw-envelope")
            val key = ingestDelivery(fixture, registration, "raw-envelope")
            val provider = GatedRawProvider()
            val worker = deliveryComposition(fixture, provider)

            BackendNotificationDeliveryRecoveryScheduler(worker, "raw-envelope-worker").use { scheduler ->
                val drain = async { scheduler.startAndDrainDueWork() }
                try {
                    val exact = withTimeout(5_000) { provider.request.await() }
                    val sending = assertNotNull(worker.current(key))
                    val lease = assertNotNull(sending.lease)
                    assertEquals(BackendDurableDeliveryState.SENDING, sending.state)
                    assertEquals(key, exact.deliveryKey)
                    assertEquals(registration.registrationId, exact.registrationId)
                    assertEquals(sending.correlationId, exact.correlationId)
                    assertEquals(sending.attempt, exact.attempt)
                    assertEquals(lease.holderId, exact.leaseHolderId)
                    assertEquals(lease.version, exact.leaseVersion)
                    assertEquals(lease.fencingToken, exact.leaseFencingToken)

                    val forged = listOf(
                        exact.copy(deliveryKey = DeliveryKey("foreign-delivery")),
                        exact.copy(correlationId = "foreign-correlation"),
                        exact.copy(attempt = exact.attempt + 1),
                        exact.copy(leaseHolderId = "foreign-holder"),
                        exact.copy(leaseVersion = exact.leaseVersion + 1),
                        exact.copy(leaseFencingToken = exact.leaseFencingToken + 1)
                    )
                    forged.forEach { forgedReference ->
                        worker.handleProviderObservation(
                            BackendRawProviderObservationCommand(forgedReference, successObservation())
                        )
                        assertEquals(sending, worker.current(key), "forged provider reference mutated delivery")
                    }

                    worker.handleProviderObservation(
                        BackendRawProviderObservationCommand(exact, successObservation())
                    )
                    assertEquals(BackendDurableDeliveryState.ACCEPTED_BY_APNS, worker.current(key)?.state)
                    provider.release.complete(successObservation())
                    withTimeout(5_000) { drain.await() }
                } finally {
                    provider.release.complete(successObservation())
                    drain.cancel()
                }
            }
            assertEquals(BackendDurableDeliveryState.ACCEPTED_BY_APNS, worker.current(key)?.state)
        }
    }

    @Test
    fun `APNs classification is exact case sensitive and scoped to its HTTP status`() {
        val cases = listOf(
            StrictCase(410, "BadDeviceToken", BackendDurableProviderOutcome.UNKNOWN_OUTCOME),
            StrictCase(400, "ExpiredToken", BackendDurableProviderOutcome.UNKNOWN_OUTCOME),
            StrictCase(410, "DeviceTokenNotForTopic", BackendDurableProviderOutcome.UNKNOWN_OUTCOME),
            StrictCase(410, "BadPath", BackendDurableProviderOutcome.UNKNOWN_OUTCOME),
            StrictCase(400, "Unregistered", BackendDurableProviderOutcome.UNKNOWN_OUTCOME),
            StrictCase(400, "ExpiredProviderToken", BackendDurableProviderOutcome.UNKNOWN_OUTCOME),
            StrictCase(400, "TooManyProviderTokenUpdates", BackendDurableProviderOutcome.UNKNOWN_OUTCOME),
            StrictCase(418, "BadPath", BackendDurableProviderOutcome.UNKNOWN_OUTCOME),
            StrictCase(400, " BadPath", BackendDurableProviderOutcome.UNKNOWN_OUTCOME),
            StrictCase(400, "BadPath ", BackendDurableProviderOutcome.UNKNOWN_OUTCOME),
            StrictCase(400, "badpath", BackendDurableProviderOutcome.UNKNOWN_OUTCOME),
            StrictCase(400, "DuplicatedHeaders", BackendDurableProviderOutcome.UNKNOWN_OUTCOME),
            StrictCase(400, "MissingDeviceToken", BackendDurableProviderOutcome.UNKNOWN_OUTCOME),
            StrictCase(501, null, BackendDurableProviderOutcome.UNKNOWN_OUTCOME),
            StrictCase(501, "BadPath", BackendDurableProviderOutcome.UNKNOWN_OUTCOME),
            StrictCase(502, null, BackendDurableProviderOutcome.UNKNOWN_OUTCOME),
            StrictCase(500, null, BackendDurableProviderOutcome.RETRY),
            StrictCase(503, null, BackendDurableProviderOutcome.RETRY),
            StrictCase(404, null, BackendDurableProviderOutcome.REJECTED_PAYLOAD),
            StrictCase(405, "future-provider-text", BackendDurableProviderOutcome.REJECTED_PAYLOAD),
            StrictCase(413, null, BackendDurableProviderOutcome.REJECTED_PAYLOAD)
        )
        val mismatches = cases.mapNotNull { case ->
            val classified = BackendDeliveryObservationClassifier.classify(
                BackendProviderRawObservation.Http(case.status, case.reason, null, "apns-strict", 100),
                BackendDeliveryRetryContext(DeliveryKey("strict-${case.status}-${case.reason}"), 100, 1_000, 0, 5),
                BackendDeliveryJitterSource { _, _ -> 0.5 }
            )
            val expectedReason = when (case.outcome) {
                BackendDurableProviderOutcome.UNKNOWN_OUTCOME -> BackendPersistedProviderReason.UNKNOWN_PROVIDER_REASON
                BackendDurableProviderOutcome.RETRY -> BackendPersistedProviderReason.HTTP_5XX
                BackendDurableProviderOutcome.REJECTED_PAYLOAD -> BackendPersistedProviderReason.PAYLOAD_REJECTED
                else -> error("unsupported strict-matrix expectation ${case.outcome}")
            }
            if (classified.outcome == case.outcome && classified.reason == expectedReason) null
            else "${case.status}/${case.reason}: expected ${case.outcome}/$expectedReason, " +
                "was ${classified.outcome}/${classified.reason}"
        }
        assertTrue(mismatches.isEmpty(), mismatches.joinToString(separator = "\n"))
    }

    @Test
    fun `last retryable attempt stages retry exhausted without another schedule`() {
        listOf(500, 503).forEach { status ->
            val classified = BackendDeliveryObservationClassifier.classify(
                BackendProviderRawObservation.Http(status, null, null, "apns-budget-$status", 100),
                BackendDeliveryRetryContext(
                    DeliveryKey("retry-budget-$status"), 100, 1_000,
                    attempt = 4, maxAttempts = 5
                ),
                BackendDeliveryJitterSource { _, _ -> 1.0 }
            )
            assertEquals(BackendDurableProviderOutcome.RETRY_EXHAUSTED, classified.outcome, status.toString())
            assertEquals(BackendPersistedProviderReason.RETRY_BUDGET_EXHAUSTED, classified.reason)
            assertEquals(null, classified.nextAttemptAtEpochSeconds)
        }
    }

    @Test
    fun `canonical identity components reject leading and trailing whitespace`() {
        val invalidCalls = listOf<() -> Unit>(
            { BackendCanonicalNotificationIdentity.effectKey(" event", "DATE_CONFIRMED", 1) },
            { BackendCanonicalNotificationIdentity.effectKey("event ", "DATE_CONFIRMED", 1) },
            { BackendCanonicalNotificationIdentity.effectKey("event", " DATE_CONFIRMED", 1) },
            { BackendCanonicalNotificationIdentity.effectKey("event", "DATE_CONFIRMED ", 1) },
            { BackendCanonicalNotificationIdentity.recipientKey(EffectKey("effect"), " participant", "push") },
            { BackendCanonicalNotificationIdentity.recipientKey(EffectKey("effect"), "participant ", "push") },
            { BackendCanonicalNotificationIdentity.recipientKey(EffectKey("effect"), "participant", " push") },
            { BackendCanonicalNotificationIdentity.recipientKey(EffectKey("effect"), "participant", "push ") },
            { BackendCanonicalNotificationIdentity.deliveryKey(RecipientKey("recipient"), " registration", "apns") },
            { BackendCanonicalNotificationIdentity.deliveryKey(RecipientKey("recipient"), "registration ", "apns") },
            { BackendCanonicalNotificationIdentity.deliveryKey(RecipientKey("recipient"), "registration", " apns") },
            { BackendCanonicalNotificationIdentity.deliveryKey(RecipientKey("recipient"), "registration", "apns ") }
        )
        invalidCalls.forEachIndexed { index, call ->
            assertFailsWith<IllegalArgumentException>("invalid canonical component $index") { call() }
        }
    }

    @Test
    fun `ingestion rejects whitespace aliases of a real registration without trimming`() = runBlocking {
        listOf<(String) -> String>({ " $it" }, { "$it " }).forEachIndexed { index, alias ->
            BackendNotificationDurabilityTestFixture().use { fixture ->
                val registration = fixture.register("ingestion-alias-$index")
                fixture.deliveryFactory.openDeliveryRuntime().use { }
                val before = backendRowCounts(fixture)
                assertFailsWith<IllegalArgumentException> {
                    BackendNotificationIngestionService(
                        fixture.deliveryFactory,
                        BackendNotificationIngestionFaultInjector { },
                        BackendNotificationIngestionCommittedPort { }
                    ).ingest(
                        BackendNotificationIngestionCommand(
                            "identity-alias-$index", "DATE_CONFIRMED", 1, "logical-identity-alias-$index",
                            listOf(
                                BackendNotificationRecipientIntent(
                                    "participant-$index", "push", "apns",
                                    listOf(alias(registration.registrationId)), 1_000
                                )
                            )
                        )
                    )
                }
                assertEquals(before, backendRowCounts(fixture), "alias $index mutated backend notification rows")
            }
        }
    }

    @Test
    fun `invalid delivery authority insert is rejected and valid row survives reopen`() = runBlocking {
        BackendNotificationDurabilityTestFixture().use { fixture ->
            val registration = fixture.register("authority-insert")
            val key = ingestDelivery(fixture, registration, "authority-insert")
            DriverManager.getConnection("jdbc:sqlite:${fixture.databasePath}").use { connection ->
                connection.autoCommit = false
                try {
                    connection.prepareStatement(
                        "DELETE FROM notification_delivery_authority WHERE delivery_key = ?"
                    ).use { statement -> statement.setString(1, key.value); statement.executeUpdate() }
                    assertFailsWith<SQLException> {
                        connection.prepareStatement(
                            "INSERT INTO notification_delivery_authority(delivery_key, authority, fencing_token) " +
                                "VALUES (?, 'attacker', 1)"
                        ).use { statement -> statement.setString(1, key.value); statement.executeUpdate() }
                    }
                } finally {
                    connection.rollback()
                }
            }
            fixture.deliveryFactory.openDeliveryRuntime().use { runtime ->
                assertEquals(DeliveryAuthority("outbox-v2"), assertNotNull(runtime.current(key)).authority)
            }
        }
    }

    @Test
    fun `invalid delivery authority update is rejected and closed API remains exact`() = runBlocking {
        assertEquals(setOf("LEGACY", "OUTBOX_V2"), BackendDeliveryAuthority.entries.map { it.name }.toSet())
        assertEquals("legacy", DeliveryAuthority("legacy").value)
        assertEquals("outbox-v2", DeliveryAuthority("outbox-v2").value)
        assertFailsWith<IllegalArgumentException> { DeliveryAuthority("attacker") }
        BackendNotificationDurabilityTestFixture().use { fixture ->
            val registration = fixture.register("authority-update")
            val key = ingestDelivery(fixture, registration, "authority-update")
            DriverManager.getConnection("jdbc:sqlite:${fixture.databasePath}").use { connection ->
                assertFailsWith<SQLException> {
                    connection.prepareStatement(
                        "UPDATE notification_delivery_authority SET authority = 'attacker' WHERE delivery_key = ?"
                    ).use { statement -> statement.setString(1, key.value); statement.executeUpdate() }
                }
            }
            fixture.deliveryFactory.openDeliveryRuntime().use { runtime ->
                assertEquals(DeliveryAuthority("outbox-v2"), assertNotNull(runtime.current(key)).authority)
            }
        }
    }

    @Test
    fun `legacy delivery migration adapter and mutators are not Java invocable`() {
        val publicStoreMethods = BackendNotificationDeliveryStore::class.java.methods.map { it.name }.toSet()
        assertFalse("enqueue" in publicStoreMethods)
        assertFalse("acquireLease" in publicStoreMethods)
        assertFalse("recordRetry" in publicStoreMethods)

        runCatching {
            Class.forName("com.guyghost.wakeve.notification.BackendLegacyNotificationDeliveryMigrationAdapter")
        }.getOrNull()?.let { adapter ->
            assertFalse(Modifier.isPublic(adapter.modifiers), "the legacy migration adapter is public to Java")
            assertTrue(
                adapter.methods.none { method ->
                    Modifier.isPublic(method.modifiers) &&
                        method.name in setOf("enqueue", "acquireLease", "recordRetry")
                },
                "legacy mutation methods remain Java-invocable"
            )
        }

        val kotlinFacade = Class.forName("com.guyghost.wakeve.notification.APNsProductionContractsKt")
        val leakedExtensions = kotlinFacade.declaredMethods.filter { method ->
            Modifier.isPublic(method.modifiers) &&
                method.parameterTypes.firstOrNull() == BackendNotificationDeliveryStore::class.java &&
                method.name.substringBefore('-') in setOf("enqueue", "acquireLease", "recordRetry")
        }
        assertTrue(leakedExtensions.isEmpty(), "legacy Kotlin extensions are callable from Java: $leakedExtensions")
    }

    private suspend fun ingestDelivery(
        fixture: BackendNotificationDurabilityTestFixture,
        registration: BackendDeviceRegistration,
        identity: String
    ): DeliveryKey = BackendNotificationIngestionService(
        fixture.deliveryFactory,
        BackendNotificationIngestionFaultInjector { },
        BackendNotificationIngestionCommittedPort { }
    ).ingest(
        BackendNotificationIngestionCommand(
            "boundary-$identity", "DATE_CONFIRMED", 1, "logical-boundary-$identity",
            listOf(
                BackendNotificationRecipientIntent(
                    "participant-$identity", "push", "apns", listOf(registration.registrationId), 1_000
                )
            )
        )
    ).deliveryKeys.single()

    private fun deliveryComposition(
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

    private fun successObservation(): BackendProviderRawObservation.Http =
        BackendProviderRawObservation.Http(200, null, null, "apns-raw-envelope", 100)

    private fun backendRowCounts(fixture: BackendNotificationDurabilityTestFixture): List<Int> =
        DriverManager.getConnection("jdbc:sqlite:${fixture.databasePath}").use { connection ->
            listOf(
                "domain_event_ingestion", "notification_logical", "notification_recipient", "notification_delivery"
            ).map { table ->
                connection.createStatement().use { statement ->
                    statement.executeQuery("SELECT COUNT(*) FROM $table").use { rows -> rows.next(); rows.getInt(1) }
                }
            }
        }

    private data class StrictCase(
        val status: Int,
        val reason: String?,
        val outcome: BackendDurableProviderOutcome
    )

    private class GatedRawProvider : BackendDeliveryProviderPort {
        val request = CompletableDeferred<BackendDeliveryProviderRequest>()
        val release = CompletableDeferred<BackendProviderRawObservation>()

        override suspend fun send(request: BackendDeliveryProviderRequest): BackendProviderRawObservation {
            check(this.request.complete(request)) { "provider was invoked more than once" }
            return release.await()
        }
    }
}
