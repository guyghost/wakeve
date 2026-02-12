package com.guyghost.wakeve.budget

import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.Budget
import com.guyghost.wakeve.models.BudgetCategory
import com.guyghost.wakeve.models.BudgetItem
import com.guyghost.wakeve.models.BudgetWithItems
import com.guyghost.wakeve.models.ParticipantBudgetShare
import kotlinx.datetime.Clock
import com.guyghost.wakeve.Budget as SqlBudget
import com.guyghost.wakeve.BudgetItem as SqlBudgetItem

/**
 * Budget Repository - Manages budget and budget items persistence.
 * 
 * Responsibilities:
 * - CRUD operations for budgets and budget items
 * - Auto-update budget totals when items change
 * - Aggregate calculations from database
 * - Map between SQLDelight entities and Kotlin models
 */
class BudgetRepository(private val db: WakeveDb) {
    
    private val budgetQueries = db.budgetQueries
    private val budgetItemQueries = db.budgetItemQueries
    
    // ==================== Budget Operations ====================
    
    /**
     * Create a new budget for an event.
     * 
     * @param eventId ID of the event
     * @return Created Budget model
     */
    fun createBudget(eventId: String): Budget {
        val now = getCurrentUtcIsoString()
        val budgetId = generateId()
        
        val budget = Budget(
            id = budgetId,
            eventId = eventId,
            totalEstimated = 0.0,
            totalActual = 0.0,
            transportEstimated = 0.0,
            transportActual = 0.0,
            accommodationEstimated = 0.0,
            accommodationActual = 0.0,
            mealsEstimated = 0.0,
            mealsActual = 0.0,
            activitiesEstimated = 0.0,
            activitiesActual = 0.0,
            equipmentEstimated = 0.0,
            equipmentActual = 0.0,
            otherEstimated = 0.0,
            otherActual = 0.0,
            createdAt = now,
            updatedAt = now
        )
        
        budgetQueries.insertBudget(
            id = budget.id,
            eventId = budget.eventId,
            totalEstimated = budget.totalEstimated,
            totalActual = budget.totalActual,
            transportEstimated = budget.transportEstimated,
            transportActual = budget.transportActual,
            accommodationEstimated = budget.accommodationEstimated,
            accommodationActual = budget.accommodationActual,
            mealsEstimated = budget.mealsEstimated,
            mealsActual = budget.mealsActual,
            activitiesEstimated = budget.activitiesEstimated,
            activitiesActual = budget.activitiesActual,
            equipmentEstimated = budget.equipmentEstimated,
            equipmentActual = budget.equipmentActual,
            otherEstimated = budget.otherEstimated,
            otherActual = budget.otherActual,
            createdAt = budget.createdAt,
            updatedAt = budget.updatedAt
        )
        return budget
    }
    
    /**
     * Get budget by ID.
     */
    fun getBudgetById(budgetId: String): Budget? {
        return budgetQueries.selectById(budgetId).executeAsOneOrNull()?.toModel()
    }
    
    /**
     * Get budget for an event.
     */
    fun getBudgetByEventId(eventId: String): Budget? {
        return budgetQueries.selectByEventId(eventId).executeAsOneOrNull()?.toModel()
    }
    
    /**
     * Get budget with all its items.
     */
    fun getBudgetWithItems(budgetId: String): BudgetWithItems? {
        val budget = getBudgetById(budgetId) ?: return null
        val items = getBudgetItems(budgetId)
        val breakdown = BudgetCalculator.calculateCategoryBreakdown(items, budget.totalEstimated)
        
        return BudgetWithItems(
            budget = budget,
            items = items,
            categoryBreakdown = breakdown
        )
    }
    
    /**
     * Update budget.
     */
    fun updateBudget(budget: Budget): Budget {
        val now = getCurrentUtcIsoString()
        val updated = budget.copy(updatedAt = now)
        budgetQueries.updateBudget(
            totalEstimated = updated.totalEstimated,
            totalActual = updated.totalActual,
            transportEstimated = updated.transportEstimated,
            transportActual = updated.transportActual,
            accommodationEstimated = updated.accommodationEstimated,
            accommodationActual = updated.accommodationActual,
            mealsEstimated = updated.mealsEstimated,
            mealsActual = updated.mealsActual,
            activitiesEstimated = updated.activitiesEstimated,
            activitiesActual = updated.activitiesActual,
            equipmentEstimated = updated.equipmentEstimated,
            equipmentActual = updated.equipmentActual,
            otherEstimated = updated.otherEstimated,
            otherActual = updated.otherActual,
            updatedAt = updated.updatedAt,
            id = updated.id
        )
        return updated
    }
    
    /**
     * Delete budget and all its items (CASCADE).
     */
    fun deleteBudget(budgetId: String) {
        budgetQueries.deleteBudget(budgetId)
    }
    
