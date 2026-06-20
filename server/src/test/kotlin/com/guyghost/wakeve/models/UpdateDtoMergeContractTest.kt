package com.guyghost.wakeve.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class UpdateDtoMergeContractTest {
    @Test
    fun `activity update preserves fields omitted by the request`() {
        val existing = Activity(
            id = "activity-1",
            eventId = "event-1",
            scenarioId = "scenario-1",
            name = "Kayak",
            description = "River trip",
            date = "2026-07-01",
            time = "10:30",
            duration = 90,
            location = "River",
            cost = 2_500,
            maxParticipants = 8,
            registeredParticipantIds = listOf("user-1", "user-2"),
            organizerId = "organizer-1",
            notes = "Bring shoes",
            createdAt = "2026-06-01T10:00:00Z",
            updatedAt = "2026-06-01T10:00:00Z"
        )

        val updated = UpdateActivityRequest(name = "Kayak sunset").applyTo(existing)

        assertEquals("activity-1", updated.id)
        assertEquals("event-1", updated.eventId)
        assertEquals("scenario-1", updated.scenarioId)
        assertEquals("Kayak sunset", updated.name)
        assertEquals("River trip", updated.description)
        assertEquals("2026-07-01", updated.date)
        assertEquals("10:30", updated.time)
        assertEquals(90, updated.duration)
        assertEquals("River", updated.location)
        assertEquals(2_500, updated.cost)
        assertEquals(8, updated.maxParticipants)
        assertEquals(listOf("user-1", "user-2"), updated.registeredParticipantIds)
        assertEquals("organizer-1", updated.organizerId)
        assertEquals("Bring shoes", updated.notes)
        assertEquals("2026-06-01T10:00:00Z", updated.createdAt)
        assertNotEquals(existing.updatedAt, updated.updatedAt)
    }

    @Test
    fun `equipment update preserves fields omitted by the request`() {
        val existing = EquipmentItem(
            id = "item-1",
            eventId = "event-1",
            name = "Tent",
            category = EquipmentCategory.CAMPING,
            quantity = 2,
            assignedTo = "user-1",
            status = ItemStatus.ASSIGNED,
            sharedCost = 12_000,
            notes = "Four-person tent",
            createdAt = "2026-06-01T10:00:00Z",
            updatedAt = "2026-06-01T10:00:00Z"
        )

        val updated = UpdateEquipmentItemRequest(quantity = 3).applyTo(existing)

        assertEquals("item-1", updated.id)
        assertEquals("event-1", updated.eventId)
        assertEquals("Tent", updated.name)
        assertEquals(EquipmentCategory.CAMPING, updated.category)
        assertEquals(3, updated.quantity)
        assertEquals("user-1", updated.assignedTo)
        assertEquals(ItemStatus.ASSIGNED, updated.status)
        assertEquals(12_000, updated.sharedCost)
        assertEquals("Four-person tent", updated.notes)
        assertEquals("2026-06-01T10:00:00Z", updated.createdAt)
        assertNotEquals(existing.updatedAt, updated.updatedAt)
    }
}
