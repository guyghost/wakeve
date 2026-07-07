package com.guyghost.wakeve.routes

import com.guyghost.wakeve.analytics.AnalyticsDashboard
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

/**
 * Configure analytics routes.
 *
 * Provides endpoints for accessing analytics metrics and data.
 * All endpoints are protected by JWT authentication.
 */
fun Application.analyticsRoutes(dashboard: AnalyticsDashboard) {
    routing {
        authenticate("auth-jwt") {
            route("/api/analytics") {

                // GET /api/analytics/metrics - Get all metrics summary
                get("/metrics") {
                    try {
                        if (!call.requireAnalyticsAccess()) return@get
                        val metrics = dashboard.getMetricsSummary()
                        call.respond(HttpStatusCode.OK, metrics)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to analyticsMetricsFailureMessage())
                        )
                    }
                }

                // GET /api/analytics/mau - Get Monthly Active Users
                get("/mau") {
                    try {
                        if (!call.requireAnalyticsAccess()) return@get
                        val mau = dashboard.getMAU()
                        call.respond(HttpStatusCode.OK, mapOf("mau" to mau))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to analyticsMauFailureMessage())
                        )
                    }
                }

                // GET /api/analytics/dau - Get Daily Active Users
                get("/dau") {
                    try {
                        if (!call.requireAnalyticsAccess()) return@get
                        val dau = dashboard.getDAU()
                        call.respond(HttpStatusCode.OK, mapOf("dau" to dau))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to analyticsDauFailureMessage())
                        )
                    }
                }

                // GET /api/analytics/retention - Get retention cohort analysis
                get("/retention") {
                    try {
                        if (!call.requireAnalyticsAccess()) return@get
                        val days = (call.request.queryParameters["days"]?.toIntOrNull() ?: 7).coerceIn(1, 365)
                        val retention = dashboard.getRetention(days)
                        call.respond(HttpStatusCode.OK, mapOf(
                            "retention" to retention,
                            "period_days" to days
                        ))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to analyticsRetentionFailureMessage())
                        )
                    }
                }

                // GET /api/analytics/funnel - Get event creation funnel
                get("/funnel") {
                    try {
                        if (!call.requireAnalyticsAccess()) return@get
                        val funnel = dashboard.getEventCreationFunnel()
                        call.respond(HttpStatusCode.OK, funnel)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to analyticsFunnelFailureMessage())
                        )
                    }
                }

                // GET /api/analytics/export - Export analytics data
                get("/export") {
                    try {
                        if (!call.requireAnalyticsAccess()) return@get
                        // Export analytics data as CSV/JSON
                        val format = call.request.queryParameters["format"] ?: "json"
                        val metrics = dashboard.getMetricsSummary()

                        when (format.lowercase()) {
                            "csv" -> {
                                call.respondText(
                                    generateCSV(metrics),
                                    contentType = ContentType.Text.CSV
                                )
                            }
                            else -> call.respond(HttpStatusCode.OK, metrics)
                        }
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to analyticsExportFailureMessage())
                        )
                    }
                }
            }
        }
    }
}

private suspend fun ApplicationCall.requireAnalyticsAccess(): Boolean {
    val principal = principal<JWTPrincipal>() ?: run {
        respond(HttpStatusCode.Unauthorized, mapOf("error" to "Authentication required"))
        return false
    }

    if (!principal.canViewAnalytics()) {
        respond(HttpStatusCode.Forbidden, mapOf("error" to "Analytics access requires administrator privileges"))
        return false
    }

    return true
}

private fun JWTPrincipal.canViewAnalytics(): Boolean {
    val role = payload.getClaim("role")?.asString()
    val roles = payload.getClaim("roles")?.asList(String::class.java).orEmpty()
    val permissions = payload.getClaim("permissions")?.asList(String::class.java).orEmpty()

    return (roles + listOfNotNull(role)).any { it.equals("ADMIN", ignoreCase = true) } ||
        permissions.any { permission ->
            permission.equals("ANALYTICS_READ", ignoreCase = true) ||
                permission.equals("ANALYTICS", ignoreCase = true)
        }
}

/**
 * Generate CSV output from metrics.
 *
 * @param metrics Analytics metrics data
 * @return CSV formatted string
 */
private fun generateCSV(metrics: AnalyticsDashboard.Metrics): String {
    return """
        Metric,Value
        MAU,${metrics.mau}
        DAU,${metrics.dau}
        New Users,${metrics.newUsers}
        Active Events,${metrics.activeEvents}
        Retention,${metrics.retention}
    """.trimIndent()
}

internal fun analyticsMetricsFailureMessage(): String =
    "Failed to fetch analytics metrics. Please try again."

internal fun analyticsMauFailureMessage(): String =
    "Failed to fetch monthly active users. Please try again."

internal fun analyticsDauFailureMessage(): String =
    "Failed to fetch daily active users. Please try again."

internal fun analyticsRetentionFailureMessage(): String =
    "Failed to fetch retention analytics. Please try again."

internal fun analyticsFunnelFailureMessage(): String =
    "Failed to fetch funnel analytics. Please try again."

internal fun analyticsExportFailureMessage(): String =
    "Failed to export analytics. Please try again."
