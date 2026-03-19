# Change: Complete Phase 3 - Core Services

## Why

La Phase 3 de Wakeve est partiellement implémentée. Cette change vise à finaliser les fonctionnalités critiques manquantes pour atteindre la complétion de la Phase 3 :

1. **Authentication complète** - Finaliser OAuth (Google/Apple), Email OTP, et mode invité
2. **Notification Service** - Implémenter les notifications push (FCM/APNs)
3. **Collaboration Management** - Ajouter les commentaires et @mentions
4. **Tests E2E** - Valider les workflows complets

## What Changes

### ADDED Capabilities
- **notification-management** - Système de notifications push cross-platform
- **collaboration-management** - Commentaires threadés avec @mentions

### MODIFIED Capabilities
- **user-auth** - Compléter les flows OAuth et Email OTP
- **event-organization** - Intégrer notifications pour les événements clés

### Technical Changes
- Intégration FCM (Android) et APNs (iOS)
- Service de notifications unifié dans shared module
- UI de commentaires sur Android et iOS
- Tests E2E complets pour tous les workflows

## Impact

### Affected Specs
- `user-auth/spec.md` - Compléter les flows manquants
- `event-organization/spec.md` - Ajouter les triggers de notification
- `collaboration-management/spec.md` - Nouvelle spec complète
- `notification-management/spec.md` - Nouvelle spec complète

### Affected Code
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/notification/`
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/collaboration/`
- `wakeveApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/collaboration/`
- `iosApp/iosApp/Views/Collaboration/`
- `server/src/main/kotlin/com/guyghost/wakeve/routes/NotificationRoutes.kt`

### Dependencies
- Firebase Cloud Messaging (Android)
- Apple Push Notification Service (iOS)
- Ktor Server pour le backend de notifications

## Success Criteria

- [ ] Google Sign-In fonctionne end-to-end
- [ ] Apple Sign-In fonctionne end-to-end  
- [ ] Email/OTP fonctionne end-to-end
- [ ] Mode invité fonctionne avec limitations
- [ ] Notifications push reçues sur Android et iOS
- [ ] Commentaires threadés fonctionnels
- [ ] @mentions déclenchent des notifications
- [ ] Tests E2E passent (100%)
- [ ] Documentation à jour

## Notes

Cette change est la dernière étape de la Phase 3. Après complétion, le projet passera à la Phase 4 (intégrations externes réelles).
