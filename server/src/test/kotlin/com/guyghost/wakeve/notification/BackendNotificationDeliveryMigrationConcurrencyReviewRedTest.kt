package com.guyghost.wakeve.notification

import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path
import java.sql.DriverManager
import java.util.concurrent.TimeUnit
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class BackendNotificationDeliveryMigrationConcurrencyReviewRedTest {
    @Test
    fun `two JVM preflights serialize legacy schema migration without busy or partial DDL`() = runBlocking {
        repeat(4) { round ->
            BackendNotificationDurabilityTestFixture().use { fixture ->
                val registration = fixture.register("delivery-migration-$round")
                createRawLegacyDeliverySchema(
                    fixture.databasePath,
                    registration.installationId,
                    "legacy-delivery-$round"
                )
                val gate = fixture.root.resolve("delivery-migration-gate-$round")
                Files.createDirectories(gate)
                val processes = (0..1).map { index ->
                    startProcess(
                        fixture,
                        gate.resolve("ready-$index"),
                        gate.resolve("go"),
                        gate.resolve("result-$index")
                    )
                }
                try {
                    awaitFiles(gate.resolve("ready-0"), gate.resolve("ready-1"))
                    gate.resolve("go").writeText("go")
                    processes.forEach { awaitSuccessfulChild(it) }
                    val results = (0..1).map { gate.resolve("result-$it").readText().trim() }
                    assertEquals(listOf("READY", "READY"), results.sorted(), results.toString())
                    assertTrue(results.none { "BUSY" in it || it.startsWith("ERROR|") }, results.toString())
                } finally {
                    processes.forEach(::terminateChild)
                }

                fixture.deliveryFactory.open().use { store ->
                    val migrated = requireNotNull(store.delivery(DeliveryKey("legacy-delivery-$round")))
                    assertEquals(registration.registrationId, migrated.registrationId)
                    assertEquals(BackendDeliveryStatus.RETRY, migrated.status)
                    assertEquals(2, migrated.attempt)
                    assertEquals(500L, migrated.nextAttemptAtEpochSeconds)
                }
                fixture.deliveryFactory.open().use { }

                val replacement = fixture.register(
                    installationId = registration.installationId,
                    userId = "delivery-migration-replacement-$round",
                    token = "delivery-migration-replacement-token-$round"
                )
                assertNotEquals(registration.registrationId, replacement.registrationId)
                fixture.deliveryFactory.open().use { store ->
                    assertEquals(
                        registration.registrationId,
                        requireNotNull(store.delivery(DeliveryKey("legacy-delivery-$round"))).registrationId,
                        "migration or later registration retargeted a historical delivery"
                    )
                }

                DriverManager.getConnection("jdbc:sqlite:${fixture.databasePath}").use { connection ->
                    assertFalse(connection.columns("notification_delivery").contains("installation_id"))
                    connection.prepareStatement(
                        "SELECT authority, fencing_token FROM notification_delivery_authority WHERE delivery_key = ?"
                    ).use { statement ->
                        statement.setString(1, "legacy-delivery-$round")
                        statement.executeQuery().use { rows ->
                            assertTrue(rows.next(), "migrated legacy delivery lost its closed authority")
                            assertEquals("legacy", rows.getString("authority"))
                            assertTrue(rows.getLong("fencing_token") > 0)
                            assertFalse(rows.next())
                        }
                    }
                    assertEquals(1, connection.rowCount("notification_delivery"))
                    assertEquals(1, connection.rowCount("notification_recipient_registration"))
                }
            }
        }
    }

    private fun startProcess(
        fixture: BackendNotificationDurabilityTestFixture,
        ready: Path,
        go: Path,
        result: Path
    ): Process = ProcessBuilder(
        Path.of(System.getProperty("java.home"), "bin", "java").toString(),
        "-cp", System.getProperty("java.class.path"),
        BackendNotificationDeliveryMigrationConcurrencyProcessMain::class.java.name,
        fixture.databasePath.toString(), ready.toString(), go.toString(), result.toString()
    ).redirectErrorStream(true).start()

    private fun awaitFiles(vararg files: Path) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (files.any { !it.exists() } && System.nanoTime() < deadline) Thread.sleep(10)
        assertTrue(files.all { it.exists() }, "migration children did not reach the start gate")
    }

    private fun awaitSuccessfulChild(process: Process) {
        val finished = process.waitFor(15, TimeUnit.SECONDS)
        if (!finished) process.destroyForcibly()
        assertTrue(finished, "delivery migration child timed out")
        assertEquals(0, process.exitValue(), process.inputStream.bufferedReader().readText())
    }

    private fun terminateChild(process: Process) {
        if (process.isAlive) {
            process.destroyForcibly()
            process.waitFor(2, TimeUnit.SECONDS)
        }
    }

    private fun createRawLegacyDeliverySchema(
        databasePath: Path,
        installationId: String,
        deliveryKey: String
    ) {
        DriverManager.getConnection("jdbc:sqlite:$databasePath").use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("PRAGMA foreign_keys = ON")
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
                    ") VALUES (?, 'legacy-recipient', ?, 'apns', 'RETRY', 2, 500, 1000, NULL, NULL)"
            ).use { statement ->
                statement.setString(1, deliveryKey)
                statement.setString(2, installationId)
                statement.executeUpdate()
            }
        }
    }

    private fun java.sql.Connection.columns(table: String): Set<String> = createStatement().use { statement ->
        statement.executeQuery("PRAGMA table_info($table)").use { rows ->
            buildSet { while (rows.next()) add(rows.getString("name")) }
        }
    }

    private fun java.sql.Connection.rowCount(table: String): Int = createStatement().use { statement ->
        statement.executeQuery("SELECT COUNT(*) FROM $table").use { rows -> rows.next(); rows.getInt(1) }
    }
}

object BackendNotificationDeliveryMigrationConcurrencyProcessMain {
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
            SqliteBackendNotificationDeliveryStoreFactory(configuration).open().use { }
            "READY"
        }.getOrElse { failure ->
            "ERROR|${failure::class.java.simpleName}|${failure.message}"
        }
        result.writeText(outcome)
    }

    private fun awaitGo(go: Path) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (!go.exists() && System.nanoTime() < deadline) Thread.sleep(10)
        check(go.exists()) { "delivery migration start gate timed out" }
    }
}
