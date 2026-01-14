package com.guyghost.wakeve.di

import com.guyghost.wakeve.AndroidDatabaseFactory
import com.guyghost.wakeve.DatabaseEventRepository
import com.guyghost.wakeve.DatabaseFactory
import com.guyghost.wakeve.DatabaseProvider
import com.guyghost.wakeve.EventRepositoryInterface
import com.guyghost.wakeve.auth.shell.services.AndroidTokenStorage
import com.guyghost.wakeve.auth.shell.services.AuthService
import com.guyghost.wakeve.auth.shell.services.EmailAuthService
import com.guyghost.wakeve.auth.shell.services.GuestModeService
import com.guyghost.wakeve.auth.shell.services.TokenStorage
import com.guyghost.wakeve.auth.shell.statemachine.AuthStateMachine
import com.guyghost.wakeve.database.WakevDb
import com.guyghost.wakeve.ui.auth.AuthViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Android-specific Koin module providing platform dependencies.
 *
 * This module provides:
 * - DatabaseFactory using Android SQLite driver
 * - WakevDb database instance
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
     * Provide WakevDb singleton.
     *
     * The database is created once and shared across the application.
     */
    single<WakevDb> {
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
        val database = get<WakevDb>()
        DatabaseEventRepository(db = database, syncManager = null)
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
        AuthViewModel(authStateMachine = get())
    }
}
