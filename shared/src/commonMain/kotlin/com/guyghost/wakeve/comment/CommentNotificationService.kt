package com.guyghost.wakeve.comment

import com.guyghost.wakeve.EventRepositoryInterface
import com.guyghost.wakeve.NotificationService
import com.guyghost.wakeve.models.Comment
import com.guyghost.wakeve.models.CommentSection
import com.guyghost.wakeve.models.NotificationMessage
import com.guyghost.wakeve.models.NotificationType
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.random.Random

/**
 * Service for managing comment-related notifications.
 *
 * Handles sending notifications when comments are posted or replies are made.
 */
class CommentNotificationService(
    private val notificationService: NotificationService,
    private val eventRepository: EventRepositoryInterface,
    private val commentRepository: CommentRepository
) {

    /**
     * Sends a notification when a user posts a comment on a section.
     *
     * Notifies all participants except the author about the new comment.
     *
     * @param eventId Event ID where the comment was posted
     * @param section Section where the comment was posted
     * @param sectionItemId Optional item ID within the section
     * @param authorId ID of the user who posted the comment
     * @param authorName Name of the user who posted the comment
     * @param content Content of the comment
     * @param commentId ID of the created comment
     * @param excludeRecipient ID to exclude from notifications (usually the author)
     */
    suspend fun notifyCommentPosted(
        eventId: String,
        section: CommentSection,
        sectionItemId: String?,
        authorId: String,
        authorName: String,
        content: String,
        commentId: String,
        excludeRecipient: String = authorId
    ) {
        // Get all participants for the event
        val participants = eventRepository.getParticipants(eventId)
            ?: throw IllegalArgumentException("Event not found: $eventId")

        // Filter out the recipient to exclude
        val recipients = participants.filter { it != excludeRecipient }

        // Send notification to all recipients concurrently
        coroutineScope {
            recipients.map { recipientId ->
                async {
                    val title = formatCommentPostedTitle(authorName, section)
                    val body = formatNotificationBody(content)

                    val notification = NotificationMessage(
                        id = generateNotificationId(),
                        userId = recipientId,
                        type = NotificationType.COMMENT_POSTED,
                        title = title,
                        body = body,
                        data = mapOf(
                            "eventId" to eventId,
                            "commentId" to commentId,
                            "section" to section.name,
                            "sectionItemId" to (sectionItemId ?: ""),
                            "authorId" to authorId,
                            "authorName" to authorName
                        )
                    )

                    notificationService.sendNotification(notification)
                }
            }.awaitAll()
        }
    }

    /**
     * Sends a notification when a user replies to a comment.
     *
     * Notifies only the author of the parent comment.
     *
     * @param eventId Event ID
     * @param parentComment Parent comment being replied to
     * @param replyComment The reply comment
     * @param excludeRecipient ID to exclude from notifications (usually the reply author)
     */
    suspend fun notifyCommentReply(
        eventId: String,
        parentComment: Comment,
        replyComment: Comment,
        excludeRecipient: String = replyComment.authorId
    ) {
        // Only notify parent comment author, unless it's the same as the reply author
        if (parentComment.authorId == excludeRecipient) {
            return // Don't notify the author about their own reply
        }

        val title = formatCommentReplyTitle(replyComment.authorName, parentComment.authorName)
        val body = formatNotificationBody(replyComment.content)

        val notification = NotificationMessage(
            id = generateNotificationId(),
            userId = parentComment.authorId,
            type = NotificationType.COMMENT_REPLY,
            title = title,
            body = body,
            data = mapOf(
                "eventId" to eventId,
                "commentId" to replyComment.id,
                "parentCommentId" to parentComment.id,
                "section" to replyComment.section.name,
                "sectionItemId" to (replyComment.sectionItemId ?: ""),
                "authorId" to replyComment.authorId,
                "authorName" to replyComment.authorName
            )
        )

        notificationService.sendNotification(notification)
    }

    /**
     * Sends a notification when a user is mentioned in a comment.
     *
     * Notifies each mentioned user.
     *
     * @param eventId Event ID
     * @param mentionedUserId User ID who was mentioned
     * @param authorId ID of the user who created the comment
     * @param authorName Name of the user who created the comment
     * @param content Content of the comment
     * @param commentId ID of the comment
     * @param excludeRecipient ID to exclude from notifications (usually the comment author)
     */
    suspend fun notifyMention(
        eventId: String,
        mentionedUserId: String,
        authorId: String,
        authorName: String,
        content: String,
        commentId: String,
        excludeRecipient: String = authorId
    ) {
        // Don't notify if the mentioned user is the comment author
        if (mentionedUserId == excludeRecipient) {
            return
        }

        val title = formatMentionTitle(authorName)
        val body = formatNotificationBody(content)

        val notification = NotificationMessage(
            id = generateNotificationId(),
            userId = mentionedUserId,
            type = NotificationType.MENTION,
            title = title,
            body = body,
            data = mapOf(
                "eventId" to eventId,
                "commentId" to commentId,
                "authorId" to authorId,
                "authorName" to authorName
            )
        )

        notificationService.sendNotification(notification)
    }

    /**
     * Formats the title for a comment posted notification.
     */
    internal fun formatCommentPostedTitle(authorName: String, section: CommentSection): String {
        return when (section) {
            CommentSection.GENERAL -> "$authorName a commenté l'événement"
            CommentSection.SCENARIO -> "$authorName a commenté un scénario"
            CommentSection.POLL -> "$authorName a commenté le sondage"
            CommentSection.TRANSPORT -> "$authorName a commenté le transport"
            CommentSection.ACCOMMODATION -> "$authorName a commenté l'hébergement"
            CommentSection.MEAL -> "$authorName a commenté un repas"
            CommentSection.EQUIPMENT -> "$authorName a commenté l'équipement"
            CommentSection.ACTIVITY -> "$authorName a commenté une activité"
            CommentSection.BUDGET -> "$authorName a commenté le budget"
        }
    }
    
    /**
     * Formats title for a comment reply notification.
     */
    internal fun formatCommentReplyTitle(authorName: String, parentAuthorName: String): String {
        return "$authorName a répondu à $parentAuthorName"
    }

    /**
     * Formats title for a mention notification.
     */
    internal fun formatMentionTitle(authorName: String): String {
        return "$authorName vous a mentionné"
    }
    
    /**
     * Formats the notification body by truncating content if necessary.
     */
    internal fun formatNotificationBody(content: String, maxLength: Int = 100): String {
        return if (content.length > maxLength) {
            content.substring(0, maxLength) + "..."
        } else {
            content
        }
    }

    /**
     * Generates a unique notification ID.
     */
    private fun generateNotificationId(): String {
        val chars = "0123456789abcdef"
        return buildString(36) {
            repeat(36) { i ->
                when (i) {
                    8, 13, 18, 23 -> append('-')
                    14 -> append('4') // UUID version 4
                    19 -> append(chars[Random.nextInt(4) + 8]) // 8, 9, a, or b
                    else -> append(chars[Random.nextInt(16)])
                }
            }
        }
    }
}
