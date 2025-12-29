package com.guyghost.wakeve.di

import androidx.lifecycle.ViewModel
import com.guyghost.wakeve.EventRepositoryInterface
import com.guyghost.wakeve.presentation.state.EventManagementContract
import com.guyghost.wakeve.presentation.statemachine.EventManagementStateMachine
import com.guyghost.wakeve.presentation.usecase.CreateEventUseCase
import com.guyghost.wakeve.presentation.usecase.LoadEventsUseCase
import com.guyghost.wakeve.viewmodel.EventManagementViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin module for dependency injection in Compose application.
 *
 * This module provides:
 * - ViewModels for Compose screens
 * - State machines for business logic
 * - Use cases and repositories
 *
 * ## Setup
 *
 * In your Android Activity or Application:
 *
 * ```kotlin
 * startKoin {
 *     modules(appModule)
 * }
 * ```
 */
val appModule: Module = module {
    // ========================================================================
    // Repository
    // ========================================================================

    /**
     * Provide EventRepositoryInterface.
     *
     * For now, we provide null since EventManagementStateMachine
     * handles it optionally. In production, this would be injected
     * from a proper implementation (e.g., DatabaseEventRepository).
     */
    single<EventRepositoryInterface>(named("eventRepository")) { null }

    // ========================================================================
    // Use Cases
    // ========================================================================

    /**
     * Provide LoadEventsUseCase as a factory.
     *
     * Each time a use case is requested, Koin creates a new instance
     * with the repository dependency injected.
     */
    factory {
        val repository = get<EventRepositoryInterface>(named("eventRepository"))
        LoadEventsUseCase(eventRepository = repository)
    }

    /**
     * Provide CreateEventUseCase as a factory.
     *
     * Each time a use case is requested, Koin creates a new instance
     * with the repository dependency injected.
     */
    factory {
        val repository = get<EventRepositoryInterface>(named("eventRepository"))
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

        // Create a CoroutineScope for the state machine that survives configuration changes
        val scope = kotlinx.coroutines.CoroutineScope(
            Dispatchers.Main.immediate + SupervisorJob()
        )

        EventManagementStateMachine(
            loadEventsUseCase = loadEventsUseCase,
            createEventUseCase = createEventUseCase,
            eventRepository = getOrNull(named("eventRepository")),
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

/**
 * Initialize Koin for Compose application.
 *
 * This should be called once in your Android Activity or Application class:
 *
 * ```kotlin
 * class MainActivity : ComponentActivity() {
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *         initializeKoin()
 *         // ... rest of onCreate
 *     }
 * }
 * ```
 *
 * Or in your Application class:
 *
 * ```kotlin
 * class MyApplication : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *         initializeKoin()
 *     }
 * }
 * ```
 */
fun initializeKoin() {
    val koinApplication = org.koin.core.context.startKoin {
        // Load the app module
        modules(appModule)
    }
}
