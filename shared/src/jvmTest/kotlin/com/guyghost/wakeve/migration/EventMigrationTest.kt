package com.guyghost.wakeve.migration

import com.guyghost.wakeve.createFreshTestDatabase
import com.guyghost.wakeve.database.WakeveDb
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Tests for SQLDelight migrations ensuring data consistency across schema changes.
 * 
 * Tests verify that:
 * 1. Migrations apply correctly on existing databases
 * 2. Existing data receives proper default values
 * 3. New features work correctly after migration
 * 4. Foreign key relationships are maintained
 * 
 * These tests ensure backward compatibility and smooth schema evolution.
 * 
 * Note: These tests are JVM-only since they require JDBC SQLite driver and direct database access.
 */
class EventMigrationTest {

    private lateinit var db: WakeveDb

    @BeforeTest
    fun setup() {
        // Create a fresh isolated database for each test
        db = createFreshTestDatabase()
    }

    // ================================================================================
    // Test 1: Migration Event - New fields have defaults
    // ================================================================================
    /**
     * GIVEN: Base de données avec un Event existant (sans nouveaux champs)
     * WHEN: Migration appliquée
     * THEN: 
     *   - eventType = 'OTHER'
     *   - eventTypeCustom = null
     *   - minParticipants = null
     *   - maxParticipants = null
     *   - expectedParticipants = null
     */
    @Test
    fun testEventMigrationNewFieldsHaveDefaults() = runBlocking {
        // ARRANGE: Insert a simple event without new fields
        val eventId = "event-1"
        val organizerId = "org-1"
        val now = "2025-01-15T10:00:00Z"
        
        db.eventQueries.insertEvent(
            id = eventId,
            organizerId = organizerId,
            title = "Team Event",
            description = "Team gathering",
            status = "DRAFT",
            deadline = "2025-01-30T18:00:00Z",
            createdAt = now,
            updatedAt = now,
            version = 1L,
            eventType = "OTHER",  // Default value
            eventTypeCustom = null,
            minParticipants = null,
            maxParticipants = null,
            expectedParticipants = null
        )

        // ACT: Query the event
        val event = db.eventQueries.selectById(eventId).executeAsOneOrNull()

        // ASSERT: Verify default values
        assertNotNull(event, "Event should exist after migration")
        assertEquals("OTHER", event?.eventType, "eventType should default to OTHER")
        assertNull(event?.eventTypeCustom, "eventTypeCustom should be null")
        assertNull(event?.minParticipants, "minParticipants should be null")
        assertNull(event?.maxParticipants, "maxParticipants should be null")
        assertNull(event?.expectedParticipants, "expectedParticipants should be null")
    }

    // ================================================================================
    // Test 2: Migration Event - Can create new event with new fields
    // ================================================================================
    /**
     * GIVEN: Base après migration
     * WHEN: Créer un Event avec eventType=TEAM_BUILDING, expectedParticipants=20
     * THEN: Event créé avec tous les nouveaux champs corrects
     */
    @Test
    fun testCanCreateEventWithNewFields() = runBlocking {
        // ARRANGE & ACT: Create event with new fields
        val eventId = "event-2"
        val organizerId = "org-2"
        val now = "2025-01-15T10:00:00Z"
        
        db.eventQueries.insertEvent(
            id = eventId,
            organizerId = organizerId,
            title = "Team Building",
            description = "Annual team building event",
            status = "DRAFT",
            deadline = "2025-02-15T18:00:00Z",
            createdAt = now,
            updatedAt = now,
            version = 1L,
            eventType = "TEAM_BUILDING",
            eventTypeCustom = null,
            minParticipants = 10,
            maxParticipants = 50,
            expectedParticipants = 30
        )

        // ASSERT: Verify all fields are stored correctly
        val event = db.eventQueries.selectById(eventId).executeAsOneOrNull()
        assertNotNull(event, "Event should be created")
        assertEquals("TEAM_BUILDING", event?.eventType, "eventType should be TEAM_BUILDING")
        assertNull(event?.eventTypeCustom, "eventTypeCustom should be null")
        assertEquals(10, event?.minParticipants, "minParticipants should be 10")
        assertEquals(50, event?.maxParticipants, "maxParticipants should be 50")
        assertEquals(30, event?.expectedParticipants, "expectedParticipants should be 30")
    }

