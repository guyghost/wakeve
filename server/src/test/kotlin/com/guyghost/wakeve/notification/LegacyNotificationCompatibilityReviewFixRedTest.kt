package com.guyghost.wakeve.notification

import com.guyghost.wakeve.JvmDatabaseFactory
import com.guyghost.wakeve.database.WakeveDb
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path
import java.sql.DriverManager
import java.util.concurrent.TimeUnit
import kotlin.io.path.deleteIfExists
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Minimal RED reproductions for the independent compatibility-saga review findings. */
class LegacyNotificationCompatibilityReviewFixRedTest {
    private val temporaryRoots = mutableListOf<Path>()

    @AfterTest
    fun tearDown() {
        temporaryRoots.forEach { root ->
            Files.walk(root).use { paths ->
                paths.sorted(Comparator.reverseOrder()).forEach { path -> path.deleteIfExists() }
            }
        }
        temporaryRoots.clear()
    }

    @Test
    fun `token custodian permits sink use only during its revocable scope`() = runBlocking {
        val fixture = fixture("secret-scope")
        val clock = ControlledCompatibilityClock(100)
        val jitter = FixedCompatibilityJitter(1.0)
        val rawToken = "raw-token-must-not-escape"
        val command = fixture.legacyCommand(
            sagaId = "saga-secret-scope",
            tokenFingerprint = compatibilityTokenFingerprint(rawToken)
        )
        val custodian = FakeRevocableTokenCustodian(command.sagaId, rawToken.toCharArray())
        val writeTrace = mutableListOf<String>()
        val legacySink = InstrumentedScopedStoreSink(
            label = "legacy",
            expectedFingerprint = command.tokenFingerprint,
            writeTrace = writeTrace
        )
        val v2Sink = InstrumentedScopedStoreSink(
            label = "v2",
            expectedFingerprint = command.tokenFingerprint,
            writeTrace = writeTrace
        )
        val converged = fixture.worker(
            clock = clock::nowEpochSeconds,
            jitter = jitter::nextUnitDouble,
            tokenCustodian = custodian,
            legacyStoreSink = legacySink,
            v2StoreSink = v2Sink
        ).execute(command)

        assertEquals(LegacyCompatibilitySagaState.CONVERGED, converged.state)
        assertEquals(listOf("legacy", "v2"), writeTrace)
        assertEquals(1, legacySink.writesWithinActiveScope)
        assertEquals(1, v2Sink.writesWithinActiveScope)
        assertEquals(2, custodian.scopedConsumptions)
        legacySink.assertCapturedScopeIsRevoked()
        v2Sink.assertCapturedScopeIsRevoked()
    }

    private class FakeRevocableTokenCustodian(
        private val expectedSagaId: String,
        private val rawToken: CharArray
    ) : LegacyCompatibilityTokenCustodian {
        var lastClosedScope: LegacyCompatibilityScopedToken? = null
            private set
        var scopedConsumptions: Int = 0
            private set

        override suspend fun withRegistrationToken(
            sagaId: String,
            block: suspend (LegacyCompatibilityScopedToken) -> Unit
        ): Boolean {
            require(sagaId == expectedSagaId)
            val scope = FakeScopedToken(rawToken.copyOf()) { scopedConsumptions += 1 }
            return try {
                block(scope)
                true
            } finally {
                scope.close()
                lastClosedScope = scope
            }
        }
    }

    private class FakeScopedToken(
        private val rawToken: CharArray,
        private val onConsumption: () -> Unit
    ) : LegacyCompatibilityScopedToken {
        private var active = true

        override suspend fun consumeWith(sink: suspend (CharArray) -> Unit) {
            check(active) { "Scoped registration token is closed" }
            val oneUseCopy = rawToken.copyOf()
            try {
                sink(oneUseCopy)
                onConsumption()
            } finally {
                oneUseCopy.fill('\u0000')
            }
        }

        fun close() {
            active = false
            rawToken.fill('\u0000')
        }
    }

