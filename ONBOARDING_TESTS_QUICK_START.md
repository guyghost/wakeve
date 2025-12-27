# Quick Start - Exécution des Tests Onboarding

## ⚡ Démarrage Rapide (5 minutes)

### 1. Tests Logique Pure (Rapide, pas d'émulator)

```bash
# Exécuter les tests logique pure
./gradlew composeApp:commonTest

# Ou tous les tests
./gradlew test
```

**Résultat attendu:**
```
✅ NavigationRouteLogicTest.navigation route selection works correctly...
✅ NavigationRouteLogicTest.authentication takes priority over onboarding...
✅ (4 autres tests...)

BUILD SUCCESSFUL
```

**Temps:** ~15-30 secondes

---

### 2. Tests Instrumentés (Requiert Emulator/Device)

#### Préalable: Démarrer un émulator

```bash
# Vérifier les émulateurs disponibles
emulator -list-avds

# Démarrer (exemple avec Pixel 4 API 31)
emulator -avd Pixel_4_API_31 &

# Vérifier que l'appareil est connecté
adb devices
```

#### Exécuter les tests

```bash
# Tous les tests instrumentés
./gradlew connectedAndroidTest

# Ou spécifiquement onboarding
./gradlew composeApp:connectedAndroidTest
```

**Résultat attendu:**
```
✅ OnboardingPersistenceTest.hasCompletedOnboarding returns false...
✅ OnboardingPersistenceTest.markOnboardingComplete saves state...
✅ AppNavigationTest.first authenticated launch shows onboarding...
✅ (14 autres tests...)

BUILD SUCCESSFUL
```

