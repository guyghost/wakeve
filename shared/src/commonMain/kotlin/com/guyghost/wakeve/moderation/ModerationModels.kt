package com.guyghost.wakeve.moderation

import kotlinx.serialization.Serializable

@Serializable
enum class ModerationStatus {
    APPROVED,
    PENDING_REVIEW,
    REJECTED,
    HIDDEN
}

@Serializable
enum class ReportTarget {
    COMMENT,
    CHAT_MESSAGE,
    EVENT,
    USER
}

@Serializable
enum class ReportReason {
    HARASSMENT,
    HATE_OR_ABUSE,
    SEXUAL_CONTENT,
    VIOLENCE_OR_THREAT,
    SPAM_OR_SCAM,
    PRIVATE_INFORMATION,
    OTHER
}

@Serializable
enum class ReportReviewStatus {
    OPEN,
    UNDER_REVIEW,
    RESOLVED,
    DISMISSED,
    ESCALATED
}

@Serializable
enum class ModerationDecisionAction {
    APPROVE,
    REJECT,
    HIDE,
    RESTORE,
    ESCALATE
}

@Serializable
enum class ModerationAuditOutcome {
    ACCEPTED,
    DENIED,
    FAILED
}

@Serializable
data class ContentReport(
    val id: String,
    val reporterId: String,
    val targetType: ReportTarget,
    val targetId: String,
    val eventId: String? = null,
    val reason: ReportReason,
    val details: String? = null,
    val status: ReportReviewStatus = ReportReviewStatus.OPEN,
    val createdAt: String,
    val reviewedAt: String? = null,
    val reviewerId: String? = null
) {
    init {
        require(id.isNotBlank()) { "Report id cannot be blank" }
        require(reporterId.isNotBlank()) { "Reporter id cannot be blank" }
        require(targetId.isNotBlank()) { "Target id cannot be blank" }
        require(details == null || details.length <= 1000) { "Report details cannot exceed 1000 characters" }
    }
}

@Serializable
data class UserBlock(
    val id: String,
    val blockerUserId: String,
    val blockedUserId: String,
    val eventId: String? = null,
    val reason: ReportReason? = null,
    val createdAt: String,
    val removedAt: String? = null
) {
    init {
        require(id.isNotBlank()) { "Block id cannot be blank" }
        require(blockerUserId.isNotBlank()) { "Blocker user id cannot be blank" }
        require(blockedUserId.isNotBlank()) { "Blocked user id cannot be blank" }
        require(blockerUserId != blockedUserId) { "A user cannot block themselves" }
    }
}

@Serializable
data class ModerationDecision(
    val id: String,
    val moderatorId: String,
    val reportId: String? = null,
    val targetType: ReportTarget,
    val targetId: String,
    val action: ModerationDecisionAction,
    val reason: String,
    val outcome: ModerationAuditOutcome,
    val createdAt: String
) {
    init {
        require(id.isNotBlank()) { "Moderation decision id cannot be blank" }
        require(moderatorId.isNotBlank()) { "Moderator id cannot be blank" }
        require(targetId.isNotBlank()) { "Target id cannot be blank" }
        require(reason.isNotBlank()) { "Moderation decision reason cannot be blank" }
        require(reason.length <= 1000) { "Moderation decision reason cannot exceed 1000 characters" }
    }
}

@Serializable
data class ModerationResult(
    val status: ModerationStatus,
    val reasonCode: String,
    val userMessage: String
) {
    val canPublish: Boolean
        get() = status == ModerationStatus.APPROVED

    val shouldPersistHidden: Boolean
        get() = status == ModerationStatus.PENDING_REVIEW
}

class ModerationRejectedException(
    val result: ModerationResult
) : IllegalArgumentException(result.userMessage)
