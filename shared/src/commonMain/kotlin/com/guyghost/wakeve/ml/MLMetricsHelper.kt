package com.guyghost.wakeve.ml

import com.guyghost.wakeve.getPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

/**
 * Helper object for simplified ML performance tracking.
 * Provides easy-to-use functions for measuring and recording ML operation metrics.
 *
 * Usage examples:
 *
 * Recommendation prediction tracking:
 * ```kotlin
 * val (result, event) = trackMLPerformance(MLOperation.RECOMMENDATION_PREDICTION) {
 *     recommendationEngine.getRecommendations(eventId)
 * }
 * collector.recordMetrics(event.copy(confidenceScore = result.confidence))
 * ```
 *
 * Voice recognition tracking:
 * ```kotlin
 * val (transcript, event) = trackMLPerformance(MLOperation.VOICE_RECOGNITION) {
 *     voiceService.recognize(audioInput)
 * }
 * if (event.durationMs > 300) {
 *     log.warn("Voice recognition exceeded target: ${event.durationMs}ms")
 * }
 * ```
 *
 * Photo tagging with memory measurement:
 * ```kotlin
 * val (tags, event) = trackMLPerformanceWithMemory(MLOperation.PHOTO_TAGGING) {
 *     photoTagger.autoTag(photo)
 * }
 * ```
 */
object MLMetricsHelper {
    
    /**
     * Track performance of an ML operation.
     * Automatically measures latency and records success/failure.
     *
     * @param operation The type of ML operation being tracked
     * @param block The suspendable operation to measure
     * @return Pair containing the operation result and the metrics event
     */
    suspend fun <T> trackMLPerformance(
        operation: MLOperation,
        block: suspend () -> T
    ): Pair<T, MLMetricsEvent> {
        val startTime = System.nanoTime()
        var result: T? = null
        var success = false
        var errorMessage: String? = null
        
        try {
            result = block()
            success = true
        } catch (e: Exception) {
            success = false
            errorMessage = e.message
        }
        
        val durationMs = (System.nanoTime() - startTime) / 1_000_000
        val platformEnum = detectPlatform()
        
        val event = MLMetricsEvent(
            operation = operation,
            platform = platformEnum,
            durationMs = durationMs,
            success = success,
            timestamp = kotlinx.datetime.Clock.System.now().toString(),
            errorMessage = errorMessage
        )
        
        return Pair(result as T, event)
    }
    
    /**
     * Detect the current platform and convert to the ml.Platform enum.
     */
    private fun detectPlatform(): Platform {
        val platformName = getPlatform().name.uppercase()
        return try {
            Platform.valueOf(platformName)
        } catch (e: IllegalArgumentException) {
            // Default to Android if detection fails
            Platform.ANDROID
        }
    }
    
    /**
     * Track performance with confidence score support.
     * Useful for ML operations that return a confidence value.
     *
     * @param operation The type of ML operation being tracked
     * @param block The suspendable operation to measure
     * @return Triple containing the result, confidence score, and metrics event
     */
    suspend fun <T> trackMLPerformanceWithConfidence(
        operation: MLOperation,
        block: suspend () -> Pair<T, Double?>
    ): Triple<T, Double?, MLMetricsEvent> {
        val startTime = System.nanoTime()
        var pairResult: Pair<T, Double?>? = null
        var success = false
        var errorMessage: String? = null
        
        try {
            pairResult = block()
            success = true
        } catch (e: Exception) {
            success = false
            errorMessage = e.message
        }
        
        val durationMs = (System.nanoTime() - startTime) / 1_000_000
        val actualPair = pairResult ?: Pair(null as T, null)
        val platformEnum = detectPlatform()
        
        val event = MLMetricsEvent(
            operation = operation,
            platform = platformEnum,
            durationMs = durationMs,
            success = success,
            confidenceScore = actualPair.second,
            timestamp = kotlinx.datetime.Clock.System.now().toString(),
            errorMessage = errorMessage
        )
        
        return Triple(actualPair.first, actualPair.second, event)
    }
    
    /**
     * Track performance with memory measurement.
     * Measures both latency and memory usage for resource-intensive operations.
     *
     * @param operation The type of ML operation being tracked
     * @param block The suspendable operation to measure
     * @return Triple containing the result, memory usage, and metrics event
     */
    suspend fun <T> trackMLPerformanceWithMemory(
        operation: MLOperation,
        block: suspend () -> T
    ): Triple<T, Double?, MLMetricsEvent> {
        val startTime = System.nanoTime()
        val memoryBefore = measureMemory()
        var result: T? = null
        var success = false
        var errorMessage: String? = null
        
        try {
            result = block()
            success = true
        } catch (e: Exception) {
            success = false
            errorMessage = e.message
        }
        
        val memoryAfter = measureMemory()
        val memoryUsage = if (memoryBefore != null && memoryAfter != null) {
            max(0.0, memoryAfter - memoryBefore)
        } else null
        
        val durationMs = (System.nanoTime() - startTime) / 1_000_000
        val platformEnum = detectPlatform()
        
        val event = MLMetricsEvent(
            operation = operation,
            platform = platformEnum,
            durationMs = durationMs,
            success = success,
            memoryUsageMB = memoryUsage,
            timestamp = kotlinx.datetime.Clock.System.now().toString(),
            errorMessage = errorMessage
        )
        
        return Triple(result as T, memoryUsage, event)
    }
    
