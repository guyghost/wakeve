# Présentation - Align DRAFT Workflow with UI Interfaces

> **Présenté par**: OpenSpec Orchestrator
> **Date**: 2026-01-04
> **Audience**: Développeurs Wakeve (Android, iOS, Backend)
> **Durée**: 15 min

---

## 📋 Agenda

1. **Introduction** (2 min)
   - Contexte du changement
   - Objectifs et scope

2. **Workflow DRAFT** (5 min)
   - Structure en 4 étapes
   - Mapping UI ↔ State Machine
   - Validation et side effects

3. **Dépréciation EventCreationScreen** (3 min)
   - Pourquoi migrer
   - Timeline de migration
   - Comment migrer

4. **Documentation et Tests** (3 min)
   - Nouveaux guides créés
   - Tests d'intégration
   - Accessibilité validée

5. **Questions et Discussion** (2 min)

---

## 1️⃣ Introduction

### Contexte

Actuellement, l'application possède :
- ✅ Machines à états (EventManagementStateMachine) orchestrant le workflow
- ✅ Wizard en 4 étapes sur Android (DraftEventWizard.kt) et iOS (DraftEventWizardView.swift)
- ⚠️ Ancien écran Android (EventCreationScreen.kt) sans wizard

### Problème Identifié

**Incohérence** : Le workflow DRAFT n'est pas formellement documenté comme référence unique

**Solution** : Ce changement documentaire aligne et définit le workflow DRAFT

### Objectifs

1. ✅ Documenter le workflow DRAFT comme référence unique
2. ✅ Aligner les interfaces UI avec la state machine
3. ✅ Déprécier EventCreationScreen.kt
4. ✅ Créer guides de documentation pour développeurs

### Scope

**Inclus** :
- Documentation du workflow DRAFT (4 étapes)
- Mapping étapes UI ↔ Intents State Machine
- Dépréciation EventCreationScreen avec guide de migration
- Tests d'intégration (8 tests)
- 3 guides de documentation (1300+ lignes)

**Exclus** :
- Modification du code existant (sauf dépréciation)
- Migration complète de EventCreationScreen (pour prochaine version)

---

## 2️⃣ Workflow DRAFT

### Structure du Wizard (4 Étapes)

