package com.guyghost.wakeve.ui.collaboration

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.guyghost.wakeve.R
import com.guyghost.wakeve.models.Comment
import com.guyghost.wakeve.models.CommentSection
import com.guyghost.wakeve.models.CommentThread
import com.guyghost.wakeve.theme.WakeveColors
import kotlinx.datetime.Clock

/**
 * Comment List Screen for Wakeve
 *
 * Material You design with threaded comment support.
 * Supports @mentions, pinning, and soft delete.
 *
 * @param eventId Event ID
 * @param section Comment section
 * @param comments List of comment threads
 * @param currentUserId Current user ID
 * @param isOrganizer Whether current user is organizer
 * @param onNavigateBack Callback for back navigation
 * @param onAddComment Callback to add a new comment
 * @param onReply Callback to reply to a comment
 * @param onEdit Callback to edit a comment
 * @param onDelete Callback to delete a comment
 * @param onPin Callback to pin/unpin a comment
 * @param onUserClick Callback when user avatar/name is clicked
 * @param onLoadMoreReplies Callback to load additional replies for a parent comment
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentListScreen(
    eventId: String,
    section: CommentSection,
    comments: List<CommentThread>,
    currentUserId: String,
    isOrganizer: Boolean,
    onNavigateBack: () -> Unit = {},
    onAddComment: (String, List<String>) -> Unit = { _, _ -> },
    onReply: (String, String) -> Unit = { _, _ -> },
    onEdit: (String, String) -> Unit = { _, _ -> },
    onDelete: (String) -> Unit = {},
    onPin: (String, Boolean) -> Unit = { _, _ -> },
    onUserClick: (String) -> Unit = {},
    onLoadMoreReplies: ((String) -> Unit)? = null
) {
    var commentText by remember { mutableStateOf("") }
    var mentionedUsers by remember { mutableStateOf<List<String>>(emptyList()) }
    var showMentionAutocomplete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(sectionTitle(section)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.a11y_comment_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WakeveColors.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(WakeveColors.background)
        ) {
            // Comment List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = comments,
                    key = { it.comment.id }
                ) { thread ->
                    CommentThreadItem(
                        thread = thread,
                        currentUserId = currentUserId,
                        isOrganizer = isOrganizer,
                        onReply = onReply,
                        onEdit = onEdit,
                        onDelete = onDelete,
                        onPin = onPin,
                        onUserClick = onUserClick,
                        onLoadMoreReplies = onLoadMoreReplies
                    )
                }

                if (comments.isEmpty()) {
                    item {
                        EmptyCommentsSection()
                    }
                }
            }

            // Comment Input
            CommentInput(
                text = commentText,
                mentionedUsers = mentionedUsers,
                showMentionAutocomplete = showMentionAutocomplete,
                onTextChange = { commentText = it },
                onMentionUsersChange = { mentionedUsers = it },
                onShowMentionAutocompleteChange = { showMentionAutocomplete = it },
                onSend = {
                    if (commentText.isNotBlank()) {
                        onAddComment(commentText, mentionedUsers)
                        commentText = ""
                        mentionedUsers = emptyList()
                    }
                }
            )
        }
    }
}

/**
 * Threaded comment item with replies
 */
@Composable
fun CommentThreadItem(
    thread: CommentThread,
    currentUserId: String,
    isOrganizer: Boolean,
    onReply: (String, String) -> Unit,
    onEdit: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onPin: (String, Boolean) -> Unit,
    onUserClick: (String) -> Unit,
    onLoadMoreReplies: ((String) -> Unit)?
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Parent comment
        CommentItem(
            comment = thread.comment,
            isPinned = thread.comment.isPinned,
            currentUserId = currentUserId,
            isOrganizer = isOrganizer,
            isParent = true,
            onReply = onReply,
            onEdit = onEdit,
            onDelete = onDelete,
            onPin = onPin,
            onUserClick = onUserClick
        )

        // Replies (indented)
        if (thread.replies.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .padding(start = 56.dp)
                    .fillMaxWidth()
            ) {
                thread.replies.forEach { reply ->
                    CommentItem(
                        comment = reply,
                        isPinned = false,
                        currentUserId = currentUserId,
                        isOrganizer = isOrganizer,
                        isParent = false,
                        onReply = onReply,
                        onEdit = onEdit,
                        onDelete = onDelete,
                        onPin = onPin,
                        onUserClick = onUserClick
                    )
                }
            }
        }

        // Load more replies indicator
        if (thread.hasMoreReplies) {
            val canLoadMoreReplies = onLoadMoreReplies != null
            Text(
                text = pluralStringResource(
                    R.plurals.comment_reply_count,
                    thread.comment.replyCount,
                    thread.comment.replyCount,
                ),
                color = if (canLoadMoreReplies) WakeveColors.primary else WakeveColors.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .padding(start = 56.dp, top = 4.dp)
                    .then(
                        if (canLoadMoreReplies) {
                            Modifier.clickable { onLoadMoreReplies?.invoke(thread.comment.id) }
                        } else {
                            Modifier
                        }
                    )
            )
        }
    }
}

/**
 * Empty comments section placeholder
 */
@Composable
fun EmptyCommentsSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.comments_empty),
            style = MaterialTheme.typography.titleLarge,
            color = WakeveColors.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.comments_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = WakeveColors.onSurfaceVariant
        )
    }
}

@Composable
private fun sectionTitle(section: CommentSection): String = stringResource(
    when (section) {
        CommentSection.GENERAL -> R.string.comments_title
        CommentSection.SCENARIO -> R.string.comments_section_options
        CommentSection.POLL -> R.string.comments_section_poll
        CommentSection.TRANSPORT -> R.string.comments_section_transport
        CommentSection.ACCOMMODATION -> R.string.comments_section_accommodation
        CommentSection.MEAL -> R.string.comments_section_meal
        CommentSection.EQUIPMENT -> R.string.comments_section_equipment
        CommentSection.ACTIVITY -> R.string.comments_section_activity
        CommentSection.BUDGET -> R.string.comments_section_budget
    },
)
