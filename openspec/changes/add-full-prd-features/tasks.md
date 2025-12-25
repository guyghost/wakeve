# Tasks - Implémentation Complète du PRD

## Change: `add-full-prd-features`
**Status**: 🟡 En cours  
**Dernière mise à jour**: 25 décembre 2025

---

## Phase 1 - Scénarios & Nouveaux Statuts (Sprint 1-2)

### Sprint 1.1 - Modèles & Base de Données

- [ ] **Task 1.1.1**: Créer `Scenario.kt` avec tous les champs
  - [ ] Ajouter `@Serializable` annotation
  - [ ] Définir `ScenarioVote` enum
  - [ ] Définir `ScenarioStatus` enum
  - [ ] Ajouter validation des données

- [ ] **Task 1.1.2**: Créer `Scenario.sq` table SQLDelight
  - [ ] Définir schéma avec index
  - [ ] Ajouter queries: insert, select, update, delete
  - [ ] Ajouter query pour ranking par score

- [ ] **Task 1.1.3**: Créer `ScenarioVote.sq` table
  - [ ] Schéma avec clés étrangères
  - [ ] Queries pour agrégation des votes

- [ ] **Task 1.1.4**: Étendre `EventStatus` enum
  - [ ] Ajouter `COMPARING`
  - [ ] Mettre à jour Event.sq
  - [ ] Migration de base de données

### Sprint 1.2 - Logique Métier

- [ ] **Task 1.2.1**: Implémenter `ScenarioLogic.kt`
  - [ ] `calculateScenarioScore()` - PREFER=2, NEUTRAL=0, AGAINST=-2
  - [ ] `rankScenarios()` - Tri par score total
  - [ ] `getBestScenario()` - Retourner le meilleur
  - [ ] Tests: ScenarioLogicTest (≥8 tests)

- [ ] **Task 1.2.2**: Implémenter `ScenarioRepository.kt`
  - [ ] `createScenario()`
  - [ ] `getScenarios(eventId)`
  - [ ] `updateScenario()`
  - [ ] `deleteScenario()`
  - [ ] `voteOnScenario()`
  - [ ] `getRankedScenarios()`
  - [ ] Tests: ScenarioRepositoryTest (≥8 tests)

### Sprint 1.3 - UI Android (Compose)

- [ ] **Task 1.3.1**: Créer `ScenarioListScreen.kt`
  - [ ] Liste des scénarios avec cards
  - [ ] Affichage du score par scénario
  - [ ] Badge de statut (PROPOSED, SELECTED)
  - [ ] Bouton "Ajouter scénario"
  - [ ] Navigation vers détails

- [ ] **Task 1.3.2**: Créer `ScenarioDetailScreen.kt`
  - [ ] Affichage de tous les détails
  - [ ] Boutons de vote (PREFER, NEUTRAL, AGAINST)
  - [ ] Affichage des votes agrégés
  - [ ] Bouton "Modifier" (si organisateur)
  - [ ] Bouton "Sélectionner" (si organisateur)

- [ ] **Task 1.3.3**: Créer `ScenarioComparisonScreen.kt`
  - [ ] Vue côte-à-côte (2-3 scénarios)
  - [ ] Comparaison visuelle des budgets
  - [ ] Comparaison des durées et lieux
  - [ ] Highlight du meilleur score

- [ ] **Task 1.3.4**: Créer `ScenarioCreationScreen.kt`
  - [ ] Form avec tous les champs
  - [ ] Validation des entrées
  - [ ] Date picker / Period selector
  - [ ] Location autocomplete (future)

### Sprint 1.4 - UI iOS (SwiftUI)

- [ ] **Task 1.4.1**: Créer `ScenarioListView.swift`
  - [ ] Équivalent de ScenarioListScreen
  - [ ] Utiliser design system Liquid Glass

- [ ] **Task 1.4.2**: Créer `ScenarioDetailView.swift`
  - [ ] Équivalent de ScenarioDetailScreen
  - [ ] Animations natives iOS