    /**
     * Track performance with both confidence and memory measurement.
     * Comprehensive tracking for complex ML operations.
     *
     * @param operation The type of ML operation being tracked
     * @param block The suspendable operation to measure
     * @return Quadruple containing result, confidence, memory, and metrics event
     */
    suspend fun <T> trackMLPerformanceComprehensive(
        operation: MLOperation,
        block: suspend () -> Triple<T, Double?, Double?>
    ): Quadruple<T, Double?, Double?, MLMetricsEvent> {
        val startTime = System.nanoTime()
        var tripleResult: Triple<T, Double?, Double?>? = null
        var success = false
        var errorMessage: String? = null
        
        try {
            tripleResult = block()
            success = true
        } catch (e: Exception) {
            success = false
            errorMessage = e.message
        }
        
        val durationMs = (System.nanoTime() - startTime) / 1_000_000
        val actualTriple = tripleResult ?: Triple(null as T, null, null)
        val platformEnum = detectPlatform()
        
        val event = MLMetricsEvent(
            operation = operation,
            platform = platformEnum,
            durationMs = durationMs,
            success = success,
            confidenceScore = actualTriple.second,
            memoryUsageMB = actualTriple.third,
            timestamp = kotlinx.datetime.Clock.System.now().toString(),
            errorMessage = errorMessage
        )
        
        return Quadruple(actualTriple.first, actualTriple.second, actualTriple.third, event)
    }
    
    /**
     * Measure current memory usage.
     * Platform-specific implementation via expect/actual.
     *
     * @return Current memory usage in megabytes, or null if measurement failed
     */
    private fun measureMemory(): Double? {
        return try {
            val runtime = Runtime.getRuntime()
            (runtime.totalMemory() - runtime.freeMemory()) / (1024.0 * 1024.0)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Check if an operation meets its performance target.
     * Useful for real-time performance monitoring and alerting.
     *
     * @param operation The ML operation type
     * @param actualLatencyMs The actual latency measured
     * @return True if latency is within target, false otherwise
     */
    fun isWithinTarget(operation: MLOperation, actualLatencyMs: Long): Boolean {
        val targetMs = when (operation) {
            MLOperation.RECOMMENDATION_PREDICTION -> 200L
            MLOperation.FACE_DETECTION -> 2000L
            MLOperation.PHOTO_TAGGING -> 1000L
            MLOperation.VOICE_RECOGNITION -> 1000L
            MLOperation.MODEL_INFERENCE -> 500L
        }
        return actualLatencyMs <= targetMs
    }
    
    /**
     * Get the performance target for an operation.
     * Useful for displaying targets vs actual metrics in UI.
     *
     * @param operation The ML operation type
     * @return Target latency in milliseconds
     */
    fun getTargetLatency(operation: MLOperation): Long {
        return when (operation) {
            MLOperation.RECOMMENDATION_PREDICTION -> 200L
            MLOperation.FACE_DETECTION -> 2000L
            MLOperation.PHOTO_TAGGING -> 1000L
            MLOperation.VOICE_RECOGNITION -> 1000L
            MLOperation.MODEL_INFERENCE -> 500L
        }
    }
    
    /**
     * Get a human-readable description of the performance target.
     *
     * @param operation The ML operation type
     * @return Description string for display purposes
     */
    fun getTargetDescription(operation: MLOperation): String {
        return when (operation) {
            MLOperation.RECOMMENDATION_PREDICTION -> "Target: < 200ms"
            MLOperation.FACE_DETECTION -> "Target: < 2s"
            MLOperation.PHOTO_TAGGING -> "Target: < 1s"
            MLOperation.VOICE_RECOGNITION -> "Target: < 300ms (simple) / < 1s (complex)"
            MLOperation.MODEL_INFERENCE -> "Target: < 500ms"
        }
    }
}

/**
 * Simple Quadruple data class for holding 4 values.
 */
data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

/**
 * Simple data class for comprehensive ML tracking results.
 * Holds all relevant metrics in a single container.
 */
data class MLTrackingResult<T>(
    val result: T,
    val latencyMs: Long,
    val confidenceScore: Double?,
    val memoryUsageMB: Double?,
    val success: Boolean,
    val errorMessage: String?
) {
    /**
     * Check if the operation met its performance target.
     */
    fun isWithinTarget(): Boolean {
        return MLMetricsHelper.isWithinTarget(
            operation = operationType,
            actualLatencyMs = latencyMs
        )
    }
    
    /**
     * The operation type that was tracked.
     */
    lateinit var operationType: MLOperation
}

/**
 * Extension function to create MLTrackingResult from MLMetricsEvent.
 */
fun <T> MLMetricsEvent.toTrackingResult(result: T): MLTrackingResult<T> {
    return MLTrackingResult(
        result = result,
        latencyMs = durationMs,
        confidenceScore = confidenceScore,
        memoryUsageMB = memoryUsageMB,
        success = success,
        errorMessage = errorMessage
    ).also { it.operationType = operation }
}
