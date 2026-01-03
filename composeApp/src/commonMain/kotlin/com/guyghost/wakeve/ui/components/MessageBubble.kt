package com.guyghost.wakeve.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.guyghost.wakeve.chat.ChatMessage
import com.guyghost.wakeve.chat.MessageStatus
import com.guyghost.wakeve.chat.Reaction

/**
 * MessageBubble - Displays a chat message with sender info, content, reactions, and status.
 * 
 * Supports:
 * - Visual differentiation (current user vs others)
 * - Thread indentation for replies
 * - Emoji reactions with counts
 * - Message status indicators (sent, delivered, read)
 * 
 * @param message The chat message to display
 * @param isCurrentUser Whether this message is from the current user
 * @param onReactionClick Callback when an emoji reaction is tapped
 * @param onRemoveReaction Callback when a reaction is long-pressed (to remove)
 * @param onReplyClick Callback when reply button is tapped
 * @param modifier Modifier for the component
 */
@Composable
fun MessageBubble(
    message: ChatMessage,
    isCurrentUser: Boolean,
    onReactionClick: (String) -> Unit,
    onRemoveReaction: (String, String) -> Unit,
    onReplyClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val bubbleColor by animateColorAsState(
        targetValue = if (isCurrentUser) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(200),
        label = "bubbleColor"
    )
    
    val textColor = if (isCurrentUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    // Calculate indentation for threaded messages
    val indentation = if (message.parentId != null) 24.dp else 0.dp
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = if (isCurrentUser) 48.dp else indentation,
                end = if (!isCurrentUser) 48.dp else 0.dp,
                top = 4.dp,
                bottom = 4.dp
            ),
        horizontalArrangement = if (isCurrentUser) {
            Arrangement.End
        } else {
            Arrangement.Start
        }
    ) {
        Column(
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            // Sender name (if not current user and not a thread reply)
            if (!isCurrentUser && message.parentId == null) {
                Text(
                    text = message.senderName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
            }
            
            // Reply reference (if this is a reply)
            if (message.parentId != null) {
                ReplyReferenceLine(
                    originalSender = message.senderName,
                    originalContent = message.content,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            
            // Message bubble
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(
                    topStart = if (isCurrentUser) 16.dp else 4.dp,
                    topEnd = if (isCurrentUser) 4.dp else 16.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 16.dp
                ),
                color = bubbleColor
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Message content
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor
                    )
                    
                    // Reactions bar
                    if (message.reactions.isNotEmpty()) {
                        ReactionBar(
                            reactions = message.reactions,
                            onReactionClick = onReactionClick,
                            onRemoveReaction = onRemoveReaction,
                            currentUserId = if (isCurrentUser) message.senderId else null
                        )
                    }
                    
                    // Timestamp and status row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTimestamp(message.timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor.copy(alpha = 0.6f)
                        )
                        
                        MessageStatusIcon(
                            status = message.status,
                            isCurrentUser = isCurrentUser
                        )
                    }
                    
                    // Reply button (only for non-replies)
                    if (onReplyClick != null && message.parentId == null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "Répondre",
                                style = MaterialTheme.typography.labelSmall,
                                color = textColor.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .clickable { onReplyClick() }
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }
            
            // Thread indicator line
            if (message.parentId != null) {
                ThreadIndicatorLine(
                    isCurrentUser = isCurrentUser,
                    modifier = Modifier.padding(start = if (isCurrentUser) Modifier else Modifier)
                )
            }
        }
    }
}

/**
 * Displays a reference to the original message being replied to.
 */
