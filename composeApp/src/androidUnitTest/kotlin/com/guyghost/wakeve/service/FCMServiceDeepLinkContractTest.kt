package com.guyghost.wakeve.service

import androidx.core.app.NotificationCompat
import com.guyghost.wakeve.notification.NotificationChannelManager
import com.guyghost.wakeve.notification.NotificationType
import io.ktor.http.HttpStatusCode
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
    fun resolveFcmBackendApiBaseUrl_appendsApiPathToConfiguredServerUrl() {
        val result = resolveFcmBackendApiBaseUrl("https://api.wakeve.app")

        assertEquals("https://api.wakeve.app/api", result)
    }

    @Test
    fun resolveFcmBackendApiBaseUrl_trimsTrailingSlashBeforeApiPath() {
        val result = resolveFcmBackendApiBaseUrl("https://api.wakeve.app/")

        assertEquals("https://api.wakeve.app/api", result)
    }

    @Test
    fun resolveFcmBackendApiBaseUrl_keepsConfiguredApiPath() {
        val result = resolveFcmBackendApiBaseUrl("https://api.wakeve.app/api")

        assertEquals("https://api.wakeve.app/api", result)
    }

    @Test
    fun resolveFcmBackendApiBaseUrl_usesLocalDefaultWhenBlank() {
        val result = resolveFcmBackendApiBaseUrl("  ")

        assertEquals("https://api.wakeve.app/api", result)
    }

    @Test
    fun resolveFcmNotificationId_prefersTrimmedPayloadId() {
        val result = resolveFcmNotificationId(
            payloadNotificationId = " notification-123 ",
            messageId = "message-456",
            fallbackId = "fallback-789"
        )

        assertEquals("notification-123", result)
    }

    @Test
    fun resolveFcmNotificationId_fallsBackFromBlankPayloadToMessageId() {
        val result = resolveFcmNotificationId(
            payloadNotificationId = "  ",
            messageId = " message-456 ",
            fallbackId = "fallback-789"
        )

        assertEquals("message-456", result)
    }

    @Test
    fun resolveFcmNotificationId_fallsBackToGeneratedIdWhenPayloadAndMessageAreBlank() {
        val result = resolveFcmNotificationId(
            payloadNotificationId = null,
            messageId = " ",
            fallbackId = " fallback-789 "
        )

        assertEquals("fallback-789", result)
    }

    @Test
    fun resolveFcmNotificationId_boundsOversizedIds() {
        val result = resolveFcmNotificationId(
            payloadNotificationId = "N".repeat(180),
            messageId = "message-456",
            fallbackId = "fallback-789"
        )

        assertEquals(120, result.length)
        assertEquals("N".repeat(120), result)
    }

    @Test
    fun resolveFcmDisplayNotificationId_replacesLowValueEventNoisePerEventAndType() {
        val firstComment = resolveFcmDisplayNotificationId(
            notificationId = "comment-notification-1",
            notificationType = NotificationType.NEW_COMMENT,
            eventId = " event-123 "
        )
        val secondComment = resolveFcmDisplayNotificationId(
            notificationId = "comment-notification-2",
            notificationType = NotificationType.NEW_COMMENT,
            eventId = "event-123"
        )
        val eventUpdate = resolveFcmDisplayNotificationId(
            notificationId = "event-update-1",
            notificationType = NotificationType.EVENT_UPDATE,
            eventId = "event-123"
        )

        assertEquals(firstComment, secondComment)
        assertFalse(firstComment == eventUpdate)
    }

    @Test
    fun resolveFcmDisplayNotificationId_replacesEventNoiseUsingDeepLinkWhenEventIdIsMissing() {
        val firstComment = resolveFcmDisplayNotificationId(
            notificationId = "comment-notification-1",
            notificationType = NotificationType.NEW_COMMENT,
            eventId = null,
            resolvedDeepLink = "wakeve://event/event-123/details?tab=comments"
        )
        val secondComment = resolveFcmDisplayNotificationId(
            notificationId = "comment-notification-2",
            notificationType = NotificationType.NEW_COMMENT,
            eventId = null,
            resolvedDeepLink = "wakeve://event/event-123/details?tab=comments"
        )
        val otherEventComment = resolveFcmDisplayNotificationId(
            notificationId = "comment-notification-3",
            notificationType = NotificationType.NEW_COMMENT,
            eventId = null,
            resolvedDeepLink = "wakeve://event/event-456/details?tab=comments"
        )

        assertEquals(firstComment, secondComment)
        assertFalse(firstComment == otherEventComment)
    }

    @Test
    fun resolveFcmDisplayNotificationId_replacesPollRemindersUsingDeepLinkWhenEventIdIsMissing() {
        val firstReminder = resolveFcmDisplayNotificationId(
            notificationId = "vote-reminder-1",
            notificationType = NotificationType.VOTE_REMINDER,
            eventId = null,
            resolvedDeepLink = "wakeve://poll/event-123"
        )
        val secondReminder = resolveFcmDisplayNotificationId(
            notificationId = "vote-reminder-2",
            notificationType = NotificationType.VOTE_REMINDER,
            eventId = null,
            resolvedDeepLink = "wakeve://poll/event-123"
        )

        assertEquals(firstReminder, secondReminder)
    }

    @Test
    fun resolveFcmDisplayNotificationId_ignoresNonEventDeepLinksForReplacement() {
        val first = resolveFcmDisplayNotificationId(
            notificationId = "comment-notification-1",
            notificationType = NotificationType.NEW_COMMENT,
            eventId = null,
            resolvedDeepLink = "wakeve://notifications?filter=unread"
        )
        val second = resolveFcmDisplayNotificationId(
            notificationId = "comment-notification-2",
            notificationType = NotificationType.NEW_COMMENT,
            eventId = null,
            resolvedDeepLink = "wakeve://notifications?filter=unread"
        )

        assertFalse(first == second)
    }

    @Test
    fun resolveFcmDisplayNotificationId_replacesRepeatedRemindersPerEventAndType() {
        val firstReminder = resolveFcmDisplayNotificationId(
            notificationId = "vote-reminder-1",
            notificationType = NotificationType.VOTE_REMINDER,
            eventId = "event-123"
        )
        val secondReminder = resolveFcmDisplayNotificationId(
            notificationId = "vote-reminder-2",
            notificationType = NotificationType.VOTE_REMINDER,
            eventId = "event-123"
        )
        val deadlineReminder = resolveFcmDisplayNotificationId(
            notificationId = "deadline-reminder-1",
            notificationType = NotificationType.DEADLINE_REMINDER,
            eventId = "event-123"
        )

        assertEquals(firstReminder, secondReminder)
        assertFalse(firstReminder == deadlineReminder)
    }

    @Test
    fun resolveFcmDisplayNotificationId_keepsActionableNotificationsDistinct() {
        val firstInvite = resolveFcmDisplayNotificationId(
            notificationId = "invite-1",
            notificationType = NotificationType.EVENT_INVITE,
            eventId = "event-123"
        )
        val secondInvite = resolveFcmDisplayNotificationId(
            notificationId = "invite-2",
            notificationType = NotificationType.EVENT_INVITE,
            eventId = "event-123"
        )
        val firstMention = resolveFcmDisplayNotificationId(
            notificationId = "mention-1",
            notificationType = NotificationType.MENTION,
            eventId = "event-123"
        )
        val secondMention = resolveFcmDisplayNotificationId(
            notificationId = "mention-2",
            notificationType = NotificationType.MENTION,
            eventId = "event-123"
        )

        assertFalse(firstInvite == secondInvite)
        assertFalse(firstMention == secondMention)
    }

    @Test
    fun resolveFcmDisplayNotificationId_doesNotUseUnsafeEventIdsForReplacement() {
        val first = resolveFcmDisplayNotificationId(
            notificationId = "comment-notification-1",
            notificationType = NotificationType.NEW_COMMENT,
            eventId = "event/123"
        )
        val second = resolveFcmDisplayNotificationId(
            notificationId = "comment-notification-2",
            notificationType = NotificationType.NEW_COMMENT,
            eventId = "event/123"
        )

        assertFalse(first == second)
    }

    @Test
    fun resolveFcmNotificationText_prefersDataPayloadOverNotificationPayload() {
        val result = resolveFcmNotificationText(
            notificationType = NotificationType.EVENT_UPDATE,
            dataTitle = "Data title",
            notificationTitle = "Notification title",
            dataBody = "Data body",
            notificationBody = "Notification body"
        )

        assertEquals(FcmNotificationText("Data title", "Data body"), result)
    }

    @Test
    fun resolveFcmNotificationText_fallsBackForBlankPayloadText() {
        val result = resolveFcmNotificationText(
            notificationType = NotificationType.EVENT_INVITE,
            dataTitle = "   ",
            notificationTitle = null,
            dataBody = "\n\t ",
            notificationBody = null
        )

        assertEquals(FcmNotificationText("Invitation Wakeve", "Tu as une nouvelle invitation"), result)
    }

    @Test
    fun resolveFcmNotificationText_usesTypeSpecificFallbacksForActionableNotifications() {
        val meeting = resolveFcmNotificationText(
            notificationType = NotificationType.MEETING_REMINDER,
            dataTitle = null,
            notificationTitle = null,
            dataBody = null,
            notificationBody = null
        )
        val payment = resolveFcmNotificationText(
            notificationType = NotificationType.PAYMENT_DUE,
            dataTitle = null,
            notificationTitle = null,
            dataBody = null,
            notificationBody = null
        )
        val mention = resolveFcmNotificationText(
            notificationType = NotificationType.MENTION,
            dataTitle = null,
            notificationTitle = null,
            dataBody = null,
            notificationBody = null
        )

        assertEquals(FcmNotificationText("Reunion bientot", "Une reunion commence bientot"), meeting)
        assertEquals(FcmNotificationText("Paiement a regler", "Un paiement est en attente"), payment)
        assertEquals(FcmNotificationText("Tu as ete mentionne", "Quelqu'un attend ton attention"), mention)
    }

    @Test
    fun resolveFcmNotificationText_collapsesWhitespace() {
        val result = resolveFcmNotificationText(
            notificationType = NotificationType.DATE_CONFIRMED,
            dataTitle = "  Vote\n\nterminé  ",
            notificationTitle = null,
            dataBody = "  Date\t\tvalidée   pour   samedi  ",
            notificationBody = null
        )

        assertEquals(FcmNotificationText("Vote terminé", "Date validée pour samedi"), result)
    }

    @Test
    fun resolveFcmNotificationText_boundsOversizedPayloadText() {
        val result = resolveFcmNotificationText(
            notificationType = NotificationType.EVENT_UPDATE,
            dataTitle = "T".repeat(120),
            notificationTitle = null,
            dataBody = "B".repeat(300),
            notificationBody = null
        )

        assertEquals(80, result.title.length)
        assertEquals(240, result.body.length)
        assertTrue(result.title.endsWith("..."))
        assertTrue(result.body.endsWith("..."))
    }

    @Test
    fun summarizeFcmNotificationForLogDoesNotExposePayloadContentOrIds() {
        val result = summarizeFcmNotificationForLog(
            data = mapOf(
                "title" to "Secret dinner moved",
                "body" to "Meet at 42 Private Street",
                "eventId" to "event-secret-123",
                "deepLink" to "wakeve://invite/INVITE-SECRET-123"
            ),
            notificationType = NotificationType.EVENT_INVITE,
            resolvedDeepLink = "wakeve://invite/INVITE-SECRET-123"
        )
        val logLine = result.toString()

        assertTrue(result.hasPayloadTitle)
        assertTrue(result.hasPayloadBody)
        assertTrue(result.hasEventContext)
        assertTrue(result.hasDeepLink)
        assertFalse(logLine.contains("Secret dinner moved"))
        assertFalse(logLine.contains("42 Private Street"))
        assertFalse(logLine.contains("event-secret-123"))
        assertFalse(logLine.contains("INVITE-SECRET-123"))
    }

    @Test
    fun fcmFailureLogMessagesDoNotExposeThrowableDetails() {
        val secret = "SECRET-FCM-TOKEN"
        val messages = listOf(
            fcmStoredTokenRegistrationFailureLogMessage(),
            fcmTokenUnregistrationFailureLogMessage(),
            fcmNotificationPreferencesReadFailureLogMessage(),
            fcmTokenRegistrationFailureLogMessage(),
            fcmNotificationPersistenceFailureLogMessage()
        )

        messages.forEach { message ->
            assertFalse(message.contains(":"))
            assertFalse(message.contains(secret))
            assertFalse(message.contains("Exception"))
            assertFalse(message.contains("token=", ignoreCase = true))
        }
    }

    @Test
    fun resolveFcmNotificationTimestampMillis_acceptsEpochMilliseconds() {
        val nowMs = 1_800_000_000_000L
        val timestampMs = 1_767_704_400_000L

        val result = resolveFcmNotificationTimestampMillis(
            rawTimestamp = " $timestampMs ",
            nowMs = nowMs
        )

        assertEquals(timestampMs, result)
    }

    @Test
    fun resolveFcmNotificationTimestampMillis_convertsEpochSeconds() {
        val nowMs = 1_800_000_000_000L

        val result = resolveFcmNotificationTimestampMillis(
            rawTimestamp = "1767704400",
            nowMs = nowMs
        )

        assertEquals(1_767_704_400_000L, result)
    }

    @Test
    fun resolveFcmNotificationTimestampMillis_acceptsIso8601Timestamps() {
        val nowMs = Instant.parse("2026-06-20T12:00:00Z").toEpochMilliseconds()

        val result = resolveFcmNotificationTimestampMillis(
            rawTimestamp = "2026-06-20T10:15:30Z",
            nowMs = nowMs
        )

        assertEquals(Instant.parse("2026-06-20T10:15:30Z").toEpochMilliseconds(), result)
    }

    @Test
    fun resolveFcmNotificationTimestampMillis_fallsBackForBlankOrMalformedTimestamps() {
        val nowMs = 1_800_000_000_000L

        assertEquals(nowMs, resolveFcmNotificationTimestampMillis(null, nowMs))
        assertEquals(nowMs, resolveFcmNotificationTimestampMillis("   ", nowMs))
        assertEquals(nowMs, resolveFcmNotificationTimestampMillis("not-a-date", nowMs))
    }

    @Test
    fun resolveFcmNotificationTimestampMillis_fallsBackForNegativeOrFarFutureTimestamps() {
        val nowMs = 1_800_000_000_000L

        val negative = resolveFcmNotificationTimestampMillis(
            rawTimestamp = "-1700000000000",
            nowMs = nowMs
        )
        val farFuture = resolveFcmNotificationTimestampMillis(
            rawTimestamp = (nowMs + 10 * 60 * 1000L).toString(),
            nowMs = nowMs
        )

        assertEquals(nowMs, negative)
        assertEquals(nowMs, farFuture)
    }

    @Test
    fun resolveFcmNotificationTimestampMillis_allowsSmallFutureClockSkew() {
        val nowMs = 1_800_000_000_000L
        val futureWithinSkew = nowMs + 4 * 60 * 1000L

        val result = resolveFcmNotificationTimestampMillis(
            rawTimestamp = futureWithinSkew.toString(),
            nowMs = nowMs
        )

        assertEquals(futureWithinSkew, result)
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
    fun validateBackendHttpSuccess_rejectsUnauthorizedResponseWithoutExposingBody() {
        val result = validateBackendHttpSuccess(
            operation = "FCM token registration",
            status = HttpStatusCode.Unauthorized
        )

        assertTrue(result.isFailure)
        val message = result.exceptionOrNull()?.message.orEmpty()
        assertContains(message, "FCM token registration failed")
        assertContains(message, "401")
        assertFalse(message.contains("invalid token"))
        assertFalse(message.contains("SECRET-FCM-TOKEN"))
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
    fun resolveNotificationPermissionRequestDecision_skipsBeforeAndroid13() {
        val result = resolveNotificationPermissionRequestDecision(
            sdkInt = 32,
            permissionGranted = false,
            hasActivityContext = true
        )

        assertEquals(NotificationPermissionRequestDecision.NOT_REQUIRED, result)
    }

    @Test
    fun resolveNotificationPermissionRequestDecision_skipsWhenAlreadyGranted() {
        val result = resolveNotificationPermissionRequestDecision(
            sdkInt = 33,
            permissionGranted = true,
            hasActivityContext = true
        )

        assertEquals(NotificationPermissionRequestDecision.ALREADY_GRANTED, result)
    }

    @Test
    fun resolveNotificationPermissionRequestDecision_rejectsMissingActivityContext() {
        val result = resolveNotificationPermissionRequestDecision(
            sdkInt = 33,
            permissionGranted = false,
            hasActivityContext = false
        )

        assertEquals(NotificationPermissionRequestDecision.MISSING_ACTIVITY, result)
    }

    @Test
    fun resolveNotificationPermissionRequestDecision_requestsForAndroid13ActivityWhenDenied() {
        val result = resolveNotificationPermissionRequestDecision(
            sdkInt = 33,
            permissionGranted = false,
            hasActivityContext = true
        )

        assertEquals(NotificationPermissionRequestDecision.REQUEST, result)
    }

    @Test
    fun resolveNotificationType_defaultsUnknownTypeToEventUpdate() {
        val result = resolveNotificationType(mapOf("type" to "UNKNOWN_TYPE"))

        assertEquals(NotificationType.EVENT_UPDATE, result)
    }

    @Test
    fun normalizeNotificationTypeToken_handlesCamelCaseKebabCaseAndWhitespace() {
        assertEquals("EVENT_INVITE", normalizeNotificationTypeToken(" eventInvite "))
        assertEquals("MEETING_REMINDER", normalizeNotificationTypeToken("meeting-reminder"))
        assertEquals("VOTE_CLOSE_REMINDER", normalizeNotificationTypeToken("vote close reminder"))
    }

    @Test
    fun resolveNotificationType_acceptsAlternatePayloadKeys() {
        val notificationType = resolveNotificationType(mapOf("notificationType" to "meetingReminder"))
        val snakeCase = resolveNotificationType(mapOf("notification_type" to "vote-reminder"))
        val category = resolveNotificationType(mapOf("category" to "payment_due"))

        assertEquals(NotificationType.MEETING_REMINDER, notificationType)
        assertEquals(NotificationType.VOTE_REMINDER, snakeCase)
        assertEquals(NotificationType.PAYMENT_DUE, category)
    }

    @Test
    fun resolveNotificationType_mapsLegacyModelAliases() {
        assertEquals(NotificationType.DATE_CONFIRMED, resolveNotificationType(mapOf("type" to "EVENT_CONFIRMED")))
        assertEquals(NotificationType.NEW_COMMENT, resolveNotificationType(mapOf("type" to "COMMENT_POSTED")))
        assertEquals(NotificationType.MEETING_REMINDER, resolveNotificationType(mapOf("type" to "MEETING_STARTING")))
        assertEquals(NotificationType.EVENT_INVITE, resolveNotificationType(mapOf("type" to "EVENT_INVITATION")))
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
    fun resolveNotificationChannelId_routesLegacyCommentAliasToEventsChannel() {
        val result = resolveNotificationChannelId(mapOf("type" to "comment_posted"))

        assertEquals(NotificationChannelManager.Companion.ChannelId.EVENTS.id, result)
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
    fun resolveNotificationPriority_usesAlternateTypeKeyForImportantPayloads() {
        val result = resolveNotificationPriority(mapOf("notificationType" to "event-invite"))

        assertEquals(NotificationCompat.PRIORITY_HIGH, result)
    }

    @Test
    fun resolveNotificationDeepLink_preservesSupportedDeepLink() {
        val result = resolveNotificationDeepLink(
            mapOf("deepLink" to "wakeve://invite/INVITE123")
        )

        assertEquals("wakeve://invite/INVITE123", result)
    }

    @Test
    fun resolveNotificationDeepLink_preservesSharedEventDetailsDeepLinkWithoutFallbackContext() {
        val result = resolveNotificationDeepLink(
            mapOf("deepLink" to "wakeve://event/event-123/details?tab=details")
        )

        assertEquals("wakeve://event/event-123/details?tab=details", result)
    }

    @Test
    fun resolveNotificationDeepLink_preservesSharedCommentsTabDeepLinkWithoutFallbackContext() {
        val result = resolveNotificationDeepLink(
            mapOf("deepLink" to "wakeve://event/event-123/details?tab=comments")
        )

        assertEquals("wakeve://event/event-123/details?tab=comments", result)
    }

    @Test
    fun resolveNotificationDeepLink_preservesSharedBudgetTabDeepLinkWithoutFallbackContext() {
        val result = resolveNotificationDeepLink(
            mapOf("deepLink" to "wakeve://event/event-123/details?tab=budget")
        )

        assertEquals("wakeve://event/event-123/details?tab=budget", result)
    }

    @Test
    fun resolveNotificationDeepLink_preservesSharedPollDeepLinkWithoutFallbackContext() {
        val result = resolveNotificationDeepLink(
            mapOf("deepLink" to "wakeve://event/event-123/poll?slotId=slot-456")
        )

        assertEquals("wakeve://event/event-123/poll?slotId=slot-456", result)
    }

    @Test
    fun resolveNotificationDeepLink_preservesSharedMeetingDeepLinkWithoutFallbackContext() {
        val result = resolveNotificationDeepLink(
            mapOf("deepLink" to "wakeve://event/event-123/meetings?meetingId=meeting-456&autoJoin=true")
        )

        assertEquals("wakeve://event/event-123/meetings?meetingId=meeting-456&autoJoin=true", result)
    }

    @Test
    fun resolveNotificationDeepLink_preservesSharedNotificationsDeepLink() {
        val result = resolveNotificationDeepLink(
            mapOf("deepLink" to "wakeve://notifications?filter=unread")
        )

        assertEquals("wakeve://notifications?filter=unread", result)
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
    fun resolveNotificationDeepLink_trimsFallbackPathSegments() {
        val invite = resolveNotificationDeepLink(
            mapOf(
                "type" to "EVENT_INVITE",
                "invitationCode" to " INVITE123 "
            )
        )
        val poll = resolveNotificationDeepLink(
            mapOf(
                "type" to " VOTE_REMINDER ",
                "eventId" to " event-123 "
            )
        )

        assertEquals("wakeve://invite/INVITE123", invite)
        assertEquals("wakeve://poll/event-123", poll)
    }

    @Test
    fun resolveNotificationDeepLink_rejectsUnsafeFallbackPathSegments() {
        val invite = resolveNotificationDeepLink(
            mapOf(
                "type" to "EVENT_INVITE",
                "invitationCode" to "INV/ITE"
            )
        )
        val poll = resolveNotificationDeepLink(
            mapOf(
                "type" to "VOTE_REMINDER",
                "eventId" to "event?tab=budget"
            )
        )
        val meeting = resolveNotificationDeepLink(
            mapOf(
                "type" to "MEETING_REMINDER",
                "meetingId" to "meeting#456"
            )
        )

        assertNull(invite)
        assertNull(poll)
        assertNull(meeting)
    }

    @Test
    fun resolveNotificationDeepLink_rejectsUnsupportedDeepLinkWithEncodedDelimiter() {
        val result = resolveNotificationDeepLink(
            mapOf("deepLink" to "wakeve://event/event%2F123")
        )

        assertNull(result)
    }

    @Test
    fun resolveNotificationDeepLink_rejectsDeepLinkFragmentsAndFallsBack() {
        val result = resolveNotificationDeepLink(
            mapOf(
                "deepLink" to "wakeve://event/event-123/details#comments",
                "eventId" to "event-fallback"
            )
        )

        assertEquals("wakeve://event/event-fallback", result)
    }

    @Test
    fun resolveNotificationDeepLink_rejectsAmbiguousUniversalLinkAuthority() {
        val withUserInfo = resolveNotificationDeepLink(
            mapOf(
                "deepLink" to "https://user@wakeve.app/invite/INVITE123",
                "eventId" to "event-fallback"
            )
        )
        val withPort = resolveNotificationDeepLink(
            mapOf(
                "deepLink" to "https://wakeve.app:443/invite/INVITE123",
                "eventId" to "event-fallback"
            )
        )

        assertEquals("wakeve://event/event-fallback", withUserInfo)
        assertEquals("wakeve://event/event-fallback", withPort)
    }

    @Test
    fun resolveNotificationDeepLink_rejectsSharedMeetingDeepLinkWithEncodedQueryDelimiter() {
        val result = resolveNotificationDeepLink(
            mapOf("deepLink" to "wakeve://event/event-123/meetings?meetingId=meeting%2F456")
        )

        assertNull(result)
    }

    @Test
    fun resolveNotificationDeepLink_rejectsSharedMeetingDeepLinkWithEncodedFragmentDelimiter() {
        val result = resolveNotificationDeepLink(
            mapOf("deepLink" to "wakeve://event/event-123/meetings?meetingId=meeting%23456")
        )

        assertNull(result)
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
    fun resolveNotificationDeepLink_usesAlternateTypeKeyForFallbackRoutes() {
        val meeting = resolveNotificationDeepLink(
            mapOf(
                "notificationType" to "meetingStarting",
                "eventId" to "event-123",
                "meetingId" to "meeting-456"
            )
        )
        val poll = resolveNotificationDeepLink(
            mapOf(
                "notification_type" to "vote-reminder",
                "eventId" to "event-123"
            )
        )

        assertEquals("wakeve://meeting/meeting-456", meeting)
        assertEquals("wakeve://poll/event-123", poll)
    }

    @Test
    fun resolveNotificationDeepLink_returnsNullWithoutDeepLinkOrContext() {
        val result = resolveNotificationDeepLink(
            mapOf("type" to "EVENT_UPDATE")
        )

        assertNull(result)
    }

    @Test
    fun resolveNotificationPersistenceUserId_prefersStoredUserWhenPayloadMatches() {
        val result = resolveNotificationPersistenceUserId(
            data = mapOf(
                "userId" to " user-123 ",
                "recipientUserId" to "recipient-user"
            ),
            storedUserId = " user-123 "
        )

        assertEquals("user-123", result)
    }

    @Test
    fun resolveNotificationPersistenceUserId_acceptsRecipientUserMatchWhenActorUserDiffers() {
        val result = resolveNotificationPersistenceUserId(
            data = mapOf(
                "userId" to "actor-user",
                "recipientUserId" to " recipient-user "
            ),
            storedUserId = "recipient-user"
        )

        assertEquals("recipient-user", result)
    }

    @Test
    fun resolveNotificationPersistenceUserId_rejectsMismatchedPayloadWhenStoredUserIsKnown() {
        val result = resolveNotificationPersistenceUserId(
            data = mapOf(
                "userId" to "other-user",
                "recipientUserId" to "other-recipient"
            ),
            storedUserId = "current-user"
        )

        assertNull(result)
    }

    @Test
    fun resolveNotificationPersistenceUserId_usesStoredUserWhenPayloadHasNoRecipient() {
        val result = resolveNotificationPersistenceUserId(
            data = emptyMap(),
            storedUserId = " stored-user "
        )

        assertEquals("stored-user", result)
    }

    @Test
    fun resolveNotificationPersistenceUserId_usesPayloadRecipientWhenStoredUserIsMissing() {
        val recipient = resolveNotificationPersistenceUserId(
            data = mapOf(
                "userId" to "  ",
                "recipientUserId" to " recipient-user "
            ),
            storedUserId = null
        )

        assertEquals("recipient-user", recipient)
    }

    @Test
    fun resolveNotificationPersistenceUserId_returnsNullWhenAllCandidatesAreBlank() {
        val result = resolveNotificationPersistenceUserId(
            data = mapOf(
                "userId" to "  ",
                "recipientUserId" to ""
            ),
            storedUserId = "  "
        )

        assertNull(result)
    }

    @Test
    fun shouldProcessNotificationForCurrentUser_allowsPayloadForCurrentUser() {
        val result = shouldProcessNotificationForCurrentUser(
            data = mapOf("userId" to " current-user "),
            storedUserId = "current-user"
        )

        assertTrue(result)
    }

    @Test
    fun shouldProcessNotificationForCurrentUser_allowsRecipientMatchWhenActorDiffers() {
        val result = shouldProcessNotificationForCurrentUser(
            data = mapOf(
                "userId" to "actor-user",
                "recipientUserId" to " current-user "
            ),
            storedUserId = "current-user"
        )

        assertTrue(result)
    }

    @Test
    fun shouldProcessNotificationForCurrentUser_rejectsMismatchedKnownUserPayload() {
        val result = shouldProcessNotificationForCurrentUser(
            data = mapOf(
                "userId" to "other-user",
                "recipientUserId" to "other-recipient"
            ),
            storedUserId = "current-user"
        )

        assertFalse(result)
    }

    @Test
    fun shouldProcessNotificationForCurrentUser_allowsMissingPayloadRecipient() {
        val withoutPayload = shouldProcessNotificationForCurrentUser(
            data = emptyMap(),
            storedUserId = "current-user"
        )
        val withoutStoredUser = shouldProcessNotificationForCurrentUser(
            data = mapOf("userId" to "other-user"),
            storedUserId = null
        )

        assertTrue(withoutPayload)
        assertTrue(withoutStoredUser)
    }

    @Test
    fun shouldAcceptFcmNotificationPayload_requiresActionableDestination() {
        val invite = shouldAcceptFcmNotificationPayload(
            notificationType = NotificationType.EVENT_INVITE,
            resolvedDeepLink = "wakeve://invite/INVITE123"
        )
        val eventUpdate = shouldAcceptFcmNotificationPayload(
            notificationType = NotificationType.EVENT_UPDATE,
            resolvedDeepLink = "wakeve://event/event-123"
        )
        val topLevelNotifications = shouldAcceptFcmNotificationPayload(
            notificationType = NotificationType.EVENT_UPDATE,
            resolvedDeepLink = "wakeve://notifications?filter=unread"
        )

        assertTrue(invite)
        assertTrue(eventUpdate)
        assertTrue(topLevelNotifications)
    }

    @Test
    fun shouldAcceptFcmNotificationPayload_rejectsNonActionableNotifications() {
        val genericUpdate = shouldAcceptFcmNotificationPayload(
            notificationType = NotificationType.EVENT_UPDATE,
            resolvedDeepLink = null
        )
        val importantButUnroutable = shouldAcceptFcmNotificationPayload(
            notificationType = NotificationType.DATE_CONFIRMED,
            resolvedDeepLink = " "
        )

        assertFalse(genericUpdate)
        assertFalse(importantButUnroutable)
    }

    @Test
    fun shouldProcessNotificationForPreferences_allowsWhenPreferencesAreMissingOrCorrupt() {
        val missing = shouldProcessNotificationForPreferences(
            notificationType = NotificationType.EVENT_UPDATE,
            enabledTypesJson = null
        )
        val corrupt = shouldProcessNotificationForPreferences(
            notificationType = NotificationType.EVENT_UPDATE,
            enabledTypesJson = "not-json"
        )
        val legacyUnknownOnly = shouldProcessNotificationForPreferences(
            notificationType = NotificationType.EVENT_UPDATE,
            enabledTypesJson = """["LEGACY_UNKNOWN_TYPE"]"""
        )

        assertTrue(missing)
        assertTrue(corrupt)
        assertTrue(legacyUnknownOnly)
    }

    @Test
    fun shouldProcessNotificationForPreferences_respectsExplicitEmptyOptOut() {
        val result = shouldProcessNotificationForPreferences(
            notificationType = NotificationType.EVENT_INVITE,
            enabledTypesJson = "[]"
        )

        assertFalse(result)
    }

    @Test
    fun shouldProcessNotificationForPreferences_rejectsDisabledType() {
        val result = shouldProcessNotificationForPreferences(
            notificationType = NotificationType.NEW_COMMENT,
            enabledTypesJson = """["EVENT_INVITE","DATE_CONFIRMED"]"""
        )

        assertFalse(result)
    }

    @Test
    fun shouldProcessNotificationForPreferences_allowsEnabledType() {
        val result = shouldProcessNotificationForPreferences(
            notificationType = NotificationType.DATE_CONFIRMED,
            enabledTypesJson = """["EVENT_INVITE","DATE_CONFIRMED"]"""
        )

        assertTrue(result)
    }

    @Test
    fun shouldProcessNotificationForPreferences_rejectsNonUrgentDuringQuietHours() {
        val result = shouldProcessNotificationForPreferences(
            notificationType = NotificationType.DATE_CONFIRMED,
            enabledTypesJson = """["DATE_CONFIRMED"]""",
            quietHoursStart = "22:00",
            quietHoursEnd = "08:00",
            currentTime = Instant.fromEpochMilliseconds(0)
        )

        assertFalse(result)
    }

    @Test
    fun resolveFcmNotificationPreferenceDecision_persistsButDoesNotDisplayDuringQuietHours() {
        val decision = resolveFcmNotificationPreferenceDecision(
            notificationType = NotificationType.DATE_CONFIRMED,
            enabledTypesJson = """["DATE_CONFIRMED"]""",
            quietHoursStart = "22:00",
            quietHoursEnd = "08:00",
            currentMinuteOfDay = 23 * 60
        )

        assertEquals(
            FcmNotificationPreferenceDecision(
                shouldPersist = true,
                shouldDisplay = false
            ),
            decision
        )
    }

    @Test
    fun resolveFcmNotificationPreferenceDecision_dropsDisabledTypesEntirely() {
        val decision = resolveFcmNotificationPreferenceDecision(
            notificationType = NotificationType.NEW_COMMENT,
            enabledTypesJson = """["EVENT_INVITE","DATE_CONFIRMED"]""",
            quietHoursStart = "22:00",
            quietHoursEnd = "08:00",
            currentMinuteOfDay = 9 * 60
        )

        assertEquals(
            FcmNotificationPreferenceDecision(
                shouldPersist = false,
                shouldDisplay = false
            ),
            decision
        )
    }

    @Test
    fun resolveFcmNotificationPreferenceDecision_displaysEnabledTypeOutsideQuietHours() {
        val decision = resolveFcmNotificationPreferenceDecision(
            notificationType = NotificationType.DATE_CONFIRMED,
            enabledTypesJson = """["DATE_CONFIRMED"]""",
            quietHoursStart = "22:00",
            quietHoursEnd = "08:00",
            currentMinuteOfDay = 9 * 60
        )

        assertEquals(
            FcmNotificationPreferenceDecision(
                shouldPersist = true,
                shouldDisplay = true
            ),
            decision
        )
    }

    @Test
    fun shouldProcessNotificationForPreferences_allowsNonUrgentOutsideQuietHours() {
        val result = shouldProcessNotificationForPreferences(
            notificationType = NotificationType.DATE_CONFIRMED,
            enabledTypesJson = """["DATE_CONFIRMED"]""",
            quietHoursStart = "22:00",
            quietHoursEnd = "08:00",
            currentTime = Instant.fromEpochMilliseconds(9 * 60 * 60 * 1000L)
        )

        assertTrue(result)
    }

    @Test
    fun shouldProcessNotificationForPreferences_usesExplicitLocalMinuteForQuietHours() {
        val allowedByInstantButBlockedByLocalMinute = shouldProcessNotificationForPreferences(
            notificationType = NotificationType.DATE_CONFIRMED,
            enabledTypesJson = """["DATE_CONFIRMED"]""",
            quietHoursStart = "22:00",
            quietHoursEnd = "08:00",
            currentTime = Instant.fromEpochMilliseconds(12 * 60 * 60 * 1000L),
            currentMinuteOfDay = 23 * 60
        )
        val blockedByInstantButAllowedByLocalMinute = shouldProcessNotificationForPreferences(
            notificationType = NotificationType.DATE_CONFIRMED,
            enabledTypesJson = """["DATE_CONFIRMED"]""",
            quietHoursStart = "22:00",
            quietHoursEnd = "08:00",
            currentTime = Instant.fromEpochMilliseconds(0),
            currentMinuteOfDay = 9 * 60
        )

        assertFalse(allowedByInstantButBlockedByLocalMinute)
        assertTrue(blockedByInstantButAllowedByLocalMinute)
    }

    @Test
    fun shouldProcessNotificationForPreferences_allowsUrgentDuringQuietHours() {
        val result = shouldProcessNotificationForPreferences(
            notificationType = NotificationType.MEETING_REMINDER,
            enabledTypesJson = """["MEETING_REMINDER"]""",
            quietHoursStart = "22:00",
            quietHoursEnd = "08:00",
            currentTime = Instant.fromEpochMilliseconds(0)
        )

        assertTrue(result)
    }

    @Test
    fun shouldProcessNotificationForPreferences_ignoresInvalidOrPartialQuietHours() {
        val invalid = shouldProcessNotificationForPreferences(
            notificationType = NotificationType.DATE_CONFIRMED,
            enabledTypesJson = """["DATE_CONFIRMED"]""",
            quietHoursStart = "invalid",
            quietHoursEnd = "08:00",
            currentTime = Instant.fromEpochMilliseconds(0)
        )
        val partial = shouldProcessNotificationForPreferences(
            notificationType = NotificationType.DATE_CONFIRMED,
            enabledTypesJson = """["DATE_CONFIRMED"]""",
            quietHoursStart = "22:00",
            quietHoursEnd = null,
            currentTime = Instant.fromEpochMilliseconds(0)
        )

        assertTrue(invalid)
        assertTrue(partial)
    }

    @Test
    fun resolveFcmNotificationDisplayOptions_defaultsMissingPreferencesToEnabled() {
        val options = resolveFcmNotificationDisplayOptions(
            soundEnabled = null,
            vibrationEnabled = null
        )

        assertEquals(FcmNotificationDisplayOptions(), options)
    }

    @Test
    fun resolveFcmNotificationDisplayOptions_mapsStoredPreferenceFlags() {
        val soundOnly = resolveFcmNotificationDisplayOptions(
            soundEnabled = 1L,
            vibrationEnabled = 0L
        )
        val vibrationOnly = resolveFcmNotificationDisplayOptions(
            soundEnabled = 0L,
            vibrationEnabled = 1L
        )
        val silent = resolveFcmNotificationDisplayOptions(
            soundEnabled = 0L,
            vibrationEnabled = 0L
        )

        assertEquals(FcmNotificationDisplayOptions(soundEnabled = true, vibrationEnabled = false), soundOnly)
        assertEquals(FcmNotificationDisplayOptions(soundEnabled = false, vibrationEnabled = true), vibrationOnly)
        assertEquals(FcmNotificationDisplayOptions(soundEnabled = false, vibrationEnabled = false), silent)
    }

    @Test
    fun resolveFcmNotificationDefaults_mapsSoundAndVibrationFlags() {
        val both = resolveFcmNotificationDefaults(FcmNotificationDisplayOptions())
        val soundOnly = resolveFcmNotificationDefaults(
            FcmNotificationDisplayOptions(soundEnabled = true, vibrationEnabled = false)
        )
        val vibrationOnly = resolveFcmNotificationDefaults(
            FcmNotificationDisplayOptions(soundEnabled = false, vibrationEnabled = true)
        )
        val silent = resolveFcmNotificationDefaults(
            FcmNotificationDisplayOptions(soundEnabled = false, vibrationEnabled = false)
        )

        assertEquals(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE, both)
        assertEquals(NotificationCompat.DEFAULT_SOUND, soundOnly)
        assertEquals(NotificationCompat.DEFAULT_VIBRATE, vibrationOnly)
        assertEquals(0, silent)
    }

    @Test
    fun enrichPersistedNotificationData_addsResolvedDeepLinkWhenMissing() {
        val result = enrichPersistedNotificationData(
            data = mapOf("eventId" to "event-123"),
            resolvedDeepLink = " wakeve://event/event-123/details "
        )

        assertEquals("wakeve://event/event-123/details", result["deepLink"])
    }

    @Test
    fun enrichPersistedNotificationData_preservesExistingPayloadDeepLink() {
        val result = enrichPersistedNotificationData(
            data = mapOf("deepLink" to "wakeve://invite/INVITE123"),
            resolvedDeepLink = "wakeve://event/event-123"
        )

        assertEquals("wakeve://invite/INVITE123", result["deepLink"])
    }

    @Test
    fun enrichPersistedNotificationData_trimsExistingSupportedPayloadDeepLink() {
        val result = enrichPersistedNotificationData(
            data = mapOf("deepLink" to " wakeve://invite/INVITE123 "),
            resolvedDeepLink = "wakeve://event/event-123"
        )

        assertEquals("wakeve://invite/INVITE123", result["deepLink"])
    }

    @Test
    fun enrichPersistedNotificationData_replacesUnsupportedPayloadDeepLinkWithResolvedFallback() {
        val result = enrichPersistedNotificationData(
            data = mapOf(
                "deepLink" to "wakeve://event/event-123/cancel",
                "eventId" to "event-123"
            ),
            resolvedDeepLink = "wakeve://event/event-123"
        )

        assertEquals("wakeve://event/event-123", result["deepLink"])
    }

    @Test
    fun enrichPersistedNotificationData_removesUnsupportedPayloadDeepLinkWithoutFallback() {
        val result = enrichPersistedNotificationData(
            data = mapOf("deepLink" to "https://evil.example/invite/INVITE123"),
            resolvedDeepLink = null
        )

        assertNull(result["deepLink"])
    }

    @Test
    fun enrichPersistedNotificationData_keepsPayloadUnchangedWithoutDeepLink() {
        val data = mapOf("eventId" to "event-123")

        val result = enrichPersistedNotificationData(
            data = data,
            resolvedDeepLink = null
        )

        assertEquals(data, result)
    }

    @Test
    fun resolveNotificationTapUri_prefersResolvedDeepLink() {
        val result = resolveNotificationTapUri(
            resolvedDeepLink = " wakeve://event/event-123/details?tab=comments ",
            eventId = "event-fallback"
        )

        assertEquals("wakeve://event/event-123/details?tab=comments", result)
    }

    @Test
    fun resolveNotificationTapUri_fallsBackFromUnsupportedResolvedDeepLink() {
        val result = resolveNotificationTapUri(
            resolvedDeepLink = "wakeve://event/event-123/cancel",
            eventId = "event-fallback"
        )

        assertEquals("wakeve://event/event-fallback", result)
    }

    @Test
    fun resolveNotificationTapUri_rejectsUnsupportedResolvedDeepLinkWithoutFallback() {
        val result = resolveNotificationTapUri(
            resolvedDeepLink = "https://evil.example/invite/INVITE123",
            eventId = null
        )

        assertNull(result)
    }

    @Test
    fun resolveNotificationTapUri_fallsBackToTrimmedSafeEventId() {
        val result = resolveNotificationTapUri(
            resolvedDeepLink = null,
            eventId = " event-123 "
        )

        assertEquals("wakeve://event/event-123", result)
    }

    @Test
    fun resolveNotificationTapUri_rejectsUnsafeFallbackEventId() {
        val result = resolveNotificationTapUri(
            resolvedDeepLink = null,
            eventId = "event/123"
        )

        assertNull(result)
    }

    @Test
    fun resolveNotificationTapUri_returnsNullWithoutDeepLinkOrEventId() {
        val result = resolveNotificationTapUri(
            resolvedDeepLink = " ",
            eventId = null
        )

        assertNull(result)
    }
}
