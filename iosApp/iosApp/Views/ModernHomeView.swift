import SwiftUI
import Shared

/// Modern home view inspired by Apple Invites
/// Features: Card-based event display, beautiful imagery, clean typography
struct ModernHomeView: View {
    let userId: String
    let repository: EventRepository
    let onEventSelected: (Event) -> Void
    let onCreateEvent: () -> Void

    @State private var events: [Event] = []
    @State private var isLoading = true

    var body: some View {
        ZStack {
            // Clean background
            Color(.systemGroupedBackground)
                .ignoresSafeArea()

            ScrollView {
                VStack(spacing: 0) {
                    // Header Section
                    if events.isEmpty && !isLoading {
                        WelcomeSection(onCreateEvent: onCreateEvent)
                    } else {
                        CompactHeader(eventCount: events.count)
                    }

                    // Events Grid/List
                    if isLoading {
                        LoadingEventsView()
                    } else if events.isEmpty {
                        // Welcome section already shown above
                        EmptyView()
                    } else {
                        EventsSection(
                            events: events,
                            onEventSelected: onEventSelected,
                            onCreateEvent: onCreateEvent
                        )
                    }
                }
            }
        }
        .onAppear {
            loadEvents()
        }
    }

    private func loadEvents() {
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            events = repository.getAllEvents()
            isLoading = false
        }
    }
}

// MARK: - Welcome Section

struct WelcomeSection: View {
    let onCreateEvent: () -> Void

    var body: some View {
        VStack(spacing: 32) {
            Spacer()
                .frame(height: 60)

            // Logo or Icon
            ZStack {
                Circle()
                    .fill(
                        LinearGradient(
                            colors: [Color.blue, Color.purple],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .frame(width: 120, height: 120)
                    .shadow(color: Color.blue.opacity(0.3), radius: 30, x: 0, y: 15)

                Image(systemName: "calendar.badge.plus")
                    .font(.system(size: 50, weight: .light))
                    .foregroundColor(.white)
            }
            .padding(.bottom, 8)

            // Welcome Text
            VStack(spacing: 12) {
                Text("Welcome to")
                    .font(.system(size: 17, weight: .regular))
                    .foregroundColor(.secondary)

                Text("Wakeve")
                    .font(.system(size: 48, weight: .bold, design: .rounded))
                    .foregroundColor(.primary)

                Text("Create beautiful invitations for all your events.\nAnyone can receive invitations. Sending\nincluded with iCloud+.")
                    .font(.system(size: 17, weight: .regular))
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .lineSpacing(4)
                    .padding(.top, 4)
            }
            .padding(.horizontal, 32)

            Spacer()
                .frame(height: 40)

            // Create Event Button
            Button(action: onCreateEvent) {
                Text("Create an Event")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundColor(.black)
                    .frame(maxWidth: .infinity)
                    .frame(height: 50)
                    .background(Color.white)
                    .cornerRadius(14)
                    .shadow(color: Color.black.opacity(0.08), radius: 10, x: 0, y: 4)
            }
            .padding(.horizontal, 20)

            Spacer()
        }
    }
}

// MARK: - Compact Header

struct CompactHeader: View {
    let eventCount: Int

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Upcoming")
                        .font(.system(size: 34, weight: .bold))
                        .foregroundColor(.primary)
                }

                Spacer()

                // Add button
                Button(action: {}) {
                    Image(systemName: "plus")
                        .font(.system(size: 20, weight: .semibold))
                        .foregroundColor(.primary)
                        .frame(width: 36, height: 36)
                }

                // Profile button
                Button(action: {}) {
                    Circle()
                        .fill(
                            LinearGradient(
                                colors: [Color.blue, Color.purple],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                        .frame(width: 36, height: 36)
                        .overlay(
                            Text("U")
                                .font(.system(size: 16, weight: .semibold))
                                .foregroundColor(.white)
                        )
                }
            }
            .padding(.horizontal, 20)
            .padding(.top, 60)
            .padding(.bottom, 16)
        }
        .background(Color(.systemGroupedBackground))
    }
}

