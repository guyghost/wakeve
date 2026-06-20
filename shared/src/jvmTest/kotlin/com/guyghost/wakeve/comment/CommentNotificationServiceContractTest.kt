package com.guyghost.wakeve.comment

import com.guyghost.wakeve.createFreshTestDatabase
import com.guyghost.wakeve.models.Comment
import com.guyghost.wakeve.models.CommentRequest
import com.guyghost.wakeve.models.CommentSection
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.NotificationMessage
import com.guyghost.wakeve.models.PushToken
import com.guyghost.wakeve.notification.NotificationServiceInterface
import com.guyghost.wakeve.repository.EventRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class CommentNotificationServiceContractTest {

    @Test
    fun notifyCommentPostedFailsWhenNotificationDeliveryFails() = runTest {
        val service = serviceWith(FailingNotificationService("comment notification failed"))

        val failure = assertFailsWith<IllegalStateException> {
            service.notifyCommentPosted(
                eventId = "event-1",
                section = CommentSection.GENERAL,
                sectionItemId = null,
                authorId = "author-1",
                authorName = "Alice",
                content = "New plan update",
                commentId = "comment-1"
            )
        }

        assertContains(failure.message.orEmpty(), "comment notification failed")
    }

    @Test
    fun notifyCommentReplyFailsWhenNotificationDeliveryFails() = runTest {
        val service = serviceWith(FailingNotificationService("reply notification failed"))
        val parentComment = comment(
            id = "comment-parent",
            authorId = "author-parent",
            authorName = "Parent Author",
            content = "Original question"
        )
        val replyComment = comment(
            id = "comment-reply",
            authorId = "author-reply",
            authorName = "Reply Author",
            content = "I can handle this",
            parentCommentId = parentComment.id
        )

        val failure = assertFailsWith<IllegalStateException> {
            service.notifyCommentReply(
                eventId = "event-1",
                parentComment = parentComment,
                replyComment = replyComment
            )
        }

        assertContains(failure.message.orEmpty(), "reply notification failed")
    }

    @Test
    fun notifyMentionFailsWhenNotificationDeliveryFails() = runTest {
        val service = serviceWith(FailingNotificationService("mention notification failed"))

        val failure = assertFailsWith<IllegalStateException> {
            service.notifyMention(
                eventId = "event-1",
                mentionedUserId = "mentioned-1",
                authorId = "author-1",
                authorName = "Alice",
                content = "@Bob can you check this?",
                commentId = "comment-1"
            )
        }

        assertContains(failure.message.orEmpty(), "mention notification failed")
    }

    @Test
    fun createCommentPersistsCommentAndQueuesNotificationRetryWhenDeliveryFails() = runTest {
        val database = createFreshTestDatabase()
        seedDatabaseEvent(database)
        val eventRepository = seededEventRepository()
        val notificationService = CommentNotificationService(
            notificationService = FailingNotificationService("comment delivery failed"),
            eventRepository = eventRepository,
            commentRepository = CommentRepository(database)
        )
        val repository = CommentRepository(
            db = database,
            commentNotificationService = notificationService,
            eventRepository = eventRepository
        )

        val comment = repository.createComment(
            eventId = "event-1",
            authorId = "author-1",
            authorName = "Alice",
            request = CommentRequest(
                section = CommentSection.GENERAL,
                content = "The plan changed"
            )
        )

        assertNotNull(repository.getCommentById(comment.id))
        val pendingRetry = database.syncMetadataQueries.selectByEntity("comment_notification", comment.id)
            .executeAsList()
            .single()
        assertEquals("SEND", pendingRetry.operation)
        assertEquals("READY", pendingRetry.retryState)
        assertEquals(0L, pendingRetry.retryCount)
        assertContains(pendingRetry.payload, "\"commentId\":\"${comment.id}\"")
        assertContains(pendingRetry.payload, "\"notificationType\":\"COMMENT_POSTED\"")
        assertContains(pendingRetry.payload, "comment delivery failed")
    }

    @Test
    fun commentNotificationRetryServiceReplaysPendingRetries() = runTest {
        val database = createFreshTestDatabase()
        seedDatabaseEvent(database)
        val eventRepository = seededEventRepository()
        val failingNotificationService = CommentNotificationService(
            notificationService = FailingNotificationService("comment delivery failed"),
            eventRepository = eventRepository,
            commentRepository = CommentRepository(database)
        )
        val repository = CommentRepository(
            db = database,
            commentNotificationService = failingNotificationService,
            eventRepository = eventRepository
        )
        val comment = repository.createComment(
            eventId = "event-1",
            authorId = "author-1",
            authorName = "Alice",
            request = CommentRequest(
                section = CommentSection.GENERAL,
                content = "The plan changed"
            )
        )
        val recordingNotificationService = RecordingNotificationService()
        val replayService = CommentNotificationRetryService(
            database = database,
            commentRepository = repository,
            commentNotificationService = CommentNotificationService(
                notificationService = recordingNotificationService,
                eventRepository = eventRepository,
                commentRepository = repository
            )
        )
        val pendingBeforeReplay = database.syncMetadataQueries.selectPending().executeAsList()
            .filter { it.entityType == "comment_notification" && it.entityId == comment.id }

        val result = replayService.replayPending()

        assertEquals(pendingBeforeReplay.size, result.getOrThrow())
        assertEquals(setOf("participant-1", "mentioned-1"), recordingNotificationService.sentMessages.map { it.userId }.toSet())
        assertEquals(
            emptyList(),
            database.syncMetadataQueries.selectPending().executeAsList()
                .filter { it.entityType == "comment_notification" && it.entityId == comment.id }
        )
    }

    private suspend fun serviceWith(notificationService: NotificationServiceInterface): CommentNotificationService {
        val eventRepository = seededEventRepository()

        return CommentNotificationService(
            notificationService = notificationService,
            eventRepository = eventRepository,
            commentRepository = CommentRepository(createFreshTestDatabase())
        )
    }

    private suspend fun seededEventRepository(): EventRepository {
        val eventRepository = EventRepository()
        eventRepository.createEvent(
            Event(
                id = "event-1",
                title = "Event",
                description = "Description",
                organizerId = "author-1",
                participants = listOf("author-1", "participant-1", "mentioned-1"),
                proposedSlots = emptyList(),
                deadline = "2026-01-01T00:00:00Z",
                status = EventStatus.ORGANIZING,
                createdAt = "2026-01-01T00:00:00Z",
                updatedAt = "2026-01-01T00:00:00Z"
            )
        ).getOrThrow()
        return eventRepository
    }

    private fun seedDatabaseEvent(database: com.guyghost.wakeve.database.WakeveDb) {
        database.eventQueries.insertEvent(
            id = "event-1",
            organizerId = "author-1",
            title = "Event",
            description = "Description",
            status = EventStatus.ORGANIZING.name,
            deadline = "2026-01-01T00:00:00Z",
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-01-01T00:00:00Z",
            version = 1,
            eventType = "OTHER",
            eventTypeCustom = null,
            minParticipants = null,
            maxParticipants = null,
            expectedParticipants = null,
            isSample = 0
        )
    }

    private fun comment(
        id: String,
        authorId: String,
        authorName: String,
        content: String,
        parentCommentId: String? = null
    ): Comment =
        Comment(
            id = id,
            eventId = "event-1",
            section = CommentSection.GENERAL,
            authorId = authorId,
            authorName = authorName,
            content = content,
            parentCommentId = parentCommentId,
            createdAt = "2026-01-01T00:00:00Z"
        )
}

