package com.guyghost.wakeve.navigation

import android.content.Intent
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import com.guyghost.wakeve.theme.WakeveTheme
import com.guyghost.wakeve.ui.designsystem.calculateWakeveAdaptiveInfo
import com.guyghost.wakeve.ui.event.ComposeTestHostActivity
import org.junit.After
import org.junit.Rule
import org.junit.Test

class WakeveAdaptiveNavigationScaffoldTest {
    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val scenarios = mutableListOf<ActivityScenario<ComposeTestHostActivity>>()

    @After
    fun tearDown() {
        scenarios.forEach { it.close() }
        scenarios.clear()
    }

    @Test
    fun compactWidthShowsBottomNavigation() {
        setContent(widthDp = 393, heightDp = 852)

        composeTestRule.onNodeWithTag("wakeve_bottom_navigation").assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("wakeve_navigation_rail").assertCountEquals(0)
    }

    @Test
    fun mediumWidthShowsNavigationRail() {
        setContent(widthDp = 840, heightDp = 760)

        composeTestRule.onNodeWithTag("wakeve_navigation_rail").assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("wakeve_bottom_navigation").assertCountEquals(0)
    }

    @Test
    fun compactHeightCollapsesBottomNavigationChrome() {
        setContent(widthDp = 393, heightDp = 390)

        composeTestRule.onAllNodesWithTag("wakeve_bottom_navigation").assertCountEquals(0)
        composeTestRule.onAllNodesWithTag("wakeve_navigation_rail").assertCountEquals(0)
    }

    private fun setContent(widthDp: Int, heightDp: Int) {
        val intent = Intent().setClassName(
            "com.guyghost.wakeve",
            ComposeTestHostActivity::class.java.name
        )
        val scenario = ActivityScenario.launch<ComposeTestHostActivity>(intent)
        scenarios += scenario
        scenario.onActivity { activity ->
            activity.setContent {
                WakeveTheme(dynamicColor = false) {
                    Box(
                        modifier = Modifier
                            .width(widthDp.dp)
                            .height(heightDp.dp)
                    ) {
                        val navController = androidx.navigation.compose.rememberNavController()
                        WakeveAdaptiveNavigationScaffold(
                            navController = navController,
                            showNavigation = true,
                            inboxUnreadCount = 2,
                            modifier = Modifier.fillMaxSize(),
                            adaptiveInfoOverride = calculateWakeveAdaptiveInfo(widthDp, heightDp)
                        ) {
                            Text("Content")
                        }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
    }
}
