package com.guyghost.wakeve.activity

import com.guyghost.wakeve.database.WakevDb
import com.guyghost.wakeve.models.ActivitiesByDate
import com.guyghost.wakeve.models.Activity
import com.guyghost.wakeve.models.ActivityParticipant
import com.guyghost.wakeve.models.ActivityWithStats
import kotlinx.datetime.Clock

/**
 * Activity Repository - Manages activity and participant registration persistence.
 * 
 * Responsibilities:
 * - CRUD operations for activities
 * - CRUD operations for activity participants (registrations)
 * - Activity queries and filtering
 * - Statistics and aggregations
 * - Map between SQLDelight entities and Kotlin models
 */
class ActivityRepository(private val db: WakevDb) {
    
    private val activityQueries = db.activityQueries
    private val participantQueries = db.activityParticipantQueries
    
    // ==================== Activity Operations ====================
    
    /**
     * Create a new activity.
     * 
     * @param activity Activity to create
     * @return Created Activity
     */
    fun createActivity(activity: Activity): Activity {
        activityQueries.insertActivity(
            id = activity.id,
            event_id = activity.eventId,
            scenario_id = activity.scenarioId,
            name = activity.name,
            description = activity.description,
            date = activity.date,
            time = activity.time,
            duration = activity.duration.toLong(),
            location = activity.location,
            cost = activity.cost,
            max_participants = activity.maxParticipants?.toLong(),
            organizer_id = activity.organizerId,
            notes = activity.notes,
            created_at = activity.createdAt,
            updated_at = activity.updatedAt
        )
        
        return activity
    }
    
    /**
     * Get activity by ID.
     */
    fun getActivityById(activityId: String): Activity? {
        val activityEntity = activityQueries.selectActivityById(activityId).executeAsOneOrNull()
            ?: return null
        
        val registeredIds = getParticipantIdsByActivity(activityId)
        
        return activityEntity.toModel(registeredIds)
    }
    
    /**
     * Get all activities for an event.
     */
    fun getActivitiesByEventId(eventId: String): List<Activity> {
        return activityQueries.selectActivitiesByEvent(eventId)
            .executeAsList()
            .map { entity ->
                val registeredIds = getParticipantIdsByActivity(entity.id)
                entity.toModel(registeredIds)
            }
    }
    
    /**
     * Get activities by event and date.
     */
    fun getActivitiesByEventAndDate(eventId: String, date: String): List<Activity> {
        return activityQueries.selectActivitiesByEventAndDate(eventId, date)
            .executeAsList()
            .map { entity ->
                val registeredIds = getParticipantIdsByActivity(entity.id)
                entity.toModel(registeredIds)
            }
    }
    
    /**
     * Get activities by scenario.
     */
    fun getActivitiesByScenario(eventId: String, scenarioId: String): List<Activity> {
        return activityQueries.selectActivitiesByScenario(eventId, scenarioId)
            .executeAsList()
            .map { entity ->
                val registeredIds = getParticipantIdsByActivity(entity.id)
                entity.toModel(registeredIds)
            }
    }
    
    /**
     * Get activities by organizer.
     */
    fun getActivitiesByOrganizer(eventId: String, organizerId: String): List<Activity> {
        return activityQueries.selectActivitiesByOrganizer(eventId, organizerId)
            .executeAsList()
            .map { entity ->
                val registeredIds = getParticipantIdsByActivity(entity.id)
                entity.toModel(registeredIds)
            }
    }
    
    /**
     * Get activities without a date set.
     */
    fun getActivitiesWithoutDate(eventId: String): List<Activity> {
        return activityQueries.selectActivitiesWithoutDate(eventId)
            .executeAsList()
            .map { entity ->
                val registeredIds = getParticipantIdsByActivity(entity.id)
                entity.toModel(registeredIds)
            }
    }
    
    /**
     * Update an existing activity.
     * 
     * @param activity Activity with updated fields
     * @return Updated Activity
     */
    fun updateActivity(activity: Activity): Activity {
        val updatedActivity = activity.copy(updatedAt = getCurrentUtcIsoString())
        
        activityQueries.updateActivity(
            scenario_id = updatedActivity.scenarioId,
            name = updatedActivity.name,
            description = updatedActivity.description,
            date = updatedActivity.date,
            time = updatedActivity.time,
            duration = updatedActivity.duration.toLong(),
            location = updatedActivity.location,
            cost = updatedActivity.cost,
            max_participants = updatedActivity.maxParticipants?.toLong(),
            organizer_id = updatedActivity.organizerId,
            notes = updatedActivity.notes,
            updated_at = updatedActivity.updatedAt,
            id = updatedActivity.id
        )
        
        return updatedActivity
    }
    
    /**
     * Update activity date and time.
     */
    fun updateActivityDate(activityId: String, date: String?, time: String?): Activity? {
        activityQueries.updateActivityDate(
            date = date,
            time = time,
            updated_at = getCurrentUtcIsoString(),
            id = activityId
        )
        
        return getActivityById(activityId)
    }
    
    /**
     * Update activity capacity.
     */
    fun updateActivityCapacity(activityId: String, maxParticipants: Int?): Activity? {
        activityQueries.updateActivityCapacity(
            max_participants = maxParticipants?.toLong(),
            updated_at = getCurrentUtcIsoString(),
            id = activityId
        )
        
        return getActivityById(activityId)
    }
    
    /**
     * Delete an activity.
     */
    fun deleteActivity(activityId: String) {
        activityQueries.deleteActivity(activityId)
    }
    
    /**
     * Delete all activities for an event.
     */
    fun deleteActivitiesByEvent(eventId: String) {
        activityQueries.deleteActivitiesByEvent(eventId)
    }
    
