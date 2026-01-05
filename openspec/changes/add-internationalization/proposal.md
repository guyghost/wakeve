# Proposition : Internationalisation (i18n)

**Date** : 4 janvier 2026
**Statut** : Proposition
**Priorité** : Haute

## Contexte

Actuellement, l'application Wakeve ne supporte qu'une seule langue (français). Les chaînes de caractères sont hardcodées dans le code, ce qui rend difficile l'ajout de nouvelles langues et la maintenance.

Pour atteindre un marché international et améliorer l'expérience utilisateur, l'application doit être localisée dans plusieurs langues, en commençant par :
- **Français** (langue actuelle)
- **Anglais** (langue globale)
- **Espagnol** (2ème langue la plus parlée)

## Objectifs

1. **Extraire toutes les chaînes de caractères** du code vers un système de localisation
2. **Implémenter le support multi-langues** pour Android, iOS et KMP
3. **Créer les fichiers de traduction** pour le français, l'anglais et l'espagnol
4. **Ajouter un sélecteur de langue** dans les paramètres de l'application
5. **Détecter automatiquement la langue du système** et utiliser celle-ci par défaut
6. **Documenter le processus** pour ajouter de nouvelles langues

## Périmètre (Scope)

### Inclus

- Extraction de toutes les chaînes hardcodées en UI (Android, iOS)
- Extraction des chaînes dans les composants KMP partagés
- Implémentation du système de localisation KMP avec expect/actual
- Fichiers de traduction pour FR, EN, ES
- Sélecteur de langue dans les paramètres
- Détection automatique de la langue système
- Tests de validation des traductions
- Documentation pour ajouter de futures langues

### Exclus

- Traduction automatique via IA (traductions manuelles uniquement)
- Support RTL (Right-to-Left) pour langues comme l'arabe ou l'hébreu
- Gestion des formats de date/heure localisés (peut être ajouté plus tard)
- Gestion des formats numériques/currency localisés
- A/B testing pour les traductions

## Scénarios Utilisateur

### Scénario 1 : Premier lancement - Détection automatique

**Given** Un utilisateur installe l'application pour la première fois
**When** L'application se lance
**Then** La langue système est détectée automatiquement
**And** L'interface s'affiche dans cette langue si supportée (FR, EN, ES)
**And** Si la langue système n'est pas supportée, l'interface s'affiche en anglais (langue par défaut)

### Scénario 2 : Changement de langue manuel

**Given** Un utilisateur est connecté à l'application
**When** Il accède aux paramètres
**And** Il sélectionne une langue (par exemple : Espagnol)
**Then** L'interface se met à jour immédiatement en espagnol
**And** Ce choix est sauvegardé localement
**And** L'application continue à utiliser cette langue aux prochains lancements

### Scénario 3 : Ajout d'une nouvelle langue (développeur)

**Given** Un développeur veut ajouter le support allemand
**When** Il crée le fichier `strings_de.xml` (Android)
**And** Il crée le fichier `Localizable.strings` (Deutsch) pour iOS
**And** Il ajoute les traductions allemandes
**Then** L'application supporte automatiquement l'allemand
**And** Les utilisateurs avec un système en allemand voient l'interface en allemand

### Scénario 4 : Chaîne manquante - Fallback

**Given** Une chaîne de caractères n'est pas traduite dans la langue sélectionnée
**When** L'application essaie d'afficher cette chaîne
**Then** La chaîne de la langue par défaut (anglais) est affichée
**And** Un warning est logged en développement pour signaler la traduction manquante

## Impact

### Expérience Utilisateur

- **Amélioration** : Utilisateurs francophones, anglophones et hispanophones ont une expérience native
- **Accessibilité** : Meilleure compréhension pour les utilisateurs internationaux
- **Adoption** : Facilite l'adoption de l'application dans de nouveaux marchés

### Implémentation Technique

#### Architecture KMP

