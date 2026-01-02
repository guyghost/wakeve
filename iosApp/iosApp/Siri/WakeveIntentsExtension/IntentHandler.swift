import Intents

/// Intent Handler Extension for SiriKit
/// This extension handles intents when the app is not running
class IntentHandler: INExtension {
    
    override func handler(for intent: INIntent) -> Any {
        return WakeveIntentHandler()
    }
}

/// Main handler for all Wakeve Siri intents
class WakeveIntentHandler: NSObject {
    
    // MARK: - Intent Handling Methods
    
    /// Handle intent to create an event
    func handleCreateEvent(
        title: String?,
        description: String?,
        date: Date?,
        participantCount: Int?,
        completion: @escaping (Bool, String?, String?) -> Void
    ) {
        guard let eventTitle = title, !eventTitle.isEmpty else {
            completion(false, nil, "Le titre de l'événement est requis")
            return
        }
        
        guard let eventDate = date else {
            completion(false, nil, "La date de l'événement est requise")
            return
        }
        
        // Generate event ID
        let eventId = UUID().uuidString
        
        // Save event to shared storage (App Group)
        saveEventLocally(
            id: eventId,
            title: eventTitle,
            description: description ?? "",
            date: eventDate,
            participantCount: participantCount ?? 0
        )
        
        completion(true, eventId, "Événement créé avec succès")
    }
    
    /// Handle intent to add a poll slot
    func handleAddPollSlot(
        eventId: String?,
        slotDate: Date?,
        timeOfDay: String?,
        completion: @escaping (Bool, String?) -> Void
    ) {
        guard let eventIdentifier = eventId, !eventIdentifier.isEmpty else {
            completion(false, "L'événement est requis")
            return
        }
        
        guard let date = slotDate else {
            completion(false, "La date du créneau est requise")
            return
        }
        
        let slotId = UUID().uuidString
        saveSlotLocally(
            id: slotId,
            eventId: eventIdentifier,
            date: date,
            timeOfDay: timeOfDay ?? "ALL_DAY"
        )
        
        completion(true, slotId)
    }
    
    /// Handle intent to confirm poll date
    func handleConfirmPollDate(
        eventId: String?,
        slotId: String?,
        completion: @escaping (Bool) -> Void
    ) {
        guard let eventIdentifier = eventId, !eventIdentifier.isEmpty else {
            completion(false)
            return
        }
        
        guard let slotIdentifier = slotId, !slotIdentifier.isEmpty else {
            completion(false)
            return
        }
        
        // Mark slot as confirmed
        confirmSlot(slotId: slotIdentifier)
        
        // Update event status
        updateEventStatus(eventId: eventIdentifier, status: "CONFIRMED")
        
        completion(true)
    }
    
    /// Handle intent to send invitations
    func handleSendInvitations(
        eventId: String?,
        completion: @escaping (Int?, Bool) -> Void
    ) {
        guard let eventIdentifier = eventId, !eventIdentifier.isEmpty else {
            completion(nil, false)
            return
        }
        
        let participantCount = getParticipantCount(eventId: eventIdentifier)
        completion(participantCount, participantCount > 0)
    }
    
    /// Handle intent to open calendar
    func handleOpenCalendar(completion: @escaping (Bool) -> Void) {
        completion(true)
    }
    
    /// Handle intent to cancel event
    func handleCancelEvent(
        eventId: String?,
        completion: @escaping (Bool) -> Void
    ) {
        if let eventIdentifier = eventId, !eventIdentifier.isEmpty {
            cancelEvent(eventId: eventIdentifier)
        } else {
            cancelLastEvent()
        }
        completion(true)
    }
    
    /// Handle intent to get stats
    func handleGetStats(
        completion: @escaping (Int?, String?) -> Void
    ) {
        let count = getEventCount()
        let response = "Vous avez créé \(count) événement\(count == 1 ? "" : "s")"
        completion(count, response)
    }
    
    // MARK: - Private Storage Helpers
    
    private func getUserDefaults() -> UserDefaults? {
        return UserDefaults(suiteName: "group.com.guyghost.wakeve")
    }
    
    private func saveEventLocally(id: String, title: String, description: String, date: Date, participantCount: Int) {
        let userDefaults = getUserDefaults()
        var events = loadEvents()
        
        let event: [String: Any] = [
            "id": id,
            "title": title,
            "description": description,
            "date": date.timeIntervalSince1970,
            "participantCount": participantCount,
            "status": "DRAFT",
            "createdAt": Date().timeIntervalSince1970
        ]
        
        events.append(event)
        if let data = try? JSONSerialization.data(withJSONObject: events) {
            userDefaults?.set(data, forKey: "events")
        }
        
        userDefaults?.set(id, forKey: "lastCreatedEventId")
    }
    
    private func loadEvents() -> [[String: Any]] {
        let userDefaults = getUserDefaults()
        guard let data = userDefaults?.data(forKey: "events"),
              let events = try? JSONSerialization.jsonObject(with: data) as? [[String: Any]] else {
            return []
        }
        return events
    }
    
    private func saveSlotLocally(id: String, eventId: String, date: Date, timeOfDay: String) {
        let userDefaults = getUserDefaults()
        var slots = loadSlots()
        
        let slot: [String: Any] = [
            "id": id,
            "eventId": eventId,
            "date": date.timeIntervalSince1970,
            "timeOfDay": timeOfDay,
            "isConfirmed": false
        ]
        
        slots.append(slot)
        if let data = try? JSONSerialization.data(withJSONObject: slots) {
            userDefaults?.set(data, forKey: "pollSlots")
        }
    }
    
    private func loadSlots() -> [[String: Any]] {
        let userDefaults = getUserDefaults()
        guard let data = userDefaults?.data(forKey: "pollSlots"),
              let slots = try? JSONSerialization.jsonObject(with: data) as? [[String: Any]] else {
            return []
        }
        return slots
    }
    
    private func confirmSlot(slotId: String) {
        let userDefaults = getUserDefaults()
        var slots = loadSlots()
        
        if let index = slots.firstIndex(where: { ($0["id"] as? String) == slotId }) {
            slots[index]["isConfirmed"] = true
            if let data = try? JSONSerialization.data(withJSONObject: slots) {
                userDefaults?.set(data, forKey: "pollSlots")
            }
        }
    }
    
    private func updateEventStatus(eventId: String, status: String) {
        let userDefaults = getUserDefaults()
        var events = loadEvents()
        
        if let index = events.firstIndex(where: { ($0["id"] as? String) == eventId }) {
            events[index]["status"] = status
            if let data = try? JSONSerialization.data(withJSONObject: events) {
                userDefaults?.set(data, forKey: "events")
            }
        }
    }
    
    private func getParticipantCount(eventId: String) -> Int {
        let events = loadEvents()
        return events.first { ($0["id"] as? String) == eventId }?["participantCount"] as? Int ?? 0
    }
    
    private func getEventCount() -> Int {
        return loadEvents().count
    }
    
    private func cancelEvent(eventId: String) {
        let userDefaults = getUserDefaults()
        var events = loadEvents()
        events.removeAll { ($0["id"] as? String) == eventId }
        if let data = try? JSONSerialization.data(withJSONObject: events) {
            userDefaults?.set(data, forKey: "events")
        }
    }
    
    private func cancelLastEvent() {
        guard let lastEventId = getUserDefaults()?.string(forKey: "lastCreatedEventId") else { return }
        cancelEvent(eventId: lastEventId)
    }
}
