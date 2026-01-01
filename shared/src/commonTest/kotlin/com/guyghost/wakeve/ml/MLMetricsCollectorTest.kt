package com.guyghost.wakeve.ml

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for MLMetricsCollector implementation.
 * Tests cover basic CRUD operations, filtering, and aggregation.
 */
class MLMetricsCollectorTest {
    
    private lateinit var collector: DefaultMLMetricsCollector
    
    @BeforeTest
    fun setup() {
        collector = DefaultMLMetricsCollector()
    }
    
    @AfterTest
    fun teardown() = runTest {
        collector.clearMetrics()
    }
    
    @Test
    fun `recordMetrics adds event to collection`() = runTest {
        val event = createTestEvent(
            operation = MLOperation.RECOMMENDATION_PREDICTION,
            platform = Platform.ANDROID,
            durationMs = 150,
            success = true
        )
        
        collector.recordMetrics(event)
        
        val metrics = collector.getMetrics()
        assertEquals(1, metrics.size)
        assertEquals(MLOperation.RECOMMENDATION_PREDICTION, metrics[0].operation)
    }
    
    @Test
    fun `recordMetrics with convenience method adds event`() = runTest {
        collector.recordMetrics(
            operation = MLOperation.FACE_DETECTION,
            platform = Platform.IOS,
            durationMs = 1500,
            success = true,
            confidenceScore = 0.85
        )
        
        val metrics = collector.getMetrics()
        assertEquals(1, metrics.size)
        assertEquals(MLOperation.FACE_DETECTION, metrics[0].operation)
        assertEquals(0.85, metrics[0].confidenceScore)
    }
    
    @Test
    fun `getMetrics with no filters returns all events`() = runTest {
        collector.recordMetrics(createTestEvent(MLOperation.RECOMMENDATION_PREDICTION, Platform.ANDROID))
        collector.recordMetrics(createTestEvent(MLOperation.FACE_DETECTION, Platform.IOS))
        collector.recordMetrics(createTestEvent(MLOperation.VOICE_RECOGNITION, Platform.ANDROID))
        
        val metrics = collector.getMetrics()
        assertEquals(3, metrics.size)
    }
    
    @Test
    fun `getMetrics with operation filter returns matching events`() = runTest {
        collector.recordMetrics(createTestEvent(MLOperation.RECOMMENDATION_PREDICTION, Platform.ANDROID))
        collector.recordMetrics(createTestEvent(MLOperation.RECOMMENDATION_PREDICTION, Platform.IOS))
        collector.recordMetrics(createTestEvent(MLOperation.FACE_DETECTION, Platform.ANDROID))
        
        val metrics = collector.getMetrics(operation = MLOperation.RECOMMENDATION_PREDICTION)
        assertEquals(2, metrics.size)
        assertTrue(metrics.all { it.operation == MLOperation.RECOMMENDATION_PREDICTION })
    }
    
    @Test
    fun `getMetrics with platform filter returns matching events`() = runTest {
        collector.recordMetrics(createTestEvent(MLOperation.RECOMMENDATION_PREDICTION, Platform.ANDROID))
        collector.recordMetrics(createTestEvent(MLOperation.RECOMMENDATION_PREDICTION, Platform.IOS))
        collector.recordMetrics(createTestEvent(MLOperation.FACE_DETECTION, Platform.ANDROID))
        
        val metrics = collector.getMetrics(platform = Platform.ANDROID)
        assertEquals(2, metrics.size)
        assertTrue(metrics.all { it.platform == Platform.ANDROID })
    }
    
    @Test
    fun `getMetrics with combined filters returns matching events`() = runTest {
        collector.recordMetrics(createTestEvent(MLOperation.RECOMMENDATION_PREDICTION, Platform.ANDROID))
        collector.recordMetrics(createTestEvent(MLOperation.RECOMMENDATION_PREDICTION, Platform.IOS))
        collector.recordMetrics(createTestEvent(MLOperation.FACE_DETECTION, Platform.ANDROID))
        
        val metrics = collector.getMetrics(
            operation = MLOperation.RECOMMENDATION_PREDICTION,
            platform = Platform.ANDROID
        )
        assertEquals(1, metrics.size)
        assertEquals(MLOperation.RECOMMENDATION_PREDICTION, metrics[0].operation)
        assertEquals(Platform.ANDROID, metrics[0].platform)
    }
    
    @Test
    fun `getMetrics with limit respects maximum count`() = runTest {
        repeat(10) { index ->
            collector.recordMetrics(createTestEvent(MLOperation.RECOMMENDATION_PREDICTION, Platform.ANDROID))
        }
        
        val metrics = collector.getMetrics(limit = 5)
        assertEquals(5, metrics.size)
    }
    
    @Test
    fun `getAverageLatency calculates correct average`() = runTest {
        collector.recordMetrics(createTestEvent(MLOperation.RECOMMENDATION_PREDICTION, Platform.ANDROID, durationMs = 100))
        collector.recordMetrics(createTestEvent(MLOperation.RECOMMENDATION_PREDICTION, Platform.ANDROID, durationMs = 200))
        collector.recordMetrics(createTestEvent(MLOperation.RECOMMENDATION_PREDICTION, Platform.ANDROID, durationMs = 300))
        
        val average = collector.getAverageLatency(MLOperation.RECOMMENDATION_PREDICTION, Platform.ANDROID)
        assertNotNull(average)
        assertEquals(200.0, average)
    }
    
    @Test
    fun `getAverageLatency returns null when no data`() = runTest {
        val average = collector.getAverageLatency(MLOperation.RECOMMENDATION_PREDICTION, Platform.ANDROID)
        assertNull(average)
    }
    