    /**
     * Recalculate and update budget totals from items.
     * Called after any item is added/updated/deleted.
     */
    fun recalculateBudget(budgetId: String): Budget? {
        val budget = getBudgetById(budgetId) ?: return null
        val items = getBudgetItems(budgetId)
        
        val now = getCurrentUtcIsoString()
        val updated = BudgetCalculator.updateBudgetFromItems(budget, items, now)
        
        budgetQueries.updateBudget(
            totalEstimated = updated.totalEstimated,
            totalActual = updated.totalActual,
            transportEstimated = updated.transportEstimated,
            transportActual = updated.transportActual,
            accommodationEstimated = updated.accommodationEstimated,
            accommodationActual = updated.accommodationActual,
            mealsEstimated = updated.mealsEstimated,
            mealsActual = updated.mealsActual,
            activitiesEstimated = updated.activitiesEstimated,
            activitiesActual = updated.activitiesActual,
            equipmentEstimated = updated.equipmentEstimated,
            equipmentActual = updated.equipmentActual,
            otherEstimated = updated.otherEstimated,
            otherActual = updated.otherActual,
            updatedAt = updated.updatedAt,
            id = updated.id
        )
        return updated
    }
    
    // ==================== Budget Item Operations ====================
    
    /**
     * Create a new budget item.
     */
    fun createBudgetItem(
        budgetId: String,
        category: BudgetCategory,
        name: String,
        description: String,
        estimatedCost: Double,
        sharedBy: List<String>,
        notes: String = ""
    ): BudgetItem {
        val now = getCurrentUtcIsoString()
        val itemId = generateId()
        
        val item = BudgetItem(
            id = itemId,
            budgetId = budgetId,
            category = category,
            name = name,
            description = description,
            estimatedCost = estimatedCost,
            actualCost = 0.0,
            isPaid = false,
            paidBy = null,
            sharedBy = sharedBy,
            notes = notes,
            createdAt = now,
            updatedAt = now
        )
        
        // Validate before inserting
        val errors = BudgetCalculator.validateBudgetItem(item)
        if (errors.isNotEmpty()) {
            throw IllegalArgumentException("Invalid budget item: ${errors.joinToString(", ")}")
        }
        
        budgetItemQueries.insertBudgetItem(
            id = item.id,
            budgetId = item.budgetId,
            category = item.category.name,
            name = item.name,
            description = item.description,
            estimatedCost = item.estimatedCost,
            actualCost = item.actualCost,
            isPaid = if (item.isPaid) 1L else 0L,
            paidBy = item.paidBy,
            sharedBy = item.sharedBy.joinToString(","),
            notes = item.notes,
            createdAt = item.createdAt,
            updatedAt = item.updatedAt
        )
        recalculateBudget(budgetId)
        
        return item
    }
    
    /**
     * Get budget item by ID.
     */
    fun getBudgetItemById(itemId: String): BudgetItem? {
        return budgetItemQueries.selectById(itemId).executeAsOneOrNull()?.toModel()
    }
    
    /**
     * Get all items for a budget.
     */
    fun getBudgetItems(budgetId: String): List<BudgetItem> {
        return budgetItemQueries.selectByBudgetId(budgetId)
            .executeAsList()
            .map { it.toModel() }
    }
    
    /**
     * Get items by category.
     */
    fun getBudgetItemsByCategory(budgetId: String, category: BudgetCategory): List<BudgetItem> {
        return budgetItemQueries.selectByBudgetIdAndCategory(budgetId, category.name)
            .executeAsList()
            .map { it.toModel() }
    }
    
    /**
     * Get paid items.
     */
    fun getPaidItems(budgetId: String): List<BudgetItem> {
        return budgetItemQueries.selectPaidItems(budgetId)
            .executeAsList()
            .map { it.toModel() }
    }
    
    /**
     * Get unpaid items.
     */
    fun getUnpaidItems(budgetId: String): List<BudgetItem> {
        return budgetItemQueries.selectUnpaidItems(budgetId)
            .executeAsList()
            .map { it.toModel() }
    }
    
    /**
     * Get items paid by a participant.
     */
    fun getItemsPaidBy(budgetId: String, participantId: String): List<BudgetItem> {
        return budgetItemQueries.selectItemsPaidBy(budgetId, participantId)
            .executeAsList()
            .map { it.toModel() }
    }
    
    /**
     * Get items shared by a participant.
     */
    fun getItemsSharedByParticipant(budgetId: String, participantId: String): List<BudgetItem> {
        return budgetItemQueries.selectItemsSharedByParticipant(budgetId, participantId)
            .executeAsList()
            .map { it.toModel() }
    }
    
    /**
     * Update budget item.
     */
    fun updateBudgetItem(item: BudgetItem): BudgetItem {
        // Validate before updating
        val errors = BudgetCalculator.validateBudgetItem(item)
        if (errors.isNotEmpty()) {
            throw IllegalArgumentException("Invalid budget item: ${errors.joinToString(", ")}")
        }
        
        val now = getCurrentUtcIsoString()
        val updated = item.copy(updatedAt = now)
        
        budgetItemQueries.updateBudgetItem(
            category = updated.category.name,
            name = updated.name,
            description = updated.description,
            estimatedCost = updated.estimatedCost,
            actualCost = updated.actualCost,
            isPaid = if (updated.isPaid) 1L else 0L,
            paidBy = updated.paidBy,
            sharedBy = updated.sharedBy.joinToString(","),
            notes = updated.notes,
            updatedAt = updated.updatedAt,
            id = updated.id
        )
        recalculateBudget(item.budgetId)
        
        return updated
    }
    
