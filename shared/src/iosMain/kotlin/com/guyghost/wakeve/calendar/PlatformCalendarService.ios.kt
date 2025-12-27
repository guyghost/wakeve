package com.guyghost.wakeve.calendar

import com.guyghost.wakeve.models.EnhancedCalendarEvent
import platform.EventKit.*

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
class PlatformCalendarServiceImpl : PlatformCalendarService {
    private val store = EKEventStore()

    override fun addEvent(event: EnhancedCalendarEvent): Result<Unit> {
        return try {
            val startDate = NSDate(timeIntervalSince1970 = event.startDate.epochSeconds.toDouble())
            val endDate = NSDate(timeIntervalSince1970 = event.endDate.epochSeconds.toDouble())

            val ekEvent = EKEvent.eventStore(store)
            ekEvent.title = event.title
            ekEvent.startDate = startDate
            ekEvent.endDate = endDate
            ekEvent.location = event.location
            ekEvent.notes = event.description

            val errorRef = objc_ref<NSError?>(null)
            store.saveEvent(ekEvent, span = 0, error = errorRef)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun updateEvent(event: EnhancedCalendarEvent): Result<Unit> {
        return try {
            val startDate = NSDate(timeIntervalSince1970 = event.startDate.epochSeconds.toDouble())
            val endDate = NSDate(timeIntervalSince1970 = event.endDate.epochSeconds.toDouble())

            val predicate = NSPredicate.predicateWithFormat("title == %@", event.title)
            val events = store.eventsMatchingPredicate(predicate)

            val ekEvent = events?.firstOrNull() as? EKEvent
                ?: return Result.failure(CalendarEventNotFoundException(event.id))

            ekEvent.title = event.title
            ekEvent.startDate = startDate
            ekEvent.endDate = endDate
            ekEvent.location = event.location
            ekEvent.notes = event.description

            val errorRef = objc_ref<NSError?>(null)
            store.saveEvent(ekEvent, span = 0, error = errorRef)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun deleteEvent(eventId: String): Result<Unit> {
        return try {
            val predicate = NSPredicate.predicateWithFormat("title == %@", eventId)
            val events = store.eventsMatchingPredicate(predicate)

            val ekEvent = events?.firstOrNull() as? EKEvent
                ?: return Result.failure(CalendarEventNotFoundException(eventId))

            val errorRef = objc_ref<NSError?>(null)
            store.removeEvent(ekEvent, span = 0, error = errorRef)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class CalendarEventNotFoundException(eventId: String) : Exception("Calendar event not found: $eventId")
