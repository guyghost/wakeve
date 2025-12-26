package com.guyghost.wakeve.budget

import com.guyghost.wakeve.models.*
import kotlin.math.roundToInt

/**
 * Budget Calculator - Business logic for budget calculations.
 * 
 * Handles:
 * - Automatic budget aggregation from items
 * - Category-wise calculations
 * - Per-person cost splitting
 * - Balance calculations between participants
 */
object BudgetCalculator {
    
    /**
     * Format a Double to string with 2 decimal places
     */
    private fun Double.format2(): String {
        val rounded = ((this * 100).roundToInt()) / 100.0
        val intPart = rounded.toInt()
        val decPart = ((rounded - intPart) * 100).roundToInt()
        return "$intPart.${decPart.toString().padStart(2, '0')}"
    }
    
    /**
     * Format a Double to string with 1 decimal place
     */
    private fun Double.format1(): String {
        val rounded = ((this * 10).roundToInt()) / 10.0
        val intPart = rounded.toInt()
        val decPart = ((rounded - intPart) * 10).roundToInt()
        return "$intPart.$decPart"
    }
    /**
     * Calculate total budget from a list of budget items.
     * 
     * @param items List of budget items
     * @return Pair of (totalEstimated, totalActual)
     */
    fun calculateTotalBudget(items: List<BudgetItem>): Pair<Double, Double> {
        val totalEstimated = items.sumOf { it.estimatedCost }
        val totalActual = items.filter { it.isPaid == true }.sumOf { it.actualCost }
        return Pair(totalEstimated, totalActual)
    }
    
    /**
     * Calculate budget totals by category.
     * 
     * @param items List of budget items
     * @param category Category to calculate for
     * @return Pair of (estimated, actual) for the category
     */
    fun calculateCategoryBudget(
        items: List<BudgetItem>,
        category: BudgetCategory
    ): Pair<Double, Double> {
        val categoryItems = items.filter { it.category == category }
        val estimated = categoryItems.sumOf { it.estimatedCost }
        val actual = categoryItems.filter { it.isPaid == true }.sumOf { it.actualCost }
        return Pair(estimated, actual)
    }
    
    /**
     * Calculate budget breakdown for all categories.
     * 
     * @param items List of budget items
     * @param totalEstimated Total estimated budget (for percentage calculation)
     * @return List of BudgetCategoryDetails for each category
     */
    fun calculateCategoryBreakdown(
        items: List<BudgetItem>,
        totalEstimated: Double
    ): List<BudgetCategoryDetails> {
        return BudgetCategory.values().map { category ->
            val categoryItems = items.filter { it.category == category }
            val estimated = categoryItems.sumOf { it.estimatedCost }
            val actual = categoryItems.filter { it.isPaid == true }.sumOf { it.actualCost }
            val itemCount = categoryItems.size
            val paidItemCount = categoryItems.count { it.isPaid == true }
            val percentage = if (totalEstimated > 0.0) {
                (estimated / totalEstimated) * 100.0
            } else {
                0.0
            }
            
            BudgetCategoryDetails(
                category = category,
                estimated = estimated,
                actual = actual,
                itemCount = itemCount,
                paidItemCount = paidItemCount,
                percentage = percentage
            )
        }.filter { it.itemCount > 0 } // Only return categories with items
    }
    
    /**
     * Calculate per-person budget for an event.
     * 
     * @param budget Budget entity
     * @param participantCount Number of participants
     * @return Pair of (estimatedPerPerson, actualPerPerson)
     */
    fun calculatePerPersonBudget(
        budget: Budget,
        participantCount: Int
    ): Pair<Double, Double> {
        if (participantCount <= 0) return Pair(0.0, 0.0)
        
        val estimatedPerPerson = budget.totalEstimated / participantCount
        val actualPerPerson = budget.totalActual / participantCount
        
        return Pair(estimatedPerPerson, actualPerPerson)
    }
    
