package com.guyghost.wakeve.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guyghost.wakeve.gamification.LeaderboardEntry
import com.guyghost.wakeve.gamification.LeaderboardType
import com.guyghost.wakeve.viewmodel.ProfileViewModel

/**
 * Standalone Leaderboard screen with filtering and pull-to-refresh.
 * Shows top users ranked by points with avatar, name, points, and badge count.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Leaderboard,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Classement",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                viewModel.loadProfileData()
                isRefreshing = false
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    // Filter tabs
                    item {
                        LeaderboardFilterTabs(
                            selectedTab = uiState.selectedLeaderboardTab,
                            onTabSelected = { viewModel.selectLeaderboardTab(it) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Podium for top 3
                    if (uiState.leaderboard.size >= 3) {
                        item {
                            LeaderboardPodium(
                                top3 = uiState.leaderboard.take(3),
                                currentUserId = uiState.currentUserId
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Remaining entries
                    val remainingEntries = if (uiState.leaderboard.size > 3) {
                        uiState.leaderboard.drop(3)
                    } else {
                        uiState.leaderboard
                    }

                    items(
                        items = remainingEntries,
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
}

/**
 * Leaderboard filter tabs: Global, Per-event, Friends.
 */
@Composable
fun LeaderboardFilterTabs(
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
                        text = getLeaderboardFilterName(tab),
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

/**
 * Podium view for the top 3 leaderboard entries.
 */
@Composable
fun LeaderboardPodium(
    top3: List<LeaderboardEntry>,
    currentUserId: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        // 2nd place (left)
        if (top3.size > 1) {
            PodiumEntry(
                entry = top3[1],
                isCurrentUser = top3[1].userId == currentUserId,
                podiumHeight = 80.dp,
                medalColor = Color(0xFFC0C0C0), // Silver
                medalEmoji = "\uD83E\uDD48"
            )
        }

        // 1st place (center, tallest)
        if (top3.isNotEmpty()) {
            PodiumEntry(
                entry = top3[0],
                isCurrentUser = top3[0].userId == currentUserId,
                podiumHeight = 100.dp,
                medalColor = Color(0xFFFFD700), // Gold
                medalEmoji = "\uD83E\uDD47"
            )
        }

        // 3rd place (right)
        if (top3.size > 2) {
            PodiumEntry(
                entry = top3[2],
                isCurrentUser = top3[2].userId == currentUserId,
                podiumHeight = 60.dp,
                medalColor = Color(0xFFCD7F32), // Bronze
                medalEmoji = "\uD83E\uDD49"
            )
        }
    }
}

/**
 * Individual podium entry for top 3 display.
 */
@Composable
fun PodiumEntry(
    entry: LeaderboardEntry,
    isCurrentUser: Boolean,
    podiumHeight: androidx.compose.ui.unit.Dp,
    medalColor: Color,
    medalEmoji: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.width(100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Medal
        Text(
            text = medalEmoji,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        // Avatar circle
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(medalColor, medalColor.copy(alpha = 0.6f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = entry.username.first().uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Name
        Text(
            text = if (isCurrentUser) "Vous" else entry.username.split(" ").first(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1
        )

        // Points
        Text(
            text = formatPoints(entry.totalPoints),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        // Badge count
        Text(
            text = "${entry.badgesCount} badges",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        // Podium bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(podiumHeight)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(medalColor.copy(alpha = 0.3f))
        )
    }
}

private fun getLeaderboardFilterName(type: LeaderboardType): String {
    return when (type) {
        LeaderboardType.ALL_TIME -> "Global"
        LeaderboardType.THIS_MONTH -> "Ce mois"
        LeaderboardType.THIS_WEEK -> "Cette semaine"
        LeaderboardType.FRIENDS -> "Amis"
    }
}

private fun formatPoints(points: Int): String {
    return when {
        points >= 1000000 -> "${points / 1000000}M"
        points >= 1000 -> "${points / 1000}k"
        else -> points.toString()
    }
}
