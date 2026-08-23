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
import java.sql.Connection
import java.sql.DriverManager
import java.util.Base64
import kotlin.io.path.deleteIfExists
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * RED runtime contracts for the approved legacyCompatibilityUniqueMigration model.
 *
 * Fixtures are real pre-index SQLite files. Tests observe behavior and durable data only; they do
 * not inspect implementation source or prescribe the SQL used by the migration.
 */
class LegacyCompatibilityUniqueMigrationRedTest {
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
    fun `old duplicate-free database installs unique index once and repeated reopen is ready`() = runBlocking {
        val fixture = fixture(
            "clean-upgrade",
            rows = listOf(oldSaga("saga-clean", "request-clean"))
        )
        val durableRowsBefore = sagaAndEffectDump(fixture.databasePath)
        val schemaBeforeMigration = schemaObjects(fixture.databasePath)
        assertFalse(hasUniqueRequestKeyIndex(fixture.databasePath))

        val first = fixture.migration().use { migration -> migration.startOrResume() }
        assertEquals(LegacyCompatibilityUniqueMigrationState.READY, first.state)
        assertTrue(first.runtimeReady)
        assertTrue(hasUniqueRequestKeyIndex(fixture.databasePath))
        assertEquals(durableRowsBefore, sagaAndEffectDump(fixture.databasePath))
        assertOnlyExpectedMigrationSchemaChanges(
            before = schemaBeforeMigration,
            after = schemaObjects(fixture.databasePath)
        )
        val schemaAfterFirstRun = schemaDump(fixture.databasePath)
        fixture.factory.openCompatibilitySagaStore().use { store ->
            assertNotNull(store.findBySagaId("saga-clean"))
            assertEquals(1, store.effectHistory("saga-clean").size)
        }
        assertEquals(
            schemaAfterFirstRun,
            schemaDump(fixture.databasePath),
            "Opening the canonical runtime store after READY must not add fallback DDL."
        )

        repeat(3) {
            val reopened = fixture.migration().use { migration -> migration.startOrResume() }
            assertEquals(LegacyCompatibilityUniqueMigrationState.READY, reopened.state)
            assertTrue(reopened.runtimeReady)
            assertEquals(schemaAfterFirstRun, schemaDump(fixture.databasePath))
            assertEquals(durableRowsBefore, sagaAndEffectDump(fixture.databasePath))
        }
    }

    @Test
    fun `every canonical public registration opening is a typed mutation-free gate before ready`() = runBlocking {
        listOf(false, true).forEach { blockedByDuplicates ->
            CanonicalRegistrationSurface.entries.forEach { surface ->
                val suffix = if (blockedByDuplicates) "blocked" else "preflight"
                val rows = if (blockedByDuplicates) {
                    listOf(
                        oldSaga("gate-$suffix-${surface.name}-a", "gate-duplicate-${surface.name}"),
                        oldSaga("gate-$suffix-${surface.name}-b", "gate-duplicate-${surface.name}")
                    )
                } else {
                    listOf(oldSaga("gate-$suffix-${surface.name}", "gate-clean-${surface.name}"))
                }
                val fixture = fixture("gate-$suffix-${surface.name}", rows)
                val expectedState = if (blockedByDuplicates) {
                    fixture.migration().use { migration ->
                        val blocked = migration.startOrResume()
                        assertEquals(
                            LegacyCompatibilityUniqueMigrationState.BLOCKED_DUPLICATES,
                            blocked.state
                        )
                        blocked.state
                    }
                } else {
                    LegacyCompatibilityUniqueMigrationState.STARTUP_PREFLIGHT
                }
                val databaseBefore = completeDatabaseDump(fixture.databasePath)

                val failure = runCatching { openCanonicalSurface(fixture.factory, surface) }
                    .exceptionOrNull()
                val typed = assertNotNull(
                    failure as? LegacyCompatibilityUniqueMigrationNotReadyException,
                    "$surface must fail with the typed migration gate while state is $expectedState."
                )
                assertEquals(expectedState, typed.migrationState)
                assertEquals(
                    databaseBefore,
                    completeDatabaseDump(fixture.databasePath),
                    "$surface must not bootstrap schema, advance migration, or mutate rows before READY."
                )
            }
        }

        CanonicalRegistrationSurface.entries.forEach { surface ->
            val fixture = fixture(
                "gate-failure-${surface.name}",
                rows = listOf(oldSaga("gate-failure-${surface.name}", "gate-failure-${surface.name}"))
            )
            val blocked = fixture.migration(
                FailBeforeCheckpoint(
                    LegacyCompatibilityUniqueMigrationEffectCheckpoint.INSTALL_UNIQUE_REQUEST_KEY_INDEX,
                    LegacyCompatibilityUniqueMigrationFailure.DDL_UNAVAILABLE
                )
            ).use { migration -> migration.startOrResume() }
            assertEquals(LegacyCompatibilityUniqueMigrationState.BLOCKED_MIGRATION_FAILURE, blocked.state)
            val databaseBefore = completeDatabaseDump(fixture.databasePath)

            val failure = runCatching { openCanonicalSurface(fixture.factory, surface) }
                .exceptionOrNull()
            val typed = assertNotNull(
                failure as? LegacyCompatibilityUniqueMigrationNotReadyException,
                "$surface must fail with the typed gate after a migration checkpoint failure."
            )
            assertEquals(LegacyCompatibilityUniqueMigrationState.BLOCKED_MIGRATION_FAILURE, typed.migrationState)
            assertEquals(databaseBefore, completeDatabaseDump(fixture.databasePath))
        }

        val migrationOnly = fixture(
            "migration-only-port",
            rows = listOf(oldSaga("migration-port-saga", "migration-port-request"))
        )
        val ready = migrationOnly.migration().use { migration -> migration.startOrResume() }
        assertEquals(
            LegacyCompatibilityUniqueMigrationState.READY,
            ready.state,
            "The explicit migration-only port remains available while canonical public ports are gated."
        )
    }

    @Test
    fun `start and resume never advance the persisted migration clock from a new process seed`() = runBlocking {
        val fixture = fixture(
            "no-implicit-clock",
            rows = listOf(oldSaga("clock-saga", "clock-request"))
        )
        val initial = fixture.migration(initialLogicalNowEpochSeconds = 100).use { migration ->
            migration.startOrResume()
        }
        assertEquals(LegacyCompatibilityUniqueMigrationState.READY, initial.state)
        assertEquals(100, initial.logicalNowEpochSeconds)
        assertEquals(0, initial.clockRevision)

        val resumed = fixture.migration(initialLogicalNowEpochSeconds = 9_999).use { migration ->
            migration.startOrResume()
        }
        assertEquals(100, resumed.logicalNowEpochSeconds)
        assertEquals(0, resumed.clockRevision)

        val restored = fixture.migration(initialLogicalNowEpochSeconds = 1).use { migration ->
            migration.currentSnapshot()
        }
        assertEquals(100, restored.logicalNowEpochSeconds)
        assertEquals(0, restored.clockRevision)
    }

