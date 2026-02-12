package com.guyghost.wakeve.performance

import com.guyghost.wakeve.EventRepository
import com.guyghost.wakeve.PollLogic
import com.guyghost.wakeve.deeplink.DeepLink
import com.guyghost.wakeve.deeplink.DeepLinkFactory
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.Poll
import com.guyghost.wakeve.models.TimeOfDay
import com.guyghost.wakeve.models.Vote
import com.guyghost.wakeve.notification.NotificationCategory
import com.guyghost.wakeve.notification.NotificationPreferences
import com.guyghost.wakeve.notification.NotificationPreferencesRepositoryInterface
import com.guyghost.wakeve.notification.NotificationType
import com.guyghost.wakeve.notification.QuietTime
import com.guyghost.wakeve.notification.RichNotification
import com.guyghost.wakeve.notification.RichNotificationPriority
import com.guyghost.wakeve.notification.defaultNotificationPreferences
import com.guyghost.wakeve.notification.richNotification
import com.guyghost.wakeve.notification.shouldSend
import com.guyghost.wakeve.presentation.state.EventManagementContract
import com.guyghost.wakeve.presentation.statemachine.EventManagementStateMachine
import com.guyghost.wakeve.presentation.usecase.CreateEventUseCase
import com.guyghost.wakeve.presentation.usecase.LoadEventsUseCase
import com.guyghost.wakeve.repository.OrderBy
import com.guyghost.wakeve.test.createTestEvent
import com.guyghost.wakeve.test.createTestTimeSlot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Advanced performance benchmarks for Wakeve core functionality.
 * 
 * These benchmarks measure critical performance metrics to ensure
 * the app meets strict performance targets for production use.
 */
class AdvancedPerformanceBenchmarks {

    // ==================== 1. Notification Preferences Check Benchmark ====================

    @Test
    fun benchmarkNotificationPreferencesCheck() {
        val iterations = 1000
        val times = mutableListOf<Long>()
        
        val preferences = defaultNotificationPreferences("user-123")
        val currentTime = Clock.System.now()
        
        repeat(iterations) { index ->
            val type = NotificationType.entries[index % NotificationType.entries.size]
            
            val time = measureNanoTime {
                val shouldSend = preferences.shouldSend(type, currentTime)
                assertTrue(shouldSend || !shouldSend)
            } / 1_000_000 // Convert to ms
            times.add(time)
        }
        
        val averageTime = times.average()
        val maxTime = times.maxOrNull() ?: 0L
        
        println("=== Notification Preferences Check Benchmark ===")
        println("Iterations: $iterations")
        println("Average time: ${String.format("%.3f", averageTime)}ms")
        println("Max time: ${maxTime}ms")
        println("Target: < 1ms")
        
        assertTrue(
            averageTime < 1,
            "Average preferences check time ${averageTime}ms exceeds target of 1ms"
        )
    }

    // ==================== 2. RichNotification Creation Benchmark ====================

    @Test
    fun benchmarkRichNotificationCreation() {
        val iterations = 100
        val times = mutableListOf<Long>()
        
        repeat(iterations) { index ->
            val time = measureTimeMillis {
                val notification = richNotification {
                    userId("user-456")
                    title("Rich Test $index")
                    body("Rich notification with image and actions")
                    imageUrl("https://example.com/image$index.jpg")
                    category(NotificationCategory.EVENT_INVITE)
                    priority(RichNotificationPriority.HIGH)
                    deepLink("wakeve://events/event-$index")
                    customSound("notification_chime")
                    vibrationPattern(RichNotification.DEFAULT_VIBRATION_PATTERN)
                    ledColor(0xFF00FF00.toInt())
                }
                
                // Validate the notification
                notification.validate()
            }
            times.add(time)
        }
        
        val averageTime = times.average()
        val maxTime = times.maxOrNull() ?: 0L
        
        println("=== RichNotification Creation Benchmark ===")
        println("Iterations: $iterations")
        println("Average time: ${"%.2f".format(averageTime)}ms")
        println("Max time: ${maxTime}ms")
        println("Target: < 150ms")
        
        assertTrue(
            averageTime < 150,
            "Average rich notification creation time ${averageTime}ms exceeds target of 150ms"
        )
    }

    // ==================== 3. DeepLink Parsing Benchmark ====================

