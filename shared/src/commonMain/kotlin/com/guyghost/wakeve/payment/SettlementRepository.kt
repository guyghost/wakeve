package com.guyghost.wakeve.payment

import com.guyghost.wakeve.budget.BudgetRepository
import com.guyghost.wakeve.database.WakeveDb
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import com.guyghost.wakeve.Settlement as SqlSettlement

@Serializable
data class SettlementRecord(
    val settlementId: String,
    val eventId: String,
    val budgetId: String,
    val fromParticipantId: String,
    val toParticipantId: String,
    val amount: Double,
    val status: String,
    val createdAt: String,
    val updatedAt: String
)

class SettlementRepository(private val db: WakeveDb) {
    fun getSettlementsForEvent(eventId: String): List<SettlementRecord> =
        db.settlementQueries.selectByEventId(eventId).executeAsList().map { it.toModel() }

    fun getSettlementsVisibleToParticipant(eventId: String, participantId: String): List<SettlementRecord> =
        getSettlementsForEvent(eventId).filter {
            it.fromParticipantId == participantId || it.toParticipantId == participantId
        }

    fun recalculateAndPersist(
        eventId: String,
        budgetId: String,
        budgetRepository: BudgetRepository
    ): List<SettlementRecord> {
        val now = Clock.System.now().toString()
        val suggestions = budgetRepository.getSettlements(budgetId)

        db.transaction {
            db.settlementQueries.deleteByBudgetId(budgetId)
            suggestions.forEach { (from, to, amount) ->
                db.settlementQueries.insertSettlement(
                    id = generateId(),
                    eventId = eventId,
                    budgetId = budgetId,
                    fromParticipantId = from,
                    toParticipantId = to,
                    amount = amount,
                    status = "PERSISTED",
                    createdAt = now,
                    updatedAt = now
                )
            }
        }

        return getSettlementsForEvent(eventId)
    }

    private fun SqlSettlement.toModel(): SettlementRecord =
        SettlementRecord(
            settlementId = id,
            eventId = eventId,
            budgetId = budgetId,
            fromParticipantId = fromParticipantId,
            toParticipantId = toParticipantId,
            amount = amount,
            status = status,
            createdAt = createdAt,
            updatedAt = updatedAt
        )

    private fun generateId(): String =
        "settlement-${Clock.System.now().toEpochMilliseconds()}-${(0..9999).random()}"
}
