# Specification: Localization Management

**Version**: 1.0
**Date**: January 4, 2026
**Status**: In Development

---

## ADDED Requirements

### Requirement: Multi-Language Support

The system SHALL support multiple languages for the user interface.

#### Scenario: Automatic System Language Detection

**GIVEN** A user installs the application for the first time
**WHEN** The application launches
**THEN** The system language is automatically detected
**AND** The interface displays in that language if supported (FR, EN, ES)
**AND** If the system language is not supported, the interface displays in English (default language)

#### Scenario: Manual Language Change

**GIVEN** A user is connected to the application
**WHEN** He accesses settings
**AND** He selects a different language (for example: Spanish)
**THEN** The interface immediately updates in the selected language
**AND** The choice is saved locally
**AND** The application continues to use this language on next launches

#### Scenario: Language Choice Persistence

**GIVEN** A user has selected Spanish as language
**WHEN** He closes and reopens the application
**THEN** The interface always displays in Spanish
**AND** The language does not revert to system language

---

### Requirement: Fallback System for Missing Translations

The system SHALL provide a fallback mechanism for character strings that are not translated.

#### Scenario: Fallback to English

**GIVEN** A character string is not translated in the selected language (for example: Spanish)
**WHEN** The application attempts to display this string
**THEN** The string from the default language (English) is displayed
**AND** A warning is logged in development to signal the missing translation
**AND** The user does not see a translation key or error

#### Scenario: Missing Translation Warning

**GIVEN** A string has no translation in a non-default language
**WHEN** The developer runs the application in development mode
**THEN** A warning is logged with the missing string key
**AND** The affected language is indicated
**AND** The source file is indicated

---

### Requirement: Language Selector in Settings

The system SHALL allow the user to manually change the language through settings.

#### Scenario: Access to Language Selector

**GIVEN** A user is connected to the application
**WHEN** He accesses settings
**THEN** A "Language" or "Langue" option is visible
**AND** The option displays the currently selected language

#### Scenario: Language Change via Selector

**GIVEN** A user is in the language selection screen
**WHEN** He selects a language among (French, English, Spanish)
**THEN** The interface immediately updates in the selected language
**AND** The language selector displays the new language as selected

#### Scenario: List of Available Languages

**GIVEN** The user is in the language selection screen
**WHEN** He consults the list
**THEN** The following languages are available:
  - French (Français)
  - English
  - Spanish (Español)
**AND** Each language is displayed in its own script (ex: "Français", "English", "Español")

---

### Requirement: Standardized Translation Files

The system SHALL use standardized translation file formats for each platform.

#### Specification Android (strings.xml)

**Format**: Standard Android XML
**Location**: `composeApp/src/androidMain/res/values-{locale}/strings.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Home Screen -->
    <string name="home_title">Accueil</string>
    <string name="home_subtitle">Organisez vos événements facilement</string>

    <!-- Create Event Screen -->
    <string name="create_event">Créer un événement</string>
    <string name="event_title_hint">Titre de l\'événement</string>
    <string name="event_description_hint">Description</string>

    <!-- Buttons -->
    <string name="save">Enregistrer</string>
    <string name="cancel">Annuler</string>
    <string name="delete">Supprimer</string>

    <!-- Error Messages -->
    <string name="error_required_field">Ce champ est requis</string>
    <string name="error_save_failed">Échec de l\'enregistrement</string>
</resources>
```

**Naming Rules**:
- Use snake_case names (ex: `home_title`, `create_event`)
- Group strings by screen with comments
- Prefix error messages with `error_`
- Prefix generic buttons with `save`, `cancel`, `delete`

#### Specification iOS (Localizable.strings)

**Format**: Standard iOS Localizable.strings format
**Location**: `iosApp/iosApp/{locale}.lproj/Localizable.strings`

```
/* Home Screen */
"home_title" = "Accueil";
"home_subtitle" = "Organisez vos événements facilement";

/* Create Event Screen */
"create_event" = "Créer un événement";
"event_title_hint" = "Titre de l'événement";
"event_description_hint" = "Description";

/* Buttons */
"save" = "Enregistrer";
"cancel" = "Annuler";
"delete" = "Supprimer";

/* Error Messages */
"error_required_field" = "Ce champ est requis";
"error_save_failed" = "Échec de l'enregistrement";
```

