# Tasks: Fix Critical Security Issues

## Overview
Ce change regroupe les corrections de sécurité et d'architecture critiques identifiées lors de l'analyse.

**Estimation**: 1 semaine
**Priorité**: P0 (Bloquant pour production)
**Dependencies**: Aucune

---

## 1. Security Fixes

### 1.1 Fix Hardcoded JWT Secret
**Files**: `server/src/main/kotlin/com/guyghost/wakeve/Application.kt`
**Time**: 2 hours
**Priority**: P0

**Current**:
```kotlin
jwt {
    secret = System.getenv("JWT_SECRET") ?: "default-secret-key-change-in-production"
}
```

**Target**:
```kotlin
jwt {
    secret = System.getenv("JWT_SECRET")
        ?: throw IllegalStateException("JWT_SECRET environment variable required")
}
```

- [x] Modifier Application.kt ligne 246
- [x] Mettre à jour documentation (JWT_SECRET requis)
- [ ] Vérifier CI/CD a JWT_SECRET configuré
- [ ] Tests: Vérifier exception si JWT_SECRET manquant

### 1.2 Fix Metrics IP Whitelist Bypass
**Files**: `server/src/main/kotlin/com/guyghost/wakeve/security/SecurityConfig.kt`
**Time**: 1 hour
**Priority**: P0

**Current**:
```kotlin
val ALLOWED_IPS = listOf("127.0.0.1", "10.0.0.0/8", "0.0.0.0/0")
```

**Target**:
```kotlin
val ALLOWED_IPS = listOf("127.0.0.1", "10.0.0.0/8")
```

- [x] Supprimer "0.0.0.0/0" de SecurityConfig.kt ligne 71
- [ ] Tests: Vérifier accès refusé depuis IP externe

### 1.3 Fix Blocking runBlocking in Async
**Files**: `server/src/main/kotlin/com/guyghost/wakeve/Application.kt`
**Time**: 4 hours
**Priority**: P0

**Current**:
```kotlin
runBlocking {
    jwtBlacklistRepository.isTokenBlacklisted(token)
}
```

**Target**:
- Implémenter un cache LRU pour JWT blacklist
- Fonction suspend sans runBlocking
- Invalidation du cache sur revocation

- [x] Créer JwtBlacklistCache
- [x] Modifier JWT validation plugin
- [ ] Tests: Vérifier performance avec 1000+ tokens

---

## 2. FC&IS Architecture Fixes

### 2.1 Remove Non-determinism from Core
**Files**: 
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/auth/core/logic/validateOTP.kt`
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/auth/core/validation/AuthValidators.kt`
**Time**: 4 hours
**Priority**: P0

**Changes**:
- [ ] Déplacer `generateOTP()` vers `auth/shell/services/EmailAuthService`
- [ ] Injecter Random comme dépendance
- [ ] Modifier tous les appelants
- [ ] Tests: Vérifier OTP génération toujours fonctionnelle

### 2.2 Remove expect/actual from Core
**Files**: `shared/src/commonMain/kotlin/com/guyghost/wakeve/auth/core/logic/parseJWT.kt`
**Time**: 3 hours
**Priority**: P0

**Changes**:
- [ ] Créer JwtParserShell dans auth/shell/
- [ ] Déplacer expect/actual vers Shell
- [ ] Core devient pure fonction avec dépendances injectées
- [ ] Tests: Vérifier parsing JWT fonctionne toujours

### 2.3 Make Models Time-Agnostic
**Files**:
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/auth/core/models/AuthToken.kt`
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/auth/core/models/User.kt`
**Time**: 3 hours
**Priority**: P0

**Changes**:
- [ ] Modifier `createGuest()` pour accepter `currentTime: Long` paramètre
- [ ] Modifier `createAuthenticated()` pour accepter `currentTime: Long` paramètre
- [ ] Mettre à jour tous les appelants dans Shell
- [ ] Tests: Vérifier création utilisateurs fonctionne

---

## 3. Database Index Fixes

