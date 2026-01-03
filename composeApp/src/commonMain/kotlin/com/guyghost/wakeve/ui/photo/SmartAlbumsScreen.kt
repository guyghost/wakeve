package com.guyghost.wakeve.ui.photo

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.guyghost.wakeve.models.*
import com.guyghost.wakeve.viewmodel.*
import com.guyghost.wakeve.ui.theme.WakevTheme

/**
 * Smart Albums screen with Material Design 3.
 * 
 * Features:
 * - Filter chips for quick album filtering (All, Recent, Favorites, Tags, Date Range)
 * - Sorting dropdown (Date A/Z, Name A/Z)
 * - Search functionality
 * - Create album dialog
 * - Album grid with intelligent organization
 * 
 * @param viewModel SmartAlbumsViewModel instance
 * @param onAlbumClick Callback when an album is clicked
 * @param onCreateAlbum Callback when a new album is created
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SmartAlbumsScreen(
    viewModel: SmartAlbumsViewModel,
    onAlbumClick: (Album) -> Unit,
    onCreateAlbum: (CreateAlbumParams) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val filterParams by viewModel.filterParams.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    var showCreateDialog by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showSortingMenu by remember { mutableStateOf(false) }
    
    // Handle side effects
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is SmartAlbumsSideEffect.NavigateToAlbum -> { /* Handle navigation */ }
                is SmartAlbumsSideEffect.ShowMessage -> { /* Show snackbar */ }
                else -> { /* Handle other effects */ }
            }
        }
    }
    
    Scaffold(
        modifier = modifier,
        topBar = {
            SmartAlbumsTopBar(
                searchQuery = searchQuery,
                onSearchQueryChange = { viewModel.searchAlbums(it) },
                onFilterClick = { showFilterSheet = true },
                onCreateClick = { showCreateDialog = true }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter and sort controls
            SmartAlbumsFilterBar(
                filterParams = filterParams,
                onFilterChange = { viewModel.changeFilter(it) },
                onSortingChange = { viewModel.changeSorting(it) },
                onClearFilters = { viewModel.clearFilters() },
                showSortingMenu = showSortingMenu,
                onShowSortingMenu = { showSortingMenu = it }
            )
            
            // Content
            when (uiState) {
                is SmartAlbumsUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                is SmartAlbumsUiState.Content -> {
                    AlbumsGrid(
                        albums = albums,
                        onAlbumClick = onAlbumClick,
                        onFavoriteClick = { viewModel.toggleFavorite(it.id) },
                        onDeleteClick = { viewModel.deleteAlbum(it.id) }
                    )
                }
                
                is SmartAlbumsUiState.Empty -> {
                    EmptyAlbumsState(
                        filterParams = (uiState as SmartAlbumsUiState.Empty).filterParams,
                        onClearFilters = { viewModel.clearFilters() },
                        onCreateClick = { showCreateDialog = true }
                    )
                }
                
                is SmartAlbumsUiState.Error -> {
                    ErrorAlbumsState(
                        message = (uiState as SmartAlbumsUiState.Error).message,
                        onRetry = { viewModel.refreshAlbums() }
                    )
                }
            }
        }
    }
    
    // Create album dialog
    if (showCreateDialog) {
        CreateAlbumDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { params ->
                viewModel.createAlbum(params.name, params.coverUri, params.tags)
                onCreateAlbum(params)
                showCreateDialog = false
            }
        )
    }
    
    // Filter bottom sheet
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false }
        ) {
            FilterBottomSheetContent(
                filterParams = filterParams,
                onFilterChange = { viewModel.changeFilter(it) },
                onDateRangeChange = { start, end -> viewModel.updateDateRange(start, end) },
                onTagsChange = { viewModel.updateTags(it) },
                onApply = { showFilterSheet = false }
            )
        }
    }
}

