package com.guyghost.wakeve

import app.cash.sqldelight.db.SqlDriver
import com.guyghost.wakeve.database.WakeveDb

/**
 * Factory for creating and managing the Wakev database instance.
 * Platform-specific implementations handle driver creation.
 */
interface DatabaseFactory {
    fun createDriver(): SqlDriver
}

/**
 * Creates a singleton instance of the WakeveDb database.
 * Handles initialization and driver setup for the current platform.
 */
object DatabaseProvider {
    private var _database: WakeveDb? = null

    fun getDatabase(factory: DatabaseFactory): WakeveDb {
        if (_database == null) {
            val driver = factory.createDriver()
            _database = WakeveDb(driver)
        }
        return _database!!
    }

    fun resetDatabase() {
        _database = null
    }
}
