package com.guyghost.wakeve.di

import com.guyghost.wakeve.AndroidDatabaseFactory
import com.guyghost.wakeve.ai.AiTextGenerationClient
import com.guyghost.wakeve.ai.EventSummaryAiAssistant
import com.guyghost.wakeve.ai.FirebaseAiLogicCloudTextGenerationClient
import com.guyghost.wakeve.ai.EventPlanningAiAssistant
import com.guyghost.wakeve.ai.FallbackEventPlanningAiAssistant
import com.guyghost.wakeve.ai.HybridOrganizerMessageAiAssistant
import com.guyghost.wakeve.ai.MlKitLocalTextGenerationClient
import com.guyghost.wakeve.ai.MlKitEventPlanningAiAssistant
import com.guyghost.wakeve.ai.OnDeviceEventSummaryAiAssistant
import com.guyghost.wakeve.ai.OrganizerMessageAiAssistant
import com.guyghost.wakeve.ai.PlanningAgentClient
import com.guyghost.wakeve.ai.RuleBasedEventPlanningAiAssistant
import com.guyghost.wakeve.ai.UnavailablePlanningAgentClient
import com.guyghost.wakeve.database.DatabaseFactory
import com.guyghost.wakeve.database.DatabaseProvider
import com.guyghost.wakeve.repository.DatabaseEventRepository
import com.guyghost.wakeve.repository.EventRepositoryInterface
import com.guyghost.wakeve.auth.shell.services.AndroidTokenStorage
import com.guyghost.wakeve.auth.shell.services.AuthService
import com.guyghost.wakeve.auth.shell.services.EmailAuthService
import com.guyghost.wakeve.auth.shell.services.GuestModeService
import com.guyghost.wakeve.auth.shell.services.AccountDeletionGateway
import com.guyghost.wakeve.auth.shell.services.TokenStorage
import com.guyghost.wakeve.auth.shell.statemachine.AuthStateMachine
import com.guyghost.wakeve.auth.SessionRepository
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.notification.NoConfiguredAPNsSender
import com.guyghost.wakeve.notification.NoConfiguredFCMSender
import com.guyghost.wakeve.notification.NotificationPreferencesRepository
import com.guyghost.wakeve.notification.NotificationPreferencesRepositoryInterface
import com.guyghost.wakeve.notification.NotificationService
import com.guyghost.wakeve.repository.ScenarioRepository
import com.guyghost.wakeve.ui.auth.AuthViewModel
import com.guyghost.wakeve.auth.AndroidAccountDeletionGateway
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Android-specific Koin module providing platform dependencies.
 *
 * This module provides:
 * - DatabaseFactory using Android SQLite driver
 * - WakeveDb database instance
 * - DatabaseEventRepository for persistent storage
 * - Authentication services (TokenStorage, AuthService, etc.)
 * - AuthStateMachine and AuthViewModel
 *
 * ## Usage
 *
 * ```kotlin
 * startKoin {
 *     androidContext(this@MainActivity)
 *     modules(appModule, platformModule())
 * }
 * ```
 */
fun platformModule(): Module = module {
    // ========================================================================
    // Database Layer
    // ========================================================================

    /**
     * Provide Android-specific DatabaseFactory.
     *
     * Uses AndroidSqliteDriver with the application context.
     */
    single<DatabaseFactory> {
        AndroidDatabaseFactory(androidContext())
    }

    /**
     * Provide WakeveDb singleton.
     *
     * The database is created once and shared across the application.
     */
    single<WakeveDb> {
        val factory = get<DatabaseFactory>()
        DatabaseProvider.getDatabase(factory)
    }

    // ========================================================================
    // Repository Layer
    // ========================================================================

    /**
     * Provide DatabaseEventRepository as the EventRepositoryInterface implementation.
     *
     * This connects the State Machine to the SQLite database for
     * persistent offline-first storage.
     */
    single<EventRepositoryInterface> {
        val database = get<WakeveDb>()
        DatabaseEventRepository(db = database, syncManager = null)
    }

    /**
     * Provide ScenarioRepository for scenario comparison and final selection.
     */
    single {
        val database = get<WakeveDb>()
        ScenarioRepository(database)
    }

    single<NotificationPreferencesRepositoryInterface> {
        NotificationPreferencesRepository(get())
    }

    single {
        NotificationService(
            database = get(),
            preferencesRepository = get(),
            fcmSender = NoConfiguredFCMSender,
            apnsSender = NoConfiguredAPNsSender
        )
    }

    single {
        SessionRepository(get())
    }
    
    // ========================================================================
    // Authentication Layer
    // ========================================================================
    
    /**
     * Provide Android-specific TokenStorage using EncryptedSharedPreferences.
     *
     * Uses Android Keystore for secure key storage with AES-256-GCM encryption.
     */
    single<TokenStorage> {
        AndroidTokenStorage(androidContext())
    }
    
    /**
     * Provide AuthService for OAuth authentication (Google, Apple).
     *
     * This is a platform-specific (expect/actual) implementation.
     */
    single {
        AuthService()
    }
    
    /**
     * Provide EmailAuthService for email OTP authentication.
     *
     * Handles OTP generation, verification, and session creation.
     */
    single {
        EmailAuthService(
            otpExpiryMinutes = 5,
            maxAttempts = 3
        )
    }
    
    /**
     * Provide GuestModeService for skip/guest mode.
     *
     * Allows users to use the app without authentication.
     * All data is stored locally and never synced.
     */
    single {
        GuestModeService(tokenStorage = get())
    }

    single<AccountDeletionGateway> {
        AndroidAccountDeletionGateway()
    }
    
    /**
     * Provide AuthStateMachine singleton.
     *
     * This is the single source of truth for authentication state.
     * It manages the complete auth flow: OAuth, Email OTP, Guest mode.
     */
    single {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        
        AuthStateMachine(
            authService = get(),
            emailAuthService = get(),
            guestModeService = get(),
            tokenStorage = get(),
            accountDeletionGateway = get(),
            scope = scope
        )
    }
    
    /**
     * Provide AuthViewModel as a factory.
     *
     * This wraps the AuthStateMachine for Compose UI.
     * Each screen that needs auth state gets a new ViewModel instance,
     * but they all share the same AuthStateMachine singleton.
     *
     * Usage in Compose:
     * ```kotlin
     * val authViewModel: AuthViewModel = koinInject()
     * ```
     */
    factory {
        AuthViewModel(
            authStateMachine = get(),
            tokenStorage = get()
        )
    }

    single<EventPlanningAiAssistant> {
        FallbackEventPlanningAiAssistant(
            primary = MlKitEventPlanningAiAssistant(
                fallback = RuleBasedEventPlanningAiAssistant()
            ),
            fallback = RuleBasedEventPlanningAiAssistant()
        )
    }

    single<AiTextGenerationClient>(named("onDeviceAiTextGeneration")) {
        MlKitLocalTextGenerationClient()
    }

    single<AiTextGenerationClient>(named("firebaseAiLogicTextGeneration")) {
        FirebaseAiLogicCloudTextGenerationClient()
    }

    single<EventSummaryAiAssistant> {
        OnDeviceEventSummaryAiAssistant(
            localClient = get(named("onDeviceAiTextGeneration"))
        )
    }

    single<OrganizerMessageAiAssistant> {
        HybridOrganizerMessageAiAssistant(
            onDeviceClient = get(named("onDeviceAiTextGeneration")),
            cloudClient = get(named("firebaseAiLogicTextGeneration"))
        )
    }

    single<PlanningAgentClient> {
        UnavailablePlanningAgentClient()
    }
}