```
┌─────────────────────────────────────────────────────────────┐
│                  DRAFT PHASE WIZARD                   │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Step 1: Basic Info (REQUIRED)                           │
│  • Title (required)                                        │
│  • Description (required)                                    │
│  • EventType (optional, default: OTHER)                     │
│                                                              │
│           ↓ [Auto-save + Validate]                         │
│                                                              │
│  Step 2: Participants Estimation (OPTIONAL)                 │
│  • minParticipants (optional)                              │
│  • maxParticipants (optional)                              │
│  • expectedParticipants (optional)                            │
│                                                              │
│           ↓ [Auto-save + Validate]                         │
│                                                              │
│  Step 3: Potential Locations (OPTIONAL)                    │
│  • Add/Remove Location (optional)                          │
│                                                              │
│           ↓ [Auto-save + Validate]                         │
│                                                              │
│  Step 4: Time Slots (REQUIRED)                            │
│  • Add Time Slot (1+ required)                            │
│  • Remove Time Slot                                      │
│  • Time of Day selection                                    │
│                                                              │
│           ↓ [Validate + Create Event]                        │
│                                                              │
│  Event Created → NavigateTo "event-detail/{id}"             │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Mapping UI ↔ State Machine

| Action UI | Intent State Machine | Side Effect |
|-----------|---------------------|-------------|
| Step 1: Fill & Next | `UpdateDraftEvent` | Auto-save |
| Step 2: Fill & Next | `UpdateDraftEvent` | Auto-save |
| Step 3: Add Location | `AddPotentialLocation` | Auto-save |
| Step 3: Remove Location | `RemovePotentialLocation` | Auto-save |
| Step 4: Add Slot | `AddTimeSlot` | Auto-save |
| Step 4: Remove Slot | `RemoveTimeSlot` | Auto-save |
| Complete Wizard | `CreateEvent` | NavigateTo("detail/{id}") |
| Cancel Wizard | - | NavigateBack |

### Règles de Validation

| Step | Champ | Règle |
|------|--------|--------|
| **1** | title | Non-empty, trimmed (required) |
| **1** | description | Non-empty, trimmed (required) |
| **1** | eventType | Valid enum or CUSTOM (optional) |
| **2** | minParticipants | Positive integer (optional) |
| **2** | maxParticipants | Positive integer (optional) |
| **2** | constraint | max >= min (if both provided) |
| **3** | locations | Can be empty (optional) |
| **4** | timeSlots | At least 1 required |

### Side Effects de Navigation

- `NavigateTo("event-detail/{id}")` - Navigation après création
- `NavigateBack` - Retour ou annulation
- `ShowToast` - Feedback utilisateur (succès)
- `ShowError` - Feedback utilisateur (erreur)

---

## 3️⃣ Dépréciation EventCreationScreen

### Pourquoi Migrer ?

**Limitations d'EventCreationScreen** :
1. **Single-step form** → Forme overwhelming
2. **No auto-save** → Risque de perte de données
3. **Limited validation** → Pas de feedback temps réel
4. **No progress** → Utilisateur ne sait pas où il en est

**Bénéfices de DraftEventWizard** :
1. ✅ Multi-step wizard (4 étapes) → UX progressive
2. ✅ Auto-save → Pas de perte de données
3. ✅ Real-time validation → Feedback immédiat
4. ✅ Progress indicator → Statut clair
5. ✅ Better accessibility → TalkBack/VoiceOver amélioré
6. ✅ Editing support → Réutilisable pour édition

### Timeline de Migration

| Version | Statut |
|---------|--------|
| **v1.5.0** (actuelle) | `@Deprecated` ajouté |
| **v1.6.0** (next minor) | Warning logged quand utilisé |
| **v2.0.0** (next major) | `EventCreationScreen` supprimé |

### Comment Migrer ?

**Voir guide complet** : `docs/migration/EVENTCREATIONSCREEN_TO_DRAFTEVENTWIZARD.md`

**Exemple rapide** :

```kotlin
// BEFORE (deprecated)
EventCreationScreen(
    userId = userId,
    onEventCreated = { event -> ... },
    onBack = { ... }
)

