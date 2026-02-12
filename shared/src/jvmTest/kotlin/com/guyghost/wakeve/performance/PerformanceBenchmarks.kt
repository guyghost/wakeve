package com.guyghost.wakeve.performance

import com.guyghost.wakeve.EventRepository
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.Vote
import com.guyghost.wakeve.repository.OrderBy
import com.guyghost.wakeve.test.createTestEvent
import com.guyghost.wakeve.test.createTestTimeSlot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Simple performance benchmarks for Wakeve core functionality.
 * 
 * These benchmarks measure critical performance metrics to ensure
 * app meets performance targets.
 */
class SimplePerformanceBenchmarks {

    private val repository = EventRepository()

    @Test
    fun benchmarkAppStartup() {
        val iterations = 10
        val times = mutableListOf<Long>()
        
        repeat(iterations) {
            val time = measureTimeMillis {
                // Simulate app startup - repository initialization
                val repo = EventRepository()
                // Simulate initial data loading
                repo.getAllEvents()
            }
            times.add(time)
        }
        
        val averageTime = times.average()
        val maxTime = times.maxOrNull() ?: 0L
        
        println("=== App Startup Benchmark ===")
        println("Average startup time: ${averageTime}ms")
        println("Max startup time: ${maxTime}ms")
        println("Target: < 2000ms")
        
        // Target: < 2 seconds
        assertTrue(
            averageTime < 2000,
            "Average startup time ${averageTime}ms exceeds target of 2000ms"
        )
    }

    @Test
    fun benchmarkEventCreation() = runBlocking {
        val iterations = 20
        val times = mutableListOf<Long>()
        
        repeat(iterations) { index ->
            val event = createTestEvent(
                id = "creation-benchmark-$index",
                title = "Benchmark Event $index",
                description = "Event created for performance benchmarking",
                status = EventStatus.DRAFT,
                eventType = EventType.BIRTHDAY,
                proposedSlots = listOf(
                    createTestTimeSlot(
                        id = "slot-$index-1",
                        start = "2025-12-25T10:00:00Z",
                        end = "2025-12-25T12:00:00Z"
                    )
                )
            )
            
            val time = measureTimeMillis {
                val result = repository.createEvent(event)
                if (result.isFailure) {
                    throw Exception("Failed to create event: ${result.exceptionOrNull()?.message}")
                }
            }
            times.add(time)
        }
        
        val averageTime = times.average()
        val maxTime = times.maxOrNull() ?: 0L
        
        println("=== Event Creation Benchmark ===")
        println("Iterations: $iterations")
        println("Average creation time: ${averageTime}ms")
        println("Max creation time: ${maxTime}ms")
        println("Target: < 500ms")
        
        // Target: < 500ms
        assertTrue(
            averageTime < 500,
            "Average creation time ${averageTime}ms exceeds target of 500ms"
        )
    }

    @Test
    fun benchmarkVoteSubmission() = runBlocking {
        val eventId = "vote-benchmark-event"
        val participantIds = (1..10).map { "participant-$it" }
        val slotIds = (1..5).map { "slot-$it" }
        
        // Create test event with polling status
        val event = createTestEvent(
            id = eventId,
            title = "Vote Benchmark Event",
            status = EventStatus.POLLING,
            participants = participantIds,
            proposedSlots = slotIds.map { slotId ->
                createTestTimeSlot(
                    id = slotId,
                    start = "2025-12-25T10:00:00Z",
                    end = "2025-12-25T12:00:00Z"
                )
            }
        )
        repository.createEvent(event)
        
        val totalVotes = participantIds.size * slotIds.size // 50 votes total
        val times = mutableListOf<Long>()
        
        participantIds.forEach { participantId ->
            slotIds.forEach { slotId ->
                val vote = when ((participantId.last().code + slotId.last().code) % 3) {
                    0 -> Vote.YES
                    1 -> Vote.MAYBE
                    else -> Vote.NO
                }
                
                val time = measureTimeMillis {
                    val result = repository.addVote(eventId, participantId, slotId, vote)
                    if (result.isFailure) {
                        throw Exception("Failed to add vote: ${result.exceptionOrNull()?.message}")
                    }
                }
                times.add(time)
            }
        }
        
        val averageTime = times.average()
        val maxTime = times.maxOrNull() ?: 0L
        val votesPerSecond = 1000.0 / averageTime
        
        println("=== Vote Submission Benchmark ===")
        println("Total votes: $totalVotes")
        println("Average vote time: ${averageTime}ms")
        println("Max vote time: ${maxTime}ms")
        println("Votes per second: ${"%.2f".format(votesPerSecond)}")
        println("Target: < 200ms per vote")
        
        // Target: < 200ms per vote
        assertTrue(
            averageTime < 200,
            "Average vote time ${averageTime}ms exceeds target of 200ms"
        )
    }

