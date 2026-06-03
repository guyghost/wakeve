import SwiftUI
import Shared

struct EventDetailExperienceView: View {
    let event: Event_
    let repository: EventRepositoryInterface
    let onBack: () -> Void
    let onOpenParticipants: () -> Void
    let onOpenVote: () -> Void
    let onOpenTransport: () -> Void
    let onOpenMessages: () -> Void

    @State private var participants: [String] = []
    @State private var messageText = ""

    var body: some View {
        ZStack {
            Color(.systemBackground).ignoresSafeArea()

            ScrollView(showsIndicators: false) {
                VStack(spacing: 16) {
                    header
                    coverCard
                    eventMetaCard
                    quickActions
                    aboutCard
                    messageComposer
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 24)
            }
        }
        .onAppear(perform: loadParticipants)
    }

    private var header: some View {
        HStack {
            Button(action: onBack) {
                Image(systemName: "chevron.left")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(.primary)
                    .frame(width: 36, height: 36)
                    .background(Color(.tertiarySystemFill))
                    .clipShape(Circle())
            }

            Spacer()

            Text(event.title)
                .font(.system(size: 20, weight: .bold))
                .lineLimit(1)

            Spacer()

            Menu {
                Button("Messages", action: onOpenMessages)
                Button("Participants", action: onOpenParticipants)
            } label: {
                Image(systemName: "ellipsis")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(.primary)
                    .frame(width: 36, height: 36)
                    .background(Color(.tertiarySystemFill))
                    .clipShape(Circle())
            }
        }
        .padding(.top, 8)
    }

    private var coverCard: some View {
        ZStack(alignment: .bottomLeading) {
            LinearGradient(
                colors: [Color.wakevePrimary, Color.wakeveAccent],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )

            VStack(alignment: .leading, spacing: 8) {
                Text(event.title)
                    .font(.system(size: 28, weight: .bold))
                    .foregroundColor(.white)
            }
            .padding(16)
        }
        .frame(height: 180)
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
    }

    private var eventMetaCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Label(primarySlotDate, systemImage: "calendar")
                .font(.subheadline.weight(.semibold))

            Label(primarySlotLocation, systemImage: "mappin.and.ellipse")
                .font(.subheadline)
                .foregroundStyle(.secondary)

            HStack(spacing: 8) {
                ForEach(participants.prefix(7), id: \.self) { participant in
                    AvatarCircle(label: participantInitials(participant))
                }

                if participants.count > 7 {
                    Text("+\(participants.count - 7)")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(.secondary)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .glassCard(cornerRadius: 20)
    }

    private var quickActions: some View {
        HStack(spacing: 10) {
            ActionTile(title: "Details", icon: "info.circle", action: { })
            ActionTile(title: "Participants", icon: "person.2", action: onOpenParticipants)
            ActionTile(
                title: voteActionTitle,
                icon: voteActionIcon,
                isEnabled: canOpenVoteAction,
                action: onOpenVote
            )
            ActionTile(title: "Transport", icon: "car", action: onOpenTransport)
        }
    }

    private var aboutCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("À propos")
                .font(.headline)

            if event.description_.isEmpty {
                Text("Pas de description pour le moment.")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            } else {
                Text(event.description_)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .glassCard(cornerRadius: 20)
    }

    private var messageComposer: some View {
        HStack(spacing: 10) {
            TextField("Écrire un message...", text: $messageText)
                .textFieldStyle(.plain)
                .padding(.horizontal, 14)
                .padding(.vertical, 12)
                .background(Color(.tertiarySystemFill))
                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))

            Button(action: onOpenMessages) {
                Image(systemName: "paperplane")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundStyle(.white)
                    .frame(width: 44, height: 44)
                    .background(Color.wakevePrimary)
                    .clipShape(Circle())
            }
        }
        .padding(12)
        .glassCard(cornerRadius: 20)
    }

    private var primarySlotDate: String {
        guard let slot = event.proposedSlots.first else { return "Date à confirmer" }
        return formatDateTime(slot.start) ?? "Date à confirmer"
    }

    private var canVote: Bool {
        event.status == .polling && !event.proposedSlots.isEmpty
    }

    private var canViewResults: Bool {
        event.status != .draft && !event.proposedSlots.isEmpty
    }

    private var canOpenVoteAction: Bool {
        canVote || canViewResults
    }

    private var voteActionTitle: String {
        canVote ? "Vote" : "Results"
    }

    private var voteActionIcon: String {
        canVote ? "checkmark.circle" : "chart.bar"
    }

    private var primarySlotLocation: String {
        return "Lieu à confirmer"
    }

    private func loadParticipants() {
        participants = repository.getParticipants(eventId: event.id) ?? []
    }

    private func participantInitials(_ email: String) -> String {
        let name = email.split(separator: "@").first.map(String.init) ?? email
        let chunks = name.split(separator: ".")
        if chunks.count >= 2 {
            return String(chunks[0].prefix(1) + chunks[1].prefix(1)).uppercased()
        }
        return String(name.prefix(2)).uppercased()
    }

    private func formatDateTime(_ value: String?) -> String? {
        guard let value, let date = ISO8601DateFormatter().date(from: value) else { return nil }
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "fr_FR")
        formatter.dateFormat = "EEEE d MMM • HH:mm"
        return formatter.string(from: date).capitalized
    }
}

