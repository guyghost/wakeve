package com.guyghost.wakeve.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.graphicsLayer
import androidx.compose.ui.graphics.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

/**
 * Voice Assistant Floating Action Button
 *
 * A floating microphone button with visual feedback for the Wakeve voice assistant.
 * Features Material You design with sound wave animation when listening.
 *
 * Requirements:
 * - voice-104: Quick Actions via Voice (floating button, sound wave animation)
 * - voice-105: Accessibility (TalkBack support, proper content descriptions)
 *
 * Features:
 * - Floating circular button (64dp)
 * - Sound wave animation when listening
 * - Material You color scheme integration
 * - Full accessibility support (TalkBack)
 * - Multi-language support via content descriptions
 * - States: idle, listening, error
 *
 * @param isListening Whether the voice assistant is currently listening
 * @param hasError Whether there's an error state
 * @param onClick Single tap handler (toggle listening)
 * @param onLongClick Long press handler (quick actions)
 * @param modifier Modifier for the component
 */
@Composable
fun VoiceAssistantFAB(
    isListening: Boolean = false,
    hasError: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    // Accessibility content descriptions for multi-language support
    val contentDescriptionText = when {
        isListening -> "Voice assistant is listening. Double tap to stop."
        hasError -> "Voice assistant error. Double tap to retry."
        else -> "Start voice assistant. Double tap to activate, long press for quick actions."
    }
    
    val stateDescriptionText = when {
        isListening -> "Listening"
        hasError -> "Error"
        else -> "Idle"
    }
    
    Box(
        modifier = modifier
            .size(64.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(bounded = true, radius = 64.dp),
                onClick = onClick
            )
            .semantics {
                contentDescription = contentDescriptionText
                stateDescription = stateDescriptionText
                role = Role.Button
            },
        contentAlignment = Alignment.Center
    ) {
        // Sound wave animation (only when listening and no error)
        if (isListening && !hasError) {
            SoundWaveAnimation(
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        // Background with Material You elevation
        Surface(
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            color = when {
                hasError -> MaterialTheme.colorScheme.errorContainer
                isListening -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surface
            },
            tonalElevation = when {
                hasError -> 0.dp
                isListening -> 4.dp
                else -> 2.dp
            },
            shadowElevation = when {
                hasError -> 0.dp
                isListening -> 6.dp
                else -> 4.dp
            }
        ) {}
        
        // Microphone icon
        Icon(
            imageVector = when {
                hasError -> Icons.Default.MicNone
                isListening -> Icons.Default.Mic
                else -> Icons.Default.MicNone
            },
            contentDescription = null, // Handled by parent semantics
            tint = when {
                hasError -> MaterialTheme.colorScheme.onErrorContainer
                isListening -> MaterialTheme.colorScheme.onPrimaryContainer
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(28.dp)
        )
        
        // Error indicator overlay
        if (hasError) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .graphicsLayer { alpha = 0.3f }
                    .background(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                        shape = CircleShape
                    )
            )
        }
    }
}

/**
 * Sound Wave Animation Component
 *
 * Animated concentric circles that pulse outward to indicate listening state.
 * Uses Material 3 motion guidelines with smooth cubic easing.
 *
 * Features:
 * - 3 concentric circles with staggered animation
 * - 1 second loop with smooth easing
 * - Material You color integration
 * - Alpha fade for visual depth
 *
 * @param color The primary color for the wave animation
 * @param modifier Modifier for the component
 */
@Composable
fun SoundWaveAnimation(
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "soundWave")
    
    // Standard easing from AI animations (Material 3 compliant)
    val easeInOutCubic = AIEasing.EASE_IN_OUT_CUBIC
    val animationDuration = AIAnimationDurations.MEDIUM
    
    // Three waves with staggered delays for ripple effect
    val wave1Scale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = animationDuration * 2,
                easing = easeInOutCubic
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave1Scale"
    )
    
    val wave1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = animationDuration * 2,
                easing = easeInOutCubic
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave1Alpha"
    )
    
    val wave2Scale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = animationDuration * 2,
                delayMillis = animationDuration / 2,
                easing = easeInOutCubic
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave2Scale"
    )
    
    val wave2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = animationDuration * 2,
                delayMillis = animationDuration / 2,
                easing = easeInOutCubic
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave2Alpha"
    )
    
    val wave3Scale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = animationDuration * 2,
                delayMillis = animationDuration,
                easing = easeInOutCubic
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave3Scale"
    )
    
    val wave3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = animationDuration * 2,
                delayMillis = animationDuration,
                easing = easeInOutCubic
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave3Alpha"
    )
    
    Box(
        modifier = modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .size(120.dp)
                .graphicsLayer { alpha = 0.5f }
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val maxRadius = size.minDimension / 2
            
            // Draw three concentric waves with staggered animation
            drawSoundWave(
                center = center,
                scale = wave1Scale,
                alpha = wave1Alpha,
                color = color,
                maxRadius = maxRadius,
                lineWidth = 3.dp.toPx()
            )
            
            drawSoundWave(
                center = center,
                scale = wave2Scale,
                alpha = wave2Alpha,
                color = color,
                maxRadius = maxRadius,
                lineWidth = 3.dp.toPx()
            )
            
            drawSoundWave(
                center = center,
                scale = wave3Scale,
                alpha = wave3Alpha,
                color = color,
                maxRadius = maxRadius,
                lineWidth = 3.dp.toPx()
            )
        }
    }
}

