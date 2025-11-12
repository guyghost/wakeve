package com.guyghost.wakeve

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.guyghost.wakeve.database.WakevDb
import java.io.File

/**
 * JVM-specific database factory using the JDBC SQLite driver.
 */
class JvmDatabaseFactory(private val dbPath: String = "wakev.db") : DatabaseFactory {
    override fun createDriver(): SqlDriver {
        val dbFile = File(dbPath)
        val driver: SqlDriver = JdbcSqliteDriver(url = "jdbc:sqlite:$dbPath")
        
        // Initialize schema if database doesn't exist
        if (!dbFile.exists()) {
            driver.execute(null, "VACUUM", 0)
        }
        
        return driver
    }
}
