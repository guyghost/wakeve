# 🔧 Plan d'Action - Réparation des Tests

## Étape 1: Fixer TransportServiceTest.kt (CRITIQUE - 20 min)

### Fichier à modifier
`shared/src/commonTest/kotlin/com/guyghost/wakeve/transport/TransportServiceTest.kt`

### Changement requis
Ajouter `= runBlocking` à chaque fonction `@Test` qui appelle une suspend function.

### Détail des changements

**AVANT - Ligne 13-31 (Test 1)**:
```kotlin
@Test
fun `getTransportOptions returns options for flight mode`() {
    // Given
    val from = TransportLocation("Paris", "Paris CDG Airport", 49.0, 2.5, "CDG")
    val to = TransportLocation("London", "London Heathrow", 51.5, -0.5, "LHR")
    val departureTime = "2025-12-25T10:00:00Z"

    // When
    val options = transportService.getTransportOptions(from, to, departureTime, TransportMode.FLIGHT)
    // ...
}
```

**APRÈS - Ligne 13-31 (Test 1)**:
```kotlin
@Test
fun `getTransportOptions returns options for flight mode`() = runBlocking {
    // Given
    val from = TransportLocation("Paris", "Paris CDG Airport", 49.0, 2.5, "CDG")
    val to = TransportLocation("London", "London Heathrow", 51.5, -0.5, "LHR")
    val departureTime = "2025-12-25T10:00:00Z"

    // When
    val options = transportService.getTransportOptions(from, to, departureTime, TransportMode.FLIGHT)
    // ...
}
```

### Résumé des changements
- **Ligne 13**: Changer `fun \`getTransportOptions returns options for flight mode\`() {` 
  - En: `fun \`getTransportOptions returns options for flight mode\`() = runBlocking {`

- **Ligne 34**: Changer `fun \`getTransportOptions returns multiple modes when no mode specified\`() {`
  - En: `fun \`getTransportOptions returns multiple modes when no mode specified\`() = runBlocking {`

- **Ligne 50**: Changer `fun \`optimizeRoutes returns plan with cost minimization\`() {`
  - En: `fun \`optimizeRoutes returns plan with cost minimization\`() = runBlocking {`

- **Ligne 75**: Changer `fun \`optimizeRoutes returns plan with time minimization\`() {`
  - En: `fun \`optimizeRoutes returns plan with time minimization\`() = runBlocking {`

- **Ligne 97**: Changer `fun \`optimizeRoutes returns plan with balanced optimization\`() {`
  - En: `fun \`optimizeRoutes returns plan with balanced optimization\`() = runBlocking {`

- **Ligne 119**: Changer `fun \`findGroupMeetingPoints groups close arrival times\`() {`
  - En: `fun \`findGroupMeetingPoints groups close arrival times\`() = runBlocking {`

- **Ligne 175**: Changer `fun \`findGroupMeetingPoints separates far arrival times\`() {`
  - En: `fun \`findGroupMeetingPoints separates far arrival times\`() = runBlocking {`

- **Ligne 229**: Changer `fun \`walking options only generated for same location\`() {`
  - En: `fun \`walking options only generated for same location\`() = runBlocking {`

- **Ligne 244**: Changer `fun \`options are sorted by cost ascending\`() {`
  - En: `fun \`options are sorted by cost ascending\`() = runBlocking {`

### Vérification que runBlocking est importé
Vérifier que ce import est présent (ligne 4):
```kotlin
import kotlinx.coroutines.runBlocking
```

Si absent, l'ajouter après les autres imports.

---

## Étape 2: Vérifier la compilation (5 min)

```bash
cd /Users/guy/Developer/dev/wakeve
./gradlew shared:test --dry-run
```

**Résultat attendu**:
```
BUILD SUCCESSFUL
```

Si compilation OK:
```bash
./gradlew shared:test
```

**Résultat attendu**:
```
> 380+ tests executed
> X failed, X skipped, X passed
```

---

## Étape 3: Analyser les résultats (10 min)

Rechercher les patterns d'erreurs:
- Aucun "Suspend function" error ✅
- Tous les tests commonTest doivent compiler ✅
- Les tests jvmTest doivent compiler ✅

---

## Étape 4: Documenter les résultats (5 min)

Créer un fichier `TEST_RESULTS_FIXED.md` avec:
- Date de la correction
- Nombre de tests compilés
- Nombre de tests passés/échoués
- Aucune erreur "Suspend function" ✅

---

## Validation Finale

### Checklist
```
Avant la correction:
[ ] BUILD FAILED
[ ] 9 erreurs "Suspend function"
[ ] 0 tests exécutés
[ ] TransportServiceTest.kt: FAILED

Après la correction:
[ ] BUILD SUCCESSFUL
[ ] 0 erreurs de compilation
[ ] 380+ tests exécutés
[ ] TransportServiceTest.kt: 9/9 tests passés OU visibles
[ ] 0 new errors dans les autres fichiers
```

### Commandes de validation
```bash
# Vérifier pas d'erreur de coroutine
./gradlew shared:test --info 2>&1 | grep -c "Suspend function"
# Attendu: 0

# Voir résumé des tests
./gradlew shared:test 2>&1 | tail -50

# Voir les noms des tests passés
./gradlew shared:test --info 2>&1 | grep "PASSED"
```

---

## Notes Supplémentaires

### Pourquoi runBlocking?
- Les fonctions `suspend` ne peuvent être appelées que dans un contexte coroutine
- `runBlocking` crée ce contexte pour les tests
- Voir: `EventRepositoryTest.kt` pour exemple

### Pattern correct
```kotlin
@Test
fun myTest() = runBlocking {
    // Can call suspend functions here
    val result = mySuspendFunction()
    assertEquals(expected, result)
}
```

### SI ça ne compile toujours pas
1. Vérifier que `runBlocking` est importé
2. Vérifier la syntaxe exacte: `fun() = runBlocking {}`
3. Vérifier les accolades: `{` et `}` doivent être présentes
4. Relancer: `./gradlew clean shared:test`

---

## Estimations d'effort

| Tâche | Durée | Complexité |
|-------|-------|-----------|
| Corriger TransportServiceTest.kt | 20 min | Basse |
| Compiler et valider | 10 min | Basse |
| Exécuter tests | 10 min | Basse |
| Documenter | 5 min | Très basse |
| **TOTAL** | **45 min** | **Basse** |

---

## Support

Si vous rencontrez une erreur inattendue:
1. Exécuter: `./gradlew clean build`
2. Vérifier le message d'erreur complet
3. Consulter `TEST_ANALYSIS_REPORT.md` pour plus de contexte
4. Vérifier que seul `TransportServiceTest.kt` a été modifié

---

**Généré**: 28 décembre 2025  
**Par**: @tests Agent  
**Status**: Prêt à exécuter
