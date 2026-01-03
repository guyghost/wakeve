package com.guyghost.wakeve.activity

import com.guyghost.wakeve.models.ActivitiesByDate
import com.guyghost.wakeve.models.Activity
import com.guyghost.wakeve.models.ActivityParticipant
import com.guyghost.wakeve.models.ActivityRequest
import com.guyghost.wakeve.models.ActivityWithStats
import com.guyghost.wakeve.models.ParticipantActivityStats
import kotlinx.datetime.Clock
import kotlin.random.Random

/**
 * Service for activity management
 * 
 * This service provides business logic for:
 * - Creating and validating activities
 * - Managing participant registration
 * - Checking capacity constraints
 * - Calculating activity statistics
 * - Validating activity data
 */
object ActivityManager {
    
    /**
     * Generate a random UUID string for cross-platform compatibility
     */
    private fun generateUuid(): String {
        val chars = "0123456789abcdef"
        return buildString(36) {
            repeat(36) { i ->
                when (i) {
                    8, 13, 18, 23 -> append('-')
                    14 -> append('4') // UUID version 4
                    19 -> append(chars[Random.nextInt(4) + 8]) // 8, 9, a, or b
                    else -> append(chars[Random.nextInt(16)])
                }
            }
        }
    }
    
    /**
     * Create a new activity
     */
    fun createActivity(
        eventId: String,
        request: ActivityRequest
    ): Activity {
        val validation = validateActivity(request)
        require(validation.isValid) { validation.errors.joinToString(", ") }
        
        val now = Clock.System.now().toString()
        return Activity(
            id = generateUuid(),
            eventId = eventId,
            scenarioId = request.scenarioId,
            name = request.name.trim(),
            description = request.description.trim(),
            date = request.date,
            time = request.time,
            duration = request.duration,
            location = request.location?.trim(),
            cost = request.cost,
            maxParticipants = request.maxParticipants,
            registeredParticipantIds = emptyList(),
            organizerId = request.organizerId,
            notes = request.notes?.trim(),
            createdAt = now,
            updatedAt = now
        )
    }
    
    /**
     * Register a participant to an activity
     * 
     * @return Updated activity with participant added, or null if registration failed
     */
    fun registerParticipant(
        activity: Activity,
        participantId: String,
        notes: String? = null
    ): RegistrationResult {
        // Check if already registered
        if (activity.registeredParticipantIds.contains(participantId)) {
            return RegistrationResult.AlreadyRegistered
        }
        
        // Check capacity
        if (!checkCapacity(activity)) {
            return RegistrationResult.Full
        }
        
        val updatedIds = activity.registeredParticipantIds + participantId
        val updatedActivity = activity.copy(
            registeredParticipantIds = updatedIds,
            updatedAt = Clock.System.now().toString()
        )
        
        val registration = ActivityParticipant(
            id = generateUuid(),
            activityId = activity.id,
            participantId = participantId,
            registeredAt = Clock.System.now().toString(),
            notes = notes
        )
        
        return RegistrationResult.Success(updatedActivity, registration)
    }
    
    /**
     * Unregister a participant from an activity
     */
    fun unregisterParticipant(
        activity: Activity,
        participantId: String
    ): Activity {
        val updatedIds = activity.registeredParticipantIds.filterNot { it == participantId }
        return activity.copy(
            registeredParticipantIds = updatedIds,
            updatedAt = Clock.System.now().toString()
        )
    }
    
    /**
     * Check if activity has available capacity
     */
    fun checkCapacity(activity: Activity): Boolean {
        val maxParticipants = activity.maxParticipants ?: return true // Unlimited
        return activity.registeredParticipantIds.size < maxParticipants
    }
    