    /**
     * Delete all activities for a scenario.
     */
    fun deleteActivitiesByScenario(scenarioId: String) {
        activityQueries.deleteActivitiesByScenario(scenarioId)
    }
    
    // ==================== Participant Registration Operations ====================
    
    /**
     * Register a participant to an activity.
     */
    fun registerParticipant(registration: ActivityParticipant): ActivityParticipant {
        participantQueries.insertActivityParticipant(
            id = registration.id,
            activity_id = registration.activityId,
            participant_id = registration.participantId,
            registered_at = registration.registeredAt,
            notes = registration.notes
        )
        
        return registration
    }
    
    /**
     * Check if participant is already registered.
     */
    fun isParticipantRegistered(activityId: String, participantId: String): Boolean {
        return participantQueries.isParticipantRegistered(activityId, participantId).executeAsOne()
    }
    
    /**
     * Get all participants registered for an activity.
     */
    fun getParticipantsByActivity(activityId: String): List<ActivityParticipant> {
        return participantQueries.selectParticipantsByActivity(activityId)
            .executeAsList()
            .map { it.toModel() }
    }
    
    /**
     * Get participant IDs for an activity.
     */
    fun getParticipantIdsByActivity(activityId: String): List<String> {
        return participantQueries.selectParticipantIdsByActivity(activityId).executeAsList()
    }
    
    /**
     * Get all activities a participant is registered for.
     */
    fun getActivitiesByParticipant(eventId: String, participantId: String): List<Activity> {
        val activityIds = participantQueries.selectActivityIdsByParticipant(participantId).executeAsList()
        
        return activityIds.mapNotNull { activityId ->
            val activity = getActivityById(activityId)
            if (activity?.eventId == eventId) activity else null
        }
    }
    
    /**
     * Unregister a participant from an activity.
     */
    fun unregisterParticipant(activityId: String, participantId: String) {
        participantQueries.deleteActivityParticipantByActivityAndParticipant(activityId, participantId)
    }
    
    /**
     * Delete all registrations for an activity.
     */
    fun deleteParticipantsByActivity(activityId: String) {
        participantQueries.deleteParticipantsByActivity(activityId)
    }
    
    // ==================== Statistics & Aggregations ====================
    
    /**
     * Count activities for an event.
     */
    fun countActivitiesByEvent(eventId: String): Long {
        return activityQueries.countActivitiesByEvent(eventId).executeAsOne()
    }
    
    /**
     * Count activities by event and date.
     */
    fun countActivitiesByEventAndDate(eventId: String, date: String): Long {
        return activityQueries.countActivitiesByEventAndDate(eventId, date).executeAsOne()
    }
    
    /**
     * Sum activity cost by event.
     */
    fun sumActivityCostByEvent(eventId: String): Long {
        return activityQueries.sumActivityCostByEvent(eventId).executeAsOne().toLong()
    }
    
    /**
     * Sum activity cost by date.
     */
    fun sumActivityCostByDate(eventId: String, date: String): Long {
        return activityQueries.sumActivityCostByDate(eventId, date).executeAsOne().toLong()
    }
    
    /**
     * Get activities grouped by date with statistics.
     */
    fun getActivitiesByDateGrouped(eventId: String): List<ActivitiesByDate> {
        return activityQueries.selectActivitiesByDateGrouped(eventId)
            .executeAsList()
            .map { stats ->
                val date = stats.date ?: return@map null
                val activities = getActivitiesByEventAndDate(eventId, date)
                
                ActivitiesByDate(
                    date = date,
                    activities = activities,
                    totalActivities = stats.activityCount.toInt(),
                    totalCost = stats.totalCost.toLong()
                )
            }
            .filterNotNull()
    }
    
    /**
     * Count registered participants for an activity.
     */
    fun countParticipantsByActivity(activityId: String): Long {
        return participantQueries.countParticipantsByActivity(activityId).executeAsOne()
    }
    
    /**
     * Count activities a participant is registered for.
     */
    fun countActivitiesByParticipant(participantId: String): Long {
        return participantQueries.countActivitiesByParticipant(participantId).executeAsOne()
    }
    
    /**
     * Get activity with statistics.
     */
    fun getActivityWithStats(activityId: String): ActivityWithStats? {
        val activity = getActivityById(activityId) ?: return null
        return ActivityManager.calculateActivityStats(activity)
    }
    
    /**
     * Check if an activity exists.
     */
    fun activityExists(activityId: String): Boolean {
        return activityQueries.activityExists(activityId).executeAsOne()
    }
    
    // ==================== Helper Methods ====================
    
    private fun getCurrentUtcIsoString(): String {
        return Clock.System.now().toString()
    }
    
    /**
     * Convert SQL Activity entity to Kotlin model.
     */
    private fun com.guyghost.wakeve.Activity.toModel(registeredParticipantIds: List<String>): Activity {
        return Activity(
            id = this.id,
            eventId = this.event_id,
            scenarioId = this.scenario_id,
            name = this.name,
            description = this.description,
            date = this.date,
            time = this.time,
            duration = this.duration.toInt(),
            location = this.location,
            cost = this.cost,
            maxParticipants = this.max_participants?.toInt(),
            registeredParticipantIds = registeredParticipantIds,
            organizerId = this.organizer_id,
            notes = this.notes,
            createdAt = this.created_at,
            updatedAt = this.updated_at
        )
    }
    
    /**
     * Convert SQL ActivityParticipant entity to Kotlin model.
     */
    private fun com.guyghost.wakeve.Activity_participant.toModel(): ActivityParticipant {
        return ActivityParticipant(
            id = this.id,
            activityId = this.activity_id,
            participantId = this.participant_id,
            registeredAt = this.registered_at,
            notes = this.notes
        )
    }
}