    /**
     * Update budget entity with new values calculated from items.
     * This creates a new Budget instance with updated totals.
     * 
     * @param budget Current budget
     * @param items List of all budget items
     * @param updatedAt New timestamp for updatedAt field
     * @return Updated Budget instance
     */
    fun updateBudgetFromItems(
        budget: Budget,
        items: List<BudgetItem>,
        updatedAt: String
    ): Budget {
        val categoryTotals = BudgetCategory.values().associateWith { category ->
            calculateCategoryBudget(items, category)
        }
        
        val (totalEst, totalAct) = calculateTotalBudget(items)
        
        return budget.copy(
            totalEstimated = totalEst,
            totalActual = totalAct,
            transportEstimated = categoryTotals[BudgetCategory.TRANSPORT]?.first ?: 0.0,
            transportActual = categoryTotals[BudgetCategory.TRANSPORT]?.second ?: 0.0,
            accommodationEstimated = categoryTotals[BudgetCategory.ACCOMMODATION]?.first ?: 0.0,
            accommodationActual = categoryTotals[BudgetCategory.ACCOMMODATION]?.second ?: 0.0,
            mealsEstimated = categoryTotals[BudgetCategory.MEALS]?.first ?: 0.0,
            mealsActual = categoryTotals[BudgetCategory.MEALS]?.second ?: 0.0,
            activitiesEstimated = categoryTotals[BudgetCategory.ACTIVITIES]?.first ?: 0.0,
            activitiesActual = categoryTotals[BudgetCategory.ACTIVITIES]?.second ?: 0.0,
            equipmentEstimated = categoryTotals[BudgetCategory.EQUIPMENT]?.first ?: 0.0,
            equipmentActual = categoryTotals[BudgetCategory.EQUIPMENT]?.second ?: 0.0,
            otherEstimated = categoryTotals[BudgetCategory.OTHER]?.first ?: 0.0,
            otherActual = categoryTotals[BudgetCategory.OTHER]?.second ?: 0.0,
            updatedAt = updatedAt
        )
    }
    
    /**
     * Calculate how much each participant owes for a specific item.
     * 
     * @param item Budget item
     * @return Map of participantId to amount owed
     */
    fun calculateItemSharePerParticipant(item: BudgetItem): Map<String, Double> {
        val cost = if (item.isPaid && item.actualCost > 0.0) item.actualCost else item.estimatedCost
        val shareCount = item.sharedBy.size
        
        if (shareCount == 0) return emptyMap()
        
        val costPerPerson = cost / shareCount
        
        return item.sharedBy.associateWith { costPerPerson }
    }
    
    /**
     * Calculate total amount each participant owes.
     * 
     * @param items List of all budget items
     * @return Map of participantId to total amount owed
     */
    fun calculateParticipantShares(items: List<BudgetItem>): Map<String, Double> {
        val shares = mutableMapOf<String, Double>()
        
        items.forEach { item ->
            val itemShares = calculateItemSharePerParticipant(item)
            itemShares.forEach { (participantId, amount) ->
                shares[participantId] = (shares[participantId] ?: 0.0) + amount
            }
        }
        
        return shares
    }
    
    /**
     * Calculate total amount each participant has paid.
     * 
     * @param items List of all budget items
     * @return Map of participantId to total amount paid
     */
    fun calculateParticipantPayments(items: List<BudgetItem>): Map<String, Double> {
        val payments = mutableMapOf<String, Double>()
        
        items.filter { it.isPaid == true && it.paidBy != null }.forEach { item ->
            val paidBy = item.paidBy!!
            payments[paidBy] = (payments[paidBy] ?: 0.0) + item.actualCost
        }
        
        return payments
    }
    
    /**
     * Calculate budget share details for a specific participant.
     * 
     * @param participantId ID of the participant
     * @param items List of all budget items
     * @return ParticipantBudgetShare with complete details
     */
    fun calculateParticipantBudgetShare(
        participantId: String,
        items: List<BudgetItem>
    ): ParticipantBudgetShare {
        val itemsShared = items.filter { participantId in it.sharedBy }
        val itemsPaid = items.filter { it.paidBy == participantId }
        
        val totalOwed = itemsShared.sumOf { 
            val cost = if (it.isPaid && it.actualCost > 0.0) it.actualCost else it.estimatedCost
            cost / it.sharedBy.size
        }
        val totalPaid = itemsPaid.filter { it.isPaid == true }.sumOf { it.actualCost }
        
        return ParticipantBudgetShare(
            participantId = participantId,
            totalOwed = totalOwed,
            totalPaid = totalPaid,
            itemsShared = itemsShared,
            itemsPaid = itemsPaid
        )
    }
    
    /**
     * Calculate balances between all participants.
     * Positive balance = owes money, Negative balance = is owed money.
     * 
     * @param items List of all budget items
     * @return Map of participantId to balance
     */
    fun calculateBalances(items: List<BudgetItem>): Map<String, Double> {
        val shares = calculateParticipantShares(items)
        val payments = calculateParticipantPayments(items)
        
        // Get all unique participant IDs
        val allParticipants = (shares.keys + payments.keys).toSet()
        
        return allParticipants.associateWith { participantId ->
            val owed = shares[participantId] ?: 0.0
            val paid = payments[participantId] ?: 0.0
            owed - paid // Positive = owes money, Negative = is owed
        }
    }
    
