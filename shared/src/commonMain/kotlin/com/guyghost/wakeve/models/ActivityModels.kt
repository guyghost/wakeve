package com.guyghost.wakeve.models

import kotlinx.serialization.Serializable

/**
 * Activity for an event
 * 
 * Represents a planned activity with participant registration.
 * 
 * @property id Unique identifier
 * @property eventId Event this activity belongs to
 * @property scenarioId Optional link to a scenario
 * @property name Name of the activity (e.g., "Hike to the lake", "Beach volleyball")
 * @property description Detailed description
 * @property date Date of the activity (ISO 8601 date, optional)
 * @property time Time of the activity (HH:MM format, optional)
 * @property duration Duration in minutes
 * @property location Where the activity takes place (optional)
 * @property cost Cost per participant in cents (optional)
 * @property maxParticipants Maximum number of participants (null for unlimited)
 * @property registeredParticipantIds List of registered participant IDs
 * @property organizerId Participant ID who organizes this activity
 * @property notes Additional notes
 * @property createdAt Creation timestamp (ISO 8601 UTC)
 * @property updatedAt Last update timestamp (ISO 8601 UTC)
 */
@Serializable
data class Activity(
    val id: String,
    val eventId: String,
    val scenarioId: String? = null,
    val name: String,
    val description: String,
    val date: String? = null,  // ISO 8601 date (e.g., "2025-12-20")
    val time: String? = null,  // HH:MM format (e.g., "14:00")
    val duration: Int,  // Minutes
    val location: String? = null,
    val cost: Long? = null,  // In cents per participant
    val maxParticipants: Int? = null,  // null = unlimited
    val registeredParticipantIds: List<String>,
    val organizerId: String,
    val notes: String? = null,
    val createdAt: String,  // ISO 8601 UTC timestamp
    val updatedAt: String   // ISO 8601 UTC timestamp
)

/**
 * Activity with additional metadata
 * 
 * @property activity The activity
 * @property registeredCount Number of registered participants
 * @property spotsRemaining Remaining spots (null if unlimited)
 * @property isFull Whether the activity is full
 * @property totalCost Total cost for all registered participants in cents
 */
@Serializable
data class ActivityWithStats(
    val activity: Activity,
    val registeredCount: Int,
    val spotsRemaining: Int?,  // null if unlimited capacity
    val isFull: Boolean,
    val totalCost: Long  // In cents
)

/**
 * Activity registration entry
 * 
 * Links a participant to an activity.
 * 
 * @property id Unique identifier
 * @property activityId Activity ID
 * @property participantId Participant ID
 * @property registeredAt Registration timestamp (ISO 8601 UTC)
 * @property notes Optional notes from participant
 */
@Serializable
data class ActivityParticipant(
    val id: String,
    val activityId: String,
    val participantId: String,
    val registeredAt: String,  // ISO 8601 UTC timestamp
    val notes: String? = null
)

/**
 * Activities grouped by date
 * 
 * @property date Date string (ISO 8601 format)
 * @property activities List of activities for this date
 * @property totalActivities Total count
 * @property totalCost Sum of all activity costs in cents
 */
@Serializable
data class ActivitiesByDate(
    val date: String,
    val activities: List<Activity>,
    val totalActivities: Int,
    val totalCost: Long  // In cents
)

/**
 * Activity schedule overview
 * 
 * @property eventId Event ID
 * @property activitiesByDate Activities grouped by date
 * @property totalActivities Total number of activities
 * @property totalCost Total cost of all activities in cents
 * @property participationRate Average participation rate (0.0 to 1.0)
 */
@Serializable
data class ActivitySchedule(
    val eventId: String,
    val activitiesByDate: List<ActivitiesByDate>,
    val totalActivities: Int,
    val totalCost: Long,  // In cents
    val participationRate: Double  // 0.0 to 1.0
)

/**
 * Participant activity statistics
 * 
 * @property participantId Participant ID
 * @property registeredCount Number of activities registered
 * @property totalCost Total cost for this participant in cents
 * @property activityNames List of activity names
 */
@Serializable
data class ParticipantActivityStats(
    val participantId: String,
    val registeredCount: Int,
    val totalCost: Long,  // In cents
    val activityNames: List<String>
)

/**
 * Request to create or update an activity
 * 
 * @property name Activity name
 * @property description Description
 * @property date Date (optional, ISO 8601)
 * @property time Time (optional, HH:MM)
 * @property duration Duration in minutes
 * @property location Location (optional)
 * @property cost Cost per participant in cents (optional)
 * @property maxParticipants Max capacity (optional, null = unlimited)
 * @property organizerId Organizer participant ID
 * @property notes Additional notes (optional)
 * @property scenarioId Link to scenario (optional)
 */
@Serializable
data class ActivityRequest(
    val name: String,
    val description: String,
    val date: String? = null,
    val time: String? = null,
    val duration: Int,
    val location: String? = null,
    val cost: Long? = null,  // In cents
    val maxParticipants: Int? = null,
    val organizerId: String,
    val notes: String? = null,
    val scenarioId: String? = null
)

/**
 * Request to register a participant to an activity
 * 
 * @property participantId Participant ID
 * @property notes Optional notes
 */
@Serializable
data class ActivityRegistrationRequest(
    val participantId: String,
    val notes: String? = null
)
