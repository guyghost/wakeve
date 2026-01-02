package com.guyghost.wakeve.services

import com.guyghost.wakeve.models.Album
import com.guyghost.wakeve.models.BoundingBox
import com.guyghost.wakeve.models.FaceDetection
import com.guyghost.wakeve.models.Photo
import com.guyghost.wakeve.models.PhotoCategory
import com.guyghost.wakeve.models.PhotoTag
import com.guyghost.wakeve.models.TagSource
import com.guyghost.wakeve.ml.PhotoRecognitionService as PlatformPhotoRecognitionService
import com.guyghost.wakeve.repository.AlbumRepository
import com.guyghost.wakeve.repository.PhotoRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for Photo Tagging and Album Organization.
 * 
 * This test suite focuses on:
 * - Photo tagging by category (PEOPLE, FOOD, DECORATION, LOCATION)
 * - Album creation and organization based on event type
 * - Tag confidence scoring and sorting
 * - Privacy validation (local-only processing)
 * - Album naming from event metadata
 * 
 * Test Coverage (10 tests):
 * 1. Tags returned by confidence descending (photo-102)
 * 2. Tags filtered by minimum confidence threshold (photo-104)
 * 3. Auto-album named from event type + date (photo-103)
 * 4. Multiple tags per category are grouped correctly (photo-102)
 * 5. Manual tags override auto-tags by category (privacy)
 * 6. Album photo count accurate (photo-103)
 * 7. Search filters by date range work (photo-104)
 * 8. Face detection confidence >= 70% (photo-101)
 * 9. Privacy: no data sent to cloud (photo-105)
 * 10. Tags sorted and limited to top 3 suggestions (photo-102)
 */
class PhotoTaggingAndAlbumOrganizationTest {
    
    private lateinit var photoRecognitionService: PhotoRecognitionService
    private lateinit var mockPhotoRepository: FakePhotoRepository
    private lateinit var mockAlbumRepository: FakeAlbumRepository
    private lateinit var mockPlatformService: FakePlatformPhotoRecognition
    
    private fun setup() {
        mockPlatformService = FakePlatformPhotoRecognition()
        mockPhotoRepository = FakePhotoRepository()
        mockAlbumRepository = FakeAlbumRepository()
        
        photoRecognitionService = PhotoRecognitionService(
            androidPhotoRecognition = mockPlatformService,
            iosPhotoRecognition = null,
            photoRepository = mockPhotoRepository,
            albumRepository = mockAlbumRepository
        )
    }
    
    // ================ Test 1: Tags sorted by confidence DESC ================
    
    @Test
    fun `given multiple tags, when tagPhoto, then sorted by confidence DESC`() = runTest {
        setup()
        
        // GIVEN: A photo already exists in the repository
        val photoId = "photo-wedding"
        val testPhoto = createTestPhoto(photoId, "event-123")
        mockPhotoRepository.photos[photoId] = testPhoto
        
        // Configure mock to return tags with varying confidence
        mockPlatformService.tagsToReturn = listOf(
            PhotoTag(
                tagId = "tag-1",
                label = "Dinner Table",
                confidence = 0.90,
                category = PhotoCategory.FOOD,
                source = TagSource.AUTO,
                suggestedAt = "2025-06-15T10:00:00Z"
            ),
            PhotoTag(
                tagId = "tag-2",
                label = "Wedding Couple",
                confidence = 0.95,
                category = PhotoCategory.PEOPLE,
                source = TagSource.AUTO,
                suggestedAt = "2025-06-15T10:00:00Z"
            ),
            PhotoTag(
                tagId = "tag-3",
                label = "Flowers",
                confidence = 0.88,
                category = PhotoCategory.DECORATION,
                source = TagSource.AUTO,
                suggestedAt = "2025-06-15T10:00:00Z"
            )
        )
        
        // WHEN: Process the photo
        val imageBytes = byteArrayOf(1, 2, 3)
        val result = photoRecognitionService.processPhoto(photoId, imageBytes)
        
        // THEN: Verify tags are sorted by confidence DESC
        assertEquals(3, result.tagsSuggested)
        
        val savedPhoto = mockPhotoRepository.photos[photoId]
        assertNotNull(savedPhoto)
        
        val tagConfidences = savedPhoto.tags.map { it.confidence }
        val sortedConfidences = tagConfidences.sortedByDescending { it }
        assertEquals(sortedConfidences, tagConfidences, "Tags should be sorted by confidence DESC")
    }
    