- [ ] **Task 1.4.3**: Créer `ScenarioComparisonView.swift`
  - [ ] Layout adapté iOS
  - [ ] Graphiques comparatifs

- [ ] **Task 1.4.4**: Créer `ScenarioCreationView.swift`
  - [ ] Form avec pickers natifs iOS

### Sprint 1.5 - API REST

- [ ] **Task 1.5.1**: Créer endpoints Scénarios
  - [ ] `POST /api/events/{id}/scenarios`
  - [ ] `GET /api/events/{id}/scenarios`
  - [ ] `GET /api/events/{id}/scenarios/{scenarioId}`
  - [ ] `PUT /api/events/{id}/scenarios/{scenarioId}`
  - [ ] `DELETE /api/events/{id}/scenarios/{scenarioId}`
  - [ ] `POST /api/events/{id}/scenarios/{scenarioId}/votes`

- [ ] **Task 1.5.2**: Tests API
  - [ ] Tests d'intégration pour chaque endpoint
  - [ ] Tests de validation
  - [ ] Tests de permissions (organizer vs participant)

### Sprint 1.6 - Documentation & Tests E2E

- [ ] **Task 1.6.1**: Documentation
  - [ ] Mettre à jour openspec/specs/scenario-management/spec.md
  - [ ] Documenter les nouveaux endpoints API
  - [ ] Screenshots des nouveaux écrans

- [ ] **Task 1.6.2**: Tests End-to-End Phase 1
  - [ ] Scénario: Créer 3 scénarios et voter
  - [ ] Scénario: Comparer et sélectionner un scénario
  - [ ] Scénario: Passer en statut COMPARING

---

## Phase 2 - Budget (Sprint 3-4)

### Sprint 2.1 - Modèles & Base de Données

- [ ] **Task 2.1.1**: Créer `Budget.kt`, `BudgetItem.kt`
  - [ ] Modèles avec `@Serializable`
  - [ ] `BudgetCategory` enum
  - [ ] `BudgetCategoryDetails` data class

- [ ] **Task 2.1.2**: Créer `Budget.sq` et `BudgetItem.sq`
  - [ ] Schémas avec relations
  - [ ] Queries d'agrégation

### Sprint 2.2 - Logique Métier

- [ ] **Task 2.2.1**: Implémenter `BudgetCalculator.kt`
  - [ ] `calculateTotalBudget()`
  - [ ] `calculatePerPersonBudget()`
  - [ ] `calculateCategoryTotals()`
  - [ ] `updateBudgetFromLogistics()` - Auto-update
  - [ ] Tests: BudgetCalculatorTest (≥10 tests)

- [ ] **Task 2.2.2**: Implémenter `BudgetRepository.kt`
  - [ ] CRUD operations
  - [ ] Agrégation par catégorie
  - [ ] Mise à jour en temps réel
  - [ ] Tests: BudgetRepositoryTest (≥8 tests)

### Sprint 2.3 - UI Android

- [ ] **Task 2.3.1**: Créer `BudgetOverviewScreen.kt`
  - [ ] Graphiques circulaires par catégorie
  - [ ] Budget total et par personne
  - [ ] Comparaison estimé vs réel
  - [ ] Navigation vers détails par catégorie

- [ ] **Task 2.3.2**: Créer `BudgetDetailScreen.kt`
  - [ ] Liste des items de budget
  - [ ] Ajout/modification d'items
  - [ ] Filtrage par catégorie
  - [ ] Export CSV (future)

### Sprint 2.4 - UI iOS

- [ ] **Task 2.4.1**: Créer `BudgetOverviewView.swift`
- [ ] **Task 2.4.2**: Créer `BudgetDetailView.swift`

### Sprint 2.5 - API & Documentation

- [ ] **Task 2.5.1**: Créer endpoints Budget
  - [ ] `GET /api/events/{id}/budget`
  - [ ] `PUT /api/events/{id}/budget`
  - [ ] `POST /api/events/{id}/budget/items`
  - [ ] `PUT /api/events/{id}/budget/items/{itemId}`
  - [ ] `DELETE /api/events/{id}/budget/items/{itemId}`

