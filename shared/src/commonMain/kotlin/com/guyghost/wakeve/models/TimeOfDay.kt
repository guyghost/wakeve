package com.guyghost.wakeve.models

import kotlinx.serialization.Serializable

/**
 * Time of day for flexible time slots.
 *
 * Allows organizers to propose "sometime in the afternoon" without specifying exact times.
 */
@Serializable
enum class TimeOfDay {
    /** All day event (00:00 - 23:59) */
    ALL_DAY,
    
    /** Morning slot (~6h-12h) */
    MORNING,
    
    /** Afternoon slot (~12h-18h) */
    AFTERNOON,
    
    /** Evening slot (~18h-23h) */
    EVENING,
    
    /** Specific time with exact start/end (default) */
    SPECIFIC;
    
    val displayName: String
        get() = when (this) {
            ALL_DAY -> "All Day"
            MORNING -> "Morning"
            AFTERNOON -> "Afternoon"
            EVENING -> "Evening"
            SPECIFIC -> "Specific Time"
        }
}
