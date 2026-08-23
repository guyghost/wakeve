package com.guyghost.wakeve.notification

import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BackendNotificationCheckpointRecoveryReviewRedTest {
    @Test
    fun `expired provider checkpoint requires one multi JVM recovery lease and a fresh fenced reference`() = runBlocking {
        repeat(2) { round ->
            BackendNotificationDurabilityTestFixture().use { fixture ->
                val registration = fixture.register("provider-recovery-$round")
                val key = ingestDelivery(fixture, registration, "provider-recovery-$round")
                val providerCalls = AtomicInteger()
                val crashing = composition(
                    fixture,
                    BackendDeliveryProviderPort {
                        providerCalls.incrementAndGet()
                        BackendProviderRawObservation.Http(200, null, null, "apns-recovery-$round", 100)
                    },
                    BackendDeliveryWorkerFaultInjector { checkpoint ->
                        if (checkpoint == BackendDeliveryWorkerFaultCheckpoint.AFTER_PROVIDER_OBSERVATION_DURABLE) {
                            error("crash-after-provider-checkpoint")
                        }
                    }
                )
                assertFails {
                    BackendNotificationDeliveryRecoveryScheduler(crashing, "provider-old-$round").use {
                        it.startAndDrainDueWork()
                    }
                }
                assertEquals(1, providerCalls.get())

                val old = fixture.deliveryFactory.openDeliveryRuntime().use { runtime ->
                    val staged = assertNotNull(runtime.current(key))
                    assertEquals(BackendDurableDeliveryState.AWAITING_PROVIDER_RESULT_PERSISTENCE, staged.state)
                    assertFalse(assertNotNull(staged.pendingCheckpoint).effectRequested)
                    staged to staged.requireProviderReference()
                }
                val oldSnapshot = old.first
                val oldReference = old.second
                val oldLease = assertNotNull(oldSnapshot.lease)
                fixture.deliveryFactory.openDeliveryRuntime().use { runtime ->
                    runtime.advanceLogicalClock(key, oldSnapshot.clockRevision, oldLease.expiresAtLogicalEpochSeconds)
                }

                val gate = fixture.root.resolve("provider-recovery-gate-$round")
                Files.createDirectories(gate)
                val processes = (0..1).map { index ->
                    startProcess(
                        fixture,
                        listOf(
                            "DELIVERY_RECOVER", key.value, "provider-new-$index",
                            gate.resolve("ready-$index").toString(), gate.resolve("go").toString(),
                            gate.resolve("result-$index").toString()
                        )
                    )
                }
                try {
                    awaitFiles(gate.resolve("ready-0"), gate.resolve("ready-1"))
                    gate.resolve("go").writeText("go")
                    processes.forEach { awaitSuccessfulChild(it, "provider checkpoint recovery") }
                    val results = (0..1).map { gate.resolve("result-$it").readText().trim() }
                    assertEquals(1, results.count { it.startsWith("LEASE|") }, results.toString())
                    assertEquals(1, results.count { it == "NULL" }, results.toString())
                    assertTrue(results.none { it.startsWith("ERROR|") }, results.toString())
                } finally {
                    processes.forEach(::terminateChild)
                }

                fixture.deliveryFactory.openDeliveryRuntime().use { runtime ->
                    val recovered = assertNotNull(runtime.current(key))
                    val newCheckpoint = assertNotNull(recovered.pendingCheckpoint)
                    val newLease = assertNotNull(recovered.lease)
                    assertTrue(newLease.version > oldLease.version)
                    assertTrue(newLease.fencingToken > oldLease.fencingToken)
                    assertTrue(newCheckpoint.revision > assertNotNull(oldSnapshot.pendingCheckpoint).revision)
                    assertNotEquals(oldSnapshot.pendingCheckpoint.effectId, newCheckpoint.effectId)
                    assertEquals(newLease.holderId, newCheckpoint.leaseHolderId)
                    assertEquals(newLease.version, newCheckpoint.leaseVersion)
                    assertEquals(newLease.fencingToken, newCheckpoint.leaseFencingToken)
                    assertFalse(newCheckpoint.effectRequested)

                    assertFalse(runtime.requestProviderCheckpoint(oldReference))
                    assertEquals(recovered, runtime.acknowledgeProviderCheckpoint(oldReference))
                    val foreign = recovered.requireProviderReference().copy(leaseHolderId = "foreign-provider")
                    assertFalse(runtime.requestProviderCheckpoint(foreign))
                    assertEquals(recovered, runtime.acknowledgeProviderCheckpoint(foreign))

                    val exact = recovered.requireProviderReference()
                    assertTrue(runtime.requestProviderCheckpoint(exact))
                    val accepted = runtime.acknowledgeProviderCheckpoint(exact)
                    assertEquals(BackendDurableDeliveryState.ACCEPTED_BY_APNS, accepted.state)
                    assertNull(accepted.pendingCheckpoint)
                    assertEquals(accepted, runtime.acknowledgeProviderCheckpoint(oldReference))
                }
            }
        }
    }

    @Test
    fun `expired fanout checkpoint requires one multi JVM recovery lease and preserves frozen receipt`() = runBlocking {
        repeat(2) { round ->
            BackendNotificationDurabilityTestFixture().use { fixture ->
                val recipientKey = RecipientKey("target-recovery-$round")
                seedPendingRecipient(fixture, recipientKey)
                val registration = fixture.register("target-recovery-$round")
                val oldSnapshot = fixture.deliveryFactory.openRecipientTargetRuntime().use { runtime ->
                    val before = assertNotNull(runtime.current(recipientKey))
                    val lease = assertNotNull(
                        runtime.acquireLease(
                            BackendRecipientTargetLeaseRequest(
                                recipientKey, before.checkpointRevision, "target-old-$round",
                                before.lastLeaseVersion, before.lastLeaseVersion + 1,
                                before.lastFencingToken + 1, 130
                            )
                        )
                    )
                    runtime.stageResolution(
                        BackendRecipientTargetResolutionRequest(
                            recipientKey, listOf(registration.registrationId), 0.5,
                            lease.holderId, lease.version, lease.fencingToken
                        )
                    )
                }
                val oldCheckpoint = assertNotNull(oldSnapshot.pendingCheckpoint)
                val oldReference = oldSnapshot.requireTargetReference()
                fixture.deliveryFactory.openRecipientTargetRuntime().use { runtime ->
                    runtime.advanceLogicalClock(recipientKey, oldSnapshot.clockRevision, 130)
                }

                val gate = fixture.root.resolve("target-recovery-gate-$round")
                Files.createDirectories(gate)
                val processes = (0..1).map { index ->
                    startProcess(
                        fixture,
                        listOf(
                            "TARGET_RECOVER", recipientKey.value, "target-new-$index", (101 + index).toString(),
                            gate.resolve("ready-$index").toString(), gate.resolve("go").toString(),
                            gate.resolve("result-$index").toString()
                        )
                    )
                }
                try {
                    awaitFiles(gate.resolve("ready-0"), gate.resolve("ready-1"))
                    gate.resolve("go").writeText("go")
                    processes.forEach { awaitSuccessfulChild(it, "target checkpoint recovery") }
                    val results = (0..1).map { gate.resolve("result-$it").readText().trim() }
                    assertEquals(1, results.count { it.startsWith("LEASE|") }, results.toString())
                    assertEquals(1, results.count { it == "NULL" }, results.toString())
                    assertTrue(results.none { it.startsWith("ERROR|") }, results.toString())
                } finally {
                    processes.forEach(::terminateChild)
                }

                fixture.deliveryFactory.openRecipientTargetRuntime().use { runtime ->
                    val recovered = assertNotNull(runtime.current(recipientKey))
                    val newCheckpoint = assertNotNull(recovered.pendingCheckpoint)
                    assertTrue(recovered.lastLeaseVersion > oldSnapshot.lastLeaseVersion)
                    assertTrue(recovered.lastFencingToken > oldSnapshot.lastFencingToken)
                    assertTrue(newCheckpoint.revision > oldCheckpoint.revision)
                    assertNotEquals(oldCheckpoint.effectId, newCheckpoint.effectId)
                    assertEquals(oldCheckpoint.transactionReceiptId, newCheckpoint.transactionReceiptId)
                    assertEquals(oldCheckpoint.deliveries, newCheckpoint.deliveries)
                    assertEquals(recovered.lastLeaseVersion, newCheckpoint.leaseVersion)
                    assertEquals(recovered.lastFencingToken, newCheckpoint.fencingToken)
                    assertFalse(newCheckpoint.effectRequested)

                    assertFalse(runtime.requestCheckpoint(oldReference))
                    assertEquals(recovered, runtime.acknowledgeCheckpoint(oldReference))
                    val foreign = recovered.requireTargetReference().copy(holderId = "foreign-target")
                    assertFalse(runtime.requestCheckpoint(foreign))
                    assertEquals(recovered, runtime.acknowledgeCheckpoint(foreign))

                    val exact = recovered.requireTargetReference()
                    assertTrue(runtime.requestCheckpoint(exact))
                    val targeted = runtime.acknowledgeCheckpoint(exact)
                    assertEquals(BackendRecipientTargetState.TARGETED, targeted.state)
                    assertEquals(1, targeted.deliveryKeys.size)
                    assertEquals(targeted, runtime.acknowledgeCheckpoint(oldReference))
                }
            }
        }
    }

    private fun composition(
        fixture: BackendNotificationDurabilityTestFixture,
        provider: BackendDeliveryProviderPort,
        fault: BackendDeliveryWorkerFaultInjector
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
        faultInjector = fault
    )

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
            "checkpoint-recovery-$identity", "DATE_CONFIRMED", 1,
            "logical-checkpoint-recovery-$identity",
            listOf(
                BackendNotificationRecipientIntent(
                    "participant-$identity", "push", "apns",
                    listOf(registration.registrationId), 1_000
                )
            )
        )
    ).deliveryKeys.single()

    private suspend fun seedPendingRecipient(
        fixture: BackendNotificationDurabilityTestFixture,
        recipientKey: RecipientKey
    ) {
        fixture.deliveryFactory.open().use { store ->
            assertTrue(
                store.persistPendingRecipient(
                    BackendNotificationRecipient(
                        recipientKey, EffectKey("effect-${recipientKey.value}"),
                        BackendRecipientStatus.PENDING_TARGET, emptySet(), 1_000
                    )
                )
            )
        }
    }

    private fun BackendNotificationDeliverySnapshot.requireProviderReference(): BackendDeliveryCheckpointReference {
        val checkpoint = assertNotNull(pendingCheckpoint)
        return BackendDeliveryCheckpointReference(
            deliveryKey, checkpoint.effectId, checkpoint.revision, authority, authorityFencingToken,
            checkpoint.leaseHolderId, checkpoint.leaseVersion, checkpoint.leaseFencingToken
        )
    }

    private fun BackendRecipientTargetSnapshot.requireTargetReference(): BackendRecipientTargetCheckpointReference {
        val checkpoint = assertNotNull(pendingCheckpoint)
        return BackendRecipientTargetCheckpointReference(
            recipientKey, checkpoint.effectId, checkpoint.revision, checkpoint.transactionReceiptId,
            checkpoint.holderId, checkpoint.leaseVersion, checkpoint.fencingToken
        )
    }

    private fun startProcess(fixture: BackendNotificationDurabilityTestFixture, arguments: List<String>): Process =
        ProcessBuilder(
            Path.of(System.getProperty("java.home"), "bin", "java").toString(),
            "-cp", System.getProperty("java.class.path"),
            BackendNotificationCheckpointRecoveryProcessMain::class.java.name,
            fixture.databasePath.toString(),
            *arguments.toTypedArray()
        ).redirectErrorStream(true).start()

    private fun awaitFiles(vararg files: Path) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (files.any { !it.exists() } && System.nanoTime() < deadline) Thread.sleep(10)
        assertTrue(files.all { it.exists() }, "checkpoint recovery children did not reach the start gate")
    }

    private fun awaitSuccessfulChild(process: Process, label: String) {
        val finished = process.waitFor(10, TimeUnit.SECONDS)
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
}

