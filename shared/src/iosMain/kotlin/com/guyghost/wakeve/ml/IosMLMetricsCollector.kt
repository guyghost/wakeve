package com.guyghost.wakeve.ml

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * iOS-specific implementation of ML metrics collection.
 * Simplified stub implementation.
 */
class IosMLMetricsCollector : MLMetricsCollector {
    private val _metricsFlow = MutableStateFlow<List<MLMetricsEvent>>(emptyList())
    override val metricsFlow: Flow<List<MLMetricsEvent>> = _metricsFlow
    override fun recordMetrics(event: MLMetricsEvent) {}
    override fun recordMetrics(operation: MLOperation, platform: Platform, durationMs: Long, success: Boolean, confidenceScore: Double?, memoryUsageMB: Double?, errorMessage: String?) {}
    override suspend fun getMetrics(operation: MLOperation?, platform: Platform?, limit: Int): List<MLMetricsEvent> = emptyList()
    override suspend fun getAverageLatency(operation: MLOperation, platform: Platform): Double? = null
    override suspend fun getSuccessRate(operation: MLOperation, platform: Platform): Double? = null
    override suspend fun getMetricsSummary(operation: MLOperation?, platform: Platform?): MLMetricsSummary? = null
    override suspend fun clearMetrics() {}
    override suspend fun getMetricsCount(): Int = 0
    override suspend fun exportMetrics(operation: MLOperation?, platform: Platform?): String = ""
}
