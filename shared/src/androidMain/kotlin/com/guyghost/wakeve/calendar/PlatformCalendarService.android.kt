package com.guyghost.wakeve.calendar

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.provider.CalendarContract.Attendees
import androidx.core.content.ContextCompat
import com.guyghost.wakeve.models.EnhancedCalendarEvent

actual class PlatformCalendarService(private val context: Context) {

    actual fun addEvent(event: EnhancedCalendarEvent): Result<Unit> = runCatching {
        // Vérifier les permissions
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.WRITE_CALENDAR) !=
                PackageManager.PERMISSION_GRANTED) {
            throw CalendarPermissionDeniedException()
        }

        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, getPrimaryCalendarId())
            put(CalendarContract.Events.TITLE, event.title)
            put(CalendarContract.Events.DESCRIPTION, event.description)
            put(CalendarContract.Events.EVENT_LOCATION, event.location)
            put(CalendarContract.Events.DTSTART, event.startDate.toEpochMilliseconds())
            put(CalendarContract.Events.DTEND, event.endDate.toEpochMilliseconds())
            put(CalendarContract.Events.EVENT_TIMEZONE, java.util.TimeZone.getDefault().id)
            put(CalendarContract.Events.HAS_ATTENDEE_DATA, if (event.attendees.isNotEmpty()) 1 else 0)
        }

        val uri = context.contentResolver.insert(
            CalendarContract.Events.CONTENT_URI,
            values
        ) ?: throw Exception("Failed to insert calendar event")

        val eventId = ContentUris.parseId(uri)

        // Ajouter les participants
        event.attendees.forEach { email ->
            val attendeeValues = ContentValues().apply {
                put(CalendarContract.Attendees.EVENT_ID, eventId)
                put(CalendarContract.Attendees.ATTENDEE_EMAIL, email)
                put(CalendarContract.Attendees.ATTENDEE_RELATIONSHIP, CalendarContract.Attendees.RELATIONSHIP_ATTENDEE)
                put(CalendarContract.Attendees.ATTENDEE_TYPE, CalendarContract.Attendees.TYPE_NONE)
            }
            context.contentResolver.insert(CalendarContract.Attendees.CONTENT_URI, attendeeValues)
        }

        Result.success(Unit)
    }

    actual fun updateEvent(event: EnhancedCalendarEvent): Result<Unit> = runCatching {
        val values = ContentValues().apply {
            put(CalendarContract.Events.TITLE, event.title)
            put(CalendarContract.Events.DESCRIPTION, event.description)
            put(CalendarContract.Events.EVENT_LOCATION, event.location)
            put(CalendarContract.Events.DTSTART, event.startDate.toEpochMilliseconds())
            put(CalendarContract.Events.DTEND, event.endDate.toEpochMilliseconds())
        }

        val selection = "${CalendarContract.Events.TITLE} = ? AND " +
                "${CalendarContract.Events.DTSTART} = ?"
        val selectionArgs = arrayOf(event.title, event.startDate.toEpochMilliseconds().toString())

        val rowsUpdated = context.contentResolver.update(
            CalendarContract.Events.CONTENT_URI,
            values,
            selection,
            selectionArgs
        )

        if (rowsUpdated == 0) {
            throw Exception("Event not found for update")
        }

        Result.success(Unit)
    }

    actual fun deleteEvent(eventId: String): Result<Unit> = runCatching {
        val uri = ContentUris.withAppendedId(
            CalendarContract.Events.CONTENT_URI,
            eventId.toLong()
        )
        val rowsDeleted = context.contentResolver.delete(uri, null, null)

        if (rowsDeleted == 0) {
            throw Exception("Event not found for deletion")
        }

        Result.success(Unit)
    }

    private fun getPrimaryCalendarId(): Long {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY
        )
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            "${CalendarContract.Calendars.VISIBLE} = 1",
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID))
            }
        }
        throw Exception("No calendar found")
    }
}

class CalendarPermissionDeniedException : Exception("Calendar permission denied")