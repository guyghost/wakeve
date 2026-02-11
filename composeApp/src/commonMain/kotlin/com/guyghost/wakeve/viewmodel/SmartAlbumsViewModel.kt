package com.guyghost.wakeve.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guyghost.wakeve.auth.AuthStateManager
import com.guyghost.wakeve.models.*
import com.guyghost.wakeve.repository.AlbumRepository
import com.guyghost.wakeve.repository.AlbumUpdateParams
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Sealed class representing the UI state for the Smart Albums screen.
 * 
 * Provides a clear state machine for loading, content, and error states.
 */
sealed class SmartAlbumsUiState {
    /**
     * Initial loading state while fetching albums.
     */
    object Loading : SmartAlbumsUiState()
    
    /**
     * Success state with list of albums.
     * 
     * @property albums List of albums to display
     */
    data class Content(
        val albums: List<Album>,
        val filterParams: AlbumFilterParams = AlbumFilterParams.DEFAULT
    ) : SmartAlbumsUiState()
    
    /**
     * Empty state when no albums match the current filter.
     * 
     * @property filterParams The filter that produced empty results
     */
    data class Empty(
        val filterParams: AlbumFilterParams = AlbumFilterParams.DEFAULT
    ) : SmartAlbumsUiState()
    
    /**
     * Error state when an operation fails.
     * 
     * @property message Error message to display
     * @property filterParams The filter that was active when error occurred
     */
    data class Error(
        val message: String,
        val filterParams: AlbumFilterParams = AlbumFilterParams.DEFAULT
    ) : SmartAlbumsUiState()
}

/**
 * Side effects for one-shot events in the Smart Albums screen.
 */
sealed class SmartAlbumsSideEffect {
    /**
     * Navigate to album detail screen.
     * 
     * @property albumId The album to view
     */
    data class NavigateToAlbum(val albumId: String) : SmartAlbumsSideEffect()
    
    /**
     * Show a toast or snackbar message.
     * 
     * @property message The message to display
     */
    data class ShowMessage(val message: String) : SmartAlbumsSideEffect()
    
    /**
     * Album was created successfully.
     * 
     * @property album The newly created album
     */
    data class AlbumCreated(val album: Album) : SmartAlbumsSideEffect()
    
    /**
     * Album was deleted successfully.
     * 
     * @property albumId The deleted album ID
     */
    data class AlbumDeleted(val albumId: String) : SmartAlbumsSideEffect()
    
    /**
     * Favorite status was toggled.
     * 
     * @property albumId The album
     * @property isFavorite New favorite status
     */
    data class FavoriteToggled(val albumId: String, val isFavorite: Boolean) : SmartAlbumsSideEffect()
}