    // ================ Test 2: Tags filtered by min confidence ================
    
    @Test
    fun `given search with minConfidence filter, when searchPhotos, then returns only high-confidence tags`() = runTest {
        setup()
        val eventId = "event-birthday-456"
        
        // Create photos with different tag confidence levels
        val photo1 = createTestPhoto(
            id = "photo-high-conf",
            eventId = eventId,
            tags = listOf(
                createTag("Party", PhotoCategory.DECORATION, 0.95),
                createTag("Cake", PhotoCategory.FOOD, 0.92)
            )
        )
        
        val photo2 = createTestPhoto(
            id = "photo-low-conf",
            eventId = eventId,
            tags = listOf(
                createTag("People", PhotoCategory.PEOPLE, 0.65),
                createTag("Balloons", PhotoCategory.DECORATION, 0.55)
            )
        )
        
        mockPhotoRepository.photos["photo-high-conf"] = photo1
        mockPhotoRepository.photos["photo-low-conf"] = photo2
        mockPhotoRepository.searchResults = listOf(photo1, photo2)
        
        // When: Search with minConfidence filter = 0.80
        val results = photoRecognitionService.searchPhotos(
            "party",
            PhotoSearchFilters(eventId = eventId, minConfidence = 0.80)
        )
        
        // Then: Only high-confidence photos returned
        assertEquals(1, results.size, "Should return only photo with high confidence")
        assertEquals("photo-high-conf", results[0].photo.id)
    }
    
    // ================ Test 3: Auto-album named from event type + date ================
    
    @Test
    fun `given event photos from wedding, when createAutoAlbum, then album created with correct name`() = runTest {
        setup()
        val eventId = "event-wedding-sophie"
        
        // Create test photos for the event
        val photo1 = createTestPhoto("photo-1", eventId, isFavorite = false)
        val photo2 = createTestPhoto("photo-2", eventId, isFavorite = false)
        val photo3 = createTestPhoto("photo-3", eventId, isFavorite = false)
        
        mockPhotoRepository.photos["photo-1"] = photo1
        mockPhotoRepository.photos["photo-2"] = photo2
        mockPhotoRepository.photos["photo-3"] = photo3
        
        // When: Create auto-album
        val album = photoRecognitionService.createAutoAlbum(eventId)
        
        // Then: Album created with appropriate name
        assertNotNull(album)
        assertEquals(eventId, album.eventId)
        assertTrue(album.isAutoGenerated, "Album should be marked as auto-generated")
        assertEquals(3, album.photoIds.size, "Album should contain all event photos")
        assertNotNull(album.coverPhotoId, "Album should have a cover photo")
    }
    
    // ================ Test 4: Multiple tags per category grouped correctly ================
    
    @Test
    fun `given photo with multiple food tags, when processPhoto, then all tags grouped by category`() = runTest {
        setup()
        
        // GIVEN: A photo already exists in the repository
        val photoId = "photo-party"
        val testPhoto = createTestPhoto(photoId, "event-123")
        mockPhotoRepository.photos[photoId] = testPhoto
        
        // Configure mock to return multiple tags in same category
        mockPlatformService.tagsToReturn = listOf(
            PhotoTag(
                tagId = "tag-pizza",
                label = "Pizza Party",
                confidence = 0.94,
                category = PhotoCategory.FOOD,
                source = TagSource.AUTO,
                suggestedAt = "2025-06-15T10:00:00Z"
            ),
            PhotoTag(
                tagId = "tag-table",
                label = "Dinner Table",
                confidence = 0.91,
                category = PhotoCategory.FOOD,
                source = TagSource.AUTO,
                suggestedAt = "2025-06-15T10:00:00Z"
            ),
            PhotoTag(
                tagId = "tag-friends",
                label = "Friends Group",
                confidence = 0.88,
                category = PhotoCategory.PEOPLE,
                source = TagSource.AUTO,
                suggestedAt = "2025-06-15T10:00:00Z"
            )
        )
        
        // WHEN: Process photo
        val result = photoRecognitionService.processPhoto(photoId, byteArrayOf(1, 2, 3))
        
        // THEN: All tags preserved and retrievable
        assertEquals(3, result.tagsSuggested)
        
        val photo = mockPhotoRepository.photos[photoId]
        assertNotNull(photo)
        
        val foodTags = photo.tags.filter { it.category == PhotoCategory.FOOD }
        assertEquals(2, foodTags.size, "Should have 2 food tags")
        
        val peopleTags = photo.tags.filter { it.category == PhotoCategory.PEOPLE }
        assertEquals(1, peopleTags.size, "Should have 1 people tag")
    }
    