**Naming Rules**:
- Use the same keys as Android (snake_case)
- Group strings by screen with comments
- Use `/* */` for comments
- Keys are in double quotes
- Values are in double quotes
- End each line with a semicolon

---

### Requirement: KMP Architecture for Localization

The system SHALL use the expect/actual pattern to provide a localization interface in shared code.

#### Interface Common (LocalizationService.kt)

```kotlin
/**
 * Enum of locales supported by the application
 */
enum class AppLocale(val code: String, val displayName: String) {
    FRENCH("fr", "Français"),
    ENGLISH("en", "English"),
    SPANISH("es", "Español");

    companion object {
        fun fromCode(code: String): AppLocale {
            return values().find { it.code == code } ?: ENGLISH
        }
    }
}

/**
 * Localization service (expect interface)
 */
expect class LocalizationService {
    /**
     * Returns the currently selected locale
     */
    fun getCurrentLocale(): AppLocale

    /**
     * Sets the application locale
     */
    fun setLocale(locale: AppLocale)

    /**
     * Returns a translated string for the given key
     * with fallback to English if the string does not exist
     */
    fun getString(key: String): String

    /**
     * Returns a translated string with arguments
     */
    fun getString(key: String, vararg args: Any): String

    companion object {
        /**
         * Returns the LocalizationService singleton instance
         */
        fun getInstance(): LocalizationService
    }
}
```

#### Implementation Android (LocalizationService.android.kt)

```kotlin
actual class LocalizationService(
    private val context: Context
) {

    override fun getCurrentLocale(): AppLocale {
        val locale = context.resources.configuration.locales[0].language
        return AppLocale.fromCode(locale)
    }

    override fun setLocale(locale: AppLocale) {
        val config = Configuration(context.resources.configuration)
        config.setLocale(Locale(locale.code))
        context.resources.updateConfiguration(config, context.resources.displayMetrics)

        // Persist the choice
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .edit()
            .putString("app_locale", locale.code)
            .apply()
    }

    override fun getString(key: String): String {
        return try {
            context.getString(
                context.resources.getIdentifier(key, "string", context.packageName)
            )
        } catch (e: Exception) {
            // Fallback to English
            val config = Configuration(context.resources.configuration)
            config.setLocale(Locale.ENGLISH)
            val resources = Resources(context.assets, context.resources.displayMetrics)
            resources.updateConfiguration(config, context.resources.displayMetrics)
            resources.getString(resources.getIdentifier(key, "string", context.packageName))
        }
    }

    override fun getString(key: String, vararg args: Any): String {
        val format = getString(key)
        return String.format(format, *args)
    }

    actual companion object {
        private var instance: LocalizationService? = null

        fun getInstance(): LocalizationService {
            return instance ?: synchronized(this) {
                instance ?: LocalizationService(context = TODO()).also { instance = it }
            }
        }
    }
}
```

#### Implementation iOS (LocalizationService.ios.kt)

```kotlin
actual class LocalizationService {

    override fun getCurrentLocale(): AppLocale {
        val code = NSLocale.currentLocale.languageCode ?: "en"
        // Check if user has manually set a language
        val userLocale = UserDefaults.standard.stringForKey("app_locale")
        return AppLocale.fromCode(userLocale ?: code)
    }

    override fun setLocale(locale: AppLocale) {
        UserDefaults.standard.set(locale.code, forKey = "app_locale")
        // Note: iOS requires app restart to apply the change
    }

    override fun getString(key: String): String {
        return NSBundle.mainBundle.localizedStringForKey(key, value: null, table: nil).also { result ->
            if (result == key) {
                // Fallback to English
                val enBundle = NSBundle(path: NSBundle.mainBundle.pathForResource("en", ofType: "lproj"))
                enBundle?.localizedStringForKey(key, value: null, table: nil) ?: key
            } else {
                result
            }
        }
    }

    override fun getString(key: String, vararg args: Any): String {
        val format = getString(key)
        return NSString.stringWithFormat(format, args)
    }

    actual companion object {
        private var instance: LocalizationService? = null

        fun getInstance(): LocalizationService {
            return instance ?: synchronized(this) {
                instance ?: LocalizationService().also { instance = it }
            }
        }
    }
}
```

