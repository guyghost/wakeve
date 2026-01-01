# Proposal: Enhanced DRAFT Phase for Event Creation

## Context

Actuellement, la phase DRAFT d'un événement est minimaliste : un titre, une description, et des créneaux horaires. Pour offrir une meilleure expérience utilisateur et préparer les phases suivantes (suggestions, scénarios, transport), nous devons enrichir cette phase initiale avec plus d'informations structurées.

Les utilisateurs ont besoin de :
- **Catégoriser leur événement** pour recevoir des suggestions pertinentes (ex: mariage → traiteur, photographe)
- **Estimer le nombre de participants** pour optimiser la recherche de logements, transports et restaurants
- **Proposer des lieux potentiels** pour permettre aux participants de voter non seulement sur les dates, mais aussi sur les destinations
- **Spécifier des créneaux horaires optionnels** (matin, après-midi, soirée) pour affiner les disponibilités

## Why

La phase actuelle de création d'événement (DRAFT) est trop simpliste pour permettre une planification efficace. Les organisateurs ne peuvent pas structurer suffisamment leur événement, ce qui limite les capacités futures de l'application (suggestions personnalisées, planification de transport, etc.).

L'ajout de ces champs structurés dès la phase DRAFT va:
- Améliorer l'expérience utilisateur avec un wizard guidé
- Donner aux agents (Suggestions, Transport, Destination) les données nécessaires
- Permettre une planification plus réaliste dès le début
- Réduire la charge cognitive avec une approche progressive

## What Changes

Ce changement ajoute 4 nouvelles capacités à la phase DRAFT:

1. **Classification d'événements** : Organiseurs peuvent catégoriser leur événement avec des presets (BIRTHDAY, WEDDING, etc.) ou un type personnalisé
2. **Estimation de participants** : Organiseurs peuvent fournir min/max/expected participants pour dimensionner la planification
3. **Lieux potentiels** : Organiseurs peuvent proposer plusieurs destinations pour discussion future
4. **Créneaux horaires flexibles** : Organiseurs peuvent spécifier des moments de journée (après-midi, matinée) sans heures précises

Tous ces champs sont optionnels et compatibles avec les événements existants (backward compatible).

## Objectives

1. **Ajouter un système de types d'événements** avec catégories prédéfinies et custom
2. **Permettre l'estimation du nombre de participants** (min/max/attendu)
3. **Créer un modèle de lieux potentiels** (ville, région, lieu précis, ou en ligne)
4. **Enrichir les créneaux horaires** avec indication de moment de journée (toute la journée, matin, après-midi, soirée)
5. **Maintenir la rétrocompatibilité** avec les événements existants

## Scope

### In Scope
- Nouveau modèle de données pour EventType (avec preset + custom)
- Ajout de `expectedParticipants`, `minParticipants`, `maxParticipants` à Event
- Nouveau modèle PotentialLocation (ville, région, lieu précis, virtuel)
- Enrichissement de TimeSlot avec `timeOfDay` (ALL_DAY, MORNING, AFTERNOON, EVENING, SPECIFIC)
- Mise à jour du schema SQLDelight
- Migration des données existantes
- UI pour la phase DRAFT (Android Compose + iOS SwiftUI)
- Tests unitaires et d'intégration

### Out of Scope (phases futures)
- Système de suggestions basé sur le type d'événement (Phase 3)
- Vote sur les lieux (Phase 3 - sera géré par ScenarioManagement)
- Intégration avec APIs externes de lieux (Google Places, etc.)
- Calcul automatique des coûts basé sur le nombre de participants

## Impact

### Benefits
- **Meilleure UX** : processus de création guidé et structuré
- **Préparation pour les agents** : données structurées pour Suggestions, Transport, Destination
- **Personnalisation** : recommandations adaptées au type d'événement
- **Planification réaliste** : estimation du nombre de participants dès le début

### Risks
- **Complexité accrue** : plus de champs à remplir en phase DRAFT
- **Migration de données** : risque d'incohérence sur les événements existants
- **Surcharge cognitive** : trop d'options peuvent décourager les utilisateurs

### Mitigation
- Rendre la plupart des champs **optionnels** (sauf titre/description)
- Fournir des **valeurs par défaut intelligentes** (type=OTHER, expectedParticipants=5)
- **Wizard progressif** : étapes de création guidées avec sauvegarde automatique
- **Migration robuste** : tests exhaustifs de migration de schéma

## Timeline

