# Liquid Glass Design Guidelines

Ce document décrit les meilleures pratiques pour suivre les guidelines Apple sur le design Liquid Glass dans l'application iOS Wakeve.

## Référence Officielle

- [Apple Developer - Adopting Liquid Glass](https://developer.apple.com/documentation/technologyoverviews/adopting-liquid-glass)
- [Human Interface Guidelines - Materials](https://developer.apple.com/design/human-interface-guidelines/materials)

## Principes Clés

### 1. Matériaux (Materials)

Utiliser les matériaux système d'Apple plutôt que des couleurs opaques :

```swift
// ❌ Éviter
.background(Color(.systemBackground))
.background(Color.white)

// ✅ Préférer
.background(.regularMaterial)
.background(.thinMaterial)
.background(.ultraThinMaterial)
```

**Hiérarchie des matériaux** :
- `.ultraThinMaterial` - Très subtil, pour overlays légers
- `.thinMaterial` - Subtil, pour cartes secondaires
- `.regularMaterial` - Standard, pour la plupart des cartes
- `.thickMaterial` - Prononcé, pour cartes importantes
- `.ultraThickMaterial` - Très prononcé, pour modals

### 2. Continuous Corners

Toujours utiliser `.continuous` pour les coins arrondis :

```swift
// ❌ Éviter
.cornerRadius(16)

// ✅ Préférer
.clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
.continuousCornerRadius(16) // Notre helper
```

**Rayons recommandés** :
- Petits éléments (boutons, tags) : 8-12px
- Cartes moyennes : 16-20px
- Grandes cartes : 24-28px
- Modals/Sheets : 28-32px

### 3. Extensions Helper

Nous avons créé des extensions pour simplifier l'application du Liquid Glass :

```swift
// Carte standard avec matériau
.glassCard(cornerRadius: 20, material: .regularMaterial)

// Carte subtile
.thinGlass(cornerRadius: 16)

// Carte très subtile
.ultraThinGlass(cornerRadius: 16)

// Carte prononcée
.thickGlass(cornerRadius: 24)

// Corners continus simples
.continuousCornerRadius(16)
```

### 4. Ombres (Shadows)

Utiliser des ombres légères et naturelles :

```swift
// ❌ Éviter
.shadow(color: .black.opacity(0.3), radius: 20, x: 0, y: 10)

// ✅ Préférer
.shadow(color: .black.opacity(0.05), radius: 8, x: 0, y: 4)
.shadow(color: .black.opacity(0.08), radius: 12, x: 0, y: 6) // Pour grandes cartes
```

### 5. Vibrancy

Les textes et icônes s'adaptent automatiquement au matériau de fond.
Pas besoin d'ajuster manuellement les couleurs.

```swift
VStack {
    Text("Title")
        .font(.headline)
        .foregroundColor(.primary) // S'adapte automatiquement

    Text("Description")
        .foregroundColor(.secondary) // S'adapte automatiquement
}
.glassCard()
```

## Exemples d'Utilisation

### Carte d'Événement

```swift
VStack(spacing: 16) {
    Text("Event Title")
        .font(.title2.bold())

    Text("Description here...")
        .font(.body)
        .foregroundColor(.secondary)
}
.padding(20)
.glassCard(cornerRadius: 20, material: .regularMaterial)
```

### Formulaire

```swift
VStack(spacing: 12) {
    TextField("Name", text: $name)
        .padding(12)
        .background(Color(.tertiarySystemFill))
        .continuousCornerRadius(12)

    TextField("Email", text: $email)
        .padding(12)
        .background(Color(.tertiarySystemFill))
        .continuousCornerRadius(12)
}
.padding(20)
.glassCard(cornerRadius: 20, material: .regularMaterial)
```

### Badge/Status

```swift
HStack {
    Image(systemName: "checkmark")
    Text("Confirmed")
}
.padding(.horizontal, 12)
.padding(.vertical, 6)
.background(.ultraThinMaterial)
.continuousCornerRadius(16)
```

### Modal/Sheet

```swift
VStack(spacing: 20) {
    // Content...
}
.padding(24)
.thickGlass(cornerRadius: 28)
```

## Checklist de Migration

Lors de la migration d'une vue vers Liquid Glass :

- [ ] Remplacer `.background(Color(...))` par `.background(.material)`
- [ ] Remplacer `.cornerRadius()` par `.continuousCornerRadius()` ou `.clipShape()`
- [ ] Utiliser les helpers `.glassCard()`, `.thinGlass()`, etc.
- [ ] Ajuster les ombres pour être plus subtiles
- [ ] Utiliser `.primary` et `.secondary` pour les couleurs de texte
- [ ] Tester sur fond clair et sombre

## Vues Mises à Jour

✅ **Toutes les vues modernisées avec Liquid Glass !**

### Vues d'authentification
- `LoginView` - Background `.ultraThinMaterial` pour logo, corners continus
- `ModernGetStartedView` - Background `.ultraThinMaterial` pour logo

### Vues principales
- `ModernHomeView` - Cards avec `.thinGlass()`, badges `.ultraThinMaterial`, corners continus
- `ModernEventDetailView` - Boutons overlay `.ultraThinMaterial`, badges modernisés
- `ModernEventCreationView` - Cartes `.glassCard()`, inputs `.thinGlass()`

### Vues de gestion
- `ModernPollVotingView` - Cartes de vote `.glassCard()`, corners continus
- `ModernPollResultsView` - Toutes cartes modernisées avec matériaux vitreux
- `ModernParticipantManagementView` - Liste `.glassCard()`, inputs modernes

## Notes Importantes

1. **Gradients colorés** : Les cartes avec des gradients colorés (comme les cartes d'événements) peuvent garder leur design actuel car c'est un choix délibéré pour créer une hiérarchie visuelle forte.

2. **Backgrounds opaques intentionnels** : Certains backgrounds opaques (comme `Color(.tertiarySystemFill)` pour les champs de formulaire) sont intentionnels et suivent les guidelines Apple.

3. **Performance** : Les matériaux vitreux utilisent le GPU pour le blur, donc éviter d'en abuser (max 3-4 niveaux de profondeur).

4. **Accessibilité** : Les matériaux s'adaptent automatiquement aux réglages d'accessibilité (contraste élevé, réduction transparence).

## Ressources

- Extensions: `iosApp/iosApp/Extensions/ViewExtensions.swift`
- Documentation Apple: https://developer.apple.com/documentation/swiftui/material
- HIG Materials: https://developer.apple.com/design/human-interface-guidelines/materials