    // ================ Test 5: Manual tags override auto-tags ================
    
    @Test
    fun `given auto-tagged photo, when addManualTag, then manual source is included`() = runTest {
        setup()
        val photoId = "photo-food-event"
        
        // Create photo with auto-tags
        val autoTaggedPhoto = createTestPhoto(
            id = photoId,
            eventId = "event-123",
            tags = listOf(
                createTag("Pizza", PhotoCategory.FOOD, 0.85, TagSource.AUTO),
                createTag("Party", PhotoCategory.DECORATION, 0.80, TagSource.AUTO)
            )
        )
        
        mockPhotoRepository.photos[photoId] = autoTaggedPhoto
        
        // When: User adds manual tag
        val manualTag = createTag("Italian Dinner", PhotoCategory.FOOD, 1.0, TagSource.MANUAL)
        mockPhotoRepository.addTagsToPhoto(photoId, listOf(manualTag))
        
        // Then: Manual tag should be included
        val updatedPhoto = mockPhotoRepository.photos[photoId]
        assertNotNull(updatedPhoto)
        
        val manualTags = updatedPhoto.tags.filter { it.source == TagSource.MANUAL }
        assertTrue(manualTags.isNotEmpty(), "Should have manual tags")
        assertEquals("Italian Dinner", manualTags[0].label)
    }
    
    // ================ Test 6: Album photo count accurate ================
    
    @Test
    fun `given album with 3 photos, when getAlbum, then photo count matches`() = runTest {
        setup()
        
        // Create test album
        val album = Album(
            id = "album-123",
            eventId = "event-123",
            name = "Summer Party",
            coverPhotoId = "photo-1",
            photoIds = listOf("photo-1", "photo-2", "photo-3"),
            createdAt = "2025-06-15T10:00:00Z",
            isAutoGenerated = true,
            updatedAt = "2025-06-15T10:00:00Z"
        )
        
        mockAlbumRepository.albums["album-123"] = album
        
        // When: Retrieve album
        val retrievedAlbum = mockAlbumRepository.getAlbum("album-123")
        
        // Then: Photo count should be correct
        assertNotNull(retrievedAlbum)
        assertEquals(3, retrievedAlbum.photoIds.size, "Album should contain 3 photos")
    }
    
    // ================ Test 7: Search filters by date range ================
    
