package com.guyghost.wakeve.notification

import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path
import java.util.ServiceLoader
import java.util.concurrent.TimeUnit
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** RED persistence contracts require a production implementation registered through the production factory port. */
class BackendNotificationPersistenceRedTest {
    @Test
    fun productionStoreFailsClosedWhenNoDurableStorageIsConfigured() {
        val previousProperties = savedConfigurationProperties()
        try {
            CONFIGURATION_PROPERTIES.forEach(System::clearProperty)
            assertTrue(
                System.getenv(DELIVERY_STORE_ENVIRONMENT).isNullOrBlank(),
                "This contract requires the test process to omit delivery-store configuration"
            )
            assertTrue(
                System.getenv(DEVICE_REGISTRATION_DATABASE_PATH_ENVIRONMENT).isNullOrBlank(),
                "This contract requires the test process to omit device-registration path configuration"
            )
            assertTrue(
                System.getenv(LEGACY_HMAC_KEY_ENVIRONMENT).isNullOrBlank(),
                "This contract requires the test process to omit legacy HMAC configuration"
            )
            assertTrue(
                System.getenv(TOKEN_ENCRYPTION_KEY_ENVIRONMENT).isNullOrBlank(),
                "This contract requires the test process to omit token-encryption configuration"
            )

            assertFailsWith<IllegalStateException>(
                "The production factory must reject the old process-local temporary database fallback"
            ) {
                SqliteBackendNotificationDeliveryStoreFactory().open()
            }
        } finally {
            restoreConfigurationProperties(previousProperties)
        }
    }

    @Test
    fun missingRegistrationPersistsPendingTargetThenReceiptFencedFanoutAfterRegistration() = runBlocking {
        BackendNotificationDurabilityTestFixture().use { fixture ->
            val command = BackendNotificationIngestionCommand(
                domainEventId = "persistence-pending-target",
                effectType = "DATE_CONFIRMED",
                schemaVersion = 1,
                logicalNotificationId = "logical-persistence-pending-target",
                recipients = listOf(
                    BackendNotificationRecipientIntent(
                        participantId = "participant-pending-target",
                        channel = "push",
                        provider = "apns",
                        registrationIds = emptyList(),
                        expiresAtEpochSeconds = 5_000
                    )
                )
            )
            val receipt = ingestionService(fixture).ingest(command)
            val recipientKey = BackendCanonicalNotificationIdentity.recipientKey(
                receipt.effectKey,
                command.recipients.single().participantId,
                command.recipients.single().channel
            )
            assertEquals(BackendEffectDispatchStatus.PENDING_RECIPIENT, receipt.effectDispatchStatus)
            assertEquals(emptySet(), receipt.deliveryKeys)

            val registration = fixture.register("pending-target-registration")
            val deliveryKey = fixture.deliveryFactory.openRecipientTargetRuntime().use { runtime ->
                val pending = assertNotNull(runtime.current(recipientKey))
                assertEquals(BackendRecipientTargetState.PENDING_TARGET, pending.state)
                val lease = assertNotNull(
                    runtime.acquireLease(
                        BackendRecipientTargetLeaseRequest(
                            recipientKey = recipientKey,
                            expectedCheckpointRevision = pending.checkpointRevision,
                            holderId = "pending-target-worker",
                            expectedLeaseVersion = pending.lastLeaseVersion,
                            newLeaseVersion = pending.lastLeaseVersion + 1,
                            fencingToken = pending.lastFencingToken + 1,
                            expiresAtLogicalEpochSeconds = pending.nowEpochSeconds + 30
                        )
                    )
                )
                val staged = runtime.stageResolution(
                    BackendRecipientTargetResolutionRequest(
                        recipientKey = recipientKey,
                        registrationIds = listOf(registration.registrationId),
                        jitterSample = 0.5,
                        holderId = lease.holderId,
                        leaseVersion = lease.version,
                        fencingToken = lease.fencingToken
                    )
                )
                val checkpoint = assertNotNull(staged.pendingCheckpoint)
                assertEquals(BackendRecipientTargetCheckpointKind.FANOUT, checkpoint.kind)
                assertEquals(listOf(registration.registrationId), checkpoint.deliveries.map { it.registrationId })
                assertTrue(checkpoint.transactionReceiptId.isNotBlank())
                assertEquals(emptySet(), staged.deliveryKeys, "FANOUT staging cannot insert deliveries before exact ACK")
                val reference = staged.requireTargetReference()
                assertTrue(runtime.requestCheckpoint(reference))
                val committed = runtime.acknowledgeCheckpoint(reference)
                assertEquals(BackendRecipientTargetState.TARGETED, committed.state)
                assertEquals(committed, runtime.acknowledgeCheckpoint(reference), "double ACK is inert")
                committed.deliveryKeys.single()
            }

            fixture.deliveryFactory.openRecipientTargetRuntime().use { reopened ->
                val targeted = assertNotNull(reopened.current(recipientKey))
                assertEquals(BackendRecipientTargetState.TARGETED, targeted.state)
                assertNull(targeted.pendingCheckpoint)
                assertEquals(setOf(deliveryKey), targeted.deliveryKeys)
            }
            fixture.deliveryFactory.open().use { store ->
                val delivery = assertNotNull(store.delivery(deliveryKey))
                assertEquals(registration.registrationId, delivery.registrationId)
                assertEquals(command.logicalNotificationId, delivery.logicalNotificationId)
                assertEquals(setOf(registration.registrationId), store.recipient(recipientKey)?.registrationIds)
            }
            fixture.registrationFactory.open().use { registrations ->
                assertEquals(registration, registrations.registration(registration.registrationId))
            }
            java.sql.DriverManager.getConnection("jdbc:sqlite:${fixture.databasePath}").use { connection ->
                connection.createStatement().use { statement ->
                    statement.executeQuery("PRAGMA foreign_key_check").use { rows ->
                        assertFalse(rows.next(), "targeted delivery must retain a valid registration foreign key")
                    }
                }
            }
        }
    }

