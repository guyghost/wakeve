package com.guyghost.wakeve.meeting

import com.guyghost.wakeve.calendar.CalendarService
import com.guyghost.wakeve.database.WakevDb
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.InvitationStatus
import com.guyghost.wakeve.models.MeetingLinkResponse
import com.guyghost.wakeve.models.MeetingPlatform
import com.guyghost.wakeve.models.MeetingReminderTiming
import com.guyghost.wakeve.models.MeetingStatus
import com.guyghost.wakeve.models.MeetingVotingResult
import com.guyghost.wakeve.models.MeetingVotingResultResponse
import com.guyghost.wakeve.models.MeetingWithVotes
import com.guyghost.wakeve.models.NotificationMessage
import com.guyghost.wakeve.models.NotificationType
import com.guyghost.wakeve.models.ReminderStatus
import com.guyghost.wakeve.notification.NotificationService
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.toInstant
import kotlin.random.Random
import kotlin.time.Duration

/**
 * Service principal pour gérer les réunions virtuelles
 *
 * Responsabilités:
 * - Génération de liens de réunion (Zoom, Google Meet, FaceTime)
 * - Gestion des invitations aux participants
 * - Planification des rappels automatiques
 * - Intégration avec le calendrier natif
 */
class MeetingService(
    private val database: WakevDb,
    private val calendarService: CalendarService,
    private val notificationService: NotificationService
) {

    private val eventQueries = database.eventQueries
    private val participantQueries = database.participantQueries
    private val meetingRepository: MeetingRepository

    // Platform providers
    private val zoomProvider = ZoomMeetingPlatformProvider()
    private val googleMeetProvider = GoogleMeetPlatformProvider()
    private val facetimeProvider = FaceTimePlatformProvider()

    /**
     * Crée une nouvelle réunion virtuelle
     */
    suspend fun createMeeting(
        eventId: String,
        organizerId: String,
        platform: MeetingPlatform,
        title: String,
        description: String?,
        scheduledFor: Instant,
        duration: Duration,
        timezone: String,
        participantLimit: Int? = null,
        requirePassword: Boolean = true,
        waitingRoom: Boolean = true
    ): Result<com.guyghost.wakeve.models.VirtualMeeting> {
        return try {
            val event = eventQueries.selectById(eventId).executeAsOne()
                ?: return Result.failure(MeetingException.EventNotFound(eventId))

            val eventStatus = EventStatus.valueOf(event.status)
            if (eventStatus != EventStatus.CONFIRMED && eventStatus != EventStatus.ORGANIZING) {
                return Result.failure(MeetingException.InvalidEventStatus(eventStatus))
            }

            val invitedParticipants = participantQueries
                .selectByEventId(eventId)
                .executeAsList()
                .filter { it.hasValidatedDate == 1L }
                .map { it.userId }

            val meetingDetails = when (platform) {
                MeetingPlatform.ZOOM -> createZoomMeetingDetails(
                    title = title,
                    description = description,
                    scheduledFor = scheduledFor,
                    duration = duration,
                    timezone = timezone,
                    participantLimit = participantLimit,
                    requirePassword = requirePassword,
                    waitingRoom = waitingRoom
                )
                MeetingPlatform.GOOGLE_MEET -> createGoogleMeetMeetingDetails(
                    title = title,
                    description = description,
                    scheduledFor = scheduledFor,
                    duration = duration,
                    timezone = timezone
                )
                MeetingPlatform.FACETIME -> createFaceTimeMeetingDetails(
                    title = title,
                    description = description,
                    scheduledFor = scheduledFor,
                    duration = duration,
                    timezone = timezone,
                    organizerId = organizerId
                )
                MeetingPlatform.TEAMS, MeetingPlatform.WEBEX ->
                    return Result.failure(MeetingException.UnsupportedPlatform(platform))
            }

            val meeting = com.guyghost.wakeve.meeting.Meeting(
                id = generateMeetingId(),
                eventId = eventId,
                organizerId = organizerId,
                title = title,
                description = description,
                startTime = scheduledFor,
                duration = duration,
                platform = platform,
                meetingLink = meetingDetails.url,
                hostMeetingId = meetingDetails.hostMeetingId,
                password = meetingDetails.password ?: "",
                invitedParticipants = invitedParticipants,
                status = MeetingStatus.SCHEDULED,
                createdAt = Clock.System.now().toString()
            )

            meetingRepository.createMeeting(meeting)

            scheduleMeetingReminders(
                meetingId = meeting.id,
                eventId = eventId,
                scheduledFor = scheduledFor,
                timezone = timezone
            )

            val virtualMeeting = toVirtualMeeting(meeting, meetingDetails, timezone)
            Result.success(virtualMeeting)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateMeeting(
        meetingId: String,
        title: String? = null,
        description: String? = null,
        scheduledFor: Instant? = null,
        duration: Duration? = null
    ): Result<com.guyghost.wakeve.models.VirtualMeeting> {
        return try {
            val updates = MeetingUpdates(
                title = title,
                description = description,
                startTime = scheduledFor,
                duration = duration
            )

            meetingRepository.updateMeeting(meetingId, updates)

            val meeting = meetingRepository.getMeetingById(meetingId) ?: return Result.failure(MeetingException.MeetingNotFound(meetingId))
            calendarService.updateNativeCalendarEvent(
                eventId = meeting.eventId,
                participantId = meeting.organizerId
            )

            val updatedMeeting = meetingRepository.getMeetingById(meetingId) ?: return Result.failure(MeetingException.MeetingNotFound(meetingId))
            Result.success(toVirtualMeeting(updatedMeeting, null, "UTC"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelMeeting(meetingId: String): Result<Unit> {
        return try {
            val meeting = meetingRepository.getMeetingById(meetingId)
                ?: return Result.failure(MeetingException.MeetingNotFound(meetingId))

            val platform = MeetingPlatform.valueOf(meeting.platform)

            when (platform) {
                MeetingPlatform.ZOOM -> zoomProvider.cancelMeeting(platform, meeting.hostMeetingId)
                MeetingPlatform.GOOGLE_MEET -> googleMeetProvider.cancelMeeting(platform, meeting.hostMeetingId)
                MeetingPlatform.FACETIME -> facetimeProvider.cancelMeeting(platform, meeting.hostMeetingId)
                else -> { }
            }

            meetingRepository.updateMeetingStatus(meetingId, MeetingStatus.CANCELLED)

            cancelMeetingReminders(meetingId)

            calendarService.removeFromNativeCalendar(
                eventId = meeting.eventId,
                participantId = meeting.organizerId
            )

            notifyParticipantsMeetingCancelled(meeting)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun startMeeting(meetingId: String): Result<com.guyghost.wakeve.models.VirtualMeeting> {
        return try {
            val meeting = meetingRepository.getMeetingById(meetingId)
                ?: return Result.failure(MeetingException.MeetingNotFound(meetingId))

            if (meeting.status == MeetingStatus.STARTED) {
                return Result.failure(MeetingException.MeetingStartedException(meetingId))
            }

            meetingRepository.updateMeetingStatus(meetingId, MeetingStatus.STARTED)

            val updated = meetingRepository.getMeetingById(meetingId) ?: return Result.failure(MeetingException.MeetingNotFound(meetingId))
            Result.success(toVirtualMeeting(updated, null, "UTC"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun endMeeting(meetingId: String): Result<com.guyghost.wakeve.models.VirtualMeeting> {
        return try {
            val meeting = meetingRepository.getMeetingById(meetingId)
                ?: return Result.failure(MeetingException.MeetingNotFound(meetingId))

            if (meeting.status == MeetingStatus.ENDED) {
                return Result.failure(MeetingException.MeetingAlreadyEndedException(meetingId))
            }

            meetingRepository.updateMeetingStatus(meetingId, MeetingStatus.ENDED)

            val updated = meetingRepository.getMeetingById(meetingId) ?: return Result.failure(MeetingException.MeetingNotFound(meetingId))
            Result.success(toVirtualMeeting(updated, null, "UTC"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendInvitations(meetingId: String): Result<Unit> {
        return try {
            val meeting = meetingRepository.getMeetingById(meetingId)
                ?: return Result.failure(MeetingException.MeetingNotFound(meetingId))

            val participants = participantQueries
                .selectByEventId(meeting.eventId)
                .executeAsList()
                .filter { it.hasValidatedDate == 1L }

            participants.forEach { participant ->
                database.meetingInvitationQueries.insertMeetingInvitation(
                    id = generateId(),
                    meeting_id = meetingId,
                    participant_id = participant.userId,
                    status = InvitationStatus.PENDING.name,
                    sent_at = Clock.System.now().toString(),
                    responded_at = null,
                    accepted_at = null
                )

                val notification = NotificationMessage(
                    id = generateId(),
                    userId = participant.userId,
                    type = NotificationType.EVENT_UPDATE,
                    title = "Invitation: ${meeting.title}",
                    body = "Vous êtes invité à une réunion virtuelle le ${formatDate(Instant.parse(meeting.startTime), "Europe/Paris")}",
                    data = mapOf("meetingId" to meetingId),
                    sentAt = Clock.System.now().toString(),
                    readAt = null
                )
                notificationService.sendNotification(notification)
            }

            calendarService.addToNativeCalendar(
                eventId = meeting.eventId,
                participantId = meeting.organizerId
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun respondToInvitation(
        invitationId: String,
        status: InvitationStatus
    ): Result<Unit> {
        return try {
            val invitation = database.meetingInvitationQueries
                .selectById(invitationId)
                .executeAsOneOrNull()
                ?: return Result.failure(MeetingException.InvitationNotFound(invitationId))

            val now = Clock.System.now().toString()
            val acceptedAt = if (status == InvitationStatus.ACCEPTED) now else null

            database.meetingInvitationQueries.updateMeetingInvitation(
                status = status.name,
                responded_at = now,
                accepted_at = acceptedAt,
                id = invitationId
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateMeetingLink(
        platform: MeetingPlatform,
        title: String,
        description: String?,
        scheduledFor: Instant,
        duration: Duration
    ): Result<MeetingLinkResponse> {
        return try {
            val meetingLink = when (platform) {
                MeetingPlatform.ZOOM -> zoomProvider.generateMeetingLink(
                    platform = platform,
                    title = title,
                    description = description,
                    startTime = scheduledFor,
                    duration = duration
                )
                MeetingPlatform.GOOGLE_MEET -> googleMeetProvider.generateMeetingLink(
                    platform = platform,
                    title = title,
                    description = description,
                    startTime = scheduledFor,
                    duration = duration
                )
                MeetingPlatform.FACETIME -> facetimeProvider.generateMeetingLink(
                    platform = platform,
                    title = title,
                    description = description,
                    startTime = scheduledFor,
                    duration = duration
                )
                else -> return Result.failure(MeetingException.UnsupportedPlatform(platform))
            }

            val hostMeetingId = when (platform) {
                MeetingPlatform.ZOOM -> zoomProvider.getHostMeetingId(meetingLink)
                MeetingPlatform.GOOGLE_MEET -> googleMeetProvider.getHostMeetingId(meetingLink)
                MeetingPlatform.FACETIME -> facetimeProvider.getHostMeetingId(meetingLink)
                else -> meetingLink
            }

            val dialInNumber = when (platform) {
                MeetingPlatform.ZOOM -> (zoomProvider as ZoomMeetingPlatformProvider).getDialInNumber()
                else -> null
            }

            val password = when (platform) {
                MeetingPlatform.ZOOM -> meetingLink.substringAfter("pwd=")
                else -> null
            }

            Result.success(
                MeetingLinkResponse(
                    meetingId = hostMeetingId,
                    meetingUrl = meetingLink,
                    dialInNumber = dialInNumber,
                    password = password
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun scheduleMeetingReminders(
        meetingId: String,
        eventId: String,
        scheduledFor: Instant,
        timezone: String
    ) {
        val timings = listOf(
            MeetingReminderTiming.ONE_DAY_BEFORE,
            MeetingReminderTiming.ONE_HOUR_BEFORE,
            MeetingReminderTiming.FIFTEEN_MINUTES_BEFORE,
            MeetingReminderTiming.FIVE_MINUTES_BEFORE
        )

        val participants = participantQueries
            .selectByEventId(eventId)
            .executeAsList()

        timings.forEach { timing ->
            val scheduledTime = calculateReminderTime(scheduledFor, timing, timezone)

            participants.forEach { participant ->
                database.meetingReminderQueries.insertMeetingReminder(
                    id = generateId(),
                    meeting_id = meetingId,
                    participant_id = participant.userId,
                    timing = timing.name,
                    scheduled_for = scheduledTime.toString(),
                    sent_at = null,
                    status = ReminderStatus.SCHEDULED.name
                )
            }
        }
    }

    private suspend fun cancelMeetingReminders(meetingId: String) {
    }

    private data class MeetingDetails(
        val url: String,
        val hostMeetingId: String,
        val password: String?,
        val dialInNumber: String?,
        val participantLimit: Int?,
        val requirePassword: Boolean,
        val waitingRoom: Boolean,
        val hostKey: String?
    )

    private fun createZoomMeetingDetails(
        title: String,
        description: String?,
        scheduledFor: Instant,
        duration: Duration,
        timezone: String,
        participantLimit: Int?,
        requirePassword: Boolean,
        waitingRoom: Boolean
    ): MeetingDetails {
        val meetingId = (1..10).map { Random.nextInt(0, 10) }.joinToString("")
        val password = generatePassword(6)
        val url = "https://zoom.us/j/\$meetingId?pwd=\$password"
        val dialIn = "+33 1 23 45 67 \${Random.nextInt(10, 99)}"
        val dialInPassword = meetingId.substring(0, 6)
        val hostKey = (1..6).map { Random.nextInt(0, 10) }.joinToString("")

        return MeetingDetails(
            url = url,
            hostMeetingId = meetingId,
            password = password,
            dialInNumber = dialIn,
            participantLimit = participantLimit,
            requirePassword = requirePassword,
            waitingRoom = waitingRoom,
            hostKey = hostKey
        )
    }

    private fun createGoogleMeetMeetingDetails(
        title: String,
        description: String?,
        scheduledFor: Instant,
        duration: Duration,
        timezone: String
    ): MeetingDetails {
        val chars = "abcdefghijklmnopqrstuvwxyz-"
        val part1 = (1..3).map { chars.random() }.joinToString("")
        val part2 = (1..3).map { chars.random() }.joinToString("")
        val part3 = (1..4).map { chars.random() }.joinToString("")
        val meetCode = "\$part1-\$part2-\$part3"
        val url = "https://meet.google.com/\$meetCode"

        return MeetingDetails(
            url = url,
            hostMeetingId = meetCode,
            password = null,
            dialInNumber = null,
            participantLimit = null,
            requirePassword = false,
            waitingRoom = false,
            hostKey = null
        )
    }

    private fun createFaceTimeMeetingDetails(
        title: String,
        description: String?,
        scheduledFor: Instant,
        duration: Duration,
        timezone: String,
        organizerId: String
    ): MeetingDetails {
        return MeetingDetails(
            url = "facetime://",
            hostMeetingId = organizerId,
            password = null,
            dialInNumber = null,
            participantLimit = null,
            requirePassword = false,
            waitingRoom = false,
            hostKey = null
        )
    }

    private fun toVirtualMeeting(
        meeting: com.guyghost.wakeve.meeting.Meeting,
        details: MeetingDetails?,
        timezone: String
    ): com.guyghost.wakeve.models.VirtualMeeting {
        return com.guyghost.wakeve.models.VirtualMeeting(
            id = meeting.id,
            eventId = meeting.eventId,
            organizerId = meeting.organizerId,
            platform = MeetingPlatform.valueOf(meeting.platform),
            meetingId = meeting.hostMeetingId,
            meetingPassword = meeting.password,
            meetingUrl = meeting.meetingLink,
            dialInNumber = details?.dialInNumber,
            dialInPassword = if (details != null && details.dialInNumber != null) {
                meeting.hostMeetingId.substring(0, 6.coerceAtMost(meeting.hostMeetingId.length))
            } else null,
            title = meeting.title,
            description = meeting.description,
            scheduledFor = meeting.startTime,
            duration = meeting.duration,
            timezone = timezone,
            participantLimit = details?.participantLimit,
            requirePassword = details?.requirePassword ?: true,
            waitingRoom = details?.waitingRoom ?: true,
            hostKey = details?.hostKey,
            createdAt = Clock.System.now(),
            status = meeting.status
        )
    }

    private fun calculateReminderTime(
        meetingTime: Instant,
        timing: MeetingReminderTiming,
        timezone: String
    ): Instant {
        val zone = TimeZone.of(timezone)
        val localTime = meetingTime.toLocalDateTime(zone)

        return when (timing) {
            MeetingReminderTiming.ONE_DAY_BEFORE -> localTime.minus(1, kotlin.time.Days).toInstant(zone)
            MeetingReminderTiming.ONE_HOUR_BEFORE -> localTime.minus(1, kotlin.time.Hours).toInstant(zone)
            MeetingReminderTiming.FIFTEEN_MINUTES_BEFORE -> localTime.minus(15, kotlin.time.Minutes).toInstant(zone)
            MeetingReminderTiming.FIVE_MINUTES_BEFORE -> localTime.minus(5, kotlin.time.Minutes).toInstant(zone)
        }
    }

    private fun notifyParticipantsMeetingCancelled(meeting: com.guyghost.wakeve.meeting.Meeting) {
    }

    private fun generatePassword(length: Int): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..length).map { chars.random() }.joinToString("")
    }

    private fun generateId(): String {
        return "\${Clock.System.now().toEpochMilliseconds()}_\${Random.nextInt(1000, 9999)}"
    }

    suspend fun getMeeting(meetingId: String): com.guyghost.wakeve.models.VirtualMeeting? {
        val meeting = meetingRepository.getMeetingById(meetingId) ?: return null
        return toVirtualMeeting(meeting, null, "UTC")
    }

    suspend fun getMeetingsForEvent(eventId: String): List<com.guyghost.wakeve.models.VirtualMeeting> {
        return meetingRepository.getMeetingsByEventId(eventId).map { toVirtualMeeting(it, null, "UTC") }
    }
}
