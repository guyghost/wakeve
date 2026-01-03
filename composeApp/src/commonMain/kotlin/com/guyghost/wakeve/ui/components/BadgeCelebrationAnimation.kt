package com.guyghost.wakeve.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.guyghost.wakeve.gamification.Badge
import com.guyghost.wakeve.gamification.BadgeRarity
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Badge Celebration Animation - Material You compliant celebration display.
 *
 * Features:
 * - Confetti particle system with 50 particles
 * - Pulsing badge animation with rotation
 * - Fade in/out transitions
 * - Automatic dismissal after duration
 * - Material 3 color scheme integration
 *
 * @param badge The badge being celebrated
 * @param modifier Modifier for the component
 * @param onAnimationComplete Callback when animation finishes
 */
@Composable
fun BadgeCelebrationAnimation(
    badge: Badge,
    modifier: Modifier = Modifier,
    durationMillis: Int = 3000,
    onAnimationComplete: () -> Unit = {}
) {
    var isVisible by remember { mutableStateOf(true) }

    // Launch auto-dismiss timer
    LaunchedEffect(Unit) {
        delay(durationMillis.toLong())
        isVisible = false
        delay(300) // Wait for fade out
        onAnimationComplete()
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(300, easing = EaseInOutCubic)) +
                scaleIn(tween(300, easing = EaseInOutCubic)),
        exit = fadeOut(tween(300, easing = EaseOutCubic)) +
                scaleOut(tween(300, easing = EaseOutCubic))
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            // Confetti particles layer
            ConfettiParticles(
                modifier = Modifier.fillMaxSize()
            )

            // Badge card with animations
            AnimatedBadgeCard(badge = badge)
        }
    }
}

/**
 * Animated badge card with pulsing and rotation effects.
 */
@Composable
private fun AnimatedBadgeCard(badge: Badge) {
    val infiniteTransition = rememberInfiniteTransition(label = "badgeCelebration")

    // Pulsing scale animation
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Gentle rotation animation
    val rotation by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gentleRotation"
    )

    // Get colors based on badge rarity
    val (containerColor, contentColor) = getBadgeColors(badge.rarity)

    Surface(
        modifier = Modifier
            .size(240.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = rotation
            },
        shape = CircleShape,
        color = containerColor,
        tonalElevation = 12.dp,
        shadowElevation = 16.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Badge icon with size based on rarity
            Text(
                text = badge.icon,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = MaterialTheme.typography.displayLarge.fontSize * 1.5f
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // "Badge unlocked" text
            Text(
                text = "Badge débloqué !",
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Badge name
            Text(
                text = badge.name,
                style = MaterialTheme.typography.headlineSmall,
                color = contentColor,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Badge description
            Text(
                text = badge.description,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            // Points reward indicator
            if (badge.pointsReward > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 4.dp
                ) {
                    Text(
                        text = "+${badge.pointsReward} points",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

/**
 * Confetti particle system with 50 animated particles.
 *
 * Features:
 * - Random initial positions
 * - Falling animation with gravity
 * - Color variation based on badge rarity
 * - Fade out at end of animation
 *
 * @param modifier Modifier for the component
 */
@Composable
fun ConfettiParticles(modifier: Modifier = Modifier) {
    val particleCount = 50
    val particles = remember {
        List(particleCount) { ConfettiParticle() }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "confetti")

    // Animate all particles
    particles.forEachIndexed { index, particle ->
        val progress by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = Random.nextInt(1500, 2500),
                    delayMillis = Random.nextInt(0, 500),
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "particle_$index"
        )
    }

    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        particles.forEach { particle ->
            val progress = particle.progress
            val x = particle.startX * canvasWidth + (canvasWidth * progress * particle.directionX)
            val y = particle.startY * canvasHeight - (canvasHeight * progress * 0.5f) +
                    (progress * progress * canvasHeight * 0.3f) // Gravity effect

            val alpha = (1f - progress).coerceIn(0f, 1f)
            val sizePx = particle.size.toPx() * (1f - progress * 0.5f)

            if (y >= 0 && y <= canvasHeight && x >= 0 && x <= canvasWidth && alpha > 0) {
                drawCircle(
                    color = particle.color,
                    radius = sizePx,
                    center = Offset(x, y),
                    alpha = alpha
                )
            }
        }
    }
}

/**
 * Data class representing a single confetti particle.
 */
private data class ConfettiParticle(
    val startX: Float = Random.nextFloat(),
    val startY: Float = Random.nextFloat() * 0.3f, // Start from top portion
    val directionX: Float = if (Random.nextBoolean()) 1f else -1f,
    val size: androidx.compose.ui.unit.Dp = Random.nextFloat().let {
        androidx.compose.ui.unit.Dp((4f + it * 10f))
    },
    val color: Color = getRandomConfettiColor(),
    val progress: Float = 0f
)

/**
 * Gets colors based on badge rarity for the celebration card.
 */
private fun getBadgeColors(rarity: BadgeRarity): Pair<Color, Color> {
    return when (rarity) {
        BadgeRarity.COMMON -> Pair(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        BadgeRarity.RARE -> Pair(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
        BadgeRarity.EPIC -> Pair(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        BadgeRarity.LEGENDARY -> Pair(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

/**
 * Gets a random confetti color from a festive palette.
 */
private fun getRandomConfettiColor(): Color {
    val colors = listOf(
        Color(0xFFFF6B6B), // Red
        Color(0xFF4ECDC4), // Teal
        Color(0xFFFFE66D), // Yellow
        Color(0xFF95E1D3), // Mint
        Color(0xFFF38181), // Coral
        Color(0xFFAA96DA), // Lavender
        Color(0xFFFCBF49), // Orange
        Color(0xFF2A9D8F)  // Green
    )
    return colors.random()
}

// Easing functions matching AIAnimations.kt patterns
private val EaseInOutCubic = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)
private val EaseOutCubic = CubicBezierEasing(0f, 0f, 1f, 1f)
private val LinearEasing = CubicBezierEasing(0f, 0f, 1f, 1f)

/**
 * Preview for BadgeCelebrationAnimation.
 */
@Composable
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
private fun BadgeCelebrationAnimationPreview() {
    MaterialTheme {
        val badge = Badge(
            id = "badge-super-organizer",
            name = "Super Organisateur",
            description = "A créé 10 événements",
            icon = "🏆",
            requirement = 10,
            pointsReward = 100,
            category = com.guyghost.wakeve.gamification.BadgeCategory.CREATION,
            rarity = BadgeRarity.EPIC
        )

        BadgeCelebrationAnimation(
            badge = badge,
            durationMillis = 5000,
            onAnimationComplete = {}
        )
    }
}
