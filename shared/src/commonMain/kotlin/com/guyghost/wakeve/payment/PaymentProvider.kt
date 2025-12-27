package com.guyghost.wakeve.payment

import kotlinx.datetime.Clock

/**
 * Provider de paiement (interface)
 */
interface PaymentProviderApi {
    suspend fun createPot(
        title: String,
        goalAmount: Double,
        currency: String
    ): String // Pot ID ou Group ID

    suspend fun addExpense(
        groupId: String,
        amount: Double,
        description: String,
        paidBy: String,
        category: TricountCategory
    ): String // Expense ID

    suspend fun getGroupLink(groupId: String): String

    suspend fun updateGroupDetails(
        groupId: String,
        title: String,
        goalAmount: Double
    ): Boolean

    suspend fun exportGroupData(groupId: String): TricountExport
}

/**
 * Mock Payment Provider pour les tests et développement
 */
class MockPaymentProvider : PaymentProviderApi {
    override suspend fun createPot(
        title: String,
        goalAmount: Double,
        currency: String
    ): String {
        // Génère un ID de groupe mocké
        return "group-${Clock.System.now().toEpochMilliseconds()}"
    }

    override suspend fun addExpense(
        groupId: String,
        amount: Double,
        description: String,
        paidBy: String,
        category: TricountCategory
    ): String {
        return "expense-${Clock.System.now().toEpochMilliseconds()}"
    }

    override suspend fun getGroupLink(groupId: String): String {
        return "https://tricount.com/g/$groupId"
    }

    override suspend fun updateGroupDetails(
        groupId: String,
        title: String,
        goalAmount: Double
    ): Boolean {
        println("Updated Tricount group $groupId: $title - €$goalAmount")
        return true
    }

    override suspend fun exportGroupData(groupId: String): TricountExport {
        return TricountExport(
            groupId = groupId,
            groupName = "Cagnotte Event",
            expenses = emptyList(),
            members = emptyList(),
            balances = emptyMap()
        )
    }
}