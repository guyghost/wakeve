package com.guyghost.wakeve.meeting

import com.guyghost.wakeve.database.WakevDb
import com.guyghost.wakeve.models.MeetingPlatform
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlin.time.Duration

/**
 * Repository pour la persistance des réunions
 */
class MeetingRepository(private val database: WakevDb) {
    
    private val meetingQueries = database.meetingQueries
    
    suspend fun createMeeting(meeting: Meeting): Result<Unit> {
        return try {
            val invitedParticipantsJson = Json.encodeToString(value = meeting.invitedParticipants)
            
            meetingQueries.insertMeeting(
                id = meeting.id,
                eventId = meeting.eventId,
                organizerId = meeting.organizerId,
                title = meeting.title,
                description = meeting.description,
                startTime = meeting.startTime.toString(),
                duration = meeting.duration.toString(),
                platform = meeting.platform.name,
                meetingLink = meeting.meetingLink,
                hostMeetingId = meeting.hostMeetingId,
                password = meeting.password,
                invitedParticipants = invitedParticipantsJson,
                status = meeting.status.name,
                createdAt = meeting.createdAt
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getMeetingById(id: String): Meeting? {
        return meetingQueries.selectById(id).executeAsOneOrNull()?.let { row ->
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
                invitedParticipants = Json.decodeFromString(row.invitedParticipants),
                status = MeetingStatus.valueOf(row.status),
                createdAt = row.createdAt
            )
        }
    }
    
    suspend fun updateMeeting(id: String, updates: MeetingUpdates): Result<Unit> {
        return try {
            val existing = getMeetingById(id) ?: return Result.failure(MeetingNotFoundException(id))
            
            meetingQueries.updateMeeting(
                title = updates.title ?: existing.title,
                description = updates.description ?: existing.description,
                startTime = (updates.startTime ?: existing.startTime).toString(),
                duration = (updates.duration ?: existing.duration).toString(),
                platform = (updates.platform ?: existing.platform).name,
                meetingLink = updates.meetingLink ?: existing.meetingLink,
                status = existing.status.name,
                id = id
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateMeetingStatus(id: String, status: MeetingStatus): Result<Unit> {
        return try {
            meetingQueries.updateMeetingStatus(status.name, id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteMeeting(id: String): Result<Unit> {
        return try {
            meetingQueries.deleteMeeting(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getMeetingsByEventId(eventId: String): List<Meeting> {
        return meetingQueries.selectByEventId(eventId).executeAsList().map { row ->
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
                invitedParticipants = Json.decodeFromString(row.invitedParticipants),
                status = MeetingStatus.valueOf(row.status),
                createdAt = row.createdAt
            )
        }
    }
}