    @Test
    fun `explicit correlated clock commands persist monotonically and alone expire recovery leases`() = runBlocking {
        val fixture = fixture(
            "explicit-clock",
            rows = listOf(oldSaga("clock-recovery-saga", "clock-recovery-request"))
        )
        val crashing = fixture.migration(
            CrashAfterCommittedCheckpoint(
                LegacyCompatibilityUniqueMigrationEffectCheckpoint.INSTALL_UNIQUE_REQUEST_KEY_INDEX,
                occurrence = 1
            )
        )
        assertTrue(
            runCatching { crashing.startOrResume() }.exceptionOrNull() is SimulatedMigrationCrash
        )
        crashing.close()

        val leaseAt100 = fixture.migration().use { migration ->
            val pending = migration.currentSnapshot()
            assertEquals(100, pending.logicalNowEpochSeconds)
            assertEquals(0, pending.clockRevision)
            val effect = assertNotNull(pending.requiredEffect)
            assertNotNull(
                migration.acquireRecoveryLease(
                    LegacyCompatibilityUniqueMigrationRecoveryLeaseRequest(
                        migrationId = pending.migrationId,
                        expectedEffectId = effect.effectId,
                        effectCheckpoint = effect.checkpoint,
                        checkpointRevision = effect.checkpointRevision,
                        holderId = "clock-holder-1",
                        expectedLeaseVersion = 0,
                        newLeaseVersion = 1,
                        fencingToken = effect.fencingToken + 10,
                        expiresAtLogicalEpochSeconds = 110
                    )
                )
            )
        }

        val commandedEpoch = longArrayOf(109)
        val clockSource: () -> Long = { commandedEpoch.single() }
        fixture.migration(initialLogicalNowEpochSeconds = 9_999).use { migration ->
            val resumed = migration.startOrResume()
            assertEquals(100, resumed.logicalNowEpochSeconds)
            assertEquals(0, resumed.clockRevision)
            assertEquals(leaseAt100, resumed.recoveryLease)

            val staleRevision = migration.advanceLogicalClock(
                expectedRevision = resumed.clockRevision + 1,
                newEpochSeconds = clockSource()
            )
            assertEquals(resumed, staleRevision)
            val rewind = migration.advanceLogicalClock(
                expectedRevision = resumed.clockRevision,
                newEpochSeconds = 99
            )
            assertEquals(resumed, rewind)

            val at109 = migration.advanceLogicalClock(
                expectedRevision = resumed.clockRevision,
                newEpochSeconds = clockSource()
            )
            assertEquals(109, at109.logicalNowEpochSeconds)
            assertEquals(1, at109.clockRevision)
            val effect = assertNotNull(at109.requiredEffect)
            assertNull(
                migration.acquireRecoveryLease(
                    LegacyCompatibilityUniqueMigrationRecoveryLeaseRequest(
                        migrationId = at109.migrationId,
                        expectedEffectId = effect.effectId,
                        effectCheckpoint = effect.checkpoint,
                        checkpointRevision = effect.checkpointRevision,
                        holderId = "clock-holder-too-early",
                        expectedLeaseVersion = leaseAt100.leaseVersion,
                        newLeaseVersion = leaseAt100.leaseVersion + 1,
                        fencingToken = leaseAt100.fencingToken + 1,
                        expiresAtLogicalEpochSeconds = 120
                    )
                ),
                "The lease remains live until an explicit clock command reaches its expiry."
            )
        }

        fixture.migration(initialLogicalNowEpochSeconds = 1).use { restored ->
            val persisted109 = restored.currentSnapshot()
            assertEquals(109, persisted109.logicalNowEpochSeconds)
            assertEquals(1, persisted109.clockRevision)
            assertEquals(leaseAt100, persisted109.recoveryLease)

            commandedEpoch[0] = 110
            val at110 = restored.advanceLogicalClock(
                expectedRevision = persisted109.clockRevision,
                newEpochSeconds = clockSource()
            )
            assertEquals(110, at110.logicalNowEpochSeconds)
            assertEquals(2, at110.clockRevision)
            val effect = assertNotNull(at110.requiredEffect)
            assertNotNull(
                restored.acquireRecoveryLease(
                    LegacyCompatibilityUniqueMigrationRecoveryLeaseRequest(
                        migrationId = at110.migrationId,
                        expectedEffectId = effect.effectId,
                        effectCheckpoint = effect.checkpoint,
                        checkpointRevision = effect.checkpointRevision,
                        holderId = "clock-holder-2",
                        expectedLeaseVersion = leaseAt100.leaseVersion,
                        newLeaseVersion = leaseAt100.leaseVersion + 1,
                        fencingToken = leaseAt100.fencingToken + 1,
                        expiresAtLogicalEpochSeconds = 120
                    )
                ),
                "Only the explicit durable advance to expiry permits a replacement lease."
            )
        }
        Unit
    }

    @Test
    fun `complete preflight observes duplicates and index read only before any checkpoint metadata`() {
        data class PreflightCase(
            val name: String,
            val rows: List<OldSagaRow>,
            val installIndexBeforeScan: Boolean,
            val expectedDuplicateGroups: Int
        )

        val cases = listOf(
            PreflightCase(
                name = "duplicates-no-index",
                rows = listOf(
                    oldSaga("preflight-duplicate-a", "preflight-duplicate-key"),
                    oldSaga("preflight-duplicate-b", "preflight-duplicate-key")
                ),
                installIndexBeforeScan = false,
                expectedDuplicateGroups = 1
            ),
            PreflightCase(
                name = "clean-index-present",
                rows = listOf(oldSaga("preflight-indexed", "preflight-indexed-key")),
                installIndexBeforeScan = true,
                expectedDuplicateGroups = 0
            )
        )

        cases.forEach { case ->
            val fixture = fixture(case.name, case.rows)
            if (case.installIndexBeforeScan) installOldUniqueRequestKeyIndex(fixture.databasePath)
            val databaseBefore = completeDatabaseDump(fixture.databasePath)
            val observations = mutableListOf<LegacyCompatibilityUniqueMigrationPreflightObservation>()
            val crash = runCatching {
                fixture.factory.openCompatibilityUniqueMigration(
                    migrationId = MIGRATION_ID,
                    schemaVersion = 1,
                    initialLogicalNowEpochSeconds = 100,
                    faultInjector = LegacyCompatibilityUniqueMigrationFaultInjector { _, _ -> null },
                    preflightProbe = LegacyCompatibilityUniqueMigrationReadOnlyPreflightProbe { observation ->
                        observations += observation
                        throw SimulatedReadOnlyPreflightCrash
                    }
                ).use { migration -> migration.startOrResume() }
            }.exceptionOrNull()

            assertTrue(crash === SimulatedReadOnlyPreflightCrash)
            val observation = observations.single()
            assertEquals(case.expectedDuplicateGroups, observation.duplicateGroupCount)
            assertEquals(case.installIndexBeforeScan, observation.uniqueRequestKeyIndexPresent)
            assertEquals(
                databaseBefore,
                completeDatabaseDump(fixture.databasePath),
                "A crash after the complete read-only observation must precede metadata and DDL."
            )
        }
    }

