package com.guyghost.wakeve.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.guyghost.wakeve.chat.TypingIndicator

/**
 * TypingIndicatorRow - Displays animated typing indicators for users who are typing.
 * 
 * Shows:
 * - Names of users currently typing
 * - Animated "..." dots
 * - Count when more than 2 users are typing
 * 
 * @param typingUsers List of users currently typing
 * @param modifier Modifier for the component
 */
@Composable
fun TypingIndicatorRow(
    typingUsers: List<TypingIndicator>,
    modifier: Modifier = Modifier
) {
    if (typingUsers.isEmpty()) return
    
    val typingText = when {
        typingUsers.size == 1 -> "${typingUsers[0].userName} est en train d'écrire..."
        typingUsers.size == 2 -> "${typingUsers[0].userName} et ${typingUsers[1].userName} sont en train d'écrire..."
        else -> "${typingUsers[0].userName}, ${typingUsers[1].userName} et ${typingUsers.size - 2} autres sont en train d'écrire..."
    }
    
    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + slideInVertically { -it },
        exit = fadeOut() + slideOutVertically { -it },
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Animated typing dots
            TypingDots(
                modifier = Modifier.size(24.dp)
            )
            
            // Typing text
            Text(
                text = typingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Animated typing dots indicator.
 * 
 * Three dots that pulse in sequence to indicate typing activity.
 */
@Composable
fun TypingDots(
    modifier: Modifier = Modifier,
    dotSize: Int = 6,
    animationDuration: Int = 600
) {
    val dotColor = MaterialTheme.colorScheme.primary
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            TypingDot(
                color = dotColor,
                size = dotSize.dp,
                delay = index * 150,
                duration = animationDuration
            )
        }
    }
}

/**
 * Individual animated typing dot.
 */
@Composable
private fun TypingDot(
    color: androidx.compose.ui.graphics.Color,
    size: androidx.compose.ui.unit.Dp,
    delay: Int,
    duration: Int
) {
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "typingDot")
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(
                durationMillis = duration,
                delayMillis = delay,
                easing = androidx.compose.animation.core.FastOutSlowInEasing
            ),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(
                durationMillis = duration,
                delayMillis = delay,
                easing = androidx.compose.animation.core.FastOutSlowInEasing
            ),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "dotScale"
    )
    
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha))
    )
}

/**
 * Online participants indicator showing who's currently in the chat.
 * 
 * @param participants List of participants with their online status
 * @param currentUserId The current user's ID
 * @param modifier Modifier for the component
 */
@Composable
fun OnlineParticipantsIndicator(
    participants: List<com.guyghost.wakeve.chat.ChatParticipant>,
    currentUserId: String,
    modifier: Modifier = Modifier
) {
    val onlineCount = participants.count { it.isOnline && it.userId != currentUserId }
    
    if (onlineCount == 0) return
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Online indicator dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiary)
        )
        
        Text(
            text = "$onlineCount participant${if (onlineCount > 1) "s" else ""} en ligne",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary
        )
    }
}

/**
 * Connection status banner for showing online/offline state.
 * 
 * @param isConnected Whether the chat is connected
 * @param modifier Modifier for the component
 */
@Composable
fun ConnectionStatusBanner(
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateFloatAsState(
        targetValue = if (isConnected) 0f else 0.1f,
        animationSpec = tween(300),
        label = "connectionBannerAlpha"
    )
    
    AnimatedVisibility(
        visible = !isConnected,
        enter = fadeIn() + slideInVertically { -it },
        exit = fadeOut() + slideOutVertically { -it },
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = if (backgroundColor > 0) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = backgroundColor)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔴 Hors ligne - Les messages seront envoyés automatiquement",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Section header for organizing chat messages by category.
 * 
 * @param section The section name to display
 * @param modifier Modifier for the component
 */
@Composable
fun ChatSectionHeader(
    section: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Section line
        Spacer(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        )
        
        // Section label
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = section,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
        
        Spacer(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        )
    }
}

/**
 * Date separator for grouping messages by date.
 * 
 * @param date The date to display
 * @param modifier Modifier for the component
 */
@Composable
fun DateSeparator(
    date: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Text(
                text = date,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

/**
 * Quick reply suggestions that appear above the input bar.
 * 
 * @param suggestions List of quick reply options
 * @param onSuggestionClick Callback when a suggestion is tapped
 * @param modifier Modifier for the component
 */
@Composable
fun QuickReplySuggestions(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (suggestions.isEmpty()) return
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        suggestions.take(3).forEach { suggestion ->
            Surface(
                onClick = { onSuggestionClick(suggestion) },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = suggestion,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
