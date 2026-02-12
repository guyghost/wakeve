package com.guyghost.wakeve.navigation

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.guyghost.wakeve.EventDetailScreen
import com.guyghost.wakeve.ExploreTabScreen
import com.guyghost.wakeve.GetStartedScreen
import com.guyghost.wakeve.HomeScreen
import com.guyghost.wakeve.InboxScreen
import com.guyghost.wakeve.OnboardingScreen
import com.guyghost.wakeve.ProfileTabScreen
import com.guyghost.wakeve.SettingsScreen
import com.guyghost.wakeve.SplashScreen
import com.guyghost.wakeve.ui.auth.AuthScreen
import com.guyghost.wakeve.ui.auth.AuthSideEffect
import com.guyghost.wakeve.ui.auth.AuthViewModel
import com.guyghost.wakeve.ui.auth.EmailAuthScreen
import com.guyghost.wakeve.ui.event.DraftEventWizard
import com.guyghost.wakeve.ui.meeting.MeetingListScreen
import com.guyghost.wakeve.ui.scenario.ScenarioComparisonScreen
import com.guyghost.wakeve.ui.scenario.ScenarioDetailScreen
import com.guyghost.wakeve.ui.scenario.ScenarioManagementScreen
import com.guyghost.wakeve.ui.budget.BudgetOverviewScreen
import com.guyghost.wakeve.ui.budget.BudgetDetailScreen
import com.guyghost.wakeve.accommodation.AccommodationRepository
import com.guyghost.wakeve.ui.accommodation.AccommodationScreen
import com.guyghost.wakeve.ui.meal.MealPlanningScreen
import com.guyghost.wakeve.ui.equipment.EquipmentChecklistScreen
import com.guyghost.wakeve.ui.activity.ActivityPlanningScreen
import com.guyghost.wakeve.ui.activity.ParticipantInfo
import com.guyghost.wakeve.ui.comment.CommentsScreen
import com.guyghost.wakeve.budget.BudgetRepository
import com.guyghost.wakeve.comment.CommentRepository
import com.guyghost.wakeve.meal.MealRepository
import com.guyghost.wakeve.equipment.EquipmentRepository
import com.guyghost.wakeve.activity.ActivityRepository
import com.guyghost.wakeve.models.CommentSection
import com.guyghost.wakeve.viewmodel.EventManagementViewModel
import com.guyghost.wakeve.EventRepository
import com.guyghost.wakeve.viewmodel.MeetingManagementViewModel
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
fun WakeveNavHost(
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
            val authViewModel: AuthViewModel = koinInject()
            val authState by authViewModel.uiState.collectAsState()
            
            ProfileTabScreen(
                userId = userId,
                isGuest = authState.isGuest,
                isAuthenticated = authState.isAuthenticated,
                userEmail = authState.currentUser?.email,
                userName = authState.currentUser?.displayName,
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToInbox = {
                    navController.navigate(Screen.Inbox.route)
                },
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onCreateAccount = {
                    navController.navigate(Screen.Auth.route)
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
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(Screen.GetStarted.route) { inclusive = true }
                    }
                }
            )
        }
        
        // ========================================
        // AUTHENTICATION (NEW)
        // ========================================
        
        composable(Screen.Auth.route) {
            val authViewModel: AuthViewModel = koinInject()
            val uiState by authViewModel.uiState.collectAsState()
            val authCallbacks = LocalAuthCallbacks.current
            val context = LocalContext.current
            
            // Handle side effects (navigation, toasts)
            LaunchedEffect(Unit) {
                authViewModel.sideEffects.collect { effect ->
                    when (effect) {
                        is AuthSideEffect.NavigateToHome -> {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Auth.route) { inclusive = true }
                            }
                        }
                        is AuthSideEffect.NavigateToOnboarding -> {
                            navController.navigate(Screen.Onboarding.route) {
                                popUpTo(Screen.Auth.route) { inclusive = true }
                            }
                        }
                        is AuthSideEffect.NavigateToEmailAuth -> {
                            navController.navigate(Screen.EmailAuth.route)
                        }
                        is AuthSideEffect.ShowError -> {
                            Toast.makeText(context, effect.message, Toast.LENGTH_LONG).show()
                        }
                        is AuthSideEffect.ShowSuccess -> {
                            Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                        }
                        is AuthSideEffect.NavigateBack -> {
                            navController.navigateUp()
                        }
                        is AuthSideEffect.ShowOTPInput,
                        is AuthSideEffect.HapticFeedback,
                        is AuthSideEffect.AnimateSuccess -> {
                            // Handled by EmailAuthScreen or ignored
                        }
                    }
                }
            }
            
            AuthScreen(
                onGoogleSignIn = {
                    // Launch OAuth flow from Activity (via AuthCallbacks)
                    // The result will be handled in MainActivity and propagated to AuthViewModel
                    authCallbacks.launchGoogleSignIn()
                },
                onAppleSignIn = {
                    // Launch Apple Sign-In web flow from Activity
                    // The result will be handled in MainActivity and propagated to AuthViewModel
                    authCallbacks.launchAppleSignIn()
                },
                onEmailSignIn = {
                    authViewModel.onEmailSignInRequested()
                },
                onSkip = {
                    authViewModel.onSkipAuth()
                },
                isLoading = uiState.isLoading,
                errorMessage = uiState.errorMessage
            )
        }
        
        composable(Screen.EmailAuth.route) {
            val authViewModel: AuthViewModel = koinInject()
            val uiState by authViewModel.uiState.collectAsState()
            val context = LocalContext.current
            
            // Handle side effects for EmailAuth
            LaunchedEffect(Unit) {
                authViewModel.sideEffects.collect { effect ->
                    when (effect) {
                        is AuthSideEffect.NavigateToHome -> {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Auth.route) { inclusive = true }
                            }
                        }
                        is AuthSideEffect.NavigateToOnboarding -> {
                            navController.navigate(Screen.Onboarding.route) {
                                popUpTo(Screen.Auth.route) { inclusive = true }
                            }
                        }
                        is AuthSideEffect.NavigateBack -> {
                            navController.navigateUp()
                        }
                        is AuthSideEffect.ShowError -> {
                            Toast.makeText(context, effect.message, Toast.LENGTH_LONG).show()
                        }
                        is AuthSideEffect.ShowSuccess -> {
                            Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            // Other effects not handled here
                        }
                    }
                }
            }
            
            EmailAuthScreen(
                email = uiState.pendingEmail ?: "",
                isLoading = uiState.isLoading,
                isOTPStage = uiState.showOTPInput,
                remainingTime = uiState.remainingOTPTime,
                attemptsRemaining = uiState.otpAttemptsRemaining,
                errorMessage = uiState.errorMessage,
                onSubmitEmail = { email ->
                    authViewModel.onSubmitEmail(email)
                },
                onSubmitOTP = { otp ->
                    authViewModel.onSubmitOTP(otp)
                },
                onResendOTP = {
                    authViewModel.onResendOTP()
                },
                onBack = {
                    authViewModel.onGoBack()
                },
                onClearError = {
                    authViewModel.clearError()
                }
            )
        }
        
        // Deprecated: Keep Login route for backward compatibility
        // but redirect to Auth
        @Suppress("DEPRECATION")
        composable(Screen.Login.route) {
            // Redirect to new Auth screen
            LaunchedEffect(Unit) {
                navController.navigate(Screen.Auth.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            }
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
            val viewModel: EventManagementViewModel = koinInject()
            // Track if the event has been created to avoid duplicates
            var eventCreated by remember { mutableStateOf(false) }
            
            DraftEventWizard(
                initialEvent = null,
                userId = userId,
                onSaveStep = { event ->
                    // Auto-save draft event on each step
                    if (!eventCreated) {
                        // First save: create the event
                        viewModel.dispatch(
                            com.guyghost.wakeve.presentation.state.EventManagementContract.Intent.CreateEvent(event)
                        )
                        eventCreated = true
                    } else {
                        // Subsequent saves: update the event
                        viewModel.dispatch(
                            com.guyghost.wakeve.presentation.state.EventManagementContract.Intent.UpdateEvent(event)
                        )
                    }
                },
                onComplete = { event ->
                    // Final save and navigate to participant management
                    // The event should already exist from onSaveStep, so just update it
                    if (!eventCreated) {
                        // Edge case: direct completion without intermediate saves
                        viewModel.dispatch(
                            com.guyghost.wakeve.presentation.state.EventManagementContract.Intent.CreateEvent(event)
                        )
                    } else {
                        viewModel.dispatch(
                            com.guyghost.wakeve.presentation.state.EventManagementContract.Intent.UpdateEvent(event)
                        )
                    }
                    navController.navigate(Screen.ParticipantManagement.createRoute(event.id)) {
                        popUpTo(Screen.EventCreation.route) { inclusive = true }
                    }
                },
                onCancel = {
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
                userId = userId,
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
            val eventViewModel: EventManagementViewModel = koinInject()
            
            ScenarioManagementScreen(
                state = viewModel.state.value,
                onDispatch = { intent -> viewModel.dispatch(intent) },
                onNavigate = { route -> navController.navigate(route) },
                eventId = eventId,
                participantId = userId,
                isOrganizer = userId == eventViewModel.state.value.selectedEvent?.organizerId
            )
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
            val viewModel: ScenarioManagementViewModel = koinInject()
            val eventViewModel: EventManagementViewModel = koinInject()
            
            // Get the scenario from the state machine
            val state = viewModel.state.value
            val scenarioWithVotes = state.scenarios.find { it.scenario.id == scenarioId }
            val scenario = scenarioWithVotes?.scenario
            
            if (scenario != null) {
                ScenarioDetailScreen(
                    scenario = scenario,
                    votingResult = scenarioWithVotes?.votingResult,
                    votes = scenarioWithVotes?.votes ?: emptyList(),
                    isOrganizer = userId == eventViewModel.state.value.selectedEvent?.organizerId,
                    onSelectAsFinal = {
                        viewModel.dispatch(
                            com.guyghost.wakeve.presentation.state.ScenarioManagementContract.Intent.SelectScenarioAsFinal(eventId, scenarioId, userId)
                        )
                    },
                    onNavigateToMeetings = {
                        navController.navigate(Screen.MeetingList.createRoute(eventId))
                    },
                    onNavigateBack = {
                        navController.navigateUp()
                    }
                )
            } else {
                // Scenario not found, navigate back
                navController.navigateUp()
            }
        }
        
        composable(
            route = Screen.ScenarioComparison.route,
            arguments = listOf(navArgument("eventId") { type = NavType.StringType })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            val viewModel: ScenarioManagementViewModel = koinInject()
            val eventViewModel: EventManagementViewModel = koinInject()
            
            val state = viewModel.state.value
            
            ScenarioComparisonScreen(
                scenarios = state.scenarios,
                eventId = eventId,
                isOrganizer = userId == eventViewModel.state.value.selectedEvent?.organizerId,
                onVote = { scenarioId ->
                    viewModel.dispatch(
                        com.guyghost.wakeve.presentation.state.ScenarioManagementContract.Intent.VoteScenario(
                            scenarioId,
                            com.guyghost.wakeve.models.ScenarioVoteType.PREFER
                        )
                    )
                },
                onSelectWinner = { scenarioId ->
                    viewModel.dispatch(
                        com.guyghost.wakeve.presentation.state.ScenarioManagementContract.Intent.SelectScenarioAsFinal(eventId, scenarioId, userId)
                    )
                    navController.navigate(Screen.MeetingList.createRoute(eventId))
                },
                onNavigateBack = {
                    viewModel.dispatch(
                        com.guyghost.wakeve.presentation.state.ScenarioManagementContract.Intent.ClearComparison
                    )
                    navController.navigateUp()
                },
                onNavigateToMeetings = { id ->
                    navController.navigate(Screen.MeetingList.createRoute(id))
                }
            )
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
        
        composable(
            route = Screen.MeetingList.route,
            arguments = listOf(navArgument("eventId") { type = NavType.StringType })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            val viewModel: MeetingManagementViewModel = koinInject()
            val eventViewModel: EventManagementViewModel = koinInject()
            
            MeetingListScreen(
                viewModel = viewModel,
                isOrganizer = userId == eventViewModel.state.value.selectedEvent?.organizerId,
                onNavigateToDetail = { route ->
                    navController.navigate(route)
                }
            )
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
            val authViewModel: AuthViewModel = koinInject()
            val authState by authViewModel.uiState.collectAsState()

            SettingsScreen(
                userId = userId,
                currentSessionId = authState.currentUser?.id ?: "",
                isGuest = authState.isGuest,
                isAuthenticated = authState.isAuthenticated,
                userEmail = authState.currentUser?.email,
                userName = authState.currentUser?.displayName,
                onLogout = {
                    authViewModel.signOut()
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onCreateAccount = {
                    navController.navigate(Screen.Auth.route)
                },
                onBack = {
                    navController.navigateUp()
                }
            )
        }
        
        // ========================================
        // BUDGET MANAGEMENT
        // ========================================
        
        composable(
            route = Screen.BudgetOverview.route,
            arguments = listOf(navArgument("eventId") { type = NavType.StringType })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            val budgetRepository: BudgetRepository = koinInject()
            val eventRepository: EventRepository = koinInject()

            BudgetOverviewScreen(
                eventId = eventId,
                budgetRepository = budgetRepository,
                eventRepository = eventRepository,
                onNavigateToDetail = { budgetItemId ->
                    navController.navigate(Screen.BudgetDetail.createRoute(eventId, budgetItemId))
                },
                onNavigateBack = {
                    navController.navigateUp()
                }
            )
        }
        
        composable(
            route = Screen.BudgetDetail.route,
            arguments = listOf(
                navArgument("eventId") { type = NavType.StringType },
                navArgument("budgetItemId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            val budgetItemId = backStackEntry.arguments?.getString("budgetItemId") ?: ""
            val budgetRepository: BudgetRepository = koinInject()
            val commentRepository: CommentRepository = koinInject()
            val eventRepository: EventRepository = koinInject()

            // Get event participants
            val event = eventRepository.getEvent(eventId)
            val eventParticipants = event?.participants ?: emptyList()

            BudgetDetailScreen(
                budgetId = budgetItemId,
                budgetRepository = budgetRepository,
                commentRepository = commentRepository,
                currentUserId = userId,
                eventParticipants = eventParticipants,
                onNavigateBack = {
                    navController.navigateUp()
                },
                onNavigateToComments = { evtId, section, itemId ->
                    navController.navigate(Screen.Comments.createRoute(evtId))
                }
            )
        }
        
        // ========================================
        // LOGISTICS & PLANNING
        // ========================================
        
        composable(
            route = Screen.Accommodation.route,
            arguments = listOf(navArgument("eventId") { type = NavType.StringType })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            val accommodationRepository: AccommodationRepository = koinInject()
            val commentRepository: CommentRepository = koinInject()

            AccommodationScreen(
                eventId = eventId,
                accommodationRepository = accommodationRepository,
                commentRepository = commentRepository,
                onNavigateBack = {
                    navController.navigateUp()
                },
                onNavigateToComments = { evtId, section, itemId ->
                    navController.navigate(Screen.Comments.createRoute(evtId))
                }
            )
        }
        
        composable(
            route = Screen.MealPlanning.route,
            arguments = listOf(navArgument("eventId") { type = NavType.StringType })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            val mealRepository: MealRepository = koinInject()
            val commentRepository: CommentRepository = koinInject()
            
            MealPlanningScreen(
                eventId = eventId,
                mealRepository = mealRepository,
                commentRepository = commentRepository,
                onNavigateBack = {
                    navController.navigateUp()
                },
                onNavigateToComments = { evtId, section, itemId ->
                    navController.navigate(Screen.Comments.createRoute(evtId))
                }
            )
        }
        
        composable(
            route = Screen.EquipmentChecklist.route,
            arguments = listOf(navArgument("eventId") { type = NavType.StringType })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            val equipmentRepository: EquipmentRepository = koinInject()
            val commentRepository: CommentRepository = koinInject()
            val eventViewModel: EventManagementViewModel = koinInject()
            
            // Get participants from event state
            val participants = eventViewModel.state.value.selectedEvent?.participants?.map { participantId ->
                ParticipantInfo(id = participantId, name = participantId)
            } ?: emptyList()
            
            EquipmentChecklistScreen(
                eventId = eventId,
                participants = participants,
                equipmentRepository = equipmentRepository,
                commentRepository = commentRepository,
                onNavigateBack = {
                    navController.navigateUp()
                },
                onNavigateToComments = { evtId, section, itemId ->
                    navController.navigate(Screen.Comments.createRoute(evtId))
                }
            )
        }
        
        composable(
            route = Screen.ActivityPlanning.route,
            arguments = listOf(navArgument("eventId") { type = NavType.StringType })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            val activityRepository: ActivityRepository = koinInject()
            val commentRepository: CommentRepository = koinInject()
            val eventViewModel: EventManagementViewModel = koinInject()
            
            // Get participants and organizer from event state
            val event = eventViewModel.state.value.selectedEvent
            val organizerId = event?.organizerId ?: userId
            val participants = event?.participants?.map { participantId ->
                ParticipantInfo(id = participantId, name = participantId)
            } ?: emptyList()
            
            ActivityPlanningScreen(
                eventId = eventId,
                organizerId = organizerId,
                participants = participants,
                activityRepository = activityRepository,
                commentRepository = commentRepository,
                onNavigateBack = {
                    navController.navigateUp()
                },
                onNavigateToComments = { evtId, section, itemId ->
                    navController.navigate(Screen.Comments.createRoute(evtId))
                }
            )
        }
        
        composable(
            route = Screen.Comments.route,
            arguments = listOf(navArgument("eventId") { type = NavType.StringType })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            val commentRepository: CommentRepository = koinInject()
            val authViewModel: AuthViewModel = koinInject()
            val authState by authViewModel.uiState.collectAsState()

            CommentsScreen(
                eventId = eventId,
                commentRepository = commentRepository,
                section = null, // Show all sections
                sectionItemId = null,
                currentUserId = userId,
                currentUserName = authState.currentUser?.displayName ?: "User",
                onBack = {
                    navController.navigateUp()
                }
            )
        }
        
        // ========================================
        // SCENARIO MANAGEMENT (Additional)
        // ========================================
        
        composable(
            route = Screen.ScenarioManagement.route,
            arguments = listOf(navArgument("eventId") { type = NavType.StringType })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            val viewModel: ScenarioManagementViewModel = koinInject()
            val eventViewModel: EventManagementViewModel = koinInject()
            
            ScenarioManagementScreen(
                state = viewModel.state.value,
                onDispatch = { intent -> viewModel.dispatch(intent) },
                onNavigate = { route -> navController.navigate(route) },
                eventId = eventId,
                participantId = userId,
                isOrganizer = userId == eventViewModel.state.value.selectedEvent?.organizerId
            )
        }
    }
}
