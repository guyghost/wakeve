package com.guyghost.wakeve.ui.collaboration

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.guyghost.wakeve.collaboration.MentionParser
import com.guyghost.wakeve.models.Comment
import com.guyghost.wakeve.theme.WakeveColors
import kotlinx.datetime.Clock

/**
 * Comment Item Component
 *
 * Material You design card with comment content, mentions, and actions.
 *
 * @param comment Comment data
 * @param isPinned Whether comment is pinned
 * @param currentUserId Current user ID
 * @param isOrganizer Whether current user is organizer
 * @param isParent Whether this is a parent comment (top-level)
 * @param onReply Callback to reply
 * @param onEdit Callback to edit
 * @param onDelete Callback to delete
 * @param onPin Callback to pin/unpin
 * @param onUserClick Callback when user is clicked
 */
@Composable
fun CommentItem(
    comment: Comment,
    isPinned: Boolean,
    currentUserId: String,
    isOrganizer: Boolean,
    isParent: Boolean,
    onReply: (String, String) -> Unit,
    onEdit: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onPin: (String, Boolean) -> Unit,
    onUserClick: (String) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isPinned) {
                    Modifier.background(
                        Color.Transparent
                    )
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPinned) {
                WakeveColors.primaryContainer.copy(alpha = 0.3f)
            } else {
                WakeveColors.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Header: Avatar + Name + Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Avatar(
                    initials = getInitials(comment.authorName),
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { onUserClick(comment.authorId) }
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Name + Time
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = comment.authorName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = WakeveColors.onSurface,
                        modifier = Modifier.clickable { onUserClick(comment.authorId) }
                    )
                    Text(
                        text = formatTimestamp(comment.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = WakeveColors.onSurfaceVariant
                    )
                }

                // Pin icon (if pinned)
                if (isPinned) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pinned",
                        tint = WakeveColors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // More options menu
                if (comment.canEdit(currentUserId) || comment.canDelete(currentUserId, isOrganizer)) {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options")
                        }

                        CommentDropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            comment = comment,
                            currentUserId = currentUserId,
                            isOrganizer = isOrganizer,
                            onReply = onReply,
                            onEdit = onEdit,
                            onDelete = onDelete,
                            onPin = onPin
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Content with highlighted mentions
            Text(
                text = buildAnnotatedString {
                    val parts = splitContentWithMentions(comment.content, comment.mentions)

                    var currentIndex = 0
                    parts.forEach { part ->
                        if (part.isMention) {
                            withStyle(
                                style = SpanStyle(
                                    color = WakeveColors.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            ) {
                                append("@${part.text}")
                            }
                        } else {
                            append(part.text)
                        }
                        currentIndex += part.text.length
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (comment.isDeleted) {
                    WakeveColors.onSurfaceVariant
                } else {
                    WakeveColors.onSurface
                }
            )

            // Reply button (for parent comments only)
            if (isParent && !comment.isDeleted) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { onReply(comment.id, comment.authorName) },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = WakeveColors.primary
                    )
                ) {
                    Text("Reply")
                }
            }
        }
    }
}

/**
 * Comment dropdown menu
 */
@Composable
fun CommentDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    comment: Comment,
    currentUserId: String,
    isOrganizer: Boolean,
    onReply: (String, String) -> Unit,
    onEdit: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onPin: (String, Boolean) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest
    ) {
        if (!comment.isDeleted) {
            // Reply
            DropdownMenuItem(
                text = { Text("Reply") },
                onClick = {
                    onReply(comment.id, comment.authorName)
                    onDismissRequest()
                }
            )

            // Edit (only own comments)
            if (comment.canEdit(currentUserId)) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = {
                        onEdit(comment.id, comment.content)
                        onDismissRequest()
                    }
                )
            }

            // Pin/Unpin (organizer only)
            if (comment.canPin(currentUserId, isOrganizer)) {
                DropdownMenuItem(
                    text = {
                        Text(if (comment.isPinned) "Unpin" else "Pin")
                    },
                    onClick = {
                        onPin(comment.id, !comment.isPinned)
                        onDismissRequest()
                    }
                )
            }

            // Delete
            if (comment.canDelete(currentUserId, isOrganizer)) {
                Divider()
                DropdownMenuItem(
                    text = {
                        Text(
                            if (comment.authorId == currentUserId) "Delete" else "Remove",
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    onClick = {
                        onDelete(comment.id)
                        onDismissRequest()
                    }
                )
            }
        }
    }
}

/**
 * Avatar placeholder with initials
 */
@Composable
fun Avatar(
    initials: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(WakeveColors.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.titleMedium,
            color = WakeveColors.onPrimaryContainer,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Get initials from name
 */
private fun getInitials(name: String): String {
    return name
        .split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
}

/**
 * Format timestamp to relative time
 */
private fun formatTimestamp(timestamp: String): String {
    return try {
        val instant = kotlinx.datetime.Instant.parse(timestamp)
        val now = Clock.System.now()
        val duration = now.minus(instant)

        when {
            duration.inWholeMinutes < 1 -> "Just now"
            duration.inWholeMinutes < 60 -> "${duration.inWholeMinutes}m ago"
            duration.inWholeHours < 24 -> "${duration.inWholeHours}h ago"
            duration.inWholeDays < 7 -> "${duration.inWholeDays}d ago"
            else -> "${duration.inWholeDays / 7}w ago"
        }
    } catch (e: Exception) {
        "Unknown time"
    }
}

/**
 * Split content into text and mention parts
 */
private data class ContentPart(
    val text: String,
    val isMention: Boolean
)

private fun splitContentWithMentions(content: String, mentions: List<String>): List<ContentPart> {
    if (mentions.isEmpty()) {
        return listOf(ContentPart(content, false))
    }

    val parts = mutableListOf<ContentPart>()
    var currentIndex = 0

    mentions.forEach { userId ->
        val mentionText = "@$userId"
        val startIndex = content.indexOf(mentionText, currentIndex)

        if (startIndex >= 0) {
            // Add text before mention
            if (startIndex > currentIndex) {
                parts.add(ContentPart(content.substring(currentIndex, startIndex), false))
            }

            // Add mention
            parts.add(ContentPart(userId, true))
            currentIndex = startIndex + mentionText.length
        }
    }

    // Add remaining text
    if (currentIndex < content.length) {
        parts.add(ContentPart(content.substring(currentIndex), false))
    }

    return parts
}
