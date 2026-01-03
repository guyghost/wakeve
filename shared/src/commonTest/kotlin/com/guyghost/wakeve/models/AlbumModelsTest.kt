package com.guyghost.wakeve.models

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Album Models (Functional Core).
 * 
 * Tests cover:
 * - AlbumSorting enum
 * - AlbumFilter enum
 * - AlbumFilterParams data class
 * - SmartAlbum data class
 * - Extension functions
 */
class AlbumModelsTest {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    // ==================== AlbumSorting Tests ====================
    
    @Test
    fun `AlbumSorting enum has correct values`() {
        val values = AlbumSorting.entries
        assertEquals(4, values.size)
        assertTrue(values.contains(AlbumSorting.DATE_ASC))
        assertTrue(values.contains(AlbumSorting.DATE_DESC))
        assertTrue(values.contains(AlbumSorting.NAME_ASC))
        assertTrue(values.contains(AlbumSorting.NAME_DESC))
    }
    
    @Test
    fun `AlbumSorting displayName is correct`() {
        assertEquals("Date (Oldest First)", AlbumSorting.DATE_ASC.displayName)
        assertEquals("Date (Newest First)", AlbumSorting.DATE_DESC.displayName)
        assertEquals("Name (A-Z)", AlbumSorting.NAME_ASC.displayName)
        assertEquals("Name (Z-A)", AlbumSorting.NAME_DESC.displayName)
    }
    
    @Test
    fun `AlbumSorting fromValue returns correct enum`() {
        assertEquals(AlbumSorting.DATE_ASC, AlbumSorting.fromValue("date_asc"))
        assertEquals(AlbumSorting.DATE_DESC, AlbumSorting.fromValue("date_desc"))
        assertEquals(AlbumSorting.NAME_ASC, AlbumSorting.fromValue("name_asc"))
        assertEquals(AlbumSorting.NAME_DESC, AlbumSorting.fromValue("name_desc"))
    }
    
    @Test
    fun `AlbumSorting fromValue returns default for unknown value`() {
        assertEquals(AlbumSorting.DATE_DESC, AlbumSorting.fromValue("unknown"))
        assertEquals(AlbumSorting.DATE_DESC, AlbumSorting.fromValue(""))
        assertEquals(AlbumSorting.DATE_DESC, AlbumSorting.fromValue("invalid_sort"))
    }
    
    // ==================== AlbumFilter Tests ====================
    
    @Test
    fun `AlbumFilter enum has correct values`() {
        val values = AlbumFilter.entries
        assertEquals(5, values.size)
        assertTrue(values.contains(AlbumFilter.ALL))
        assertTrue(values.contains(AlbumFilter.RECENT))
        assertTrue(values.contains(AlbumFilter.FAVORITES))
        assertTrue(values.contains(AlbumFilter.TAGS))
        assertTrue(values.contains(AlbumFilter.DATE_RANGE))
    }
    
    @Test
    fun `AlbumFilter displayName is correct`() {
        assertEquals("All Albums", AlbumFilter.ALL.displayName)
        assertEquals("Recent", AlbumFilter.RECENT.displayName)
        assertEquals("Favorites", AlbumFilter.FAVORITES.displayName)
        assertEquals("Tags", AlbumFilter.TAGS.displayName)
        assertEquals("Date Range", AlbumFilter.DATE_RANGE.displayName)
    }
    
    @Test
    fun `AlbumFilter fromValue returns correct enum`() {
        assertEquals(AlbumFilter.ALL, AlbumFilter.fromValue("all"))
        assertEquals(AlbumFilter.RECENT, AlbumFilter.fromValue("recent"))
        assertEquals(AlbumFilter.FAVORITES, AlbumFilter.fromValue("favorites"))
        assertEquals(AlbumFilter.TAGS, AlbumFilter.fromValue("tags"))
        assertEquals(AlbumFilter.DATE_RANGE, AlbumFilter.fromValue("date_range"))
    }
    
    @Test
    fun `AlbumFilter fromValue returns default for unknown value`() {
        assertEquals(AlbumFilter.ALL, AlbumFilter.fromValue("unknown"))
        assertEquals(AlbumFilter.ALL, AlbumFilter.fromValue(""))
        assertEquals(AlbumFilter.ALL, AlbumFilter.fromValue("invalid_filter"))
    }
    