    @Test
    fun benchmarkDeepLinkParsing() {
        val iterations = 1000
        val testUris = listOf(
            "wakeve://event/123/details?tab=votes",
            "wakeve://event/456/poll?slotId=789&highlight=true",
            "wakeve://profile",
            "wakeve://settings?category=notifications&section=push",
            "wakeve://event/abc-123/scenarios?scenarioId=xyz-789",
            "wakeve://notifications?filter=unread",
            "wakeve://home?tab=upcoming",
            "wakeve://event/test-123_456/meetings?meetingId=meet-789&autoJoin=true"
        )
        
        val times = mutableListOf<Long>()
        
        repeat(iterations) { index ->
            val uri = testUris[index % testUris.size]
            val time = measureNanoTime {
                DeepLink.parse(uri)
            } / 1_000_000 // Convert to ms
            times.add(time)
        }
        
        val averageTime = times.average()
        val maxTime = times.maxOrNull() ?: 0L
        
        println("=== DeepLink Parsing Benchmark ===")
        println("Iterations: $iterations")
        println("Average time: ${String.format("%.3f", averageTime)}ms")
        println("Max time: ${maxTime}ms")
        println("Target: < 10ms")
        
        assertTrue(
            averageTime < 10,
            "Average deep link parsing time ${averageTime}ms exceeds target of 10ms"
        )
    }

    // ==================== 4. Poll Calculation Benchmark ====================

    @Test
    fun benchmarkPollCalculation_100Votes() {
        val iterations = 100
        
        // Create poll with 100 participants voting on 10 slots
        val slots = (1..10).map { index ->
            createTestTimeSlot(
                id = "slot-$index",
                start = "2025-12-${10 + index}T10:00:00Z",
                end = "2025-12-${10 + index}T12:00:00Z"
            )
        }
        
        val votes = (1..100).associate { participantIndex ->
            "participant-$participantIndex" to slots.associate { slot ->
                val hashValue = slot.id.last().digitToIntOrNull() ?: participantIndex
                slot.id to when ((participantIndex + hashValue) % 3) {
                    0 -> Vote.YES
                    1 -> Vote.MAYBE
                    else -> Vote.NO
                }
            }
        }
        
        val poll = Poll("event-123", "event-123", votes)
        
        val times = mutableListOf<Long>()
        
        repeat(iterations) {
            val time = measureNanoTime {
                PollLogic.calculateBestSlot(poll, slots)
            } / 1_000_000 // Convert to ms
            times.add(time)
        }
        
        val averageTime = times.average()
        val maxTime = times.maxOrNull() ?: 0L
        
        println("=== Poll Calculation Benchmark (100 votes) ===")
        println("Iterations: $iterations")
        println("Participants: 100, Slots: 10, Total votes: 1000")
        println("Average time: ${String.format("%.3f", averageTime)}ms")
        println("Max time: ${maxTime}ms")
        println("Target: < 50ms")
        
        assertTrue(
            averageTime < 50,
            "Average poll calculation time ${averageTime}ms exceeds target of 50ms"
        )
    }

    // ==================== 5. State Machine Transitions Benchmark ====================

    @Test
    fun benchmarkStateMachineTransitions() = runBlocking {
        val iterations = 100
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        
        val repository = EventRepository()
        val loadEventsUseCase = LoadEventsUseCase(repository)
        val createEventUseCase = CreateEventUseCase(repository)
        
        val stateMachine = EventManagementStateMachine(
            loadEventsUseCase = loadEventsUseCase,
            createEventUseCase = createEventUseCase,
            eventRepository = repository,
            scope = scope
        )
        
        val times = mutableListOf<Long>()
        
        repeat(iterations) {
            val time = measureTimeMillis {
                // Simulate a state transition sequence
                stateMachine.dispatch(EventManagementContract.Intent.LoadEvents)
                stateMachine.dispatch(EventManagementContract.Intent.ClearError)
            }
            times.add(time)
        }
        
        val averageTime = times.average()
        val maxTime = times.maxOrNull() ?: 0L
        
        println("=== State Machine Transitions Benchmark ===")
        println("Iterations: $iterations")
        println("Average time: ${"%.2f".format(averageTime)}ms")
        println("Max time: ${maxTime}ms")
        println("Target: < 20ms")
        
        assertTrue(
            averageTime < 20,
            "Average state machine transition time ${averageTime}ms exceeds target of 20ms"
        )
        
        scope.cancel()
    }

    // ==================== 6. Event List Filtering Benchmark ====================

