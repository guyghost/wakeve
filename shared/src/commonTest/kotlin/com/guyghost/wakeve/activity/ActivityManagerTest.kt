package com.guyghost.wakeve.activity

import com.guyghost.wakeve.models.Activity
import com.guyghost.wakeve.models.ActivityRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for ActivityManager service
 */
class ActivityManagerTest {
    
    @Test
    fun `createActivity creates valid activity with required fields`() {
        val request = ActivityRequest(
            name = "Hike to the lake",
            description = "Beautiful mountain hike",
            duration = 180,
            organizerId = "user-1"
        )
        
        val activity = ActivityManager.createActivity("event-1", request)
        
        assertEquals("event-1", activity.eventId)
        assertEquals("Hike to the lake", activity.name)
        assertEquals("Beautiful mountain hike", activity.description)
        assertEquals(180, activity.duration)
        assertEquals("user-1", activity.organizerId)
        assertNull(activity.date)
        assertNull(activity.time)
        assertNull(activity.location)
        assertNull(activity.cost)
        assertNull(activity.maxParticipants)
        assertTrue(activity.registeredParticipantIds.isEmpty())
        assertNotNull(activity.id)
        assertNotNull(activity.createdAt)
        assertNotNull(activity.updatedAt)
    }
    
    @Test
    fun `createActivity creates activity with all optional fields`() {
        val request = ActivityRequest(
            name = "Beach volleyball",
            description = "Friendly volleyball game",
            date = "2025-12-26",
            time = "14:30",
            duration = 90,
            location = "Main Beach",
            cost = 1000,
            maxParticipants = 12,
            organizerId = "user-1",
            notes = "Bring sunscreen",
            scenarioId = "scenario-1"
        )
        
        val activity = ActivityManager.createActivity("event-1", request)
        
        assertEquals("Beach volleyball", activity.name)
        assertEquals("2025-12-26", activity.date)
        assertEquals("14:30", activity.time)
        assertEquals("Main Beach", activity.location)
        assertEquals(1000, activity.cost)
        assertEquals(12, activity.maxParticipants)
        assertEquals("Bring sunscreen", activity.notes)
        assertEquals("scenario-1", activity.scenarioId)
    }
    
    @Test
    fun createActivityTrimsNameDescriptionLocationAndNotes() {
        val request = ActivityRequest(
            name = "  Hiking  ",
            description = "  Great trail  ",
            duration = 120,
            location = "  Mountain  ",
            organizerId = "user-1",
            notes = "  Important  "
        )
        
        val activity = ActivityManager.createActivity("event-1", request)
        
        assertEquals("Hiking", activity.name)
        assertEquals("Great trail", activity.description)
        assertEquals("Mountain", activity.location)
        assertEquals("Important", activity.notes)
    }
    
    @Test
    fun `createActivity rejects blank name`() {
        val request = ActivityRequest(
            name = "   ",
            description = "Description",
            duration = 60,
            organizerId = "user-1"
        )
        
        assertFailsWith<IllegalArgumentException> {
            ActivityManager.createActivity("event-1", request)
        }
    }
    
    @Test
    fun `createActivity rejects blank description`() {
        val request = ActivityRequest(
            name = "Activity",
            description = "   ",
            duration = 60,
            organizerId = "user-1"
        )
        
        assertFailsWith<IllegalArgumentException> {
            ActivityManager.createActivity("event-1", request)
        }
    }
    
    @Test
    fun `createActivity rejects invalid duration`() {
        val request = ActivityRequest(
            name = "Activity",
            description = "Description",
            duration = 0,
            organizerId = "user-1"
        )
        
        assertFailsWith<IllegalArgumentException> {
            ActivityManager.createActivity("event-1", request)
        }
    }
    
    @Test
    fun `createActivity rejects negative cost`() {
        val request = ActivityRequest(
            name = "Activity",
            description = "Description",
            duration = 60,
            cost = -100,
            organizerId = "user-1"
        )
        
        assertFailsWith<IllegalArgumentException> {
            ActivityManager.createActivity("event-1", request)
        }
    }
    
    @Test
    fun `createActivity rejects invalid maxParticipants`() {
        val requestTooLow = ActivityRequest(
            name = "Activity",
            description = "Description",
            duration = 60,
            maxParticipants = 0,
            organizerId = "user-1"
        )
        
        assertFailsWith<IllegalArgumentException> {
            ActivityManager.createActivity("event-1", requestTooLow)
        }
        
        val requestTooHigh = ActivityRequest(
            name = "Activity",
            description = "Description",
            duration = 60,
            maxParticipants = 1001,
            organizerId = "user-1"
        )
        
        assertFailsWith<IllegalArgumentException> {
            ActivityManager.createActivity("event-1", requestTooHigh)
        }
    }
    
