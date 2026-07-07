package com.guyghost.wakeve.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import com.guyghost.wakeve.models.photoCount
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guyghost.wakeve.models.Album
import com.guyghost.wakeve.models.Photo
import com.guyghost.wakeve.services.SharingSuggestion
import com.guyghost.wakeve.viewmodel.AlbumsUiState
import com.guyghost.wakeve.viewmodel.AlbumsViewModel
import com.guyghost.wakeve.viewmodel.SearchResult

/**
 * AlbumsScreen - Main screen for managing photo albums with smart grid.
 *
 * Features:
 * - Smart grid display of albums (auto-generated and custom)
 * - Search functionality across photos and tags
 * - Auto-album suggestions
 * - Album detail view with sharing options
 * - Intelligent sharing suggestions based on content
 *
 * ## Usage
 *
 * ```kotlin
 * @Composable
 * fun AlbumsRoute(
 *     viewModel: AlbumsViewModel = viewModel()
 * ) {
 *     val state by viewModel.uiState.collectAsState()
 *
 *     LaunchedEffect(Unit) {
 *         viewModel.loadAlbums()
 *     }
 *
 *     AlbumsScreen(
 *         uiState = state,
 *         onSearchQueryChange = { viewModel.updateSearchQuery(it) },
 *         onAlbumClick = { viewModel.selectAlbum(it) },
 *         onCreateAlbum = { viewModel.createNewAlbum(it) },
 *         onDeleteAlbum = { viewModel.deleteAlbum(it) },
 *         onBackFromDetail = { viewModel.clearSelectedAlbum() },
 *         onClearError = { viewModel.clearError() }
 *     )
 * }
 * ```
 *
 * @param uiState The current UI state
 * @param onSearchQueryChange Callback when search query changes
 * @param onAlbumClick Callback when an album is clicked
 * @param onCreateAlbum Callback when creating a new album
 * @param onDeleteAlbum Callback when deleting an album
 * @param onBackFromDetail Callback when going back from album detail
 * @param onClearError Callback to clear error state
 * @param onSharePhotos Callback when sharing photos
 * @param modifier Modifier for the component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsScreen(
    uiState: AlbumsUiState,
    onSearchQueryChange: (String) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onCreateAlbum: (String) -> Unit,
    onDeleteAlbum: (String) -> Unit,
    onBackFromDetail: () -> Unit,
    onClearError: () -> Unit,
    onSharePhotos: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showCreateAlbumDialog by remember { mutableStateOf(false) }
    var searchActive by remember { mutableStateOf(false) }

    // Show error in snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            onClearError()
        }
    }

    Scaffold(
        topBar = {
            if (uiState.selectedAlbum != null) {
                // Album detail top bar
                AlbumDetailTopBar(
                    albumName = uiState.selectedAlbum.name,
                    onBack = onBackFromDetail,
                    onDelete = { uiState.selectedAlbum?.let { onDeleteAlbum(it.id) } },
                    onShare = { onSharePhotos(uiState.selectedAlbum?.photoIds ?: emptyList()) }
                )
            } else {
                // Main albums top bar
                TopAppBar(
                    title = {
                        Text(
                            text = albumsScreenTitle(),
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    actions = {
                        IconButton(onClick = { searchActive = true }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = albumSearchContentDescription()
                            )
                        }
                        IconButton(onClick = { showCreateAlbumDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = albumCreateContentDescription()
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        floatingActionButton = {
            if (uiState.selectedAlbum == null) {
                FloatingActionButton(
                    onClick = { showCreateAlbumDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = albumAddPhotosContentDescription()
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                // Search mode
                uiState.searchQuery.isNotBlank() -> {
                    SearchResultsContent(
                        searchResults = uiState.searchResults,
                        isSearching = uiState.isSearching,
                        onPhotoClick = { /* Navigate to photo detail */ }
                    )
                }

                // Album detail view
                uiState.selectedAlbum != null -> {
                    AlbumDetailContent(
                        album = uiState.selectedAlbum,
                        photos = uiState.selectedAlbumPhotos,
                        sharingSuggestions = uiState.sharingSuggestions,
                        isLoading = uiState.isLoadingDetails,
                        onPhotoClick = { /* Navigate to photo detail */ },
                        onShareSuggestionClick = { suggestion ->
                            onSharePhotos(suggestion.photoIds)
                        }
                    )
                }

                // Main albums list
                else -> {
                    AlbumsListContent(
                        albums = uiState.albums,
                        autoAlbumSuggestions = uiState.autoAlbumSuggestions,
                        isLoading = uiState.isLoading,
                        onAlbumClick = onAlbumClick,
                        onCreateAutoAlbum = onCreateAlbum,
                        onSharePhotos = onSharePhotos
                    )
                }
            }

            // Search overlay
            AnimatedVisibility(
                visible = searchActive,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                SearchOverlay(
                    query = uiState.searchQuery,
                    onQueryChange = onSearchQueryChange,
                    onClose = {
                        searchActive = false
                        onSearchQueryChange("")
                    }
                )
            }
        }
    }

    // Create album dialog
    if (showCreateAlbumDialog) {
        CreateAlbumDialog(
            onDismiss = { showCreateAlbumDialog = false },
            onCreate = { name ->
                showCreateAlbumDialog = false
                onCreateAlbum(name)
            }
        )
    }
}