    @Test
    fun duplicateEnqueueCreatesOneDeliveryWithExactDeliveryKey() = runBlocking {
        BackendNotificationDurabilityTestFixture().use { fixture ->
            val registration = fixture.register("canonical-duplicate")
            val command = canonicalCommand(registration, "canonical-duplicate")
            val service = ingestionService(fixture)
            val first = service.ingest(command)
            val duplicate = service.ingest(command)
            val expected = BackendCanonicalNotificationIdentity.deliveryKey(
                BackendCanonicalNotificationIdentity.recipientKey(
                    first.effectKey,
                    command.recipients.single().participantId,
                    command.recipients.single().channel
                ),
                registration.registrationId,
                "apns"
            )

            assertTrue(first.created)
            assertEquals(setOf(expected), first.deliveryKeys)
            assertEquals(first.transactionId, duplicate.transactionId)
            assertEquals(first.deliveryKeys, duplicate.deliveryKeys)
            assertFalse(duplicate.created, "duplicate ingestion must reuse rather than recreate logical delivery")
            fixture.deliveryFactory.open().use { store ->
                assertEquals(expected, store.delivery(expected)?.deliveryKey)
                assertEquals(1, store.deliveryCount(expected))
            }
        }
    }

    @Test
    fun exactDeliveryLeaseLostRequeuesAndRestartAcquiresStrictlyNewerLease() = runBlocking {
        BackendNotificationDurabilityTestFixture().use { fixture ->
            val registration = fixture.register("canonical-lost-lease")
            val key = ingestionService(fixture).ingest(
                canonicalCommand(registration, "canonical-lost-lease")
            ).deliveryKeys.single()
            val firstLease = fixture.deliveryFactory.openDeliveryRuntime().use { runtime ->
                val policy = assertNotNull(runtime.current(key))
                runtime.markPolicyAllowed(
                    key,
                    policy.checkpointRevision,
                    policy.authority,
                    policy.authorityFencingToken
                )
                val lease = assertNotNull(runtime.acquireLease(key, "worker-a"))
                val auth = assertNotNull(runtime.current(key))
                assertEquals(BackendDurableDeliveryState.AUTH, auth.state)
                val exactLoss = BackendDeliveryLeaseLostCommand(
                    deliveryKey = key,
                    correlationId = assertNotNull(auth.correlationId),
                    attempt = auth.attempt,
                    leaseHolderId = lease.holderId,
                    leaseVersion = lease.version,
                    leaseFencingToken = lease.fencingToken
                )
                assertEquals(auth, runtime.markLeaseLost(exactLoss.copy(leaseFencingToken = lease.fencingToken + 1)))
                val queued = runtime.markLeaseLost(exactLoss)
                assertEquals(BackendDurableDeliveryState.QUEUED, queued.state)
                assertNull(queued.lease)
                assertNull(queued.correlationId)
                lease
            }

            fixture.deliveryFactory.openDeliveryRuntime().use { restarted ->
                val recovered = assertNotNull(restarted.acquireLease(key, "worker-b"))
                assertEquals("worker-b", recovered.holderId)
                assertTrue(recovered.version > firstLease.version)
                assertTrue(recovered.fencingToken > firstLease.fencingToken)
                assertTrue(recovered.expiresAtLogicalEpochSeconds > assertNotNull(restarted.current(key)).nowEpochSeconds)
                assertEquals(key, restarted.current(key)?.deliveryKey)
            }
        }
    }

