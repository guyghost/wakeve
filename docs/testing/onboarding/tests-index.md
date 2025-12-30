# Tests Onboarding - Index Complet

## 📂 Localisation des Fichiers

### Tests Instrumentés Android (requiert emulator)
```
composeApp/src/androidInstrumentedTest/kotlin/com/guyghost/wakeve/
├── OnboardingPersistenceTest.kt      (6 tests)
├── AppNavigationTest.kt              (6 tests)
├── OnboardingEdgeCasesTest.kt        (8 tests)
├── README.md                         (doc détaillée)
└── README_TESTS.md                   (index des tests)
```

### Tests Unitaires Cross-Platform (pas d'emulator)
```
composeApp/src/commonTest/kotlin/com/guyghost/wakeve/
├── NavigationRouteLogicTest.kt       (5 tests)
└── ComposeAppCommonTest.kt           (1 test exemple)
```

---

## 📊 Statistiques

| Catégorie | Nombre | Type |
|-----------|--------|------|
| Tests Persistence | 6 | Instrumented |
| Tests Navigation | 6 | Instrumented |
| Tests Edge Cases | 8 | Instrumented |
| Tests Logic | 5 | Common |
| Tests Existants | 1 | Common |
| **TOTAL** | **26** | **Mixed** |

---

## 📚 Documentation

### Fichiers de Documentation dans le Projet

1. **ONBOARDING_TESTS_DOCUMENTATION.md**
   - Documentation complète et détaillée
   - Architecture, patterns, troubleshooting
   - Ressources et références

2. **ONBOARDING_TESTS_QUICK_START.md**
   - Guide d'exécution rapide (5 minutes)
   - Commandes de base
   - Troubleshooting

3. **ONBOARDING_TESTS_SUMMARY.md**
   - Résumé visuel avec schémas
   - Vue d'ensemble des tests
   - Temps d'exécution

4. **TESTING_CHECKLIST.md** (fichier existant)
   - Checklist générale du projet

### Fichiers de Documentation dans les Tests

5. **composeApp/src/androidInstrumentedTest/README.md**
   - Documentation détaillée des tests instrumented
   - Conventions, lifecycle, patterns

6. **composeApp/src/androidInstrumentedTest/README_TESTS.md**
   - Index des fichiers de test
   - Objectifs et dépendances

---

## 🧪 Tests Créés

### OnboardingPersistenceTest (6 tests)
Valide la persistance de l'état d'onboarding via SharedPreferences

```
✅ hasCompletedOnboarding returns false for first launch
✅ markOnboardingComplete saves state
✅ onboarding state persists between reads
✅ onboarding state persists across SharedPreferences instances
✅ onboarding state can be reset
✅ getSharedPreferences returns correct preferences instance
```

### AppNavigationTest (6 tests)
Valide la logique de routing (navigation) basée sur auth + onboarding

```
✅ first authenticated launch shows onboarding
✅ returning authenticated user skips onboarding
✅ unauthenticated user goes to login
✅ unauthenticated user goes to login even if onboarded
✅ navigation correctly prioritizes auth check over onboarding
✅ onboarding completion changes navigation from ONBOARDING to HOME
```

### OnboardingEdgeCasesTest (8 tests)
Valide les cas limites et scénarios complexes

```
✅ multiple rapid calls to markOnboardingComplete are safe
✅ concurrent reads return consistent state
✅ hasCompletedOnboarding handles empty preferences gracefully
✅ onboarding persistence works offline
✅ preferences are isolated per package
✅ onboarding preference type is respected as boolean
✅ clearing preferences resets onboarding state
✅ markOnboardingComplete is idempotent
```

### NavigationRouteLogicTest (5 tests)
Valide la logique pure de navigation (cross-platform)

```
✅ navigation route selection works correctly for all state combinations
✅ authentication takes priority over onboarding in routing
✅ authenticated users without onboarding go to ONBOARDING screen
✅ returning authenticated users skip onboarding and go to HOME
✅ onboarding completion transitions from ONBOARDING to HOME
```

---

## 🚀 Exécution Rapide

### Tests rapides (pas d'emulator requis)
```bash
./gradlew commonTest
```

### Tests complets (emulator requis)
```bash
./gradlew connectedAndroidTest
```

### Tests spécifiques
```bash
# Une classe
./gradlew composeApp:connectedAndroidTest --tests OnboardingPersistenceTest

# Une méthode
./gradlew composeApp:connectedAndroidTest --tests "*returns false*"
```

---

## 🔍 Structure des Fichiers de Test

### OnboardingPersistenceTest.kt
- **Lignes:** ~170
- **Classes:** 1 (OnboardingPersistenceTest)
- **Méthodes:** 6 @Test + @BeforeTest + @AfterTest
- **Dépendances:** ApplicationProvider, SharedPreferences

