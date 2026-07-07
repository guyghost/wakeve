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

## Gouvernance Liquid Glass

Liquid Glass est un langage de **controle**, pas un conteneur par defaut. Les ecrans Wakeve doivent rester lisibles avec Reduce Transparency, Increase Contrast et Dynamic Type eleve. Cette matrice decide le composant a utiliser avant d'ajouter une nouvelle surface.

| Surface | Composant recommande | Liquid Glass autorise | Raison |
| --- | --- | --- | --- |
| Barre de navigation flottante | `LiquidGlassToolbar`, `WakeveGlassControl` | Oui | Controle persistant, peu de texte, interaction directe |
| Bouton icone flottant | `WakeveCircleButton`, `WakeveGlassControl` | Oui | Cible tactile courte et identifiable |
| CTA bas fixe | `LiquidGlassButton` | Oui si interactif | Action principale; feedback tactile clair |
| Menu contextuel / filtre / FAB | `WakeveGlassControl`, `.liquidGlass` | Oui | Controle court, hors lecture longue |
| Carte evenement hero | `EventHeroCard` opaque/tintee | Non, sauf controle superpose | La lecture du titre et des meta prime |
| Liste longue / row / table | `WakeveGlassCard`, `WakeveListRow`, `Form` natif | Non | Lisibilite, performance et scan repetitif |
| Messages, commentaires, threads | `WakeveGlassCard` opaque/tintee | Non | Contenu social long; confiance et contraste |
| Profil, reglages, preferences | `Form`, `ProfileCard`, `WakeveGlassCard` opaque | Non | Patterns iOS familiers et accessibles |
| Resultats, scores, budgets | `WakeveGlassCard` opaque/tintee | Non | Donnees comparatives, besoin de contraste stable |
| Empty/loading/error states | `EmptyState`, `WakeveGlassCard` opaque | Non | Message explicatif, pas de decoration |

### Regles De Decision

1. Si l'element contient plus de deux lignes de texte, ne pas utiliser Liquid Glass.
2. Si l'element se repete dans une liste scrollable, ne pas utiliser Liquid Glass.
3. Si l'element est un controle icon-only, fournir `accessibilityLabel` et Liquid Glass est autorise.
4. Si l'element est une action principale, Liquid Glass est autorise uniquement si l'etat disabled explique pourquoi l'action est bloquee.
5. Si la surface doit porter une donnee comparee (score, budget, statut, participant), utiliser une surface opaque/tintee.
6. Tester Reduce Transparency et Increase Contrast pour toute nouvelle surface Glass.

### Audit Rapide Avant Merge

- Les contenus longs utilisent `cardFill(for:)` ou un `Form` natif.
- Les controles Glass sont interactifs et ont une cible d'au moins 44 pt.
- Les heros laissent voir au moins une partie du contenu suivant.
- Les overlays n'obscurcissent pas les CTA bas ni les titres.
- Les composants nommes `GlassCard` ne doivent pas appliquer de blur/transparence de type Liquid Glass.

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

### Protocole Dynamic Type

Verifier chaque ecran principal sur un petit iPhone et en taille Accessibility XXL avant de valider un changement de layout.

| Ecran | Points a verifier |
| --- | --- |
| Home | Le bloc "Prochaine action", le hero et le FAB ne se chevauchent pas; le titre reste lisible. |
| Creation | Le CTA bas reste visible; le titre d'etape et les champs ne se superposent pas. |
| Participants | Le hero ne masque pas les sections; le CTA "Lancer le sondage" reste atteignable. |
| Vote | Une seule question reste visible; les options de vote gardent 44 pt minimum. |
| Scenarios | Le hero laisse apparaitre le resume; les boutons de vote ne tronquent pas leur sens. |
| Transport | Le CTA disabled affiche sa raison; les cartes readiness restent scannables. |
| Profil | Les lignes de preferences gardent preference Wakeve et permission iOS lisibles. |

Commandes/simulateur recommandees :

```bash
xcrun simctl ui booted content_size accessibilityExtraExtraExtraLarge
xcrun simctl ui booted appearance dark
```

Definition de passage :

- Aucune action principale n'est masquee par un `safeAreaInset`.
- Aucun texte critique n'est reduit sous `minimumScaleFactor(0.72)`.
- Les titres fixes peuvent passer sur deux lignes quand le mot est long ou localise.
- Les listes restent scrollables sans contenu cache sous le CTA bas.

## Voir Aussi

- [Migration Liquid Glass](../../migration/liquid-glass/README.md)
- [iOS Development Guides](README.md)
