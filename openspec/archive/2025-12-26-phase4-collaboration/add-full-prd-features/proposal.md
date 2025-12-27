# Proposition: Implémenter Toutes les Fonctionnalités du PRD

## Change ID
`add-full-prd-features`

## Affected Specs
- **Spec**: `event-organization` (Existant - Extension)
- **Spec**: `scenario-management` (Nouveau)
- **Spec**: `logistics-planning` (Nouveau)
- **Spec**: `budget-management` (Nouveau)
- **Spec**: `collaboration-system` (Nouveau)

## Related Links
- **PRD**: `/PRD-Application-Planification-Evenements.md`
- **Analysis**: `openspec/changes/add-full-prd-features/analysis.md`
- **Current Status**: Phase 2 Complete (Event Organization + Polling)

## Why

Le PRD complet définit une application de planification d'événements entre amis qui va bien au-delà du simple sondage de dates. Pour que Wakeve soit un outil complet et compétitif, nous devons implémenter:

1. **Shortlist de Scénarios (6.3)** - Comparer plusieurs options de planification avec dates, lieux, durées et budgets
2. **Organisation Logistique Complète (6.5)**:
   - Transport avec lieux de départ et optimisation multi-participants
   - Logement avec répartition des chambres
   - Nourriture avec contraintes alimentaires
   - Équipements & Activités avec checklist collaborative
3. **Système de Budget (6.6)** - Suivi en temps réel par catégorie et par personne
4. **Collaboration Avancée (6.7)** - Commentaires par section et historique

**Problème résolu**: Actuellement, Wakeve ne permet que de choisir une date. Les utilisateurs doivent ensuite utiliser d'autres outils (Google Sheets, WhatsApp, Tricount) pour gérer la logistique et le budget, ce qui fragmente l'expérience.

## What Changes

### 1. Nouveaux Modèles de Données

#### Scénarios
```kotlin
@Serializable
data class Scenario(
    val id: String,
    val eventId: String,
    val name: String,
    val dateOrPeriod: String, // Référence à TimeSlot ou période flexible
    val location: String,
    val duration: Int, // nombre de jours
    val estimatedParticipants: Int,
    val estimatedBudgetPerPerson: Double,
    val description: String,
    val votes: Map<String, ScenarioVote> = emptyMap(),
    val status: ScenarioStatus,
    val createdAt: String,
    val updatedAt: String
)

enum class ScenarioVote { PREFER, NEUTRAL, AGAINST }
enum class ScenarioStatus { PROPOSED, SELECTED, REJECTED }
```

#### Budget
```kotlin
@Serializable
data class Budget(
    val id: String,
    val eventId: String,
    val scenarioId: String?,
    val categories: Map<BudgetCategory, BudgetCategoryDetails>,
    val totalEstimated: Double,
    val totalActual: Double,
    val perPersonEstimated: Double,
    val perPersonActual: Double,
    val currency: String,
    val lastUpdated: String
)

enum class BudgetCategory {
    TRANSPORT, ACCOMMODATION, FOOD, ACTIVITIES, EQUIPMENT, MISCELLANEOUS
}

@Serializable
data class BudgetCategoryDetails(
    val estimated: Double,
    val actual: Double,
    val items: List<BudgetItem>
)

@Serializable
data class BudgetItem(
    val id: String,
    val name: String,
    val estimated: Double,
    val actual: Double?,
    val paidBy: String?,
    val sharedBy: List<String>,
    val date: String?
)
```

#### Logement
```kotlin
@Serializable
data class Accommodation(
    val id: String,
    val eventId: String,
    val scenarioId: String?,
    val type: AccommodationType,
    val name: String,
    val address: String,
    val capacity: Int,
    val roomAssignments: Map<String, String>, // participantId -> roomId
    val costPerPerson: Double,
    val costPerNight: Double,
    val totalNights: Int,
    val amenities: List<String>,
    val bookingStatus: BookingStatus,
    val bookingReference: String?
)

enum class AccommodationType { HOTEL, AIRBNB, CAMPING, SHARED_HOUSE, OTHER }
enum class BookingStatus { NOT_BOOKED, PENDING, CONFIRMED, CANCELLED }
```

#### Repas
```kotlin
@Serializable
data class Meal(
    val id: String,
    val eventId: String,
    val scenarioId: String?,
    val type: MealType,
    val date: String,
    val name: String,
    val assignedTo: List<String>,
    val dietaryRestrictions: Map<String, List<DietaryRestriction>>,
    val estimatedCost: Double,
    val actualCost: Double?,
    val status: MealStatus
)

enum class MealType { BREAKFAST, LUNCH, DINNER, SNACK }
enum class DietaryRestriction { VEGETARIAN, VEGAN, GLUTEN_FREE, LACTOSE_FREE, HALAL, KOSHER, ALLERGIES }
enum class MealStatus { PLANNED, PREPARED, COMPLETED }
```

