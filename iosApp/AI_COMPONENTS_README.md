# AI Components Index

## Vue d'ensemble

Ce dossier contient les composants UI iOS (SwiftUI) pour afficher les badges et suggestions IA dans les écrans de sondage Wakeve, conformément au design system Liquid Glass.

## Composants créés

### Modèles de données (`Models/`)

| Fichier | Description |
|---------|-------------|
| `AISuggestionModels.swift` | Modèles Swift équivalents aux modèles Kotlin : `AIBadge`, `AIBadgeType`, `AIMetadata`, `AISuggestion<T>`, `DateRecommendation` |

### Composants UI (`Components/`)

| Fichier | Description |
|---------|-------------|
| `AIBadgeView.swift` | Badge IA avec effet Liquid Glass (`.ultraThinMaterial`), support tooltip, animations spring |
| `AISuggestionCardView.swift` | Carte de suggestion IA avec confiance, raisonnement, boutons Accept/Dismiss |
| `AIRecommendationListView.swift` | Liste scrollable de suggestions avec animations staggered |
| `LiquidGlassAnimations.swift` | Animations Liquid Glass réutilisables : `spring`, `fadeIn`, `scale`, etc. |

## Design System Liquid Glass

### Matériaux utilisés

- **Badges** : `.ultraThinMaterial` (subtil, overlay léger)
- **Cartes** : `.regularMaterial` (standard, avec ombre subtile)
- **Coins** : `.continuous` (arrondis fluides)

### Animations

```swift
// Animation standard pour interactions UI
LiquidGlassAnimations.spring      // response: 0.3, dampingFraction: 0.7
LiquidGlassAnimations.fadeIn      // easeOut(duration: 0.25)
LiquidGlassAnimations.scale       // response: 0.25, dampingFraction: 0.6
```

### Helpers disponibles

```swift
// Appliquer style glass card
.view.glassCard(cornerRadius: 16, material: .regularMaterial)

// Animation staggered
.view.staggerAnimation(for: index)

// Effet pulse
.view.pulseEffect(scale: 1.05, duration: 1.0)
```

## Types de badges IA

| Type | Display Name | Icon | Couleur |
|------|--------------|------|---------|
| `AI_SUGGESTION` | AI Suggestion | 🤖 | #6200EE (Purple) |
| `HIGH_CONFIDENCE` | High Confidence | 🎯 | #4CAF50 (Green) |
| `MEDIUM_CONFIDENCE` | Medium Confidence | 📊 | #FF9800 (Orange) |
| `PERSONALIZED` | Personalized | ✨ | #9C27B0 (Purple) |
| `POPULAR_CHOICE` | Popular | 🔥 | #F44336 (Red) |
| `SEASONAL` | Seasonal | 🍂 | #795548 (Brown) |

## Exemples d'utilisation

### Badge simple

```swift
AIBadgeView(type: .highConfidence)
```

### Badge avec tooltip

```swift
AIBadgeView(badge: myBadge, showTooltip: true) {
    showTooltipSheet()
}
```

### Liste de suggestions

```swift
AIRecommendationListView(
    suggestions: suggestions,
    onAccept: { id in acceptSuggestion(id) },
    onDismiss: { id in dismissSuggestion(id) }
)
```

### Carte de suggestion

```swift
AISuggestionCardView(
    suggestion: dateRecommendation,
    metadata: aiMetadata,
    onAccept: { print("Accepted") },
    onDismiss: { print("Dismissed") }
)
```

## Accessibilité

- Tous les composants supportent **Dynamic Type**
- **VoiceOver** : `.accessibilityLabel` et `.accessibilityHint` configurés
- Contraste suffisant pour les modes clair et sombre

## Fichiers concernés

```
iosApp/
├── iosApp/
│   ├── Models/
│   │   └── AISuggestionModels.swift    # 11.7 KB
│   ├── Components/
│   │   ├── AIBadgeView.swift           # 6.8 KB
│   │   ├── AISuggestionCardView.swift  # 9.8 KB
│   │   ├── AIRecommendationListView.swift # 6.5 KB
│   │   └── LiquidGlassAnimations.swift # 11.1 KB
│   └── Extensions/
│       └── ViewExtensions.swift        # Helpers existants
```

## Intégration Kotlin/Native

Ces modèles Swift sont des équivalents temporaires aux modèles Kotlin. Lors de l'intégration Kotlin/Native :

1. Les modèles Kotlin seront exposés via le bridge KMP
2. Les structs Swift pourront être supprimés ou utilisés comme interfaces
3. Les conversions seront gérées automatiquement par le compilateur KMP

## Tests

Prévoir des tests XCTest pour :
- `AIBadgeView_Previews` - Validation des badges
- `AISuggestionCardView_Previews` - Validation des cartes
- `AIRecommendationListView_Previews` - Validation des listes

## Prérequis

- iOS 16+ (SwiftUI)
- Swift 5.9+
- Design System Liquid Glass (déjà implémenté)

## Notes

- Tous les composants utilisent `.continuous` pour les coins arrondis
- Les animations utilisent `spring(response: 0.3, dampingFraction: 0.7)` conformément aux guidelines
- Les couleurs hex sont converties en `SwiftUI.Color` via l'extension `Color(hex:)`
- 3+ previews par composant pour validation
