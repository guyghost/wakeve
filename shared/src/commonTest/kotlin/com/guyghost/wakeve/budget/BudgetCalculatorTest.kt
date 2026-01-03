package com.guyghost.wakeve.budget

import com.guyghost.wakeve.models.Budget
import com.guyghost.wakeve.models.BudgetCategory
import com.guyghost.wakeve.models.BudgetItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BudgetCalculatorTest {
    
    private fun createTestBudget(): Budget {
        return Budget(
            id = "budget-1",
            eventId = "event-1",
            totalEstimated = 1000.0,
            totalActual = 500.0,
            transportEstimated = 400.0,
            transportActual = 200.0,
            accommodationEstimated = 300.0,
            accommodationActual = 200.0,
            mealsEstimated = 200.0,
            mealsActual = 100.0,
            activitiesEstimated = 100.0,
            activitiesActual = 0.0,
            equipmentEstimated = 0.0,
            equipmentActual = 0.0,
            otherEstimated = 0.0,
            otherActual = 0.0,
            createdAt = "2025-12-25T10:00:00Z",
            updatedAt = "2025-12-25T10:00:00Z"
        )
    }
    
    private fun createTestItems(): List<BudgetItem> {
        return listOf(
            BudgetItem(
                id = "item-1",
                budgetId = "budget-1",
                category = BudgetCategory.TRANSPORT,
                name = "Train tickets",
                description = "Paris to Lyon",
                estimatedCost = 200.0,
                actualCost = 180.0,
                isPaid = true,
                paidBy = "user-1",
                sharedBy = listOf("user-1", "user-2", "user-3"),
                notes = "",
                createdAt = "2025-12-25T10:00:00Z",
                updatedAt = "2025-12-25T10:00:00Z"
            ),
            BudgetItem(
                id = "item-2",
                budgetId = "budget-1",
                category = BudgetCategory.ACCOMMODATION,
                name = "Hotel",
                description = "3 nights",
                estimatedCost = 300.0,
                actualCost = 0.0,
                isPaid = false,
                paidBy = null,
                sharedBy = listOf("user-1", "user-2", "user-3"),
                notes = "",
                createdAt = "2025-12-25T10:00:00Z",
                updatedAt = "2025-12-25T10:00:00Z"
            ),
            BudgetItem(
                id = "item-3",
                budgetId = "budget-1",
                category = BudgetCategory.MEALS,
                name = "Groceries",
                description = "Food shopping",
                estimatedCost = 150.0,
                actualCost = 120.0,
                isPaid = true,
                paidBy = "user-2",
                sharedBy = listOf("user-1", "user-2", "user-3"),
                notes = "",
                createdAt = "2025-12-25T10:00:00Z",
                updatedAt = "2025-12-25T10:00:00Z"
            )
        )
    }
    
    @Test
    fun testCalculateTotalBudget() {
        val items = createTestItems()
        val (estimated, actual) = BudgetCalculator.calculateTotalBudget(items)
        
        assertEquals(650.0, estimated, "Total estimated should sum all items")
        assertEquals(300.0, actual, "Total actual should sum only paid items")
    }
    
    @Test
    fun testCalculateCategoryBudget() {
        val items = createTestItems()
        val (estimated, actual) = BudgetCalculator.calculateCategoryBudget(
            items,
            BudgetCategory.TRANSPORT
        )
        
        assertEquals(200.0, estimated)
        assertEquals(180.0, actual)
    }
    
    @Test
    fun testCalculateCategoryBudgetEmptyCategory() {
        val items = createTestItems()
        val (estimated, actual) = BudgetCalculator.calculateCategoryBudget(
            items,
            BudgetCategory.EQUIPMENT
        )
        
        assertEquals(0.0, estimated)
        assertEquals(0.0, actual)
    }
    
    @Test
    fun testCalculateCategoryBreakdown() {
        val items = createTestItems()
        val breakdown = BudgetCalculator.calculateCategoryBreakdown(items, 650.0)
        
        // Should only return categories with items
        assertEquals(3, breakdown.size)
        
        val transportBreakdown = breakdown.find { it.category == BudgetCategory.TRANSPORT }!!
        assertEquals(200.0, transportBreakdown.estimated)
        assertEquals(180.0, transportBreakdown.actual)
        assertEquals(1, transportBreakdown.itemCount)
        assertEquals(1, transportBreakdown.paidItemCount)
        assertTrue(transportBreakdown.percentage > 30.0) // ~30.77%
    }
    
    @Test
    fun testCalculatePerPersonBudget() {
        val budget = createTestBudget()
        val (estimatedPP, actualPP) = BudgetCalculator.calculatePerPersonBudget(budget, 4)
        
        assertEquals(250.0, estimatedPP)
        assertEquals(125.0, actualPP)
    }
    
    @Test
    fun testCalculatePerPersonBudgetZeroParticipants() {
        val budget = createTestBudget()
        val (estimatedPP, actualPP) = BudgetCalculator.calculatePerPersonBudget(budget, 0)
        
        assertEquals(0.0, estimatedPP)
        assertEquals(0.0, actualPP)
    }
    
    @Test
    fun testUpdateBudgetFromItems() {
        val budget = createTestBudget()
        val items = createTestItems()
        val updated = BudgetCalculator.updateBudgetFromItems(
            budget,
            items,
            "2025-12-25T11:00:00Z"
        )
        
        assertEquals(650.0, updated.totalEstimated)
        assertEquals(300.0, updated.totalActual)
        assertEquals(200.0, updated.transportEstimated)
        assertEquals(180.0, updated.transportActual)
        assertEquals(300.0, updated.accommodationEstimated)
        assertEquals(0.0, updated.accommodationActual)
        assertEquals(150.0, updated.mealsEstimated)
        assertEquals(120.0, updated.mealsActual)
        assertEquals("2025-12-25T11:00:00Z", updated.updatedAt)
    }
    
    @Test
    fun testCalculateItemSharePerParticipant() {
        val item = createTestItems()[0] // Transport item, 180.0 actual, 3 participants
        val shares = BudgetCalculator.calculateItemSharePerParticipant(item)
        
        assertEquals(3, shares.size)
        assertEquals(60.0, shares["user-1"])
        assertEquals(60.0, shares["user-2"])
        assertEquals(60.0, shares["user-3"])
    }
    
    @Test
    fun testCalculateItemShareForUnpaidItem() {
        val item = createTestItems()[1] // Accommodation, unpaid, uses estimated
        val shares = BudgetCalculator.calculateItemSharePerParticipant(item)
        
        assertEquals(3, shares.size)
        assertEquals(100.0, shares["user-1"]) // 300.0 / 3
        assertEquals(100.0, shares["user-2"])
        assertEquals(100.0, shares["user-3"])
    }
    
    @Test
    fun testCalculateParticipantShares() {
        val items = createTestItems()
        val shares = BudgetCalculator.calculateParticipantShares(items)
        
        // Each participant shares in all 3 items:
        // Transport: 180/3 = 60
        // Accommodation: 300/3 = 100 (uses estimated since unpaid)
        // Meals: 120/3 = 40
        // Total: 200 per person
        
        assertEquals(200.0, shares["user-1"]!!, 0.01)
        assertEquals(200.0, shares["user-2"]!!, 0.01)
        assertEquals(200.0, shares["user-3"]!!, 0.01)
    }
    
    @Test
    fun testCalculateParticipantPayments() {
        val items = createTestItems()
        val payments = BudgetCalculator.calculateParticipantPayments(items)
        
        assertEquals(180.0, payments["user-1"]) // Paid for transport
        assertEquals(120.0, payments["user-2"]) // Paid for meals
        assertNull(payments["user-3"]) // Hasn't paid anything
    }
    
    @Test
    fun testCalculateParticipantBudgetShare() {
        val items = createTestItems()
        val share = BudgetCalculator.calculateParticipantBudgetShare("user-1", items)
        
        assertEquals("user-1", share.participantId)
        assertEquals(200.0, share.totalOwed, 0.01)
        assertEquals(180.0, share.totalPaid)
        assertEquals(3, share.itemsShared.size)
        assertEquals(1, share.itemsPaid.size)
        assertTrue(share.owesMore) // Owes 20 more
        assertFalse(share.isOwed)
        assertFalse(share.isBalanced)
    }
    
    @Test
    fun testCalculateBalances() {
        val items = createTestItems()
        val balances = BudgetCalculator.calculateBalances(items)
        
        // user-1: owes 200, paid 180 = +20 (owes 20)
        // user-2: owes 200, paid 120 = +80 (owes 80)
        // user-3: owes 200, paid 0 = +200 (owes 200)
        
        assertEquals(20.0, balances["user-1"]!!, 0.01)
        assertEquals(80.0, balances["user-2"]!!, 0.01)
        assertEquals(200.0, balances["user-3"]!!, 0.01)
    }
    
    @Test
    fun testCalculateBalancesWithCreditor() {
        val items = listOf(
            BudgetItem(
                id = "item-1",
                budgetId = "budget-1",
                category = BudgetCategory.TRANSPORT,
                name = "Flight",
                description = "Round trip",
                estimatedCost = 300.0,
                actualCost = 300.0,
                isPaid = true,
                paidBy = "user-1",
                sharedBy = listOf("user-1", "user-2"),
                notes = "",
                createdAt = "2025-12-25T10:00:00Z",
                updatedAt = "2025-12-25T10:00:00Z"
            )
        )
        
        val balances = BudgetCalculator.calculateBalances(items)
        
        // user-1: owes 150, paid 300 = -150 (is owed 150)
        // user-2: owes 150, paid 0 = +150 (owes 150)
        
        assertEquals(-150.0, balances["user-1"]!!, 0.01)
        assertEquals(150.0, balances["user-2"]!!, 0.01)
    }
    
    @Test
    fun testCalculateSettlements() {
        val items = createTestItems()
        val settlements = BudgetCalculator.calculateSettlements(items)
        
        // With current test data, everyone owes money (no creditors)
        // So there are no settlements possible
        // This is expected when only partial payments have been made
        assertTrue(settlements.isEmpty(), "Settlements should be empty when everyone owes money")
    }
    
    @Test
    fun testCalculateSettlementsSimple() {
        val items = listOf(
            BudgetItem(
                id = "item-1",
                budgetId = "budget-1",
                category = BudgetCategory.MEALS,
                name = "Dinner",
                description = "Restaurant",
                estimatedCost = 100.0,
                actualCost = 100.0,
                isPaid = true,
                paidBy = "user-1",
                sharedBy = listOf("user-1", "user-2"),
                notes = "",
                createdAt = "2025-12-25T10:00:00Z",
                updatedAt = "2025-12-25T10:00:00Z"
            )
        )
        
        val settlements = BudgetCalculator.calculateSettlements(items)
        
        // Should have exactly one settlement: user-2 pays user-1 50
        assertEquals(1, settlements.size)
        val (from, to, amount) = settlements[0]
        assertEquals("user-2", from)
        assertEquals("user-1", to)
        assertEquals(50.0, amount, 0.01)
    }
    
    @Test
    fun testValidateBudgetItem() {
        val validItem = createTestItems()[0]
        val errors = BudgetCalculator.validateBudgetItem(validItem)
        assertTrue(errors.isEmpty())
    }
    
    @Test
    fun testValidateBudgetItemBlankName() {
        val invalidItem = createTestItems()[0].copy(name = "  ")
        val errors = BudgetCalculator.validateBudgetItem(invalidItem)
        assertTrue(errors.any { it.contains("name cannot be blank") })
    }
    
    @Test
    fun testValidateBudgetItemNegativeCost() {
        val invalidItem = createTestItems()[0].copy(estimatedCost = -10.0)
        val errors = BudgetCalculator.validateBudgetItem(invalidItem)
        assertTrue(errors.any { it.contains("Estimated cost cannot be negative") })
    }
    
    @Test
    fun testValidateBudgetItemPaidWithoutPaidBy() {
        val invalidItem = createTestItems()[0].copy(isPaid = true, paidBy = null)
        val errors = BudgetCalculator.validateBudgetItem(invalidItem)
        assertTrue(errors.any { it.contains("must have a paidBy") })
    }
    
    @Test
    fun testValidateBudgetItemPaidWithZeroCost() {
        val invalidItem = createTestItems()[0].copy(isPaid = true, actualCost = 0.0)
        val errors = BudgetCalculator.validateBudgetItem(invalidItem)
        assertTrue(errors.any { it.contains("must have a positive actual cost") })
    }
    
    @Test
    fun testValidateBudgetItemEmptySharedBy() {
        val invalidItem = createTestItems()[0].copy(sharedBy = emptyList())
        val errors = BudgetCalculator.validateBudgetItem(invalidItem)
        assertTrue(errors.any { it.contains("must be shared by at least one") })
    }
    
    @Test
    fun testValidateBudget() {
        val budget = createTestBudget()
        val errors = BudgetCalculator.validateBudget(budget)
        assertTrue(errors.isEmpty())
    }
    
    @Test
    fun testValidateBudgetNegativeTotals() {
        val invalidBudget = createTestBudget().copy(totalEstimated = -100.0)
        val errors = BudgetCalculator.validateBudget(invalidBudget)
        assertTrue(errors.any { it.contains("Total estimated cannot be negative") })
    }
    
    @Test
    fun testValidateBudgetNegativeCategoryAmount() {
        val invalidBudget = createTestBudget().copy(transportActual = -50.0)
        val errors = BudgetCalculator.validateBudget(invalidBudget)
        assertTrue(errors.any { it.contains("transport") && it.contains("actual") })
    }
    
    @Test
    fun testIsWithinBudget() {
        val budget = createTestBudget() // actual: 500, estimated: 1000
        assertTrue(BudgetCalculator.isWithinBudget(budget))
    }
    
    @Test
    fun testIsOverBudget() {
        val budget = createTestBudget().copy(totalActual = 1500.0)
        assertFalse(BudgetCalculator.isWithinBudget(budget))
    }
    
    @Test
    fun testCalculateCategoryUsagePercentages() {
        val budget = createTestBudget()
        val percentages = BudgetCalculator.calculateCategoryUsagePercentages(budget)
        
        assertEquals(50.0, percentages[BudgetCategory.TRANSPORT]!!, 0.01) // 200/400
        assertEquals(66.67, percentages[BudgetCategory.ACCOMMODATION]!!, 0.01) // 200/300
        assertEquals(50.0, percentages[BudgetCategory.MEALS]!!, 0.01) // 100/200
        assertEquals(0.0, percentages[BudgetCategory.ACTIVITIES]!!) // 0/100
    }
    
    @Test
    fun testFindOverBudgetCategories() {
        val budget = createTestBudget().copy(
            accommodationActual = 400.0 // Over the 300 estimated
        )
        
        val overBudget = BudgetCalculator.findOverBudgetCategories(budget)
        
        assertTrue(overBudget.contains(BudgetCategory.ACCOMMODATION))
        assertFalse(overBudget.contains(BudgetCategory.TRANSPORT))
    }
    
    @Test
    fun testGenerateBudgetSummary() {
        val budget = createTestBudget()
        val items = createTestItems()
        val summary = BudgetCalculator.generateBudgetSummary(budget, items, 3)
        
        assertTrue(summary.contains("Budget Summary"))
        assertTrue(summary.contains("Total Estimated"))
        assertTrue(summary.contains("Per Person"))
        assertTrue(summary.contains("By Category"))
        assertTrue(summary.contains("Within Budget"))
    }
}
