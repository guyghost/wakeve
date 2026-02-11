package com.guyghost.wakeve.ui.collaboration

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.guyghost.wakeve.theme.WakevColors
import com.guyghost.wakeve.collaboration.MentionParser

/**
 * Comment Input Component
 *
 * Material You design input field with @mention autocomplete support.
 *
 * @param text Current comment text
 * @param mentionedUsers List of mentioned user IDs
 * @param showMentionAutocomplete Whether to show mention autocomplete
 * @param onTextChange Callback when text changes
 * @param onMentionUsersChange Callback when mentioned users change
 * @param onShowMentionAutocompleteChange Callback to toggle mention autocomplete visibility
 * @param onSend Callback to send comment
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentInput(
    text: String,
    mentionedUsers: List<String>,
    showMentionAutocomplete: Boolean,
    onTextChange: (String) -> Unit,
    onMentionUsersChange: (List<String>) -> Unit,
    onShowMentionAutocompleteChange: (Boolean) -> Unit,
    onSend: () -> Unit
) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue(text)) }
    val focusRequester = remember { FocusRequester() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(WakevColors.surface)
    ) {
        Column {
            // Input field
            OutlinedTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    textFieldValue = newValue
                    onTextChange(newValue.text)

                    // Check for @ to trigger autocomplete
                    val cursorPos = newValue.selection.start
                    val textBeforeCursor = newValue.text.substring(0, cursorPos)
                    val lastAtIndex = textBeforeCursor.lastIndexOf('@')

                    if (lastAtIndex >= 0) {
                        // Extract username after @
                        val username = textBeforeCursor.substring(lastAtIndex + 1)
                        onShowMentionAutocompleteChange(username.length >= 2)
                    } else {
                        onShowMentionAutocompleteChange(false)
                    }

                    // Parse mentions from text
                    val newMentions = MentionParser.extractUsernames(newValue.text)
                        .map { it.removePrefix("@") }
                        .distinct()
                    onMentionUsersChange(newMentions)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                placeholder = { Text("Add a comment...") },
                minLines = 1,
                maxLines = 4,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WakevColors.primary,
                    unfocusedBorderColor = WakevColors.outline,
                    focusedContainerColor = WakevColors.surfaceVariant,
                    unfocusedContainerColor = WakevColors.surfaceVariant
                ),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (text.isNotBlank()) {
                                onSend()
                            }
                        },
                        enabled = text.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = if (text.isNotBlank()) {
                                WakevColors.primary
                            } else {
                                WakevColors.onSurfaceVariant
                            }
                        )
                    }
                }
            )

            // Mention Autocomplete Dropdown
            if (showMentionAutocomplete) {
                MentionAutocomplete(
                    onUserSelected = { username ->
                        val cursorPos = textFieldValue.selection.start
                        val textBeforeCursor = textFieldValue.text.substring(0, cursorPos)
                        val lastAtIndex = textBeforeCursor.lastIndexOf('@')

                        if (lastAtIndex >= 0) {
                            // Replace @partial with @username
                            val newText = textFieldValue.text.substring(0, lastAtIndex) +
                                    "@$username " +
                                    textFieldValue.text.substring(cursorPos)

                            val newCursorPos = lastAtIndex + username.length + 2 // +2 for "@" and space
                            textFieldValue = TextFieldValue(
                                text = newText,
                                selection = TextRange(newCursorPos, newCursorPos)
                            )
                            onTextChange(newText)
                            onShowMentionAutocompleteChange(false)
                            focusRequester.requestFocus()
                        }
                    }
                )
            }
        }
    }
}

/**
 * Mention Autocomplete Dropdown
 *
 * Shows list of users matching the typed @username.
 *
 * @param onUserSelected Callback when user is selected
 */
@Composable
fun MentionAutocomplete(
    onUserSelected: (String) -> Unit
) {
    // Mock user data - in real app, this would come from UserRepository
    val allUsers = remember {
        listOf(
            "alice",
            "bob",
            "charlie",
            "diana",
            "eve",
            "frank"
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 200.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = WakevColors.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = "Mention someone...",
                style = MaterialTheme.typography.labelSmall,
                color = WakevColors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )

            allUsers.forEach { username ->
                MentionUserItem(
                    username = username,
                    onClick = { onUserSelected(username) }
                )
            }
        }
    }
}

/**
 * Mention user item in autocomplete dropdown
 */
@Composable
fun MentionUserItem(
    username: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar initials
        Avatar(
            initials = getInitials(username),
            modifier = Modifier.size(32.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Username
        Text(
            text = "@$username",
            style = MaterialTheme.typography.bodyMedium,
            color = WakevColors.onSurface
        )
    }
}

/**
 * Get initials from username
 */
private fun getInitials(username: String): String {
    return username
        .take(2)
        .uppercase()
}
