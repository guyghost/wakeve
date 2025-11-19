package com.guyghost.wakeve.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_get_status
import platform.Network.nw_path_status_satisfied
import platform.darwin.dispatch_queue_create
import kotlin.native.concurrent.freeze

/**
 * iOS-specific network status detector using Network framework
 */
class IosNetworkStatusDetector : NetworkStatusDetector {
    private val _isNetworkAvailable = MutableStateFlow(true)
    override val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable

    private val monitor = nw_path_monitor_create()
    private val queue = dispatch_queue_create("network-monitor", null)

    init {
        monitor.freeze()
        queue.freeze()

        nw_path_monitor_set_queue(monitor, queue)
        nw_path_monitor_set_update_handler(monitor) { path ->
            val status = nw_path_get_status(path)
            _isNetworkAvailable.value = (status == nw_path_status_satisfied)
        }

        nw_path_monitor_start(monitor)
    }

    fun dispose() {
        nw_path_monitor_cancel(monitor)
    }
}