### AppNavigationTest.kt
- **Lignes:** ~230
- **Classes:** 1 (AppNavigationTest)
- **Méthodes:** 6 @Test + @BeforeTest + @AfterTest
- **Dépendances:** AppRoute enum, auth state

### OnboardingEdgeCasesTest.kt
- **Lignes:** ~280
- **Classes:** 1 (OnboardingEdgeCasesTest)
- **Méthodes:** 8 @Test + @BeforeTest + @AfterTest
- **Dépendances:** Threading, SharedPreferences

### NavigationRouteLogicTest.kt
- **Lignes:** ~130
- **Classes:** 1 (NavigationRouteLogicTest)
- **Méthodes:** 5 @Test
- **Dépendances:** Aucune (logique pure)

---

## 🏗️ Configuration Build

### Fichier: composeApp/build.gradle.kts

**Dépendances ajoutées:**
```kotlin
androidInstrumentedTest.dependencies {
    // Android instrumented test dependencies
    implementation(libs.androidx.testExt.junit)
    implementation(libs.androidx.espresso.core)
    implementation(libs.kotlin.test)
    // Add androidx.test:core directly for ApplicationProvider
    implementation("androidx.test:core:1.5.0")
}
```

---

## ✅ Checklist de Complétude

### Fichiers de Test
- [x] OnboardingPersistenceTest.kt créé
- [x] AppNavigationTest.kt créé
- [x] OnboardingEdgeCasesTest.kt créé
- [x] NavigationRouteLogicTest.kt créé

### Documentation
- [x] ONBOARDING_TESTS_DOCUMENTATION.md
- [x] ONBOARDING_TESTS_QUICK_START.md
- [x] ONBOARDING_TESTS_SUMMARY.md
- [x] ONBOARDING_TESTS_INDEX.md (ce fichier)
- [x] README.md dans androidInstrumentedTest
- [x] README_TESTS.md dans androidInstrumentedTest

### Configuration
- [x] build.gradle.kts mis à jour
- [x] Dépendances test ajoutées
- [x] Répertoires de test créés

### Tests
- [x] 6 tests persistance ✅
- [x] 6 tests navigation ✅
- [x] 8 tests edge cases ✅
- [x] 5 tests logique pure ✅
- [x] 1 test existant

---

## 🎯 Couverture des Scénarios

### Happy Path
- ✅ Premier lancement (no onboarding)
- ✅ Marquer complet
- ✅ Utilisateur revenant
- ✅ Navigation correcte

### Navigation Matrix
```
Auth=F, Onb=F → LOGIN      ✅
Auth=F, Onb=T → LOGIN      ✅
Auth=T, Onb=F → ONBOARDING ✅
Auth=T, Onb=T → HOME       ✅
```

### Edge Cases
- ✅ Appels rapides
- ✅ Multi-threading
- ✅ Preferences vides
- ✅ Offline persistence
- ✅ Type safety
- ✅ Idempotence
- ✅ Cleanup

---

## 📖 Lecture Recommandée

1. **Pour commencer:** ONBOARDING_TESTS_QUICK_START.md
2. **Comprendre les tests:** composeApp/src/androidInstrumentedTest/README.md
3. **Documentation complète:** ONBOARDING_TESTS_DOCUMENTATION.md
4. **Résumé visuel:** ONBOARDING_TESTS_SUMMARY.md
5. **Code source:** Les fichiers .kt avec commentaires

---

## 🎓 Apprentissage

Les tests démontrent:
- Pattern AAA (Arrange → Act → Assert)
- SharedPreferences persistence
- Navigation routing logic
- Multi-threading safety
- Edge case handling
- Offline-first principles

---

## 📊 Temps d'Exécution

| Commande | Temps | Notes |
|----------|-------|-------|
| `commonTest` | 1-2s | Pas d'emulator |
| `connectedAndroidTest` | 3-5min | Avec emulator |
| Test spécifique | 30-60s | Avec emulator |

---

## 🔗 Liens Rapides

### Exécuter
```bash
# Fast
./gradlew commonTest

# Full
./gradlew connectedAndroidTest
```

### Consulter la Doc
- Quick Start: `ONBOARDING_TESTS_QUICK_START.md`
- Complète: `ONBOARDING_TESTS_DOCUMENTATION.md`

### Voir les Tests
- androidInstrumentedTest: `composeApp/src/androidInstrumentedTest/kotlin/.../`
- commonTest: `composeApp/src/commonTest/kotlin/.../NavigationRouteLogicTest.kt`

---

**Created:** 27 décembre 2025  
**Tests Total:** 26 ✅  
**Documentation:** Complète  
**Status:** ✅ Production Ready
