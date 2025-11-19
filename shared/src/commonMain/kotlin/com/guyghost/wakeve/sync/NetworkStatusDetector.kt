package com.guyghost.wakeve.sync

import kotlinx.coroutines.flow.StateFlow

/**
 * Platform-agnostic network status detector interface
 */
interface NetworkStatusDetector {
    val isNetworkAvailable: StateFlow<Boolean>
}

/**
 * Factory function to create platform-specific network status detector
 */
expect fun createNetworkStatusDetector(): NetworkStatusDetector