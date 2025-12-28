# Tasks - Cleanup and Complete Calendar Management

## Phase 1: Analyse et Nettoyage du Code (shared)

### 1.1 Analyser les usages de l'ancien CalendarService
- [ ] Rechercher tous les imports de `com.guyghost.wakeve.CalendarService`
- [ ] Rechercher tous les usages de l'interface `CalendarService`
- [ ] Documenter les fichiers qui utilisent l'ancien service
- [ ] Identifier les breaking changes potentiels

### 1.2 Migrer vers le nouveau CalendarService
- [ ] Mettre à jour tous les usages de l'ancien `CalendarService` vers `com.guyghost.wakeve.calendar.CalendarService`
- [ ] Remacer les appels à `addEventToCalendar` par `addToNativeCalendar`
- [ ] Remplacer les appels à `generateICSInvite` par `generateICSInvitation`
- [ ] Mettre à jour les imports dans tous les fichiers affectés

### 1.3 Nettoyer les modèles
- [ ] Supprimer l'ancien modèle `CalendarEvent` (si non utilisé)
- [ ] Vérifier que `EnhancedCalendarEvent` est utilisé partout
- [ ] Supprimer l'ancienne interface `CalendarService` de `models/CalendarModels.kt`
- [ ] Conserver uniquement `EnhancedCalendarService` interface

### 1.4 Supprimer l'ancien fichier CalendarService.kt
- [ ] Supprimer `shared/src/commonMain/kotlin/com/guyghost/wakeve/CalendarService.kt` (ancien)
- [ ] Conserver uniquement `shared/src/commonMain/kotlin/com/guyghost/wakeve/calendar/CalendarService.kt`
- [ ] Vérifier que le code compile sans erreurs

### 1.5 Vérifier les implémentations platform
- [ ] Vérifier `PlatformCalendarServiceImpl` Android pour `PlatformCalendarService`
- [ ] Vérifier `PlatformCalendarServiceImpl` iOS pour `PlatformCalendarService`
- [ ] Vérifier implémentations JVM et JS
- [ ] S'assurer que toutes les méthodes de l'interface sont implémentées

## Phase 2: Tests (shared)

### 2.1 Activer les tests
- [ ] Renommer `CalendarServiceTest.kt.disabled` → `CalendarServiceTest.kt`
- [ ] Corriger l'import du service (nouveau emplacement)

### 2.2 Compléter les tests existants
- [ ] Vérifier que `generateICSInviteCreatesValidICS` fonctionne
- [ ] Vérifier que `generateICSInviteIncludesCorrectTimestamps` fonctionne
- [ ] Vérifier que `generateICSInviteHasUniqueUID` fonctionne
- [ ] Mettre à jour pour utiliser `generateICSInvitation` au lieu de `generateICSInvite`

### 2.3 Ajouter les tests manquants (selon la spec)
- [ ] Test: `generate ICS document with all event details`
- [ ] Test: `ICS document includes correct timezone`
- [ ] Test: `Add to native calendar requires permission on Android`
- [ ] Test: `Update event updates existing calendar entry`
- [ ] Test: `Delete event removes from native calendar`
- [ ] Test: `Meeting reminders are scheduled correctly`

### 2.4 Créer des tests pour PlatformCalendarService
- [ ] Test: `PlatformCalendarService Android addEvent`
- [ ] Test: `PlatformCalendarService Android updateEvent`
- [ ] Test: `PlatformCalendarService Android deleteEvent`
- [ ] Test: `PlatformCalendarService iOS addEvent`
- [ ] Test: `PlatformCalendarService iOS updateEvent`
- [ ] Test: `PlatformCalendarService iOS deleteEvent`

### 2.5 Exécuter les tests
- [ ] Exécuter tous les tests calendrier: `./gradlew shared:test --tests "*CalendarServiceTest"`
- [ ] Corriger les erreurs éventuelles
- [ ] Viser 100% passant (au moins 10 tests)

## Phase 3: API Endpoints (server)

### 3.1 Créer les modèles DTOs
- [x] Créer `ICSInvitationRequest.kt`
- [x] Créer `ICSInvitationResponse.kt`
- [x] Créer `NativeCalendarRequest.kt`
- [x] Créer `CalendarReminderRequest.kt`

