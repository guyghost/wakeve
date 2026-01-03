package com.guyghost.wakeve.repository

import com.guyghost.wakeve.models.*

/**
 * Repository for managing photo albums.
 * Supports offline-first access with local caching.
 * 
 * Provides comprehensive CRUD operations plus advanced filtering
 * and sorting capabilities for smart album organization.
 */
interface AlbumRepository {
    
    // ==================== Basic CRUD Operations ====================
    
    /**
     * Creates a new album.
     *
     * @param album The album to create
     */
    suspend fun createAlbum(album: Album)
    
    /**
     * Gets all albums for a specific event.
     *
     * @param eventId The event ID (null for all albums)
     * @return List of albums sorted by creation date (newest first)
     */
    suspend fun getAlbums(eventId: String?): List<Album>
    
    /**
     * Gets an album by its ID (simple version).
     *
     * @param albumId The album ID
     * @return The album or null if not found
     */
    suspend fun getAlbumSimple(albumId: String): Album?
    
    /**
     * Updates an album.
     *
     * @param album The album with updated values
     */
    suspend fun updateAlbum(album: Album)
    
    /**
     * Updates an album's name.
     *
     * @param albumId The album ID
     * @param name The new name
     */
    suspend fun updateAlbumName(albumId: String, name: String)
    
    /**
     * Updates an album's cover photo.
     *
     * @param albumId The album ID
     * @param coverPhotoId The new cover photo ID
     */
    suspend fun updateAlbumCover(albumId: String, coverPhotoId: String)
    
    /**
     * Deletes an album (simple version).
     *
     * @param albumId The album ID to delete
     */
    suspend fun deleteAlbumSimple(albumId: String)
    
    /**
     * Adds a photo to an album.
     *
     * @param albumId The album ID
     * @param photoId The photo ID to add
     */
    suspend fun addPhotoToAlbum(albumId: String, photoId: String)
    
    /**
     * Removes a photo from an album.
     *
     * @param albumId The album ID
     * @param photoId The photo ID to remove
     */
    suspend fun removePhotoFromAlbum(albumId: String, photoId: String)
    
    // ==================== Smart Album Operations ====================
    
    /**
     * Gets all albums with filtering and sorting.
     * 
     * This is the main method for smart album queries, supporting
     * all filter types and sorting options.
     *
     * @param params Filter and sort parameters
     * @return Result containing list of albums or error
     */
    suspend fun getAlbums(params: AlbumFilterParams): Result<List<Album>>
    
    /**
     * Gets album by ID with full details.
     *
     * @param albumId The album ID
     * @return Result containing album or error
     */
    suspend fun getAlbum(albumId: String): Result<Album?>
    
    /**
     * Create a new album with smart features.
     *
     * @param name The album name
     * @param coverUri Optional cover photo URI
     * @param tags Optional list of tags
     * @param isFavorite Whether to mark as favorite
     * @return Result containing created album or error
     */
    suspend fun createSmartAlbum(
        name: String,
        coverUri: String? = null,
        tags: List<String> = emptyList(),
        isFavorite: Boolean = false
    ): Result<Album>
    
    /**
     * Update album details.
     *
     * @param albumId The album ID
     * @param updates Parameters to update
     * @return Result containing updated album or error
     */
    suspend fun updateAlbum(albumId: String, updates: AlbumUpdateParams): Result<Album>
    
    /**
     * Delete an album.
     *
     * @param albumId The album ID
     * @return Result indicating success or failure
     */
    suspend fun deleteAlbum(albumId: String): Result<Unit>
    
    /**
     * Toggle favorite status for an album.
     *
     * @param albumId The album ID
     * @return Result containing updated album or error
     */
    suspend fun toggleFavorite(albumId: String): Result<Album>
    
    /**
     * Add photos to an album.
     *
     * @param albumId The album ID
     * @param photoIds List of photo IDs to add
     * @return Result containing updated album or error
     */
    suspend fun addPhotosToAlbum(albumId: String, photoIds: List<String>): Result<Album>
    
    /**
     * Remove photos from an album.
     *
     * @param albumId The album ID
     * @param photoIds List of photo IDs to remove
     * @return Result containing updated album or error
     */
    suspend fun removePhotosFromAlbum(albumId: String, photoIds: List<String>): Result<Album>
    
