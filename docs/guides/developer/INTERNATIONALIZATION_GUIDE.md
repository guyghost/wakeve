# Guide d'Internationalisation (Internationalization Guide)

**Version** : 1.0
**Date** : 4 janvier 2026
**Statut** : Production

---

## Table des Matières

1. [Vue d'ensemble](#vue-densemble)
2. [Architecture](#architecture)
3. [Utilisation dans Android (Compose)](#utilisation-dans-android-compose)
4. [Utilisation dans iOS (SwiftUI)](#utilisation-dans-ios-swiftui)
5. [Utilisation dans KMP (Shared)](#utilisation-dans-kmp-shared)
6. [Ajout de nouvelles chaînes](#ajout-de-nouvelles-chaînes)
7. [Ajout de nouvelles langues](#ajout-de-nouvelles-langues)
8. [Bonnes pratiques](#bonnes-pratiques)
9. [Résolution de problèmes](#résolution-de-problèmes)

---

## Vue d'ensemble

Wakeve utilise un système d'internationalisation (i18n) complet qui supporte :
- **Langues** : Français (FR), Anglais (EN), Espagnol (ES)
- **Plateformes** : Android (Compose), iOS (SwiftUI), KMP (Kotlin Multiplatform)
- **Fallback** : Anglais comme langue par défaut
- **Persistance** : Choix de langue sauvegardé localement

### Fichiers Clés

| Fichier | Plateforme | Langue | Localisation |
|---------|-----------|--------|---------------|
| `composeApp/src/androidMain/res/values/strings.xml` | Android | FR (défaut) | `stringResource(R.string.xxx)` |
| `composeApp/src/androidMain/res/values-en/strings.xml` | Android | EN | `stringResource(R.string.xxx)` |
| `composeApp/src/androidMain/res/values-es/strings.xml` | Android | ES | `stringResource(R.string.xxx)` |
| `iosApp/iosApp/fr.lproj/Localizable.strings` | iOS | FR | `NSLocalizedString("xxx", comment: "...")` |
| `iosApp/iosApp/en.lproj/Localizable.strings` | iOS | EN | `NSLocalizedString("xxx", comment: "...")` |
| `iosApp/iosApp/es.lproj/Localizable.strings` | iOS | ES | `NSLocalizedString("xxx", comment: "...")` |

---

## Architecture

### Functional Core (Pure Models)

**Fichier** : `shared/src/commonMain/kotlin/com/guyghost/wakeve/localization/AppLocale.kt`

```kotlin
enum class AppLocale(
    val code: String,
    val displayName: String
) {
    FRENCH("fr", "Français"),
    ENGLISH("en", "English"),
    SPANISH("es", "Español");

    companion object {
        fun fromCode(code: String): AppLocale {
            return values().find { it.code == code } ?: ENGLISH
        }
    }
}
```

**Caractéristiques** :
- ✅ Pure model (pas d'effets de bord)
- ✅ Fonction pure `fromCode()` (déterministe)
- ✅ 100% testable sans mocks
- ✅ Fallback vers ENGLISH par défaut

### Imperative Shell (Service)

**Interface Common** : `shared/src/commonMain/kotlin/com/guyghost/wakeve/localization/LocalizationService.kt`

```kotlin
expect class LocalizationService {
    fun getCurrentLocale(): AppLocale
    fun setLocale(locale: AppLocale)
    fun getString(key: String): String
    fun getString(key: String, vararg args: Any): String

    companion object {
        fun getInstance(): LocalizationService
    }
}
```

**Implémentation Android** : `shared/src/androidMain/kotlin/com/guyghost/wakeve/localization/LocalizationService.android.kt`

- Utilise `SharedPreferences` pour la persistance
- Utilise `Configuration` pour changer la langue
- Utilise `string resources` pour les traductions

**Implémentation iOS** : `shared/src/iosMain/kotlin/com/guyghost/wakeve/localization/LocalizationService.ios.kt`

- Utilise `UserDefaults` pour la persistance
- Utilise `NSBundle` pour les traductions
- Nécessite un redémarrage de l'app pour appliquer le changement

---

## Utilisation dans Android (Compose)

### Importer les ressources

```kotlin
import androidx.compose.ui.res.stringResource
import R // Important: Importer R pour accéder aux ressources
```

### Utilisation de base

```kotlin
@Composable
fun MyScreen() {
    Text(text = stringResource(R.string.save))
    Button(onClick = { /* ... */ }) {
        Text(text = stringResource(R.string.cancel))
    }
}
```

### Utilisation avec placeholders

```kotlin
@Composable
fun UserInfoScreen(name: String) {
    Text(
        text = stringResource(
            id = R.string.welcome_message,
            name
        )
    )
}
```

**Dans strings.xml** :
```xml
<string name="welcome_message">Bonjour, %s !</string>
```

### Utilisation dans les TextField

```kotlin
OutlinedTextField(
    value = text,
    onValueChange = { text = it },
    label = { Text(stringResource(R.string.username)) },
    placeholder = { Text(stringResource(R.string.username_hint)) },
    error = if (hasError) {
        Text(stringResource(R.string.username_error))
    } else null
)
```

### Utilisation dans les boutons

```kotlin
Button(onClick = onSave) {
    Text(stringResource(R.string.save))
}
```

---

## Utilisation dans iOS (SwiftUI)

### Utilisation de base

```swift
struct MyView: View {
    var body: some View {
        VStack {
            Text(NSLocalizedString("save", comment: "Save button"))
            Button(action: { /* ... */ }) {
                Text(NSLocalizedString("cancel", comment: "Cancel button"))
            }
        }
    }
}
```

### Utilisation avec placeholders

```swift
struct WelcomeView: View {
    let name: String
    
    var body: some View {
        Text(
            String(
                format: NSLocalizedString(
                    "welcome_message",
                    comment: "Welcome message with name"
                ),
                name
            )
        )
    }
}
```

**Dans Localizable.strings** :
```
/* Welcome message with user name */
"welcome_message" = "Bonjour, %@ !";
```

### Utilisation dans les TextField

```swift
struct LoginView: View {
    @State private var username = ""
    
    var body: some View {
        TextField(
            NSLocalizedString("username_hint", comment: "Username placeholder"),
            text: $username
        )
        .overlay(
            Text(NSLocalizedString("username", comment: "Username label"))
                .foregroundColor(.gray),
            alignment: .leading
        )
    }
}
```

### Utilisation dans les boutons

```swift
Button(action: onSave) {
    Text(NSLocalizedString("save", comment: "Save button"))
}
```

### Bonnes pratiques iOS

1. **Toujours ajouter un commentaire contextuel** :
   ```swift
   NSLocalizedString("save", comment: "Save changes button")
   ```

2. **Utiliser `comment` pour clarifier l'ambiguïté** :
   ```swift
   // Mauvais
   NSLocalizedString("book", comment: "")
   
   // Bon
   NSLocalizedString("book", comment: "Noun: a book to read")
   NSLocalizedString("book", comment: "Verb: to book a hotel")
   ```

---

## Utilisation dans KMP (Shared)

### Accéder au LocalizationService

```kotlin
class MyViewModel(
    private val localization: LocalizationService
) : ViewModel() {

    fun getLocalizedText(): String {
        return localization.getString("save")
    }

    fun getFormattedMessage(name: String): String {
        return localization.getString("welcome_message", name)
    }

    fun getCurrentLanguage(): AppLocale {
        return localization.getCurrentLocale()
    }
}
```

### Utilisation dans les Use Cases

```kotlin
class CreateEventUseCase(
    private val localization: LocalizationService
) {
    operator fun invoke(event: Event): Result<Unit> {
        // Valider l'événement
        if (event.title.isBlank()) {
            return Result.failure(
                Exception(localization.getString("error_empty_title"))
            )
        }
        // ...
    }
}
```

### Initialisation (Android uniquement)

**IMPORTANT** : Sur Android, vous devez initialiser `LocalizationService` avec le contexte de l'application dans `Application.kt` ou `MainActivity.kt` :

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        LocalizationService.initialize(this)
    }
}
```

---

## Ajout de nouvelles chaînes

### Étape 1: Définir la clé

Choisissez un nom en **snake_case** qui décrit clairement la chaîne :

✅ `save_button` - Spécifique, contextuel
✅ `error_empty_title` - Catégorisé avec préfixe
❌ `text1` - Trop vague
❌ `saveBtn` - Pas en snake_case

### Étape 2: Ajouter aux fichiers Android

Ajoutez la clé dans les 3 fichiers `strings.xml` :

**values/strings.xml** (Français) :
```xml
<!-- New Feature -->
<string name="my_new_feature">Ma nouvelle fonctionnalité</string>
```

**values-en/strings.xml** (Anglais) :
```xml
<!-- New Feature -->
<string name="my_new_feature">My new feature</string>
```

**values-es/strings.xml** (Espagnol) :
```xml
<!-- New Feature -->
<string name="my_new_feature">Mi nueva funcionalidad</string>
```

### Étape 3: Ajouter aux fichiers iOS

Ajoutez la clé dans les 3 fichiers `Localizable.strings` :

**fr.lproj/Localizable.strings** (Français) :
```
/* New Feature - Displayed in the new feature screen */
"my_new_feature" = "Ma nouvelle fonctionnalité";
```

**en.lproj/Localizable.strings** (Anglais) :
```
/* New Feature - Displayed in the new feature screen */
"my_new_feature" = "My new feature";
```

**es.lproj/Localizable.strings** (Espagnol) :
```
/* New Feature - Displayed in the new feature screen */
"my_new_feature" = "Mi nueva funcionalidad";
```

### Étape 4: Utiliser dans le code

**Android (Compose)** :
```kotlin
Text(stringResource(R.string.my_new_feature))
```

**iOS (SwiftUI)** :
```swift
Text(NSLocalizedString("my_new_feature", comment: "New feature label"))
```

### Étape 5: Compiler et tester

```bash
# Android
./gradlew composeApp:assembleDebug

# iOS
open iosApp/iosApp.xcodeproj  # Compiler depuis Xcode
```

---

## Ajout de nouvelles langues

### Prérequis

- Connaissance natif de la langue cible
- Connaissance du contexte de l'application Wakeve
- Accès au glossaire des termes Wakeve

### Étape 1: Créer les dossiers de langue

**Android** :
```bash
mkdir -p composeApp/src/androidMain/res/values-de  # Allemand
mkdir -p composeApp/src/androidMain/res/values-it  # Italien
```

**iOS** :
```bash
mkdir -p iosApp/iosApp/de.lproj  # Allemand
mkdir -p iosApp/iosApp/it.lproj  # Italien
```

### Étape 2: Créer les fichiers de traduction

**Android - values-de/strings.xml** :
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Wakeve</string>
    <string name="save">Speichern</string>
    <string name="cancel">Abbrechen</string>
    <!-- ... toutes les autres chaînes ... -->
</resources>
```

**iOS - de.lproj/Localizable.strings** :
```
/* App */
"app_name" = "Wakeve";

/* General */
"save" = "Speichern";
"cancel" = "Abbrechen";
/* ... toutes les autres chaînes ... */
```

### Étape 3: Ajouter la locale à AppLocale.kt

Mettre à jour `shared/src/commonMain/kotlin/com/guyghost/wakeve/localization/AppLocale.kt` :

```kotlin
enum class AppLocale(
    val code: String,
    val displayName: String
) {
    FRENCH("fr", "Français"),
    ENGLISH("en", "English"),
    SPANISH("es", "Español"),
    GERMAN("de", "Deutsch"),    // Nouveau
    ITALIAN("it", "Italiano");   // Nouveau

    companion object {
        fun fromCode(code: String): AppLocale {
            return values().find { it.code == code } ?: ENGLISH
        }
    }
}
```

### Étape 4: Mettre à jour l'écran de paramètres

**Android - SettingsScreen.kt** :
```kotlin
// La liste AppLocale.values() inclut automatiquement les nouvelles langues
AppLocale.values().forEach { locale ->
    LanguageOption(
        locale = locale,
        isSelected = locale == selectedLocale,
        onClick = { /* ... */ }
    )
}
```

**iOS - SettingsView.swift** :
```swift
// La liste AppLocale.allCases inclut automatiquement les nouvelles langues
ForEach(AppLocale.allCases, id: \.self) { locale in
    LanguageOption(locale: locale, isSelected: locale == selectedLocale)
}
```

### Étape 5: Ajouter l'extension nativeName (iOS)

Mettre à jour `SettingsView.swift` :

```swift
extension AppLocale {
    var nativeName: String {
        switch self {
        case .french:
            return "Français"
        case .english:
            return "English"
        case .spanish:
            return "Español"
        case .german:     // Nouveau
            return "Deutsch"
        case .italian:    // Nouveau
            return "Italiano"
        }
    }
}
```

### Étape 6: Compiler et tester

1. Tester la détection automatique de la nouvelle langue
2. Tester le changement manuel vers la nouvelle langue
3. Tester que toutes les chaînes sont traduites
4. Vérifier la mise en page (text wrapping)
5. Valider avec un natif de la langue

---

## Bonnes Pratiques

### 1. Nommage des clés

✅ **Utiliser snake_case** : `error_empty_title`, `create_event_button`
✅ **Préfixer les catégories** : `error_`, `step_`, `settings_`
✅ **Être descriptif** : `settings_language_title` (pas `language_title`)

❌ **Éviter les abréviations** : `btn_save` (utilisez `save_button`)
❌ **Éviter les nombres génériques** : `text1`, `label2`

### 2. Organisation des chaînes

**Android (strings.xml)** :
```xml
<!-- Groupez par écran/feature -->
<!-- App -->
<string name="app_name">Wakeve</string>

<!-- General -->
<string name="save">Enregistrer</string>
<string name="cancel">Annuler</string>

<!-- Event Creation Wizard -->
<string name="create_event">Créer un événement</string>
<string name="event_title">Titre</string>
```

**iOS (Localizable.strings)** :
```
/* App */
"app_name" = "Wakeve";

/* General */
"save" = "Enregistrer";
"cancel" = "Annuler";

/* Event Creation Wizard */
"create_event" = "Créer un événement";
"event_title" = "Titre";
```

### 3. Commentaires contextuels (iOS)

Toujours ajouter un commentaire pour chaque clé iOS :

```swift
// Mauvais
"book" = "Livre";

// Bon
"book" = "Livre"; /* Noun: a book to read */
"book" = "Réserver"; /* Verb: to book a hotel */
```

### 4. Consistance cross-plateforme

Utilisez les **mêmes clés** sur Android et iOS :

✅ `save_button` sur Android ✅ `save_button` sur iOS
✅ `error_empty_title` sur Android ✅ `error_empty_title` sur iOS

### 5. Traduction professionnelle

- Utilisez des traducteurs natifs si possible
- Validez avec des utilisateurs natifs
- Respectez le glossaire Wakeve
- Testez la mise en page (text wrapping peut varier)

### 6. Tests

- Ajoutez des tests pour chaque nouvelle clé
- Testez les placeholders et le formatting
- Testez le fallback vers l'anglais
- Testez la persistance du choix de langue

---

## Résolution de Problèmes

### Problème 1: La chaîne s'affiche en anglais au lieu de la langue sélectionnée

**Cause** : La clé n'existe pas dans le fichier de traduction de la langue sélectionnée.

**Solution** :
1. Vérifiez que la clé existe dans `values-{locale}/strings.xml` (Android) ou `{locale}.lproj/Localizable.strings` (iOS)
2. Vérifiez que le nom de la clé est correct (snake_case)
3. Vérifiez que vous n'avez pas oublié de traduire la clé

### Problème 2: Le changement de langue ne s'applique pas sur Android

**Cause** : Le contexte n'est pas correctement mis à jour ou l'activité n'est pas recréée.

**Solution** :
```kotlin
// Assurez-vous que Configuration est mise à jour
val config = Configuration(context.resources.configuration)
config.setLocale(Locale(locale.code))
context.resources.updateConfiguration(config, context.resources.displayMetrics)

// Recréez l'activité pour appliquer le changement
recreate()
```

### Problème 3: Le changement de langue ne s'applique pas sur iOS

**Cause** : iOS nécessite un redémarrage de l'application pour appliquer le changement de langue.

**Solution** :
- Informez l'utilisateur que le changement sera appliqué au prochain lancement
- Ou, implémentez un mécanisme de redémarrage automatique

### Problème 4: Les placeholders ne fonctionnent pas

**Cause** : Format incorrect de la chaîne formatée.

**Android** :
```kotlin
// Mauvais
getString("Hello %s", name)  // Utilise %s

// Bon
getString("Hello %s", name)  // Utilise %s
```

**iOS** :
```swift
// Mauvais
NSLocalizedString("hello %d", comment: "")  // Utilise %d mais pas de formatage

// Bon
String(format: NSLocalizedString("hello %@", comment: ""), name)  // Utilise %@
```

### Problème 5: Tests échouent avec "No such resource"

**Cause** : La clé n'existe pas dans le fichier `strings.xml` de test.

**Solution** :
1. Assurez-vous que toutes les clés utilisées dans les tests existent dans `values/strings.xml`
2. Vérifiez que le nom de la clé est correct
3. Vérifiez que `R` est correctement importé

### Problème 6: Le texte est coupé ou mal aligné

**Cause** : La longueur du texte varie selon la langue et dépasse l'espace alloué.

**Solution** :
- Utilisez `Modifier.weight()` pour le layout flexible
- Testez avec des textes longs dans toutes les langues
- Utilisez `maxLines` et `overflow` pour gérer le débordement
```kotlin
Text(
    text = stringResource(R.string.long_text),
    maxLines = 2,
    overflow = TextOverflow.Ellipsis
)
```

---

## Références

- **Specs complètes** : `openspec/specs/localization/spec.md`
- **Tests** : `shared/src/commonTest/kotlin/com/guyghost/wakeve/localization/AppLocaleTest.kt`
- **Architecture** : `skills/shared/architecture.md` (Functional Core & Imperative Shell)
- **Glossaire** : `openspec/specs/localization/spec.md` (Section Glossaire)

---

## Support

Pour toute question ou problème lié à l'internationalisation :
1. Consultez ce guide
2. Vérifiez les specs complètes
3. Consultez les tests existants
4. Contactez l'équipe de développement