#### Équipements
```kotlin
@Serializable
data class EquipmentItem(
    val id: String,
    val eventId: String,
    val name: String,
    val category: EquipmentCategory,
    val quantity: Int,
    val assignedTo: String?,
    val status: ItemStatus,
    val sharedCost: Double?,
    val notes: String?
)

enum class EquipmentCategory { CAMPING, SPORTS, COOKING, ELECTRONICS, OTHER }
enum class ItemStatus { NEEDED, ASSIGNED, CONFIRMED, PACKED }
```

#### Activités
```kotlin
@Serializable
data class Activity(
    val id: String,
    val eventId: String,
    val scenarioId: String?,
    val name: String,
    val description: String,
    val date: String?,
    val duration: Int, // minutes
    val location: String?,
    val cost: Double?,
    val maxParticipants: Int?,
    val registeredParticipants: List<String>,
    val organizer: String
)
```

#### Commentaires
```kotlin
@Serializable
data class Comment(
    val id: String,
    val eventId: String,
    val section: CommentSection,
    val sectionItemId: String?,
    val authorId: String,
    val authorName: String,
    val content: String,
    val createdAt: String,
    val updatedAt: String?,
    val parentCommentId: String?
)

enum class CommentSection {
    GENERAL, SCENARIO, TRANSPORT, ACCOMMODATION, FOOD, EQUIPMENT, BUDGET, ACTIVITY
}
```

### 2. Extension EventStatus

```kotlin
enum class EventStatus {
    DRAFT,        // Création initiale
    POLLING,      // Sondage de dates en cours
    COMPARING,    // 🆕 Comparaison de scénarios
    CONFIRMED,    // Date confirmée
    ORGANIZING,   // 🆕 Planification logistique en cours
    FINALIZED     // 🆕 Tout est confirmé, prêt pour l'événement
}
```

### 3. Nouvelles Tables SQLDelight

- `Scenario.sq` - Stockage des scénarios
- `ScenarioVote.sq` - Votes sur les scénarios
- `Budget.sq` - Budgets par événement/scénario
- `BudgetItem.sq` - Items de budget individuels
- `Accommodation.sq` - Logements
- `RoomAssignment.sq` - Répartition des chambres
- `Meal.sq` - Repas planifiés
- `DietaryRestrictionMapping.sq` - Contraintes alimentaires par participant
- `EquipmentItem.sq` - Checklist d'équipements
- `Activity.sq` - Activités planifiées
- `ActivityParticipant.sq` - Inscriptions aux activités
- `Comment.sq` - Commentaires

### 4. Nouvelle Logique Métier

#### ScenarioLogic.kt
```kotlin
object ScenarioLogic {
    fun calculateScenarioScore(scenario: Scenario): ScenarioScore
    fun rankScenarios(scenarios: List<Scenario>): List<Pair<Scenario, ScenarioScore>>
    fun getBestScenario(scenarios: List<Scenario>): Scenario?
}

data class ScenarioScore(
    val voteScore: Int, // PREFER=2, NEUTRAL=0, AGAINST=-2
    val budgetScore: Double, // Normalised 0-1
    val totalScore: Double
)
```

#### BudgetCalculator.kt
```kotlin
object BudgetCalculator {
    fun calculateTotalBudget(items: List<BudgetItem>): Double
    fun calculatePerPersonBudget(total: Double, participants: Int): Double
    fun calculateCategoryTotals(items: List<BudgetItem>, category: BudgetCategory): BudgetCategoryDetails
    fun updateBudgetFromLogistics(
        transport: List<TransportBooking>,
        accommodation: List<Accommodation>,
        meals: List<Meal>,
        equipment: List<EquipmentItem>,
        activities: List<Activity>
    ): Budget
}
```

#### AccommodationService.kt
```kotlin
class AccommodationService {
    fun assignRooms(accommodation: Accommodation, participants: List<String>): Map<String, String>
    fun calculateCostPerPerson(accommodation: Accommodation, nights: Int): Double
    fun validateCapacity(accommodation: Accommodation, participants: List<String>): Boolean
}
```

#### MealPlanner.kt
```kotlin
class MealPlanner {
    fun planMeals(eventDuration: Int, participants: List<String>): List<Meal>
    fun assignMealResponsibilities(meals: List<Meal>, participants: List<String>): List<Meal>
    fun validateDietaryRestrictions(meal: Meal, restrictions: Map<String, List<DietaryRestriction>>): Boolean
}
```

