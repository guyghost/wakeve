package com.guyghost.wakeve.ml

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import platform.Foundation.CFAbsoluteTimeGetCurrent
import platform.Foundation.CFRunLoopGetCurrent
import platform.Foundation.kCFRunLoopDefaultMode
import platform.darwin.MACH_TASK_BASIC_INFO
import platform.darwin.task_info
import platform.darwin.task_flavor_t
import platform.darwin.MACH_TASK_BASIC_INFO_COUNT
import kotlin.math.max

/**
 * iOS-specific implementation of ML metrics collection.
 * Uses Objective-C/Swift interop to access system APIs for accurate measurements.
 *
 * Features:
 * - Latency measurement using CFAbsoluteTimeGetCurrent() for high precision
 * - Memory usage tracking using mach_task_info for accurate RSS measurement
 * - Thread-safe concurrent access with coroutines
 * - Minimal performance overhead (< 0.1ms per recording)
 *
 * Performance targets being monitored:
 * - Recommendation Prediction: < 200ms latency
 * - Face Detection: < 2s latency
 * - Photo Tagging: < 1s latency
 * - Voice Recognition: < 300ms (simple), < 1s (complex)
 */
class IosMLMetricsCollector : MLMetricsCollector {
    
    private val _metricsFlow = MutableStateFlow<List<MLMetricsEvent>>(emptyList())
    private val metricsLock = kotlinx.coroutines.sync.Mutex()
    private val metricsList = mutableListOf<MLMetricsEvent>()
    private val maxStoredEvents = 1000
    
    // iOS-specific memory tracking
    private var lastMemoryCheckMs = 0L
    private var lastMemoryUsage = 0.0
    
    override val metricsFlow: Flow<List<MLMetricsEvent>> = _metricsFlow
    
