package com.guyghost.wakeve.ml

/**
 * iOS-specific implementation of ML metrics collection.
 *
 * Keeps metrics locally in memory until a durable iOS persistence layer is wired.
 */
class IosMLMetricsCollector(
    private val delegate: MLMetricsCollector = DefaultMLMetricsCollector()
) : MLMetricsCollector by delegate