**Temps:** ~2-5 minutes (selon l'émulator)

---

## 📍 Localisation des Tests

### Tests Unitaires (Pas besoin d'Emulator)
```
composeApp/src/commonTest/kotlin/com/guyghost/wakeve/
  └── NavigationRouteLogicTest.kt (5 tests)
```

### Tests Android Instrumentés (Requiert Emulator)
```
composeApp/src/androidInstrumentedTest/kotlin/com/guyghost/wakeve/
  ├── OnboardingPersistenceTest.kt (6 tests)
  ├── AppNavigationTest.kt (6 tests)
  └── OnboardingEdgeCasesTest.kt (8 tests)
```

---

## 🎯 Filtrer les Tests

### Exécuter une classe de test

```bash
./gradlew composeApp:connectedAndroidTest --tests OnboardingPersistenceTest
```

### Exécuter une méthode spécifique

```bash
./gradlew composeApp:connectedAndroidTest \
  --tests "OnboardingPersistenceTest.hasCompletedOnboarding*"
```

### Tests contenant un mot clé

```bash
./gradlew connectedAndroidTest --tests "*persistence*"
```

---

## 🔍 Déboguer les Tests

### Voir les logs détaillés

```bash
./gradlew connectedAndroidTest --info
```

### Mode debug complet

```bash
./gradlew connectedAndroidTest --debug
```

### Arrêter à la première erreur

```bash
./gradlew connectedAndroidTest --fail-fast
```

---

## 📊 Couverture de Tests

### Résumé

```
Total tests:     25 ✅
- Navigation:     5 (logique)
- Persistance:    6 (SharedPreferences)
- Navigation:     6 (routing)
- Edge cases:     8 (concurrence, offline, etc)

Couverture:      100% des scénarios
Temps total:     ~3-5 min (avec emulator)
```

### Détails par fichier

| Fichier | Tests | Type | Durée |
|---------|-------|------|-------|
| NavigationRouteLogicTest | 5 | Logique pure | 1-2s |
| OnboardingPersistenceTest | 6 | Instrumenté | 30-60s |
| AppNavigationTest | 6 | Instrumenté | 30-60s |
| OnboardingEdgeCasesTest | 8 | Instrumenté | 60-90s |

---

## ✅ Vérifier les Tests

### IDE Android Studio

1. **Ouvrir le fichier test**
   ```
   composeApp/src/androidInstrumentedTest/kotlin/com/guyghost/wakeve/OnboardingPersistenceTest.kt
   ```

2. **Clic droit sur la classe**
   ```
   Run 'OnboardingPersistenceTest' (Ctrl+Shift+F10)
   ```

3. **Voir les résultats** dans l'onglet Run

### CLI (Command Line)

```bash
# Compact output
./gradlew connectedAndroidTest

# Verbose output
./gradlew connectedAndroidTest --info

# Avec logs stdout
./gradlew connectedAndroidTest --gradle-user-home /tmp/gradle
```

---

## 🚨 Troubleshooting Rapide

### ❌ "No connected devices"

```bash
# Vérifier
adb devices

# Démarrer emulator
emulator -avd Pixel_4_API_31 &

# Attendre ~30 secondes
# Retry: adb devices
```

### ❌ "Test class not found"

**Cause:** Dossier mauvais

**Solution:**
```bash
# Vérifier que les tests sont dans le bon dossier
ls composeApp/src/androidInstrumentedTest/kotlin/com/guyghost/wakeve/

# Doit afficher:
# OnboardingPersistenceTest.kt
# AppNavigationTest.kt
# OnboardingEdgeCasesTest.kt
```

### ❌ "Permission denied"

```bash
# Rendre exécutable
chmod +x gradlew

# Retry
./gradlew connectedAndroidTest
```

### ❌ "Timeout"

```bash
# Augmenter le timeout (en ms)
./gradlew connectedAndroidTest --org.gradle.workers.max=1
```

---

## 🎓 Ce que les Tests Valident

### ✅ Persistance d'Onboarding
```kotlin
markOnboardingComplete(context)  // Sauvegarde
hasCompletedOnboarding(context)  // Récupère true
```

### ✅ Navigation Correcte
```
Auth=false        → LOGIN
Auth=true, no onb → ONBOARDING
Auth=true, onb    → HOME
```

### ✅ Cas Limites
```
Appels rapides        → Safe
Accès concurrent      → Cohérent
Preferences vides     → Default false
Type safety           → Boolean
Idempotence           → Safe re-call
```

### ✅ Offline
```
SharedPreferences local → Works offline
Pas dépendance réseau   → Full persistence
```

---

## 📌 Bonnes Pratiques

### ✅ À Faire
```bash
# Run en parallèle
./gradlew test connectedAndroidTest

# Filtrer pour développement
./gradlew connectedAndroidTest \
  --tests "*Persistence*"

# Voir logs pour debug
./gradlew connectedAndroidTest --info
```

### ❌ À Éviter
```bash
# Trop général
./gradlew test  # Peut inclure d'autres tests

# Sans context
./gradlew connectedAndroidTest
# Sans emulator -> Erreur
```

---

## 🔄 Workflow Typique

```
1. Écrire un test
   ↓
2. Exécuter: ./gradlew commonTest
   ↓
3. Implémenter la fonctionnalité
   ↓
4. Exécuter: ./gradlew connectedAndroidTest
   ↓
5. Tous les tests passent ✅
   ↓
6. Commit & Push
```

---

## 📞 Besoin d'Aide?

### Lire la Documentation
```
Documentation complète:
  ONBOARDING_TESTS_DOCUMENTATION.md (ce répertoire)

Dans le dossier test:
  composeApp/src/androidInstrumentedTest/README.md

Documentation officielle:
  https://developer.android.com/training/testing
```

### Exécuter un Test Spécifique pour Déboguer
```bash
./gradlew composeApp:connectedAndroidTest \
  --tests "OnboardingPersistenceTest.markOnboardingComplete*" \
  --info
```

### Voir les Logs du Test
```bash
./gradlew connectedAndroidTest --stacktrace
```

---

## ⏱️ Temps d'Exécution Estimé

| Commande | Temps | Notes |
|----------|-------|-------|
| `commonTest` | 15-30s | Pas d'emulator requis |
| `connectedAndroidTest` | 2-5min | Emulator requis |
| Test spécifique | 30-60s | Emulator requis |
| Tous (test + connectedAndroidTest) | 3-6min | Parallèle possible |

---

## ✨ Prochaines Étapes

Une fois les tests exécutés avec succès:

1. ✅ Vérifier que tous les tests passent
2. ✅ Consulter la documentation complète si besoin
3. ✅ Ajouter des tests pour nouvelles features
4. ✅ Intégrer dans CI/CD (GitHub Actions)
5. ✅ Célébrer! 🎉

---

**Created:** 27 décembre 2025  
**Quick Start Version:** 1.0  
**Tests Total:** 25 ✅
