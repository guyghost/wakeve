package com.guyghost.wakeve.models

import kotlinx.serialization.Serializable

/**
 * Search and discovery models for the Wakeve event exploration feature.
 * Used by both server endpoints and client-side view models.
 */

// MARK: - Search Query

/**
 * Represents a search query with filters for finding events.
 */
@Serializable
data class SearchQuery(
    val query: String? = null,
    val category: String? = null, // EventType enum name
    val location: String? = null,
    val dateFrom: String? = null, // ISO 8601
    val dateTo: String? = null,   // ISO 8601
    val status: String? = null,   // EventStatus enum name
    val sortBy: SearchSortBy = SearchSortBy.RELEVANCE,
    val offset: Int = 0,
    val limit: Int = 20
)

/**
 * Sort options for search results.
 */
@Serializable
enum class SearchSortBy {
    /** Sort by search relevance (title match > description match) */
    RELEVANCE,
    /** Sort by event date (nearest first) */
    DATE,
    /** Sort by popularity (most participants first) */
    POPULARITY,
    /** Sort by creation date (newest first) */
    NEWEST
}

// MARK: - Search Results

/**
 * Paginated search results response.
 */
@Serializable
data class SearchResultsResponse(
    val events: List<EventSearchResult>,
    val totalCount: Int,
    val offset: Int,
    val limit: Int,
    val hasMore: Boolean
)

/**
 * A single event search result with computed fields for display.
 */
@Serializable
data class EventSearchResult(
    val id: String,
    val title: String,
    val description: String,
    val organizerId: String,
    val status: String,
    val eventType: String,
    val eventTypeCustom: String? = null,
    val participantCount: Int,
    val maxParticipants: Int? = null,
    val deadline: String,
    val createdAt: String,
    val locationName: String? = null,
    val locationCoordinates: String? = null // JSON: {"latitude": ..., "longitude": ...}
)

// MARK: - Trending

/**
 * Response for trending events endpoint.
 */
@Serializable
data class TrendingEventsResponse(
    val events: List<EventSearchResult>,
    val period: String // e.g., "7_days"
)

// MARK: - Nearby

/**
 * Response for nearby events endpoint.
 */
@Serializable
data class NearbyEventsResponse(
    val events: List<NearbyEventResult>,
    val centerLat: Double,
    val centerLon: Double,
    val radiusKm: Double
)

/**
 * A nearby event result including distance from search center.
 */
@Serializable
data class NearbyEventResult(
    val event: EventSearchResult,
    val distanceKm: Double
)

// MARK: - Recommended

/**
 * Response for recommended events endpoint.
 */
@Serializable
data class RecommendedEventsResponse(
    val events: List<EventSearchResult>,
    val userId: String,
    val reason: String // e.g., "based_on_past_event_types"
)

// MARK: - Event Category (for UI filter chips)

/**
 * Categories for filtering events in the explore UI.
 * Maps to EventType values but provides a simplified, user-facing grouping.
 */
@Serializable
enum class EventCategory(val displayNameFr: String, val icon: String, val eventTypes: List<String>) {
    ALL("Tout", "square.grid.2x2", listOf()),
    SOCIAL("Social", "person.2.fill", listOf("BIRTHDAY", "WEDDING", "PARTY", "FAMILY_GATHERING")),
    SPORT("Sport", "figure.run", listOf("SPORTS_EVENT", "SPORT_EVENT", "OUTDOOR_ACTIVITY")),
    CULTURE("Culture", "theatermasks.fill", listOf("CULTURAL_EVENT", "CREATIVE_WORKSHOP")),
    PROFESSIONAL("Pro", "briefcase.fill", listOf("TEAM_BUILDING", "CONFERENCE", "WORKSHOP", "TECH_MEETUP")),
    FOOD("Food", "fork.knife", listOf("FOOD_TASTING")),
    WELLNESS("Bien-etre", "leaf.fill", listOf("WELLNESS_EVENT"));

    companion object {
        /**
         * Find the category for a given EventType name.
         */
        fun fromEventType(eventType: String): EventCategory {
            return entries.firstOrNull { it.eventTypes.contains(eventType) } ?: ALL
        }
    }
}
