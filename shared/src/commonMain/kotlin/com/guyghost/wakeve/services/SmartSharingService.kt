package com.guyghost.wakeve.services

import com.guyghost.wakeve.EventRepositoryInterface
import com.guyghost.wakeve.models.Album
import com.guyghost.wakeve.models.PhotoCategory
import com.guyghost.wakeve.repository.AlbumRepository
import com.guyghost.wakeve.repository.PhotoRepository

/**
 * Service providing intelligent sharing suggestions based on photo content,
 * event context, and user preferences.
 *
 * This service analyzes photos and generates shareable groupings with
 * contextual suggestions for sharing with friends, family, or groups.
 *
 * ## Features
 *
 * - **Event-based grouping**: Groups photos by event for easy sharing
 * - **Tag-based categorization**: Organizes photos by content category
 * - **Smart suggestions**: Recommends who to share with based on detected people
 * - **Quick share actions**: Provides shareable links for various platforms
 *
 * ## Usage
 *
 * ```kotlin
 * val smartSharingService = SmartSharingService(
 *     photoRepository = photoRepository,
 *     eventRepository = eventRepository,
 *     albumRepository = albumRepository
 * )
 *
 * // Get sharing suggestions for a list of photos
 * val suggestions = smartSharingService.getSharingSuggestions(
 *     photoIds = listOf("photo-1", "photo-2", "photo-3")
 * )
 *
 * // Share photos from a specific album
 * val albumSuggestions = smartSharingService.getAlbumSharingSuggestions(
 *     albumId = "album-123"
 * )
 * ```
 *
 * @property photoRepository Repository for accessing photo data and tags
 * @property eventRepository Repository for accessing event information
 * @property albumRepository Repository for accessing album information
 */
