package com.guyghost.wakeve.equipment

import com.guyghost.wakeve.database.WakevDb
import com.guyghost.wakeve.models.EquipmentByCategory
import com.guyghost.wakeve.models.EquipmentCategory
import com.guyghost.wakeve.models.EquipmentChecklist
import com.guyghost.wakeve.models.EquipmentItem
import com.guyghost.wakeve.models.ItemStatus
import com.guyghost.wakeve.models.ParticipantEquipmentStats
import kotlinx.datetime.Clock

/**
 * Equipment Repository - Manages equipment item persistence.
 * 
 * Responsibilities:
 * - CRUD operations for equipment items
 * - Equipment queries and filtering
 * - Statistics and aggregations
 * - Map between SQLDelight entities and Kotlin models
 */
class EquipmentRepository(private val db: WakevDb) {
    
    private val equipmentQueries = db.equipmentItemQueries
    
    // ==================== Equipment Item Operations ====================
    
    /**
     * Create a new equipment item.
     * 
     * @param item Equipment item to create
     * @return Created EquipmentItem
     */
    fun createEquipmentItem(item: EquipmentItem): EquipmentItem {
        equipmentQueries.insertEquipmentItem(
            id = item.id,
            event_id = item.eventId,
            name = item.name,
            category = item.category.name,
            quantity = item.quantity.toLong(),
            assigned_to = item.assignedTo,
            status = item.status.name,
            shared_cost = item.sharedCost,
            notes = item.notes,
            created_at = item.createdAt,
            updated_at = item.updatedAt
        )
        
        return item
    }
    
    /**
     * Get equipment item by ID.
     */
    fun getEquipmentItemById(itemId: String): EquipmentItem? {
        return equipmentQueries.selectEquipmentItemById(itemId).executeAsOneOrNull()?.toModel()
    }
    
    /**
     * Get all equipment items for an event.
     */
    fun getEquipmentItemsByEventId(eventId: String): List<EquipmentItem> {
        return equipmentQueries.selectEquipmentItemsByEvent(eventId)
            .executeAsList()
            .map { it.toModel() }
    }
    
    /**
     * Get equipment items by category.
     */
    fun getEquipmentItemsByCategory(eventId: String, category: EquipmentCategory): List<EquipmentItem> {
        return equipmentQueries.selectEquipmentItemsByEventAndCategory(eventId, category.name)
            .executeAsList()
            .map { it.toModel() }
    }
    
    /**
     * Get equipment items by status.
     */
    fun getEquipmentItemsByStatus(eventId: String, status: ItemStatus): List<EquipmentItem> {
        return equipmentQueries.selectEquipmentItemsByEventAndStatus(eventId, status.name)
            .executeAsList()
            .map { it.toModel() }
    }
    
    /**
     * Get equipment items assigned to a participant.
     */
    fun getEquipmentItemsByAssignee(eventId: String, participantId: String): List<EquipmentItem> {
        return equipmentQueries.selectEquipmentItemsByAssignee(eventId, participantId)
            .executeAsList()
            .map { it.toModel() }
    }
    
    /**
     * Get unassigned equipment items.
     */
    fun getUnassignedItems(eventId: String): List<EquipmentItem> {
        return equipmentQueries.selectUnassignedItems(eventId)
            .executeAsList()
            .map { it.toModel() }
    }
    
    /**
     * Update an existing equipment item.
     * 
     * @param item Equipment item with updated fields
     * @return Updated EquipmentItem
     */
    fun updateEquipmentItem(item: EquipmentItem): EquipmentItem {
        val updatedItem = item.copy(updatedAt = getCurrentUtcIsoString())
        
        equipmentQueries.updateEquipmentItem(
            name = updatedItem.name,
            category = updatedItem.category.name,
            quantity = updatedItem.quantity.toLong(),
            assigned_to = updatedItem.assignedTo,
            status = updatedItem.status.name,
            shared_cost = updatedItem.sharedCost,
            notes = updatedItem.notes,
            updated_at = updatedItem.updatedAt,
            id = updatedItem.id
        )
        
        return updatedItem
    }
    
    /**
     * Update equipment item status.
     */
    fun updateEquipmentItemStatus(itemId: String, status: ItemStatus): EquipmentItem? {
        equipmentQueries.updateEquipmentItemStatus(
            status = status.name,
            updated_at = getCurrentUtcIsoString(),
            id = itemId
        )
        
        return getEquipmentItemById(itemId)
    }
    
    /**
     * Update equipment item assignment.
     */
    fun updateEquipmentItemAssignment(itemId: String, participantId: String?, status: ItemStatus): EquipmentItem? {
        equipmentQueries.updateEquipmentItemAssignment(
            assigned_to = participantId,
            status = status.name,
            updated_at = getCurrentUtcIsoString(),
            id = itemId
        )
        
        return getEquipmentItemById(itemId)
    }
    
