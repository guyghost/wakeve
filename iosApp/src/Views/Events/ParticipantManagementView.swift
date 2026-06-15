import Contacts
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
    @State private var showStartPollConfirmation = false
    @State private var contactCandidates: [DeviceContactParticipantCandidate] = []
    @State private var selectedContactEmails: Set<String> = []
    @State private var contactSearchQuery = ""
    @State private var showContactSheet = false
    @State private var showContactPermissionRationale = false
    @State private var isLoadingContacts = false
    @State private var moderationTarget: ModerationActionTarget?

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
        ZStack {
                pageBackground
                    .ignoresSafeArea()

                ScrollView(showsIndicators: false) {
                    VStack(alignment: .leading, spacing: WakeveTheme.Spacing.lg) {
                        headerSummary
                        statusSummary

                        if event.status == .draft {
                            addParticipantCard
                        }

                        participantsContent
                    }
                    .padding(.horizontal, WakeveTheme.Spacing.page)
                    .padding(.top, 92)
                    .padding(.bottom, event.status == .draft ? 124 : 44)
                }
            }
        .safeAreaInset(edge: .top, spacing: 0) {
            topControls
        }
            .safeAreaInset(edge: .bottom, spacing: 0) {
                if event.status == .draft {
                    bottomActionBar
                }
            }
        .toolbar(.hidden, for: .tabBar)
        .onAppear {
            loadParticipants()
        }
        .alert(String(localized: "common.error"), isPresented: $showError) {
            Button(String(localized: "common.ok"), role: .cancel) {}
        } message: {
            Text(errorMessage)
        }
        .alert(String(localized: "participants.poll_started.title"), isPresented: $showSuccess) {
            Button(String(localized: "common.ok"), role: .cancel) {
                onBack()
            }
        } message: {
            Text(String(localized: "participants.poll_started.message"))
        }
        .confirmationDialog(
            String(localized: "participants.start_poll.title"),
            isPresented: $showStartPollConfirmation,
            titleVisibility: .visible
        ) {
            Button(String(localized: "participants.start_poll.action")) {
                Task { await startPoll() }
            }
            Button(String(localized: "common.cancel"), role: .cancel) {}
        } message: {
            Text(String(localized: "participants.start_poll.message"))
        }
        .confirmationDialog(
            String(localized: "participants.contacts_permission.title"),
            isPresented: $showContactPermissionRationale,
            titleVisibility: .visible
        ) {
            Button(String(localized: "participants.contacts_permission.continue")) {
                Task { await loadContacts() }
            }
            Button(String(localized: "common.cancel"), role: .cancel) {}
        } message: {
            Text(String(localized: "participants.contacts_permission.message"))
        }
        .sheet(isPresented: $showContactSheet) {
            ContactParticipantSelectionSheet(
                contacts: contactCandidates,
                selectedEmails: $selectedContactEmails,
                searchQuery: $contactSearchQuery,
                existingEmails: Set(participantRows.compactMap { normalizedEmail($0.email) }),
                onAddSelected: {
                    Task { await addSelectedContacts() }
                }
            )
        }
        .sheet(item: $moderationTarget) { target in
            ModerationActionSheet(target: target)
        }
    }

    private var headerSummary: some View {
        EventHeroCard(
            title: String(localized: "participants.title"),
            subtitle: event.title,
            metadata: statusTitle,
            gradient: WakeveTheme.EventGradient.invitation
        ) {
            HStack(spacing: WakeveTheme.Spacing.sm) {
                ParticipantSummaryPill(title: String(localized: "participants.accepted"), value: "\(acceptedRows.count)")
                ParticipantSummaryPill(title: String(localized: "participants.pending"), value: "\(pendingRows.count)")
                ParticipantSummaryPill(title: String(localized: "participants.declined"), value: "\(declinedRows.count)")
            }
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

                Text(String(localized: "participants.title"))
                    .font(.system(size: 42, weight: .bold))
                    .foregroundColor(.white)
                    .lineLimit(2)
                    .minimumScaleFactor(0.74)

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
                    accessibilityLabel: String(localized: "common.close"),
                    variant: .glass,
                    size: 48,
                    action: onBack
                )

                Spacer()

                Menu {
                    Button {
                        newParticipantEmail = ""
                    } label: {
                        Label(String(localized: "participants.reset_invitation"), systemImage: "arrow.counterclockwise")
                    }

                if canStartPoll {
                    Button {
                        showStartPollConfirmation = true
                    } label: {
                        Label(String(localized: "participants.start_poll.action"), systemImage: "chart.bar.xaxis")
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
                .accessibilityLabel(String(localized: "participants.options_accessibility"))
            }
            .padding(.horizontal, WakeveTheme.Spacing.page)
            .padding(.top, WakeveTheme.Navigation.controlTopPadding(safeAreaTop: topInset))

            Spacer()
        }
    }

    private var topControls: some View {
        LiquidGlassToolbar(title: String(localized: "participants.title"), subtitle: participantCountText) {
            WakeveCircleButton(
                systemImage: "xmark",
                accessibilityLabel: String(localized: "common.close"),
                variant: .glass,
                size: 40,
                action: onBack
            )
        } trailing: {
            Menu {
                Button {
                    newParticipantEmail = ""
                } label: {
                    Label(String(localized: "participants.reset_invitation"), systemImage: "arrow.counterclockwise")
                }

                    if canStartPoll {
                        Button {
                            showStartPollConfirmation = true
                        } label: {
                            Label(String(localized: "participants.start_poll.action"), systemImage: "chart.bar.xaxis")
                        }
                }
            } label: {
                Image(systemName: "ellipsis")
                    .font(.body.weight(.bold))
                    .foregroundColor(primaryText)
                    .frame(width: 40, height: 40)
                    .background(WakeveTheme.ColorToken.controlFill(for: colorScheme))
                    .clipShape(Circle())
            }
            .accessibilityLabel(String(localized: "participants.options_accessibility"))
        }
        .padding(.horizontal, WakeveTheme.Spacing.page)
        .padding(.top, WakeveTheme.Spacing.sm)
        .padding(.bottom, WakeveTheme.Spacing.xs)
        .background(
            LinearGradient(
                colors: [
                    pageBackground,
                    pageBackground.opacity(0)
                ],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea(edges: .top)
        )
    }

    private var addParticipantCard: some View {
        WakeveContentCard(prominence: .regular, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
                Text(String(localized: "participants.invite_one"))
                    .font(WakeveTheme.Typography.section)
                    .foregroundColor(primaryText)

                HStack(spacing: 12) {
                    TextField(String(localized: "participants.email_placeholder"), text: $newParticipantEmail)
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
                                .background(SemanticColor.selectedState(for: colorScheme))
                                .clipShape(Circle())
                                .accessibilityHidden(true)
                        } else {
                            Image(systemName: "plus")
                                .font(.system(size: 24, weight: .bold))
                                .foregroundColor(.white)
                                .frame(width: 52, height: 52)
                                .background(newParticipantEmail.isEmpty ? Color.gray.opacity(0.35) : SemanticColor.selectedState(for: colorScheme))
                                .clipShape(Circle())
                        }
                    }
                    .disabled(newParticipantEmail.isEmpty || isLoading)
                    .accessibilityLabel(String(localized: "participants.add_accessibility"))
                }

                LiquidGlassButton(
                    String(localized: "participants.choose_from_contacts"),
                    systemImage: "person.crop.circle.badge.plus",
                    variant: .secondary,
                    isDisabled: isLoadingContacts,
                    isLoading: isLoadingContacts
                ) {
                    beginContactSelection()
                }
                .accessibilityLabel(String(localized: "participants.choose_from_contacts_accessibility"))
            }
        }
    }

    private var statusSummary: some View {
        WakeveContentCard(prominence: .subtle, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
            HStack(spacing: WakeveTheme.Spacing.md) {
                Image(systemName: statusIcon)
                    .font(.title3.weight(.bold))
                    .foregroundColor(statusColor)
                    .frame(width: 46, height: 46)
                    .background(statusColor.opacity(0.14))
                    .clipShape(Circle())

                VStack(alignment: .leading, spacing: WakeveTheme.Spacing.xxs) {
                    Text(participantCountText)
                        .font(WakeveTheme.Typography.rowTitle)
                        .foregroundColor(primaryText)

                    Text(statusText)
                        .font(WakeveTheme.Typography.callout)
                        .foregroundColor(secondaryText)
                        .lineLimit(2)
                }

                Spacer()
            }
        }
    }

    @ViewBuilder
    private var participantsContent: some View {
        if participantRows.isEmpty {
            EmptyState(
                systemImage: "person.crop.circle.badge.plus",
                title: String(localized: "participants.empty.title"),
                subtitle: event.status == .draft ? String(localized: "participants.empty.draft_subtitle") : String(localized: "participants.empty.joined_subtitle")
            )
        } else {
            VStack(spacing: WakeveTheme.Spacing.md) {
                if !acceptedRows.isEmpty {
                    ParticipantGroupSection(
                        title: String(localized: "participants.accepted"),
                        subtitle: String(localized: "participants.accepted_subtitle"),
                        rows: acceptedRows,
                        emptyText: String(localized: "participants.empty.accepted"),
                        eventId: event.id,
                        onModerationTarget: { moderationTarget = $0 }
                    )
                }

                if !pendingRows.isEmpty {
                    ParticipantGroupSection(
                        title: String(localized: "participants.pending"),
                        subtitle: String(localized: "participants.pending_subtitle"),
                        rows: pendingRows,
                        emptyText: String(localized: "participants.empty.pending"),
                        eventId: event.id,
                        onModerationTarget: { moderationTarget = $0 }
                    )
                }

                if !declinedRows.isEmpty {
                    ParticipantGroupSection(
                        title: String(localized: "participants.declined"),
                        subtitle: String(localized: "participants.declined_subtitle"),
                        rows: declinedRows,
                        emptyText: String(localized: "participants.empty.declined"),
                        eventId: event.id,
                        onModerationTarget: { moderationTarget = $0 }
                    )
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

            LiquidGlassButton(
                String(localized: "participants.start_poll.action"),
                systemImage: "chart.bar.xaxis",
                variant: .primary,
                isDisabled: !canStartPoll,
                isLoading: isLoading
            ) {
                showStartPollConfirmation = true
            }
            .padding(.horizontal, WakeveTheme.Spacing.page)
            .padding(.top, WakeveTheme.Spacing.xs)
            .padding(.bottom, WakeveTheme.Spacing.md)
            .background(pageBackground)
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
        case .polling: return String(localized: "participants.status.polling")
        case .confirmed: return String(localized: "participants.status.confirmed")
        default: return String(localized: "participants.status.unavailable")
        }
    }

    private var statusTitle: String {
        switch event.status {
        case .draft: return String(localized: "participants.status_title.draft")
        case .polling: return String(localized: "participants.status.polling")
        case .confirmed: return String(localized: "participants.status_title.confirmed")
        default: return String(localized: "participants.title")
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

    private var acceptedRows: [ParticipantPresentationRow] {
        participantRows.filter { participant in
            participant.status == .accepted || participant.canAccessOrganizationDetails
        }
    }

    private var pendingRows: [ParticipantPresentationRow] {
        participantRows.filter { participant in
            participant.status == .pending && !participant.canAccessOrganizationDetails
        }
    }

    private var declinedRows: [ParticipantPresentationRow] {
        participantRows.filter { participant in
            participant.status == .declined
        }
    }

    private var canStartPoll: Bool {
        event.status == .draft && !participantRows.isEmpty && !event.proposedSlots.isEmpty
    }

    private var draftStatusText: String {
        if participantRows.isEmpty {
            return String(localized: "participants.draft_status.no_participants")
        }

        if event.proposedSlots.isEmpty {
            return String(localized: "participants.draft_status.no_slots")
        }

        return String(localized: "participants.draft_status.ready")
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
            let isOrganizer = participant == event.organizerId
            return ParticipantPresentationRow(
                id: participant,
                email: participant,
                roleLabel: isOrganizer ? String(localized: "participants.role.organizer") : String(localized: "participants.role.member"),
                statusLabel: isOrganizer ? String(localized: "participants.status.confirmed_short") : String(localized: "participants.pending"),
                status: isOrganizer ? .accepted : .pending,
                canAccessOrganizationDetails: isOrganizer
            )
        }
    }

    private func addParticipant() async {
        guard !newParticipantEmail.isEmpty else { return }

        // Basic email validation
        guard let participantEmail = normalizedEmail(newParticipantEmail) else {
            errorMessage = "Saisissez une adresse email valide"
            showError = true
            return
        }

        isLoading = true

        guard let repository else {
            participantRows.append(
                ParticipantPresentationRow(
                    id: participantEmail,
                    email: participantEmail,
                    roleLabel: String(localized: "participants.role.member"),
                    statusLabel: String(localized: "participants.pending"),
                    status: .pending,
                    canAccessOrganizationDetails: false
                )
            )
            newParticipantEmail = ""
            isLoading = false
            onParticipantsUpdated()
            return
        }

        do {
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

    private func loadContacts() async {
        isLoadingContacts = true
        do {
            let contacts = try await DeviceContactParticipantLoader.loadContacts()
            let normalizedContacts = contacts.compactMap { contact -> DeviceContactParticipantCandidate? in
                guard let email = normalizedEmail(contact.email) else { return nil }
                return DeviceContactParticipantCandidate(
                    displayName: contact.displayName.isEmpty ? email : contact.displayName,
                    email: email
                )
            }
            .uniquedByEmail()

            guard !normalizedContacts.isEmpty else {
                isLoadingContacts = false
                errorMessage = String(localized: "participants.error.no_email_contacts")
                showError = true
                return
            }

            contactCandidates = normalizedContacts
            selectedContactEmails = []
            contactSearchQuery = ""
            isLoadingContacts = false
            showContactSheet = true
        } catch {
            isLoadingContacts = false
            errorMessage = error.localizedDescription
            showError = true
        }
    }

    private func beginContactSelection() {
        switch CNContactStore.authorizationStatus(for: .contacts) {
        case .authorized, .limited:
            Task { await loadContacts() }
        default:
            showContactPermissionRationale = true
        }
    }

    private func addSelectedContacts() async {
        let existingEmails = Set(participantRows.compactMap { normalizedEmail($0.email) })
        let emailsToAdd = selectedContactEmails
            .compactMap(normalizedEmail)
            .filter { !existingEmails.contains($0) }
            .sorted()

        guard !emailsToAdd.isEmpty else {
            showContactSheet = false
            errorMessage = String(localized: "participants.error.no_new_participants")
            showError = true
            return
        }

        isLoading = true
        var failedEmails: [String] = []

        if let repository {
            for email in emailsToAdd {
                do {
                    _ = try await repository.addParticipant(eventId: event.id, participantId: email)
                } catch {
                    failedEmails.append(email)
                }
            }
            loadParticipants()
        } else {
            let existingPreviewEmails = Set(participantRows.map(\.email))
            let rows = emailsToAdd
                .filter { !existingPreviewEmails.contains($0) }
                .map {
                    ParticipantPresentationRow(
                        id: $0,
                        email: $0,
                        roleLabel: String(localized: "participants.role.member"),
                        statusLabel: String(localized: "participants.pending"),
                        status: .pending,
                        canAccessOrganizationDetails: false
                    )
                }
            participantRows.append(contentsOf: rows)
        }

        isLoading = false
        showContactSheet = false
        selectedContactEmails = []
        contactSearchQuery = ""
        onParticipantsUpdated()

        if !failedEmails.isEmpty {
            errorMessage = "Impossible d'ajouter \(failedEmails.count) participant(s)"
            showError = true
        }
    }

    private func normalizedEmail(_ email: String) -> String? {
        let normalized = email.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard !normalized.isEmpty else { return nil }
        let parts = normalized.split(separator: "@")
        guard parts.count == 2, !parts[0].isEmpty, parts[1].contains(".") else { return nil }
        return normalized
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

// MARK: - Contact Selection

private struct DeviceContactParticipantCandidate: Identifiable, Equatable {
    var id: String { email }
    let displayName: String
    let email: String
}

private enum DeviceContactParticipantLoader {
    static func loadContacts() async throws -> [DeviceContactParticipantCandidate] {
        let store = CNContactStore()
        let status = CNContactStore.authorizationStatus(for: .contacts)

        switch status {
        case .notDetermined:
            let granted = try await requestAccess(store: store)
            guard granted else { throw ContactParticipantError.permissionDenied }
        case .authorized, .limited:
            break
        case .denied, .restricted:
            throw ContactParticipantError.permissionDenied
        @unknown default:
            throw ContactParticipantError.permissionDenied
        }

        let keys = [
            CNContactGivenNameKey,
            CNContactFamilyNameKey,
            CNContactEmailAddressesKey
        ] as [CNKeyDescriptor]
        let request = CNContactFetchRequest(keysToFetch: keys)
        var candidates: [DeviceContactParticipantCandidate] = []

        try store.enumerateContacts(with: request) { contact, _ in
            let displayName = [contact.givenName, contact.familyName]
                .filter { !$0.isEmpty }
                .joined(separator: " ")
            contact.emailAddresses.forEach { labeledEmail in
                let email = String(labeledEmail.value)
                candidates.append(
                    DeviceContactParticipantCandidate(
                        displayName: displayName.isEmpty ? email : displayName,
                        email: email
                    )
                )
            }
        }

        return candidates
    }

    private static func requestAccess(store: CNContactStore) async throws -> Bool {
        try await withCheckedThrowingContinuation { continuation in
            store.requestAccess(for: .contacts) { granted, error in
                if let error {
                    continuation.resume(throwing: error)
                } else {
                    continuation.resume(returning: granted)
                }
            }
        }
    }
}

private enum ContactParticipantError: LocalizedError {
    case permissionDenied

    var errorDescription: String? {
        switch self {
        case .permissionDenied:
            return String(localized: "participants.error.contacts_permission_denied")
        }
    }
}

private struct ContactParticipantSelectionSheet: View {
    let contacts: [DeviceContactParticipantCandidate]
    @Binding var selectedEmails: Set<String>
    @Binding var searchQuery: String
    let existingEmails: Set<String>
    let onAddSelected: () -> Void

    @Environment(\.dismiss) private var dismiss

    private var filteredContacts: [DeviceContactParticipantCandidate] {
        let query = searchQuery.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !query.isEmpty else { return contacts }
        return contacts.filter {
            $0.displayName.localizedCaseInsensitiveContains(query) ||
                $0.email.localizedCaseInsensitiveContains(query)
        }
    }

    var body: some View {
        NavigationStack {
            List(filteredContacts) { contact in
                let alreadyAdded = existingEmails.contains(contact.email)
                Button {
                    guard !alreadyAdded else { return }
                    if selectedEmails.contains(contact.email) {
                        selectedEmails.remove(contact.email)
                    } else {
                        selectedEmails.insert(contact.email)
                    }
                } label: {
                    HStack(spacing: 12) {
                        Image(systemName: selectedEmails.contains(contact.email) ? "checkmark.circle.fill" : "circle")
                            .foregroundColor(alreadyAdded ? .secondary : .blue)
                        VStack(alignment: .leading, spacing: 3) {
                            Text(contact.displayName)
                                .foregroundColor(.primary)
                            Text(alreadyAdded ? String(format: String(localized: "participants.contact_already_added_format"), contact.email) : contact.email)
                                .font(.footnote)
                                .foregroundColor(.secondary)
                        }
                    }
                }
                .disabled(alreadyAdded)
            }
            .searchable(text: $searchQuery, prompt: String(localized: "common.search"))
            .navigationTitle(String(localized: "participants.contacts"))
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(String(localized: "common.cancel")) { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(String(format: String(localized: "participants.add_selected_format"), selectedEmails.count)) {
                        onAddSelected()
                    }
                    .disabled(selectedEmails.isEmpty)
                }
            }
        }
    }
}

private extension Array where Element == DeviceContactParticipantCandidate {
    func uniquedByEmail() -> [DeviceContactParticipantCandidate] {
        var seen = Set<String>()
        return filter { contact in
            guard !seen.contains(contact.email) else { return false }
            seen.insert(contact.email)
            return true
        }
        .sorted {
            if $0.displayName == $1.displayName {
                return $0.email < $1.email
            }
            return $0.displayName.localizedCaseInsensitiveCompare($1.displayName) == .orderedAscending
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
    let status: ParticipantDisplayStatus
    let canAccessOrganizationDetails: Bool

    init(
        id: String,
        email: String,
        roleLabel: String,
        statusLabel: String,
        status: ParticipantDisplayStatus? = nil,
        canAccessOrganizationDetails: Bool
    ) {
        self.id = id
        self.email = email
        self.roleLabel = roleLabel
        self.statusLabel = statusLabel
        self.status = status ?? ParticipantDisplayStatus(statusLabel: statusLabel, canAccessOrganizationDetails: canAccessOrganizationDetails)
        self.canAccessOrganizationDetails = canAccessOrganizationDetails
    }

    init(sharedRow: ParticipantManagementRow) {
        self.id = sharedRow.userIdOrEmail
        self.email = sharedRow.userIdOrEmail
        self.roleLabel = sharedRow.roleLabel
        self.statusLabel = sharedRow.statusLabel
        self.status = ParticipantDisplayStatus(statusLabel: sharedRow.statusLabel, canAccessOrganizationDetails: sharedRow.canAccessOrganizationDetails)
        self.canAccessOrganizationDetails = sharedRow.canAccessOrganizationDetails
    }
}

private enum ParticipantDisplayStatus: Equatable {
    case accepted
    case pending
    case declined

    init(statusLabel: String, canAccessOrganizationDetails: Bool) {
        let normalized = statusLabel
            .folding(options: [.diacriticInsensitive, .caseInsensitive], locale: .current)
            .lowercased()

        if normalized.contains("declined") || normalized.contains("refuse") {
            self = .declined
        } else if canAccessOrganizationDetails || normalized.contains("confirmed") || normalized.contains("confirme") {
            self = .accepted
        } else {
            self = .pending
        }
    }
}

private struct ParticipantSummaryPill: View {
    let title: String
    let value: String

    var body: some View {
        VStack(spacing: WakeveTheme.Spacing.xxs) {
            Text(value)
                .font(WakeveTheme.Typography.rowTitle)
                .foregroundColor(.white)
            Text(title)
                .font(WakeveTheme.Typography.tiny)
                .foregroundColor(.white.opacity(0.74))
                .lineLimit(1)
                .minimumScaleFactor(0.78)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, WakeveTheme.Spacing.sm)
        .background(Color.black.opacity(0.24))
        .clipShape(RoundedRectangle(cornerRadius: WakeveTheme.Radius.md, style: .continuous))
    }
}

private struct ParticipantGroupSection: View {
    @Environment(\.colorScheme) private var colorScheme

    let title: String
    let subtitle: String
    let rows: [ParticipantPresentationRow]
    let emptyText: String
    let eventId: String
    let onModerationTarget: (ModerationActionTarget) -> Void

    var body: some View {
        WakeveGroupCard(title: title, subtitle: subtitle, count: rows.count) {
            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
                if rows.isEmpty {
                    Text(emptyText)
                        .font(TypographyTokens.callout)
                        .foregroundColor(SemanticColor.secondaryText(for: colorScheme))
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(WakeveTheme.Spacing.sm)
                        .background(SemanticColor.badge(for: colorScheme))
                        .clipShape(RoundedRectangle(cornerRadius: WakeveTheme.Radius.md, style: .continuous))
                } else {
                    VStack(spacing: WakeveTheme.Spacing.sm) {
                        ForEach(rows) { participant in
                            ParticipantRowView(
                                participant: participant,
                                eventId: eventId,
                                onModerationTarget: onModerationTarget
                            )
                        }
                    }
                }
            }
        }
    }
}

private struct ParticipantRowView: View {
    @Environment(\.colorScheme) private var colorScheme

    let participant: ParticipantPresentationRow
    let eventId: String
    let onModerationTarget: (ModerationActionTarget) -> Void

    private var initials: String {
        let components = participant.email.components(separatedBy: "@")
        if let name = components.first, !name.isEmpty {
            return String(name.prefix(1).uppercased())
        }
        return String(participant.email.prefix(1).uppercased())
    }

    private var statusColor: Color {
        switch participant.status {
        case .accepted:
            return WakeveColors.success
        case .declined:
            return WakeveColors.error
        case .pending:
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
        HStack(spacing: WakeveTheme.Spacing.md) {
            WakeveAvatar(initials: initials, size: 44)

            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.xxs) {
                Text(participant.email)
                    .font(WakeveTheme.Typography.bodySemibold)
                    .foregroundColor(WakeveTheme.ColorToken.primaryText(for: colorScheme))
                    .lineLimit(1)
                    .minimumScaleFactor(0.78)

                Text(subtitleText)
                    .font(WakeveTheme.Typography.callout)
                    .foregroundColor(WakeveTheme.ColorToken.secondaryText(for: colorScheme))
                    .lineLimit(1)
                    .minimumScaleFactor(0.78)
            }

            Spacer()

            HStack(spacing: WakeveTheme.Spacing.xs) {
                Circle()
                    .fill(statusColor)
                    .frame(width: 9, height: 9)

                Image(systemName: accessIconName)
                    .font(.body.weight(.semibold))
                    .foregroundColor(accessColor)
                    .frame(width: 24, height: 24)

                participantModerationMenu
            }
            .accessibilityLabel(participant.canAccessOrganizationDetails ? String(localized: "participants.details_unlocked_accessibility") : String(localized: "participants.details_locked_accessibility"))
        }
        .padding(WakeveTheme.Spacing.sm)
        .background(WakeveTheme.ColorToken.controlFill(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: WakeveTheme.Radius.md, style: .continuous))
    }

    private var participantModerationMenu: some View {
        Menu {
            Button {
                onModerationTarget(
                    ModerationActionTarget(
                        type: .user,
                        targetId: participant.id,
                        eventId: eventId,
                        authorId: participant.id,
                        displayName: participant.email,
                        allowsBlock: true
                    )
                )
            } label: {
                Label(String(localized: "moderation.report_user"), systemImage: "person.crop.circle.badge.exclamationmark")
            }
            .accessibilityIdentifier("reportParticipantUserAction")

            Button(role: .destructive) {
                onModerationTarget(
                    ModerationActionTarget(
                        type: .user,
                        targetId: participant.id,
                        eventId: eventId,
                        authorId: participant.id,
                        displayName: participant.email,
                        allowsBlock: true
                    )
                )
            } label: {
                Label(String(localized: "moderation.block_user"), systemImage: "person.crop.circle.badge.xmark")
            }
            .accessibilityIdentifier("blockParticipantUserAction")
        } label: {
            Image(systemName: "ellipsis.circle")
                .font(.body.weight(.semibold))
                .foregroundColor(WakeveTheme.ColorToken.secondaryText(for: colorScheme))
                .frame(width: 28, height: 28)
        }
    }
}