class SmartSharingService(
    private val photoRepository: PhotoRepository,
    private val eventRepository: EventRepositoryInterface,
    private val albumRepository: AlbumRepository
) {

    /**
     * Generates sharing suggestions for a list of photos.
     *
     * This method analyzes the provided photos and creates intelligent
     * groupings based on events, tags, and detected content. Each suggestion
     * includes metadata about what to share and how.
     *
     * @param photoIds List of photo IDs to analyze for sharing
     * @return List of [SharingSuggestion] sorted by priority (highest first)
     */
    suspend fun getSharingSuggestions(photoIds: List<String>): List<SharingSuggestion> {
        if (photoIds.isEmpty()) {
            return emptyList()
        }

        val photos = photoRepository.getPhotosByIds(photoIds)
        if (photos.isEmpty()) {
            return emptyList()
        }

        val suggestions = mutableListOf<SharingSuggestion>()

        // Analyze tags
        val allTags = photos.flatMap { it.tags }.map { it.label }
        val uniqueTags = allTags.distinct()

        // Group by event
        val eventsByPhoto = photos.associate { it.id to it.eventId }
        val photoCountByEvent = eventsByPhoto.values.groupingBy { it }
            .eachCount()
            .filter { it.value >= MIN_PHOTOS_PER_GROUP }

        // Suggest sharing by event
        photoCountByEvent.forEach { (eventId, count) ->
            val event = eventRepository.getEvent(eventId)
            val eventTitle = event?.title ?: "Événement"
            val participantCount = event?.participants?.size ?: 0

            suggestions.add(
                SharingSuggestion(
                    type = SharingType.SHARE_BY_EVENT,
                    title = eventTitle,
                    description = "$count photos de l'événement",
                    photoIds = photos.filter { it.eventId == eventId }.map { it.id },
                    targetUris = listOf("wakeve://event/$eventId"),
                    icon = getEventIcon(eventTitle),
                    priority = calculateEventPriority(count, eventTitle)
                )
            )
        }

        // Suggest sharing by tag category
        val tagCategories = uniqueTags.mapNotNull { tag ->
            getTagCategory(tag)
        }.distinct()

        tagCategories.forEach { category ->
            val matchingPhotos = photos.filter { photo ->
                photo.tags.any { getTagCategory(it.label) == category }
            }

            if (matchingPhotos.isNotEmpty()) {
                suggestions.add(
                    SharingSuggestion(
                        type = SharingType.SHARE_BY_TAG,
                        title = getCategoryTitle(category),
                        description = "${matchingPhotos.size} photos",
                        photoIds = matchingPhotos.map { it.id },
                        targetUris = emptyList(),
                        icon = getIconForCategory(category),
                        priority = matchingPhotos.size
                    )
                )
            }
        }

        // Suggest sharing with detected people
        val peopleTags = photos.flatMap { photo ->
            photo.tags.filter { getTagCategory(it.label) == PhotoCategory.PEOPLE }
        }

        if (peopleTags.isNotEmpty()) {
            val uniquePeople = peopleTags.map { it.label }.distinct()
            suggestions.add(
                SharingSuggestion(
                    type = SharingType.SHARE_WITH_PEOPLE,
                    title = "Photos avec des personnes",
                    description = "${uniquePeople.size} personne(s) détectée(s)",
                    photoIds = photos.map { it.id },
                    targetUris = emptyList(),
                    icon = "👨‍👩‍👧‍👦",
                    priority = uniquePeople.size * 2
                )
            )
        }

        return suggestions.sortedByDescending { it.priority }
    }

    /**
     * Generates sharing suggestions for all photos in an album.
     *
     * @param albumId The album ID to get suggestions for
     * @return List of [SharingSuggestion] for the album
     */
    suspend fun getAlbumSharingSuggestions(albumId: String): List<SharingSuggestion> {
        val album = albumRepository.getAlbum(albumId) ?: return emptyList()
        return getSharingSuggestions(album.photoIds)
    }

    /**
     * Suggests albums that could be shared with specific contacts.
     *
     * This method analyzes albums and suggests which ones might be
     * appropriate to share with a given contact based on event participation.
     *
     * @param contactId The contact ID to suggest albums for
     * @return List of [AlbumSharingOption] with sharing suggestions
     */
    suspend fun suggestAlbumsForContact(contactId: String): List<AlbumSharingOption> {
        val albums = albumRepository.getAlbums(eventId = null)
        val options = mutableListOf<AlbumSharingOption>()

        albums.forEach { album ->
            val firstPhoto = album.photoIds.firstOrNull()?.let { photoRepository.getPhoto(it) }
            val event = firstPhoto?.eventId?.let { eventRepository.getEvent(it) }

            // Suggest if contact participated in the event (contactId is in participants list)
            if (event != null && event.participants.contains(contactId)) {
                options.add(
                    AlbumSharingOption(
                        album = album,
                        reason = "Vous avez participé à ${event.title}",
                        shareUrl = "wakeve://share/album/${album.id}?contact=$contactId",
                        priority = album.photoIds.size
                    )
                )
            }
        }

        return options.sortedByDescending { it.priority }
    }

    /**
     * Creates shareable links for a set of photos.
     *
     * @param photoIds List of photo IDs to create share links for
     * @param platform The target platform for sharing
     * @return List of [ShareableLink] with platform-specific share URLs
     */
    suspend fun createShareableLinks(
        photoIds: List<String>,
        platform: SharePlatform
    ): List<ShareableLink> {
        val photos = photoRepository.getPhotosByIds(photoIds)
        if (photos.isEmpty()) {
            return emptyList()
        }

        val baseUrl = "https://wakeve.app/share"
        val photoQuery = photoIds.joinToString(",")

        return when (platform) {
            SharePlatform.WHATSAPP -> listOf(
                ShareableLink(
                    platform = platform,
                    url = "https://wa.me/?text=${photos.size}%20photos%20Wakeve%3A%20$baseUrl%2Fphotos%3Fids%3D$photoQuery",
                    label = "Partager sur WhatsApp"
                )
            )
            SharePlatform.MESSAGES -> listOf(
                ShareableLink(
                    platform = platform,
                    url = "sms:?body=${photos.size}%20photos%20Wakeve%3A%20$baseUrl%2Fphotos%3Fids%3D$photoQuery",
                    label = "Partager par SMS"
                )
            )
            SharePlatform.EMAIL -> listOf(
                ShareableLink(
                    platform = platform,
                    url = "mailto:?subject=${photos.size}%20photos%20Wakeve&body=Voici%20$baseUrl%2Fphotos%3Fids%3D$photoQuery",
                    label = "Partager par Email"
                )
            )
            SharePlatform.LINK -> listOf(
                ShareableLink(
                    platform = platform,
                    url = "$baseUrl/photos?ids=$photoQuery",
                    label = "Copier le lien"
                )
            )
        }
    }

    /**
     * Gets the category for a given tag label.
     *
     * @param tag The tag label to categorize
     * @return The [PhotoCategory] for the tag, or null if unknown
     */
    private fun getTagCategory(tag: String): PhotoCategory? {
        return when {
            tag.lowercase() in PEOPLE_TAGS -> PhotoCategory.PEOPLE
            tag.lowercase() in FOOD_TAGS -> PhotoCategory.FOOD
            tag.lowercase() in DECORATION_TAGS -> PhotoCategory.DECORATION
            tag.lowercase() in LOCATION_TAGS -> PhotoCategory.LOCATION
            else -> null
        }
    }

    /**
     * Calculates priority for event-based suggestions.
     *
     * @param photoCount Number of photos in the event
     * @param eventTitle Optional event title for priority boost
     * @return Priority score
     */
    private fun calculateEventPriority(photoCount: Int, eventTitle: String?): Int {
        var priority = photoCount
        // Boost priority for special events
        if (eventTitle != null && EVENT_KEYWORDS.any { eventTitle.contains(it, ignoreCase = true) }) {
            priority += 10
        }
        return priority
    }

    /**
     * Gets an emoji icon based on event title.
     */
    private fun getEventIcon(eventTitle: String?): String {
        if (eventTitle == null) return "📁"
        return when {
            eventTitle.contains("mariage", ignoreCase = true) -> "💒"
            eventTitle.contains("anniversaire", ignoreCase = true) -> "🎂"
            eventTitle.contains("team building", ignoreCase = true) -> "👥"
            eventTitle.contains("soirée", ignoreCase = true) -> "🎉"
            eventTitle.contains("vacances", ignoreCase = true) -> "✈️"
            else -> "📁"
        }
    }

    /**
     * Gets the display title for a photo category.
     */
    private fun getCategoryTitle(category: PhotoCategory): String {
        return when (category) {
            PhotoCategory.PEOPLE -> "Photos de personnes"
            PhotoCategory.FOOD -> "Photos de nourriture"
            PhotoCategory.DECORATION -> "Photos de décoration"
            PhotoCategory.LOCATION -> "Photos de lieux"
        }
    }

    /**
     * Gets an emoji icon for a photo category.
     */
    private fun getIconForCategory(category: PhotoCategory): String {
        return when (category) {
            PhotoCategory.PEOPLE -> "👨‍👩‍👧‍👦"
            PhotoCategory.FOOD -> "🍕"
            PhotoCategory.DECORATION -> "🎈"
            PhotoCategory.LOCATION -> "📍"
        }
    }

    companion object {
        /** Minimum photos required to create a sharing suggestion */
        private const val MIN_PHOTOS_PER_GROUP = 3

        /** Keywords used to boost priority for special events */
        private val EVENT_KEYWORDS = listOf(
            "mariage", "wedding", "anniversaire", "birthday",
            "team building", "conférence", "conference"
        )

        /** Tags associated with people */
        private val PEOPLE_TAGS = setOf(
            "person", "people", "face", "portrait", "selfie",
            "group", "crowd", "family", "friends"
        )

        /** Tags associated with food */
        private val FOOD_TAGS = setOf(
            "food", "meal", "dish", "restaurant", "cooking",
            "breakfast", "lunch", "dinner", "snack", "drink"
        )

        /** Tags associated with decoration */
        private val DECORATION_TAGS = setOf(
            "decoration", "decor", "flowers", "balloons",
            "cake", "candles", "lights", "signs"
        )

        /** Tags associated with locations */
        private val LOCATION_TAGS = setOf(
            "beach", "mountain", "city", "building", "park",
            "restaurant", "hotel", "venue", "outdoor", "indoor"
        )
    }
}

