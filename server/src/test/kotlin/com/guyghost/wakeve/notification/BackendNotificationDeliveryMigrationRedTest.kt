package com.guyghost.wakeve.notification

import java.lang.reflect.Constructor
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.util.Comparator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * RED migration boundary: an installation-scoped delivery outbox must be upgraded in place to
 * the historical registration identity without losing durable worker state.
 */
class BackendNotificationDeliveryMigrationRedTest {
    @Test
    fun additiveMigrationRemapsLegacyInstallationDeliveriesToActiveRegistrationIdempotently() {
        val directory = Files.createTempDirectory("wakeve-delivery-migration-contract-")
        val databasePath = directory.resolve("notifications.sqlite")
        val previousProperties = savedConfigurationProperties()
        try {
            createValidDeviceRegistrationSchemaWithActiveRegistration(databasePath)
            createLegacyInstallationScopedDeliverySchema(databasePath)
            configureDeviceRegistrationProperties(databasePath)
            System.setProperty(DELIVERY_STORE_PROPERTY, databasePath.toString())

            SqliteBackendNotificationDeliveryStoreFactory().open()

            assertMigratedDelivery(databasePath)

            SqliteBackendNotificationDeliveryStoreFactory().open()
            DriverManager.getConnection("jdbc:sqlite:$databasePath").use { connection ->
                assertEquals(1, connection.count("notification_delivery"), "a second open must not duplicate delivery rows")
                assertEquals(
                    1,
                    connection.count("notification_recipient_registration"),
                    "a second open must not duplicate recipient/registration associations"
                )
                assertFalse(connection.tableExists("notification_recipient_installation"))
            }
        } finally {
            restoreConfigurationProperties(previousProperties)
            deleteDirectory(directory)
        }
    }

    @Test
    fun factoryBoundToSharedRegistrationConfigurationRejectsAValidButDistinctDeliveryDatabase() {
        val directory = Files.createTempDirectory("wakeve-delivery-shared-db-contract-")
        val registrationDatabase = directory.resolve("registration.sqlite")
        val distinctDeliveryDatabase = directory.resolve("delivery.sqlite")
        val previousProperties = savedConfigurationProperties()
        try {
            createValidDeviceRegistrationSchemaWithActiveRegistration(registrationDatabase)
            createValidDeviceRegistrationSchemaWithActiveRegistration(distinctDeliveryDatabase)
            configureDeviceRegistrationProperties(registrationDatabase)
            System.setProperty(DELIVERY_STORE_PROPERTY, distinctDeliveryDatabase.toString())

            val boundFactory = deliveryFactoryBoundTo(registrationConfiguration(registrationDatabase).getOrThrow())
            assertFailsWith<IllegalStateException>(
                "a delivery factory must reject a configured database distinct from the immutable registration datastore"
            ) {
                boundFactory.open()
            }
            DriverManager.getConnection("jdbc:sqlite:$distinctDeliveryDatabase").use { connection ->
                assertFalse(
                    connection.tableExists("notification_delivery"),
                    "a rejected distinct datastore must not receive delivery schema mutations"
                )
            }
        } finally {
            restoreConfigurationProperties(previousProperties)
            deleteDirectory(directory)
        }
    }

