package com.guyghost.wakeve

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.guyghost.wakeve.database.WakevDb
import java.util.concurrent.atomic.AtomicInteger

/**
 * JVM test database factory that creates an in-memory SQLite database.
 * Each TestDatabaseFactory instance gets a unique database to ensure test isolation.
 */
class TestDatabaseFactory : DatabaseFactory {
    companion object {
        private val counter = AtomicInteger(0)
    }

    private val dbName = "test_${counter.incrementAndGet()}"
    private var driver: SqlDriver? = null

    override fun createDriver(): SqlDriver {
        // Create a unique named in-memory database
        // Named databases persist within the same connection URL
        if (driver == null) {
            driver = JdbcSqliteDriver("jdbc:sqlite:file:$dbName?mode=memory&cache=shared").also {
                it.execute(null, "PRAGMA foreign_keys = ON", 0)
                WakevDb.Schema.create(it)
            }
        }
        return driver!!
    }
}

/**
 * Creates a fresh database for each test, bypassing the singleton.
 * Use this for tests that need isolation.
 */
fun createFreshTestDatabase(): WakevDb {
    val factory = TestDatabaseFactory()
    return WakevDb(factory.createDriver())
}