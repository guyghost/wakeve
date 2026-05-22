import SwiftUI
import Shared

/// Participant management view inspired by Apple Invites
/// Features: Clean list design, easy participant management, clear status indicators
struct ParticipantManagementView: View {
    @Environment(\.colorScheme) private var colorScheme

    let event: Event
    let repository: EventRepositoryInterface?
    let previewParticipants: [String]?
    let onParticipantsUpdated: () -> Void
    let onBack: () -> Void

    @State private var newParticipantEmail = ""
    @State private var participantRows: [ParticipantPresentationRow] = []
    @State private var isLoading = false
    @State private var errorMessage = ""
    @State private var showError = false
    @State private var showSuccess = false

    init(
        event: Event,
        repository: EventRepositoryInterface,
        onParticipantsUpdated: @escaping () -> Void,
        onBack: @escaping () -> Void
    ) {
        self.event = event
        self.repository = repository
        self.previewParticipants = nil
        self.onParticipantsUpdated = onParticipantsUpdated
        self.onBack = onBack
    }

#if DEBUG
    init(
        event: Event,
        previewParticipants: [String],
        onParticipantsUpdated: @escaping () -> Void = {},
        onBack: @escaping () -> Void = {}
    ) {
        self.event = event
        self.repository = nil
        self.previewParticipants = previewParticipants
        self.onParticipantsUpdated = onParticipantsUpdated
        self.onBack = onBack
    }
#endif

