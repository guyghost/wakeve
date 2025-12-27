package com.guyghost.wakeve.calendar

import com.guyghost.wakeve.models.EnhancedCalendarEvent
import platform.EventKit.*
import platform.Foundation.*

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
class PlatformCalendarServiceImpl : PlatformCalendarService {
    private val store = EKEventStore()

    override fun addEvent(event: EnhancedCalendarEvent): Result<Unit> {
        return try {
            val startDate = NSDate.dateWithTimeIntervalSince1970(event.startDate.epochSeconds.toDouble())
            val endDate = NSDate.dateWithTimeIntervalSince1970(event.endDate.epochSeconds.toDouble())

            val ekEvent = EKEvent.eventWithEventStore(store)
            ekEvent.title = event.title
            ekEvent.startDate = startDate
            ekEvent.endDate = endDate
            ekEvent.location = event.location
            ekEvent.notes = event.description

            val success = store.saveEvent(ekEvent, span = EKSpan.EKSpanThisEvent, commit = true, error = null)

            if (success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to save event"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun updateEvent(event: EnhancedCalendarEvent): Result<Unit> {
        return try {
            val startDate = NSDate.dateWithTimeIntervalSince1970(event.startDate.epochSeconds.toDouble())
            val endDate = NSDate.dateWithTimeIntervalSince1970(event.endDate.epochSeconds.toDouble())

            val predicate = NSPredicate.predicateWithFormat("title == %@", event.title as NSString)
            val events = store.eventsMatchingPredicate(predicate)

            val ekEvent = events?.firstOrNull() as? EKEvent
                ?: return Result.failure(CalendarEventNotFoundException(event.id))

            ekEvent.title = event.title
            ekEvent.startDate = startDate
            ekEvent.endDate = endDate
            ekEvent.location = event.location
            ekEvent.notes = event.description

            val success = store.saveEvent(ekEvent, span = EKSpan.EKSpanThisEvent, commit = true, error = null)

            if (success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to update event"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun deleteEvent(eventId: String): Result<Unit> {
        return try {
            val predicate = NSPredicate.predicateWithFormat("title == %@", eventId as NSString)
            val events = store.eventsMatchingPredicate(predicate)

            val ekEvent = events?.firstOrNull() as? EKEvent
                ?: return Result.failure(CalendarEventNotFoundException(eventId))

            val success = store.removeEvent(ekEvent, span = EKSpan.EKSpanThisEvent, commit = true, error = null)

            if (success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete event"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class CalendarEventNotFoundException(eventId: String) : Exception("Calendar event not found: $eventId")