---

### Requirement: Use of Character Strings in UI

The system SHALL provide standardized methods to access translated strings in the UI.

#### Specification Android (Compose)

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

@Composable
fun CreateEventScreen() {
    Column {
        Text(text = stringResource(R.string.create_event))
        TextField(
            value = "",
            onValueChange = {},
            placeholder = { Text(stringResource(R.string.event_title_hint)) }
        )
    }
}
```

#### Specification iOS (SwiftUI)

```swift
import SwiftUI

struct CreateEventView: View {
    var body: some View {
        VStack {
            Text(NSLocalizedString("create_event", comment: "Create event button"))
            TextField("", text: $title)
                .textFieldStyle(.roundedBorder)
                .overlay(
                    Text(NSLocalizedString("event_title_hint", comment: "Event title placeholder"))
                        .foregroundColor(.gray)
                )
        }
    }
}
```

#### Specification KMP (in ViewModels)

```kotlin
class CreateEventViewModel(
    private val localization: LocalizationService
) : ViewModel() {

    fun getButtonTitle(): String {
        return localization.getString("create_event")
    }

    fun getErrorMessage(error: ValidationError): String {
        return when (error) {
            ValidationError.EMPTY_TITLE -> localization.getString("error_empty_title")
            ValidationError.INVALID_DATE -> localization.getString("error_invalid_date")
        }
    }
}
```

---

## MODIFIED Requirements

**No requirements modified** - This is a new feature.

---

## REMOVED Requirements

**No requirements removed** - This is a new feature.

---

## Constraints

### Technical Constraints

- **Kotlin Multiplatform 2.2.20**: Use expect/actual for localization
- **Android**: Use standard resources system (strings.xml)
- **iOS**: Use standard NSBundle system (Localizable.strings)
- **SQLDelight**: Data in the database is not translated
- **Ktor**: API responses are not translated

### UX Constraints

- **English as fallback**: English must always be available as default language
- **Text wrapping**: The UI must support text of different lengths
- **RTL not supported**: Right-to-left language support is not in scope

### Time Constraints

- **Version 1.0**: Support only FR, EN, ES
- **Future**: Add new languages upon request

---

## Wakeve Term Glossary

| Term (FR) | Term (EN) | Term (ES) | Definition |
|-----------|-----------|-----------|------------|
| Événement | Event | Evento | An organized gathering (birthday, wedding, etc.) |
| Sondage | Poll | Encuesta | Voting system to choose a date |
| Créneau | Time Slot | Franja horaria | Time slot proposed for the event |
| Participant | Participant | Participante | Person invited to the event |
| Brouillon | Draft | Borrador | Initial state of an event before publication |
| Scénario | Scenario | Escenario | Detailed plan of a destination/lodging |
| Calendrier | Calendar | Calendario | Integration with native calendar |

---

## Required Tests

### Unit Tests

1. `LocalizationServiceTest.kt`
   - Test `getCurrentLocale()` returns the correct system language
   - Test `setLocale()` changes the language
   - Test `getString()` returns the correct string
   - Test `getString()` falls back to English if string is missing

### UI Tests

1. Android UI Tests
   - Test display in French
   - Test display in English
   - Test display in Spanish
   - Test language selector

2. iOS UI Tests
   - Test display in French
   - Test display in English
   - Test display in Spanish
   - Test language selector

### Integration Tests

1. Test automatic language detection
2. Test language choice persistence
3. Test consistency between Android and iOS

---

## Associated Documentation

- `openspec/changes/add-internationalization/proposal.md` - Detailed proposal
- `openspec/changes/add-internationalization/tasks.md` - Task list
- `docs/guides/developer/INTERNATIONALIZATION_GUIDE.md` - Development guide
- `docs/guides/developer/ADDING_NEW_LANGUAGE.md` - Guide to add a language

---

## Version Notes

### v1.0 (January 4, 2026)
- Specification creation
- Definition of supported languages (FR, EN, ES)
- KMP architecture with expect/actual
- Translation file specifications
- Definition of required tests
