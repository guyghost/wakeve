# Context: Phase 6 - Optimizations, Analytics & Advanced Push

## Objective
Implémenter la Phase 6 du projet Wakeve axée sur les optimisations de performance, l'analytics et les notifications push avancées.

## Décision d'Architecture: Scission en 3 Sous-Changes

**Risque élevé** d'implémenter Phase 6 comme un seul changement OpenSpec:
- **Complexité**: 3 domaines distincts (Performance, Analytics, Push) avec peu de dépendances
- **Delivery**: Livraison monolithique après 8 semaines sans feedback utilisateur intermédiaire
- **Reviews**: PR géant impossible à review en profondeur
- **Testing**: Tests cross-domain difficiles à maintenir

**Recommandation**: Scinder en 3 sous-changes OpenSpec:
1. **`phase-6-performance`** (Weeks 1-3) - DB indexes, pagination, cache, image optimization
2. **`phase-6-analytics`** (Weeks 4-5) - Analytics provider, Firebase integration, dashboard, RGPD
3. **`phase-6-advanced-push`** (Weeks 6-8) - Rich notifications, scheduler, deep linking

**Avantages**:
- ✅ Delivery incrémental avec valeur utilisateur à chaque étape
- ✅ Feedback rapide (après 3, 5, 8 semaines)
- ✅ PRs plus petits = reviews plus rapides et approfondies
- ✅ Tests isolés par domaine
- ✅ Risque distribué sur 3 changes
- ✅ Priorisation flexible (possibilité de cut scope si nécessaire)

## Scope Phase 6

### 1. Performance Optimizations

#### 1.1 Database Indexes
**Fichiers**: `.sqldelight/*.sq`
- Ajouter indexes manquants pour requêtes fréquentes
- Analyser slow queries avec EXPLAIN QUERY PLAN
- Optimiser les jointures

#### 1.2 Lazy Loading
**Fichiers**: `wakeveApp/.../ListScreens.kt`
- Pagination pour les listes d'événements (50 items/page)
- LazyColumn avec keys pour recyclage efficace
- Image loading avec Coil (placeholder, cache)

#### 1.3 Memory Management
**Fichiers**: `shared/.../repositories/*.kt`
- Cache LRU pour les données fréquemment accédées
- Cleanup automatique des vieilles données
- Gestion des coroutines (scope approprié)

### 2. Analytics

#### 2.1 Events Tracking
**Fichier**: `shared/src/commonMain/kotlin/com/guyghost/wakeve/analytics/AnalyticsService.kt`

**Events à tracker**:
- `event_created` - Création d'événement
- `event_joined` - Participation à un événement
- `poll_voted` - Vote sur un sondage
- `scenario_selected` - Sélection d'un scénario
- `meeting_created` - Création de réunion
- `error_occurred` - Erreurs (avec contexte)
- `screen_view` - Navigation entre écrans

#### 2.2 Analytics Provider
- Interface `AnalyticsProvider` (Firebase, Mixpanel, Amplitude)
- Mock pour tests/développement
- Respect RGPD (consentement utilisateur)

#### 2.3 Dashboard
**Fichier**: `server/src/main/kotlin/com/guyghost/wakeve/analytics/AnalyticsDashboard.kt`
- Endpoints pour métriques clés
- MAU, DAU, rétention
- Funnel de conversion (creation → finalization)

### 3. Advanced Push Notifications

#### 3.1 Rich Notifications
**Fichiers**: `wakeveApp/.../notification/`
- Images dans les notifications
- Actions rapides ("Oui/Non/Peut-être" pour sondage)
- Grouped notifications par événement
- Priorité et interruption level

#### 3.2 Notification Categories
- **Poll**: Nouveau sondage, rappel vote
- **Scenario**: Nouveau scénario proposé
- **Meeting**: Rappel réunion (15min, 1h, 24h)
- **Comment**: Mention @username
- **System**: Invitations, confirmations

#### 3.3 Backend Integration
**Fichier**: `server/src/main/kotlin/com/guyghost/wakeve/notification/NotificationScheduler.kt`
- Scheduled notifications avec WorkManager
- Batch processing pour réduire les appels FCM
- Rate limiting

#### 3.4 Deep Linking
- Navigation automatique vers l'écran concerné
- Gestion des notifications quand app est fermée
- Tracking de conversion (notification → action)

### 4. Monitoring & Observability

