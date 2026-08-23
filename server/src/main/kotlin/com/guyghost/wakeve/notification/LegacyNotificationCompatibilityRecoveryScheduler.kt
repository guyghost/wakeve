package com.guyghost.wakeve.notification

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** Production lifecycle owner for compatibility checkpoints that outlive their HTTP request. */
class LegacyNotificationCompatibilityRecoveryScheduler(
    private val worker: LegacyNotificationRegistrationCompatibilityWorker,
    private val pollInterval: Duration = 1.seconds
) : AutoCloseable {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val started = AtomicBoolean(false)
    private var recoveryJob: Job? = null

    init {
        require(pollInterval.isPositive()) { "Compatibility recovery interval must be positive" }
    }

    fun start() {
        if (!started.compareAndSet(false, true)) return
        recoveryJob = scope.launch {
            while (isActive) {
                try {
                    worker.recoverDue()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // No saga/token/error payload is logged from the security-sensitive worker.
                    logger.error("Legacy notification compatibility recovery pass failed")
                }
                delay(pollInterval)
            }
        }
    }

    override fun close() {
        recoveryJob?.cancel()
        scope.cancel()
    }
}