    @Test
    fun `registerParticipant adds participant successfully`() {
        val request = ActivityRequest(
            name = "Hike",
            description = "Mountain hike",
            duration = 180,
            maxParticipants = 10,
            organizerId = "user-1"
        )
        
        val activity = ActivityManager.createActivity("event-1", request)
        val result = ActivityManager.registerParticipant(activity, "user-2", "Excited to join!")
        
        assertTrue(result is RegistrationResult.Success)
        val success = result as RegistrationResult.Success
        
        assertTrue(success.activity.registeredParticipantIds.contains("user-2"))
        assertEquals(1, success.activity.registeredParticipantIds.size)
        assertEquals("user-2", success.registration.participantId)
        assertEquals(activity.id, success.registration.activityId)
        assertEquals("Excited to join!", success.registration.notes)
    }
    
    @Test
    fun `registerParticipant prevents duplicate registration`() {
        val request = ActivityRequest(
            name = "Hike",
            description = "Mountain hike",
            duration = 180,
            organizerId = "user-1"
        )
        
        val activity = ActivityManager.createActivity("event-1", request)
        val result1 = ActivityManager.registerParticipant(activity, "user-2")
        assertTrue(result1 is RegistrationResult.Success)
        
        val updatedActivity = (result1 as RegistrationResult.Success).activity
        val result2 = ActivityManager.registerParticipant(updatedActivity, "user-2")
        
        assertTrue(result2 is RegistrationResult.AlreadyRegistered)
    }
    
    @Test
    fun `registerParticipant prevents registration when full`() {
        val request = ActivityRequest(
            name = "Small Group",
            description = "Limited capacity",
            duration = 60,
            maxParticipants = 2,
            organizerId = "user-1"
        )
        
        val activity = ActivityManager.createActivity("event-1", request)
        
        val result1 = ActivityManager.registerParticipant(activity, "user-2")
        assertTrue(result1 is RegistrationResult.Success)
        
        val activity2 = (result1 as RegistrationResult.Success).activity
        val result2 = ActivityManager.registerParticipant(activity2, "user-3")
        assertTrue(result2 is RegistrationResult.Success)
        
        val activity3 = (result2 as RegistrationResult.Success).activity
        val result3 = ActivityManager.registerParticipant(activity3, "user-4")
        
        assertTrue(result3 is RegistrationResult.Full)
    }
    
    @Test
    fun `registerParticipant allows unlimited capacity when maxParticipants is null`() {
        val request = ActivityRequest(
            name = "Open Activity",
            description = "Everyone welcome",
            duration = 60,
            maxParticipants = null,
            organizerId = "user-1"
        )
        
        var activity = ActivityManager.createActivity("event-1", request)
        
        // Register many participants
        for (i in 1..100) {
            val result = ActivityManager.registerParticipant(activity, "user-$i")
            assertTrue(result is RegistrationResult.Success)
            activity = (result as RegistrationResult.Success).activity
        }
        
        assertEquals(100, activity.registeredParticipantIds.size)
    }
    
    @Test
    fun `unregisterParticipant removes participant`() {
        val request = ActivityRequest(
            name = "Hike",
            description = "Mountain hike",
            duration = 180,
            organizerId = "user-1"
        )
        
        val activity = ActivityManager.createActivity("event-1", request)
        val result = ActivityManager.registerParticipant(activity, "user-2")
        val activityWithParticipant = (result as RegistrationResult.Success).activity
        
        val activityAfterUnregister = ActivityManager.unregisterParticipant(activityWithParticipant, "user-2")
        
        assertFalse(activityAfterUnregister.registeredParticipantIds.contains("user-2"))
        assertEquals(0, activityAfterUnregister.registeredParticipantIds.size)
    }
    
    @Test
    fun `checkCapacity returns true when not full`() {
        val request = ActivityRequest(
            name = "Activity",
            description = "Description",
            duration = 60,
            maxParticipants = 5,
            organizerId = "user-1"
        )
        
        val activity = ActivityManager.createActivity("event-1", request)
        assertTrue(ActivityManager.checkCapacity(activity))
        
        val result = ActivityManager.registerParticipant(activity, "user-2")
        val activityWith1 = (result as RegistrationResult.Success).activity
        assertTrue(ActivityManager.checkCapacity(activityWith1))
    }
    