#### 4.1 Crash Reporting
- Firebase Crashlytics (Android)
- Crashlytics iOS
- Groupement des crashes par version

#### 4.2 Performance Monitoring
- Firebase Performance
- Temps de démarrage
- Temps de chargement des écrans
- Requêtes réseau lentes

#### 4.3 Logging
**Fichier**: `shared/src/commonMain/kotlin/com/guyghost/wakeve/logging/WakevLogger.kt`
- Niveaux: DEBUG, INFO, WARN, ERROR
- Structured logging (JSON)
- Rotation des logs

## Livrables

### Performance
- [ ] Indexes SQLDelight optimisés
- [ ] Pagination implémentée sur toutes les listes
- [ ] Cache LRU pour repositories
- [ ] Memory profiling et optimisations

### Analytics
- [ ] AnalyticsService.kt avec interface
- [ ] FirebaseAnalyticsProvider.kt
- [ ] Events tracking dans les ViewModels
- [ ] Dashboard backend avec endpoints

### Push Notifications
- [ ] Rich notification service
- [ ] NotificationScheduler.kt
- [ ] Deep linking handler
- [ ] Categories et actions rapides

### Monitoring
- [ ] Crashlytics intégré (Android/iOS)
- [ ] Performance monitoring
- [ ] WakevLogger.kt

---

## Analyse Détaillée

### État Actuel de la Base de Données

**Tables SQLDelight analysées** (37 tables au total):
- ✅ `Comment.sq`: **8 indexes** (y compris composites) + pagination déjà implémentée
- ❌ `Event.sq`: **0 indexes** (requêtes fréquentes: selectByOrganizerId, selectByStatus)
- ❌ `Participant.sq`: **0 indexes** (requêtes fréquentes: selectByEventId, selectByRole)
- ❌ `Vote.sq`: **0 indexes** (requêtes fréquentes: selectByEventId, selectByTimeslotId)
- ❌ `TimeSlot.sq`: **0 indexes** (requêtes fréquentes: selectByEventId)
- ❌ `Scenario.sq`: **0 indexes** (requêtes fréquentes: selectByEventId)
- ❌ `Meeting.sq`: **0 indexes** (requêtes fréquentes: selectByEventId)

