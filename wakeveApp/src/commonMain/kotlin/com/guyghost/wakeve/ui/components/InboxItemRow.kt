package com.guyghost.wakeve.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.guyghost.wakeve.models.InboxItem

/**
 * Wakeve Design System Colors for InboxItem
 */
private object InboxItemColors {
    val Primary = Color(0xFF2563EB)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceHover = Color(0xFFF8FAFC)
    val OnSurface = Color(0xFF0F172A)
    val OnSurfaceVariant = Color(0xFF475569)
    val OnSurfaceTertiary = Color(0xFF64748B)
    val Outline = Color(0xFFE2E8F0)
    val BadgeBackground = Color(0xFFF1F5F9)
}

/**
 * InboxItemRow - A single notification/inbox item row
 * 
 * Inspired by GitHub Mobile's notification list items:
 * - Status indicator on the left
 * - Content in the middle (context, title, description)
 * - Timestamp and comment count on the right
 * - Unread dot indicator
 * 
 * Adapted to Wakeve's design system with our color palette.
 * 
 * @param item The inbox item to display
 * @param onClick Callback when the item is clicked
 * @param modifier Modifier for the component
 */
@Composable
fun InboxItemRow(
    item: InboxItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (item.isRead) InboxItemColors.Surface else InboxItemColors.SurfaceHover
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Status indicator
            StatusIndicator(
                status = item.status,
                type = item.type,
                size = 40.dp
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Context line (event title or category)
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Unread indicator
                    if (!item.isRead) {
                        UnreadDot(
                            size = 8.dp,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                    
                    Text(
                        text = item.eventTitle ?: item.subtitle ?: getDefaultContext(item),
                        style = MaterialTheme.typography.bodySmall,
                        color = InboxItemColors.OnSurfaceTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.height(2.dp))
                
                // Title
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (item.isRead) FontWeight.Normal else FontWeight.SemiBold,
                    color = InboxItemColors.OnSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Description (if available)
                item.description?.let { description ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = InboxItemColors.OnSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Right side: timestamp and comment count
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Timestamp
                Text(
                    text = formatRelativeTime(item.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = InboxItemColors.OnSurfaceTertiary
                )
                
                // Comment count badge
                if (item.commentCount > 0) {
                    CommentCountBadge(count = item.commentCount)
                }
            }
        }
    }
}

/**
 * Compact version of InboxItemRow for denser lists
 */
@Composable
fun InboxItemRowCompact(
    item: InboxItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (item.isRead) InboxItemColors.Surface else InboxItemColors.SurfaceHover
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Unread dot
            if (!item.isRead) {
                UnreadDot(
                    size = 8.dp,
                    modifier = Modifier.padding(end = 8.dp)
                )
            } else {
                Spacer(modifier = Modifier.width(16.dp))
            }
            
            // Status dot (compact)
            StatusDot(
                status = item.status,
                size = 10.dp
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Title only
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (item.isRead) FontWeight.Normal else FontWeight.Medium,
                color = InboxItemColors.OnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Timestamp
            Text(
                text = formatRelativeTime(item.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = InboxItemColors.OnSurfaceTertiary
            )
        }
    }
}

/**
 * Comment count badge
 */
@Composable
private fun CommentCountBadge(
    count: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = InboxItemColors.BadgeBackground
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = InboxItemColors.OnSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

/**
 * Get default context text based on item type
 */
private fun getDefaultContext(item: InboxItem): String {
    return when (item.type) {
        com.guyghost.wakeve.models.InboxItemType.EVENT_INVITATION -> "Invitation"
        com.guyghost.wakeve.models.InboxItemType.POLL_UPDATE -> "Sondage"
        com.guyghost.wakeve.models.InboxItemType.VOTE_REMINDER -> "Rappel de vote"
        com.guyghost.wakeve.models.InboxItemType.EVENT_CONFIRMED -> "Confirmation"
        com.guyghost.wakeve.models.InboxItemType.PARTICIPANT_JOINED -> "Participant"
        com.guyghost.wakeve.models.InboxItemType.VOTE_SUBMITTED -> "Vote"
        com.guyghost.wakeve.models.InboxItemType.COMMENT_POSTED -> "Commentaire"
        com.guyghost.wakeve.models.InboxItemType.COMMENT_REPLY -> "Réponse"
        com.guyghost.wakeve.models.InboxItemType.BUDGET_UPDATE -> "Budget"
        com.guyghost.wakeve.models.InboxItemType.ACTIVITY_UPDATE -> "Activité"
        com.guyghost.wakeve.models.InboxItemType.ACCOMMODATION_UPDATE -> "Hébergement"
        com.guyghost.wakeve.models.InboxItemType.GENERAL -> "Notification"
    }
}

/**
 * Format timestamp to relative time (e.g., "2h", "3d", "1w")
 */
private fun formatRelativeTime(isoTimestamp: String): String {
    return try {
        val timestamp = kotlinx.datetime.Instant.parse(isoTimestamp)
        val now = kotlinx.datetime.Clock.System.now()
        val duration = now - timestamp
        
        val minutes = duration.inWholeMinutes
        val hours = duration.inWholeHours
        val days = duration.inWholeDays
        
        when {
            minutes < 1 -> "À l'instant"
            minutes < 60 -> "${minutes}min"
            hours < 24 -> "${hours}h"
            days < 7 -> "${days}j"
            days < 30 -> "${days / 7}sem"
            days < 365 -> "${days / 30}mois"
            else -> "${days / 365}an"
        }
    } catch (e: Exception) {
        // Fallback: show raw date
        isoTimestamp.take(10)
    }
}
