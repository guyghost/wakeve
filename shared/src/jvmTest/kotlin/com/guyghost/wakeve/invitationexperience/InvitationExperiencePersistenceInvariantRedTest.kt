package com.guyghost.wakeve.invitationexperience

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventPlanningMode
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.Scenario
import com.guyghost.wakeve.models.ScenarioStatus
import com.guyghost.wakeve.repository.DatabaseEventRepository
import com.guyghost.wakeve.repository.ScenarioRepository
import com.guyghost.wakeve.repository.UserRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class InvitationExperiencePersistenceInvariantRedTest {

    @Test
    fun `artwork storage rejects every non-total union shape`() {
        val invalidArtworkRows = listOf(
            """INSERT INTO event_artwork(event_id, kind, preset_id, updated_at)
               VALUES ('event-1', 'NONE', 'party', '2030-01-01T00:00:00Z')""",
            """INSERT INTO event_artwork(event_id, kind, structured_version, updated_at)
               VALUES ('event-1', 'STRUCTURED', 1, '2030-01-01T00:00:00Z')""",
            """INSERT INTO event_artwork(event_id, kind, legacy_remote_url, updated_at)
               VALUES ('event-1', 'LEGACY_REMOTE', 'http://cdn.wakeve.app/a.jpg', '2030-01-01T00:00:00Z')""",
            """INSERT INTO event_artwork(
                   event_id, kind, legacy_remote_url, source_kind, preset_id, updated_at
               ) VALUES (
                   'event-1', 'LEGACY_REMOTE', 'https://cdn.wakeve.app/a.jpg',
                   'PRESET', 'party', '2030-01-01T00:00:00Z'
               )""",
            """INSERT INTO event_artwork(
                   event_id, kind, structured_version, source_kind, preset_id,
                   alt_kind, alt_text, focal_x, focal_y, crop, updated_at
               ) VALUES (
                   'event-1', 'STRUCTURED', 1, 'PRESET', 'party',
                   'INFORMATIVE', '', 0.5, 0.5, 'FILL', '2030-01-01T00:00:00Z'
               )"""
        )

        invalidArtworkRows.forEachIndexed { index, invalidInsert ->
            val driver = freshDriver()
            seedEvent(driver, "event-1")

            assertFails("invalid artwork union row #$index must be rejected") {
                driver.execute(null, invalidInsert, 0).value
            }
        }
    }

    @Test
    fun `migration backfills one total artwork value and sanitizes invalid legacy urls`() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(null, legacyEventSchema, 0).value
        seedLegacyEvent(driver, "valid", "https://cdn.wakeve.app/events/valid.jpg")
        seedLegacyEvent(driver, "missing", null)
        seedLegacyEvent(
            driver,
            "invalid",
            "https://user:password@evil.invalid/event.jpg?token=secret#private"
        )
        seedLegacyEvent(
            driver,
            "invalid-allowlisted",
            "https://cdn.wakeve.app/events/private.jpg?token=secret"
        )

        WakeveDb.Schema.migrate(driver, oldVersion = 8, newVersion = 9)

        assertEquals("LEGACY_REMOTE", scalarString(driver, "SELECT kind FROM event_artwork WHERE event_id = 'valid'"))
        assertEquals("NONE", scalarString(driver, "SELECT kind FROM event_artwork WHERE event_id = 'missing'"))
        assertEquals("NONE", scalarString(driver, "SELECT kind FROM event_artwork WHERE event_id = 'invalid'"))
        assertEquals(
            "NONE",
            scalarString(driver, "SELECT kind FROM event_artwork WHERE event_id = 'invalid-allowlisted'"),
            "An allowlisted host must not bypass URL credential/query/fragment validation."
        )
        assertEquals(
            4L,
            scalarLong(driver, "SELECT COUNT(*) FROM event_artwork"),
            "Every migrated event must have one persisted total artwork projection."
        )

        val diagnostic = scalarString(
            driver,
            "SELECT sanitized_detail FROM event_artwork_migration_issue WHERE event_id = 'invalid'"
        ).orEmpty()
        assertEquals(false, diagnostic.contains("password"), "Migration issues must not retain credentials.")
        assertEquals(false, diagnostic.contains("secret"), "Migration issues must not retain secret queries.")
        assertEquals(false, diagnostic.contains("evil.invalid"), "Migration issues must not retain rejected URLs.")

        val allowlistedDiagnostic = scalarString(
            driver,
            "SELECT sanitized_detail FROM event_artwork_migration_issue WHERE event_id = 'invalid-allowlisted'"
        ).orEmpty()
        assertEquals(false, allowlistedDiagnostic.contains("secret"))
    }

    @Test
    fun `migration from the real previous head succeeds when event has no hero image column`() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(null, previousHeadEventSchemaWithoutHeroImageUrl, 0).value
        execute(
            driver,
            """INSERT INTO event(
                id, organizerId, title, description, status, deadline, createdAt, updatedAt
            ) VALUES (
                'previous-head', 'organizer-1', 'Existing event', 'Description', 'DRAFT',
                '2030-01-10T00:00:00Z', '2030-01-01T00:00:00Z', '2030-01-01T00:00:00Z'
            )"""
        )

        WakeveDb.Schema.migrate(driver, oldVersion = 8, newVersion = 9)

        assertEquals(
            "NONE",
            scalarString(driver, "SELECT kind FROM event_artwork WHERE event_id = 'previous-head'"),
            "The real previous HEAD has no heroImageUrl; migration must still create one total artwork row."
        )
        assertEquals(1L, scalarLong(driver, "SELECT aggregateRevision FROM event WHERE id = 'previous-head'"))
        assertEquals(1L, scalarLong(driver, "SELECT aggregateSchemaVersion FROM event WHERE id = 'previous-head'"))
    }

    @Test
    fun `deleting an event cascades through every event-owned invitation record`() {
        val driver = freshDriver()
        seedEvent(driver, "event-1")
        seedUser(driver, "viewer-1")
        execute(driver, "INSERT INTO event_artwork(event_id, kind, updated_at) VALUES ('event-1', 'NONE', '2030-01-01T00:00:00Z')")
        execute(driver, "INSERT INTO event_artwork_migration_issue(id, event_id, issue_code, created_at) VALUES ('issue-1', 'event-1', 'INVALID_LEGACY_URL', '2030-01-01T00:00:00Z')")
        execute(driver, "INSERT INTO event_operation_receipt(operation_id, event_id, actor_id, action, aggregate_revision, status, created_at, updated_at) VALUES ('operation-1', 'event-1', 'viewer-1', 'UPDATE_DRAFT_AGGREGATE', 2, 'PENDING_SYNC', '2030-01-01T00:00:00Z', '2030-01-01T00:00:00Z')")
        execute(driver, "INSERT INTO direct_invite_batch(batch_id, event_id, actor_id, operation_id, access_revision, status, created_at, updated_at) VALUES ('batch-1', 'event-1', 'viewer-1', 'operation-2', 4, 'PENDING_SYNC', '2030-01-01T00:00:00Z', '2030-01-01T00:00:00Z')")
        execute(driver, "INSERT INTO direct_invite_recipient_outcome(batch_id, recipient_key, status, updated_at) VALUES ('batch-1', 'hmac-v1-abcdef', 'FAILED', '2030-01-01T00:00:00Z')")
        execute(driver, "INSERT INTO event_notification_preference(event_id, user_id, preference, operation_id, sync_status, updated_at) VALUES ('event-1', 'viewer-1', 'ESSENTIAL_ONLY', 'operation-3', 'PENDING', '2030-01-01T00:00:00Z')")

        execute(driver, "DELETE FROM event WHERE id = 'event-1'")

        listOf(
            "event_artwork",
            "event_artwork_migration_issue",
            "event_operation_receipt",
            "direct_invite_batch",
            "direct_invite_recipient_outcome",
            "event_notification_preference"
        ).forEach { table ->
            assertEquals(0L, scalarLong(driver, "SELECT COUNT(*) FROM $table"), table)
        }
    }

    @Test
    fun `repository deletion explicitly clears invitation records when production foreign keys are disabled`() = runTest {
        val driver = freshDriver(enableForeignKeys = false)
        val database = WakeveDb(driver)
        seedEvent(driver, "event-1")
        seedUser(driver, "viewer-1")
        execute(driver, "INSERT INTO event_artwork(event_id, kind, updated_at) VALUES ('event-1', 'NONE', '2030-01-01T00:00:00Z')")
        execute(driver, "INSERT INTO event_artwork_migration_issue(id, event_id, issue_code, created_at) VALUES ('issue-1', 'event-1', 'INVALID_LEGACY_URL', '2030-01-01T00:00:00Z')")
        execute(driver, "INSERT INTO event_operation_receipt(operation_id, event_id, actor_id, action, aggregate_revision, status, created_at, updated_at) VALUES ('operation-1', 'event-1', 'viewer-1', 'UPDATE_DRAFT_AGGREGATE', 2, 'PENDING_SYNC', '2030-01-01T00:00:00Z', '2030-01-01T00:00:00Z')")
        execute(driver, "INSERT INTO direct_invite_batch(batch_id, event_id, actor_id, operation_id, access_revision, status, created_at, updated_at) VALUES ('batch-1', 'event-1', 'viewer-1', 'operation-2', 4, 'PENDING_SYNC', '2030-01-01T00:00:00Z', '2030-01-01T00:00:00Z')")
        execute(driver, "INSERT INTO direct_invite_recipient_outcome(batch_id, recipient_key, status, updated_at) VALUES ('batch-1', 'hmac-v1-abcdef', 'FAILED', '2030-01-01T00:00:00Z')")
        execute(driver, "INSERT INTO event_notification_preference(event_id, user_id, preference, operation_id, sync_status, updated_at) VALUES ('event-1', 'viewer-1', 'ESSENTIAL_ONLY', 'operation-3', 'PENDING', '2030-01-01T00:00:00Z')")
        execute(driver, "INSERT INTO syncMetadata(id, entityType, entityId, operation, timestamp, synced) VALUES ('sync-batch', 'direct_invite_batch', 'batch-1', 'CREATE', '2030-01-01T00:00:00Z', 0)")
        execute(driver, "INSERT INTO syncMetadata(id, entityType, entityId, operation, timestamp, synced) VALUES ('sync-preference', 'event_notification_preference', 'event-1:viewer-1', 'UPDATE', '2030-01-01T00:00:00Z', 0)")

        assertTrue(DatabaseEventRepository(database).deleteEvent("event-1").isSuccess)

        listOf(
            "event_artwork",
            "event_artwork_migration_issue",
            "event_operation_receipt",
            "direct_invite_batch",
            "direct_invite_recipient_outcome",
            "event_notification_preference"
        ).forEach { table ->
            assertEquals(
                0L,
                scalarLong(driver, "SELECT COUNT(*) FROM $table"),
                "$table must not depend on an unconfigured PRAGMA for privacy deletion"
            )
        }
        assertEquals(
            0L,
            scalarLong(
                driver,
                "SELECT COUNT(*) FROM syncMetadata WHERE id IN ('sync-batch', 'sync-preference')"
            ),
            "Repository deletion must clear pending invitation sync subjects even without FK support."
        )
    }

    @Test
    fun `legacy writer that cannot round trip a current commit is fenced without losing protected fields`() = runTest {
        val driver = freshDriver()
        val database = WakeveDb(driver)
        seedEvent(driver, "event-1")
        execute(driver, "UPDATE event SET aggregateRevision = 4, aggregateSchemaVersion = 1 WHERE id = 'event-1'")
        execute(driver, "INSERT INTO event_artwork(event_id, kind, structured_version, source_kind, preset_id, alt_kind, focal_x, focal_y, crop, updated_at) VALUES ('event-1', 'STRUCTURED', 1, 'PRESET', 'weekend', 'DECORATIVE', 0.5, 0.5, 'FILL', '2030-01-01T00:00:00Z')")
        execute(driver, "INSERT INTO event_operation_receipt(operation_id, event_id, actor_id, action, aggregate_revision, status, created_at, updated_at) VALUES ('operation-1', 'event-1', 'organizer-1', 'UPDATE_DRAFT_AGGREGATE', 4, 'COMMITTED', '2030-01-01T00:00:00Z', '2030-01-01T00:00:00Z')")

        val repository = DatabaseEventRepository(database)
        val snapshot = checkNotNull(repository.getEvent("event-1"))
        val result = repository.updateEvent(snapshot.copy(title = "Legacy-compatible title"))

        assertTrue(result.isFailure, "A legacy adapter without the current operation receipt must fail closed.")
        assertEquals("Event", scalarString(driver, "SELECT title FROM event WHERE id = 'event-1'"))
        assertEquals(4L, scalarLong(driver, "SELECT aggregateRevision FROM event WHERE id = 'event-1'"))
        assertEquals("STRUCTURED", scalarString(driver, "SELECT kind FROM event_artwork WHERE event_id = 'event-1'"))
        assertEquals(1L, scalarLong(driver, "SELECT COUNT(*) FROM event_operation_receipt WHERE event_id = 'event-1'"))
    }

    @Test
    fun `writer that cannot preserve the minimum aggregate schema is fenced before mutation`() {
        val driver = freshDriver()
        val database = WakeveDb(driver)
        seedEvent(driver, "event-1")
        execute(driver, "UPDATE event SET aggregateRevision = 9, aggregateSchemaVersion = 99 WHERE id = 'event-1'")

        legacyUpdate(database, "Lossy title")

        assertEquals("Event", scalarString(driver, "SELECT title FROM event WHERE id = 'event-1'"))
        assertEquals(9L, scalarLong(driver, "SELECT aggregateRevision FROM event WHERE id = 'event-1'"))
    }

    @Test
    fun `repository update reports failure and leaves every field unchanged when writer is fenced`() = runTest {
        val driver = freshDriver()
        val database = WakeveDb(driver)
        seedEvent(driver, "event-1")
        execute(driver, "UPDATE event SET aggregateRevision = 9, aggregateSchemaVersion = 99 WHERE id = 'event-1'")
        val repository = DatabaseEventRepository(database)
        val stale = checkNotNull(repository.getEvent("event-1")).copy(
            title = "Must not be reported saved",
            planningMode = EventPlanningMode.SCENARIO_MATRIX
        )

        val result = repository.updateEvent(stale)

        assertTrue(result.isFailure, "A zero-row fenced write must never be reported as success.")
        assertEquals("Event", scalarString(driver, "SELECT title FROM event WHERE id = 'event-1'"))
        assertEquals("TIME_SLOT_POLL", scalarString(driver, "SELECT planningMode FROM event WHERE id = 'event-1'"))
        assertEquals(9L, scalarLong(driver, "SELECT aggregateRevision FROM event WHERE id = 'event-1'"))
    }

    @Test
    fun `legacy update API cannot overwrite a newer protected aggregate without caller held revision`() = runTest {
        val driver = freshDriver()
        val database = WakeveDb(driver)
        seedEvent(driver, "event-1")
        val repository = DatabaseEventRepository(database)
        val stale = checkNotNull(repository.getEvent("event-1")).copy(title = "Stale overwrite")
        execute(
            driver,
            "UPDATE event SET title = 'Authoritative revision', aggregateRevision = 5, " +
                "aggregateSchemaVersion = 1 WHERE id = 'event-1'"
        )
        execute(
            driver,
            "INSERT INTO event_operation_receipt(" +
                "operation_id, event_id, actor_id, action, aggregate_revision, status, created_at, updated_at" +
                ") VALUES (" +
                "'protected-operation', 'event-1', 'organizer-1', 'UPDATE_DRAFT_AGGREGATE', 5, " +
                "'COMMITTED', '2030-01-01T00:00:00Z', '2030-01-01T00:00:00Z'" +
                ")"
        )

        val result = repository.updateEvent(stale)

        assertTrue(
            result.isFailure,
            "A legacy API with no actor and expectedRevision cannot overwrite a newer protected commit."
        )
        assertEquals("Authoritative revision", scalarString(driver, "SELECT title FROM event WHERE id = 'event-1'"))
        assertEquals(5L, scalarLong(driver, "SELECT aggregateRevision FROM event WHERE id = 'event-1'"))
    }

    @Test
    fun `legacy save API cannot replace a newer protected aggregate or its child rows`() = runTest {
        val driver = freshDriver()
        val database = WakeveDb(driver)
        seedEvent(driver, "event-1")
        val repository = DatabaseEventRepository(database)
        val stale = checkNotNull(repository.getEvent("event-1")).copy(title = "Stale autosave")
        execute(
            driver,
            "UPDATE event SET title = 'Authoritative revision', aggregateRevision = 5, " +
                "aggregateSchemaVersion = 1 WHERE id = 'event-1'"
        )
        execute(
            driver,
            "INSERT INTO event_operation_receipt(" +
                "operation_id, event_id, actor_id, action, aggregate_revision, status, created_at, updated_at" +
                ") VALUES (" +
                "'protected-operation', 'event-1', 'organizer-1', 'UPDATE_DRAFT_AGGREGATE', 5, " +
                "'COMMITTED', '2030-01-01T00:00:00Z', '2030-01-01T00:00:00Z'" +
                ")"
        )
        execute(
            driver,
            "INSERT INTO timeSlot(id, eventId, startTime, endTime, timezone, createdAt, updatedAt) " +
                "VALUES ('authoritative-slot', 'event-1', '2099-01-01T10:00:00Z', " +
                "'2099-01-01T12:00:00Z', 'UTC', '2030-01-01T00:00:00Z', '2030-01-01T00:00:00Z')"
        )

        val result = repository.saveEvent(stale)

        assertTrue(result.isFailure)
        assertEquals("Authoritative revision", scalarString(driver, "SELECT title FROM event WHERE id = 'event-1'"))
        assertEquals(5L, scalarLong(driver, "SELECT aggregateRevision FROM event WHERE id = 'event-1'"))
        assertEquals(
            1L,
            scalarLong(driver, "SELECT COUNT(*) FROM timeSlot WHERE id = 'authoritative-slot'"),
            "A rejected stale save must not replace protected aggregate child rows."
        )
    }

    @Test
    fun `scenario writer rejects a stale child snapshot and advances the parent aggregate revision`() = runTest {
        val driver = freshDriver()
        val database = WakeveDb(driver)
        seedEvent(driver, "event-1")
        val repository = ScenarioRepository(database)
        val original = Scenario(
            id = "scenario-1",
            eventId = "event-1",
            name = "Original",
            dateOrPeriod = "2030-02-01/2030-02-02",
            location = "Annecy",
            duration = 2,
            estimatedParticipants = 8,
            estimatedBudgetPerPerson = 120.0,
            description = "Original description",
            status = ScenarioStatus.PROPOSED,
            createdAt = "2030-01-01T00:00:00Z",
            updatedAt = "2030-01-01T00:00:00Z"
        )
        assertTrue(repository.createScenario(original).isSuccess)
        val stale = checkNotNull(repository.getScenarioById("scenario-1"))
        val revisionBeforeUpdate = scalarLong(
            driver,
            "SELECT aggregateRevision FROM event WHERE id = 'event-1'"
        )

        val first = repository.updateScenario(stale.copy(name = "Authoritative update"))
        val staleRetry = repository.updateScenario(stale.copy(name = "Stale overwrite"))

        assertTrue(first.isSuccess)
        assertTrue(
            staleRetry.isFailure,
            "Scenario writes must compare the caller-held child/aggregate revision instead of silently overwriting a newer edit."
        )
        assertEquals("Authoritative update", repository.getScenarioById("scenario-1")?.name)
        assertEquals(
            checkNotNull(revisionBeforeUpdate) + 1,
            scalarLong(driver, "SELECT aggregateRevision FROM event WHERE id = 'event-1'"),
            "A successful scenario child mutation must advance the protected parent aggregate revision exactly once."
        )
    }

    @Test
    fun `legacy delete API cannot bypass historical owner guard`() = runTest {
        val driver = freshDriver()
        val database = WakeveDb(driver)
        seedEvent(driver, "historical-event")
        execute(driver, "UPDATE event SET status = 'FINALIZED', aggregateRevision = 5 WHERE id = 'historical-event'")
        execute(
            driver,
            "INSERT INTO timeSlot(id, eventId, startTime, endTime, timezone, createdAt, updatedAt) " +
                "VALUES ('past-slot', 'historical-event', '2000-01-01T10:00:00Z', " +
                "'2000-01-01T12:00:00Z', 'UTC', '1999-12-01T00:00:00Z', '1999-12-01T00:00:00Z')"
        )

        val result = DatabaseEventRepository(database).deleteEvent("historical-event")

        assertTrue(
            result.isFailure,
            "The unbound repository API must not let stale callers bypass DeleteEvent authorization/read-only guards."
        )
        assertEquals(1L, scalarLong(driver, "SELECT COUNT(*) FROM event WHERE id = 'historical-event'"))
        assertEquals(1L, scalarLong(driver, "SELECT COUNT(*) FROM timeSlot WHERE eventId = 'historical-event'"))
    }

    @Test
    fun `status writer is fenced before lifecycle mutation or sync side effect`() = runTest {
        val driver = freshDriver()
        val database = WakeveDb(driver)
        seedEvent(driver, "event-1")
        execute(driver, "UPDATE event SET aggregateRevision = 9, aggregateSchemaVersion = 99 WHERE id = 'event-1'")
        val repository = DatabaseEventRepository(database)

        val result = repository.updateEventStatus("event-1", EventStatus.POLLING, null)

        assertTrue(result.isFailure, "Lifecycle writers must obey the same aggregate writer fence.")
        assertEquals("DRAFT", scalarString(driver, "SELECT status FROM event WHERE id = 'event-1'"))
        assertEquals(9L, scalarLong(driver, "SELECT aggregateRevision FROM event WHERE id = 'event-1'"))
        assertEquals(
            0L,
            scalarLong(driver, "SELECT COUNT(*) FROM syncMetadata WHERE entityId = 'event-1'"),
            "A rejected writer must not enqueue a false-success sync operation."
        )
    }

    @Test
    fun `successful lifecycle writer advances aggregate revision exactly once`() = runTest {
        val driver = freshDriver()
        val database = WakeveDb(driver)
        seedEvent(driver, "event-1")
        execute(driver, "UPDATE event SET aggregateRevision = 4, aggregateSchemaVersion = 1 WHERE id = 'event-1'")

        val result = DatabaseEventRepository(database)
            .updateEventStatus("event-1", EventStatus.POLLING, null)

        assertTrue(result.isSuccess)
        assertEquals("POLLING", scalarString(driver, "SELECT status FROM event WHERE id = 'event-1'"))
        assertEquals(
            5L,
            scalarLong(driver, "SELECT aggregateRevision FROM event WHERE id = 'event-1'"),
            "Every preserving writer must advance the aggregate CAS revision exactly once."
        )
    }

    @Test
    fun `planning mode writer advances the aggregate revision and is fenced for newer schemas`() {
        val driver = freshDriver()
        val database = WakeveDb(driver)
        seedEvent(driver, "supported-event")
        seedEvent(driver, "fenced-event")
        execute(
            driver,
            "UPDATE event SET aggregateRevision = 9, aggregateSchemaVersion = 99 " +
                "WHERE id = 'fenced-event'"
        )

        database.eventQueries.updateEventPlanningMode(
            planningMode = "SCENARIO_MATRIX",
            updatedAt = "2030-01-02T00:00:00Z",
            id = "supported-event"
        )
        database.eventQueries.updateEventPlanningMode(
            planningMode = "SCENARIO_MATRIX",
            updatedAt = "2030-01-02T00:00:00Z",
            id = "fenced-event"
        )

        assertEquals(
            "SCENARIO_MATRIX",
            scalarString(driver, "SELECT planningMode FROM event WHERE id = 'supported-event'")
        )
        assertEquals(
            2L,
            scalarLong(driver, "SELECT aggregateRevision FROM event WHERE id = 'supported-event'"),
            "Planning mode is protected aggregate state and cannot change without advancing its revision."
        )
        assertEquals(
            "TIME_SLOT_POLL",
            scalarString(driver, "SELECT planningMode FROM event WHERE id = 'fenced-event'"),
            "A legacy planning-mode writer must be fenced before mutating a newer aggregate schema."
        )
        assertEquals(9L, scalarLong(driver, "SELECT aggregateRevision FROM event WHERE id = 'fenced-event'"))
    }

    @Test
    fun `saveEvent propagates a fenced update failure without replacing dependent rows`() = runTest {
        val driver = freshDriver()
        val database = WakeveDb(driver)
        seedEvent(driver, "event-1")
        execute(driver, "UPDATE event SET aggregateRevision = 9, aggregateSchemaVersion = 99 WHERE id = 'event-1'")
        val repository = DatabaseEventRepository(database)
        val stale = checkNotNull(repository.getEvent("event-1")).copy(title = "False auto-save")

        val result = repository.saveEvent(stale)

        assertTrue(result.isFailure, "saveEvent must propagate the owner rejection instead of returning a false success.")
        assertEquals("Event", scalarString(driver, "SELECT title FROM event WHERE id = 'event-1'"))
        assertEquals(9L, scalarLong(driver, "SELECT aggregateRevision FROM event WHERE id = 'event-1'"))
        assertEquals(0L, scalarLong(driver, "SELECT COUNT(*) FROM syncMetadata WHERE entityId = 'event-1'"))
    }

    @Test
    fun `repository create atomically installs one total NONE artwork`() = runTest {
        val driver = freshDriver()
        val repository = DatabaseEventRepository(WakeveDb(driver))

        val result = repository.createEvent(newEvent("created-with-artwork"))

        assertTrue(result.isSuccess)
        assertEquals(
            1L,
            scalarLong(driver, "SELECT COUNT(*) FROM event_artwork WHERE event_id = 'created-with-artwork'"),
            "Every created event aggregate must own exactly one total artwork row."
        )
        assertEquals(
            "NONE",
            scalarString(driver, "SELECT kind FROM event_artwork WHERE event_id = 'created-with-artwork'")
        )
    }

    @Test
    fun `sample event seeder installs exactly one total artwork row`() = runTest {
        val driver = freshDriver()
        val repository = DatabaseEventRepository(WakeveDb(driver))

        val first = repository.seedSampleEvent()
        val replay = repository.seedSampleEvent()
        val sampleId = com.guyghost.wakeve.sample.SampleEventFactory.SAMPLE_EVENT_ID

        assertTrue(first.isSuccess)
        assertTrue(replay.isSuccess, "Idempotent sample seeding must remain readable after the first install.")
        assertEquals(
            1L,
            scalarLong(driver, "SELECT COUNT(*) FROM event_artwork WHERE event_id = '$sampleId'"),
            "Every event entry point, including first-launch sample seeding, must install one total artwork projection."
        )
        assertEquals(
            "NONE",
            scalarString(driver, "SELECT kind FROM event_artwork WHERE event_id = '$sampleId'")
        )
    }

    @Test
    fun `getEvent reads validated legacy artwork from the aggregate instead of stale legacy field`() {
        val driver = freshDriver()
        seedEvent(driver, "event-1")
        execute(
            driver,
            "UPDATE event SET heroImageUrl = 'https://evil.invalid/stale.jpg' WHERE id = 'event-1'"
        )
        execute(
            driver,
            "INSERT INTO event_artwork(event_id, kind, legacy_remote_url, updated_at) " +
                "VALUES ('event-1', 'LEGACY_REMOTE', 'https://cdn.wakeve.app/events/validated.jpg', '2030-01-01T00:00:00Z')"
        )

        val loaded = DatabaseEventRepository(WakeveDb(driver)).getEvent("event-1")

        assertEquals(
            "https://cdn.wakeve.app/events/validated.jpg",
            loaded?.heroImageUrl,
            "Repository reads must consume the validated total artwork aggregate, never stale legacy source data."
        )
    }

    @Test
    fun `authenticated account erasure removes or anonymizes every invitation-experience user binding`() = runTest {
        val driver = freshDriver()
        val database = WakeveDb(driver)
        seedUser(driver, "actor-1")
        seedEvent(driver, "event-1")
        execute(driver, "UPDATE event SET organizerId = 'actor-1' WHERE id = 'event-1'")
        execute(
            driver,
            "INSERT INTO event_operation_receipt(operation_id, event_id, actor_id, action, aggregate_revision, status, created_at, updated_at) " +
                "VALUES ('operation-1', 'event-1', 'actor-1', 'UPDATE_DRAFT_AGGREGATE', 1, 'COMMITTED', '2030-01-01T00:00:00Z', '2030-01-01T00:00:00Z')"
        )
        execute(
            driver,
            "INSERT INTO direct_invite_batch(batch_id, event_id, actor_id, operation_id, access_revision, status, created_at, updated_at) " +
                "VALUES ('batch-1', 'event-1', 'actor-1', 'operation-2', 1, 'COMPLETED', '2030-01-01T00:00:00Z', '2030-01-01T00:00:00Z')"
        )
        execute(
            driver,
            "INSERT INTO direct_invite_recipient_outcome(batch_id, recipient_key, status, expires_at, updated_at) " +
                "VALUES ('batch-1', 'hmac-v1-abcdef', 'DELIVERED', '2030-02-01T00:00:00Z', '2030-01-01T00:00:00Z')"
        )
        execute(
            driver,
            "INSERT INTO event_notification_preference(event_id, user_id, preference, operation_id, sync_status, updated_at) " +
                "VALUES ('event-1', 'actor-1', 'ESSENTIAL_ONLY', 'operation-3', 'SYNCED', '2030-01-01T00:00:00Z')"
        )
        execute(
            driver,
            "INSERT INTO syncMetadata(id, entityType, entityId, operation, timestamp, synced) " +
                "VALUES ('sync-batch', 'direct_invite_batch', 'batch-1', 'CREATE', '2030-01-01T00:00:00Z', 0)"
        )
        execute(
            driver,
            "INSERT INTO syncMetadata(id, entityType, entityId, operation, timestamp, synced) " +
                "VALUES ('sync-preference', 'event_notification_preference', 'event-1:actor-1', 'UPDATE', '2030-01-01T00:00:00Z', 0)"
        )

        assertTrue(UserRepository(database).deleteUser("actor-1").isSuccess)

        assertEquals(1L, scalarLong(driver, "SELECT COUNT(*) FROM event WHERE id = 'event-1'"))
        assertTrue(
            checkNotNull(scalarString(driver, "SELECT organizerId FROM event WHERE id = 'event-1'"))
                .startsWith("deleted_user_")
        )
        assertEquals(
            2L,
            scalarLong(driver, "SELECT aggregateRevision FROM event WHERE id = 'event-1'"),
            "Account-erasure anonymization changes protected ownership and must advance the aggregate revision."
        )
        assertEquals(0L, scalarLong(driver, "SELECT COUNT(*) FROM event_notification_preference WHERE user_id = 'actor-1'"))
        assertEquals(0L, scalarLong(driver, "SELECT COUNT(*) FROM event_operation_receipt WHERE actor_id = 'actor-1'"))
        assertEquals(0L, scalarLong(driver, "SELECT COUNT(*) FROM direct_invite_batch WHERE actor_id = 'actor-1'"))
        assertEquals(
            0L,
            scalarLong(
                driver,
                "SELECT COUNT(*) FROM direct_invite_recipient_outcome WHERE batch_id = 'batch-1'"
            ),
            "Protected recipient identifiers owned by the erased actor must not become orphan PII."
        )
        assertEquals(
            0L,
            scalarLong(
                driver,
                "SELECT COUNT(*) FROM syncMetadata WHERE id IN ('sync-batch', 'sync-preference')"
            ),
            "Account erasure must remove pending sync subjects whose protected rows were erased."
        )
    }

    @Test
    fun `account erasure rolls back the complete anonymization and deletion envelope on failure`() = runTest {
        val driver = freshDriver()
        val database = WakeveDb(driver)
        seedUser(driver, "actor-1")
        seedEvent(driver, "event-1")
        execute(driver, "UPDATE event SET organizerId = 'actor-1' WHERE id = 'event-1'")
        execute(
            driver,
            "INSERT INTO event_operation_receipt(operation_id, event_id, actor_id, action, aggregate_revision, status, created_at, updated_at) " +
                "VALUES ('operation-1', 'event-1', 'actor-1', 'UPDATE_DRAFT_AGGREGATE', 1, 'COMMITTED', '2030-01-01T00:00:00Z', '2030-01-01T00:00:00Z')"
        )
        execute(
            driver,
            "INSERT INTO direct_invite_batch(batch_id, event_id, actor_id, operation_id, access_revision, status, created_at, updated_at) " +
                "VALUES ('batch-1', 'event-1', 'actor-1', 'operation-2', 1, 'COMPLETED', '2030-01-01T00:00:00Z', '2030-01-01T00:00:00Z')"
        )
        execute(
            driver,
            "CREATE TRIGGER abort_actor_erasure BEFORE DELETE ON user " +
                "WHEN OLD.id = 'actor-1' BEGIN SELECT RAISE(ABORT, 'forced erasure failure'); END"
        )

        assertTrue(UserRepository(database).deleteUser("actor-1").isFailure)

        assertEquals("actor-1", scalarString(driver, "SELECT organizerId FROM event WHERE id = 'event-1'"))
        assertEquals(
            1L,
            scalarLong(driver, "SELECT aggregateRevision FROM event WHERE id = 'event-1'"),
            "A failed erasure must roll back the ownership revision together with the organizer identity."
        )
        assertEquals(1L, scalarLong(driver, "SELECT COUNT(*) FROM user WHERE id = 'actor-1'"))
        assertEquals(1L, scalarLong(driver, "SELECT COUNT(*) FROM event_operation_receipt WHERE actor_id = 'actor-1'"))
        assertEquals(1L, scalarLong(driver, "SELECT COUNT(*) FROM direct_invite_batch WHERE actor_id = 'actor-1'"))
    }

    @Test
    fun `shared server artwork references are retained until the final owning event is deleted`() {
        val driver = freshDriver()
        seedEvent(driver, "event-1")
        seedEvent(driver, "event-2")
        listOf("event-1", "event-2").forEach { eventId ->
            execute(
                driver,
                "INSERT INTO event_artwork(event_id, kind, structured_version, source_kind, server_asset_id, canonical_https_url, asset_revision, alt_kind, focal_x, focal_y, crop, updated_at) " +
                    "VALUES ('$eventId', 'STRUCTURED', 1, 'SERVER_ASSET', 'asset-shared', 'https://cdn.wakeve.app/assets/shared.jpg', 4, 'DECORATIVE', 0.5, 0.5, 'FILL', '2030-01-01T00:00:00Z')"
            )
        }

        assertEquals(2L, serverAssetReferenceCount(driver, "asset-shared"))
        execute(driver, "DELETE FROM event WHERE id = 'event-1'")
        assertEquals(1L, serverAssetReferenceCount(driver, "asset-shared"))
        execute(driver, "DELETE FROM event WHERE id = 'event-2'")
        assertEquals(0L, serverAssetReferenceCount(driver, "asset-shared"))
    }

    @Test
    fun `failed event deletion rolls back artwork release and preserves shared asset reference count`() = runTest {
        val driver = freshDriver()
        val database = WakeveDb(driver)
        seedEvent(driver, "event-1")
        seedEvent(driver, "event-2")
        listOf("event-1", "event-2").forEach { eventId ->
            execute(
                driver,
                "INSERT INTO event_artwork(event_id, kind, structured_version, source_kind, server_asset_id, canonical_https_url, asset_revision, alt_kind, focal_x, focal_y, crop, updated_at) " +
                    "VALUES ('$eventId', 'STRUCTURED', 1, 'SERVER_ASSET', 'asset-shared', 'https://cdn.wakeve.app/assets/shared.jpg', 4, 'DECORATIVE', 0.5, 0.5, 'FILL', '2030-01-01T00:00:00Z')"
            )
        }
        execute(
            driver,
            "CREATE TRIGGER abort_event_deletion BEFORE DELETE ON event " +
                "WHEN OLD.id = 'event-1' BEGIN SELECT RAISE(ABORT, 'forced event deletion failure'); END"
        )

        assertTrue(DatabaseEventRepository(database).deleteEvent("event-1").isFailure)

        assertEquals(2L, serverAssetReferenceCount(driver, "asset-shared"))
        assertEquals(1L, scalarLong(driver, "SELECT COUNT(*) FROM event_artwork WHERE event_id = 'event-1'"))
        assertEquals(1L, scalarLong(driver, "SELECT COUNT(*) FROM event WHERE id = 'event-1'"))
    }

    @Test
    fun `recipient outcome storage rejects raw or reversibly normalized contact identifiers`() {
        val unsafeRecipientKeys = listOf(
            "alice@example.com",
            "33612345678",
            "alice-smith",
            "Alice Example",
            "recipient-alice-smith",
            "rk-alice"
        )

        unsafeRecipientKeys.forEachIndexed { index, unsafeKey ->
            val driver = freshDriver()
            seedEvent(driver, "event-1")
            execute(
                driver,
                "INSERT INTO direct_invite_batch(batch_id, event_id, actor_id, operation_id, access_revision, status, created_at, updated_at) " +
                    "VALUES ('batch-1', 'event-1', 'organizer-1', 'operation-1', 1, 'PENDING_SYNC', '2030-01-01T00:00:00Z', '2030-01-01T00:00:00Z')"
            )

            assertFails("unsafe recipient key #$index must be rejected before persistence") {
                execute(
                    driver,
                    "INSERT INTO direct_invite_recipient_outcome(batch_id, recipient_key, status, expires_at, updated_at) " +
                        "VALUES ('batch-1', '$unsafeKey', 'QUEUED_LOCAL', '2030-02-01T00:00:00Z', '2030-01-01T00:00:00Z')"
                )
            }
        }
    }

    @Test
    fun `retention purge removes only expired protected recipient data`() = runTest {
        val driver = freshDriver()
        val database = WakeveDb(driver)
        seedEvent(driver, "event-1")
        execute(
            driver,
            "INSERT INTO direct_invite_batch(batch_id, event_id, actor_id, operation_id, access_revision, status, created_at, updated_at, expires_at) " +
                "VALUES ('batch-1', 'event-1', 'organizer-1', 'operation-1', 1, 'COMPLETED', '2030-01-01T00:00:00Z', '2030-01-01T00:00:00Z', '2030-02-01T00:00:00Z')"
        )
        execute(
            driver,
            "INSERT INTO direct_invite_recipient_outcome(batch_id, recipient_key, status, expires_at, updated_at) " +
                "VALUES ('batch-1', 'hmac-v1-expired', 'DELIVERED', '2030-01-05T00:00:00Z', '2030-01-01T00:00:00Z')"
        )
        execute(
            driver,
            "INSERT INTO direct_invite_recipient_outcome(batch_id, recipient_key, status, expires_at, updated_at) " +
                "VALUES ('batch-1', 'hmac-v1-future', 'DELIVERED', '2030-02-01T00:00:00Z', '2030-01-01T00:00:00Z')"
        )
        execute(
            driver,
            "INSERT INTO direct_invite_delivery_envelope(batch_id, recipient_key, ciphertext, key_version, expires_at, transport_state) " +
                "VALUES ('batch-1', 'hmac-v1-expired', 'ciphertext-expired', 1, '2030-01-05T00:00:00Z', 'SERVER_ACCEPTED')"
        )
        execute(
            driver,
            "INSERT INTO direct_invite_delivery_envelope(batch_id, recipient_key, ciphertext, key_version, expires_at, transport_state) " +
                "VALUES ('batch-1', 'hmac-v1-future', 'ciphertext-future', 1, '2030-02-01T00:00:00Z', 'SERVER_ACCEPTED')"
        )
        execute(
            driver,
            "INSERT INTO direct_invite_batch(batch_id, event_id, actor_id, operation_id, access_revision, status, created_at, updated_at, expires_at) " +
                "VALUES ('batch-expired', 'event-1', 'organizer-1', 'operation-expired', 1, 'COMPLETED', '2030-01-01T00:00:00Z', '2030-01-01T00:00:00Z', '2030-01-05T00:00:00Z')"
        )
        execute(
            driver,
            "INSERT INTO direct_invite_recipient_outcome(batch_id, recipient_key, status, expires_at, updated_at) " +
                "VALUES ('batch-expired', 'hmac-v1-deadbeef', 'DELIVERED', '2030-01-05T00:00:00Z', '2030-01-01T00:00:00Z')"
        )
        execute(
            driver,
            "INSERT INTO direct_invite_delivery_envelope(batch_id, recipient_key, ciphertext, key_version, expires_at, transport_state) " +
                "VALUES ('batch-expired', 'hmac-v1-deadbeef', 'ciphertext-deadbeef', 1, '2030-01-05T00:00:00Z', 'SERVER_ACCEPTED')"
        )
        execute(
            driver,
            "INSERT INTO syncMetadata(id, entityType, entityId, operation, timestamp, synced) " +
                "VALUES ('sync-expired-batch', 'direct_invite_batch', 'batch-expired', 'CREATE', '2030-01-01T00:00:00Z', 1)"
        )

        assertEquals(
            2L,
            InvitationExperienceRetentionRepository(database)
                .purgeExpiredProtectedRecipientData("2030-01-10T00:00:00Z")
        )
        assertEquals(
            0L,
            scalarLong(
                driver,
                "SELECT COUNT(*) FROM direct_invite_recipient_outcome WHERE recipient_key = 'hmac-v1-expired'"
            )
        )
        assertEquals(
            1L,
            scalarLong(
                driver,
                "SELECT COUNT(*) FROM direct_invite_recipient_outcome WHERE recipient_key = 'hmac-v1-future'"
            )
        )
        assertEquals(
            0L,
            scalarLong(
                driver,
                "SELECT COUNT(*) FROM direct_invite_delivery_envelope WHERE recipient_key = 'hmac-v1-expired'"
            ),
            "Expiry must remove the separately encrypted delivery value together with its protected recipient outcome."
        )
        assertEquals(
            1L,
            scalarLong(
                driver,
                "SELECT COUNT(*) FROM direct_invite_delivery_envelope WHERE recipient_key = 'hmac-v1-future'"
            )
        )
        assertEquals(
            0L,
            scalarLong(
                driver,
                "SELECT COUNT(*) FROM direct_invite_delivery_envelope WHERE batch_id = 'batch-expired'"
            ),
            "No encrypted delivery value may outlive its expired owning batch."
        )
        assertEquals(1L, scalarLong(driver, "SELECT COUNT(*) FROM direct_invite_batch WHERE batch_id = 'batch-1'"))
        assertEquals(
            0L,
            scalarLong(driver, "SELECT COUNT(*) FROM direct_invite_batch WHERE batch_id = 'batch-expired'"),
            "A fully expired batch envelope must not survive after all of its protected keys are purged."
        )
        assertEquals(
            0L,
            scalarLong(driver, "SELECT COUNT(*) FROM syncMetadata WHERE id = 'sync-expired-batch'"),
            "Retention must remove the expired batch sync subject in the same envelope."
        )
        assertEquals(1L, scalarLong(driver, "SELECT COUNT(*) FROM event WHERE id = 'event-1'"))
    }

    private fun freshDriver(enableForeignKeys: Boolean = true): SqlDriver =
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { driver ->
            if (enableForeignKeys) {
                driver.execute(null, "PRAGMA foreign_keys = ON", 0).value
            }
            WakeveDb.Schema.create(driver)
        }

    private fun seedEvent(driver: SqlDriver, id: String) {
        execute(
            driver,
            """INSERT INTO event(
                id, organizerId, title, description, status, deadline, createdAt, updatedAt
            ) VALUES (
                '$id', 'organizer-1', 'Event', 'Description', 'DRAFT',
                '2030-01-10T00:00:00Z', '2030-01-01T00:00:00Z', '2030-01-01T00:00:00Z'
            )"""
        )
    }

    private fun seedUser(driver: SqlDriver, id: String) {
        execute(
            driver,
            """INSERT INTO user(
                id, provider_id, email, name, provider, created_at, updated_at
            ) VALUES (
                '$id', 'provider-$id', '$id@example.com', 'Viewer', 'apple',
                '2030-01-01T00:00:00Z', '2030-01-01T00:00:00Z'
            )"""
        )
    }

    private fun newEvent(id: String) = Event(
        id = id,
        title = "Created event",
        description = "Description",
        organizerId = "organizer-1",
        proposedSlots = emptyList(),
        deadline = "2030-01-10T00:00:00Z",
        status = EventStatus.DRAFT,
        createdAt = "2030-01-01T00:00:00Z",
        updatedAt = "2030-01-01T00:00:00Z"
    )

    private fun seedLegacyEvent(driver: SqlDriver, id: String, heroImageUrl: String?) {
        val quotedUrl = heroImageUrl?.replace("'", "''")?.let { "'$it'" } ?: "NULL"
        execute(
            driver,
            """INSERT INTO event(
                id, organizerId, title, description, status, deadline, createdAt, updatedAt, heroImageUrl
            ) VALUES (
                '$id', 'organizer-1', 'Event', 'Description', 'DRAFT',
                '2030-01-10T00:00:00Z', '2030-01-01T00:00:00Z', '2030-01-01T00:00:00Z', $quotedUrl
            )"""
        )
    }

    private fun legacyUpdate(database: WakeveDb, title: String) {
        database.eventQueries.updateEvent(
            title = title,
            description = "Description",
            status = "DRAFT",
            deadline = "2030-01-10T00:00:00Z",
            updatedAt = "2030-01-02T00:00:00Z",
            eventType = "OTHER",
            eventTypeCustom = null,
            minParticipants = null,
            maxParticipants = null,
            expectedParticipants = null,
            isSample = 0,
            id = "event-1"
        )
    }

    private fun execute(driver: SqlDriver, sql: String) {
        driver.execute(null, sql, 0).value
    }

    private fun scalarLong(driver: SqlDriver, sql: String): Long? =
        driver.executeQuery(
            identifier = null,
            sql = sql,
            mapper = { cursor ->
                cursor.next()
                app.cash.sqldelight.db.QueryResult.Value(cursor.getLong(0))
            },
            parameters = 0
        ).value

    private fun scalarString(driver: SqlDriver, sql: String): String? =
        driver.executeQuery(
            identifier = null,
            sql = sql,
            mapper = { cursor ->
                cursor.next()
                app.cash.sqldelight.db.QueryResult.Value(cursor.getString(0))
            },
            parameters = 0
        ).value

    private fun serverAssetReferenceCount(driver: SqlDriver, assetId: String): Long? =
        scalarLong(
            driver,
            "SELECT COUNT(*) FROM event_artwork WHERE source_kind = 'SERVER_ASSET' AND server_asset_id = '$assetId'"
        )

    private companion object {
        val previousHeadEventSchemaWithoutHeroImageUrl = """
            CREATE TABLE event (
                id TEXT PRIMARY KEY NOT NULL,
                organizerId TEXT NOT NULL,
                title TEXT NOT NULL,
                description TEXT NOT NULL,
                status TEXT NOT NULL,
                deadline TEXT NOT NULL,
                createdAt TEXT NOT NULL,
                updatedAt TEXT NOT NULL,
                version INTEGER NOT NULL DEFAULT 1,
                eventType TEXT DEFAULT 'OTHER',
                eventTypeCustom TEXT,
                minParticipants INTEGER,
                maxParticipants INTEGER,
                expectedParticipants INTEGER,
                isSample INTEGER NOT NULL DEFAULT 0,
                planningMode TEXT NOT NULL DEFAULT 'TIME_SLOT_POLL'
            )
        """.trimIndent()

        val legacyEventSchema = """
            CREATE TABLE event (
                id TEXT PRIMARY KEY NOT NULL,
                organizerId TEXT NOT NULL,
                title TEXT NOT NULL,
                description TEXT NOT NULL,
                status TEXT NOT NULL,
                deadline TEXT NOT NULL,
                createdAt TEXT NOT NULL,
                updatedAt TEXT NOT NULL,
                version INTEGER NOT NULL DEFAULT 1,
                eventType TEXT DEFAULT 'OTHER',
                eventTypeCustom TEXT,
                minParticipants INTEGER,
                maxParticipants INTEGER,
                expectedParticipants INTEGER,
                isSample INTEGER NOT NULL DEFAULT 0,
                planningMode TEXT NOT NULL DEFAULT 'TIME_SLOT_POLL',
                heroImageUrl TEXT
            )
        """.trimIndent()
    }
}