    private class InstrumentedScopedStoreSink(
        private val label: String,
        private val expectedFingerprint: String?,
        private val writeTrace: MutableList<String>
    ) : LegacyCompatibilityStoreSink {
        var writesWithinActiveScope: Int = 0
            private set
        private var capturedScope: LegacyCompatibilityScopedToken? = null

        override suspend fun write(
            snapshot: LegacyCompatibilitySnapshot,
            tokenScope: LegacyCompatibilityScopedToken?
        ): LegacyCompatibilityWriteOutcome {
            assertEquals(LegacyCompatibilityOperation.REGISTER, snapshot.operation)
            val activeScope = assertNotNull(tokenScope)
            capturedScope = activeScope
            var fingerprintAtWrite: String? = null
            activeScope.consumeWith { rawToken ->
                fingerprintAtWrite = compatibilityTokenFingerprint(rawToken.concatToString())
                rawToken.fill('\u0000')
            }
            assertEquals(expectedFingerprint, fingerprintAtWrite)
            writesWithinActiveScope += 1
            writeTrace += label
            return LegacyCompatibilityWriteOutcome.APPLIED
        }

        suspend fun assertCapturedScopeIsRevoked() {
            var writesAfterClose = 0
            val closedScope = assertNotNull(capturedScope)
            assertFailsWith<IllegalStateException> {
                closedScope.consumeWith { writesAfterClose += 1 }
            }
            assertEquals(
                0,
                writesAfterClose,
                "$label sink must not write after the custody scope is revoked."
            )
        }
    }

    @Test
    fun `request key has a SQLite unique constraint`() {
        val fixture = fixture("request-key-schema")
        fixture.registrationFactory.openCompatibilitySagaStore().close()

        assertTrue(
            uniqueIndexes(fixture.registrationDatabasePath, "legacy_notification_compatibility_saga")
                .any { columns -> columns == listOf("request_key") },
            "SQLite must own UNIQUE(request_key), not only process-local coalescing."
        )
    }

    @Test
    fun `two store instances atomically coalesce the same request key without primary-key failure`() = runBlocking {
        val fixture = fixture("request-key-race")
        val first = fixture.legacyCommand(sagaId = "saga-request-a")
        val second = fixture.legacyCommand(sagaId = "saga-request-b")
        assertEquals(first.requestKey, second.requestKey)
        val start = CompletableDeferred<Unit>()

        val results = coroutineScope {
            listOf(first, second).map { command ->
                async(Dispatchers.IO) {
                    start.await()
                    runCatching {
                        fixture.registrationFactory.openCompatibilitySagaStore().use {
                            it.persistIntent(command)
                        }
                    }
                }
            }.also { start.complete(Unit) }.awaitAll()
        }

        assertTrue(results.all { it.isSuccess }, "A uniqueness race must coalesce, never expose a PK error.")
        val snapshots = results.map { it.getOrThrow() }
        assertEquals(1, snapshots.map { it.sagaId }.toSet().size)
        assertEquals(1, countRowsForRequestKey(fixture.registrationDatabasePath, first.requestKey))
    }

    @Test
    fun `two JVM processes allow one lease holder and the stale loser emits no effect`() = runBlocking {
        val fixture = fixture("lease-process-race")
        repeat(4) { round -> assertInterProcessLeaseRound(fixture, round) }
    }

