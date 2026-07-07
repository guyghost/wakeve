package com.guyghost.wakeve.gamification

import com.guyghost.wakeve.models.BadgeNotification
import com.guyghost.wakeve.models.BadgeType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NoConfiguredBadgeNotificationServiceTest {

    @Test
    fun `sendBadgeNotification fails when badge notifications are not configured`() = runTest {
        val error = assertFailsWith<IllegalStateException> {
            NoConfiguredBadgeNotificationService.sendBadgeNotification(
                BadgeNotification(
                    id = "badge-notification-1",
                    type = BadgeType.EVENT_CREATED,
                    title = "Badge unlocked",
                    message = "You earned a badge"
                )
            )
        }

        assertEquals("Badge notification service is not configured", error.message)
    }

    @Test
    fun `points notification fails when badge notifications are not configured`() = runTest {
        val error = assertFailsWith<IllegalStateException> {
            NoConfiguredBadgeNotificationService.showPointsEarnedNotification(
                userId = "user-1",
                points = 10,
                action = "created an event"
            )
        }

        assertEquals("Badge notification service is not configured", error.message)
    }

    @Test
    fun `badge count update fails when badge notifications are not configured`() = runTest {
        val error = assertFailsWith<IllegalStateException> {
            NoConfiguredBadgeNotificationService.updateBadgeCount(3)
        }

        assertEquals("Badge notification service is not configured", error.message)
    }

    @Test
    fun `permission state fails when badge notifications are not configured`() = runTest {
        val requestError = assertFailsWith<IllegalStateException> {
            NoConfiguredBadgeNotificationService.requestNotificationPermission()
        }
        val enabledError = assertFailsWith<IllegalStateException> {
            NoConfiguredBadgeNotificationService.isNotificationEnabled()
        }

        assertEquals("Badge notification service is not configured", requestError.message)
        assertEquals("Badge notification service is not configured", enabledError.message)
    }
}
