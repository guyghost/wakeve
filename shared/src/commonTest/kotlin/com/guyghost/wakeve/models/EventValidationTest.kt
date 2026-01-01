package com.guyghost.wakeve.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class EventValidationTest {
    
    @Test
    fun `valid event with all fields passes validation`() {
        val event = Event(
            id = "event-1",
            title = "Team Retreat",
            description = "Annual team building",
            organizerId = "user-1",
            participants = listOf("user-2", "user-3"),
            proposedSlots = listOf(
                TimeSlot(
                    id = "slot-1",
                    start = "2025-06-15T14:00:00Z",
                    end = "2025-06-15T18:00:00Z",
                    timezone = "Europe/Paris",
                    timeOfDay = TimeOfDay.SPECIFIC
                )
            ),
            deadline = "2025-06-01T00:00:00Z",
            status = EventStatus.DRAFT,
            createdAt = "2025-12-31T10:00:00Z",
            updatedAt = "2025-12-31T10:00:00Z",
            eventType = EventType.TEAM_BUILDING,
            minParticipants = 15,
            maxParticipants = 25,
            expectedParticipants = 20
        )
        
        assertNull(event.validate(), "Valid event should pass validation")
    }
    
    @Test
    fun `maxParticipants less than minParticipants fails validation`() {
        val event = Event(
            id = "event-1",
            title = "Test Event",
            description = "Test",
            organizerId = "user-1",
            proposedSlots = emptyList(),
            deadline = "2025-06-01T00:00:00Z",
            status = EventStatus.DRAFT,
            createdAt = "2025-12-31T10:00:00Z",
            updatedAt = "2025-12-31T10:00:00Z",
            minParticipants = 30,
            maxParticipants = 20 // Invalid: max < min
        )
        
        val error = event.validate()
        assertNotNull(error, "Should have validation error")
        assert(error.contains("Maximum participants", ignoreCase = true))
    }
    
    @Test
    fun `eventType CUSTOM without eventTypeCustom fails validation`() {
        val event = Event(
            id = "event-1",
            title = "Custom Event",
            description = "Test",
            organizerId = "user-1",
            proposedSlots = emptyList(),
            deadline = "2025-06-01T00:00:00Z",
            status = EventStatus.DRAFT,
            createdAt = "2025-12-31T10:00:00Z",
            updatedAt = "2025-12-31T10:00:00Z",
            eventType = EventType.CUSTOM,
            eventTypeCustom = null // Invalid: CUSTOM requires custom text
        )
        
        val error = event.validate()
        assertNotNull(error, "Should have validation error")
        assert(error.contains("custom", ignoreCase = true))
    }
    
    @Test
    fun `eventType CUSTOM with eventTypeCustom passes validation`() {
        val event = Event(
            id = "event-1",
            title = "Custom Event",
            description = "Test",
            organizerId = "user-1",
            proposedSlots = emptyList(),
            deadline = "2025-06-01T00:00:00Z",
            status = EventStatus.DRAFT,
            createdAt = "2025-12-31T10:00:00Z",
            updatedAt = "2025-12-31T10:00:00Z",
            eventType = EventType.CUSTOM,
            eventTypeCustom = "Hackathon de robotique"
        )
        
        assertNull(event.validate(), "Valid custom event should pass validation")
    }
    
    @Test
    fun `negative participants counts fail validation`() {
        val event1 = Event(
            id = "event-1",
            title = "Test",
            description = "Test",
            organizerId = "user-1",
            proposedSlots = emptyList(),
            deadline = "2025-06-01T00:00:00Z",
            status = EventStatus.DRAFT,
            createdAt = "2025-12-31T10:00:00Z",
            updatedAt = "2025-12-31T10:00:00Z",
            minParticipants = -5
        )
        
        assertNotNull(event1.validate())
        
        val event2 = Event(
            id = "event-2",
            title = "Test",
            description = "Test",
            organizerId = "user-1",
            proposedSlots = emptyList(),
            deadline = "2025-06-01T00:00:00Z",
            status = EventStatus.DRAFT,
            createdAt = "2025-12-31T10:00:00Z",
            updatedAt = "2025-12-31T10:00:00Z",
            expectedParticipants = 0
        )
        
        assertNotNull(event2.validate())
    }
    
    @Test
    fun `default values are correct`() {
        val event = Event(
            id = "event-1",
            title = "Simple Event",
            description = "Test",
            organizerId = "user-1",
            proposedSlots = emptyList(),
            deadline = "2025-06-01T00:00:00Z",
            status = EventStatus.DRAFT,
            createdAt = "2025-12-31T10:00:00Z",
            updatedAt = "2025-12-31T10:00:00Z"
        )
        
        assertEquals(EventType.OTHER, event.eventType, "Default event type should be OTHER")
        assertNull(event.eventTypeCustom, "Default eventTypeCustom should be null")
        assertNull(event.minParticipants, "Default minParticipants should be null")
        assertNull(event.maxParticipants, "Default maxParticipants should be null")
        assertNull(event.expectedParticipants, "Default expectedParticipants should be null")
    }
}
