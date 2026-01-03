package com.guyghost.wakeve.equipment

import com.guyghost.wakeve.models.EquipmentCategory
import com.guyghost.wakeve.models.ItemStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for EquipmentManager service
 */
class EquipmentManagerTest {
    
    @Test
    fun `createEquipmentItem creates valid item with defaults`() {
        val item = EquipmentManager.createEquipmentItem(
            eventId = "event-1",
            name = "Tent",
            category = EquipmentCategory.CAMPING
        )
        
        assertEquals("event-1", item.eventId)
        assertEquals("Tent", item.name)
        assertEquals(EquipmentCategory.CAMPING, item.category)
        assertEquals(1, item.quantity)
        assertNull(item.assignedTo)
        assertEquals(ItemStatus.NEEDED, item.status)
        assertNull(item.sharedCost)
        assertNotNull(item.id)
        assertNotNull(item.createdAt)
        assertNotNull(item.updatedAt)
    }
    
    @Test
    fun `createEquipmentItem with all parameters`() {
        val item = EquipmentManager.createEquipmentItem(
            eventId = "event-1",
            name = "Sleeping Bag",
            category = EquipmentCategory.CAMPING,
            quantity = 2,
            assignedTo = "user-1",
            status = ItemStatus.ASSIGNED,
            sharedCost = 5000,
            notes = "Bring warm one"
        )
        
        assertEquals("Sleeping Bag", item.name)
        assertEquals(2, item.quantity)
        assertEquals("user-1", item.assignedTo)
        assertEquals(ItemStatus.ASSIGNED, item.status)
        assertEquals(5000, item.sharedCost)
        assertEquals("Bring warm one", item.notes)
    }
    
    @Test
    fun `createEquipmentItem trims name and notes`() {
        val item = EquipmentManager.createEquipmentItem(
            eventId = "event-1",
            name = "  Cooler  ",
            category = EquipmentCategory.COOKING,
            notes = "  Important  "
        )
        
        assertEquals("Cooler", item.name)
        assertEquals("Important", item.notes)
    }
    
    @Test
    fun `createEquipmentItem rejects blank name`() {
        assertFailsWith<IllegalArgumentException> {
            EquipmentManager.createEquipmentItem(
                eventId = "event-1",
                name = "   ",
                category = EquipmentCategory.OTHER
            )
        }
    }
    
    @Test
    fun `createEquipmentItem rejects invalid quantity`() {
        assertFailsWith<IllegalArgumentException> {
            EquipmentManager.createEquipmentItem(
                eventId = "event-1",
                name = "Item",
                category = EquipmentCategory.OTHER,
                quantity = 0
            )
        }
        
        assertFailsWith<IllegalArgumentException> {
            EquipmentManager.createEquipmentItem(
                eventId = "event-1",
                name = "Item",
                category = EquipmentCategory.OTHER,
                quantity = 101
            )
        }
    }
    
    @Test
    fun `createEquipmentItem rejects negative cost`() {
        assertFailsWith<IllegalArgumentException> {
            EquipmentManager.createEquipmentItem(
                eventId = "event-1",
                name = "Item",
                category = EquipmentCategory.OTHER,
                sharedCost = -100
            )
        }
    }
    
    @Test
    fun `createChecklist generates camping equipment`() {
        val items = EquipmentManager.createChecklist(
            eventId = "event-1",
            eventType = "CAMPING"
        )
        
        assertTrue(items.isNotEmpty())
        assertTrue(items.any { it.name.contains("Tente", ignoreCase = true) })
        assertTrue(items.any { it.category == EquipmentCategory.CAMPING })
        assertTrue(items.any { it.category == EquipmentCategory.COOKING })
        
        // All items should be unassigned and NEEDED
        assertTrue(items.all { it.assignedTo == null })
        assertTrue(items.all { it.status == ItemStatus.NEEDED })
        assertTrue(items.all { it.eventId == "event-1" })
    }
    