- [ ] **Task 2.5.2**: Documentation & Tests E2E Phase 2

---

## Phase 3 - Logistique (Sprint 5-7)

### Sprint 3.1 - Logement

- [ ] **Task 3.1.1**: Créer `Accommodation.kt`
  - [ ] Modèle avec tous les champs
  - [ ] `AccommodationType` enum
  - [ ] `BookingStatus` enum

- [ ] **Task 3.1.2**: Créer `Accommodation.sq` et `RoomAssignment.sq`

- [ ] **Task 3.1.3**: Implémenter `AccommodationService.kt`
  - [ ] `assignRooms()` - Algorithme de répartition
  - [ ] `calculateCostPerPerson()`
  - [ ] `validateCapacity()`
  - [ ] Tests: AccommodationServiceTest (≥6 tests)

- [ ] **Task 3.1.4**: Créer `AccommodationScreen.kt` (Android)
  - [ ] Form d'ajout de logement
  - [ ] Répartition des chambres (drag & drop)
  - [ ] Calcul auto du coût par personne

- [ ] **Task 3.1.5**: Créer `AccommodationView.swift` (iOS)

- [ ] **Task 3.1.6**: Endpoints API Logement
  - [ ] `POST /api/events/{id}/accommodation`
  - [ ] `GET /api/events/{id}/accommodation`
  - [ ] `PUT /api/events/{id}/accommodation/{accommodationId}`

### Sprint 3.2 - Transport & Repas

- [ ] **Task 3.2.1**: Améliorer `TransportPlanningScreen.kt`
  - [ ] Ajout lieu de départ par participant
  - [ ] Sélection type de transport
  - [ ] Saisie horaires et coûts
  - [ ] Intégration budget

- [ ] **Task 3.2.2**: Créer `Meal.kt` et `DietaryRestriction.kt`
  - [ ] Modèles complets
  - [ ] `MealType` enum
  - [ ] `MealStatus` enum

- [ ] **Task 3.2.3**: Créer `Meal.sq` et `DietaryRestrictionMapping.sq`

- [ ] **Task 3.2.4**: Implémenter `MealPlanner.kt`
  - [ ] `planMeals()` - Génération automatique
  - [ ] `assignMealResponsibilities()`
  - [ ] `validateDietaryRestrictions()`
  - [ ] Tests: MealPlannerTest (≥5 tests)

- [ ] **Task 3.2.5**: Créer `MealPlanningScreen.kt` (Android)
  - [ ] Calendrier des repas
  - [ ] Assignment des responsables
  - [ ] Saisie contraintes alimentaires

- [ ] **Task 3.2.6**: Créer `MealPlanningView.swift` (iOS)

- [ ] **Task 3.2.7**: Endpoints API Repas
  - [ ] `POST /api/events/{id}/meals`
  - [ ] `GET /api/events/{id}/meals`
  - [ ] `PUT /api/events/{id}/meals/{mealId}`

### Sprint 3.3 - Équipements & Activités

- [ ] **Task 3.3.1**: Créer `EquipmentItem.kt`
  - [ ] `EquipmentCategory` enum
  - [ ] `ItemStatus` enum

- [ ] **Task 3.3.2**: Créer `EquipmentItem.sq`

- [ ] **Task 3.3.3**: Implémenter `EquipmentManager.kt`
  - [ ] `createChecklist()` - Génération basée sur type d'événement
  - [ ] `assignEquipment()`
  - [ ] `trackEquipmentStatus()`
  - [ ] Tests: EquipmentManagerTest (≥5 tests)

- [ ] **Task 3.3.4**: Créer `EquipmentChecklistScreen.kt` (Android)
  - [ ] Checklist avec checkboxes
  - [ ] Assignment par item
  - [ ] Filtre par statut

- [ ] **Task 3.3.5**: Créer `Activity.kt`
  - [ ] Modèle complet

