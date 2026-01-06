package com.guyghost.wakeve.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guyghost.wakeve.gamification.Badge
import com.guyghost.wakeve.gamification.BadgeCategory
import com.guyghost.wakeve.gamification.BadgeRarity
import com.guyghost.wakeve.gamification.LeaderboardEntry
import com.guyghost.wakeve.gamification.LeaderboardType
import com.guyghost.wakeve.theme.BadgeCommon
import com.guyghost.wakeve.theme.BadgeEpic
import com.guyghost.wakeve.theme.BadgeLegendary
import com.guyghost.wakeve.theme.BadgeRare
import com.guyghost.wakeve.theme.PointsCommentColor
import com.guyghost.wakeve.theme.PointsCreationColor
import com.guyghost.wakeve.theme.PointsParticipationColor
import com.guyghost.wakeve.theme.PointsVotingColor
import com.guyghost.wakeve.viewmodel.ProfileViewModel

/**
 * Profile & Achievements screen with gamification data.
 * Displays user points, badges, and leaderboard rankings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error snackbar if needed
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Profil & Succès",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
            ) {
                // Points Summary Card
                item {
                    PointsSummaryCard(
                        totalPoints = uiState.userPoints?.totalPoints ?: 0,
                        eventCreationPoints = uiState.userPoints?.eventCreationPoints ?: 0,
                        votingPoints = uiState.userPoints?.votingPoints ?: 0,
                        commentPoints = uiState.userPoints?.commentPoints ?: 0,
                        participationPoints = uiState.userPoints?.participationPoints ?: 0
                    )
                }

                // Badges Section Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Succès",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "${uiState.userBadges.size} badges",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Badges by Category
                items(BadgeCategory.entries.toList(), key = { it.name }) { category ->
                    val categoryBadges = uiState.userBadges.filter { it.category == category }
                    if (categoryBadges.isNotEmpty()) {
                        BadgeCategorySection(
                            category = category,
                            badges = categoryBadges
                        )
                    }
                }

                // Leaderboard Section Header
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Leaderboard,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Classement",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                // Leaderboard Tabs
                item {
                    LeaderboardTabs(
                        selectedTab = uiState.selectedLeaderboardTab,
                        onTabSelected = { viewModel.selectLeaderboardTab(it) }
                    )
                }

                // Leaderboard List
                items(
                    items = uiState.leaderboard,
                    key = { it.userId }
                ) { entry ->
                    LeaderboardItem(
                        entry = entry,
                        isCurrentUser = entry.userId == uiState.currentUserId
                    )
                }

                // Bottom spacing
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

/**
 * Card displaying user's points summary with breakdown by category.
 */
@Composable
fun PointsSummaryCard(
    totalPoints: Int,
    eventCreationPoints: Int,
    votingPoints: Int,
    commentPoints: Int,
    participationPoints: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with total points
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Points Totaux",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatPoints(totalPoints),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Animated points icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

            // Points breakdown
            PointBreakdownRow(
                label = "Création d'événements",
                points = eventCreationPoints,
                color = PointsCreationColor
            )
            PointBreakdownRow(
                label = "Votes",
                points = votingPoints,
                color = PointsVotingColor
            )
            PointBreakdownRow(
                label = "Commentaires",
                points = commentPoints,
                color = PointsCommentColor
            )
            PointBreakdownRow(
                label = "Participation",
                points = participationPoints,
                color = PointsParticipationColor
            )
        }
    }
}

/**
 * Row displaying a single point category breakdown.
 */
@Composable
fun PointBreakdownRow(
    label: String,
    points: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = formatPoints(points),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

/**
 * Section displaying badges for a specific category.
 */
@Composable
fun BadgeCategorySection(
    category: BadgeCategory,
    badges: List<Badge>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = getCategoryDisplayName(category),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(badges, key = { it.id }) { badge ->
                BadgeItem(badge = badge)
            }
        }
    }
}

/**
 * Individual badge display item.
 */
@Composable
fun BadgeItem(
    badge: Badge,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = getRarityColor(badge.rarity).copy(alpha = 0.2f),
        animationSpec = tween(300),
        label = "badgeBackgroundColor"
    )

    val borderColor by animateColorAsState(
        targetValue = getRarityColor(badge.rarity),
        animationSpec = tween(300),
        label = "badgeBorderColor"
    )

    Card(
        modifier = modifier
            .width(110.dp)
            .height(130.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Badge icon
            Text(
                text = badge.icon,
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.size(40.dp)
            )

            // Badge name
            Text(
                text = badge.name,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Rarity indicator
            Text(
                text = badge.rarity.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelSmall,
                color = borderColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Leaderboard tabs for filtering by time period.
 */
@Composable
fun LeaderboardTabs(
    selectedTab: LeaderboardType,
    onTabSelected: (LeaderboardType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LeaderboardType.entries.forEach { tab ->
            FilterChip(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                label = {
                    Text(
                        text = getLeaderboardTabName(tab),
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.semantics {
                    contentDescription = "Leaderboard filter: ${getLeaderboardTabName(tab)}"
                }
            )
        }
    }
}

/**
 * Individual leaderboard entry item.
 */
@Composable
fun LeaderboardItem(
    entry: LeaderboardEntry,
    isCurrentUser: Boolean,
    modifier: Modifier = Modifier
) {
    val elevation by animateFloatAsState(
        targetValue = if (isCurrentUser) 4f else 0f,
        animationSpec = tween(300),
        label = "leaderboardItemElevation"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "${entry.username}, rank ${entry.rank}, ${entry.totalPoints} points"
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentUser)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank
            Text(
                text = "#${entry.rank}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = when (entry.rank) {
                    1 -> Color(0xFFFFD700) // Gold
                    2 -> Color(0xFFC0C0C0) // Silver
                    3 -> Color(0xFFCD7F32) // Bronze
                    else -> MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.width(40.dp)
            )

            // User info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = entry.username,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (entry.isFriend) {
                        Text(
                            text = "👥",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                Text(
                    text = "${entry.badgesCount} badges",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Points
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = formatPoints(entry.totalPoints),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "points",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Helper functions

private fun formatPoints(points: Int): String {
    return when {
        points >= 1000000 -> "${points / 1000000}M"
        points >= 1000 -> "${points / 1000}k"
        else -> points.toString()
    }
}

private fun getCategoryDisplayName(category: BadgeCategory): String {
    return when (category) {
        BadgeCategory.CREATION -> "Création"
        BadgeCategory.VOTING -> "Votes"
        BadgeCategory.PARTICIPATION -> "Participation"
        BadgeCategory.ENGAGEMENT -> "Engagement"
        BadgeCategory.SPECIAL -> "Spéciaux"
    }
}

private fun getLeaderboardTabName(type: LeaderboardType): String {
    return when (type) {
        LeaderboardType.ALL_TIME -> "Tout temps"
        LeaderboardType.THIS_MONTH -> "Ce mois"
        LeaderboardType.THIS_WEEK -> "Cette semaine"
        LeaderboardType.FRIENDS -> "Amis"
    }
}

private fun getRarityColor(rarity: BadgeRarity): Color {
    return when (rarity) {
        BadgeRarity.COMMON -> BadgeCommon
        BadgeRarity.RARE -> BadgeRare
        BadgeRarity.EPIC -> BadgeEpic
        BadgeRarity.LEGENDARY -> BadgeLegendary
    }
}
