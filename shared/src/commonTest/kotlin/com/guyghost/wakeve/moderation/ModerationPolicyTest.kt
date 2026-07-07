package com.guyghost.wakeve.moderation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ModerationPolicyTest {
    private val policy = ModerationPolicy()

    @Test
    fun `hard-policy user-generated content is rejected before publish`() {
        val result = policy.evaluate("This includes a credible threat against a participant")

        assertEquals(ModerationStatus.REJECTED, result.status)
        assertEquals("hard_policy_term", result.reasonCode)
        assertFalse(result.canPublish)
        assertFalse(result.shouldPersistHidden)
        assertEquals(ModerationPolicy.SAFE_REJECTION_MESSAGE, result.userMessage)
    }

    @Test
    fun `uncertain user-generated content is held pending review and hidden from regular users`() {
        val result = policy.evaluate("Let's move this to an off-platform payment flow")

        assertEquals(ModerationStatus.PENDING_REVIEW, result.status)
        assertEquals("needs_review", result.reasonCode)
        assertFalse(result.canPublish)
        assertTrue(result.shouldPersistHidden)
    }

    @Test
    fun `spam-like user-generated content is held pending review`() {
        val result = policy.evaluate("Limited offer free money via https://a.test and https://b.test")

        assertEquals(ModerationStatus.PENDING_REVIEW, result.status)
        assertEquals("needs_review", result.reasonCode)
    }

    @Test
    fun `ordinary event planning content is approved`() {
        val result = policy.evaluate("Train leaves at 18:30 and dinner starts near the station.")

        assertEquals(ModerationStatus.APPROVED, result.status)
        assertEquals("approved", result.reasonCode)
        assertTrue(result.canPublish)
        assertFalse(result.shouldPersistHidden)
    }

    @Test
    fun `content reports require stable reporter and target identifiers`() {
        val report = ContentReport(
            id = "report-1",
            reporterId = "user-1",
            targetType = ReportTarget.COMMENT,
            targetId = "comment-1",
            eventId = "event-1",
            reason = ReportReason.HARASSMENT,
            createdAt = "2026-06-13T10:00:00Z"
        )

        assertEquals(ReportReviewStatus.OPEN, report.status)
        assertEquals(ReportTarget.COMMENT, report.targetType)
        assertEquals("comment-1", report.targetId)
    }

    @Test
    fun `user block is owner scoped and cannot target the blocker`() {
        val block = UserBlock(
            id = "block-1",
            blockerUserId = "user-1",
            blockedUserId = "user-2",
            eventId = "event-1",
            reason = ReportReason.HARASSMENT,
            createdAt = "2026-06-13T10:00:00Z"
        )

        assertEquals("user-1", block.blockerUserId)
        assertEquals("user-2", block.blockedUserId)
        assertEquals("event-1", block.eventId)
    }
}