- [ ] **Task 3.3.6**: Créer `Activity.sq` et `ActivityParticipant.sq`

- [ ] **Task 3.3.7**: Créer `ActivityPlanningScreen.kt` (Android)
  - [ ] Ajout d'activités
  - [ ] Inscription des participants
  - [ ] Gestion capacité max

- [ ] **Task 3.3.8**: Créer vues iOS équivalentes

- [ ] **Task 3.3.9**: Endpoints API
  - [ ] Équipements: POST, GET, PUT
  - [ ] Activités: POST, GET, POST register

- [ ] **Task 3.3.10**: Ajouter nouveaux statuts `ORGANIZING` et `FINALIZED`
  - [ ] Mise à jour enum
  - [ ] Migration DB
  - [ ] Update UI badges

---

## Phase 4 - Collaboration (Sprint 8)

### Sprint 4.1 - Commentaires

- [ ] **Task 4.1.1**: Créer `Comment.kt`
  - [ ] `CommentSection` enum
  - [ ] Support threads (parentCommentId)

- [ ] **Task 4.1.2**: Créer `Comment.sq`

- [ ] **Task 4.1.3**: Implémenter `CommentRepository.kt`
  - [ ] CRUD operations
  - [ ] Queries par section
  - [ ] Thread building
  - [ ] Tests: CommentRepositoryTest (≥8 tests)

- [ ] **Task 4.1.4**: Créer `CommentsScreen.kt` (Android)
  - [ ] Liste des commentaires avec threads
  - [ ] Ajout de commentaires
  - [ ] Réponses à des commentaires
  - [ ] Filtrage par section

- [ ] **Task 4.1.5**: Intégrer commentaires dans chaque section
  - [ ] ScenarioDetailScreen
  - [ ] BudgetDetailScreen
  - [ ] AccommodationScreen
  - [ ] MealPlanningScreen
  - [ ] EquipmentChecklistScreen
  - [ ] ActivityPlanningScreen

- [ ] **Task 4.1.6**: Créer vues iOS

- [ ] **Task 4.1.7**: Endpoints API Commentaires
  - [ ] `POST /api/events/{id}/comments`
  - [ ] `GET /api/events/{id}/comments?section=...&itemId=...`
  - [ ] `PUT /api/events/{id}/comments/{commentId}`
  - [ ] `DELETE /api/events/{id}/comments/{commentId}`

### Sprint 4.2 - Notifications & Polish

- [ ] **Task 4.2.1**: Intégrer notifications commentaires
  - [ ] Notification quand quelqu'un commente
  - [ ] Notification de réponses

- [ ] **Task 4.2.2**: Tests d'intégration complets
  - [ ] Workflow complet: Création → Scénarios → Budget → Logistique → Commentaires
  - [ ] Tests multi-utilisateurs

- [ ] **Task 4.2.3**: Performance optimization
  - [ ] Index DB optimization
  - [ ] Lazy loading
  - [ ] Cache strategies

- [ ] **Task 4.2.4**: Documentation finale
  - [ ] Guide utilisateur
  - [ ] API documentation complète
  - [ ] Architecture documentation

---

## Résumé de Progression

### Phase 1 - Scénarios
- [ ] 0/6 sprints complétés
- [ ] 0/24 tasks complétées

### Phase 2 - Budget
- [ ] 0/5 sprints complétés
- [ ] 0/10 tasks complétées

### Phase 3 - Logistique
- [ ] 0/3 sprints complétés
- [ ] 0/19 tasks complétées

### Phase 4 - Collaboration
- [ ] 0/2 sprints complétés
- [ ] 0/7 tasks complétées

### **Total Progression**: 0/60 tasks (0%)

---

## Notes

- Chaque task doit inclure des tests
- UI doit respecter le design system (.opencode/design-system.md)
- Suivre les conventions de code (Kotlin/Swift)
- TDD: Tests avant implémentation
- Documentation au fur et à mesure

---

**Dernière mise à jour**: 25 décembre 2025  
**Prochaine revue**: Après Phase 1 Sprint 1
