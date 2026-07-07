package com.guyghost.wakeve.organization

import com.guyghost.wakeve.calendar.CalendarService
import com.guyghost.wakeve.calendar.PlatformCalendarService
import com.guyghost.wakeve.createFreshTestDatabase
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.meeting.DeterministicMeetingLinkProvider
import com.guyghost.wakeve.meeting.MeetingService
import com.guyghost.wakeve.models.EnhancedCalendarEvent
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.MeetingPlatform
import com.guyghost.wakeve.models.NotificationMessage
import com.guyghost.wakeve.models.PushToken
import com.guyghost.wakeve.notification.NotificationServiceInterface
import com.guyghost.wakeve.payment.PaymentPotRepository
import com.guyghost.wakeve.payment.TricountHandoffRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours

class EventOrganizationPhase56SharedOfflineRedTest {

    private lateinit var database: WakeveDb
    private lateinit var meetingService: MeetingService
    private lateinit var paymentPotRepository: PaymentPotRepository
    private lateinit var tricountHandoffRepository: TricountHandoffRepository

    @BeforeTest
    fun setUp() {
        database = createFreshTestDatabase()
        meetingService = MeetingService(
            database = database,
            calendarService = CalendarService(database, Phase56NoopPlatformCalendarService()),
            notificationService = Phase56NoopNotificationService(),
            meetingLinkProvider = DeterministicMeetingLinkProvider()
        )
        paymentPotRepository = PaymentPotRepository(database)
        tricountHandoffRepository = TricountHandoffRepository(database)
    }

    @Test
    fun `Phase5_6 generated meeting URLs are concrete safe links without literal interpolation tokens`() = runTest {
        seedOrganizingEvent("phase5-6-safe-links")
        seedParticipant("phase5-6-safe-links", "organizer-1", role = "ORGANIZER", confirmed = true)

        val zoomMeeting = meetingService.createMeeting(
            eventId = "phase5-6-safe-links",
            organizerId = "organizer-1",
            platform = MeetingPlatform.ZOOM,
            title = "Zoom safe link regression",
            description = null,
            scheduledFor = Instant.parse("2026-07-18T08:00:00Z"),
            duration = 1.hours,
            timezone = "Europe/Paris"
        ).getOrThrow()

        val googleMeeting = meetingService.createMeeting(
            eventId = "phase5-6-safe-links",
            organizerId = "organizer-1",
            platform = MeetingPlatform.GOOGLE_MEET,
            title = "Meet safe link regression",
            description = null,
            scheduledFor = Instant.parse("2026-07-18T09:00:00Z"),
            duration = 1.hours,
            timezone = "Europe/Paris"
        ).getOrThrow()

        val failures = buildList {
            if (!Regex("""^https://zoom\.us/j/\d{10}\?pwd=[A-Z0-9]{6}$""").matches(zoomMeeting.meetingUrl)) {
                add("Zoom URL is not a concrete safe Zoom join URL: ${zoomMeeting.meetingUrl}")
            }
            if (!Regex("""^https://meet\.google\.com/[a-z]{3}-[a-z]{3}-[a-z]{4}$""").matches(googleMeeting.meetingUrl)) {
                add("Google Meet URL is not a concrete safe Meet join URL: ${googleMeeting.meetingUrl}")
            }

            listOf(zoomMeeting.meetingUrl, googleMeeting.meetingUrl).forEach { url ->
                val forbiddenTokens = listOf("\$", "\\$", "\${", "{meetingId}", "{meetCode}", "{password}", "{")
                val leakedTokens = forbiddenTokens.filter { it in url }
                if (leakedTokens.isNotEmpty()) {
                    add("Meeting URL leaks interpolation/template token(s) $leakedTokens: $url")
                }
            }
        }

        assertTrue(
            failures.isEmpty(),
            "Phase 5.6 meeting providers must persist real safe links without escaped/literal interpolation.\n" +
                failures.joinToString("\n")
        )
    }

