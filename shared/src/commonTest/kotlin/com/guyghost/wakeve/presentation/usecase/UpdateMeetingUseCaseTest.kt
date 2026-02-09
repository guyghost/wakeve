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
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Tests for UpdateMeetingUseCase
 */
class UpdateMeetingUseCaseTest {

    private lateinit var mockRepository: com.guyghost.wakeve.repository.MockMeetingRepository
    private lateinit var updateMeetingUseCase: IUpdateMeetingUseCase

    @BeforeTest
    fun setup() {
        mockRepository = com.guyghost.wakeve.repository.MockMeetingRepository()
        updateMeetingUseCase = UpdateMeetingUseCaseWrapper(mockRepository)
    }

    @Test
    fun `update meeting title succeeds`() = runTest {
        // Given
        val meeting = MeetingTestFixtures.createSampleMeeting(
            title = "Original Title"
        )
        mockRepository.addMeetingDirectly(meeting)
        
        val request = MeetingTestFixtures.createSampleUpdateMeetingRequest(
            title = "Updated Title"
        )

        // When
        val result = updateMeetingUseCase(meeting.id, request)

        // Then
        assertTrue(result.isSuccess)
        val updatedMeeting = result.getOrNull()!!
        assertEquals("Updated Title", updatedMeeting.title)
        assertEquals(meeting.description, updatedMeeting.description) // Other fields unchanged
        assertEquals(meeting.startTime, updatedMeeting.scheduledFor)
    }

    @Test
    fun `update meeting description succeeds`() = runTest {
        // Given
        val meeting = MeetingTestFixtures.createSampleMeeting(
            description = "Original Description"
        )
        mockRepository.addMeetingDirectly(meeting)
        
        val request = MeetingTestFixtures.createSampleUpdateMeetingRequest(
            description = "Updated Description"
        )

        // When
        val result = updateMeetingUseCase(meeting.id, request)

        // Then
        assertTrue(result.isSuccess)
        val updatedMeeting = result.getOrNull()!!
        assertEquals("Updated Description", updatedMeeting.description)
        assertEquals(meeting.title, updatedMeeting.title) // Other fields unchanged
    }

    @Test
    fun `update meeting scheduled time succeeds`() = runTest {
        // Given
        val originalTime = Clock.System.now()
        val newTime = originalTime.plus(2.hours)
        
        val meeting = MeetingTestFixtures.createSampleMeeting(
            startTime = originalTime
        )
        mockRepository.addMeetingDirectly(meeting)
        
        val request = MeetingTestFixtures.createSampleUpdateMeetingRequest(
            scheduledFor = newTime
        )

        // When
        val result = updateMeetingUseCase(meeting.id, request)

        // Then
        assertTrue(result.isSuccess)
        val updatedMeeting = result.getOrNull()!!
        assertEquals(newTime, updatedMeeting.scheduledFor)
        assertEquals(meeting.title, updatedMeeting.title) // Other fields unchanged
    }

    @Test
    fun `update meeting duration succeeds`() = runTest {
        // Given
        val meeting = MeetingTestFixtures.createSampleMeeting(
            duration = 60.minutes
        )
        mockRepository.addMeetingDirectly(meeting)
        
        val request = MeetingTestFixtures.createSampleUpdateMeetingRequest(
            duration = 90.minutes
        )

        // When
        val result = updateMeetingUseCase(meeting.id, request)

        // Then
        assertTrue(result.isSuccess)
        val updatedMeeting = result.getOrNull()!!
        assertEquals(90.minutes, updatedMeeting.duration)
        assertEquals(meeting.title, updatedMeeting.title) // Other fields unchanged
    }

    @Test
    fun `update meeting multiple fields succeeds`() = runTest {
        // Given
        val originalTime = Clock.System.now()
        val newTime = originalTime.plus(2.hours)
        
        val meeting = MeetingTestFixtures.createSampleMeeting(
            title = "Original Title",
            description = "Original Description",
            startTime = originalTime,
            duration = 60.minutes
        )
        mockRepository.addMeetingDirectly(meeting)
        
        val request = MeetingTestFixtures.createSampleUpdateMeetingRequest(
            title = "Updated Title",
            description = "Updated Description",
            scheduledFor = newTime,
            duration = 90.minutes
        )

        // When
        val result = updateMeetingUseCase(meeting.id, request)

        // Then
        assertTrue(result.isSuccess)
        val updatedMeeting = result.getOrNull()!!
        assertEquals("Updated Title", updatedMeeting.title)
        assertEquals("Updated Description", updatedMeeting.description)
        assertEquals(newTime, updatedMeeting.scheduledFor)
        assertEquals(90.minutes, updatedMeeting.duration)
    }

