import Foundation
import Shared

#if DEBUG

enum EventFactory {

    private static let iso8601 = ISO8601DateFormatter()
    private static var now: String { iso8601.string(from: Date()) }

    private static func dateString(daysFromNow days: Int, hour: Int = 10) -> String {
        var components = DateComponents()
        components.day = days
        components.hour = hour - Calendar.current.component(.hour, from: Date())
        let date = Calendar.current.date(byAdding: components, to: Date()) ?? Date()
        return iso8601.string(from: date)
    }

    private static func slot(
        id: String,
        start: String?,
        end: String?,
        timezone: String = "Europe/Paris",
        timeOfDay: TimeOfDay = .specific
    ) -> TimeSlot_ {
        TimeSlot_(id: id, start: start, end: end, timezone: timezone, timeOfDay: timeOfDay)
    }

    static var empty: Event_ {
        make(
            id: "event-empty-001",
            title: "",
            description: "",
            status: .draft,
            eventType: Shared.EventType.other
        )
    }

    static var complete: Event_ {
        make(
            id: "event-complete-002",
            title: "Weekend Hiking Trip",
            description: "Join us for a scenic hike through the Calanques National Park followed by a picnic.",
            participants: UserFactory.group(count: 5).map(\.id),
            proposedSlots: sampleTimeSlots,
            deadline: dateString(daysFromNow: 3),
            status: .confirmed,
            finalDate: dateString(daysFromNow: 14, hour: 9),
            createdAt: dateString(daysFromNow: -7),
            eventType: Shared.EventType.outdoorActivity,
            minParticipants: 3,
            maxParticipants: 20,
            expectedParticipants: 8,
            heroImageUrl: "https://images.unsplash.com/photo-1551632811-561732d1e306?w=800"
        )
    }

    static var past: Event_ {
        make(
            id: "event-past-003",
            title: "Team Offsite 2025",
            description: "Annual team building weekend in the Alps.",
            participants: UserFactory.group(count: 4).map(\.id),
            deadline: dateString(daysFromNow: -30),
            status: .finalized,
            finalDate: dateString(daysFromNow: -14, hour: 10),
            createdAt: dateString(daysFromNow: -60),
            updatedAt: dateString(daysFromNow: -14),
            eventType: Shared.EventType.teamBuilding,
            minParticipants: 4,
            expectedParticipants: 6
        )
    }

    static var polling: Event_ {
        make(
            id: "event-polling-004",
            title: "Birthday Dinner",
            description: "Celebrating Emma's birthday! Vote for the best date.",
            participants: UserFactory.group(count: 3).map(\.id),
            proposedSlots: [
                slot(id: "slot-poll-1", start: dateString(daysFromNow: 7, hour: 19), end: dateString(daysFromNow: 7, hour: 22), timeOfDay: .evening),
                slot(id: "slot-poll-2", start: dateString(daysFromNow: 8, hour: 12), end: dateString(daysFromNow: 8, hour: 14), timeOfDay: .afternoon),
                slot(id: "slot-poll-3", start: nil, end: nil, timeOfDay: .morning)
            ],
            deadline: dateString(daysFromNow: 5),
            status: .polling,
            createdAt: dateString(daysFromNow: -2),
            eventType: Shared.EventType.birthday,
            minParticipants: 5,
            maxParticipants: 15,
            expectedParticipants: 10
        )
    }

    static var withManyParticipants: Event_ {
        make(
            id: "event-crowd-005",
            title: "Company Summer Party",
            description: "End-of-summer celebration with BBQ, games, and live music at Parc Borély.",
            participants: (0..<18).map { "user-crowd-\($0)" },
            deadline: dateString(daysFromNow: 10),
            status: .confirmed,
            finalDate: dateString(daysFromNow: 21, hour: 16),
            createdAt: dateString(daysFromNow: -14),
            eventType: Shared.EventType.party,
            minParticipants: 10,
            maxParticipants: 50,
            expectedParticipants: 25,
            heroImageUrl: "https://images.unsplash.com/photo-1533174072545-7a4b6ad7a6c3?w=800"
        )
    }

    static func make(
        id: String = "event-\(UUID().uuidString.prefix(8))",
        title: String = "Test Event",
        description: String = "A test event for preview purposes.",
        organizerId: String = UserFactory.organizer.id,
        participants: [String] = [],
        proposedSlots: [TimeSlot_] = [],
        deadline: String? = nil,
        status: EventStatus = .draft,
        finalDate: String? = nil,
        createdAt: String? = nil,
        updatedAt: String? = nil,
        eventType: Shared.EventType = Shared.EventType.other,
        eventTypeCustom: String? = nil,
        minParticipants: Int32? = nil,
        maxParticipants: Int32? = nil,
        expectedParticipants: Int32? = nil,
        heroImageUrl: String? = nil
    ) -> Event_ {
        let nowString = now
        return Event_(
            id: id,
            title: title,
            description: description,
            organizerId: organizerId,
            participants: participants,
            proposedSlots: proposedSlots,
            deadline: deadline ?? dateString(daysFromNow: 7),
            status: status,
            finalDate: finalDate,
            createdAt: createdAt ?? nowString,
            updatedAt: updatedAt ?? nowString,
            eventType: eventType,
            eventTypeCustom: eventTypeCustom,
            minParticipants: minParticipants.map { KotlinInt(value: $0) },
            maxParticipants: maxParticipants.map { KotlinInt(value: $0) },
            expectedParticipants: expectedParticipants.map { KotlinInt(value: $0) },
            heroImageUrl: heroImageUrl
        )
    }

    private static var sampleTimeSlots: [TimeSlot_] {
        [
            slot(id: "slot-sample-1", start: dateString(daysFromNow: 14, hour: 9), end: dateString(daysFromNow: 14, hour: 17), timeOfDay: .specific),
            slot(id: "slot-sample-2", start: nil, end: nil, timeOfDay: .morning)
        ]
    }
}

#endif
