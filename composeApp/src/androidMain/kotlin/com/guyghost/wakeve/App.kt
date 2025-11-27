package com.guyghost.wakeve

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.guyghost.wakeve.auth.AndroidAuthenticationService
import com.guyghost.wakeve.auth.AuthState
import com.guyghost.wakeve.auth.AuthStateManager
import com.guyghost.wakeve.auth.GoogleSignInHelper
import com.guyghost.wakeve.database.WakevDb
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.OAuthProvider
import com.guyghost.wakeve.models.Vote
import com.guyghost.wakeve.sync.AndroidNetworkStatusDetector
import com.guyghost.wakeve.sync.KtorSyncHttpClient
import com.guyghost.wakeve.sync.SyncManager
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview

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
@Preview
fun App() {
    MaterialTheme {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        // Initialize authentication dependencies
        val authService = remember { AndroidAuthenticationService(context) }
        val authStateManager = remember {
            AuthStateManager(
                secureStorage = com.guyghost.wakeve.security.AndroidSecureTokenStorage(context),
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

        // Google Sign-In helper
        val googleSignInHelper = remember {
            GoogleSignInHelper(
                context = context,
                clientId = "YOUR_GOOGLE_CLIENT_ID", // TODO: Move to BuildConfig
                serverClientId = "YOUR_SERVER_CLIENT_ID" // TODO: Move to BuildConfig
            )
        }

        // Activity result launcher for Google Sign-In
        val googleSignInLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            scope.launch {
                val signInResult = googleSignInHelper.handleSignInResult(result.data)
                signInResult.onSuccess { authCode ->
                    authStateManager.login(authCode, OAuthProvider.GOOGLE)
                }.onFailure { error ->
                    // Error handling is done by AuthStateManager
                    println("Google Sign-In failed: ${error.message}")
                }
            }
        }

        // Render appropriate screen based on auth state
        when (val state = authState) {
            is AuthState.Loading -> {
                LoadingScreen()
            }
            is AuthState.Unauthenticated -> {
                LoginScreen(
                    onGoogleSignIn = {
                        val signInIntent = googleSignInHelper.getSignInIntent()
                        googleSignInLauncher.launch(signInIntent)
                    },
                    onAppleSignIn = {
                        // Apple Sign-In not available on Android
                    }
                )
            }
            is AuthState.Authenticated -> {
                // Store current access token for SyncManager
                var currentAccessToken by remember { mutableStateOf<String?>(null) }

                // Load access token
                LaunchedEffect(state.userId) {
                    currentAccessToken = authStateManager.getCurrentAccessToken()
                }

                // Initialize app dependencies with authenticated user
                val database = remember { DatabaseProvider.getDatabase(AndroidDatabaseFactory(context)) }
                val networkDetector = remember { AndroidNetworkStatusDetector(context) }
                val httpClient = remember { KtorSyncHttpClient() }
                val userRepository = remember { UserRepository(database) }

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
                    // Sync status indicator
                    SyncStatusIndicator(
                        syncStatus = repository.syncStatus,
                        isNetworkAvailable = repository.isNetworkAvailable,
                        hasPendingChanges = hasPendingChanges
                    )

                    when (appState.currentScreen) {
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
            }
        }
    }
}