struct TransportPlanView: View {
    let event: Event_
    let repository: EventRepositoryInterface
    let onBack: () -> Void

    @State private var selectedMode: TransportMode = .trip
    @State private var participants: [String] = []

    var body: some View {
        ZStack {
            Color(.systemBackground).ignoresSafeArea()

            VStack(spacing: 16) {
                HStack {
                    Button(action: onBack) {
                        Image(systemName: "chevron.left")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(.primary)
                            .frame(width: 36, height: 36)
                            .background(Color(.tertiarySystemFill))
                            .clipShape(Circle())
                    }

                    Spacer()

                    Text("Transport")
                        .font(.system(size: 24, weight: .bold))

                    Spacer()

                    Image(systemName: "ellipsis")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(.secondary)
                        .frame(width: 36, height: 36)
                }
                .padding(.horizontal, 16)
                .padding(.top, 8)

                Picker("Transport mode", selection: $selectedMode) {
                    Text("Trajet").tag(TransportMode.trip)
                    Text("Covoiturage").tag(TransportMode.carpool)
                }
                .pickerStyle(.segmented)
                .padding(.horizontal, 16)

                VStack(spacing: 14) {
                    RoundedRectangle(cornerRadius: 18, style: .continuous)
                        .fill(Color(.tertiarySystemFill))
                        .overlay(
                            Image(systemName: "point.topleft.down.curvedto.point.bottomright.up")
                                .font(.system(size: 34, weight: .medium))
                                .foregroundStyle(.secondary)
                        )
                        .frame(height: 190)

                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("Départ")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                            Text(slotStartTime)
                                .font(.title3.weight(.bold))
                        }

                        Spacer()

                        VStack(alignment: .trailing, spacing: 4) {
                            Text("Arrivée")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                            Text(slotEndTime)
                                .font(.title3.weight(.bold))
                        }
                    }

                    HStack(spacing: 8) {
                        ForEach(participants.prefix(6), id: \.self) { participant in
                            AvatarCircle(label: participantInitials(participant))
                        }
                        if participants.count > 6 {
                            Text("+\(participants.count - 6)")
                                .font(.caption.weight(.semibold))
                                .foregroundStyle(.secondary)
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
                .padding(16)
                .glassCard(cornerRadius: 20)
                .padding(.horizontal, 16)

                Button {
                } label: {
                    Text("Je participe")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                        .background(Color.wakevePrimary)
                        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                }
                .padding(.horizontal, 16)

                Spacer()
            }
        }
        .onAppear {
            participants = repository.getParticipants(eventId: event.id) ?? []
        }
    }

    private var slotStartTime: String {
        timeLabel(event.proposedSlots.first?.start, fallback: "17:30")
    }

    private var slotEndTime: String {
        timeLabel(event.proposedSlots.first?.end, fallback: "18:00")
    }

    private func participantInitials(_ email: String) -> String {
        String(email.prefix(2)).uppercased()
    }

    private func timeLabel(_ value: String?, fallback: String) -> String {
        guard let value, let date = ISO8601DateFormatter().date(from: value) else {
            return fallback
        }
        let formatter = DateFormatter()
        formatter.timeStyle = .short
        formatter.locale = Locale(identifier: "fr_FR")
        return formatter.string(from: date)
    }
}

private enum TransportMode {
    case trip
    case carpool
}

private struct ActionTile: View {
    let title: String
    let icon: String
    var isEnabled: Bool = true
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 8) {
                Image(systemName: icon)
                    .font(.system(size: 18, weight: .semibold))
                Text(title)
                    .font(.caption.weight(.semibold))
            }
            .frame(maxWidth: .infinity)
            .frame(height: 64)
            .foregroundStyle(isEnabled ? .primary : .secondary)
            .background(Color(.tertiarySystemFill).opacity(isEnabled ? 1 : 0.6))
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        }
        .disabled(!isEnabled)
        .buttonStyle(ScaleButtonStyle())
    }
}

private struct AvatarCircle: View {
    let label: String

    var body: some View {
        Text(label)
            .font(.caption.weight(.semibold))
            .foregroundStyle(.primary)
            .frame(width: 28, height: 28)
            .background(Color(.tertiarySystemFill))
            .clipShape(Circle())
    }
}
