import Contacts
import SwiftUI
import Shared
#if canImport(UIKit)
import UIKit
#endif

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
    @State private var showInvitationShareSheet = false
    @State private var showCopiedInvitationMessage = false
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
                        groupPresenceCard
                        if hasTimezoneCoordinationContext {
                            timezoneCoordinationCard
                        }

                        if event.status == .draft {
                            invitationShareCard
                            addParticipantCard
                        }

                        participantsContent
                    }
                    .padding(.horizontal, WakeveTheme.Spacing.page)
                    .padding(.top, WakeveTheme.Spacing.md)
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
        .sheet(isPresented: $showInvitationShareSheet) {
            InvitationShareSheet(
                eventId: event.id,
                eventTitle: event.title,
                invitationCode: invitationCode,
                onDismiss: {
                    showInvitationShareSheet = false
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

                    Button {
                        showInvitationShareSheet = true
                    } label: {
                        Label(String(localized: "invitation.share"), systemImage: "square.and.arrow.up")
                    }

                    if canStartPoll {
                        Button {
                            presentStartPollConfirmation()
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

                Button {
                    showInvitationShareSheet = true
                } label: {
                    Label(String(localized: "invitation.share"), systemImage: "square.and.arrow.up")
                }

                if canStartPoll {
                    Button {
                        presentStartPollConfirmation()
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

    private var invitationShareCard: some View {
        WakeveContentCard(prominence: .prominent, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
                WakeveInvitationPreviewCard(
                    title: event.title,
                    subtitle: invitationPosterSubtitle,
                    inviteUrl: invitationInviteUrl,
                    moodPalette: invitationMoodPalette,
                    qrImage: nil
                )

                HStack(alignment: .top, spacing: WakeveTheme.Spacing.md) {
                    Image(systemName: "square.and.arrow.up.circle.fill")
                        .font(.title2.weight(.bold))
                        .foregroundColor(SemanticColor.selectedState(for: colorScheme))
                        .frame(width: 46, height: 46)
                        .background(SemanticColor.badge(for: colorScheme))
                        .clipShape(Circle())

                    VStack(alignment: .leading, spacing: WakeveTheme.Spacing.xxs) {
                        Text(String(localized: "invitation.share"))
                            .font(WakeveTheme.Typography.section)
                            .foregroundColor(primaryText)

                        Text(invitationDeliveryStateText)
                            .font(WakeveTheme.Typography.callout)
                            .foregroundColor(secondaryText)
                            .lineLimit(3)
                    }

                    Spacer(minLength: WakeveTheme.Spacing.xs)
                }

                HStack(spacing: WakeveTheme.Spacing.sm) {
                    Text(invitationInviteUrl)
                        .font(.system(.caption, design: .monospaced))
                        .foregroundColor(secondaryText)
                        .lineLimit(1)
                        .truncationMode(.middle)
                        .padding(.horizontal, WakeveTheme.Spacing.md)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .frame(height: 44)
                        .background(inputBackground)
                        .clipShape(RoundedRectangle(cornerRadius: WakeveTheme.Radius.md, style: .continuous))

                    HStack(spacing: WakeveTheme.Spacing.xs) {
                        Button(action: copyInvitationMessage) {
                            Image(systemName: showCopiedInvitationMessage ? "checkmark" : "doc.on.doc")
                                .font(.headline.weight(.bold))
                                .foregroundColor(showCopiedInvitationMessage ? SemanticColor.confirmation(for: colorScheme) : SemanticColor.selectedState(for: colorScheme))
                                .frame(width: 44, height: 44)
                                .background(SemanticColor.badge(for: colorScheme))
                                .clipShape(Circle())
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel(showCopiedInvitationMessage ? String(localized: "invitation.copied_message") : String(localized: "invitation.copy_message"))

                        ShareLink(item: invitationShareMessage) {
                            Label(String(localized: "invitation.share_whatsapp_messages"), systemImage: "paperplane.fill")
                                .labelStyle(.iconOnly)
                                .font(.headline.weight(.bold))
                                .foregroundColor(.white)
                                .frame(width: 44, height: 44)
                                .background(SemanticColor.selectedState(for: colorScheme))
                                .clipShape(Circle())
                        }
                        .accessibilityLabel(String(localized: "invitation.share_whatsapp_messages"))
                    }
                }

                Button {
                    showInvitationShareSheet = true
                } label: {
                    Label(String(localized: "invitation.preview_poster"), systemImage: "qrcode")
                        .font(WakeveTheme.Typography.bodySemibold)
                        .foregroundColor(SemanticColor.selectedState(for: colorScheme))
                        .frame(maxWidth: .infinity)
                        .frame(height: 46)
                        .background(SemanticColor.badge(for: colorScheme))
                        .clipShape(RoundedRectangle(cornerRadius: WakeveTheme.Radius.md, style: .continuous))
                }
                .buttonStyle(.plain)
            }
        }
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

    private var groupPresenceCard: some View {
        ParticipantPresenceCard(
            rows: participantRows,
            acceptedCount: acceptedRows.count,
            pendingCount: pendingRows.count,
            declinedCount: declinedRows.count,
            subtitle: groupPresenceSubtitle,
            nextAction: groupPresenceNextAction,
            progress: groupPresenceProgress
        )
    }

    private var timezoneCoordinationCard: some View {
        ParticipantTimezoneCoordinationCard(
            subtitle: timezoneCoordinationSubtitle,
            rows: participantTimezoneRows,
            footnote: String(localized: "participants.timezone.footnote")
        )
        .accessibilityIdentifier("participantTimezoneCoordinationCard")
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
                presentStartPollConfirmation()
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
        if participantRows.count == 1 {
            return String(format: String(localized: "participants.count.singular_format"), participantRows.count)
        }

        return String(format: String(localized: "participants.count.plural_format"), participantRows.count)
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
        event.status == .draft && !event.proposedSlots.isEmpty
    }

    private var hasTimezoneCoordinationContext: Bool {
        !event.proposedSlots.isEmpty
    }

    private var eventTimezoneIdentifiers: [String] {
        Array(
            Set(
                event.proposedSlots
                    .map(\.timezone)
                    .filter { !$0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
            )
        )
        .sorted()
    }

    private var primaryEventTimezoneIdentifier: String {
        eventTimezoneIdentifiers.first ?? TimeZone.current.identifier
    }

    private var participantTimezoneRows: [ParticipantTimezoneRow] {
        [
            ParticipantTimezoneRow(
                id: "event",
                icon: "calendar.badge.clock",
                title: String(localized: "participants.timezone.event_label"),
                value: timezoneDisplayName(primaryEventTimezoneIdentifier)
            ),
            ParticipantTimezoneRow(
                id: "local",
                icon: "location.fill",
                title: String(localized: "participants.timezone.local_label"),
                value: timezoneDisplayName(TimeZone.current.identifier),
                badge: TimeZone.current.identifier == primaryEventTimezoneIdentifier
                    ? String(localized: "participants.timezone.same_badge")
                    : String(localized: "participants.timezone.local_badge")
            ),
            ParticipantTimezoneRow(
                id: "options",
                icon: "clock.badge.checkmark",
                title: String(localized: "participants.timezone.options_label"),
                value: String(
                    format: String(localized: "participants.timezone.options_format"),
                    event.proposedSlots.count
                )
            )
        ]
    }

    private var timezoneCoordinationSubtitle: String {
        if eventTimezoneIdentifiers.count > 1 {
            return String(
                format: String(localized: "participants.timezone.multi_subtitle_format"),
                eventTimezoneIdentifiers.count
            )
        }

        return String(
            format: String(localized: "participants.timezone.single_subtitle_format"),
            timezoneDisplayName(primaryEventTimezoneIdentifier)
        )
    }

    private var invitationCode: String {
        InvitationTokenCodec.invitationCode(forEventId: event.id)
    }

    private var invitationInviteUrl: String {
        "https://wakeve.app/invite/\(invitationCode)"
    }

    private var invitationMoodPalette: EventMoodPalette {
        EventMoodPalette.palette(for: event.eventType.name)
    }

    private var invitationPosterSubtitle: String {
        if let firstSlot = event.proposedSlots.first {
            let start = firstSlot.start ?? String(localized: "invitation.poster_subtitle")
            if let end = firstSlot.end, end != start {
                return "\(start) / \(end)"
            }
            return start
        }

        return String(localized: "invitation.poster_subtitle")
    }

    private var invitationShareMessage: String {
        String(
            format: String(localized: "invitation.social_share_text"),
            event.title,
            invitationPosterSubtitle,
            invitationInviteUrl
        )
    }

    private func copyInvitationMessage() {
        #if canImport(UIKit)
        UIPasteboard.general.string = invitationShareMessage
        #endif

        WakeveHaptics.success()

        withAnimation(.easeInOut(duration: 0.2)) {
            showCopiedInvitationMessage = true
        }

        DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
            withAnimation(.easeInOut(duration: 0.2)) {
                showCopiedInvitationMessage = false
            }
        }
    }

    private func presentStartPollConfirmation() {
        WakeveHaptics.selection()
        showStartPollConfirmation = true
    }

    private var invitationDeliveryStateText: String {
        if acceptedRows.isEmpty && pendingRows.isEmpty {
            return String(localized: "invitation.delivery_state.not_sent")
        }

        if acceptedRows.isEmpty {
            return String(
                format: String(localized: "invitation.delivery_state.pending_only"),
                pendingRows.count
            )
        }

        return String(
            format: String(localized: "invitation.delivery_state.with_acceptances"),
            acceptedRows.count,
            pendingRows.count
        )
    }

    private var groupPresenceSubtitle: String {
        if participantRows.isEmpty {
            return String(localized: "participants.presence.subtitle.empty")
        }

        if acceptedRows.isEmpty {
            return String(
                format: String(localized: "participants.presence.subtitle.pending_only"),
                pendingRows.count
            )
        }

        return String(
            format: String(localized: "participants.presence.subtitle.active"),
            acceptedRows.count,
            pendingRows.count,
            declinedRows.count
        )
    }

    private var groupPresenceNextAction: String {
        if participantRows.isEmpty {
            return String(localized: "participants.presence.next_action.share")
        }

        if !pendingRows.isEmpty {
            return String(localized: "participants.presence.next_action.nudge")
        }

        if event.status == .draft {
            return String(localized: "participants.presence.next_action.poll")
        }

        return String(localized: "participants.presence.next_action.ready")
    }

    private var groupPresenceProgress: Double {
        guard !participantRows.isEmpty else { return 0 }
        let resolvedCount = acceptedRows.count + declinedRows.count
        return Double(resolvedCount) / Double(participantRows.count)
    }

    private var draftStatusText: String {
        if event.proposedSlots.isEmpty {
            return String(localized: "participants.draft_status.no_slots")
        }

        if participantRows.isEmpty {
            return String(localized: "participants.draft_status.link_ready")
        }

        return String(localized: "participants.draft_status.ready")
    }

    private func timezoneDisplayName(_ identifier: String) -> String {
        let timeZone = TimeZone(identifier: identifier) ?? .current
        let name = timeZone.localizedName(for: .shortGeneric, locale: .autoupdatingCurrent) ?? timeZone.identifier
        return "\(name) · \(timeZone.identifier)"
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
            errorMessage = String(localized: "participants.error.invalid_email")
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
                errorMessage = String(localized: "participants.error.add_failed")
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
            errorMessage = String(
                format: String(localized: "participants.error.add_selected_failed_format"),
                failedEmails.count
            )
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
            WakeveHaptics.warning()
            errorMessage = String(localized: "participants.start_poll.requires_slot")
            showError = true
            return
        }

        isLoading = true

        guard let repository else {
            isLoading = false
            WakeveHaptics.success()
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
                WakeveHaptics.success()
                showSuccess = true
                onParticipantsUpdated()
            } else {
                isLoading = false
                WakeveHaptics.warning()
                errorMessage = String(localized: "participants.error.start_poll_failed")
                showError = true
            }
        } catch {
            isLoading = false
            WakeveHaptics.warning()
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

private struct ParticipantPresenceCard: View {
    @Environment(\.colorScheme) private var colorScheme

    let rows: [ParticipantPresentationRow]
    let acceptedCount: Int
    let pendingCount: Int
    let declinedCount: Int
    let subtitle: String
    let nextAction: String
    let progress: Double

    var body: some View {
        WakeveContentCard(prominence: .regular, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
                HStack(alignment: .top, spacing: WakeveTheme.Spacing.md) {
                    VStack(alignment: .leading, spacing: WakeveTheme.Spacing.xxs) {
                        Label(String(localized: "participants.presence.title"), systemImage: "person.2.wave.2.fill")
                            .font(WakeveTheme.Typography.section)
                            .foregroundColor(primaryText)

                        Text(subtitle)
                            .font(WakeveTheme.Typography.callout)
                            .foregroundColor(secondaryText)
                            .fixedSize(horizontal: false, vertical: true)
                    }

                    Spacer(minLength: WakeveTheme.Spacing.sm)

                    ParticipantFacePile(rows: Array(rows.prefix(5)))
                }

                VStack(alignment: .leading, spacing: WakeveTheme.Spacing.xs) {
                    GeometryReader { proxy in
                        ZStack(alignment: .leading) {
                            Capsule()
                                .fill(SemanticColor.separator(for: colorScheme))

                            Capsule()
                                .fill(SemanticColor.progress(for: colorScheme))
                                .frame(width: max(8, proxy.size.width * progress))
                        }
                    }
                    .frame(height: 8)
                    .accessibilityHidden(true)

                    HStack(spacing: WakeveTheme.Spacing.sm) {
                        ParticipantPresenceMetric(
                            title: String(localized: "participants.presence.metric.going"),
                            value: "\(acceptedCount)",
                            color: SemanticColor.confirmation(for: colorScheme)
                        )
                        ParticipantPresenceMetric(
                            title: String(localized: "participants.presence.metric.waiting"),
                            value: "\(pendingCount)",
                            color: SemanticColor.warning(for: colorScheme)
                        )
                        ParticipantPresenceMetric(
                            title: String(localized: "participants.presence.metric.unavailable"),
                            value: "\(declinedCount)",
                            color: SemanticColor.destructive(for: colorScheme)
                        )
                    }
                }

                HStack(spacing: WakeveTheme.Spacing.sm) {
                    Text(String(localized: "participants.presence.next_action.label"))
                        .font(WakeveTheme.Typography.tiny)
                        .foregroundColor(secondaryText)
                        .textCase(.uppercase)

                    Text(nextAction)
                        .font(WakeveTheme.Typography.callout.weight(.semibold))
                        .foregroundColor(primaryText)
                        .lineLimit(2)
                        .minimumScaleFactor(0.82)

                    Spacer(minLength: WakeveTheme.Spacing.xs)
                }
                .padding(.horizontal, WakeveTheme.Spacing.sm)
                .padding(.vertical, WakeveTheme.Spacing.xs)
                .background(SemanticColor.badge(for: colorScheme))
                .clipShape(RoundedRectangle(cornerRadius: WakeveTheme.Radius.md, style: .continuous))
            }
        }
    }

    private var primaryText: Color {
        SemanticColor.primaryText(for: colorScheme)
    }

    private var secondaryText: Color {
        SemanticColor.secondaryText(for: colorScheme)
    }
}

private struct ParticipantPresenceMetric: View {
    let title: String
    let value: String
    let color: Color

    var body: some View {
        VStack(alignment: .leading, spacing: WakeveTheme.Spacing.xxs) {
            Text(value)
                .font(WakeveTheme.Typography.rowTitle)
                .foregroundColor(color)

            Text(title)
                .font(WakeveTheme.Typography.tiny)
                .foregroundColor(.secondary)
                .lineLimit(1)
                .minimumScaleFactor(0.76)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(WakeveTheme.Spacing.sm)
        .background(color.opacity(0.12))
        .clipShape(RoundedRectangle(cornerRadius: WakeveTheme.Radius.md, style: .continuous))
    }
}

private struct ParticipantTimezoneRow: Identifiable {
    let id: String
    let icon: String
    let title: String
    let value: String
    let badge: String?

    init(id: String, icon: String, title: String, value: String, badge: String? = nil) {
        self.id = id
        self.icon = icon
        self.title = title
        self.value = value
        self.badge = badge
    }
}

private struct ParticipantTimezoneCoordinationCard: View {
    @Environment(\.colorScheme) private var colorScheme

    let subtitle: String
    let rows: [ParticipantTimezoneRow]
    let footnote: String

    var body: some View {
        WakeveContentCard(prominence: .regular, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
                HStack(alignment: .top, spacing: WakeveTheme.Spacing.md) {
                    Image(systemName: "globe.europe.africa.fill")
                        .font(.title3.weight(.bold))
                        .foregroundColor(SemanticColor.selectedState(for: colorScheme))
                        .frame(width: 46, height: 46)
                        .background(SemanticColor.badge(for: colorScheme))
                        .clipShape(Circle())

                    VStack(alignment: .leading, spacing: WakeveTheme.Spacing.xxs) {
                        Text(String(localized: "participants.timezone.title"))
                            .font(WakeveTheme.Typography.section)
                            .foregroundColor(primaryText)

                        Text(subtitle)
                            .font(WakeveTheme.Typography.callout)
                            .foregroundColor(secondaryText)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                }

                VStack(spacing: WakeveTheme.Spacing.sm) {
                    ForEach(rows) { row in
                        ParticipantTimezoneCoordinationRow(row: row)
                    }
                }

                Label(footnote, systemImage: "checkmark.shield.fill")
                    .font(WakeveTheme.Typography.tiny)
                    .foregroundColor(secondaryText)
                    .fixedSize(horizontal: false, vertical: true)
                    .padding(.horizontal, WakeveTheme.Spacing.sm)
                    .padding(.vertical, WakeveTheme.Spacing.xs)
                    .background(SemanticColor.badge(for: colorScheme))
                    .clipShape(RoundedRectangle(cornerRadius: WakeveTheme.Radius.md, style: .continuous))
            }
        }
    }

    private var primaryText: Color {
        SemanticColor.primaryText(for: colorScheme)
    }

    private var secondaryText: Color {
        SemanticColor.secondaryText(for: colorScheme)
    }
}

private struct ParticipantTimezoneCoordinationRow: View {
    @Environment(\.colorScheme) private var colorScheme

    let row: ParticipantTimezoneRow

    var body: some View {
        HStack(spacing: WakeveTheme.Spacing.sm) {
            Image(systemName: row.icon)
                .font(.body.weight(.semibold))
                .foregroundColor(SemanticColor.selectedState(for: colorScheme))
                .frame(width: 32, height: 32)
                .background(SemanticColor.badge(for: colorScheme))
                .clipShape(Circle())

            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.xxs) {
                Text(row.title)
                    .font(WakeveTheme.Typography.tiny)
                    .foregroundColor(SemanticColor.secondaryText(for: colorScheme))
                    .textCase(.uppercase)

                Text(row.value)
                    .font(WakeveTheme.Typography.callout.weight(.semibold))
                    .foregroundColor(SemanticColor.primaryText(for: colorScheme))
                    .lineLimit(2)
                    .minimumScaleFactor(0.78)
            }

            Spacer(minLength: WakeveTheme.Spacing.xs)

            if let badge = row.badge {
                Text(badge)
                    .font(WakeveTheme.Typography.tiny.weight(.semibold))
                    .foregroundColor(SemanticColor.selectedState(for: colorScheme))
                    .padding(.horizontal, WakeveTheme.Spacing.sm)
                    .padding(.vertical, WakeveTheme.Spacing.xxs)
                    .background(SemanticColor.badge(for: colorScheme))
                    .clipShape(Capsule())
            }
        }
        .padding(WakeveTheme.Spacing.sm)
        .background(WakeveTheme.ColorToken.controlFill(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: WakeveTheme.Radius.md, style: .continuous))
    }
}

private struct ParticipantFacePile: View {
    @Environment(\.colorScheme) private var colorScheme

    let rows: [ParticipantPresentationRow]

    var body: some View {
        HStack(spacing: -10) {
            if rows.isEmpty {
                Image(systemName: "person.crop.circle.badge.plus")
                    .font(.title3.weight(.semibold))
                    .foregroundColor(.white)
                    .frame(width: 42, height: 42)
                    .background(SemanticColor.selectedState(for: colorScheme))
                    .clipShape(Circle())
            } else {
                ForEach(rows) { row in
                    WakeveAvatar(initials: initials(for: row), size: 42)
                        .overlay(Circle().stroke(Color.white.opacity(0.84), lineWidth: 2))
                }
            }
        }
        .frame(minWidth: 42, alignment: .trailing)
        .accessibilityHidden(true)
    }

    private func initials(for row: ParticipantPresentationRow) -> String {
        let localPart = row.email.components(separatedBy: "@").first ?? row.email
        guard let first = localPart.first else { return "?" }
        return String(first).uppercased()
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
