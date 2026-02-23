package com.guyghost.wakeve

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.guyghost.wakeve.models.EventSearchResult
import com.guyghost.wakeve.models.SearchResultsResponse
import com.guyghost.wakeve.models.TrendingEventsResponse
import com.guyghost.wakeve.models.RecommendedEventsResponse
import kotlinx.coroutines.launch

/**
 * Explore Tab Screen - Search, discover, and browse events.
 *
 * Features:
 * - Search bar with real-time filtering
 * - Category filter chips (horizontal scroll)
 * - Sections: "Tendances", "Pres de vous", "Recommandes pour vous"
 * - Pull-to-refresh
 * - Empty state with illustration
 *
 * Matches iOS ExploreTabView with Material You design.
 *
 * @param onEventClick Callback when user clicks on an event (eventId)
 * @param repository The DatabaseEventRepository for search/discovery queries
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreTabScreen(
    onEventClick: (String) -> Unit,
    repository: DatabaseEventRepository? = null
) {
    var searchText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ExploreCategoryItem.ALL) }
    var isLoading by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    var trendingEvents by remember { mutableStateOf<List<EventSearchResult>>(emptyList()) }
    var recommendedEvents by remember { mutableStateOf<List<EventSearchResult>>(emptyList()) }
    var searchResults by remember { mutableStateOf<List<EventSearchResult>>(emptyList()) }

    val isSearching = searchText.isNotBlank()
    val scope = rememberCoroutineScope()

    // Load data function
    fun loadData() {
        if (repository == null) return
        scope.launch {
            isLoading = true
            try {
                val trending = repository.getTrendingEvents(limit = 10)
                trendingEvents = trending.events
                val recommended = repository.getRecommendedEvents(userId = "current_user", limit = 10)
                recommendedEvents = recommended.events
            } catch (_: Exception) {
                // Silently handle errors for now
            }
            isLoading = false
        }
    }

    // Search function
    fun performSearch() {
        if (repository == null) return
        scope.launch {
            try {
                val categoryFilter = if (selectedCategory == ExploreCategoryItem.ALL) null
                    else selectedCategory.eventTypes.firstOrNull()
                val results = repository.searchEvents(
                    query = if (searchText.isBlank()) null else searchText.trim(),
                    category = categoryFilter,
                    location = null,
                    dateFrom = null,
                    dateTo = null,
                    status = null,
                    sortBy = "RELEVANCE",
                    offset = 0,
                    limit = 30
                )
                searchResults = results.events
            } catch (_: Exception) {
                searchResults = emptyList()
            }
        }
    }

    // Initial load
    LaunchedEffect(Unit) {
        loadData()
    }

    // Re-search on text/category change
    LaunchedEffect(searchText, selectedCategory) {
        if (isSearching) {
            performSearch()
        } else {
            searchResults = emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Explorer",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                scope.launch {
                    loadData()
                    if (isSearching) performSearch()
                    isRefreshing = false
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Search bar
                item {
                    ExploreSearchBar(
                        searchText = searchText,
                        onSearchTextChange = { searchText = it },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                // Category chips
                item {
                    ExploreCategoryChips(
                        selectedCategory = selectedCategory,
                        onCategorySelected = { selectedCategory = it }
                    )
                }

                // Loading state
                if (isLoading && trendingEvents.isEmpty() && !isSearching) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Chargement...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }

                // Search results mode
                if (isSearching) {
                    if (searchResults.isEmpty()) {
                        item {
                            ExploreSearchEmptyState(searchText = searchText)
                        }
                    } else {
                        item {
                            Text(
                                text = "${searchResults.size} resultats",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        items(searchResults, key = { it.id }) { event ->
                            SearchResultCard(
                                event = event,
                                onClick = { onEventClick(event.id) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                    }
                } else if (!isLoading) {
                    // Discovery mode

                    // Empty state
                    if (trendingEvents.isEmpty() && recommendedEvents.isEmpty()) {
                        item { ExploreDiscoveryEmptyState() }
                    }

                    // Trending section
                    if (trendingEvents.isNotEmpty()) {
                        item {
                            ExploreSectionHeader(
                                title = "Tendances",
                                icon = Icons.Default.Favorite,
                                iconTint = Color(0xFFFF6B35)
                            )
                        }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(trendingEvents, key = { it.id }) { event ->
                                    ExploreEventCard(
                                        event = event,
                                        onClick = { onEventClick(event.id) }
                                    )
                                }
                            }
                        }
                    }

                    // Recommended section
                    if (recommendedEvents.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            ExploreSectionHeader(
                                title = "Recommandes pour vous",
                                icon = Icons.Default.Star,
                                iconTint = MaterialTheme.colorScheme.primary
                            )
                        }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(recommendedEvents, key = { it.id }) { event ->
                                    ExploreEventCard(
                                        event = event,
                                        onClick = { onEventClick(event.id) }
                                    )
                                }
                            }
                        }
                    }

                    // Event ideas section (always shown)
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        ExploreSectionHeader(
                            title = "Idees d'evenements",
                            icon = Icons.Default.Info,
                            iconTint = Color(0xFFFFC107)
                        )
                    }
                    items(eventTemplates) { template ->
                        TemplateCard(template = template, onClick = {
                            // TODO: Create event from template
                        })
                    }

                    // Planning tips
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        ExploreSectionHeader(
                            title = "Conseils de planification",
                            icon = Icons.Default.Info,
                            iconTint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    items(planningTips) { tip ->
                        TipCard(tip = tip)
                    }
                }
            }
        }
    }
}

// MARK: - Search Bar

@Composable
private fun ExploreSearchBar(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = searchText,
        onValueChange = onSearchTextChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = {
            Text("Rechercher un evenement...")
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Rechercher",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    )
}

// MARK: - Category Chips

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExploreCategoryChips(
    selectedCategory: ExploreCategoryItem,
    onCategorySelected: (ExploreCategoryItem) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ExploreCategoryItem.entries.forEach { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = { Text(category.displayName) },
                leadingIcon = if (selectedCategory == category) {
                    {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else null
            )
        }
    }
}

// MARK: - Section Header

@Composable
private fun ExploreSectionHeader(
    title: String,
    icon: ImageVector,
    iconTint: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            )
        )
    }
}

// MARK: - Event Card (Horizontal Scroll)

@Composable
private fun ExploreEventCard(
    event: EventSearchResult,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(240.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Type badge row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = eventTypeIcon(event.eventType),
                    contentDescription = null,
                    tint = eventTypeColor(event.eventType),
                    modifier = Modifier.size(28.dp)
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = eventTypeColor(event.eventType).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = eventTypeDisplayName(event.eventType),
                        style = MaterialTheme.typography.labelSmall,
                        color = eventTypeColor(event.eventType),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Description
            Text(
                text = event.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Location
            event.locationName?.let { location ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = location,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Participants
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                val participantText = if (event.maxParticipants != null) {
                    "${event.participantCount}/${event.maxParticipants} participants"
                } else {
                    "${event.participantCount} participants"
                }
                Text(
                    text = participantText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// MARK: - Search Result Card

@Composable
private fun SearchResultCard(
    event: EventSearchResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = eventTypeColor(event.eventType).copy(alpha = 0.12f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = eventTypeIcon(event.eventType),
                        contentDescription = null,
                        tint = eventTypeColor(event.eventType),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "${event.participantCount} participants",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    event.locationName?.let { location ->
                        Text(
                            text = location,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = eventTypeDisplayName(event.eventType),
                        style = MaterialTheme.typography.labelSmall,
                        color = eventTypeColor(event.eventType),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// MARK: - Empty States

@Composable
private fun ExploreDiscoveryEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Rien a explorer pour le moment",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Les evenements apparaitront ici des qu'ils seront crees.\nCommencez par en creer un !",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 40.dp)
        )
    }
}

@Composable
private fun ExploreSearchEmptyState(searchText: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Aucun resultat pour \"$searchText\"",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Essayez avec d'autres mots-cles ou modifiez les filtres.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 40.dp)
        )
    }
}

// MARK: - Helper functions

@Composable
private fun eventTypeIcon(eventType: String): ImageVector {
    return when (eventType) {
        "BIRTHDAY" -> Icons.Default.Favorite
        "WEDDING" -> Icons.Default.Favorite
        "TEAM_BUILDING" -> Icons.Default.AccountCircle
        "CONFERENCE" -> Icons.Default.DateRange
        "PARTY" -> Icons.Default.Star
        "SPORTS_EVENT", "SPORT_EVENT" -> Icons.Default.Face
        "FAMILY_GATHERING" -> Icons.Default.Face
        "OUTDOOR_ACTIVITY" -> Icons.Default.Star
        "FOOD_TASTING" -> Icons.Default.Favorite
        "TECH_MEETUP" -> Icons.Default.Info
        "WELLNESS_EVENT" -> Icons.Default.Favorite
        "CREATIVE_WORKSHOP" -> Icons.Default.Star
        else -> Icons.Default.DateRange
    }
}

private fun eventTypeColor(eventType: String): Color {
    return when (eventType) {
        "BIRTHDAY", "PARTY" -> Color(0xFFE91E63)
        "WEDDING" -> Color(0xFFF44336)
        "TEAM_BUILDING", "CONFERENCE", "TECH_MEETUP" -> Color(0xFF2196F3)
        "WORKSHOP", "CREATIVE_WORKSHOP" -> Color(0xFF9C27B0)
        "SPORTS_EVENT", "SPORT_EVENT", "OUTDOOR_ACTIVITY" -> Color(0xFF4CAF50)
        "CULTURAL_EVENT" -> Color(0xFFFF9800)
        "FAMILY_GATHERING" -> Color(0xFFFFC107)
        "FOOD_TASTING" -> Color(0xFF795548)
        "WELLNESS_EVENT" -> Color(0xFF009688)
        else -> Color(0xFF607D8B)
    }
}

private fun eventTypeDisplayName(eventType: String): String {
    return when (eventType) {
        "BIRTHDAY" -> "Anniversaire"
        "WEDDING" -> "Mariage"
        "TEAM_BUILDING" -> "Team building"
        "CONFERENCE" -> "Conference"
        "WORKSHOP" -> "Atelier"
        "PARTY" -> "Soiree"
        "SPORTS_EVENT", "SPORT_EVENT" -> "Sport"
        "CULTURAL_EVENT" -> "Culture"
        "FAMILY_GATHERING" -> "Famille"
        "OUTDOOR_ACTIVITY" -> "Plein air"
        "FOOD_TASTING" -> "Gastronomie"
        "TECH_MEETUP" -> "Tech"
        "WELLNESS_EVENT" -> "Bien-etre"
        "CREATIVE_WORKSHOP" -> "Creatif"
        else -> "Autre"
    }
}

// MARK: - Category Enum

enum class ExploreCategoryItem(
    val displayName: String,
    val icon: ImageVector,
    val eventTypes: List<String>
) {
    ALL("Tout", Icons.Default.Star, emptyList()),
    SOCIAL("Social", Icons.Default.Favorite, listOf("BIRTHDAY", "WEDDING", "PARTY", "FAMILY_GATHERING")),
    SPORT("Sport", Icons.Default.Face, listOf("SPORTS_EVENT", "SPORT_EVENT", "OUTDOOR_ACTIVITY")),
    CULTURE("Culture", Icons.Default.Star, listOf("CULTURAL_EVENT", "CREATIVE_WORKSHOP")),
    PROFESSIONAL("Pro", Icons.Default.AccountCircle, listOf("TEAM_BUILDING", "CONFERENCE", "WORKSHOP", "TECH_MEETUP")),
    FOOD("Food", Icons.Default.Favorite, listOf("FOOD_TASTING")),
    WELLNESS("Bien-etre", Icons.Default.Favorite, listOf("WELLNESS_EVENT"))
}

// MARK: - Existing Components (Template & Tip Cards)

/**
 * Template card composable.
 */
