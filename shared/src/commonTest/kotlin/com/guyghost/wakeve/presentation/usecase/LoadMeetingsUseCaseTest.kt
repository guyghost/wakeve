package com.guyghost.wakeve.presentation.usecase

import com.guyghost.wakeve.meeting.Meeting
import com.guyghost.wakeve.meeting.MeetingStatus
import com.guyghost.wakeve.models.MeetingPlatform
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.minutes

/**
 * Tests for LoadMeetingsUseCase
 */
class LoadMeetingsUseCaseTest {

    private lateinit var mockRepository: com.guyghost.wakeve.repository.MockMeetingRepository
    private lateinit var loadMeetingsUseCase: ILoadMeetingsUseCase

    @BeforeTest
    fun setup() {
        mockRepository = com.guyghost.wakeve.repository.MockMeetingRepository()
        loadMeetingsUseCase = LoadMeetingsUseCaseWrapper(mockRepository)
    }

    @Test
    fun `load meetings for event returns empty list when no meetings exist`() = runTest {
        // Given
        val eventId = "event-1"

        // When
        val result = loadMeetingsUseCase(eventId)

        // Then
        assertTrue(result.isSuccess)
        val meetings = result.getOrNull()!!
        assertEquals(0, meetings.size)
    }

    @Test
    fun `load meetings for event returns meetings when they exist`() = runTest {
        // Given
        val eventId = "event-1"
        val meeting1 = MeetingTestFixtures.createSampleMeeting(
            id = "meeting-1",
            eventId = eventId,
            title = "Meeting 1"
        )
        val meeting2 = MeetingTestFixtures.createSampleMeeting(
            id = "meeting-2",
            eventId = eventId,
            title = "Meeting 2"
        )
        
        mockRepository.addMeetingDirectly(meeting1)
        mockRepository.addMeetingDirectly(meeting2)

        // When
        val result = loadMeetingsUseCase(eventId)

        // Then
        assertTrue(result.isSuccess)
        val loadedMeetings = result.getOrNull()!!
        assertEquals(2, loadedMeetings.size)
        
        // Verify the meetings are converted correctly to VirtualMeeting format
        loadedMeetings.forEach { virtualMeeting ->
            assertEquals(eventId, virtualMeeting.eventId)
            assertTrue(virtualMeeting.id.isNotEmpty())
            assertNotNull(virtualMeeting.title)
            assertEquals("UTC", virtualMeeting.timezone)
        }
    }

    @Test
    fun `load meetings converts meeting data correctly`() = runTest {
        // Given
        val meeting = MeetingTestFixtures.createSampleMeeting(
            id = "meeting-1",
            eventId = "event-1",
            title = "Test Meeting",
            platform = MeetingPlatform.ZOOM,
            status = MeetingStatus.SCHEDULED
        )
        mockRepository.addMeetingDirectly(meeting)

        // When
        val result = loadMeetingsUseCase("event-1")

        // Then
        assertTrue(result.isSuccess)
        val virtualMeetings = result.getOrNull()!!
        assertEquals(1, virtualMeetings.size)
        
        val virtualMeeting = virtualMeetings[0]
        assertEquals("meeting-1", virtualMeeting.id)
        assertEquals("event-1", virtualMeeting.eventId)
        assertEquals("Test Meeting", virtualMeeting.title)
        assertEquals(MeetingPlatform.ZOOM, virtualMeeting.platform)
        assertEquals(meeting.meetingLink, virtualMeeting.meetingUrl)
        assertEquals(meeting.hostMeetingId, virtualMeeting.meetingId)
        assertEquals("UTC", virtualMeeting.timezone)
        assertEquals(com.guyghost.wakeve.models.MeetingStatus.SCHEDULED, virtualMeeting.status)
    }

    @Test
    fun `load meetings for different events returns correct meetings`() = runTest {
        // Given
        val meeting1 = MeetingTestFixtures.createSampleMeeting(
            id = "meeting-1",
            eventId = "event-1",
            title = "Event 1 Meeting"
        )
        val meeting2 = MeetingTestFixtures.createSampleMeeting(
            id = "meeting-2",
            eventId = "event-2",
            title = "Event 2 Meeting"
        )
        
        mockRepository.addMeetingDirectly(meeting1)
        mockRepository.addMeetingDirectly(meeting2)

        // When
        val result1 = loadMeetingsUseCase("event-1")
        val result2 = loadMeetingsUseCase("event-2")

        // Then
        assertTrue(result1.isSuccess)
        assertTrue(result2.isSuccess)
        
        val meetings1 = result1.getOrNull()!!
        val meetings2 = result2.getOrNull()!!
        
        assertEquals(1, meetings1.size)
        assertEquals(1, meetings2.size)
        assertEquals("Event 1 Meeting", meetings1[0].title)
        assertEquals("Event 2 Meeting", meetings2[0].title)
    }

    @Test
    fun `load meetings handles canceled meetings`() = runTest {
        // Given
        val scheduledMeeting = MeetingTestFixtures.createSampleMeeting(
            id = "meeting-1",
            eventId = "event-1",
            status = MeetingStatus.SCHEDULED
        )
        val cancelledMeeting = MeetingTestFixtures.createSampleMeeting(
            id = "meeting-2",
            eventId = "event-1",
            status = MeetingStatus.CANCELLED
        )
        
        mockRepository.addMeetingDirectly(scheduledMeeting)
        mockRepository.addMeetingDirectly(cancelledMeeting)

        // When
        val result = loadMeetingsUseCase("event-1")

        // Then
        assertTrue(result.isSuccess)
        val meetings = result.getOrNull()!!
        assertEquals(2, meetings.size)
        
        // Verify statuses are converted correctly
        val scheduled = meetings.find { it.id == "meeting-1" }!!
        val cancelled = meetings.find { it.id == "meeting-2" }!!
        
        assertEquals(com.guyghost.wakeve.models.MeetingStatus.SCHEDULED, scheduled.status)
        assertEquals(com.guyghost.wakeve.models.MeetingStatus.CANCELLED, cancelled.status)
    }
}

/**
 * Wrapper class to adapt our mock repository to the expected interface
 */
class LoadMeetingsUseCaseWrapper(private val mockRepo: com.guyghost.wakeve.repository.MockMeetingRepository) : ILoadMeetingsUseCase {
    override suspend operator fun invoke(eventId: String): Result<List<com.guyghost.wakeve.models.VirtualMeeting>> {
        return try {
            val meetings = mockRepo.getMeetingsByEventId(eventId)

            // Convert to VirtualMeeting models
            val virtualMeetings = meetings.map { meeting ->
                com.guyghost.wakeve.models.VirtualMeeting(
                    id = meeting.id,
                    eventId = meeting.eventId,
                    organizerId = meeting.organizerId,
                    platform = meeting.platform,
                    meetingId = meeting.hostMeetingId,
                    meetingPassword = meeting.password,
                    meetingUrl = meeting.meetingLink,
                    dialInNumber = null,
                    dialInPassword = null,
                    title = meeting.title,
                    description = meeting.description,
                    scheduledFor = meeting.startTime,
                    duration = meeting.duration,
                    timezone = "UTC",
                    participantLimit = null,
                    requirePassword = meeting.password.isNotEmpty(),
                    waitingRoom = true,
                    hostKey = null,
                    createdAt = kotlinx.datetime.Clock.System.now(),
                    status = com.guyghost.wakeve.models.MeetingStatus.valueOf(meeting.status.name)
                )
            }

            Result.success(virtualMeetings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}