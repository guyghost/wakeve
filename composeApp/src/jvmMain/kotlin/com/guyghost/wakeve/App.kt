package com.guyghost.wakeve

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.guyghost.wakeve.auth.AuthState
import com.guyghost.wakeve.auth.AuthStateManager
import com.guyghost.wakeve.auth.BrowserOAuthHelper
import com.guyghost.wakeve.auth.JvmAuthenticationService
import com.guyghost.wakeve.database.WakevDb
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.OAuthProvider
import com.guyghost.wakeve.models.Vote
import com.guyghost.wakeve.security.JvmSecureTokenStorage
import com.guyghost.wakeve.sync.SyncManager
import com.guyghost.wakeve.sync.KtorSyncHttpClient
import com.guyghost.wakeve.sync.JvmNetworkStatusDetector
import kotlinx.coroutines.launch

/**
 * Event repository that uses database persistence and sync capabilities
 */
class SyncedEventRepository(
    private val database: WakevDb,
    private val syncManager: SyncManager
) : EventRepositoryInterface {
    private val eventRepository = DatabaseEventRepository(database, syncManager)
    private val userRepository = UserRepository(database)

    override suspend fun createEvent(event: Event): Result<Event> {
        return eventRepository.createEvent(event).also { result ->
            if (result.isSuccess) {
                syncManager.triggerSync()
            }
        }
    }

    override fun getEvent(id: String): Event? = eventRepository.getEvent(id)

    override fun getPoll(eventId: String) = eventRepository.getPoll(eventId)

    override suspend fun addParticipant(eventId: String, participantId: String): Result<Boolean> {
        return eventRepository.addParticipant(eventId, participantId).also { result ->
            if (result.isSuccess) {
                syncManager.triggerSync()
            }
        }
    }

    override fun getParticipants(eventId: String): List<String>? = eventRepository.getParticipants(eventId)

    override suspend fun addVote(eventId: String, participantId: String, slotId: String, vote: Vote): Result<Boolean> {
        return eventRepository.addVote(eventId, participantId, slotId, vote).also { result ->
            if (result.isSuccess) {
                syncManager.triggerSync()
            }
        }
    }

    override suspend fun updateEvent(event: Event): Result<Event> {
        return eventRepository.updateEvent(event).also { result ->
            if (result.isSuccess) {
                syncManager.triggerSync()
            }
        }
    }

    override suspend fun updateEventStatus(id: String, status: EventStatus, finalDate: String?): Result<Boolean> {
        return eventRepository.updateEventStatus(id, status, finalDate).also { result ->
            if (result.isSuccess) {
                syncManager.triggerSync()
            }
        }
    }

    override fun isDeadlinePassed(deadline: String): Boolean = eventRepository.isDeadlinePassed(deadline)

    override fun isOrganizer(eventId: String, userId: String): Boolean = eventRepository.isOrganizer(eventId, userId)

    override fun canModifyEvent(eventId: String, userId: String): Boolean = eventRepository.canModifyEvent(eventId, userId)

    override fun getAllEvents(): List<Event> = eventRepository.getAllEvents()

    // Sync-specific methods
    val syncStatus = syncManager.syncStatus
    val isNetworkAvailable = syncManager.isNetworkAvailable

    suspend fun triggerSync() = syncManager.triggerSync()
}

enum class Screen {
    HOME,
    EVENT_CREATION,
    PARTICIPANT_MANAGEMENT,
    POLL_VOTING,
    POLL_RESULTS
}

data class AppState(
    val currentScreen: Screen = Screen.EVENT_CREATION,
    val currentEventId: String? = null,
    val currentUserId: String? = null  // Provided by auth state
)

