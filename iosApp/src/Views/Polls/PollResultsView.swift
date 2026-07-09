import SwiftUI
import Shared
#if canImport(UIKit)
import UIKit
#endif

/// Poll results view inspired by Apple Invites
/// Features: Clean results visualization, progress indicators, clear winner highlighting
struct PollResultsView: View {
    let event: Event
    let userId: String
    let onDateConfirmed: (String) -> Void
    let onBack: () -> Void

    @StateObject private var confirmationViewModel: PollConfirmationViewModel
    @Environment(\.colorScheme) private var colorScheme

    init(
        event: Event,
        userId: String,
        onDateConfirmed: @escaping (String) -> Void,
        onBack: @escaping () -> Void
    ) {
        self.event = event
        self.userId = userId
        self.onDateConfirmed = onDateConfirmed
        self.onBack = onBack
        _confirmationViewModel = StateObject(
            wrappedValue: PollConfirmationViewModel(
                event: event,
                actorId: userId,
                onDateConfirmed: onDateConfirmed
            )
        )
    }

    var body: some View {
        PollResultsContentView(
            event: event,
            slotScores: confirmationViewModel.slotScores,
            bestSlot: confirmationViewModel.bestSlot,
            canConfirmDate: confirmationViewModel.canConfirmDate,
            isLoading: confirmationViewModel.isConfirmActionDisabled,
            onConfirmDate: {
                WakeveHaptics.selection()
                guard let slotId = confirmationViewModel.bestSlot?.id else { return }
                confirmationViewModel.requestConfirmation(for: slotId)
            },
            onBack: onBack
        )
        .confirmationDialog(
            String(localized: "poll.results.confirm_dialog_title"),
            isPresented: Binding(
                get: { confirmationViewModel.state == .confirmPrompt },
                set: { isPresented in
                    if !isPresented {
                        confirmationViewModel.cancelConfirmation()
                    }
                }
            ),
            titleVisibility: .visible
        ) {
            Button(String(localized: "poll.results.confirm_date")) {
                confirmationViewModel.submitConfirmation()
            }
            .accessibilityIdentifier("pollConfirmationConfirmButton")
            .accessibilityLabel(String(localized: "poll.results.confirmation.accessibility.confirm"))

            Button(String(localized: "common.cancel"), role: .cancel) {
                confirmationViewModel.cancelConfirmation()
            }
            .accessibilityIdentifier("pollConfirmationCancelButton")
            .accessibilityLabel(String(localized: "poll.results.confirmation.accessibility.cancel"))
        } message: {
            Text(String(localized: "poll.results.confirm_dialog_message"))
        }
        .overlay(alignment: .bottom) {
            confirmationStatus
        }
    }