    @Test
    fun `old duplicates block without DDL mutation or disclosure and every runtime surface stays closed`() = runBlocking {
        val rawRequestKey = "raw-request-key-must-not-escape"
        val rawToken = "raw-token-must-not-escape"
        val fixture = fixture(
            "blocked-duplicates",
            rows = listOf(
                oldSaga("saga-duplicate-a", rawRequestKey, rawToken = rawToken),
                oldSaga("saga-duplicate-b", rawRequestKey, rawToken = rawToken)
            )
        )
        val databaseBefore = completeDatabaseDump(fixture.databasePath)

        val migration = fixture.migration()
        val blocked = migration.startOrResume()
        assertEquals(LegacyCompatibilityUniqueMigrationState.BLOCKED_DUPLICATES, blocked.state)
        assertFalse(blocked.runtimeReady)
        assertFalse(hasUniqueRequestKeyIndex(fixture.databasePath))
        assertEquals(databaseBefore, completeDatabaseDump(fixture.databasePath))

        val diagnostic = migration.diagnostic()
        assertEquals(1, diagnostic.duplicateGroupCount)
        assertEquals(2, diagnostic.duplicateRowCount)
        assertEquals(1, diagnostic.groupDigests.size)
        assertTrue(diagnostic.groupDigests.single().isNotBlank())
        val renderedDiagnostic = diagnostic.toString()
        listOf(rawRequestKey, rawToken, "saga-duplicate-a", "saga-duplicate-b").forEach { secret ->
            assertFalse(renderedDiagnostic.contains(secret), "Blocked diagnostics leaked $secret")
        }

        LegacyCompatibilityRuntimeSurface.entries.forEach { surface ->
            assertFalse(blocked.runtimeReadyFor(surface), "$surface must remain disabled before READY.")
        }
        assertTrue(
            runCatching { fixture.factory.openCompatibilitySagaStore().close() }
                .exceptionOrNull() is LegacyCompatibilityUniqueMigrationNotReadyException,
            "The saga store must fail closed with the typed migration blocker."
        )
        assertTrue(
            runCatching { fixture.worker().recoverDue() }
                .exceptionOrNull() is LegacyCompatibilityUniqueMigrationNotReadyException,
            "The recovery scheduler must not run while duplicate migration is blocked."
        )
        migration.close()
        fixture.migration().use { reopened ->
            val stillBlocked = reopened.startOrResume()
            assertEquals(
                LegacyCompatibilityUniqueMigrationState.BLOCKED_DUPLICATES,
                stillBlocked.state
            )
            assertFalse(stillBlocked.runtimeReady)
        }
        assertEquals(databaseBefore, completeDatabaseDump(fixture.databasePath))
    }

    @Test
    fun `operator inspection rejects stale foreign divergent nonquiescent and leased groups`() = runBlocking {
        val simpleKey = "request-simple"
        val divergentKey = "request-divergent"
        val activeKey = "request-active"
        val nonquiescentKey = "request-nonquiescent"
        val fixture = fixture(
            "operator-guards",
            rows = listOf(
                oldSaga("simple-a", simpleKey),
                oldSaga("simple-b", simpleKey),
                oldSaga("divergent-a", divergentKey),
                oldSaga(
                    "divergent-b",
                    divergentKey,
                    targetInstallationId = "different-target",
                    tokenFingerprint = "different-token-fingerprint"
                ),
                oldSaga("active-a", activeKey, state = OldSagaState.ACTIVE_LEASE),
                oldSaga("active-b", activeKey),
                oldSaga("working-a", nonquiescentKey, state = OldSagaState.WRITING),
                oldSaga("working-b", nonquiescentKey)
            )
        )
        val databaseBefore = completeDatabaseDump(fixture.databasePath)
        fixture.migration().use { migration ->
            val blocked = migration.startOrResume()
            assertEquals(LegacyCompatibilityUniqueMigrationState.BLOCKED_DUPLICATES, blocked.state)
            val groups = migration.inspectOperatorGroups()
            assertEquals(4, groups.size)
            val simple = groups.single { "simple-a" in it.sagaIds }
            val divergent = groups.single { "divergent-a" in it.sagaIds }
            val active = groups.single { "active-a" in it.sagaIds }
            val nonquiescent = groups.single { "working-a" in it.sagaIds }
            assertTrue(simple.operatorResolvable)
            assertFalse(divergent.operatorResolvable)
            assertFalse(active.operatorResolvable)
            assertFalse(nonquiescent.operatorResolvable)

            val rejected = listOf(
                resolution(simple, scanRevision = blocked.scanRevision - 1, canonicalSagaId = "simple-a"),
                resolution(simple, scanRevision = blocked.scanRevision, canonicalSagaId = "not-a-member"),
                resolution(divergent, blocked.scanRevision, "divergent-a"),
                resolution(active, blocked.scanRevision, "active-a"),
                resolution(nonquiescent, blocked.scanRevision, "working-a")
            )
            rejected.forEach { request ->
                val unchanged = migration.requestOperatorResolution(request)
                assertEquals(LegacyCompatibilityUniqueMigrationState.BLOCKED_DUPLICATES, unchanged.state)
                assertTrue(unchanged.archiveAudit.isEmpty())
                assertFalse(hasUniqueRequestKeyIndex(fixture.databasePath))
                assertEquals(databaseBefore, completeDatabaseDump(fixture.databasePath))
            }
        }
    }

