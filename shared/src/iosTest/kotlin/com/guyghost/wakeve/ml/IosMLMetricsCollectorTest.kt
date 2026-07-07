package com.guyghost.wakeve.ml

import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class IosMLMetricsCollectorTest {

    @Test
    fun `recorded metrics are retained and queryable on iOS`() = runTest {
        val collector = IosMLMetricsCollector()
        val event = MLMetricsEvent(
            operation = MLOperation.PHOTO_TAGGING,
            platform = Platform.IOS,
            durationMs = 240,
            success = true,
            confidenceScore = 0.91,
            memoryUsageMB = 42.0,
            timestamp = Instant.parse("2026-06-20T12:00:00Z").toString()
        )

        collector.recordMetrics(event)

        val metrics = collector.getMetrics(
            operation = MLOperation.PHOTO_TAGGING,
            platform = Platform.IOS
        )
        val summary = collector.getMetricsSummary(
            operation = MLOperation.PHOTO_TAGGING,
            platform = Platform.IOS
        )
        val exported = collector.exportMetrics(platform = Platform.IOS)

        assertEquals(listOf(event), metrics)
        assertEquals(1, collector.getMetricsCount())
        assertEquals(240.0, collector.getAverageLatency(MLOperation.PHOTO_TAGGING, Platform.IOS))
        assertEquals(100.0, collector.getSuccessRate(MLOperation.PHOTO_TAGGING, Platform.IOS))
        assertNotNull(summary)
        assertEquals(1, summary.totalOperations)
        assertEquals(1, summary.successfulOperations)
        assertTrue(exported.contains("PHOTO_TAGGING"))
    }
}
