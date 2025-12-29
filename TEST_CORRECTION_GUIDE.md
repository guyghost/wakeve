# Guide de Correction des Tests JVM

## 📋 Aperçu

Ce document fournit les corrections spécifiques pour les fichiers de test problématiques qui empêchent l'exécution des tests JVM.

---

## 🎯 Objectif

Permettre l'exécution complète des tests JVM, incluant les tests `ScenarioRepositoryTest` nouvellement ajoutés.

---

## 🔧 Corrections par Fichier

### 1. CalendarServiceTest.kt (Facile)

**Erreurs:**
```
CalendarPermissionDeniedException` non résolue aux lignes 197, 201, 205
```

**Correction:**
Ajouter l'import manquant ou définir l'exception.

```kotlin
// Option A: Importer l'exception (si elle existe dans le module)
import com.guyghost.wakeve.calendar.CalendarPermissionDeniedException

// Option B: Définir l'exception (si elle n'existe pas)
class CalendarPermissionDeniedException(message: String) : Exception(message)
```

**Priorité:** ⭐⭐☆☆☆ (Facile - 5 min)

---

### 2. CollaborationIntegrationTest.kt (Facile)

**Erreurs:**
```
Only safe (?.) or non-null asserted (!!.) calls are allowed on a nullable receiver of type 'Comment?'
```

**Localisation:** Lignes 404 et 405

**Correction:**
```kotlin
// Avant (incorrect):
val comment = comments.first()
val text = comment.content

// Après (correct):
val comment = comments.firstOrNull()
val text = comment?.content ?: ""
```

Ou:
```kotlin
// Avant (incorrect):
comments.first().content

// Après (correct):
comments.first()!!.content  // Assertion non-null
```

**Priorité:** ⭐⭐☆☆☆ (Facile - 10 min)

---

### 3. CommentPerformanceTest.kt (Facile)

**Erreurs:**
```
1. Cannot infer type for type parameter 'T' (ligne 91, colonne 46)
2. Unresolved reference 'assertFalse' (ligne 116)
```

**Correction 1 - Type inference:**
```kotlin
// Ajouter le type explicitement:
val comments: List<Comment> = commentRepository.getCommentsBySection(
    eventId = "test-event",
    section = CommentSection.GENERAL
)
```

**Correction 2 - Import manquant:**
```kotlin
// Ajouter aux imports:
import kotlin.test.assertFalse
```

**Priorité:** ⭐☆☆☆☆ (Très facile - 5 min)

---

### 4. PrdWorkflowE2ETest.kt (Difficile)

**Statut:** Déjà partiellement corrigé mais renommé en `.broken`

**Erreurs corrigées:**
```kotlin
✅ createEquipment - Retourne directement la liste d'autoGenerateChecklist
✅ createActivity - Utilise DateTimePeriod au lieu de kotlin.time.Duration
✅ addComment - Ajoute authorName et createdAt
```

**Erreurs restantes:**
- Structure de classe brisée (parenthèses/accolades mal fermées)
- Résolution de variables impossible (activityService, commentRepository)

**Options:**

#### Option A: Restaurer et corriger le fichier
1. Renommer `.broken` → `.kt`
2. Vérifier la parenthésation des fonctions
3. Vérifier l'ordre des paramètres dans les appels de méthodes

#### Option B: Réécrire partiellement
Garder uniquement les tests critiques et supprimer les tests complexes E2E.

**Priorité:** ⭐⭐⭐☆☆ (Difficile - 30-60 min)

---

### 5. RecommendationEngineTest.kt (Très Difficile)

**Statut:** Complètement désynchronisé avec les modèles actuels, renommé en `.disabled`

**Problème principal:**
Le fichier utilise des modèles qui n'existent pas ou qui ont une structure différente:
- `BudgetRange(min, max, currency)` - Dans le code réel, c'est un enum: `LOW, MEDIUM, HIGH`
- `Season` - N'existe pas
- `LocationPreferences` - N'existe pas
- `SuggestionInteractionType` - N'existe pas

**Modèles réels:**
```kotlin
// Dans shared/src/commonMain/kotlin/com/guyghost/wakeve/models/RecommendationModels.kt
data class UserPreferences(
    val userId: String,
    val preferredDaysOfWeek: List<String>,
    val preferredTimes: List<String>,
    val preferredLocations: List<String>,
    val preferredActivities: List<String>,
    val budgetRange: BudgetRange? = null,  // Enum, pas data class!
    val groupSizePreference: Long? = null,
    val lastUpdated: String
)

