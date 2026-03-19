# Context: Fix Critical TODOs (Ralph Mode)

## Objective
Corriger les TODOs critiques identifiés dans le changement `complete-phase5-and-fixes` et réduire le nombre total de TODOs.

## Résumé des Corrections

### ✅ TODOs Critiques Corrigés (Change complete-phase5-and-fixes)

| TODO | Description | Fichier | Statut |
|------|-------------|---------|--------|
| TODO-001 | OAuth config hardcodé → BuildConfig | MainActivity.kt:34 | ✅ Déjà résolu |
| TODO-002 | SHA-256 non implémenté sur iOS | SessionRepository.ios.kt:21 | ✅ Déjà résolu |
| TODO-003 | AccommodationRoutes repository | AccommodationRoutes.kt | ✅ Déjà résolu (fichier complet) |
| TODO-004 | participantCount hardcodé | BudgetRoutes.kt:378 | ✅ Corrigé |
| TODO-005 | paidBy/sharedBy hardcodés | BudgetDetailScreen.kt:238,669 | ✅ Corrigé |
| TODO-006 | Validate user is organizer | EventManagementStateMachine.kt:668,731,826,900 | ✅ Corrigé |
| TODO-007 | Validate user is organizer | ScenarioManagementStateMachine.kt:422 | ✅ Corrigé |

### ✅ TODOs Additionnels Corrigés (Nettoyage Run 3)

| Description | Fichier | Action |
|-------------|---------|--------|
| 3 TODOs obsolètes | EventManagementStateMachine.kt | Suppression commentaires lignes 738, 833, 907 |
| participantCount hardcodé | BudgetOverviewScreen.kt:70 | Chargement dynamique depuis event |
| Load/Save/Delete repository | AccommodationScreen.kt:100,237,259 | Implémentation complète |
| currentSessionId/currentUserName | WakevNavHost.kt:604,795 | Récupération depuis authState |

## Bilan des TODOs

| Métrique | Avant | Après | Delta |
|----------|-------|-------|-------|
| TODOs critiques | 7 | 0 | -7 ✅ |
| TODOs obsolètes | 3 | 0 | -3 ✅ |
| TODOs faciles | 4 | 0 | -4 ✅ |
| **Total TODOs** | **~54** | **40** | **-14** |

## TODOs Restants (40) - Par Catégorie

### Fonctionnalités Futures (à garder pour Phase 3+)
- **Suggestions**: 7 TODOs (interaction tracking - feature future)
- **Image Loading**: 4 TODOs (attente Coil pour KMP)
- **MeetingService**: 2 TODOs (Phase 4/5)
- **Auth OTP Backend**: 2 TODOs (backend wiring)

### Améliorations Mineures
- **UI/UX**: ~15 TODOs (options menu, templates, time formatting)
- **Settings**: 3 TODOs (session management)
- **Server**: 6 TODOs (cache, notifications, reminders)

## Fichiers Modifiés

### TODOs Critiques (Run 1-2)
1. `server/src/main/kotlin/.../BudgetRoutes.kt`
2. `server/src/main/kotlin/.../Application.kt`
3. `wakeveApp/src/.../BudgetDetailScreen.kt`
4. `wakeveApp/src/.../EventManagementContract.kt`
5. `wakeveApp/src/.../EventManagementStateMachine.kt`
6. `wakeveApp/src/.../ScenarioManagementStateMachine.kt`
7. `shared/src/.../DraftPhaseIntegrationTest.kt`

### Nettoyage Additionnel (Run 3)
8. `wakeveApp/src/.../BudgetOverviewScreen.kt`
9. `wakeveApp/src/.../AccommodationScreen.kt`
10. `wakeveApp/src/.../WakevNavHost.kt`

## Validation

- ✅ **Tests**: 36/36 passent (100%)
- ✅ **Build**: SUCCESSFUL
- ✅ **TODOs critiques**: 0 restant
- ✅ **TODOs obsolètes**: 0 restant

## Prochaines Étapes Recommandées

Pour atteindre l'objectif de < 20 TODOs:

1. **Implémenter Suggestions Tracking** (7 TODOs) - Fonctionnalité Phase 3
2. **Ajouter Auth Context au Server** (3 TODOs dans CommentRoutes) - Nécessite middleware auth
3. **Compléter Settings Screen** (3 TODOs) - Gestion des sessions
4. **Implémenter Date Validation** (1 TODO dans AccommodationService)

Ou alternativement:
- Accepter 40 TODOs comme baseline pour Phase 3 (majorité sont features futures)
- Créer des changements OpenSpec dédiés pour chaque groupe de TODOs

## Mode Ralph

- **Itérations utilisées**: 2
- **Itérations max**: 10
- **Statut**: APPROVED ✅
- **Prochaine action**: Décider si on continue à réduire les TODOs ou on passe à la Phase 5

## Contraintes
- **Mode**: Ralph (max 10 itérations)
- **Tests**: Maintenir 100% de passage
- **Architecture**: FC&IS (Functional Core & Imperative Shell)

## Décisions Techniques
| Décision | Justification |
|----------|---------------|
| Injecter userId dans les méthodes concernées | Permet la validation d'organisateur |
| Utiliser EventRepositoryInterface pour récupérer participantCount | Évite les valeurs hardcodées + utilise interface existante |
| Passer currentUserId et eventParticipants à BudgetDetailScreen | Permet d'utiliser l'utilisateur courant et la liste des participants réels |
| Utiliser expectedParticipants ou participants.size | Priorité à expectedParticipants si défini, sinon participants.size |

## Notes Inter-Agents
- [@orchestrator → @codegen] ✅ TODOs critiques corrigés
- [@orchestrator → @codegen] ✅ Nettoyage des TODOs obsolètes et faciles effectué
- [@orchestrator → @tests] ✅ Tous les tests passent (36/36)
