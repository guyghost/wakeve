package com.guyghost.wakeve.ui.photo

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.guyghost.wakeve.models.Album
import com.guyghost.wakeve.ui.theme.WakevTheme
import com.guyghost.wakeve.ui.theme.spacing

/**
 * Smart Albums screen displaying auto-generated and custom albums in a grid layout.
 *
 * Features:
 * - Material You design with dynamic colors
 * - Smart Grid layout with adaptive columns
 * - Animated album cards with hover effects
 * - Quick actions (share, delete, edit)
 * - Album badges for auto-generated albums
 * - Photo count indicators
 * - Gradient overlays for text readability
 *
 * @param albums List of albums to display
 * @param onAlbumClick Callback when an album is clicked
 * @param onAlbumShare Callback when share action is triggered
 * @param onAlbumDelete Callback when delete action is triggered
 * @param onAlbumEdit Callback when edit action is triggered
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SmartAlbumsScreen(
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit,
    onAlbumShare: (Album) -> Unit = {},
    onAlbumDelete: (Album) -> Unit = {},
    onAlbumEdit: (Album) -> Unit = {}
) {
    var selectedAlbum by remember { mutableStateOf<Album?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Albums",
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                actions = {
                    IconButton(onClick = { /* TODO: Create new album */ }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create new album"
                        )
                    }
                    IconButton(onClick = { /* TODO: Filter albums */ }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter albums"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        if (albums.isEmpty()) {
            // Empty state
            EmptyAlbumsState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            // Albums grid
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                contentPadding = PaddingValues(WakevTheme.spacing.medium),
                horizontalArrangement = Arrangement.spacedBy(WakevTheme.spacing.small),
                verticalArrangement = Arrangement.spacedBy(WakevTheme.spacing.small),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(albums, key = { it.id }) { album ->
                    AlbumCard(
                        album = album,
                        onClick = { onAlbumClick(album) },
                        onShare = { onAlbumShare(album) },
                        onDelete = { onAlbumDelete(album) },
                        onEdit = { onAlbumEdit(album) },
                        modifier = Modifier.animateItemPlacement()
                    )
                }
            }
        }
    }
}

/**
 * Album card displaying cover photo, name, and metadata.
 *
 * Features:
 * - Animated scale on hover/press
 * - Gradient overlay for text readability
 * - Badge for auto-generated albums
 * - Photo count indicator
 * - Quick action menu
 */
@Composable
private fun AlbumCard(
    album: Album,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
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

    val backgroundColor by animateColorAsState(
        targetValue = if (isPressed) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "backgroundColor"
    )

    Card(
        modifier = modifier
            .aspectRatio(1f)
            .scale(scale)
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        Box {
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
                    text = "${album.photoCount()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Album info (bottom)
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
            }

            // Quick action menu button
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
        }

        // Album menu
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            DropdownMenuItem(
                text = { Text("Share") },
                leadingIcon = {
                    Icon(Icons.Default.Share, contentDescription = null)
                },
                onClick = {
                    showMenu = false
                    onShare()
                }
            )
            DropdownMenuItem(
                text = { Text("Edit") },
                leadingIcon = {
                    Icon(Icons.Default.Edit, contentDescription = null)
                },
                onClick = {
                    showMenu = false
                    onEdit()
                }
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                leadingIcon = {
                    Icon(Icons.Default.Delete, contentDescription = null)
                },
                onClick = {
                    showMenu = false
                    onDelete()
                }
            )
        }
    }
}

/**
 * Empty state displayed when no albums are available.
 */
@Composable
private fun EmptyAlbumsState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.PhotoAlbum,
            contentDescription = null,
            modifier = Modifier
                .size(120.dp)
                .padding(bottom = WakevTheme.spacing.large),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Text(
            text = "No albums yet",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(WakevTheme.spacing.small))
        Text(
            text = "Photos will be automatically organized into albums",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}