### 3.2 Créer CalendarRoutes.kt
- [x] Créer le fichier `server/src/main/kotlin/com/guyghost/wakeve/routing/CalendarRoutes.kt`
- [x] Implémenter `POST /api/events/{id}/calendar/ics` - Générer invitation ICS
- [x] Implémenter `GET /api/events/{id}/calendar/ics` - Télécharger fichier ICS
- [x] Implémenter `POST /api/events/{id}/calendar/native` - Ajouter au calendrier natif
- [x] Implémenter `PUT /api/events/{id}/calendar/native/{participantId}` - Mettre à jour calendrier
- [x] Implémenter `DELETE /api/events/{id}/calendar/native/{participantId}` - Supprimer du calendrier
- [x] Implémenter `POST /api/events/{id}/calendar/reminders/{timing}` - Envoyer rappels (TODO + notification)

### 3.3 Intégrer les routes dans Application.kt
- [x] Importer `calendarRoutes`
- [x] Enregistrer les routes dans le routing principal
- [x] Vérifier que les routes sont accessibles

### 3.4 Tests des API endpoints
- [x] Test: `POST /api/events/{id}/calendar/ics` génère ICS valide
- [x] Test: `GET /api/events/{id}/calendar/ics` télécharge fichier ICS
- [x] Test: `POST /api/events/{id}/calendar/native` ajoute au calendrier
- [x] Test: `PUT /api/events/{id}/calendar/native/{participantId}` met à jour
- [x] Test: `DELETE /api/events/{id}/calendar/native/{participantId}` supprime

## Phase 4: UI Android

### 4.1 Créer le composant CalendarScreen.kt
- [x] Créer `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/CalendarIntegrationCard.kt` (Note: We created this instead of CalendarScreen.kt for better integration)
- [ ] Afficher les détails de l'événement calendrier
- [ ] Bouton "Ajouter au calendrier natif"
- [ ] Bouton "Télécharger invitation ICS"
- [ ] Bouton "Supprimer du calendrier" (si déjà ajouté)
- [ ] Appliquer le design system Material You

### 4.2 Créer le composant AddToCalendarButton.kt
- [x] Créer `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/components/AddToCalendarButton.kt`
- [ ] Bouton stylisé avec icône calendrier
- [ ] État de chargement pendant l'ajout
- [ ] Messages de succès/erreur
- [ ] Material You styling

### 4.3 Implémenter la logique d'ajout au calendrier
- [ ] Vérifier les permissions `WRITE_CALENDAR`
- [ ] Demander les permissions si non accordées
- [ ] Appeler `CalendarService.addToNativeCalendar`
- [ ] Gérer les erreurs de permission refusée
- [ ] Afficher un message explicatif sur le fallback ICS

### 4.4 Créer le composant DownloadICSButton.kt
- [ ] Créer bouton pour télécharger l'ICS
- [ ] Appeler `CalendarService.generateICSInvitation`
- [ ] Sauvegarder le fichier ICS sur l'appareil
- [ ] Intégration avec le système de partage Android

### 4.5 Intégrer avec EventDetailsScreen
- [x] Ajouter le bouton "Calendrier" dans EventDetailsScreen (Added to ModernEventDetailView.kt)
- [x] Navigation vers CalendarScreen (replaced by in-place CalendarIntegrationCard)
- [x] Passer eventId et participantId en paramètres


**Note:** We implemented CalendarIntegrationCard directly inside EventDetailsScreen (ModernEventDetailView) instead of a separate CalendarScreen for better UX. The UI exposes callbacks `onAddToCalendar` and `onShareInvite` but the platform wiring (CalendarService calls) is not implemented yet — that will be done in a next step (App wiring / DI).

### 4.6 Tests UI Android
- [ ] Test de navigation vers CalendarScreen
- [ ] Test du bouton Ajouter au calendrier
- [ ] Test de la demande de permission
- [ ] Test du bouton Télécharger ICS
- [ ] Test des messages d'erreur

## Phase 5: UI iOS

### 5.1 Créer CalendarView.swift
- [ ] Créer `iosApp/iosApp/Views/CalendarView.swift`
- [ ] Afficher les détails de l'événement calendrier
- [ ] Bouton "Ajouter au calendrier"
- [ ] Bouton "Télécharger invitation ICS"
- [ ] Bouton "Supprimer du calendrier"
- [ ] Appliquer le design system Liquid Glass

### 5.2 Créer AddToCalendarButton.swift
- [ ] Créer `iosApp/iosApp/Components/AddToCalendarButton.swift`
- [ ] Bouton stylisé avec SF Symbol `calendar.badge.plus`
- [ ] État de chargement pendant l'ajout
- [ ] Alertes pour succès/erreur
- [ ] Liquid Glass styling avec `.glassCard()`

