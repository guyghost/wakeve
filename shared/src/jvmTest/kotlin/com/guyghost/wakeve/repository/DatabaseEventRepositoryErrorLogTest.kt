package com.guyghost.wakeve.repository

import kotlin.test.Test
import kotlin.test.assertFalse

class DatabaseEventRepositoryErrorLogTest {
    @Test
    fun databaseEventRepositoryFailureLogsDoNotExposeExceptionDetails() {
        val messages = listOf(
            databaseEventRepositoryTimeSlotSyncFailureLogMessage(),
            databaseEventRepositoryPaginatedEventsFailureLogMessage()
        )

        messages.forEach { message ->
            assertFalse(message.contains("SECRET", ignoreCase = true))
            assertFalse(message.contains("SQL", ignoreCase = true))
            assertFalse(message.contains("token=", ignoreCase = true))
            assertFalse(message.contains("internal.local", ignoreCase = true))
        }
    }
}
