package com.guyghost.wakeve.di

import com.guyghost.wakeve.repository.EventRepositoryInterface
import com.guyghost.wakeve.repository.ScenarioRepository
import com.guyghost.wakeve.ai.EventPlanningAiAssistant
import com.guyghost.wakeve.ai.EventSummaryAiAssistant
import com.guyghost.wakeve.ai.OrganizerMessageAiAssistant
import com.guyghost.wakeve.ai.PlanningAgentClient
import com.guyghost.wakeve.ai.RuleBasedEventPlanningAiAssistant
import com.guyghost.wakeve.ai.UnavailablePlanningAgentClient
import com.guyghost.wakeve.gamification.BadgeEligibilityChecker
import com.guyghost.wakeve.gamification.GamificationService
import com.guyghost.wakeve.gamification.repository.InMemoryUserBadgesRepository
import com.guyghost.wakeve.gamification.repository.InMemoryUserPointsRepository
import com.guyghost.wakeve.gamification.repository.UserBadgesRepository
import com.guyghost.wakeve.gamification.repository.UserPointsRepository
import com.guyghost.wakeve.presentation.statemachine.EventManagementStateMachine
import com.guyghost.wakeve.presentation.statemachine.ScenarioManagementStateMachine
import com.guyghost.wakeve.presentation.usecase.CreateEventUseCase
import com.guyghost.wakeve.presentation.usecase.CreateScenarioUseCase
import com.guyghost.wakeve.presentation.usecase.DeleteScenarioUseCase
import com.guyghost.wakeve.presentation.usecase.IScenarioRepositoryWrite
import com.guyghost.wakeve.presentation.usecase.LoadEventsUseCase
import com.guyghost.wakeve.presentation.usecase.LoadScenariosUseCase
import com.guyghost.wakeve.presentation.usecase.UpdateScenarioUseCase
import com.guyghost.wakeve.presentation.usecase.VoteScenarioUseCase
import com.guyghost.wakeve.viewmodel.AiWorkflowDemoViewModel
import com.guyghost.wakeve.viewmodel.EventManagementViewModel
import com.guyghost.wakeve.viewmodel.EventPlanningAssistantViewModel
import com.guyghost.wakeve.viewmodel.ProfileViewModel
import com.guyghost.wakeve.viewmodel.ScenarioManagementViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Common Koin module for dependency injection in Compose application.
 *
 * This module provides:
 * - ViewModels for Compose screens
 * - State machines for business logic
 * - Use cases
 *
 * ## Note on Repository
 *
 * The repository is platform-specific and must be provided via
 * platformModule() from androidMain or iosMain.
 *
 * ## Setup (Android)
 *
 * ```kotlin
 * startKoin {
 *     androidContext(this@MainActivity)
 *     modules(appModule, platformModule())
 * }
 * ```
 */
