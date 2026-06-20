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
    fun applyTo(existing: Activity): Activity {
        return existing.copy(
            name = name ?: existing.name,
            description = description ?: existing.description,
            date = date ?: existing.date,
            time = time ?: existing.time,
            duration = durationMinutes ?: existing.duration,
            location = location ?: existing.location,
            maxParticipants = maxParticipants ?: existing.maxParticipants,
            cost = costPerPerson ?: existing.cost,
            organizerId = organizerId ?: existing.organizerId,
            updatedAt = java.time.Instant.now().toString()
        )
    }
}
