package com.guyghost.wakeve.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.guyghost.wakeve.models.AISuggestion
import com.guyghost.wakeve.models.DateRecommendation
import com.guyghost.wakeve.models.RecommendationSummary

/**
 * AI Recommendation List - Displays a scrollable list of AI suggestions.
 *
 * Features:
 * - Animated item appearance
 * - Lazy loading with LazyColumn
 * - Empty state when no suggestions
 * - Loading state
 * - Summary header
 *
 * @param suggestions List of AI suggestions to display
 * @param modifier Modifier for the component
 * @param onAcceptRecommendation Callback when user accepts a recommendation
 * @param onDismissRecommendation Callback when user dismisses a recommendation
 * @param isLoading Whether suggestions are currently loading
 * @param summary Optional recommendation summary
 * @param emptyMessage Message to show when no suggestions are available
 */
@Composable
fun AIRecommendationList(
    suggestions: List<AISuggestion<DateRecommendation>>,
    modifier: Modifier = Modifier,
    onAcceptRecommendation: (String) -> Unit,
    onDismissRecommendation: (String) -> Unit,
    isLoading: Boolean = false,
    summary: RecommendationSummary? = null,
    emptyMessage: String = "No AI recommendations available"
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = if (isLoading) {
                    "Loading AI recommendations"
                } else if (suggestions.isEmpty()) {
                    emptyMessage
                } else {
                    "AI recommendations list. ${suggestions.size} suggestions available."
                }
            }
    ) {
        // Summary header (optional)
        if (summary != null) {
            RecommendationSummaryHeader(summary = summary)
        }

        // Content
        when {
            isLoading -> {
                LoadingState()
            }
            suggestions.isEmpty() -> {
                EmptyState(message = emptyMessage)
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(
                        items = suggestions,
                        key = { it.id }
                    ) { suggestion ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(
                                animationSpec = tween(
                                    durationMillis = 300,
                                    delayMillis = suggestions.indexOf(suggestion) * 50
                                )
                            ) + expandVertically(
                                animationSpec = tween(
                                    durationMillis = 300,
                                    delayMillis = suggestions.indexOf(suggestion) * 50
                                )
                            ),
                            exit = fadeOut(animationSpec = tween(200))
                        ) {
                            AISuggestionCard(
                                suggestion = suggestion,
                                onAccept = { onAcceptRecommendation(suggestion.id) },
                                onDismiss = { onDismissRecommendation(suggestion.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Header showing summary of recommendations.
 */
@Composable
private fun RecommendationSummaryHeader(
    summary: RecommendationSummary
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "AI Insights",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "${summary.totalRecommendations} recommendations • " +
                    "${summary.highConfidenceCount} high confidence • " +
                    "${(summary.averageConfidence * 100).toInt()}% avg confidence",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

/**
 * Loading state with progress indicator.
 */
@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Generating AI recommendations...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Empty state when no suggestions are available.
 */
@Composable
private fun EmptyState(
    message: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Inbox,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.padding(8.dp)
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Compact version of recommendation list for smaller spaces.
 */
@Composable
fun CompactAIRecommendationList(
    suggestions: List<AISuggestion<DateRecommendation>>,
    modifier: Modifier = Modifier,
    onAcceptRecommendation: (String) -> Unit,
    onDismissRecommendation: (String) -> Unit,
    maxVisible: Int = 3
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        val visibleSuggestions = suggestions.take(maxVisible)

        items(
            items = visibleSuggestions,
            key = { it.id }
        ) { suggestion ->
            CompactAISuggestionCard(
                suggestion = suggestion,
                onAccept = { onAcceptRecommendation(suggestion.id) },
                onDismiss = { onDismissRecommendation(suggestion.id) }
            )
        }

        // Show "View all" if there are more suggestions
        if (suggestions.size > maxVisible) {
            item {
                Text(
                    text = "View all ${suggestions.size} recommendations",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .semantics {
                            contentDescription = "View all ${suggestions.size} AI recommendations"
                        }
                )
            }
        }
    }
}

/**
 * Compact suggestion card for lists with limited space.
 */
@Composable
private fun CompactAISuggestionCard(
    suggestion: AISuggestion<DateRecommendation>,
    onAccept: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactAIBadge(badge = suggestion.badge)

                Text(
                    text = formatCompactTimeSlot(suggestion.data.timeSlot),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${(suggestion.metadata.confidenceScore * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = getConfidenceColor(suggestion.metadata.confidenceScore)
                )

                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
            }
        }

        // Compact action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("Dismiss", style = MaterialTheme.typography.labelSmall)
            }

            Button(
                onClick = onAccept,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("Accept", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/**
 * Helper function to get color based on confidence score.
 */
private fun getConfidenceColor(confidenceScore: Double): Color {
    return when {
        confidenceScore >= 0.9f -> MaterialTheme.colorScheme.tertiary
        confidenceScore >= 0.7f -> MaterialTheme.colorScheme.primary
        confidenceScore >= 0.5f -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.error
    }
}

/**
 * Helper function to format TimeSlot compactly.
 */
private fun formatCompactTimeSlot(timeSlot: com.guyghost.wakeve.models.TimeSlot): String {
    return "${timeSlot.timeOfDay.name.lowercase().replaceFirstChar { it.uppercase() }}"
}