object BackendNotificationCheckpointRecoveryProcessMain {
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        val configuration = DeviceRegistrationStoreConfiguration.resolve(
            systemProperties = mapOf(
                "wakeve.notification.device-registration.db.path" to args[0],
                "wakeve.notification.device-registration.legacy-identity-hmac-key" to "h".repeat(32),
                "wakeve.notification.device-registration.token-encryption-key" to "e".repeat(32)
            ),
            environment = emptyMap()
        ).getOrThrow()
        val factory = SqliteBackendNotificationDeliveryStoreFactory(configuration)
        val mode = args[1]
        val ready = Path.of(if (mode == "DELIVERY_RECOVER") args[4] else args[5])
        val go = Path.of(if (mode == "DELIVERY_RECOVER") args[5] else args[6])
        val result = Path.of(if (mode == "DELIVERY_RECOVER") args[6] else args[7])
        ready.writeText("ready")
        awaitGo(go)
        runCatching {
            when (mode) {
                "DELIVERY_RECOVER" -> factory.openDeliveryRuntime().use { runtime ->
                    runtime.acquireLease(DeliveryKey(args[2]), args[3])?.let {
                        "LEASE|${it.holderId}|${it.version}|${it.fencingToken}"
                    } ?: "NULL"
                }

                "TARGET_RECOVER" -> factory.openRecipientTargetRuntime().use { runtime ->
                    val key = RecipientKey(args[2])
                    val snapshot = checkNotNull(runtime.current(key))
                    runtime.acquireLease(
                        BackendRecipientTargetLeaseRequest(
                            key, snapshot.checkpointRevision, args[3], snapshot.lastLeaseVersion,
                            snapshot.lastLeaseVersion + 1, args[4].toLong(), snapshot.nowEpochSeconds + 30
                        )
                    )?.let { "LEASE|${it.holderId}|${it.version}|${it.fencingToken}" } ?: "NULL"
                }

                else -> error("unsupported recovery mode $mode")
            }
        }.fold(
            onSuccess = result::writeText,
            onFailure = { result.writeText("ERROR|${it::class.java.simpleName}|${it.message}") }
        )
    }

    private fun awaitGo(go: Path) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (!go.exists() && System.nanoTime() < deadline) Thread.sleep(10)
        check(go.exists()) { "checkpoint recovery start gate timed out" }
    }
}
