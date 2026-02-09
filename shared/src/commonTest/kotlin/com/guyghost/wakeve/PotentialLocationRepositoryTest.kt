package com.guyghost.wakeve

import com.guyghost.wakeve.models.*
import com.guyghost.wakeve.test.createTestEvent
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class PotentialLocationRepositoryTest {
    
    private lateinit var eventRepository: EventRepository
    private lateinit var locationRepository: PotentialLocationRepository
    
    @BeforeTest
    fun setup() {
        eventRepository = EventRepository()
        locationRepository = PotentialLocationRepository(eventRepository)
    }
    
    @Test
    fun `addLocation succeeds for event in DRAFT status`() = runTest {
        // Create event in DRAFT status
        val event = createTestEvent(status = EventStatus.DRAFT)
        eventRepository.createEvent(event)
        
        // Add location
        val location = PotentialLocation(
            id = "loc-1",
            eventId = event.id,
            name = "Paris",
            locationType = LocationType.CITY,
            createdAt = "2025-12-31T10:00:00Z"
        )
        
        val result = locationRepository.addLocation(event.id, location)
        
        assertTrue(result.isSuccess)
        assertEquals(location, result.getOrNull())
    }
    
    @Test
    fun `addLocation fails for event not in DRAFT status`() = runTest {
        // Create event in POLLING status
        val event = createTestEvent(status = EventStatus.POLLING)
        eventRepository.createEvent(event)
        
        // Try to add location
        val location = PotentialLocation(
            id = "loc-1",
            eventId = event.id,
            name = "Paris",
            locationType = LocationType.CITY,
            createdAt = "2025-12-31T10:00:00Z"
        )
        
        val result = locationRepository.addLocation(event.id, location)
        
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertIs<IllegalStateException>(exception)
        assertTrue(exception.message?.contains("DRAFT", ignoreCase = true) == true)
    }
    
    @Test
    fun `addLocation fails for non-existent event`() = runTest {
        val location = PotentialLocation(
            id = "loc-1",
            eventId = "non-existent-event",
            name = "Paris",
            locationType = LocationType.CITY,
            createdAt = "2025-12-31T10:00:00Z"
        )
        
        val result = locationRepository.addLocation("non-existent-event", location)
        
        assertTrue(result.isFailure)
        assertIs<IllegalArgumentException>(result.exceptionOrNull())
    }
    
    @Test
    fun `addLocation fails if eventId mismatch`() = runTest {
        val event = createTestEvent(status = EventStatus.DRAFT)
        eventRepository.createEvent(event)
        
        val location = PotentialLocation(
            id = "loc-1",
            eventId = "different-event-id", // Mismatch
            name = "Paris",
            locationType = LocationType.CITY,
            createdAt = "2025-12-31T10:00:00Z"
        )
        
        val result = locationRepository.addLocation(event.id, location)
        
        assertTrue(result.isFailure)
    }
    
    @Test
    fun `getLocationsByEventId returns all locations for event`() = runTest {
        val event = createTestEvent(status = EventStatus.DRAFT)
        eventRepository.createEvent(event)
        
        // Add multiple locations
        val loc1 = PotentialLocation(
            id = "loc-1",
            eventId = event.id,
            name = "Paris",
            locationType = LocationType.CITY,
            createdAt = "2025-12-31T10:00:00Z"
        )
        val loc2 = PotentialLocation(
            id = "loc-2",
            eventId = event.id,
            name = "Berlin",
            locationType = LocationType.CITY,
            createdAt = "2025-12-31T11:00:00Z"
        )
        
        locationRepository.addLocation(event.id, loc1)
        locationRepository.addLocation(event.id, loc2)
        
        val locations = locationRepository.getLocationsByEventId(event.id)
        
        assertEquals(2, locations.size)
        assertTrue(locations.any { it.name == "Paris" })
        assertTrue(locations.any { it.name == "Berlin" })
    }
    
    @Test
    fun `getLocationsByEventId returns empty list for event with no locations`() = runTest {
        val event = createTestEvent(status = EventStatus.DRAFT)
        eventRepository.createEvent(event)
        
        val locations = locationRepository.getLocationsByEventId(event.id)
        
        assertTrue(locations.isEmpty())
    }
    
    @Test
    fun `removeLocation succeeds for event in DRAFT status`() = runTest {
        val event = createTestEvent(status = EventStatus.DRAFT)
        eventRepository.createEvent(event)
        
        val location = PotentialLocation(
            id = "loc-1",
            eventId = event.id,
            name = "Paris",
            locationType = LocationType.CITY,
            createdAt = "2025-12-31T10:00:00Z"
        )
        locationRepository.addLocation(event.id, location)
        
        // Remove location
        val result = locationRepository.removeLocation(event.id, location.id)
        
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)
        
        // Verify it's gone
        val locations = locationRepository.getLocationsByEventId(event.id)
        assertTrue(locations.isEmpty())
    }
    
    @Test
    fun `removeLocation fails for event not in DRAFT status`() = runTest {
        val event = createTestEvent(status = EventStatus.DRAFT)
        eventRepository.createEvent(event)
        
        val location = PotentialLocation(
            id = "loc-1",
            eventId = event.id,
            name = "Paris",
            locationType = LocationType.CITY,
            createdAt = "2025-12-31T10:00:00Z"
        )
        locationRepository.addLocation(event.id, location)
        
        // Change event status to POLLING
        eventRepository.updateEventStatus(event.id, EventStatus.POLLING, null)
        
        // Try to remove location
        val result = locationRepository.removeLocation(event.id, location.id)
        
        assertTrue(result.isFailure)
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }
    
    @Test
    fun `removeLocation fails for non-existent location`() = runTest {
        val event = createTestEvent(status = EventStatus.DRAFT)
        eventRepository.createEvent(event)
        
        val result = locationRepository.removeLocation(event.id, "non-existent-loc")
        
        assertTrue(result.isFailure)
    }
    
    @Test
    fun `getLocationById returns correct location`() = runTest {
        val event = createTestEvent(status = EventStatus.DRAFT)
        eventRepository.createEvent(event)
        
        val location = PotentialLocation(
            id = "loc-1",
            eventId = event.id,
            name = "Paris",
            locationType = LocationType.CITY,
            createdAt = "2025-12-31T10:00:00Z"
        )
        locationRepository.addLocation(event.id, location)
        
        val found = locationRepository.getLocationById("loc-1")
        
        assertNotNull(found)
        assertEquals("Paris", found.name)
    }
    
    @Test
    fun `getLocationById returns null for non-existent location`() = runTest {
        val found = locationRepository.getLocationById("non-existent")
        
        assertNull(found)
    }
    
}