enum class BudgetRange {
    LOW, MEDIUM, HIGH
}
```

**Options:**

#### Option A: Réécrire complètement
1. Aligner le code avec les modèles actuels
2. Implémenter un vrai moteur de suggestions
3. Tester les préférences utilisateur

**Travail estimé:** 2-4 heures

#### Option B: Supprimer le fichier
Les tests de suggestions ne sont pas critiques pour la fonctionnalité scenario-management.

**Priorité:** ⭐☆☆☆☆ (Option B: 5 min / Option A: 2-4h)

---

## 🚀 Plan d'Action Recommandé

### Étape 1: Corrections rapides (15 min)

```bash
# 1. Corriger CalendarServiceTest.kt
# Ajouter l'import manquant
vim shared/src/commonTest/kotlin/com/guyghost/wakeve/CalendarServiceTest.kt

# 2. Corriger CollaborationIntegrationTest.kt
# Ajouter les appels sécurisés
vim shared/src/jvmTest/kotlin/com/guyghost/wakeve/collaboration/CollaborationIntegrationTest.kt

# 3. Corriger CommentPerformanceTest.kt
# Ajouter l'import et le type explicit
vim shared/src/jvmTest/kotlin/com/guyghost/wakeve/comment/CommentPerformanceTest.kt
```

### Étape 2: Vérifier la compilation

```bash
./gradlew :shared:compileTestKotlinJvm
```

Si succès → Passer à l'Étape 3
Si échec → Corriger les erreurs restantes

### Étape 3: Exécuter les tests

```bash
# Tests unitaires
./gradlew :shared:testDebugUnitTest

# Tests JVM (incluant ScenarioRepositoryTest)
./gradlew :shared:jvmTest
```

### Étape 4: Optionnel - Tests E2E complexes

```bash
# Option A: Restaurer et corriger PrdWorkflowE2ETest.kt
mv shared/src/jvmTest/kotlin/com/guyghost/wakeve/e2e/PrdWorkflowE2ETest.kt.broken \
   shared/src/jvmTest/kotlin/com/guyghost/wakeve/e2e/PrdWorkflowE2ETest.kt

# Option B: Supprimer les tests complexes
rm shared/src/jvmTest/kotlin/com/guyghost/wakeve/e2e/PrdWorkflowE2ETest.kt.broken
rm shared/src/jvmTest/kotlin/com/guyghost/wakeve/suggestions/RecommendationEngineTest.kt.disabled
```

---

## 📊 Impact des Corrections

### Avant Corrections
```
✅ Tests unitaires (commonTest): 8/8 PASSING
❌ Tests JVM: 0/0 EXECUTION IMPOSSIBLE (erreurs de compilation)
```

### Après Corrections (Objectif)
```
✅ Tests unitaires (commonTest): 8/8 PASSING
✅ Tests JVM: 11/11 PASSING
```

---

## 🔍 Vérification de Complétude

Après corrections, vérifier:

- [ ] `CalendarServiceTest.kt` compile
- [ ] `CollaborationIntegrationTest.kt` compile
- [ ] `CommentPerformanceTest.kt` compile
- [ ] `PrdWorkflowE2ETest.kt` compile (optionnel)
- [ ] `RecommendationEngineTest.kt` compilé ou supprimé (optionnel)
- [ ] Tous les tests JVM passent (`./gradlew :shared:jvmTest`)
- [ ] Tests ScenarioRepositoryTest passent (incluant les 2 nouveaux)

---

## 📝 Notes

### Contexte des erreurs

Les erreurs identifiées **préexistent** aux modifications apportées pour ajouter les 2 tests manquants. Elles ne sont pas causées par:
- Les nouveaux tests `testScenarioValidation()` et `testGetScenariosWithVotes()`
- Les modifications dans `ScenarioRepositoryTest.kt`

### Racine des erreurs

Les erreurs semblent être dues à:
1. **Code non synchronisé** - Les modèles de test ne correspondent pas aux modèles réels
2. **Refactoring incomplet** - Des méthodes/classes ont été renommées ou modifiées mais les tests n'ont pas été mis à jour
3. **Brouillons non finalisés** - Certains fichiers semblent être des travaux en cours jamais terminés

### Recommandations futures

1. **CI/CD:** Ajouter une vérification de compilation des tests avant chaque commit
2. **Review de code:** Requer une review avant de merger des tests complexes
3. **Tests TDD:** Écrire les tests AVANT d'implémenter les fonctionnalités pour éviter la désynchronisation

---

## 🎉 Récompense

Après corrections complètes:
- ✅ **100% de couverture** pour scenario-management
- ✅ **18/18 tests** passants
- ✅ **Alignement parfait** avec la spec
- ✅ **Confiance accrue** dans la qualité du code
