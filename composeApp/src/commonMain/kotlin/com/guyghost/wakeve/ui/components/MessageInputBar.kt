package com.guyghost.wakeve.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

/**
 * MessageInputBar - Input bar for composing and sending chat messages.
 * 
 * Features:
 * - Text input with placeholder
 * - Attachment button (for images, files)
 * - Emoji button (emoji picker)
 * - Voice message option
 * - Send button (enabled only when text is not blank)
 * - Section selector for organizing messages
 * 
 * @param onSendMessage Callback when message is sent
 * @param onTypingStart Callback when user starts typing
 * @param onTypingStop Callback when user stops typing
 * @param modifier Modifier for the component
 * @param selectedSection Currently selected section/category
 * @param onSectionChange Callback when section is changed
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageInputBar(
    onSendMessage: (String) -> Unit,
    onTypingStart: () -> Unit = {},
    onTypingStop: () -> Unit = {},
    modifier: Modifier = Modifier,
    selectedSection: com.guyghost.wakeve.chat.CommentSection? = null,
    onSectionChange: ((com.guyghost.wakeve.chat.CommentSection?) -> Unit)? = null
) {
    var text by remember { mutableStateOf("") }
    var showAttachmentMenu by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Section selector (optional)
            if (onSectionChange != null) {
                SectionSelector(
                    selectedSection = selectedSection,
                    onSectionChange = onSectionChange
                )
            }
            
            // Main input row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Attachment button
                Box {
                    IconButton(
                        onClick = { showAttachmentMenu = !showAttachmentMenu },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Ajouter une pièce jointe",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Attachment menu popup
                    if (showAttachmentMenu) {
                        AttachmentMenu(
                            onDismiss = { showAttachmentMenu = false },
                            onImageSelected = { /* TODO: Open image picker */ },
                            onFileSelected = { /* TODO: Open file picker */ }
                        )
                    }
                }
                
                // Emoji button
                IconButton(
                    onClick = { showEmojiPicker = !showEmojiPicker },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEmotions,
                        contentDescription = "Émojis",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Text input field
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = text,
                            onValueChange = { newText ->
                                text = newText
                                if (newText.isNotBlank()) {
                                    onTypingStart()
                                } else {
                                    onTypingStop()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                            placeholder = {
                                Text(
                                    text = "Écrivez votre message...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Send
                            ),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (text.isNotBlank()) {
                                        onSendMessage(text)
                                        text = ""
                                        onTypingStop()
                                    }
                                }
                            ),
                            maxLines = 4
                        )
                    }
                }
                
                // Voice message button (when text is empty)
                AnimatedVisibility(
                    visible = text.isBlank(),
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    IconButton(
                        onClick = { /* TODO: Start voice recording */ },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MicNone,
                            contentDescription = "Message vocal",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Send button (when text is not empty)
                AnimatedVisibility(
                    visible = text.isNotBlank(),
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    IconButton(
                        onClick = {
                            if (text.isNotBlank()) {
                                onSendMessage(text)
                                text = ""
                                onTypingStop()
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .semantics {
                                contentDescription = "Envoyer le message"
                            }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Envoyer",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
            
            // Emoji picker (when expanded)
            AnimatedVisibility(
                visible = showEmojiPicker,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                EmojiPicker(
                    onEmojiSelected = { emoji ->
                        text = text + emoji
                        showEmojiPicker = false
                        onTypingStart()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
        }
    }
}

/**
 * Section selector for organizing messages by category.
 */
@Composable
private fun SectionSelector(
    selectedSection: com.guyghost.wakeve.chat.CommentSection?,
    onSectionChange: (com.guyghost.wakeve.chat.CommentSection?) -> Unit,
    modifier: Modifier = Modifier
) {
    val sections = listOf(
        null to "Général",
        com.guyghost.wakeve.chat.CommentSection.TRANSPORT to "🚗 Transport",
        com.guyghost.wakeve.chat.CommentSection.FOOD to "🍕 Repas",
        com.guyghost.wakeve.chat.CommentSection.ACCOMMODATION to "🏠 Logement",
        com.guyghost.wakeve.chat.CommentSection.EQUIPMENT to "🎒 Équipement",
        com.guyghost.wakeve.chat.CommentSection.ACTIVITIES to "🎯 Activités"
    )
    
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(sections) { (section, label) ->
            SectionChip(
                label = label,
                isSelected = selectedSection == section,
                onClick = { onSectionChange(section) }
            )
        }
    }
}

/**
 * Individual section chip.
 */
@Composable
private fun SectionChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

/**
 * Attachment menu popup.
 */
@Composable
private fun AttachmentMenu(
    onDismiss: () -> Unit,
    onImageSelected: () -> Unit,
    onFileSelected: () -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(start = 48.dp, bottom = 8.dp),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        onClick = onDismiss
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AttachmentMenuItem(
                icon = Icons.Default.Image,
                label = "Image",
                onClick = {
                    onImageSelected()
                    onDismiss()
                }
            )
            AttachmentMenuItem(
                icon = Icons.Default.AttachFile,
                label = "Fichier",
                onClick = {
                    onFileSelected()
                    onDismiss()
                }
            )
        }
    }
}

/**
 * Individual attachment menu item.
 */
@Composable
private fun AttachmentMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Emoji picker component with common emojis.
 * 
 * @param onEmojiSelected Callback when an emoji is selected
 * @param modifier Modifier for the component
 */
@Composable
fun EmojiPicker(
    onEmojiSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val commonEmojis = listOf(
        "😀", "😂", "🥰", "😍", "🤔", "😎", "🙌", "👍", "❤️", "🔥",
        "🎉", "👏", "🙏", "💪", "🤝", "👀", "✅", "❌", "💯", "✨",
        "🎸", "🍕", "🍺", "☕", "🍷", "🥂", "🎁", "🎈", "🏆", "💡"
    )
    
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        LazyRow(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(commonEmojis) { emoji ->
                Text(
                    text = emoji,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onEmojiSelected(emoji) }
                        .padding(8.dp)
                        .semantics {
                            contentDescription = "Émoji $emoji"
                        }
                )
            }
        }
    }
}

/**
 * Action bubble for creating scenarios or quick actions.
 * 
 * @param label The action label
 * @param emoji The emoji icon
 * @param onClick Callback when the action is tapped
 * @param modifier Modifier for the component
 */
@Composable
fun ActionBubble(
    label: String,
    emoji: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}
