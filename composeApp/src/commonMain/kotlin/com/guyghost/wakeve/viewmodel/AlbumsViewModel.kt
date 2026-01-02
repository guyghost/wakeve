package com.guyghost.wakeve.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guyghost.wakeve.models.Album
import com.guyghost.wakeve.models.Photo
import com.guyghost.wakeve.models.photoCount
import com.guyghost.wakeve.repository.AlbumRepository
import com.guyghost.wakeve.repository.EventRepository
import com.guyghost.wakeve.repository.PhotoRepository
import com.guyghost.wakeve.services.SmartSharingService
import com.guyghost.wakeve.services.SharingSuggestion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Albums screen.
 *
 * Manages album display, search functionality, and intelligent sharing suggestions.
 * Provides a reactive UI state for album operations and photo search.
 *
 * ## Features
 *
 * - Album listing with auto-generated and custom albums
 * - Search functionality across photos and tags
 * - Auto-album suggestions based on content
 * - Smart sharing recommendations
 * - Album detail navigation
 *
 * ## Usage in Compose
 *
 * ```kotlin
 * @Composable
 * fun AlbumsScreen(
 *     viewModel: AlbumsViewModel = viewModel()
 * ) {
 *     val uiState by viewModel.uiState.collectAsState()
 *
 *     LaunchedEffect(Unit) {
 *         viewModel.loadAlbums()
 *     }
 *
 *     AlbumsContent(
 *         state = uiState,
 *         onSearchQueryChange = { viewModel.updateSearchQuery(it) },
 *         onAlbumClick = { viewModel.selectAlbum(it) },
 *         onCreateAlbum = { viewModel.createNewAlbum(it) }
 *     )
 * }
 * ```
 *
 * @property albumRepository Repository for album operations
 * @property photoRepository Repository for photo operations
 * @property eventRepository Repository for event information
 * @property smartSharingService Service for intelligent sharing suggestions
 */
