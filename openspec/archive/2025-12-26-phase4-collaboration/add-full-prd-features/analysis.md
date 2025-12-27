# Analyse des Fonctionnalités PRD vs État Actuel

## Date: 25 décembre 2025
## Version: 1.0.0

---

## 1. État Actuel de l'Implémentation

### ✅ Fonctionnalités Complètes (Phase 2)

#### 6.1 Création d'un Événement
- **Status**: ✅ Implémenté
- **Modèle**: `Event` avec id, title, description, organizerId, participants, proposedSlots, deadline, status
- **UI**: EventCreationScreen (Android Compose)
- **DB**: Event.sq, Participant.sq
- **Tests**: EventRepositoryTest (10 tests)

#### 6.2 Sondage de Dates
- **Status**: ✅ Implémenté
- **Modèle**: `TimeSlot`, `Vote` enum (YES, MAYBE, NO), `Poll`
- **Logique**: PollLogic avec scoring (YES=2, MAYBE=1, NO=-1)
- **UI**: PollVotingScreen, PollResultsScreen
- **DB**: TimeSlot.sq, Vote.sq
- **Tests**: PollLogicTest (6 tests), DatabaseEventRepositoryTest (13 tests)

#### 6.4 Confirmation de la Date
- **Status**: ✅ Implémenté
- **Modèle**: EventStatus (DRAFT, POLLING, CONFIRMED), finalDate
- **DB**: ConfirmedDate.sq
- **Tests**: Inclus dans EventRepositoryTest

#### Gestion des Participants
- **Status**: ✅ Implémenté
- **UI**: ParticipantManagementScreen
- **DB**: Participant.sq avec role (ORGANIZER, PARTICIPANT)
- **Tests**: OfflineScenarioTest (7 tests)

---

## 2. Gaps Identifiés par Rapport au PRD

### ❌ 6.3 Shortlist de Scénarios
**Status**: Non implémenté

**Requis**:
- Modèle `Scenario` avec:
  - id: String
  - eventId: String
  - dateOrPeriod: String (référence à TimeSlot ou période flexible)
  - location: String (destination)
  - duration: Int (nombre de jours)
  - estimatedParticipants: Int
  - estimatedBudgetPerPerson: Double
  - votes: Map<String, ScenarioVote> (participantId -> vote)
  - status: ScenarioStatus (PROPOSED, SELECTED, REJECTED)
  - createdAt: String
  - updatedAt: String

**Fonctionnalités manquantes**:
- Création/modification/suppression de scénarios
- Système de vote sur scénarios
- Classement automatique par score
- Comparaison visuelle des scénarios

**Impact**:
- Nouveau modèle de données
- Nouvelle table SQLDelight `Scenario.sq`
- Nouvelle logique métier `ScenarioLogic`
- 2 nouveaux écrans UI (ScenarioListScreen, ScenarioComparisonScreen)

---

### ❌ 6.5 Organisation Logistique
**Status**: Partiellement implémenté (modèles de base existent)

#### 6.5.1 Transport
**Status**: Modèles existent, UI manquante

**Modèles existants**:
- `TransportModels.kt` avec TransportOption, TransportBooking

**Manquant**:
- UI pour saisie lieu de départ par participant
- Gestion des horaires de transport
- Calcul et affichage des coûts
- Optimisation multi-participants (Agent Transport)

#### 6.5.2 Logement
**Status**: Non implémenté

**Requis**:
- Modèle `Accommodation` avec:
  - id, eventId
  - type: AccommodationType (HOTEL, AIRBNB, CAMPING, SHARED_HOUSE)
  - name: String
  - address: String
  - capacity: Int
  - roomAssignments: Map<String, String> (participantId -> roomId)
  - costPerPerson: Double
  - costPerNight: Double
  - totalNights: Int
  - amenities: List<String>
  - bookingStatus: BookingStatus
  - bookingReference: String?