    @ViewBuilder
    private var confirmationStatus: some View {
        switch confirmationViewModel.state {
        case .confirming:
            HStack(spacing: 10) {
                ProgressView()
                    .accessibilityIdentifier("pollConfirmationProgress")
                    .accessibilityLabel(String(localized: "poll.results.confirmation.accessibility.progress"))
                Text(String(localized: "poll.results.confirmation.progress"))
                    .font(.callout.weight(.semibold))
            }
            .padding(14)
            .background(.regularMaterial, in: Capsule())
            .padding()

        case .failed:
            VStack(alignment: .leading, spacing: 10) {
                Text(confirmationViewModel.failureMessage ?? String(localized: "poll.results.error.confirm_failed"))
                    .font(.callout)
                Button(String(localized: "poll.results.confirmation.retry")) {
                    confirmationViewModel.retryConfirmation()
                }
                .accessibilityIdentifier("pollConfirmationRetryButton")
                .accessibilityLabel(String(localized: "poll.results.confirmation.accessibility.retry"))
            }
            .padding(16)
            .background(SemanticColor.destructive(for: colorScheme).opacity(0.14), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
            .accessibilityIdentifier("pollConfirmationError")
            .accessibilityLabel(String(localized: "poll.results.confirmation.accessibility.error"))
            .padding()

        case .pendingSync:
            VStack(alignment: .leading, spacing: 6) {
                Text(String(localized: "poll.results.confirmation.pending_sync.title"))
                    .font(.callout.weight(.semibold))
                Text(String(localized: "poll.results.confirmation.pending_sync.message"))
                    .font(.caption)
            }
            .padding(16)
            .background(SemanticColor.warning(for: colorScheme).opacity(0.14), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
            .accessibilityIdentifier("pollConfirmationPendingSyncStatus")
            .accessibilityLabel(String(localized: "poll.results.confirmation.accessibility.pending_sync"))
            .padding()

        case .synced:
            VStack(alignment: .leading, spacing: 6) {
                Text(String(localized: "poll.results.confirmation.synced.title"))
                    .font(.callout.weight(.semibold))
                Text(String(localized: "poll.results.confirmation.synced.message"))
                    .font(.caption)
            }
            .padding(16)
            .background(SemanticColor.confirmation(for: colorScheme).opacity(0.14), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
            .accessibilityIdentifier("pollConfirmationSyncedStatus")
            .accessibilityLabel(String(localized: "poll.results.confirmation.accessibility.synced"))
            .padding()

        case .legacyApplied:
            VStack(alignment: .leading, spacing: 6) {
                Text(String(localized: "poll.results.confirmation.legacy_applied.title"))
                    .font(.callout.weight(.semibold))
                Text(String(localized: "poll.results.confirmation.legacy_applied.message"))
                    .font(.caption)
            }
            .padding(16)
            .background(Color.secondary.opacity(0.12), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
            .accessibilityIdentifier("pollConfirmationLegacyAppliedStatus")
            .accessibilityLabel(String(localized: "poll.results.confirmation.accessibility.legacy_applied"))
            .padding()

        case .quarantined:
            VStack(alignment: .leading, spacing: 6) {
                Text(String(localized: "poll.results.confirmation.quarantined.title"))
                    .font(.callout.weight(.semibold))
                Text(String(localized: "poll.results.confirmation.quarantined.message"))
                    .font(.caption)
            }
            .padding(16)
            .background(SemanticColor.warning(for: colorScheme).opacity(0.14), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
            .accessibilityIdentifier("pollConfirmationQuarantinedStatus")
            .accessibilityLabel(String(localized: "poll.results.confirmation.accessibility.quarantined"))
            .padding()

        case .reviewingResults, .confirmPrompt:
            EmptyView()
        }
    }
}

// MARK: - Poll Results Content View

struct PollResultsContentView: View {
    let event: Event
    let slotScores: [PollLogic.SlotScore]
    let bestSlot: TimeSlot?
    let canConfirmDate: Bool
    let isLoading: Bool
    let onConfirmDate: () -> Void
    let onBack: () -> Void

    var body: some View {
        ZStack {
            WakeveScreenBackground(style: .grouped)

            VStack(spacing: 0) {
                // Header
                VStack(spacing: 16) {
                    HStack {
                        WakeveCircleButton(
                            systemImage: "xmark",
                            accessibilityLabel: String(localized: "common.close"),
                            variant: .light,
                            size: 44,
                            action: onBack
                        )

                        Spacer()
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, WakeveTheme.Navigation.controlTopSpacing)

                    VStack(spacing: 8) {
                        Text(String(localized: "poll.results.title"))
                            .font(WakeveTheme.Typography.display)
                            .foregroundColor(.primary)
                            .frame(maxWidth: .infinity, alignment: .leading)

                        Text(event.title)
                            .font(WakeveTheme.Typography.title2)
                            .foregroundColor(.secondary)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    .padding(.horizontal, 20)
                }

                ScrollView {
                    VStack(spacing: 16) {
                        if event.status == .confirmed, let finalDate = event.finalDate {
                            // Confirmed State
                            let finalSlot = event.proposedSlots.first { $0.id == finalDate }
                            ConfirmedDateCard(event: event, finalSlot: finalSlot)
                            if let finalSlot {
                                PollDecisionAnnouncementCard(event: event, slot: finalSlot, isConfirmed: true)
                            }
                            PollResolutionNextStepsCard()
                        } else {
                            // Polling State - Show Results
                            if !slotScores.isEmpty {
                                // Best Slot Highlight
                                if let best = bestSlot {
                                    BestSlotCard(slot: best)
                                    if canConfirmDate {
                                        PollDecisionAnnouncementCard(event: event, slot: best, isConfirmed: false)
                                    }
                                }

                                // Results List
                                VStack(alignment: .leading, spacing: 12) {
                                    Text(String(localized: "poll.results.all_options"))
                                        .font(WakeveTheme.Typography.section)
                                        .foregroundColor(.primary)
                                        .padding(.horizontal, 4)

                                    ForEach(slotScores.sorted(by: { $0.totalScore > $1.totalScore }), id: \.slotId) { score in
                                        if let slot = event.proposedSlots.first(where: { $0.id == score.slotId }) {
                                            SlotResultCard(
                                                slot: slot,
                                                score: score,
                                                isBest: score.slotId == bestSlot?.id
                                            )
                                        }
                                    }
                                }

                                // Confirm Button
                                if canConfirmDate, bestSlot != nil {
                                    WakeveActionButton(
                                        String(localized: "poll.results.confirm_date"),
                                        systemImage: "checkmark",
                                        variant: .primary,
                                        isDisabled: isLoading,
                                        isLoading: isLoading
                                    ) { onConfirmDate() }
                                    .padding(.top, 8)
                                }
                            } else {
                                // Empty State
                                VStack(spacing: 20) {
                                    Image(systemName: "chart.bar")
                                        .font(.system(size: 56))
                                        .foregroundColor(.secondary.opacity(0.5))
                                        .padding(.top, 40)

                                    VStack(spacing: 8) {
                                        Text(event.proposedSlots.isEmpty
                                             ? String(localized: "poll.results.no_slots_title")
                                             : String(localized: "poll.results.no_votes_title"))
                                            .font(WakeveTheme.Typography.title2)
                                            .foregroundColor(.primary)

                                        Text(event.proposedSlots.isEmpty
                                             ? String(localized: "poll.results.no_slots_subtitle")
                                             : String(localized: "poll.results.no_votes_subtitle"))
                                            .font(WakeveTheme.Typography.body)
                                            .foregroundColor(.secondary)
                                            .multilineTextAlignment(.center)
                                    }
                                    .padding(.bottom, 40)
                                }
                                .frame(maxWidth: .infinity)
                                .glassCard(cornerRadius: 20)
                            }
                        }

                        Spacer()
                            .frame(height: 40)
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 16)
                }
            }
        }
    }
}

// MARK: - Best Slot Card

struct BestSlotCard: View {
    let slot: TimeSlot
    @Environment(\.colorScheme) private var colorScheme
    @ScaledMetric(relativeTo: .title2) private var starSize: CGFloat = 20
    
    var body: some View {
        VStack(spacing: 16) {
            HStack {
                Image(systemName: "star.fill")
                    .font(.title2)
                    .foregroundColor(SemanticColor.warning(for: colorScheme))
                    .frame(width: starSize, height: starSize)
                
                Text(String(localized: "poll.results.best_time"))
                    .font(WakeveTheme.Typography.section)
                    .foregroundColor(.primary)
                
                Spacer()
            }
            
            VStack(spacing: 8) {
                Text(formatDate(slot.start ?? "", timezone: slot.timezone))
                    .font(WakeveTheme.Typography.title)
                    .foregroundColor(.primary)
                
                HStack(spacing: 6) {
                    Image(systemName: "clock")
                        .font(.callout)
                        .foregroundColor(.secondary)
                    
                    Text("\(formatTime(slot.start ?? "", timezone: slot.timezone)) - \(formatTime(slot.end ?? "", timezone: slot.timezone))")
                        .font(WakeveTheme.Typography.body)
                        .foregroundColor(.secondary)
                }

                PollTimeZoneBadge(
                    label: formatTimeZoneLabel(slot.timezone, at: slot.start),
                    foregroundColor: .secondary,
                    backgroundColor: Color(.tertiarySystemFill)
                )
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity)
        .glassCard(cornerRadius: 20)
        .overlay(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .stroke(SemanticColor.warning(for: colorScheme).opacity(0.3), lineWidth: 2)
        )
    }
    
    private func formatDate(_ dateString: String, timezone: String) -> String {
        if let date = ISO8601DateFormatter().date(from: dateString) {
            let formatter = DateFormatter()
            formatter.locale = .autoupdatingCurrent
            formatter.timeZone = timeZone(for: timezone)
            formatter.setLocalizedDateFormatFromTemplate("EEEEdMMMM")
            return formatter.string(from: date)
        }
        return dateString
    }
    
    private func formatTime(_ dateString: String, timezone: String) -> String {
        if let date = ISO8601DateFormatter().date(from: dateString) {
            let formatter = DateFormatter()
            formatter.locale = .autoupdatingCurrent
            formatter.timeZone = timeZone(for: timezone)
            formatter.timeStyle = .short
            return formatter.string(from: date)
        }
        return dateString
    }

    private func timeZone(for identifier: String) -> TimeZone {
        TimeZone(identifier: identifier.trimmingCharacters(in: .whitespacesAndNewlines)) ?? .current
    }

    private func formatTimeZoneLabel(_ identifier: String, at dateString: String?) -> String {
        String(
            format: String(localized: "poll.timezone.label_format"),
            formatTimeZoneDisplay(identifier, at: dateString)
        )
    }

    private func formatTimeZoneDisplay(_ identifier: String, at dateString: String?) -> String {
        let trimmed = identifier.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return TimeZone.current.identifier }
        guard let timeZone = TimeZone(identifier: trimmed) else {
            return trimmed.replacingOccurrences(of: "_", with: " ")
        }

        let date = dateString.flatMap { ISO8601DateFormatter().date(from: $0) }
        let abbreviation = date.flatMap { timeZone.abbreviation(for: $0) } ?? timeZone.abbreviation() ?? trimmed
        let city = trimmed.split(separator: "/").last.map { String($0).replacingOccurrences(of: "_", with: " ") } ?? trimmed

        return city == abbreviation ? abbreviation : "\(abbreviation) · \(city)"
    }
}

// MARK: - Confirmed Date Card

struct ConfirmedDateCard: View {
    let event: Event
    let finalSlot: TimeSlot?
    @ScaledMetric(relativeTo: .largeTitle) private var confirmationIconSize: CGFloat = 64
    @Environment(\.colorScheme) private var colorScheme
    
    var body: some View {
        VStack(spacing: 20) {
            // Success Icon
            ZStack {
                Circle()
                    .fill(SemanticColor.confirmation(for: colorScheme).opacity(0.15))
                    .frame(width: confirmationIconSize, height: confirmationIconSize)
                
                Image(systemName: "checkmark.circle.fill")
                    .font(.largeTitle)
                    .foregroundColor(SemanticColor.confirmation(for: colorScheme))
            }
            
            // Title
            VStack(spacing: 8) {
                Text(String(localized: "poll.results.confirmed_title"))
                    .font(WakeveTheme.Typography.title)
                    .foregroundColor(.primary)
                
                Text(String(localized: "poll.results.confirmed_subtitle"))
                    .font(WakeveTheme.Typography.callout)
                    .foregroundColor(.secondary)
            }
            
            // Confirmed Date
            if let slot = finalSlot {
                VStack(spacing: 8) {
                    Text(formatDate(slot.start ?? "", timezone: slot.timezone))
                        .font(WakeveTheme.Typography.section)
                        .foregroundColor(.primary)
                    
                    HStack(spacing: 6) {
                        Image(systemName: "clock")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        
                        Text("\(formatTime(slot.start ?? "", timezone: slot.timezone)) - \(formatTime(slot.end ?? "", timezone: slot.timezone))")
                            .font(WakeveTheme.Typography.callout)
                            .foregroundColor(.secondary)
                    }

                    PollTimeZoneBadge(
                        label: formatTimeZoneLabel(slot.timezone, at: slot.start),
                        foregroundColor: .secondary,
                        backgroundColor: Color(.secondarySystemFill)
                    )
                }
                .padding(16)
                .frame(maxWidth: .infinity)
                .background(Color(.tertiarySystemFill))
                .continuousCornerRadius(12)
            }
        }
        .padding(24)
        .frame(maxWidth: .infinity)
        .glassCard(cornerRadius: 20)
    }
    
    private func formatDate(_ dateString: String, timezone: String) -> String {
        if let date = ISO8601DateFormatter().date(from: dateString) {
            let formatter = DateFormatter()
            formatter.locale = .autoupdatingCurrent
            formatter.timeZone = timeZone(for: timezone)
            formatter.setLocalizedDateFormatFromTemplate("EEEEdMMMMy")
            return formatter.string(from: date)
        }
        return dateString
    }
    
    private func formatTime(_ dateString: String, timezone: String) -> String {
        if let date = ISO8601DateFormatter().date(from: dateString) {
            let formatter = DateFormatter()
            formatter.locale = .autoupdatingCurrent
            formatter.timeZone = timeZone(for: timezone)
            formatter.timeStyle = .short
            return formatter.string(from: date)
        }
        return dateString
    }

    private func timeZone(for identifier: String) -> TimeZone {
        TimeZone(identifier: identifier.trimmingCharacters(in: .whitespacesAndNewlines)) ?? .current
    }

    private func formatTimeZoneLabel(_ identifier: String, at dateString: String?) -> String {
        String(
            format: String(localized: "poll.timezone.label_format"),
            formatTimeZoneDisplay(identifier, at: dateString)
        )
    }

    private func formatTimeZoneDisplay(_ identifier: String, at dateString: String?) -> String {
        let trimmed = identifier.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return TimeZone.current.identifier }
        guard let timeZone = TimeZone(identifier: trimmed) else {
            return trimmed.replacingOccurrences(of: "_", with: " ")
        }

        let date = dateString.flatMap { ISO8601DateFormatter().date(from: $0) }
        let abbreviation = date.flatMap { timeZone.abbreviation(for: $0) } ?? timeZone.abbreviation() ?? trimmed
        let city = trimmed.split(separator: "/").last.map { String($0).replacingOccurrences(of: "_", with: " ") } ?? trimmed

        return city == abbreviation ? abbreviation : "\(abbreviation) · \(city)"
    }
}

// MARK: - Poll Decision Announcement
// MARK: - Decision Announcement Card

struct PollDecisionAnnouncementCard: View {
    let event: Event
    let slot: TimeSlot
    let isConfirmed: Bool
    @State private var showCopiedAnnouncementMessage = false
    @Environment(\.colorScheme) private var colorScheme

    private var announcementMessage: String {
        let date = formatDate(slot.start ?? "", timezone: slot.timezone)
        let time = formatTimeRange(start: slot.start, end: slot.end, timezone: slot.timezone)

        if isConfirmed {
            return String(format: String(localized: "poll.results.announcement.confirmed_message_format"), event.title, date, time)
        }

        return String(format: String(localized: "poll.results.announcement.pending_message_format"), event.title, date, time)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .top, spacing: 12) {
                Image(systemName: isConfirmed ? "megaphone.fill" : "text.bubble.fill")
                    .font(.system(size: 22, weight: .bold))
                    .foregroundColor(SemanticColor.accent(for: colorScheme))
                    .frame(width: 36, height: 36)
                    .background(SemanticColor.accent(for: colorScheme).opacity(0.12))
                    .clipShape(Circle())

                VStack(alignment: .leading, spacing: 6) {
                    Text(isConfirmed ? String(localized: "poll.results.announcement.confirmed_title") : String(localized: "poll.results.announcement.pending_title"))
                        .font(WakeveTheme.Typography.section)
                        .foregroundColor(.primary)

                    Text(announcementMessage)
                        .font(WakeveTheme.Typography.callout)
                        .foregroundColor(.secondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
            }

            HStack(spacing: 10) {
                ShareLink(item: announcementMessage) {
                    Label(String(localized: "poll.results.announcement.share_action"), systemImage: "square.and.arrow.up")
                        .font(WakeveTheme.Typography.bodySemibold)
                        .lineLimit(1)
                        .minimumScaleFactor(0.76)
                        .frame(maxWidth: .infinity)
                        .frame(height: 46)
                }
                .buttonStyle(.borderedProminent)
                .simultaneousGesture(TapGesture().onEnded {
                    WakeveHaptics.selection()
                })
                .accessibilityIdentifier("pollDecisionAnnouncementShareLink")

                Button {
                    copyAnnouncementMessage()
                } label: {
                    Label(String(localized: "poll.results.announcement.copy_action"), systemImage: showCopiedAnnouncementMessage ? "checkmark" : "doc.on.doc.fill")
                        .font(WakeveTheme.Typography.bodySemibold)
                        .labelStyle(.iconOnly)
                        .frame(width: 46, height: 46)
                }
                .buttonStyle(.bordered)
                .accessibilityLabel(showCopiedAnnouncementMessage ? String(localized: "poll.results.announcement.copied") : String(localized: "poll.results.announcement.copy_action"))
                .accessibilityIdentifier("pollDecisionAnnouncementCopyButton")
            }

            Text(String(localized: "poll.results.announcement.share_hint"))
                .font(WakeveTheme.Typography.caption)
                .foregroundColor(.secondary)

            if showCopiedAnnouncementMessage {
                Label(String(localized: "poll.results.announcement.copied"), systemImage: "checkmark.circle.fill")
                    .font(WakeveTheme.Typography.caption)
                    .foregroundColor(SemanticColor.confirmation(for: colorScheme))
                    .transition(.opacity.combined(with: .move(edge: .top)))
                    .accessibilityIdentifier("pollDecisionAnnouncementCopiedFeedback")
            }
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .glassCard(cornerRadius: 20)
        .accessibilityIdentifier("pollDecisionAnnouncementCard")
    }

    private func copyAnnouncementMessage() {
        #if canImport(UIKit)
        UIPasteboard.general.string = announcementMessage
        #endif
        WakeveHaptics.success()
        withAnimation(.easeInOut(duration: 0.18)) {
            showCopiedAnnouncementMessage = true
        }

        DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
            withAnimation(.easeInOut(duration: 0.18)) {
                showCopiedAnnouncementMessage = false
            }
        }
    }

    private func formatDate(_ dateString: String, timezone: String) -> String {
        if let date = ISO8601DateFormatter().date(from: dateString) {
            let formatter = DateFormatter()
            formatter.locale = .autoupdatingCurrent
            formatter.timeZone = timeZone(for: timezone)
            formatter.setLocalizedDateFormatFromTemplate("EEEEdMMM")
            return formatter.string(from: date)
        }
        return dateString
    }

    private func formatTimeRange(start: String?, end: String?, timezone: String) -> String {
        let startValue = formatTime(start ?? "", timezone: timezone)
        let endValue = formatTime(end ?? "", timezone: timezone)
        let timezoneDisplay = formatTimeZoneDisplay(timezone, at: start)

        if startValue.isEmpty {
            return ""
        }

        if endValue.isEmpty || endValue == startValue {
            let time = String(format: String(localized: "poll.results.announcement.time_at_format"), startValue)
            return "\(time) (\(timezoneDisplay))"
        }

        let time = String(format: String(localized: "poll.results.announcement.time_range_format"), startValue, endValue)
        return "\(time) (\(timezoneDisplay))"
    }

    private func formatTime(_ dateString: String, timezone: String) -> String {
        guard let date = ISO8601DateFormatter().date(from: dateString) else {
            return dateString
        }

        let formatter = DateFormatter()
        formatter.locale = .autoupdatingCurrent
        formatter.timeZone = timeZone(for: timezone)
        formatter.timeStyle = .short
        return formatter.string(from: date)
    }

    private func timeZone(for identifier: String) -> TimeZone {
        TimeZone(identifier: identifier.trimmingCharacters(in: .whitespacesAndNewlines)) ?? .current
    }

    private func formatTimeZoneDisplay(_ identifier: String, at dateString: String?) -> String {
        let trimmed = identifier.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return TimeZone.current.identifier }
        guard let timeZone = TimeZone(identifier: trimmed) else {
            return trimmed.replacingOccurrences(of: "_", with: " ")
        }

        let date = dateString.flatMap { ISO8601DateFormatter().date(from: $0) }
        let abbreviation = date.flatMap { timeZone.abbreviation(for: $0) } ?? timeZone.abbreviation() ?? trimmed
        let city = trimmed.split(separator: "/").last.map { String($0).replacingOccurrences(of: "_", with: " ") } ?? trimmed

        return city == abbreviation ? abbreviation : "\(abbreviation) · \(city)"
    }
}

// MARK: - Resolution Next Steps Card

struct PollResolutionNextStepsCard: View {
    private let steps: [PollResolutionStep] = [
        PollResolutionStep(
            icon: "megaphone.fill",
            titleKey: "poll.results.next_steps.announce_title",
            detailKey: "poll.results.next_steps.announce_detail"
        ),
        PollResolutionStep(
            icon: "calendar.badge.checkmark",
            titleKey: "poll.results.next_steps.calendar_title",
            detailKey: "poll.results.next_steps.calendar_detail"
        ),
        PollResolutionStep(
            icon: "map.fill",
            titleKey: "poll.results.next_steps.plan_title",
            detailKey: "poll.results.next_steps.plan_detail"
        ),
        PollResolutionStep(
            icon: "checklist.checked",
            titleKey: "poll.results.next_steps.owners_title",
            detailKey: "poll.results.next_steps.owners_detail"
        )
    ]

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            VStack(alignment: .leading, spacing: 6) {
                Label(String(localized: "poll.results.next_steps.title"), systemImage: "sparkles")
                    .font(WakeveTheme.Typography.section)
                    .foregroundColor(.primary)

                Text(String(localized: "poll.results.next_steps.subtitle"))
                    .font(WakeveTheme.Typography.callout)
                    .foregroundColor(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }

            VStack(spacing: 12) {
                ForEach(steps) { step in
                    PollResolutionStepRow(step: step)
                }
            }
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .glassCard(cornerRadius: 20)
        .accessibilityIdentifier("pollResolutionNextStepsCard")
    }
}

private struct PollResolutionStep: Identifiable {
    let id = UUID()
    let icon: String
    let titleKey: LocalizedStringResource
    let detailKey: LocalizedStringResource
}

private struct PollResolutionStepRow: View {
    let step: PollResolutionStep
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: step.icon)
                .font(.system(size: 15, weight: .bold))
                .foregroundColor(SemanticColor.accent(for: colorScheme))
                .frame(width: 34, height: 34)
                .background(SemanticColor.accent(for: colorScheme).opacity(0.12))
                .clipShape(Circle())

            VStack(alignment: .leading, spacing: 4) {
                Text(String(localized: step.titleKey))
                    .font(WakeveTheme.Typography.bodySemibold)
                    .foregroundColor(.primary)

                Text(String(localized: step.detailKey))
                    .font(WakeveTheme.Typography.caption)
                    .foregroundColor(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }

            Spacer(minLength: 0)
        }
        .padding(12)
        .background(Color(.tertiarySystemFill))
        .continuousCornerRadius(14)
    }
}

// MARK: - Slot Result Card

struct SlotResultCard: View {
    let slot: TimeSlot
    let score: PollLogic.SlotScore
    let isBest: Bool
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        VStack(spacing: 16) {
            // Date and Time
            VStack(spacing: 6) {
                Text(formatDate(slot.start ?? "", timezone: slot.timezone))
                    .font(WakeveTheme.Typography.bodySemibold)
                    .foregroundColor(.primary)

                HStack(spacing: 6) {
                    Image(systemName: "clock")
                        .font(.system(size: 14))
                        .foregroundColor(.secondary)

                    Text("\(formatTime(slot.start ?? "", timezone: slot.timezone)) - \(formatTime(slot.end ?? "", timezone: slot.timezone))")
                        .font(WakeveTheme.Typography.callout)
                        .foregroundColor(.secondary)
                }

                PollTimeZoneBadge(
                    label: formatTimeZoneLabel(slot.timezone, at: slot.start),
                    foregroundColor: .secondary,
                    backgroundColor: Color(.tertiarySystemFill)
                )
            }

            // Vote Breakdown
            HStack(spacing: 16) {
                VoteCountBadge(
                    label: String(localized: "poll.results.vote.yes"),
                    count: Int(score.yesCount),
                    color: SemanticColor.confirmation(for: colorScheme)
                )

                VoteCountBadge(
                    label: String(localized: "poll.results.vote.maybe"),
                    count: Int(score.maybeCount),
                    color: SemanticColor.warning(for: colorScheme)
                )

                VoteCountBadge(
                    label: String(localized: "poll.results.vote.no"),
                    count: Int(score.noCount),
                    color: SemanticColor.destructive(for: colorScheme)
                )

                Spacer()

                // Total Score
                VStack(spacing: 4) {
                    Text(String(localized: "poll.results.score"))
                        .font(WakeveTheme.Typography.tiny)
                        .foregroundColor(.secondary)
                        .textCase(.uppercase)

                    Text("\(score.totalScore)")
                        .font(WakeveTheme.Typography.title2)
                        .foregroundColor(isBest ? SemanticColor.accent(for: colorScheme) : .primary)
                }
            }

            // Best Indicator
            if isBest {
                HStack {
                    Image(systemName: "star.fill")
                        .font(.system(size: 12))
                        .foregroundColor(SemanticColor.warning(for: colorScheme))

                    Text(String(localized: "poll.results.most_popular"))
                        .font(WakeveTheme.Typography.caption)
                        .foregroundColor(.primary)

                    Spacer()
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(SemanticColor.warning(for: colorScheme).opacity(0.15))
                .continuousCornerRadius(8)
            }
        }
        .padding(16)
        .glassCard(cornerRadius: 20)
        .overlay(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .stroke(isBest ? SemanticColor.accent(for: colorScheme).opacity(0.3) : Color.clear, lineWidth: 2)
        )
    }

    private func formatDate(_ dateString: String, timezone: String) -> String {
        if let date = ISO8601DateFormatter().date(from: dateString) {
            let formatter = DateFormatter()
            formatter.locale = .autoupdatingCurrent
            formatter.timeZone = timeZone(for: timezone)
            formatter.setLocalizedDateFormatFromTemplate("EEEdMMM")
            return formatter.string(from: date)
        }
        return dateString
    }

    private func formatTime(_ dateString: String, timezone: String) -> String {
        if let date = ISO8601DateFormatter().date(from: dateString) {
            let formatter = DateFormatter()
            formatter.locale = .autoupdatingCurrent
            formatter.timeZone = timeZone(for: timezone)
            formatter.timeStyle = .short
            return formatter.string(from: date)
        }
        return dateString
    }

    private func timeZone(for identifier: String) -> TimeZone {
        TimeZone(identifier: identifier.trimmingCharacters(in: .whitespacesAndNewlines)) ?? .current
    }

    private func formatTimeZoneLabel(_ identifier: String, at dateString: String?) -> String {
        String(
            format: String(localized: "poll.timezone.label_format"),
            formatTimeZoneDisplay(identifier, at: dateString)
        )
    }

    private func formatTimeZoneDisplay(_ identifier: String, at dateString: String?) -> String {
        let trimmed = identifier.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return TimeZone.current.identifier }
        guard let timeZone = TimeZone(identifier: trimmed) else {
            return trimmed.replacingOccurrences(of: "_", with: " ")
        }

        let date = dateString.flatMap { ISO8601DateFormatter().date(from: $0) }
        let abbreviation = date.flatMap { timeZone.abbreviation(for: $0) } ?? timeZone.abbreviation() ?? trimmed
        let city = trimmed.split(separator: "/").last.map { String($0).replacingOccurrences(of: "_", with: " ") } ?? trimmed

        return city == abbreviation ? abbreviation : "\(abbreviation) · \(city)"
    }
}

// MARK: - Vote Count Badge

struct VoteCountBadge: View {
    let label: String
    let count: Int
    let color: Color

