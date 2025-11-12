package com.guyghost.wakeve

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.guyghost.wakeve.database.WakevDb

/**
 * Test database factory that creates an in-memory SQLite database.
 * Used for unit and integration tests.
 */
class TestDatabaseFactory : DatabaseFactory {
    override fun createDriver(): SqlDriver {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        WakevDb.Schema.create(driver)
        return driver
    }
}

/**
 * Creates a fresh database instance for testing.
 * Resets the DatabaseProvider singleton to ensure isolation between tests.
 */
fun createTestDatabase(): WakevDb {
    DatabaseProvider.resetDatabase()
    return DatabaseProvider.getDatabase(TestDatabaseFactory())
}
