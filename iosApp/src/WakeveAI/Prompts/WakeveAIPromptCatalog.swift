import Foundation

struct WakeveAIPrompt: Equatable, Sendable {
    var id: String
    var system: String
    var user: String
}

enum WakeveAIPromptCatalog {
    private static let baseSystem = """
    You are Wakeve's private on-device planning assistant.
    Help users transform vague social plans into structured event suggestions.
    Be concise, warm, practical, and never corporate.
    Never invent participants, availability, votes, addresses, prices, or existing transport facts.
    Return only the requested typed Swift structure.
    Prefer 3 high-quality suggestions over many generic ones.
    """

    static func eventDraft(userInput: String, localeIdentifier: String) -> WakeveAIPrompt {
        WakeveAIPrompt(
            id: "event_draft_v1",
            system: baseSystem,
            user: """
            Locale: \(localeIdentifier)
            Use case: create an EventDraft from this phrase.
            Keep dates as hints if vague. Maximum 3 options per category.
            Phrase: \(userInput)
            """
        )
    }

    static func pollSuggestions(context: String, localeIdentifier: String) -> WakeveAIPrompt {
        WakeveAIPrompt(
            id: "poll_suggestions_v1",
            system: baseSystem,
            user: """
            Locale: \(localeIdentifier)
            Suggest up to 3 practical polls for this Wakeve event context.
            Context: \(context)
            """
        )
    }

    static func checklist(context: String, localeIdentifier: String) -> WakeveAIPrompt {
        WakeveAIPrompt(
            id: "checklist_v1",
            system: baseSystem,
            user: """
            Locale: \(localeIdentifier)
            Suggest a short checklist grouped by food, transport, venue, guests, equipment, and budget.
            Context: \(context)
            """
        )
    }

    static func invitationMessage(context: String, localeIdentifier: String) -> WakeveAIPrompt {
        WakeveAIPrompt(
            id: "invitation_message_v1",
            system: baseSystem,
            user: """
            Locale: \(localeIdentifier)
            Write simple, warm, and short WhatsApp invitation variants.
            Context: \(context)
            """
        )
    }

    static func eventSummary(context: String, localeIdentifier: String) -> WakeveAIPrompt {
        WakeveAIPrompt(
            id: "event_summary_v1",
            system: baseSystem,
            user: """
            Locale: \(localeIdentifier)
            Summarize what is decided, what is missing, and one next action.
            Use only provided context and tool facts.
            Context: \(context)
            """
        )
    }

    static func transportSuggestions(context: String, localeIdentifier: String) -> WakeveAIPrompt {
        WakeveAIPrompt(
            id: "transport_suggestions_v1",
            system: baseSystem,
            user: """
            Locale: \(localeIdentifier)
            Suggest transport coordination next steps and an editable group message draft.
            Use only provided transport facts.
            Context: \(context)
            """
        )
    }
}