class AlbumsViewModel(
    private val albumRepository: AlbumRepository,
    private val photoRepository: PhotoRepository,
    private val eventRepository: EventRepository,
    private val smartSharingService: SmartSharingService
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlbumsUiState())
    val uiState: StateFlow<AlbumsUiState> = _uiState.asStateFlow()

    init {
        loadAlbums()
    }

    /**
     * Loads all albums (both auto-generated and custom).
     */
    fun loadAlbums() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val albums = albumRepository.getAlbums(eventId = null)
                val autoSuggestions = generateAutoAlbumSuggestions(albums)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        albums = albums.sortedByDescending { album ->
                            album.createdAt
                        },
                        autoAlbumSuggestions = autoSuggestions
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Erreur lors du chargement des albums"
                    )
                }
            }
        }
    }

    /**
     * Updates the search query and triggers search if query is not empty.
     *
     * @param query The new search query
     */
    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }

        if (query.isNotBlank()) {
            performSearch(query)
        } else {
            _uiState.update { it.copy(searchResults = emptyList()) }
        }
    }

    /**
     * Performs a search across photos based on the query.
     *
     * @param query The search query
     */
    private fun performSearch(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }

            try {
                val photos = photoRepository.searchByQuery(query)
                val searchResults = photos.map { photo ->
                    SearchResult(
                        photo = photo,
                        relevanceScore = calculateRelevanceScore(photo, query),
                        matchedTags = photo.tags
                            .filter { it.label.contains(query, ignoreCase = true) }
                            .map { it.label }
                    )
                }.sortedByDescending { it.relevanceScore }

                _uiState.update {
                    it.copy(
                        isSearching = false,
                        searchResults = searchResults
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        error = e.message ?: "Erreur lors de la recherche"
                    )
                }
            }
        }
    }

    /**
     * Selects an album to view its details.
     *
     * @param album The album to select
     */
    fun selectAlbum(album: Album) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingDetails = true) }

            try {
                val photos = album.photoIds.mapNotNull { photoRepository.getPhoto(it) }
                val sharingSuggestions = smartSharingService.getAlbumSharingSuggestions(album.id)

                _uiState.update {
                    it.copy(
                        isLoadingDetails = false,
                        selectedAlbum = album,
                        selectedAlbumPhotos = photos,
                        sharingSuggestions = sharingSuggestions
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingDetails = false,
                        error = e.message ?: "Erreur lors du chargement de l'album"
                    )
                }
            }
        }
    }

    /**
     * Clears the selected album.
     */
    fun clearSelectedAlbum() {
        _uiState.update {
            it.copy(
                selectedAlbum = null,
                selectedAlbumPhotos = emptyList(),
                sharingSuggestions = emptyList()
            )
        }
    }

    /**
     * Creates a new custom album.
     *
     * @param name The name for the new album
     * @param photoIds Optional list of photo IDs to add to the album
     */
    fun createNewAlbum(name: String, photoIds: List<String> = emptyList()) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val newAlbum = Album(
                    id = generateAlbumId(),
                    eventId = null,
                    name = name,
                    coverPhotoId = photoIds.firstOrNull(),
                    photoIds = photoIds,
                    createdAt = getCurrentTimestamp(),
                    isAutoGenerated = false
                )

                albumRepository.createAlbum(newAlbum)
                loadAlbums()

                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Erreur lors de la création de l'album"
                    )
                }
            }
        }
    }

    /**
     * Creates an album from auto-suggestion.
     *
     * @param suggestionTitle The auto-suggestion title
     */
    fun createAutoAlbum(suggestionTitle: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val newAlbum = Album(
                    id = generateAlbumId(),
                    eventId = null,
                    name = suggestionTitle,
                    coverPhotoId = null,
                    photoIds = emptyList(),
                    createdAt = getCurrentTimestamp(),
                    isAutoGenerated = true
                )

                albumRepository.createAlbum(newAlbum)
                loadAlbums()

                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Erreur lors de la création de l'album"
                    )
                }
            }
        }
    }

    /**
     * Deletes an album.
     *
     * @param albumId The album ID to delete
     */
    fun deleteAlbum(albumId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                albumRepository.deleteAlbum(albumId)
                loadAlbums()

                if (_uiState.value.selectedAlbum?.id == albumId) {
                    clearSelectedAlbum()
                }

                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Erreur lors de la suppression de l'album"
                    )
                }
            }
        }
    }

    /**
     * Updates the cover photo of an album.
     *
     * @param albumId The album ID
     * @param coverPhotoId The new cover photo ID
     */
    fun updateAlbumCover(albumId: String, coverPhotoId: String) {
        viewModelScope.launch {
            try {
                albumRepository.updateAlbumCover(albumId, coverPhotoId)
                loadAlbums()

                // Update selected album if needed
                _uiState.value.selectedAlbum?.let { selected ->
                    if (selected.id == albumId) {
                        selectAlbum(selected.copy(coverPhotoId = coverPhotoId))
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Erreur lors de la mise à jour de la couverture")
                }
            }
        }
    }

    /**
     * Generates auto-album suggestions based on existing albums and content.
     *
     * @param existingAlbums List of existing albums
     * @return List of suggestion titles
     */
    private fun generateAutoAlbumSuggestions(existingAlbums: List<Album>): List<String> {
        val suggestions = mutableSetOf<String>()
        val existingNames = existingAlbums.map { it.name.lowercase() }

        // Time-based suggestions
        val currentMonth = getCurrentMonth()
        val currentYear = getCurrentYear()

        if (!existingNames.contains("mai 2025")) {
            suggestions.add("Mai 2025")
        }
        if (!existingNames.contains("été 2025")) {
            suggestions.add("été 2025")
        }

        return suggestions.toList()
    }

    /**
     * Calculates a relevance score for a search result.
     *
     * @param photo The photo to score
     * @param query The search query
     * @return Relevance score (higher = more relevant)
     */
    private fun calculateRelevanceScore(photo: Photo, query: String): Double {
        var score = 0.0

        // Caption match
        if (photo.caption?.contains(query, ignoreCase = true) == true) {
            score += 2.0
        }

        // Tag match with confidence boost
        photo.tags.forEach { tag ->
            if (tag.label.contains(query, ignoreCase = true)) {
                score += 1.0 + (tag.confidence * 0.5)
            }
        }

        // Favorites boost
        if (photo.isFavorite) {
            score += 0.5
        }

        return score
    }

    /**
     * Clears any error state.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Gets sharing suggestions for specific photos.
     *
     * @param photoIds List of photo IDs
     */
    fun getSharingSuggestionsForPhotos(photoIds: List<String>) {
        viewModelScope.launch {
            try {
                val suggestions = smartSharingService.getSharingSuggestions(photoIds)
                _uiState.update { it.copy(sharingSuggestions = suggestions) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Erreur lors de la génération des suggestions")
                }
            }
        }
    }

    // Helper functions
    private fun generateAlbumId(): String = "album-${System.currentTimeMillis()}"
    private fun getCurrentTimestamp(): String = java.time.Instant.now().toString()
    private fun getCurrentMonth(): String = java.time.LocalDate.now().month.name.lowercase()
        .replaceFirstChar { it.uppercase() }
    private fun getCurrentYear(): String = java.time.LocalDate.now().year.toString()
}

/**
 * UI state for the Albums screen.
 *
 * @property albums List of all albums
 * @property selectedAlbum Currently selected album (for detail view)
 * @property selectedAlbumPhotos Photos in the selected album
 * @property searchQuery Current search query
 * @property searchResults Search results for the query
 * @property autoAlbumSuggestions Auto-generated album suggestions
 * @property sharingSuggestions Sharing suggestions for selected content
 * @property isLoading Whether data is being loaded
 * @property isSearching Whether a search is in progress
 * @property isLoadingDetails Whether album details are loading
 * @property error Error message if any
 */
data class AlbumsUiState(
    val albums: List<Album> = emptyList(),
    val selectedAlbum: Album? = null,
    val selectedAlbumPhotos: List<Photo> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<SearchResult> = emptyList(),
    val autoAlbumSuggestions: List<String> = emptyList(),
    val sharingSuggestions: List<SharingSuggestion> = emptyList(),
    val isLoading: Boolean = false,
    val isSearching: Boolean = false,
    val isLoadingDetails: Boolean = false,
    val error: String? = null
)

/**
 * Represents a search result for photos.
 *
 * @property photo The photo that matched
 * @property relevanceScore Score indicating how well it matches the query
 * @property matchedTags Tags that matched the search query
 */
data class SearchResult(
    val photo: Photo,
    val relevanceScore: Double,
    val matchedTags: List<String>
)
