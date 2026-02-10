package com.guyghost.wakeve.analytics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable

/**
 * Queue for offline analytics events.
 * Events are stored locally and synced when online.
 *
 * Thread-safe implementation using coroutines Mutex.
 */
class AnalyticsQueue {

    @Serializable
    data class QueuedEvent(
        val id: String,
        val eventName: String,
        val properties: Map<String, String>,
        val timestamp: Long,
        val retryCount: Int = 0
    )

    private val _queue = MutableStateFlow<List<QueuedEvent>>(emptyList())
    val queue: StateFlow<List<QueuedEvent>> = _queue.asStateFlow()

    private val mutex = Mutex()
    private val maxRetries = 3

    /**
     * Add event to queue.
     *
     * @param event The analytics event to enqueue
     * @param properties Optional custom properties for the event
     */
    suspend fun enqueue(event: AnalyticsEvent, properties: Map<String, Any?>) {
        mutex.withLock {
            val queuedEvent = QueuedEvent(
                id = generateEventId(),
                eventName = event.eventName,
                properties = properties.mapValues { it.value?.toString() ?: "" },
                timestamp = currentTimeMillis()
            )
            _queue.value = _queue.value + queuedEvent
        }
    }

    /**
     * Get all pending events for sync.
     * Returns events with retry count less than maxRetries.
     */
    suspend fun getPendingEvents(): List<QueuedEvent> {
        return mutex.withLock {
            _queue.value.filter { it.retryCount < maxRetries }
        }
    }

    /**
     * Mark events as successfully synced and remove from queue.
     *
     * @param eventIds List of event IDs to remove
     */
    suspend fun markAsSynced(eventIds: List<String>) {
        mutex.withLock {
            _queue.value = _queue.value.filter { it.id !in eventIds }
        }
    }

    /**
     * Mark events as failed and increment retry count.
     * Events that exceed maxRetries are removed from queue.
     *
     * @param eventIds List of event IDs to mark as failed
     */
    suspend fun markAsFailed(eventIds: List<String>) {
        mutex.withLock {
            _queue.value = _queue.value.map { event ->
                if (event.id in eventIds) {
                    event.copy(retryCount = event.retryCount + 1)
                } else {
                    event
                }
            }.filter { it.retryCount < maxRetries }
        }
    }

    /**
     * Clear all queued events.
     */
    suspend fun clear() {
        mutex.withLock {
            _queue.value = emptyList()
        }
    }

    /**
     * Get current queue size.
     */
    val size: Int
        get() = _queue.value.size

    /**
     * Generate unique event ID.
     */
    private fun generateEventId(): String {
        return "event-${currentTimeMillis()}-${(0..9999).random()}"
    }

    /**
     * Get current timestamp in milliseconds.
     * Platform-specific implementation will be provided.
     */
    private fun currentTimeMillis(): Long {
        return kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
    }
}