    // ==================== AlbumFilterParams Tests ====================
    
    @Test
    fun `AlbumFilterParams default values are correct`() {
        val params = AlbumFilterParams()
        assertEquals(AlbumFilter.ALL, params.filter)
        assertEquals(AlbumSorting.DATE_DESC, params.sorting)
        assertNull(params.startDate)
        assertNull(params.endDate)
        assertTrue(params.tags.isEmpty())
    }
    
    @Test
    fun `AlbumFilterParams requiresDateRange returns true for DATE_RANGE filter`() {
        val params = AlbumFilterParams(filter = AlbumFilter.DATE_RANGE)
        assertTrue(params.requiresDateRange)
    }
    
    @Test
    fun `AlbumFilterParams requiresDateRange returns false for other filters`() {
        assertFalse(AlbumFilterParams(filter = AlbumFilter.ALL).requiresDateRange)
        assertFalse(AlbumFilterParams(filter = AlbumFilter.RECENT).requiresDateRange)
        assertFalse(AlbumFilterParams(filter = AlbumFilter.FAVORITES).requiresDateRange)
        assertFalse(AlbumFilterParams(filter = AlbumFilter.TAGS).requiresDateRange)
    }
    
    @Test
    fun `AlbumFilterParams requiresTags returns true for TAGS filter`() {
        val params = AlbumFilterParams(filter = AlbumFilter.TAGS)
        assertTrue(params.requiresTags)
    }
    
    @Test
    fun `AlbumFilterParams requiresTags returns false for other filters`() {
        assertFalse(AlbumFilterParams(filter = AlbumFilter.ALL).requiresTags)
        assertFalse(AlbumFilterParams(filter = AlbumFilter.RECENT).requiresTags)
        assertFalse(AlbumFilterParams(filter = AlbumFilter.FAVORITES).requiresTags)
        assertFalse(AlbumFilterParams(filter = AlbumFilter.DATE_RANGE).requiresTags)
    }
    
    @Test
    fun `AlbumFilterParams isValid returns true for ALL filter`() {
        val params = AlbumFilterParams(filter = AlbumFilter.ALL)
        assertTrue(params.isValid)
    }
    
    @Test
    fun `AlbumFilterParams isValid returns true for DATE_RANGE with dates`() {
        val params = AlbumFilterParams(
            filter = AlbumFilter.DATE_RANGE,
            startDate = "2024-01-01",
            endDate = "2024-12-31"
        )
        assertTrue(params.isValid)
    }
    
    @Test
    fun `AlbumFilterParams isValid returns false for DATE_RANGE without dates`() {
        val params = AlbumFilterParams(
            filter = AlbumFilter.DATE_RANGE,
            startDate = null,
            endDate = "2024-12-31"
        )
        assertFalse(params.isValid)
    }
    
    @Test
    fun `AlbumFilterParams isValid returns true for TAGS with tags`() {
        val params = AlbumFilterParams(
            filter = AlbumFilter.TAGS,
            tags = listOf("vacation", "beach")
        )
        assertTrue(params.isValid)
    }
    
    @Test
    fun `AlbumFilterParams isValid returns false for TAGS without tags`() {
        val params = AlbumFilterParams(
            filter = AlbumFilter.TAGS,
            tags = emptyList()
        )
        assertFalse(params.isValid)
    }
    
    @Test
    fun `AlbumFilterParams withSorting creates copy with new sorting`() {
        val params = AlbumFilterParams.DEFAULT
        val newParams = params.withSorting(AlbumSorting.NAME_ASC)
        assertEquals(AlbumSorting.NAME_ASC, newParams.sorting)
        assertEquals(params.filter, newParams.filter)
    }
    
    @Test
    fun `AlbumFilterParams withFilter creates copy with new filter`() {
        val params = AlbumFilterParams.DEFAULT
        val newParams = params.withFilter(AlbumFilter.FAVORITES)
        assertEquals(AlbumFilter.FAVORITES, newParams.filter)
        assertEquals(params.sorting, newParams.sorting)
    }
    
