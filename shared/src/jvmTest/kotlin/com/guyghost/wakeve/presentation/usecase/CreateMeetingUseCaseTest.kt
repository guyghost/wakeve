package com.guyghost.wakeve.presentation.usecase

import com.guyghost.wakeve.meeting.MeetingService
import com.guyghost.wakeve.models.MeetingPlatform
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.hours

/**
 * Tests for CreateMeetingUseCase
 */
class CreateMeetingUseCaseTest {

    private lateinit var mockMeetingService: MockMeetingService
    private lateinit var mockRepository: com.guyghost.wakeve.repository.MockMeetingRepository
    private lateinit var createMeetingUseCase: ICreateMeetingUseCase

    @BeforeTest
    fun setup() {
        mockMeetingService = MockMeetingService()
        mockRepository = com.guyghost.wakeve.repository.MockMeetingRepository()
        createMeetingUseCase = CreateMeetingUseCaseWrapper(mockMeetingService, mockRepository)
    }

    @Test
    fun `create meeting with valid data succeeds`() = runTest {
        // Given
        val request = MeetingTestFixtures.createSampleCreateMeetingRequest(
            eventId = "event-1",
            organizerId = "user-1",
            title = "Test Meeting",
            platform = MeetingPlatform.ZOOM
        )

        // When
        val result = createMeetingUseCase(request)

        // Then
        assertTrue(result.isSuccess)
        val meeting = result.getOrNull()
        assertNotNull(meeting)
        assertEquals("Test Meeting", meeting.title)
        assertEquals(MeetingPlatform.ZOOM, meeting.platform)
        assertEquals("event-1", meeting.eventId)
        assertEquals("user-1", meeting.organizerId)
        assertTrue(meeting.requirePassword)
        assertTrue(meeting.waitingRoom)
    }

    @Test
    fun `create meeting with google meet platform succeeds`() = runTest {
        // Given
        val request = MeetingTestFixtures.createSampleCreateMeetingRequest(
            eventId = "event-1",
            organizerId = "user-1",
            platform = MeetingPlatform.GOOGLE_MEET,
            title = "Google Meet Test",
            requirePassword = false, // Google Meet doesn't require passwords
            waitingRoom = false   // Google Meet doesn't have waiting rooms
        )

        // When
        val result = createMeetingUseCase(request)

        // Then
        assertTrue(result.isSuccess)
        val meeting = result.getOrNull()
        assertNotNull(meeting)
        assertEquals(MeetingPlatform.GOOGLE_MEET, meeting.platform)
        assertTrue(meeting.meetingUrl.contains("meet.google.com"))
        assertFalse(meeting.requirePassword)
        assertFalse(meeting.waitingRoom)
    }

    @Test
    fun `create meeting with facetime platform succeeds`() = runTest {
        // Given
        val request = MeetingTestFixtures.createSampleCreateMeetingRequest(
            eventId = "event-1",
            organizerId = "user-1",
            platform = MeetingPlatform.FACETIME,
            title = "FaceTime Test",
            requirePassword = false, // FaceTime doesn't require passwords
            waitingRoom = false    // FaceTime doesn't have waiting rooms
        )

        // When
        val result = createMeetingUseCase(request)

        // Then
        assertTrue(result.isSuccess)
        val meeting = result.getOrNull()
        assertNotNull(meeting)
        assertEquals(MeetingPlatform.FACETIME, meeting.platform)
        assertTrue(meeting.meetingUrl.contains("facetime://"))
        assertFalse(meeting.requirePassword)
        assertFalse(meeting.waitingRoom)
    }

    @Test
    fun `create meeting fails when service fails`() = runTest {
        // Given
        mockMeetingService.shouldFailCreate = true
        val request = MeetingTestFixtures.createSampleCreateMeetingRequest()

        // When
        val result = createMeetingUseCase(request)

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun `create meeting generates unique id`() = runTest {
        // Given
        val request1 = MeetingTestFixtures.createSampleCreateMeetingRequest(
            eventId = "event-1",
            organizerId = "user-1",
            title = "Meeting 1"
        )
        val request2 = MeetingTestFixtures.createSampleCreateMeetingRequest(
            eventId = "event-1",
            organizerId = "user-1",
            title = "Meeting 2"
        )

        // When
        val result1 = createMeetingUseCase(request1)
        val result2 = createMeetingUseCase(request2)

        // Then
        assertTrue(result1.isSuccess)
        assertTrue(result2.isSuccess)
        val meeting1 = result1.getOrNull()!!
        val meeting2 = result2.getOrNull()!!
        assertTrue(meeting1.id != meeting2.id)
    }

    @Test
    fun `create meeting with custom duration succeeds`() = runTest {
        // Given
        val request = MeetingTestFixtures.createSampleCreateMeetingRequest(
            duration = 2.hours
        )

        // When
        val result = createMeetingUseCase(request)

        // Then
        assertTrue(result.isSuccess)
        val meeting = result.getOrNull()!!
        assertEquals(2.hours, meeting.duration)
    }
}