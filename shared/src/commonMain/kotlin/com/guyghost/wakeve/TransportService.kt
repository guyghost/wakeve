package com.guyghost.wakeve

import com.guyghost.wakeve.models.OptimizationType
import com.guyghost.wakeve.models.Route
import com.guyghost.wakeve.models.TransportLocation
import com.guyghost.wakeve.models.TransportMode
import com.guyghost.wakeve.models.TransportOption
import com.guyghost.wakeve.models.TransportPlan
import com.guyghost.wakeve.models.TransportService

/**
 * Compatibility wrapper kept for older call sites.
 *
 * Canonical implementation lives in `com.guyghost.wakeve.transport.TransportService`.
 */
@Deprecated(
    message = "Use com.guyghost.wakeve.transport.TransportService instead.",
    replaceWith = ReplaceWith("com.guyghost.wakeve.transport.TransportService()")
)
class DefaultTransportService : TransportService {

    private val delegate = com.guyghost.wakeve.transport.TransportService()

    override suspend fun getTransportOptions(
        from: TransportLocation,
        to: TransportLocation,
        departureTime: String,
        mode: TransportMode?
    ): List<TransportOption> {
        return delegate.getTransportOptions(from, to, departureTime, mode)
    }

    override suspend fun optimizeRoutes(
        participants: Map<String, TransportLocation>,
        destination: TransportLocation,
        eventTime: String,
        optimizationType: OptimizationType
    ): TransportPlan {
        return delegate.optimizeRoutes(participants, destination, eventTime, optimizationType)
    }

    override suspend fun findGroupMeetingPoints(
        routes: Map<String, Route>,
        maxWaitTimeMinutes: Int
    ): List<String> {
        return delegate.findGroupMeetingPoints(routes, maxWaitTimeMinutes)
    }
}
