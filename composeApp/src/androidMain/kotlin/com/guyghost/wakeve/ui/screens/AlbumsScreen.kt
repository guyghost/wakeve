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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guyghost.wakeve.models.Album
import com.guyghost.wakeve.models.Photo
import com.guyghost.wakeve.R
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
    val localizedError = stringResource(R.string.error_generic)

    // Show error in snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(localizedError)
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
                            text = stringResource(R.string.albums),
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    actions = {
                        IconButton(onClick = { searchActive = true }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = stringResource(R.string.a11y_album_search)
                            )
                        }
                        IconButton(onClick = { showCreateAlbumDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.a11y_album_create)
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
                        contentDescription = stringResource(R.string.a11y_album_add_photos)
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
                    contentDescription = stringResource(R.string.a11y_album_back, albumName)
                )
            }
        },
        actions = {
            IconButton(onClick = onShare) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = stringResource(R.string.a11y_album_share, albumName)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.a11y_album_delete, albumName)
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
                    placeholder = { Text(stringResource(R.string.search_photos)) },
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
                                contentDescription = stringResource(R.string.a11y_album_close_search)
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
                        text = stringResource(R.string.my_albums),
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
    val shareDescription = stringResource(R.string.a11y_album_share, summary.albumName)
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
                    text = stringResource(R.string.album_share_summary_title, summary.albumName),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = pluralStringResource(
                        if (summary.isAutoGenerated) R.plurals.album_share_summary_auto_body else R.plurals.album_share_summary_manual_body,
                        summary.photoCount,
                        summary.photoCount
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                )
            }
            TextButton(
                onClick = onShare,
                modifier = Modifier.clearAndSetSemantics {
                    contentDescription = shareDescription
                }
            ) {
                Text(stringResource(R.string.action_share))
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
            text = stringResource(R.string.album_suggestions),
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
    val photoCount = album.photoCount()
    val albumDescription = stringResource(
        R.string.a11y_album_open,
        album.name,
        pluralStringResource(R.plurals.album_photo_count, photoCount, photoCount)
    )
    val coverDescription = album.coverPhotoId?.let { stringResource(R.string.a11y_album_cover, album.name) }
    val openDescription = listOfNotNull(albumDescription, coverDescription)
        .joinToString(stringResource(R.string.list_separator))
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable(onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = openDescription
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
                            contentDescription = null,
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
                            text = stringResource(R.string.album_auto_badge),
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
                        text = pluralStringResource(R.plurals.album_photo_count, photoCount, photoCount),
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
                    text = stringResource(
                        R.string.album_photos_section,
                        pluralStringResource(R.plurals.album_photo_count, photos.size, photos.size)
                    ),
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
                    text = pluralStringResource(R.plurals.album_photo_count, photoCount, photoCount),
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
                text = stringResource(R.string.album_smart_sharing),
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
    val shareDescription = stringResource(R.string.a11y_share_suggestion, suggestion.title)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .clearAndSetSemantics { contentDescription = shareDescription },
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
                contentDescription = null,
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
    val caption = photo.caption?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.photo_without_caption)
    val description = stringResource(
        R.string.a11y_photo,
        caption,
        stringResource(if (photo.isFavorite) R.string.a11y_photo_favorite else R.string.a11y_photo_not_favorite)
    )
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = description
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
                    contentDescription = null,
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
                            text = stringResource(R.string.no_photos_found),
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
    val relevance = (result.relevanceScore * 100).toInt()
    val caption = result.photo.caption?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.photo_without_caption)
    val resultDescription = stringResource(R.string.a11y_photo_search_result, caption, relevance)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .clearAndSetSemantics { contentDescription = resultDescription },
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
                        text = stringResource(R.string.album_relevance_score, relevance),
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
                        text = stringResource(
                            R.string.album_matched_tags,
                            result.matchedTags.take(2).joinToString(stringResource(R.string.list_separator))
                        ),
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
                text = stringResource(R.string.new_album),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.album_create_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = albumName,
                    onValueChange = { albumName = it },
                    label = { Text(stringResource(R.string.album_name)) },
                    placeholder = { Text(stringResource(R.string.album_name_hint)) },
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
                Text(stringResource(R.string.action_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
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
            text = stringResource(R.string.no_albums),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.album_empty_body),
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
            text = stringResource(R.string.no_photos),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.album_empty_photos_body),
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

internal data class AlbumShareSummary(
    val albumId: String,
    val albumName: String,
    val photoCount: Int,
    val isAutoGenerated: Boolean,
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
        albumName = recommended.name,
        photoCount = recommended.photoCount(),
        isAutoGenerated = recommended.isAutoGenerated,
        photoIds = recommended.photoIds
    )
}

private fun Album.shareScore(): Int =
    photoCount() +
        if (isAutoGenerated) 25 else 0 +
        if (coverPhotoId != null) 10 else 0