    @Test
    fun retryAttemptAndScheduleSurviveRestartUntilBusinessExpiry() = runBlocking {
        BackendNotificationDurabilityTestFixture().use { fixture ->
            val registration = fixture.register("canonical-retry-restart")
            val key = ingestionService(fixture).ingest(
                canonicalCommand(registration, "canonical-retry-restart", expiresAt = 1_000)
            ).deliveryKeys.single()
            val retrying = deliveryComposition(
                fixture,
                BackendDeliveryProviderPort {
                    BackendProviderRawObservation.Http(503, null, 400.0, "retry-persisted", 100)
                }
            )
            BackendNotificationDeliveryRecoveryScheduler(retrying, "retry-persistence-worker").use {
                it.startAndDrainDueWork()
            }

            fixture.deliveryFactory.openDeliveryRuntime().use { restarted ->
                val retry = assertNotNull(restarted.current(key))
                assertEquals(BackendDurableDeliveryState.RETRY_SCHEDULED, retry.state)
                assertEquals(1L, retry.attempt)
                assertEquals(400L, retry.nextAttemptAtEpochSeconds)
                assertFalse(key in restarted.dueDeliveryKeys(BackendDeliveryAuthority.OUTBOX_V2))
                val due = restarted.advanceLogicalClock(key, retry.clockRevision, 400)
                assertTrue(key in restarted.dueDeliveryKeys(BackendDeliveryAuthority.OUTBOX_V2))
                restarted.advanceLogicalClock(key, due.clockRevision, 1_000)
            }
            val expiring = deliveryComposition(
                fixture,
                BackendDeliveryProviderPort { error("business expiry cannot perform provider I/O") }
            )
            BackendNotificationDeliveryRecoveryScheduler(expiring, "retry-expiry-worker").use {
                it.startAndDrainDueWork()
            }
            assertEquals(BackendDurableDeliveryState.EXPIRED, expiring.current(key)?.state)
        }
    }

    @Test
    fun unknownTerminalDeliveryCannotBeLeasedOrRetried() = runBlocking {
        BackendNotificationDurabilityTestFixture().use { fixture ->
            val registration = fixture.register("canonical-terminal")
            val command = canonicalCommand(registration, "canonical-terminal")
            val key = ingestionService(fixture).ingest(command).deliveryKeys.single()
            val accepted = deliveryComposition(
                fixture,
                BackendDeliveryProviderPort {
                    BackendProviderRawObservation.Http(200, null, null, "terminal-accepted", 200)
                }
            )
            BackendNotificationDeliveryRecoveryScheduler(accepted, "terminal-worker").use {
                it.startAndDrainDueWork()
            }
            fixture.deliveryFactory.openDeliveryRuntime().use { runtime ->
                val terminal = assertNotNull(runtime.current(key))
                assertEquals(BackendDurableDeliveryState.ACCEPTED_BY_APNS, terminal.state)
                assertNull(runtime.acquireLease(key, "worker-a"), "terminal delivery cannot return to a lease")
                val replay = ingestionService(fixture).ingest(command)
                assertFalse(replay.created, "terminal delivery cannot be recreated for retry")
                assertEquals(terminal, runtime.current(key))
            }
        }
    }

    @Test
    fun unknownOutcomeCannotAcquireLeaseButAnExpiredLeaseCanBeRecovered() = runBlocking {
        BackendNotificationDurabilityTestFixture().use { fixture ->
            val registration = fixture.register("canonical-unknown")
            val key = ingestionService(fixture).ingest(
                canonicalCommand(registration, "canonical-unknown", expiresAt = 1_000)
            ).deliveryKeys.single()
            val uncertain = deliveryComposition(
                fixture,
                BackendDeliveryProviderPort {
                    BackendProviderRawObservation.Transport(BackendProviderTransportPhase.MAY_HAVE_WRITTEN)
                }
            )
            BackendNotificationDeliveryRecoveryScheduler(uncertain, "unknown-worker").use {
                it.startAndDrainDueWork()
            }
            fixture.deliveryFactory.openDeliveryRuntime().use { runtime ->
                val unknown = assertNotNull(runtime.current(key))
                assertEquals(BackendDurableDeliveryState.UNKNOWN_OUTCOME, unknown.state)
                assertNull(runtime.acquireLease(key, "worker-a"))
                val beforeRestart = assertNotNull(runtime.current(key))
                assertEquals(unknown, beforeRestart)
                runtime.advanceLogicalClock(key, beforeRestart.clockRevision, 1_000)
            }
            val restarted = deliveryComposition(
                fixture,
                BackendDeliveryProviderPort { error("unknown outcome cannot be resent automatically") }
            )
            BackendNotificationDeliveryRecoveryScheduler(restarted, "unknown-restart-worker").use {
                it.startAndDrainDueWork()
            }
            assertEquals(BackendDurableDeliveryState.EXPIRED, restarted.current(key)?.state)
            // The former legacy expired-lease half maps 1:1 to expiredLeaseIsRecoverableAfterWorkerRestartWithSameIdentity.
        }
    }