/**
 * Represents a type of sharing operation.
 */
enum class SharingType {
    /** Share photos grouped by event */
    SHARE_BY_EVENT,
    /** Share photos grouped by tag category */
    SHARE_BY_TAG,
    /** Share photos with detected people */
    SHARE_WITH_PEOPLE,
    /** Share all selected photos together */
    SHARE_ALL
}

/**
 * Represents a suggestion for sharing photos.
 *
 * @property type The type of sharing suggestion
 * @property title Display title for the suggestion
 * @property description Brief description of what will be shared
 * @property photoIds List of photo IDs included in this suggestion
 * @property targetUris URIs for navigation after sharing
 * @property icon Emoji icon representing the suggestion
 * @property priority Priority score (higher = more important)
 */
data class SharingSuggestion(
    val type: SharingType,
    val title: String,
    val description: String,
    val photoIds: List<String>,
    val targetUris: List<String>,
    val icon: String,
    val priority: Int = 0
)

/**
 * Represents an album that can be shared with a contact.
 *
 * @property album The album to share
 * @property reason Explanation of why this album should be shared
 * @property shareUrl The shareable URL for this album
 * @property priority Priority score based on photo count
 */
data class AlbumSharingOption(
    val album: Album,
    val reason: String,
    val shareUrl: String,
    val priority: Int
)

/**
 * Represents a platform for sharing photos.
 */
enum class SharePlatform {
    /** WhatsApp sharing */
    WHATSAPP,
    /** Native Messages/SMS sharing */
    MESSAGES,
    /** Email sharing */
    EMAIL,
    /** Generic link sharing */
    LINK
}

/**
 * Represents a shareable link for a specific platform.
 *
 * @property platform The target platform
 * @property url The shareable URL
 * @property label Display label for the link
 */
data class ShareableLink(
    val platform: SharePlatform,
    val url: String,
    val label: String
)
