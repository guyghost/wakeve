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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
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

    @Test
    fun `createMeeting normalizes direct writes before persistence`() = runTest {
        val result = repository.createMeeting(
            meeting(" meeting-normalized ").copy(
                eventId = " event-1 ",
                organizerId = " organizer-1 ",
                title = "  Planning sync  ",
                description = "  Align the group  ",
                meetingLink = "  https://zoom.us/j/1234567890?pwd=ABC123  ",
                hostMeetingId = " host-normalized ",
                password = " secret ",
                invitedParticipants = listOf(" user-1 ", "user-1", "", " user-2 "),
                createdAt = " 2026-06-20T10:00:00Z "
            )
        )

        assertTrue(result.isSuccess)
        val persisted = repository.getMeetingById("meeting-normalized")
        assertNotNull(persisted)
        assertEquals("meeting-normalized", persisted.id)
        assertEquals("event-1", persisted.eventId)
        assertEquals("organizer-1", persisted.organizerId)
        assertEquals("Planning sync", persisted.title)
        assertEquals("Align the group", persisted.description)
        assertEquals("https://zoom.us/j/1234567890?pwd=ABC123", persisted.meetingLink)
        assertEquals("host-normalized", persisted.hostMeetingId)
        assertEquals("secret", persisted.password)
        assertEquals(listOf("user-1", "user-2"), persisted.invitedParticipants)
        assertEquals("2026-06-20T10:00:00Z", persisted.createdAt)
    }

    @Test
    fun `createMeeting rejects invalid direct writes before persistence`() = runTest {
        val invalidMeetings = listOf(
            meeting("meeting-blank-title").copy(title = " "),
            meeting("meeting-unsafe-url").copy(meetingLink = "https://example.com/meeting"),
            meeting("meeting-template-url").copy(meetingLink = "https://zoom.us/j/${'$'}MEETING_ID?pwd=ABC123"),
            meeting("meeting-zero-duration").copy(duration = Duration.ZERO),
            meeting("meeting-blank-event").copy(eventId = " ")
        )

        invalidMeetings.forEach { candidate ->
            val result = repository.createMeeting(candidate)

            assertTrue(result.isFailure)
            assertNull(repository.getMeetingById(candidate.id.trim()))
        }
        assertTrue(database.meetingQueries.selectByEventId("event-1").executeAsList().isEmpty())
    }

    @Test
    fun `updateMeeting normalizes direct updates and rejects invalid mutation`() = runTest {
        repository.createMeeting(meeting("meeting-1")).getOrThrow()

        val normalizedResult = repository.updateMeeting(
            id = " meeting-1 ",
            updates = MeetingUpdates(
                title = "  Updated sync  ",
                description = "  Updated notes  ",
                meetingLink = "  https://zoom.us/j/1234567890?pwd=XYZ789  "
            )
        )

        assertTrue(normalizedResult.isSuccess)
        val updated = repository.getMeetingById("meeting-1")
        assertNotNull(updated)
        assertEquals("Updated sync", updated.title)
        assertEquals("Updated notes", updated.description)
        assertEquals("https://zoom.us/j/1234567890?pwd=XYZ789", updated.meetingLink)

        val rejected = repository.updateMeeting(
            id = "meeting-1",
            updates = MeetingUpdates(title = " ")
        )

        assertTrue(rejected.isFailure)
        assertEquals("Updated sync", repository.getMeetingById("meeting-1")?.title)
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
        meetingLink = "https://zoom.us/j/1234567890?pwd=ABC123",
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