    @Test
    fun interProcessEqualIdentityConflictReusesProgressWithoutResettingIt() = runBlocking {
        BackendNotificationDurabilityTestFixture().use { fixture ->
            val registration = fixture.register("canonical-concurrent-equal")
            val command = canonicalCommand(registration, "canonical-concurrent-equal")
            val results = runConcurrentIngestionProcesses(
                fixture = fixture,
                commands = listOf(command, command),
                gateName = "equal-ingestion"
            )
            assertEquals(1, results.count { it.created }, results.toString())
            assertEquals(1, results.count { !it.created }, results.toString())
            assertEquals(1, results.map { it.transactionId }.distinct().size)
            assertEquals(1, results.flatMap { it.deliveryKeys }.distinct().size)
            val key = results.flatMap { it.deliveryKeys }.distinct().single()
            fixture.deliveryFactory.open().use { store ->
                assertEquals(1, store.deliveryCount(key), "two JVM replay calls must coalesce to one delivery row")
            }

            val retrying = deliveryComposition(
                fixture,
                BackendDeliveryProviderPort {
                    BackendProviderRawObservation.Http(503, null, 400.0, "equal-retry", 100)
                }
            )
            BackendNotificationDeliveryRecoveryScheduler(retrying, "equal-worker").use {
                it.startAndDrainDueWork()
            }
            val replay = ingestionService(fixture).ingest(command)
            assertFalse(replay.created)
            fixture.deliveryFactory.openDeliveryRuntime().use { runtime ->
                val persisted = assertNotNull(runtime.current(key))
                assertEquals(BackendDurableDeliveryState.RETRY_SCHEDULED, persisted.state)
                assertEquals(1L, persisted.attempt)
                assertEquals(400L, persisted.nextAttemptAtEpochSeconds)
            }
            fixture.deliveryFactory.open().use { store -> assertEquals(1, store.deliveryCount(key)) }
        }
    }

    @Test
    fun interProcessContradictoryIdentityConflictFailsClosedWithoutResettingTheWinner() = runBlocking {
        BackendNotificationDurabilityTestFixture().use { fixture ->
            val registration = fixture.register("canonical-conflict")
            val command = canonicalCommand(registration, "canonical-conflict")
            val conflicting = command.copy(
                recipients = command.recipients.map { it.copy(participantId = "${it.participantId}-conflict") }
            )
            val results = runConcurrentIngestionProcesses(
                fixture = fixture,
                commands = listOf(command, conflicting),
                gateName = "contradictory-ingestion"
            )
            val winner = results.singleOrNull { it.created }
            assertNotNull(winner, results.toString())
            assertEquals(1, results.count { it.conflict }, results.toString())
            assertEquals(1, results.count { it.created }, results.toString())
            val key = winner.deliveryKeys.single()
            fixture.deliveryFactory.open().use { store ->
                assertEquals(1, store.deliveryCount(key), "contradictory JVM ingestion cannot partially create a second row")
            }
            val retrying = deliveryComposition(
                fixture,
                BackendDeliveryProviderPort {
                    BackendProviderRawObservation.Http(503, null, 400.0, "winner-retry", 100)
                }
            )
            BackendNotificationDeliveryRecoveryScheduler(retrying, "winner-worker").use {
                it.startAndDrainDueWork()
            }
            fixture.deliveryFactory.openDeliveryRuntime().use { runtime ->
                val persisted = assertNotNull(runtime.current(key))
                assertEquals(BackendDurableDeliveryState.RETRY_SCHEDULED, persisted.state)
                assertEquals(1L, persisted.attempt)
                assertEquals(400L, persisted.nextAttemptAtEpochSeconds)
            }
            fixture.deliveryFactory.open().use { store ->
                val persisted = assertNotNull(store.delivery(key))
                assertEquals("apns", persisted.provider)
                assertEquals(1, store.deliveryCount(key))
            }
        }
    }