private class FailingNotificationService(
    private val message: String
) : NotificationServiceInterface {
    override suspend fun sendNotification(message: NotificationMessage): Result<Unit> =
        Result.failure(IllegalStateException(this.message))

    override suspend fun registerPushToken(token: PushToken): Result<Unit> =
        Result.failure(IllegalStateException(message))

    override suspend fun unregisterPushToken(userId: String, deviceId: String): Result<Unit> =
        Result.failure(IllegalStateException(message))

    override suspend fun getUnreadNotifications(userId: String): List<NotificationMessage> = emptyList()

    override suspend fun markAsRead(notificationId: String): Result<Unit> =
        Result.failure(IllegalStateException(message))
}

private class RecordingNotificationService : NotificationServiceInterface {
    val sentMessages = mutableListOf<NotificationMessage>()

    override suspend fun sendNotification(message: NotificationMessage): Result<Unit> {
        sentMessages += message
        return Result.success(Unit)
    }

    override suspend fun registerPushToken(token: PushToken): Result<Unit> = Result.success(Unit)

    override suspend fun unregisterPushToken(userId: String, deviceId: String): Result<Unit> = Result.success(Unit)

    override suspend fun getUnreadNotifications(userId: String): List<NotificationMessage> =
        sentMessages.filter { it.userId == userId && it.readAt == null }

    override suspend fun markAsRead(notificationId: String): Result<Unit> = Result.success(Unit)
}
