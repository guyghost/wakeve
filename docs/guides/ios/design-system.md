# iOS Design System

[← Retour](README.md)

Ce guide définit le design system iOS de Wakeve. Il s'inspire des écrans de référence fournis : permission/notification sombre, recherche d'amis, profil immersif, invitation événementielle et aperçu détaillé.

## Principes

- **Immersion d'abord** : les parcours événementiels utilisent des fonds pleine page, des gradients expressifs et des surfaces lisibles.
- **Hiérarchie forte** : les écrans clés ont de grands titres SF Pro bold, des métadonnées muted et des actions capsules.
- **Géométrie cohérente** : contrôles circulaires, boutons capsules, cartes continues à grands rayons, listes groupées.
- **Système avant local** : ne pas recréer de couleurs, rayons, typographies ou boutons dans une vue.
- **Glass réservé aux contrôles** : Liquid Glass est réservé aux boutons flottants, barres de navigation et contrôles contextuels. Les cartes de contenu restent opaques/tintées.
- **Accessibilité native** : les surfaces doivent rester lisibles avec Reduce Transparency, contraste élevé, VoiceOver et Dynamic Type.

## Tokens

Les tokens vivent dans `iosApp/src/Theme/DesignSystem.swift`, namespace `WakeveTheme`.

| Token | Usage |
|-------|-------|
| `WakeveTheme.ColorToken.appDark` | Fond sombre principal des écrans immersifs |
| `WakeveTheme.ColorToken.eventNight` | Base des cartes d'invitation et d'événement |
| `WakeveTheme.ColorToken.permissionBlue` | Action principale des parcours utilitaires |
| `WakeveTheme.ColorToken.eventLilacAction` | Bouton "Suivant" sur fonds événementiels violet/bleu |
| `WakeveTheme.ColorToken.cardFill(for:)` | Remplissage standard des cartes |
| `WakeveTheme.ColorToken.cardBorder(for:)` | Bordure standard des cartes |
| `WakeveTheme.EventGradient.invitation` | Fond des invitations et previews événement |
| `WakeveTheme.EventGradient.profile` | Fond profil chaleureux |

## Typographie

| Token | Usage |
|-------|-------|
| `display` | Titre événement très visible |
| `hero` | Titre héro secondaire |
| `largeTitle` | Titre principal d'écran |
| `section` | Sections comme "Amis", "Vue d'ensemble", "Suggestions" |
| `rowTitle` | Texte principal de row |
| `bodySemibold` | Boutons et labels forts |
| `metadata` | Sous-titres, statuts, compteurs |

## Composants

Les composants vivent dans `iosApp/src/Components/DesignSystem/`.

### `WakeveScreenBackground`

Utiliser pour les fonds de page :

```swift
ZStack {
    WakeveScreenBackground(style: .event)
    content
}
```

Styles disponibles : `.app`, `.utility`, `.profile`, `.event`, `.grouped`.

### `WakeveGlassCard`

Surface standard pour cartes, groupes et panneaux de contenu. Malgré son nom historique, ce composant ne doit pas appliquer Liquid Glass au contenu.

```swift
WakeveGlassCard(cornerRadius: WakeveTheme.Radius.xl) {
    Text("Contenu")
}
```

Le composant applique une surface stable avec bordure et ombre légère.

### `WakeveGlassControl`

Surface Liquid Glass pour contrôles flottants uniquement.

```swift
WakeveGlassControl {
    Image(systemName: "ellipsis")
        .frame(width: 48, height: 48)
}
```

### `WakeveEventPanel`

Surface sombre pour les panneaux de contenu sur fonds événementiels immersifs.

```swift
WakeveEventPanel {
    Text("Détails de l'événement")
}
```

### `WakeveActionButton`

Bouton capsule pour actions principales.

```swift
WakeveActionButton("Continuer", variant: .primary) {
    continueFlow()
}
```

Variants : `.primary`, `.secondary`, `.neutral`, `.eventNext`, `.destructive`.

### `WakeveCircleButton`

Contrôle circulaire pour retour, fermeture, confirmation et menu.

```swift
WakeveCircleButton(
    systemImage: "chevron.left",
    accessibilityLabel: "Retour",
    variant: .eventBack
) {
    dismiss()
}
```

### `WakeveSearchField`

Champ de recherche arrondi avec icône de recherche et action optionnelle.

```swift
WakeveSearchField(
    placeholder: "Trouver des contacts",
    text: $query,
    trailingSystemImage: "mic"
)
```

### `WakeveListRow`

Row standard avec leading, titre, sous-titre et trailing.

```swift
WakeveListRow(
    title: "Ajouter des amis",
    subtitle: "15 suggestions d'amis",
    leading: { Image(systemName: "person.badge.plus") },
    trailing: { Image(systemName: "chevron.right") }
)
```

### `WakeveSegmentedVoteControl`

Contrôle RSVP Oui/Non/Peut-être pour les sondages.

```swift
WakeveSegmentedVoteControl(selectedVote: vote) { newVote in
    vote = newVote
}
```

## Règles D'utilisation

- Utiliser `WakeveTheme` pour toute couleur, typo, spacing ou radius ajouté.
- Utiliser `WakeveGlassCard` pour les cartes au lieu de `.background(...).cornerRadius(...)` local.
- Utiliser `WakeveEventPanel` pour les panneaux de contenu placés sur les gradients événementiels.
- Utiliser `WakeveGlassControl`, `.liquidGlass`, `.thinGlass` ou `.ultraThinGlass` uniquement pour des contrôles et barres flottantes.
- Utiliser `WakeveActionButton` pour les actions principales ou destructives.
- Utiliser `WakeveCircleButton` pour les boutons icône-only et toujours fournir un `accessibilityLabel`.
- Réserver les gradients très saturés aux écrans événementiels et profil.
- Garder les écrans de réglages proches des contrôles natifs, mais appliquer les tokens d'accent et de fond.

## Migration D'une Vue

1. Remplacer le fond local par `WakeveScreenBackground`.
2. Remplacer les cartes locales par `WakeveGlassCard`.
3. Remplacer les boutons primaires par `WakeveActionButton`.
4. Remplacer les boutons icônes par `WakeveCircleButton`.
5. Remplacer les couleurs et fontes hardcodées par `WakeveTheme`.
6. Vérifier Dynamic Type, VoiceOver, contraste élevé et Reduce Transparency.

## Accessibilité

- Hit target minimum : 44 pt.
- Ne pas transmettre une information uniquement par couleur.
- Les labels icon-only sont obligatoires.
- Les titres larges doivent avoir une stratégie de retour : `lineLimit`, `minimumScaleFactor` ou mise en page verticale.
- Les surfaces Liquid Glass doivent rester lisibles sans transparence et ne doivent jamais être le seul conteneur d'un contenu long.

## Voir Aussi

- [Migration Liquid Glass](../../migration/liquid-glass/README.md)
- [iOS Development Guides](README.md)
