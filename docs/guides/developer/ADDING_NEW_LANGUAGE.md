# Guide pour Ajouter une Nouvelle Langue (Adding New Language Guide)

**Version** : 1.0
**Date** : 4 janvier 2026
**Statut** : Production

---

## Prérequis

Avant d'ajouter une nouvelle langue à Wakeve, assurez-vous de :

- ✅ **Connaissance natif de la langue** : Vous (ou un traducteur) devez maîtriser la langue cible
- ✅ **Contexte de l'application** : Comprendre les fonctionnalités et le glossaire Wakeve
- ✅ **Accès au glossaire** : Consulter `openspec/specs/localization/spec.md` (Section Glossaire)
- ✅ **Tests** : Prévoir de tester avec un natif de la langue
- ✅ **Documentation** : Mettre à jour la documentation si nécessaire

---

## Étape 1: Choisir la langue

### Liste des langues actuelles

| Code | Nom | Supporté |
|------|-----|----------|
| `fr` | Français | ✅ |
| `en` | English (Anglais) | ✅ |
| `es` | Español (Espagnol) | ✅ |

### Codes de langue ISO 639-1

Voici les codes de langue courants :

| Code | Nom | Anglais |
|------|-----|----------|
| `de` | Deutsch | German (Allemand) |
| `it` | Italiano | Italian (Italien) |
| `pt` | Português | Portuguese (Portugais) |
| `ru` | Русский | Russian (Russe) |
| `zh` | 中文 | Chinese (Chinois) |
| `ja` | 日本語 | Japanese (Japonais) |
| `ko` | 한국어 | Korean (Coréen) |
| `ar` | العربية | Arabic (Arabe) |

**Note** : Pour les langues comme l'arabe (RTL - droite à gauche), des ajustements supplémentaires seront nécessaires (pas supporté dans la version 1.0).

### Exemple : Ajouter l'allemand

Pour ce guide, nous utiliserons **l'allemand** comme exemple.

- **Code ISO** : `de`
- **Nom natif** : Deutsch
- **Nom anglais** : German

---

## Étape 2: Créer les dossiers de langue

### Android

```bash
# Créer le dossier pour l'allemand
mkdir -p composeApp/src/androidMain/res/values-de
```

### iOS

```bash
# Créer le dossier pour l'allemand
mkdir -p iosApp/iosApp/de.lproj
```

**Vérification** :
```bash
# Vérifier que les dossiers sont créés
ls -la composeApp/src/androidMain/res/
ls -la iosApp/iosApp/
```

---

## Étape 3: Créer les fichiers de traduction

### Android - `values-de/strings.xml`

Créer `composeApp/src/androidMain/res/values-de/strings.xml` :

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- App -->
    <string name="app_name">Wakeve</string>

    <!-- General -->
    <string name="save">Speichern</string>
    <string name="cancel">Abbrechen</string>
    <string name="delete">Löschen</string>
    <string name="back">Zurück</string>
    <string name="next">Weiter</string>
    <string name="previous">Vorherige</string>
    <string name="done">Fertig</string>
    <string name="loading">Laden…</string>
    <string name="error">Fehler</string>

    <!-- Event Creation Wizard -->
    <string name="create_event">Veranstaltung erstellen</string>
    <string name="event_title">Veranstaltungstitel</string>
    <string name="event_title_hint">Titel der Veranstaltung eingeben</string>
    <string name="event_description">Beschreibung</string>
    <string name="event_description_hint">Beschreiben Sie Ihre Veranstaltung</string>
    <string name="event_type">Veranstaltungsart</string>
    <string name="event_type_custom">Andere</string>
    <string name="event_type_custom_hint">Veranstaltungsart eingeben</string>

    <!-- Wizard Steps -->
    <string name="step_basic_info">Grundlegende Informationen</string>
    <string name="step_participants">Teilnehmer</string>
    <string name="step_locations">Standorte</string>
    <string name="step_time_slots">Zeitfenster</string>

    <!-- Settings -->
    <string name="settings_title">Einstellungen</string>
    <string name="language_title">Sprache</string>
    <string name="language_description">Wählen Sie die Anwendungssprache</string>

    <!-- ... Traduisez TOUTES les autres chaînes ... -->
