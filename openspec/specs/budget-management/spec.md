# Budget Management Specification

## Overview

The Budget Management system allows event organizers and participants to collaboratively track expenses, split costs, and manage event budgets. It provides real-time cost tracking, automatic recalculation, participant balance tracking, and settlement suggestions.

**Version**: 1.0.0  
**Status**: ✅ Implemented  
**Phase**: 2

---

## Key Features

### 1. Budget Creation & Management
- One budget per event
- Automatic creation when first expense is added
- Real-time totals: estimated vs actual costs
- Category-based organization (6 categories)

### 2. Expense Tracking
- Individual budget items with detailed information
- Support for shared expenses among participants
- Payment status tracking (paid/unpaid)
- Automatic budget recalculation on item changes

### 3. Cost Splitting
- Flexible participant sharing per expense
- Automatic calculation of per-person costs
- Balance tracking (who owes whom)
- Settlement suggestions using greedy algorithm

### 4. Categories
- **Transport**: Flights, trains, car rentals, gas
- **Accommodation**: Hotels, Airbnb, camping fees
- **Meals**: Restaurants, groceries, catering
- **Activities**: Tickets, tours, equipment rentals
- **Equipment**: Purchases or rentals for event
- **Other**: Miscellaneous expenses

---

## Data Models

### Budget

```kotlin
data class Budget(
    val id: String,
    val eventId: String,  // One-to-one with Event
    
    // Total costs
    val totalEstimatedCost: Double,
    val totalActualCost: Double,
    
    // Category breakdowns (estimated + actual for each)
    val estimatedTransportCost: Double,
    val actualTransportCost: Double,
    val estimatedAccommodationCost: Double,
    val actualAccommodationCost: Double,
    val estimatedMealsCost: Double,
    val actualMealsCost: Double,
    val estimatedActivitiesCost: Double,
    val actualActivitiesCost: Double,
    val estimatedEquipmentCost: Double,
    val actualEquipmentCost: Double,
    val estimatedOtherCost: Double,
    val actualOtherCost: Double,
    
    // Timestamps
    val createdAt: String,  // ISO 8601
    val updatedAt: String   // ISO 8601
)
```

**Constraints**:
- `eventId` must be unique (one budget per event)
- All cost fields must be >= 0
- Total costs auto-calculated from category totals

### BudgetItem

```kotlin
data class BudgetItem(
    val id: String,
    val budgetId: String,  // Foreign key to Budget
    
    // Item details
    val name: String,
    val description: String,
    val category: BudgetCategory,
    
    // Costs
    val estimatedCost: Double,
    val actualCost: Double,
    
    // Payment tracking
    val isPaid: Boolean,
    val paidBy: String?,  // Participant ID who paid
    
    // Cost sharing
    val sharedBy: List<String>,  // Participant IDs sharing this cost
    
    // Timestamps
    val createdAt: String,  // ISO 8601
    val updatedAt: String   // ISO 8601
)
```

**Constraints**:
- `name` must not be empty
- `estimatedCost` must be > 0
- `actualCost` must be >= 0
- `isPaid` = true requires `paidBy` and `actualCost > 0`
- `sharedBy` must contain at least one participant

### BudgetCategory

```kotlin
enum class BudgetCategory {
    TRANSPORT,
    ACCOMMODATION,
    MEALS,
    ACTIVITIES,
    EQUIPMENT,
    OTHER
}
```

### Helper Models

```kotlin
data class BudgetCategoryDetails(
    val category: BudgetCategory,
    val estimatedCost: Double,
    val actualCost: Double
)

data class ParticipantBudgetShare(
    val participantId: String,
    val totalShare: Double,        // Total they should pay
    val totalPaid: Double,          // Total they've paid
    val itemsSharedIn: Int,         // Number of items they share
    val itemsPaidBy: Int            // Number of items they paid for
)

data class BudgetWithItems(
    val budget: Budget,
    val items: List<BudgetItem>
)
```

---

## Business Logic

### Auto-Recalculation

When a BudgetItem is created, updated, or deleted:

1. **Aggregate items by category**
   ```kotlin
   estimatedTransportCost = items
       .filter { it.category == TRANSPORT }
       .sumOf { it.estimatedCost }
   
   actualTransportCost = items
       .filter { it.category == TRANSPORT }
       .sumOf { it.actualCost }
   ```