    @Test
    fun `quiescent resolution survives crashes at archive delete rescan and index with immutable audit`() = runBlocking {
        val checkpoints = listOf(
            LegacyCompatibilityUniqueMigrationEffectCheckpoint.ARCHIVE_NONCANONICAL_ROWS_AND_EFFECTS to 1,
            LegacyCompatibilityUniqueMigrationEffectCheckpoint.DELETE_ARCHIVED_NONCANONICAL_ROWS to 1,
            LegacyCompatibilityUniqueMigrationEffectCheckpoint.SCAN_DUPLICATES to 2,
            LegacyCompatibilityUniqueMigrationEffectCheckpoint.INSTALL_UNIQUE_REQUEST_KEY_INDEX to 1
        )

        checkpoints.forEachIndexed { index, (checkpoint, occurrence) ->
            val fixture = fixture(
                "crash-$index",
                rows = listOf(
                    oldSaga("canonical-$index", "request-crash-$index", effectOrdinal = 1),
                    oldSaga("duplicate-b-$index", "request-crash-$index", effectOrdinal = 2),
                    oldSaga("duplicate-c-$index", "request-crash-$index", effectOrdinal = 3)
                )
            )
            val crash = CrashAfterCommittedCheckpoint(checkpoint, occurrence)
            val failing = fixture.migration(crash)
            val blocked = failing.startOrResume()
            val group = failing.inspectOperatorGroups().single()
            val failure = runCatching {
                failing.requestOperatorResolution(
                    resolution(group, blocked.scanRevision, "canonical-$index")
                )
            }.exceptionOrNull()
            assertTrue(failure is SimulatedMigrationCrash, "$checkpoint must expose the injected crash.")
            failing.close()

            val liveAfterCrash = liveSagaIds(fixture.databasePath)
            if (checkpoint == LegacyCompatibilityUniqueMigrationEffectCheckpoint.ARCHIVE_NONCANONICAL_ROWS_AND_EFFECTS) {
                assertEquals(3, liveAfterCrash.size, "Archive must commit before any live-row deletion.")
            }

            val auditAtReady = fixture.migration().use { restored ->
                val pending = restored.currentSnapshot()
                assertFalse(pending.runtimeReady)
                val durableAuditBeforeResume = restored.archiveAudit().single()
                assertNotNull(restored.archiveBundle(durableAuditBeforeResume.archiveId))
                val ready = restored.startOrResume()
                assertEquals(LegacyCompatibilityUniqueMigrationState.READY, ready.state)
                assertTrue(ready.runtimeReady)
                assertEquals(setOf("canonical-$index"), liveSagaIds(fixture.databasePath))
                assertTrue(hasUniqueRequestKeyIndex(fixture.databasePath))

                val audit = restored.archiveAudit().single()
                assertEquals(2, audit.archivedRowCount)
                assertEquals("canonical-$index", audit.canonicalSagaId)
                assertTrue(audit.archiveId.isNotBlank())
                assertTrue(audit.archiveDigest.isNotBlank())
                val bundle = assertNotNull(restored.archiveBundle(audit.archiveId))
                assertEquals(
                    setOf("duplicate-b-$index", "duplicate-c-$index"),
                    bundle.rows.map { it.sagaId }.toSet()
                )
                assertEquals(
                    setOf("duplicate-b-$index", "duplicate-c-$index"),
                    bundle.effects.map { it.sagaId }.toSet()
                )
                val schemaBeforeRuntimeOpen = schemaDump(fixture.databasePath)
                fixture.factory.openCompatibilitySagaStore().use { store ->
                    assertNotNull(store.findBySagaId("canonical-$index"))
                    assertNull(store.findBySagaId("duplicate-b-$index"))
                    assertEquals(1, store.effectHistory("canonical-$index").size)
                }
                assertEquals(
                    schemaBeforeRuntimeOpen,
                    schemaDump(fixture.databasePath),
                    "READY resolution must open the real saga store without extra DDL."
                )
                restored.archiveAudit()
            }

            val auditAfterReady = fixture.migration().use { repeated ->
                assertEquals(
                    LegacyCompatibilityUniqueMigrationState.READY,
                    repeated.startOrResume().state
                )
                repeated.archiveAudit()
            }
            assertEquals(auditAtReady, auditAfterReady, "Archive audit must be immutable across reopen.")
        }
    }

    @Test
    fun `two store instances fence migration recovery and stale acknowledgements are inert`() = runBlocking {
        val fixture = fixture(
            "recovery-cas",
            rows = listOf(
                oldSaga("recovery-a", "request-recovery", effectOrdinal = 1),
                oldSaga("recovery-b", "request-recovery", effectOrdinal = 2)
            )
        )
        val crashing = fixture.migration(
            CrashAfterCommittedCheckpoint(
                LegacyCompatibilityUniqueMigrationEffectCheckpoint.ARCHIVE_NONCANONICAL_ROWS_AND_EFFECTS,
                occurrence = 1
            )
        )
        val blocked = crashing.startOrResume()
        val group = crashing.inspectOperatorGroups().single()
        assertTrue(
            runCatching {
                crashing.requestOperatorResolution(resolution(group, blocked.scanRevision, "recovery-a"))
            }.exceptionOrNull() is SimulatedMigrationCrash
        )
        crashing.close()

        val first = fixture.migration()
        val second = fixture.migration()
        try {
            val pending = first.currentSnapshot()
            val effect = assertNotNull(pending.requiredEffect)
            val start = CompletableDeferred<Unit>()
            val contenders = listOf(first to "migration-worker-a", second to "migration-worker-b")
            val results = coroutineScope {
                contenders.mapIndexed { index, (migration, holder) ->
                    async(Dispatchers.IO) {
                        start.await()
                        migration.acquireRecoveryLease(
                            LegacyCompatibilityUniqueMigrationRecoveryLeaseRequest(
                                migrationId = pending.migrationId,
                                expectedEffectId = effect.effectId,
                                effectCheckpoint = effect.checkpoint,
                                checkpointRevision = effect.checkpointRevision,
                                holderId = holder,
                                expectedLeaseVersion = 0,
                                newLeaseVersion = 1,
                                fencingToken = effect.fencingToken + 10 + index,
                                expiresAtLogicalEpochSeconds = pending.logicalNowEpochSeconds + 10
                            )
                        )
                    }
                }.also { start.complete(Unit) }.awaitAll()
            }
            assertEquals(1, results.count { it != null })
            assertEquals(1, results.count { it == null })

            val winnerIndex = results.indexOfFirst { it != null }
            val winner = contenders[winnerIndex].first
            val lease = assertNotNull(results[winnerIndex])
            val emitted = assertNotNull(winner.requestRecovery(lease.asRecoveryRequest()))
            assertEquals(effect.effectId, emitted.effectId)
            assertNull(winner.requestRecovery(lease.asRecoveryRequest()), "One lease emits once.")

            val beforeStaleAck = winner.currentSnapshot()
            val staleAck = LegacyCompatibilityUniqueMigrationEffectAcknowledgement(
                effectId = effect.effectId,
                checkpoint = effect.checkpoint,
                checkpointRevision = effect.checkpointRevision,
                fencingToken = effect.fencingToken
            )
            assertEquals(beforeStaleAck, winner.acknowledgeEffect(staleAck))
            assertEquals(beforeStaleAck, winner.currentSnapshot())
        } finally {
            first.close()
            second.close()
        }
    }

