import SwiftUI

// MARK: - Shared Models for Wakeve iOS

/// Simple User model for authentication
public struct User: Identifiable, Codable {
    public let id: String
    public let name: String
    public let email: String
    public let avatarUrl: String?

    var initials: String {
        let components = name.components(separatedBy: " ")
        let first = components.first?.first.map(String.init) ?? ""
        let last = components.last?.first.map(String.init) ?? ""
        return (first + last).uppercased()
    }
}

/// Meal type enum
enum MealType: String, Codable, CaseIterable {
    case breakfast = "BREAKFAST"
    case lunch = "LUNCH"
    case dinner = "DINNER"
    case snack = "SNACK"
    case aperitif = "APERITIF"
}

/// Meal status enum
enum MealStatus: String, Codable, CaseIterable {
    case planned = "PLANNED"
    case assigned = "ASSIGNED"
    case inProgress = "IN_PROGRESS"
    case completed = "COMPLETED"
    case cancelled = "CANCELLED"
}

/// Dietary restriction enum
enum DietaryRestriction: String, Codable, CaseIterable {
    case vegetarian = "VEGETARIAN"
    case vegan = "VEGAN"
    case glutenFree = "GLUTEN_FREE"
    case lactoseIntolerant = "LACTOSE_INTOLERANT"
    case nutAllergy = "NUT_ALLERGY"
    case shellfishAllergy = "SHELLFISH_ALLERGY"
    case kosher = "KOSHER"
    case halal = "HALAL"
    case diabetic = "DIABETIC"
    case other = "OTHER"
}

/// Represents a meal in the event planning
struct MealModel: Identifiable, Codable {
    let id: String
    let eventId: String
    let type: MealType
    let name: String
    let date: String
    let time: String
    let location: String?
    let responsibleParticipantIds: [String]
    let estimatedCost: Int64
    let actualCost: Int64?
    let servings: Int
    let status: MealStatus
    let notes: String?
    let createdAt: String
    let updatedAt: String
}

/// Represents a participant in the event
struct ParticipantModel: Identifiable, Codable {
    let id: String
    let name: String
    let email: String
    let dietaryRestrictions: [DietaryRestrictionModel]
}

/// Represents a dietary restriction
struct DietaryRestrictionModel: Identifiable, Codable {
    let id: String
    let participantId: String
    let eventId: String
    let restriction: DietaryRestriction
    let notes: String?
    let createdAt: String
}

// MARK: - Sample Data for Previews

extension MealModel {
    static var sample: MealModel {
        MealModel(
            id: UUID().uuidString,
            eventId: "event-1",
            type: .lunch,
            name: "Team Lunch",
            date: "2024-12-25",
            time: "12:00",
            location: "Restaurant Central",
            responsibleParticipantIds: [],
            estimatedCost: 4500,
            actualCost: nil,
            servings: 4,
            status: .planned,
            notes: nil,
            createdAt: "2024-12-20T10:00:00Z",
            updatedAt: "2024-12-20T10:00:00Z"
        )
    }
}

extension ParticipantModel {
    static var samples: [ParticipantModel] {
        [
            ParticipantModel(
                id: UUID().uuidString,
                name: "Alice Johnson",
                email: "alice@example.com",
                dietaryRestrictions: [DietaryRestrictionModel.vegetarian]
            ),
            ParticipantModel(
                id: UUID().uuidString,
                name: "Bob Smith",
                email: "bob@example.com",
                dietaryRestrictions: [DietaryRestrictionModel.vegan, DietaryRestrictionModel.glutenFree]
            )
        ]
    }
}

extension DietaryRestrictionModel {
    static var samples: [DietaryRestrictionModel] {
        [
            DietaryRestrictionModel(
                id: UUID().uuidString,
                participantId: "participant-1",
                eventId: "event-1",
                restriction: .vegetarian,
                notes: nil,
                createdAt: "2024-12-20T10:00:00Z"
            ),
            DietaryRestrictionModel(
                id: UUID().uuidString,
                participantId: "participant-2",
                eventId: "event-1",
                restriction: .vegan,
                notes: "No animal products",
                createdAt: "2024-12-20T10:00:00Z"
            ),
            DietaryRestrictionModel(
                id: UUID().uuidString,
                participantId: "participant-3",
                eventId: "event-1",
                restriction: .glutenFree,
                notes: nil,
                createdAt: "2024-12-20T10:00:00Z"
            )
        ]
    }

    static var vegetarian: DietaryRestrictionModel {
        DietaryRestrictionModel(
            id: UUID().uuidString,
            participantId: "participant-1",
            eventId: "event-1",
            restriction: .vegetarian,
            notes: nil,
            createdAt: "2024-12-20T10:00:00Z"
        )
    }

    static var vegan: DietaryRestrictionModel {
        DietaryRestrictionModel(
            id: UUID().uuidString,
            participantId: "participant-2",
            eventId: "event-1",
            restriction: .vegan,
            notes: "No animal products",
            createdAt: "2024-12-20T10:00:00Z"
        )
    }

    static var glutenFree: DietaryRestrictionModel {
        DietaryRestrictionModel(
            id: UUID().uuidString,
            participantId: "participant-3",
            eventId: "event-1",
            restriction: .glutenFree,
            notes: nil,
            createdAt: "2024-12-20T10:00:00Z"
        )
    }
}