    /**
     * Calculate simplified debt settlements using a greedy algorithm.
     * Minimizes the number of transactions needed to settle all debts.
     * 
     * @param items List of all budget items
     * @return List of (from, to, amount) tuples representing settlements
     */
    fun calculateSettlements(items: List<BudgetItem>): List<Triple<String, String, Double>> {
        val balances = calculateBalances(items).toMutableMap()
        val settlements = mutableListOf<Triple<String, String, Double>>()
        
        // Filter out balanced participants
        val debtors = balances.filter { it.value > 0.01 }.toMutableMap() // Owes money
        val creditors = balances.filter { it.value < -0.01 }.toMutableMap() // Is owed
        
        // Greedy settlement algorithm
        while (debtors.isNotEmpty() && creditors.isNotEmpty()) {
            val (debtor, debtAmount) = debtors.entries.first()
            val (creditor, creditAmount) = creditors.entries.first()
            
            val settlementAmount = minOf(debtAmount, -creditAmount)
            
            settlements.add(Triple(debtor, creditor, settlementAmount))
            
            // Update balances
            debtors[debtor] = debtAmount - settlementAmount
            creditors[creditor] = creditAmount + settlementAmount
            
            // Remove if settled
            if (debtors[debtor]!! < 0.01) debtors.remove(debtor)
            if (creditors[creditor]!! > -0.01) creditors.remove(creditor)
        }
        
        return settlements
    }
    
    /**
     * Validate a budget item before creation/update.
     * 
     * @param item Budget item to validate
     * @return List of validation errors (empty if valid)
     */
    fun validateBudgetItem(item: BudgetItem): List<String> {
        val errors = mutableListOf<String>()
        
        if (item.name.isBlank()) {
            errors.add("Item name cannot be blank")
        }
        
        if (item.estimatedCost < 0.0) {
            errors.add("Estimated cost cannot be negative")
        }
        
        if (item.actualCost < 0.0) {
            errors.add("Actual cost cannot be negative")
        }
        
        if (item.isPaid == true && item.paidBy == null) {
            errors.add("Paid items must have a paidBy participant")
        }
        
        if (item.isPaid == true && item.actualCost <= 0.0) {
            errors.add("Paid items must have a positive actual cost")
        }
        
        if (item.sharedBy.isEmpty()) {
            errors.add("Item must be shared by at least one participant")
        }
        
        return errors
    }
    
    /**
     * Validate a budget before creation/update.
     * 
     * @param budget Budget to validate
     * @return List of validation errors (empty if valid)
     */
    fun validateBudget(budget: Budget): List<String> {
        val errors = mutableListOf<String>()
        
        if (budget.totalEstimated < 0.0) {
            errors.add("Total estimated cannot be negative")
        }
        
        if (budget.totalActual < 0.0) {
            errors.add("Total actual cannot be negative")
        }
        
        // Validate all category amounts are non-negative
        val categories = listOf(
            "transport" to listOf(budget.transportEstimated, budget.transportActual),
            "accommodation" to listOf(budget.accommodationEstimated, budget.accommodationActual),
            "meals" to listOf(budget.mealsEstimated, budget.mealsActual),
            "activities" to listOf(budget.activitiesEstimated, budget.activitiesActual),
            "equipment" to listOf(budget.equipmentEstimated, budget.equipmentActual),
            "other" to listOf(budget.otherEstimated, budget.otherActual)
        )
        
        categories.forEach { (categoryName, amounts) ->
            amounts.forEachIndexed { index, amount ->
                if (amount < 0.0) {
                    val type = if (index == 0) "estimated" else "actual"
                    errors.add("$categoryName $type cannot be negative")
                }
            }
        }
        
        return errors
    }
    
    /**
     * Check if budget is within limits (not over budget).
     * 
     * @param budget Budget to check
     * @return true if within budget, false if over budget
     */
    fun isWithinBudget(budget: Budget): Boolean {
        return budget.totalActual <= budget.totalEstimated
    }
    
