package com.guyghost.wakeve

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.guyghost.wakeve.database.DatabaseFactory
import com.guyghost.wakeve.database.WakeveDb
import java.io.File
import java.util.Properties

/**
 * JVM-specific database factory using the JDBC SQLite driver.
 */
class JvmDatabaseFactory(private val dbPath: String = "wakev.db") : DatabaseFactory {
    override fun createDriver(): SqlDriver {
        val dbFile = File(dbPath)
        // Opening a SQLite JDBC connection creates the file. Capture this before constructing
        // the driver so a new durable database receives the SQLDelight schema exactly once.
        val isNewDatabase = !dbFile.exists()
        val driver: SqlDriver = JdbcSqliteDriver(
            url = "jdbc:sqlite:$dbPath",
            properties = Properties().apply {
                setProperty("foreign_keys", "true")
            }
        )
        driver.execute(null, "PRAGMA foreign_keys = ON", 0).value
        
        // Initialize schema if database doesn't exist
        if (isNewDatabase) {
            WakeveDb.Schema.create(driver)
        }
        // Schema initialization may use a transaction; SQLite only accepts this
        // connection-level setting outside one, so enforce it after initialization too.
        driver.execute(null, "PRAGMA foreign_keys = ON", 0).value
        
        return driver
    }
}
