package com.guyghost.wakeve.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

/**
 * AI Animation Utilities - Shared animation utilities for AI components.
 *
 * Provides consistent animations across AI badges, cards, and lists:
 * - Fade in/out with easing
 * - Expand/shrink animations
 * - Slide animations
 * - Staggered entrance animations
 */

// Standard easing for AI animations (Material Design 3 compliant)
private val EaseInOutCubic = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)
private val EaseOutCubic = CubicBezierEasing(0f, 0f, 1f, 1f)
private val EaseInCubic = CubicBezierEasing(0.5f, 0f, 1f, 1f)

/**
 * Animated visibility wrapper for AI badge appearance.
 *
 * Features:
 * - Fade in with cubic easing (300ms)
 * - Vertical expand with cubic easing (300ms)
 * - Reverse animation on exit
 *
 * @param visible Whether the content is visible
 * @param modifier Modifier for the component
 * @param content Content to animate
 */
@Composable
fun AIBadgeAnimation(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = 300,
                easing = EaseInOutCubic
            )
        ) + expandVertically(
            animationSpec = tween(
                durationMillis = 300,
                easing = EaseInOutCubic
            )
        ),
        exit = fadeOut(
            animationSpec = tween(
                durationMillis = 200,
                easing = EaseOutCubic
            )
        ) + shrinkVertically(
            animationSpec = tween(
                durationMillis = 200,
                easing = EaseOutCubic
            )
        )
    ) {
        content()
    }
}

/**
 * Animated visibility with slide effect for suggestion cards.
 *
 * Features:
 * - Slide in from bottom (300ms)
 * - Fade in with slide (300ms)
 * - Slide out to bottom (200ms)
 *
 * @param visible Whether the content is visible
 * @param modifier Modifier for the component
 * @param content Content to animate
 */
@Composable
fun AISlideInAnimation(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(
                durationMillis = 300,
                easing = EaseInOutCubic
            )
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = 300,
                easing = EaseInOutCubic
            )
        ),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(
                durationMillis = 200,
                easing = EaseOutCubic
            )
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = 200,
                easing = EaseOutCubic
            )
        )
    ) {
        content()
    }
}

/**
 * Fade animation with custom duration.
 *
 * @param visible Whether the content is visible
 * @param durationMillis Animation duration in milliseconds
 * @param modifier Modifier for the component
 * @param content Content to animate
 */
@Composable
fun AIFadeAnimation(
    visible: Boolean,
    durationMillis: Int = 300,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = EaseInOutCubic
            )
        ),
        exit = fadeOut(
            animationSpec = tween(
                durationMillis = durationMillis / 2,
                easing = EaseOutCubic
            )
        )
    ) {
        content()
    }
}

/**
 * Scale and fade animation for emphasis.
 *
 * @param visible Whether the content is visible
 * @param modifier Modifier for the component
 * @param content Content to animate
 */
@Composable
fun AIScaleFadeAnimation(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = 250,
                easing = EaseInOutCubic
            )
        ) + expandHorizontally(
            expandFrom = androidx.compose.ui.Alignment.End,
            animationSpec = tween(
                durationMillis = 250,
                easing = EaseInOutCubic
            )
        ),
        exit = fadeOut(
            animationSpec = tween(
                durationMillis = 150,
                easing = EaseOutCubic
            )
        ) + shrinkHorizontally(
            shrinkTowards = androidx.compose.ui.Alignment.End,
            animationSpec = tween(
                durationMillis = 150,
                easing = EaseOutCubic
            )
        )
    ) {
        content()
    }
}

/**
 * Staggered animation for list items.
 * Each item animates with a delay based on its index.
 *
 * @param index Index of the item in the list
 * @param baseDelayMs Base delay in milliseconds
 * @param visible Whether the content is visible
 * @param modifier Modifier for the component
 * @param content Content to animate
 */
@Composable
fun AIStaggeredAnimation(
    index: Int,
    baseDelayMs: Int = 50,
    visible: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val delayMillis = index * baseDelayMs

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = 300,
                delayMillis = delayMillis,
                easing = EaseInOutCubic
            )
        ) + slideInVertically(
            initialOffsetY = { it / 4 },
            animationSpec = tween(
                durationMillis = 300,
                delayMillis = delayMillis,
                easing = EaseInOutCubic
            )
        ),
        exit = fadeOut(
            animationSpec = tween(
                durationMillis = 150,
                easing = EaseOutCubic
            )
        )
    ) {
        content()
    }
}

/**
 * Pulsing animation for AI indicators.
 *
 * @param isPulsing Whether the animation is active
 * @param modifier Modifier for the component
 * @param content Content to animate
 */
@Composable
fun AIPulsingAnimation(
    isPulsing: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val alpha by animateFloatAsState(
        targetValue = if (isPulsing) 0.6f else 1f,
        animationSpec = tween(
            durationMillis = 1000,
            easing = EaseInOutCubic
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier.alpha(alpha)
    ) {
        content()
    }
}

/**
 * Highlight animation for important AI suggestions.
 *
 * @param isHighlighted Whether the component is highlighted
 * @param modifier Modifier for the component
 * @param content Content to animate
 */
@Composable
fun AIHighlightAnimation(
    isHighlighted: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val offset by animateFloatAsState(
        targetValue = if (isHighlighted) 4f else 0f,
        animationSpec = tween(
            durationMillis = 150,
            easing = EaseOutCubic
        ),
        label = "highlightOffset"
    )

    Column(
        modifier = modifier.offset { IntOffset(offset.toInt(), offset.toInt()) }
    ) {
        content()
    }
}

/**
 * Animation duration constants for consistency.
 */
object AIAnimationDurations {
    const val SHORT = 150
    const val MEDIUM = 300
    const val LONG = 450
    const val STAGGER_DELAY = 50
}

/**
 * Animation easing constants for consistency.
 */
object AIEasing {
    val EASE_IN_OUT_CUBIC = EaseInOutCubic
    val EASE_OUT_CUBIC = EaseOutCubic
    val EASE_IN_CUBIC = EaseInCubic
}