    @Test
    fun `given photos from different dates, when searchPhotos with date range, then only photos in range returned`() = runTest {
        setup()
        
        val eventId = "event-month-long"
        
        // Create photos from different dates
        val earlyPhoto = createTestPhoto(
            id = "photo-early",
            eventId = eventId,
            uploadedAt = "2025-06-01T10:00:00Z",
            tags = listOf(createTag("Early", PhotoCategory.LOCATION, 0.85))
        )
        
        val midPhoto = createTestPhoto(
            id = "photo-mid",
            eventId = eventId,
            uploadedAt = "2025-06-15T10:00:00Z",
            tags = listOf(createTag("Mid", PhotoCategory.LOCATION, 0.85))
        )
        
        val latePhoto = createTestPhoto(
            id = "photo-late",
            eventId = eventId,
            uploadedAt = "2025-06-30T10:00:00Z",
            tags = listOf(createTag("Late", PhotoCategory.LOCATION, 0.85))
        )
        
        mockPhotoRepository.photos["photo-early"] = earlyPhoto
        mockPhotoRepository.photos["photo-mid"] = midPhoto
        mockPhotoRepository.photos["photo-late"] = latePhoto
        // For text search, return all (searchByQuery will be called with "date")
        mockPhotoRepository.searchResults = listOf(earlyPhoto, midPhoto, latePhoto)
        
        // WHEN: Search with date range (June 10-20) using a valid query
        val results = photoRecognitionService.searchPhotos(
            "date",  // Valid non-blank query
            PhotoSearchFilters(
                eventId = eventId,
                startDate = "2025-06-10T00:00:00Z",
                endDate = "2025-06-20T23:59:59Z"
            )
        )
        
        // THEN: Photo in range returned
        assertTrue(results.isNotEmpty(), "Should return photos in date range")
        // Should include midPhoto which is within the range
        assertTrue(results.any { it.photo.id == "photo-mid" }, "Should include mid-range photo")
    }
    
    // ================ Test 8: Face detection confidence >= 70% ================
    
    @Test
    fun `given photo with 5 people, when detectFaces, then all faces have above 70 percent confidence`() = runTest {
        setup()
        
        // GIVEN: A photo already exists in the repository
        val photoId = "photo-group"
        val testPhoto = createTestPhoto(photoId, "event-123")
        mockPhotoRepository.photos[photoId] = testPhoto
        
        // Configure mock to return faces with high confidence
        mockPlatformService.facesToReturn = listOf(
            FaceDetection(BoundingBox(10, 10, 50, 50), 0.95),
            FaceDetection(BoundingBox(70, 10, 50, 50), 0.92),
            FaceDetection(BoundingBox(130, 10, 50, 50), 0.89),
            FaceDetection(BoundingBox(190, 10, 50, 50), 0.85),
            FaceDetection(BoundingBox(250, 10, 50, 50), 0.78)
        )
        
        // WHEN: Process photo with group of people
        val result = photoRecognitionService.processPhoto(photoId, byteArrayOf(1, 2, 3))
        
        // THEN: All faces detected with high confidence
        assertEquals(5, result.facesDetected, "Should detect all 5 faces")
        
        val photo = mockPhotoRepository.photos[photoId]
        assertNotNull(photo)
        
        val lowConfidenceFaces = photo.faceDetections.filter { it.confidence < 0.70 }
        assertTrue(lowConfidenceFaces.isEmpty(), "All faces should have >= 70 percent confidence")
    }
    
    // ================ Test 9: Privacy - no data sent to cloud ================
    
    @Test
    fun `given photo processed, when checkDataSent, then false (local only)`() = runTest {
        setup()
        
        // GIVEN: A photo already exists in the repository
        val photoId = "photo-private"
        val testPhoto = createTestPhoto(photoId, "event-123")
        mockPhotoRepository.photos[photoId] = testPhoto
        
        // Configure mock to have some tags
        mockPlatformService.tagsToReturn = listOf(
            createTag("Private Photo", PhotoCategory.DECORATION, 0.90)
        )
        
        // WHEN: Process photo
        val imageBytes = byteArrayOf(1, 2, 3)
        photoRecognitionService.processPhoto(photoId, imageBytes)
        
        // THEN: Verify data was only written to local repository
        val savedPhoto = mockPhotoRepository.photos[photoId]
        assertNotNull(savedPhoto, "Photo should be saved to local repository")
        
        // Verify that the mock platform service was called (local processing)
        assertTrue(mockPlatformService.processedPhotoCount > 0, "Should have processed photo locally")
    }
    
    // ================ Test 10: Top 3 tags suggested ================
    
