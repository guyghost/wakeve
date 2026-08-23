package com.guyghost.wakeve.notification

import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.util.concurrent.TimeUnit
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BackendNotificationPartialRuntimeSchemaMigrationRedTest {
    @Test
    fun `ready-looking partial runtime schema receives every additive column and preserves rows idempotently`() =
        runBlocking {
            BackendNotificationDurabilityTestFixture().use { fixture ->
                val registration = fixture.register("partial-runtime-schema")
                val key = BackendNotificationIngestionService(
                    fixture.deliveryFactory,
                    BackendNotificationIngestionFaultInjector { },
                    BackendNotificationIngestionCommittedPort { }
                ).ingest(
                    BackendNotificationIngestionCommand(
                        domainEventId = "partial-runtime-schema-event",
                        effectType = "DATE_CONFIRMED",
                        schemaVersion = 1,
                        logicalNotificationId = "partial-runtime-schema-logical",
                        recipients = listOf(
                            BackendNotificationRecipientIntent(
                                participantId = "partial-runtime-schema-participant",
                                channel = "push",
                                provider = "apns",
                                registrationIds = listOf(registration.registrationId),
                                expiresAtEpochSeconds = 1_000
                            )
                        )
                    )
                ).deliveryKeys.single()

                DriverManager.getConnection("jdbc:sqlite:${fixture.databasePath}").use { connection ->
                    connection.prepareStatement(
                        """
                        INSERT INTO notification_delivery_provider_checkpoint(
                            delivery_key, effect_id, checkpoint_revision, outcome, reason,
                            http_status, invalidation_reason, accepted_at, next_attempt_at,
                            next_attempt, provider_request_id, lease_holder, lease_version,
                            lease_fence, effect_requested
                        ) VALUES (?, 'partial-checkpoint-effect', 1, 'ACCEPTED', 'HTTP_200',
                                  200, NULL, 123, NULL, 0, 'safe-provider-id', NULL, NULL, NULL, 0)
                        """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, key.value)
                        statement.executeUpdate()
                    }
                    connection.createStatement().use { statement ->
                        statement.execute(
                            "UPDATE notification_provider_credential_circuit " +
                                "SET blocked_credential_version = 'sentinel-version', " +
                                "blocked_credential_fingerprint = 'sentinel-fingerprint' WHERE singleton = 1"
                        )
                        // Keep every column used by the old readiness shortcut, while removing other
                        // additive columns from two independent runtime tables.
                        statement.execute("ALTER TABLE notification_delivery DROP COLUMN clock_revision")
                        statement.execute("ALTER TABLE notification_delivery DROP COLUMN provider_checkpoint_count")
                        statement.execute("ALTER TABLE notification_delivery DROP COLUMN credential_fingerprint")
                        statement.execute(
                            "ALTER TABLE notification_delivery_provider_checkpoint DROP COLUMN invalidation_reason"
                        )
                    }
                    assertEquals(1, connection.rowCount("notification_delivery"))
                    assertEquals(1, connection.rowCount("notification_delivery_provider_checkpoint"))
                }

                val gate = fixture.root.resolve("partial-runtime-schema-gate")
                Files.createDirectories(gate)
                val processes = (0..1).map { index ->
                    startRuntimeOpenProcess(
                        fixture.databasePath,
                        gate.resolve("ready-$index"),
                        gate.resolve("go"),
                        gate.resolve("result-$index")
                    )
                }
                try {
                    awaitFiles(gate.resolve("ready-0"), gate.resolve("ready-1"))
                    gate.resolve("go").writeText("go")
                    processes.forEach(::awaitSuccessfulChild)
                    val outcomes = (0..1).map { gate.resolve("result-$it").readText().trim() }
                    assertEquals(listOf("READY", "READY"), outcomes.sorted(), outcomes.toString())
                } finally {
                    processes.forEach(::terminateChild)
                }

                val schemaAfterFirstOpen = DriverManager.getConnection("jdbc:sqlite:${fixture.databasePath}").use { connection ->
                    EXPECTED_ADDITIVE_COLUMNS.forEach { (table, expected) ->
                        val actual = connection.columns(table)
                        assertTrue(expected.all(actual::contains), "$table missing ${expected - actual}; actual=$actual")
                    }
                    connection.prepareStatement(
                        "SELECT device_registration_id FROM notification_delivery WHERE delivery_key = ?"
                    ).use { statement ->
                        statement.setString(1, key.value)
                        statement.executeQuery().use { rows ->
                            assertTrue(rows.next())
                            assertEquals(registration.registrationId, rows.getString(1))
                        }
                    }
                    connection.prepareStatement(
                        "SELECT effect_id FROM notification_delivery_provider_checkpoint WHERE delivery_key = ?"
                    ).use { statement ->
                        statement.setString(1, key.value)
                        statement.executeQuery().use { rows ->
                            assertTrue(rows.next())
                            assertEquals("partial-checkpoint-effect", rows.getString(1))
                        }
                    }
                    connection.createStatement().use { statement ->
                        statement.executeQuery(
                            "SELECT blocked_credential_version, blocked_credential_fingerprint " +
                                "FROM notification_provider_credential_circuit WHERE singleton = 1"
                        ).use { rows ->
                            assertTrue(rows.next())
                            assertEquals("sentinel-version", rows.getString(1))
                            assertEquals("sentinel-fingerprint", rows.getString(2))
                        }
                    }
                    EXPECTED_ADDITIVE_COLUMNS.mapValues { (table, _) -> connection.columns(table) }
                }

                fixture.deliveryFactory.openDeliveryRuntime().use { }
                DriverManager.getConnection("jdbc:sqlite:${fixture.databasePath}").use { connection ->
                    assertEquals(
                        schemaAfterFirstOpen,
                        EXPECTED_ADDITIVE_COLUMNS.mapValues { (table, _) -> connection.columns(table) },
                        "reopening an already migrated runtime schema must be idempotent"
                    )
                    assertEquals(1, connection.rowCount("notification_delivery"))
                    assertEquals(1, connection.rowCount("notification_delivery_provider_checkpoint"))
                }
            }
        }

    private fun Connection.columns(table: String): Set<String> = createStatement().use { statement ->
        statement.executeQuery("PRAGMA table_info($table)").use { rows ->
            buildSet { while (rows.next()) add(rows.getString("name")) }
        }
    }

    private fun Connection.rowCount(table: String): Int = createStatement().use { statement ->
        statement.executeQuery("SELECT COUNT(*) FROM $table").use { rows ->
            rows.next()
            rows.getInt(1)
        }
    }

    private fun startRuntimeOpenProcess(database: Path, ready: Path, go: Path, result: Path): Process =
        ProcessBuilder(
            Path.of(System.getProperty("java.home"), "bin", "java").toString(),
            "-cp",
            System.getProperty("java.class.path"),
            BackendNotificationPartialRuntimeSchemaMigrationProcessMain::class.java.name,
            database.toString(),
            ready.toString(),
            go.toString(),
            result.toString()
        ).redirectErrorStream(true).start()

    private fun awaitFiles(vararg files: Path) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (files.any { !it.exists() } && System.nanoTime() < deadline) Thread.sleep(10)
        assertTrue(files.all { it.exists() }, "partial-schema children did not reach the start gate")
    }

    private fun awaitSuccessfulChild(process: Process) {
        val finished = process.waitFor(15, TimeUnit.SECONDS)
        if (!finished) process.destroyForcibly()
        assertTrue(finished, "partial-schema child timed out")
        assertEquals(0, process.exitValue(), process.inputStream.bufferedReader().readText())
    }

    private fun terminateChild(process: Process) {
        if (process.isAlive) {
            process.destroyForcibly()
            process.waitFor(2, TimeUnit.SECONDS)
        }
    }

    private companion object {
        val EXPECTED_ADDITIVE_COLUMNS = mapOf(
            "domain_event_ingestion" to setOf("identity_version"),
            "notification_logical" to setOf("identity_version"),
            "notification_recipient" to setOf("identity_version"),
            "notification_delivery_provider_checkpoint" to setOf(
                "http_status", "invalidation_reason", "next_attempt"
            ),
            "notification_provider_credential_circuit" to setOf("blocked_credential_fingerprint"),
            "notification_delivery" to setOf(
                "logical_now", "clock_revision", "checkpoint_revision", "lease_version", "lease_fence",
                "provider_checkpoint_count", "credential_version", "credential_fingerprint",
                "auth_refresh_count", "correlation_id", "identity_version"
            ),
            "notification_delivery_authority" to setOf("fencing_token")
        )
    }
}

object BackendNotificationPartialRuntimeSchemaMigrationProcessMain {
    @JvmStatic
    fun main(args: Array<String>) {
        val ready = Path.of(args[1])
        val go = Path.of(args[2])
        val result = Path.of(args[3])
        ready.writeText("ready")
        awaitGo(go)
        val outcome = runCatching {
            val configuration = DeviceRegistrationStoreConfiguration.resolve(
                systemProperties = mapOf(
                    "wakeve.notification.device-registration.db.path" to args[0],
                    "wakeve.notification.device-registration.legacy-identity-hmac-key" to "h".repeat(32),
                    "wakeve.notification.device-registration.token-encryption-key" to "e".repeat(32)
                ),
                environment = emptyMap()
            ).getOrThrow()
            SqliteBackendNotificationDeliveryStoreFactory(configuration).openDeliveryRuntime().use { }
            "READY"
        }.getOrElse { failure -> "ERROR|${failure::class.java.simpleName}|${failure.message}" }
        result.writeText(outcome)
    }

    private fun awaitGo(go: Path) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (!go.exists() && System.nanoTime() < deadline) Thread.sleep(10)
        check(go.exists()) { "partial-schema start gate timed out" }
    }
}
