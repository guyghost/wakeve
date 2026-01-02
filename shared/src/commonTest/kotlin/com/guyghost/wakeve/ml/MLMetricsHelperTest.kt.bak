package com.guyghost.wakeve.ml

import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for MLMetricsHelper functions.
 * Tests cover performance tracking, target validation, and utility functions.
 */
class MLMetricsHelperTest {
    
    @AfterTest
    fun teardown() = runTest {
        // Cleanup if needed
    }
    
    @Test
    fun `trackMLPerformance returns result and event for successful operation`() = runTest {
        val (result, event) = MLMetricsHelper.trackMLPerformance(
            operation = MLOperation.RECOMMENDATION_PREDICTION
        ) {
            "test_result"
        }
        
        assertEquals("test_result", result)
        assertNotNull(event)
        assertEquals(MLOperation.RECOMMENDATION_PREDICTION, event.operation)
        assertTrue(event.success)
        assertTrue(event.durationMs >= 0)
    }
    
    @Test
    fun `trackMLPerformance captures exception as failure`() = runTest {
        val (result, event) = MLMetricsHelper.trackMLPerformance(
            operation = MLOperation.FACE_DETECTION
        ) {
            throw RuntimeException("Test error")
        }
        
        assertEquals(null, result)
        assertNotNull(event)
        assertFalse(event.success)
        assertEquals("Test error", event.errorMessage)
    }
    
    @Test
    fun `trackMLPerformanceWithConfidence returns result and confidence`() = runTest {
        val (result, confidence, event) = MLMetricsHelper.trackMLPerformanceWithConfidence(
            operation = MLOperation.RECOMMENDATION_PREDICTION
        ) {
            Pair("prediction_result", 0.85)
        }
        
        assertEquals("prediction_result", result)
        assertEquals(0.85, confidence)
        assertTrue(event.success)
        assertEquals(0.85, event.confidenceScore)
    }
    
    @Test
    fun `trackMLPerformanceWithConfidence handles null confidence`() = runTest {
        val (result, confidence, event) = MLMetricsHelper.trackMLPerformanceWithConfidence(
            operation = MLOperation.MODEL_INFERENCE
        ) {
            Pair("inference_result", null)
        }
        
        assertEquals("inference_result", result)
        assertEquals(null, confidence)
        assertEquals(null, event.confidenceScore)
    }
    
    @Test
    fun `trackMLPerformanceWithMemory returns result and memory usage`() = runTest {
        val (result, memoryUsage, event) = MLMetricsHelper.trackMLPerformanceWithMemory(
            operation = MLOperation.PHOTO_TAGGING
        ) {
            "tagged_photo"
        }
        
        assertEquals("tagged_photo", result)
        assertNotNull(memoryUsage)
        assertTrue(memoryUsage >= 0)
        assertNotNull(event.memoryUsageMB)
    }
    
    @Test
    fun `trackMLPerformanceComprehensive returns all metrics`() = runTest {
        val (result, confidence, memoryUsage, event) = MLMetricsHelper.trackMLPerformanceComprehensive(
            operation = MLOperation.VOICE_RECOGNITION
        ) {
            Triple("transcript", 0.92, 15.5)
        }
        
        assertEquals("transcript", result)
        assertEquals(0.92, confidence)
        assertEquals(15.5, memoryUsage)
        assertTrue(event.success)
        assertEquals(0.92, event.confidenceScore)
        assertEquals(15.5, event.memoryUsageMB)
    }
    
    @Test
    fun `isWithinTarget returns true for fast operations`() {
        assertTrue(MLMetricsHelper.isWithinTarget(MLOperation.RECOMMENDATION_PREDICTION, 100))
        assertTrue(MLMetricsHelper.isWithinTarget(MLOperation.FACE_DETECTION, 1500))
        assertTrue(MLMetricsHelper.isWithinTarget(MLOperation.PHOTO_TAGGING, 500))
        assertTrue(MLMetricsHelper.isWithinTarget(MLOperation.VOICE_RECOGNITION, 500))
        assertTrue(MLMetricsHelper.isWithinTarget(MLOperation.MODEL_INFERENCE, 250))
    }
    
    @Test
    fun `isWithinTarget returns false for slow operations`() {
        assertFalse(MLMetricsHelper.isWithinTarget(MLOperation.RECOMMENDATION_PREDICTION, 300))
        assertFalse(MLMetricsHelper.isWithinTarget(MLOperation.FACE_DETECTION, 2500))
        assertFalse(MLMetricsHelper.isWithinTarget(MLOperation.PHOTO_TAGGING, 1500))
        assertFalse(MLMetricsHelper.isWithinTarget(MLOperation.VOICE_RECOGNITION, 1500))
        assertFalse(MLMetricsHelper.isWithinTarget(MLOperation.MODEL_INFERENCE, 600))
    }
    
