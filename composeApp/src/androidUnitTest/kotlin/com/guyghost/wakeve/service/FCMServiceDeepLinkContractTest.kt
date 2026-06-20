package com.guyghost.wakeve.service

import androidx.core.app.NotificationCompat
import com.guyghost.wakeve.notification.NotificationChannelManager
import com.guyghost.wakeve.notification.NotificationType
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FCMServiceDeepLinkContractTest {
    @Test
    fun resolveFcmTokenRegistrationDecision_registersTrimmedTokenAndAccessToken() {
        val result = resolveFcmTokenRegistrationDecision(
            storedToken = "  fcm-token  ",
            accessToken = "  access-token  "
        )

        assertEquals(
            FcmTokenRegistrationDecision.Register(
                token = "fcm-token",
                accessToken = "access-token"
            ),
            result
        )
    }

    @Test
    fun resolveFcmTokenRegistrationDecision_rejectsMissingToken() {
        val result = resolveFcmTokenRegistrationDecision(
            storedToken = "  ",
            accessToken = "access-token"
        )

        assertEquals(FcmTokenRegistrationDecision.MissingToken, result)
    }

    @Test
    fun resolveFcmTokenRegistrationDecision_rejectsMissingAccessToken() {
        val result = resolveFcmTokenRegistrationDecision(
            storedToken = "fcm-token",
            accessToken = null
        )

        assertEquals(FcmTokenRegistrationDecision.MissingAccessToken, result)
    }

    @Test
    fun validateBackendHttpSuccess_accepts2xxResponses() {
        val result = validateBackendHttpSuccess(
            operation = "FCM token registration",
            status = HttpStatusCode.Created
        )

        assertTrue(result.isSuccess)
    }

    @Test
    fun validateBackendHttpSuccess_rejectsUnauthorizedResponseWithBody() {
        val result = validateBackendHttpSuccess(
            operation = "FCM token registration",
            status = HttpStatusCode.Unauthorized,
            responseBody = """{"error":"invalid token"}"""
        )

        assertTrue(result.isFailure)
        val message = result.exceptionOrNull()?.message.orEmpty()
        assertContains(message, "FCM token registration failed")
        assertContains(message, "401")
        assertContains(message, "invalid token")
    }

    @Test
    fun validateBackendHttpSuccess_rejectsServerErrorWithoutBody() {
        val result = validateBackendHttpSuccess(
            operation = "FCM token unregistration",
            status = HttpStatusCode.InternalServerError
        )

        assertTrue(result.isFailure)
        assertContains(result.exceptionOrNull()?.message.orEmpty(), "500")
    }

    @Test
    fun resolveNotificationType_defaultsUnknownTypeToEventUpdate() {
        val result = resolveNotificationType(mapOf("type" to "UNKNOWN_TYPE"))

        assertEquals(NotificationType.EVENT_UPDATE, result)
    }

    @Test
    fun resolveNotificationChannelId_routesInviteToHighPriorityChannel() {
        val result = resolveNotificationChannelId(mapOf("type" to "EVENT_INVITE"))

        assertEquals(NotificationChannelManager.Companion.ChannelId.HIGH_PRIORITY.id, result)
    }

    @Test
    fun resolveNotificationChannelId_routesVoteReminderToRemindersChannel() {
        val result = resolveNotificationChannelId(mapOf("type" to "VOTE_REMINDER"))

        assertEquals(NotificationChannelManager.Companion.ChannelId.REMINDERS.id, result)
    }

    @Test
    fun resolveNotificationPriority_mapsGenericUpdateToLowPriority() {
        val result = resolveNotificationPriority(mapOf("type" to "EVENT_UPDATE"))

        assertEquals(NotificationCompat.PRIORITY_LOW, result)
    }

    @Test
    fun resolveNotificationPriority_mapsMeetingReminderToMaxPriority() {
        val result = resolveNotificationPriority(mapOf("type" to "MEETING_REMINDER"))

        assertEquals(NotificationCompat.PRIORITY_MAX, result)
    }

    @Test
    fun resolveNotificationDeepLink_preservesSupportedDeepLink() {
        val result = resolveNotificationDeepLink(
            mapOf("deepLink" to "wakeve://invite/INVITE123")
        )

        assertEquals("wakeve://invite/INVITE123", result)
    }

    @Test
    fun resolveNotificationDeepLink_fallsBackFromUnsupportedPollLink() {
        val result = resolveNotificationDeepLink(
            mapOf(
                "type" to "VOTE_REMINDER",
                "eventId" to "event-123",
                "deepLink" to "wakeve://events/event-123/poll"
            )
        )

        assertEquals("wakeve://poll/event-123", result)
    }

    @Test
    fun resolveNotificationDeepLink_prefersInvitationCodeForInvites() {
        val result = resolveNotificationDeepLink(
            mapOf(
                "type" to "EVENT_INVITE",
                "eventId" to "event-123",
                "invitationCode" to "INVITE123"
            )
        )

        assertEquals("wakeve://invite/INVITE123", result)
    }

    @Test
    fun resolveNotificationDeepLink_routesMeetingNotificationToMeeting() {
        val result = resolveNotificationDeepLink(
            mapOf(
                "type" to "MEETING_REMINDER",
                "eventId" to "event-123",
                "meetingId" to "meeting-456"
            )
        )

        assertEquals("wakeve://meeting/meeting-456", result)
    }

    @Test
    fun resolveNotificationDeepLink_returnsNullWithoutDeepLinkOrContext() {
        val result = resolveNotificationDeepLink(
            mapOf("type" to "EVENT_UPDATE")
        )

        assertNull(result)
    }
}
