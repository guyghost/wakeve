package com.guyghost.wakeve.moderation

import com.guyghost.wakeve.Content_report
import com.guyghost.wakeve.Moderation_decision
import com.guyghost.wakeve.User_block
import com.guyghost.wakeve.database.WakeveDb
import kotlinx.datetime.Clock

class ModerationRepository(
    private val db: WakeveDb,
    private val clock: Clock = Clock.System
) {
    private val queries = db.moderationQueries

    fun createReport(
        id: String,
        reporterId: String,
        targetType: ReportTarget,
        targetId: String,
        eventId: String?,
        reason: ReportReason,
        details: String? = null
    ): ContentReport {
        val report = ContentReport(
            id = id,
            reporterId = reporterId,
            targetType = targetType,
            targetId = targetId,
            eventId = eventId,
            reason = reason,
            details = details,
            createdAt = now()
        )

        queries.insertContentReport(
            id = report.id,
            reporter_id = report.reporterId,
            target_type = report.targetType.name,
            target_id = report.targetId,
            event_id = report.eventId,
            reason = report.reason.name,
            details = report.details,
            status = report.status.name,
            created_at = report.createdAt,
            reviewed_at = report.reviewedAt,
            reviewer_id = report.reviewerId
        )

        return report
    }

    fun getReport(id: String): ContentReport? =
        queries.selectContentReportById(id).executeAsOneOrNull()?.toModel()

    fun getOpenReports(): List<ContentReport> =
        queries.selectOpenContentReports().executeAsList().map { it.toModel() }

    fun recordDecision(
        id: String,
        moderatorId: String,
        reportId: String?,
        targetType: ReportTarget,
        targetId: String,
        action: ModerationDecisionAction,
        reason: String,
        outcome: ModerationAuditOutcome
    ): ModerationDecision {
        val decision = ModerationDecision(
            id = id,
            moderatorId = moderatorId,
            reportId = reportId,
            targetType = targetType,
            targetId = targetId,
            action = action,
            reason = reason,
            outcome = outcome,
            createdAt = now()
        )

        queries.insertModerationDecision(
            id = decision.id,
            moderator_id = decision.moderatorId,
            report_id = decision.reportId,
            target_type = decision.targetType.name,
            target_id = decision.targetId,
            decision_action = decision.action.name,
            reason = decision.reason,
            outcome = decision.outcome.name,
            created_at = decision.createdAt
        )

        return decision
    }

    fun blockUser(
        id: String,
        blockerUserId: String,
        blockedUserId: String,
        eventId: String? = null,
        reason: ReportReason? = null
    ): UserBlock {
        val block = UserBlock(
            id = id,
            blockerUserId = blockerUserId,
            blockedUserId = blockedUserId,
            eventId = eventId,
            reason = reason,
            createdAt = now()
        )

        queries.insertUserBlock(
            id = block.id,
            blocker_user_id = block.blockerUserId,
            blocked_user_id = block.blockedUserId,
            event_id = block.eventId,
            reason = block.reason?.name,
            created_at = block.createdAt
        )

        return block
    }

    fun unblockUser(blockerUserId: String, blockedUserId: String, eventId: String? = null) {
        if (eventId == null) {
            queries.removeGlobalUserBlock(
                removed_at = now(),
                blocker_user_id = blockerUserId,
                blocked_user_id = blockedUserId
            )
        } else {
            queries.removeUserBlockForEvent(
                removed_at = now(),
                blocker_user_id = blockerUserId,
                blocked_user_id = blockedUserId,
                event_id = eventId
            )
        }
    }

    fun getActiveBlocks(blockerUserId: String): List<UserBlock> =
        queries.selectActiveUserBlocksForUser(blockerUserId).executeAsList().map { it.toModel() }

    fun isBlocked(blockerUserId: String, blockedUserId: String): Boolean =
        queries.isUserBlocked(blockerUserId, blockedUserId).executeAsOne() > 0

    private fun now(): String = clock.now().toString()
}

private fun Content_report.toModel(): ContentReport =
    ContentReport(
        id = id,
        reporterId = reporter_id,
        targetType = ReportTarget.valueOf(target_type),
        targetId = target_id,
        eventId = event_id,
        reason = ReportReason.valueOf(reason),
        details = details,
        status = ReportReviewStatus.valueOf(status),
        createdAt = created_at,
        reviewedAt = reviewed_at,
        reviewerId = reviewer_id
    )

private fun Moderation_decision.toModel(): ModerationDecision =
    ModerationDecision(
        id = id,
        moderatorId = moderator_id,
        reportId = report_id,
        targetType = ReportTarget.valueOf(target_type),
        targetId = target_id,
        action = ModerationDecisionAction.valueOf(decision_action),
        reason = reason,
        outcome = ModerationAuditOutcome.valueOf(outcome),
        createdAt = created_at
    )

private fun User_block.toModel(): UserBlock =
    UserBlock(
        id = id,
        blockerUserId = blocker_user_id,
        blockedUserId = blocked_user_id,
        eventId = event_id,
        reason = reason?.let { ReportReason.valueOf(it) },
        createdAt = created_at,
        removedAt = removed_at
    )