    private suspend fun assertInterProcessLeaseRound(fixture: ReviewFixture, round: Int) {
        val command = fixture.legacyCommand(
            sagaId = "saga-lease-race-$round",
            tokenFingerprint = compatibilityTokenFingerprint("lease-token-$round")
        )
        val snapshot = fixture.registrationFactory.openCompatibilitySagaStore().use {
            it.persistIntent(command)
        }
        val effect = assertNotNull(snapshot.requiredEffect)
        val root = assertNotNull(fixture.registrationDatabasePath.parent)
        val attemptsRelease = root.resolve("lease-race-$round.attempts-release")
        val contenders = listOf("worker-a", "worker-b").mapIndexed { index, holder ->
            LeaseProcessContender(
                holderId = holder,
                fencingToken = effect.fencingToken + 10 + index,
                readyPath = root.resolve("$holder-$round.ready"),
                attemptStartedPath = root.resolve("$holder-$round.attempt-started"),
                validatedReadPath = root.resolve("$holder-$round.validated-read"),
                validationReleasePath = root.resolve("$holder-$round.validation-release"),
                resultPath = root.resolve("$holder-$round.result"),
                logPath = root.resolve("$holder-$round.log")
            )
        }
        val processes = contenders.map { contender ->
            launchLeaseProcess(fixture, command.sagaId, contender, attemptsRelease)
        }
        try {
            waitUntilFilesExist(
                paths = contenders.map { it.readyPath },
                barrier = "ready",
                diagnosticLogs = contenders.map { it.logPath }
            )
            waitUntilFilesExist(
                paths = contenders.map { it.attemptStartedPath },
                barrier = "attempt-started",
                diagnosticLogs = contenders.map { it.logPath }
            )
            Files.writeString(attemptsRelease, "release-attempts")
            coordinateValidatedReads(contenders)
            processes.forEach { process ->
                assertTrue(process.waitFor(15, TimeUnit.SECONDS), "Lease contender did not terminate.")
            }
            val processLogs = contenders.map { Files.readString(it.logPath) }
            assertTrue(processes.all { it.exitValue() == 0 }, processLogs.joinToString())

            val results = contenders.associateWith { contender ->
                Files.readString(contender.resultPath)
            }
            assertTrue(results.values.none { it.startsWith("ERROR|") }, results.values.joinToString())
            assertTrue(results.values.none { it.contains("BUSY", ignoreCase = true) }, results.values.joinToString())
            assertEquals(1, results.values.count { it.startsWith("LEASE|") })
            assertEquals(1, results.values.count { it == "NULL" })

            val winnerHolder = results.entries.single { it.value.startsWith("LEASE|") }.key.holderId
            val loser = results.keys.single { it.holderId != winnerHolder }
            val historyBeforeStaleAttempt = fixture.registrationFactory.openCompatibilitySagaStore().use {
                it.effectHistory(command.sagaId)
            }
            val staleRecovery = LegacyCompatibilityRecoveryRequest(
                sagaId = command.sagaId,
                leaseId = "stale-loser-lease",
                holderId = loser.holderId,
                leaseVersion = 1,
                fencingToken = loser.fencingToken,
                expiresAtEpochSeconds = 110,
                expectedEffectId = effect.effectId,
                effectCheckpoint = effect.checkpoint,
                checkpointRevision = effect.checkpointRevision
            )
            fixture.registrationFactory.openCompatibilitySagaStore().use { store ->
                assertNull(store.requestRecovery(staleRecovery))
                assertEquals(historyBeforeStaleAttempt, store.effectHistory(command.sagaId))
            }
        } finally {
            processes.filter(Process::isAlive).forEach(Process::destroyForcibly)
        }
    }

    @Test
    fun `recovery composition reopens retry wait and converges when its controlled clock reaches deadline`() = runBlocking {
        val fixture = fixture("retry-recovery")
        val clock = ControlledCompatibilityClock(100)
        val jitter = FixedCompatibilityJitter(1.0)
        val rawToken = "retry-token"
        val command = fixture.legacyCommand(
            sagaId = "saga-retry-recovery",
            tokenFingerprint = compatibilityTokenFingerprint(rawToken)
        )
        val start = fixture.registrationFactory.openCompatibilitySagaStore().use {
            it.persistIntent(command, rawToken)
        }
        val recordingRetry = fixture.registrationFactory.openCompatibilitySagaStore().use {
            it.acknowledgeWriteFailed(
                command.sagaId,
                assertNotNull(start.requiredEffect),
                LegacyCompatibilityFailure.UNAVAILABLE
            )
        }
        assertEquals(LegacyCompatibilitySagaState.RECORDING_RETRY, recordingRetry.state)

        val firstWorker = fixture.worker(clock::nowEpochSeconds, jitter::nextUnitDouble)
        val waiting = firstWorker.resume(command.sagaId)
        assertEquals(LegacyCompatibilitySagaState.RETRY_WAIT, waiting.state)
        val deadline = assertNotNull(waiting.nextRetryAtEpochSeconds)

        val beforeDeadline: List<LegacyCompatibilitySnapshot> = fixture
            .worker(clock::nowEpochSeconds, jitter::nextUnitDouble)
            .recoverDue()
        assertTrue(
            beforeDeadline.none { snapshot -> snapshot.sagaId == command.sagaId },
            "Recovery must not run before the durable deadline."
        )
        clock.advanceTo(deadline)
        val recoveredSnapshots: List<LegacyCompatibilitySnapshot> = fixture
            .worker(clock::nowEpochSeconds, jitter::nextUnitDouble)
            .recoverDue()
        val recovered = recoveredSnapshots.single { snapshot -> snapshot.sagaId == command.sagaId }
        assertEquals(
            LegacyCompatibilitySagaState.CONVERGED,
            recovered.state,
            "A fresh recovery composition must discover and resume the durable checkpoint without HTTP."
        )
        assertEquals(LegacyCompatibilityReconciliationStatus.CONVERGED, recovered.reconciliationStatus)
    }