    /**
     * Mark item as paid.
     */
    fun markItemAsPaid(itemId: String, actualCost: Double, paidBy: String): BudgetItem {
        val item = getBudgetItemById(itemId)
            ?: throw IllegalArgumentException("Budget item not found: $itemId")
        
        if (actualCost <= 0.0) {
            throw IllegalArgumentException("Actual cost must be positive")
        }
        
        val now = getCurrentUtcIsoString()
        val updated = item.copy(
            isPaid = true,
            actualCost = actualCost,
            paidBy = paidBy,
            updatedAt = now
        )
        
        budgetItemQueries.markAsPaid(
            id = itemId,
            actualCost = actualCost,
            paidBy = paidBy,
            updatedAt = now
        )
        
        recalculateBudget(item.budgetId)
        
        return updated
    }
    
    /**
     * Delete budget item.
     */
    fun deleteBudgetItem(itemId: String) {
        val item = getBudgetItemById(itemId)
        budgetItemQueries.deleteBudgetItem(itemId)
        
        // Recalculate budget if item was found
        item?.let { recalculateBudget(it.budgetId) }
    }
    
    // ==================== Participant Operations ====================
    
    /**
     * Get budget share details for a participant.
     */
    fun getParticipantBudgetShare(budgetId: String, participantId: String): ParticipantBudgetShare {
        val items = getBudgetItems(budgetId)
        return BudgetCalculator.calculateParticipantBudgetShare(participantId, items)
    }
    
    /**
     * Get balances for all participants in a budget.
     */
    fun getParticipantBalances(budgetId: String): Map<String, Double> {
        val items = getBudgetItems(budgetId)
        return BudgetCalculator.calculateBalances(items)
    }
    
    /**
     * Get settlement suggestions for a budget.
     */
    fun getSettlements(budgetId: String): List<Triple<String, String, Double>> {
        val items = getBudgetItems(budgetId)
        return BudgetCalculator.calculateSettlements(items)
    }
    
    // ==================== Statistics ====================
    
    /**
     * Count items in a budget.
     */
    fun countItems(budgetId: String): Long {
        return budgetItemQueries.countByBudgetId(budgetId).executeAsOne()
    }
    
    /**
     * Count paid items.
     */
    fun countPaidItems(budgetId: String): Long {
        return budgetItemQueries.countPaidItems(budgetId).executeAsOne()
    }
    
    /**
     * Sum actual costs by category.
     */
    fun sumActualByCategory(budgetId: String, category: BudgetCategory): Double {
        return budgetItemQueries.sumActualByCategory(budgetId, category.name).executeAsOne()
    }
    
    /**
     * Sum estimated costs by category.
     */
    fun sumEstimatedByCategory(budgetId: String, category: BudgetCategory): Double {
        return budgetItemQueries.sumEstimatedByCategory(budgetId, category.name).executeAsOne()
    }
    
    // ==================== Utility ====================
    
    /**
     * Generate unique ID for budget/item.
     */
    private fun generateId(): String {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        return "budget-${timestamp}-${(0..9999).random()}"
    }
    
    /**
     * Get current UTC timestamp as ISO string.
     */
    private fun getCurrentUtcIsoString(): String {
        // For Phase 2, we use a fixed test date
        // In Phase 3+, integrate with kotlinx.datetime for full timezone support
        return "2025-12-25T10:00:00Z"
    }
    
    // ==================== Mappers ====================
    
    /**
     * Convert SQLDelight Budget entity to Kotlin model.
     */
    private fun SqlBudget.toModel(): Budget {
        return Budget(
            id = id,
            eventId = eventId,
            totalEstimated = totalEstimated,
            totalActual = totalActual,
            transportEstimated = transportEstimated,
            transportActual = transportActual,
            accommodationEstimated = accommodationEstimated,
            accommodationActual = accommodationActual,
            mealsEstimated = mealsEstimated,
            mealsActual = mealsActual,
            activitiesEstimated = activitiesEstimated,
            activitiesActual = activitiesActual,
            equipmentEstimated = equipmentEstimated,
            equipmentActual = equipmentActual,
            otherEstimated = otherEstimated,
            otherActual = otherActual,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
    
    /**
     * Convert SQLDelight BudgetItem entity to Kotlin model.
     */
    private fun SqlBudgetItem.toModel(): BudgetItem {
        return BudgetItem(
            id = id,
            budgetId = budgetId,
            category = BudgetCategory.valueOf(category),
            name = name,
            description = description,
            estimatedCost = estimatedCost,
            actualCost = actualCost,
            isPaid = isPaid == 1L,
            paidBy = paidBy,
            sharedBy = if (sharedBy.isBlank()) emptyList() else sharedBy.split(","),
            notes = notes,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
