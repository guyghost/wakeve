package com.guyghost.wakeve.notification

import kotlinx.coroutines.runBlocking
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BackendNotificationIngestionDurabilityRedTest {
    @Test
    fun `one backend commit persists receipt logical recipient and frozen deliveries before post commit work`() =
        runBlocking {
            BackendNotificationDurabilityTestFixture().use { fixture ->
                val phone = fixture.register("ingestion-phone")
                val tablet = fixture.register("ingestion-tablet")
                var postCommitCalls = 0
                val service = BackendNotificationIngestionService(
                    storeFactory = fixture.deliveryFactory,
                    faultInjector = BackendNotificationIngestionFaultInjector { },
                    committedPort = BackendNotificationIngestionCommittedPort { receipt ->
                        postCommitCalls += 1
                        assertEquals(1, fixture.rowCount("domain_event_ingestion"))
                        assertEquals(1, fixture.rowCount("notification_logical"))
                        assertEquals(1, fixture.rowCount("notification_recipient"))
                        assertEquals(2, fixture.rowCount("notification_delivery"))
                        assertEquals(2, receipt.deliveryKeys.size)
                    }
                )
                val command = ingestionCommand(listOf(tablet.registrationId, phone.registrationId, phone.registrationId))

                val first = service.ingest(command)
                assertEquals(1, postCommitCalls, "Newly committed work is exposed only after its durable transaction.")
                val replay = service.ingest(command)

                assertTrue(first.created)
                assertFalse(replay.created)
                assertEquals(first.transactionId, replay.transactionId)
                assertEquals(first.effectKey, replay.effectKey)
                assertEquals(first.deliveryKeys, replay.deliveryKeys)
                assertEquals(2, first.deliveryKeys.size)
                assertEquals(BackendDecisionSyncStatus.ACKNOWLEDGED, first.decisionSyncStatus)
                assertEquals(BackendEffectDispatchStatus.QUEUED, first.effectDispatchStatus)
                assertEquals(1, fixture.rowCount("domain_event_ingestion"))
                assertEquals(1, fixture.rowCount("notification_logical"))
                assertEquals(1, fixture.rowCount("notification_recipient"))
                assertEquals(2, fixture.rowCount("notification_delivery"))
                assertFalse(
                    fixture.tableExists("confirmation_effect_outbox"),
                    "The local confirmation outbox is never a backend/server table."
                )
            }
        }

    @Test
    fun `an injected failure at every ingestion write boundary rolls the whole backend transaction back`() =
        runBlocking {
            listOf(
                BackendNotificationIngestionCheckpoint.AFTER_DOMAIN_EVENT_INGESTION_WRITE,
                BackendNotificationIngestionCheckpoint.AFTER_LOGICAL_NOTIFICATION_WRITE,
                BackendNotificationIngestionCheckpoint.AFTER_RECIPIENTS_WRITE,
                BackendNotificationIngestionCheckpoint.AFTER_DELIVERIES_WRITE
            ).forEach { failingCheckpoint ->
                BackendNotificationDurabilityTestFixture().use { fixture ->
                    val registration = fixture.register("rollback-${failingCheckpoint.name.lowercase()}")
                    var postCommitCalls = 0
                    val service = BackendNotificationIngestionService(
                        storeFactory = fixture.deliveryFactory,
                        faultInjector = BackendNotificationIngestionFaultInjector { checkpoint ->
                            if (checkpoint == failingCheckpoint) error("injected-${checkpoint.name}")
                        },
                        committedPort = BackendNotificationIngestionCommittedPort { postCommitCalls += 1 }
                    )

                    assertFails { service.ingest(ingestionCommand(listOf(registration.registrationId))) }

                    assertEquals(0, fixture.rowCountIfPresent("domain_event_ingestion"), failingCheckpoint.name)
                    assertEquals(0, fixture.rowCountIfPresent("notification_logical"), failingCheckpoint.name)
                    assertEquals(0, fixture.rowCountIfPresent("notification_recipient"), failingCheckpoint.name)
                    assertEquals(0, fixture.rowCountIfPresent("notification_delivery"), failingCheckpoint.name)
                    assertEquals(0, postCommitCalls, failingCheckpoint.name)
                }
            }
        }

    @Test
    fun `backend acknowledgement changes decision sync only and cannot imply provider acceptance`() = runBlocking {
        BackendNotificationDurabilityTestFixture().use { fixture ->
            val registration = fixture.register("decision-boundary")
            val receipt = BackendNotificationIngestionService(
                storeFactory = fixture.deliveryFactory,
                faultInjector = BackendNotificationIngestionFaultInjector { },
                committedPort = BackendNotificationIngestionCommittedPort { }
            ).ingest(ingestionCommand(listOf(registration.registrationId)))

            assertEquals(BackendDecisionSyncStatus.ACKNOWLEDGED, receipt.decisionSyncStatus)
            assertEquals(BackendEffectDispatchStatus.QUEUED, receipt.effectDispatchStatus)
            assertEquals(0, fixture.countWhereNotNull("notification_delivery", "accepted_at"))
            assertEquals(0, fixture.countWhere("notification_delivery", "status", "ACCEPTED_BY_APNS"))
        }
    }

    private fun ingestionCommand(registrationIds: List<String>) = BackendNotificationIngestionCommand(
        domainEventId = "domain-event-1",
        effectType = "DATE_CONFIRMED",
        schemaVersion = 2,
        logicalNotificationId = "logical-notification-1",
        recipients = listOf(
            BackendNotificationRecipientIntent(
                participantId = "participant-1",
                channel = "push",
                provider = "apns",
                registrationIds = registrationIds,
                expiresAtEpochSeconds = 1_000
            )
        )
    )
}

