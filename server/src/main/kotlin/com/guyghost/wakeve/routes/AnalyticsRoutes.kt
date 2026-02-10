package com.guyghost.wakeve.routes

import com.guyghost.wakeve.analytics.AnalyticsDashboard
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
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
                        val metrics = dashboard.getMetricsSummary()
                        call.respond(HttpStatusCode.OK, metrics)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to e.message.orEmpty())
                        )
                    }
                }

                // GET /api/analytics/mau - Get Monthly Active Users
                get("/mau") {
                    try {
                        val mau = dashboard.getMAU()
                        call.respond(HttpStatusCode.OK, mapOf("mau" to mau))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to e.message.orEmpty())
                        )
                    }
                }

                // GET /api/analytics/dau - Get Daily Active Users
                get("/dau") {
                    try {
                        val dau = dashboard.getDAU()
                        call.respond(HttpStatusCode.OK, mapOf("dau" to dau))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to e.message.orEmpty())
                        )
                    }
                }

                // GET /api/analytics/retention - Get retention cohort analysis
                get("/retention") {
                    try {
                        val days = call.request.queryParameters["days"]?.toIntOrNull() ?: 7
                        val retention = dashboard.getRetention(days)
                        call.respond(HttpStatusCode.OK, mapOf(
                            "retention" to retention,
                            "period_days" to days
                        ))
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to e.message.orEmpty())
                        )
                    }
                }

                // GET /api/analytics/funnel - Get event creation funnel
                get("/funnel") {
                    try {
                        val funnel = dashboard.getEventCreationFunnel()
                        call.respond(HttpStatusCode.OK, funnel)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            mapOf("error" to e.message.orEmpty())
                        )
                    }
                }

                // GET /api/analytics/export - Export analytics data
                get("/export") {
                    try {
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
                            mapOf("error" to e.message.orEmpty())
                        )
                    }
                }
            }
        }
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
