package com.guyghost.wakeve.meeting

import com.guyghost.wakeve.createFreshTestDatabase
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.MeetingPlatform
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours

class MeetingRepositoryContractTest {

    private lateinit var database: WakeveDb
    private lateinit var repository: MeetingRepository

    @BeforeTest
    fun setUp() {
        database = createFreshTestDatabase()
        repository = MeetingRepository(database)
        seedEvent("event-1")
    }

    @Test
    fun `updateMeetingStatus fails when meeting does not exist`() = runTest {
        val result = repository.updateMeetingStatus("missing-meeting", MeetingStatus.CANCELLED)

        assertFalse(result.isSuccess)
        assertIs<MeetingNotFoundException>(result.exceptionOrNull())
    }

    @Test
    fun `deleteMeeting fails when meeting does not exist`() = runTest {
        val result = repository.deleteMeeting("missing-meeting")

        assertFalse(result.isSuccess)
        assertIs<MeetingNotFoundException>(result.exceptionOrNull())
    }

    @Test
    fun `updateMeetingStatus updates existing meeting`() = runTest {
        repository.createMeeting(meeting("meeting-1")).getOrThrow()

        val result = repository.updateMeetingStatus("meeting-1", MeetingStatus.CANCELLED)

        assertTrue(result.isSuccess)
        assertEquals(MeetingStatus.CANCELLED, repository.getMeetingById("meeting-1")?.status)
    }

    @Test
    fun `deleteMeeting removes existing meeting`() = runTest {
        repository.createMeeting(meeting("meeting-1")).getOrThrow()

        val result = repository.deleteMeeting("meeting-1")

        assertTrue(result.isSuccess)
        assertNull(repository.getMeetingById("meeting-1"))
    }

    private fun meeting(id: String): Meeting = Meeting(
        id = id,
        eventId = "event-1",
        organizerId = "organizer-1",
        title = "Planning",
        description = null,
        startTime = Instant.parse("2026-06-20T12:00:00Z"),
        duration = 1.hours,
        platform = MeetingPlatform.ZOOM,
        meetingLink = "https://example.com/meeting",
        hostMeetingId = "host-$id",
        password = "secret",
        invitedParticipants = emptyList(),
        status = MeetingStatus.SCHEDULED,
        createdAt = "2026-06-20T10:00:00Z"
    )

    private fun seedEvent(eventId: String) {
        database.eventQueries.insertEvent(
            id = eventId,
            organizerId = "organizer-1",
            title = "Event",
            description = "Description",
            status = "ORGANIZING",
            deadline = "2026-06-30T00:00:00Z",
            createdAt = "2026-06-20T00:00:00Z",
            updatedAt = "2026-06-20T00:00:00Z",
            version = 1,
            eventType = "OTHER",
            eventTypeCustom = null,
            minParticipants = null,
            maxParticipants = null,
            expectedParticipants = null,
            isSample = 0L
        )
    }
}