**Fonctionnalités manquantes**:
- Création/modification d'options de logement
- Répartition des chambres
- Calcul des coûts par personne
- UI pour gérer le logement

#### 6.5.3 Nourriture
**Status**: Non implémenté

**Requis**:
- Modèle `Meal` avec:
  - id, eventId, scenarioId
  - type: MealType (BREAKFAST, LUNCH, DINNER, SNACK)
  - date: String
  - name: String
  - assignedTo: List<String> (participantIds responsables)
  - dietaryRestrictions: Map<String, List<DietaryRestriction>>
  - estimatedCost: Double
  - status: MealStatus (PLANNED, PREPARED, COMPLETED)

**Fonctionnalités manquantes**:
- Planification des repas
- Gestion des contraintes alimentaires (végétarien, vegan, allergies, etc.)
- Assignment des responsables
- Suivi des coûts

#### 6.5.4 Équipements & Activités
**Status**: Non implémenté

**Requis**:
- Modèle `EquipmentItem` avec:
  - id, eventId
  - name: String
  - category: EquipmentCategory (CAMPING, SPORTS, COOKING, ELECTRONICS, OTHER)
  - quantity: Int
  - assignedTo: String? (participantId responsable)
  - status: ItemStatus (NEEDED, ASSIGNED, CONFIRMED, PACKED)
  - sharedCost: Double?
  - notes: String?

- Modèle `Activity` avec:
  - id, eventId, scenarioId
  - name: String
  - description: String
  - date: String?
  - duration: Int (minutes)
  - location: String?
  - cost: Double?
  - maxParticipants: Int?
  - registeredParticipants: List<String>
  - organizer: String (participantId)

**Fonctionnalités manquantes**:
- Checklist collaborative d'équipement
- Assignment et suivi des responsables
- Planification d'activités
- Inscription aux activités

---

### ❌ 6.6 Budget
**Status**: Non implémenté

**Requis**:
- Modèle `Budget` avec:
  - id, eventId, scenarioId
  - categories: Map<BudgetCategory, BudgetCategoryDetails>
  - totalEstimated: Double
  - totalActual: Double
  - perPersonEstimated: Double
  - perPersonActual: Double
  - currency: String (ISO code)
  - lastUpdated: String

- Enum `BudgetCategory`:
  - TRANSPORT
  - ACCOMMODATION
  - FOOD
  - ACTIVITIES
  - EQUIPMENT
  - MISCELLANEOUS

- Modèle `BudgetCategoryDetails`:
  - estimated: Double
  - actual: Double
  - items: List<BudgetItem>

- Modèle `BudgetItem`:
  - name: String
  - estimated: Double
  - actual: Double?
  - paidBy: String? (participantId)
  - sharedBy: List<String> (participantIds)
  - date: String?

**Fonctionnalités manquantes**:
- Création et gestion du budget par catégorie
- Calcul automatique du budget par personne
- Mise à jour en temps réel
- Suivi des dépenses réelles vs estimées
- Interface pour saisir/modifier le budget

---

### ❌ 6.7 Collaboration & Communication
**Status**: Notifications implémentées, commentaires manquants

**Existant**:
- `NotificationService` avec support FCM/APNs
- `NotificationModels.kt`

**Manquant**:
- Modèle `Comment` avec:
  - id, eventId
  - section: CommentSection (GENERAL, SCENARIO, TRANSPORT, ACCOMMODATION, FOOD, EQUIPMENT, BUDGET)
  - sectionItemId: String? (référence à un scénario, repas, etc.)
  - authorId: String
  - content: String
  - createdAt: String
  - updatedAt: String?
  - parentCommentId: String? (pour les réponses)

**Fonctionnalités manquantes**:
- Système de commentaires par section
- Fil de discussion
- Notifications de nouveaux commentaires
- UI pour afficher/créer des commentaires

---