    @Test
    fun `checkCapacity returns false when full`() {
        val request = ActivityRequest(
            name = "Activity",
            description = "Description",
            duration = 60,
            maxParticipants = 1,
            organizerId = "user-1"
        )
        
        val activity = ActivityManager.createActivity("event-1", request)
        val result = ActivityManager.registerParticipant(activity, "user-2")
        val fullActivity = (result as RegistrationResult.Success).activity
        
        assertFalse(ActivityManager.checkCapacity(fullActivity))
    }
    
    @Test
    fun `checkCapacity returns true when unlimited`() {
        val request = ActivityRequest(
            name = "Activity",
            description = "Description",
            duration = 60,
            maxParticipants = null,
            organizerId = "user-1"
        )
        
        val activity = ActivityManager.createActivity("event-1", request)
        assertTrue(ActivityManager.checkCapacity(activity))
    }
    
    @Test
    fun `calculateActivityStats computes correctly with cost`() {
        val request = ActivityRequest(
            name = "Activity",
            description = "Description",
            duration = 60,
            cost = 1500,
            maxParticipants = 10,
            organizerId = "user-1"
        )
        
        var activity = ActivityManager.createActivity("event-1", request)
        
        // Register 3 participants
        for (i in 1..3) {
            val result = ActivityManager.registerParticipant(activity, "user-$i")
            activity = (result as RegistrationResult.Success).activity
        }
        
        val stats = ActivityManager.calculateActivityStats(activity)
        
        assertEquals(3, stats.registeredCount)
        assertEquals(7, stats.spotsRemaining)
        assertFalse(stats.isFull)
        assertEquals(4500, stats.totalCost) // 1500 * 3
    }
    
    @Test
    fun `calculateActivityStats handles unlimited capacity`() {
        val request = ActivityRequest(
            name = "Activity",
            description = "Description",
            duration = 60,
            cost = 1000,
            maxParticipants = null,
            organizerId = "user-1"
        )
        
        var activity = ActivityManager.createActivity("event-1", request)
        
        val result = ActivityManager.registerParticipant(activity, "user-2")
        activity = (result as RegistrationResult.Success).activity
        
        val stats = ActivityManager.calculateActivityStats(activity)
        
        assertEquals(1, stats.registeredCount)
        assertNull(stats.spotsRemaining)
        assertFalse(stats.isFull)
        assertEquals(1000, stats.totalCost)
    }
    
    @Test
    fun `calculateActivityStats handles no cost`() {
        val request = ActivityRequest(
            name = "Free Activity",
            description = "Description",
            duration = 60,
            cost = null,
            organizerId = "user-1"
        )
        
        var activity = ActivityManager.createActivity("event-1", request)
        val result = ActivityManager.registerParticipant(activity, "user-2")
        activity = (result as RegistrationResult.Success).activity
        
        val stats = ActivityManager.calculateActivityStats(activity)
        
        assertEquals(0, stats.totalCost)
    }
    
    @Test
    fun `validateActivity validates name`() {
        // Valid
        val valid = ActivityRequest(
            name = "Valid Name",
            description = "Description",
            duration = 60,
            organizerId = "user-1"
        )
        val validResult = ActivityManager.validateActivity(valid)
        assertTrue(validResult.isValid)
        
        // Blank
        val blank = ActivityRequest(
            name = "   ",
            description = "Description",
            duration = 60,
            organizerId = "user-1"
        )
        val blankResult = ActivityManager.validateActivity(blank)
        assertFalse(blankResult.isValid)
        assertTrue(blankResult.errors.any { it.contains("blank") })
        
        // Too long
        val tooLong = ActivityRequest(
            name = "a".repeat(101),
            description = "Description",
            duration = 60,
            organizerId = "user-1"
        )
        val tooLongResult = ActivityManager.validateActivity(tooLong)
        assertFalse(tooLongResult.isValid)
        assertTrue(tooLongResult.errors.any { it.contains("100 characters") })
    }
    
    @Test
    fun `validateActivity validates time format`() {
        // Valid time
        val validTime = ActivityRequest(
            name = "Activity",
            description = "Description",
            duration = 60,
            time = "14:30",
            organizerId = "user-1"
        )
        val validResult = ActivityManager.validateActivity(validTime)
        assertTrue(validResult.isValid)
        
        // Invalid time
        val invalidTime = ActivityRequest(
            name = "Activity",
            description = "Description",
            duration = 60,
            time = "25:00",
            organizerId = "user-1"
        )
        val invalidResult = ActivityManager.validateActivity(invalidTime)
        assertFalse(invalidResult.isValid)
        assertTrue(invalidResult.errors.any { it.contains("HH:MM") })
    }
    
