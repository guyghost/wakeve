package com.guyghost.wakeve.models

import kotlinx.serialization.Serializable

/**
 * Type of location for potential event venues.
 */
@Serializable
enum class LocationType {
    /** A city (e.g., "Paris", "New York") */
    CITY,
    
    /** A region or area (e.g., "Provence", "California") */
    REGION,
    
    /** A specific venue with address (e.g., "Château de Versailles") */
    SPECIFIC_VENUE,
    
    /** Online/virtual event (no physical location) */
    ONLINE;
    
    val displayName: String
        get() = when (this) {
            CITY -> "City"
            REGION -> "Region"
            SPECIFIC_VENUE -> "Specific Venue"
            ONLINE -> "Online"
        }
}
