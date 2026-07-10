package com.guyghost.wakeve.navigation

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.guyghost.wakeve.R
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import com.guyghost.wakeve.ui.designsystem.WakeveNavigationKind
import com.guyghost.wakeve.ui.designsystem.WakeveAdaptiveInfo
import com.guyghost.wakeve.ui.designsystem.calculateWakeveAdaptiveInfo

@Composable
fun WakeveAdaptiveNavigationScaffold(
    navController: NavController,
    showNavigation: Boolean,
    inboxUnreadCount: Int,
    modifier: Modifier = Modifier,
    adaptiveInfoOverride: WakeveAdaptiveInfo? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        val adaptiveInfo = adaptiveInfoOverride
            ?: calculateWakeveAdaptiveInfo(
                widthDp = maxWidth.value.toInt(),
                heightDp = maxHeight.value.toInt()
            )
        val useRail = showNavigation && adaptiveInfo.navigationKind == WakeveNavigationKind.Rail
        val useBottomBar = showNavigation &&
            adaptiveInfo.navigationKind == WakeveNavigationKind.BottomBar &&
            !adaptiveInfo.useCompactChrome

        if (useRail) {
            Row {
                WakeveNavigationRail(
                    navController = navController,
                    inboxUnreadCount = inboxUnreadCount
                )
                Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
                    content(padding)
                }
            }
        } else {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    if (useBottomBar) {
                        WakeveNavigationBar(
                            navController = navController,
                            inboxUnreadCount = inboxUnreadCount
                        )
                    }
                }
            ) { padding ->
                content(padding)
            }
        }
    }
}

@Composable
private fun WakeveNavigationBar(
    navController: NavController,
    inboxUnreadCount: Int
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar(
        modifier = Modifier.testTag("wakeve_bottom_navigation"),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        wakevePrimaryDestinations().forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
            NavigationBarItem(
                icon = { WakeveNavIcon(item = item, inboxUnreadCount = inboxUnreadCount) },
                label = { Text(text = item.label, style = MaterialTheme.typography.labelMedium) },
                selected = selected,
                onClick = { navController.navigatePrimary(item.screen.route) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun WakeveNavigationRail(
    navController: NavController,
    inboxUnreadCount: Int
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationRail(
        modifier = Modifier
            .fillMaxHeight()
            .testTag("wakeve_navigation_rail"),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        wakevePrimaryDestinations().forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
            NavigationRailItem(
                icon = { WakeveNavIcon(item = item, inboxUnreadCount = inboxUnreadCount) },
                label = { Text(text = item.label, style = MaterialTheme.typography.labelMedium) },
                selected = selected,
                onClick = { navController.navigatePrimary(item.screen.route) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun WakeveNavIcon(
    item: WakevePrimaryDestination,
    inboxUnreadCount: Int
) {
    if (item.screen == Screen.Inbox && inboxUnreadCount > 0) {
        BadgedBox(
            badge = {
                Badge {
                    Text(text = if (inboxUnreadCount > 99) "99+" else inboxUnreadCount.toString())
                }
            }
        ) {
            Icon(imageVector = item.icon, contentDescription = item.contentDescription)
        }
    } else {
        Icon(imageVector = item.icon, contentDescription = item.contentDescription)
    }
}

private fun NavController.navigatePrimary(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

private data class WakevePrimaryDestination(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
    val contentDescription: String
)

@Composable
private fun wakevePrimaryDestinations() = listOf(
    WakevePrimaryDestination(
        screen = Screen.Home,
        label = stringResource(R.string.nav_upcoming),
        icon = Icons.Filled.Home,
        contentDescription = stringResource(R.string.a11y_nav_upcoming)
    ),
    WakevePrimaryDestination(
        screen = Screen.Inbox,
        label = stringResource(R.string.notifications),
        icon = Icons.Filled.Inbox,
        contentDescription = stringResource(R.string.a11y_nav_notifications)
    ),
    WakevePrimaryDestination(
        screen = Screen.Explore,
        label = stringResource(R.string.tab_ideas),
        icon = Icons.Filled.Search,
        contentDescription = stringResource(R.string.a11y_nav_ideas)
    )
)
