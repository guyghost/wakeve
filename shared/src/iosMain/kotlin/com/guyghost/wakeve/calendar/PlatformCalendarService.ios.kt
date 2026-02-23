package com.guyghost.wakeve.calendar

import com.guyghost.wakeve.models.EnhancedCalendarEvent
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.runBlocking
import platform.EventKit.EKAuthorizationStatus
import platform.EventKit.EKAuthorizationStatusAuthorized
import platform.EventKit.EKAuthorizationStatusDenied
import platform.EventKit.EKAuthorizationStatusNotDetermined
import platform.EventKit.EKAuthorizationStatusRestricted
import platform.EventKit.EKEntityType
import platform.EventKit.EKEvent
import platform.EventKit.EKEventStore
import platform.EventKit.EKSpan
import platform.Foundation.NSDate
import platform.Foundation.NSPredicate
import platform.Foundation.NSString
import platform.Foundation.dateWithTimeIntervalSince1970

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
class PlatformCalendarServiceImpl : PlatformCalendarService {
    private val store = EKEventStore()

    /**
     * Vérifie et demande l'autorisation d'accès au calendrier
     * Retourne true si l'accès est autorisé, false sinon
     */
    private fun ensureCalendarAuthorization(): Result<Unit> {
        val status = EKEventStore.authorizationStatusForEntityType(EKEntityType.EKEntityTypeEvent)

        return when (status) {
            EKAuthorizationStatusAuthorized -> Result.success(Unit)

            EKAuthorizationStatusNotDetermined -> {
                // Demander l'accès de manière synchrone via runBlocking
                val granted = runBlocking {
                    suspendCoroutine<Boolean> { continuation ->
                        store.requestAccessToEntityType(EKEntityType.EKEntityTypeEvent) { granted, _ ->
                            continuation.resume(granted)
                        }
                    }
                }
                if (granted) {
                    Result.success(Unit)
                } else {
                    Result.failure(CalendarPermissionDeniedException())
                }
            }

            EKAuthorizationStatusDenied ->
                Result.failure(CalendarPermissionDeniedException())

            EKAuthorizationStatusRestricted ->
                Result.failure(CalendarPermissionRestrictedException())

            else -> Result.failure(CalendarPermissionDeniedException())
        }
    }

    override fun addEvent(event: EnhancedCalendarEvent): Result<Unit> {
        // Vérifier l'autorisation avant toute opération
        ensureCalendarAuthorization().onFailure { return Result.failure(it) }

        return try {
            val startDate = NSDate.dateWithTimeIntervalSince1970(event.startDate.epochSeconds.toDouble())
            val endDate = NSDate.dateWithTimeIntervalSince1970(event.endDate.epochSeconds.toDouble())

            val ekEvent = EKEvent.eventWithEventStore(store)
            ekEvent.title = event.title
            ekEvent.startDate = startDate
            ekEvent.endDate = endDate
            ekEvent.location = event.location

            // EKEvent.attendees est en lecture seule, on ajoute les participants dans les notes
            val attendeesSection = if (event.attendees.isNotEmpty()) {
                "\n\n--- Participants ---\n" + event.attendees.joinToString("\n") { "- $it" }
            } else ""
            ekEvent.notes = (event.description ?: "") + attendeesSection

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
        // Vérifier l'autorisation avant toute opération
        ensureCalendarAuthorization().onFailure { return Result.failure(it) }

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

            // EKEvent.attendees est en lecture seule, on ajoute les participants dans les notes
            val attendeesSection = if (event.attendees.isNotEmpty()) {
                "\n\n--- Participants ---\n" + event.attendees.joinToString("\n") { "- $it" }
            } else ""
            ekEvent.notes = (event.description ?: "") + attendeesSection

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
        // Vérifier l'autorisation avant toute opération
        ensureCalendarAuthorization().onFailure { return Result.failure(it) }

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
class CalendarPermissionDeniedException : Exception("L'accès au calendrier a été refusé. Veuillez autoriser l'accès dans les Réglages.")
class CalendarPermissionRestrictedException : Exception("L'accès au calendrier est restreint sur cet appareil.")
