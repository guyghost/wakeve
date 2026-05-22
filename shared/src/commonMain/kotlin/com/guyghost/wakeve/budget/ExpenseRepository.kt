package com.guyghost.wakeve.budget

import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.BudgetCategory
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.guyghost.wakeve.Expense as SqlExpense

@Serializable
data class ExpenseRecord(
    val id: String,
    val eventId: String,
    val budgetId: String,
    val amount: Double,
    val category: BudgetCategory,
    val payerId: String,
    val splitParticipantIds: List<String>,
    val receiptMetadata: Map<String, String>,
    val syncState: String,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class ParticipantExpenseBalance(
    val participantId: String,
    val totalOwed: Double,
    val totalPaid: Double,
    val balance: Double
)

class ExpenseRepository(private val db: WakeveDb) {
    private val json = Json { ignoreUnknownKeys = true }

    fun createExpense(
        eventId: String,
        amount: Double,
        category: BudgetCategory,
        payerId: String,
        splitParticipantIds: List<String>,
        receiptMetadata: Map<String, String> = emptyMap(),
        syncState: String = "PENDING"
    ): ExpenseRecord {
        require(amount > 0.0) { "Expense amount must be positive" }
        require(splitParticipantIds.isNotEmpty()) { "Expense must have at least one split participant" }

        val budget = db.budgetQueries.selectByEventId(eventId).executeAsOneOrNull()
            ?: throw IllegalArgumentException("Budget not found for event: $eventId")
        val now = now()
        val expense = ExpenseRecord(
            id = generateId("expense"),
            eventId = eventId,
            budgetId = budget.id,
            amount = amount,
            category = category,
            payerId = payerId,
            splitParticipantIds = splitParticipantIds.distinct(),
            receiptMetadata = receiptMetadata,
            syncState = syncState,
            createdAt = now,
            updatedAt = now
        )

        db.transaction {
            db.expenseQueries.insertExpense(
                id = expense.id,
                eventId = expense.eventId,
                budgetId = expense.budgetId,
                amount = expense.amount,
                category = expense.category.name,
                payerId = expense.payerId,
                splitParticipantIds = expense.splitParticipantIds.joinToString(","),
                receiptMetadata = encodeMetadata(expense.receiptMetadata),
                syncState = expense.syncState,
                createdAt = expense.createdAt,
                updatedAt = expense.updatedAt
            )
            if (expense.syncState == "PENDING") {
                db.syncMetadataQueries.insertSyncMetadataWithPayload(
                    id = generateId("sync"),
                    entityType = "expense",
                    entityId = expense.id,
                    operation = "CREATE",
                    payload = json.encodeToString(expense),
                    timestamp = now,
                    retryState = "READY",
                    retryCount = 0L,
                    synced = 0L
                )
            }
        }

        return expense
    }

    fun getExpensesForEvent(eventId: String): List<ExpenseRecord> =
        db.expenseQueries.selectByEventId(eventId).executeAsList().map { it.toModel() }

    fun getExpensesForBudget(budgetId: String): List<ExpenseRecord> =
        db.expenseQueries.selectByBudgetId(budgetId).executeAsList().map { it.toModel() }

    fun getBalancesForEvent(eventId: String): List<ParticipantExpenseBalance> {
        val expenses = getExpensesForEvent(eventId)
        val owed = mutableMapOf<String, Double>()
        val paid = mutableMapOf<String, Double>()

        expenses.forEach { expense ->
            paid[expense.payerId] = (paid[expense.payerId] ?: 0.0) + expense.amount
            val share = expense.amount / expense.splitParticipantIds.size
            expense.splitParticipantIds.forEach { participantId ->
                owed[participantId] = (owed[participantId] ?: 0.0) + share
            }
        }

        return (owed.keys + paid.keys).map { participantId ->
            val totalOwed = owed[participantId] ?: 0.0
            val totalPaid = paid[participantId] ?: 0.0
            ParticipantExpenseBalance(
                participantId = participantId,
                totalOwed = totalOwed,
                totalPaid = totalPaid,
                balance = totalOwed - totalPaid
            )
        }
    }

    private fun SqlExpense.toModel(): ExpenseRecord =
        ExpenseRecord(
            id = id,
            eventId = eventId,
            budgetId = budgetId,
            amount = amount,
            category = BudgetCategory.valueOf(category),
            payerId = payerId,
            splitParticipantIds = if (splitParticipantIds.isBlank()) emptyList() else splitParticipantIds.split(","),
            receiptMetadata = decodeMetadata(receiptMetadata),
            syncState = syncState,
            createdAt = createdAt,
            updatedAt = updatedAt
        )

    private fun encodeMetadata(metadata: Map<String, String>): String =
        json.encodeToString(MapSerializer(String.serializer(), String.serializer()), metadata)

    private fun decodeMetadata(raw: String): Map<String, String> =
        if (raw.isBlank()) {
            emptyMap()
        } else {
            json.decodeFromString(MapSerializer(String.serializer(), String.serializer()), raw)
        }

    private fun generateId(prefix: String): String =
        "$prefix-${Clock.System.now().toEpochMilliseconds()}-${(0..9999).random()}"

    private fun now(): String = Clock.System.now().toString()
}
