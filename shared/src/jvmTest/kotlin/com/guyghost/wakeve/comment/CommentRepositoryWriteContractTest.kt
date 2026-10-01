package com.guyghost.wakeve.comment

import com.guyghost.wakeve.createFreshTestDatabase
import com.guyghost.wakeve.models.CommentRequest
import com.guyghost.wakeve.models.CommentSection
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.moderation.ModerationRejectedException
import com.guyghost.wakeve.repository.DatabaseEventRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Contrat des écritures de commentaires appelées depuis iOS (refonte couche 5c, Swarm DAO #47).
 *
 * Swift ne peut rattraper une exception Kotlin que si la fonction la déclare (`@Throws`) : sans
 * déclaration, un refus de modération ou un parent introuvable arrête l'application.
 */
class CommentRepositoryWriteContractTest {

    private val eventId = "event-comments"

    @Test
    fun writesCalledFromIosDeclareTheirFailures() {
        val methods = CommentRepository::class.java.methods
        for (name in listOf("createComment", "updateComment")) {
            val method = methods.single { it.name == name }
            assertTrue(
                method.exceptionTypes.any { IllegalArgumentException::class.java.isAssignableFrom(it) },
                "$name must declare IllegalArgumentException for the iOS bridge"
            )
        }
    }

    @Test
    fun rejectedContentFailsWithAModerationException() = runTest {
        val repository = repositoryWithEvent()

        assertFailsWith<ModerationRejectedException> {
            repository.createComment(eventId, "user-1", "Léa", request("credible threat"))
        }
        val posted = repository.createComment(eventId, "user-1", "Léa", request("On part à 9 h"))
        assertFailsWith<ModerationRejectedException> {
            repository.updateComment(posted.id, "credible threat")
        }
    }

    @Test
    fun replyToAMissingCommentFailsWithAnIllegalArgument() = runTest {
        val repository = repositoryWithEvent()

        assertFailsWith<IllegalArgumentException> {
            repository.createComment(eventId, "user-1", "Léa", request("Moi aussi", parent = "missing"))
        }
    }

    @Test
    fun postReplyEditPinAndSoftDeleteStayInTheirSection() = runTest {
        val repository = repositoryWithEvent()

        val meal = repository.createComment(eventId, "user-1", "Léa", request("Je fais le dessert", CommentSection.MEAL))
        repository.createComment(eventId, "user-2", "Tom", request("Message général"))
        val reply = repository.createComment(
            eventId, "user-2", "Tom", request("Je prends le pain", CommentSection.MEAL, parent = meal.id)
        )

        assertEquals(listOf(meal.id), repository.getTopLevelComments(eventId, CommentSection.MEAL).map { it.id })
        assertEquals(listOf(reply.id), repository.getCommentThread(meal.id)?.replies?.map { it.id })

        assertEquals("Je fais la tarte", repository.updateComment(meal.id, "Je fais la tarte")?.content)
        assertTrue(repository.pinComment(meal.id)?.isPinned == true)
        assertTrue(repository.getCommentById(meal.id)?.isPinned == true)

        repository.softDeleteComment(reply.id)
        assertEquals(emptyList(), repository.getCommentThread(meal.id)?.replies?.map { it.id })
        repository.softDeleteComment(meal.id)
        assertEquals(emptyList(), repository.getTopLevelComments(eventId, CommentSection.MEAL).map { it.id })
    }

    private suspend fun repositoryWithEvent(): CommentRepository {
        val db = createFreshTestDatabase()
        DatabaseEventRepository(db).createEvent(
            Event(
                id = eventId,
                title = "Week-end",
                description = "Commentaires",
                organizerId = "user-1",
                participants = listOf("user-1", "user-2"),
                proposedSlots = emptyList(),
                deadline = "2026-11-30T23:59:59Z",
                status = EventStatus.ORGANIZING,
                createdAt = "2026-10-01T08:00:00Z",
                updatedAt = "2026-10-01T08:00:00Z"
            )
        )
        return CommentRepository(db)
    }

    private fun request(
        content: String,
        section: CommentSection = CommentSection.GENERAL,
        parent: String? = null
    ) = CommentRequest(section = section, content = content, parentCommentId = parent)
}