#### EquipmentManager.kt
```kotlin
class EquipmentManager {
    fun createChecklist(eventType: String, participants: Int): List<EquipmentItem>
    fun assignEquipment(items: List<EquipmentItem>, participants: List<String>): List<EquipmentItem>
    fun trackEquipmentStatus(items: List<EquipmentItem>): Map<ItemStatus, Int>
}
```

### 5. Nouveaux Repositories

- `ScenarioRepository` - CRUD + Voting
- `BudgetRepository` - CRUD + Calculs
- `AccommodationRepository` - CRUD + Répartition
- `MealRepository` - CRUD + Planification
- `EquipmentRepository` - CRUD + Suivi
- `ActivityRepository` - CRUD + Inscriptions
- `CommentRepository` - CRUD + Threads

### 6. Nouveaux Écrans UI

#### Android (Jetpack Compose)
1. `ScenarioListScreen` - Liste des scénarios avec comparaison visuelle
2. `ScenarioDetailScreen` - Détails d'un scénario + vote
3. `ScenarioComparisonScreen` - Vue côte-à-côte de plusieurs scénarios
4. `TransportPlanningScreen` - Saisie et visualisation des transports
5. `AccommodationScreen` - Gestion du logement et répartition
6. `MealPlanningScreen` - Planification des repas
7. `EquipmentChecklistScreen` - Checklist collaborative
8. `ActivityPlanningScreen` - Planification et inscription aux activités
9. `BudgetOverviewScreen` - Vue d'ensemble avec graphiques
10. `BudgetDetailScreen` - Détail par catégorie
11. `CommentsScreen` - Fil de commentaires par section

#### iOS (SwiftUI)
- Équivalents de tous les écrans Android

### 7. Nouveaux Endpoints API REST

```
# Scénarios
POST   /api/events/{id}/scenarios
GET    /api/events/{id}/scenarios
GET    /api/events/{id}/scenarios/{scenarioId}
PUT    /api/events/{id}/scenarios/{scenarioId}
DELETE /api/events/{id}/scenarios/{scenarioId}
POST   /api/events/{id}/scenarios/{scenarioId}/votes

# Budget
GET    /api/events/{id}/budget
PUT    /api/events/{id}/budget
POST   /api/events/{id}/budget/items
PUT    /api/events/{id}/budget/items/{itemId}
DELETE /api/events/{id}/budget/items/{itemId}

# Logistique
POST   /api/events/{id}/accommodation
GET    /api/events/{id}/accommodation
PUT    /api/events/{id}/accommodation/{accommodationId}
POST   /api/events/{id}/meals
GET    /api/events/{id}/meals
POST   /api/events/{id}/equipment
GET    /api/events/{id}/equipment
PUT    /api/events/{id}/equipment/{itemId}
POST   /api/events/{id}/activities
GET    /api/events/{id}/activities
POST   /api/events/{id}/activities/{activityId}/register

# Commentaires
POST   /api/events/{id}/comments
GET    /api/events/{id}/comments
GET    /api/events/{id}/comments?section={section}&itemId={itemId}
```

### 8. Tests

Nouveaux tests à implémenter:
- `ScenarioLogicTest` (≥8 tests)
- `BudgetCalculatorTest` (≥10 tests)
- `AccommodationServiceTest` (≥6 tests)
- `MealPlannerTest` (≥5 tests)
- `EquipmentManagerTest` (≥5 tests)
- `ScenarioRepositoryTest` (≥8 tests)
- `BudgetRepositoryTest` (≥8 tests)
- `CommentRepositoryTest` (≥8 tests)
- Tests d'intégration (≥15 tests)

**Total**: ≥73 nouveaux tests

## Impact