### 5.3 Implémenter la logique d'ajout au calendrier
- [ ] Appeler `CalendarService.addToNativeCalendar`
- [ ] Gérer les erreurs EventKit
- [ ] Afficher une alerte en cas de succès/erreur
- [ ] Stocker l'ID de l'événement calendrier créé

### 5.4 Créer DownloadICSButton.swift
- [ ] Créer bouton pour télécharger l'ICS
- [ ] Appeler `CalendarService.generateICSInvitation`
- [ ] Sauvegarder le fichier ICS via UIDocumentPicker
- [ ] Intégration avec le partage iOS (ShareSheet)

### 5.5 Intégrer avec EventDetailView
- [ ] Ajouter le bouton "Calendrier" dans EventDetailView
- [ ] Navigation vers CalendarView
- [ ] Passer eventId et participantId en paramètres

### 5.6 Tests UI iOS
- [ ] Test de navigation vers CalendarView
- [ ] Test du bouton Ajouter au calendrier
- [ ] Test de l'intégration EventKit
- [ ] Test du bouton Télécharger ICS
- [ ] Test des alertes et messages

## Phase 6: Documentation

### 6.1 Mettre à jour la spec
- [ ] Mettre à jour `openspec/specs/calendar-management/spec.md`
- [ ] Changer le statut de "✅ Implémenté" à "🚧 En cours"
- [ ] Mettre à jour les sections "Implementation Notes" avec les nouveaux endpoints
- [ ] Ajouter les références aux UI Android et iOS
- [ ] Documenter la nouvelle architecture sans duplication

### 6.2 Mettre à jour AGENTS.md
- [ ] Ajouter/Mettre à jour la section "Agent Calendrier"
- [ ] Documenter les responsabilités complètes
- [ ] Mettre à jour les interactions avec les autres agents
- [ ] Ajouter les références aux fichiers implémentés

### 6.3 Créer une documentation utilisateur
- [ ] Créer `docs/CALENDAR_GUIDE.md`
- [ ] Comment ajouter un événement au calendrier (Android)
- [ ] Comment ajouter un événement au calendrier (iOS)
- [ ] Comment télécharger une invitation ICS
- [ ] Comment gérer les permissions sur Android
- [ ] FAQ sur les problèmes courants

### 6.4 Mettre à jour README.md
- [ ] Ajouter une section sur les fonctionnalités calendrier
- [ ] Lier vers `CALENDAR_GUIDE.md`
- [ ] Mettre à jour les statistiques du projet

### 6.5 Créer un résumé de l'implémentation
- [ ] Créer `CALENDAR_IMPLEMENTATION_SUMMARY.md`
- [ ] Architecture finale
- [ ] Fichiers modifiés/créés
- [ ] API endpoints documentés
- [ ] Tests coverage report

## Phase 7: Validation Finale

### 7.1 Validation technique
- [ ] Tous les tests passent: `./gradlew shared:test` (100%)
- [ ] Build Android réussit: `./gradlew composeApp:assembleDebug`
- [ ] Build iOS réussit: Test dans Xcode
- [ ] Serveur compile et exécute sans erreurs
- [ ] Zero warnings de compilation

### 7.2 Validation fonctionnelle
- [ ] Tests manuels sur Android (ajouter au calendrier)
- [ ] Tests manuels sur iOS (ajouter au calendrier)
- [ ] Tests manuels du téléchargement ICS
- [ ] Tests des API endpoints avec Postman/curl
- [ ] Vérification des permissions Android

### 7.3 Review
- [ ] Review de code par @review
- [ ] Validation de la conformité design system
- [ ] Validation de l'accessibilité
- [ ] Validation de la documentation

### 7.4 Archivage
- [ ] Mettre à jour tasks.md avec toutes les tâches complétées
- [ ] Vérifier que tous les critères de succès sont remplis
- [ ] Archiver le changement: `openspec archive cleanup-complete-calendar-management --yes`

## Résumé

**Total des tâches:** 88
**Phases:** 7
**Priorité:** Haute
**Estimation:** 3-4 jours

### Tâches par phase

| Phase | Tâches | Description |
|-------|--------|-------------|
| 1 | 5 | Nettoyage du code (shared) |
| 2 | 10 | Tests (shared) |
| 3 | 9 | API endpoints (server) |
| 4 | 6 | UI Android |
| 5 | 6 | UI iOS |
| 6 | 5 | Documentation |
| 7 | 4 | Validation finale |

---

**Notes importantes:**
- Les tâches marquées avec [ ] sont à faire
- Mettre à jour le statut en [x] une fois complétées
- Les tests doivent être écrits AVANT l'implémentation (TDD)
- Toujours demander @review avant de considérer une phase terminée
