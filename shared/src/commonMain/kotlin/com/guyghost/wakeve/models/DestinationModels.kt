package com.guyghost.wakeve.models

import kotlinx.serialization.Serializable

/**
 * Suggestion de destination
 */
@Serializable
data class DestinationSuggestion(
    val destination: Destination,
    val score: Double,
    val reasons: List<String>
)

/**
 * Score d'une destination
 */
@Serializable
data class DestinationScore(
    val destination: Destination,
    val overallScore: Double,
    val costScore: Double,
    val accessibilityScore: Double,
    val popularityScore: Double,
    val seasonalityScore: Double,
    val groupSizeScore: Double,
    val reasons: List<String>
) {
    val suggestion: DestinationSuggestion
        get() = DestinationSuggestion(destination, overallScore, reasons)
}

/**
 * Suggestion d'hébergement
 */
@Serializable
data class AccommodationSuggestion(
    val accommodation: ExternalAccommodation,
    val score: Double,
    val reasons: List<String>
)

/**
 * Score d'un hébergement
 */
@Serializable
data class AccommodationScore(
    val accommodation: ExternalAccommodation,
    val overallScore: Double,
    val costScore: Double,
    val ratingScore: Double,
    val amenitiesScore: Double,
    val locationScore: Double,
    val reasons: List<String>
)

/**
 * Destination
 */
@Serializable
data class Destination(
    val id: String,
    val name: String,
    val city: String,
    val country: String,
    val region: String,
    val averageCostPerPerson: Double,
    val popularityScore: Double,
    val nearbyAttractions: Int,
    val imageUrl: String,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Hébergement externe (pour suggestions)
 */
@Serializable
data class ExternalAccommodation(
    val id: String,
    val name: String,
    val type: AccommodationType,
    val location: Location,
    val costPerNight: Double,
    val rating: Double, // 1-5 étoiles
    val amenities: List<String>,
    val maxCapacity: Int,
    val imageUrl: String,
    val provider: String
)

/**
 * Localisation
 */
@Serializable
data class Location(
    val address: String,
    val city: String,
    val country: String,
    val region: String,
    val latitude: Double,
    val longitude: Double,
    val nearbyAttractions: Int
)