private fun BackendNotificationDurabilityTestFixture.tableExists(table: String): Boolean =
    DriverManager.getConnection("jdbc:sqlite:$databasePath").use { connection ->
        connection.createStatement().use { it.execute("PRAGMA query_only = ON") }
        connection.prepareStatement(
            "SELECT 1 FROM sqlite_schema WHERE type = 'table' AND name = ?"
        ).use { statement ->
            statement.setString(1, table)
            statement.executeQuery().use { it.next() }
        }
    }

private fun BackendNotificationDurabilityTestFixture.rowCount(table: String): Int =
    DriverManager.getConnection("jdbc:sqlite:$databasePath").use { connection ->
        connection.createStatement().use { statement ->
            statement.execute("PRAGMA query_only = ON")
            statement.executeQuery("SELECT COUNT(*) FROM \"$table\"").use { rows ->
                rows.next()
                rows.getInt(1)
            }
        }
    }

private fun BackendNotificationDurabilityTestFixture.rowCountIfPresent(table: String): Int =
    if (tableExists(table)) rowCount(table) else 0

private fun BackendNotificationDurabilityTestFixture.countWhereNotNull(table: String, column: String): Int =
    DriverManager.getConnection("jdbc:sqlite:$databasePath").use { connection ->
        connection.createStatement().use { statement ->
            statement.execute("PRAGMA query_only = ON")
            statement.executeQuery("SELECT COUNT(*) FROM \"$table\" WHERE \"$column\" IS NOT NULL").use { rows ->
                rows.next()
                rows.getInt(1)
            }
        }
    }

private fun BackendNotificationDurabilityTestFixture.countWhere(
    table: String,
    column: String,
    value: String
): Int = DriverManager.getConnection("jdbc:sqlite:$databasePath").use { connection ->
    connection.createStatement().use { it.execute("PRAGMA query_only = ON") }
    connection.prepareStatement("SELECT COUNT(*) FROM \"$table\" WHERE \"$column\" = ?").use { statement ->
        statement.setString(1, value)
        statement.executeQuery().use { rows ->
            rows.next()
            rows.getInt(1)
        }
    }
}
