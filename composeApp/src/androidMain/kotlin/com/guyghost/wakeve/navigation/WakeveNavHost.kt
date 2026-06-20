package com.guyghost.wakeve.navigation

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.guyghost.wakeve.EventDetailScreen
import com.guyghost.wakeve.ExploreTabScreen
import com.guyghost.wakeve.GetStartedScreen
import com.guyghost.wakeve.InboxScreen
import com.guyghost.wakeve.OnboardingScreen
import com.guyghost.wakeve.ProfileTabScreen
import com.guyghost.wakeve.SettingsScreen
import com.guyghost.wakeve.SplashScreen
import com.guyghost.wakeve.deeplink.AndroidInvitationShareService
import com.guyghost.wakeve.deeplink.AndroidNavigationDeepLinkHandler
import com.guyghost.wakeve.deeplink.InvitationShareCreationResult
import com.guyghost.wakeve.deeplink.normalizeDeepLinkPathSegment
import com.guyghost.wakeve.ui.auth.AuthScreen
import com.guyghost.wakeve.ui.auth.AuthSideEffect
import com.guyghost.wakeve.ui.auth.AuthViewModel
import com.guyghost.wakeve.ui.auth.EmailAuthScreen
import com.guyghost.wakeve.ui.event.DraftEventCreationRouteUiState
import com.guyghost.wakeve.ui.event.DraftEventWizardEventFactory
import com.guyghost.wakeve.ui.event.DraftEventWizard
import com.guyghost.wakeve.ui.event.EventPhotosFollowUpUiState
import com.guyghost.wakeve.ui.event.EventWorkspaceRoute
import com.guyghost.wakeve.ui.event.fallbackEventPhotosFollowUpUiState
import com.guyghost.wakeve.ui.meeting.MeetingDetailScreen
import com.guyghost.wakeve.ui.meeting.MeetingListScreen
import com.guyghost.wakeve.ui.scenario.ScenarioComparisonScreen
import com.guyghost.wakeve.ui.scenario.ScenarioDetailScreen
import com.guyghost.wakeve.ui.scenario.ScenarioManagementScreen
import com.guyghost.wakeve.ui.transport.TransportPlanningScreen
import com.guyghost.wakeve.ui.budget.BudgetOverviewScreen
import com.guyghost.wakeve.ui.budget.BudgetDetailScreen
import com.guyghost.wakeve.ui.budget.PaymentPotScreen
import com.guyghost.wakeve.ui.budget.TricountHandoffScreen
import com.guyghost.wakeve.accommodation.AccommodationRepository
import com.guyghost.wakeve.ui.accommodation.AccommodationScreen
import com.guyghost.wakeve.ui.meal.MealPlanningScreen
import com.guyghost.wakeve.ui.equipment.EquipmentChecklistScreen
import com.guyghost.wakeve.ui.activity.ActivityPlanningScreen
import com.guyghost.wakeve.ui.ai.EventPlanningAssistantScreen
import com.guyghost.wakeve.ui.activity.ParticipantInfo
import com.guyghost.wakeve.ui.comment.CommentsScreen
import com.guyghost.wakeve.ui.invitation.InvitationShareScreen
import com.guyghost.wakeve.ui.notification.NotificationsScreen
import com.guyghost.wakeve.ui.notification.NotificationPreferencesScreen
import com.guyghost.wakeve.ui.notification.parseNotificationInboxFilter
import com.guyghost.wakeve.ui.screens.LeaderboardScreen
import com.guyghost.wakeve.ui.screens.OrganizerDashboardScreen
import com.guyghost.wakeve.budget.BudgetRepository
import com.guyghost.wakeve.comment.CommentRepository
import com.guyghost.wakeve.meal.MealRepository
import com.guyghost.wakeve.equipment.EquipmentRepository
import com.guyghost.wakeve.activity.ActivityRepository
import com.guyghost.wakeve.models.CommentSection
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.OptimizationType
import com.guyghost.wakeve.models.Route
import com.guyghost.wakeve.models.ScenarioStatus
import com.guyghost.wakeve.access.DateValidationState
import com.guyghost.wakeve.access.ParticipantRepositoryRecord
import com.guyghost.wakeve.auth.SessionRepository
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.TransportLocation
import com.guyghost.wakeve.models.TransportOption
import com.guyghost.wakeve.models.TransportPlan
import com.guyghost.wakeve.models.TransportReadiness
import com.guyghost.wakeve.notification.NotificationService
import com.guyghost.wakeve.notification.isDeepLinkClickTarget
import com.guyghost.wakeve.payment.PaymentPotRepository
import com.guyghost.wakeve.payment.TricountHandoffRepository
import com.guyghost.wakeve.repository.ScenarioRepository
import com.guyghost.wakeve.transport.TransportRepository
import com.guyghost.wakeve.viewmodel.EventManagementViewModel
import com.guyghost.wakeve.viewmodel.EventPlanningAssistantViewModel
import com.guyghost.wakeve.repository.EventRepositoryInterface as EventRepository
import com.guyghost.wakeve.viewmodel.MeetingManagementViewModel
import com.guyghost.wakeve.viewmodel.ProfileViewModel
import com.guyghost.wakeve.viewmodel.ScenarioManagementViewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

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
 * @param onProfileClick Callback when profile icon is clicked (opens bottom sheet)
 */
