package com.guyghost.wakeve.notification

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.guyghost.wakeve.database.WakeveDb
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * RED boundary for task 3.2: provider deliveries are keyed to historical backend registrations,
 * never to stable installations or to the mobile SQLDelight schema.
 */
class BackendNotificationDeliveryRegistrationIdentityRedTest {
    @Test
    fun notificationContractsAreRegistrationScopedAndExposeRegistrationEnrollment() {
        val recipientFields = BackendNotificationRecipient::class.java.declaredFields
            .filterNot { it.isSynthetic }
            .map { it.name }
            .toSet()
        assertTrue("registrationIds" in recipientFields, "recipients must target registration IDs")
        assertFalse("installationIds" in recipientFields, "stable installation IDs are not delivery targets")

        val deliveryFields = BackendNotificationDelivery::class.java.declaredFields
            .filterNot { it.isSynthetic }
            .map { it.name }
            .toSet()
        assertTrue("registrationId" in deliveryFields, "a provider delivery must retain its registration ID")
        assertFalse("installationId" in deliveryFields, "a provider delivery must not target an installation ID")

        assertTrue(
            BackendNotificationDeliveryStore::class.java.methods.any { it.name == "registerRegistration" },
            "the delivery port must enroll a backend registration, never a stable installation"
        )
    }

