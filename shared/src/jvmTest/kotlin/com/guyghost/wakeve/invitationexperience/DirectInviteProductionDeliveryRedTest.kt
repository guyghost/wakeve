package com.guyghost.wakeve.invitationexperience

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.EventStatus
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

class DirectInviteProductionDeliveryRedTest {

    @Test
    fun `recipient owner seals normalized input and rejects forged envelope dimensions`() {
        val normalizedInputs = mutableListOf<String>()
        val binding = binding()
        val expiresAt = Clock.System.now().plus(29.days).toString()
        val owner = DirectInviteRecipientKeyOwner(
            digestPort = DirectInviteRecipientDigestPort { normalized ->
                normalizedInputs += normalized
                "a1".repeat(32)
            },
            keyVersion = 3
        )
        val sealed = owner.protectAndSeal(
            rawRecipientInput = "  Alice@Example.COM  ",
            binding = binding,
            expiresAt = expiresAt,
            sealer = DirectInviteDeliverySealer { actualBinding, recipientKey, normalized, expiry ->
                DirectInviteDeliveryEnvelope(
                    binding = actualBinding,
                    recipientKey = recipientKey,
                    ciphertext = "ciphertext-v3-${recipientKey.value.takeLast(12)}",
                    keyVersion = 3,
                    expiresAt = expiry
                ).also {
                    assertEquals("alice@example.com", normalized)
                }
            }
        )

        assertNotNull(sealed)
        assertEquals(listOf("alice@example.com"), normalizedInputs)
        assertEquals(binding, sealed.envelope.binding)
        assertEquals(sealed.recipientKey, sealed.envelope.recipientKey)
        assertNotEquals("alice@example.com", sealed.envelope.ciphertext)
        assertNotEquals(sealed.recipientKey.value, sealed.envelope.ciphertext)

        assertNull(
            owner.protectAndSeal(
                rawRecipientInput = "alice@example.com",
                binding = binding,
                expiresAt = expiresAt,
                sealer = DirectInviteDeliverySealer { _, recipientKey, _, expiry ->
                    DirectInviteDeliveryEnvelope(
                        binding = binding.copy(operationId = "forged-operation"),
                        recipientKey = recipientKey,
                        ciphertext = "ciphertext-forged-binding",
                        keyVersion = 3,
                        expiresAt = expiry
                    )
                }
            ),
            "The sealer cannot substitute an operation binding after the capability was checked."
        )
        assertNull(
            owner.protectAndSeal(
                rawRecipientInput = "alice@example.com",
                binding = binding,
                expiresAt = expiresAt,
                sealer = DirectInviteDeliverySealer { actualBinding, recipientKey, normalized, expiry ->
                    DirectInviteDeliveryEnvelope(
                        binding = actualBinding,
                        recipientKey = recipientKey,
                        ciphertext = normalized,
                        keyVersion = 3,
                        expiresAt = expiry
                    )
                }
            ),
            "A fake sealer returning normalized PII must fail closed before persistence."
        )
    }

    @Test
    fun `legacy key-only submit cannot create a false pending delivery`() = runTest {
        val fixture = fixture()
        fixture.seedDraft()
        val transport = RecordingTransport(mutableListOf())
        val repository = DatabaseDirectInviteBatchRepository(fixture.database, transport)

        val result = repository.submit(command(setOf(RecipientKey("hmac-v1-a1b2c3"))))

        assertIs<DirectInviteOperation.Failed>(result)
        assertTrue(transport.requests.isEmpty())
        assertEquals(0L, fixture.number("SELECT COUNT(*) FROM direct_invite_batch"))
        assertEquals(0L, fixture.number("SELECT COUNT(*) FROM direct_invite_recipient_outcome"))
        assertEquals(0L, fixture.number("SELECT COUNT(*) FROM direct_invite_delivery_envelope"))
    }

