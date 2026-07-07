package com.guyghost.wakeve.comment

import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.sync.PendingSyncSideEffectReplayer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Replays comment notification side effects that failed after comment persistence.
 */
class CommentNotificationRetryService(
    private val database: WakeveDb,
    private val commentRepository: CommentRepository,
    private val commentNotificationService: CommentNotificationService
) : PendingSyncSideEffectReplayer {
    override suspend fun hasPending(): Boolean {
        return database.syncMetadataQueries
            .selectPending()
            .executeAsList()
            .any { it.entityType == "comment_notification" && it.operation == "SEND" && it.retryState == "READY" }
    }

    override suspend fun replayPending(): Result<Int> {
        return try {
            var replayed = 0
            database.syncMetadataQueries
                .selectPending()
                .executeAsList()
                .filter { it.entityType == "comment_notification" && it.operation == "SEND" && it.retryState == "READY" }
                .forEach { pending ->
                    replayPayload(pending.payload)
                    database.syncMetadataQueries.markSynced(pending.id)
                    replayed += 1
                }
            Result.success(replayed)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun replayPayload(payload: String) {
        val data = Json.parseToJsonElement(payload).jsonObject
        val eventId = data.requiredString("eventId")
        val commentId = data.requiredString("commentId")
        val notificationType = data.requiredString("notificationType")
        val recipientId = data["recipientId"]?.jsonPrimitive?.content.orEmpty().ifBlank { null }
        val comment = commentRepository.getCommentById(commentId)
            ?: error("Comment not found for notification retry: $commentId")

        when (notificationType) {
            "COMMENT_POSTED" -> commentNotificationService.notifyCommentPosted(
                eventId = eventId,
                section = comment.section,
                sectionItemId = comment.sectionItemId,
                authorId = comment.authorId,
                authorName = comment.authorName,
                content = comment.content,
                commentId = comment.id,
                excludeRecipient = comment.authorId
            )
            "COMMENT_REPLY" -> {
                val parentCommentId = comment.parentCommentId
                    ?: error("Reply notification retry has no parent comment: $commentId")
                val parentComment = commentRepository.getCommentById(parentCommentId)
                    ?: error("Parent comment not found for notification retry: $parentCommentId")
                commentNotificationService.notifyCommentReply(
                    eventId = eventId,
                    parentComment = parentComment,
                    replyComment = comment,
                    excludeRecipient = comment.authorId
                )
            }
            "MENTION" -> {
                val mentionedUserId = recipientId
                    ?: error("Mention notification retry has no recipient: $commentId")
                commentNotificationService.notifyMention(
                    eventId = eventId,
                    mentionedUserId = mentionedUserId,
                    authorId = comment.authorId,
                    authorName = comment.authorName,
                    content = comment.content,
                    commentId = comment.id,
                    excludeRecipient = comment.authorId
                )
            }
            else -> error("Unsupported comment notification retry type: $notificationType")
        }
    }

    private fun Map<String, kotlinx.serialization.json.JsonElement>.requiredString(key: String): String =
        this[key]?.jsonPrimitive?.content ?: error("Missing '$key' in comment notification retry payload")
}
