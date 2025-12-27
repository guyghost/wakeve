package com.guyghost.wakeve.meeting

import com.guyghost.wakeve.models.EventStatus

/**
 * Exception levée lorsqu'un événement n'est pas trouvé
 */
class EventNotFoundException(eventId: String) : Exception("Event not found: $eventId")

/**
 * Exception levée lorsqu'une réunion n'est pas trouvée
 */
class MeetingNotFoundException(meetingId: String) : Exception("Meeting not found: $meetingId")

/**
 * Exception levée lorsqu'un événement a un statut invalide pour l'opération
 */
class InvalidEventStatusException(status: EventStatus) : Exception("Invalid event status: $status")