- **Proposition & Specs** : 1 jour (aujourd'hui)
- **Backend & Schema** : 1 jour
- **Business Logic (shared)** : 1 jour
- **UI Android** : 2 jours
- **UI iOS** : 2 jours
- **Tests & Documentation** : 1 jour

**Total** : ~8 jours

## Dependencies

- ✅ Aucune dépendance bloquante
- 🔜 Ce changement **débloque** :
  - Agent Suggestions (utilise EventType)
  - Agent Transport (utilise expectedParticipants + PotentialLocation)
  - Agent Destination (utilise PotentialLocation)
  - ScenarioManagement (vote sur lieux/dates combinés)

## Decision Log

### ADR-001: EventType avec enum + custom text
**Décision** : Fournir une liste d'enum prédéfinis (BIRTHDAY, WEDDING, TEAM_BUILDING, etc.) + option CUSTOM avec champ texte libre.

**Rationale** : 
- Les presets permettent des suggestions automatiques
- Le champ custom permet flexibilité pour cas non couverts
- Pattern standard dans les UIs mobiles (Material/iOS)

**Alternatives rejetées** : 
- Texte libre uniquement → pas de catégorisation automatique
- Enum fixe uniquement → trop rigide, frustrant pour les utilisateurs

### ADR-002: Estimation de participants (min/max/expected) vs nombre fixe
**Décision** : 3 champs optionnels `minParticipants`, `maxParticipants`, `expectedParticipants`.

**Rationale** :
- Reflète la réalité : on ne connaît jamais le nombre exact à l'avance
- Permet aux agents (Transport, Destination) de calculer des fourchettes de prix
- `expected` est la valeur par défaut utilisée si min/max non spécifiés

**Alternatives rejetées** :
- Nombre fixe → irréaliste, changerait constamment
- Uniquement expected → manque de flexibilité pour les scénarios "pire cas"

### ADR-003: PotentialLocation distinct de Scenario.destination
**Décision** : Créer un modèle `PotentialLocation` en phase DRAFT, distinct de `Scenario.destination`.

**Rationale** :
- En DRAFT, l'organisateur **propose** des lieux pour discussion
- En COMPARING, les scénarios **comparent** des destinations précises avec coût/logement
- Séparation des préoccupations : brainstorming vs planification détaillée

**Alternatives rejetées** :
- Réutiliser Scenario.destination → confusion conceptuelle entre proposition et planification

### ADR-004: TimeSlot.timeOfDay vs créneaux horaires stricts
**Décision** : Ajouter un champ `timeOfDay` (ALL_DAY, MORNING, AFTERNOON, EVENING, SPECIFIC) **en plus** de start/end.

**Rationale** :
- Permet flexibilité : "un après-midi début juin" sans heure fixe
- Si `timeOfDay != SPECIFIC`, start/end peuvent être null
- Simplifie la saisie en phase DRAFT (moins de précision = moins de friction)

**Alternatives rejetées** :
- Uniquement start/end stricts → force trop de précision trop tôt
- Uniquement timeOfDay → perd la possibilité de créneaux précis

## Open Questions

1. **Q: Faut-il permettre plusieurs types d'événements simultanés ?**  
   R: **Non** pour simplifier. Un événement = un type. Si besoin de combiner, utiliser CUSTOM.

2. **Q: Doit-on valider que maxParticipants >= minParticipants ?**  
   R: **Oui**, validation côté shared avec feedback UX clair.

3. **Q: Les PotentialLocations sont-elles votables en phase DRAFT ?**  
   R: **Non**. En DRAFT, c'est juste une liste indicative. Le vote se fera en phase COMPARING via Scenarios.

4. **Q: Que se passe-t-il si l'organisateur ne remplit aucun champ optionnel ?**  
   R: **L'événement est créé avec valeurs par défaut** (type=OTHER, expectedParticipants=null). Les agents se comportent de manière dégradée (suggestions génériques).

## Success Criteria

✅ **Doit** :
- [ ] Schema SQLDelight updated avec nouveaux champs (rétrocompatible)
- [ ] Migration de données testée (événements existants → valeurs par défaut)
- [ ] Event model enrichi (EventType, participants counts, PotentialLocations)
- [ ] TimeSlot enrichi avec timeOfDay
- [ ] UI DRAFT Android avec wizard en étapes (Material You)
- [ ] UI DRAFT iOS avec wizard en étapes (Liquid Glass)
- [ ] Tests unitaires (EventRepository, validation)
- [ ] Tests d'intégration (création DRAFT → POLLING)
- [ ] Documentation mise à jour (AGENTS.md, API.md)

✅ **Devrait** :
- [ ] Sauvegarde automatique en brouillon (auto-save)
- [ ] Feedback temps réel (validation des champs)
- [ ] Accessibilité validée (VoiceOver, TalkBack)

🔜 **Pourrait** (phases futures) :
- Suggestions de lieux basées sur géolocalisation
- Import de contacts pour estimation de participants
- Prévisualisation des coûts estimés
