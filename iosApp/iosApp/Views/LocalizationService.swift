import Foundation

/**
 * Supported locales for the Wakeve app.
 */
enum AppLocale: String, CaseIterable {
    case english = "en"
    case french = "fr"
    case spanish = "es"
    
    var displayName: String {
        switch self {
        case .english:
            return NSLocalizedString("language_english", comment: "English language name")
        case .french:
            return NSLocalizedString("language_french", comment: "French language name")
        case .spanish:
            return NSLocalizedString("language_spanish", comment: "Spanish language name")
        }
    }
    
    var nativeName: String {
        switch self {
        case .english:
            return "English"
        case .french:
            return "Français"
        case .spanish:
            return "Español"
        }
    }
}

/**
 * Service for managing app localization.
 * Handles language selection and persistence.
 */
class LocalizationService {
    private let localeKey = "app_locale"
    
    /**
     * Get the currently selected locale.
     * Returns the saved locale or falls back to the system locale.
     */
    func getCurrentLocale() -> AppLocale {
        // Try to get saved locale
        if let savedLocaleString = UserDefaults.standard.string(forKey: localeKey),
           let savedLocale = AppLocale(rawValue: savedLocaleString) {
            return savedLocale
        }
        
        // Fall back to system locale
        let systemLanguageCode = Locale.current.language.languageCode?.identifier ?? "en"
        
        // Map system locale to supported locales
        if systemLanguageCode.hasPrefix("fr") {
            return .french
        } else if systemLanguageCode.hasPrefix("es") {
            return .spanish
        } else {
            return .english
        }
    }
    
    /**
     * Set the app locale and persist the selection.
     * This will trigger a UI update on next launch.
     */
    func setLocale(_ locale: AppLocale) {
        UserDefaults.standard.set(locale.rawValue, forKey: localeKey)
        
        // Set the override language for the app
        UserDefaults.standard.set([locale.rawValue], forKey: "AppleLanguages")
        UserDefaults.standard.synchronize()
        
        // Note: In a production app, you might want to restart the app or
        // use a notification to update all views immediately
    }
}
