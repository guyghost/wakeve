package com.guyghost.wakeve.models

import kotlinx.serialization.Serializable

/**
 * Equipment category
 */
@Serializable
enum class EquipmentCategory {
    CAMPING,      // Tents, sleeping bags, camping gear
    SPORTS,       // Sports equipment, outdoor gear
    COOKING,      // Kitchen equipment, utensils
    ELECTRONICS,  // Cameras, speakers, chargers
    SAFETY,       // First aid, flashlights, tools
    OTHER         // Miscellaneous items
}

/**
 * Equipment item status
 */
@Serializable
enum class ItemStatus {
    NEEDED,      // Item is needed but not assigned
    ASSIGNED,    // Someone is assigned to bring it
    CONFIRMED,   // Assignment confirmed by assignee
    PACKED,      // Item is packed and ready
    CANCELLED    // No longer needed
}

/**
 * Equipment item for an event
 * 
 * Represents an item needed for an event with assignment tracking.
 * 
 * @property id Unique identifier
 * @property eventId Event this item belongs to
 * @property name Name of the item (e.g., "Tent 4 places", "Barbecue grill")
 * @property category Category of equipment
 * @property quantity Number of items needed
 * @property assignedTo Participant ID who will bring this item (null if unassigned)
 * @property status Current status of the item
 * @property sharedCost Cost shared among participants (in cents, null if no cost)
 * @property notes Additional notes or specifications
 * @property createdAt Creation timestamp (ISO 8601 UTC)
 * @property updatedAt Last update timestamp (ISO 8601 UTC)
 */
@Serializable
data class EquipmentItem(
    val id: String,
    val eventId: String,
    val name: String,
    val category: EquipmentCategory,
    val quantity: Int,
    val assignedTo: String? = null,
    val status: ItemStatus,
    val sharedCost: Long? = null,  // In cents
    val notes: String? = null,
    val createdAt: String,  // ISO 8601 UTC timestamp
    val updatedAt: String   // ISO 8601 UTC timestamp
) {
    init {
        require(name.isNotBlank()) { "Equipment name cannot be blank" }
        require(quantity > 0) { "Quantity must be positive" }
        if (sharedCost != null) {
            require(sharedCost >= 0) { "Shared cost cannot be negative" }
        }
    }
}

/**
 * Equipment checklist with statistics
 * 
 * Provides an overview of equipment items grouped by category.
 * 
 * @property eventId Event ID
 * @property items List of all equipment items
 * @property totalItems Total number of items
 * @property assignedItems Number of assigned items
 * @property confirmedItems Number of confirmed items
 * @property packedItems Number of packed items
 * @property totalCost Total shared cost in cents
 */
@Serializable
data class EquipmentChecklist(
    val eventId: String,
    val items: List<EquipmentItem>,
    val totalItems: Int,
    val assignedItems: Int,
    val confirmedItems: Int,
    val packedItems: Int,
    val totalCost: Long  // In cents
)

/**
 * Equipment items grouped by category
 * 
 * @property category Equipment category
 * @property items List of items in this category
 * @property itemCount Total items in category
 * @property assignedCount Number of assigned items
 * @property totalCost Total cost for this category in cents
 */
@Serializable
data class EquipmentByCategory(
    val category: EquipmentCategory,
    val items: List<EquipmentItem>,
    val itemCount: Int,
    val assignedCount: Int,
    val totalCost: Long  // In cents
)

/**
 * Equipment statistics per participant
 * 
 * @property participantId Participant ID
 * @property assignedItemsCount Number of items assigned to this participant
 * @property itemNames List of item names assigned
 * @property totalValue Total value of assigned items in cents
 */
@Serializable
data class ParticipantEquipmentStats(
    val participantId: String,
    val assignedItemsCount: Int,
    val itemNames: List<String>,
    val totalValue: Long  // In cents
)

/**
 * Request to create or update an equipment item
 * 
 * @property name Item name
 * @property category Category
 * @property quantity Quantity needed
 * @property assignedTo Assigned participant ID (optional)
 * @property status Item status
 * @property sharedCost Shared cost in cents (optional)
 * @property notes Additional notes (optional)
 */
@Serializable
data class EquipmentItemRequest(
    val name: String,
    val category: EquipmentCategory,
    val quantity: Int,
    val assignedTo: String? = null,
    val status: ItemStatus,
    val sharedCost: Long? = null,  // In cents
    val notes: String? = null
)

/**
 * Request to generate a default equipment checklist
 * 
 * @property eventType Type of event (e.g., "camping", "beach", "ski", "indoor")
 * @property participantCount Number of participants
 * @property duration Duration in days
 */
@Serializable
data class GenerateChecklistRequest(
    val eventType: String,
    val participantCount: Int,
    val duration: Int
)
