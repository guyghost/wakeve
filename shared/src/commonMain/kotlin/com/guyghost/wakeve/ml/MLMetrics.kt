package com.guyghost.wakeve.ml

import kotlinx.serialization.Serializable

/**
 * Enum representing the type of ML operation being monitored.
 * Each operation has specific performance targets for latency and resource usage.
 */
enum class MLOperation {
    /** AI Predictive Recommendations - Target: < 200ms latency */
    RECOMMENDATION_PREDICTION,
    
    /** Photo Recognition - Face Detection - Target: < 2s latency */
    FACE_DETECTION,
    
    /** Photo Recognition - Auto-tagging - Target: < 1s latency */
    PHOTO_TAGGING,
    
    /** Voice Assistant - Speech Recognition - Target: < 300ms (simple), < 1s (complex) */
    VOICE_RECOGNITION,
    
    /** General ML Model Inference */
    MODEL_INFERENCE
}

/**
 * Enum representing the target platform for ML operations.
 */
enum class Platform {
    ANDROID,
    IOS
}

/**
 * Event data class for recording ML performance metrics.
 * All metrics are stored locally and never sent to external servers.
 *
 * @property operation Type of ML operation being performed
 * @property platform Target platform (Android/iOS)
 * @property durationMs Duration of the operation in milliseconds
 * @property success Whether the operation completed successfully
 * @property confidenceScore Optional confidence score from ML model (0.0 - 1.0)
 * @property memoryUsageMB Optional memory usage during operation in megabytes
 * @property timestamp ISO 8601 timestamp of when the operation occurred
 * @property errorMessage Optional error message if operation failed
 */
@Serializable
data class MLMetricsEvent(
    val operation: MLOperation,
    val platform: Platform,
    val durationMs: Long,
    val success: Boolean,
    val confidenceScore: Double? = null,
    val memoryUsageMB: Double? = null,
    val timestamp: String,
    val errorMessage: String? = null
)

/**
 * Aggregated metrics summary for a specific ML operation.
 * Used for performance reporting and optimization analysis.
 *
 * @property operation The ML operation type
 * @property platform The target platform
 * @property totalOperations Total number of operations recorded
 * @property successfulOperations Number of successful operations
 * @property averageLatencyMs Average duration in milliseconds
 * @property minLatencyMs Minimum latency observed
 * @property maxLatencyMs Maximum latency observed
 * @property averageConfidenceScore Average confidence score (0.0 - 1.0)
 * @property averageMemoryUsageMB Average memory usage in megabytes
 */
@Serializable
data class MLMetricsSummary(
    val operation: MLOperation,
    val platform: Platform,
    val totalOperations: Int,
    val successfulOperations: Int,
    val averageLatencyMs: Double,
    val minLatencyMs: Long,
    val maxLatencyMs: Long,
    val averageConfidenceScore: Double?,
    val averageMemoryUsageMB: Double?
) {
    /**
     * Calculate success rate as percentage (0.0 - 100.0).
     */
    val successRate: Double
        get() = if (totalOperations > 0) {
            (successfulOperations.toDouble() / totalOperations) * 100.0
        } else 0.0
}
