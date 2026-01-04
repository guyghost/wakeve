# Proposal: Align DRAFT Workflow with UI Interfaces

## Context

Actuellement, l'application possède :
- Des **machines à états** (EventManagementStateMachine) qui orchestrent le cycle de vie des événements
- Des **interfaces UI wizard** sur Android (DraftEventWizard.kt) et iOS (DraftEventWizardView.swift) en 4 étapes
- Un **ancien écran Android** (EventCreationScreen.kt) qui utilise un formulaire simple sans wizard

**Problème identifié :**
- Il y a une incohérence entre les interfaces et le workflow orchestré par la state machine
- Le workflow DRAFT n'est pas formellement documenté comme référence unique
- L'ancien EventCreationScreen.kt n'utilise pas le wizard et doit être déprécié/remplacé

## Why

Pour assurer une expérience utilisateur cohérente cross-platform et simplifier la maintenance, nous devons :

1. **Aligner le workflow DRAFT** avec les interfaces UI existantes
2. **Documenter formellement** le workflow DRAFT comme référence unique
3. **Garantir que la state machine** orchestre correctement le flux entre les étapes
4. **Uniformiser les comportements** entre Android et iOS (validation, auto-save, navigation)
5. **Faciliter les futures évolutions** du workflow DRAFT

## What Changes

Ce changement **ne modifie pas le code** mais documente et aligne le workflow DRAFT existant :

1. **Documentation du workflow DRAFT** dans la spécification workflow-coordination
2. **Mapping entre états UI et Intents State Machine**
3. **Dépréciation de EventCreationScreen.kt** (au profit de DraftEventWizard)
4. **Règles de validation et side effects** pour chaque étape
5. **Navigation pattern** orchestré par la state machine

## Objectives

1. **Documenter le workflow DRAFT** avec 4 étapes et leurs règles métier
2. **Mapper chaque étape UI** aux Intents de la state machine
3. **Définir les side effects** de navigation (auto-save, validation)
4. **Spécifier la validation** à chaque étape
5. **Documenter l'intégration** avec les autres phases (POLLING, CONFIRMED)

## Scope

### In Scope
- Documentation du workflow DRAFT (4 étapes)
- Mapping étapes UI ↔ State Machine Intents
- Spécification des validations par étape
- Spécification des side effects (auto-save, navigation)
- Dépréciation de EventCreationScreen.kt
- Tests de workflow d'intégration (DRAFT → POLLING)

### Out of Scope
- Modification du code existant (sauf dépréciation)
- Création de nouvelles fonctionnalités
- Changement du design system

## Impact

### Benefits
- **Référence unique** pour le workflow DRAFT
- **Meilleure compréhension** pour les développeurs
- **Maintenabilité accrue** du code cross-platform
- **Cohérence UI** entre Android et iOS

### Risks
- Risque minimal : ce changement est documentaire
- Si des incohérences sont détectées, elles devront être corrigées

## Timeline

- **Documentation** : 1 jour
- **Tests de workflow** (si nécessaires) : 1 jour
- **Review & validation** : 1 jour

**Total** : ~3 jours

## Dependencies

- ✅ Aucune dépendance bloquante
- 🔑 Utilise les specs existantes :
  - `event-organization` (workflow événement)
  - `workflow-coordination` (coordination cross-state-machine)

## Decision Log

### ADR-001: Workflow DRAFT en 4 étapes
**Décision** : Conserver le workflow DRAFT en 4 étapes existant (Basic Info, Participants, Locations, Time Slots).

**Rationale** :
- Les implémentations Android et iOS utilisent déjà ce workflow
- L'expérience utilisateur est validée (wizard progressif)
- Les machines à états supportent ce workflow via Intents appropriés

### ADR-002: Auto-save à chaque transition d'étape
**Décision** : Auto-sauvegarder l'événement à chaque transition d'étape du wizard.

**Rationale** :
- L'utilisateur peut quitter à tout moment sans perte de données
- Permet de reprendre l'édition plus tard
- Les implémentations Android et iOS le font déjà

### ADR-003: Validation stricte avant navigation
**Décision** : Valider strictement l'étape actuelle avant de permettre la navigation vers l'étape suivante.

**Rationale** :
- Évite de créer des événements invalides
- Guide l'utilisateur avec des erreurs explicites
- Les deux plateformes implémentent déjà cette validation

### ADR-004: State Machine orchestre la navigation
**Décision** : La state machine orchestre la navigation via des side effects (NavigateTo, ShowToast, etc.).

**Rationale** :
- Centralise la logique de navigation
- Assure la cohérence cross-platform
- Pattern MVI déjà utilisé dans le projet

## Open Questions

1. **Q: Faut-il conserver EventCreationScreen.kt pour compatibilité ?**
   R: **Non**, il faut le déprécier et migrer vers DraftEventWizard. Le wizard offre une meilleure UX.

2. **Q: Les étapes doivent-elles être réordonnables ?**
   R: **Non** pour l'instant. L'ordre actuel (Basic Info → Participants → Locations → Time Slots) est logique.

3. **Q: L'utilisateur peut-il sauter des étapes optionnelles ?**
   R: **Oui**, Locations et certains champs de Participants sont optionnels. Le wizard doit le permettre.

4. **Q: Comment gérer la modification d'un événement DRAFT existant ?**
   R: Le wizard doit être réutilisable en mode édition (initialEvent != null). C'est déjà implémenté.

## Success Criteria

✅ **Doit** :
- [x] Spécification du workflow DRAFT documentée dans `workflow-coordination/spec.md`
- [x] Mapping étapes UI ↔ State Machine Intents documenté
- [x] Règles de validation par étape documentées
- [x] Side effects de navigation documentés
- [x] EventCreationScreen.kt marqué comme @Deprecated avec commentaire migratoire
- [x] Tests de workflow d'intégration passants (DRAFT → POLLING)

✅ **Devrait** :
- [x] Diagramme de séquence du workflow DRAFT
- [x] Guide de migration vers DraftEventWizard
- [x] Documentation des edge cases (champs optionnels, valeurs par défaut)

🔜 **Pourrait** (phases futures) :
- [ ] Ajouter une étape de "Résumé" avant création
- [ ] Permettre la réorganisation des étapes
- [ ] Intégration avec AI (remplissage automatique des champs)
