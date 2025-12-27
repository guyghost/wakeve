package com.guyghost.wakeve.calendar

import com.guyghost.wakeve.database.WakevDb
import com.guyghost.wakeve.Event
import com.guyghost.wakeve.User
import com.guyghost.wakeve.Participant
import com.guyghost.wakeve.models.EnhancedCalendarEvent
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.ICSDocument
import kotlinx.datetime.*

/**
 * Service de calendrier pour Wakeve
 *
 * Génère des invitations ICS avec détails complets d'événements
 * et s'intègre aux calendriers natifs Android (CalendarContract) et iOS (EventKit).
 */
class CalendarService(
    private val database: WakevDb,
    private val platformCalendarService: PlatformCalendarService
) {

    /**
     * Génère une invitation ICS pour un événement
     */
    suspend fun generateICSInvitation(
        eventId: String,
        invitees: List<String>
    ): ICSDocument {
        val event = database.eventQueries.selectById(eventId).executeAsOneOrNull()
            ?: throw EventNotFoundException(eventId)

        val organizer = database.userQueries
            .selectUserById(event.organizerId).executeAsOneOrNull()

        val participantsWithUsers = database.participantQueries
            .selectByEventId(eventId)
            .executeAsList()
            .mapNotNull { participant ->
                val user = database.userQueries.selectUserById(participant.userId).executeAsOneOrNull()
                if (user != null) {
                    ParticipantWithUser(participant, user)
                } else null
            }

        val icsContent = buildString {
            appendLine("BEGIN:VCALENDAR")
            appendLine("VERSION:2.0")
            appendLine("PRODID:-//Wakeve//Wakeve Event//FR")
            appendLine("CALSCALE:GREGORIAN")
            appendLine("METHOD:REQUEST")

            // Événement principal
            val startDate = extractStartDate(event)
            val endDate = extractEndDate(event, startDate)

            appendLine("BEGIN:VEVENT")
            appendLine("UID:${event.id}@wakeve.app")
            appendLine("DTSTAMP:${formatDateTime(Clock.System.now())}")
            appendLine("DTSTART:${formatDateTime(startDate)}")
            appendLine("DTEND:${formatDateTime(endDate)}")
            appendLine("SUMMARY:${event.title}")
            appendLine("DESCRIPTION:Vous êtes invité à participer à ${event.title}\\n\\n${event.description ?: ""}")

            val location = extractLocation(event)
            if (location.isNotEmpty()) {
                appendLine("LOCATION:$location")
            }

            appendLine("ORGANIZER;CN=${organizer?.name ?: "Wakeve"}:mailto:${organizer?.email ?: event.organizerId}")

            // Participants comme attendees
            participantsWithUsers.forEach { participantWithUser ->
                appendLine("ATTENDEE;ROLE=REQ-PARTICIPANT;PARTSTAT=NEEDS-ACTION;CN=${participantWithUser.user.name}:mailto:${participantWithUser.user.email}")
            }

            appendLine("END:VEVENT")

            // Rappels
            appendLine("BEGIN:VALARM")
            appendLine("TRIGGER:-P1DT090000") // 1 jour avant à 9h
            appendLine("DESCRIPTION:Rappel: Événement dans 1 jour")
            appendLine("ACTION:DISPLAY")
            appendLine("END:VALARM")

            appendLine("BEGIN:VALARM")
            appendLine("TRIGGER:-P1W") // 1 semaine avant
            appendLine("DESCRIPTION:Rappel: Événement dans 1 semaine")
            appendLine("ACTION:DISPLAY")
            appendLine("END:VALARM")

            appendLine("END:VCALENDAR")
        }

        return ICSDocument(
            content = icsContent,
            filename = "${event.title.replace(Regex("[^a-zA-Z0-9\\s]"), "").replace("\\s+".toRegex(), "_")}_invitation.ics"
        )
    }

    /**
     * Met à jour un événement dans le calendrier natif
     */
    suspend fun updateNativeCalendarEvent(
        eventId: String,
        participantId: String
    ): Result<Unit> {
        val event = database.eventQueries.selectById(eventId).executeAsOneOrNull()
            ?: return Result.failure(EventNotFoundException(eventId))

        val startDate = extractStartDate(event)
        val endDate = extractEndDate(event, startDate)

        val updatedEvent = EnhancedCalendarEvent(
            id = "${event.id}_${participantId}",
            title = event.title,
            description = event.description,
            location = extractLocation(event),
            startDate = startDate,
            endDate = endDate,
            attendees = emptyList(),
            organizer = database.userQueries.selectUserById(event.organizerId).executeAsOneOrNull()?.email ?: event.organizerId
        )

        return platformCalendarService.updateEvent(updatedEvent)
    }

    /**
     * Ajoute un événement au calendrier natif
     */
    suspend fun addToNativeCalendar(
        eventId: String,
        participantId: String
    ): Result<Unit> {
        val event = database.eventQueries.selectById(eventId).executeAsOneOrNull()
            ?: return Result.failure(EventNotFoundException(eventId))

        val startDate = extractStartDate(event)
        val endDate = extractEndDate(event, startDate)

        val participantsWithUsers = database.participantQueries
            .selectByEventId(eventId)
            .executeAsList()
            .mapNotNull { participant ->
                val user = database.userQueries.selectUserById(participant.userId).executeAsOneOrNull()
                if (user != null) {
                    ParticipantWithUser(participant, user)
                } else null
            }

        val calendarEvent = EnhancedCalendarEvent(
            id = "${event.id}_${participantId}",
            title = event.title,
            description = event.description,
            location = extractLocation(event),
            startDate = startDate,
            endDate = endDate,
            attendees = participantsWithUsers.map { it.user.email },
            organizer = database.userQueries.selectUserById(event.organizerId).executeAsOneOrNull()?.email ?: event.organizerId
        )

        return platformCalendarService.addEvent(calendarEvent)
    }

    /**
     * Extrait la date de début depuis l'événement
     */
    private fun extractStartDate(event: Event): Instant {
        return when (event.status) {
            "CONFIRMED" -> {
                // Utiliser confirmedDate si disponible
                val confirmedDate = database.confirmedDateQueries
                    .selectWithTimeslotDetails(event.id).executeAsOneOrNull()
                if (confirmedDate != null) {
                    parseISO8601(confirmedDate.startTime)
                } else {
                    // Fallback aux slots proposés
                    val firstSlot = database.timeSlotQueries
                        .selectByEventId(event.id)
                        .executeAsList()
                        .firstOrNull()
                    firstSlot?.let { parseISO8601(it.startTime) } ?: Clock.System.now()
                }
            }
            else -> {
                // Pour les autres statuts, utiliser le premier slot proposé
                val firstSlot = database.timeSlotQueries
                    .selectByEventId(event.id)
                    .executeAsList()
                    .firstOrNull()
                firstSlot?.let { parseISO8601(it.startTime) } ?: Clock.System.now()
            }
        }
    }

    /**
     * Extrait la date de fin depuis l'événement
     */
    private fun extractEndDate(event: Event, startDate: Instant): Instant {
        return when (event.status) {
            "CONFIRMED" -> {
                // Utiliser confirmedDate si disponible
                val confirmedDate = database.confirmedDateQueries
                    .selectWithTimeslotDetails(event.id).executeAsOneOrNull()
                if (confirmedDate != null) {
                    parseISO8601(confirmedDate.endTime)
                } else {
                    // Durée par défaut de 2 heures
                    startDate.plus(2, DateTimeUnit.HOUR)
                }
            }
            else -> {
                // Calculer depuis les slots proposés
                val firstSlot = database.timeSlotQueries
                    .selectByEventId(event.id)
                    .executeAsList()
                    .firstOrNull()
                firstSlot?.let { parseISO8601(it.endTime) } ?: startDate.plus(2, DateTimeUnit.HOUR)
            }
        }
    }

    /**
     * Extrait le lieu de l'événement
     */
    private fun extractLocation(event: Event): String {
        // Pour l'instant, pas de lieu stocké dans Event
        // À étendre quand les scénarios auront des lieux
        return ""
    }

    /**
     * Parse une date ISO 8601 en Instant
     */
    private fun parseISO8601(dateString: String): Instant {
        return try {
            Instant.parse(dateString)
        } catch (e: Exception) {
            // Fallback en cas d'erreur de parsing
            Clock.System.now()
        }
    }

    /**
     * Formate un Instant en format ICS (YYYYMMDDTHHMMSSZ)
     */
    private fun formatDateTime(instant: Instant): String {
        val local = instant.toLocalDateTime(TimeZone.UTC)
        val year = local.year.toString()
        val month = local.monthNumber.toString().padStart(2, '0')
        val day = local.dayOfMonth.toString().padStart(2, '0')
        val hour = local.hour.toString().padStart(2, '0')
        val minute = local.minute.toString().padStart(2, '0')
        val second = local.second.toString().padStart(2, '0')
        return "${year}${month}${day}T${hour}${minute}${second}Z"
    }
}

/**
 * Exception pour événement non trouvé
 */
class EventNotFoundException(eventId: String) :
    Exception("Event not found: $eventId")

/**
 * Participant avec informations utilisateur
 */
private data class ParticipantWithUser(
    val participant: Participant,
    val user: User
)

/**
 * Interface pour le service calendrier plateforme-spécifique
 */
interface PlatformCalendarService {
    fun addEvent(event: EnhancedCalendarEvent): Result<Unit>
    fun updateEvent(event: EnhancedCalendarEvent): Result<Unit>
    fun deleteEvent(eventId: String): Result<Unit>
}