    @Test
    fun `AlbumFilterParams withDateRange creates copy with new dates`() {
        val params = AlbumFilterParams.DEFAULT
        val newParams = params.withDateRange("2024-01-01", "2024-12-31")
        assertEquals("2024-01-01", newParams.startDate)
        assertEquals("2024-12-31", newParams.endDate)
    }
    
    @Test
    fun `AlbumFilterParams withTags creates copy with new tags`() {
        val params = AlbumFilterParams.DEFAULT
        val newParams = params.withTags(listOf("tag1", "tag2"))
        assertEquals(listOf("tag1", "tag2"), newParams.tags)
    }
    
    @Test
    fun `AlbumFilterParams DEFAULT is correct`() {
        val default = AlbumFilterParams.DEFAULT
        assertEquals(AlbumFilter.ALL, default.filter)
        assertEquals(AlbumSorting.DATE_DESC, default.sorting)
        assertNull(default.startDate)
        assertNull(default.endDate)
        assertTrue(default.tags.isEmpty())
    }
    
    @Test
    fun `AlbumFilterParams RECENT is correct`() {
        val recent = AlbumFilterParams.RECENT
        assertEquals(AlbumFilter.RECENT, recent.filter)
        assertEquals(AlbumSorting.DATE_DESC, recent.sorting)
    }
    
    @Test
    fun `AlbumFilterParams FAVORITES is correct`() {
        val favorites = AlbumFilterParams.FAVORITES
        assertEquals(AlbumFilter.FAVORITES, favorites.filter)
        assertEquals(AlbumSorting.DATE_DESC, favorites.sorting)
    }
    
    // ==================== AlbumFilterParams Serialization Tests ====================
    
    @Test
    fun `AlbumFilterParams serializes correctly`() {
        val params = AlbumFilterParams(
            filter = AlbumFilter.DATE_RANGE,
            sorting = AlbumSorting.NAME_ASC,
            startDate = "2024-01-01",
            endDate = "2024-12-31",
            tags = listOf("vacation")
        )
        val jsonStr = json.encodeToString(params)
        assertTrue(jsonStr.contains("\"filter\":\"date_range\""))
        assertTrue(jsonStr.contains("\"sorting\":\"name_asc\""))
        assertTrue(jsonStr.contains("\"startDate\":\"2024-01-01\""))
        assertTrue(jsonStr.contains("\"endDate\":\"2024-12-31\""))
        assertTrue(jsonStr.contains("\"tags\":[\"vacation\"]"))
    }
    
    @Test
    fun `AlbumFilterParams deserializes correctly`() {
        val jsonStr = """{"filter":"favorites","sorting":"name_desc","tags":["beach"]}"""
        val params = json.decodeFromString<AlbumFilterParams>(jsonStr)
        assertEquals(AlbumFilter.FAVORITES, params.filter)
        assertEquals(AlbumSorting.NAME_DESC, params.sorting)
        assertEquals(listOf("beach"), params.tags)
    }
    
    // ==================== SmartAlbum Tests ====================
    
    @Test
    fun `SmartAlbum default values are correct`() {
        val album = SmartAlbum(
            id = "album-1",
            name = "Test Album",
            coverUri = "content://photo.jpg",
            photoCount = 10,
            dateCreated = "2024-01-15T10:30:00Z"
        )
        assertFalse(album.isFavorite)
        assertTrue(album.tags.isEmpty())
        assertNull(album.location)
        assertTrue(album.suggestedFor.isEmpty())
        assertEquals(0.0, album.aiConfidence, 0.001)
        assertEquals(SmartAlbumType.CUSTOM, album.smartType)
    }
    
    @Test
    fun `SmartAlbum isAiSuggested returns true for AI_SUGGESTED type`() {
        val album = SmartAlbum(
            id = "album-1",
            name = "Suggested Album",
            coverUri = "content://photo.jpg",
            photoCount = 5,
            dateCreated = "2024-01-15T10:30:00Z",
            smartType = SmartAlbumType.AI_SUGGESTED
        )
        assertTrue(album.isAiSuggested())
    }
    