### 3.1 Add Event Table Indexes
**Files**: `shared/src/commonMain/sqldelight/com/guyghost/wakeve/Event.sq`
**Time**: 2 hours
**Priority**: P0

```sql
CREATE INDEX IF NOT EXISTS idx_event_organizer ON event(organizerId, createdAt DESC);
CREATE INDEX IF NOT EXISTS idx_event_status ON event(status, createdAt DESC);
CREATE INDEX IF NOT EXISTS idx_event_updated ON event(updatedAt);
```

- [x] Ajouter indexes dans Event.sq
- [x] Générer code SQLDelight
- [ ] Tests: Benchmark requêtes avant/après

### 3.2 Add Vote Table Indexes
**Files**: `shared/src/commonMain/sqldelight/com/guyghost/wakeve/Vote.sq`
**Time**: 2 hours
**Priority**: P0

```sql
CREATE INDEX IF NOT EXISTS idx_vote_event ON vote(eventId, createdAt DESC);
CREATE INDEX IF NOT EXISTS idx_vote_timeslot ON vote(timeslotId, vote);
CREATE INDEX IF NOT EXISTS idx_vote_participant ON vote(participantId, createdAt DESC);
CREATE INDEX IF NOT EXISTS idx_vote_timeslot_participant ON vote(timeslotId, participantId);
```

- [x] Ajouter indexes dans Vote.sq
- [ ] Tests: Benchmark agrégation votes

### 3.3 Add Participant Table Indexes
**Files**: `shared/src/commonMain/sqldelight/com/guyghost/wakeve/Participant.sq`
**Time**: 1 hour
**Priority**: P0

```sql
CREATE INDEX IF NOT EXISTS idx_participant_event ON participant(eventId, joinedAt ASC);
CREATE INDEX IF NOT EXISTS idx_participant_user ON participant(userId);
CREATE INDEX IF NOT EXISTS idx_participant_role ON participant(eventId, role);
CREATE INDEX IF NOT EXISTS idx_participant_validated ON participant(eventId, hasValidatedDate);
```

- [x] Ajouter indexes dans Participant.sq

### 3.4 Add TimeSlot Table Indexes
**Files**: `shared/src/commonMain/sqldelight/com/guyghost/wakeve/TimeSlot.sq`
**Time**: 1 hour
**Priority**: P0

```sql
CREATE INDEX IF NOT EXISTS idx_timeslot_event ON timeSlot(eventId, startTime ASC);
```

- [x] Ajouter indexes dans TimeSlot.sq

### 3.5 Add Scenario Table Indexes
**Files**: `shared/src/commonMain/sqldelight/com/guyghost/wakeve/Scenario.sq`
**Time**: 1 hour
**Priority**: P0

```sql
CREATE INDEX IF NOT EXISTS idx_scenario_event ON scenario(eventId, createdAt DESC);
```

- [x] Ajouter indexes dans Scenario.sq

---

## 4. Test Fixes

### 4.1 Enable Meeting UseCase Tests
**Files**:
- `shared/src/commonTest/kotlin/com/guyghost/wakeve/presentation/usecase/UpdateMeetingUseCaseTest.kt`
- `shared/src/commonTest/kotlin/com/guyghost/wakeve/presentation/usecase/LoadMeetingsUseCaseTest.kt`
- `shared/src/commonTest/kotlin/com/guyghost/wakeve/presentation/usecase/CreateMeetingUseCaseTest.kt`
**Time**: 4 hours
**Priority**: P1

- [ ] Implémenter mocking pour Database
- [ ] Créer test fixtures pour Meeting
- [ ] Activer et faire passer tous les tests
- [ ] Couverture: 80%+ pour Meeting use cases

---

## Validation Checklist

- [ ] Tous les tests passent (./gradlew test)
- [ ] Benchmarks DB montrent amélioration >30%
- [ ] Sécurité: JWT secret requis, IP whitelist restrictif
- [ ] FC&IS: Core 100% pur (vérifié par @validator)
- [ ] Code review approuvée
- [ ] Documentation mise à jour

---

**Total Estimation**: 5 jours (~1 semaine)
