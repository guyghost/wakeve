package com.guyghost.wakeve.comment

import com.guyghost.wakeve.createFreshTestDatabase
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.repository.DatabaseEventRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Requêtes du fil d'activité iOS (refonte couche 6, Swarm DAO #47).
 *
 * `selectParticipantActivity` regroupe par `(author_id, author_name)` : un auteur renommé produit
 * plusieurs lignes et `executeAsOneOrNull()` lève une exception non rattrapable côté Swift.
 * Les requêtes dédiées renvoient toujours une seule ligne et restent dans une section.
 */
class CommentActivityQueriesTest {

    private val eventId = "event-activity"

    @Test
    fun lastCommentAtIsOneRowEvenWhenTheAuthorWasRenamed() = runTest {
        val db = databaseWithEvent()
        insert(db, "c1", author = "user-1", name = "Léa", at = "2026-10-01T09:00:00.000Z")
        insert(db, "c2", author = "user-1", name = "Léa M.", at = "2026-10-01T10:00:00.000Z")
        insert(db, "c3", author = "user-1", name = "Léa", at = "2026-10-01T12:00:00.000Z", section = "MEAL")
        insert(db, "c4", author = "user-1", name = "Léa", at = "2026-10-01T13:00:00.000Z", deleted = true)

        val last = db.commentQueries
            .selectLastCommentAtByAuthorInSection(eventId, "user-1", "GENERAL")
            .executeAsOne()
            .lastCommentAt
        assertEquals("2026-10-01T10:00:00.000Z", last)

        val none = db.commentQueries
            .selectLastCommentAtByAuthorInSection(eventId, "user-2", "GENERAL")
            .executeAsOne()
            .lastCommentAt
        assertNull(none)
    }

    @Test
    fun recentActivityCountsOnlyVisibleCommentsOfTheSection() = runTest {
        val db = databaseWithEvent()
        insert(db, "old", author = "user-2", name = "Tom", at = "2026-10-01T08:00:00.000Z")
        insert(db, "new", author = "user-2", name = "Tom", at = "2026-10-01T11:00:00.000Z")
        insert(db, "renamed", author = "user-2", name = "Thomas", at = "2026-10-01T11:30:00.000Z")
        insert(db, "meal", author = "user-2", name = "Tom", at = "2026-10-01T11:00:00.000Z", section = "MEAL")
        insert(db, "deleted", author = "user-2", name = "Tom", at = "2026-10-01T11:00:00.000Z", deleted = true)
        insert(db, "pending", author = "user-2", name = "Tom", at = "2026-10-01T11:00:00.000Z", moderation = "PENDING")

        val count = db.commentQueries
            .countRecentActivityInSection(eventId, "GENERAL", "2026-10-01T09:00:00.000Z")
            .executeAsOne()
        assertEquals(2L, count)
    }

    private suspend fun databaseWithEvent(): WakeveDb {
        val db = createFreshTestDatabase()
        DatabaseEventRepository(db).createEvent(
            Event(
                id = eventId,
                title = "Week-end",
                description = "Activité",
                organizerId = "user-1",
                participants = listOf("user-1", "user-2"),
                proposedSlots = emptyList(),
                deadline = "2026-11-30T23:59:59Z",
                status = EventStatus.ORGANIZING,
                createdAt = "2026-10-01T08:00:00Z",
                updatedAt = "2026-10-01T08:00:00Z"
            )
        )
        return db
    }

    private fun insert(
        db: WakeveDb,
        id: String,
        author: String,
        name: String,
        at: String,
        section: String = "GENERAL",
        deleted: Boolean = false,
        moderation: String = "APPROVED"
    ) {
        db.commentQueries.insertComment(
            id = id,
            event_id = eventId,
            section = section,
            section_item_id = null,
            author_id = author,
            author_name = name,
            content = "Message $id",
            parent_comment_id = null,
            mentions = null,
            is_deleted = if (deleted) 1L else 0L,
            is_pinned = 0L,
            created_at = at,
            updated_at = null,
            is_edited = 0L,
            reply_count = 0L,
            moderation_status = moderation
        )
    }
}
