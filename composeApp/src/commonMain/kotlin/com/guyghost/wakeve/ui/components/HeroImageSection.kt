package com.guyghost.wakeve.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus

/**
 * Hero image section displaying event banner image
 *
 * Matches iOS HeroImageSection functionality:
 * - Displays hero image if available
 * - Shows gradient overlay for text readability
 * - Falls back to gradient background if no image
 *
 * @param event The event to display
 * @param modifier Modifier for customizing the layout
 */
@Composable
fun HeroImageSection(
    event: Event,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
    ) {
        // Gradient background based on event status
        // TODO: Add image loading when coil-compose is available for KMP
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            getEventStatusGradientColor(event.status),
                            getEventStatusGradientColor(event.status).copy(alpha = 0.7f)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )
    }
}

/**
 * Get gradient color for event status
 */
private fun getEventStatusGradientColor(status: EventStatus): Color {
    return when (status) {
        EventStatus.DRAFT -> Color(0xFF9E9E9E)
        EventStatus.POLLING -> Color(0xFFBB86FC)
        EventStatus.CONFIRMED -> Color(0xFF4DB6AC)
        EventStatus.ORGANIZING -> Color(0xFFFFB74D)
        EventStatus.FINALIZED -> Color(0xFF81C784)
        else -> Color(0xFFE0E0E0)
    }
}