    @Test
    fun `Phase5_6 non organizer meeting create is rejected and leaves no local write or sync queue`() = runTest {
        seedOrganizingEvent("phase5-6-non-organizer")
        seedParticipant("phase5-6-non-organizer", "organizer-1", role = "ORGANIZER", confirmed = true)
        seedParticipant("phase5-6-non-organizer", "confirmed-participant", confirmed = true)

        val result = meetingService.createMeeting(
            eventId = "phase5-6-non-organizer",
            organizerId = "confirmed-participant",
            platform = MeetingPlatform.ZOOM,
            title = "Participant should not create",
            description = null,
            scheduledFor = Instant.parse("2026-07-18T08:00:00Z"),
            duration = 1.hours,
            timezone = "Europe/Paris"
        )

        val persistedMeetings = database.meetingQueries
            .selectByEventId("phase5-6-non-organizer")
            .executeAsList()
        val queuedMeetingSync = database.syncMetadataQueries
            .selectPending()
            .executeAsList()
            .filter { it.entityType == "meeting" }

        val failures = buildList {
            if (result.isSuccess) {
                add("createMeeting succeeded for a confirmed participant that is not the event organizer")
            }
            if (persistedMeetings.isNotEmpty()) {
                add("Rejected non-organizer create persisted ${persistedMeetings.size} meeting row(s)")
            }
            if (queuedMeetingSync.isNotEmpty()) {
                add("Rejected non-organizer create queued ${queuedMeetingSync.size} sync operation(s)")
            }
        }

        assertTrue(
            failures.isEmpty(),
            "Phase 5.6 shared meeting creation must enforce an explicit organizer actor atomically.\n" +
                failures.joinToString("\n")
        )
    }

    @Test
    fun `Phase5_6 meeting local create queues replayable sync metadata`() = runTest {
        seedOrganizingEvent("phase5-6-meeting-sync")
        seedParticipant("phase5-6-meeting-sync", "organizer-1", role = "ORGANIZER", confirmed = true)

        val meeting = meetingService.createMeeting(
            eventId = "phase5-6-meeting-sync",
            organizerId = "organizer-1",
            platform = MeetingPlatform.GOOGLE_MEET,
            title = "Replayable meeting sync",
            description = "Must survive offline replay",
            scheduledFor = Instant.parse("2026-07-18T08:00:00Z"),
            duration = 1.hours,
            timezone = "Europe/Paris"
        ).getOrThrow()

        assertReplayableSyncMetadata(
            entityType = "meeting",
            entityId = meeting.id,
            expectedOperation = "CREATE",
            expectedPayloadSnippets = listOf(
                "\"eventId\":\"phase5-6-meeting-sync\"",
                "\"title\":\"Replayable meeting sync\"",
                "\"meetingLink\":"
            )
        )
    }

    @Test
    fun `Phase5_6 payment pot local create and close queue replayable sync metadata`() {
        seedOrganizingEvent("phase5-6-payment-pot-sync")
        seedParticipant("phase5-6-payment-pot-sync", "organizer-1", role = "ORGANIZER", confirmed = true)

        val pot = paymentPotRepository.createPot(
            eventId = "phase5-6-payment-pot-sync",
            organizerId = "organizer-1",
            goalAmount = 600.0,
            title = "Shared settlement pot",
            tricountGroupId = "tricount-123",
            tricountGroupUrl = "https://www.tricount.com/group/tricount-123"
        )
        paymentPotRepository.closePot(pot.id)

        val potSyncOperations = database.syncMetadataQueries
            .selectByEntity("payment_pot", pot.id)
            .executeAsList()
        val operations = potSyncOperations.map { it.operation }.toSet()

        assertTrue(
            setOf("CREATE", "UPDATE").all { it in operations },
            "Phase 5.6 payment pot create/close must queue replayable CREATE and UPDATE sync metadata; got $operations"
        )
        potSyncOperations.forEach { sync ->
            assertEquals(0L, sync.synced)
            assertEquals("READY", sync.retryState)
            assertEquals(0L, sync.retryCount)
            assertTrue(sync.payload.contains("\"eventId\":\"phase5-6-payment-pot-sync\""))
            assertTrue(sync.payload.contains("\"id\":\"${pot.id}\""))
            assertTrue(sync.payload != "{}")
        }
    }

