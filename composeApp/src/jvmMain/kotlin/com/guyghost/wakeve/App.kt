package com.guyghost.wakeve

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.guyghost.wakeve.database.WakevDb
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.Vote
import com.guyghost.wakeve.sync.SyncManager
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
    val currentUserId: String = "organizer-1" // TODO: Get from auth
)

@Composable
fun App() {
    MaterialTheme {
        // For JVM, use a simple in-memory database or mock
        // TODO: Implement proper JVM database factory
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory())
        val networkDetector = JvmNetworkStatusDetector()
        val httpClient = KtorSyncHttpClient()
        val userRepository = UserRepository(database)

        val syncManager = remember {
            SyncManager(
                database = database,
                eventRepository = DatabaseEventRepository(database),
                userRepository = userRepository,
                networkDetector = networkDetector,
                httpClient = httpClient,
                authTokenProvider = { "demo-token" } // TODO: Get from auth
            )
        }

        val repository = remember {
            SyncedEventRepository(database, syncManager)
        }

        var appState by remember { mutableStateOf(AppState()) }
        val scope = rememberCoroutineScope()

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
                Screen.EVENT_CREATION -> {
                    EventCreationScreen(
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
}