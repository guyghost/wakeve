package com.guyghost.wakeve.di

import com.guyghost.wakeve.EventRepositoryInterface
import com.guyghost.wakeve.presentation.statemachine.EventManagementStateMachine
import com.guyghost.wakeve.presentation.usecase.CreateEventUseCase
import com.guyghost.wakeve.presentation.usecase.LoadEventsUseCase
import com.guyghost.wakeve.viewmodel.EventManagementViewModel
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

        // Create a CoroutineScope for the state machine that survives configuration changes
        val scope = kotlinx.coroutines.CoroutineScope(
            Dispatchers.Main.immediate + SupervisorJob()
        )

        EventManagementStateMachine(
            loadEventsUseCase = loadEventsUseCase,
            createEventUseCase = createEventUseCase,
            eventRepository = repository,
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
}