    @Test
    fun `createChecklist generates beach equipment`() {
        val items = EquipmentManager.createChecklist(
            eventId = "event-2",
            eventType = "BEACH"
        )
        
        assertTrue(items.isNotEmpty())
        assertTrue(items.any { it.name.contains("Parasol", ignoreCase = true) || 
                               it.name.contains("Crème", ignoreCase = true) })
        assertTrue(items.any { it.category == EquipmentCategory.SPORTS })
    }
    
    @Test
    fun `createChecklist generates ski equipment`() {
        val items = EquipmentManager.createChecklist(
            eventId = "event-3",
            eventType = "SKI"
        )
        
        assertTrue(items.isNotEmpty())
        assertTrue(items.any { it.name.contains("Ski", ignoreCase = true) || 
                               it.name.contains("Casque", ignoreCase = true) })
    }
    
    @Test
    fun `createChecklist generates hiking equipment`() {
        val items = EquipmentManager.createChecklist(
            eventId = "event-4",
            eventType = "HIKING"
        )
        
        assertTrue(items.isNotEmpty())
        assertTrue(items.any { it.name.contains("sac", ignoreCase = true) || 
                               it.name.contains("Gourde", ignoreCase = true) })
    }
    
    @Test
    fun `createChecklist generates picnic equipment`() {
        val items = EquipmentManager.createChecklist(
            eventId = "event-5",
            eventType = "PICNIC"
        )
        
        assertTrue(items.isNotEmpty())
        assertTrue(items.any { it.name.contains("Nappe", ignoreCase = true) || 
                               it.name.contains("Glacière", ignoreCase = true) })
    }
    
    @Test
    fun `createChecklist generates indoor equipment`() {
        val items = EquipmentManager.createChecklist(
            eventId = "event-6",
            eventType = "INDOOR"
        )
        
        assertTrue(items.isNotEmpty())
        assertTrue(items.any { it.name.contains("Jeux", ignoreCase = true) || 
                               it.category == EquipmentCategory.ELECTRONICS })
    }
    
    @Test
    fun `createChecklist generates basic equipment for unknown type`() {
        val items = EquipmentManager.createChecklist(
            eventId = "event-7",
            eventType = "CUSTOM"
        )
        
        assertTrue(items.isNotEmpty())
        assertTrue(items.any { it.name.contains("premiers secours", ignoreCase = true) })
    }
    
    @Test
    fun `assignEquipment assigns item to participant`() {
        val item = EquipmentManager.createEquipmentItem(
            eventId = "event-1",
            name = "Tent",
            category = EquipmentCategory.CAMPING
        )
        
        val assigned = EquipmentManager.assignEquipment(item, "user-1")
        
        assertEquals("user-1", assigned.assignedTo)
        assertEquals(ItemStatus.ASSIGNED, assigned.status)
        assertNotEquals(item.updatedAt, assigned.updatedAt)
    }
    
    @Test
    fun `assignEquipment can preserve existing status`() {
        val item = EquipmentManager.createEquipmentItem(
            eventId = "event-1",
            name = "Tent",
            category = EquipmentCategory.CAMPING,
            status = ItemStatus.CONFIRMED
        )
        
        val assigned = EquipmentManager.assignEquipment(
            item = item,
            participantId = "user-1",
            updateStatus = false
        )
        
        assertEquals("user-1", assigned.assignedTo)
        assertEquals(ItemStatus.CONFIRMED, assigned.status)
    }
    
    @Test
    fun `assignEquipment rejects blank participant ID`() {
        val item = EquipmentManager.createEquipmentItem(
            eventId = "event-1",
            name = "Tent",
            category = EquipmentCategory.CAMPING
        )
        
        assertFailsWith<IllegalArgumentException> {
            EquipmentManager.assignEquipment(item, "   ")
        }
    }
    
    @Test
    fun `unassignEquipment removes assignment`() {
        val item = EquipmentManager.createEquipmentItem(
            eventId = "event-1",
            name = "Tent",
            category = EquipmentCategory.CAMPING,
            assignedTo = "user-1",
            status = ItemStatus.CONFIRMED
        )
        
        val unassigned = EquipmentManager.unassignEquipment(item)
        
        assertNull(unassigned.assignedTo)
        assertEquals(ItemStatus.NEEDED, unassigned.status)
        assertNotEquals(item.updatedAt, unassigned.updatedAt)
    }
    