</resources>
```

### iOS - `de.lproj/Localizable.strings`

Créer `iosApp/iosApp/de.lproj/Localizable.strings` :

```
/* App */
"app_name" = "Wakeve";

/* General */
"save" = "Speichern";
"cancel" = "Abbrechen";
"delete" = "Löschen";
"back" = "Zurück";
"next" = "Weiter";
"previous" = "Vorherige";
"done" = "Fertig";
"loading" = "Laden…";
"error" = "Fehler";

/* Event Creation Wizard */
"create_event" = "Veranstaltung erstellen";
"event_title" = "Veranstaltungstitel";
"event_title_hint" = "Titel der Veranstaltung eingeben";
"event_description" = "Beschreibung";
"event_description_hint" = "Beschreiben Sie Ihre Veranstaltung";
"event_type" = "Veranstaltungsart";
"event_type_custom" = "Andere";
"event_type_custom_hint" = "Veranstaltungsart eingeben";

/* Wizard Steps */
"step_basic_info" = "Grundlegende Informationen";
"step_participants" = "Teilnehmer";
"step_locations" = "Standorte";
"step_time_slots" = "Zeitfenster";

/* Settings */
"settings_title" = "Einstellungen";
"language_title" = "Sprache";
"language_description" = "Wählen Sie die Anwendungssprache";

/* ... Traduisez TOUTES les autres chaînes ... */
```

---

## Étape 4: Mettre à jour AppLocale.kt

Modifier `shared/src/commonMain/kotlin/com/guyghost/wakeve/localization/AppLocale.kt` :

```kotlin
enum class AppLocale(
    val code: String,
    val displayName: String
) {
    FRENCH("fr", "Français"),
    ENGLISH("en", "English"),
    SPANISH("es", "Español"),
    GERMAN("de", "Deutsch");    // ← Ajouter l'allemand

    companion object {
        fun fromCode(code: String): AppLocale {
            return values().find { it.code == code } ?: ENGLISH
        }
    }
}
```

---

## Étape 5: Mettre à jour l'écran de paramètres

### Android - SettingsScreen.kt

L'écran de paramètres détecte automatiquement la nouvelle langue, car il utilise `AppLocale.values()` :

```kotlin
// Dans SettingsScreen.kt
AppLocale.values().forEach { locale ->
    LanguageOption(
        locale = locale,
        isSelected = locale == selectedLocale,
        onClick = {
            selectedLocale = locale
            localizationService.setLocale(locale)
        }
    )
}
```

**Aucune modification nécessaire** ! L'allemand s'affichera automatiquement.

### iOS - SettingsView.swift

Deux modifications nécessaires :

#### 1. Mettre à jour l'extension `nativeName`

Modifier `SettingsView.swift` :

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
        case .german:     // ← Ajouter l'allemand
            return "Deutsch"
        }
    }
}
```

#### 2. Mettre à jour le case si vous utilisez des couleurs/icônes

Si vous avez des icônes ou couleurs spécifiques par langue :

```swift
struct LanguageOption: View {
    let locale: AppLocale
    let isSelected: Bool
    let onClick: () -> Void

    var localeImage: String {
        switch locale {
        case .french: return "fr.circle"
        case .english: return "en.circle"
        case .spanish: return "es.circle"
        case .german: return "de.circle"  // ← Ajouter l'allemand
        }
    }

    var body: some View {
        Button(action: onClick) {
            HStack {
                Image(systemName: localeImage)
                Text(locale.displayName)
                Spacer()
                if isSelected {
                    Image(systemName: "checkmark.circle.fill")
                }
            }
        }
    }
}
```

---

## Étape 6: Tester la nouvelle langue

### 1. Compilation

**Android** :
```bash
./gradlew composeApp:assembleDebug
```

**iOS** :
```bash
open iosApp/iosApp.xcodeproj
# Puis Compiler depuis Xcode (Cmd+B)
```

### 2. Tests unitaires

Créer des tests pour vérifier que la nouvelle locale fonctionne :

**Créer `shared/src/commonTest/kotlin/com/guyghost/wakeve/localization/AppLocaleTest.kt`** :

```kotlin
@Test
fun `fromCode returns GERMAN for de code`() {
    val result = AppLocale.fromCode("de")
    assertEquals(AppLocale.GERMAN, result)
}

@Test
fun `GERMAN has correct code and displayName`() {
    assertEquals("de", AppLocale.GERMAN.code)
    assertEquals("Deutsch", AppLocale.GERMAN.displayName)
}
```

