import Foundation
import Shared

#if DEBUG

enum EventFactory {

    // MARK: - ISO 8601 Helpers

    private static let iso8601: ISO8601DateFormatter = {
        let f = ISO8601DateFormatter()
        return f
    }()

    private static var now: String { iso8601.string(from: Date()) }

    private static func dateString(daysFromNow days: Int, hour: Int = 10) -> String {
        var components = DateComponents()
        components.day = days
        components.hour = hour - Calendar.current.component(.hour, from: Date())
        let date = Calendar.current.date(byAdding: components, to: Date())!
        return iso8601.string(from: date)
    }

    // MARK: - Named Variants

    /// Minimal draft event — no participants, no final date, no proposed slots.
    static var empty: Event {
        Event(
            id: "event-empty-001",
            title: "",
            description: "",
            organizerId: UserFactory.organizer.id,
            participants: [],
            proposedSlots: [],
            deadline: dateString(daysFromNow: 7),
            status: .draft,
            finalDate: nil,
            createdAt: now,
            updatedAt: now,
            eventType: .other,
            eventTypeCustom: nil,
            minParticipants: nil,
            maxParticipants: nil,
            expectedParticipants: nil,
            heroImageUrl: nil
        )
    }

    /// Fully populated event — confirmed status, participants, final date, hero image.
    static var complete: Event {
        let participants = UserFactory.group(count: 5).map(\.id)
        return Event(
            id: "event-complete-002",
            title: "Weekend Hiking Trip",
            description: "Join us for a scenic hike through the Calanques National Park followed by a picnic.",
            organizerId: UserFactory.organizer.id,
            participants: participants,
            proposedSlots: sampleTimeSlots,
            deadline: dateString(daysFromNow: 3),
            status: .confirmed,
            finalDate: dateString(daysFromNow: 14, hour: 9),
            createdAt: dateString(daysFromNow: -7),
            updatedAt: now,
            eventType: .outdoorActivity,
            eventTypeCustom: nil,
            minParticipants: KotlinInt(value: 3),
            maxParticipants: KotlinInt(value: 20),
            expectedParticipants: KotlinInt(value: 8),
            heroImageUrl: "https://images.unsplash.com/photo-1551632811-561732d1e306?w=800"
        )
    }

    /// Event with finalDate in the past, finalized status.
    static var past: Event {
        let participants = UserFactory.group(count: 4).map(\.id)
        return Event(
            id: "event-past-003",
            title: "Team Offsite 2025",
            description: "Annual team building weekend in the Alps.",
            organizerId: UserFactory.organizer.id,
            participants: participants,
            proposedSlots: [],
            deadline: dateString(daysFromNow: -30),
            status: .finalized,
            finalDate: dateString(daysFromNow: -14, hour: 10),
            createdAt: dateString(daysFromNow: -60),
            updatedAt: dateString(daysFromNow: -14),
            eventType: .teamBuilding,
            eventTypeCustom: nil,
            minParticipants: KotlinInt(value: 4),
            maxParticipants: nil,
            expectedParticipants: KotlinInt(value: 6),
            heroImageUrl: nil
        )
    }

    /// Event in polling status with proposed time slots.
    static var polling: Event {
        let participants = UserFactory.group(count: 3).map(\.id)
        return Event(
            id: "event-polling-004",
            title: "Birthday Dinner",
            description: "Celebrating Emma's birthday! Vote for the best date.",
            organizerId: UserFactory.organizer.id,
            participants: participants,
            proposedSlots: [
                TimeSlot(
                    id: "slot-poll-1",
                    start: dateString(daysFromNow: 7, hour: 19),
                    end: dateString(daysFromNow: 7, hour: 22),
                    timezone: "Europe/Paris",
                    timeOfDay: .evening
                ),
                TimeSlot(
                    id: "slot-poll-2",
                    start: dateString(daysFromNow: 8, hour: 12),
                    end: dateString(daysFromNow: 8, hour: 14),
                    timezone: "Europe/Paris",
                    timeOfDay: .afternoon
                ),
                TimeSlot(
                    id: "slot-poll-3",
                    start: nil,
                    end: nil,
                    timezone: "Europe/Paris",
                    timeOfDay: .morning
                )
            ],
            deadline: dateString(daysFromNow: 5),
            status: .polling,
            finalDate: nil,
            createdAt: dateString(daysFromNow: -2),
            updatedAt: now,
            eventType: .birthday,
            eventTypeCustom: nil,
            minParticipants: KotlinInt(value: 5),
            maxParticipants: KotlinInt(value: 15),
            expectedParticipants: KotlinInt(value: 10),
            heroImageUrl: nil
        )
    }

    /// Event with 15+ participants for testing large participant lists.
    static var withManyParticipants: Event {
        let participantIds = (0..<18).map { "user-crowd-\($0)" }
        return Event(
            id: "event-crowd-005",
            title: "Company Summer Party",
            description: "End-of-summer celebration with BBQ, games, and live music at Parc Borély.",
            organizerId: UserFactory.organizer.id,
            participants: participantIds,
            proposedSlots: [],
            deadline: dateString(daysFromNow: 10),
            status: .confirmed,
            finalDate: dateString(daysFromNow: 21, hour: 16),
            createdAt: dateString(daysFromNow: -14),
            updatedAt: now,
            eventType: .party,
            eventTypeCustom: nil,
            minParticipants: KotlinInt(value: 10),
            maxParticipants: KotlinInt(value: 50),
            expectedParticipants: KotlinInt(value: 25),
            heroImageUrl: "https://images.unsplash.com/photo-1533174072545-7a4b6ad7a6c3?w=800"
        )
    }

    // MARK: - Builder

    /// Create an event with sensible defaults. Override only the parameters you need.
    static func make(
        id: String = "event-\(UUID().uuidString.prefix(8))",
        title: String = "Test Event",
        description: String = "A test event for preview purposes.",
        organizerId: String = UserFactory.organizer.id,
        participants: [String] = [],
        proposedSlots: [TimeSlot] = [],
        deadline: String? = nil,
        status: EventStatus = .draft,
        finalDate: String? = nil,
        createdAt: String? = nil,
        updatedAt: String? = nil,
        eventType: Shared.EventType = .other,
        eventTypeCustom: String? = nil,
        minParticipants: Int32? = nil,
        maxParticipants: Int32? = nil,
        expectedParticipants: Int32? = nil,
        heroImageUrl: String? = nil
    ) -> Event {
        let nowString = now
        return Event(
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

    // MARK: - Shared Time Slots

    /// Sample time slots reusable across variants.
    private static var sampleTimeSlots: [TimeSlot] {
        [
            TimeSlot(
                id: "slot-sample-1",
                start: dateString(daysFromNow: 14, hour: 9),
                end: dateString(daysFromNow: 14, hour: 17),
                timezone: "Europe/Paris",
                timeOfDay: .specific
            ),
            TimeSlot(
                id: "slot-sample-2",
                start: nil,
                end: nil,
                timezone: "Europe/Paris",
                timeOfDay: .morning
            )
        ]
    }
}

#endif