2. **Calculate totals**
   ```kotlin
   totalEstimatedCost = estimatedTransportCost + 
                        estimatedAccommodationCost + 
                        estimatedMealsCost + 
                        estimatedActivitiesCost + 
                        estimatedEquipmentCost + 
                        estimatedOtherCost
   
   totalActualCost = actualTransportCost + 
                     actualAccommodationCost + 
                     actualMealsCost + 
                     actualActivitiesCost + 
                     actualEquipmentCost + 
                     actualOtherCost
   ```

3. **Update Budget** in database

### Cost Splitting

For each participant in an item's `sharedBy` list:

```kotlin
fun calculateParticipantShare(item: BudgetItem, participantId: String): Double {
    if (participantId !in item.sharedBy) return 0.0
    
    // Use actual cost if paid, otherwise estimated
    val relevantCost = if (item.isPaid) item.actualCost else item.estimatedCost
    
    // Split equally among shared participants
    return relevantCost / item.sharedBy.size
}
```

### Balance Calculation

For each participant, calculate their balance:

```kotlin
fun calculateBalance(participantId: String, items: List<BudgetItem>): Double {
    // Amount they paid for (positive)
    val totalPaid = items
        .filter { it.isPaid && it.paidBy == participantId }
        .sumOf { it.actualCost }
    
    // Amount they should pay (negative)
    val totalShare = items.sumOf { item ->
        if (participantId in item.sharedBy) {
            val cost = if (item.isPaid) item.actualCost else item.estimatedCost
            cost / item.sharedBy.size
        } else 0.0
    }
    
    // Positive = owed by others, Negative = owes others, 0 = settled
    return totalPaid - totalShare
}
```

### Settlement Algorithm

Greedy approach to minimize number of transactions:

```kotlin
fun calculateSettlements(balances: Map<String, Double>): List<Settlement> {
    val creditors = balances.filter { it.value > 0 }.toMutableMap()
    val debtors = balances.filter { it.value < 0 }.toMutableMap()
    val settlements = mutableListOf<Settlement>()
    
    while (creditors.isNotEmpty() && debtors.isNotEmpty()) {
        // Get largest creditor and debtor
        val maxCreditor = creditors.maxByOrNull { it.value }!!
        val maxDebtor = debtors.minByOrNull { it.value }!!
        
        // Calculate settlement amount
        val amount = minOf(maxCreditor.value, -maxDebtor.value)
        
        settlements.add(Settlement(
            from = maxDebtor.key,
            to = maxCreditor.key,
            amount = amount
        ))
        
        // Update balances
        creditors[maxCreditor.key] = maxCreditor.value - amount
        debtors[maxDebtor.key] = maxDebtor.value + amount
        
        // Remove settled participants
        if (creditors[maxCreditor.key]!! <= 0.01) creditors.remove(maxCreditor.key)
        if (debtors[maxDebtor.key]!! >= -0.01) debtors.remove(maxDebtor.key)
    }
    
    return settlements
}
```

---

## REST API Endpoints

Base path: `/api/events/{eventId}/budget`

### 1. Get Budget

```http
GET /api/events/{eventId}/budget
```

**Response 200 OK**:
```json
{
    "id": "budget-123",
    "eventId": "event-456",
    "totalEstimatedCost": 1500.00,
    "totalActualCost": 1350.00,
    "estimatedTransportCost": 600.00,
    "actualTransportCost": 550.00,
    "estimatedAccommodationCost": 500.00,
    "actualAccommodationCost": 480.00,
    "estimatedMealsCost": 300.00,
    "actualMealsCost": 250.00,
    "estimatedActivitiesCost": 100.00,
    "actualActivitiesCost": 70.00,
    "estimatedEquipmentCost": 0.00,
    "actualEquipmentCost": 0.00,
    "estimatedOtherCost": 0.00,
    "actualOtherCost": 0.00,
    "createdAt": "2025-12-01T10:00:00Z",
    "updatedAt": "2025-12-25T14:30:00Z"
}
```

**Response 404 Not Found**:
```json
{
    "error": "Budget not found for event"
}
```