    @Test
    fun `migration failure and rollback remain closed until exact repair re-preflights`() = runBlocking {
        val fixture = fixture(
            "fail-closed",
            rows = listOf(oldSaga("fail-closed-saga", "request-fail-closed"))
        )
        val rowsBefore = sagaAndEffectDump(fixture.databasePath)
        val failureInjector = FailBeforeCheckpoint(
            LegacyCompatibilityUniqueMigrationEffectCheckpoint.INSTALL_UNIQUE_REQUEST_KEY_INDEX,
            LegacyCompatibilityUniqueMigrationFailure.DDL_UNAVAILABLE
        )
        val blocked = fixture.migration(failureInjector).use { migration ->
            val snapshot = migration.startOrResume()
            assertEquals(LegacyCompatibilityUniqueMigrationState.BLOCKED_MIGRATION_FAILURE, snapshot.state)
            assertFalse(snapshot.runtimeReady)
            assertFalse(hasUniqueRequestKeyIndex(fixture.databasePath))
            assertEquals(rowsBefore, sagaAndEffectDump(fixture.databasePath))
            val staleRepair = migration.confirmExternalRepair(
                scanRevision = snapshot.scanRevision - 1,
                repairEvidenceDigest = "sha256:stale-repair"
            )
            assertEquals(snapshot, staleRepair)
            snapshot
        }

        fixture.migration().use { repaired ->
            val repreflight = repaired.confirmExternalRepair(
                scanRevision = blocked.scanRevision,
                repairEvidenceDigest = "sha256:operator-repair"
            )
            assertNotEquals(LegacyCompatibilityUniqueMigrationState.READY, repreflight.state)
            assertFalse(repreflight.runtimeReady)
            val ready = repaired.startOrResume()
            assertEquals(LegacyCompatibilityUniqueMigrationState.READY, ready.state)
            assertTrue(hasUniqueRequestKeyIndex(fixture.databasePath))
            assertEquals(rowsBefore, sagaAndEffectDump(fixture.databasePath))
        }
    }

    private fun resolution(
        group: LegacyCompatibilityUniqueMigrationDuplicateGroup,
        scanRevision: Long,
        canonicalSagaId: String
    ) = LegacyCompatibilityUniqueMigrationOperatorResolutionRequest(
        migrationId = MIGRATION_ID,
        scanRevision = scanRevision,
        groupDigest = group.groupDigest,
        canonicalSagaId = canonicalSagaId,
        operatorResolutionId = "operator-resolution-$canonicalSagaId"
    )

    private fun fixture(
        name: String,
        rows: List<OldSagaRow>
    ): MigrationFixture {
        val root = Files.createTempDirectory("wakeve-unique-migration-$name-")
        temporaryRoots.add(root)
        val databasePath = root.resolve("registration.sqlite")
        createOldSchema(databasePath)
        rows.forEach { row -> insertOldSaga(databasePath, row) }
        seedOldGenerationRows(databasePath, rows)
        assertOldGenerationSchema(databasePath, rows)
        val configuration = DeviceRegistrationStoreConfiguration.resolve(
            systemProperties = mapOf(
                "wakeve.notification.device-registration.db.path" to databasePath.toString(),
                "wakeve.notification.device-registration.legacy-identity-hmac-key" to "h".repeat(32),
                "wakeve.notification.device-registration.token-encryption-key" to "e".repeat(32)
            ),
            environment = emptyMap()
        ).getOrThrow()
        val factory = SqliteBackendDeviceRegistrationStoreFactory(configuration)
        val database = WakeveDb(JvmDatabaseFactory(root.resolve("legacy.sqlite").toString()).createDriver())
        return MigrationFixture(
            factory = factory,
            databasePath = databasePath,
            notificationService = NotificationService(
                database = database,
                preferencesRepository = NotificationPreferencesRepository(database),
                fcmSender = NoConfiguredFCMSender,
                apnsSender = NoConfiguredAPNsSender
            )
        )
    }

    private data class MigrationFixture(
        val factory: SqliteBackendDeviceRegistrationStoreFactory,
        val databasePath: Path,
        val notificationService: NotificationService
    ) {
        fun migration(
            faultInjector: LegacyCompatibilityUniqueMigrationFaultInjector =
                LegacyCompatibilityUniqueMigrationFaultInjector { _, _ -> null },
            initialLogicalNowEpochSeconds: Long = 100
        ): LegacyCompatibilityUniqueMigrationRuntime =
            factory.openCompatibilityUniqueMigration(
                migrationId = MIGRATION_ID,
                schemaVersion = 1,
                initialLogicalNowEpochSeconds = initialLogicalNowEpochSeconds,
                faultInjector = faultInjector
            )

        fun worker(): LegacyNotificationRegistrationCompatibilityWorker =
            LegacyNotificationRegistrationCompatibilityWorker(
                notificationService = notificationService,
                registrationStoreFactory = factory
            )
    }

    private class CrashAfterCommittedCheckpoint(
        private val target: LegacyCompatibilityUniqueMigrationEffectCheckpoint,
        private val occurrence: Int
    ) : LegacyCompatibilityUniqueMigrationFaultInjector {
        private var committedOccurrences = 0

        override fun evaluate(
            checkpoint: LegacyCompatibilityUniqueMigrationEffectCheckpoint,
            phase: LegacyCompatibilityUniqueMigrationCheckpointPhase
        ): LegacyCompatibilityUniqueMigrationFailure? {
            if (
                checkpoint == target &&
                phase == LegacyCompatibilityUniqueMigrationCheckpointPhase.AFTER_COMMIT
            ) {
                committedOccurrences += 1
                if (committedOccurrences == occurrence) throw SimulatedMigrationCrash(checkpoint)
            }
            return null
        }
    }

    private class FailBeforeCheckpoint(
        private val target: LegacyCompatibilityUniqueMigrationEffectCheckpoint,
        private val failure: LegacyCompatibilityUniqueMigrationFailure
    ) : LegacyCompatibilityUniqueMigrationFaultInjector {
        override fun evaluate(
            checkpoint: LegacyCompatibilityUniqueMigrationEffectCheckpoint,
            phase: LegacyCompatibilityUniqueMigrationCheckpointPhase
        ): LegacyCompatibilityUniqueMigrationFailure? =
            failure.takeIf {
                checkpoint == target &&
                    phase == LegacyCompatibilityUniqueMigrationCheckpointPhase.BEFORE_EFFECT
            }
    }

    private class SimulatedMigrationCrash(
        checkpoint: LegacyCompatibilityUniqueMigrationEffectCheckpoint
    ) : RuntimeException("simulated process crash after $checkpoint")

