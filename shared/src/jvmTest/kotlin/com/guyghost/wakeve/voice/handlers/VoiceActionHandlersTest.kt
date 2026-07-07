package com.guyghost.wakeve.voice.handlers

import com.guyghost.wakeve.calendar.CalendarService
import com.guyghost.wakeve.calendar.PlatformCalendarService
import com.guyghost.wakeve.createFreshTestDatabase
import com.guyghost.wakeve.models.EnhancedCalendarEvent
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.ml.Language
import com.guyghost.wakeve.ml.SessionStatus
import com.guyghost.wakeve.ml.VoiceCommand
import com.guyghost.wakeve.ml.VoiceContext
import com.guyghost.wakeve.ml.VoiceIntent
import com.guyghost.wakeve.ml.VoiceSession
import com.guyghost.wakeve.ml.VoiceStep
import com.guyghost.wakeve.repository.EventRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class VoiceActionHandlersTest {

    @Test
    fun `send invitations fails when delivery service is not configured`() = runTest {
        val repository = seededRepository()
        val handlers = VoiceActionHandlers(repository)

        val result = handlers.handleSendInvitations(
            session = session(),
            command = sendInvitationsCommand()
        )

        assertFailure(result, "Invitation delivery service is not configured")
    }

    @Test
    fun `send invitations propagates calendar generation failure`() = runTest {
        val repository = seededRepository()
        val handlers = VoiceActionHandlers(
            eventRepository = repository,
            calendarService = CalendarService(
                database = createFreshTestDatabase(),
                platformCalendarService = NoopPlatformCalendarService()
            )
        )

        val result = handlers.handleSendInvitations(
            session = session(),
            command = sendInvitationsCommand()
        )

        assertFalse(result.isSuccess)
        assertContains(result.exceptionOrNull()?.message.orEmpty(), "Event not found")
    }

    private suspend fun seededRepository(): EventRepository {
        val repository = EventRepository()
        repository.createEvent(
            Event(
                id = "event-voice-invite",
                title = "Voice Invite Event",
                description = "Event used by voice invitation tests",
                organizerId = "organizer-1",
                participants = listOf("organizer-1", "participant-1"),
                proposedSlots = listOf(
                    TimeSlot(
                        id = "slot-1",
                        start = "2026-07-01T10:00:00Z",
                        end = "2026-07-01T12:00:00Z",
                        timezone = "UTC"
                    )
                ),
                deadline = "2026-06-25T00:00:00Z",
                status = EventStatus.CONFIRMED,
                createdAt = "2026-06-20T10:00:00Z",
                updatedAt = "2026-06-20T10:00:00Z"
            )
        ).getOrThrow()
        return repository
    }

    private fun session(): VoiceSession = VoiceSession(
        sessionId = "voice-session-1",
        userId = "organizer-1",
        commands = emptyList(),
        context = VoiceContext(
            eventId = "event-voice-invite",
            step = VoiceStep.COMPLETE,
            language = Language.EN,
            suggestionsProvided = false
        ),
        status = SessionStatus.ACTIVE,
        startTime = "2026-06-20T10:00:00Z",
        endTime = null
    )

    private fun sendInvitationsCommand(): VoiceCommand = VoiceCommand(
        intent = VoiceIntent.SEND_INVITATIONS,
        parameters = emptyMap(),
        confidenceScore = 1.0,
        rawTranscript = "send invitations",
        timestamp = "2026-06-20T10:01:00Z"
    )

    private fun assertFailure(result: Result<*>, expectedMessage: String) {
        assertFalse(result.isSuccess)
        val error = assertIs<IllegalStateException>(result.exceptionOrNull())
        assertEquals(expectedMessage, error.message)
    }
}

private class NoopPlatformCalendarService : PlatformCalendarService {
    override fun addEvent(event: EnhancedCalendarEvent): Result<Unit> = Result.success(Unit)
    override fun updateEvent(event: EnhancedCalendarEvent): Result<Unit> = Result.success(Unit)
    override fun deleteEvent(eventId: String): Result<Unit> = Result.success(Unit)
}
