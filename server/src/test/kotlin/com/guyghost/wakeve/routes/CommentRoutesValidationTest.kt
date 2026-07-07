package com.guyghost.wakeve.routes

import kotlin.test.Test
import kotlin.test.assertEquals

class CommentRoutesValidationTest {
    @Test
    fun parseCommentListLimit_defaultsMissingOrInvalidLimit() {
        assertEquals(50, parseCommentListLimit(null))
        assertEquals(50, parseCommentListLimit("not-a-number"))
    }

    @Test
    fun parseCommentListLimit_acceptsTrimmedValidLimit() {
        assertEquals(25, parseCommentListLimit(" 25 "))
    }

    @Test
    fun parseCommentListLimit_clampsOutsideBounds() {
        assertEquals(1, parseCommentListLimit("0"))
        assertEquals(1, parseCommentListLimit("-10"))
        assertEquals(100, parseCommentListLimit("100000"))
    }

    @Test
    fun parseCommentContributorLimit_clampsToContributorMaximum() {
        assertEquals(10, parseCommentContributorLimit(null))
        assertEquals(1, parseCommentContributorLimit("0"))
        assertEquals(50, parseCommentContributorLimit("100000"))
    }

    @Test
    fun parseRecentCommentLimit_clampsToRecentMaximum() {
        assertEquals(20, parseRecentCommentLimit(null))
        assertEquals(1, parseRecentCommentLimit("-1"))
        assertEquals(100, parseRecentCommentLimit("100000"))
    }

    @Test
    fun parseCommentOffset_defaultsAndClampsOutsideBounds() {
        assertEquals(0, parseCommentOffset(null))
        assertEquals(0, parseCommentOffset("not-a-number"))
        assertEquals(0, parseCommentOffset("-10"))
        assertEquals(15, parseCommentOffset(" 15 "))
        assertEquals(10_000, parseCommentOffset("100000"))
    }

    @Test
    fun bindCommentAuthorToAuthenticatedUser_allowsMatchingAuthor() {
        val result = bindCommentAuthorToAuthenticatedUser(
            requestedAuthorId = " user-123 ",
            authenticatedUserId = "user-123"
        )

        assertEquals("user-123", result.getOrThrow())
    }

    @Test
    fun bindCommentAuthorToAuthenticatedUser_replacesBlankAuthorWithJwtUserId() {
        val result = bindCommentAuthorToAuthenticatedUser(
            requestedAuthorId = "  ",
            authenticatedUserId = " user-123 "
        )

        assertEquals("user-123", result.getOrThrow())
    }

    @Test
    fun bindCommentAuthorToAuthenticatedUser_rejectsMissingJwtUserId() {
        val result = bindCommentAuthorToAuthenticatedUser(
            requestedAuthorId = "user-123",
            authenticatedUserId = null
        )

        kotlin.test.assertTrue(result.isFailure)
    }

    @Test
    fun bindCommentAuthorToAuthenticatedUser_rejectsMismatchedAuthor() {
        val result = bindCommentAuthorToAuthenticatedUser(
            requestedAuthorId = "victim-user",
            authenticatedUserId = "attacker-user"
        )

        kotlin.test.assertTrue(result.isFailure)
    }

    @Test
    fun resolveAuthenticatedCommentAuthorName_prefersJwtUserName() {
        assertEquals(
            "Trusted Name",
            resolveAuthenticatedCommentAuthorName(
                authenticatedUserName = " Trusted Name ",
                authenticatedEmail = "trusted@example.test",
                authenticatedUserId = "user-123"
            )
        )
    }

    @Test
    fun resolveAuthenticatedCommentAuthorName_fallsBackToEmailPrefix() {
        assertEquals(
            "trusted",
            resolveAuthenticatedCommentAuthorName(
                authenticatedUserName = "  ",
                authenticatedEmail = " trusted@example.test ",
                authenticatedUserId = "user-123"
            )
        )
    }

    @Test
    fun resolveAuthenticatedCommentAuthorName_fallsBackToUserId() {
        assertEquals(
            "user-123",
            resolveAuthenticatedCommentAuthorName(
                authenticatedUserName = null,
                authenticatedEmail = null,
                authenticatedUserId = "user-123"
            )
        )
    }
}
