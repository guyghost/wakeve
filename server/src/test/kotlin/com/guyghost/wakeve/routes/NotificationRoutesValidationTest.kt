package com.guyghost.wakeve.routes

import com.guyghost.wakeve.notification.Platform
import com.guyghost.wakeve.notification.defaultNotificationPreferences
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NotificationRoutesValidationTest {
    @Test
    fun validatePushToken_acceptsTrimmedToken() {
        val result = validatePushToken("  fcm-token  ")

        assertEquals("fcm-token", result.getOrThrow())
    }

    @Test
    fun validatePushToken_rejectsBlankToken() {
        val result = validatePushToken("  ")

        assertTrue(result.isFailure)
        assertContains(result.exceptionOrNull()?.message.orEmpty(), "must not be blank")
    }

    @Test
    fun parseNotificationPlatform_acceptsAndroidCaseInsensitively() {
        val result = parseNotificationPlatform("  AnDrOiD  ")

        assertEquals(Platform.ANDROID, result.getOrThrow())
    }

    @Test
    fun parseNotificationPlatform_acceptsIosCaseInsensitively() {
        val result = parseNotificationPlatform("  IOS  ")

        assertEquals(Platform.IOS, result.getOrThrow())
    }

    @Test
    fun parseNotificationPlatform_rejectsMissingPlatform() {
        val result = parseNotificationPlatform(null)

        assertTrue(result.isFailure)
        assertContains(result.exceptionOrNull()?.message.orEmpty(), "platform query parameter required")
    }

    @Test
    fun parseNotificationPlatform_rejectsUnsupportedPlatform() {
        val result = parseNotificationPlatform("web")

        assertTrue(result.isFailure)
        assertContains(result.exceptionOrNull()?.message.orEmpty(), "Invalid platform: web")
    }

    @Test
    fun bindPreferencesToAuthenticatedUser_allowsMatchingUserId() {
        val preferences = defaultNotificationPreferences("user-123")

        val result = bindPreferencesToAuthenticatedUser(
            preferences = preferences,
            authenticatedUserId = " user-123 "
        )

        assertEquals("user-123", result.getOrThrow().userId)
    }

    @Test
    fun bindPreferencesToAuthenticatedUser_replacesBlankBodyUserIdWithJwtUserId() {
        val preferences = defaultNotificationPreferences("").copy(userId = "  ")

        val result = bindPreferencesToAuthenticatedUser(
            preferences = preferences,
            authenticatedUserId = "user-123"
        )

        assertEquals("user-123", result.getOrThrow().userId)
    }

    @Test
    fun bindPreferencesToAuthenticatedUser_rejectsMismatchedBodyUserId() {
        val preferences = defaultNotificationPreferences("victim-user")

        val result = bindPreferencesToAuthenticatedUser(
            preferences = preferences,
            authenticatedUserId = "attacker-user"
        )

        assertTrue(result.isFailure)
        assertContains(result.exceptionOrNull()?.message.orEmpty(), "another user")
    }

    @Test
    fun bindPreferencesToAuthenticatedUser_rejectsBlankAuthenticatedUserId() {
        val preferences = defaultNotificationPreferences("user-123")

        val result = bindPreferencesToAuthenticatedUser(
            preferences = preferences,
            authenticatedUserId = "  "
        )

        assertTrue(result.isFailure)
        assertContains(result.exceptionOrNull()?.message.orEmpty(), "Missing userId")
    }

    @Test
    fun authorizeNotificationSend_allowsSelfTarget() {
        val result = authorizeNotificationSend(
            senderUserId = " user-123 ",
            targetUserId = "user-123",
            role = null,
            roles = emptyList(),
            permissions = emptyList()
        )

        assertTrue(result.isSuccess)
    }

    @Test
    fun authorizeNotificationSend_rejectsBlankTargetUserId() {
        val result = authorizeNotificationSend(
            senderUserId = "user-123",
            targetUserId = "  ",
            role = "ADMIN",
            roles = emptyList(),
            permissions = emptyList()
        )

        assertTrue(result.isFailure)
        assertContains(result.exceptionOrNull()?.message.orEmpty(), "target userId")
    }

    @Test
    fun authorizeNotificationSend_rejectsCrossUserWithoutPrivilege() {
        val result = authorizeNotificationSend(
            senderUserId = "sender-user",
            targetUserId = "target-user",
            role = "USER",
            roles = emptyList(),
            permissions = listOf("READ", "WRITE")
        )

        assertTrue(result.isFailure)
        assertContains(result.exceptionOrNull()?.message.orEmpty(), "another user")
    }

    @Test
    fun authorizeNotificationSend_allowsCrossUserForAdminRole() {
        val result = authorizeNotificationSend(
            senderUserId = "admin-user",
            targetUserId = "target-user",
            role = "admin",
            roles = emptyList(),
            permissions = emptyList()
        )

        assertTrue(result.isSuccess)
    }

    @Test
    fun authorizeNotificationSend_allowsCrossUserForNotificationPermission() {
        val result = authorizeNotificationSend(
            senderUserId = "service-user",
            targetUserId = "target-user",
            role = null,
            roles = emptyList(),
            permissions = listOf("notifications_send")
        )

        assertTrue(result.isSuccess)
    }

    @Test
    fun parseNotificationHistoryLimit_defaultsMissingOrInvalidLimit() {
        assertEquals(50, parseNotificationHistoryLimit(null))
        assertEquals(50, parseNotificationHistoryLimit("not-a-number"))
    }

    @Test
    fun parseNotificationHistoryLimit_acceptsTrimmedValidLimit() {
        assertEquals(25, parseNotificationHistoryLimit(" 25 "))
    }

    @Test
    fun parseNotificationHistoryLimit_clampsNonPositiveLimitToOne() {
        assertEquals(1, parseNotificationHistoryLimit("-1"))
        assertEquals(1, parseNotificationHistoryLimit("0"))
    }

    @Test
    fun parseNotificationHistoryLimit_clampsLargeLimitToMaximum() {
        assertEquals(100, parseNotificationHistoryLimit("100000"))
    }
}