    @Test
    fun benchmarkEventListLoad() = runBlocking {
        val eventCount = 50
        val repository = EventRepository()
        
        // Pre-populate with test events
        repeat(eventCount) { index ->
            val event = createTestEvent(
                id = "perf-event-$index",
                title = "Performance Test Event $index",
                description = "Test event for performance benchmarking",
                status = EventStatus.DRAFT,
                eventType = EventType.OTHER
            )
            repository.createEvent(event)
        }
        
        val iterations = 10
        val times = mutableListOf<Long>()
        
        repeat(iterations) {
            val time = measureTimeMillis {
                repository.getEventsPaginated(
                    page = 0,
                    pageSize = eventCount,
                    orderBy = OrderBy.CREATED_AT_DESC
                ).first()
            }
            times.add(time)
        }
        
        val averageTime = times.average()
        val maxTime = times.maxOrNull() ?: 0L
        
        println("=== Event List Load Benchmark ===")
        println("Event count: $eventCount")
        println("Average load time: ${averageTime}ms")
        println("Max load time: ${maxTime}ms")
        println("Target: < 100ms for $eventCount items")
        
        // Target: < 100ms for 50 items
        assertTrue(
            averageTime < 100,
            "Average load time ${averageTime}ms exceeds target of 100ms for $eventCount items"
        )
    }

    @Test
    fun benchmarkMemoryUsage() = runBlocking {
        // Force garbage collection before measurement
        System.gc()
        Thread.sleep(100)
        
        val runtime = Runtime.getRuntime()
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()
        
        // Simulate app usage with various operations
        repeat(100) { index ->
            val event = createTestEvent(
                id = "memory-test-$index",
                title = "Memory Test Event $index",
                description = "Event created for memory usage benchmarking with a longer description to test memory allocation patterns",
                status = EventStatus.DRAFT,
                participants = (1..5).map { "participant-$index-$it" }
            )
            repository.createEvent(event)
        }
        
        // Load all events
        repository.getAllEvents()
        repository.getEventsPaginated(0, 50, OrderBy.CREATED_AT_DESC).first()
        
        // Measure memory after operations
        System.gc()
        Thread.sleep(100)
        val finalMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryUsedMB = (finalMemory - initialMemory) / (1024 * 1024)
        val totalMemoryMB = finalMemory / (1024 * 1024)
        
        println("=== Memory Usage Benchmark ===")
        println("Events created: 100")
        println("Initial memory: ${initialMemory / (1024 * 1024)}MB")
        println("Final memory: ${finalMemory / (1024 * 1024)}MB")
        println("Memory used by operations: ${memoryUsedMB}MB")
        println("Total memory usage: ${totalMemoryMB}MB")
        println("Target: < 100MB idle")
        
        // Target: < 100MB total memory usage
        assertTrue(
            totalMemoryMB < 100,
            "Total memory usage ${totalMemoryMB}MB exceeds target of 100MB"
        )
    }
}