    @Test
    fun `getAverageLatency filters by platform correctly`() = runTest {
        collector.recordMetrics(createTestEvent(MLOperation.RECOMMENDATION_PREDICTION, Platform.ANDROID, durationMs = 100))
        collector.recordMetrics(createTestEvent(MLOperation.RECOMMENDATION_PREDICTION, Platform.IOS, durationMs = 200))
        
        val androidAverage = collector.getAverageLatency(MLOperation.RECOMMENDATION_PREDICTION, Platform.ANDROID)
        val iosAverage = collector.getAverageLatency(MLOperation.RECOMMENDATION_PREDICTION, Platform.IOS)
        
        assertEquals(100.0, androidAverage)
        assertEquals(200.0, iosAverage)
    }
    
    @Test
    fun `getSuccessRate calculates correct percentage`() = runTest {
        collector.recordMetrics(createTestEvent(MLOperation.RECOMMENDATION_PREDICTION, Platform.ANDROID, success = true))
        collector.recordMetrics(createTestEvent(MLOperation.RECOMMENDATION_PREDICTION, Platform.ANDROID, success = true))
        collector.recordMetrics(createTestEvent(MLOperation.RECOMMENDATION_PREDICTION, Platform.ANDROID, success = false))
        
        val successRate = collector.getSuccessRate(MLOperation.RECOMMENDATION_PREDICTION, Platform.ANDROID)
        assertNotNull(successRate)
        assertEquals(66.666, successRate, 0.01)
    }
    
    @Test
    fun `getSuccessRate returns null when no data`() = runTest {
        val successRate = collector.getSuccessRate(MLOperation.RECOMMENDATION_PREDICTION, Platform.ANDROID)
        assertNull(successRate)
    }
    
    @Test
    fun `getMetricsSummary returns correct aggregated data`() = runTest {
        collector.recordMetrics(createTestEvent(MLOperation.RECOMMENDATION_PREDICTION, Platform.ANDROID, durationMs = 100, success = true, confidenceScore = 0.8))
        collector.recordMetrics(createTestEvent(MLOperation.RECOMMENDATION_PREDICTION, Platform.ANDROID, durationMs = 200, success = true, confidenceScore = 0.9))
        collector.recordMetrics(createTestEvent(MLOperation.RECOMMENDATION_PREDICTION, Platform.ANDROID, durationMs = 300, success = false, confidenceScore = 0.7))
        
        val summary = collector.getMetricsSummary(MLOperation.RECOMMENDATION_PREDICTION, Platform.ANDROID)
        assertNotNull(summary)
        assertEquals(3, summary.totalOperations)
        assertEquals(2, summary.successfulOperations)
        assertEquals(200.0, summary.averageLatencyMs)
        assertEquals(100L, summary.minLatencyMs)
        assertEquals(300L, summary.maxLatencyMs)
        assertEquals(0.8, summary.averageConfidenceScore)
        assertEquals(66.666, summary.successRate, 0.01)
    }
    
    @Test
    fun `getMetricsSummary with no data returns null`() = runTest {
        val summary = collector.getMetricsSummary(MLOperation.RECOMMENDATION_PREDICTION, Platform.ANDROID)
        assertNull(summary)
    }
    
    @Test
    fun `clearMetrics removes all events`() = runTest {
        collector.recordMetrics(createTestEvent(MLOperation.RECOMMENDATION_PREDICTION, Platform.ANDROID))
        collector.recordMetrics(createTestEvent(MLOperation.FACE_DETECTION, Platform.IOS))
        
        collector.clearMetrics()
        
        val count = collector.getMetricsCount()
        assertEquals(0, count)
    }
    
    @Test
    fun `getMetricsCount returns correct count`() = runTest {
        collector.recordMetrics(createTestEvent(MLOperation.RECOMMENDATION_PREDICTION, Platform.ANDROID))
        collector.recordMetrics(createTestEvent(MLOperation.FACE_DETECTION, Platform.IOS))
        collector.recordMetrics(createTestEvent(MLOperation.VOICE_RECOGNITION, Platform.ANDROID))
        
        val count = collector.getMetricsCount()
        assertEquals(3, count)
    }
    
    @Test
    fun `exportMetrics returns valid JSON`() = runTest {
        collector.recordMetrics(createTestEvent(MLOperation.RECOMMENDATION_PREDICTION, Platform.ANDROID, durationMs = 100))
        
        val json = collector.exportMetrics()
        assertTrue(json.contains("RECOMMENDATION_PREDICTION"))
        assertTrue(json.contains("ANDROID"))
        assertTrue(json.contains("100"))
    }
    
    @Test
    fun `metricsFlow emits updated events`() = runTest {
        collector.recordMetrics(createTestEvent(MLOperation.RECOMMENDATION_PREDICTION, Platform.ANDROID))
        
        val flow = collector.metricsFlow.first()
        assertEquals(1, flow.size)
    }
    
    @Test
    fun `maxStoredEvents limit is respected`() = runTest {
        // Add more events than the max (1000)
        repeat(1005) { index ->
            collector.recordMetrics(createTestEvent(MLOperation.RECOMMENDATION_PREDICTION, Platform.ANDROID))
        }
        
        val count = collector.getMetricsCount()
        assertEquals(1000, count)
    }
    
    // Helper function to create test events
    private fun createTestEvent(
        operation: MLOperation,
        platform: Platform,
        durationMs: Long = 100,
        success: Boolean = true,
        confidenceScore: Double? = null,
        memoryUsageMB: Double? = null
    ): MLMetricsEvent {
        return MLMetricsEvent(
            operation = operation,
            platform = platform,
            durationMs = durationMs,
            success = success,
            confidenceScore = confidenceScore,
            memoryUsageMB = memoryUsageMB,
            timestamp = kotlinx.datetime.Clock.System.now().toString()
        )
    }
}
