import Foundation

enum IosRoute: Equatable {
    case topLevel(IosTopLevelRoute)
    case eventCreate
    case event(IosEventRoute)
    case meetingDetail(meetingId: String)
    case invite(token: String)
}

enum IosTopLevelRoute: Equatable {
    case home
    case profile
    case settings
    case notifications(filter: String?)
    case notificationPreferences
    case leaderboard
    case organizerDashboard
}

enum IosEventRoute: Equatable {
    case detail(eventId: String)
    case participants(eventId: String)
    case information(eventId: String)
    case archive(eventId: String)
    case pollVoting(eventId: String)
    case pollResults(eventId: String)
    case scenarioList(eventId: String)
    case scenarioComparison(eventId: String)
    case scenarioManagement(eventId: String)
    case scenarioDetail(eventId: String, scenarioId: String)
    case budgetOverview(eventId: String)
    case budgetDetail(eventId: String, budgetItemId: String)
    case meetingList(eventId: String)
    case comments(eventId: String)
    case invitationShare(eventId: String)
    case transport(eventId: String)
    case accommodation(eventId: String)
    case meals(eventId: String)
    case equipment(eventId: String)
    case activities(eventId: String)
    case payment(eventId: String)
    case tricount(eventId: String)
    case photos(eventId: String)

    static func parse(
        eventId: String,
        components: ArraySlice<String>,
        detailsTab: String?
    ) -> IosEventRoute? {
        guard let destination = components.first else {
            return .detail(eventId: eventId)
        }
        let remainder = components.dropFirst()
        switch destination {
        case "details":
            guard remainder.isEmpty else { return nil }
            switch detailsTab {
            case "comments": return .comments(eventId: eventId)
            case "budget": return .budgetOverview(eventId: eventId)
            case "participants": return .participants(eventId: eventId)
            default: return .detail(eventId: eventId)
            }
        case "participants":
            return remainder.isEmpty ? .participants(eventId: eventId) : nil
        case "information":
            return remainder.isEmpty ? .information(eventId: eventId) : nil
        case "archive":
            return remainder.isEmpty ? .archive(eventId: eventId) : nil
        case "poll":
            if remainder.isEmpty { return .pollVoting(eventId: eventId) }
            return remainder.elementsEqual(["results"]) ? .pollResults(eventId: eventId) : nil
        case "scenarios":
            if remainder.isEmpty { return .scenarioList(eventId: eventId) }
            if remainder.elementsEqual(["compare"]) { return .scenarioComparison(eventId: eventId) }
            return remainder.elementsEqual(["manage"]) ? .scenarioManagement(eventId: eventId) : nil
        case "scenario":
            guard remainder.count == 1, let scenarioId = remainder.first else { return nil }
            return .scenarioDetail(eventId: eventId, scenarioId: scenarioId)
        case "budget":
            guard remainder.count <= 1 else { return nil }
            return remainder.first.map { .budgetDetail(eventId: eventId, budgetItemId: $0) }
                ?? .budgetOverview(eventId: eventId)
        case "meetings": return remainder.isEmpty ? .meetingList(eventId: eventId) : nil
        case "comments": return remainder.isEmpty ? .comments(eventId: eventId) : nil
        case "invite": return remainder.isEmpty ? .invitationShare(eventId: eventId) : nil
        case "transport": return remainder.isEmpty ? .transport(eventId: eventId) : nil
        case "accommodation": return remainder.isEmpty ? .accommodation(eventId: eventId) : nil
        case "meals": return remainder.isEmpty ? .meals(eventId: eventId) : nil
        case "equipment": return remainder.isEmpty ? .equipment(eventId: eventId) : nil
        case "activities": return remainder.isEmpty ? .activities(eventId: eventId) : nil
        case "payment": return remainder.isEmpty ? .payment(eventId: eventId) : nil
        case "tricount": return remainder.isEmpty ? .tricount(eventId: eventId) : nil
        case "photos": return remainder.isEmpty ? .photos(eventId: eventId) : nil
        default: return nil
        }
    }

}