    @Test
    fun `N-1 and register commands reject target identity shapes outside the reviewed model`() {
        val fixture = fixture("command-validation")
        val identity = fixture.legacyIdentity
        val common = fixture.legacyCommand(sagaId = "valid-shape")

        val mismatchedInstallation = LegacyCompatibilityCommand.create(
            sagaId = "invalid-n-minus-1-installation",
            operation = common.operation,
            clientGeneration = common.clientGeneration,
            authenticatedUserId = common.authenticatedUserId,
            platform = common.platform,
            legacyPrimaryKeyFingerprint = common.legacyPrimaryKeyFingerprint,
            legacyInstallationId = identity.installationId,
            legacyRegistrationId = identity.registrationId,
            targetInstallationId = "another-installation",
            targetRegistrationId = null,
            tokenFingerprint = common.tokenFingerprint,
            compatibilityGeneration = common.compatibilityGeneration,
            maxAttemptsPerStore = common.maxAttemptsPerStore,
            initialNowEpochSeconds = common.initialNowEpochSeconds,
            scope = common.scope
        )
        assertTrue(mismatchedInstallation.isFailure)

        val nMinusOneRegistrationTarget = LegacyCompatibilityCommand.create(
            sagaId = "invalid-n-minus-1-registration-target",
            operation = common.operation,
            clientGeneration = common.clientGeneration,
            authenticatedUserId = common.authenticatedUserId,
            platform = common.platform,
            legacyPrimaryKeyFingerprint = common.legacyPrimaryKeyFingerprint,
            legacyInstallationId = identity.installationId,
            legacyRegistrationId = identity.registrationId,
            targetInstallationId = identity.installationId,
            targetRegistrationId = identity.registrationId,
            tokenFingerprint = common.tokenFingerprint,
            compatibilityGeneration = common.compatibilityGeneration,
            maxAttemptsPerStore = common.maxAttemptsPerStore,
            initialNowEpochSeconds = common.initialNowEpochSeconds,
            scope = common.scope
        )
        assertTrue(nMinusOneRegistrationTarget.isFailure)

        val nRegisterWithTargetRegistration = LegacyCompatibilityCommand.create(
            sagaId = "invalid-n-register-registration-target",
            operation = LegacyCompatibilityOperation.REGISTER,
            clientGeneration = LegacyCompatibilityClientGeneration.N,
            authenticatedUserId = "owner",
            platform = Platform.IOS,
            legacyPrimaryKeyFingerprint = null,
            legacyInstallationId = null,
            legacyRegistrationId = null,
            targetInstallationId = "installation-n",
            targetRegistrationId = "registration-must-be-null-on-register",
            tokenFingerprint = "token-fingerprint",
            compatibilityGeneration = 1,
            maxAttemptsPerStore = 3,
            initialNowEpochSeconds = 100,
            scope = fixture.scope
        )
        assertTrue(nRegisterWithTargetRegistration.isFailure)
    }

    @Test
    fun `real retry scheduling persists strict future full-jitter deadlines capped at 300`() = runBlocking {
        data class RetryCase(val attempt: Int, val sample: Double)

        val cases = listOf(
            RetryCase(attempt = 1, sample = 0.0),
            RetryCase(attempt = 1, sample = 1.0),
            RetryCase(attempt = 2, sample = 0.5),
            RetryCase(attempt = 3, sample = 0.75),
            RetryCase(attempt = 64, sample = 1.0),
            RetryCase(attempt = Int.MAX_VALUE, sample = 1.0)
        )

        cases.forEachIndexed { index, retryCase ->
            val fixture = fixture("backoff-$index")
            val rawToken = "retry-token-$index"
            val command = fixture.legacyCommand(
                sagaId = "saga-backoff-$index",
                tokenFingerprint = compatibilityTokenFingerprint(rawToken),
                maxAttemptsPerStore = Int.MAX_VALUE
            )
            val start = fixture.registrationFactory.openCompatibilitySagaStore().use {
                it.persistIntent(command, rawToken)
            }
            var recordingRetry = fixture.registrationFactory.openCompatibilitySagaStore().use {
                it.acknowledgeWriteFailed(
                    command.sagaId,
                    assertNotNull(start.requiredEffect),
                    LegacyCompatibilityFailure.UNAVAILABLE
                )
            }
            recordingRetry = advanceThroughRealFailures(
                fixture = fixture,
                snapshot = recordingRetry,
                targetAttempt = minOf(retryCase.attempt, 3)
            )
            if (retryCase.attempt > 3) {
                seedLegacyRetryAttempt(
                    databasePath = fixture.registrationDatabasePath,
                    sagaId = command.sagaId,
                    attempt = retryCase.attempt
                )
                recordingRetry = fixture.registrationFactory.openCompatibilitySagaStore().use {
                    assertNotNull(it.findBySagaId(command.sagaId))
                }
            }
            assertEquals(retryCase.attempt, recordingRetry.legacyAttempt)
            assertEquals(LegacyCompatibilitySagaState.RECORDING_RETRY, recordingRetry.state)

            val clock = ControlledCompatibilityClock(recordingRetry.logicalNowEpochSeconds)
            val jitter = FixedCompatibilityJitter(retryCase.sample)
            val scheduled = fixture.worker(
                clock = clock::nowEpochSeconds,
                jitter = jitter::nextUnitDouble
            ).resume(command.sagaId)
            val persisted = fixture.registrationFactory.openCompatibilitySagaStore().use {
                assertNotNull(it.findBySagaId(command.sagaId))
            }
            val deadline = assertNotNull(persisted.nextRetryAtEpochSeconds)
            val expectedUpperBound = cappedRetryUpperBound(retryCase.attempt)
            val expectedDelay = maxOf(1L, (expectedUpperBound * retryCase.sample).toLong())

            assertEquals(LegacyCompatibilitySagaState.RETRY_WAIT, scheduled.state)
            assertEquals(deadline, scheduled.nextRetryAtEpochSeconds)
            assertEquals(recordingRetry.logicalNowEpochSeconds + expectedDelay, deadline)
            assertTrue(deadline > persisted.logicalNowEpochSeconds)
            assertTrue(deadline <= persisted.logicalNowEpochSeconds + 300)
        }
    }

