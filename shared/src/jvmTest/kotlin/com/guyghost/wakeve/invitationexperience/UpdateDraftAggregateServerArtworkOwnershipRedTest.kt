package com.guyghost.wakeve.invitationexperience

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.guyghost.wakeve.database.WakeveDb
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class UpdateDraftAggregateServerArtworkOwnershipRedTest {

    @Test
    fun `replacing shared server artwork releases old reference and binds new reference atomically`() = runTest {
        val fixture = fixture()
        fixture.seedServerArtwork("event-1", "asset-old", revision = 4)
        fixture.seedServerArtwork("event-2", "asset-old", revision = 4)
        fixture.bind("event-1", "asset-old", operationId = "bind-old-1")
        fixture.bind("event-2", "asset-old", operationId = "bind-old-2")

        val result = fixture.useCase.execute(fixture.serverAssetCommand("asset-new", operationId = "studio-1"))

        assertIs<UpdateDraftAggregateResult.Committed>(result)
        assertEquals("asset-new", fixture.text("SELECT server_asset_id FROM event_artwork WHERE event_id = 'event-1'"))
        assertEquals(
            1L,
            fixture.number(
                "SELECT COUNT(*) FROM server_artwork_reference " +
                    "WHERE asset_id = 'asset-old' AND asset_revision = 4"
            ),
            "The other event keeps the shared old asset alive."
        )
        assertEquals(
            1L,
            fixture.number(
                "SELECT COUNT(*) FROM server_artwork_reference " +
                    "WHERE event_id = 'event-1' AND asset_id = 'asset-new' AND asset_revision = 5"
            )
        )
        assertEquals(
            0L,
            fixture.number(
                "SELECT COUNT(*) FROM server_artwork_release_outbox " +
                    "WHERE asset_id = 'asset-old' AND asset_revision = 4"
            ),
            "A still-referenced asset must not be scheduled for final release."
        )
    }

    @Test
    fun `replacing final server artwork reference schedules exactly one release`() = runTest {
        val fixture = fixture()
        fixture.seedServerArtwork("event-1", "asset-old", revision = 4)
        fixture.bind("event-1", "asset-old", operationId = "bind-old")

        val command = fixture.serverAssetCommand("asset-new", operationId = "studio-final-release")
        assertIs<UpdateDraftAggregateResult.Committed>(fixture.useCase.execute(command))
        assertEquals(
            1L,
            fixture.number(
                "SELECT COUNT(*) FROM server_artwork_release_outbox " +
                    "WHERE asset_id = 'asset-old' AND asset_revision = 4"
            )
        )
        assertEquals(
            1L,
            fixture.number(
                "SELECT COUNT(*) FROM server_artwork_reference " +
                    "WHERE event_id = 'event-1' AND asset_id = 'asset-new'"
            )
        )

        assertEquals(
            fixture.useCase.execute(command),
            fixture.useCase.execute(command),
            "Operation replay must not duplicate either bind or final-release ownership effects."
        )
        assertEquals(
            1L,
            fixture.number(
                "SELECT COUNT(*) FROM server_artwork_release_outbox " +
                    "WHERE asset_id = 'asset-old' AND asset_revision = 4"
            )
        )
    }

    @Test
    fun `server artwork bind failure rolls back event artwork revision receipt sync and old reference`() = runTest {
        val fixture = fixture()
        fixture.seedServerArtwork("event-1", "asset-old", revision = 4)
        fixture.bind("event-1", "asset-old", operationId = "bind-old")
        fixture.execute(
            """CREATE TRIGGER force_server_artwork_bind_failure
                BEFORE INSERT ON server_artwork_reference
                WHEN NEW.asset_id = 'asset-new'
                BEGIN
                    SELECT RAISE(ABORT, 'forced server artwork bind failure');
                END"""
        )

        val result = fixture.useCase.execute(
            fixture.serverAssetCommand("asset-new", operationId = "studio-rollback")
        )

        val unknown = assertIs<UpdateDraftAggregateResult.OutcomeUnknown>(result)
        assertEquals(InvitationExperienceError.COMMIT_OUTCOME_UNKNOWN, unknown.error)
        assertEquals("Original event event-1", fixture.text("SELECT title FROM event WHERE id = 'event-1'"))
        assertEquals(4L, fixture.number("SELECT aggregateRevision FROM event WHERE id = 'event-1'"))
        assertEquals("asset-old", fixture.text("SELECT server_asset_id FROM event_artwork WHERE event_id = 'event-1'"))
        assertEquals(
            1L,
            fixture.number(
                "SELECT COUNT(*) FROM server_artwork_reference " +
                    "WHERE event_id = 'event-1' AND asset_id = 'asset-old'"
            )
        )
        assertEquals(0L, fixture.number("SELECT COUNT(*) FROM server_artwork_release_outbox"))
        assertEquals(0L, fixture.number("SELECT COUNT(*) FROM event_operation_receipt WHERE operation_id = 'studio-rollback'"))
        assertEquals(0L, fixture.number("SELECT COUNT(*) FROM syncMetadata WHERE id = 'studio:studio-rollback'"))
    }

    private fun fixture(): Fixture {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(null, "PRAGMA foreign_keys = ON", 0).value
        WakeveDb.Schema.create(driver)
        val database = WakeveDb(driver)
        return Fixture(driver, database, DatabaseUpdateDraftAggregateUseCase(database))
    }

    private class Fixture(
        private val driver: SqlDriver,
        val database: WakeveDb,
        val useCase: DatabaseUpdateDraftAggregateUseCase
    ) {
        fun seedServerArtwork(eventId: String, assetId: String, revision: Long) {
            execute(
                """INSERT INTO event(
                    id, organizerId, title, description, status, deadline, createdAt, updatedAt,
                    aggregateRevision, aggregateSchemaVersion
                ) VALUES (
                    '$eventId', 'organizer-1', 'Original event $eventId', 'Original description', 'DRAFT',
                    '2030-01-20T00:00:00Z', '2030-01-01T00:00:00Z', '2030-01-01T00:00:00Z',
                    $revision, 1
                )"""
            )
            execute(
                "INSERT INTO event_artwork(" +
                    "event_id, kind, structured_version, source_kind, server_asset_id, " +
                    "canonical_https_url, asset_revision, alt_kind, focal_x, focal_y, crop, updated_at" +
                    ") VALUES (" +
                    "'$eventId', 'STRUCTURED', 1, 'SERVER_ASSET', '$assetId', " +
                    "'https://cdn.wakeve.app/assets/$assetId.jpg', $revision, " +
                    "'DECORATIVE', 0.5, 0.5, 'FILL', '2030-01-01T00:00:00Z'" +
                    ")"
            )
        }

        suspend fun bind(eventId: String, assetId: String, operationId: String) {
            assertIs<ServerArtworkReferenceResult.Bound>(
                DatabaseServerArtworkReferenceOwner(database).bind(
                    ServerArtworkReference(eventId, assetId, 4),
                    operationId
                )
            )
        }

        fun serverAssetCommand(assetId: String, operationId: String): UpdateDraftAggregateCommand {
            val source = ArtworkSource.ServerAsset(
                assetId = assetId,
                canonicalHttpsUrl = "https://cdn.wakeve.app/assets/$assetId.jpg",
                assetRevision = 5
            )
            return UpdateDraftAggregateCommand(
                eventId = "event-1",
                actorId = "organizer-1",
                expectedBaseRevision = 4,
                eventDraft = StudioEventFields(
                    title = "Updated event",
                    description = "Updated description",
                    deadline = "2030-02-20T00:00:00Z"
                ),
                artwork = Artwork.Structured(
                    version = 1,
                    ref = ArtworkRef(
                        source = source,
                        alt = ArtworkAlt.Decorative,
                        focalPoint = ArtworkFocalPoint(0.5, 0.5),
                        crop = ArtworkCrop.FILL
                    )
                ),
                operationId = operationId,
                artworkCapability = ArtworkSelectionCapability.Ready(
                    actorId = "organizer-1",
                    accessRevision = 4,
                    authorizedAssetsByOpaqueId = mapOf(assetId to source)
                )
            )
        }

        fun execute(sql: String) {
            driver.execute(null, sql, 0).value
        }

        fun text(sql: String): String? = driver.executeQuery(
            identifier = null,
            sql = sql,
            mapper = { cursor ->
                cursor.next()
                QueryResult.Value(cursor.getString(0))
            },
            parameters = 0
        ).value

        fun number(sql: String): Long? = driver.executeQuery(
            identifier = null,
            sql = sql,
            mapper = { cursor ->
                cursor.next()
                QueryResult.Value(cursor.getLong(0))
            },
            parameters = 0
        ).value
    }
}
