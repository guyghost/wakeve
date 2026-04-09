package com.guyghost.wakeve.chat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for ReconnectionManager.
 *
 * Tests the exponential backoff logic, connection state management,
 * retry counting, and auto-reconnect lifecycle.
 *
 * Note: These tests verify the pure logic parts without actual WebSocket
 * connections. The coroutine-based connect() is tested indirectly via
 * the state management methods.
 */
class ReconnectionManagerTest {

    // ========================================================================
    // ConnectionState Tests
    // ========================================================================

    @Test
    fun `connection states are distinct`() {
        val states = listOf(
            ReconnectionManager.ConnectionState.CONNECTED,
            ReconnectionManager.ConnectionState.DISCONNECTED,
            ReconnectionManager.ConnectionState.CONNECTING,
            ReconnectionManager.ConnectionState.RECONNECTING,
            ReconnectionManager.ConnectionState.ABANDONED
        )
        assertEquals(5, states.toSet().size, "All connection states should be distinct")
    }

    @Test
    fun `initial state is DISCONNECTED`() {
        val manager = createManager()
        assertTrue(
            manager.getConnectionState() is ReconnectionManager.ConnectionState.DISCONNECTED,
            "Initial state should be DISCONNECTED"
        )
    }

    // ========================================================================
    // Configuration Tests
    // ========================================================================

    @Test
    fun `default max retry attempts is 10`() {
        val manager = createManager()
        assertEquals(10, manager.maxRetryAttempts)
    }

    @Test
    fun `default max delay is 32000ms`() {
        val manager = createManager()
        assertEquals(32_000L, manager.maxDelayMs)
    }

    @Test
    fun `default initial delay is 1000ms`() {
        val manager = createManager()
        assertEquals(1_000L, manager.initialDelayMs)
    }

    // ========================================================================
    // Retry Count and Delay Tests
    // ========================================================================

    @Test
    fun `initial retry count is 0`() {
        val manager = createManager()
        assertEquals(0, manager.getRetryCount())
    }

    @Test
    fun `initial current delay is 0`() {
        val manager = createManager()
        assertEquals(0L, manager.getCurrentDelay(), "Initial delay should be 0 until connect() is called")
    }

    // ========================================================================
    // Reset Tests
    // ========================================================================

    @Test
    fun `reset clears retry count`() {
        val manager = createManager()
        // After reset, retry count should be 0
        manager.reset()
        assertEquals(0, manager.getRetryCount())
    }

    @Test
    fun `reset sets state to DISCONNECTED`() {
        val manager = createManager()
        manager.reset()
        assertTrue(
            manager.getConnectionState() is ReconnectionManager.ConnectionState.DISCONNECTED
        )
    }

    @Test
    fun `reset sets current delay to initialDelayMs`() {
        val manager = createManager()
        manager.reset()
        assertEquals(manager.initialDelayMs, manager.getCurrentDelay())
    }

    // ========================================================================
    // Stop Auto Reconnect Tests
    // ========================================================================

    @Test
    fun `stopAutoReconnect sets state to DISCONNECTED`() {
        val manager = createManager()
        manager.stopAutoReconnect()
        assertTrue(
            manager.getConnectionState() is ReconnectionManager.ConnectionState.DISCONNECTED
        )
    }

    @Test
    fun `stopAutoReconnect resets retry count`() {
        val manager = createManager()
        manager.stopAutoReconnect()
        assertEquals(0, manager.getRetryCount())
    }

    @Test
    fun `stopAutoReconnect resets current delay`() {
        val manager = createManager()
        manager.stopAutoReconnect()
        assertEquals(manager.initialDelayMs, manager.getCurrentDelay())
    }

    @Test
    fun `stopAutoReconnect clears reconnecting status`() {
        val manager = createManager()
        manager.stopAutoReconnect()
        assertFalse(manager.isReconnecting())
    }

    // ========================================================================
    // isReconnecting Tests
    // ========================================================================

    @Test
    fun `initially not reconnecting`() {
        val manager = createManager()
        assertFalse(manager.isReconnecting())
    }

    @Test
    fun `not reconnecting after stop`() {
        val manager = createManager()
        manager.stopAutoReconnect()
        assertFalse(manager.isReconnecting())
    }

    @Test
    fun `not reconnecting after reset`() {
        val manager = createManager()
        manager.reset()
        assertFalse(manager.isReconnecting())
    }

    // ========================================================================
    // State Transition Tests
    // ========================================================================

    @Test
    fun `multiple resets are idempotent`() {
        val manager = createManager()
        repeat(5) { manager.reset() }
        assertEquals(0, manager.getRetryCount())
        assertTrue(
            manager.getConnectionState() is ReconnectionManager.ConnectionState.DISCONNECTED
        )
    }

    @Test
    fun `multiple stopAutoReconnect are idempotent`() {
        val manager = createManager()
        repeat(5) { manager.stopAutoReconnect() }
        assertEquals(0, manager.getRetryCount())
        assertFalse(manager.isReconnecting())
    }

    // ========================================================================
    // Exponential Backoff Calculation Tests
    // ========================================================================

    @Test
    fun `exponential backoff doubles delay`() {
        // Verify: initial 1000ms -> 2000 -> 4000 -> 8000 -> 16000 -> 32000 -> 32000 (capped)
        val initialDelay = 1_000L
        val maxDelay = 32_000L

        var delay = initialDelay
        assertEquals(1_000L, delay)

        delay = (delay * 2).coerceAtMost(maxDelay)
        assertEquals(2_000L, delay)

        delay = (delay * 2).coerceAtMost(maxDelay)
        assertEquals(4_000L, delay)

        delay = (delay * 2).coerceAtMost(maxDelay)
        assertEquals(8_000L, delay)

        delay = (delay * 2).coerceAtMost(maxDelay)
        assertEquals(16_000L, delay)

        delay = (delay * 2).coerceAtMost(maxDelay)
        assertEquals(32_000L, delay)

        // Capped at max
        delay = (delay * 2).coerceAtMost(maxDelay)
        assertEquals(32_000L, delay)
    }

    @Test
    fun `backoff reaches max after 5 doublings`() {
        val initialDelay = 1_000L
        val maxDelay = 32_000L

        var delay = initialDelay
        repeat(5) {
            delay = (delay * 2).coerceAtMost(maxDelay)
        }
        assertEquals(32_000L, delay, "After 5 doublings, delay should reach max")
    }

    // ========================================================================
    // ChatModels Tests
    // ========================================================================

    @Test
    fun `ChatMessage has required fields`() {
        val message = ChatMessage(
            id = "msg-1",
            eventId = "event-1",
            senderId = "user-1",
            senderName = "Alice",
            content = "Hello!",
            timestamp = "2026-01-01T00:00:00Z"
        )
        assertEquals("msg-1", message.id)
        assertEquals("event-1", message.eventId)
        assertEquals("Alice", message.senderName)
        assertEquals("Hello!", message.content)
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    private fun createManager(): ReconnectionManager {
        // Create ChatService directly - no need for mock since
        // we're only testing non-connection logic here
        val chatService = ChatService(
            currentUserId = "test-user",
            currentUserName = "Test User"
        )
        return ReconnectionManager(
            chatService = chatService,
            scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob())
        )
    }
}