### Affected Components
- ✅ **shared/models/** - 8 nouveaux fichiers de modèles
- ✅ **shared/sqldelight/** - 12 nouvelles tables
- ✅ **shared/repositories/** - 7 nouveaux repositories
- ✅ **shared/services/** - 4 nouveaux services
- ✅ **composeApp/** - 11 nouveaux écrans Android
- ✅ **iosApp/** - 11 nouveaux écrans iOS
- ✅ **server/routes/** - 25 nouveaux endpoints
- ✅ **tests/** - 73 nouveaux tests

### Database Migration
- Migration de `EventStatus` enum (ajout de 3 valeurs)
- Création de 12 nouvelles tables
- Ajout d'index pour performance

### API Breaking Changes
- ✅ Aucun - Endpoints existants restent inchangés
- ✅ Ajout uniquement de nouveaux endpoints

### Backward Compatibility
- ✅ Tous les événements existants restent compatibles
- ✅ Les nouveaux statuts sont optionnels (workflow progressif)
- ✅ Les nouvelles fonctionnalités ne sont pas obligatoires

## Prioritization

### Phase 1 - Scénarios & Nouveaux Statuts (Sprint 1-2)
**Objectif**: Permettre la comparaison de scénarios

- Modèles: `Scenario`, `ScenarioVote`
- DB: `Scenario.sq`, `ScenarioVote.sq`
- Logique: `ScenarioLogic`
- Repository: `ScenarioRepository`
- UI: ScenarioListScreen, ScenarioDetailScreen, ScenarioComparisonScreen
- API: Endpoints scénarios
- Tests: ScenarioLogicTest, ScenarioRepositoryTest
- EventStatus: Ajout de COMPARING

**Critère de succès**: Utilisateur peut créer 3 scénarios, les comparer, voter et en sélectionner un

### Phase 2 - Budget (Sprint 3-4)
**Objectif**: Suivi budgétaire en temps réel

- Modèles: `Budget`, `BudgetItem`, `BudgetCategory`
- DB: `Budget.sq`, `BudgetItem.sq`
- Logique: `BudgetCalculator`
- Repository: `BudgetRepository`
- UI: BudgetOverviewScreen, BudgetDetailScreen
- API: Endpoints budget
- Tests: BudgetCalculatorTest, BudgetRepositoryTest

**Critère de succès**: Budget se met à jour automatiquement quand on ajoute transport/logement/repas

### Phase 3 - Logistique (Sprint 5-7)
**Objectif**: Planification complète de l'événement

#### Sprint 5 - Logement
- Modèles: `Accommodation`, `RoomAssignment`
- Service: `AccommodationService`
- UI: AccommodationScreen
- Tests: AccommodationServiceTest

#### Sprint 6 - Transport & Repas
- Amélioration: TransportPlanningScreen
- Modèles: `Meal`, `DietaryRestriction`
- Service: `MealPlanner`
- UI: MealPlanningScreen
- Tests: MealPlannerTest

#### Sprint 7 - Équipements & Activités
- Modèles: `EquipmentItem`, `Activity`
- Services: `EquipmentManager`
- UI: EquipmentChecklistScreen, ActivityPlanningScreen
- Tests: EquipmentManagerTest
- EventStatus: Ajout de ORGANIZING, FINALIZED

**Critère de succès**: Organisateur peut planifier transport, logement, repas, équipement et activités

### Phase 4 - Collaboration (Sprint 8)
**Objectif**: Communication in-app

- Modèles: `Comment`, `CommentSection`
- Repository: `CommentRepository`
- UI: CommentsScreen (intégré dans chaque section)
- API: Endpoints commentaires
- Tests: CommentRepositoryTest

**Critère de succès**: Participants peuvent commenter chaque section et recevoir des notifications

## Risks & Mitigations

### Risque 1: Complexité UX
**Impact**: Utilisateurs submergés par trop d'options
**Mitigation**:
- Workflow progressif (étape par étape)
- Sections optionnelles (on peut skip Équipement par exemple)
- Onboarding clair avec tooltips

### Risque 2: Performance DB
**Impact**: Requêtes lentes avec beaucoup de données
**Mitigation**:
- Index sur toutes les clés étrangères
- Pagination pour les listes longues
- Cache local SQLDelight
- Lazy loading des commentaires

### Risque 3: Synchronisation complexe
**Impact**: Conflits de données en temps réel
**Mitigation**:
- Utiliser le système de sync existant (Phase 3)
- Versioning optimiste sur toutes les entités
- CRDT pour édition collaborative (Phase 4)

### Risque 4: Effort de développement
**Impact**: 8 semaines de développement
**Mitigation**:
- Implémentation par phases indépendantes
- Réutilisation des composants UI existants
- Tests automatisés dès le début

## Success Metrics

### Adoption
- ✅ 80% des utilisateurs explorent au moins 1 scénario
- ✅ 60% des utilisateurs utilisent le budget
- ✅ 50% des utilisateurs planifient la logistique

### Engagement
- ✅ 3+ scénarios créés par événement en moyenne
- ✅ 5+ commentaires par événement
- ✅ Temps de planification réduit de 50%

### Technique
- ✅ 100% des tests passent
- ✅ Temps de réponse API < 200ms
- ✅ Support offline complet

## Next Steps

1. ✅ **Approbation de la proposition** par l'équipe
2. ✅ **Créer les spécifications détaillées** pour chaque fonctionnalité
3. ✅ **Setup des branches de développement** par phase
4. ✅ **Kickoff Phase 1** - Scénarios & Nouveaux Statuts

---

**Proposition créée**: 25 décembre 2025  
**Auteur**: Équipe Wakeve  
**Status**: En attente d'approbation  
**Effort estimé**: 8 semaines (4 phases de 2 sprints)