    private fun cappedRetryUpperBound(attempt: Int): Long = when {
        attempt <= 1 -> 1
        attempt >= 10 -> 300
        else -> minOf(300, 1L shl (attempt - 1))
    }

    private suspend fun advanceThroughRealFailures(
        fixture: ReviewFixture,
        snapshot: LegacyCompatibilitySnapshot,
        targetAttempt: Int
    ): LegacyCompatibilitySnapshot {
        var current = snapshot
        while (current.legacyAttempt < targetAttempt) {
            val retryEffect = assertNotNull(current.requiredEffect)
            val retryAt = current.logicalNowEpochSeconds + 1
            current = fixture.registrationFactory.openCompatibilitySagaStore().use {
                it.recordRetry(current.sagaId, retryEffect, retryAt)
            }
            current = fixture.registrationFactory.openCompatibilitySagaStore().use {
                it.advanceLogicalClock(
                    sagaId = current.sagaId,
                    clockRevision = current.clockRevision + 1,
                    nowEpochSeconds = retryAt
                )
            }
            current = fixture.registrationFactory.openCompatibilitySagaStore().use {
                it.retryDue(current.sagaId, current.checkpointRevision)
            }
            current = fixture.registrationFactory.openCompatibilitySagaStore().use {
                it.acknowledgeWriteFailed(
                    current.sagaId,
                    assertNotNull(current.requiredEffect),
                    LegacyCompatibilityFailure.UNAVAILABLE
                )
            }
        }
        return current
    }

    private fun seedLegacyRetryAttempt(databasePath: Path, sagaId: String, attempt: Int) {
        DriverManager.getConnection("jdbc:sqlite:$databasePath").use { connection ->
            connection.prepareStatement(
                "UPDATE legacy_notification_compatibility_saga SET legacy_attempt = ? WHERE saga_id = ?"
            ).use { statement ->
                statement.setInt(1, attempt)
                statement.setString(2, sagaId)
                assertEquals(1, statement.executeUpdate())
            }
        }
    }

    private data class LeaseProcessContender(
        val holderId: String,
        val fencingToken: Long,
        val readyPath: Path,
        val attemptStartedPath: Path,
        val validatedReadPath: Path,
        val validationReleasePath: Path,
        val resultPath: Path,
        val logPath: Path
    )

    private fun launchLeaseProcess(
        fixture: ReviewFixture,
        sagaId: String,
        contender: LeaseProcessContender,
        attemptsReleasePath: Path
    ): Process = ProcessBuilder(
        Path.of(System.getProperty("java.home"), "bin", "java").toString(),
        "-cp",
        System.getProperty("java.class.path"),
        LegacyCompatibilityLeaseProcessProbe::class.java.name,
        fixture.registrationDatabasePath.toString(),
        sagaId,
        contender.holderId,
        contender.fencingToken.toString(),
        contender.readyPath.toString(),
        contender.attemptStartedPath.toString(),
        attemptsReleasePath.toString(),
        contender.validatedReadPath.toString(),
        contender.validationReleasePath.toString(),
        contender.resultPath.toString()
    )
        .redirectErrorStream(true)
        .redirectOutput(contender.logPath.toFile())
        .start()