    @Test
    fun acquireLeaseAtomicallyRefusesRetryBeforeItsNextAttempt() = runBlocking {
        BackendNotificationDurabilityTestFixture().use { fixture ->
            val registration = fixture.register("canonical-retry-deadline")
            val key = ingestionService(fixture).ingest(
                canonicalCommand(registration, "canonical-retry-deadline")
            ).deliveryKeys.single()
            val retrying = deliveryComposition(
                fixture,
                BackendDeliveryProviderPort {
                    BackendProviderRawObservation.Http(503, null, 400.0, "deadline-retry", 100)
                }
            )
            BackendNotificationDeliveryRecoveryScheduler(retrying, "deadline-worker").use {
                it.startAndDrainDueWork()
            }
            fixture.deliveryFactory.openDeliveryRuntime().use { runtime ->
                val retry = assertNotNull(runtime.current(key))
                assertNull(runtime.acquireLease(key, "worker-a"), "retry cannot be claimed before its deadline")
                val beforeDue = runtime.advanceLogicalClock(key, retry.clockRevision, 399)
                assertNull(runtime.acquireLease(key, "worker-a"), "lease CAS must enforce durable nextAttempt")
                runtime.advanceLogicalClock(key, beforeDue.clockRevision, 400)
                assertNotNull(runtime.acquireLease(key, "worker-a"))
            }
        }
        Unit
    }

    @Test
    fun duplicateEnqueueAfterLeaseAndRetryReusesImmutableIdentityWithoutResettingProgress() = runBlocking {
        BackendNotificationDurabilityTestFixture().use { fixture ->
            val registration = fixture.register("canonical-replay-progress")
            val command = canonicalCommand(registration, "canonical-replay-progress")
            val first = ingestionService(fixture).ingest(command)
            val key = first.deliveryKeys.single()
            val retrying = deliveryComposition(
                fixture,
                BackendDeliveryProviderPort {
                    BackendProviderRawObservation.Http(503, null, 400.0, "replay-retry", 100)
                }
            )
            BackendNotificationDeliveryRecoveryScheduler(retrying, "replay-worker").use {
                it.startAndDrainDueWork()
            }

            val duplicate = ingestionService(fixture).ingest(command)
            assertFalse(duplicate.created, "a duplicate immutable identity must reuse its existing delivery")
            assertEquals(first.transactionId, duplicate.transactionId)
            fixture.deliveryFactory.open().use { store ->
                val persisted = assertNotNull(store.delivery(key))
                assertEquals(BackendDeliveryStatus.RETRY_SCHEDULED, persisted.status)
                assertEquals(1, persisted.attempt)
                assertEquals(400L, persisted.nextAttemptAtEpochSeconds)
                assertEquals(registration.registrationId, persisted.registrationId)
                assertEquals(command.logicalNotificationId, persisted.logicalNotificationId)
                assertEquals(key.value, persisted.idempotencyKey)
            }

            assertFailsWith<IllegalStateException>("a delivery key cannot be rebound to another immutable identity") {
                ingestionService(fixture).ingest(
                    command.copy(
                        recipients = command.recipients.map {
                            it.copy(participantId = "${it.participantId}-conflict")
                        }
                    )
                )
            }
        }
        Unit
    }

    @Test
    fun providerOutcomeFieldsRoundTripAcrossRestart() = runBlocking {
        BackendNotificationDurabilityTestFixture().use { fixture ->
            val registration = fixture.register("canonical-provider-metadata")
            val key = ingestionService(fixture).ingest(
                canonicalCommand(registration, "canonical-provider-metadata")
            ).deliveryKeys.single()
            val accepting = deliveryComposition(
                fixture,
                BackendDeliveryProviderPort {
                    BackendProviderRawObservation.Http(200, null, null, "apns-request-1", 700)
                }
            )
            BackendNotificationDeliveryRecoveryScheduler(accepting, "metadata-worker").use {
                it.startAndDrainDueWork()
            }

            fixture.deliveryFactory.open().use { restarted ->
                val afterRestart = assertNotNull(restarted.delivery(key))
                assertEquals(BackendDeliveryStatus.ACCEPTED_BY_APNS, afterRestart.status)
                assertEquals(700, afterRestart.acceptedAtEpochSeconds)
                assertEquals(200, afterRestart.providerStatus)
                assertEquals(BackendPersistedProviderReason.HTTP_200, afterRestart.providerReason)
                assertEquals("apns-request-1", afterRestart.providerRequestId)
            }
        }
    }