    var body: some View {
        GeometryReader { proxy in
            ZStack(alignment: .top) {
                pageBackground
                    .ignoresSafeArea()

                ScrollView(showsIndicators: false) {
                    VStack(spacing: 0) {
                        heroSection(topInset: proxy.safeAreaInsets.top)

                        VStack(alignment: .leading, spacing: 18) {
                            if event.status == .draft {
                                addParticipantCard
                            }

                            statusSummary
                            participantsContent
                        }
                        .padding(.horizontal, WakeveTheme.Spacing.page)
                        .padding(.top, -22)
                        .padding(.bottom, bottomContentInset(for: proxy.safeAreaInsets.bottom))
                    }
                }
                .scrollClipDisabled()
                .ignoresSafeArea(edges: .top)

                overlayControls(topInset: proxy.safeAreaInsets.top)
            }
            .safeAreaInset(edge: .bottom, spacing: 0) {
                if event.status == .draft {
                    bottomActionBar
                }
            }
        }
        .toolbar(.hidden, for: .tabBar)
        .onAppear {
            loadParticipants()
        }
        .alert("Erreur", isPresented: $showError) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(errorMessage)
        }
        .alert("Sondage lancé", isPresented: $showSuccess) {
            Button("OK", role: .cancel) {
                onBack()
            }
        } message: {
            Text("Le sondage est maintenant ouvert aux participants.")
        }
    }

    private func heroSection(topInset: CGFloat) -> some View {
        ZStack(alignment: .bottomLeading) {
            LinearGradient(
                colors: heroColors,
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .overlay(alignment: .trailing) {
                Image(systemName: "person.2.fill")
                    .font(.system(size: 116, weight: .black))
                    .foregroundColor(.white.opacity(0.18))
                    .rotationEffect(.degrees(-8))
                    .offset(x: 32, y: -8)
            }
            .overlay(alignment: .bottom) {
                LinearGradient(
                    colors: [.clear, pageBackground.opacity(colorScheme == .dark ? 0.94 : 0.98)],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .frame(height: 142)
            }

            VStack(alignment: .leading, spacing: 10) {
                statusBadge

                Text("Participants")
                    .font(.system(size: 42, weight: .bold))
                    .foregroundColor(.white)
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)

                Text(event.title)
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundColor(.white.opacity(0.72))
                    .lineLimit(2)
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 62)
        }
        .frame(height: 318 + topInset)
    }

    private func overlayControls(topInset: CGFloat) -> some View {
        VStack(spacing: 0) {
            HStack {
                WakeveCircleButton(
                    systemImage: "xmark",
                    accessibilityLabel: "Fermer",
                    variant: .glass,
                    size: 48,
                    action: onBack
                )

                Spacer()

                Menu {
                    Button {
                        newParticipantEmail = ""
                    } label: {
                        Label("Réinitialiser l’invitation", systemImage: "arrow.counterclockwise")
                    }

                    if canStartPoll {
                        Button {
                            Task { await startPoll() }
                        } label: {
                            Label("Lancer le sondage", systemImage: "chart.bar.xaxis")
                        }
                    }
                } label: {
                    WakeveGlassControl {
                        Image(systemName: "ellipsis")
                            .font(.system(size: 17, weight: .bold))
                            .foregroundColor(.white)
                            .frame(width: 48, height: 48)
                    }
                }
                .accessibilityLabel("Options participants")
            }
            .padding(.horizontal, WakeveTheme.Spacing.page)
            .padding(.top, WakeveTheme.Navigation.controlTopPadding(safeAreaTop: topInset))

            Spacer()
        }
    }

    private var addParticipantCard: some View {
        WakeveGlassCard(cornerRadius: WakeveTheme.Radius.xl) {
            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
                Text("Inviter un participant")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(primaryText)

                HStack(spacing: 12) {
                    TextField("Adresse email", text: $newParticipantEmail)
                        .font(.system(size: 17, weight: .medium))
                        .textFieldStyle(.plain)
                        .padding(.horizontal, 16)
                        .frame(height: 54)
                        .foregroundColor(primaryText)
                        .background(inputBackground)
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                        .keyboardType(.emailAddress)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .textContentType(.emailAddress)

                    Button {
                        Task {
                            await addParticipant()
                        }
                    } label: {
                        if isLoading {
                            ProgressView()
                                .tint(.white)
                                .frame(width: 52, height: 52)
                                .background(Color.blue.opacity(0.86))
                                .clipShape(Circle())
                        } else {
                            Image(systemName: "plus")
                                .font(.system(size: 24, weight: .bold))
                                .foregroundColor(.white)
                                .frame(width: 52, height: 52)
                                .background(newParticipantEmail.isEmpty ? Color.gray.opacity(0.35) : Color.blue)
                                .clipShape(Circle())
                        }
                    }
                    .disabled(newParticipantEmail.isEmpty || isLoading)
                    .accessibilityLabel("Ajouter le participant")
                }
            }
        }
    }

    private var statusSummary: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(participantCountText)
                .font(.system(size: 24, weight: .bold))
                .foregroundColor(primaryText)

            HStack(spacing: 8) {
                Circle()
                    .fill(statusColor)
                    .frame(width: 8, height: 8)

                Text(statusText)
                    .font(.system(size: 15, weight: .medium))
                    .foregroundColor(secondaryText)
            }
        }
    }

    @ViewBuilder
    private var participantsContent: some View {
        if participantRows.isEmpty {
            VStack(spacing: 18) {
                Image(systemName: "person.crop.circle.badge.plus")
                    .font(.system(size: 54, weight: .semibold))
                    .foregroundColor(secondaryText.opacity(0.65))
                    .padding(.top, 24)

                VStack(spacing: 8) {
                    Text("Aucun participant")
                        .font(.system(size: 24, weight: .bold))
                        .foregroundColor(primaryText)

                    Text(event.status == .draft ? "Ajoutez au moins un participant pour ouvrir le sondage." : "Personne n’a encore rejoint cet événement.")
                        .font(.system(size: 16, weight: .medium))
                        .foregroundColor(secondaryText)
                        .multilineTextAlignment(.center)
                        .lineSpacing(3)
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 26)
            }
            .frame(maxWidth: .infinity)
            .background(cardBackground)
            .overlay(
                RoundedRectangle(cornerRadius: 24)
                    .stroke(cardBorder, lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 24))
        } else {
            VStack(spacing: 10) {
                ForEach(participantRows) { participant in
                    ParticipantRowView(participant: participant)
                }
            }
        }
    }

    private var bottomActionBar: some View {
        VStack(spacing: 0) {
            LinearGradient(
                colors: [pageBackground.opacity(0), pageBackground.opacity(0.94)],
                startPoint: .top,
                endPoint: .bottom
            )
            .frame(height: 42)
            .allowsHitTesting(false)

            WakeveActionButton(
                "Lancer le sondage",
                systemImage: "chart.bar.xaxis",
                variant: .primary,
                isDisabled: !canStartPoll,
                isLoading: isLoading
            ) {
                Task { await startPoll() }
            }
            .padding(.horizontal, WakeveTheme.Spacing.page)
            .padding(.top, WakeveTheme.Spacing.xs)
            .padding(.bottom, WakeveTheme.Spacing.md)
            .background(pageBackground.opacity(0.94))
        }
    }

    private func bottomContentInset(for safeAreaBottom: CGFloat) -> CGFloat {
        guard event.status == .draft else {
            return safeAreaBottom + WakeveTheme.Spacing.xxl
        }

        return safeAreaBottom + 92
    }

    private var statusBadge: some View {
        HStack(spacing: 6) {
            Image(systemName: statusIcon)
                .font(.system(size: 13, weight: .bold))
            Text(statusTitle)
                .font(.system(size: 14, weight: .bold))
        }
        .foregroundColor(.white)
        .padding(.horizontal, 12)
        .padding(.vertical, 7)
        .background(Color.black.opacity(0.25))
        .clipShape(Capsule())
    }

    private var statusColor: Color {
        switch event.status {
        case .draft: return .orange
        case .polling: return .blue
        case .confirmed: return .green
        default: return .gray
        }
    }

    private var statusText: String {
        switch event.status {
        case .draft: return draftStatusText
        case .polling: return "Sondage actif"
        case .confirmed: return "Événement confirmé"
        default: return "Statut indisponible"
        }
    }

    private var statusTitle: String {
        switch event.status {
        case .draft: return "Préparation du sondage"
        case .polling: return "Sondage actif"
        case .confirmed: return "Date confirmée"
        default: return "Participants"
        }
    }

    private var statusIcon: String {
        switch event.status {
        case .draft: return "person.badge.plus"
        case .polling: return "chart.bar.fill"
        case .confirmed: return "checkmark.circle.fill"
        default: return "person.2.fill"
        }
    }

    private var participantCountText: String {
        "\(participantRows.count) participant\(participantRows.count > 1 ? "s" : "")"
    }

    private var canStartPoll: Bool {
        event.status == .draft && !participantRows.isEmpty && !event.proposedSlots.isEmpty
    }

    private var draftStatusText: String {
        if participantRows.isEmpty {
            return "Brouillon - ajoutez les participants pour commencer"
        }

        if event.proposedSlots.isEmpty {
            return "Brouillon - ajoutez au moins un créneau"
        }

        return "Prêt à lancer le sondage"
    }

    private var heroColors: [Color] {
        switch event.eventType {
        case .birthday, .party:
            return [Color(hex: "FFB86B"), Color(hex: "F43F5E"), pageBackground]
        case .sportsEvent, .sportEvent:
            return [Color(hex: "88D18A"), Color(hex: "2F855A"), pageBackground]
        case .teamBuilding, .conference, .workshop, .techMeetup:
            return [Color(hex: "67E8F9"), Color(hex: "2563EB"), pageBackground]
        case .outdoorActivity:
            return [Color(hex: "FDE68A"), Color(hex: "0F766E"), pageBackground]
        default:
            return [Color(hex: "F6C177"), Color(hex: "7C3AED"), pageBackground]
        }
    }

    private var pageBackground: Color {
        WakeveTheme.ColorToken.pageBackground(for: colorScheme)
    }

    private var primaryText: Color {
        WakeveTheme.ColorToken.primaryText(for: colorScheme)
    }

    private var secondaryText: Color {
        WakeveTheme.ColorToken.secondaryText(for: colorScheme)
    }

    private var cardBackground: Color {
        WakeveTheme.ColorToken.cardFill(for: colorScheme)
    }

    private var inputBackground: Color {
        colorScheme == .dark ? WakeveTheme.ColorToken.searchFieldDark.opacity(0.72) : WakeveTheme.ColorToken.searchFieldLight
    }

    private var cardBorder: Color {
        WakeveTheme.ColorToken.cardBorder(for: colorScheme)
    }

    private func loadParticipants() {
        if let previewParticipants {
            participantRows = makeFallbackRows(from: previewParticipants)
            return
        }

        if let participantRecords = repository?.getParticipantRecords(eventId: event.id), !participantRecords.isEmpty {
            let participantAccessStates = participantRecords.map { record in
                ParticipantAccessMapper.shared.fromRepositoryRecord(record: record)
            }
            participantRows = ParticipantManagementPresentationMapper.shared
                .map(participants: participantAccessStates)
                .map { ParticipantPresentationRow(sharedRow: $0) }
            return
        }

        let fallbackParticipants = repository?.getParticipants(eventId: event.id) ?? event.participants
        participantRows = makeFallbackRows(from: fallbackParticipants)
    }

    private func makeFallbackRows(from participants: [String]) -> [ParticipantPresentationRow] {
        participants.map { participant in
            ParticipantPresentationRow(
                id: participant,
                email: participant,
                roleLabel: participant == event.organizerId ? "Organizer" : "Member",
                statusLabel: participant == event.organizerId ? "Confirmed" : "Pending",
                canAccessOrganizationDetails: participant == event.organizerId
            )
        }
    }

    private func addParticipant() async {
        guard !newParticipantEmail.isEmpty else { return }

        // Basic email validation
        let emailRegex = "[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}"
        let emailPredicate = NSPredicate(format:"SELF MATCHES %@", emailRegex)

        guard emailPredicate.evaluate(with: newParticipantEmail) else {
            errorMessage = "Saisissez une adresse email valide"
            showError = true
            return
        }

        isLoading = true

        guard let repository else {
            participantRows.append(
                ParticipantPresentationRow(
                    id: newParticipantEmail,
                    email: newParticipantEmail,
                    roleLabel: "Member",
                    statusLabel: "Pending",
                    canAccessOrganizationDetails: false
                )
            )
            newParticipantEmail = ""
            isLoading = false
            onParticipantsUpdated()
            return
        }

        do {
            let participantEmail = newParticipantEmail
            _ = try await repository.addParticipant(eventId: event.id, participantId: participantEmail)
            loadParticipants()

            if participantRows.contains(where: { $0.email == participantEmail }) {
                isLoading = false
                newParticipantEmail = ""
                onParticipantsUpdated()
            } else {
                isLoading = false
                errorMessage = "Impossible d’ajouter ce participant"
                showError = true
            }
        } catch {
            isLoading = false
            errorMessage = error.localizedDescription
            showError = true
        }
    }

    private func startPoll() async {
        guard canStartPoll else {
            errorMessage = event.proposedSlots.isEmpty
                ? "Ajoutez au moins un créneau avant de lancer le sondage"
                : "Ajoutez au moins un participant avant de lancer le sondage"
            showError = true
            return
        }

        isLoading = true

        guard let repository else {
            isLoading = false
            showSuccess = true
            onParticipantsUpdated()
            return
        }

        do {
            _ = try await repository.updateEventStatus(
                id: event.id,
                status: EventStatus.polling,
                finalDate: nil
            )
            let updatedEvent = repository.getEvent(id: event.id)

            if updatedEvent?.status == EventStatus.polling {
                isLoading = false
                showSuccess = true
                onParticipantsUpdated()
            } else {
                isLoading = false
                errorMessage = "Impossible de lancer le sondage"
                showError = true
            }
        } catch {
            isLoading = false
            errorMessage = error.localizedDescription
            showError = true
        }
    }
}

