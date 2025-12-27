# Calendar Management Specification

## Version
**Version**: 1.0.0
**Status**: ✅ Implémenté
**Date de création**: 26 décembre 2025

## Overview

Le système de calendrier de Wakeve permet de générer des invitations ICS et de s'intégrer avec les calendriers natifs Android (CalendarContract) et iOS (EventKit).

## Domain Model

### Core Concepts

- **ICS Document**: Format standard iCalendar (RFC 5545) pour les invitations
- **Native Integration**: Ajout d'événements aux calendriers natifs
- **Timezone Handling**: Gestion automatique des fuseaux horaires
- **Multi-participant Support**: Invitations pour tous les participants

### CalendarEvent

```kotlin
@Serializable
data class CalendarEvent(
    val id: String,                    // Unique ID (event_participantId)
    val title: String,
    val description: String?,
    val location: String?,
    val startDate: Instant,             // Début de l'événement
    val endDate: Instant,               // Fin de l'événement
    val attendees: List<String>,           // Liste des participantIds
    val organizer: String
)
```

### ICS Document

```kotlin
@Serializable
data class ICSDocument(
    val content: String,                // Contenu ICS complet
    val filename: String               // Nom de fichier pour téléchargement
)
)
```

### MeetingReminder

```kotlin
enum class MeetingReminderTiming {
    ONE_DAY_BEFORE,
    ONE_HOUR_BEFORE,
    FIFTEEN_MINUTES_BEFORE
}
```

### Platform Calendar Service

```kotlin
expect class PlatformCalendarService {
    fun addEvent(event: CalendarEvent): Result<Unit>
    fun updateEvent(event: CalendarEvent): Result<Unit>
    fun deleteEvent(eventId: String): Result<Unit>
}
```

### CalendarService

```kotlin
class CalendarService(
    private val database: WakevDb,
    private val platformCalendarService: PlatformCalendarService
) {
    suspend fun generateICSInvitation(
        eventId: String,
        invites: List<String>
    ): ICSDocument {
        val event = database.eventQueries.selectById(eventId).executeAsOne()
        
        return buildICS(event, invites)
    }
    
    suspend fun addToNativeCalendar(
        eventId: String,
        participantId: String
    ): Result<Unit> {
        val event = database.eventQueries.selectById(eventId).executeAsOne()
        
        val calendarEvent = buildCalendarEvent(event, participantId)
        return platformCalendarService.addEvent(calendarEvent)
    }
    
    suspend fun updateNativeCalendarEvent(
        eventId: String,
        participantId: String
    ): Result<Unit> {
        val event = database.eventQueries.selectById(eventId).executeAsOne()
        
        val calendarEvent = buildCalendarEvent(event, participantId)
        return platformCalendarService.updateEvent(calendarEvent)
    }
    
    suspend fun removeFromNativeCalendar(
        eventId: String,
        participantId: String
    ): Result<Unit> {
        platformCalendarService.deleteEvent("${eventId}_${participantId}")
    }
    
    suspend fun sendMeetingReminders(
        eventId: String,
        reminderTiming: MeetingReminderTiming
    ): Result<Unit> {
        val event = database.eventQueries.selectById(eventId).executeAsOne() ?: 
            return Result.failure("Event not found")
        
        val reminders = createReminderEvents(event, reminderTiming)
        
        // TODO: Intégrer avec NotificationService
        // notificationService.sendReminder(reminder)
        
        reminders.forEach { reminder ->
            // notificationService.sendNotification(...)
        }
        
        return Result.success(Unit)
    }
}
```

## Calendar Integration

### Android (CalendarContract)

**Permissions Required**:
```xml
<uses-permission android:name="android.permission.WRITE_CALENDAR" />
```

**Key Classes**:

```kotlin
// androidMain
actual class PlatformCalendarService(
    private val context: Context
) : PlatformCalendarService {
    
    private val calendar: Calendar by lazy { Calendar.getInstance() }
    
    actual override fun addEvent(event: CalendarEvent): Result<Unit> = runCatching {
        // Check permissions
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.WRITE_CALENDAR) != 
                PackageManager.PERMISSION_GRANTED) {
            throw CalendarPermissionDeniedException()
        }
        
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, 1)
            put(CalendarContract.Events.TITLE, event.title)
            put(CalendarContract.Events.DESCRIPTION, event.description ?: "")
            put(CalendarContract.Events.EVENT_LOCATION, event.location ?: "")
            put(CalendarContract.Events.DTSTART, event.startDate.toEpochMilliseconds())
            put(CalendarContract.Events.DTEND, event.endDate.toEpochMilliseconds())
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            
            // Participants
            if (event.attendees.isNotEmpty()) {
                put(CalendarContract.Events.HAS_ATTENDEE_DATA, 1)
                val attendeeEmails = event.attendees.joinToString(",")
                put(CalendarContract.Events.ATTENDEES, attendeeEmails)
            }
        }
        
        val uri = context.contentResolver.insert(
            CalendarContract.Events.CONTENT_URI,
            values
        )
        
        Result.success(Unit)
    }
    
    actual override fun updateEvent(event: CalendarEvent): Result<Unit> = runCatching {
        val values = ContentValues().apply {
            put(CalendarContract.Events.TITLE, event.title)
            put(CalendarContract.Events.DESCRIPTION, event.description ?: "")
            put(CalendarContract.Events.EVENT_LOCATION, event.location ?: "")
            put(CalendarContract.Events.DTSTART, event.startDate.toEpochMilliseconds())
            put(CalendarContract.Events.DTEND, event.endDate.toEpochMilliseconds())
        put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
        }
        
        val selection = "${CalendarContract.Events.TITLE} = ?"
        val selectionArgs = arrayOf(event.title, 
            event.startDate.toEpochMilliseconds().toString())
        
        context.contentResolver.update(
            CalendarContract.Events.CONTENT_URI,
            selection,
            selectionArgs,
            values
        )
        
        Result.success(Unit)
    }
    
    actual override fun deleteEvent(eventId: String): Result<Unit> = runCatching {
        val uri = ContentUris.withAppendedId(
            CalendarContract.Events.CONTENT_URI,
            eventId.toLong()
        )
        context.contentResolver.delete(uri, null, null)
        Result.success(Unit)
    }
}

class CalendarPermissionDeniedException : 
    Exception("Calendar permission denied")
```

### iOS (EventKit)

**Framework**: EventKit (iOS 16+)

```swift
import EventKit

actual class PlatformCalendarService : PlatformCalendarService {
    private let store = EKEventStore()
    
    func addEvent(event: CalendarEvent) -> Result<Unit> {
        let startDate = Date(timeIntervalSince1970: event.startDate)
        let endDate = Date(timeIntervalSince1970: event.endDate)
        
        let ekEvent = EKEvent(
            eventIdentifier: event.id,
            title: event.title,
            startDate: startDate,
            endDate: endDate,
            location: event.location,
            notes: event.description
        )
        
        do {
            try store.save(ekEvent, span: nil)
            return .success(())
        } catch {
            return .failure(error)
        }
    }
    
    func updateEvent(event: CalendarEvent) -> Result<Unit> {
        let startDate = Date(timeIntervalSince1970: event.startDate)
        let endDate = Date(timeIntervalSince1970: event.endDate)
        
        let predicate: NSPredicate = NSPredicate(
            format: "eventIdentifier == %@",
            argumentArray: [event.id]
        )
        
        do {
            let events = try? store.events(matching: predicate)
            guard let ekEvent = events?.first else {
                return .failure(CalendarEventNotFound(event.id))
            }
            
            ekEvent.title = event.title
            ekEvent.startDate = Date(timeIntervalSince1970: event.startDate)
            ekEvent.endDate = Date(timeIntervalSince1970: event.endDate)
            ekEvent.location = event.location
            ekEvent.notes = event.description
            
            try store.save(ekEvent, span: nil)
            return .success(())
        } catch {
            return .failure(error)
        }
    }
    
    func deleteEvent(eventId: String) -> Result<Unit> {
        let predicate: NSPredicate = NSPredicate(
            format: "eventIdentifier == %@",
            argumentArray: [eventId]
        )
        
        do {
            let events = try? store.events(matching: predicate)
            guard let ekEvent = events?.first else {
                return .failure(CalendarEventNotFound(eventId))
            }
            
            try store.remove(ekEvent, span: nil)
            return .success(())
        } catch {
            return .failure(error)
        }
    }
}

class CalendarEventNotFound(eventId: String) : Error {
    let eventId: String
    
    localizedDescription: String {
        return "Calendar event not found: \(eventId)"
    }
}
```

## API Endpoints

```
POST   /api/events/{id}/calendar/ics
GET    /api/events/{id}/calendar/ics
POST   /api/events/{id}/calendar/native
PUT    /api/events/{id}/calendar/native/{participantId}
DELETE /api/events/{id}/calendar/native/{participantId}
POST   /api/events/{id}/calendar/reminders/{timing}
```

## Scenarios

### SCENARIO 1: Generate ICS Invitation

**GIVEN**: Organisateur crée un événement avec 3 participants
**WHEN**: Organisateur génère l'ICS invitation
**THEN**: Système génère fichier ICS avec tous les détails
**AND**: Participants peuvent télécharger l'ICS

