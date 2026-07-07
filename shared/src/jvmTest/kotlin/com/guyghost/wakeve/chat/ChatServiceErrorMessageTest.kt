package com.guyghost.wakeve.chat

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ChatServiceErrorMessageTest {

    @Test
    fun `websocket connection exception emits stable non-sensitive error`() = runTest {
        val secret = "token=secret-123"
        val service = ChatService(
            currentUserId = "user-1",
            currentUserName = "User One",
            webSocketClient = ThrowingWebSocketClient(secret)
        )
        service.setWebSocketUrlForTest("wss://example.invalid/chat")

        val errorEvent = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(1_000) {
                service.connectionEvents.filterIsInstance<ConnectionEvent.Error>().first()
            }
        }

        val connected = service.connectWebSocket("event-1")

        assertFalse(connected)
        val message = errorEvent.await().message
        assertEquals(chatConnectionFailureMessage(), message)
        assertFalse(message.contains(secret))
    }

    private fun ChatService.setWebSocketUrlForTest(url: String) {
        val field = ChatService::class.java.getDeclaredField("webSocketUrl")
        field.isAccessible = true
        field.set(this, url)
    }

    private class ThrowingWebSocketClient(private val failureMessage: String) : WebSocketClient {
        override val incomingMessages: Flow<String> = MutableSharedFlow()

        override suspend fun connect(url: String): Boolean {
            throw IllegalStateException(failureMessage)
        }

        override suspend fun send(message: String): Boolean = true

        override suspend fun close() = Unit

        override fun isConnected(): Boolean = false
    }
}
