package com.guyghost.wakeve

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.guyghost.wakeve.database.WakevDb

/**
 * JVM test database factory that creates an in-memory SQLite database.
 */
class TestDatabaseFactory : DatabaseFactory {
    override fun createDriver(): SqlDriver {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        // Enable foreign key constraints
        driver.execute(null, "PRAGMA foreign_keys = ON", 0)
        WakevDb.Schema.create(driver)
        return driver
    }
}