// MARK: - Events Section

struct EventsSection: View {
    let events: [Event]
    let onEventSelected: (Event) -> Void
    let onCreateEvent: () -> Void

    @Namespace private var namespace

    var body: some View {
        VStack(spacing: 16) {
            // Event Cards
            ForEach(events, id: \.id) { event in
                ModernEventCard(
                    event: event,
                    onTap: { onEventSelected(event) }
                )
                .matchedGeometryEffect(id: event.id, in: namespace)
            }

            // Add Event Card
            AddEventCard(onTap: onCreateEvent)

            Spacer()
                .frame(height: 40)
        }
        .padding(.horizontal, 20)
    }
}

// MARK: - Modern Event Card

struct ModernEventCard: View {
    let event: Event
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            ZStack {
                // Background Image or Gradient
                RoundedRectangle(cornerRadius: 20)
                    .fill(
                        LinearGradient(
                            colors: gradientColors,
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .overlay(
                        // Pattern overlay
                        GeometryReader { geometry in
                            Image(systemName: "calendar")
                                .font(.system(size: 200))
                                .foregroundColor(.white.opacity(0.05))
                                .offset(x: geometry.size.width * 0.6, y: -50)
                        }
                    )

                // Content Overlay
                VStack {
                    // Top Bar with status badge
                    HStack {
                        StatusBadge(status: event.status)

                        Spacer()
                    }
                    .padding(20)

                    Spacer()

                    // Bottom Content
                    VStack(alignment: .leading, spacing: 12) {
                        // Participant Avatars
                        if !event.participants.isEmpty {
                            HStack(spacing: -8) {
                                ForEach(event.participants.prefix(5), id: \.self) { participant in
                                    ParticipantAvatar(participantId: participant)
                                }

                                if event.participants.count > 5 {
                                    AdditionalParticipantsCount(count: event.participants.count - 5)
                                }
                            }
                            .padding(.bottom, 4)
                        }

                        // Event Info
                        VStack(alignment: .leading, spacing: 4) {
                            Text(event.title)
                                .font(.system(size: 28, weight: .bold))
                                .foregroundColor(.white)
                                .lineLimit(2)

                            if let finalDate = event.finalDate {
                                Text(formatEventDate(finalDate))
                                    .font(.system(size: 16, weight: .medium))
                                    .foregroundColor(.white.opacity(0.9))
                            } else {
                                Text(formatDeadline(event.deadline))
                                    .font(.system(size: 16, weight: .medium))
                                    .foregroundColor(.white.opacity(0.9))
                            }

                            if !event.description.isEmpty {
                                Text(event.description)
                                    .font(.system(size: 15))
                                    .foregroundColor(.white.opacity(0.8))
                                    .lineLimit(2)
                                    .padding(.top, 2)
                            }
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(20)
                    .background(
                        LinearGradient(
                            colors: [Color.black.opacity(0), Color.black.opacity(0.7)],
                            startPoint: .top,
                            endPoint: .bottom
                        )
                    )
                }
            }
            .frame(height: 380)
            .cornerRadius(20)
            .shadow(color: Color.black.opacity(0.15), radius: 15, x: 0, y: 8)
        }
        .buttonStyle(ScaleButtonStyle())
    }

    private var gradientColors: [Color] {
        switch event.status {
        case .draft:
            return [Color.orange, Color.red]
        case .polling:
            return [Color.blue, Color.purple]
        case .confirmed:
            return [Color.green, Color.teal]
        default:
            return [Color.gray, Color.gray.opacity(0.7)]
        }
    }

    private func formatEventDate(_ dateString: String) -> String {
        if let date = ISO8601DateFormatter().date(from: dateString) {
            let formatter = DateFormatter()
            formatter.dateFormat = "EEEE, MMM d, h:mm a"
            return formatter.string(from: date)
        }
        return dateString
    }

    private func formatDeadline(_ deadlineString: String) -> String {
        if let date = ISO8601DateFormatter().date(from: deadlineString) {
            let formatter = DateFormatter()
            formatter.dateFormat = "Deadline: MMM d"
            return formatter.string(from: date)
        }
        return "Vote by: \(deadlineString)"
    }
}

// MARK: - Add Event Card

struct AddEventCard: View {
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(spacing: 16) {
                Image(systemName: "plus.circle.fill")
                    .font(.system(size: 50))
                    .foregroundColor(.blue)

                Text("Create New Event")
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundColor(.primary)
            }
            .frame(maxWidth: .infinity)
            .frame(height: 180)
            .background(Color(.systemBackground))
            .cornerRadius(20)
            .overlay(
                RoundedRectangle(cornerRadius: 20)
                    .stroke(Color(.separator), lineWidth: 1)
            )
        }
        .buttonStyle(ScaleButtonStyle())
    }
}

