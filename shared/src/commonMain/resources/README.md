# Resources Partagées KMP

Ce répertoire contient les resources partagées entre toutes les plateformes (Android, iOS, Desktop, Web) dans le module Kotlin Multiplatform `shared`.

## Structure

```
shared/src/commonMain/resources/
├── README.md              # Ce fichier
├── images/                # Images et icônes partagées
│   ├── logo.png
│   ├── icons/
│   └── backgrounds/
├── strings/               # Chaînes de caractères (XML)
│   └── strings.xml        # Fichier principal de traduction
├── fonts/                 # Polices de caractères
│   ├── Inter-Regular.ttf
│   └── Inter-Bold.ttf
└── MR/                    # Multiplatform Resources (localization)
    ├── base/              # Chaînes par défaut
    ├── en/                # Anglais
    ├── fr/                # Français
    ├── es/                # Espagnol
    └── de/                # Allemand
```

## Conventions

### Images

- Format recommandé : PNG pour les icônes, WebP pour les photos (meilleure compression)
- Structure : organiser par type (icons/, backgrounds/, illustrations/)
- Nommage : `snake_case` avec préfixe descriptif
  - `ic_` pour les icônes (ex: `ic_calendar.png`)
  - `bg_` pour les arrière-plans (ex: `bg_gradient.png`)
  - `img_` pour les illustrations (ex: `img_onboarding.png`)

### Strings

- Format : XML Android standard
- Séparation logique par sections commentées
- Utiliser des placeholders numérotés : `%1$s`, `%2$d`
- Suivre la structure : `[module]_[action]_[context]`

Exemple :
```xml
<string name="event_create_title">Titre de l'événement</string>
<string name="event_create_description">Description</string>
<string name="poll_vote_submit">Valider mon vote</string>
```

### Fonts

- Formats supportés : TTF, OTF
- Inclure toutes les variantes nécessaires (Regular, Bold, Italic, etc.)
- Nommer clairement : `[Family]-[Weight].[ext]` (ex: `Inter-Bold.ttf`)

### Multiplatform Resources (MR)

Pour une gestion avancée de la localization, utiliser la structure MR avec les fichiers `.strings` ou `.xml` par locale.

Format MR (fichier `MR/base/strings.xml`) :
```xml
<resources>
    <string name="common_ok">OK</string>
    <string name="common_cancel">Cancel</string>
</resources>
```

## Utilisation dans le Code

### Images

```kotlin
// Compose Multiplatform
Image(
    painter = painterResource("images/logo.png"),
    contentDescription = "Logo"
)
```

### Strings

```kotlin
// Avec la librairie de resources (à configurer dans build.gradle.kts)
val appName = stringResource(MR.strings.app_name)

// Ou directement depuis les resources XML
val appName = stringResource(Res.string.app_name)
```

### Fonts

```kotlin
// Dans le Material Theme
val fontFamily = FontFamily(
    Font(resource = "fonts/Inter-Regular.ttf", weight = FontWeight.Normal),
    Font(resource = "fonts/Inter-Bold.ttf", weight = FontWeight.Bold)
)
```

## Guidelines

1. **Prioriser commonMain** : Placez les resources ici si elles sont utilisées sur 2+ plateformes
2. **Platform-specific** : Si une resource n'est utilisée que sur Android, la placer dans `androidMain/res/`
3. **Fallback** : Toujours fournir une version par défaut (base/) pour MR
4. **Optimisation** : Compresser les images avant commit (outils : ImageOptim, Squoosh)
5. **Accessibilité** : Toujours inclure `contentDescription` pour les images

## Configuration Gradle

Pour activer les Multiplatform Resources, ajouter dans `shared/build.gradle.kts` :

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(compose.components.resources)
        }
    }
}

compose.resources {
    packageOfResClass = "com.guyghost.wakeve.resources"
    generateResClass = always
}
```

## Ressources Actuelles

### Chaînes de caractères (`strings/strings.xml`)

| Catégorie | Description |
|-----------|-------------|
| App General | Nom de l'app, tagline |
| Navigation | Labels de navigation |
| Event Actions | Actions sur les événements |
| Poll | Sondage de disponibilité |
| Common Actions | Actions génériques (save, cancel, etc.) |
| Status | États de l'application |
| Formatting | Patterns de formatage |

## Ajouter une Nouvelle Resource

1. Choisir le bon répertoire selon le type
2. Nommer selon les conventions établies
3. Si traduction : ajouter dans tous les dossiers `MR/[locale]/`
4. Mettre à jour ce README si nécessaire
5. Tester sur toutes les plateformes cibles

---

**Note** : Les resources Android existantes restent dans `androidMain/res/`. Ne pas déplacer les resources existantes sans évaluation préalable de l'impact.
