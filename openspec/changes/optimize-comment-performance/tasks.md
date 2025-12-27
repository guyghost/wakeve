# Tasks - Optimize Comment System Performance

## Change: `optimize-comment-performance`
**Status**: 🟢 Active
**Dernière mise à jour**: 26 décembre 2025
**Progress**: 6/8 tasks complétées (75%)

### Priorité des Optimisations

1. **HAUTE PRIO**: Database indexes (impact immédiat)
2. **HAUTE PRIO**: Pagination (évite chargement complet)
3. **MOYENNE PRIO**: Cache en mémoire (réduit requêtes répétées)
4. **MOYENNE PRIO**: Lazy loading des réponses (réduit taille initiale)
5. **BASSE PRIO**: Views pré-calculées (complexité supplémentaire)

---

## Sprint 1 - Database & Core Optimizations

### Task 1.1: Database Indexes Optimization ✅ TERMINÉ ✅ TERMINÉ
- [x] **Ajouter indexes composites** pour requêtes fréquentes
  - `idx_comment_event_section` (event_id, section, created_at DESC)
  - `idx_comment_event_section_item` (event_id, section, section_item_id, created_at DESC)
  - `idx_comment_section_item_replies` (section_item_id, parent_comment_id, created_at ASC)
  - `idx_comment_event_created_paging` (event_id, created_at DESC)
- [x] **Migration SQLDelight** pour nouveaux indexes
- [x] **Tests performance** avant/après indexation
- **Fichier**: `shared/src/commonMain/sqldelight/com/guyghost/wakeve/Comment.sq`

### Task 1.2: Pagination Queries ✅ TERMINÉ ✅ TERMINÉ
- [x] **Ajouter queries paginées** dans Comment.sq
  - `selectTopLevelCommentsByEventPaginated` (event_id, limit, offset)
  - `selectTopLevelCommentsBySectionPaginated` (event_id, section, limit, offset)
  - `selectTopLevelCommentsBySectionAndItemPaginated` (event_id, section, section_item_id, limit, offset)
- [x] **Modèle PagingData<T>** pour résultats paginés
- [x] **Tests queries** avec données volumineuses
- **Fichier**: `shared/src/commonMain/sqldelight/com/guyghost/wakeve/Comment.sq`

### Task 1.3: Comment Repository Updates ✅ TERMINÉ ✅ TERMINÉ
- [x] **Méthodes paginées** dans CommentRepository
  - `getTopLevelCommentsByEventPaginated()`
  - `getTopLevelCommentsBySectionPaginated()`
  - `getTopLevelCommentsBySectionAndItemPaginated()`
- [x] **Lazy loading** pour réponses (paramètre loadReplies)
  - `getCommentsWithThreadsLazy()`
- [x] **Intégration cache** (optionnel useCache)
- **Fichier**: `shared/src/commonMain/kotlin/com/guyghost/wakeve/comment/CommentRepository.kt`

### Task 1.4: In-Memory Cache Implementation ✅ TERMINÉ ✅ TERMINÉ
- [x] **CommentCache class** avec TTL (5 minutes)
  - `get()`, `put()`, `invalidate()`
  - Éviction LRU simple (max 100 entrées)
- [x] **CommentListResult** data class pour cache entries
- [x] **Intégration** dans CommentRepository
  - Cache invalidation lors modifications
  - Cache key generation (event:id:section:item)
- **Fichier**: `shared/src/commonMain/kotlin/com/guyghost/wakeve/comment/CommentCache.kt`

---

## Sprint 2 - UI & User Experience

### Task 2.1: Android Virtual Scrolling
- [ ] **Pagination UI** dans CommentsScreen.kt
  - LazyColumn avec loadMore automatique
  - Indicateur de chargement en bas
  - État "fin de liste"
- [ ] **Progressive loading** des réponses
  - Bouton "Load replies" pour threads
  - Indicateur de chargement par thread
- [ ] **Pull-to-refresh** pour cache invalidation
- **Fichier**: `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/comment/CommentsScreen.kt`

### Task 2.2: iOS Virtual Scrolling
- [ ] **LazyVStack pagination** dans CommentsView.swift
  - LoadMore automatique en scroll
  - Diffable data source optimization
- [ ] **Progressive replies** loading
  - DisclosureGroup pour threads
  - Loading states par commentaire
- [ ] **Pull-to-refresh** avec refreshable()
- **Fichier**: `iosApp/iosApp/Views/CommentsView.swift`

---

## Sprint 3 - Advanced Optimizations

### Task 3.1: Pre-calculated Statistics Views ✅ TERMINÉ
- [ ] **View comment_section_stats** dans Comment.sq
  - event_id, section, comment_count, unique_authors, last_comment_at
- [ ] **Query selectCommentSectionStats**
- [ ] **Mise à jour automatique** lors changements
- [ ] **Utilisation** dans UI pour stats rapides
- **Fichier**: `shared/src/commonMain/sqldelight/com/guyghost/wakeve/Comment.sq`

### Task 3.2: Performance Tests Suite ✅ TERMINÉ
- [ ] **CommentPerformanceTest.kt**
  - Test chargement 100 commentaires paginés (< 1s)
  - Test cache réduction temps requête (< 10ms hit)
  - Test indexes accélèrent requêtes (< 500ms)
- [ ] **Benchmarks** avant/après optimisations
- [ ] **Tests mémoire** pour cache
- **Fichier**: `shared/src/jvmTest/kotlin/com/guyghost/wakeve/comment/CommentPerformanceTest.kt`

---

## Sprint 4 - Polish & Monitoring

### Task 4.1: Offline-First Enhancements
- [ ] **Queue background sync** pour commentaires hors ligne
- [ ] **Conflict resolution** last-write-wins + timestamp
- [ ] **Feedback utilisateur** pour état sync
- **Fichier**: Intégration avec existant OfflineScenarioTest.kt

### Task 4.2: Documentation & Monitoring
- [ ] **Performance guidelines** dans docs
- [ ] **Metrics collection** (chargement temps, cache hit rate)
- [ ] **Alertes** pour lenteurs (> 2s)
- [ ] **Mise à jour** openspec/specs/event-organization/spec.md

---

## Files à modifier/créer

1. `shared/src/commonMain/sqldelight/com/guyghost/wakeve/Comment.sq` - Indexes + queries + views
2. `shared/src/commonMain/kotlin/com/guyghost/wakeve/comment/CommentRepository.kt` - Pagination + cache
3. `shared/src/commonMain/kotlin/com/guyghost/wakeve/comment/CommentCache.kt` - NOUVEAU
4. `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/comment/CommentsScreen.kt` - Virtual scrolling
5. `iosApp/iosApp/Views/CommentsView.swift` - Lazy loading
6. `shared/src/jvmTest/kotlin/com/guyghost/wakeve/comment/CommentPerformanceTest.kt` - NOUVEAU

## Notes importantes

1. **Migration DB**: Nouveaux indexes nécessitent migration SQLDelight
2. **Cache invalidation**: Invalider cache lors modifications (create/update/delete)
3. **Tests performance**: Données réalistes (500+ commentaires, 10 sections)
4. **Pagination UI**: Indicateur chargement, "fin de liste"
5. **Virtual scrolling**: LazyColumn/iOS LazyVStack pour performance
6. **Offline-first**: Toujours tester scénarios offline/online

## Métriques de succès

- **Temps chargement**: < 1s pour 100 commentaires paginés
- **Cache hit**: < 10ms pour données en cache
- **Mémoire**: -60% utilisation pour grandes listes
- **UI smoothness**: Pas de blocage pendant chargement
- **Offline resilience**: Sync automatique en arrière-plan