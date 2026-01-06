package com.guyghost.wakeve.di

import com.guyghost.wakeve.AndroidDatabaseFactory
import com.guyghost.wakeve.DatabaseEventRepository
import com.guyghost.wakeve.DatabaseFactory
import com.guyghost.wakeve.DatabaseProvider
import com.guyghost.wakeve.EventRepositoryInterface
import com.guyghost.wakeve.database.WakevDb
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
}
