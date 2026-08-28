package com.guyghost.wakeve.invitationexperience

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.repository.TimeSlotStorageIdentity
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DatabaseUpdateDraftAggregateUseCaseContractTest {

    @Test
    fun `NEW studio owner atomically creates event NONE or structured artwork receipt and pending sync`() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(null, "PRAGMA foreign_keys = ON", 0).value
        WakeveDb.Schema.create(driver)
        val useCase = DatabaseUpdateDraftAggregateUseCase(WakeveDb(driver))

        val result = useCase.execute(command(expectedRevision = 0))

        assertEquals(
            UpdateDraftAggregateResult.Committed(
                eventId = "event-1",
                committedRevision = 1,
                operationId = "operation-1",
                pendingSync = true
            ),
            result
        )
        assertEquals(1L, number(driver, "SELECT COUNT(*) FROM event WHERE id = 'event-1'"))
        assertEquals("STRUCTURED", text(driver, "SELECT kind FROM event_artwork WHERE event_id = 'event-1'"))
        assertEquals(1L, number(driver, "SELECT COUNT(*) FROM event_operation_receipt WHERE operation_id = 'operation-1'"))
        assertEquals(1L, number(driver, "SELECT COUNT(*) FROM syncMetadata WHERE entityId = 'event-1'"))
    }

    @Test
    fun `valid edit atomically commits fields slots artwork revision receipt and sync operation`() = runTest {
        val fixture = fixture()
        val command = command()

        assertEquals(
            UpdateDraftAggregateResult.Committed(
                eventId = "event-1",
                committedRevision = 5,
                operationId = "operation-1",
                pendingSync = true
            ),
            fixture.useCase.execute(command)
        )

        assertEquals("Updated event", text(fixture.driver, "SELECT title FROM event WHERE id = 'event-1'"))
        assertEquals(5L, number(fixture.driver, "SELECT aggregateRevision FROM event WHERE id = 'event-1'"))
        assertEquals(1L, number(fixture.driver, "SELECT COUNT(*) FROM timeSlot WHERE eventId = 'event-1'"))
        assertEquals(
            TimeSlotStorageIdentity.physicalId("event-1", "slot-new"),
            text(fixture.driver, "SELECT id FROM timeSlot WHERE eventId = 'event-1'")
        )
        assertEquals("STRUCTURED", text(fixture.driver, "SELECT kind FROM event_artwork WHERE event_id = 'event-1'"))
        assertEquals("weekend", text(fixture.driver, "SELECT preset_id FROM event_artwork WHERE event_id = 'event-1'"))
        assertEquals(
            1L,
            number(
                fixture.driver,
                "SELECT COUNT(*) FROM event_operation_receipt WHERE event_id = 'event-1' AND operation_id = 'operation-1' AND aggregate_revision = 5"
            )
        )
        assertEquals(
            1L,
            number(
                fixture.driver,
                "SELECT COUNT(*) FROM syncMetadata WHERE entityType = 'event' AND entityId = 'event-1' AND operation = 'UPDATE' AND synced = 0"
            )
        )
    }

    @Test
    fun `owner status and exact base revision guards reject before every aggregate write`() = runTest {
        val cases = listOf(
            GuardCase("wrong organizer", organizerId = "organizer-1", actorId = "other-user"),
            GuardCase("not draft", organizerId = "organizer-1", actorId = "organizer-1", status = EventStatus.POLLING),
            GuardCase("stale base", organizerId = "organizer-1", actorId = "organizer-1", expectedRevision = 3)
        )

        cases.forEach { case ->
            val fixture = fixture(
                organizerId = case.organizerId,
                status = case.status
            )
            assertIs<UpdateDraftAggregateResult.Rejected>(
                fixture.useCase.execute(
                    command(
                        actorId = case.actorId,
                        expectedRevision = case.expectedRevision
                    )
                ),
                case.name
            )
            assertUnchanged(fixture.driver, case.name)
        }
    }

    @Test
    fun `operation replay is idempotent and cannot be rebound to different content`() = runTest {
        val fixture = fixture()
        val original = command()
        val first = assertIs<UpdateDraftAggregateResult.Committed>(fixture.useCase.execute(original))

        assertEquals(first, fixture.useCase.execute(original))
        assertIs<UpdateDraftAggregateResult.Rejected>(
            fixture.useCase.execute(
                original.copy(eventDraft = original.eventDraft.copy(title = "Rebound content"))
            )
        )

        assertEquals("Updated event", text(fixture.driver, "SELECT title FROM event WHERE id = 'event-1'"))
        assertEquals(5L, number(fixture.driver, "SELECT aggregateRevision FROM event WHERE id = 'event-1'"))
        assertEquals(1L, number(fixture.driver, "SELECT COUNT(*) FROM event_operation_receipt WHERE operation_id = 'operation-1'"))
        assertEquals(1L, number(fixture.driver, "SELECT COUNT(*) FROM syncMetadata WHERE entityId = 'event-1'"))
    }

    @Test
    fun `invalid aggregate member cannot leave a partial event mutation`() = runTest {
        val fixture = fixture()
        val invalidArtwork = Artwork.Structured(
            version = 1,
            ref = ArtworkRef(
                source = ArtworkSource.Preset("weekend"),
                alt = ArtworkAlt.Decorative,
                focalPoint = ArtworkFocalPoint(x = 2.0, y = 0.5),
                crop = ArtworkCrop.FILL
            )
        )

        assertIs<UpdateDraftAggregateResult.Rejected>(
            fixture.useCase.execute(command().copy(artwork = invalidArtwork))
        )
        assertUnchanged(fixture.driver, "invalid artwork")
    }

    @Test
    fun `release one rejects unbound legacy arbitrary preset and server asset artwork`() = runTest {
        val unboundArtwork = listOf(
            "legacy remote" to Artwork.LegacyRemote(
                "https://cdn.wakeve.app/events/legacy.jpg"
            ),
            "arbitrary preset" to Artwork.Structured(
                version = 1,
                ref = ArtworkRef(
                    source = ArtworkSource.Preset("caller-controlled-preset"),
                    alt = ArtworkAlt.Decorative,
                    focalPoint = ArtworkFocalPoint(0.5, 0.5),
                    crop = ArtworkCrop.FILL
                )
            ),
            "unbound server asset" to Artwork.Structured(
                version = 1,
                ref = ArtworkRef(
                    source = ArtworkSource.ServerAsset(
                        assetId = "asset-not-authorized-by-owner",
                        canonicalHttpsUrl = "https://cdn.wakeve.app/assets/unbound.jpg",
                        assetRevision = 1
                    ),
                    alt = ArtworkAlt.Decorative,
                    focalPoint = ArtworkFocalPoint(0.5, 0.5),
                    crop = ArtworkCrop.FILL
                )
            )
        )

        unboundArtwork.forEach { (case, artwork) ->
            val fixture = fixture()

            assertIs<UpdateDraftAggregateResult.Rejected>(
                fixture.useCase.execute(command().copy(artwork = artwork)),
                case
            )
            assertUnchanged(fixture.driver, case)
        }
    }

    @Test
    fun `edit keep existing preserves the validated LEGACY_REMOTE artwork exactly`() = runTest {
        val fixture = fixture()
        execute(fixture.driver, "DELETE FROM event_artwork WHERE event_id = 'event-1'")
        execute(
            fixture.driver,
            "INSERT INTO event_artwork(event_id, kind, legacy_remote_url, updated_at) " +
                "VALUES ('event-1', 'LEGACY_REMOTE', 'https://cdn.wakeve.app/events/existing.jpg', '2030-01-01T00:00:00Z')"
        )

        val result = fixture.useCase.execute(
            command().copy(
                artwork = Artwork.LegacyRemote(
                    "https://cdn.wakeve.app/events/existing.jpg"
                )
            )
        )

        assertIs<UpdateDraftAggregateResult.Committed>(result)
        assertEquals("LEGACY_REMOTE", text(fixture.driver, "SELECT kind FROM event_artwork WHERE event_id = 'event-1'"))
        assertEquals(
            "https://cdn.wakeve.app/events/existing.jpg",
            text(fixture.driver, "SELECT legacy_remote_url FROM event_artwork WHERE event_id = 'event-1'")
        )
    }

    @Test
    fun `caller constructed server asset capability cannot forge repository ownership`() = runTest {
        val fixture = fixture()
        val source = ArtworkSource.ServerAsset(
            assetId = "asset-forged",
            canonicalHttpsUrl = "https://cdn.wakeve.app/assets/forged.jpg",
            assetRevision = 1
        )
        val forgedCapability = ArtworkSelectionCapability.Ready(
            actorId = "organizer-1",
            accessRevision = 4,
            authorizedAssetsByOpaqueId = mapOf("opaque-forged" to source)
        )

        val result = fixture.useCase.execute(
            command().copy(
                artwork = Artwork.Structured(
                    version = 1,
                    ref = ArtworkRef(
                        source = source,
                        alt = ArtworkAlt.Decorative,
                        focalPoint = ArtworkFocalPoint(0.5, 0.5),
                        crop = ArtworkCrop.FILL
                    )
                ),
                artworkCapability = forgedCapability
            )
        )

        assertIs<UpdateDraftAggregateResult.Rejected>(result)
        assertUnchanged(fixture.driver, "forged server-asset capability")
    }

    private fun fixture(
        organizerId: String = "organizer-1",
        status: EventStatus = EventStatus.DRAFT
    ): Fixture {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(null, "PRAGMA foreign_keys = ON", 0).value
        WakeveDb.Schema.create(driver)
        execute(
            driver,
            "INSERT INTO event(id, organizerId, title, description, status, deadline, createdAt, updatedAt, aggregateRevision, aggregateSchemaVersion) " +
                "VALUES ('event-1', '$organizerId', 'Original event', 'Original description', '${status.name}', '2030-01-20T00:00:00Z', '2030-01-01T00:00:00Z', '2030-01-01T00:00:00Z', 4, 1)"
        )
        execute(
            driver,
            "INSERT INTO event_artwork(event_id, kind, updated_at) VALUES ('event-1', 'NONE', '2030-01-01T00:00:00Z')"
        )
        val database = WakeveDb(driver)
        return Fixture(driver, DatabaseUpdateDraftAggregateUseCase(database))
    }

    private fun command(
        actorId: String = "organizer-1",
        expectedRevision: Long = 4
    ) = UpdateDraftAggregateCommand(
        eventId = "event-1",
        actorId = actorId,
        expectedBaseRevision = expectedRevision,
        eventDraft = StudioEventFields(
            title = "Updated event",
            description = "Updated description",
            deadline = "2030-02-20T00:00:00Z",
            proposedSlots = listOf(
                TimeSlot(
                    id = "slot-new",
                    start = "2030-02-21T10:00:00Z",
                    end = "2030-02-21T12:00:00Z",
                    timezone = "UTC"
                )
            )
        ),
        artwork = Artwork.Structured(
            version = 1,
            ref = ArtworkRef(
                source = ArtworkSource.Preset("weekend"),
                alt = ArtworkAlt.Decorative,
                focalPoint = ArtworkFocalPoint(0.5, 0.5),
                crop = ArtworkCrop.FILL
            )
        ),
        operationId = "operation-1"
    )

    private fun assertUnchanged(driver: SqlDriver, case: String) {
        assertEquals("Original event", text(driver, "SELECT title FROM event WHERE id = 'event-1'"), case)
        assertEquals(4L, number(driver, "SELECT aggregateRevision FROM event WHERE id = 'event-1'"), case)
        assertEquals("NONE", text(driver, "SELECT kind FROM event_artwork WHERE event_id = 'event-1'"), case)
        assertEquals(0L, number(driver, "SELECT COUNT(*) FROM timeSlot WHERE eventId = 'event-1'"), case)
        assertEquals(0L, number(driver, "SELECT COUNT(*) FROM event_operation_receipt WHERE event_id = 'event-1'"), case)
        assertEquals(0L, number(driver, "SELECT COUNT(*) FROM syncMetadata WHERE entityId = 'event-1'"), case)
    }

    private fun execute(driver: SqlDriver, sql: String) {
        driver.execute(null, sql, 0).value
    }

    private fun number(driver: SqlDriver, sql: String): Long? = driver.executeQuery(
        identifier = null,
        sql = sql,
        mapper = { cursor ->
            cursor.next()
            app.cash.sqldelight.db.QueryResult.Value(cursor.getLong(0))
        },
        parameters = 0
    ).value

    private fun text(driver: SqlDriver, sql: String): String? = driver.executeQuery(
        identifier = null,
        sql = sql,
        mapper = { cursor ->
            cursor.next()
            app.cash.sqldelight.db.QueryResult.Value(cursor.getString(0))
        },
        parameters = 0
    ).value

    private data class Fixture(
        val driver: SqlDriver,
        val useCase: DatabaseUpdateDraftAggregateUseCase
    )

    private data class GuardCase(
        val name: String,
        val organizerId: String,
        val actorId: String,
        val status: EventStatus = EventStatus.DRAFT,
        val expectedRevision: Long = 4
    )
}