```
shared/src/commonMain/kotlin/
└── localization/
    ├── LocalizationService.kt        # Interface expect
    ├── Strings.kt                    # String resources (expect/actual)
    └── models/
        ├── AppLocale.kt              # Enum des locales supportées
        └── LocalizationConfig.kt     # Configuration de localisation

shared/src/androidMain/kotlin/
└── localization/
    └── LocalizationService.android.kt    # Implémentation Android
    └── Strings.android.kt                 # Accès aux string resources Android

shared/src/iosMain/kotlin/
└── localization/
    └── LocalizationService.ios.kt        # Implémentation iOS
    └── Strings.ios.kt                     # Accès aux NSBundle iOS

composeApp/src/androidMain/res/
├── values/
│   └── strings.xml                        # Français (défaut)
├── values-en/
│   └── strings.xml                        # Anglais
└── values-es/
    └── strings.xml                        # Espagnol

iosApp/iosApp/
├── fr.lproj/
│   └── Localizable.strings                # Français
├── en.lproj/
│   └── Localizable.strings                # Anglais
└── es.lproj/
    └── Localizable.strings                # Espagnol
```

#### Fichiers de traduction

**Android (strings.xml)**:
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Wakeve</string>
    <string name="home_title">Accueil</string>
    <string name="create_event">Créer un événement</string>
    <!-- ... -->
</resources>
```

**iOS (Localizable.strings)**:
```
/* Comment */
"app_name" = "Wakeve";
"home_title" = "Accueil";
"create_event" = "Créer un événement";
/* ... */
```

#### Pattern expect/actual pour KMP

```kotlin
// commonMain
expect class LocalizationService {
    fun getCurrentLocale(): AppLocale
    fun setLocale(locale: AppLocale)
    fun getString(key: String): String

    companion object {
        fun getInstance(): LocalizationService
    }
}

// androidMain
actual class LocalizationService(
    private val context: Context
) : LocalizationServiceContract {
    override fun getCurrentLocale(): AppLocale {
        val config = context.resources.configuration
        return when (config.locales[0].language) {
            "fr" -> AppLocale.FRENCH
            "es" -> AppLocale.SPANISH
            else -> AppLocale.ENGLISH
        }
    }

    override fun setLocale(locale: AppLocale) {
        val config = Configuration(context.resources.configuration)
        config.setLocale(Locale(locale.code))
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }

    override fun getString(key: String): String {
        return context.getString(
            context.resources.getIdentifier(key, "string", context.packageName)
        )
    }
}