// MARK: - Status Badge

struct StatusBadge: View {
    let status: EventStatus

    var body: some View {
        HStack(spacing: 6) {
            Image(systemName: statusIcon)
                .font(.system(size: 12, weight: .semibold))

            Text(statusText)
                .font(.system(size: 13, weight: .semibold))
        }
        .foregroundColor(.white)
        .padding(.horizontal, 12)
        .padding(.vertical, 6)
        .background(Color.white.opacity(0.25))
        .backdrop(radius: 10, opaque: false)
        .cornerRadius(20)
    }

    private var statusText: String {
        switch status {
        case .draft: return "Draft"
        case .polling: return "Polling"
        case .confirmed: return "Confirmed"
        default: return ""
        }
    }

    private var statusIcon: String {
        switch status {
        case .draft: return "doc"
        case .polling: return "chart.bar"
        case .confirmed: return "checkmark.circle"
        default: return "questionmark"
        }
    }
}

// MARK: - Participant Avatar

struct ParticipantAvatar: View {
    let participantId: String

    private var initials: String {
        String(participantId.prefix(1).uppercased())
    }

    private var avatarColor: Color {
        let colors: [Color] = [.red, .orange, .yellow, .green, .blue, .purple, .pink]
        let hash = abs(participantId.hashValue)
        return colors[hash % colors.count]
    }

    var body: some View {
        Circle()
            .fill(avatarColor)
            .frame(width: 40, height: 40)
            .overlay(
                Text(initials)
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(.white)
            )
            .overlay(
                Circle()
                    .stroke(Color.white, lineWidth: 2)
            )
    }
}

struct AdditionalParticipantsCount: View {
    let count: Int

    var body: some View {
        Circle()
            .fill(Color.white.opacity(0.3))
            .frame(width: 40, height: 40)
            .backdrop(radius: 10, opaque: false)
            .overlay(
                Text("+\(count)")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(.white)
            )
            .overlay(
                Circle()
                    .stroke(Color.white, lineWidth: 2)
            )
    }
}

// MARK: - Loading View

struct LoadingEventsView: View {
    var body: some View {
        VStack(spacing: 20) {
            ProgressView()
                .progressViewStyle(CircularProgressViewStyle())
                .scaleEffect(1.2)

            Text("Loading events...")
                .font(.system(size: 16, weight: .medium))
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 100)
    }
}

// MARK: - Button Styles

struct ScaleButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.97 : 1.0)
            .animation(.spring(response: 0.3, dampingFraction: 0.6), value: configuration.isPressed)
    }
}

// MARK: - View Extension for Backdrop

extension View {
    func backdrop(radius: CGFloat, opaque: Bool = true) -> some View {
        self.background(
            .ultraThinMaterial,
            in: RoundedRectangle(cornerRadius: radius, style: .continuous)
        )
    }
}
