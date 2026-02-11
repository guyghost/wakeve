# Prochaines Étapes - Roadmap Wakeve

## 🎯 Objectif: Production Ready

Ce document définit les prochaines étapes pour rendre Wakeve production-ready après les travaux en mode Ralph.

---

## Phase 1: Tests & Qualité (P0)

### 1.1 Atteindre 60% de couverture de tests
**Priorité**: P0 | **Estimation**: 2-3 jours

**Tâches**:
- [ ] Ajouter 15 tests pour les services (NotificationService, SyncService, etc.)
- [ ] Tests pour error handling et edge cases
- [ ] Tests pour retry logic et circuit breakers

**Fichiers concernés**:
- `shared/src/commonTest/kotlin/com/guyghost/wakeve/services/`

### 1.2 Tests d'intégration E2E
**Priorité**: P0 | **Estimation**: 3-4 jours

**Scénarios à tester**:
- [ ] Workflow complet: DRAFT → POLLING → CONFIRMED → ORGANIZING → FINALIZED
- [ ] Offline/Online transitions
- [ ] Multi-participant concurrent access
- [ ] Sync conflicts resolution

**Fichiers**:
- `shared/src/commonTest/kotlin/com/guyghost/wakeve/e2e/CompleteWorkflowE2ETest.kt`

### 1.3 Mesurer couverture avec JaCoCo
**Priorité**: P0 | **Estimation**: 1 jour

```kotlin
// Ajouter à build.gradle.kts
plugins {
    jacoco
}

jacoco {
    toolVersion = "0.8.11"
}

tasks.jacocoTestReport {
    reports {
        xml.required = true
        html.required = true
    }
}
```

---

## Phase 2: Phase 6 Advanced Push (P0.6-P0.9)

### 2.1 Rich Notifications (P0.6)
**Priorité**: P0 | **Estimation**: 3-4 jours

**Android**:
- [ ] Images dans notifications (BigPictureStyle)
- [ ] Actions rapides (buttons)
- [ ] Progress notifications

**iOS**:
- [ ] Rich media attachments
- [ ] Notification categories
- [ ] Custom UI

**Fichiers**:
- `wakeveApp/src/androidMain/kotlin/com/guyghost/wakeve/notification/RichNotificationManager.kt`
- `wakeveApp/wakeveApp/Services/RichNotificationService.swift`

### 2.2 Notification Categories et Actions (P0.7)
**Priorité**: P0 | **Estimation**: 2-3 jours

**Catégories**:
- [ ] EVENT_INVITE (avec actions: Accept/Decline/Maybe)
- [ ] POLL_REMINDER (avec action: Vote)
- [ ] MEETING_STARTING (avec action: Join)
- [ ] SCENARIO_VOTE (avec action: Vote Yes/No)

**Fichiers**:
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/notification/NotificationCategory.kt`

### 2.3 Notification Scheduler (P0.8)
**Priorité**: P1 | **Estimation**: 2-3 jours

**Features**:
- [ ] WorkManager (Android) pour rappels programmés
- [ ] UNNotificationRequest (iOS) pour local notifications
- [ ] Rappels: 24h, 1h, 15min avant événement
- [ ] Rappels de vote (date limite approchant)

**Fichiers**:
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/notification/NotificationScheduler.kt`

### 2.4 Deep Linking (P0.9)
**Priorité**: P1 | **Estimation**: 2-3 jours

**Deep Links**:
- [ ] `wakeve://event/{id}` - Ouvrir événement
- [ ] `wakeve://poll/{eventId}` - Ouvrir sondage
- [ ] `wakeve://meeting/{meetingId}` - Rejoindre réunion
- [ ] `wakeve://invite/{token}` - Accepter invitation

**Fichiers**:
- `wakeveApp/src/androidMain/kotlin/com/guyghost/wakeve/deeplink/DeepLinkHandler.kt`
- `wakeveApp/wakeveApp/Services/DeepLinkService.swift`

---

## Phase 3: Production Readiness

### 3.1 Performance Benchmarks
**Priorité**: P0 | **Estimation**: 2-3 jours

