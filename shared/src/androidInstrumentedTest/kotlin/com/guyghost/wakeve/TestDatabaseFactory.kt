package com.guyghost.wakeve

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.guyghost.wakeve.database.WakevDb

/**
 * Android test database factory that creates an in-memory SQLite database.
 */
actual class TestDatabaseFactory actual constructor() : DatabaseFactory {
    override fun createDriver(): SqlDriver {
        // For tests, we can use a temporary database
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<Context>()
        val driver = AndroidSqliteDriver(WakevDb.Schema, context, "test_wakev.db")
        // Note: AndroidSqliteDriver doesn't support in-memory, so we use a file that gets cleaned up
        return driver
    }
}