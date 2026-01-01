package com.guyghost.wakeve.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TimeSlotValidationTest {
    
    @Test
    fun `SPECIFIC timeOfDay with start and end passes validation`() {
        val slot = TimeSlot(
            id = "slot-1",
            start = "2025-06-15T14:00:00Z",
            end = "2025-06-15T18:00:00Z",
            timezone = "Europe/Paris",
            timeOfDay = TimeOfDay.SPECIFIC
        )
        
        assertNull(slot.validate(), "Valid specific time slot should pass validation")
    }
    
    @Test
    fun `SPECIFIC timeOfDay without start fails validation`() {
        val slot = TimeSlot(
            id = "slot-1",
            start = null,
            end = "2025-06-15T18:00:00Z",
            timezone = "Europe/Paris",
            timeOfDay = TimeOfDay.SPECIFIC
        )
        
        val error = slot.validate()
        assertNotNull(error, "Should have validation error")
        assert(error.contains("specific", ignoreCase = true))
    }
    
    @Test
    fun `SPECIFIC timeOfDay without end fails validation`() {
        val slot = TimeSlot(
            id = "slot-1",
            start = "2025-06-15T14:00:00Z",
            end = null,
            timezone = "Europe/Paris",
            timeOfDay = TimeOfDay.SPECIFIC
        )
        
        val error = slot.validate()
        assertNotNull(error, "Should have validation error")
    }
    
    @Test
    fun `ALL_DAY without start and end passes validation`() {
        val slot = TimeSlot(
            id = "slot-1",
            start = null,
            end = null,
            timezone = "Europe/Paris",
            timeOfDay = TimeOfDay.ALL_DAY
        )
        
        assertNull(slot.validate(), "ALL_DAY slots don't require specific times")
    }
    
    @Test
    fun `AFTERNOON without start and end passes validation`() {
        val slot = TimeSlot(
            id = "slot-1",
            start = null,
            end = null,
            timezone = "Europe/Paris",
            timeOfDay = TimeOfDay.AFTERNOON
        )
        
        assertNull(slot.validate(), "AFTERNOON slots don't require specific times")
    }
    
    @Test
    fun `default timeOfDay is SPECIFIC`() {
        val slot = TimeSlot(
            id = "slot-1",
            start = "2025-06-15T14:00:00Z",
            end = "2025-06-15T18:00:00Z",
            timezone = "Europe/Paris"
        )
        
        assertEquals(TimeOfDay.SPECIFIC, slot.timeOfDay, "Default timeOfDay should be SPECIFIC")
    }
}

class PotentialLocationTest {
    
    @Test
    fun `PotentialLocation with valid data is created correctly`() {
        val location = PotentialLocation(
            id = "loc-1",
            eventId = "event-1",
            name = "Paris",
            locationType = LocationType.CITY,
            address = null,
            coordinates = null,
            createdAt = "2025-12-31T10:00:00Z"
        )
        
        assertEquals("Paris", location.name)
        assertEquals(LocationType.CITY, location.locationType)
        assertNull(location.address)
        assertNull(location.coordinates)
    }
    
    @Test
    fun `PotentialLocation with coordinates is created correctly`() {
        val coords = Coordinates(48.8566, 2.3522)
        val location = PotentialLocation(
            id = "loc-1",
            eventId = "event-1",
            name = "Eiffel Tower",
            locationType = LocationType.SPECIFIC_VENUE,
            address = "Champ de Mars, Paris",
            coordinates = coords,
            createdAt = "2025-12-31T10:00:00Z"
        )
        
        assertEquals("Eiffel Tower", location.name)
        assertEquals(LocationType.SPECIFIC_VENUE, location.locationType)
        assertEquals("Champ de Mars, Paris", location.address)
        assertNotNull(location.coordinates)
        assertEquals(48.8566, location.coordinates?.latitude)
        assertEquals(2.3522, location.coordinates?.longitude)
    }
}

class CoordinatesTest {
    
    @Test
    fun `Coordinates toJson formats correctly`() {
        val coords = Coordinates(48.8566, 2.3522)
        val json = coords.toJson()
        
        assert(json.contains("48.8566"))
        assert(json.contains("2.3522"))
        assert(json.contains("latitude"))
        assert(json.contains("longitude"))
    }
    
    @Test
    fun `Coordinates fromJson parses correctly`() {
        val json = """{"latitude":48.8566,"longitude":2.3522}"""
        val coords = Coordinates.fromJson(json)
        
        assertNotNull(coords)
        assertEquals(48.8566, coords.latitude, 0.0001)
        assertEquals(2.3522, coords.longitude, 0.0001)
    }
    
    @Test
    fun `Coordinates fromJson with spaces parses correctly`() {
        val json = """{"latitude": 48.8566, "longitude": 2.3522}"""
        val coords = Coordinates.fromJson(json)
        
        assertNotNull(coords)
        assertEquals(48.8566, coords.latitude, 0.0001)
    }
    
    @Test
    fun `Coordinates fromJson with invalid JSON returns null`() {
        val json = """invalid json"""
        val coords = Coordinates.fromJson(json)
        
        assertNull(coords)
    }
    
    @Test
    fun `Coordinates validates latitude range`() {
        try {
            Coordinates(100.0, 0.0) // Invalid: lat > 90
            throw AssertionError("Should have thrown exception for invalid latitude")
        } catch (e: IllegalArgumentException) {
            // Expected
        }
    }
    
    @Test
    fun `Coordinates validates longitude range`() {
        try {
            Coordinates(0.0, 200.0) // Invalid: lng > 180
            throw AssertionError("Should have thrown exception for invalid longitude")
        } catch (e: IllegalArgumentException) {
            // Expected
        }
    }
}