    @Test
    fun `getTargetLatency returns correct targets`() {
        assertEquals(200L, MLMetricsHelper.getTargetLatency(MLOperation.RECOMMENDATION_PREDICTION))
        assertEquals(2000L, MLMetricsHelper.getTargetLatency(MLOperation.FACE_DETECTION))
        assertEquals(1000L, MLMetricsHelper.getTargetLatency(MLOperation.PHOTO_TAGGING))
        assertEquals(1000L, MLMetricsHelper.getTargetLatency(MLOperation.VOICE_RECOGNITION))
        assertEquals(500L, MLMetricsHelper.getTargetLatency(MLOperation.MODEL_INFERENCE))
    }
    
    @Test
    fun `getTargetDescription returns human readable strings`() {
        val desc = MLMetricsHelper.getTargetDescription(MLOperation.RECOMMENDATION_PREDICTION)
        assertTrue(desc.contains("200ms"))
        
        val faceDesc = MLMetricsHelper.getTargetDescription(MLOperation.FACE_DETECTION)
        assertTrue(faceDesc.contains("2s"))
        
        val voiceDesc = MLMetricsHelper.getTargetDescription(MLOperation.VOICE_RECOGNITION)
        assertTrue(voiceDesc.contains("300ms"))
        assertTrue(voiceDesc.contains("1s"))
    }
    
    @Test
    fun `trackMLPerformance measures actual latency`() = runTest {
        val (_, event) = MLMetricsHelper.trackMLPerformance(
            operation = MLOperation.MODEL_INFERENCE
        ) {
            // Simulate some work
            Thread.sleep(50)
            "result"
        }
        
        // Should have taken at least 50ms
        assertTrue(event.durationMs >= 50)
    }
    
    @Test
    fun `trackMLPerformance captures all operation types`() = runTest {
        MLOperation.entries.forEach { operation ->
            val (_, event) = MLMetricsHelper.trackMLPerformance(operation) {
                "test"
            }
            assertEquals(operation, event.operation)
        }
    }
    
    @Test
    fun `MLTrackingResult isWithinTarget works correctly`() = runTest {
        val fastResult = MLTrackingResult(
            result = "test",
            latencyMs = 100,
            confidenceScore = 0.9,
            memoryUsageMB = 10.0,
            success = true,
            errorMessage = null
        ).apply {
            operationType = MLOperation.RECOMMENDATION_PREDICTION
        }
        
        val slowResult = MLTrackingResult(
            result = "test",
            latencyMs = 300,
            confidenceScore = 0.9,
            memoryUsageMB = 10.0,
            success = true,
            errorMessage = null
        ).apply {
            operationType = MLOperation.RECOMMENDATION_PREDICTION
        }
        
        assertTrue(fastResult.isWithinTarget())
        assertFalse(slowResult.isWithinTarget())
    }
    
    @Test
    fun `toTrackingResult extension works correctly`() {
        val event = MLMetricsEvent(
            operation = MLOperation.RECOMMENDATION_PREDICTION,
            platform = Platform.ANDROID,
            durationMs = 150,
            success = true,
            confidenceScore = 0.85,
            memoryUsageMB = 20.5,
            timestamp = "2025-01-01T00:00:00Z"
        )
        
        val result = event.toTrackingResult("prediction")
        
        assertEquals("prediction", result.result)
        assertEquals(150L, result.latencyMs)
        assertEquals(0.85, result.confidenceScore)
        assertEquals(20.5, result.memoryUsageMB)
        assertTrue(result.success)
        assertEquals(MLOperation.RECOMMENDATION_PREDICTION, result.operationType)
    }
    
    @Test
    fun `trackMLPerformanceWithMemory measures memory delta`() = runTest {
        // Allocate some memory first
        val largeList = mutableListOf<ByteArray>()
        repeat(100) {
            largeList.add(ByteArray(1024 * 1024)) // 1MB each
        }
        
        val (_, memoryUsage, _) = MLMetricsHelper.trackMLPerformanceWithMemory(
            operation = MLOperation.PHOTO_TAGGING
        ) {
            // Additional allocation during operation
            val temp = ByteArray(512 * 1024)
            "result"
        }
        
        // Memory usage should be positive (delta from baseline)
        assertNotNull(memoryUsage)
        // Note: Actual value depends on system, but should be non-negative
        assertTrue(memoryUsage >= 0)
    }
    
    @Test
    fun `trackMLPerformanceComprehensive handles errors with all null metrics`() = runTest {
        val (_, confidence, memoryUsage, event) = MLMetricsHelper.trackMLPerformanceComprehensive(
            operation = MLOperation.FACE_DETECTION
        ) {
            throw RuntimeException("Processing failed")
        }
        
        assertEquals(null, confidence)
        assertEquals(null, memoryUsage)
        assertFalse(event.success)
        assertEquals("Processing failed", event.errorMessage)
    }
}