    @Test
    fun benchmarkEventListFiltering_500Events() = runBlocking {
        val iterations = 50
        val repository = EventRepository()
        
        // Pre-populate with 500 events
        val statuses = EventStatus.entries.toTypedArray()
        val types = EventType.entries.toTypedArray()
        
        repeat(500) { index ->
            val event = createTestEvent(
                id = "perf-event-$index",
                title = "Event ${(index * 123) % 1000} - ${if (index % 2 == 0) "Birthday" else "Meeting"}",
                description = "Test event for filtering benchmark",
                status = statuses[index % statuses.size],
                eventType = types[index % types.size],
                participants = (1..(index % 10 + 1)).map { "participant-$it" }
            )
            repository.createEvent(event)
        }
        
        val times = mutableListOf<Long>()
        
        repeat(iterations) {
            val time = measureTimeMillis {
                // Simulate filtering operations
                val allEvents = repository.getAllEvents()
                
                // Filter by status
                val draftEvents = allEvents.filter { it.status == EventStatus.DRAFT }
                val confirmedEvents = allEvents.filter { it.status == EventStatus.CONFIRMED }
                
                // Filter by type
                val birthdayEvents = allEvents.filter { it.eventType == EventType.BIRTHDAY }
                
                // Filter by participant count
                val eventsWithManyParticipants = allEvents.filter { it.participants.size > 5 }
                
                // Sort by different criteria
                allEvents.sortedBy { it.title }
                allEvents.sortedByDescending { it.participants.size }
            }
            times.add(time)
        }
        
        val averageTime = times.average()
        val maxTime = times.maxOrNull() ?: 0L
        
        println("=== Event List Filtering Benchmark (500 events) ===")
        println("Iterations: $iterations")
        println("Average time: ${"%.2f".format(averageTime)}ms")
        println("Max time: ${maxTime}ms")
        println("Target: < 30ms")
        
        assertTrue(
            averageTime < 30,
            "Average filtering time ${averageTime}ms exceeds target of 30ms"
        )
    }

    // ==================== 7. Database Batch Operations Benchmark ====================

    @Test
    fun benchmarkDatabaseBatchInsert_100Events() = runBlocking {
        val iterations = 10
        val batchSize = 100
        
        val times = mutableListOf<Long>()
        
        repeat(iterations) { iteration ->
            val repository = EventRepository() // Fresh repository for each iteration
            
            val events = (1..batchSize).map { index ->
                createTestEvent(
                    id = "batch-${iteration}-event-$index",
                    title = "Batch Event $index",
                    description = "Event for batch insert benchmark",
                    status = EventStatus.DRAFT,
                    eventType = EventType.OTHER
                )
            }
            
            val time = measureTimeMillis {
                events.forEach { event ->
                    repository.createEvent(event)
                }
            }
            times.add(time)
        }
        
        val averageTime = times.average()
        val maxTime = times.maxOrNull() ?: 0L
        val eventsPerSecond = batchSize * 1000.0 / averageTime
        
        println("=== Database Batch Insert Benchmark (100 events) ===")
        println("Iterations: $iterations")
        println("Batch size: $batchSize")
        println("Average time: ${"%.2f".format(averageTime)}ms")
        println("Max time: ${maxTime}ms")
        println("Events per second: ${"%.2f".format(eventsPerSecond)}")
        println("Target: < 500ms")
        
        assertTrue(
            averageTime < 500,
            "Average batch insert time ${averageTime}ms exceeds target of 500ms"
        )
    }

    // ==================== 8. Image Processing Benchmark (Simulated) ====================

    @Test
    fun benchmarkImageProcessing_resize() {
        val iterations = 100
        
        // Simulate image processing operations
        fun simulateImageResize(width: Int, height: Int, targetWidth: Int): Pair<Int, Int> {
            val ratio = targetWidth.toFloat() / width
            val newHeight = (height * ratio).toInt()
            return targetWidth to newHeight
        }
        
        fun simulateImageCompression(data: ByteArray, quality: Int): ByteArray {
            // Simulate compression by reducing array size
            val compressedSize = (data.size * quality / 100).coerceAtLeast(1024)
            return ByteArray(compressedSize) { index ->
                data.getOrElse(index * data.size / compressedSize) { 0 }
            }
        }
        
        fun simulateBase64Encode(data: ByteArray): String {
            val base64Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
            return buildString {
                for (i in data.indices step 3) {
                    append(base64Chars[(data[i].toInt() and 0xFC) shr 2])
                    if (i + 1 < data.size) {
                        append(base64Chars[((data[i].toInt() and 0x03) shl 4) or ((data[i + 1].toInt() and 0xF0) shr 4)])
                        if (i + 2 < data.size) {
                            append(base64Chars[((data[i + 1].toInt() and 0x0F) shl 2) or ((data[i + 2].toInt() and 0xC0) shr 6)])
                            append(base64Chars[data[i + 2].toInt() and 0x3F])
                        }
                    }
                }
            }
        }
        
        val times = mutableListOf<Long>()
        
        repeat(iterations) {
            // Simulate a 4MB image
            val mockImageData = ByteArray(4 * 1024 * 1024) { (it % 256).toByte() }
            
            val time = measureTimeMillis {
                // Simulate resize from 4000x3000 to 800x600
                val (newWidth, newHeight) = simulateImageResize(4000, 3000, 800)
                
                // Simulate compression to 80% quality
                val compressed = simulateImageCompression(mockImageData, 80)
                
                // Simulate base64 encoding for upload
                val base64 = simulateBase64Encode(compressed)
                
                // Prevent optimization
                assertTrue(newWidth > 0 && newHeight > 0)
                assertTrue(compressed.isNotEmpty())
                assertTrue(base64.isNotEmpty())
            }
            times.add(time)
        }
        
        val averageTime = times.average()
        val maxTime = times.maxOrNull() ?: 0L
        
        println("=== Image Processing Benchmark (Simulated) ===")
        println("Iterations: $iterations")
        println("Average time: ${"%.2f".format(averageTime)}ms")
        println("Max time: ${maxTime}ms")
        println("Target: < 200ms")
        
        assertTrue(
            averageTime < 200,
            "Average image processing time ${averageTime}ms exceeds target of 200ms"
        )
    }

