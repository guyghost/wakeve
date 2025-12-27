package com.guyghost.wakeve.meeting

import com.guyghost.wakeve.database.WakevDb
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.MeetingPlatform
import com.guyghost.wakeve.models.ParticipantStatus
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Service principal pour gérer les réunions virtuelles
 */
class MeetingService(
    private val database: WakevDb,
    private val meetingRepository: MeetingRepository,
    private val meetingPlatformProvider: MeetingPlatformProvider
) {

    private val eventQueries = database.eventQueries
    private val participantQueries = database.participantQueries

    /**
     * Crée une nouvelle réunion pour un événement
     */
    suspend fun createMeeting(
        eventId: String,
        organizerId: String,
        title: String,
        description: String? = null,
        startTime: Instant,
        duration: Duration = 1.hours,
        platform: MeetingPlatform
    ): Result<Meeting> {
        return try {
            // Vérifier que l'événement est confirmé ou en organisation
            val event = eventQueries.selectById(eventId).executeAsOne()
                ?: return Result.failure(MeetingException.EventNotFound(eventId))

            val eventStatus = EventStatus.valueOf(event.status)
            if (eventStatus != EventStatus.CONFIRMED && eventStatus != EventStatus.ORGANIZING) {
                return Result.failure(MeetingException.InvalidEventStatus(eventStatus))
            }

            // Obtenir les participants validés
            val invitedParticipants = participantQueries
                .selectByEventId(eventId)
                .executeAsList()
                .filter { it.hasValidatedDate == 1L }
                .map { it.userId }

            // Générer les détails de réunion via le provider
            val meetingLink = meetingPlatformProvider.generateMeetingLink(
                platform = platform,
                title = title,
                description = description,
                startTime = startTime,
                duration = duration
            )

            // Créer l'objet Meeting
            val meeting = Meeting(
                id = generateMeetingId(),
                eventId = eventId,
                organizerId = organizerId,
                title = title,
                description = description,
                startTime = startTime,
                duration = duration,
                platform = platform,
                meetingLink = meetingLink,
                hostMeetingId = meetingPlatformProvider.getHostMeetingId(meetingLink),
                password = generateMeetingPassword(),
                invitedParticipants = invitedParticipants,
                status = MeetingStatus.SCHEDULED,
                createdAt = Clock.System.now().toString()
            )

            // Sauvegarder en base
            meetingRepository.createMeeting(meeting)

            Result.success(meeting)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Annule une réunion
     */
    suspend fun cancelMeeting(meetingId: String, organizerId: String): Result<Unit> {
        return try {
            val meeting = meetingRepository.getMeetingById(meetingId)
                ?: return Result.failure(MeetingException.MeetingNotFound(meetingId))

            if (meeting.organizerId != organizerId) {
                return Result.failure(MeetingException.UnauthorizedAccess())
            }

            // Annuler sur la plateforme
            meetingPlatformProvider.cancelMeeting(meeting.platform, meeting.hostMeetingId)

            // Mettre à jour le statut
            meetingRepository.updateMeetingStatus(meetingId, MeetingStatus.CANCELLED)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Génère un mot de passe pour la réunion (8 caractères)
     */
    fun generateMeetingPassword(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..8)
            .map { chars.random() }
            .joinToString("")
    }

    /**
     * Génère un ID unique pour la réunion
     */
    private fun generateMeetingId(): String {
        return "meeting_${Clock.System.now().toEpochMilliseconds()}_${Random.nextInt(1000, 9999)}"
    }

    /**
     * Obtient une réunion par ID
     */
    suspend fun getMeeting(meetingId: String): Meeting? {
        return meetingRepository.getMeetingById(meetingId)
    }

    /**
     * Liste les réunions pour un événement
     */
    suspend fun getMeetingsForEvent(eventId: String): List<Meeting> {
        return meetingRepository.getMeetingsByEventId(eventId)
    }
}