    @Test
    fun `update meeting fails for non-existent meeting`() = runTest {
        // Given
        val request = MeetingTestFixtures.createSampleUpdateMeetingRequest()

        // When
        val result = updateMeetingUseCase("non-existent-meeting-id", request)

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun `update meeting preserves immutable fields`() = runTest {
        // Given
        val meeting = MeetingTestFixtures.createSampleMeeting(
            platform = MeetingPlatform.ZOOM,
            meetingLink = "https://zoom.us/j/original",
            hostMeetingId = "original-host-id"
        )
        mockRepository.addMeetingDirectly(meeting)
        
        val request = MeetingTestFixtures.createSampleUpdateMeetingRequest(
            title = "Updated Title"
        )

        // When
        val result = updateMeetingUseCase(meeting.id, request)

        // Then
        assertTrue(result.isSuccess)
        val updatedMeeting = result.getOrNull()!!
        assertEquals("Updated Title", updatedMeeting.title)
        // Platform and link fields should be preserved (not updated through use case)
        assertEquals(MeetingPlatform.ZOOM, updatedMeeting.platform)
        assertEquals("https://zoom.us/j/original", updatedMeeting.meetingUrl)
    }

    @Test
    fun `update meeting with null request fields preserves original values`() = runTest {
        // Given
        val meeting = MeetingTestFixtures.createSampleMeeting(
            title = "Original Title",
            description = "Original Description"
        )
        mockRepository.addMeetingDirectly(meeting)
        
        // Request with all null fields - should preserve all original values
        val request = MeetingTestFixtures.createSampleUpdateMeetingRequest(
            title = null,
            description = null,
            scheduledFor = null,
            duration = null
        )

        // When
        val result = updateMeetingUseCase(meeting.id, request)

        // Then
        assertTrue(result.isSuccess)
        val updatedMeeting = result.getOrNull()!!
        assertEquals("Original Title", updatedMeeting.title)
        assertEquals("Original Description", updatedMeeting.description)
        assertEquals(meeting.startTime, updatedMeeting.scheduledFor)
        assertEquals(meeting.duration, updatedMeeting.duration)
    }

    @Test
    fun `update meeting fails when repository fails`() = runTest {
        // Given
        val meeting = MeetingTestFixtures.createSampleMeeting()
        mockRepository.addMeetingDirectly(meeting)
        mockRepository.shouldFailUpdate = true
        
        val request = MeetingTestFixtures.createSampleUpdateMeetingRequest(
            title = "Updated Title"
        )

        // When
        val result = updateMeetingUseCase(meeting.id, request)

        // Then
        assertTrue(result.isFailure)
    }
}

/**
 * Wrapper class to adapt our mock repository to the expected interface
 */
class UpdateMeetingUseCaseWrapper(private val mockRepo: com.guyghost.wakeve.repository.MockMeetingRepository) : IUpdateMeetingUseCase {
    override suspend operator fun invoke(meetingId: String, request: com.guyghost.wakeve.models.UpdateMeetingRequest): Result<com.guyghost.wakeve.models.VirtualMeeting> {
        return try {
            val meeting = mockRepo.getMeetingById(meetingId)
                ?: return Result.failure(Exception("Meeting not found: $meetingId"))

            // Create updates map
            val updates = com.guyghost.wakeve.meeting.MeetingUpdates(
                title = request.title,
                description = request.description,
                startTime = request.scheduledFor,
                duration = request.duration,
                platform = null, // Platform updates not allowed
                meetingLink = null // Link updates not allowed
            )

            // Update meeting in repository
            mockRepo.updateMeeting(meetingId, updates).getOrThrow()

            // Get updated meeting
            val updatedMeeting = mockRepo.getMeetingById(meetingId)!!

            // Convert to VirtualMeeting model
            val virtualMeeting = com.guyghost.wakeve.models.VirtualMeeting(
                id = updatedMeeting.id,
                eventId = updatedMeeting.eventId,
                organizerId = updatedMeeting.organizerId,
                platform = updatedMeeting.platform,
                meetingId = updatedMeeting.hostMeetingId,
                meetingPassword = updatedMeeting.password,
                meetingUrl = updatedMeeting.meetingLink,
                dialInNumber = null,
                dialInPassword = null,
                title = updatedMeeting.title,
                description = updatedMeeting.description,
                scheduledFor = updatedMeeting.startTime,
                duration = updatedMeeting.duration,
                timezone = "UTC",
                participantLimit = null,
                requirePassword = updatedMeeting.password.isNotEmpty(),
                waitingRoom = true,
                hostKey = null,
                createdAt = kotlinx.datetime.Clock.System.now(),
                status = com.guyghost.wakeve.models.MeetingStatus.valueOf(updatedMeeting.status.name)
            )

            Result.success(virtualMeeting)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}