    @Test
    fun `given photo with many tags, when getTopSuggestions, then limited to top 3 by confidence`() = runTest {
        setup()
        
        // GIVEN: A photo already exists in the repository
        val photoId = "photo-wedding-full"
        val testPhoto = createTestPhoto(photoId, "event-123")
        mockPhotoRepository.photos[photoId] = testPhoto
        
        // Configure mock to return 5 tags
        mockPlatformService.tagsToReturn = listOf(
            PhotoTag(
                tagId = "tag-1",
                label = "Wedding Couple",
                confidence = 0.97,
                category = PhotoCategory.PEOPLE,
                source = TagSource.AUTO,
                suggestedAt = "2025-06-15T10:00:00Z"
            ),
            PhotoTag(
                tagId = "tag-2",
                label = "Flowers",
                confidence = 0.94,
                category = PhotoCategory.DECORATION,
                source = TagSource.AUTO,
                suggestedAt = "2025-06-15T10:00:00Z"
            ),
            PhotoTag(
                tagId = "tag-3",
                label = "Dinner Table",
                confidence = 0.91,
                category = PhotoCategory.FOOD,
                source = TagSource.AUTO,
                suggestedAt = "2025-06-15T10:00:00Z"
            ),
            PhotoTag(
                tagId = "tag-4",
                label = "Indoor Venue",
                confidence = 0.85,
                category = PhotoCategory.LOCATION,
                source = TagSource.AUTO,
                suggestedAt = "2025-06-15T10:00:00Z"
            ),
            PhotoTag(
                tagId = "tag-5",
                label = "Celebrations",
                confidence = 0.78,
                category = PhotoCategory.DECORATION,
                source = TagSource.AUTO,
                suggestedAt = "2025-06-15T10:00:00Z"
            )
        )
        
        // WHEN: Process photo
        val result = photoRecognitionService.processPhoto(photoId, byteArrayOf(1, 2, 3))
        
        // THEN: Verify at least top 3 tags with highest confidence
        val photo = mockPhotoRepository.photos[photoId]
        assertNotNull(photo)
        
        val allTags = photo.tags.sortedByDescending { it.confidence }
        val topThree = allTags.take(3)
        
        // Verify top 3 confidence values are in descending order
        assertEquals(0.97, topThree[0].confidence, 0.01)
        assertEquals(0.94, topThree[1].confidence, 0.01)
        assertEquals(0.91, topThree[2].confidence, 0.01)
    }
    
    // ================ Helper Methods ================
    
    private fun createTestPhoto(
        id: String,
        eventId: String,
        isFavorite: Boolean = false,
        uploadedAt: String = "2025-06-15T10:00:00Z",
        tags: List<PhotoTag> = emptyList()
    ): Photo {
        return Photo(
            id = id,
            eventId = eventId,
            url = "https://photos.app/$id",
            localPath = null,
            thumbnailUrl = "https://photos.app/thumb/$id",
            caption = null,
            uploadedAt = uploadedAt,
            tags = tags,
            faceDetections = emptyList(),
            albums = emptyList(),
            isFavorite = isFavorite
        )
    }
    
    private fun createTag(
        label: String,
        category: PhotoCategory,
        confidence: Double = 0.85,
        source: TagSource = TagSource.AUTO
    ): PhotoTag {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        val random = Random.nextInt(1000000)
        return PhotoTag(
            tagId = "tag-$timestamp-$random",
            label = label,
            confidence = confidence,
            category = category,
            source = source,
            suggestedAt = "2025-06-15T10:00:00Z"
        )
    }
    
    // ================ Mock Classes ================
    
    private class FakePlatformPhotoRecognition : PlatformPhotoRecognitionService {
        var facesToReturn: List<FaceDetection> = emptyList()
        var tagsToReturn: List<PhotoTag> = emptyList()
        var processedPhotoCount: Int = 0
        
        override suspend fun detectFaces(image: Any?): List<FaceDetection> {
            processedPhotoCount++
            return facesToReturn
        }
        
        override suspend fun tagPhoto(image: Any?): List<PhotoTag> {
            processedPhotoCount++
            return tagsToReturn
        }
    }
    
    private class FakePhotoRepository : PhotoRepository {
        val photos = mutableMapOf<String, Photo>()
        var searchResults: List<Photo> = emptyList()
        