    @Test
    fun configuredStorePersistsRecipientUpsertAuthorityAndZeroTargetResolutionAcrossInstances() = runBlocking {
        withRegistrationBackedDeliveryStore { firstStore, _ ->
            val recipient = BackendNotificationRecipient(
                recipientKey = RecipientKey("zero-target-recipient"),
                effectKey = EffectKey("confirmation-effect"),
                status = BackendRecipientStatus.PENDING_TARGET,
                registrationIds = emptySet(),
                expiresAtEpochSeconds = 1_000
            )

            assertTrue(firstStore.persistPendingRecipient(recipient))
            assertFalse(firstStore.persistPendingRecipient(recipient), "recipient upsert must not create a second logical target")
            assertTrue(
                firstStore.acquireDeliveryAuthority(
                    "confirmation-delivery",
                    DeliveryAuthority(BackendDeliveryAuthority.OUTBOX_V2.wireValue)
                )
            )

            productionStore().use { restartedStore ->
                assertFalse(
                    restartedStore.acquireDeliveryAuthority(
                        "confirmation-delivery",
                        DeliveryAuthority(BackendDeliveryAuthority.LEGACY.wireValue)
                    ),
                    "A second worker must never become a second durable delivery authority"
                )
                assertEquals(
                    BackendRecipientStatus.PENDING_TARGET,
                    restartedStore.resolvePendingRecipient(recipient.recipientKey, nowEpochSeconds = 999)
                )
                assertEquals(
                    BackendRecipientStatus.EXPIRED,
                    restartedStore.resolvePendingRecipient(recipient.recipientKey, nowEpochSeconds = 1_000)
                )
                assertTrue(
                    restartedStore.recordRecipientTerminalAcknowledgement(
                        BackendRecipientTerminalAcknowledgement(
                            recipient.recipientKey,
                            BackendRecipientTerminalReason.EXPIRED_WITHOUT_TARGET,
                            acknowledgedAtEpochSeconds = 1_000
                        )
                    )
                )
            }

            productionStore().use { afterRestart ->
                assertEquals(BackendRecipientStatus.EXPIRED, afterRestart.recipient(recipient.recipientKey)?.status)
                assertNull(
                    afterRestart.delivery(DeliveryKey("zero-target-recipient:unresolved-registration:apns")),
                    "Zero-target resolution must remain a recipient acknowledgement and must not create a provider delivery"
                )
            }
        }
    }

    private fun productionStore(): BackendNotificationDeliveryStore {
        val factory = ServiceLoader.load(BackendNotificationDeliveryStoreFactory::class.java).firstOrNull()
        return assertNotNull(factory, "backend production delivery store is not implemented").open()
    }

    private fun ingestionService(fixture: BackendNotificationDurabilityTestFixture) =
        BackendNotificationIngestionService(
            fixture.deliveryFactory,
            BackendNotificationIngestionFaultInjector { },
            BackendNotificationIngestionCommittedPort { }
        )

    private fun canonicalCommand(
        registration: BackendDeviceRegistration,
        identity: String,
        expiresAt: Long = 10_000
    ) = BackendNotificationIngestionCommand(
        domainEventId = "persistence-$identity",
        effectType = "DATE_CONFIRMED",
        schemaVersion = 1,
        logicalNotificationId = "logical-persistence-$identity",
        recipients = listOf(
            BackendNotificationRecipientIntent(
                participantId = "participant-$identity",
                channel = "push",
                provider = "apns",
                registrationIds = listOf(registration.registrationId),
                expiresAtEpochSeconds = expiresAt
            )
        )
    )

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

    private fun BackendRecipientTargetSnapshot.requireTargetReference(): BackendRecipientTargetCheckpointReference {
        val checkpoint = assertNotNull(pendingCheckpoint)
        return BackendRecipientTargetCheckpointReference(
            recipientKey = recipientKey,
            effectId = checkpoint.effectId,
            checkpointRevision = checkpoint.revision,
            transactionReceiptId = checkpoint.transactionReceiptId,
            holderId = checkpoint.holderId,
            leaseVersion = checkpoint.leaseVersion,
            fencingToken = checkpoint.fencingToken
        )
    }

    private fun runConcurrentIngestionProcesses(
        fixture: BackendNotificationDurabilityTestFixture,
        commands: List<BackendNotificationIngestionCommand>,
        gateName: String
    ): List<ProcessIngestionResult> {
        val gate = fixture.root.resolve(gateName)
        Files.createDirectories(gate)
        val go = gate.resolve("go")
        val resultPaths = commands.indices.map { gate.resolve("result-$it") }
        val processes = commands.mapIndexed { index, command ->
            startIngestionProcess(
                databasePath = fixture.databasePath,
                command = command,
                ready = gate.resolve("ready-$index"),
                go = go,
                result = resultPaths[index]
            )
        }
        return try {
            awaitFiles(*commands.indices.map { gate.resolve("ready-$it") }.toTypedArray())
            go.writeText("go")
            processes.forEach { awaitSuccessfulChild(it, "notification ingestion") }
            resultPaths.map { parseProcessIngestionResult(it.readText().trim()) }
        } finally {
            processes.forEach(::terminateChild)
        }
    }

