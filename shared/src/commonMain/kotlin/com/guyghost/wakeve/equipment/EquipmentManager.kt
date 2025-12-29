package com.guyghost.wakeve.equipment

import com.guyghost.wakeve.models.*
import kotlinx.datetime.Clock
import kotlin.random.Random

/**
 * Service for equipment checklist management
 * 
 * This service provides business logic for:
 * - Creating equipment items
 * - Auto-generating checklists by event type
 * - Assigning items to participants
 * - Tracking equipment status (NEEDED → ASSIGNED → CONFIRMED → PACKED)
 * - Calculating checklist statistics
 * - Validating equipment data
 */
object EquipmentManager {
    
    /**
     * Generate a random UUID string for cross-platform compatibility
     */
    private fun generateUuid(): String {
        val chars = "0123456789abcdef"
        return buildString(36) {
            repeat(36) { i ->
                when (i) {
                    8, 13, 18, 23 -> append('-')
                    14 -> append('4') // UUID version 4
                    19 -> append(chars[Random.nextInt(4) + 8]) // 8, 9, a, or b
                    else -> append(chars[Random.nextInt(16)])
                }
            }
        }
    }
    
    /**
     * Create a new equipment item
     */
    fun createEquipmentItem(
        eventId: String,
        name: String,
        category: EquipmentCategory,
        quantity: Int = 1,
        assignedTo: String? = null,
        status: ItemStatus = ItemStatus.NEEDED,
        sharedCost: Long? = null,
        notes: String? = null
    ): EquipmentItem {
        val validation = validateEquipmentItem(name, quantity, sharedCost)
        require(validation.isValid) { validation.errors.joinToString(", ") }
        
        val now = Clock.System.now().toString()
        return EquipmentItem(
            id = generateUuid(),
            eventId = eventId,
            name = name.trim(),
            category = category,
            quantity = quantity,
            assignedTo = assignedTo,
            status = status,
            sharedCost = sharedCost,
            notes = notes?.trim(),
            createdAt = now,
            updatedAt = now
        )
    }
    
