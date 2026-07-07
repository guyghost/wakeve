package com.guyghost.wakeve.auth

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SessionManagerErrorLogTest {
    @Test
    fun tokenRefreshFailureLogMessageDoesNotExposeThrowableDetails() {
        val message = tokenRefreshFailureLogMessage()

        assertTrue(message.contains("Token refresh failed"))
        assertFalse(message.contains("SECRET"))
        assertFalse(message.contains("token="))
        assertFalse(message.contains("internal.local"))
        assertFalse(message.contains("SQL"))
    }
}
