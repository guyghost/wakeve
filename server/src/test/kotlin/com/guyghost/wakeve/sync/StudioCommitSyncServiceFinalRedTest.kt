package com.guyghost.wakeve.sync

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.invitationexperience.Artwork
import com.guyghost.wakeve.invitationexperience.ArtworkAlt
import com.guyghost.wakeve.invitationexperience.ArtworkCrop
import com.guyghost.wakeve.invitationexperience.ArtworkFocalPoint
import com.guyghost.wakeve.invitationexperience.ArtworkRef
import com.guyghost.wakeve.invitationexperience.ArtworkSelectionCapability
import com.guyghost.wakeve.invitationexperience.ArtworkSource
import com.guyghost.wakeve.invitationexperience.StudioCommitEnvelopeFactory
import com.guyghost.wakeve.invitationexperience.StudioEventFields
import com.guyghost.wakeve.invitationexperience.StudioPendingSyncSubject
import com.guyghost.wakeve.invitationexperience.UpdateDraftAggregateCommand
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.SyncChange
import com.guyghost.wakeve.models.SyncOperation
import com.guyghost.wakeve.models.SyncRequest
import com.guyghost.wakeve.repository.DatabaseEventRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StudioCommitSyncServiceFinalRedTest {

    @Test
    fun `server persists one Studio receipt and replay returns it without incrementing revision`() = runBlocking {
        val fixture = fixture()
        val request = request(command(), committedRevision = 1)

        val first = fixture.service.processSyncChanges(request, ACTOR_ID)
        val replay = SyncService(fixture.database).processSyncChanges(request, ACTOR_ID)

        assertEquals(1, first.appliedChanges, first.conflicts.toString())
        assertEquals(1, replay.appliedChanges, replay.conflicts.toString())
        assertEquals(1L, fixture.number("SELECT aggregateRevision FROM event WHERE id = '$EVENT_ID'"))
        assertEquals(
            1L,
            fixture.number("SELECT COUNT(*) FROM event_operation_receipt WHERE operation_id = '$OPERATION_ID'"),
            "The server receipt is the durable idempotency authority across SyncService restarts."
        )
        assertEquals(
            first.studioAcknowledgements.single().serverReceiptId,
            replay.studioAcknowledgements.single().serverReceiptId
        )
    }

    @Test
    fun `Studio edit rejects a stale base revision before mutating the aggregate`() = runBlocking {
        val fixture = fixture(existing = event())
        val before = fixture.number("SELECT aggregateRevision FROM event WHERE id = '$EVENT_ID'")
        val stale = command(expectedBaseRevision = 7, operationId = "studio-stale-edit")

        val response = fixture.service.processSyncChanges(request(stale, committedRevision = 8), ACTOR_ID)

        assertEquals(0, response.appliedChanges)
        assertEquals(1, response.conflicts.size)
        assertEquals(before, fixture.number("SELECT aggregateRevision FROM event WHERE id = '$EVENT_ID'"))
        assertEquals("Original server draft", fixture.text("SELECT title FROM event WHERE id = '$EVENT_ID'"))
    }

    @Test
    fun `Studio server enforces authenticated actor organizer and DRAFT status guards`() = runBlocking {
        data class GuardCase(
            val name: String,
            val existing: Event,
            val command: UpdateDraftAggregateCommand,
            val changeUserId: String = ACTOR_ID,
            val authenticatedUserId: String = ACTOR_ID
        )
        val cases = listOf(
            GuardCase("authenticated actor", event(), command(expectedBaseRevision = 1), authenticatedUserId = "intruder"),
            GuardCase("payload actor", event(), command(actorId = "intruder", expectedBaseRevision = 1)),
            GuardCase("organizer", event(organizerId = "another-owner"), command(expectedBaseRevision = 1)),
            GuardCase("DRAFT status", event(status = EventStatus.POLLING), command(expectedBaseRevision = 1))
        )

        cases.forEach { case ->
            val fixture = fixture(existing = case.existing)
            val response = fixture.service.processSyncChanges(
                request(
                    case.command,
                    committedRevision = 2,
                    changeUserId = case.changeUserId
                ),
                case.authenticatedUserId
            )
            assertEquals(0, response.appliedChanges, case.name)
            assertEquals(1, response.conflicts.size, case.name)
            assertEquals(case.existing.status.name, fixture.text("SELECT status FROM event WHERE id = '$EVENT_ID'"), case.name)
        }
    }

    @Test
    fun `Studio server atomically applies the canonical artwork`() = runBlocking {
        val fixture = fixture()
        val artwork = presetArtwork("weekend")

        val response = fixture.service.processSyncChanges(
            request(command(artwork = artwork), committedRevision = 1),
            ACTOR_ID
        )

        assertEquals(1, response.appliedChanges, response.conflicts.toString())
        assertEquals("STRUCTURED", fixture.text("SELECT kind FROM event_artwork WHERE event_id = '$EVENT_ID'"))
        assertEquals("weekend", fixture.text("SELECT preset_id FROM event_artwork WHERE event_id = '$EVENT_ID'"))
    }

    @Test
    fun `Studio replay compares artwork as part of the idempotent payload`() = runBlocking {
        val fixture = fixture()
        val first = request(command(artwork = Artwork.None), committedRevision = 1)
        val divergent = request(command(artwork = presetArtwork("different")), committedRevision = 1)

        assertEquals(1, fixture.service.processSyncChanges(first, ACTOR_ID).appliedChanges)
        val replay = SyncService(fixture.database).processSyncChanges(divergent, ACTOR_ID)

        assertEquals(0, replay.appliedChanges)
        assertEquals(1, replay.conflicts.size)
        assertTrue(replay.studioAcknowledgements.isEmpty())
    }

    @Test
    fun `Studio acknowledgement reports the actual persisted aggregate revision`() = runBlocking {
        val fixture = fixture(existing = event())
        val edit = command(expectedBaseRevision = 1, operationId = "studio-valid-edit")

        val response = fixture.service.processSyncChanges(request(edit, committedRevision = 2), ACTOR_ID)

        val actualRevision = assertNotNull(fixture.number("SELECT aggregateRevision FROM event WHERE id = '$EVENT_ID'"))
        val acknowledgement = assertNotNull(response.studioAcknowledgements.singleOrNull())
        assertEquals(actualRevision, acknowledgement.committedRevision)
    }

    @Test
    fun `Studio aggregate and final acknowledgement rollback together when final receipt persistence fails`() = runBlocking {
        val fixture = fixture()
        fixture.execute(
            """CREATE TRIGGER fail_studio_final_ack
                BEFORE UPDATE OF status, server_receipt_id ON event_operation_receipt
                WHEN NEW.server_receipt_id IS NOT NULL
                BEGIN
                    SELECT RAISE(ABORT, 'INJECTED_STUDIO_FINAL_ACK_FAILURE');
                END""".trimIndent()
        )

        val response = fixture.service.processSyncChanges(request(command(), committedRevision = 1), ACTOR_ID)

        assertEquals(0, response.appliedChanges)
        assertTrue(response.studioAcknowledgements.isEmpty())
        assertEquals(0L, fixture.number("SELECT COUNT(*) FROM event WHERE id = '$EVENT_ID'"))
        assertEquals(0L, fixture.number("SELECT COUNT(*) FROM event_artwork WHERE event_id = '$EVENT_ID'"))
        assertEquals(0L, fixture.number("SELECT COUNT(*) FROM event_operation_receipt WHERE operation_id = '$OPERATION_ID'"))
        assertEquals(0L, fixture.number("SELECT COUNT(*) FROM syncMetadata WHERE id = 'studio:$OPERATION_ID'"))
    }

    @Test
    fun `Studio replay returns a byte identical durable ACK before mutable status and revision guards`() = runBlocking {
        val fixture = fixture()
        val syncRequest = request(command(), committedRevision = 1)
        val first = fixture.service.processSyncChanges(syncRequest, ACTOR_ID)
        val firstAck = Json.encodeToString(first.studioAcknowledgements.single())
        fixture.execute(
            "UPDATE event SET status = 'POLLING', aggregateRevision = 99 WHERE id = '$EVENT_ID'"
        )

        val replay = SyncService(fixture.database).processSyncChanges(syncRequest, ACTOR_ID)

        assertEquals(1, replay.appliedChanges, replay.conflicts.toString())
        assertTrue(replay.conflicts.isEmpty())
        assertEquals(
            firstAck,
            Json.encodeToString(replay.studioAcknowledgements.single()),
            "The persisted ACK is immutable idempotency evidence; later aggregate state cannot rewrite or veto it."
        )
        assertEquals(99L, fixture.number("SELECT aggregateRevision FROM event WHERE id = '$EVENT_ID'"))
        assertEquals("POLLING", fixture.text("SELECT status FROM event WHERE id = '$EVENT_ID'"))
    }

    @Test
    fun `Studio durable ACK carries CREATED or UPDATED disposition and resulting artwork byte identically on replay`() = runBlocking {
        val createdFixture = fixture()
        val createRequest = request(command(artwork = Artwork.None), committedRevision = 1)
        val created = createdFixture.service.processSyncChanges(createRequest, ACTOR_ID)
        val createdReplay = SyncService(createdFixture.database).processSyncChanges(createRequest, ACTOR_ID)

        val updatedFixture = fixture(existing = event())
        val updateRequest = request(
            command(
                expectedBaseRevision = 1,
                operationId = "studio-updated-ack",
                artwork = Artwork.None
            ),
            committedRevision = 2
        )
        val updated = updatedFixture.service.processSyncChanges(updateRequest, ACTOR_ID)
        val updatedReplay = SyncService(updatedFixture.database).processSyncChanges(updateRequest, ACTOR_ID)

        val createdAck = created.studioAcknowledgements.single()
        val updatedAck = updated.studioAcknowledgements.single()
        val dispositionConstants = runCatching {
            Class.forName("com.guyghost.wakeve.invitationexperience.StudioCommitDisposition")
                .enumConstants
                .map(Any::toString)
                .toSet()
        }.getOrDefault(emptySet())
        val actual = mapOf<String, Any?>(
            "dispositionConstants" to dispositionConstants,
            "createdDisposition" to reflectedValue(createdAck, "Disposition")?.toString(),
            "updatedDisposition" to reflectedValue(updatedAck, "Disposition")?.toString(),
            "createdArtwork" to reflectedValue(createdAck, "Artwork"),
            "updatedArtwork" to reflectedValue(updatedAck, "Artwork"),
            "createdReplayByteIdentical" to (
                Json.encodeToString(createdAck) ==
                    Json.encodeToString(createdReplay.studioAcknowledgements.single())
                ),
            "updatedReplayByteIdentical" to (
                Json.encodeToString(updatedAck) ==
                    Json.encodeToString(updatedReplay.studioAcknowledgements.single())
                )
        )
        val expected = mapOf<String, Any?>(
            "dispositionConstants" to setOf("CREATED", "UPDATED"),
            "createdDisposition" to "CREATED",
            "updatedDisposition" to "UPDATED",
            "createdArtwork" to Artwork.None,
            "updatedArtwork" to Artwork.None,
            "createdReplayByteIdentical" to true,
            "updatedReplayByteIdentical" to true
        )

        assertEquals(expected, actual)
    }

    @Test
    fun `Studio FORBIDDEN rejection is typed nonretryable and correlated`() = runBlocking {
        val fixture = fixture(existing = event())
        val syncRequest = request(command(expectedBaseRevision = 1), committedRevision = 2)

        val response = fixture.service.processSyncChanges(syncRequest, "intruder")

        assertTypedNonRetryableConflict(response, syncRequest, "FORBIDDEN")
    }

    @Test
    fun `Studio EVENT_NOT_DRAFT rejection is typed nonretryable and correlated`() = runBlocking {
        val fixture = fixture(existing = event(status = EventStatus.POLLING))
        val syncRequest = request(command(expectedBaseRevision = 1), committedRevision = 2)

        val response = fixture.service.processSyncChanges(syncRequest, ACTOR_ID)

        assertTypedNonRetryableConflict(response, syncRequest, "EVENT_NOT_DRAFT")
    }

    @Test
    fun `Studio STALE_BASE_REVISION rejection is typed nonretryable and correlated`() = runBlocking {
        val fixture = fixture(existing = event())
        val syncRequest = request(
            command(expectedBaseRevision = 7, operationId = "studio-stale-typed"),
            committedRevision = 8
        )

        val response = fixture.service.processSyncChanges(syncRequest, ACTOR_ID)

        assertTypedNonRetryableConflict(response, syncRequest, "STALE_BASE_REVISION")
    }

    @Test
    fun `two server instances racing the same Studio operation converge on one byte identical artwork receipt`() = runBlocking {
        val databaseFile = Files.createTempFile("wakeve-studio-race-", ".sqlite")
        val jdbcUrl = "jdbc:sqlite:${databaseFile.toAbsolutePath()}"
        val firstDriver = JdbcSqliteDriver(jdbcUrl)
        val secondDriver = JdbcSqliteDriver(jdbcUrl)
        try {
            WakeveDb.Schema.create(firstDriver)
            val firstDatabase = WakeveDb(firstDriver)
            val secondDatabase = WakeveDb(secondDriver)
            val expectedArtwork = presetArtwork("weekend")
            val syncRequest = request(
                command(artwork = expectedArtwork),
                committedRevision = 1
            )
            val ready = AtomicInteger(0)
            val start = CompletableDeferred<Unit>()

            val responses = coroutineScope {
                listOf(firstDatabase, secondDatabase).map { database ->
                    async(Dispatchers.IO) {
                        ready.incrementAndGet()
                        start.await()
                        SyncService(database).processSyncChanges(syncRequest, ACTOR_ID)
                    }
                }.also {
                    while (ready.get() != 2) yield()
                    start.complete(Unit)
                }.awaitAll()
            }

            val acknowledgements = responses.map { response ->
                assertTrue(
                    response.conflicts.isEmpty(),
                    "A same-payload Studio race must converge on the winner receipt, never a generic conflict: ${response.conflicts}"
                )
                assertNotNull(
                    response.studioAcknowledgements.singleOrNull(),
                    "Every racing caller must receive the non-null durable winner acknowledgement."
                )
            }
            val encodedAcks = acknowledgements.map { Json.encodeToString(it) }
            assertEquals(encodedAcks.first(), encodedAcks.last())
            assertEquals(expectedArtwork, acknowledgements.first().artwork)

            val durableReceipt = assertNotNull(
                firstDatabase.invitationExperienceQueries
                    .selectOperationReceiptByOperationId(OPERATION_ID)
                    .executeAsOneOrNull()
            )
            val durableAckPayload = assertNotNull(durableReceipt.server_ack_payload)
            assertEquals(encodedAcks.first(), durableAckPayload)
            assertEquals(
                expectedArtwork,
                Json.decodeFromString(
                    com.guyghost.wakeve.invitationexperience.StudioSyncAck.serializer(),
                    durableAckPayload
                ).artwork
            )
            assertEquals(
                1L,
                firstDriver.executeQuery(
                    identifier = null,
                    sql = "SELECT COUNT(*) FROM event_operation_receipt WHERE operation_id = '$OPERATION_ID'",
                    mapper = { cursor ->
                        cursor.next()
                        app.cash.sqldelight.db.QueryResult.Value(cursor.getLong(0))
                    },
                    parameters = 0
                ).value
            )
        } finally {
            firstDriver.close()
            secondDriver.close()
            Files.deleteIfExists(databaseFile)
        }
    }

    private fun assertTypedNonRetryableConflict(
        response: com.guyghost.wakeve.models.SyncResponse,
        request: SyncRequest,
        expectedCode: String
    ) {
        assertEquals(0, response.appliedChanges)
        assertTrue(response.studioAcknowledgements.isEmpty())
        val change = request.changes.single()
        val conflict = response.conflicts.single()
        assertEquals(change.id, conflict.changeId)
        assertEquals(change.table, conflict.table)
        assertEquals(change.recordId, conflict.recordId)
        assertEquals(expectedCode, conflict.code)
        assertEquals(false, conflict.retryable)
    }

    private fun reflectedValue(instance: Any, suffix: String): Any? = instance.javaClass.methods
        .singleOrNull { it.name == "get$suffix" && it.parameterCount == 0 }
        ?.invoke(instance)

    private suspend fun fixture(existing: Event? = null): Fixture {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        WakeveDb.Schema.create(driver)
        val database = WakeveDb(driver)
        if (existing != null) DatabaseEventRepository(database).createEvent(existing).getOrThrow()
        return Fixture(driver, database, SyncService(database))
    }

    private fun request(
        command: UpdateDraftAggregateCommand,
        committedRevision: Long,
        changeUserId: String = command.actorId
    ): SyncRequest {
        val envelope = StudioCommitEnvelopeFactory.build(command)
        val subject = StudioPendingSyncSubject(
            eventId = command.eventId,
            committedRevision = committedRevision,
            localReceiptId = command.operationId,
            envelope = envelope
        )
        return SyncRequest(
            changes = listOf(
                SyncChange(
                    id = "sync:${command.operationId}",
                    table = "studio_commit",
                    operation = if (command.expectedBaseRevision == 0L) {
                        SyncOperation.CREATE.name
                    } else {
                        SyncOperation.UPDATE.name
                    },
                    recordId = command.operationId,
                    data = Json.encodeToString(subject),
                    timestamp = "2030-01-01T00:00:00Z",
                    userId = changeUserId
                )
            )
        )
    }

    private fun command(
        actorId: String = ACTOR_ID,
        expectedBaseRevision: Long = 0,
        operationId: String = OPERATION_ID,
        artwork: Artwork = Artwork.None
    ) = UpdateDraftAggregateCommand(
        eventId = EVENT_ID,
        actorId = actorId,
        expectedBaseRevision = expectedBaseRevision,
        eventDraft = StudioEventFields(
            title = if (expectedBaseRevision == 0L) "Created server draft" else "Edited server draft",
            description = "Canonical Studio server payload",
            deadline = "2099-01-01T00:00:00Z",
            eventType = EventType.OTHER
        ),
        artwork = artwork,
        operationId = operationId,
        artworkCapability = ArtworkSelectionCapability.Hidden,
        draftRevision = 1
    )

    private fun event(
        organizerId: String = ACTOR_ID,
        status: EventStatus = EventStatus.DRAFT
    ) = Event(
        id = EVENT_ID,
        title = "Original server draft",
        description = "Before Studio edit",
        organizerId = organizerId,
        proposedSlots = emptyList(),
        deadline = "2099-01-01T00:00:00Z",
        status = status,
        createdAt = "2030-01-01T00:00:00Z",
        updatedAt = "2030-01-01T00:00:00Z"
    )

    private fun presetArtwork(presetId: String) = Artwork.Structured(
        version = 1,
        ref = ArtworkRef(
            source = ArtworkSource.Preset(presetId),
            alt = ArtworkAlt.Decorative,
            focalPoint = ArtworkFocalPoint(0.5, 0.5),
            crop = ArtworkCrop.FILL
        )
    )

    private data class Fixture(
        val driver: JdbcSqliteDriver,
        val database: WakeveDb,
        val service: SyncService
    ) {
        fun execute(sql: String) {
            driver.execute(identifier = null, sql = sql, parameters = 0).value
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

    private companion object {
        const val EVENT_ID = "server-studio-event"
        const val ACTOR_ID = "server-studio-actor"
        const val OPERATION_ID = "server-studio-operation"
    }
}