@Composable
private fun ReplyReferenceLine(
    originalSender: String,
    originalContent: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .width(200.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
        Text(
            text = originalSender,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = MaterialTheme.typography.labelSmall.fontWeight
        )
        Text(
            text = ": ${originalContent.take(30)}${if (originalContent.length > 30) "..." else ""}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Thread indicator line for visual threading.
 */
@Composable
private fun ThreadIndicatorLine(
    isCurrentUser: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(2.dp)
            .height(20.dp)
            .background(
                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
    )
}

/**
 * ReactionBar - Displays emoji reactions in a horizontal scrollable row.
 * 
 * Reactions are grouped by emoji and show a count badge.
 * 
 * @param reactions List of all reactions on the message
 * @param onReactionClick Callback when an emoji is tapped (to add/react)
 * @param onRemoveReaction Callback when an emoji is long-pressed (to remove own reaction)
 * @param currentUserId ID of the current user (for highlighting own reactions)
 * @param modifier Modifier for the component
 */
@Composable
fun ReactionBar(
    reactions: List<Reaction>,
    onReactionClick: (String) -> Unit,
    onRemoveReaction: (String, String) -> Unit,
    currentUserId: String? = null,
    modifier: Modifier = Modifier
) {
    // Group reactions by emoji
    val grouped = reactions.groupBy { it.emoji }
    
    if (grouped.isEmpty()) return
    
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        grouped.forEach { (emoji, reactionList) ->
            val hasCurrentUserReacted = currentUserId != null && 
                reactionList.any { it.userId == currentUserId }
            
            ReactionChip(
                emoji = emoji,
                count = reactionList.size,
                isHighlighted = hasCurrentUserReacted,
                onClick = { onReactionClick(emoji) },
                onLongClick = if (currentUserId != null) {
                    { onRemoveReaction(currentUserId, emoji) }
                } else null
            )
        }
    }
}

/**
 * Individual reaction chip with emoji and count.
 * 
 * @param emoji The emoji character
 * @param count Number of users who reacted with this emoji
 * @param isHighlighted Whether this reaction is from the current user
 * @param onClick Callback when the chip is tapped
 * @param onLongClick Callback when the chip is long-pressed (optional)
 */
@Composable
fun ReactionChip(
    emoji: String,
    count: Int,
    isHighlighted: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isHighlighted) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        },
        animationSpec = tween(200),
        label = "reactionChipBg"
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (isHighlighted) {
            MaterialTheme.colorScheme.primary
        } else {
            Color.Transparent
        },
        animationSpec = tween(200),
        label = "reactionChipBorder"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isHighlighted) 1.05f else 1f,
        animationSpec = tween(200),
        label = "reactionChipScale"
    )
    
    Surface(
        modifier = Modifier
            .semantics {
                contentDescription = "$emoji, $count réactions"
            }
            .then(
                if (onLongClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier.clickable(onClick = onClick)
                }
            ),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = if (isHighlighted) {
            Modifier.border(1.dp, borderColor, RoundedCornerShape(12.dp))
        } else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Message status icon showing delivery/read status.
 * 
 * @param status The message status
 * @param isCurrentUser Whether this message is from current user
 */
@Composable
fun MessageStatusIcon(
    status: MessageStatus,
    isCurrentUser: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isCurrentUser) return // Don't show status for received messages
    
    val (icon, tint, contentDescription) = when (status) {
        MessageStatus.SENT -> Triple(
            Icons.Default.Check,
            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
            "Envoyé"
        )
        MessageStatus.DELIVERED -> Triple(
            Icons.Default.DoneAll,
            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
            "Distribué"
        )
        MessageStatus.READ -> Triple(
            Icons.Default.DoneAll,
            MaterialTheme.colorScheme.onPrimary,
            "Lu"
        )
        MessageStatus.FAILED -> Triple(
            Icons.Default.Error,
            MaterialTheme.colorScheme.error,
            "Échec d'envoi"
        )
    }
    
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier.size(16.dp)
    )
}

/**
 * Format ISO 8601 timestamp to readable time.
 */
private fun formatTimestamp(timestamp: String): String {
    return try {
        val instant = java.time.Instant.parse(timestamp)
        val localTime = java.time.LocalDateTime.ofInstant(
            instant,
            java.time.ZoneId.systemDefault()
        )
        val formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
        localTime.format(formatter)
    } catch (e: Exception) {
        timestamp
    }
}