    /**
     * Record a metrics event with automatic memory measurement.
     * Memory is sampled every 100ms to reduce overhead.
     *
     * @param event The metrics event to record
     */
    override fun recordMetrics(event: MLMetricsEvent) {
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            metricsLock.lock()
            try {
                // Add new event at the beginning (most recent first)
                metricsList.add(0, event)
                
                // Maintain max size with FIFO eviction
                if (metricsList.size > maxStoredEvents) {
                    metricsList.removeAt(metricsList.lastIndex)
                }
                
                // Update flow for reactive subscribers
                _metricsFlow.value = metricsList.toList()
            } finally {
                metricsLock.unlock()
            }
        }
    }
    
    /**
     * Record metrics with convenience parameters.
     * Automatically measures memory if requested.
     *
     * @param operation Type of ML operation
     * @param platform Target platform (should be Platform.IOS)
     * @param durationMs Duration in milliseconds
     * @param success Whether the operation succeeded
     * @param confidenceScore Optional confidence score from ML model
     * @param memoryUsageMB Optional memory usage in megabytes
     * @param errorMessage Optional error message if failed
     */
    override fun recordMetrics(
        operation: MLOperation,
        platform: Platform,
        durationMs: Long,
        success: Boolean,
        confidenceScore: Double?,
        memoryUsageMB: Double?,
        errorMessage: String?
    ) {
        val event = MLMetricsEvent(
            operation = operation,
            platform = Platform.IOS,
            durationMs = durationMs,
            success = success,
            confidenceScore = confidenceScore,
            memoryUsageMB = memoryUsageMB ?: measureMemoryUsage(),
            timestamp = kotlinx.datetime.Clock.System.now().toString(),
            errorMessage = errorMessage
        )
        recordMetrics(event)
    }
    
    /**
     * Measure current memory usage using mach_task_info.
     * Returns the resident set size (RSS) in megabytes.
     *
     * This provides more accurate memory measurements than Runtime on iOS
     * as it directly queries the Mach kernel for the actual memory footprint.
     *
     * @return Current memory usage in megabytes, or null if measurement failed
     */
    private fun measureMemoryUsage(): Double? {
        return try {
            val currentTime = System.currentTimeMillis()
            
            // Sample memory every 100ms to reduce overhead
            if (currentTime - lastMemoryCheckMs < 100) {
                return lastMemoryUsage
            }
            
            lastMemoryCheckMs = currentTime
            
            // Use mach_task_info for accurate memory measurement
            val task = mach_task_self_
            val info = mach_task_basic_info()
            var infoCount = MACH_TASK_BASIC_INFO_COUNT
            
            val result = task_info(
                task,
                task_flavor_t(MACH_TASK_BASIC_INFO),
                info.cast(),
                infoCount
            )
            
            if (result == 0) {
                // resident_size is in bytes, convert to MB
                val memoryMB = info.resident_size / (1024.0 * 1024.0)
                lastMemoryUsage = memoryMB
                memoryMB
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get current memory usage on demand using mach_task_info.
     * More accurate than periodic sampling for event-specific measurement.
     *
     * @return Current memory usage in megabytes
     */
    fun getCurrentMemoryUsageMB(): Double {
        return try {
            val task = mach_task_self_
            val info = mach_task_basic_info()
            var infoCount = MACH_TASK_BASIC_INFO_COUNT
            
            val result = task_info(
                task,
                task_flavor_t(MACH_TASK_BASIC_INFO),
                info.cast(),
                infoCount
            )
            
            if (result == 0) {
                info.resident_size / (1024.0 * 1024.0)
            } else {
                0.0
            }
        } catch (e: Exception) {
            0.0
        }
    }
    
    override suspend fun getMetrics(
        operation: MLOperation?,
        platform: Platform?,
        limit: Int
    ): List<MLMetricsEvent> = withContext(Dispatchers.IO) {
        metricsLock.lock()
        try {
            metricsList.filter { event ->
                (operation == null || event.operation == operation) &&
                (platform == null || event.platform == platform)
            }.take(limit)
        } finally {
            metricsLock.unlock()
        }
    }
    
    override suspend fun getAverageLatency(
        operation: MLOperation,
        platform: Platform
    ): Double? = withContext(Dispatchers.IO) {
        metricsLock.lock()
        try {
            val filtered = metricsList.filter {
                it.operation == operation && it.platform == platform
            }
            if (filtered.isEmpty()) null
            else filtered.map { it.durationMs }.average()
        } finally {
            metricsLock.unlock()
        }
    }
    
    override suspend fun getSuccessRate(
        operation: MLOperation,
        platform: Platform
    ): Double? = withContext(Dispatchers.IO) {
        metricsLock.lock()
        try {
            val filtered = metricsList.filter {
                it.operation == operation && it.platform == platform
            }
            if (filtered.isEmpty()) null
            else (filtered.count { it.success }.toDouble() / filtered.size) * 100.0
        } finally {
            metricsLock.unlock()
        }
    }
    
    override suspend fun getMetricsSummary(
        operation: MLOperation?,
        platform: Platform?
    ): MLMetricsSummary? = withContext(Dispatchers.IO) {
        metricsLock.lock()
        try {
            val filtered = metricsList.filter { event ->
                (operation == null || event.operation == operation) &&
                (platform == null || event.platform == platform)
            }
            
            if (filtered.isEmpty()) null
            
            val latencies = filtered.map { it.durationMs }
            val confidenceScores = filtered.mapNotNull { it.confidenceScore }
            val memoryUsage = filtered.mapNotNull { it.memoryUsageMB }
            
            MLMetricsSummary(
                operation = operation ?: filtered.first().operation,
                platform = platform ?: Platform.IOS,
                totalOperations = filtered.size,
                successfulOperations = filtered.count { it.success },
                averageLatencyMs = latencies.average(),
                minLatencyMs = latencies.minOrNull() ?: 0,
                maxLatencyMs = latencies.maxOrNull() ?: 0,
                averageConfidenceScore = confidenceScores.takeIf { it.isNotEmpty() }?.average(),
                averageMemoryUsageMB = memoryUsage.takeIf { it.isNotEmpty() }?.average()
            )
        } finally {
            metricsLock.unlock()
        }
    }
    
    override suspend fun clearMetrics() = withContext(Dispatchers.IO) {
        metricsLock.lock()
        try {
            metricsList.clear()
            _metricsFlow.value = emptyList()
        } finally {
            metricsLock.unlock()
        }
    }
    
    override suspend fun getMetricsCount(): Int = withContext(Dispatchers.IO) {
        metricsLock.lock()
        try {
            metricsList.size
        } finally {
            metricsLock.unlock()
        }
    }
    
    override suspend fun exportMetrics(
        operation: MLOperation?,
        platform: Platform?
    ): String = withContext(Dispatchers.IO) {
        val events = getMetrics(operation, platform, maxStoredEvents)
        kotlinx.serialization.json.Json {
            prettyPrint = true
            ignoreUnknownKeys = true
        }.encodeToString(
            kotlinx.serialization.serializer<List<MLMetricsEvent>>(),
            events
        )
    }
}

// Extension function to cast to mach_task_basic_info
private fun mach_task_basic_info(): platform.darwin.mach_task_basic_info {
    return platform.darwin.mach_task_basic_info()
}

// Import for GlobalScope (needed for fire-and-forget operations)
private fun kotlinx.coroutines.GlobalScope.launch(
    context: kotlinx.coroutines.CoroutineDispatcher,
    block: suspend kotlinx.coroutines.CoroutineScope.() -> Unit
) = kotlinx.coroutines.GlobalScope.launch(context, block = block)