@Composable
private fun TemplateCard(
    template: EventTemplate,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = template.icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = template.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Utiliser ce modele",
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

/**
 * Tip card composable.
 */
@Composable
private fun TipCard(tip: PlanningTip) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = tip.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tip.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/**
 * Event template data class.
 */
private data class EventTemplate(
    val name: String,
    val description: String,
    val icon: ImageVector
)

/**
 * Planning tip data class.
 */
private data class PlanningTip(
    val title: String,
    val content: String
)

/**
 * Sample event templates.
 */
private val eventTemplates = listOf(
    EventTemplate(
        name = "Week-end entre amis",
        description = "Escapade de 2-3 jours avec hebergement et activites",
        icon = Icons.Default.Favorite
    ),
    EventTemplate(
        name = "Reunion de famille",
        description = "Rassemblement familial avec repas et photos",
        icon = Icons.Default.Face
    ),
    EventTemplate(
        name = "Voyage de groupe",
        description = "Sejour de plusieurs jours avec transport et budget partage",
        icon = Icons.Default.DateRange
    ),
    EventTemplate(
        name = "Evenement sportif",
        description = "Competition ou sortie sportive collective",
        icon = Icons.Default.AccountCircle
    )
)

/**
 * Sample planning tips.
 */
private val planningTips = listOf(
    PlanningTip(
        title = "Proposez plusieurs creneaux",
        content = "Plus vous proposez de dates, plus il est facile de trouver un consensus."
    ),
    PlanningTip(
        title = "Definissez une echeance claire",
        content = "Une date limite de vote incite les participants a repondre rapidement."
    ),
    PlanningTip(
        title = "Creez des scenarios",
        content = "Comparez differentes options (destination, hebergement, activites) pour optimiser votre evenement."
    ),
    PlanningTip(
        title = "Partagez le budget tot",
        content = "Une transparence precoce sur les couts evite les mauvaises surprises."
    )
)
