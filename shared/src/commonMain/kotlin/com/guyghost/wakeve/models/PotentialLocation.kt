package com.guyghost.wakeve.models

import kotlinx.serialization.Serializable

/**
 * Geographic coordinates for a location.
 */
@Serializable
data class Coordinates(
    val latitude: Double,
    val longitude: Double
) {
    init {
        require(latitude in -90.0..90.0) { "Latitude must be between -90 and 90" }
        require(longitude in -180.0..180.0) { "Longitude must be between -180 and 180" }
    }
    
    /**
     * JSON representation for storage in SQLDelight.
     */
    fun toJson(): String = """{"latitude":$latitude,"longitude":$longitude}"""
    
    companion object {
        /**
         * Parse coordinates from JSON string.
         */
        fun fromJson(json: String): Coordinates? {
            return try {
                val latMatch = Regex(""""latitude":\s*(-?\d+\.?\d*)""").find(json)
                val lngMatch = Regex(""""longitude":\s*(-?\d+\.?\d*)""").find(json)
                
                if (latMatch != null && lngMatch != null) {
                    val lat = latMatch.groupValues[1].toDouble()
                    val lng = lngMatch.groupValues[1].toDouble()
                    Coordinates(lat, lng)
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * Potential location for an event (proposed in DRAFT phase).
 *
 * Used for brainstorming destinations before detailed scenario planning.
 */
@Serializable
data class PotentialLocation(
    val id: String,
    val eventId: String,
    val name: String,
    val locationType: LocationType,
    val address: String? = null,
    val coordinates: Coordinates? = null,
    val createdAt: String // ISO 8601 timestamp
)
