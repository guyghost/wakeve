package com.guyghost.wakeve.models

import kotlinx.serialization.Serializable

@Serializable
data class CreateEquipmentItemRequest(
    val name: String,
    val category: EquipmentCategory,
    val quantity: Int,
    val assignedTo: String? = null,
    val status: ItemStatus = ItemStatus.NEEDED,
    val sharedCost: Long? = null,
    val notes: String? = null
) {
    fun toEquipmentItem(eventId: String): EquipmentItem {
        // ID and timestamps will be handled by the Manager/Repository or logic
        // For DTO to Model conversion, we might need a helper or use the Manager to create it.
        // Since the route calls repository.createEquipmentItem directly with a model,
        // we need to construct a robust EquipmentItem here or change the route to use Manager.
        // The repository takes an EquipmentItem.
        // Let's assume a basic mapping here for now, ID generation might need attention.
        // Actually, looking at EquipmentManager.createEquipmentItem, it generates ID and timestamps.
        // The route should probably use EquipmentManager.createEquipmentItem instead of manual construction if possible,
        // OR we map it manually here. The route calls repository.createEquipmentItem(request.toEquipmentItem(eventId)).
        // We will implement toEquipmentItem to return a basic object, but the ID needs to be generated.
        // Since we can't easily access the internal UUID generator of Manager from this DTO,
        // we'll rely on the standard java.util.UUID or kotlin.random for now, or better, 
        // the route should rely on Manager.
        
        // However, to satisfy the compilation of `request.toEquipmentItem(eventId)`, we implement it.
        // We'll use a placeholder ID that the repository or manager might overwrite, or generate a random one.
        
        return EquipmentItem(
            id = java.util.UUID.randomUUID().toString(),
            eventId = eventId,
            name = name,
            category = category,
            quantity = quantity,
            assignedTo = assignedTo,
            status = status,
            sharedCost = sharedCost,
            notes = notes,
            createdAt = java.time.Instant.now().toString(),
            updatedAt = java.time.Instant.now().toString()
        )
    }
}

@Serializable
data class UpdateEquipmentItemRequest(
    val name: String? = null,
    val category: EquipmentCategory? = null,
    val quantity: Int? = null,
    val assignedTo: String? = null,
    val status: ItemStatus? = null,
    val sharedCost: Long? = null,
    val notes: String? = null
) {
    fun applyTo(existing: EquipmentItem): EquipmentItem {
        return existing.copy(
            name = name ?: existing.name,
            category = category ?: existing.category,
            quantity = quantity ?: existing.quantity,
            assignedTo = assignedTo ?: existing.assignedTo,
            status = status ?: existing.status,
            sharedCost = sharedCost ?: existing.sharedCost,
            notes = notes ?: existing.notes,
            updatedAt = java.time.Instant.now().toString()
        )
    }
}

@Serializable
data class AssignEquipmentItemRequest(
    val participantId: String?
)

@Serializable
data class UpdateEquipmentStatusRequest(
    val newStatus: ItemStatus
)

@Serializable
data class AutoGenerateEquipmentRequest(
    val eventType: String,
    val participantCount: Int
)
