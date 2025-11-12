package com.guyghost.wakeve

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.guyghost.wakeve.database.WakevDb

/**
 * Android-specific database factory using the Android SQLite driver.
 */
class AndroidDatabaseFactory(private val context: Context) : DatabaseFactory {
    override fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            WakevDb.Schema,
            context,
            "wakev.db"
        )
    }
}
