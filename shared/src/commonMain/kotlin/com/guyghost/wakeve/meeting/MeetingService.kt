package com.guyghost.wakeve.meeting

import com.guyghost.wakeve.notification.NotificationServiceInterface
import com.guyghost.wakeve.calendar.CalendarService
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.InvitationStatus
import com.guyghost.wakeve.models.MeetingLinkResponse
import com.guyghost.wakeve.models.MeetingPlatform
import com.guyghost.wakeve.models.MeetingReminderTiming
import com.guyghost.wakeve.models.NotificationMessage
import com.guyghost.wakeve.models.NotificationType
import com.guyghost.wakeve.models.ReminderStatus
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

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
    private val database: WakeveDb,
    private val calendarService: CalendarService,
    private val notificationService: NotificationServiceInterface,
    private val meetingLinkProvider: MeetingLinkProvider = NoConfiguredMeetingLinkProvider
) {

    private val eventQueries = database.eventQueries
    private val participantQueries = database.participantQueries
    private val syncMetadataQueries = database.syncMetadataQueries
    private val meetingRepository: MeetingRepository = MeetingRepository(database)

    private data class ConfirmedParticipant(
        val participantId: String,
        val userId: String
    )

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
            val normalizedEventId = eventId.trim()
            val normalizedOrganizerId = organizerId.trim()
            val normalizedTitle = title.trim()
            val normalizedDescription = description?.trim()?.takeIf { it.isNotEmpty() }
            val normalizedTimezone = timezone.trim().ifEmpty { "UTC" }

            val event = eventQueries.selectById(normalizedEventId).executeAsOneOrNull()
                ?: return Result.failure(MeetingException.EventNotFound(normalizedEventId))

            val eventStatus = EventStatus.valueOf(event.status)
            if (eventStatus != EventStatus.ORGANIZING) {
                return Result.failure(MeetingException.InvalidEventStatus(eventStatus))
            }
            if (event.organizerId != normalizedOrganizerId) {
                return Result.failure(MeetingException.UnauthorizedAccess())
            }

            val confirmedParticipants = getConfirmedParticipants(normalizedEventId)
            val invitedParticipantIds = confirmedParticipants.map { it.participantId }

            val meetingDetails = when (platform) {
                MeetingPlatform.ZOOM,
                MeetingPlatform.GOOGLE_MEET,
                MeetingPlatform.FACETIME -> meetingLinkProvider.createMeeting(
                    platform = platform,
                    title = normalizedTitle,
                    description = normalizedDescription,
                    scheduledFor = scheduledFor,
                    duration = duration,
                    timezone = normalizedTimezone,
                    participantLimit = participantLimit,
                    requirePassword = requirePassword,
                    waitingRoom = waitingRoom
                )
                MeetingPlatform.TEAMS, MeetingPlatform.WEBEX ->
                    return Result.failure(Exception("Platform $platform is not supported"))
            }
            if (!isTrustedMeetingLink(platform, meetingDetails.url)) {
                return Result.failure(IllegalArgumentException("Unsafe meeting link rejected"))
            }

            val meeting = com.guyghost.wakeve.meeting.Meeting(
                id = generateId(),
                eventId = normalizedEventId,
                organizerId = normalizedOrganizerId,
                title = normalizedTitle,
                description = normalizedDescription,
                startTime = scheduledFor,
                duration = duration,
                platform = platform,
                meetingLink = meetingDetails.url,
                hostMeetingId = meetingDetails.hostMeetingId,
                password = meetingDetails.password ?: "",
                invitedParticipants = invitedParticipantIds,
                status = com.guyghost.wakeve.meeting.MeetingStatus.SCHEDULED,
                createdAt = Clock.System.now().toString()
            )

            meetingRepository.createMeeting(meeting).getOrThrow()
            val persistedMeeting = meetingRepository.getMeetingById(meeting.id)
                ?: return Result.failure(MeetingException.MeetingNotFound(meeting.id))
            queueMeetingCreateSync(persistedMeeting, meetingDetails)

            scheduleMeetingReminders(
                meetingId = persistedMeeting.id,
                eventId = persistedMeeting.eventId,
                scheduledFor = persistedMeeting.startTime,
                timezone = normalizedTimezone,
                participantIds = persistedMeeting.invitedParticipants
            )

            prepareCalendarEntries(
                eventId = persistedMeeting.eventId,
                participantIds = persistedMeeting.invitedParticipants
            )

            notifyMeetingCreated(
                meeting = persistedMeeting,
                userIds = confirmedParticipants.map { it.userId }
            )

            val virtualMeeting = toVirtualMeeting(persistedMeeting, meetingDetails, normalizedTimezone)
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

            meetingRepository.updateMeeting(meetingId, updates).getOrThrow()

            val meeting = meetingRepository.getMeetingById(meetingId) ?: return Result.failure(Exception("Meeting $meetingId not found"))
            calendarService.updateNativeCalendarEvent(
                eventId = meeting.eventId,
                participantId = meeting.organizerId
            ).getOrThrow()

            val updatedMeeting = meetingRepository.getMeetingById(meetingId) ?: return Result.failure(MeetingException.MeetingNotFound(meetingId))
            Result.success(toVirtualMeeting(updatedMeeting, null, "UTC"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelMeeting(meetingId: String): Result<Unit> {
        return try {
            val meeting = meetingRepository.getMeetingById(meetingId)
                ?: return Result.failure(Exception("Meeting $meetingId not found"))

            val platform = meeting.platform

            when (platform) {
                MeetingPlatform.ZOOM,
                MeetingPlatform.GOOGLE_MEET,
                MeetingPlatform.FACETIME -> meetingLinkProvider.cancelMeeting(platform, meeting.hostMeetingId).getOrThrow()
                MeetingPlatform.TEAMS,
                MeetingPlatform.WEBEX -> { }
            }

            meetingRepository.updateMeetingStatus(meetingId, com.guyghost.wakeve.meeting.MeetingStatus.CANCELLED).getOrThrow()

            cancelMeetingReminders(meetingId)

            notifyParticipantsMeetingCancelled(meeting)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun startMeeting(meetingId: String): Result<com.guyghost.wakeve.models.VirtualMeeting> {
        return try {
            val meeting = meetingRepository.getMeetingById(meetingId)
                ?: return Result.failure(Exception("Meeting $meetingId not found"))

            if (meeting.status == com.guyghost.wakeve.meeting.MeetingStatus.STARTED) {
                return Result.failure(Exception("Meeting $meetingId has already been started"))
            }

            meetingRepository.updateMeetingStatus(meetingId, com.guyghost.wakeve.meeting.MeetingStatus.STARTED).getOrThrow()

            val updated = meetingRepository.getMeetingById(meetingId) ?: return Result.failure(Exception("Meeting $meetingId not found"))
            Result.success(toVirtualMeeting(updated, null, "UTC"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun endMeeting(meetingId: String): Result<com.guyghost.wakeve.models.VirtualMeeting> {
        return try {
            val meeting = meetingRepository.getMeetingById(meetingId)
                ?: return Result.failure(Exception("Meeting $meetingId not found"))

            if (meeting.status == com.guyghost.wakeve.meeting.MeetingStatus.ENDED) {
                return Result.failure(Exception("Meeting $meetingId has already ended"))
            }

            meetingRepository.updateMeetingStatus(meetingId, com.guyghost.wakeve.meeting.MeetingStatus.ENDED).getOrThrow()

            val updated = meetingRepository.getMeetingById(meetingId) ?: return Result.failure(Exception("Meeting $meetingId not found"))
            Result.success(toVirtualMeeting(updated, null, "UTC"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendInvitations(meetingId: String): Result<Unit> {
        return try {
            val normalizedMeetingId = meetingId.trim()
            val meeting = meetingRepository.getMeetingById(normalizedMeetingId)
                ?: return Result.failure(Exception("Meeting $normalizedMeetingId not found"))

            val participants = participantQueries
                .selectByEventId(meeting.eventId)
                .executeAsList()
                .filter { it.hasValidatedDate == 1L }
                .mapNotNull {
                    val participantId = it.id.trim()
                    val userId = it.userId.trim()
                    if (participantId.isEmpty() || userId.isEmpty()) {
                        null
                    } else {
                        ConfirmedParticipant(participantId = participantId, userId = userId)
                    }
                }
                .distinctBy { it.participantId }
            val alreadyInvited = database.meetingInvitationQueries
                .selectByMeetingId(meeting.id)
                .executeAsList()
                .map { it.participant_id }
                .toSet()

            participants
                .filterNot { it.participantId in alreadyInvited }
                .forEach { participant ->
                    database.meetingInvitationQueries.insertMeetingInvitation(
                        id = generateId(),
                        meeting_id = meeting.id,
                        participant_id = participant.participantId,
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
                        body = "Vous êtes invité à une réunion virtuelle le ${formatDate(meeting.startTime)}",
                        data = mapOf("meetingId" to meeting.id),
                        sentAt = Clock.System.now().toString(),
                        readAt = null
                    )
                    notificationService.sendNotification(notification).getOrThrow()
                }

            calendarService.addToNativeCalendar(
                eventId = meeting.eventId,
                participantId = meeting.organizerId
            ).getOrThrow()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun respondToInvitation(
        invitationId: String,
        currentUserId: String,
        status: InvitationStatus
    ): Result<Unit> {
        return try {
            val normalizedInvitationId = invitationId.trim()
            val normalizedUserId = currentUserId.trim()
            if (normalizedInvitationId.isEmpty()) {
                return Result.failure(IllegalArgumentException("invitationId is required"))
            }
            if (normalizedUserId.isEmpty()) {
                return Result.failure(IllegalArgumentException("currentUserId is required"))
            }
            if (status == InvitationStatus.PENDING) {
                return Result.failure(IllegalArgumentException("Invitation response must be ACCEPTED, DECLINED, or TENTATIVE"))
            }

            val invitation = database.meetingInvitationQueries
                .selectById(normalizedInvitationId)
                .executeAsOneOrNull()
                ?: return Result.failure(Exception("Invitation $normalizedInvitationId not found"))
            val participant = participantQueries
                .selectById(invitation.participant_id)
                .executeAsOneOrNull()
                ?: return Result.failure(Exception("Participant ${invitation.participant_id} not found"))
            if (participant.userId.trim() != normalizedUserId) {
                return Result.failure(MeetingException.UnauthorizedAccess())
            }

            val now = Clock.System.now().toString()
            val acceptedAt = if (status == InvitationStatus.ACCEPTED) now else null

            database.meetingInvitationQueries.updateMeetingInvitation(
                status = status.name,
                responded_at = now,
                accepted_at = acceptedAt,
                id = normalizedInvitationId
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
            val response = when (platform) {
                MeetingPlatform.ZOOM,
                MeetingPlatform.GOOGLE_MEET,
                MeetingPlatform.FACETIME -> meetingLinkProvider.generateMeetingLink(
                        platform = platform,
                        title = title,
                        description = description,
                        scheduledFor = scheduledFor,
                        duration = duration
                    )
                else -> return Result.failure(Exception("Platform $platform is not supported"))
            }
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun scheduleMeetingReminders(
        meetingId: String,
        eventId: String,
        scheduledFor: Instant,
        timezone: String,
        participantIds: List<String> = getConfirmedParticipants(eventId).map { it.participantId }
    ) {
        val timings = listOf(
            MeetingReminderTiming.ONE_DAY_BEFORE,
            MeetingReminderTiming.ONE_HOUR_BEFORE,
            MeetingReminderTiming.FIFTEEN_MINUTES_BEFORE,
            MeetingReminderTiming.FIVE_MINUTES_BEFORE
        )

        timings.forEach { timing ->
            val scheduledTime = calculateReminderTime(scheduledFor, timing, timezone)

            participantIds.forEach { participantId ->
                database.meetingReminderQueries.insertMeetingReminder(
                    id = meetingReminderId(meetingId, participantId, timing),
                    meeting_id = meetingId,
                    participant_id = participantId,
                    timing = timing.name,
                    scheduled_for = scheduledTime.toString(),
                    sent_at = null,
                    status = ReminderStatus.SCHEDULED.name
                )
            }
        }
    }

    private fun meetingReminderId(
        meetingId: String,
        participantId: String,
        timing: MeetingReminderTiming
    ): String = "reminder-$meetingId-$participantId-${timing.name}"

    private suspend fun cancelMeetingReminders(meetingId: String) {
        database.meetingReminderQueries.deleteByMeetingId(meetingId)
    }

    private fun getConfirmedParticipants(eventId: String): List<ConfirmedParticipant> =
        participantQueries
            .selectByEventId(eventId)
            .executeAsList()
            .filter { it.hasValidatedDate == 1L }
            .mapNotNull {
                val participantId = it.id.trim()
                val userId = it.userId.trim()
                if (participantId.isEmpty() || userId.isEmpty()) {
                    null
                } else {
                    ConfirmedParticipant(participantId = participantId, userId = userId)
                }
            }
            .distinctBy { it.participantId }

    private suspend fun prepareCalendarEntries(
        eventId: String,
        participantIds: List<String>
    ) {
        participantIds.forEach { participantId ->
            calendarService.addToNativeCalendar(
                eventId = eventId,
                participantId = participantId
            ).getOrThrow()
        }
    }

    private suspend fun notifyMeetingCreated(
        meeting: com.guyghost.wakeve.meeting.Meeting,
        userIds: List<String>
    ) {
        userIds.distinct().forEach { userId ->
            val notification = NotificationMessage(
                id = generateId(),
                userId = userId,
                type = NotificationType.EVENT_UPDATE,
                title = "Réunion créée: ${meeting.title}",
                body = "Une réunion virtuelle est prévue le ${formatDate(meeting.startTime)}",
                data = mapOf(
                    "eventId" to meeting.eventId,
                    "meetingId" to meeting.id
                ),
                sentAt = Clock.System.now().toString(),
                readAt = null
            )
            notificationService.sendNotification(notification).getOrThrow()
        }
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
            platform = meeting.platform,
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
            status = com.guyghost.wakeve.models.MeetingStatus.valueOf(meeting.status.name)
        )
    }

    private fun calculateReminderTime(
        meetingTime: Instant,
        timing: MeetingReminderTiming,
        timezone: String
    ): Instant {
        return when (timing) {
            MeetingReminderTiming.ONE_DAY_BEFORE -> meetingTime.minus(1.days)
            MeetingReminderTiming.ONE_HOUR_BEFORE -> meetingTime.minus(1.hours)
            MeetingReminderTiming.FIFTEEN_MINUTES_BEFORE -> meetingTime.minus(15.minutes)
            MeetingReminderTiming.FIVE_MINUTES_BEFORE -> meetingTime.minus(5.minutes)
        }
    }

    private suspend fun notifyParticipantsMeetingCancelled(meeting: com.guyghost.wakeve.meeting.Meeting) {
        meeting.invitedParticipants.distinct().forEach { participantId ->
            val userId = participantQueries
                .selectById(participantId)
                .executeAsOneOrNull()
                ?.userId
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: participantId
            val notification = NotificationMessage(
                id = generateId(),
                userId = userId,
                type = NotificationType.EVENT_UPDATE,
                title = "Réunion annulée: ${meeting.title}",
                body = "La réunion virtuelle prévue le ${formatDate(meeting.startTime)} a été annulée",
                data = mapOf(
                    "eventId" to meeting.eventId,
                    "meetingId" to meeting.id
                ),
                sentAt = Clock.System.now().toString(),
                readAt = null
            )
            notificationService.sendNotification(notification).getOrThrow()
        }
    }

    private fun queueMeetingCreateSync(
        meeting: com.guyghost.wakeve.meeting.Meeting,
        details: MeetingDetails
    ) {
        val timestamp = Clock.System.now().toString()
        val payload = buildString {
            append("{")
            appendJson("id", meeting.id)
            append(",")
            appendJson("eventId", meeting.eventId)
            append(",")
            appendJson("organizerId", meeting.organizerId)
            append(",")
            appendJson("platform", meeting.platform.name)
            append(",")
            appendJson("title", meeting.title)
            append(",")
            appendJson("meetingLink", meeting.meetingLink)
            append(",")
            appendJson("targetUrl", meeting.meetingLink)
            append(",")
            appendJson("hostMeetingId", meeting.hostMeetingId)
            append(",")
            appendJson("password", meeting.password)
            append(",")
            appendJson("status", meeting.status.name)
            append(",")
            appendJson("scheduledFor", meeting.startTime.toString())
            append(",")
            appendJson("duration", meeting.duration.toString())
            append(",")
            appendJson("dialInNumber", details.dialInNumber.orEmpty())
            append("}")
        }
        syncMetadataQueries.insertSyncMetadataWithPayload(
            id = "sync-meeting-create-${meeting.id}-$timestamp",
            entityType = "meeting",
            entityId = meeting.id,
            operation = "CREATE",
            payload = payload,
            timestamp = timestamp,
            retryState = "READY",
            retryCount = 0,
            synced = 0
        )
    }

    private fun StringBuilder.appendJson(key: String, value: String) {
        append("\"")
        append(key)
        append("\":\"")
        append(value.replace("\\", "\\\\").replace("\"", "\\\""))
        append("\"")
    }

    private fun formatDate(instant: Instant): String {
        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        return "${localDateTime.date} ${localDateTime.time}"
    }

    private fun generateId(): String {
        return "${Clock.System.now().toEpochMilliseconds()}_${Random.nextInt(1000, 9999)}"
    }

    private fun isTrustedMeetingLink(platform: MeetingPlatform, url: String): Boolean {
        if (containsTemplateMarker(url)) return false
        return when (platform) {
            MeetingPlatform.ZOOM -> Regex("""^https://zoom\.us/j/\d{10}\?pwd=[A-Z0-9]{6}$""").matches(url)
            MeetingPlatform.GOOGLE_MEET -> Regex("""^https://meet\.google\.com/[a-z]{3}-[a-z]{3}-[a-z]{4}$""").matches(url)
            MeetingPlatform.FACETIME -> url == "facetime://"
            MeetingPlatform.TEAMS,
            MeetingPlatform.WEBEX -> false
        }
    }

    private fun containsTemplateMarker(value: String): Boolean =
        value.contains("\${") ||
            value.contains("{") ||
            value.contains("}") ||
            Regex("""\$[A-Za-z_][A-Za-z0-9_]*""").containsMatchIn(value)

    suspend fun getMeeting(meetingId: String): com.guyghost.wakeve.models.VirtualMeeting? {
        val meeting = meetingRepository.getMeetingById(meetingId) ?: return null
        return toVirtualMeeting(meeting, null, "UTC")
    }

    suspend fun getMeetingsForEvent(eventId: String): List<com.guyghost.wakeve.models.VirtualMeeting> {
        return meetingRepository.getMeetingsByEventId(eventId).map { toVirtualMeeting(it, null, "UTC") }
    }
}