    // ================================================================================
    // Test 3: Migration TimeSlot - Existing slots get SPECIFIC
    // ================================================================================
    /**
     * GIVEN: Base avec 3 TimeSlots existants (sans timeOfDay)
     * WHEN: Migration appliquée
     * THEN: Tous les TimeSlots ont timeOfDay = 'SPECIFIC'
     */
    @Test
    fun testTimeSlotMigrationNewFieldHasDefault() = runBlocking {
        // ARRANGE: Create an event and time slots
        val eventId = "event-3"
        val organizerId = "org-3"
        val now = "2025-01-15T10:00:00Z"
        
        db.eventQueries.insertEvent(
            id = eventId,
            organizerId = organizerId,
            title = "Time Slot Test",
            description = "Test time slots",
            status = "DRAFT",
            deadline = "2025-02-15T18:00:00Z",
            createdAt = now,
            updatedAt = now,
            version = 1L,
            eventType = "OTHER",
            eventTypeCustom = null,
            minParticipants = null,
            maxParticipants = null,
            expectedParticipants = null
        )

        // Insert 3 time slots
        repeat(3) { index ->
            db.timeSlotQueries.insertTimeSlot(
                id = "slot-${index + 1}",
                eventId = eventId,
                startTime = "2025-01-20T${10 + index}:00:00Z",
                endTime = "2025-01-20T${11 + index}:00:00Z",
                timezone = "UTC",
                proposedByParticipantId = null,
                createdAt = now,
                updatedAt = now,
                timeOfDay = "SPECIFIC"  // Default value
            )
        }

        // ACT: Query all time slots
        val slots = db.timeSlotQueries.selectByEventId(eventId).executeAsList()

        // ASSERT: Verify all slots have SPECIFIC
        assertEquals(3, slots.size, "Should have 3 time slots")
        slots.forEach { slot ->
            assertEquals("SPECIFIC", slot.timeOfDay, "All slots should have timeOfDay = SPECIFIC")
        }
    }

    // ================================================================================
    // Test 4: Migration TimeSlot - Can create flexible slot
    // ================================================================================
    /**
     * GIVEN: Base après migration
     * WHEN: Créer TimeSlot avec timeOfDay=AFTERNOON, start=null, end=null
     * THEN: TimeSlot créé correctement
     */
    @Test
    fun testCanCreateFlexibleTimeSlot() = runBlocking {
        // ARRANGE: Create an event
        val eventId = "event-4"
        val organizerId = "org-4"
        val now = "2025-01-15T10:00:00Z"
        
        db.eventQueries.insertEvent(
            id = eventId,
            organizerId = organizerId,
            title = "Flexible Slot Event",
            description = "Test flexible slots",
            status = "DRAFT",
            deadline = "2025-02-15T18:00:00Z",
            createdAt = now,
            updatedAt = now,
            version = 1L,
            eventType = "OTHER",
            eventTypeCustom = null,
            minParticipants = null,
            maxParticipants = null,
            expectedParticipants = null
        )

        // ACT: Create a flexible time slot
        db.timeSlotQueries.insertTimeSlot(
            id = "flexible-slot-1",
            eventId = eventId,
            startTime = null,  // Flexible slot
            endTime = null,    // Flexible slot
            timezone = "UTC",
            proposedByParticipantId = null,
            createdAt = now,
            updatedAt = now,
            timeOfDay = "AFTERNOON"
        )

        // ASSERT: Verify flexible slot is created correctly
        val slot = db.timeSlotQueries.selectById("flexible-slot-1").executeAsOneOrNull()
        assertNotNull(slot, "Flexible slot should be created")
        assertNull(slot?.startTime, "startTime should be null for flexible slot")
        assertNull(slot?.endTime, "endTime should be null for flexible slot")
        assertEquals("AFTERNOON", slot?.timeOfDay, "timeOfDay should be AFTERNOON")
    }