@Composable
fun WakeveNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Home.route,
    userId: String,
    onProfileClick: () -> Unit = {},
    onInboxUnreadCountChanged: ((Int) -> Unit)? = null
) {
    val context = LocalContext.current
    fun navigateBackOrHome() {
        if (!navController.navigateUp()) {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Home.route) { inclusive = false }
                launchSingleTop = true
            }
        }
    }
    
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
            EventWorkspaceRoute(
                viewModel = viewModel,
                currentUserId = userId,
                onNavigateTo = { route ->
                    navController.navigate(route)
                },
                onShowToast = { message ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                },
                onOpenProfile = onProfileClick
            )
        }
        
        // Events tab removed - functionality moved to Home tab
        // Users can access event list from Home screen
        
        composable(Screen.Explore.route) {
            ExploreTabScreen(
                onEventClick = { eventId ->
                    navController.navigate(Screen.EventDetail.createRoute(eventId))
                },
                onTemplateClick = { template ->
                    navController.navigate(
                        Screen.EventCreation.createRoute(
                            templateTitle = template.title,
                            templateDescription = template.description,
                            templateType = template.eventType
                        )
                    )
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
                appVersionLabel = "Version ${com.guyghost.wakeve.BuildConfig.VERSION_NAME} (${com.guyghost.wakeve.BuildConfig.VERSION_CODE})",
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToInbox = {
                    navController.navigate(Screen.Inbox.route)
                },
                onNavigateToDashboard = {
                    navController.navigate(Screen.OrganizerDashboard.route)
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

        composable(Screen.EventPlanningAssistant.route) {
            val viewModel: EventPlanningAssistantViewModel = koinInject()
            EventPlanningAssistantScreen(
                viewModel = viewModel,
                onClose = { navigateBackOrHome() }
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
        
        composable(
            route = Screen.EventCreation.route,
            arguments = listOf(
                navArgument("templateTitle") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("templateDescription") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("templateType") {
                    type = NavType.StringType
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val viewModel: EventManagementViewModel = koinInject()
            var routeState by remember { mutableStateOf(DraftEventCreationRouteUiState()) }
            val templateTitle = backStackEntry.arguments?.getString("templateTitle").orEmpty()
            val templateDescription = backStackEntry.arguments?.getString("templateDescription").orEmpty()
            val templateType = backStackEntry.arguments
                ?.getString("templateType")
                ?.let { type -> runCatching { EventType.valueOf(type) }.getOrNull() }
                ?: EventType.OTHER
            val generatedEventId = remember { "event-${Clock.System.now().toEpochMilliseconds()}" }
            val templateInitialEvent = remember(templateTitle, templateDescription, templateType, generatedEventId, userId) {
                if (templateTitle.isBlank() && templateDescription.isBlank()) {
                    null
                } else {
                    val now = Clock.System.now().toString()
                    Event(
                        id = generatedEventId,
                        title = templateTitle,
                        description = templateDescription,
                        organizerId = userId,
                        proposedSlots = emptyList(),
                        deadline = now,
                        status = EventStatus.DRAFT,
                        createdAt = now,
                        updatedAt = now,
                        eventType = templateType
                    )
                }
            }
            val eventFactory = remember(templateInitialEvent, generatedEventId) {
                DraftEventWizardEventFactory(
                    initialEvent = templateInitialEvent,
                    generatedId = generatedEventId,
                    nowIso = { Clock.System.now().toString() }
                )
            }
            
            DraftEventWizard(
                initialEvent = templateInitialEvent,
                userId = userId,
                onSaveStep = { wizardState ->
                    val event = eventFactory.buildEvent(
                        userId = userId,
                        state = wizardState,
                        status = EventStatus.DRAFT
                    )
                    // Auto-save draft event on each step
                    if (!routeState.hasPersistedDraft) {
                        // First save: create the event
                        viewModel.dispatch(
                            com.guyghost.wakeve.presentation.state.EventManagementContract.Intent.CreateEvent(event)
                        )
                        routeState = routeState.copy(
                            hasPersistedDraft = true,
                            lastSavedEventId = event.id
                        )
                    } else {
                        // Subsequent saves: update the event
                        viewModel.dispatch(
                            com.guyghost.wakeve.presentation.state.EventManagementContract.Intent.UpdateEvent(event)
                        )
                        routeState = routeState.copy(lastSavedEventId = event.id)
                    }
                },
                onComplete = { wizardState ->
                    val event = eventFactory.buildEvent(
                        userId = userId,
                        state = wizardState,
                        status = EventStatus.DRAFT
                    )
                    // Final save and navigate to participant management
                    // The event should already exist from onSaveStep, so just update it
                    if (!routeState.hasPersistedDraft) {
                        // Edge case: direct completion without intermediate saves
                        viewModel.dispatch(
                            com.guyghost.wakeve.presentation.state.EventManagementContract.Intent.CreateEvent(event)
                        )
                    } else {
                        viewModel.dispatch(
                            com.guyghost.wakeve.presentation.state.EventManagementContract.Intent.UpdateEvent(event)
                        )
                    }
                    routeState = routeState.copy(
                        hasPersistedDraft = true,
                        lastSavedEventId = event.id
                    )
                    navController.navigate(Screen.ParticipantManagement.createRoute(event.id)) {
                        popUpTo(Screen.EventCreation.route) { inclusive = true }
                    }
                },
                onCancel = {
                    navigateBackOrHome()
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
                    navigateBackOrHome()
                },
                onShareInvite = { evtId, _ ->
                    navController.navigate(Screen.InvitationShare.createRoute(evtId))
                },
                onCreateFromTemplate = { template ->
                    navController.navigate(
                        Screen.EventCreation.createRoute(
                            templateTitle = template.title,
                            templateDescription = template.description,
                            templateType = template.eventType
                        )
                    )
                }
            )
        }
        
        // ========================================
        // INVITATION SHARE
        // ========================================

        composable(
            route = Screen.InvitationShare.route,
            arguments = listOf(navArgument("eventId") { type = NavType.StringType })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            val eventViewModel: EventManagementViewModel = koinInject()
            val eventTitle = eventViewModel.state.value.selectedEvent?.title ?: "Événement"
            val invitationShareService = remember(context) {
                AndroidInvitationShareService(context.applicationContext)
            }
            var invitationCode by remember(eventId) { mutableStateOf<String?>(null) }
            var isCreatingInvitation by remember(eventId) { mutableStateOf(false) }
            var invitationError by remember(eventId) { mutableStateOf<String?>(null) }
            var retryInvitationCreation by remember(eventId) { mutableStateOf(0) }

            LaunchedEffect(eventId, retryInvitationCreation) {
                if (eventId.isBlank()) {
                    invitationError = "Événement introuvable."
                    return@LaunchedEffect
                }

                isCreatingInvitation = true
                invitationError = null
                when (val result = invitationShareService.createInvitation(eventId)) {
                    is InvitationShareCreationResult.Created -> {
                        invitationCode = result.invitation.code
                        invitationError = null
                    }
                    is InvitationShareCreationResult.AuthenticationRequired -> {
                        invitationCode = null
                        invitationError = result.message
                    }
                    is InvitationShareCreationResult.Failed -> {
                        invitationCode = null
                        invitationError = result.message
                    }
                }
                isCreatingInvitation = false
            }

            InvitationShareScreen(
                eventId = eventId,
                eventTitle = eventTitle,
                invitationCode = invitationCode,
                isLoading = isCreatingInvitation,
                errorMessage = invitationError,
                onRetry = { retryInvitationCreation += 1 },
                onDismiss = {
                    navigateBackOrHome()
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
                    navigateBackOrHome()
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
                    navigateBackOrHome()
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
                    navigateBackOrHome()
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
            val eventState = eventViewModel.state.value
            val isOrganizer = userId == eventState.selectedEvent?.organizerId
            val isParticipantConfirmed = eventState.participantAccessStates
                .firstOrNull { it.userId == userId }
                ?.dateValidation == DateValidationState.VALIDATED_RETAINED_DATE
            
            ScenarioManagementScreen(
                state = viewModel.state.value,
                onDispatch = { intent -> viewModel.dispatch(intent) },
                onNavigate = { route -> navController.navigate(route) },
                eventId = eventId,
                participantId = userId,
                isOrganizer = isOrganizer,
                isParticipantConfirmed = isParticipantConfirmed
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
                val eventState = eventViewModel.state.value
                val isOrganizer = userId == eventState.selectedEvent?.organizerId
                val isParticipantConfirmed = eventState.participantAccessStates
                    .firstOrNull { it.userId == userId }
                    ?.dateValidation == DateValidationState.VALIDATED_RETAINED_DATE
                val eventStatus = state.eventStatus ?: eventState.selectedEvent?.status
                val navigateToMeetingsAfterWorkflowUnlock: (String) -> Unit = { unlockedEventId ->
                    navController.navigate(Screen.MeetingList.createRoute(unlockedEventId))
                }
                val openMeetingsAfterWorkflowUnlock = {
                    navigateToMeetingsAfterWorkflowUnlock(eventId)
                }
                ScenarioDetailScreen(
                    scenario = scenario,
                    votingResult = scenarioWithVotes?.votingResult,
                    votes = scenarioWithVotes?.votes ?: emptyList(),
                    isOrganizer = isOrganizer,
                    isParticipantConfirmed = isParticipantConfirmed,
                    eventStatus = eventStatus,
                    onSelectAsFinal = {
                        viewModel.dispatch(
                            com.guyghost.wakeve.presentation.state.ScenarioManagementContract.Intent.SelectScenarioAsFinal(eventId, scenarioId, userId)
                        )
                    },
                    onNavigateToMeetings = openMeetingsAfterWorkflowUnlock,
                    onNavigateToTransport = {
                        navController.navigate(Screen.TransportPlanning.createRoute(eventId))
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
            val eventState = eventViewModel.state.value
            val isOrganizer = userId == eventState.selectedEvent?.organizerId
            val isParticipantConfirmed = eventState.participantAccessStates
                .firstOrNull { it.userId == userId }
                ?.dateValidation == DateValidationState.VALIDATED_RETAINED_DATE
            val navigateToMeetingsAfterWorkflowUnlock: (String) -> Unit = { unlockedEventId ->
                navController.navigate(Screen.MeetingList.createRoute(unlockedEventId))
            }
            
            ScenarioComparisonScreen(
                scenarios = state.scenarios,
                eventId = eventId,
                isOrganizer = isOrganizer,
                eventStatus = state.eventStatus ?: eventState.selectedEvent?.status,
                isParticipantConfirmed = isParticipantConfirmed,
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
                },
                onNavigateBack = {
                    viewModel.dispatch(
                        com.guyghost.wakeve.presentation.state.ScenarioManagementContract.Intent.ClearComparison
                    )
                    navController.navigateUp()
                },
                onNavigateToMeetings = navigateToMeetingsAfterWorkflowUnlock,
                onNavigateToTransport = {
                    navController.navigate(Screen.TransportPlanning.createRoute(eventId))
                }
            )
        }

        composable(
            route = Screen.TransportPlanning.route,
            arguments = listOf(navArgument("eventId") { type = NavType.StringType })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            val eventViewModel: EventManagementViewModel = koinInject()
            val scenarioViewModel: ScenarioManagementViewModel = koinInject()
            val database: WakeveDb = koinInject()
            val scenarioRepository: ScenarioRepository = koinInject()
            val eventRepository: EventRepository = koinInject()
            val coroutineScope = rememberCoroutineScope()
            val json = remember {
                Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                }
            }
            val transportRepository = remember(database) { TransportRepository(database) }
            val eventState = eventViewModel.state.value
            val scenarioState = scenarioViewModel.state.value
            var persistedEvent by remember(eventId) {
                mutableStateOf<Event?>(null)
            }
            var persistedParticipantRecords by remember(eventId) {
                mutableStateOf<List<ParticipantRepositoryRecord>>(emptyList())
            }
            val eventFromRepository = persistedEvent
            val participantRecords = persistedParticipantRecords
            val routeScopedSelectedEvent = eventState.selectedEvent?.takeIf { it.id == eventId }
            val selectedEvent = routeScopedSelectedEvent ?: eventFromRepository
            val eventStatus = selectedEvent?.status
            val isReadOnly = eventStatus == null || eventStatus == EventStatus.FINALIZED
            val isOrganizer = userId == selectedEvent?.organizerId
            val isParticipantConfirmed = if (routeScopedSelectedEvent != null) {
                eventState.participantAccessStates
                    .firstOrNull { it.userId == userId }
                    ?.dateValidation == DateValidationState.VALIDATED_RETAINED_DATE
            } else {
                participantRecords.firstOrNull { it.userId == userId }?.hasValidatedDate == 1L
            }
            var persistedSelectedScenario by remember(eventId) {
                mutableStateOf<com.guyghost.wakeve.models.Scenario?>(null)
            }
            val selectedScenarioFromRepository = persistedSelectedScenario
            val selectedScenario = scenarioState.scenarios
                .firstOrNull {
                    it.scenario.eventId == eventId &&
                        it.scenario.status == ScenarioStatus.SELECTED
                }
                ?.scenario
                ?: scenarioState.selectedScenario?.takeIf {
                    it.eventId == eventId && it.status == ScenarioStatus.SELECTED
                }
                ?: selectedScenarioFromRepository?.takeIf { it.status == ScenarioStatus.SELECTED }
            val selectedDestination: TransportLocation? = selectedScenario?.let { scenario ->
                TransportLocation(name = scenario.location, address = scenario.location)
            }
            var readiness by remember(eventId, selectedDestination) {
                mutableStateOf<TransportReadiness?>(null)
            }
            var transportPlans by remember(eventId, selectedDestination) {
                mutableStateOf<List<TransportPlan>>(emptyList())
            }
            var selectedTransportPlanId by remember(eventId) {
                mutableStateOf<String?>(null)
            }
            var pendingTransportSync by remember(eventId) {
                mutableStateOf(false)
            }

            suspend fun loadTransportState() {
                readiness = selectedDestination?.let { destination ->
                    transportRepository.getReadiness(eventId, destination)
                }
                transportPlans = database.transportQueries.selectPlansByEvent(eventId)
                    .executeAsList()
                    .map { row ->
                        val routes = database.transportQueries.selectRoutesByPlan(row.id)
                            .executeAsList()
                            .associate { routeRow ->
                                routeRow.participant_id to Route(
                                    id = routeRow.id,
                                    segments = json.decodeFromString(
                                        ListSerializer(TransportOption.serializer()),
                                        routeRow.segments
                                    ),
                                    totalDurationMinutes = routeRow.total_duration_minutes.toInt(),
                                    totalCost = routeRow.total_cost,
                                    currency = routeRow.currency,
                                    score = routeRow.score
                                )
                            }

                        TransportPlan(
                            id = row.id,
                            eventId = row.event_id,
                            participantRoutes = routes,
                            groupArrivals = json.decodeFromString(
                                ListSerializer(String.serializer()),
                                row.group_arrivals_json
                            ),
                            totalGroupCost = row.total_group_cost,
                            optimizationType = OptimizationType.valueOf(row.optimization_type),
                            createdAt = row.created_at
                        )
                    }
                selectedTransportPlanId = database.transportQueries
                    .selectSelectedPlan(eventId)
                    .executeAsOneOrNull()
                    ?.plan_id
                pendingTransportSync = transportRepository.hasPendingTransportSync(eventId)
            }

            fun loadSelectedScenario() {
                persistedSelectedScenario = scenarioRepository.getSelectedScenario(eventId)
                    ?: scenarioRepository
                        .getScenariosByEventIdAndStatus(eventId, ScenarioStatus.SELECTED)
                        .firstOrNull()
            }

            fun loadTransportEvent() {
                persistedEvent = eventRepository.getEvent(eventId)
                persistedParticipantRecords = eventRepository.getParticipantRecords(eventId).orEmpty()
            }

            LaunchedEffect(eventId) {
                loadTransportEvent()
                loadSelectedScenario()
            }

            LaunchedEffect(eventId, selectedDestination) {
                loadTransportState()
            }

            if (eventStatus == null) {
                return@composable
            }

            TransportPlanningScreen(
                eventId = eventId,
                isOrganizer = isOrganizer,
                isParticipantConfirmed = isParticipantConfirmed,
                confirmedDate = selectedEvent?.finalDate,
                selectedDestination = selectedDestination,
                eventStatus = eventStatus,
                isReadOnly = isReadOnly,
                readiness = readiness,
                plans = transportPlans,
                selectedPlanId = selectedTransportPlanId,
                pendingSync = pendingTransportSync,
                isTransportProviderConfigured = false,
                onSaveDepartureLocation = { departureName ->
                    coroutineScope.launch {
                        if (selectedDestination == null) return@launch
                        transportRepository.saveDepartureLocation(
                            eventId = eventId,
                            participantId = userId,
                            location = TransportLocation(
                                name = departureName,
                                address = departureName
                            ),
                            updatedByUserId = userId
                        )
                        loadTransportState()
                    }
                },
                onGeneratePlan = { optimizationType ->
                    coroutineScope.launch {
                        val destination = selectedDestination ?: return@launch
                        transportRepository.generatePlan(
                            eventId = eventId,
                            destination = destination,
                            optimizationType = optimizationType,
                            generatedByUserId = userId
                        )
                        loadTransportState()
                    }
                },
                onSelectFinalPlan = { plan ->
                    coroutineScope.launch {
                        if (selectedDestination == null) return@launch
                        transportRepository.selectFinalPlan(
                            eventId = eventId,
                            planId = plan.id,
                            selectedByOrganizerId = userId
                        )
                        loadTransportState()
                    }
                },
                onMarkTransportNotNeeded = {
                    coroutineScope.launch {
                        if (selectedDestination == null) return@launch
                        transportRepository.markTransportNotNeeded(
                            eventId = eventId,
                            updatedByUserId = userId
                        )
                        loadTransportState()
                    }
                },
                onNavigateBack = {
                    navController.navigateUp()
                }
            )
        }
        
        // ========================================
        // COMMUNICATION
        // ========================================
        
        composable(Screen.Inbox.route) {
            val notificationService: NotificationService = koinInject()
            InboxScreen(
                userId = userId,
                notificationService = notificationService,
                onNotificationClick = { clickTarget ->
                    navController.navigateNotificationClickTarget(clickTarget)
                },
                onBack = {
                    // Inbox is a main tab, no back navigation
                },
                onNavigateToNotifications = {
                    navController.navigate(Screen.NotificationPreferences.route)
                },
                onUnreadCountChanged = onInboxUnreadCountChanged
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
            val eventRepository: EventRepository = koinInject()
            val database: WakeveDb = koinInject()
            val phase5Access = rememberPhase5Access(
                eventId = eventId,
                userId = userId,
                eventViewModel = eventViewModel,
                eventRepository = eventRepository
            )
            val canAccessOrganizationDetails = phase5Access.canAccessOrganizationDetails
            val pendingSync = remember(eventId) { database.hasPendingPhase5Sync(eventId) }

            if (!canAccessOrganizationDetails || !phase5Access.isOrganizationStatusAllowed) {
                AccessDenied(
                    message = "Confirmez votre présence pour accéder aux détails des réunions.",
                    onBack = { navController.navigateUp() }
                )
                return@composable
            }

            MeetingListScreen(
                viewModel = viewModel,
                eventId = eventId,
                isOrganizer = phase5Access.canManageMeetings,
                isReadOnly = phase5Access.isReadOnly,
                canCreateMeetings = phase5Access.canManageMeetings,
                pendingSync = pendingSync,
                isOnline = !pendingSync,
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
            val viewModel: MeetingManagementViewModel = koinInject()
            val meetingState by viewModel.state.collectAsState()

            MeetingDetailScreen(
                meetingId = meetingId,
                viewModel = viewModel,
                isOrganizer = meetingState.selectedMeeting?.organizerId == userId,
                currentUserId = userId,
                onBack = { navController.navigateUp() },
                onDeleted = { navController.navigateUp() }
            )
        }

        composable(
            route = Screen.PaymentPot.route,
            arguments = listOf(navArgument("eventId") { type = NavType.StringType })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            val eventViewModel: EventManagementViewModel = koinInject()
            val eventRepository: EventRepository = koinInject()
            val database: WakeveDb = koinInject()
            val paymentPotRepository = remember(database) { PaymentPotRepository(database) }
            val phase5Access = rememberPhase5Access(
                eventId = eventId,
                userId = userId,
                eventViewModel = eventViewModel,
                eventRepository = eventRepository
            )
            val canAccessOrganizationDetails = phase5Access.canAccessOrganizationDetails
            val pendingSync = remember(eventId) { database.hasPendingPhase5Sync(eventId) }

            if (!canAccessOrganizationDetails || !phase5Access.isOrganizationStatusAllowed) {
                AccessDenied(
                    message = "Confirmez votre présence pour accéder aux détails de la cagnotte.",
                    onBack = { navController.navigateUp() }
                )
                return@composable
            }

            PaymentPotScreen(
                eventId = eventId,
                organizerId = userId,
                isOrganizer = phase5Access.canManagePayment,
                isReadOnly = phase5Access.isReadOnly,
                canManagePayment = phase5Access.canManagePayment,
                paymentPotRepository = paymentPotRepository,
                pendingSync = pendingSync,
                isOnline = !pendingSync,
                onOpenPaymentPot = { },
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable(
            route = Screen.Tricount.route,
            arguments = listOf(navArgument("eventId") { type = NavType.StringType })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            val eventViewModel: EventManagementViewModel = koinInject()
            val eventRepository: EventRepository = koinInject()
            val database: WakeveDb = koinInject()
            val tricountHandoffRepository = remember(database) { TricountHandoffRepository(database) }
            val uriHandler = LocalUriHandler.current
            val phase5Access = rememberPhase5Access(
                eventId = eventId,
                userId = userId,
                eventViewModel = eventViewModel,
                eventRepository = eventRepository
            )
            val canAccessOrganizationDetails = phase5Access.canAccessOrganizationDetails
            val pendingSync = remember(eventId) { database.hasPendingPhase5Sync(eventId) }

            if (!canAccessOrganizationDetails || !phase5Access.isOrganizationStatusAllowed) {
                AccessDenied(
                    message = "Confirmez votre présence pour ouvrir les détails Tricount.",
                    onBack = { navController.navigateUp() }
                )
                return@composable
            }

            TricountHandoffScreen(
                eventId = eventId,
                currentUserId = userId,
                isOrganizer = phase5Access.canManageTricount,
                isReadOnly = phase5Access.isReadOnly,
                canManageTricount = phase5Access.canManageTricount,
                tricountHandoffRepository = tricountHandoffRepository,
                pendingSync = pendingSync,
                isOnline = !pendingSync,
                onOpenSafeTricount = { link -> openSafeUrl(link.validatedURL, uriHandler::openUri) },
                onNavigateBack = { navController.navigateUp() }
            )
        }
        
        // ========================================
        // SETTINGS
        // ========================================
        
        composable(Screen.Settings.route) {
            val authViewModel: AuthViewModel = koinInject()
            val authState by authViewModel.uiState.collectAsState()
            val sessionRepository: SessionRepository = koinInject()

            SettingsScreen(
                userId = userId,
                currentSessionId = "",
                isGuest = authState.isGuest,
                isAuthenticated = authState.isAuthenticated,
                userEmail = authState.currentUser?.email,
                userName = authState.currentUser?.displayName,
                sessionRepository = sessionRepository,
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
            val eventViewModel: EventManagementViewModel = koinInject()
            val database: WakeveDb = koinInject()
            val phase5Access = rememberPhase5Access(
                eventId = eventId,
                userId = userId,
                eventViewModel = eventViewModel,
                eventRepository = eventRepository
            )
            val canAccessOrganizationDetails = phase5Access.canAccessOrganizationDetails
            val pendingSync = remember(eventId) { database.hasPendingPhase5Sync(eventId) }

            if (!canAccessOrganizationDetails || !phase5Access.isOrganizationStatusAllowed) {
                AccessDenied(
                    message = "Confirmez votre présence pour accéder aux détails du budget.",
                    onBack = { navController.navigateUp() }
                )
                return@composable
            }

            BudgetOverviewScreen(
                eventId = eventId,
                budgetRepository = budgetRepository,
                eventRepository = eventRepository,
                onNavigateToDetail = { budgetItemId ->
                    navController.navigate(Screen.BudgetDetail.createRoute(eventId, budgetItemId))
                },
                onOpenPaymentPot = {
                    navController.navigate(Screen.PaymentPot.createRoute(eventId))
                },
                onOpenTricount = {
                    navController.navigate(Screen.Tricount.createRoute(eventId))
                },
                onNavigateBack = {
                    navController.navigateUp()
                },
                pendingSync = pendingSync,
                isOnline = !pendingSync,
                isReadOnly = phase5Access.isReadOnly,
                canCreateBudget = phase5Access.canManageBudget,
                canManagePayment = phase5Access.canManagePayment,
                canManageTricount = phase5Access.canManageTricount
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
            val eventViewModel: EventManagementViewModel = koinInject()
            val database: WakeveDb = koinInject()
            val phase5Access = rememberPhase5Access(
                eventId = eventId,
                userId = userId,
                eventViewModel = eventViewModel,
                eventRepository = eventRepository
            )
            val pendingSync = remember(eventId) { database.hasPendingPhase5Sync(eventId) }

            if (!phase5Access.canEnterOrganizationRoutes) {
                AccessDenied(
                    message = "Confirmez votre présence pour accéder aux détails des dépenses.",
                    onBack = { navController.navigateUp() }
                )
                return@composable
            }

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
                },
                pendingSync = pendingSync,
                isOnline = !pendingSync,
                isReadOnly = phase5Access.isReadOnly,
                canManageBudget = phase5Access.canManageBudget,
                canMutateBudget = phase5Access.canManageBudget
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

        composable(
            route = Screen.EventPhotos.route,
            arguments = listOf(navArgument("eventId") { type = NavType.StringType })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""

            EventPhotosFollowUpScreen(
                eventId = eventId,
                onBack = { navController.navigateUp() }
            )
        }
        
        // ========================================
        // NOTIFICATIONS
        // ========================================

        composable(
            route = Screen.Notifications.route,
            arguments = listOf(
                navArgument(NOTIFICATIONS_FILTER_ARG) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val notificationService: NotificationService = koinInject()
            NotificationsScreen(
                onBack = {
                    navController.navigateUp()
                },
                onNavigateToPreferences = {
                    navController.navigate(Screen.NotificationPreferences.route)
                },
                onNotificationClick = { clickTarget ->
                    navController.navigateNotificationClickTarget(clickTarget)
                },
                userId = userId,
                notificationService = notificationService,
                initialFilter = parseNotificationInboxFilter(
                    backStackEntry.arguments?.getString(NOTIFICATIONS_FILTER_ARG)
                )
            )
        }

        composable(Screen.NotificationPreferences.route) {
            val notificationService: NotificationService = koinInject()
            NotificationPreferencesScreen(
                onBack = {
                    navController.navigateUp()
                },
                userId = userId,
                notificationService = notificationService
            )
        }

        // ========================================
        // GAMIFICATION
        // ========================================

        composable(Screen.Leaderboard.route) {
            val profileViewModel: ProfileViewModel = koinInject(
                parameters = { parametersOf(userId) }
            )
            LeaderboardScreen(
                viewModel = profileViewModel,
                onNavigateBack = {
                    navController.navigateUp()
                }
            )
        }

        // ========================================
        // ORGANIZER DASHBOARD
        // ========================================

        composable(Screen.OrganizerDashboard.route) {
            val eventViewModel: EventManagementViewModel = koinInject()
            val eventState by eventViewModel.state.collectAsState()

            LaunchedEffect(Unit) {
                eventViewModel.loadEvents()
            }

            OrganizerDashboardScreen(
                events = eventState.events,
                currentUserId = userId,
                isLoading = eventState.isLoading,
                error = eventState.error,
                onNavigateBack = {
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
            val eventState = eventViewModel.state.value
            val isOrganizer = userId == eventState.selectedEvent?.organizerId
            val isParticipantConfirmed = eventState.participantAccessStates
                .firstOrNull { it.userId == userId }
                ?.dateValidation == DateValidationState.VALIDATED_RETAINED_DATE
            
            ScenarioManagementScreen(
                state = viewModel.state.value,
                onDispatch = { intent -> viewModel.dispatch(intent) },
                onNavigate = { route -> navController.navigate(route) },
                eventId = eventId,
                participantId = userId,
                isOrganizer = isOrganizer,
                isParticipantConfirmed = isParticipantConfirmed
            )
        }
    }
}

@Composable
private fun EventPhotosFollowUpScreen(
    eventId: String,
    state: EventPhotosFollowUpUiState = fallbackEventPhotosFollowUpUiState(eventId),
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = state.title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = state.subtitle,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = state.statusLabel,
            style = MaterialTheme.typography.titleMedium,
            color = if (state.canShareNow) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
        Text(
            text = state.body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Prochaine action : ${state.recommendedActionLabel}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.checklist.forEach { item ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${item.statusLabel} - ${item.title}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = item.body,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Button(onClick = onBack) {
            Text(state.backActionLabel)
        }
    }
}

private data class Phase5AccessState(
    val isOrganizer: Boolean,
    val isParticipantConfirmed: Boolean,
    val eventStatus: EventStatus?,
    val isOrganizationStatusAllowed: Boolean,
    val canAccessOrganizationDetails: Boolean,
    val canEnterOrganizationRoutes: Boolean,
    val isReadOnly: Boolean,
    val canManageMeetings: Boolean,
    val canManageBudget: Boolean,
    val canManagePayment: Boolean,
    val canManageTricount: Boolean
)

@Composable
private fun rememberPhase5Access(
    eventId: String,
    userId: String,
    eventViewModel: EventManagementViewModel,
    eventRepository: EventRepository
): Phase5AccessState {
    val eventState = eventViewModel.state.value
    val routeScopedSelectedEvent = eventState.selectedEvent?.takeIf { it.id == eventId }
    val event = routeScopedSelectedEvent ?: eventRepository.getEvent(eventId)
    val participantRecords = eventRepository.getParticipantRecords(eventId).orEmpty()
    val isOrganizer = userId == event?.organizerId
    val isParticipantConfirmed = if (routeScopedSelectedEvent != null) {
        eventState.participantAccessStates
            .firstOrNull { it.userId == userId }
            ?.dateValidation == DateValidationState.VALIDATED_RETAINED_DATE
    } else {
        participantRecords.firstOrNull { it.userId == userId }?.hasValidatedDate == 1L
    }
    val eventStatus = event?.status
    val allowedOrganizationStatuses = setOf(EventStatus.ORGANIZING, EventStatus.FINALIZED)
    val preOrganizingStatuses = setOf(
        EventStatus.DRAFT,
        EventStatus.POLLING,
        EventStatus.CONFIRMED,
        EventStatus.COMPARING
    )
    val isOrganizationStatusAllowed = eventStatus in allowedOrganizationStatuses
    val isReadOnly = eventStatus == EventStatus.FINALIZED
    val canAccessOrganizationDetails = isOrganizer || isParticipantConfirmed
    val canEnterOrganizationRoutes = canAccessOrganizationDetails && isOrganizationStatusAllowed
    val canMutateOrganization = canEnterOrganizationRoutes && isOrganizer && !isReadOnly

    return Phase5AccessState(
        isOrganizer = isOrganizer,
        isParticipantConfirmed = isParticipantConfirmed,
        eventStatus = eventStatus,
        isOrganizationStatusAllowed = isOrganizationStatusAllowed && eventStatus !in preOrganizingStatuses,
        canAccessOrganizationDetails = canAccessOrganizationDetails,
        canEnterOrganizationRoutes = canEnterOrganizationRoutes,
        isReadOnly = isReadOnly,
        canManageMeetings = canMutateOrganization,
        canManageBudget = canMutateOrganization,
        canManagePayment = canMutateOrganization,
        canManageTricount = canMutateOrganization
    )
}

private fun WakeveDb.hasPendingPhase5Sync(eventId: String): Boolean =
    syncMetadataQueries.selectPending()
        .executeAsList()
        .any { pending ->
            pending.entityType in setOf(
                "meeting",
                "budget",
                "budget_item",
                "expense",
                "settlement",
                "payment",
                "payment_pot",
                "tricount",
                "tricount_handoff"
            ) && (
                pending.entityId == eventId ||
                    pending.entityId.startsWith("$eventId:") ||
                    pending.entityId.contains(eventId)
                )
        }

private fun openSafeUrl(
    validatedURL: String,
    safeUrlOpener: (String) -> Unit
) {
    if (validatedURL.startsWith("https://") && !validatedURL.contains("\${")) {
        safeUrlOpener(validatedURL)
    }
}

private fun NavHostController.navigateNotificationClickTarget(clickTarget: String) {
    val normalizedTarget = clickTarget.trim()
    if (normalizedTarget.isBlank()) return

    if (isDeepLinkClickTarget(normalizedTarget)) {
        AndroidNavigationDeepLinkHandler().handleDeepLink(
            uri = Uri.parse(normalizedTarget),
            navController = this,
            isAuthenticated = true
        )
        return
    }

    normalizeDeepLinkPathSegment(normalizedTarget)
        ?.let { navigate(Screen.EventDetail.createRoute(it)) }
}

@Composable
private fun AccessDenied(
    message: String,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Text(
                text = "Accès restreint",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = onBack) {
                Text("Retour")
            }
        }
    }
}
