package com.guyghost.wakeve.models

import kotlinx.serialization.Serializable

@Serializable
data class CreateActivityRequest(
    val name: String,
    val description: String,
    val date: String? = null, // YYYY-MM-DD
    val time: String? = null, // HH:MM
    val durationMinutes: Int,
    val location: String? = null,
    val maxParticipants: Int? = null,
    val costPerPerson: Long? = null, // In cents
    val organizerId: String
) {
    fun toActivity(eventId: String): Activity {
        // Basic mapping, ID will be handled by logic or repo
        return Activity(
            id = java.util.UUID.randomUUID().toString(),
            eventId = eventId,
            name = name,
            description = description,
            date = date,
            time = time,
            duration = durationMinutes,
            location = location,
            maxParticipants = maxParticipants,
            cost = costPerPerson,
            registeredParticipantIds = emptyList(), // Initially empty
            organizerId = organizerId,
            createdAt = java.time.Instant.now().toString(),
            updatedAt = java.time.Instant.now().toString()
        )
    }
}

@Serializable
data class RegisterActivityRequest(
    val participantId: String
)

@Serializable
data class UpdateActivityRequest(
    val name: String? = null,
    val description: String? = null,
    val date: String? = null,
    val time: String? = null,
    val durationMinutes: Int? = null,
    val location: String? = null,
    val maxParticipants: Int? = null,
    val costPerPerson: Long? = null,
    val organizerId: String? = null
) {
    fun applyTo(activityId: String, eventId: String): Activity {
        // Dummy implementation to satisfy compilation
        return Activity(
            id = activityId,
            eventId = eventId,
            name = name ?: "",
            description = description ?: "",
            date = date,
            time = time,
            duration = durationMinutes ?: 60,
            location = location,
            maxParticipants = maxParticipants,
            cost = costPerPerson,
            registeredParticipantIds = emptyList(), // Logic needs to preserve this
            organizerId = organizerId ?: "unknown",
            createdAt = "",
            updatedAt = java.time.Instant.now().toString()
        )
    }
}