    /**
     * Add tags to an album.
     *
     * @param albumId The album ID
     * @param tags List of tags to add
     * @return Result containing updated album or error
     */
    suspend fun addTagsToAlbum(albumId: String, tags: List<String>): Result<Album>
    
    /**
     * Remove tags from an album.
     *
     * @param albumId The album ID
     * @param tags List of tags to remove
     * @return Result containing updated album or error
     */
    suspend fun removeTagsFromAlbum(albumId: String, tags: List<String>): Result<Album>
    
    /**
     * Get all auto-generated albums.
     *
     * @return List of auto-generated albums sorted by date
     */
    suspend fun getAutoGeneratedAlbums(): Result<List<Album>>
    
    /**
     * Get all custom (user-created) albums.
     *
     * @return List of custom albums sorted by date
     */
    suspend fun getCustomAlbums(): Result<List<Album>>
    
    /**
     * Get favorite albums.
     *
     * @return List of favorite albums sorted by date
     */
    suspend fun getFavoriteAlbums(): Result<List<Album>>
    
    /**
     * Get recent albums (created within specified days).
     *
     * @param days Number of days to look back (default 30)
     * @return List of recent albums sorted by date
     */
    suspend fun getRecentAlbums(days: Int = 30): Result<List<Album>>
    
    /**
     * Search albums by name.
     *
     * @param query The search query
     * @return List of matching albums sorted by relevance
     */
    suspend fun searchAlbums(query: String): Result<List<Album>>
    
    /**
     * Get albums by tags.
     *
     * @param tags List of tags to filter by
     * @param matchAll If true, albums must have all tags; if false, any tag
     * @return List of matching albums sorted by date
     */
    suspend fun getAlbumsByTags(
        tags: List<String>,
        matchAll: Boolean = false
    ): Result<List<Album>>
    
    /**
     * Get albums within a date range.
     *
     * @param startDate Start date (ISO 8601)
     * @param endDate End date (ISO 8601)
     * @return List of albums within the range sorted by date
     */
    suspend fun getAlbumsByDateRange(
        startDate: String,
        endDate: String
    ): Result<List<Album>>
    
    /**
     * Get albums containing specific photos.
     *
     * @param photoIds List of photo IDs
     * @return List of albums that contain any of the photos
     */
    suspend fun getAlbumsContainingPhotos(photoIds: List<String>): Result<List<Album>>
}

/**
 * Album update parameters.
 * 
 * Immutable data class for partial updates to an album.
 * Only non-null values will be applied.
 *
 * @property name New name (null to keep existing)
 * @property coverUri New cover URI (null to keep existing)
 * @property tags New tags list (null to keep existing)
 * @property isFavorite New favorite status (null to keep existing)
 */
@kotlinx.serialization.Serializable
data class AlbumUpdateParams(
    val name: String? = null,
    val coverUri: String? = null,
    val tags: List<String>? = null,
    val isFavorite: Boolean? = null
) {
    /**
     * Check if this update has any parameters set.
     */
    val hasUpdates: Boolean
        get() = name != null || coverUri != null || tags != null || isFavorite != null
    
    /**
     * Apply these updates to an album.
     */
    fun applyTo(album: Album): Album {
        return album.copy(
            name = name ?: album.name,
            coverPhotoId = coverUri ?: album.coverPhotoId,
            updatedAt = if (hasUpdates) kotlinx.datetime.Clock.System.now().toString() else album.updatedAt
        )
    }
    
    companion object {
        /**
         * Update for renaming an album.
         */
        fun rename(name: String): AlbumUpdateParams = AlbumUpdateParams(name = name)
        
        /**
         * Update for changing cover photo.
         */
        fun changeCover(coverUri: String): AlbumUpdateParams = AlbumUpdateParams(coverUri = coverUri)
        
        /**
         * Update for adding/removing favorite status.
         */
        fun setFavorite(isFavorite: Boolean): AlbumUpdateParams = AlbumUpdateParams(isFavorite = isFavorite)
        
        /**
         * Update for replacing tags.
         */
        fun updateTags(tags: List<String>): AlbumUpdateParams = AlbumUpdateParams(tags = tags)
    }
}
