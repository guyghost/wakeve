package com.guyghost.wakeve.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Web/JavaScript network status detector
 */
class WebNetworkStatusDetector : NetworkStatusDetector {
    private val _isNetworkAvailable = MutableStateFlow(true)
    override val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable

    init {
        // In a real implementation, this would use navigator.onLine
        // and listen to 'online'/'offline' events
        _isNetworkAvailable.value = true // Assume online for web
    }
}