```kotlin
val ics = calendarService.generateICSInvitation(
    eventId = "event-1",
    invites = listOf("user-1", "user-2", "user-3")
)
// ICS content:
// BEGIN:VCALENDAR
// VERSION:2.0
// PRODID:-//Wakeve//Event//FR
// METHOD:REQUEST
// ...
```

### SCENARIO 2: Add to Native Calendar

**GIVEN**: Événement confirmé avec date fixée
**WHEN**: Participant clique sur "Ajouter au calendrier"
**THEN**: Événement ajouté au calendrier natif
**AND**: Rappels automatiques créés

### SCENARIO 3: Update Event

**GIVEN**: Date de l'événement modifiée
**WHEN**: Participant met à jour dans l'appli
**THEN**: Événement mis à jour dans le calendrier

### SCENARIO 4: Delete Event

**GIVEN**: Événement annulé
**WHEN**: Organisateur supprime l'événement
**THEN**: Événement supprimé du calendrier

### SCENARIO 5: Meeting Reminders

**GIVEN**: Événement avec réunion planifiée
**WHEN**: Date de la réunion approche
**THEN**: Notification envoyée à tous les participants

```kotlin
calendarService.sendMeetingReminders(
    eventId = "event-1",
    reminderTiming = MeetingReminderTiming.ONE_DAY_BEFORE
)
// Envoie notification à tous les participants
```

## Implementation Notes

### Database Integration

```sql
-- Event.sq (add if not exists)
-- No DB changes needed for Phase 1
-- Calendar integration is stateless - uses platform APIs directly
```

### Permissions

**Android**:
- `WRITE_CALENDAR` permission in AndroidManifest.xml
- Runtime check with ContextCompat.checkSelfPermission()
- Graceful degradation if not granted

**iOS**:
- EventKit doesn't require permissions for user calendars
- Privacy prompt for other calendars

### Error Handling

```kotlin
class CalendarServiceException(message: String) : Exception(message)

sealed class AddEventResult {
    data class Success(val calendarEventId: String?) : AddEventResult()
    data class Failure(val error: CalendarServiceException) : AddEventResult()
}
```

## Testing

```kotlin
// CalendarServiceTest.kt
class CalendarServiceTest {
    @Test
    fun `generate ICS document with all event details`() {
        val ics = calendarService.generateICSInvitation(eventId, emptyList())
        
        assertTrue(ics.content.contains("BEGIN:VCALENDAR"))
        assertTrue(ics.content.contains("SUMMARY:$eventTitle"))
        assertTrue(ics.content.contains("DTSTART:"))
        assertTrue(ics.content.contains("DTEND:"))
    }
    
    @Test
    fun `ICS document includes correct timezone`() {
        val ics = calendarService.generateICSInvitation(eventId, emptyList())
        
        assertTrue(ics.content.contains("TZID:"))
        assertTrue(ics.content.contains("TZID:"))
        assertTrue(ics.content.contains("TZID:Europe/Paris"))
    }
    
    @Test
    fun `Add to native calendar requires permission on Android`() {
        // Test permission handling
    }
    
    @Test
    fun `Update event updates existing calendar entry`() {
        // Test update logic
    }
    
    @Test
    fun `Delete event removes from native calendar`() {
        // Test delete logic
    }
    
    @Test
    fun `Meeting reminders are scheduled correctly`() {
        // Test reminder timing logic
    }
}
```

## Platform-Specific Considerations

### Android
- Uses CalendarContract for structured events
- Support for recurring events (future enhancement)
- Handles multiple calendar apps (Google, Samsung, etc.)

### iOS
- Uses EventKit framework
- Automatic sync with iCloud
- Privacy-preserving (local calendars only)

### Future Enhancements

- Recurring events (daily, weekly, monthly)
- Multiple calendar provider selection
- Calendar conflict resolution
- Offline ICS generation
- Calendar sync conflicts handling
- Rich event descriptions with formatting

## Limitations

**Phase 1** (Current):
- Single event operations only
- No recurring support
- No calendar conflict resolution
- Native calendars only (no 3rd party providers)

**Phase 2** (Future):
- Recurring events
- Calendar conflict detection
- Multiple calendar providers
- Batch operations
- Enhanced ICS generation

## Related Specs

- `event-organization/spec.md` - Main event management
- `suggestion-management/spec.md` - Recommendations based on preferences
- `transport-optimization/spec.md` - Departure times
- `destination-planning/spec.md` - Destination selection
- `meeting-service/spec.md` - Meeting links generation