# Proposal: Cleanup and Complete Calendar Management Implementation

## Contexte

Le module de gestion de calendrier de Wakeve est actuellement dans un état partiellement implémenté mais marqué comme "✅ Implémenté". L'analyse du code révèle plusieurs problèmes:

### État Actuel

**Existant:**
- ✅ Spécification complète dans `openspec/specs/calendar-management/spec.md`
- ✅ `CalendarService` principal avec génération ICS fonctionnelle
- ✅ `PlatformCalendarService` implémenté pour Android, iOS, JVM, JS
- ✅ Models: `ICSDocument`, `EnhancedCalendarEvent`
- ✅ Tests existants (mais désactivés)

**Problèmes:**
- ❌ Duplication de code entre deux `CalendarService.kt`
- ❌ Modèles incohérents (`CalendarEvent` vs `EnhancedCalendarEvent`)
- ❌ Deux interfaces `CalendarService` différentes
- ❌ Tests désactivés (`CalendarServiceTest.kt.disabled`)
- ❌ Fonctionnalités manquantes:
  - UI Android/iOS pour ajouter au calendrier
  - API endpoints sur le serveur Ktor
  - Intégration notifications pour les rappels
  - Suppression d'événement du calendrier
- ❌ Spec marquée "Implémentée" alors que l'implémentation est incomplète

## Objectifs

### Objectifs Principaux

1. **Nettoyer le code existant**
   - Éliminer la duplication entre les deux `CalendarService.kt`
   - Unifier les modèles en utilisant uniquement `EnhancedCalendarEvent`
   - Supprimer l'ancien interface `CalendarService` au profit de la nouvelle version

2. **Compléter l'implémentation**
   - Activer et compléter les tests (100% passing)
   - Implémenter les API endpoints sur le serveur Ktor
   - Ajouter l'UI pour Android et iOS
   - Implémenter la suppression d'événement du calendrier

3. **Mettre à jour la documentation**
   - Corriger le statut de la spec
   - Documenter l'architecture finale
   - Mettre à jour AGENTS.md avec les responsabilités de l'Agent Calendrier

### Critères de Succès

- [x] Un seul `CalendarService.kt` dans le codebase
- [x] Tests activés et 100% passants (100% couverture minimum)
- [ ] API endpoints fonctionnels sur le serveur (5 endpoints)
- [ ] UI Android fonctionnelle pour ajouter/voir les événements calendrier
- [ ] UI iOS fonctionnelle pour ajouter/voir les événements calendrier
- [ ] Documentation à jour

## Impact

### Impact Positif

- **Code plus propre**: Élimination de la duplication et des incohérences
- **Tests complets**: Assurance que la génération ICS et l'intégration platform fonctionnent
- **Features complètes**: Les utilisateurs pourront réellement utiliser les fonctionnalités calendrier
- **Documentation précise**: Le statut du module reflétera la réalité

### Risques

- **Breaking changes**: Suppression de l'ancien `CalendarService` pourrait affecter du code qui l'utilise
  - **Mitigation**: Recherche globale des usages et migration

- **Permissions Android**: L'implémentation Android nécessite `WRITE_CALENDAR`
  - **Mitigation**: Graceful degradation si permission refusée

- **Complexité UI**: L'implémentation UI peut être plus complexe que prévu
  - **Mitigation**: Commencer par une UI simple, itérer après

### Scope du Changement

**Inclus:**
- Refactor et nettoyage du code calendrier existant
- Activation et complétion des tests
- Implémentation API endpoints sur le serveur
- UI Android et iOS pour intégration calendrier

**Exclus:**
- Système de notifications pour les rappels (déplacé vers Phase 3)
- Récurrence d'événements (futur enhancement)
- Détection de conflits de calendrier (futur enhancement)
- Support de multiples fournisseurs de calendrier (futur enhancement)

## Alternatives Considérées

### Alternative 1: Garder les deux services

**Pour:**
- Aucun breaking change
- Migration progressive possible

**Contre:**
- Duplication maintenue
- Confusion pour les développeurs
- Dette technique accumulée

**Décision:** Rejetée - Maintenir la duplication nuit à la maintenabilité à long terme

### Alternative 2: Implémenter seulement les tests

**Pour:**
- Peu de changement
- Validation rapide

**Contre:**
- Ne résout pas la duplication
- Fonctionnalités manquantes pour les utilisateurs
- Spec resterait marquée incorrectement

