package com.guyghost.wakeve.chat

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Manages WebSocket reconnection logic with exponential backoff.
 * 
 * This class handles automatic reconnection attempts when the WebSocket connection
 * is lost, following an exponential backoff strategy:
 * - Immediate first attempt
 * - Then: 1s, 2s, 4s, 8s, 16s, 32s (maximum)
 * - Maximum of 10 retry attempts before abandoning
 * 
 * @property maxRetryAttempts Maximum number of reconnection attempts before giving up (default: 10)
 * @property maxDelayMs Maximum delay between retries in milliseconds (default: 32,000ms = 32s)
 * @property initialDelayMs Initial delay before first reconnection attempt (default: 1,000ms = 1s)
 */
class ReconnectionManager(
    private val chatService: ChatService,
    private val scope: CoroutineScope
) {
    /**
     * Maximum number of reconnection attempts before abandoning.
     */
    val maxRetryAttempts: Int = 10

    /**
     * Maximum delay between retries in milliseconds.
     * This caps the exponential backoff at 32 seconds.
     */
    val maxDelayMs: Long = 32_000L

    /**
     * Initial delay before first reconnection attempt in milliseconds.
     */
    val initialDelayMs: Long = 1_000L

    private var connectionState: ConnectionState = ConnectionState.DISCONNECTED
    private var retryCount = 0
    private var currentDelay: Long = 0L
    private var reconnectionJob: Job? = null

    /**
     * Represents the current state of the WebSocket connection.
     */
    sealed class ConnectionState {
        /**
         * Connection is active and stable.
         */
        data object CONNECTED : ConnectionState()

        /**
         * No active connection exists.
         */
        data object DISCONNECTED : ConnectionState()

        /**
         * Initial connection attempt in progress.
         */
        data object CONNECTING : ConnectionState()

        /**
         * Automatic reconnection attempt in progress.
         */
        data object RECONNECTING : ConnectionState()

        /**
         * Maximum retry attempts exceeded, connection abandoned.
         */
        data object ABANDONED : ConnectionState()
    }

    /**
     * Initiates a connection attempt to the WebSocket server.
     * Resets retry count and delay to initial values before attempting connection.
     * 
     * @param eventId The event ID to connect to
     * @return true if connection successful, false if all attempts exhausted
     */
    suspend fun connect(eventId: String): Boolean {
        retryCount = 0
        currentDelay = initialDelayMs

        return tryConnect(eventId)
    }

    /**
     * Attempts to connect to the WebSocket server with exponential backoff.
     * 
     * @param eventId The event ID to connect to
     * @return true if connection successful, false if all attempts exhausted
     */
    private suspend fun tryConnect(eventId: String): Boolean {
        while (retryCount < maxRetryAttempts) {
            try {
                connectionState = ConnectionState.CONNECTING
                val success = chatService.connectWebSocket(eventId)

                // Wait briefly to verify connection
                delay(1_000L)

                if (chatService.isConnected(eventId)) {
                    connectionState = ConnectionState.CONNECTED
                    retryCount = 0
                    currentDelay = initialDelayMs
                    return true
                }
            } catch (e: Exception) {
                connectionState = ConnectionState.DISCONNECTED
                logError("Connection failed for event $eventId", e)

                retryCount++
                currentDelay = (currentDelay * 2).coerceAtMost(maxDelayMs)

                logInfo("Retry $retryCount/$maxRetryAttempts in ${currentDelay}ms")

                // Exponential backoff wait
                delay(currentDelay)

                connectionState = ConnectionState.RECONNECTING
            }
        }

        // Max retries exceeded
        connectionState = ConnectionState.ABANDONED
        logError("Max retry attempts ($maxRetryAttempts) exceeded for event $eventId")

        return false
    }

    /**
     * Starts automatic reconnection in the background.
     * Cancels any existing reconnection job before starting a new one.
     * 
     * @param eventId The event ID to reconnect to
     */
    fun startAutoReconnect(eventId: String) {
        // Cancel any existing reconnection job
        reconnectionJob?.cancel()

        reconnectionJob = scope.launch {
            connect(eventId)
        }
    }

    /**
     * Stops automatic reconnection attempts.
     * Resets retry count, delay, and connection state to initial values.
     */
    fun stopAutoReconnect() {
        reconnectionJob?.cancel()
        reconnectionJob = null
        retryCount = 0
        currentDelay = initialDelayMs
        connectionState = ConnectionState.DISCONNECTED
    }

    /**
     * Returns the current connection state.
     * 
     * @return The current [ConnectionState]
     */
    fun getConnectionState(): ConnectionState = connectionState

    /**
     * Returns the current number of retry attempts.
     * 
     * @return The number of retries attempted so far
     */
    fun getRetryCount(): Int = retryCount

    /**
     * Returns the current delay before the next reconnection attempt.
     * 
     * @return The current delay in milliseconds
     */
    fun getCurrentDelay(): Long = currentDelay

    /**
     * Resets the reconnection manager to its initial state.
     * This clears retry count, delay, and sets state to DISCONNECTED.
     */
    fun reset() {
        retryCount = 0
        currentDelay = initialDelayMs
        connectionState = ConnectionState.DISCONNECTED
    }

    /**
     * Checks if the reconnection manager is currently attempting to reconnect.
     * 
     * @return true if a reconnection job is active, false otherwise
     */
    fun isReconnecting(): Boolean = reconnectionJob?.isActive == true

    /**
     * Logs an error message for debugging purposes.
     * In production, this would integrate with a proper logging framework.
     */
    private fun logError(message: String, throwable: Throwable? = null) {
        // Using println for now - in production, use proper logging
        println("[ReconnectionManager] ERROR: $message")
        throwable?.let { println("[ReconnectionManager] Exception: ${it.message}") }
    }

    /**
     * Logs an informational message for debugging purposes.
     * In production, this would integrate with a proper logging framework.
     */
    private fun logInfo(message: String) {
        // Using println for now - in production, use proper logging
        println("[ReconnectionManager] INFO: $message")
    }
}
