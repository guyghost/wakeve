package com.guyghost.wakeve

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.guyghost.wakeve.database.DatabaseFactory
import com.guyghost.wakeve.database.WakeveDb

/**
 * iOS-specific database factory using the native SQLite driver.
 */
class IosDatabaseFactory : DatabaseFactory {
    override fun createDriver(): SqlDriver {
        val driver = NativeSqliteDriver(WakeveDb.Schema, "wakev.db")
        driver.execute(null, "PRAGMA foreign_keys = ON", 0).value
        return driver
    }
}
