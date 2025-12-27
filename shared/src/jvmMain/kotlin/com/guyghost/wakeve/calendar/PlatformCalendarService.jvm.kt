package com.guyghost.wakeve.calendar

import com.guyghost.wakeve.models.EnhancedCalendarEvent

class PlatformCalendarServiceImpl : PlatformCalendarService {
    override fun addEvent(event: EnhancedCalendarEvent): Result<Unit> {
        // JVM implementation - not supported
        return Result.failure(UnsupportedOperationException("Calendar integration not supported on JVM"))
    }

    override fun updateEvent(event: EnhancedCalendarEvent): Result<Unit> {
        // JVM implementation - not supported
        return Result.failure(UnsupportedOperationException("Calendar integration not supported on JVM"))
    }

    override fun deleteEvent(eventId: String): Result<Unit> {
        // JVM implementation - not supported
        return Result.failure(UnsupportedOperationException("Calendar integration not supported on JVM"))
    }
}