    @Test
    fun `offline submit persists exact encrypted envelopes before dispatch and survives relaunch`() = runTest {
        val fixture = fixture()
        fixture.seedDraft()
        val protected = protectedRecipients(binding())
        val transport = RecordingTransport(
            mutableListOf(DirectInviteDeliveryResult.Deferred(InvitationExperienceError.NETWORK_UNAVAILABLE))
        )
        val repository = DatabaseDirectInviteBatchRepository(fixture.database, transport)

        val pending = assertIs<DirectInviteOperation.PendingSync>(
            repository.submit(
                command(protected.mapTo(linkedSetOf()) { it.recipientKey }),
                protected.mapTo(linkedSetOf()) { it.envelope }
            )
        )

        assertEquals(protected.mapTo(linkedSetOf()) { it.recipientKey }, pending.recipientKeys)
        assertEquals(1, transport.requests.size)
        assertEquals(binding(), transport.requests.single().binding)
        assertEquals(protected.mapTo(linkedSetOf()) { it.envelope }, transport.requests.single().envelopes)
        assertEquals(2L, fixture.number("SELECT COUNT(*) FROM direct_invite_delivery_envelope"))
        protected.forEach { recipient ->
            assertEquals(
                recipient.envelope.ciphertext,
                fixture.text(
                    "SELECT ciphertext FROM direct_invite_delivery_envelope " +
                        "WHERE batch_id = 'batch-1' AND recipient_key = '${recipient.recipientKey.value}'"
                )
            )
        }
        assertEquals(
            pending,
            DatabaseDirectInviteBatchRepository(fixture.database, transport).load("batch-1"),
            "A relaunch must reconstruct the queued operation from protected keys while envelopes remain owner-private."
        )
    }

    @Test
    fun `transport acknowledgement persists and retry dispatches only unresolved envelope`() = runTest {
        val fixture = fixture()
        fixture.seedDraft()
        val protected = protectedRecipients(binding())
        val acceptedKey = protected[0].recipientKey
        val retryableKey = protected[1].recipientKey
        val firstOutcomes = mapOf(
            acceptedKey to DirectInviteRecipientOutcome.ServerAccepted("invitation-accepted"),
            retryableKey to DirectInviteRecipientOutcome.Failed(
                InvitationExperienceError.NETWORK_UNAVAILABLE
            )
        )
        val transport = RecordingTransport(
            mutableListOf(
                DirectInviteDeliveryResult.Acknowledged(
                    batchId = "batch-1",
                    operationId = "operation-1",
                    outcomesByRecipientKey = firstOutcomes
                ),
                DirectInviteDeliveryResult.Acknowledged(
                    batchId = "batch-1",
                    operationId = "operation-1",
                    outcomesByRecipientKey = mapOf(
                        retryableKey to DirectInviteRecipientOutcome.ServerAccepted(
                            "invitation-retried"
                        )
                    )
                )
            )
        )
        val repository = DatabaseDirectInviteBatchRepository(fixture.database, transport)
        val failed = assertIs<DirectInviteOperation.Failed>(
            repository.submit(
                command(protected.mapTo(linkedSetOf()) { it.recipientKey }),
                protected.mapTo(linkedSetOf()) { it.envelope }
            )
        )

        val completed = assertIs<DirectInviteOperation.Completed>(
            DatabaseDirectInviteBatchRepository(fixture.database, transport).retry(
                RetryDirectInviteBatchCommand(failed, capability())
            )
        )

        assertEquals(2, transport.requests.size)
        assertEquals(
            setOf(retryableKey),
            transport.requests[1].envelopes.mapTo(linkedSetOf()) { it.recipientKey },
            "A retry must never redispatch the recipient already accepted by the server."
        )
        assertEquals(setOf(acceptedKey, retryableKey), completed.outcomesByRecipientKey.keys)
        assertIs<DirectInviteRecipientOutcome.ServerAccepted>(
            completed.outcomesByRecipientKey.getValue(acceptedKey)
        )
        assertIs<DirectInviteRecipientOutcome.ServerAccepted>(
            completed.outcomesByRecipientKey.getValue(retryableKey)
        )
        assertEquals(
            completed,
            DatabaseDirectInviteBatchRepository(fixture.database, transport).load("batch-1")
        )
    }