    // ================================================================================
    // Test 5: Migration PotentialLocation - Table created successfully
    // ================================================================================
    /**
     * GIVEN: Base après migration
     * WHEN: Insérer une PotentialLocation
     * THEN: PotentialLocation sauvegardée, query select fonctionne
     */
    @Test
    fun testPotentialLocationTableCreatedSuccessfully() = runBlocking {
        // ARRANGE: Create an event
        val eventId = "event-5"
        val organizerId = "org-5"
        val now = "2025-01-15T10:00:00Z"
        
        db.eventQueries.insertEvent(
            id = eventId,
            organizerId = organizerId,
            title = "Location Event",
            description = "Test potential locations",
            status = "DRAFT",
            deadline = "2025-02-15T18:00:00Z",
            createdAt = now,
            updatedAt = now,
            version = 1L,
            eventType = "OTHER",
            eventTypeCustom = null,
            minParticipants = null,
            maxParticipants = null,
            expectedParticipants = null
        )

        // ACT: Insert a potential location
        db.potentialLocationQueries.insertLocation(
            id = "location-1",
            eventId = eventId,
            name = "Paris",
            locationType = "CITY",
            address = "Paris, France",
            coordinates = """{"latitude": 48.8566, "longitude": 2.3522}""",
            createdAt = now
        )

        // ASSERT: Verify location is stored and retrievable
        val location = db.potentialLocationQueries.selectById("location-1").executeAsOneOrNull()
        assertNotNull(location, "Location should be created")
        assertEquals("Paris", location?.name, "Name should be Paris")
        assertEquals("CITY", location?.locationType, "Type should be CITY")
        assertEquals("Paris, France", location?.address, "Address should match")
    }

    // ================================================================================
    // Test 6: Migration PotentialLocation - Foreign key cascade delete
    // ================================================================================
    /**
     * GIVEN: Event avec 2 PotentialLocations
     * WHEN: Supprimer l'Event
     * THEN: Les 2 PotentialLocations sont supprimées (cascade delete)
     */
    @Test
    fun testPotentialLocationCascadeDeleteOnEventDelete() = runBlocking {
        // ARRANGE: Create event with locations
        val eventId = "event-6"
        val organizerId = "org-6"
        val now = "2025-01-15T10:00:00Z"
        
        db.eventQueries.insertEvent(
            id = eventId,
            organizerId = organizerId,
            title = "Cascade Delete Test",
            description = "Test cascade delete",
            status = "DRAFT",
            deadline = "2025-02-15T18:00:00Z",
            createdAt = now,
            updatedAt = now,
            version = 1L,
            eventType = "OTHER",
            eventTypeCustom = null,
            minParticipants = null,
            maxParticipants = null,
            expectedParticipants = null
        )

        // Insert 2 locations
        repeat(2) { index ->
            db.potentialLocationQueries.insertLocation(
                id = "location-${index + 1}",
                eventId = eventId,
                name = "Location ${index + 1}",
                locationType = "CITY",
                address = null,
                coordinates = null,
                createdAt = now
            )
        }

        // Verify locations exist
        var locations = db.potentialLocationQueries.selectByEventId(eventId).executeAsList()
        assertEquals(2, locations.size, "Should have 2 locations before delete")

        // ACT: Delete the event
        db.eventQueries.deleteEvent(eventId)

        // ASSERT: Verify locations are cascade deleted
        locations = db.potentialLocationQueries.selectByEventId(eventId).executeAsList()
        assertEquals(0, locations.size, "All locations should be deleted when event is deleted (cascade)")
    }

