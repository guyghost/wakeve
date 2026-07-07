package com.guyghost.wakeve.meeting

import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.MeetingPlatform
import kotlinx.datetime.Instant
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

/**
 * Repository pour la persistance des réunions
 */
class MeetingRepository(private val database: WakeveDb) {
    
    private val meetingQueries = database.meetingQueries
    
    suspend fun createMeeting(meeting: Meeting): Result<Unit> {
        return try {
            val normalizedMeeting = meeting.normalized()
            val invitedParticipantsJson = Json.encodeToString(
                ListSerializer(String.serializer()),
                normalizedMeeting.invitedParticipants
            )
            
            meetingQueries.insertMeeting(
                id = normalizedMeeting.id,
                eventId = normalizedMeeting.eventId,
                organizerId = normalizedMeeting.organizerId,
                title = normalizedMeeting.title,
                description = normalizedMeeting.description,
                startTime = normalizedMeeting.startTime.toString(),
                duration = normalizedMeeting.duration.toString(),
                platform = normalizedMeeting.platform.name,
                meetingLink = normalizedMeeting.meetingLink,
                provider = normalizedMeeting.platform.name,
                displayLabel = safeLinkDisplayLabel(normalizedMeeting.platform),
                targetUrl = normalizedMeeting.meetingLink,
                creatorId = normalizedMeeting.organizerId,
                verificationState = "VERIFIED",
                hostMeetingId = normalizedMeeting.hostMeetingId,
                password = normalizedMeeting.password,
                invitedParticipants = invitedParticipantsJson,
                status = normalizedMeeting.status.name,
                createdAt = normalizedMeeting.createdAt
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getMeetingById(id: String): Meeting? {
        return meetingQueries.selectById(id.trim()).executeAsOneOrNull()?.let { row ->
            Meeting(
                id = row.id,
                eventId = row.eventId,
                organizerId = row.organizerId,
                title = row.title,
                description = row.description,
                startTime = Instant.parse(row.startTime),
                duration = Duration.parse(row.duration),
                platform = MeetingPlatform.valueOf(row.platform),
                meetingLink = row.meetingLink,
                hostMeetingId = row.hostMeetingId,
                password = row.password,
                invitedParticipants = Json.decodeFromString(ListSerializer(String.serializer()), row.invitedParticipants),
                status = MeetingStatus.valueOf(row.status),
                createdAt = row.createdAt
            )
        }
    }

    suspend fun getEventStatusForMeeting(id: String): EventStatus? {
        val meeting = getMeetingById(id) ?: return null
        return database.eventQueries.selectById(meeting.eventId).executeAsOneOrNull()?.status?.let {
            EventStatus.valueOf(it)
        }
    }
    
    suspend fun updateMeeting(id: String, updates: MeetingUpdates): Result<Unit> {
        return try {
            val normalizedId = id.trim()
            val existing = getMeetingById(normalizedId) ?: return Result.failure(MeetingNotFoundException(normalizedId))
            val normalizedMeeting = existing.copy(
                title = updates.title ?: existing.title,
                description = updates.description ?: existing.description,
                startTime = updates.startTime ?: existing.startTime,
                duration = updates.duration ?: existing.duration,
                platform = updates.platform ?: existing.platform,
                meetingLink = updates.meetingLink ?: existing.meetingLink
            ).normalized()
            
            meetingQueries.updateMeeting(
                title = normalizedMeeting.title,
                description = normalizedMeeting.description,
                startTime = normalizedMeeting.startTime.toString(),
                duration = normalizedMeeting.duration.toString(),
                platform = normalizedMeeting.platform.name,
                meetingLink = normalizedMeeting.meetingLink,
                status = normalizedMeeting.status.name,
                id = normalizedId
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateMeetingStatus(id: String, status: MeetingStatus): Result<Unit> {
        return try {
            val normalizedId = id.trim()
            getMeetingById(normalizedId) ?: return Result.failure(MeetingNotFoundException(normalizedId))
            meetingQueries.updateMeetingStatus(status.name, normalizedId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteMeeting(id: String): Result<Unit> {
        return try {
            val normalizedId = id.trim()
            getMeetingById(normalizedId) ?: return Result.failure(MeetingNotFoundException(normalizedId))
            meetingQueries.deleteMeeting(normalizedId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getMeetingsByEventId(eventId: String): List<Meeting> {
        return meetingQueries.selectByEventId(eventId.trim()).executeAsList().map { row ->
            Meeting(
                id = row.id,
                eventId = row.eventId,
                organizerId = row.organizerId,
                title = row.title,
                description = row.description,
                startTime = Instant.parse(row.startTime),
                duration = Duration.parse(row.duration),
                platform = MeetingPlatform.valueOf(row.platform),
                meetingLink = row.meetingLink,
                hostMeetingId = row.hostMeetingId,
                password = row.password,
                invitedParticipants = Json.decodeFromString(ListSerializer(String.serializer()), row.invitedParticipants),
                status = MeetingStatus.valueOf(row.status),
                createdAt = row.createdAt
            )
        }
    }

    private fun safeLinkDisplayLabel(platform: MeetingPlatform): String =
        when (platform) {
            MeetingPlatform.ZOOM -> "Zoom meeting"
            MeetingPlatform.GOOGLE_MEET -> "Google Meet"
            MeetingPlatform.FACETIME -> "FaceTime call"
            MeetingPlatform.TEAMS -> "Microsoft Teams meeting"
            MeetingPlatform.WEBEX -> "Webex meeting"
        }

    private fun Meeting.normalized(): Meeting {
        val normalizedId = id.trim()
        val normalizedEventId = eventId.trim()
        val normalizedOrganizerId = organizerId.trim()
        val normalizedTitle = title.trim()
        val normalizedDescription = description?.trim()?.takeIf { it.isNotEmpty() }
        val normalizedMeetingLink = meetingLink.trim()
        val normalizedHostMeetingId = hostMeetingId.trim()
        val normalizedPassword = password.trim()
        val normalizedParticipants = invitedParticipants
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
        val normalizedCreatedAt = createdAt.trim()

        require(normalizedId.isNotEmpty()) { "Meeting id is required" }
        require(normalizedEventId.isNotEmpty()) { "Meeting eventId is required" }
        require(normalizedOrganizerId.isNotEmpty()) { "Meeting organizerId is required" }
        require(normalizedTitle.isNotEmpty()) { "Meeting title is required" }
        require(normalizedTitle.length <= 160) { "Meeting title must not exceed 160 characters" }
        require(normalizedDescription == null || normalizedDescription.length <= 2_000) {
            "Meeting description must not exceed 2000 characters"
        }
        require(duration > Duration.ZERO && duration <= 24.hours) {
            "Meeting duration must be between 1 second and 24 hours"
        }
        require(isTrustedMeetingLink(platform, normalizedMeetingLink)) {
            "Unsafe meeting link rejected"
        }
        require(normalizedHostMeetingId.isNotEmpty()) { "Meeting hostMeetingId is required" }
        require(normalizedHostMeetingId.length <= 200) { "Meeting hostMeetingId must not exceed 200 characters" }
        require(normalizedPassword.length <= 128) { "Meeting password must not exceed 128 characters" }
        require(normalizedParticipants.size <= 1_000) { "Meeting invited participant count must not exceed 1000" }
        require(normalizedCreatedAt.isNotEmpty()) { "Meeting createdAt is required" }

        return copy(
            id = normalizedId,
            eventId = normalizedEventId,
            organizerId = normalizedOrganizerId,
            title = normalizedTitle,
            description = normalizedDescription,
            meetingLink = normalizedMeetingLink,
            hostMeetingId = normalizedHostMeetingId,
            password = normalizedPassword,
            invitedParticipants = normalizedParticipants,
            createdAt = normalizedCreatedAt
        )
    }

    private fun isTrustedMeetingLink(platform: MeetingPlatform, url: String): Boolean {
        if (containsTemplateMarker(url)) return false
        return when (platform) {
            MeetingPlatform.ZOOM -> Regex("""^https://zoom\.us/j/\d{10}\?pwd=[A-Za-z0-9]{6,64}$""").matches(url)
            MeetingPlatform.GOOGLE_MEET -> Regex("""^https://meet\.google\.com/[a-z]{3}-[a-z]{3}-[a-z]{4}$""").matches(url)
            MeetingPlatform.FACETIME -> url == "facetime://"
            MeetingPlatform.TEAMS -> hasTrustedHttpsHost(
                url = url,
                allowedHosts = setOf("teams.microsoft.com", "teams.live.com")
            )
            MeetingPlatform.WEBEX -> hasTrustedHttpsHost(
                url = url,
                allowedHosts = setOf("webex.com", "www.webex.com")
            ) { host -> host.endsWith(".webex.com") }
        }
    }

    private fun hasTrustedHttpsHost(
        url: String,
        allowedHosts: Set<String>,
        extraHostCheck: (String) -> Boolean = { false }
    ): Boolean {
        val match = Regex("""^https://([^/?#]+)(?:[/?#].*)?$""").find(url) ?: return false
        val host = match.groupValues[1].lowercase()
        return host in allowedHosts || extraHostCheck(host)
    }

    private fun containsTemplateMarker(value: String): Boolean =
        value.contains("\${") ||
            value.contains("{") ||
            value.contains("}") ||
            Regex("""\$[A-Za-z_][A-Za-z0-9_]*""").containsMatchIn(value)
}