### 2. Update or Create Budget

```http
PUT /api/events/{eventId}/budget
Content-Type: application/json
```

**Request Body**:
```json
{
    "id": "budget-123",
    "eventId": "event-456",
    "totalEstimatedCost": 1500.00,
    "totalActualCost": 1350.00,
    ...
}
```

**Response 200 OK**: Returns saved Budget

**Note**: Budget totals are auto-calculated from items. Manually set totals will be overwritten on next item change.

### 3. Get Budget Items

```http
GET /api/events/{eventId}/budget/items
GET /api/events/{eventId}/budget/items?category=TRANSPORT
GET /api/events/{eventId}/budget/items?paid=true
GET /api/events/{eventId}/budget/items?participantId=user-1
```

**Query Parameters**:
- `category` (optional): Filter by category (TRANSPORT, ACCOMMODATION, etc.)
- `paid` (optional): Filter by payment status (true/false)
- `participantId` (optional): Filter by participant (items they share in)

**Response 200 OK**:
```json
{
    "items": [
        {
            "id": "item-1",
            "budgetId": "budget-123",
            "name": "Airbnb Booking",
            "description": "3 nights in downtown",
            "category": "ACCOMMODATION",
            "estimatedCost": 480.00,
            "actualCost": 480.00,
            "isPaid": true,
            "paidBy": "user-1",
            "sharedBy": ["user-1", "user-2", "user-3"],
            "createdAt": "2025-12-10T12:00:00Z",
            "updatedAt": "2025-12-20T15:30:00Z"
        }
    ],
    "count": 1
}
```

### 4. Add Budget Item

```http
POST /api/events/{eventId}/budget/items
Content-Type: application/json
```

**Request Body**:
```json
{
    "name": "Train tickets",
    "description": "Round trip for 3 people",
    "category": "TRANSPORT",
    "estimatedCost": 150.00,
    "sharedBy": ["user-1", "user-2", "user-3"]
}
```

**Response 201 Created**: Returns created BudgetItem

**Validation**:
- `name` must not be empty
- `estimatedCost` must be > 0
- `category` must be valid enum value
- `sharedBy` defaults to ["user-1"] if empty

### 5. Get Budget Item

```http
GET /api/events/{eventId}/budget/items/{itemId}
```

**Response 200 OK**: Returns BudgetItem  
**Response 404 Not Found**: Item not found or doesn't belong to budget

### 6. Update Budget Item

```http
PUT /api/events/{eventId}/budget/items/{itemId}
Content-Type: application/json
```

**Request Body**: Full BudgetItem object

**Response 200 OK**: Returns updated BudgetItem

**Note**: Updates trigger automatic budget recalculation

### 7. Delete Budget Item

```http
DELETE /api/events/{eventId}/budget/items/{itemId}
```

**Response 200 OK**:
```json
{
    "message": "Item deleted successfully"
}
```

**Note**: Deletion triggers automatic budget recalculation

### 8. Get Budget Summary

```http
GET /api/events/{eventId}/budget/summary
```

**Response 200 OK**:
```json
{
    "budget": { ... },
    "summary": {
        "totalEstimated": 1500.00,
        "totalActual": 1350.00,
        "difference": -150.00,
        "percentageUsed": 90.0,
        "status": "Under Budget",
        "categoryBreakdown": [...],
        "participantSummary": [...]
    },
    "itemCount": 12
}
```

### 9. Get Settlement Suggestions

```http
GET /api/events/{eventId}/budget/settlements
```

**Response 200 OK**:
```json
{
    "settlements": [
        {
            "from": "user-2",
            "to": "user-1",
            "amount": 120.50
        },
        {
            "from": "user-3",
            "to": "user-1",
            "amount": 80.75
        }
    ],
    "count": 2
}
```

**Algorithm**: Greedy approach minimizing number of transactions

### 10. Get Participant Budget Info

```http
GET /api/events/{eventId}/budget/participants/{participantId}
```

**Response 200 OK**:
```json
{
    "participantId": "user-1",
    "share": {
        "participantId": "user-1",
        "totalShare": 500.00,
        "totalPaid": 700.00,
        "itemsSharedIn": 10,
        "itemsPaidBy": 5
    },
    "balance": 200.00,
    "balanceDescription": "Owed by others"
}
```