#if DEBUG
#Preview("Participants - Draft") {
    ParticipantManagementView(
        event: EventFactory.make(
            title: "Anniversaire sur le rooftop",
            description: "Preparation du sondage avant invitation.",
            participants: [
                "marie@example.com",
                "lucas@example.com",
                "ines@example.com"
            ],
            status: .draft,
            eventType: .birthday,
            minParticipants: 3,
            maxParticipants: 12,
            expectedParticipants: 8
        ),
        previewParticipants: [
            "marie@example.com",
            "lucas@example.com",
            "ines@example.com"
        ]
    )
    .preferredColorScheme(.light)
}

#Preview("Participants - Confirmed") {
    ParticipantManagementView(
        event: EventFactory.withManyParticipants,
        previewParticipants: [
            "alexandre@example.com",
            "camille@example.com",
            "nora@example.com",
            "samir@example.com",
            "julie@example.com",
            "theo@example.com"
        ]
    )
    .preferredColorScheme(.dark)
}
#endif

// MARK: - Participant Row

private struct ParticipantPresentationRow: Identifiable, Equatable {
    let id: String
    let email: String
    let roleLabel: String
    let statusLabel: String
    let canAccessOrganizationDetails: Bool

    init(
        id: String,
        email: String,
        roleLabel: String,
        statusLabel: String,
        canAccessOrganizationDetails: Bool
    ) {
        self.id = id
        self.email = email
        self.roleLabel = roleLabel
        self.statusLabel = statusLabel
        self.canAccessOrganizationDetails = canAccessOrganizationDetails
    }

