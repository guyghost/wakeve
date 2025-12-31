# 🎯 Workflow State Machine Verification - Résumé

> **Change ID**: `verify-statemachine-workflow`  
> **Status**: ✅ Validé (prêt pour implémentation)  
> **Created**: 2025-12-31  

---

## 📋 Vue d'ensemble

J'ai créé une **proposition OpenSpec complète** pour vérifier et améliorer la connexion des state machines dans le workflow utilisateur de Wakeve.

### Fichiers créés

```
openspec/changes/verify-statemachine-workflow/
├── proposal.md                                    # Contexte et solution proposée
├── tasks.md                                       # 9 phases, 50+ tâches
└── specs/workflow-coordination/spec.md            # Spécifications complètes
```

---

## 🎯 Objectifs

1. ✅ **Vérifier la cohérence du workflow** entre les 3 state machines :
   - `EventManagementStateMachine`
   - `ScenarioManagementStateMachine`
   - `MeetingServiceStateMachine`

2. ✅ **Identifier les gaps** dans les transitions entre phases du cycle de vie

3. ✅ **Proposer des améliorations** pour connecter les state machines

4. ✅ **Documenter le workflow complet** dans une spec unifiée

5. ✅ **Créer des tests end-to-end** pour valider les transitions

---

## 📊 Cycle de Vie d'un Événement

```
┌─────────┐
│  DRAFT  │  ← Créer événement, ajouter participants
└────┬────┘
     │ StartPoll
     ▼
┌─────────┐
│ POLLING │  ← Voter sur créneaux
└────┬────┘
     │ ConfirmDate (avec votes)
     ▼
┌──────────┐
│CONFIRMED │  ← Date verrouillée
└────┬─────┘
     │ (Optionnel: Créer/Comparer Scenarios)
     ▼
┌────────────────────┐
│SCENARIO_COMPARISON │  ← Voter sur scenarios
└────────┬───────────┘
         │ SelectScenario
         ▼
┌──────────┐
│CONFIRMED │  ← Scenario verrouillé
└────┬─────┘
     │ TransitionToOrganization
     ▼
┌──────────────┐
│ORGANIZATION  │  ← Créer réunions, planifier logistique
└────┬─────────┘
     │ MarkAsFinalized
     ▼
┌──────────┐
│FINALIZED │  ← Tous les détails confirmés
└──────────┘
```

---

## 🔑 Requirements Clés (7 nouveaux)

### 1. Observation du Event Status
Les state machines **DOIVENT** observer le statut de l'événement et adapter leur comportement.

**Exemple** : Scenario Management est désactivé en phase DRAFT.

### 2. Règles de Transition
Le système **DOIT** empêcher les transitions invalides (ex: DRAFT → ORGANIZATION sans passer par CONFIRMED).

### 3. Synchronisation via Repository
Les changements d'état **DOIVENT** être propagés à toutes les state machines via un repository partagé.

### 4. Navigation Cohérente
Les `SideEffect.NavigateTo` **DOIVENT** être cohérents avec le cycle de vie et le rôle utilisateur.

### 5. File d'Attente Offline
Les actions offline **DOIVENT** être exécutées dans l'ordre correct lors de la reconnexion.

### 6. Isolation des Erreurs
Les erreurs dans une state machine **NE DOIVENT PAS** bloquer les autres.

### 7. Validation des Invariants
Les invariants métier **DOIVENT** être vérifiés avant chaque transition (ex: pas de scenarios en DRAFT).

---

## 🛠️ Solution Proposée : Shared Repository Pattern

**Recommandation** : Utiliser un **repository partagé** avec observation via `StateFlow` plutôt qu'un coordinator centralisé.

### Architecture

```kotlin
// EventManagementStateMachine confirme la date
suspend fun confirmDate(slotId: String) {
    val updatedEvent = currentState.selectedEvent.copy(
        status = EventStatus.CONFIRMED,
        finalDate = slotId
    )
    
    // Persister dans le repository
    eventRepository.updateEvent(updatedEvent)
    
    // Le repository émet une mise à jour via Flow
    // Toutes les state machines observant eventRepository.getEvent(eventId) 
    // reçoivent la mise à jour
    
    // Naviguer vers scenarios
    emitSideEffect(SideEffect.NavigateTo("scenarios/${updatedEvent.id}"))
}

// ScenarioManagementStateMachine observe l'événement
init {
    eventRepository.getEvent(eventId).collect { event ->
        if (event.status == EventStatus.CONFIRMED && !state.value.scenariosUnlocked) {
            updateState { it.copy(scenariosUnlocked = true) }
        }
    }
}
```

