package com.guyghost.wakeve.services

import com.guyghost.wakeve.models.Album
import com.guyghost.wakeve.models.AlbumFilterParams
import com.guyghost.wakeve.models.FaceDetection
import com.guyghost.wakeve.models.Photo
import com.guyghost.wakeve.models.PhotoTag
import com.guyghost.wakeve.repository.AlbumRepository
import com.guyghost.wakeve.repository.AlbumUpdateParams
import com.guyghost.wakeve.repository.PhotoRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PhotoRecognitionServiceContractTest {
    @Test
    fun `processPhoto fails before writing empty recognition data when no platform service is configured`() = runTest {
        val photoRepository = RecordingPhotoRepository()
        val service = PhotoRecognitionService(
            androidPhotoRecognition = null,
            iosPhotoRecognition = null,
            photoRepository = photoRepository,
            albumRepository = FailingAlbumRepository()
        )

        val error = assertFailsWith<IllegalStateException> {
            service.processPhoto(
                photoId = "photo-1",
                imageBytes = byteArrayOf(1, 2, 3)
            )
        }

        assertEquals("Photo recognition service is not configured", error.message)
        assertEquals(0, photoRepository.updateWithTagsCalls)
    }

    private class RecordingPhotoRepository : PhotoRepository {
        var updateWithTagsCalls = 0
            private set

        override suspend fun getPhoto(photoId: String): Photo? = null
        override suspend fun getPhotosByEvent(eventId: String): List<Photo> = emptyList()
        override suspend fun getAllPhotos(): List<Photo> = emptyList()
        override suspend fun savePhoto(photo: Photo) = Unit
        override suspend fun updatePhotoCaption(photoId: String, caption: String?) = Unit
        override suspend fun updatePhotoWithTags(
            photoId: String,
            faces: List<FaceDetection>,
            tags: List<PhotoTag>
        ) {
            updateWithTagsCalls += 1
        }

        override suspend fun addTagsToPhoto(photoId: String, tags: List<PhotoTag>) = Unit
        override suspend fun removeTagFromPhoto(photoId: String, tagId: String) = Unit
        override suspend fun addPhotoToAlbum(photoId: String, albumId: String) = Unit
        override suspend fun removePhotoFromAlbum(photoId: String, albumId: String) = Unit
        override suspend fun searchByQuery(query: String): List<Photo> = emptyList()
        override suspend fun getPhotosByMinConfidence(minConfidence: Double): List<Photo> = emptyList()
        override suspend fun getPhotosWithFaces(): List<Photo> = emptyList()
        override suspend fun getPhotosByIds(ids: List<String>): List<Photo> = emptyList()
        override suspend fun deletePhoto(photoId: String) = Unit
        override suspend fun setFavorite(photoId: String, isFavorite: Boolean) = Unit
    }

    private class FailingAlbumRepository : AlbumRepository {
        override suspend fun createAlbum(album: Album) = error("Unexpected album write")
        override suspend fun getAlbums(eventId: String?): List<Album> = emptyList()
        override suspend fun getAlbumSimple(albumId: String): Album? = null
        override suspend fun updateAlbum(album: Album) = error("Unexpected album write")
        override suspend fun updateAlbumName(albumId: String, name: String) = error("Unexpected album write")
        override suspend fun updateAlbumCover(albumId: String, coverPhotoId: String) = error("Unexpected album write")
        override suspend fun deleteAlbumSimple(albumId: String) = error("Unexpected album write")
        override suspend fun addPhotoToAlbum(albumId: String, photoId: String) = error("Unexpected album write")
        override suspend fun removePhotoFromAlbum(albumId: String, photoId: String) = error("Unexpected album write")
        override suspend fun getAlbums(params: AlbumFilterParams): Result<List<Album>> = Result.success(emptyList())
        override suspend fun getAlbum(albumId: String): Result<Album?> = Result.success(null)
        override suspend fun createSmartAlbum(
            name: String,
            coverUri: String?,
            tags: List<String>,
            isFavorite: Boolean
        ): Result<Album> = Result.failure(IllegalStateException("Unexpected album write"))

        override suspend fun updateAlbum(albumId: String, updates: AlbumUpdateParams): Result<Album> =
            Result.failure(IllegalStateException("Unexpected album write"))

        override suspend fun deleteAlbum(albumId: String): Result<Unit> =
            Result.failure(IllegalStateException("Unexpected album write"))

        override suspend fun toggleFavorite(albumId: String): Result<Album> =
            Result.failure(IllegalStateException("Unexpected album write"))

        override suspend fun addPhotosToAlbum(albumId: String, photoIds: List<String>): Result<Album> =
            Result.failure(IllegalStateException("Unexpected album write"))

        override suspend fun removePhotosFromAlbum(albumId: String, photoIds: List<String>): Result<Album> =
            Result.failure(IllegalStateException("Unexpected album write"))

        override suspend fun addTagsToAlbum(albumId: String, tags: List<String>): Result<Album> =
            Result.failure(IllegalStateException("Unexpected album write"))

        override suspend fun removeTagsFromAlbum(albumId: String, tags: List<String>): Result<Album> =
            Result.failure(IllegalStateException("Unexpected album write"))

        override suspend fun getAutoGeneratedAlbums(): Result<List<Album>> = Result.success(emptyList())
        override suspend fun getCustomAlbums(): Result<List<Album>> = Result.success(emptyList())
        override suspend fun getFavoriteAlbums(): Result<List<Album>> = Result.success(emptyList())
        override suspend fun getRecentAlbums(days: Int): Result<List<Album>> = Result.success(emptyList())
        override suspend fun searchAlbums(query: String): Result<List<Album>> = Result.success(emptyList())
        override suspend fun getAlbumsByTags(tags: List<String>, matchAll: Boolean): Result<List<Album>> =
            Result.success(emptyList())

        override suspend fun getAlbumsByDateRange(startDate: String, endDate: String): Result<List<Album>> =
            Result.success(emptyList())

        override suspend fun getAlbumsContainingPhotos(photoIds: List<String>): Result<List<Album>> =
            Result.success(emptyList())
    }
}