    /**
     * Delete an equipment item.
     */
    fun deleteEquipmentItem(itemId: String) {
        equipmentQueries.deleteEquipmentItem(itemId)
    }
    
    /**
     * Delete all equipment items for an event.
     */
    fun deleteEquipmentItemsByEvent(eventId: String) {
        equipmentQueries.deleteEquipmentItemsByEvent(eventId)
    }
    
    // ==================== Statistics & Aggregations ====================
    
    /**
     * Count equipment items for an event.
     */
    fun countEquipmentItemsByEvent(eventId: String): Long {
        return equipmentQueries.countEquipmentItemsByEvent(eventId).executeAsOne()
    }
    
    /**
     * Count equipment items by status.
     */
    fun countEquipmentItemsByStatus(eventId: String, status: ItemStatus): Long {
        return equipmentQueries.countEquipmentItemsByEventAndStatus(eventId, status.name).executeAsOne()
    }
    
    /**
     * Count equipment items by category.
     */
    fun countEquipmentItemsByCategory(eventId: String, category: EquipmentCategory): Long {
        return equipmentQueries.countEquipmentItemsByEventAndCategory(eventId, category.name).executeAsOne()
    }
    
    /**
     * Sum equipment cost by event.
     */
    fun sumEquipmentCostByEvent(eventId: String): Long {
        return equipmentQueries.sumEquipmentCostByEvent(eventId).executeAsOne().toLong()
    }
    
    /**
     * Sum equipment cost by category.
     */
    fun sumEquipmentCostByCategory(eventId: String, category: EquipmentCategory): Long {
        return equipmentQueries.sumEquipmentCostByEventAndCategory(eventId, category.name).executeAsOne().toLong()
    }
    
    /**
     * Sum equipment cost by assignee.
     */
    fun sumEquipmentCostByAssignee(eventId: String, participantId: String): Long {
        return equipmentQueries.sumEquipmentCostByAssignee(eventId, participantId).executeAsOne().toLong()
    }
    
    /**
     * Get equipment statistics by category.
     */
    fun getEquipmentStatsByCategory(eventId: String): List<EquipmentByCategory> {
        return equipmentQueries.selectEquipmentStatsByCategory(eventId)
            .executeAsList()
            .map { stats ->
                val category = EquipmentCategory.valueOf(stats.category)
                val items = getEquipmentItemsByCategory(eventId, category)
                
                EquipmentByCategory(
                    category = category,
                    items = items,
                    itemCount = stats.itemCount.toInt(),
                    assignedCount = stats.assignedCount.toInt(),
                    totalCost = stats.totalCost.toLong()
                )
            }
    }
    
    /**
     * Get equipment statistics by assignee.
     */
    fun getEquipmentStatsByAssignee(eventId: String): List<ParticipantEquipmentStats> {
        return equipmentQueries.selectEquipmentStatsByAssignee(eventId)
            .executeAsList()
            .mapNotNull { stats ->
                val participantId = stats.assigned_to ?: return@mapNotNull null
                val itemNames = equipmentQueries.selectItemNamesByAssignee(eventId, participantId)
                    .executeAsList()
                
                ParticipantEquipmentStats(
                    participantId = participantId,
                    assignedItemsCount = stats.itemCount.toInt(),
                    itemNames = itemNames,
                    totalValue = stats.totalValue.toLong()
                )
            }
    }
    
    /**
     * Get overall equipment checklist statistics.
     */
    fun getEquipmentChecklist(eventId: String): EquipmentChecklist {
        val items = getEquipmentItemsByEventId(eventId)
        val stats = equipmentQueries.selectEquipmentOverallStats(eventId).executeAsOne()
        
        return EquipmentChecklist(
            eventId = eventId,
            items = items,
            totalItems = stats.totalItems.toInt(),
            assignedItems = stats.assignedItems.toInt(),
            confirmedItems = stats.confirmedItems.toInt(),
            packedItems = stats.packedItems.toInt(),
            totalCost = stats.totalCost.toLong()
        )
    }
    
    /**
     * Check if an equipment item exists.
     */
    fun equipmentItemExists(itemId: String): Boolean {
        return equipmentQueries.equipmentItemExists(itemId).executeAsOne()
    }
    
    // ==================== Helper Methods ====================
    
    private fun getCurrentUtcIsoString(): String {
        return Clock.System.now().toString()
    }
    
    /**
     * Convert SQL EquipmentItem entity to Kotlin model.
     */
    private fun com.guyghost.wakeve.Equipment_item.toModel(): EquipmentItem {
        return EquipmentItem(
            id = this.id,
            eventId = this.event_id,
            name = this.name,
            category = EquipmentCategory.valueOf(this.category),
            quantity = this.quantity.toInt(),
            assignedTo = this.assigned_to,
            status = ItemStatus.valueOf(this.status),
            sharedCost = this.shared_cost,
            notes = this.notes,
            createdAt = this.created_at,
            updatedAt = this.updated_at
        )
    }
}