/**
 * Top app bar for album detail view.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlbumDetailTopBar(
    albumName: String,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = albumName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = albumBackContentDescription()
                )
            }
        },
        actions = {
            IconButton(onClick = onShare) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = albumShareContentDescription()
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = albumDeleteContentDescription()
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

/**
 * Search overlay for photo search.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchOverlay(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit
) {
    var searchQuery by remember { mutableStateOf(query) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        SearchBar(
            inputField = {
                SearchBarDefaults.InputField(
                    query = searchQuery,
                    onQueryChange = {
                        searchQuery = it
                        onQueryChange(it)
                    },
                    onSearch = { onQueryChange(searchQuery) },
                    expanded = true,
                    onExpandedChange = {},
                    placeholder = { Text(albumSearchPlaceholder()) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { onClose() }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = albumCloseContentDescription()
                            )
                        }
                    }
                )
            },
            expanded = true,
            onExpandedChange = { if (!it) onClose() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // Search suggestions could go here
        }
    }
}

/**
 * Main albums list content with auto-album suggestions.
 */
@Composable
private fun AlbumsListContent(
    albums: List<Album>,
    autoAlbumSuggestions: List<String>,
    isLoading: Boolean,
    onAlbumClick: (Album) -> Unit,
    onCreateAutoAlbum: (String) -> Unit,
    onSharePhotos: (List<String>) -> Unit
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            albumShareSummary(albums)?.let { summary ->
                item {
                    AlbumShareSummaryCard(
                        summary = summary,
                        onShare = { onSharePhotos(summary.photoIds) }
                    )
                }
            }

            // Auto-album suggestions section
            if (autoAlbumSuggestions.isNotEmpty()) {
                item {
                    AutoAlbumSuggestionsSection(
                        suggestions = autoAlbumSuggestions,
                        onSuggestionClick = onCreateAutoAlbum
                    )
                }
            }

            // Albums section header
            if (albums.isNotEmpty()) {
                item {
                    Text(
                        text = albumListTitle(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // Albums grid
            item {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.height(((albums.size / 2 + 1) * 200).dp)
                ) {
                    items(
                        items = albums,
                        key = { it.id }
                    ) { album ->
                        AlbumCard(
                            album = album,
                            onClick = { onAlbumClick(album) }
                        )
                    }
                }
            }

            // Empty state
            if (albums.isEmpty() && autoAlbumSuggestions.isEmpty()) {
                item {
                    EmptyAlbumsState(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumShareSummaryCard(
    summary: AlbumShareSummary,
    onShare: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = summary.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = summary.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                )
            }
            TextButton(onClick = onShare) {
                Text(albumShareNowActionLabel())
            }
        }
    }
}

/**
 * Section displaying auto-album suggestions.
 */
@Composable
private fun AutoAlbumSuggestionsSection(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = albumSuggestionsTitle(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(suggestions) { suggestion ->
                AssistChip(
                    onClick = { onSuggestionClick(suggestion) },
                    label = { Text(suggestion) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

/**
 * Card displaying an album with cover photo and metadata.
 */
@Composable
fun AlbumCard(
    album: Album,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = albumCardContentDescription(album.name, album.photoCount())
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Cover photo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (album.coverPhotoId != null) {
                    // TODO: Replace with actual image loading when Coil is added
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = albumCoverContentDescription(album.name),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                } else {
                    // Placeholder when no cover
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                MaterialTheme.colorScheme.primaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoAlbum,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                // Gradient overlay at bottom
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.6f)
                                )
                            )
                        )
                )

                // Auto-generated badge
                if (album.isAutoGenerated) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = albumAutoBadgeLabel(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Album info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = album.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoAlbum,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = albumPhotoCountLabel(album.photoCount()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Album detail content showing photos and sharing options.
 */
@Composable
private fun AlbumDetailContent(
    album: Album,
    photos: List<Photo>,
    sharingSuggestions: List<SharingSuggestion>,
    isLoading: Boolean,
    onPhotoClick: (Photo) -> Unit,
    onShareSuggestionClick: (SharingSuggestion) -> Unit
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Album info header
            item {
                AlbumInfoHeader(album = album, photoCount = photos.size)
            }

            // Smart sharing suggestions
            if (sharingSuggestions.isNotEmpty()) {
                item {
                    SmartSharingSection(
                        suggestions = sharingSuggestions,
                        onSuggestionClick = onShareSuggestionClick
                    )
                }
            }

            // Photos grid
            item {
                Text(
                    text = albumPhotosSectionTitle(photos.size),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            item {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 100.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(((photos.size / 3 + 1) * 110).dp)
                ) {
                    items(
                        items = photos,
                        key = { it.id }
                    ) { photo ->
                        PhotoThumbnailItem(
                            photo = photo,
                            onClick = { onPhotoClick(photo) }
                        )
                    }
                }
            }

            // Empty state
            if (photos.isEmpty()) {
                item {
                    EmptyPhotosState(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp)
                    )
                }
            }
        }
    }
}

/**
 * Header showing album information.
 */
@Composable
private fun AlbumInfoHeader(
    album: Album,
    photoCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.PhotoAlbum,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = album.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = albumPhotoCountLabel(photoCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Section showing intelligent sharing suggestions.
 */
@Composable
private fun SmartSharingSection(
    suggestions: List<SharingSuggestion>,
    onSuggestionClick: (SharingSuggestion) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = albumSmartSharingTitle(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        suggestions.forEach { suggestion ->
            SharingSuggestionItem(
                suggestion = suggestion,
                onClick = { onSuggestionClick(suggestion) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * Individual sharing suggestion item.
 */
@Composable
private fun SharingSuggestionItem(
    suggestion: SharingSuggestion,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = suggestion.icon,
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = suggestion.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = suggestion.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = albumShareContentDescription(),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Thumbnail item for photos in album detail.
 */
@Composable
private fun PhotoThumbnailItem(
    photo: Photo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = albumPhotoContentDescription(photo.caption)
            },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // TODO: Replace with actual image loading when Coil is added
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Favorite indicator
            if (photo.isFavorite) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = albumFavoriteContentDescription(),
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(16.dp)
                )
            }
        }
    }
}

/**
 * Search results content showing matching photos.
 */
@Composable
private fun SearchResultsContent(
    searchResults: List<SearchResult>,
    isSearching: Boolean,
    onPhotoClick: (Photo) -> Unit
) {
    if (isSearching) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 120.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = searchResults,
                key = { it.photo.id }
            ) { result ->
                SearchResultCard(
                    result = result,
                    onClick = { onPhotoClick(result.photo) }
                )
            }

            // Empty state
            if (searchResults.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = albumSearchEmptyMessage(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Card displaying a search result with relevance score.
 */
@Composable
private fun SearchResultCard(
    result: SearchResult,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column {
            // Photo thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                // TODO: Replace with actual image loading when Coil is added
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Relevance score badge
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    Text(
                        text = "${(result.relevanceScore * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Caption and matched tags
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                result.photo.caption?.let { caption ->
                    Text(
                        text = caption,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (result.matchedTags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = result.matchedTags.take(2).joinToString(", "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Dialog for creating a new album.
 */
@Composable
private fun CreateAlbumDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var albumName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = albumCreateDialogTitle(),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column {
                Text(
                    text = albumCreateDialogDescription(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = albumName,
                    onValueChange = { albumName = it },
                    label = { Text(albumNameFieldLabel()) },
                    placeholder = { Text(albumNamePlaceholder()) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(albumName) },
                enabled = albumName.isNotBlank()
            ) {
                Text(albumCreateActionLabel())
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(albumCancelActionLabel())
            }
        }
    )
}

/**
 * Empty state when no albums exist.
 */
@Composable
private fun EmptyAlbumsState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.PhotoAlbum,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp)
        )
        Text(
            text = albumEmptyTitle(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = albumEmptyDescription(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Empty state when album has no photos.
 */
@Composable
private fun EmptyPhotosState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.PhotoCamera,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp)
        )
        Text(
            text = albumEmptyPhotosTitle(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = albumEmptyPhotosDescription(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Convenience function to create AlbumsScreen with default ViewModel.
 *
 * @param viewModel The AlbumsViewModel instance
 * @param modifier Modifier for the component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsScreen(
    modifier: Modifier = Modifier,
    viewModel: AlbumsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    AlbumsScreen(
        uiState = uiState,
        onSearchQueryChange = { viewModel.updateSearchQuery(it) },
        onAlbumClick = { viewModel.selectAlbum(it) },
        onCreateAlbum = { viewModel.createNewAlbum(it) },
        onDeleteAlbum = { viewModel.deleteAlbum(it) },
        onBackFromDetail = { viewModel.clearSelectedAlbum() },
        onClearError = { viewModel.clearError() },
        onSharePhotos = { viewModel.getSharingSuggestionsForPhotos(it) },
        modifier = modifier
    )
}

internal fun albumsScreenTitle(): String = "Albums"

internal fun albumSearchContentDescription(): String = "Rechercher des photos"

internal fun albumCreateContentDescription(): String = "Créer un album"

internal fun albumAddPhotosContentDescription(): String = "Ajouter des photos"

internal fun albumBackContentDescription(): String = "Retour"

internal fun albumShareContentDescription(): String = "Partager"

internal fun albumDeleteContentDescription(): String = "Supprimer"

internal fun albumCloseContentDescription(): String = "Fermer"

internal fun albumSearchPlaceholder(): String = "Rechercher des photos..."

internal fun albumListTitle(): String = "Mes albums"

internal fun albumSuggestionsTitle(): String = "Suggestions d'albums"

internal fun albumCardContentDescription(albumName: String, photoCount: Int): String =
    "Album $albumName, ${albumPhotoCountLabel(photoCount)}"

internal fun albumCoverContentDescription(albumName: String): String =
    "Photo de couverture de l'album $albumName"

internal fun albumAutoBadgeLabel(): String = "Auto"

internal fun albumPhotoCountLabel(photoCount: Int): String =
    "$photoCount photo${if (photoCount > 1) "s" else ""}"

internal fun albumPhotosSectionTitle(photoCount: Int): String =
    "Photos (${albumPhotoCountLabel(photoCount)})"

internal fun albumSmartSharingTitle(): String = "Partage intelligent"

internal data class AlbumShareSummary(
    val albumId: String,
    val title: String,
    val body: String,
    val photoIds: List<String>
)

internal fun albumShareSummary(albums: List<Album>): AlbumShareSummary? {
    val recommended = albums
        .filter { it.photoIds.isNotEmpty() }
        .maxWithOrNull(
            compareBy<Album> { it.shareScore() }
                .thenBy { it.updatedAt ?: it.createdAt }
                .thenBy { it.name }
        )
        ?: return null

    return AlbumShareSummary(
        albumId = recommended.id,
        title = albumShareSummaryTitle(recommended.name),
        body = albumShareSummaryBody(recommended.photoCount(), recommended.isAutoGenerated),
        photoIds = recommended.photoIds
    )
}

private fun Album.shareScore(): Int =
    photoCount() +
        if (isAutoGenerated) 25 else 0 +
        if (coverPhotoId != null) 10 else 0

internal fun albumShareSummaryTitle(albumName: String): String =
    "À partager maintenant : $albumName"

internal fun albumShareSummaryBody(photoCount: Int, isAutoGenerated: Boolean): String {
    val sourceLabel = if (isAutoGenerated) {
        "cette sélection automatique"
    } else {
        "cet album prêt"
    }
    return "${albumPhotoCountLabel(photoCount)} dans $sourceLabel pour relancer le groupe après l'événement."
}

internal fun albumShareNowActionLabel(): String = "Partager"

internal fun albumPhotoContentDescription(caption: String?): String =
    "Photo : ${caption?.takeIf { it.isNotBlank() } ?: "sans légende"}"

internal fun albumFavoriteContentDescription(): String = "Photo favorite"

internal fun albumSearchEmptyMessage(): String = "Aucune photo trouvée"

internal fun albumCreateDialogTitle(): String = "Nouvel album"

internal fun albumCreateDialogDescription(): String = "Donnez un nom à votre album."

internal fun albumNameFieldLabel(): String = "Nom de l'album"

internal fun albumNamePlaceholder(): String = "Ex. Mariage de Sophie"

internal fun albumCreateActionLabel(): String = "Créer"

internal fun albumCancelActionLabel(): String = "Annuler"

internal fun albumEmptyTitle(): String = "Aucun album"

internal fun albumEmptyDescription(): String =
    "Créez un album pour retrouver les photos après l'événement."

internal fun albumEmptyPhotosTitle(): String = "Aucune photo"

internal fun albumEmptyPhotosDescription(): String =
    "Ajoutez des photos pour partager les souvenirs avec le groupe."