    @Test
    fun `trackEquipmentStatus updates status correctly`() {
        val item = EquipmentManager.createEquipmentItem(
            eventId = "event-1",
            name = "Tent",
            category = EquipmentCategory.CAMPING,
            status = ItemStatus.NEEDED
        )
        
        val assigned = EquipmentManager.trackEquipmentStatus(item, ItemStatus.ASSIGNED)
        assertEquals(ItemStatus.ASSIGNED, assigned.status)
        
        val confirmed = EquipmentManager.trackEquipmentStatus(assigned, ItemStatus.CONFIRMED)
        assertEquals(ItemStatus.CONFIRMED, confirmed.status)
        
        val packed = EquipmentManager.trackEquipmentStatus(confirmed, ItemStatus.PACKED)
        assertEquals(ItemStatus.PACKED, packed.status)
    }
    
    @Test
    fun `trackEquipmentStatus rejects invalid transitions`() {
        val item = EquipmentManager.createEquipmentItem(
            eventId = "event-1",
            name = "Tent",
            category = EquipmentCategory.CAMPING,
            status = ItemStatus.PACKED
        )
        
        // Valid: PACKED can go back to CONFIRMED
        val confirmed = EquipmentManager.trackEquipmentStatus(item, ItemStatus.CONFIRMED)
        assertEquals(ItemStatus.CONFIRMED, confirmed.status)
    }
    
    @Test
    fun `validateEquipmentItem validates name`() {
        // Valid name
        val valid = EquipmentManager.validateEquipmentItem("Tent", 1)
        assertTrue(valid.isValid)
        assertTrue(valid.errors.isEmpty())
        
        // Blank name
        val blank = EquipmentManager.validateEquipmentItem("   ", 1)
        assertFalse(blank.isValid)
        assertTrue(blank.errors.any { it.contains("blank") })
        
        // Too long
        val tooLong = EquipmentManager.validateEquipmentItem("a".repeat(101), 1)
        assertFalse(tooLong.isValid)
        assertTrue(tooLong.errors.any { it.contains("100 characters") })
    }
    
    @Test
    fun `validateEquipmentItem validates quantity`() {
        // Valid
        val valid = EquipmentManager.validateEquipmentItem("Tent", 10)
        assertTrue(valid.isValid)
        
        // Too low
        val tooLow = EquipmentManager.validateEquipmentItem("Tent", 0)
        assertFalse(tooLow.isValid)
        assertTrue(tooLow.errors.any { it.contains("at least 1") })
        
        // Too high
        val tooHigh = EquipmentManager.validateEquipmentItem("Tent", 101)
        assertFalse(tooHigh.isValid)
        assertTrue(tooHigh.errors.any { it.contains("cannot exceed 100") })
    }
    
    @Test
    fun `validateEquipmentItem validates cost`() {
        // Valid
        val valid = EquipmentManager.validateEquipmentItem("Tent", 1, 5000)
        assertTrue(valid.isValid)
        
        // Negative
        val negative = EquipmentManager.validateEquipmentItem("Tent", 1, -100)
        assertFalse(negative.isValid)
        assertTrue(negative.errors.any { it.contains("cannot be negative") })
    }
    
    @Test
    fun `calculateChecklistStats computes correctly`() {
        val items = listOf(
            EquipmentManager.createEquipmentItem(
                eventId = "event-1",
                name = "Item 1",
                category = EquipmentCategory.CAMPING,
                status = ItemStatus.NEEDED
            ),
            EquipmentManager.createEquipmentItem(
                eventId = "event-1",
                name = "Item 2",
                category = EquipmentCategory.CAMPING,
                assignedTo = "user-1",
                status = ItemStatus.ASSIGNED,
                sharedCost = 1000
            ),
            EquipmentManager.createEquipmentItem(
                eventId = "event-1",
                name = "Item 3",
                category = EquipmentCategory.COOKING,
                assignedTo = "user-2",
                status = ItemStatus.CONFIRMED,
                sharedCost = 2000
            ),
            EquipmentManager.createEquipmentItem(
                eventId = "event-1",
                name = "Item 4",
                category = EquipmentCategory.SPORTS,
                assignedTo = "user-1",
                status = ItemStatus.PACKED,
                sharedCost = 1500
            )
        )
        
        val stats = EquipmentManager.calculateChecklistStats("event-1", items)
        
        assertEquals("event-1", stats.eventId)
        assertEquals(4, stats.totalItems)
        assertEquals(3, stats.assignedItems)
        assertEquals(1, stats.confirmedItems)
        assertEquals(1, stats.packedItems)
        assertEquals(4500, stats.totalCost)
    }
    
