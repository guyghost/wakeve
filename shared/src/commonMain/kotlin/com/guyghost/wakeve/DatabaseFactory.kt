package com.guyghost.wakeve

import com.guyghost.wakeve.database.WakevDb
import app.cash.sqldelight.db.SqlDriver

/**
 * Factory for creating and managing the Wakev database instance.
 * Platform-specific implementations handle driver creation.
 */
interface DatabaseFactory {
    fun createDriver(): SqlDriver
}

/**
 * Creates a singleton instance of the WakevDb database.
 * Handles initialization and driver setup for the current platform.
 */
object DatabaseProvider {
    private var _database: WakevDb? = null

    fun getDatabase(factory: DatabaseFactory): WakevDb {
        if (_database == null) {
            val driver = factory.createDriver()
            _database = WakevDb(driver)
        }
        return _database!!
    }

    fun resetDatabase() {
        _database = null
    }
}