    @Test
    fun `validateActivity validates date format`() {
        // Valid date
        val validDate = ActivityRequest(
            name = "Activity",
            description = "Description",
            duration = 60,
            date = "2025-12-26",
            organizerId = "user-1"
        )
        val validResult = ActivityManager.validateActivity(validDate)
        assertTrue(validResult.isValid)
        
        // Invalid date
        val invalidDate = ActivityRequest(
            name = "Activity",
            description = "Description",
            duration = 60,
            date = "12/26/2025",
            organizerId = "user-1"
        )
        val invalidResult = ActivityManager.validateActivity(invalidDate)
        assertFalse(invalidResult.isValid)
        assertTrue(invalidResult.errors.any { it.contains("ISO 8601") })
    }
    
    @Test
    fun `groupActivitiesByDate groups correctly`() {
        val activities = listOf(
            Activity(
                id = "1",
                eventId = "event-1",
                name = "Morning Hike",
                description = "Early hike",
                date = "2025-12-26",
                time = "08:00",
                duration = 120,
                cost = 1000,
                registeredParticipantIds = emptyList(),
                organizerId = "user-1",
                createdAt = "2025-12-26T00:00:00Z",
                updatedAt = "2025-12-26T00:00:00Z"
            ),
            Activity(
                id = "2",
                eventId = "event-1",
                name = "Afternoon Activity",
                description = "Fun activity",
                date = "2025-12-26",
                time = "14:00",
                duration = 90,
                cost = 1500,
                registeredParticipantIds = emptyList(),
                organizerId = "user-1",
                createdAt = "2025-12-26T00:00:00Z",
                updatedAt = "2025-12-26T00:00:00Z"
            ),
            Activity(
                id = "3",
                eventId = "event-1",
                name = "Next Day Activity",
                description = "Another day",
                date = "2025-12-27",
                time = "10:00",
                duration = 60,
                cost = 2000,
                registeredParticipantIds = emptyList(),
                organizerId = "user-1",
                createdAt = "2025-12-26T00:00:00Z",
                updatedAt = "2025-12-26T00:00:00Z"
            )
        )
        
        val grouped = ActivityManager.groupActivitiesByDate(activities)
        
        assertEquals(2, grouped.size)
        
        val dec26 = grouped.find { it.date == "2025-12-26" }
        assertNotNull(dec26)
        assertEquals(2, dec26.totalActivities)
        assertEquals(2500, dec26.totalCost)
        assertEquals("Morning Hike", dec26.activities[0].name)
        
        val dec27 = grouped.find { it.date == "2025-12-27" }
        assertNotNull(dec27)
        assertEquals(1, dec27.totalActivities)
        assertEquals(2000, dec27.totalCost)
    }
    
    @Test
    fun `calculateParticipantStats computes correctly`() {
        val activities = listOf(
            Activity(
                id = "1",
                eventId = "event-1",
                name = "Activity 1",
                description = "Description",
                duration = 60,
                cost = 1000,
                registeredParticipantIds = listOf("user-1", "user-2"),
                organizerId = "user-3",
                createdAt = "2025-12-26T00:00:00Z",
                updatedAt = "2025-12-26T00:00:00Z"
            ),
            Activity(
                id = "2",
                eventId = "event-1",
                name = "Activity 2",
                description = "Description",
                duration = 90,
                cost = 1500,
                registeredParticipantIds = listOf("user-1"),
                organizerId = "user-3",
                createdAt = "2025-12-26T00:00:00Z",
                updatedAt = "2025-12-26T00:00:00Z"
            ),
            Activity(
                id = "3",
                eventId = "event-1",
                name = "Activity 3",
                description = "Description",
                duration = 120,
                cost = 2000,
                registeredParticipantIds = listOf("user-2"),
                organizerId = "user-3",
                createdAt = "2025-12-26T00:00:00Z",
                updatedAt = "2025-12-26T00:00:00Z"
            )
        )
        
        val stats = ActivityManager.calculateParticipantStats(activities, "user-1")
        
        assertEquals("user-1", stats.participantId)
        assertEquals(2, stats.registeredCount)
        assertEquals(2500, stats.totalCost) // 1000 + 1500
        assertEquals(2, stats.activityNames.size)
        assertTrue(stats.activityNames.contains("Activity 1"))
        assertTrue(stats.activityNames.contains("Activity 2"))
    }
}