### ❌ Nouveaux Statuts d'Événement
**Status**: Partiellement implémenté

**Actuel**: 
```kotlin
enum class EventStatus {
    DRAFT, POLLING, CONFIRMED
}
```

**Requis selon PRD**:
```kotlin
enum class EventStatus {
    DRAFT,        // ✅ Existant
    POLLING,      // ✅ Existant
    COMPARING,    // ❌ Nouveau - pour comparaison de scénarios
    CONFIRMED,    // ✅ Existant
    ORGANIZING,   // ❌ Nouveau - phase de planification logistique
    FINALIZED     // ❌ Nouveau - tout est confirmé, prêt pour le jour J
}
```

**Impact**:
- Modification de l'enum EventStatus
- Migration de la base de données
- Mise à jour des transitions d'état
- Mise à jour de la UI (badges de statut)

---

## 3. Résumé des Gaps

| Fonctionnalité | Status | Priorité | Effort |
|----------------|--------|----------|--------|
| Shortlist de Scénarios | ❌ Non implémenté | Haute | Moyen |
| Transport UI | 🟡 Modèles OK, UI manquante | Haute | Faible |
| Logement | ❌ Non implémenté | Haute | Moyen |
| Nourriture | ❌ Non implémenté | Moyenne | Moyen |
| Équipements & Activités | ❌ Non implémenté | Moyenne | Moyen |
| Budget | ❌ Non implémenté | Haute | Élevé |
| Commentaires | ❌ Non implémenté | Moyenne | Faible |
| Nouveaux Statuts | 🟡 Partiellement | Haute | Faible |

---

## 4. Nouveaux Modèles à Créer

### Priorité Haute
1. `Scenario.kt` + `Scenario.sq`
2. `Budget.kt` + `BudgetCategory.sq` + `BudgetItem.sq`
3. `Accommodation.kt` + `Accommodation.sq`
4. EventStatus avec COMPARING, ORGANIZING, FINALIZED

### Priorité Moyenne
5. `Meal.kt` + `Meal.sq` + `DietaryRestriction.kt`
6. `EquipmentItem.kt` + `EquipmentItem.sq`
7. `Activity.kt` + `Activity.sq`
8. `Comment.kt` + `Comment.sq`

---

## 5. Nouveaux Services/Logique à Créer

1. `ScenarioLogic.kt` - Calcul de scores, classement
2. `BudgetCalculator.kt` - Agrégation, calcul par personne
3. `AccommodationService.kt` - Répartition des chambres
4. `MealPlanner.kt` - Planification des repas
5. `EquipmentManager.kt` - Checklist collaborative
6. `CommentRepository.kt` - Gestion des commentaires

---

## 6. Nouveaux Écrans UI à Créer

### Android (Compose)
1. `ScenarioListScreen.kt` - Liste et comparaison de scénarios
2. `ScenarioDetailScreen.kt` - Détails et vote d'un scénario
3. `TransportPlanningScreen.kt` - Planification transport
4. `AccommodationScreen.kt` - Gestion logement
5. `MealPlanningScreen.kt` - Planification repas
6. `EquipmentChecklistScreen.kt` - Checklist équipements
7. `ActivityPlanningScreen.kt` - Planification activités
8. `BudgetOverviewScreen.kt` - Vue d'ensemble budget
9. `BudgetDetailScreen.kt` - Détail par catégorie
10. `CommentsScreen.kt` - Fil de commentaires

### iOS (SwiftUI)
- Équivalents de tous les écrans ci-dessus

---

## 7. Nouveaux Endpoints API