    private fun startIngestionProcess(
        databasePath: Path,
        command: BackendNotificationIngestionCommand,
        ready: Path,
        go: Path,
        result: Path
    ): Process {
        val recipient = command.recipients.single()
        return ProcessBuilder(
            Path.of(System.getProperty("java.home"), "bin", "java").toString(),
            "-cp",
            System.getProperty("java.class.path"),
            BackendNotificationPersistenceIngestionProcessMain::class.java.name,
            databasePath.toString(),
            command.domainEventId,
            command.effectType,
            command.schemaVersion.toString(),
            command.logicalNotificationId,
            recipient.participantId,
            recipient.channel,
            recipient.provider,
            recipient.registrationIds.single(),
            recipient.expiresAtEpochSeconds.toString(),
            ready.toString(),
            go.toString(),
            result.toString()
        ).redirectErrorStream(true).start()
    }

    private fun awaitFiles(vararg paths: Path) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (paths.any { !it.exists() } && System.nanoTime() < deadline) Thread.sleep(10)
        assertTrue(paths.all { it.exists() }, "ingestion JVMs did not reach their bounded start gate")
    }

    private fun awaitSuccessfulChild(process: Process, label: String) {
        val finished = process.waitFor(15, TimeUnit.SECONDS)
        if (!finished) process.destroyForcibly()
        assertTrue(finished, "$label child timed out")
        assertEquals(0, process.exitValue(), process.inputStream.bufferedReader().readText())
    }

    private fun terminateChild(process: Process) {
        if (process.isAlive) {
            process.destroyForcibly()
            process.waitFor(2, TimeUnit.SECONDS)
        }
    }

    private fun parseProcessIngestionResult(raw: String): ProcessIngestionResult {
        val fields = raw.split('|')
        return when (fields.firstOrNull()) {
            "RECEIPT" -> ProcessIngestionResult(
                created = fields[1] == "CREATED",
                conflict = false,
                transactionId = fields[2],
                deliveryKeys = fields.getOrElse(3) { "" }
                    .split(',')
                    .filter(String::isNotEmpty)
                    .map(::DeliveryKey),
                diagnostic = raw
            )
            "CONFLICT" -> ProcessIngestionResult(false, true, null, emptyList(), raw)
            else -> ProcessIngestionResult(false, false, null, emptyList(), raw)
        }
    }

    private data class ProcessIngestionResult(
        val created: Boolean,
        val conflict: Boolean,
        val transactionId: String?,
        val deliveryKeys: List<DeliveryKey>,
        val diagnostic: String
    )

    private suspend fun withRegistrationBackedDeliveryStore(
        block: suspend (BackendNotificationDeliveryStore, BackendDeviceRegistration) -> Unit
    ) {
        withRegistrationBackedDeliveryDatabase { _, registration, _ ->
            productionStore().use { store -> block(store, registration) }
        }
    }

    private suspend fun withRegistrationBackedDeliveryDatabase(
        block: suspend (Path, BackendDeviceRegistration, DeviceRegistrationStoreConfiguration) -> Unit
    ) {
        val directory = Files.createTempDirectory("wakeve-delivery-store-contract-")
        val databasePath = directory.resolve("delivery.sqlite")
        val previousProperties = savedConfigurationProperties()
        try {
            val configuration = registrationConfiguration(databasePath).getOrThrow()
            val registrationFactory = SqliteBackendDeviceRegistrationStoreFactory(
                configuration
            )
            val registration = registrationFactory.open().use { registrationStore ->
                registrationStore.register(
                    BackendDeviceRegistrationRequest.create(
                        installationId = "stable-installation-a",
                        authenticatedUserId = "user-a",
                        platform = Platform.IOS,
                        scope = DeviceRegistrationScope.create(
                            APNsEnvironment.PRODUCTION,
                            "com.guyghost.wakeve"
                        ).getOrThrow(),
                        rawToken = "registration-token-a",
                        registeredAtEpochSeconds = 100
                    ).getOrThrow()
                )
            }
            configureDeviceRegistrationProperties(databasePath)
            System.setProperty(DELIVERY_STORE_PROPERTY, databasePath.toString())
            block(databasePath, registration, configuration)
        } finally {
            restoreConfigurationProperties(previousProperties)
            Files.deleteIfExists(databasePath)
            Files.deleteIfExists(directory)
        }
    }

    private fun registrationConfiguration(path: Path): Result<DeviceRegistrationStoreConfiguration> =
        DeviceRegistrationStoreConfiguration.resolve(
            environment = emptyMap(),
            systemProperties = mapOf(
                DEVICE_REGISTRATION_DATABASE_PATH_PROPERTY to path.toString(),
                LEGACY_HMAC_KEY_PROPERTY to HMAC_KEY,
                TOKEN_ENCRYPTION_KEY_PROPERTY to TOKEN_ENCRYPTION_KEY
            )
        )

    private fun configureDeviceRegistrationProperties(databasePath: Path) {
        System.setProperty(DEVICE_REGISTRATION_DATABASE_PATH_PROPERTY, databasePath.toString())
        System.setProperty(LEGACY_HMAC_KEY_PROPERTY, HMAC_KEY)
        System.setProperty(TOKEN_ENCRYPTION_KEY_PROPERTY, TOKEN_ENCRYPTION_KEY)
    }

    private fun savedConfigurationProperties(): Map<String, String?> = CONFIGURATION_PROPERTIES.associateWith(System::getProperty)

    private fun restoreConfigurationProperties(previous: Map<String, String?>) {
        previous.forEach { (property, value) ->
            if (value == null) System.clearProperty(property) else System.setProperty(property, value)
        }
    }

    private companion object {
        const val DELIVERY_STORE_PROPERTY = "wakeve.notification.delivery.db.path"
        const val DELIVERY_STORE_ENVIRONMENT = "WAKEVE_NOTIFICATION_DELIVERY_DB_PATH"
        const val DEVICE_REGISTRATION_DATABASE_PATH_PROPERTY = "wakeve.notification.device-registration.db.path"
        const val DEVICE_REGISTRATION_DATABASE_PATH_ENVIRONMENT = "WAKEVE_NOTIFICATION_DEVICE_REGISTRATION_DB_PATH"
        const val LEGACY_HMAC_KEY_PROPERTY =
            "wakeve.notification.device-registration.legacy-identity-hmac-key"
        const val LEGACY_HMAC_KEY_ENVIRONMENT = "WAKEVE_NOTIFICATION_LEGACY_IDENTITY_HMAC_KEY"
        const val TOKEN_ENCRYPTION_KEY_PROPERTY =
            "wakeve.notification.device-registration.token-encryption-key"
        const val TOKEN_ENCRYPTION_KEY_ENVIRONMENT = "WAKEVE_NOTIFICATION_TOKEN_ENCRYPTION_KEY"
        const val HMAC_KEY = "delivery-persistence-hmac-key-with-at-least-32-bytes"
        const val TOKEN_ENCRYPTION_KEY = "delivery-persistence-encryption-key-with-at-least-32-bytes"

        val CONFIGURATION_PROPERTIES = setOf(
            DELIVERY_STORE_PROPERTY,
            DEVICE_REGISTRATION_DATABASE_PATH_PROPERTY,
            LEGACY_HMAC_KEY_PROPERTY,
            TOKEN_ENCRYPTION_KEY_PROPERTY
        )
    }
}

