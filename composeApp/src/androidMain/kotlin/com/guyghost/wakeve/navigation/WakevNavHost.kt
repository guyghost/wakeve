package com.guyghost.wakeve.navigation

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.guyghost.wakeve.EventCreationScreen
import com.guyghost.wakeve.EventDetailScreen
import com.guyghost.wakeve.ExploreTabScreen
import com.guyghost.wakeve.GetStartedScreen
import com.guyghost.wakeve.HomeScreen
import com.guyghost.wakeve.InboxScreen
import com.guyghost.wakeve.LoginScreen
import com.guyghost.wakeve.OnboardingScreen
import com.guyghost.wakeve.ProfileTabScreen
import com.guyghost.wakeve.SettingsScreen
import com.guyghost.wakeve.SplashScreen
import com.guyghost.wakeve.viewmodel.EventManagementViewModel
import com.guyghost.wakeve.viewmodel.ScenarioManagementViewModel
import org.koin.compose.koinInject

/**
 * Navigation host for the Wakeve Android app.
 * 
 * Defines all navigation destinations and their composable screens.
 * Handles route parameters extraction and screen composition.
 * 
 * @param navController The navigation controller for routing
 * @param modifier Modifier for customizing the layout
 * @param startDestination The initial destination route
 * @param userId The current authenticated user ID
 */
@Composable
fun WakevNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Home.route,
    userId: String
) {
    val context = LocalContext.current
    
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // ========================================
        // BOTTOM NAVIGATION TABS
        // ========================================
        
        composable(Screen.Home.route) {
            val viewModel: EventManagementViewModel = koinInject()
            HomeScreen(
                viewModel = viewModel,
                onNavigateTo = { route ->
                    navController.navigate(route)
                },
                onShowToast = { message ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            )
        }
        
        // Events tab removed - functionality moved to Home tab
        // Users can access event list from Home screen
        
        composable(Screen.Explore.route) {
            ExploreTabScreen(
                onEventClick = { eventId ->
                    navController.navigate(Screen.EventDetail.createRoute(eventId))
                }
            )
        }
        
        composable(Screen.Profile.route) {
            ProfileTabScreen(
                userId = userId,
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToInbox = {
                    navController.navigate(Screen.Inbox.route)
                },
                onSignOut = {
                    // Clear back stack and navigate to login
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        // ========================================
        // AUTH & ONBOARDING
        // ========================================
        
        composable(Screen.Splash.route) {
            SplashScreen(
                onAnimationComplete = {
                    // Navigation handled in App.kt based on auth state
                }
            )
        }
        
        composable(Screen.GetStarted.route) {
            GetStartedScreen(
                onGetStarted = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.GetStarted.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Login.route) {
            LoginScreen(
                onGoogleSignIn = {
                    // TODO: Implement Google Sign-In
                },
                onAppleSignIn = {
                    // Not available on Android
                }
            )
        }
        
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onOnboardingComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        
        // ========================================
        // EVENT MANAGEMENT
        // ========================================
        
        composable(Screen.EventCreation.route) {
            EventCreationScreen(
                userId = userId,
                onEventCreated = { event ->
                    navController.navigate(Screen.ParticipantManagement.createRoute(event.id)) {
                        popUpTo(Screen.EventCreation.route) { inclusive = true }
                    }
                },
                onBack = {
                    navController.navigateUp()
                }
            )
        }
        
        composable(
            route = Screen.EventDetail.route,
            arguments = listOf(navArgument("eventId") { type = NavType.StringType })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            val viewModel: EventManagementViewModel = koinInject()
            
            EventDetailScreen(
                eventId = eventId,
                viewModel = viewModel,
                onNavigateTo = { route ->
                    navController.navigate(route)
                },
                onShowToast = { message ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                },
                onNavigateBack = {
                    navController.navigateUp()
                }
            )
        }
        
        // ========================================
        // PARTICIPANT & POLL MANAGEMENT
        // ========================================
        
        composable(
            route = Screen.ParticipantManagement.route,
            arguments = listOf(navArgument("eventId") { type = NavType.StringType })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            
            ParticipantManagementScreenWrapper(
                eventId = eventId,
                onParticipantAdded = {
                    Toast.makeText(context, "Participant ajouté", Toast.LENGTH_SHORT).show()
                },
                onPollStarted = {
                    navController.navigate(Screen.PollVoting.createRoute(eventId))
                },
                onBack = {
                    navController.navigateUp()
                }
            )
        }
        
        composable(
            route = Screen.PollVoting.route,
            arguments = listOf(navArgument("eventId") { type = NavType.StringType })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            
            PollVotingScreenWrapper(
                eventId = eventId,
                participantId = userId,
                onVoteSubmitted = {
                    navController.navigate(Screen.PollResults.createRoute(eventId))
                },
                onBack = {
                    navController.navigateUp()
                }
            )
        }
        
        composable(
            route = Screen.PollResults.route,
            arguments = listOf(navArgument("eventId") { type = NavType.StringType })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            
            PollResultsScreenWrapper(
                eventId = eventId,
                userId = userId,
                onDateConfirmed = {
                    navController.navigate(Screen.EventDetail.createRoute(eventId)) {
                        popUpTo(Screen.EventDetail.createRoute(eventId)) { inclusive = true }
                    }
                },
                onBack = {
                    navController.navigateUp()
                }
            )
        }
        
        // ========================================
        // SCENARIO MANAGEMENT
        // ========================================
        
        composable(
            route = Screen.ScenarioList.route,
            arguments = listOf(navArgument("eventId") { type = NavType.StringType })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            val viewModel: ScenarioManagementViewModel = koinInject()
            
            // TODO: Get event from repository
            // For now, navigate back
            navController.navigateUp()
        }
        
        composable(
            route = Screen.ScenarioDetail.route,
            arguments = listOf(
                navArgument("eventId") { type = NavType.StringType },
                navArgument("scenarioId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            val scenarioId = backStackEntry.arguments?.getString("scenarioId") ?: ""
            
            // TODO: Implement ScenarioDetailScreen
            // For now, show placeholder
            navController.navigateUp()
        }
        
        composable(
            route = Screen.ScenarioComparison.route,
            arguments = listOf(navArgument("eventId") { type = NavType.StringType })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            
            // TODO: Implement ScenarioComparisonScreen
            // For now, show placeholder
            navController.navigateUp()
        }
        
        // ========================================
        // COMMUNICATION
        // ========================================
        
        composable(Screen.Inbox.route) {
            InboxScreen(
                userId = userId,
                onNotificationClick = { notificationId ->
                    // TODO: Navigate to relevant screen based on notification type
                },
                onBack = {
                    // Inbox is a main tab, no back navigation
                }
            )
        }
        
        // ========================================
        // MEETINGS (Phase 4)
        // ========================================
        
        composable(Screen.MeetingList.route) {
            // TODO: Implement MeetingListScreen (Phase 4)
            // For now, show placeholder
            navController.navigateUp()
        }
        
        composable(
            route = Screen.MeetingDetail.route,
            arguments = listOf(navArgument("meetingId") { type = NavType.StringType })
        ) { backStackEntry ->
            val meetingId = backStackEntry.arguments?.getString("meetingId") ?: ""
            
            // TODO: Implement MeetingDetailScreen (Phase 4)
            // For now, show placeholder
            navController.navigateUp()
        }
        
        // ========================================
        // SETTINGS
        // ========================================
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                userId = userId,
                currentSessionId = "", // TODO: Get from auth state
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onBack = {
                    navController.navigateUp()
                }
            )
        }
    }
}