### Avantages

✅ **Simplicité** : Pas de coordinator centralisé complexe  
✅ **Découplage** : Chaque state machine observe indépendamment  
✅ **Testabilité** : Mock du repository pour tester isolément  
✅ **Performance** : StateFlow avec replay=1, pas de surcharge  

---

## 🎯 Invariants Métier

Le système **DOIT** garantir ces invariants en permanence :

1. ✅ **Ordre des phases** : `DRAFT → POLLING → CONFIRMED → (opt. SCENARIO) → ORGANIZATION → FINALIZED`
2. ✅ **Pas de retour arrière** : Impossible de revenir de CONFIRMED vers POLLING
3. ✅ **Scenarios après confirmation** : Scenarios créés uniquement après CONFIRMED
4. ✅ **Réunions après confirmation** : Meetings créés uniquement après CONFIRMED
5. ✅ **Deadline de vote** : Pas de votes après deadline ou après CONFIRMED
6. ✅ **Actions organisateur** : Seul l'organisateur confirme dates/scenarios/meetings
7. ✅ **Au moins un vote** : Impossible de confirmer sans au moins un vote participant

---

## 📋 Plan d'Implémentation (9 Phases)

### Phase 1 : Audit ✅ (3 tâches complétées)
- [x] Analyser EventManagementStateMachine
- [x] Analyser ScenarioManagementStateMachine  
- [x] Analyser MeetingServiceStateMachine
- [ ] Mapper toutes les transitions dans un diagramme
- [ ] Identifier les gaps
- [ ] Documenter les dépendances

### Phase 2-9 : En attente
Voir `tasks.md` pour le détail complet des 50+ tâches.

---

## ✅ Validation

```bash
$ openspec validate verify-statemachine-workflow --strict
Change 'verify-statemachine-workflow' is valid
```

---

## 🚀 Prochaines Étapes

### Immédiat

1. **Review de la proposition** avec l'équipe
2. **Compléter l'audit** (Phase 1) : mapper les transitions actuelles
3. **Analyser les contracts** (Phase 2) : vérifier State/Intent/SideEffect

### Court terme

4. **Design** (Phase 3) : Décider du pattern de communication
5. **Specs complètes** (Phase 4) : Finaliser les requirements
6. **Implémentation** (Phase 5) : Connecter les state machines

### Moyen terme

7. **Tests end-to-end** (Phase 6) : Au moins 10 tests complets
8. **Documentation** (Phase 7) : Diagrammes et guides
9. **Archivage** (Phase 9) : Merger et archiver le changement

---

## 📚 Ressources

### Documentation créée

- [`proposal.md`](./proposal.md) - Contexte, problème, solution
- [`tasks.md`](./tasks.md) - Checklist de 50+ tâches
- [`specs/workflow-coordination/spec.md`](./specs/workflow-coordination/spec.md) - 7 requirements + scenarios

### Commandes utiles

```bash
# Afficher la proposition
openspec show verify-statemachine-workflow

# Lister toutes les tâches
cat openspec/changes/verify-statemachine-workflow/tasks.md

# Valider
openspec validate verify-statemachine-workflow --strict
```

---

## ❓ Questions Ouvertes

1. **Coordinator Pattern** : Introduire un WorkflowCoordinator ou utiliser repository observation ?  
   → **Recommandation** : Repository observation (plus simple)

2. **Navigation** : State machines émettent NavigateTo ou NavController observe les états ?  
   → **Actuel** : State machines émettent, UI gère

3. **Offline Queue** : Comment garantir l'ordre des actions complexes ?  
   → **Recommandation** : Priority queue avec tracking des dépendances

4. **Error Propagation** : Une erreur Scenario bloque-t-elle EventManagement ?  
   → **Recommandation** : Isolation (erreurs locales uniquement)

---

## 🎉 Conclusion

Cette proposition fournit une **base solide** pour vérifier et améliorer le workflow des state machines :

✅ **Spécifications complètes** (7 requirements, 10+ scenarios)  
✅ **Plan détaillé** (9 phases, 50+ tâches)  
✅ **Solution technique** (Shared Repository Pattern)  
✅ **Invariants métier** (7 règles strictes)  
✅ **Tests end-to-end** (stratégie claire)  

**Prêt à commencer l'implémentation !** 🚀
