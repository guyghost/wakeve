package com.guyghost.wakeve.routes

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.guyghost.wakeve.calendar.CalendarService
import com.guyghost.wakeve.calendar.PlatformCalendarServiceImpl
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.TimeOfDay
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.repository.DatabaseEventRepository
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Regression tests for BUG-9 (QA session 2026-09-18): the generated ICS must
 * use RFC 5545 compliant VALARM trigger durations and must address the
 * explicit invitee list instead of only internal guest accounts.
 */
class IcsInvitationInviteesTest {

    private lateinit var db: WakeveDb

    @BeforeTest
    fun setup() {
        val jdbcUrl = "jdbc:sqlite:${Files.createTempFile("wakeve-ics-", ".db")}"
        val driver = JdbcSqliteDriver(jdbcUrl)
        WakeveDb.Schema.create(driver)
        db = WakeveDb(driver)
    }

    private fun seedEvent() {
        val repository = DatabaseEventRepository(db)
        val slot = TimeSlot(
            id = "slot-1",
            start = "2026-12-05T12:00:00Z",
            end = "2026-12-12T22:00:00Z",
            timezone = "America/Sao_Paulo",
            timeOfDay = TimeOfDay.ALL_DAY
        )
        val event = Event(
            id = "event-ics",
            title = "Vacances à Rio",
            description = "QA ICS",
            organizerId = "organizer-1",
            participants = listOf("organizer-1"),
            proposedSlots = listOf(slot),
            deadline = "2099-01-01T00:00:00Z",
            status = EventStatus.CONFIRMED,
            createdAt = "2026-09-18T19:00:00Z",
            updatedAt = "2026-09-18T19:00:00Z"
        )
        runBlocking { repository.createEvent(event).getOrThrow() }
    }

    @Test
    fun `explicit invitees appear as attendees`() = runBlocking {
        seedEvent()
        val service = CalendarService(db, PlatformCalendarServiceImpl())

        val ics = service.generateICSInvitation(
            eventId = "event-ics",
            invitees = listOf("alice@example.com", "bob@example.com")
        )

        assertTrue(ics.content.contains("mailto:alice@example.com"), ics.content)
        assertTrue(ics.content.contains("mailto:bob@example.com"), ics.content)
        assertTrue(ics.content.contains("CN=alice"), ics.content)
    }

    @Test
    fun `valarm triggers use RFC 5545 durations`() = runBlocking {
        seedEvent()
        val service = CalendarService(db, PlatformCalendarServiceImpl())

        val ics = service.generateICSInvitation(eventId = "event-ics", invitees = listOf("alice@example.com"))

        assertTrue(ics.content.contains("TRIGGER:-P1D"), ics.content)
        assertTrue(ics.content.contains("TRIGGER:-P1W"), ics.content)
        // The previously generated compact form is not a valid RFC 5545 duration.
        assertTrue(!ics.content.contains("TRIGGER:-P1DT090000"), ics.content)
        Unit
    }
}
