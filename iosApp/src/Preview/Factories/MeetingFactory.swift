import Foundation
import Shared

#if DEBUG

enum MeetingFactory {
    static var scheduled: VirtualMeeting {
        make(
            id: "meeting-planning",
            platform: .zoom,
            meetingId: "845-221-900",
            meetingUrl: "https://zoom.us/j/845221900",
            title: "Point organisation",
            description: "Validation du programme et des derniers participants.",
            scheduledFor: date(daysFromNow: 2, hour: 18),
            status: .scheduled
        )
    }

    static var started: VirtualMeeting {
        make(
            id: "meeting-live",
            platform: .googleMeet,
            meetingId: "wakeve-live",
            meetingUrl: "https://meet.google.com/wkv-live",
            title: "Brief participants",
            description: "Point rapide avant le depart.",
            scheduledFor: date(daysFromNow: 0, hour: 14),
            status: .started
        )
    }

    static var ended: VirtualMeeting {
        make(
            id: "meeting-recap",
            platform: .facetime,
            meetingId: "facetime-recap",
            meetingUrl: "",
            title: "Recap post-evenement",
            description: "Notes et prochaines actions.",
            scheduledFor: date(daysFromNow: -1, hour: 11),
            status: .ended
        )
    }

    static var list: [VirtualMeeting] {
        [scheduled, started, ended]
    }

    static func make(
        id: String,
        platform: MeetingPlatform,
        meetingId: String,
        meetingUrl: String,
        title: String,
        description: String?,
        scheduledFor: Date,
        status: MeetingStatus_
    ) -> VirtualMeeting {
        let instant = Kotlinx_datetimeInstant.companion.fromEpochSeconds(
            epochSeconds: Int64(scheduledFor.timeIntervalSince1970),
            nanosecondAdjustment: 0
        )
        let created = Kotlinx_datetimeInstant.companion.fromEpochSeconds(
            epochSeconds: Int64(Date().addingTimeInterval(-86_400).timeIntervalSince1970),
            nanosecondAdjustment: 0
        )

        return VirtualMeeting(
            id: id,
            eventId: "event-preview-meeting",
            organizerId: "user-organizer",
            platform: platform,
            meetingId: meetingId,
            meetingPassword: "wakeve",
            meetingUrl: meetingUrl,
            dialInNumber: nil,
            dialInPassword: nil,
            title: title,
            description: description,
            scheduledFor: instant,
            duration: 3_600_000_000_000,
            timezone: "Europe/Paris",
            participantLimit: KotlinInt(value: 12),
            requirePassword: true,
            waitingRoom: true,
            hostKey: nil,
            createdAt: created,
            status: status
        )
    }

    private static func date(daysFromNow days: Int, hour: Int) -> Date {
        var components = Calendar.current.dateComponents([.year, .month, .day], from: Date())
        components.day = (components.day ?? 1) + days
        components.hour = hour
        components.minute = 0
        return Calendar.current.date(from: components) ?? Date()
    }
}

#endif