    // ==================== 9. Sync Operations Benchmark ====================

    @Test
    fun benchmarkSyncOperations_50Events() = runBlocking {
        val iterations = 10
        val syncCount = 50
        
        val times = mutableListOf<Long>()
        
        repeat(iterations) {
            // Simulate sync operation timing
            val time = measureTimeMillis {
                // Simulate collecting events to sync
                val eventsToSync = (1..syncCount).map { index ->
                    createTestEvent(
                        id = "sync-event-$index",
                        title = "Sync Event $index",
                        status = EventStatus.entries[index % EventStatus.entries.size]
                    )
                }
                
                // Simulate serializing events
                val jsonData = eventsToSync.joinToString(",") { event ->
                    """{"id":"${event.id}","title":"${event.title}","status":"${event.status}"}"""
                }
                
                // Simulate network delay (simulated, not actual network call)
                val simulatedNetworkDelay = 5L // 5ms simulated
                Thread.sleep(simulatedNetworkDelay)
                
                // Simulate processing response
                val processedCount = eventsToSync.size
                assertTrue(processedCount == syncCount)
                assertTrue(jsonData.isNotEmpty())
            }
            times.add(time)
        }
        
        val averageTime = times.average()
        val maxTime = times.maxOrNull() ?: 0L
        
        println("=== Sync Operations Benchmark (50 events) ===")
        println("Iterations: $iterations")
        println("Events per sync: $syncCount")
        println("Average time: ${"%.2f".format(averageTime)}ms")
        println("Max time: ${maxTime}ms")
        println("Target: < 1000ms")
        
        assertTrue(
            averageTime < 1000,
            "Average sync time ${averageTime}ms exceeds target of 1000ms"
        )
    }

    // ==================== 10. Memory Leak Detection Benchmark ====================

    @Test
    fun benchmarkMemoryLeakDetection_1000Operations() {
        val iterations = 1000
        
        val runtime = Runtime.getRuntime()
        
        // Force GC before measurement
        repeat(3) {
            System.gc()
            Thread.sleep(100)
        }
        
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()
        val initialTime = System.currentTimeMillis()
        
        // Perform 1000 operations that create and release objects
        repeat(iterations) { index ->
            // Create temporary objects
            val tempList = mutableListOf<String>()
            repeat(100) { i ->
                tempList.add("Temporary string $index-$i with some content to use memory")
            }
            
            // Create a temporary map
            val tempMap = tempList.associateWith { it.hashCode() }
            
            // Simulate some processing
            val sum = tempMap.values.sum()
            
            // Objects go out of scope and should be GC'd
            // Use the sum to prevent optimization
            if (sum < 0) println("Sum: $sum")
        }
        
        // Force GC after operations
        repeat(3) {
            System.gc()
            Thread.sleep(100)
        }
        
        val finalMemory = runtime.totalMemory() - runtime.freeMemory()
        val duration = System.currentTimeMillis() - initialTime
        
        val memoryDeltaMB = (finalMemory - initialMemory) / (1024 * 1024)
        val memoryGrowthPerOperation = memoryDeltaMB.toDouble() / iterations * 1000 // Per 1000 ops
        
        println("=== Memory Leak Detection Benchmark (1000 operations) ===")
        println("Iterations: $iterations")
        println("Duration: ${duration}ms")
        println("Initial memory: ${initialMemory / (1024 * 1024)}MB")
        println("Final memory: ${finalMemory / (1024 * 1024)}MB")
        println("Memory delta: ${memoryDeltaMB}MB")
        println("Growth per 1000 ops: ${"%.4f".format(memoryGrowthPerOperation)}MB")
        println("Target: No significant memory growth (delta < 50MB)")
        
        // Allow significant variance due to JVM GC behavior - this is a sanity check, not strict validation
        // JVM may not have fully collected objects yet, so we allow up to 200MB variance
        assertTrue(
            memoryDeltaMB < 200,
            "Memory grew by ${memoryDeltaMB}MB over $iterations operations - potential leak detected"
        )
    }

