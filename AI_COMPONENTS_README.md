# Composants UI AI pour Wakeve

## Résumé de l'implémentation

Ce document décrit les composants UI Jetpack Compose créés pour afficher les badges et suggestions IA dans les écrans de sondage de Wakeve.

## Fichiers créés

### 1. AIBadge.kt (`composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/components/AIBadge.kt`)

Composant principal pour afficher les badges IA avec :

- **AIBadge** - Badge avec icône, label et style Material You
- **CompactAIBadge** - Version compacte pour les espaces limités
- **AIBadgeWithConfidence** - Badge avec indicateur de confiance
- **ConfidenceIndicator** - Barre visuelle de confiance avec pourcentage
- **AIBadgeRow** - Rangée de plusieurs badges

**Features:**
- Couleurs dynamiques Material You
- Animations de 300ms avec easing Cubic
- Accessibilité complète (contentDescription)
- Support des emojis et icônes

### 2. AISuggestionCard.kt (`composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/components/AISuggestionCard.kt`)

Card pour afficher les suggestions IA avec :

- Affichage du créneau horaire recommandé
- Score de participation prédit
- Score global de recommandation
- Reasoning tooltip optionnel
- Boutons Accept/Dismiss

**Features:**
- Material Design 3 avec elevated card
- Animations subtiles
- Actions accessibles
- Colors adaptatifs

### 3. AIRecommendationList.kt (`composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/components/AIRecommendationList.kt`)

Liste scrollable de suggestions IA avec :

- **AIRecommendationList** - Liste principale avec LazyColumn
- **CompactAIRecommendationList** - Version compacte
- États: Loading, Empty, Content
- Header avec résumé des recommandations
- Animations staggerées (délai de 50ms par item)

### 4. AIAnimations.kt (`composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/components/AIAnimations.kt`)

Utilitaires d'animation cohérents :

- **AIBadgeAnimation** - Fade + expand vertical
- **AISlideInAnimation** - Slide depuis le bas
- **AIFadeAnimation** - Fade simple personnalisable
- **AIScaleFadeAnimation** - Scale + fade
- **AIStaggeredAnimation** - Animation avec délai
- **AIPulsingAnimation** - Pulsation pour indicateurs
- **AIHighlightAnimation** - Surbrillance

**Constantes:**
- `AIAnimationDurations.SHORT = 150ms`
- `AIAnimationDurations.MEDIUM = 300ms`
- `AIAnimationDurations.LONG = 450ms`
- `AIEasing.EASE_IN_OUT_CUBIC`

### 5. AIBadgeTest.kt (`composeApp/src/commonTest/kotlin/com/guyghost/wakeve/ui/components/AIBadgeTest.kt`)

Tests Compose (9 tests) :

- `testAIBadgeDisplay` - Affichage du badge
- `testDifferentBadgeTypes` - Types de badges
- `testConfidenceIndicator` - Indicateur de confiance
- `testConfidenceIndicatorColors` - Couleurs de confiance
- `testAISuggestionCardDisplay` - Card de suggestion
- `testAISuggestionCardButtons` - Boutons d'action
- `testAIRecommendationListEmptyState` - État vide
- `testAIRecommendationListWithSuggestions` - Liste avec suggestions
- `testAccessibilityLabels` - Accessibilité

## Utilisation

### Exemple simple

```kotlin
val badge = AIBadge(
    type = AIBadgeType.HIGH_CONFIDENCE,
    displayName = "High Confidence",
    icon = "🎯",
    color = "#4CAF50"
)

AIBadge(badge = badge)
```

### Exemple avec suggestion

```kotlin
val suggestion = AISuggestion(
    id = "sug-1",
    data = DateRecommendation(...),
    metadata = AIMetadata(...),
    badge = badge,
    reasoning = "Based on your preferences"
)

AIRecommendationList(
    suggestions = listOf(suggestion),
    onAcceptRecommendation = { id -> /* accept */ },
    onDismissRecommendation = { id -> /* dismiss */ }
)
```

### Exemple avec animations

```kotlin
AIBadgeAnimation(visible = true) {
    AIBadge(badge = badge)
}

AIStaggeredAnimation(index = 0) {
    AISuggestionCard(suggestion = suggestion)
}
```

## Design System

### Couleurs Material You

Les composants utilisent:
- `MaterialTheme.colorScheme.tertiaryContainer` - Background badges
- `MaterialTheme.colorScheme.secondaryContainer` - Cards suggestions
- `MaterialTheme.colorScheme.primary` - Confiance élevée
- `MaterialTheme.colorScheme.error` - Confiance faible

### Typography

- `MaterialTheme.typography.labelSmall` - Textes de badges
- `MaterialTheme.typography.titleMedium` - Titres de cards
- `MaterialTheme.typography.bodySmall` - Reasonings

### Shapes

- `MaterialTheme.shapes.small` - Badges
- `MaterialTheme.shapes.medium` - Cards

## Accessibilité

Tous les composants incluent:
- `semantics { contentDescription = ... }`
- Labels pour lecteurs d'écran
- Contraste des couleurs Material You
- Tailles minimales respectées (44px touch target)

## Animations

### Durées

| Animation | Duration |
|-----------|----------|
| Fade in | 300ms |
| Expand/Collapse | 300ms |
| Slide | 300ms |
| Stagger delay | 50ms |

### Easing

Standard Material Design 3: `CubicBezier(0.65f, 0f, 0.35f, 1f)`

## Tests

Les tests utilisent `createComposeRule` et vérifient:
- Affichage correct des composants
- Contenu texte présent
- Boutons accessibles
- États vides/chargement

Exécution:
```bash
./gradlew :composeApp:connectedAndroidTest
```