/**
 * Top app bar with search functionality.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SmartAlbumsTopBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onFilterClick: () -> Unit,
    onCreateClick: () -> Unit
) {
    var isSearchActive by remember { mutableStateOf(false) }
    
    if (isSearchActive) {
        SearchBar(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            onSearch = { isSearchActive = false },
            active = false,
            onActiveChange = { },
            leadingIcon = {
                IconButton(onClick = {
                    isSearchActive = false
                    onSearchQueryChange("")
                }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Close search")
                }
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            placeholder = { Text("Search albums...") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = WakevTheme.spacing.medium)
        ) { }
    } else {
        TopAppBar(
            title = {
                Text(
                    text = "Albums",
                    style = MaterialTheme.typography.headlineMedium
                )
            },
            actions = {
                IconButton(onClick = { isSearchActive = true }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search albums"
                    )
                }
                IconButton(onClick = onFilterClick) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter albums"
                    )
                }
                IconButton(onClick = onCreateClick) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create album"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )
    }
}

/**
 * Filter bar with chips and sorting dropdown.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SmartAlbumsFilterBar(
    filterParams: AlbumFilterParams,
    onFilterChange: (AlbumFilter) -> Unit,
    onSortingChange: (AlbumSorting) -> Unit,
    onClearFilters: () -> Unit,
    showSortingMenu: Boolean,
    onShowSortingMenu: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = WakevTheme.spacing.small)
    ) {
        // Filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(WakevTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(WakevTheme.spacing.medium))
            
            AlbumFilter.entries.forEach { filter ->
                FilterChip(
                    selected = filterParams.filter == filter,
                    onClick = { onFilterChange(filter) },
                    label = { Text(filter.displayName) },
                    leadingIcon = if (filterParams.filter == filter) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }
                    } else null
                )
            }
            
            // Clear filters button
            if (filterParams.filter != AlbumFilter.ALL) {
                TextButton(onClick = onClearFilters) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear")
                }
            }
            
            Spacer(modifier = Modifier.width(WakevTheme.spacing.medium))
        }
        
        // Sorting dropdown
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = WakevTheme.spacing.medium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${albums.size} albums",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Box {
                OutlinedButton(
                    onClick = { onShowSortingMenu(true) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(filterParams.sorting.displayName)
                }
                
                DropdownMenu(
                    expanded = showSortingMenu,
                    onDismissRequest = { onShowSortingMenu(false) }
                ) {
                    AlbumSorting.entries.forEach { sorting ->
                        DropdownMenuItem(
                            text = { Text(sorting.displayName) },
                            onClick = {
                                onSortingChange(sorting)
                                onShowSortingMenu(false)
                            },
                            leadingIcon = {
                                if (filterParams.sorting == sorting) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Grid of album cards.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlbumsGrid(
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit,
    onFavoriteClick: (Album) -> Unit,
    onDeleteClick: (Album) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        contentPadding = PaddingValues(WakevTheme.spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(WakevTheme.spacing.small),
        verticalArrangement = Arrangement.spacedBy(WakevTheme.spacing.small),
        modifier = Modifier.fillMaxSize()
    ) {
        items(albums, key = { it.id }) { album ->
            SmartAlbumCard(
                album = album,
                onClick = { onAlbumClick(album) },
                onFavoriteClick = { onFavoriteClick(album) },
                onDeleteClick = { onDeleteClick(album) },
                modifier = Modifier.animateItemPlacement()
            )
        }
    }
}

/**
 * Smart album card with cover, metadata, and actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SmartAlbumCard(
    album: Album,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 300f
        ),
        label = "scale"
    )
    
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .scale(scale)
            .clickable(
                onClick = onClick,
                onPress = { isPressed = it },
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Cover photo
            AsyncImage(
                model = album.coverPhotoId,
                contentDescription = album.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
            )
            
            // Auto-generated badge
            if (album.isAutoGenerated) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(WakevTheme.spacing.small),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Auto-generated",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
            
            // Photo count
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(WakevTheme.spacing.small),
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.5f)
            ) {
                Text(
                    text = "${album.photoIds.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            
            // Album info
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(WakevTheme.spacing.small)
            ) {
                Text(
                    text = album.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatDate(album.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            
            // Favorite button
            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(WakevTheme.spacing.medium)
            ) {
                Icon(
                    imageVector = if (album.isAutoGenerated) Icons.Default.FavoriteBorder else Icons.Default.Favorite,
                    contentDescription = "Toggle favorite",
                    tint = Color.White
                )
            }
            
            // Menu button
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Album options",
                    tint = Color.White
                )
            }
            
            // Menu dropdown
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Share") },
                    leadingIcon = { Icon(Icons.Default.Share, null) },
                    onClick = {
                        showMenu = false
                        // Handle share
                    }
                )
                DropdownMenuItem(
                    text = { Text("Edit") },
                    leadingIcon = { Icon(Icons.Default.Edit, null) },
                    onClick = {
                        showMenu = false
                        // Handle edit
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    leadingIcon = { Icon(Icons.Default.Delete, null) },
                    onClick = {
                        showMenu = false
                        onDeleteClick()
                    }
                )
            }
        }
    }
}

/**
 * Empty state when no albums match the filter.
 */
