package com.guyghost.wakeve.calendar

import com.guyghost.wakeve.models.EnhancedCalendarEvent
import platform.EventKit.EKEvent
import platform.EventKit.EKEventStore
import platform.Foundation.*

actual class PlatformCalendarService : PlatformCalendarService {
    private val store = EKEventStore()

    actual override fun addEvent(event: EnhancedCalendarEvent): Result<Unit> {
        return try {
            val startDate = NSDate(timeIntervalSince1970 = event.startDate.epochSeconds.toDouble())
            val endDate = NSDate(timeIntervalSince1970 = event.endDate.epochSeconds.toDouble())

            val ekEvent = EKEvent(eventStore = store).apply {
                this.title = event.title
                this.startDate = startDate
                this.endDate = endDate
                this.location = event.location
                this.notes = event.description
            }

            store.saveEvent(ekEvent, span = 0UL, commit = true, error = null)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual override fun updateEvent(event: EnhancedCalendarEvent): Result<Unit> {
        return try {
            val startDate = NSDate(timeIntervalSince1970 = event.startDate.epochSeconds.toDouble())
            val endDate = NSDate(timeIntervalSince1970 = event.endDate.epochSeconds.toDouble())

            val predicate = NSPredicate(format = "title == %@", argumentArray = listOf(event.title))
            val events = store.eventsMatchingPredicate(predicate)

            val ekEvent = events?.firstOrNull() as? EKEvent
                ?: return Result.failure(CalendarEventNotFoundException(event.id))

            ekEvent.title = event.title
            ekEvent.startDate = startDate
            ekEvent.endDate = endDate
            ekEvent.location = event.location
            ekEvent.notes = event.description

            store.saveEvent(ekEvent, span = 0UL, commit = true, error = null)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual override fun deleteEvent(eventId: String): Result<Unit> {
        return try {
            val predicate = NSPredicate(format = "title == %@", argumentArray = listOf(eventId))
            val events = store.eventsMatchingPredicate(predicate)

            val ekEvent = events?.firstOrNull() as? EKEvent
                ?: return Result.failure(CalendarEventNotFoundException(eventId))

            store.removeEvent(ekEvent, span = 0UL, commit = true, error = null)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class CalendarEventNotFoundException(eventId: String) : Exception("Calendar event not found: $eventId")