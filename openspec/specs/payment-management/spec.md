# Payment Management Specification

## Version
**Version**: 1.0.0
**Status**: ✅ Implémenté
**Date de création**: 26 décembre 2025
**Auteur**: Équipe Wakeve

## Overview

Le service de paiement de Wakeve permet de gérer les cagnottes de groupe, de suivre les dépenses partagées, et de s'intégrer avec Tricount pour la répartition équitable des coûts entre participants.

## Domain Model

### Core Concepts

- **Money Pot**: Cagnotte de groupe pour collecter les fonds
- **Expense**: Dépense individuelle à répartir
- **Payment Contribution**: Contribution d'un participant à la cagnotte
- **Tricount Integration**: Synchronisation avec Tricount pour partage des coûts
- **Settlements**: Calculs de qui doit combien à qui

### MoneyPot

\`\`\`kotlin
@Serializable
data class MoneyPot(
    val id: String,
    val eventId: String,
    val organizerId: String,
    val title: String,
    val description: String?,
    val targetAmount: Double,
    val currency: String,
    val currentAmount: Double,
    val createdAt: Instant,
    val expiresAt: Instant?,
    val status: PotStatus,
    val provider: PaymentProvider,
    val providerPotId: String?,
    val providerUrl: String?,
    val isPublic: Boolean,
    val allowAnonymous: Boolean
)
\`\`\`

### PotStatus

\`\`\`kotlin
enum class PotStatus {
    DRAFT,
    ACTIVE,
    PAUSED,
    COMPLETED,
    EXPIRED
}
\`\`\`

### PaymentProvider

\`\`\`kotlin
enum class PaymentProvider {
    MOCK,
    STRIPE,
    PAYPAL,
    TRICOUNT,
    LYDIA,
    LEETCHI,
    SWISH
}
\`\`\`

### Expense

\`\`\`kotlin
@Serializable
data class Expense(
    val id: String,
    val eventId: String,
    val potId: String?,
    val paidBy: String,
    val category: ExpenseCategory,
    val title: String,
    val description: String?,
    val amount: Double,
    val currency: String,
    val date: Instant,
    val splitType: SplitType,
    val splitAmong: List<String>,
    val receipts: List<String>,
    val approvedBy: List<String>,
    val isTricountImported: Boolean,
    val tricountExpenseId: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)
\`\`\`

### SplitType

\`\`\`kotlin
enum class SplitType {
    EQUAL,
    PERCENTAGE,
    AMOUNT,
    WEIGHTED,
    CUSTOM
}
\`\`\`

### Settlement

\`\`\`kotlin
@Serializable
data class Settlement(
    val id: String,
    val eventId: String,
    val fromParticipantId: String,
    val toParticipantId: String,
    val amount: Double,
    val currency: String,
    val status: SettlementStatus,
    val suggestedAt: Instant,
    val confirmedAt: Instant?,
    val completedAt: Instant?
)
\`\`\`

## API Endpoints

\`\`\`
POST   /api/events/{id}/pots
GET    /api/events/{id}/pots
GET    /api/pots/{potId}
POST   /api/pots/{potId}/contribute
GET    /api/pots/{potId}/contributions
POST   /api/pots/{potId}/refund
PUT    /api/pots/{potId}
POST   /api/events/{id}/expenses
GET    /api/events/{id}/expenses
GET    /api/expenses/{expenseId}
PUT    /api/expenses/{expenseId}
DELETE /api/expenses/{expenseId}
GET    /api/events/{id}/settlements
\`\`\`

## Scenarios

### SCENARIO 1: Create Pot
- Create pot with target amount
- Status becomes ACTIVE
- Provider URL generated

### SCENARIO 2: Contribute
- Participant adds contribution
- Pot balance updated
- Transaction ID generated

### SCENARIO 3: Record Expense
- Expense recorded with split type
- Expense saved to database
- Synced to Tricount if enabled

### SCENARIO 4: Calculate Settlements
- System calculates who owes whom
- Settlements generated based on balances
- Status: SUGGESTED

## Database

### Tables
- money_pot
- payment_contribution
- expense
- settlement

## Testing

### Unit Tests
- Test create pot
- Test contribute
- Test record expense
- Test calculate settlements
- Test refund

## Limitations

**Phase 1**:
- Mock providers only
- Simplified split types
- No real payments

**Phase 2** (Future):
- Real provider integration
- Advanced splits
- Receipts upload
- Notifications

---

**Version**: 1.0.0
**Last Updated**: 26 décembre 2025
**Maintainer**: Équipe Wakeve