**Métriques à mesurer**:
- [ ] Temps de chargement initial app: < 2s
- [ ] Temps de chargement liste événements (50 items): < 100ms
- [ ] Temps de création d'événement: < 500ms
- [ ] Temps de vote: < 200ms
- [ ] Memory usage: < 100MB en idle
- [ ] Battery usage: optimisation des syncs

**Outils**:
- Android Profiler
- Xcode Instruments
- Firebase Performance Monitoring

### 3.2 Security Audit Finale
**Priorité**: P0 | **Estimation**: 2-3 jours

**Vérifications**:
- [ ] Audit JWT token handling
- [ ] Vérifier input validation sur tous les endpoints
- [ ] SQL injection prevention (SQLDelight safe)
- [ ] Certificate pinning pour API calls
- [ ] Secure storage audit (Keychain/Keystore)
- [ ] Hardcoded secrets scan
- [ ] OWASP Mobile Top 10 compliance

**Fichiers à auditer**:
- `server/src/main/kotlin/com/guyghost/wakeve/security/`
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/auth/`

### 3.3 Documentation API Complète
**Priorité**: P1 | **Estimation**: 3-4 jours

**Documentation**:
- [ ] OpenAPI/Swagger spec pour tous les endpoints
- [ ] Authentication flow documentation
- [ ] Error codes et handling guide
- [ ] Rate limiting documentation
- [ ] Webhook documentation (si applicable)

**Fichiers**:
- `docs/API/openapi.yaml`
- `docs/API/AUTHENTICATION.md`
- `docs/API/ERROR_HANDLING.md`

---

## Phase 4: Optimisations Futures (P2)

### 4.1 LRU Cache (P0.3 - Deferred)
**Priorité**: P2 | **Estimation**: 2-3 jours

Implémenter si profiling montre besoin:
- [ ] Cache LRU pour résultats de requêtes
- [ ] Cache Coil déjà implémenté
- [ ] Benchmark avant/après

### 4.2 CRDT pour Conflict Resolution
**Priorité**: P2 | **Estimation**: 5-7 jours

- [ ] Remplacer last-write-wins par CRDT
- [ ] Collaborative editing support
- [ ] Better offline experience

### 4.3 Multi-language Support
**Priorité**: P2 | **Estimation**: 3-4 jours

- [ ] i18n pour FR, EN, ES, DE
- [ ] String resources extraction
- [ ] RTL support

---

## 📋 Checklist Pre-Production

### Code Quality
- [ ] 60%+ test coverage (JaCoCo)
- [ ] 0 critical issues (SonarQube/detekt)
- [ ] All tests passing (800+)
- [ ] Documentation à jour

### Performance
- [ ] Benchmarks passants
- [ ] No memory leaks (Profiler)
- [ ] Battery usage acceptable
- [ ] Cold start < 2s

### Security
- [ ] Security audit passed
- [ ] Penetration testing
- [ ] RGPD compliance verified
- [ ] Data encryption at rest

### Infrastructure
- [ ] CI/CD pipeline stable
- [ ] Monitoring (Crashlytics, Analytics)
- [ ] Backup strategy
- [ ] Rollback plan

---

## 📅 Planning Estimé

| Phase | Durée | Dépendances |
|-------|-------|-------------|
| Tests & Qualité | 1 semaine | - |
| Advanced Push | 1.5 semaines | Tests |
| Production Ready | 1.5 semaines | Push |
| **Total** | **4 semaines** | - |

---

## 🎯 Prochaine Session Ralph

**Recommandation**: Commencer par **Phase 1.1** (atteindre 60% coverage) car:
- Base essentielle pour la confiance
- Bloque les régressions futures
- Nécessaire avant Advanced Push

**Commande pour démarrer**:
```bash
# Mesurer couverture actuelle
./gradlew jacocoTestReport

# Voir rapport
open build/reports/jacoco/index.html
```

---

**Document créé**: 2026-02-10
**Dernière mise à jour**: 2026-02-10
**Version**: 1.0