    private fun coordinateValidatedReads(contenders: List<LeaseProcessContender>) {
        val firstValidatedPath = waitUntilAnyFileExists(
            paths = contenders.map { it.validatedReadPath },
            event = "first validated read",
            diagnosticLogs = contenders.map { it.logPath }
        )
        val firstContender = contenders.single { it.validatedReadPath == firstValidatedPath }
        when (readClaimStrategy(firstValidatedPath)) {
            LegacyCompatibilityLeaseClaimStrategy.OPTIMISTIC_CAS_STILL_REQUIRED -> {
                waitUntilFilesExist(
                    paths = contenders.map { it.validatedReadPath },
                    barrier = "optimistic validated-read",
                    diagnosticLogs = contenders.map { it.logPath }
                )
                assertEquals(
                    setOf(LegacyCompatibilityLeaseClaimStrategy.OPTIMISTIC_CAS_STILL_REQUIRED),
                    contenders.map { readClaimStrategy(it.validatedReadPath) }.toSet(),
                    "Optimistic contenders must validate the same snapshot before either mutation is released."
                )
                contenders.forEach { contender ->
                    Files.writeString(contender.validationReleasePath, "release-validated-read")
                }
            }

            LegacyCompatibilityLeaseClaimStrategy.SERIALIZED_MUTATION_AUTHORITY_ALREADY_HELD -> {
                assertEquals(
                    listOf(firstContender),
                    contenders.filter { Files.exists(it.validatedReadPath) },
                    "Serialized mutation authority must not be held by both contenders concurrently."
                )
                Files.writeString(firstContender.validationReleasePath, "release-validated-read")

                val secondContender = contenders.single { it != firstContender }
                when (
                    waitUntilAnyFileExists(
                        paths = listOf(secondContender.validatedReadPath, secondContender.resultPath),
                        event = "serialized follower validation or rejection",
                        diagnosticLogs = contenders.map { it.logPath }
                    )
                ) {
                    secondContender.validatedReadPath -> {
                        assertEquals(
                            LegacyCompatibilityLeaseClaimStrategy.SERIALIZED_MUTATION_AUTHORITY_ALREADY_HELD,
                            readClaimStrategy(secondContender.validatedReadPath)
                        )
                        Files.writeString(secondContender.validationReleasePath, "release-validated-read")
                    }

                    secondContender.resultPath -> assertEquals(
                        "NULL",
                        Files.readString(secondContender.resultPath),
                        "A serialized follower rejected after validation must not acquire a lease."
                    )
                }
            }
        }
    }

    private fun readClaimStrategy(path: Path): LegacyCompatibilityLeaseClaimStrategy =
        LegacyCompatibilityLeaseClaimStrategy.valueOf(Files.readString(path))

    private fun waitUntilAnyFileExists(
        paths: List<Path>,
        event: String,
        diagnosticLogs: List<Path>
    ): Path {
        val deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(15)
        while (paths.none(Files::exists) && System.nanoTime() < deadlineNanos) {
            Thread.sleep(10)
        }
        return assertNotNull(
            paths.firstOrNull(Files::exists),
            buildString {
                append("$event missing: ${paths.joinToString()}")
                diagnosticLogs.filter(Files::exists).forEach { log ->
                    append("\n${log.fileName}: ${Files.readString(log).takeLast(2_000)}")
                }
            }
        )
    }

    private fun waitUntilFilesExist(
        paths: List<Path>,
        barrier: String,
        diagnosticLogs: List<Path>
    ) {
        val deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(15)
        while (paths.any { !Files.exists(it) } && System.nanoTime() < deadlineNanos) {
            Thread.sleep(10)
        }
        assertTrue(
            paths.all(Files::exists),
            buildString {
                append("$barrier barrier missing: ${paths.filterNot(Files::exists).joinToString()}")
                diagnosticLogs.filter(Files::exists).forEach { log ->
                    append("\n${log.fileName}: ${Files.readString(log).takeLast(2_000)}")
                }
            }
        )
    }

    private class ControlledCompatibilityClock(initialNowEpochSeconds: Long) {
        private var currentNowEpochSeconds = initialNowEpochSeconds

        fun nowEpochSeconds(): Long = currentNowEpochSeconds

        fun advanceTo(nowEpochSeconds: Long) {
            require(nowEpochSeconds >= currentNowEpochSeconds)
            currentNowEpochSeconds = nowEpochSeconds
        }
    }

