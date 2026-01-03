package com.guyghost.wakeve.models

import kotlinx.serialization.Serializable

/**
 * Sorting options for albums.
 * 
 * Provides different ways to sort albums in the UI.
 * Each option has a display name for the user and a value for storage/filtering.
 */
@Serializable
enum class AlbumSorting(
    val displayName: String,
    val value: String
) {
    DATE_ASC("Date (Oldest First)", "date_asc"),
    DATE_DESC("Date (Newest First)", "date_desc"),
    NAME_ASC("Name (A-Z)", "name_asc"),
    NAME_DESC("Name (Z-A)", "name_desc");

    companion object {
        /**
         * Get sorting option from value string.
         * Returns DATE_DESC as default if not found.
         */
        fun fromValue(value: String): AlbumSorting {
            return entries.find { it.value == value } ?: DATE_DESC
        }
    }
}

/**
 * Filter types for albums.
 * 
 * Defines different filter categories for smart album organization.
 */
@Serializable
enum class AlbumFilter(
    val displayName: String,
    val value: String
) {
    ALL("All Albums", "all"),
    RECENT("Recent", "recent"),
    FAVORITES("Favorites", "favorites"),
    TAGS("Tags", "tags"),
    DATE_RANGE("Date Range", "date_range");

    companion object {
        /**
         * Get filter option from value string.
         * Returns ALL as default if not found.
         */
        fun fromValue(value: String): AlbumFilter {
            return entries.find { it.value == value } ?: ALL
        }
    }
}

/**
 * Album filter parameters.
 * 
 * Encapsulates all filtering and sorting criteria for album queries.
 * Immutable data class suitable for use in StateFlow and UI state.
 * 
 * @property filter The type of filter to apply
 * @property sorting The sorting order for results
 * @property startDate Start date for date range filtering (ISO 8601 format)
 * @property endDate End date for date range filtering (ISO 8601 format)
 * @property tags List of tags to filter by (for TAGS filter)
 */
@Serializable
data class AlbumFilterParams(
    val filter: AlbumFilter = AlbumFilter.ALL,
    val sorting: AlbumSorting = AlbumSorting.DATE_DESC,
    val startDate: String? = null,
    val endDate: String? = null,
    val tags: List<String> = emptyList()
) {
    /**
     * Check if this filter requires a date range.
     */
    val requiresDateRange: Boolean
        get() = filter == AlbumFilter.DATE_RANGE

    /**
     * Check if this filter requires tags.
     */
    val requiresTags: Boolean
        get() = filter == AlbumFilter.TAGS

    /**
     * Check if this filter has valid parameters.
     */
    val isValid: Boolean
        get() = when {
            requiresDateRange -> startDate != null && endDate != null
            requiresTags -> tags.isNotEmpty()
            else -> true
        }

    /**
     * Create a copy with updated sorting.
     */
    fun withSorting(newSorting: AlbumSorting): AlbumFilterParams =
        copy(sorting = newSorting)

    /**
     * Create a copy with updated filter.
     */
    fun withFilter(newFilter: AlbumFilter): AlbumFilterParams =
        copy(filter = newFilter)

    /**
     * Create a copy with updated date range.
     */
    fun withDateRange(start: String?, end: String?): AlbumFilterParams =
        copy(startDate = start, endDate = end)

    /**
     * Create a copy with updated tags.
     */
    fun withTags(newTags: List<String>): AlbumFilterParams =
        copy(tags = newTags)

    companion object {
        /**
         * Default filter params (all albums, sorted by newest first).
         */
        val DEFAULT = AlbumFilterParams()
        
        /**
         * Filter for recent albums (last 30 days).
         */
        val RECENT = AlbumFilterParams(
            filter = AlbumFilter.RECENT,
            sorting = AlbumSorting.DATE_DESC
        )
        
        /**
         * Filter for favorite albums.
         */
        val FAVORITES = AlbumFilterParams(
            filter = AlbumFilter.FAVORITES,
            sorting = AlbumSorting.DATE_DESC
        )
    }
}

/**
 * Smart album with AI-powered suggestions.
 * 
 * Extends the basic Album model with additional metadata for
 * intelligent organization and recommendations.
 * 
 * @property id Unique album identifier
 * @property name Display name (e.g., "Mariage de Sophie", "Summer 2025")
 * @property coverUri URI of the cover photo
 * @property photoCount Number of photos in this album
 * @property dateCreated ISO 8601 timestamp of creation
 * @property isFavorite Whether this album is marked as favorite
 * @property tags List of associated tags for organization
 * @property location Optional location name where photos were taken
 * @property suggestedFor List of user IDs this album is suggested for (AI recommendations)
 * @property aiConfidence AI confidence score for this suggestion (0.0 - 1.0)
 * @property smartType Type of smart album (auto-generated, suggested, custom)
 */
@Serializable
data class SmartAlbum(
    val id: String,
    val name: String,
    val coverUri: String,
    val photoCount: Int,
    val dateCreated: String,
    val isFavorite: Boolean = false,
    val tags: List<String> = emptyList(),
    val location: String? = null,
    val suggestedFor: List<String> = emptyList(),
    val aiConfidence: Double = 0.0,
    val smartType: SmartAlbumType = SmartAlbumType.CUSTOM
)

/**
 * Types of smart albums.
 */
@Serializable
enum class SmartAlbumType {
    /** User-created album */
    CUSTOM,
    /** Automatically generated by the system based on date/event */
    AUTO_GENERATED,
    /** AI-suggested album based on content analysis */
    AI_SUGGESTED,
    /** Album created from a shared event */
    EVENT_BASED
}

/**
 * Extension to convert SmartAlbum to basic Album.
 */
fun SmartAlbum.toAlbum(): Album = Album(
    id = id,
    eventId = null,
    name = name,
    coverPhotoId = coverUri,
    photoIds = emptyList(), // SmartAlbum doesn't track individual photo IDs
    createdAt = dateCreated,
    isAutoGenerated = smartType != SmartAlbumType.CUSTOM,
    updatedAt = null
)

/**
 * Extension to check if smart album is AI-suggested.
 */
fun SmartAlbum.isAiSuggested(): Boolean = smartType == SmartAlbumType.AI_SUGGESTED

/**
 * Extension to check if smart album is recent (created within 30 days).
 */
fun SmartAlbum.isRecent(): Boolean {
    val thirtyDaysAgo = kotlinx.datetime.Clock.System.now().toEpochMilliseconds() - (30L * 24 * 60 * 60 * 1000)
    return try {
        val createdAt = kotlinx.datetime.Instant.parse(dateCreated).toEpochMilliseconds()
        createdAt > thirtyDaysAgo
    } catch (e: Exception) {
        false
    }
}
