package com.guyghost.wakeve

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class DataManagementScreenContractTest {
    @Test
    fun dataManagementMessagesUseStableSafeCopy() {
        val messages = listOf(
            accountDeletionFailureMessage(),
            guestDataDeletionSuccessMessage(),
            accountDeletionSuccessMessage()
        )

        assertEquals(messages.size, messages.distinct().size)
        messages.forEach { message ->
            assertFalse(message.isBlank())
        }
    }
}
