package com.guyghost.wakeve.navigation

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
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.guyghost.wakeve.R
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState

/**
 * Bottom Navigation Bar for Wakeve Android app.
 *
 * Implements Material You design with 3 tabs:
 * - À venir: Upcoming event overview
 * - Notifications: Event alerts and updates (with unread badge)
 * - Explore: Featured events, templates, tips
 *
 * @param navController The navigation controller for routing
 * @param inboxUnreadCount Number of unread items to display as badge on Inbox tab
 */
@Composable
fun WakeveBottomBar(
    navController: NavController,
    inboxUnreadCount: Int = 0
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Define bottom navigation items
    val items = listOf(
        BottomNavItem(
            screen = Screen.Home,
            label = stringResource(R.string.nav_upcoming),
            icon = Icons.Filled.Home,
            contentDescriptionRes = R.string.a11y_nav_upcoming
        ),
        BottomNavItem(
            screen = Screen.Inbox,
            label = stringResource(R.string.notifications),
            icon = Icons.Filled.Inbox,
            contentDescriptionRes = R.string.a11y_nav_notifications
        ),
        BottomNavItem(
            screen = Screen.Explore,
            label = stringResource(R.string.tab_ideas),
            icon = Icons.Filled.Search,
            contentDescriptionRes = R.string.a11y_nav_ideas
        )
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        items.forEach { item ->
            val selected = currentDestination?.hierarchy?.any {
                it.route == item.screen.route
            } == true

            NavigationBarItem(
                icon = {
                    if (item.screen == Screen.Inbox && inboxUnreadCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge {
                                    Text(
                                        text = if (inboxUnreadCount > 99) "99+" else inboxUnreadCount.toString()
                                    )
                                }
                            }
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = stringResource(
                                    item.contentDescriptionRes,
                                    stringResource(if (selected) R.string.a11y_selected else R.string.a11y_unselected)
                                )
                            )
                        }
                    } else {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = stringResource(
                                item.contentDescriptionRes,
                                stringResource(if (selected) R.string.a11y_selected else R.string.a11y_unselected)
                            )
                        )
                    }
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                selected = selected,
                onClick = {
                    navController.navigate(item.screen.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
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

/**
 * Data class representing a bottom navigation item.
 */
private data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
    val contentDescriptionRes: Int
)
