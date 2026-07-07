import SwiftUI
import Shared
#if canImport(UIKit)
import UIKit
#endif

/// Poll results view inspired by Apple Invites
/// Features: Clean results visualization, progress indicators, clear winner highlighting
struct PollResultsView: View {
    let event: Event
    let repository: EventRepositoryInterface
    let userId: String
    let onDateConfirmed: (String) -> Void
    let onBack: () -> Void

    @State private var poll: Poll?
    @State private var slotScores: [PollLogic.SlotScore] = []
    @State private var bestSlot: TimeSlot?
    @State private var isLoading = false
    @State private var errorMessage = ""
    @State private var showError = false
    @State private var showSuccess = false
    @State private var showConfirmDateDialog = false

    var body: some View {
        PollResultsContentView(
            event: event,
            slotScores: slotScores,
            bestSlot: bestSlot,
            canConfirmDate: repository.isOrganizer(eventId: event.id, userId: userId),
            isLoading: isLoading,
            onConfirmDate: {
                WakeveHaptics.selection()
                showConfirmDateDialog = true
            },
            onBack: onBack
        )
        .onAppear {
            loadPollResults()
        }
        .alert(String(localized: "common.error"), isPresented: $showError) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(errorMessage)
        }
        .alert(String(localized: "common.success"), isPresented: $showSuccess) {
            Button("OK", role: .cancel) {
                onDateConfirmed(event.id)
            }
        } message: {
            Text(String(localized: "poll.results.date_confirmed"))
        }
        .confirmationDialog(
            String(localized: "poll.results.confirm_dialog_title"),
            isPresented: $showConfirmDateDialog,
            titleVisibility: .visible
        ) {
            Button(String(localized: "poll.results.confirm_date")) {
                Task { await confirmDate() }
            }
            Button(String(localized: "common.cancel"), role: .cancel) {}
        } message: {
            Text(String(localized: "poll.results.confirm_dialog_message"))
        }
    }

    private func loadPollResults() {
        poll = repository.getPoll(eventId: event.id)

        if let poll = poll {
            slotScores = PollLogic.shared.getSlotScores(poll: poll, slots: event.proposedSlots)

            if let bestResult = PollLogic.shared.getBestSlotWithScore(poll: poll, slots: event.proposedSlots) {
                bestSlot = bestResult.first
            }
        }
    }

    private func confirmDate() async {
        guard let bestSlot = bestSlot else { return }

        guard repository.isOrganizer(eventId: event.id, userId: userId) else {
            errorMessage = String(localized: "poll.results.error.organizer_only")
            showError = true
            return
        }

        isLoading = true

        do {
            _ = try await repository.updateEventStatus(
                id: event.id,
                status: EventStatus.confirmed,
                finalDate: bestSlot.id
            )
            let updatedEvent = repository.getEvent(id: event.id)

            if updatedEvent?.status == EventStatus.confirmed {
                isLoading = false
                WakeveHaptics.success()
                showSuccess = true
            } else {
                isLoading = false
                WakeveHaptics.warning()
                errorMessage = String(localized: "poll.results.error.confirm_failed")
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
                            .font(.system(size: 34, weight: .bold))
                            .foregroundColor(.primary)
                            .frame(maxWidth: .infinity, alignment: .leading)

                        Text(event.title)
                            .font(.system(size: 20, weight: .medium))
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
                                        .font(.system(size: 20, weight: .semibold))
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
                                            .font(.system(size: 24, weight: .bold))
                                            .foregroundColor(.primary)

                                        Text(event.proposedSlots.isEmpty
                                             ? String(localized: "poll.results.no_slots_subtitle")
                                             : String(localized: "poll.results.no_votes_subtitle"))
                                            .font(.system(size: 17))
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
    
    var body: some View {
        VStack(spacing: 16) {
            HStack {
                Image(systemName: "star.fill")
                    .font(.system(size: 20))
                    .foregroundColor(.yellow)
                
                Text(String(localized: "poll.results.best_time"))
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(.primary)
                
                Spacer()
            }
            
            VStack(spacing: 8) {
                Text(formatDate(slot.start ?? "", timezone: slot.timezone))
                    .font(.system(size: 24, weight: .bold))
                    .foregroundColor(.primary)
                
                HStack(spacing: 6) {
                    Image(systemName: "clock")
                        .font(.system(size: 14))
                        .foregroundColor(.secondary)
                    
                    Text("\(formatTime(slot.start ?? "", timezone: slot.timezone)) - \(formatTime(slot.end ?? "", timezone: slot.timezone))")
                        .font(.system(size: 17))
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
                .stroke(Color.yellow.opacity(0.3), lineWidth: 2)
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
    
    var body: some View {
        VStack(spacing: 20) {
            // Success Icon
            ZStack {
                Circle()
                    .fill(Color.green.opacity(0.15))
                    .frame(width: 64, height: 64)
                
                Image(systemName: "checkmark.circle.fill")
                    .font(.system(size: 40))
                    .foregroundColor(.green)
            }
            
            // Title
            VStack(spacing: 8) {
                Text(String(localized: "poll.results.confirmed_title"))
                    .font(.system(size: 24, weight: .bold))
                    .foregroundColor(.primary)
                
                Text(String(localized: "poll.results.confirmed_subtitle"))
                    .font(.system(size: 15))
                    .foregroundColor(.secondary)
            }
            
            // Confirmed Date
            if let slot = finalSlot {
                VStack(spacing: 8) {
                    Text(formatDate(slot.start ?? "", timezone: slot.timezone))
                        .font(.system(size: 20, weight: .semibold))
                        .foregroundColor(.primary)
                    
                    HStack(spacing: 6) {
                        Image(systemName: "clock")
                            .font(.system(size: 14))
                            .foregroundColor(.secondary)
                        
                        Text("\(formatTime(slot.start ?? "", timezone: slot.timezone)) - \(formatTime(slot.end ?? "", timezone: slot.timezone))")
                            .font(.system(size: 15))
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

// MARK: - Decision Announcement Card

struct PollDecisionAnnouncementCard: View {
    let event: Event
    let slot: TimeSlot
    let isConfirmed: Bool
    @State private var showCopiedAnnouncementMessage = false

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
                    .foregroundColor(.blue)
                    .frame(width: 36, height: 36)
                    .background(Color.blue.opacity(0.12))
                    .clipShape(Circle())

                VStack(alignment: .leading, spacing: 6) {
                    Text(isConfirmed ? String(localized: "poll.results.announcement.confirmed_title") : String(localized: "poll.results.announcement.pending_title"))
                        .font(.system(size: 19, weight: .semibold))
                        .foregroundColor(.primary)

                    Text(announcementMessage)
                        .font(.system(size: 15))
                        .foregroundColor(.secondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
            }

            HStack(spacing: 10) {
                ShareLink(item: announcementMessage) {
                    Label(String(localized: "poll.results.announcement.share_action"), systemImage: "square.and.arrow.up")
                        .font(.system(size: 16, weight: .semibold))
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
                        .font(.system(size: 16, weight: .semibold))
                        .labelStyle(.iconOnly)
                        .frame(width: 46, height: 46)
                }
                .buttonStyle(.bordered)
                .accessibilityLabel(showCopiedAnnouncementMessage ? String(localized: "poll.results.announcement.copied") : String(localized: "poll.results.announcement.copy_action"))
                .accessibilityIdentifier("pollDecisionAnnouncementCopyButton")
            }

            Text(String(localized: "poll.results.announcement.share_hint"))
                .font(.system(size: 13))
                .foregroundColor(.secondary)

            if showCopiedAnnouncementMessage {
                Label(String(localized: "poll.results.announcement.copied"), systemImage: "checkmark.circle.fill")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundColor(.green)
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
                    .font(.system(size: 19, weight: .semibold))
                    .foregroundColor(.primary)

                Text(String(localized: "poll.results.next_steps.subtitle"))
                    .font(.system(size: 15))
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

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: step.icon)
                .font(.system(size: 15, weight: .bold))
                .foregroundColor(.blue)
                .frame(width: 34, height: 34)
                .background(Color.blue.opacity(0.12))
                .clipShape(Circle())

            VStack(alignment: .leading, spacing: 4) {
                Text(String(localized: step.titleKey))
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(.primary)

                Text(String(localized: step.detailKey))
                    .font(.system(size: 13))
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

    var body: some View {
        VStack(spacing: 16) {
            // Date and Time
            VStack(spacing: 6) {
                Text(formatDate(slot.start ?? "", timezone: slot.timezone))
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundColor(.primary)

                HStack(spacing: 6) {
                    Image(systemName: "clock")
                        .font(.system(size: 14))
                        .foregroundColor(.secondary)

                    Text("\(formatTime(slot.start ?? "", timezone: slot.timezone)) - \(formatTime(slot.end ?? "", timezone: slot.timezone))")
                        .font(.system(size: 15))
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
                    color: .green
                )

                VoteCountBadge(
                    label: String(localized: "poll.results.vote.maybe"),
                    count: Int(score.maybeCount),
                    color: .orange
                )

                VoteCountBadge(
                    label: String(localized: "poll.results.vote.no"),
                    count: Int(score.noCount),
                    color: .red
                )

                Spacer()

                // Total Score
                VStack(spacing: 4) {
                    Text(String(localized: "poll.results.score"))
                        .font(.system(size: 11, weight: .medium))
                        .foregroundColor(.secondary)
                        .textCase(.uppercase)

                    Text("\(score.totalScore)")
                        .font(.system(size: 24, weight: .bold))
                        .foregroundColor(isBest ? .blue : .primary)
                }
            }

            // Best Indicator
            if isBest {
                HStack {
                    Image(systemName: "star.fill")
                        .font(.system(size: 12))
                        .foregroundColor(.yellow)

                    Text(String(localized: "poll.results.most_popular"))
                        .font(.system(size: 13, weight: .medium))
                        .foregroundColor(.primary)

                    Spacer()
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(Color.yellow.opacity(0.15))
                .continuousCornerRadius(8)
            }
        }
        .padding(16)
        .glassCard(cornerRadius: 20)
        .overlay(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .stroke(isBest ? Color.blue.opacity(0.3) : Color.clear, lineWidth: 2)
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
                .font(.system(size: 18, weight: .semibold))
                .foregroundColor(color)

            Text(label)
                .font(.system(size: 10, weight: .medium))
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