### 3. Tests UI

**Android** :

Créer `composeApp/src/androidInstrumentedTest/kotlin/com/guyghost/wakeve/ui/screens/SettingsScreenTest.kt` :

```kotlin
@Test
fun `settings screen displays German as option`() {
    composeTestRule.setContent {
        SettingsScreen(onBack = {})
    }

    composeTestRule.onNodeWithText("Deutsch").assertExists()
}

@Test
fun `tapping on German selects it`() {
    composeTestRule.setContent {
        SettingsScreen(onBack = {})
    }

    composeTestRule
        .onNodeWithText("Deutsch")
        .performClick()

    // Vérifier que la sélection est persistée
}
```

**iOS** :

Créer `iosApp/iosApp/Tests/SettingsViewTests.swift` :

```swift
func testLanguageOptionDisplaysGerman() {
    let view = SettingsView()
    // Vérifier que "Deutsch" est affiché
}

func testSelectingGermanUpdatesLocale() {
    let view = SettingsView()
    // Simuler la sélection de l'allemand
    // Vérifier que la locale est mise à jour
}
```

### 4. Tests fonctionnels

1. **Détection automatique** :
   - Changez la langue système de votre appareil en allemand
   - Relancez l'application
   - Vérifiez que l'interface s'affiche en allemand

2. **Sélection manuelle** :
   - Ouvrez l'application
   - Allez dans les paramètres
   - Sélectionnez "Deutsch"
   - Vérifiez que l'interface se met à jour immédiatement (Android) ou au prochain lancement (iOS)
   - Relancez l'application
   - Vérifiez que la sélection est persistée

3. **Mise en page** :
   - Naviguez dans tous les écrans
   - Vérifiez que le texte n'est pas coupé
   - Vérifiez que l'alignement est correct

4. **Fallback** :
   - Ajoutez une nouvelle clé uniquement en allemand
   - Supprimez-la de l'anglais
   - Vérifiez que le fallback fonctionne (affiche en anglais)

### 5. Validation avec un natif

- Demandez à un natif de tester l'application
- Notez les corrections de traduction
- Vérifiez le ton et le style
- Validez le vocabulaire spécifique à l'application

---

## Étape 7: Mettre à jour la documentation

### 1. Mettre à jour ce guide

Ajoutez la nouvelle langue à la liste des langues :

```markdown
| Code | Nom | Supporté |
|------|-----|----------|
| `fr` | Français | ✅ |
| `en` | English (Anglais) | ✅ |
| `es` | Español (Espagnol) | ✅ |
| `de` | Deutsch | ✅ |  ← Ajouter l'allemand
```

### 2. Mettre à jour les specs

Modifier `openspec/specs/localization/spec.md` pour inclure la nouvelle langue dans les exemples.

### 3. Mettre à jour CONTRIBUTING.md

Ajoutez des instructions pour la nouvelle langue.

---

## Étape 8: Pull Request et Code Review

Créez une Pull Request avec :

1. **Commit message** (Conventional Commits) :
   ```
   feat(i18n): add German language support
   
   Adds German (Deutsch) as a supported language:
   - Created values-de/strings.xml
   - Created de.lproj/Localizable.strings
   - Updated AppLocale.kt to include GERMAN
   - Updated SettingsView.swift extension
   - Added tests for German locale
   ```

2. **Fichiers modifiés** :
   - `composeApp/src/androidMain/res/values-de/strings.xml` (nouveau)
   - `iosApp/iosApp/de.lproj/Localizable.strings` (nouveau)
   - `shared/src/commonMain/kotlin/com/guyghost/wakeve/localization/AppLocale.kt`
   - `iosApp/iosApp/Views/SettingsView.swift`
   - Tests ajoutés

3. **Checklist de review** :
   - [ ] Toutes les chaînes sont traduites en allemand
   - [ ] Les tests unitaires passent
   - [ ] Les tests UI passent
   - [ ] La mise en page est correcte
   - [ ] Validé par un natif
   - [ ] La documentation est à jour

---

## Exemple complet : Ajouter l'italien

Voici un exemple complet pour ajouter l'italien :

### 1. Dossiers

```bash
mkdir -p composeApp/src/androidMain/res/values-it
mkdir -p iosApp/iosApp/it.lproj
```