    private data object SimulatedReadOnlyPreflightCrash : RuntimeException(
        "simulated crash after complete read-only preflight"
    )

    private enum class OldSagaState { QUIESCENT, WRITING, ACTIVE_LEASE }

    private enum class CanonicalRegistrationSurface { STORE, PROVIDER_TOKEN_PORT, SAGA_STORE }

    private fun openCanonicalSurface(
        factory: SqliteBackendDeviceRegistrationStoreFactory,
        surface: CanonicalRegistrationSurface
    ) {
        when (surface) {
            CanonicalRegistrationSurface.STORE -> factory.open().close()
            CanonicalRegistrationSurface.PROVIDER_TOKEN_PORT -> factory.openProviderTokenPort().close()
            CanonicalRegistrationSurface.SAGA_STORE -> factory.openCompatibilitySagaStore().close()
        }
    }

    private data class OldSagaRow(
        val sagaId: String,
        val requestKey: String,
        val authenticatedUserId: String,
        val targetInstallationId: String,
        val legacyRegistrationId: String,
        val tokenFingerprint: String,
        val rawToken: String,
        val state: OldSagaState,
        val effectOrdinal: Int
    )

    private data class OldGenerationRow(
        val subjectKey: String,
        val targetKey: String,
        val desiredKey: String,
        val compatibilityGeneration: Long
    )

    private fun oldSaga(
        sagaId: String,
        requestKey: String,
        authenticatedUserId: String = "owner",
        targetInstallationId: String = "legacy-installation",
        legacyRegistrationId: String = "legacy-registration",
        tokenFingerprint: String = "token-fingerprint",
        rawToken: String = "encrypted-token-placeholder",
        state: OldSagaState = OldSagaState.QUIESCENT,
        effectOrdinal: Int = 1
    ) = OldSagaRow(
        sagaId = sagaId,
        requestKey = requestKey,
        authenticatedUserId = authenticatedUserId,
        targetInstallationId = targetInstallationId,
        legacyRegistrationId = legacyRegistrationId,
        tokenFingerprint = tokenFingerprint,
        rawToken = rawToken,
        state = state,
        effectOrdinal = effectOrdinal
    )

    private fun createOldSchema(databasePath: Path) {
        DriverManager.getConnection("jdbc:sqlite:$databasePath").use { connection ->
            connection.createStatement().use { statement ->
                statement.execute(OLD_SAGA_TABLE_DDL)
                statement.execute(OLD_EFFECT_TABLE_DDL)
                statement.execute(OLD_GENERATION_TABLE_DDL)
            }
        }
    }

    private fun installOldUniqueRequestKeyIndex(databasePath: Path) {
        DriverManager.getConnection("jdbc:sqlite:$databasePath").use { connection ->
            connection.createStatement().use { statement ->
                statement.execute(
                    "CREATE UNIQUE INDEX old_fixture_request_key_unique " +
                        "ON legacy_notification_compatibility_saga(request_key)"
                )
            }
        }
    }

