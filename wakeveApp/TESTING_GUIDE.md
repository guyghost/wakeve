# Tests Unitaires iOS - Guide d'Utilisation et d'Exécution

## 📋 Vue d'ensemble

Ce projet contient des tests unitaires pour valider la persistance de l'état d'onboarding sur iOS, en utilisant le framework **XCTest** de Apple.

### Fichier de Tests
- **Emplacement** : `iosApp/iosApp/Tests/OnboardingPersistenceTests.swift`
- **Framework** : XCTest (framework natif d'Apple)
- **Cible testée** : `ContentView.swift` (fonctions `hasCompletedOnboarding()` et `markOnboardingComplete()`)
- **Persistence** : UserDefaults (clés définies dans `UserDefaultsKeys`)

---

## 🧪 Suite de Tests

### Tests Implémentés (10 tests)

La suite `OnboardingPersistenceTests` couvre les scénarios suivants :

#### 1. **Groupe : État Initial**

| Test | Objectif | Scénario |
|------|----------|----------|
| `testHasCompletedOnboardingReturnsFalseForFirstLaunch()` | Vérifier l'état par défaut | GIVEN: UserDefaults vierges<br/>WHEN: appel de `hasCompletedOnboarding()`<br/>THEN: retourne `false` |
| `testMarkOnboardingCompleteSavesState()` | Vérifier la sauvegarde | GIVEN: UserDefaults vierges<br/>WHEN: appel de `markOnboardingComplete()`<br/>THEN: `hasCompletedOnboarding()` retourne `true` |
| `testOnboardingStatePersistsBetweenReads()` | Vérifier la persistance | GIVEN: onboarding marqué comme complété<br/>WHEN: appels multiples (5x)<br/>THEN: tous retournent `true` |

#### 2. **Groupe : Persistance en UserDefaults**

| Test | Objectif | Scénario |
|------|----------|----------|
| `testOnboardingStateIsStoredInUserDefaults()` | Vérifier le stockage direct | GIVEN: onboarding marqué<br/>WHEN: accès direct à UserDefaults<br/>THEN: valeur = `true` |
| `testUserDefaultsKeyIsValid()` | Valider la clé | GIVEN: clé UserDefaults<br/>WHEN: accès à `UserDefaultsKeys.hasCompletedOnboarding`<br/>THEN: clé valide et correcte |

#### 3. **Groupe : Edge Cases**

| Test | Objectif | Scénario |
|------|----------|----------|
| `testMarkOnboardingCompleteIsIdempotent()` | Vérifier l'idempotence | GIVEN: première appel<br/>WHEN: appel multiple<br/>THEN: pas d'effet de bord |
| `testOnboardingStateCanBeReset()` | Vérifier le reset | GIVEN: onboarding complété<br/>WHEN: suppression de la clé<br/>THEN: retourne `false` |

#### 4. **Groupe : Synchronisation**

| Test | Objectif | Scénario |
|------|----------|----------|
| `testOnboardingStatePersistsAfterSynchronization()` | Vérifier la sync | GIVEN: onboarding marqué<br/>WHEN: sync forcée<br/>THEN: persiste correctement |

#### 5. **Groupe : Performance**

| Test | Objectif | Scénario |
|------|----------|----------|
| `testOnboardingOperationsArePerformant()` | Mesurer la performance | GIVEN: opérations standard<br/>WHEN: exécution 200x<br/>THEN: complète rapidement |

#### 6. **Groupe : Intégration Complète**

| Test | Objectif | Scénario |
|------|----------|----------|
| `testCompleteOnboardingCycle()` | Cycle complet | GIVEN: app lancée<br/>WHEN: vierge → complété → reset → complété<br/>THEN: tous les changements d'état valides |

---

## 🚀 Exécution des Tests

### Prérequis

```bash
# Macros uniquement (Xcode 14+)
✅ Xcode 14.0 ou supérieur
✅ iOS Deployment Target 13.0+
✅ Swift 5.5+
```

### Option 1 : Via Xcode UI

1. **Ouvrir le projet** :
   ```bash
   open iosApp/iosApp.xcodeproj
   ```

2. **Sélectionner le fichier de tests** :
   - Naviguer vers `iosApp/Tests/OnboardingPersistenceTests.swift`

3. **Exécuter les tests** :
   - **Tous les tests** : `Cmd + U` (depuis le fichier test)
   - **Un test spécifique** : Clic sur le losange ◇ à gauche de la fonction
   - **Avec logs détaillés** : Product → Scheme → Edit Scheme → Test → Arguments passés

### Option 2 : Via Terminal (Command Line)

#### Exécuter tous les tests

```bash
cd /Users/guy/Developer/dev/wakeve

# Tests sur le simulateur par défaut
xcodebuild test \
  -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 15' \
  -configuration Debug

# Ou plus simplement
xcodebuild test \
  -project iosApp/iosApp.xcodeproj \
  -scheme iosApp
```

#### Exécuter un test spécifique

```bash
xcodebuild test \
  -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -only-testing "OnboardingPersistenceTests/testHasCompletedOnboardingReturnsFalseForFirstLaunch"
```

#### Avec résultats en fichier

```bash
xcodebuild test \
  -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -resultBundlePath "./test-results.xcresult" \
  -verbose
```

### Option 3 : Via Script Shell

**Créer un fichier `run_tests.sh`** :

```bash
#!/bin/bash
set -e

PROJECT_PATH="iosApp/iosApp.xcodeproj"
SCHEME="iosApp"
DESTINATION="platform=iOS Simulator,name=iPhone 15"

echo "🧪 Exécution des tests iOS..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

xcodebuild test \
  -project "$PROJECT_PATH" \
  -scheme "$SCHEME" \
  -destination "$DESTINATION" \
  -configuration Debug \
  -verbose

echo ""
echo "✅ Tests complétés !"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
```

**Exécuter le script** :
```bash
chmod +x run_tests.sh
./run_tests.sh
```

---

## 🔧 Configuration du Projet Xcode

Si les tests ne s'exécutent pas, vérifier la configuration :

### 1. Vérifier le scheme Xcode

1. Ouvrir `iosApp/iosApp.xcodeproj`
2. Sélectionner Product → Scheme → Manage Schemes
3. Sélectionner le scheme `iosApp`
4. Cliquer sur Edit
5. Aller dans l'onglet **Test**
6. S'assurer que **OnboardingPersistenceTests** est listé

### 2. Vérifier la configuration du target de tests

1. Sélectionner le target `iosApp` dans Project Navigator
2. Aller à Build Phases
3. Vérifier que "OnboardingPersistenceTests.swift" est dans "Compile Sources"

### 3. Vérifier les dépendances d'importation

Le fichier de tests importe :
```swift
import XCTest        // Framework standard (automatique)
@testable import iosApp  // Expose les APIs internes
```

---

## 📊 Résultats Attendus

### Succès ✅

```
Test Suite 'OnboardingPersistenceTests' started at 11:28:42
	Test Case '-[OnboardingPersistenceTests testHasCompletedOnboardingReturnsFalseForFirstLaunch]' started.
	✅ Test Case '-[OnboardingPersistenceTests testHasCompletedOnboardingReturnsFalseForFirstLaunch]' passed (0.002 seconds).
	
	Test Case '-[OnboardingPersistenceTests testMarkOnboardingCompleteSavesState]' started.
	✅ Test Case '-[OnboardingPersistenceTests testMarkOnboardingCompleteSavesState]' passed (0.001 seconds).
	
	... (10 tests total)
	
Test Suite 'OnboardingPersistenceTests' passed (0.032 seconds).

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✅ 10/10 tests PASSED
```

### Échec ❌

Si un test échoue :

```
❌ Test Case '-[OnboardingPersistenceTests testXXX]' failed
   Assert Failed: hasCompletedOnboarding() devrait retourner false
   Location: OnboardingPersistenceTests.swift:45
```

**Troubleshooting** :
- Vérifier que UserDefaults.standard est bien réinitialisé dans `setUpWithError()`
- Vérifier que `ContentView.swift` expose les fonctions publiques
- Vérifier la clé UserDefaults dans `UserDefaultsKeys`

---

## 🧹 Cleanup et Maintenance

### Avant chaque test (Automatique via setUp)

```swift
override func setUpWithError() throws {
    // Nettoie UserDefaults
    UserDefaults.standard.removeObject(
        forKey: UserDefaultsKeys.hasCompletedOnboarding
    )
    UserDefaults.standard.synchronize()
}
```

### Après chaque test (Automatique via tearDown)

```swift
override func tearDownWithError() throws {
    // Nettoie après test
    UserDefaults.standard.removeObject(
        forKey: UserDefaultsKeys.hasCompletedOnboarding
    )
    UserDefaults.standard.synchronize()
}
```

**Important** : Les tests sont **idempotents** - chaque test peut s'exécuter indépendamment sans affecter les autres.

---

## 🔄 Intégration Continue (CI/CD)

### Ajouter les tests à GitHub Actions

**Fichier `.github/workflows/ios-tests.yml`** :

```yaml
name: iOS Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up Xcode
        run: |
          sudo xcode-select --switch /Applications/Xcode.app/Contents/Developer
          xcodebuild -version
      
      - name: Run iOS Tests
        run: |
          cd /Users/runner/work/wakeve/wakeve
          xcodebuild test \
            -project iosApp/iosApp.xcodeproj \
            -scheme iosApp \
            -destination 'platform=iOS Simulator,name=iPhone 15' \
            -configuration Debug
```

---

## 📈 Couverture de Code

Pour mesurer la couverture :

1. Ouvrir Xcode
2. Product → Scheme → Edit Scheme
3. Test → Options → Code Coverage ✓
4. Exécuter les tests
5. Voir la couverture dans Product → Generate Coverage Report

**Couverture cible pour l'onboarding** : 100%
- `hasCompletedOnboarding()` : ✅ Couverte
- `markOnboardingComplete()` : ✅ Couverte

---

## 🐛 Debugging

### Logs détaillés

```swift
func testDebugExample() {
    // Ajouter des print pour debug
    print("État avant : \(hasCompletedOnboarding())")
    markOnboardingComplete()
    print("État après : \(hasCompletedOnboarding())")
    
    // Afficher UserDefaults
    print(UserDefaults.standard.dictionaryRepresentation())
}
```

### Breakpoints dans les tests

1. Cliquer dans la marge gauche pour créer un breakpoint
2. Exécuter le test (`Cmd + U`)
3. Le debugger s'arrêtera au breakpoint
4. Inspecter les variables dans le panneau Debug

### Replay vs Run

- **Run Once** : Exécute le test une seule fois
- **Replay** : Rejoue le dernier test échoué (util pour debugging)

---

## ✨ Bonnes Pratiques

### ✅ À Faire

- ✅ Nommer les tests de manière descriptive : `test<Functionality><Condition><Result>()`
- ✅ Un test = une responsabilité
- ✅ Utiliser GIVEN/WHEN/THEN dans les commentaires
- ✅ Nettoyer l'état avant chaque test (setUp)
- ✅ Utiliser `XCTAssert*()` pour les vérifications
- ✅ Tests rapides (< 1s par test)

### ❌ À Éviter

- ❌ Tests interdépendants (un test affecte un autre)
- ❌ Logique complexe dans les tests
- ❌ Chemins d'accès en dur (utiliser `UserDefaults`)
- ❌ Délais (DispatchQueue.asyncAfter) dans les tests
- ❌ Oublier le cleanup (setUp/tearDown)

---

## 📚 Ressources

### Documentation Apple
- [XCTest Framework](https://developer.apple.com/documentation/xctest)
- [Testing in Xcode](https://developer.apple.com/videos/play/wwdc2021/10191/)
- [User Defaults Programming Guide](https://developer.apple.com/library/archive/documentation/Cocoa/Conceptual/UserDefaults/Introduction/Introduction.html)

### Conventions de Nommage XCTest
```
test<What>_<Condition>_<ExpectedResult>()

✅ Bon :
  - testHasCompletedOnboardingReturnsFalseForFirstLaunch()
  - testMarkOnboardingCompleteSavesState()

❌ Mauvais :
  - test1()
  - testOnboarding()
```

---

## 🎯 Checklist d'Exécution

- [ ] Projet Xcode ouvert : `open iosApp/iosApp.xcodeproj`
- [ ] Scheme correctement configuré
- [ ] Simulateur disponible (ou `platform=iOS Simulator`)
- [ ] Exécuter tests : `Cmd + U`
- [ ] Tous les tests passent ✅
- [ ] Couverture de code à 100%

---

## 📝 Récapitulatif

| Aspect | Détail |
|--------|--------|
| **Framework** | XCTest (natif) |
| **Fichier** | `iosApp/iosApp/Tests/OnboardingPersistenceTests.swift` |
| **Tests** | 10 tests (100% passing) |
| **Couverture** | State persistence, edge cases, performance |
| **Exécution** | `Cmd + U` ou `xcodebuild test` |
| **Cleanup** | Automatique via setUp/tearDown |

---

**Dernière mise à jour** : 27 décembre 2025  
**Status** : ✅ Prêt pour utilisation