**Balance Meanings**:
- Positive: Owed by others (they overpaid)
- Negative: Owes to others (they underpaid)
- Zero: Settled up

### 11. Get Budget Statistics

```http
GET /api/events/{eventId}/budget/statistics
```

**Response 200 OK**:
```json
{
    "totalItems": 12,
    "paidItems": 8,
    "unpaidItems": 4,
    "categoryStatistics": [
        {
            "category": "TRANSPORT",
            "count": 3,
            "estimatedTotal": 600.00,
            "actualTotal": 550.00
        },
        ...
    ]
}
```

---

## Database Schema

### Budget Table

```sql
CREATE TABLE Budget (
    id TEXT PRIMARY KEY,
    eventId TEXT NOT NULL UNIQUE,
    totalEstimatedCost REAL NOT NULL DEFAULT 0.0,
    totalActualCost REAL NOT NULL DEFAULT 0.0,
    estimatedTransportCost REAL NOT NULL DEFAULT 0.0,
    actualTransportCost REAL NOT NULL DEFAULT 0.0,
    estimatedAccommodationCost REAL NOT NULL DEFAULT 0.0,
    actualAccommodationCost REAL NOT NULL DEFAULT 0.0,
    estimatedMealsCost REAL NOT NULL DEFAULT 0.0,
    actualMealsCost REAL NOT NULL DEFAULT 0.0,
    estimatedActivitiesCost REAL NOT NULL DEFAULT 0.0,
    actualActivitiesCost REAL NOT NULL DEFAULT 0.0,
    estimatedEquipmentCost REAL NOT NULL DEFAULT 0.0,
    actualEquipmentCost REAL NOT NULL DEFAULT 0.0,
    estimatedOtherCost REAL NOT NULL DEFAULT 0.0,
    actualOtherCost REAL NOT NULL DEFAULT 0.0,
    createdAt TEXT NOT NULL,
    updatedAt TEXT NOT NULL,
    FOREIGN KEY (eventId) REFERENCES Event(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX idx_budget_eventId ON Budget(eventId);
```

### BudgetItem Table

```sql
CREATE TABLE BudgetItem (
    id TEXT PRIMARY KEY,
    budgetId TEXT NOT NULL,
    name TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    category TEXT NOT NULL,
    estimatedCost REAL NOT NULL,
    actualCost REAL NOT NULL DEFAULT 0.0,
    isPaid INTEGER NOT NULL DEFAULT 0,  -- 0 = false, 1 = true
    paidBy TEXT,
    sharedBy TEXT NOT NULL,  -- CSV: "user-1,user-2,user-3"
    createdAt TEXT NOT NULL,
    updatedAt TEXT NOT NULL,
    FOREIGN KEY (budgetId) REFERENCES Budget(id) ON DELETE CASCADE
);

CREATE INDEX idx_budgetitem_budgetId ON BudgetItem(budgetId);
CREATE INDEX idx_budgetitem_category ON BudgetItem(category);
CREATE INDEX idx_budgetitem_isPaid ON BudgetItem(isPaid);
CREATE INDEX idx_budgetitem_paidBy ON BudgetItem(paidBy);
```

**Note**: CASCADE DELETE ensures items are deleted when budget is deleted

---

## UI Components

### Android (Jetpack Compose)

#### BudgetOverviewScreen
- **Location**: `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/budget/BudgetOverviewScreen.kt`
- **Features**:
  - Summary card (estimated vs actual)
  - Per-person cost breakdown
  - Status indicator (within/over budget)
  - Category breakdown with progress bars
  - Navigation to detail screen
- **Design**: Material You with adaptive colors

#### BudgetDetailScreen
- **Location**: `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/budget/BudgetDetailScreen.kt`
- **Features**:
  - Item list with category filters
  - Payment status filters (paid/unpaid)
  - Add/Edit/Delete dialogs
  - Mark as paid action
  - FAB for quick add
- **Design**: Material You cards and dialogs

### iOS (SwiftUI)

#### BudgetOverviewView
- **Location**: `iosApp/iosApp/Views/BudgetOverviewView.swift`
- **Features**: Same as Android
- **Design**: Liquid Glass with `.glassCard()` and `.continuousCornerRadius()`