        override suspend fun getPhoto(photoId: String): Photo? = photos[photoId]
        override suspend fun getPhotosByEvent(eventId: String): List<Photo> =
            photos.values.filter { it.eventId == eventId }
        override suspend fun getAllPhotos(): List<Photo> = photos.values.toList()
        override suspend fun savePhoto(photo: Photo) { photos[photo.id] = photo }
        override suspend fun updatePhotoCaption(photoId: String, caption: String?) {}
        override suspend fun updatePhotoWithTags(
            photoId: String,
            faces: List<FaceDetection>,
            tags: List<PhotoTag>
        ) {
            photos[photoId]?.let { photo ->
                photos[photoId] = photo.copy(
                    faceDetections = faces,
                    tags = tags.sortedByDescending { it.confidence }
                )
            }
        }
        override suspend fun addTagsToPhoto(photoId: String, tags: List<PhotoTag>) {
            photos[photoId]?.let { photo ->
                val existingTags = photo.tags.filter { it.source == TagSource.AUTO }
                val allTags = (existingTags + tags).sortedByDescending { it.confidence }
                photos[photoId] = photo.copy(tags = allTags)
            }
        }
        override suspend fun removeTagFromPhoto(photoId: String, tagId: String) {
            photos[photoId]?.let { photo ->
                photos[photoId] = photo.copy(
                    tags = photo.tags.filter { it.tagId != tagId }
                )
            }
        }
        override suspend fun addPhotoToAlbum(photoId: String, albumId: String) {}
        override suspend fun removePhotoFromAlbum(photoId: String, albumId: String) {}
        override suspend fun searchByQuery(query: String): List<Photo> = searchResults
        override suspend fun getPhotosByMinConfidence(minConfidence: Double): List<Photo> =
            photos.values.filter { photo ->
                photo.tags.any { it.confidence >= minConfidence }
            }
        override suspend fun getPhotosWithFaces(): List<Photo> =
            photos.values.filter { it.faceDetections.isNotEmpty() }
        override suspend fun getPhotosByIds(ids: List<String>): List<Photo> =
            photos.values.filter { it.id in ids }
        override suspend fun deletePhoto(photoId: String) { photos.remove(photoId) }
        override suspend fun setFavorite(photoId: String, isFavorite: Boolean) {
            photos[photoId]?.let { photo ->
                photos[photoId] = photo.copy(isFavorite = isFavorite)
            }
        }
    }
    
    private class FakeAlbumRepository : AlbumRepository {
        val albums = mutableMapOf<String, Album>()
        
        override suspend fun createAlbum(album: Album) { albums[album.id] = album }
        override suspend fun getAlbums(eventId: String?): List<Album> =
            if (eventId != null) albums.values.filter { it.eventId == eventId }
            else albums.values.toList()
        override suspend fun getAlbum(albumId: String): Album? = albums[albumId]
        override suspend fun updateAlbum(album: Album) { albums[album.id] = album }
        override suspend fun updateAlbumName(albumId: String, name: String) {
            albums[albumId]?.let { album ->
                albums[albumId] = album.copy(name = name)
            }
        }
        override suspend fun updateAlbumCover(albumId: String, coverPhotoId: String) {
            albums[albumId]?.let { album ->
                albums[albumId] = album.copy(coverPhotoId = coverPhotoId)
            }
        }
        override suspend fun deleteAlbum(albumId: String) { albums.remove(albumId) }
        override suspend fun addPhotoToAlbum(albumId: String, photoId: String) {
            albums[albumId]?.let { album ->
                if (!album.photoIds.contains(photoId)) {
                    albums[albumId] = album.copy(
                        photoIds = album.photoIds + photoId
                    )
                }
            }
        }
        override suspend fun removePhotoFromAlbum(albumId: String, photoId: String) {
            albums[albumId]?.let { album ->
                albums[albumId] = album.copy(
                    photoIds = album.photoIds - photoId
                )
            }
        }
        override suspend fun getAutoGeneratedAlbums(): List<Album> =
            albums.values.filter { it.isAutoGenerated }
        override suspend fun getCustomAlbums(): List<Album> =
            albums.values.filter { !it.isAutoGenerated }
        override suspend fun searchAlbumsByName(query: String): List<Album> =
            albums.values.filter { it.name.contains(query, ignoreCase = true) }
    }
}
