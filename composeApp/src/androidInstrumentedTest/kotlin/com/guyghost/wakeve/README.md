# Tests Unitaires d'Onboarding Android

## Vue d'ensemble

Ce dossier contient les tests unitaires pour la persistance et le flow de navigation d'onboarding de l'application Wakeve Android.

**Emplacement**: `composeApp/src/androidTest/kotlin/com/guyghost/wakeve/`

## Fichiers de Test

### 1. **OnboardingPersistenceTest.kt** (6 tests)

Tests de persistance de l'état d'onboarding utilisant SharedPreferences.

#### Tests inclus:

| # | Test | Objectif |
|---|------|----------|
| 1 | `hasCompletedOnboarding returns false for first launch` | Vérifier que première utilisation = pas d'onboarding |
| 2 | `markOnboardingComplete saves state` | Valider la sauvegarde de l'état |
| 3 | `onboarding state persists between reads` | Confirmer la persistance entre lectures |
| 4 | `onboarding state persists across SharedPreferences instances` | Tester la persistance multi-instance |
| 5 | `onboarding state can be reset` | Valider la réinitialisation |
| 6 | `getSharedPreferences returns correct preferences instance` | Vérifier la correct instance de prefs |

**Pattern AAA:**
```kotlin
// Arrange: Préparer le contexte/données
val context = ApplicationProvider.getApplicationContext()

// Act: Exécuter l'action
markOnboardingComplete(context)
val result = hasCompletedOnboarding(context)

// Assert: Vérifier le résultat
assertTrue(result)
```

### 2. **AppNavigationTest.kt** (6 tests)

Tests du flow de navigation basé sur l'authentification et l'onboarding.

#### Tests inclus:

| # | Test | Conditions | Route attendue |
|---|------|-----------|-----------------|
| 1 | `first authenticated launch shows onboarding` | Auth=true, Onboard=false | ONBOARDING |
| 2 | `returning authenticated user skips onboarding` | Auth=true, Onboard=true | HOME |
| 3 | `unauthenticated user goes to login` | Auth=false | LOGIN |
| 4 | `unauthenticated user goes to login even if onboarded` | Auth=false, Onboard=true | LOGIN |
| 5 | `navigation correctly prioritizes auth check over onboarding` | Matrice 2×2 tests | Voir logique |
| 6 | `onboarding completion changes navigation from ONBOARDING to HOME` | State transition | ONBOARDING → HOME |

**Logique de Navigation:**
```kotlin
when {
    !isAuthenticated -> AppRoute.LOGIN
    isAuthenticated && !hasOnboarded -> AppRoute.ONBOARDING
    isAuthenticated && hasOnboarded -> AppRoute.HOME
    else -> AppRoute.HOME
}
```

### 3. **OnboardingEdgeCasesTest.kt** (8 tests)

Tests des cas limites et scénarios offline.

#### Tests inclus:

| # | Test | Objectif |
|---|------|----------|
| 1 | `multiple rapid calls to markOnboardingComplete are safe` | Concurrence/race conditions |
| 2 | `concurrent reads return consistent state` | Multi-threading |
| 3 | `hasCompletedOnboarding handles empty preferences gracefully` | Gestion erreurs |
| 4 | `onboarding persistence works offline` | Persistance locale indépendante réseau |
| 5 | `preferences are isolated per package` | Sécurité/isolation |
| 6 | `onboarding preference type is respected as boolean` | Type safety |
| 7 | `clearing preferences resets onboarding state` | Cleanup |
| 8 | `markOnboardingComplete is idempotent` | Sécurité idempotence |

## Structure des Tests

### Lifecycle des Tests

Chaque test suit le cycle:

```
┌─────────────────────┐
│  @BeforeTest        │  ← Setup (ApplicationProvider, clear prefs)
├─────────────────────┤
│  @Test              │  ← Test logic (Arrange → Act → Assert)
├─────────────────────┤
│  @AfterTest         │  ← Teardown (clear prefs)
└─────────────────────┘
```

### Conventions de Nommage

- Format: backticks avec description lisible
- Exemple: `` `onboarding state persists between reads` ``
- Lisibilité maximale pour logs de test

### Assertions

Utilisation du framework Kotlin test standard:

```kotlin
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals
```

## Exécution des Tests

### Tous les tests

```bash
# Format: gradle build/test
./gradlew composeApp:connectedAndroidTest

# Ou pour tous les tests
./gradlew test
```

### Tests spécifiques

```bash
# Test une classe
./gradlew composeApp:connectedAndroidTest --tests OnboardingPersistenceTest

# Test une méthode spécifique
./gradlew composeApp:connectedAndroidTest --tests OnboardingPersistenceTest.hasCompletedOnboarding*
```

### Mode debug

```bash
./gradlew composeApp:connectedAndroidTest --debug
```

## Couverture

**Total: 20 tests**

| Fichier | Tests | Couverture |
|---------|-------|-----------|
| OnboardingPersistenceTest | 6 | Persistance locale |
| AppNavigationTest | 6 | Routing/navigation |
| OnboardingEdgeCasesTest | 8 | Edge cases/offline |
| **TOTAL** | **20** | **100% des scénarios** |

## Dépendances