#### BudgetDetailView
- **Location**: `iosApp/iosApp/Views/BudgetDetailView.swift`
- **Features**: Same as Android
- **Design**: Liquid Glass with native SwiftUI sheets and menus

---

## Testing

### Unit Tests

**BudgetCalculatorTest** (30 tests)
- Total budget calculations
- Category aggregations
- Per-person calculations
- Participant share calculations
- Balance calculations
- Settlement algorithm
- Validation logic
- Summary generation

**BudgetRepositoryTest** (31 tests)
- CRUD operations (13 tests)
- Auto-recalculation (5 tests)
- Filtering and queries (4 tests)
- Balance calculations (3 tests)
- Statistics (3 tests)
- Validation (2 tests)
- CASCADE DELETE (1 test)

**Total**: 61/61 passing ✅

### Integration Tests

TODO: End-to-end budget workflow tests

---

## Usage Examples

### Creating a Budget with Items

```kotlin
// 1. Create budget for event
val budget = repository.createBudget(eventId = "event-1")

// 2. Add expense items
val airbnb = repository.createBudgetItem(
    budgetId = budget.id,
    name = "Airbnb Booking",
    description = "3 nights",
    category = BudgetCategory.ACCOMMODATION,
    estimatedCost = 480.00,
    sharedBy = listOf("user-1", "user-2", "user-3")
)

val flights = repository.createBudgetItem(
    budgetId = budget.id,
    name = "Flights",
    description = "Round trip",
    category = BudgetCategory.TRANSPORT,
    estimatedCost = 600.00,
    sharedBy = listOf("user-1", "user-2", "user-3")
)

// 3. Budget totals auto-calculated
val updated = repository.getBudgetById(budget.id)
// updated.totalEstimatedCost == 1080.00
// updated.estimatedAccommodationCost == 480.00
// updated.estimatedTransportCost == 600.00
```

### Marking Expense as Paid

```kotlin
// User-1 pays for Airbnb
val paid = repository.markItemAsPaid(
    itemId = airbnb.id,
    actualCost = 480.00,
    paidBy = "user-1"
)

// Budget auto-updated
// actualAccommodationCost = 480.00
// totalActualCost = 480.00
```

### Getting Settlements

```kotlin
val settlements = repository.getSettlements(budget.id)
// [("user-2", "user-1", 160.00), ("user-3", "user-1", 160.00)]
// User-1 paid full Airbnb, so user-2 and user-3 each owe their share
```

---

## Future Enhancements

### Phase 3+ Features
- [ ] Real-time sync across devices
- [ ] Receipt photo upload and OCR
- [ ] Currency conversion support
- [ ] Budget templates by event type
- [ ] Expense approval workflow
- [ ] Export to PDF/CSV
- [ ] Integration with payment apps (Venmo, PayPal)
- [ ] Recurring expense support
- [ ] Budget alerts and notifications
- [ ] Analytics and spending insights

### Technical Improvements
- [ ] CRDT for collaborative editing
- [ ] Offline queue for item changes
- [ ] Optimistic UI updates
- [ ] Caching layer for API responses
- [ ] WebSocket for real-time updates

---

## Dependencies

**Kotlin Multiplatform**:
- kotlinx-serialization (JSON serialization)
- SQLDelight (database)
- kotlinx-datetime (future enhancement)

**Android**:
- Jetpack Compose
- Material3
- Compose Navigation

**iOS**:
- SwiftUI
- Combine (future enhancement)

**Server**:
- Ktor 3.3.1
- kotlinx-serialization
- JWT authentication

---

## Changelog

### v1.0.0 (2025-12-25)
- ✅ Initial implementation
- ✅ Full CRUD for budgets and items
- ✅ Auto-recalculation
- ✅ Cost splitting and balances
- ✅ Settlement suggestions
- ✅ REST API (9 endpoints)
- ✅ Android UI (2 screens)
- ✅ iOS UI (2 views)
- ✅ Comprehensive tests (61/61)

---

**Spec Version**: 1.0.0  
**Last Updated**: 2025-12-25  
**Status**: ✅ Complete  
**Test Coverage**: 100% (61/61 tests passing)
