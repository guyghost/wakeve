import Foundation

enum IosRoute: Equatable {
    case topLevel(IosTopLevelRoute)
    case eventCreate
    case event(IosEventRoute)
    case meetingDetail(meetingId: String)
    case invite(token: String)

    var navigationPath: [String] {
        switch self {
        case .topLevel(let route):
            return route.navigationPath
        case .eventCreate:
            return ["event", "create"]
        case .event(let route):
            return route.navigationPath
        case .meetingDetail(let meetingId):
            return ["meeting", meetingId]
        case .invite(let token):
            return ["invite", token]
        }
    }
}

enum IosTopLevelRoute: Equatable {
    case home
    case profile
    case settings
    case notifications(filter: String?)
    case notificationPreferences
    case leaderboard
    case organizerDashboard

    var navigationPath: [String] {
        switch self {
        case .home:
            return ["home"]
        case .profile:
            return ["profile"]
        case .settings:
            return ["settings"]
        case .notifications(let filter):
            return filter == "unread" ? ["notifications", "unread"] : ["notifications"]
        case .notificationPreferences:
            return ["notification_preferences"]
        case .leaderboard:
            return ["leaderboard"]
        case .organizerDashboard:
            return ["organizer_dashboard"]
        }
    }
}

enum IosEventRoute: Equatable {
    case detail(eventId: String)
    case participants(eventId: String)
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

    var navigationPath: [String] {
        switch self {
        case .detail(let eventId):
            return ["event", eventId]
        case .participants(let eventId):
            return ["event", eventId, "participants"]
        case .pollVoting(let eventId):
            return ["event", eventId, "poll"]
        case .pollResults(let eventId):
            return ["event", eventId, "poll_results"]
        case .scenarioList(let eventId):
            return ["event", eventId, "scenarios"]
        case .scenarioComparison(let eventId):
            return ["event", eventId, "scenarios_compare"]
        case .scenarioManagement(let eventId):
            return ["event", eventId, "scenarios_manage"]
        case .scenarioDetail(let eventId, let scenarioId):
            return ["event", eventId, "scenario", scenarioId]
        case .budgetOverview(let eventId):
            return ["event", eventId, "budget"]
        case .budgetDetail(let eventId, let budgetItemId):
            return ["event", eventId, "budget", budgetItemId]
        case .meetingList(let eventId):
            return ["event", eventId, "meetings"]
        case .comments(let eventId):
            return ["event", eventId, "comments"]
        case .invitationShare(let eventId):
            return ["event", eventId, "invite"]
        case .transport(let eventId):
            return ["event", eventId, "transport"]
        case .accommodation(let eventId):
            return ["event", eventId, "accommodation"]
        case .meals(let eventId):
            return ["event", eventId, "meals"]
        case .equipment(let eventId):
            return ["event", eventId, "equipment"]
        case .activities(let eventId):
            return ["event", eventId, "activities"]
        case .payment(let eventId):
            return ["event", eventId, "payment"]
        case .tricount(let eventId):
            return ["event", eventId, "tricount"]
        case .photos(let eventId):
            return ["event", eventId, "photos"]
        }
    }
}
