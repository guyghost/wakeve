package com.guyghost.wakeve

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.presentation.state.EventManagementContract
import com.guyghost.wakeve.viewmodel.EventManagementViewModel
import com.guyghost.wakeve.theme.HomeBackgroundDark
import com.guyghost.wakeve.theme.HomeBackgroundLight
import com.guyghost.wakeve.theme.HomeGradientBlue
import com.guyghost.wakeve.theme.HomeGradientGreen
import com.guyghost.wakeve.theme.HomeGradientOrange
import com.guyghost.wakeve.theme.HomeGradientTeal
import com.guyghost.wakeve.theme.HomeTextPrimaryDark
import com.guyghost.wakeve.theme.HomeTextPrimaryLight
import com.guyghost.wakeve.theme.HomeTextSecondaryDark
import com.guyghost.wakeve.theme.HomeTextSecondaryLight
import kotlinx.datetime.toLocalDateTime

enum class HomeEventFilter(
    val label: String,
    val icon: ImageVector
) {
    UPCOMING("À venir", Icons.Outlined.CalendarToday),
    PAST("Évènements passés", Icons.Outlined.History),
    DRAFTS("Brouillons", Icons.Outlined.Edit),
    ORGANIZED_BY_ME("Organisés par moi", Icons.Outlined.Star),
    CONFIRMED("Confirmés", Icons.Outlined.CheckCircle)
}

data class EventTheme(
    val backgroundColor: Color,
    val emojis: List<String>,
    val emojiPositions: List<Pair<Float, Float>>
) {
    companion object {
        val Beach = EventTheme(
            backgroundColor = Color(0xFF7DD3C0),
            emojis = listOf("🏖️", "☀️", "🐚", "🐠", "🚤"),
            emojiPositions = listOf(
                0.5f to 0.4f,
                0.85f to 0.15f,
                0.75f to 0.75f,
                0.15f to 0.8f,
                0.1f to 0.55f
            )
        )
        
        val Party = EventTheme(
            backgroundColor = Color(0xFFC084FC),
            emojis = listOf("🎉", "🎈", "🎊", "🎁", "🎂"),
            emojiPositions = listOf(
                0.5f to 0.35f, 0.8f to 0.2f, 0.2f to 0.25f, 0.75f to 0.7f, 0.25f to 0.75f
            )
        )
        
        val Dinner = EventTheme(
            backgroundColor = Color(0xFFFB923C),
            emojis = listOf("🍽️", "🍷", "🥗", "🍝", "🥂"),
            emojiPositions = listOf(
                0.5f to 0.4f, 0.75f to 0.2f, 0.25f to 0.3f, 0.7f to 0.75f, 0.2f to 0.7f
            )
        )
        
        val Sport = EventTheme(
            backgroundColor = Color(0xFF34D399),
            emojis = listOf("⚽", "🏆", "🥅", "👟", "🎯"),
            emojiPositions = listOf(
                0.5f to 0.4f, 0.8f to 0.15f, 0.2f to 0.2f, 0.75f to 0.8f, 0.15f to 0.75f
            )
        )
        
        val Travel = EventTheme(
            backgroundColor = Color(0xFF60A5FA),
            emojis = listOf("✈️", "🗺️", "🧳", "📸", "🏔️"),
            emojiPositions = listOf(
                0.5f to 0.35f, 0.75f to 0.2f, 0.25f to 0.3f, 0.8f to 0.75f, 0.2f to 0.8f
            )
        )
        
        val Default = EventTheme(
            backgroundColor = Color(0xFF94A3B8),
            emojis = listOf("📅", "✨", "🎊", "🎈", "🎉"),
            emojiPositions = listOf(
                0.5f to 0.4f, 0.8f to 0.2f, 0.2f to 0.25f, 0.75f to 0.75f, 0.25f to 0.8f
            )
        )
        
        fun forEventType(eventType: String?): EventTheme {
            val type = eventType?.lowercase() ?: return Default
            return when {
                type.contains("beach") || type.contains("plage") || type.contains("sea") -> Beach
                type.contains("party") || type.contains("fête") || type.contains("birthday") -> Party
                type.contains("dinner") || type.contains("dîner") || type.contains("restaurant") -> Dinner
                type.contains("sport") || type.contains("football") || type.contains("match") -> Sport
                type.contains("travel") || type.contains("voyage") || type.contains("trip") -> Travel
                else -> Default
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: EventManagementViewModel,
    onNavigateTo: (String) -> Unit = {},
    onShowToast: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = true
) {
    val state by viewModel.state.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    var showFilterDropdown by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf(HomeEventFilter.UPCOMING) }

    LaunchedEffect(Unit) {
        viewModel.dispatch(EventManagementContract.Intent.LoadEvents)
    }

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is EventManagementContract.SideEffect.NavigateTo -> onNavigateTo(effect.route)
                is EventManagementContract.SideEffect.ShowToast -> onShowToast(effect.message)
                is EventManagementContract.SideEffect.NavigateBack -> {}
            }
        }
    }

    val backgroundColor = MaterialTheme.colorScheme.background

    val filteredEvents = remember(state.events, selectedFilter) {
        filterEvents(state.events, selectedFilter)
    }

    val draftCount = remember(state.events) {
        state.events.count { it.status == EventStatus.DRAFT }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showFilterDropdown = true }
                    ) {
                        Text(
                            text = selectedFilter.label,
                            style = MaterialTheme.typography.headlineMedium,
                            color = if (isDarkTheme) HomeTextPrimaryDark else HomeTextPrimaryLight
                        )
                        Icon(
                            imageVector = if (showFilterDropdown) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Filtrer",
                            tint = if (isDarkTheme) HomeTextPrimaryDark else HomeTextPrimaryLight,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onNavigateTo("event_creation") },
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = if (isDarkTheme) Color(0xFF2A2A2A) else Color(0xFFE2E8F0),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Créer un événement",
                            tint = if (isDarkTheme) HomeTextPrimaryDark else HomeTextPrimaryLight,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Surface(
                        modifier = Modifier
                            .size(40.dp)
                            .clickable { showMenu = true },
                        shape = CircleShape,
                        color = HomeGradientOrange
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "U",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor,
                    titleContentColor = if (isDarkTheme) HomeTextPrimaryDark else HomeTextPrimaryLight
                ),
                modifier = Modifier.statusBarsPadding()
            )
        },
        containerColor = backgroundColor
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(backgroundColor)
        ) {
            if (state.isLoading && state.events.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                if (state.hasError && filteredEvents.isEmpty()) {
                    ErrorState(
                        error = state.error ?: "Une erreur s'est produite",
                        isDarkTheme = isDarkTheme,
                        onRetry = {
                            viewModel.dispatch(EventManagementContract.Intent.LoadEvents)
                            viewModel.clearError()
                        },
                        onDismiss = { viewModel.clearError() }
                    )
                } else if (filteredEvents.isEmpty()) {
                    EmptyState(
                        isDarkTheme = isDarkTheme,
                        onCreateEvent = { onNavigateTo("event_creation") },
                        title = getEmptyStateTitle(selectedFilter),
                        subtitle = getEmptyStateSubtitle(selectedFilter)
                    )
                } else {
                    EventsCarousel(
                        events = filteredEvents,
                        isDarkTheme = isDarkTheme,
                        userId = "currentUser", // TODO: Replace with actual user ID
                        onEventClick = { event ->
                            viewModel.dispatch(EventManagementContract.Intent.SelectEvent(event.id))
                        }
                    )
                }
            }

            if (showFilterDropdown) {
                FilterDropdownMenu(
                    selectedFilter = selectedFilter,
                    onFilterSelected = { 
                        selectedFilter = it
                        showFilterDropdown = false
                    },
                    onDismiss = { showFilterDropdown = false },
                    isDarkTheme = isDarkTheme,
                    draftCount = draftCount
                )
            }
        }
    }

    if (showMenu) {
        UserMenu(
            onDismiss = { showMenu = false },
            onSignOut = { }
        )
    }
}

