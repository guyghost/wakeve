package com.guyghost.wakeve.notification

import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BackendRecipientTargetResolverHardeningRedTest {
    @Test
    fun `lease rejects caller chosen invalid holder version fence and expiry without mutation`() = runBlocking {
        val invalidRequests = listOf<(BackendRecipientTargetSnapshot) -> BackendRecipientTargetLeaseRequest>(
            { leaseRequest(it, "", 1, 1, 150) },
            { leaseRequest(it, "resolver", it.lastLeaseVersion, 1, 150) },
            { leaseRequest(it, "resolver", it.lastLeaseVersion - 1, 1, 150) },
            { leaseRequest(it, "resolver", 1, it.lastFencingToken, 150) },
            { leaseRequest(it, "resolver", 1, it.lastFencingToken - 1, 150) },
            { leaseRequest(it, "resolver", 1, 1, it.nowEpochSeconds) },
            { leaseRequest(it, "resolver", 1, 1, it.nowEpochSeconds - 1) }
        )
        invalidRequests.forEachIndexed { index, invalid ->
            BackendNotificationDurabilityTestFixture().use { fixture ->
                val key = RecipientKey("invalid-lease-$index")
                seedPendingRecipient(fixture, key, 1_000)
                fixture.deliveryFactory.openRecipientTargetRuntime().use { runtime ->
                    val before = assertNotNull(runtime.current(key))
                    assertNull(runtime.acquireLease(invalid(before)), "invalid request $index")
                    assertEquals(before, runtime.current(key), "invalid request $index mutated durable state")
                }
            }
        }
    }

    @Test
    fun `four multi process lease rounds yield one holder and one fenced loser`() = runBlocking {
        repeat(4) { round ->
            BackendNotificationDurabilityTestFixture().use { fixture ->
                val key = RecipientKey("multiprocess-lease-$round")
                seedPendingRecipient(fixture, key, 1_000)
                val gate = fixture.root.resolve("lease-gate-$round")
                Files.createDirectories(gate)
                val processes = listOf("resolver-a" to 11L, "resolver-b" to 12L).mapIndexed { index, pair ->
                    startProcess(
                        fixture,
                        listOf(
                            "LEASE", key.value, pair.first, "1", pair.second.toString(), "150",
                            gate.resolve("ready-$index").toString(), gate.resolve("go").toString(),
                            gate.resolve("result-$index").toString()
                        )
                    )
                }
                try {
                    awaitFiles(gate.resolve("ready-0"), gate.resolve("ready-1"))
                    gate.resolve("go").writeText("go")
                    processes.forEach { awaitSuccessfulChild(it, "target lease") }
                    val results = listOf(0, 1).map { gate.resolve("result-$it").readText().trim() }
                    assertEquals(1, results.count { it.startsWith("LEASE|") }, results.toString())
                    assertEquals(1, results.count { it == "NULL" }, results.toString())
                    fixture.deliveryFactory.openRecipientTargetRuntime().use { runtime ->
                        val durable = assertNotNull(runtime.current(key))
                        assertEquals(1L, durable.lastLeaseVersion)
                        assertTrue(durable.lastFencingToken in setOf(11L, 12L))
                    }
                } finally {
                    processes.forEach(::terminateChild)
                }
            }
        }
    }

    @Test
    fun `concurrent exact checkpoint ACK is one full CAS winner and increments attempt once`() = runBlocking {
        repeat(4) { round ->
            BackendNotificationDurabilityTestFixture().use { fixture ->
                val key = RecipientKey("multiprocess-ack-$round")
                seedPendingRecipient(fixture, key, 1_000)
                val reference = fixture.deliveryFactory.openRecipientTargetRuntime().use { runtime ->
                    val before = assertNotNull(runtime.current(key))
                    val lease = assertNotNull(runtime.acquireLease(leaseRequest(before, "stager", 1, 21, 150)))
                    val staged = runtime.stageResolution(
                        BackendRecipientTargetResolutionRequest(
                            key, emptyList(), 0.5, lease.holderId, lease.version, lease.fencingToken
                        )
                    )
                    staged.requireReference().also { assertTrue(runtime.requestCheckpoint(it)) }
                }
                val gate = fixture.root.resolve("ack-gate-$round")
                Files.createDirectories(gate)
                val processes = (0..1).map { index ->
                    startProcess(
                        fixture,
                        listOf(
                            "ACK", key.value, reference.effectId, reference.checkpointRevision.toString(),
                            reference.transactionReceiptId, reference.holderId, reference.leaseVersion.toString(),
                            reference.fencingToken.toString(), gate.resolve("ready-$index").toString(),
                            gate.resolve("go").toString(), gate.resolve("result-$index").toString()
                        )
                    )
                }
                try {
                    awaitFiles(gate.resolve("ready-0"), gate.resolve("ready-1"))
                    gate.resolve("go").writeText("go")
                    processes.forEach { awaitSuccessfulChild(it, "target ACK") }
                    val results = listOf(0, 1).map { gate.resolve("result-$it").readText().trim() }
                    assertEquals(1, results.count { it == "APPLIED|COMMITTED" }, results.toString())
                    assertEquals(1, results.count { it == "NOOP|NOT_COMMITTED" }, results.toString())

                    fixture.deliveryFactory.openRecipientTargetRuntime().use { runtime ->
                        val durable = assertNotNull(runtime.current(key))
                        assertEquals(1L, durable.attempt)
                        assertNull(durable.pendingCheckpoint)
                        val stale = runtime.acknowledgeCheckpointCas(
                            reference.copy(checkpointRevision = reference.checkpointRevision + 1)
                        )
                        val foreign = runtime.acknowledgeCheckpointCas(reference.copy(holderId = "foreign"))
                        assertFalse(stale.applied)
                        assertFalse(foreign.applied)
                        assertEquals(durable, runtime.current(key))
                    }
                } finally {
                    processes.forEach(::terminateChild)
                }
            }
        }
    }

    @Test
    fun `FANOUT frozen set receipt and stage commit are one crash recoverable CAS transaction`() = runBlocking {
        val crashPoints = listOf(
            BackendRecipientTargetFaultCheckpoint.BEFORE_FANOUT_COMMIT,
            BackendRecipientTargetFaultCheckpoint.AFTER_RECIPIENT_CAS,
            BackendRecipientTargetFaultCheckpoint.AFTER_DELIVERY_INSERTS,
            BackendRecipientTargetFaultCheckpoint.AFTER_RECEIPT_INSERT,
            BackendRecipientTargetFaultCheckpoint.AFTER_FANOUT_COMMIT
        )
        crashPoints.forEach { crashPoint ->
            BackendNotificationDurabilityTestFixture().use { fixture ->
                val key = RecipientKey("fanout-boundary-${crashPoint.name}")
                seedPendingRecipient(fixture, key, 1_000)
                val first = fixture.register("fanout-first-${crashPoint.name}")
                val second = fixture.register("fanout-second-${crashPoint.name}")
                lateinit var reference: BackendRecipientTargetCheckpointReference
                fixture.deliveryFactory.openRecipientTargetRuntime(
                    BackendRecipientTargetFaultInjector { if (it == crashPoint) error("crash-$it") }
                ).use { runtime ->
                    val before = assertNotNull(runtime.current(key))
                    val lease = assertNotNull(runtime.acquireLease(leaseRequest(before, "fanout", 1, 31, 150)))
                    val staged = runtime.stageResolution(
                        BackendRecipientTargetResolutionRequest(
                            key,
                            listOf(second.registrationId, first.registrationId, first.registrationId),
                            0.5,
                            lease.holderId,
                            lease.version,
                            lease.fencingToken
                        )
                    )
                    assertEquals(BackendRecipientTargetCheckpointKind.FANOUT, staged.pendingCheckpoint?.kind)
                    assertEquals(
                        setOf(first.registrationId, second.registrationId),
                        staged.pendingCheckpoint?.deliveries?.map { it.registrationId }?.toSet()
                    )
                    assertTrue(assertNotNull(staged.pendingCheckpoint).transactionReceiptId.isNotBlank())
                    reference = staged.requireReference()
                    assertTrue(runtime.requestCheckpoint(reference))
                    assertFails { runtime.acknowledgeCheckpoint(reference) }
                }
                val late = fixture.register("fanout-late-${crashPoint.name}")
                fixture.deliveryFactory.openRecipientTargetRuntime().use { restored ->
                    val afterCrash = assertNotNull(restored.current(key))
                    if (crashPoint == BackendRecipientTargetFaultCheckpoint.AFTER_FANOUT_COMMIT) {
                        assertEquals(BackendRecipientTargetState.TARGETED, afterCrash.state)
                    } else {
                        assertEquals(BackendRecipientTargetState.PENDING_TARGET, afterCrash.state)
                        assertTrue(afterCrash.deliveryKeys.isEmpty())
                        restored.acknowledgeCheckpoint(reference)
                    }
                    val committed = assertNotNull(restored.current(key))
                    assertEquals(BackendRecipientTargetState.TARGETED, committed.state)
                    assertEquals(2, committed.deliveryKeys.size)
                    fixture.deliveryFactory.open().use { store ->
                        assertEquals(
                            setOf(first.registrationId, second.registrationId),
                            committed.deliveryKeys.mapNotNull { store.delivery(it)?.registrationId }.toSet()
                        )
                        assertTrue(committed.deliveryKeys.none { store.delivery(it)?.registrationId == late.registrationId })
                    }
                    assertEquals(committed, restored.acknowledgeCheckpoint(reference))
                }
            }
        }
    }

    @Test
    fun `pending retry uses bounded full jitter then persists exhausted or expiry checkpoint`() = runBlocking {
        BackendNotificationDurabilityTestFixture().use { fixture ->
            val key = RecipientKey("target-jitter-budget")
            seedPendingRecipient(fixture, key, 10_000)
            val expectedDelays = listOf(1L, 1L, 3L)
            fixture.deliveryFactory.openRecipientTargetRuntime().use { runtime ->
                expectedDelays.forEachIndexed { index, expectedDelay ->
                    val before = assertNotNull(runtime.current(key))
                    if (before.nextAttemptAtEpochSeconds != null) {
                        runtime.advanceLogicalClock(key, before.clockRevision, before.nextAttemptAtEpochSeconds)
                    }
                    val due = assertNotNull(runtime.current(key))
                    val lease = assertNotNull(
                        runtime.acquireLease(
                            leaseRequest(due, "jitter-$index", index + 1L, 41L + index, due.nowEpochSeconds + 30)
                        )
                    )
                    val staged = runtime.stageResolution(
                        BackendRecipientTargetResolutionRequest(
                            key, emptyList(), 0.75, lease.holderId, lease.version, lease.fencingToken
                        )
                    )
                    val deadline = assertNotNull(staged.pendingCheckpoint?.nextAttemptAtEpochSeconds)
                    assertEquals(expectedDelay, deadline - due.nowEpochSeconds)
                    assertTrue(deadline - due.nowEpochSeconds in 1..300)
                    val reference = staged.requireReference()
                    assertTrue(runtime.requestCheckpoint(reference))
                    runtime.acknowledgeCheckpoint(reference)
                }
            }
        }

        BackendNotificationDurabilityTestFixture().use { fixture ->
            val key = RecipientKey("target-retry-expiry")
            seedPendingRecipient(fixture, key, 101)
            fixture.deliveryFactory.openRecipientTargetRuntime().use { runtime ->
                val before = assertNotNull(runtime.current(key))
                val lease = assertNotNull(runtime.acquireLease(leaseRequest(before, "expiry", 1, 51, 150)))
                val staged = runtime.stageResolution(
                    BackendRecipientTargetResolutionRequest(
                        key, emptyList(), 1.0, lease.holderId, lease.version, lease.fencingToken
                    )
                )
                assertEquals(BackendRecipientTargetCheckpointKind.EXPIRY, staged.pendingCheckpoint?.kind)
            }
        }

        BackendNotificationDurabilityTestFixture().use { fixture ->
            val key = RecipientKey("target-retry-exhausted")
            seedPendingRecipient(fixture, key, 10_000)
            fixture.deliveryFactory.openRecipientTargetRuntime().use { runtime ->
                repeat(6) { index ->
                    val before = assertNotNull(runtime.current(key))
                    before.nextAttemptAtEpochSeconds?.let {
                        runtime.advanceLogicalClock(key, before.clockRevision, it)
                    }
                    val due = assertNotNull(runtime.current(key))
                    val lease = assertNotNull(
                        runtime.acquireLease(leaseRequest(due, "budget-$index", index + 1L, 61L + index, due.nowEpochSeconds + 30))
                    )
                    val staged = runtime.stageResolution(
                        BackendRecipientTargetResolutionRequest(
                            key, emptyList(), 0.5, lease.holderId, lease.version, lease.fencingToken
                        )
                    )
                    val reference = staged.requireReference()
                    assertTrue(runtime.requestCheckpoint(reference))
                    val committed = runtime.acknowledgeCheckpoint(reference)
                    if (index == 5) {
                        assertEquals("EXHAUSTION", staged.pendingCheckpoint?.kind?.name)
                        assertEquals(BackendRecipientTargetState.TARGET_EXHAUSTED, committed.state)
                    }
                }
            }
        }
    }

    private suspend fun seedPendingRecipient(
        fixture: BackendNotificationDurabilityTestFixture,
        recipientKey: RecipientKey,
        expiresAt: Long
    ) {
        fixture.deliveryFactory.open().use { store ->
            assertTrue(
                store.persistPendingRecipient(
                    BackendNotificationRecipient(
                        recipientKey,
                        EffectKey("effect-${recipientKey.value}"),
                        BackendRecipientStatus.PENDING_TARGET,
                        emptySet(),
                        expiresAt
                    )
                )
            )
        }
    }

    private fun leaseRequest(
        snapshot: BackendRecipientTargetSnapshot,
        holder: String,
        version: Long,
        fence: Long,
        expiry: Long
    ) = BackendRecipientTargetLeaseRequest(
        snapshot.recipientKey,
        snapshot.checkpointRevision,
        holder,
        snapshot.lastLeaseVersion,
        version,
        fence,
        expiry
    )

    private fun BackendRecipientTargetSnapshot.requireReference(): BackendRecipientTargetCheckpointReference {
        val checkpoint = assertNotNull(pendingCheckpoint)
        return BackendRecipientTargetCheckpointReference(
            recipientKey,
            checkpoint.effectId,
            checkpoint.revision,
            checkpoint.transactionReceiptId,
            checkpoint.holderId,
            checkpoint.leaseVersion,
            checkpoint.fencingToken
        )
    }

    private fun startProcess(
        fixture: BackendNotificationDurabilityTestFixture,
        arguments: List<String>
    ): Process = ProcessBuilder(
        Path.of(System.getProperty("java.home"), "bin", "java").toString(),
        "-cp",
        System.getProperty("java.class.path"),
        BackendRecipientTargetResolverProcessMain::class.java.name,
        fixture.databasePath.toString(),
        *arguments.toTypedArray()
    ).redirectErrorStream(true).start()

    private fun awaitFiles(vararg files: Path) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (files.any { !it.exists() } && System.nanoTime() < deadline) Thread.sleep(10)
        assertTrue(files.all { it.exists() }, "children did not reach the upstream start gate")
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

object BackendRecipientTargetResolverProcessMain {
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        val database = args[0]
        val mode = args[1]
        val configuration = DeviceRegistrationStoreConfiguration.resolve(
            systemProperties = mapOf(
                "wakeve.notification.device-registration.db.path" to database,
                "wakeve.notification.device-registration.legacy-identity-hmac-key" to "h".repeat(32),
                "wakeve.notification.device-registration.token-encryption-key" to "e".repeat(32)
            ),
            environment = emptyMap()
        ).getOrThrow()
        val factory = SqliteBackendNotificationDeliveryStoreFactory(configuration)
        when (mode) {
            "LEASE" -> {
                val key = RecipientKey(args[2])
                val holder = args[3]
                val version = args[4].toLong()
                val fence = args[5].toLong()
                val expiry = args[6].toLong()
                val ready = Path.of(args[7])
                val go = Path.of(args[8])
                val result = Path.of(args[9])
                ready.writeText("ready")
                awaitGo(go)
                factory.openRecipientTargetRuntime().use { runtime ->
                    val snapshot = checkNotNull(runtime.current(key))
                    val lease = runtime.acquireLease(
                        BackendRecipientTargetLeaseRequest(
                            key,
                            snapshot.checkpointRevision,
                            holder,
                            snapshot.lastLeaseVersion,
                            version,
                            fence,
                            expiry
                        )
                    )
                    result.writeText(lease?.let { "LEASE|${it.holderId}|${it.version}|${it.fencingToken}" } ?: "NULL")
                }
            }

            "ACK" -> {
                val reference = BackendRecipientTargetCheckpointReference(
                    RecipientKey(args[2]), args[3], args[4].toLong(), args[5], args[6], args[7].toLong(), args[8].toLong()
                )
                val ready = Path.of(args[9])
                val go = Path.of(args[10])
                val result = Path.of(args[11])
                ready.writeText("ready")
                awaitGo(go)
                factory.openRecipientTargetRuntime().use { runtime ->
                    val outcome = runtime.acknowledgeCheckpointCas(reference)
                    result.writeText(
                        "${if (outcome.applied) "APPLIED" else "NOOP"}|" +
                            if (outcome.transactionCommitted) "COMMITTED" else "NOT_COMMITTED"
                    )
                }
            }
        }
    }

    private fun awaitGo(go: Path) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (!go.exists() && System.nanoTime() < deadline) Thread.sleep(10)
        check(go.exists()) { "start gate timed out" }
    }
}