    /**
     * Auto-generate equipment checklist based on event type
     * 
     * Creates a comprehensive equipment list tailored to the event type.
     * 
     * @param eventId Event ID
     * @param eventType Type of event
     * @return List of generated equipment items
     */
    fun autoGenerateChecklist(eventId: String, eventType: String, participantCount: Int): List<EquipmentItem> {
        val items = mutableListOf<EquipmentItem>()
        val now = Clock.System.now().toString()
        
        val templates = when (eventType.uppercase()) {
            "CAMPING" -> getCampingEquipment()
            "BEACH" -> getBeachEquipment()
            "SKI" -> getSkiEquipment()
            "HIKING" -> getHikingEquipment()
            "PICNIC" -> getPicnicEquipment()
            "INDOOR" -> getIndoorEquipment()
            else -> getBasicEquipment()
        }
        
        for ((name, category) in templates) {
            // Adjust quantity based on participants if needed (simple logic for now)
            val quantity = if (category == EquipmentCategory.COOKING || category == EquipmentCategory.OTHER) {
               1 // Shared items
            } else {
               1 // Could be participantCount for some items, but keeping simple for now
            }

            items.add(
                EquipmentItem(
                    id = generateUuid(),
                    eventId = eventId,
                    name = name,
                    category = category,
                    quantity = quantity,
                    assignedTo = null,
                    status = ItemStatus.NEEDED,
                    sharedCost = null,
                    notes = null,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
        
        return items
    }

    /**
     * Calculate equipment statistics for a list of items
     */
    fun calculateEquipmentStats(items: List<EquipmentItem>): EquipmentChecklist {
        if (items.isEmpty()) {
            return EquipmentChecklist(
                eventId = "",
                items = emptyList(),
                totalItems = 0,
                assignedItems = 0,
                confirmedItems = 0,
                packedItems = 0,
                totalCost = 0
            )
        }
        
        val eventId = items.first().eventId
        return calculateChecklistStats(eventId, items)
    }

    /**
     * Legacy method for backward compatibility
     */
    fun createChecklist(eventId: String, eventType: String): List<EquipmentItem> {
        return autoGenerateChecklist(eventId, eventType, 1)
    }

    
    /**
     * Assign equipment item to a participant
     */
    fun assignEquipment(
        item: EquipmentItem,
        participantId: String,
        updateStatus: Boolean = true
    ): EquipmentItem {
        require(participantId.isNotBlank()) { "Participant ID cannot be blank" }
        
        val newStatus = if (updateStatus && item.status == ItemStatus.NEEDED) {
            ItemStatus.ASSIGNED
        } else {
            item.status
        }
        
        return item.copy(
            assignedTo = participantId,
            status = newStatus,
            updatedAt = Clock.System.now().toString()
        )
    }
    
    /**
     * Unassign equipment item
     */
    fun unassignEquipment(item: EquipmentItem): EquipmentItem {
        return item.copy(
            assignedTo = null,
            status = ItemStatus.NEEDED,
            updatedAt = Clock.System.now().toString()
        )
    }
    
    /**
     * Track equipment status through lifecycle
     */
    fun trackEquipmentStatus(
        item: EquipmentItem,
        newStatus: ItemStatus
    ): EquipmentItem {
        // Validate status transition
        val validTransition = isValidStatusTransition(item.status, newStatus)
        require(validTransition) { 
            "Invalid status transition from ${item.status} to $newStatus" 
        }
        
        return item.copy(
            status = newStatus,
            updatedAt = Clock.System.now().toString()
        )
    }
    
    /**
     * Validate status transition
     */
    fun isValidStatusTransition(from: ItemStatus, to: ItemStatus): Boolean {
        return when (from) {
            ItemStatus.NEEDED -> to in listOf(ItemStatus.ASSIGNED, ItemStatus.CONFIRMED, ItemStatus.PACKED, ItemStatus.CANCELLED)
            ItemStatus.ASSIGNED -> to in listOf(ItemStatus.CONFIRMED, ItemStatus.PACKED, ItemStatus.NEEDED, ItemStatus.CANCELLED)
            ItemStatus.CONFIRMED -> to in listOf(ItemStatus.PACKED, ItemStatus.ASSIGNED, ItemStatus.CANCELLED)
            ItemStatus.PACKED -> to in listOf(ItemStatus.CONFIRMED, ItemStatus.ASSIGNED, ItemStatus.NEEDED)
            ItemStatus.CANCELLED -> false // Cannot transition from cancelled
        }
    }
    
    /**
     * Validate equipment item data
     */
    fun validateEquipmentItem(
        name: String,
        quantity: Int,
        sharedCost: Long? = null
    ): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (name.isBlank()) {
            errors.add("Equipment name cannot be blank")
        }
        
        if (name.length > 100) {
            errors.add("Equipment name must be 100 characters or less")
        }
        
        if (quantity < 1) {
            errors.add("Quantity must be at least 1")
        }
        
        if (quantity > 100) {
            errors.add("Quantity cannot exceed 100")
        }
        
        if (sharedCost != null && sharedCost < 0) {
            errors.add("Shared cost cannot be negative")
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }
    
    /**
     * Calculate checklist statistics
     */
    fun calculateChecklistStats(eventId: String, items: List<EquipmentItem>): EquipmentChecklist {
        val totalItems = items.size
        val assignedItems = items.count { it.assignedTo != null }
        val confirmedItems = items.count { it.status == ItemStatus.CONFIRMED }
        val packedItems = items.count { it.status == ItemStatus.PACKED }
        val totalCost = items.mapNotNull { it.sharedCost }.sum()
        
        return EquipmentChecklist(
            eventId = eventId,
            items = items,
            totalItems = totalItems,
            assignedItems = assignedItems,
            confirmedItems = confirmedItems,
            packedItems = packedItems,
            totalCost = totalCost
        )
    }
    
    /**
     * Group equipment by category
     */
    fun groupByCategory(items: List<EquipmentItem>): List<EquipmentByCategory> {
        return items.groupBy { it.category }
            .map { (category, categoryItems) ->
                val totalCost = categoryItems.mapNotNull { it.sharedCost }.sum()
                EquipmentByCategory(
                    category = category,
                    items = categoryItems.sortedBy { it.name },
                    itemCount = categoryItems.size,
                    assignedCount = categoryItems.count { it.assignedTo != null },
                    totalCost = totalCost
                )
            }
            .sortedBy { it.category.name }
    }
    
    /**
     * Calculate participant equipment statistics
     */
    fun calculateParticipantStats(items: List<EquipmentItem>, participantId: String): ParticipantEquipmentStats {
        val participantItems = items.filter { it.assignedTo == participantId }
        val itemNames = participantItems.map { it.name }
        
        return ParticipantEquipmentStats(
            participantId = participantId,
            assignedItemsCount = participantItems.size,
            itemNames = itemNames,
            totalValue = participantItems.mapNotNull { it.sharedCost }.sum()
        )
    }
    
    // ==================== EQUIPMENT TEMPLATES ====================
    
    private fun getCampingEquipment(): List<Pair<String, EquipmentCategory>> = listOf(
        "Tente" to EquipmentCategory.CAMPING,
        "Sac de couchage" to EquipmentCategory.CAMPING,
        "Matelas gonflable" to EquipmentCategory.CAMPING,
        "Lampe frontale" to EquipmentCategory.ELECTRONICS,
        "Réchaud de camping" to EquipmentCategory.COOKING,
        "Gaz pour réchaud" to EquipmentCategory.COOKING,
        "Casseroles et poêles" to EquipmentCategory.COOKING,
        "Ustensiles de cuisine" to EquipmentCategory.COOKING,
        "Vaisselle réutilisable" to EquipmentCategory.COOKING,
        "Glacière" to EquipmentCategory.COOKING,
        "Pains de glace" to EquipmentCategory.COOKING,
        "Bidons d'eau" to EquipmentCategory.OTHER,
        "Trousse de premiers secours" to EquipmentCategory.OTHER,
        "Allumettes/Briquet" to EquipmentCategory.OTHER,
        "Sacs poubelle" to EquipmentCategory.OTHER
    )
    
    private fun getBeachEquipment(): List<Pair<String, EquipmentCategory>> = listOf(
        "Parasol" to EquipmentCategory.OTHER,
        "Serviettes de plage" to EquipmentCategory.OTHER,
        "Crème solaire" to EquipmentCategory.OTHER,
        "Glacière" to EquipmentCategory.COOKING,
        "Pains de glace" to EquipmentCategory.COOKING,
        "Ballon de plage" to EquipmentCategory.SPORTS,
        "Raquettes de beach" to EquipmentCategory.SPORTS,
        "Masque et tuba" to EquipmentCategory.SPORTS,
        "Bouée" to EquipmentCategory.SPORTS,
        "Enceinte portable" to EquipmentCategory.ELECTRONICS,
        "Jeux de cartes" to EquipmentCategory.OTHER,
        "Sacs étanches" to EquipmentCategory.OTHER
    )
    
    private fun getSkiEquipment(): List<Pair<String, EquipmentCategory>> = listOf(
        "Skis/Snowboard" to EquipmentCategory.SPORTS,
        "Bâtons de ski" to EquipmentCategory.SPORTS,
        "Casque" to EquipmentCategory.SPORTS,
        "Masque de ski" to EquipmentCategory.SPORTS,
        "Gants" to EquipmentCategory.OTHER,
        "Chaufferettes" to EquipmentCategory.OTHER,
        "Crème solaire haute protection" to EquipmentCategory.OTHER,
        "Thermos" to EquipmentCategory.COOKING,
        "Trousse de premiers secours" to EquipmentCategory.OTHER
    )
    
    private fun getHikingEquipment(): List<Pair<String, EquipmentCategory>> = listOf(
        "Sacs à dos" to EquipmentCategory.OTHER,
        "Gourdes/Camelback" to EquipmentCategory.OTHER,
        "Bâtons de randonnée" to EquipmentCategory.SPORTS,
        "Carte/GPS" to EquipmentCategory.ELECTRONICS,
        "Trousse de premiers secours" to EquipmentCategory.OTHER,
        "Couteau suisse" to EquipmentCategory.OTHER,
        "Nourriture de trail" to EquipmentCategory.COOKING,
        "Protection pluie" to EquipmentCategory.OTHER,
        "Lampe frontale" to EquipmentCategory.ELECTRONICS
    )
    
    private fun getPicnicEquipment(): List<Pair<String, EquipmentCategory>> = listOf(
        "Nappe" to EquipmentCategory.OTHER,
        "Assiettes et couverts" to EquipmentCategory.COOKING,
        "Verres réutilisables" to EquipmentCategory.COOKING,
        "Glacière" to EquipmentCategory.COOKING,
        "Pains de glace" to EquipmentCategory.COOKING,
        "Tire-bouchon" to EquipmentCategory.COOKING,
        "Sacs poubelle" to EquipmentCategory.OTHER,
        "Ballon" to EquipmentCategory.SPORTS,
        "Jeux de société" to EquipmentCategory.OTHER,
        "Enceinte portable" to EquipmentCategory.ELECTRONICS
    )
    
    private fun getIndoorEquipment(): List<Pair<String, EquipmentCategory>> = listOf(
        "Projecteur/TV" to EquipmentCategory.ELECTRONICS,
        "Enceintes" to EquipmentCategory.ELECTRONICS,
        "Jeux de société" to EquipmentCategory.OTHER,
        "Jeux de cartes" to EquipmentCategory.OTHER,
        "Décoration" to EquipmentCategory.OTHER,
        "Nappes" to EquipmentCategory.OTHER,
        "Vaisselle supplémentaire" to EquipmentCategory.COOKING,
        "Glacière pour boissons" to EquipmentCategory.COOKING
    )
    
    private fun getBasicEquipment(): List<Pair<String, EquipmentCategory>> = listOf(
        "Trousse de premiers secours" to EquipmentCategory.OTHER,
        "Sacs poubelle" to EquipmentCategory.OTHER,
        "Chargeurs téléphone" to EquipmentCategory.ELECTRONICS,
        "Batterie externe" to EquipmentCategory.ELECTRONICS
    )
}

/**
 * Validation result data class
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String>
)