**Requêtes lentes identifiées** (basé sur l'analyse des fichiers .sq):
```sql
-- Event.sq: Sans index, full table scan
SELECT * FROM event WHERE organizerId = ? ORDER BY createdAt DESC;
SELECT * FROM event WHERE status = ? ORDER BY createdAt DESC;

-- Participant.sq: Sans index, full table scan
SELECT * FROM participant WHERE eventId = ? ORDER BY joinedAt ASC;
SELECT * FROM participant WHERE eventId = ? AND role = ?;

-- Vote.sq: Sans index, full table scan + JOIN
SELECT v.*, t.eventId, p.userId FROM vote v
JOIN timeSlot t ON v.timeslotId = t.id
JOIN participant p ON v.participantId = p.id
WHERE t.eventId = ?;
```

**Impact attendu**: -50% de temps de requête après ajout des indexes

---

### Réponses aux Questions Clés

#### 1. Quelle est la tâche la plus risquée ? Pourquoi ?

**Réponse**: **P0.6/P0.8: Rich Notifications + NotificationScheduler**

**Raisons**:
1. **Complexité cross-platform élevée**:
   - Android: NotificationCompat avec actions, images, grouping
   - iOS: UNUserNotificationCenter avec categories, attachments, UNNotificationAction
   - APIs très différentes → risque d'inconsistences

2. **WorkManager (Android) instabilité**:
   - WorkManager peut avoir des bugs sur certains devices
   - Constraints (network type) ne sont pas toujours respectées
   - Background execution peut être tuée par le système

3. **iOS Notifications limitations**:
   - UNNotificationAttachment nécessite des images locales (download préalable)
   - Categories doivent être enregistrées avant d'afficher les notifications
   - Actions ne fonctionnent pas si l'app est en background

4. **Deep linking depuis notifications**:
   - Gestion complexe: app fermée vs ouverte
   - Navigation state peut être corrompu si mal géré
   - Deeplink peut être intercepté par d'autres apps

**Mitigations**:
- Tests instrumentés étendus pour Android/iOS
- Fallback aux notifications basiques si crash
- Monitoring via Crashlytics pour détecter rapidement les problèmes
- Feature flag pour désactiver rapidement si bugs en production
- Documentation détaillée des différences platform

---

#### 2. Quelles tâches peuvent être parallélisées ?

**Graphe de dépendances**:

```
Indépendantes (peuvent être faites en parallèle):
┌─────────────────────────────────────────────┐
│ P0.1 (DB Indexes)                      │
│ P0.3 (Cache LRU)                       │
│ P1.10 (Crashlytics)                    │
│ P1.11 (Performance Monitoring)            │
│ P1.12 (WakevLogger)                    │
└─────────────────────────────────────────────┘

Dépendantes (nécessitent les tâches ci-dessus):
┌─────────────────────────────────────────────┐
│ P0.2 (Pagination) ← P0.1              │
│ P0.5 (Memory Profiling) ← P0.2, P0.3   │
│ P1.1 (Analytics Interface)               │
│ P1.2 (Firebase Provider) ← P1.1         │
│ P1.3 (Analytics Integration) ← P1.2      │
│ P1.4 (Dashboard) ← P1.2                │
│ P1.5 (RGPD) ← P1.1                    │
└─────────────────────────────────────────────┘

Phase Push (indépendante de Performance/Analytics):
┌─────────────────────────────────────────────┐
│ P0.6 (Rich Notifications)               │
│ P0.7 (Categories) ← P0.6               │
│ P0.8 (Scheduler) ← P0.6, P0.7          │
│ P1.9 (Deep Linking) ← P0.6              │
└─────────────────────────────────────────────┘
```

**Recommandation de parallélisation**:

**Week 1** (4 développeurs en parallèle):
- Dev A: P0.1 (DB Indexes)
- Dev B: P0.3 (Cache LRU)
- Dev C: P1.10 (Crashlytics)
- Dev D: P1.12 (WakevLogger)

**Week 2** (3 développeurs en parallèle):
- Dev A: P0.2 (Pagination) ← P0.1 terminé
- Dev B: P0.4 (Coil)
- Dev C: P1.11 (Performance Monitoring)

**Week 3**:
- Dev A: P0.5 (Memory Profiling)
- Dev B: Tests Performance
- Dev C: Bug fixes Performance

**Week 4** (3 développeurs en parallèle):
- Dev A: P1.1 (Analytics Interface)
- Dev B: P1.2 (Firebase Provider)
- Dev C: P1.5 (RGPD Consent)

**Week 5** (2 développeurs en parallèle):
- Dev A: P1.3 (Analytics Integration)
- Dev B: P1.4 (Dashboard)

**Weeks 6-7** (2 développeurs en parallèle):
- Dev A: P0.6 (Rich Notifications)
- Dev B: P0.7 (Categories)

**Week 8**:
- Dev A: P0.8 (Scheduler) ← P0.6, P0.7 terminés
- Dev B: P1.9 (Deep Linking)

**Résultat**: De 8 semaines (1 dev) → **5 semaines** (4 devs)

---

#### 3. Quelle est l'estimation totale de temps pour la Phase 6 ?

**Réponse**: **8 semaines avec 1 développeur**, **5-6 semaines avec 2-3 développeurs**

**Breakdown détaillé par sous-change**:

| Sous-Change | Tâches | Estimation (1 dev) | Estimation (2-3 devs) |
|-------------|---------|-------------------|----------------------|
| `phase-6-performance` | P0.1, P0.2, P0.3, P0.4, P0.5 | 15 jours (3 semaines) | 10 jours (2 semaines) |
| `phase-6-analytics` | P1.1, P1.2, P1.3, P1.4, P1.5, P1.10, P1.11, P1.12 | 10 jours (2 semaines) | 8 jours (1.5 semaines) |
| `phase-6-advanced-push` | P0.6, P0.7, P0.8, P1.9 | 15 jours (3 semaines) | 12 jours (2.5 semaines) |
| **TOTAL** | - | **40 jours (8 semaines)** | **30 jours (6 semaines)** |

**Assumptions**:
- 5 jours de travail effectif par semaine
- 20% de buffer pour imprévus
- Tests inclus dans l'estimation
- Code reviews inclus (1 jour par semaine)

**Facteurs d'ajustement**:
- **+20%** si équipe junior sur Kotlin Multiplatform
- **+15%** si tests iOS instrumentés non disponibles
- **+10%** si intégration Firebase requiert plus de temps que prévu

---

#### 4. Faut-il scinder Phase 6 en plusieurs changements plus petits ?

**Réponse**: **OUI, fortement recommandé** → 3 sous-changes OpenSpec

**Comparaison: Monolithique vs Scindé**

| Critère | Monolithique (1 change) | Scindé (3 changes) | Gagnant |
|---------|------------------------|---------------------|---------|
| **Complexité** | Très élevée (3 domaines) | Élevée par change (1 domaine) | Scindé ✅ |
| **Delivery** | 8 semaines avant valeur utilisateur | 3, 5, 8 semaines (delivery incrémental) | Scindé ✅ |
| **Reviews** | PR géant (~2000 lignes) impossible à review en profondeur | 3 PRs plus petits (~600 lignes chacun) | Scindé ✅ |
| **Testing** | Tests cross-domain difficiles à maintenir | Tests isolés par domaine, plus faciles | Scindé ✅ |
| **Risk** | Un bug bloque tout le change | Bug affecte seulement 1 sous-change | Scindé ✅ |
| **Priorisation** | Impossible de cut scope sans rework | Peut prioriser/supprimer un sous-change | Scindé ✅ |
| **Tracking** | Difficile de suivre l'avancement par domaine | Suivi clair par sous-change | Scindé ✅ |
| **Archiving** | Un seul gros change à archiver | 3 changements indépendants | Scindé ✅ |

**Structure recommandée des 3 sous-changes**:

##### 1. `phase-6-performance`
- **Files**: `shared/src/commonMain/sqldelight/**/*.sq`, `wakeveApp/ui/event/*ListScreen.kt`, `shared/cache/LRUCache.kt`
- **Tasks**: P0.1, P0.2, P0.3, P0.4, P0.5
- **Livrable**: DB optimisée, listes paginées, cache LRU, images optimisées
- **Valeur utilisateur**: -50% query time, scroll 60fps
- **Dependencies**: Aucune
- **Estimation**: 3 semaines (1 dev), 2 semaines (2-3 devs)

##### 2. `phase-6-analytics`
- **Files**: `shared/analytics/*`, `server/analytics/*`, `wakeveApp/ui/settings/*`
- **Tasks**: P1.1, P1.2, P1.3, P1.4, P1.5, P1.10, P1.11, P1.12
- **Livrable**: Analytics complet, dashboard backend, monitoring RGPD compliant
- **Valeur utilisateur**: Data-driven decisions, crash reporting
- **Dependencies**: Aucune (peut être fait en parallèle avec phase-6-performance)
- **Estimation**: 2 semaines (1 dev), 1.5 semaines (2-3 devs)

##### 3. `phase-6-advanced-push`
- **Files**: `shared/notification/*`, `wakeveApp/notification/*`
- **Tasks**: P0.6, P0.7, P0.8, P1.9
- **Livrable**: Rich notifications, scheduler, deep linking
- **Valeur utilisateur**: Engagement augmenté, réduction des no-shows
- **Dependencies**: Aucune (peut être fait en parallèle avec les autres)
- **Estimation**: 3 semaines (1 dev), 2.5 semaines (2-3 devs)

**Workflow recommandé**:

```
Week 1-2:
  ├── phase-6-performance (P0.1, P0.3, P0.4) ─┐
  ├── phase-6-analytics (P1.1, P1.10, P1.12) ───┤
  └── phase-6-advanced-push (P0.6) ────────────────┤ → Parallel execution

Week 3-4:
  ├── phase-6-performance (P0.2, P0.5) → ARCHIVE
  ├── phase-6-analytics (P1.2, P1.5) ───────────────┤
  └── phase-6-advanced-push (P0.7) ────────────────┤ → Parallel execution

Week 5-6:
  ├── phase-6-analytics (P1.3, P1.4) → ARCHIVE
  └── phase-6-advanced-push (P0.8, P1.9) ────────┤ → Parallel execution

Week 7-8:
  └── phase-6-advanced-push → ARCHIVE
```

**Avantages additionnels**:
- Feedback utilisateur après 3 semaines (performance)
- Feedback après 5 semaines (analytics)
- Si un sous-change est bloqué, les autres continuent
- Possibilité de livrer partiellement en production (staged rollout)

## Non-Goals (Phase 7+)
- Machine Learning pour recommandations
- A/B testing framework
- Feature flags

## Contraintes
- **Offline-first**: Analytics en file d'attente si offline
- **RGPD**: Consentement explicite pour analytics/push
- **Batterie**: Pas de polling, utiliser push uniquement
- **Privacy**: Pas de tracking utilisateur sensible