@Composable
private fun EmptyAlbumsState(
    filterParams: AlbumFilterParams,
    onClearFilters: () -> Unit,
    onCreateClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(WakevTheme.spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.PhotoAlbum,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        
        Spacer(modifier = Modifier.height(WakevTheme.spacing.large))
        
        Text(
            text = when (filterParams.filter) {
                AlbumFilter.RECENT -> "No recent albums"
                AlbumFilter.FAVORITES -> "No favorite albums"
                AlbumFilter.TAGS -> "No albums with these tags"
                AlbumFilter.DATE_RANGE -> "No albums in this date range"
                AlbumFilter.ALL -> "No albums yet"
            },
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.height(WakevTheme.spacing.small))
        
        if (filterParams.filter != AlbumFilter.ALL) {
            TextButton(onClick = onClearFilters) {
                Text("Clear filters")
            }
        } else {
            Text(
                text = "Create your first album to get started",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            
            Spacer(modifier = Modifier.height(WakevTheme.spacing.medium))
            
            Button(onClick = onCreateClick) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Album")
            }
        }
    }
}

/**
 * Error state when loading fails.
 */
@Composable
private fun ErrorAlbumsState(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(WakevTheme.spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(WakevTheme.spacing.medium))
        
        Text(
            text = "Error loading albums",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(WakevTheme.spacing.small))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(WakevTheme.spacing.large))
        
        Button(onClick = onRetry) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Retry")
        }
    }
}

/**
 * Dialog for creating a new album.
 */
@Composable
private fun CreateAlbumDialog(
    onDismiss: () -> Unit,
    onCreate: (CreateAlbumParams) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var coverUri by remember { mutableStateOf<String?>(null) }
    var tags by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Album") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(WakevTheme.spacing.medium)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Album name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Tags (comma separated)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text(
                    text = "You can add photos after creating the album",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onCreate(
                            CreateAlbumParams(
                                name = name.trim(),
                                coverUri = coverUri,
                                tags = tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
                            )
                        )
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Bottom sheet for advanced filtering.
 */
@Composable
private fun FilterBottomSheetContent(
    filterParams: AlbumFilterParams,
    onFilterChange: (AlbumFilter) -> Unit,
    onDateRangeChange: (String?, String?) -> Unit,
    onTagsChange: (List<String>) -> Unit,
    onApply: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(WakevTheme.spacing.large),
        verticalArrangement = Arrangement.spacedBy(WakevTheme.spacing.medium)
    ) {
        Text(
            text = "Filter Albums",
            style = MaterialTheme.typography.titleLarge
        )
        
        // Filter type selection
        Text(
            text = "Filter by",
            style = MaterialTheme.typography.titleSmall
        )
        
        AlbumFilter.entries.forEach { filter ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onFilterChange(filter) }
                    .padding(vertical = WakevTheme.spacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = filterParams.filter == filter,
                    onClick = { onFilterChange(filter) }
                )
                Spacer(modifier = Modifier.width(WakevTheme.spacing.small))
                Text(
                    text = filter.displayName,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        
        // Date range input (if DATE_RANGE selected)
        if (filterParams.filter == AlbumFilter.DATE_RANGE) {
            Text(
                text = "Date Range",
                style = MaterialTheme.typography.titleSmall
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(WakevTheme.spacing.small)
            ) {
                OutlinedTextField(
                    value = filterParams.startDate ?: "",
                    onValueChange = { onDateRangeChange(it, filterParams.endDate) },
                    label = { Text("Start") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = filterParams.endDate ?: "",
                    onValueChange = { onDateRangeChange(filterParams.startDate, it) },
                    label = { Text("End") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        // Tags input (if TAGS selected)
        if (filterParams.filter == AlbumFilter.TAGS) {
            Text(
                text = "Tags to filter",
                style = MaterialTheme.typography.titleSmall
            )
            OutlinedTextField(
                value = filterParams.tags.joinToString(", "),
                onValueChange = { onTagsChange(it.split(",").map { t -> t.trim() }.filter { t -> t.isNotBlank() }) },
                label = { Text("Tags (comma separated)") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        Spacer(modifier = Modifier.height(WakevTheme.spacing.medium))
        
        Button(
            onClick = onApply,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Apply Filters")
        }
        
        Spacer(modifier = Modifier.height(WakevTheme.spacing.large))
    }
}

/**
 * Format ISO 8601 date to readable string.
 */
private fun formatDate(isoDate: String): String {
    return try {
        val instant = java.time.Instant.parse(isoDate)
        val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy")
        formatter.format(instant.atZone(java.time.ZoneId.systemDefault()))
    } catch (e: Exception) {
        isoDate
    }
}
