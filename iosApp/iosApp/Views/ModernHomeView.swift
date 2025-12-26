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
            // Dark background with subtle gradient (like Apple Invites)
            LinearGradient(
                colors: [
                    Color(red: 0.11, green: 0.11, blue: 0.12),
                    Color(red: 0.09, green: 0.09, blue: 0.10)
                ],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()

            VStack(spacing: 0) {
                // Header
                AppleInvitesHeader(
                    userId: userId,
                    onCreateEvent: onCreateEvent
                )

                // Content
                if isLoading {
                    LoadingEventsView()
                } else if events.isEmpty {
                    AppleInvitesEmptyState(onCreateEvent: onCreateEvent)
                } else {
                    ScrollView {
                        VStack(spacing: 16) {
                            // Event Cards
                            ForEach(events, id: \.id) { event in
                                ModernEventCard(
                                    event: event,
                                    onTap: { onEventSelected(event) }
                                )
                            }

                            // Add Event Card
                            AddEventCard(onTap: onCreateEvent)

                            Spacer()
                                .frame(height: 40)
                        }
                        .padding(.horizontal, 20)
                        .padding(.top, 8)
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

// MARK: - Apple Invites Header

struct AppleInvitesHeader: View {
    let userId: String
    let onCreateEvent: () -> Void

    private var userInitial: String {
        String(userId.prefix(1).uppercased())
    }

    var body: some View {
        HStack(alignment: .center, spacing: 12) {
            // "Upcoming" with dropdown
            HStack(spacing: 4) {
                Text("Upcoming")
                    .font(.system(size: 34, weight: .bold))
                    .foregroundColor(.white)

                Image(systemName: "chevron.down")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundColor(.white.opacity(0.6))
            }

            Spacer()

            // Add button
            Button(action: onCreateEvent) {
                Image(systemName: "plus")
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundColor(.white)
                    .frame(width: 36, height: 36)
                    .background(Color.white.opacity(0.15))
                    .clipShape(Circle())
            }

            // Profile avatar
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
                    Text(userInitial)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(.white)
                )
        }
        .padding(.horizontal, 20)
        .padding(.top, 60)
        .padding(.bottom, 16)
    }
}

// MARK: - Apple Invites Empty State

struct AppleInvitesEmptyState: View {
    let onCreateEvent: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            Spacer()

            VStack(spacing: 24) {
                // Calendar icon
                ZStack {
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .fill(Color.white.opacity(0.15))
                        .frame(width: 80, height: 80)

                    Image(systemName: "calendar")
                        .font(.system(size: 40, weight: .regular))
                        .foregroundColor(.white.opacity(0.5))
                }

                // Text content
                VStack(spacing: 12) {
                    Text("No Upcoming Events")
                        .font(.system(size: 28, weight: .bold))
                        .foregroundColor(.white)

                    Text("Upcoming events, whether you're a host\nor a guest, will appear here.")
                        .font(.system(size: 17))
                        .foregroundColor(.white.opacity(0.6))
                        .multilineTextAlignment(.center)
                        .lineSpacing(4)
                }

                // Create Event button
                Button(action: onCreateEvent) {
                    Text("Create Event")
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundColor(.black)
                        .frame(width: 280)
                        .frame(height: 50)
                        .background(Color.white)
                        .continuousCornerRadius(25)
                }
                .padding(.top, 16)
            }

            Spacer()
        }
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
                        EventStatusBadge(status: event.status)

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
            .continuousCornerRadius(20)
            .shadow(color: Color.black.opacity(0.08), radius: 12, x: 0, y: 6)
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
                    .foregroundColor(.white.opacity(0.7))

                Text("Create New Event")
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundColor(.white)
            }
            .frame(maxWidth: .infinity)
            .frame(height: 180)
            .background(Color.white.opacity(0.1))
            .continuousCornerRadius(20)
            .overlay(
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .stroke(Color.white.opacity(0.2), lineWidth: 1)
            )
        }
        .buttonStyle(ScaleButtonStyle())
    }
}

// MARK: - Event Status Badge

struct EventStatusBadge: View {
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
        .background(.ultraThinMaterial)
        .continuousCornerRadius(20)
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
            .background(.ultraThinMaterial, in: Circle())
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
                .progressViewStyle(CircularProgressViewStyle(tint: .white))
                .scaleEffect(1.2)

            Text("Loading events...")
                .font(.system(size: 16, weight: .medium))
                .foregroundColor(.white.opacity(0.6))
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
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

