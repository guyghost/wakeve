package com.guyghost.wakeve.models

import kotlinx.serialization.Serializable

/**
 * Budget categories for event planning.
 * Organized by main expense types.
 */
@Serializable
enum class BudgetCategory {
    /** Transport costs (flights, trains, car rental, gas, etc.) */
    TRANSPORT,
    
    /** Accommodation costs (hotels, rentals, camping, etc.) */
    ACCOMMODATION,
    
    /** Meal costs (restaurants, groceries, catering, etc.) */
    MEALS,
    
    /** Activity costs (tickets, gear rental, guides, etc.) */
    ACTIVITIES,
    
    /** Equipment costs (purchase or rental of specific equipment) */
    EQUIPMENT,
    
    /** Miscellaneous costs that don't fit other categories */
    OTHER
}

/**
 * Main budget entity for an event.
 * Tracks total budget with breakdown by category.
 */
@Serializable
data class Budget(
    val id: String,
    val eventId: String,
    val totalEstimated: Double,           // Total estimated budget
    val totalActual: Double,              // Actual total spent (updated from items)
    val transportEstimated: Double,       // Estimated for TRANSPORT category
    val transportActual: Double,          // Actual for TRANSPORT category
    val accommodationEstimated: Double,   // Estimated for ACCOMMODATION
    val accommodationActual: Double,      // Actual for ACCOMMODATION
    val mealsEstimated: Double,           // Estimated for MEALS
    val mealsActual: Double,              // Actual for MEALS
    val activitiesEstimated: Double,      // Estimated for ACTIVITIES
    val activitiesActual: Double,         // Actual for ACTIVITIES
    val equipmentEstimated: Double,       // Estimated for EQUIPMENT
    val equipmentActual: Double,          // Actual for EQUIPMENT
    val otherEstimated: Double,           // Estimated for OTHER
    val otherActual: Double,              // Actual for OTHER
    val createdAt: String,                // ISO timestamp (UTC)
    val updatedAt: String                 // ISO timestamp (UTC)
) {
    /**
     * Get estimated budget for a specific category.
     */
    fun getEstimatedForCategory(category: BudgetCategory): Double = when (category) {
        BudgetCategory.TRANSPORT -> transportEstimated
        BudgetCategory.ACCOMMODATION -> accommodationEstimated
        BudgetCategory.MEALS -> mealsEstimated
        BudgetCategory.ACTIVITIES -> activitiesEstimated
        BudgetCategory.EQUIPMENT -> equipmentEstimated
        BudgetCategory.OTHER -> otherEstimated
    }
    
    /**
     * Get actual budget for a specific category.
     */
    fun getActualForCategory(category: BudgetCategory): Double = when (category) {
        BudgetCategory.TRANSPORT -> transportActual
        BudgetCategory.ACCOMMODATION -> accommodationActual
        BudgetCategory.MEALS -> mealsActual
        BudgetCategory.ACTIVITIES -> activitiesActual
        BudgetCategory.EQUIPMENT -> equipmentActual
        BudgetCategory.OTHER -> otherActual
    }
    
    /**
     * Calculate percentage of budget used for a category.
     */
    fun getCategoryPercentage(category: BudgetCategory): Double {
        val estimated = getEstimatedForCategory(category)
        return if (totalEstimated > 0.0) {
            (estimated / totalEstimated) * 100.0
        } else {
            0.0
        }
    }
    
    /**
     * Calculate overall budget usage percentage.
     */
    val budgetUsagePercentage: Double
        get() = if (totalEstimated > 0.0) {
            (totalActual / totalEstimated) * 100.0
        } else {
            0.0
        }
    
    /**
     * Check if budget is exceeded.
     */
    val isOverBudget: Boolean
        get() = totalActual > totalEstimated
    
    /**
     * Calculate remaining budget.
     */
    val remainingBudget: Double
        get() = totalEstimated - totalActual
}

/**
 * Individual budget item (expense).
 */
@Serializable
data class BudgetItem(
    val id: String,
    val budgetId: String,                // Reference to parent budget
    val category: BudgetCategory,        // Category of this item
    val name: String,                    // Item name (e.g., "Train Paris-Lyon")
    val description: String,             // Detailed description
    val estimatedCost: Double,           // Estimated cost
    val actualCost: Double,              // Actual cost (0.0 if not yet paid)
    val isPaid: Boolean,                 // Whether the expense has been paid
    val paidBy: String?,                 // Participant ID who paid (null if not paid)
    val sharedBy: List<String>,          // List of participant IDs sharing this cost
    val notes: String,                   // Additional notes
    val createdAt: String,               // ISO timestamp (UTC)
    val updatedAt: String                // ISO timestamp (UTC)
) {
    /**
     * Calculate cost per person for this item.
     */
    val costPerPerson: Double
        get() {
            val cost = if (actualCost > 0.0) actualCost else estimatedCost
            return if (sharedBy.isNotEmpty()) cost / sharedBy.size else cost
        }
    
    /**
     * Get the relevant cost (actual if paid, estimated otherwise).
     */
    val relevantCost: Double
        get() = if (isPaid && actualCost > 0.0) actualCost else estimatedCost
}

/**
 * Category details for budget breakdown.
 * Used for UI display and analysis.
 */
data class BudgetCategoryDetails(
    val category: BudgetCategory,
    val estimated: Double,
    val actual: Double,
    val itemCount: Int,
    val paidItemCount: Int,
    val percentage: Double
) {
    val isOverBudget: Boolean
        get() = actual > estimated
    
    val remaining: Double
        get() = estimated - actual
    
    val usagePercentage: Double
        get() = if (estimated > 0.0) {
            (actual / estimated) * 100.0
        } else {
            0.0
        }
}

/**
 * Budget with all its items.
 * Used for complete budget view.
 */
data class BudgetWithItems(
    val budget: Budget,
    val items: List<BudgetItem>,
    val categoryBreakdown: List<BudgetCategoryDetails>
)

/**
 * Participant's share of the budget.
 * Used for cost splitting and tracking.
 */
data class ParticipantBudgetShare(
    val participantId: String,
    val totalOwed: Double,              // Total amount this participant owes
    val totalPaid: Double,              // Total amount this participant has paid
    val itemsShared: List<BudgetItem>,  // Items this participant is sharing
    val itemsPaid: List<BudgetItem>     // Items this participant has paid for
) {
    /**
     * Calculate balance (positive = owes money, negative = is owed money).
     */
    val balance: Double
        get() = totalOwed - totalPaid
    
    val owesMore: Boolean
        get() = balance > 0.0
    
    val isOwed: Boolean
        get() = balance < 0.0
    
    val isBalanced: Boolean
        get() = kotlin.math.abs(balance) < 0.01 // Within 1 cent
}