@Composable
fun App() {
    MaterialTheme {
        val scope = rememberCoroutineScope()

        // Initialize authentication dependencies
        val authService = remember { JvmAuthenticationService() }
        val authStateManager = remember {
            AuthStateManager(
                secureStorage = JvmSecureTokenStorage(),
                authService = authService,
                enableOAuth = BuildConfig.ENABLE_OAUTH
            )
        }

        // Initialize on first composition
        LaunchedEffect(Unit) {
            authStateManager.initialize()
        }

        // Observe authentication state
        val authState by authStateManager.authState.collectAsState()

        // Google OAuth helper
        var browserOAuthHelper by remember { mutableStateOf<BrowserOAuthHelper?>(null) }
        var isOAuthInProgress by remember { mutableStateOf(false) }

        // Render appropriate screen based on auth state
        when (val state = authState) {
            is AuthState.Loading -> {
                LoadingScreen()
            }
            is AuthState.Unauthenticated -> {
                LoginScreen(
                    onGoogleSignIn = {
                        if (!isOAuthInProgress) {
                            isOAuthInProgress = true
                            scope.launch {
                                // Create browser OAuth helper
                                val helper = BrowserOAuthHelper(
                                    clientId = "YOUR_GOOGLE_CLIENT_ID", // TODO: Move to BuildConfig
                                    authorizationEndpoint = "https://accounts.google.com/o/oauth2/v2/auth"
                                )
                                browserOAuthHelper = helper

                                // Start OAuth flow
                                val result = helper.authorize()
                                result.onSuccess { authCode ->
                                    authStateManager.login(authCode, OAuthProvider.GOOGLE)
                                }.onFailure { error ->
                                    println("Google Sign-In failed: ${error.message}")
                                }

                                isOAuthInProgress = false
                            }
                        }
                    },
                    onAppleSignIn = {
                        // Apple Sign-In not available on JVM
                    }
                )
            }
            is AuthState.Authenticated -> {
                // Initialize app dependencies with authenticated user
                val database = remember { DatabaseProvider.getDatabase(JvmDatabaseFactory()) }
                val networkDetector = remember { JvmNetworkStatusDetector() }
                val httpClient = remember { KtorSyncHttpClient() }
                val userRepository = remember { UserRepository(database) }

                // Store current access token for SyncManager
                var currentAccessToken by remember { mutableStateOf<String?>(null) }

                // Load access token
                LaunchedEffect(state.userId) {
                    currentAccessToken = authStateManager.getCurrentAccessToken()
                }

                val syncManager = remember {
                    SyncManager(
                        database = database,
                        eventRepository = DatabaseEventRepository(database),
                        userRepository = userRepository,
                        networkDetector = networkDetector,
                        httpClient = httpClient,
                        authTokenProvider = { currentAccessToken },
                        authTokenRefreshProvider = {
                            authStateManager.refreshTokenIfNeeded()
                            authStateManager.getCurrentAccessToken()
                        }
                    )
                }

                val repository = remember {
                    SyncedEventRepository(database, syncManager)
                }

                var appState by remember {
                    mutableStateOf(AppState(currentUserId = state.userId))
                }
                var hasPendingChanges by remember { mutableStateOf(false) }

                // Update pending changes when sync status changes
                LaunchedEffect(repository.syncStatus.collectAsState().value) {
                    hasPendingChanges = syncManager.hasPendingChanges()
                }

                Column(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.background)
                        .fillMaxSize()
                ) {
                    // Sync status indicator (commented out for now)
                    // SyncStatusIndicator(
                    //     syncStatus = repository.syncStatus,
                    //     isNetworkAvailable = repository.isNetworkAvailable,
                    //     hasPendingChanges = hasPendingChanges
                    // )

                    when (appState.currentScreen) {
                        Screen.HOME -> {
                            // Simple home screen - just navigate to event creation
                            Text("Home - Click to create an event")
                            Button(onClick = {
                                appState = appState.copy(currentScreen = Screen.EVENT_CREATION)
                            }) {
                                Text("Create Event")
                            }
                        }
                        Screen.EVENT_CREATION -> {
                            EventCreationScreen(
                                userId = state.userId,
                                onEventCreated = { event ->
                                    scope.launch {
                                        repository.createEvent(event).onSuccess {
                                            appState = appState.copy(
                                                currentEventId = event.id,
                                                currentScreen = Screen.PARTICIPANT_MANAGEMENT
                                            )
                                        }
                                    }
                                },
                                onBack = {
                                    appState = appState.copy(currentScreen = Screen.HOME)
                                }
                            )
                        }
                        Screen.PARTICIPANT_MANAGEMENT -> {
                            val event = repository.getEvent(appState.currentEventId ?: "")
                            if (event != null) {
                                ParticipantManagementScreen(
                                    event = event,
                                    repository = repository,
                                    onParticipantsAdded = { eventId ->
                                        appState = appState.copy(currentEventId = eventId)
                                    },
                                    onNavigateToPoll = { eventId ->
                                        appState = appState.copy(
                                            currentEventId = eventId,
                                            currentScreen = Screen.POLL_VOTING
                                        )
                                    }
                                )
                            }
                        }
                        Screen.POLL_VOTING -> {
                            val event = repository.getEvent(appState.currentEventId ?: "")
                            if (event != null) {
                                PollVotingScreen(
                                    event = event,
                                    repository = repository,
                                    participantId = state.userId,
                                    onVoteSubmitted = { eventId ->
                                        appState = appState.copy(
                                            currentEventId = eventId,
                                            currentScreen = Screen.POLL_RESULTS
                                        )
                                    }
                                )
                            }
                        }
                        Screen.POLL_RESULTS -> {
                            val event = repository.getEvent(appState.currentEventId ?: "")
                            if (event != null) {
                                PollResultsScreen(
                                    event = event,
                                    repository = repository,
                                    userId = state.userId,
                                    onDateConfirmed = { eventId ->
                                        // Could navigate to event details or home
                                        appState = appState.copy(currentEventId = eventId)
                                    }
                                )
                            }
                        }
                    }
                }
            }
            is AuthState.Error -> {
                ErrorScreen(
                    message = state.message,
                    onRetry = {
                        scope.launch {
                            authStateManager.initialize()
                        }
                    }
                )
            }
        }

        // Cleanup on disposal
        DisposableEffect(Unit) {
            onDispose {
                authStateManager.dispose()
                browserOAuthHelper?.close()
            }
        }
    }
}