    @Test
    fun `SmartAlbum isAiSuggested returns false for non-AI types`() {
        val custom = SmartAlbum(
            id = "album-1",
            name = "Custom Album",
            coverUri = "content://photo.jpg",
            photoCount = 5,
            dateCreated = "2024-01-15T10:30:00Z",
            smartType = SmartAlbumType.CUSTOM
        )
        assertFalse(custom.isAiSuggested())
        
        val auto = SmartAlbum(
            id = "album-2",
            name = "Auto Album",
            coverUri = "content://photo.jpg",
            photoCount = 5,
            dateCreated = "2024-01-15T10:30:00Z",
            smartType = SmartAlbumType.AUTO_GENERATED
        )
        assertFalse(auto.isAiSuggested())
    }
    
    @Test
    fun `SmartAlbum isRecent returns true for recent albums`() {
        val recent = SmartAlbum(
            id = "album-1",
            name = "Recent Album",
            coverUri = "content://photo.jpg",
            photoCount = 5,
            dateCreated = java.time.Instant.now().toString()
        )
        assertTrue(recent.isRecent())
    }
    
    @Test
    fun `SmartAlbum isRecent returns false for old albums`() {
        val old = SmartAlbum(
            id = "album-1",
            name = "Old Album",
            coverUri = "content://photo.jpg",
            photoCount = 5,
            dateCreated = "2020-01-15T10:30:00Z"
        )
        assertFalse(old.isRecent())
    }
    
    @Test
    fun `SmartAlbum isRecent returns false for invalid date`() {
        val invalid = SmartAlbum(
            id = "album-1",
            name = "Invalid Date Album",
            coverUri = "content://photo.jpg",
            photoCount = 5,
            dateCreated = "not-a-date"
        )
        assertFalse(invalid.isRecent())
    }
    
    // ==================== SmartAlbumType Tests ====================
    
    @Test
    fun `SmartAlbumType enum has all expected values`() {
        val types = SmartAlbumType.entries
        assertEquals(4, types.size)
        assertTrue(types.contains(SmartAlbumType.CUSTOM))
        assertTrue(types.contains(SmartAlbumType.AUTO_GENERATED))
        assertTrue(types.contains(SmartAlbumType.AI_SUGGESTED))
        assertTrue(types.contains(SmartAlbumType.EVENT_BASED))
    }
    
    // ==================== Extension Function Tests ====================
    
    @Test
    fun `SmartAlbum toAlbum converts correctly`() {
        val smartAlbum = SmartAlbum(
            id = "smart-1",
            name = "Smart Album",
            coverUri = "content://cover.jpg",
            photoCount = 15,
            dateCreated = "2024-01-15T10:30:00Z",
            isFavorite = true,
            tags = listOf("vacation", "beach"),
            location = "Miami",
            suggestedFor = listOf("user-1", "user-2"),
            aiConfidence = 0.85,
            smartType = SmartAlbumType.AI_SUGGESTED
        )
        
        val album = smartAlbum.toAlbum()
        assertEquals("smart-1", album.id)
        assertNull(album.eventId)
        assertEquals("Smart Album", album.name)
        assertEquals("content://cover.jpg", album.coverPhotoId)
        assertTrue(album.photoIds.isEmpty())
        assertEquals("2024-01-15T10:30:00Z", album.createdAt)
        assertTrue(album.isAutoGenerated)
        assertNull(album.updatedAt)
    }
    
    @Test
    fun `SmartAlbum toAlbum preserves auto-generated status for AI_SUGGESTED`() {
        val aiAlbum = SmartAlbum(
            id = "ai-1",
            name = "AI Album",
            coverUri = "content://cover.jpg",
            photoCount = 10,
            dateCreated = "2024-01-15T10:30:00Z",
            smartType = SmartAlbumType.AI_SUGGESTED
        )
        assertTrue(aiAlbum.toAlbum().isAutoGenerated)
    }
    
    @Test
    fun `SmartAlbum toAlbum preserves auto-generated status for CUSTOM`() {
        val customAlbum = SmartAlbum(
            id = "custom-1",
            name = "Custom Album",
            coverUri = "content://cover.jpg",
            photoCount = 10,
            dateCreated = "2024-01-15T10:30:00Z",
            smartType = SmartAlbumType.CUSTOM
        )
        assertFalse(customAlbum.toAlbum().isAutoGenerated)
    }
}

