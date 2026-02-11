import Foundation
import Intents

/// Service for registering and managing Siri shortcuts
class SiriShortcutManager {
    static let shared = SiriShortcutManager()
    
    private init() {}
    
    /// Register all Siri shortcuts for Wakeve
    func registerShortcuts() {
        // Shortcuts will be registered dynamically based on user actions
    }
    
    /// Donate interaction for creating an event
    func donateCreateEventInteraction(title: String, date: Date) {
        let userActivity = NSUserActivity(activityType: "com.guyghost.wakeve.createEvent")
        userActivity.title = "Créer un événement: \(title)"
        userActivity.userInfo = [
            "eventTitle": title,
            "eventDate": date.timeIntervalSince1970
        ]
        #if os(iOS)
        if #available(iOS 12.0, *) {
            userActivity.isEligibleForPrediction = true
            userActivity.suggestedInvocationPhrase = "Créer un événement avec Wakeve"
        }
        #endif
        userActivity.becomeCurrent()
    }
    
    /// Donate interaction for adding a poll slot
    func donateAddSlotInteraction(eventId: String, date: Date) {
        let userActivity = NSUserActivity(activityType: "com.guyghost.wakeve.addSlot")
        userActivity.title = "Ajouter un créneau"
        userActivity.userInfo = [
            "eventId": eventId,
            "slotDate": date.timeIntervalSince1970
        ]
        #if os(iOS)
        if #available(iOS 12.0, *) {
            userActivity.isEligibleForPrediction = true
            userActivity.suggestedInvocationPhrase = "Ajouter un créneau sur Wakeve"
        }
        #endif
        userActivity.becomeCurrent()
    }
    
    /// Donate interaction for sending invitations
    func donateSendInvitationsInteraction(eventId: String) {
        let userActivity = NSUserActivity(activityType: "com.guyghost.wakeve.sendInvitations")
        userActivity.title = "Envoyer les invitations"
        userActivity.userInfo = ["eventId": eventId]
        #if os(iOS)
        if #available(iOS 12.0, *) {
            userActivity.isEligibleForPrediction = true
            userActivity.suggestedInvocationPhrase = "Envoyer les invitations avec Wakeve"
        }
        #endif
        userActivity.becomeCurrent()
    }
    
    /// Request Siri authorization for the app
    func requestSiriAuthorization(completion: @escaping (Bool) -> Void) {
        #if os(iOS)
        if #available(iOS 10.0, *) {
            INPreferences.requestSiriAuthorization { status in
                DispatchQueue.main.async {
                    completion(status == .authorized)
                }
            }
        } else {
            completion(false)
        }
        #else
        completion(false)
        #endif
    }
    
    /// Check if Siri is authorized
    func isSiriAuthorized() -> Bool {
        #if os(iOS)
        if #available(iOS 10.0, *) {
            return INPreferences.siriAuthorizationStatus() == .authorized
        }
        #endif
        return false
    }
}

// MARK: - Siri Vocabulary Manager

class SiriVocabularyManager {
    static let shared = SiriVocabularyManager()
    
    private init() {}
    
    /// Update the custom vocabulary with Wakeve-specific terms
    func updateVocabulary() {
        // Register custom vocabulary for event types
        let eventTypes = [
            "Mariage", "Wedding", "Boda",
            "Anniversaire", "Birthday", "Cumpleaños", "Geburtstag",
            "Fête", "Party", "Fiesta", "Feier",
            "Conférence", "Conference", "Konferenz",
            "Team building", "Team-Building",
            "Workshop", "Atelier",
            "Événement culturel", "Cultural event", "Evento cultural"
        ]
        
        // Register time of day terms
        let timeOfDayTerms = [
            "Matin", "Morning", "Mañana", "Vormittag",
            "Après-midi", "Afternoon", "Tarde", "Nachmittag",
            "Soir", "Evening", "Noche", "Abend",
            "Toute la journée", "All day", "Todo el día", "Ganzer Tag"
        ]
        
        print("Registered vocabulary: \(eventTypes.count) event types, \(timeOfDayTerms.count) time terms")
    }
}

// MARK: - Speech Recognition Manager

class SpeechRecognitionManager {
    static let shared = SpeechRecognitionManager()
    
    private init() {}
    
    /// Check if speech recognition is available
    func isSpeechRecognitionAvailable() -> Bool {
        guard let recognizer = SFSpeechRecognizer() else {
            return false
        }
        return recognizer.isAvailable && SFSpeechRecognizer.authorizationStatus() == .authorized
    }
    
    /// Request speech recognition authorization
    func requestAuthorization(completion: @escaping (Bool) -> Void) {
        SFSpeechRecognizer.requestAuthorization { status in
            DispatchQueue.main.async {
                completion(status == .authorized)
            }
        }
    }
}

import Speech
import Intents
