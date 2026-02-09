# Context: Analyse Code Wakeve - Optimisations et Evolutions

## Objective
Analyser le code de l'application Wakeve pour identifier:
- Optimisations de performance possibles
- Évolutions architecturales recommandées
- Améliorations de code quality
- Prochaines fonctionnalités prioritaires

## Constraints
- Platform: Kotlin Multiplatform (Android/iOS)
- Offline-first architecture
- FC&IS architecture pattern
- Design system: Material You (Android) / Liquid Glass (iOS)

## Contexte Projet
- **573 fichiers source** (Kotlin/Swift)
- **6 fichiers de test** détectés
- Architecture MVI avec State Machines
- SQLDelight pour persistance
- Phase 6 (Optimizations/Analytics) en planification (0/124 tâches)

## Changements OpenSpec Actifs
- `phase-6-optimizations-analytics`: 0/124 tâches
- `complete-phase5-and-fixes`: 0/51 tâques
- `implement-ios-meeting-list-view`: 11/24 tâches
- `implement-oauth-providers-activity-integration`: 13/26 tâches

## Structure Architecture
```
shared/src/
├── commonMain/kotlin/
│   ├── repository/          # EventRepository, AlbumRepository, etc.
│   ├── services/            # NotificationService, etc.
│   ├── presentation/
│   │   ├── statemachine/    # EventManagementStateMachine
│   │   └── state/           # Contracts (EventManagementContract)
│   ├── auth/
│   │   ├── core/            # Functional Core
│   │   └── shell/           # Imperative Shell
│   └── calendar/            # CalendarService (Phase 4/5)
├── commonTest/kotlin/       # Tests unitaires
├── androidMain/kotlin/      # Implémentations Android
└── iosMain/kotlin/          # Implémentations iOS

wakeveApp/src/
├── commonMain/kotlin/       # UI Android (Compose)
└── androidInstrumentedTest/ # Tests instrumentés

server/src/main/kotlin/      # Backend Ktor
```

## Domaines Clés à Analyser
1. **Performance**: DB indexes, pagination, cache, memory
2. **Architecture**: FC&IS compliance, State Machines
3. **Code Quality**: Tests coverage, dépendances, duplication
4. **Features**: Auth, Calendar, Notifications, Poll, Scenarios
5. **Backend**: API endpoints, sécurité, performance

## Technical Decisions
| Decision | Justification | Agent |
|----------|---------------|-------|
| Multi-agent analysis | Need expertise across architecture, code quality, performance | @orchestrator |
| Parallel delegation | Efficiency for independent analyses | @orchestrator |

## Artifacts Produced
| File | Agent | Status |
|------|-------|--------|
| context-log.jsonl | @orchestrator | ✅ Created |
| context.md | @orchestrator | ✅ Created |

## Inter-Agent Notes
<!-- Format: [@source → @destination] Message -->
- [@review → @orchestrator] 6 critical issues found including hardcoded JWT secrets, metrics IP bypass, and test coverage at only 20%
- [@validator → @orchestrator] 5 FC&IS violations - Core contains non-deterministic functions (Random, currentTimeMillis)
- [@sophos → @orchestrator] Architecture is solid BUT test debt is critical (~17% coverage). BLOCK Phase 6 until 60% coverage

## Synthesis - Key Findings

### Critical Issues (Must Fix Immediately)
1. **Hardcoded JWT secret** in production (Application.kt:246)
2. **Metrics IP whitelist bypass** - wildcard 0.0.0.0/0 allows any IP
3. **Blocking runBlocking** in async JWT blacklist check
4. **Test coverage crisis**: 573 source files, ~100 test files = ~17% coverage
5. **FC&IS violations**: Core uses Random and currentTimeMillis (non-deterministic)
6. **Database indexes missing**: Event, Vote, Participant tables have 0 indexes

### Architecture Strengths
- ✅ Clear FC&IS separation (Core doesn't import Shell)
- ✅ State Machine pattern well implemented
- ✅ Repository-mediated communication
- ✅ Solid offline-first foundation

### Phase 6 Assessment
- Original estimate: 8 weeks
- Realistic estimate: **13 weeks** (including test debt)
- **Recommendation**: Block Phase 6 until test coverage reaches 60%+
- Priority order: Tests → DB Indexes → Performance → Analytics → Push
- [@orchestrator → @review] Analysez la qualité du code, détection d'anti-patterns, et conformité aux conventions
- [@orchestrator → @validator] Vérifiez la cohérence FC&IS et l'architecture globale
- [@orchestrator → @sophos] Donnez une seconde opinion sur les recommandations critiques