/**
 * Unit tests for AlbumUpdateParams (Functional Core).
 */
class AlbumUpdateParamsTest {
    
    @Test
    fun `AlbumUpdateParams hasUpdates returns false when no updates`() {
        val params = AlbumUpdateParams()
        assertFalse(params.hasUpdates)
    }
    
    @Test
    fun `AlbumUpdateParams hasUpdates returns true when name is set`() {
        val params = AlbumUpdateParams(name = "New Name")
        assertTrue(params.hasUpdates)
    }
    
    @Test
    fun `AlbumUpdateParams hasUpdates returns true when coverUri is set`() {
        val params = AlbumUpdateParams(coverUri = "content://new.jpg")
        assertTrue(params.hasUpdates)
    }
    
    @Test
    fun `AlbumUpdateParams hasUpdates returns true when tags is set`() {
        val params = AlbumUpdateParams(tags = listOf("tag1"))
        assertTrue(params.hasUpdates)
    }
    
    @Test
    fun `AlbumUpdateParams hasUpdates returns true when isFavorite is set`() {
        val params = AlbumUpdateParams(isFavorite = true)
        assertTrue(params.hasUpdates)
    }
    
    @Test
    fun `AlbumUpdateParams applyTo updates name`() {
        val album = Album(
            id = "album-1",
            eventId = null,
            name = "Old Name",
            coverPhotoId = "content://old.jpg",
            photoIds = emptyList(),
            createdAt = "2024-01-01T00:00:00Z",
            isAutoGenerated = false,
            updatedAt = null
        )
        val params = AlbumUpdateParams.rename("New Name")
        val updated = params.applyTo(album)
        assertEquals("New Name", updated.name)
        assertNotNull(updated.updatedAt)
    }
    
    @Test
    fun `AlbumUpdateParams applyTo updates coverUri`() {
        val album = Album(
            id = "album-1",
            eventId = null,
            name = "Album",
            coverPhotoId = "content://old.jpg",
            photoIds = emptyList(),
            createdAt = "2024-01-01T00:00:00Z",
            isAutoGenerated = false,
            updatedAt = null
        )
        val params = AlbumUpdateParams.changeCover("content://new.jpg")
        val updated = params.applyTo(album)
        assertEquals("content://new.jpg", updated.coverPhotoId)
    }
    
    @Test
    fun `AlbumUpdateParams applyTo keeps existing values when not updating`() {
        val album = Album(
            id = "album-1",
            eventId = null,
            name = "Album",
            coverPhotoId = "content://old.jpg",
            photoIds = emptyList(),
            createdAt = "2024-01-01T00:00:00Z",
            isAutoGenerated = false,
            updatedAt = null
        )
        val params = AlbumUpdateParams() // No updates
        val updated = params.applyTo(album)
        assertEquals("Album", updated.name)
        assertEquals("content://old.jpg", updated.coverPhotoId)
        assertNull(updated.updatedAt)
    }
    
    @Test
    fun `AlbumUpdateParams rename creates correct params`() {
        val params = AlbumUpdateParams.rename("Test Album")
        assertEquals("Test Album", params.name)
        assertNull(params.coverUri)
        assertNull(params.tags)
        assertNull(params.isFavorite)
    }
    
    @Test
    fun `AlbumUpdateParams changeCover creates correct params`() {
        val params = AlbumUpdateParams.changeCover("content://photo.jpg")
        assertNull(params.name)
        assertEquals("content://photo.jpg", params.coverUri)
        assertNull(params.tags)
        assertNull(params.isFavorite)
    }
    
    @Test
    fun `AlbumUpdateParams setFavorite creates correct params`() {
        val params = AlbumUpdateParams.setFavorite(true)
        assertNull(params.name)
        assertNull(params.coverUri)
        assertNull(params.tags)
        assertEquals(true, params.isFavorite)
    }
    
    @Test
    fun `AlbumUpdateParams updateTags creates correct params`() {
        val params = AlbumUpdateParams.updateTags(listOf("tag1", "tag2"))
        assertNull(params.name)
        assertNull(params.coverUri)
        assertEquals(listOf("tag1", "tag2"), params.tags)
        assertNull(params.isFavorite)
    }
}
