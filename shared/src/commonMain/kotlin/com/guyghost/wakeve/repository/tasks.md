# UserPreferencesRepository Implementation - Tasks

## Overview
Implémentation du `UserPreferencesRepository` pour collecter l'historique de vote anonymisé et apprendre des préférences utilisateur pour le système de recommandations IA.

## Fichiers Créés/Modifiés

### 1. Modèle de données
- **Fichier**: `shared/src/commonMain/kotlin/com/guyghost/wakeve/models/UserPreferenceModels.kt`
- **Contenu**:
  - `UserPreference` - Modèle principal des préférences utilisateur
  - `ScoreWeights` - Configuration des poids pour le scoring
  - `DayOfWeek` - Enum pour les jours de la semaine
  - `VoteType` - Enum pour les types de vote (YES, MAYBE, NO)
  - `InteractionType` - Enum pour les types d'interactions
  - `PreferenceInteraction` - Modèle d'interaction pour l'historique

### 2. Schéma SQLDelight
- **Fichier**: `shared/src/commonMain/sqldelight/com/guyghost/wakeve/UserPreferences.sq`
- **Tables**:
  - `user_preferences` - Stocke les préférences calculées
  - `preference_interaction` - Stocke l'historique des interactions
- **Queries**: CRUD pour les deux tables

### 3. Repository
- **Fichier**: `shared/src/commonMain/kotlin/com/guyghost/wakeve/repository/UserPreferencesRepository.kt`
- **Fonctions**:
  - `getUserPreferences(userId)` - Récupère les préférences d'un utilisateur
  - `updateUserPreferences(userId, preferences)` - Met à jour les préférences
  - `recordVote(userId, eventId, voteType, timeOfDay, dayOfWeek)` - Enregistre un vote
  - `recordEventCreation(userId, eventId, eventType)` - Enregistre la création d'un événement
  - `recordEventParticipation(userId, eventId, location)` - Enregistre la participation
  - `calculateImplicitPreferences(userId, decayDays)` - Calcule les préférences implicites avec decay
  - `applyDecay(userId, decayDays)` - Supprime les anciennes interactions
  - `getInteractionHistory(userId)` - Récupère l'historique complet
  - `deleteUserData(userId)` - Supprime toutes les données (RGPD)
  - `mergeWithExplicit(userId, explicitPreferences)` - Fusionne préférences implicites et explicites

### 4. Tests unitaires
- **Fichier**: `shared/src/commonTest/kotlin/com/guyghost/wakeve/repository/UserPreferencesRepositoryTest.kt`
- **Tests** (14 tests):
  1. `getUserPreferences returns null when no preferences exist`
  2. `getUserPreferences returns preferences when found`
  3. `updateUserPreferences inserts new preferences`
  4. `recordVote creates interaction record`
  5. `recordEventCreation creates interaction with event type`
  6. `calculateImplicitPreferences returns empty for no interactions`
  7. `calculateImplicitPreferences learns from YES votes`
  8. `calculateImplicitPreferences learns from event creation`
  9. `calculateImplicitPreferences learns from participation with location`
  10. `applyDecay deletes old interactions`
  11. `UserPreference empty factory creates empty preferences`
  12. `ScoreWeights isValid returns true for default weights`
  13. `ScoreWeights isValid returns false for invalid weights`
  14. `getInteractionHistory returns mapped interactions`
  15. `deleteUserData removes all user data`
  16. `mergeWithExplicit prefers explicit over implicit`
  17. `NO votes decrease preference score`
  18. `multiple YES votes for same day increase preference score`

## Business Rules Implémentées

### 1. Implicit Preferences
- Analyse de l'historique des votes (30 derniers jours par défaut)
- Détection des patterns: jours préférés, heures préférées, types d'événements
- Application du decay exponentiel: `weight = exp(-days / decayConstant)`

### 2. Explicit Preferences
- Les préférences explicites ont priorité sur les implicites
- Stockage persistant via SQLDelight

### 3. Decay Exponentiel
- Constante de decay configurable (défaut: 30 jours)
- Aujourd'hui: weight = 1.0
- 30 jours: weight = 0.37
- 60 jours: weight = 0.14
- 90 jours: weight = 0.05

### 4. Catégories de Préférences
- `preferredDays`: Jours de la semaine préférés
- `preferredTimeOfDay`: Moments de la journée préférés (MORNING, AFTERNOON, EVENING)
- `preferredEventTypes`: Types d'événements préférés
- `preferredLocations`: Lieux préférés

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                   UserPreferencesRepository                  │
│  (shared/src/commonMain/kotlin/com/guyghost/wakeve/repository)
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌───────────────────┐    ┌───────────────────────────────┐ │
│  │ user_preferences  │    │   preference_interaction      │ │
│  │     (SQLDelight)  │    │        (SQLDelight)           │ │
│  └───────────────────┘    └───────────────────────────────┘ │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## Points d'intégration

### Avec le système de vote existant
- Appel de `recordVote()` après chaque vote utilisateur
- Extraction automatique du jour et de l'heure du créneau

### Avec le système d'événements
- Appel de `recordEventCreation()` lors de la création d'un événement
- Appel de `recordEventParticipation()` lors de la participation

### Avec le système de recommandations
- Utilisation de `calculateImplicitPreferences()` pour générer des recommandations personnalisées
- Utilisation de `mergeWithExplicit()` pour combiner préférences implicites et explicites

## Checklist

- [x] Créer le modèle UserPreference avec ScoreWeights
- [x] Créer l'enum DayOfWeek
- [x] Créer l'enum VoteType
- [x] Créer l'enum InteractionType
- [x] Mettre à jour le schéma SQLDelight UserPreferences.sq
- [x] Implémenter UserPreferencesRepository
- [x] Implémenter la logique de calcul des préférences implicites
- [x] Implémenter le decay exponentiel
- [x] Créer les tests unitaires (18 tests)
- [x] Vérifier la compilation

## Notes

- Le decay des interactions старше 3x la période de decay pour conserver l'historique
- Les votes NO ont un poids négatif (-0.3) pour éviter ces créneaux
- Les votes MAYBE ont un poids réduit (0.5)
- Les préférences vides sont remplies par les valeurs implicites si disponibles
- La suppression de données utilisateur est conforme au RGPD (droit à l'oubli)
