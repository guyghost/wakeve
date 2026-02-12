package com.guyghost.wakeve.analytics

import com.guyghost.wakeve.database.WakeveDb
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

/**
 * Service for calculating analytics metrics.
 *
 * Provides metrics for MAU, DAU, retention, and event creation funnels.
 */
class AnalyticsDashboard(private val database: WakeveDb) {

    data class Metrics(
        val mau: Int,
        val dau: Int,
        val newUsers: Int,
        val activeEvents: Int,
        val retention: Double
    )

    data class FunnelStep(
        val step: String,
        val count: Int,
        val conversionRate: Double
    )

    /**
     * Get Monthly Active Users (MAU)
     *
     * @return Number of unique users active in the last 30 days
     */
    fun getMAU(): Int {
        val thirtyDaysAgo = Clock.System.now().minus(30, DateTimeUnit.DAY)
        return database.analyticsQueries
            .getActiveUsersSince(thirtyDaysAgo.toEpochMilliseconds())
            .executeAsOne()
            .toInt()
    }

    /**
     * Get Daily Active Users (DAU)
     *
     * @return Number of unique users active today (UTC)
     */
    fun getDAU(): Int {
        val today = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
        val startOfDay = today.atTime(0, 0).toInstant(TimeZone.UTC)

        return database.analyticsQueries
            .getActiveUsersSince(startOfDay.toEpochMilliseconds())
            .executeAsOne()
            .toInt()
    }

    /**
     * Get retention cohort analysis
     *
     * Calculates retention rate for users who signed up N days ago.
     *
     * @param days Number of days ago for cohort analysis (default: 7)
     * @return Retention rate as percentage (0-100)
     */
    fun getRetention(days: Int = 7): Double {
        val cohortDate = Clock.System.now().minus(days, DateTimeUnit.DAY)

        val cohortUsers = database.analyticsQueries
            .getNewUsersOnDate(cohortDate.toLocalDateTime(TimeZone.UTC).date.toString())
            .executeAsOne()

        val retainedUsers = database.analyticsQueries
            .getRetainedUsers(cohortDate.toEpochMilliseconds())
            .executeAsOne()

        return if (cohortUsers > 0) {
            (retainedUsers.toDouble() / cohortUsers) * 100
        } else 0.0
    }

    /**
     * Get event creation funnel
     *
     * Tracks conversion through the event creation process.
     *
     * @return List of funnel steps with counts and conversion rates
     */
    fun getEventCreationFunnel(): List<FunnelStep> {
        val steps = listOf(
            "event_started" to "Event Started",
            "event_details_entered" to "Details Entered",
            "time_slots_added" to "Time Slots Added",
            "event_created" to "Event Created"
        )

        return steps.mapIndexed { index, (eventName, label) ->
            val count = database.analyticsQueries
                .getEventCount(eventName)
                .executeAsOne()
                .toInt()

            val conversionRate = if (index == 0) 100.0
            else {
                val previousCount = database.analyticsQueries
                    .getEventCount(steps[index - 1].first)
                    .executeAsOne()
                    .toInt()
                if (previousCount > 0) (count.toDouble() / previousCount) * 100 else 0.0
            }

            FunnelStep(label, count, conversionRate)
        }
    }

    /**
     * Get all metrics summary
     *
     * @return Combined metrics summary including MAU, DAU, retention, etc.
     */
    fun getMetricsSummary(): Metrics {
        return Metrics(
            mau = getMAU(),
            dau = getDAU(),
            newUsers = getNewUsersToday(),
            activeEvents = getActiveEventsCount(),
            retention = getRetention()
        )
    }

    /**
     * Get new users today
     *
     * @return Number of users who registered today (UTC)
     */
    private fun getNewUsersToday(): Int {
        val today = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
        return database.analyticsQueries
            .getNewUsersOnDate(today.toString())
            .executeAsOne()
            .toInt()
    }

    /**
     * Get active events count
     *
     * @return Number of events viewed in the last 30 days
     */
    private fun getActiveEventsCount(): Int {
        val thirtyDaysAgo = Clock.System.now().minus(30, DateTimeUnit.DAY)
        return database.analyticsQueries
            .getActiveEventsCount(thirtyDaysAgo.toEpochMilliseconds())
            .executeAsOne()
            .toInt()
    }
}