    var body: some View {
        VStack(spacing: 4) {
            Text("\(count)")
                .font(WakeveTheme.Typography.bodySemibold)
                .foregroundColor(color)

            Text(label)
                .font(WakeveTheme.Typography.tiny)
                .foregroundColor(.secondary)
                .textCase(.uppercase)
        }
    }
}

// MARK: - Preview

#if DEBUG
#Preview("Poll Results - Loaded Light") {
    PollResultsContentView(
        event: EventFactory.polling,
        slotScores: EventFactory.pollingPreviewScores,
        bestSlot: EventFactory.polling.proposedSlots.first,
        canConfirmDate: true,
        isLoading: false,
        onConfirmDate: {},
        onBack: {}
    )
    .preferredColorScheme(.light)
}

#Preview("Poll Results - Loaded Dark") {
    PollResultsContentView(
        event: EventFactory.polling,
        slotScores: EventFactory.pollingPreviewScores,
        bestSlot: EventFactory.polling.proposedSlots.first,
        canConfirmDate: true,
        isLoading: false,
        onConfirmDate: {},
        onBack: {}
    )
    .preferredColorScheme(.dark)
}

#Preview("Poll Results - No Votes") {
    PollResultsContentView(
        event: EventFactory.polling,
        slotScores: [],
        bestSlot: nil,
        canConfirmDate: true,
        isLoading: false,
        onConfirmDate: {},
        onBack: {}
    )
}

#Preview("Poll Results - Confirmed") {
    let event = EventFactory.confirmedWithFinalSlot
    PollResultsContentView(
        event: event,
        slotScores: [],
        bestSlot: nil,
        canConfirmDate: false,
        isLoading: false,
        onConfirmDate: {},
        onBack: {}
    )
}
#endif
