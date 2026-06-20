package com.guyghost.wakeve.meeting

import com.guyghost.wakeve.createFreshTestDatabase
import com.guyghost.wakeve.calendar.CalendarService
import com.guyghost.wakeve.calendar.PlatformCalendarService
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.EnhancedCalendarEvent
import com.guyghost.wakeve.models.InvitationStatus
import com.guyghost.wakeve.models.MeetingLinkResponse
import com.guyghost.wakeve.models.MeetingPlatform
import com.guyghost.wakeve.models.MeetingStatus
import com.guyghost.wakeve.models.NotificationMessage
import com.guyghost.wakeve.models.PushToken
import com.guyghost.wakeve.notification.DefaultNotificationService
import com.guyghost.wakeve.notification.NotificationServiceInterface
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

class MeetingServiceTest {

    private lateinit var database: WakeveDb
    private lateinit var service: MeetingService
    private lateinit var notificationService: RecordingNotificationService

    @BeforeTest
    fun setUp() {
        database = createFreshTestDatabase()
        val calendarService = CalendarService(database, NoopPlatformCalendarService())
        notificationService = RecordingNotificationService()
        service = MeetingService(
            database = database,
            calendarService = calendarService,
            notificationService = notificationService,
            meetingLinkProvider = DeterministicMeetingLinkProvider()
        )
    }

    @Test
    fun createMeetingSucceedsForOrganizingEvent() = runTest {
        seedEvent(eventId = "event-1", status = "ORGANIZING")
        seedParticipant(eventId = "event-1", participantId = "participant-1", userId = "participant-1", validated = true)

        val result = service.createMeeting(
            eventId = "event-1",
            organizerId = "organizer-1",
            platform = MeetingPlatform.ZOOM,
            title = "Planning meeting",
            description = "Kickoff",
            scheduledFor = Clock.System.now(),
            duration = 1.hours,
            timezone = "Europe/Paris"
        )

        assertTrue(result.isSuccess)
        val meeting = result.getOrThrow()
        assertEquals(MeetingPlatform.ZOOM, meeting.platform)
        assertEquals(MeetingStatus.SCHEDULED, meeting.status)

        val pendingReminders = database.meetingReminderQueries.countPendingByMeetingId(meeting.id).executeAsOne()
        assertEquals(4L, pendingReminders)
    }

    @Test
    fun createMeetingNormalizesInputsAndUsesParticipantIdsForLocalRows() = runTest {
        seedEvent(eventId = "event-normalized", status = "ORGANIZING")
        seedParticipant(
            eventId = "event-normalized",
            participantId = "participant-row-1",
            userId = "user-1",
            validated = true
        )

        val meeting = service.createMeeting(
            eventId = " event-normalized ",
            organizerId = " organizer-1 ",
            platform = MeetingPlatform.ZOOM,
            title = "  Planning meeting  ",
            description = "  Sync before departure  ",
            scheduledFor = Instant.parse("2026-07-18T08:00:00Z"),
            duration = 1.hours,
            timezone = " Europe/Paris "
        ).getOrThrow()

        assertEquals("event-normalized", meeting.eventId)
        assertEquals("organizer-1", meeting.organizerId)
        assertEquals("Planning meeting", meeting.title)
        assertEquals("Sync before departure", meeting.description)
        assertEquals("Europe/Paris", meeting.timezone)

        val persisted = database.meetingQueries.selectById(meeting.id).executeAsOne()
        assertTrue(persisted.invitedParticipants.contains("participant-row-1"))
        assertFalse(persisted.invitedParticipants.contains("user-1"))

        val reminders = database.meetingReminderQueries.selectByMeetingId(meeting.id).executeAsList()
        assertEquals(4, reminders.size)
        assertTrue(reminders.all { it.participant_id == "participant-row-1" })

        val creationNotifications = notificationService.sentMessages.filter { it.title.startsWith("Réunion créée:") }
        assertEquals(1, creationNotifications.size)
        assertEquals("user-1", creationNotifications.single().userId)

        val sync = database.syncMetadataQueries.selectByEntity("meeting", meeting.id).executeAsList().single()
        assertTrue(sync.payload.contains("\"eventId\":\"event-normalized\""))
        assertTrue(sync.payload.contains("\"organizerId\":\"organizer-1\""))
        assertTrue(sync.payload.contains("\"title\":\"Planning meeting\""))
        assertFalse(sync.payload.contains(" event-normalized "))
        assertFalse(sync.payload.contains("  Planning meeting  "))
    }