    private class FixedCompatibilityJitter(private val sample: Double) {
        init {
            require(sample in 0.0..1.0)
        }

        fun nextUnitDouble(): Double = sample
    }

    private fun fixture(name: String): ReviewFixture {
        val root = Files.createTempDirectory("wakeve-legacy-review-$name-")
        temporaryRoots.add(root)
        val registrationPath = root.resolve("registration.sqlite")
        val configuration = DeviceRegistrationStoreConfiguration.resolve(
            systemProperties = mapOf(
                "wakeve.notification.device-registration.db.path" to registrationPath.toString(),
                "wakeve.notification.device-registration.legacy-identity-hmac-key" to "h".repeat(32),
                "wakeve.notification.device-registration.token-encryption-key" to "e".repeat(32)
            ),
            environment = emptyMap()
        ).getOrThrow()
        val registrationFactory = SqliteBackendDeviceRegistrationStoreFactory(configuration)
        val database = WakeveDb(JvmDatabaseFactory(root.resolve("legacy.sqlite").toString()).createDriver())
        val notificationService = NotificationService(
            database = database,
            preferencesRepository = NotificationPreferencesRepository(database),
            fcmSender = NoConfiguredFCMSender,
            apnsSender = NoConfiguredAPNsSender
        )
        val scope = DeviceRegistrationScope.create(
            APNsEnvironment.PRODUCTION,
            LEGACY_COMPATIBILITY_IOS_TOPIC
        ).getOrThrow()
        return ReviewFixture(
            registrationFactory = registrationFactory,
            registrationDatabasePath = registrationPath,
            notificationService = notificationService,
            scope = scope,
            legacyIdentity = registrationFactory.deriveLegacyCompatibilityIdentity(
                legacyCompatibilityRowKey("owner", Platform.IOS)
            )
        )
    }

    private fun uniqueIndexes(databasePath: Path, table: String): List<List<String>> =
        DriverManager.getConnection("jdbc:sqlite:$databasePath").use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("PRAGMA index_list('$table')").use { indexes ->
                    buildList {
                        while (indexes.next()) {
                            if (indexes.getInt("unique") != 1) continue
                            val indexName = indexes.getString("name").replace("'", "''")
                            connection.createStatement().use { columnsStatement ->
                                columnsStatement.executeQuery("PRAGMA index_info('$indexName')").use { columns ->
                                    add(buildList { while (columns.next()) add(columns.getString("name")) })
                                }
                            }
                        }
                    }
                }
            }
        }

    private fun countRowsForRequestKey(databasePath: Path, requestKey: String): Int =
        DriverManager.getConnection("jdbc:sqlite:$databasePath").use { connection ->
            connection.prepareStatement(
                "SELECT COUNT(*) FROM legacy_notification_compatibility_saga WHERE request_key = ?"
            ).use { statement ->
                statement.setString(1, requestKey)
                statement.executeQuery().use { rows ->
                    assertTrue(rows.next())
                    rows.getInt(1)
                }
            }
        }

    private data class ReviewFixture(
        val registrationFactory: SqliteBackendDeviceRegistrationStoreFactory,
        val registrationDatabasePath: Path,
        val notificationService: NotificationService,
        val scope: DeviceRegistrationScope,
        val legacyIdentity: LegacyCompatibilityIdentity
    ) {
        fun worker(
            clock: () -> Long,
            jitter: () -> Double,
            tokenCustodian: LegacyCompatibilityTokenCustodian? = null,
            legacyStoreSink: LegacyCompatibilityStoreSink? = null,
            v2StoreSink: LegacyCompatibilityStoreSink? = null
        ): LegacyNotificationRegistrationCompatibilityWorker = if (
            tokenCustodian == null && legacyStoreSink == null && v2StoreSink == null
        ) {
            LegacyNotificationRegistrationCompatibilityWorker(
                notificationService = notificationService,
                registrationStoreFactory = registrationFactory,
                logicalClock = clock,
                jitterSource = jitter
            )
        } else {
            LegacyNotificationRegistrationCompatibilityWorker(
                notificationService = notificationService,
                registrationStoreFactory = registrationFactory,
                logicalClock = clock,
                jitterSource = jitter,
                tokenCustodian = assertNotNull(tokenCustodian),
                legacyStoreSink = assertNotNull(legacyStoreSink),
                v2StoreSink = assertNotNull(v2StoreSink)
            )
        }

        fun legacyCommand(
            sagaId: String,
            tokenFingerprint: String = "token-fingerprint",
            maxAttemptsPerStore: Int = 3
        ): LegacyCompatibilityCommand = LegacyCompatibilityCommand.create(
            sagaId = sagaId,
            operation = LegacyCompatibilityOperation.REGISTER,
            clientGeneration = LegacyCompatibilityClientGeneration.N_MINUS_1,
            authenticatedUserId = "owner",
            platform = Platform.IOS,
            legacyPrimaryKeyFingerprint = "legacy-primary-key-fingerprint",
            legacyInstallationId = legacyIdentity.installationId,
            legacyRegistrationId = legacyIdentity.registrationId,
            targetInstallationId = legacyIdentity.installationId,
            targetRegistrationId = null,
            tokenFingerprint = tokenFingerprint,
            compatibilityGeneration = 1,
            maxAttemptsPerStore = maxAttemptsPerStore,
            initialNowEpochSeconds = 100,
            scope = scope
        ).getOrThrow()
    }
}