val appModule: Module = module {
    // ========================================================================
    // Gamification
    // ========================================================================

    single<UserPointsRepository> {
        InMemoryUserPointsRepository()
    }

    single<UserBadgesRepository> {
        InMemoryUserBadgesRepository()
    }

    single {
        BadgeEligibilityChecker(
            userPointsRepository = get(),
            userBadgesRepository = get()
        )
    }

    single {
        GamificationService(
            userPointsRepository = get(),
            userBadgesRepository = get(),
            badgeEligibilityChecker = get()
        )
    }

    // ========================================================================
    // Use Cases
    // ========================================================================

    /**
     * Provide LoadEventsUseCase as a factory.
     *
     * The repository is injected from platformModule.
     * If no repository is provided, returns null (mock mode with sample data).
     */
    factory {
        val repository = getOrNull<EventRepositoryInterface>()
        LoadEventsUseCase(eventRepository = repository)
    }

    /**
     * Provide CreateEventUseCase as a factory.
     *
     * The repository is injected from platformModule.
     * If no repository is provided, returns null (mock mode).
     */
    factory {
        val repository = getOrNull<EventRepositoryInterface>()
        CreateEventUseCase(eventRepository = repository)
    }

    factory {
        val repository = getOrNull<ScenarioRepository>()
        if (repository != null) {
            LoadScenariosUseCase(repository)
        } else {
            LoadScenariosUseCase(get<IScenarioRepositoryWrite>())
        }
    }

    factory {
        val repository = getOrNull<ScenarioRepository>()
        if (repository != null) {
            CreateScenarioUseCase(repository)
        } else {
            CreateScenarioUseCase(get<IScenarioRepositoryWrite>())
        }
    }

    factory {
        val repository = getOrNull<ScenarioRepository>()
        if (repository != null) {
            UpdateScenarioUseCase(repository)
        } else {
            UpdateScenarioUseCase(get<IScenarioRepositoryWrite>())
        }
    }

    factory {
        val repository = getOrNull<ScenarioRepository>()
        if (repository != null) {
            DeleteScenarioUseCase(repository)
        } else {
            DeleteScenarioUseCase(get<IScenarioRepositoryWrite>())
        }
    }

    factory {
        val repository = getOrNull<ScenarioRepository>()
        if (repository != null) {
            VoteScenarioUseCase(repository)
        } else {
            VoteScenarioUseCase(get<IScenarioRepositoryWrite>())
        }
    }

    // ========================================================================
    // State Machines
    // ========================================================================

    /**
     * Provide EventManagementStateMachine singleton.
     *
     * The state machine is scoped to application lifecycle using a
     * CoroutineScope with SupervisorJob to ensure it survives configuration changes.
     */
    single {
        val loadEventsUseCase = get<LoadEventsUseCase>()
        val createEventUseCase = get<CreateEventUseCase>()
        val repository = getOrNull<EventRepositoryInterface>()

        // If the repository implements SampleEventSeeder (DatabaseEventRepository),
        // pass it to the state machine for first-launch onboarding support
        val sampleEventSeeder = repository as? com.guyghost.wakeve.presentation.statemachine.SampleEventSeeder

        // Create a CoroutineScope for the state machine that survives configuration changes
        val scope = kotlinx.coroutines.CoroutineScope(
            Dispatchers.Main.immediate + SupervisorJob()
        )

        val stateMachine = EventManagementStateMachine(
            loadEventsUseCase = loadEventsUseCase,
            createEventUseCase = createEventUseCase,
            eventRepository = repository,
            sampleEventSeeder = sampleEventSeeder,
            scope = scope
        )

        // Wire SyncManager conflict callback → state machine side effect.
        // When SyncManager detects critical conflicts it calls this lambda,
        // which emits a ConflictDetected side effect to the UI layer.
        // Hook: register via SyncManager.onCriticalConflictsDetected = { summary ->
        //     stateMachine.dispatch(EventManagementContract.Intent.NotifyConflict(summary))
        // }
        // (Full wiring happens when SyncManager is added to the DI graph in a future phase.)

        stateMachine
    }

    /**
     * Provide ScenarioManagementStateMachine singleton for destination and lodging scenarios.
     */
    single {
        val scope = kotlinx.coroutines.CoroutineScope(
            Dispatchers.Main.immediate + SupervisorJob()
        )

        ScenarioManagementStateMachine(
            loadScenariosUseCase = get(),
            createScenarioUseCase = get(),
            voteScenarioUseCase = get(),
            updateScenarioUseCase = get(),
            deleteScenarioUseCase = get(),
            eventRepository = getOrNull<EventRepositoryInterface>(),
            scenarioRepository = getOrNull<ScenarioRepository>(),
            scope = scope
        )
    }

    // ========================================================================
    // ViewModels
    // ========================================================================

    /**
     * Provide EventManagementViewModel for Compose screens.
     *
     * This is a factory that creates a new ViewModel for each screen that
     * requests it. The ViewModel will be cached by Compose navigation
     * framework.
     *
     * Usage in Compose:
     * ```kotlin
     * @Composable
     * fun MyScreen(
     *     viewModel: EventManagementViewModel = koinViewModel()
     * ) {
     *     // ...
     * }
     * ```
     */
    factory {
        val stateMachine = get<EventManagementStateMachine>()
        EventManagementViewModel(stateMachine = stateMachine)
    }

    factory {
        val stateMachine = get<ScenarioManagementStateMachine>()
        ScenarioManagementViewModel(stateMachine = stateMachine)
    }

    factory { (userId: String) ->
        ProfileViewModel(
            currentUserId = userId,
            gamificationService = get(),
            userBadgesRepository = get()
        )
    }

    factory {
        EventPlanningAssistantViewModel(
            assistant = getOrNull<EventPlanningAiAssistant>() ?: RuleBasedEventPlanningAiAssistant()
        )
    }

    factory {
        val summaryAssistant = getOrNull<EventSummaryAiAssistant>()
        val messageAssistant = getOrNull<OrganizerMessageAiAssistant>()
        val planningAgentClient = getOrNull<PlanningAgentClient>() ?: UnavailablePlanningAgentClient()

        if (summaryAssistant != null && messageAssistant != null) {
            AiWorkflowDemoViewModel(
                summaryAssistant = summaryAssistant,
                messageAssistant = messageAssistant,
                planningAgentClient = planningAgentClient
            )
        } else {
            AiWorkflowDemoViewModel(planningAgentClient = planningAgentClient)
        }
    }
}
