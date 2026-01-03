# 🔧 Corrections Requises - Phases 1 & 2

**Date**: 2026-01-03  
**Assigné à**: @codegen  
**Priorité**: 🔴 **BLOQUANT** - Empêche la compilation

---

## ⚠️ Erreurs de Compilation à Corriger

### Erreur 1: Type Inference Failed (SQLDelight)

**Fichier**: `shared/src/commonMain/kotlin/com/guyghost/wakeve/suggestions/DatabaseSuggestionPreferencesRepository.kt`

**Lignes**: 226, 247

**Erreur**:
```
Cannot infer type for value parameter 'row'. Specify it explicitly.
Unresolved reference 'user_id', 'suggestion_id', 'interaction_type', 'timestamp', 'metadata'
```

**Cause**: SQLDelight n'a pas généré correctement l'interface pour `suggestion_interactions` table

**Solution**:

```kotlin
// AVANT (ligne 226)
interactionsQueries.selectInteractionsByUserId(userId).executeAsList().map { row ->
    SuggestionInteraction(
        userId = row.user_id,
        suggestionId = row.suggestion_id,
        interactionType = SuggestionInteractionType.valueOf(row.interaction_type),
        timestamp = row.timestamp,
        metadata = decodeStringMap(row.metadata)
    )
}

// APRÈS
interactionsQueries.selectInteractionsByUserId(userId).executeAsList().map { row ->
    SuggestionInteraction(
        userId = row.user_id,
        suggestionId = row.suggestion_id,
        interactionType = SuggestionInteractionType.valueOf(row.interaction_type),
        timestamp = row.timestamp,
        metadata = decodeStringMap(row.metadata)
    )
}
```

**Actions**:
1. Regénérer SQLDelight:
   ```bash
   ./gradlew shared:generateCommonMainWakevDbInterface --rerun-tasks
   ```

2. Vérifier que la table `suggestion_interactions` est correctement générée:
   ```bash
   ls -la shared/build/generated/sqldelight/code/WakevDb/commonMain/com/guyghost/wakeve/
   ```

3. Si l'erreur persiste, ajouter le type explicitement:
   ```kotlin
   .map { row: Suggestion_interactions ->
       SuggestionInteraction(...)
   }
   ```

---

### Erreur 2: Missing Parameter

**Fichier**: `shared/src/commonMain/kotlin/com/guyghost/wakeve/suggestions/DatabaseSuggestionPreferencesRepository.kt`

**Ligne**: 165

**Erreur**:
```
No value passed for parameter 'user_id'
```

**Cause**: Appel de query mal formé

**Solution**:

1. Identifier la ligne 165:
   ```bash
   sed -n '165p' shared/src/commonMain/kotlin/com/guyghost/wakeve/suggestions/DatabaseSuggestionPreferencesRepository.kt
   ```

2. Ajouter le paramètre `user_id` manquant dans l'appel de query

3. Vérifier la signature de la query dans `SuggestionPreferences.sq`:
   ```sql
   -- Ligne correspondante dans SuggestionPreferences.sq
   ```

---

### Erreur 3: Imports Manquants (CORRIGÉ ✅)

**Fichier**: `shared/src/commonMain/kotlin/com/guyghost/wakeve/suggestions/DatabaseSuggestionPreferencesRepository.kt`

**Erreur**:
```
Unresolved reference 'SuggestionInteractionType'
Unresolved reference 'SuggestionInteraction'
```

**Solution Appliquée**:
```kotlin
import com.guyghost.wakeve.models.SuggestionInteractionType
import com.guyghost.wakeve.suggestions.SuggestionInteraction
```

✅ **CORRIGÉ**

---

## 🧪 Tests à Exécuter Après Corrections

### 1. Compilation

```bash
# Nettoyer le build
./gradlew clean

# Regénérer SQLDelight
./gradlew shared:generateCommonMainWakevDbInterface --rerun-tasks

# Compiler shared module
./gradlew shared:compileCommonMainKotlinMetadata

# Si succès, compiler tout
./gradlew shared:build
```

### 2. Tests Unitaires

```bash
# Tests shared
./gradlew shared:jvmTest

# Tests spécifiques
./gradlew shared:jvmTest --tests "DatabaseSuggestionPreferencesRepositoryTest"
./gradlew shared:jvmTest --tests "CommentRepositoryTest"
./gradlew shared:jvmTest --tests "RealTimeChatIntegrationTest"
```

### 3. Tests Android

```bash
# Tests unitaires Android
./gradlew composeApp:test

# Tests instrumentés Android (nécessite émulateur)
./gradlew composeApp:connectedAndroidTest
```

---

## 📋 Checklist de Validation

Avant de considérer les Phases 1 & 2 complètes:

### Compilation
- [ ] `./gradlew shared:generateCommonMainWakevDbInterface --rerun-tasks` succès
- [ ] `./gradlew shared:compileCommonMainKotlinMetadata` succès
- [ ] `./gradlew shared:build` succès
- [ ] Aucun warning de compilation

### Tests
- [ ] `./gradlew shared:jvmTest` succès (60+ tests)
- [ ] CommentRepositoryTest: 20+ tests ✅
- [ ] DatabaseSuggestionPreferencesRepositoryTest: 18 tests ✅
- [ ] RealTimeChatIntegrationTest: tests ✅
- [ ] Navigation tests: tests ✅

### Code Quality
- [ ] Aucun TODO critique non résolu
- [ ] Architecture FC&IS respectée
- [ ] Imports propres et cohérents
- [ ] KDoc pour fonctions publiques

### Documentation
- [ ] SYNTHESIS_PHASES_1_2_COMPLETE.md à jour
- [ ] PHASES_1_2_DELIVERY_SUMMARY.md à jour
- [ ] CORRECTIONS_REQUIRED.md (ce fichier) à jour
- [ ] README.md du projet mis à jour

---

## 🎯 Critères de Succès

Les Phases 1 & 2 seront considérées **COMPLÈTES** quand:

1. ✅ Toutes les erreurs de compilation sont corrigées
2. ✅ Tous les tests passent (60+ tests)
3. ✅ `./gradlew build` réussit sans erreur
4. ✅ Code review approuvé
5. ✅ Documentation complète

---

## 📞 Support

**Questions?** Contacter @synthesizer ou @codegen

**Bugs?** Créer un ticket GitHub Issue avec:
- Titre descriptif
- Erreur complète (stack trace)
- Commandes pour reproduire
- Environnement (OS, Kotlin version, etc.)

---

**Créé par**: @synthesizer  
**Date**: 2026-01-03  
**Dernière mise à jour**: 2026-01-03