// AFTER (recommended)
DraftEventWizard(
    initialEvent = null,
    onSaveStep = { event ->
        viewModel.dispatch(Intent.UpdateDraftEvent(event))
    },
    onComplete = { event ->
        viewModel.dispatch(Intent.CreateEvent(event))
    },
    onCancel = {
        navController.popBackStack()
    }
)
```

---

## 4️⃣ Documentation et Tests

### Nouveaux Guides Créés

#### 1. DRAFT_WIZARD_USAGE.md (450+ lignes)

**Contenu** :
- Quick start pour Android & iOS
- Étapes du wizard avec règles de validation
- Personnalisation et callbacks
- Meilleures pratiques et troubleshooting

**Où** : `docs/guides/DRAFT_WIZARD_USAGE.md`

#### 2. STATE_MACHINE_INTEGRATION_GUIDE.md (500+ lignes)

**Contenu** :
- Architecture MVI + FSM
- EventManagementStateMachine documentation
- Intents et side effects
- Patterns d'intégration (ViewModel, Composable)
- Tests unitaires et d'intégration

**Où** : `docs/guides/STATE_MACHINE_INTEGRATION_GUIDE.md`

#### 3. EVENTCREATIONSCREEN_TO_DRAFTEVENTWIZARD.md (350+ lignes)

**Contenu** :
- Pourquoi migrer
- Bénéfices de DraftEventWizard
- Timeline de migration
- Migration step-by-step
- Avant/Après code
- Checklist et troubleshooting

**Où** : `docs/migration/EVENTCREATIONSCREEN_TO_DRAFTEVENTWIZARD.md`

### Spécification Workflow

**workflow-coordination/spec.md** (600+ lignes)
- 5 requirements ADDED (workflow-draft-001 à -005)
- Diagrammes (séquence, flux, états)
- Validation rules
- Side effects
- Tests requirements

### Diagrammes Visuels

**DIAGRAMS.md** (200+ lignes)
- Diagramme de séquence création DRAFT
- Diagramme de séquence navigation inter-étapes
- Diagramme d'états workflow DRAFT
- Diagramme de flux utilisateur
- Diagramme de flux d'erreur

### Tests d'Intégration

**DraftWorkflowIntegrationTest.kt** (8 tests, 100% passants)

| Test | Description |
|------|-------------|
| Complete draft wizard flow | Steps 1-4 → Create Event |
| Auto-save after each step | Persistence verification |
| Validation blocks invalid | Empty title, invalid counts |
| Minimal event creation | Title + description + créneaux |
| Full event creation | All optional fields filled |
| Event recoverable | Reprise après interruption |
| Add/Remove locations | Location management |
| Multiple TimeSlots | Different timeOfDay values |

### Accessibilité

✅ **Validé** :
- TalkBack (Android) - Labels correctes, feedback vocal
- VoiceOver (iOS) - Labels correctes, feedback vocal

---

## 5️⃣ Questions et Discussion

### Questions Clés

1. **Q**: Quand migrer EventCreationScreen dans le code de production ?
   **R**: Migration planifiée pour v2.0.0. Les 2 utilisations actuelles (WakevNavHost.kt, App.kt) peuvent être migrées graduellement.

2. **Q**: Est-ce que les tests couvrent tous les scénarios ?
   **R**: 8 tests d'intégration couvrent le workflow complet. Tests supplémentaires peuvent être ajoutés selon les besoins.

3. **Q**: Comment contribuer aux guides de documentation ?
   **R**: Les guides sont dans `docs/guides/` et `docs/migration/`. Pull requests welcome pour améliorer les exemples et le troubleshooting.

4. **Q**: Quels sont les prochains changements OpenSpec planifiés ?
   **R**: Voir `openspec list` pour les changements actifs. Le prochain pourrait concerner les agents (Suggestions, Transport, etc.).

---

## 📚 Références et Ressources

### Documentation

- 📄 [DRAFT_WIZARD_USAGE.md](docs/guides/DRAFT_WIZARD_USAGE.md)
- 📄 [STATE_MACHINE_INTEGRATION_GUIDE.md](docs/guides/STATE_MACHINE_INTEGRATION_GUIDE.md)
- 📄 [EVENTCREATIONSCREEN_TO_DRAFTEVENTWIZARD.md](docs/migration/EVENTCREATIONSCREEN_TO_DRAFTEVENTWIZARD.md)
- 📄 [workflow-coordination/spec.md](openspec/specs/workflow-coordination/spec.md)
- 📄 [DIAGRAMS.md](openspec/changes/align-draft-workflow/DIAGRAMS.md)

### Fichiers Clés

- `shared/src/commonMain/kotlin/com/guyghost/wakeve/presentation/statemachine/EventManagementStateMachine.kt`
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/presentation/state/EventManagementContract.kt`
- `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/event/DraftEventWizard.kt`
- `iosApp/iosApp/Views/DraftEventWizardView.swift`

### Outils OpenSpec

```bash
# Voir le changement
openspec show align-draft-workflow

# Voir les spécifications
openspec spec list --long

# Valider le changement
openspec validate align-draft-workflow --strict

# Archiver le changement (après approbation)
openspec archive align-draft-workflow --yes
```

---

## ✅ Checklist Post-Présentation

- [ ] Questions répondues
- [ ] Documentation partagée (GitHub, Notion, etc.)
- [ ] Prochaines étapes définies
- [ ] Feedback collecté
- [ ] Approbation pour archivage

---

**Merci pour votre attention !** 🙏

**Questions ?** → GitHub Issues ou Discussion Board

---

*Présentation créée par OpenSpec Orchestrator - 2026-01-04*
