package com.guyghost.wakeve

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.guyghost.wakeve.database.DatabaseFactory
import com.guyghost.wakeve.database.WakeveDb

/**
 * Android-specific database factory using the Android SQLite driver.
 */
class AndroidDatabaseFactory(private val context: Context) : DatabaseFactory {
    override fun createDriver(): SqlDriver {
        val driver = AndroidSqliteDriver(
            WakeveDb.Schema,
            context,
            "wakev.db"
        )
        driver.execute(null, "PRAGMA foreign_keys = ON", 0).value
        return driver
    }
}