/** Separate test JVM entry point: JVM-static locks cannot coordinate these contenders. */
object LegacyCompatibilityLeaseProcessProbe {
    @JvmStatic
    fun main(args: Array<String>): Unit = runBlocking {
        val databasePath = Path.of(args[0])
        val sagaId = args[1]
        val holderId = args[2]
        val fencingToken = args[3].toLong()
        val readyPath = Path.of(args[4])
        val attemptStartedPath = Path.of(args[5])
        val attemptsReleasePath = Path.of(args[6])
        val validatedReadPath = Path.of(args[7])
        val validationReleasePath = Path.of(args[8])
        val resultPath = Path.of(args[9])
        val result = runCatching {
            val configuration = DeviceRegistrationStoreConfiguration.resolve(
                systemProperties = mapOf(
                    "wakeve.notification.device-registration.db.path" to databasePath.toString(),
                    "wakeve.notification.device-registration.legacy-identity-hmac-key" to "h".repeat(32),
                    "wakeve.notification.device-registration.token-encryption-key" to "e".repeat(32)
                ),
                environment = emptyMap()
            ).getOrThrow()
            val factory = SqliteBackendDeviceRegistrationStoreFactory(configuration)
            val claimProbe = object : LegacyCompatibilityLeaseClaimProbe {
                override suspend fun attemptStarted(request: LegacyCompatibilityRecoveryLeaseRequest) {
                    validateRequest(request)
                    Files.writeString(attemptStartedPath, "attempt-started")
                    waitForRelease(attemptsReleasePath, "Attempt-start release")
                }

                override suspend fun validatedRead(
                    request: LegacyCompatibilityRecoveryLeaseRequest,
                    strategy: LegacyCompatibilityLeaseClaimStrategy
                ) {
                    validateRequest(request)
                    Files.writeString(validatedReadPath, strategy.name)
                    waitForRelease(validationReleasePath, "Validated-read release")
                }

                private fun validateRequest(request: LegacyCompatibilityRecoveryLeaseRequest) {
                    check(request.sagaId == sagaId)
                    check(request.holderId == holderId)
                    check(request.fencingToken == fencingToken)
                }
            }
            factory.openCompatibilitySagaStore(leaseClaimProbe = claimProbe).use { store ->
                val snapshot = assertNotNull(store.findBySagaId(sagaId))
                val effect = assertNotNull(snapshot.requiredEffect)
                Files.writeString(readyPath, "ready")
                store.acquireRecoveryLease(
                    LegacyCompatibilityRecoveryLeaseRequest(
                        sagaId = sagaId,
                        expectedEffectId = effect.effectId,
                        effectCheckpoint = effect.checkpoint,
                        checkpointRevision = effect.checkpointRevision,
                        holderId = holderId,
                        expectedLeaseVersion = 0,
                        newLeaseVersion = 1,
                        fencingToken = fencingToken,
                        expiresAtEpochSeconds = 110
                    )
                )
            }.let { lease ->
                if (lease == null) "NULL" else {
                    "LEASE|${lease.holderId}|${lease.leaseId}|${lease.fencingToken}"
                }
            }
        }.getOrElse { failure ->
            "ERROR|${failure::class.simpleName}|${failure.message.orEmpty().replace('|', '/')}"
        }
        Files.writeString(resultPath, result)
        Unit
    }

    private fun waitForRelease(path: Path, label: String) {
        val waitDeadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(15)
        while (!Files.exists(path) && System.nanoTime() < waitDeadlineNanos) {
            Thread.sleep(10)
        }
        check(Files.exists(path)) { "$label timed out" }
    }
}