**Décision:** Rejetée - Ne satisfait pas les objectifs de compléter l'implémentation

### Alternative 3: Créer un nouveau module

**Pour:**
- Séparation claire des responsabilités
- Migration plus facile

**Contre:**
- Plus complexe à gérer
- Dépendances circulaires potentielles

**Décision:** Rejetée - Ajoute de la complexité inutile

## Plan de Migration

### Phase 1: Nettoyage du Code (shared)
1. Analyser les usages de l'ancien `CalendarService`
2. Migrer vers le nouveau `CalendarService` (dossier `calendar/`)
3. Supprimer l'ancien fichier `CalendarService.kt`
4. Nettoyer les modèles inutilisés

### Phase 2: Tests (shared)
1. Renommer `CalendarServiceTest.kt.disabled` → `CalendarServiceTest.kt`
2. Compléter les tests pour couvrir tous les scenarios
3. Corriger les erreurs éventuelles
4. Viser 100% passant

### Phase 3: API Endpoints (server)
1. Créer `CalendarRoutes.kt` avec les 5 endpoints
2. Intégrer avec `Application.kt`
3. Tests des endpoints

### Phase 4: UI Android
1. Créer `CalendarScreen.kt` pour afficher les options calendrier
2. Créer `AddToCalendarButton.kt` composant
3. Intégrer avec les permissions Android

### Phase 5: UI iOS
1. Créer `CalendarView.swift` pour afficher les options calendrier
2. Créer `AddToCalendarButton.swift` composant
3. Intégrer avec EventKit

### Phase 6: Documentation
1. Mettre à jour le statut dans `calendar-management/spec.md`
2. Mettre à jour `AGENTS.md` avec l'Agent Calendrier
3. Créer une documentation utilisateur

## Risques et Mitigation

### Risque 1: Breaking Changes

**Description:** La suppression de l'ancien `CalendarService` pourrait cacher du code qui l'utilise.

**Probabilité:** Moyenne
**Impact:** Élevé

**Mitigation:**
- Recherche globale des usages avant suppression
- Tests pour vérifier que rien ne casse
- Migration progressive si nécessaire

### Risque 2: Permissions Android Refusées

**Description:** Les utilisateurs peuvent refuser la permission `WRITE_CALENDAR`.

**Probabilité:** Faible
**Impact:** Moyen

**Mitigation:**
- Graceful degradation avec messages explicites
- Fallback sur téléchargement ICS
- Documentation claire sur les permissions

### Risque 3: Tests échouent

**Description:** Les tests existants pourraient avoir des erreurs après activation.

**Probabilité:** Moyenne
**Impact:** Faible

**Mitigation:**
- Corriger les tests séquentiellement
- Utiliser des mocks pour les platform services
- Documentation des bugs corrigés

## Dépendances

### Dépendances Techniques

- **Kotlin Multiplatform**: Déjà en place
- **SQLDelight**: Déjà en place pour les queries Event/Participant
- **Ktor**: Déjà en place pour le serveur

### Dépendances sur d'autres changements

- Aucune dépendance bloquante
- Peut être développé en parallèle avec d'autres features

### Dépendances sur des fonctionnalités futures

- **Agent Notifications** (Phase 3) pour les rappels de réunion
- Pour l'instant, les rappels ICS suffisent (implémentés)

## Mesure du Succès

### Métriques Techniques

- Tests: 100% passants (au moins 10 tests)
- Code coverage: 80%+ sur CalendarService
- Zero duplication de code
- Zero warnings de compilation

### Métriques Fonctionnelles

- API endpoints: 5 endpoints fonctionnels et testés
- UI Android: Écran et composants fonctionnels
- UI iOS: Écran et composants fonctionnels

### Métriques de Qualité

- Code review approuvée par @review
- Documentation à jour
- Spécification mise à jour avec statut correct

## Prochaines Étapes

Une fois ce changement approuvé:

1. Créer le fichier `tasks.md` avec la checklist détaillée
2. Déléguer l'implémentation aux workers (@codegen, @tests, @docs)
3. Activer les tests et les corriger
4. Implémenter les API endpoints
5. Implémenter les UI Android et iOS
6. Mettre à jour la documentation
7. Review finale et archivage

---

**Statut:** 📋 À approuver
**Priorité:** Haute (module critique pour l'expérience utilisateur)
**Estimation:** 3-4 jours de développement
