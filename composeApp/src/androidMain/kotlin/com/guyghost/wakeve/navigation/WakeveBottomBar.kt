package com.guyghost.wakeve.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState

/**
 * Bottom Navigation Bar for Wakeve Android app.
 * 
 * Implements Material You design with 4 tabs:
 * - Home: Main dashboard with event overview
 * - Events: Filtered list of events (À venir / En cours / Passés)
 * - Explore: Featured events, templates, tips
 * - Profile: User info, settings, inbox link
 * 
 * @param navController The navigation controller for routing
 */
@Composable
fun WakeveBottomBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    // Define bottom navigation items
    val items = listOf(
        BottomNavItem(
            screen = Screen.Home,
            label = "Accueil",
            icon = Icons.Filled.Home,
            contentDescription = "Accueil - Tableau de bord principal"
        ),
        BottomNavItem(
            screen = Screen.Inbox,
            label = "Inbox",
            icon = Icons.Filled.Notifications,
            contentDescription = "Inbox - Notifications, tâches et messages"
        ),
        BottomNavItem(
            screen = Screen.Explore,
            label = "Explorer",
            icon = Icons.Filled.Search,
            contentDescription = "Explorer - Découvrir des événements et modèles"
        ),
        BottomNavItem(
            screen = Screen.Profile,
            label = "Profil",
            icon = Icons.Filled.Person,
            contentDescription = "Profil - Paramètres et informations utilisateur"
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
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.contentDescription
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                selected = selected,
                onClick = {
                    // Navigate and clear back stack to avoid stack buildup
                    navController.navigate(item.screen.route) {
                        // Pop up to the start destination to avoid building up a large stack
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination
                        launchSingleTop = true
                        // Restore state when reselecting a previously selected item
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
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val contentDescription: String
)
