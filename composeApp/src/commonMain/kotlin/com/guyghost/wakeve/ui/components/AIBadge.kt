package com.guyghost.wakeve.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guyghost.wakeve.models.AIBadge

/**
 * AI Badge Component - Displays an AI suggestion badge with icon and label.
 *
 * Material You design with:
 * - Elevated Surface with tonal elevation
 * - LabelSmall typography
 * - Emoji or icon support
 * - Accessibility labels for screen readers
 * - Animated appearance
 *
 * @param badge The AIBadge to display
 * @param modifier Modifier for the component
 * @param onClick Optional click handler for interactive badges
 */
@Composable
fun AIBadge(
    badge: AIBadge,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val badgeColor = parseColor(badge.color)
    val backgroundColor = badgeColor.copy(alpha = 0.12f)
    val contentColor = badgeColor

    val animatedAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 300),
        label = "badgeAlpha"
    )

    Surface(
        modifier = modifier
            .semantics {
                contentDescription = "${badge.displayName} AI suggestion${if (badge.tooltip != null) ". ${badge.tooltip}" else ""}"
            }
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            ),
        shape = MaterialTheme.shapes.small,
        color = backgroundColor,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .alpha(animatedAlpha),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Icon/Emoji
            Text(
                text = badge.icon,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.semantics { contentDescription = "AI icon" }
            )

            Spacer(modifier = Modifier.width(2.dp))

            // Display name
            Text(
                text = badge.displayName,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

/**
 * Compact AI Badge - Smaller version for inline use.
 *
 * @param badge The AIBadge to display
 * @param modifier Modifier for the component
 */
@Composable
fun CompactAIBadge(
    badge: AIBadge,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.semantics {
            contentDescription = "${badge.displayName} AI"
        },
        shape = RoundedCornerShape(4.dp),
        color = parseColor(badge.color).copy(alpha = 0.15f),
        tonalElevation = 1.dp
    ) {
        Text(
            text = badge.icon,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

/**
 * AI Badge with confidence indicator.
 * Shows badge with a visual confidence level.
 *
 * @param badge The AIBadge to display
 * @param confidenceScore Confidence score (0.0 - 1.0)
 * @param modifier Modifier for the component
 */
@Composable
fun AIBadgeWithConfidence(
    badge: AIBadge,
    confidenceScore: Double,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AIBadge(badge = badge)

        // Confidence indicator
        ConfidenceIndicator(confidenceScore = confidenceScore)
    }
}

/**
 * Visual confidence indicator with label.
 *
 * @param confidenceScore Confidence score (0.0 - 1.0)
 * @param showLabel Whether to show the percentage label
 * @param modifier Modifier for the component
 */
@Composable
fun ConfidenceIndicator(
    confidenceScore: Double,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true
) {
    val (color, label) = when {
        confidenceScore >= 0.9f -> MaterialTheme.colorScheme.tertiary to "Excellent"
        confidenceScore >= 0.7f -> MaterialTheme.colorScheme.secondary to "Good"
        confidenceScore >= 0.5f -> MaterialTheme.colorScheme.tertiary to "Fair"
        else -> MaterialTheme.colorScheme.error to "Low"
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Confidence bar
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color.copy(alpha = 0.2f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(confidenceScore.toFloat().coerceIn(0f, 1f))
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }

        if (showLabel) {
            Text(
                text = "${(confidenceScore * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}

/**
 * Badge row showing multiple AI badges.
 *
 * @param badges List of AIBadges to display
 * @param modifier Modifier for the component
 * @param horizontalArrangement Arrangement of badges
 */
@Composable
fun AIBadgeRow(
    badges: List<AIBadge>,
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(6.dp)
) {
    Row(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement
    ) {
        badges.forEach { badge ->
            AIBadge(badge = badge)
        }
    }
}

/**
 * Helper function to parse hex color string to Compose Color.
 */
private fun parseColor(hexColor: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hexColor))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }
}