object BackendNotificationPersistenceIngestionProcessMain {
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        val databasePath = Path.of(args[0])
        val ready = Path.of(args[10])
        val go = Path.of(args[11])
        val result = Path.of(args[12])
        val command = BackendNotificationIngestionCommand(
            domainEventId = args[1],
            effectType = args[2],
            schemaVersion = args[3].toInt(),
            logicalNotificationId = args[4],
            recipients = listOf(
                BackendNotificationRecipientIntent(
                    participantId = args[5],
                    channel = args[6],
                    provider = args[7],
                    registrationIds = listOf(args[8]),
                    expiresAtEpochSeconds = args[9].toLong()
                )
            )
        )
        ready.writeText("ready")
        awaitGo(go)
        val configuration = DeviceRegistrationStoreConfiguration.resolve(
            systemProperties = mapOf(
                "wakeve.notification.device-registration.db.path" to databasePath.toString(),
                "wakeve.notification.device-registration.legacy-identity-hmac-key" to "h".repeat(32),
                "wakeve.notification.device-registration.token-encryption-key" to "e".repeat(32)
            ),
            environment = emptyMap()
        ).getOrThrow()
        val service = BackendNotificationIngestionService(
            SqliteBackendNotificationDeliveryStoreFactory(configuration),
            BackendNotificationIngestionFaultInjector { },
            BackendNotificationIngestionCommittedPort { }
        )
        val outcome = runCatching { service.ingest(command) }.fold(
            onSuccess = { receipt ->
                "RECEIPT|${if (receipt.created) "CREATED" else "REPLAY"}|${receipt.transactionId}|" +
                    receipt.deliveryKeys.joinToString(",") { it.value }
            },
            onFailure = { failure ->
                if (failure is IllegalStateException) {
                    "CONFLICT|${failure::class.java.simpleName}|${failure.message}"
                } else {
                    "ERROR|${failure::class.java.simpleName}|${failure.message}"
                }
            }
        )
        result.writeText(outcome)
    }

    private fun awaitGo(go: Path) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (!go.exists() && System.nanoTime() < deadline) Thread.sleep(10)
        check(go.exists()) { "ingestion start gate timed out" }
    }
}
