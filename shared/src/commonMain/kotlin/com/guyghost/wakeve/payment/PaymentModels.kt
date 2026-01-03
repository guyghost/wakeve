package com.guyghost.wakeve.payment

import kotlinx.serialization.Serializable

/**
 * Cagnotte
 */
@Serializable
data class Pot(
    val id: String,
    val eventId: String,
    val organizerId: String,
    val goalAmount: Double,
    val currentAmount: Double,
    val currency: String,
    val title: String,
    val status: PotStatus,
    val paymentProvider: PaymentProviderApi,
    val tricountGroupId: String?,
    val tricountGroupUrl: String?,
    val createdAt: String,
    val closedAt: String?
)

/**
 * Status de cagnotte
 */
enum class PotStatus {
    ACTIVE,
    CLOSED_SUCCESS,
    CLOSED_INCOMPLETE
}

/**
 * Contribution
 */
@Serializable
data class Contribution(
    val id: String,
    val potId: String,
    val participantId: String,
    val amount: Double,
    val description: String,
    val paidBy: String,
    val sharedBy: List<String>,
    val paidAt: String
)

/**
 * Balance de participant
 */
@Serializable
data class ParticipantBalance(
    val contributed: Double = 0.0,
    val owed: Double = 0.0
)

/**
 * Balance de cagnotte
 */
@Serializable
data class PotBalance(
    val potId: String,
    val goalAmount: Double,
    val currentAmount: Double,
    val totalContributed: Double,
    val balance: Double,
    val percentage: Int,
    val contributors: Int
)

/**
 * Lien Tricount
 */
@Serializable
data class TricountLink(
    val potId: String,
    val tricountLink: String,
    val groupName: String,
    val goalAmount: Double,
    val currentAmount: Double,
    val balance: Double
)

/**
 * Données exportées Tricount
 */
@Serializable
data class TricountExport(
    val groupId: String,
    val groupName: String,
    val expenses: List<TricountExpense>,
    val members: List<TricountMember>,
    val balances: Map<String, Double>
)

@Serializable
data class TricountExpense(
    val id: String,
    val amount: Double,
    val category: TricountCategory,
    val date: String,
    val payerId: String,
    val involved: List<String>
)

@Serializable
data class TricountMember(
    val id: String,
    val name: String,
    val balance: Double
)

enum class TricountCategory {
    TRANSPORT,
    ACCOMMODATION,
    FOOD,
    ACTIVITIES,
    EQUIPMENT,
    MISCELLANEOUS
}

/**
 * Statistiques de paiement
 */
@Serializable
data class PaymentStatistics(
    val potId: String,
    val goalAmount: Double,
    val currentAmount: Double,
    val totalContributed: Double,
    val averageContribution: Double,
    val contributorsCount: Int,
    val balances: Map<String, ParticipantBalance>
)

/**
 * Provider de paiement
 */
enum class PaymentProvider {
    TRICOUNT,
    PAYPAL,
    STRIPE,
    REVOLUT,
    KNOT
}