1. `POST /api/events/{id}/scenarios` - Créer scénario
2. `GET /api/events/{id}/scenarios` - Liste scénarios
3. `PUT /api/events/{id}/scenarios/{scenarioId}` - Modifier scénario
4. `DELETE /api/events/{id}/scenarios/{scenarioId}` - Supprimer scénario
5. `POST /api/events/{id}/scenarios/{scenarioId}/votes` - Voter scénario
6. `GET /api/events/{id}/budget` - Récupérer budget
7. `PUT /api/events/{id}/budget` - Mettre à jour budget
8. `POST /api/events/{id}/accommodation` - Ajouter logement
9. `POST /api/events/{id}/meals` - Ajouter repas
10. `POST /api/events/{id}/equipment` - Ajouter équipement
11. `POST /api/events/{id}/activities` - Ajouter activité
12. `POST /api/events/{id}/comments` - Ajouter commentaire
13. `GET /api/events/{id}/comments` - Liste commentaires

---

## 8. Tests à Créer

### Tests Unitaires
- `ScenarioLogicTest` - Tests de scoring et classement (≥ 8 tests)
- `BudgetCalculatorTest` - Tests de calcul budget (≥ 10 tests)
- `AccommodationServiceTest` - Tests de répartition (≥ 6 tests)
- `MealPlannerTest` - Tests de planification (≥ 5 tests)
- `CommentRepositoryTest` - Tests de commentaires (≥ 8 tests)

### Tests d'Intégration
- `ScenarioIntegrationTest` - Création, vote, sélection (≥ 5 tests)
- `BudgetIntegrationTest` - Mise à jour en temps réel (≥ 5 tests)
- `LogisticsIntegrationTest` - Transport + Logement + Repas (≥ 8 tests)

**Total estimé**: ≥ 55 nouveaux tests

---

## 9. Effort Estimé

| Phase | Composant | Effort (jours) |
|-------|-----------|----------------|
| 1 | Modèles + DB | 3-4 |
| 2 | Logique métier + Services | 4-5 |
| 3 | UI Android (Compose) | 8-10 |
| 4 | UI iOS (SwiftUI) | 8-10 |
| 5 | API REST endpoints | 3-4 |
| 6 | Tests | 5-6 |
| 7 | Documentation | 2 |

**Total**: 33-41 jours de développement

---

## 10. Recommandations d'Implémentation

### Phase 1 - Fondations (Semaine 1-2)
1. ✅ Créer tous les nouveaux modèles de données
2. ✅ Créer toutes les tables SQLDelight
3. ✅ Ajouter les nouveaux statuts EventStatus
4. ✅ Écrire les tests unitaires pour les modèles

### Phase 2 - Scénarios & Budget (Semaine 3-4)
1. ✅ Implémenter ScenarioLogic et ScenarioRepository
2. ✅ Implémenter BudgetCalculator et BudgetRepository
3. ✅ Créer UI pour scénarios (Android + iOS)
4. ✅ Créer UI pour budget (Android + iOS)
5. ✅ Ajouter endpoints API

### Phase 3 - Logistique (Semaine 5-6)
1. ✅ Implémenter AccommodationService
2. ✅ Implémenter MealPlanner
3. ✅ Implémenter EquipmentManager
4. ✅ Créer UI pour logistique (Android + iOS)
5. ✅ Ajouter endpoints API

### Phase 4 - Collaboration (Semaine 7)
1. ✅ Implémenter CommentRepository
2. ✅ Créer UI pour commentaires
3. ✅ Intégrer notifications
4. ✅ Tests d'intégration complets

### Phase 5 - Polish & Documentation (Semaine 8)
1. ✅ Tests end-to-end
2. ✅ Optimisation performance
3. ✅ Documentation utilisateur
4. ✅ Documentation développeur

---

## 11. Prochaines Actions

1. ✅ Créer proposition OpenSpec `add-full-prd-features`
2. ✅ Définir les spécifications détaillées pour chaque fonctionnalité
3. ✅ Créer les branches de développement
4. ✅ Commencer l'implémentation par Phase 1

---

**Document créé**: 25 décembre 2025  
**Auteur**: Analyse automatique basée sur PRD  
**Status**: Prêt pour revue et approbation
