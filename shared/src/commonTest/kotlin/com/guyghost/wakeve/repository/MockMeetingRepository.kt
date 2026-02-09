package com.guyghost.wakeve.repository

import com.guyghost.wakeve.meeting.Meeting
import com.guyghost.wakeve.meeting.MeetingStatus
import com.guyghost.wakeve.meeting.MeetingUpdates

/**
 * Mock implementation of MeetingRepository for testing
 * This provides an in-memory implementation for testing without database dependencies
 */
class MockMeetingRepository {
    
    // Keep a mutable map for operations
    val meetings = mutableMapOf<String, Meeting>()
    var shouldFailCreate = false
    var shouldFailUpdate = false
    var shouldFailGet = false
    var shouldFailDelete = false
    
    suspend fun createMeeting(meeting: Meeting): Result<Unit> {
        return if (shouldFailCreate) {
            Result.failure(Exception("Repository error: Failed to create meeting"))
        } else {
            meetings[meeting.id] = meeting
            Result.success(Unit)
        }
    }
    
    suspend fun getMeetingById(id: String): Meeting? {
        return if (shouldFailGet) {
            null
        } else {
            meetings[id]
        }
    }
    
    suspend fun updateMeeting(id: String, updates: MeetingUpdates): Result<Unit> {
        return if (shouldFailUpdate) {
            Result.failure(Exception("Repository error: Failed to update meeting"))
        } else {
            val existing = meetings[id]
            if (existing == null) {
                Result.failure(Exception("Meeting not found: $id"))
            } else {
                val updated = existing.copy(
                    title = updates.title ?: existing.title,
                    description = updates.description ?: existing.description,
                    startTime = updates.startTime ?: existing.startTime,
                    duration = updates.duration ?: existing.duration,
                    platform = updates.platform ?: existing.platform,
                    meetingLink = updates.meetingLink ?: existing.meetingLink
                )
                meetings[id] = updated
                Result.success(Unit)
            }
        }
    }
    
    suspend fun updateMeetingStatus(id: String, status: MeetingStatus): Result<Unit> {
        return if (shouldFailUpdate) {
            Result.failure(Exception("Repository error: Failed to update meeting status"))
        } else {
            val existing = meetings[id]
            if (existing == null) {
                Result.failure(Exception("Meeting not found: $id"))
            } else {
                val updated = existing.copy(status = status)
                meetings[id] = updated
                Result.success(Unit)
            }
        }
    }
    
    suspend fun deleteMeeting(id: String): Result<Unit> {
        return if (shouldFailDelete) {
            Result.failure(Exception("Repository error: Failed to delete meeting"))
        } else {
            val removed = meetings.remove(id)
            if (removed == null) {
                Result.failure(Exception("Meeting not found: $id"))
            } else {
                Result.success(Unit)
            }
        }
    }
    
    suspend fun getMeetingsByEventId(eventId: String): List<Meeting> {
        return if (shouldFailGet) {
            emptyList()
        } else {
            meetings.values.filter { it.eventId == eventId }
        }
    }
    
    // Helper methods for tests
    
    /**
     * Clear all meetings
     */
    fun clear() {
        meetings.clear()
    }
    
    /**
     * Get current count of meetings
     */
    fun getCount(): Int = meetings.size
    
    /**
     * Check if a meeting exists
     */
    fun hasMeeting(id: String): Boolean = meetings.containsKey(id)
    
    /**
     * Add a meeting without going through the create method
     */
    fun addMeetingDirectly(meeting: Meeting) {
        meetings[meeting.id] = meeting
    }
}