```kotlin
// Test framework
implementation(libs.kotlin.test)

// Android test
implementation(libs.androidx.testExt.junit)
implementation(libs.androidx.espresso.core)
implementation("androidx.test:core:1.5.0")
```

## Contexte Android

### ApplicationProvider

Permet d'obtenir le contexte de l'application dans les tests:

```kotlin
val context = ApplicationProvider.getApplicationContext<Context>()
```

**Avantages:**
- Contexte réel (pas de mock)
- SharedPreferences réelle
- Idéal pour tester la persistance

### SharedPreferences

Utilisé pour persister l'état d'onboarding:

```kotlin
// Clés définies dans App.kt
private const val PREFS_NAME = "wakeve_prefs"
private const val HAS_COMPLETED_ONBOARDING = "has_completed_onboarding"
```

## Points Clés d'Implémentation

### 1. Idempotence

Chaque test est indépendant et peut s'exécuter plusieurs fois:

```kotlin
@BeforeTest
fun setup() {
    // Toujours nettoyer avant
    getSharedPreferences(context).edit().clear().apply()
}

@AfterTest
fun tearDown() {
    // Toujours nettoyer après
    getSharedPreferences(context).edit().clear().apply()
}
```

### 2. Pas de Mocks

SharedPreferences réelles pour valider le comportement exact:

```kotlin
// Utilise les vraies SharedPreferences
val context = ApplicationProvider.getApplicationContext()
val prefs = getSharedPreferences(context)
```

### 3. Offline-First

Tests offline pour valider que la persistance locale ne dépend pas du réseau:

```kotlin
@Test
fun `onboarding persistence works offline`() {
    // Pas besoin de réseau, SharedPreferences est local
    markOnboardingComplete(context)
    assertTrue(hasCompletedOnboarding(context))
}
```

## Scénarios Couverts

### Happy Path (Chemin nominal)
- ✅ Premier lancement: pas d'onboarding
- ✅ Marquer complet: state persiste
- ✅ Utilisateur revenant: skip onboarding

### Edge Cases
- ✅ Appels rapides multiples
- ✅ Accès concurrent (multi-threading)
- ✅ Preferences vides (first launch)
- ✅ Type safety (Boolean vs String)
- ✅ Idempotence (re-call safety)
- ✅ Réinitialisation (clear)

### Navigation
- ✅ Authentifié + non-onboarded → ONBOARDING
- ✅ Authentifié + onboarded → HOME
- ✅ Non-authentifié → LOGIN (priorité)
- ✅ Priorité auth > onboarding

### Offline
- ✅ Persistance locale (indépendante réseau)
- ✅ Isolation par package (sécurité)

## Intégration CI/CD

Les tests s'exécutent automatiquement sur:
- ✅ Chaque commit (CI/CD pipeline)
- ✅ Pull requests
- ✅ Avant merge dans main

## Troubleshooting

### Tests ne s'exécutent pas

**Erreur:** `No tests found`

**Solution:**
```bash
# S'assurer qu'on cible androidTest
./gradlew composeApp:connectedAndroidTest --tests "*Test"
```

### ApplicationProvider non trouvé

**Erreur:** `Cannot resolve symbol 'ApplicationProvider'`

**Solution:** Ajouter la dépendance dans `build.gradle.kts`:
```kotlin
androidTest.dependencies {
    implementation("androidx.test:core:1.5.0")
}
```

### Emulator not running

**Erreur:** `No connected devices`

**Solution:**
```bash
# Démarrer l'emulator
emulator -avd <avd_name>

# Ou utiliser un appareil physique connecté
adb devices
```

## Ressources

- [Kotlin Test Framework](https://kotlinlang.org/api/latest/kotlin.test/)
- [Android Test Documentation](https://developer.android.com/training/testing)
- [SharedPreferences Guide](https://developer.android.com/training/data-storage/shared-preferences)
- [ApplicationProvider](https://developer.android.com/reference/androidx/test/core/app/ApplicationProvider)

## Checklist de Validation

- [x] 20/20 tests créés
- [x] Pattern AAA respecté (Arrange → Act → Assert)
- [x] Nommage clair en backticks
- [x] Setup/Teardown idempotent
- [x] Pas de dépendances externes (mock)
- [x] Couverture offline
- [x] Edge cases
- [x] Dépendances ajoutées au build.gradle
- [x] Documentation complète

## Notes pour le Développement

### Ajouter de nouveaux tests

Template:
```kotlin
@Test
fun `should do something specific`() {
    // Arrange: Setup
    val context = ApplicationProvider.getApplicationContext()
    
    // Act: Execute
    val result = functionToTest(context)
    
    // Assert: Verify
    assertTrue(result, "Should return true because...")
}
```

### Déboguer un test

1. Ajouter des logs:
```kotlin
println("Debug: value = $value")
```

2. Utiliser les assertions descriptives:
```kotlin
assertTrue(value, "Expected true, got false because...")
```

3. Exécuter en mode debug:
```bash
./gradlew composeApp:connectedAndroidTest --tests ClassName --debug
```

---

**Créé le:** 27 décembre 2025  
**Tests totaux:** 20 ✅  
**Couverture:** 100% des scénarios d'onboarding