    @Test
    fun `Phase5_6 Tricount local mutations queue replayable sync metadata`() {
        seedOrganizingEvent("phase5-6-tricount-sync")
        seedParticipant("phase5-6-tricount-sync", "organizer-1", role = "ORGANIZER", confirmed = true)

        tricountHandoffRepository.linkHandoff(
            eventId = "phase5-6-tricount-sync",
            provider = "TRICOUNT",
            providerId = "tricount-456",
            providerUrl = "https://tricount.com/group/tricount-456",
            syncStatus = "LINKED"
        )
        tricountHandoffRepository.markNotNeeded(
            eventId = "phase5-6-tricount-sync",
            decidedBy = "organizer-1"
        )

        val tricountSyncOperations = database.syncMetadataQueries
            .selectByEntity("tricount_handoff", "phase5-6-tricount-sync")
            .executeAsList()
        val operations = tricountSyncOperations.map { it.operation }.toSet()

        assertTrue(
            setOf("CREATE", "UPDATE").all { it in operations },
            "Phase 5.6 Tricount link/not-needed mutations must queue replayable CREATE and UPDATE sync metadata; got $operations"
        )
        tricountSyncOperations.forEach { sync ->
            assertEquals(0L, sync.synced)
            assertEquals("READY", sync.retryState)
            assertEquals(0L, sync.retryCount)
            assertTrue(sync.payload.contains("\"eventId\":\"phase5-6-tricount-sync\""))
            assertTrue(sync.payload.contains("\"provider\":\"TRICOUNT\""))
            assertTrue(sync.payload != "{}")
        }
    }

    private fun assertReplayableSyncMetadata(
        entityType: String,
        entityId: String,
        expectedOperation: String,
        expectedPayloadSnippets: List<String>
    ) {
        val pending = database.syncMetadataQueries
            .selectByEntity(entityType, entityId)
            .executeAsList()
            .singleOrNull { it.operation == expectedOperation }

        assertTrue(
            pending != null,
            "Expected pending $expectedOperation sync metadata for $entityType/$entityId"
        )
        assertEquals(0L, pending.synced)
        assertEquals("READY", pending.retryState)
        assertEquals(0L, pending.retryCount)
        assertTrue(pending.payload != "{}")
        expectedPayloadSnippets.forEach { snippet ->
            assertTrue(
                pending.payload.contains(snippet),
                "Replayable $entityType payload must contain $snippet, got ${pending.payload}"
            )
        }
    }

    private fun seedOrganizingEvent(eventId: String) {
        val now = "2026-05-22T10:00:00Z"
        database.eventQueries.insertEvent(
            id = eventId,
            organizerId = "organizer-1",
            title = "Phase 5.6 Event",
            description = "Shared/offline regression event",
            status = EventStatus.ORGANIZING.name,
            deadline = "2026-06-01T00:00:00Z",
            createdAt = now,
            updatedAt = now,
            version = 1,
            eventType = "OTHER",
            eventTypeCustom = null,
            minParticipants = null,
            maxParticipants = null,
            expectedParticipants = null,
            isSample = 0L
        )
    }

    private fun seedParticipant(
        eventId: String,
        userId: String,
        role: String = "PARTICIPANT",
        confirmed: Boolean
    ) {
        database.participantQueries.insertParticipant(
            id = userId,
            eventId = eventId,
            userId = userId,
            role = role,
            hasValidatedDate = if (confirmed) 1L else 0L,
            joinedAt = "2026-05-22T10:05:00Z",
            updatedAt = "2026-05-22T10:05:00Z"
        )
    }
}

private class Phase56NoopPlatformCalendarService : PlatformCalendarService {
    override fun addEvent(event: EnhancedCalendarEvent): Result<Unit> = Result.success(Unit)
    override fun updateEvent(event: EnhancedCalendarEvent): Result<Unit> = Result.success(Unit)
    override fun deleteEvent(eventId: String): Result<Unit> = Result.success(Unit)
}

private class Phase56NoopNotificationService : NotificationServiceInterface {
    override suspend fun sendNotification(message: NotificationMessage): Result<Unit> = Result.success(Unit)
    override suspend fun registerPushToken(token: PushToken): Result<Unit> = Result.success(Unit)
    override suspend fun unregisterPushToken(userId: String, deviceId: String): Result<Unit> = Result.success(Unit)
    override suspend fun getUnreadNotifications(userId: String): List<NotificationMessage> = emptyList()
    override suspend fun markAsRead(notificationId: String): Result<Unit> = Result.success(Unit)
}
