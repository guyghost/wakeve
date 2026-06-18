package com.guyghost.wakeve.ui.designsystem

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object WakeveSpacing {
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 16.dp
    val lg: Dp = 24.dp
    val xl: Dp = 32.dp
    val xxl: Dp = 48.dp
}

object WakeveSize {
    val minTouchTarget: Dp = 48.dp
    val progressIndicator: Dp = 20.dp
    val compactListItemHeight: Dp = 88.dp
    val eventListPaneWidth: Dp = 360.dp
    val maxReadableWidth: Dp = 920.dp
}

object WakeveElevation {
    val level0: Dp = 0.dp
    val level1: Dp = 1.dp
    val level2: Dp = 3.dp
    val level3: Dp = 6.dp
}

val WakeveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

enum class WakeveWindowClass {
    Compact,
    Medium,
    Expanded
}

enum class WakeveHeightClass {
    Compact,
    Medium,
    Expanded
}

enum class WakevePointerPrecision {
    Coarse,
    Fine
}

enum class WakeveNavigationKind {
    BottomBar,
    Rail
}

enum class WakeveFilterLayout {
    Scrollable,
    Wrapped
}

data class WakeveAdaptiveInfo(
    val widthDp: Int,
    val heightDp: Int,
    val widthClass: WakeveWindowClass,
    val heightClass: WakeveHeightClass,
    val pointerPrecision: WakevePointerPrecision,
    val navigationKind: WakeveNavigationKind,
    val filterLayout: WakeveFilterLayout,
    val eventGridColumns: Int,
    val supportsListDetail: Boolean,
    val useCompactChrome: Boolean
)

fun calculateWakeveAdaptiveInfo(
    widthDp: Int,
    heightDp: Int,
    pointerPrecision: WakevePointerPrecision = WakevePointerPrecision.Coarse
): WakeveAdaptiveInfo {
    val widthClass = when {
        widthDp < 600 -> WakeveWindowClass.Compact
        widthDp < 840 -> WakeveWindowClass.Medium
        else -> WakeveWindowClass.Expanded
    }
    val heightClass = when {
        heightDp < 480 -> WakeveHeightClass.Compact
        heightDp < 900 -> WakeveHeightClass.Medium
        else -> WakeveHeightClass.Expanded
    }
    val columns = when {
        widthDp < 600 -> 1
        widthDp < 960 -> 2
        else -> (widthDp / 320).coerceAtLeast(3)
    }

    return WakeveAdaptiveInfo(
        widthDp = widthDp,
        heightDp = heightDp,
        widthClass = widthClass,
        heightClass = heightClass,
        pointerPrecision = pointerPrecision,
        navigationKind = if (widthClass == WakeveWindowClass.Compact) {
            WakeveNavigationKind.BottomBar
        } else {
            WakeveNavigationKind.Rail
        },
        filterLayout = if (widthDp >= 600) WakeveFilterLayout.Wrapped else WakeveFilterLayout.Scrollable,
        eventGridColumns = columns,
        supportsListDetail = widthClass != WakeveWindowClass.Compact && heightClass != WakeveHeightClass.Compact,
        useCompactChrome = heightClass == WakeveHeightClass.Compact
    )
}

@Composable
fun WakeveAdaptivePane(
    modifier: Modifier = Modifier,
    compact: @Composable (WakeveAdaptiveInfo) -> Unit,
    expanded: @Composable (WakeveAdaptiveInfo) -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        val adaptiveInfo = calculateWakeveAdaptiveInfo(
            widthDp = maxWidth.value.toInt(),
            heightDp = maxHeight.value.toInt()
        )

        if (adaptiveInfo.supportsListDetail) {
            expanded(adaptiveInfo)
        } else {
            compact(adaptiveInfo)
        }
    }
}
