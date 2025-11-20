package com.guyghost.wakeve

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import com.guyghost.wakeve.models.*

class SuggestionEngineTest {

    private val engine = DefaultSuggestionEngine() // Assume we implement this

    private val sampleEvent = Event(
        id = "event-1",
        title = "Team Meeting",
        description = "Weekly sync",
        organizerId = "org-1",
        participants = listOf("p1", "p2"),
        proposedSlots = listOf(
            TimeSlot("slot-1", "2025-12-01T10:00:00Z", "2025-12-01T12:00:00Z", "UTC"),
            TimeSlot("slot-2", "2025-12-02T14:00:00Z", "2025-12-02T16:00:00Z", "UTC")
        ),
        deadline = "2025-11-30T23:59:59Z",
        status = EventStatus.DRAFT
    )

    private val userPrefs = UserPreferences(
        userId = "p1",
        preferredDaysOfWeek = listOf("monday", "friday"),
        preferredTimes = listOf("morning"),
        preferredLocations = listOf("office"),
        preferredActivities = listOf("meeting"),
        budgetRange = BudgetRange.MEDIUM,
        lastUpdated = "2025-11-20T10:00:00Z"
    )

    @Test
    fun suggestDatesReturnsRecommendations() {
        val recommendations = engine.suggestDates(sampleEvent, userPrefs)

        assertTrue(recommendations.isNotEmpty())
        recommendations.forEach { rec ->
            assertEquals(RecommendationType.DATE, rec.type)
            assertEquals(sampleEvent.id, rec.eventId)
            assertTrue(rec.score >= 0.0 && rec.score <= 1.0)
            assertTrue(rec.reason.isNotEmpty())
        }
    }

    @Test
    fun suggestDatesPrioritizesPreferredDays() {
        // Assuming slot-1 is Monday, slot-2 is Tuesday
        val recommendations = engine.suggestDates(sampleEvent, userPrefs)

        // The Monday slot should have higher score
        val mondayRec = recommendations.find { it.content.contains("2025-12-01") }
        val tuesdayRec = recommendations.find { it.content.contains("2025-12-02") }

        assertNotNull(mondayRec)
        assertNotNull(tuesdayRec)
        assertTrue(mondayRec.score > tuesdayRec.score)
    }

    @Test
    fun suggestLocationsReturnsRecommendations() {
        val recommendations = engine.suggestLocations(sampleEvent, userPrefs)

        assertTrue(recommendations.isNotEmpty())
        recommendations.forEach { rec ->
            assertEquals(RecommendationType.LOCATION, rec.type)
            assertEquals(sampleEvent.id, rec.eventId)
            assertTrue(rec.score >= 0.0 && rec.score <= 1.0)
            assertTrue(rec.reason.isNotEmpty())
        }
    }

    @Test
    fun suggestActivitiesReturnsRecommendations() {
        val recommendations = engine.suggestActivities(sampleEvent, userPrefs)

        assertTrue(recommendations.isNotEmpty())
        recommendations.forEach { rec ->
            assertEquals(RecommendationType.ACTIVITY, rec.type)
            assertEquals(sampleEvent.id, rec.eventId)
            assertTrue(rec.score >= 0.0 && rec.score <= 1.0)
            assertTrue(rec.reason.isNotEmpty())
        }
    }

    @Test
    fun suggestionsAreSortedByScoreDescending() {
        val recommendations = engine.suggestDates(sampleEvent, userPrefs)

        for (i in 0 until recommendations.size - 1) {
            assertTrue(recommendations[i].score >= recommendations[i + 1].score)
        }
    }
}</content>
<parameter name="filePath">shared/src/commonTest/kotlin/com/guyghost/wakeve/SuggestionEngineTest.kt