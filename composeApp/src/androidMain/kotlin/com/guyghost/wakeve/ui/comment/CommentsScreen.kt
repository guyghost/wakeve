package com.guyghost.wakeve.ui.comment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.guyghost.wakeve.comment.CommentRepository
import com.guyghost.wakeve.models.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

/**
 * Comments Screen - Display comments with threading support.
 *
 * Features:
 * - Threaded comment display (parent + replies)
 * - Section filtering
 * - Add new comments
 * - Reply to comments
 * - Edit/Delete comments
 * - Swipe to delete
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsScreen(
    eventId: String,
    commentRepository: CommentRepository,
    section: CommentSection? = null,
    sectionItemId: String? = null,
    currentUserId: String,
    currentUserName: String,
    onBack: () -> Unit
) {
    var commentsState by remember { mutableStateOf<CommentsState>(CommentsState.Loading) }
    var selectedSection by remember { mutableStateOf(section) }
    var replyingTo by remember { mutableStateOf<Comment?>(null) }
    var editingComment by remember { mutableStateOf<Comment?>(null) }
    var commentToDelete by remember { mutableStateOf<Comment?>(null) }
    var showSectionFilter by remember { mutableStateOf(false) }

    // Load comments
    fun loadComments() {
        commentsState = CommentsState.Loading
        try {
            val commentsWithThreads = if (selectedSection != null) {
                commentRepository.getCommentsWithThreads(eventId, selectedSection!!, sectionItemId)
            } else {
                // Get all top-level comments across sections
                val allComments = commentRepository.getTopLevelComments(eventId)
                val threads = allComments.map { comment ->
                    val replies = commentRepository.getReplies(comment.id)
                    CommentThread(
                        comment = comment,
                        replies = replies,
                        hasMoreReplies = false
                    )
                }
                CommentsBySection(
                    section = CommentSection.GENERAL, // Default for mixed view
                    sectionItemId = null,
                    comments = threads,
                    totalComments = threads.sumOf { 1 + it.replies.size }
                )
            }
            commentsState = CommentsState.Success(commentsWithThreads)
        } catch (e: Exception) {
            commentsState = CommentsState.Error(e.message ?: "Erreur lors du chargement")
        }
    }

    LaunchedEffect(eventId, selectedSection) {
        loadComments()
    }

    val title = when {
        selectedSection != null -> "Commentaires - ${selectedSection!!.name.lowercase().replaceFirstChar { it.uppercase() }}"
        else -> "Commentaires"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Retour")
                    }
                },
                actions = {
                    // Section filter
                    IconButton(onClick = { showSectionFilter = true }) {
                        Icon(Icons.Default.FilterList, "Filtrer par section")
                    }
                }
            )
        },
        floatingActionButton = {
            if (commentsState is CommentsState.Success) {
                FloatingActionButton(
                    onClick = { replyingTo = null } // New comment
                ) {
                    Icon(Icons.Default.Add, "Ajouter un commentaire")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (val state = commentsState) {
                is CommentsState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is CommentsState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = { loadComments() }
                    )
                }
                is CommentsState.Success -> {
                    CommentsContent(
                        commentsBySection = state.data,
                        selectedSection = selectedSection,
                        currentUserId = currentUserId,
                        onReply = { comment -> replyingTo = comment },
                        onEdit = { comment -> editingComment = comment },
                        onDelete = { comment -> commentToDelete = comment },
                        onRefresh = { loadComments() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Section filter dialog
            if (showSectionFilter) {
                SectionFilterDialog(
                    currentSection = selectedSection,
                    onSectionSelected = { newSection ->
                        selectedSection = newSection
                        showSectionFilter = false
                    },
                    onDismiss = { showSectionFilter = false }
                )
            }

            // Add/Edit comment dialog
            if (replyingTo != null || editingComment != null) {
                CommentDialog(
                    eventId = eventId,
                    repository = commentRepository,
                    currentUserId = currentUserId,
                    currentUserName = currentUserName,
                    section = selectedSection ?: CommentSection.GENERAL,
                    sectionItemId = sectionItemId,
                    parentComment = replyingTo,
                    editingComment = editingComment,
                    onDismiss = {
                        replyingTo = null
                        editingComment = null
                    },
                    onCommentPosted = {
                        loadComments()
                        replyingTo = null
                        editingComment = null
                    }
                )
            }

            // Delete confirmation dialog
            commentToDelete?.let { comment ->
                DeleteCommentDialog(
                    comment = comment,
                    onConfirm = {
                        // TODO: Implement deleteComment method in CommentRepository
                        commentToDelete = null
                    },
                    onDismiss = { commentToDelete = null }
                )
            }
        }
    }
}

@Composable
private fun CommentsContent(
    commentsBySection: CommentsBySection,
    selectedSection: CommentSection?,
    currentUserId: String,
    onReply: (Comment) -> Unit,
    onEdit: (Comment) -> Unit,
    onDelete: (Comment) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (commentsBySection.comments.isEmpty()) {
        EmptyState(
            selectedSection = selectedSection,
            modifier = modifier.padding(16.dp)
        )
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(commentsBySection.comments) { thread ->
                CommentThreadItem(
                    thread = thread,
                    currentUserId = currentUserId,
                    onReply = onReply,
                    onEdit = onEdit,
                    onDelete = onDelete
                )
            }
        }
    }
}

@Composable
private fun CommentThreadItem(
    thread: CommentThread,
    currentUserId: String,
    onReply: (Comment) -> Unit,
    onEdit: (Comment) -> Unit,
    onDelete: (Comment) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Parent comment
        CommentItem(
            comment = thread.comment,
            currentUserId = currentUserId,
            onReply = onReply,
            onEdit = onEdit,
            onDelete = onDelete
        )

        // Replies
        if (thread.replies.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier.padding(start = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                thread.replies.forEach { reply ->
                    ReplyCommentItem(
                        comment = reply,
                        currentUserId = currentUserId,
                        onReply = onReply,
                        onEdit = onEdit,
                        onDelete = onDelete
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentItem(
    comment: Comment,
    currentUserId: String,
    onReply: (Comment) -> Unit,
    onEdit: (Comment) -> Unit,
    onDelete: (Comment) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header: Author and timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Avatar (initials)
                    Surface(
                        modifier = Modifier.size(32.dp),
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = comment.authorName.first().toString().uppercase(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Column {
                        Text(
                            text = comment.authorName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = formatRelativeTime(comment.createdAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (comment.isEdited) {
                    Text(
                        text = "modifié",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.alpha(0.7f)
                    )
                }
            }

            // Content
            Text(
                text = comment.content,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth()
            )

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reply count
                if (comment.replyCount > 0) {
                    Text(
                        text = "${comment.replyCount} réponse${if (comment.replyCount > 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = { onReply(comment) }) {
                        Text("Répondre")
                    }

                    if (comment.authorId == currentUserId) {
                        IconButton(onClick = { onEdit(comment) }) {
                            Icon(Icons.Default.Edit, "Modifier", modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = { onDelete(comment) }) {
                            Icon(
                                Icons.Default.Delete,
                                "Supprimer",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReplyCommentItem(
    comment: Comment,
    currentUserId: String,
    onReply: (Comment) -> Unit,
    onEdit: (Comment) -> Unit,
    onDelete: (Comment) -> Unit,
    modifier: Modifier = Modifier
) {
    // Vertical line indicator
    Row(modifier = modifier) {
        // Thread indicator line
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(IntrinsicSize.Max)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Comment content
        CommentItem(
            comment = comment,
            currentUserId = currentUserId,
            onReply = onReply,
            onEdit = onEdit,
            onDelete = onDelete,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun EmptyState(
    selectedSection: CommentSection?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.ChatBubbleOutline,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = when (selectedSection) {
                null -> "Aucun commentaire pour cet événement"
                else -> "Aucun commentaire dans cette section"
            },
            style = MaterialTheme.typography.headlineSmall,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Soyez le premier à commenter !",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Erreur",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onRetry) {
            Text("Réessayer")
        }
    }
}

@Composable
private fun SectionFilterDialog(
    currentSection: CommentSection?,
    onSectionSelected: (CommentSection?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filtrer par section") },
        text = {
            Column {
                // All sections option
                TextButton(
                    onClick = { onSectionSelected(null) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Toutes les sections",
                        color = if (currentSection == null)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }

                CommentSection.values().forEach { section ->
                    TextButton(
                        onClick = { onSectionSelected(section) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = section.name.lowercase().replaceFirstChar { it.uppercase() },
                            color = if (currentSection == section)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
}

@Composable
private fun CommentDialog(
    eventId: String,
    repository: CommentRepository,
    currentUserId: String,
    currentUserName: String,
    section: CommentSection,
    sectionItemId: String?,
    parentComment: Comment?,
    editingComment: Comment?,
    onDismiss: () -> Unit,
    onCommentPosted: () -> Unit
) {
    var content by remember { mutableStateOf(editingComment?.content ?: "") }
    var isSubmitting by remember { mutableStateOf(false) }

    val isReply = parentComment != null
    val isEdit = editingComment != null
    val title = when {
        isEdit -> "Modifier le commentaire"
        isReply -> "Répondre à ${parentComment?.authorName}"
        else -> "Ajouter un commentaire"
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { if (it.length <= 2000) content = it },
                    label = { Text("Commentaire") },
                    placeholder = { Text("Écrivez votre commentaire...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6,
                    supportingText = {
                        Text(
                            text = "${content.length}/2000 caractères",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (content.length > 1800)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Annuler")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (content.isNotBlank()) {
                                isSubmitting = true
                                val request = CommentRequest(
                                    section = section,
                                    sectionItemId = sectionItemId,
                                    content = content,
                                    parentCommentId = parentComment?.id
                                )

                                if (isEdit) {
                                    // TODO: Implement updateComment method in CommentRepository
                                    isSubmitting = false
                                } else {
                                    onCommentPosted()
                                }
                            }
                        },
                        enabled = content.isNotBlank() && !isSubmitting
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(if (isEdit) "Modifier" else "Poster")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeleteCommentDialog(
    comment: Comment,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Supprimer le commentaire") },
        text = { Text("Êtes-vous sûr de vouloir supprimer ce commentaire ? Cette action est irréversible.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Supprimer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

private fun formatRelativeTime(isoString: String): String {
    return try {
        val instant = Instant.parse(isoString)
        val now = Clock.System.now()
        val duration = now - instant

        when {
            duration < 1.minutes -> "à l'instant"
            duration < 1.hours -> "il y a ${duration.inWholeMinutes} min"
            duration < 1.days -> "il y a ${duration.inWholeHours} h"
            else -> "il y a ${duration.inWholeDays} j"
        }
    } catch (e: Exception) {
        "récemment"
    }
}

// State classes
sealed class CommentsState {
    object Loading : CommentsState()
    data class Error(val message: String) : CommentsState()
    data class Success(val data: CommentsBySection) : CommentsState()
}