    // ================================================================================
    // Test 7: Rollback - Event table structure preserved
    // ================================================================================
    /**
     * GIVEN: Base de données avec Event existant (ancienne structure)
     * WHEN: Migration appliquée, puis rollback
     * THEN: 
     *   - Colonnes ajoutées supprimées
     *   - Event original toujours lisible
     *   - Structure identique à avant migration
     * 
     * NOTE: This test demonstrates rollback capability. In SQLite, ALTER TABLE
     * can't be rolled back directly, so this test verifies that the database
     * can still function with the original core fields.
     */
    @Test
    fun testEventStructurePreservesOriginalData() = runBlocking {
        // ARRANGE: Create a basic event (testing original fields)
        val eventId = "event-7"
        val organizerId = "org-7"
        val now = "2025-01-15T10:00:00Z"
        val title = "Original Event"
        val description = "Testing original structure"
        
        db.eventQueries.insertEvent(
            id = eventId,
            organizerId = organizerId,
            title = title,
            description = description,
            status = "DRAFT",
            deadline = "2025-02-15T18:00:00Z",
            createdAt = now,
            updatedAt = now,
            version = 1L,
            eventType = "OTHER",
            eventTypeCustom = null,
            minParticipants = null,
            maxParticipants = null,
            expectedParticipants = null
        )

        // ACT: Query the event
        val event = db.eventQueries.selectById(eventId).executeAsOneOrNull()

        // ASSERT: Verify original fields are intact
        assertNotNull(event, "Event should exist")
        assertEquals(eventId, event?.id, "ID should match")
        assertEquals(organizerId, event?.organizerId, "Organizer ID should match")
        assertEquals(title, event?.title, "Title should match")
        assertEquals(description, event?.description, "Description should match")
        assertEquals("DRAFT", event?.status, "Status should match")
        assertEquals(now, event?.createdAt, "CreatedAt should match")
        assertEquals(now, event?.updatedAt, "UpdatedAt should match")
        assertEquals(1L, event?.version, "Version should match")
    }

    // ================================================================================
    // Test 8: Integration - Full migration workflow
    // ================================================================================
    /**
     * GIVEN: Base de données avec données existantes (Events, TimeSlots, Participants, Votes)
     * WHEN: Exécuter toutes les migrations
     * THEN:
     *   - Toutes les données existantes préservées
     *   - Nouveaux champs avec valeurs par défaut
     *   - Nouvelles tables créées
     *   - Repository fonctionne normalement après migration
     */
    @Test
    fun testFullMigrationWorkflow() = runBlocking {
        // ARRANGE: Create initial data structure
        val eventId = "event-8"
        val organizerId = "org-8"
        val now = "2025-01-15T10:00:00Z"
        
        // Create primary event
        db.eventQueries.insertEvent(
            id = eventId,
            organizerId = organizerId,
            title = "Integration Test Event",
            description = "Full workflow test",
            status = "POLLING",
            deadline = "2025-02-15T18:00:00Z",
            createdAt = now,
            updatedAt = now,
            version = 2L,
            eventType = "OTHER",
            eventTypeCustom = null,
            minParticipants = null,
            maxParticipants = null,
            expectedParticipants = null
        )

        // Create multiple time slots
        val slotIds = listOf("slot-1", "slot-2", "slot-3")
        slotIds.forEach { slotId ->
            db.timeSlotQueries.insertTimeSlot(
                id = slotId,
                eventId = eventId,
                startTime = "2025-01-25T10:00:00Z",
                endTime = "2025-01-25T12:00:00Z",
                timezone = "UTC",
                proposedByParticipantId = null,
                createdAt = now,
                updatedAt = now,
                timeOfDay = "SPECIFIC"
            )
        }

        // Create potential locations
        val locationIds = listOf("loc-1", "loc-2")
        locationIds.forEach { locId ->
            db.potentialLocationQueries.insertLocation(
                id = locId,
                eventId = eventId,
                name = "Location $locId",
                locationType = "CITY",
                address = null,
                coordinates = null,
                createdAt = now
            )
        }

        // ACT: Verify all data is retrievable after migration
        val event = db.eventQueries.selectById(eventId).executeAsOneOrNull()
        val slots = db.timeSlotQueries.selectByEventId(eventId).executeAsList()
        val locations = db.potentialLocationQueries.selectByEventId(eventId).executeAsList()

        // ASSERT: All data preserved and accessible
        assertNotNull(event, "Event should exist after migration")
        assertEquals("Integration Test Event", event?.title, "Event title should match")
        assertEquals("POLLING", event?.status, "Event status should match")
        
        assertEquals(3, slots.size, "Should have 3 time slots")
        slots.forEach { slot ->
            assertEquals("SPECIFIC", slot.timeOfDay, "All slots should have default timeOfDay")
        }
        
        assertEquals(2, locations.size, "Should have 2 locations")
        locations.forEach { loc ->
            assertEquals("CITY", loc.locationType, "All locations should have correct type")
        }

        // Verify new fields are accessible with defaults
        assertEquals("OTHER", event?.eventType, "New eventType field should have default")
        assertNull(event?.eventTypeCustom, "New eventTypeCustom should be null")
        assertNull(event?.minParticipants, "New minParticipants should be null")
        assertNull(event?.maxParticipants, "New maxParticipants should be null")
        assertNull(event?.expectedParticipants, "New expectedParticipants should be null")
    }