### 2. Fichiers de traduction

**Android - `values-it/strings.xml` :
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Wakeve</string>
    <string name="save">Salva</string>
    <string name="cancel">Annulla</string>
    <!-- ... toutes les autres chaînes ... -->
</resources>
```

**iOS - `it.lproj/Localizable.strings` :
```
/* App */
"app_name" = "Wakeve";

/* General */
"save" = "Salva";
"cancel" = "Annulla";
/* ... toutes les autres chaînes ... */
```

### 3. Mettre à jour AppLocale.kt

```kotlin
enum class AppLocale(
    val code: String,
    val displayName: String
) {
    FRENCH("fr", "Français"),
    ENGLISH("en", "English"),
    SPANISH("es", "Español"),
    GERMAN("de", "Deutsch"),
    ITALIAN("it", "Italiano");    // ← Ajouter l'italien
}
```

### 4. Mettre à jour SettingsView.swift

```swift
extension AppLocale {
    var nativeName: String {
        switch self {
        case .french: return "Français"
        case .english: return "English"
        case .spanish: return "Español"
        case .german: return "Deutsch"
        case .italian: return "Italiano"  // ← Ajouter l'italien
        }
    }
}
```

### 5. Tests

```kotlin
@Test
fun `fromCode returns ITALIAN for it code`() {
    assertEquals(AppLocale.ITALIAN, AppLocale.fromCode("it"))
}
```

---

## Support RTL (Right-to-Left)

Pour les langues comme l'arabe, l'hébreu, le persan, etc. :

### Prérequis supplémentaires

- ✅ Support des layouts RTL dans le code
- ✅ Tests sur appareils RTL
- ✅ Miroir des icônes si nécessaire
- ✅ Ajustement des marges et padding

### Exemple : Arabe

**Code ISO** : `ar`

**Fichiers** :
```bash
mkdir -p composeApp/src/androidMain/res/values-ar
mkdir -p iosApp/iosApp/ar.lproj
```

**Mises à jour nécessaires** :
1. Ajouter `ARABIC("ar", "العربية")` à AppLocale.kt
2. Traduire toutes les chaînes en arabe
3. Configurer le layout RTL dans Android :
   ```kotlin
   // Dans onCreate() de l'activité
   window.decorView.layoutDirection = View.LAYOUT_DIRECTION_RTL
   ```
4. Configurer le layout RTL dans iOS :
   ```swift
   // Dans SceneDelegate
   UIView.appearance().semanticContentAttribute = .forceRightToLeft
   ```

**Note** : Le support RTL n'est pas inclus dans la version 1.0 de Wakeve. Ce sera implémenté dans une future version.

---

## Checklist finale

Avant de considérer la nouvelle langue comme terminée :

- [ ] Tous les dossiers de langue sont créés (Android + iOS)
- [ ] Tous les fichiers de traduction sont créés (TOUTES les chaînes)
- [ ] AppLocale.kt est mis à jour avec la nouvelle locale
- [ ] SettingsView.swift (Android) est mis à jour si nécessaire
- [ ] SettingsView.swift (iOS) est mis à jour (extension nativeName)
- [ ] Les tests unitaires sont créés et passent
- [ ] Les tests UI sont créés et passent
- [ ] La détection automatique de langue fonctionne
- [ ] La sélection manuelle de langue fonctionne
- [ ] La persistance du choix de langue fonctionne
- [ ] La mise en page est correcte dans tous les écrans
- [ ] Validé par un natif de la langue
- [ ] La documentation est à jour
- [ ] Une Pull Request est créée et passée en review

---

## Références

- **Guide d'internationalisation** : `docs/guides/developer/INTERNATIONALIZATION_GUIDE.md`
- **Specs** : `openspec/specs/localization/spec.md`
- **Tests existants** : `shared/src/commonTest/kotlin/com/guyghost/wakeve/localization/AppLocaleTest.kt`
- **Glossaire Wakeve** : `openspec/specs/localization/spec.md` (Section Glossaire)

---

## Support

Pour toute question ou problème lors de l'ajout d'une nouvelle langue :

1. Consultez ce guide
2. Consultez le guide d'internationalisation
3. Vérifiez les exemples de langues existantes (FR, EN, ES)
4. Contactez l'équipe de développement