    private fun insertOldSaga(databasePath: Path, row: OldSagaRow) {
        DriverManager.getConnection("jdbc:sqlite:$databasePath").use { connection ->
            connection.autoCommit = false
            val effectId = "effect-${row.sagaId}-${row.effectOrdinal}"
            val working = row.state != OldSagaState.QUIESCENT
            val activeLease = row.state == OldSagaState.ACTIVE_LEASE
            val values = listOf<Any?>(
                row.sagaId,
                row.requestKey,
                "REGISTER",
                "N_MINUS_1",
                row.authenticatedUserId,
                "IOS",
                "legacy-primary-key-fingerprint",
                row.targetInstallationId,
                row.legacyRegistrationId,
                row.targetInstallationId,
                null,
                row.tokenFingerprint,
                1L,
                3,
                "PRODUCTION",
                "com.guyghost.wakeve",
                if (working) "WRITING_LEGACY" else "CONVERGED",
                if (working) "PENDING" else "CONVERGED",
                if (working) "RECONCILIATION_ACCEPTED" else "CONVERGED_SUCCESS",
                if (working) "PENDING" else "APPLIED",
                if (working) "PENDING" else "APPLIED",
                "LEGACY_DETERMINISTIC_INSTALLATION_ONLY",
                0,
                0,
                null,
                1L,
                100L,
                0L,
                effectId.takeIf { working },
                "WRITE_LEGACY_STORE".takeIf { working },
                1L.takeIf { working },
                1L.takeIf { working },
                null,
                "lease-${row.sagaId}".takeIf { activeLease },
                "holder-${row.sagaId}".takeIf { activeLease },
                1L.takeIf { activeLease },
                2L.takeIf { activeLease },
                110L.takeIf { activeLease },
                0.takeIf { activeLease },
                null,
                row.rawToken.toByteArray()
            )
            connection.prepareStatement(
                "INSERT INTO legacy_notification_compatibility_saga " +
                    "(${OLD_SAGA_COLUMNS.joinToString()}) VALUES " +
                    "(${OLD_SAGA_COLUMNS.joinToString { "?" }})"
            ).use { statement ->
                values.forEachIndexed { index, value -> statement.setObject(index + 1, value) }
                statement.executeUpdate()
            }
            connection.prepareStatement(
                """
                INSERT INTO legacy_notification_compatibility_effect_history(
                    saga_id, effect_id, effect_checkpoint, checkpoint_revision, fencing_token
                ) VALUES (?, ?, ?, ?, ?)
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, row.sagaId)
                statement.setString(2, effectId)
                statement.setString(3, "WRITE_LEGACY_STORE")
                statement.setLong(4, 1)
                statement.setLong(5, row.effectOrdinal.toLong())
                statement.executeUpdate()
            }
            connection.commit()
        }
    }

    private fun seedOldGenerationRows(databasePath: Path, rows: List<OldSagaRow>) {
        DriverManager.getConnection("jdbc:sqlite:$databasePath").use { connection ->
            connection.prepareStatement(
                """
                INSERT INTO legacy_notification_compatibility_generation(
                    subject_key, target_key, desired_key, compatibility_generation
                ) VALUES (?, ?, ?, ?)
                """.trimIndent()
            ).use { statement ->
                expectedOldGenerationRows(rows).forEach { row ->
                    statement.setString(1, row.subjectKey)
                    statement.setString(2, row.targetKey)
                    statement.setString(3, row.desiredKey)
                    statement.setLong(4, row.compatibilityGeneration)
                    statement.addBatch()
                }
                statement.executeBatch()
            }
        }
    }

    private fun expectedOldGenerationRows(rows: List<OldSagaRow>): List<OldGenerationRow> =
        rows.groupBy { row -> row.authenticatedUserId to row.legacyRegistrationId }
            .map { (_, candidates) -> candidates.maxBy(OldSagaRow::effectOrdinal) }
            .map { row ->
                OldGenerationRow(
                    subjectKey = opaqueCompatibilityDigest(listOf("subject", row.authenticatedUserId)),
                    targetKey = opaqueCompatibilityDigest(listOf("target", row.legacyRegistrationId)),
                    desiredKey = opaqueCompatibilityDigest(
                        listOf("desired", LegacyCompatibilityOperation.REGISTER.name, row.tokenFingerprint)
                    ),
                    compatibilityGeneration = 1
                )
            }
            .sortedWith(compareBy(OldGenerationRow::subjectKey, OldGenerationRow::targetKey))

    private fun hasUniqueRequestKeyIndex(databasePath: Path): Boolean =
        DriverManager.getConnection("jdbc:sqlite:$databasePath").use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery(
                    "PRAGMA index_list('legacy_notification_compatibility_saga')"
                ).use { indexes ->
                    var found = false
                    while (indexes.next()) {
                        if (indexes.getInt("unique") != 1) continue
                        val escaped = indexes.getString("name").replace("'", "''")
                        connection.createStatement().use { columnsStatement ->
                            columnsStatement.executeQuery("PRAGMA index_info('$escaped')").use { columns ->
                                val names = buildList {
                                    while (columns.next()) add(columns.getString("name"))
                                }
                                if (names == listOf("request_key")) found = true
                            }
                        }
                    }
                    found
                }
            }
        }

    private fun assertOldGenerationSchema(databasePath: Path, oldSagaRows: List<OldSagaRow>) {
        DriverManager.getConnection("jdbc:sqlite:$databasePath").use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery(
                    "PRAGMA table_info('legacy_notification_compatibility_generation')"
                ).use { columns ->
                    val actual = buildList {
                        while (columns.next()) {
                            add(
                                Triple(
                                    columns.getString("name"),
                                    columns.getInt("notnull"),
                                    columns.getInt("pk")
                                )
                            )
                        }
                    }
                    assertEquals(
                        listOf(
                            Triple("subject_key", 1, 1),
                            Triple("target_key", 1, 2),
                            Triple("desired_key", 1, 0),
                            Triple("compatibility_generation", 1, 0)
                        ),
                        actual
                    )
                }
            }
            connection.createStatement().use { statement ->
                statement.executeQuery(
                    "PRAGMA index_list('legacy_notification_compatibility_generation')"
                ).use { indexes ->
                    assertTrue(indexes.next())
                    assertEquals(1, indexes.getInt("unique"))
                    assertEquals("pk", indexes.getString("origin"))
                    val escaped = indexes.getString("name").replace("'", "''")
                    connection.createStatement().use { indexStatement ->
                        indexStatement.executeQuery("PRAGMA index_info('$escaped')").use { indexed ->
                            val indexedColumns = buildList {
                                while (indexed.next()) add(indexed.getString("name"))
                            }
                            assertEquals(listOf("subject_key", "target_key"), indexedColumns)
                        }
                    }
                    assertFalse(indexes.next(), "The N-1 generation table has only its composite-PK index.")
                }
            }
            connection.createStatement().use { statement ->
                statement.executeQuery(
                    """
                    SELECT subject_key, target_key, desired_key, compatibility_generation
                    FROM legacy_notification_compatibility_generation
                    ORDER BY subject_key, target_key
                    """.trimIndent()
                ).use { rows ->
                    val actual = buildList {
                        while (rows.next()) {
                            add(
                                OldGenerationRow(
                                    subjectKey = rows.getString("subject_key"),
                                    targetKey = rows.getString("target_key"),
                                    desiredKey = rows.getString("desired_key"),
                                    compatibilityGeneration = rows.getLong("compatibility_generation")
                                )
                            )
                        }
                    }
                    assertEquals(expectedOldGenerationRows(oldSagaRows), actual)
                    assertTrue(actual.isNotEmpty(), "The old N-1 generation allocator must be seeded.")
                }
            }
            val generationDdl = connection.createStatement().use { statement ->
                statement.executeQuery(
                    """
                    SELECT sql FROM sqlite_schema
                    WHERE type = 'table'
                      AND name = 'legacy_notification_compatibility_generation'
                    """.trimIndent()
                ).use { rows ->
                    assertTrue(rows.next())
                    rows.getString(1)
                }
            }
            assertTrue(
                Regex("CHECK\\s*\\(\\s*compatibility_generation\\s*>\\s*0\\s*\\)", RegexOption.IGNORE_CASE)
                    .containsMatchIn(generationDdl)
            )
        }
    }

    private fun liveSagaIds(databasePath: Path): Set<String> =
        DriverManager.getConnection("jdbc:sqlite:$databasePath").use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery(
                    "SELECT saga_id FROM legacy_notification_compatibility_saga ORDER BY saga_id"
                ).use { rows -> buildSet { while (rows.next()) add(rows.getString(1)) } }
            }
        }

    private fun completeDatabaseDump(databasePath: Path): List<String> =
        schemaDump(databasePath) + sagaAndEffectDump(databasePath)

    private fun schemaDump(databasePath: Path): List<String> =
        schemaObjects(databasePath).map { schema ->
            listOf(schema.type, schema.name, schema.tableName, schema.sql).joinToString("|")
        }

    private data class SchemaObject(
        val type: String,
        val name: String,
        val tableName: String,
        val sql: String
    )

    private fun schemaObjects(databasePath: Path): List<SchemaObject> =
        DriverManager.getConnection("jdbc:sqlite:$databasePath").use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery(
                    """
                    SELECT type, name, tbl_name, COALESCE(sql, '') AS sql
                    FROM sqlite_schema
                    WHERE name NOT LIKE 'sqlite_%'
                    ORDER BY type, name
                    """.trimIndent()
                ).use { rows ->
                    buildList {
                        while (rows.next()) {
                            add(
                                SchemaObject(
                                    type = rows.getString("type"),
                                    name = rows.getString("name"),
                                    tableName = rows.getString("tbl_name"),
                                    sql = rows.getString("sql")
                                )
                            )
                        }
                    }
                }
            }
        }

    private fun assertOnlyExpectedMigrationSchemaChanges(
        before: List<SchemaObject>,
        after: List<SchemaObject>
    ) {
        val beforeByIdentity = before.associateBy { schema -> schema.type to schema.name }
        val afterByIdentity = after.associateBy { schema -> schema.type to schema.name }
        beforeByIdentity.forEach { (identity, original) ->
            assertEquals(original, afterByIdentity[identity], "Migration rewrote existing schema object $identity")
        }
        val added = after.filter { schema -> (schema.type to schema.name) !in beforeByIdentity }
        assertTrue(added.isNotEmpty(), "Migration must at least add UNIQUE(request_key).")
        added.forEach { schema ->
            when {
                schema.type == "index" &&
                    schema.tableName == "legacy_notification_compatibility_saga" -> {
                    assertTrue(
                        Regex("\\(\\s*request_key\\s*\\)", RegexOption.IGNORE_CASE)
                            .containsMatchIn(schema.sql),
                        "The only new live-saga index must own request_key uniqueness."
                    )
                }

                schema.type == "table" -> assertTrue(
                    migrationMetadataColumns(schema.sql),
                    "Unexpected non-migration table ${schema.name} was added."
                )

                schema.type == "index" -> assertTrue(
                    schema.tableName in added.filter { it.type == "table" }.map { it.name },
                    "Unexpected index ${schema.name} targets pre-existing table ${schema.tableName}."
                )

                else -> assertTrue(false, "Unexpected migration schema object: $schema")
            }
        }
    }

    private fun migrationMetadataColumns(createSql: String): Boolean {
        val normalized = createSql.lowercase()
        return "migration_id" in normalized || "archive_id" in normalized
    }

    private fun sagaAndEffectDump(databasePath: Path): List<String> =
        DriverManager.getConnection("jdbc:sqlite:$databasePath").use { connection ->
            queryDump(
                connection,
                "SELECT * FROM legacy_notification_compatibility_saga ORDER BY saga_id"
            ) + queryDump(
                connection,
                """
                SELECT saga_id, effect_id, effect_checkpoint, checkpoint_revision, fencing_token
                FROM legacy_notification_compatibility_effect_history
                ORDER BY saga_id, sequence_id
                """.trimIndent()
            ) + queryDump(
                connection,
                """
                SELECT subject_key, target_key, desired_key, compatibility_generation
                FROM legacy_notification_compatibility_generation
                ORDER BY subject_key, target_key
                """.trimIndent()
            )
        }

    private fun queryDump(connection: Connection, sql: String): List<String> =
        connection.createStatement().use { statement ->
            statement.executeQuery(sql).use { rows ->
                val columns = rows.metaData.columnCount
                buildList {
                    while (rows.next()) {
                        add(
                            (1..columns).joinToString("|") { column ->
                                when (val value = rows.getObject(column)) {
                                    is ByteArray -> Base64.getEncoder().encodeToString(value)
                                    null -> "<null>"
                                    else -> value.toString()
                                }
                            }
                        )
                    }
                }
            }
        }

    private companion object {
        const val MIGRATION_ID = "legacy-compatibility-request-key-unique-v1"

        val OLD_SAGA_COLUMNS = listOf(
            "saga_id", "request_key", "operation", "client_generation",
            "authenticated_user_id", "platform", "legacy_primary_key_fingerprint",
            "legacy_installation_id", "legacy_registration_id", "target_installation_id",
            "target_registration_id", "token_fingerprint", "compatibility_generation",
            "max_attempts_per_store", "scope_environment", "scope_topic", "state",
            "reconciliation_status", "response_disposition", "legacy_write_status",
            "v2_write_status", "v2_target_kind", "legacy_attempt", "v2_attempt",
            "next_retry_at_epoch_seconds", "checkpoint_revision", "logical_now_epoch_seconds",
            "clock_revision", "required_effect_id", "required_effect_checkpoint",
            "required_effect_revision", "required_effect_fencing", "resume_checkpoint",
            "lease_id", "lease_holder_id", "lease_version", "lease_fencing_token",
            "lease_expires_at_epoch_seconds", "lease_effect_emitted", "last_failure",
            "token_ciphertext"
        )

        val OLD_SAGA_TABLE_DDL =
            """
            CREATE TABLE legacy_notification_compatibility_saga (
                saga_id TEXT PRIMARY KEY NOT NULL,
                request_key TEXT NOT NULL,
                operation TEXT NOT NULL,
                client_generation TEXT NOT NULL,
                authenticated_user_id TEXT NOT NULL,
                platform TEXT NOT NULL,
                legacy_primary_key_fingerprint TEXT,
                legacy_installation_id TEXT,
                legacy_registration_id TEXT,
                target_installation_id TEXT NOT NULL,
                target_registration_id TEXT,
                token_fingerprint TEXT,
                compatibility_generation INTEGER NOT NULL,
                max_attempts_per_store INTEGER NOT NULL,
                scope_environment TEXT NOT NULL,
                scope_topic TEXT NOT NULL,
                state TEXT NOT NULL,
                reconciliation_status TEXT NOT NULL,
                response_disposition TEXT NOT NULL,
                legacy_write_status TEXT NOT NULL,
                v2_write_status TEXT NOT NULL,
                v2_target_kind TEXT NOT NULL,
                legacy_attempt INTEGER NOT NULL,
                v2_attempt INTEGER NOT NULL,
                next_retry_at_epoch_seconds INTEGER,
                checkpoint_revision INTEGER NOT NULL,
                logical_now_epoch_seconds INTEGER NOT NULL,
                clock_revision INTEGER NOT NULL,
                required_effect_id TEXT,
                required_effect_checkpoint TEXT,
                required_effect_revision INTEGER,
                required_effect_fencing INTEGER,
                resume_checkpoint TEXT,
                lease_id TEXT,
                lease_holder_id TEXT,
                lease_version INTEGER,
                lease_fencing_token INTEGER,
                lease_expires_at_epoch_seconds INTEGER,
                lease_effect_emitted INTEGER,
                last_failure TEXT,
                token_ciphertext BLOB
            )
            """.trimIndent()

        val OLD_EFFECT_TABLE_DDL =
            """
            CREATE TABLE legacy_notification_compatibility_effect_history (
                sequence_id INTEGER PRIMARY KEY AUTOINCREMENT,
                saga_id TEXT NOT NULL,
                effect_id TEXT NOT NULL,
                effect_checkpoint TEXT NOT NULL,
                checkpoint_revision INTEGER NOT NULL,
                fencing_token INTEGER NOT NULL,
                UNIQUE(saga_id, effect_id, fencing_token),
                FOREIGN KEY(saga_id)
                    REFERENCES legacy_notification_compatibility_saga(saga_id)
                    ON DELETE RESTRICT
            )
            """.trimIndent()

        val OLD_GENERATION_TABLE_DDL =
            """
            CREATE TABLE legacy_notification_compatibility_generation (
                subject_key TEXT NOT NULL,
                target_key TEXT NOT NULL,
                desired_key TEXT NOT NULL,
                compatibility_generation INTEGER NOT NULL CHECK(compatibility_generation > 0),
                PRIMARY KEY(subject_key, target_key)
            )
            """.trimIndent()
    }
}