    /**
     * Calculate activity statistics
     */
    fun calculateActivityStats(activity: Activity): ActivityWithStats {
        val registeredCount = activity.registeredParticipantIds.size
        val maxParticipants = activity.maxParticipants
        
        val spotsRemaining = if (maxParticipants != null) {
            maxParticipants - registeredCount
        } else {
            null // Unlimited
        }
        
        val isFull = if (maxParticipants != null) {
            registeredCount >= maxParticipants
        } else {
            false
        }
        
        val totalCost = if (activity.cost != null) {
            activity.cost * registeredCount
        } else {
            0L
        }
        
        return ActivityWithStats(
            activity = activity,
            registeredCount = registeredCount,
            spotsRemaining = spotsRemaining,
            isFull = isFull,
            totalCost = totalCost
        )
    }
    
    /**
     * Validate activity data
     */
    fun validateActivity(request: ActivityRequest): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (request.name.isBlank()) {
            errors.add("Activity name cannot be blank")
        }
        
        if (request.name.length > 100) {
            errors.add("Activity name must be 100 characters or less")
        }
        
        if (request.description.isBlank()) {
            errors.add("Activity description cannot be blank")
        }
        
        if (request.description.length > 500) {
            errors.add("Activity description must be 500 characters or less")
        }
        
        if (request.duration < 1) {
            errors.add("Duration must be at least 1 minute")
        }
        
        if (request.duration > 10080) { // 7 days in minutes
            errors.add("Duration cannot exceed 7 days (10080 minutes)")
        }
        
        if (request.cost != null && request.cost < 0) {
            errors.add("Cost cannot be negative")
        }
        
        if (request.maxParticipants != null && request.maxParticipants < 1) {
            errors.add("Max participants must be at least 1")
        }
        
        if (request.maxParticipants != null && request.maxParticipants > 1000) {
            errors.add("Max participants cannot exceed 1000")
        }
        
        // Validate time format if provided
        if (request.time != null && !isValidTimeFormat(request.time)) {
            errors.add("Time must be in HH:MM format (e.g., '14:30')")
        }
        
        // Validate date format if provided
        if (request.date != null && !isValidDateFormat(request.date)) {
            errors.add("Date must be in ISO 8601 format (e.g., '2025-12-20')")
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }
    
    /**
     * Validate time format (HH:MM)
     */
    private fun isValidTimeFormat(time: String): Boolean {
        val regex = Regex("^([01]\\d|2[0-3]):([0-5]\\d)$")
        return regex.matches(time)
    }
    
    /**
     * Validate date format (ISO 8601)
     */
    private fun isValidDateFormat(date: String): Boolean {
        val regex = Regex("^\\d{4}-\\d{2}-\\d{2}$")
        return regex.matches(date)
    }
    
    /**
     * Group activities by date
     */
    fun groupActivitiesByDate(activities: List<Activity>): List<ActivitiesByDate> {
        return activities
            .filter { it.date != null }
            .groupBy { it.date!! }
            .map { (date, dateActivities) ->
                ActivitiesByDate(
                    date = date,
                    activities = dateActivities.sortedBy { it.time },
                    totalActivities = dateActivities.size,
                    totalCost = dateActivities.mapNotNull { it.cost }.sum()
                )
            }
            .sortedBy { it.date }
    }
    
    /**
     * Calculate participant activity statistics
     */
    fun calculateParticipantStats(
        activities: List<Activity>,
        participantId: String
    ): ParticipantActivityStats {
        val participantActivities = activities.filter { 
            it.registeredParticipantIds.contains(participantId) 
        }
        
        val totalCost = participantActivities.mapNotNull { it.cost }.sum()
        val activityNames = participantActivities.map { it.name }
        
        return ParticipantActivityStats(
            participantId = participantId,
            registeredCount = participantActivities.size,
            totalCost = totalCost,
            activityNames = activityNames
        )
    }
}

/**
 * Result of a registration attempt
 */
sealed class RegistrationResult {
    data class Success(val activity: Activity, val registration: ActivityParticipant) : RegistrationResult()
    object AlreadyRegistered : RegistrationResult()
    object Full : RegistrationResult()
}

/**
 * Validation result data class
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String>
)