private fun filterEvents(events: List<Event>, filter: HomeEventFilter): List<Event> {
    return when (filter) {
        HomeEventFilter.UPCOMING -> events.filter { it.status != EventStatus.FINALIZED && it.status != EventStatus.DRAFT }
        HomeEventFilter.PAST -> events.filter { it.status == EventStatus.FINALIZED }
        HomeEventFilter.DRAFTS -> events.filter { it.status == EventStatus.DRAFT }
        HomeEventFilter.ORGANIZED_BY_ME -> events.filter { it.organizerId == "currentUser" }
        HomeEventFilter.CONFIRMED -> events.filter { it.status == EventStatus.CONFIRMED }
    }
}

private fun getEmptyStateTitle(filter: HomeEventFilter): String = when (filter) {
    HomeEventFilter.UPCOMING -> "Aucun évènement à venir"
    HomeEventFilter.PAST -> "Aucun évènement passé"
    HomeEventFilter.DRAFTS -> "Aucun brouillon"
    HomeEventFilter.ORGANIZED_BY_ME -> "Aucun évènement organisé"
    HomeEventFilter.CONFIRMED -> "Aucun évènement confirmé"
}

private fun getEmptyStateSubtitle(filter: HomeEventFilter): String = when (filter) {
    HomeEventFilter.UPCOMING -> "Les évènements à venir apparaîtront ici, que vous les organisiez ou non."
    HomeEventFilter.PAST -> "Les évènements passés apparaîtront ici."
    HomeEventFilter.DRAFTS -> "Vos brouillons d'évènements apparaîtront ici."
    HomeEventFilter.ORGANIZED_BY_ME -> "Les évènements que vous organisez apparaîtront ici."
    HomeEventFilter.CONFIRMED -> "Les évènements confirmés apparaîtront ici."
}

