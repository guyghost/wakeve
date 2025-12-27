package com.guyghost.wakeve.calendar

import com.guyghost.wakeve.models.EnhancedCalendarEvent

class PlatformCalendarServiceImpl : PlatformCalendarService {
    override fun addEvent(event: EnhancedCalendarEvent): Result<Unit> {
        // JS implementation - not supported
        return Result.failure(UnsupportedOperationException("Calendar integration not supported on JavaScript"))
    }

    override fun updateEvent(event: EnhancedCalendarEvent): Result<Unit> {
        // JS implementation - not supported
        return Result.failure(UnsupportedOperationException("Calendar integration not supported on JavaScript"))
    }

    override fun deleteEvent(eventId: String): Result<Unit> {
        // JS implementation - not supported
        return Result.failure(UnsupportedOperationException("Calendar integration not supported on JavaScript"))
    }
}