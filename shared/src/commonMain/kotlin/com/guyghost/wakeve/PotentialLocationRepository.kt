package com.guyghost.wakeve

import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.PotentialLocation

/**
 * Repository interface for managing potential locations.
 */
interface PotentialLocationRepositoryInterface {
    /**
     * Add a potential location to an event.
     * Only allowed in DRAFT status.
     */
    suspend fun addLocation(eventId: String, location: PotentialLocation): Result<PotentialLocation>
    
    /**
     * Remove a potential location from an event.
     * Only allowed in DRAFT status.
     */
    suspend fun removeLocation(eventId: String, locationId: String): Result<Boolean>
    
    /**
     * Get all potential locations for an event.
     */
    fun getLocationsByEventId(eventId: String): List<PotentialLocation>
    
    /**
     * Get a specific potential location by ID.
     */
    fun getLocationById(id: String): PotentialLocation?
    
    /**
     * Remove all potential locations for an event (used when deleting event).
     */
    suspend fun removeAllLocationsForEvent(eventId: String): Result<Boolean>
}

/**
 * In-memory implementation of PotentialLocationRepository.
 * 
 * This will be replaced with DatabasePotentialLocationRepository using SQLDelight
 * in a future implementation phase.
 */
class PotentialLocationRepository(
    private val eventRepository: EventRepositoryInterface
) : PotentialLocationRepositoryInterface {
    
    // In-memory storage: eventId -> list of locations
    private val locationsByEvent = mutableMapOf<String, MutableList<PotentialLocation>>()
    
    override suspend fun addLocation(
        eventId: String,
        location: PotentialLocation
    ): Result<PotentialLocation> {
        return try {
            // Check that event exists
            val event = eventRepository.getEvent(eventId)
                ?: return Result.failure(IllegalArgumentException("Event not found"))
            
            // Check that event is in DRAFT status
            if (event.status != EventStatus.DRAFT) {
                return Result.failure(
                    IllegalStateException("Potential locations can only be modified in DRAFT status")
                )
            }
            
            // Check that location eventId matches
            if (location.eventId != eventId) {
                return Result.failure(
                    IllegalArgumentException("Location eventId does not match provided eventId")
                )
            }
            
            // Add location
            val locations = locationsByEvent.getOrPut(eventId) { mutableListOf() }
            
            // Check for duplicate ID
            if (locations.any { it.id == location.id }) {
                return Result.failure(IllegalArgumentException("Location with this ID already exists"))
            }
            
            locations.add(location)
            Result.success(location)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun removeLocation(eventId: String, locationId: String): Result<Boolean> {
        return try {
            // Check that event exists
            val event = eventRepository.getEvent(eventId)
                ?: return Result.failure(IllegalArgumentException("Event not found"))
            
            // Check that event is in DRAFT status
            if (event.status != EventStatus.DRAFT) {
                return Result.failure(
                    IllegalStateException("Potential locations can only be modified in DRAFT status")
                )
            }
            
            // Remove location
            val locations = locationsByEvent[eventId]
                ?: return Result.failure(IllegalArgumentException("No locations found for this event"))
            
            val locationToRemove = locations.find { it.id == locationId }
            
            if (locationToRemove == null) {
                return Result.failure(IllegalArgumentException("Location not found"))
            }
            
            locations.remove(locationToRemove)
            
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun getLocationsByEventId(eventId: String): List<PotentialLocation> {
        return locationsByEvent[eventId]?.toList() ?: emptyList()
    }
    
    override fun getLocationById(id: String): PotentialLocation? {
        return locationsByEvent.values
            .flatten()
            .firstOrNull { it.id == id }
    }
    
    override suspend fun removeAllLocationsForEvent(eventId: String): Result<Boolean> {
        return try {
            locationsByEvent.remove(eventId)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