    private fun createValidDeviceRegistrationSchemaWithActiveRegistration(databasePath: Path) {
        SqliteBackendDeviceRegistrationStoreFactory(registrationConfiguration(databasePath).getOrThrow())
            .open()
            .close()
        DriverManager.getConnection("jdbc:sqlite:$databasePath").use { connection ->
            connection.createStatement().use { it.execute("PRAGMA foreign_keys = ON") }
            connection.prepareStatement(
                """
                INSERT INTO device_installation(
                    installation_id, platform, created_at_epoch_seconds, updated_at_epoch_seconds
                ) VALUES (?, ?, ?, ?)
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, INSTALLATION_ID)
                statement.setString(2, Platform.IOS.name)
                statement.setLong(3, 100)
                statement.setLong(4, 100)
                statement.executeUpdate()
            }
            connection.prepareStatement(
                """
                INSERT INTO device_registration(
                    registration_id, installation_id, user_id, environment, topic,
                    token_ciphertext, token_hash, status,
                    created_at_epoch_seconds, updated_at_epoch_seconds, last_registered_at_epoch_seconds
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, REGISTRATION_ID)
                statement.setString(2, INSTALLATION_ID)
                statement.setString(3, "user-a")
                statement.setString(4, APNsEnvironment.PRODUCTION.name)
                statement.setString(5, "com.guyghost.wakeve")
                statement.setBytes(6, byteArrayOf(1))
                statement.setString(7, "token-hash-a")
                statement.setString(8, DeviceRegistrationStatus.ACTIVE.name)
                statement.setLong(9, 100)
                statement.setLong(10, 100)
                statement.setLong(11, 100)
                statement.executeUpdate()
            }
        }
    }