    /**
     * Calculate budget usage percentage by category.
     * 
     * @param budget Budget entity
     * @return Map of BudgetCategory to usage percentage
     */
    fun calculateCategoryUsagePercentages(budget: Budget): Map<BudgetCategory, Double> {
        return BudgetCategory.values().associateWith { category ->
            val estimated = when (category) {
                BudgetCategory.TRANSPORT -> budget.transportEstimated
                BudgetCategory.ACCOMMODATION -> budget.accommodationEstimated
                BudgetCategory.MEALS -> budget.mealsEstimated
                BudgetCategory.ACTIVITIES -> budget.activitiesEstimated
                BudgetCategory.EQUIPMENT -> budget.equipmentEstimated
                BudgetCategory.OTHER -> budget.otherEstimated
            }
            val actual = when (category) {
                BudgetCategory.TRANSPORT -> budget.transportActual
                BudgetCategory.ACCOMMODATION -> budget.accommodationActual
                BudgetCategory.MEALS -> budget.mealsActual
                BudgetCategory.ACTIVITIES -> budget.activitiesActual
                BudgetCategory.EQUIPMENT -> budget.equipmentActual
                BudgetCategory.OTHER -> budget.otherActual
            }
            
            if (estimated > 0.0) {
                (actual / estimated) * 100.0
            } else {
                0.0
            }
        }
    }
    
    /**
     * Find categories that are over budget.
     * 
     * @param budget Budget entity
     * @return List of categories that exceeded their estimated budget
     */
    fun findOverBudgetCategories(budget: Budget): List<BudgetCategory> {
        return BudgetCategory.values().filter { category ->
            val estimated = when (category) {
                BudgetCategory.TRANSPORT -> budget.transportEstimated
                BudgetCategory.ACCOMMODATION -> budget.accommodationEstimated
                BudgetCategory.MEALS -> budget.mealsEstimated
                BudgetCategory.ACTIVITIES -> budget.activitiesEstimated
                BudgetCategory.EQUIPMENT -> budget.equipmentEstimated
                BudgetCategory.OTHER -> budget.otherEstimated
            }
            val actual = when (category) {
                BudgetCategory.TRANSPORT -> budget.transportActual
                BudgetCategory.ACCOMMODATION -> budget.accommodationActual
                BudgetCategory.MEALS -> budget.mealsActual
                BudgetCategory.ACTIVITIES -> budget.activitiesActual
                BudgetCategory.EQUIPMENT -> budget.equipmentActual
                BudgetCategory.OTHER -> budget.otherActual
            }
            actual > estimated
        }
    }
    
    /**
     * Generate a budget summary report.
     * 
     * @param budget Budget entity
     * @param items List of budget items
     * @param participantCount Number of participants
     * @return Human-readable summary string
     */
    fun generateBudgetSummary(
        budget: Budget,
        items: List<BudgetItem>,
        participantCount: Int
    ): String {
        val (estimatedPP, actualPP) = calculatePerPersonBudget(budget, participantCount)
        val categoryBreakdown = calculateCategoryBreakdown(items, budget.totalEstimated)
        val overBudgetCategories = findOverBudgetCategories(budget)
        
        return buildString {
            appendLine("=== Budget Summary ===")
            appendLine("Total Estimated: €${budget.totalEstimated.format2()}")
            appendLine("Total Actual: €${budget.totalActual.format2()}")
            appendLine("Per Person Estimated: €${estimatedPP.format2()}")
            appendLine("Per Person Actual: €${actualPP.format2()}")
            val budgetUsagePercentage = if (budget.totalEstimated > 0.0) {
                (budget.totalActual / budget.totalEstimated) * 100.0
            } else {
                0.0
            }
            val isOverBudget = budget.totalActual > budget.totalEstimated
            appendLine("Budget Usage: ${budgetUsagePercentage.format1()}%")
            appendLine("Status: ${if (isOverBudget) "OVER BUDGET ⚠️" else "Within Budget ✓"}")
            appendLine()
            appendLine("=== By Category ===")
            categoryBreakdown.forEach { detail ->
                appendLine("${detail.category.name}:")
                appendLine("  Estimated: €${detail.estimated.format2()} (${detail.percentage.format1()}%)")
                appendLine("  Actual: €${detail.actual.format2()}")
                appendLine("  Items: ${detail.paidItemCount}/${detail.itemCount} paid")
                if (detail.isOverBudget) {
                    appendLine("  ⚠️ OVER BUDGET by €${(detail.actual - detail.estimated).format2()}")
                }
            }
            
            if (overBudgetCategories.isNotEmpty()) {
                appendLine()
                appendLine("⚠️ Categories over budget: ${overBudgetCategories.joinToString(", ") { it.name }}")
            }
        }
    }
}
