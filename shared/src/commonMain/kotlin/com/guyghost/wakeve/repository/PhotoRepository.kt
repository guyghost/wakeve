package com.guyghost.wakeve.repository

import com.guyghost.wakeve.models.Photo
import com.guyghost.wakeve.models.PhotoTag

/**
 * Repository interface for managing photo data and metadata.
 * Provides methods for CRUD operations, search, and tagging.
 */
interface PhotoRepository {
    
    /**
     * Gets a photo by its ID.
     *
     * @param photoId The photo ID to retrieve
     * @return The photo or null if not found
     */
    suspend fun getPhoto(photoId: String): Photo?
    
    /**
     * Gets all photos for a specific event.
     *
     * @param eventId The event ID
     * @return List of photos sorted by upload date (newest first)
     */
    suspend fun getPhotosByEvent(eventId: String): List<Photo>
    
    /**
     * Gets all photos in the repository.
     *
     * @return All photos
     */
    suspend fun getAllPhotos(): List<Photo>
    
    /**
     * Saves a new photo to the repository.
     *
     * @param photo The photo to save
     */
    suspend fun savePhoto(photo: Photo)
    
    /**
     * Updates a photo's caption.
     *
     * @param photoId The photo ID
     * @param caption The new caption (null to remove)
     */
    suspend fun updatePhotoCaption(photoId: String, caption: String?)
    
    /**
     * Updates a photo with detected faces and tags.
     *
     * @param photoId The photo ID
     * @param faces List of face detections
     * @param tags List of photo tags
     */
    suspend fun updatePhotoWithTags(
        photoId: String,
        faces: List<com.guyghost.wakeve.models.FaceDetection>,
        tags: List<PhotoTag>
    )
    
    /**
     * Adds tags to a photo.
     *
     * @param photoId The photo ID
     * @param tags Tags to add
     */
    suspend fun addTagsToPhoto(photoId: String, tags: List<PhotoTag>)
    
    /**
     * Removes a tag from a photo.
     *
     * @param photoId The photo ID
     * @param tagId The tag ID to remove
     */
    suspend fun removeTagFromPhoto(photoId: String, tagId: String)
    
    /**
     * Adds a photo to an album.
     *
     * @param photoId The photo ID
     * @param albumId The album ID
     */
    suspend fun addPhotoToAlbum(photoId: String, albumId: String)
    
    /**
     * Removes a photo from an album.
     *
     * @param photoId The photo ID
     * @param albumId The album ID
     */
    suspend fun removePhotoFromAlbum(photoId: String, albumId: String)
    
    /**
     * Searches photos by text query (tags and captions).
     *
     * @param query The search query
     * @return List of matching photos
     */
    suspend fun searchByQuery(query: String): List<Photo>
    
    /**
     * Gets photos with a minimum confidence score for auto-tags.
     *
     * @param minConfidence Minimum confidence threshold (0.0 - 1.0)
     * @return List of photos with high-confidence tags
     */
    suspend fun getPhotosByMinConfidence(minConfidence: Double): List<Photo>
    
    /**
     * Gets photos that have face detections.
     *
     * @return List of photos with faces
     */
    suspend fun getPhotosWithFaces(): List<Photo>
    
    /**
     * Gets photos by their IDs.
     *
     * @param ids List of photo IDs to retrieve
     * @return List of photos found (empty if none match)
     */
    suspend fun getPhotosByIds(ids: List<String>): List<Photo>

    /**
     * Deletes a photo from the repository.
     *
     * @param photoId The photo ID to delete
     */
    suspend fun deletePhoto(photoId: String)
    
    /**
     * Marks a photo as favorite.
     *
     * @param photoId The photo ID
     * @param isFavorite Whether the photo is a favorite
     */
    suspend fun setFavorite(photoId: String, isFavorite: Boolean)
}