    private fun protectedRecipients(
        binding: DirectInviteDeliveryBinding
    ): List<DirectInviteProtectedRecipient> {
        val expiresAt = Clock.System.now().plus(29.days).toString()
        val owner = DirectInviteRecipientKeyOwner(
            digestPort = DirectInviteRecipientDigestPort { normalized ->
                when (normalized) {
                    "alice@example.com" -> "a1".repeat(32)
                    "bob@example.com" -> "b2".repeat(32)
                    else -> null
                }
            },
            keyVersion = 1
        )
        return listOf("alice@example.com", "bob@example.com").map { raw ->
            assertNotNull(
                owner.protectAndSeal(
                    rawRecipientInput = raw,
                    binding = binding,
                    expiresAt = expiresAt,
                    sealer = DirectInviteDeliverySealer { actualBinding, key, _, expiry ->
                        DirectInviteDeliveryEnvelope(
                            binding = actualBinding,
                            recipientKey = key,
                            ciphertext = "ciphertext-${key.value.takeLast(16)}",
                            keyVersion = 1,
                            expiresAt = expiry
                        )
                    }
                )
            )
        }
    }

    private fun command(recipientKeys: Set<RecipientKey>) = SubmitDirectInviteBatchCommand(
        eventId = "event-1",
        actorId = "organizer-1",
        eventStatus = EventStatus.DRAFT,
        batchId = "batch-1",
        operationId = "operation-1",
        recipientKeys = recipientKeys,
        capability = capability()
    )

    private fun capability() = DirectInviteCapability.Ready(
        eventId = "event-1",
        actorId = "organizer-1",
        accessRevision = 4,
        allowedEventStatuses = setOf(EventStatus.DRAFT)
    )

    private fun binding() = DirectInviteDeliveryBinding(
        eventId = "event-1",
        actorId = "organizer-1",
        accessRevision = 4,
        batchId = "batch-1",
        operationId = "operation-1"
    )

    private class RecordingTransport(
        private val responses: MutableList<DirectInviteDeliveryResult>
    ) : DirectInviteDeliveryTransport {
        val requests = mutableListOf<DirectInviteDeliveryRequest>()

        override suspend fun dispatch(request: DirectInviteDeliveryRequest): DirectInviteDeliveryResult {
            requests += request
            return responses.removeFirstOrNull()
                ?: DirectInviteDeliveryResult.Deferred(InvitationExperienceError.NETWORK_UNAVAILABLE)
        }
    }

    private fun fixture(): Fixture {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(null, "PRAGMA foreign_keys = ON", 0).value
        WakeveDb.Schema.create(driver)
        return Fixture(driver, WakeveDb(driver))
    }

    private class Fixture(
        private val driver: SqlDriver,
        val database: WakeveDb
    ) {
        fun seedDraft() {
            execute(
                """INSERT INTO event(
                    id, organizerId, title, description, status, deadline, createdAt, updatedAt,
                    aggregateRevision, aggregateSchemaVersion
                ) VALUES (
                    'event-1', 'organizer-1', 'Event', 'Description', 'DRAFT',
                    '2100-01-10T00:00:00Z', '2030-01-01T00:00:00Z', '2030-01-01T00:00:00Z',
                    4, 1
                )"""
            )
            execute(
                "INSERT INTO event_artwork(event_id, kind, updated_at) " +
                    "VALUES ('event-1', 'NONE', '2030-01-01T00:00:00Z')"
            )
        }

        fun execute(sql: String) {
            driver.execute(null, sql, 0).value
        }

        fun number(sql: String): Long? = driver.executeQuery(
            identifier = null,
            sql = sql,
            mapper = { cursor ->
                cursor.next()
                app.cash.sqldelight.db.QueryResult.Value(cursor.getLong(0))
            },
            parameters = 0
        ).value

        fun text(sql: String): String? = driver.executeQuery(
            identifier = null,
            sql = sql,
            mapper = { cursor ->
                cursor.next()
                app.cash.sqldelight.db.QueryResult.Value(cursor.getString(0))
            },
            parameters = 0
        ).value
    }
}