@Composable
private fun FilterDropdownMenu(
    selectedFilter: HomeEventFilter,
    onFilterSelected: (HomeEventFilter) -> Unit,
    onDismiss: () -> Unit,
    isDarkTheme: Boolean,
    draftCount: Int
) {
    val backgroundColor = if (isDarkTheme) Color(0xFF1E293B) else Color.White
    val textColor = if (isDarkTheme) HomeTextPrimaryDark else HomeTextPrimaryLight
    val iconColor = if (isDarkTheme) HomeTextPrimaryDark else HomeTextPrimaryLight

    Popup(
        alignment = Alignment.TopStart,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .width(280.dp)
                .padding(top = 60.dp, start = 16.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                HomeEventFilter.entries.forEachIndexed { index, filter ->
                    val isSelected = filter == selectedFilter
                    val showBadge = filter == HomeEventFilter.DRAFTS && draftCount > 0

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onFilterSelected(filter) }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Box(modifier = Modifier.width(24.dp)) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = textColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Icon(
                            imageVector = filter.icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = if (showBadge) "${filter.label} ($draftCount)" else filter.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = textColor
                        )
                    }

                    if (index == 1) {
                        Divider(
                            color = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EventsCarousel(
    events: List<Event>,
    isDarkTheme: Boolean,
    userId: String,
    onEventClick: (Event) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(events) { event ->
            VisualEventCard(
                event = event,
                isOrganizer = event.organizerId == userId,
                onClick = { onEventClick(event) }
            )
        }
    }
}

@Composable
fun VisualEventCard(
    event: Event,
    isOrganizer: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = remember(event.eventType) {
        EventTheme.forEventType(event.eventType?.name)
    }

    Card(
        onClick = onClick,
        modifier = modifier
            .width(360.dp)
            .height(640.dp),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = theme.backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Emoji decorations
            theme.emojis.forEachIndexed { index, emoji ->
                if (index < theme.emojiPositions.size) {
                    val (x, y) = theme.emojiPositions[index]
                    Text(
                        text = emoji,
                        fontSize = 72.sp,
                        modifier = Modifier
                            .padding(
                                start = (x * 360).dp,
                                top = (y * 640).dp
                            )
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Badge
                if (isOrganizer) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color.White.copy(alpha = 0.25f),
                            shape = CircleShape
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "👑",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Organisé par moi",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(20.dp))
                }

                Spacer(modifier = Modifier.weight(1f))

                // Title
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White,
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 32.dp)
                        .fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun ErrorState(
    error: String,
    isDarkTheme: Boolean,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textColor = if (isDarkTheme) HomeTextPrimaryDark else HomeTextPrimaryLight
    val textSecondaryColor = if (isDarkTheme) HomeTextSecondaryDark else HomeTextSecondaryLight

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Une erreur s'est produite",
            style = MaterialTheme.typography.headlineSmall,
            color = textColor,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = textSecondaryColor,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(0.7f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            ) {
                Text("Fermer")
            }
            Button(
                onClick = onRetry,
                modifier = Modifier.weight(1f)
            ) {
                Text("Réessayer")
            }
        }
    }
}

@Composable
fun EmptyState(
    isDarkTheme: Boolean,
    onCreateEvent: () -> Unit,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val textSecondaryColor = MaterialTheme.colorScheme.onSurfaceVariant
    val iconColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.CalendarToday,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = iconColor
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = textColor,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = textSecondaryColor,
                textAlign = TextAlign.Center
            )
        }
        
        Button(
            onClick = onCreateEvent,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp)
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            )
        ) {
            Text(
                text = "Créer un évènement",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Black
            )
        }
    }
}

@Composable
fun UserMenu(
    onDismiss: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onSignOut) {
                Text("Se déconnecter")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        },
        title = { Text("Menu utilisateur") },
        text = { Text("Voulez-vous vous déconnecter ?") }
    )
}

private fun formatDate(isoDate: String): String {
    return try {
        val date = kotlinx.datetime.Instant.parse(isoDate)
        val local = date.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
        "${local.dayOfMonth} ${local.month.name.lowercase().take(3)}"
    } catch (e: Exception) {
        isoDate.take(10)
    }
}

@Composable
fun StatusChip(
    status: EventStatus,
    isDarkTheme: Boolean = true,
    modifier: Modifier = Modifier
) {
    val (label, color) = when (status) {
        EventStatus.DRAFT -> "Brouillon" to Color(0xFF64748B)
        EventStatus.POLLING -> "En sondage" to Color(0xFF3B82F6)
        EventStatus.COMPARING -> "Comparaison" to Color(0xFF8B5CF6)
        EventStatus.CONFIRMED -> "Confirmé" to Color(0xFF10B981)
        EventStatus.ORGANIZING -> "Organisation" to Color(0xFFF59E0B)
        EventStatus.FINALIZED -> "Finalisé" to Color(0xFFEC4899)
    }

    Surface(
        color = if (isDarkTheme) color.copy(alpha = 0.2f) else color.copy(alpha = 0.1f),
        modifier = modifier.clip(RoundedCornerShape(8.dp))
    ) {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