    @Test
    fun createMeetingFailsForConfirmedEvent() = runTest {
        seedEvent(eventId = "event-1", status = "CONFIRMED")
        seedParticipant(eventId = "event-1", participantId = "participant-1", userId = "participant-1", validated = true)

        val result = service.createMeeting(
            eventId = "event-1",
            organizerId = "organizer-1",
            platform = MeetingPlatform.ZOOM,
            title = "Planning meeting",
            description = null,
            scheduledFor = Clock.System.now(),
            duration = 1.hours,
            timezone = "Europe/Paris"
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun createMeetingFailsForDraftEvent() = runTest {
        seedEvent(eventId = "event-1", status = "DRAFT")
        seedParticipant(eventId = "event-1", participantId = "participant-1", userId = "participant-1", validated = true)

        val result = service.createMeeting(
            eventId = "event-1",
            organizerId = "organizer-1",
            platform = MeetingPlatform.ZOOM,
            title = "Planning meeting",
            description = null,
            scheduledFor = Clock.System.now(),
            duration = 1.hours,
            timezone = "Europe/Paris"
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun createMeetingFailsWhenCreationNotificationDeliveryFails() = runTest {
        seedEvent(eventId = "event-1", status = "ORGANIZING")
        seedParticipant(eventId = "event-1", participantId = "participant-1", userId = "participant-1", validated = true)
        val failingService = serviceWithNotification(FailingNotificationService("meeting creation notification failed"))

        val result = failingService.createMeeting(
            eventId = "event-1",
            organizerId = "organizer-1",
            platform = MeetingPlatform.ZOOM,
            title = "Notification failure",
            description = null,
            scheduledFor = Clock.System.now(),
            duration = 1.hours,
            timezone = "Europe/Paris"
        )

        assertTrue(result.isFailure)
        assertContains(result.exceptionOrNull()?.message.orEmpty(), "meeting creation notification failed")
    }

    @Test
    fun sendInvitationsInvitesOnlyValidatedParticipants() = runTest {
        seedEvent(eventId = "event-1", status = "ORGANIZING")
        seedParticipant(eventId = "event-1", participantId = "validated-user", userId = "validated-user", validated = true)
        seedParticipant(eventId = "event-1", participantId = "unvalidated-user", userId = "unvalidated-user", validated = false)

        val meetingId = service.createMeeting(
            eventId = "event-1",
            organizerId = "organizer-1",
            platform = MeetingPlatform.GOOGLE_MEET,
            title = "Invites",
            description = null,
            scheduledFor = Clock.System.now(),
            duration = 1.hours,
            timezone = "Europe/Paris"
        ).getOrThrow().id

        val result = service.sendInvitations(meetingId)

        assertTrue(result.isSuccess)

        val invitations = database.meetingInvitationQueries.selectByMeetingId(meetingId).executeAsList()
        assertTrue(invitations.any { it.participant_id == "validated-user" })
        assertFalse(invitations.any { it.participant_id == "unvalidated-user" })
    }

    @Test
    fun sendInvitationsIsIdempotentAndKeepsParticipantRowsSeparateFromUserNotifications() = runTest {
        seedEvent(eventId = "event-invite-retry", status = "ORGANIZING")
        seedParticipant(eventId = "event-invite-retry", participantId = "participant-row-1", userId = "user-1", validated = true)
        seedParticipant(eventId = "event-invite-retry", participantId = "participant-row-2", userId = "user-2", validated = true)
        seedParticipant(eventId = "event-invite-retry", participantId = "participant-row-3", userId = "user-3", validated = false)

        val meetingId = service.createMeeting(
            eventId = "event-invite-retry",
            organizerId = "organizer-1",
            platform = MeetingPlatform.GOOGLE_MEET,
            title = "Retry invites",
            description = null,
            scheduledFor = Instant.parse("2026-07-18T08:00:00Z"),
            duration = 1.hours,
            timezone = "Europe/Paris"
        ).getOrThrow().id

        val first = service.sendInvitations(" $meetingId ")
        val second = service.sendInvitations(meetingId)

        assertTrue(first.isSuccess)
        assertTrue(second.isSuccess)

        val invitations = database.meetingInvitationQueries.selectByMeetingId(meetingId).executeAsList()
        assertEquals(2, invitations.size)
        assertTrue(invitations.any { it.participant_id == "participant-row-1" })
        assertTrue(invitations.any { it.participant_id == "participant-row-2" })
        assertFalse(invitations.any { it.participant_id == "participant-row-3" })
        assertFalse(invitations.any { it.participant_id == "user-1" || it.participant_id == "user-2" })

        val invitationNotifications = notificationService.sentMessages.filter { it.title.startsWith("Invitation:") }
        assertEquals(2, invitationNotifications.size)
        assertTrue(invitationNotifications.any { it.userId == "user-1" })
        assertTrue(invitationNotifications.any { it.userId == "user-2" })
        assertFalse(invitationNotifications.any { it.userId == "user-3" })
    }

    @Test
    fun respondToInvitationUpdatesStatusAndTimestampsForInvitationOwner() = runTest {
        seedEvent(eventId = "event-1", status = "ORGANIZING")
        seedParticipant(eventId = "event-1", participantId = "participant-row-1", userId = "user-1", validated = true)

        val meetingId = service.createMeeting(
            eventId = "event-1",
            organizerId = "organizer-1",
            platform = MeetingPlatform.FACETIME,
            title = "Respond",
            description = null,
            scheduledFor = Clock.System.now(),
            duration = 1.hours,
            timezone = "Europe/Paris"
        ).getOrThrow().id

        service.sendInvitations(meetingId).getOrThrow()
        val invitation = database.meetingInvitationQueries.selectByMeetingId(meetingId).executeAsList().firstOrNull()
        assertNotNull(invitation)

        val result = service.respondToInvitation(
            invitationId = " ${invitation.id} ",
            currentUserId = " user-1 ",
            status = InvitationStatus.ACCEPTED
        )

        assertTrue(result.isSuccess)

        val updated = database.meetingInvitationQueries.selectById(invitation.id).executeAsOne()
        assertEquals(InvitationStatus.ACCEPTED.name, updated.status)
        assertNotNull(updated.responded_at)
        assertNotNull(updated.accepted_at)
    }

    @Test
    fun respondToInvitationRejectsNonOwnerWithoutMutation() = runTest {
        seedEvent(eventId = "event-invite-auth", status = "ORGANIZING")
        seedParticipant(eventId = "event-invite-auth", participantId = "participant-row-1", userId = "user-1", validated = true)
        seedParticipant(eventId = "event-invite-auth", participantId = "participant-row-2", userId = "user-2", validated = true)

        val meetingId = service.createMeeting(
            eventId = "event-invite-auth",
            organizerId = "organizer-1",
            platform = MeetingPlatform.FACETIME,
            title = "Invitation ownership",
            description = null,
            scheduledFor = Clock.System.now(),
            duration = 1.hours,
            timezone = "Europe/Paris"
        ).getOrThrow().id

        service.sendInvitations(meetingId).getOrThrow()
        val invitation = database.meetingInvitationQueries
            .selectByMeetingId(meetingId)
            .executeAsList()
            .single { it.participant_id == "participant-row-1" }

        val result = service.respondToInvitation(
            invitationId = invitation.id,
            currentUserId = "user-2",
            status = InvitationStatus.DECLINED
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is MeetingException.UnauthorizedAccess)
        val unchanged = database.meetingInvitationQueries.selectById(invitation.id).executeAsOne()
        assertEquals(InvitationStatus.PENDING.name, unchanged.status)
        assertEquals(null, unchanged.responded_at)
        assertEquals(null, unchanged.accepted_at)
    }

    @Test
    fun respondToInvitationRejectsPendingStatusWithoutMutation() = runTest {
        seedEvent(eventId = "event-invite-pending", status = "ORGANIZING")
        seedParticipant(eventId = "event-invite-pending", participantId = "participant-row-1", userId = "user-1", validated = true)

        val meetingId = service.createMeeting(
            eventId = "event-invite-pending",
            organizerId = "organizer-1",
            platform = MeetingPlatform.GOOGLE_MEET,
            title = "Pending response",
            description = null,
            scheduledFor = Clock.System.now(),
            duration = 1.hours,
            timezone = "Europe/Paris"
        ).getOrThrow().id

        service.sendInvitations(meetingId).getOrThrow()
        val invitation = database.meetingInvitationQueries.selectByMeetingId(meetingId).executeAsList().single()

        val result = service.respondToInvitation(
            invitationId = invitation.id,
            currentUserId = "user-1",
            status = InvitationStatus.PENDING
        )

        assertTrue(result.isFailure)
        val unchanged = database.meetingInvitationQueries.selectById(invitation.id).executeAsOne()
        assertEquals(InvitationStatus.PENDING.name, unchanged.status)
        assertEquals(null, unchanged.responded_at)
    }

    @Test
    fun createMeetingGeneratesGoogleMeetLink() = runTest {
        seedEvent(eventId = "event-1", status = "ORGANIZING")
        seedParticipant(eventId = "event-1", participantId = "participant-1", userId = "participant-1", validated = true)

        val result = service.createMeeting(
            eventId = "event-1",
            organizerId = "organizer-1",
            platform = MeetingPlatform.GOOGLE_MEET,
            title = "Google Meet Test",
            description = null,
            scheduledFor = Clock.System.now(),
            duration = 1.hours,
            timezone = "Europe/Paris"
        )

        assertTrue(result.isSuccess)
        val meeting = result.getOrThrow()
        assertEquals(MeetingPlatform.GOOGLE_MEET, meeting.platform)
        assertTrue(meeting.meetingUrl.contains("meet.google.com"))
        assertEquals(MeetingStatus.SCHEDULED, meeting.status)
    }

    @Test
    fun createMeetingGeneratesFaceTimeURL() = runTest {
        seedEvent(eventId = "event-1", status = "ORGANIZING")
        seedParticipant(eventId = "event-1", participantId = "participant-1", userId = "participant-1", validated = true)

        val result = service.createMeeting(
            eventId = "event-1",
            organizerId = "organizer-1",
            platform = MeetingPlatform.FACETIME,
            title = "FaceTime Test",
            description = null,
            scheduledFor = Clock.System.now(),
            duration = 1.hours,
            timezone = "Europe/Paris"
        )

        assertTrue(result.isSuccess)
        val meeting = result.getOrThrow()
        assertEquals(MeetingPlatform.FACETIME, meeting.platform)
        assertTrue(meeting.meetingUrl.contains("facetime://"))
        assertEquals(MeetingStatus.SCHEDULED, meeting.status)
    }

    @Test
    fun cancelMeetingUpdatesStatusToCancelled() = runTest {
        seedEvent(eventId = "event-1", status = "ORGANIZING")
        seedParticipant(eventId = "event-1", participantId = "participant-1", userId = "participant-1", validated = true)

        val meeting = service.createMeeting(
            eventId = "event-1",
            organizerId = "organizer-1",
            platform = MeetingPlatform.ZOOM,
            title = "To Cancel",
            description = null,
            scheduledFor = Clock.System.now(),
            duration = 1.hours,
            timezone = "Europe/Paris"
        ).getOrThrow()

        val result = service.cancelMeeting(meeting.id)

        assertTrue(result.isSuccess)

        val cancelledMeeting = service.getMeeting(meeting.id)
        assertNotNull(cancelledMeeting)
        assertEquals(MeetingStatus.CANCELLED, cancelledMeeting?.status)
        assertEquals(
            0L,
            database.meetingReminderQueries.countPendingByMeetingId(meeting.id).executeAsOne(),
            "Cancelling a meeting must remove pending reminders so users are not reminded about cancelled calls"
        )
    }

    @Test
    fun cancelMeetingNotifiesInvitedParticipants() = runTest {
        seedEvent(eventId = "event-1", status = "ORGANIZING")
        seedParticipant(eventId = "event-1", participantId = "user-1", userId = "user-1", validated = true)
        seedParticipant(eventId = "event-1", participantId = "user-2", userId = "user-2", validated = true)
        seedParticipant(eventId = "event-1", participantId = "user-3", userId = "user-3", validated = false)

        val meeting = service.createMeeting(
            eventId = "event-1",
            organizerId = "organizer-1",
            platform = MeetingPlatform.ZOOM,
            title = "Cancelled",
            description = null,
            scheduledFor = Clock.System.now(),
            duration = 1.hours,
            timezone = "Europe/Paris"
        ).getOrThrow()

        val result = service.cancelMeeting(meeting.id)

        assertTrue(result.isSuccess)
        val cancellationNotifications = notificationService.sentMessages.filter { it.title.startsWith("Réunion annulée:") }
        assertEquals(2, cancellationNotifications.size)
        assertTrue(cancellationNotifications.any { it.userId == "user-1" })
        assertTrue(cancellationNotifications.any { it.userId == "user-2" })
        assertFalse(cancellationNotifications.any { it.userId == "user-3" })
    }

    @Test
    fun cancelMeetingFailsWhenCancellationNotificationDeliveryFails() = runTest {
        seedEvent(eventId = "event-1", status = "ORGANIZING")
        seedParticipant(eventId = "event-1", participantId = "participant-1", userId = "participant-1", validated = true)

        val meeting = service.createMeeting(
            eventId = "event-1",
            organizerId = "organizer-1",
            platform = MeetingPlatform.ZOOM,
            title = "Cancellation failure",
            description = null,
            scheduledFor = Clock.System.now(),
            duration = 1.hours,
            timezone = "Europe/Paris"
        ).getOrThrow()
        val failingService = serviceWithNotification(FailingNotificationService("cancellation notification failed"))

        val result = failingService.cancelMeeting(meeting.id)

        assertTrue(result.isFailure)
        assertContains(result.exceptionOrNull()?.message.orEmpty(), "cancellation notification failed")
    }

    @Test
    fun cancelMeetingFailsWhenMeetingProviderCancellationFails() = runTest {
        seedEvent(eventId = "event-1", status = "ORGANIZING")
        seedParticipant(eventId = "event-1", participantId = "participant-1", userId = "participant-1", validated = true)

        val meeting = service.createMeeting(
            eventId = "event-1",
            organizerId = "organizer-1",
            platform = MeetingPlatform.ZOOM,
            title = "Provider failure",
            description = null,
            scheduledFor = Clock.System.now(),
            duration = 1.hours,
            timezone = "Europe/Paris"
        ).getOrThrow()
        val failingService = serviceWithMeetingLinkProvider(FailingCancelMeetingLinkProvider("provider cancellation failed"))

        val result = failingService.cancelMeeting(meeting.id)

        assertTrue(result.isFailure)
        assertContains(result.exceptionOrNull()?.message.orEmpty(), "provider cancellation failed")
        val persistedMeeting = service.getMeeting(meeting.id)
        assertEquals(MeetingStatus.SCHEDULED, persistedMeeting?.status)
    }

    @Test
    fun updateMeetingFailsWhenNativeCalendarUpdateFails() = runTest {
        seedEvent(eventId = "event-1", status = "ORGANIZING")
        seedParticipant(eventId = "event-1", participantId = "participant-1", userId = "participant-1", validated = true)

        val meeting = service.createMeeting(
            eventId = "event-1",
            organizerId = "organizer-1",
            platform = MeetingPlatform.ZOOM,
            title = "Calendar failure",
            description = null,
            scheduledFor = Clock.System.now(),
            duration = 1.hours,
            timezone = "Europe/Paris"
        ).getOrThrow()
        val failingService = serviceWithCalendar(FailingPlatformCalendarService("calendar update failed"))

        val result = failingService.updateMeeting(
            meetingId = meeting.id,
            title = "Updated title"
        )

        assertTrue(result.isFailure)
        assertContains(result.exceptionOrNull()?.message.orEmpty(), "calendar update failed")
    }

    @Test
    fun sendInvitationsFailsWhenNativeCalendarAddFails() = runTest {
        seedEvent(eventId = "event-1", status = "ORGANIZING")
        seedParticipant(eventId = "event-1", participantId = "participant-1", userId = "participant-1", validated = true)

        val meeting = service.createMeeting(
            eventId = "event-1",
            organizerId = "organizer-1",
            platform = MeetingPlatform.ZOOM,
            title = "Calendar failure",
            description = null,
            scheduledFor = Clock.System.now(),
            duration = 1.hours,
            timezone = "Europe/Paris"
        ).getOrThrow()
        val failingService = serviceWithCalendar(FailingPlatformCalendarService("calendar add failed"))

        val result = failingService.sendInvitations(meeting.id)

        assertTrue(result.isFailure)
        assertContains(result.exceptionOrNull()?.message.orEmpty(), "calendar add failed")
    }

    @Test
    fun sendInvitationsFailsWhenNotificationDeliveryFails() = runTest {
        seedEvent(eventId = "event-1", status = "ORGANIZING")
        seedParticipant(eventId = "event-1", participantId = "participant-1", userId = "participant-1", validated = true)

        val meeting = service.createMeeting(
            eventId = "event-1",
            organizerId = "organizer-1",
            platform = MeetingPlatform.ZOOM,
            title = "Notification failure",
            description = null,
            scheduledFor = Clock.System.now(),
            duration = 1.hours,
            timezone = "Europe/Paris"
        ).getOrThrow()
        val failingService = serviceWithNotification(FailingNotificationService("notification delivery failed"))

        val result = failingService.sendInvitations(meeting.id)

        assertTrue(result.isFailure)
        assertContains(result.exceptionOrNull()?.message.orEmpty(), "notification delivery failed")
    }

    @Test
    fun generateMeetingLinkReturnsValidLinkForZoom() = runTest {
        val result = service.generateMeetingLink(
            platform = MeetingPlatform.ZOOM,
            title = "Test Meeting",
            description = "Test Description",
            scheduledFor = Clock.System.now(),
            duration = 1.hours
        )

        assertTrue(result.isSuccess)
        val response = result.getOrThrow()
        assertNotNull(response.meetingId)
        assertNotNull(response.meetingUrl)
        assertTrue(response.meetingUrl.contains("zoom.us"))
        assertNotNull(response.password)
    }

    @Test
    fun generateMeetingLinkReturnsValidLinkForGoogleMeet() = runTest {
        val result = service.generateMeetingLink(
            platform = MeetingPlatform.GOOGLE_MEET,
            title = "Test Meeting",
            description = "Test Description",
            scheduledFor = Clock.System.now(),
            duration = 1.hours
        )

        assertTrue(result.isSuccess)
        val response = result.getOrThrow()
        assertNotNull(response.meetingId)
        assertNotNull(response.meetingUrl)
        assertTrue(response.meetingUrl.contains("meet.google.com"))
    }

    @Test
    fun createMeetingFailsWhenEventNotFound() = runTest {
        val result = service.createMeeting(
            eventId = "non-existent-event",
            organizerId = "organizer-1",
            platform = MeetingPlatform.ZOOM,
            title = "Test",
            description = null,
            scheduledFor = Clock.System.now(),
            duration = 1.hours,
            timezone = "Europe/Paris"
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun createMeetingFailsWhenMeetingLinkProviderIsNotConfigured() = runTest {
        val unconfiguredService = MeetingService(
            database = database,
            calendarService = CalendarService(database, NoopPlatformCalendarService()),
            notificationService = DefaultNotificationService()
        )
        seedEvent(eventId = "event-unconfigured-provider", status = "ORGANIZING")
        seedParticipant(
            eventId = "event-unconfigured-provider",
            participantId = "participant-1",
            userId = "participant-1",
            validated = true
        )

        val result = unconfiguredService.createMeeting(
            eventId = "event-unconfigured-provider",
            organizerId = "organizer-1",
            platform = MeetingPlatform.ZOOM,
            title = "No provider",
            description = null,
            scheduledFor = Clock.System.now(),
            duration = 1.hours,
            timezone = "Europe/Paris"
        )

        assertTrue(result.isFailure)
        assertTrue(
            result.exceptionOrNull()?.message?.contains("provider", ignoreCase = true) == true,
            "Meeting creation should fail honestly when no real provider is configured"
        )
        assertTrue(database.meetingQueries.selectByEventId("event-unconfigured-provider").executeAsList().isEmpty())
    }

    @Test
    fun generateMeetingLinkFailsWhenMeetingLinkProviderIsNotConfigured() = runTest {
        val unconfiguredService = MeetingService(
            database = database,
            calendarService = CalendarService(database, NoopPlatformCalendarService()),
            notificationService = DefaultNotificationService()
        )

        val result = unconfiguredService.generateMeetingLink(
            platform = MeetingPlatform.GOOGLE_MEET,
            title = "No provider",
            description = null,
            scheduledFor = Clock.System.now(),
            duration = 1.hours
        )

        assertTrue(result.isFailure)
        assertTrue(
            result.exceptionOrNull()?.message?.contains("provider", ignoreCase = true) == true,
            "Link generation should fail honestly when no real provider is configured"
        )
    }

    @Test
    fun legacyMeetingPlatformProvidersDoNotFabricateLinks() = runTest {
        val providers = listOf(
            ZoomMeetingPlatformProvider() to MeetingPlatform.ZOOM,
            GoogleMeetPlatformProvider() to MeetingPlatform.GOOGLE_MEET,
            FaceTimePlatformProvider() to MeetingPlatform.FACETIME
        )

        providers.forEach { (provider, platform) ->
            assertFailsWith<IllegalStateException> {
                provider.generateMeetingLink(
                    platform = platform,
                    title = "No fake link",
                    description = null,
                    startTime = Clock.System.now(),
                    duration = 1.hours
                )
            }
        }
    }

    @Test
    fun sendInvitationsCreatesNotificationsForValidatedParticipants() = runTest {
        seedEvent(eventId = "event-1", status = "ORGANIZING")
        seedParticipant(eventId = "event-1", participantId = "user-1", userId = "user-1", validated = true)
        seedParticipant(eventId = "event-1", participantId = "user-2", userId = "user-2", validated = true)
        seedParticipant(eventId = "event-1", participantId = "user-3", userId = "user-3", validated = false)

        val meetingId = service.createMeeting(
            eventId = "event-1",
            organizerId = "organizer-1",
            platform = MeetingPlatform.ZOOM,
            title = "Meeting",
            description = null,
            scheduledFor = Clock.System.now(),
            duration = 1.hours,
            timezone = "Europe/Paris"
        ).getOrThrow().id

        val result = service.sendInvitations(meetingId)

        assertTrue(result.isSuccess)

        val invitations = database.meetingInvitationQueries.selectByMeetingId(meetingId).executeAsList()
        assertEquals(2, invitations.size) // Only validated participants
        assertTrue(invitations.any { it.participant_id == "user-1" })
        assertTrue(invitations.any { it.participant_id == "user-2" })
        assertFalse(invitations.any { it.participant_id == "user-3" })

        val invitationNotifications = notificationService.sentMessages.filter { it.title.startsWith("Invitation:") }
        assertEquals(2, invitationNotifications.size)
        assertTrue(invitationNotifications.any { it.userId == "user-1" })
        assertTrue(invitationNotifications.any { it.userId == "user-2" })
        assertFalse(invitationNotifications.any { it.userId == "user-3" })
    }

    private fun seedEvent(eventId: String, status: String) {
        database.eventQueries.insertEvent(
            id = eventId,
            organizerId = "organizer-1",
            title = "Event $eventId",
            description = "Description",
            status = status,
            deadline = "2026-01-01T00:00:00Z",
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-01-01T00:00:00Z",
            version = 1,
            eventType = "OTHER",
            eventTypeCustom = null,
            minParticipants = null,
            maxParticipants = null,
            expectedParticipants = null,
            isSample = 0L
        )
    }

    private fun seedParticipant(eventId: String, participantId: String, userId: String, validated: Boolean) {
        database.participantQueries.insertParticipant(
            id = participantId,
            eventId = eventId,
            userId = userId,
            role = "PARTICIPANT",
            hasValidatedDate = if (validated) 1L else 0L,
            joinedAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-01-01T00:00:00Z"
        )
    }

    private fun serviceWithCalendar(platformCalendarService: PlatformCalendarService): MeetingService {
        return MeetingService(
            database = database,
            calendarService = CalendarService(database, platformCalendarService),
            notificationService = RecordingNotificationService(),
            meetingLinkProvider = DeterministicMeetingLinkProvider()
        )
    }

    private fun serviceWithNotification(notificationService: NotificationServiceInterface): MeetingService {
        return MeetingService(
            database = database,
            calendarService = CalendarService(database, NoopPlatformCalendarService()),
            notificationService = notificationService,
            meetingLinkProvider = DeterministicMeetingLinkProvider()
        )
    }

    private fun serviceWithMeetingLinkProvider(meetingLinkProvider: MeetingLinkProvider): MeetingService {
        return MeetingService(
            database = database,
            calendarService = CalendarService(database, NoopPlatformCalendarService()),
            notificationService = RecordingNotificationService(),
            meetingLinkProvider = meetingLinkProvider
        )
    }
}

private class FailingCancelMeetingLinkProvider(
    private val message: String
) : MeetingLinkProvider {
    private val delegate = DeterministicMeetingLinkProvider()

    override suspend fun createMeeting(
        platform: MeetingPlatform,
        title: String,
        description: String?,
        scheduledFor: Instant,
        duration: Duration,
        timezone: String,
        participantLimit: Int?,
        requirePassword: Boolean,
        waitingRoom: Boolean
    ): MeetingDetails =
        delegate.createMeeting(
            platform = platform,
            title = title,
            description = description,
            scheduledFor = scheduledFor,
            duration = duration,
            timezone = timezone,
            participantLimit = participantLimit,
            requirePassword = requirePassword,
            waitingRoom = waitingRoom
        )

    override suspend fun generateMeetingLink(
        platform: MeetingPlatform,
        title: String,
        description: String?,
        scheduledFor: Instant,
        duration: Duration
    ): MeetingLinkResponse =
        delegate.generateMeetingLink(
            platform = platform,
            title = title,
            description = description,
            scheduledFor = scheduledFor,
            duration = duration
        )

    override fun cancelMeeting(platform: MeetingPlatform, hostMeetingId: String): Result<Unit> =
        Result.failure(IllegalStateException(message))
}

private class RecordingNotificationService : NotificationServiceInterface {
    val sentMessages = mutableListOf<NotificationMessage>()

    override suspend fun sendNotification(message: NotificationMessage): Result<Unit> {
        sentMessages += message
        return Result.success(Unit)
    }

    override suspend fun registerPushToken(token: PushToken): Result<Unit> = Result.success(Unit)

    override suspend fun unregisterPushToken(userId: String, deviceId: String): Result<Unit> = Result.success(Unit)

    override suspend fun getUnreadNotifications(userId: String): List<NotificationMessage> =
        sentMessages.filter { it.userId == userId && it.readAt == null }

    override suspend fun markAsRead(notificationId: String): Result<Unit> = Result.success(Unit)
}

private class FailingNotificationService(
    private val message: String
) : NotificationServiceInterface {
    override suspend fun sendNotification(message: NotificationMessage): Result<Unit> =
        Result.failure(IllegalStateException(this.message))

    override suspend fun registerPushToken(token: PushToken): Result<Unit> =
        Result.failure(IllegalStateException(message))

    override suspend fun unregisterPushToken(userId: String, deviceId: String): Result<Unit> =
        Result.failure(IllegalStateException(message))

    override suspend fun getUnreadNotifications(userId: String): List<NotificationMessage> = emptyList()

    override suspend fun markAsRead(notificationId: String): Result<Unit> =
        Result.failure(IllegalStateException(message))
}

private class NoopPlatformCalendarService : PlatformCalendarService {
    override fun addEvent(event: EnhancedCalendarEvent): Result<Unit> = Result.success(Unit)
    override fun updateEvent(event: EnhancedCalendarEvent): Result<Unit> = Result.success(Unit)
    override fun deleteEvent(eventId: String): Result<Unit> = Result.success(Unit)
}

private class FailingPlatformCalendarService(
    private val message: String
) : PlatformCalendarService {
    override fun addEvent(event: EnhancedCalendarEvent): Result<Unit> =
        Result.failure(IllegalStateException(message))

    override fun updateEvent(event: EnhancedCalendarEvent): Result<Unit> =
        Result.failure(IllegalStateException(message))

    override fun deleteEvent(eventId: String): Result<Unit> =
        Result.failure(IllegalStateException(message))
}