    private fun createLegacyInstallationScopedDeliverySchema(databasePath: Path) {
        DriverManager.getConnection("jdbc:sqlite:$databasePath").use { connection ->
            connection.createStatement().use { statement ->
                statement.execute(
                    """
                    CREATE TABLE notification_recipient (
                        recipient_key TEXT PRIMARY KEY NOT NULL,
                        effect_key TEXT NOT NULL,
                        status TEXT NOT NULL,
                        expires_at_epoch_seconds INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                statement.execute(
                    """
                    CREATE TABLE notification_recipient_installation (
                        recipient_key TEXT NOT NULL,
                        installation_id TEXT NOT NULL,
                        PRIMARY KEY (recipient_key, installation_id)
                    )
                    """.trimIndent()
                )
                statement.execute(
                    """
                    CREATE TABLE notification_delivery (
                        delivery_key TEXT PRIMARY KEY NOT NULL,
                        recipient_key TEXT NOT NULL,
                        installation_id TEXT NOT NULL,
                        provider TEXT NOT NULL,
                        status TEXT NOT NULL,
                        attempt INTEGER NOT NULL,
                        next_attempt_at_epoch_seconds INTEGER,
                        expires_at_epoch_seconds INTEGER NOT NULL,
                        lease_owner TEXT,
                        lease_expires_at_epoch_seconds INTEGER
                    )
                    """.trimIndent()
                )
            }
            connection.prepareStatement(
                "INSERT INTO notification_recipient(recipient_key, effect_key, status, expires_at_epoch_seconds) VALUES (?, ?, ?, ?)"
            ).use { statement ->
                statement.setString(1, RECIPIENT_KEY)
                statement.setString(2, "effect-a")
                statement.setString(3, BackendRecipientStatus.TARGETED.name)
                statement.setLong(4, 1_000)
                statement.executeUpdate()
            }
            connection.prepareStatement(
                "INSERT INTO notification_recipient_installation(recipient_key, installation_id) VALUES (?, ?)"
            ).use { statement ->
                statement.setString(1, RECIPIENT_KEY)
                statement.setString(2, INSTALLATION_ID)
                statement.executeUpdate()
            }
            connection.prepareStatement(
                """
                INSERT INTO notification_delivery(
                    delivery_key, recipient_key, installation_id, provider, status, attempt,
                    next_attempt_at_epoch_seconds, expires_at_epoch_seconds,
                    lease_owner, lease_expires_at_epoch_seconds
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, LEGACY_DELIVERY_KEY)
                statement.setString(2, RECIPIENT_KEY)
                statement.setString(3, INSTALLATION_ID)
                statement.setString(4, "apns")
                statement.setString(5, BackendDeliveryStatus.RETRY.name)
                statement.setInt(6, 2)
                statement.setLong(7, 500)
                statement.setLong(8, 1_000)
                statement.setString(9, "worker-a")
                statement.setLong(10, 600)
                statement.executeUpdate()
            }
        }
    }

    private fun assertMigratedDelivery(databasePath: Path) {
        DriverManager.getConnection("jdbc:sqlite:$databasePath").use { connection ->
            connection.createStatement().use { it.execute("PRAGMA foreign_keys = ON") }
            val deliveryColumns = connection.columns("notification_delivery")
            assertTrue(
                REQUIRED_DELIVERY_COLUMNS.all(deliveryColumns::contains),
                "the additive migration must add safe provider/audit columns: " +
                    REQUIRED_DELIVERY_COLUMNS.minus(deliveryColumns)
            )
            assertTrue(
                connection.foreignKeys("notification_delivery").any {
                    it.from == "device_registration_id" &&
                        it.table == "device_registration" &&
                        it.to == "registration_id" &&
                        it.onDelete != "CASCADE"
                },
                "migrated deliveries must retain a non-cascade historical registration FK"
            )
            assertTrue(
                connection.uniqueIndexes("notification_delivery").any {
                    it == setOf("recipient_key", "device_registration_id", "provider")
                },
                "migrated deliveries must enforce registration-scoped provider identity"
            )
            assertFalse(connection.tableExists("notification_recipient_installation"))

            connection.prepareStatement(
                """
                SELECT delivery_key, logical_notification_id, idempotency_key, recipient_key,
                       device_registration_id, provider, status, attempt,
                       next_attempt_at_epoch_seconds, expires_at_epoch_seconds,
                       lease_owner, lease_expires_at_epoch_seconds,
                       accepted_at, provider_status, provider_reason, provider_request_id
                FROM notification_delivery
                WHERE delivery_key = ?
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, LEGACY_DELIVERY_KEY)
                statement.executeQuery().use { rows ->
                    assertTrue(rows.next(), "the legacy queued delivery must remain present")
                    assertEquals(LEGACY_DELIVERY_KEY, rows.getString("delivery_key"))
                    assertEquals(LEGACY_DELIVERY_KEY, rows.getString("logical_notification_id"))
                    assertEquals(LEGACY_DELIVERY_KEY, rows.getString("idempotency_key"))
                    assertEquals(RECIPIENT_KEY, rows.getString("recipient_key"))
                    assertEquals(REGISTRATION_ID, rows.getString("device_registration_id"))
                    assertEquals("apns", rows.getString("provider"))
                    assertEquals(BackendDeliveryStatus.RETRY.name, rows.getString("status"))
                    assertEquals(2, rows.getInt("attempt"))
                    assertEquals(500, rows.getLong("next_attempt_at_epoch_seconds"))
                    assertEquals(1_000, rows.getLong("expires_at_epoch_seconds"))
                    assertEquals("worker-a", rows.getString("lease_owner"))
                    assertEquals(600, rows.getLong("lease_expires_at_epoch_seconds"))
                    assertNull(rows.getObject("accepted_at"))
                    assertNull(rows.getObject("provider_status"))
                    assertNull(rows.getString("provider_reason"))
                    assertNull(rows.getString("provider_request_id"))
                    assertFalse(rows.next(), "migration must preserve one row, not duplicate it")
                }
            }
            connection.prepareStatement(
                "SELECT device_registration_id FROM notification_recipient_registration WHERE recipient_key = ?"
            ).use { statement ->
                statement.setString(1, RECIPIENT_KEY)
                statement.executeQuery().use { rows ->
                    assertTrue(rows.next(), "the legacy recipient target must be remapped")
                    assertEquals(REGISTRATION_ID, rows.getString("device_registration_id"))
                    assertFalse(rows.next(), "the recipient has one migrated active registration target")
                }
            }
        }
    }

    private fun deliveryFactoryBoundTo(
        registrationConfiguration: DeviceRegistrationStoreConfiguration
    ): BackendNotificationDeliveryStoreFactory {
        val constructor = SqliteBackendNotificationDeliveryStoreFactory::class.java.constructors
            .filterIsInstance<Constructor<*>>()
            .singleOrNull { it.parameterTypes.contentEquals(arrayOf(DeviceRegistrationStoreConfiguration::class.java)) }
        val boundConstructor = assertNotNull(
            constructor,
            "delivery factory must expose an explicit immutable DeviceRegistrationStoreConfiguration constructor"
        )
        return boundConstructor.newInstance(registrationConfiguration) as BackendNotificationDeliveryStoreFactory
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

    private fun Connection.columns(table: String): Set<String> = createStatement().use { statement ->
        statement.executeQuery("PRAGMA table_info($table)").use { rows ->
            buildSet { while (rows.next()) add(rows.getString("name")) }
        }
    }

    private fun Connection.foreignKeys(table: String): List<ForeignKey> = createStatement().use { statement ->
        statement.executeQuery("PRAGMA foreign_key_list($table)").use { rows ->
            buildList {
                while (rows.next()) {
                    add(
                        ForeignKey(
                            table = rows.getString("table"),
                            from = rows.getString("from"),
                            to = rows.getString("to"),
                            onDelete = rows.getString("on_delete")
                        )
                    )
                }
            }
        }
    }

    private fun Connection.uniqueIndexes(table: String): List<Set<String>> = createStatement().use { statement ->
        statement.executeQuery("PRAGMA index_list($table)").use { indexes ->
            buildList {
                while (indexes.next()) {
                    if (indexes.getInt("unique") == 1) {
                        val indexName = indexes.getString("name")
                        createStatement().use { indexStatement ->
                            indexStatement.executeQuery("PRAGMA index_info($indexName)").use { columns ->
                                add(buildSet { while (columns.next()) add(columns.getString("name")) })
                            }
                        }
                    }
                }
            }
        }
    }

    private fun Connection.tableExists(table: String): Boolean = prepareStatement(
        "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?"
    ).use { statement ->
        statement.setString(1, table)
        statement.executeQuery().use { it.next() }
    }

    private fun Connection.count(table: String): Int = createStatement().use { statement ->
        statement.executeQuery("SELECT COUNT(*) FROM $table").use { rows -> rows.next(); rows.getInt(1) }
    }

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

    private fun deleteDirectory(directory: Path) {
        Files.walk(directory).sorted(Comparator.reverseOrder<Path>()).use { paths ->
            paths.forEach { Files.deleteIfExists(it) }
        }
    }

    private data class ForeignKey(val table: String, val from: String, val to: String, val onDelete: String)

    private companion object {
        const val DELIVERY_STORE_PROPERTY = "wakeve.notification.delivery.db.path"
        const val DEVICE_REGISTRATION_DATABASE_PATH_PROPERTY = "wakeve.notification.device-registration.db.path"
        const val LEGACY_HMAC_KEY_PROPERTY =
            "wakeve.notification.device-registration.legacy-identity-hmac-key"
        const val TOKEN_ENCRYPTION_KEY_PROPERTY =
            "wakeve.notification.device-registration.token-encryption-key"
        const val HMAC_KEY = "migration-delivery-hmac-key-with-at-least-32-bytes"
        const val TOKEN_ENCRYPTION_KEY = "migration-delivery-encryption-key-with-at-least-32-bytes"
        const val INSTALLATION_ID = "installation-a"
        const val REGISTRATION_ID = "registration-a"
        const val RECIPIENT_KEY = "recipient-a"
        const val LEGACY_DELIVERY_KEY = "recipient-a:installation-a:apns"

        val CONFIGURATION_PROPERTIES = setOf(
            DELIVERY_STORE_PROPERTY,
            DEVICE_REGISTRATION_DATABASE_PATH_PROPERTY,
            LEGACY_HMAC_KEY_PROPERTY,
            TOKEN_ENCRYPTION_KEY_PROPERTY
        )

        val REQUIRED_DELIVERY_COLUMNS = setOf(
            "delivery_key",
            "logical_notification_id",
            "idempotency_key",
            "recipient_key",
            "device_registration_id",
            "provider",
            "status",
            "attempt",
            "next_attempt_at_epoch_seconds",
            "expires_at_epoch_seconds",
            "lease_owner",
            "lease_expires_at_epoch_seconds",
            "accepted_at",
            "provider_status",
            "provider_reason",
            "provider_request_id"
        )
    }
}
