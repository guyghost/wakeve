import Foundation

struct WakeveAIGroupContext: Equatable, Sendable {
    var groupId: String
    var memberDisplayNames: [String]
}

struct WakeveAIEventContext: Equatable, Sendable {
    var eventId: String
    var title: String
    var date: String?
    var location: String?
    var participantNames: [String]
    var voteSummaries: [String]
    var taskTitles: [String]
    var recentMessages: [String]
}

struct WakeveAIParticipantStatuses: Equatable, Sendable {
    var accepted: [String]
    var pending: [String]
    var declined: [String]
}

struct WakeveAIVoteResults: Equatable, Sendable {
    var activePolls: [String]
    var results: [String]
}

struct WakeveAITransportContext: Equatable, Sendable {
    var proposedTrips: [String]
    var participantNames: [String]
    var schedules: [String]
    var missingDepartureParticipants: [String]
}

struct WakeveAIUserPreferences: Equatable, Sendable {
    var languageCode: String
    var localPreferences: [String]
}

protocol WakeveAIContextProviding: Sendable {
    func currentGroup() async -> WakeveAIGroupContext?
    func eventContext(eventId: String) async -> WakeveAIEventContext?
    func participantStatuses(eventId: String) async -> WakeveAIParticipantStatuses?
    func voteResults(eventId: String) async -> WakeveAIVoteResults?
    func transportContext(eventId: String) async -> WakeveAITransportContext?
    func userPreferences() async -> WakeveAIUserPreferences?
}

struct EmptyWakeveAIContextProvider: WakeveAIContextProviding {
    func currentGroup() async -> WakeveAIGroupContext? { nil }
    func eventContext(eventId: String) async -> WakeveAIEventContext? { nil }
    func participantStatuses(eventId: String) async -> WakeveAIParticipantStatuses? { nil }
    func voteResults(eventId: String) async -> WakeveAIVoteResults? { nil }
    func transportContext(eventId: String) async -> WakeveAITransportContext? { nil }
    func userPreferences() async -> WakeveAIUserPreferences? { nil }
}

protocol WakeveAITool {
    associatedtype Output: Sendable

    var name: String { get }
    var description: String { get }
    func call() async -> Output
}

struct GetCurrentGroupTool: WakeveAITool {
    let name = "get_current_group"
    let description = "Returns visible members for the selected Wakeve group."
    let provider: WakeveAIContextProviding

    func call() async -> WakeveAIGroupContext? {
        await provider.currentGroup()
    }
}

struct GetEventContextTool: WakeveAITool {
    let name = "get_event_context"
    let description = "Returns event facts visible to the current user."
    let eventId: String
    let provider: WakeveAIContextProviding

    func call() async -> WakeveAIEventContext? {
        await provider.eventContext(eventId: eventId)
    }
}

struct GetParticipantStatusesTool: WakeveAITool {
    let name = "get_participant_statuses"
    let description = "Returns accepted, pending, and declined participant statuses."
    let eventId: String
    let provider: WakeveAIContextProviding

    func call() async -> WakeveAIParticipantStatuses? {
        await provider.participantStatuses(eventId: eventId)
    }
}

struct GetVoteResultsTool: WakeveAITool {
    let name = "get_vote_results"
    let description = "Returns active polls and current vote results."
    let eventId: String
    let provider: WakeveAIContextProviding

    func call() async -> WakeveAIVoteResults? {
        await provider.voteResults(eventId: eventId)
    }
}

struct GetTransportContextTool: WakeveAITool {
    let name = "get_transport_context"
    let description = "Returns proposed trips, concerned participants, schedules, and missing departure data."
    let eventId: String
    let provider: WakeveAIContextProviding

    func call() async -> WakeveAITransportContext? {
        await provider.transportContext(eventId: eventId)
    }
}

struct GetUserPreferencesTool: WakeveAITool {
    let name = "get_user_preferences"
    let description = "Returns local user preferences when available."
    let provider: WakeveAIContextProviding

    func call() async -> WakeveAIUserPreferences? {
        await provider.userPreferences()
    }
}

extension WakeveAIEventContext {
    var knownFacts: WakeveAIKnownFacts {
        WakeveAIKnownFacts(
            participantNames: Set(participantNames),
            voteLabels: Set(voteSummaries),
            transportLabels: Set([])
        )
    }

    var promptSummary: String {
        [
            "title: \(title)",
            date.map { "date: \($0)" },
            location.map { "location: \($0)" },
            participantNames.isEmpty ? nil : "participants: \(participantNames.joined(separator: ", "))",
            voteSummaries.isEmpty ? nil : "votes: \(voteSummaries.joined(separator: "; "))",
            taskTitles.isEmpty ? nil : "tasks: \(taskTitles.joined(separator: "; "))",
            recentMessages.isEmpty ? nil : "recent messages: \(recentMessages.prefix(3).joined(separator: "; "))"
        ]
        .compactMap { $0 }
        .joined(separator: "\n")
    }
}

extension WakeveAITransportContext {
    var knownFacts: WakeveAIKnownFacts {
        WakeveAIKnownFacts(
            participantNames: Set(participantNames + missingDepartureParticipants),
            transportLabels: Set(proposedTrips + schedules)
        )
    }

    var promptSummary: String {
        [
            proposedTrips.isEmpty ? nil : "proposed trips: \(proposedTrips.joined(separator: "; "))",
            participantNames.isEmpty ? nil : "participants: \(participantNames.joined(separator: ", "))",
            schedules.isEmpty ? nil : "schedules: \(schedules.joined(separator: "; "))",
            missingDepartureParticipants.isEmpty ? nil : "missing departures: \(missingDepartureParticipants.joined(separator: ", "))"
        ]
        .compactMap { $0 }
        .joined(separator: "\n")
    }
}