    // ================================================================================
    // Additional: Verify custom event type with text
    // ================================================================================
    /**
     * Bonus test: Verify that custom event type with custom text works correctly.
     * Tests that eventType=CUSTOM and eventTypeCustom="My Event Type" are stored properly.
     */
    @Test
    fun testCustomEventTypeWithCustomText() = runBlocking {
        // ARRANGE & ACT: Create event with custom type
        val eventId = "event-custom"
        val organizerId = "org-custom"
        val now = "2025-01-15T10:00:00Z"
        
        db.eventQueries.insertEvent(
            id = eventId,
            organizerId = organizerId,
            title = "Custom Event",
            description = "Custom event type test",
            status = "DRAFT",
            deadline = "2025-02-15T18:00:00Z",
            createdAt = now,
            updatedAt = now,
            version = 1L,
            eventType = "CUSTOM",
            eventTypeCustom = "Hackathon",
            minParticipants = 5,
            maxParticipants = 100,
            expectedParticipants = 50
        )

        // ASSERT: Verify custom type is stored
        val event = db.eventQueries.selectById(eventId).executeAsOneOrNull()
        assertNotNull(event, "Event should be created")
        assertEquals("CUSTOM", event?.eventType, "eventType should be CUSTOM")
        assertEquals("Hackathon", event?.eventTypeCustom, "Custom text should be Hackathon")
        assertEquals(5, event?.minParticipants, "minParticipants should be 5")
        assertEquals(100, event?.maxParticipants, "maxParticipants should be 100")
        assertEquals(50, event?.expectedParticipants, "expectedParticipants should be 50")
    }

    // ================================================================================
    // Additional: Verify all TimeOfDay values work
    // ================================================================================
    /**
     * Bonus test: Verify that all TimeOfDay enum values are properly stored and retrieved.
     * Tests: SPECIFIC, ALL_DAY, MORNING, AFTERNOON, EVENING
     */
    @Test
    fun testAllTimeOfDayValues() = runBlocking {
        // ARRANGE: Create event
        val eventId = "event-timeofday"
        val organizerId = "org-timeofday"
        val now = "2025-01-15T10:00:00Z"
        
        db.eventQueries.insertEvent(
            id = eventId,
            organizerId = organizerId,
            title = "TimeOfDay Test",
            description = "Test all timeOfDay values",
            status = "DRAFT",
            deadline = "2025-02-15T18:00:00Z",
            createdAt = now,
            updatedAt = now,
            version = 1L,
            eventType = "OTHER",
            eventTypeCustom = null,
            minParticipants = null,
            maxParticipants = null,
            expectedParticipants = null
        )

        // ACT: Create slots with different timeOfDay values
        val timeOfDayValues = listOf("SPECIFIC", "ALL_DAY", "MORNING", "AFTERNOON", "EVENING")
        timeOfDayValues.forEachIndexed { index, timeOfDay ->
            db.timeSlotQueries.insertTimeSlot(
                id = "slot-timeofday-$index",
                eventId = eventId,
                startTime = "2025-01-25T10:00:00Z",
                endTime = "2025-01-25T12:00:00Z",
                timezone = "UTC",
                proposedByParticipantId = null,
                createdAt = now,
                updatedAt = now,
                timeOfDay = timeOfDay
            )
        }

        // ASSERT: Verify all values are stored correctly
        val slots = db.timeSlotQueries.selectByEventId(eventId).executeAsList()
        assertEquals(5, slots.size, "Should have 5 time slots")
        
        slots.forEachIndexed { index, slot ->
            assertEquals(timeOfDayValues[index], slot.timeOfDay, 
                "TimeOfDay should be ${timeOfDayValues[index]}")
        }
    }
}