/**
 * ViewModel for the Smart Albums screen.
 * 
 * Orchestrates album operations using the repository pattern.
 * Provides reactive UI state through StateFlow for Compose integration.
 * 
 * ## Features
 * 
 * - Loading albums with filtering and sorting
 * - Creating new albums
 * - Updating album metadata (name, cover, tags)
 * - Deleting albums
 * - Toggling favorite status
 * - Searching albums by name
 * 
 * ## Architecture
 * 
 * This ViewModel follows the Imperative Shell pattern:
 * - **State**: Exposed via StateFlow for reactive UI updates
 * - **Effects**: Exposed via SharedFlow for one-shot events (navigation, toasts)
 * - **Repository**: Handles all I/O operations
 * 
 * ## Usage in Compose
 * 
 * ```kotlin
 * @Composable
 * fun SmartAlbumsScreen(
 *     viewModel: SmartAlbumsViewModel = viewModel()
 * ) {
 *     val uiState by viewModel.uiState.collectAsState()
 *     val filterParams by viewModel.filterParams.collectAsState()
 *     
 *     LaunchedEffect(Unit) {
 *         viewModel.sideEffect.collect { effect ->
 *             when (effect) {
 *                 is SmartAlbumsSideEffect.NavigateToAlbum -> { /* Navigate */ }
 *                 is SmartAlbumsSideEffect.ShowMessage -> { /* Show snackbar */ }
 *             }
 *         }
 *     }
 *     
 *     SmartAlbumsContent(
 *         state = uiState,
 *         filterParams = filterParams,
 *         onFilterChange = { viewModel.changeFilter(it) },
 *         onSortingChange = { viewModel.changeSorting(it) },
 *         onAlbumClick = { viewModel.selectAlbum(it) },
 *         onCreateAlbum = { viewModel.createAlbum(it.name, it.coverUri, it.tags) },
 *         onDeleteAlbum = { viewModel.deleteAlbum(it) },
 *         onToggleFavorite = { viewModel.toggleFavorite(it) }
 *     )
 * }
 * ```
 * 
 * @property albumRepository Repository for album operations
 * @property authStateManager Optional authentication state manager for user context
 */
class SmartAlbumsViewModel(
    private val albumRepository: AlbumRepository,
    private val authStateManager: AuthStateManager? = null
) : ViewModel() {
    
    // ==================== State ====================
    
    /**
     * Main UI state for the screen.
     * Emits Loading, Content, Empty, or Error states.
     */
    private val _uiState = MutableStateFlow<SmartAlbumsUiState>(SmartAlbumsUiState.Loading)
    val uiState: StateFlow<SmartAlbumsUiState> = _uiState.asStateFlow()
    
    /**
     * List of albums for direct access in UI.
     */
    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums.asStateFlow()
    
    /**
     * Current filter parameters.
     */
    private val _filterParams = MutableStateFlow(AlbumFilterParams.DEFAULT)
    val filterParams: StateFlow<AlbumFilterParams> = _filterParams.asStateFlow()
    
    /**
     * Search query for filtering albums.
     */
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    /**
     * Loading state for async operations.
     */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // ==================== Side Effects ====================
    
    /**
     * One-shot side effects for the UI.
     * Collect this in a LaunchedEffect to handle navigation and toasts.
     */
    private val _sideEffect = MutableSharedFlow<SmartAlbumsSideEffect>()
    val sideEffect: SharedFlow<SmartAlbumsSideEffect> = _sideEffect.asSharedFlow()
    
    // ==================== Initialization ====================
    
    init {
        loadAlbums()
    }
    
    // ==================== Load Operations ====================
    
    /**
     * Load albums with current or specified filter parameters.
     * 
     * @param params Optional filter parameters (uses current state if not specified)
     */
    fun loadAlbums(params: AlbumFilterParams = _filterParams.value) {
        viewModelScope.launch {
            _uiState.value = SmartAlbumsUiState.Loading
            _isLoading.value = true
            
            try {
                val result = albumRepository.getAlbums(params)
                
                result.fold(
                    onSuccess = { albumList ->
                        val sortedList = sortAlbums(albumList, params.sorting)
                        _albums.value = sortedList
                        _filterParams.value = params.copy()
                        
                        if (sortedList.isEmpty()) {
                            _uiState.value = SmartAlbumsUiState.Empty(params)
                        } else {
                            _uiState.value = SmartAlbumsUiState.Content(sortedList, params)
                        }
                    },
                    onFailure = { error ->
                        _uiState.value = SmartAlbumsUiState.Error(
                            message = error.message ?: "Unknown error loading albums",
                            filterParams = params
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = SmartAlbumsUiState.Error(
                    message = e.message ?: "Unknown error loading albums",
                    filterParams = params
                )
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Refresh albums with current filter parameters.
     */
    fun refreshAlbums() {
        loadAlbums(_filterParams.value)
    }
    
    /**
     * Search albums by name query.
     * 
     * @param query Search query string
     */
    fun searchAlbums(query: String) {
        _searchQuery.value = query
        
        viewModelScope.launch {
            if (query.isBlank()) {
                // Reset to normal filtered view
                loadAlbums(_filterParams.value)
                return@launch
            }
            
            _uiState.value = SmartAlbumsUiState.Loading
            
            val result = albumRepository.searchAlbums(query)
            result.fold(
                onSuccess = { albumList ->
                    val sortedList = sortAlbums(albumList, _filterParams.value.sorting)
                    _albums.value = sortedList
                    
                    if (sortedList.isEmpty()) {
                        _uiState.value = SmartAlbumsUiState.Empty(_filterParams.value)
                    } else {
                        _uiState.value = SmartAlbumsUiState.Content(sortedList, _filterParams.value)
                    }
                },
                onFailure = { error ->
                    _uiState.value = SmartAlbumsUiState.Error(
                        message = error.message ?: "Search failed"
                    )
                }
            )
        }
    }
    
    // ==================== Filter Operations ====================
    
    /**
     * Change the current filter type.
     * 
     * @param filter New filter to apply
     */
    fun changeFilter(filter: AlbumFilter) {
        val newParams = _filterParams.value.copy(
            filter = filter,
            // Reset filter-specific params when changing filter type
            startDate = if (filter != AlbumFilter.DATE_RANGE) null else _filterParams.value.startDate,
            endDate = if (filter != AlbumFilter.DATE_RANGE) null else _filterParams.value.endDate,
            tags = if (filter != AlbumFilter.TAGS) emptyList() else _filterParams.value.tags
        )
        loadAlbums(newParams)
    }
    
    /**
     * Change the current sorting option.
     * 
     * @param sorting New sorting option
     */
    fun changeSorting(sorting: AlbumSorting) {
        val newParams = _filterParams.value.copy(sorting = sorting)
        loadAlbums(newParams)
    }
    
    /**
     * Update date range filter.
     * 
     * @param startDate Start date (ISO 8601)
     * @param endDate End date (ISO 8601)
     */
    fun updateDateRange(startDate: String?, endDate: String?) {
        val newParams = _filterParams.value.copy(
            filter = AlbumFilter.DATE_RANGE,
            startDate = startDate,
            endDate = endDate
        )
        loadAlbums(newParams)
    }
    
    /**
     * Update tags filter.
     * 
     * @param tags List of tags to filter by
     */
    fun updateTags(tags: List<String>) {
        val newParams = _filterParams.value.copy(
            filter = AlbumFilter.TAGS,
            tags = tags
        )
        loadAlbums(newParams)
    }
    
    /**
     * Clear all filters and return to default view.
     */
    fun clearFilters() {
        _searchQuery.value = ""
        loadAlbums(AlbumFilterParams.DEFAULT)
    }
    
    // ==================== CRUD Operations ====================
    
    /**
     * Create a new album.
     * 
     * @param name Album name
     * @param coverUri Optional cover photo URI
     * @param tags Optional list of tags
     */
    fun createAlbum(name: String, coverUri: String? = null, tags: List<String> = emptyList()) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val result = albumRepository.createSmartAlbum(
                    name = name,
                    coverUri = coverUri,
                    tags = tags
                )
                
                result.fold(
                    onSuccess = { album ->
                        _sideEffect.emit(SmartAlbumsSideEffect.AlbumCreated(album))
                        _sideEffect.emit(SmartAlbumsSideEffect.ShowMessage("Album created: $name"))
                        refreshAlbums()
                    },
                    onFailure = { error ->
                        _sideEffect.emit(SmartAlbumsSideEffect.ShowMessage(
                            error.message ?: "Failed to create album"
                        ))
                    }
                )
            } catch (e: Exception) {
                _sideEffect.emit(SmartAlbumsSideEffect.ShowMessage(
                    e.message ?: "Failed to create album"
                ))
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Update album details.
     * 
     * @param albumId Album ID to update
     * @param updates Parameters to update
     */
    fun updateAlbum(albumId: String, updates: AlbumUpdateParams) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val result = albumRepository.updateAlbum(albumId, updates)
                result.fold(
                    onSuccess = { album ->
                        _sideEffect.emit(SmartAlbumsSideEffect.ShowMessage("Album updated"))
                        refreshAlbums()
                    },
                    onFailure = { error ->
                        _sideEffect.emit(SmartAlbumsSideEffect.ShowMessage(
                            error.message ?: "Failed to update album"
                        ))
                    }
                )
            } catch (e: Exception) {
                _sideEffect.emit(SmartAlbumsSideEffect.ShowMessage(
                    e.message ?: "Failed to update album"
                ))
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Rename an album.
     * 
     * @param albumId Album ID
     * @param newName New album name
     */
    fun renameAlbum(albumId: String, newName: String) {
        updateAlbum(albumId, AlbumUpdateParams.rename(newName))
    }
    
    /**
     * Change album cover photo.
     * 
     * @param albumId Album ID
     * @param coverUri New cover photo URI
     */
    fun changeAlbumCover(albumId: String, coverUri: String) {
        updateAlbum(albumId, AlbumUpdateParams.changeCover(coverUri))
    }
    
    /**
     * Delete an album.
     * 
     * @param albumId Album ID to delete
     */
    fun deleteAlbum(albumId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val result = albumRepository.deleteAlbum(albumId)
                result.fold(
                    onSuccess = {
                        _sideEffect.emit(SmartAlbumsSideEffect.AlbumDeleted(albumId))
                        _sideEffect.emit(SmartAlbumsSideEffect.ShowMessage("Album deleted"))
                        refreshAlbums()
                    },
                    onFailure = { error ->
                        _sideEffect.emit(SmartAlbumsSideEffect.ShowMessage(
                            error.message ?: "Failed to delete album"
                        ))
                    }
                )
            } catch (e: Exception) {
                _sideEffect.emit(SmartAlbumsSideEffect.ShowMessage(
                    e.message ?: "Failed to delete album"
                ))
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Toggle favorite status for an album.
     * 
     * @param albumId Album ID
     */
    fun toggleFavorite(albumId: String) {
        viewModelScope.launch {
            try {
                val result = albumRepository.toggleFavorite(albumId)
                result.fold(
                    onSuccess = { album ->
                        val newStatus = album.isAutoGenerated // Simplified check
                        _sideEffect.emit(SmartAlbumsSideEffect.FavoriteToggled(albumId, newStatus))
                        refreshAlbums()
                    },
                    onFailure = { error ->
                        _sideEffect.emit(SmartAlbumsSideEffect.ShowMessage(
                            error.message ?: "Failed to update favorite status"
                        ))
                    }
                )
            } catch (e: Exception) {
                _sideEffect.emit(SmartAlbumsSideEffect.ShowMessage(
                    e.message ?: "Failed to update favorite status"
                ))
            }
        }
    }
    
    /**
     * Add photos to an album.
     * 
     * @param albumId Album ID
     * @param photoIds List of photo IDs to add
     */
    fun addPhotosToAlbum(albumId: String, photoIds: List<String>) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val result = albumRepository.addPhotosToAlbum(albumId, photoIds)
                result.fold(
                    onSuccess = {
                        _sideEffect.emit(SmartAlbumsSideEffect.ShowMessage("Photos added to album"))
                        refreshAlbums()
                    },
                    onFailure = { error ->
                        _sideEffect.emit(SmartAlbumsSideEffect.ShowMessage(
                            error.message ?: "Failed to add photos"
                        ))
                    }
                )
            } catch (e: Exception) {
                _sideEffect.emit(SmartAlbumsSideEffect.ShowMessage(
                    e.message ?: "Failed to add photos"
                ))
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Remove photos from an album.
     * 
     * @param albumId Album ID
     * @param photoIds List of photo IDs to remove
     */
    fun removePhotosFromAlbum(albumId: String, photoIds: List<String>) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val result = albumRepository.removePhotosFromAlbum(albumId, photoIds)
                result.fold(
                    onSuccess = {
                        _sideEffect.emit(SmartAlbumsSideEffect.ShowMessage("Photos removed from album"))
                        refreshAlbums()
                    },
                    onFailure = { error ->
                        _sideEffect.emit(SmartAlbumsSideEffect.ShowMessage(
                            error.message ?: "Failed to remove photos"
                        ))
                    }
                )
            } catch (e: Exception) {
                _sideEffect.emit(SmartAlbumsSideEffect.ShowMessage(
                    e.message ?: "Failed to remove photos"
                ))
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Select an album for viewing detail.
     * 
     * @param album Album to select
     */
    fun selectAlbum(album: Album) {
        viewModelScope.launch {
            _sideEffect.emit(SmartAlbumsSideEffect.NavigateToAlbum(album.id))
        }
    }
    
    /**
     * Clear any error state.
     */
    fun clearError() {
        _uiState.value = SmartAlbumsUiState.Content(
            albums = _albums.value,
            filterParams = _filterParams.value
        )
    }
    
    // ==================== Helper Functions ====================
    
    /**
     * Sort albums according to the specified sorting option.
     * 
     * @param albums List of albums to sort
     * @param sorting Sorting option
     * @return Sorted list of albums
     */
    private fun sortAlbums(albums: List<Album>, sorting: AlbumSorting): List<Album> {
        return when (sorting) {
            AlbumSorting.DATE_ASC -> albums.sortedBy { it.createdAt }
            AlbumSorting.DATE_DESC -> albums.sortedByDescending { it.createdAt }
            AlbumSorting.NAME_ASC -> albums.sortedBy { it.name.lowercase() }
            AlbumSorting.NAME_DESC -> albums.sortedByDescending { it.name.lowercase() }
        }
    }
    
    /**
     * Get the current user ID from AuthStateManager.
     */
    private suspend fun getCurrentUserId(): String? {
        return authStateManager?.getCurrentUserId()
    }
}

/**
 * Data class for album creation parameters.
 * 
 * Used to pass creation parameters from UI to ViewModel.
 * 
 * @property name Album name (required)
 * @property coverUri Optional cover photo URI
 * @property tags Optional list of tags
 */
data class CreateAlbumParams(
    val name: String,
    val coverUri: String? = null,
    val tags: List<String> = emptyList()
)
