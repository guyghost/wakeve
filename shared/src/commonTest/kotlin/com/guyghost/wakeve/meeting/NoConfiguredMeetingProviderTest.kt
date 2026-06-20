package com.guyghost.wakeve.meeting

import com.guyghost.wakeve.models.MeetingPlatform
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.hours

class NoConfiguredMeetingProviderTest {

    @Test
    fun `createMeeting fails when meeting provider is not configured`() = runTest {
        val result = NoConfiguredMeetingProvider.createMeeting(
            platform = MeetingPlatform.ZOOM,
            title = "Roadmap sync",
            description = null,
            scheduledFor = Instant.parse("2026-06-20T12:00:00Z"),
            duration = 1.hours,
            timezone = "Europe/Paris",
            participantLimit = null,
            requirePassword = true,
            waitingRoom = true
        )

        val error = assertIs<MeetingProviderException>(result.exceptionOrNull())
        assertEquals("Meeting provider is not configured for ZOOM", error.message)
    }

    @Test
    fun `platforms are unavailable when meeting provider is not configured`() {
        MeetingPlatform.entries.forEach { platform ->
            assertFalse(NoConfiguredMeetingProvider.isPlatformAvailable(platform))
            assertNull(NoConfiguredMeetingProvider.getAppUrl(platform))
        }
    }

    @Test
    fun `launchMeeting fails when meeting provider is not configured`() {
        val result = NoConfiguredMeetingProvider.launchMeeting("https://example.com/meeting")

        val error = assertIs<MeetingProviderException>(result.exceptionOrNull())
        assertEquals("Meeting provider is not configured", error.message)
    }
}
