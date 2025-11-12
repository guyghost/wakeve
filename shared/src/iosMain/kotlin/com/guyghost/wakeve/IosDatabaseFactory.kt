package com.guyghost.wakeve

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.guyghost.wakeve.database.WakevDb

/**
 * iOS-specific database factory using the native SQLite driver.
 */
class IosDatabaseFactory : DatabaseFactory {
    override fun createDriver(): SqlDriver {
        return NativeSqliteDriver(WakevDb.Schema, "wakev.db")
    }
}