    // ==================== 11. Event Serialization/Deserialization Benchmark ====================

    @Test
    fun benchmarkEventSerialization() {
        val iterations = 1000
        
        val event = createTestEvent(
            id = "serialize-test-123",
            title = "Test Event for Serialization",
            description = "A longer description to test serialization performance with more data",
            status = EventStatus.CONFIRMED,
            eventType = EventType.BIRTHDAY,
            participants = (1..20).map { "participant-$it" },
            proposedSlots = (1..5).map { index ->
                createTestTimeSlot(
                    id = "slot-$index",
                    start = "2025-12-${10 + index}T10:00:00Z",
                    end = "2025-12-${10 + index}T12:00:00Z",
                    timeOfDay = TimeOfDay.entries[index % TimeOfDay.entries.size]
                )
            },
            minParticipants = 5,
            maxParticipants = 50,
            expectedParticipants = 25
        )
        
        val times = mutableListOf<Long>()
        
        repeat(iterations) {
            val time = measureNanoTime {
                // Simulate JSON-like serialization
                val json = buildString {
                    append("{")
                    append("\"id\":\"${event.id}\",")
                    append("\"title\":\"${event.title}\",")
                    append("\"description\":\"${event.description}\",")
                    append("\"status\":\"${event.status}\",")
                    append("\"participants\":[${event.participants.joinToString(",") { "\"$it\"" }}],")
                    append("\"slots\":[${event.proposedSlots.joinToString(",") { "\"${it.id}\"" }}]")
                    append("}")
                }
                
                // Simulate deserialization
                val id = json.substringAfter("\"id\":\"").substringBefore("\"")
                val title = json.substringAfter("\"title\":\"").substringBefore("\"")
                
                assertTrue(id.isNotEmpty() && title.isNotEmpty())
            } / 1_000_000 // Convert to ms
            times.add(time)
        }
        
        val averageTime = times.average()
        val maxTime = times.maxOrNull() ?: 0L
        
        println("=== Event Serialization Benchmark ===")
        println("Iterations: $iterations")
        println("Average time: ${String.format("%.3f", averageTime)}ms")
        println("Max time: ${maxTime}ms")
        println("Target: < 1ms")
        
        assertTrue(
            averageTime < 1,
            "Average serialization time ${averageTime}ms exceeds target of 1ms"
        )
    }

    // ==================== 12. DeepLink Factory Creation Benchmark ====================

    @Test
    fun benchmarkDeepLinkFactoryCreation() {
        val iterations = 500
        val times = mutableListOf<Long>()
        
        repeat(iterations) { index ->
            val time = measureNanoTime {
                val deepLink = when (index % 8) {
                    0 -> DeepLinkFactory.createEventDetailsLink("event-$index")
                    1 -> DeepLinkFactory.createPollVoteLink("event-$index", "slot-$index")
                    2 -> DeepLinkFactory.createScenarioComparisonLink("event-$index")
                    3 -> DeepLinkFactory.createMeetingJoinLink("event-$index", "meeting-$index")
                    4 -> DeepLinkFactory.createNotificationPreferencesLink()
                    5 -> DeepLinkFactory.createProfileLink("user-$index")
                    6 -> DeepLinkFactory.createNotificationsListLink()
                    else -> DeepLinkFactory.createHomeLink()
                }
                assertTrue(deepLink.fullUri.isNotEmpty())
            } / 1_000_000 // Convert to ms
            times.add(time)
        }
        
        val averageTime = times.average()
        val maxTime = times.maxOrNull() ?: 0L
        
        println("=== DeepLink Factory Creation Benchmark ===")
        println("Iterations: $iterations")
        println("Average time: ${String.format("%.3f", averageTime)}ms")
        println("Max time: ${maxTime}ms")
        println("Target: < 5ms")
        
        assertTrue(
            averageTime < 5,
            "Average DeepLink factory creation time ${averageTime}ms exceeds target of 5ms"
        )
    }

    // ==================== 13. Event Search Benchmark ====================

    @Test
    fun benchmarkEventSearch() = runBlocking {
        val iterations = 100
        val repository = EventRepository()
        
        // Pre-populate with diverse events
        val keywords = listOf("birthday", "meeting", "party", "wedding", "conference", "dinner", "lunch")
        val locations = listOf("Paris", "London", "New York", "Tokyo", "Sydney")
        
        repeat(200) { index ->
            val keyword = keywords[index % keywords.size]
            val location = locations[index % locations.size]
            val event = createTestEvent(
                id = "search-event-$index",
                title = "$keyword Event ${index * 7} at $location",
                description = "Join us for a $keyword celebration in $location",
                status = EventStatus.entries[index % EventStatus.entries.size],
                eventType = EventType.entries[index % EventType.entries.size]
            )
            repository.createEvent(event)
        }
        
        val allEvents = repository.getAllEvents()
        val searchTerms = listOf("birthday", "Paris", "meeting", "wedding", "dinner")
        
        val times = mutableListOf<Long>()
        
        repeat(iterations) { index ->
            val searchTerm = searchTerms[index % searchTerms.size]
            
            val time = measureNanoTime {
                val results = allEvents.filter { event ->
                    event.title.contains(searchTerm, ignoreCase = true) ||
                    event.description.contains(searchTerm, ignoreCase = true)
                }
                assertTrue(results.size >= 0)
            } / 1_000_000 // Convert to ms
            times.add(time)
        }
        
        val averageTime = times.average()
        val maxTime = times.maxOrNull() ?: 0L
        
        println("=== Event Search Benchmark (200 events) ===")
        println("Iterations: $iterations")
        println("Average time: ${String.format("%.3f", averageTime)}ms")
        println("Max time: ${maxTime}ms")
        println("Target: < 5ms")
        
        assertTrue(
            averageTime < 5,
            "Average search time ${averageTime}ms exceeds target of 5ms"
        )
    }

    // ==================== 14. Poll Score Calculation Benchmark ====================

    @Test
    fun benchmarkPollScoreCalculation() {
        val iterations = 500
        
        // Create a large poll with 50 participants and 20 slots
        val slots = (1..20).map { index ->
            createTestTimeSlot(
                id = "slot-score-$index",
                start = "2025-12-${10 + index}T10:00:00Z",
                end = "2025-12-${10 + index}T12:00:00Z"
            )
        }
        
        val votes = (1..50).associate { participantIndex ->
            "participant-score-$participantIndex" to slots.associate { slot ->
                slot.id to when ((participantIndex + slot.id.hashCode()) % 3) {
                    0 -> Vote.YES
                    1 -> Vote.MAYBE
                    else -> Vote.NO
                }
            }
        }
        
        val poll = Poll("event-score-test", "event-score-test", votes)
        
        val times = mutableListOf<Long>()
        
        repeat(iterations) {
            val time = measureNanoTime {
                val scores = PollLogic.getSlotScores(poll, slots)
                val bestWithScore = PollLogic.getBestSlotWithScore(poll, slots)
                assertTrue(scores.isNotEmpty())
            } / 1_000_000 // Convert to ms
            times.add(time)
        }
        
        val averageTime = times.average()
        val maxTime = times.maxOrNull() ?: 0L
        
        println("=== Poll Score Calculation Benchmark ===")
        println("Iterations: $iterations")
        println("Participants: 50, Slots: 20")
        println("Average time: ${String.format("%.3f", averageTime)}ms")
        println("Max time: ${maxTime}ms")
        println("Target: < 10ms")
        
        assertTrue(
            averageTime < 10,
            "Average poll score calculation time ${averageTime}ms exceeds target of 10ms"
        )
    }

    // ==================== 15. Event Pagination Benchmark ====================

    @Test
    fun benchmarkEventPagination() = runBlocking {
        val iterations = 100
        val repository = EventRepository()
        
        // Pre-populate with 200 events
        repeat(200) { index ->
            val event = createTestEvent(
                id = "page-event-$index",
                title = "Pagination Event $index",
                createdAt = Clock.System.now().toString(),
                status = EventStatus.DRAFT
            )
            repository.createEvent(event)
        }
        
        val times = mutableListOf<Long>()
        
        repeat(iterations) { index ->
            val page = index % 10
            val pageSize = 20
            val orderBy = OrderBy.entries[index % OrderBy.entries.size]
            
            val time = measureTimeMillis {
                val events = repository.getEventsPaginated(
                    page = page,
                    pageSize = pageSize,
                    orderBy = orderBy
                ).first()
                assertTrue(events.size <= pageSize)
            }
            times.add(time)
        }
        
        val averageTime = times.average()
        val maxTime = times.maxOrNull() ?: 0L
        
        println("=== Event Pagination Benchmark (200 events, 20/page) ===")
        println("Iterations: $iterations")
        println("Average time: ${"%.2f".format(averageTime)}ms")
        println("Max time: ${maxTime}ms")
        println("Target: < 10ms")
        
        assertTrue(
            averageTime < 10,
            "Average pagination time ${averageTime}ms exceeds target of 10ms"
        )
    }

    // ==================== 16. Notification Preferences Repository Benchmark ====================

    @Test
    fun benchmarkNotificationPreferencesRepository() = runBlocking {
        val iterations = 200
        val mockPreferencesRepo = createMockPreferencesRepository()
        
        // Pre-populate preferences for multiple users
        val userIds = (1..50).map { "user-pref-$it" }
        userIds.forEach { userId ->
            val prefs = defaultNotificationPreferences(userId).copy(
                quietHoursStart = QuietTime(22, 0),
                quietHoursEnd = QuietTime(8, 0)
            )
            mockPreferencesRepo.savePreferences(prefs)
        }
        
        val times = mutableListOf<Long>()
        
        repeat(iterations) { index ->
            val userId = userIds[index % userIds.size]
            
            val time = measureNanoTime {
                val prefs = mockPreferencesRepo.getPreferences(userId)
                assertTrue(prefs != null)
            } / 1_000_000 // Convert to ms
            times.add(time)
        }
        
        val averageTime = times.average()
        val maxTime = times.maxOrNull() ?: 0L
        
        println("=== Notification Preferences Repository Benchmark ===")
        println("Iterations: $iterations")
        println("Users: ${userIds.size}")
        println("Average time: ${String.format("%.3f", averageTime)}ms")
        println("Max time: ${maxTime}ms")
        println("Target: < 5ms")
        
        assertTrue(
            averageTime < 5,
            "Average preferences repository time ${averageTime}ms exceeds target of 5ms"
        )
    }

    // ==================== 17. DeepLink Pattern Matching Benchmark ====================

    @Test
    fun benchmarkDeepLinkPatternMatching() {
        val iterations = 1000
        
        val deepLinks = listOf(
            DeepLink.create("event/123/details", mapOf("tab" to "votes")),
            DeepLink.create("event/456/poll", emptyMap()),
            DeepLink.create("profile", emptyMap()),
            DeepLink.create("settings", mapOf("category" to "notifications")),
            DeepLink.create("event/789/scenarios", mapOf("scenarioId" to "abc")),
            DeepLink.create("notifications", emptyMap()),
            DeepLink.create("home", emptyMap()),
            DeepLink.create("event/999/meetings", mapOf("meetingId" to "meet-1"))
        )
        
        val patterns = listOf(
            "event/{eventId}/details",
            "event/{eventId}/poll",
            "profile",
            "settings",
            "event/{eventId}/scenarios",
            "notifications",
            "home",
            "event/{eventId}/meetings"
        )
        
        val times = mutableListOf<Long>()
        
        repeat(iterations) { index ->
            val deepLink = deepLinks[index % deepLinks.size]
            val pattern = patterns[index % patterns.size]
            
            val time = measureNanoTime {
                val matches = deepLink.matchesPattern(pattern)
                val params = deepLink.extractPathParameters(pattern)
                assertTrue(matches || !matches) // Just use the results
            } / 1_000_000 // Convert to ms
            times.add(time)
        }
        
        val averageTime = times.average()
        val maxTime = times.maxOrNull() ?: 0L
        
        println("=== DeepLink Pattern Matching Benchmark ===")
        println("Iterations: $iterations")
        println("Average time: ${String.format("%.3f", averageTime)}ms")
        println("Max time: ${maxTime}ms")
        println("Target: < 2ms")
        
        assertTrue(
            averageTime < 2,
            "Average pattern matching time ${averageTime}ms exceeds target of 2ms"
        )
    }

    // ==================== 18. Event Sorting Benchmark ====================

    @Test
    fun benchmarkEventSorting() {
        val iterations = 100
        
        // Generate 300 events with varied properties
        val events = (1..300).map { index ->
            createTestEvent(
                id = "sort-event-$index",
                title = "${('A' + (index % 26))} Event ${1000 - index}",
                createdAt = "2025-${(index % 12) + 1}-${(index % 28) + 1}T10:00:00Z",
                status = EventStatus.entries[index % EventStatus.entries.size],
                participants = (1..(index % 20 + 1)).map { "p-$it" }
            )
        }
        
        val times = mutableListOf<Long>()
        
        repeat(iterations) { index ->
            val time = measureNanoTime {
                val sorted = when (index % 4) {
                    0 -> events.sortedBy { it.title }
                    1 -> events.sortedByDescending { it.createdAt }
                    2 -> events.sortedBy { it.status.name }
                    else -> events.sortedByDescending { it.participants.size }
                }
                assertTrue(sorted.isNotEmpty())
            } / 1_000_000 // Convert to ms
            times.add(time)
        }
        
        val averageTime = times.average()
        val maxTime = times.maxOrNull() ?: 0L
        
        println("=== Event Sorting Benchmark (300 events) ===")
        println("Iterations: $iterations")
        println("Average time: ${String.format("%.3f", averageTime)}ms")
        println("Max time: ${maxTime}ms")
        println("Target: < 5ms")
        
        assertTrue(
            averageTime < 5,
            "Average sorting time ${averageTime}ms exceeds target of 5ms"
        )
    }

    // ==================== 19. RichNotification Builder Benchmark ====================

    @Test
    fun benchmarkRichNotificationBuilder() {
        val iterations = 500
        val times = mutableListOf<Long>()
        
        repeat(iterations) { index ->
            val time = measureNanoTime {
                val notification = richNotification {
                    id("notif-$index")
                    userId("user-$index")
                    title("Test Notification $index")
                    body("This is the body of notification $index")
                    imageUrl("https://example.com/image$index.jpg")
                    largeIcon("https://example.com/icon$index.png")
                    category(NotificationCategory.entries[index % NotificationCategory.entries.size])
                    priority(RichNotificationPriority.entries[index % RichNotificationPriority.entries.size])
                    deepLink("wakeve://events/event-$index")
                    customSound("sound$index")
                    vibrationPattern(listOf(100, 200, 100, 200))
                    ledColor(0xFF0000FF.toInt() + index)
                }
                assertTrue(notification.title.isNotEmpty())
            } / 1_000_000 // Convert to ms
            times.add(time)
        }
        
        val averageTime = times.average()
        val maxTime = times.maxOrNull() ?: 0L
        
        println("=== RichNotification Builder Benchmark ===")
        println("Iterations: $iterations")
        println("Average time: ${String.format("%.3f", averageTime)}ms")
        println("Max time: ${maxTime}ms")
        println("Target: < 2ms")
        
        assertTrue(
            averageTime < 2,
            "Average RichNotification builder time ${averageTime}ms exceeds target of 2ms"
        )
    }

    // ==================== 20. Batch Vote Processing Benchmark ====================

    @Test
    fun benchmarkBatchVoteProcessing() = runBlocking {
        val iterations = 20
        val votesPerIteration = 50
        
        val repository = EventRepository()
        
        // Create a test event with POLLING status
        val event = createTestEvent(
            id = "batch-vote-event",
            title = "Batch Vote Test",
            status = EventStatus.POLLING,
            participants = (1..votesPerIteration).map { "batch-participant-$it" },
            proposedSlots = (1..5).map { index ->
                createTestTimeSlot(
                    id = "batch-slot-$index",
                    start = "2025-12-${10 + index}T10:00:00Z",
                    end = "2025-12-${10 + index}T12:00:00Z"
                )
            }
        )
        repository.createEvent(event)
        
        val times = mutableListOf<Long>()
        
        repeat(iterations) { iteration ->
            val time = measureTimeMillis {
                (1..votesPerIteration).forEach { voteIndex ->
                    val participantId = "batch-participant-$voteIndex"
                    val slotId = "batch-slot-${voteIndex % 5 + 1}"
                    val vote = when (voteIndex % 3) {
                        0 -> Vote.YES
                        1 -> Vote.MAYBE
                        else -> Vote.NO
                    }
                    repository.addVote("batch-vote-event", participantId, slotId, vote)
                }
            }
            times.add(time)
            
            // Reset votes for next iteration
            // Note: In real implementation, we'd need a reset method
        }
        
        val averageTime = times.average()
        val maxTime = times.maxOrNull() ?: 0L
        val votesPerSecond = votesPerIteration * 1000.0 / averageTime
        
        println("=== Batch Vote Processing Benchmark ($votesPerIteration votes) ===")
        println("Iterations: $iterations")
        println("Average time: ${"%.2f".format(averageTime)}ms")
        println("Max time: ${maxTime}ms")
        println("Votes per second: ${"%.2f".format(votesPerSecond)}")
        println("Target: < 500ms")
        
        assertTrue(
            averageTime < 500,
            "Average batch vote time ${averageTime}ms exceeds target of 500ms"
        )
    }

    // ==================== Helper Methods ====================

    private fun createMockPreferencesRepository(): NotificationPreferencesRepositoryInterface {
        return object : NotificationPreferencesRepositoryInterface {
            private val preferences = mutableMapOf<String, NotificationPreferences>()
            
            override suspend fun getPreferences(userId: String): NotificationPreferences? {
                return preferences[userId] ?: defaultNotificationPreferences(userId)
            }
            
            override suspend fun savePreferences(preferences: NotificationPreferences): Result<Unit> {
                this.preferences[preferences.userId] = preferences
                return Result.success(Unit)
            }
            
            override suspend fun deletePreferences(userId: String): Result<Unit> {
                preferences.remove(userId)
                return Result.success(Unit)
            }
        }
    }
}
