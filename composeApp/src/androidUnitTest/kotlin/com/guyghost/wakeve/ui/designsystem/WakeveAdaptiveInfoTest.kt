package com.guyghost.wakeve.ui.designsystem

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WakeveAdaptiveInfoTest {
    @Test
    fun compactWidthUsesBottomNavigationScrollableFiltersAndSingleColumn() {
        val info = calculateWakeveAdaptiveInfo(widthDp = 393, heightDp = 852)

        assertEquals(WakeveWindowClass.Compact, info.widthClass)
        assertEquals(WakeveNavigationKind.BottomBar, info.navigationKind)
        assertEquals(WakeveFilterLayout.Scrollable, info.filterLayout)
        assertEquals(1, info.eventGridColumns)
        assertFalse(info.supportsListDetail)
    }

    @Test
    fun mediumWidthUsesRailWrappedFiltersAndTwoColumns() {
        val info = calculateWakeveAdaptiveInfo(widthDp = 673, heightDp = 841)

        assertEquals(WakeveWindowClass.Medium, info.widthClass)
        assertEquals(WakeveNavigationKind.Rail, info.navigationKind)
        assertEquals(WakeveFilterLayout.Wrapped, info.filterLayout)
        assertEquals(2, info.eventGridColumns)
        assertTrue(info.supportsListDetail)
    }

    @Test
    fun desktopWidthUsesThreeOrMoreColumns() {
        val info = calculateWakeveAdaptiveInfo(widthDp = 1440, heightDp = 960)

        assertEquals(WakeveWindowClass.Expanded, info.widthClass)
        assertEquals(WakeveNavigationKind.Rail, info.navigationKind)
        assertTrue(info.eventGridColumns >= 3)
        assertTrue(info.supportsListDetail)
    }

    @Test
    fun compactHeightDisablesListDetailAndRequestsCompactChrome() {
        val info = calculateWakeveAdaptiveInfo(widthDp = 820, heightDp = 390)

        assertEquals(WakeveHeightClass.Compact, info.heightClass)
        assertFalse(info.supportsListDetail)
        assertTrue(info.useCompactChrome)
    }
}
