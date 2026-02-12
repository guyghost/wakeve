package com.guyghost.wakeve.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.guyghost.wakeve.models.InboxItemStatus
import com.guyghost.wakeve.models.InboxItemType

/**
 * Wakeve Design System Colors
 */
private object WakeveColors {
    // Primary
    val Primary = Color(0xFF2563EB)
    val PrimaryLight = Color(0xFF4A90E2)
    
    // Success
    val Success = Color(0xFF059669)
    val SuccessLight = Color(0xFF10B981)
    
    // Warning
    val Warning = Color(0xFFD97706)
    val WarningLight = Color(0xFFF59E0B)
    
    // Accent
    val Accent = Color(0xFF7C3AED)
    val AccentLight = Color(0xFF8B5CF6)
    
    // Neutral
    val Gray = Color(0xFF64748B)
    val GrayLight = Color(0xFF94A3B8)
}

/**
 * StatusIndicator - Circular status indicator with icon
 * 
 * Inspired by GitHub Mobile's issue/PR status indicators,
 * adapted to Wakeve's design system and notification types.
 * 
 * @param status The status determining the color
 * @param type The type determining the icon
 * @param size Size of the indicator (default 40dp)
 * @param modifier Modifier for the component
 */
@Composable
fun StatusIndicator(
    status: InboxItemStatus,
    type: InboxItemType,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val (backgroundColor, iconColor) = getStatusColors(status)
    val icon = getTypeIcon(type)
    
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor.copy(alpha = 0.12f))
            .border(
                width = 1.5.dp,
                color = backgroundColor.copy(alpha = 0.3f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}

/**
 * StatusIndicator variant with just a colored dot (for compact views)
 */
@Composable
fun StatusDot(
    status: InboxItemStatus,
    modifier: Modifier = Modifier,
    size: Dp = 10.dp
) {
    val (color, _) = getStatusColors(status)
    
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
    )
}

/**
 * Unread indicator - small blue dot
 */
@Composable
fun UnreadDot(
    modifier: Modifier = Modifier,
    size: Dp = 8.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(WakeveColors.Primary)
    )
}

/**
 * Get colors based on status
 */
private fun getStatusColors(status: InboxItemStatus): Pair<Color, Color> {
    return when (status) {
        InboxItemStatus.ACTION_REQUIRED -> WakeveColors.Primary to WakeveColors.Primary
        InboxItemStatus.INFO -> WakeveColors.Gray to WakeveColors.Gray
        InboxItemStatus.SUCCESS -> WakeveColors.Success to WakeveColors.Success
        InboxItemStatus.WARNING -> WakeveColors.Warning to WakeveColors.Warning
        InboxItemStatus.COMPLETED -> WakeveColors.Accent to WakeveColors.Accent
    }
}

/**
 * Get icon based on type
 */
private fun getTypeIcon(type: InboxItemType): ImageVector {
    return when (type) {
        InboxItemType.EVENT_INVITATION -> Icons.Default.Event
        InboxItemType.POLL_UPDATE -> Icons.Default.HowToVote
        InboxItemType.VOTE_REMINDER -> Icons.Default.Notifications
        InboxItemType.EVENT_CONFIRMED -> Icons.Outlined.EventAvailable
        InboxItemType.PARTICIPANT_JOINED -> Icons.Default.Person
        InboxItemType.VOTE_SUBMITTED -> Icons.Default.Check
        InboxItemType.COMMENT_POSTED -> Icons.Default.Comment
        InboxItemType.COMMENT_REPLY -> Icons.Default.Comment
        InboxItemType.BUDGET_UPDATE -> Icons.Default.Info
        InboxItemType.ACTIVITY_UPDATE -> Icons.Default.Event
        InboxItemType.ACCOMMODATION_UPDATE -> Icons.Default.Info
        InboxItemType.GENERAL -> Icons.Default.Notifications
    }
}
