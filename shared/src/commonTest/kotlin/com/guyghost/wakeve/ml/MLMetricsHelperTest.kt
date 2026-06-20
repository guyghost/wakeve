package com.guyghost.wakeve.ml

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class MLMetricsHelperTest {

    @Test
    fun `trackMLPerformance records stable error message`() = runTest {
        val secret = "SECRET-ML-TOKEN"

        val event = runCatching {
            MLMetricsHelper.trackMLPerformance<String>(MLOperation.MODEL_INFERENCE) {
                throw IllegalStateException("model failed token=$secret")
            }.second
        }.getOrNull()

        assertStableMlError(event, secret)
    }

    @Test
    fun `trackMLPerformanceWithConfidence records stable error message`() = runTest {
        val secret = "SECRET-CONFIDENCE-TOKEN"

        val event = runCatching {
            MLMetricsHelper.trackMLPerformanceWithConfidence<String>(MLOperation.RECOMMENDATION_PREDICTION) {
                throw IllegalStateException("confidence failed token=$secret")
            }.third
        }.getOrNull()

        assertStableMlError(event, secret)
    }

    @Test
    fun `trackMLPerformanceWithMemory records stable error message`() = runTest {
        val secret = "SECRET-MEMORY-TOKEN"

        val event = runCatching {
            MLMetricsHelper.trackMLPerformanceWithMemory<String>(MLOperation.PHOTO_TAGGING) {
                throw IllegalStateException("memory failed token=$secret")
            }.third
        }.getOrNull()

        assertStableMlError(event, secret)
    }

    @Test
    fun `trackMLPerformanceComprehensive records stable error message`() = runTest {
        val secret = "SECRET-COMPREHENSIVE-TOKEN"

        val event = runCatching {
            MLMetricsHelper.trackMLPerformanceComprehensive<String>(MLOperation.FACE_DETECTION) {
                throw IllegalStateException("comprehensive failed token=$secret")
            }.fourth
        }.getOrNull()

        assertStableMlError(event, secret)
    }

    private fun assertStableMlError(event: MLMetricsEvent?, secret: String) {
        val message = event?.errorMessage.orEmpty()

        assertEquals(false, event?.success)
        assertEquals(mlOperationFailureMessage(), message)
        assertFalse(message.contains(secret))
        assertFalse(message.contains("token=", ignoreCase = true))
        assertFalse(message.contains("failed token", ignoreCase = true))
    }
}