    init(sharedRow: ParticipantManagementRow) {
        self.id = sharedRow.userIdOrEmail
        self.email = sharedRow.userIdOrEmail
        self.roleLabel = sharedRow.roleLabel
        self.statusLabel = sharedRow.statusLabel
        self.canAccessOrganizationDetails = sharedRow.canAccessOrganizationDetails
    }
}

private struct ParticipantRowView: View {
    let participant: ParticipantPresentationRow

    private var initials: String {
        let components = participant.email.components(separatedBy: "@")
        if let name = components.first, !name.isEmpty {
            return String(name.prefix(1).uppercased())
        }
        return String(participant.email.prefix(1).uppercased())
    }

    private var statusColor: Color {
        switch participant.statusLabel {
        case "Confirmed":
            return WakeveColors.success
        case "Declined":
            return WakeveColors.error
        default:
            return WakeveColors.warning
        }
    }

    private var accessIconName: String {
        participant.canAccessOrganizationDetails ? "lock.open.fill" : "lock.fill"
    }

    private var accessColor: Color {
        participant.canAccessOrganizationDetails ? WakeveColors.success : WakeveColors.onSurfaceVariant
    }

    private var subtitleText: String {
        "\(participant.roleLabel) · \(participant.statusLabel)"
    }

    var body: some View {
        WakeveGlassCard(cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
            WakeveListRow(
                title: participant.email,
                subtitle: subtitleText,
                leading: {
                    WakeveAvatar(initials: initials, size: 52)
                },
                trailing: {
                    HStack(spacing: 10) {
                        Circle()
                            .fill(statusColor)
                            .frame(width: 9, height: 9)

                        Image(systemName: accessIconName)
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(accessColor)
                            .frame(width: 24, height: 24)
                    }
                    .accessibilityLabel(participant.canAccessOrganizationDetails ? "Détails déverrouillés" : "Détails verrouillés")
                }
            )
        }
    }
}
