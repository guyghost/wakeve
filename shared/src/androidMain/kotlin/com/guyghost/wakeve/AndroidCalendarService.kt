package com.guyghost.wakeve

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.CalendarContract
import com.guyghost.wakeve.models.*
import java.util.*

class AndroidCalendarService(private val context: Context) : CalendarService {

    override suspend fun addEventToCalendar(event: CalendarEvent): Result<String> {
        return try {
            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, parseIsoToMillis(event.startTime))
                put(CalendarContract.Events.DTEND, parseIsoToMillis(event.endTime))
                put(CalendarContract.Events.TITLE, event.title)
                put(CalendarContract.Events.DESCRIPTION, event.description)
                put(CalendarContract.Events.EVENT_LOCATION, event.location)
                put(CalendarContract.Events.CALENDAR_ID, getPrimaryCalendarId())
                put(CalendarContract.Events.EVENT_TIMEZONE, event.timezone)
            }

            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            val eventId = uri?.lastPathSegment ?: throw Exception("Failed to insert event")
            Result.success(eventId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun generateICSInvite(event: CalendarEvent): CalendarInvite {
        // Use the common DefaultCalendarService for ICS generation
        val defaultService = DefaultCalendarService()
        return defaultService.generateICSInvite(event)
    }

    override suspend fun updateCalendarEvent(calendarEventId: String, event: CalendarEvent): Result<Unit> {
        return try {
            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, parseIsoToMillis(event.startTime))
                put(CalendarContract.Events.DTEND, parseIsoToMillis(event.endTime))
                put(CalendarContract.Events.TITLE, event.title)
                put(CalendarContract.Events.DESCRIPTION, event.description)
                put(CalendarContract.Events.EVENT_LOCATION, event.location)
            }

            val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, calendarEventId.toLong())
            context.contentResolver.update(uri, values, null, null)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeCalendarEvent(calendarEventId: String): Result<Unit> {
        return try {
            val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, calendarEventId.toLong())
            context.contentResolver.delete(uri, null, null)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getPrimaryCalendarId(): Long {
        // Get the first available calendar
        val projection = arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.IS_PRIMARY)
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            "${CalendarContract.Calendars.VISIBLE} = 1",
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getLong(0)
            }
        }
        throw Exception("No calendar found")
    }

    private fun parseIsoToMillis(iso: String): Long {
        // Simple parsing - in real app, use proper date parsing
        val date = iso.substring(0, 10)
        val time = iso.substring(11, 19)
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.parse("$date $time")?.time ?: 0L
    }
}