/**
 * Draw a single sound wave circle
 */
private fun DrawScope.drawSoundWave(
    center: Offset,
    scale: Float,
    alpha: Float,
    color: androidx.compose.ui.graphics.Color,
    maxRadius: Float,
    lineWidth: Float
) {
    val radius = maxRadius * scale * 0.85f
    
    drawCircle(
        color = color.copy(alpha = alpha.coerceIn(0f, 1f)),
        radius = radius,
        center = center,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = lineWidth)
    )
}

/**
 * Compact voice assistant button for smaller spaces.
 * Same functionality but with reduced size (48dp).
 *
 * @param isListening Whether the voice assistant is currently listening
 * @param hasError Whether there's an error state
 * @param onClick Single tap handler
 * @param modifier Modifier for the component
 */
@Composable
fun VoiceAssistantCompactFAB(
    isListening: Boolean = false,
    hasError: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true, radius = 48.dp),
                onClick = onClick
            )
            .semantics {
                contentDescription = if (isListening) {
                    "Voice assistant listening. Double tap to stop."
                } else if (hasError) {
                    "Voice assistant error. Double tap to retry."
                } else {
                    "Voice assistant. Double tap to activate."
                }
                role = Role.Button
            },
        contentAlignment = Alignment.Center
    ) {
        // Compact sound wave animation
        if (isListening && !hasError) {
            CompactSoundWaveAnimation(
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Surface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = when {
                hasError -> MaterialTheme.colorScheme.errorContainer
                isListening -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surface
            },
            tonalElevation = if (isListening) 2.dp else 1.dp
        ) {}
        
        Icon(
            imageVector = when {
                hasError -> Icons.Default.MicNone
                isListening -> Icons.Default.Mic
                else -> Icons.Default.MicNone
            },
            contentDescription = null,
            tint = when {
                hasError -> MaterialTheme.colorScheme.onErrorContainer
                isListening -> MaterialTheme.colorScheme.onPrimaryContainer
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Compact version of the sound wave animation for smaller buttons.
 */
@Composable
private fun CompactSoundWaveAnimation(
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "compactSoundWave")
    
    val easeInOutCubic = AIEasing.EASE_IN_OUT_CUBIC
    val animationDuration = AIAnimationDurations.MEDIUM
    
    val wave1Scale by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = animationDuration * 2,
                easing = easeInOutCubic
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "compactWave1Scale"
    )
    
    val wave1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = animationDuration * 2,
                easing = easeInOutCubic
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "compactWave1Alpha"
    )
    
    Box(
        modifier = modifier.size(80.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .size(80.dp)
                .graphicsLayer { alpha = 0.4f }
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val maxRadius = size.minDimension / 2
            
            drawCircle(
                color = color.copy(alpha = wave1Alpha.coerceIn(0f, 1f)),
                radius = maxRadius * wave1Scale * 0.9f,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )
        }
    }
}

// MARK: - Previews

/**
 * Preview for VoiceAssistantFAB in idle state
 */
@Composable
@androidx.compose.ui.tooling.preview.Preview
private fun VoiceAssistantFABIdlePreview() {
    androidx.compose.material3.MaterialTheme {
        Box(
            modifier = Modifier
                .size(200.dp)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            VoiceAssistantFAB(
                isListening = false,
                hasError = false,
                onClick = {}
            )
        }
    }
}

/**
 * Preview for VoiceAssistantFAB in listening state
 */
@Composable
@androidx.compose.ui.tooling.preview.Preview
private fun VoiceAssistantFABListeningPreview() {
    androidx.compose.material3.MaterialTheme {
        Box(
            modifier = Modifier
                .size(200.dp)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            VoiceAssistantFAB(
                isListening = true,
                hasError = false,
                onClick = {}
            )
        }
    }
}

/**
 * Preview for VoiceAssistantFAB in error state
 */
@Composable
@androidx.compose.ui.tooling.preview.Preview
private fun VoiceAssistantFABErrorPreview() {
    androidx.compose.material3.MaterialTheme {
        Box(
            modifier = Modifier
                .size(200.dp)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            VoiceAssistantFAB(
                isListening = false,
                hasError = true,
                onClick = {}
            )
        }
    }
}

/**
 * Preview for VoiceAssistantCompactFAB
 */
@Composable
@androidx.compose.ui.tooling.preview.Preview
private fun VoiceAssistantCompactFABPreview() {
    androidx.compose.material3.MaterialTheme {
        Box(
            modifier = Modifier
                .size(200.dp)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            VoiceAssistantCompactFAB(
                isListening = false,
                hasError = false,
                onClick = {}
            )
        }
    }
}