// iosMain
actual class LocalizationService : LocalizationServiceContract {
    override fun getCurrentLocale(): AppLocale {
        val currentLanguage = NSLocale.currentLocale.languageCode
        return when (currentLanguage) {
            "fr" -> AppLocale.FRENCH
            "es" -> AppLocale.SPANISH
            else -> AppLocale.ENGLISH
        }
    }

    override fun setLocale(locale: AppLocale) {
        UserDefaults.standard.set(locale.code, forKey = "app_locale")
        // Note: iOS nécessite un redémarrage pour appliquer le changement
    }

    override fun getString(key: String): String {
        return NSBundle.mainBundle.localizedStringForKey(key, value: null, table: nil)
    }
}
```

### Code Existant

- Android : Aucun système de localisation implémenté
- iOS : Aucun système de localisation implémenté
- KMP : Aucune interface de localisation

## Design System

### Android (Material You + Jetpack Compose)

Utiliser le pattern standard Compose avec `stringResource()`:

```kotlin
@Composable
fun CreateEventButton(onClick: () -> Unit) {
    Button(onClick = onClick) {
        Text(text = stringResource(R.string.create_event))
    }
}
```

### iOS (Liquid Glass + SwiftUI)

Utiliser le pattern standard SwiftUI avec `NSLocalizedString`:

```swift
struct CreateEventButton: View {
    var body: some View {
        Button(action: {}) {
            Text(NSLocalizedString("create_event", comment: "Button title"))
        }
    }
}
```

### KMP

Accès aux strings depuis le code partagé:

```kotlin
class CreateEventViewModel(
    private val localization: LocalizationService
) {
    fun getButtonTitle(): String {
        return localization.getString("create_event")
    }
}
```

## Livrables

### Tâches d'implémentation

#### Phase 1 : Préparation et Architecture
- [ ] Créer les models `AppLocale` et `LocalizationConfig` dans KMP
- [ ] Créer l'interface `LocalizationService` (expect)
- [ ] Créer les implémentations Android et iOS (actual)

#### Phase 2 : Extraction des chaînes
- [ ] Inventorier toutes les chaînes de caractères dans Android (Compose)
- [ ] Inventorier toutes les chaînes de caractères dans iOS (SwiftUI)
- [ ] Créer les fichiers `strings.xml` de base (FR, EN, ES)
- [ ] Créer les fichiers `Localizable.strings` de base (FR, EN, ES)
- [ ] Extraire et traduire toutes les chaînes UI Android
- [ ] Extraire et traduire toutes les chaînes UI iOS

#### Phase 3 : Intégration
- [ ] Remplacer les chaînes hardcodées par `stringResource()` dans Android
- [ ] Remplacer les chaînes hardcodées par `NSLocalizedString` dans iOS
- [ ] Ajouter la détection automatique de la langue système
- [ ] Créer l'écran de sélection de langue dans les paramètres

#### Phase 4 : Tests
- [ ] Tests unitaires pour `LocalizationService`
- [ ] Tests UI pour vérifier que les chaînes sont correctement affichées
- [ ] Tests de changement de langue manuel
- [ ] Tests de détection automatique de langue
- [ ] Tests de fallback pour chaînes manquantes

#### Phase 5 : Documentation
- [ ] Documenter le processus d'ajout d'une nouvelle langue
- [ ] Mettre à jour le guide de développement
- [ ] Ajouter des guidelines pour la maintenance des traductions

### Tests

- Test de détection automatique de langue (FR/EN/ES)
- Test de changement de langue manuel
- Test de persistance du choix de langue
- Test de fallback pour chaînes manquantes
- Test de toutes les langues sur Android
- Test de toutes les langues sur iOS

## Risques et Mitigations

### Risque 1 : Nombre élevé de chaînes à extraire

**Mitigation** :
- Commencer par les écrans principaux (Home, Create Event, Onboarding)
- Utiliser des scripts d'extraction semi-automatique si possible
- Prioriser les écrans les plus utilisés

### Risque 2 : Qualité des traductions

**Mitigation** :
- Traductions effectuées par des natifs ou outils professionnels
- Validation par des utilisateurs de chaque langue
- Review en pair pour assurer la cohérence

### Risque 3 : Contexte manquant pour certaines traductions

**Mitigation** :
- Ajouter des commentaires contextuels dans les fichiers de traduction
- Documenter les termes spécifiques à l'application
- Créer un glossaire des termes Wakeve

### Risque 4 : Évolution des chaînes de caractères

**Mitigation** :
- Documenter le workflow pour ajouter/modifier des chaînes
- Automatiser la détection de chaînes manquantes
- Intégrer la localisation dans le process de code review

## Success Criteria

✅ L'application supporte le français, l'anglais et l'espagnol
✅ La langue système est détectée automatiquement au premier lancement
✅ L'utilisateur peut changer manuellement la langue dans les paramètres
✅ Le choix de langue est persisté entre les sessions
✅ Toutes les chaînes UI Android sont extraites et traduites
✅ Toutes les chaînes UI iOS sont extraites et traduites
✅ Le système de fallback fonctionne pour les chaînes manquantes
✅ Les tests valident le fonctionnement dans les 3 langues
✅ La documentation est complète pour ajouter de futures langues

## Documentation

- `openspec/specs/localization/spec.md` : Delta des spécifications
- `docs/guides/developer/INTERNATIONALIZATION_GUIDE.md` : Guide de développement i18n
- `docs/guides/developer/ADDING_NEW_LANGUAGE.md` : Guide pour ajouter une langue

## Notes

- L'anglais sera la langue par défaut (fallback)
- Priorité à la qualité des traductions plutôt qu'à la quantité de langues
- L'interface doit être testée dans chaque langue pour vérifier la mise en page (text wrapping)
- Les dates/heures/currency resteront en format système pour l'instant (sera traité dans une future iteration)
- Les termes techniques (comme "draft", "poll", "scenario") peuvent être gardés en anglais ou traduits selon le contexte