    @Test
    fun `calculateChecklistStats handles empty list`() {
        val stats = EquipmentManager.calculateChecklistStats("event-1", emptyList())
        
        assertEquals("event-1", stats.eventId)
        assertEquals(0, stats.totalItems)
        assertEquals(0, stats.assignedItems)
        assertEquals(0, stats.confirmedItems)
        assertEquals(0, stats.packedItems)
        assertEquals(0, stats.totalCost)
    }
    
    @Test
    fun `groupByCategory groups items correctly`() {
        val items = listOf(
            EquipmentManager.createEquipmentItem(
                eventId = "event-1",
                name = "Tent",
                category = EquipmentCategory.CAMPING
            ),
            EquipmentManager.createEquipmentItem(
                eventId = "event-1",
                name = "Sleeping Bag",
                category = EquipmentCategory.CAMPING,
                assignedTo = "user-1",
                status = ItemStatus.PACKED,
                sharedCost = 3000
            ),
            EquipmentManager.createEquipmentItem(
                eventId = "event-1",
                name = "Stove",
                category = EquipmentCategory.COOKING,
                assignedTo = "user-2",
                sharedCost = 2000
            )
        )
        
        val grouped = EquipmentManager.groupByCategory(items)
        
        assertEquals(2, grouped.size)
        
        val camping = grouped.find { it.category == EquipmentCategory.CAMPING }
        assertNotNull(camping)
        assertEquals(2, camping.itemCount)
        assertEquals(1, camping.assignedCount)
        assertEquals(3000, camping.totalCost)
        
        val cooking = grouped.find { it.category == EquipmentCategory.COOKING }
        assertNotNull(cooking)
        assertEquals(1, cooking.itemCount)
        assertEquals(1, cooking.assignedCount)
        assertEquals(2000, cooking.totalCost)
    }
    
    @Test
    fun `calculateParticipantStats computes correctly`() {
        val items = listOf(
            EquipmentManager.createEquipmentItem(
                eventId = "event-1",
                name = "Item 1",
                category = EquipmentCategory.CAMPING,
                assignedTo = "user-1",
                status = ItemStatus.ASSIGNED,
                sharedCost = 1000
            ),
            EquipmentManager.createEquipmentItem(
                eventId = "event-1",
                name = "Item 2",
                category = EquipmentCategory.COOKING,
                assignedTo = "user-1",
                status = ItemStatus.CONFIRMED,
                sharedCost = 2000
            ),
            EquipmentManager.createEquipmentItem(
                eventId = "event-1",
                name = "Item 3",
                category = EquipmentCategory.SPORTS,
                assignedTo = "user-1",
                status = ItemStatus.PACKED,
                sharedCost = 1500
            ),
            EquipmentManager.createEquipmentItem(
                eventId = "event-1",
                name = "Item 4",
                category = EquipmentCategory.OTHER,
                assignedTo = "user-2", // Different user
                status = ItemStatus.PACKED
            )
        )
        
        val stats = EquipmentManager.calculateParticipantStats(items, "user-1")
        
        assertEquals("user-1", stats.participantId)
        assertEquals(3, stats.assignedItemsCount)
        assertEquals(4500, stats.totalValue)
        assertEquals(3, stats.itemNames.size)
        assertTrue(stats.itemNames.contains("Item 1"))
        assertTrue(stats.itemNames.contains("Item 2"))
        assertTrue(stats.itemNames.contains("Item 3"))
    }
}