    @Test
    fun deliverySchemaUsesRegistrationForeignKeyUniqueIdentityAndPreservesRegistrationHistory() = runBlocking {
        val root = Files.createTempDirectory("wakeve-delivery-registration-identity-contract-")
        val databasePath = root.resolve("backend-notifications.sqlite")
        val previousProperties = savedConfigurationProperties()
        try {
            val registrationFactory = SqliteBackendDeviceRegistrationStoreFactory(
                registrationConfiguration(databasePath).getOrThrow()
            )
            val registration = registrationFactory.open().use { store ->
                store.register(
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
            val deliveryStore = SqliteBackendNotificationDeliveryStoreFactory().open()
            (deliveryStore as? AutoCloseable)?.close()

            registrationFactory.open().use { reopened ->
                assertNotNull(
                    reopened.registration(registration.registrationId),
                    "opening delivery state in the registration datastore must preserve registration history"
                )
            }

            DriverManager.getConnection("jdbc:sqlite:$databasePath").use { connection ->
                connection.createStatement().use { it.execute("PRAGMA foreign_keys = ON") }
                val deliveryColumns = connection.tableColumns("notification_delivery")
                assertTrue(
                    REQUIRED_DELIVERY_COLUMNS.all(deliveryColumns::contains),
                    "notification_delivery is missing durable provider fields: " +
                        REQUIRED_DELIVERY_COLUMNS.minus(deliveryColumns)
                )
                assertTrue(
                    connection.foreignKeys("notification_delivery").any {
                        it.from == "device_registration_id" &&
                            it.table == "device_registration" &&
                            it.to == "registration_id" &&
                            it.onDelete != "CASCADE"
                    },
                    "delivery registration FK must be non-cascade and reference the historical registration"
                )
                assertTrue(
                    connection.uniqueIndexes("notification_delivery").any {
                        it == setOf("recipient_key", "device_registration_id", "provider")
                    },
                    "a second delivery key must not duplicate one recipient/registration/provider identity"
                )

                connection.prepareStatement(
                    "INSERT INTO notification_recipient(recipient_key, effect_key, status, expires_at_epoch_seconds) VALUES (?, ?, ?, ?)"
                ).use { statement ->
                    statement.setString(1, "recipient-a")
                    statement.setString(2, "effect-a")
                    statement.setString(3, "TARGETED")
                    statement.setLong(4, 10_000)
                    statement.executeUpdate()
                }
                insertDelivery(connection, "delivery-a", registration.registrationId)
                val duplicate = runCatching { insertDelivery(connection, "delivery-b", registration.registrationId) }
                assertTrue(
                    duplicate.isFailure,
                    "unique recipient/registration/provider identity must reject a different delivery key"
                )
            }
        } finally {
            restoreConfigurationProperties(previousProperties)
            Files.deleteIfExists(databasePath)
            Files.deleteIfExists(root)
        }
    }

    @Test
    fun mobileSqlDelightSchemaContainsNoBackendRegistrationOrProviderDeliveryArtifacts() {
        val localDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        WakeveDb.Schema.create(localDriver)
        val backendTables = localDriver.executeQuery(
            identifier = null,
            sql = "SELECT name FROM sqlite_master WHERE type = 'table' AND name IN ('device_installation', 'device_registration', 'notification_recipient', 'notification_delivery')",
            mapper = { cursor ->
                val names = mutableListOf<String>()
                while (cursor.next().value) names += cursor.getString(0).orEmpty()
                app.cash.sqldelight.db.QueryResult.Value(names)
            },
            parameters = 0
        ).value

        assertTrue(backendTables.isEmpty(), "backend registration/delivery artifacts must not leak into local SQLDelight")
    }

    private fun insertDelivery(connection: Connection, deliveryKey: String, registrationId: String) {
        connection.prepareStatement(
            """
            INSERT INTO notification_delivery(
                delivery_key, logical_notification_id, idempotency_key, recipient_key,
                device_registration_id, provider, status, attempt, expires_at_epoch_seconds
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, deliveryKey)
            statement.setString(2, "notification-a")
            statement.setString(3, "idempotency-a")
            statement.setString(4, "recipient-a")
            statement.setString(5, registrationId)
            statement.setString(6, "apns")
            statement.setString(7, BackendDeliveryStatus.QUEUED.name)
            statement.setInt(8, 0)
            statement.setLong(9, 10_000)
            statement.executeUpdate()
        }
    }

    private fun registrationConfiguration(path: Path): Result<DeviceRegistrationStoreConfiguration> =
        DeviceRegistrationStoreConfiguration.resolve(
            environment = emptyMap(),
            systemProperties = mapOf(
                DATABASE_PATH_PROPERTY to path.toString(),
                LEGACY_HMAC_KEY_PROPERTY to HMAC_KEY,
                TOKEN_ENCRYPTION_KEY_PROPERTY to TOKEN_ENCRYPTION_KEY
            )
        )

    private fun configureDeviceRegistrationProperties(databasePath: Path) {
        System.setProperty(DATABASE_PATH_PROPERTY, databasePath.toString())
        System.setProperty(LEGACY_HMAC_KEY_PROPERTY, HMAC_KEY)
        System.setProperty(TOKEN_ENCRYPTION_KEY_PROPERTY, TOKEN_ENCRYPTION_KEY)
    }

    private fun savedConfigurationProperties(): Map<String, String?> = CONFIGURATION_PROPERTIES.associateWith(System::getProperty)

    private fun restoreConfigurationProperties(previous: Map<String, String?>) {
        previous.forEach { (property, value) ->
            if (value == null) System.clearProperty(property) else System.setProperty(property, value)
        }
    }

    private fun Connection.tableColumns(table: String): Set<String> = createStatement().use { statement ->
        statement.executeQuery("PRAGMA table_info($table)").use { rows ->
            buildSet { while (rows.next()) add(rows.getString("name")) }
        }
    }

    private fun Connection.foreignKeys(table: String): List<ForeignKey> = createStatement().use { statement ->
        statement.executeQuery("PRAGMA foreign_key_list($table)").use { rows ->
            buildList {
                while (rows.next()) add(
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

    private data class ForeignKey(val table: String, val from: String, val to: String, val onDelete: String)

    private companion object {
        const val DELIVERY_STORE_PROPERTY = "wakeve.notification.delivery.db.path"
        const val DATABASE_PATH_PROPERTY = "wakeve.notification.device-registration.db.path"
        const val LEGACY_HMAC_KEY_PROPERTY =
            "wakeve.notification.device-registration.legacy-identity-hmac-key"
        const val TOKEN_ENCRYPTION_KEY_PROPERTY =
            "wakeve.notification.device-registration.token-encryption-key"
        const val HMAC_KEY = "delivery-identity-hmac-key-with-at-least-32-bytes"
        const val TOKEN_ENCRYPTION_KEY = "delivery-identity-encryption-key-with-at-least-32-bytes"

        val CONFIGURATION_PROPERTIES = setOf(
            DELIVERY_STORE_PROPERTY,
            DATABASE_PATH_PROPERTY,
            LEGACY_HMAC_KEY_PROPERTY,
            TOKEN_ENCRYPTION_KEY_PROPERTY
        )

        val REQUIRED_DELIVERY_COLUMNS = setOf(
            "accepted_at",
            "provider_status",
            "provider_reason",
            "provider_request_id",
            "lease_owner",
            "lease_expires_at_epoch_seconds",
            "attempt",
            "next_attempt_at_epoch_seconds",
            "expires_at_epoch_seconds",
            "logical_notification_id",
            "idempotency_key"
        )
    }
}
