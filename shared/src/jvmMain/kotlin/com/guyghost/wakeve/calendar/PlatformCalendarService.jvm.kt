package com.guyghost.wakeve.calendar

import com.guyghost.wakeve.models.EnhancedCalendarEvent

actual class PlatformCalendarService : PlatformCalendarService {
    actual override fun addEvent(event: EnhancedCalendarEvent): Result<Unit> {
        // JVM implementation - not supported
        return Result.failure(UnsupportedOperationException("Calendar integration not supported on JVM"))
    }

    actual override fun updateEvent(event: EnhancedCalendarEvent): Result<Unit> {
        // JVM implementation - not supported
        return Result.failure(UnsupportedOperationException("Calendar integration not supported on JVM"))
    }

    actual override fun deleteEvent(eventId: String): Result<Unit> {
        // JVM implementation - not supported
        return Result.failure(UnsupportedOperationException("Calendar integration not supported on JVM"))
    }
}